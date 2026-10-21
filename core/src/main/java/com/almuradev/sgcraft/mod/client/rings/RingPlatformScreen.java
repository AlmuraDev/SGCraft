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

package com.almuradev.sgcraft.mod.client.rings;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.rings.RingPlatformMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * A ring platform's screen, as 2.2.0's: the gate energy it holds, whether it is linked, how many transports it has
 * left counting its naquadah, and its four naquadah slots over the player's inventory. It shares the Naquadah
 * Generator's background, which has the same slots and gauge.
 */
public final class RingPlatformScreen extends AbstractContainerScreen<RingPlatformMenu> {

    private static final Identifier BACKGROUND = SGCraft.id("textures/gui/naquadah_generator.png");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 166;
    // The store's gauge, filling upwards, and where the readouts start
    private static final int GAUGE_X = 8;
    private static final int GAUGE_Y = 18;
    private static final int GAUGE_WIDTH = 10;
    private static final int GAUGE_HEIGHT = 44;
    private static final int TEXT_X = 70;
    private static final int TEXT_Y = 18;
    private static final int LINE = 10;

    public RingPlatformScreen(final RingPlatformMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, RingPlatformScreen.WIDTH, RingPlatformScreen.HEIGHT);
        this.inventoryLabelY = RingPlatformMenu.INVENTORY_Y - 11;
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
            final float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, RingPlatformScreen.BACKGROUND, this.leftPos, this.topPos, 0.0F,
                0.0F, RingPlatformScreen.WIDTH, RingPlatformScreen.HEIGHT, 256, 256);
        final long capacity = Math.max(1, this.menu.capacity());
        final int filled = (int) Math.round(RingPlatformScreen.GAUGE_HEIGHT * Math.min(1.0,
                (double) this.menu.stored() / capacity));
        final int x = this.leftPos + RingPlatformScreen.GAUGE_X;
        final int bottom = this.topPos + RingPlatformScreen.GAUGE_Y + RingPlatformScreen.GAUGE_HEIGHT;
        graphics.fill(x, bottom - filled, x + RingPlatformScreen.GAUGE_WIDTH, bottom, 0xFF4FA3E0);
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int xm, final int ym) {
        super.extractLabels(graphics, xm, ym);
        final NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.ROOT);
        int y = RingPlatformScreen.TEXT_Y;
        graphics.text(this.font, this.menu.linked()
                ? Component.translatableWithFallback("container.sgcraft.ring_platform.linked", "Linked")
                : Component.translatableWithFallback("container.sgcraft.ring_platform.not_linked", "Not linked"),
                RingPlatformScreen.TEXT_X, y, this.menu.linked() ? 0xFF207020 : 0xFF902020, false);
        y += RingPlatformScreen.LINE;
        graphics.text(this.font, Component.translatableWithFallback("container.sgcraft.ring_platform.stored",
                "%s/%s GE", numbers.format(this.menu.stored()), numbers.format(this.menu.capacity())),
                RingPlatformScreen.TEXT_X, y, 0xFF404040, false);
        y += RingPlatformScreen.LINE;
        graphics.text(this.font, Component.translatableWithFallback("container.sgcraft.ring_platform.uses",
                "Transports left: %s", numbers.format(this.menu.usesLeft())), RingPlatformScreen.TEXT_X, y,
                0xFF404040, false);
    }
}
