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

import com.almuradev.sgcraft.gate.GateBaseActions;
import com.almuradev.sgcraft.gate.GateFormation;
import com.almuradev.sgcraft.gate.GateLight;
import com.almuradev.sgcraft.item.GateItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * A block of a gate's ring, either a plain segment or a chevron. Once a gate forms, its ring blocks are merged into
 * it, and the block under each of its chevrons gives light while that chevron is lit.
 */
public final class RingBlock extends Block implements SimpleWaterloggedBlock {

    public static final BooleanProperty MERGED = BooleanProperty.create("merged");
    /** Whether a chevron of the gate stands lit over this block, so it gives light; only while merged. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private final boolean chevron;
    private final boolean supergate;

    public RingBlock(final Properties properties, final boolean chevron) {
        this(properties, chevron, false);
    }

    public RingBlock(final Properties properties, final boolean chevron, final boolean supergate) {
        super(properties.lightLevel(RingBlock::lightOf));
        this.chevron = chevron;
        this.supergate = supergate;
        this.registerDefaultState(this.stateDefinition.any().setValue(RingBlock.MERGED, false)
                .setValue(RingBlock.LIT, false).setValue(BlockStateProperties.WATERLOGGED, false));
    }

    private static int lightOf(final BlockState state) {
        return state.getValue(RingBlock.MERGED) && state.getValue(RingBlock.LIT) ? GateLight.CHEVRON : 0;
    }

    /** @return whether this is a chevron block rather than a plain ring segment */
    public boolean isChevron() {
        return this.chevron;
    }

    /** @return whether this block makes supergates' rings rather than standard gates' */
    public boolean isSupergate() {
        return this.supergate;
    }

    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        // A formed gate is drawn whole by its base's renderer
        return state.getValue(RingBlock.MERGED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getOcclusionShape(final BlockState state) {
        // Merged blocks are drawn as part of the gate, so they mustn't hide the faces of blocks beside them or block
        // their light
        return state.getValue(RingBlock.MERGED) ? Shapes.empty() : super.getOcclusionShape(state);
    }

    @Override
    protected boolean propagatesSkylightDown(final BlockState state) {
        return state.getValue(RingBlock.MERGED) || super.propagatesSkylightDown(state);
    }

    @Override
    protected int getLightDampening(final BlockState state) {
        return state.getValue(RingBlock.MERGED) ? 0 : super.getLightDampening(state);
    }

    @Override
    protected float getShadeBrightness(final BlockState state, final BlockGetter level, final BlockPos pos) {
        // Unmerged, a full block shades the corners of blocks beside it; merged, it isn't there to see
        return state.getValue(RingBlock.MERGED) ? 1.0F : super.getShadeBrightness(state, level, pos);
    }

    // Picking a block of a finished gate gives the spawner that builds one like it, as in the original
    @Override
    protected ItemStack getCloneItemStack(final LevelReader level, final BlockPos pos, final BlockState state,
            final boolean includeData) {
        if (state.getValue(RingBlock.MERGED) && level instanceof Level world) {
            final Optional<GateSpawnerBlock> spawner = GateFormation.baseOf(world, pos, this.supergate)
                    .flatMap(base -> GateSpawnerBlock.forGate(base.gateType(), this.supergate));
            if (spawner.isPresent()) {
                return new ItemStack(spawner.get());
            }
        }
        return super.getCloneItemStack(level, pos, state, includeData);
    }

    // The tools and upgrades work on a gate themselves, so they get their turn before the base's screen
    @Override
    protected InteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level,
            final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hit) {
        return stack.getItem() instanceof GateItem ? InteractionResult.PASS
                : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    // Using any block of a formed gate's ring opens its base's screen, as in the original
    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
            final Player player, final BlockHitResult hit) {
        if (!state.getValue(RingBlock.MERGED)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            GateBaseActions.open(serverPlayer, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RingBlock.MERGED, RingBlock.LIT, BlockStateProperties.WATERLOGGED);
    }

    // Water stays where the block goes, so a gate built in water stands in it
    @Override
    public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
        return this.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,
                context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER));
    }

    @Override
    protected FluidState getFluidState(final BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(final BlockState state, final LevelReader level, final ScheduledTickAccess ticks,
            final BlockPos pos, final Direction directionToNeighbor, final BlockPos neighborPos,
            final BlockState neighborState, final RandomSource random) {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbor, neighborPos, neighborState, random);
    }

    @Override
    protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState,
            final boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel serverLevel && !oldState.is(this) && !state.getValue(RingBlock.MERGED)) {
            GateFormation.formAround(serverLevel, pos, this.supergate);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(final BlockState state, final ServerLevel level, final BlockPos pos,
            final boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        if (state.getValue(RingBlock.MERGED)) {
            GateFormation.breakAround(level, pos, this.supergate);
        }
    }
}
