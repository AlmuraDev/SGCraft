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
import com.almuradev.sgcraft.mod.entity.MalpEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;

/**
 * Draws a MALP: its model turned the way it faces, rocking when hit as a boat does, with its wheels, camera mast and
 * headlights as the MALP has them.
 */
public final class MalpRenderer extends EntityRenderer<MalpEntity, MalpRenderState> {

    private static final Identifier TEXTURE = SGCraft.id("textures/entity/malp.png");

    private final MalpModel model;
    private final Model.Simple glow;

    public MalpRenderer(final EntityRendererProvider.Context context) {
        super(context);
        this.model = new MalpModel(context.bakeLayer(MalpModel.LAYER));
        this.glow = new Model.Simple(context.bakeLayer(MalpModel.GLOW_LAYER), RenderTypes::eyes);
        this.shadowRadius = 0.6F;
    }

    @Override
    public MalpRenderState createRenderState() {
        return new MalpRenderState();
    }

    @Override
    public void extractRenderState(final MalpEntity entity, final MalpRenderState state, final float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yRot = entity.getYRot(partialTick);
        state.hurtTime = entity.getHurtTime() - partialTick;
        state.hurtDir = entity.getHurtDir();
        state.damageTime = Math.max(entity.getDamage() - partialTick, 0.0F);
        state.leftWheels = entity.wheels(true, partialTick);
        state.rightWheels = entity.wheels(false, partialTick);
        state.mastYaw = entity.mastYaw(partialTick);
        state.mastPitch = entity.mastPitch(partialTick);
        state.lamp = entity.viewLamp();
    }

    @Override
    public void submit(final MalpRenderState state, final PoseStack poseStack, final SubmitNodeCollector collector,
            final CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.rotateDegrees(Axis.YP, 180.0F - state.yRot);
        if (state.hurtTime > 0.0F) {
            poseStack.rotateDegrees(Axis.XP, Mth.sin(state.hurtTime) * state.hurtTime * state.damageTime / 10.0F
                    * state.hurtDir);
        }
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        collector.submitModel(this.model, state, poseStack, MalpRenderer.TEXTURE, state.lightCoords,
                OverlayTexture.NO_OVERLAY, state.outlineColor);
        if (state.lamp) {
            collector.submitModel(this.glow, Unit.INSTANCE, poseStack, MalpRenderer.TEXTURE,
                    LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
