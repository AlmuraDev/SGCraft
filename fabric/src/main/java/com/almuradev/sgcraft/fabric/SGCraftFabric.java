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

import com.almuradev.sgcraft.api.SGCraftRegistries;
import com.almuradev.sgcraft.api.address.AddressReach;
import com.almuradev.sgcraft.api.addressbook.AddressBookProvider;
import com.almuradev.sgcraft.api.energy.EnergyBridge;
import com.almuradev.sgcraft.api.energy.EnergySourceProvider;
import com.almuradev.sgcraft.api.gate.GateProperty;
import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.api.gate.IrisType;
import com.almuradev.sgcraft.mod.SGBuiltIns;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.block.SGBlocks;
import com.almuradev.sgcraft.mod.block.entity.SGBlockEntityTypes;
import com.almuradev.sgcraft.mod.command.SGCommands;
import com.almuradev.sgcraft.mod.entity.SGEntityTypes;
import com.almuradev.sgcraft.mod.gate.GateProtection;
import com.almuradev.sgcraft.mod.gate.SGGateTypes;
import com.almuradev.sgcraft.mod.item.SGDataComponents;
import com.almuradev.sgcraft.mod.item.SGItems;
import com.almuradev.sgcraft.mod.malp.MalpLinks;
import com.almuradev.sgcraft.mod.network.SGNetwork;
import com.almuradev.sgcraft.mod.registry.SGRegistries;
import com.almuradev.sgcraft.mod.sound.SGSounds;
import com.almuradev.sgcraft.mod.villager.SGVillagers;
import com.almuradev.sgcraft.mod.worldgen.ChestLoot;
import com.almuradev.sgcraft.mod.worldgen.GateAddressBookFunction;
import com.almuradev.sgcraft.mod.worldgen.NaquadahOre;
import com.almuradev.sgcraft.mod.worldgen.StructureGates;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PoiHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.GenerationStep;
import team.reborn.energy.api.EnergyStorage;

