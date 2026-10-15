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
 * How a world is divided into addresses: how big each addressed cell is, and whether addresses dialed from the world
 * end with a point of origin.
 *
 * <p>Every gate in one cell has the same position symbols, so a cell holds at most one gate. Bigger cells reach
 * further from the world's center with the same symbols, at the cost of more space between gates.</p>
 *
 * <p>Implementations must let the server's configuration set both values, for all worlds and for single worlds. They
 * must record the values in use when the server first starts. If the configuration differs from the recorded values
 * on a later start, they must log a prominent warning naming each affected world and saying that every address in it
 * has changed, then record the new values.</p>
 *
 * @since 0.1.0
 */
public interface AddressPartition {

    /** @return how many chunks wide and long each addressed cell is; at least 1 */
    int cellChunks();

    /**
     * When present, every address dialed from this world ends with this symbol, and the
     * {@linkplain AddressReach reaches} count it among their symbols, leaving one fewer for the position.
     *
     * @return the world's point of origin, or empty if its addresses don't use one
     */
    Optional<Symbol> pointOfOrigin();
}
