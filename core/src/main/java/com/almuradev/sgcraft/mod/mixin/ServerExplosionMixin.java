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

import com.almuradev.sgcraft.mod.shield.CityShields;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps an explosion outside a raised city shield off the blocks and the things inside it.
 */
@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {

    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    @Final
    private Vec3 center;

    @Inject(method = "calculateExplodedPositions", at = @At("RETURN"), cancellable = true)
    private void sgcraft$spareShielded(final CallbackInfoReturnable<List<BlockPos>> cir) {
        if (!CityShields.any()) {
            return;
        }
        final List<BlockPos> blocks = new ArrayList<>(cir.getReturnValue());
        if (blocks.removeIf(pos -> CityShields.protects(this.level, this.center, Vec3.atCenterOf(pos)))) {
            cir.setReturnValue(blocks);
        }
    }

    @Redirect(method = "hurtEntities(Ljava/util/List;)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;getEntities(Lnet/minecraft/world/entity/Entity;"
                    + "Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<Entity> sgcraft$spareShieldedEntities(final ServerLevel level, final Entity source,
            final AABB area) {
        final List<Entity> entities = level.getEntities(source, area);
        if (!CityShields.any()) {
            return entities;
        }
        final List<Entity> reached = new ArrayList<>(entities);
        reached.removeIf(entity -> CityShields.protects(this.level, this.center, entity.position()));
        return reached;
    }
}
