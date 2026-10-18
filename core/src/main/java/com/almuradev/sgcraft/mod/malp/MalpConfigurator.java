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

package com.almuradev.sgcraft.mod.malp;

import com.almuradev.sgcraft.mod.entity.MalpEntity;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.ConfigApply;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.ConfigState;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.ConfigUnpair;
import com.almuradev.sgcraft.mod.network.SGNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * What the server does for a configurator used on a MALP: it opens the MALP's settings screen for its owner and for
 * operators, and makes the changes that screen asks for, unpairing the MALP from its console among them, checking
 * again each time that the player may and that the MALP is still within their reach. Anyone else is told they may
 * not.
 */
public final class MalpConfigurator {

    // How much further than their reach a player may step back from the MALP with its screen open
    private static final double SLACK = 4;

    private MalpConfigurator() {
    }

    /** @return whether a player may change a MALP's settings: they own it, or they are an operator */
    public static boolean mayConfigure(final Player player, final MalpEntity malp) {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)
                || player.getUUID().equals(malp.owner());
    }

    /** A player used a configurator on a MALP: its settings screen opens, if they may change them. */
    public static void open(final ServerPlayer player, final MalpEntity malp) {
        if (!MalpConfigurator.mayConfigure(player, malp)) {
            MalpConfigurator.refuse(player);
            return;
        }
        MalpConfigurator.send(player, malp, true, false);
    }

    /** Makes the changes a MALP's settings screen asks for, if the player may, and answers with the settings. */
    public static void apply(final ServerPlayer player, final ConfigApply apply) {
        final MalpEntity malp = MalpConfigurator.target(player, apply.malp());
        if (malp == null) {
            return;
        }
        if (!apply.refresh()) {
            malp.setOptions(apply.options());
            apply.name().filter(name -> !name.strip().equals(malp.displayName())).ifPresent(malp::rename);
        }
        MalpConfigurator.send(player, malp, false, !apply.refresh());
    }

    /**
     * Unpairs a MALP from its console, as its settings screen asks, if the player may, and answers with the settings.
     * It is unpaired as the {@code sgcraft malp unpair} command unpairs it.
     */
    public static void unpair(final ServerPlayer player, final ConfigUnpair unpair) {
        final MalpEntity malp = MalpConfigurator.target(player, unpair.malp());
        if (malp == null) {
            return;
        }
        MalpLinks.unpair(malp);
        MalpConfigurator.send(player, malp, false, false);
    }

    // The MALP a player's settings screen is for, if it is still within their reach and they may change its settings;
    // otherwise they are told why not
    private static @Nullable MalpEntity target(final ServerPlayer player, final UUID id) {
        final MalpEntity malp = MalpRegistry.find(player.level().getServer(), id);
        if (malp == null || !malp.isAlive() || malp.level() != player.level()
                || !player.isWithinEntityInteractionRange(malp, MalpConfigurator.SLACK)) {
            player.sendOverlayMessage(Component.translatableWithFallback("message.sgcraft.malp.not_near",
                    "That MALP isn't near you").withStyle(ChatFormatting.RED));
            return null;
        }
        if (!MalpConfigurator.mayConfigure(player, malp)) {
            MalpConfigurator.refuse(player);
            return null;
        }
        return malp;
    }

    /** @return the name a player had when last seen, or their UUID if they never were */
    public static String ownerName(final MinecraftServer server, final UUID owner) {
        return server.services().nameToIdCache().get(owner).map(NameAndId::name).orElse(owner.toString());
    }

    private static void refuse(final ServerPlayer player) {
        player.sendOverlayMessage(Component.translatableWithFallback("message.sgcraft.malp.cannot_configure",
                "You aren't allowed to configure this MALP").withStyle(ChatFormatting.RED));
    }

    private static void send(final ServerPlayer player, final MalpEntity malp, final boolean open,
            final boolean saved) {
        final Optional<String> owner = Optional.ofNullable(malp.owner())
                .map(id -> MalpConfigurator.ownerName(player.level().getServer(), id));
        SGNetwork.send(player, new ConfigState(open, malp.getUUID(), malp.displayName(), owner,
                Optional.ofNullable(malp.console()), malp.options(), saved));
    }
}
