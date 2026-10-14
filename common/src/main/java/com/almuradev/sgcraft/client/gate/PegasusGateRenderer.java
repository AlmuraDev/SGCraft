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

import com.almuradev.sgcraft.gate.ChevronOrder;
import com.almuradev.sgcraft.gate.DialPhase;
import com.almuradev.sgcraft.gate.DialView;
import com.almuradev.sgcraft.SGCraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;

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
    // Its chevrons' lights, locked: white above, pale blue in the bars
    private static final int BLOCK_LIT = ARGB.white(1.0F);
    private static final int BARS_LIT = ARGB.colorFromFloat(1.0F, 0.7F, 0.88F, 1.0F);
    // Its glyphs, lit: a pale blue of their own, and a blue glow over them
    private static final int GLYPH_LIT = ARGB.colorFromFloat(1.0F, 0.8F, 0.95F, 1.0F);
    private static final int GLYPH_GLOW = ARGB.colorFromFloat(1.0F, 0.3F, 0.7F, 1.0F);
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
        final double[] screens = PegasusGateMesh.screenAngles(state.chevrons);
        final boolean[] lit = PegasusGateRenderer.litScreens(state, screens);
        for (int screen = 0; screen < screens.length; screen++) {
            final float[] glyph = mesh.glyph(state.chevrons, screen);
            if (!lit[screen]) {
                PegasusGateRenderer.draw(collector, poseStack, PegasusGateRenderer.STAR, glyph,
                        PegasusGateRenderer.GLYPH_UNLIT, light);
                continue;
            }
            // A lit glyph shines on its own, then glows over itself
            PegasusGateRenderer.draw(collector, poseStack, PegasusGateRenderer.STAR, glyph,
                    PegasusGateRenderer.GLYPH_LIT, LightCoordsUtil.FULL_BRIGHT);
            collector.submitCustomGeometry(poseStack, RenderTypes.energySwirl(PegasusGateRenderer.STAR, 0, 0),
                    (pose, buffer) -> GateGeometry.emit(pose, buffer, glyph, PegasusGateRenderer.GLYPH_GLOW,
                            LightCoordsUtil.FULL_BRIGHT));
        }
        final boolean[] chevronsLit = PegasusGateRenderer.litChevrons(state);
        final int engraved = ARGB.colorFromFloat(1.0F, PegasusGateRenderer.ENGRAVED, PegasusGateRenderer.ENGRAVED,
                PegasusGateRenderer.ENGRAVED);
        final int first = ChevronOrder.first(state.chevrons);
        for (int i = first; i < first + state.chevrons; i++) {
            poseStack.pushPose();
            // Position 4 is the top chevron, where the model's chevron is
            poseStack.rotateDegrees(Axis.ZP, (4 - i) * StargateBaseRenderer.CHEVRON_SPACING);
            PegasusGateRenderer.draw(collector, poseStack, PegasusGateRenderer.METAL,
                    mesh.chevron(PegasusGateMesh.CHEVRON_METAL), white, light);
            PegasusGateRenderer.draw(collector, poseStack, PegasusGateRenderer.METAL,
                    mesh.chevron(PegasusGateMesh.CHEVRON_DARK), engraved, light);
            final boolean locked = chevronsLit[i];
            final int lightsLight = locked ? LightCoordsUtil.FULL_BRIGHT : light;
            PegasusGateRenderer.draw(collector, poseStack, PegasusGateRenderer.CHEVRON_LIGHT,
                    mesh.chevron(PegasusGateMesh.LIGHT_BLOCK),
                    locked ? PegasusGateRenderer.BLOCK_LIT : PegasusGateRenderer.BLOCK_UNLIT, lightsLight);
            PegasusGateRenderer.draw(collector, poseStack, PegasusGateRenderer.WHITE,
                    mesh.chevron(PegasusGateMesh.LIGHT_BARS),
                    locked ? PegasusGateRenderer.BARS_LIT : PegasusGateRenderer.BARS_UNLIT, lightsLight);
            poseStack.popPose();
        }
    }

    // Which screens' glyphs are lit. The dialling gate lights the glyph under each locked chevron, and one light runs
    // round the screens to the next; the dialled gate fills with light clockwise from the top, the top last. Their
    // glyphs go dark as the wormhole shuts down.
    private static boolean[] litScreens(final StargateBaseRenderState state, final double[] screens) {
        final boolean[] lit = new boolean[screens.length];
        final DialView view = state.view;
        if (!view.active()) {
            return lit;
        }
        final int symbols = view.symbols().size();
        final int[] positions = ChevronOrder.positions(state.chevrons, symbols);
        if (view.initiator()) {
            for (int i = 0; i < Math.min(view.engaged(), positions.length); i++) {
                lit[PegasusGateRenderer.nearest(screens, ChevronOrder.angle(positions[i]))] = true;
            }
            if (view.phase() == DialPhase.DIALLING && view.engaged() < positions.length) {
                final int from = view.engaged() == 0 ? ChevronOrder.TOP : positions[view.engaged() - 1];
                final double angle = ChevronOrder.angle(from) + view.ringAngle(state.time) - view.ringFrom();
                lit[PegasusGateRenderer.nearest(screens, angle)] = true;
            }
            return lit;
        }
        final double fill = PegasusGateRenderer.fill(state);
        for (int s = 0; s < screens.length; s++) {
            lit[s] = PegasusGateRenderer.clockwiseFromTop(screens[s]) <= fill;
        }
        return lit;
    }

    // Which chevrons are lit: the dialling gate's as they lock; the dialled gate's as its light fills past them,
    // for as many as the address has symbols. They stay lit until the gate is idle.
    private static boolean[] litChevrons(final StargateBaseRenderState state) {
        final DialView view = state.view;
        if (view.initiator() || !view.active()) {
            return state.locked;
        }
        final boolean[] lit = new boolean[ChevronOrder.POSITIONS];
        final double fill = PegasusGateRenderer.fill(state);
        for (final int position : ChevronOrder.positions(state.chevrons, view.symbols().size())) {
            lit[position] = PegasusGateRenderer.clockwiseFromTop(ChevronOrder.angle(position)) <= fill;
        }
        return lit;
    }

    // How far a dialled gate's light has filled, in degrees clockwise from the top
    private static double fill(final StargateBaseRenderState state) {
        final DialView view = state.view;
        if (view.engaged() >= view.symbols().size() && view.engaged() > 0) {
            return 360;
        }
        final long length = view.fillEnd() - view.fillStart();
        if (length <= 0) {
            return 0;
        }
        return 360 * Math.max(0, Math.min(1, (state.time - view.fillStart()) / length));
    }

    // Degrees clockwise from the top, the top itself counting as the end of a full turn
    private static double clockwiseFromTop(final double angle) {
        final double clockwise = ((90 - angle) % 360 + 360) % 360;
        return clockwise < 0.5 ? 360 : clockwise;
    }

    private static int nearest(final double[] screens, final double angle) {
        int best = 0;
        double bestDistance = Double.MAX_VALUE;
        for (int s = 0; s < screens.length; s++) {
            final double distance = Math.abs(((screens[s] - angle) % 360 + 540) % 360 - 180);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = s;
            }
        }
        return best;
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
