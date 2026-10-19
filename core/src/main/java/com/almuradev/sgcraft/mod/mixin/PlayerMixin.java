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

package com.almuradev.sgcraft.mod.mixin;

import com.almuradev.sgcraft.mod.entity.JumperShelter;
import com.almuradev.sgcraft.mod.entity.PuddleJumperEntity;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// A puddle jumper's riders stay in their seats while it moves, and are told to wait if they try to get up; and those
// riding in one, or inside its hull, are safe from mobs, which neither go after them nor keep after them
@Mixin(Player.class)
abstract class PlayerMixin {

    @Inject(method = "wantsToStopRiding()Z", at = @At("HEAD"), cancellable = true)
    private void sgcraft$stayInMovingJumper(final CallbackInfoReturnable<Boolean> cir) {
        final Player self = (Player) (Object) this;
        if (self.isShiftKeyDown() && self.getVehicle() instanceof final PuddleJumperEntity jumper && !jumper.still()) {
            if (self instanceof final ServerPlayer player && player.tickCount % 20 == 0) {
                player.sendOverlayMessage(Component.translatableWithFallback("message.sgcraft.puddle_jumper.moving",
                        "Wait until the jumper stops to get up"));
            }
            cir.setReturnValue(false);
        }
    }

    @ModifyReturnValue(method = "canBeSeenAsEnemy", at = @At("RETURN"))
    private boolean sgcraft$safeInJumper(final boolean enemy) {
        return enemy && !JumperShelter.sheltered((Player) (Object) this);
    }
}
