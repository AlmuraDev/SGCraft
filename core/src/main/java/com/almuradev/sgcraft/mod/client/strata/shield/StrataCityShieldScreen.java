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

package com.almuradev.sgcraft.mod.client.strata.shield;

import com.almuradev.sgcraft.mod.client.ClientPlatform;
import com.almuradev.sgcraft.mod.client.strata.SGStrata;
import com.almuradev.sgcraft.mod.client.strata.StrataMenuScreen;
import com.almuradev.sgcraft.mod.shield.CityShieldBlockEntity;
import com.almuradev.sgcraft.mod.shield.CityShieldMenu;
import com.almuradev.sgcraft.mod.shield.ShieldPayloads.CityAction;
import com.almuradev.sgcraft.mod.shield.ShieldPayloads.CityState;
import com.almuradev.sgcraft.mod.shield.ShieldSettings;
import net.minecraft.core.BlockPos;
import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.attribute.text.TextInput;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.minecraft.MenuPanel;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * A city shield generator drawn with Strata, laid out as SGCraft 2.3's MalisisCore one was: whether the shield is up,
 * large, at the top; how far it reaches all round, up and down, in boxes to type in; what a shield that size holds,
 * takes and could run for on the energy the generator can draw on; Activate, Apply and Deactivate across the window;
 * then the generator's three ZPM slots and the player's inventory, where the menu puts them. It asks the server for
 * the generator's state each second once the first has come, as the standard screen does.
 */
public final class StrataCityShieldScreen implements StrataMenuScreen.View<CityShieldMenu> {

    /** The menu's slot area, which the window's content is as large as. */
    public static final int WIDTH = 260;
    public static final int HEIGHT = 292;
    private static final int REFRESH_TICKS = 20;
    private static final long TICKS_PER_SECOND = 20;
    private static final float FIELD_X = 150f;
    private static final float BUTTON_Y = 138f;
    // The units a run time is given in, largest first, with a month of thirty days
    private static final long[] UNITS = {30L * 24 * 3600, 7L * 24 * 3600, 24L * 3600, 3600, 60, 1};
    private static final String[] UNIT_NAMES = {"mo", "w", "d", "h", "m", "s"};

    // The generator's state, from the server; until it comes, a lowered shield of the default size
    private CityState state = new CityState(false, BlockPos.ZERO, false, 32, 24, 8, 128, 0.333, 0, false, "");
    private boolean known;
    private int ticks;
    private @Nullable StrataMenuScreen<CityShieldMenu, ?> screen;
    private @Nullable Label status;
    private @Nullable Label size;
    private @Nullable Label volume;
    private @Nullable Label cost;
    private @Nullable Label energy;
    private @Nullable Label runTime;
    private @Nullable Label message;
    private @Nullable TextBox widthBox;
    private @Nullable TextBox aboveBox;
    private @Nullable TextBox belowBox;
    private @Nullable Button raise;
    private @Nullable Button apply;
    private @Nullable Button lower;

    @Override
    public void attach(final StrataMenuScreen<CityShieldMenu, ?> screen) {
        this.screen = screen;
    }

    /** Shows a newer state from the server, keeping what the player has typed, or filling the boxes the first time. */
    public void update(final CityState state) {
        this.state = state;
        if (!this.known) {
            this.known = true;
            StrataCityShieldScreen.fill(this.widthBox, state.width());
            StrataCityShieldScreen.fill(this.aboveBox, state.above());
            StrataCityShieldScreen.fill(this.belowBox, state.below());
        }
        this.refresh();
    }

    private static void fill(final @Nullable TextBox box, final int value) {
        if (box != null) {
            box.setText(String.valueOf(value));
        }
    }

