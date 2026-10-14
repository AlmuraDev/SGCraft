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
import com.almuradev.sgcraft.api.gate.GateSize;
import com.almuradev.sgcraft.api.gate.GateType;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.GlobalPos;

/**
 * A gate forming from blocks a player built, as opposed to one {@linkplain PlaceGateEvent placed} whole.
 *
 * @since 0.1.0
 */
public interface FormGateEvent extends Event {

    /**
     * Fired when the last block of a gate is in place, before the gate forms. Cancelling it leaves the blocks
     * unformed.
     */
    interface Pre extends FormGateEvent, Cancellable {

        /** @return where the gate's base block is */
        GlobalPos position();

        /** @return which way the gate would face */
        FrontAndTop orientation();

        /** @return the gate's type */
        GateType type();

        /** @return the size of the gate's ring */
        GateSize size();
    }

    /** Fired once the gate has formed. */
    interface Post extends FormGateEvent, GateEvent {
    }
}
