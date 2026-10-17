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

import com.almuradev.sgcraft.mod.block.entity.DialingComputerBlockEntity;
import com.almuradev.sgcraft.mod.block.entity.SGBlockEntityTypes;
import com.almuradev.sgcraft.mod.computer.DialingComputers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * The SGC's dialing computer: a desk three blocks wide with a wide monitor standing on it, two blocks high, as at
 * Stargate Command. It is one structure of six parts, placed and broken together: the item places it with its middle
 * lower part where it is used, and the rest to either side and above. Its screen faces the player who placed it.
 *
 * <p>The middle lower part holds the computer's link to its gate. A player sits in a chair in front of the computer
 * to use it; see {@link DialingComputers}.</p>
 */
public final class DialingComputerBlock extends HorizontalDirectionalBlock implements EntityBlock {

    /** Which of the six parts a block is. */
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    // The shapes of the parts with the screen facing north, the operator standing north of it: the desk takes the
    // lower row up to twelve pixels, and the monitor's case stands on its far half, from the desk to the top
    private static final VoxelShape DESK = Block.box(0, 0, 0, 16, 12, 16);
    private static final VoxelShape CASE_LOWER = Block.box(0, 12, 8, 16, 16, 15);
    private static final VoxelShape CASE_UPPER = Block.box(0, 0, 8, 16, 16, 15);
    private static final Map<Direction, VoxelShape> LOWER = DialingComputerBlock.rotations(
            Shapes.or(DialingComputerBlock.DESK, DialingComputerBlock.CASE_LOWER));
    private static final Map<Direction, VoxelShape> UPPER = DialingComputerBlock.rotations(
            DialingComputerBlock.CASE_UPPER);