    @Override
    public Component<?> build(final ComponentFactory factory, final CityShieldMenu menu, final MenuPanel slots) {
        this.status = factory.label("").setTextScale(1.2f).setTextAlignment(HorizontalAlignment.CENTER);
        this.size = StrataMenuScreen.label(factory, Text.empty(), 8f, 18f);
        this.widthBox = this.field(factory, this.state.width());
        this.aboveBox = this.field(factory, this.state.above());
        this.belowBox = this.field(factory, this.state.below());
        this.volume = StrataMenuScreen.label(factory, Text.empty(), 8f, 82f);
        this.cost = StrataMenuScreen.label(factory, Text.empty(), 8f, 94f);
        this.energy = StrataMenuScreen.label(factory, Text.empty(), 8f, 106f);
        this.runTime = StrataMenuScreen.label(factory, Text.empty(), 8f, 118f);
        this.message = StrataMenuScreen.label(factory, Text.empty(), 8f, BUTTON_Y + SGStrata.BUTTON_HEIGHT + 4f)
                .setTextScale(0.8f).setWrap(true);
        this.message.setWidth(WIDTH - 16f);
        final float third = (WIDTH - 16f - 8f) / 3f;
        this.raise = SGStrata.button(factory, SGStrata.text("screen.sgcraft.city_shield.raise", "Activate"), third,
                () -> this.send(CityAction.Kind.RAISE));
        this.apply = SGStrata.button(factory, SGStrata.text("screen.sgcraft.city_shield.apply", "Apply"), third,
                () -> this.send(CityAction.Kind.APPLY));
        this.lower = SGStrata.button(factory, SGStrata.text("screen.sgcraft.city_shield.lower", "Deactivate"), third,
                () -> this.send(CityAction.Kind.LOWER));
        final AnchorPanel content = factory.anchorPanel()
                .add(slots.setAnchor(Anchor.TOP_LEFT))
                .add(this.status.setAnchor(Anchor.TOP_CENTER).setMargin(0f, 2f, 0f, 0f).setWidth(WIDTH))
                .add(this.size)
                .add(this.sizeLabel(factory, "width", "Width (all around)", 32f))
                .add(this.widthBox.setAnchor(Anchor.TOP_LEFT).setMargin(FIELD_X, 32f, 0f, 0f))
                .add(this.sizeLabel(factory, "above", "Above generator", 47f))
                .add(this.aboveBox.setAnchor(Anchor.TOP_LEFT).setMargin(FIELD_X, 47f, 0f, 0f))
                .add(this.sizeLabel(factory, "below", "Below generator", 62f))
                .add(this.belowBox.setAnchor(Anchor.TOP_LEFT).setMargin(FIELD_X, 62f, 0f, 0f))
                .add(this.volume)
                .add(this.cost)
                .add(this.energy)
                .add(this.runTime)
                .add(this.raise.setAnchor(Anchor.TOP_LEFT).setMargin(8f, BUTTON_Y, 0f, 0f))
                .add(this.apply.setAnchor(Anchor.TOP_LEFT).setMargin(12f + third, BUTTON_Y, 0f, 0f))
                .add(this.lower.setAnchor(Anchor.TOP_LEFT).setMargin(16f + 2f * third, BUTTON_Y, 0f, 0f))
                .add(this.message)
                .add(StrataMenuScreen.label(factory, SGStrata.text("screen.sgcraft.city_shield.zpms", "ZPMs")
                        .color(SGStrata.WHITE), CityShieldMenu.SLOTS_X, CityShieldMenu.SLOTS_Y - 11))
                .add(StrataMenuScreen.label(factory, SGStrata.text("container.inventory", "Inventory")
                        .color(SGStrata.WHITE), CityShieldMenu.INVENTORY_X, CityShieldMenu.INVENTORY_Y - 11));
        content.setSize(WIDTH, HEIGHT);
        this.refresh();
        final StrataMenuScreen<CityShieldMenu, ?> screen = this.screen();
        return screen.window(factory, SGStrata.of(screen.getTitle()).color(SGStrata.WHITE), content);
    }

    private StrataMenuScreen<CityShieldMenu, ?> screen() {
        if (this.screen == null) {
            throw new IllegalStateException("Built before it was attached to its screen");
        }
        return this.screen;
    }

    // A size's label, as 2.3's, left of its box
    private Component<?> sizeLabel(final ComponentFactory factory, final String key, final String fallback,
            final float y) {
        return StrataMenuScreen.label(factory, SGStrata.text("screen.sgcraft.city_shield." + key, fallback)
                .append(":").color(SGStrata.WHITE), 10f, y + 4f);
    }

    // A box for a size, that takes only digits
    private TextBox field(final ComponentFactory factory, final int value) {
        final TextBox box = factory.textBox().setMaxLength(3).setText(String.valueOf(value));
        box.setSize(60f, 14f);
        box.onEvent(TextInput.EDIT, event -> event.setValue(event.value().replaceAll("[^0-9]", "")));
        // What a shield of the size typed would hold, take and run for, as it is typed
        box.onEvent(TextInput.EDITED, event -> this.refresh());
        return box;
    }

