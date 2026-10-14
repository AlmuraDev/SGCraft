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
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import org.jspecify.annotations.Nullable;

/**
 * Draws a Milky Way gate, as on the show, at rest: its rings, and its chevrons with their lights unlit.
 */
public final class MilkyWayGateRenderer implements GateTypeRenderer {

    // How bright an unlit chevron light is, as a fraction of full colour
    private static final float UNLIT_LIGHT = 0.3F;
    // How long the top chevron takes to come down, stays down and takes to rise as a symbol locks, in ticks; and
    // the same for the last symbol, which it holds longer
    private static final int[] LOCK_TICKS = {6, 10, 6};
    private static final int[] FINAL_LOCK_TICKS = {4, 22, 3};

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
        final DialView view = state.view;
        final int light = state.lightCoords;
        final int white = ARGB.white(1.0F);
        MilkyWayGateRenderer.draw(collector, poseStack, MilkyWayGateRenderer.METALS[MilkyWayGateModel.BODY],
                mesh.part(MilkyWayGateModel.BODY), white, light);
        MilkyWayGateRenderer.draw(collector, poseStack, MilkyWayGateRenderer.METALS[MilkyWayGateModel.DETAIL],
                mesh.part(MilkyWayGateModel.DETAIL), white, light);
        // Only the dialling gate's symbol ring turns
        poseStack.pushPose();
        if (view.initiator() && view.phase() != DialPhase.IDLE) {
            poseStack.rotateDegrees(Axis.ZP, view.ringAngle(state.time));
        }
        MilkyWayGateRenderer.draw(collector, poseStack, MilkyWayGateRenderer.METALS[MilkyWayGateModel.SYMBOL_RING],
                mesh.part(MilkyWayGateModel.SYMBOL_RING), white, light);
        MilkyWayGateRenderer.draw(collector, poseStack, MilkyWayGateRenderer.METALS[MilkyWayGateModel.GLYPHS],
                mesh.part(MilkyWayGateModel.GLYPHS), white, light);
        poseStack.popPose();
        final Lock lock = MilkyWayGateRenderer.lock(state);
        final int first = ChevronOrder.first(state.chevrons);
        for (int i = first; i < first + state.chevrons; i++) {
            boolean lit = state.locked[i];
            float dip = 0;
            if (lock != null) {
                if (i == lock.position && lock.time < lock.down) {
                    // The newly locked chevron lights once the top chevron has come down
                    lit = false;
                }
                if (i == ChevronOrder.TOP) {
                    dip = lock.dip;
                    lit = lit || lock.time >= lock.down && lock.time < lock.down + lock.hold;
                }
            }
            poseStack.pushPose();
            // Position 4 is the top chevron, where the model's chevron is
            poseStack.rotateDegrees(Axis.ZP, (4 - i) * StargateBaseRenderer.CHEVRON_SPACING);
            MilkyWayGateRenderer.submitChevron(mesh, poseStack, collector, light, lit, dip);
            poseStack.popPose();
        }
    }

    /**
     * How the top chevron is moving as the dialling gate locks a symbol: it comes down, holds and rises, and the
     * symbol's own chevron lights once it is down.
     *
     * @param position the position of the chevron the symbol locks
     * @param time how long since the symbol locked, in ticks
     * @param down how long the top chevron takes to come down
     * @param hold how long it stays down
     * @param dip how far down it is, from 0 to 1
     */
    private record Lock(int position, double time, int down, int hold, float dip) {
    }

    private static @Nullable Lock lock(final StargateBaseRenderState state) {
        final DialView view = state.view;
        final int symbols = view.symbols().size();
        if (!view.initiator() || view.engaged() <= 0 || symbols == 0) {
            return null;
        }
        switch (view.phase()) {
            case DIALLING, INTER_DIALLING, SYNC_AWAIT, ESTABLISHING -> {
            }
            default -> {
                return null;
            }
        }
        final boolean last = view.engaged() >= symbols;
        final int[] ticks = last ? MilkyWayGateRenderer.FINAL_LOCK_TICKS : MilkyWayGateRenderer.LOCK_TICKS;
        final double time = state.time - view.lockTick();
        if (time < 0 || time >= ticks[0] + ticks[1] + ticks[2]) {
            return null;
        }
        final double progress;
        if (time < ticks[0]) {
            progress = time / ticks[0];
        } else if (time < ticks[0] + ticks[1]) {
            progress = 1;
        } else {
            progress = 1 - (time - ticks[0] - ticks[1]) / ticks[2];
        }
        final float dip = (float) (0.5 - 0.5 * Math.cos(Math.PI * Math.max(0, Math.min(1, progress))));
        final int position = ChevronOrder.positions(state.chevrons, symbols)[view.engaged() - 1];
        return new Lock(position, time, ticks[0], ticks[1], dip);
    }

    // A chevron at the top of the gate, its lights glowing while it is locked; the parts that move when the top
    // chevron locks are moved by dip
    private static void submitChevron(final MilkyWayGateMesh mesh, final PoseStack poseStack,
            final SubmitNodeCollector collector, final int light, final boolean lit, final float dip) {
        final int metal = ARGB.white(1.0F);
        final int glass = lit ? ARGB.white(1.0F) : ARGB.colorFromFloat(1.0F, MilkyWayGateRenderer.UNLIT_LIGHT,
                MilkyWayGateRenderer.UNLIT_LIGHT, MilkyWayGateRenderer.UNLIT_LIGHT);
        final int glassLight = lit ? LightCoordsUtil.FULL_BRIGHT : light;
        for (int m = 0; m < mesh.moves(); m++) {
            poseStack.pushPose();
            final double[] offset = mesh.offset(m);
            poseStack.translate(offset[0] * dip, offset[1] * dip, offset[2] * dip);
            MilkyWayGateRenderer.draw(collector, poseStack, MilkyWayGateRenderer.CHEVRON,
                    mesh.chevron(MilkyWayGateModel.METAL, m), metal, light);
            for (int piece = MilkyWayGateModel.LIGHTS; piece <= MilkyWayGateModel.LOWER_LIGHT; piece++) {
                // The lower light shows only while the top chevron is down and lit
                final boolean shining = piece == MilkyWayGateModel.LOWER_LIGHT ? lit && dip > 0 : lit;
                final Identifier texture = MilkyWayGateRenderer.LIGHT_GLASS[
                        Math.min(m, MilkyWayGateRenderer.LIGHT_GLASS.length - 1)];
                MilkyWayGateRenderer.draw(collector, poseStack, texture, mesh.chevron(piece, m),
                        shining ? glass : MilkyWayGateRenderer.unlit(), shining ? glassLight : light);
            }
            poseStack.popPose();
        }
    }

    private static int unlit() {
        return ARGB.colorFromFloat(1.0F, MilkyWayGateRenderer.UNLIT_LIGHT, MilkyWayGateRenderer.UNLIT_LIGHT,
                MilkyWayGateRenderer.UNLIT_LIGHT);
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
