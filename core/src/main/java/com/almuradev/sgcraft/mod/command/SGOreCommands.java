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

import com.almuradev.sgcraft.mod.worldgen.NaquadahOre;
import com.almuradev.sgcraft.mod.worldgen.NaquadahOreSettings;
import com.almuradev.sgcraft.mod.worldgen.OreChunks;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * {@code /sgcraft ore}: how naquadah ore generates. On its own it lists the settings;
 * {@code enable_naquadah_ore <true|false>}, {@code add_ores_to_existing_worlds <true|false>} and each count's name
 * with a number set one, and {@code dimension_ids <add|remove> <world>} changes the worlds it generates in. Changes
 * apply to chunks generated or loaded from then on and are written to the settings file.
 *
 * <p>{@code chunk [<x z>]} tells whether the chunk the caller is in, or the one holding a position, has had its
 * naquadah ore. {@code generate} after it gives that chunk ore now, and {@code unmark} has it get ore again the next
 * time it loads, while ore is added to existing worlds.</p>
 */
final class SGOreCommands {

    private static final SimpleCommandExceptionType NOT_LOADED = new SimpleCommandExceptionType(
            SGCommands.text("ore.not_loaded", "That chunk isn't loaded"));

    private SGOreCommands() {
    }

    @FunctionalInterface
    private interface ChunkFinder {
        ChunkPos find(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException;
    }

    @FunctionalInterface
    private interface FlagSetter {
        NaquadahOreSettings with(final NaquadahOreSettings settings, final boolean value);
    }

