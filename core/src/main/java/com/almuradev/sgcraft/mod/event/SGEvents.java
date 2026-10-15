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

package com.almuradev.sgcraft.mod.event;

import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.access.AddressRule;
import com.almuradev.sgcraft.api.access.GateRight;
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.event.Cancelable;
import com.almuradev.sgcraft.api.event.Event;
import com.almuradev.sgcraft.api.event.gate.AccessEvent;
import com.almuradev.sgcraft.api.event.gate.BreakGateEvent;
import com.almuradev.sgcraft.api.event.gate.ChangeGateOwnerEvent;
import com.almuradev.sgcraft.api.event.gate.ChangeGatePropertyEvent;
import com.almuradev.sgcraft.api.event.gate.ChangeGateStateEvent;
import com.almuradev.sgcraft.api.event.gate.ConnectionEvent;
import com.almuradev.sgcraft.api.event.gate.DialEvent;
import com.almuradev.sgcraft.api.event.gate.FormGateEvent;
import com.almuradev.sgcraft.api.event.gate.IrisEvent;
import com.almuradev.sgcraft.api.event.gate.PlaceGateEvent;
import com.almuradev.sgcraft.api.event.gate.TravelEvent;
import com.almuradev.sgcraft.api.gate.Connection;
import com.almuradev.sgcraft.api.gate.DialResult;
import com.almuradev.sgcraft.api.gate.GatePlacement;
import com.almuradev.sgcraft.api.gate.GateProperty;
import com.almuradev.sgcraft.api.gate.GateSize;
import com.almuradev.sgcraft.api.gate.GateState;
import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.api.gate.Iris;
import com.almuradev.sgcraft.api.gate.IrisState;
import com.almuradev.sgcraft.api.gate.Stargate;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.Entity;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * SGCraft's own objects for the API's events, which it fires on its {@link SGEventBus}.
 */
public final class SGEvents {

    private SGEvents() {
    }

    /** Every event: why it is happening. */
    private abstract static class Base implements Event {

        private final Cause cause;

        Base(final Cause cause) {
            this.cause = Objects.requireNonNull(cause, "cause");
        }

        @Override
        public Cause cause() {
            return this.cause;
        }
    }

    /** An event about one gate. */
    private abstract static class OfGate extends Base {

        private final Stargate gate;

        OfGate(final Cause cause, final Stargate gate) {
            super(cause);
            this.gate = Objects.requireNonNull(gate, "gate");
        }

        public Stargate gate() {
            return this.gate;
        }
    }

    /** An event about one gate that a listener can stop. */
    private abstract static class CancelableOfGate extends OfGate implements Cancelable {

        private boolean canceled;

        CancelableOfGate(final Cause cause, final Stargate gate) {
            super(cause, gate);
        }

        @Override
        public boolean isCanceled() {
            return this.canceled;
        }

        @Override
        public void setCanceled(final boolean canceled) {
            this.canceled = canceled;
        }
    }

    public static final class DialPre extends CancelableOfGate implements DialEvent.Pre {

        private final Address address;
        private final Stargate target;

        public DialPre(final Cause cause, final Stargate gate, final Address address, final Stargate target) {
            super(cause, gate);
            this.address = address;
            this.target = target;
        }

        @Override
        public Address address() {
            return this.address;
        }

        @Override
        public Stargate target() {
            return this.target;
        }
    }

    public static final class DialCost extends OfGate implements DialEvent.Cost {

        private final Address address;
        private final Stargate target;
        private long chevronCost;
        private long openCost;
        private double tickCost;

        public DialCost(final Cause cause, final Stargate gate, final Address address, final Stargate target,
                final long chevronCost, final long openCost, final double tickCost) {
            super(cause, gate);
            this.address = address;
            this.target = target;
            this.chevronCost = chevronCost;
            this.openCost = openCost;
            this.tickCost = tickCost;
        }

        @Override
        public Address address() {
            return this.address;
        }

        @Override
        public Stargate target() {
            return this.target;
        }

        @Override
        public long chevronCost() {
            return this.chevronCost;
        }

