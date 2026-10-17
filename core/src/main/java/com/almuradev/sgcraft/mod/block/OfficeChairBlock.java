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

import com.almuradev.sgcraft.mod.computer.DialingComputers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * An office chair, as the SGC's operators sit in. Used, it seats the player facing the way the chair does; a player
 * seated facing a dialing computer works it. The chair faces the way the player placing it looks, so a chair put
 * down while looking at a computer faces that computer.
 */
public final class OfficeChairBlock extends HorizontalDirectionalBlock {

    // The seat and its column, and the back behind the seat, for a chair facing north
    private static final VoxelShape SEAT = Block.box(2, 0, 2, 14, 9, 14);
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        OfficeChairBlock.SHAPES.put(Direction.NORTH, Shapes.or(OfficeChairBlock.SEAT, Block.box(3, 9, 12, 13, 20, 14)));
        OfficeChairBlock.SHAPES.put(Direction.SOUTH, Shapes.or(OfficeChairBlock.SEAT, Block.box(3, 9, 2, 13, 20, 4)));
        OfficeChairBlock.SHAPES.put(Direction.WEST, Shapes.or(OfficeChairBlock.SEAT, Block.box(12, 9, 3, 14, 20, 13)));
        OfficeChairBlock.SHAPES.put(Direction.EAST, Shapes.or(OfficeChairBlock.SEAT, Block.box(2, 9, 3, 4, 20, 13)));
    }

    public OfficeChairBlock(final Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING,
                Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
        return this.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
                context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
            final CollisionContext context) {
        return OfficeChairBlock.SHAPES.get(state.getValue(HorizontalDirectionalBlock.FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
            final Player player, final BlockHitResult hit) {
        if (player instanceof final ServerPlayer serverPlayer) {
            DialingComputers.sit(serverPlayer, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
