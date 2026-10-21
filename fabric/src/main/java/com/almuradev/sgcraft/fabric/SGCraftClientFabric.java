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

import com.almuradev.sgcraft.mod.block.entity.SGBlockEntityTypes;
import com.almuradev.sgcraft.mod.client.SGClient;
import com.almuradev.sgcraft.mod.client.computer.DialingComputerRenderer;
import com.almuradev.sgcraft.mod.client.dhd.DhdFuelScreen;
import com.almuradev.sgcraft.mod.client.dhd.DhdItemRenderer;
import com.almuradev.sgcraft.mod.client.dhd.DhdRenderer;
import com.almuradev.sgcraft.mod.client.gate.GateBaseScreen;
import com.almuradev.sgcraft.mod.client.gate.GateFeatureRenderer;
import com.almuradev.sgcraft.mod.client.gate.StargateBaseRenderer;
import com.almuradev.sgcraft.mod.client.generator.GeneratorItemRenderer;
import com.almuradev.sgcraft.mod.client.generator.GeneratorRenderer;
import com.almuradev.sgcraft.mod.client.generator.GeneratorScreen;
import com.almuradev.sgcraft.mod.client.malp.MalpConsoleRenderer;
import com.almuradev.sgcraft.mod.client.malp.MalpModel;
import com.almuradev.sgcraft.mod.client.malp.MalpRenderer;
import com.almuradev.sgcraft.mod.client.malp.MalpScreen;
import com.almuradev.sgcraft.mod.client.obelisk.ObeliskItemRenderer;
import com.almuradev.sgcraft.mod.client.obelisk.ObeliskRenderer;
import com.almuradev.sgcraft.mod.client.power.BatteryChargerScreen;
import com.almuradev.sgcraft.mod.client.power.PowerUnitScreen;
import com.almuradev.sgcraft.mod.client.shield.CityShieldRenderer;
import com.almuradev.sgcraft.mod.client.shield.CityShieldScreen;
import com.almuradev.sgcraft.mod.client.view.RemoteViewClient;
import com.almuradev.sgcraft.mod.client.view.RemoteViewPicture;
import com.almuradev.sgcraft.mod.client.zpm.ZpmScreen;
import com.almuradev.sgcraft.mod.dhd.DhdFuelMenu;
import com.almuradev.sgcraft.mod.entity.SGEntityTypes;
import com.almuradev.sgcraft.mod.gate.GateBaseMenu;
import com.almuradev.sgcraft.mod.generator.NaquadahGeneratorMenu;
import com.almuradev.sgcraft.mod.malp.MalpMenu;
import com.almuradev.sgcraft.mod.network.SGNetwork;
import com.almuradev.sgcraft.mod.power.BatteryChargerMenu;
import com.almuradev.sgcraft.mod.power.PowerUnitMenu;
import com.almuradev.sgcraft.mod.shield.CityShieldMenu;
import com.almuradev.sgcraft.mod.zpm.ZpmMenu;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.FeatureRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;


/**
 * Sets up the client: the gate renderer, and the renderer that draws gates' parts.
 */
public final class SGCraftClientFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BlockEntityRendererRegistry.register(SGBlockEntityTypes.STARGATE_BASE, StargateBaseRenderer::new);
        FeatureRendererRegistry.register(GateFeatureRenderer.TYPE, GateFeatureRenderer::new);
        BlockEntityRendererRegistry.register(SGBlockEntityTypes.DHD, DhdRenderer::new);
        BlockEntityRendererRegistry.register(SGBlockEntityTypes.NAQUADAH_GENERATOR, GeneratorRenderer::new);
        BlockEntityRendererRegistry.register(SGBlockEntityTypes.ASGARD_OBELISK, ObeliskRenderer::new);
        BlockEntityRendererRegistry.register(SGBlockEntityTypes.MALP_CONSOLE, MalpConsoleRenderer::new);
        BlockEntityRendererRegistry.register(SGBlockEntityTypes.DIALING_COMPUTER, DialingComputerRenderer::new);
        BlockEntityRendererRegistry.register(SGBlockEntityTypes.CITY_SHIELD, CityShieldRenderer::new);
        // The iris's plates are unseen; the iris itself is drawn with its gate
        EntityRendererRegistry.register(SGEntityTypes.IRIS_PLATE, NoopRenderer::new);
        EntityRendererRegistry.register(SGEntityTypes.SEAT, NoopRenderer::new);
        ModelLayerRegistry.registerModelLayer(MalpModel.LAYER, MalpModel::createLayer);
        ModelLayerRegistry.registerModelLayer(MalpModel.GLOW_LAYER, MalpModel::createGlowLayer);
        EntityRendererRegistry.register(SGEntityTypes.MALP, MalpRenderer::new);
        // Into vanilla's own list, which the access widener opens, as Fabric API has no way to register one
        SpecialModelRenderers.ID_MAPPER.put(DhdItemRenderer.TYPE, DhdItemRenderer.Unbaked.CODEC);
        SpecialModelRenderers.ID_MAPPER.put(GeneratorItemRenderer.TYPE, GeneratorItemRenderer.Unbaked.CODEC);
        SpecialModelRenderers.ID_MAPPER.put(ObeliskItemRenderer.TYPE, ObeliskItemRenderer.Unbaked.CODEC);
        SGClient.init();
        MenuScreens.register(DhdFuelMenu.TYPE, DhdFuelScreen::new);
        MenuScreens.register(GateBaseMenu.TYPE, GateBaseScreen::new);
        MenuScreens.register(PowerUnitMenu.TYPE, PowerUnitScreen::new);
        MenuScreens.register(NaquadahGeneratorMenu.TYPE, GeneratorScreen::new);
        MenuScreens.register(BatteryChargerMenu.TYPE, BatteryChargerScreen::new);
        MenuScreens.register(MalpMenu.TYPE, MalpScreen::new);
        MenuScreens.register(ZpmMenu.SINGLE, ZpmScreen::new);
        MenuScreens.register(ZpmMenu.HUB, ZpmScreen::new);
        MenuScreens.register(CityShieldMenu.TYPE, CityShieldScreen::new);
        SGNetwork.clientbound().forEach(SGCraftClientFabric::receive);
        // Remote views, drawn into screens as pictures in pictures, and dropped when the client leaves the server
        PictureInPictureRendererRegistry.register(context -> new RemoteViewPicture());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> RemoteViewClient.disconnected());
    }

    // Fabric runs these receivers on the client's main thread
    private static <T extends CustomPacketPayload> void receive(final SGNetwork.Clientbound<T> spec) {
        ClientPlayNetworking.registerGlobalReceiver(spec.type(),
                (payload, context) -> SGNetwork.handleOnClient(payload));
    }
}
