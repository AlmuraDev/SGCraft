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

package com.almuradev.sgcraft.mod.client.generator;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.client.model.ObjMesh;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

/**
 * The Naquadah Generator's model, 2.2.0's, read from its OBJ files (see {@link ObjMesh}) into triangles for each of
 * its materials: once with
 * its arms out, as it is switched on, and once with them drawn in. Its corners are in the block's space, facing
 * north, as the block's own are.
 */
public final class GeneratorMesh {

    /** Its materials, each drawn with its own texture. */
    public static final String[] MATERIALS = {"body", "dark", "rod", "dome", "crystal", "roof", "coil", "sphere"};
    /** The materials that glow while it runs. */
    public static final int ROD = 2, DOME = 3, CRYSTAL = 4;

    private static @Nullable GeneratorMesh instance;
    private static boolean failed;

    private final float[][] on;
    private final float[][] off;

    private GeneratorMesh(final float[][] on, final float[][] off) {
        this.on = on;
        this.off = off;
    }

    /** @return the mesh, or null if the model couldn't be loaded */
    public static synchronized @Nullable GeneratorMesh get() {
        if (GeneratorMesh.instance == null && !GeneratorMesh.failed) {
            try {
                GeneratorMesh.instance = new GeneratorMesh(GeneratorMesh.load("on"), GeneratorMesh.load("off"));
            } catch (final IOException | RuntimeException e) {
                GeneratorMesh.failed = true;
                SGCraft.LOG.error("Couldn't load the Naquadah Generator's model", e);
            }
        }
        return GeneratorMesh.instance;
    }

    /** @return the triangles of one material, with its arms out or drawn in */
    public float[] part(final boolean out, final int material) {
        return (out ? this.on : this.off)[material];
    }

    private static float[][] load(final String state) throws IOException {
        return ObjMesh.load("/assets/sgcraft/models/generator/naquadah_generator_" + state + ".obj",
                GeneratorMesh.MATERIALS);
    }
}
