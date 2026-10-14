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

package com.almuradev.sgcraft.api.access;

import com.almuradev.sgcraft.api.SGCraftApi;
import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;

import java.util.Objects;

/**
 * Something a player may or may not do with a gate.
 *
 * <p>The API defines three rights, and implementations must check them. An add-on may define its own in its own
 * namespace and check it through {@link GateAccess#allows(java.util.UUID, GateRight)}; implementations store and
 * report such rights but give them no meaning.</p>
 *
 * @param id the right's identifier
 * @since 0.1.0
 */
public record GateRight(Identifier id) {

    /** How a right is saved: as its identifier. */
    public static final Codec<GateRight> CODEC = Identifier.CODEC.xmap(GateRight::new, GateRight::id);

    /** Dialling the gate, through its DHD or otherwise, and closing its wormhole. */
    public static final GateRight USE = GateRight.of("use");

    /** Opening and closing the gate's iris. */
    public static final GateRight IRIS = GateRight.of("iris");

    /** Changing the gate's type, chevrons, properties and access. */
    public static final GateRight CONFIGURE = GateRight.of("configure");

    public GateRight {
        Objects.requireNonNull(id, "id");
    }

    private static GateRight of(final String path) {
        return new GateRight(Identifier.fromNamespaceAndPath(SGCraftApi.NAMESPACE, path));
    }
}
