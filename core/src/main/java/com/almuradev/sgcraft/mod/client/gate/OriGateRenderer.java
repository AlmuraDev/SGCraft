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

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.gate.OriGate;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Draws an Ori supergate, as in SGCraft 2.2.0: a ring of separate metal segments, each with a spine down its faces and
 * a light housing at its inner end, and no chevrons. As the gate dials, its segments link one after another,
 * anticlockwise from the top: each linked segment glows, a band of energy joins it to its linked neighbors, and arcs
 * crackle across the gaps between segments still forming.
 *
 * <p>The gate is drawn in the frame of a standard gate, which its supergate scales up; see
 * {@link StargateBaseRenderer}.</p>
 */
public final class OriGateRenderer implements GateTypeRenderer {

    private static final Identifier METAL = SGCraft.id("textures/gate/ori/ring.png");

    // The ring in a standard gate's frame: the inner edge of the segments, the outer edge the band reaches toward,
    // and how thick the segments are
    private static final double INNER = 2.0;
    private static final double OUTER = 2.5 * 1.15;
    private static final double METAL_END = OriGateRenderer.INNER
            + 0.5 * (OriGateRenderer.OUTER - OriGateRenderer.INNER);
    private static final double HALF_DEPTH = 0.25 / 4;
    // How much of the space round the ring each segment takes, and its spine and light housing
    private static final double WIDTH = 0.45;
    private static final double SPINE = 0.55;
    private static final double SPINE_PROUD = 0.06 * 0.25;
    private static final double SPINE_START = 0.08;
    private static final double NOSE = 0.12;
    private static final double NOSE_SECTION = 0.7;
    // The energy band: where it starts out along the segment, how far past the metal it reaches, how much thicker
    // than the segments it is, and how finely each piece is cut
    private static final double BAND_START = 0.5;
    private static final double BAND_PAST = 0.04;
    private static final double BAND_DEPTH = 1.3;
    private static final int BAND_STEPS = 8;
    // The arcs: how many pieces each bolt has, how far it wanders, and how wide its glow, core and branches are
    private static final int BOLT_PIECES = 9;
    private static final double WANDER = 0.35;
    private static final double GLOW_WIDTH = 0.05;
    private static final double CORE_WIDTH = 0.016;
    private static final double BRANCH_WIDTH = 0.012;
    // The colors: the band's inner edge and body, the spines, the light housings and the arcs' cores and glow
    private static final float[] RIM = {0.75F, 0.9F, 1.0F};
    private static final float[] BODY = {0.35F * 0.6F, 0.65F * 0.6F, 1.0F * 0.6F};
    private static final float[] SPINE_COLOR = {0.55F, 0.8F, 1.0F};
    private static final float[] ARC_CORE = {0.9F, 0.97F, 1.0F};
    private static final float[] ARC_GLOW = {0.35F * 0.6F, 0.6F * 0.6F, 1.0F * 0.6F};
    private static final int GLOW = ARGB.color(140, 205, 255);

    // The metal of each ring, by how many segments it has
    private static final Map<Integer, float[]> RINGS = new ConcurrentHashMap<>();

