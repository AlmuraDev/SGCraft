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

/**
 * The order a gate's chevrons lock in, as on the show: the chevrons to the right of the top one, going down, then
 * those on the left, going up, then the two at the bottom of a gate with nine; the top chevron always locks last.
 *
 * <p>Chevrons are numbered by position, 0 to 8, with 4 at the top; each is 40 degrees anticlockwise from the one
 * after it. A gate with seven chevrons has positions 1 to 7, and one with more has 0 to 8.</p>
 */
public final class ChevronOrder {

    /** The position of the top chevron. */
    public static final int TOP = 4;
    /** How many chevron positions there are. */
    public static final int POSITIONS = 9;

    private static final int[] ORDER = {5, 6, 7, 1, 2, 3, 8, 0};

    private ChevronOrder() {
    }

    /** @return the first chevron position of a gate with {@code chevrons} chevrons */
    public static int first(final int chevrons) {
        return chevrons > 7 ? 0 : 1;
    }

    /**
     * @param chevrons how many chevrons the gate has
     * @param symbols how many symbols are being dialed
     * @return the position of the chevron each symbol locks at, in dialing order; the last symbol locks at the top
     */
    public static int[] positions(final int chevrons, final int symbols) {
        final int[] positions = new int[Math.max(0, symbols)];
        if (symbols <= 0) {
            return positions;
        }
        final int first = ChevronOrder.first(chevrons);
        int next = 0;
        for (final int position : ChevronOrder.ORDER) {
            if (next == symbols - 1) {
                break;
            }
            if (position >= first && position < first + chevrons) {
                positions[next++] = position;
            }
        }
        // A gate with too few chevrons for the address locks the rest at the top
        while (next < symbols) {
            positions[next++] = ChevronOrder.TOP;
        }
        return positions;
    }

    /**
     * @param chevrons how many chevrons the gate has
     * @param symbols how many symbols are being dialed
     * @param locked how many have locked
     * @return whether each position's chevron is locked, by position
     */
    public static boolean[] locked(final int chevrons, final int symbols, final int locked) {
        final boolean[] lit = new boolean[ChevronOrder.POSITIONS];
        final int[] positions = ChevronOrder.positions(chevrons, symbols);
        for (int i = 0; i < Math.min(locked, positions.length); i++) {
            lit[positions[i]] = true;
        }
        return lit;
    }

    /** @return the angle of a chevron position, in degrees anticlockwise from the right, with the top at 90 */
    public static double angle(final int position) {
        return 90 - (position - ChevronOrder.TOP) * 40.0;
    }
}
