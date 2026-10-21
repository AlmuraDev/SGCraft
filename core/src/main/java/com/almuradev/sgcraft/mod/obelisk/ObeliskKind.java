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

package com.almuradev.sgcraft.mod.obelisk;

/**
 * The two kinds of Asgard obelisk, as in 2.2.0, each linking only with its own kind: Thor's hammer on a gray stone
 * pillar, from Cimmeria, and a tapering sandstone obelisk carved with knotwork. Each has its gem at its own height and
 * depth on its top half.
 */
public enum ObeliskKind {
    HAMMER(5 / 16.0, 2.3 / 16.0),
    KNOTWORK(8 / 16.0, 4.9 / 16.0);

    private final double gemY;
    private final double gemZ;

    ObeliskKind(final double gemY, final double gemZ) {
        this.gemY = gemY;
        this.gemZ = gemZ;
    }

    /** @return how high on its top half its gem's middle is, in blocks */
    public double gemY() {
        return this.gemY;
    }

    /** @return how far from its north face its gem's face is, facing north, in blocks */
    public double gemZ() {
        return this.gemZ;
    }
}
