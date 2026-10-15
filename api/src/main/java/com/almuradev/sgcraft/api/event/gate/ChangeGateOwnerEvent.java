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

import java.util.Optional;
import java.util.UUID;

/**
 * A gate being handed to another owner through
 * {@link com.almuradev.sgcraft.api.gate.Stargate#setOwner(Optional, com.almuradev.sgcraft.api.Cause)}. A gate
 * getting its first owner as it is built fires none of these; {@link FormGateEvent} and {@link PlaceGateEvent} cover
 * that.
 *
 * @since 0.1.0
 */
public interface ChangeGateOwnerEvent extends GateEvent {

    /** @return the owner before the change, or empty if the gate had none */
    Optional<UUID> oldOwner();

    /** @return the owner after the change, or empty if the gate is being left with none */
    Optional<UUID> newOwner();

    /** Fired before the change. Canceling it keeps the old owner. */
    interface Pre extends ChangeGateOwnerEvent, Cancelable {
    }

    /** Fired once the change is made. */
    interface Post extends ChangeGateOwnerEvent {
    }
}
