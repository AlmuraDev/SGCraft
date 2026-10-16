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

package com.almuradev.sgcraft.mod.client.power;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.power.PowerUnitMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * A power unit's charge, laid out as the original's screen: its name, a gauge filling red, and how much gate energy it
 * holds against the most it can. Below it are the battery slot, with the battery's charge, and the player's inventory.
 */
public final class PowerUnitScreen extends AbstractContainerScreen<PowerUnitMenu> {

    private static final Identifier BACKGROUND = SGCraft.id("textures/gui/power_unit_battery.png");
    private static final Identifier PANEL = SGCraft.id("textures/gui/power_unit.png");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 180;
    // The original's panel, set into the top of the screen
    private static final int PANEL_X = 14;
    private static final int PANEL_Y = 4;
    private static final int PANEL_WIDTH = 148;
    private static final int PANEL_HEIGHT = 64;
    // The gauge's inside on the art, which fills from the left
    private static final int GAUGE_X = PowerUnitScreen.PANEL_X + 20;
    private static final int GAUGE_Y = PowerUnitScreen.PANEL_Y + 28;
    private static final int GAUGE_WIDTH = 24;
    private static final int GAUGE_HEIGHT = 9;
    private static final int GAUGE_COLOR = 0xFFCC0000;
    // Where the numbers end on the right, and where their labels end, as the original placed them
    private static final int NUMBERS_X = PowerUnitScreen.PANEL_X + 141;
    private static final int LABELS_X = PowerUnitScreen.PANEL_X + 90;
    private static final int STORED_Y = PowerUnitScreen.PANEL_Y + 23;
    private static final int CAPACITY_Y = PowerUnitScreen.PANEL_Y + 45;
    private static final int TEXT_COLOR = 0xFF404040;

    private final NumberFormat format = NumberFormat.getIntegerInstance(Locale.US);

    public PowerUnitScreen(final PowerUnitMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, PowerUnitScreen.WIDTH, PowerUnitScreen.HEIGHT);
        this.titleLabelX = (PowerUnitScreen.WIDTH - this.font.width(title)) / 2;
        this.titleLabelY = PowerUnitScreen.PANEL_Y + 8;
        this.inventoryLabelY = PowerUnitMenu.INVENTORY_Y - 11;
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
            final float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, PowerUnitScreen.BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F,
                PowerUnitScreen.WIDTH, PowerUnitScreen.HEIGHT, PowerUnitScreen.WIDTH, PowerUnitScreen.HEIGHT);
        graphics.blit(RenderPipelines.GUI_TEXTURED, PowerUnitScreen.PANEL, this.leftPos + PowerUnitScreen.PANEL_X,
                this.topPos + PowerUnitScreen.PANEL_Y, 0.0F, 0.0F, PowerUnitScreen.PANEL_WIDTH,
                PowerUnitScreen.PANEL_HEIGHT, PowerUnitScreen.PANEL_WIDTH, PowerUnitScreen.PANEL_HEIGHT);
        final long capacity = this.menu.capacity();
        final int filled = capacity <= 0 ? 0
                : (int) Math.round(PowerUnitScreen.GAUGE_WIDTH * Math.min(1.0, (double) this.menu.stored() / capacity));
        if (filled > 0) {
            final int x = this.leftPos + PowerUnitScreen.GAUGE_X;
            final int y = this.topPos + PowerUnitScreen.GAUGE_Y;
            graphics.fill(x, y, x + filled, y + PowerUnitScreen.GAUGE_HEIGHT, PowerUnitScreen.GAUGE_COLOR);
        }
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int xm, final int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, PowerUnitScreen.TEXT_COLOR, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                PowerUnitScreen.TEXT_COLOR, false);
        this.row(graphics, Component.translatableWithFallback("container.sgcraft.power_unit.stored", "GE:"),
                this.menu.stored(), PowerUnitScreen.STORED_Y);
        this.row(graphics, Component.translatableWithFallback("container.sgcraft.power_unit.capacity", "Max:"),
                this.menu.capacity(), PowerUnitScreen.CAPACITY_Y);
        final Component battery = Component.translatableWithFallback("container.sgcraft.power_unit.battery",
                "Battery");
        final int y = PowerUnitMenu.BATTERY_Y + 4;
        graphics.text(this.font, battery, PowerUnitMenu.BATTERY_X - 6 - this.font.width(battery), y,
                PowerUnitScreen.TEXT_COLOR, false);
        final int percent = this.menu.batteryPercent();
        if (percent >= 0) {
            graphics.text(this.font, Component.literal(percent + "%"), PowerUnitMenu.BATTERY_X + 22, y,
                    PowerUnitScreen.TEXT_COLOR, false);
        }
    }

    private void row(final GuiGraphicsExtractor graphics, final Component label, final long value, final int y) {
        final Component number = Component.literal(this.format.format(value));
        graphics.text(this.font, label, PowerUnitScreen.LABELS_X - this.font.width(label), y,
                PowerUnitScreen.TEXT_COLOR, false);
        graphics.text(this.font, number, PowerUnitScreen.NUMBERS_X - this.font.width(number), y,
                PowerUnitScreen.TEXT_COLOR, false);
    }
}
