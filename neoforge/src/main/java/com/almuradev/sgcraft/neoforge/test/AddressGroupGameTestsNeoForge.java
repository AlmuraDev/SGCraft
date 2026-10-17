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

import com.almuradev.sgcraft.api.address.Symbol;
import com.almuradev.sgcraft.mod.SGConfig;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.address.AddressChanges.Change;
import com.almuradev.sgcraft.mod.address.AddressChanges.Kind;
import com.almuradev.sgcraft.mod.address.AddressChanges;
import com.almuradev.sgcraft.mod.address.AddressPlan.Recorded;
import com.almuradev.sgcraft.mod.address.AddressPlan;
import com.almuradev.sgcraft.mod.address.AddressSettings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Game tests of world groups: how the settings name them, how groups and worlds get their symbols and keep them, the
 * limits of {@link Symbol#COUNT} groups and {@link Symbol#COUNT} worlds a group, and the changes the commands and the
 * settings screen make to the settings file. They run with {@link SGGameTestsNeoForge}, which registers them.
 */
final class AddressGroupGameTestsNeoForge {

    private static final ResourceKey<Level> ABYDOS = AddressGroupGameTestsNeoForge.world("abydos");
    private static final ResourceKey<Level> CHULAK = AddressGroupGameTestsNeoForge.world("chulak");
    private static final ResourceKey<Level> ATLANTIS = AddressGroupGameTestsNeoForge.world("atlantis");

    private AddressGroupGameTestsNeoForge() {
    }

    static void register(final Map<String, BiConsumer<GameTestHelper, BlockPos>> tests) {
        tests.put("group_settings", AddressGroupGameTestsNeoForge::settings);
        tests.put("groups_get_symbols", AddressGroupGameTestsNeoForge::groupSymbols);
        tests.put("group_symbols_stay", AddressGroupGameTestsNeoForge::symbolsStay);
        tests.put("group_limits", AddressGroupGameTestsNeoForge::limits);
        tests.put("group_settings_edits", AddressGroupGameTestsNeoForge::edits);
    }

    private static ResourceKey<Level> world(final String name) {
        return ResourceKey.create(Registries.DIMENSION, SGCraft.id(name));
    }

    // The settings name groups, with or without a symbol, and the group each world is in; a world they put in none
    // is in the default group, and bad names and symbols are refused
    static void settings(final GameTestHelper helper, final BlockPos origin) {
        final AddressSettings settings = AddressGroupGameTestsNeoForge.parse("{\"groups\": {\"pegasus\": "
                + "{\"symbol\": \"P\"}, \"destiny\": {}}, \"worlds\": {\"sgcraft:atlantis\": {\"group\": "
                + "\"pegasus\"}}}").orElseThrow();
        helper.assertTrue(settings.groupOf(AddressGroupGameTestsNeoForge.ATLANTIS).equals("pegasus"),
                "Atlantis isn't in group pegasus");
        helper.assertTrue(settings.groupOf(AddressGroupGameTestsNeoForge.ABYDOS).equals(AddressSettings.DEFAULT_GROUP),
                "A world the settings put in no group isn't in the default group");
        helper.assertTrue(settings.groups().get("pegasus").symbol().equals(Optional.of("P")),
                "Group pegasus doesn't have the symbol P");
        helper.assertTrue(settings.groups().get("destiny").symbol().isEmpty(), "Group destiny has a symbol");
        helper.assertTrue(AddressGroupGameTestsNeoForge.parse("{\"groups\": {\"Bad Name\": {}}}").isEmpty(),
                "A group named with capitals and a space was taken");
        helper.assertTrue(AddressGroupGameTestsNeoForge.parse("{\"groups\": {\"pegasus\": {\"symbol\": \"PP\"}}}")
                .isEmpty(), "A group symbol of two symbols was taken");
        helper.succeed();
    }

    // The default group is @; a group with a symbol of its own has it; one without takes the first free symbol;
    // worlds take the first free symbol in their group, in the order the server first saw them
    static void groupSymbols(final GameTestHelper helper, final BlockPos origin) {
        final AddressSettings settings = AddressGroupGameTestsNeoForge.parse("{\"groups\": {\"destiny\": {}, "
                + "\"pegasus\": {\"symbol\": \"A\"}}, \"worlds\": {\"sgcraft:atlantis\": {\"group\": \"pegasus\"}}}")
                .orElseThrow();
        final AddressPlan plan = AddressPlan.of(settings, Recorded.NONE, List.of(AddressGroupGameTestsNeoForge.ABYDOS,
                AddressGroupGameTestsNeoForge.ATLANTIS, AddressGroupGameTestsNeoForge.CHULAK));
        helper.assertTrue(plan.problems().isEmpty(), "The plan has problems: " + plan.problems());
        AddressGroupGameTestsNeoForge.groupIs(helper, plan, AddressSettings.DEFAULT_GROUP, Symbol.GIZA);
        AddressGroupGameTestsNeoForge.groupIs(helper, plan, "pegasus", AddressGroupGameTestsNeoForge.symbol('A'));
        // A is pegasus's own, so destiny takes the next free symbol
        AddressGroupGameTestsNeoForge.groupIs(helper, plan, "destiny", AddressGroupGameTestsNeoForge.symbol('B'));
        AddressGroupGameTestsNeoForge.worldIs(helper, plan, AddressGroupGameTestsNeoForge.ABYDOS,
                AddressSettings.DEFAULT_GROUP, 'A');
        AddressGroupGameTestsNeoForge.worldIs(helper, plan, AddressGroupGameTestsNeoForge.CHULAK,
                AddressSettings.DEFAULT_GROUP, 'B');
        AddressGroupGameTestsNeoForge.worldIs(helper, plan, AddressGroupGameTestsNeoForge.ATLANTIS, "pegasus", 'A');
        // A world set to a group that doesn't exist is in the default group, with a problem saying so
        final AddressPlan unknown = AddressPlan.of(AddressGroupGameTestsNeoForge.parse("{\"worlds\": "
                + "{\"sgcraft:atlantis\": {\"group\": \"pegasus\"}}}").orElseThrow(), Recorded.NONE,
                List.of(AddressGroupGameTestsNeoForge.ATLANTIS));
        helper.assertTrue(unknown.groupOf(AddressGroupGameTestsNeoForge.ATLANTIS).equals(AddressSettings.DEFAULT_GROUP),
                "A world set to a missing group isn't in the default group");
        helper.assertTrue(AddressGroupGameTestsNeoForge.has(unknown, "unknown_group"),
                "A world set to a missing group raises no problem");
        // An empty symbol for the default group means @, with a warning
        final AddressPlan cleared = AddressPlan.of(AddressGroupGameTestsNeoForge.parse("{\"groups\": {\"default\": "
                + "{\"symbol\": \"\"}}}").orElseThrow(), Recorded.NONE, List.of());
        AddressGroupGameTestsNeoForge.groupIs(helper, cleared, AddressSettings.DEFAULT_GROUP, Symbol.GIZA);
        helper.assertTrue(AddressGroupGameTestsNeoForge.has(cleared, "default_cleared"),
                "Clearing the default group's symbol raises no warning");
        helper.succeed();
    }

    // Symbols given once stay: a group without its own symbol keeps the one it had, and a world keeps its symbol in
    // its group even when worlds before it are gone; a world moved to another group keeps its symbol if it is free
    // there, or takes the first free one
    static void symbolsStay(final GameTestHelper helper, final BlockPos origin) {
        final Map<String, Symbol> groupSymbols = new LinkedHashMap<>();
        groupSymbols.put(AddressSettings.DEFAULT_GROUP, Symbol.GIZA);
        groupSymbols.put("destiny", AddressGroupGameTestsNeoForge.symbol('Q'));
        final Map<ResourceKey<Level>, Symbol> worldSymbols = new LinkedHashMap<>();
        worldSymbols.put(AddressGroupGameTestsNeoForge.ABYDOS, AddressGroupGameTestsNeoForge.symbol('A'));
        worldSymbols.put(AddressGroupGameTestsNeoForge.CHULAK, AddressGroupGameTestsNeoForge.symbol('C'));
        worldSymbols.put(AddressGroupGameTestsNeoForge.ATLANTIS, AddressGroupGameTestsNeoForge.symbol('B'));
        final Recorded recorded = new Recorded(groupSymbols, worldSymbols, Map.of());
        final AddressSettings settings = AddressGroupGameTestsNeoForge.parse("{\"groups\": {\"destiny\": {}, "
                + "\"pegasus\": {\"symbol\": \"P\"}}, \"worlds\": {\"sgcraft:atlantis\": {\"group\": \"pegasus\"}}}")
                .orElseThrow();
        // Abydos isn't loaded, but keeps its symbol, so Chulak doesn't take it
        final AddressPlan plan = AddressPlan.of(settings, recorded, List.of(AddressGroupGameTestsNeoForge.CHULAK,
                AddressGroupGameTestsNeoForge.ATLANTIS));
        AddressGroupGameTestsNeoForge.groupIs(helper, plan, "destiny", AddressGroupGameTestsNeoForge.symbol('Q'));
        AddressGroupGameTestsNeoForge.worldIs(helper, plan, AddressGroupGameTestsNeoForge.CHULAK,
                AddressSettings.DEFAULT_GROUP, 'C');
        helper.assertTrue(plan.symbolOf(AddressGroupGameTestsNeoForge.ABYDOS).equals(
                Optional.of(AddressGroupGameTestsNeoForge.symbol('A'))), "A world the server doesn't have lost its "
                + "symbol");
        // Atlantis had B in the default group, which is free in pegasus
        AddressGroupGameTestsNeoForge.worldIs(helper, plan, AddressGroupGameTestsNeoForge.ATLANTIS, "pegasus", 'B');
        // Chulak moved to pegasus, where Atlantis has C, so Chulak takes the first free symbol, A
        final Map<ResourceKey<Level>, Symbol> clash = new LinkedHashMap<>();
        clash.put(AddressGroupGameTestsNeoForge.ATLANTIS, AddressGroupGameTestsNeoForge.symbol('C'));
        clash.put(AddressGroupGameTestsNeoForge.CHULAK, AddressGroupGameTestsNeoForge.symbol('C'));
        final Map<ResourceKey<Level>, String> clashGroups = new LinkedHashMap<>();
        clashGroups.put(AddressGroupGameTestsNeoForge.ATLANTIS, "pegasus");
        clashGroups.put(AddressGroupGameTestsNeoForge.CHULAK, AddressSettings.DEFAULT_GROUP);
        final AddressPlan moved = AddressPlan.of(AddressGroupGameTestsNeoForge.parse("{\"groups\": {\"pegasus\": "
                + "{\"symbol\": \"P\"}}, \"worlds\": {\"sgcraft:atlantis\": {\"group\": \"pegasus\"}, "
                + "\"sgcraft:chulak\": {\"group\": \"pegasus\"}}}").orElseThrow(),
                new Recorded(groupSymbols, clash, clashGroups), List.of(AddressGroupGameTestsNeoForge.ATLANTIS,
                        AddressGroupGameTestsNeoForge.CHULAK));
        AddressGroupGameTestsNeoForge.worldIs(helper, moved, AddressGroupGameTestsNeoForge.ATLANTIS, "pegasus", 'C');
        AddressGroupGameTestsNeoForge.worldIs(helper, moved, AddressGroupGameTestsNeoForge.CHULAK, "pegasus", 'A');
        helper.succeed();
    }

    // A server has at most 37 groups and a group at most 37 worlds; a group whose symbol another has is left out
    static void limits(final GameTestHelper helper, final BlockPos origin) {
        final StringBuilder groups = new StringBuilder("{\"groups\": {");
        for (int index = 0; index < Symbol.COUNT; index++) {
            groups.append(index == 0 ? "" : ", ").append("\"g").append(index).append("\": {}");
        }
        final AddressPlan many = AddressPlan.of(AddressGroupGameTestsNeoForge.parse(groups + "}}").orElseThrow(),
                Recorded.NONE, List.of());
        helper.assertTrue(many.groups().size() == Symbol.COUNT, "The server has " + many.groups().size()
                + " groups, not " + Symbol.COUNT);
        helper.assertTrue(many.group("g36").isEmpty(), "The 38th group wasn't left out");
        helper.assertTrue(AddressGroupGameTestsNeoForge.has(many, "too_many_groups"),
                "Too many groups raise no problem");
        final AddressPlan clash = AddressPlan.of(AddressGroupGameTestsNeoForge.parse("{\"groups\": {\"pegasus\": "
                + "{\"symbol\": \"P\"}, \"ori\": {\"symbol\": \"P\"}}}").orElseThrow(), Recorded.NONE, List.of());
        helper.assertTrue(clash.group("ori").isEmpty(), "A group with another's symbol wasn't left out");
        helper.assertTrue(AddressGroupGameTestsNeoForge.has(clash, "symbol_taken"),
                "Two groups with one symbol raise no problem");
        final List<ResourceKey<Level>> worlds = new ArrayList<>();
        for (int index = 0; index <= Symbol.COUNT; index++) {
            worlds.add(AddressGroupGameTestsNeoForge.world("world_" + index));
        }
        final AddressPlan full = AddressPlan.of(AddressSettings.DEFAULTS, Recorded.NONE, worlds);
        helper.assertTrue(full.symbolOf(worlds.get(Symbol.COUNT - 1)).isPresent(), "The 37th world has no symbol");
        helper.assertTrue(full.symbolOf(worlds.get(Symbol.COUNT)).isEmpty(), "The 38th world has a symbol");
        helper.assertTrue(AddressGroupGameTestsNeoForge.has(full, "group_full"), "A full group raises no warning");
        helper.succeed();
    }

    // The changes write the file as the settings expect: removing a group takes its worlds out of it, putting a
    // world in the default group drops its group, and nothing empty is left behind
    static void edits(final GameTestHelper helper, final BlockPos origin) {
        final JsonObject root = new JsonObject();
        final Identifier atlantis = AddressGroupGameTestsNeoForge.ATLANTIS.identifier();
        AddressChanges.edit(root, new Change(Kind.CREATE_GROUP, Optional.empty(), "pegasus", "P"));
        AddressChanges.edit(root, new Change(Kind.WORLD_GROUP, Optional.of(atlantis), "pegasus", ""));
        AddressChanges.edit(root, new Change(Kind.POINT_OF_ORIGIN, Optional.of(atlantis), "", "A"));
        AddressSettings settings = AddressGroupGameTestsNeoForge.read(helper, root);
        helper.assertTrue(settings.groupOf(AddressGroupGameTestsNeoForge.ATLANTIS).equals("pegasus"),
                "Atlantis wasn't put in group pegasus");
        helper.assertTrue(settings.layoutOf(AddressGroupGameTestsNeoForge.ATLANTIS).pointOfOrigin().equals("A"),
                "Atlantis's point of origin wasn't set");
        AddressChanges.edit(root, new Change(Kind.REMOVE_GROUP, Optional.empty(), "pegasus", ""));
        settings = AddressGroupGameTestsNeoForge.read(helper, root);
        helper.assertTrue(settings.groupOf(AddressGroupGameTestsNeoForge.ATLANTIS).equals(
                AddressSettings.DEFAULT_GROUP), "Removing a group left its world in it");
        helper.assertTrue(settings.layoutOf(AddressGroupGameTestsNeoForge.ATLANTIS).pointOfOrigin().equals("A"),
                "Removing a group dropped its world's point of origin");
        AddressChanges.edit(root, new Change(Kind.CLEAR_ORIGIN, Optional.of(atlantis), "", ""));
        final JsonObject addresses = root.getAsJsonObject("addresses");
        helper.assertTrue(!addresses.has("worlds") && !addresses.has("groups"),
                "Empty sections were left in the file: " + root);
        helper.succeed();
    }

    private static AddressSettings read(final GameTestHelper helper, final JsonObject root) {
        return SGConfig.CODEC.parse(JsonOps.INSTANCE, root).result().map(SGConfig::addresses).orElseThrow(() ->
                helper.assertionException(Component.literal("The file doesn't read: "
                        + root)));
    }

    private static Optional<AddressSettings> parse(final String addresses) {
        return SGConfig.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"addresses\": " + addresses + "}"))
                .result().map(SGConfig::addresses);
    }

    private static Symbol symbol(final char character) {
        return Symbol.of(character).orElseThrow();
    }

    private static boolean has(final AddressPlan plan, final String key) {
        return plan.problems().stream().anyMatch(problem -> problem.key().endsWith("." + key));
    }

    private static void groupIs(final GameTestHelper helper, final AddressPlan plan, final String name,
            final Symbol symbol) {
        final Optional<Symbol> has = plan.group(name).map(AddressPlan.Group::symbol);
        helper.assertTrue(has.equals(Optional.of(symbol)), "Group " + name + " has " + has + ", not " + symbol);
    }

    private static void worldIs(final GameTestHelper helper, final AddressPlan plan, final ResourceKey<Level> world,
            final String group, final char symbol) {
        helper.assertTrue(plan.groupOf(world).equals(group), world.identifier() + " is in group "
                + plan.groupOf(world) + ", not " + group);
        final Optional<Symbol> has = plan.symbolOf(world);
        helper.assertTrue(has.equals(Optional.of(AddressGroupGameTestsNeoForge.symbol(symbol))),
                world.identifier() + " has " + has + ", not " + symbol);
    }
}
