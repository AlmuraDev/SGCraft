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

package com.almuradev.sgcraft.mod.gate;

import com.mojang.serialization.Codec;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * Whether a gate stands up or lies flat, as the original's {@code gateOrientation}: 1 standing, 2 flat facing up, 3
 * flat facing down. A flat gate's ring lies at its base's height and runs out from the base along the base's back.
 * Whichever way a gate lies, its own frame is a standing gate's (see {@link GateFrame}); only the turn from that frame
 * into the world differs.
 */
public enum GateOrientation implements StringRepresentable {
    /** Upright, its front the way its base faces. */
    STANDING,
    /** Flat, its front up. */
    FACE_UP,
    /** Flat, its front down. */
    FACE_DOWN;

    public static final Codec<GateOrientation> CODEC = StringRepresentable.fromEnum(GateOrientation::values);

    /** @return whether the gate lies flat */
    public boolean isFlat() {
        return this != GateOrientation.STANDING;
    }

    /**
     * @param facing the way the gate's base faces
     * @return which way the gate's front and the top of its ring point, as the API gives it
     */
    public FrontAndTop frontAndTop(final Direction facing) {
        return switch (this) {
            case STANDING -> FrontAndTop.fromFrontAndTop(facing, Direction.UP);
            case FACE_UP -> FrontAndTop.fromFrontAndTop(Direction.UP, facing.getOpposite());
            case FACE_DOWN -> FrontAndTop.fromFrontAndTop(Direction.DOWN, facing.getOpposite());
        };
    }

    /** @return how a gate of the API's orientation lies */
    public static GateOrientation of(final FrontAndTop orientation) {
        return switch (orientation.front()) {
            case UP -> GateOrientation.FACE_UP;
            case DOWN -> GateOrientation.FACE_DOWN;
            default -> GateOrientation.STANDING;
        };
    }

    /** @return the way the base faces of a gate of the API's orientation: away from the top of a flat gate's ring */
    public static Direction facing(final FrontAndTop orientation) {
        return orientation.front().getAxis().isVertical() ? orientation.top().getOpposite() : orientation.front();
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
