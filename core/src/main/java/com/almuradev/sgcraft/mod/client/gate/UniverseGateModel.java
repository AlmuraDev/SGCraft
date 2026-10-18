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

import com.almuradev.sgcraft.mod.gate.DialStyle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The shape of a Universe (Destiny) gate, as on the show, as triangles, read from {@code universe_gate.txt}. It is in
 * its own units: the ring's outer edge is {@link #OUTER_RADIUS} from its center and its faces are either side of
 * z = 0. Its chevrons are at 90 degrees and every 40 degrees round from there, clockwise.
 *
 * <p>The ring itself is a body turned from one profile ({@link #PROFILE}). Everything standing out of it (the frames
 * round the symbols' arches, the posts between them, the lattice along the rim, the pipes round the inside and the
 * chevrons) is a set of faces, each an outline with a height at each corner, with sides dropping from its edges to the
 * body. Those are given for a ninth of a turn on the front, a whole chevron with half the way to the next one either
 * side of it, and the rest of the gate is that, turned, front and back.</p>
 *
 * <p>The model has four arches to each ninth. Where the gate has five, as {@link DialStyle} lays its symbols out, the
 * four arches' frames and posts are drawn narrower, toward the chevron before them, and the first again after them.</p>
 */
public final class UniverseGateModel {

    /** The parts of the gate, which are drawn in their own metals. */
    public static final int BODY = 0, CHEVRON = 1, RELIEF = 2, DETAIL = 3, PARTS = 4;

    public static final double OUTER_RADIUS = 3.4111;
    /** The angle from a chevron to the next, which the faces are given for. */
    public static final double SECTION_ANGLE = 40;
    public static final int CHEVRONS = 9;
    // Where a ninth's arches start, past the gap under the chevron before them, and how wide the model's are, in
    // degrees. A post between arches is a thin face on the line between them: a face whose middle is within ARCH_EDGE
    // of the line is the post, and within SLACK of the ninth's ends is still in the ninth
    private static final double ARCHES_FROM = 4;
    private static final double MODEL_ARCH = 8;
    private static final int MODEL_ARCHES = 4;
    private static final double ARCH_EDGE = 0.1;
    private static final double SLACK = 0.5;

    // The body's profile on the front, (radius, z), from the middle of the slot round the inside of the ring where the
    // event horizon sits, up its inner edge, out across its face to the outer edge and down that to the middle of the
    // groove round it. The back is its mirror.
    static final double[][] PROFILE = {
            {2.5810, 0.0}, {2.5810, 0.0114}, {2.5188, 0.0149}, {2.5188, 0.0771}, {2.5209, 0.0828},
            {2.5261, 0.0859}, {2.6479, 0.1080}, {2.6699, 0.1403}, {2.7414, 0.1482}, {3.1400, 0.1864},
            {3.2618, 0.1969}, {3.2618, 0.2139}, {3.2671, 0.2201}, {3.3290, 0.2314}, {3.3436, 0.2300},
            {3.3542, 0.2211}, {3.3581, 0.2070}, {3.3581, 0.1534}, {3.3870, 0.1176}, {3.3870, 0.0566},
            {3.4111, 0.0570}, {3.3854, 0.0035}, {3.3735, 0.0035}, {3.3735, 0.0}
    };
    // The part of the profile that faces forwards, from the inner edge to the outer
    static final int FACE_FROM = 3;
    static final int FACE_TO = 20;

    // A ninth of the gate's raised faces and their sides, in a frame where the chevron's center is along x:
    // {part, ax, ay, az, bx, ..., nx, ny, nz}
    private final List<double[]> section = new ArrayList<>();
    private List<double[]> chevronSurface;

    private UniverseGateModel() {
    }

    /**
     * The face of the ring between the symbols' arches, which the arches' insides are flush with and the symbols are
     * on: a cone, z rising with radius.
     */
    public static double faceZ(final double r) {
        return UniverseGateModel.interpolate(UniverseGateModel.PROFILE, 7, 10, r);
    }

    /** Receives the gate's triangles: corners anticlockwise seen from the side the normal points to. */
    @FunctionalInterface
    public interface Sink {

        void triangle(final int part, final double[] a, final double[] b, final double[] c, final double[] normal);
    }

    public static UniverseGateModel load(final InputStream in) throws IOException {
        final List<Face> faces = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                final String[] f = line.split("\\s+");
                final Face face = new Face();
                face.part = f[0].equals("a") ? UniverseGateModel.CHEVRON
                        : f[0].equals("b") ? UniverseGateModel.RELIEF : UniverseGateModel.DETAIL;
                final int n = (f.length - 1) / 3;
                face.points = new double[n][];
                for (int i = 0; i < n; i++) {
                    // The faces were measured on the side of the model without the symbols, which is the gate's back;
                    // the front is them seen from the other side, mirrored across the chevron's center
                    final double d = -Double.parseDouble(f[1 + 3 * i]);
                    final double r = Double.parseDouble(f[2 + 3 * i]);
                    final double a = Math.toRadians(-d);
                    face.points[i] = new double[] {r * Math.cos(a), r * Math.sin(a), Double.parseDouble(f[3 + 3 * i]),
                            d, r};
                }
                faces.add(face);
            }
        }
        final UniverseGateModel model = new UniverseGateModel();
        model.buildSection(faces);
        return model;
    }

    /** Gives the whole gate to the sink: the body turned in the given number of steps round, and the raised faces. */
    public void build(final Sink sink, final int bodySteps) {
        this.buildBody(sink, bodySteps);
        for (int g = 0; g < UniverseGateModel.CHEVRONS; g++) {
            // The ninth clockwise from this chevron, and the one anticlockwise, and how many arches each has
            final int after = DialStyle.universeGroupSize(g);
            final int before = DialStyle.universeGroupSize((g + UniverseGateModel.CHEVRONS - 1)
                    % UniverseGateModel.CHEVRONS);
            for (final double[] t : this.section) {
                final double middle = UniverseGateModel.clockwise(t);
                double[] placed = t;
                if ((int) t[0] == UniverseGateModel.RELIEF) {
                    final double half = UniverseGateModel.SECTION_ANGLE / 2;
                    final double start = UniverseGateModel.ARCHES_FROM;
                    if (after > UniverseGateModel.MODEL_ARCHES && middle >= start - UniverseGateModel.SLACK
                            && middle <= half + UniverseGateModel.SLACK) {
                        placed = UniverseGateModel.narrowed(t, start, start, after);
                    } else if (before > UniverseGateModel.MODEL_ARCHES
                            && middle >= -half - UniverseGateModel.SLACK
                            && middle < -start - UniverseGateModel.ARCH_EDGE) {
                        // In the ninth's own terms, it starts a whole ninth back
                        final double from = start - UniverseGateModel.SECTION_ANGLE;
                        placed = UniverseGateModel.narrowed(t, from, from, before);
                    }
                    // The first arch again, after the narrowed four, as many times as the ninth has more arches
                    if (after > UniverseGateModel.MODEL_ARCHES && middle >= start - UniverseGateModel.SLACK
                            && middle < start + UniverseGateModel.MODEL_ARCH - UniverseGateModel.ARCH_EDGE) {
                        final double width = UniverseGateModel.MODEL_ARCH * UniverseGateModel.MODEL_ARCHES / after;
                        for (int extra = UniverseGateModel.MODEL_ARCHES; extra < after; extra++) {
                            this.emit(sink, UniverseGateModel.narrowed(t, start, start + extra * width, after), g);
                        }
                    }
                }
                this.emit(sink, placed, g);
            }
        }
    }

    // Gives a triangle of the section to the sink at a chevron, on the front and, mirrored, the back
    private void emit(final Sink sink, final double[] t, final int chevron) {
        final double turn = Math.toRadians(90 - chevron * UniverseGateModel.SECTION_ANGLE);
        final double cos = Math.cos(turn);
        final double sin = Math.sin(turn);
        for (int face = -1; face <= 1; face += 2) {
            final double[][] q = new double[3][];
            for (int k = 0; k < 3; k++) {
                // Mirrored through the ring onto the back as needed, then turned to the chevron
                final double x = t[1 + 3 * k];
                final double y = t[2 + 3 * k];
                final double z = t[3 + 3 * k] * face;
                q[k] = new double[] {x * cos - y * sin, x * sin + y * cos, z};
            }
            final double[] n = {t[10] * cos - t[11] * sin, t[10] * sin + t[11] * cos, t[12] * face};
            // Mirroring turns the corners the other way round
            if (face > 0) {
                sink.triangle((int) t[0], q[0], q[1], q[2], n);
            } else {
                sink.triangle((int) t[0], q[0], q[2], q[1], n);
            }
        }
    }

    // How far clockwise from the chevron a triangle of the section's middle is, in degrees
    private static double clockwise(final double[] t) {
        final double x = t[1] + t[4] + t[7];
        final double y = t[2] + t[5] + t[8];
        return -Math.toDegrees(Math.atan2(y, x));
    }

    // A triangle of a ninth's arches, narrowed for a ninth of the given number of arches: each corner as far past
    // {@code to} as it was past {@code from}, made that much narrower, turned round the ring's center
    private static double[] narrowed(final double[] t, final double from, final double to, final int arches) {
        final double narrowing = (double) UniverseGateModel.MODEL_ARCHES / arches;
        final double[] out = t.clone();
        for (int k = 0; k < 3; k++) {
            final double x = t[1 + 3 * k];
            final double y = t[2 + 3 * k];
            final double r = Math.hypot(x, y);
            final double d = -Math.toDegrees(Math.atan2(y, x));
            final double a = Math.toRadians(-(to + (d - from) * narrowing));
            out[1 + 3 * k] = r * Math.cos(a);
            out[2 + 3 * k] = r * Math.sin(a);
        }
        // The normal turns as the middle does
        final double before = Math.toRadians(-UniverseGateModel.clockwise(t));
        final double after = Math.toRadians(-UniverseGateModel.clockwise(out));
        final double cos = Math.cos(after - before);
        final double sin = Math.sin(after - before);
        out[10] = t[10] * cos - t[11] * sin;
        out[11] = t[10] * sin + t[11] * cos;
        return out;
    }

    /** @return the forward faces of one chevron, as triangles {ax, ay, az, bx, ..., cz} with its center along x */
    public List<double[]> chevronSurface() {
        if (this.chevronSurface == null) {
            this.chevronSurface = new ArrayList<>();
            for (final double[] t : this.section) {
                if ((int) t[0] == UniverseGateModel.CHEVRON && t[12] >= 0.05) {
                    this.chevronSurface.add(Arrays.copyOfRange(t, 1, 10));
                }
            }
        }
        return this.chevronSurface;
    }

    // How far forward the body's front is at the given radius; where it steps, the lower side
    private static double bodyZ(final double r) {
        return UniverseGateModel.interpolate(UniverseGateModel.PROFILE, UniverseGateModel.FACE_FROM,
                UniverseGateModel.FACE_TO, r);
    }

    private static double interpolate(final double[][] p, final int from, final int to, final double r) {
        if (r <= p[from][0]) {
            return p[from][1];
        }
        double best = Double.NaN;
        for (int i = from; i < to; i++) {
            final double r0 = p[i][0];
            final double r1 = p[i + 1][0];
            final double lo = Math.min(r0, r1);
            final double hi = Math.max(r0, r1);
            if (r < lo || r > hi) {
                continue;
            }
            final double z = hi - lo < 1e-9 ? Math.min(p[i][1], p[i + 1][1])
                    : p[i][1] + (p[i + 1][1] - p[i][1]) * (r - r0) / (r1 - r0);
            best = Double.isNaN(best) ? z : Math.min(best, z);
        }
        return Double.isNaN(best) ? p[to][1] : best;
    }

    private static final class Face {
        private int part;
        // Each corner: x, y, z, then degrees from the chevron's center and radius
        private double[][] points;
    }

    private void buildSection(final List<Face> faces) {
        // Edges two faces share have no side
        final Map<String, Integer> edgeUse = new HashMap<>();
        for (final Face f : faces) {
            for (int i = 0; i < f.points.length; i++) {
                edgeUse.merge(UniverseGateModel.edgeKey(f.points[i], f.points[(i + 1) % f.points.length]), 1,
                        Integer::sum);
            }
        }
        for (final Face f : faces) {
            final double[][] p = f.points;
            final boolean anticlockwise = UniverseGateModel.signedArea(p) > 0;
            for (final int[] t : UniverseGateModel.triangulate(p)) {
                final double[] a = p[t[0]];
                final double[] b = p[t[1]];
                final double[] c = p[t[2]];
                // Each face is on the front, so faces forwards whichever way round its outline goes
                this.add(f.part, a, b, c, UniverseGateModel.normal(a, b, c));
            }
            for (int i = 0; i < p.length; i++) {
                final double[] a = p[i];
                final double[] b = p[(i + 1) % p.length];
                if (edgeUse.get(UniverseGateModel.edgeKey(a, b)) > 1 || UniverseGateModel.onSectionEdge(a, b)) {
                    continue;
                }
                final double baseA = UniverseGateModel.bodyZ(a[4]);
                final double baseB = UniverseGateModel.bodyZ(b[4]);
                if (a[2] <= baseA + 1e-5 && b[2] <= baseB + 1e-5) {
                    continue;
                }
                // Outwards from the face: to the right of its edges going anticlockwise
                final double ex = b[0] - a[0];
                final double ey = b[1] - a[1];
                final double len = Math.hypot(ex, ey);
                if (len < 1e-9) {
                    continue;
                }
                final double s = anticlockwise ? 1 : -1;
                final double[] n = {s * ey / len, -s * ex / len, 0};
                final double[] a0 = {a[0], a[1], Math.min(baseA, a[2])};
                final double[] b0 = {b[0], b[1], Math.min(baseB, b[2])};
                this.add(f.part, a0, b0, b, n);
                this.add(f.part, a0, b, a, n);
            }
        }
    }

    private static boolean onSectionEdge(final double[] a, final double[] b) {
        final double h = UniverseGateModel.SECTION_ANGLE / 2;
        return Math.abs(a[3] + h) < 1e-3 && Math.abs(b[3] + h) < 1e-3
                || Math.abs(a[3] - h) < 1e-3 && Math.abs(b[3] - h) < 1e-3;
    }

    private static String edgeKey(final double[] a, final double[] b) {
        final String ka = UniverseGateModel.key(a);
        final String kb = UniverseGateModel.key(b);
        return ka.compareTo(kb) < 0 ? ka + "|" + kb : kb + "|" + ka;
    }

    private static String key(final double[] p) {
        return Math.round(p[0] * 2e4) + "," + Math.round(p[1] * 2e4) + "," + Math.round(p[2] * 2e4);
    }

    private static double signedArea(final double[][] p) {
        double s = 0;
        for (int i = 0; i < p.length; i++) {
            final double[] a = p[i];
            final double[] b = p[(i + 1) % p.length];
            s += a[0] * b[1] - b[0] * a[1];
        }
        return s / 2;
    }

    // The unit normal of a triangle, turned to face forwards (+z)
    private static double[] normal(final double[] a, final double[] b, final double[] c) {
        final double ux = b[0] - a[0];
        final double uy = b[1] - a[1];
        final double uz = b[2] - a[2];
        final double vx = c[0] - a[0];
        final double vy = c[1] - a[1];
        final double vz = c[2] - a[2];
        final double nx = uy * vz - uz * vy;
        final double ny = uz * vx - ux * vz;
        final double nz = ux * vy - uy * vx;
        final double l = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (l < 1e-15) {
            return new double[] {0, 0, 1};
        }
        final double s = nz >= 0 ? 1 / l : -1 / l;
        return new double[] {nx * s, ny * s, nz * s};
    }

    // Keeps a triangle of the section, its corners put anticlockwise seen from where n points
    private void add(final int part, final double[] a, final double[] b, final double[] c, final double[] n) {
        final double ux = b[0] - a[0];
        final double uy = b[1] - a[1];
        final double uz = b[2] - a[2];
        final double vx = c[0] - a[0];
        final double vy = c[1] - a[1];
        final double vz = c[2] - a[2];
        final double cx = uy * vz - uz * vy;
        final double cy = uz * vx - ux * vz;
        final double cz = ux * vy - uy * vx;
        final boolean flip = cx * n[0] + cy * n[1] + cz * n[2] < 0;
        final double[] second = flip ? c : b;
        final double[] third = flip ? b : c;
        this.section.add(new double[] {part, a[0], a[1], a[2], second[0], second[1], second[2], third[0], third[1],
                third[2], n[0], n[1], n[2]});
    }

    // Splits a simple polygon, of corners (x, y, ...) in either winding, into triangles of its corners, by cutting off
    // ears
    private static List<int[]> triangulate(final double[][] p) {
        final List<int[]> tris = new ArrayList<>();
        final int n = p.length;
        final List<Integer> v = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            v.add(i);
        }
        final double s = UniverseGateModel.signedArea(p) > 0 ? 1 : -1;
        int guard = 0;
        while (v.size() > 3 && guard++ < 10 * n * n) {
            boolean cut = false;
            for (int i = 0; i < v.size(); i++) {
                final int i0 = v.get((i + v.size() - 1) % v.size());
                final int i1 = v.get(i);
                final int i2 = v.get((i + 1) % v.size());
                final double cross = (p[i1][0] - p[i0][0]) * (p[i2][1] - p[i0][1])
                        - (p[i1][1] - p[i0][1]) * (p[i2][0] - p[i0][0]);
                if (cross * s <= 1e-14) {
                    continue;
                }
                boolean empty = true;
                for (final int j : v) {
                    if (j != i0 && j != i1 && j != i2 && UniverseGateModel.inTriangle(p[j], p[i0], p[i1], p[i2])) {
                        empty = false;
                        break;
                    }
                }
                if (empty) {
                    tris.add(new int[] {i0, i1, i2});
                    v.remove(i);
                    cut = true;
                    break;
                }
            }
            if (!cut) {
                // Nothing left that is a clean ear: what is left is all but flat
                v.remove(0);
            }
        }
        if (v.size() == 3) {
            tris.add(new int[] {v.get(0), v.get(1), v.get(2)});
        }
        return tris;
    }

    private static boolean inTriangle(final double[] q, final double[] a, final double[] b, final double[] c) {
        final double d1 = UniverseGateModel.side(q, a, b);
        final double d2 = UniverseGateModel.side(q, b, c);
        final double d3 = UniverseGateModel.side(q, c, a);
        final boolean neg = d1 < 0 || d2 < 0 || d3 < 0;
        final boolean pos = d1 > 0 || d2 > 0 || d3 > 0;
        return !(neg && pos);
    }

    private static double side(final double[] q, final double[] a, final double[] b) {
        return (q[0] - b[0]) * (a[1] - b[1]) - (a[0] - b[0]) * (q[1] - b[1]);
    }

    private void buildBody(final Sink sink, final int steps) {
        final double[][] p = UniverseGateModel.PROFILE;
        for (int face = -1; face <= 1; face += 2) {
            for (int i = 0; i + 1 < p.length; i++) {
                final double r0 = p[i][0];
                final double z0 = p[i][1] * face;
                final double r1 = p[i + 1][0];
                final double z1 = p[i + 1][1] * face;
                final double dr = r1 - r0;
                final double dz = z1 - z0;
                final double len = Math.hypot(dr, dz);
                if (len < 1e-9) {
                    continue;
                }
                // Outwards from the body: to the left going along the front's profile, so to the right along the
                // back's, which is mirrored
                final double nr = -dz / len * face;
                final double nz = dr / len * face;
                for (int j = 0; j < steps; j++) {
                    final double a0 = 2 * Math.PI * j / steps;
                    final double a1 = 2 * Math.PI * (j + 1) / steps;
                    final double c0 = Math.cos(a0);
                    final double s0 = Math.sin(a0);
                    final double c1 = Math.cos(a1);
                    final double s1 = Math.sin(a1);
                    final double am = (a0 + a1) / 2;
                    final double[] n = {nr * Math.cos(am), nr * Math.sin(am), nz};
                    final double[] pa = {r0 * c0, r0 * s0, z0};
                    final double[] pb = {r1 * c0, r1 * s0, z1};
                    final double[] pc = {r1 * c1, r1 * s1, z1};
                    final double[] pd = {r0 * c1, r0 * s1, z0};
                    UniverseGateModel.emitFacing(sink, UniverseGateModel.BODY, pa, pb, pc, n);
                    UniverseGateModel.emitFacing(sink, UniverseGateModel.BODY, pa, pc, pd, n);
                }
            }
        }
    }

    // Gives a triangle to the sink with its corners in the order that makes it face along n
    private static void emitFacing(final Sink sink, final int part, final double[] a, final double[] b,
            final double[] c, final double[] n) {
        final double ux = b[0] - a[0];
        final double uy = b[1] - a[1];
        final double uz = b[2] - a[2];
        final double vx = c[0] - a[0];
        final double vy = c[1] - a[1];
        final double vz = c[2] - a[2];
        final double cx = uy * vz - uz * vy;
        final double cy = uz * vx - ux * vz;
        final double cz = ux * vy - uy * vx;
        if (cx * n[0] + cy * n[1] + cz * n[2] >= 0) {
            sink.triangle(part, a, b, c, n);
        } else {
            sink.triangle(part, a, c, b, n);
        }
    }
}
