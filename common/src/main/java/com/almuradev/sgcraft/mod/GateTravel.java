/*
 * This file is part of SGCraft, licensed under the MIT License (MIT).
 * It is based on the original SGCraft by gcewing.
 *
 * Copyright (c) AlmuraDev <https://github.com/AlmuraDev>
 * Copyright (c) 2016 gcewing <https://github.com/gcewing>
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

package com.almuradev.sgcraft.mod;

import com.almuradev.sgcraft.SGCraft;
import com.almuradev.sgcraft.api.access.GateRight;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.block.RingBlock;
import com.almuradev.sgcraft.block.StargateBaseBlock;
import com.almuradev.sgcraft.gate.GateFrame;
import com.almuradev.sgcraft.sound.SGSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * What passes through one end of a wormhole, as in the original SGCraft: travellers crossing its event horizon, and
 * the kawoosh as it opens.
 *
 * <p>A traveller crosses when the bottom of it passes from in front of the horizon to behind it, through the opening.
 * It is watched for two ticks, so its movement is known, and comes out of the other gate's front, turned to face out
 * of it, moving as it went in. Its riders go with it.</p>
 */
final class GateTravel {

    /** The damage the kawoosh does: {@code sgcraft:transient}. */
    static final ResourceKey<DamageType> KAWOOSH = ResourceKey.create(Registries.DAMAGE_TYPE, SGCraft.id("transient"));

    // The opening, in the gate's frame: the three blocks across and up inside the ring
    private static final double APERTURE_HALF_WIDTH = 1.5;
    private static final double APERTURE_BOTTOM = 0.5;
    private static final double APERTURE_TOP = 3.5;
    // How far either side of the horizon travellers are watched, how far behind it a crossing can be, and how far
    // from the opening a watched traveller can be and still be at this gate
    private static final double TRACKING_DEPTH = 1.5;
    private static final double MAX_CROSSING_DEPTH = 5;
    private static final double MAX_APPROACH = 4;
    // The kawoosh: the space it hurts, out from the base's front, and how hard it hits
    private static final Vec3 KAWOOSH_MIN = new Vec3(-1.5, 0.5, 0.5);
    private static final Vec3 KAWOOSH_MAX = new Vec3(1.5, 3.5, 5.5);
    private static final float KAWOOSH_DAMAGE = 5000;
    // The blocks it clears, in the gate's frame from the base block: three across, three up, and four deep from the
    // ring's own plane
    private static final int KAWOOSH_BLOCKS_DEEP = 3;

    private final SGStargate gate;
    private List<Tracked> tracked = new ArrayList<>();
    private boolean blocksCleared;

    GateTravel(final SGStargate gate) {
        this.gate = gate;
    }

    private record Tracked(Entity entity, Vec3 last) {
    }

    /**
     * Sends on whatever crossed the horizon since last tick, then watches what is near it now.
     *
     * @param other the gate at the other end
     * @param sends whether travellers may go from this end
     * @param reverseKills whether a traveller entering an end it may not go from dies
     */
    void tick(final SGStargate other, final boolean sends, final boolean reverseKills) {
        final ServerLevel level = this.gate.level();
        if (level == null) {
            this.tracked = new ArrayList<>();
            return;
        }
        final GateFrame frame = this.gate.frame();
        for (final Tracked t : this.tracked) {
            if (this.crossed(level, frame, t)) {
                if (sends) {
                    this.send(level, frame, t, other);
                } else if (reverseKills) {
                    GateTravel.reverse(t.entity);
                }
            }
        }
        final List<Tracked> next = new ArrayList<>();
        final Set<Entity> seen = new HashSet<>();
        final AABB box = GateTravel.trackingBox(frame);
        for (final Entity entity : level.getEntities((Entity) null, box, GateTravel::travels)) {
            if (seen.add(entity)) {
                next.add(new Tracked(entity, entity.position()));
            }
        }
        this.tracked = next;
    }

    /** Stops watching, such as when the wormhole closes. */
    void clear() {
        this.tracked = new ArrayList<>();
    }

    // Only the bottom of a stack of riders travels, taking the rest with it
    private static boolean travels(final Entity entity) {
        return entity.isAlive() && !entity.isPassenger() && !(entity instanceof FishingHook);
    }

    private static AABB trackingBox(final GateFrame frame) {
        return frame.box(
                new Vec3(-GateTravel.APERTURE_HALF_WIDTH, GateTravel.APERTURE_BOTTOM, -GateTravel.TRACKING_DEPTH),
                new Vec3(GateTravel.APERTURE_HALF_WIDTH, GateTravel.APERTURE_TOP, GateTravel.TRACKING_DEPTH));
    }

    // Whether it went from in front of the horizon to just behind it, through the opening. Where it was is taken a
    // tick further back than last seen, so a traveller already just behind the horizon when first seen still counts.
    private boolean crossed(final ServerLevel level, final GateFrame frame, final Tracked t) {
        final Entity entity = t.entity;
        final AABB near = GateTravel.trackingBox(frame).inflate(GateTravel.MAX_APPROACH);
        if (!entity.isAlive() || entity.level() != level || entity.isPassenger()
                || !near.intersects(entity.getBoundingBox())) {
            return false;
        }
        final Vec3 now = entity.position();
        final Vec3 before = t.last.scale(2).subtract(now);
        final Vec3 p0 = frame.toLocal(before);
        final Vec3 p1 = frame.toLocal(now);
        final Vec3 middle = frame.toLocal(now.add(0, entity.getBbHeight() / 2, 0));
        final boolean inOpening = Math.abs(middle.x) <= GateTravel.APERTURE_HALF_WIDTH + entity.getBbWidth() / 2;
        return inOpening && p0.z >= 0 && p1.z < 0 && p1.z > -GateTravel.MAX_CROSSING_DEPTH;
    }

