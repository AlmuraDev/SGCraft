/*
 * This file is part of SGCraft, licensed under the MIT License (MIT).
 * It is based on the original SGCraft by gcewing.
 *
 * Copyright (c) AlmuraDev <https://github.com/AlmuraDev>
 * Copyright (c) 2016 gcewing <https://github.com/gcewing>
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

package com.almuradev.sgcraft.mod.client.gate;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.gate.GateBaseMenu;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.GateInfo;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * A gate's base, for a player who may configure the gate: its address, and the camouflage drawn over its bottom row,
 * one block to a slot from left to right as seen from in front. Laid out and drawn as the original's.
 */
public final class GateBaseScreen extends AbstractContainerScreen<GateBaseMenu> {

    private static final Identifier TEXTURE = SGCraft.id("textures/gui/gate/base.png");
    private static final int HEIGHT = 208;
    // A ZPM slot's edge and inside, drawn as vanilla's slots are
    private static final int SLOT_EDGE = 0xFF373737;
    private static final int SLOT_INSIDE = 0xFF8B8B8B;

    private final GateAddressPanel panel = new GateAddressPanel();

    public GateBaseScreen(final GateBaseMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, GateAddressPanel.WIDTH, GateBaseScreen.HEIGHT);
    }

    /** Shows the gate the server says this menu is for. */
    public void update(final GateInfo gate) {
        this.panel.set(gate);
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
            final float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, GateBaseScreen.TEXTURE, this.leftPos, this.topPos, 0, 0,
                this.imageWidth, this.imageHeight, 256, 256);
        this.panel.draw(graphics, this.font, this.leftPos, this.topPos, this.title);
        // A supergate's ZPM slots, which the original's art has no room drawn for
        if (this.menu.takesZpms()) {
            for (int slot = 0; slot < GateBaseMenu.ZPM_SLOTS; slot++) {
                final int x = this.leftPos + GateBaseMenu.ZPM_X + slot * 18 - 1;
                final int y = this.topPos + GateBaseMenu.SLOTS_Y - 1;
                graphics.fill(x, y, x + 18, y + 18, GateBaseScreen.SLOT_EDGE);
                graphics.fill(x + 1, y + 1, x + 17, y + 17, GateBaseScreen.SLOT_INSIDE);
            }
        }
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        // Over the camouflage slots, as the original labels them; the inventory has no label
        if (this.menu.standing()) {
            GateAddressPanel.centered(graphics, this.font, Component.translatableWithFallback(
                    "screen.sgcraft.gate_base.camouflage", "Base Camouflage"),
                    GateBaseMenu.SLOTS_X + GateBaseMenu.SLOTS * 9, GateBaseMenu.SLOTS_Y - 12);
        }
        if (!this.menu.takesZpms()) {
            return;
        }
        GateAddressPanel.centered(graphics, this.font, Component.translatableWithFallback(
                "screen.sgcraft.gate_base.zpms", "ZPMs"),
                GateBaseMenu.ZPM_X + GateBaseMenu.ZPM_SLOTS * 9, GateBaseMenu.SLOTS_Y - 12);
        // How the supergate's ZPMs stand, below the screen, as the original's panel
        graphics.text(this.font, Component.translatableWithFallback("screen.sgcraft.gate_base.zpm_count",
                "ZPMs: %s", this.menu.zpmCount()), 0, this.imageHeight + 4, 0xFFFFFFFF, true);
        graphics.text(this.font, Component.translatableWithFallback("screen.sgcraft.gate_base.zpm_energy",
                "ZPM energy: %s GE", NumberFormat.getIntegerInstance(Locale.US).format(this.menu.zpmEnergy())), 0,
                this.imageHeight + 16, 0xFFFFFFFF, true);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        return this.panel.click(event.x(), event.y(), this.leftPos, this.topPos)
                || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
