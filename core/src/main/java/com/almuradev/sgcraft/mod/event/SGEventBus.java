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

package com.almuradev.sgcraft.mod.event;

import com.almuradev.sgcraft.api.event.Cancelable;
import com.almuradev.sgcraft.api.event.Event;
import com.almuradev.sgcraft.api.event.EventBus;
import com.almuradev.sgcraft.api.event.EventListener;
import com.almuradev.sgcraft.api.event.EventPriority;
import com.almuradev.sgcraft.mod.SGCraft;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SGCraft's event bus. It is the same on every loader, and lives as long as the game does, so add-ons can register as
 * soon as they load. Listeners for a type also hear its subtypes; the listeners for each concrete event class are
 * worked out once and kept until a listener is added or removed.
 */
public final class SGEventBus implements EventBus {

    private static final SGEventBus INSTANCE = new SGEventBus();

    private final List<Listener<?>> listeners = new CopyOnWriteArrayList<>();
    private final Map<Class<?>, List<Listener<?>>> byClass = new ConcurrentHashMap<>();
    private final AtomicLong order = new AtomicLong();

    private SGEventBus() {
    }

    /** @return the bus */
    public static SGEventBus get() {
        return SGEventBus.INSTANCE;
    }

    @Override
    public <E extends Event> Registration register(final Class<E> type, final EventPriority priority,
            final boolean ignoreCanceled, final EventListener<? super E> listener) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        final Listener<E> registered = new Listener<>(type, priority, ignoreCanceled, listener,
                this.order.getAndIncrement());
        this.listeners.add(registered);
        this.byClass.clear();
        return () -> {
            if (this.listeners.remove(registered)) {
                this.byClass.clear();
            }
        };
    }

    /**
     * Tells every listener for the event's type, in priority order. A listener that throws is logged and skipped.
     *
     * @param event the event
     * @return the event, as the listeners left it
     */
    public <E extends Event> E post(final E event) {
        final List<Listener<?>> matching = this.byClass.computeIfAbsent(event.getClass(), this::matching);
        for (final Listener<?> listener : matching) {
            if (listener.ignoreCanceled && event instanceof Cancelable cancelable && cancelable.isCanceled()) {
                continue;
            }
            try {
                SGEventBus.call(listener, event);
            } catch (final RuntimeException | LinkageError e) {
                SGCraft.LOG.error("A listener for {} threw", event.getClass().getSimpleName(), e);
            }
        }
        return event;
    }

    /**
     * Posts a cancelable event.
     *
     * @return whether the action may go ahead: true unless a listener left the event canceled
     */
    public <E extends Event & Cancelable> boolean allows(final E event) {
        return !this.post(event).isCanceled();
    }

    @SuppressWarnings("unchecked")
    private static <E extends Event> void call(final Listener<?> listener, final Event event) {
        ((EventListener<Event>) listener.listener).handle(event);
    }

    private List<Listener<?>> matching(final Class<?> eventClass) {
        final List<Listener<?>> matching = new ArrayList<>();
        for (final Listener<?> listener : this.listeners) {
            if (listener.type.isAssignableFrom(eventClass)) {
                matching.add(listener);
            }
        }
        matching.sort(Comparator.<Listener<?>, EventPriority>comparing(listener -> listener.priority)
                .thenComparingLong(listener -> listener.order));
        return List.copyOf(matching);
    }

    private record Listener<E extends Event>(Class<E> type, EventPriority priority, boolean ignoreCanceled,
            EventListener<? super E> listener, long order) {
    }
}
