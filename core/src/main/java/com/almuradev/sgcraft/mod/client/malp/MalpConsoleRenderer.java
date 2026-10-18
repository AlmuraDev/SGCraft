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

package com.almuradev.sgcraft.mod.client.malp;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.block.entity.MalpConsoleBlockEntity;
import com.almuradev.sgcraft.mod.client.view.RemoteViewDisplays;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a MALP console's wide monitor, lit by its own light, as its screen looks to the player at it: the MALPs paired
 * with it on the left, and on the right the feed in its frame, with the MALP's name, readouts and warnings, as
 * {@link MalpMonitorDisplays} paints them. The feed shows the live picture while the server streams it to this
 * client, faintly green, with scan lines over it, and static otherwise. Switched off, the monitor is dark. It is drawn
 * only while its screen faces the camera, is in view and is near enough; otherwise the monitor shows its own idle
 * screen.
 */
public final class MalpConsoleRenderer implements BlockEntityRenderer<MalpConsoleBlockEntity,
        MalpConsoleRenderer.State> {

    private static final Identifier OVERLAY = SGCraft.id("textures/misc/malp_monitor.png");
    private static final Identifier STATIC = SGCraft.id("textures/gui/malp_static.png");
    // The monitor's screen, in the middle lower part's space with the console facing north: from the operator's
    // left, a block east, to their right, a block west, and from just above the desk to near the top of the upper
    // row, its face at the middle of the blocks, facing north
    private static final float LEFT = 2 - 1 / 16F;
    private static final float RIGHT = -1 + 1 / 16F;
    private static final float BOTTOM = 13.5F / 16;
    private static final float TOP = 1 + 14.5F / 16;
    private static final float FACE = 8.0F / 16;
    private static final float CENTER_Y = (MalpConsoleRenderer.TOP + MalpConsoleRenderer.BOTTOM) / 2;
    // How far in front of the screen the feed, the scan lines over it, and the painted screen over all are drawn
    private static final float FEED_OFFSET = 0.002F;
    private static final float OVERLAY_OFFSET = 0.003F;
    private static final float SCREEN_OFFSET = 0.004F;
    // How near a player must be for the monitor to be drawn
    private static final double VIEW_DISTANCE = 48;
    // How much of the static texture shows on the feed at once, moved about each frame
    private static final float STATIC_SPAN_U = 0.75F;
    private static final float STATIC_SPAN_V = 0.42F;
    private static final int TINT = 0xFFDCFFE6;
    private static final int STATIC_TINT = 0xFFB0D8C8;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;

    private final RandomSource noise = RandomSource.create();

    public MalpConsoleRenderer(final BlockEntityRendererProvider.Context context) {
    }

    /** What the console renderer needs to know about a console for one frame. */
    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        // Whether the monitor is drawn at all, and whether it is running
        public boolean drawn;
        public boolean on;
        // The picture to show on the feed, or null for static
        public @Nullable Identifier picture;
        // The painted screen over it all
        public @Nullable Identifier screen;
        // Whether the console's feed is offline, or it has no MALPs, so its feed is plain dark rather than static
        public boolean feedDark;
        // Where on the static texture the static starts this frame
        public float staticU;
        public float staticV;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(final MalpConsoleBlockEntity console, final State state, final float partialTicks,
            final Vec3 cameraPosition, final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(console, state, partialTicks, cameraPosition, breakProgress);
        final BlockPos pos = console.getBlockPos();
        state.facing = console.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        state.on = console.running();
        state.drawn = Vec3.atCenterOf(pos).closerThan(cameraPosition, MalpConsoleRenderer.VIEW_DISTANCE)
                && MalpConsoleRenderer.seen(pos, state.facing, cameraPosition);
        state.picture = state.drawn && state.on ? RemoteViewDisplays.picture(pos, cameraPosition) : null;
        state.screen = state.drawn && state.on ? MalpMonitorDisplays.texture(pos, state.picture != null) : null;
        state.feedDark = MalpMonitorDisplays.feedDark(pos);
        state.staticU = this.noise.nextFloat() * (1 - MalpConsoleRenderer.STATIC_SPAN_U);
        state.staticV = this.noise.nextFloat() * (1 - MalpConsoleRenderer.STATIC_SPAN_V);
    }

    // Whether the camera is in front of the monitor's screen and any of the screen is in view
    private static boolean seen(final BlockPos pos, final Direction facing, final Vec3 camera) {
        final Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + MalpConsoleRenderer.CENTER_Y, pos.getZ() + 0.5);
        final Vec3 toCamera = camera.subtract(center);
        if (toCamera.x * facing.getStepX() + toCamera.z * facing.getStepZ() <= 0) {
            return false;
        }
        return Minecraft.getInstance().gameRenderer.mainCamera().getCullFrustum().isVisible(
                MalpConsoleRenderer.screen(pos));
    }

    // The space the screen can take up, whichever way the console faces: a block either side of the middle lower
    // part and up through the upper row
    private static AABB screen(final BlockPos pos) {
        return new AABB(pos).inflate(1, 0, 1).expandTowards(0, 1, 0);
    }

    // The screen reaches a block either side of the block entity, past the block the game culls it by
    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    /**
     * The space the whole screen can take up, so it isn't culled while any of it is in view. NeoForge culls block
     * entities by this box; it isn't part of the vanilla interface, so there is no {@code @Override}.
     */
    public AABB getRenderBoundingBox(final MalpConsoleBlockEntity console) {
        return MalpConsoleRenderer.screen(console.getBlockPos());
    }

    @Override
    public void submit(final State state, final PoseStack poseStack, final SubmitNodeCollector collector,
            final CameraRenderState camera) {
        if (!state.drawn) {
            return;
        }
        poseStack.pushPose();
        // The model's own turn for each facing, from its north facing
        poseStack.translate(0.5, 0, 0.5);
        poseStack.rotateDegrees(Axis.YP, 180 - state.facing.toYRot());
        poseStack.translate(-0.5, 0, -0.5);
        final Identifier screen = state.screen;
        if (!state.on || screen == null) {
            // A dark screen, all of one black
            collector.submitCustomGeometry(poseStack, RenderTypes.textPolygonOffset(MalpConsoleRenderer.STATIC),
                    (pose, buffer) -> MalpConsoleRenderer.quad(pose, buffer, 0, 0, MalpMonitorDisplays.WIDTH,
                            MalpMonitorDisplays.HEIGHT, MalpConsoleRenderer.SCREEN_OFFSET, MalpConsoleRenderer.BLACK,
                            0, 0, 0, 0));
            poseStack.popPose();
            return;
        }
        final Identifier picture = state.picture;
        if (picture != null) {
            // The picture is drawn upside down in its texture, as the GPU lays out what it draws
            collector.submitCustomGeometry(poseStack, RenderTypes.textPolygonOffset(picture), (pose, buffer) ->
                    MalpConsoleRenderer.feed(pose, buffer, MalpConsoleRenderer.FEED_OFFSET, MalpConsoleRenderer.TINT,
                            0, 1, 1, 0));
        } else if (state.feedDark) {
            collector.submitCustomGeometry(poseStack, RenderTypes.textPolygonOffset(MalpConsoleRenderer.STATIC),
                    (pose, buffer) -> MalpConsoleRenderer.feed(pose, buffer, MalpConsoleRenderer.FEED_OFFSET,
                            MalpConsoleRenderer.BLACK, 0, 0, 0, 0));
        } else {
            final float u = state.staticU;
            final float v = state.staticV;
            collector.submitCustomGeometry(poseStack, RenderTypes.textPolygonOffset(MalpConsoleRenderer.STATIC),
                    (pose, buffer) -> MalpConsoleRenderer.feed(pose, buffer, MalpConsoleRenderer.FEED_OFFSET,
                            MalpConsoleRenderer.STATIC_TINT, u, u + MalpConsoleRenderer.STATIC_SPAN_U, v,
                            v + MalpConsoleRenderer.STATIC_SPAN_V));
        }
        collector.submitCustomGeometry(poseStack, RenderTypes.textPolygonOffset(MalpConsoleRenderer.OVERLAY),
                (pose, buffer) -> MalpConsoleRenderer.feed(pose, buffer, MalpConsoleRenderer.OVERLAY_OFFSET,
                        MalpConsoleRenderer.WHITE, 0, 1, 0, 1));
        collector.submitCustomGeometry(poseStack, RenderTypes.textPolygonOffset(screen),
                (pose, buffer) -> MalpConsoleRenderer.quad(pose, buffer, 0, 0, MalpMonitorDisplays.WIDTH,
                        MalpMonitorDisplays.HEIGHT, MalpConsoleRenderer.SCREEN_OFFSET, MalpConsoleRenderer.WHITE,
                        0, 1, 0, 1));
        poseStack.popPose();
    }

    // The feed's rectangle on the screen
    private static void feed(final PoseStack.Pose pose, final VertexConsumer buffer, final float offset,
            final int color, final float leftU, final float rightU, final float topV, final float bottomV) {
        MalpConsoleRenderer.quad(pose, buffer, MalpMonitorDisplays.FEED_LEFT, MalpMonitorDisplays.FEED_TOP,
                MalpMonitorDisplays.FEED_RIGHT, MalpMonitorDisplays.FEED_BOTTOM, offset, color, leftU, rightU, topV,
                bottomV);
    }

    // A rectangle on the screen, by where it is in the painted screen's pixels, facing north, at full brightness,
    // its corners counterclockwise as seen from in front
    private static void quad(final PoseStack.Pose pose, final VertexConsumer buffer, final int left, final int top,
            final int right, final int bottom, final float offset, final int color, final float leftU,
            final float rightU, final float topV, final float bottomV) {
        final int light = LightCoordsUtil.FULL_BRIGHT;
        final float z = MalpConsoleRenderer.FACE - offset;
        final float x0 = MalpConsoleRenderer.x(left);
        final float x1 = MalpConsoleRenderer.x(right);
        final float y0 = MalpConsoleRenderer.y(top);
        final float y1 = MalpConsoleRenderer.y(bottom);
        buffer.addVertex(pose, x0, y0, z).setColor(color).setUv(leftU, topV).setLight(light);
        buffer.addVertex(pose, x0, y1, z).setColor(color).setUv(leftU, bottomV).setLight(light);
        buffer.addVertex(pose, x1, y1, z).setColor(color).setUv(rightU, bottomV).setLight(light);
        buffer.addVertex(pose, x1, y0, z).setColor(color).setUv(rightU, topV).setLight(light);
    }

    // Where a column and a row of the painted screen's pixels are on the monitor
    private static float x(final int pixel) {
        return MalpConsoleRenderer.LEFT
                - (MalpConsoleRenderer.LEFT - MalpConsoleRenderer.RIGHT) * pixel / MalpMonitorDisplays.WIDTH;
    }

    private static float y(final int pixel) {
        return MalpConsoleRenderer.TOP
                - (MalpConsoleRenderer.TOP - MalpConsoleRenderer.BOTTOM) * pixel / MalpMonitorDisplays.HEIGHT;
    }
}
