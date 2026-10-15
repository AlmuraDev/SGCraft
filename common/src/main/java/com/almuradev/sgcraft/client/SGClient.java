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

package com.almuradev.sgcraft.client;

import com.almuradev.sgcraft.client.dhd.DhdScreen;
import com.almuradev.sgcraft.client.gate.GateAddressScreen;
import com.almuradev.sgcraft.client.gate.GateBaseScreen;
import com.almuradev.sgcraft.client.tool.ConfiguratorScreen;
import com.almuradev.sgcraft.client.tool.GdoScreen;
import com.almuradev.sgcraft.client.tool.PddScreen;
import com.almuradev.sgcraft.client.tool.TabletScreen;
import com.almuradev.sgcraft.dhd.DhdHooks;
import com.almuradev.sgcraft.gate.GateBaseInfo;
import com.almuradev.sgcraft.network.SGNetwork;
import com.almuradev.sgcraft.tool.ToolPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Sets up the client side of what both loaders share: the DHD's screen, and the tools' screens, which open when the
 * server sends a tool's state and update in place when it sends more. Each loader calls {@link #init()} once.
 */
public final class SGClient {

    private SGClient() {
    }

    public static void init() {
        DhdHooks.setOpenScreen(pos -> Minecraft.getInstance().gui.setScreen(new DhdScreen(pos)));
        SGNetwork.onClient(GateBaseInfo.TYPE, SGClient::gateBase);
        SGNetwork.onClient(ToolPayloads.PddState.TYPE, state ->
                SGClient.show(PddScreen.class, state, state.open(), PddScreen::new, PddScreen::update));
        SGNetwork.onClient(ToolPayloads.TabletState.TYPE, state ->
                SGClient.show(TabletScreen.class, state, state.open(), TabletScreen::new, TabletScreen::update));
        SGNetwork.onClient(ToolPayloads.GdoState.TYPE, state ->
                SGClient.show(GdoScreen.class, state, state.open(), GdoScreen::new, GdoScreen::update));
        SGNetwork.onClient(ToolPayloads.ConfiguratorState.TYPE, state -> SGClient.show(ConfiguratorScreen.class,
                state, state.open(), ConfiguratorScreen::new, ConfiguratorScreen::update));
    }

    // A gate's base: the address goes to the base's menu the server has just opened, or opens on its own for a player
    // who may only look
    private static void gateBase(final GateBaseInfo info) {
        if (info.menu() == GateBaseInfo.NO_MENU) {
            Minecraft.getInstance().gui.setScreen(new GateAddressScreen(info.gate()));
        } else if (Minecraft.getInstance().gui.screen() instanceof GateBaseScreen screen
                && screen.getMenu().containerId == info.menu()) {
            screen.update(info.gate());
        }
    }

    // Updates the tool's screen if it is open; opens it only when the player has just used the tool, so a reply that
    // arrives after the screen is closed doesn't open it again
    private static <S extends Screen, T> void show(final Class<S> type, final T state, final boolean open,
            final Function<T, S> opener, final BiConsumer<S, T> update) {
        final Screen current = Minecraft.getInstance().gui.screen();
        if (type.isInstance(current)) {
            update.accept(type.cast(current), state);
        } else if (open) {
            Minecraft.getInstance().gui.setScreen(opener.apply(state));
        }
    }
}
