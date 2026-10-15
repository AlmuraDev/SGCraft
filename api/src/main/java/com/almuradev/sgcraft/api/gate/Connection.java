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

import com.almuradev.sgcraft.api.address.Address;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * A link between two gates, from the moment dialing starts until the wormhole closes. Both gates return the same
 * connection.
 *
 * @since 0.1.0
 */
public interface Connection {

    /** @return the gate that dialed */
    Stargate dialer();

    /** @return the gate that was dialed */
    Stargate target();

    /** @return the address the dialing gate dialed */
    Address address();

    /**
     * @param gate one of the two gates
     * @return the gate at the other end, or empty if {@code gate} is neither end of this connection
     */
    Optional<Stargate> other(final Stargate gate);

    /** @return how many ticks the wormhole has been open, or 0 before it opens */
    int ticksOpen();

    /** @return how many ticks the wormhole has left before it closes on its own, or empty if it has no limit */
    OptionalInt ticksRemaining();
}
