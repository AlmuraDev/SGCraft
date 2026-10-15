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

package com.almuradev.sgcraft.api.address;

import com.almuradev.sgcraft.api.SGCraftApi;
import com.almuradev.sgcraft.api.SGCraftRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * The reaches every implementation must register.
 *
 * @since 0.1.0
 */
public final class AddressReaches {

    /** Seven symbols: a gate in the same world as the dialing gate, by its position. */
    public static final ResourceKey<AddressReach> SAME_WORLD = AddressReaches.key("same_world");

    /**
     * Eight symbols: a gate in another world of the dialing gate's {@linkplain WorldGroup group}. The position,
     * then the world's symbol.
     */
    public static final ResourceKey<AddressReach> SAME_GROUP = AddressReaches.key("same_group");

    /** Nine symbols: a gate in a world of another group. The position, the world's symbol, then the group's. */
    public static final ResourceKey<AddressReach> OTHER_GROUP = AddressReaches.key("other_group");

    private AddressReaches() {
    }

    private static ResourceKey<AddressReach> key(final String path) {
        return ResourceKey.create(SGCraftRegistries.ADDRESS_REACH,
                Identifier.fromNamespaceAndPath(SGCraftApi.NAMESPACE, path));
    }
}
