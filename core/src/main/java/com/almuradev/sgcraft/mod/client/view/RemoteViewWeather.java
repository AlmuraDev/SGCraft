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

import com.almuradev.sgcraft.mod.view.RemoteViewPackets;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The rain and snow falling round a remote view's camera, drawn as the game draws its own: a strip turned to the
 * camera in each block column near it, from where rain stops up past the camera, rain or snow by the column's biome,
 * as strong as the source's world rains. Where the server hasn't said where rain stops, none falls. Only the render
 * thread uses this.
 */
final class RemoteViewWeather implements AutoCloseable {

    private static final Identifier RAIN = Identifier.withDefaultNamespace("textures/environment/rain.png");
    private static final Identifier SNOW = Identifier.withDefaultNamespace("textures/environment/snow.png");
    // How far round the camera it rains, in blocks, at most: the columns' strips are turned from a 32 by 32 table
    private static final int MOST_RADIUS = 15;
    private static final int TABLE = 32;

    // Which way each column's strip lies, turned to face the camera, by its place in the table round it
    private final float[] sizeX = new float[RemoteViewWeather.TABLE * RemoteViewWeather.TABLE];
    private final float[] sizeZ = new float[RemoteViewWeather.TABLE * RemoteViewWeather.TABLE];
    private final List<Column> rain = new ArrayList<>();
    private final List<Column> snow = new ArrayList<>();
    private @Nullable GpuBuffer vertices;
    private int indexCount;
    private int radius;
    private float intensity;

    // A column's strip: where it is, from where to where it falls, where its texture starts and how lit it is
    private record Column(int x, int z, int bottom, int top, float u, float v, int light) {
    }

    RemoteViewWeather() {
        final int half = RemoteViewWeather.TABLE / 2;
        for (int z = 0; z < RemoteViewWeather.TABLE; z++) {
            for (int x = 0; x < RemoteViewWeather.TABLE; x++) {
                final float dx = x - half;
                final float dz = z - half;
                final float distance = Mth.length(dx, dz);
                this.sizeX[z * RemoteViewWeather.TABLE + x] = distance == 0 ? 1 : -dz / distance;
                this.sizeZ[z * RemoteViewWeather.TABLE + x] = distance == 0 ? 0 : dx / distance;
            }
        }
    }

    /** @return how many quad indices drawing the weather takes, once it is prepared */
    int indexCount() {
        return this.indexCount;
    }

    /**
     * Works out the columns it falls in round the camera and writes their strips, before the view's pass. With no
     * rain, nothing is written.
     *
     * @param partialTick how far the client is through its tick, for the falling
     */
    void prepare(final RemoteViewLevel level, final RemoteViewPackets.Atmosphere atmosphere, final Vec3 camera,
            final float partialTick) {
        this.rain.clear();
        this.snow.clear();
        this.indexCount = 0;
        this.intensity = atmosphere.rain();
        if (this.intensity <= 0.0F) {
            return;
        }
        this.radius = Math.min(RemoteViewWeather.MOST_RADIUS, Minecraft.getInstance().options.weatherRadius().get());
        final int cameraX = Mth.floor(camera.x);
        final int cameraY = Mth.floor(camera.y);
        final int cameraZ = Mth.floor(camera.z);
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final RandomSource random = RandomSource.createThreadLocalInstance();
        for (int z = cameraZ - this.radius; z <= cameraZ + this.radius; z++) {
            for (int x = cameraX - this.radius; x <= cameraX + this.radius; x++) {
                final int ground = level.rainHeight(x, z);
                if (ground == Integer.MIN_VALUE) {
                    continue;
                }
                final int bottom = Math.max(cameraY - this.radius, ground);
                final int top = Math.max(cameraY + this.radius, ground);
                if (top == bottom) {
                    continue;
                }
                final Holder<Biome> biome = level.biome(pos.set(x, cameraY, z));
                if (biome == null) {
                    continue;
                }
                final Biome.Precipitation precipitation = biome.value().getPrecipitationAt(pos, level.seaLevel());
                if (precipitation == Biome.Precipitation.NONE) {
                    continue;
                }
                random.setSeed(x * x * 3121 + x * 45238971 ^ z * z * 418711 + z * 13761);
                pos.set(x, Math.max(cameraY, ground), z);
                final int light = LightCoordsUtil.pack(level.getBrightness(LightLayer.BLOCK, pos),
                        level.getBrightness(LightLayer.SKY, pos));
                if (precipitation == Biome.Precipitation.RAIN) {
                    this.rain.add(RemoteViewWeather.rainColumn(random, atmosphere.gameTime(), x, bottom, top, z,
                            light, partialTick));
                } else {
                    this.snow.add(RemoteViewWeather.snowColumn(random, atmosphere.gameTime(), x, bottom, top, z,
                            light, partialTick));
                }
            }
        }
        this.upload(camera);
    }

    // Rain falls fast, each column at its own speed
    private static Column rainColumn(final RandomSource random, final long ticks, final int x, final int bottom,
            final int top, final int z, final int light, final float partialTick) {
        final int wrapped = (int) (ticks & 131071L);
        final int offset = x * x * 3121 + x * 45238971 + z * z * 418711 + z * 13761 & 0xFF;
        final float speed = 3.0F + random.nextFloat();
        final float v = -(wrapped + offset + partialTick) / 32.0F * speed % 32.0F;
        return new Column(x, z, bottom, top, 0.0F, v, light);
    }

