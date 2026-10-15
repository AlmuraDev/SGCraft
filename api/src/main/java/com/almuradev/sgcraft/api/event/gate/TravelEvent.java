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

import com.almuradev.sgcraft.api.event.Cancelable;
import com.almuradev.sgcraft.api.gate.Stargate;
import net.minecraft.world.entity.Entity;

/**
 * Something passing through a wormhole. {@link #gate()} is the gate it enters.
 *
 * @since 0.1.0
 */
public interface TravelEvent extends GateEvent {

    /** @return what is traveling */
    Entity traveler();

    /**
     * Fired when something enters the wormhole, before it moves. Canceling it leaves the traveler where it was.
     */
    interface Pre extends TravelEvent, Cancelable {

        /** @return the gate the traveler will come out of */
        Stargate destination();

        /**
         * @param destination the gate to send the traveler to instead; it need not be the other end of the
         *     connection
         */
        void setDestination(final Stargate destination);
    }

    /** Fired once the traveler has come out of the destination gate. */
    interface Post extends TravelEvent {

        /** @return the gate the traveler came out of */
        Stargate destination();
    }
}
