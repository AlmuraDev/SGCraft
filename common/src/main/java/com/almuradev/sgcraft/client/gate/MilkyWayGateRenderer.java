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
 * Draws a Milky Way gate, as on the show, at rest: its rings, and its chevrons with their lights unlit.
 */
public final class MilkyWayGateRenderer implements GateTypeRenderer {

    // How bright an unlit chevron light is, as a fraction of full colour
    private static final float UNLIT_LIGHT = 0.3F;

    private static final Identifier[] METALS = {
            MilkyWayGateRenderer.texture("ring"),
            MilkyWayGateRenderer.texture("detail"),
            MilkyWayGateRenderer.texture("symbols"),
            MilkyWayGateRenderer.texture("glyphs"),
    };
    private static final Identifier CHEVRON = MilkyWayGateRenderer.texture("chevron");
    // The glass of each way a chevron's lights move: the slatted lights beside the V slide down with it, and the
    // triangle above rises
    private static final Identifier[] LIGHT_GLASS = {
            MilkyWayGateRenderer.texture("light"),
            MilkyWayGateRenderer.texture("slats"),
            MilkyWayGateRenderer.texture("light"),
    };

    @Override
    public void submit(final StargateBaseRenderState state, final PoseStack poseStack,
            final SubmitNodeCollector collector) {
        final MilkyWayGateMesh mesh = MilkyWayGateMesh.get();
        if (mesh == null) {
            return;
        }
        final int light = state.lightCoords;
        final int white = ARGB.white(1.0F);
        for (int part = MilkyWayGateModel.BODY; part <= MilkyWayGateModel.GLYPHS; part++) {
            MilkyWayGateRenderer.draw(collector, poseStack, MilkyWayGateRenderer.METALS[part], mesh.part(part), white,
                    light);
        }
        final int first = state.chevrons > 7 ? 0 : 1;
        for (int i = first; i < first + state.chevrons; i++) {
            poseStack.pushPose();
            // Position 4 is the top chevron, where the model's chevron is
            poseStack.rotateDegrees(Axis.ZP, (4 - i) * StargateBaseRenderer.CHEVRON_SPACING);
            MilkyWayGateRenderer.submitChevron(mesh, poseStack, collector, light);
            poseStack.popPose();
        }
    }

    // A chevron at the top of the gate, its lights unlit
    private static void submitChevron(final MilkyWayGateMesh mesh, final PoseStack poseStack,
            final SubmitNodeCollector collector, final int light) {
        final int metal = ARGB.white(1.0F);
        final int unlit = ARGB.colorFromFloat(1.0F, MilkyWayGateRenderer.UNLIT_LIGHT, MilkyWayGateRenderer.UNLIT_LIGHT,
                MilkyWayGateRenderer.UNLIT_LIGHT);
        for (int m = 0; m < mesh.moves(); m++) {
            MilkyWayGateRenderer.draw(collector, poseStack, MilkyWayGateRenderer.CHEVRON,
                    mesh.chevron(MilkyWayGateModel.METAL, m), metal, light);
        }
        for (int piece = MilkyWayGateModel.LIGHTS; piece <= MilkyWayGateModel.LOWER_LIGHT; piece++) {
            for (int m = 0; m < mesh.moves(); m++) {
                final Identifier glass = MilkyWayGateRenderer.LIGHT_GLASS[
                        Math.min(m, MilkyWayGateRenderer.LIGHT_GLASS.length - 1)];
                MilkyWayGateRenderer.draw(collector, poseStack, glass, mesh.chevron(piece, m), unlit, light);
            }
        }
    }

    // Its triangles are kept on the GPU, so each part is only a draw
    private static void draw(final SubmitNodeCollector collector, final PoseStack poseStack, final Identifier texture,
            final float[] triangles, final int color, final int light) {
        GateFeatureRenderer.submit(collector, poseStack.last(), texture, GateMesh.of(triangles), color, light);
    }

    private static Identifier texture(final String name) {
        return SGCraft.id("textures/gate/milkyway/" + name + ".png");
    }
}
