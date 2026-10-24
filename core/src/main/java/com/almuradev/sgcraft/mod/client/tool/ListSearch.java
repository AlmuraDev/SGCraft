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

package com.almuradev.sgcraft.mod.client.tool;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * The search box over the PDD's and explorer dialer's lists, as in SGCraft 2.2.x, with a red x after it that empties
 * it. What is typed narrows the list to the entries whose name, world or address has it in them, ignoring case, and
 * dashes in addresses. What was typed is kept while the screen is resized.
 */
final class ListSearch {

    /** How tall the box and its button are. */
    static final int HEIGHT = 18;

    private static final int CLEAR_WIDTH = 20;
    private static final int GAP = 2;
    private static final Component LABEL = Component.translatableWithFallback("screen.sgcraft.search", "Search");

    private String query = "";
    private @Nullable EditBox box;
    private @Nullable Button clear;

    /**
     * Makes the box, filled with what was typed before, for a row that starts at x and is width wide; its clear button
     * takes the end of the row.
     *
     * @param changed run each time what is typed changes
     */
    EditBox box(final Font font, final int x, final int y, final int width, final Runnable changed) {
        final EditBox box = new EditBox(font, x, y, width - ListSearch.CLEAR_WIDTH - ListSearch.GAP,
                ListSearch.HEIGHT, ListSearch.LABEL);
        box.setMaxLength(48);
        box.setHint(ListSearch.LABEL);
        box.setValue(this.query);
        box.setResponder(text -> {
            this.query = text;
            this.updateClear();
            changed.run();
        });
        this.box = box;
        return box;
    }

    /** Makes the button that empties the box, at the end of the same row as the box. */
    Button clear(final int x, final int y, final int width) {
        this.clear = Button.builder(Component.literal("x").withStyle(ChatFormatting.RED), button -> {
            if (this.box != null) {
                this.box.setValue("");
            }
        }).bounds(x + width - ListSearch.CLEAR_WIDTH, y, ListSearch.CLEAR_WIDTH, ListSearch.HEIGHT).build();
        this.clear.setTooltip(Tooltip.create(Component.translatableWithFallback("screen.sgcraft.search.clear",
                "Clear the search")));
        this.updateClear();
        return this.clear;
    }

    private void updateClear() {
        if (this.clear != null) {
            this.clear.active = this.searching();
        }
    }

    /** @return whether anything is typed, so that the list may not show every entry */
    boolean searching() {
        return !this.query.isBlank();
    }

    /**
     * @param world the world's identifier, or empty if it isn't known
     * @return whether an entry is shown with what is typed
     */
    boolean matches(final String name, final String world, final String address) {
        final String query = this.query.strip().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return true;
        }
        final String symbols = query.replace("-", "");
        return name.toLowerCase(Locale.ROOT).contains(query)
                || !world.isEmpty() && (world.toLowerCase(Locale.ROOT).contains(query)
                        || ExplorerScreen.worldName(world).getString().toLowerCase(Locale.ROOT).contains(query))
                || !symbols.isEmpty() && address.replace("-", "").toLowerCase(Locale.ROOT).contains(symbols);
    }
}