    @Override
    public void submit(final StargateBaseRenderState state, final PoseStack poseStack,
            final SubmitNodeCollector collector) {
        final int segments = OriGate.segments(state.shape);
        final float[] metal = OriGateRenderer.RINGS.computeIfAbsent(segments, OriGateRenderer::metal);
        GateFeatureRenderer.submit(collector, poseStack.last(), OriGateRenderer.METAL, GateMesh.of(metal),
                ARGB.white(1.0F), state.lightCoords);
        state.glows.clear();
        final double reach = OriGate.reach(state.view, state.time, segments);
        if (reach <= 0) {
            return;
        }
        final double[] glow = new double[segments];
        for (int k = 0; k < segments; k++) {
            glow[k] = OriGate.glow(reach, k);
        }
        final double time = state.time;
        final int seed = state.blockPos.hashCode();
        collector.submitCustomGeometry(poseStack, GateGlow.SHIELD_TYPE, (pose, buffer) -> {
            for (int k = 0; k < segments; k++) {
                if (glow[k] > 0) {
                    OriGateRenderer.lights(pose, buffer, k, segments, glow[k], time);
                    OriGateRenderer.band(pose, buffer, k, segments, glow, time);
                }
            }
            final Random random = new Random();
            for (int k = 0; k < segments; k++) {
                final int next = (k + 1) % segments;
                final double strength = Math.max(glow[k], glow[next]);
                if (strength <= 0) {
                    continue;
                }
                random.setSeed((long) Math.floor(time / 2) * 7919L + k * 104729L + seed);
                final boolean forming = glow[k] < 1 || glow[next] < 1 || glow[k] != glow[next];
                final int bolts = forming ? 3 : 1 + random.nextInt(2);
                for (int b = 0; b < bolts; b++) {
                    OriGateRenderer.bolt(pose, buffer, k, segments, strength, random);
                }
            }
        });
    }

    @Override
    public int glowColor() {
        return OriGateRenderer.GLOW;
    }

    // Segment k's angle round the ring: the top first, then on anticlockwise
    private static double angle(final int k, final int segments) {
        return Math.PI / 2 + 2 * Math.PI * k / segments;
    }

    // Half the width of a segment, across the ring
    private static double halfWidth(final int segments) {
        return Math.PI * (OriGateRenderer.INNER + OriGateRenderer.OUTER) / 2 / segments * OriGateRenderer.WIDTH;
    }

    // The segments' metal: each a block from the ring's inner edge outward, a spine standing out of each face and a
    // housing for its light at its inner end
    private static float[] metal(final int segments) {
        final GateGeometry.Builder builder = new GateGeometry.Builder();
        final double hw = OriGateRenderer.halfWidth(segments);
        final double hz = OriGateRenderer.HALF_DEPTH;
        final double spineFrom = OriGateRenderer.INNER + OriGateRenderer.SPINE_START * 2 * hw;
        for (int k = 0; k < segments; k++) {
            final double angle = OriGateRenderer.angle(k, segments);
            OriGateRenderer.box(builder, angle, OriGateRenderer.INNER, OriGateRenderer.METAL_END, hw, -hz, hz);
            OriGateRenderer.box(builder, angle, spineFrom, OriGateRenderer.METAL_END, hw * OriGateRenderer.SPINE, hz,
                    hz + OriGateRenderer.SPINE_PROUD);
            OriGateRenderer.box(builder, angle, spineFrom, OriGateRenderer.METAL_END, hw * OriGateRenderer.SPINE,
                    -hz - OriGateRenderer.SPINE_PROUD, -hz);
            OriGateRenderer.box(builder, angle, OriGateRenderer.INNER - OriGateRenderer.NOSE * 2 * hw,
                    OriGateRenderer.INNER, hw * OriGateRenderer.NOSE_SECTION, -hz * OriGateRenderer.NOSE_SECTION,
                    hz * OriGateRenderer.NOSE_SECTION);
        }
        return builder.build();
    }

    // A block along the ring's radius at an angle: out from r0 to r1, hw either side of the radius, from z0 to z1
    private static void box(final GateGeometry.Builder builder, final double angle, final double r0, final double r1,
            final double hw, final double z0, final double z1) {
        final double[] out = {Math.cos(angle), Math.sin(angle), 0};
        final double[] side = {-Math.sin(angle), Math.cos(angle), 0};
        final double[][] c = new double[8][];
        for (int i = 0; i < 8; i++) {
            final double r = (i & 1) == 0 ? r0 : r1;
            final double t = (i & 2) == 0 ? -hw : hw;
            final double z = (i & 4) == 0 ? z0 : z1;
            c[i] = new double[] {out[0] * r + side[0] * t, out[1] * r + side[1] * t, z};
        }
        // Each face by its corners, anticlockwise seen from outside, and the way it faces
        OriGateRenderer.face(builder, c[4], c[5], c[7], c[6], 0, 0, 1);
        OriGateRenderer.face(builder, c[0], c[2], c[3], c[1], 0, 0, -1);
        OriGateRenderer.face(builder, c[1], c[3], c[7], c[5], out[0], out[1], 0);
        OriGateRenderer.face(builder, c[0], c[4], c[6], c[2], -out[0], -out[1], 0);
        OriGateRenderer.face(builder, c[2], c[6], c[7], c[3], side[0], side[1], 0);
        OriGateRenderer.face(builder, c[0], c[1], c[5], c[4], -side[0], -side[1], 0);
    }

