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

package com.almuradev.sgcraft.api;

import com.almuradev.sgcraft.api.address.AddressReach;
import com.almuradev.sgcraft.api.addressbook.AddressBookProvider;
import com.almuradev.sgcraft.api.energy.EnergyBridge;
import com.almuradev.sgcraft.api.energy.EnergySourceProvider;
import com.almuradev.sgcraft.api.gate.GateProperty;
import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.api.gate.IrisType;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Keys of the registries the API defines. Implementations must create these registries. Add-ons register into them
 * through their mod loader, as they would register blocks or items.
 *
 * @since 0.1.0
 */
public final class SGCraftRegistries {

    /** Every kind of address, as {@link AddressReach}. */
    public static final ResourceKey<Registry<AddressReach>> ADDRESS_REACH = SGCraftRegistries.key("address_reach");

    /** Every style of gate, as {@link GateType}. */
    public static final ResourceKey<Registry<GateType>> GATE_TYPE = SGCraftRegistries.key("gate_type");

    /** Every kind of iris, as {@link IrisType}. */
    public static final ResourceKey<Registry<IrisType>> IRIS_TYPE = SGCraftRegistries.key("iris_type");

    /** Everything that finds energy for gates, as {@link EnergySourceProvider}. */
    public static final ResourceKey<Registry<EnergySourceProvider>> ENERGY_SOURCE_PROVIDER =
            SGCraftRegistries.key("energy_source_provider");

    /** Every link to an outside energy system, as {@link EnergyBridge}. */
    public static final ResourceKey<Registry<EnergyBridge>> ENERGY_BRIDGE = SGCraftRegistries.key("energy_bridge");

    /** Every setting or fitting a gate holds, as {@link GateProperty}. */
    public static final ResourceKey<Registry<GateProperty<?>>> GATE_PROPERTY =
            SGCraftRegistries.key("gate_property");

    /** Everything that offers players address books beyond their own, as {@link AddressBookProvider}. */
    public static final ResourceKey<Registry<AddressBookProvider>> ADDRESS_BOOK_PROVIDER =
            SGCraftRegistries.key("address_book_provider");

    private SGCraftRegistries() {
    }

    private static <T> ResourceKey<Registry<T>> key(final String path) {
        return ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(SGCraftApi.NAMESPACE, path));
    }
}
