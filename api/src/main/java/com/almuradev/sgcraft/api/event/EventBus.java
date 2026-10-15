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

package com.almuradev.sgcraft.api.event;

/**
 * Where listeners register for the API's events. It works the same on every mod loader.
 *
 * <p>A listener registered for a type also receives events of its subtypes, so a listener for {@code GateEvent}
 * hears every gate event. Only implementations fire the API's events.</p>
 *
 * <p>A listener that throws doesn't stop the others. Implementations must log what it threw and carry on with the
 * next listener.</p>
 *
 * @since 0.1.0
 */
public interface EventBus {

    /**
     * @param type the type of event
     * @param priority when the listener runs
     * @param ignoreCanceled whether to skip the listener when a {@link Cancelable} event is already canceled
     * @param listener the listener
     * @return the registration, which can remove the listener later
     */
    <E extends Event> Registration register(final Class<E> type, final EventPriority priority,
            final boolean ignoreCanceled, final EventListener<? super E> listener);

    /**
     * Registers a listener at {@link EventPriority#NORMAL} that also runs for canceled events.
     *
     * @param type the type of event
     * @param listener the listener
     * @return the registration, which can remove the listener later
     */
    default <E extends Event> Registration register(final Class<E> type, final EventListener<? super E> listener) {
        return this.register(type, EventPriority.NORMAL, false, listener);
    }

    /** A registered listener. */
    interface Registration {

        /** Removes the listener. It hears no events after this returns. Calling it again does nothing. */
        void unregister();
    }
}
