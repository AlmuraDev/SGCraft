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

package com.almuradev.sgcraft.mod.gate;

import com.almuradev.sgcraft.mod.SGConfig;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.block.RingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The light a gate gives the world round it. Each lit chevron lights the block of the ring it stands on, and an open
 * wormhole's horizon lights the opening, brightening and dimming as waves run out across it from its center, about the
 * server's {@code gates.event_horizon_light}; behind a shut iris, {@link GateBackLight} lights the gate's back.
 */
public final class GateLight {

    /** How bright the block under a lit chevron is. */
    public static final int CHEVRON = 12;
    /** How often an open horizon's light moves on with its waves, in ticks. */
    public static final int HORIZON_TICKS = 4;

    /**
     * How a gate's blocks are set when only their light changes: with no neighbor told and no shape updated, as
     * nothing round a gate depends on its light.
     */
    public static final int LIGHT_UPDATE = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    // The horizon's light: how far either way of the server's level it waves, how long a wave takes to pass, in ticks,
    // and how far apart the waves are, in a standard gate's blocks
    private static final double HORIZON_SWING = 3;
    private static final double WAVE_TICKS = 40;
    private static final double WAVE_LENGTH = 4;
    // How far apart the blocks of a supergate's horizon that give light are, at least, so even the biggest has only a
    // few dozen to change
    private static final int SUPERGATE_SPACING = 3;

    // The ring block under each chevron position, as x and y of the gate's frame, for each shape
    private static final Map<GateShape, int[][]> CHEVRON_BLOCKS = new ConcurrentHashMap<>();

    private GateLight() {
    }

    /**
     * Lights the ring blocks under the lit chevrons, and puts out the rest. Blocks not loaded are left as they are.
     *
     * @param lit whether each chevron position is lit, by position
     */
    public static void showChevrons(final Level level, final GateFrame frame, final boolean[] lit) {
        final int[][] blocks = GateLight.chevronBlocks(frame.shape());
        for (int position = 0; position < blocks.length; position++) {
            // Chevrons close together may stand on the same block, which is lit if any of them is
            boolean on = false;
            for (int other = 0; other < blocks.length; other++) {
                on |= other < lit.length && lit[other] && blocks[other][0] == blocks[position][0]
                        && blocks[other][1] == blocks[position][1];
            }
            final BlockPos pos = frame.block(blocks[position][0], blocks[position][1], 0);
            if (!level.isLoaded(pos)) {
                continue;
            }
            final BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof RingBlock && state.getValue(RingBlock.MERGED)
                    && state.getValue(RingBlock.LIT) != on) {
                level.setBlock(pos, state.setValue(RingBlock.LIT, on), GateLight.LIGHT_UPDATE);
            }
        }
    }

    /**
     * How bright a block of an open horizon is. Waves run out from the center, so the light rises and falls in rings
     * across the opening; a supergate's horizon gives light from only some of its blocks, spread across it, so its
     * light changes take little work.
     *
     * @param x across the gate, from its middle
     * @param y up from the base
     * @param time the game time, in ticks
     * @return the block's light level, from 0 to 15
     */
    public static int horizon(final GateShape shape, final int x, final int y, final long time) {
        final int level = GateLight.level();
        if (level == 0 || !GateLight.givesLight(shape, x, y)) {
            return 0;
        }
        final double distance = Math.hypot(x, y - shape.centerY()) / shape.scale();
        final long light = Math.round(level + GateLight.HORIZON_SWING * GateLight.wave(time, distance));
        return (int) Math.max(1, Math.min(SGConfig.MAX_LIGHT, light));
    }

    /**
     * @param x across the gate, from its middle
     * @param y up from the base
     * @return whether a block of a gate's opening gives light while its horizon is up: every block of a standard
     *     gate's, and only some of a supergate's, spread across it
     */
    public static boolean givesLight(final GateShape shape, final int x, final int y) {
        if (!shape.isSupergate()) {
            return true;
        }
        final int spacing = Math.max(GateLight.SUPERGATE_SPACING, shape.radius() / 3);
        return Math.floorMod(x, spacing) == 0 && Math.floorMod(y - shape.radius(), spacing) == 0;
    }

    // The level open horizons light the world at, as the server sets it
    private static int level() {
        return SGServer.current().map(server -> server.config().eventHorizonLight())
                .orElse(SGConfig.DEFAULTS.eventHorizonLight());
    }

    /**
     * The waves running out across an open horizon, which its light follows, and its glow as clients draw it.
     *
     * @param time the game time, in ticks, which may have a fraction
     * @param distance how far from the horizon's center, in a standard gate's blocks
     * @return how high the wave is there, from -1 to 1
     */
    public static double wave(final double time, final double distance) {
        return Math.sin(2 * Math.PI * (time / GateLight.WAVE_TICKS - distance / GateLight.WAVE_LENGTH));
    }

    // The block of the ring nearest each chevron position, round the ring's center as far out as its blocks
    private static int[][] chevronBlocks(final GateShape shape) {
        return GateLight.CHEVRON_BLOCKS.computeIfAbsent(shape, key -> {
            final int[][] blocks = new int[ChevronOrder.POSITIONS][];
            for (int position = 0; position < ChevronOrder.POSITIONS; position++) {
                final double angle = Math.toRadians(ChevronOrder.angle(position));
                final double cx = shape.radius() * Math.cos(angle);
                final double cy = shape.centerY() + shape.radius() * Math.sin(angle);
                double best = Double.MAX_VALUE;
                for (int x = -shape.radius(); x <= shape.radius(); x++) {
                    for (int y = 0; y < shape.height(); y++) {
                        final double distance = Math.hypot(x - cx, y - cy);
                        if (shape.cellAt(x, y) != GateShape.EMPTY && distance < best) {
                            best = distance;
                            blocks[position] = new int[] {x, y};
                        }
                    }
                }
            }
            return blocks;
        });
    }
}
