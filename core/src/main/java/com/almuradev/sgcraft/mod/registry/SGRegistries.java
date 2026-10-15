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

package com.almuradev.sgcraft.mod.registry;

import com.almuradev.sgcraft.api.SGCraftRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/**
 * Creates the registries the API defines, through each loader's own way of making a registry.
 */
public final class SGRegistries {

    private SGRegistries() {
    }

    /** Makes one registry; each loader provides its own. */
    public interface Creator {

        <T> void create(final ResourceKey<Registry<T>> key);
    }

    /** Asks the loader to create every registry the API defines. */
    public static void createAll(final Creator creator) {
        creator.create(SGCraftRegistries.ADDRESS_REACH);
        creator.create(SGCraftRegistries.GATE_TYPE);
        creator.create(SGCraftRegistries.IRIS_TYPE);
        creator.create(SGCraftRegistries.GATE_PROPERTY);
        creator.create(SGCraftRegistries.ADDRESS_BOOK_PROVIDER);
        creator.create(SGCraftRegistries.ENERGY_SOURCE_PROVIDER);
        creator.create(SGCraftRegistries.ENERGY_BRIDGE);
    }
}
