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

import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.address.Symbol;
import com.almuradev.sgcraft.mod.gate.DialStyle;
import com.almuradev.sgcraft.mod.gate.GlyphText;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Picks one of the 39 symbols, each shown as its letter and its glyph, or one of a few choices that aren't a symbol,
 * such as none. The symbol already chosen can't be picked again, and neither can those {@code taken} names.
 */
final class SymbolPickerScreen extends Screen {

    private static final int COLUMNS = 10;
    private static final int SIZE = 30;

    /** A choice that isn't a symbol. */
    enum Extra {
        /** No symbol, as for addresses without a point of origin. */
        NONE("", "screen.sgcraft.settings.pick.none", "None"),
        /** The server's value, for a world that has its own. */
        SERVER("*server", "screen.sgcraft.settings.pick.server", "Server's"),
        /** A symbol SGCraft chooses, for a group. */
        AUTOMATIC("*auto", "screen.sgcraft.settings.pick.automatic", "SGCraft chooses");

        private final String value;
        private final String key;
        private final String fallback;

        Extra(final String value, final String key, final String fallback) {
            this.value = value;
            this.key = key;
            this.fallback = fallback;
        }

        /** @return the value the picker gives back for this choice */
        String value() {
            return this.value;
        }
    }

    private final Screen parent;
    private final String current;
    private final List<Extra> extras;
    private final Set<String> taken;
    private final Consumer<String> chosen;

    /**
     * @param current the value chosen now: a symbol, or an extra's value
     * @param taken the symbols that can't be picked, as one character each
     * @param chosen given the value picked: a symbol as one character, or an extra's value
     */
    SymbolPickerScreen(final Screen parent, final Component title, final String current, final List<Extra> extras,
            final Set<String> taken, final Consumer<String> chosen) {
        super(title);
        this.parent = parent;
        this.current = current;
        this.extras = extras;
        this.taken = taken;
        this.chosen = chosen;
    }

    @Override
    protected void init() {
        final int left = (this.width - SymbolPickerScreen.COLUMNS * SymbolPickerScreen.SIZE) / 2;
        final int top = 40;
        for (int index = 0; index < Symbol.COUNT; index++) {
            final Symbol symbol = new Symbol(index);
            final String text = String.valueOf(symbol.character());
            final Component label = Component.literal(text + " ")
                    .append(GlyphText.of(new Address(List.of(symbol)), DialStyle.MILKY_WAY));
            final Button button = this.addRenderableWidget(Button.builder(label, b -> this.pick(text))
                    .bounds(left + index % SymbolPickerScreen.COLUMNS * SymbolPickerScreen.SIZE,
                            top + index / SymbolPickerScreen.COLUMNS * 22, SymbolPickerScreen.SIZE - 2, 20)
                    .build());
            if (text.equals(this.current)) {
                button.active = false;
                button.setTooltip(Tooltip.create(Component.translatableWithFallback(
                        "screen.sgcraft.settings.pick.current", "Chosen now")));
            } else if (this.taken.contains(text)) {
                button.active = false;
                button.setTooltip(Tooltip.create(Component.translatableWithFallback(
                        "screen.sgcraft.settings.pick.taken", "Another group has this symbol")));
            }
        }
        final int rows = (Symbol.COUNT + SymbolPickerScreen.COLUMNS - 1) / SymbolPickerScreen.COLUMNS;
        final int extraTop = top + rows * 22 + 8;
        final int width = SymbolPickerScreen.COLUMNS * SymbolPickerScreen.SIZE;
        final int each = this.extras.isEmpty() ? 0 : (width + 4) / this.extras.size() - 4;
        for (int index = 0; index < this.extras.size(); index++) {
            final Extra extra = this.extras.get(index);
            final Button button = this.addRenderableWidget(Button.builder(Component.translatableWithFallback(
                    extra.key, extra.fallback), b -> this.pick(extra.value))
                    .bounds(left + index * (each + 4), extraTop, each, 20).build());
            button.active = !extra.value.equals(this.current);
        }
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> this.onClose())
                .bounds(this.width / 2 - 100, extraTop + 28, 200, 20).build());
    }

    private void pick(final String value) {
        this.minecraft.gui.setScreen(this.parent);
        this.chosen.accept(value);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
            final float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
    }
}
