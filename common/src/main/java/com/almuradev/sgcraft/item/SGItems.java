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
import com.almuradev.sgcraft.block.SGBlocks;
import com.almuradev.sgcraft.registry.RegistrySink;
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

    private SGItems() {
    }

    public static void register(final RegistrySink<Item> sink) {
        SGItems.STARGATE_RING = SGItems.registerBlock(sink, SGBlocks.STARGATE_RING);
        SGItems.STARGATE_CHEVRON = SGItems.registerBlock(sink, SGBlocks.STARGATE_CHEVRON);
        SGItems.STARGATE_BASE = SGItems.registerBlock(sink, SGBlocks.STARGATE_BASE);
        SGItems.STARGATE_BASE_PEGASUS = SGItems.registerBlock(sink, SGBlocks.STARGATE_BASE_PEGASUS);
        SGItems.STARGATE_BASE_UNIVERSE = SGItems.registerBlock(sink, SGBlocks.STARGATE_BASE_UNIVERSE);
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
