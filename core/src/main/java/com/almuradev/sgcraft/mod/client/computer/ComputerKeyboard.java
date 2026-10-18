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

import com.almuradev.sgcraft.api.address.Symbol;
import com.almuradev.sgcraft.api.gate.IrisState;
import com.almuradev.sgcraft.api.gate.IrisTypes;
import com.almuradev.sgcraft.mod.computer.ComputerDialMode;
import com.almuradev.sgcraft.mod.gate.IrisView;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The keyboard of a dialing computer's screen, painted in the program's own pixels under it: the address typed so
 * far, the keys that work the computer, and a key for each symbol, its gate's glyph over the letter or digit it is
 * typed with, laid out as a keyboard's are. A key lights for a moment when it is pressed, whether by a click or by
 * the key it stands for.
 */
final class ComputerKeyboard {

    /** How many pixels across and down the keyboard is painted. */
    static final int WIDTH = ComputerDisplay.WIDTH;
    static final int HEIGHT = 196;

    /** What a key does. */
    enum Kind {
        SYMBOL, DIAL, BACK, CLEAR, MODE, IRIS, ALARM, ABORT, LEAVE
    }

    /**
     * A key, where it is on the keyboard.
     *
     * @param symbol the symbol a symbol key types
     */
    record Key(Kind kind, @Nullable Symbol symbol, int x, int y, int width, int height) {

        boolean contains(final double px, final double py) {
            return px >= this.x && px < this.x + this.width && py >= this.y && py < this.y + this.height;
        }
    }

    // How long a pressed key stays lit, in nanoseconds
    private static final long LIT_NANOS = 160_000_000L;
    // The address boxes, and the keys: their size, the gap between them, and the top of each row
    private static final int BOX = 28;
    private static final int BOXES_Y = 4;
    private static final int KEY = 32;
    private static final int KEY_HEIGHT = 30;
    private static final int GAP = 4;
    private static final int CONTROLS_Y = 38;
    private static final int CONTROLS_HEIGHT = 18;
    private static final int ROWS_Y = 60;
    private static final int KEY_GLYPH = 18;
    private static final int BOX_GLYPH = 22;
    // The symbol rows, as a keyboard's, and how far in each starts
    private static final String[] ROWS = {"1234567890", "QWERTYUIOP", "ASDFGHJKL@", "ZXCVBNM+."};
    private static final int[] ROW_X = {10, 16, 24, 40};

    private final List<Key> keys = new ArrayList<>();
    private final long[] litUntil = new long[Kind.values().length + Symbol.COUNT];

    ComputerKeyboard() {
        final int controlWidth = (ComputerKeyboard.WIDTH - 20 - 4 * ComputerKeyboard.GAP) / 5;
        final Kind[] controls = {Kind.MODE, Kind.IRIS, Kind.ALARM, Kind.ABORT, Kind.LEAVE};
        for (int i = 0; i < controls.length; i++) {
            this.keys.add(new Key(controls[i], null, 10 + i * (controlWidth + ComputerKeyboard.GAP),
                    ComputerKeyboard.CONTROLS_Y, controlWidth, ComputerKeyboard.CONTROLS_HEIGHT));
        }
        for (int row = 0; row < ComputerKeyboard.ROWS.length; row++) {
            final int y = ComputerKeyboard.ROWS_Y + row * (ComputerKeyboard.KEY_HEIGHT + ComputerKeyboard.GAP);
            int x = ComputerKeyboard.ROW_X[row];
            for (final char character : ComputerKeyboard.ROWS[row].toCharArray()) {
                this.keys.add(new Key(Kind.SYMBOL, Symbol.of(character).orElseThrow(), x, y, ComputerKeyboard.KEY,
                        ComputerKeyboard.KEY_HEIGHT));
                x += ComputerKeyboard.KEY + ComputerKeyboard.GAP;
            }
            // Backspace ends the top row, enter stands down the right of the next two, and clear ends the last
            final int right = ComputerKeyboard.WIDTH - 10;
            if (row == 0) {
                this.keys.add(new Key(Kind.BACK, null, x, y, right - x, ComputerKeyboard.KEY_HEIGHT));
            } else if (row == 1) {
                final int left = ComputerKeyboard.ROW_X[2] + 10 * (ComputerKeyboard.KEY + ComputerKeyboard.GAP);
                this.keys.add(new Key(Kind.DIAL, null, left, y, right - left,
                        2 * ComputerKeyboard.KEY_HEIGHT + ComputerKeyboard.GAP));
            } else if (row == 3) {
                this.keys.add(new Key(Kind.CLEAR, null, x, y, right - x, ComputerKeyboard.KEY_HEIGHT));
            }
        }
    }

    /** @return the key at a place on the keyboard, in its pixels, if there is one */
    @Nullable Key keyAt(final double x, final double y) {
        for (final Key key : this.keys) {
            if (key.contains(x, y)) {
                return key;
            }
        }
        return null;
    }

