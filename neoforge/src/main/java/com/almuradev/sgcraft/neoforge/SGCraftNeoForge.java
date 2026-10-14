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

package com.almuradev.sgcraft.neoforge;

import com.almuradev.sgcraft.SGCraft;
import com.almuradev.sgcraft.api.SGCraftRegistries;
import com.almuradev.sgcraft.block.SGBlocks;
import com.almuradev.sgcraft.block.entity.SGBlockEntityTypes;
import com.almuradev.sgcraft.gate.SGGateTypes;
import com.almuradev.sgcraft.item.SGItems;
import com.almuradev.sgcraft.registry.SGRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

@Mod(SGCraft.MOD_ID)
public final class SGCraftNeoForge {

    public SGCraftNeoForge(final IEventBus modBus) {
        SGCraft.init();
        modBus.addListener(SGCraftNeoForge::createRegistries);
        modBus.addListener(SGCraftNeoForge::register);
    }

    private static void createRegistries(final NewRegistryEvent event) {
        SGRegistries.createAll(new SGRegistries.Creator() {
            @Override
            public <T> void create(final ResourceKey<Registry<T>> key) {
                event.create(new RegistryBuilder<>(key));
            }
        });
    }

    // NeoForge fires this once per registry, blocks before items and block entity types
    private static void register(final RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> SGBlocks.register(helper::register));
        event.register(Registries.ITEM, helper -> SGItems.register(helper::register));
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> SGBlockEntityTypes.register(helper::register));
        event.register(SGCraftRegistries.GATE_TYPE, helper -> SGGateTypes.register(helper::register));
        event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(SGItems.tabKey(), CreativeModeTab.builder()
                .title(SGItems.tabTitle())
                .icon(() -> new ItemStack(SGItems.STARGATE_CHEVRON))
                .displayItems((parameters, output) -> SGItems.tabItems().forEach(output::accept))
                .build()));
    }
}
