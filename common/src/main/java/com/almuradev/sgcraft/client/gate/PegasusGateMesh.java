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

import com.almuradev.sgcraft.SGCraft;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The triangles of a Pegasus gate, built from {@link PegasusGateModel} at the size of a standard gate. The ring is
 * {@link PegasusGateModel#CHEVRONS} identical sections, so only one is kept, to be drawn turned round the ring; the
 * chevron is kept once too. The screens and their glyphs sit between the chevrons, so they are built for each number
 * of chevrons a gate has.
 */
public final class PegasusGateMesh {

    /** The ring's parts, each drawn in its own texture. */
    public static final int FRONT = 0, BACK = 1, OUTER = 2, METAL = 3;
    /** The chevron's parts. */
    public static final int CHEVRON_METAL = 0, CHEVRON_DARK = 1, LIGHT_BLOCK = 2, LIGHT_BARS = 3;

    private static final double RING_OUTER_RADIUS = 2.5;
    // How many glyphs a Pegasus gate has, one to each screen round its ring, and the angle between them
    private static final int GLYPHS = 36;
    private static final double GLYPH_ANGLE = 360.0 / PegasusGateMesh.GLYPHS;
    // The screens' crystal is CRYSTAL_SIZE metres across its texture; each screen shows a different part of it, as the
    // screens were cut from different parts of a block of it
    private static final double CRYSTAL_SIZE = 2;
    // Which of the reference model's glyphs each of the gate's symbols is, in the order of its symbol strip
    private static final int[] REFERENCE_GLYPH = {
            6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
            24, 25, 28, 32, 29, 31, 34, 33, 26, 27, 30, 35, 0, 1, 2, 3, 4, 5};

    private static PegasusGateMesh instance;
    private static boolean failed;

    private final PegasusGateModel model;
    private final PegasusGateModel glyphs;
    private final double scale;
    private final float[][] ring = new float[4][];
    private final float[] separators;
    private final float[][] chevron = new float[4][];
    private final Map<Integer, float[][]> screensByChevrons = new HashMap<>();

    private PegasusGateMesh(final PegasusGateModel model, final PegasusGateModel glyphs) {
        this.model = model;
        this.glyphs = glyphs;
        this.scale = PegasusGateMesh.RING_OUTER_RADIUS / model.outerRadius();
        this.ring[PegasusGateMesh.FRONT] = this.copies("front", 1, 0);
        this.ring[PegasusGateMesh.BACK] = this.copies("back", 1, 0);
        this.ring[PegasusGateMesh.OUTER] = this.copies("outer", 1, 0);
        final GateGeometry.Builder metal = new GateGeometry.Builder();
        this.addCopies(metal, model, "rim", 1, 0);
        this.addCopies(metal, model, "wall", 1, 0);
        this.ring[PegasusGateMesh.METAL] = metal.build();
        this.separators = this.copies("separator", PegasusGateModel.SCREENS, PegasusGateModel.SCREEN_ANGLE);
        this.chevron[PegasusGateMesh.CHEVRON_METAL] = this.copies("chevron", 1, 0);
        this.chevron[PegasusGateMesh.CHEVRON_DARK] = this.copies("chevron_dark", 1, 0);
        this.chevron[PegasusGateMesh.LIGHT_BLOCK] = this.lightBlock();
        this.chevron[PegasusGateMesh.LIGHT_BARS] = this.copies("light_bars", 1, 0);
    }

    /** @return the mesh, or null if the model couldn't be loaded */
    public static synchronized PegasusGateMesh get() {
        if (PegasusGateMesh.instance == null && !PegasusGateMesh.failed) {
            try (InputStream gate = PegasusGateMesh.class.getResourceAsStream(
                    "/assets/sgcraft/models/gate/pegasus_gate.txt");
                 InputStream glyphs = PegasusGateMesh.class.getResourceAsStream(
                         "/assets/sgcraft/models/gate/pegasus_glyphs.txt")) {
                if (gate == null || glyphs == null) {
                    throw new IllegalStateException("The Pegasus gate model is missing");
                }
                PegasusGateMesh.instance = new PegasusGateMesh(PegasusGateModel.load(gate),
                        PegasusGateModel.load(glyphs));
            } catch (final Exception e) {
                PegasusGateMesh.failed = true;
                SGCraft.LOG.error("Unable to load the Pegasus gate model", e);
            }
        }
        return PegasusGateMesh.instance;
    }

    /**
     * @return the triangles of one of the ring's parts, for one of its sections; the others are the
     *     same, turned {@link PegasusGateModel#SECTION_ANGLE} degrees apart
     */
    public float[] ring(final int part) {
        return this.ring[part];
    }

    /** @return the metal between the screens, all round the ring */
    public float[] separators() {
        return this.separators;
    }

    /** @return the triangles of one of the top chevron's parts */
    public float[] chevron(final int part) {
        return this.chevron[part];
    }

    /** @return the screens' crystal, for a gate with the given number of chevrons */
    public synchronized float[] screens(final int chevrons) {
        return this.screensFor(chevrons)[0];
    }

    /** @return the glyphs standing out of the screens, for a gate with the given number of chevrons */
    public synchronized float[] glyphs(final int chevrons) {
        return this.screensFor(chevrons)[1];
    }

    private float[][] screensFor(final int chevrons) {
        return this.screensByChevrons.computeIfAbsent(chevrons, this::buildScreens);
    }

    // Each screen with the crystal across it at its own size and from its own part of the texture, and its glyph
    private float[][] buildScreens(final int chevrons) {
        final double[] angles = PegasusGateMesh.screenAngles(chevrons);
        final double half = PegasusGateModel.SCREEN_ANGLE / 2;
        final double in = this.model.screenInner();
        final double out = this.model.screenOuter();
        final double across = Math.toRadians(PegasusGateModel.SCREEN_ANGLE) * (in + out) / 2
                / PegasusGateMesh.CRYSTAL_SIZE;
        final double down = (out - in) / PegasusGateMesh.CRYSTAL_SIZE;
        final List<double[]> tris = this.model.group("screen");
        final List<double[]> normals = this.model.cornerNormals("screen");
        final GateGeometry.Builder screens = new GateGeometry.Builder();
        final GateGeometry.Builder glyphs = new GateGeometry.Builder();
        for (int s = 0; s < angles.length; s++) {
            final double turn = Math.toRadians(angles[s] - 90);
            final double offsetU = (s * 0.37) % 1;
            final double offsetV = (s * 0.61) % 1;
            for (int i = 0; i < tris.size(); i++) {
                final double[] t = tris.get(i);
                final double[] n = normals.get(i);
                for (int v = 0; v < 3; v++) {
                    final double x = t[5 * v];
                    final double y = t[5 * v + 1];
                    final double angle = Math.toDegrees(Math.atan2(y, x));
                    final double r = Math.hypot(x, y);
                    final double u = Math.max(0, Math.min(1, (90 + half - angle) / (2 * half)));
                    final double w = Math.max(0, Math.min(1, (out - r) / (out - in)));
                    this.corner(screens, t, n, v, turn, u * across + offsetU, w * down + offsetV);
                }
            }
            final String glyph = "glyph" + PegasusGateMesh.REFERENCE_GLYPH[PegasusGateMesh.glyphAt(angles[s])];
            final List<double[]> glyphTris = this.glyphs.group(glyph);
            final List<double[]> glyphNormals = this.glyphs.cornerNormals(glyph);
            for (int i = 0; i < glyphTris.size(); i++) {
                final double[] t = glyphTris.get(i);
                for (int v = 0; v < 3; v++) {
                    this.corner(glyphs, t, glyphNormals.get(i), v, turn, t[5 * v + 3], t[5 * v + 4]);
                }
            }
        }
        return new float[][] {screens.build(), glyphs.build()};
    }

    // The screens' places round the ring: the gaps between the chevrons, each split into screens about a glyph apart,
    // so they line up with the chevrons. Angles (degrees) rise anticlockwise round the ring.
    private static double[] screenAngles(final int chevrons) {
        final int first = chevrons > 7 ? 0 : 1;
        final double[] at = new double[chevrons];
        for (int k = 0; k < chevrons; k++) {
            at[k] = ((90 - (first + k - 4) * StargateBaseRenderer.CHEVRON_SPACING) % 360 + 360) % 360;
        }
        Arrays.sort(at);
        final int[] between = new int[chevrons];
        int count = 0;
        for (int k = 0; k < chevrons; k++) {
            between[k] = Math.max(1, (int) Math.round(PegasusGateMesh.gap(at, k) / PegasusGateMesh.GLYPH_ANGLE));
            count += between[k];
        }
        final double[] angles = new double[count];
        int s = 0;
        for (int k = 0; k < chevrons; k++) {
            for (int t = 0; t < between[k]; t++) {
                angles[s++] = at[k] + PegasusGateMesh.gap(at, k) * t / between[k];
            }
        }
        return angles;
    }

    private static double gap(final double[] at, final int k) {
        return k + 1 < at.length ? at[k + 1] - at[k] : at[0] + 360 - at[k];
    }

    // The glyph on the screen at the given angle: the glyphs go clockwise round the ring from the one at the top
    private static int glyphAt(final double angle) {
        final int k = (int) Math.round((90 - angle) / PegasusGateMesh.GLYPH_ANGLE);
        return (k % PegasusGateMesh.GLYPHS + PegasusGateMesh.GLYPHS) % PegasusGateMesh.GLYPHS;
    }

    // The light above the chevron, its texture across it from in front over its bounds
    private float[] lightBlock() {
        final List<double[]> tris = this.model.group("light_block");
        double x0 = Double.MAX_VALUE;
        double x1 = -Double.MAX_VALUE;
        double y0 = Double.MAX_VALUE;
        double y1 = -Double.MAX_VALUE;
        for (final double[] t : tris) {
            for (int v = 0; v < 3; v++) {
                x0 = Math.min(x0, t[5 * v]);
                x1 = Math.max(x1, t[5 * v]);
                y0 = Math.min(y0, t[5 * v + 1]);
                y1 = Math.max(y1, t[5 * v + 1]);
            }
        }
        final List<double[]> normals = this.model.cornerNormals("light_block");
        final GateGeometry.Builder out = new GateGeometry.Builder();
        for (int i = 0; i < tris.size(); i++) {
            final double[] t = tris.get(i);
            for (int v = 0; v < 3; v++) {
                this.corner(out, t, normals.get(i), v, 0, (t[5 * v] - x0) / (x1 - x0), (y1 - t[5 * v + 1]) / (y1 - y0));
            }
        }
        return out.build();
    }

    private float[] copies(final String name, final int count, final double step) {
        final GateGeometry.Builder out = new GateGeometry.Builder();
        this.addCopies(out, this.model, name, count, step);
        return out.build();
    }

    // A group turned round the ring the given number of times, a step of degrees apart, with its own texture
    // coordinates
    private void addCopies(final GateGeometry.Builder out, final PegasusGateModel source, final String name,
            final int count, final double step) {
        final List<double[]> tris = source.group(name);
        final List<double[]> normals = source.cornerNormals(name);
        for (int k = 0; k < count; k++) {
            final double turn = Math.toRadians(k * step);
            for (int i = 0; i < tris.size(); i++) {
                final double[] t = tris.get(i);
                for (int v = 0; v < 3; v++) {
                    this.corner(out, t, normals.get(i), v, turn, t[5 * v + 3], t[5 * v + 4]);
                }
            }
        }
    }

    // One corner of a triangle, turned about the gate's centre and scaled into blocks
    private void corner(final GateGeometry.Builder out, final double[] t, final double[] n, final int v,
            final double turn, final double u, final double w) {
        final double c = Math.cos(turn);
        final double sn = Math.sin(turn);
        final double x = t[5 * v];
        final double y = t[5 * v + 1];
        final double nx = n[3 * v];
        final double ny = n[3 * v + 1];
        out.corner((x * c - y * sn) * this.scale, (x * sn + y * c) * this.scale, t[5 * v + 2] * this.scale, u, w,
                nx * c - ny * sn, nx * sn + ny * c, n[3 * v + 2]);
    }
}
