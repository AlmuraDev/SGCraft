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

package com.almuradev.sgcraft.mod.client.strata;

import com.almuradev.sgcraft.mod.client.strata.generator.StrataGeneratorScreen;
import com.almuradev.sgcraft.mod.client.strata.obelisk.StrataObeliskScreen;
import com.almuradev.sgcraft.mod.client.strata.rings.StrataRingPlatformScreen;
import com.almuradev.sgcraft.mod.client.strata.shield.StrataCityShieldScreen;
import com.almuradev.sgcraft.mod.client.strata.shield.StrataShieldConsoleScreen;
import com.almuradev.sgcraft.mod.client.strata.tool.StrataConfiguratorScreen;
import com.almuradev.sgcraft.mod.client.strata.tool.StrataExplorerScreen;
import com.almuradev.sgcraft.mod.client.strata.tool.StrataGdoScreen;
import com.almuradev.sgcraft.mod.client.strata.tool.StrataPddScreen;
import com.almuradev.sgcraft.mod.client.strata.zpm.StrataZpmScreen;
import com.almuradev.sgcraft.mod.generator.NaquadahGeneratorMenu;
import com.almuradev.sgcraft.mod.obelisk.ObeliskPayloads;
import com.almuradev.sgcraft.mod.rings.RingPlatformMenu;
import com.almuradev.sgcraft.mod.shield.CityShieldMenu;
import com.almuradev.sgcraft.mod.shield.ShieldPayloads;
import com.almuradev.sgcraft.mod.tool.ExplorerPayloads;
import com.almuradev.sgcraft.mod.tool.ToolPayloads;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.PddState;
import com.almuradev.sgcraft.mod.zpm.ZpmMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Where the client shows SGCraft's screens drawn with Strata, in place of the standard ones, as the server's state for
 * each arrives: it updates the one open, or opens one if the server says to. Called only once {@code StrataScreens}
 * says Strata is installed and turned on, as this is the first of SGCraft's classes to name Strata's.
 */
public final class StrataViews {

    private StrataViews() {
    }

    /** The PDD's state. */
    public static void pdd(final PddState state) {
        StrataHost.show(StrataPddScreen.class, state.open(), () -> new StrataPddScreen(state),
                screen -> screen.update(state));
    }

    /** The explorer dialer's state. */
    public static void explorer(final ExplorerPayloads.State state) {
        StrataHost.show(StrataExplorerScreen.class, state.open(), () -> new StrataExplorerScreen(state),
                screen -> screen.update(state));
    }

    /** The GDO's state. */
    public static void gdo(final ToolPayloads.GdoState state) {
        StrataHost.show(StrataGdoScreen.class, state.open(), () -> new StrataGdoScreen(state),
                screen -> screen.update(state));
    }

    /** The configurator's state. */
    public static void configurator(final ToolPayloads.ConfiguratorState state) {
        StrataHost.show(StrataConfiguratorScreen.class, state.open(), () -> new StrataConfiguratorScreen(state),
                screen -> screen.update(state));
    }

    /** A shield console's state. */
    public static void shieldConsole(final ShieldPayloads.ConsoleState state) {
        StrataHost.show(StrataShieldConsoleScreen.class, state.open(), () -> new StrataShieldConsoleScreen(state),
                screen -> screen.update(state));
    }

    /** An Asgard obelisk's or a ring platform's settings, from the configurator. */
    public static void obelisk(final ObeliskPayloads.State state) {
        StrataHost.show(StrataObeliskScreen.class, state.open(), () -> new StrataObeliskScreen(state),
                screen -> screen.update(state));
    }

    /** A city shield generator's state, for its screen if it is open; the screen opens with the menu. */
    public static void cityShieldState(final ShieldPayloads.CityState state) {
        StrataMenuScreen.current(StrataCityShieldScreen.class).ifPresent(screen -> screen.update(state));
    }

    /** @return a city shield generator's screen, for its menu */
    public static AbstractContainerScreen<CityShieldMenu> cityShield(final CityShieldMenu menu,
            final Inventory inventory, final Component title) {
        return new StrataMenuScreen<>(menu, inventory, title, StrataCityShieldScreen.WIDTH,
                StrataCityShieldScreen.HEIGHT, new StrataCityShieldScreen());
    }

    /** @return a Naquadah Generator's screen, for its menu */
    public static AbstractContainerScreen<NaquadahGeneratorMenu> generator(final NaquadahGeneratorMenu menu,
            final Inventory inventory, final Component title) {
        return new StrataMenuScreen<>(menu, inventory, title, StrataGeneratorScreen.WIDTH,
                StrataGeneratorScreen.HEIGHT, new StrataGeneratorScreen());
    }

    /** @return a ring platform's fuel screen, for its menu */
    public static AbstractContainerScreen<RingPlatformMenu> ringPlatform(final RingPlatformMenu menu,
            final Inventory inventory, final Component title) {
        return new StrataMenuScreen<>(menu, inventory, title, StrataRingPlatformScreen.WIDTH,
                StrataRingPlatformScreen.HEIGHT, new StrataRingPlatformScreen());
    }

    /** @return a ZPM console's, interface cart's or hub's screen, for its menu (either type) */
    public static AbstractContainerScreen<ZpmMenu> zpm(final ZpmMenu menu, final Inventory inventory,
            final Component title) {
        return new StrataMenuScreen<>(menu, inventory, title, StrataZpmScreen.WIDTH, StrataZpmScreen.HEIGHT,
                new StrataZpmScreen());
    }
}
