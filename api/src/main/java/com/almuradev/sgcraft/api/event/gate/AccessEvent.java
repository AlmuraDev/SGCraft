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

package com.almuradev.sgcraft.api.event.gate;

import com.almuradev.sgcraft.api.access.AddressRule;
import com.almuradev.sgcraft.api.access.GateRight;
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.event.Cancellable;
import com.almuradev.sgcraft.api.gate.Stargate;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Who and what a gate lets in: answers being given, and the settings behind them changing.
 *
 * @since 0.1.0
 */
public interface AccessEvent extends GateEvent {

    /**
     * Fired each time {@link com.almuradev.sgcraft.api.access.GateAccess} answers whether a player has a right.
     * {@link #allowed()} starts as the implementation's answer, including any platform permission check, and the
     * answer given is its value once every listener has run.
     */
    interface CheckPlayer extends AccessEvent {

        /** @return the player asked about, if there is one; a cause with no player has none */
        Optional<UUID> player();

        /** @return the right asked about */
        GateRight right();

        /** @return whether the right is granted */
        boolean allowed();

        /** @param allowed whether to grant the right */
        void setAllowed(final boolean allowed);
    }

    /**
     * Fired each time {@link com.almuradev.sgcraft.api.access.GateAccess} answers whether {@link #gate()} connects
     * with another gate. {@link #allowed()} starts as the implementation's answer, and the answer given is its
     * value once every listener has run.
     */
    interface CheckConnection extends AccessEvent {

        /** @return the gate at the other end */
        Stargate other();

        /** @return true if {@link #other()} is dialling {@link #gate()}, false if {@link #gate()} is dialling it */
        boolean incoming();

        /** @return whether the connection is allowed */
        boolean allowed();

        /** @param allowed whether to allow the connection */
        void setAllowed(final boolean allowed);
    }

    /**
     * The rule for one address changing, or the gate's defaults for addresses without a rule. A change to
     * {@link com.almuradev.sgcraft.api.gate.GateProperties#ADDRESS_RULES} fires one of these for each address whose
     * rule changes. A change to {@link com.almuradev.sgcraft.api.gate.GateProperties#ALLOW_INCOMING} or
     * {@link com.almuradev.sgcraft.api.gate.GateProperties#ALLOW_OUTGOING} fires one with no {@link #address()},
     * the defaults written as a rule.
     */
    interface ChangeAddressRule extends AccessEvent {

        /** @return the address, or empty when the gate's defaults are changing */
        Optional<Address> address();

        /** @return the rule before the change, or empty if the address had none */
        Optional<AddressRule> oldRule();

        /** @return the rule after the change, or empty if the address's rule is being removed */
        Optional<AddressRule> newRule();

        /**
         * Fired before the change. Cancelling it keeps the old rule; other changes made at the same time still
         * apply.
         */
        interface Pre extends ChangeAddressRule, Cancellable {

            /**
             * @param rule the rule to set instead, or empty to remove the address's rule; for the defaults, empty
             *     restores the properties' default values
             */
            void setNewRule(final Optional<AddressRule> rule);
        }

        /** Fired once the change is made. */
        interface Post extends ChangeAddressRule {
        }
    }

    /**
     * One player's rights changing, or the gate's default rights. A change to
     * {@link com.almuradev.sgcraft.api.gate.GateProperties#PLAYER_RIGHTS} fires one of these for each player whose
     * rights change. A change to {@link com.almuradev.sgcraft.api.gate.GateProperties#DEFAULT_RIGHTS} fires one
     * with no {@link #player()}.
     */
    interface ChangeRights extends AccessEvent {

        /** @return the player, or empty when the gate's default rights are changing */
        Optional<UUID> player();

        /** @return the rights before the change, or empty if the player wasn't listed */
        Optional<Set<GateRight>> oldRights();

        /** @return the rights after the change, or empty if the player is being taken off the list */
        Optional<Set<GateRight>> newRights();

        /**
         * Fired before the change. Cancelling it keeps the old rights; other changes made at the same time still
         * apply.
         */
        interface Pre extends ChangeRights, Cancellable {

            /**
             * @param rights the rights to set instead, or empty to take the player off the list; for the defaults,
             *     empty restores the property's default value
             */
            void setNewRights(final Optional<Set<GateRight>> rights);
        }

        /** Fired once the change is made. */
        interface Post extends ChangeRights {
        }
    }
}
