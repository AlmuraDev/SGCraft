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

package com.almuradev.sgcraft.mod.client.dhd;

import com.almuradev.sgcraft.mod.dhd.DhdFuelMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

import java.util.Locale;

/**
 * A DHD's fuel, laid out as a hopper is: five slots of naquadah over the player's inventory, with how full the gate's
 * store is beside the title.
 */
public final class DhdFuelScreen extends AbstractContainerScreen<DhdFuelMenu> {

    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/hopper.png");

    public DhdFuelScreen(final DhdFuelMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, 176, 133);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
            final float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, DhdFuelScreen.BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);
    }

    // Light, with a shadow, so it reads on a resource pack's dark container as on the vanilla light one
    private static final int TEXT = 0xFFF0F0F0;

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int xm, final int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, DhdFuelScreen.TEXT, true);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                DhdFuelScreen.TEXT, true);
        // Beside the title, the gate's own store; beside the inventory's, what the DHD's naquadah holds
        final ContainerData data = this.menu.data();
        final Component gate = data.get(DhdFuelMenu.GATE_PERCENT) < 0
                ? Component.translatableWithFallback("container.sgcraft.dhd_fuel.no_gate", "No gate")
                : Component.translatableWithFallback("container.sgcraft.dhd_fuel.gate_energy", "Gate: %s / %s GE",
                        DhdFuelScreen.amount(DhdFuelMenu.wide(data, DhdFuelMenu.GATE_ENERGY)),
                        DhdFuelScreen.amount(DhdFuelMenu.wide(data, DhdFuelMenu.GATE_CAPACITY)));
        graphics.text(this.font, gate, this.imageWidth - 8 - this.font.width(gate), this.titleLabelY,
                DhdFuelScreen.TEXT, true);
        final Component fuel = Component.translatableWithFallback("container.sgcraft.dhd_fuel.fuel_energy",
                "Fuel: %s GE", DhdFuelScreen.amount(DhdFuelMenu.wide(data, DhdFuelMenu.FUEL_ENERGY)));
        graphics.text(this.font, fuel, this.imageWidth - 8 - this.font.width(fuel), this.inventoryLabelY,
                DhdFuelScreen.TEXT, true);
    }

    private static String amount(final int value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
