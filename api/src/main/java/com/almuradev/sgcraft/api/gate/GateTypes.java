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

import com.almuradev.sgcraft.api.SGCraftApi;
import com.almuradev.sgcraft.api.SGCraftRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * The gate types every implementation must register.
 *
 * @since 0.1.0
 */
public final class GateTypes {

    /** The Milky Way gate, the style the Ancients built throughout the Milky Way. */
    public static final ResourceKey<GateType> MILKY_WAY = GateTypes.key("milky_way");

    /** The Pegasus gate, the style found in the Pegasus galaxy. */
    public static final ResourceKey<GateType> PEGASUS = GateTypes.key("pegasus");

    /** The Universe gate, the style the seed ships set down ahead of Destiny. */
    public static final ResourceKey<GateType> UNIVERSE = GateTypes.key("universe");

    /**
     * The Ori supergate, built only as a supergate. It connects only to other Ori supergates and takes no iris.
     */
    public static final ResourceKey<GateType> ORI = GateTypes.key("ori");

    private GateTypes() {
    }

    private static ResourceKey<GateType> key(final String path) {
        return ResourceKey.create(SGCraftRegistries.GATE_TYPE,
                Identifier.fromNamespaceAndPath(SGCraftApi.NAMESPACE, path));
    }
}
