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

package com.almuradev.sgcraft.mod.command;

import com.almuradev.sgcraft.mod.SGConfig;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.gate.SGStargates;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * The server's settings, shown and changed in place of editing {@code sgcraft-server.json}. A change applies at once
 * and is written to the file.
 *
 * <ul>
 *     <li>{@code /sgcraft settings} shows the settings for every gate; {@code /sgcraft settings <name> <true|false>}
 *     sets one: {@code kawoosh_breaks_unbreakable}, {@code owners_break_gates} and
 *     {@code generated_gates_breakable}.</li>
 *     <li>{@code /sgcraft gate_defaults} lists the values gates have for a property when they have none of their
 *     own; {@code /sgcraft gate_defaults <property> <json>} sets one and {@code /sgcraft gate_defaults <property>
 *     reset} clears it. {@code sgcraft:iris} and {@code sgcraft:chevrons} reach gates as they first form.</li>
 * </ul>
 */
final class SGSettingsCommands {

    private static final String KAWOOSH_BREAKS_UNBREAKABLE = "kawoosh_breaks_unbreakable";
    private static final String OWNERS_BREAK_GATES = "owners_break_gates";
    private static final String GENERATED_GATES_BREAKABLE = "generated_gates_breakable";
    private static final String WORMHOLES_JUMP = "wormholes_jump";

    private static final SimpleCommandExceptionType UNKNOWN_KEY = new SimpleCommandExceptionType(
            SGCommands.text("gate_defaults.unknown", "There is no gate property by that name"));
    private static final SimpleCommandExceptionType BAD_VALUE = new SimpleCommandExceptionType(
            SGCommands.text("gate_defaults.bad_value", "That isn't a valid value for it, written as JSON"));

    private SGSettingsCommands() {
    }

