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

package com.almuradev.sgcraft.mod.client.malp;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.malp.MalpMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * A MALP up close: its name, its battery slot with the battery's charge, or that it needs no power, and the console it
 * is paired with, over the player's inventory. For a MALP tablet: its name, its battery slot with the battery's
 * charge, and how many MALPs it is paired with.
 */
public final class MalpScreen extends AbstractContainerScreen<MalpMenu> {

    private static final Identifier BACKGROUND = SGCraft.id("textures/gui/malp.png");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 166;
    private static final int TEXT_COLOR = 0xFF404040;

    public MalpScreen(final MalpMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, MalpScreen.WIDTH, MalpScreen.HEIGHT);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
            final float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, MalpScreen.BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F,
                MalpScreen.WIDTH, MalpScreen.HEIGHT, 256, 256);
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int xm, final int ym) {
        super.extractLabels(graphics, xm, ym);
        final int battery = this.menu.data().get(MalpMenu.BATTERY);
        final Component charge = this.menu.data().get(MalpMenu.POWER_REQUIRED) == 0
                ? Component.translatableWithFallback("container.sgcraft.malp.no_power_needed", "Needs no power")
                : battery < 0
                ? Component.translatableWithFallback("container.sgcraft.malp.no_battery", "No battery")
                : Component.translatableWithFallback("container.sgcraft.malp.battery", "Battery %s%%", battery);
        graphics.text(this.font, charge, 104, 39, MalpScreen.TEXT_COLOR, false);
        final int tablet = this.menu.data().get(MalpMenu.TABLET);
        if (tablet > 0) {
            graphics.text(this.font, Component.translatableWithFallback("container.sgcraft.malp_tablet.malps",
                    "Paired MALPs: %s", tablet - 1), 8, 60, MalpScreen.TEXT_COLOR, false);
            return;
        }
        final int console = this.menu.data().get(MalpMenu.CONSOLE);
        final Component paired = switch (console) {
            case 0 -> Component.translatableWithFallback("container.sgcraft.malp.unpaired", "Not paired");
            case 1 -> Component.translatableWithFallback("container.sgcraft.malp.console", "Console at %s",
                    this.menu.console().toShortString());
            default -> Component.translatableWithFallback("container.sgcraft.malp.console_elsewhere",
                    "Console in another world");
        };
        graphics.text(this.font, paired, 8, 60, MalpScreen.TEXT_COLOR, false);
    }
}
