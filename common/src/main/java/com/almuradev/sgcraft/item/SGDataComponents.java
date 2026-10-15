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

package com.almuradev.sgcraft.item;

import com.almuradev.sgcraft.SGCraft;
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.addressbook.AddressBookEntry;
import com.almuradev.sgcraft.mod.ListAddressBook;
import com.almuradev.sgcraft.registry.RegistrySink;
import com.almuradev.sgcraft.tool.TabletGate;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;

import java.util.List;

/**
 * The data SGCraft keeps on items.
 */
public final class SGDataComponents {

    /** The addresses a PDD or a gate control tablet holds, in the order its screen lists them. */
    public static final DataComponentType<List<AddressBookEntry>> ADDRESSES =
            DataComponentType.<List<AddressBookEntry>>builder()
                    .persistent(ListAddressBook.ENTRY_CODEC.listOf())
                    .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(ListAddressBook.ENTRY_CODEC.listOf()))
                    .build();

    /** The addresses on a PDD or tablet that its holder can't rename or remove, put there as server defaults. */
    public static final DataComponentType<List<Address>> LOCKED_ADDRESSES =
            DataComponentType.<List<Address>>builder()
                    .persistent(Address.CODEC.listOf())
                    .networkSynchronized(ByteBufCodecs.fromCodec(Address.CODEC.listOf()))
                    .build();

    /** The gates on a gate control tablet's list, in the order its screen lists them. */
    public static final DataComponentType<List<TabletGate>> TABLET_GATES =
            DataComponentType.<List<TabletGate>>builder()
                    .persistent(TabletGate.CODEC.listOf())
                    .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(TabletGate.CODEC.listOf()))
                    .build();

    /** The FE a power unit held when it was broken, which it holds again when placed. */
    public static final DataComponentType<Long> ENERGY = DataComponentType.<Long>builder()
            .persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG).build();

    private SGDataComponents() {
    }

    public static void register(final RegistrySink<DataComponentType<?>> sink) {
        sink.register(ResourceKey.create(Registries.DATA_COMPONENT_TYPE, SGCraft.id("addresses")),
                SGDataComponents.ADDRESSES);
        sink.register(ResourceKey.create(Registries.DATA_COMPONENT_TYPE, SGCraft.id("energy")),
                SGDataComponents.ENERGY);
        sink.register(ResourceKey.create(Registries.DATA_COMPONENT_TYPE, SGCraft.id("locked_addresses")),
                SGDataComponents.LOCKED_ADDRESSES);
        sink.register(ResourceKey.create(Registries.DATA_COMPONENT_TYPE, SGCraft.id("tablet_gates")),
                SGDataComponents.TABLET_GATES);
    }
}
