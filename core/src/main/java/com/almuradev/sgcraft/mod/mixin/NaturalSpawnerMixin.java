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
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Measures how far a spawning mob would be from the nearest player from a watched MALP instead, where the MALP is
// nearer, so mobs spawn around it no nearer and no farther than around a player
@Mixin(NaturalSpawner.class)
abstract class NaturalSpawnerMixin {

    // The overload that spawns a pack of mobs, which the others call
    private static final String SPAWN = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;"
            + "Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;"
            + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;"
            + "Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V";

    // Where the world has no player at all, one stands in while a MALP is there, so spawning goes on to measure
    @WrapOperation(method = NaturalSpawnerMixin.SPAWN, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;getNearestPlayer(DDDDZ)"
                    + "Lnet/minecraft/world/entity/player/Player;"))
    private static @Nullable Player sgcraft$standIn(final ServerLevel level, final double x, final double y,
            final double z, final double range, final boolean creative, final Operation<Player> original) {
        final Player player = original.call(level, x, y, z, range, creative);
        return player != null ? player : MalpPresence.standIn(level, new Vec3(x, y, z));
    }

    @WrapOperation(method = NaturalSpawnerMixin.SPAWN, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;distanceToSqr(DDD)D"))
    private static double sgcraft$malpDistance(final Player player, final double x, final double y, final double z,
            final Operation<Double> original, @Local(argsOnly = true) final ServerLevel level) {
        return MalpPresence.spawnDistanceSqr(level, player, new Vec3(x, y, z), original.call(player, x, y, z));
    }
}
