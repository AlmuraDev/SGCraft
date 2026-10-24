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
import com.almuradev.sgcraft.mod.view.RemoteViewPackets;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.renderer.DynamicGpuData;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * A texture a remote view is drawn into, with everything drawing it needs. The view is drawn as the game draws its
 * world: the sky, then the solid and cut out blocks, the entities and block entities, then the translucent blocks and
 * the rain or snow falling, from the source's camera with a perspective of its own, lit by a lightmap made from the view's own atmosphere, with
 * fog that hides where the streamed world ends. While it draws, it stands in its own projection, fog, global settings
 * and lightmap for the game's, and puts the game's back when it is done. Only the render thread uses this.
 */
final class RemoteViewTarget implements AutoCloseable {

    private static final float FOV = 70.0F;
    private static final float NEAR = 0.05F;
    private static final float FAR = 1024.0F;
    // How far inside the view's reach the fog starts, and how far inside it is thickest
    private static final float FOG_START_INSIDE = 10.0F;
    private static final float FOG_END_INSIDE = 2.0F;
    private static final ChunkSectionLayer[] OPAQUE = {ChunkSectionLayer.SOLID, ChunkSectionLayer.CUTOUT};
    private static final ChunkSectionLayer[] TRANSLUCENT = {ChunkSectionLayer.TRANSLUCENT};

    private final String label;
    private @Nullable GpuTexture color;
    private @Nullable GpuTextureView colorView;
    private @Nullable GpuTexture depth;
    private @Nullable GpuTextureView depthView;
    private @Nullable RemoteViewSky sky;
    private @Nullable RemoteViewWeather weather;
    private @Nullable GpuSampler terrainSampler;
    private @Nullable GlobalSettingsUniform globals;
    private @Nullable GpuBuffer fog;
    private @Nullable GpuBuffer sectionBuffer;
    private @Nullable Lightmap lightmap;
    private final LightmapRenderState lightmapState = new LightmapRenderState();
    private final Projection projection = new Projection();
    private final ProjectionMatrixBuffer projectionBuffer;
    private final SubmitNodeStorage submits = new SubmitNodeStorage();
    private boolean failed;

    /** @param label what the target's GPU objects are called, for debugging tools */
    RemoteViewTarget(final String label) {
        this.label = label;
        this.projectionBuffer = new ProjectionMatrixBuffer(label);
    }

    /** @return the texture last drawn into, or null before the first draw */
    @Nullable GpuTexture color() {
        return this.color;
    }

    /** @return a view of the texture last drawn into, or null before the first draw */
    @Nullable GpuTextureView colorView() {
        return this.colorView;
    }

    /**
     * Draws a scene's latest frame into the texture, made the given size first if it isn't. A view that fails to
     * draw is logged once and leaves the texture cleared to the fog's color.
     *
     * @param partialTick how far the client is through its tick, for animations
     * @return whether there was a frame to draw
     */
    boolean draw(final RemoteViewScene scene, final int width, final int height,
            final FeatureRenderDispatcher features, final float partialTick) {
        final RemoteViewPackets.Frame frame = scene.latest();
        if (frame == null || width <= 0 || height <= 0) {
            return false;
        }
        this.ensureTargets(width, height);
        final long now = System.nanoTime();
        final Vec3 camera = scene.cameraPosition(now);
        try {
            scene.prepare(camera);
            this.render(scene, frame, camera, now, width, height, features, partialTick);
        } catch (final RuntimeException e) {
            if (!this.failed) {
                SGCraft.LOG.error("Couldn't draw a remote view", e);
                this.failed = true;
            }
        }
        return true;
    }

