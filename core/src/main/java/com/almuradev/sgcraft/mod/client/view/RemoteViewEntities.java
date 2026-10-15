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

package com.almuradev.sgcraft.mod.client.view;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.view.RemoteViewEntityData;
import com.almuradev.sgcraft.mod.view.RemoteViewPackets;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.DragonFlightHistory;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;

/**
 * The entities in a remote view, as copies made on the client that are never added to its world. Each is drawn with
 * its own renderer where the stream last put it, moving smoothly between the server's ticks. A type that fails to
 * be made or drawn is left out of every view until the client leaves the server, and the first failure is logged.
 */
final class RemoteViewEntities {

    // Moves arrive this many ticks apart
    private static final int MOVE_TICKS = 2;
    // Players' names show this close to the camera
    private static final double NAME_DISTANCE = 32;

    // The types that failed to be made or drawn, by their id in the entity type registry. They are kept while the
    // client stays on the server, across views, so a type that fails isn't made and drawn again each time a view
    // starts; the ids are the server's, so they are forgotten with it
    private static final IntSet FAILED_TYPES = new IntOpenHashSet();
    private static WeakReference<@Nullable ClientPacketListener> failedOn = new WeakReference<>(null);

    private final Int2ObjectMap<Copy> entities = new Int2ObjectOpenHashMap<>();

    /** An entity's copy, and where it is going between ticks. */
    private static final class Copy {

        final Entity entity;
        final RemoteViewSmoothing x = new RemoteViewSmoothing(RemoteViewEntities.MOVE_TICKS);
        final RemoteViewSmoothing y = new RemoteViewSmoothing(RemoteViewEntities.MOVE_TICKS);
        final RemoteViewSmoothing z = new RemoteViewSmoothing(RemoteViewEntities.MOVE_TICKS);
        final RemoteViewSmoothing yaw = new RemoteViewSmoothing(RemoteViewEntities.MOVE_TICKS);
        final RemoteViewSmoothing pitch = new RemoteViewSmoothing(RemoteViewEntities.MOVE_TICKS);
        final RemoteViewSmoothing headYaw = new RemoteViewSmoothing(RemoteViewEntities.MOVE_TICKS);
        final RemoteViewSmoothing bodyYaw = new RemoteViewSmoothing(RemoteViewEntities.MOVE_TICKS);
        boolean placed;
        // How far it moves a tick, between its last two moves
        double motionX;
        double motionY;
        double motionZ;

        Copy(final Entity entity) {
            this.entity = entity;
        }
    }

