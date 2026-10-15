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

package com.almuradev.sgcraft.mod.client.dhd;

import com.almuradev.sgcraft.api.gate.GateTypes;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.block.entity.DhdBlockEntity;
import com.almuradev.sgcraft.mod.block.entity.StargateBaseBlockEntity;
import com.almuradev.sgcraft.mod.client.gate.GateFeatureRenderer;
import com.almuradev.sgcraft.mod.client.gate.GateMesh;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a DHD from the original SGCraft's model, its dome tinted by its style and lit while its gate is active.
 */
public final class DhdRenderer implements BlockEntityRenderer<DhdBlockEntity, DhdRenderer.State> {

    private static final Identifier[] TEXTURES = {
            SGCraft.id("textures/block/dhd_top.png"),
            SGCraft.id("textures/block/dhd_side.png"),
            SGCraft.id("textures/block/dhd_detail.png"),
            SGCraft.id("textures/block/dhd_detail.png"),
    };
    // The dome while the DHD has no gate, while its gate is idle, and while its gate is active, for each style
    private static final int[] MILKY_WAY_DOME = {
            ARGB.colorFromFloat(1, 0.5F, 0.5F, 0.5F), ARGB.colorFromFloat(1, 0.5F, 0.25F, 0),
            ARGB.colorFromFloat(1, 1, 0.5F, 0)};
    private static final int[] PEGASUS_DOME = {
            ARGB.colorFromFloat(1, 0, 0.5F, 0.5F), ARGB.colorFromFloat(1, 0, 0.25F, 0.75F),
            ARGB.colorFromFloat(1, 0, 0.5F, 1)};

    public DhdRenderer(final BlockEntityRendererProvider.Context context) {
    }

    /** What the DHD renderer needs to know about a DHD for one frame. */
    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public boolean pegasus;
        // 0 with no gate, 1 with its gate idle, 2 with its gate active
        public int dome;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(final DhdBlockEntity dhd, final State state, final float partialTicks,
            final Vec3 cameraPosition, final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(dhd, state, partialTicks, cameraPosition, breakProgress);
        state.facing = dhd.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        state.pegasus = dhd.style().equals(GateTypes.PEGASUS);
        state.dome = 0;
        if (dhd.getLevel() != null && dhd.linked().isPresent()) {
            state.dome = dhd.getLevel().getBlockEntity(dhd.linked().get()) instanceof StargateBaseBlockEntity gate
                    && gate.dialView().active() ? 2 : 1;
        }
    }

    @Override
    public void submit(final State state, final PoseStack poseStack, final SubmitNodeCollector collector,
            final CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        poseStack.rotateDegrees(Axis.YP, -state.facing.toYRot());
        poseStack.translate(-0.5, 0, -0.5);
        DhdRenderer.submitModel(poseStack, collector, state.pegasus, state.dome, state.lightCoords);
        poseStack.popPose();
    }

    /**
     * Draws a DHD in a block's space, facing north, as the block and its item both are.
     *
     * @param dome 0 with no gate, 1 with its gate idle, 2 with its gate active
     */
    static void submitModel(final PoseStack poseStack, final SubmitNodeCollector collector, final boolean pegasus,
            final int dome, final int light) {
        final DhdMesh mesh = DhdMesh.get();
        if (mesh == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        final int white = ARGB.white(1.0F);
        for (int texture = DhdMesh.TOP; texture <= DhdMesh.DETAIL; texture++) {
            GateFeatureRenderer.submit(collector, poseStack.last(), DhdRenderer.TEXTURES[texture],
                    GateMesh.of(mesh.part(texture)), white, light);
        }
        final int color = (pegasus ? DhdRenderer.PEGASUS_DOME : DhdRenderer.MILKY_WAY_DOME)[dome];
        // Its dome shines with its own light while its gate is active
        if (dome == 2) {
            GateFeatureRenderer.submitEmissive(collector, poseStack.last(), DhdRenderer.TEXTURES[DhdMesh.BUTTON],
                    GateMesh.of(mesh.part(DhdMesh.BUTTON)), color);
        } else {
            GateFeatureRenderer.submit(collector, poseStack.last(), DhdRenderer.TEXTURES[DhdMesh.BUTTON],
                    GateMesh.of(mesh.part(DhdMesh.BUTTON)), color, light);
        }
        poseStack.popPose();
    }
}
