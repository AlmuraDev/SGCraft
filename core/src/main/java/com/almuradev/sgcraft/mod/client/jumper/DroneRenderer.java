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

package com.almuradev.sgcraft.mod.client.jumper;

import com.almuradev.sgcraft.mod.entity.DroneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

/**
 * An Ancient drone as clients draw it: a glowing golden mote, white hot at its heart, with a short tail of fading
 * glows behind it the way it flies, all light added to whatever lies behind them, facing the camera.
 */
public final class DroneRenderer extends EntityRenderer<DroneEntity, DroneRenderer.State> {

    // Its glow: the heart, and the halo round it, in blocks across, and its tail, how many glows long, how far apart,
    // as a share of how far it flies in a tick
    private static final float HEART = 0.35F;
    private static final float HALO = 1.1F;
    private static final int TAIL = 5;
    private static final float TAIL_STEP = 0.35F;
    private static final int[] HEART_COLORS = {0xFFFFFFF0, 0xF0FFE9A0, 0x80FFC040, 0x00FFA020};
    private static final int[] HALO_COLORS = {0x90FFD060, 0x60FFB030, 0x28E08010, 0x00C06000};

    public DroneRenderer(final EntityRendererProvider.Context context) {
        super(context);
    }

    /** What a drone is drawn from: which way, and how fast, it flies. */
    public static final class State extends EntityRenderState {
        public Vec3 motion = Vec3.ZERO;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(final DroneEntity entity, final State state, final float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.motion = entity.getDeltaMovement();
    }

    @Override
    public void submit(final State state, final PoseStack poseStack, final SubmitNodeCollector collector,
            final CameraRenderState camera) {
        final Vec3 back = state.motion.scale(-DroneRenderer.TAIL_STEP);
        for (int step = DroneRenderer.TAIL; step >= 0; step--) {
            final int at = step;
            final float fade = 1 - step / (DroneRenderer.TAIL + 1F);
            poseStack.pushPose();
            poseStack.translate(back.x * step, 0.2 + back.y * step, back.z * step);
            poseStack.rotate(camera.orientation);
            collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, buffer) -> {
                if (at == 0) {
                    JumperRenderer.disc(pose, buffer, DroneRenderer.HALO / 2, DroneRenderer.HALO_COLORS, 1);
                }
                JumperRenderer.disc(pose, buffer, DroneRenderer.HEART / 2 * (0.6F + 0.4F * fade),
                        DroneRenderer.HEART_COLORS, fade * fade);
            });
            poseStack.popPose();
        }
        super.submit(state, poseStack, collector, camera);
    }
}
