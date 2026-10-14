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

import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * The outcome of placing a gate.
 *
 * @since 0.1.0
 */
public interface PlacementResult {

    /** @return how the placement ended */
    Status status();

    /** @return the new gate, present only when {@link #status()} is {@link Status#PLACED} */
    Optional<Stargate> gate();

    /** @return why the placement failed, present only when {@link #status()} is {@link Status#RULE_FAILED} */
    Optional<Component> reason();

    /**
     * How a placement ended.
     *
     * <p>A minor version of the API may add constants, so a {@code switch} over this type needs a {@code default}
     * branch.</p>
     */
    enum Status {

        /** The gate is built and formed. */
        PLACED,

        /** Blocks are in the way everywhere it was tried, and the mode was {@link PlacementMode#ATTEMPT}. */
        OBSTRUCTED,

        /** Another gate is in the way, or already has the address the new gate would have. */
        GATE_IN_THE_WAY,

        /** The gate's type can't be built at the requested size. */
        UNSUPPORTED_SIZE,

        /** One of the type's {@linkplain PlacementRule placement rules} failed everywhere it was tried. */
        RULE_FAILED,

        /** The position is outside the world, or too far out to have an address. */
        INVALID_POSITION,

        /** The cause isn't allowed to place a gate there. */
        DENIED,

        /** A listener cancelled the placement's event. */
        CANCELLED
    }
}
