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

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.block.entity.SGBlockEntityTypes;
import com.almuradev.sgcraft.mod.client.SGClient;
import com.almuradev.sgcraft.mod.client.computer.DialingComputerRenderer;
import com.almuradev.sgcraft.mod.client.dhd.DhdFuelScreen;
import com.almuradev.sgcraft.mod.client.dhd.DhdItemRenderer;
import com.almuradev.sgcraft.mod.client.dhd.DhdRenderer;
import com.almuradev.sgcraft.mod.client.gate.GateBaseScreen;
import com.almuradev.sgcraft.mod.client.gate.GateFeatureRenderer;
import com.almuradev.sgcraft.mod.client.gate.StargateBaseRenderer;
import com.almuradev.sgcraft.mod.client.malp.MalpConsoleRenderer;
import com.almuradev.sgcraft.mod.client.malp.MalpModel;
import com.almuradev.sgcraft.mod.client.malp.MalpRenderer;
import com.almuradev.sgcraft.mod.client.malp.MalpScreen;
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
import com.almuradev.sgcraft.mod.malp.MalpMenu;
import com.almuradev.sgcraft.mod.power.BatteryChargerMenu;
import com.almuradev.sgcraft.mod.power.PowerUnitMenu;
import com.almuradev.sgcraft.mod.shield.CityShieldMenu;
import com.almuradev.sgcraft.mod.zpm.ZpmMenu;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterFeatureRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Sets up the client: the gate renderer, and the renderer that draws gates' parts.
 */
@EventBusSubscriber(modid = SGCraft.MOD_ID, value = Dist.CLIENT)
public final class SGCraftClientNeoForge {

    private SGCraftClientNeoForge() {
    }

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(SGBlockEntityTypes.STARGATE_BASE, StargateBaseRenderer::new);
        event.registerBlockEntityRenderer(SGBlockEntityTypes.DHD, DhdRenderer::new);
        event.registerBlockEntityRenderer(SGBlockEntityTypes.MALP_CONSOLE, MalpConsoleRenderer::new);
        event.registerBlockEntityRenderer(SGBlockEntityTypes.DIALING_COMPUTER, DialingComputerRenderer::new);
        event.registerBlockEntityRenderer(SGBlockEntityTypes.CITY_SHIELD, CityShieldRenderer::new);
        // The iris's plates are unseen; the iris itself is drawn with its gate
        event.registerEntityRenderer(SGEntityTypes.IRIS_PLATE, NoopRenderer::new);
        event.registerEntityRenderer(SGEntityTypes.SEAT, NoopRenderer::new);
        event.registerEntityRenderer(SGEntityTypes.MALP, MalpRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MalpModel.LAYER, MalpModel::createLayer);
        event.registerLayerDefinition(MalpModel.GLOW_LAYER, MalpModel::createGlowLayer);
    }

    @SubscribeEvent
    public static void setUp(final FMLClientSetupEvent event) {
        SGClient.init();
        // A remote view goes when the client leaves the server
        NeoForge.EVENT_BUS.addListener((final ClientPlayerNetworkEvent.LoggingOut loggingOut) ->
                RemoteViewClient.disconnected());
    }

    @SubscribeEvent
    public static void registerScreens(final RegisterMenuScreensEvent event) {
        event.register(DhdFuelMenu.TYPE, DhdFuelScreen::new);
        event.register(GateBaseMenu.TYPE, GateBaseScreen::new);
        event.register(PowerUnitMenu.TYPE, PowerUnitScreen::new);
        event.register(BatteryChargerMenu.TYPE, BatteryChargerScreen::new);
        event.register(MalpMenu.TYPE, MalpScreen::new);
        event.register(ZpmMenu.SINGLE, ZpmScreen::new);
        event.register(ZpmMenu.HUB, ZpmScreen::new);
        event.register(CityShieldMenu.TYPE, CityShieldScreen::new);
    }

    @SubscribeEvent
    public static void registerItemRenderers(final RegisterSpecialModelRendererEvent event) {
        event.register(DhdItemRenderer.TYPE, DhdItemRenderer.Unbaked.CODEC);
    }

    @SubscribeEvent
    public static void registerFeatureRenderers(final RegisterFeatureRenderersEvent event) {
        event.register(GateFeatureRenderer.TYPE, new GateFeatureRenderer());
    }

    // Remote views, drawn into screens as pictures in pictures
    @SubscribeEvent
    public static void registerPictureInPicture(final RegisterPictureInPictureRenderersEvent event) {
        event.register(RemoteViewPicture.State.class, RemoteViewPicture::new);
    }
}
