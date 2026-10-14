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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A standing gate's own frame: its origin the centre of its base block, x to the gate's right as seen from in front,
 * y up, and z out of its front. The event horizon is the plane z = 0, and the ring's centre is
 * {@link #CENTRE_Y} above the origin.
 */
public final class GateFrame {

    /** How far the ring's centre is above the centre of the base, in blocks. */
    public static final double CENTRE_Y = 2.0;

    private final Vec3 origin;
    private final Vec3 right;
    private final Vec3 front;

    public GateFrame(final BlockPos base, final Direction facing) {
        this.origin = Vec3.atCenterOf(base);
        this.right = Vec3.atLowerCornerOf(facing.getCounterClockWise().getUnitVec3i());
        this.front = Vec3.atLowerCornerOf(facing.getUnitVec3i());
    }

    /** @return a point of the world in the gate's frame */
    public Vec3 toLocal(final Vec3 world) {
        final Vec3 d = world.subtract(this.origin);
        return new Vec3(d.dot(this.right), d.y, d.dot(this.front));
    }

    /** @return a point of the gate's frame in the world */
    public Vec3 toWorld(final Vec3 local) {
        return this.origin.add(this.right.scale(local.x)).add(0, local.y, 0).add(this.front.scale(local.z));
    }

    /** @return a direction of the world in the gate's frame */
    public Vec3 directionToLocal(final Vec3 world) {
        return new Vec3(world.dot(this.right), world.y, world.dot(this.front));
    }

    /** @return a direction of the gate's frame in the world */
    public Vec3 directionToWorld(final Vec3 local) {
        return this.right.scale(local.x).add(0, local.y, 0).add(this.front.scale(local.z));
    }

    /** @return the box in the world that holds a box of the gate's frame, from one corner to the other */
    public AABB box(final Vec3 from, final Vec3 to) {
        return new AABB(this.toWorld(from), this.toWorld(to));
    }

    /** @return the block of the world at a block of the gate's frame, counted from the base block */
    public BlockPos block(final int x, final int y, final int z) {
        return BlockPos.containing(this.toWorld(new Vec3(x, y, z)));
    }
}
