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

package com.almuradev.sgcraft.gate;

import com.almuradev.sgcraft.api.address.Symbol;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * What a gate is doing, as its base sends it to clients to draw. Times are the level's game time, so a client can
 * animate between updates.
 *
 * @param phase the step of the dial the gate is at
 * @param initiator whether this gate dialed, rather than being dialed
 * @param address the symbols in play: those the gate dialed, or, for the dialed gate, the dialing gate's address
 * @param engaged how many chevrons have locked
 * @param ringFrom the dialing gate's ring angle when it started its current move, in degrees anticlockwise
 * @param ringTo the angle it is moving to
 * @param ringStart when the move started
 * @param ringEnd when the move ends
 * @param lockTick when the last chevron locked
 * @param fillStart when a dialed Pegasus gate starts filling with light
 * @param fillEnd when it is full
 * @param phaseStart when the gate entered its phase
 * @param expected how many symbols the address will have, if more than are in it so far, as while a DHD dials; 0
 *     when the address is complete
 */
public record DialView(DialPhase phase, boolean initiator, String address, int engaged, float ringFrom,
        float ringTo, long ringStart, long ringEnd, long lockTick, long fillStart, long fillEnd, long phaseStart,
        int expected) {

    /** A gate doing nothing. */
    public static final DialView IDLE = new DialView(DialPhase.IDLE, false, "", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

    /** @return how many symbols the address has, or will have; which chevron each locks at depends on it */
    public int length() {
        return Math.max(this.expected, this.symbols().size());
    }

    /** @return the symbols in play, in order */
    public List<Symbol> symbols() {
        final List<Symbol> symbols = new ArrayList<>(this.address.length());
        for (final char character : this.address.toCharArray()) {
            Symbol.of(character).ifPresent(symbols::add);
        }
        return symbols;
    }

    /** @return whether the gate is dialing, being dialed or open, but not idle or shutting down */
    public boolean active() {
        return this.phase != DialPhase.IDLE && this.phase != DialPhase.DISCONNECTING;
    }

    /**
     * @return the dialing gate's ring angle at {@code time}, moving evenly from {@link #ringFrom} to
     *     {@link #ringTo}
     */
    public float ringAngle(final double time) {
        if (this.ringEnd <= this.ringStart || time >= this.ringEnd) {
            return this.ringTo;
        }
        final double t = Math.max(0, (time - this.ringStart) / (this.ringEnd - this.ringStart));
        return (float) (this.ringFrom + (this.ringTo - this.ringFrom) * t);
    }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("phase", this.phase.name());
        tag.putBoolean("initiator", this.initiator);
        tag.putString("address", this.address);
        tag.putInt("engaged", this.engaged);
        tag.putFloat("ring_from", this.ringFrom);
        tag.putFloat("ring_to", this.ringTo);
        tag.putLong("ring_start", this.ringStart);
        tag.putLong("ring_end", this.ringEnd);
        tag.putLong("lock_tick", this.lockTick);
        tag.putLong("fill_start", this.fillStart);
        tag.putLong("fill_end", this.fillEnd);
        tag.putLong("phase_start", this.phaseStart);
        tag.putInt("expected", this.expected);
        return tag;
    }

    public static DialView load(final CompoundTag tag) {
        final DialPhase phase = tag.getString("phase")
                .flatMap(name -> {
                    try {
                        return Optional.of(DialPhase.valueOf(name.toUpperCase(Locale.ROOT)));
                    } catch (final IllegalArgumentException e) {
                        return Optional.empty();
                    }
                })
                .orElse(DialPhase.IDLE);
        return new DialView(phase, tag.getBooleanOr("initiator", false), tag.getStringOr("address", ""),
                tag.getIntOr("engaged", 0), tag.getFloatOr("ring_from", 0), tag.getFloatOr("ring_to", 0),
                tag.getLongOr("ring_start", 0), tag.getLongOr("ring_end", 0), tag.getLongOr("lock_tick", 0),
                tag.getLongOr("fill_start", 0), tag.getLongOr("fill_end", 0), tag.getLongOr("phase_start", 0),
                tag.getIntOr("expected", 0));
    }
}
