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

/**
 * The steps of a dial, as the original SGCraft has them.
 */
public enum DialPhase {
    /** Doing nothing. */
    IDLE,
    /** The dialing gate's ring is moving to a symbol, or its light is running to a chevron. */
    DIALING,
    /** A chevron has just locked, and the next symbol hasn't started. */
    INTER_DIALING,
    /** Every chevron has locked, and the gates are waiting on each other. */
    SYNC_AWAIT,
    /** The wormhole is forming. */
    ESTABLISHING,
    /** The wormhole has just burst open: the kawoosh. */
    TRANSIENT,
    /** The wormhole is open. */
    CONNECTED,
    /** The wormhole is shutting down. */
    DISCONNECTING
}
