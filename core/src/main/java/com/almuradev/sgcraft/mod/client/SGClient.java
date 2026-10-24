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

package com.almuradev.sgcraft.mod.client;

import com.almuradev.sgcraft.mod.client.computer.ComputerConfigScreen;
import com.almuradev.sgcraft.mod.client.computer.DialingComputerScreen;
import com.almuradev.sgcraft.mod.client.dhd.DhdScreen;
import com.almuradev.sgcraft.mod.client.gate.GateAddressScreen;
import com.almuradev.sgcraft.mod.client.gate.GateBaseScreen;
import com.almuradev.sgcraft.mod.client.malp.MalpConfigScreen;
import com.almuradev.sgcraft.mod.client.malp.MalpConsoleScreen;
import com.almuradev.sgcraft.mod.client.malp.MalpConsoleUsers;
import com.almuradev.sgcraft.mod.client.malp.MalpMonitorDisplays;
import com.almuradev.sgcraft.mod.client.obelisk.BeamEffects;
import com.almuradev.sgcraft.mod.client.obelisk.ObeliskScreen;
import com.almuradev.sgcraft.mod.client.settings.SettingsScreen;
import com.almuradev.sgcraft.mod.client.shield.CityShieldScreen;
import com.almuradev.sgcraft.mod.client.shield.ShieldConsoleScreen;
import com.almuradev.sgcraft.mod.client.tool.ConfiguratorScreen;
import com.almuradev.sgcraft.mod.client.tool.ExplorerScreen;
import com.almuradev.sgcraft.mod.client.tool.GdoScreen;
import com.almuradev.sgcraft.mod.client.tool.PddScreen;
import com.almuradev.sgcraft.mod.client.tool.TabletScreen;
import com.almuradev.sgcraft.mod.client.view.RemoteViewClient;
import com.almuradev.sgcraft.mod.computer.ComputerPayloads;
import com.almuradev.sgcraft.mod.dhd.DhdHooks;
import com.almuradev.sgcraft.mod.gate.GateBaseInfo;
import com.almuradev.sgcraft.mod.malp.MalpPayloads;
import com.almuradev.sgcraft.mod.network.SGNetwork;
import com.almuradev.sgcraft.mod.obelisk.ObeliskPayloads;
import com.almuradev.sgcraft.mod.settings.SettingsPayloads;
import com.almuradev.sgcraft.mod.settings.SharedSettings;
import com.almuradev.sgcraft.mod.shield.ShieldPayloads;
import com.almuradev.sgcraft.mod.tool.ExplorerPayloads;
import com.almuradev.sgcraft.mod.tool.ToolPayloads;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Sets up the client side of what both loaders share: the player's own settings, the DHD's screen, and the tools'
 * screens, which open when the server sends a tool's state and update in place when it sends more. Each loader calls
 * {@link #init()} once.
 */
public final class SGClient {

    private SGClient() {
    }

    public static void init() {
        SGClientConfig.load();
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
        RemoteViewClient.init();
        SGNetwork.onClient(MalpPayloads.ConsoleState.TYPE, state -> SGClient.show(MalpConsoleScreen.class, state,
                state.open(), MalpConsoleScreen::new, MalpConsoleScreen::update));
        SGNetwork.onClient(MalpPayloads.ConfigState.TYPE, state -> SGClient.show(MalpConfigScreen.class, state,
                state.open(), MalpConfigScreen::new, MalpConfigScreen::update));
        SGNetwork.onClient(MalpPayloads.ConsoleUse.TYPE, MalpConsoleUsers::update);
        SGNetwork.onClient(MalpPayloads.MonitorState.TYPE, MalpMonitorDisplays::update);
        SGNetwork.onClient(ExplorerPayloads.State.TYPE, state -> SGClient.show(ExplorerScreen.class, state,
                state.open(), ExplorerScreen::new, ExplorerScreen::update));
        // The generator's screen opens with its menu; its state only fills it
        SGNetwork.onClient(ShieldPayloads.CityState.TYPE, state -> {
            if (Minecraft.getInstance().gui.screen() instanceof final CityShieldScreen screen) {
                screen.update(state);
            }
        });
        SGNetwork.onClient(ComputerPayloads.Screen.TYPE, SGClient::dialingComputer);
        SGNetwork.onClient(ComputerPayloads.Notice.TYPE, SGClient::computerNotice);
        SGNetwork.onClient(ComputerPayloads.ConfigState.TYPE, state -> SGClient.show(ComputerConfigScreen.class,
                state, state.open(), ComputerConfigScreen::new, ComputerConfigScreen::update));
        SGNetwork.onClient(ObeliskPayloads.Beam.TYPE, BeamEffects::handle);
        SGNetwork.onClient(ObeliskPayloads.State.TYPE, state -> SGClient.show(ObeliskScreen.class, state,
                state.open(), ObeliskScreen::new, ObeliskScreen::update));
        SGNetwork.onClient(ShieldPayloads.ConsoleState.TYPE, state -> SGClient.show(ShieldConsoleScreen.class, state,
                state.open(), ShieldConsoleScreen::new, ShieldConsoleScreen::update));
        SGNetwork.onClient(SettingsPayloads.State.TYPE, state -> SGClient.show(SettingsScreen.class, state,
                state.open(), SettingsScreen::new, SettingsScreen::update));
        SGNetwork.onClient(SettingsPayloads.Confirm.TYPE, confirm -> {
            if (Minecraft.getInstance().gui.screen() instanceof final SettingsScreen screen) {
                screen.confirm(confirm);
            }
        });
        SGNetwork.onClient(SharedSettings.Packet.TYPE, packet -> SharedSettings.receive(packet.values()));
    }

    /** Forgets what the server the player has left sent. Each loader calls this as the client disconnects. */
    public static void disconnected() {
        RemoteViewClient.disconnected();
        SharedSettings.forget();
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

    // A dialing computer's screen opens as the player sits down at it, and closes as they get up
    private static void dialingComputer(final ComputerPayloads.Screen screen) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (screen.open()) {
            minecraft.gui.setScreen(new DialingComputerScreen(screen));
        } else if (minecraft.gui.screen() instanceof final DialingComputerScreen open
                && open.computer().equals(screen.computer())) {
            minecraft.gui.setScreen(null);
        }
    }

    // What a dialing computer tells its player shows on its screen if that is open, and over the hotbar if not
    private static void computerNotice(final ComputerPayloads.Notice notice) {
        final Minecraft minecraft = Minecraft.getInstance();
        final Component text = Component.translatableWithFallback(notice.key(), notice.fallback());
        if (minecraft.gui.screen() instanceof final DialingComputerScreen open
                && open.computer().equals(notice.computer())) {
            open.notice(text);
        } else if (minecraft.player != null) {
            minecraft.player.sendOverlayMessage(text.copy().withStyle(ChatFormatting.RED));
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
