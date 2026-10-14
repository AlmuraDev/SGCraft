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
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/**
 * Draws a Universe gate, as on the show, at rest: its ring, its symbols cut into the panels in their arches, and its
 * chevrons with their bronze studs and their lights unlit.
 */
public final class UniverseGateRenderer implements GateTypeRenderer {

    private static final Identifier[] RING = {
            UniverseGateRenderer.texture("ring"),
            UniverseGateRenderer.texture("frame"),
            UniverseGateRenderer.texture("detail"),
    };
    private static final Identifier CHEVRON = UniverseGateRenderer.texture("chevron");
    private static final Identifier PANEL = UniverseGateRenderer.texture("panel");
    private static final Identifier SYMBOLS_CUT = UniverseGateRenderer.texture("glyphs_cut");
    private static final Identifier PAINT = UniverseGateRenderer.texture("chevron_paint");
    private static final Identifier LIGHTS_CUT = UniverseGateRenderer.texture("chevron_lights_cut");

    // How much darker the unlit symbols are than the panels they are cut into, and the chevrons' unlit lights than
    // the metal round them
    private static final float SYMBOL_CUT = 0.4F;
    private static final float UNLIT_LIGHT = 0.6F;

    @Override
    public void submit(final StargateBaseRenderState state, final PoseStack poseStack,
            final SubmitNodeCollector collector) {
        final UniverseGateMesh mesh = UniverseGateMesh.get();
        if (mesh == null) {
            return;
        }
        final int light = state.lightCoords;
        final int white = ARGB.white(1.0F);
        for (int part = UniverseGateMesh.BODY; part <= UniverseGateMesh.DETAIL; part++) {
            UniverseGateRenderer.solid(collector, poseStack, UniverseGateRenderer.RING[part], mesh.ring(part), white,
                    light);
        }
        // Like the other gates, it has seven chevrons, the bottom two left out, until it has nine: chevron i is the
        // model's (i - 4)th clockwise from the top
        final int first = state.chevrons > 7 ? 0 : 1;
        for (int i = first; i < first + state.chevrons; i++) {
            final int slot = ((i - 4) % UniverseGateModel.CHEVRONS + UniverseGateModel.CHEVRONS)
                    % UniverseGateModel.CHEVRONS;
            UniverseGateRenderer.solid(collector, poseStack, UniverseGateRenderer.CHEVRON, mesh.chevron(slot), white,
                    light);
        }
        UniverseGateRenderer.solid(collector, poseStack, UniverseGateRenderer.PANEL, mesh.panels(), white, light);
        UniverseGateRenderer.overlay(collector, poseStack, UniverseGateRenderer.SYMBOLS_CUT, mesh.symbols(),
                ARGB.colorFromFloat(UniverseGateRenderer.SYMBOL_CUT, 0.0F, 0.0F, 0.0F), light);
        final int darkened = ARGB.colorFromFloat(UniverseGateRenderer.UNLIT_LIGHT, 0.0F, 0.0F, 0.0F);
        for (int i = first; i < first + state.chevrons; i++) {
            poseStack.pushPose();
            poseStack.rotateDegrees(Axis.ZP, (4 - i) * StargateBaseRenderer.CHEVRON_SPACING);
            UniverseGateRenderer.overlay(collector, poseStack, UniverseGateRenderer.PAINT,
                    mesh.overlay(UniverseGateMesh.PAINT), white, light);
            UniverseGateRenderer.overlay(collector, poseStack, UniverseGateRenderer.LIGHTS_CUT,
                    mesh.overlay(UniverseGateMesh.LIGHTS), darkened, light);
            poseStack.popPose();
        }
    }

    private static void solid(final SubmitNodeCollector collector, final PoseStack poseStack, final Identifier texture,
            final float[] triangles, final int color, final int light) {
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(texture),
                (pose, buffer) -> GateGeometry.emit(pose, buffer, triangles, color, light));
    }

    // Blended over what is beneath it by the texture's alpha and the colour's
    private static void overlay(final SubmitNodeCollector collector, final PoseStack poseStack,
            final Identifier texture, final float[] triangles, final int color, final int light) {
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(texture),
                (pose, buffer) -> GateGeometry.emit(pose, buffer, triangles, color, light));
    }

    private static Identifier texture(final String name) {
        return SGCraft.id("textures/gate/universe/" + name + ".png");
    }
}
