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
import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.access.GateRight;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.block.RingBlock;
import com.almuradev.sgcraft.block.StargateBaseBlock;
import com.almuradev.sgcraft.gate.GateFrame;
import com.almuradev.sgcraft.gate.GateShape;
import com.almuradev.sgcraft.item.SGItems;
import com.almuradev.sgcraft.mod.event.SGEventBus;
import com.almuradev.sgcraft.mod.event.SGEvents;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * What passes through one end of a wormhole, as in the original SGCraft: travelers crossing its event horizon, and
 * the kawoosh as it opens.
 *
 * <p>A traveler crosses when the bottom of it passes from in front of the horizon to behind it, through the opening.
 * It is watched for two ticks, so its movement is known, and comes out of the other gate's front, turned to face out
 * of it, moving as it went in. Its riders go with it.</p>
 */
final class GateTravel {

    /** The damage the kawoosh does: {@code sgcraft:transient}. */
    static final ResourceKey<DamageType> KAWOOSH = ResourceKey.create(Registries.DAMAGE_TYPE, SGCraft.id("transient"));
    /** The damage of hitting a shut iris: {@code sgcraft:iris}. */
    static final ResourceKey<DamageType> IRIS = ResourceKey.create(Registries.DAMAGE_TYPE, SGCraft.id("iris"));

