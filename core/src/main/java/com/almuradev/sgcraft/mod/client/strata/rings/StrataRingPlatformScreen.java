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

package com.almuradev.sgcraft.mod.client.strata.rings;

import com.almuradev.sgcraft.mod.client.strata.SGStrata;
import com.almuradev.sgcraft.mod.client.strata.StrataMenuScreen;
import com.almuradev.sgcraft.mod.rings.RingPlatformBlockEntity;
import com.almuradev.sgcraft.mod.rings.RingPlatformMenu;
import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.minecraft.MenuPanel;
import org.jspecify.annotations.Nullable;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * A ring platform's fuel drawn with Strata, laid out as SGCraft 2.3's MalisisCore one was: the gate energy it holds,
 * the naquadah in it, how many transports it has left and whether it is linked, one under another; then its four
 * naquadah slots, with the store's gauge beside them, and the player's inventory, where the menu puts them. It follows
 * the menu's data as the server syncs it.
 */
public final class StrataRingPlatformScreen implements StrataMenuScreen.View<RingPlatformMenu> {

    /** The menu's slot area, as the standard screen's. */
    public static final int WIDTH = 176;
    public static final int HEIGHT = 166;
    // How far down the slots start, under the read-out, and how tall the window's content is
    private static final float SLOTS_TOP = 56f;
    private static final float CONTENT_HEIGHT = StrataRingPlatformScreen.SLOTS_TOP + 162f;
    // The store's gauge, filling upwards, within the slot area
    private static final float GAUGE_X = 8f;
    private static final float GAUGE_Y = 18f;
    private static final float GAUGE_WIDTH = 10f;
    private static final float GAUGE_HEIGHT = 44f;
    private static final Paint GAUGE = Paint.solid(0xFF4FA3E0);

    private final NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.ROOT);
    private @Nullable StrataMenuScreen<RingPlatformMenu, ?> screen;
    private @Nullable RingPlatformMenu menu;
    private @Nullable Label stored;
    private @Nullable Label naquadah;
    private @Nullable Label uses;
    private @Nullable Label linked;
    private @Nullable AnchorPanel gauge;

    @Override
    public void attach(final StrataMenuScreen<RingPlatformMenu, ?> screen) {
        this.screen = screen;
    }

    @Override
    public Component<?> build(final ComponentFactory factory, final RingPlatformMenu menu, final MenuPanel slots) {
        if (this.screen == null) {
            throw new IllegalStateException("Built before it was attached to its screen");
        }
        this.menu = menu;
        this.stored = StrataMenuScreen.label(factory, Text.empty(), 5f, 4f);
        this.naquadah = StrataMenuScreen.label(factory, Text.empty(), 5f, 16f);
        this.uses = StrataMenuScreen.label(factory, Text.empty(), 5f, 28f);
        this.linked = StrataMenuScreen.label(factory, Text.empty(), 5f, 40f);
        this.gauge = factory.anchorPanel().setBackground(StrataRingPlatformScreen.GAUGE);
        this.gauge.setHitTestVisible(false);
        final AnchorPanel gaugeBox = factory.anchorPanel().setBackground(SGStrata.BOX).setBorder(SGStrata.BOX_BORDER)
                .add(this.gauge.setAnchor(Anchor.FILL).setAlignment(HorizontalAlignment.STRETCH,
                        VerticalAlignment.BOTTOM).setHeight(0f));
        gaugeBox.setHitTestVisible(false);
        gaugeBox.setSize(StrataRingPlatformScreen.GAUGE_WIDTH + 2f, StrataRingPlatformScreen.GAUGE_HEIGHT + 2f);
        final float top = StrataRingPlatformScreen.SLOTS_TOP;
        final AnchorPanel content = factory.anchorPanel()
                .add(slots.setAnchor(Anchor.TOP_LEFT).setMargin(0f, top, 0f, 0f))
                .add(this.stored)
                .add(this.naquadah)
                .add(this.uses)
                .add(this.linked)
                .add(SGStrata.line(factory, 5f, top - 4f, 5f))
                .add(gaugeBox.setAnchor(Anchor.TOP_LEFT).setMargin(StrataRingPlatformScreen.GAUGE_X - 1f,
                        top + StrataRingPlatformScreen.GAUGE_Y - 1f, 0f, 0f))
                .add(StrataMenuScreen.label(factory, SGStrata.text("container.sgcraft.ring_platform.fuel", "Fuel")
                        .color(SGStrata.WHITE), RingPlatformMenu.FUEL_X, top + RingPlatformMenu.FUEL_Y - 11f))
                .add(StrataMenuScreen.label(factory, SGStrata.text("container.inventory", "Inventory")
                        .color(SGStrata.WHITE), 8f, top + RingPlatformMenu.INVENTORY_Y - 11f));
        content.setSize(StrataRingPlatformScreen.WIDTH, StrataRingPlatformScreen.CONTENT_HEIGHT);
        this.tick();
        return this.screen.window(factory, SGStrata.of(this.screen.getTitle()).color(SGStrata.WHITE), content);
    }

    // Its store, its naquadah, the transports it has left and its link, in 2.3's colours, and the gauge
    @Override
    public void tick() {
        final RingPlatformMenu menu = this.menu;
        if (menu == null || this.stored == null || this.naquadah == null || this.uses == null || this.linked == null
                || this.gauge == null) {
            return;
        }
        this.stored.setText(StrataMenuScreen.text(SGStrata.GRAY, "container.sgcraft.ring_platform.stored",
                "%s/%s GE", StrataMenuScreen.value(this.numbers.format(menu.stored()), SGStrata.WHITE),
                this.numbers.format(menu.capacity())));
        // The naquadah in its slots, as the menu shows them
        int count = 0;
        for (int slot = 0; slot < RingPlatformBlockEntity.SLOTS; slot++) {
            count += menu.getSlot(slot).getItem().getCount();
        }
        this.naquadah.setText(StrataMenuScreen.text(SGStrata.GRAY, "container.sgcraft.ring_platform.naquadah",
                "Naquadah: %s", StrataMenuScreen.value(this.numbers.format(count), SGStrata.WHITE)));
        this.uses.setText(StrataMenuScreen.text(SGStrata.GRAY, "container.sgcraft.ring_platform.uses",
                "Transports left: %s", StrataMenuScreen.value(this.numbers.format(menu.usesLeft()),
                        SGStrata.YELLOW)));
        this.linked.setText(menu.linked()
                ? SGStrata.text("container.sgcraft.ring_platform.linked", "Linked").color(SGStrata.AQUA)
                : SGStrata.text("container.sgcraft.ring_platform.not_linked", "Not linked").color(SGStrata.RED));
        final long capacity = Math.max(1, menu.capacity());
        this.gauge.setHeight((float) Math.round(StrataRingPlatformScreen.GAUGE_HEIGHT * Math.min(1.0,
                (double) menu.stored() / capacity)));
    }
}
