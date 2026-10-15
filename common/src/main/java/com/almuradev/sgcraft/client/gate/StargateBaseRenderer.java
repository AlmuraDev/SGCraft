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

import com.almuradev.sgcraft.SGCraft;
import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.api.gate.GateTypes;
import com.almuradev.sgcraft.block.entity.StargateBaseBlockEntity;
import com.almuradev.sgcraft.gate.ChevronOrder;
import com.almuradev.sgcraft.gate.ChevronSpacing;
import com.almuradev.sgcraft.gate.DialPhase;
import com.almuradev.sgcraft.gate.EventHorizon;
import com.almuradev.sgcraft.gate.GateFrame;
import com.almuradev.sgcraft.gate.GateOrientation;
import com.almuradev.sgcraft.gate.GateShape;
import com.almuradev.sgcraft.gate.IrisView;
import com.almuradev.sgcraft.mod.SGIris;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * Draws a formed gate from its base, by its type's {@link GateTypeRenderer}.
 *
 * <p>The gate is drawn in its own frame (see {@link GateFrame}): its center up its ring from the middle of the base,
 * x to its right as seen from in front, y up its ring, and its front towards +z. The base's frame is turned to face
 * the way the base does, then, for a gate lying flat, turned over so the ring runs out along the base's back, its
 * front up or down, as the original turns it.</p>
 */
