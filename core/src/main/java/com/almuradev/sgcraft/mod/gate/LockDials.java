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

import com.almuradev.sgcraft.api.ActionResult;
import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.gate.DialResult;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.GateState;
import com.almuradev.sgcraft.api.gate.Stargate;
import net.minecraft.core.GlobalPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Dials for a gate whose chevrons lock as each symbol is entered ({@link GateProperties#CHEVRONS_LOCK_AS_DIALED}),
 * from a PDD or EDD: one symbol is {@linkplain Stargate#engage locked} every {@value #SYMBOL_TICKS} ticks, as someone
 * pressing a DHD's keys would, and the gate dials what is locked once the last is in. As in 2.3, whose PDD did the
 * same.
 */
public final class LockDials {

    /** How far apart the symbols are entered, in ticks. */
    public static final int SYMBOL_TICKS = 15;

    private static final Map<GlobalPos, LockDial> DIALS = new HashMap<>();

    private LockDials() {
    }

    /** @return whether a dial from a PDD or EDD at this gate goes a symbol at a time */
    public static boolean wanted(final Stargate gate) {
        return gate.get(GateProperties.CHEVRONS_LOCK_AS_DIALED).orElse(false)
                && !gate.get(GateProperties.CHEVRONS_LOCK_ON_DIAL).orElse(false);
    }

    /**
     * Starts entering an address a symbol at a time, the first now.
     *
     * @param failed told why, if a symbol isn't taken or the dial fails
     * @return whether it started: the gate is idle, with no symbols locked, and not entering another address
     */
    public static boolean start(final Stargate gate, final Address address, final Cause cause,
            final Consumer<DialResult> failed) {
        if (gate.state() != GateState.IDLE || !gate.engaged().isEmpty()
                || LockDials.DIALS.containsKey(gate.position())) {
            failed.accept(DialResult.BUSY);
            return false;
        }
        // The whole address is known, so its chevrons light in its order from the first, the top one last
        if (gate instanceof final SGStargate sg) {
            sg.expectEngaged(address.size());
        }
        final LockDial dial = new LockDial(gate, address, cause, failed);
        LockDials.DIALS.put(gate.position(), dial);
        if (!dial.next()) {
            LockDials.DIALS.remove(gate.position());
        }
        return true;
    }

    /** Enters the next symbol of each address that is due. Call each server tick. */
    public static void tick() {
        final Iterator<LockDial> dials = LockDials.DIALS.values().iterator();
        while (dials.hasNext()) {
            final LockDial dial = dials.next();
            if (++dial.ticks >= LockDials.SYMBOL_TICKS && !dial.next()) {
                dials.remove();
            }
        }
    }

    /** Forgets every dial, as the server stops. */
    public static void clear() {
        LockDials.DIALS.clear();
    }

    private static final class LockDial {

        private final Stargate gate;
        private final Address address;
        private final Cause cause;
        private final Consumer<DialResult> failed;
        private int symbol;
        private int ticks;

        LockDial(final Stargate gate, final Address address, final Cause cause, final Consumer<DialResult> failed) {
            this.gate = gate;
            this.address = address;
            this.cause = cause;
            this.failed = failed;
        }

        // Locks the next symbol, and dials once the last is in; false once it is done or has stopped
        boolean next() {
            this.ticks = 0;
            // Stopped since the last: disconnected, or its symbols released
            if (!this.gate.exists() || this.gate.engaged().size() != this.symbol) {
                return false;
            }
            final ActionResult result = this.gate.engage(this.address.get(this.symbol), this.cause);
            if (result != ActionResult.DONE) {
                this.failed.accept(result == ActionResult.DENIED ? DialResult.DENIED : DialResult.INTERRUPTED);
                this.gate.disconnect(this.cause);
                return false;
            }
            if (++this.symbol < this.address.size()) {
                return true;
            }
            this.gate.dialEngaged(this.cause).thenAccept(outcome -> {
                if (outcome != DialResult.CONNECTED) {
                    this.failed.accept(outcome);
                }
            });
            return false;
        }
    }
}
