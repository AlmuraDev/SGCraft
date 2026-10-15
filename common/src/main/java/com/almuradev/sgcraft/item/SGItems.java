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
import com.almuradev.sgcraft.api.gate.GateTypes;
import com.almuradev.sgcraft.block.SGBlocks;
import com.almuradev.sgcraft.registry.RegistrySink;
import com.almuradev.sgcraft.tool.ToolActions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * The mod's items. They exist once the loader has called {@link #register(RegistrySink)}, after the blocks.
 */
public final class SGItems {

    private static final List<Item> TAB_ITEMS = new ArrayList<>();

    public static BlockItem STARGATE_RING;
    public static BlockItem STARGATE_CHEVRON;
    public static BlockItem STARGATE_BASE;
    public static BlockItem STARGATE_BASE_PEGASUS;
    public static BlockItem STARGATE_BASE_UNIVERSE;
    public static BlockItem SUPERGATE_RING;
    public static BlockItem SUPERGATE_CHEVRON;
    public static BlockItem SUPERGATE_BASE;
    public static BlockItem DHD;
    public static BlockItem DHD_PEGASUS;
    public static BlockItem NAQUADAH_ORE;
    public static BlockItem NAQUADAH_BLOCK;
    public static BlockItem POWER_UNIT;
    public static Item NAQUADAH;
    public static Item NAQUADAH_INGOT;
    public static Item CORE_CRYSTAL;
    public static Item CONTROLLER_CRYSTAL;
    public static Item IRIS_BLADE;
    public static Item IRIS_UPGRADE;
    public static Item CHEVRON_UPGRADE;
    public static Item PEGASUS_UPGRADE;
    public static Item UNIVERSE_UPGRADE;
    public static Item TOLLAN_PHASE_SHIFT_DEVICE;
    public static Item PDD;
    public static Item GATE_CONTROL_TABLET;
    public static Item GDO;
    public static Item CONFIGURATOR;

    private SGItems() {
    }

    public static void register(final RegistrySink<Item> sink) {
        SGItems.STARGATE_RING = SGItems.registerBlock(sink, SGBlocks.STARGATE_RING);
        SGItems.STARGATE_CHEVRON = SGItems.registerBlock(sink, SGBlocks.STARGATE_CHEVRON);
        SGItems.STARGATE_BASE = SGItems.registerBlock(sink, SGBlocks.STARGATE_BASE);
        SGItems.STARGATE_BASE_PEGASUS = SGItems.registerBlock(sink, SGBlocks.STARGATE_BASE_PEGASUS);
        SGItems.STARGATE_BASE_UNIVERSE = SGItems.registerBlock(sink, SGBlocks.STARGATE_BASE_UNIVERSE);
        SGItems.SUPERGATE_RING = SGItems.registerBlock(sink, SGBlocks.SUPERGATE_RING);
        SGItems.SUPERGATE_CHEVRON = SGItems.registerBlock(sink, SGBlocks.SUPERGATE_CHEVRON);
        SGItems.SUPERGATE_BASE = SGItems.registerBlock(sink, SGBlocks.SUPERGATE_BASE);
        SGBlocks.SPAWNERS.forEach(spawner -> SGItems.registerBlock(sink, spawner));
        SGItems.DHD = SGItems.registerBlock(sink, SGBlocks.DHD);
        SGItems.DHD_PEGASUS = SGItems.registerBlock(sink, SGBlocks.DHD_PEGASUS);
        SGItems.NAQUADAH_ORE = SGItems.registerBlock(sink, SGBlocks.NAQUADAH_ORE);
        SGItems.POWER_UNIT = SGItems.registerBlock(sink, SGBlocks.POWER_UNIT);
        SGItems.NAQUADAH_BLOCK = SGItems.registerBlock(sink, SGBlocks.NAQUADAH_BLOCK);
        SGItems.NAQUADAH = SGItems.register(sink, "naquadah", Item::new, properties -> properties);
        SGItems.NAQUADAH_INGOT = SGItems.register(sink, "naquadah_ingot", Item::new, properties -> properties);
        SGItems.CORE_CRYSTAL = SGItems.register(sink, "core_crystal", Item::new, properties -> properties);
        SGItems.CONTROLLER_CRYSTAL = SGItems.register(sink, "controller_crystal", Item::new,
                properties -> properties);
        SGItems.IRIS_BLADE = SGItems.register(sink, "iris_blade", Item::new, properties -> properties);
        SGItems.IRIS_UPGRADE = SGItems.register(sink, "iris_upgrade", IrisUpgradeItem::new,
                properties -> properties);
        SGItems.CHEVRON_UPGRADE = SGItems.register(sink, "chevron_upgrade", ChevronUpgradeItem::new,
                properties -> properties);
        SGItems.PEGASUS_UPGRADE = SGItems.register(sink, "pegasus_upgrade",
                properties -> new GateTypeUpgradeItem(properties, GateTypes.PEGASUS), properties -> properties);
        SGItems.UNIVERSE_UPGRADE = SGItems.register(sink, "universe_upgrade",
                properties -> new GateTypeUpgradeItem(properties, GateTypes.UNIVERSE), properties -> properties);
        // Held in either hand, it takes its holder through a shut iris; as in the original, it has no recipe
        SGItems.TOLLAN_PHASE_SHIFT_DEVICE = SGItems.register(sink, "tollan_phase_shift_device", Item::new,
                properties -> properties.stacksTo(1));
        SGItems.PDD = SGItems.register(sink, "pdd", properties -> new ToolItem(properties, ToolActions::openPdd),
                properties -> properties.stacksTo(1));
        // Like a PDD, but for a list of gates, wherever they are
        SGItems.GATE_CONTROL_TABLET = SGItems.register(sink, "gate_control_tablet",
                properties -> new ToolItem(properties, ToolActions::openTablet), properties -> properties.stacksTo(1));
        SGItems.GDO = SGItems.register(sink, "gdo", properties -> new ToolItem(properties, ToolActions::openGdo),
                properties -> properties.stacksTo(1));
        SGItems.CONFIGURATOR = SGItems.register(sink, "configurator",
                properties -> new ToolItem(properties, ToolActions::openConfigurator),
                properties -> properties.stacksTo(1));
    }

    private static <I extends Item> I register(final RegistrySink<Item> sink, final String name,
            final Function<Item.Properties, I> factory, final UnaryOperator<Item.Properties> settings) {
        final ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SGCraft.id(name));
        final I item = factory.apply(settings.apply(new Item.Properties().setId(key)));
        sink.register(key, item);
        SGItems.TAB_ITEMS.add(item);
        return item;
    }

    /** @return the items to show in the mod's creative tab, in order */
    public static List<Item> tabItems() {
        return List.copyOf(SGItems.TAB_ITEMS);
    }

    /** @return the key of the mod's creative tab, which each loader builds with its own tab builder */
    public static ResourceKey<CreativeModeTab> tabKey() {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB, SGCraft.id(SGCraft.MOD_ID));
    }

    /** @return the title of the mod's creative tab */
    public static Component tabTitle() {
        return Component.translatable("itemGroup." + SGCraft.MOD_ID);
    }

    private static BlockItem registerBlock(final RegistrySink<Item> sink, final Block block) {
        final ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BuiltInRegistries.BLOCK.getKey(block));
        final BlockItem item = new BlockItem(block, new Item.Properties().setId(key).useBlockDescriptionPrefix());
        sink.register(key, item);
        SGItems.TAB_ITEMS.add(item);
        return item;
    }
}