        @Override
        public void setChevronCost(final long cost) {
            this.chevronCost = SGEvents.notNegative(cost);
        }

        @Override
        public long openCost() {
            return this.openCost;
        }

        @Override
        public void setOpenCost(final long cost) {
            this.openCost = SGEvents.notNegative(cost);
        }

        @Override
        public double tickCost() {
            return this.tickCost;
        }

        @Override
        public void setTickCost(final double cost) {
            this.tickCost = SGEvents.notNegative(cost);
        }
    }

    public static final class DialLockChevron extends OfGate implements DialEvent.LockChevron {

        private final Address address;
        private final int index;

        public DialLockChevron(final Cause cause, final Stargate gate, final Address address, final int index) {
            super(cause, gate);
            this.address = address;
            this.index = index;
        }

        @Override
        public Address address() {
            return this.address;
        }

        @Override
        public int index() {
            return this.index;
        }
    }

    public static final class DialPost extends OfGate implements DialEvent.Post {

        private final Address address;
        private final DialResult result;
        private final Optional<Stargate> target;

        public DialPost(final Cause cause, final Stargate gate, final Address address, final DialResult result,
                final Optional<Stargate> target) {
            super(cause, gate);
            this.address = address;
            this.result = result;
            this.target = target;
        }

        @Override
        public Address address() {
            return this.address;
        }

        @Override
        public DialResult result() {
            return this.result;
        }

        @Override
        public Optional<Stargate> target() {
            return this.target;
        }
    }

    /** A wormhole event: about a connection rather than one gate. */
    private abstract static class OfConnection extends Base implements ConnectionEvent {

        private final Connection connection;

        OfConnection(final Cause cause, final Connection connection) {
            super(cause);
            this.connection = Objects.requireNonNull(connection, "connection");
        }

        @Override
        public Connection connection() {
            return this.connection;
        }
    }

    public static final class ConnectionOpen extends OfConnection implements ConnectionEvent.Open {

        public ConnectionOpen(final Cause cause, final Connection connection) {
            super(cause, connection);
        }
    }

    public static final class ConnectionDisconnect extends OfConnection implements ConnectionEvent.Disconnect {

        private boolean canceled;

        public ConnectionDisconnect(final Cause cause, final Connection connection) {
            super(cause, connection);
        }

        @Override
        public boolean isCanceled() {
            return this.canceled;
        }

        @Override
        public void setCanceled(final boolean canceled) {
            this.canceled = canceled;
        }
    }

    public static final class ConnectionClose extends OfConnection implements ConnectionEvent.Close {

        public ConnectionClose(final Cause cause, final Connection connection) {
            super(cause, connection);
        }
    }

    public static final class StateChange extends OfGate implements ChangeGateStateEvent {

        private final GateState previous;
        private final GateState current;

        public StateChange(final Cause cause, final Stargate gate, final GateState previous, final GateState current) {
            super(cause, gate);
            this.previous = previous;
            this.current = current;
        }

        @Override
        public GateState previous() {
            return this.previous;
        }

        @Override
        public GateState current() {
            return this.current;
        }
    }

    public static final class TravelPre extends CancelableOfGate implements TravelEvent.Pre {

        private final Entity traveler;
        private Stargate destination;

        public TravelPre(final Cause cause, final Stargate gate, final Entity traveler, final Stargate destination) {
            super(cause, gate);
            this.traveler = traveler;
            this.destination = destination;
        }

        @Override
        public Entity traveler() {
            return this.traveler;
        }

        @Override
        public Stargate destination() {
            return this.destination;
        }

        @Override
        public void setDestination(final Stargate destination) {
            this.destination = Objects.requireNonNull(destination, "destination");
        }
    }

    public static final class TravelPost extends OfGate implements TravelEvent.Post {

        private final Entity traveler;
        private final Stargate destination;

        public TravelPost(final Cause cause, final Stargate gate, final Entity traveler, final Stargate destination) {
            super(cause, gate);
            this.traveler = traveler;
            this.destination = destination;
        }

