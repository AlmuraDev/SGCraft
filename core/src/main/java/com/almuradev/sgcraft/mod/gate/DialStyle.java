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

package com.almuradev.sgcraft.mod.gate;

import com.almuradev.sgcraft.api.address.Symbol;
import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.api.gate.GateTypes;
import net.minecraft.resources.ResourceKey;

/**
 * How each type of gate dials, with the timings of the original SGCraft.
 *
 * <p>A Milky Way gate turns its symbol ring to bring each symbol to the top chevron; a Universe gate turns its whole
 * ring the same way. Both always go the long way round, at least half a turn. A Pegasus gate's ring doesn't turn:
 * a light runs round its screens to each chevron instead, the first anticlockwise, then each the other way to the one
 * before. The angle a move covers is the ring's turn, or how far the light runs.</p>
 */
public enum DialStyle {

    MILKY_WAY(24, 30),
    PEGASUS(10, 10),
    UNIVERSE(24, 30),
    // The Ori supergate's ring doesn't turn: each symbol takes its share of the dial as its segments link, and the
    // wormhole forms, then fills the horizon from the ring inward, before the kawoosh
    ORI(OriGate.INTER_DIALING_TICKS, OriGate.ESTABLISH_TICKS + OriGate.FILL_TICKS);

    // How fast a ring turns, in degrees a tick, unless the gate says otherwise
    private static final double RING_SPEED = 2.0;
    // How many glyphs the Milky Way ring carries; glyph 0 is the point of origin
    private static final int MILKY_WAY_GLYPHS = 39;
    // The Universe gate's symbols, every one but @: four to each ninth of the ring, between the gaps under its
    // chevrons, but five to the two ninths either side of the bottom, the third and fifth clockwise from the top
    private static final int[] UNIVERSE_GROUPS = {4, 4, 4, 5, 4, 5, 4, 4, 4};
    private static final double UNIVERSE_GROUP = 40;
    private static final double UNIVERSE_GAP = 4;
    /** How many symbols a Universe gate carries: every one but {@code @}. */
    public static final int UNIVERSE_SYMBOLS = Symbol.COUNT - 1;
    // How long a Pegasus gate's light takes to pass one screen, and how far apart the screens are
    private static final double PEGASUS_TICKS_PER_GLYPH = 1.4;
    private static final double PEGASUS_GLYPH_ANGLE = 10;

    private final int interDialingTicks;
    private final int establishingTicks;

    DialStyle(final int interDialingTicks, final int establishingTicks) {
        this.interDialingTicks = interDialingTicks;
        this.establishingTicks = establishingTicks;
    }

    public static DialStyle of(final ResourceKey<GateType> type) {
        if (type.equals(GateTypes.PEGASUS)) {
            return DialStyle.PEGASUS;
        }
        if (OriGate.is(type)) {
            return DialStyle.ORI;
        }
        return type.equals(GateTypes.UNIVERSE) ? DialStyle.UNIVERSE : DialStyle.MILKY_WAY;
    }

    /**
     * @return how fast a gate of this style dials unless it says otherwise, in degrees a tick: its ring's turn, or
     *     its light's run
     */
    public double defaultSpeed() {
        return this == DialStyle.PEGASUS ? DialStyle.PEGASUS_GLYPH_ANGLE / DialStyle.PEGASUS_TICKS_PER_GLYPH
                : DialStyle.RING_SPEED;
    }

    /** @return how long the gate rests between one chevron locking and the next symbol starting */
    public int interDialingTicks() {
        return this.interDialingTicks;
    }

    /** @return how long the wormhole takes to form once every chevron has locked */
    public int establishingTicks() {
        return this.establishingTicks;
    }

    /**
     * One symbol's move.
     *
     * @param from the angle the move starts at
     * @param to the angle it ends at
     * @param ticks how long it takes
     */
    public record Move(float from, float to, int ticks) {
    }

