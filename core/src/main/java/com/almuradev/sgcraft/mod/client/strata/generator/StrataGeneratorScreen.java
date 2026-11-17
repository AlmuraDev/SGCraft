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

package com.almuradev.sgcraft.mod.client.strata.generator;

import com.almuradev.sgcraft.mod.client.strata.SGStrata;
import com.almuradev.sgcraft.mod.client.strata.StrataMenuScreen;
import com.almuradev.sgcraft.mod.generator.NaquadahGeneratorMenu;
import net.minecraft.client.Minecraft;
import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.button.Button;
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
 * A Naquadah Generator drawn with Strata, laid out as SGCraft 2.3's MalisisCore one was: whether it runs, with On and
 * Off beside it; the gate energy it holds, what it makes and how long its fuel lasts under that; then its four fuel
 * slots, with the store's gauge beside them, and the player's inventory, where the menu puts them. It follows the
 * menu's data as the server syncs it.
 */
public final class StrataGeneratorScreen implements StrataMenuScreen.View<NaquadahGeneratorMenu> {

    /** The menu's slot area, as the standard screen's. */
    public static final int WIDTH = 176;
    public static final int HEIGHT = 166;
    // How far down the slots start, under the read-out, and how tall the window's content is
    private static final float SLOTS_TOP = 56f;
    private static final float CONTENT_HEIGHT = StrataGeneratorScreen.SLOTS_TOP + 162f;
    // The store's gauge, filling upwards, within the slot area
    private static final float GAUGE_X = 8f;
    private static final float GAUGE_Y = 18f;
    private static final float GAUGE_WIDTH = 10f;
    private static final float GAUGE_HEIGHT = 44f;
    private static final Paint GAUGE = Paint.solid(0xFF4FA3E0);

