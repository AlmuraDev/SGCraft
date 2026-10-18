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

package com.almuradev.sgcraft.mod.block;

import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.api.gate.GateTypes;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.registry.RegistrySink;
import com.almuradev.sgcraft.mod.shield.CityShieldBlock;
import com.almuradev.sgcraft.mod.shield.ShieldConsoleBlock;
import com.almuradev.sgcraft.mod.zpm.ZpmDevice;
import com.almuradev.sgcraft.mod.zpm.ZpmDeviceBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The mod's blocks. They exist once the loader has called {@link #register(RegistrySink)}.
 */
public final class SGBlocks {

    public static RingBlock STARGATE_RING;
    public static RingBlock STARGATE_CHEVRON;
    public static StargateBaseBlock STARGATE_BASE;
    public static StargateBaseBlock STARGATE_BASE_PEGASUS;
    public static StargateBaseBlock STARGATE_BASE_UNIVERSE;
    public static RingBlock SUPERGATE_RING;
    public static RingBlock SUPERGATE_CHEVRON;
    public static StargateBaseBlock SUPERGATE_BASE;
    public static EventHorizonBlock EVENT_HORIZON;
    /** The gate spawners, a stargate's then a supergate's of each built in type, in the creative tab's order. */
    public static final List<GateSpawnerBlock> SPAWNERS = new ArrayList<>();
    public static DhdBlock DHD;
    public static DhdBlock DHD_PEGASUS;
    public static Block NAQUADAH_ORE;
    public static Block NAQUADAH_BLOCK;
    public static PowerUnitBlock POWER_UNIT;
    public static MalpConsoleBlock MALP_CONSOLE;
    public static DialingComputerBlock DIALING_COMPUTER;
    public static OfficeChairBlock OFFICE_CHAIR;
    public static BatteryChargerBlock BATTERY_CHARGER;
    public static EnergyConverterBlock ENERGY_CONVERTER;
    public static ShieldConsoleBlock SHIELD_CONSOLE;
    public static CityShieldBlock CITY_SHIELD_GENERATOR;
    public static ZpmDeviceBlock ZPM_CONSOLE;
    public static ZpmDeviceBlock ZPM_INTERFACE_CART;
    public static ZpmDeviceBlock ZPM_HUB;

    private SGBlocks() {
    }

