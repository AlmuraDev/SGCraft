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

package com.almuradev.sgcraft.mod.tool;

import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.SGServer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * What each player has found with the explorer dialer, as in SGCraft 2.2.0: the generated gates they have visited,
 * and the names they have given them. It is kept with the world, by player, so it lasts through death.
 *
 * <p>A player visits a generated gate by using an explorer dialer at it, or by going through it, either way.</p>
 */
public final class ExplorerData {

    /** The longest name a player can give a gate. */
    public static final int MAX_NAME_LENGTH = 40;

    private ExplorerData() {
    }

    /** Marks a gate as visited by a player, if world generation built it. */
    public static void visit(final ServerPlayer player, final GlobalPos gate) {
        final boolean generated = SGServer.current().flatMap(server -> server.stargates().gate(gate))
                .flatMap(found -> found.get(GateProperties.GENERATED)).orElse(false);
        if (!generated) {
            return;
        }
        final Store store = ExplorerData.store(player.level().getServer());
        if (store.of(player.getUUID()).visited.add(gate)) {
            store.setDirty();
        }
    }

    /** @return the generated gates a player has visited */
    public static Set<GlobalPos> visited(final MinecraftServer server, final UUID player) {
        return Set.copyOf(ExplorerData.store(server).of(player).visited);
    }

    /** @return the name a player gave a gate, if any */
    public static Optional<String> name(final MinecraftServer server, final UUID player, final GlobalPos gate) {
        return Optional.ofNullable(ExplorerData.store(server).of(player).names.get(gate));
    }

    /** Names a gate for a player, cut to {@link #MAX_NAME_LENGTH}; a blank name takes the name off. */
    public static void rename(final MinecraftServer server, final UUID player, final GlobalPos gate,
            final String name) {
        final Store store = ExplorerData.store(server);
        final String kept = name.strip();
        if (kept.isEmpty()) {
            store.of(player).names.remove(gate);
        } else {
            store.of(player).names.put(gate, kept.substring(0, Math.min(kept.length(), ExplorerData.MAX_NAME_LENGTH)));
        }
        store.setDirty();
    }

    /** Forgets everything a player has found: their visits and their names for gates. */
    public static void forget(final MinecraftServer server, final UUID player) {
        final Store store = ExplorerData.store(server);
        if (store.players.remove(player) != null) {
            store.setDirty();
        }
    }

    private static Store store(final MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(Store.TYPE);
    }

    // One player's finds
    private static final class Finds {

        private static final Codec<Map.Entry<GlobalPos, String>> NAME_CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        GlobalPos.CODEC.fieldOf("gate").forGetter(Map.Entry::getKey),
                        Codec.STRING.fieldOf("name").forGetter(Map.Entry::getValue)
                ).apply(instance, Map::entry));

        static final Codec<Finds> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                GlobalPos.CODEC.listOf().optionalFieldOf("visited", List.of())
                        .forGetter(finds -> new ArrayList<>(finds.visited)),
                Finds.NAME_CODEC.listOf().optionalFieldOf("names", List.of())
                        .forGetter(finds -> new ArrayList<>(finds.names.entrySet()))
        ).apply(instance, Finds::new));

        private final Set<GlobalPos> visited = new HashSet<>();
        private final Map<GlobalPos, String> names = new HashMap<>();

        Finds() {
        }

        Finds(final List<GlobalPos> visited, final List<Map.Entry<GlobalPos, String>> names) {
            this.visited.addAll(visited);
            names.forEach(entry -> this.names.put(entry.getKey(), entry.getValue()));
        }
    }

    // Every player's finds, as saved
    static final class Store extends SavedData {

        private static final Codec<Store> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Finds.CODEC)
                .xmap(Store::new, store -> store.players);

        static final SavedDataType<Store> TYPE = new SavedDataType<>(SGCraft.id("explorer"), Store::new,
                Store.CODEC, null);

        private final Map<UUID, Finds> players = new HashMap<>();

        private Store() {
        }

        private Store(final Map<UUID, Finds> players) {
            this.players.putAll(players);
        }

        Finds of(final UUID player) {
            return this.players.computeIfAbsent(player, id -> new Finds());
        }
    }
}
