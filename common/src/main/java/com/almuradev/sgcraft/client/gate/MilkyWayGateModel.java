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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The shape of a Milky Way (SG-1) gate, as on the show, as triangles, read from {@code milkyway_gate.txt}. It is in
 * metres: the ring's outer edge is {@link #OUTER_RADIUS} from its centre, its faces either side of z = 0, the front
 * towards +z. Its chevrons are at 90 degrees and every 40 degrees round from there.
 *
 * <p>It is three things, each drawn on its own: the fixed ring, the symbol ring turning in a channel round the front
 * of it, and a chevron. The fixed ring is a body turned from one profile, with everything standing out of it or cut
 * into it given as polygons for a ninth of it, round the top chevron; the rest is that, turned. Where something is cut
 * into the body, cells of it are left out and the polygons there are the body's own faces. The symbol ring is the
 * front of a cone, with its glyphs and the dividers between them standing out of it, all round. The chevron is
 * polygons, each corner moving one of a few ways as the top chevron locks a symbol.</p>
 */
public final class MilkyWayGateModel {

    /** The parts of the gate, which are drawn in their own metals. */
    public static final int BODY = 0, DETAIL = 1, SYMBOL_RING = 2, GLYPHS = 3, CHEVRON = 4, LIGHT = 5, PARTS = 6;

    /** The pieces of a chevron: its metal, its lights, and the lower part of its light's triangle. */
    public static final int METAL = 0, LIGHTS = 1, LOWER_LIGHT = 2, PIECES = 3;

    public static final double OUTER_RADIUS = 6.0960;
    public static final int CHEVRONS = 9;
    public static final double SECTION_ANGLE = 360.0 / MilkyWayGateModel.CHEVRONS;

    // The ninth of the fixed ring its detail is given for, and the columns it is cut into: the model's own faces are
    // flat, one every COLUMN degrees from there
    static final double SECTION_FROM = 70;
    static final double COLUMN = 4;
    static final int COLUMNS = 10;

    private double[][] staticProfile;
    private double[][] ringProfile;
    private final List<double[][]> staticFaces = new ArrayList<>();
    private final List<double[][]> ringFaces = new ArrayList<>();
    private final List<double[][]> chevronFaces = new ArrayList<>();
    private final List<double[][]> lightFaces = new ArrayList<>();
    private final List<double[][]> lowerLightFaces = new ArrayList<>();
    // Cells of the fixed body left out: column * 1000 + profile segment
    private final Set<Integer> cut = new HashSet<>();
    // How far each way of moving takes a corner of the top chevron when it is locked, indexed by the way (0 is not
    // moving)
    private double[][] moves = new double[1][3];

    private MilkyWayGateModel() {
    }

