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

package com.almuradev.sgcraft.mod.item;

import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.addressbook.AddressBookEntry;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.addressbook.ListAddressBook;
import com.almuradev.sgcraft.mod.malp.MalpOptions;
import com.almuradev.sgcraft.mod.registry.RegistrySink;
import com.almuradev.sgcraft.mod.tool.TabletGate;
import com.mojang.serialization.Codec;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;

import java.util.List;
import java.util.UUID;

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

    /** The gate energy a power unit or battery holds; a power unit holds it again when placed. */
    public static final DataComponentType<Double> ENERGY = DataComponentType.<Double>builder()
            .persistent(Codec.DOUBLE).networkSynchronized(ByteBufCodecs.DOUBLE).build();

    /** The MALP console a MALP item is paired with, which the MALP placed from it reports to. */
    public static final DataComponentType<GlobalPos> MALP_CONSOLE = DataComponentType.<GlobalPos>builder()
            .persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build();

    /** The player a MALP item's MALP belongs to, by UUID, which the MALP placed from it keeps. */
    public static final DataComponentType<UUID> MALP_OWNER = DataComponentType.<UUID>builder()
            .persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC).build();

    /** The settings of a MALP item's MALP, which the MALP placed from it keeps; absent for the defaults. */
    public static final DataComponentType<MalpOptions> MALP_OPTIONS = DataComponentType.<MalpOptions>builder()
            .persistent(MalpOptions.CODEC).networkSynchronized(ByteBufCodecs.fromCodec(MalpOptions.CODEC)).build();

    /** The MALPs a MALP tablet is paired with, by UUID, in the order they were paired. */
    public static final DataComponentType<List<UUID>> TABLET_MALPS = DataComponentType.<List<UUID>>builder()
            .persistent(UUIDUtil.CODEC.listOf())
            .networkSynchronized(UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()))
            .build();

    /** The gate energy a ZPM holds; a ZPM without it is full. */
    public static final DataComponentType<Double> ZPM_ENERGY = DataComponentType.<Double>builder()
            .persistent(Codec.DOUBLE).networkSynchronized(ByteBufCodecs.DOUBLE).build();

    /** Marks a tainted ZPM, which explodes when drawn on. */
    public static final DataComponentType<Unit> ZPM_TAINTED = DataComponentType.<Unit>builder()
            .persistent(Unit.CODEC).networkSynchronized(Unit.STREAM_CODEC).build();

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
        sink.register(ResourceKey.create(Registries.DATA_COMPONENT_TYPE, SGCraft.id("malp_console")),
                SGDataComponents.MALP_CONSOLE);
        sink.register(ResourceKey.create(Registries.DATA_COMPONENT_TYPE, SGCraft.id("malp_owner")),
                SGDataComponents.MALP_OWNER);
        sink.register(ResourceKey.create(Registries.DATA_COMPONENT_TYPE, SGCraft.id("malp_options")),
                SGDataComponents.MALP_OPTIONS);
        sink.register(ResourceKey.create(Registries.DATA_COMPONENT_TYPE, SGCraft.id("tablet_malps")),
                SGDataComponents.TABLET_MALPS);
        sink.register(ResourceKey.create(Registries.DATA_COMPONENT_TYPE, SGCraft.id("zpm_energy")),
                SGDataComponents.ZPM_ENERGY);
        sink.register(ResourceKey.create(Registries.DATA_COMPONENT_TYPE, SGCraft.id("zpm_tainted")),
                SGDataComponents.ZPM_TAINTED);
    }
}
