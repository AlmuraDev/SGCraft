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

package com.almuradev.sgcraft.mod.energy;

/**
 * A block that takes in FE from other mods' cables and keeps it as gate energy (GE), at the rate {@link FeRate} sets:
 * a power unit, or a ring platform. The loaders hand cables a handler that works through it, which undoes what a
 * transaction took by putting back what it held before.
 */
public interface FeSink {

    /** @return what it holds, as cables see it, in FE */
    long feAmount();

    /** @return the most it holds, as cables see it, in FE */
    long feLimit();

    /**
     * Takes in FE from a cable, as much as there is room for, and keeps the gate energy it is worth.
     *
     * @return how much FE it took, or would take
     */
    long insertFe(final long fe, final boolean simulate);

    /** @return the gate energy it holds */
    double stored();

    /** Sets the gate energy it holds, for putting back what a transaction undone took. */
    void setStored(final double energy);
}
