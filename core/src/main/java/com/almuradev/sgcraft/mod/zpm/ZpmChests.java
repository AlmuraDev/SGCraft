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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/**
 * The chests found beside generated gates, as in the original: now and then a gate world generation builds has a
 * chest by it holding a full ZPM, which is sometimes tainted. ZPMs are found nowhere else.
 */
public final class ZpmChests {

    // Where the chest stands from the gate's base: in front of it, and off to its side, clear of its ring
    private static final int FRONT = 2;
    private static final int SIDE = 4;

    private ZpmChests() {
    }

    /**
     * Rolls for a chest beside a gate world generation has just built, and puts one there if the roll comes up and
     * there is room on firm ground.
     *
     * @param base the gate's base
     * @param facing the way the gate faces
     * @return whether a chest was put down
     */
    public static boolean roll(final ServerLevel level, final BlockPos base, final Direction facing) {
        final ZpmSettings settings = ZpmSettings.current();
        if (level.getRandom().nextInt(100) >= settings.chestChance()) {
            return false;
        }
        final BlockPos column = base.relative(facing, ZpmChests.FRONT).relative(facing.getClockWise(), ZpmChests.SIDE);
        for (int dy = 1; dy >= -1; dy--) {
            final BlockPos at = column.above(dy);
            if (level.getBlockState(at).canBeReplaced() && level.getBlockState(at.below()).isFaceSturdy(level,
                    at.below(), Direction.UP)) {
                level.setBlock(at, Blocks.CHEST.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
                        facing.getCounterClockWise()).setValue(ChestBlock.WATERLOGGED, false), Block.UPDATE_ALL);
                if (level.getBlockEntity(at) instanceof final ChestBlockEntity chest) {
                    chest.setItem(level.getRandom().nextInt(chest.getContainerSize()),
                            ZpmItem.full(level.getRandom().nextInt(100) < settings.taintedChance()));
                    return true;
                }
            }
        }
        return false;
    }
}
