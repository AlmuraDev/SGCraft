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

package com.almuradev.sgcraft.mod.zpm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A ZPM console, interface cart or hub: faces the player who places it, opens its ZPM slots when used, and shows the
 * ZPMs it holds. A console and a cart show whether they hold one; a hub shows how many of its three it holds.
 */
public abstract class ZpmDeviceBlock extends HorizontalDirectionalBlock implements EntityBlock {

    /** Whether a console or cart holds a ZPM. */
    public static final BooleanProperty LOADED = BooleanProperty.create("loaded");
    /** How many ZPMs a hub holds. */
    public static final IntegerProperty ZPMS = IntegerProperty.create("zpms", 0, 3);

    private final ZpmDevice device;

    protected ZpmDeviceBlock(final Properties properties, final ZpmDevice device) {
        super(properties);
        this.device = device;
    }

    /** @return the kind of device it is */
    public ZpmDevice device() {
        return this.device;
    }

    /** @return the state that shows a device holding that many ZPMs */
    public abstract BlockState showing(final BlockState state, final int held);

    @Override
    public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
        return this.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
                context.getHorizontalDirection().getOpposite());
    }

    // The player who puts it down owns it, and alone, with operators, links it to a gate
    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state,
            final @Nullable LivingEntity by, final ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        if (!level.isClientSide() && by instanceof Player
                && level.getBlockEntity(pos) instanceof final ZpmDeviceBlockEntity device) {
            device.setOwner(by.getUUID());
        }
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
            final Player player, final BlockHitResult hit) {
        if (player instanceof final ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof final ZpmDeviceBlockEntity device) {
            // Sneaking with an empty hand links it to a gate instead, as in 2.2.0
            if (serverPlayer.isSecondaryUseActive()) {
                ZpmLinks.sneakUse(serverPlayer, pos);
            } else {
                device.open(serverPlayer);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new ZpmDeviceBlockEntity(pos, state);
    }

    /** A ZPM console or interface cart, which holds one ZPM and shows whether it does. */
    public static final class Single extends ZpmDeviceBlock {

        // The cart stands half a block deep, across the way it faces, as the original's
        private static final VoxelShape CART_NORTH_SOUTH = Block.box(0, 0, 4, 16, 16, 12);
        private static final VoxelShape CART_EAST_WEST = Block.box(4, 0, 0, 12, 16, 16);

        public Single(final Properties properties, final ZpmDevice device) {
            super(properties, device);
            this.registerDefaultState(this.stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING,
                    Direction.NORTH).setValue(ZpmDeviceBlock.LOADED, false));
        }

        @Override
        protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(HorizontalDirectionalBlock.FACING, ZpmDeviceBlock.LOADED);
        }

        @Override
        public BlockState showing(final BlockState state, final int held) {
            return state.setValue(ZpmDeviceBlock.LOADED, held > 0);
        }

        @Override
        protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                final CollisionContext context) {
            if (this.device() != ZpmDevice.CART) {
                return Shapes.block();
            }
            return state.getValue(HorizontalDirectionalBlock.FACING).getAxis() == Direction.Axis.Z
                    ? Single.CART_NORTH_SOUTH : Single.CART_EAST_WEST;
        }
    }

    /** A ZPM hub, which holds three ZPMs and shows how many it does. */
    public static final class Hub extends ZpmDeviceBlock {

        public Hub(final Properties properties) {
            super(properties, ZpmDevice.HUB);
            this.registerDefaultState(this.stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING,
                    Direction.NORTH).setValue(ZpmDeviceBlock.ZPMS, 0));
        }

        @Override
        protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(HorizontalDirectionalBlock.FACING, ZpmDeviceBlock.ZPMS);
        }

        @Override
        public BlockState showing(final BlockState state, final int held) {
            return state.setValue(ZpmDeviceBlock.ZPMS, Math.min(3, held));
        }
    }
}
