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

import com.almuradev.sgcraft.api.SGCraftApi;
import com.almuradev.sgcraft.api.SGCraftRegistries;
import com.almuradev.sgcraft.api.address.AddressReach;
import com.almuradev.sgcraft.api.address.AddressReaches;
import com.almuradev.sgcraft.api.addressbook.AddressBookProvider;
import com.almuradev.sgcraft.api.energy.EnergyBridge;
import com.almuradev.sgcraft.api.energy.EnergySourceProvider;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.GateProperty;
import com.almuradev.sgcraft.api.gate.IrisType;
import com.almuradev.sgcraft.api.gate.IrisTypes;
import com.almuradev.sgcraft.mod.address.GridReach;
import com.almuradev.sgcraft.mod.addressbook.ItemAddressBook;
import com.almuradev.sgcraft.mod.block.entity.PowerUnitBlockEntity;
import com.almuradev.sgcraft.mod.dhd.DhdFuelMenu;
import com.almuradev.sgcraft.mod.dhd.DhdFuelProvider;
import com.almuradev.sgcraft.mod.gate.GateBaseMenu;
import com.almuradev.sgcraft.mod.item.SGItems;
import com.almuradev.sgcraft.mod.malp.MalpMenu;
import com.almuradev.sgcraft.mod.power.BatteryChargerMenu;
import com.almuradev.sgcraft.mod.power.PowerUnitMenu;
import com.almuradev.sgcraft.mod.power.PowerUnitProvider;
import com.almuradev.sgcraft.mod.registry.RegistrySink;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.MenuType;

import java.util.Arrays;
import java.util.List;

/**
 * The registry entries the API requires every implementation to register: the three address reaches, the gate
 * properties and the metal iris; and SGCraft's own energy source, address books, energy bridge and menus.
 */
public final class SGBuiltIns {

    private SGBuiltIns() {
    }

    public static void registerReaches(final RegistrySink<AddressReach> sink) {
        sink.register(AddressReaches.SAME_WORLD, new GridReach(7, GridReach.Kind.SAME_WORLD));
        sink.register(AddressReaches.SAME_GROUP, new GridReach(8, GridReach.Kind.SAME_GROUP));
        sink.register(AddressReaches.OTHER_GROUP, new GridReach(9, GridReach.Kind.OTHER_GROUP));
    }

    public static void registerIrisTypes(final RegistrySink<IrisType> sink) {
        sink.register(IrisTypes.METAL, () -> Component.translatable("iris_type.sgcraft.metal"));
    }

    /** SGCraft's own energy sources: the naquadah in a gate's DHDs. */
    public static void registerEnergySources(final RegistrySink<EnergySourceProvider> sink) {
        sink.register(ResourceKey.create(SGCraftRegistries.ENERGY_SOURCE_PROVIDER, SGCraft.id("dhd_fuel")),
                new DhdFuelProvider());
        sink.register(ResourceKey.create(SGCraftRegistries.ENERGY_SOURCE_PROVIDER, SGCraft.id("power_unit")),
                new PowerUnitProvider());
    }

    /**
     * SGCraft's own address books: each player's own, which the address book commands use, and the books on the PDDs
     * and gate control tablets a player holds.
     */
    public static void registerAddressBooks(final RegistrySink<AddressBookProvider> sink) {
        sink.register(ResourceKey.create(SGCraftRegistries.ADDRESS_BOOK_PROVIDER, SGCraft.id("player")),
                player -> List.of(SGCraftApi.get().addressBooks().forPlayer(player.getUUID())));
        sink.register(ResourceKey.create(SGCraftRegistries.ADDRESS_BOOK_PROVIDER, SGCraft.id("pdd")),
                player -> Arrays.stream(InteractionHand.values()).map(player::getItemInHand)
                        .filter(stack -> stack.is(SGItems.PDD) || stack.is(SGItems.GATE_CONTROL_TABLET))
                        .map(ItemAddressBook::of).toList());
    }

    /**
     * SGCraft's own energy bridge: energy already in SGCraft's units, at one for one unless the server's settings scale
     * it. An add-on that hands gates energy it has worked out itself can use this bridge's rate.
     */
    public static void registerEnergyBridges(final RegistrySink<EnergyBridge> sink) {
        sink.register(ResourceKey.create(SGCraftRegistries.ENERGY_BRIDGE, SGCraft.id("direct")), () -> 1.0);
        // FE, the loaders' common energy, which power units store
        sink.register(ResourceKey.create(SGCraftRegistries.ENERGY_BRIDGE, SGCraft.id("forge_energy")),
                PowerUnitBlockEntity.FORGE_ENERGY);
    }

    /** SGCraft's menus. */
    public static void registerMenus(final RegistrySink<MenuType<?>> sink) {
        DhdFuelMenu.TYPE = Platform.get().menuType(DhdFuelMenu::new);
        sink.register(ResourceKey.create(Registries.MENU, SGCraft.id("dhd_fuel")), DhdFuelMenu.TYPE);
        GateBaseMenu.TYPE = Platform.get().menuType(GateBaseMenu::new);
        sink.register(ResourceKey.create(Registries.MENU, SGCraft.id("gate_base")), GateBaseMenu.TYPE);
        PowerUnitMenu.TYPE = Platform.get().menuType(PowerUnitMenu::new);
        sink.register(ResourceKey.create(Registries.MENU, SGCraft.id("power_unit")), PowerUnitMenu.TYPE);
        BatteryChargerMenu.TYPE = Platform.get().menuType(BatteryChargerMenu::new);
        sink.register(ResourceKey.create(Registries.MENU, SGCraft.id("battery_charger")), BatteryChargerMenu.TYPE);
        MalpMenu.TYPE = Platform.get().menuType(MalpMenu::new);
        sink.register(ResourceKey.create(Registries.MENU, SGCraft.id("malp")), MalpMenu.TYPE);
    }

    public static void registerProperties(final RegistrySink<GateProperty<?>> sink) {
        GateProperties.all().forEach((id, property) ->
                sink.register(ResourceKey.create(SGCraftRegistries.GATE_PROPERTY, id), property));
    }
}
