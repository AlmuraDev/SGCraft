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

package com.almuradev.sgcraft.mod.client.gate;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.client.ClientPlatform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRenderer;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.oit.OitStage;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws gates' parts from their {@link GateMesh meshes}, which stay on the GPU, with the level's solid features.
 *
 * <p>A gate's renderer {@link #submit submits} its parts as block entities are submitted. Each part is then one
 * draw, its placement, tint and light pushed to the shader as constants, so nothing is rebuilt from frame to frame.
 * Each loader registers this renderer for {@link #TYPE} and puts its submits in the solid phase.</p>
 */
public final class GateFeatureRenderer implements FeatureRenderer<GateFeatureRenderer.Submit> {

    /** The kind of submit this renderer draws. */
    public static final FeatureRendererType<Submit> TYPE = FeatureRendererType.create("SGCraft gate");

    // A part's placement (camera relative), its tint, the light it's lit by, and whether it shines with its own: a
    // mat4, a vec4, an ivec2 and an int, rounded up to a multiple of 16 bytes
    private static final int CONSTANTS_SIZE = 96;
    private static final int TINT_OFFSET = 64;
    private static final int LIGHT_OFFSET = 80;
    private static final int EMISSIVE_OFFSET = 88;

    private static final RenderPipeline PIPELINE = RenderPipeline.builder()
            .withLocation(SGCraft.id("pipeline/gate"))
            .withVertexShader(SGCraft.id("core/gate"))
            .withFragmentShader(SGCraft.id("core/gate"))
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withBindGroupLayout(BindGroupLayouts.FOG)
            .withBindGroupLayout(BindGroupLayouts.LIGHTING)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
            .withVertexBinding(0, GateMesh.FORMAT)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withCull(false)
            .withPushConstantSize(GateFeatureRenderer.CONSTANTS_SIZE)
            .build();

    private final ByteBuffer constants = MemoryUtil.memCalloc(GateFeatureRenderer.CONSTANTS_SIZE);
    // The shader, the view's turn and the parts' textures for this frame, found while the frame is prepared: the
    // shader is compiled and a texture uploaded when first asked for, which can't happen once a pass is open
    private @Nullable CompiledRenderPipeline pipeline;
    private @Nullable GpuBufferSlice transforms;
    private final Map<Identifier, AbstractTexture> textures = new HashMap<>();
    private boolean failed;

    /**
     * Submits a part of a gate, to be drawn with the level's solid features.
     *
     * @param pose where the part goes, as block entities are placed: relative to the camera, not turned with it
     * @param color the color the part is tinted, as ARGB
     * @param light the packed light the part is lit by
     */
    public static void submit(final SubmitNodeCollector collector, final PoseStack.Pose pose, final Identifier texture,
            final GateMesh mesh, final int color, final int light) {
        if (mesh.buffer() != null) {
            ClientPlatform.get().submitSolid(collector,
                    new Submit(new Matrix4f(pose.pose()), texture, mesh, color, light, false));
        }
    }

    /**
     * Submits a part of a gate that shines with its own light, such as a lit chevron: drawn as bright as its color
     * whatever light it stands in, and without the shading of the faces turned from the level's light.
     *
     * @param pose where the part goes, as block entities are placed: relative to the camera, not turned with it
     * @param color the color the part shines, as ARGB
     */
    public static void submitEmissive(final SubmitNodeCollector collector, final PoseStack.Pose pose,
            final Identifier texture, final GateMesh mesh, final int color) {
        if (mesh.buffer() != null) {
            ClientPlatform.get().submitSolid(collector,
                    new Submit(new Matrix4f(pose.pose()), texture, mesh, color, LightCoordsUtil.FULL_BRIGHT, true));
        }
    }

    @Override
    public void beginPrepare(final FeatureFrameContext context) {
        this.pipeline = null;
        this.transforms = null;
        this.textures.clear();
    }

    @Override
    public void prepareGroup(final FeatureFrameContext context, final List<Submit> submits,
            final boolean strictlyOrdered) {
        if (this.failed) {
            return;
        }
        if (this.pipeline == null) {
            this.pipeline = RenderSystem.getCompiledPipelineNullable(GateFeatureRenderer.PIPELINE);
            if (this.pipeline == null) {
                this.failed = true;
                SGCraft.LOG.error("Unable to compile the gate shader; gates won't be drawn");
                return;
            }
        }
        // The glows drawn over gates use a shader of their own, which mustn't be compiled once their pass is open
        GateGlow.prepare();
        if (this.transforms == null) {
            this.transforms = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy());
        }
        for (final Submit submit : submits) {
            this.textures.computeIfAbsent(submit.texture, context.textureManager()::getTexture);
        }
    }

    @Override
    public void executeGroup(final FeatureFrameContext context, final @Nullable OitStage stage,
            final RenderPass renderPass, final int groupIndex, final List<Submit> submits,
            final boolean strictlyOrdered) {
        final CompiledRenderPipeline pipeline = this.pipeline;
        final GpuBufferSlice transforms = this.transforms;
        if (this.failed || pipeline == null || transforms == null || submits.isEmpty()) {
            return;
        }
        renderPass.pushDebugGroup(() -> "SGCraft gates");
        renderPass.setPipeline(pipeline);
        RenderSystem.bindDefaultUniforms(renderPass);
        renderPass.setUniform("DynamicTransforms", transforms);
        renderPass.setUniform("Sampler2", context.lightmap(),
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        Identifier bound = null;
        for (final Submit submit : submits) {
            if (!submit.texture.equals(bound)) {
                final AbstractTexture texture = this.textures.get(submit.texture);
                if (texture == null) {
                    continue;
                }
                renderPass.setUniform("Sampler0", texture.getTextureView(), texture.getSampler());
                bound = submit.texture;
            }
            this.pushConstants(renderPass, submit);
            final GpuBuffer buffer = submit.mesh.buffer();
            if (buffer != null) {
                renderPass.setVertexBuffer(0, buffer.slice());
                renderPass.draw(submit.mesh.vertices(), 1, 0, 0);
            }
        }
        renderPass.popDebugGroup();
    }

    private void pushConstants(final RenderPass renderPass, final Submit submit) {
        final int color = submit.color;
        submit.model.get(0, this.constants);
        this.constants.putFloat(GateFeatureRenderer.TINT_OFFSET, ARGB.redFloat(color))
                .putFloat(GateFeatureRenderer.TINT_OFFSET + 4, ARGB.greenFloat(color))
                .putFloat(GateFeatureRenderer.TINT_OFFSET + 8, ARGB.blueFloat(color))
                .putFloat(GateFeatureRenderer.TINT_OFFSET + 12, ARGB.alphaFloat(color))
                .putInt(GateFeatureRenderer.LIGHT_OFFSET, submit.light & 0xFFFF)
                .putInt(GateFeatureRenderer.LIGHT_OFFSET + 4, submit.light >>> 16 & 0xFFFF)
                .putInt(GateFeatureRenderer.EMISSIVE_OFFSET, submit.emissive ? 1 : 0);
        this.constants.clear();
        renderPass.pushConstants(this.constants);
    }

    @Override
    public void close() {
        MemoryUtil.memFree(this.constants);
    }

    /**
     * A part of a gate to draw: its placement relative to the camera, its texture, its mesh, tint and light, and
     * whether it shines with its own light.
     */
    public record Submit(Matrix4f model, Identifier texture, GateMesh mesh, int color, int light, boolean emissive)
            implements SubmitNode {

        @Override
        public FeatureRendererType<Submit> featureType() {
            return GateFeatureRenderer.TYPE;
        }
    }
}
