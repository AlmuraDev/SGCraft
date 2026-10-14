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

import com.almuradev.sgcraft.SGCraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/**
 * Draws a Pegasus gate, as on the show, at rest: its fixed ring, its crystal screens with their glyphs dark, and its
 * chevrons unlit.
 */
public final class PegasusGateRenderer implements GateTypeRenderer {

    private static final Identifier[] RING = {
            PegasusGateRenderer.texture("front"),
            PegasusGateRenderer.texture("back"),
            PegasusGateRenderer.texture("outer"),
            PegasusGateRenderer.texture("metal"),
    };
    private static final Identifier METAL = PegasusGateRenderer.texture("metal");
    private static final Identifier CRYSTAL = PegasusGateRenderer.texture("crystal");
    private static final Identifier STAR = PegasusGateRenderer.texture("star");
    private static final Identifier CHEVRON_LIGHT = PegasusGateRenderer.texture("chevron_light");
    private static final Identifier WHITE = SGCraft.id("textures/gate/white.png");

    // How dark the engraved parts of its chevrons are against the polished metal
    private static final float ENGRAVED = 0.45F;
    // Its chevrons' lights, unlit: the block above each, and the bars and lower part
    private static final int BLOCK_UNLIT = ARGB.colorFromFloat(1.0F, 0.3F, 0.36F, 0.45F);
    private static final int BARS_UNLIT = ARGB.colorFromFloat(1.0F, 0.42F, 0.45F, 0.5F);
    // Its glyphs, unlit: a dark blue in the crystal
    private static final int GLYPH_UNLIT = ARGB.colorFromFloat(1.0F, 0.16F, 0.26F, 0.45F);

    @Override
    public void submit(final StargateBaseRenderState state, final PoseStack poseStack,
            final SubmitNodeCollector collector) {
        final PegasusGateMesh mesh = PegasusGateMesh.get();
        if (mesh == null) {
            return;
        }
        final int light = state.lightCoords;
        final int white = ARGB.white(1.0F);
        for (int section = 0; section < PegasusGateModel.CHEVRONS; section++) {
            poseStack.pushPose();
            poseStack.rotateDegrees(Axis.ZP, (float) (section * PegasusGateModel.SECTION_ANGLE));
            for (int part = PegasusGateMesh.FRONT; part <= PegasusGateMesh.METAL; part++) {
                PegasusGateRenderer.draw(collector, poseStack, PegasusGateRenderer.RING[part], mesh.ring(part), white,
                        light);
            }
            poseStack.popPose();
        }
        PegasusGateRenderer.draw(collector, poseStack, PegasusGateRenderer.METAL, mesh.separators(), white, light);
        PegasusGateRenderer.draw(collector, poseStack, PegasusGateRenderer.CRYSTAL, mesh.screens(state.chevrons), white,
                light);
        PegasusGateRenderer.draw(collector, poseStack, PegasusGateRenderer.STAR, mesh.glyphs(state.chevrons),
                PegasusGateRenderer.GLYPH_UNLIT, light);
        final int engraved = ARGB.colorFromFloat(1.0F, PegasusGateRenderer.ENGRAVED, PegasusGateRenderer.ENGRAVED,
                PegasusGateRenderer.ENGRAVED);
        final int first = state.chevrons > 7 ? 0 : 1;
        for (int i = first; i < first + state.chevrons; i++) {
            poseStack.pushPose();
            // Position 4 is the top chevron, where the model's chevron is
            poseStack.rotateDegrees(Axis.ZP, (4 - i) * StargateBaseRenderer.CHEVRON_SPACING);
            PegasusGateRenderer.draw(collector, poseStack, PegasusGateRenderer.METAL,
                    mesh.chevron(PegasusGateMesh.CHEVRON_METAL), white, light);
            PegasusGateRenderer.draw(collector, poseStack, PegasusGateRenderer.METAL,
                    mesh.chevron(PegasusGateMesh.CHEVRON_DARK), engraved, light);
            PegasusGateRenderer.draw(collector, poseStack, PegasusGateRenderer.CHEVRON_LIGHT,
                    mesh.chevron(PegasusGateMesh.LIGHT_BLOCK), PegasusGateRenderer.BLOCK_UNLIT, light);
            PegasusGateRenderer.draw(collector, poseStack, PegasusGateRenderer.WHITE,
                    mesh.chevron(PegasusGateMesh.LIGHT_BARS), PegasusGateRenderer.BARS_UNLIT, light);
            poseStack.popPose();
        }
    }

    // Its triangles are kept on the GPU, so each part is only a draw
    private static void draw(final SubmitNodeCollector collector, final PoseStack poseStack, final Identifier texture,
            final float[] triangles, final int color, final int light) {
        GateFeatureRenderer.submit(collector, poseStack.last(), texture, GateMesh.of(triangles), color, light);
    }

    private static Identifier texture(final String name) {
        return SGCraft.id("textures/gate/pegasus/" + name + ".png");
    }
}
