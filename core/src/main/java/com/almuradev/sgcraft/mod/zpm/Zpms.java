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

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Drawing on several ZPMs at once, as the original did: evenly, each giving its share, and those with energy left
 * making up what the empty ones can't.
 */
public final class Zpms {

    // Less gate energy than this is left alone, so splitting a fraction among several ZPMs ends
    private static final double EPSILON = 1e-9;

    private Zpms() {
    }

    /** @return the ZPMs in a container's slots that still hold energy */
    public static List<ItemStack> charged(final Container container) {
        final List<ItemStack> charged = new ArrayList<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            final ItemStack stack = container.getItem(slot);
            if (ZpmItem.is(stack) && ZpmItem.energy(stack) > 0) {
                charged.add(stack);
            }
        }
        return charged;
    }

    /** @return how much gate energy some ZPMs hold between them */
    public static double energy(final List<ItemStack> zpms) {
        double energy = 0;
        for (final ItemStack zpm : zpms) {
            energy += ZpmItem.energy(zpm);
        }
        return energy;
    }

    /** @return how much whole gate energy some ZPMs could give */
    public static long available(final List<ItemStack> zpms) {
        return (long) Math.floor(Zpms.energy(zpms));
    }

    /**
     * Takes whole gate energy from some ZPMs, as evenly as they allow.
     *
     * @return how much was taken, which is less than asked only when they run dry
     */
    public static long drawEnergy(final List<ItemStack> zpms, final long wanted) {
        final long given = Math.max(0, Math.min(wanted, Zpms.available(zpms)));
        if (given > 0) {
            Zpms.drain(zpms, given);
        }
        return given;
    }

    /**
     * Takes gate energy from some ZPMs, as evenly as they allow.
     *
     * @return how much was taken, which is less than asked only when they run dry
     */
    public static double drain(final List<ItemStack> zpms, final double energy) {
        double left = energy;
        final List<ItemStack> giving = new ArrayList<>(zpms);
        while (left > Zpms.EPSILON && !giving.isEmpty()) {
            final double share = left / giving.size();
            final List<ItemStack> drained = new ArrayList<>();
            for (final ItemStack zpm : giving) {
                if (left <= Zpms.EPSILON) {
                    break;
                }
                final double has = ZpmItem.energy(zpm);
                final double taken = Math.min(has, Math.min(share, left));
                ZpmItem.setEnergy(zpm, has - taken);
                left -= taken;
                if (taken == has) {
                    drained.add(zpm);
                }
            }
            giving.removeAll(drained);
        }
        return energy - left;
    }
}
