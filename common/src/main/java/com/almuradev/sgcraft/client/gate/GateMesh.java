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

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Triangles of a gate kept on the GPU, so they are uploaded once rather than sent again every frame. A gate's parts
 * are drawn from these by {@link GateFeatureRenderer}.
 *
 * <p>Each corner is its position, its texture coordinates and its normal, in {@link #FORMAT}.</p>
 */
public final class GateMesh {

    /** How each corner is laid out on the GPU. */
    public static final VertexFormat FORMAT = VertexFormat.builder(0)
            .addAttribute("Position", GpuFormat.RGB32_FLOAT)
            .addAttribute("UV0", GpuFormat.RG32_FLOAT)
            .addAttribute("Normal", GpuFormat.RGBA8_SNORM)
            .build();

    // The meshes made from each array of triangles, so an array is only uploaded once
    private static final Map<float[], GateMesh> MESHES = new IdentityHashMap<>();

    private final @Nullable GpuBuffer buffer;
    private final int vertices;

    private GateMesh(final @Nullable GpuBuffer buffer, final int vertices) {
        this.buffer = buffer;
        this.vertices = vertices;
    }

    /**
     * The mesh of the given triangles, laid out as {@link GateGeometry} lays them out. The same array always gives the
     * same mesh, so it must not be changed after it is first drawn. Only call this on the render thread.
     */
    public static GateMesh of(final float[] triangles) {
        return GateMesh.MESHES.computeIfAbsent(triangles, GateMesh::upload);
    }

    private static GateMesh upload(final float[] triangles) {
        final int vertices = triangles.length / GateGeometry.FLOATS * 3;
        if (vertices == 0) {
            return new GateMesh(null, 0);
        }
        final ByteBuffer data = MemoryUtil.memAlloc(vertices * GateMesh.FORMAT.getVertexSize());
        try {
            for (int c = 0; c < triangles.length; c += 8) {
                data.putFloat(triangles[c]).putFloat(triangles[c + 1]).putFloat(triangles[c + 2])
                        .putFloat(triangles[c + 3]).putFloat(triangles[c + 4])
                        .put(GateMesh.normal(triangles[c + 5])).put(GateMesh.normal(triangles[c + 6]))
                        .put(GateMesh.normal(triangles[c + 7])).put((byte) 0);
            }
            data.flip();
            return new GateMesh(RenderSystem.getDevice().createBuffer(() -> "SGCraft gate mesh", GpuBuffer.USAGE_VERTEX,
                    data), vertices);
        } finally {
            MemoryUtil.memFree(data);
        }
    }

    private static byte normal(final float n) {
        return (byte) Math.round(Math.max(-1.0F, Math.min(1.0F, n)) * 127.0F);
    }

    /** @return the buffer holding the corners, or null if there are none */
    public @Nullable GpuBuffer buffer() {
        return this.buffer;
    }

    /** @return how many corners there are, three to each triangle */
    public int vertices() {
        return this.vertices;
    }
}
