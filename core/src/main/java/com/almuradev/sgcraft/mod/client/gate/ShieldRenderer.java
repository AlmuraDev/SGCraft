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

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;

/**
 * Draws a gate's shield as the original did: a disc of blue light across the ring's opening, brighter toward its rim,
 * shimmering as it stands, added to whatever is behind it so the horizon shows through. It fades in as the shield
 * rises and out as it falls.
 */
public final class ShieldRenderer {

    // How far the disc reaches past the ring's opening, so it meets the ring, and how far in front of the gate it lies
    private static final double OVERLAP = 0.05;
    private static final double DEPTH = 0.1;
    // How finely it is cut: rings out from the center, and spokes round it
    private static final int RINGS = 12;
    private static final int SPOKES = 48;
    // Its color at the center and at the rim, and how bright it is at each
    private static final float[] CENTER = {0.35F, 0.65F, 1.0F};
    private static final float[] RIM = {0.75F, 0.92F, 1.0F};
    private static final float CENTER_ALPHA = 0.45F;
    private static final float RIM_ALPHA = 0.75F;

    private ShieldRenderer() {
    }

    /**
     * Draws the shield.
     *
     * @param opening the radius of the ring's opening, in the gate's frame
     * @param aperture how far it has fallen, from 0 raised to 1 lowered; a lowered shield isn't drawn
     * @param time the game time, in ticks, with the part tick, which moves its shimmer
     */
    public static void submit(final double opening, final float aperture, final double time,
            final PoseStack poseStack, final SubmitNodeCollector collector) {
        final float fade = 1 - aperture;
        if (fade <= 0) {
            return;
        }
        final double radius = opening + ShieldRenderer.OVERLAP;
        collector.submitCustomGeometry(poseStack, GateGlow.SHIELD_TYPE, (pose, buffer) -> {
            for (int ring = 0; ring < ShieldRenderer.RINGS; ring++) {
                for (int spoke = 0; spoke < ShieldRenderer.SPOKES; spoke++) {
                    ShieldRenderer.corner(pose, buffer, radius, ring, spoke, time, fade);
                    ShieldRenderer.corner(pose, buffer, radius, ring + 1, spoke, time, fade);
                    ShieldRenderer.corner(pose, buffer, radius, ring + 1, spoke + 1, time, fade);
                    ShieldRenderer.corner(pose, buffer, radius, ring, spoke + 1, time, fade);
                }
            }
        });
    }

    private static void corner(final PoseStack.Pose pose, final VertexConsumer buffer, final double radius,
            final int ring, final int spoke, final double time, final float fade) {
        final double f = (double) ring / ShieldRenderer.RINGS;
        final double angle = 2 * Math.PI * spoke / ShieldRenderer.SPOKES;
        final double rim = f * f * f * f;
        final double shimmer = 0.5 + 0.25 * Math.sin(time * 0.12 - f * 9) + 0.25 * Math.sin(time * 0.07 + angle * 3
                + f * 4);
        final double alpha = (ShieldRenderer.CENTER_ALPHA + (ShieldRenderer.RIM_ALPHA - ShieldRenderer.CENTER_ALPHA)
                * rim) * (0.75 + 0.25 * shimmer) * fade;
        // Added to what is behind it, so its brightness is its color times how much of it shows
        final int color = ARGB.colorFromFloat(1.0F,
                (float) (alpha * (ShieldRenderer.CENTER[0] + (ShieldRenderer.RIM[0] - ShieldRenderer.CENTER[0]) * rim)),
                (float) (alpha * (ShieldRenderer.CENTER[1] + (ShieldRenderer.RIM[1] - ShieldRenderer.CENTER[1]) * rim)),
                (float) (alpha * (ShieldRenderer.CENTER[2] + (ShieldRenderer.RIM[2] - ShieldRenderer.CENTER[2])
                        * rim)));
        final double r = radius * f;
        final float x = (float) (r * Math.cos(angle));
        final float y = (float) (r * Math.sin(angle));
        buffer.addVertex(pose, x, y, (float) ShieldRenderer.DEPTH)
                .setColor(color)
                .setUv(0.5F, 0.5F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, 0, 0, 1);
    }
}
