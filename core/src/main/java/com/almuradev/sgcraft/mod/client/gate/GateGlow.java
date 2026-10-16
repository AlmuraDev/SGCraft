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
import com.almuradev.sgcraft.mod.mixin.client.RenderTypeInvoker;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Colored light round a gate's lit parts and over its open horizon, which the world's own light can't give: each
 * glow is a square across the gate's face, its texture fading out from its middle, added to whatever is behind it.
 * The ring and the blocks round a gate so catch its color where a glow lies over them, while their light stays the
 * world's.
 *
 * <p>A gate's renderer adds its lit parts' glows to its state's {@link Glows} each frame, and they are drawn together;
 * nothing is made for them anew from frame to frame.</p>
 */
public final class GateGlow {

    private static final Identifier HALO = SGCraft.id("textures/gate/glow.png");
    private static final Identifier HORIZON = SGCraft.id("textures/gate/horizon_glow.png");
    private static final Identifier PLAIN = SGCraft.id("textures/gate/shield.png");

    // Drawn as vanilla's energy swirl is, added to what is behind it and as bright whatever light it is in, but
    // without hiding what is drawn behind it afterwards, such as another glow. The fragment shader is SGCraft's own,
    // which fogs a glow toward black so it fades out with distance rather than turning into a square of fog color
    private static final RenderPipeline.Snippet SNIPPET = RenderPipeline.builder()
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withBindGroupLayout(BindGroupLayouts.FOG)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withVertexShader("core/entity")
            .withFragmentShader(SGCraft.id("core/gate_glow"))
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withShaderDefine("EMISSIVE")
            .withShaderDefine("NO_OVERLAY")
            .withShaderDefine("NO_CARDINAL_LIGHTING")
            .withShaderDefine("APPLY_TEXTURE_MATRIX")
            .withCull(false)
            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .buildSnippet();
    private static final RenderPipeline PIPELINE = RenderPipeline.builder(GateGlow.SNIPPET)
            .withLocation(SGCraft.id("pipeline/gate_glow"))
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .build();
    // The same, for when the game draws translucent things in any order
    private static final OitPipelineSet OIT = OitPipelineSet.builder("sgcraft_gate_glow",
            RenderPipeline.builder(GateGlow.SNIPPET).withShaderDefine("OIT_ADDITIVE")).build();

    /** The glow round a lit part: bright in the middle, fading softly to nothing. */
    public static final RenderType HALO_TYPE = GateGlow.type("sgcraft_gate_glow", GateGlow.HALO);
    /** The glow over an open horizon: even across most of it, fading out at its edge. */
    public static final RenderType HORIZON_TYPE = GateGlow.type("sgcraft_horizon_glow", GateGlow.HORIZON);
    /** Plain light in the colors of its vertices, as a shield is drawn. */
    public static final RenderType SHIELD_TYPE = GateGlow.type("sgcraft_shield", GateGlow.PLAIN);

    // The box round each array of triangles, as the least x, y and z then the most, by the array
    private static final Map<float[], float[]> BOUNDS = new IdentityHashMap<>();

    private GateGlow() {
    }

    private static RenderType type(final String name, final Identifier texture) {
        return RenderTypeInvoker.sgcraft$create(name, RenderSetup.builder(GateGlow.PIPELINE)
                .setOitPipelines(GateGlow.OIT)
                .withTexture("Sampler0", texture)
                .setTextureTransform(new TextureTransform.OffsetTextureTransform(0, 0))
                .useLightmap()
                .useOverlay()
                .sortOnUpload()
                .createRenderSetup());
    }

    /** Compiles the glows' shader, if it isn't yet, while no pass is open. Only call this on the render thread. */
    public static void prepare() {
        RenderSystem.getCompiledPipelineNullable(GateGlow.PIPELINE);
        RenderSystem.getCompiledPipelineNullable(GateGlow.OIT.depthBoundsPipeline());
        RenderSystem.getCompiledPipelineNullable(GateGlow.OIT.transmittancePipeline());
        RenderSystem.getCompiledPipelineNullable(GateGlow.OIT.accumulatePipeline());
    }

