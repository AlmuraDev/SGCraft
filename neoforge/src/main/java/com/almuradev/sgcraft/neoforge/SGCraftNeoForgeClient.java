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
import com.almuradev.sgcraft.block.entity.SGBlockEntityTypes;
import com.almuradev.sgcraft.client.SGClient;
import com.almuradev.sgcraft.client.dhd.DhdFuelScreen;
import com.almuradev.sgcraft.client.dhd.DhdItemRenderer;
import com.almuradev.sgcraft.client.dhd.DhdRenderer;
import com.almuradev.sgcraft.client.gate.GateBaseScreen;
import com.almuradev.sgcraft.client.gate.GateFeatureRenderer;
import com.almuradev.sgcraft.client.gate.StargateBaseRenderer;
import com.almuradev.sgcraft.client.power.PowerUnitScreen;
import com.almuradev.sgcraft.dhd.DhdFuelMenu;
import com.almuradev.sgcraft.entity.SGEntityTypes;
import com.almuradev.sgcraft.gate.GateBaseMenu;
import com.almuradev.sgcraft.power.PowerUnitMenu;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterFeatureRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;

/**
 * Sets up the client: the gate renderer, and the renderer that draws gates' parts.
 */
@EventBusSubscriber(modid = SGCraft.MOD_ID, value = Dist.CLIENT)
public final class SGCraftNeoForgeClient {

    private SGCraftNeoForgeClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(SGBlockEntityTypes.STARGATE_BASE, StargateBaseRenderer::new);
        event.registerBlockEntityRenderer(SGBlockEntityTypes.DHD, DhdRenderer::new);
        // The iris's plates are unseen; the iris itself is drawn with its gate
        event.registerEntityRenderer(SGEntityTypes.IRIS_PLATE, NoopRenderer::new);
    }

    @SubscribeEvent
    public static void setUp(final FMLClientSetupEvent event) {
        SGClient.init();
    }

    @SubscribeEvent
    public static void registerScreens(final RegisterMenuScreensEvent event) {
        event.register(DhdFuelMenu.TYPE, DhdFuelScreen::new);
        event.register(GateBaseMenu.TYPE, GateBaseScreen::new);
        event.register(PowerUnitMenu.TYPE, PowerUnitScreen::new);
    }

    @SubscribeEvent
    public static void registerItemRenderers(final RegisterSpecialModelRendererEvent event) {
        event.register(DhdItemRenderer.TYPE, DhdItemRenderer.Unbaked.CODEC);
    }

    @SubscribeEvent
    public static void registerFeatureRenderers(final RegisterFeatureRenderersEvent event) {
        event.register(GateFeatureRenderer.TYPE, new GateFeatureRenderer());
    }
}
