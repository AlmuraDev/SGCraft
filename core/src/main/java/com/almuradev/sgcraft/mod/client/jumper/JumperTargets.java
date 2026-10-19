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

import com.almuradev.sgcraft.mod.entity.JumperShelter;
import com.almuradev.sgcraft.mod.entity.PuddleJumperEntity;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The creatures a puddle jumper's weapons are aimed at, on its weapons' operator's client. They aim where they look:
 * only what lies in a cone thrown out from their camera, the way it points, in first person or third, with nothing
 * solid between, is marked, so nothing behind them, behind a hill or under the ground. Hostiles are always marked; anything else living
 * only while friendly fire is on, and never anyone aboard a jumper.
 *
 * <p>With nothing chosen the lock falls of itself on the marked creature nearest straight ahead, and follows
 * whichever that is. The target key chooses the next marked creature instead, and holds it, in the cone or out, until
 * it dies, goes, or flies out of the drones' range; pressed with nothing marked, it gives the choice back. Only clients
 * call this.</p>
 */
public final class JumperTargets {

    // The cone: how far it reaches, in blocks, and how far it opens about where the camera points, as the tangent of
    // the angle
    private static final double REACH = 128;
    private static final double OPEN = Math.tan(Math.toRadians(25));
    // How often the marked creatures are found again, in ticks; how many sight lines are traced each time, at most,
    // and for how long one found clear holds, in ticks, for a creature not reached again before they run out
    private static final int LOOK_TICKS = 2;
    private static final int SIGHT_LINES = 24;
    private static final int SEEN_TICKS = 10;

    // The marked creatures, nearest straight ahead first; the locked target, by its entity's id, or -1; and whether
    // the operator chose it, rather than it falling to the one nearest ahead
    private static List<LivingEntity> marked = List.of();
    private static int locked = -1;
    private static boolean chosen;
    private static int sinceLook;
    // Until when each creature lately found in plain sight is taken to be, by its entity's id, in game ticks
    private static Int2LongMap seenUntil = new Int2LongOpenHashMap();

    private JumperTargets() {
    }

    /** Each tick, for the jumper's weapons' operator: finds what lies in the cone again, every few ticks. */
    public static void tick(final LocalPlayer operator, final PuddleJumperEntity jumper) {
        if (++JumperTargets.sinceLook < JumperTargets.LOOK_TICKS) {
            return;
        }
        JumperTargets.sinceLook = 0;
        final Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
        final Vec3 from = camera.position();
        final Vec3 ahead = new Vec3(camera.forwardVector().x(), camera.forwardVector().y(),
                camera.forwardVector().z());
        // Every point of a creature that lies in the cone, nearest straight ahead first, looked for only in the box
        // round the cone
        final List<Aimed> inCone = new ArrayList<>();
        final AABB cone = new AABB(from, from.add(ahead.scale(JumperTargets.REACH)))
                .inflate(JumperTargets.REACH * JumperTargets.OPEN);
        for (final LivingEntity entity : jumper.level().getEntitiesOfClass(LivingEntity.class, cone,
                entity -> JumperTargets.markable(entity, operator, jumper))) {
            for (final Vec3 point : JumperTargets.points(entity)) {
                final double off = JumperTargets.offAxis(from, ahead, point);
                if (off >= 0) {
                    inCone.add(new Aimed(entity, point, off));
                }
            }
        }
        inCone.sort(Comparator.comparingDouble(Aimed::off));
        // A creature is marked by its nearest point ahead in plain sight; the sight lines are traced in that order,
        // so the creatures nearest ahead are always looked at, and one past them all holds as last seen
        final long now = jumper.level().getGameTime();
        final Int2LongMap seen = new Int2LongOpenHashMap();
        final List<LivingEntity> marked = new ArrayList<>();
        final IntSet looked = new IntOpenHashSet();
        int lines = 0;
        for (final Aimed aimed : inCone) {
            final int id = aimed.entity().getId();
            if (looked.contains(id)) {
                continue;
            }
            final boolean clear;
            if (lines < JumperTargets.SIGHT_LINES) {
                lines++;
                clear = operator.level().clip(new ClipContext(from, aimed.point(), ClipContext.Block.VISUAL,
                        ClipContext.Fluid.NONE, operator)).getType() == HitResult.Type.MISS;
                if (clear) {
                    seen.put(id, now + JumperTargets.SEEN_TICKS);
                }
            } else {
                final long until = JumperTargets.seenUntil.getOrDefault(id, Long.MIN_VALUE);
                clear = until >= now;
                if (clear) {
                    seen.put(id, until);
                }
            }
            if (clear) {
                looked.add(id);
                marked.add(aimed.entity());
            }
        }
        JumperTargets.seenUntil = seen;
        JumperTargets.marked = List.copyOf(marked);
        // Unchosen, or the chosen one gone, the lock falls on whatever is nearest straight ahead
        if (!JumperTargets.chosen || JumperTargets.locked(jumper) == null) {
            JumperTargets.chosen = false;
            JumperTargets.locked = JumperTargets.marked.isEmpty() ? -1 : JumperTargets.marked.get(0).getId();
        }
    }

