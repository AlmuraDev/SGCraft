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

package com.almuradev.sgcraft.mod.client.jumper;

import com.almuradev.sgcraft.mod.entity.JumperIndex;
import com.almuradev.sgcraft.mod.entity.PuddleJumperEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * What a player outside a puddle jumper aims at on it. The game aims only at an entity's hitbox, which for a jumper is
 * a square as wide as its hull round its middle, so its nose and its cargo door, beyond that, can't be used. Here the
 * whole hull is aimed at, as its solid parts lie: whichever of them the player looks at first, nearer than whatever
 * else they look at, is the jumper. Only clients call this.
 */
public final class JumperPick {

    private JumperPick() {
    }

    /** Once the game has worked out what its player aims at: the jumper, if its hull lies first along their look. */
    public static void pick(final Minecraft minecraft, final float partialTicks) {
        final LocalPlayer player = minecraft.player;
        final Entity camera = minecraft.getCameraEntity();
        if (player == null || camera == null || minecraft.level == null || !JumperIndex.any(minecraft.level)
                || player.isSpectator()) {
            return;
        }
        final Vec3 eye = camera.getEyePosition(partialTicks);
        final double reach = player.entityInteractionRange();
        final Vec3 end = eye.add(camera.getViewVector(partialTicks).scale(reach));
        final HitResult current = minecraft.hitResult;
        double nearest = current == null || current.getType() == HitResult.Type.MISS ? reach * reach
                : current.getLocation().distanceToSqr(eye);
        PuddleJumperEntity aimed = null;
        Vec3 at = null;
        for (final PuddleJumperEntity jumper : JumperIndex.near(minecraft.level, new AABB(eye, end).inflate(1))) {
            // Those inside it, seated or standing, already aim at it wherever they look
            if (jumper.inside(eye) || player.getVehicle() == jumper) {
                continue;
            }
            for (final VoxelShape part : jumper.hull()) {
                final BlockHitResult hit = part.clip(eye, end, BlockPos.ZERO);
                if (hit != null && hit.getLocation().distanceToSqr(eye) < nearest) {
                    nearest = hit.getLocation().distanceToSqr(eye);
                    aimed = jumper;
                    at = hit.getLocation();
                }
            }
        }
        if (aimed != null && !(current instanceof final EntityHitResult entity && entity.getEntity() == aimed)) {
            minecraft.hitResult = new EntityHitResult(aimed, at);
            minecraft.crosshairPickEntity = aimed;
        }
    }
}
