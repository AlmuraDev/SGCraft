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

package com.almuradev.sgcraft.mod.computer;

import com.almuradev.sgcraft.mod.computer.ComputerPayloads.ConfigApply;
import com.almuradev.sgcraft.mod.computer.ComputerPayloads.ConfigState;
import com.almuradev.sgcraft.mod.malp.MalpConfigurator;
import com.almuradev.sgcraft.mod.network.SGNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * What the server does for a configurator used on a computer, a dialing computer or a MALP console: as for a MALP, it
 * opens the computer's settings screen for its owner, the player who placed it, and for operators, and makes the
 * changes that screen asks for, checking again each time that the player may and that the computer is still within
 * their reach. Anyone else is told they may not.
 */
public final class ComputerConfigurator {

    // How much further than their reach a player may step back from the computer with its screen open
    private static final double SLACK = 4;

    private ComputerConfigurator() {
    }

    /** @return whether a player may change a computer's settings: they own it, or they are an operator */
    public static boolean mayConfigure(final Player player, final PoweredComputer computer) {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)
                || player.getUUID().equals(computer.power().owner());
    }

    /**
     * A player used a configurator on a computer: its settings screen opens, if they may change them.
     *
     * @param computer the computer's middle lower part
     */
    public static void open(final ServerPlayer player, final BlockPos computer) {
        if (!(player.level().getBlockEntity(computer) instanceof final PoweredComputer powered)) {
            return;
        }
        if (!ComputerConfigurator.mayConfigure(player, powered)) {
            ComputerConfigurator.refuse(player);
            return;
        }
        ComputerConfigurator.send(player, computer, powered, true, false);
    }

    /** Makes the changes a computer's settings screen asks for, if the player may, and answers with the settings. */
    public static void apply(final ServerPlayer player, final ConfigApply apply) {
        final PoweredComputer computer = ComputerConfigurator.target(player, apply.computer());
        if (computer == null) {
            return;
        }
        if (!apply.refresh()) {
            computer.power().setRequiresPower(player.level(), apply.computer(), apply.requiresPower());
            // Switched as it was, which saves it, tells clients, and takes everyone off it if it no longer runs
            computer.setOn(computer.power().on());
        }
        ComputerConfigurator.send(player, apply.computer(), computer, false, !apply.refresh());
    }

    // The computer a player's settings screen is for, if it is still within their reach and they may change its
    // settings; otherwise they are told why not
    private static @Nullable PoweredComputer target(final ServerPlayer player, final BlockPos computer) {
        // Reach first: looking up a far position would load or generate its chunk
        if (!player.isWithinBlockInteractionRange(computer, ComputerConfigurator.SLACK)
                || !(player.level().getBlockEntity(computer) instanceof final PoweredComputer powered)) {
            player.sendOverlayMessage(Component.translatableWithFallback("message.sgcraft.computer.not_near",
                    "That computer isn't near you").withStyle(ChatFormatting.RED));
            return null;
        }
        if (!ComputerConfigurator.mayConfigure(player, powered)) {
            ComputerConfigurator.refuse(player);
            return null;
        }
        return powered;
    }

    private static void refuse(final ServerPlayer player) {
        player.sendOverlayMessage(Component.translatableWithFallback("message.sgcraft.computer.cannot_configure",
                "You aren't allowed to configure this computer").withStyle(ChatFormatting.RED));
    }

    private static void send(final ServerPlayer player, final BlockPos at, final PoweredComputer computer,
            final boolean open, final boolean saved) {
        final Optional<String> owner = Optional.ofNullable(computer.power().owner())
                .map(id -> MalpConfigurator.ownerName(player.level().getServer(), id));
        SGNetwork.send(player, new ConfigState(open, at, owner, computer.power().requiresPower(),
                ComputerSettings.current().requiresPower(), saved));
    }
}
