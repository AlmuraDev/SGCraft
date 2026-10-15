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
import com.almuradev.sgcraft.api.gate.GateProperty;

import java.util.Optional;

/**
 * A gate's property being set or removed through {@link com.almuradev.sgcraft.api.gate.Stargate}.
 *
 * <p>A listener reads the values through the property it cares about, which also tells it whether this event is
 * about that property: {@link #newValue(GateProperty)} is empty for any other property.</p>
 *
 * @since 0.1.0
 */
public interface ChangeGatePropertyEvent extends GateEvent {

    /** @return the property */
    GateProperty<?> property();

    /**
     * @param property the property to read the value as
     * @return the value before the change, or empty if there was none or {@code property} isn't this event's
     */
    <T> Optional<T> oldValue(final GateProperty<T> property);

    /**
     * @param property the property to read the value as
     * @return the value after the change, or empty if the change removes the value or {@code property} isn't this
     *     event's
     */
    <T> Optional<T> newValue(final GateProperty<T> property);

    /** Fired before the change. Canceling it keeps the old value. */
    interface Pre extends ChangeGatePropertyEvent, Cancelable {

        /**
         * @param property this event's property
         * @param value the value to set instead, or empty to remove the value
         * @return false, changing nothing, if {@code property} isn't this event's
         */
        <T> boolean setNewValue(final GateProperty<T> property, final Optional<T> value);
    }

    /** Fired once the change is made. */
    interface Post extends ChangeGatePropertyEvent {
    }
}
