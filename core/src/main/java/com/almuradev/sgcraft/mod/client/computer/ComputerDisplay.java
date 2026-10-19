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
import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.api.gate.GateTypes;
import com.almuradev.sgcraft.api.gate.IrisState;
import com.almuradev.sgcraft.api.gate.IrisTypes;
import com.almuradev.sgcraft.mod.Platform;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.block.entity.StargateBaseBlockEntity;
import com.almuradev.sgcraft.mod.computer.DialingTerminal;
import com.almuradev.sgcraft.mod.gate.ChevronOrder;
import com.almuradev.sgcraft.mod.gate.DialPhase;
import com.almuradev.sgcraft.mod.gate.DialStyle;
import com.almuradev.sgcraft.mod.gate.DialView;
import com.almuradev.sgcraft.mod.gate.IrisView;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Paints a dialing computer's screen: the SGC's dialing program, drawn after the show's. A column of scrolling data
 * and a light for each chevron stand on the left, the gate's ring in the middle, and a numbered box for each symbol
 * of the address on the right. As the gate dials, the symbol being dialed shows large inside the ring; when its
 * chevron locks, the chevron turns red on the ring and the symbol flies to its box. An incoming wormhole flashes the
 * screen red, and an open one fills the ring with the event horizon. The status line says how far the wormhole
 * reaches, by how many symbols its address has: 7 within this world, 8 to another world, and 9 to another group of
 * worlds, out of this reality.
 *
 * <p>Everything is read from the linked gate's base as the client has it, so every player near the computer sees
 * the same as the player working it.</p>
 */
final class ComputerDisplay {

    /** How many pixels across and down the screen is painted. */
    static final int WIDTH = 460;
    static final int HEIGHT = 170;

    // The program's colors, which its keyboard shares
    static final int BACKGROUND = 0xFF040E1A;
    static final int GRID = 0xFF081A28;
    static final int CYAN = 0xFF46D6E6;
    static final int CYAN_DIM = 0x8046D6E6;
    static final int CYAN_FAINT = 0x3046D6E6;
    static final int TEAL = 0xFF2E9FA8;
    static final int RED = 0xFFE8463C;
    static final int GREEN = 0xFF5BE38A;
    static final int WHITE = 0xFFE6F6FA;
    private static final int HORIZON = 0xFF0B3C78;
    private static final int HORIZON_LIGHT = 0x6082C8FF;

    // The ring: its middle, its outer edge, and the width of the band its glyphs stand in
    private static final int RING_X = 230;
    private static final int RING_Y = 74;
    private static final int RING = 62;
    private static final int BAND = 12;
    // How many glyphs a Milky Way ring carries, glyph 0 being the point of origin
    private static final int RING_GLYPHS = 39;
    // How long a locked symbol takes to fly from the middle of the ring to its box, in ticks
    private static final double FLY_TICKS = 12;
    // How long a hidden caller's box shows each symbol it flickers through
    private static final double SCRAMBLE_TICKS = 3;
    // The symbol being dialed, shown large in the middle of the ring
    private static final int BIG_GLYPH = 66;
    // The boxes on the right: where their column starts, and the space they share
    private static final int BOXES_X = 372;
    private static final int BOXES_TOP = 6;
    private static final int BOXES_BOTTOM = 164;
    private static final int BOX_WIDTH = 72;

    private ComputerDisplay() {
    }

    /**
     * What a computer's screen shows, read from its linked gate.
     *
     * @param linked whether the computer is linked to a formed gate
     * @param type the gate's type, which picks its glyphs and how its ring turns
     * @param chevrons how many chevrons the gate has
     * @param dial the gate's dialing, as its base shows it
     * @param iris the gate's iris, if it has one
     * @param alarmOff whether the alarm has been turned off
     * @param time the world's time, in ticks, with the part of a tick drawn
     */
    record View(boolean linked, ResourceKey<GateType> type, int chevrons, DialView dial, @Nullable IrisView iris,
            boolean alarmOff, double time) {

        /** @return what a computer's screen shows now */
        static View of(final Level level, final DialingTerminal computer, final float partialTick) {
            final double time = level.getGameTime() + partialTick;
            if (computer.linked().isPresent()
                    && level.getBlockEntity(computer.linked().get()) instanceof final StargateBaseBlockEntity gate
                    && gate.isFormed()) {
                return new View(true, gate.gateType(), gate.chevrons(), gate.dialView(), gate.iris(),
                        computer.alarmOff(), time);
            }
            return new View(false, GateTypes.MILKY_WAY, 7, DialView.IDLE, null, false, time);
        }
    }