    // A quad as two triangles, its texture laid on in the plane it faces, repeating every two blocks of a standard
    // gate
    private static void face(final GateGeometry.Builder builder, final double[] a, final double[] b, final double[] c,
            final double[] d, final double nx, final double ny, final double nz) {
        for (final double[] p : new double[][] {a, b, c, a, c, d}) {
            final double u;
            final double v;
            if (Math.abs(nz) > 0.5) {
                u = p[0] / 2;
                v = p[1] / 2;
            } else {
                u = (p[0] * -ny + p[1] * nx) / 2;
                v = p[2] / 2;
            }
            builder.corner(p[0], p[1], p[2], u, v, nx, ny, nz);
        }
    }

    // A linked segment's lights: its spines glowing from its inner end, and its housing's light, pulsing
    private static void lights(final PoseStack.Pose pose, final VertexConsumer buffer, final int k,
            final int segments, final double glow, final double time) {
        final double angle = OriGateRenderer.angle(k, segments);
        final double hw = OriGateRenderer.halfWidth(segments) * OriGateRenderer.SPINE;
        final double pulse = (0.85 + 0.15 * Math.sin(time * 0.3 + 1.7 * k)) * glow;
        final double from = OriGateRenderer.INNER + OriGateRenderer.SPINE_START * 2
                * OriGateRenderer.halfWidth(segments);
        final double z = OriGateRenderer.HALF_DEPTH + OriGateRenderer.SPINE_PROUD + 0.002;
        final int inner = OriGateRenderer.color(OriGateRenderer.SPINE_COLOR, pulse);
        final int outer = OriGateRenderer.color(OriGateRenderer.SPINE_COLOR, pulse * 0.35);
        for (final double face : new double[] {z, -z}) {
            OriGateRenderer.vertex(pose, buffer, angle, from, -hw, face, inner);
            OriGateRenderer.vertex(pose, buffer, angle, OriGateRenderer.METAL_END, -hw, face, outer);
            OriGateRenderer.vertex(pose, buffer, angle, OriGateRenderer.METAL_END, hw, face, outer);
            OriGateRenderer.vertex(pose, buffer, angle, from, hw, face, inner);
        }
        // The housing's face toward the ring's middle
        final double nose = OriGateRenderer.INNER - OriGateRenderer.NOSE * 2 * OriGateRenderer.halfWidth(segments)
                - 0.002;
        final double nw = OriGateRenderer.halfWidth(segments) * OriGateRenderer.NOSE_SECTION;
        final double nz = OriGateRenderer.HALF_DEPTH * OriGateRenderer.NOSE_SECTION;
        final int core = OriGateRenderer.color(OriGateRenderer.ARC_CORE, pulse);
        OriGateRenderer.radial(pose, buffer, angle, nose, -nw, -nz, core);
        OriGateRenderer.radial(pose, buffer, angle, nose, nw, -nz, core);
        OriGateRenderer.radial(pose, buffer, angle, nose, nw, nz, core);
        OriGateRenderer.radial(pose, buffer, angle, nose, -nw, nz, core);
    }