    // Through to the other gate: mirrored across it, as though the two horizons were one, coming out of its front
    private void send(final ServerLevel level, final GateFrame frame, final Tracked t, final SGStargate other) {
        final Entity entity = t.entity;
        final ServerLevel destination = other.level();
        if (destination == null || !this.allowed(entity, other)) {
            return;
        }
        final GateFrame there = other.frame();
        final Vec3 p = frame.toLocal(entity.position());
        final Vec3 v = frame.directionToLocal(entity.position().subtract(t.last));
        final Vec3 position = there.toWorld(new Vec3(-p.x, p.y, -p.z));
        final Vec3 motion = there.directionToWorld(new Vec3(-v.x, v.y, -v.z));
        final Vec3 look = frame.directionToLocal(Vec3.directionFromRotation(0, entity.getYRot()));
        final Vec3 facing = there.directionToWorld(new Vec3(-look.x, 0, -look.z));
        final float yaw = (float) (Mth.atan2(-facing.x, facing.z) * Mth.RAD_TO_DEG);
        GateTravel.playTeleport(level, entity, entity.position());
        final Entity arrived = entity.teleport(new TeleportTransition(destination, position, motion, yaw,
                entity.getXRot(), TeleportTransition.DO_NOTHING));
        if (arrived != null) {
            GateTravel.playTeleport(destination, arrived, position);
        }
    }

    // Every player in the stack must be allowed to use both gates
    private boolean allowed(final Entity entity, final SGStargate other) {
        for (final Entity rider : entity.getPassengersAndSelf().toList()) {
            if (rider instanceof ServerPlayer player && (!this.gate.access().allows(player.getUUID(), GateRight.USE)
                    || !other.access().allows(player.getUUID(), GateRight.USE))) {
                player.sendSystemMessage(Component.translatableWithFallback("message.sgcraft.access_denied",
                        "You aren't allowed to travel through this gate").withStyle(ChatFormatting.RED));
                return false;
            }
        }
        return true;
    }

    // Entering an end that only receives
    private static void reverse(final Entity entity) {
        for (final Entity rider : entity.getPassengersAndSelf().toList()) {
            if (rider instanceof ServerPlayer player && player.isCreative()) {
                player.sendSystemMessage(Component.translatableWithFallback("message.sgcraft.reverse_wormhole",
                        "You can't travel backwards through a one way wormhole").withStyle(ChatFormatting.RED));
                continue;
            }
            if (rider.level() instanceof ServerLevel level) {
                rider.hurtServer(level, GateTravel.kawoosh(level), Float.MAX_VALUE);
                if (rider.isAlive() && !(rider instanceof LivingEntity)) {
                    rider.discard();
                }
            }
        }
    }

    // Small travellers sound quieter and higher
    private static void playTeleport(final ServerLevel level, final Entity entity, final Vec3 at) {
        final float size = Math.min(entity.getBbWidth() * entity.getBbHeight(), 1);
        level.playSound(null, at.x, at.y, at.z, SGSounds.TELEPORT, SoundSource.BLOCKS, size, 2 - size);
    }

    /**
     * The kawoosh, for a tick of the wormhole opening: whatever living is in front of the gate is torn apart, and the
     * first tick clears the blocks in its way. Only when the gate's {@link GateProperties#OPENING_DAMAGE} is on.
     */
    void kawoosh() {
        final ServerLevel level = this.gate.level();
        if (level == null || !this.gate.get(GateProperties.OPENING_DAMAGE).orElse(true)) {
            return;
        }
        final GateFrame frame = this.gate.frame();
        final AABB box = frame.box(GateTravel.KAWOOSH_MIN, GateTravel.KAWOOSH_MAX);
        final DamageSource source = GateTravel.kawoosh(level);
        for (final LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
            entity.hurtServer(level, source, GateTravel.KAWOOSH_DAMAGE);
        }
        if (!this.blocksCleared) {
            this.blocksCleared = true;
            this.clearBlocks(level, frame);
        }
    }

    // Nearest first; never another gate's blocks, and nothing that can't be broken
    private void clearBlocks(final ServerLevel level, final GateFrame frame) {
        for (int z = 0; z <= GateTravel.KAWOOSH_BLOCKS_DEEP; z++) {
            for (int y = 1; y <= 3; y++) {
                for (int x = -1; x <= 1; x++) {
                    final BlockPos pos = frame.block(x, y, z);
                    final BlockState state = level.getBlockState(pos);
                    if (state.isAir() || state.getBlock() instanceof RingBlock
                            || state.getBlock() instanceof StargateBaseBlock || state.getDestroySpeed(level, pos) < 0) {
                        continue;
                    }
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    private static DamageSource kawoosh(final ServerLevel level) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(GateTravel.KAWOOSH));
    }
}
