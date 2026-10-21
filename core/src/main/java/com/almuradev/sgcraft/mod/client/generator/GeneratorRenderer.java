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

package com.almuradev.sgcraft.mod.client.generator;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.client.gate.GateFeatureRenderer;
import com.almuradev.sgcraft.mod.client.gate.GateMesh;
import com.almuradev.sgcraft.mod.generator.GeneratorMark;
import com.almuradev.sgcraft.mod.generator.NaquadahGeneratorBlock;
import com.almuradev.sgcraft.mod.generator.NaquadahGeneratorBlockEntity;
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
 * Draws a Naquadah Generator from its model, its arms out while it is switched on. While it runs, its crystal, red on
 * a Mark I and blue on a Mark II, the rod across its middle and the lights round its dome shine with their own light,
 * pulsing gently, as 2.2.0's did.
 */
public final class GeneratorRenderer implements BlockEntityRenderer<NaquadahGeneratorBlockEntity,
        GeneratorRenderer.State> {

    private static final Identifier[] TEXTURES = new Identifier[GeneratorMesh.MATERIALS.length];
    private static final Identifier CRYSTAL_BLUE = SGCraft.id("textures/block/naquadah_generator_crystal_blue.png");

    static {
        for (int i = 0; i < GeneratorMesh.MATERIALS.length; i++) {
            final String name = GeneratorMesh.MATERIALS[i].equals("crystal") ? "crystal_red"
                    : GeneratorMesh.MATERIALS[i];
            GeneratorRenderer.TEXTURES[i] = SGCraft.id("textures/block/naquadah_generator_" + name + ".png");
        }
    }

    public GeneratorRenderer(final BlockEntityRendererProvider.Context context) {
    }

    /** What the renderer needs to know about a generator for one frame. */
    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public boolean on;
        public boolean running;
        public boolean markTwo;
        public float time;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(final NaquadahGeneratorBlockEntity generator, final State state,
            final float partialTicks, final Vec3 cameraPosition,
            final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(generator, state, partialTicks, cameraPosition, breakProgress);
        state.facing = generator.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        state.on = generator.getBlockState().getValue(NaquadahGeneratorBlock.ON);
        state.running = generator.getBlockState().getValue(NaquadahGeneratorBlock.RUNNING);
        state.markTwo = generator.mark() == GeneratorMark.TWO;
        state.time = generator.getLevel() == null ? 0 : generator.getLevel().getGameTime() + partialTicks;
    }

    @Override
    public void submit(final State state, final PoseStack poseStack, final SubmitNodeCollector collector,
            final CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        // Its model faces north, as the block does unturned
        poseStack.rotateDegrees(Axis.YP, 180 - state.facing.toYRot());
        poseStack.translate(-0.5, 0, -0.5);
        GeneratorRenderer.submitModel(poseStack, collector, state.on, state.running, state.markTwo, state.time,
                state.lightCoords);
        poseStack.popPose();
    }

    /** Draws a generator in a block's space, facing north, as the block and its item both are. */
    static void submitModel(final PoseStack poseStack, final SubmitNodeCollector collector, final boolean on,
            final boolean running, final boolean markTwo, final float time, final int light) {
        final GeneratorMesh mesh = GeneratorMesh.get();
        if (mesh == null) {
            return;
        }
        final int white = ARGB.white(1.0F);
        final float pulse = (float) (0.8 + 0.2 * Math.sin(time * 0.15));
        final int glow = ARGB.colorFromFloat(1, pulse, pulse, pulse);
        for (int material = 0; material < GeneratorMesh.MATERIALS.length; material++) {
            final Identifier texture = material == GeneratorMesh.CRYSTAL && markTwo ? GeneratorRenderer.CRYSTAL_BLUE
                    : GeneratorRenderer.TEXTURES[material];
            final GateMesh part = GateMesh.of(mesh.part(on, material));
            final boolean lit = material == GeneratorMesh.CRYSTAL || material == GeneratorMesh.ROD
                    || material == GeneratorMesh.DOME;
            if (running && lit) {
                GateFeatureRenderer.submitEmissive(collector, poseStack.last(), texture, part, glow);
            } else {
                GateFeatureRenderer.submit(collector, poseStack.last(), texture, part, white, light);
            }
        }
    }
}
