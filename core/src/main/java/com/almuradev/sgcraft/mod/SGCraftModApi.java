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

package com.almuradev.sgcraft.mod;

import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.SGCraftApi;
import com.almuradev.sgcraft.api.address.Addressing;
import com.almuradev.sgcraft.api.addressbook.AddressBooks;
import com.almuradev.sgcraft.api.energy.EnergyService;
import com.almuradev.sgcraft.api.event.EventBus;
import com.almuradev.sgcraft.api.gate.Stargates;
import com.almuradev.sgcraft.mod.energy.SGEnergyService;
import com.almuradev.sgcraft.mod.event.SGEventBus;

/**
 * SGCraft's own implementation of the API, found through {@link java.util.ServiceLoader}. Addresses and gates
 * belong to the running server, so asking for them with no server running throws {@link IllegalStateException}.
 */
public final class SGCraftModApi implements SGCraftApi {

    private final EnergyService energy = new SGEnergyService();

    @Override
    public Addressing addressing() {
        return SGServer.require().addressing();
    }

    @Override
    public Stargates stargates() {
        return SGServer.require().stargates();
    }

    @Override
    public AddressBooks addressBooks() {
        return SGServer.require().addressBooks();
    }

    @Override
    public EnergyService energy() {
        return this.energy;
    }

    @Override
    public EventBus eventBus() {
        return SGEventBus.get();
    }

    @Override
    public Cause.Factory causeFactory() {
        return SimpleCause::of;
    }
}
