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

package com.almuradev.sgcraft.command;

import com.almuradev.sgcraft.mod.DialCosts;
import com.almuradev.sgcraft.mod.SGServer;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * {@code /sgcraft costs}: what a wormhole costs the gate that dials it, before distance, for every gate without costs
 * of its own; a gate's own are set with the configurator or {@code /sgcraft gate property}. On its own it lists the
 * costs; {@code <per_chevron|open|per_tick> enabled <true|false>} turns one on or off, and
 * {@code <per_chevron|open|per_tick> energy <amount>} sets how much it takes. Changes apply to dials from then on and
 * are written to the settings file.
 */
final class SGCostCommands {

    private SGCostCommands() {
    }

    static ArgumentBuilder<CommandSourceStack, ?> costs() {
        final LiteralArgumentBuilder<CommandSourceStack> costs = Commands.literal("costs")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> SGCostCommands.show(context.getSource()));
        for (final DialCosts.Kind kind : DialCosts.Kind.values()) {
            costs.then(Commands.literal(kind.id())
                    .then(Commands.literal("enabled")
                            .then(Commands.argument("enabled", BoolArgumentType.bool())
                                    .executes(context -> SGCostCommands.set(context.getSource(), kind,
                                            new DialCosts.Cost(BoolArgumentType.getBool(context, "enabled"),
                                                    SGCostCommands.current().get(kind).energy())))))
                    .then(Commands.literal("energy")
                            .then(Commands.argument("energy", DoubleArgumentType.doubleArg(0))
                                    .executes(context -> SGCostCommands.set(context.getSource(), kind,
                                            new DialCosts.Cost(SGCostCommands.current().get(kind).enabled(),
                                                    DoubleArgumentType.getDouble(context, "energy")))))));
        }
        return costs;
    }

    private static DialCosts current() {
        return SGServer.require().config().dialCosts();
    }

    private static int show(final CommandSourceStack source) {
        source.sendSuccess(() -> SGCommands.text("costs.header",
                "What a wormhole costs, before distance, for gates without costs of their own:"), false);
        for (final DialCosts.Kind kind : DialCosts.Kind.values()) {
            source.sendSuccess(() -> SGCostCommands.line(kind, SGCostCommands.current().get(kind)), false);
        }
        return 1;
    }

    private static Component line(final DialCosts.Kind kind, final DialCosts.Cost cost) {
        return Component.literal("  ").append(Component.literal(kind.id()).withStyle(ChatFormatting.AQUA))
                .append(" ").append(cost.enabled()
                        ? SGCommands.text("costs.on", "%s energy", SGCostCommands.amount(cost.energy()))
                        : SGCommands.text("costs.off", "off (%s energy when on)",
                                SGCostCommands.amount(cost.energy())).withStyle(ChatFormatting.GRAY));
    }

    private static String amount(final double energy) {
        return energy == Math.rint(energy) ? String.valueOf((long) energy) : String.valueOf(energy);
    }

    private static int set(final CommandSourceStack source, final DialCosts.Kind kind, final DialCosts.Cost cost) {
        final boolean saved = SGServer.require().stargates().setDialCosts(SGCostCommands.current().with(kind, cost));
        source.sendSuccess(() -> SGCostCommands.line(kind, cost), true);
        if (!saved) {
            source.sendFailure(SGCommands.text("costs.not_saved",
                    "The change applies until the server stops, but the settings file couldn't be written"));
        }
        return 1;
    }
}
