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

package com.almuradev.sgcraft.mod.client.rings;

import com.almuradev.sgcraft.mod.rings.RingPayloads;

/**
 * The player's movement keys, stopped while a ring transport holds them; the mouse still turns their head. The game's
 * keyboard input asks {@link #held()} each tick.
 */
public final class RingHoldInput {

    // Let go anyway after this long, in milliseconds, should the server's word be lost
    private static final long LONGEST_HOLD = 30000;

    // When the server last said they're held, in milliseconds, or 0
    private static long heldSince;

    private RingHoldInput() {
    }

    /** The server held the player, or let them go. */
    public static void handle(final RingPayloads.Hold hold) {
        RingHoldInput.heldSince = hold.held() ? System.currentTimeMillis() : 0;
    }

    /** @return whether the player's movement keys are stopped now */
    public static boolean held() {
        if (RingHoldInput.heldSince != 0
                && System.currentTimeMillis() - RingHoldInput.heldSince > RingHoldInput.LONGEST_HOLD) {
            RingHoldInput.heldSince = 0;
        }
        return RingHoldInput.heldSince != 0;
    }

    /** Lets the player go, as the client leaves the server. */
    public static void disconnected() {
        RingHoldInput.heldSince = 0;
    }
}
