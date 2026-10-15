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
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.MoonPhase;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * The sky of a remote view: the sky's dome in the world's sky color, fading to the fog color at the horizon, the glow
 * of sunrise and sunset, the sun, the moon and the stars, laid out as the game lays out its own sky, from the
 * atmosphere the server streams.
 */
final class RemoteViewSky implements AutoCloseable {

    private static final Identifier SUN = Identifier.withDefaultNamespace("sun");
    private static final int DISC_VERTICES = 10;
    private static final int SUNRISE_VERTICES = 18;
    private static final int STARS = 1500;

    private final TextureAtlas celestials;
    private final GpuBuffer disc;
    private final GpuBuffer sunrise;
    private final GpuBuffer sun;
    private final GpuBuffer moon;
    private final GpuBuffer stars;
    private final int starIndices;

    RemoteViewSky() {
        this.celestials = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.CELESTIALS);
        this.disc = RemoteViewSky.buildDisc();
        this.sunrise = RemoteViewSky.buildSunrise();
        this.sun = RemoteViewSky.buildQuads("sun", new TextureAtlasSprite[] {
                this.celestials.getSprite(RemoteViewSky.SUN)}, false);
        final MoonPhase[] phases = MoonPhase.values();
        final TextureAtlasSprite[] moons = new TextureAtlasSprite[phases.length];
        for (int i = 0; i < phases.length; i++) {
            moons[i] = this.celestials.getSprite(Identifier.withDefaultNamespace("moon/"
                    + phases[i].getSerializedName()));
        }
        this.moon = RemoteViewSky.buildQuads("moon", moons, true);
        final int[] indices = new int[1];
        this.stars = RemoteViewSky.buildStars(indices);
        this.starIndices = indices[0];
    }

    /** @return how many shared quad indices drawing the sky takes */
    int indexCount() {
        return Math.max(6, this.starIndices);
    }

    /**
     * Draws the sky. The pass must have the view's projection and fog bound, and shared quad indices for at least
     * {@link #indexCount()}.
     *
     * @param view the camera's rotation, which the sky turns with but never moves with
     */
    void draw(final RenderPass pass, final Matrix4fc view, final RemoteViewPackets.Atmosphere atmosphere) {
        final float rainBrightness = 1.0F - atmosphere.rain();
        final float sunAngle = atmosphere.sunAngle() * Mth.DEG_TO_RAD;
        // The dome, whose fog gives the gradient down to the horizon
        RemoteViewSky.drawPlain(pass, RenderPipelines.SKY, new Matrix4f(view),
                RemoteViewSky.color(atmosphere.skyColor(), 1.0F), this.disc, RemoteViewSky.DISC_VERTICES);
        // Sunrise and sunset, on the side the sun is
        final float glow = (atmosphere.sunriseColor() >>> 24) / 255.0F;
        if (glow > 0.001F) {
            final Matrix4f pose = new Matrix4f(view).rotateX(Mth.HALF_PI)
                    .rotateZ((Mth.sin(sunAngle) < 0.0F ? Mth.PI : 0.0F) + Mth.HALF_PI).scale(1.0F, 1.0F, glow);
            RemoteViewSky.drawPlain(pass, RenderPipelines.SUNRISE_SUNSET, pose,
                    RemoteViewSky.color(atmosphere.sunriseColor(), glow), this.sunrise,
                    RemoteViewSky.SUNRISE_VERTICES);
        }
        final Matrix4f sky = new Matrix4f(view).rotateY(-Mth.HALF_PI);
        this.drawBody(pass, new Matrix4f(sky).rotateX(sunAngle).translate(0.0F, 100.0F, 0.0F)
                .scale(30.0F, 1.0F, 30.0F), rainBrightness, this.sun, 0);
        this.drawBody(pass, new Matrix4f(sky).rotateX(atmosphere.moonAngle() * Mth.DEG_TO_RAD)
                .translate(0.0F, 100.0F, 0.0F).scale(20.0F, 1.0F, 20.0F), rainBrightness, this.moon,
                atmosphere.moonPhase() * 4);
        final float stars = atmosphere.starBrightness() * rainBrightness;
        if (stars > 0.0F) {
            final RenderSystem.AutoStorageIndexBuffer quads = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
            pass.setPipeline(RenderSystem.getCompiledPipeline(RenderPipelines.STARS));
            pass.setUniform("DynamicTransforms", RenderSystem.getDynamicUniforms().writeTransform(
                    new Matrix4f(sky).rotateX(atmosphere.starAngle() * Mth.DEG_TO_RAD),
                    new Vector4f(stars, stars, stars, stars)));
            pass.setVertexBuffer(0, this.stars.slice());
            pass.setIndexBuffer(quads.getBuffer(), quads.type());
            pass.drawIndexed(this.starIndices, 1, 0, 0, 0);
        }
    }

    private void drawBody(final RenderPass pass, final Matrix4f pose, final float brightness, final GpuBuffer quad,
            final int baseVertex) {
        final RenderSystem.AutoStorageIndexBuffer quads = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        pass.setPipeline(RenderSystem.getCompiledPipeline(RenderPipelines.CELESTIAL));
        pass.setUniform("DynamicTransforms", RenderSystem.getDynamicUniforms().writeTransform(pose,
                new Vector4f(1.0F, 1.0F, 1.0F, brightness)));
        pass.setUniform("Sampler0", this.celestials.getTextureView(), this.celestials.getSampler());
        pass.setVertexBuffer(0, quad.slice());
        pass.setIndexBuffer(quads.getBuffer(), quads.type());
        pass.drawIndexed(6, 1, 0, baseVertex, 0);
    }

    private static void drawPlain(final RenderPass pass, final RenderPipeline pipeline, final Matrix4f pose,
            final Vector4f color, final GpuBuffer vertices, final int count) {
        pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
        pass.setUniform("DynamicTransforms", RenderSystem.getDynamicUniforms().writeTransform(pose, color));
        pass.setVertexBuffer(0, vertices.slice());
        pass.draw(count, 1, 0, 0);
    }

    private static Vector4f color(final int rgb, final float alpha) {
        return new Vector4f((rgb >> 16 & 255) / 255.0F, (rgb >> 8 & 255) / 255.0F, (rgb & 255) / 255.0F, alpha);
    }

    // A fan over the camera, sixteen blocks up and reaching far out, as the game's own sky has
    private static GpuBuffer buildDisc() {
        try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(RemoteViewSky.DISC_VERTICES
                * DefaultVertexFormat.POSITION.getVertexSize())) {
            final BufferBuilder builder = new BufferBuilder(bytes, PrimitiveTopology.TRIANGLE_FAN,
                    DefaultVertexFormat.POSITION);
            builder.addVertex(0.0F, 16.0F, 0.0F);
            for (int i = -180; i <= 180; i += 45) {
                builder.addVertex(512.0F * Mth.cos(i * Mth.DEG_TO_RAD), 16.0F, 512.0F * Mth.sin(i * Mth.DEG_TO_RAD));
            }
            try (MeshData mesh = builder.buildOrThrow()) {
                return RenderSystem.getDevice().createBuffer(() -> "SGCraft remote view sky",
                        GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
            }
        }
    }

    private static GpuBuffer buildSunrise() {
        try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(RemoteViewSky.SUNRISE_VERTICES
                * DefaultVertexFormat.POSITION_COLOR.getVertexSize())) {
            final BufferBuilder builder = new BufferBuilder(bytes, PrimitiveTopology.TRIANGLE_FAN,
                    DefaultVertexFormat.POSITION_COLOR);
            builder.addVertex(0.0F, 100.0F, 0.0F).setColor(-1);
            for (int i = 0; i <= 16; i++) {
                final float angle = i * Mth.TWO_PI / 16.0F;
                builder.addVertex(Mth.sin(angle) * 120.0F, Mth.cos(angle) * 120.0F, -Mth.cos(angle) * 40.0F)
                        .setColor(0x00FFFFFF);
            }
            try (MeshData mesh = builder.buildOrThrow()) {
                return RenderSystem.getDevice().createBuffer(() -> "SGCraft remote view sunrise",
                        GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
            }
        }
    }

    // One quad for each sprite, in order; the moon's faces are turned the other way, as the game draws them
    private static GpuBuffer buildQuads(final String name, final TextureAtlasSprite[] sprites,
            final boolean flip) {
        try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(sprites.length * 4
                * DefaultVertexFormat.POSITION_TEX.getVertexSize())) {
            final BufferBuilder builder = new BufferBuilder(bytes, PrimitiveTopology.QUADS,
                    DefaultVertexFormat.POSITION_TEX);
            for (final TextureAtlasSprite sprite : sprites) {
                final float u0 = flip ? sprite.getU1() : sprite.getU0();
                final float u1 = flip ? sprite.getU0() : sprite.getU1();
                final float v0 = flip ? sprite.getV1() : sprite.getV0();
                final float v1 = flip ? sprite.getV0() : sprite.getV1();
                builder.addVertex(-1.0F, 0.0F, -1.0F).setUv(u0, v0);
                builder.addVertex(1.0F, 0.0F, -1.0F).setUv(u1, v0);
                builder.addVertex(1.0F, 0.0F, 1.0F).setUv(u1, v1);
                builder.addVertex(-1.0F, 0.0F, 1.0F).setUv(u0, v1);
            }
            try (MeshData mesh = builder.buildOrThrow()) {
                return RenderSystem.getDevice().createBuffer(() -> "SGCraft remote view " + name,
                        GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
            }
        }
    }

    // The same stars as the game's own sky, from the same seed
    private static GpuBuffer buildStars(final int[] indexCount) {
        final RandomSource random = RandomSource.createThreadLocalInstance(10842L);
        try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(DefaultVertexFormat.POSITION.getVertexSize()
                * RemoteViewSky.STARS * 4)) {
            final BufferBuilder builder = new BufferBuilder(bytes, PrimitiveTopology.QUADS,
                    DefaultVertexFormat.POSITION);
            for (int i = 0; i < RemoteViewSky.STARS; i++) {
                final float x = random.nextFloat() * 2.0F - 1.0F;
                final float y = random.nextFloat() * 2.0F - 1.0F;
                final float z = random.nextFloat() * 2.0F - 1.0F;
                final float size = 0.15F + random.nextFloat() * 0.1F;
                final float lengthSq = Mth.lengthSquared(x, y, z);
                if (lengthSq <= 0.010000001F || lengthSq >= 1.0F) {
                    continue;
                }
                final Vector3f center = new Vector3f(x, y, z).normalize(100.0F);
                final float spin = (float) (random.nextDouble() * Mth.TWO_PI);
                final Matrix3f rotation = new Matrix3f().rotateTowards(new Vector3f(center).negate(),
                        new Vector3f(0.0F, 1.0F, 0.0F)).rotateZ(-spin);
                builder.addVertex(new Vector3f(size, -size, 0.0F).mul(rotation).add(center));
                builder.addVertex(new Vector3f(size, size, 0.0F).mul(rotation).add(center));
                builder.addVertex(new Vector3f(-size, size, 0.0F).mul(rotation).add(center));
                builder.addVertex(new Vector3f(-size, -size, 0.0F).mul(rotation).add(center));
            }
            try (MeshData mesh = builder.buildOrThrow()) {
                indexCount[0] = mesh.drawState().indexCount();
                return RenderSystem.getDevice().createBuffer(() -> "SGCraft remote view stars",
                        GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
            }
        }
    }

    @Override
    public void close() {
        this.disc.close();
        this.sunrise.close();
        this.sun.close();
        this.moon.close();
        this.stars.close();
    }
}
