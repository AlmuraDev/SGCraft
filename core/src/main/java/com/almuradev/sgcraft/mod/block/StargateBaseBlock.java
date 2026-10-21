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

package com.almuradev.sgcraft.mod.block;

import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.block.entity.SGBlockEntityTypes;
import com.almuradev.sgcraft.mod.block.entity.StargateBaseBlockEntity;
import com.almuradev.sgcraft.mod.gate.GateBaseActions;
import com.almuradev.sgcraft.mod.gate.GateFormation;
import com.almuradev.sgcraft.mod.gate.GateFrame;
import com.almuradev.sgcraft.mod.gate.GateRedstone;
import com.almuradev.sgcraft.mod.gate.SGStargate;
import com.almuradev.sgcraft.mod.item.GateItem;
import com.almuradev.sgcraft.mod.zpm.ZpmLinks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * The base of a gate: the block at the bottom center of the ring, which holds the gate's state once it forms. It
 * faces the player who placed it, and the gate it forms faces the same way. Each type of gate has its own base, which
 * gives the gate its type when it forms.
 */
public final class StargateBaseBlock extends HorizontalDirectionalBlock implements EntityBlock,
        SimpleWaterloggedBlock {

    /** Whether the base is part of a formed gate, which its renderer then draws whole. */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    private final ResourceKey<GateType> gateType;
    private final boolean supergate;

    public StargateBaseBlock(final Properties properties, final ResourceKey<GateType> gateType) {
        this(properties, gateType, false);
    }

    /**
     * @param gateType the type of gate the base builds; a supergate's base builds this type until it is changed
     * @param supergate whether the base forms supergates rather than standard gates
     */
    public StargateBaseBlock(final Properties properties, final ResourceKey<GateType> gateType,
            final boolean supergate) {
        super(properties);
        this.gateType = gateType;
        this.supergate = supergate;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                .setValue(StargateBaseBlock.FORMED, false)
                .setValue(BlockStateProperties.WATERLOGGED, false));
    }

    /** @return the type of the gates this base forms */
    public ResourceKey<GateType> gateType() {
        return this.gateType;
    }

    /** @return whether this base forms supergates rather than standard gates */
    public boolean isSupergate() {
        return this.supergate;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING, StargateBaseBlock.FORMED, BlockStateProperties.WATERLOGGED);
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
        // Water stays where the base goes, so a gate built in water stands in it. The player placing it builds the
        // gate it may finish
        GateFormation.building(context);
        return this.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
                context.getHorizontalDirection().getOpposite()).setValue(BlockStateProperties.WATERLOGGED,
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

    // The tools and upgrades work on a gate themselves, so they get their turn before the base's screen
    @Override
    protected InteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level,
            final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hit) {
        return stack.getItem() instanceof GateItem ? InteractionResult.PASS
                : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    // Using a formed gate's base opens its screen, as in the original
    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
            final Player player, final BlockHitResult hit) {
        if (!state.getValue(StargateBaseBlock.FORMED)) {
            return InteractionResult.PASS;
        }
        // A player linking a ZPM device to a gate finishes here, rather than opening the base
        if (player instanceof ServerPlayer serverPlayer && !ZpmLinks.complete(serverPlayer, pos)) {
            GateBaseActions.open(serverPlayer, pos);
        }
        return InteractionResult.SUCCESS;
    }

    // Picking a block of a finished gate gives the spawner that builds one like it, as in the original
    @Override
    protected ItemStack getCloneItemStack(final LevelReader level, final BlockPos pos, final BlockState state,
            final boolean includeData) {
        if (state.getValue(StargateBaseBlock.FORMED)
                && level.getBlockEntity(pos) instanceof StargateBaseBlockEntity base) {
            final Optional<GateSpawnerBlock> spawner = GateSpawnerBlock.forGate(base.gateType(), this.supergate);
            if (spawner.isPresent()) {
                return new ItemStack(spawner.get());
            }
        }
        return super.getCloneItemStack(level, pos, state, includeData);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new StargateBaseBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
            final BlockEntityType<T> type) {
        if (type != SGBlockEntityTypes.STARGATE_BASE) {
            return null;
        }
        if (level.isClientSide()) {
            return (tickLevel, pos, tickState, entity) -> StargateBaseBlockEntity.clientTick(tickLevel, pos,
                    (StargateBaseBlockEntity) entity);
        }
        return (tickLevel, pos, tickState, entity) -> StargateBaseBlockEntity.serverTick((ServerLevel) tickLevel, pos,
                (StargateBaseBlockEntity) entity);
    }

    // A redstone signal arriving or going works the iris, if the gate allows it: on shuts it, off opens it
    @Override
    protected void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block block,
            final @Nullable Orientation orientation, final boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (level instanceof ServerLevel serverLevel && state.getValue(StargateBaseBlock.FORMED)
                && level.getBlockEntity(pos) instanceof StargateBaseBlockEntity base) {
            final boolean powered = serverLevel.hasNeighborSignal(pos);
            if (base.setPowered(powered)) {
                GateRedstone.powered(serverLevel, pos, powered);
            }
        }
    }

    @Override
    protected boolean isSignalSource(final BlockState state) {
        return state.getValue(StargateBaseBlock.FORMED);
    }

    // A gate doing anything gives out a full signal, if it allows it, as in the original
    @Override
    protected int getSignal(final BlockState state, final BlockGetter level, final BlockPos pos,
            final Direction direction) {
        return level.getBlockEntity(pos) instanceof StargateBaseBlockEntity base && base.signal() ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(final BlockState state, final BlockGetter level, final BlockPos pos,
            final Direction direction) {
        return this.getSignal(state, level, pos, direction);
    }

    @Override
    protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState,
            final boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel serverLevel && !oldState.is(this)) {
            GateFormation.tryForm(serverLevel, pos);
        }
    }

    // A player in creative mode breaking a formed gate's base takes its whole ring away with it, as in 2.2.0
    @Override
    public BlockState playerWillDestroy(final Level level, final BlockPos pos, final BlockState state,
            final Player player) {
        if (level instanceof final ServerLevel serverLevel && player.isCreative()
                && state.getValue(StargateBaseBlock.FORMED)) {
            SGServer.current().flatMap(server -> server.stargates().gate(GlobalPos.of(level.dimension(), pos)))
                    .ifPresent(gate -> GateFormation.removeRing(serverLevel, gate.frame()));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void affectNeighborsAfterRemoval(final BlockState state, final ServerLevel level, final BlockPos pos,
            final boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        // The gate's size, and how it lies, are still known to the server until it is forgotten below
        final GateFrame frame = SGServer.current()
                .flatMap(server -> server.stargates().gate(GlobalPos.of(level.dimension(), pos)))
                .map(SGStargate::frame)
                .orElseGet(() -> new GateFrame(pos, state.getValue(HorizontalDirectionalBlock.FACING)));
        GateFormation.releaseRing(level, frame);
        if (state.getValue(StargateBaseBlock.FORMED)) {
            GateFormation.unregister(level, pos);
        }
    }
}