    private final NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.ROOT);
    private @Nullable StrataMenuScreen<NaquadahGeneratorMenu, ?> screen;
    private @Nullable NaquadahGeneratorMenu menu;
    private @Nullable Label status;
    private @Nullable Label stored;
    private @Nullable Label output;
    private @Nullable Label lasts;
    private @Nullable AnchorPanel gauge;
    private @Nullable Button on;
    private @Nullable Button off;

    @Override
    public void attach(final StrataMenuScreen<NaquadahGeneratorMenu, ?> screen) {
        this.screen = screen;
    }

    @Override
    public Component<?> build(final ComponentFactory factory, final NaquadahGeneratorMenu menu,
            final MenuPanel slots) {
        if (this.screen == null) {
            throw new IllegalStateException("Built before it was attached to its screen");
        }
        this.menu = menu;
        this.status = StrataMenuScreen.label(factory, Text.empty(), 5f, 4f);
        this.stored = StrataMenuScreen.label(factory, Text.empty(), 5f, 20f);
        this.output = StrataMenuScreen.label(factory, Text.empty(), 5f, 32f);
        this.lasts = StrataMenuScreen.label(factory, Text.empty(), 5f, 44f);
        // The menu has one button, which switches it the other way: each of 2.3's two presses it while it may
        this.on = SGStrata.button(factory, SGStrata.text("container.sgcraft.naquadah_generator.switch_on", "On"),
                36f, this::toggle);
        this.off = SGStrata.button(factory, SGStrata.text("container.sgcraft.naquadah_generator.switch_off", "Off"),
                36f, this::toggle);
        this.gauge = factory.anchorPanel().setBackground(StrataGeneratorScreen.GAUGE);
        this.gauge.setHitTestVisible(false);
        final AnchorPanel gaugeBox = factory.anchorPanel().setBackground(SGStrata.BOX).setBorder(SGStrata.BOX_BORDER)
                .add(this.gauge.setAnchor(Anchor.FILL).setAlignment(HorizontalAlignment.STRETCH,
                        VerticalAlignment.BOTTOM).setHeight(0f));
        gaugeBox.setHitTestVisible(false);
        gaugeBox.setSize(StrataGeneratorScreen.GAUGE_WIDTH + 2f, StrataGeneratorScreen.GAUGE_HEIGHT + 2f);
        final float top = StrataGeneratorScreen.SLOTS_TOP;
        final AnchorPanel content = factory.anchorPanel()
                .add(slots.setAnchor(Anchor.TOP_LEFT).setMargin(0f, top, 0f, 0f))
                .add(this.status)
                .add(this.on.setAnchor(Anchor.TOP_RIGHT).setMargin(0f, 0f, 41f, 0f))
                .add(this.off.setAnchor(Anchor.TOP_RIGHT).setMargin(0f, 0f, 3f, 0f))
                .add(this.stored)
                .add(this.output)
                .add(this.lasts)
                .add(SGStrata.line(factory, 5f, top - 4f, 5f))
                .add(gaugeBox.setAnchor(Anchor.TOP_LEFT).setMargin(StrataGeneratorScreen.GAUGE_X - 1f,
                        top + StrataGeneratorScreen.GAUGE_Y - 1f, 0f, 0f))
                .add(StrataMenuScreen.label(factory, SGStrata.text("container.sgcraft.naquadah_generator.fuel",
                        "Fuel").color(SGStrata.WHITE), NaquadahGeneratorMenu.FUEL_X, top + NaquadahGeneratorMenu.FUEL_Y
                        - 11f))
                .add(StrataMenuScreen.label(factory, SGStrata.text("container.inventory", "Inventory")
                        .color(SGStrata.WHITE), 8f, top + NaquadahGeneratorMenu.INVENTORY_Y - 11f));
        content.setSize(StrataGeneratorScreen.WIDTH, StrataGeneratorScreen.CONTENT_HEIGHT);
        this.tick();
        return this.screen.window(factory, SGStrata.of(this.screen.getTitle()).color(SGStrata.WHITE), content);
    }

    private void toggle() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (this.menu != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, NaquadahGeneratorMenu.TOGGLE);
        }
    }

    // Whether it runs, in 2.3's colours, its store, output and fuel, the gauge and the switches, as the menu has them
    @Override
    public void tick() {
        final NaquadahGeneratorMenu menu = this.menu;
        if (menu == null || this.status == null || this.stored == null || this.output == null || this.lasts == null
                || this.gauge == null || this.on == null || this.off == null) {
            return;
        }
        final boolean switchedOn = menu.on();
        final boolean running = switchedOn && (menu.stored() > 0 || menu.ticksLeft() > 0);
        final Text state = !switchedOn
                ? SGStrata.text("container.sgcraft.naquadah_generator.off", "Switched off").color(SGStrata.GRAY)
                : running ? SGStrata.text("container.sgcraft.naquadah_generator.running", "Running")
                        .color(SGStrata.AQUA)
                : SGStrata.text("container.sgcraft.naquadah_generator.no_fuel", "No fuel").color(SGStrata.RED);
        this.status.setText(SGStrata.text("container.sgcraft.naquadah_generator.status", "Status").append(": ")
                .color(SGStrata.WHITE).append(state));
        this.stored.setText(StrataMenuScreen.text(SGStrata.GRAY, "container.sgcraft.naquadah_generator.stored",
                "%s/%s GE", StrataMenuScreen.value(this.numbers.format(menu.stored()), SGStrata.WHITE),
                this.numbers.format(menu.capacity())));
        this.output.setText(StrataMenuScreen.text(SGStrata.GRAY, "container.sgcraft.naquadah_generator.output",
                "Makes %s GE a tick", StrataMenuScreen.value(StrataGeneratorScreen.decimal(menu.perTick()),
                        SGStrata.YELLOW)));
        final long seconds = menu.ticksLeft() / 20;
        this.lasts.setText(StrataMenuScreen.text(SGStrata.GRAY, "container.sgcraft.naquadah_generator.lasts",
                "Fuel lasts %s", StrataMenuScreen.value(StrataGeneratorScreen.duration(seconds),
                        seconds > 0 ? SGStrata.WHITE : SGStrata.RED)));
        final long capacity = Math.max(1, menu.capacity());
        this.gauge.setHeight((float) Math.round(StrataGeneratorScreen.GAUGE_HEIGHT * Math.min(1.0,
                (double) menu.stored() / capacity)));
        this.on.setEnabled(!switchedOn);
        this.off.setEnabled(switchedOn);
    }

    private static String decimal(final double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : String.format(Locale.ROOT, "%.2f", value);
    }

    // As hours, minutes and seconds, leaving out what is none
    private static String duration(final long seconds) {
        final long hours = seconds / 3600;
        final long minutes = seconds / 60 % 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes > 0 ? minutes + "m " + seconds % 60 + "s" : seconds + "s";
    }
}
