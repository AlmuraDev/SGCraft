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
import com.almuradev.sgcraft.mod.power.BatteryChargerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * A battery charger: its naquadah slot on the left, beside a bar for how full its store is, and its battery bay on
 * the right, beside a bar for how full the battery is, over the player's inventory.
 */
public final class BatteryChargerScreen extends AbstractContainerScreen<BatteryChargerMenu> {

    private static final Identifier BACKGROUND = SGCraft.id("textures/gui/battery_charger.png");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 166;
    // The insides of the two bars on the art, which fill from the bottom
    private static final int STORE_X = 22;
    private static final int BATTERY_X = 142;
    private static final int BAR_TOP = 18;
    private static final int BAR_WIDTH = 10;
    private static final int BAR_HEIGHT = 50;
    private static final int STORE_COLOR = 0xFFE0A020;
    private static final int BATTERY_COLOR = 0xFF3FD07F;
    private static final int TEXT_COLOR = 0xFF404040;

    public BatteryChargerScreen(final BatteryChargerMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, BatteryChargerScreen.WIDTH, BatteryChargerScreen.HEIGHT);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
            final float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BatteryChargerScreen.BACKGROUND, this.leftPos, this.topPos, 0.0F,
                0.0F, BatteryChargerScreen.WIDTH, BatteryChargerScreen.HEIGHT, 256, 256);
        this.bar(graphics, BatteryChargerScreen.STORE_X, this.menu.data().get(BatteryChargerMenu.STORE_PERCENT),
                BatteryChargerScreen.STORE_COLOR);
        this.bar(graphics, BatteryChargerScreen.BATTERY_X, this.menu.data().get(BatteryChargerMenu.BATTERY_PERCENT),
                BatteryChargerScreen.BATTERY_COLOR);
    }

    private void bar(final GuiGraphicsExtractor graphics, final int x, final int percent, final int color) {
        final int filled = Math.round(BatteryChargerScreen.BAR_HEIGHT * Math.max(0, Math.min(percent, 100)) / 100F);
        if (filled > 0) {
            final int left = this.leftPos + x;
            final int bottom = this.topPos + BatteryChargerScreen.BAR_TOP + BatteryChargerScreen.BAR_HEIGHT;
            graphics.fill(left, bottom - filled, left + BatteryChargerScreen.BAR_WIDTH, bottom, color);
        }
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int xm, final int ym) {
        super.extractLabels(graphics, xm, ym);
        final int battery = this.menu.data().get(BatteryChargerMenu.BATTERY_PERCENT);
        if (battery >= 0) {
            final Component text = Component.literal(battery + "%");
            graphics.text(this.font, text, BatteryChargerMenu.BATTERY_X + 8 - this.font.width(text) / 2,
                    BatteryChargerMenu.BATTERY_Y + 20, BatteryChargerScreen.TEXT_COLOR, false);
        }
    }
}
