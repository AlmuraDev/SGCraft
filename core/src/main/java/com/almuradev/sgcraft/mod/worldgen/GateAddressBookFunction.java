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

package com.almuradev.sgcraft.mod.worldgen;

import com.almuradev.sgcraft.api.SGCraftApi;
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.gate.GateStore;
import com.almuradev.sgcraft.mod.gate.SGStargate;
import com.almuradev.sgcraft.mod.registry.RegistrySink;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The loot function {@code sgcraft:gate_address_book}, as in 2.2.0: the stack becomes a tattered written book holding
 * the address of one gate world generation built, picked at random, as dialed from the world the chest is in. A gate
 * whose address isn't shown, or that no address reaches from there, isn't picked; with none to pick, no book is left.
 */
public final class GateAddressBookFunction extends LootItemConditionalFunction {

    public static final MapCodec<GateAddressBookFunction> CODEC = RecordCodecBuilder.mapCodec(instance ->
            LootItemConditionalFunction.commonFields(instance).apply(instance, GateAddressBookFunction::new));

    // A book found, not written: the most worn copy
    private static final int TATTERED = 3;

    private GateAddressBookFunction(final Optional<Holder<LootItemCondition>> condition) {
        super(condition);
    }

    public static void register(final RegistrySink<MapCodec<? extends LootItemFunction>> sink) {
        sink.register(ResourceKey.create(Registries.LOOT_FUNCTION_TYPE, SGCraft.id("gate_address_book")),
                GateAddressBookFunction.CODEC);
    }

    @Override
    public MapCodec<GateAddressBookFunction> codec() {
        return GateAddressBookFunction.CODEC;
    }

    @Override
    protected ItemStack run(final ItemStack stack, final LootContext context) {
        final ServerLevel level = context.getLevel();
        final List<Address> addresses = GateAddressBookFunction.generated(level);
        if (addresses.isEmpty()) {
            return ItemStack.EMPTY;
        }
        final Address address = addresses.get(context.getRandom().nextInt(addresses.size()));
        final Component page = Component.translatableWithFallback("book.sgcraft.gate_address.page",
                "The symbols of a distant Stargate are scrawled across this page:").append("\n\n")
                .append(Component.literal(address.toString()).withStyle(ChatFormatting.BOLD));
        final ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("Stargate Address"),
                "Unknown", GateAddressBookFunction.TATTERED, List.of(Filterable.passThrough(page)), true));
        return book;
    }

    // The addresses, as dialed from this world, of the gates world generation built that show their address
    private static List<Address> generated(final ServerLevel level) {
        final List<Address> addresses = new ArrayList<>();
        SGServer.current().ifPresent(server -> {
            for (final GateStore.GateRecord record : server.stargates().store().gates()) {
                final Optional<SGStargate> gate = server.stargates().gate(record.position());
                if (gate.isPresent() && gate.get().get(GateProperties.GENERATED).orElse(false)
                        && gate.get().get(GateProperties.ADDRESS_VISIBLE).orElse(true)) {
                    SGCraftApi.get().addressing().addressOf(record.position(), level.dimension())
                            .ifPresent(addresses::add);
                }
            }
        });
        return addresses;
    }
}
