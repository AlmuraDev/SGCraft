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

package com.almuradev.sgcraft.mod.client.shield;

import com.almuradev.sgcraft.mod.client.gate.GateGlow;
import com.almuradev.sgcraft.mod.shield.CityShieldBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a city shield, as in SGCraft 2.2.0: a dome of blue light round its generator, brighter where it is seen edge
 * on, shimmering as it stands. As it rises it spreads up and over from the generator's level and down under it, its
 * spreading edge bright, and it draws back the same way as it falls. It is light added to whatever is behind it, so
 * it hides nothing.
 */
public final class CityShieldRenderer implements BlockEntityRenderer<CityShieldBlockEntity,
        CityShieldRenderer.State> {

    // How finely it is cut: bands from its foot to its top, and slices round it
    private static final int STACKS = 32;
    private static final int SLICES = 64;
    private static final float[] BASE = {0.30F, 0.58F, 0.95F};
    private static final float[] RIM = {0.75F, 0.9F, 1.0F};
    // How far past the shield it is still drawn, in blocks
    private static final int VIEW_PAST = 256;

    public CityShieldRenderer(final BlockEntityRendererProvider.Context context) {
    }

    /** What a shield is drawn from. */
    public static final class State extends BlockEntityRenderState {
        private float spread;
        private int width;
        private int above;
        private int below;
        private double time;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(final CityShieldBlockEntity shield, final State state, final float partialTicks,
            final Vec3 cameraPosition, final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(shield, state, partialTicks, cameraPosition, breakProgress);
        state.spread = shield.spread();
        state.width = shield.width();
        state.above = shield.above();
        state.below = shield.below();
        state.time = shield.getLevel() == null ? 0 : shield.getLevel().getGameTime() + partialTicks;
    }

    @Override
    public void submit(final State state, final PoseStack poseStack, final SubmitNodeCollector collector,
            final CameraRenderState camera) {
        if (state.spread <= 0) {
            return;
        }
        final double s = state.spread * state.spread * (3 - 2 * state.spread);
        final double edge = s * Math.PI / 2;
        final double band = Math.min(1, (1 - s) * 5);
        // The camera, from the middle of the generator, which the dome is drawn about
        final Vec3 eye = camera.pos.subtract(Vec3.atCenterOf(state.blockPos));
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        collector.submitCustomGeometry(poseStack, GateGlow.SHIELD_TYPE, (pose, buffer) -> {
            for (int i = 0; i < CityShieldRenderer.STACKS; i++) {
                final double lat0 = -edge + 2 * edge * i / CityShieldRenderer.STACKS;
                final double lat1 = -edge + 2 * edge * (i + 1) / CityShieldRenderer.STACKS;
                for (int j = 0; j < CityShieldRenderer.SLICES; j++) {
                    final double lon0 = 2 * Math.PI * j / CityShieldRenderer.SLICES;
                    final double lon1 = 2 * Math.PI * (j + 1) / CityShieldRenderer.SLICES;
                    CityShieldRenderer.corner(pose, buffer, state, eye, lat0, lon0, edge, band);
                    CityShieldRenderer.corner(pose, buffer, state, eye, lat1, lon0, edge, band);
                    CityShieldRenderer.corner(pose, buffer, state, eye, lat1, lon1, edge, band);
                    CityShieldRenderer.corner(pose, buffer, state, eye, lat0, lon1, edge, band);
                }
            }
        });
        poseStack.popPose();
    }

    private static void corner(final PoseStack.Pose pose, final VertexConsumer buffer, final State state,
            final Vec3 eye, final double lat, final double lon, final double edge, final double band) {
        final double up = lat >= 0 ? state.above : state.below;
        final double x = state.width * Math.cos(lat) * Math.cos(lon);
        final double y = up * Math.sin(lat);
        final double z = state.width * Math.cos(lat) * Math.sin(lon);
        // The surface's normal, and how edge on it is seen from the camera
        final Vec3 normal = new Vec3(x / (state.width * state.width), y / (up * up), z / (state.width * state.width))
                .normalize();
        final Vec3 toEye = eye.subtract(x, y, z).normalize();
        final double rim = Math.pow(1 - Math.abs(normal.dot(toEye)), 3);
        final double shimmer = 0.5 + 0.25 * Math.sin(state.time * 0.05 - lat * 6) + 0.25 * Math.sin(state.time * 0.03
                + lon * 5);
        final double spreading = band * Math.exp(-Math.pow((edge - Math.abs(lat)) / 0.12, 2));
        final double alpha = Math.min(1, (0.22 + (0.6 - 0.22) * rim) * (0.6 + 0.4 * shimmer) + 0.75 * spreading);
        final float mix = (float) Math.min(1, rim + spreading);
        final int color = ARGB.colorFromFloat(1.0F,
                (float) (alpha * (CityShieldRenderer.BASE[0] + (CityShieldRenderer.RIM[0] - CityShieldRenderer.BASE[0])
                        * mix)),
                (float) (alpha * (CityShieldRenderer.BASE[1] + (CityShieldRenderer.RIM[1] - CityShieldRenderer.BASE[1])
                        * mix)),
                (float) (alpha * (CityShieldRenderer.BASE[2] + (CityShieldRenderer.RIM[2] - CityShieldRenderer.BASE[2])
                        * mix)));
        buffer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(color)
                .setUv(0.5F, 0.5F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        // The dome reaches far beyond its generator, so it stays drawn while the generator is off screen
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256 + CityShieldRenderer.VIEW_PAST;
    }

    @Override
    public boolean shouldRender(final CityShieldBlockEntity shield, final Vec3 cameraPosition) {
        final int reach = Math.max(shield.width(), Math.max(shield.above(), shield.below()));
        return (shield.active() || shield.spread() > 0) && Vec3.atCenterOf(shield.getBlockPos())
                .closerThan(cameraPosition, reach + CityShieldRenderer.VIEW_PAST);
    }

    /**
     * The space the whole dome takes up, so it isn't culled while any of it is in view. NeoForge culls block entities
     * by this box; it isn't part of the vanilla interface, so there is no {@code @Override}.
     */
    public AABB getRenderBoundingBox(final CityShieldBlockEntity shield) {
        final Vec3 center = shield.center();
        return new AABB(center.x - shield.width(), center.y - shield.below(), center.z - shield.width(),
                center.x + shield.width(), center.y + shield.above(), center.z + shield.width());
    }
}