    // A box's value, kept within the server's limits
    private int value(final @Nullable TextBox box, final int fallback) {
        if (box == null || box.text().isEmpty()) {
            return fallback;
        }
        try {
            return Math.max(ShieldSettings.CITY_MIN_RADIUS, Math.min(this.state.maxRadius(),
                    Integer.parseInt(box.text())));
        } catch (final NumberFormatException e) {
            return fallback;
        }
    }

    private void send(final CityAction.Kind kind) {
        ClientPlatform.get().sendToServer(new CityAction(this.state.generator(), kind,
                this.value(this.widthBox, this.state.width()), this.value(this.aboveBox, this.state.above()),
                this.value(this.belowBox, this.state.below())));
    }

    @Override
    public void tick() {
        if (this.known && ++this.ticks % REFRESH_TICKS == 0) {
            this.send(CityAction.Kind.REFRESH);
        }
    }

    // The shield's state, the read-out for the size typed, in 2.3's colours, and the buttons
    private void refresh() {
        if (this.status == null || this.size == null || this.volume == null || this.cost == null
                || this.energy == null || this.runTime == null || this.message == null || this.raise == null
                || this.apply == null || this.lower == null) {
            return;
        }
        this.status.setText(this.state.active()
                ? SGStrata.text("screen.sgcraft.shield.active", "Shield Active").color(SGStrata.AQUA)
                : SGStrata.text("screen.sgcraft.shield.inactive", "Shield Inactive").color(SGStrata.GRAY));
        this.size.setText(SGStrata.text("screen.sgcraft.city_shield.size",
                "Reach from the generator (blocks, up to %s)", this.state.maxRadius()).color(SGStrata.GRAY));
        final int width = this.value(this.widthBox, this.state.width());
        final int above = this.value(this.aboveBox, this.state.above());
        final int below = this.value(this.belowBox, this.state.below());
        final double volume = CityShieldBlockEntity.volume(width, above, below);
        final double cost = volume / 1_000_000 * this.state.energyPerMillion();
        this.volume.setText(StrataMenuScreen.text(SGStrata.GRAY, "screen.sgcraft.city_shield.volume",
                "Volume (blocks): %s", StrataMenuScreen.value(String.format(Locale.ROOT, "%,d", Math.round(volume)),
                        SGStrata.WHITE)));
        this.cost.setText(StrataMenuScreen.text(SGStrata.GRAY, "screen.sgcraft.city_shield.cost",
                "Energy per tick: %s", StrataMenuScreen.value(String.format(Locale.ROOT, "%.3f", cost),
                        SGStrata.YELLOW)));
        this.energy.setText(StrataMenuScreen.text(SGStrata.GRAY, "screen.sgcraft.city_shield.energy",
                "Energy available: %s", StrataMenuScreen.value(String.format(Locale.ROOT, "%,d",
                        this.state.available()), this.state.available() > 0 ? SGStrata.GREEN : SGStrata.RED)));
        this.runTime.setText(StrataMenuScreen.text(SGStrata.GRAY, "screen.sgcraft.city_shield.run_time",
                "Runs for: %s", StrataMenuScreen.value(cost <= 0 ? "-" : StrataCityShieldScreen.time(
                        (long) (this.state.available() / cost) / TICKS_PER_SECOND), SGStrata.WHITE)));
        this.message.setText(Text.of(this.state.message()).color(SGStrata.RED));
        final boolean control = this.known && this.state.control();
        this.raise.setEnabled(control && !this.state.active());
        this.apply.setEnabled(control);
        this.lower.setEnabled(control && this.state.active());
        for (final TextBox box : new TextBox[] {this.widthBox, this.aboveBox, this.belowBox}) {
            if (box != null) {
                box.setReadOnly(!control);
            }
        }
    }

    // A run time in its two largest units, such as 3d 4h
    private static String time(final long seconds) {
        if (seconds <= 0) {
            return "0s";
        }
        final StringBuilder text = new StringBuilder();
        long left = seconds;
        int shown = 0;
        for (int i = 0; i < UNITS.length && shown < 2; i++) {
            final long count = left / UNITS[i];
            if (count > 0 || shown > 0) {
                if (shown > 0) {
                    text.append(' ');
                }
                text.append(count).append(UNIT_NAMES[i]);
                left -= count * UNITS[i];
                shown++;
            }
        }
        return text.toString();
    }
}
