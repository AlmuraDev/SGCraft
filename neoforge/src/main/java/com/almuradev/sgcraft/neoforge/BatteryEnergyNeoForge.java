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

package com.almuradev.sgcraft.neoforge;

import com.almuradev.sgcraft.mod.item.BatteryItem;
import com.almuradev.sgcraft.mod.item.SGDataComponents;
import com.almuradev.sgcraft.mod.item.SGItems;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * A battery as NeoForge's chargers and machines see it: FE goes in and comes out, kept on the stack where the rest of
 * SGCraft reads it. Changes go through the item access, so a transaction undone puts the old stack back.
 */
final class BatteryEnergyNeoForge implements EnergyHandler {

    private final ItemAccess access;

    BatteryEnergyNeoForge(final ItemAccess access) {
        this.access = access;
    }

    private double stored(final ItemResource resource) {
        return resource.is(SGItems.BATTERY) ? BatteryItem.energy(resource.getComponents()) : 0;
    }

    @Override
    public long getAmountAsLong() {
        final ItemResource resource = this.access.getResource();
        return resource.is(SGItems.BATTERY) ? this.access.getAmount() * BatteryItem.fe(resource.getComponents()) : 0;
    }

    @Override
    public long getCapacityAsLong() {
        return this.access.getResource().is(SGItems.BATTERY) ? this.access.getAmount() * BatteryItem.feCapacity() : 0;
    }

    @Override
    public int insert(final int amount, final TransactionContext transaction) {
        return this.change(amount, transaction, true);
    }

    @Override
    public int extract(final int amount, final TransactionContext transaction) {
        return this.change(amount, transaction, false);
    }

    // Moves FE into or out of every battery in the stack alike, as much as each has room for or holds, each keeping
    // the gate energy it is worth
    private int change(final int amount, final TransactionContext transaction, final boolean in) {
        final ItemResource resource = this.access.getResource();
        final int count = this.access.getAmount();
        if (count <= 0 || amount <= 0 || !resource.is(SGItems.BATTERY)) {
            return 0;
        }
        final double stored = this.stored(resource);
        final long each = BatteryItem.feMovable(stored, amount / count, in);
        if (each <= 0) {
            return 0;
        }
        final ItemResource changed = resource.with(SGDataComponents.ENERGY, BatteryItem.afterFe(stored, each, in));
        return (int) (each * this.access.exchange(changed, count, transaction));
    }
}