    public DialingComputerBlock(final Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                .setValue(DialingComputerBlock.PART, Part.MIDDLE_LOWER));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING, DialingComputerBlock.PART);
    }

    /** @return where a part of the computer at {@code pos} has the computer's middle lower part */
    public static BlockPos main(final BlockPos pos, final BlockState state) {
        final Part part = state.getValue(DialingComputerBlock.PART);
        return DialingComputerBlock.offset(pos, state.getValue(HorizontalDirectionalBlock.FACING), -part.column,
                -part.row);
    }

    // A place a number of columns to the operator's right and rows up from another
    private static BlockPos offset(final BlockPos pos, final Direction facing, final int columns, final int rows) {
        return pos.relative(DialingComputerBlock.right(facing), columns).above(rows);
    }

    /** @return the direction to the right of the player using a computer whose screen faces {@code facing} */
    public static Direction right(final Direction facing) {
        return facing.getCounterClockWise();
    }

    @Override
    public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
        final Direction facing = context.getHorizontalDirection().getOpposite();
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        for (final Part part : Part.values()) {
            final BlockPos place = DialingComputerBlock.offset(pos, facing, part.column, part.row);
            if (place.getY() >= level.getMaxY() || !level.getWorldBorder().isWithinBounds(place)
                    || !level.getBlockState(place).canBeReplaced(context)) {
                return null;
            }
        }
        return this.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state,
            final @Nullable LivingEntity by, final ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        if (level.isClientSide()) {
            return;
        }
        final Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        for (final Part part : Part.values()) {
            if (part != Part.MIDDLE_LOWER) {
                level.setBlock(DialingComputerBlock.offset(pos, facing, part.column, part.row),
                        state.setValue(DialingComputerBlock.PART, part), Block.UPDATE_ALL);
            }
        }
    }

    // A part goes when a part beside it that belongs to the same computer goes, so the whole computer goes together
    @Override
    protected BlockState updateShape(final BlockState state, final LevelReader level, final ScheduledTickAccess ticks,
            final BlockPos pos, final Direction direction, final BlockPos neighborPos, final BlockState neighborState,
            final RandomSource random) {
        final Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        final Part part = state.getValue(DialingComputerBlock.PART);
        final Direction right = DialingComputerBlock.right(facing);
        final int column = part.column + (direction == right ? 1 : direction == right.getOpposite() ? -1 : 0);
        final int row = part.row + (direction == Direction.UP ? 1 : direction == Direction.DOWN ? -1 : 0);
        final boolean across = direction.getAxis() == right.getAxis() || direction.getAxis() == Direction.Axis.Y;
        final Part expected = across ? Part.at(column, row) : null;
        if (expected != null && (!neighborState.is(this)
                || neighborState.getValue(HorizontalDirectionalBlock.FACING) != facing
                || neighborState.getValue(DialingComputerBlock.PART) != expected)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    // Only the middle lower part drops the computer, so a player breaking another part is given it from there
    @Override
    public BlockState playerWillDestroy(final Level level, final BlockPos pos, final BlockState state,
            final Player player) {
        if (!level.isClientSide() && state.getValue(DialingComputerBlock.PART) != Part.MIDDLE_LOWER
                && !player.preventsBlockDrops()) {
            final BlockPos main = DialingComputerBlock.main(pos, state);
            final BlockState mainState = level.getBlockState(main);
            if (mainState.is(this)) {
                Block.dropResources(mainState, level, main, level.getBlockEntity(main), player,
                        player.getMainHandItem());
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
            final CollisionContext context) {
        final Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        return state.getValue(DialingComputerBlock.PART).row == 0 ? DialingComputerBlock.LOWER.get(facing)
                : DialingComputerBlock.UPPER.get(facing);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return state.getValue(DialingComputerBlock.PART) == Part.MIDDLE_LOWER
                ? new DialingComputerBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
            final BlockEntityType<T> type) {
        if (level.isClientSide() || type != SGBlockEntityTypes.DIALING_COMPUTER) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) -> DialingComputerBlockEntity.serverTick((ServerLevel) tickLevel,
                pos, (DialingComputerBlockEntity) entity);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
            final Player player, final BlockHitResult hit) {
        if (player instanceof final ServerPlayer serverPlayer) {
            DialingComputers.use(serverPlayer, DialingComputerBlock.main(pos, state));
        }
        return InteractionResult.SUCCESS;
    }

    // A shape drawn facing north, turned to each facing
    private static Map<Direction, VoxelShape> rotations(final VoxelShape north) {
        final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        for (final Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (final AABB box : north.toAabbs()) {
                shape = Shapes.or(shape, switch (facing) {
                    case SOUTH -> Shapes.box(1 - box.maxX, box.minY, 1 - box.maxZ, 1 - box.minX, box.maxY,
                            1 - box.minZ);
                    case WEST -> Shapes.box(box.minZ, box.minY, 1 - box.maxX, box.maxZ, box.maxY, 1 - box.minX);
                    case EAST -> Shapes.box(1 - box.maxZ, box.minY, box.minX, 1 - box.minZ, box.maxY, box.maxX);
                    default -> Shapes.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
                });
            }
            shapes.put(facing, shape);
        }
        return shapes;
    }

    /** The six parts of a computer, by column, from the operator's left, and row, from the bottom. */
    public enum Part implements StringRepresentable {
        LEFT_LOWER(-1, 0),
        MIDDLE_LOWER(0, 0),
        RIGHT_LOWER(1, 0),
        LEFT_UPPER(-1, 1),
        MIDDLE_UPPER(0, 1),
        RIGHT_UPPER(1, 1);

        private final int column;
        private final int row;

        Part(final int column, final int row) {
            this.column = column;
            this.row = row;
        }

        /** @return how many blocks to the operator's right of the middle this part is */
        public int column() {
            return this.column;
        }

        /** @return how many blocks above the desk's row this part is */
        public int row() {
            return this.row;
        }

        static @Nullable Part at(final int column, final int row) {
            for (final Part part : Part.values()) {
                if (part.column == column && part.row == row) {
                    return part;
                }
            }
            return null;
        }

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}
