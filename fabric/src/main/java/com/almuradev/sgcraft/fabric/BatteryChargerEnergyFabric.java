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

import com.almuradev.sgcraft.mod.block.entity.BatteryChargerBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import team.reborn.energy.api.EnergyStorage;

/**
 * A battery charger as Team Reborn Energy's cables see it: it takes FE in, for its battery, and gives none out. A
 * transaction undone puts its store back.
 */
final class BatteryChargerEnergyFabric extends SnapshotParticipant<Double> implements EnergyStorage {

    private final BatteryChargerBlockEntity charger;

    BatteryChargerEnergyFabric(final BatteryChargerBlockEntity charger) {
        this.charger = charger;
    }

    @Override
    public long insert(final long amount, final TransactionContext transaction) {
        final long taken = this.charger.insertFe(amount, true);
        if (taken > 0) {
            this.updateSnapshots(transaction);
            this.charger.insertFe(taken, false);
        }
        return taken;
    }

    @Override
    public boolean supportsExtraction() {
        return false;
    }

    @Override
    public long extract(final long amount, final TransactionContext transaction) {
        return 0;
    }

    @Override
    public long getAmount() {
        return this.charger.feStored();
    }

    @Override
    public long getCapacity() {
        return BatteryChargerBlockEntity.feCapacity();
    }

    @Override
    protected Double createSnapshot() {
        return this.charger.stored();
    }

    @Override
    protected void readSnapshot(final Double snapshot) {
        this.charger.setStored(snapshot);
    }
}