        @Override
        public Entity traveler() {
            return this.traveler;
        }

        @Override
        public Stargate destination() {
            return this.destination;
        }
    }

    public static final class FormPre extends Base implements FormGateEvent.Pre {

        private final GlobalPos position;
        private final FrontAndTop orientation;
        private final GateType type;
        private final GateSize size;
        private boolean canceled;

        public FormPre(final Cause cause, final GlobalPos position, final FrontAndTop orientation, final GateType type,
                final GateSize size) {
            super(cause);
            this.position = position;
            this.orientation = orientation;
            this.type = type;
            this.size = size;
        }

        @Override
        public GlobalPos position() {
            return this.position;
        }

        @Override
        public FrontAndTop orientation() {
            return this.orientation;
        }

        @Override
        public GateType type() {
            return this.type;
        }

        @Override
        public GateSize size() {
            return this.size;
        }

        @Override
        public boolean isCanceled() {
            return this.canceled;
        }

        @Override
        public void setCanceled(final boolean canceled) {
            this.canceled = canceled;
        }
    }

    public static final class FormPost extends OfGate implements FormGateEvent.Post {

        public FormPost(final Cause cause, final Stargate gate) {
            super(cause, gate);
        }
    }

    public static final class BreakPre extends CancelableOfGate implements BreakGateEvent.Pre {

        public BreakPre(final Cause cause, final Stargate gate) {
            super(cause, gate);
        }
    }

    public static final class BreakPost extends OfGate implements BreakGateEvent.Post {

        public BreakPost(final Cause cause, final Stargate gate) {
            super(cause, gate);
        }
    }

    public static final class OwnerPre extends CancelableOfGate implements ChangeGateOwnerEvent.Pre {

        private final Optional<UUID> oldOwner;
        private final Optional<UUID> newOwner;

        public OwnerPre(final Cause cause, final Stargate gate, final Optional<UUID> oldOwner,
                final Optional<UUID> newOwner) {
            super(cause, gate);
            this.oldOwner = oldOwner;
            this.newOwner = newOwner;
        }

        @Override
        public Optional<UUID> oldOwner() {
            return this.oldOwner;
        }

        @Override
        public Optional<UUID> newOwner() {
            return this.newOwner;
        }
    }

    public static final class OwnerPost extends OfGate implements ChangeGateOwnerEvent.Post {

        private final Optional<UUID> oldOwner;
        private final Optional<UUID> newOwner;

        public OwnerPost(final Cause cause, final Stargate gate, final Optional<UUID> oldOwner,
                final Optional<UUID> newOwner) {
            super(cause, gate);
            this.oldOwner = oldOwner;
            this.newOwner = newOwner;
        }

        @Override
        public Optional<UUID> oldOwner() {
            return this.oldOwner;
        }

        @Override
        public Optional<UUID> newOwner() {
            return this.newOwner;
        }
    }

    public static final class PlacePre extends Base implements PlaceGateEvent.Pre {

        private GatePlacement placement;
        private boolean canceled;

        public PlacePre(final Cause cause, final GatePlacement placement) {
            super(cause);
            this.placement = placement;
        }

        @Override
        public GatePlacement placement() {
            return this.placement;
        }

        @Override
        public void setPlacement(final GatePlacement placement) {
            this.placement = Objects.requireNonNull(placement, "placement");
        }

        @Override
        public boolean isCanceled() {
            return this.canceled;
        }

        @Override
        public void setCanceled(final boolean canceled) {
            this.canceled = canceled;
        }
    }

    public static final class PlacePost extends OfGate implements PlaceGateEvent.Post {

        private final GatePlacement placement;

        public PlacePost(final Cause cause, final Stargate gate, final GatePlacement placement) {
            super(cause, gate);
            this.placement = placement;
        }

        @Override
        public GatePlacement placement() {
            return this.placement;
        }
    }

    public static final class IrisPre extends CancelableOfGate implements IrisEvent.Pre {

        private final Iris iris;
        private final IrisState target;

        public IrisPre(final Cause cause, final Stargate gate, final Iris iris, final IrisState target) {
            super(cause, gate);
            this.iris = iris;
            this.target = target;
        }

