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

import com.almuradev.sgcraft.mod.view.RemoteViews;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * {@code /sgcraft view list}: who is watching a remote view, and what, and on which display if it isn't their own
 * view; {@code stop <players>} stops every view those players have.
 */
final class SGViewCommands {

    private SGViewCommands() {
    }

    static ArgumentBuilder<CommandSourceStack, ?> view() {
        return Commands.literal("view")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("list")
                        .executes(context -> SGViewCommands.list(context.getSource())))
                .then(Commands.literal("stop")
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(context -> SGViewCommands.stop(context.getSource(),
                                        EntityArgument.getPlayers(context, "players")))));
    }

    private static int list(final CommandSourceStack source) {
        final List<RemoteViews.Watch> views = RemoteViews.views();
        if (views.isEmpty()) {
            source.sendSuccess(() -> SGCommands.text("view.none", "No one is watching a remote view"), false);
            return 0;
        }
        for (final RemoteViews.Watch view : views) {
            final ServerPlayer viewer = source.getServer().getPlayerList().getPlayer(view.viewer());
            final Entity watched = view.world().getEntity(view.source());
            final Component who = viewer != null ? viewer.getDisplayName()
                    : Component.literal(view.viewer().toString());
            final Component what = watched != null ? watched.getDisplayName()
                    : Component.literal(view.source().toString());
            final String where = watched != null ? watched.blockPosition().toShortString() : "?";
            final String world = view.world().dimension().identifier().toString();
            source.sendSuccess(() -> view.display()
                    .map(display -> SGCommands.text("view.watching_display",
                            "%s is watching %s in %s at %s on the display at %s", who, what, world, where,
                            display.toShortString()))
                    .orElseGet(() -> SGCommands.text("view.watching", "%s is watching %s in %s at %s", who, what,
                            world, where)), false);
        }
        return views.size();
    }

    private static int stop(final CommandSourceStack source, final Collection<ServerPlayer> players) {
        int stopped = 0;
        for (final ServerPlayer player : players) {
            final UUID id = player.getUUID();
            if (RemoteViews.views().stream().anyMatch(view -> view.viewer().equals(id))) {
                RemoteViews.stopAll(player);
                stopped++;
            }
        }
        final int count = stopped;
        source.sendSuccess(() -> SGCommands.text("view.stopped", "Stopped %s remote views", count), true);
        return count;
    }
}