    static void paint(final ComputerCanvas canvas, final View view) {
        canvas.clear(ComputerDisplay.BACKGROUND);
        for (int x = 0; x < ComputerDisplay.WIDTH; x += 10) {
            canvas.fill(x, 0, 1, ComputerDisplay.HEIGHT, ComputerDisplay.GRID);
        }
        for (int y = 0; y < ComputerDisplay.HEIGHT; y += 10) {
            canvas.fill(0, y, ComputerDisplay.WIDTH, 1, ComputerDisplay.GRID);
        }
        final DialView dial = view.dial();
        final boolean incoming = view.linked() && dial.active() && !dial.initiator();
        final boolean alarm = incoming && ((long) (view.time() / 10)) % 2 == 0;
        canvas.rect(1, 1, ComputerDisplay.WIDTH - 2, ComputerDisplay.HEIGHT - 2,
                alarm ? ComputerDisplay.RED : ComputerDisplay.CYAN_DIM);
        if (alarm) {
            canvas.rect(2, 2, ComputerDisplay.WIDTH - 4, ComputerDisplay.HEIGHT - 4, ComputerDisplay.RED);
        }
        // An idle gate shows a box and a light for each chevron it has; a dialing one, for each symbol it dials
        final int length = dial.active() ? Math.max(1, dial.length()) : view.linked() ? view.chevrons() : 7;
        ComputerDisplay.data(canvas, view);
        ComputerDisplay.status(canvas, view, length);
        ComputerDisplay.ring(canvas, view, length);
        if (!view.linked()) {
            // No gate in reach, large across the empty ring
            canvas.centeredText("NO GATE", ComputerDisplay.RING_X, ComputerDisplay.RING_Y - 17, 2, ComputerDisplay.RED);
            canvas.centeredText("FOUND", ComputerDisplay.RING_X, ComputerDisplay.RING_Y + 2, 2, ComputerDisplay.RED);
        }
        ComputerDisplay.boxes(canvas, view, length);
        ComputerDisplay.statusBar(canvas, view, length);
        ComputerDisplay.scanLines(canvas);
    }

    // The version the program shows, read once, such as V26.3.0.0
    private static @Nullable String version;

    private static String version() {
        if (ComputerDisplay.version == null) {
            final String mod = Platform.get().modVersion();
            ComputerDisplay.version = mod.isEmpty() ? "" : "V" + mod.toUpperCase(Locale.ROOT);
        }
        return ComputerDisplay.version;
    }

    // The scrolling columns of ones and zeros, faster while the gate is busy
    private static void data(final ComputerCanvas canvas, final View view) {
        canvas.rect(4, 4, 114, 13, ComputerDisplay.CYAN_DIM);
        canvas.text("SGC DIAL PROGRAM", 8, 7, 1, ComputerDisplay.CYAN);
        // The program's version, SGCraft's own, tagged on beside its name
        final String version = ComputerDisplay.version();
        if (!version.isEmpty()) {
            final int width = canvas.textWidth(version, 1) + 8;
            canvas.rect(120, 4, width, 13, ComputerDisplay.CYAN_DIM);
            canvas.text(version, 124, 7, 1, ComputerDisplay.CYAN_DIM);
        }
        canvas.rect(4, 19, 114, 86, ComputerDisplay.CYAN_DIM);
        final long scroll = (long) (view.time() / (view.dial().active() ? 2 : 10));
        for (int row = 0; row < 9; row++) {
            final long line = scroll + row;
            final StringBuilder bits = new StringBuilder();
            long hash = line * 0x9E3779B97F4A7C15L;
            hash ^= hash >>> 31;
            hash *= 0xBF58476D1CE4E5B9L;
            for (int bit = 0; bit < 16; bit++) {
                bits.append(((hash >>> bit) & 1) == 0 ? '0' : '1');
            }
            // Brighter toward the middle of the column, as on a glowing tube
            final int fade = 0x60 + 0x9F * (4 - Math.abs(row - 4)) / 4;
            canvas.text(bits.toString(), 10, 23 + row * 9, 1, (fade << 24) | (ComputerDisplay.TEAL & 0xFFFFFF));
        }
    }

