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

package com.almuradev.sgcraft.api.event.gate;

import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.event.Cancelable;
import com.almuradev.sgcraft.api.gate.DialResult;
import com.almuradev.sgcraft.api.gate.Stargate;

import java.util.Optional;

/**
 * A gate dialing out. {@link #gate()} is the dialing gate.
 *
 * @since 0.1.0
 */
public interface DialEvent extends GateEvent {

    /** @return the address being dialed */
    Address address();

    /**
     * Fired after the address is checked and before the first chevron locks. Canceling it ends the dial with
     * {@link DialResult#CANCELED}.
     */
    interface Pre extends DialEvent, Cancelable {

        /** @return the gate the address reaches */
        Stargate target();
    }

    /**
     * Fired after {@link Pre} if it isn't canceled, to settle what the connection costs the dialing gate. Not
     * fired when the gate's {@link com.almuradev.sgcraft.api.gate.GateProperties#REQUIRES_POWER} property is false.
     * The dial fails with {@link DialResult#NOT_ENOUGH_ENERGY} if, once every listener has run, the gate can't pay
     * for every chevron and the opening together, or later if it can't pay a chevron as it locks or the opening as
     * the wormhole forms.
     */
    interface Cost extends DialEvent {

        /** @return the gate the address reaches */
        Stargate target();

        /** @return the energy taken from the dialing gate as each chevron locks */
        long chevronCost();

        /** @param cost the energy to take as each chevron locks instead; not negative */
        void setChevronCost(final long cost);

        /** @return the energy taken from the dialing gate when the wormhole opens */
        long openCost();

        /** @param cost the energy to take when the wormhole opens instead; not negative */
        void setOpenCost(final long cost);

        /**
         * @return the energy taken from the dialing gate each tick the wormhole stays open, which may be a
         *     fraction; the gate pays whole units as the fractions add up
         */
        double tickCost();

        /** @param cost the energy to take each tick instead, which may be a fraction; not negative */
        void setTickCost(final double cost);
    }

    /** Fired once a chevron locks on the dialing gate. */
    interface LockChevron extends DialEvent {

        /** @return which chevron locked, from 0 for the first */
        int index();
    }

    /** Fired when the dial ends, whether the wormhole opened or not. */
    interface Post extends DialEvent {

        /** @return how the dial ended */
        DialResult result();

        /** @return the gate that was dialed, if the address reached one */
        Optional<Stargate> target();
    }
}
