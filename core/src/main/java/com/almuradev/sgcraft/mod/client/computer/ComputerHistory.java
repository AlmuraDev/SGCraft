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
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * The dialing history beside a dialing computer's screen, painted in the program's own pixels: the addresses last
 * dialed from the computer, newest first, each written out and in its gate's glyphs, with whether it connected. The
 * one chosen to dial again is marked.
 */
final class ComputerHistory {

    /** How many pixels across and down the history is painted. */
    static final int WIDTH = 140;
    static final int HEIGHT = ComputerDisplay.HEIGHT + 4 + ComputerKeyboard.HEIGHT;

    // Where the rows start, how tall each is and the space between them, and how large their glyphs are
    private static final int ROWS_Y = 20;
    private static final int ROW = 32;
    private static final int GAP = 2;
    private static final int GLYPH = 12;
    private static final int GLYPH_SPACING = 13;

    private ComputerHistory() {
    }

    /** @return the row at a place on the history, in its pixels, or -1 if there is none */
    static int rowAt(final double x, final double y, final int rows) {
        if (x < 4 || x >= ComputerHistory.WIDTH - 4 || y < ComputerHistory.ROWS_Y) {
            return -1;
        }
        final int row = (int) ((y - ComputerHistory.ROWS_Y) / (ComputerHistory.ROW + ComputerHistory.GAP));
        final double within = (y - ComputerHistory.ROWS_Y) % (ComputerHistory.ROW + ComputerHistory.GAP);
        return row < rows && within < ComputerHistory.ROW ? row : -1;
    }

    /**
     * Paints the history.
     *
     * @param glyphs the glyph font of the computer's gate
     * @param selected the row chosen to dial again, or -1
     * @param hovered the row under the pointer, or -1
     */
    static void paint(final ComputerCanvas canvas, final Identifier glyphs,
            final List<DialingComputerBlockEntity.Dialed> history, final int selected, final int hovered) {
        canvas.clear(ComputerDisplay.BACKGROUND);
        for (int x = 0; x < ComputerHistory.WIDTH; x += 10) {
            canvas.fill(x, 0, 1, ComputerHistory.HEIGHT, ComputerDisplay.GRID);
        }
        for (int y = 0; y < ComputerHistory.HEIGHT; y += 10) {
            canvas.fill(0, y, ComputerHistory.WIDTH, 1, ComputerDisplay.GRID);
        }
        canvas.rect(0, 0, ComputerHistory.WIDTH, ComputerHistory.HEIGHT, ComputerDisplay.TEAL);
        canvas.text("DIAL HISTORY", 8, 7, 1, ComputerDisplay.CYAN);
        if (history.isEmpty()) {
            canvas.centeredText("NO DIALS YET", ComputerHistory.WIDTH / 2, ComputerHistory.ROWS_Y + 10, 1,
                    ComputerDisplay.CYAN_DIM);
        }
        for (int i = 0; i < history.size(); i++) {
            final DialingComputerBlockEntity.Dialed dialed = history.get(i);
            final int x = 4;
            final int y = ComputerHistory.ROWS_Y + i * (ComputerHistory.ROW + ComputerHistory.GAP);
            final int width = ComputerHistory.WIDTH - 8;
            canvas.fill(x, y, width, ComputerHistory.ROW, ComputerDisplay.BACKGROUND);
            if (i == selected || i == hovered) {
                canvas.fill(x, y, width, ComputerHistory.ROW, ComputerDisplay.CYAN_FAINT);
            }
            canvas.rect(x, y, width, ComputerHistory.ROW, i == selected ? ComputerDisplay.WHITE
                    : ComputerDisplay.CYAN_DIM);
            canvas.text(dialed.address(), x + 4, y + 4, 1, ComputerDisplay.CYAN);
            final String result = dialed.connected() ? "OK" : "FAIL";
            canvas.text(result, x + width - 4 - canvas.textWidth(result, 1), y + 4, 1,
                    dialed.connected() ? ComputerDisplay.GREEN : ComputerDisplay.RED);
            int glyph = 0;
            for (final char character : dialed.address().toCharArray()) {
                if (character == '-') {
                    continue;
                }
                canvas.glyph(glyphs, character, x + 4 + ComputerHistory.GLYPH / 2.0
                        + glyph * ComputerHistory.GLYPH_SPACING, y + 22, ComputerHistory.GLYPH,
                        ComputerDisplay.WHITE);
                glyph++;
            }
        }
        ComputerDisplay.scanLines(canvas);
    }
}
