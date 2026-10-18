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

import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.computer.ComputerSettings;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * The server's dialing computer settings.
 *
 * <ul>
 *     <li>{@code /sgcraft computers} shows the settings.</li>
 *     <li>{@code /sgcraft computers link_range <blocks>} sets how far from a computer the gate it dials may be.</li>
 *     <li>{@code /sgcraft computers chair_reach <blocks>} sets how far in front of a computer its chair may
 *     stand.</li>
 *     <li>{@code /sgcraft computers requires_power <true|false>} sets whether dialing computers and MALP consoles
 *     need power to run.</li>
 *     <li>{@code /sgcraft computers energy_per_tick <energy>} sets how much gate energy a computer uses each
 *     tick.</li>
 *     <li>{@code /sgcraft computers power_reach <blocks>} sets how far from a computer the power units and ZPM
 *     blocks it draws on may stand.</li>
 *     <li>{@code /sgcraft computers history_size <addresses>} sets how many of the addresses last dialed from a
 *     computer it keeps.</li>
 * </ul>
 */
final class SGComputerCommands {

    private SGComputerCommands() {
    }

    static ArgumentBuilder<CommandSourceStack, ?> computers() {
        return Commands.literal("computers")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> SGComputerCommands.show(context.getSource()))
                .then(Commands.literal("link_range").then(Commands.argument("blocks",
                        IntegerArgumentType.integer(1, ComputerSettings.MAX_LINK_RANGE))
                        .executes(context -> SGComputerCommands.set(context.getSource(), "link_range",
                                IntegerArgumentType.getInteger(context, "blocks")))))
                .then(Commands.literal("chair_reach").then(Commands.argument("blocks",
                        IntegerArgumentType.integer(1, ComputerSettings.MAX_CHAIR_REACH))
                        .executes(context -> SGComputerCommands.set(context.getSource(), "chair_reach",
                                IntegerArgumentType.getInteger(context, "blocks")))))
                .then(Commands.literal("requires_power").then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> SGComputerCommands.set(context.getSource(), ComputerSettings.current()
                                .withRequiresPower(BoolArgumentType.getBool(context, "value"))))))
                .then(Commands.literal("energy_per_tick").then(Commands.argument("energy",
                        DoubleArgumentType.doubleArg(0))
                        .executes(context -> SGComputerCommands.set(context.getSource(), ComputerSettings.current()
                                .withEnergyPerTick(DoubleArgumentType.getDouble(context, "energy"))))))
                .then(Commands.literal("power_reach").then(Commands.argument("blocks",
                        IntegerArgumentType.integer(1, ComputerSettings.MAX_POWER_REACH))
                        .executes(context -> SGComputerCommands.set(context.getSource(), "power_reach",
                                IntegerArgumentType.getInteger(context, "blocks")))))
                .then(Commands.literal("history_size").then(Commands.argument("addresses",
                        IntegerArgumentType.integer(0, ComputerSettings.MAX_HISTORY_SIZE))
                        .executes(context -> SGComputerCommands.set(context.getSource(), "history_size",
                                IntegerArgumentType.getInteger(context, "addresses")))));
    }

    private static int show(final CommandSourceStack source) {
        final ComputerSettings settings = ComputerSettings.current();
        source.sendSuccess(() -> SGCommands.text("computers.header", "The server's dialing computer settings:"),
                false);
        source.sendSuccess(() -> SGComputerCommands.line("link_range", settings.linkRange()), false);
        source.sendSuccess(() -> SGComputerCommands.line("chair_reach", settings.chairReach()), false);
        source.sendSuccess(() -> SGComputerCommands.line("requires_power", settings.requiresPower()), false);
        source.sendSuccess(() -> SGComputerCommands.line("energy_per_tick", settings.energyPerTick()), false);
        source.sendSuccess(() -> SGComputerCommands.line("power_reach", settings.powerReach()), false);
        source.sendSuccess(() -> SGComputerCommands.line("history_size", settings.historySize()), false);
        return 1;
    }

    private static int set(final CommandSourceStack source, final String name, final int value) {
        return SGComputerCommands.set(source, ComputerSettings.current().with(name, value));
    }

    private static int set(final CommandSourceStack source, final ComputerSettings settings) {
        final boolean saved = SGServer.require().stargates().setComputers(settings);
        SGComputerCommands.show(source);
        if (!saved) {
            source.sendFailure(SGCommands.text("settings.not_saved",
                    "The change applies until the server stops, but the settings file couldn't be written"));
        }
        return 1;
    }

    private static Component line(final String name, final Object value) {
        return Component.literal("  ").append(Component.literal(name).withStyle(ChatFormatting.AQUA))
                .append(" = " + value);
    }
}
