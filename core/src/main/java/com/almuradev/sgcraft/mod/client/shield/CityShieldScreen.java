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

package com.almuradev.sgcraft.mod.client.shield;

import com.almuradev.sgcraft.mod.client.ClientPlatform;
import com.almuradev.sgcraft.mod.shield.CityShieldBlockEntity;
import com.almuradev.sgcraft.mod.shield.CityShieldMenu;
import com.almuradev.sgcraft.mod.shield.ShieldPayloads.CityAction;
import com.almuradev.sgcraft.mod.shield.ShieldPayloads.CityState;
import com.almuradev.sgcraft.mod.shield.ShieldSettings;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * A city shield generator's screen, as the original's: how far the shield reaches all round, up and down; what a
 * shield that size holds, takes and could run for on the energy the generator can draw on; buttons to raise it, to
 * resize it as it stands, and to lower it; and the generator's three ZPM slots above the player's inventory. It keeps
 * up with the generator while it is open.
 */
public final class CityShieldScreen extends AbstractContainerScreen<CityShieldMenu> {

    private static final int WIDTH = 260;
    private static final int HEIGHT = 296;
    private static final int REFRESH_TICKS = 20;
    private static final long TICKS_PER_SECOND = 20;
    // The units a run time is given in, largest first, with a month of thirty days
    private static final long[] UNITS = {30L * 24 * 3600, 7L * 24 * 3600, 24L * 3600, 3600, 60, 1};
    private static final String[] UNIT_NAMES = {"mo", "w", "d", "h", "m", "s"};
    // The screen's colors: its panel, its edge, a slot's edge and inside, and its writing
    private static final int PANEL = 0xFFC6C6C6;
    private static final int EDGE = 0xFF555555;
    private static final int SLOT_EDGE = 0xFF373737;
    private static final int SLOT_INSIDE = 0xFF8B8B8B;
    private static final int TEXT = 0xFF404040;

    // The generator's state, from the server; until it comes, a lowered shield of the default size
    private CityState state = new CityState(false, BlockPos.ZERO, false, 32, 24, 8, 128, 0.333, 0, false, "");
    private boolean known;
    private @Nullable EditBox widthBox;
    private @Nullable EditBox aboveBox;
    private @Nullable EditBox belowBox;
    private @Nullable Button raise;
    private @Nullable Button apply;
    private @Nullable Button lower;
    private int ticks;

