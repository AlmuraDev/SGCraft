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

import com.almuradev.sgcraft.worldgen.StructureGate;
import com.almuradev.sgcraft.worldgen.StructureGates;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Map;

/**
 * {@code /sgcraft worldgen}: the gates added beside generated structures. On its own it lists each structure with
 * its gate's chance; {@code always <true|false>} gives every such structure a gate until the server stops, for
 * trying them out with {@code /place structure}.
 */
final class SGWorldgenCommands {

    private SGWorldgenCommands() {
    }

    static ArgumentBuilder<CommandSourceStack, ?> worldgen() {
        return Commands.literal("worldgen")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> {
                    final Map<Identifier, StructureGate> gates = StructureGates.structures();
                    if (gates.isEmpty()) {
                        context.getSource().sendSuccess(() -> SGCommands.text("worldgen.none",
                                "No structures are set to have gates"), false);
                        return 0;
                    }
                    gates.forEach((structure, gate) -> context.getSource().sendSuccess(() -> Component.literal("  ")
                            .append(Component.literal(structure.toString()).withStyle(ChatFormatting.AQUA))
                            .append(" ").append(SGCommands.text("worldgen.entry", "%s%% from %s",
                                    StructureGates.always() ? 100 : gate.chance(), gate.template().toString())),
                            false));
                    return gates.size();
                })
                .then(Commands.literal("always")
                        .then(Commands.argument("always", BoolArgumentType.bool())
                                .executes(context -> SGWorldgenCommands.always(context.getSource(),
                                        BoolArgumentType.getBool(context, "always")))));
    }

    private static int always(final CommandSourceStack source, final boolean always) {
        StructureGates.always(always);
        final Component said = always
                ? SGCommands.text("worldgen.always",
                        "Every structure set to have a gate gets one until the server stops")
                : SGCommands.text("worldgen.by_chance", "Structures get gates by their chance again");
        source.sendSuccess(() -> said, true);
        return 1;
    }
}
