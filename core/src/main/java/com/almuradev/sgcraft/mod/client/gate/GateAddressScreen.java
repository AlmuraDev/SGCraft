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
import com.almuradev.sgcraft.mod.tool.ToolPayloads.GateInfo;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * A gate's base, for a player who may only look: its address, as the original shows it to a player who may not change
 * the gate.
 */
public final class GateAddressScreen extends Screen {

    private static final Identifier TEXTURE = SGCraft.id("textures/gui/gate/address.png");
    private static final int HEIGHT = 92;
    // How far the player may walk from the gate before the screen closes
    private static final double REACH = 8;

    private final GateAddressPanel panel = new GateAddressPanel();
    private final BlockPos pos;
    private int left;
    private int top;

    public GateAddressScreen(final GateInfo gate) {
        super(Component.translatableWithFallback("screen.sgcraft.gate_base", "Stargate Address"));
        this.panel.set(gate);
        this.pos = gate.pos();
    }

    @Override
    protected void init() {
        this.left = (this.width - GateAddressPanel.WIDTH) / 2;
        this.top = (this.height - GateAddressScreen.HEIGHT) / 2;
    }

    @Override
    public void tick() {
        if (this.minecraft == null || this.minecraft.player == null
                || !this.pos.closerToCenterThan(this.minecraft.player.position(), GateAddressScreen.REACH)) {
            this.onClose();
        }
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
            final float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, GateAddressScreen.TEXTURE, this.left, this.top, 0, 0,
                GateAddressPanel.WIDTH, GateAddressScreen.HEIGHT, 256, 128);
        this.panel.draw(graphics, this.font, this.left, this.top, this.title);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        return this.panel.click(event.x(), event.y(), this.left, this.top) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
