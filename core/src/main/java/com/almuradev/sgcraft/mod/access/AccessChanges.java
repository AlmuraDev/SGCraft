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
import com.almuradev.sgcraft.api.access.GateRight;
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.GateProperty;
import com.almuradev.sgcraft.mod.event.SGEventBus;
import com.almuradev.sgcraft.mod.event.SGEvents;
import com.almuradev.sgcraft.mod.gate.SGStargate;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Fires the access events for a change to one of the access properties, one for each entry that changes, and works
 * out the value the property gets once their listeners have had their say. Their after events wait in a list until the
 * change is made.
 */
public final class AccessChanges {

    private AccessChanges() {
    }

    /**
     * @param old the property's value now
     * @param value the value asked for, or empty to remove the gate's own value
     * @param posts where to put the after events, to fire once the change is made
     * @return the value to set, or empty to remove the gate's own value
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> adjust(final SGStargate gate, final GateProperty<T> property, final Optional<T> old,
            final Optional<T> value, final Cause cause, final List<Runnable> posts) {
        if (property == GateProperties.ADDRESS_RULES) {
            return (Optional<T>) AccessChanges.rules(gate, (Optional<Map<Address, AddressRule>>) old,
                    (Optional<Map<Address, AddressRule>>) value, cause, posts);
        }
        if (property == GateProperties.ALLOW_INCOMING || property == GateProperties.ALLOW_OUTGOING) {
            return (Optional<T>) AccessChanges.defaults(gate, (GateProperty<Boolean>) property,
                    (Optional<Boolean>) value, cause, posts);
        }
        if (property == GateProperties.PLAYER_RIGHTS) {
            return (Optional<T>) AccessChanges.players(gate, (Optional<Map<UUID, Set<GateRight>>>) old,
                    (Optional<Map<UUID, Set<GateRight>>>) value, cause, posts);
        }
        if (property == GateProperties.DEFAULT_RIGHTS) {
            return (Optional<T>) AccessChanges.defaultRights(gate, (Optional<Set<GateRight>>) old,
                    (Optional<Set<GateRight>>) value, cause, posts);
        }
        return value;
    }

    private static Optional<Map<Address, AddressRule>> rules(final SGStargate gate,
            final Optional<Map<Address, AddressRule>> old, final Optional<Map<Address, AddressRule>> value,
            final Cause cause, final List<Runnable> posts) {
        final Map<Address, AddressRule> before = old.orElse(Map.of());
        final Map<Address, AddressRule> after = value.orElseGet(() -> gate.stargates()
                .defaultOf(GateProperties.ADDRESS_RULES).orElse(Map.of()));
        final Map<Address, AddressRule> result = new LinkedHashMap<>(after);
        final Set<Address> addresses = new HashSet<>(before.keySet());
        addresses.addAll(after.keySet());
        for (final Address address : addresses) {
            final Optional<AddressRule> was = Optional.ofNullable(before.get(address));
            final Optional<AddressRule> will = Optional.ofNullable(after.get(address));
            if (was.equals(will)) {
                continue;
            }
            final SGEvents.AddressRulePre pre = SGEventBus.get().post(new SGEvents.AddressRulePre(cause, gate,
                    Optional.of(address), was, will));
            final Optional<AddressRule> settled = pre.isCanceled() ? was : pre.newRule();
            settled.ifPresentOrElse(rule -> result.put(address, rule), () -> result.remove(address));
            if (!settled.equals(was)) {
                posts.add(() -> SGEventBus.get().post(new SGEvents.AddressRulePost(cause, gate, Optional.of(address),
                        was, settled)));
            }
        }
        return value.isEmpty() && result.equals(after) ? Optional.empty() : Optional.of(Map.copyOf(result));
    }

    // The gate's defaults for addresses with no rule, written as a rule
    private static Optional<Boolean> defaults(final SGStargate gate, final GateProperty<Boolean> property,
            final Optional<Boolean> value, final Cause cause, final List<Runnable> posts) {
        final boolean incoming = property == GateProperties.ALLOW_INCOMING;
        final AddressRule was = new AddressRule(gate.get(GateProperties.ALLOW_INCOMING).orElse(true),
                gate.get(GateProperties.ALLOW_OUTGOING).orElse(true));
        final boolean wanted = value.orElseGet(() -> gate.stargates().defaultOf(property).orElse(true));
        final AddressRule will = incoming ? new AddressRule(wanted, was.outgoing())
                : new AddressRule(was.incoming(), wanted);
        final SGEvents.AddressRulePre pre = SGEventBus.get().post(new SGEvents.AddressRulePre(cause, gate,
                Optional.empty(), Optional.of(was), Optional.of(will)));
        if (pre.isCanceled()) {
            return Optional.of(incoming ? was.incoming() : was.outgoing());
        }
        if (pre.newRule().isEmpty()) {
            // Empty restores the defaults
            posts.add(() -> SGEventBus.get().post(new SGEvents.AddressRulePost(cause, gate, Optional.empty(),
                    Optional.of(was), Optional.empty())));
            return Optional.empty();
        }
        final AddressRule settled = pre.newRule().get();
        posts.add(() -> SGEventBus.get().post(new SGEvents.AddressRulePost(cause, gate, Optional.empty(),
                Optional.of(was), Optional.of(settled))));
        return Optional.of(incoming ? settled.incoming() : settled.outgoing());
    }

    private static Optional<Map<UUID, Set<GateRight>>> players(final SGStargate gate,
            final Optional<Map<UUID, Set<GateRight>>> old, final Optional<Map<UUID, Set<GateRight>>> value,
            final Cause cause, final List<Runnable> posts) {
        final Map<UUID, Set<GateRight>> before = old.orElse(Map.of());
        final Map<UUID, Set<GateRight>> after = value.orElse(Map.of());
        final Map<UUID, Set<GateRight>> result = new LinkedHashMap<>(after);
        final Set<UUID> players = new HashSet<>(before.keySet());
        players.addAll(after.keySet());
        for (final UUID player : players) {
            final Optional<Set<GateRight>> was = Optional.ofNullable(before.get(player));
            final Optional<Set<GateRight>> will = Optional.ofNullable(after.get(player));
            if (Objects.equals(was, will)) {
                continue;
            }
            final SGEvents.RightsPre pre = SGEventBus.get().post(new SGEvents.RightsPre(cause, gate,
                    Optional.of(player), was, will));
            final Optional<Set<GateRight>> settled = pre.isCanceled() ? was : pre.newRights();
            settled.ifPresentOrElse(rights -> result.put(player, rights), () -> result.remove(player));
            if (!settled.equals(was)) {
                posts.add(() -> SGEventBus.get().post(new SGEvents.RightsPost(cause, gate, Optional.of(player),
                        was, settled)));
            }
        }
        return value.isEmpty() && result.equals(after) ? Optional.empty() : Optional.of(Map.copyOf(result));
    }

    private static Optional<Set<GateRight>> defaultRights(final SGStargate gate, final Optional<Set<GateRight>> old,
            final Optional<Set<GateRight>> value, final Cause cause, final List<Runnable> posts) {
        final Optional<Set<GateRight>> will = Optional.of(value.orElseGet(() ->
                gate.stargates().defaultOf(GateProperties.DEFAULT_RIGHTS).orElse(Set.of())));
        final SGEvents.RightsPre pre = SGEventBus.get().post(new SGEvents.RightsPre(cause, gate, Optional.empty(),
                old, will));
        if (pre.isCanceled()) {
            return old;
        }
        final Optional<Set<GateRight>> settled = pre.newRights();
        posts.add(() -> SGEventBus.get().post(new SGEvents.RightsPost(cause, gate, Optional.empty(), old, settled)));
        // Empty restores the default
        return settled.isEmpty() ? Optional.empty() : settled;
    }
}
