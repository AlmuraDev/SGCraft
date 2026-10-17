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
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * The server settings for every gate that aren't about cost. {@code /sgcraft chunk_loading_range [<chunks>]} shows or
 * sets how many chunks around a busy gate's own stay loaded; {@code /sgcraft log_events [<true|false>]} shows or sets
 * whether gate events go to the server log. Changes are written to the settings file.
 */
final class SGOptionCommands {

    private SGOptionCommands() {
    }

    static ArgumentBuilder<CommandSourceStack, ?> chunkLoadingRange() {
        return Commands.literal("chunk_loading_range")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> SGOptionCommands.showRange(context.getSource()))
                .then(Commands.argument("chunks", IntegerArgumentType.integer(0, SGConfig.MAX_CHUNK_LOADING_RANGE))
                        .executes(context -> SGOptionCommands.setRange(context.getSource(),
                                IntegerArgumentType.getInteger(context, "chunks"))));
    }

    static ArgumentBuilder<CommandSourceStack, ?> logEvents() {
        return Commands.literal("log_events")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> SGOptionCommands.showLog(context.getSource()))
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> SGOptionCommands.setLog(context.getSource(),
                                BoolArgumentType.getBool(context, "enabled"))));
    }

    private static int showRange(final CommandSourceStack source) {
        final int range = SGServer.require().config().chunkLoadingRange();
        source.sendSuccess(() -> SGCommands.text("chunk_loading_range.show",
                "Busy gates keep %s chunks around their own loaded", range), false);
        return 1;
    }

    private static int setRange(final CommandSourceStack source, final int range) {
        final boolean saved = SGServer.require().stargates().setChunkLoadingRange(range);
        source.sendSuccess(() -> SGCommands.text("chunk_loading_range.set",
                "Busy gates now keep %s chunks around their own loaded", range), true);
        SGOptionCommands.saved(source, saved);
        return 1;
    }

    private static int showLog(final CommandSourceStack source) {
        source.sendSuccess(() -> SGServer.require().config().logStargateEvents()
                ? SGCommands.text("log_events.on", "Gate events are written to the server log")
                : SGCommands.text("log_events.off", "Gate events aren't written to the server log"), false);
        return 1;
    }

    private static int setLog(final CommandSourceStack source, final boolean log) {
        final boolean saved = SGServer.require().stargates().setLogStargateEvents(log);
        source.sendSuccess(() -> log
                ? SGCommands.text("log_events.on", "Gate events are written to the server log")
                : SGCommands.text("log_events.off", "Gate events aren't written to the server log"), true);
        SGOptionCommands.saved(source, saved);
        return 1;
    }

    private static void saved(final CommandSourceStack source, final boolean saved) {
        if (!saved) {
            source.sendFailure(SGCommands.text("costs.not_saved",
                    "The settings file couldn't be written, so the change lasts only until the server stops"));
        }
    }
}
