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

package com.almuradev.sgcraft.fabric;

import com.almuradev.sgcraft.api.address.AddressReach;
import com.almuradev.sgcraft.api.gate.GateProperty;
import com.almuradev.sgcraft.command.SGCommands;
import com.almuradev.sgcraft.mod.SGBuiltIns;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.sound.SGSounds;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import com.almuradev.sgcraft.SGCraft;
import com.almuradev.sgcraft.api.SGCraftRegistries;
import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.block.SGBlocks;
import com.almuradev.sgcraft.block.entity.SGBlockEntityTypes;
import com.almuradev.sgcraft.gate.SGGateTypes;
import com.almuradev.sgcraft.item.SGItems;
import com.almuradev.sgcraft.registry.SGRegistries;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.ItemStack;

public final class SGCraftFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        SGCraft.init();
        final Map<ResourceKey<?>, Registry<?>> created = new HashMap<>();
        SGRegistries.createAll(new SGRegistries.Creator() {
            @Override
            public <T> void create(final ResourceKey<Registry<T>> key) {
                created.put(key, FabricRegistryBuilder.create(key).buildAndRegister());
            }
        });
        @SuppressWarnings("unchecked")
        final Registry<GateType> gateTypes = (Registry<GateType>) created.get(SGCraftRegistries.GATE_TYPE);
        SGGateTypes.register((key, type) -> Registry.register(gateTypes, key, type));
        @SuppressWarnings("unchecked")
        final Registry<AddressReach> reaches = (Registry<AddressReach>) created.get(SGCraftRegistries.ADDRESS_REACH);
        SGBuiltIns.registerReaches((key, reach) -> Registry.register(reaches, key, reach));
        @SuppressWarnings("unchecked")
        final Registry<GateProperty<?>> properties =
                (Registry<GateProperty<?>>) created.get(SGCraftRegistries.GATE_PROPERTY);
        SGBuiltIns.registerProperties((key, property) -> Registry.register(properties, key, property));
        SGBlocks.register((key, block) -> Registry.register(BuiltInRegistries.BLOCK, key, block));
        SGSounds.register((key, sound) -> Registry.register(BuiltInRegistries.SOUND_EVENT, key, sound));
        SGItems.register((key, item) -> Registry.register(BuiltInRegistries.ITEM, key, item));
        SGBlockEntityTypes.register((key, type) -> Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, key, type));
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SGItems.tabKey(), FabricCreativeModeTab.builder()
                .title(SGItems.tabTitle())
                .icon(() -> new ItemStack(SGItems.STARGATE_CHEVRON))
                .displayItems((parameters, output) -> SGItems.tabItems().forEach(output::accept))
                .build());
        ServerLifecycleEvents.SERVER_STARTING.register(SGServer::starting);
        ServerTickEvents.END_SERVER_TICK.register(SGServer::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(SGServer::stopped);
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) ->
                SGCommands.register(dispatcher));
    }
}