    private void ensureTargets(final int width, final int height) {
        if (this.color != null && (this.color.getWidth(0) != width || this.color.getHeight(0) != height)) {
            this.closeTargets();
        }
        final GpuDevice device = RenderSystem.getDevice();
        if (this.color == null) {
            this.color = device.createTexture(() -> this.label, GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC
                    | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT, GpuFormat.RGBA8_UNORM,
                    width, height, 1, 1);
            this.colorView = device.createTextureView(this.color);
            final GpuTexture mainDepth = Minecraft.getInstance().gameRenderer.mainRenderTarget().getDepthTexture();
            final GpuFormat depthFormat = mainDepth != null ? mainDepth.getFormat() : GpuFormat.D32_FLOAT;
            this.depth = device.createTexture(() -> this.label + " depth", GpuTexture.USAGE_COPY_DST
                    | GpuTexture.USAGE_RENDER_ATTACHMENT, depthFormat, width, height, 1, 1);
            this.depthView = device.createTextureView(this.depth);
        }
        if (this.sky == null) {
            this.sky = new RemoteViewSky();
            this.weather = new RemoteViewWeather();
            this.terrainSampler = device.createSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
                    FilterMode.LINEAR, FilterMode.LINEAR, 1, OptionalDouble.empty());
            this.globals = new GlobalSettingsUniform();
            this.fog = device.createBuffer(() -> this.label + " fog",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, FogRenderer.FOG_UBO_SIZE);
            this.lightmap = new Lightmap();
        }
    }

    private void render(final RemoteViewScene scene, final RemoteViewPackets.Frame frame, final Vec3 camera,
            final long now, final int width, final int height, final FeatureRenderDispatcher features,
            final float partialTick) {
        final RemoteViewPackets.Atmosphere atmosphere = frame.atmosphere();
        final float yaw = scene.cameraYaw(now);
        final float pitch = scene.cameraPitch(now);
        // The camera turned as the game turns its own, and the world turned the other way to face it
        final Quaternionf rotation = new Quaternionf().rotationYXZ(Mth.PI - yaw * Mth.DEG_TO_RAD,
                -pitch * Mth.DEG_TO_RAD, 0.0F);
        final Matrix4f view = new Matrix4f().rotation(new Quaternionf(rotation).conjugate());
        final Vector4f fogColor = RemoteViewTarget.fogColor(scene, atmosphere, rotation);
        final CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.clearColorAndDepthTextures(this.color, fogColor, this.depth, 0.0);
        this.updateLightmap(atmosphere);
        this.updateFog(scene, atmosphere, fogColor);
        this.projection.setupPerspective(RemoteViewTarget.NEAR, RemoteViewTarget.FAR, RemoteViewTarget.FOV, width,
                height);
        final GpuBufferSlice projectionSlice = this.projectionBuffer.getBuffer(this.projection);
        final Minecraft minecraft = Minecraft.getInstance();
        final GpuBufferSlice savedFog = RenderSystem.getShaderFog();
        final GpuBuffer savedGlobals = RenderSystem.getGlobalSettingsUniform();
        final GpuBufferSlice savedLights = RenderSystem.getShaderLights();
        final Matrix4fStack modelView = RenderSystem.getModelViewStack();
        RenderSystem.backupProjectionMatrix();
        modelView.pushMatrix();
        RemoteViewRenderer.setLightmapOverride(this.lightmap.getTextureView());
        try {
            RenderSystem.setProjectionMatrix(projectionSlice, ProjectionType.PERSPECTIVE);
            RenderSystem.setShaderFog(this.fog.slice(0, FogRenderer.FOG_UBO_SIZE));
            this.globals.update(width, height, minecraft.options.glintStrength().get(), atmosphere.gameTime(),
                    partialTick, 0, camera, minecraft.options.textureFiltering().get() == TextureFilteringMethod.RGSS);
            minecraft.gameRenderer.lighting().setupFor(Lighting.Entry.LEVEL);
            modelView.set(view);
            this.submitFeatures(scene, camera, rotation, view, pitch, yaw, partialTick, now);
            // Only the sections the view's own camera can see are drawn, as the game culls its own
            final Frustum frustum = new Frustum(view, this.projection.getMatrix(new Matrix4f()));
            frustum.prepare(camera.x, camera.y, camera.z);
            final List<RemoteViewMeshes.SectionMesh> sections = new ArrayList<>();
            for (final RemoteViewMeshes.SectionMesh mesh : scene.meshes.sections()) {
                if (frustum.isVisible(new AABB(mesh.x, mesh.y, mesh.z, mesh.x + 16, mesh.y + 16, mesh.z + 16))) {
                    sections.add(mesh);
                }
            }
            sections.sort(Comparator.comparingDouble(mesh -> RemoteViewMeshes.distanceSq(mesh.pos, camera)));
            // The weather is written before the pass, as the game writes its own
            this.weather.prepare(scene.level, atmosphere, camera, partialTick);
            // The shared quad indices are made large enough before the pass, as they can't grow during one
            final RenderSystem.AutoStorageIndexBuffer quads = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
            quads.getBuffer(Math.max(Math.max(this.sky.indexCount(), this.weather.indexCount()),
                    RemoteViewTarget.largestIndexCount(sections)));
            final GpuBufferSlice[] sectionUniforms = this.writeSections(sections);
            try (FeatureRenderDispatcher.PreparedFrame prepared = features.prepareFrame(this.submits);
                    RenderPass pass = encoder.createRenderPass(() -> this.label, this.colorView,
                            Optional.empty(), this.depthView, OptionalDouble.empty())) {
                RenderSystem.bindDefaultUniforms(pass);
                if (scene.world.skybox() == DimensionType.Skybox.OVERWORLD.ordinal()) {
                    this.sky.draw(pass, view, atmosphere);
                }
                this.drawTerrain(pass, sections, sectionUniforms, view, RemoteViewTarget.OPAQUE, false);
                prepared.executeSolid(pass);
                prepared.executeTranslucent(pass);
                RenderSystem.bindDefaultUniforms(pass);
                this.drawTerrain(pass, sections, sectionUniforms, view, RemoteViewTarget.TRANSLUCENT, true);
                prepared.executeTranslucentAfterTerrain(pass);
                this.weather.draw(pass, view, this.lightmap.getTextureView());
            }
        } finally {
            RemoteViewRenderer.setLightmapOverride(null);
            modelView.popMatrix();
            RenderSystem.restoreProjectionMatrix();
            if (savedFog != null) {
                RenderSystem.setShaderFog(savedFog);
            }
            if (savedGlobals != null) {
                RenderSystem.setGlobalSettingsUniform(savedGlobals);
            }
            if (savedLights != null) {
                RenderSystem.setShaderLights(savedLights);
            }
        }
    }

    // Entities and block entities, posed relative to the camera, for the game's feature renderers to draw
    private void submitFeatures(final RemoteViewScene scene, final Vec3 camera, final Quaternionf rotation,
            final Matrix4f view, final float pitch, final float yaw, final float partialTick, final long now) {
        final CameraRenderState cameraState = new CameraRenderState();
        cameraState.pos = camera;
        cameraState.blockPos = BlockPos.containing(camera);
        cameraState.xRot = pitch;
        cameraState.yRot = yaw;
        cameraState.orientation.set(rotation);
        cameraState.viewRotationMatrix.set(view);
        final PoseStack poseStack = new PoseStack();
        scene.blockEntities.submit(scene.level, camera, cameraState, poseStack, this.submits, partialTick);
        scene.entities.submit(scene.level, camera, cameraState, poseStack, this.submits, partialTick, now);
    }

    // Where each section is, for the terrain shader, in a buffer of the view's own; the game's own per frame storage
    // for these is shared with its world, which may keep them in another kind of buffer
    private GpuBufferSlice[] writeSections(final List<RemoteViewMeshes.SectionMesh> sections) {
        final GpuBufferSlice[] slices = new GpuBufferSlice[sections.size()];
        if (sections.isEmpty()) {
            return slices;
        }
        final GpuDevice device = RenderSystem.getDevice();
        final int stride = Mth.roundToward(DynamicGpuData.CHUNK_SECTION_UBO_SIZE,
                device.getDeviceInfo().limits().minUniformOffsetAlignment());
        final int size = stride * sections.size();
        if (this.sectionBuffer == null || this.sectionBuffer.size() < size) {
            if (this.sectionBuffer != null) {
                this.sectionBuffer.close();
            }
            this.sectionBuffer = device.createBuffer(() -> this.label + " sections",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, Math.max(size, stride * 128));
        }
        final ByteBuffer data = MemoryUtil.memCalloc(size);
        try {
            for (int i = 0; i < sections.size(); i++) {
                final RemoteViewMeshes.SectionMesh mesh = sections.get(i);
                data.position(i * stride);
                Std140Builder.intoBuffer(data).putIVec3(mesh.x, mesh.y, mesh.z).putFloat(1.0F);
                slices[i] = this.sectionBuffer.slice((long) i * stride, DynamicGpuData.CHUNK_SECTION_UBO_SIZE);
            }
            data.clear();
            device.createCommandEncoder().writeToBuffer(this.sectionBuffer.slice(0, size), data);
        } finally {
            MemoryUtil.memFree(data);
        }
        return slices;
    }

    private void drawTerrain(final RenderPass pass, final List<RemoteViewMeshes.SectionMesh> sections,
            final GpuBufferSlice[] sectionUniforms, final Matrix4f view, final ChunkSectionLayer[] layers,
            final boolean backToFront) {
        if (sections.isEmpty()) {
            return;
        }
        final DynamicGpuData uniforms = RenderSystem.getDynamicUniforms();
        final GpuTextureView atlas = Minecraft.getInstance().getTextureManager()
                .getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
        pass.setUniform("TerrainUniform", uniforms.writeTerrainTransform(view, atlas.getWidth(0), atlas.getHeight(0)));
        pass.setUniform("Sampler0", atlas, this.terrainSampler);
        pass.setUniform("Sampler2", this.lightmap.getTextureView(),
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        final RenderSystem.AutoStorageIndexBuffer quads = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        for (final ChunkSectionLayer layer : layers) {
            pass.setPipeline(RenderSystem.getCompiledPipeline(layer.pipeline(false)));
            for (int n = 0; n < sections.size(); n++) {
                final int i = backToFront ? sections.size() - 1 - n : n;
                final RemoteViewMeshes.LayerMesh mesh = sections.get(i).layers.get(layer);
                if (mesh == null) {
                    continue;
                }
                pass.setUniform("ChunkSection", sectionUniforms[i]);
                pass.setVertexBuffer(0, mesh.vertices.slice());
                if (mesh.indices != null) {
                    pass.setIndexBuffer(mesh.indices, mesh.indexType);
                } else {
                    pass.setIndexBuffer(quads.getBuffer(), quads.type());
                }
                pass.drawIndexed(mesh.indexCount, 1, 0, 0, 0);
            }
        }
    }

    private static int largestIndexCount(final List<RemoteViewMeshes.SectionMesh> sections) {
        int largest = 0;
        for (final RemoteViewMeshes.SectionMesh section : sections) {
            for (final RemoteViewMeshes.LayerMesh layer : section.layers.values()) {
                if (layer.indices == null) {
                    largest = Math.max(largest, layer.indexCount);
                }
            }
        }
        return largest;
    }

    // The fog's color, as the game works it out for its own sky: the world's fog color drawn towards the sky's,
    // dimmed by rain and thunder, and towards the sunrise's when looking towards the sun
    private static Vector4f fogColor(final RemoteViewScene scene, final RemoteViewPackets.Atmosphere atmosphere,
            final Quaternionf rotation) {
        final Vector3f fog = RemoteViewTarget.rgb(atmosphere.fogColor());
        if (scene.world.skybox() != DimensionType.Skybox.OVERWORLD.ordinal()) {
            return new Vector4f(fog, 1.0F);
        }
        final float sunAngle = atmosphere.sunAngle() * Mth.DEG_TO_RAD;
        final Vector3f forward = rotation.transform(new Vector3f(0.0F, 0.0F, -1.0F));
        final float towardsSun = forward.dot(Mth.sin(sunAngle) > 0.0F ? -1.0F : 1.0F, 0.0F, 0.0F);
        final float glow = (atmosphere.sunriseColor() >>> 24) / 255.0F;
        if (towardsSun > 0.0F && glow > 0.0F) {
            fog.lerp(RemoteViewTarget.rgb(atmosphere.sunriseColor()), towardsSun * glow);
        }
        final Vector3f sky = RemoteViewTarget.rgb(atmosphere.skyColor());
        if (atmosphere.rain() > 0.0F) {
            sky.mul(1.0F - atmosphere.rain() * 0.5F, 1.0F - atmosphere.rain() * 0.5F, 1.0F - atmosphere.rain() * 0.4F);
        }
        if (atmosphere.thunder() > 0.0F) {
            sky.mul(1.0F - atmosphere.thunder() * 0.5F);
        }
        final float reach = Math.min(atmosphere.skyFogEnd(), scene.world.horizontalRadius()) / 16.0F;
        final float mix = 1.0F - (float) Math.pow(Mth.clampedLerp(reach / 32.0F, 0.25F, 1.0F), 0.25);
        fog.lerp(sky, mix);
        return new Vector4f(fog, 1.0F);
    }

    private void updateFog(final RemoteViewScene scene, final RemoteViewPackets.Atmosphere atmosphere,
            final Vector4f color) {
        final float reach = scene.world.horizontalRadius();
        final float skyEnd = Math.min(atmosphere.skyFogEnd(), Math.max(reach * 4.0F, 128.0F));
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final ByteBuffer data = Std140Builder.onStack(stack, FogRenderer.FOG_UBO_SIZE)
                    .putVec4(color)
                    .putFloat(atmosphere.fogStart())
                    .putFloat(atmosphere.fogEnd())
                    .putFloat(reach - RemoteViewTarget.FOG_START_INSIDE)
                    .putFloat(reach - RemoteViewTarget.FOG_END_INSIDE)
                    .putFloat(skyEnd)
                    .putFloat(skyEnd)
                    .get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.fog.slice(), data);
        }
    }

    // The view's light from its own world's sky and time of day, with the player's own brightness setting
    private void updateLightmap(final RemoteViewPackets.Atmosphere atmosphere) {
        final LightmapRenderState state = this.lightmapState;
        state.needsUpdate = true;
        state.blockFactor = 1.4F;
        state.blockLightTint = RemoteViewTarget.rgb(atmosphere.blockLightTint());
        state.skyFactor = atmosphere.skyLightFactor();
        state.skyLightColor = RemoteViewTarget.rgb(atmosphere.skyLightColor());
        state.ambientColor = RemoteViewTarget.rgb(atmosphere.ambientColor());
        state.brightness = Minecraft.getInstance().options.gamma().get().floatValue();
        state.darknessEffectScale = 0.0F;
        state.nightVisionEffectIntensity = 0.0F;
        state.bossOverlayWorldDarkening = 0.0F;
        this.lightmap.render(state);
    }

    private static Vector3f rgb(final int color) {
        return new Vector3f((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F);
    }

    private void closeTargets() {
        if (this.colorView != null) {
            this.colorView.close();
        }
        if (this.color != null) {
            this.color.close();
        }
        if (this.depthView != null) {
            this.depthView.close();
        }
        if (this.depth != null) {
            this.depth.close();
        }
        this.color = null;
        this.colorView = null;
        this.depth = null;
        this.depthView = null;
    }

    @Override
    public void close() {
        this.closeTargets();
        if (this.sky != null) {
            this.sky.close();
            this.sky = null;
        }
        if (this.weather != null) {
            this.weather.close();
            this.weather = null;
        }
        if (this.terrainSampler != null) {
            this.terrainSampler.close();
            this.terrainSampler = null;
        }
        if (this.globals != null) {
            this.globals.close();
            this.globals = null;
        }
        if (this.fog != null) {
            this.fog.close();
            this.fog = null;
        }
        if (this.sectionBuffer != null) {
            this.sectionBuffer.close();
            this.sectionBuffer = null;
        }
        if (this.lightmap != null) {
            this.lightmap.close();
            this.lightmap = null;
        }
        this.projectionBuffer.close();
    }
}
