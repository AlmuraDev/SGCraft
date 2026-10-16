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

import com.almuradev.sgcraft.mod.tool.ExplorerData;
import com.almuradev.sgcraft.mod.tool.ExplorerDialer;
import com.almuradev.sgcraft.mod.tool.ExplorerPayloads;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * What players have found with the explorer dialer, without one in hand.
 *
 * <ul>
 *     <li>{@code /sgcraft explorer} lists every generated gate, and which the caller has visited.</li>
 *     <li>{@code /sgcraft explorer <player>} lists them for another player, for operators.</li>
 *     <li>{@code /sgcraft explorer forget <player>} forgets a player's visits and names, for operators.</li>
 * </ul>
 */
final class SGExplorerCommands {

    private SGExplorerCommands() {
    }

    static ArgumentBuilder<CommandSourceStack, ?> explorer() {
        return Commands.literal("explorer")
                .executes(context -> SGExplorerCommands.list(context.getSource(),
                        context.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> SGExplorerCommands.list(context.getSource(),
                                EntityArgument.getPlayer(context, "player"))))
                .then(Commands.literal("forget").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    final ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                    ExplorerData.forget(context.getSource().getServer(), player.getUUID());
                                    context.getSource().sendSuccess(() -> SGCommands.text("explorer.forgot",
                                            "Forgot what %s has found", player.getName()), true);
                                    return 1;
                                })));
    }

    private static int list(final CommandSourceStack source, final ServerPlayer player)
            throws CommandSyntaxException {
        final List<ExplorerPayloads.Listed> gates = ExplorerDialer.entries(player);
        final long visited = gates.stream().filter(ExplorerPayloads.Listed::visited).count();
        source.sendSuccess(() -> SGCommands.text("explorer.header", "%s has visited %s of %s generated gates:",
                player.getName(), visited, gates.size()), false);
        for (final ExplorerPayloads.Listed gate : gates) {
            source.sendSuccess(() -> Component.literal("  ")
                    .append(Component.literal(gate.address().isEmpty() ? "????-???-??" : gate.address())
                            .withStyle(ChatFormatting.AQUA))
                    .append(" " + gate.world())
                    .append(gate.name().isEmpty() ? "" : " \"" + gate.name() + "\"")
                    .append(" ").append(gate.visited()
                            ? SGCommands.text("explorer.visited", "visited").withStyle(ChatFormatting.GREEN)
                            : SGCommands.text("explorer.not_visited", "not visited")
                                    .withStyle(ChatFormatting.DARK_GRAY)), false);
        }
        return gates.size();
    }
}
