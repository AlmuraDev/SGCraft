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

package com.almuradev.sgcraft.mod.address;

import com.almuradev.sgcraft.api.address.Symbol;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * How one world is laid out into addresses: how many chunks wide and long each addressed cell is, the symbol its
 * addresses end with, and whether the server's secret key orders its cells. Changing any of them changes every address
 * in the world.
 *
 * @param cellChunks how many chunks wide and long each addressed cell is, from 1 to {@link #MAX_CELL_CHUNKS}
 * @param pointOfOrigin the symbol every address dialed from the world ends with, as one character, or {@code ""} for
 *     addresses without one
 * @param secret whether the cells are given their addresses in an order only the server's secret key can follow, so
 *     no one can work out where a gate is from its address
 */
public record AddressLayout(int cellChunks, String pointOfOrigin, boolean secret) {

    /** The most chunks wide and long a cell can be. */
    public static final int MAX_CELL_CHUNKS = 256;

    public static final AddressLayout DEFAULTS = new AddressLayout(8, "@");

    /** Reads a point of origin: one symbol, or {@code ""} for none. */
    public static final Codec<String> ORIGIN_CODEC = Codec.STRING.validate(AddressLayout::validOrigin);

    public static final Codec<Integer> CELL_CHUNKS_CODEC = Codec.intRange(1, AddressLayout.MAX_CELL_CHUNKS);

    /** Reads a layout as it is recorded with the server's data. */
    public static final Codec<AddressLayout> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AddressLayout.CELL_CHUNKS_CODEC.fieldOf("cell_chunks").forGetter(AddressLayout::cellChunks),
            Codec.STRING.fieldOf("point_of_origin").forGetter(AddressLayout::pointOfOrigin),
            Codec.BOOL.optionalFieldOf("secret", false).forGetter(AddressLayout::secret)
    ).apply(instance, AddressLayout::new));

    /** A layout whose addresses anyone can work out from where the cell is. */
    public AddressLayout(final int cellChunks, final String pointOfOrigin) {
        this(cellChunks, pointOfOrigin, false);
    }

    private static DataResult<String> validOrigin(final String origin) {
        if (origin.isEmpty() || origin.length() == 1 && Symbol.of(origin.charAt(0)).isPresent()) {
            return DataResult.success(origin);
        }
        return DataResult.error(() -> "A point of origin is one symbol, or \"\" for none: " + origin);
    }

    /** @return the symbol every address dialed from the world ends with, if its addresses have one */
    public Optional<Symbol> originSymbol() {
        return this.pointOfOrigin.length() == 1 ? Symbol.of(this.pointOfOrigin.charAt(0)) : Optional.empty();
    }

    /**
     * @return the layout written as people read it, such as {@code 1 chunk per cell, point of origin @} or
     *     {@code 8 chunks per cell, point of origin @, secret layout}
     */
    public String describe() {
        return this.cellChunks + (this.cellChunks == 1 ? " chunk" : " chunks") + " per cell, "
                + (this.pointOfOrigin.isEmpty() ? "no point of origin" : "point of origin " + this.pointOfOrigin)
                + (this.secret ? ", secret layout" : "");
    }
}
