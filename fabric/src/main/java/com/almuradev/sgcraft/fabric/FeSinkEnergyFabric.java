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

import com.almuradev.sgcraft.mod.energy.FeSink;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import team.reborn.energy.api.EnergyStorage;

/**
 * A power unit or a ring platform as Team Reborn Energy's cables see it: it takes FE in, and gives none out, as the
 * original's did. A transaction undone puts its charge back.
 */
final class FeSinkEnergyFabric extends SnapshotParticipant<Double> implements EnergyStorage {

    private final FeSink sink;

    FeSinkEnergyFabric(final FeSink sink) {
        this.sink = sink;
    }

    @Override
    public long insert(final long amount, final TransactionContext transaction) {
        final long taken = this.sink.insertFe(amount, true);
        if (taken > 0) {
            this.updateSnapshots(transaction);
            this.sink.insertFe(taken, false);
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
        return this.sink.feAmount();
    }

    @Override
    public long getCapacity() {
        return this.sink.feLimit();
    }

    @Override
    protected Double createSnapshot() {
        return this.sink.stored();
    }

    @Override
    protected void readSnapshot(final Double snapshot) {
        this.sink.setStored(snapshot);
    }
}