    static ArgumentBuilder<CommandSourceStack, ?> settings() {
        return Commands.literal("settings").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> SGSettingsCommands.show(context.getSource()))
                .then(SGSettingsCommands.setting(SGSettingsCommands.KAWOOSH_BREAKS_UNBREAKABLE))
                .then(SGSettingsCommands.setting(SGSettingsCommands.OWNERS_BREAK_GATES))
                .then(SGSettingsCommands.setting(SGSettingsCommands.GENERATED_GATES_BREAKABLE))
                .then(SGSettingsCommands.setting(SGSettingsCommands.WORMHOLES_JUMP));
    }

    // <name> shows the settings, and <name> <true|false> sets one
    private static ArgumentBuilder<CommandSourceStack, ?> setting(final String name) {
        return Commands.literal(name)
                .executes(context -> SGSettingsCommands.show(context.getSource()))
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> SGSettingsCommands.set(context.getSource(), name,
                                BoolArgumentType.getBool(context, "value"))));
    }

    private static int show(final CommandSourceStack source) {
        final SGConfig config = SGServer.require().config();
        source.sendSuccess(() -> SGCommands.text("settings.header", "The server's settings for every gate:"),
                false);
        source.sendSuccess(() -> SGSettingsCommands.line(SGSettingsCommands.KAWOOSH_BREAKS_UNBREAKABLE,
                config.kawooshBreaksUnbreakable()), false);
        source.sendSuccess(() -> SGSettingsCommands.line(SGSettingsCommands.OWNERS_BREAK_GATES,
                config.ownersBreakGates()), false);
        source.sendSuccess(() -> SGSettingsCommands.line(SGSettingsCommands.GENERATED_GATES_BREAKABLE,
                config.generatedGatesBreakable()), false);
        source.sendSuccess(() -> SGSettingsCommands.line(SGSettingsCommands.WORMHOLES_JUMP,
                config.wormholesJump()), false);
        return 1;
    }

    private static int set(final CommandSourceStack source, final String name, final boolean value) {
        final SGStargates stargates = SGServer.require().stargates();
        final SGConfig config = SGServer.require().config();
        final boolean saved = switch (name) {
            case SGSettingsCommands.OWNERS_BREAK_GATES -> stargates.setBreaking(value,
                    config.generatedGatesBreakable());
            case SGSettingsCommands.GENERATED_GATES_BREAKABLE -> stargates.setBreaking(config.ownersBreakGates(),
                    value);
            case SGSettingsCommands.WORMHOLES_JUMP -> stargates.setWormholesJump(value);
            default -> stargates.setKawooshBreaksUnbreakable(value);
        };
        source.sendSuccess(() -> SGSettingsCommands.line(name, value), true);
        SGSettingsCommands.reportSaved(source, saved);
        return 1;
    }

    static ArgumentBuilder<CommandSourceStack, ?> gateDefaults() {
        return Commands.literal("gate_defaults").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> SGSettingsCommands.listDefaults(context.getSource()))
                .then(Commands.argument("property", IdentifierArgument.id())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                SGServer.require().stargates().gateDefaultKeys(), builder))
                        .then(Commands.literal("reset")
                                .executes(context -> SGSettingsCommands.setDefault(context, Optional.empty())))
                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                .executes(context -> SGSettingsCommands.setDefault(context, Optional.of(
                                        StringArgumentType.getString(context, "value"))))));
    }

    private static int listDefaults(final CommandSourceStack source) {
        final Map<Identifier, JsonElement> defaults = new TreeMap<>(SGServer.require().config().gateDefaults());
        if (defaults.isEmpty()) {
            source.sendSuccess(() -> SGCommands.text("gate_defaults.none",
                    "Gates without their own values have each property's default"), false);
            return 0;
        }
        source.sendSuccess(() -> SGCommands.text("gate_defaults.header",
                "Values gates have when they have none of their own:"), false);
        defaults.forEach((id, json) -> source.sendSuccess(() -> SGSettingsCommands.line(id.toString(), json), false));
        return defaults.size();
    }

    private static int setDefault(final CommandContext<CommandSourceStack> context, final Optional<String> text)
            throws CommandSyntaxException {
        final SGStargates stargates = SGServer.require().stargates();
        final Identifier id = IdentifierArgument.getId(context, "property");
        if (!stargates.gateDefaultKeys().contains(id)) {
            throw SGSettingsCommands.UNKNOWN_KEY.create();
        }
        final Map<Identifier, JsonElement> defaults = new HashMap<>(SGServer.require().config().gateDefaults());
        if (text.isEmpty()) {
            if (defaults.remove(id) == null) {
                context.getSource().sendFailure(SGCommands.text("result.no_change", "Nothing changed"));
                return 0;
            }
        } else {
            final JsonElement json;
            try {
                json = JsonParser.parseString(text.get());
            } catch (final RuntimeException e) {
                throw SGSettingsCommands.BAD_VALUE.create();
            }
            if (!stargates.isGateDefault(id, json)) {
                throw SGSettingsCommands.BAD_VALUE.create();
            }
            defaults.put(id, json);
        }
        final boolean saved = stargates.setGateDefaults(defaults);
        context.getSource().sendSuccess(() -> text.isEmpty()
                ? SGCommands.text("gate_defaults.reset", "Gates without their own %s have its default again",
                        id.toString())
                : SGCommands.text("gate_defaults.set", "Gates without their own %s now have %s", id.toString(),
                        defaults.get(id).toString()), true);
        SGSettingsCommands.reportSaved(context.getSource(), saved);
        return 1;
    }

    private static Component line(final String name, final Object value) {
        return Component.literal("  ").append(Component.literal(name).withStyle(ChatFormatting.AQUA))
                .append(" = " + value);
    }

    private static void reportSaved(final CommandSourceStack source, final boolean saved) {
        if (!saved) {
            source.sendFailure(SGCommands.text("settings.not_saved",
                    "The change applies until the server stops, but the settings file couldn't be written"));
        }
    }
}
