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
import com.almuradev.sgcraft.network.SGNetwork;
import com.almuradev.sgcraft.power.PowerUnitMenu;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.FeatureRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;


/**
 * Sets up the client: the gate renderer, and the renderer that draws gates' parts.
 */
public final class SGCraftFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BlockEntityRendererRegistry.register(SGBlockEntityTypes.STARGATE_BASE, StargateBaseRenderer::new);
        FeatureRendererRegistry.register(GateFeatureRenderer.TYPE, GateFeatureRenderer::new);
        BlockEntityRendererRegistry.register(SGBlockEntityTypes.DHD, DhdRenderer::new);
        // The iris's plates are unseen; the iris itself is drawn with its gate
        EntityRendererRegistry.register(SGEntityTypes.IRIS_PLATE, NoopRenderer::new);
        // Into vanilla's own list, which the access widener opens, as Fabric API has no way to register one
        SpecialModelRenderers.ID_MAPPER.put(DhdItemRenderer.TYPE, DhdItemRenderer.Unbaked.CODEC);
        SGClient.init();
        MenuScreens.register(DhdFuelMenu.TYPE, DhdFuelScreen::new);
        MenuScreens.register(GateBaseMenu.TYPE, GateBaseScreen::new);
        MenuScreens.register(PowerUnitMenu.TYPE, PowerUnitScreen::new);
        SGNetwork.clientbound().forEach(SGCraftFabricClient::receive);
    }

    // Fabric runs these receivers on the client's main thread
    private static <T extends CustomPacketPayload> void receive(final SGNetwork.Clientbound<T> spec) {
        ClientPlayNetworking.registerGlobalReceiver(spec.type(),
                (payload, context) -> SGNetwork.handleOnClient(payload));
    }
}
