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

import com.almuradev.sgcraft.mod.block.OfficeChairBlock;
import com.almuradev.sgcraft.mod.computer.DialingComputers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * The unseen seat of an office chair, which a seated player rides. It stands where the sitter's hips go, and goes as
 * soon as its sitter gets up or its chair is gone. Seats aren't saved.
 */
public final class SeatEntity extends Entity {

    // How high above the chair's block the sitter's hips are: the top of the seat's cushion
    private static final double HEIGHT = 0.56;

    public SeatEntity(final EntityType<? extends SeatEntity> type, final Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** Makes a seat on the chair at {@code chair}, facing the way the chair does. */
    public static SeatEntity create(final ServerLevel level, final BlockPos chair, final Direction facing) {
        final SeatEntity seat = new SeatEntity(SGEntityTypes.SEAT, level);
        seat.setPos(chair.getX() + 0.5, chair.getY() + SeatEntity.HEIGHT, chair.getZ() + 0.5);
        seat.setYRot(facing.toYRot());
        return seat;
    }

    /** @return the chair the seat is on */
    public BlockPos chair() {
        return BlockPos.containing(this.getX(), this.getY() - SeatEntity.HEIGHT + 0.01, this.getZ());
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel level && (this.getPassengers().isEmpty()
                || !(level.getBlockState(this.chair()).getBlock() instanceof OfficeChairBlock))) {
            this.ejectPassengers();
            this.discard();
        }
    }

    // The seat goes as its sitter gets up
    @Override
    protected void removePassenger(final Entity passenger) {
        super.removePassenger(passenger);
        if (passenger instanceof final ServerPlayer player) {
            DialingComputers.stoodUp(player);
        }
        if (!this.level().isClientSide() && this.getPassengers().isEmpty()) {
            this.discard();
        }
    }

    // The sitter gets up behind the chair, or to either side of it, where there is room, or else on the seat
    @Override
    public Vec3 getDismountLocationForPassenger(final LivingEntity passenger) {
        final BlockPos chair = this.chair();
        final BlockState state = this.level().getBlockState(chair);
        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            final Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
            for (final Direction side : new Direction[] {facing.getOpposite(), facing.getClockWise(),
                    facing.getCounterClockWise()}) {
                final Vec3 place = DismountHelper.findSafeDismountLocation(passenger.getType(), this.level(),
                        chair.relative(side), true);
                if (place != null) {
                    return place;
                }
            }
        }
        return super.getDismountLocationForPassenger(passenger);
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
