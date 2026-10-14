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

package com.almuradev.sgcraft.api.energy;

import com.almuradev.sgcraft.api.gate.Stargate;

import java.util.List;

/**
 * Finds the energy sources a gate can reach, such as the fuel in its DHD, a ZPM hub beside it, or an entity nearby.
 *
 * <p>Providers are entries in the {@link com.almuradev.sgcraft.api.SGCraftRegistries#ENERGY_SOURCE_PROVIDER}
 * registry, so an add-on registers its own through its mod loader. When a gate needs energy, implementations must
 * ask every registered provider and draw from the sources they return, in the order returned within each provider.
 * The order across providers is up to the implementation.</p>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface EnergySourceProvider {

    /**
     * @param gate a gate that needs energy
     * @return the sources this provider offers that gate right now, which may be none
     */
    List<EnergySource> sourcesFor(final Stargate gate);
}
