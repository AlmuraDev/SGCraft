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

package com.almuradev.sgcraft.client.gate;

import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.api.gate.GateTypes;
import com.almuradev.sgcraft.block.entity.StargateBaseBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Draws a formed gate from its base, by its type's {@link GateTypeRenderer}.
 *
 * <p>The gate is drawn in its own frame: its centre two blocks above the middle of the base, x to its right as seen
 * from in front, y up, and its front towards +z, turned to face the way the base does.</p>
 */
public final class StargateBaseRenderer implements BlockEntityRenderer<StargateBaseBlockEntity, StargateBaseRenderState> {

    /** The angle between neighbouring chevrons, in degrees. */
    public static final float CHEVRON_SPACING = 40.0F;

    // How far above the middle of the base the ring's centre is, in blocks
    private static final double CENTRE_HEIGHT = 2.0;
    // How far the gate reaches to either side of its base, and how tall it is, in blocks
    private static final double GATE_HALF_WIDTH = 3.0;
    private static final double GATE_HEIGHT = 5.0;

    private static final Map<ResourceKey<GateType>, GateTypeRenderer> RENDERERS = Map.of(
            GateTypes.MILKY_WAY, new MilkyWayGateRenderer(),
            GateTypes.PEGASUS, new PegasusGateRenderer(),
            GateTypes.UNIVERSE, new UniverseGateRenderer());

    public StargateBaseRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public StargateBaseRenderState createRenderState() {
        return new StargateBaseRenderState();
    }

    @Override
    public void extractRenderState(final StargateBaseBlockEntity gate, final StargateBaseRenderState state,
            final float partialTicks, final Vec3 cameraPosition,
            final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(gate, state, partialTicks, cameraPosition, breakProgress);
        state.formed = gate.isFormed();
        state.gateType = gate.gateType();
        state.facing = gate.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        // The base is solid, so it has no light of its own; the gate is lit as the middle of its ring is
        if (gate.getLevel() != null) {
            state.lightCoords = LightCoordsUtil.getLightCoords(gate.getLevel(), gate.getBlockPos().above(2));
        }
    }

    @Override
    public void submit(final StargateBaseRenderState state, final PoseStack poseStack,
            final SubmitNodeCollector collector, final CameraRenderState camera) {
        final GateTypeRenderer renderer = StargateBaseRenderer.RENDERERS.get(state.gateType);
        if (!state.formed || renderer == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.rotateDegrees(Axis.YP, -state.facing.toYRot());
        poseStack.translate(0.0, StargateBaseRenderer.CENTRE_HEIGHT, 0.0);
        renderer.submit(state, poseStack, collector);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        // The gate reaches well beyond its base, so it stays drawn while the base is off screen
        return true;
    }

    /**
     * The space the whole gate can take up, whichever way it faces, so it isn't culled while any of it is in view.
     * NeoForge culls block entities by this box; it isn't part of the vanilla interface, so there is no
     * {@code @Override}.
     */
    public AABB getRenderBoundingBox(final StargateBaseBlockEntity gate) {
        return new AABB(gate.getBlockPos()).inflate(StargateBaseRenderer.GATE_HALF_WIDTH, 0.0,
                StargateBaseRenderer.GATE_HALF_WIDTH).expandTowards(0.0, StargateBaseRenderer.GATE_HEIGHT - 1, 0.0);
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
