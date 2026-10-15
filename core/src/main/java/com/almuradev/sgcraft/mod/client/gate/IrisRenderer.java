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

package com.almuradev.sgcraft.mod.client.gate;

import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.api.gate.GateTypes;
import com.almuradev.sgcraft.mod.SGCraft;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Draws a gate's metal iris, as the original SGCraft did: twenty curved blades round the ring that swing in about
 * their pivots to close, with a low tower of overlapping blade tips rising at the middle as they meet. Drawn in the
 * gate's frame, centered on the ring, with its front towards +z.
 *
 * <p>The geometry is a port of gcewing's SGBaseTERenderer, its constants and formulas unchanged, but for one thing:
 * the blades stop at the edge of the ring's opening, which the original let them pass, so their ends showed against
 * the ring as a row of notches. Opening, they slide back into the ring there.</p>
 */
public final class IrisRenderer {

    private static final Map<ResourceKey<GateType>, Identifier> TEXTURES = Map.of(
            GateTypes.MILKY_WAY, SGCraft.id("textures/gate/milkyway/iris.png"),
            GateTypes.PEGASUS, SGCraft.id("textures/gate/pegasus/iris.png"),
            GateTypes.UNIVERSE, SGCraft.id("textures/gate/universe/iris.png"));

    // The edge of the ring's opening for each type of gate, which the blades stop at; a type not here lets them run on
    // under its ring
    private static final Map<ResourceKey<GateType>, Double> RIMS = Map.of(GateTypes.MILKY_WAY, 2.0,
            GateTypes.PEGASUS, 2.0);

    private static final int BLADES = 20;
    // A standard gate's ring's inner radius, which the blades are sized for
    private static final double RING_INNER_RADIUS = 2.0;
    private static final double SCALE = IrisRenderer.RING_INNER_RADIUS / 2.0;
    // The blades: how far each pivot is from the center, its length, its trailing edge, its width at the tip and at
    // the pivot, how far it swings to open, how wide it is open, how finely it is cut, and how much the middle swirls
    private static final double PIVOT = 2.31;
    private static final double LENGTH = 2.40;
    private static final double TRAILING = 1.85;
    private static final double TIP_WIDTH = 0.16;
    private static final double PIVOT_WIDTH = 1.00;
    private static final double SWING = 62;
    private static final double OPEN_WIDTH = 0.03;
    private static final int STEPS = 8;
    private static final double SWIRL = 40;
    private static final double Z0 = 0.1;
    private static final double Z1 = 0.01;
    // The tower at the middle: its radius, height, how wide its base is against its top, its twist, the extra height
    // at the center, the step between overlapping tips, their curve, and how finely it is cut
    private static final double TOWER_RADIUS = 0.8;
    private static final double TOWER_HEIGHT = 0.15;
    private static final double TOWER_BASE = 0.3;
    private static final double TOWER_TWIST = 60;
    private static final double TOWER_PEAK = 0.02;
    private static final double TOWER_STEP = 0.02;
    private static final double TOWER_CURVE = 50;
    private static final double TOWER_OVERLAP = 2.2;
    private static final int TOWER_ALONG = 10;
    private static final int TOWER_ACROSS = 4;
    private static final int TOWER_DOWN = 8;

    private IrisRenderer() {
    }