    // The status box: whether the gate is busy, a light and OK for each chevron locked, and the iris
    private static void status(final ComputerCanvas canvas, final View view, final int length) {
        canvas.rect(4, 107, 114, 60, ComputerDisplay.CYAN_DIM);
        final DialView dial = view.dial();
        final String state = !view.linked() ? "OFFLINE" : dial.active() ? "ACTIVE" : "IDLE";
        canvas.text("STATUS: " + state, 8, 110, 1, view.linked() ? ComputerDisplay.CYAN : ComputerDisplay.RED);
        for (int i = 0; i < Math.min(length, 9); i++) {
            final int x = 8 + (i / 5) * 54;
            final int y = 120 + (i % 5) * 9;
            final boolean locked = i < dial.engaged();
            if (locked) {
                canvas.fill(x, y, 6, 6, ComputerDisplay.GREEN);
            } else {
                canvas.rect(x, y, 6, 6, ComputerDisplay.CYAN_DIM);
            }
            canvas.text(String.valueOf(i + 1), x + 9, y - 1, 1, ComputerDisplay.CYAN);
            if (locked) {
                canvas.text("OK", x + 20, y - 1, 1, ComputerDisplay.GREEN);
            }
        }
        final IrisView iris = view.iris();
        if (iris != null) {
            final boolean shield = iris.type().equals(IrisTypes.SHIELD);
            final boolean shut = iris.state() == IrisState.CLOSED || iris.state() == IrisState.CLOSING;
            final String text = (shield ? "SHIELD " : "IRIS ") + (shut ? (shield ? "UP" : "CLOSED")
                    : (shield ? "DOWN" : "OPEN"));
            // Against the box's right edge, clear of the chevron lights
            canvas.text(text, 114 - canvas.textWidth(text, 1), 156, 1, shut ? ComputerDisplay.RED
                    : ComputerDisplay.CYAN);
        }
    }

