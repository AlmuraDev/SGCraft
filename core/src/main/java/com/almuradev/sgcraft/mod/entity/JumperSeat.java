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

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * The seats of a puddle jumper, as on the show. Up front in the cockpit, side by side and facing ahead, sit the pilot
 * at the helm, on the left, and the co-pilot, who works the dialer, on the right, with the center console between
 * them and the computer's screen on the dashboard before the co-pilot; two more chairs stand behind theirs. Behind the
 * bulkhead, the rear compartment has a bench along each wall, three seats to each, facing across the aisle.
 */
public enum JumperSeat implements StringRepresentable {

    HELM(0.5625, 2.375, 0),
    DIALER(-0.5625, 2.375, 0),
    COCKPIT_LEFT(0.6875, 1.375, 0),
    COCKPIT_RIGHT(-0.6875, 1.375, 0),
    CABIN_LEFT_FORE(1.0, -0.125, 90),
    CABIN_RIGHT_FORE(-1.0, -0.125, -90),
    CABIN_LEFT_MIDDLE(1.0, -1, 90),
    CABIN_RIGHT_MIDDLE(-1.0, -1, -90),
    CABIN_LEFT_AFT(1.0, -1.875, 90),
    CABIN_RIGHT_AFT(-1.0, -1.875, -90);

    public static final Codec<JumperSeat> CODEC = StringRepresentable.fromEnum(JumperSeat::values);

    /** How high the seats' cushions are above the jumper's underside, where a sitter's hips go. */
    public static final double HEIGHT = 0.7;
    /** How high the floor is above the jumper's underside, where those walking about inside stand. */
    public static final double FLOOR = 0.25;

    private final Vec3 offset;
    private final float facing;
    private final String name;

    JumperSeat(final double left, final double forward, final float facing) {
        this.offset = new Vec3(left, JumperSeat.HEIGHT, forward);
        this.facing = facing;
        this.name = this.name().toLowerCase(Locale.ROOT);
    }

    /** @return which way a sitter faces, in degrees clockwise from the jumper's ahead, seen from above */
    public float facing() {
        return this.facing;
    }

    /**
     * @return where a sitter's hips go, from the jumper's middle underside, with the jumper facing south, as entities
     *     are when their yaw is 0: x to the jumper's left, y up and z ahead
     */
    public Vec3 offset() {
        return this.offset;
    }

    /** @return whether a player in this seat may work the computer */
    public boolean atComputer() {
        return this == JumperSeat.HELM || this == JumperSeat.DIALER;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    /** @return the seat of that name, or null if there is none */
    public static @Nullable JumperSeat byName(final String name) {
        for (final JumperSeat seat : JumperSeat.values()) {
            if (seat.name.equals(name)) {
                return seat;
            }
        }
        return null;
    }
}
