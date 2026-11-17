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

package com.almuradev.sgcraft.mod.client.strata.zpm;

import com.almuradev.sgcraft.mod.client.strata.SGStrata;
import com.almuradev.sgcraft.mod.client.strata.StrataMenuScreen;
import com.almuradev.sgcraft.mod.zpm.ZpmMenu;
import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.minecraft.MenuPanel;
import org.jspecify.annotations.Nullable;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * A ZPM console's, interface cart's or hub's screen drawn with Strata, in the dark window of SGCraft 2.3's
 * MalisisCore screens: the energy its ZPMs hold, large, at the top, and for a hub how much further that goes with more
 * of them in it; then its ZPM slots and the player's inventory, where the menu puts them. It follows the menu's data
 * as the server syncs it.
 */
public final class StrataZpmScreen implements StrataMenuScreen.View<ZpmMenu> {

    /** The menu's slot area, as the standard screen's. */
    public static final int WIDTH = 256;
    public static final int HEIGHT = 208;
    // How tall the window's content is: down to the bottom of the player's inventory
    private static final float CONTENT_HEIGHT = ZpmMenu.INVENTORY_Y + 4 * 18 + 4;

    private final NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.US);
    private @Nullable StrataMenuScreen<ZpmMenu, ?> screen;
    private @Nullable ZpmMenu menu;
    private @Nullable Label available;
    private @Nullable Label efficiency;

    @Override
    public void attach(final StrataMenuScreen<ZpmMenu, ?> screen) {
        this.screen = screen;
    }

    @Override
    public Component<?> build(final ComponentFactory factory, final ZpmMenu menu, final MenuPanel slots) {
        if (this.screen == null) {
            throw new IllegalStateException("Built before it was attached to its screen");
        }
        this.menu = menu;
        final boolean hub = menu.zpmSlots() > 1;
        this.available = factory.label("").setTextScale(1.2f).setTextAlignment(HorizontalAlignment.CENTER);
        this.available.setHitTestVisible(false);
        this.efficiency = factory.label("").setTextAlignment(HorizontalAlignment.CENTER);
        this.efficiency.setHitTestVisible(false);
        final Text slotsLabel = hub
                ? SGStrata.text("container.sgcraft.zpm.slots", "ZPM Slots")
                : SGStrata.text("container.sgcraft.zpm.slot", "ZPM");
        final AnchorPanel content = factory.anchorPanel()
                .add(slots.setAnchor(Anchor.TOP_LEFT))
                .add(this.available.setAnchor(Anchor.TOP_CENTER).setMargin(0f, 6f, 0f, 0f).setWidth(WIDTH))
                .add(this.efficiency.setAnchor(Anchor.TOP_CENTER).setMargin(0f, 22f, 0f, 0f).setWidth(WIDTH))
                .add(SGStrata.line(factory, 10f, 36f, 10f))
                .add(StrataMenuScreen.label(factory, slotsLabel.color(SGStrata.WHITE),
                        ZpmMenu.SLOT_X + (hub ? 3 : 0), ZpmMenu.SLOT_Y - 12))
                .add(StrataMenuScreen.label(factory, SGStrata.text("container.inventory", "Inventory")
                        .color(SGStrata.WHITE), ZpmMenu.INVENTORY_X, ZpmMenu.INVENTORY_Y - 11));
        content.setSize(WIDTH, CONTENT_HEIGHT);
        this.tick();
        return this.screen.window(factory, SGStrata.of(this.screen.getTitle()).color(SGStrata.WHITE), content);
    }

    // The energy its ZPMs hold, and for a hub with more than one in it, how much further that goes
    @Override
    public void tick() {
        final ZpmMenu menu = this.menu;
        if (menu == null || this.available == null || this.efficiency == null) {
            return;
        }
        this.available.setText(StrataMenuScreen.text(SGStrata.GRAY, "container.sgcraft.zpm.available",
                "Available Power: %s GE", StrataMenuScreen.value(this.numbers.format(menu.stored()),
                        menu.stored() > 0 ? SGStrata.AQUA : SGStrata.RED)));
        final int loaded = menu.loaded();
        this.efficiency.setText(menu.zpmSlots() > 1 && loaded > 1
                ? StrataMenuScreen.text(SGStrata.GRAY, "container.sgcraft.zpm.efficiency",
                        "Optimization factor: x%s", StrataMenuScreen.value(loaded, SGStrata.YELLOW))
                : Text.empty());
    }
}
