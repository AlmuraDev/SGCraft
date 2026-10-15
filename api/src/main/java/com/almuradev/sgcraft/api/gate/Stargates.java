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

import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.address.Address;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Optional;

/**
 * Finds and places gates. Every method needs a running server. Without one, lookups return nothing and placement
 * fails with {@link PlacementResult.Status#INVALID_POSITION}.
 *
 * @since 0.1.0
 */
public interface Stargates {

    /**
     * @param position any block
     * @return the gate that block is part of, if any
     */
    Optional<Stargate> at(final GlobalPos position);

    /**
     * @param address an address
     * @param from the world it is dialed from
     * @return the gate the address reaches from that world, if there is one
     */
    Optional<Stargate> byAddress(final Address address, final ResourceKey<Level> from);

    /**
     * @param world a world
     * @return every gate in that world, including gates in chunks that aren't loaded
     */
    Collection<Stargate> in(final ResourceKey<Level> world);

    /**
     * The gate best suited to {@code position}, such as the one to dial from or send a traveler to. How the gate is
     * chosen is up to the implementation.
     *
     * @param position a position
     * @return the gate, or empty if there is none to choose
     */
    Optional<Stargate> mostAppropriate(final GlobalPos position);

    /**
     * The gate best suited to {@code position} for {@code cause}. Implementations may use the cause to leave out
     * gates it may not use.
     *
     * @param position a position
     * @param cause who the gate is for
     * @return the gate, or empty if there is none to choose
     */
    Optional<Stargate> mostAppropriate(final GlobalPos position, final Cause cause);

    /**
     * Builds a formed gate in the world, without a player building it piece by piece. Implementations must load the
     * chunks the gate needs.
     *
     * @param placement what to build, and where
     * @param cause who is placing it
     * @return the outcome, with the new gate if it was placed
     */
    PlacementResult place(final GatePlacement placement, final Cause cause);
}
