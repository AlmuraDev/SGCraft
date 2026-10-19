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

package com.almuradev.sgcraft.mod.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * What a puddle jumper's hull keeps out: it is sealed, as the show's is, flying under the sea, so those inside it, or
 * riding in it, breathe and see as in air though it is under water, and rain doesn't fall through its roof.
 */
public final class JumperShelter {

    // How far round a point to look for a jumper whose hull might hold it, in blocks
    private static final double REACH = PuddleJumperEntity.LENGTH / 2 + 1;

    private JumperShelter() {
    }

    /** @return whether an entity rides in a jumper, or stands inside one's hull with its eyes */
    public static boolean sheltered(final Entity entity) {
        if (entity instanceof PuddleJumperEntity) {
            return false;
        }
        return entity.getRootVehicle() instanceof PuddleJumperEntity
                || JumperShelter.inside(entity.level(), entity.getEyePosition());
    }

    /**
     * @return whether a player stands inside a jumper's hull, or anything rides in one, so that the water round the
     *     hull doesn't touch it; only players are looked for inside, as the look costs a search of the jumpers near
     */
    public static boolean dryInside(final Entity entity) {
        if (entity instanceof PuddleJumperEntity) {
            return false;
        }
        if (entity.getRootVehicle() instanceof PuddleJumperEntity) {
            return true;
        }
        return entity instanceof Player && !entity.level().getFluidState(entity.blockPosition()).isEmpty()
                && JumperShelter.inside(entity.level(), entity.getBoundingBox().getCenter());
    }

    /** @return whether a point lies inside a jumper's hull, between its walls, floor and roof */
    public static boolean inside(final Level level, final Vec3 point) {
        // Asked of every mob's targets and every entity in water, so a world with no jumper answers at once
        if (!JumperIndex.any(level)) {
            return false;
        }
        for (final PuddleJumperEntity jumper : JumperIndex.near(level,
                new AABB(point, point).inflate(JumperShelter.REACH))) {
            if (JumperHull.within(JumperHull.toLocal(point.subtract(jumper.position()), jumper.getYRot()))) {
                return true;
            }
        }
        return false;
    }

    /** @return the jumpers near a point, whose roofs may shelter the columns round it */
    public static List<PuddleJumperEntity> near(final Level level, final Vec3 point, final double radius) {
        return JumperIndex.near(level, new AABB(point, point).inflate(radius, radius + PuddleJumperEntity.HEIGHT,
                radius));
    }

    /**
     * @param jumpers the jumpers near the column, as {@link #near} finds them
     * @param height where the rain stops in a column now, the top of the ground
     * @return where the rain stops instead, on the roof of any of the jumpers over the column
     */
    public static int roof(final List<PuddleJumperEntity> jumpers, final int x, final int z, final int height) {
        int top = height;
        for (final PuddleJumperEntity jumper : jumpers) {
            final Vec3 column = new Vec3(x + 0.5 - jumper.getX(), 0, z + 0.5 - jumper.getZ());
            final Vec3 local = JumperHull.toLocal(column, jumper.getYRot());
            if (Math.abs(local.x) < PuddleJumperEntity.WIDTH / 2.0
                    && Math.abs(local.z) < PuddleJumperEntity.LENGTH / 2) {
                top = Math.max(top, Mth.ceil(jumper.getY() + PuddleJumperEntity.HEIGHT));
            }
        }
        return top;
    }
}
