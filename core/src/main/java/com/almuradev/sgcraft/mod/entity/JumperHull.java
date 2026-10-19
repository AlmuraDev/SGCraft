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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * The solid parts of a puddle jumper's hull, which players and creatures stand on and walk among: its floor, its
 * walls and roof, the dashboard across the nose and the center console behind it, the nose itself, raked back as it
 * rises, the bulkhead between the cockpit and the rear compartment with its doorway and the consoles before it, and
 * the cargo door in the back while it is shut. Lowered, the hatch is a ramp out behind the open cabin,
 * solid to walk on.
 *
 * <p>The parts are boxes in the jumper's own frame, in blocks from its middle underside: x to its left, y up and z
 * ahead. Turned with the jumper, a box that doesn't lie along the world's axes is cut into narrow pieces, each stood
 * in by the box around it, so the walls step a little where the jumper faces between the compass points.</p>
 */
public final class JumperHull {

    /** How far the lowered hatch reaches out behind the hull, in blocks. */
    public static final double RAMP_LENGTH = 2.55;

    // How far out the walls' insides are, and the floor's top and the roof's underside
    private static final double INSIDE = 1.25;
    private static final double OUTSIDE = PuddleJumperEntity.WIDTH / 2.0;
    private static final double FLOOR = 0.25;
    private static final double ROOF = 2.4;
    private static final double TOP = PuddleJumperEntity.HEIGHT;
    private static final double NOSE = PuddleJumperEntity.LENGTH / 2;
    // How far the nose and the cargo door rake in for each block up, and how high each step of them is
    private static final double RAKE = 0.25;
    private static final double STEP = 0.5;
    // How high the windshield's sill is, which the dashboard and the center console stand to
    private static final double SILL = 1.6;
    // The size of the pieces a turned box is cut into, in blocks: small enough that the box round each reaches only a
    // little past the piece, so the bulkhead's doorway and the way past the center console stay wide enough to walk
    // through, and no one walking along a wall is knocked back and forth by its steps
    private static final double PIECE = 0.2;
    // How far behind the middle the hatch's hinge is, set in behind the lip of the hull, and the top of the lowered
    // hatch
    private static final double HINGE = JumperHull.NOSE - 0.125;
    private static final double RAMP_TOP = 0.15;
    // The bulkhead, ahead of the middle: its front and back, and its doorway's half width and height
    private static final double BULKHEAD_FRONT = 0.75;
    private static final double BULKHEAD_BACK = 0.625;
    private static final double DOOR_HALF = 0.4375;
    private static final double DOOR_TOP = 2.125;

    private static final List<AABB> ALWAYS = JumperHull.always();
    private static final List<AABB> HATCH = JumperHull.raked(-1);
    // The hatch lowered: a ramp out behind the cabin, as long as the hatch and level with its hinge, which holds up
    // whoever walks on it even while the jumper hangs in the air
    private static final List<AABB> RAMP = List.of(
            new AABB(-JumperHull.INSIDE, 0, -JumperHull.HINGE - JumperHull.RAMP_LENGTH, JumperHull.INSIDE,
                    JumperHull.RAMP_TOP, -JumperHull.HINGE));

    private static List<AABB> always() {
        final List<AABB> boxes = new ArrayList<>(List.of(
                // The floor, the walls and the roof
                new AABB(-JumperHull.INSIDE, 0, -JumperHull.NOSE, JumperHull.INSIDE, JumperHull.FLOOR, JumperHull.NOSE),
                new AABB(JumperHull.INSIDE, 0, -3.2, JumperHull.OUTSIDE, JumperHull.TOP, 3.2),
                new AABB(-JumperHull.OUTSIDE, 0, -3.2, -JumperHull.INSIDE, JumperHull.TOP, 3.2),
                new AABB(-JumperHull.OUTSIDE, JumperHull.ROOF, -2.9, JumperHull.OUTSIDE, JumperHull.TOP, 2.9),
                // The dashboard across the nose, and the center console between the front seats, each standing as
                // high as the windshield's sill, so no one walking in the cockpit steps or jumps up onto them and into
                // the nose's low arch
                new AABB(-JumperHull.INSIDE, 0, 2.75, JumperHull.INSIDE, JumperHull.SILL, JumperHull.NOSE),
                new AABB(-0.25, 0, 2.0, 0.25, JumperHull.SILL, 2.75),
                // The bulkhead, either side of its doorway and over it, and the consoles before it
                new AABB(JumperHull.DOOR_HALF, JumperHull.FLOOR, JumperHull.BULKHEAD_BACK, JumperHull.INSIDE,
                        JumperHull.ROOF, JumperHull.BULKHEAD_FRONT),
                new AABB(-JumperHull.INSIDE, JumperHull.FLOOR, JumperHull.BULKHEAD_BACK, -JumperHull.DOOR_HALF,
                        JumperHull.ROOF, JumperHull.BULKHEAD_FRONT),
                new AABB(-JumperHull.DOOR_HALF, JumperHull.DOOR_TOP, JumperHull.BULKHEAD_BACK, JumperHull.DOOR_HALF,
                        JumperHull.ROOF, JumperHull.BULKHEAD_FRONT),
                new AABB(0.5, 0, JumperHull.BULKHEAD_FRONT, 1.1875, 1.625, 1.0),
                new AABB(-1.1875, 0, JumperHull.BULKHEAD_FRONT, -0.5, 1.625, 1.0)));
        // The nose, raked back as it rises
        boxes.addAll(JumperHull.raked(1));
        return List.copyOf(boxes);
    }

