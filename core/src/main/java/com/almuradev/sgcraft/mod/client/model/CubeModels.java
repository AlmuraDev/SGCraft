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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Reads an entity's cube model from a JSON file in the mod's assets, so its shape lives in data rather than in code. A
 * file holds one or more layers by name, all on one texture:
 *
 * <pre>{@code
 * {
 *   "texture_size": [width, height],
 *   "layers": {
 *     "main": {
 *       "body": {
 *         "offset": [x, y, z], "rotation": [x, y, z],
 *         "cubes": [{"uv": [u, v], "from": [x, y, z], "size": [width, height, depth], "grow": [x, y, z]}],
 *         "parts": {"child": {...}}
 *       }
 *     }
 *   }
 * }
 * }</pre>
 *
 * <p>Each layer is a set of parts by name. A part's offset, rotation, cubes and children may each be left out, as may
 * a cube's grow, which swells it about its middle without changing where its texture lies. A part may instead name a
 * part beside it with {@code "copy"}, to take that part's cubes and children at its own offset and rotation, as a
 * vehicle's wheels do. Sizes are in pixels, as entity models' are, and rotations in degrees.</p>
 */
public final class CubeModels {

    private CubeModels() {
    }

    /**
     * @param path the file, as a resource path
     * @param layer the layer's name in it
     * @return the layer, ready to register as a model layer
     * @throws UncheckedIOException if the file is missing or can't be read
     * @throws IllegalArgumentException if it has no such layer
     */
    public static LayerDefinition layer(final String path, final String layer) {
        final JsonObject file = CubeModels.read(path);
        final JsonObject parts = file.getAsJsonObject("layers").getAsJsonObject(layer);
        if (parts == null) {
            throw new IllegalArgumentException(path + " has no layer " + layer);
        }
        final MeshDefinition mesh = new MeshDefinition();
        CubeModels.addParts(mesh.getRoot(), parts);
        final JsonArray size = file.getAsJsonArray("texture_size");
        return LayerDefinition.create(mesh, size.get(0).getAsInt(), size.get(1).getAsInt());
    }

    private static JsonObject read(final String path) {
        try (InputStream in = CubeModels.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException(path + " is missing");
            }
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void addParts(final PartDefinition parent, final JsonObject parts) {
        for (final Map.Entry<String, JsonElement> entry : parts.entrySet()) {
            final JsonObject part = CubeModels.copied(parts, entry.getKey(), entry.getValue().getAsJsonObject());
            final CubeListBuilder cubes = CubeListBuilder.create();
            if (part.has("cubes")) {
                for (final JsonElement element : part.getAsJsonArray("cubes")) {
                    final JsonObject cube = element.getAsJsonObject();
                    final float[] uv = CubeModels.floats(cube, "uv", 2);
                    final float[] from = CubeModels.floats(cube, "from", 3);
                    final float[] size = CubeModels.floats(cube, "size", 3);
                    final float[] grow = CubeModels.floats(cube, "grow", 3);
                    cubes.texOffs((int) uv[0], (int) uv[1]).addBox(from[0], from[1], from[2], size[0], size[1],
                            size[2], new CubeDeformation(grow[0], grow[1], grow[2]));
                }
            }
            final float[] offset = CubeModels.floats(part, "offset", 3);
            final float[] rotation = CubeModels.floats(part, "rotation", 3);
            final PartDefinition child = parent.addOrReplaceChild(entry.getKey(), cubes,
                    PartPose.offsetAndRotation(offset[0], offset[1], offset[2], rotation[0] * Mth.DEG_TO_RAD,
                            rotation[1] * Mth.DEG_TO_RAD, rotation[2] * Mth.DEG_TO_RAD));
            if (part.has("parts")) {
                CubeModels.addParts(child, part.getAsJsonObject("parts"));
            }
        }
    }

    // A part that copies a sibling takes the sibling's cubes and children, keeping its own offset and rotation
    private static JsonObject copied(final JsonObject siblings, final String name, final JsonObject part) {
        if (!part.has("copy")) {
            return part;
        }
        final String from = part.get("copy").getAsString();
        final JsonElement source = siblings.get(from);
        if (source == null || from.equals(name) || source.getAsJsonObject().has("copy")) {
            throw new IllegalArgumentException(name + " copies " + from + ", which isn't a part beside it to copy");
        }
        final JsonObject merged = part.deepCopy();
        for (final String key : new String[] {"cubes", "parts"}) {
            if (source.getAsJsonObject().has(key)) {
                merged.add(key, source.getAsJsonObject().get(key));
            }
        }
        return merged;
    }

    // A list of numbers, or as many zeros if it is left out
    private static float[] floats(final JsonObject object, final String key, final int count) {
        final float[] values = new float[count];
        final JsonArray array = object.getAsJsonArray(key);
        if (array != null) {
            for (int i = 0; i < count; i++) {
                values[i] = array.get(i).getAsFloat();
            }
        }
        return values;
    }
}
