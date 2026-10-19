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

package com.almuradev.sgcraft.mod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The light blocks a puddle jumper's headlights once set in the world, which a jumper saved by an earlier version may
 * still have standing; it takes them away. Headlights are now light the client adds as it draws the world ahead of
 * the jumper, and set nothing in the world; see {@code JumperLights}.
 */
final class JumperHeadlights {

    private final Set<BlockPos> placed = new HashSet<>();

    /** @return where any lights left standing are, to save until they are taken away */
    List<BlockPos> placed() {
        return new ArrayList<>(this.placed);
    }

    /** @return whether any lights are left standing */
    boolean any() {
        return !this.placed.isEmpty();
    }

    /** Remembers lights saved with the jumper, to be taken away. */
    void remember(final List<BlockPos> saved) {
        this.placed.addAll(saved);
    }

    /** Takes every light left standing away, where it is loaded. */
    void clear(final ServerLevel level) {
        this.placed.removeIf(pos -> {
            if (!level.isLoaded(pos)) {
                return false;
            }
            final BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.LIGHT)) {
                level.setBlock(pos, state.getValue(BlockStateProperties.WATERLOGGED)
                        ? Fluids.WATER.defaultFluidState().createLegacyBlock() : Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_CLIENTS);
            }
            return true;
        });
    }
}
