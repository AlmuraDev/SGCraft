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

package com.almuradev.sgcraft.mod.client.view;

import com.almuradev.sgcraft.mod.SGCraft;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The meshes of a remote view's sections, built with the game's own block and fluid renderers and kept on the GPU
 * until the section changes or is forgotten. A section is built again when it or a neighbor changes, once things have
 * settled for a moment, nearest the camera first and only a few each frame. Only the render thread uses this.
 */
final class RemoteViewMeshes implements AutoCloseable {

    // How long a section waits after it or a neighbor last changed before it is built, so a burst of sections
    // arriving together builds each once
    private static final long SETTLE_NANOS = 100_000_000L;
    // How much building each frame may do
    private static final long BUILD_NANOS = 3_000_000L;
    private static final int MAX_BUILDS = 12;
    // A translucent layer is sorted again once the camera has moved this far, for at most this many layers a frame
    private static final double RESORT_DISTANCE = 1.0;
    private static final int MAX_RESORTS = 8;

    private final Long2ObjectMap<SectionMesh> meshes = new Long2ObjectOpenHashMap<>();
    // Sections waiting to be built, with when they were last marked
    private final Long2LongMap dirty = new Long2LongOpenHashMap();
    private final Map<ChunkSectionLayer, ByteBufferBuilder> builders = new EnumMap<>(ChunkSectionLayer.class);
    private final ByteBufferBuilder indexBuilder = new ByteBufferBuilder(64 * 1024);

    RemoteViewMeshes() {
        for (final ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            this.builders.put(layer, new ByteBufferBuilder(256 * 1024));
        }
    }

    /** One section's meshes, by layer, and where the section is. */
    static final class SectionMesh {

        final long pos;
        final int x;
        final int y;
        final int z;
        final Map<ChunkSectionLayer, LayerMesh> layers = new EnumMap<>(ChunkSectionLayer.class);

        SectionMesh(final long pos) {
            this.pos = pos;
            this.x = SectionPos.sectionToBlockCoord(SectionPos.x(pos));
            this.y = SectionPos.sectionToBlockCoord(SectionPos.y(pos));
            this.z = SectionPos.sectionToBlockCoord(SectionPos.z(pos));
        }

        void close() {
            this.layers.values().forEach(LayerMesh::close);
            this.layers.clear();
        }
    }

    /**
     * One layer of a section on the GPU. A translucent layer has its own index buffer, its quads sorted back to front
     * from where the camera was when it was last sorted; the others draw with the shared quad indices.
     */
    static final class LayerMesh {

        final GpuBuffer vertices;
        final int indexCount;
        final @Nullable GpuBuffer indices;
        final IndexType indexType;
        final MeshData.@Nullable SortState sort;
        private Vec3 sortedFrom;

        LayerMesh(final GpuBuffer vertices, final int indexCount, final @Nullable GpuBuffer indices,
                final IndexType indexType, final MeshData.@Nullable SortState sort, final Vec3 sortedFrom) {
            this.vertices = vertices;
            this.indexCount = indexCount;
            this.indices = indices;
            this.indexType = indexType;
            this.sort = sort;
            this.sortedFrom = sortedFrom;
        }

        void close() {
            this.vertices.close();
            if (this.indices != null) {
                this.indices.close();
            }
        }
    }

