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

package com.almuradev.sgcraft.api.address;

import java.util.Optional;

/**
 * One symbol of a gate address.
 *
 * <p>Symbols are numbered, and each has a character for writing addresses down. How a symbol looks on a gate
 * is up to the gate's type: the same symbol is a different glyph on a Milky Way gate and on a Pegasus gate.</p>
 *
 * @param index the symbol's number, from 0 to {@link #COUNT} - 1
 * @since 0.1.0
 */
public record Symbol(int index) implements Comparable<Symbol> {

    /** How many symbols there are. */
    public static final int COUNT = 37;

    /**
     * The Giza symbol, Earth's point of origin in the show, written {@code @}. It is the default symbol of the
     * default world group; see {@link WorldGroup}.
     */
    public static final Symbol GIZA = new Symbol(36);

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@";

    /**
     * @throws IllegalArgumentException if {@code index} is outside 0 to {@link #COUNT} - 1
     */
    public Symbol {
        if (index < 0 || index >= COUNT) {
            throw new IllegalArgumentException("Symbol index must be from 0 to " + (COUNT - 1) + ", not " + index);
        }
    }

    /**
     * @param character a letter (either case), digit or {@code @}
     * @return the symbol written with that character, or empty if none is
     */
    public static Optional<Symbol> of(final char character) {
        final int index = CHARACTERS.indexOf(Character.toUpperCase(character));
        return index < 0 ? Optional.empty() : Optional.of(new Symbol(index));
    }

    /** @return the character this symbol is written with */
    public char character() {
        return CHARACTERS.charAt(this.index);
    }

    @Override
    public int compareTo(final Symbol other) {
        return Integer.compare(this.index, other.index);
    }

    @Override
    public String toString() {
        return String.valueOf(this.character());
    }
}
