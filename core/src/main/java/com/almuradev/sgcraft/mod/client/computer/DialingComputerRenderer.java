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

package com.almuradev.sgcraft.mod.client.computer;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.block.entity.DialingComputerBlockEntity;
import com.almuradev.sgcraft.mod.client.ViewCulling;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the dialing program on a dialing computer's monitor, lit by its own light, for every player who can see it,
 * so those standing behind the operator watch the gate dial too. The picture spans the monitor's screen across all
 * three columns of the computer. Switched off, or without power, the monitor is dark.
 */
public final class DialingComputerRenderer implements BlockEntityRenderer<DialingComputerBlockEntity,
        DialingComputerRenderer.State> {

    // The screen, in the middle lower part's space with the computer facing north: from the operator's left, a block
    // east, to their right, a block west, and from just above the desk to near the top of the upper row
    private static final float LEFT = 2 - 1 / 16F;
    private static final float RIGHT = -1 + 1 / 16F;
    private static final float BOTTOM = 13.5F / 16;
    private static final float TOP = 1 + 14.5F / 16;
    // Just in front of the monitor's face
    private static final float FACE = 8 / 16F - 0.002F;
    // How near a player must be for the picture to be drawn
    private static final double VIEW_DISTANCE = 48;
    // Any texture with no see-through pixels, drawn all black for a dark screen
    private static final Identifier DARK = SGCraft.id("textures/gui/malp_static.png");
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;

    public DialingComputerRenderer(final BlockEntityRendererProvider.Context context) {
    }

    /** The computer's facing, whether it runs, and its picture, if it is near enough to be drawn. */
    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        boolean near;
        boolean running;
        @Nullable Identifier picture;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(final DialingComputerBlockEntity computer, final State state,
            final float partialTicks, final Vec3 cameraPosition,
            final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(computer, state, partialTicks, cameraPosition, breakProgress);
        state.facing = computer.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        state.near = Vec3.atCenterOf(computer.getBlockPos()).closerThan(cameraPosition,
                DialingComputerRenderer.VIEW_DISTANCE);
        state.running = computer.running();
        state.picture = state.near && state.running ? ComputerDisplays.texture(computer, partialTicks) : null;
    }

    @Override
    public void submit(final State state, final PoseStack poseStack, final SubmitNodeCollector collector,
            final CameraRenderState camera) {
        final Identifier picture = state.picture;
        if (picture == null && (!state.near || state.running)) {
            return;
        }
        poseStack.pushPose();
        // The model's own turn for each facing, from its north facing
        poseStack.translate(0.5, 0, 0.5);
        poseStack.rotateDegrees(Axis.YP, 180 - state.facing.toYRot());
        poseStack.translate(-0.5, 0, -0.5);
        if (picture != null) {
            collector.submitCustomGeometry(poseStack, RenderTypes.textPolygonOffset(picture),
                    (pose, buffer) -> DialingComputerRenderer.quad(pose, buffer, DialingComputerRenderer.WHITE));
        } else {
            collector.submitCustomGeometry(poseStack, RenderTypes.textPolygonOffset(DialingComputerRenderer.DARK),
                    (pose, buffer) -> DialingComputerRenderer.quad(pose, buffer, DialingComputerRenderer.BLACK));
        }
        poseStack.popPose();
    }

    // The screen's rectangle, facing north, at full brightness, its corners counterclockwise as seen from in front
    private static void quad(final PoseStack.Pose pose, final VertexConsumer buffer, final int color) {
        final int light = LightCoordsUtil.FULL_BRIGHT;
        final float z = DialingComputerRenderer.FACE;
        buffer.addVertex(pose, DialingComputerRenderer.LEFT, DialingComputerRenderer.TOP, z).setColor(color)
                .setUv(0, 0).setLight(light);
        buffer.addVertex(pose, DialingComputerRenderer.LEFT, DialingComputerRenderer.BOTTOM, z).setColor(color)
                .setUv(0, 1).setLight(light);
        buffer.addVertex(pose, DialingComputerRenderer.RIGHT, DialingComputerRenderer.BOTTOM, z).setColor(color)
                .setUv(1, 1).setLight(light);
        buffer.addVertex(pose, DialingComputerRenderer.RIGHT, DialingComputerRenderer.TOP, z).setColor(color)
                .setUv(1, 0).setLight(light);
    }

    // The screen reaches a block either side of the block entity, past the block the game culls it by
    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    // Culled by its whole box, on Fabric too
    @Override
    public boolean shouldRender(final DialingComputerBlockEntity blockEntity, final Vec3 cameraPosition) {
        return BlockEntityRenderer.super.shouldRender(blockEntity, cameraPosition)
                && ViewCulling.visible(this.getRenderBoundingBox(blockEntity));
    }

    /**
     * The space the whole screen can take up, whichever way the computer faces: a block either side of the block
     * entity and up through the upper row, so it isn't culled while any of it is in view. NeoForge culls block
     * entities by this box; it isn't part of the vanilla interface, so there is no {@code @Override}.
     */
    public AABB getRenderBoundingBox(final DialingComputerBlockEntity computer) {
        return new AABB(computer.getBlockPos()).inflate(1, 0, 1).expandTowards(0, 1, 0);
    }
}
