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

import com.almuradev.sgcraft.api.SGCraftRegistries;
import com.almuradev.sgcraft.api.address.AddressReach;
import com.almuradev.sgcraft.api.address.AddressReaches;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.GateProperty;
import com.almuradev.sgcraft.registry.RegistrySink;
import net.minecraft.resources.ResourceKey;

/**
 * The registry entries the API requires every implementation to register: the three address reaches and the gate
 * properties.
 */
public final class SGBuiltIns {

    private SGBuiltIns() {
    }

    public static void registerReaches(final RegistrySink<AddressReach> sink) {
        sink.register(AddressReaches.SAME_WORLD, new GridReach(7, GridReach.Kind.SAME_WORLD));
        sink.register(AddressReaches.SAME_GROUP, new GridReach(8, GridReach.Kind.SAME_GROUP));
        sink.register(AddressReaches.OTHER_GROUP, new GridReach(9, GridReach.Kind.OTHER_GROUP));
    }

    public static void registerProperties(final RegistrySink<GateProperty<?>> sink) {
        GateProperties.all().forEach((id, property) ->
                sink.register(ResourceKey.create(SGCraftRegistries.GATE_PROPERTY, id), property));
    }
}
