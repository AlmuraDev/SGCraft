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

/**
 * How a dial ended.
 *
 * <p>A minor version of the API may add constants, so a {@code switch} over this type needs a {@code default}
 * branch.</p>
 *
 * @since 0.1.0
 */
public enum DialResult {

    /** The wormhole is open. */
    CONNECTED,

    /** No reach is registered for the address's length. */
    INVALID_ADDRESS,

    /** No address reaches the chosen gate from the dialing gate. */
    UNREACHABLE,

    /** The gate has fewer chevrons than the address has symbols. */
    NOT_ENOUGH_CHEVRONS,

    /** The address points nowhere, or no gate is there. */
    NO_GATE,

    /** The address points back at the dialing gate. */
    SAME_GATE,

    /** The dialing gate isn't {@linkplain GateState#IDLE idle}. */
    BUSY,

    /** The gate at the address isn't idle. */
    TARGET_BUSY,

    /**
     * The two gates can't connect, as one of their types refuses the other; see {@link GateType#connectsTo}.
     */
    INCOMPATIBLE,

    /** The dialing gate doesn't have the energy to make the connection. */
    NOT_ENOUGH_ENERGY,

    /** The cause isn't allowed to dial this gate, or to reach the gate at the address. */
    DENIED,

    /** A listener canceled the dial's event. */
    CANCELED,

    /** Either gate {@linkplain Stargate#disconnect disconnected} before the wormhole opened. */
    INTERRUPTED,

    /** One of the two gates was broken before the wormhole opened. */
    GATE_GONE
}
