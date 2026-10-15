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
import com.almuradev.sgcraft.command.SGCommands;
import com.almuradev.sgcraft.entity.SGEntityTypes;
import com.almuradev.sgcraft.gate.SGGateTypes;
import com.almuradev.sgcraft.item.SGDataComponents;
import com.almuradev.sgcraft.item.SGItems;
import com.almuradev.sgcraft.mod.GateProtection;
import com.almuradev.sgcraft.mod.SGBuiltIns;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.neoforge.test.SGGameTests;
import com.almuradev.sgcraft.network.SGNetwork;
import com.almuradev.sgcraft.registry.SGRegistries;
import com.almuradev.sgcraft.sound.SGSounds;
import com.almuradev.sgcraft.villager.SGVillagers;
import com.almuradev.sgcraft.worldgen.StructureGates;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

@Mod(SGCraft.MOD_ID)
public final class SGCraftNeoForge {

    public SGCraftNeoForge(final IEventBus modBus) {
        SGCraft.init();
        SGGameTests.register(modBus);
        modBus.addListener(SGCraftNeoForge::createRegistries);
        modBus.addListener(SGCraftNeoForge::register);
        modBus.addListener(SGCraftNeoForge::registerPayloads);
        modBus.addListener(SGCraftNeoForge::registerCapabilities);
        // Before the server makes its worlds, so their structures have gates from the first chunk
        NeoForge.EVENT_BUS.addListener((final ServerAboutToStartEvent event) ->
                StructureGates.prepare(event.getServer()));
        NeoForge.EVENT_BUS.addListener((final ServerStartingEvent event) -> SGServer.starting(event.getServer()));
        NeoForge.EVENT_BUS.addListener((final ServerTickEvent.Post event) -> SGServer.tick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((final ServerStoppedEvent event) -> SGServer.stopped(event.getServer()));
        NeoForge.EVENT_BUS.addListener((final RegisterCommandsEvent event) ->
                SGCommands.register(event.getDispatcher()));
        // Formed gates are kept whole unless they may be broken
        NeoForge.EVENT_BUS.addListener((final BreakBlockEvent event) -> {
            if (event.getPlayer() instanceof ServerPlayer player && event.getLevel() instanceof ServerLevel level
                    && !GateProtection.mayBreak(player, level, event.getPos())) {
                event.setCanceled(true);
            }
        });
    }

    private static void createRegistries(final NewRegistryEvent event) {
        SGRegistries.createAll(new SGRegistries.Creator() {
            @Override
            public <T> void create(final ResourceKey<Registry<T>> key) {
                event.create(new RegistryBuilder<>(key));
            }
        });
    }

    // Every payload, both ways; NeoForge handles them on the main thread
    private static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(SGNetwork.VERSION);
        SGNetwork.serverbound().forEach(spec -> SGCraftNeoForge.serverbound(registrar, spec));
        SGNetwork.clientbound().forEach(spec -> SGCraftNeoForge.clientbound(registrar, spec));
    }

    private static <T extends CustomPacketPayload> void serverbound(final PayloadRegistrar registrar,
            final SGNetwork.Serverbound<T> spec) {
        registrar.playToServer(spec.type(), spec.codec(),
                (payload, context) -> spec.handler().accept((ServerPlayer) context.player(), payload));
    }

    private static <T extends CustomPacketPayload> void clientbound(final PayloadRegistrar registrar,
            final SGNetwork.Clientbound<T> spec) {
        registrar.playToClient(spec.type(), spec.codec(), (payload, context) -> SGNetwork.handleOnClient(payload));
    }

    // NeoForge fires this once per registry, blocks before items and block entity types
    // Power units take FE from cables
    private static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, SGBlockEntityTypes.POWER_UNIT,
                (unit, side) -> unit.handler(PowerUnitEnergy::new));
    }

    private static void register(final RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> SGBlocks.register(helper::register));
        event.register(Registries.DATA_COMPONENT_TYPE, helper -> SGDataComponents.register(helper::register));
        event.register(Registries.ITEM, helper -> SGItems.register(helper::register));
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> SGBlockEntityTypes.register(helper::register));
        event.register(Registries.ENTITY_TYPE, helper -> SGEntityTypes.register(helper::register));
        event.register(Registries.POINT_OF_INTEREST_TYPE, helper -> SGVillagers.registerPoiTypes(helper::register));
        event.register(Registries.VILLAGER_PROFESSION,
                helper -> SGVillagers.registerProfessions(helper::register));
        event.register(SGCraftRegistries.GATE_TYPE, helper -> SGGateTypes.register(helper::register));
        event.register(Registries.SOUND_EVENT, helper -> SGSounds.register(helper::register));
        event.register(Registries.STRUCTURE_PIECE, helper -> StructureGates.register(helper::register));
        event.register(SGCraftRegistries.ADDRESS_REACH, helper -> SGBuiltIns.registerReaches(helper::register));
        event.register(SGCraftRegistries.GATE_PROPERTY, helper -> SGBuiltIns.registerProperties(helper::register));
        event.register(SGCraftRegistries.IRIS_TYPE, helper -> SGBuiltIns.registerIrisTypes(helper::register));
        event.register(SGCraftRegistries.ENERGY_SOURCE_PROVIDER,
                helper -> SGBuiltIns.registerEnergySources(helper::register));
        event.register(SGCraftRegistries.ADDRESS_BOOK_PROVIDER,
                helper -> SGBuiltIns.registerAddressBooks(helper::register));
        event.register(SGCraftRegistries.ENERGY_BRIDGE, helper -> SGBuiltIns.registerEnergyBridges(helper::register));
        event.register(Registries.MENU, helper -> SGBuiltIns.registerMenus(helper::register));
        event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(SGItems.tabKey(),
                CreativeModeTab.builder()
                        .title(SGItems.tabTitle())
                        .icon(() -> new ItemStack(SGItems.STARGATE_CHEVRON))
                        .displayItems((parameters, output) -> SGItems.tabItems().forEach(output::accept))
                        .build()));
    }
}
