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

package com.almuradev.sgcraft.client.gate;

import com.almuradev.sgcraft.gate.ChevronSpacing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Triangles of a gate, kept as flat arrays and drawn through an entity render type.
 *
 * <p>Each triangle is {@link #FLOATS} floats: three corners of x, y, z, u, v, then that corner's normal x, y, z.</p>
 */
public final class GateGeometry {

    /** How many floats each triangle takes. */
    public static final int FLOATS = 24;

    private GateGeometry() {
    }

    /**
     * Draws triangles, each as a quad with its last corner repeated, since entity render types draw quads.
     *
     * @param color the color every corner is tinted, as ARGB
     * @param light the packed light every corner is lit by
     */
    public static void emit(final PoseStack.Pose pose, final VertexConsumer buffer, final float[] triangles,
            final int color, final int light) {
        for (int t = 0; t < triangles.length; t += GateGeometry.FLOATS) {
            for (int corner = 0; corner < 4; corner++) {
                final int c = t + 8 * Math.min(corner, 2);
                buffer.addVertex(pose, triangles[c], triangles[c + 1], triangles[c + 2])
                        .setColor(color)
                        .setUv(triangles[c + 3], triangles[c + 4])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light)
                        .setNormal(pose, triangles[c + 5], triangles[c + 6], triangles[c + 7]);
            }
        }
    }

    /**
     * Triangles with every corner moved round the gate's center as {@link ChevronSpacing#squeeze} moves its angle,
     * their normals turned with them. Each is made once for each spacing and kept, so it is the same array each time.
     *
     * @param triangles triangles about the gate's center, which is the origin, the gate facing +z
     * @param spacing how far apart the chevrons stand
     */
    public static float[] squeezed(final float[] triangles, final double spacing) {
        if (spacing == ChevronSpacing.STANDARD) {
            return triangles;
        }
        return GateGeometry.SQUEEZED.computeIfAbsent(triangles, key -> new HashMap<>())
                .computeIfAbsent(spacing, key -> GateGeometry.squeeze(triangles, spacing));
    }

    private static final Map<float[], Map<Double, float[]>> SQUEEZED = new IdentityHashMap<>();

    private static float[] squeeze(final float[] triangles, final double spacing) {
        final float[] out = triangles.clone();
        for (int c = 0; c < out.length; c += 8) {
            final double x = out[c];
            final double y = out[c + 1];
            if (x == 0 && y == 0) {
                continue;
            }
            final double angle = Math.toDegrees(Math.atan2(y, x));
            final double turn = Math.toRadians(ChevronSpacing.squeeze(angle, spacing) - angle);
            GateGeometry.turn(out, c, turn);
            GateGeometry.turn(out, c + 5, turn);
        }
        return out;
    }

    /** @return the triangles turned anticlockwise about the gate's center, seen from in front */
    public static float[] turned(final float[] triangles, final double degrees) {
        final float[] out = triangles.clone();
        final double turn = Math.toRadians(degrees);
        for (int c = 0; c < out.length; c += 8) {
            GateGeometry.turn(out, c, turn);
            GateGeometry.turn(out, c + 5, turn);
        }
        return out;
    }

    /** @return every one of the arrays' triangles in one */
    public static float[] joined(final List<float[]> parts) {
        int size = 0;
        for (final float[] part : parts) {
            size += part.length;
        }
        final float[] out = new float[size];
        int at = 0;
        for (final float[] part : parts) {
            System.arraycopy(part, 0, out, at, part.length);
            at += part.length;
        }
        return out;
    }

    // Turns the x and y at an index about the z axis
    private static void turn(final float[] data, final int at, final double radians) {
        final double cos = Math.cos(radians);
        final double sin = Math.sin(radians);
        final double x = data[at];
        final double y = data[at + 1];
        data[at] = (float) (x * cos - y * sin);
        data[at + 1] = (float) (x * sin + y * cos);
    }

    /** Collects triangles into an array. */
    public static final class Builder {

        private float[] data = new float[GateGeometry.FLOATS * 64];
        private int size;

        /** Adds one corner; three make a triangle. */
        public Builder corner(final double x, final double y, final double z, final double u, final double v,
                final double nx, final double ny, final double nz) {
            if (this.size + 8 > this.data.length) {
                this.data = Arrays.copyOf(this.data, this.data.length * 2);
            }
            this.data[this.size++] = (float) x;
            this.data[this.size++] = (float) y;
            this.data[this.size++] = (float) z;
            this.data[this.size++] = (float) u;
            this.data[this.size++] = (float) v;
            this.data[this.size++] = (float) nx;
            this.data[this.size++] = (float) ny;
            this.data[this.size++] = (float) nz;
            return this;
        }

        public float[] build() {
            return Arrays.copyOf(this.data, this.size);
        }
    }
}