    // The ring, its glyphs turning with the gate's, its chevrons lit as they lock, and the symbol being dialed
    private static void ring(final ComputerCanvas canvas, final View view, final int length) {
        final int cx = ComputerDisplay.RING_X;
        final int cy = ComputerDisplay.RING_Y;
        final int outer = ComputerDisplay.RING;
        final int inner = outer - ComputerDisplay.BAND;
        final DialView dial = view.dial();
        final double time = view.time();
        if (dial.phase() == DialPhase.CONNECTED || dial.phase() == DialPhase.TRANSIENT) {
            canvas.disc(cx, cy, inner - 1, ComputerDisplay.HORIZON);
            for (int wave = 0; wave < 5; wave++) {
                final double radius = (time * 0.8 + wave * 10) % (inner - 2);
                canvas.circle(cx, cy, radius, ComputerDisplay.HORIZON_LIGHT);
            }
        }
        final int line = view.linked() ? ComputerDisplay.CYAN : ComputerDisplay.CYAN_DIM;
        canvas.circle(cx, cy, outer, line);
        canvas.circle(cx, cy, outer + 2, ComputerDisplay.CYAN_DIM);
        canvas.circle(cx, cy, inner, line);
        // The glyphs on the ring, where the gate's own stand; a Pegasus or Ori gate's don't turn
        final DialStyle style = DialStyle.of(view.type());
        final boolean turns = style == DialStyle.MILKY_WAY || style == DialStyle.UNIVERSE;
        final double turn = turns ? dial.ringAngle(time) : 0;
        final Identifier glyphs = ComputerDisplay.glyphs(view.type());
        for (int glyph = 0; glyph < ComputerDisplay.RING_GLYPHS; glyph++) {
            final double slot = glyph * 360.0 / ComputerDisplay.RING_GLYPHS - turn;
            final double edge = Math.toRadians(slot - 180.0 / ComputerDisplay.RING_GLYPHS);
            canvas.line(cx + Math.sin(edge) * inner, cy - Math.cos(edge) * inner, cx + Math.sin(edge) * outer,
                    cy - Math.cos(edge) * outer, ComputerDisplay.CYAN_FAINT);
            // A Universe gate carries no @
            if (glyph == 0 && style == DialStyle.UNIVERSE) {
                continue;
            }
            // Glyph 0, the point of origin, stands for @, as on the gate; the other symbols follow it in order
            final Symbol symbol = DialStyle.milkyWaySymbol(glyph);
            final char character = symbol.character();
            final double angle = style == DialStyle.UNIVERSE
                    ? DialStyle.universeOffset(symbol.index()) - turn : slot;
            final double radians = Math.toRadians(angle);
            final double radius = outer - ComputerDisplay.BAND / 2.0;
            final boolean top = Math.abs(((angle % 360) + 540) % 360 - 180) < 180.0 / ComputerDisplay.RING_GLYPHS;
            canvas.glyph(glyphs, character, cx + Math.sin(radians) * radius, cy - Math.cos(radians) * radius, 9,
                    top && dial.active() ? ComputerDisplay.WHITE : ComputerDisplay.CYAN_DIM);
        }
        // The chevrons, red once locked
        final boolean[] locked = ChevronOrder.locked(view.chevrons(), length, dial.engaged());
        final int first = ChevronOrder.first(view.chevrons());
        for (int position = first; position < first + view.chevrons() && position < ChevronOrder.POSITIONS;
                position++) {
            ComputerDisplay.chevron(canvas, 90 - ChevronOrder.angle(position), locked[position]);
        }
        // An empty ring; paint writes across it that there is no gate
        if (!view.linked()) {
            return;
        }
        final List<Symbol> symbols = dial.symbols();
        final int engaged = dial.engaged();
        // The symbol being dialed, large in the middle, pulsing while the ring turns to it
        if (dial.initiator() && engaged < symbols.size() && (dial.phase() == DialPhase.DIALING
                || dial.phase() == DialPhase.INTER_DIALING) && time - dial.lockTick() >= ComputerDisplay.FLY_TICKS) {
            final float pulse = 0.65F + 0.35F * (float) Math.sin(time * 0.4);
            canvas.glyph(glyphs, symbols.get(engaged).character(), cx, cy, ComputerDisplay.BIG_GLYPH,
                    ComputerDisplay.alpha(ComputerDisplay.WHITE, pulse));
        }
        // The symbol last locked, flying to its box
        final double flown = (time - dial.lockTick()) / ComputerDisplay.FLY_TICKS;
        if (engaged > 0 && engaged <= symbols.size() && flown >= 0 && flown < 1) {
            final double ease = flown * flown * (3 - 2 * flown);
            final int[] box = ComputerDisplay.box(engaged - 1, length);
            final double x = cx + (box[0] + box[2] / 2.0 - cx) * ease;
            final double y = cy + (box[1] + box[3] / 2.0 - cy) * ease;
            final int size = (int) Math.round(ComputerDisplay.BIG_GLYPH + (box[3] - 4 - ComputerDisplay.BIG_GLYPH)
                    * ease);
            canvas.glyph(glyphs, symbols.get(engaged - 1).character(), x, y, size, ComputerDisplay.WHITE);
        }
        if (!dial.initiator() && dial.active() && ((long) (time / 10)) % 2 == 0) {
            canvas.centeredText("INCOMING", cx, cy - 8, 2, ComputerDisplay.RED);
        }
    }

