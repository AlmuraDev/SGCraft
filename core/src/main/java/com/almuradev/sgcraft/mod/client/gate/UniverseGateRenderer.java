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

import com.almuradev.sgcraft.api.address.Symbol;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.gate.ChevronOrder;
import com.almuradev.sgcraft.mod.gate.ChevronSpacing;
import com.almuradev.sgcraft.mod.gate.DialPhase;
import com.almuradev.sgcraft.mod.gate.DialView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;

import java.util.List;

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
    private static final Identifier SYMBOLS = UniverseGateRenderer.texture("glyphs");
    private static final Identifier SYMBOLS_GLOW = UniverseGateRenderer.texture("glyphs_glow");
    private static final Identifier CHEVRON_GLOW = UniverseGateRenderer.texture("chevron_glow");
    private static final Identifier CHEVRON_LIGHTS = UniverseGateRenderer.texture("chevron_lights");
    // A lit symbol is its glyph added twice in white, then its blurred glow in pale blue; a lit chevron is its glow
    // added in gray, then its lights in white
    private static final int CORE_LAYERS = 2;
    private static final int SYMBOL_GLOW = ARGB.colorFromFloat(1.0F, 0.96F, 1.0F, 1.0F);
    private static final int CHEVRON_GLOW_COLOR = ARGB.colorFromFloat(1.0F, 0.35F, 0.35F, 0.35F);

    // How much darker the unlit symbols are than the panels they are cut into, and the chevrons' unlit lights than
    // the metal round them
    private static final float SYMBOL_CUT = 0.4F;
    private static final float UNLIT_LIGHT = 0.6F;
    // The cool white its lit parts light the gate with; how bright the glow round a lit symbol and a lit chevron is at
    // most, and how far each reaches, in the half size of the symbol or the chevron's lights
    private static final int GLOW = ARGB.color(225, 235, 255);
    private static final float SYMBOL_GLOW_STRENGTH = 0.075F;
    private static final double SYMBOL_REACH = 2.4;
    private static final float CHEVRON_GLOW_STRENGTH = 0.11F;
    private static final double CHEVRON_REACH = 2.0;

    @Override
    public void submit(final StargateBaseRenderState state, final PoseStack poseStack,
            final SubmitNodeCollector collector) {
        final UniverseGateMesh mesh = UniverseGateMesh.get();
        if (mesh == null) {
            return;
        }
        final DialView view = state.view;
        final int light = state.lightCoords;
        final int white = ARGB.white(1.0F);
        final double spacing = state.spacing;
        // Only the dialing gate turns, its chevrons with it
        poseStack.pushPose();
        if (view.initiator() && view.phase() != DialPhase.IDLE) {
            poseStack.rotateDegrees(Axis.ZP, UniverseGateRenderer.turn(view, state.time, spacing));
        }
        for (int part = UniverseGateMesh.BODY; part <= UniverseGateMesh.DETAIL; part++) {
            UniverseGateRenderer.solid(collector, poseStack, UniverseGateRenderer.RING[part],
                    GateGeometry.squeezed(mesh.ring(part), spacing), white, light);
        }
        // Like the other gates, it has seven chevrons, the bottom two left out, until it has nine: chevron i is the
        // model's (i - 4)th clockwise from the top
        final int first = ChevronOrder.first(state.chevrons);
        for (int i = first; i < first + state.chevrons; i++) {
            final int slot = ((i - 4) % UniverseGateModel.CHEVRONS + UniverseGateModel.CHEVRONS)
                    % UniverseGateModel.CHEVRONS;
            // Each chevron moves whole to where it now stands
            poseStack.pushPose();
            poseStack.rotateDegrees(Axis.ZP, (float) (ChevronSpacing.angle(i, spacing)
                    - ChevronSpacing.angle(i, ChevronSpacing.STANDARD)));
            UniverseGateRenderer.solid(collector, poseStack, UniverseGateRenderer.CHEVRON, mesh.chevron(slot), white,
                    light);
            poseStack.popPose();
        }
        UniverseGateRenderer.solid(collector, poseStack, UniverseGateRenderer.PANEL,
                GateGeometry.squeezed(mesh.panels(), spacing), white, light);
        UniverseGateRenderer.overlay(collector, poseStack, UniverseGateRenderer.SYMBOLS_CUT,
                GateGeometry.squeezed(mesh.symbols(), spacing),
                ARGB.colorFromFloat(UniverseGateRenderer.SYMBOL_CUT, 0.0F, 0.0F, 0.0F), light);
        state.glows.clear();
        // The symbols dialed so far shine out of the ring, white, with a blue glow
        if (view.active()) {
            final int symbolGlow = ARGB.scaleRGB(UniverseGateRenderer.GLOW, UniverseGateRenderer.SYMBOL_GLOW_STRENGTH);
            final List<Symbol> symbols = view.symbols();
            for (int k = 0; k < Math.min(view.engaged(), symbols.size()); k++) {
                final float[] standard = mesh.symbol(symbols.get(k).index());
                if (standard == null) {
                    continue;
                }
                final float[] symbol = GateGeometry.squeezed(standard, spacing);
                for (int layer = 0; layer < UniverseGateRenderer.CORE_LAYERS; layer++) {
                    UniverseGateRenderer.glow(collector, poseStack, UniverseGateRenderer.SYMBOLS, symbol, white);
                }
                UniverseGateRenderer.glow(collector, poseStack, UniverseGateRenderer.SYMBOLS_GLOW, symbol,
                        UniverseGateRenderer.SYMBOL_GLOW);
                state.glows.around(GateGlow.bounds(symbol), 0, UniverseGateRenderer.SYMBOL_REACH, symbolGlow, true);
            }
        }
        // Every chevron lights at once while the gate is active; otherwise their lights are darkened
        final int darkened = ARGB.colorFromFloat(UniverseGateRenderer.UNLIT_LIGHT, 0.0F, 0.0F, 0.0F);
        final int chevronGlow = ARGB.scaleRGB(UniverseGateRenderer.GLOW, UniverseGateRenderer.CHEVRON_GLOW_STRENGTH);
        for (int i = first; i < first + state.chevrons; i++) {
            poseStack.pushPose();
            final double turn = ChevronSpacing.angle(i, spacing) - 90;
            poseStack.rotateDegrees(Axis.ZP, (float) turn);
            UniverseGateRenderer.overlay(collector, poseStack, UniverseGateRenderer.PAINT,
                    mesh.overlay(UniverseGateMesh.PAINT), white, light);
            if (view.active()) {
                UniverseGateRenderer.glow(collector, poseStack, UniverseGateRenderer.CHEVRON_GLOW,
                        mesh.overlay(UniverseGateMesh.LIGHTS), UniverseGateRenderer.CHEVRON_GLOW_COLOR);
                UniverseGateRenderer.glow(collector, poseStack, UniverseGateRenderer.CHEVRON_LIGHTS,
                        mesh.overlay(UniverseGateMesh.LIGHTS), white);
                state.glows.around(GateGlow.bounds(mesh.overlay(UniverseGateMesh.LIGHTS)), turn,
                        UniverseGateRenderer.CHEVRON_REACH, chevronGlow, true);
            } else {
                UniverseGateRenderer.overlay(collector, poseStack, UniverseGateRenderer.LIGHTS_CUT,
                        mesh.overlay(UniverseGateMesh.LIGHTS), darkened, light);
            }
            poseStack.popPose();
        }
        // Turned with the ring, as its symbols and chevrons are
        state.glows.submit(collector, poseStack, GateGlow.HALO_TYPE);
        poseStack.popPose();
    }

    @Override
    public int glowColor() {
        return UniverseGateRenderer.GLOW;
    }

    // How far the ring has turned. Its turn is worked out at the standard spacing; with its chevrons drawn together,
    // each move starts and ends where the same symbols come to the top, and turns evenly between, so it doesn't
    // speed up across the ring's stretched bottom
    private static float turn(final DialView view, final double time, final double spacing) {
        final float standard = view.ringAngle(time);
        if (spacing == ChevronSpacing.STANDARD) {
            return standard;
        }
        final double span = view.ringTo() - view.ringFrom();
        final double progress = span == 0 ? 1 : (standard - view.ringFrom()) / span;
        final double from = UniverseGateRenderer.atTop(view.ringFrom(), spacing);
        final double to = UniverseGateRenderer.atTop(view.ringTo(), spacing);
        return (float) (from + (to - from) * progress);
    }

    // A turn of the ring at the standard spacing, as the same turn at another: the symbol it brings to the top
    private static double atTop(final double turn, final double spacing) {
        return 90 - ChevronSpacing.squeeze(90 - turn, spacing);
    }

    // Added to what is beneath it, at full brightness
    private static void glow(final SubmitNodeCollector collector, final PoseStack poseStack,
            final Identifier texture, final float[] triangles, final int color) {
        collector.submitCustomGeometry(poseStack, RenderTypes.energySwirl(texture, 0, 0),
                (pose, buffer) -> GateGeometry.emit(pose, buffer, triangles, color, LightCoordsUtil.FULL_BRIGHT));
    }

    private static void solid(final SubmitNodeCollector collector, final PoseStack poseStack, final Identifier texture,
            final float[] triangles, final int color, final int light) {
        GateFeatureRenderer.submit(collector, poseStack.last(), texture, GateMesh.of(triangles), color, light);
    }

    // Blended over what is beneath it by the texture's alpha and the color's. These are few triangles, sent each
    // frame with the level's translucent geometry, so they blend as it does
    private static void overlay(final SubmitNodeCollector collector, final PoseStack poseStack,
            final Identifier texture, final float[] triangles, final int color, final int light) {
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(texture),
                (pose, buffer) -> GateGeometry.emit(pose, buffer, triangles, color, light));
    }

    private static Identifier texture(final String name) {
        return SGCraft.id("textures/gate/universe/" + name + ".png");
    }
}
