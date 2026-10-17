/*
 * This file is part of SGCraft, licensed under the MIT License (MIT).
 *
 * Copyright (c) AlmuraDev <https://github.com/AlmuraDev>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.almuradev.sgcraft.neoforge.test;

import com.almuradev.sgcraft.mod.Platform;
import com.almuradev.sgcraft.mod.SGConfig;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.energy.DialCosts;
import com.almuradev.sgcraft.mod.settings.SettingsCatalog;
import com.almuradev.sgcraft.mod.settings.SettingsCatalog.Result;
import com.almuradev.sgcraft.mod.settings.SettingsCatalog.Setting;
import com.almuradev.sgcraft.mod.worldgen.NaquadahOre;
import com.almuradev.sgcraft.mod.worldgen.NaquadahOreSettings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Game tests of the settings the settings screen and {@code /sgcraft config} change: that each reads its default,
 * that a live one applies at once and a restart one only in the file, that values out of range are refused, and that
 * the file can still be read after each change. Each test puts the settings back as they were. They run with
 * {@link SGGameTestsNeoForge}, which registers them.
 */
final class SettingsCatalogGameTestsNeoForge {

    private SettingsCatalogGameTestsNeoForge() {
    }

    static void register(final Map<String, BiConsumer<GameTestHelper, BlockPos>> tests) {
        tests.put("settings_catalog_defaults", SettingsCatalogGameTestsNeoForge::defaults);
        tests.put("settings_catalog_changes", SettingsCatalogGameTestsNeoForge::changes);
        tests.put("settings_catalog_refusals", SettingsCatalogGameTestsNeoForge::refusals);
    }

    // Every setting has its own key, reads its default from the defaults, and can be set to its default
    static void defaults(final GameTestHelper helper, final BlockPos origin) {
        final Set<String> keys = new HashSet<>();
        for (final Setting setting : SettingsCatalog.all()) {
            helper.assertTrue(keys.add(setting.key()), "Two settings have the key " + setting.key());
        }
        SettingsCatalogGameTestsNeoForge.restoring(helper, server -> {
            for (final Setting setting : SettingsCatalog.all()) {
                final Result result = SettingsCatalog.set(server, setting.key(), setting.defaultValue());
                helper.assertTrue(result.refusal().isEmpty(), setting.key() + " can't be set to its default "
                        + setting.defaultValue() + ": " + result.refusal().map(why -> why.getString()).orElse(""));
                helper.assertTrue(setting.valueIn(SGConfig.load()).equals(setting.defaultValue()),
                        "The file doesn't read back " + setting.key() + " as its default");
            }
        });
        helper.succeed();
    }

    // A live setting applies at once and is written to the file; a restart one is only written; a cost written as a
    // whole object stays readable, even set back to its default; naquadah ore generates with its change at once
    static void changes(final GameTestHelper helper, final BlockPos origin) {
        SettingsCatalogGameTestsNeoForge.restoring(helper, server -> {
            final boolean jump = server.config().wormholesJump();
            SettingsCatalogGameTestsNeoForge.set(helper, server, "gates.wormholes_jump", String.valueOf(!jump));
            helper.assertTrue(server.config().wormholesJump() == !jump, "Wormholes jumping didn't change at once");
            helper.assertTrue(SGConfig.load().wormholesJump() == !jump, "Wormholes jumping wasn't written");

            SettingsCatalogGameTestsNeoForge.set(helper, server, "energy.dial_costs.per_chevron.energy", "150");
            helper.assertTrue(server.config().dialCosts().get(DialCosts.Kind.CHEVRON).energy() == 150,
                    "The cost per chevron didn't change at once");
            helper.assertTrue(SGConfig.load().dialCosts().equals(server.config().dialCosts()),
                    "The file's dial costs aren't the server's");
            SettingsCatalogGameTestsNeoForge.set(helper, server, "energy.dial_costs.per_chevron.energy", "100");
            helper.assertTrue(SGConfig.load().dialCosts().equals(DialCosts.DEFAULTS),
                    "Dial costs set back to their defaults don't read back as the defaults");

            final int radius = server.config().remoteView().horizontalRadius();
            SettingsCatalogGameTestsNeoForge.set(helper, server, "remote_view.horizontal_radius", "40");
            helper.assertTrue(server.config().remoteView().horizontalRadius() == radius,
                    "A setting that waits for a restart changed at once");
            helper.assertTrue(SGConfig.load().remoteView().horizontalRadius() == 40,
                    "A setting that waits for a restart wasn't written");

            SettingsCatalogGameTestsNeoForge.set(helper, server, "worldgen.naquadah_ore.max_isolated_nodes", "5");
            helper.assertTrue(NaquadahOreSettings.Count.MAX_ISOLATED_NODES.get(NaquadahOre.settings()) == 5,
                    "Naquadah ore doesn't generate with its change at once");

            SettingsCatalogGameTestsNeoForge.set(helper, server, "zpm.tainted_explosion", "12.5");
            helper.assertTrue(server.config().options().zpm().taintedExplosion() == 12.5f,
                    "A tainted ZPM's explosion didn't change");
        });
        helper.succeed();
    }

