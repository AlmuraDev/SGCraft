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

package com.almuradev.sgcraft.mod.client.rings;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.client.ViewCulling;
import com.almuradev.sgcraft.mod.client.gate.GateGeometry;
import com.almuradev.sgcraft.mod.rings.RingPlatformBlockEntity;
import com.almuradev.sgcraft.mod.rings.Rings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a ring platform's five rings, as 2.2.0's did. At rest they lie sunk in the floor around the platform, a lip
 * showing, or in the ceiling. Set off, they rise one after another, the first highest, and stack around those inside
 * (a ceiling platform's drop, the first to the floor); a column of white light fills them as those inside leave, or
 * arrive, then they go back one at a time, the one nearest its resting place first. Each platform runs in its own half
 * of a transport: the receiver's rings rise once the sender's are home.
 */
public final class RingPlatformRenderer implements BlockEntityRenderer<RingPlatformBlockEntity,
        RingPlatformRenderer.State> {

    private static final Identifier TOP = SGCraft.id("textures/block/ring_top.png");
    private static final Identifier SIDE = SGCraft.id("textures/block/ring_side.png");

    private static final int RINGS = 5;
    private static final int SEGMENTS = 36;
    // Each ring's size, and how far apart they stack, in blocks; how far the resting rings stand out of the floor
    private static final double OUTER = 2.45;
    private static final double INNER = 2.05;
    private static final double HEIGHT = 0.35;
    private static final double SPACING = 0.5;
    private static final double REST_LIP = 0.05;
    // Ticks between rings setting off, and each one's travel; when they start back, once the light has gone, the gap
    // between them going back and each one's travel back, the last of them home as the platform's half ends
    private static final int LAUNCH_GAP = 12;
    private static final int TRAVEL = 16;
    private static final int RETURN_START = Rings.LIGHT_END_TICK;
    private static final int RETURN_GAP = 5;
    private static final int RETURN_TRAVEL = 12;
    // The light, as on the show: a column of solid white lines snapping on a quarter of a second after the last ring
    // locks, holding, and fading before the rings drop; the rims' glow gone as they move off
    private static final int LIGHT_START = Rings.DEPART_TICK;
    private static final int LIGHT_ON = RingPlatformRenderer.LIGHT_START + 2;
    private static final int AFTERGLOW_END = RingPlatformRenderer.RETURN_START + 8;
    // How long the lines take to shoot up from the floor to the top ring, in ticks
    private static final double RISE = 4;

    // One ring with its bottom at 0: its top and bottom faces, and its outside and inside walls
    private static final float[] FACES = RingPlatformRenderer.faces();
    private static final float[] WALLS = RingPlatformRenderer.walls();

    public RingPlatformRenderer(final BlockEntityRendererProvider.Context context) {
    }

    /** What the renderer needs to know about a platform for one frame. */
    public static final class State extends BlockEntityRenderState {
        public boolean ceiling;
        // Ticks since its rings set off in its own half, or -1 at rest
        public double ticks = -1;
        // The floor its rings stack on, from the platform's own block
        public double floor;
        public int floorLight;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(final RingPlatformBlockEntity platform, final State state, final float partialTicks,
            final Vec3 cameraPosition, final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(platform, state, partialTicks, cameraPosition, breakProgress);
        final Level level = platform.getLevel();
        final BlockPos pos = platform.getBlockPos();
        state.ceiling = platform.ceiling();
        final double ticks = level == null ? -1 : level.getGameTime() - platform.startTime() + partialTicks;
        // At rest outside its own half: a receiver waits for the sender's to end
        state.ticks = ticks >= 0 && ticks <= Rings.HALF_TICKS ? ticks : -1;
        state.floor = (state.ticks >= 0 ? platform.runFloor() : platform.findFloor()) - pos.getY();
        // Lit as the floor inside the rings is
        final BlockPos lit = BlockPos.containing(pos.getX(), pos.getY() + (state.ceiling ? state.floor : 1),
                pos.getZ());
        state.floorLight = level == null ? state.lightCoords : LightCoordsUtil.getLightCoords(level, lit);
    }

    // Its rings reach far past its block
    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    // Culled by its whole box, on Fabric too
    @Override
    public boolean shouldRender(final RingPlatformBlockEntity blockEntity, final Vec3 cameraPosition) {
        return BlockEntityRenderer.super.shouldRender(blockEntity, cameraPosition)
                && ViewCulling.visible(this.getRenderBoundingBox(blockEntity));
    }

    /**
     * The space its rings move in, so they aren't culled while in view. NeoForge culls block entities by this box; it
     * isn't part of the vanilla interface, so there is no {@code @Override}.
     */
    public AABB getRenderBoundingBox(final RingPlatformBlockEntity platform) {
        final BlockPos pos = platform.getBlockPos();
        final boolean ceiling = platform.ceiling();
        return new AABB(pos.getX() - 3, pos.getY() - (ceiling ? Rings.MAX_DROP + 1 : 0), pos.getZ() - 3,
                pos.getX() + 4, pos.getY() + (ceiling ? 1 : 4), pos.getZ() + 4);
    }

    @Override
    public void submit(final State state, final PoseStack poseStack, final SubmitNodeCollector collector,
            final CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        final double rest = state.ceiling ? -RingPlatformRenderer.REST_LIP
                : 1 + RingPlatformRenderer.REST_LIP - RingPlatformRenderer.HEIGHT;
        final double t = state.ticks;
        if (t < 0) {
            // At rest they lie together, so one shows
            RingPlatformRenderer.submitRing(poseStack, collector, rest, state.floorLight);
        } else {
            // In the column of light the rings shine as it does
            final double column = RingPlatformRenderer.columnStrength(t);
            final int light = RingPlatformRenderer.brighten(state.floorLight, column);
            for (int k = 0; k < RingPlatformRenderer.RINGS; k++) {
                // A floor platform's first ring goes highest; a ceiling platform's first lands lowest
                final double stacked = state.floor + (state.ceiling ? k : RingPlatformRenderer.RINGS - 1 - k)
                        * RingPlatformRenderer.SPACING;
                final double go = RingPlatformRenderer.ease((t - k * RingPlatformRenderer.LAUNCH_GAP)
                        / RingPlatformRenderer.TRAVEL);
                final double back = RingPlatformRenderer.ease((t - RingPlatformRenderer.RETURN_START
                        - (RingPlatformRenderer.RINGS - 1 - k) * RingPlatformRenderer.RETURN_GAP)
                        / RingPlatformRenderer.RETURN_TRAVEL);
                RingPlatformRenderer.submitRing(poseStack, collector, rest + (stacked - rest) * go * (1 - back),
                        light);
            }
            if (t >= RingPlatformRenderer.LIGHT_START && t <= RingPlatformRenderer.AFTERGLOW_END) {
                RingPlatformRenderer.submitLight(poseStack, collector, t, state.floor);
            }
        }
        poseStack.popPose();
    }

    private static void submitRing(final PoseStack poseStack, final SubmitNodeCollector collector, final double y,
            final int light) {
        poseStack.pushPose();
        poseStack.translate(0, y, 0);
        final int white = ARGB.white(1.0F);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(RingPlatformRenderer.TOP),
                (pose, buffer) -> GateGeometry.emit(pose, buffer, RingPlatformRenderer.FACES, white, light));
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(RingPlatformRenderer.SIDE),
                (pose, buffer) -> GateGeometry.emit(pose, buffer, RingPlatformRenderer.WALLS, white, light));
        poseStack.popPose();
    }

    // Light a share of the way from what it is to full
    private static int brighten(final int light, final double share) {
        if (share <= 0) {
            return light;
        }
        final int block = LightCoordsUtil.block(light);
        final int sky = LightCoordsUtil.sky(light);
        return LightCoordsUtil.pack((int) Math.round(block + (15 - block) * share),
                (int) Math.round(sky + (15 - sky) * share));
    }

    private static double ease(final double f) {
        final double x = Math.max(0, Math.min(1, f));
        return x * x * (3 - 2 * x);
    }

    // How bright the column of light inside the rings is, from 0 to 1: snapping on, holding, fading before the rings
    // drop
    private static double columnStrength(final double t) {
        if (t < RingPlatformRenderer.LIGHT_START || t > Rings.LIGHT_END_TICK) {
            return 0;
        }
        if (t < RingPlatformRenderer.LIGHT_ON) {
            return RingPlatformRenderer.ease((t - RingPlatformRenderer.LIGHT_START)
                    / (RingPlatformRenderer.LIGHT_ON - RingPlatformRenderer.LIGHT_START));
        }
        return t <= Rings.LIGHT_FADE_TICK ? 1 : 1 - RingPlatformRenderer.ease((t - Rings.LIGHT_FADE_TICK)
                / (Rings.LIGHT_END_TICK - Rings.LIGHT_FADE_TICK));
    }

    // The transport's light, as on the show: the rings' inner rims shining a steady white, and a column of solid white
    // lines filling them, from the floor to the top ring
    private static void submitLight(final PoseStack poseStack, final SubmitNodeCollector collector, final double t,
            final double floor) {
        final double column = RingPlatformRenderer.columnStrength(t);
        // The rims: on with the column, steady until the rings drop, then fading
        final double edge = t < RingPlatformRenderer.LIGHT_ON
                ? RingPlatformRenderer.ease((t - RingPlatformRenderer.LIGHT_START)
                        / (RingPlatformRenderer.LIGHT_ON - RingPlatformRenderer.LIGHT_START))
                : t <= RingPlatformRenderer.RETURN_START ? 1
                        : 1 - RingPlatformRenderer.ease((t - RingPlatformRenderer.RETURN_START)
                                / (RingPlatformRenderer.AFTERGLOW_END - RingPlatformRenderer.RETURN_START));
        final double top = floor + (RingPlatformRenderer.RINGS - 1) * RingPlatformRenderer.SPACING
                + RingPlatformRenderer.HEIGHT;
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, buffer) -> {
            // Along each ring's inner edge, and in the gaps between them: a band of bluish white light, brightest at
            // its middle, steady
            if (edge > 0) {
                final float alpha = (float) (0.55 * edge);
                final double radius = RingPlatformRenderer.INNER - 0.03;
                for (int k = 0; k < RingPlatformRenderer.RINGS; k++) {
                    final double y0 = floor + k * RingPlatformRenderer.SPACING;
                    final double y1 = y0 + RingPlatformRenderer.SPACING;
                    for (int i = 0; i < RingPlatformRenderer.SEGMENTS; i++) {
                        RingPlatformRenderer.band(pose, buffer, i, radius, y0, y1, 0.85F, 0.93F, 1, 0, alpha);
                    }
                }
            }
            // The column: solid white lines shooting up from the floor to the top ring, no higher, in nested circles
            // that fill the rings; each line evenly bright from end to end, some brighter than others, shimmering a
            // little. Drawn adding light, so where they overlap they are pure white.
            if (column > 0) {
                final double rise = RingPlatformRenderer.ease((t - RingPlatformRenderer.LIGHT_START)
                        / RingPlatformRenderer.RISE);
                final double y1 = floor + (top - floor) * rise;
                for (int layer = 0; layer < 4; layer++) {
                    final double radius = (RingPlatformRenderer.INNER - 0.08) * (0.2 + 0.8 * layer / 3.0);
                    for (int i = 0; i < RingPlatformRenderer.SEGMENTS; i++) {
                        final double line = 0.5 + 0.5 * Math.sin(i * 2.7 + layer * 1.9);
                        final double shimmer = 0.85 + 0.15 * Math.sin(t * 1.1 + i * 0.9 + layer);
                        final float alpha = (float) (column * (0.45 + 0.55 * line) * shimmer);
                        RingPlatformRenderer.band(pose, buffer, i, radius, floor, y1, 1, 1, 1, alpha, alpha);
                    }
                }
            }
        });
    }

    // One segment of a wall of light from y0 to y1, as two quads meeting at its middle: faint at the ends, bright there
    private static void band(final PoseStack.Pose pose, final VertexConsumer buffer, final int segment,
            final double radius, final double y0, final double y1, final float r, final float g, final float b,
            final float endAlpha, final float midAlpha) {
        final double a0 = 2 * Math.PI * segment / RingPlatformRenderer.SEGMENTS;
        final double a1 = 2 * Math.PI * (segment + 1) / RingPlatformRenderer.SEGMENTS;
        final float x0 = (float) (radius * Math.cos(a0));
        final float z0 = (float) (radius * Math.sin(a0));
        final float x1 = (float) (radius * Math.cos(a1));
        final float z1 = (float) (radius * Math.sin(a1));
        final float lo = (float) y0;
        final float mid = (float) ((y0 + y1) / 2);
        final float hi = (float) y1;
        buffer.addVertex(pose, x0, lo, z0).setColor(r, g, b, endAlpha);
        buffer.addVertex(pose, x1, lo, z1).setColor(r, g, b, endAlpha);
        buffer.addVertex(pose, x1, mid, z1).setColor(r, g, b, midAlpha);
        buffer.addVertex(pose, x0, mid, z0).setColor(r, g, b, midAlpha);
        buffer.addVertex(pose, x0, mid, z0).setColor(r, g, b, midAlpha);
        buffer.addVertex(pose, x1, mid, z1).setColor(r, g, b, midAlpha);
        buffer.addVertex(pose, x1, hi, z1).setColor(r, g, b, endAlpha);
        buffer.addVertex(pose, x0, hi, z0).setColor(r, g, b, endAlpha);
    }

    // A ring's top and bottom: each of its 36 segments with its own panel of the texture, across the ring's width
    // and along it
    private static float[] faces() {
        final GateGeometry.Builder builder = new GateGeometry.Builder();
        final double y = RingPlatformRenderer.HEIGHT;
        for (int i = 0; i < RingPlatformRenderer.SEGMENTS; i++) {
            final double[] a = RingPlatformRenderer.corners(i);
            // Top, facing up
            RingPlatformRenderer.quad(builder, 0, 1, 0,
                    RingPlatformRenderer.INNER * a[0], y, RingPlatformRenderer.INNER * a[1], 0, 0,
                    RingPlatformRenderer.INNER * a[2], y, RingPlatformRenderer.INNER * a[3], 1, 0,
                    RingPlatformRenderer.OUTER * a[2], y, RingPlatformRenderer.OUTER * a[3], 1, 1,
                    RingPlatformRenderer.OUTER * a[0], y, RingPlatformRenderer.OUTER * a[1], 0, 1);
            // Bottom, facing down
            RingPlatformRenderer.quad(builder, 0, -1, 0,
                    RingPlatformRenderer.OUTER * a[0], 0, RingPlatformRenderer.OUTER * a[1], 0, 1,
                    RingPlatformRenderer.OUTER * a[2], 0, RingPlatformRenderer.OUTER * a[3], 1, 1,
                    RingPlatformRenderer.INNER * a[2], 0, RingPlatformRenderer.INNER * a[3], 1, 0,
                    RingPlatformRenderer.INNER * a[0], 0, RingPlatformRenderer.INNER * a[1], 0, 0);
        }
        return builder.build();
    }

    // A ring's outside and inside walls
    private static float[] walls() {
        final GateGeometry.Builder builder = new GateGeometry.Builder();
        final double y = RingPlatformRenderer.HEIGHT;
        for (int i = 0; i < RingPlatformRenderer.SEGMENTS; i++) {
            final double[] a = RingPlatformRenderer.corners(i);
            final double mid = 2 * Math.PI * (i + 0.5) / RingPlatformRenderer.SEGMENTS;
            final double nx = Math.cos(mid);
            final double nz = Math.sin(mid);
            final double o = RingPlatformRenderer.OUTER;
            final double n = RingPlatformRenderer.INNER;
            RingPlatformRenderer.quad(builder, nx, 0, nz,
                    o * a[0], 0, o * a[1], 0, 1, o * a[0], y, o * a[1], 0, 0,
                    o * a[2], y, o * a[3], 1, 0, o * a[2], 0, o * a[3], 1, 1);
            RingPlatformRenderer.quad(builder, -nx, 0, -nz,
                    n * a[2], 0, n * a[3], 0, 1, n * a[2], y, n * a[3], 0, 0,
                    n * a[0], y, n * a[1], 1, 0, n * a[0], 0, n * a[1], 1, 1);
        }
        return builder.build();
    }

    // The cosine and sine of a segment's two edges
    private static double[] corners(final int segment) {
        final double a0 = 2 * Math.PI * segment / RingPlatformRenderer.SEGMENTS;
        final double a1 = 2 * Math.PI * (segment + 1) / RingPlatformRenderer.SEGMENTS;
        return new double[] {Math.cos(a0), Math.sin(a0), Math.cos(a1), Math.sin(a1)};
    }

    // A quad as two triangles, all four corners sharing one normal
    private static void quad(final GateGeometry.Builder builder, final double nx, final double ny, final double nz,
            final double x0, final double y0, final double z0, final double u0, final double v0,
            final double x1, final double y1, final double z1, final double u1, final double v1,
            final double x2, final double y2, final double z2, final double u2, final double v2,
            final double x3, final double y3, final double z3, final double u3, final double v3) {
        builder.corner(x0, y0, z0, u0, v0, nx, ny, nz).corner(x1, y1, z1, u1, v1, nx, ny, nz)
                .corner(x2, y2, z2, u2, v2, nx, ny, nz);
        builder.corner(x0, y0, z0, u0, v0, nx, ny, nz).corner(x2, y2, z2, u2, v2, nx, ny, nz)
                .corner(x3, y3, z3, u3, v3, nx, ny, nz);
    }
}
