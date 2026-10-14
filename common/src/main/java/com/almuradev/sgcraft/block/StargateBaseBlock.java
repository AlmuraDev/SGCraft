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

package com.almuradev.sgcraft.block;

import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.block.entity.StargateBaseBlockEntity;
import com.almuradev.sgcraft.gate.GateFormation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The base of a gate: the block at the bottom centre of the ring, which holds the gate's state once it forms. It
 * faces the player who placed it, and the gate it forms faces the same way. Each type of gate has its own base, which
 * gives the gate its type when it forms.
 */
public final class StargateBaseBlock extends HorizontalDirectionalBlock implements EntityBlock {

    /** Whether the base is part of a formed gate, which its renderer then draws whole. */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    private final ResourceKey<GateType> gateType;

    public StargateBaseBlock(final Properties properties, final ResourceKey<GateType> gateType) {
        super(properties);
        this.gateType = gateType;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                .setValue(StargateBaseBlock.FORMED, false));
    }

    /** @return the type of the gates this base forms */
    public ResourceKey<GateType> gateType() {
        return this.gateType;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING, StargateBaseBlock.FORMED);
    }

    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return state.getValue(StargateBaseBlock.FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getOcclusionShape(final BlockState state) {
        return state.getValue(StargateBaseBlock.FORMED) ? Shapes.empty() : super.getOcclusionShape(state);
    }

    @Override
    protected boolean propagatesSkylightDown(final BlockState state) {
        return state.getValue(StargateBaseBlock.FORMED) || super.propagatesSkylightDown(state);
    }

    @Override
    protected int getLightDampening(final BlockState state) {
        return state.getValue(StargateBaseBlock.FORMED) ? 0 : super.getLightDampening(state);
    }

    @Override
    protected float getShadeBrightness(final BlockState state, final BlockGetter level, final BlockPos pos) {
        // Unmerged, a full block shades the corners of blocks beside it; merged, it isn't there to see
        return state.getValue(StargateBaseBlock.FORMED) ? 1.0F : super.getShadeBrightness(state, level, pos);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
        return this.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
                context.getHorizontalDirection().getOpposite());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new StargateBaseBlockEntity(pos, state);
    }

    @Override
    protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState,
            final boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel serverLevel && !oldState.is(this)) {
            GateFormation.tryForm(serverLevel, pos);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(final BlockState state, final ServerLevel level, final BlockPos pos,
            final boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        GateFormation.releaseRing(level, pos, state.getValue(HorizontalDirectionalBlock.FACING));
    }
}
