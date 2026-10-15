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

package com.almuradev.sgcraft.mod.access;

import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.access.AddressRule;
import com.almuradev.sgcraft.api.access.GateAccess;
import com.almuradev.sgcraft.api.access.GateRight;
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.event.SGEventBus;
import com.almuradev.sgcraft.mod.event.SGEvents;
import com.almuradev.sgcraft.mod.gate.SGStargate;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Who and what a gate lets in, from its properties and its owner. The owner and server operators have every right on
 * the gate and manage it, and a cause with no player in it, such as redstone or the server itself, does too. Every
 * answer about a right goes through the event bus, whose listeners have the final say.
 */
public final class SGGateAccess implements GateAccess {

    private final SGStargate gate;

    public SGGateAccess(final SGStargate gate) {
        this.gate = gate;
    }

    @Override
    public boolean allowsIncoming(final Stargate from) {
        final boolean allowed = this.rule(from).map(AddressRule::incoming)
                .orElseGet(() -> this.gate.get(GateProperties.ALLOW_INCOMING).orElse(true));
        return SGEventBus.get().post(new SGEvents.CheckConnection(Cause.of(this.gate.server()), this.gate, from,
                true, allowed)).allowed();
    }

    @Override
    public boolean allowsOutgoing(final Stargate to) {
        final boolean allowed = this.rule(to).map(AddressRule::outgoing)
                .orElseGet(() -> this.gate.get(GateProperties.ALLOW_OUTGOING).orElse(true));
        return SGEventBus.get().post(new SGEvents.CheckConnection(Cause.of(this.gate.server()), this.gate, to,
                false, allowed)).allowed();
    }

    // The rule whose address, dialed from this gate's world, reaches the other gate
    private Optional<AddressRule> rule(final Stargate other) {
        final Map<Address, AddressRule> rules = this.gate.get(GateProperties.ADDRESS_RULES).orElse(Map.of());
        if (rules.isEmpty()) {
            return Optional.empty();
        }
        for (final Map.Entry<Address, AddressRule> rule : rules.entrySet()) {
            final Optional<Stargate> reached = this.gate.stargates()
                    .byAddress(rule.getKey(), this.gate.position().dimension());
            if (reached.isPresent() && reached.get().position().equals(other.position())) {
                return Optional.of(rule.getValue());
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean allows(final UUID player, final GateRight right) {
        return SGEventBus.get().post(new SGEvents.CheckPlayer(Cause.of(this.gate.server()), this.gate,
                Optional.of(player), right, this.listed(player, right))).allowed();
    }

    @Override
    public boolean allows(final Cause cause, final GateRight right) {
        final Optional<Player> player = cause.first(Player.class);
        final boolean allowed = player.map(p -> SGGateAccess.isOperator(p) || this.listed(p.getUUID(), right))
                .orElse(true);
        return SGEventBus.get().post(new SGEvents.CheckPlayer(cause, this.gate,
                player.map(Player::getUUID), right, allowed)).allowed();
    }

    // Only the owner, an operator, or the server itself with no player in the cause, manage the gate
    @Override
    public boolean mayManage(final Cause cause) {
        return cause.first(Player.class)
                .map(player -> SGGateAccess.isOperator(player) || this.gate.owner().filter(player.getUUID()::equals)
                        .isPresent())
                .orElse(true);
    }

    // Every right for the owner; for anyone else, their own rights if the gate lists them, otherwise its defaults
    private boolean listed(final UUID player, final GateRight right) {
        if (this.gate.owner().filter(player::equals).isPresent()) {
            return true;
        }
        final Set<GateRight> rights = this.gate.get(GateProperties.PLAYER_RIGHTS).orElse(Map.of()).get(player);
        if (rights != null) {
            return rights.contains(right);
        }
        return this.gate.get(GateProperties.DEFAULT_RIGHTS).orElse(Set.of()).contains(right);
    }

    /** @return whether a player is a server operator, who may do anything with any gate */
    public static boolean isOperator(final Player player) {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }
}