    public CityShieldScreen(final CityShieldMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, CityShieldScreen.WIDTH, CityShieldScreen.HEIGHT);
        this.inventoryLabelX = CityShieldMenu.INVENTORY_X;
        this.inventoryLabelY = CityShieldMenu.INVENTORY_Y - 11;
    }

    /** Shows a newer state from the server, keeping what the player has typed, or filling the boxes the first time. */
    public void update(final CityState state) {
        this.state = state;
        if (!this.known) {
            this.known = true;
            this.fill(this.widthBox, state.width());
            this.fill(this.aboveBox, state.above());
            this.fill(this.belowBox, state.below());
        }
        this.updateButtons();
    }

    private void fill(final @Nullable EditBox box, final int value) {
        if (box != null) {
            box.setValue(String.valueOf(value));
        }
    }

    @Override
    protected void init() {
        super.init();
        final int left = this.leftPos + 8;
        final int top = this.topPos;
        this.widthBox = this.field(left + 150, top + 40, this.state.width(), "width");
        this.aboveBox = this.field(left + 150, top + 60, this.state.above(), "above");
        this.belowBox = this.field(left + 150, top + 80, this.state.below(), "below");
        final int y = top + 154;
        final int third = (CityShieldScreen.WIDTH - 16 - 8) / 3;
        this.raise = this.addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "screen.sgcraft.city_shield.raise", "Activate"), b -> this.send(CityAction.Kind.RAISE))
                .bounds(left, y, third, 20).build());
        this.apply = this.addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "screen.sgcraft.city_shield.apply", "Apply"), b -> this.send(CityAction.Kind.APPLY))
                .bounds(left + third + 4, y, third, 20).build());
        this.lower = this.addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "screen.sgcraft.city_shield.lower", "Deactivate"), b -> this.send(CityAction.Kind.LOWER))
                .bounds(left + 2 * (third + 4), y, third, 20).build());
        this.updateButtons();
    }

    private EditBox field(final int x, final int y, final int value, final String key) {
        final EditBox box = this.addRenderableWidget(new EditBox(this.font, x, y, 60, 16,
                Component.translatableWithFallback("screen.sgcraft.city_shield." + key, key)));
        box.setMaxLength(3);
        box.setValue(String.valueOf(value));
        // Only digits stay in the box
        box.setResponder(text -> {
            final String digits = text.replaceAll("[^0-9]", "");
            if (!digits.equals(text)) {
                box.setValue(digits);
            }
        });
        return box;
    }

    // A box's value, kept within the server's limits
    private int value(final @Nullable EditBox box, final int fallback) {
        if (box == null || box.getValue().isEmpty()) {
            return fallback;
        }
        try {
            return Math.max(ShieldSettings.CITY_MIN_RADIUS, Math.min(this.state.maxRadius(),
                    Integer.parseInt(box.getValue())));
        } catch (final NumberFormatException e) {
            return fallback;
        }
    }

    private void send(final CityAction.Kind kind) {
        ClientPlatform.get().sendToServer(new CityAction(this.state.generator(), kind,
                this.value(this.widthBox, this.state.width()), this.value(this.aboveBox, this.state.above()),
                this.value(this.belowBox, this.state.below())));
    }

    private void updateButtons() {
        final boolean control = this.known && this.state.control();
        if (this.raise != null) {
            this.raise.active = control && !this.state.active();
        }
        if (this.apply != null) {
            this.apply.active = control;
        }
        if (this.lower != null) {
            this.lower.active = control && this.state.active();
        }
        for (final EditBox box : new EditBox[] {this.widthBox, this.aboveBox, this.belowBox}) {
            if (box != null) {
                box.setEditable(control);
            }
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.known && ++this.ticks % CityShieldScreen.REFRESH_TICKS == 0) {
            this.send(CityAction.Kind.REFRESH);
        }
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
            final float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        final int x = this.leftPos;
        final int y = this.topPos;
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, CityShieldScreen.EDGE);
        graphics.fill(x + 2, y + 2, x + this.imageWidth - 2, y + this.imageHeight - 2, CityShieldScreen.PANEL);
        for (final Slot slot : this.menu.slots) {
            graphics.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17,
                    CityShieldScreen.SLOT_EDGE);
            graphics.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, CityShieldScreen.SLOT_INSIDE);
        }
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int xm, final int ym) {
        final int left = 8;
        graphics.text(this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2, 8,
                CityShieldScreen.TEXT, false);
        final Component status = this.state.active()
                ? Component.translatableWithFallback("screen.sgcraft.shield.active", "Shield Active")
                : Component.translatableWithFallback("screen.sgcraft.shield.inactive", "Shield Inactive");
        graphics.text(this.font, status, (this.imageWidth - this.font.width(status)) / 2, 19,
                this.state.active() ? 0xFF007F7F : CityShieldScreen.TEXT, false);
        graphics.text(this.font, Component.translatableWithFallback("screen.sgcraft.city_shield.size",
                "Reach from the generator (blocks, up to %s)", this.state.maxRadius()), left, 30,
                CityShieldScreen.TEXT, false);
        graphics.text(this.font, Component.translatableWithFallback("screen.sgcraft.city_shield.width",
                "Width (all around)"), left, 44, CityShieldScreen.TEXT, false);
        graphics.text(this.font, Component.translatableWithFallback("screen.sgcraft.city_shield.above",
                "Above generator"), left, 64, CityShieldScreen.TEXT, false);
        graphics.text(this.font, Component.translatableWithFallback("screen.sgcraft.city_shield.below",
                "Below generator"), left, 84, CityShieldScreen.TEXT, false);
        // What a shield of the size typed would hold, take and run for
        final int width = this.value(this.widthBox, this.state.width());
        final int above = this.value(this.aboveBox, this.state.above());
        final int below = this.value(this.belowBox, this.state.below());
        final double volume = CityShieldBlockEntity.volume(width, above, below);
        final double cost = volume / 1_000_000 * this.state.energyPerMillion();
        graphics.text(this.font, Component.translatableWithFallback("screen.sgcraft.city_shield.volume",
                "Volume (blocks): %s", String.format(Locale.ROOT, "%,d", Math.round(volume))), left, 102,
                CityShieldScreen.TEXT, false);
        graphics.text(this.font, Component.translatableWithFallback("screen.sgcraft.city_shield.cost",
                "Energy per tick: %s", String.format(Locale.ROOT, "%.3f", cost)), left, 114, CityShieldScreen.TEXT,
                false);
        graphics.text(this.font, Component.translatableWithFallback("screen.sgcraft.city_shield.energy",
                "Energy available: %s", String.format(Locale.ROOT, "%,d", this.state.available())), left, 126,
                this.state.available() > 0 ? 0xFF007F00 : 0xFFAA0000, false);
        graphics.text(this.font, Component.translatableWithFallback("screen.sgcraft.city_shield.run_time",
                "Runs for: %s", cost <= 0 ? "-" : CityShieldScreen.time((long) (this.state.available() / cost)
                        / CityShieldScreen.TICKS_PER_SECOND)), left, 138, CityShieldScreen.TEXT, false);
        graphics.text(this.font, Component.translatableWithFallback("screen.sgcraft.city_shield.zpms", "ZPMs"),
                CityShieldMenu.SLOTS_X, CityShieldMenu.SLOTS_Y - 11, CityShieldScreen.TEXT, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                CityShieldScreen.TEXT, false);
        if (!this.state.message().isEmpty()) {
            graphics.text(this.font, this.state.message(), 0, this.imageHeight + 4, 0xFFFF6060, true);
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
        for (int i = 0; i < CityShieldScreen.UNITS.length && shown < 2; i++) {
            final long count = left / CityShieldScreen.UNITS[i];
            if (count > 0 || shown > 0) {
                if (shown > 0) {
                    text.append(' ');
                }
                text.append(count).append(CityShieldScreen.UNIT_NAMES[i]);
                left -= count * CityShieldScreen.UNITS[i];
                shown++;
            }
        }
        return text.toString();
    }
}
