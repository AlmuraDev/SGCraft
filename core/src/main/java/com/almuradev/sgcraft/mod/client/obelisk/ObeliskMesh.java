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

package com.almuradev.sgcraft.mod.client.obelisk;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.client.model.ObjMesh;
import com.almuradev.sgcraft.mod.obelisk.ObeliskKind;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/**
 * The Asgard obelisks' models, 2.2.0's, read from their OBJ files into triangles for each material, each with its
 * texture. Each model is the whole obelisk, both its halves, in the space of its bottom block, facing north.
 */
public final class ObeliskMesh {

    private static final String[] HAMMER = {"stone", "sandstone", "hammer", "ring", "gem"};
    private static final String[] KNOTWORK = {"celtic_stone", "celtic_knot", "celtic_front", "gem"};
    private static final Map<String, String> TEXTURES = Map.of("stone", "asgard_obelisk_stone",
            "sandstone", "asgard_obelisk_sandstone", "hammer", "asgard_obelisk_hammer", "ring", "asgard_obelisk_ring",
            "gem", "asgard_obelisk_gem", "celtic_stone", "asgard_obelisk_knotwork_stone",
            "celtic_knot", "asgard_obelisk_knotwork_knot", "celtic_front", "asgard_obelisk_knotwork_front");

    private static final Map<ObeliskKind, ObeliskMesh> MESHES = new EnumMap<>(ObeliskKind.class);
    private static boolean failed;

    private final float[][] parts;
    private final Identifier[] textures;
    private final int gem;

    private ObeliskMesh(final float[][] parts, final String[] materials) {
        this.parts = parts;
        this.textures = new Identifier[materials.length];
        int gem = 0;
        for (int i = 0; i < materials.length; i++) {
            this.textures[i] = SGCraft.id("textures/block/" + ObeliskMesh.TEXTURES.get(materials[i]) + ".png");
            if (materials[i].equals("gem")) {
                gem = i;
            }
        }
        this.gem = gem;
    }

    /** @return the model of a kind of obelisk, or null if it couldn't be loaded */
    public static synchronized @Nullable ObeliskMesh get(final ObeliskKind kind) {
        if (!ObeliskMesh.MESHES.containsKey(kind) && !ObeliskMesh.failed) {
            final boolean hammer = kind == ObeliskKind.HAMMER;
            final String[] materials = hammer ? ObeliskMesh.HAMMER : ObeliskMesh.KNOTWORK;
            try {
                ObeliskMesh.MESHES.put(kind, new ObeliskMesh(ObjMesh.load("/assets/sgcraft/models/obelisk/"
                        + (hammer ? "asgard_obelisk" : "asgard_obelisk_knotwork") + ".obj", materials), materials));
            } catch (final IOException | RuntimeException e) {
                ObeliskMesh.failed = true;
                SGCraft.LOG.error("Couldn't load the Asgard obelisk's model", e);
            }
        }
        return ObeliskMesh.MESHES.get(kind);
    }

    /** @return how many materials it has */
    public int parts() {
        return this.parts.length;
    }

    /** @return the triangles of one material */
    public float[] part(final int part) {
        return this.parts[part];
    }

    /** @return the texture of one material */
    public Identifier texture(final int part) {
        return this.textures[part];
    }

    /** @return which material is its gem, whose texture's corners are clear */
    public int gem() {
        return this.gem;
    }
}
