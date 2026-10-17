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
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The triangles of a Universe gate, built once from {@link UniverseGateModel} at the size of a standard gate: the
 * ring's metals, each chevron on its own so a gate can leave some out, the panels in the symbols' arches with the
 * symbols cut into them, and the paint and light masks laid over a chevron.
 */
public final class UniverseGateMesh {

    /** The ring's parts, each in its own metal. */
    public static final int BODY = 0, RELIEF = 1, DETAIL = 2;
    /** The overlays on a chevron at the top of the gate. */
    public static final int PAINT = 0, LIGHTS = 1;

    private static final double RING_OUTER_RADIUS = 2.5;
    // The metals repeat across the gate every this many of the model's units
    private static final double METAL_REPEAT = 0.6;
    // How many steps round the gate its body is turned in
    private static final int STEPS = 180;
    // How the symbols are laid round the ring: 36 of them, four to each ninth, with a gap under each chevron
    private static final int SYMBOLS = 36;
    private static final double GROUP_ANGLE = 360.0 / 9;
    private static final double CHEVRON_SECTOR_ANGLE = 8;
    private static final double SLOT_ANGLE = (UniverseGateMesh.GROUP_ANGLE - UniverseGateMesh.CHEVRON_SECTOR_ANGLE) / 4;
    // The inside edge of the arches, as measured with the model: (degrees either side of the arch's center, radius)
    // from the bottom up to the point of the arch
    private static final double[][] ARCH_INSIDE = {
            {2.716, 2.669}, {2.685, 2.778}, {2.364, 2.946}, {2.141, 3.022}, {1.624, 3.099},
            {1.272, 3.137}, {0.809, 3.168}, {0.480, 3.183}, {0.143, 3.189}, {0, 3.191}
    };
    // The glyph sheet's cells are the panels measured with the model: GLYPH_HALF degrees either side of the arch's
    // center, from GLYPH_INNER out to GLYPH_OUTER, six cells across and down
    private static final double GLYPH_HALF = 2.0;
    private static final double GLYPH_INNER = 2.7424;
    private static final double GLYPH_OUTER = 3.1404;
    // Where a chevron's light and paint masks are laid: from LIGHTS_HALF degrees either side of its center, and from
    // LIGHTS_OUTER in to LIGHTS_INNER; only on faces turned at least LIGHT_FACING forward
    private static final double LIGHTS_HALF = 17;
    private static final double LIGHTS_INNER = 2.60;
    private static final double LIGHTS_OUTER = 3.40;
    private static final double LIGHT_FACING = 0.5;
    // How far overlays stand off the surface beneath them, in the model's units, so they don't fight it
    private static final double OVERLAY_LIFT = 0.002;

    private static UniverseGateMesh instance;
    private static boolean failed;

    private final double scale;
    private final float[][] ring = new float[3][];
    private final float[][] chevrons = new float[UniverseGateModel.CHEVRONS][];
    private final float[][] overlays = new float[2][];
    private final float[] panels;
    private final float[] symbols;
    private final float[][] symbol = new float[UniverseGateMesh.SYMBOLS][];

