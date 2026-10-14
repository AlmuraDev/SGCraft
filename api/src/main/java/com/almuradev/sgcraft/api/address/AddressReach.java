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

import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * One kind of address: how many symbols it has, and what they mean.
 *
 * <p>Reaches are entries in the {@link com.almuradev.sgcraft.api.SGCraftRegistries#ADDRESS_REACH} registry, so an
 * add-on registers its own through its mod loader, such as a ten-symbol address that reaches another server.
 * {@link AddressReaches} names the reaches every implementation must register. Only one reach may have a given
 * symbol count. When the registry is built, implementations must refuse a second reach with the same count and log
 * an error.</p>
 *
 * @since 0.1.0
 */
public interface AddressReach {

    /** @return how many symbols an address of this reach has */
    int symbolCount();

    /**
     * The address of a gate at {@code target}, dialled from a gate in {@code from}.
     *
     * @param target the base of the gate to reach
     * @param from the world of the dialling gate
     * @return the address, or empty if this reach can't reach {@code target} from {@code from}
     */
    Optional<Address> addressOf(final GlobalPos target, final ResourceKey<Level> from);

    /**
     * Where an address of this reach dialled from {@code from} points.
     *
     * @param address an address
     * @param from the world of the dialling gate
     * @return the chunk it points to, or empty if it points nowhere or doesn't have {@link #symbolCount()} symbols
     */
    Optional<AddressTarget> resolve(final Address address, final ResourceKey<Level> from);
}
