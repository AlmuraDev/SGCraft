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

package com.almuradev.sgcraft.mod.obelisk;

import com.almuradev.sgcraft.mod.network.SGNetwork;
import com.almuradev.sgcraft.mod.item.SGItems;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * An Asgard obelisk's screen, opened with the configurator: where it is linked to, its owner, and who may use and
 * configure it. Only those who may configure it change anything.
 */
public final class ObeliskActions {

    /** The most players an obelisk's list holds. */
    public static final int MAX_ENTRIES = 64;
    // How near an obelisk its screen's user must stay, in blocks
    private static final double USE_RANGE = 8;

    private ObeliskActions() {
    }

    /** A player used a configurator on an obelisk: its screen opens, if they may configure it. */
    static void open(final ServerPlayer player, final AsgardObeliskBlockEntity obelisk) {
        if (!obelisk.mayConfigure(player)) {
            Obelisks.tell(player, "no_configure", "You aren't allowed to configure this obelisk", true);
            return;
        }
        ObeliskActions.send(player, obelisk, true, "");
    }

    /** Changes made on an obelisk's screen. */
    public static void apply(final ServerPlayer player, final ObeliskPayloads.Apply apply) {
        // Reach first: looking up a far position would load or generate its chunk
        if (!apply.obelisk().closerToCenterThan(player.position(), ObeliskActions.USE_RANGE)
                || !(player.level().getBlockEntity(apply.obelisk()) instanceof final AsgardObeliskBlockEntity obelisk)
                || !player.getMainHandItem().is(SGItems.CONFIGURATOR) && !player.getOffhandItem().is(
                        SGItems.CONFIGURATOR)) {
            return;
        }
        if (!obelisk.mayConfigure(player)) {
            Obelisks.tell(player, "no_configure", "You aren't allowed to configure this obelisk", true);
            return;
        }
        String message = "Changes saved";
        if (apply.unlink()) {
            Obelisks.unlinkPartner(obelisk);
        }
        final List<ObeliskAccess> entries = new ArrayList<>();
        for (final ObeliskPayloads.Entry entry : apply.entries()) {
            if (entries.size() < ObeliskActions.MAX_ENTRIES) {
                entries.add(new ObeliskAccess(entry.player(), entry.name(), entry.use(), entry.admin()));
            }
        }
        final String name = apply.addPlayer().strip();
        if (!name.isEmpty()) {
            // Only a player online now is added, so naming one never waits on a lookup
            final ServerPlayer added = player.level().getServer().getPlayerList().getPlayerByName(name);
            if (added == null) {
                message = "No player called " + name + " is online";
            } else if (entries.size() >= ObeliskActions.MAX_ENTRIES) {
                message = "An obelisk lists at most " + ObeliskActions.MAX_ENTRIES + " players";
            } else if (entries.stream().noneMatch(entry -> entry.player().equals(added.getUUID()))) {
                entries.add(new ObeliskAccess(added.getUUID(), added.getName().getString(), true, false));
            }
        }
        obelisk.setAccess(apply.everyone(), entries);
        ObeliskActions.send(player, obelisk, false, message);
    }

    private static void send(final ServerPlayer player, final AsgardObeliskBlockEntity obelisk, final boolean open,
            final String message) {
        final String link = obelisk.link().map(ObeliskActions::describe).orElse("");
        final List<ObeliskPayloads.Entry> entries = obelisk.access().stream().map(entry -> new ObeliskPayloads.Entry(
                entry.player(), entry.name(), entry.use(), entry.admin())).toList();
        SGNetwork.send(player, new ObeliskPayloads.State(open, obelisk.getBlockPos(), link, obelisk.ownerName(),
                obelisk.everyone(), entries, message));
    }

    private static String describe(final GlobalPos place) {
        return place.dimension().identifier() + " " + place.pos().toShortString();
    }
}
