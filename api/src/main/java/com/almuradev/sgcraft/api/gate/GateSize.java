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

package com.almuradev.sgcraft.api.gate;

import java.util.Optional;

/**
 * How big a gate's ring is, as its radius in blocks from the center of the ring to its edge.
 *
 * <p>The API defines the standard size, which is also the smallest. It sets no maximum. Which sizes a gate can be
 * built at is up to its {@linkplain GateType#supports(GateSize) type}, and implementations may let the server's
 * configuration limit sizes further.</p>
 *
 * @param radius the ring's radius in blocks
 * @since 0.1.0
 */
public record GateSize(int radius) {

    /** The radius of a standard gate, and the smallest radius of any gate. */
    public static final int MIN_RADIUS = 2;

    /** A standard gate. */
    public static final GateSize STANDARD = new GateSize(GateSize.MIN_RADIUS);

    /**
     * @throws IllegalArgumentException if {@code radius} is below {@link #MIN_RADIUS}
     */
    public GateSize {
        if (radius < GateSize.MIN_RADIUS) {
            throw new IllegalArgumentException(
                    "A gate's radius is at least " + GateSize.MIN_RADIUS + ", not " + radius);
        }
    }

    /**
     * @param radius a radius in blocks
     * @return the size, or empty if {@code radius} is below {@link #MIN_RADIUS}
     */
    public static Optional<GateSize> of(final int radius) {
        return radius < GateSize.MIN_RADIUS ? Optional.empty() : Optional.of(new GateSize(radius));
    }

    /** @return how many blocks wide and tall the ring is */
    public int blocksAcross() {
        return this.radius * 2 + 1;
    }
}