    static ArgumentBuilder<CommandSourceStack, ?> ore() {
        final LiteralArgumentBuilder<CommandSourceStack> ore = Commands.literal("ore")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> SGOreCommands.show(context.getSource()));
        ore.then(SGOreCommands.flag("enable_naquadah_ore", NaquadahOreSettings::withEnableNaquadahOre));
        ore.then(SGOreCommands.flag("add_ores_to_existing_worlds",
                NaquadahOreSettings::withAddOresToExistingWorlds));
        for (final NaquadahOreSettings.Count count : NaquadahOreSettings.Count.values()) {
            ore.then(Commands.literal(count.id())
                    .then(Commands.argument("value", IntegerArgumentType.integer(0))
                            .executes(context -> SGOreCommands.set(context.getSource(), count.with(
                                    NaquadahOre.settings(), IntegerArgumentType.getInteger(context, "value"))))));
        }
        ore.then(Commands.literal("dimension_ids")
                .then(Commands.literal("add")
                        .then(Commands.argument("world", IdentifierArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                        context.getSource().getServer().levelKeys().stream()
                                                .map(ResourceKey::identifier), builder))
                                .executes(context -> SGOreCommands.set(context.getSource(), NaquadahOre.settings()
                                        .withDimension(SGOreCommands.world(context), true)))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("world", IdentifierArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                        NaquadahOre.settings().dimensionIds().stream()
                                                .map(ResourceKey::identifier), builder))
                                .executes(context -> SGOreCommands.set(context.getSource(), NaquadahOre.settings()
                                        .withDimension(SGOreCommands.world(context), false))))));
        final ChunkFinder here = context -> ChunkPos.containing(BlockPos.containing(
                context.getSource().getPosition()));
        final ChunkFinder at = context -> ColumnPosArgument.getColumnPos(context, "pos").toChunkPos();
        ore.then(SGOreCommands.chunk(Commands.literal("chunk"), here)
                .then(SGOreCommands.chunk(Commands.argument("pos", ColumnPosArgument.columnPos()), at)));
        return ore;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> flag(final String name, final FlagSetter setter) {
        return Commands.literal(name)
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> SGOreCommands.set(context.getSource(),
                                setter.with(NaquadahOre.settings(), BoolArgumentType.getBool(context, "value")))));
    }

    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T chunk(final T builder,
            final ChunkFinder finder) {
        return builder
                .executes(context -> SGOreCommands.chunk(context.getSource(), finder.find(context)))
                .then(Commands.literal("generate")
                        .executes(context -> SGOreCommands.generate(context.getSource(), finder.find(context))))
                .then(Commands.literal("unmark")
                        .executes(context -> SGOreCommands.unmark(context.getSource(), finder.find(context))));
    }

    private static ResourceKey<Level> world(final CommandContext<CommandSourceStack> context) {
        return ResourceKey.create(Registries.DIMENSION, IdentifierArgument.getId(context, "world"));
    }

    private static int show(final CommandSourceStack source) {
        final NaquadahOreSettings settings = NaquadahOre.settings();
        source.sendSuccess(() -> SGCommands.text("ore.header", "How naquadah ore generates:"), false);
        SGOreCommands.line(source, "enable_naquadah_ore", String.valueOf(settings.enableNaquadahOre()));
        for (final NaquadahOreSettings.Count count : NaquadahOreSettings.Count.values()) {
            SGOreCommands.line(source, count.id(), String.valueOf(count.get(settings)));
        }
        SGOreCommands.line(source, "dimension_ids", SGOreCommands.worlds(settings));
        SGOreCommands.line(source, "add_ores_to_existing_worlds",
                String.valueOf(settings.addOresToExistingWorlds()));
        final int pending = NaquadahOre.pending();
        if (pending > 0) {
            source.sendSuccess(() -> SGCommands.text("ore.pending", "%s old chunks are waiting for ore", pending),
                    false);
        }
        return 1;
    }

    private static void line(final CommandSourceStack source, final String name, final String value) {
        source.sendSuccess(() -> Component.literal("  ").append(Component.literal(name)
                .withStyle(ChatFormatting.AQUA)).append(" " + value), false);
    }

    private static String worlds(final NaquadahOreSettings settings) {
        return settings.dimensionIds().isEmpty() ? "[]" : settings.dimensionIds().stream()
                .map(world -> world.identifier().toString()).collect(Collectors.joining(", "));
    }

    private static int set(final CommandSourceStack source, final NaquadahOreSettings settings) {
        final boolean saved = NaquadahOre.save(settings);
        source.sendSuccess(() -> SGCommands.text("ore.set",
                "Naquadah ore generates with these settings from now on"), true);
        SGOreCommands.show(source);
        if (!saved) {
            source.sendFailure(SGCommands.text("ore.not_saved",
                    "The change applies until the server stops, but the settings file couldn't be written"));
        }
        return 1;
    }

    private static int chunk(final CommandSourceStack source, final ChunkPos chunk) {
        final ServerLevel level = source.getLevel();
        if (OreChunks.of(level).marked(chunk)) {
            source.sendSuccess(() -> SGCommands.text("ore.chunk.marked",
                    "Chunk %s, %s has had its naquadah ore", chunk.x(), chunk.z()), false);
        } else if (NaquadahOre.pending(level, chunk)) {
            source.sendSuccess(() -> SGCommands.text("ore.chunk.pending",
                    "Chunk %s, %s is waiting for its naquadah ore", chunk.x(), chunk.z()), false);
        } else {
            source.sendSuccess(() -> SGCommands.text("ore.chunk.unmarked",
                    "Chunk %s, %s hasn't had its naquadah ore", chunk.x(), chunk.z()), false);
        }
        if (!NaquadahOre.settings().generatesIn(level.dimension())) {
            source.sendSuccess(() -> SGCommands.text("ore.chunk.not_here",
                    "Naquadah ore doesn't generate in this world").withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int generate(final CommandSourceStack source, final ChunkPos pos) throws CommandSyntaxException {
        final ServerLevel level = source.getLevel();
        final LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x(), pos.z());
        if (chunk == null) {
            throw SGOreCommands.NOT_LOADED.create();
        }
        final Optional<Integer> placed = NaquadahOre.generate(level, chunk, RandomSource.create());
        if (placed.isEmpty()) {
            source.sendFailure(SGCommands.text("ore.generate.other_feature",
                    "A data pack made sgcraft:naquadah_ore another kind of feature, so it can't run for one chunk"));
            return 0;
        }
        source.sendSuccess(() -> SGCommands.text("ore.generate.done",
                "Turned %s blocks of chunk %s, %s to naquadah ore", placed.get(), pos.x(), pos.z()), true);
        return placed.get();
    }

    private static int unmark(final CommandSourceStack source, final ChunkPos chunk) {
        if (!OreChunks.of(source.getLevel()).unmark(chunk)) {
            source.sendFailure(SGCommands.text("ore.unmark.none", "Chunk %s, %s hasn't had its naquadah ore",
                    chunk.x(), chunk.z()));
            return 0;
        }
        source.sendSuccess(() -> SGCommands.text("ore.unmark.done",
                "Chunk %s, %s gets naquadah ore again when it next loads, while add_ores_to_existing_worlds is on",
                chunk.x(), chunk.z()), true);
        return 1;
    }
}