    /**
     * Draws the iris.
     *
     * @param aperture how far open it is, from 0 shut to 1 open; a fully open iris isn't drawn
     */
    public static void submit(final ResourceKey<GateType> type, final float aperture, final PoseStack poseStack,
            final SubmitNodeCollector collector, final int light) {
        if (aperture >= 1) {
            return;
        }
        final float[] triangles = IrisRenderer.build(aperture,
                IrisRenderer.RIMS.getOrDefault(type, Double.MAX_VALUE));
        final Identifier texture = IrisRenderer.TEXTURES.getOrDefault(type,
                IrisRenderer.TEXTURES.get(GateTypes.MILKY_WAY));
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(texture),
                (pose, buffer) -> GateGeometry.emit(pose, buffer, triangles, -1, light));
    }

    // Cached for the shut iris, which is drawn most, by the rim it stops at
    private static final Map<Double, float[]> CLOSED = new ConcurrentHashMap<>();

    static float[] build(final double aperture, final double rim) {
        if (aperture <= 0) {
            return IrisRenderer.CLOSED.computeIfAbsent(rim, r -> IrisRenderer.make(0, r));
        }
        return IrisRenderer.make(aperture, rim);
    }

    private static float[] make(final double aperture, final double rim) {
        final GateGeometry.Builder builder = new GateGeometry.Builder();
        IrisRenderer.blades(builder, aperture, rim);
        IrisRenderer.tower(builder, aperture);
        return builder.build();
    }

    private static void blades(final GateGeometry.Builder builder, final double a, final double rim) {
        final double h = IrisRenderer.PIVOT_WIDTH - (IrisRenderer.PIVOT_WIDTH - IrisRenderer.OPEN_WIDTH) * a * a;
        final double z0 = IrisRenderer.Z0;
        final double z1 = IrisRenderer.Z1;
        final double[] tip = {-IrisRenderer.LENGTH, 0, z0, 0, 0};
        final double[] edge = {-IrisRenderer.LENGTH + IrisRenderer.TRAILING, h, z0,
                IrisRenderer.TRAILING / IrisRenderer.LENGTH, h / IrisRenderer.PIVOT_WIDTH};
        final double[] tail = {-IrisRenderer.LENGTH, h, z0, 0, h};
        final double[] front1 = {0, 0, z0 + z1, 1, 0};
        final double[] front2 = {0, IrisRenderer.TIP_WIDTH, z0 + z1, 1, IrisRenderer.TIP_WIDTH};
        final double[] back1 = {0, 0, z0 - z1, 1, 0};
        final double[] back2 = {0, IrisRenderer.TIP_WIDTH, z0 - z1, 1, IrisRenderer.TIP_WIDTH};
        final double[][][] triangles = {
                {tip, front1, front2}, {tip, front2, edge}, {tip, edge, tail},
                {tip, tail, edge}, {tip, edge, back2}, {tip, back2, back1}};
        final double sw = Math.toRadians(-a * IrisRenderer.SWING);
        for (int blade = 0; blade < IrisRenderer.BLADES; blade++) {
            final double t = Math.toRadians(360.0 * blade / IrisRenderer.BLADES);
            for (final double[][] triangle : triangles) {
                IrisRenderer.subdivide(builder, triangle, sw, t, rim);
            }
        }
    }

    // A triangle cut into a fine grid, so the swirl can bend it
    private static void subdivide(final GateGeometry.Builder builder, final double[][] tri, final double sw,
            final double t, final double rim) {
        final int n = IrisRenderer.STEPS;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n - i; j++) {
                IrisRenderer.corner(builder, tri, i, j, sw, t, rim);
                IrisRenderer.corner(builder, tri, i + 1, j, sw, t, rim);
                IrisRenderer.corner(builder, tri, i, j + 1, sw, t, rim);
                if (j < n - 1 - i) {
                    IrisRenderer.corner(builder, tri, i + 1, j, sw, t, rim);
                    IrisRenderer.corner(builder, tri, i + 1, j + 1, sw, t, rim);
                    IrisRenderer.corner(builder, tri, i, j + 1, sw, t, rim);
                }
            }
        }
    }

    private static void corner(final GateGeometry.Builder builder, final double[][] tri, final int i, final int j,
            final double sw, final double t, final double rim) {
        final double b = (double) i / IrisRenderer.STEPS;
        final double c = (double) j / IrisRenderer.STEPS;
        final double a = 1 - b - c;
        final double[] p = new double[5];
        for (int k = 0; k < 5; k++) {
            p[k] = a * tri[0][k] + b * tri[1][k] + c * tri[2][k];
        }
        // Swing about the pivot, turn into place, then swirl towards the middle
        final double x = IrisRenderer.PIVOT + p[0] * Math.cos(sw) - p[1] * Math.sin(sw);
        final double y = p[0] * Math.sin(sw) + p[1] * Math.cos(sw);
        final double gx = x * Math.cos(t) - y * Math.sin(t);
        final double gy = x * Math.sin(t) + y * Math.cos(t);
        final double d = Math.hypot(gx, gy);
        final double angle = Math.atan2(gy, gx) + IrisRenderer.swirl(d);
        // Whatever lies past the rim is drawn on it, so the blades end in a circle there
        final double r = Math.min(d * IrisRenderer.SCALE, rim);
        builder.corner(r * Math.cos(angle), r * Math.sin(angle), p[2], p[3], p[4], 0, 0, 1);
    }

    private static double swirl(final double d) {
        return Math.toRadians(IrisRenderer.SWIRL * Math.max(0, 1 - d / 2.0));
    }

    // The tower of overlapping blade tips at the middle, rising as the iris shuts
    private static void tower(final GateGeometry.Builder builder, final double a) {
        final double rise = 1 - a;
        if (rise <= 0) {
            return;
        }
        final double step = IrisRenderer.TOWER_RADIUS / IrisRenderer.TOWER_ALONG;
        for (int b = 0; b < IrisRenderer.BLADES; b++) {
            for (int i = 0; i < IrisRenderer.TOWER_ALONG; i++) {
                final double r1 = step * i;
                final double r2 = step * (i + 1);
                for (int j = 0; j < IrisRenderer.TOWER_ACROSS; j++) {
                    final double s1 = (double) j / IrisRenderer.TOWER_ACROSS;
                    final double s2 = (double) (j + 1) / IrisRenderer.TOWER_ACROSS;
                    IrisRenderer.quad(builder, new double[][] {
                            IrisRenderer.hub(b, r1, s1, 0, a, rise), IrisRenderer.hub(b, r2, s1, 0, a, rise),
                            IrisRenderer.hub(b, r2, s2, 0, a, rise), IrisRenderer.hub(b, r1, s2, 0, a, rise)},
                            new double[] {0, 0, 1});
                }
                for (int edge = 0; edge <= 1; edge++) {
                    final double side = edge == 0 ? 1 : -1;
                    for (int k = 0; k < IrisRenderer.TOWER_DOWN; k++) {
                        final double d1 = (double) k / IrisRenderer.TOWER_DOWN;
                        final double d2 = (double) (k + 1) / IrisRenderer.TOWER_DOWN;
                        final double[] q1 = IrisRenderer.hub(b, r1, edge, d1, a, rise);
                        final double[] q2 = IrisRenderer.hub(b, r2, edge, d1, a, rise);
                        final double ang = Math.atan2(q1[1] + q2[1], q1[0] + q2[0]);
                        IrisRenderer.quad(builder, new double[][] {q1, q2,
                                IrisRenderer.hub(b, r2, edge, d2, a, rise), IrisRenderer.hub(b, r1, edge, d2, a, rise)},
                                new double[] {side * Math.sin(ang), -side * Math.cos(ang), 0});
                    }
                }
            }
        }
    }

    private static double[] hub(final int b, final double rho, final double across, final double down,
            final double a, final double rise) {
        final double spacing = 360.0 / IrisRenderer.BLADES;
        final double widest = spacing * IrisRenderer.TOWER_OVERLAP;
        final double t = rho / IrisRenderer.TOWER_RADIUS;
        final double f = 1 - t;
        final double phi = across * widest * (1 - t * t);
        final double step = IrisRenderer.TOWER_STEP * (IrisRenderer.TOWER_BASE + (1 - IrisRenderer.TOWER_BASE) * f);
        final double top = IrisRenderer.TOWER_HEIGHT + IrisRenderer.TOWER_PEAK * f + step * (1 - phi / widest);
        final double z = IrisRenderer.Z0 + IrisRenderer.SCALE * (0.005 + rise * top * (1 - down));
        final double up = 1 - down;
        final double r = rho * (IrisRenderer.TOWER_BASE + (1 - IrisRenderer.TOWER_BASE) * up * up);
        final double sw = Math.toRadians(a * IrisRenderer.SWING);
        final double hole = IrisRenderer.PIVOT * Math.sin(sw);
        final double dist = hole + r * rise;
        final double at = Math.toRadians(b * spacing + IrisRenderer.TOWER_CURVE * f + phi
                + IrisRenderer.TOWER_TWIST * down) + IrisRenderer.swirl(dist) - sw;
        return new double[] {dist * Math.cos(at) * IrisRenderer.SCALE, dist * Math.sin(at) * IrisRenderer.SCALE, z,
                t, across + down};
    }

    // A quad as two triangles, turned so its face points the given way
    private static void quad(final GateGeometry.Builder builder, final double[][] q, final double[] facing) {
        final double[] d1 = {q[2][0] - q[0][0], q[2][1] - q[0][1], q[2][2] - q[0][2]};
        final double[] d2 = {q[3][0] - q[1][0], q[3][1] - q[1][1], q[3][2] - q[1][2]};
        final double nx = d1[1] * d2[2] - d1[2] * d2[1];
        final double ny = d1[2] * d2[0] - d1[0] * d2[2];
        final double nz = d1[0] * d2[1] - d1[1] * d2[0];
        final boolean flip = nx * facing[0] + ny * facing[1] + nz * facing[2] < 0;
        final int[] order = flip ? new int[] {0, 3, 2, 0, 2, 1} : new int[] {0, 1, 2, 0, 2, 3};
        final double length = Math.max(1e-9, Math.sqrt(nx * nx + ny * ny + nz * nz));
        final double sign = flip ? -1 : 1;
        for (final int k : order) {
            builder.corner(q[k][0], q[k][1], q[k][2], q[k][3], q[k][4], sign * nx / length, sign * ny / length,
                    sign * nz / length);
        }
    }
}
