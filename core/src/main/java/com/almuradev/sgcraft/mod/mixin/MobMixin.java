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

import com.almuradev.sgcraft.mod.malp.MalpPresence;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Despawns a mob nearer a watched MALP than any player as if a player stood where the MALP does. Runs where vanilla
// looks for the nearest player, after a loader's own despawn hooks and the peaceful check have had their say
@Mixin(Mob.class)
abstract class MobMixin {

    @Inject(method = "checkDespawn", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getNearestPlayer(Lnet/minecraft/world/entity/Entity;D)"
                    + "Lnet/minecraft/world/entity/player/Player;"))
    private void sgcraft$despawnNearMalp(final CallbackInfo callback) {
        final Mob mob = (Mob) (Object) this;
        final double malp = MalpPresence.nearestSqr(mob.level(), mob.position());
        if (malp == Double.POSITIVE_INFINITY) {
            return;
        }
        final Player player = mob.level().getNearestPlayer(mob, -1.0);
        if (player != null && player.distanceToSqr(mob) <= malp) {
            return;
        }
        MalpPresence.checkDespawn(mob, malp);
        callback.cancel();
    }
}