    private UniverseGateMesh(final UniverseGateModel model) {
        this.scale = UniverseGateMesh.RING_OUTER_RADIUS / UniverseGateModel.OUTER_RADIUS;
        final List<List<double[]>> parts = new ArrayList<>();
        for (int part = 0; part < UniverseGateModel.PARTS + UniverseGateModel.CHEVRONS; part++) {
            parts.add(new ArrayList<>());
        }
        model.build((part, a, b, c, n) -> {
            final double[] t = {a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2], n[0], n[1], n[2]};
            if (part != UniverseGateModel.CHEVRON) {
                parts.get(part).add(t);
                return;
            }
            // Which of the model's chevrons, clockwise from the top, the triangle is nearest
            final double angle = Math.toDegrees(Math.atan2(a[1] + b[1] + c[1], a[0] + b[0] + c[0]));
            int m = (int) Math.round((90 - angle) / UniverseGateModel.SECTION_ANGLE);
            m = (m % UniverseGateModel.CHEVRONS + UniverseGateModel.CHEVRONS) % UniverseGateModel.CHEVRONS;
            parts.get(UniverseGateModel.PARTS + m).add(t);
        }, UniverseGateMesh.STEPS);
        this.ring[UniverseGateMesh.BODY] = this.metal(parts.get(UniverseGateModel.BODY));
        this.ring[UniverseGateMesh.RELIEF] = this.metal(parts.get(UniverseGateModel.RELIEF));
        this.ring[UniverseGateMesh.DETAIL] = this.metal(parts.get(UniverseGateModel.DETAIL));
        for (int m = 0; m < UniverseGateModel.CHEVRONS; m++) {
            this.chevrons[m] = this.metal(parts.get(UniverseGateModel.PARTS + m));
        }
        this.panels = this.buildPanels();
        final GateGeometry.Builder all = new GateGeometry.Builder();
        for (int s = 0; s < UniverseGateMesh.SYMBOLS; s++) {
            this.symbol[s] = this.buildSymbol(s);
            this.addSymbol(all, s);
        }
        this.symbols = all.build();
        this.overlays[UniverseGateMesh.PAINT] = this.chevronSurface(model);
        this.overlays[UniverseGateMesh.LIGHTS] = this.chevronSurface(model);
    }

    /** @return the mesh, or null if the model couldn't be loaded */
    public static synchronized UniverseGateMesh get() {
        if (UniverseGateMesh.instance == null && !UniverseGateMesh.failed) {
            try (InputStream in = UniverseGateMesh.class.getResourceAsStream(
                    "/assets/sgcraft/models/gate/universe_gate.txt")) {
                if (in == null) {
                    throw new IllegalStateException("universe_gate.txt is missing");
                }
                UniverseGateMesh.instance = new UniverseGateMesh(UniverseGateModel.load(in));
            } catch (final Exception e) {
                UniverseGateMesh.failed = true;
                SGCraft.LOG.error("Couldn't load the Universe gate model", e);
            }
        }
        return UniverseGateMesh.instance;
    }

    public float[] ring(final int part) {
        return this.ring[part];
    }

    /** @return the model's chevron the given number of places clockwise from the top */
    public float[] chevron(final int slot) {
        return this.chevrons[slot];
    }

    public float[] panels() {
        return this.panels;
    }

    public float[] symbols() {
        return this.symbols;
    }

    /** @return one symbol's cell of the glyph sheet, or null if the gate carries no such symbol */
    public float @Nullable [] symbol(final int symbol) {
        return symbol >= 0 && symbol < UniverseGateMesh.SYMBOLS ? this.symbol[symbol] : null;
    }

    /** @return one of the overlays on a chevron at the top of the gate, on both its faces */
    public float[] overlay(final int overlay) {
        return this.overlays[overlay];
    }

    /** @return the center of a symbol, in degrees anticlockwise from the right, with the top chevron at 90 */
    public static double symbolAngle(final int symbol) {
        return 90 - (symbol / 4 * UniverseGateMesh.GROUP_ANGLE + UniverseGateMesh.CHEVRON_SECTOR_ANGLE / 2
                + (symbol % 4 + 0.5) * UniverseGateMesh.SLOT_ANGLE);
    }

    // Triangles in the metal laid flat on whichever of the gate's planes each faces most
    private float[] metal(final List<double[]> tris) {
        final GateGeometry.Builder out = new GateGeometry.Builder();
        final double k = 1 / UniverseGateMesh.METAL_REPEAT;
        for (final double[] t : tris) {
            final double ax = Math.abs(t[9]);
            final double ay = Math.abs(t[10]);
            final double az = Math.abs(t[11]);
            for (int v = 0; v < 3; v++) {
                final double x = t[3 * v];
                final double y = t[3 * v + 1];
                final double z = t[3 * v + 2];
                final double u = az >= ax && az >= ay ? x * k : ax >= ay ? y * k : x * k;
                final double w = az >= ax && az >= ay ? y * k : z * k;
                out.corner(x * this.scale, y * this.scale, z * this.scale, u, w, t[9], t[10], t[11]);
            }
        }
        return out.build();
    }

    // The panel filling each symbol's arch, just in front of the ring's face, in its own metal
    private float[] buildPanels() {
        final GateGeometry.Builder out = new GateGeometry.Builder();
        final double k = 1 / UniverseGateMesh.METAL_REPEAT;
        final double[][] p = UniverseGateMesh.ARCH_INSIDE;
        for (int symbol = 0; symbol < UniverseGateMesh.SYMBOLS; symbol++) {
            final double center = UniverseGateMesh.symbolAngle(symbol);
            for (int i = 0; i + 1 < p.length; i++) {
                // Anticlockwise seen from in front: bottom left, bottom right, top right, top left
                final double[][] q = {{center + p[i][0], p[i][1]}, {center - p[i][0], p[i][1]},
                        {center - p[i + 1][0], p[i + 1][1]}, {center + p[i + 1][0], p[i + 1][1]}};
                final double[][] corners = new double[4][];
                for (int c = 0; c < 4; c++) {
                    final double a = Math.toRadians(q[c][0]);
                    final double x = q[c][1] * Math.cos(a);
                    final double y = q[c][1] * Math.sin(a);
                    corners[c] = new double[] {x, y, UniverseGateModel.faceZ(q[c][1]) + 0.0003, x * k, y * k};
                }
                this.quad(out, corners);
            }
        }
        return out.build();
    }

    private float[] buildSymbol(final int symbol) {
        final GateGeometry.Builder out = new GateGeometry.Builder();
        this.addSymbol(out, symbol);
        return out.build();
    }

    // A symbol's cell of the glyph sheet, a little in front of its panel, on the front only
    private void addSymbol(final GateGeometry.Builder out, final int symbol) {
        final double cell = 1.0 / 6;
        final double r0 = UniverseGateMesh.GLYPH_INNER;
        final double r1 = UniverseGateMesh.GLYPH_OUTER;
        final double lift = 0.0006;
        final double z0 = UniverseGateModel.faceZ(r0) + lift;
        final double z1 = UniverseGateModel.faceZ(r1) + lift;
        final int pieces = 6;
        final double center = UniverseGateMesh.symbolAngle(symbol);
        final double u0 = symbol % 6 * cell;
        final double v0 = symbol / 6 * cell;
        for (int i = 0; i < pieces; i++) {
            // Left to right across the symbol: anticlockwise to clockwise
            final double fa = (double) i / pieces;
            final double fb = (double) (i + 1) / pieces;
            final double aa = Math.toRadians(center + UniverseGateMesh.GLYPH_HALF * (1 - 2 * fa));
            final double ab = Math.toRadians(center + UniverseGateMesh.GLYPH_HALF * (1 - 2 * fb));
            final double ua = u0 + cell * fa;
            final double ub = u0 + cell * fb;
            this.quad(out, new double[][] {
                    {r0 * Math.cos(aa), r0 * Math.sin(aa), z0, ua, v0 + cell},
                    {r0 * Math.cos(ab), r0 * Math.sin(ab), z0, ub, v0 + cell},
                    {r1 * Math.cos(ab), r1 * Math.sin(ab), z1, ub, v0},
                    {r1 * Math.cos(aa), r1 * Math.sin(aa), z1, ua, v0}});
        }
    }

    // A quad facing forwards, corners {x, y, z, u, v} anticlockwise seen from in front
    private void quad(final GateGeometry.Builder out, final double[][] c) {
        for (final int k : new int[] {0, 1, 2, 0, 2, 3}) {
            out.corner(c[k][0] * this.scale, c[k][1] * this.scale, c[k][2] * this.scale, c[k][3], c[k][4], 0, 0, 1);
        }
    }

    // The forward faces of a chevron, on both faces of the ring, with a mask laid over them, turned to the top of the
    // gate and lifted off the surface
    private float[] chevronSurface(final UniverseGateModel model) {
        final GateGeometry.Builder out = new GateGeometry.Builder();
        final double half = UniverseGateMesh.LIGHTS_HALF;
        final double r0 = UniverseGateMesh.LIGHTS_INNER;
        final double r1 = UniverseGateMesh.LIGHTS_OUTER;
        for (int face = -1; face <= 1; face += 2) {
            for (final double[] t : model.chevronSurface()) {
                final double ux = t[3] - t[0];
                final double uy = t[4] - t[1];
                final double uz = t[5] - t[2];
                final double vx = t[6] - t[0];
                final double vy = t[7] - t[1];
                final double vz = t[8] - t[2];
                final double nx = uy * vz - uz * vy;
                final double ny = uz * vx - ux * vz;
                final double nz = ux * vy - uy * vx;
                if (nz < UniverseGateMesh.LIGHT_FACING * Math.sqrt(nx * nx + ny * ny + nz * nz)) {
                    continue;
                }
                for (int n = 0; n < 3; n++) {
                    // Anticlockwise seen from in front of the face
                    final int k = face > 0 ? n : 2 - n;
                    final double x = t[3 * k];
                    final double y = t[3 * k + 1];
                    final double z = (t[3 * k + 2] + UniverseGateMesh.OVERLAY_LIFT) * face;
                    final double d = -Math.toDegrees(Math.atan2(y, x));
                    final double r = Math.hypot(x, y);
                    // Turned a quarter anticlockwise, from along x to the top
                    out.corner(-y * this.scale, x * this.scale, z * this.scale, (d + half) / (2 * half),
                            (r1 - r) / (r1 - r0), 0, 0, face);
                }
            }
        }
        return out.build();
    }
}
