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

package com.almuradev.sgcraft.mod.client.zpm;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.zpm.ZpmMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * A ZPM console's, interface cart's or hub's screen, on the original's art: its ZPM slots, the energy its ZPMs hold,
 * and for a hub how much further that goes with more of them in it.
 */
public final class ZpmScreen extends AbstractContainerScreen<ZpmMenu> {

    private static final Identifier SINGLE = SGCraft.id("textures/gui/zpm_gui.png");
    private static final Identifier HUB = SGCraft.id("textures/gui/zpm_hub_gui.png");
    private static final int WIDTH = 256;
    private static final int HEIGHT = 208;
    private static final int TEXTURE = 256;
    private static final int TEXT_COLOR = 0xFF004C66;
    private static final int TEXT_X = 60;

    private final NumberFormat format = NumberFormat.getIntegerInstance(Locale.US);

    public ZpmScreen(final ZpmMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, ZpmScreen.WIDTH, ZpmScreen.HEIGHT);
        this.titleLabelX = (ZpmScreen.WIDTH - this.font.width(title)) / 2;
        this.titleLabelY = 8;
        this.inventoryLabelX = ZpmMenu.INVENTORY_X;
        this.inventoryLabelY = ZpmMenu.INVENTORY_Y - 11;
    }

    private boolean hub() {
        return this.menu.zpmSlots() > 1;
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
            final float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, this.hub() ? ZpmScreen.HUB : ZpmScreen.SINGLE, this.leftPos,
                this.topPos, 0.0F, 0.0F, ZpmScreen.WIDTH, ZpmScreen.HEIGHT, ZpmScreen.TEXTURE, ZpmScreen.TEXTURE);
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int xm, final int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, ZpmScreen.TEXT_COLOR, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                ZpmScreen.TEXT_COLOR, false);
        final Component slots = this.hub()
                ? Component.translatableWithFallback("container.sgcraft.zpm.slots", "ZPM Slots")
                : Component.translatableWithFallback("container.sgcraft.zpm.slot", "ZPM");
        graphics.text(this.font, slots, ZpmMenu.SLOT_X + (this.hub() ? 3 : 0), ZpmMenu.SLOT_Y - 12,
                ZpmScreen.TEXT_COLOR, false);
        final int loaded = this.menu.loaded();
        if (this.hub() && loaded > 1) {
            graphics.text(this.font, Component.translatableWithFallback("container.sgcraft.zpm.efficiency",
                    "Optimization factor: x%s", loaded), ZpmScreen.TEXT_X, 90, ZpmScreen.TEXT_COLOR, false);
        }
        graphics.text(this.font, Component.translatableWithFallback("container.sgcraft.zpm.available",
                "Available Power: %s GE", this.format.format(this.menu.stored())), ZpmScreen.TEXT_X, 100,
                ZpmScreen.TEXT_COLOR, false);
    }
}
