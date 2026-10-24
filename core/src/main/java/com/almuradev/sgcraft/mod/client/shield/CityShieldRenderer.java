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

import com.almuradev.sgcraft.mod.client.ViewCulling;
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

import java.util.HashMap;
import java.util.Map;

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
    // How many domes' shapes are kept at once
    private static final int GRIDS_KEPT = 8;

    // The points of each dome's shape lately drawn, worked out once for as long as it keeps that shape
    private final Map<Shape, Grid> grids = new HashMap<>();

    public CityShieldRenderer(final BlockEntityRendererProvider.Context context) {
    }

    // A dome's shape: its size, and how far round it has spread, from its foot and top, in radians
    private record Shape(int width, int above, int below, double edge) {
    }

    // The points of a dome's shape, row by row from its foot up, each row round from the same side, and the surface's
    // normal at each
    private static final class Grid {

        private final double[] lat = new double[CityShieldRenderer.STACKS + 1];
        private final double[] lon = new double[CityShieldRenderer.SLICES + 1];
        private final float[] points = new float[(CityShieldRenderer.STACKS + 1) * (CityShieldRenderer.SLICES + 1) * 3];
        private final float[] normals = new float[this.points.length];

        private Grid(final Shape shape) {
            for (int i = 0; i <= CityShieldRenderer.STACKS; i++) {
                this.lat[i] = -shape.edge() + 2 * shape.edge() * i / CityShieldRenderer.STACKS;
            }
            for (int j = 0; j <= CityShieldRenderer.SLICES; j++) {
                this.lon[j] = 2 * Math.PI * j / CityShieldRenderer.SLICES;
            }
            for (int i = 0; i <= CityShieldRenderer.STACKS; i++) {
                final double up = this.lat[i] >= 0 ? shape.above() : shape.below();
                for (int j = 0; j <= CityShieldRenderer.SLICES; j++) {
                    final int at = (i * (CityShieldRenderer.SLICES + 1) + j) * 3;
                    final double x = shape.width() * Math.cos(this.lat[i]) * Math.cos(this.lon[j]);
                    final double y = up * Math.sin(this.lat[i]);
                    final double z = shape.width() * Math.cos(this.lat[i]) * Math.sin(this.lon[j]);
                    final double nx = x / (shape.width() * shape.width());
                    final double ny = y / (up * up);
                    final double nz = z / (shape.width() * shape.width());
                    final double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
                    this.points[at] = (float) x;
                    this.points[at + 1] = (float) y;
                    this.points[at + 2] = (float) z;
                    this.normals[at] = (float) (nx / length);
                    this.normals[at + 1] = (float) (ny / length);
                    this.normals[at + 2] = (float) (nz / length);
                }
            }
        }
    }

    private Grid grid(final State state, final double edge) {
        final Shape shape = new Shape(state.width, state.above, state.below, edge);
        Grid grid = this.grids.get(shape);
        if (grid == null) {
            // A dome spreading or drawing back changes its shape each frame; once it stands, its one shape stays
            if (this.grids.size() >= CityShieldRenderer.GRIDS_KEPT) {
                this.grids.clear();
            }
            grid = new Grid(shape);
            this.grids.put(shape, grid);
        }
        return grid;
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
        final Grid grid = this.grid(state, edge);
        final int[] colors = CityShieldRenderer.colors(grid, eye, state.time, edge, band);
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        collector.submitCustomGeometry(poseStack, GateGlow.SHIELD_TYPE, (pose, buffer) -> {
            final int row = CityShieldRenderer.SLICES + 1;
            for (int i = 0; i < CityShieldRenderer.STACKS; i++) {
                for (int j = 0; j < CityShieldRenderer.SLICES; j++) {
                    CityShieldRenderer.corner(pose, buffer, grid, colors, i * row + j);
                    CityShieldRenderer.corner(pose, buffer, grid, colors, (i + 1) * row + j);
                    CityShieldRenderer.corner(pose, buffer, grid, colors, (i + 1) * row + j + 1);
                    CityShieldRenderer.corner(pose, buffer, grid, colors, i * row + j + 1);
                }
            }
        });
        poseStack.popPose();
    }

    // Each point's colour this frame: brighter where the dome is seen edge on, shimmering in waves that run up it and
    // round it, and bright along its edge while it spreads
    private static int[] colors(final Grid grid, final Vec3 eye, final double time, final double edge,
            final double band) {
        final double[] rows = new double[grid.lat.length];
        final double[] spreading = new double[grid.lat.length];
        for (int i = 0; i < grid.lat.length; i++) {
            rows[i] = 0.25 * Math.sin(time * 0.05 - grid.lat[i] * 6);
            final double off = (edge - Math.abs(grid.lat[i])) / 0.12;
            spreading[i] = band * Math.exp(-off * off);
        }
        final double[] columns = new double[grid.lon.length];
        for (int j = 0; j < grid.lon.length; j++) {
            columns[j] = 0.25 * Math.sin(time * 0.03 + grid.lon[j] * 5);
        }
        final int[] colors = new int[grid.lat.length * grid.lon.length];
        for (int i = 0; i < grid.lat.length; i++) {
            for (int j = 0; j < grid.lon.length; j++) {
                final int at = i * grid.lon.length + j;
                final double dx = eye.x - grid.points[at * 3];
                final double dy = eye.y - grid.points[at * 3 + 1];
                final double dz = eye.z - grid.points[at * 3 + 2];
                final double toEye = Math.sqrt(dx * dx + dy * dy + dz * dz);
                final double facing = toEye == 0 ? 1 : Math.abs(grid.normals[at * 3] * dx
                        + grid.normals[at * 3 + 1] * dy + grid.normals[at * 3 + 2] * dz) / toEye;
                final double rim = (1 - facing) * (1 - facing) * (1 - facing);
                final double shimmer = 0.5 + rows[i] + columns[j];
                final double alpha = Math.min(1, (0.22 + (0.6 - 0.22) * rim) * (0.6 + 0.4 * shimmer)
                        + 0.75 * spreading[i]);
                final float mix = (float) Math.min(1, rim + spreading[i]);
                colors[at] = ARGB.colorFromFloat(1.0F, CityShieldRenderer.channel(0, alpha, mix),
                        CityShieldRenderer.channel(1, alpha, mix), CityShieldRenderer.channel(2, alpha, mix));
            }
        }
        return colors;
    }

    // One of red, green and blue: the base colour drawn towards the rim's, as bright as the light is strong
    private static float channel(final int channel, final double alpha, final float mix) {
        return (float) (alpha * (CityShieldRenderer.BASE[channel]
                + (CityShieldRenderer.RIM[channel] - CityShieldRenderer.BASE[channel]) * mix));
    }

    private static void corner(final PoseStack.Pose pose, final VertexConsumer buffer, final Grid grid,
            final int[] colors, final int at) {
        buffer.addVertex(pose, grid.points[at * 3], grid.points[at * 3 + 1], grid.points[at * 3 + 2])
                .setColor(colors[at])
                .setUv(0.5F, 0.5F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, grid.normals[at * 3], grid.normals[at * 3 + 1], grid.normals[at * 3 + 2]);
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
                .closerThan(cameraPosition, reach + CityShieldRenderer.VIEW_PAST)
                && ViewCulling.visible(this.getRenderBoundingBox(shield));
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
