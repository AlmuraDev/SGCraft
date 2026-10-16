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

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;

import java.util.List;

/**
 * A style of gate, such as a Milky Way or a Pegasus gate.
 *
 * <p>Gate types are entries in the {@link com.almuradev.sgcraft.api.SGCraftRegistries#GATE_TYPE} registry, so an
 * add-on registers its own through its mod loader. {@link GateTypes} names the types every implementation must
 * register. How a type looks and sounds belongs to the client and is not part of this interface.</p>
 *
 * @since 0.1.0
 */
public interface GateType {

    /** @return the type's name as players see it */
    Component name();

    /**
     * @param size a gate size
     * @return whether a gate of this type can be built at that size
     */
    boolean supports(final GateSize size);

    /**
     * The blocks this type builds around a gate, such as a DHD. A type with none, such as one dialed only
     * remotely, returns an empty buffer.
     *
     * @param size the size of the gate's ring
     * @return a new buffer each call, which the caller may change
     */
    FixtureBuffer fixtures(final GateSize size);

    /** @return the conditions on where gates of this type can be placed, beyond the space being free */
    default List<PlacementRule> placementRules() {
        return List.of();
    }

    /**
     * Whether a DHD can dial gates of this type. Each DHD is made for one type of gate, its style. A type that accepts
     * no DHD is dialed some other way, such as through the API.
     *
     * @param style the type of gate the DHD is made for
     * @return whether that DHD can dial a gate of this type
     */
    default boolean acceptsDhd(final ResourceKey<GateType> style) {
        return false;
    }

    /**
     * Whether a gate of this type can connect to a gate of another type. A wormhole forms only if both types allow
     * it, each asked about the other's type, so either type can refuse the connection.
     *
     * @param other the type of the gate at the other end
     * @return whether a gate of this type can connect to it
     */
    default boolean connectsTo(final ResourceKey<GateType> other) {
        return true;
    }

    /**
     * Whether a gate of this type can have an iris of a kind fitted.
     *
     * @param iris the kind of iris
     * @return whether it can be fitted
     */
    default boolean acceptsIris(final ResourceKey<IrisType> iris) {
        return true;
    }
}
