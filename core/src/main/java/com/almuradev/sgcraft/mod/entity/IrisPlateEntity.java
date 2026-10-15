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

package com.almuradev.sgcraft.mod.entity;

import com.almuradev.sgcraft.mod.block.entity.StargateBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Part of a shut iris that nothing gets through, as the original's iris entity was: an unseen plate across a piece of
 * a gate's opening, where the blades are, as solid to walk into as a boat. Its gate's base puts plates across the
 * opening once the iris is half shut, when it starts stopping travelers, and takes them away again as it opens. Each
 * is at most two blocks across, standing at its middle, so the world finds it wherever something meets it. Plates
 * aren't saved; the base puts them back when it loads.
 */
public final class IrisPlateEntity extends Entity {

    // How far the plate reaches across the gate and up it from its middle, the way the gate's front faces, and the
    // way the top of its ring points
    private static final EntityDataAccessor<Float> HALF_WIDTH = SynchedEntityData.defineId(IrisPlateEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HALF_HEIGHT = SynchedEntityData.defineId(IrisPlateEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Direction> FACING = SynchedEntityData.defineId(IrisPlateEntity.class,
            EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Direction> TOP = SynchedEntityData.defineId(IrisPlateEntity.class,
            EntityDataSerializers.DIRECTION);
    // How thick the plate is, from just behind the blades to just in front of them
    private static final double HALF_DEPTH = 0.15;
    // How often a plate checks its gate is still there
    private static final int CHECK_TICKS = 20;

    private @Nullable BlockPos base;

    public IrisPlateEntity(final EntityType<? extends IrisPlateEntity> type, final Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /**
     * Makes a plate for a gate's iris.
     *
     * @param middle where the plate's middle is, which it stands at
     * @param facing the way the gate's front faces
     * @param top the way the top of the gate's ring points: up, unless the gate lies flat
     * @param halfWidth how far the plate reaches either way across the gate
     * @param halfHeight how far it reaches up and down the gate's ring
     * @param base the gate's base, which the plate checks is still there
     */
    public static IrisPlateEntity create(final ServerLevel level, final Vec3 middle, final Direction facing,
            final Direction top, final double halfWidth, final double halfHeight, final BlockPos base) {
        final IrisPlateEntity plate = new IrisPlateEntity(SGEntityTypes.IRIS_PLATE, level);
        plate.entityData.set(IrisPlateEntity.FACING, facing);
        plate.entityData.set(IrisPlateEntity.TOP, top);
        plate.entityData.set(IrisPlateEntity.HALF_WIDTH, (float) halfWidth);
        plate.entityData.set(IrisPlateEntity.HALF_HEIGHT, (float) halfHeight);
        plate.base = base;
        plate.setPos(middle);
        return plate;
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        builder.define(IrisPlateEntity.HALF_WIDTH, 0F);
        builder.define(IrisPlateEntity.HALF_HEIGHT, 0F);
        builder.define(IrisPlateEntity.FACING, Direction.NORTH);
        builder.define(IrisPlateEntity.TOP, Direction.UP);
    }

    @Override
    public void onSyncedDataUpdated(final EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        this.setBoundingBox(this.makeBoundingBox(this.position()));
    }

    // Across the gate and up its ring by the plate's size, and through it by its thickness
    @Override
    protected AABB makeBoundingBox(final Vec3 position) {
        final Direction.Axis through = this.entityData.get(IrisPlateEntity.FACING).getAxis();
        final Direction.Axis up = this.entityData.get(IrisPlateEntity.TOP).getAxis();
        final double x = this.reach(Direction.Axis.X, through, up);
        final double y = this.reach(Direction.Axis.Y, through, up);
        final double z = this.reach(Direction.Axis.Z, through, up);
        return new AABB(position.x - x, position.y - y, position.z - z, position.x + x, position.y + y,
                position.z + z);
    }

    // How far the plate reaches from its middle along an axis of the world
    private double reach(final Direction.Axis axis, final Direction.Axis through, final Direction.Axis up) {
        if (axis == through) {
            return IrisPlateEntity.HALF_DEPTH;
        }
        return axis == up ? this.entityData.get(IrisPlateEntity.HALF_HEIGHT)
                : this.entityData.get(IrisPlateEntity.HALF_WIDTH);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel level && this.tickCount % IrisPlateEntity.CHECK_TICKS == 0
                && (this.base == null || !(level.getBlockEntity(this.base) instanceof StargateBaseBlockEntity gate)
                        || !gate.holdsPlate(this))) {
            this.discard();
        }
    }

    @Override
    public boolean canBeCollidedWith(final @Nullable Entity other) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(final Entity entity) {
    }

    @Override
    public boolean ignoreExplosion(final Explosion explosion) {
        return true;
    }

    @Override
    public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(final ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(final ValueOutput output) {
    }
}