    /** Lights a key, as it is pressed. */
    void light(final Kind kind, final @Nullable Symbol symbol) {
        this.litUntil[ComputerKeyboard.slot(kind, symbol)] = System.nanoTime() + ComputerKeyboard.LIT_NANOS;
    }

    private boolean lit(final Key key) {
        return System.nanoTime() < this.litUntil[ComputerKeyboard.slot(key.kind(), key.symbol())];
    }

    private static int slot(final Kind kind, final @Nullable Symbol symbol) {
        return kind == Kind.SYMBOL && symbol != null ? Kind.values().length + symbol.index() : kind.ordinal();
    }

    /**
     * Paints the keyboard.
     *
     * @param glyphs the glyph font of the computer's gate
     * @param typed the address typed so far
     * @param limit how many symbols an address can have
     * @param hovered the key under the pointer, if any
     * @param mode how the computer dials
     * @param alarmOff whether the alarm has been turned off
     * @param iris the gate's iris, or null if it has none, or there is no gate
     * @param busy whether the gate is dialing, open or shutting down, so there is something to abort
     * @param blink whether the next box's cursor shows
     */
    void paint(final ComputerCanvas canvas, final Identifier glyphs, final List<Symbol> typed, final int limit,
            final @Nullable Key hovered, final ComputerDialMode mode, final boolean alarmOff,
            final @Nullable IrisView iris, final boolean busy, final boolean blink) {
        canvas.clear(ComputerDisplay.BACKGROUND);
        for (int x = 0; x < ComputerKeyboard.WIDTH; x += 10) {
            canvas.fill(x, 0, 1, ComputerKeyboard.HEIGHT, ComputerDisplay.GRID);
        }
        for (int y = 0; y < ComputerKeyboard.HEIGHT; y += 10) {
            canvas.fill(0, y, ComputerKeyboard.WIDTH, 1, ComputerDisplay.GRID);
        }
        canvas.rect(0, 0, ComputerKeyboard.WIDTH, ComputerKeyboard.HEIGHT, ComputerDisplay.TEAL);
        this.paintAddress(canvas, glyphs, typed, limit, blink);
        for (final Key key : this.keys) {
            this.paintKey(canvas, glyphs, key, key == hovered, mode, alarmOff, iris, busy, !typed.isEmpty());
        }
        ComputerDisplay.scanLines(canvas);
    }

    // The address typed so far, a box for each symbol it can have
    private void paintAddress(final ComputerCanvas canvas, final Identifier glyphs, final List<Symbol> typed,
            final int limit, final boolean blink) {
        canvas.text("ADDRESS", 10, ComputerKeyboard.BOXES_Y + 10, 1, ComputerDisplay.CYAN_DIM);
        final int span = limit * ComputerKeyboard.BOX + (limit - 1) * ComputerKeyboard.GAP;
        final int left = (ComputerKeyboard.WIDTH - span) / 2;
        for (int i = 0; i < limit; i++) {
            final int x = left + i * (ComputerKeyboard.BOX + ComputerKeyboard.GAP);
            final boolean filled = i < typed.size();
            canvas.fill(x, ComputerKeyboard.BOXES_Y, ComputerKeyboard.BOX, ComputerKeyboard.BOX,
                    ComputerDisplay.GRID);
            canvas.rect(x, ComputerKeyboard.BOXES_Y, ComputerKeyboard.BOX, ComputerKeyboard.BOX,
                    filled ? ComputerDisplay.CYAN : ComputerDisplay.CYAN_DIM);
            if (filled) {
                canvas.glyph(glyphs, typed.get(i).character(), x + ComputerKeyboard.BOX / 2.0,
                        ComputerKeyboard.BOXES_Y + ComputerKeyboard.BOX / 2.0, ComputerKeyboard.BOX_GLYPH,
                        ComputerDisplay.WHITE);
            } else if (i == typed.size() && blink) {
                canvas.fill(x + 6, ComputerKeyboard.BOXES_Y + ComputerKeyboard.BOX - 6, ComputerKeyboard.BOX - 12, 2,
                        ComputerDisplay.CYAN);
            }
        }
        final String hint = "CTRL+V PASTE";
        canvas.text(hint, ComputerKeyboard.WIDTH - 10 - canvas.textWidth(hint, 1), ComputerKeyboard.BOXES_Y + 10, 1,
                ComputerDisplay.CYAN_FAINT);
    }

    /**
     * @return whether a key does anything now: the iris key does nothing for a gate with no iris, abort while the gate
     *     is idle, dial while it is busy, though an address can still be typed ready, nor backspace and clear with
     *     nothing typed
     */
    static boolean works(final Kind kind, final @Nullable IrisView iris, final boolean busy, final boolean typed) {
        return switch (kind) {
            case IRIS -> iris != null;
            case ABORT -> busy;
            case DIAL -> !busy;
            case BACK, CLEAR -> typed;
            default -> true;
        };
    }

