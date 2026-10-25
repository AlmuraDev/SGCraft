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

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

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
        return JumperShelter.around(level, point) != null;
    }

    /** @return the jumper whose hull a point lies inside, between its walls, floor and roof, if any */
    public static @Nullable PuddleJumperEntity around(final Level level, final Vec3 point) {
        // Asked of every mob's targets and every entity in water, so a world with no jumper answers at once
        if (!JumperIndex.any(level)) {
            return null;
        }
        for (final PuddleJumperEntity jumper : JumperIndex.near(level,
                new AABB(point, point).inflate(JumperShelter.REACH))) {
            if (JumperHull.within(JumperHull.toLocal(point.subtract(jumper.position()), jumper.getYRot()))) {
                return jumper;
            }
        }
        return null;
    }

    /**
     * @return whether an entity rides in, or stands inside, a cloaked jumper, which hides it from mobs; one that isn't
     *     cloaked, mobs may still go after
     */
    public static boolean hidden(final Entity entity) {
        final PuddleJumperEntity jumper = JumperShelter.shelter(entity);
        return jumper != null && jumper.cloaked();
    }

    /**
     * A blow from outside a jumper, at a player riding in it or standing inside it, falls on the jumper instead: on its
     * shield if it is up, else on its hull. A blow struck from inside, or one that comes from nowhere, such as hunger,
     * reaches the player, as does any blow while the cargo door is down; an explosion has already struck the jumper
     * itself, so it is only kept off them.
     *
     * @return whether the jumper took the blow, so the player takes none
     */
    public static boolean takeBlow(final Player player, final ServerLevel level, final DamageSource source,
            final float damage) {
        final PuddleJumperEntity jumper = JumperShelter.shelter(player);
        // With its cargo door down, the cabin is open, and blows reach those inside
        if (jumper == null || jumper.hatchOpen() || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        // Whether the blow came from inside is told by who struck it, not by where it landed: an arrow that has
        // reached a rider is inside the hull by then, though it was shot from outside
        final Entity attacker = source.getEntity();
        final boolean thrown = source.getDirectEntity() != null && source.getDirectEntity() != attacker;
        final Vec3 from = attacker != null ? attacker.getEyePosition() : source.getSourcePosition();
        if (from == null || attacker != null && (attacker.getRootVehicle() == jumper || jumper.inside(from))
                || attacker == null && !thrown && jumper.inside(from)) {
            return false;
        }
        if (!source.is(DamageTypeTags.IS_EXPLOSION)) {
            jumper.hurtFrom(level, source, damage, from);
        }
        return true;
    }

    /** @return the jumper an entity rides in, or stands inside the hull of with its eyes, if any */
    public static @Nullable PuddleJumperEntity shelter(final Entity entity) {
        if (entity instanceof PuddleJumperEntity) {
            return null;
        }
        return entity.getRootVehicle() instanceof final PuddleJumperEntity jumper ? jumper
                : JumperShelter.around(entity.level(), entity.getEyePosition());
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
