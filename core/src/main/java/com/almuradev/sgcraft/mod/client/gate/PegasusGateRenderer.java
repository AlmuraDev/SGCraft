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
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

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
    // The blue its lit parts light the gate with; how bright the glow round a lit glyph and a lit chevron is at most,
    // and how far each reaches, in the half size of the glyph or the chevron's lights
    private static final int GLOW = ARGB.color(90, 160, 255);
    private static final float GLYPH_GLOW_STRENGTH = 0.14F;
    private static final double GLYPH_REACH = 2.4;
    private static final float CHEVRON_GLOW = 0.2F;
    private static final double CHEVRON_REACH = 2.2;

    // The box round a chevron's lights at the top, out to the front of its metal, and the mesh it is worked out for
    private final float[] chevronBox = new float[6];
    private @Nullable PegasusGateMesh boxMesh;

    @Override
    public void submit(final StargateBaseRenderState state, final PoseStack poseStack,
            final SubmitNodeCollector collector) {
        final PegasusGateMesh mesh = PegasusGateMesh.get();
        if (mesh == null) {
            return;
        }
        final int light = state.lightCoords;
        final int white = ARGB.white(1.0F);
        final double spacing = state.spacing;
        if (spacing == ChevronSpacing.STANDARD) {
            for (int section = 0; section < PegasusGateModel.CHEVRONS; section++) {
                poseStack.pushPose();
                poseStack.rotateDegrees(Axis.ZP, (float) (section * PegasusGateModel.SECTION_ANGLE));
                for (int part = PegasusGateMesh.FRONT; part <= PegasusGateMesh.METAL; part++) {
                    PegasusGateRenderer.draw(collector, poseStack, PegasusGateRenderer.RING[part], mesh.ring(part),
                            white, light);
                }
                poseStack.popPose();
            }
        } else {
            // Drawn together, the sections are no longer alike, so the whole ring is drawn following its chevrons
            for (int part = PegasusGateMesh.FRONT; part <= PegasusGateMesh.METAL; part++) {
                PegasusGateRenderer.draw(collector, poseStack, PegasusGateRenderer.RING[part],
                        GateGeometry.squeezed(PegasusGateRenderer.whole(mesh.ring(part)), spacing), white, light);
            }
        }
        PegasusGateRenderer.draw(collector, poseStack, PegasusGateRenderer.METAL,
                GateGeometry.squeezed(mesh.separators(), spacing), white, light);
        PegasusGateRenderer.draw(collector, poseStack, PegasusGateRenderer.CRYSTAL,
                GateGeometry.squeezed(mesh.screens(state.chevrons), spacing), white, light);
        state.glows.clear();
        final double[] screens = PegasusGateMesh.screenAngles(state.chevrons);
        final boolean[] lit = PegasusGateRenderer.litScreens(state, screens);
        final int glyphGlow = ARGB.scaleRGB(PegasusGateRenderer.GLOW, PegasusGateRenderer.GLYPH_GLOW_STRENGTH);
        for (int screen = 0; screen < screens.length; screen++) {
            final float[] glyph = GateGeometry.squeezed(mesh.glyph(state.chevrons, screen), spacing);
            if (!lit[screen]) {
                PegasusGateRenderer.draw(collector, poseStack, PegasusGateRenderer.STAR, glyph,
                        PegasusGateRenderer.GLYPH_UNLIT, light);
                continue;
            }
            state.glows.around(GateGlow.bounds(glyph), 0, PegasusGateRenderer.GLYPH_REACH, glyphGlow, false);
            // A lit glyph shines with its own light, then glows over itself
            PegasusGateRenderer.shine(collector, poseStack, PegasusGateRenderer.STAR, glyph,
                    PegasusGateRenderer.GLYPH_LIT);
            collector.submitCustomGeometry(poseStack, RenderTypes.energySwirl(PegasusGateRenderer.STAR, 0, 0),
                    (pose, buffer) -> GateGeometry.emit(pose, buffer, glyph, PegasusGateRenderer.GLYPH_GLOW,
                            LightCoordsUtil.FULL_BRIGHT));
        }
        final boolean[] chevronsLit = PegasusGateRenderer.litChevrons(state);
        final int engraved = ARGB.colorFromFloat(1.0F, PegasusGateRenderer.ENGRAVED, PegasusGateRenderer.ENGRAVED,
                PegasusGateRenderer.ENGRAVED);
        final float[] box = this.chevronBox(mesh);
        final int chevronGlow = ARGB.scaleRGB(PegasusGateRenderer.GLOW, PegasusGateRenderer.CHEVRON_GLOW);
        final int first = ChevronOrder.first(state.chevrons);
        for (int i = first; i < first + state.chevrons; i++) {
            poseStack.pushPose();
            // Position 4 is the top chevron, where the model's chevron is
            final double turn = ChevronSpacing.angle(i, spacing) - 90;
            poseStack.rotateDegrees(Axis.ZP, (float) turn);
            PegasusGateRenderer.draw(collector, poseStack, PegasusGateRenderer.METAL,
                    mesh.chevron(PegasusGateMesh.CHEVRON_METAL), white, light);
            PegasusGateRenderer.draw(collector, poseStack, PegasusGateRenderer.METAL,
                    mesh.chevron(PegasusGateMesh.CHEVRON_DARK), engraved, light);
            if (chevronsLit[i]) {
                PegasusGateRenderer.shine(collector, poseStack, PegasusGateRenderer.CHEVRON_LIGHT,
                        mesh.chevron(PegasusGateMesh.LIGHT_BLOCK), PegasusGateRenderer.BLOCK_LIT);
                PegasusGateRenderer.shine(collector, poseStack, PegasusGateRenderer.WHITE,
                        mesh.chevron(PegasusGateMesh.LIGHT_BARS), PegasusGateRenderer.BARS_LIT);
                state.glows.around(box, turn, PegasusGateRenderer.CHEVRON_REACH, chevronGlow, false);
            } else {
                PegasusGateRenderer.draw(collector, poseStack, PegasusGateRenderer.CHEVRON_LIGHT,
                        mesh.chevron(PegasusGateMesh.LIGHT_BLOCK), PegasusGateRenderer.BLOCK_UNLIT, light);
                PegasusGateRenderer.draw(collector, poseStack, PegasusGateRenderer.WHITE,
                        mesh.chevron(PegasusGateMesh.LIGHT_BARS), PegasusGateRenderer.BARS_UNLIT, light);
            }
            poseStack.popPose();
        }
        state.glows.submit(collector, poseStack, GateGlow.HALO_TYPE);
    }

    @Override
    public int glowColor() {
        return PegasusGateRenderer.GLOW;
    }

    // The box round the top chevron's lights, out to the front of its metal, which its glow is laid round; worked out
    // once for each mesh
    private float[] chevronBox(final PegasusGateMesh mesh) {
        if (this.boxMesh != mesh) {
            final float[] box = this.chevronBox;
            final float[] block = GateGlow.bounds(mesh.chevron(PegasusGateMesh.LIGHT_BLOCK));
            final float[] bars = GateGlow.bounds(mesh.chevron(PegasusGateMesh.LIGHT_BARS));
            final float[] metal = GateGlow.bounds(mesh.chevron(PegasusGateMesh.CHEVRON_METAL));
            for (int axis = 0; axis < 3; axis++) {
                box[axis] = Math.min(block[axis], bars[axis]);
                box[3 + axis] = Math.max(block[3 + axis], bars[3 + axis]);
            }
            box[5] = Math.max(box[5], metal[5]);
            this.boxMesh = mesh;
        }
        return this.chevronBox;
    }

    // A section of the ring, turned to every section's place, as one
    private static float[] whole(final float[] section) {
        return PegasusGateRenderer.WHOLE.computeIfAbsent(section, key -> {
            final List<float[]> sections = new ArrayList<>();
            for (int s = 0; s < PegasusGateModel.CHEVRONS; s++) {
                sections.add(GateGeometry.turned(section, s * PegasusGateModel.SECTION_ANGLE));
            }
            return GateGeometry.joined(sections);
        });
    }

    private static final Map<float[], float[]> WHOLE = new IdentityHashMap<>();

    // Which screens' glyphs are lit, worked out at the standard spacing, which the screens are drawn from. The
    // dialing gate lights the glyph under each locked chevron, and one light runs round the screens to the next; the
    // dialed gate fills with light clockwise from the top, the top last. Their glyphs go dark as the wormhole shuts
    // down.
    private static boolean[] litScreens(final StargateBaseRenderState state, final double[] screens) {
        final boolean[] lit = new boolean[screens.length];
        final DialView view = state.view;
        if (!view.active()) {
            return lit;
        }
        final int symbols = view.length();
        final int[] positions = ChevronOrder.positions(state.chevrons, symbols);
        if (view.initiator()) {
            for (int i = 0; i < Math.min(view.engaged(), positions.length); i++) {
                lit[PegasusGateRenderer.nearest(screens, ChevronOrder.angle(positions[i]))] = true;
            }
            if (view.phase() == DialPhase.DIALING && view.engaged() < positions.length) {
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

    // Which chevrons are lit: the dialing gate's as they lock; the dialed gate's as its light fills past them,
    // for as many as the address has symbols. They stay lit until the gate is idle.
    private static boolean[] litChevrons(final StargateBaseRenderState state) {
        final DialView view = state.view;
        if (view.initiator() || !view.active()) {
            return state.locked;
        }
        final boolean[] lit = new boolean[ChevronOrder.POSITIONS];
        final double fill = PegasusGateRenderer.fill(state);
        for (final int position : ChevronOrder.positions(state.chevrons, view.length())) {
            lit[position] = PegasusGateRenderer.clockwiseFromTop(ChevronOrder.angle(position)) <= fill;
        }
        return lit;
    }

    // How far a dialed gate's light has filled, in degrees clockwise from the top
    private static double fill(final StargateBaseRenderState state) {
        final DialView view = state.view;
        if (view.engaged() >= view.length() && view.engaged() > 0) {
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

    // Shining with its own light, such as a lit glyph or chevron
    private static void shine(final SubmitNodeCollector collector, final PoseStack poseStack,
            final Identifier texture, final float[] triangles, final int color) {
        GateFeatureRenderer.submitEmissive(collector, poseStack.last(), texture, GateMesh.of(triangles), color);
    }

    private static Identifier texture(final String name) {
        return SGCraft.id("textures/gate/pegasus/" + name + ".png");
    }
}