    // The band of energy over a linked segment's outer half, reaching to meet its linked neighbors, on both faces
    private static void band(final PoseStack.Pose pose, final VertexConsumer buffer, final int k, final int segments,
            final double[] glow, final double time) {
        final double from = OriGateRenderer.INNER + OriGateRenderer.BAND_START
                * (OriGateRenderer.METAL_END - OriGateRenderer.INNER);
        final double to = OriGateRenderer.METAL_END + OriGateRenderer.BAND_PAST
                * (OriGateRenderer.METAL_END - OriGateRenderer.INNER);
        final double z = OriGateRenderer.HALF_DEPTH * OriGateRenderer.BAND_DEPTH;
        final double shimmer = 0.75 + 0.25 * Math.sin(time * 0.25 + 2.3 * k) * Math.sin(time * 0.11 + k);
        final double middle = OriGateRenderer.angle(k, segments);
        final double half = Math.PI / segments;
        for (int step = 0; step < OriGateRenderer.BAND_STEPS; step++) {
            final double t0 = -1 + 2.0 * step / OriGateRenderer.BAND_STEPS;
            final double t1 = -1 + 2.0 * (step + 1) / OriGateRenderer.BAND_STEPS;
            final double b0 = OriGateRenderer.bandBrightness(glow, k, segments, t0) * shimmer;
            final double b1 = OriGateRenderer.bandBrightness(glow, k, segments, t1) * shimmer;
            final double a0 = middle + half * t0;
            final double a1 = middle + half * t1;
            for (final double face : new double[] {z, -z}) {
                OriGateRenderer.polar(pose, buffer, a0, from, face, OriGateRenderer.color(OriGateRenderer.RIM, b0));
                OriGateRenderer.polar(pose, buffer, a0, to, face, OriGateRenderer.color(OriGateRenderer.BODY, b0));
                OriGateRenderer.polar(pose, buffer, a1, to, face, OriGateRenderer.color(OriGateRenderer.BODY, b1));
                OriGateRenderer.polar(pose, buffer, a1, from, face, OriGateRenderer.color(OriGateRenderer.RIM, b1));
            }
            // Its outer edge, dimmer
            final int edge0 = OriGateRenderer.color(OriGateRenderer.BODY, b0 * 0.6);
            final int edge1 = OriGateRenderer.color(OriGateRenderer.BODY, b1 * 0.6);
            OriGateRenderer.polar(pose, buffer, a0, to, -z, edge0);
            OriGateRenderer.polar(pose, buffer, a0, to, z, edge0);
            OriGateRenderer.polar(pose, buffer, a1, to, z, edge1);
            OriGateRenderer.polar(pose, buffer, a1, to, -z, edge1);
        }
    }

    // How bright the band is across a segment, at t from -1 at its trailing side to 1 at its leading side: its own
    // glow in the middle, falling to its neighbor's at the edge, so it only joins linked segments
    private static double bandBrightness(final double[] glow, final int k, final int segments, final double t) {
        final int neighbor = t < 0 ? (k - 1 + segments) % segments : (k + 1) % segments;
        final double edge = Math.min(glow[k], glow[neighbor]);
        return (glow[k] + (edge - glow[k]) * Math.abs(t)) * 0.55;
    }

