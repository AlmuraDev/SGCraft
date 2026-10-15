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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the live feed on a MALP console's monitor while someone drives at it and the server streams the feed to this
 * client: the picture fills the monitor's screen, lit by its own light, faintly green, with scan lines and a thin
 * frame over it. The picture is asked for only while the screen faces the camera and is in view; otherwise, and
 * while there is no picture, the monitor shows its own idle screen.
 */
public final class MalpConsoleRenderer implements BlockEntityRenderer<MalpConsoleBlockEntity,
        MalpConsoleRenderer.State> {

    private static final Identifier OVERLAY = SGCraft.id("textures/misc/malp_monitor.png");
    // The monitor's screen, in a console facing north, where the model's screen texture shows inside its bezel: its
    // face is at the block's middle, facing north
    private static final float LEFT = 13.15F / 16;
    private static final float RIGHT = 2.85F / 16;
    private static final float BOTTOM = 10.1F / 16;
    private static final float TOP = 15.9F / 16;
    private static final float FACE = 8.0F / 16;
    private static final float CENTER_Y = (10.0F + 16.0F) / 2 / 16;
    // How far in front of the screen the picture and what is over it are drawn
    private static final float PICTURE_OFFSET = 0.002F;
    private static final float OVERLAY_OFFSET = 0.004F;
    private static final int TINT = 0xFFDCFFE6;
    private static final int WHITE = 0xFFFFFFFF;

    public MalpConsoleRenderer(final BlockEntityRendererProvider.Context context) {
    }

    /** What the console renderer needs to know about a console for one frame. */
    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        // The picture to show, or null to leave the monitor's own screen showing
        public @Nullable Identifier picture;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(final MalpConsoleBlockEntity console, final State state, final float partialTicks,
            final Vec3 cameraPosition, final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(console, state, partialTicks, cameraPosition, breakProgress);
        state.facing = console.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        state.picture = MalpConsoleRenderer.seen(console.getBlockPos(), state.facing, cameraPosition)
                ? RemoteViewDisplays.picture(console.getBlockPos(), cameraPosition) : null;
    }

    // Whether the camera is in front of the monitor's screen and the console is in view
    private static boolean seen(final BlockPos pos, final Direction facing, final Vec3 camera) {
        final Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + MalpConsoleRenderer.CENTER_Y, pos.getZ() + 0.5);
        final Vec3 toCamera = camera.subtract(center);
        if (toCamera.x * facing.getStepX() + toCamera.z * facing.getStepZ() <= 0) {
            return false;
        }
        return Minecraft.getInstance().gameRenderer.mainCamera().getCullFrustum().isVisible(new AABB(pos));
    }

    @Override
    public void submit(final State state, final PoseStack poseStack, final SubmitNodeCollector collector,
            final CameraRenderState camera) {
        final Identifier picture = state.picture;
        if (picture == null) {
            return;
        }
        poseStack.pushPose();
        // The model's own turn for each facing, from its north facing
        poseStack.translate(0.5, 0, 0.5);
        poseStack.rotateDegrees(Axis.YP, 180 - state.facing.toYRot());
        poseStack.translate(-0.5, 0, -0.5);
        // The picture is drawn upside down in its texture, as the GPU lays out what it draws
        collector.submitCustomGeometry(poseStack, RenderTypes.textPolygonOffset(picture), (pose, buffer) ->
                MalpConsoleRenderer.quad(pose, buffer, MalpConsoleRenderer.FACE - MalpConsoleRenderer.PICTURE_OFFSET,
                        MalpConsoleRenderer.TINT, 1, 0));
        collector.submitCustomGeometry(poseStack, RenderTypes.textPolygonOffset(MalpConsoleRenderer.OVERLAY),
                (pose, buffer) -> MalpConsoleRenderer.quad(pose, buffer,
                        MalpConsoleRenderer.FACE - MalpConsoleRenderer.OVERLAY_OFFSET, MalpConsoleRenderer.WHITE,
                        0, 1));
        poseStack.popPose();
    }

    // The screen's rectangle, facing north, at full brightness, its corners counterclockwise as seen from in front
    private static void quad(final PoseStack.Pose pose, final VertexConsumer buffer, final float z, final int color,
            final float topV, final float bottomV) {
        final int light = LightCoordsUtil.FULL_BRIGHT;
        buffer.addVertex(pose, MalpConsoleRenderer.LEFT, MalpConsoleRenderer.TOP, z).setColor(color).setUv(0, topV)
                .setLight(light);
        buffer.addVertex(pose, MalpConsoleRenderer.LEFT, MalpConsoleRenderer.BOTTOM, z).setColor(color)
                .setUv(0, bottomV).setLight(light);
        buffer.addVertex(pose, MalpConsoleRenderer.RIGHT, MalpConsoleRenderer.BOTTOM, z).setColor(color)
                .setUv(1, bottomV).setLight(light);
        buffer.addVertex(pose, MalpConsoleRenderer.RIGHT, MalpConsoleRenderer.TOP, z).setColor(color).setUv(1, topV)
                .setLight(light);
    }
}