public final class StargateBaseRenderer
        implements BlockEntityRenderer<StargateBaseBlockEntity, StargateBaseRenderState> {

    /** The angle between neighboring chevrons, in degrees. */
    public static final float CHEVRON_SPACING = 40.0F;


    private static final Identifier HORIZON = SGCraft.id("textures/gate/event_horizon.png");
    // The glow over an open horizon, in the gate's color: how bright it is, how far it reaches from the center, past
    // the ring, how far in front of and behind the gate it lies, out of the ring, and how long it takes to fade as the
    // wormhole shuts, in ticks
    private static final float HORIZON_GLOW = 0.1F;
    private static final double HORIZON_GLOW_REACH = 3.0;
    private static final double HORIZON_GLOW_DEPTH = 0.15;
    private static final double HORIZON_GLOW_FADE_TICKS = 40;

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
        state.shape = gate.shape();
        state.gateType = gate.gateType();
        state.facing = gate.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        state.orientation = gate.orientation();
        // After the facing, which says which way the bottom row runs, and the orientation, as a flat gate has none
        StargateBaseRenderer.extractCamouflage(gate, state);
        state.chevrons = gate.chevrons();
        state.spacing = ChevronSpacing.of(gate.chevrons(), gate.camouflage());
        state.view = gate.dialView();
        state.locked = ChevronOrder.locked(gate.chevrons(), state.view.length(), state.view.engaged());
        state.time = gate.getLevel() != null ? gate.getLevel().getGameTime() + partialTicks : 0;
        final IrisView iris = gate.iris();
        state.irisAperture = iris == null ? 1 : SGIris.aperture(iris.state(), iris.since(), state.time);
        state.transparent = gate.transparent();
        final EventHorizon horizon = gate.horizon();
        if (horizon == null) {
            state.horizon = null;
        } else {
            if (state.horizon == null) {
                state.horizon = new double[EventHorizon.SPOKES + 1][EventHorizon.RINGS + 1];
            }
            for (int j = 0; j <= EventHorizon.SPOKES; j++) {
                for (int i = 0; i <= EventHorizon.RINGS; i++) {
                    state.horizon[j][i] = horizon.offset(j, i, partialTicks);
                }
            }
        }
        // The base is solid, so it has no light of its own; the gate is lit as the middle of its ring is
        if (gate.getLevel() != null) {
            state.lightCoords = LightCoordsUtil.getLightCoords(gate.getLevel(),
                    gate.frame().block(0, gate.shape().radius(), 0));
        }
    }

    @Override
    public void submit(final StargateBaseRenderState state, final PoseStack poseStack,
            final SubmitNodeCollector collector, final CameraRenderState camera) {
        final GateTypeRenderer renderer = StargateBaseRenderer.RENDERERS.get(state.gateType);
        if (state.formed) {
            StargateBaseRenderer.submitCamouflage(state, poseStack, collector);
        }
        if (!state.formed || renderer == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.rotateDegrees(Axis.YP, -state.facing.toYRot());
        // From the base's frame into the gate's: lying flat, its ring runs out along the base's back
        if (state.orientation == GateOrientation.FACE_UP) {
            poseStack.rotateDegrees(Axis.XP, -90.0F);
        } else if (state.orientation == GateOrientation.FACE_DOWN) {
            poseStack.rotateDegrees(Axis.ZP, 180.0F);
            poseStack.rotateDegrees(Axis.XP, -90.0F);
        }
        poseStack.translate(0.0, state.shape.centerY(), 0.0);
        // A supergate is a standard gate scaled up to its blocks, but only as thick as them, as in the original
        final double scale = state.shape.scale();
        poseStack.scale((float) scale, (float) scale, (float) Math.min(scale, state.shape.depthScale()));
        renderer.submit(state, poseStack, collector);
        if (state.horizon != null) {
            StargateBaseRenderer.submitHorizon(state.horizon, state.irisAperture, state.transparent, poseStack,
                    collector);
            StargateBaseRenderer.submitHorizonGlow(state, renderer.glowColor(), poseStack, collector);
        }
        IrisRenderer.submit(state.gateType, state.irisAperture, poseStack, collector, state.lightCoords);
        poseStack.popPose();
    }

    // The open wormhole's surface, on both faces, its texture repeating every block. As in the original it lets a
    // little of what is behind it show through, unless the gate is set to keep it solid. Behind an iris, as in the
    // original, it ripples forward only inside the hole the blades leave, and lies flat behind them everywhere else,
    // so none of it shows through them.
    private static void submitHorizon(final double[][] horizon, final float irisAperture, final boolean transparent,
            final PoseStack poseStack, final SubmitNodeCollector collector) {
        final double clip = EventHorizon.RINGS * EventHorizon.BAND * Math.min(1, irisAperture);
        collector.submitCustomGeometry(poseStack, RenderTypes.beaconBeam(StargateBaseRenderer.HORIZON, transparent),
                (pose, buffer) -> {
                    for (int j = 0; j < EventHorizon.SPOKES; j++) {
                        for (int i = 0; i < EventHorizon.RINGS; i++) {
                            // The innermost band closes to a point at the center
                            final double[][] quad = {
                                    StargateBaseRenderer.point(horizon, j, i, clip),
                                    StargateBaseRenderer.point(horizon, j + 1, i, clip),
                                    StargateBaseRenderer.point(horizon, j + 1, i + 1, clip),
                                    StargateBaseRenderer.point(horizon, j, i + 1, clip)};
                            StargateBaseRenderer.horizonQuad(pose, buffer, quad, false);
                            StargateBaseRenderer.horizonQuad(pose, buffer, quad, true);
                        }
                    }
                });
    }

    // A soft, steady glow in the gate's color over its open horizon, on both faces; an iris shuts it off, and it fades
    // as the wormhole shuts down
    private static void submitHorizonGlow(final StargateBaseRenderState state, final int color,
            final PoseStack poseStack, final SubmitNodeCollector collector) {
        double strength = StargateBaseRenderer.HORIZON_GLOW * state.irisAperture;
        if (state.view.phase() == DialPhase.DISCONNECTING) {
            strength *= Math.max(0, 1 - (state.time - state.view.phaseStart())
                    / StargateBaseRenderer.HORIZON_GLOW_FADE_TICKS);
        }
        state.horizonGlow.clear();
        if (strength <= 0) {
            return;
        }
        final int glow = ARGB.scaleRGB(color, (float) strength);
        state.horizonGlow.add(0, 0, StargateBaseRenderer.HORIZON_GLOW_DEPTH, StargateBaseRenderer.HORIZON_GLOW_REACH,
                glow);
        state.horizonGlow.add(0, 0, -StargateBaseRenderer.HORIZON_GLOW_DEPTH,
                StargateBaseRenderer.HORIZON_GLOW_REACH, glow);
        state.horizonGlow.submit(collector, poseStack, GateGlow.HORIZON_TYPE);
    }

    private static double[] point(final double[][] horizon, final int spoke, final int ring, final double clip) {
        final double angle = 2 * Math.PI * spoke / EventHorizon.SPOKES;
        final double radius = ring * EventHorizon.BAND;
        final double z = horizon[spoke % EventHorizon.SPOKES][ring];
        return new double[] {radius * Math.cos(angle), radius * Math.sin(angle), radius >= clip ? Math.min(z, 0) : z};
    }

    private static void horizonQuad(final PoseStack.Pose pose, final VertexConsumer buffer, final double[][] quad,
            final boolean back) {
        for (int k = 0; k < 4; k++) {
            final double[] p = quad[back ? 3 - k : k];
            buffer.addVertex(pose, (float) p[0], (float) p[1], (float) p[2])
                    .setColor(-1)
                    .setUv((float) p[0], (float) p[1])
                    .setLight(LightCoordsUtil.FULL_BRIGHT);
        }
    }

    @Override
    public boolean shouldRenderOffScreen() {
        // The gate reaches well beyond its base, so it stays drawn while the base is off screen
        return true;
    }

    // Each block of camouflage, as a block lit where it stands, as the original draws it in place of that block
    private static void extractCamouflage(final StargateBaseBlockEntity gate, final StargateBaseRenderState state) {
        final Direction right = state.facing.getCounterClockWise();
        for (int slot = 0; slot < StargateBaseBlockEntity.CAMOUFLAGE_SLOTS; slot++) {
            final Optional<BlockState> block = gate.isFormed() && !state.orientation.isFlat()
                    ? gate.camouflageState(slot) : Optional.empty();
            if (block.isEmpty() || !(gate.getLevel() instanceof ClientLevel level)) {
                state.camouflage[slot] = null;
                continue;
            }
            final BlockPos offset = BlockPos.ZERO.relative(right, slot - StargateBaseBlockEntity.CAMOUFLAGE_SLOTS / 2);
            final BlockPos pos = gate.getBlockPos().offset(offset);
            final MovingBlockRenderState camouflage = new MovingBlockRenderState();
            camouflage.randomSeedPos = pos;
            camouflage.blockPos = pos;
            camouflage.blockState = block.get();
            camouflage.biome = level.getBiome(pos);
            camouflage.cardinalLighting = level.cardinalLighting();
            camouflage.lightEngine = level.getLightEngine();
            state.camouflage[slot] = camouflage;
            state.camouflageOffsets[slot] = offset;
        }
    }

    private static void submitCamouflage(final StargateBaseRenderState state, final PoseStack poseStack,
            final SubmitNodeCollector collector) {
        for (int slot = 0; slot < StargateBaseBlockEntity.CAMOUFLAGE_SLOTS; slot++) {
            final MovingBlockRenderState camouflage = state.camouflage[slot];
            if (camouflage != null) {
                final BlockPos offset = state.camouflageOffsets[slot];
                poseStack.pushPose();
                poseStack.translate(offset.getX(), offset.getY(), offset.getZ());
                collector.submitMovingBlock(poseStack, camouflage, 0);
                poseStack.popPose();
            }
        }
    }

    /**
     * The space the whole gate can take up, whichever way it faces or lies, so it isn't culled while any of it is in
     * view. NeoForge culls block entities by this box; it isn't part of the vanilla interface, so there is no
     * {@code @Override}.
     */
    public AABB getRenderBoundingBox(final StargateBaseBlockEntity gate) {
        final GateShape shape = gate.shape();
        final GateFrame frame = gate.frame();
        final double half = shape.radius() + 1.0;
        // Round the ring, and out of either face by half its width
        return frame.box(new Vec3(-half, -0.5, -half), new Vec3(half, shape.height() - 0.5, half));
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
