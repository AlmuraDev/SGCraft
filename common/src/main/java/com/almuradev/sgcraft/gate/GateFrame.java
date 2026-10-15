/*
 * This file is part of SGCraft, licensed under the MIT License (MIT).
 * It is based on the original SGCraft by gcewing.
 *
 * Copyright (c) AlmuraDev <https://github.com/AlmuraDev>
 * Copyright (c) 2016 gcewing <https://github.com/gcewing>
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

package com.almuradev.sgcraft.gate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A gate's own frame: its origin the center of its base block, x to the gate's right as seen from in front, y up its
 * ring from the base, and z out of its front. The event horizon is the plane z = 0, and the ring's center is its
 * {@linkplain #shape() shape's} {@linkplain GateShape#centerY() center height} up the ring from the origin.
 *
 * <p>A standing gate's frame has y up and z the way its base faces. A gate lying flat (see {@link GateOrientation})
 * has y along its base's back, where its ring runs out, and z up or down; facing down, x is turned round too, so the
 * frame stays the same handedness. Everything measured in the frame, such as travel, the horizon and the kawoosh, is
 * so the same for every gate.</p>
 */
public final class GateFrame {

    private final BlockPos base;
    private final Vec3 origin;
    private final Vec3 right;
    private final Vec3 up;
    private final Vec3 front;
    private final Direction facing;
    private final GateOrientation orientation;
    private final GateShape shape;

    public GateFrame(final BlockPos base, final Direction facing) {
        this(base, facing, GateShape.STANDARD);
    }

    public GateFrame(final BlockPos base, final Direction facing, final GateShape shape) {
        this(base, facing, GateOrientation.STANDING, shape);
    }

    /**
     * @param facing the way the base faces
     * @param orientation whether the gate stands or lies flat
     */
    public GateFrame(final BlockPos base, final Direction facing, final GateOrientation orientation,
            final GateShape shape) {
        this.base = base.immutable();
        this.shape = shape;
        this.facing = facing;
        this.orientation = orientation;
        this.origin = Vec3.atCenterOf(base);
        final Vec3 side = Vec3.atLowerCornerOf(facing.getCounterClockWise().getUnitVec3i());
        final Vec3 out = Vec3.atLowerCornerOf(facing.getUnitVec3i());
        final Vec3 sky = new Vec3(0, 1, 0);
        switch (orientation) {
            case FACE_UP -> {
                this.right = side;
                this.up = out.reverse();
                this.front = sky;
            }
            case FACE_DOWN -> {
                this.right = side.reverse();
                this.up = out.reverse();
                this.front = sky.reverse();
            }
            default -> {
                this.right = side;
                this.up = sky;
                this.front = out;
            }
        }
    }

    /** @return the frame of a gate of the API's orientation */
    public static GateFrame of(final BlockPos base, final FrontAndTop orientation, final GateShape shape) {
        return new GateFrame(base, GateOrientation.facing(orientation), GateOrientation.of(orientation), shape);
    }

    /** @return the way the gate's base faces, which is the gate's front only while it stands */
    public Direction facing() {
        return this.facing;
    }

    /** @return whether the gate stands or lies flat */
    public GateOrientation orientation() {
        return this.orientation;
    }

    /** @return the way the gate's front faces in the world */
    public Direction front() {
        return Direction.getApproximateNearest(this.front);
    }

    /** @return the way the top of the gate's ring points in the world */
    public Direction top() {
        return Direction.getApproximateNearest(this.up);
    }

    /** @return the API's orientation of the gate */
    public FrontAndTop frontAndTop() {
        return this.orientation.frontAndTop(this.facing);
    }

    /** @return the frame of the same base standing up, the base block's own frame, whichever way the gate lies */
    public GateFrame upright() {
        return this.orientation == GateOrientation.STANDING ? this
                : new GateFrame(this.base, this.facing, GateOrientation.STANDING, this.shape);
    }

    /** @return how big the gate is and how it is laid out */
    public GateShape shape() {
        return this.shape;
    }

    /** @return a point of the world in the gate's frame */
    public Vec3 toLocal(final Vec3 world) {
        return this.directionToLocal(world.subtract(this.origin));
    }

    /** @return a point of the gate's frame in the world */
    public Vec3 toWorld(final Vec3 local) {
        return this.origin.add(this.directionToWorld(local));
    }

    /** @return a direction of the world in the gate's frame */
    public Vec3 directionToLocal(final Vec3 world) {
        return new Vec3(world.dot(this.right), world.dot(this.up), world.dot(this.front));
    }

    /** @return a direction of the gate's frame in the world */
    public Vec3 directionToWorld(final Vec3 local) {
        return this.right.scale(local.x).add(this.up.scale(local.y)).add(this.front.scale(local.z));
    }

    /** @return the box in the world that holds a box of the gate's frame, from one corner to the other */
    public AABB box(final Vec3 from, final Vec3 to) {
        return new AABB(this.toWorld(from), this.toWorld(to));
    }

    /** @return the block of the world at a block of the gate's frame, counted from the base block */
    public BlockPos block(final int x, final int y, final int z) {
        return BlockPos.containing(this.toWorld(new Vec3(x, y, z)));
    }

    /** @return where a block of the world is in the gate's frame, counted from the base block */
    public BlockPos local(final BlockPos pos) {
        return BlockPos.containing(this.toLocal(Vec3.atCenterOf(pos)).add(0.5, 0.5, 0.5));
    }
}
