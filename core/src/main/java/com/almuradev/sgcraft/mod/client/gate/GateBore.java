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

package com.almuradev.sgcraft.mod.client.gate;

/**
 * The inside of a gate's ring, where an iris or a shield across its opening stops, in the gate's frame at the size of
 * a standard gate: how far it is from the center, and how far forward its front edge is.
 *
 * @param radius how far the bore is from the center
 * @param front how far forward of the gate's middle the bore's front edge is
 */
public record GateBore(double radius, double front) {

    /** A bore for a gate whose ring's own isn't known: a standard gate's opening, and an iris where 1.12 drew it. */
    public static final GateBore STANDARD = new GateBore(2.0, 0.11);

    // How near the bore a corner of the ring is to count as on it, in blocks
    private static final double ON_BORE = 0.02;

    /**
     * @param triangles the ring's triangles, as {@link GateGeometry} lays them out
     * @return the bore of a ring: its corners nearest the center, and the furthest forward of those; a ring with no
     *     triangles has {@link #STANDARD}'s
     */
    public static GateBore of(final float[] triangles) {
        double radius = Double.MAX_VALUE;
        for (int c = 0; c < triangles.length; c += GateBore.CORNER) {
            radius = Math.min(radius, Math.hypot(triangles[c], triangles[c + 1]));
        }
        if (radius == Double.MAX_VALUE) {
            return GateBore.STANDARD;
        }
        double front = -Double.MAX_VALUE;
        for (int c = 0; c < triangles.length; c += GateBore.CORNER) {
            if (Math.hypot(triangles[c], triangles[c + 1]) <= radius + GateBore.ON_BORE) {
                front = Math.max(front, triangles[c + 2]);
            }
        }
        return new GateBore(radius, front);
    }

    // The floats of each corner
    private static final int CORNER = GateGeometry.FLOATS / 3;
}
