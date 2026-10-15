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
import com.almuradev.sgcraft.gate.ChevronOrder;
import com.almuradev.sgcraft.gate.ChevronSpacing;
import com.almuradev.sgcraft.gate.DialPhase;
import com.almuradev.sgcraft.gate.DialView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

/**
 * Draws a Milky Way gate, as on the show, at rest: its rings, and its chevrons with their lights unlit.
 */
public final class MilkyWayGateRenderer implements GateTypeRenderer {

    // How bright an unlit chevron light is, as a fraction of full color
    private static final float UNLIT_LIGHT = 0.3F;
    // How long the top chevron takes to come down, stays down and takes to rise as a symbol locks, in ticks; and
    // the same for the last symbol, which it holds longer
    private static final int[] LOCK_TICKS = {6, 10, 6};
    private static final int[] FINAL_LOCK_TICKS = {4, 22, 3};
    // The warm orange its chevrons light the gate with, how bright their glow is at most, and how far it reaches, in
    // the half size of a chevron's lights
    private static final int GLOW = ARGB.color(255, 150, 50);
    private static final float CHEVRON_GLOW = 0.4F;
    private static final double CHEVRON_REACH = 2.6;

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

    // The box round the top chevron's lights, and the mesh it is worked out for
    private final float[] chevronBox = new float[6];
    private @Nullable MilkyWayGateMesh boxMesh;

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
        // The fixed ring follows its chevrons, which camouflage can draw together
        MilkyWayGateRenderer.draw(collector, poseStack, MilkyWayGateRenderer.METALS[MilkyWayGateModel.BODY],
                GateGeometry.squeezed(mesh.part(MilkyWayGateModel.BODY), state.spacing), white, light);
        MilkyWayGateRenderer.draw(collector, poseStack, MilkyWayGateRenderer.METALS[MilkyWayGateModel.DETAIL],
                GateGeometry.squeezed(mesh.part(MilkyWayGateModel.DETAIL), state.spacing), white, light);
        // Only the dialing gate's symbol ring turns
        poseStack.pushPose();
        if (view.initiator() && view.phase() != DialPhase.IDLE) {
            poseStack.rotateDegrees(Axis.ZP, view.ringAngle(state.time));
        }
        MilkyWayGateRenderer.draw(collector, poseStack, MilkyWayGateRenderer.METALS[MilkyWayGateModel.SYMBOL_RING],
                mesh.part(MilkyWayGateModel.SYMBOL_RING), white, light);
        MilkyWayGateRenderer.draw(collector, poseStack, MilkyWayGateRenderer.METALS[MilkyWayGateModel.GLYPHS],
                mesh.part(MilkyWayGateModel.GLYPHS), white, light);
        poseStack.popPose();
        state.glows.clear();
        final float[] box = this.chevronBox(mesh);
        final int glow = ARGB.scaleRGB(MilkyWayGateRenderer.GLOW, MilkyWayGateRenderer.CHEVRON_GLOW);
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
            final double turn = ChevronSpacing.angle(i, state.spacing) - 90;
            poseStack.rotateDegrees(Axis.ZP, (float) turn);
            MilkyWayGateRenderer.submitChevron(mesh, poseStack, collector, light, lit, dip);
            poseStack.popPose();
            if (lit) {
                state.glows.around(box, turn, MilkyWayGateRenderer.CHEVRON_REACH, glow, false);
            }
        }
        state.glows.submit(collector, poseStack, GateGlow.HALO_TYPE);
    }

    @Override
    public int glowColor() {
        return MilkyWayGateRenderer.GLOW;
    }

    // The box round the top chevron's lights, out to the front of its metal, which its glow is laid round; worked out
    // once for each mesh
    private float[] chevronBox(final MilkyWayGateMesh mesh) {
        if (this.boxMesh != mesh) {
            final float[] box = this.chevronBox;
            System.arraycopy(GateGlow.bounds(mesh.chevron(MilkyWayGateModel.LIGHTS, 0)), 0, box, 0, box.length);
            for (int m = 0; m < mesh.moves(); m++) {
                for (int piece = MilkyWayGateModel.METAL; piece <= MilkyWayGateModel.LOWER_LIGHT; piece++) {
                    final float[] part = GateGlow.bounds(mesh.chevron(piece, m));
                    if (piece != MilkyWayGateModel.METAL) {
                        for (int axis = 0; axis < 2; axis++) {
                            box[axis] = Math.min(box[axis], part[axis]);
                            box[3 + axis] = Math.max(box[3 + axis], part[3 + axis]);
                        }
                    }
                    box[5] = Math.max(box[5], part[5]);
                }
            }
            this.boxMesh = mesh;
        }
        return this.chevronBox;
    }

    /**
     * How the top chevron is moving as the dialing gate locks a symbol: it comes down, holds and rises, and the
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
        final int symbols = view.length();
        if (!view.initiator() || view.engaged() <= 0 || symbols == 0) {
            return null;
        }
        switch (view.phase()) {
            case DIALING, INTER_DIALING, SYNC_AWAIT, ESTABLISHING -> {
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

    // A chevron at the top of the gate, its lights shining with their own light while it is locked; the parts that
    // move when the top chevron locks are moved by dip
    private static void submitChevron(final MilkyWayGateMesh mesh, final PoseStack poseStack,
            final SubmitNodeCollector collector, final int light, final boolean lit, final float dip) {
        final int metal = ARGB.white(1.0F);
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
                if (shining) {
                    MilkyWayGateRenderer.shine(collector, poseStack, texture, mesh.chevron(piece, m),
                            ARGB.white(1.0F));
                } else {
                    MilkyWayGateRenderer.draw(collector, poseStack, texture, mesh.chevron(piece, m),
                            MilkyWayGateRenderer.unlit(), light);
                }
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

    // Shining with its own light, as a locked chevron's lights do
    private static void shine(final SubmitNodeCollector collector, final PoseStack poseStack,
            final Identifier texture, final float[] triangles, final int color) {
        GateFeatureRenderer.submitEmissive(collector, poseStack.last(), texture, GateMesh.of(triangles), color);
    }

    private static Identifier texture(final String name) {
        return SGCraft.id("textures/gate/milkyway/" + name + ".png");
    }
}
