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

import com.almuradev.sgcraft.api.address.Addressing;
import com.almuradev.sgcraft.api.addressbook.AddressBooks;
import com.almuradev.sgcraft.api.energy.EnergyService;
import com.almuradev.sgcraft.api.event.EventBus;
import com.almuradev.sgcraft.api.gate.Stargates;

import java.util.ServiceLoader;

/**
 * The entry point to the API.
 *
 * <p>{@link #get()} returns the single instance, which hands out each part of the API. The instance is found as a
 * Java service, so an implementation must name its class in
 * {@code META-INF/services/com.almuradev.sgcraft.api.SGCraftApi}. The instance must be usable as soon as it is
 * loaded, in whatever order mods start; a part that needs a running server says so. Code that depends on an
 * implementation only optionally must ask its mod loader whether one is installed before it touches any class in
 * this API.</p>
 *
 * <p>The API is the {@code com.almuradev.sgcraft.api} packages and nothing else. Add-ons should build against the
 * api jar alone.</p>
 *
 * @since 0.1.0
 */
public interface SGCraftApi {

    /**
     * The version of the API contracts. It follows semantic versioning separately from any implementation's
     * version: a new major version may break callers, and a new minor version only adds.
     */
    String VERSION = "0.1.0";

    /** The namespace of every identifier the API defines. */
    String NAMESPACE = "sgcraft";

    /**
     * @return the API
     * @throws IllegalStateException if no implementation is installed
     */
    static SGCraftApi get() {
        return Holder.INSTANCE;
    }

    /** @return the addressing service */
    Addressing addressing();

    /** @return the service that finds gates */
    Stargates stargates();

    /** @return the service that gets address books */
    AddressBooks addressBooks();

    /** @return energy settings across all gates */
    EnergyService energy();

    /** @return the event bus */
    EventBus eventBus();

    /** @return the factory behind {@link Cause#of(Object, Object...)} */
    Cause.Factory causeFactory();

    /** Holds the instance, which is loaded the first time {@link #get()} is called. Callers should use that method. */
    final class Holder {

        private static final SGCraftApi INSTANCE =
                ServiceLoader.load(SGCraftApi.class, SGCraftApi.class.getClassLoader())
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "No implementation of " + SGCraftApi.class.getName() + " is installed"));

        private Holder() {
        }
    }
}
