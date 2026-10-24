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

package com.almuradev.sgcraft.mod.client.obelisk;

import com.almuradev.sgcraft.mod.client.ViewCulling;
import com.almuradev.sgcraft.mod.client.gate.GateFeatureRenderer;
import com.almuradev.sgcraft.mod.client.gate.GateGeometry;
import com.almuradev.sgcraft.mod.client.gate.GateMesh;
import com.almuradev.sgcraft.mod.obelisk.AsgardObeliskBlockEntity;
import com.almuradev.sgcraft.mod.obelisk.ObeliskKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws an Asgard obelisk, both its halves, from its bottom block. As it beams someone its red gem shines, brighter
 * and whiter until the beam, then dying away, as 2.2.0's did.
 */
public final class ObeliskRenderer implements BlockEntityRenderer<AsgardObeliskBlockEntity, ObeliskRenderer.State> {

    public ObeliskRenderer(final BlockEntityRendererProvider.Context context) {
    }

    /** What the renderer needs to know about an obelisk for one frame. */
    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public ObeliskKind kind = ObeliskKind.HAMMER;
        public float shine;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(final AsgardObeliskBlockEntity obelisk, final State state, final float partialTicks,
            final Vec3 cameraPosition, final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(obelisk, state, partialTicks, cameraPosition, breakProgress);
        state.facing = obelisk.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        state.kind = obelisk.kind();
        state.shine = obelisk.shine(partialTicks);
    }

    // Its top half reaches past the block the game culls it by
    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    // Culled by its whole box, on Fabric too
    @Override
    public boolean shouldRender(final AsgardObeliskBlockEntity blockEntity, final Vec3 cameraPosition) {
        return BlockEntityRenderer.super.shouldRender(blockEntity, cameraPosition)
                && ViewCulling.visible(this.getRenderBoundingBox(blockEntity));
    }

    /**
     * Both its halves, so it isn't culled while its top is in view. NeoForge culls block entities by this box; it isn't
     * part of the vanilla interface, so there is no {@code @Override}.
     */
    public AABB getRenderBoundingBox(final AsgardObeliskBlockEntity obelisk) {
        return new AABB(obelisk.getBlockPos()).expandTowards(0, 1, 0);
    }

    @Override
    public void submit(final State state, final PoseStack poseStack, final SubmitNodeCollector collector,
            final CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        // Its model faces north, as the block does unturned
        poseStack.rotateDegrees(Axis.YP, 180 - state.facing.toYRot());
        poseStack.translate(-0.5, 0, -0.5);
        ObeliskRenderer.submitModel(poseStack, collector, state.kind, state.shine, state.lightCoords);
        poseStack.popPose();
    }

    /** Draws an obelisk in its bottom block's space, facing north, as the block and its item both are. */
    static void submitModel(final PoseStack poseStack, final SubmitNodeCollector collector, final ObeliskKind kind,
            final float shine, final int light) {
        final ObeliskMesh mesh = ObeliskMesh.get(kind);
        if (mesh == null) {
            return;
        }
        final int white = ARGB.white(1.0F);
        for (int part = 0; part < mesh.parts(); part++) {
            if (part != mesh.gem()) {
                GateFeatureRenderer.submit(collector, poseStack.last(), mesh.texture(part),
                        GateMesh.of(mesh.part(part)), white, light);
            }
        }
        // The gem's corners are clear; shining, it lights itself and whitens
        final float[] gem = mesh.part(mesh.gem());
        final int gemLight = shine > 0 ? LightCoordsUtil.FULL_BRIGHT : light;
        final float pale = 0.5F + 0.5F * shine;
        final int color = shine > 0 ? ARGB.colorFromFloat(1, 1, pale, pale) : white;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(mesh.texture(mesh.gem())),
                (pose, buffer) -> GateGeometry.emit(pose, buffer, gem, color, gemLight));
        if (shine > 0) {
            collector.submitCustomGeometry(poseStack, RenderTypes.eyes(mesh.texture(mesh.gem())),
                    (pose, buffer) -> GateGeometry.emit(pose, buffer, gem,
                            ARGB.colorFromFloat(shine, 1, 0.3F + 0.7F * shine, 0.3F + 0.7F * shine),
                            LightCoordsUtil.FULL_BRIGHT));
        }
    }
}