    // Whether a gate's iris lets travelers through: an iris open, or a shield down
    private static boolean irisOpen(final IrisView iris) {
        return iris.state() != IrisState.CLOSED && iris.state() != IrisState.CLOSING;
    }

    private void paintKey(final ComputerCanvas canvas, final Identifier glyphs, final Key key, final boolean hovered,
            final ComputerDialMode mode, final boolean alarmOff, final @Nullable IrisView iris, final boolean busy,
            final boolean typed) {
        if (!ComputerKeyboard.works(key.kind(), iris, busy, typed)) {
            // A key that does nothing is drawn faint, and doesn't light up
            canvas.rect(key.x(), key.y(), key.width(), key.height(), ComputerDisplay.CYAN_FAINT);
            canvas.centeredText(key.kind() == Kind.IRIS ? "NO IRIS" : ComputerKeyboard.label(key.kind(), mode,
                    alarmOff, iris)[0], key.x() + key.width() / 2, key.y() + (key.height() - 8) / 2, 1,
                    ComputerDisplay.CYAN_FAINT);
            return;
        }
        final boolean lit = this.lit(key);
        // An open iris is red, as the gate is open to whatever comes through, as are the alarm turned off and abort
        final boolean warning = key.kind() == Kind.ALARM && alarmOff || key.kind() == Kind.ABORT
                || key.kind() == Kind.IRIS && iris != null && ComputerKeyboard.irisOpen(iris);
        final int edge = warning ? ComputerDisplay.RED : ComputerDisplay.TEAL;
        final int ink = lit ? ComputerDisplay.BACKGROUND : warning ? ComputerDisplay.RED : ComputerDisplay.CYAN;
        final int hint = lit ? ComputerDisplay.BACKGROUND : ComputerDisplay.CYAN_DIM;
        canvas.fill(key.x(), key.y(), key.width(), key.height(), lit ? (warning ? ComputerDisplay.RED
                : ComputerDisplay.CYAN) : ComputerDisplay.BACKGROUND);
        if (hovered && !lit) {
            canvas.fill(key.x(), key.y(), key.width(), key.height(), ComputerDisplay.CYAN_FAINT);
        }
        canvas.rect(key.x(), key.y(), key.width(), key.height(), lit ? ComputerDisplay.WHITE : edge);
        final int cx = key.x() + key.width() / 2;
        final Symbol symbol = key.symbol();
        if (key.kind() == Kind.SYMBOL && symbol != null) {
            canvas.glyph(glyphs, symbol.character(), cx, key.y() + 11, ComputerKeyboard.KEY_GLYPH,
                    lit ? ComputerDisplay.BACKGROUND : ComputerDisplay.WHITE);
            canvas.centeredText(String.valueOf(symbol.character()), cx, key.y() + 21, 1, ink);
            return;
        }
        final String[] lines = ComputerKeyboard.label(key.kind(), mode, alarmOff, iris);
        // A key too short for two lines has its typed key beside what it does, dimmer, as the others have it under
        if (lines.length == 2 && key.height() < 2 * 9 + 2) {
            final String gap = "  ";
            final int width = canvas.textWidth(lines[0] + gap + lines[1], 1);
            final int x = cx - width / 2;
            final int y = key.y() + (key.height() - 8) / 2;
            canvas.text(lines[0], x, y, 1, ink);
            canvas.text(lines[1], x + canvas.textWidth(lines[0] + gap, 1), y, 1, hint);
            return;
        }
        final int top = key.y() + (key.height() - lines.length * 9 + 1) / 2;
        for (int i = 0; i < lines.length; i++) {
            canvas.centeredText(lines[i], cx, top + i * 9, 1, i == 0 ? ink : hint);
        }
    }

    // What a key that isn't a symbol says: what it does, and under it the key it is typed with, if any
    private static String[] label(final Kind kind, final ComputerDialMode mode, final boolean alarmOff,
            final @Nullable IrisView iris) {
        return switch (kind) {
            case DIAL -> new String[] {"DIAL", "ENTER"};
            case BACK -> new String[] {"BACKSPACE"};
            case CLEAR -> new String[] {"CLEAR", "DELETE"};
            case MODE -> new String[] {switch (mode) {
                case GATE -> "GATE'S DIAL";
                case SLOW -> "SLOW DIAL";
                case FAST -> "FAST DIAL";
            }};
            case IRIS -> {
                final boolean shield = iris != null && iris.type().equals(IrisTypes.SHIELD);
                final boolean open = iris == null || ComputerKeyboard.irisOpen(iris);
                yield new String[] {shield ? (open ? "SHIELD DOWN" : "SHIELD UP")
                        : open ? "IRIS OPEN" : "IRIS CLOSED"};
            }
            case ALARM -> new String[] {alarmOff ? "ALARM OFF" : "ALARM ON"};
            case ABORT -> new String[] {"ABORT"};
            case LEAVE -> new String[] {"LEAVE", "ESC"};
            case SYMBOL -> new String[0];
        };
    }
}
