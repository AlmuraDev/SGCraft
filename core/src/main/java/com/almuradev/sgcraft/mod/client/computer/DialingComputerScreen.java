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

package com.almuradev.sgcraft.mod.client.computer;

import com.almuradev.sgcraft.mod.block.entity.DialingComputerBlockEntity;
import com.almuradev.sgcraft.mod.client.ClientPlatform;
import com.almuradev.sgcraft.mod.computer.ComputerPayloads;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * The screen of a player seated at a dialing computer: the dialing program as the computer's monitor shows it, large,
 * and under it the address to dial, whether to dial it slowly, the ring turning to each symbol and each chevron
 * locking in turn as on the show, or quickly, every chevron locking at once, and buttons to dial, shut the gate down
 * and work the iris. Closing the screen gets the player up from the chair.
 */
public final class DialingComputerScreen extends Screen {

    private static final int GAP = 4;
    private static final int ROW = 20;

    private final BlockPos computer;
    private @Nullable EditBox address;
    // Whether the next dial locks every chevron at once
    private boolean fast;
    // Where the program is drawn, and how large
    private int displayX;
    private int displayY;
    private int displayWidth;
    private int displayHeight;

    public DialingComputerScreen(final ComputerPayloads.Screen open) {
        super(Component.translatableWithFallback("screen.sgcraft.dialing_computer", "Dialing Computer"));
        this.computer = open.computer();
    }

    /** @return the computer the screen works */
    public BlockPos computer() {
        return this.computer;
    }

    @Override
    protected void init() {
        final float scale = Math.min((this.width - 20) / (float) ComputerDisplay.WIDTH,
                (this.height - 20 - DialingComputerScreen.ROW - 10) / (float) ComputerDisplay.HEIGHT);
        this.displayWidth = (int) (ComputerDisplay.WIDTH * scale);
        this.displayHeight = (int) (ComputerDisplay.HEIGHT * scale);
        this.displayX = (this.width - this.displayWidth) / 2;
        this.displayY = 10;
        final int[] widths = {120, 70, 50, 50, 50, 60};
        int total = (widths.length - 1) * DialingComputerScreen.GAP;
        for (final int width : widths) {
            total += width;
        }
        int x = (this.width - total) / 2;
        final int y = this.displayY + this.displayHeight + 8;
        final String typed = this.address == null ? "" : this.address.getValue();
        this.address = this.addRenderableWidget(new EditBox(this.font, x, y, widths[0], DialingComputerScreen.ROW,
                Component.translatableWithFallback("screen.sgcraft.dialing_computer.address", "Address")));
        this.address.setMaxLength(9);
        this.address.setHint(Component.translatableWithFallback("screen.sgcraft.dialing_computer.address_hint",
                "Address"));
        this.address.setValue(typed);
        x += widths[0] + DialingComputerScreen.GAP;
        this.addRenderableWidget(Button.builder(this.modeLabel(), button -> {
            this.fast = !this.fast;
            button.setMessage(this.modeLabel());
        }).bounds(x, y, widths[1], DialingComputerScreen.ROW).build());
        x += widths[1] + DialingComputerScreen.GAP;
        this.addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "screen.sgcraft.dialing_computer.dial", "Dial"), button -> this.dial())
                .bounds(x, y, widths[2], DialingComputerScreen.ROW).build());
        x += widths[2] + DialingComputerScreen.GAP;
        this.addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "screen.sgcraft.dialing_computer.abort", "Abort"),
                button -> this.send(ComputerPayloads.Action.Kind.ABORT, ""))
                .bounds(x, y, widths[3], DialingComputerScreen.ROW).build());
        x += widths[3] + DialingComputerScreen.GAP;
        this.addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "screen.sgcraft.dialing_computer.iris", "Iris"),
                button -> this.send(ComputerPayloads.Action.Kind.IRIS, ""))
                .bounds(x, y, widths[4], DialingComputerScreen.ROW).build());
        x += widths[4] + DialingComputerScreen.GAP;
        this.addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "screen.sgcraft.dialing_computer.stand_up", "Stand up"), button -> this.onClose())
                .bounds(x, y, widths[5], DialingComputerScreen.ROW).build());
        this.setInitialFocus(this.address);
    }

    private Component modeLabel() {
        return this.fast ? Component.translatableWithFallback("screen.sgcraft.dialing_computer.fast", "Fast dial")
                : Component.translatableWithFallback("screen.sgcraft.dialing_computer.slow", "Slow dial");
    }

    private void dial() {
        if (this.address == null || this.address.getValue().isBlank()) {
            return;
        }
        this.send(this.fast ? ComputerPayloads.Action.Kind.DIAL_FAST : ComputerPayloads.Action.Kind.DIAL_SLOW,
                this.address.getValue().trim().toUpperCase(Locale.ROOT));
    }

    private void send(final ComputerPayloads.Action.Kind kind, final String address) {
        ClientPlatform.get().sendToServer(new ComputerPayloads.Action(this.computer, kind, address));
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
            this.dial();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void tick() {
        // The computer was broken under the player
        if (this.minecraft.level == null
                || !(this.minecraft.level.getBlockEntity(this.computer) instanceof DialingComputerBlockEntity)) {
            this.onClose();
        }
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
            final float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (this.minecraft.level != null && this.minecraft.level.getBlockEntity(this.computer)
                instanceof final DialingComputerBlockEntity entity) {
            final Identifier picture = ComputerDisplays.texture(entity, partialTick);
            graphics.blit(RenderPipelines.GUI_TEXTURED, picture, this.displayX, this.displayY, 0, 0,
                    this.displayWidth, this.displayHeight, ComputerDisplay.WIDTH, ComputerDisplay.HEIGHT,
                    ComputerDisplay.WIDTH, ComputerDisplay.HEIGHT);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Closing the screen, however it is closed, gets the player up
    @Override
    public void removed() {
        this.send(ComputerPayloads.Action.Kind.CLOSE, "");
    }
}