    /** Makes the copy of an entity that has come into view, or makes it again with the looks it has now. */
    void looks(final RemoteViewPackets.EntityLooks looks) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null || RemoteViewEntities.failedTypes().contains(looks.type())) {
            return;
        }
        final Entity entity;
        try {
            entity = RemoteViewEntities.create(looks, level);
            if (entity == null) {
                RemoteViewEntities.fail(looks.type(), "make a copy of", null);
                return;
            }
            RemoteViewEntityData.apply(looks.data(), entity, level.registryAccess());
        } catch (final RuntimeException e) {
            RemoteViewEntities.fail(looks.type(), "make a copy of", e);
            return;
        }
        final Copy copy = new Copy(entity);
        final Copy old = this.entities.put(looks.id(), copy);
        if (old != null) {
            // Looks changed in place: keep it where it was and as far through its animations
            copy.x.copy(old.x);
            copy.y.copy(old.y);
            copy.z.copy(old.z);
            copy.yaw.copy(old.yaw);
            copy.pitch.copy(old.pitch);
            copy.headYaw.copy(old.headYaw);
            copy.bodyYaw.copy(old.bodyYaw);
            copy.placed = old.placed;
            copy.motionX = old.motionX;
            copy.motionY = old.motionY;
            copy.motionZ = old.motionZ;
            entity.tickCount = old.entity.tickCount;
            if (entity instanceof EnderDragon dragon && old.entity instanceof EnderDragon was) {
                dragon.flightHistory.copyFrom(was.flightHistory);
                dragon.flapTime = was.flapTime;
                dragon.oFlapTime = was.oFlapTime;
            }
            if (entity instanceof LivingEntity living && old.entity instanceof LivingEntity was) {
                living.walkAnimation.setSpeed(was.walkAnimation.speed());
            }
        }
    }

    private static @Nullable Entity create(final RemoteViewPackets.EntityLooks looks, final ClientLevel level) {
        final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.byId(looks.type());
        if (type == EntityTypes.PLAYER) {
            // A player's copy has their UUID and name, so their skin is found as the client finds it for anyone
            final RemotePlayer player = new RemotePlayer(level, new GameProfile(looks.uuid(), looks.name().orElse("")));
            player.setId(looks.id());
            return player;
        }
        final Entity entity = type.create(level, EntitySpawnReason.LOAD);
        if (entity != null) {
            // The entity's own id and UUID, so what the client knows of it by them, such as a player being at a MALP
            // console, shows on the copy too
            entity.setId(looks.id());
            entity.setUUID(looks.uuid());
        }
        return entity;
    }

    void remove(final int id) {
        this.entities.remove(id);
    }

    void clear() {
        this.entities.clear();
    }

    /** Takes where the entities are this tick, and moves their animations on by a tick. */
    void moves(final Iterable<RemoteViewPackets.EntityMove> moves, final long now) {
        for (final RemoteViewPackets.EntityMove move : moves) {
            final Copy copy = this.entities.get(move.id());
            if (copy == null) {
                continue;
            }
            final double moved = copy.placed ? Math.hypot(move.x() - copy.x.target(), move.z() - copy.z.target()) : 0;
            final boolean snap = !copy.placed;
            if (copy.placed) {
                copy.motionX = (move.x() - copy.x.target()) / RemoteViewEntities.MOVE_TICKS;
                copy.motionY = (move.y() - copy.y.target()) / RemoteViewEntities.MOVE_TICKS;
                copy.motionZ = (move.z() - copy.z.target()) / RemoteViewEntities.MOVE_TICKS;
            } else if (copy.entity instanceof EnderDragon dragon) {
                // As if it had always flown here, so its neck and tail start out straight behind it
                for (int i = 0; i < DragonFlightHistory.LENGTH; i++) {
                    dragon.flightHistory.record(move.y(), Mth.wrapDegrees(move.yaw()));
                }
            }
            copy.x.set(move.x(), now, snap);
            copy.y.set(move.y(), now, snap);
            copy.z.set(move.z(), now, snap);
            copy.yaw.setAngle(move.yaw(), now, snap);
            copy.pitch.setAngle(move.pitch(), now, snap);
            copy.headYaw.setAngle(move.headYaw(), now, snap);
            copy.bodyYaw.setAngle(move.bodyYaw(), now, snap);
            copy.placed = true;
            copy.entity.setOnGround(move.onGround());
            if (copy.entity instanceof LivingEntity living) {
                // Moves arrive every other tick, so the step is halved, as a living entity works out its stride
                living.walkAnimation.update(Math.min((float) moved * 2.0F, 1.0F), 0.4F, 1.0F);
            }
        }
    }

    /** Moves every copy's age on by a tick, for animations that run on it. */
    void tick() {
        for (final Copy copy : this.entities.values()) {
            copy.entity.tickCount++;
            if (copy.placed && copy.entity instanceof EnderDragon dragon) {
                RemoteViewEntities.fly(copy, dragon);
            }
        }
    }

    // The dragon's renderer bends its neck and tail along the heights and turns it has flown through, and beats its
    // wings by a time that speeds up as it slows: its own tick keeps both, which a copy never runs, so they are kept
    // here as that tick keeps them on a client
    private static void fly(final Copy copy, final EnderDragon dragon) {
        dragon.oFlapTime = dragon.flapTime;
        if (dragon.getPhaseManager().getCurrentPhase().isSitting()) {
            dragon.flapTime += 0.1F;
        } else {
            final float flap = 0.2F / ((float) Math.hypot(copy.motionX, copy.motionZ) * 10.0F + 1.0F);
            dragon.flapTime += flap * (float) Math.pow(2.0, copy.motionY);
        }
        dragon.flightHistory.record(copy.y.target(), Mth.wrapDegrees((float) copy.yaw.target()));
    }

    /** Submits every copy that has a place for drawing, relative to the camera. */
    void submit(final RemoteViewLevel level, final Vec3 camera, final CameraRenderState cameraState,
            final PoseStack poseStack, final SubmitNodeCollector collector, final float partialTick,
            final long now) {
        final EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        for (final Int2ObjectMap.Entry<Copy> entry : this.entities.int2ObjectEntrySet()) {
            final Copy copy = entry.getValue();
            if (!copy.placed) {
                continue;
            }
            final Entity entity = copy.entity;
            final int type = BuiltInRegistries.ENTITY_TYPE.getId(entity.getType());
            if (RemoteViewEntities.failedTypes().contains(type)) {
                continue;
            }
            RemoteViewEntities.place(copy, now);
            try {
                // The renderer itself rather than the dispatcher, which builds a whole crash report for any failure
                final EntityRenderState state = dispatcher.getRenderer(entity).createRenderState(entity, partialTick);
                // The client's own world knows nothing of this place: its light, shadows and name tags are the
                // view's own
                state.shadowPieces.clear();
                state.outlineColor = 0;
                state.scoreText = null;
                state.nameTag = null;
                if (entity instanceof Player player
                        && entity.position().distanceToSqr(camera) < Mth.square(RemoteViewEntities.NAME_DISTANCE)) {
                    state.nameTag = player.getDisplayName();
                    state.nameTagAttachment = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0,
                            entity.getYRot());
                }
                final BlockPos light = BlockPos.containing(entity.getEyePosition());
                final int block = Math.max(level.getBrightness(LightLayer.BLOCK, light), entity.isOnFire() ? 15 : 0);
                state.lightCoords = LightCoordsUtil.pack(block, level.getBrightness(LightLayer.SKY, light));
                dispatcher.submit(state, cameraState, entity.getX() - camera.x, entity.getY() - camera.y,
                        entity.getZ() - camera.z, poseStack, collector);
            } catch (final RuntimeException e) {
                RemoteViewEntities.fail(type, "draw", e);
            }
        }
    }

    private static IntSet failedTypes() {
        final ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (RemoteViewEntities.failedOn.get() != connection) {
            RemoteViewEntities.FAILED_TYPES.clear();
            RemoteViewEntities.failedOn = new WeakReference<>(connection);
        }
        return RemoteViewEntities.FAILED_TYPES;
    }

    // Leaves a type out of remote views from now on, saying why the first time
    private static void fail(final int type, final String doing, final @Nullable RuntimeException cause) {
        if (RemoteViewEntities.failedTypes().add(type)) {
            final Identifier name = BuiltInRegistries.ENTITY_TYPE.getKey(BuiltInRegistries.ENTITY_TYPE.byId(type));
            SGCraft.LOG.warn("Couldn't {} {} for a remote view, so remote views leave it out until the client leaves "
                    + "the server", doing, name, cause);
        }
    }

    // Puts a copy where it is between the last two ticks, with nothing left over to interpolate
    private static void place(final Copy copy, final long now) {
        final Entity entity = copy.entity;
        final double x = copy.x.value(now);
        final double y = copy.y.value(now);
        final double z = copy.z.value(now);
        entity.setPos(x, y, z);
        entity.xo = x;
        entity.yo = y;
        entity.zo = z;
        entity.xOld = x;
        entity.yOld = y;
        entity.zOld = z;
        final float yaw = (float) copy.yaw.value(now);
        final float pitch = (float) copy.pitch.value(now);
        entity.setYRot(yaw);
        entity.setXRot(pitch);
        entity.yRotO = yaw;
        entity.xRotO = pitch;
        if (entity instanceof LivingEntity living) {
            living.yHeadRot = (float) copy.headYaw.value(now);
            living.yHeadRotO = living.yHeadRot;
            living.yBodyRot = (float) copy.bodyYaw.value(now);
            living.yBodyRotO = living.yBodyRot;
        }
    }
}
