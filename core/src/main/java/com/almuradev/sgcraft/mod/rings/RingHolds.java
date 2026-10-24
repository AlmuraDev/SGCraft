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

package com.almuradev.sgcraft.mod.rings;

import com.almuradev.sgcraft.mod.network.SGNetwork;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Players held still and safe through a ring transport. From the moment a platform's rings set off around them until
 * the receiver's are home again, the players inside can look around but not move, and take no damage, save from
 * {@code /kill}. Their clients stop their movement keys; the server puts them back where they are held should
 * anything else move them. A hold ends once its time is up, or as the player dies, leaves the world they were held in,
 * or leaves the server.
 */
public final class RingHolds {

    // How far a held player may drift from where they are held before they are put back, in blocks
    private static final double MAX_DRIFT = 0.1;

    private static final class Hold {

        private final ServerPlayer player;
        private final ResourceKey<Level> world;
        // Where they are held: where they stood, then where they arrived
        private Vec3 at;
        private int ticksLeft;

        private Hold(final ServerPlayer player, final int ticks) {
            this.player = player;
            this.world = player.level().dimension();
            this.at = player.position();
            this.ticksLeft = ticks;
        }
    }

    // By player
    private static final Map<UUID, Hold> HOLDS = new ConcurrentHashMap<>();

    private RingHolds() {
    }

    /** Holds the players inside a platform's rings for some ticks, from where they stand now. */
    static void hold(final RingPlatformBlockEntity platform, final int ticks) {
        if (platform.getLevel() == null) {
            return;
        }
        for (final ServerPlayer player : platform.getLevel().getEntitiesOfClass(ServerPlayer.class,
                platform.volume(), ServerPlayer::isAlive)) {
            if (RingHolds.HOLDS.put(player.getUUID(), new Hold(player, ticks)) == null) {
                SGNetwork.send(player, new RingPayloads.Hold(true));
            }
        }
    }

    /** A held player has been moved by the transport, and is held where they arrived. */
    static void moved(final Entity entity) {
        final Hold hold = RingHolds.HOLDS.get(entity.getUUID());
        if (hold != null && hold.player == entity) {
            hold.at = entity.position();
        }
    }

    /** @return whether a ring transport holds a player now */
    public static boolean held(final Entity entity) {
        final Hold hold = RingHolds.HOLDS.get(entity.getUUID());
        return hold != null && hold.player == entity;
    }

    /** @return whether a held player is kept from some damage: all of it, save {@code /kill} */
    public static boolean shields(final ServerPlayer player, final DamageSource source) {
        return RingHolds.held(player) && !source.is(DamageTypes.GENERIC_KILL);
    }

    /** Keeps held players where they are held, facing however they like, and lets them go once it is over. */
    public static void tick(final MinecraftServer server) {
        for (final Hold hold : RingHolds.HOLDS.values()) {
            final ServerPlayer player = hold.player;
            if (--hold.ticksLeft <= 0 || player.isRemoved() || !player.isAlive() || player.hasDisconnected()
                    || !player.level().dimension().equals(hold.world)) {
                RingHolds.release(hold);
                continue;
            }
            player.setDeltaMovement(Vec3.ZERO);
            player.resetFallDistance();
            player.clearFire();
            if (player.position().distanceToSqr(hold.at) > RingHolds.MAX_DRIFT * RingHolds.MAX_DRIFT) {
                // Turned by nothing, so the way they look is left alone
                player.connection.teleport(new PositionMoveRotation(hold.at, Vec3.ZERO, 0, 0), Relative.ROTATION);
            }
        }
    }

    private static void release(final Hold hold) {
        if (RingHolds.HOLDS.remove(hold.player.getUUID(), hold) && !hold.player.hasDisconnected()) {
            SGNetwork.send(hold.player, new RingPayloads.Hold(false));
        }
    }

    /** Forgets a player's hold, as they leave the server. */
    public static void forget(final UUID player) {
        RingHolds.HOLDS.remove(player);
    }

    /** Forgets every hold, as the server stops. */
    public static void clear() {
        RingHolds.HOLDS.clear();
    }
}
