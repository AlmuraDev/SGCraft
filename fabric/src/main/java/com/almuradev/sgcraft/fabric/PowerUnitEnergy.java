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

import com.almuradev.sgcraft.block.entity.PowerUnitBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import team.reborn.energy.api.EnergyStorage;

/**
 * A power unit as Team Reborn Energy's cables see it: it takes FE in, and gives none out, as the original's did. A
 * transaction undone puts its charge back.
 */
final class PowerUnitEnergy extends SnapshotParticipant<Long> implements EnergyStorage {

    private final PowerUnitBlockEntity unit;

    PowerUnitEnergy(final PowerUnitBlockEntity unit) {
        this.unit = unit;
    }

    @Override
    public long insert(final long amount, final TransactionContext transaction) {
        final long taken = this.unit.insert(amount, true);
        if (taken > 0) {
            this.updateSnapshots(transaction);
            this.unit.insert(taken, false);
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
        return this.unit.stored();
    }

    @Override
    public long getCapacity() {
        return PowerUnitBlockEntity.capacity();
    }

    @Override
    protected Long createSnapshot() {
        return this.unit.stored();
    }

    @Override
    protected void readSnapshot(final Long snapshot) {
        this.unit.setStored(snapshot);
    }
}