    private record Aimed(LivingEntity entity, Vec3 point, double off) {
    }

    /**
     * @return how far off where the camera points a point is, as the cone's own measure from 0 straight ahead to 1 at
     *     its edge; or -1 if it isn't in the cone
     */
    private static double offAxis(final Vec3 from, final Vec3 ahead, final Vec3 point) {
        final Vec3 to = point.subtract(from);
        final double along = to.dot(ahead);
        if (along <= 0 || along > JumperTargets.REACH) {
            return -1;
        }
        // How far it lies off the camera's line, for each block along it
        final double off = to.subtract(ahead.scale(along)).length() / along / JumperTargets.OPEN;
        return off > 1 ? -1 : off;
    }

    // The points of a creature that may be seen: its middle and its eyes, or each of the Ender Dragon's parts
    private static List<Vec3> points(final LivingEntity entity) {
        if (entity instanceof final EnderDragon dragon) {
            final List<Vec3> points = new ArrayList<>();
            for (final EnderDragonPart part : dragon.getSubEntities()) {
                points.add(part.getBoundingBox().getCenter());
            }
            return points;
        }
        return List.of(entity.getBoundingBox().getCenter(), entity.getEyePosition());
    }

    /** Lets go of the marks and the lock, as for someone who no longer works the weapons. */
    public static void clear() {
        JumperTargets.marked = List.of();
        JumperTargets.seenUntil = new Int2LongOpenHashMap();
        JumperTargets.locked = -1;
        JumperTargets.chosen = false;
    }

    /** @return the creatures marked, nearest straight ahead first */
    public static List<LivingEntity> marked() {
        return JumperTargets.marked;
    }

    /** @return whether a creature is hostile, rather than marked only for friendly fire */
    public static boolean hostile(final Entity entity) {
        return entity instanceof Enemy;
    }

    // What may be marked: a living creature, hostile or, with friendly fire on, anything but the operator and those
    // aboard a jumper
    private static boolean markable(final LivingEntity entity, final LocalPlayer operator,
            final PuddleJumperEntity jumper) {
        if (!entity.isAlive() || entity.isInvisible() || entity == operator || entity instanceof ArmorStand
                || entity instanceof final Player player && player.isSpectator() || JumperShelter.sheltered(entity)) {
            return false;
        }
        return JumperTargets.hostile(entity) || jumper.friendlyFire();
    }

    /**
     * Chooses the next marked creature after the one locked, and holds it; with nothing marked, gives the choice back,
     * so the lock falls again on whatever comes nearest ahead.
     */
    public static void cycle(final LocalPlayer operator, final PuddleJumperEntity jumper) {
        JumperTargets.sinceLook = JumperTargets.LOOK_TICKS;
        JumperTargets.tick(operator, jumper);
        final List<LivingEntity> marked = JumperTargets.marked;
        if (marked.isEmpty()) {
            JumperTargets.clear();
            return;
        }
        int at = -1;
        for (int i = 0; i < marked.size(); i++) {
            if (marked.get(i).getId() == JumperTargets.locked) {
                at = i;
            }
        }
        JumperTargets.locked = marked.get((at + 1) % marked.size()).getId();
        JumperTargets.chosen = true;
    }

    /** @return the locked target, if it is still there and in the drones' range */
    public static @Nullable LivingEntity locked(final PuddleJumperEntity jumper) {
        if (JumperTargets.locked < 0) {
            return null;
        }
        final Entity target = jumper.level().getEntity(JumperTargets.locked);
        if (!(target instanceof final LivingEntity living) || !living.isAlive()
                || !JumperTargets.hostile(living) && !jumper.friendlyFire()
                || target.distanceToSqr(jumper) > PuddleJumperEntity.DRONE_RANGE * PuddleJumperEntity.DRONE_RANGE) {
            return null;
        }
        return living;
    }

    /** @return whether the operator chose the locked target, rather than it falling to the one nearest ahead */
    public static boolean chosen() {
        return JumperTargets.chosen;
    }
}
