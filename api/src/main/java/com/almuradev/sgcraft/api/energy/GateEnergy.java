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
 * A gate's energy store, in the API's own unit (see the package documentation).
 *
 * <p>Dialling draws from the store. {@linkplain EnergySource Sources} and {@linkplain EnergyBridge bridges} to
 * other energy systems fill it. A gate whose {@link com.almuradev.sgcraft.api.gate.GateProperties#REQUIRES_POWER}
 * property is false dials without drawing anything.</p>
 *
 * @since 0.1.0
 */
public interface GateEnergy {

    /** @return how much energy the store holds */
    long stored();

    /** @return how much energy the store can hold */
    long capacity();

    /**
     * @param amount how much energy to add; not negative
     * @param simulate whether to only work out the answer, without changing the store
     * @return how much was, or would be, added, which is less than {@code amount} when the store fills up
     */
    long insert(final long amount, final boolean simulate);

    /**
     * @param amount how much energy to take; not negative
     * @param simulate whether to only work out the answer, without changing the store
     * @return how much was, or would be, taken, which is less than {@code amount} when the store runs out
     */
    long extract(final long amount, final boolean simulate);
}
