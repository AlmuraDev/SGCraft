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

import com.almuradev.sgcraft.api.address.Symbol;
import com.almuradev.sgcraft.mod.SGConfig;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.address.AddressLayout;
import com.almuradev.sgcraft.mod.address.AddressSettings;
import com.almuradev.sgcraft.mod.address.SGAddressing;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Locale;
import java.util.Optional;

/**
 * The address layout: how many chunks wide each addressed cell is, and the point of origin addresses end with, for
 * every world or for one. {@code /sgcraft addresses} shows each world's layout, now and after a restart;
 * {@code cell_chunks <chunks> [<world>]}, {@code point_of_origin <symbol> [<world>]} and {@code clear <world>} change
 * it. Changes are written to the settings file and take effect when the server next starts, as they change every
 * address in the worlds they reach.
 */
final class SGAddressCommands {

    private static final DynamicCommandExceptionType NOT_A_SYMBOL = new DynamicCommandExceptionType(
            text -> SGCommands.text("addresses.not_a_symbol",
                    "Not a point of origin: %s. Give one symbol, giza or \"@\" for @, or none", text));

    private SGAddressCommands() {
    }

    static ArgumentBuilder<CommandSourceStack, ?> addresses() {
        return Commands.literal("addresses")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> SGAddressCommands.show(context.getSource()))
                .then(Commands.literal("cell_chunks")
                        .then(Commands.argument("chunks", IntegerArgumentType.integer(1, AddressLayout.MAX_CELL_CHUNKS))
                                .executes(context -> SGAddressCommands.setCellChunks(context.getSource(),
                                        Optional.empty(), IntegerArgumentType.getInteger(context, "chunks")))
                                .then(Commands.argument("world", DimensionArgument.dimension())
                                        .executes(context -> SGAddressCommands.setCellChunks(context.getSource(),
                                                Optional.of(DimensionArgument.getDimension(context, "world")),
                                                IntegerArgumentType.getInteger(context, "chunks"))))))
                .then(Commands.literal("point_of_origin")
                        .then(Commands.argument("symbol", StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        new String[] {"giza", "none"}, builder))
                                .executes(context -> SGAddressCommands.setOrigin(context.getSource(),
                                        Optional.empty(), StringArgumentType.getString(context, "symbol")))
                                .then(Commands.argument("world", DimensionArgument.dimension())
                                        .executes(context -> SGAddressCommands.setOrigin(context.getSource(),
                                                Optional.of(DimensionArgument.getDimension(context, "world")),
                                                StringArgumentType.getString(context, "symbol"))))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("world", DimensionArgument.dimension())
                                .executes(context -> SGAddressCommands.clear(context.getSource(),
                                        DimensionArgument.getDimension(context, "world")))));
    }

    private static int show(final CommandSourceStack source) {
        final SGAddressing addressing = SGServer.require().addressing();
        final AddressSettings pending = SGConfig.load().addresses();
        for (final ServerLevel level : source.getServer().getAllLevels()) {
            final ResourceKey<Level> world = level.dimension();
            final AddressLayout now = addressing.layoutOf(world);
            final AddressLayout next = pending.layoutOf(world);
            final long gates = addressing.gateCount(world);
            source.sendSuccess(() -> SGCommands.text("addresses.world", "%s: %s, %s gates",
                    world.identifier().toString(), now.describe(), gates), false);
            if (!next.equals(now)) {
                source.sendSuccess(() -> SGCommands.text("addresses.pending", "  After a restart: %s",
                        next.describe()).withStyle(gates > 0 ? ChatFormatting.RED : ChatFormatting.YELLOW), false);
            }
        }
        return 1;
    }

    private static int setCellChunks(final CommandSourceStack source, final Optional<ServerLevel> world,
            final int chunks) {
        final boolean saved = SGConfig.saveCellChunks(world.map(level -> level.dimension().identifier()), chunks);
        return SGAddressCommands.changed(source, world, saved);
    }

    private static int setOrigin(final CommandSourceStack source, final Optional<ServerLevel> world,
            final String text) throws CommandSyntaxException {
        final String origin = SGAddressCommands.origin(text);
        final boolean saved = SGConfig.savePointOfOrigin(world.map(level -> level.dimension().identifier()), origin);
        return SGAddressCommands.changed(source, world, saved);
    }

    private static int clear(final CommandSourceStack source, final ServerLevel world) {
        final boolean saved = SGConfig.clearWorldLayout(world.dimension().identifier());
        return SGAddressCommands.changed(source, Optional.of(world), saved);
    }

    // Reads a point of origin as typed: one symbol, giza for @, or none for addresses without one
    private static String origin(final String text) throws CommandSyntaxException {
        final String lower = text.toLowerCase(Locale.ROOT);
        if (lower.equals("none")) {
            return "";
        }
        if (lower.equals("giza")) {
            return String.valueOf(Symbol.GIZA.character());
        }
        final String upper = text.toUpperCase(Locale.ROOT);
        if (upper.length() == 1 && Symbol.of(upper.charAt(0)).isPresent()) {
            return upper;
        }
        throw SGAddressCommands.NOT_A_SYMBOL.create(text);
    }

    // Tells the caller what the change does when the server next starts, warning about each world whose gates'
    // addresses it changes, in chat and in the server log
    private static int changed(final CommandSourceStack source, final Optional<ServerLevel> only,
            final boolean saved) {
        if (!saved) {
            source.sendFailure(SGCommands.text("settings.not_saved", "The settings file couldn't be written"));
            return 0;
        }
        final SGAddressing addressing = SGServer.require().addressing();
        final AddressSettings pending = SGConfig.load().addresses();
        int affected = 0;
        for (final ServerLevel level : source.getServer().getAllLevels()) {
            final ResourceKey<Level> world = level.dimension();
            if (only.isPresent() && !only.get().dimension().equals(world)) {
                continue;
            }
            final AddressLayout now = addressing.layoutOf(world);
            final AddressLayout next = pending.layoutOf(world);
            final Identifier id = world.identifier();
            if (next.equals(now)) {
                source.sendSuccess(() -> SGCommands.text("addresses.unchanged",
                        "%s keeps its layout after a restart: %s", id.toString(), now.describe()), true);
                continue;
            }
            final long gates = addressing.gateCount(world);
            source.sendSuccess(() -> SGCommands.text("addresses.next",
                    "%s will have %s when the server next starts", id.toString(), next.describe()), true);
            if (gates > 0) {
                affected++;
                source.sendFailure(SGCommands.text("addresses.warning",
                        "WARNING: this changes the address of every gate in %s (%s gates). Address books and saved "
                                + "addresses will stop reaching them. Set it back before restarting to keep them.",
                        id.toString(), gates).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                SGCraft.LOG.warn("Every address in {} ({} gates) changes when the server next starts: {} becomes {}",
                        id, gates, now.describe(), next.describe());
            }
        }
        if (affected == 0) {
            source.sendSuccess(() -> SGCommands.text("addresses.no_gates_changed",
                    "No gate's address changes"), false);
        }
        return 1;
    }
}
