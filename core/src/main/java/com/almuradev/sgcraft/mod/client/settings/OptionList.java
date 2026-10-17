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

import com.almuradev.sgcraft.mod.settings.SettingsCatalog.Type;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * A page of settings under their headings, each a name with a switch or a number box beside it. A number is sent
 * when Enter is pressed or the box loses focus, but not when the screen is laid out again, as it is when another
 * operator changes a setting; a number not sent yet is kept as a draft and shown again. A setting whose value after
 * a restart differs from its value now has its name in yellow, and one changed but not saved yet in aqua.
 */
final class OptionList extends ContainerObjectSelectionList<OptionList.Entry> {

    private static final int ROW_WIDTH = 310;
    private static final int WIDGET_WIDTH = 120;

    /**
     * One setting, as the list shows it.
     *
     * @param key what names it, for its draft
     * @param section the heading it is under
     * @param label its name
     * @param tip what it does
     * @param type the kind of value it holds
     * @param live whether a change applies at once, rather than when the server next starts
     * @param now its value now
     * @param next its value after a restart
     * @param defaultValue its value when it isn't set
     * @param unsaved whether it has been changed on this screen but not saved yet
     * @param change given a new value, written as text
     */
    record Item(String key, Component section, Component label, Component tip, Type type, boolean live, String now,
            String next, String defaultValue, boolean unsaved, Consumer<String> change) {
    }

    private final Font font;
    private final Map<String, String> drafts;
    private final BooleanSupplier rebuilding;

    /**
     * @param drafts the numbers typed but not sent yet, by setting, kept by the screen while it is laid out again
     * @param rebuilding whether the screen is being laid out again, when a box losing focus sends nothing
     */
    OptionList(final Minecraft minecraft, final int width, final int height, final int y,
            final Map<String, String> drafts, final BooleanSupplier rebuilding) {
        super(minecraft, width, height, y, 24);
        this.font = minecraft.font;
        this.drafts = drafts;
        this.rebuilding = rebuilding;
    }

    /** Adds settings, with a heading before each one under a heading the one before it isn't. */
    void addAll(final List<Item> items) {
        String section = null;
        for (final Item item : items) {
            final String heading = item.section().getString();
            if (!heading.equals(section)) {
                section = heading;
                this.addEntry(new Heading(item.section()), 20);
            }
            this.addEntry(new Setting(item));
        }
    }

    @Override
    public int getRowWidth() {
        return OptionList.ROW_WIDTH;
    }

    abstract static class Entry extends ContainerObjectSelectionList.Entry<Entry> {
    }

    // A heading, in gold, with nothing to focus
    private final class Heading extends Entry {

        private final Component text;

        Heading(final Component text) {
            this.text = text;
        }

        @Override
        public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
                final boolean hovered, final float a) {
            graphics.text(OptionList.this.font, this.text, this.getContentX(), this.getContentY() + 8, 0xFFFFC040);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of();
        }
    }

    // A setting: its name on the left, and a switch or a number box on the right
    private final class Setting extends Entry {

        private final Item item;
        private final AbstractWidget widget;
        private final Component name;

        Setting(final Item item) {
            this.item = item;
            final MutableComponent name = Component.empty().append(item.label());
            if (!item.live()) {
                name.append(" *");
            }
            this.name = name;
            if (item.type() == Type.BOOL) {
                this.widget = CycleButton.onOffBuilder(Boolean.parseBoolean(item.next())).displayOnlyValue()
                        .create(0, 0, OptionList.WIDGET_WIDTH, 20, item.label(),
                                (button, value) -> item.change().accept(String.valueOf(value)));
            } else {
                this.widget = new NumberBox(item);
            }
            this.widget.setTooltip(Tooltip.create(OptionList.tooltip(item)));
        }

        @Override
        public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
                final boolean hovered, final float a) {
            final boolean pending = !this.item.now().equals(this.item.next());
            graphics.text(OptionList.this.font, this.name, this.getContentX(), this.getContentY() + 6,
                    this.item.unsaved() ? 0xFF60FFFF : pending ? 0xFFFFFF60 : 0xFFFFFFFF);
            this.widget.setPosition(this.getContentRight() - OptionList.WIDGET_WIDTH, this.getContentY());
            this.widget.extractRenderState(graphics, mouseX, mouseY, a);
            if (hovered && mouseX < this.widget.getX()) {
                graphics.setTooltipForNextFrame(OptionList.this.font.split(OptionList.tooltip(this.item), 200),
                        mouseX, mouseY);
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.widget);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.widget);
        }
    }

    // What a setting does, its default, and when a change takes effect
    private static Component tooltip(final Item item) {
        final MutableComponent tip = Component.empty().append(item.tip()).append("\n\n")
                .append(Component.translatableWithFallback("screen.sgcraft.settings.option.default", "Default: %s",
                        item.defaultValue()));
        if (!item.live()) {
            tip.append("\n").append(Component.translatableWithFallback("screen.sgcraft.settings.option.restart",
                    "Takes effect when the server next starts"));
        }
        if (item.unsaved()) {
            tip.append("\n").append(Component.translatableWithFallback("screen.sgcraft.settings.option.unsaved",
                    "Changed, but not saved yet"));
        }
        if (!item.now().equals(item.next())) {
            tip.append("\n").append(Component.translatableWithFallback("screen.sgcraft.settings.option.pending",
                    "Now %s. After a restart, %s", item.now(), item.next()));
        }
        return tip;
    }

    // A number, sent when Enter is pressed or the box loses focus, if it changed
    private final class NumberBox extends EditBox {

        private final Item item;
        private String sent;

        NumberBox(final Item item) {
            super(OptionList.this.font, 0, 0, OptionList.WIDGET_WIDTH, 20, item.label());
            this.item = item;
            this.sent = item.next();
            this.setMaxLength(24);
            final String draft = OptionList.this.drafts.get(item.key());
            this.setValue(draft == null ? item.next() : draft);
            this.color(this.getValue());
            this.setResponder(text -> {
                this.color(text);
                if (text.trim().equals(item.next())) {
                    OptionList.this.drafts.remove(item.key());
                } else {
                    OptionList.this.drafts.put(item.key(), text);
                }
            });
        }

        // Text that isn't a number is shown in red; the server says why when it is sent
        private void color(final String text) {
            this.setTextColor(NumberBox.number(this.item.type(), text) ? 0xFFE0E0E0 : 0xFFFF6060);
        }

        private static boolean number(final Type type, final String text) {
            try {
                if (type == Type.WHOLE) {
                    Long.parseLong(text.trim());
                    return true;
                }
                return Double.isFinite(Double.parseDouble(text.trim()));
            } catch (final NumberFormatException e) {
                return false;
            }
        }

        private void send() {
            final String value = this.getValue().trim();
            if (!value.isEmpty() && !value.equals(this.sent)) {
                this.sent = value;
                OptionList.this.drafts.remove(this.item.key());
                this.item.change().accept(value);
            }
        }

        @Override
        public boolean keyPressed(final KeyEvent event) {
            if (this.isFocused() && event.isConfirmation()) {
                this.send();
                return true;
            }
            return super.keyPressed(event);
        }

        @Override
        public void setFocused(final boolean focused) {
            if (this.isFocused() && !focused && !OptionList.this.rebuilding.getAsBoolean()) {
                this.send();
            }
            super.setFocused(focused);
        }
    }
}
