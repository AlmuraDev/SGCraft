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

import com.almuradev.sgcraft.mod.energy.FeSink;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * A power unit or a ring platform as NeoForge's cables see it: it takes FE in, and gives none out, as the
 * original's did. A transaction undone puts its charge back.
 */
final class FeSinkEnergyNeoForge extends SnapshotJournal<Double> implements EnergyHandler {

    private final FeSink sink;

    FeSinkEnergyNeoForge(final FeSink sink) {
        this.sink = sink;
    }

    @Override
    public long getAmountAsLong() {
        return this.sink.feAmount();
    }

    @Override
    public long getCapacityAsLong() {
        return this.sink.feLimit();
    }

    @Override
    public int insert(final int amount, final TransactionContext transaction) {
        final long taken = this.sink.insertFe(amount, true);
        if (taken > 0) {
            this.updateSnapshots(transaction);
            this.sink.insertFe(taken, false);
        }
        return (int) taken;
    }

    @Override
    public int extract(final int amount, final TransactionContext transaction) {
        return 0;
    }

    @Override
    protected Double createSnapshot() {
        return this.sink.stored();
    }

    @Override
    protected void revertToSnapshot(final Double snapshot) {
        this.sink.setStored(snapshot);
    }
}
