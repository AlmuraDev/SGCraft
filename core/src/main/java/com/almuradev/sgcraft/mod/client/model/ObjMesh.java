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

package com.almuradev.sgcraft.mod.client.model;

import com.almuradev.sgcraft.mod.client.gate.GateGeometry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a model from an OBJ file into triangles, one array for each of its materials, laid out as {@link GateGeometry}
 * lays them out. Each face is turned into triangles from its first corner and lit by its own flat normal. Texture
 * coordinates are counted up from the bottom, as OBJ counts them. A face of a material not named is left out.
 */
public final class ObjMesh {

    private ObjMesh() {
    }

    /**
     * @param path the file, as a resource path
     * @param materials the materials to read, in the order the arrays come back in
     * @return the triangles of each material
     */
    public static float[][] load(final String path, final String[] materials) throws IOException {
        try (InputStream in = ObjMesh.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException(path + " is missing");
            }
            final List<double[]> positions = new ArrayList<>();
            final List<double[]> uvs = new ArrayList<>();
            final GateGeometry.Builder[] builders = new GateGeometry.Builder[materials.length];
            for (int i = 0; i < builders.length; i++) {
                builders[i] = new GateGeometry.Builder();
            }
            int material = -1;
            final BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                final String[] parts = line.trim().split("\\s+");
                switch (parts[0]) {
                    case "v" -> positions.add(new double[] {Double.parseDouble(parts[1]),
                            Double.parseDouble(parts[2]), Double.parseDouble(parts[3])});
                    case "vt" -> uvs.add(new double[] {Double.parseDouble(parts[1]),
                            1 - Double.parseDouble(parts[2])});
                    case "usemtl" -> material = ObjMesh.material(materials, parts[1]);
                    case "f" -> {
                        if (material >= 0) {
                            ObjMesh.face(builders[material], parts, positions, uvs);
                        }
                    }
                    default -> {
                    }
                }
            }
            final float[][] built = new float[builders.length][];
            for (int i = 0; i < builders.length; i++) {
                built[i] = builders[i].build();
            }
            return built;
        }
    }

    private static int material(final String[] materials, final String name) {
        for (int i = 0; i < materials.length; i++) {
            if (materials[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }

    private static void face(final GateGeometry.Builder builder, final String[] parts, final List<double[]> positions,
            final List<double[]> uvs) {
        final int corners = parts.length - 1;
        final double[][] p = new double[corners][];
        final double[][] t = new double[corners][];
        for (int i = 0; i < corners; i++) {
            final String[] index = parts[i + 1].split("/");
            p[i] = positions.get(Integer.parseInt(index[0]) - 1);
            t[i] = index.length > 1 && !index[1].isEmpty() ? uvs.get(Integer.parseInt(index[1]) - 1)
                    : new double[] {0, 0};
        }
        final double[] n = ObjMesh.normal(p);
        for (int i = 1; i + 1 < corners; i++) {
            for (final int k : new int[] {0, i, i + 1}) {
                builder.corner(p[k][0], p[k][1], p[k][2], t[k][0], t[k][1], n[0], n[1], n[2]);
            }
        }
    }

    // Newell's normal of a flat polygon
    private static double[] normal(final double[][] p) {
        double x = 0;
        double y = 0;
        double z = 0;
        for (int i = 0; i < p.length; i++) {
            final double[] a = p[i];
            final double[] b = p[(i + 1) % p.length];
            x += (a[1] - b[1]) * (a[2] + b[2]);
            y += (a[2] - b[2]) * (a[0] + b[0]);
            z += (a[0] - b[0]) * (a[1] + b[1]);
        }
        final double length = Math.max(1e-9, Math.sqrt(x * x + y * y + z * z));
        return new double[] {x / length, y / length, z / length};
    }
}