    /**
     * The box round some triangles, laid out as {@link GateGeometry} lays them out, worked out once for each array.
     * Only call this on the render thread.
     *
     * @return the least x, y and z, then the most
     */
    public static float[] bounds(final float[] triangles) {
        return GateGlow.BOUNDS.computeIfAbsent(triangles, key -> {
            final float[] box = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                    -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
            for (int c = 0; c < triangles.length; c += 8) {
                for (int axis = 0; axis < 3; axis++) {
                    box[axis] = Math.min(box[axis], triangles[c + axis]);
                    box[3 + axis] = Math.max(box[3 + axis], triangles[c + axis]);
                }
            }
            return box;
        });
    }

    /** One gate's glows for a frame, each a square of one color round a point of the gate's face. */
    public static final class Glows implements SubmitNodeCollector.CustomGeometryRenderer {

        private static final int MAX = 64;
        // How far in front of a part its glow lies, so the part doesn't cut through it
        private static final double CLEARANCE = 0.02;

        // Each glow's x, y, z and how far it reaches from its middle, and its color
        private final float[] glows = new float[Glows.MAX * 4];
        private final int[] colors = new int[Glows.MAX];
        private int count;

        /** Forgets the last frame's glows. */
        public void clear() {
            this.count = 0;
        }

        /**
         * Adds a glow facing along the gate's axis, seen from both sides.
         *
         * @param radius how far it reaches from its middle, out to its corners' sides
         * @param color the color it adds at its brightest, as ARGB
         */
        public void add(final double x, final double y, final double z, final double radius, final int color) {
            if (this.count >= Glows.MAX) {
                return;
            }
            final int at = this.count * 4;
            this.glows[at] = (float) x;
            this.glows[at + 1] = (float) y;
            this.glows[at + 2] = (float) z;
            this.glows[at + 3] = (float) radius;
            this.colors[this.count++] = color;
        }

        /**
         * Adds a glow round a part, in front of it, turned round the gate's center as the part is.
         *
         * @param box the part's box, as {@link GateGlow#bounds} gives it, before it is turned
         * @param turn how far the part is turned anticlockwise round the gate's center, in degrees
         * @param reach how far the glow reaches from the part's middle, in the larger of the box's half sizes
         * @param color the color it adds at its brightest, as ARGB
         * @param back whether it glows behind the gate too, for a part on both its faces
         */
        public void around(final float[] box, final double turn, final double reach, final int color,
                final boolean back) {
            final double cx = (box[0] + box[3]) / 2;
            final double cy = (box[1] + box[4]) / 2;
            final double radius = reach * Math.max(box[3] - box[0], box[4] - box[1]) / 2;
            final double radians = Math.toRadians(turn);
            final double x = cx * Math.cos(radians) - cy * Math.sin(radians);
            final double y = cx * Math.sin(radians) + cy * Math.cos(radians);
            this.add(x, y, box[5] + Glows.CLEARANCE, radius, color);
            if (back) {
                this.add(x, y, box[2] - Glows.CLEARANCE, radius, color);
            }
        }

        /** Submits the glows, if there are any, where the pose puts them. */
        public void submit(final SubmitNodeCollector collector, final PoseStack poseStack, final RenderType type) {
            if (this.count > 0) {
                collector.submitCustomGeometry(poseStack, type, this);
            }
        }

        @Override
        public void render(final PoseStack.Pose pose, final VertexConsumer buffer) {
            for (int g = 0; g < this.count; g++) {
                final int at = g * 4;
                final float x = this.glows[at];
                final float y = this.glows[at + 1];
                final float z = this.glows[at + 2];
                final float r = this.glows[at + 3];
                this.corner(pose, buffer, x - r, y - r, z, 0, 1, this.colors[g]);
                this.corner(pose, buffer, x + r, y - r, z, 1, 1, this.colors[g]);
                this.corner(pose, buffer, x + r, y + r, z, 1, 0, this.colors[g]);
                this.corner(pose, buffer, x - r, y + r, z, 0, 0, this.colors[g]);
            }
        }

        private void corner(final PoseStack.Pose pose, final VertexConsumer buffer, final float x, final float y,
                final float z, final float u, final float v, final int color) {
            buffer.addVertex(pose, x, y, z)
                    .setColor(color)
                    .setUv(u, v)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightCoordsUtil.FULL_BRIGHT)
                    .setNormal(pose, 0, 0, 1);
        }
    }
}
