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
import com.almuradev.sgcraft.api.event.Event;
import com.almuradev.sgcraft.api.gate.Connection;

/**
 * A wormhole opening or closing. The event is fired once per connection, not once per gate.
 *
 * @since 0.1.0
 */
public interface ConnectionEvent extends Event {

    /** @return the connection */
    Connection connection();

    /** Fired once the wormhole is open. */
    interface Open extends ConnectionEvent {
    }

    /**
     * Fired before the wormhole closes because someone asked it to. Canceling it keeps the wormhole open. A wormhole
     * that closes because its time ran out or a gate broke fires only {@link Close}.
     */
    interface Disconnect extends ConnectionEvent, Cancelable {
    }

    /** Fired once the wormhole has closed, for any reason. */
    interface Close extends ConnectionEvent {
    }
}