        @Override
        public Iris iris() {
            return this.iris;
        }

        @Override
        public IrisState target() {
            return this.target;
        }
    }

    public static final class IrisPost extends OfGate implements IrisEvent.Post {

        private final Iris iris;
        private final IrisState target;

        public IrisPost(final Cause cause, final Stargate gate, final Iris iris, final IrisState target) {
            super(cause, gate);
            this.iris = iris;
            this.target = target;
        }

        @Override
        public Iris iris() {
            return this.iris;
        }

        @Override
        public IrisState target() {
            return this.target;
        }
    }

    public static final class PropertyPre extends CancelableOfGate implements ChangeGatePropertyEvent.Pre {

        private final GateProperty<?> property;
        private final Optional<?> oldValue;
        private Optional<?> newValue;

        public PropertyPre(final Cause cause, final Stargate gate, final GateProperty<?> property,
                final Optional<?> oldValue, final Optional<?> newValue) {
            super(cause, gate);
            this.property = property;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        @Override
        public GateProperty<?> property() {
            return this.property;
        }

        @Override
        public <T> Optional<T> oldValue(final GateProperty<T> property) {
            return SGEvents.as(this.property, property, this.oldValue);
        }

        @Override
        public <T> Optional<T> newValue(final GateProperty<T> property) {
            return SGEvents.as(this.property, property, this.newValue);
        }

        /** @return the value to set, as the listeners left it, or empty to remove it */
        public Optional<?> newValue() {
            return this.newValue;
        }

        @Override
        public <T> boolean setNewValue(final GateProperty<T> property, final Optional<T> value) {
            if (property != this.property) {
                return false;
            }
            this.newValue = Objects.requireNonNull(value, "value");
            return true;
        }
    }

    public static final class PropertyPost extends OfGate implements ChangeGatePropertyEvent.Post {

        private final GateProperty<?> property;
        private final Optional<?> oldValue;
        private final Optional<?> newValue;

        public PropertyPost(final Cause cause, final Stargate gate, final GateProperty<?> property,
                final Optional<?> oldValue, final Optional<?> newValue) {
            super(cause, gate);
            this.property = property;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        @Override
        public GateProperty<?> property() {
            return this.property;
        }

        @Override
        public <T> Optional<T> oldValue(final GateProperty<T> property) {
            return SGEvents.as(this.property, property, this.oldValue);
        }

        @Override
        public <T> Optional<T> newValue(final GateProperty<T> property) {
            return SGEvents.as(this.property, property, this.newValue);
        }
    }

    public static final class CheckPlayer extends OfGate implements AccessEvent.CheckPlayer {

        private final Optional<UUID> player;
        private final GateRight right;
        private boolean allowed;

        public CheckPlayer(final Cause cause, final Stargate gate, final Optional<UUID> player, final GateRight right,
                final boolean allowed) {
            super(cause, gate);
            this.player = player;
            this.right = right;
            this.allowed = allowed;
        }

        @Override
        public Optional<UUID> player() {
            return this.player;
        }

        @Override
        public GateRight right() {
            return this.right;
        }

        @Override
        public boolean allowed() {
            return this.allowed;
        }

        @Override
        public void setAllowed(final boolean allowed) {
            this.allowed = allowed;
        }
    }

    public static final class CheckConnection extends OfGate implements AccessEvent.CheckConnection {

        private final Stargate other;
        private final boolean incoming;
        private boolean allowed;

        public CheckConnection(final Cause cause, final Stargate gate, final Stargate other, final boolean incoming,
                final boolean allowed) {
            super(cause, gate);
            this.other = other;
            this.incoming = incoming;
            this.allowed = allowed;
        }

        @Override
        public Stargate other() {
            return this.other;
        }

        @Override
        public boolean incoming() {
            return this.incoming;
        }

        @Override
        public boolean allowed() {
            return this.allowed;
        }

        @Override
        public void setAllowed(final boolean allowed) {
            this.allowed = allowed;
        }
    }

