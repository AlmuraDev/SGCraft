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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The shape of a Pegasus (Atlantis) gate, as on the show, as groups of triangles, read from
 * {@code pegasus_gate.txt} (and its glyphs from {@code pegasus_glyphs.txt}). It is in meters, the front towards +z,
 * its chevrons at 90 degrees and every 40 degrees round from there. The fixed ring's faces and edges are given for
 * the ninth of it round the top chevron, the inner ring's screens and the dividers between them for the one at the
 * top, and the chevron for the top one; the rest is those, turned.
 */
public final class PegasusGateModel {

    public static final int CHEVRONS = 9;
    public static final int SCREENS = 36;
    public static final double SECTION_ANGLE = 360.0 / PegasusGateModel.CHEVRONS;
    public static final double SCREEN_ANGLE = 360.0 / PegasusGateModel.SCREENS;

    // Faces meeting at more than this angle (degrees) are a crease, lit apart; less, and the surface is smooth across
    // them
    private static final double CREASE = 40;

    // A group's triangles: {ax, ay, az, au, av, bx, ..., cv}, corners anticlockwise seen from outside
    private final Map<String, List<double[]>> groups = new HashMap<>();
    private final Map<String, List<double[]>> smoothNormals = new HashMap<>();
    private double outerRadius;
    // Where the screens are: from screenInner out to screenOuter
    private double screenInner;
    private double screenOuter;

    private PegasusGateModel() {
    }

    public static PegasusGateModel load(final InputStream in) throws IOException {
        final PegasusGateModel model = new PegasusGateModel();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            List<double[]> corners = null;
            List<double[]> tris = null;
            while ((line = reader.readLine()) != null) {
                final String[] f = line.trim().split("\\s+");
                switch (f[0]) {
                    case "g" -> {
                        corners = new ArrayList<>();
                        tris = model.groups.computeIfAbsent(f[1], k -> new ArrayList<>());
                    }
                    case "v" -> corners.add(new double[] {Double.parseDouble(f[1]), Double.parseDouble(f[2]),
                            Double.parseDouble(f[3]), Double.parseDouble(f[4]), Double.parseDouble(f[5])});
                    case "t" -> {
                        final double[] t = new double[15];
                        for (int k = 0; k < 3; k++) {
                            System.arraycopy(corners.get(Integer.parseInt(f[1 + k])), 0, t, 5 * k, 5);
                        }
                        tris.add(t);
                    }
                    default -> {
                    }
                }
            }
        }
        model.measure();
        return model;
    }

    /** @return how far the ring's outer edge is from its center, in meters */
    public double outerRadius() {
        return this.outerRadius;
    }

    public double screenInner() {
        return this.screenInner;
    }

    public double screenOuter() {
        return this.screenOuter;
    }

    public List<double[]> group(final String name) {
        final List<double[]> g = this.groups.get(name);
        return g != null ? g : new ArrayList<>();
    }

    /**
     * The normal at each corner of each of a group's triangles, {ax, ay, az, bx, ..., cz}: the faces round that
     * corner averaged, by their area, those across a crease left out.
     */
    public List<double[]> cornerNormals(final String name) {
        final List<double[]> cached = this.smoothNormals.get(name);
        if (cached != null) {
            return cached;
        }
        final List<double[]> tris = this.group(name);
        final Map<String, List<double[]>> round = new HashMap<>();
        final List<double[]> faceNormals = new ArrayList<>();
        for (final double[] t : tris) {
            final double[] n = PegasusGateModel.normal(t);
            final double ux = t[5] - t[0];
            final double uy = t[6] - t[1];
            final double uz = t[7] - t[2];
            final double vx = t[10] - t[0];
            final double vy = t[11] - t[1];
            final double vz = t[12] - t[2];
            final double cx = uy * vz - uz * vy;
            final double cy = uz * vx - ux * vz;
            final double cz = ux * vy - uy * vx;
            final double area = Math.sqrt(cx * cx + cy * cy + cz * cz) / 2;
            final double[] weighted = {n[0], n[1], n[2], area};
            faceNormals.add(weighted);
            for (int k = 0; k < 3; k++) {
                round.computeIfAbsent(PegasusGateModel.cornerKey(t, k), x -> new ArrayList<>()).add(weighted);
            }
        }
        final double creaseCos = Math.cos(Math.toRadians(PegasusGateModel.CREASE));
        final List<double[]> normals = new ArrayList<>();
        for (int i = 0; i < tris.size(); i++) {
            final double[] t = tris.get(i);
            final double[] own = faceNormals.get(i);
            final double[] out = new double[9];
            for (int k = 0; k < 3; k++) {
                double sx = 0;
                double sy = 0;
                double sz = 0;
                for (final double[] n : round.get(PegasusGateModel.cornerKey(t, k))) {
                    if (n[0] * own[0] + n[1] * own[1] + n[2] * own[2] >= creaseCos) {
                        sx += n[0] * n[3];
                        sy += n[1] * n[3];
                        sz += n[2] * n[3];
                    }
                }
                double l = Math.sqrt(sx * sx + sy * sy + sz * sz);
                if (l < 1e-15) {
                    sx = own[0];
                    sy = own[1];
                    sz = own[2];
                    l = 1;
                }
                out[3 * k] = sx / l;
                out[3 * k + 1] = sy / l;
                out[3 * k + 2] = sz / l;
            }
            normals.add(out);
        }
        this.smoothNormals.put(name, normals);
        return normals;
    }

    /** @return the unit normal of a triangle, the way its corners go round */
    public static double[] normal(final double[] t) {
        final double ux = t[5] - t[0];
        final double uy = t[6] - t[1];
        final double uz = t[7] - t[2];
        final double vx = t[10] - t[0];
        final double vy = t[11] - t[1];
        final double vz = t[12] - t[2];
        final double nx = uy * vz - uz * vy;
        final double ny = uz * vx - ux * vz;
        final double nz = ux * vy - uy * vx;
        final double l = Math.sqrt(nx * nx + ny * ny + nz * nz);
        return l < 1e-15 ? new double[] {0, 0, 1} : new double[] {nx / l, ny / l, nz / l};
    }

    private static String cornerKey(final double[] t, final int k) {
        return Math.round(t[5 * k] * 1e4) + "," + Math.round(t[5 * k + 1] * 1e4) + "," + Math.round(t[5 * k + 2] * 1e4);
    }

    private void measure() {
        for (final double[] t : this.group("front")) {
            for (int k = 0; k < 3; k++) {
                this.outerRadius = Math.max(this.outerRadius, Math.hypot(t[5 * k], t[5 * k + 1]));
            }
        }
        this.screenInner = Double.MAX_VALUE;
        for (final double[] t : this.group("screen")) {
            if (PegasusGateModel.normal(t)[2] < 0.9) {
                continue;
            }
            for (int k = 0; k < 3; k++) {
                final double r = Math.hypot(t[5 * k], t[5 * k + 1]);
                this.screenInner = Math.min(this.screenInner, r);
                this.screenOuter = Math.max(this.screenOuter, r);
            }
        }
    }
}