    // The opening, in the gate's frame: the three blocks across and up inside the ring
    // How far either side of the horizon travelers are watched, how far behind it a crossing can be, and how far
    // from the opening a watched traveler can be and still be at this gate
    private static final double TRACKING_DEPTH = 1.5;
    private static final double MAX_CROSSING_DEPTH = 5;
    private static final double MAX_APPROACH = 4;
    // The blocks it clears, in the gate's frame from the base block: three across, three up, and four deep from the
    // ring's own plane

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
     * @param sends whether travelers may go from this end
     * @param reverseKills whether a traveler entering an end it may not go from dies
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
                    GateTravel.reverse(t.entity, this.gate);
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
        final GateShape shape = frame.shape();
        return frame.box(new Vec3(-shape.openingHalfWidth(), shape.openingFloor(), -GateTravel.TRACKING_DEPTH),
                new Vec3(shape.openingHalfWidth(), shape.openingTop(), GateTravel.TRACKING_DEPTH));
    }

    // Whether it went from in front of the horizon to just behind it, through the opening. Where it was is taken a
    // tick further back than last seen, so a traveler already just behind the horizon when first seen still counts.
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
        // A supergate's opening is the circle inside its drawn ring, which a traveler's middle must be in
        final boolean inOpening = frame.shape().isSupergate() ? frame.shape().inOpening(middle.x, middle.y)
                : Math.abs(middle.x) <= frame.shape().openingHalfWidth() + entity.getBbWidth() / 2;
        final double deepest = Math.max(GateTravel.MAX_CROSSING_DEPTH, frame.shape().height());
        return inOpening && p0.z >= 0 && p1.z < 0 && p1.z > -deepest;
    }

    // Through to the other gate: mirrored across it, as though the two horizons were one, coming out of its front
    private void send(final ServerLevel level, final GateFrame frame, final Tracked t, final SGStargate other) {
        final Entity entity = t.entity;
        if (other.level() == null || !this.allowed(entity, other)) {
            return;
        }
        final Cause cause = Cause.of(entity);
        final SGEvents.TravelPre pre = SGEventBus.get().post(new SGEvents.TravelPre(cause, this.gate, entity, other));
        if (pre.isCanceled()) {
            return;
        }
        final SGStargate to = pre.destination() == other ? other : this.gate.stargates()
                .gate(pre.destination().position()).orElse(null);
        final ServerLevel destination = to == null ? null : to.level();
        if (to == null || destination == null) {
            return;
        }
        if (to.irisBlocks() && !GateTravel.phases(entity, to)) {
            GateTravel.splat(level, entity, to);
            return;
        }
        final GateFrame there = to.frame();
        final Vec3 p = frame.toLocal(entity.position());
        final Vec3 v = frame.directionToLocal(entity.position().subtract(t.last));
        final Vec3 position = there.toWorld(GateTravel.arrival(entity, p, frame.shape(), there.shape()));
        final Vec3 motion = there.directionToWorld(new Vec3(-v.x, v.y, -v.z));
        final Vec3 look = frame.directionToLocal(Vec3.directionFromRotation(0, entity.getYRot()));
        final Vec3 facing = there.directionToWorld(new Vec3(-look.x, 0, -look.z));
        final float yaw = (float) (Mth.atan2(-facing.x, facing.z) * Mth.RAD_TO_DEG);
        GateTravel.playTeleport(level, entity, entity.position());
        final Entity arrived = entity.teleport(new TeleportTransition(destination, position, motion, yaw,
                entity.getXRot(), TeleportTransition.DO_NOTHING));
        if (arrived != null) {
            GateTravel.playTeleport(destination, arrived, position);
            SGEventBus.get().post(new SGEvents.TravelPost(cause, this.gate, arrived, to));
        }
    }

    // Where a traveler comes out, in the far gate's frame: mirrored across the horizon, as though the two were one.
    // Into a supergate, or between gates of different sizes, it goes inside the far opening as it looks, as in the
    // original: a player on foot comes out standing on its floor, anything else at the same place in the opening,
    // scaled to its size.
    private static Vec3 arrival(final Entity entity, final Vec3 feet, final GateShape from, final GateShape to) {
        double x = -feet.x;
        double y = feet.y;
        if (from != to || to.isSupergate()) {
            final boolean onFoot = entity instanceof Player player && !player.getAbilities().flying;
            final double height = entity.getBbHeight();
            final double scale = to.openingRadius() / from.openingRadius();
            final double floor = to.openingFloor();
            final double middle = feet.y + height / 2;
            y = onFoot ? floor : to.centerY() + (middle - from.centerY()) * scale - height / 2;
            y = Mth.clamp(y, floor, Math.max(floor, to.openingTop() - height));
            final double half = Math.max(0, to.openingHalfWidthAt(y + height / 2) - entity.getBbWidth() / 2);
            x = Mth.clamp(x * (onFoot ? 1 : scale), -half, half);
        }
        return new Vec3(x, y, -feet.z);
    }

    // Whether a traveler goes through the far gate's shut iris: one holding a Tollan phase shift device in either
    // hand does, where the gate lets it; the original took it only in the off hand
    private static boolean phases(final Entity entity, final SGStargate to) {
        return entity instanceof LivingEntity living && (living.getOffhandItem().is(SGItems.TOLLAN_PHASE_SHIFT_DEVICE)
                || living.getMainHandItem().is(SGItems.TOLLAN_PHASE_SHIFT_DEVICE))
                && to.get(GateProperties.PHASE_SHIFT_PASSES_IRIS).orElse(true);
    }

    // A traveler meeting a shut iris at the far end, as in the original: creative players are only told, anything
    // else dies against it
    private static void splat(final ServerLevel level, final Entity entity, final SGStargate to) {
        to.play(SGSounds.IRIS_HIT);
        GateTravel.playTeleport(level, entity, entity.position());
        for (final Entity rider : entity.getPassengersAndSelf().toList()) {
            if (rider instanceof ServerPlayer player && player.isCreative()) {
                player.sendSystemMessage(Component.translatableWithFallback("message.sgcraft.iris_at_destination",
                        "Destination blocked by iris").withStyle(ChatFormatting.RED));
                continue;
            }
            if (rider.level() instanceof ServerLevel riderLevel) {
                GateTravel.loseInventory(riderLevel, rider, to);
                rider.hurtServer(riderLevel, new DamageSource(riderLevel.registryAccess()
                        .lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(GateTravel.IRIS)), Float.MAX_VALUE);
                if (rider.isAlive() && !(rider instanceof LivingEntity)) {
                    rider.discard();
                }
            }
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
    private static void reverse(final Entity entity, final SGStargate gate) {
        for (final Entity rider : entity.getPassengersAndSelf().toList()) {
            if (rider instanceof ServerPlayer player && player.isCreative()) {
                player.sendSystemMessage(Component.translatableWithFallback("message.sgcraft.reverse_wormhole",
                        "You can't travel backwards through a one way wormhole").withStyle(ChatFormatting.RED));
                continue;
            }
            if (rider.level() instanceof ServerLevel level) {
                GateTravel.loseInventory(level, rider, gate);
                rider.hurtServer(level, GateTravel.kawoosh(level), Float.MAX_VALUE);
                if (rider.isAlive() && !(rider instanceof LivingEntity)) {
                    rider.discard();
                }
            }
        }
    }

    // A player the gate kills loses what they carry rather than dropping it, as in the original, unless the gate or
    // the keepInventory game rule says to keep it
    private static void loseInventory(final ServerLevel level, final Entity rider, final SGStargate gate) {
        if (rider instanceof ServerPlayer player && !gate.get(GateProperties.PRESERVE_INVENTORY).orElse(false)
                && !level.getGameRules().get(GameRules.KEEP_INVENTORY)) {
            player.getInventory().clearContent();
        }
    }

    // Small travelers sound quieter and higher
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
        // A shut iris keeps the kawoosh in
        if (level == null || !this.gate.get(GateProperties.OPENING_DAMAGE).orElse(true) || this.gate.irisBlocks()) {
            return;
        }
        final GateFrame frame = this.gate.frame();
        final AABB box = frame.box(frame.shape().kawooshMin(), frame.shape().kawooshMax());
        final DamageSource source = GateTravel.kawoosh(level);
        // How hard it hits is the gate's; the space it hurts and the blocks it clears come from its shape
        final float damage = this.gate.get(GateProperties.OPENING_DAMAGE_AMOUNT).orElse(5000.0).floatValue();
        for (final LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
            entity.hurtServer(level, source, damage);
        }
        if (!this.blocksCleared) {
            this.blocksCleared = true;
            this.clearBlocks(level, frame);
        }
    }

    // Nearest first, up to the most a kawoosh clears; never another gate's blocks, and nothing that can't be broken
    private void clearBlocks(final ServerLevel level, final GateFrame frame) {
        final GateShape shape = frame.shape();
        int cleared = 0;
        for (int z = 0; z <= shape.kawooshBlocksDeep(); z++) {
            for (int y = 1; y < shape.height() - 1; y++) {
                for (int x = -shape.radius(); x <= shape.radius(); x++) {
                    if (!shape.inKawooshBore(x, y) || cleared >= shape.maxKawooshBlocks()) {
                        continue;
                    }
                    final BlockPos pos = frame.block(x, y, z);
                    final BlockState state = level.getBlockState(pos);
                    if (state.isAir() || state.getBlock() instanceof LiquidBlock
                            || state.getBlock() instanceof RingBlock
                            || state.getBlock() instanceof StargateBaseBlock || state.getDestroySpeed(level, pos) < 0) {
                        continue;
                    }
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    cleared++;
                }
            }
        }
    }

    private static DamageSource kawoosh(final ServerLevel level) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(GateTravel.KAWOOSH));
    }
}