    /**
     * @param angle where the ring is, or how far the light has run, before this symbol
     * @param symbol the symbol being dialed
     * @param index its place in the address, from 0
     * @param positions the chevron position each symbol of the address locks at; see {@link ChevronOrder}
     * @param speed how fast the gate dials, in degrees a tick
     * @return the move to dial the symbol
     */
    public Move move(final float angle, final Symbol symbol, final int index, final int[] positions,
            final double speed) {
        if (this == DialStyle.ORI) {
            return new Move(angle, angle, OriGate.symbolTicks(positions.length, index, OriGate.SYNC_TICKS));
        }
        if (this == DialStyle.PEGASUS) {
            final int from = index == 0 ? ChevronOrder.TOP : positions[index - 1];
            final int to = positions[index];
            final int between = (from - to) * 40;
            final int direction = index % 2 == 0 ? 1 : -1;
            int distance = ((direction * between) % 360 + 360) % 360;
            if (distance <= 240) {
                distance += 360;
            }
            final double travel = direction * Math.round(distance / DialStyle.PEGASUS_GLYPH_ANGLE)
                    * DialStyle.PEGASUS_GLYPH_ANGLE;
            final int ticks = (int) Math.round(Math.abs(travel) / speed);
            return new Move(angle, (float) (angle + travel), ticks);
        }
        float start = angle;
        if (Math.abs(start) >= 720) {
            start %= 360;
        }
        double target = this.symbolAngle(symbol);
        double difference = target - start;
        if (Math.abs(difference) < 180) {
            target -= Math.copySign(360, start == 0 ? 1 : start);
            difference = target - start;
        }
        return new Move(start, (float) target, (int) Math.abs(difference / speed));
    }

    /**
     * @return how far the ring turns, anticlockwise, to bring the symbol to the top chevron; 0 for a symbol the ring
     *     doesn't carry
     */
    public double symbolAngle(final Symbol symbol) {
        if (this == DialStyle.UNIVERSE) {
            return DialStyle.universeOffset(symbol.index());
        }
        return DialStyle.milkyWayGlyph(symbol) * 360.0 / DialStyle.MILKY_WAY_GLYPHS;
    }

    /**
     * @return which of the Milky Way ring's 39 glyphs, counted anticlockwise from the top, stands for a symbol: glyph 0
     *     is the point of origin, {@code @}, and the other symbols follow it in order
     */
    public static int milkyWayGlyph(final Symbol symbol) {
        final int origin = Symbol.GIZA.index();
        return symbol.index() == origin ? 0 : symbol.index() < origin ? symbol.index() + 1 : symbol.index();
    }

    /** @return the symbol a glyph of the Milky Way ring stands for, counted anticlockwise from the top */
    public static Symbol milkyWaySymbol(final int glyph) {
        final int origin = Symbol.GIZA.index();
        return new Symbol(glyph == 0 ? origin : glyph <= origin ? glyph - 1 : glyph);
    }

    /** @return how far clockwise from the top a Universe gate's symbol sits, or 0 if it carries no such symbol */
    public static double universeOffset(final int symbol) {
        final int slot = DialStyle.universeSlot(symbol);
        return slot < 0 ? 0 : DialStyle.universeSlotOffset(slot);
    }

    /**
     * @return which of a Universe gate's arches, counted clockwise from the top, carries a symbol, or -1 for
     *     {@code @}, which it carries none of: the symbols are in order, passing over {@code @}
     */
    public static int universeSlot(final int symbol) {
        final int origin = Symbol.GIZA.index();
        return symbol == origin ? -1 : symbol < origin ? symbol : symbol - 1;
    }

    /** @return how far clockwise from the top the middle of one of a Universe gate's arches is */
    public static double universeSlotOffset(final int slot) {
        int first = 0;
        for (int group = 0; group < DialStyle.UNIVERSE_GROUPS.length; group++) {
            final int size = DialStyle.UNIVERSE_GROUPS[group];
            if (slot < first + size) {
                return group * DialStyle.UNIVERSE_GROUP + DialStyle.UNIVERSE_GAP
                        + (slot - first + 0.5) * DialStyle.universeSlotWidth(slot);
            }
            first += size;
        }
        return 0;
    }

    /**
     * @return how wide one of a Universe gate's arches is, in degrees: a ninth of the ring, less the gaps under its
     *     chevrons, shared by the arches in it
     */
    public static double universeSlotWidth(final int slot) {
        int first = 0;
        for (final int size : DialStyle.UNIVERSE_GROUPS) {
            if (slot < first + size) {
                return (DialStyle.UNIVERSE_GROUP - 2 * DialStyle.UNIVERSE_GAP) / size;
            }
            first += size;
        }
        return 0;
    }

    /** @return how many arches the ninth of a Universe gate's ring clockwise from a chevron has */
    public static int universeGroupSize(final int group) {
        return DialStyle.UNIVERSE_GROUPS[group];
    }
}
