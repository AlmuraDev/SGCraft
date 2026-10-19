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

import com.almuradev.sgcraft.mod.entity.JumperHull;
import com.almuradev.sgcraft.mod.entity.JumperIndex;
import com.almuradev.sgcraft.mod.entity.JumperShelter;
import com.almuradev.sgcraft.mod.entity.PuddleJumperEntity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

// Moving things collide with the solid parts of puddle jumpers' hulls, rather than with their hitboxes, so they may
// walk about inside one, stand on its roof and walk up through its open hatch; its own riders don't. And as a jumper's
// hull is sealed, those riding in one, or with their eyes inside one's hull, aren't under water though it is: they
// breathe, and don't see the water about them; nor are they in it at all, walking about inside rather than
// swimming
@Mixin(Entity.class)
abstract class EntityMixin {

    @ModifyExpressionValue(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntityCollisions("
                    + "Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<VoxelShape> sgcraft$collideWithJumpers(final List<VoxelShape> collisions, final Vec3 movement) {
        final Entity self = (Entity) (Object) this;
        // Every entity that moves passes through here each tick, so a world with no jumper costs only this look
        if (!JumperIndex.any(self.level())
                || self.isPassenger() && self.getRootVehicle() instanceof PuddleJumperEntity) {
            return collisions;
        }
        final AABB reach = self.getBoundingBox().expandTowards(movement).inflate(1.0E-7);
        List<VoxelShape> all = collisions;
        for (final PuddleJumperEntity jumper : JumperIndex.near(self.level(),
                reach.inflate(PuddleJumperEntity.LENGTH / 2 + JumperHull.RAMP_LENGTH))) {
            if (jumper == self) {
                continue;
            }
            if (all == collisions) {
                all = new ArrayList<>(collisions);
            }
            for (final VoxelShape part : jumper.hull()) {
                if (part.bounds().intersects(reach)) {
                    all.add(part);
                }
            }
        }
        return all;
    }

    @ModifyReturnValue(method = "isEyeInFluid", at = @At("RETURN"))
    private boolean sgcraft$sealedInJumper(final boolean inFluid, final TagKey<Fluid> type) {
        return inFluid && type == FluidTags.WATER && JumperShelter.sheltered((Entity) (Object) this) ? false
                : inFluid;
    }

    // Nor does water about the hull touch those walking about inside it, or riding in it: they walk, rather than swim
    @ModifyReturnValue(method = "getFluidInteractionBox", at = @At("RETURN"))
    private @Nullable AABB sgcraft$dryInJumper(final @Nullable AABB box) {
        return box != null && JumperShelter.dryInside((Entity) (Object) this) ? null : box;
    }
}
