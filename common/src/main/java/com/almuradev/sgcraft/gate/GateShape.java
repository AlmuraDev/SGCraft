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

import com.almuradev.sgcraft.api.gate.GateSize;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * How big a gate is and how it is laid out, as the original's {@code GateShape}: everything that depends on a gate's
 * size reads it from here. A standard gate is five blocks square; a supergate is a ring of blocks following a circle,
 * from {@link #MIN_SUPERGATE_RADIUS} to {@link #MAX_SUPERGATE_RADIUS} blocks from its center to its base.
 *
 * <p>Positions are in the gate's own frame (see {@link GateFrame}): the base is at the origin at the bottom center of
 * the ring, x runs to the gate's right as seen from in front, y runs up, and the event horizon is the plane z = 0. A
 * supergate is drawn as a standard gate scaled up to fit its blocks, so its details keep their proportions, but its
 * ring is only as thick as its blocks.</p>
 */
public final class GateShape {

    /** What a cell of the layout holds. */
    public static final int EMPTY = 0;
    public static final int RING = 1;
    public static final int CHEVRON = 2;

    /** The sizes of supergate, as the original's: smaller ones would be standard gates, bigger ones too heavy. */
    public static final int MIN_SUPERGATE_RADIUS = 4;
    public static final int MAX_SUPERGATE_RADIUS = 30;

    // A standard gate's drawn ring, from its center: its opening, and its outer edge
    private static final double STANDARD_RING_INNER = 2.0;
    private static final double STANDARD_RING_OUTER = 2.5;
    // How thick a standard gate's drawn ring is, and a supergate's, which is as thick as its blocks
    private static final double STANDARD_RING_DEPTH = 0.25;
    private static final double SUPERGATE_RING_DEPTH = 1.0;
    // Where the original draws the nine chevrons, in degrees anticlockwise from the right
    private static final double[] CHEVRON_ANGLES = {250, 210, 170, 130, 90, 50, 10, -30, -70};
    // Most blocks a supergate's kawoosh clears, so the biggest gates don't stall the server
    private static final int MAX_KAWOOSH_BLOCKS = 4096;

    /** A standard gate. */
    public static final GateShape STANDARD = new GateShape(GateSize.MIN_RADIUS, new int[][] {
            {2, 1, 2, 1, 2},
            {1, 0, 0, 0, 1},
            {2, 0, 0, 0, 2},
            {1, 0, 0, 0, 1},
            {2, 1, 0, 1, 2},
    }, 1.0, false);

    private static final GateShape[] SUPERGATES = new GateShape[GateShape.MAX_SUPERGATE_RADIUS + 1];

    private final int radius;
    // Rows from top to bottom; the base is the center of the bottom row
    private final int[][] pattern;
    private final double scale;
    private final boolean supergate;

    private GateShape(final int radius, final int[][] pattern, final double scale, final boolean supergate) {
        this.radius = radius;
        this.pattern = pattern;
        this.scale = scale;
        this.supergate = supergate;
    }

    /**
     * @param radius blocks from the ring's center to its base, from {@link #MIN_SUPERGATE_RADIUS} to
     *     {@link #MAX_SUPERGATE_RADIUS}
     * @return the supergate of that size; shapes are shared, so they can be compared with ==
     */
    public static synchronized GateShape supergate(final int radius) {
        if (radius < GateShape.MIN_SUPERGATE_RADIUS || radius > GateShape.MAX_SUPERGATE_RADIUS) {
            throw new IllegalArgumentException("A supergate's radius is from " + GateShape.MIN_SUPERGATE_RADIUS
                    + " to " + GateShape.MAX_SUPERGATE_RADIUS + ", not " + radius);
        }
        if (GateShape.SUPERGATES[radius] == null) {
            GateShape.SUPERGATES[radius] = new GateShape(radius, GateShape.circle(radius),
                    (radius + 0.5) / GateShape.STANDARD_RING_OUTER, true);
        }
        return GateShape.SUPERGATES[radius];
    }

    /** @return the shape of a gate of this size, if gates come in it */
    public static Optional<GateShape> of(final GateSize size) {
        if (size.radius() == GateSize.MIN_RADIUS) {
            return Optional.of(GateShape.STANDARD);
        }
        return size.radius() >= GateShape.MIN_SUPERGATE_RADIUS && size.radius() <= GateShape.MAX_SUPERGATE_RADIUS
                ? Optional.of(GateShape.supergate(size.radius())) : Optional.empty();
    }

    /** @return the shape of a gate whose ring is this many blocks from its center, a standard gate otherwise */
    public static GateShape ofRadius(final int radius) {
        return GateSize.of(radius).flatMap(GateShape::of).orElse(GateShape.STANDARD);
    }

    /** @return the API's size of this shape */
    public GateSize size() {
        return new GateSize(this.radius);
    }

    /** @return blocks from the ring's center to its base, and from its center to either side */
    public int radius() {
        return this.radius;
    }

    /** @return how many blocks tall the gate is */
    public int height() {
        return this.radius * 2 + 1;
    }

    public boolean isSupergate() {
        return this.supergate;
    }

    /** @return how far the ring's center is above the base's center */
    public double centerY() {
        return this.radius;
    }

    /** @return how much bigger the drawn gate is than a standard one, across its face */
    public double scale() {
        return this.scale;
    }

    /** @return how much thicker the drawn ring is than a standard gate's */
    public double depthScale() {
        return this.supergate ? GateShape.SUPERGATE_RING_DEPTH / GateShape.STANDARD_RING_DEPTH : 1.0;
    }

    /** @return how far the drawn ring's opening reaches from its center */
    public double ringInnerRadius() {
        return GateShape.STANDARD_RING_INNER * this.scale;
    }

    /**
     * @param x across the gate, from its middle
     * @param y up from the base
     * @return what the gate has there: {@link #EMPTY}, {@link #RING} or {@link #CHEVRON}; the base's own cell is empty
     */
    public int cellAt(final int x, final int y) {
        if (Math.abs(x) > this.radius || y < 0 || y >= this.height()) {
            return GateShape.EMPTY;
        }
        return this.pattern[this.height() - 1 - y][this.radius + x];
    }

    // The opening, which travelers are tracked and arrive in. A supergate's drawn ring covers much of the square
    // between its blocks, so its opening is the circle inside the drawn ring; a standard gate's is the gap between its
    // blocks. Heights are above the base's center.

    /** @return the floor of the opening */
    public double openingFloor() {
        return this.supergate ? Math.max(0.5, this.centerY() - this.ringInnerRadius()) : 0.5;
    }

    /** @return the top of the opening */
    public double openingTop() {
        return this.supergate ? this.centerY() + this.ringInnerRadius() : this.height() - 1.5;
    }

    /** @return half the opening's widest width */
    public double openingHalfWidth() {
        return this.supergate ? this.ringInnerRadius() : this.radius - 0.5;
    }

    /** @return half the opening's height */
    public double openingRadius() {
        return (this.openingTop() - this.openingFloor()) / 2;
    }

    /** @return half the opening's width at a height above the base's center */
    public double openingHalfWidthAt(final double y) {
        if (!this.supergate) {
            return this.openingHalfWidth();
        }
        final double dy = y - this.centerY();
        return Math.sqrt(Math.max(0, this.ringInnerRadius() * this.ringInnerRadius() - dy * dy));
    }

    /** @return whether a point, across from the gate's middle and up from its base, is in the opening */
    public boolean inOpening(final double x, final double y) {
        if (!this.supergate) {
            return Math.abs(x) <= this.openingHalfWidth();
        }
        final double dy = y - this.centerY();
        return x * x + dy * dy <= this.ringInnerRadius() * this.ringInnerRadius();
    }

    /** @return the near bottom left corner of the space the kawoosh hurts */
    public Vec3 kawooshMin() {
        return new Vec3(-(this.radius - 0.5), 0.5, 0.5);
    }

    /** @return the far top right corner of the space the kawoosh hurts */
    public Vec3 kawooshMax() {
        return this.supergate ? new Vec3(this.radius - 0.5, 2 * this.radius - 0.5, 2 * this.radius - 0.5)
                : new Vec3(1.5, 3.5, 5.5);
    }

    /** @return how many blocks out from the horizon the kawoosh clears */
    public int kawooshBlocksDeep() {
        return this.supergate ? (int) Math.ceil(2.5 * this.scale - 1) : 3;
    }

    /**
     * @param x across the gate, from its middle
     * @param y up from the base
     * @return whether a block there is in the kawoosh's way
     */
    public boolean inKawooshBore(final int x, final int y) {
        if (!this.supergate) {
            return Math.abs(x) <= 1 && y >= 1 && y <= 3;
        }
        final double bore = Math.max(this.ringInnerRadius(), this.radius - 0.5) - 0.25;
        return y >= 1 && Math.hypot(x, y - this.centerY()) <= bore;
    }

    /** @return most blocks the kawoosh clears at once */
    public int maxKawooshBlocks() {
        return GateShape.MAX_KAWOOSH_BLOCKS;
    }

    // A ring of blocks of the given radius: every block the circle passes through, with the ring blocks nearest each
    // chevron's angle as chevrons, as the original lays it out
    private static int[][] circle(final int radius) {
        final int size = 2 * radius + 1;
        final int[][] pattern = new int[size][size];
        for (int y = 0; y < size; y++) {
            for (int x = -radius; x <= radius; x++) {
                if (GateShape.circleCrossesCell(x, y - radius, radius)) {
                    pattern[size - 1 - y][radius + x] = GateShape.RING;
                }
            }
        }
        for (final double angle : GateShape.CHEVRON_ANGLES) {
            // Left hand chevrons mirror right hand ones, so ties between equally near blocks break the same way
            final double a = Math.toRadians(angle);
            final boolean mirror = Math.cos(a) < -1e-9;
            final double px = GateShape.round6(Math.abs(radius * Math.cos(a)));
            final double py = GateShape.round6(radius * Math.sin(a));
            int bestX = 0;
            int bestY = 0;
            double best = Double.MAX_VALUE;
            for (int y = 0; y < size; y++) {
                for (int x = -radius; x <= radius; x++) {
                    if (pattern[size - 1 - y][radius + x] == GateShape.RING) {
                        final double d = Math.hypot(x - px, y - radius - py);
                        if (d < best) {
                            best = d;
                            bestX = x;
                            bestY = y;
                        }
                    }
                }
            }
            pattern[size - 1 - bestY][radius + (mirror ? -bestX : bestX)] = GateShape.CHEVRON;
        }
        // The base's own cell
        pattern[size - 1][radius] = GateShape.EMPTY;
        return pattern;
    }

    private static double round6(final double x) {
        return Math.round(x * 1e6) / 1e6;
    }

    // Whether a circle about the origin passes through the unit cell centered on (cx, cy)
    private static boolean circleCrossesCell(final int cx, final int cy, final double r) {
        final double nx = Math.max(Math.abs(cx) - 0.5, 0);
        final double ny = Math.max(Math.abs(cy) - 0.5, 0);
        final double fx = Math.abs(cx) + 0.5;
        final double fy = Math.abs(cy) + 0.5;
        return Math.hypot(nx, ny) < r && Math.hypot(fx, fy) > r;
    }
}
