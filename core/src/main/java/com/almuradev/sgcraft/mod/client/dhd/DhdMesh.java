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

package com.almuradev.sgcraft.mod.client.dhd;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.client.gate.GateGeometry;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * The DHD's model, the original SGCraft's, read into triangles for each of its textures. Its corners are in the
 * block's frame with the origin at the middle of its bottom face and its front towards +z.
 */
public final class DhdMesh {

    /** The model's textures: its top, its sides, its detail, and the dome in its middle. */
    public static final int TOP = 0, SIDE = 1, DETAIL = 2, BUTTON = 3, TEXTURES = 4;

    private static @Nullable DhdMesh instance;
    private static boolean failed;

    private final float[][] parts = new float[DhdMesh.TEXTURES][];

    private DhdMesh(final JsonObject model) {
        final GateGeometry.Builder[] builders = new GateGeometry.Builder[DhdMesh.TEXTURES];
        for (int i = 0; i < DhdMesh.TEXTURES; i++) {
            builders[i] = new GateGeometry.Builder();
        }
        for (final JsonElement element : model.getAsJsonArray("faces")) {
            final JsonObject face = element.getAsJsonObject();
            final int texture = face.get("texture").getAsInt();
            if (texture < 0 || texture >= DhdMesh.TEXTURES) {
                continue;
            }
            final JsonArray vertices = face.getAsJsonArray("vertices");
            for (final JsonElement triangle : face.getAsJsonArray("triangles")) {
                for (final JsonElement corner : triangle.getAsJsonArray()) {
                    final JsonArray v = vertices.get(corner.getAsInt()).getAsJsonArray();
                    final double[] uv = DhdMesh.uv(texture, v.get(6).getAsDouble(), v.get(7).getAsDouble());
                    builders[texture].corner(v.get(0).getAsDouble(), v.get(1).getAsDouble(), v.get(2).getAsDouble(),
                            uv[0], uv[1], v.get(3).getAsDouble(), v.get(4).getAsDouble(), v.get(5).getAsDouble());
                }
            }
        }
        for (int i = 0; i < DhdMesh.TEXTURES; i++) {
            this.parts[i] = builders[i].build();
        }
    }

    // The detail and the dome share one texture, each a quarter of it: the detail its bottom right, the dome its top
    // left
    private static double[] uv(final int texture, final double u, final double v) {
        return switch (texture) {
            case DhdMesh.DETAIL -> new double[] {0.5 + u * 0.5, 0.5 + v * 0.5};
            case DhdMesh.BUTTON -> new double[] {u * 0.5, v * 0.5};
            default -> new double[] {u, v};
        };
    }

    /** @return the mesh, or null if the model couldn't be loaded */
    public static synchronized @Nullable DhdMesh get() {
        if (DhdMesh.instance == null && !DhdMesh.failed) {
            try (InputStream in = DhdMesh.class.getResourceAsStream("/assets/sgcraft/models/dhd/dhd.smeg")) {
                if (in == null) {
                    throw new IllegalStateException("dhd.smeg is missing");
                }
                DhdMesh.instance = new DhdMesh(JsonParser.parseReader(new InputStreamReader(in,
                        StandardCharsets.UTF_8)).getAsJsonObject());
            } catch (final Exception e) {
                DhdMesh.failed = true;
                SGCraft.LOG.error("Unable to load the DHD model", e);
            }
        }
        return DhdMesh.instance;
    }

    /** @return the triangles drawn in one of the model's textures */
    public float[] part(final int texture) {
        return this.parts[texture];
    }
}
