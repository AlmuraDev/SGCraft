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

package com.almuradev.sgcraft.mod.generator;

import com.almuradev.sgcraft.mod.ClientTicks;
import com.almuradev.sgcraft.mod.block.entity.SGBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Tau'ri Naquadah Generator, as in 2.2.0, in two marks: a low, half-block-tall machine facing whoever placed it.
 * Switched on, its arms stand out and, while it has energy or fuel, its lights glow and it hums; switched off, its
 * arms draw in. Its block entity draws it, so the block itself is unseen.
 */
public final class NaquadahGeneratorBlock extends Block implements EntityBlock {

    /** Whether it is switched on, its arms out. */
    public static final BooleanProperty ON = BooleanProperty.create("on");
    /** Whether it is switched on with energy or fuel to give: its lights glow and it hums. */
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");

    // Its shape facing north, arms out and drawn in, in sixteenths of a block
    private static final VoxelShape OUT_NORTH = Block.box(0.16, 0, 5.73, 15.84, 7.64, 10.21);
    private static final VoxelShape IN_NORTH = Block.box(1.68, 0, 5.73, 14.32, 7.64, 10.21);
    private static final VoxelShape OUT_EAST = Block.box(5.79, 0, 0.16, 10.27, 7.64, 15.84);
    private static final VoxelShape IN_EAST = Block.box(5.79, 0, 1.68, 10.27, 7.64, 14.32);

    private final GeneratorMark mark;

    public NaquadahGeneratorBlock(final Properties properties, final GeneratorMark mark) {
        super(properties.lightLevel(state -> state.getValue(NaquadahGeneratorBlock.RUNNING) ? 7 : 0));
        this.mark = mark;
        this.registerDefaultState(this.stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING,
                Direction.NORTH).setValue(NaquadahGeneratorBlock.ON, false)
                .setValue(NaquadahGeneratorBlock.RUNNING, false));
    }

    /** @return its mark */
    public GeneratorMark mark() {
        return this.mark;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING, NaquadahGeneratorBlock.ON, NaquadahGeneratorBlock.RUNNING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
        return this.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
                context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
            final CollisionContext context) {
        final boolean out = state.getValue(NaquadahGeneratorBlock.ON);
        if (state.getValue(HorizontalDirectionalBlock.FACING).getAxis() == Direction.Axis.Z) {
            return out ? NaquadahGeneratorBlock.OUT_NORTH : NaquadahGeneratorBlock.IN_NORTH;
        }
        return out ? NaquadahGeneratorBlock.OUT_EAST : NaquadahGeneratorBlock.IN_EAST;
    }

    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
            final Player player, final BlockHitResult hit) {
        if (player instanceof final ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof final NaquadahGeneratorBlockEntity generator) {
            generator.open(serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new NaquadahGeneratorBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
            final BlockEntityType<T> type) {
        if (type != SGBlockEntityTypes.NAQUADAH_GENERATOR) {
            return null;
        }
        if (level.isClientSide()) {
            return (tickLevel, pos, tickState, entity) -> ClientTicks.generator((NaquadahGeneratorBlockEntity) entity);
        }
        return (tickLevel, pos, tickState, entity) -> NaquadahGeneratorBlockEntity.serverTick((ServerLevel) tickLevel,
                pos, tickState, (NaquadahGeneratorBlockEntity) entity);
    }
}