    // A key that isn't a setting, a value of the wrong kind, and numbers out of range are refused, and change
    // nothing
    static void refusals(final GameTestHelper helper, final BlockPos origin) {
        SettingsCatalogGameTestsNeoForge.restoring(helper, server -> {
            final SGConfig before = server.config();
            SettingsCatalogGameTestsNeoForge.refused(helper, server, "gates.no_such_setting", "true");
            SettingsCatalogGameTestsNeoForge.refused(helper, server, "gates.wormholes_jump", "maybe");
            SettingsCatalogGameTestsNeoForge.refused(helper, server, "gates.chunk_loading_range", "1.5");
            SettingsCatalogGameTestsNeoForge.refused(helper, server, "gates.chunk_loading_range", "9");
            SettingsCatalogGameTestsNeoForge.refused(helper, server, "zpm.chest_chance", "101");
            SettingsCatalogGameTestsNeoForge.refused(helper, server, "energy.bridge_rates.fe_in_per_ge", "0");
            SettingsCatalogGameTestsNeoForge.refused(helper, server, "energy.per_naquadah", "-1");
            SettingsCatalogGameTestsNeoForge.refused(helper, server, "malp.tablet_drain", "NaN");
            helper.assertTrue(server.config().equals(before), "A refused change changed the settings");
        });
        helper.succeed();
    }

    private static void set(final GameTestHelper helper, final SGServer server, final String key,
            final String value) {
        final Result result = SettingsCatalog.set(server, key, value);
        helper.assertTrue(result.refusal().isEmpty(), "Setting " + key + " to " + value + " was refused: "
                + result.refusal().map(why -> why.getString()).orElse(""));
        helper.assertTrue(result.saved(), "Setting " + key + " wasn't written");
        helper.assertTrue(SettingsCatalogGameTestsNeoForge.readable(), "The settings file can't be read after "
                + "setting " + key);
    }

    private static void refused(final GameTestHelper helper, final SGServer server, final String key,
            final String value) {
        helper.assertTrue(SettingsCatalog.set(server, key, value).refusal().isPresent(),
                "Setting " + key + " to " + value + " wasn't refused");
    }

    // Whether the file parses with no mistakes, rather than falling back to defaults
    private static boolean readable() {
        try {
            final Path file = Platform.get().configDir().resolve("sgcraft-server.json");
            final JsonObject json = Files.exists(file) && JsonParser.parseString(Files.readString(file))
                    instanceof final JsonObject read ? read : new JsonObject();
            return SGConfig.CODEC.parse(JsonOps.INSTANCE, json).error().isEmpty();
        } catch (final IOException e) {
            return false;
        }
    }

    // Runs a test of changes, then puts the settings file and the running settings back as they were
    private static void restoring(final GameTestHelper helper, final Consumer<SGServer> test) {
        final SGServer server = SGServer.require();
        final SGConfig config = server.config();
        final NaquadahOreSettings ore = NaquadahOre.settings();
        final Path file = Platform.get().configDir().resolve("sgcraft-server.json");
        final Optional<String> text;
        try {
            text = Files.exists(file) ? Optional.of(Files.readString(file)) : Optional.empty();
        } catch (final IOException e) {
            throw new IllegalStateException("Couldn't read " + file, e);
        }
        try {
            test.accept(server);
        } finally {
            server.stargates().useConfig(config);
            NaquadahOre.use(ore);
            try {
                if (text.isPresent()) {
                    Files.writeString(file, text.get());
                } else {
                    Files.deleteIfExists(file);
                }
            } catch (final IOException e) {
                helper.assertTrue(false, "Couldn't put " + file + " back: " + e.getMessage());
            }
        }
    }
}