import java.util.HashMap;
import java.util.Map;

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
        @SuppressWarnings("unchecked")
        final Registry<IrisType> irisTypes = (Registry<IrisType>) created.get(SGCraftRegistries.IRIS_TYPE);
        SGBuiltIns.registerIrisTypes((key, type) -> Registry.register(irisTypes, key, type));
        @SuppressWarnings("unchecked")
        final Registry<EnergySourceProvider> energy =
                (Registry<EnergySourceProvider>) created.get(SGCraftRegistries.ENERGY_SOURCE_PROVIDER);
        SGBuiltIns.registerEnergySources((key, provider) -> Registry.register(energy, key, provider));
        final Registry<AddressBookProvider> books =
                (Registry<AddressBookProvider>) created.get(SGCraftRegistries.ADDRESS_BOOK_PROVIDER);
        SGBuiltIns.registerAddressBooks((key, provider) -> Registry.register(books, key, provider));
        final Registry<EnergyBridge> bridges = (Registry<EnergyBridge>) created.get(SGCraftRegistries.ENERGY_BRIDGE);
        SGBuiltIns.registerEnergyBridges((key, bridge) -> Registry.register(bridges, key, bridge));
        SGBuiltIns.registerMenus((key, menu) -> Registry.register(BuiltInRegistries.MENU, key, menu));
        SGBlocks.register((key, block) -> Registry.register(BuiltInRegistries.BLOCK, key, block));
        SGSounds.register((key, sound) -> Registry.register(BuiltInRegistries.SOUND_EVENT, key, sound));
        StructureGates.register((key, type) -> Registry.register(BuiltInRegistries.STRUCTURE_PIECE, key, type));
        NaquadahOre.register((key, type) -> Registry.register(BuiltInRegistries.FEATURE_TYPE, key, type));
        GateAddressBookFunction.register((key, type) -> Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, key,
                type));
        SGDataComponents.register((key, type) -> Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, key, type));
        SGItems.register((key, item) -> Registry.register(BuiltInRegistries.ITEM, key, item));
        SGBlockEntityTypes.register((key, type) -> Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, key, type));
        SGEntityTypes.register((key, type) -> Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type));
        // Power units take FE from Team Reborn Energy's cables
        EnergyStorage.SIDED.registerForBlockEntity((unit, side) -> unit.handler(FeSinkEnergyFabric::new),
                SGBlockEntityTypes.POWER_UNIT);
        // Ring platforms take FE from cables too
        EnergyStorage.SIDED.registerForBlockEntity((platform, side) -> platform.handler(FeSinkEnergyFabric::new),
                SGBlockEntityTypes.RING_PLATFORM);
        // Battery chargers take FE from cables too, and batteries are energy items any charger fills
        EnergyStorage.SIDED.registerForBlockEntity((charger, side) -> charger.handler(BatteryChargerEnergyFabric::new),
                SGBlockEntityTypes.BATTERY_CHARGER);
        EnergyStorage.ITEM.registerForItems((stack, context) -> new BatteryEnergyFabric(context), SGItems.BATTERY);
        SGItems.registerRecipeSerializers((key, serializer) ->
                Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, key, serializer));
        MalpLinks.registerTickets((key, ticket) -> Registry.register(BuiltInRegistries.TICKET_TYPE, key, ticket));
        // Fabric's helper also tells the world which block states are the DHD's
        PoiHelper.register(SGVillagers.DHD.identifier(), 1, 1, SGVillagers.dhdStates());
        SGVillagers.registerProfessions((key, profession) ->
                Registry.register(BuiltInRegistries.VILLAGER_PROFESSION, key, profession));
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SGItems.tabKey(), FabricCreativeModeTab.builder()
                .title(SGItems.tabTitle())
                .icon(() -> new ItemStack(SGItems.STARGATE_CHEVRON))
                .displayItems((parameters, output) -> SGItems.tabItems().forEach(output::accept))
                .build());
        // Every payload, both ways; the client side sets its receivers in SGCraftClientFabric
        SGNetwork.serverbound().forEach(SGCraftFabric::serverbound);
        SGNetwork.clientbound().forEach(spec -> SGCraftFabric.clientbound(spec));
        // Formed gates are kept whole unless they may be broken
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) ->
                !(player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel)
                        || GateProtection.mayBreak(serverPlayer, serverLevel, pos));
        // SGCraft's loot in vanilla's chests; NeoForge does the same with loot modifiers
        LootTableEvents.MODIFY_DROPS.register(ChestLoot::addTo);
        // Naquadah ore in every biome, as it picks its worlds itself; NeoForge reads the same from its biome modifier
        BiomeModifications.addFeature(BiomeSelectors.all(), GenerationStep.Decoration.UNDERGROUND_ORES,
                ResourceKey.create(Registries.PLACED_FEATURE, SGCraft.id("naquadah_ore")));
        // Fabric starts a server before it makes its worlds, so their structures have gates from the first chunk
        ServerLifecycleEvents.SERVER_STARTING.register(StructureGates::prepare);
        ServerLifecycleEvents.SERVER_STARTING.register(SGServer::starting);
        ServerTickEvents.END_SERVER_TICK.register(SGServer::tick);
        // Naquadah ore takes its settings before the worlds are made, and is added to old chunks as they load
        ServerLifecycleEvents.SERVER_STARTING.register(NaquadahOre::prepare);
        ServerChunkEvents.CHUNK_LOAD.register(NaquadahOre::onChunkLoad);
        ServerTickEvents.END_SERVER_TICK.register(NaquadahOre::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(SGServer::stopped);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> SGServer.joined(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SGServer.left(handler.getPlayer()));
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) ->
                SGCommands.register(dispatcher));
    }

    // Fabric runs these receivers on the server thread
    private static <T extends CustomPacketPayload> void serverbound(final SGNetwork.Serverbound<T> spec) {
        PayloadTypeRegistry.serverboundPlay().register(spec.type(), spec.codec());
        ServerPlayNetworking.registerGlobalReceiver(spec.type(),
                (payload, context) -> spec.handler().accept(context.player(), payload));
    }

    private static <T extends CustomPacketPayload> void clientbound(final SGNetwork.Clientbound<T> spec) {
        PayloadTypeRegistry.clientboundPlay().register(spec.type(), spec.codec());
    }
}
