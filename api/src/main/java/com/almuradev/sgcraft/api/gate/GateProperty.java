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

package com.almuradev.sgcraft.api.gate;

import com.mojang.serialization.Codec;

import java.util.Objects;
import java.util.Optional;

/**
 * A setting or fitting each gate holds its own value of, such as how long its wormhole stays open, or which iris it
 * has.
 *
 * <p>Properties are entries in the {@link com.almuradev.sgcraft.api.SGCraftRegistries#GATE_PROPERTY} registry, so
 * an add-on registers its own through its mod loader. Implementations must keep each gate's values for every
 * registered property, saved with {@link #codec()}. {@link GateProperties} holds the properties every
 * implementation must register.</p>
 *
 * <p>A property may have a default. A gate that has never had a value set has the default, if there is one, and
 * otherwise no value at all, such as a gate with no iris.</p>
 *
 * <p>Each property is its own instance, and two properties are equal only if they are the same instance, even when
 * their codec and default match.</p>
 *
 * @param <T> the type of the value
 * @since 0.1.0
 */
public final class GateProperty<T> {

    private final Codec<T> codec;
    private final Optional<T> defaultValue;

    /**
     * A property with no default, which a gate has no value of until one is set.
     *
     * @param codec how the value is saved
     */
    public GateProperty(final Codec<T> codec) {
        this.codec = Objects.requireNonNull(codec, "codec");
        this.defaultValue = Optional.empty();
    }

    /**
     * @param codec how the value is saved
     * @param defaultValue the value of a gate that has never had one set
     */
    public GateProperty(final Codec<T> codec, final T defaultValue) {
        this.codec = Objects.requireNonNull(codec, "codec");
        this.defaultValue = Optional.of(defaultValue);
    }

    /** @return how the value is saved */
    public Codec<T> codec() {
        return this.codec;
    }

    /**
     * @return the value of a gate that has never had one set, if the property has a default; implementations may
     *     let the server's configuration choose another default, which {@link Stargate#get(GateProperty)} then
     *     gives in place of this one
     */
    public Optional<T> defaultValue() {
        return this.defaultValue;
    }
}