    // One bolt of an arc across the gap from segment k to the next, wandering as it goes, with a glow round it and
    // now and then a branch
    private static void bolt(final PoseStack.Pose pose, final VertexConsumer buffer, final int k, final int segments,
            final double strength, final Random random) {
        final double hw = OriGateRenderer.halfWidth(segments);
        final double bandInner = OriGateRenderer.INNER + OriGateRenderer.BAND_START
                * (OriGateRenderer.METAL_END - OriGateRenderer.INNER);
        final double radius = OriGateRenderer.INNER + (bandInner - OriGateRenderer.INNER)
                * (0.15 + 0.7 * random.nextDouble());
        final double from = OriGateRenderer.angle(k, segments) + hw / radius;
        final double to = OriGateRenderer.angle(k + 1, segments) - hw / radius;
        final double[][] points = new double[OriGateRenderer.BOLT_PIECES + 1][];
        final double length = radius * (to - from);
        for (int i = 0; i <= OriGateRenderer.BOLT_PIECES; i++) {
            final double t = (double) i / OriGateRenderer.BOLT_PIECES;
            final double wander = OriGateRenderer.WANDER * length * Math.sin(Math.PI * t);
            final double r = radius + wander * (random.nextDouble() * 2 - 1);
            final double z = wander * 0.5 * (random.nextDouble() * 2 - 1) * OriGateRenderer.HALF_DEPTH;
            final double a = from + (to - from) * t;
            points[i] = new double[] {r * Math.cos(a), r * Math.sin(a), z};
        }
        final int glow = OriGateRenderer.color(OriGateRenderer.ARC_GLOW, strength);
        final int core = OriGateRenderer.color(OriGateRenderer.ARC_CORE, strength);
        OriGateRenderer.line(pose, buffer, points, OriGateRenderer.GLOW_WIDTH, glow);
        OriGateRenderer.line(pose, buffer, points, OriGateRenderer.CORE_WIDTH, core);
        if (random.nextBoolean()) {
            final int at = 2 + random.nextInt(OriGateRenderer.BOLT_PIECES - 4);
            final double[][] branch = new double[4][];
            branch[0] = points[at];
            for (int i = 1; i < branch.length; i++) {
                branch[i] = new double[] {branch[i - 1][0] + (random.nextDouble() - 0.5) * length * 0.25,
                        branch[i - 1][1] + (random.nextDouble() - 0.5) * length * 0.25, branch[i - 1][2]};
            }
            OriGateRenderer.line(pose, buffer, branch, OriGateRenderer.BRANCH_WIDTH, core);
        }
    }

    // A wandering line through points, as a strip of quads across the gate's face
    private static void line(final PoseStack.Pose pose, final VertexConsumer buffer, final double[][] points,
            final double width, final int color) {
        for (int i = 0; i + 1 < points.length; i++) {
            final double[] a = points[i];
            final double[] b = points[i + 1];
            final double dx = b[0] - a[0];
            final double dy = b[1] - a[1];
            final double length = Math.max(1e-6, Math.hypot(dx, dy));
            final double ox = -dy / length * width / 2;
            final double oy = dx / length * width / 2;
            OriGateRenderer.raw(pose, buffer, a[0] - ox, a[1] - oy, a[2], color);
            OriGateRenderer.raw(pose, buffer, b[0] - ox, b[1] - oy, b[2], color);
            OriGateRenderer.raw(pose, buffer, b[0] + ox, b[1] + oy, b[2], color);
            OriGateRenderer.raw(pose, buffer, a[0] + ox, a[1] + oy, a[2], color);
        }
    }

    // A corner on a segment's face: out along its radius, across it, and in front of or behind the gate
    private static void vertex(final PoseStack.Pose pose, final VertexConsumer buffer, final double angle,
            final double r, final double t, final double z, final int color) {
        OriGateRenderer.raw(pose, buffer, Math.cos(angle) * r - Math.sin(angle) * t,
                Math.sin(angle) * r + Math.cos(angle) * t, z, color);
    }

    // A corner on a face that looks along a segment's radius
    private static void radial(final PoseStack.Pose pose, final VertexConsumer buffer, final double angle,
            final double r, final double t, final double z, final int color) {
        OriGateRenderer.vertex(pose, buffer, angle, r, t, z, color);
    }

    private static void polar(final PoseStack.Pose pose, final VertexConsumer buffer, final double angle,
            final double r, final double z, final int color) {
        OriGateRenderer.raw(pose, buffer, Math.cos(angle) * r, Math.sin(angle) * r, z, color);
    }

    private static void raw(final PoseStack.Pose pose, final VertexConsumer buffer, final double x, final double y,
            final double z, final int color) {
        buffer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(color)
                .setUv(0.5F, 0.5F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, 0, 0, 1);
    }

    // A color scaled by a brightness, as light added to what is behind it
    private static int color(final float[] rgb, final double brightness) {
        final float b = (float) Math.max(0, Math.min(1, brightness));
        return ARGB.colorFromFloat(1.0F, rgb[0] * b, rgb[1] * b, rgb[2] * b);
    }
}
