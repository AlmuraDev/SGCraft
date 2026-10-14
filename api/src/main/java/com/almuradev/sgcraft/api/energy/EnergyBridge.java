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

/**
 * A link between gates and an energy system outside this API, such as a mod loader's own.
 *
 * <p>Bridges are entries in the {@link com.almuradev.sgcraft.api.SGCraftRegistries#ENERGY_BRIDGE} registry, so an
 * add-on registers its own through its mod loader. A bridge does its own work: it makes gates reachable from its
 * system and moves energy into each gate through {@link GateEnergy#insert(long, boolean)}, converting at the rate
 * {@link EnergyService#rate(EnergyBridge)} gives. Where no bridge is registered for a system, gates simply take no
 * energy from it; {@linkplain EnergySource sources} still work.</p>
 *
 * @since 0.1.0
 */
public interface EnergyBridge {

    /**
     * @return how many of the API's units one unit of the bridged system is worth, unless the server's
     *     configuration says otherwise; above 0
     */
    double defaultRate();
}
