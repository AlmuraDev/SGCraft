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

import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.gate.Stargate;

import java.util.UUID;

/**
 * Answers who and what a gate lets in. The settings behind the answers are the gate's properties:
 * {@link com.almuradev.sgcraft.api.gate.GateProperties#ALLOW_INCOMING},
 * {@link com.almuradev.sgcraft.api.gate.GateProperties#ALLOW_OUTGOING} and
 * {@link com.almuradev.sgcraft.api.gate.GateProperties#ADDRESS_RULES} for other gates, and
 * {@link com.almuradev.sgcraft.api.gate.GateProperties#DEFAULT_RIGHTS} and
 * {@link com.almuradev.sgcraft.api.gate.GateProperties#PLAYER_RIGHTS} for players. Changing those changes the
 * answers.
 *
 * <p>Implementations must check these answers before the actions they cover, and report
 * {@link com.almuradev.sgcraft.api.ActionResult#DENIED} or {@link com.almuradev.sgcraft.api.gate.DialResult#DENIED}
 * when the answer is no. Before answering, they may also consult the platform's own permission system, or let
 * server operators past every check.</p>
 *
 * <p>Every answer fires an {@link com.almuradev.sgcraft.api.event.gate.AccessEvent.CheckPlayer} or
 * {@link com.almuradev.sgcraft.api.event.gate.AccessEvent.CheckConnection}, and listeners have the final say. A
 * change to any of the properties above fires the matching specific {@code AccessEvent} for each entry that
 * changes, before the general {@link com.almuradev.sgcraft.api.event.gate.ChangeGatePropertyEvent}, which then
 * sees the value as those listeners left it.</p>
 *
 * @since 0.1.0
 */
public interface GateAccess {

    /**
     * An address rule applies to {@code from} when the rule's address, dialed from this gate's world, reaches it.
     *
     * @param from a gate that wants to dial this one
     * @return whether this gate accepts the connection: the rule for {@code from} if there is one, otherwise
     *     {@link com.almuradev.sgcraft.api.gate.GateProperties#ALLOW_INCOMING}
     */
    boolean allowsIncoming(final Stargate from);

    /**
     * @param to a gate this one wants to dial
     * @return whether this gate may dial it: the rule for {@code to} if there is one, otherwise
     *     {@link com.almuradev.sgcraft.api.gate.GateProperties#ALLOW_OUTGOING}
     */
    boolean allowsOutgoing(final Stargate to);

    /**
     * @param player a player's UUID
     * @param right a right
     * @return whether the player has the right on this gate: their own rights if the gate lists them, otherwise
     *     {@link com.almuradev.sgcraft.api.gate.GateProperties#DEFAULT_RIGHTS}
     */
    boolean allows(final UUID player, final GateRight right);

    /**
     * Implementations must answer for the most direct player in the cause, as {@link #allows(UUID, GateRight)}
     * does. For a cause with no player in it, such as redstone or the server acting on its own, the answer is up to
     * the implementation.
     *
     * @param cause who wants to act
     * @param right a right
     * @return whether the cause has the right on this gate
     */
    boolean allows(final Cause cause, final GateRight right);
}
