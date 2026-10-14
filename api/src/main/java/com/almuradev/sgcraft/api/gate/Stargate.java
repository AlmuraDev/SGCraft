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

import com.almuradev.sgcraft.api.ActionResult;
import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.access.GateAccess;
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.energy.GateEnergy;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * A formed gate.
 *
 * <p>A {@code Stargate} is a view of a gate in the world. Callers may keep one, but the gate it shows can be broken
 * at any time. After that, {@link #exists()} returns false, the getters return the gate's last values, and actions
 * do nothing and report that the gate is gone.</p>
 *
 * @since 0.1.0
 */
public interface Stargate {

    /** @return the gate's base block, which its address points to */
    GlobalPos position();

    /**
     * Which way the gate's front and the top of its ring point. A gate standing upright has its top
     * {@linkplain net.minecraft.core.Direction#UP up} and faces one of the four horizontal directions. A gate lying
     * flat faces up or down, and the top of its ring points one of the four horizontal directions.
     *
     * @return the gate's orientation
     */
    FrontAndTop orientation();

    /** @return the gate's type */
    Holder<GateType> type();

    /** @return the size of the gate's ring */
    GateSize size();

    /** @return how many chevrons the gate has, which limits how many symbols it can dial */
    int chevrons();

    /** @return whether the gate is still formed */
    boolean exists();

    /** @return what the gate is doing */
    GateState state();

    /** @return the gate's connection while it dials, is dialled, or is open */
    Optional<Connection> connection();

    /** @return the gate's iris, present while its {@link GateProperties#IRIS} property is set */
    Optional<Iris> iris();

    /** @return the gate's energy store */
    GateEnergy energy();

    /** @return who and what the gate lets in */
    GateAccess access();

    /**
     * @param property a property
     * @return the gate's value of it; the property's default if the gate has never had a value set; or empty if
     *     there is neither, or the property isn't registered
     */
    <T> Optional<T> get(final GateProperty<T> property);

    /**
     * Dials an address. Dialling takes many ticks, so the result arrives later.
     *
     * <p>The returned stage completes on the server thread, with {@link DialResult#CONNECTED} once the wormhole is
     * open, or with the reason dialling failed. A dial refused at the start, such as one to a gate that is
     * {@linkplain DialResult#BUSY busy}, returns a stage that is already complete.</p>
     *
     * @param address the address to dial
     * @param cause why the gate is dialling
     * @return the result, once known
     */
    default CompletionStage<DialResult> dial(final Address address, final Cause cause) {
        return this.dial(address, DialMode.STANDARD, cause);
    }

    /**
     * Dials an address in the given mode. Otherwise this is the same as {@link #dial(Address, Cause)}.
     *
     * @param address the address to dial
     * @param mode how to dial it
     * @param cause why the gate is dialling
     * @return the result, once known
     */
    CompletionStage<DialResult> dial(final Address address, final DialMode mode, final Cause cause);

    /**
     * Dials the gate {@link Stargates#mostAppropriate(GlobalPos, Cause)} chooses for {@code target}, using the
     * shortest address that reaches it from this gate. Otherwise this is the same as {@link #dial(Address, Cause)}.
     *
     * @param target where to dial
     * @param cause why the gate is dialling
     * @return the result, once known; {@link DialResult#NO_GATE} if there is no gate to choose, or
     *     {@link DialResult#UNREACHABLE} if no address reaches it from here
     */
    CompletionStage<DialResult> dial(final GlobalPos target, final Cause cause);

    /**
     * Stops dialling, or closes the wormhole, from either end. A dial in progress completes with
     * {@link DialResult#INTERRUPTED}.
     *
     * @param cause why the gate is disconnecting
     * @return {@link ActionResult#NO_CHANGE} if the gate has no connection, or {@link ActionResult#NOT_POSSIBLE}
     *     if the gate no longer exists
     */
    ActionResult disconnect(final Cause cause);

    /**
     * @param property a property
     * @param value its new value
     * @param cause who is changing it
     * @return {@link ActionResult#NO_CHANGE} if the gate already has that value, or
     *     {@link ActionResult#NOT_POSSIBLE} if the property isn't registered or the gate no longer exists
     */
    <T> ActionResult set(final GateProperty<T> property, final T value, final Cause cause);

    /**
     * Clears the gate's own value of a property, so that it has the property's default again, or no value if there
     * is no default.
     *
     * @param property a property
     * @param cause who is clearing it
     * @return {@link ActionResult#NO_CHANGE} if the gate has no value of its own, or
     *     {@link ActionResult#NOT_POSSIBLE} if the property isn't registered or the gate no longer exists
     */
    ActionResult remove(final GateProperty<?> property, final Cause cause);

    /**
     * Changes the gate's type.
     *
     * @param type the new type
     * @param cause who is changing it
     * @return {@link ActionResult#NOT_POSSIBLE} if the new type doesn't support the gate's size, the gate isn't
     *     {@linkplain GateState#IDLE idle}, or the gate no longer exists
     */
    ActionResult setType(final GateType type, final Cause cause);

    /**
     * The same as {@link #setType(GateType, Cause)}, for a type still in its registry holder.
     *
     * @param type the new type
     * @param cause who is changing it
     * @return the same as {@link #setType(GateType, Cause)}
     */
    ActionResult setType(final Holder<GateType> type, final Cause cause);

    /**
     * Changes how many chevrons the gate has.
     *
     * @param chevrons the new count; at least 1
     * @param cause who is changing it
     * @return {@link ActionResult#NOT_POSSIBLE} if {@code chevrons} is below 1, the gate isn't idle, or the gate no
     *     longer exists
     */
    ActionResult setChevrons(final int chevrons, final Cause cause);

    /**
     * Builds blocks around this gate, such as a DHD for a gate placed without one.
     *
     * @param fixtures the blocks to build, laid out as {@link FixtureBuffer} describes
     * @param mode what to do about blocks already in the way; with {@link PlacementMode#ATTEMPT}, nothing is built
     *     unless every spot is clear
     * @param cause who is building them
     * @return {@link ActionResult#NOT_POSSIBLE} if blocks are in the way and the mode is
     *     {@link PlacementMode#ATTEMPT}, or the gate no longer exists
     */
    ActionResult placeFixtures(final FixtureBuffer fixtures, final PlacementMode mode, final Cause cause);
}
