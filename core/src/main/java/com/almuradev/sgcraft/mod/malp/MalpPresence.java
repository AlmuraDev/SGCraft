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
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Where the MALPs someone is watching stand, so that the world around a watched MALP behaves as it would around a
 * player standing there. Vanilla's rules for when a mob gives up wandering or despawns, and for where and how many
 * mobs spawn, measure from the nearest player; here a watched MALP nearer than any player is measured from instead.
 * Kept up to date once a tick.
 *
 * <p>A player has every chunk within their spawning range running, and their share of a world's mob cap is spread
 * over those chunks. A MALP keeps only a few chunks around it running, so its share of the cap is the share of those
 * chunks alone, and mobs spawn around it as thickly as around a player rather than crowding into them.</p>
 */
public final class MalpPresence {

    // How far from a chunk's middle a player, or a MALP, may be for the chunk to spawn mobs, squared, as vanilla
    private static final double SPAWNING_RANGE_SQR = 128 * 128;
    // How many chunks out from a MALP's own the chunks it spawns mobs in may lie
    private static final int SPAWNING_CHUNKS = 8;

    // Where each watched MALP stood at the start of this tick, by its world
    private static Map<ResourceKey<Level>, List<Vec3>> watched = Map.of();

    private MalpPresence() {
    }

    /** Takes where the watched MALPs stand this tick. */
    static void update(final MinecraftServer server, final Collection<UUID> malps) {
        if (malps.isEmpty()) {
            MalpPresence.watched = Map.of();
            return;
        }
        final Map<ResourceKey<Level>, List<Vec3>> now = new HashMap<>();
        for (final UUID id : malps) {
            final MalpEntity malp = MalpRegistry.find(server, id);
            if (malp != null && malp.isAlive()) {
                now.computeIfAbsent(malp.level().dimension(), key -> new ArrayList<>()).add(malp.position());
            }
        }
        MalpPresence.watched = now;
    }

    /** Forgets every MALP, as the server stops. */
    static void clear() {
        MalpPresence.watched = Map.of();
    }

    /**
     * @return the squared distance from a point to the nearest watched MALP in a world, or positive infinity if
     *     there is none
     */
    public static double nearestSqr(final Level level, final Vec3 point) {
        final List<Vec3> malps = MalpPresence.watched.get(level.dimension());
        if (malps == null) {
            return Double.POSITIVE_INFINITY;
        }
        double nearest = Double.POSITIVE_INFINITY;
        for (final Vec3 malp : malps) {
            nearest = Math.min(nearest, malp.distanceToSqr(point));
        }
        return nearest;
    }

    /** @return whether a watched MALP is near enough a chunk's middle for mobs to spawn in it, as a player would be */
    public static boolean nearForSpawning(final Level level, final ChunkPos chunk) {
        final List<Vec3> malps = MalpPresence.watched.get(level.dimension());
        if (malps == null) {
            return false;
        }
        for (final Vec3 malp : malps) {
            final double x = chunk.getMiddleBlockX() - malp.x;
            final double z = chunk.getMiddleBlockZ() - malp.z;
            if (x * x + z * z < MalpPresence.SPAWNING_RANGE_SQR) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the running chunks that spawn mobs because a watched MALP is near them, and no player is: those whose
     *     blocks tick, within the distance of a MALP a player's would be
     */
    public static List<LevelChunk> spawningChunks(final ServerLevel level) {
        final List<Vec3> malps = MalpPresence.watched.get(level.dimension());
        if (malps == null) {
            return List.of();
        }
        final LongSet seen = new LongOpenHashSet();
        final List<LevelChunk> chunks = new ArrayList<>();
        final ChunkMap map = level.getChunkSource().chunkMap;
        for (final Vec3 malp : malps) {
            final ChunkPos center = ChunkPos.containing(BlockPos.containing(malp));
            final int reach = MalpPresence.SPAWNING_CHUNKS;
            for (int x = center.x() - reach; x <= center.x() + reach; x++) {
                for (int z = center.z() - reach; z <= center.z() + reach; z++) {
                    final long key = ChunkPos.pack(x, z);
                    if (!seen.add(key) || !level.shouldTickBlocksAt(key)) {
                        continue;
                    }
                    final ChunkPos pos = new ChunkPos(x, z);
                    if (!MalpPresence.nearForSpawning(level, pos) || map.anyPlayerCloseEnoughForSpawning(pos)) {
                        continue;
                    }
                    final LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                    if (chunk != null) {
                        chunks.add(chunk);
                    }
                }
            }
        }
        return chunks;
    }

    /**
     * @return the squared distance from a point to whichever is nearer, a player in the same world or a watched
     *     MALP, for spawning a mob there
     */
    public static double spawnDistanceSqr(final ServerLevel level, final @Nullable Player player, final Vec3 point,
            final double playerDistanceSqr) {
        final double own = player != null && player.level() == level ? playerDistanceSqr : Double.POSITIVE_INFINITY;
        return Math.min(own, MalpPresence.nearestSqr(level, point));
    }

    /**
     * @return a player for vanilla's spawning to measure from where it found none in the world, if a watched MALP is
     *     there instead; its distance is then taken from the MALP by {@link #spawnDistanceSqr}
     */
    public static @Nullable Player standIn(final ServerLevel level, final Vec3 point) {
        if (MalpPresence.nearestSqr(level, point) == Double.POSITIVE_INFINITY) {
            return null;
        }
        final List<ServerPlayer> players = level.getServer().getPlayerList().getPlayers();
        return players.isEmpty() ? null : players.getFirst();
    }

    /**
     * Decides whether a mob despawns as vanilla does for a player at a distance: at once past its category's
     * despawn distance, now and then once it has been idle a while past its no-despawn distance, and never closer.
     * Within the no-despawn distance it counts as having just been near a player.
     *
     * @param distanceSqr the squared distance from the mob to the watched MALP nearest it
     */
    public static void checkDespawn(final Mob mob, final double distanceSqr) {
        final boolean persistent = mob.isPersistenceRequired() || mob.requiresCustomPersistence();
        final int despawn = mob.getType().getCategory().getDespawnDistance();
        if (!persistent && distanceSqr > despawn * despawn && mob.removeWhenFarAway(distanceSqr)) {
            mob.discard();
            return;
        }
        final int keep = mob.getType().getCategory().getNoDespawnDistance();
        if (!persistent && mob.getNoActionTime() > 600 && mob.getRandom().nextInt(800) == 0
                && distanceSqr > keep * keep && mob.removeWhenFarAway(distanceSqr)) {
            mob.discard();
        } else if (distanceSqr < keep * keep) {
            mob.setNoActionTime(0);
        }
    }
}
