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

package com.almuradev.sgcraft.mod.zpm;

/**
 * The blocks that hold ZPMs for gates and cables, as in the original.
 */
public enum ZpmDevice {

    /** The ZPM console: one ZPM, whose gate energy gates and an energy converter draw on. */
    CONSOLE(1),
    /** The ZPM interface cart: one ZPM, whose gate energy gates and an energy converter draw on. */
    CART(1),
    /**
     * The ZPM hub: three ZPMs, whose gate energy gates and an energy converter draw on. The more it holds, the further
     * each goes: with two it draws half as much from them, with three a third.
     */
    HUB(3);

    private final int slots;

    ZpmDevice(final int slots) {
        this.slots = slots;
    }

    /** @return how many ZPMs it holds */
    public int slots() {
        return this.slots;
    }

    /** @return how many times further its energy goes with this many ZPMs in it */
    public int efficiency(final int loaded) {
        return this == ZpmDevice.HUB ? Math.max(1, loaded) : 1;
    }
}
