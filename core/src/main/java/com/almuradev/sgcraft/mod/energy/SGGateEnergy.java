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

import com.almuradev.sgcraft.api.SGCraftRegistries;
import com.almuradev.sgcraft.api.energy.EnergySource;
import com.almuradev.sgcraft.api.energy.EnergySourceProvider;
import com.almuradev.sgcraft.api.energy.GateEnergy;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.mod.gate.SGStargate;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

/**
 * A gate's energy store, kept in its saved state. Paying for a wormhole takes from the sources the registered
 * providers offer the gate first, so a power unit or a DHD gives up each payment as it is made, and from the store
 * only what they can't give. The store is topped up from the sources as it goes, as the original did, and is a
 * reserve for when they run short.
 */
public final class SGGateEnergy implements GateEnergy {

    private static final String STORED = "energy";
    // How long the sources found for paying are kept before they are looked for again, in ticks: an open wormhole
    // pays every tick, and finding them searches the blocks round the gate for each kind of source
    private static final int SOURCES_TICKS = 20;

    private final SGStargate gate;
    // The sources found for paying, and the server tick they were found on
    private List<EnergySource> paying = List.of();
    private int payingFound = Integer.MIN_VALUE;

    public SGGateEnergy(final SGStargate gate) {
        this.gate = gate;
    }

    @Override
    public long stored() {
        return Math.min(this.gate.saved().getLongOr(SGGateEnergy.STORED, 0), this.capacity());
    }

    @Override
    public long capacity() {
        return Math.max(0, this.gate.get(GateProperties.ENERGY_CAPACITY).orElse(0L));
    }

    @Override
    public long insert(final long amount, final boolean simulate) {
        SGGateEnergy.notNegative(amount);
        final long added = Math.min(amount, this.capacity() - this.stored());
        if (!simulate && added > 0) {
            this.store(this.stored() + added);
        }
        return Math.max(0, added);
    }

    @Override
    public long extract(final long amount, final boolean simulate) {
        SGGateEnergy.notNegative(amount);
        final long taken = Math.min(amount, this.stored());
        if (!simulate && taken > 0) {
            this.store(this.stored() - taken);
        }
        return taken;
    }

    private void store(final long amount) {
        // Each payment comes here, mostly leaving the store as full as it was, and the saved state is copied to change
        if (amount == this.gate.saved().getLongOr(SGGateEnergy.STORED, 0)) {
            return;
        }
        final CompoundTag state = this.gate.saved();
        state.putLong(SGGateEnergy.STORED, amount);
        this.gate.setSaved(state);
    }

    /** @return the sources every registered provider offers the gate now */
    List<EnergySource> sources() {
        final List<EnergySource> sources = new ArrayList<>();
        for (final EnergySourceProvider provider : this.gate.server().registryAccess()
                .lookupOrThrow(SGCraftRegistries.ENERGY_SOURCE_PROVIDER)) {
            sources.addAll(provider.sourcesFor(this.gate));
        }
        return sources;
    }

    // The sources to pay from, found again once those found last are a little old, or if asked and they weren't
    // found this tick
    private List<EnergySource> payingSources(final boolean again) {
        final int now = this.gate.server().getTickCount();
        if (now - this.payingFound >= SGGateEnergy.SOURCES_TICKS || now < this.payingFound
                || again && now != this.payingFound) {
            this.paying = this.sources();
            this.payingFound = now;
        }
        return this.paying;
    }

    /** @return how much the gate could pay now, from its store and its sources */
    public long available() {
        long available = this.stored();
        for (final EnergySource source : this.sources()) {
            available = SGGateEnergy.add(available, source.available());
        }
        return available;
    }

    /**
     * Pays an amount from the sources, and from the store what they can't give, filling the store from them as far as
     * it holds.
     *
     * @return whether it was paid; nothing is taken if not
     */
    public boolean pay(final long amount) {
        if (amount <= 0) {
            return true;
        }
        // The sources found a moment ago, unless they are old or fall short, when they are looked for again, as one
        // may have been set down since
        List<EnergySource> sources = this.payingSources(false);
        long fromSources = SGGateEnergy.available(sources);
        if (SGGateEnergy.add(fromSources, this.stored()) < amount) {
            sources = this.payingSources(true);
            fromSources = SGGateEnergy.available(sources);
        }
        if (SGGateEnergy.add(fromSources, this.stored()) < amount) {
            return false;
        }
        // Draw the amount, and what fills the store besides
        long wanted = Math.min(fromSources, SGGateEnergy.add(amount, this.capacity() - this.stored()));
        long drawn = 0;
        for (final EnergySource source : sources) {
            if (wanted <= 0) {
                break;
            }
            final long got = source.draw(wanted, false);
            drawn += got;
            wanted -= got;
        }
        final long total = this.stored() + drawn;
        if (total < amount) {
            // A source gave less than it said; keep what came, but the payment fails
            this.store(Math.min(total, this.capacity()));
            return false;
        }
        this.store(Math.min(total - amount, this.capacity()));
        return true;
    }

    private static long available(final List<EnergySource> sources) {
        long available = 0;
        for (final EnergySource source : sources) {
            available = SGGateEnergy.add(available, source.available());
        }
        return available;
    }

    private static long add(final long a, final long b) {
        final long sum = a + b;
        return sum < 0 ? Long.MAX_VALUE : sum;
    }

    private static void notNegative(final long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("An amount of energy can't be negative: " + amount);
        }
    }
}
