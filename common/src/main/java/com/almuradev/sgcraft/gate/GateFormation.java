/*
 * This file is part of SGCraft, licensed under the MIT License (MIT).
 * It is based on the original SGCraft by gcewing.
 *
 * Copyright (c) AlmuraDev <https://github.com/AlmuraDev>
 * Copyright (c) 2016 gcewing <https://github.com/gcewing>
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

package com.almuradev.sgcraft.gate;

import com.almuradev.sgcraft.block.RingBlock;
import com.almuradev.sgcraft.block.SGBlocks;
import com.almuradev.sgcraft.block.StargateBaseBlock;
import com.almuradev.sgcraft.block.entity.StargateBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

/**
 * Forms a gate from a base block and a complete ring around it, and breaks it apart again.
 *
 * <p>A standard gate stands upright, five blocks wide and tall, with its base at the bottom centre. Positions are in
 * the gate's own frame: x runs to the gate's right as seen from in front, and y runs up from the base.</p>
 */
public final class GateFormation {

    private static final int EMPTY = 0;
    private static final int RING = 1;
    private static final int CHEVRON = 2;

    // Rows from top to bottom; the base is the centre of the bottom row
    private static final int[][] STANDARD_PATTERN = {
            {2, 1, 2, 1, 2},
            {1, 0, 0, 0, 1},
            {2, 0, 0, 0, 2},
            {1, 0, 0, 0, 1},
            {2, 1, 0, 1, 2},
    };
    private static final int HALF_WIDTH = 2;
    private static final int HEIGHT = 5;

    private GateFormation() {
    }

    /**
     * Forms the gate on the base at {@code basePos} if the ring round it is complete and not already part of a gate.
     *
     * @return whether a gate formed
     */
    public static boolean tryForm(final ServerLevel level, final BlockPos basePos) {
        final BlockState baseState = level.getBlockState(basePos);
        if (!(baseState.getBlock() instanceof StargateBaseBlock)
                || !(level.getBlockEntity(basePos) instanceof StargateBaseBlockEntity base)
                || base.isFormed()) {
            return false;
        }
        final Direction facing = baseState.getValue(HorizontalDirectionalBlock.FACING);
        if (!GateFormation.ringMatches(level, basePos, facing)) {
            return false;
        }
        GateFormation.forEachRingBlock(basePos, facing, ringPos -> {
            final BlockState ringState = level.getBlockState(ringPos);
            level.setBlock(ringPos, ringState.setValue(RingBlock.MERGED, true), Block.UPDATE_ALL);
        });
        base.setFormed(true);
        return true;
    }

    /** Tries to form a gate on every base whose ring could include the ring block at {@code ringPos}. */
    public static void formAround(final ServerLevel level, final BlockPos ringPos) {
        GateFormation.forEachBaseAround(level, ringPos, basePos -> GateFormation.tryForm(level, basePos));
    }

    /** Breaks apart any formed gate whose ring included the block that was at {@code ringPos}. */
    public static void breakAround(final ServerLevel level, final BlockPos ringPos) {
        GateFormation.forEachBaseAround(level, ringPos, basePos -> {
            final BlockState baseState = level.getBlockState(basePos);
            if (level.getBlockEntity(basePos) instanceof StargateBaseBlockEntity base && base.isFormed()
                    && GateFormation.isInRing(basePos, baseState.getValue(HorizontalDirectionalBlock.FACING), ringPos)) {
                base.setFormed(false);
                GateFormation.releaseRing(level, basePos, baseState.getValue(HorizontalDirectionalBlock.FACING));
            }
        });
    }

    /** Un-merges every ring block of a gate whose base faced {@code facing}, such as after the base is broken. */
    public static void releaseRing(final ServerLevel level, final BlockPos basePos, final Direction facing) {
        GateFormation.forEachRingBlock(basePos, facing, ringPos -> {
            final BlockState ringState = level.getBlockState(ringPos);
            if (ringState.getBlock() instanceof RingBlock && ringState.getValue(RingBlock.MERGED)) {
                level.setBlock(ringPos, ringState.setValue(RingBlock.MERGED, false), Block.UPDATE_ALL);
            }
        });
    }

    private static boolean ringMatches(final ServerLevel level, final BlockPos basePos, final Direction facing) {
        final Direction right = facing.getCounterClockWise();
        for (int x = -GateFormation.HALF_WIDTH; x <= GateFormation.HALF_WIDTH; x++) {
            for (int y = 0; y < GateFormation.HEIGHT; y++) {
                if (x == 0 && y == 0) {
                    continue;
                }
                final BlockState state = level.getBlockState(basePos.relative(right, x).above(y));
                if (GateFormation.kindOf(state) != GateFormation.cellAt(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int kindOf(final BlockState state) {
        if (state.isAir()) {
            return GateFormation.EMPTY;
        }
        if (state.getBlock() instanceof RingBlock ring && !state.getValue(RingBlock.MERGED)) {
            return ring.isChevron() ? GateFormation.CHEVRON : GateFormation.RING;
        }
        return -1;
    }

    private static int cellAt(final int x, final int y) {
        return GateFormation.STANDARD_PATTERN[GateFormation.HEIGHT - 1 - y][x + GateFormation.HALF_WIDTH];
    }

    private static boolean isInRing(final BlockPos basePos, final Direction facing, final BlockPos pos) {
        final boolean[] found = {false};
        GateFormation.forEachRingBlock(basePos, facing, ringPos -> found[0] |= ringPos.equals(pos));
        return found[0];
    }

    private static void forEachRingBlock(final BlockPos basePos, final Direction facing,
            final Consumer<BlockPos> action) {
        final Direction right = facing.getCounterClockWise();
        for (int x = -GateFormation.HALF_WIDTH; x <= GateFormation.HALF_WIDTH; x++) {
            for (int y = 0; y < GateFormation.HEIGHT; y++) {
                if (GateFormation.cellAt(x, y) != GateFormation.EMPTY) {
                    action.accept(basePos.relative(right, x).above(y));
                }
            }
        }
    }

    // Bases whose ring could reach pos: up to a gate's width away to either side, and up to its height below
    private static void forEachBaseAround(final ServerLevel level, final BlockPos pos,
            final Consumer<BlockPos> action) {
        final int reach = GateFormation.HALF_WIDTH;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                for (int dy = -(GateFormation.HEIGHT - 1); dy <= 0; dy++) {
                    final BlockPos candidate = pos.offset(dx, dy, dz);
                    if (level.getBlockState(candidate).is(SGBlocks.STARGATE_BASE)) {
                        action.accept(candidate);
                    }
                }
            }
        }
    }
}
