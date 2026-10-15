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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Turns gate positions into addresses and back, through the registered {@linkplain AddressReach reaches}, and
 * reports how worlds are grouped.
 *
 * <p>Addresses are relative, so each method takes the world the address is dialed from.</p>
 *
 * @since 0.1.0
 */
public interface Addressing {

    /** @return every registered reach, from the fewest symbols to the most */
    List<AddressReach> reaches();

    /**
     * @param symbolCount the length of an address
     * @return the reach of addresses that long, or empty if none is registered
     */
    Optional<AddressReach> reachOf(final int symbolCount);

    /**
     * @param address an address
     * @return the reach that gives its symbols meaning, or empty if none is registered for its length
     */
    Optional<AddressReach> reachOf(final Address address);

    /**
     * The shortest address that reaches a gate at {@code target} from a gate in {@code from}. Implementations must
     * ask the {@linkplain #reaches() reaches} in order, fewest symbols first, and return the first answer.
     *
     * @param target the base of the gate to reach
     * @param from the world of the dialing gate
     * @return the address, or empty if no reach can reach {@code target} from {@code from}
     */
    Optional<Address> addressOf(final GlobalPos target, final ResourceKey<Level> from);

    /**
     * Where an address dialed from {@code from} points, as its {@linkplain #reachOf(Address) reach} reads it.
     *
     * @param address the address
     * @param from the world of the dialing gate
     * @return the chunk it points to, or empty if no reach is registered for its length or it points nowhere
     */
    Optional<AddressTarget> resolve(final Address address, final ResourceKey<Level> from);

    /**
     * @param world a world
     * @return how the world is divided into addresses
     */
    AddressPartition partition(final ResourceKey<Level> world);

    /** @return every group, including the default one */
    Collection<WorldGroup> groups();

    /** @return the group for worlds the server's configuration doesn't assign to a group */
    WorldGroup defaultGroup();

    /**
     * @param world a world
     * @return the group the world is in, which is the default group if the configuration doesn't assign it one
     */
    WorldGroup groupOf(final ResourceKey<Level> world);
}