    // Snow drifts slowly and wanders, a little brighter than where it falls
    private static Column snowColumn(final RandomSource random, final long ticks, final int x, final int bottom,
            final int top, final int z, final int light, final float partialTick) {
        final float time = (int) (ticks & 131071L) + partialTick;
        final float u = (float) (random.nextDouble() + time * 0.01F * (float) random.nextGaussian());
        final float v = (float) (random.nextDouble() + time * (float) random.nextGaussian() * 0.001F);
        final float fall = -((float) (ticks & 511L) + partialTick) / 512.0F;
        final int bright = LightCoordsUtil.pack((LightCoordsUtil.block(light) * 3 + 15) / 4,
                (LightCoordsUtil.sky(light) * 3 + 15) / 4);
        return new Column(x, z, bottom, top, u, fall + v, bright);
    }

    private void upload(final Vec3 camera) {
        final int columns = this.rain.size() + this.snow.size();
        if (columns == 0) {
            return;
        }
        try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(columns * 4
                * DefaultVertexFormat.PARTICLE.getVertexSize())) {
            final BufferBuilder builder = new BufferBuilder(bytes, PrimitiveTopology.QUADS,
                    DefaultVertexFormat.PARTICLE);
            this.strips(builder, this.rain, camera, 1.0F);
            this.strips(builder, this.snow, camera, 0.8F);
            try (MeshData mesh = builder.buildOrThrow()) {
                final int size = mesh.vertexBuffer().remaining();
                if (this.vertices == null || this.vertices.size() < size) {
                    if (this.vertices != null) {
                        this.vertices.close();
                    }
                    this.vertices = RenderSystem.getDevice().createBuffer(() -> "SGCraft remote view weather",
                            GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, size);
                }
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.vertices.slice(),
                        mesh.vertexBuffer());
                this.indexCount = mesh.drawState().indexCount();
            }
        }
    }

    // Each column's strip, relative to the camera, fading out with distance
    private void strips(final BufferBuilder builder, final List<Column> columns, final Vec3 camera,
            final float alpha) {
        final float radiusSq = this.radius * this.radius;
        final int half = RemoteViewWeather.TABLE / 2;
        for (final Column column : columns) {
            final float relativeX = (float) (column.x + 0.5 - camera.x);
            final float relativeZ = (float) (column.z + 0.5 - camera.z);
            final float distanceSq = Mth.lengthSquared(relativeX, relativeZ);
            final int color = ARGB.white(Mth.lerp(Math.min(distanceSq / radiusSq, 1.0F), alpha, 0.5F)
                    * this.intensity);
            final int index = (column.z - Mth.floor(camera.z) + half) * RemoteViewWeather.TABLE + column.x
                    - Mth.floor(camera.x) + half;
            final float halfX = this.sizeX[index] / 2.0F;
            final float halfZ = this.sizeZ[index] / 2.0F;
            final float y1 = (float) (column.top - camera.y);
            final float y0 = (float) (column.bottom - camera.y);
            final float v0 = column.bottom * 0.25F + column.v;
            final float v1 = column.top * 0.25F + column.v;
            builder.addVertex(relativeX - halfX, y1, relativeZ - halfZ).setUv(column.u, v0).setColor(color)
                    .setLight(column.light);
            builder.addVertex(relativeX + halfX, y1, relativeZ + halfZ).setUv(column.u + 1.0F, v0).setColor(color)
                    .setLight(column.light);
            builder.addVertex(relativeX + halfX, y0, relativeZ + halfZ).setUv(column.u + 1.0F, v1).setColor(color)
                    .setLight(column.light);
            builder.addVertex(relativeX - halfX, y0, relativeZ - halfZ).setUv(column.u, v1).setColor(color)
                    .setLight(column.light);
        }
    }

    /** Draws what was prepared in the view's pass, the rain then the snow, lit by the view's lightmap. */
    void draw(final RenderPass pass, final Matrix4f view, final GpuTextureView lightmap) {
        if (this.vertices == null || this.indexCount == 0) {
            return;
        }
        final RenderSystem.AutoStorageIndexBuffer quads = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        pass.setPipeline(RenderSystem.getCompiledPipeline(RenderPipelines.WEATHER));
        RenderSystem.bindDefaultUniforms(pass);
        pass.setUniform("DynamicTransforms", RenderSystem.getDynamicUniforms().writeTransform(view));
        pass.setUniform("Sampler2", lightmap, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        pass.setIndexBuffer(quads.getBuffer(), quads.type());
        pass.setVertexBuffer(0, this.vertices.slice());
        final AbstractTexture rainTexture = Minecraft.getInstance().getTextureManager()
                .getTexture(RemoteViewWeather.RAIN);
        final AbstractTexture snowTexture = Minecraft.getInstance().getTextureManager()
                .getTexture(RemoteViewWeather.SNOW);
        if (!this.rain.isEmpty()) {
            pass.setUniform("Sampler0", rainTexture.getTextureView(), rainTexture.getSampler());
            pass.drawIndexed(this.rain.size() * 6, 1, 0, 0, 0);
        }
        if (!this.snow.isEmpty()) {
            pass.setUniform("Sampler0", snowTexture.getTextureView(), snowTexture.getSampler());
            pass.drawIndexed(this.snow.size() * 6, 1, this.rain.size() * 6, 0, 0);
        }
    }

    @Override
    public void close() {
        if (this.vertices != null) {
            this.vertices.close();
            this.vertices = null;
        }
    }
}
