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

import com.almuradev.sgcraft.api.event.Cancellable;
import com.almuradev.sgcraft.api.event.Event;
import com.almuradev.sgcraft.api.gate.GatePlacement;
import com.almuradev.sgcraft.api.gate.Stargates;

/**
 * A gate being placed through {@link Stargates#place(GatePlacement, com.almuradev.sgcraft.api.Cause)}.
 *
 * @since 0.1.0
 */
public interface PlaceGateEvent extends Event {

    /**
     * Fired before anything is placed, and before the search for a spot. Cancelling it fails the placement with
     * {@link com.almuradev.sgcraft.api.gate.PlacementResult.Status#CANCELLED}.
     */
    interface Pre extends PlaceGateEvent, Cancellable {

        /** @return the placement as it will be carried out */
        GatePlacement placement();

        /** @param placement the placement to carry out instead, such as one with other fixtures */
        void setPlacement(final GatePlacement placement);
    }

    /** Fired once the gate is placed and formed. */
    interface Post extends PlaceGateEvent, GateEvent {

        /** @return the placement as it was carried out, at the spot the gate went */
        GatePlacement placement();
    }
}
