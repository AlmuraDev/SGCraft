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

import com.almuradev.sgcraft.mod.client.jumper.JumperControls;
import com.almuradev.sgcraft.mod.entity.JumperShelter;
import com.almuradev.sgcraft.mod.entity.PuddleJumperEntity;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

// In third person, a puddle jumper's rider sees it from far enough back to see all of it, rather than from inside it;
// and in third person from a seat it sees wider, so the craft fills less of the view; and inside one's hull the
// camera sees as in air, though it is under water, as its hull is sealed
@Mixin(Camera.class)
abstract class CameraMixin {

    // How far back the camera is, in blocks
    private static final float SGCRAFT_JUMPER_DISTANCE = 10;

    @Shadow
    private @Nullable Entity entity;
    @Shadow
    private @Nullable Level level;

    @ModifyArg(method = "alignWithEntity(F)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"))
    private float sgcraft$backFromJumper(final float distance) {
        return this.entity != null && this.entity.getRootVehicle() instanceof PuddleJumperEntity
                ? Math.max(distance, CameraMixin.SGCRAFT_JUMPER_DISTANCE) : distance;
    }

    @ModifyReturnValue(method = "calculateFov", at = @At("RETURN"))
    private float sgcraft$wideFromJumper(final float fov, final float partialTicks) {
        return JumperControls.fov(fov, partialTicks);
    }

    @ModifyReturnValue(method = "getFluidInCamera", at = @At("RETURN"))
    private FogType sgcraft$dryInsideJumper(final FogType fog) {
        return fog == FogType.WATER && this.level != null
                && JumperShelter.inside(this.level, ((Camera) (Object) this).position()) ? FogType.NONE : fog;
    }
}
