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

package com.almuradev.sgcraft.mod.client.view;

import net.minecraft.util.Mth;

/**
 * A value streamed every tick or few, eased from where it was drawn last towards each new value over the time
 * until the next is due, so that what moves in a remote view moves smoothly however the packets happen to arrive.
 */
final class RemoteViewSmoothing {

    private static final double TICK_NANOS = 50_000_000.0;

    private final double nanos;
    private double from;
    private double target;
    private long since;

    /** @param ticks how many ticks apart new values arrive */
    RemoteViewSmoothing(final int ticks) {
        this.nanos = ticks * RemoteViewSmoothing.TICK_NANOS;
    }

    /** Takes a new value, easing to it from wherever the value is now, or jumping to it if {@code snap}. */
    void set(final double value, final long now, final boolean snap) {
        this.from = snap ? value : this.value(now);
        this.target = value;
        this.since = now;
    }

    /** As {@link #set}, for an angle in degrees, which eases the short way round. */
    void setAngle(final double value, final long now, final boolean snap) {
        final double current = snap ? value : this.value(now);
        this.from = current;
        this.target = current + Mth.wrapDegrees(value - current);
        this.since = now;
    }

    /** Takes another's value and where it is easing to. */
    void copy(final RemoteViewSmoothing other) {
        this.from = other.from;
        this.target = other.target;
        this.since = other.since;
    }

    /** @return the value to draw now */
    double value(final long now) {
        final double progress = Mth.clamp((now - this.since) / this.nanos, 0.0, 1.0);
        return Mth.lerp(progress, this.from, this.target);
    }

    /** @return the last value taken */
    double target() {
        return this.target;
    }
}
