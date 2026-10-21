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

package com.almuradev.sgcraft.mod.obelisk;

import com.almuradev.sgcraft.mod.block.entity.SGBlockEntityTypes;
import com.almuradev.sgcraft.mod.item.SGItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * An Asgard transport obelisk, as in 2.2.0: a pillar two blocks tall with a red gem in the face of its top half, facing
 * whoever placed it. Obelisks are linked in pairs, and touching the gem of one beams the player to the other (see
 * {@link Obelisks}). Its bottom half holds the link; either half goes when the other does, and only the bottom half
 * drops the obelisk. Its block entity draws it, so the blocks themselves are unseen.
 */
public final class AsgardObeliskBlock extends Block implements EntityBlock {

    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    // How far around the gem's middle a click counts, in blocks
    private static final double GEM_REACH = 2 / 16.0;

    private final ObeliskKind kind;
    private final VoxelShape lower;
    private final VoxelShape upper;

    public AsgardObeliskBlock(final Properties properties, final ObeliskKind kind) {
        super(properties);
        this.kind = kind;
        this.lower = kind == ObeliskKind.HAMMER ? Block.box(2, 0, 2, 14, 16, 14) : Block.box(1, 0, 1, 15, 16, 15);
        this.upper = kind == ObeliskKind.HAMMER ? Block.box(1, 0, 1, 15, 16, 15) : Block.box(4, 0, 4, 12, 16, 12);
        this.registerDefaultState(this.stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING,
                Direction.NORTH).setValue(AsgardObeliskBlock.HALF, DoubleBlockHalf.LOWER));
    }

    /** @return its kind */
    public ObeliskKind kind() {
        return this.kind;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING, AsgardObeliskBlock.HALF);
    }

    // It needs room for its top half, and its gem faces whoever placed it
    @Override
    public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
        final BlockPos pos = context.getClickedPos();
        final Level level = context.getLevel();
        if (pos.getY() >= level.getMaxY() || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        return this.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
                context.getHorizontalDirection().getOpposite());
    }

    // Its top half goes on, and whoever placed it owns it
    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state,
            final @Nullable LivingEntity placer, final ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(AsgardObeliskBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        if (placer instanceof final Player player && level.getBlockEntity(pos) instanceof final AsgardObeliskBlockEntity
                obelisk) {
            obelisk.setOwner(player.getUUID(), player.getName().getString());
        }
    }

    // Either half goes when the other does
    @Override
    protected BlockState updateShape(final BlockState state, final LevelReader level,
            final ScheduledTickAccess ticks, final BlockPos pos, final Direction direction, final BlockPos neighborPos,
            final BlockState neighbor, final RandomSource random) {
        final boolean upper = state.getValue(AsgardObeliskBlock.HALF) == DoubleBlockHalf.UPPER;
        if (direction == (upper ? Direction.DOWN : Direction.UP)
                && (!neighbor.is(this) || neighbor.getValue(AsgardObeliskBlock.HALF) == state.getValue(
                        AsgardObeliskBlock.HALF))) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
    }

    // Breaking the top half in creative mode takes the bottom half away without dropping it
    @Override
    public BlockState playerWillDestroy(final Level level, final BlockPos pos, final BlockState state,
            final Player player) {
        if (!level.isClientSide() && player.preventsBlockDrops()
                && state.getValue(AsgardObeliskBlock.HALF) == DoubleBlockHalf.UPPER) {
            final BlockPos below = pos.below();
            final BlockState lower = level.getBlockState(below);
            if (lower.is(this) && lower.getValue(AsgardObeliskBlock.HALF) == DoubleBlockHalf.LOWER) {
                level.setBlock(below, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(player, 2001, below, Block.getId(lower));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
            final CollisionContext context) {
        return state.getValue(AsgardObeliskBlock.HALF) == DoubleBlockHalf.UPPER ? this.upper : this.lower;
    }

    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.INVISIBLE;
    }

    // With a configurator, its screen opens
    @Override
    protected InteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level,
            final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hit) {
        if (!stack.is(SGItems.CONFIGURATOR)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (player instanceof final ServerPlayer serverPlayer
                && level.getBlockEntity(AsgardObeliskBlock.base(state, pos)) instanceof final AsgardObeliskBlockEntity
                obelisk) {
            ObeliskActions.open(serverPlayer, obelisk);
        }
        return InteractionResult.SUCCESS;
    }

    // Sneaking links it to another; touching its gem beams the player to the obelisk it is linked to
    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
            final Player player, final BlockHitResult hit) {
        if (player instanceof final ServerPlayer serverPlayer
                && level.getBlockEntity(AsgardObeliskBlock.base(state, pos)) instanceof final AsgardObeliskBlockEntity
                obelisk) {
            if (serverPlayer.isSecondaryUseActive()) {
                Obelisks.link(serverPlayer, obelisk);
            } else if (this.onGem(state, pos, hit)) {
                Obelisks.transport(serverPlayer, obelisk);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** @return where the bottom half of the obelisk a block belongs to is */
    public static BlockPos base(final BlockState state, final BlockPos pos) {
        return state.getValue(AsgardObeliskBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    // A click on the face the gem is on, of the top half, near enough the gem
    private boolean onGem(final BlockState state, final BlockPos pos, final BlockHitResult hit) {
        final Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        if (state.getValue(AsgardObeliskBlock.HALF) != DoubleBlockHalf.UPPER || hit.getDirection() != facing) {
            return false;
        }
        final Vec3 local = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        final double across = (facing.getAxis() == Direction.Axis.Z ? local.x : local.z) - 0.5;
        final double up = local.y - this.kind.gemY();
        return across * across + up * up <= AsgardObeliskBlock.GEM_REACH * AsgardObeliskBlock.GEM_REACH;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return state.getValue(AsgardObeliskBlock.HALF) == DoubleBlockHalf.LOWER
                ? new AsgardObeliskBlockEntity(pos, state) : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
            final BlockEntityType<T> type) {
        if (level.isClientSide() || type != SGBlockEntityTypes.ASGARD_OBELISK) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) -> ((AsgardObeliskBlockEntity) entity).serverTick(
                (ServerLevel) tickLevel);
    }
}
