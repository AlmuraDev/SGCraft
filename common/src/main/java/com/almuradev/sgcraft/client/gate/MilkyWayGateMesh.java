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
import java.util.ArrayList;
import java.util.List;

/**
 * The triangles of a Milky Way gate, built once from {@link MilkyWayGateModel} at the size of a standard gate and
 * kept: the rings' parts ({@link MilkyWayGateModel#BODY} to {@link MilkyWayGateModel#GLYPHS}), then each piece of
 * the chevron, one list for each way its corners move.
 *
 * <p>Triangles are in the layout {@link GateGeometry} gives.</p>
 */
public final class MilkyWayGateMesh {

    /** How far a standard gate's ring reaches from its centre, in blocks. */
    public static final double RING_OUTER_RADIUS = 2.5;

    // How many steps round the gate its rings are turned in
    private static final int STEPS = 180;
    // The metals repeat across the gate every this many metres of the model
    private static final double METAL_REPEAT = 1.2;
    // A chevron's lights are laid over the whole chevron, from LIGHT_HALF metres either side of its middle, and from
    // LIGHT_TOP down to LIGHT_BOTTOM from the gate's centre
    private static final double LIGHT_HALF = 0.54;
    private static final double LIGHT_TOP = 6.09;
    private static final double LIGHT_BOTTOM = 5.45;

    private static MilkyWayGateMesh instance;
    private static boolean failed;

    private final List<float[]> lists;
    private final int moves;

    private MilkyWayGateMesh(final List<float[]> lists, final int moves) {
        this.lists = lists;
        this.moves = moves;
    }

    /** @return the mesh, or null if the model couldn't be loaded */
    public static synchronized MilkyWayGateMesh get() {
        if (MilkyWayGateMesh.instance == null && !MilkyWayGateMesh.failed) {
            try (InputStream in = MilkyWayGateMesh.class.getResourceAsStream(
                    "/assets/sgcraft/models/gate/milkyway_gate.txt")) {
                if (in == null) {
                    throw new IllegalStateException("milkyway_gate.txt is missing");
                }
                MilkyWayGateMesh.instance = MilkyWayGateMesh.build(MilkyWayGateModel.load(in));
            } catch (final Exception e) {
                MilkyWayGateMesh.failed = true;
                SGCraft.LOG.error("Unable to load the Milky Way gate model", e);
            }
        }
        return MilkyWayGateMesh.instance;
    }

    /** @return how many ways the corners of the top chevron move as it locks a symbol */
    public int moves() {
        return this.moves;
    }

    /** @return the triangles of one of the rings' parts */
    public float[] part(final int part) {
        return this.lists.get(part);
    }

    /** @return the triangles of one piece of the chevron, only the corners that move the given way */
    public float[] chevron(final int piece, final int move) {
        return this.lists.get(MilkyWayGateModel.CHEVRON + piece * this.moves + move);
    }

    private static MilkyWayGateMesh build(final MilkyWayGateModel model) {
        final int moves = model.moveCount();
        final int count = MilkyWayGateModel.CHEVRON + MilkyWayGateModel.PIECES * moves;
        final List<List<double[]>> tris = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tris.add(new ArrayList<>());
        }
        final int[] into = new int[1];
        final MilkyWayGateModel.Sink sink = (part, a, b, c, n) -> tris.get(
                part < MilkyWayGateModel.CHEVRON ? part : into[0]).add(
                new double[] {a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2], n[0], n[1], n[2]});
        model.buildFixed(sink, (int) Math.ceil(MilkyWayGateMesh.STEPS / (360 / MilkyWayGateModel.COLUMN)));
        model.buildRing(sink, MilkyWayGateMesh.STEPS);
        for (int piece = 0; piece < MilkyWayGateModel.PIECES; piece++) {
            for (int m = 0; m < moves; m++) {
                into[0] = MilkyWayGateModel.CHEVRON + piece * moves + m;
                model.buildChevron(sink, piece, m);
            }
        }
        final double scale = MilkyWayGateMesh.RING_OUTER_RADIUS / MilkyWayGateModel.OUTER_RADIUS;
        final List<float[]> lists = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final boolean light = i >= MilkyWayGateModel.CHEVRON + moves;
            lists.add(MilkyWayGateMesh.flatten(tris.get(i), scale, light));
        }
        return new MilkyWayGateMesh(lists, moves);
    }

    // The triangles scaled into blocks, each corner given texture coordinates: a light's glass laid over the whole
    // chevron, or a metal laid flat on whichever of the gate's planes the triangle faces most
    private static float[] flatten(final List<double[]> tris, final double scale, final boolean light) {
        final GateGeometry.Builder out = new GateGeometry.Builder();
        final double k = 1 / MilkyWayGateMesh.METAL_REPEAT;
        for (final double[] t : tris) {
            final double ax = Math.abs(t[9]);
            final double ay = Math.abs(t[10]);
            final double az = Math.abs(t[11]);
            for (int v = 0; v < 3; v++) {
                final double x = t[3 * v];
                final double y = t[3 * v + 1];
                final double z = t[3 * v + 2];
                final double u;
                final double w;
                if (light) {
                    u = (x + MilkyWayGateMesh.LIGHT_HALF) / (2 * MilkyWayGateMesh.LIGHT_HALF);
                    w = (MilkyWayGateMesh.LIGHT_TOP - y) / (MilkyWayGateMesh.LIGHT_TOP - MilkyWayGateMesh.LIGHT_BOTTOM);
                } else if (az >= ax && az >= ay) {
                    u = x * k;
                    w = y * k;
                } else if (ax >= ay) {
                    u = y * k;
                    w = z * k;
                } else {
                    u = x * k;
                    w = z * k;
                }
                out.corner(x * scale, y * scale, z * scale, u, w, t[9], t[10], t[11]);
            }
        }
        return out.build();
    }
}
