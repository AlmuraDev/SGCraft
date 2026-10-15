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

package com.almuradev.sgcraft.mod.gate;

import net.minecraft.util.RandomSource;

/**
 * The surface of an open wormhole: a disc of rings and spokes whose points move along the gate's axis as waves, as in
 * the original SGCraft. The kawoosh is this surface thrown forward as the wormhole opens. Only clients simulate it.
 *
 * <p>The disc has {@link #RINGS} rings out from its center, each {@link #BAND} blocks wide, and {@link #SPOKES}
 * points round each ring. The outermost ring is held flat by the gate, and the center moves with the average of the
 * first ring.</p>
 */
public final class EventHorizon {

    /** How many rings the disc has out to the gate's edge, which stays flat. */
    public static final int RINGS = 5;
    /** How many points each ring has. */
    public static final int SPOKES = 32;
    /** How far apart the rings are, in blocks. */
    public static final double BAND = 2.0 / EventHorizon.RINGS;

    private static final double WAVE_SPEED_SQ = 0.03;
    private static final double DAMPING = 0.95;
    private static final double RANDOM_IMPULSE = 0.05;
    private static final double OPENING_IMPULSE = 1.3;
    private static final double OPENING_SPREAD = 0.25;
    private static final double CLOSING_IMPULSE = 0.25;

    // [spoke][ring] for spokes 0 to SPOKES + 1, where 0 and SPOKES + 1 are copies of SPOKES and 1, so the disc wraps
    private final double[][] position = new double[EventHorizon.SPOKES + 2][EventHorizon.RINGS + 1];
    private final double[][] velocity = new double[EventHorizon.SPOKES + 2][EventHorizon.RINGS + 1];
    private final double[][] previous = new double[EventHorizon.SPOKES + 2][EventHorizon.RINGS + 1];

    /** Throws the surface forward from its center: the kawoosh. */
    public void open(final RandomSource random) {
        for (int j = 1; j <= EventHorizon.SPOKES; j++) {
            this.velocity[j][0] = EventHorizon.OPENING_IMPULSE;
            this.velocity[j][1] = EventHorizon.OPENING_IMPULSE + EventHorizon.OPENING_SPREAD * random.nextGaussian();
        }
    }

    /** Stirs the whole surface as the wormhole shuts down. */
    public void close(final RandomSource random) {
        for (int j = 1; j <= EventHorizon.SPOKES; j++) {
            for (int i = 1; i < EventHorizon.RINGS; i++) {
                this.velocity[j][i] += EventHorizon.CLOSING_IMPULSE * random.nextGaussian();
            }
        }
    }

    /** Moves the surface on by a tick, with a small random ripple. */
    public void tick(final RandomSource random) {
        for (int j = 0; j < this.position.length; j++) {
            System.arraycopy(this.position[j], 0, this.previous[j], 0, this.position[j].length);
        }
        final int spoke = 1 + random.nextInt(EventHorizon.SPOKES);
        final int ring = 1 + random.nextInt(EventHorizon.RINGS - 1);
        this.velocity[spoke][ring] += EventHorizon.RANDOM_IMPULSE * random.nextGaussian();
        this.wrap();
        final double[][] u = this.position;
        for (int j = 1; j <= EventHorizon.SPOKES; j++) {
            for (int i = 1; i < EventHorizon.RINGS; i++) {
                final double radial = u[j][i + 1] - 2 * u[j][i] + u[j][i - 1]
                        + 0.5 * (u[j][i + 1] - u[j][i - 1]) / i;
                final double around = (u[j + 1][i] - 2 * u[j][i] + u[j - 1][i]) / (i * i);
                this.velocity[j][i] = EventHorizon.DAMPING * this.velocity[j][i]
                        + EventHorizon.WAVE_SPEED_SQ * (radial + around);
            }
        }
        for (int j = 1; j <= EventHorizon.SPOKES; j++) {
            for (int i = 1; i < EventHorizon.RINGS; i++) {
                u[j][i] += this.velocity[j][i];
            }
        }
        double center = 0;
        for (int j = 1; j <= EventHorizon.SPOKES; j++) {
            center += u[j][1];
        }
        center /= EventHorizon.SPOKES;
        for (int j = 1; j <= EventHorizon.SPOKES; j++) {
            u[j][0] = center;
        }
        this.wrap();
    }

    private void wrap() {
        for (final double[][] grid : new double[][][] {this.position, this.velocity}) {
            System.arraycopy(grid[EventHorizon.SPOKES], 0, grid[0], 0, grid[0].length);
            System.arraycopy(grid[1], 0, grid[EventHorizon.SPOKES + 1], 0, grid[0].length);
        }
    }

    /**
     * @param spoke a spoke, from 0 to {@link #SPOKES}, where 0 and {@link #SPOKES} are the same
     * @param ring a ring, from 0 at the center to {@link #RINGS} at the gate's edge
     * @param partialTick how far through the tick, from 0 to 1
     * @return how far forward of the gate that point of the surface is, in blocks
     */
    public double offset(final int spoke, final int ring, final float partialTick) {
        final int j = spoke == 0 ? EventHorizon.SPOKES : spoke;
        return this.previous[j][ring] + (this.position[j][ring] - this.previous[j][ring]) * partialTick;
    }
}
