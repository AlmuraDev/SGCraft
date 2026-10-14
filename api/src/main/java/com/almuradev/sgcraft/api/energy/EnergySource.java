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

package com.almuradev.sgcraft.api.energy;

/**
 * Something a gate can draw energy from, wherever it is: an item such as naquadah in a fuel slot, a block such as a
 * ZPM hub or a generator near the gate, or an entity such as a ship parked beside it.
 *
 * <p>{@linkplain EnergySourceProvider Providers} find the sources a gate can reach. A source is only valid while the
 * provider's answer is current; implementations ask the providers again each time they draw, rather than keeping
 * sources.</p>
 *
 * @since 0.1.0
 */
public interface EnergySource {

    /** @return how much energy the source could give right now, in the API's unit */
    long available();

    /**
     * Draws energy from the source. When not simulating, the source changes whatever backs it to match, such as
     * using up an item or lowering a block's charge.
     *
     * @param wanted the most energy to draw; not negative
     * @param simulate whether to only work out the answer, without changing anything
     * @return how much energy was, or would be, drawn
     */
    long draw(final long wanted, final boolean simulate);
}