    // An end of the hull, raked in as it rises, in steps: the nose ahead, or the shut cargo door behind
    private static List<AABB> raked(final int end) {
        final List<AABB> boxes = new ArrayList<>();
        for (double low = 0; low < JumperHull.TOP - 1.0E-6; low += JumperHull.STEP) {
            final double high = Math.min(JumperHull.TOP, low + JumperHull.STEP);
            final double face = JumperHull.NOSE - high * JumperHull.RAKE - 0.1;
            boxes.add(end > 0 ? new AABB(-JumperHull.OUTSIDE, low, face, JumperHull.OUTSIDE, high, JumperHull.NOSE)
                    : new AABB(-JumperHull.OUTSIDE, low, -JumperHull.NOSE, JumperHull.OUTSIDE, high, -face));
        }
        return List.copyOf(boxes);
    }

    private JumperHull() {
    }

    /**
     * @param yaw which way the jumper faces
     * @param hatchShut whether its hatch is shut; if not, it lies lowered as a ramp
     * @return the hull's solid parts, in the world's frame from the jumper's middle underside
     */
    public static List<VoxelShape> shapes(final float yaw, final boolean hatchShut) {
        final List<VoxelShape> shapes = new ArrayList<>();
        // Within half a degree of a compass point, each box is turned whole
        final float turn = Mth.wrapDegrees(yaw);
        final boolean square = Math.abs(turn - Math.round(turn / 90) * 90) < 0.5F;
        final double angle = (square ? Math.round(turn / 90) * 90 : turn) * Mth.DEG_TO_RAD;
        JumperHull.add(shapes, JumperHull.ALWAYS, angle, square);
        JumperHull.add(shapes, hatchShut ? JumperHull.HATCH : JumperHull.RAMP, angle, square);
        return shapes;
    }

    private static void add(final List<VoxelShape> shapes, final List<AABB> boxes, final double angle,
            final boolean square) {
        for (final AABB box : boxes) {
            final int across = square ? 1 : Math.max(1, Mth.ceil(box.getXsize() / JumperHull.PIECE));
            final int along = square ? 1 : Math.max(1, Mth.ceil(box.getZsize() / JumperHull.PIECE));
            for (int i = 0; i < across; i++) {
                for (int j = 0; j < along; j++) {
                    final double x0 = box.minX + box.getXsize() * i / across;
                    final double x1 = box.minX + box.getXsize() * (i + 1) / across;
                    final double z0 = box.minZ + box.getZsize() * j / along;
                    final double z1 = box.minZ + box.getZsize() * (j + 1) / along;
                    shapes.add(JumperHull.turned(x0, x1, box.minY, box.maxY, z0, z1, angle));
                }
            }
        }
    }

    // The box around a piece turned with the jumper
    private static VoxelShape turned(final double x0, final double x1, final double y0, final double y1,
            final double z0, final double z1, final double angle) {
        double minX = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        for (final double x : new double[] {x0, x1}) {
            for (final double z : new double[] {z0, z1}) {
                final Vec3 world = JumperHull.toWorld(new Vec3(x, 0, z), angle);
                minX = Math.min(minX, world.x);
                maxX = Math.max(maxX, world.x);
                minZ = Math.min(minZ, world.z);
                maxZ = Math.max(maxZ, world.z);
            }
        }
        return Shapes.box(minX, y0, minZ, maxX, y1, maxZ);
    }

    /** @return a point in the jumper's frame, turned into the world's, still from the jumper's middle underside */
    public static Vec3 toWorld(final Vec3 local, final double angle) {
        return local.yRot((float) -angle);
    }

    /** @return a point from the jumper's middle underside, in the world's frame, turned into the jumper's */
    public static Vec3 toLocal(final Vec3 offset, final float yaw) {
        return offset.yRot(yaw * Mth.DEG_TO_RAD);
    }

    /** @return whether a point in the jumper's frame is inside its hull, between its walls, floor and roof */
    public static boolean inside(final Vec3 local) {
        return Math.abs(local.x) < JumperHull.INSIDE && local.y > JumperHull.FLOOR && local.y < JumperHull.ROOF
                && local.z > -JumperHull.NOSE && local.z < JumperHull.NOSE;
    }

    /** @return how far a point in the jumper's frame is inside its hull, to the nearest of its sides, in blocks */
    public static double depthInside(final Vec3 local) {
        return Math.min(Math.min(JumperHull.OUTSIDE - Math.abs(local.x), JumperHull.NOSE - Math.abs(local.z)),
                Math.min(local.y, JumperHull.TOP - local.y));
    }

    /** @return whether a point in the jumper's frame is anywhere within its hull, walls and all */
    public static boolean within(final Vec3 local) {
        return Math.abs(local.x) < JumperHull.OUTSIDE && local.y > 0 && local.y < JumperHull.TOP
                && Math.abs(local.z) < JumperHull.NOSE;
    }
}