    /** Marks a section to be built again, with its neighbors, whose faces, shading and fluids depend on it. */
    void changed(final long pos) {
        final long now = System.nanoTime();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    this.dirty.putIfAbsent(SectionPos.offset(pos, dx, dy, dz), now);
                }
            }
        }
    }

    /** Marks sections to be built again on their own, as when the light in them has changed. */
    void relight(final LongList sections) {
        final long now = System.nanoTime();
        for (int i = 0; i < sections.size(); i++) {
            this.dirty.putIfAbsent(sections.getLong(i), now);
        }
    }

    /** Drops a section's meshes. */
    void forget(final long pos) {
        final SectionMesh mesh = this.meshes.remove(pos);
        if (mesh != null) {
            mesh.close();
        }
        this.dirty.remove(pos);
    }

    /** @return every section that has meshes */
    Collection<SectionMesh> sections() {
        return this.meshes.values();
    }

    /** @return the sections that have meshes, within a distance of a point */
    LongList near(final Vec3 point, final double distance) {
        final LongList near = new LongArrayList();
        for (final SectionMesh mesh : this.meshes.values()) {
            if (RemoteViewMeshes.distanceSq(mesh.pos, point) <= (distance + 14) * (distance + 14)) {
                near.add(mesh.pos);
            }
        }
        return near;
    }

    /** @return whether a section the level has, within a distance of a point, is still to be built */
    boolean pendingNear(final RemoteViewLevel level, final Vec3 point, final double distance) {
        for (final long pos : this.dirty.keySet()) {
            if (level.has(pos) && RemoteViewMeshes.distanceSq(pos, point) <= distance * distance) {
                return true;
            }
        }
        return false;
    }

    /** Builds the sections that have settled, nearest the camera first, as far as a frame's share allows. */
    void build(final RemoteViewLevel level, final Vec3 camera) {
        if (this.dirty.isEmpty()) {
            return;
        }
        final long start = System.nanoTime();
        final List<Long> ready = new ArrayList<>();
        final ObjectIterator<Long2LongMap.Entry> iterator = this.dirty.long2LongEntrySet().iterator();
        while (iterator.hasNext()) {
            final Long2LongMap.Entry entry = iterator.next();
            if (!level.has(entry.getLongKey())) {
                // Not here, or gone: nothing to build until it arrives
                final SectionMesh gone = this.meshes.remove(entry.getLongKey());
                if (gone != null) {
                    gone.close();
                }
                iterator.remove();
            } else if (start - entry.getLongValue() >= RemoteViewMeshes.SETTLE_NANOS) {
                ready.add(entry.getLongKey());
            }
        }
        if (ready.isEmpty()) {
            return;
        }
        ready.sort((a, b) -> Double.compare(RemoteViewMeshes.distanceSq(a, camera),
                RemoteViewMeshes.distanceSq(b, camera)));
        final Minecraft minecraft = Minecraft.getInstance();
        final ModelBlockRenderer blocks = new ModelBlockRenderer(minecraft.options.ambientOcclusion().get(), true,
                minecraft.getBlockColors());
        final FluidRenderer fluids = new FluidRenderer(minecraft.getModelManager().getFluidStateModelSet());
        final BlockStateModelSet models = minecraft.getModelManager().getBlockStateModelSet();
        final boolean cutoutLeaves = minecraft.options.cutoutLeaves().get();
        int built = 0;
        for (final long pos : ready) {
            if (built >= RemoteViewMeshes.MAX_BUILDS || System.nanoTime() - start > RemoteViewMeshes.BUILD_NANOS) {
                break;
            }
            this.dirty.remove(pos);
            final SectionMesh mesh = this.compile(pos, level, camera, blocks, fluids, models, cutoutLeaves);
            final SectionMesh old = mesh.layers.isEmpty() ? this.meshes.remove(pos) : this.meshes.put(pos, mesh);
            if (old != null) {
                old.close();
            }
            built++;
        }
    }

    private SectionMesh compile(final long pos, final RemoteViewLevel level, final Vec3 camera,
            final ModelBlockRenderer blocks, final FluidRenderer fluids, final BlockStateModelSet models,
            final boolean cutoutLeaves) {
        final SectionMesh mesh = new SectionMesh(pos);
        final Map<ChunkSectionLayer, BufferBuilder> started = new EnumMap<>(ChunkSectionLayer.class);
        final BlockQuadOutput quads = (x, y, z, quad, instance) ->
                this.begin(started, quad.materialInfo().layer()).putBlockBakedQuad(x, y, z, quad, instance);
        final BlockQuadOutput opaque = (x, y, z, quad, instance) ->
                this.begin(started, ChunkSectionLayer.SOLID).putBlockBakedQuad(x, y, z, quad, instance);
        final FluidRenderer.Output fluidOutput = layer -> this.begin(started, layer);
        final BlockPos origin = new BlockPos(mesh.x, mesh.y, mesh.z);
        BlockModelLighter.enableCaching();
        try {
            for (final BlockPos at : BlockPos.betweenClosed(origin, origin.offset(15, 15, 15))) {
                final BlockState state = level.getBlockState(at);
                if (state.isAir()) {
                    continue;
                }
                // A block that fails to draw is left out, rather than the whole view breaking
                try {
                    final FluidState fluid = state.getFluidState();
                    if (!fluid.isEmpty()) {
                        fluids.tesselate(level, at, fluidOutput, state, fluid);
                    }
                    if (state.getRenderShape() == RenderShape.MODEL) {
                        blocks.tesselateBlock(ModelBlockRenderer.forceOpaque(cutoutLeaves, state) ? opaque : quads,
                                at.getX() & 15, at.getY() & 15, at.getZ() & 15, level, at, state, models.get(state),
                                state.getSeed(at));
                    }
                } catch (final RuntimeException e) {
                    SGCraft.LOG.debug("Couldn't draw {} at {} in a remote view", state, at, e);
                }
            }
        } finally {
            BlockModelLighter.clearCache();
        }
        for (final Map.Entry<ChunkSectionLayer, BufferBuilder> entry : started.entrySet()) {
            final MeshData data = entry.getValue().build();
            if (data != null) {
                mesh.layers.put(entry.getKey(), this.upload(entry.getKey(), data, mesh, camera));
            }
        }
        return mesh;
    }

    private BufferBuilder begin(final Map<ChunkSectionLayer, BufferBuilder> started, final ChunkSectionLayer layer) {
        return started.computeIfAbsent(layer, key -> new BufferBuilder(this.builders.get(key),
                PrimitiveTopology.QUADS, key.vertexFormat()));
    }

    private LayerMesh upload(final ChunkSectionLayer layer, final MeshData data, final SectionMesh section,
            final Vec3 camera) {
        try (data) {
            final GpuDevice device = RenderSystem.getDevice();
            final GpuBuffer vertices = device.createBuffer(() -> "SGCraft remote view " + layer.label(),
                    GpuBuffer.USAGE_VERTEX, data.vertexBuffer());
            final int indexCount = data.drawState().indexCount();
            if (!layer.translucent()) {
                return new LayerMesh(vertices, indexCount, null, data.drawState().indexType(), null, camera);
            }
            final MeshData.SortState sort = data.sortQuads(this.indexBuilder, RemoteViewMeshes.sorting(section,
                    camera));
            final ByteBuffer sorted = data.indexBuffer();
            final GpuBuffer indices = sorted == null ? null : device.createBuffer(
                    () -> "SGCraft remote view translucent indices", GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST,
                    sorted);
            return new LayerMesh(vertices, indexCount, indices, data.drawState().indexType(), sort, camera);
        }
    }

    /** Sorts translucent layers again where the camera has moved far enough, a few each frame. */
    void resort(final Vec3 camera) {
        int resorted = 0;
        for (final SectionMesh section : this.meshes.values()) {
            final LayerMesh layer = section.layers.get(ChunkSectionLayer.TRANSLUCENT);
            if (layer == null || layer.sort == null || layer.indices == null
                    || layer.sortedFrom.distanceToSqr(camera) < RemoteViewMeshes.RESORT_DISTANCE) {
                continue;
            }
            final ByteBuffer buffer = MemoryUtil.memAlloc(layer.indexCount * layer.indexType.bytes);
            try {
                layer.sort.writeSortedIndexBuffer(buffer, RemoteViewMeshes.sorting(section, camera));
                buffer.flip();
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(layer.indices.slice(), buffer);
            } finally {
                MemoryUtil.memFree(buffer);
            }
            layer.sortedFrom = camera;
            if (++resorted >= RemoteViewMeshes.MAX_RESORTS) {
                return;
            }
        }
    }

    // Quads are stored relative to their section, so they are sorted from the camera's place relative to it
    private static VertexSorting sorting(final SectionMesh section, final Vec3 camera) {
        return VertexSorting.byDistance((float) (camera.x - section.x), (float) (camera.y - section.y),
                (float) (camera.z - section.z));
    }

    static double distanceSq(final long pos, final Vec3 point) {
        final double dx = SectionPos.sectionToBlockCoord(SectionPos.x(pos)) + 8 - point.x;
        final double dy = SectionPos.sectionToBlockCoord(SectionPos.y(pos)) + 8 - point.y;
        final double dz = SectionPos.sectionToBlockCoord(SectionPos.z(pos)) + 8 - point.z;
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public void close() {
        this.meshes.values().forEach(SectionMesh::close);
        this.meshes.clear();
        this.dirty.clear();
        this.builders.values().forEach(ByteBufferBuilder::close);
        this.indexBuilder.close();
    }

    /** Drops every mesh but keeps what is needed to build them again, as when the view is not being drawn. */
    void release() {
        for (final SectionMesh mesh : this.meshes.values()) {
            this.dirty.put(mesh.pos, 0L);
            mesh.close();
        }
        this.meshes.clear();
    }
}
