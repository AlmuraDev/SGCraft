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

package com.almuradev.sgcraft.fabric;

import com.almuradev.sgcraft.mod.item.BatteryItem;
import com.almuradev.sgcraft.mod.item.SGDataComponents;
import com.almuradev.sgcraft.mod.item.SGItems;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.component.DataComponentPatch;
import team.reborn.energy.api.EnergyStorage;

/**
 * A battery as Team Reborn Energy's chargers and machines see it: FE goes in and comes out, kept on the stack where
 * the rest of SGCraft reads it. Changes go through the item's context, so a transaction undone puts the old stack
 * back.
 */
final class BatteryEnergyFabric implements EnergyStorage {

    private final ContainerItemContext context;

    BatteryEnergyFabric(final ContainerItemContext context) {
        this.context = context;
    }

    private double stored(final ItemVariant variant) {
        return variant.isOf(SGItems.BATTERY) ? BatteryItem.energy(variant.getComponents()) : 0;
    }

    @Override
    public long insert(final long amount, final TransactionContext transaction) {
        return this.change(amount, transaction, true);
    }

    @Override
    public long extract(final long amount, final TransactionContext transaction) {
        return this.change(amount, transaction, false);
    }

    // Moves FE into or out of every battery in the stack alike, as much as each has room for or holds, each keeping
    // the gate energy it is worth
    private long change(final long amount, final TransactionContext transaction, final boolean in) {
        final ItemVariant variant = this.context.getItemVariant();
        final long count = this.context.getAmount();
        if (count <= 0 || amount <= 0 || !variant.isOf(SGItems.BATTERY)) {
            return 0;
        }
        final double stored = this.stored(variant);
        final long each = BatteryItem.feMovable(stored, amount / count, in);
        if (each <= 0) {
            return 0;
        }
        final ItemVariant changed = variant.withComponents(DataComponentPatch.builder()
                .set(SGDataComponents.ENERGY, BatteryItem.afterFe(stored, each, in)).build());
        return this.context.exchange(changed, count, transaction) == count ? each * count : 0;
    }

    @Override
    public long getAmount() {
        final ItemVariant variant = this.context.getItemVariant();
        return variant.isOf(SGItems.BATTERY) ? this.context.getAmount() * BatteryItem.fe(variant.getComponents()) : 0;
    }

    @Override
    public long getCapacity() {
        return this.context.getItemVariant().isOf(SGItems.BATTERY)
                ? this.context.getAmount() * BatteryItem.feCapacity() : 0;
    }
}
