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

package com.almuradev.sgcraft.mod.client.settings;

import com.almuradev.sgcraft.mod.address.AddressLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.util.OptionalInt;
import java.util.function.Consumer;

/**
 * Asks how many chunks across a world's cells are, from 1 to {@link AddressLayout#MAX_CELL_CHUNKS}, or, for one
 * world, whether it uses the server's cell size again.
 */
final class CellSizeScreen extends Screen {

    private static final int WIDTH = 200;

    private final Screen parent;
    private final int current;
    private final boolean serverChoice;
    private final Consumer<OptionalInt> chosen;
    private String text;
    private @Nullable Button done;

    /**
     * @param current how many chunks across the cells are now
     * @param serverChoice whether to offer the server's cell size, for one world
     * @param chosen given the cell size chosen, or nothing for the server's
     */
    CellSizeScreen(final Screen parent, final Component title, final int current, final boolean serverChoice,
            final Consumer<OptionalInt> chosen) {
        super(title);
        this.parent = parent;
        this.current = current;
        this.serverChoice = serverChoice;
        this.chosen = chosen;
        this.text = String.valueOf(current);
    }

    @Override
    protected void init() {
        final int left = (this.width - CellSizeScreen.WIDTH) / 2;
        final int top = this.height / 2 - 30;
        final EditBox box = this.addRenderableWidget(new EditBox(this.font, left, top, CellSizeScreen.WIDTH, 20,
                this.title));
        box.setMaxLength(3);
        box.setValue(this.text);
        box.setResponder(value -> {
            this.text = value;
            this.check();
        });
        this.setInitialFocus(box);
        final int each = this.serverChoice ? (CellSizeScreen.WIDTH - 8) / 3 : (CellSizeScreen.WIDTH - 4) / 2;
        int x = left;
        this.done = this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.finish())
                .bounds(x, top + 28, each, 20).build());
        x += each + 4;
        if (this.serverChoice) {
            this.addRenderableWidget(Button.builder(Component.translatableWithFallback(
                    "screen.sgcraft.settings.pick.server", "Server's"), b -> this.pick(OptionalInt.empty()))
                    .bounds(x, top + 28, each, 20).build());
            x += each + 4;
        }
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> this.onClose())
                .bounds(x, top + 28, each, 20).build());
        this.check();
    }

    private OptionalInt chunks() {
        try {
            final int chunks = Integer.parseInt(this.text);
            return chunks >= 1 && chunks <= AddressLayout.MAX_CELL_CHUNKS ? OptionalInt.of(chunks)
                    : OptionalInt.empty();
        } catch (final NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    private void check() {
        if (this.done != null) {
            this.done.active = this.chunks().isPresent();
        }
    }

    private void finish() {
        final OptionalInt chunks = this.chunks();
        if (chunks.isEmpty()) {
            return;
        }
        if (chunks.getAsInt() == this.current) {
            this.onClose();
            return;
        }
        this.pick(chunks);
    }

    private void pick(final OptionalInt chunks) {
        this.minecraft.gui.setScreen(this.parent);
        this.chosen.accept(chunks);
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        if (event.isConfirmation()) {
            this.finish();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
            final float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        final int center = this.width / 2;
        final int top = this.height / 2 - 30;
        graphics.centeredText(this.font, this.title, center, top - 34, 0xFFFFFFFF);
        final Component hint = Component.translatableWithFallback("screen.sgcraft.settings.cell.hint", "Chunks "
                + "across each cell, from 1 to %s. A cell of 8 chunks is 128 blocks", AddressLayout.MAX_CELL_CHUNKS);
        int y = top - 20;
        for (final FormattedCharSequence line : this.font.split(hint, CellSizeScreen.WIDTH + 100)) {
            graphics.centeredText(this.font, line, center, y, 0xFFA0A0A0);
            y += 10;
        }
    }
}
