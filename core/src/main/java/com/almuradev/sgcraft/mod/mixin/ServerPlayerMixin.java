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
import com.almuradev.sgcraft.mod.malp.MalpLinks;
import com.almuradev.sgcraft.mod.rings.RingHolds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Keeps a player a ring transport holds from damage, and takes a player who takes damage off their MALP console, if
// the server's settings say damage does; and keeps fire off players inside a puddle jumper
@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void sgcraft$ringHold(final ServerLevel level, final DamageSource source, final float damage,
            final CallbackInfoReturnable<Boolean> callback) {
        if (RingHolds.shields((ServerPlayer) (Object) this, source)) {
            callback.setReturnValue(false);
        }
    }

    // Fire and lava don't reach anyone inside a puddle jumper, seated or standing, and any fire on them goes out
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void sgcraft$shelteredFromFire(final ServerLevel level, final DamageSource source, final float damage,
            final CallbackInfoReturnable<Boolean> callback) {
        if (source.is(DamageTypeTags.IS_FIRE) && JumperShelter.sheltered((ServerPlayer) (Object) this)) {
            ((ServerPlayer) (Object) this).clearFire();
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void sgcraft$putOutSheltered(final CallbackInfo callback) {
        final ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.getRemainingFireTicks() > 0 && JumperShelter.sheltered(player)) {
            player.clearFire();
        }
    }

    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void sgcraft$consoleDamage(final ServerLevel level, final DamageSource source, final float damage,
            final CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValue()) {
            MalpLinks.hurt((ServerPlayer) (Object) this);
        }
    }
}