    // A chevron: a V at the ring's edge, pointing in, at an angle clockwise from the top
    private static void chevron(final ComputerCanvas canvas, final double angle, final boolean lit) {
        final double radians = Math.toRadians(angle);
        final double sin = Math.sin(radians);
        final double cos = Math.cos(radians);
        final int cx = ComputerDisplay.RING_X;
        final int cy = ComputerDisplay.RING_Y;
        final int color = lit ? ComputerDisplay.RED : ComputerDisplay.CYAN;
        for (double width = 0; width <= 3; width += 0.5) {
            final double tip = ComputerDisplay.RING - 4 + width;
            final double arms = ComputerDisplay.RING + 7 + width;
            final double spread = 5 - width * 0.6;
            final double tipX = cx + sin * tip;
            final double tipY = cy - cos * tip;
            canvas.line(tipX, tipY, cx + sin * arms + cos * spread, cy - cos * arms + sin * spread, color);
            canvas.line(tipX, tipY, cx + sin * arms - cos * spread, cy - cos * arms - sin * spread, color);
        }
    }

    // The numbered boxes, a symbol in each once its chevron has locked, wired to the ring. A caller that hides its
    // address sends none of its symbols, so each box it locks flickers through symbols at random, never settling
    private static void boxes(final ComputerCanvas canvas, final View view, final int length) {
        final DialView dial = view.dial();
        final List<Symbol> symbols = dial.symbols();
        final boolean hidden = dial.addressHidden();
        final Identifier glyphs = ComputerDisplay.glyphs(view.type());
        final int engaged = dial.engaged();
        final boolean flying = view.time() - dial.lockTick() < ComputerDisplay.FLY_TICKS;
        for (int i = 0; i < length; i++) {
            final int[] box = ComputerDisplay.box(i, length);
            final boolean locked = i < engaged && (hidden || i < symbols.size());
            final boolean landed = locked && (hidden || !(flying && i == engaged - 1));
            final boolean next = dial.active() && dial.initiator() && i == engaged
                    && ((long) (view.time() / 6)) % 2 == 0;
            canvas.text(String.valueOf(i + 1), box[0] - 10, box[1] + box[3] / 2 - 4, 1, ComputerDisplay.CYAN);
            canvas.rect(box[0], box[1], box[2], box[3], landed ? ComputerDisplay.RED
                    : next ? ComputerDisplay.WHITE : ComputerDisplay.CYAN_DIM);
            if (landed) {
                canvas.glyph(glyphs, hidden ? ComputerDisplay.scrambled(view, i) : symbols.get(i).character(),
                        box[0] + box[2] / 2.0, box[1] + box[3] / 2.0, box[3] - 4,
                        hidden ? ComputerDisplay.RED : ComputerDisplay.WHITE);
                // A wire from the box to its chevron on the ring, for the chevrons on the ring's near side, each
                // bending at its own place so the wires don't run together
                final int[] positions = ChevronOrder.positions(view.chevrons(), length);
                final double angle = Math.toRadians(90 - ChevronOrder.angle(positions[i]));
                if (Math.sin(angle) > 0.1) {
                    final double reach = ComputerDisplay.RING + 9;
                    final double endX = ComputerDisplay.RING_X + Math.sin(angle) * reach;
                    final double endY = ComputerDisplay.RING_Y - Math.cos(angle) * reach;
                    final double bend = Math.max(endX + 4, 316 + i * 4);
                    final double middle = box[1] + box[3] / 2.0;
                    final int wire = ComputerDisplay.alpha(ComputerDisplay.RED, 0.6F);
                    canvas.line(box[0] - 12, middle, bend, middle, wire);
                    canvas.line(bend, middle, bend, endY, wire);
                    canvas.line(bend, endY, endX, endY, wire);
                }
            }
        }
    }

    // The symbol a hidden caller's box shows now: a new one at random every few ticks, each box its own
    private static char scrambled(final View view, final int index) {
        final long step = (long) (view.time() / ComputerDisplay.SCRAMBLE_TICKS);
        final long mixed = (step * 0x9E3779B97F4A7C15L + index * 0xBF58476D1CE4E5B9L) >>> 33;
        // A Universe gate carries no @, so it is passed over there
        final boolean universe = DialStyle.of(view.type()) == DialStyle.UNIVERSE;
        final int count = universe ? Symbol.COUNT - 1 : Symbol.COUNT;
        final int pick = (int) (mixed % count);
        return new Symbol(universe && pick >= Symbol.GIZA.index() ? pick + 1 : pick).character();
    }

