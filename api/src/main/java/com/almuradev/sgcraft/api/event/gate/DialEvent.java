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
import com.almuradev.sgcraft.api.event.Cancellable;
import com.almuradev.sgcraft.api.gate.DialResult;
import com.almuradev.sgcraft.api.gate.Stargate;

import java.util.Optional;

/**
 * A gate dialling out. {@link #gate()} is the dialling gate.
 *
 * @since 0.1.0
 */
public interface DialEvent extends GateEvent {

    /** @return the address being dialled */
    Address address();

    /**
     * Fired after the address is checked and before the first chevron locks. Cancelling it ends the dial with
     * {@link DialResult#CANCELLED}.
     */
    interface Pre extends DialEvent, Cancellable {

        /** @return the gate the address reaches */
        Stargate target();
    }

    /**
     * Fired after {@link Pre} if it isn't cancelled, to settle what the connection costs the dialling gate. Not
     * fired when the gate's {@link com.almuradev.sgcraft.api.gate.GateProperties#REQUIRES_POWER} property is false.
     * The dial fails with {@link DialResult#NOT_ENOUGH_ENERGY} if the gate can't pay the opening cost once every
     * listener has run.
     */
    interface Cost extends DialEvent {

        /** @return the gate the address reaches */
        Stargate target();

        /** @return the energy taken from the dialling gate when the wormhole opens */
        long openCost();

        /** @param cost the energy to take when the wormhole opens instead; not negative */
        void setOpenCost(final long cost);

        /** @return the energy taken from the dialling gate each tick the wormhole stays open */
        long tickCost();

        /** @param cost the energy to take each tick instead; not negative */
        void setTickCost(final long cost);
    }

    /** Fired once a chevron locks on the dialling gate. */
    interface LockChevron extends DialEvent {

        /** @return which chevron locked, from 0 for the first */
        int index();
    }

    /** Fired when the dial ends, whether the wormhole opened or not. */
    interface Post extends DialEvent {

        /** @return how the dial ended */
        DialResult result();

        /** @return the gate that was dialled, if the address reached one */
        Optional<Stargate> target();
    }
}
