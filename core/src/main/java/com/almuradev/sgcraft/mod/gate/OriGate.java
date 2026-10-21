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

package com.almuradev.sgcraft.mod.gate;

import com.almuradev.sgcraft.api.gate.FixtureBuffer;
import com.almuradev.sgcraft.api.gate.GateSize;
import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.api.gate.GateTypes;
import com.almuradev.sgcraft.api.gate.IrisType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;

/**
 * The Ori supergate, as in SGCraft 2.2.0: built only as a supergate, it connects only to other Ori supergates, takes
 * no iris and no upgrades, and always has nine chevrons' reach. It has no chevrons to see: its ring is a circle of
 * segments that link one after another, anticlockwise from the top, as it dials, and the same at the gate dialed.
 *
 * <p>How far the ring has linked is worked out from the gate's {@link DialView} at a time, on the server for the
 * linking sounds and on clients for the drawing, so both agree.</p>
 */
public final class OriGate implements GateType {

    /**
     * How long a dial takes from its first symbol to the whoosh of its wormhole forming, in ticks: its dialing sound
     * plays from the first symbol and swells there, as in 2.2.0.
     */
    public static final int DIAL_TICKS = 444;
    /** How long the gate rests between one symbol linking and the next starting, in ticks. */
    public static final int INTER_DIALING_TICKS = 6;
    /** How long the wormhole takes to form once the ring has linked, before its horizon fills: in ticks. */
    public static final int ESTABLISH_TICKS = 5;
    /** How long the horizon takes to fill from the ring inward, before the kawoosh, in ticks. */
    public static final int FILL_TICKS = 30;
    /** How long into the wormhole forming its whoosh starts, in ticks. */
    public static final int WHOOSH_TICKS = OriGate.ESTABLISH_TICKS + 5;
    /** How long the gates wait on each other once every chevron has locked, in the same world, in ticks. */
    public static final int SYNC_TICKS = 10;
    // The least a symbol takes, in ticks
    private static final int MIN_SYMBOL_TICKS = 10;
    // How long a segment is round the ring, and the fewest a ring has
    private static final double SEGMENT_PITCH = 1.65;
    private static final int MIN_SEGMENTS = 22;

    private final Component name;

    public OriGate(final Component name) {
        this.name = name;
    }

    @Override
    public Component name() {
        return this.name;
    }

    @Override
    public boolean supports(final GateSize size) {
        return GateShape.of(size).map(shape -> shape.radius() >= GateShape.MIN_SUPERGATE_RADIUS).orElse(false);
    }

    @Override
    public FixtureBuffer fixtures(final GateSize size) {
        return new FixtureBuffer();
    }

    @Override
    public boolean connectsTo(final ResourceKey<GateType> other) {
        return other.identifier().equals(GateTypes.ORI.identifier());
    }

    @Override
    public boolean acceptsIris(final ResourceKey<IrisType> iris) {
        return false;
    }

    /** @return whether a type is the Ori supergate */
    public static boolean is(final ResourceKey<GateType> type) {
        return type.identifier().equals(GateTypes.ORI.identifier());
    }

    /**
     * @param symbols how many symbols the address has
     * @param index the symbol's place in the address, from 0
     * @param syncTicks how long the gates wait on each other once every chevron has locked
     * @return how long a symbol takes to link, in ticks, so the dial from its first symbol to the whoosh takes
     *     {@link #DIAL_TICKS} whatever the address; the first takes what doesn't divide evenly. Each phase lasts a
     *     tick more than the ticks it is given.
     */
    public static int symbolTicks(final int symbols, final int index, final int syncTicks) {
        final int length = Math.max(1, symbols);
        final int budget = OriGate.DIAL_TICKS - (length - 1) * (OriGate.INTER_DIALING_TICKS + 1) - (syncTicks + 1)
                - OriGate.WHOOSH_TICKS;
        final int each = budget / length;
        final int spare = index == 0 ? budget - each * length : 0;
        return Math.max(OriGate.MIN_SYMBOL_TICKS, each - 1 + spare);
    }

    /** @return how many segments the ring of a supergate of this shape has */
    public static int segments(final GateShape shape) {
        final double outer = shape.radius() + 0.5;
        final double middle = (0.8 * outer + outer) / 2;
        return Math.max(OriGate.MIN_SEGMENTS, (int) Math.round(2 * Math.PI * middle / OriGate.SEGMENT_PITCH));
    }

    /**
     * How far round the ring has linked: from 0 for none to the number of segments for all. Segment {@code k}, counted
     * anticlockwise from the top, glows as much as this is past {@code k}, up to fully.
     *
     * @param time the game time, which may have a fraction
     */
    public static double reach(final DialView view, final double time, final int segments) {
        final int length = Math.max(1, view.length());
        final double progress = switch (view.phase()) {
            case DIALING -> {
                final double span = Math.max(1, view.ringEnd() - view.ringStart());
                final double symbol = Math.max(0, Math.min(1, (time - view.ringStart()) / span));
                yield Math.min(1, (view.engaged() + symbol) / length);
            }
            case INTER_DIALING -> Math.min(1, (double) view.engaged() / length);
            case IDLE -> 0;
            default -> 1;
        };
        return switch (view.phase()) {
            // The last link holds half made until the wormhole forms
            case SYNC_AWAIT, ESTABLISHING -> segments - 0.5;
            case DIALING, INTER_DIALING, IDLE -> progress * (segments - 0.5);
            default -> progress * segments;
        };
    }

    /** @return how brightly segment {@code k} glows at a reach, from 0 to 1 */
    public static double glow(final double reach, final int k) {
        return Math.max(0, Math.min(1, reach - k));
    }

    /**
     * @return how far the horizon has filled in from the ring, from 0 to 1, while the wormhole is forming; 1 once it
     *     has
     */
    public static double fill(final DialView view, final double time) {
        if (view.phase() != DialPhase.ESTABLISHING) {
            return view.active() && view.phase() != DialPhase.DIALING && view.phase() != DialPhase.INTER_DIALING
                    && view.phase() != DialPhase.SYNC_AWAIT ? 1 : 0;
        }
        return Math.max(0, Math.min(1, (time - view.phaseStart() - OriGate.ESTABLISH_TICKS) / OriGate.FILL_TICKS));
    }
}
