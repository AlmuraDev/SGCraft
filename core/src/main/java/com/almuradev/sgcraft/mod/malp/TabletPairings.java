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

import com.almuradev.sgcraft.mod.SGCraft;
import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Which MALPs each MALP tablet was paired with, by the tablet's own ID, as the server paired them. A tablet's list on
 * the item only says what to show; a MALP on it is watched and driven only while the server paired that tablet with
 * it, so a list a player in creative mode writes into a tablet reaches no one's MALP. Kept with the world.
 */
public final class TabletPairings extends SavedData {

    private static final Codec<TabletPairings> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC,
            UUIDUtil.CODEC.listOf()).xmap(TabletPairings::new, pairings -> {
                final Map<UUID, List<UUID>> saved = new HashMap<>();
                pairings.tablets.forEach((tablet, malps) -> saved.put(tablet, new ArrayList<>(malps)));
                return saved;
            });

    private static final SavedDataType<TabletPairings> TYPE = new SavedDataType<>(SGCraft.id("malp_tablets"),
            TabletPairings::new, TabletPairings.CODEC, null);

    // The MALPs each tablet was paired with, by the tablet's ID
    private final Map<UUID, Set<UUID>> tablets = new HashMap<>();

    private TabletPairings() {
    }

    private TabletPairings(final Map<UUID, List<UUID>> saved) {
        saved.forEach((tablet, malps) -> this.tablets.put(tablet, new HashSet<>(malps)));
    }

    /** @return the server's pairings */
    public static TabletPairings get(final MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TabletPairings.TYPE);
    }

    /** @return whether the server paired a tablet with a MALP */
    public boolean paired(final UUID tablet, final UUID malp) {
        final Set<UUID> malps = this.tablets.get(tablet);
        return malps != null && malps.contains(malp);
    }

    /** Pairs a tablet with a MALP. */
    public void pair(final UUID tablet, final UUID malp) {
        if (this.tablets.computeIfAbsent(tablet, id -> new HashSet<>()).add(malp)) {
            this.setDirty();
        }
    }

    /** Takes a MALP off a tablet's pairings. */
    public void unpair(final UUID tablet, final UUID malp) {
        final Set<UUID> malps = this.tablets.get(tablet);
        if (malps != null && malps.remove(malp)) {
            if (malps.isEmpty()) {
                this.tablets.remove(tablet);
            }
            this.setDirty();
        }
    }
}