    public static final class AddressRulePre extends CancelableOfGate implements AccessEvent.ChangeAddressRule.Pre {

        private final Optional<Address> address;
        private final Optional<AddressRule> oldRule;
        private Optional<AddressRule> newRule;

        public AddressRulePre(final Cause cause, final Stargate gate, final Optional<Address> address,
                final Optional<AddressRule> oldRule, final Optional<AddressRule> newRule) {
            super(cause, gate);
            this.address = address;
            this.oldRule = oldRule;
            this.newRule = newRule;
        }

        @Override
        public Optional<Address> address() {
            return this.address;
        }

        @Override
        public Optional<AddressRule> oldRule() {
            return this.oldRule;
        }

        @Override
        public Optional<AddressRule> newRule() {
            return this.newRule;
        }

        @Override
        public void setNewRule(final Optional<AddressRule> rule) {
            this.newRule = Objects.requireNonNull(rule, "rule");
        }
    }

    public static final class AddressRulePost extends OfGate implements AccessEvent.ChangeAddressRule.Post {

        private final Optional<Address> address;
        private final Optional<AddressRule> oldRule;
        private final Optional<AddressRule> newRule;

        public AddressRulePost(final Cause cause, final Stargate gate, final Optional<Address> address,
                final Optional<AddressRule> oldRule, final Optional<AddressRule> newRule) {
            super(cause, gate);
            this.address = address;
            this.oldRule = oldRule;
            this.newRule = newRule;
        }

        @Override
        public Optional<Address> address() {
            return this.address;
        }

        @Override
        public Optional<AddressRule> oldRule() {
            return this.oldRule;
        }

        @Override
        public Optional<AddressRule> newRule() {
            return this.newRule;
        }
    }

    public static final class RightsPre extends CancelableOfGate implements AccessEvent.ChangeRights.Pre {

        private final Optional<UUID> player;
        private final Optional<Set<GateRight>> oldRights;
        private Optional<Set<GateRight>> newRights;

        public RightsPre(final Cause cause, final Stargate gate, final Optional<UUID> player,
                final Optional<Set<GateRight>> oldRights, final Optional<Set<GateRight>> newRights) {
            super(cause, gate);
            this.player = player;
            this.oldRights = oldRights;
            this.newRights = newRights;
        }

        @Override
        public Optional<UUID> player() {
            return this.player;
        }

        @Override
        public Optional<Set<GateRight>> oldRights() {
            return this.oldRights;
        }

        @Override
        public Optional<Set<GateRight>> newRights() {
            return this.newRights;
        }

        @Override
        public void setNewRights(final Optional<Set<GateRight>> rights) {
            this.newRights = Objects.requireNonNull(rights, "rights").map(Set::copyOf);
        }
    }

    public static final class RightsPost extends OfGate implements AccessEvent.ChangeRights.Post {

        private final Optional<UUID> player;
        private final Optional<Set<GateRight>> oldRights;
        private final Optional<Set<GateRight>> newRights;

        public RightsPost(final Cause cause, final Stargate gate, final Optional<UUID> player,
                final Optional<Set<GateRight>> oldRights, final Optional<Set<GateRight>> newRights) {
            super(cause, gate);
            this.player = player;
            this.oldRights = oldRights;
            this.newRights = newRights;
        }

        @Override
        public Optional<UUID> player() {
            return this.player;
        }

        @Override
        public Optional<Set<GateRight>> oldRights() {
            return this.oldRights;
        }

        @Override
        public Optional<Set<GateRight>> newRights() {
            return this.newRights;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Optional<T> as(final GateProperty<?> mine, final GateProperty<T> asked,
            final Optional<?> value) {
        return mine == asked ? (Optional<T>) value : Optional.empty();
    }

    private static double notNegative(final double value) {
        if (!(value >= 0)) {
            throw new IllegalArgumentException("A cost can't be negative: " + value);
        }
        return value;
    }

    private static long notNegative(final long value) {
        if (value < 0) {
            throw new IllegalArgumentException("A cost can't be negative: " + value);
        }
        return value;
    }
}
