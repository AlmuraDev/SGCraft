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

package com.almuradev.sgcraft.mod.block.entity;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.block.SGBlocks;
import com.almuradev.sgcraft.mod.registry.RegistrySink;
import com.almuradev.sgcraft.mod.shield.CityShieldBlockEntity;
import com.almuradev.sgcraft.mod.zpm.ZpmDeviceBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

/**
 * The mod's block entity types. They exist once the loader has called {@link #register(RegistrySink)}, after the
 * blocks.
 */
public final class SGBlockEntityTypes {

    public static BlockEntityType<StargateBaseBlockEntity> STARGATE_BASE;
    public static BlockEntityType<DhdBlockEntity> DHD;
    public static BlockEntityType<GateSpawnerBlockEntity> GATE_SPAWNER;
    public static BlockEntityType<PowerUnitBlockEntity> POWER_UNIT;
    public static BlockEntityType<BatteryChargerBlockEntity> BATTERY_CHARGER;
    public static BlockEntityType<EnergyConverterBlockEntity> ENERGY_CONVERTER;
    public static BlockEntityType<MalpConsoleBlockEntity> MALP_CONSOLE;
    public static BlockEntityType<DialingComputerBlockEntity> DIALING_COMPUTER;
    public static BlockEntityType<CityShieldBlockEntity> CITY_SHIELD;
    public static BlockEntityType<ZpmDeviceBlockEntity> ZPM_DEVICE;

    private SGBlockEntityTypes() {
    }

    public static void register(final RegistrySink<BlockEntityType<?>> sink) {
        SGBlockEntityTypes.STARGATE_BASE = new BlockEntityType<>(StargateBaseBlockEntity::new,
                Set.of(SGBlocks.STARGATE_BASE, SGBlocks.STARGATE_BASE_PEGASUS, SGBlocks.STARGATE_BASE_UNIVERSE,
                        SGBlocks.SUPERGATE_BASE));
        sink.register(ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, SGCraft.id("stargate_base")),
                SGBlockEntityTypes.STARGATE_BASE);
        SGBlockEntityTypes.DHD = new BlockEntityType<>(DhdBlockEntity::new, Set.of(SGBlocks.DHD, SGBlocks.DHD_PEGASUS));
        sink.register(ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, SGCraft.id("dhd")), SGBlockEntityTypes.DHD);
        SGBlockEntityTypes.GATE_SPAWNER = new BlockEntityType<>(GateSpawnerBlockEntity::new,
                Set.copyOf(SGBlocks.SPAWNERS));
        sink.register(ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, SGCraft.id("gate_spawner")),
                SGBlockEntityTypes.GATE_SPAWNER);
        SGBlockEntityTypes.POWER_UNIT = new BlockEntityType<>(PowerUnitBlockEntity::new, Set.of(SGBlocks.POWER_UNIT));
        sink.register(ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, SGCraft.id("power_unit")),
                SGBlockEntityTypes.POWER_UNIT);
        SGBlockEntityTypes.BATTERY_CHARGER = new BlockEntityType<>(BatteryChargerBlockEntity::new,
                Set.of(SGBlocks.BATTERY_CHARGER));
        sink.register(ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, SGCraft.id("battery_charger")),
                SGBlockEntityTypes.BATTERY_CHARGER);
        SGBlockEntityTypes.ENERGY_CONVERTER = new BlockEntityType<>(EnergyConverterBlockEntity::new,
                Set.of(SGBlocks.ENERGY_CONVERTER));
        sink.register(ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, SGCraft.id("energy_converter")),
                SGBlockEntityTypes.ENERGY_CONVERTER);
        SGBlockEntityTypes.MALP_CONSOLE = new BlockEntityType<>(MalpConsoleBlockEntity::new,
                Set.of(SGBlocks.MALP_CONSOLE));
        sink.register(ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, SGCraft.id("malp_console")),
                SGBlockEntityTypes.MALP_CONSOLE);
        SGBlockEntityTypes.DIALING_COMPUTER = new BlockEntityType<>(DialingComputerBlockEntity::new,
                Set.of(SGBlocks.DIALING_COMPUTER));
        sink.register(ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, SGCraft.id("dialing_computer")),
                SGBlockEntityTypes.DIALING_COMPUTER);
        SGBlockEntityTypes.CITY_SHIELD = new BlockEntityType<>(CityShieldBlockEntity::new,
                Set.of(SGBlocks.CITY_SHIELD_GENERATOR));
        sink.register(ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, SGCraft.id("city_shield_generator")),
                SGBlockEntityTypes.CITY_SHIELD);
        SGBlockEntityTypes.ZPM_DEVICE = new BlockEntityType<>(ZpmDeviceBlockEntity::new,
                Set.of(SGBlocks.ZPM_CONSOLE, SGBlocks.ZPM_INTERFACE_CART, SGBlocks.ZPM_HUB));
        sink.register(ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, SGCraft.id("zpm_device")),
                SGBlockEntityTypes.ZPM_DEVICE);
    }
}
