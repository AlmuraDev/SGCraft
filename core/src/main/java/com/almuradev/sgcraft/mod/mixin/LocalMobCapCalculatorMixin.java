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
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LocalMobCapCalculator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

// A chunk no player is near has no player's own cap to keep to; near a watched MALP it keeps only to the world's cap,
// which the MALP's chunks have added to
@Mixin(LocalMobCapCalculator.class)
abstract class LocalMobCapCalculatorMixin {

    @Shadow
    @Final
    private ChunkMap chunkMap;

    @Shadow
    private List<ServerPlayer> getPlayersNear(final ChunkPos pos) {
        throw new AssertionError();
    }

    @Inject(method = "canSpawn", at = @At("RETURN"), cancellable = true)
    private void sgcraft$spawnNearMalp(final MobCategory category, final ChunkPos pos,
            final CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValue() && this.getPlayersNear(pos).isEmpty()
                && MalpPresence.nearForSpawning(((ChunkMapAccessor) this.chunkMap).sgcraft$level(), pos)) {
            callback.setReturnValue(true);
        }
    }
}
