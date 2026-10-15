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

import com.almuradev.sgcraft.api.SGCraftRegistries;
import com.almuradev.sgcraft.api.energy.EnergyBridge;
import com.almuradev.sgcraft.api.energy.EnergyService;
import net.minecraft.resources.Identifier;

import java.util.Optional;

/**
 * Energy settings across all gates: each bridge's rate comes from the server's settings, by the bridge's registry
 * identifier, and otherwise from the bridge itself. With no server running, every bridge has its own rate.
 */
public final class SGEnergyService implements EnergyService {

    @Override
    public double rate(final EnergyBridge bridge) {
        final Optional<SGServer> server = SGServer.current();
        if (server.isEmpty()) {
            return bridge.defaultRate();
        }
        final Identifier id = server.get().server().registryAccess().lookupOrThrow(SGCraftRegistries.ENERGY_BRIDGE)
                .getKey(bridge);
        final Double rate = id == null ? null : server.get().config().bridgeRates().get(id);
        return rate != null && rate > 0 ? rate : bridge.defaultRate();
    }
}
