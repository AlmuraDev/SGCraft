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

package com.almuradev.sgcraft.mod;

import com.almuradev.sgcraft.api.Cause;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A cause held as a list, the most direct object first.
 */
public final class SimpleCause implements Cause {

    private final List<Object> objects;

    private SimpleCause(final List<Object> objects) {
        this.objects = List.copyOf(objects);
    }

    public static Cause of(final Object root, final Object... others) {
        final List<Object> objects = new ArrayList<>(others.length + 1);
        objects.add(Objects.requireNonNull(root, "root"));
        for (final Object other : others) {
            objects.add(Objects.requireNonNull(other, "others"));
        }
        return new SimpleCause(objects);
    }

    @Override
    public Object root() {
        return this.objects.getFirst();
    }

    @Override
    public <T> Optional<T> first(final Class<T> type) {
        for (final Object object : this.objects) {
            if (type.isInstance(object)) {
                return Optional.of(type.cast(object));
            }
        }
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> last(final Class<T> type) {
        for (int i = this.objects.size() - 1; i >= 0; i--) {
            if (type.isInstance(this.objects.get(i))) {
                return Optional.of(type.cast(this.objects.get(i)));
            }
        }
        return Optional.empty();
    }

    @Override
    public <T> List<T> all(final Class<T> type) {
        return this.objects.stream().filter(type::isInstance).map(type::cast).toList();
    }

    @Override
    public boolean contains(final Class<?> type) {
        return this.objects.stream().anyMatch(type::isInstance);
    }

    @Override
    public List<Object> objects() {
        return this.objects;
    }

    @Override
    public Cause withDirect(final Object direct) {
        final List<Object> objects = new ArrayList<>(this.objects.size() + 1);
        objects.add(Objects.requireNonNull(direct, "direct"));
        objects.addAll(this.objects);
        return new SimpleCause(objects);
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof SimpleCause cause && this.objects.equals(cause.objects);
    }

    @Override
    public int hashCode() {
        return this.objects.hashCode();
    }

    @Override
    public String toString() {
        return "Cause" + this.objects;
    }
}
