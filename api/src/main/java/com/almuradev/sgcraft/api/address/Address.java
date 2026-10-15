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

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A gate address: the symbols dialed to reach a gate, in dialing order.
 *
 * <p>An address is only symbols. What they mean is up to the {@link AddressReach} with that many symbols, which
 * {@link Addressing#reachOf(Address)} returns. An address is always relative to the gate that dials it: the same
 * symbols can reach a different gate from another world.</p>
 *
 * <p>Written down, an address is grouped as {@code XXXX-XXX}, then a dash before any symbols after the seventh,
 * such as {@code XXXX-XXX-XX}. {@link #parse(String)} reads any grouping and ignores case, dashes and spaces.</p>
 *
 * @param symbols the symbols in dialing order; at least one
 * @since 0.1.0
 */
public record Address(List<Symbol> symbols) implements Iterable<Symbol> {

    /** How an address is saved: as text, the way {@link #toString()} writes it. */
    public static final Codec<Address> CODEC = Codec.STRING.comapFlatMap(
            text -> Address.parse(text)
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Not an address: " + text)),
            Address::toString);

    /**
     * @throws IllegalArgumentException if there are no symbols
     */
    public Address {
        symbols = List.copyOf(Objects.requireNonNull(symbols, "symbols"));
        if (symbols.isEmpty()) {
            throw new IllegalArgumentException("An address has at least one symbol");
        }
    }

    /**
     * @param text an address as written, such as {@code AB12-CDE-F}
     * @return the address, or empty if the text has a character that isn't a symbol, or no symbols at all
     */
    public static Optional<Address> parse(final String text) {
        final List<Symbol> symbols = new ArrayList<>();
        for (final char character : Objects.requireNonNull(text, "text").toCharArray()) {
            if (character == '-' || Character.isWhitespace(character)) {
                continue;
            }
            final Optional<Symbol> symbol = Symbol.of(character);
            if (symbol.isEmpty()) {
                return Optional.empty();
            }
            symbols.add(symbol.get());
        }
        return symbols.isEmpty() ? Optional.empty() : Optional.of(new Address(symbols));
    }

    /** @return how many symbols this address has */
    public int size() {
        return this.symbols.size();
    }

    /**
     * @param index a position in dialing order, from 0
     * @return the symbol dialed there
     * @throws IndexOutOfBoundsException if {@code index} is outside 0 to {@link #size()} - 1
     */
    public Symbol get(final int index) {
        return this.symbols.get(index);
    }

    /** @return the symbols in dialing order */
    @Override
    public Iterator<Symbol> iterator() {
        return this.symbols.iterator();
    }

    /** @return the address as written, such as {@code AB12-CDE-F} */
    @Override
    public String toString() {
        final StringBuilder text = new StringBuilder(this.symbols.size() + 2);
        for (int i = 0; i < this.symbols.size(); i++) {
            if (i == 4 || i == 7) {
                text.append('-');
            }
            text.append(this.symbols.get(i).character());
        }
        return text.toString();
    }
}