    public static MilkyWayGateModel load(final InputStream in) throws IOException {
        final MilkyWayGateModel model = new MilkyWayGateModel();
        final List<double[]> moveList = new ArrayList<>();
        moveList.add(new double[3]);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                final String[] f = line.split("\\s+");
                switch (f[0]) {
                    case "p" -> {
                        final double[][] p = new double[(f.length - 2) / 2][];
                        for (int i = 0; i < p.length; i++) {
                            p[i] = new double[] {Double.parseDouble(f[2 + 2 * i]), Double.parseDouble(f[3 + 2 * i])};
                        }
                        if (f[1].equals("static")) {
                            model.staticProfile = p;
                        } else {
                            model.ringProfile = p;
                        }
                    }
                    case "c" -> model.cut.add(Integer.parseInt(f[1]) * 1000 + Integer.parseInt(f[2]));
                    case "m" -> {
                        final int n = Integer.parseInt(f[1]);
                        while (moveList.size() <= n) {
                            moveList.add(new double[3]);
                        }
                        moveList.set(n, new double[] {Double.parseDouble(f[2]), Double.parseDouble(f[3]),
                                Double.parseDouble(f[4])});
                    }
                    case "s" -> model.staticFaces.add(MilkyWayGateModel.corners(f, 3));
                    case "r" -> model.ringFaces.add(MilkyWayGateModel.corners(f, 3));
                    case "v" -> model.chevronFaces.add(MilkyWayGateModel.corners(f, 4));
                    case "l" -> model.lightFaces.add(MilkyWayGateModel.corners(f, 4));
                    case "t" -> model.lowerLightFaces.add(MilkyWayGateModel.corners(f, 4));
                    default -> {
                    }
                }
            }
        }
        model.moves = moveList.toArray(new double[0][]);
        return model;
    }

    /** Receives the gate's triangles: corners anticlockwise seen from the side the normal points to. */
    @FunctionalInterface
    public interface Sink {

        void triangle(final int part, final double[] a, final double[] b, final double[] c, final double[] normal);
    }

    /** The fixed ring: its body turned in the given number of steps to each column, and its detail. */
    public void buildFixed(final Sink sink, final int stepsPerColumn) {
        final double[][] p = this.staticProfile;
        for (int g = 0; g < MilkyWayGateModel.CHEVRONS; g++) {
            for (int j = 0; j < MilkyWayGateModel.COLUMNS; j++) {
                for (int i = 0; i + 1 < p.length; i++) {
                    if (this.cut.contains(j * 1000 + i)) {
                        continue;
                    }
                    final double a0 = MilkyWayGateModel.SECTION_FROM + g * MilkyWayGateModel.SECTION_ANGLE
                            + j * MilkyWayGateModel.COLUMN;
                    // Going along the profile, the body is on the left, so outwards is to the right
                    MilkyWayGateModel.revolve(sink, MilkyWayGateModel.BODY, p[i], p[i + 1], a0,
                            a0 + MilkyWayGateModel.COLUMN, stepsPerColumn, 1);
                }
            }
            final double turn = Math.toRadians(g * MilkyWayGateModel.SECTION_ANGLE);
            for (final double[][] face : this.staticFaces) {
                MilkyWayGateModel.polygon(sink, MilkyWayGateModel.DETAIL, MilkyWayGateModel.turned(face, turn));
            }
        }
    }

    /**
     * The symbol ring, at rest: the point of origin at the top and the rest of its glyphs clockwise from there, one
     * every 360 / 39 degrees. Its cone turned in the given number of steps all round, and its glyphs and dividers.
     */
    public void buildRing(final Sink sink, final int steps) {
        final double[][] p = this.ringProfile;
        for (int i = 0; i + 1 < p.length; i++) {
            // The front of a cone, facing forwards: outwards is to the left going outwards
            MilkyWayGateModel.revolve(sink, MilkyWayGateModel.SYMBOL_RING, p[i], p[i + 1], 0, 360, steps, -1);
        }
        for (final double[][] face : this.ringFaces) {
            MilkyWayGateModel.polygon(sink, MilkyWayGateModel.GLYPHS, face);
        }
    }

    /** The top chevron, one piece of it, only the corners that move the given way. */
    public void buildChevron(final Sink sink, final int piece, final int move) {
        final List<double[][]> faces = piece == MilkyWayGateModel.METAL ? this.chevronFaces
                : piece == MilkyWayGateModel.LIGHTS ? this.lightFaces : this.lowerLightFaces;
        for (final double[][] face : faces) {
            if ((int) face[0][3] == move) {
                MilkyWayGateModel.polygon(sink,
                        piece == MilkyWayGateModel.METAL ? MilkyWayGateModel.CHEVRON : MilkyWayGateModel.LIGHT, face);
            }
        }
    }

    public int moveCount() {
        return this.moves.length;
    }

    /** @return how far the corners moving the given way go when the top chevron is locked */
    public double[] move(final int way) {
        return this.moves[way];
    }

    private static double[][] corners(final String[] f, final int each) {
        final double[][] p = new double[(f.length - 1) / each][];
        for (int i = 0; i < p.length; i++) {
            p[i] = new double[each];
            for (int k = 0; k < each; k++) {
                p[i][k] = Double.parseDouble(f[1 + each * i + k]);
            }
        }
        return p;
    }

    private static double[][] turned(final double[][] face, final double turn) {
        final double c = Math.cos(turn);
        final double s = Math.sin(turn);
        final double[][] q = new double[face.length][];
        for (int i = 0; i < face.length; i++) {
            final double[] f = face[i];
            q[i] = new double[] {f[0] * c - f[1] * s, f[0] * s + f[1] * c, f[2]};
        }
        return q;
    }

    // The surface swept by the profile segment from p to q, turned from degrees a0 to a1, facing to the right of the
    // way the segment goes in (r, z) if side is 1, else to the left
    private static void revolve(final Sink sink, final int part, final double[] p, final double[] q, final double a0,
            final double a1, final int steps, final int side) {
        final double dr = q[0] - p[0];
        final double dz = q[1] - p[1];
        final double len = Math.hypot(dr, dz);
        if (len < 1e-9) {
            return;
        }
        final double nr = dz / len * side;
        final double nz = -dr / len * side;
        for (int k = 0; k < steps; k++) {
            final double b0 = Math.toRadians(a0 + (a1 - a0) * k / steps);
            final double b1 = Math.toRadians(a0 + (a1 - a0) * (k + 1) / steps);
            final double c0 = Math.cos(b0);
            final double s0 = Math.sin(b0);
            final double c1 = Math.cos(b1);
            final double s1 = Math.sin(b1);
            final double bm = (b0 + b1) / 2;
            final double[] n = {nr * Math.cos(bm), nr * Math.sin(bm), nz};
            final double[] w = {p[0] * c0, p[0] * s0, p[1]};
            final double[] x = {q[0] * c0, q[0] * s0, q[1]};
            final double[] y = {q[0] * c1, q[0] * s1, q[1]};
            final double[] z = {p[0] * c1, p[0] * s1, p[1]};
            MilkyWayGateModel.emitFacing(sink, part, w, x, y, n);
            MilkyWayGateModel.emitFacing(sink, part, w, y, z, n);
        }
    }

    // A polygon's triangles, as its corners go round: it faces the way they go anticlockwise
    private static void polygon(final Sink sink, final int part, final double[][] p) {
        final double[] n = MilkyWayGateModel.newell(p);
        if (n == null) {
            return;
        }
        for (final int[] t : MilkyWayGateModel.triangulate(p, n)) {
            sink.triangle(part, p[t[0]], p[t[1]], p[t[2]], n);
        }
    }

    // The unit normal of a polygon, the way its corners go round
    private static double[] newell(final double[][] p) {
        double nx = 0;
        double ny = 0;
        double nz = 0;
        for (int i = 0; i < p.length; i++) {
            final double[] a = p[i];
            final double[] b = p[(i + 1) % p.length];
            nx += (a[1] - b[1]) * (a[2] + b[2]);
            ny += (a[2] - b[2]) * (a[0] + b[0]);
            nz += (a[0] - b[0]) * (a[1] + b[1]);
        }
        final double l = Math.sqrt(nx * nx + ny * ny + nz * nz);
        return l < 1e-14 ? null : new double[] {nx / l, ny / l, nz / l};
    }

    // Splits a simple polygon into triangles of its corners, by cutting off ears, flat in the plane it most faces,
    // with its corners in their own order
    private static List<int[]> triangulate(final double[][] p, final double[] n) {
        final double ax = Math.abs(n[0]);
        final double ay = Math.abs(n[1]);
        final double az = Math.abs(n[2]);
        final boolean alongZ = az >= ax && az >= ay;
        final boolean alongX = !alongZ && ax >= ay;
        final int u = alongZ ? 0 : alongX ? 1 : 0;
        final int w = alongZ ? 1 : 2;
        final boolean flip = (alongZ ? n[2] : alongX ? n[0] : -n[1]) < 0;
        final double[][] q = new double[p.length][];
        for (int i = 0; i < p.length; i++) {
            q[i] = new double[] {p[i][u], flip ? -p[i][w] : p[i][w]};
        }
        final List<int[]> tris = new ArrayList<>();
        final List<Integer> v = new ArrayList<>();
        for (int i = 0; i < q.length; i++) {
            v.add(i);
        }
        int guard = 0;
        while (v.size() > 3 && guard++ < 10 * q.length * q.length) {
            boolean cutEar = false;
            for (int i = 0; i < v.size(); i++) {
                final int i0 = v.get((i + v.size() - 1) % v.size());
                final int i1 = v.get(i);
                final int i2 = v.get((i + 1) % v.size());
                final double cross = (q[i1][0] - q[i0][0]) * (q[i2][1] - q[i0][1])
                        - (q[i1][1] - q[i0][1]) * (q[i2][0] - q[i0][0]);
                if (cross <= 1e-14) {
                    continue;
                }
                boolean empty = true;
                for (final int j : v) {
                    if (j != i0 && j != i1 && j != i2 && MilkyWayGateModel.inTriangle(q[j], q[i0], q[i1], q[i2])) {
                        empty = false;
                        break;
                    }
                }
                if (empty) {
                    tris.add(new int[] {i0, i1, i2});
                    v.remove(i);
                    cutEar = true;
                    break;
                }
            }
            if (!cutEar) {
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
        final double d1 = MilkyWayGateModel.side(q, a, b);
        final double d2 = MilkyWayGateModel.side(q, b, c);
        final double d3 = MilkyWayGateModel.side(q, c, a);
        final boolean neg = d1 < 0 || d2 < 0 || d3 < 0;
        final boolean pos = d1 > 0 || d2 > 0 || d3 > 0;
        return !(neg && pos);
    }

    private static double side(final double[] q, final double[] a, final double[] b) {
        return (q[0] - b[0]) * (a[1] - b[1]) - (a[0] - b[0]) * (q[1] - b[1]);
    }

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