    public static void register(final RegistrySink<Block> sink) {
        SGBlocks.STARGATE_RING = SGBlocks.register(sink, "stargate_ring",
                properties -> new RingBlock(properties, false));
        SGBlocks.STARGATE_CHEVRON = SGBlocks.register(sink, "stargate_chevron",
                properties -> new RingBlock(properties, true));
        SGBlocks.STARGATE_BASE = SGBlocks.register(sink, "stargate_base",
                properties -> new StargateBaseBlock(properties, GateTypes.MILKY_WAY));
        SGBlocks.STARGATE_BASE_PEGASUS = SGBlocks.register(sink, "stargate_base_pegasus",
                properties -> new StargateBaseBlock(properties, GateTypes.PEGASUS));
        SGBlocks.STARGATE_BASE_UNIVERSE = SGBlocks.register(sink, "stargate_base_universe",
                properties -> new StargateBaseBlock(properties, GateTypes.UNIVERSE));
        // Supergates: a ring of these blocks following a circle, any size the base finds; a Milky Way gate until its
        // type is changed
        SGBlocks.SUPERGATE_RING = SGBlocks.register(sink, "supergate_ring",
                properties -> new RingBlock(properties, false, true));
        SGBlocks.SUPERGATE_CHEVRON = SGBlocks.register(sink, "supergate_chevron",
                properties -> new RingBlock(properties, true, true));
        SGBlocks.SUPERGATE_BASE = SGBlocks.register(sink, "supergate_base",
                properties -> new StargateBaseBlock(properties, GateTypes.MILKY_WAY, true));
        // Gate spawners, for testing: each builds a finished gate in its place, and drops nothing
        for (final boolean supergate : new boolean[] {false, true}) {
            for (final ResourceKey<GateType> type : List.of(GateTypes.MILKY_WAY, GateTypes.PEGASUS,
                    GateTypes.UNIVERSE)) {
                final String name = type.identifier().getPath() + (supergate ? "_supergate" : "_stargate")
                        + "_spawner";
                SGBlocks.SPAWNERS.add(SGBlocks.register(sink, name, properties -> new GateSpawnerBlock(
                        properties.sound(SoundType.STONE).mapColor(MapColor.STONE).noLootTable(), type,
                        supergate)));
            }
        }
        // The Ori supergate comes only as a supergate
        SGBlocks.SPAWNERS.add(SGBlocks.register(sink, "ori_supergate_spawner", properties -> new GateSpawnerBlock(
                properties.sound(SoundType.STONE).mapColor(MapColor.STONE).noLootTable(), GateTypes.ORI, true)));
        // A DHD's model is drawn by its block entity, so the block hides nothing behind it
        SGBlocks.DHD = SGBlocks.register(sink, "dhd",
                properties -> new DhdBlock(properties.noOcclusion(), GateTypes.MILKY_WAY));
        SGBlocks.EVENT_HORIZON = SGBlocks.register(sink, "event_horizon",
                properties -> new EventHorizonBlock(properties.noCollision().noOcclusion().noLootTable()
                        .strength(-1.0F, 3600000.0F).pushReaction(PushReaction.IMMOVEABLE).mapColor(MapColor.NONE)));
        SGBlocks.DHD_PEGASUS = SGBlocks.register(sink, "dhd_pegasus",
                properties -> new DhdBlock(properties.noOcclusion(), GateTypes.PEGASUS));
        // Naquadah: ore found deep down, which fuels gates, and a block of its alloy
        SGBlocks.NAQUADAH_ORE = SGBlocks.register(sink, "naquadah_ore", properties -> new DropExperienceBlock(
                UniformInt.of(2, 5), properties.mapColor(MapColor.STONE).sound(SoundType.STONE).strength(3.0F, 3.0F)));
        SGBlocks.NAQUADAH_BLOCK = SGBlocks.register(sink, "naquadah_block",
                properties -> new Block(properties.strength(5.0F, 6.0F)));
        // The original's power unit, as hard as its machines
        SGBlocks.POWER_UNIT = SGBlocks.register(sink, "power_unit",
                properties -> new PowerUnitBlock(properties.strength(1.5F, 10.0F)));
        // The MALP's console, six parts of a desk that don't fill their blocks, and the charger for its batteries
        SGBlocks.MALP_CONSOLE = SGBlocks.register(sink, "malp_console",
                properties -> new MalpConsoleBlock(properties.noOcclusion().pushReaction(PushReaction.IMMOVEABLE)));
        SGBlocks.BATTERY_CHARGER = SGBlocks.register(sink, "battery_charger", BatteryChargerBlock::new);
        // The SGC's dialing computer, six parts that don't fill their blocks, and the chair its operator sits in
        SGBlocks.DIALING_COMPUTER = SGBlocks.register(sink, "dialing_computer",
                properties -> new DialingComputerBlock(properties.noOcclusion().pushReaction(PushReaction.IMMOVEABLE)));
        SGBlocks.OFFICE_CHAIR = SGBlocks.register(sink, "office_chair",
                properties -> new OfficeChairBlock(properties.noOcclusion().strength(0.8F, 1.0F)
                        .sound(SoundType.WOOL).mapColor(MapColor.COLOR_BLACK)));
        // Turns an SGCraft store's gate energy into FE for other mods
        SGBlocks.ENERGY_CONVERTER = SGBlocks.register(sink, "energy_converter", EnergyConverterBlock::new);
        // The original's shield console, which works the nearest gate's shield, as hard as its machines
        SGBlocks.SHIELD_CONSOLE = SGBlocks.register(sink, "shield_console",
                properties -> new ShieldConsoleBlock(properties.strength(1.5F, 10.0F)));
        // The city shield generator, as hard as the original's, and proof against blasts
        SGBlocks.CITY_SHIELD_GENERATOR = SGBlocks.register(sink, "city_shield_generator",
                properties -> new CityShieldBlock(properties.strength(5.0F, 2000.0F)));
        // The ZPM console, interface cart and hub, which hold ZPMs for gates and cables; models of their own, so they
        // hide nothing behind them
        SGBlocks.ZPM_CONSOLE = SGBlocks.register(sink, "zpm_console",
                properties -> new ZpmDeviceBlock.Single(properties.noOcclusion(), ZpmDevice.CONSOLE));
        SGBlocks.ZPM_INTERFACE_CART = SGBlocks.register(sink, "zpm_interface_cart",
                properties -> new ZpmDeviceBlock.Single(properties.noOcclusion(), ZpmDevice.CART));
        SGBlocks.ZPM_HUB = SGBlocks.register(sink, "zpm_hub",
                properties -> new ZpmDeviceBlock.Hub(properties.noOcclusion()));
    }

    private static <B extends Block> B register(final RegistrySink<Block> sink, final String name,
            final Function<BlockBehaviour.Properties, B> factory) {
        final ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SGCraft.id(name));
        final B block = factory.apply(BlockBehaviour.Properties.of()
                .setId(key)
                .mapColor(MapColor.METAL)
                .sound(SoundType.METAL)
                .strength(1.5F, 6.0F)
                .requiresCorrectToolForDrops());
        sink.register(key, block);
        return block;
    }
}
