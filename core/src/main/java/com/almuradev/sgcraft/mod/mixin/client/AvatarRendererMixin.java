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

package com.almuradev.sgcraft.mod.mixin.client;

import com.almuradev.sgcraft.mod.client.malp.MalpConsolePose;
import com.almuradev.sgcraft.mod.entity.PuddleJumperEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Poses a player at a MALP console, or looking at a MALP tablet, once the renderer has read their own state; and
// empties the hands of a rider in a puddle jumper's seat
@Mixin(AvatarRenderer.class)
abstract class AvatarRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;"
            + "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void sgcraft$consolePose(final Avatar entity, final AvatarRenderState state, final float partialTicks,
            final CallbackInfo callback) {
        MalpConsolePose.extract(state);
    }

    // A rider in a jumper's seat holds nothing that shows, their hands at the controls or in their lap
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;"
            + "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void sgcraft$riderHandsEmpty(final Avatar entity, final AvatarRenderState state, final float partialTicks,
            final CallbackInfo callback) {
        if (entity.getVehicle() instanceof PuddleJumperEntity) {
            state.rightHandItemState.clear();
            state.leftHandItemState.clear();
            state.rightHandItemStack = ItemStack.EMPTY;
            state.leftHandItemStack = ItemStack.EMPTY;
            state.rightArmPose = HumanoidModel.ArmPose.EMPTY;
            state.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        }
    }
}