    // Where a box is: its left, top, width and height
    private static int[] box(final int index, final int length) {
        final int space = (ComputerDisplay.BOXES_BOTTOM - ComputerDisplay.BOXES_TOP) / Math.max(7, length);
        final int height = Math.min(22, space - 2);
        return new int[] {ComputerDisplay.BOXES_X, ComputerDisplay.BOXES_TOP + index * space, ComputerDisplay.BOX_WIDTH,
            height};
    }

    // The status line under the ring, and what it says
    private static void statusBar(final ComputerCanvas canvas, final View view, final int length) {
        canvas.rect(150, 145, 160, 22, ComputerDisplay.CYAN_DIM);
        final DialView dial = view.dial();
        final int engaged = dial.engaged();
        String main;
        String detail = "";
        int color = ComputerDisplay.CYAN;
        if (!view.linked()) {
            main = "NO GATE FOUND";
            detail = "NONE IN REACH";
            color = ComputerDisplay.RED;
        } else if (!dial.initiator() && dial.active()) {
            main = dial.phase() == DialPhase.CONNECTED ? "INCOMING WORMHOLE" : ComputerDisplay.activation(length);
            detail = view.alarmOff() ? "ALARM OFF" : dial.phase() == DialPhase.CONNECTED
                    ? ComputerDisplay.origin(length) : dial.addressHidden() ? "ADDRESS HIDDEN"
                    : engaged > 0 ? "CHEVRON " + engaged + " LOCKED" : "";
            color = ComputerDisplay.RED;
        } else {
            switch (dial.phase()) {
                case DIALING, INTER_DIALING, SYNC_AWAIT -> {
                    main = "SEQUENCE IN PROGRESS";
                    detail = engaged > 0 ? "CHEVRON " + engaged + (engaged >= length ? " LOCKED" : " ENCODED") : "";
                }
                case ESTABLISHING, TRANSIENT -> {
                    main = "CHEVRON " + Math.max(engaged, length) + " LOCKED";
                    detail = "ESTABLISHING WORMHOLE";
                }
                case CONNECTED -> {
                    main = "WORMHOLE ESTABLISHED";
                    detail = ComputerDisplay.destination(length);
                    color = ComputerDisplay.GREEN;
                }
                case DISCONNECTING -> main = "WORMHOLE DISENGAGED";
                default -> main = "SYSTEM READY";
            }
        }
        canvas.centeredText(main, 230, 148, 1, color);
        if (!detail.isEmpty()) {
            canvas.centeredText(detail, 230, 157, 1, ComputerDisplay.CYAN_DIM);
        }
    }

    // What an incoming dial is called, by how far it comes from: within this world, from another, or from another
    // group of worlds
    private static String activation(final int length) {
        return length <= 7 ? "LOCAL ACTIVATION" : length >= 9 ? "OFF-REALITY ACTIVATION" : "OFFWORLD ACTIVATION";
    }

    private static String origin(final int length) {
        return length <= 7 ? "FROM THIS WORLD" : length >= 9 ? "FROM ANOTHER REALITY" : "FROM OFFWORLD";
    }

    private static String destination(final int length) {
        return length <= 7 ? "LOCAL DESTINATION" : length >= 9 ? "OFF-REALITY DESTINATION" : "OFFWORLD DESTINATION";
    }

    // Darkens every other row a little, as on a tube's screen
    static void scanLines(final ComputerCanvas canvas) {
        for (int y = 1; y < canvas.height; y += 2) {
            for (int x = 0; x < canvas.width; x++) {
                canvas.blend(x, y, 0xFF000000, 0.18F);
            }
        }
    }

    private static int alpha(final int color, final float alpha) {
        return ((int) (((color >>> 24) & 0xFF) * Math.min(1, Math.max(0, alpha))) << 24) | (color & 0xFFFFFF);
    }

    // The glyph font a gate's type is drawn with
    static Identifier glyphs(final ResourceKey<GateType> type) {
        final String name = type.equals(GateTypes.PEGASUS) ? "pegasus"
                : type.equals(GateTypes.UNIVERSE) ? "universe" : "milkyway";
        return SGCraft.id("textures/font/glyphs_" + name + ".png");
    }
}
