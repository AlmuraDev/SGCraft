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

package com.almuradev.sgcraft.api;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Every object that had a hand in an action, from the most direct to the least.
 *
 * <p>The first object is the {@linkplain #root() root}, the most direct cause, and each object after it acted
 * through the ones before it. A player dialling with a DHD, for example, gives the DHD's block entity and then the
 * player. Listeners should look up the types they care about with {@link #first(Class)} and the other lookups, so
 * that a cause holding types they don't know about still works for them.</p>
 *
 * <p>Any non-null object can be part of a cause. Implementations should use Minecraft's own objects where one fits,
 * such as a {@code ServerPlayer}, a {@code BlockEntity} or the {@code MinecraftServer}. Each event documents what
 * its cause must contain. Callers acting through the API should put an object of their own at the root, such as
 * their block entity, their entity or a record type they define.</p>
 *
 * <p>A cause is immutable. Two causes are equal when they hold equal objects in the same order, and
 * {@link #hashCode()} must agree with that. An implementation may wrap a cause from another system instead of
 * copying it.</p>
 *
 * <p>The design follows the {@code Cause} of SpongeAPI, by SpongePowered and its contributors, MIT licensed:
 * <a href="https://github.com/SpongePowered/SpongeAPI">github.com/SpongePowered/SpongeAPI</a>.</p>
 *
 * @since 0.1.0
 */
public interface Cause extends Iterable<Object> {

    /**
     * @param root the most direct cause
     * @param others the rest, from the most direct to the least
     * @return the cause
     * @throws NullPointerException if {@code root} or any of {@code others} is null
     */
    static Cause of(final Object root, final Object... others) {
        return SGCraftApi.get().causeFactory().of(root, others);
    }

    /** @return the most direct cause */
    Object root();

    /**
     * @param type the type to look for
     * @return the most direct object of that type, if there is one
     */
    <T> Optional<T> first(final Class<T> type);

    /**
     * @param type the type to look for
     * @return the least direct object of that type, if there is one
     */
    <T> Optional<T> last(final Class<T> type);

    /**
     * @param type the type to look for
     * @return every object of that type, from the most direct to the least
     */
    <T> List<T> all(final Class<T> type);

    /**
     * @param type the type to look for
     * @return whether any object is of that type
     */
    boolean contains(final Class<?> type);

    /** @return every object, from the most direct to the least */
    List<Object> objects();

    /**
     * @param direct an object acting through this cause, such as an add-on's own object dialling for a player
     * @return a new cause with {@code direct} as its root, followed by this cause's objects
     * @throws NullPointerException if {@code direct} is null
     */
    Cause withDirect(final Object direct);

    /** @return the objects from the most direct to the least */
    @Override
    default Iterator<Object> iterator() {
        return this.objects().iterator();
    }

    /** The factory behind {@link Cause#of(Object, Object...)}. Callers should use that method instead. */
    interface Factory {

        /**
         * @param root the most direct cause
         * @param others the rest, from the most direct to the least
         * @return the cause
         * @throws NullPointerException if {@code root} or any of {@code others} is null
         */
        Cause of(final Object root, final Object... others);
    }
}
