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

package com.almuradev.sgcraft.mod.gate;

import com.almuradev.sgcraft.api.ActionResult;
import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.SGCraftRegistries;
import com.almuradev.sgcraft.api.access.GateAccess;
import com.almuradev.sgcraft.api.access.GateRight;
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.address.AddressTarget;
import com.almuradev.sgcraft.api.address.Symbol;
import com.almuradev.sgcraft.api.gate.Connection;
import com.almuradev.sgcraft.api.gate.DialMode;
import com.almuradev.sgcraft.api.gate.DialResult;
import com.almuradev.sgcraft.api.gate.FixtureBuffer;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.GateProperty;
import com.almuradev.sgcraft.api.gate.GateSize;
import com.almuradev.sgcraft.api.gate.GateState;
import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.api.gate.GateTypes;
import com.almuradev.sgcraft.api.gate.Iris;
import com.almuradev.sgcraft.api.gate.PlacementMode;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.access.AccessChanges;
import com.almuradev.sgcraft.mod.access.SGGateAccess;
import com.almuradev.sgcraft.mod.block.entity.StargateBaseBlockEntity;
import com.almuradev.sgcraft.mod.energy.DialCosts;
import com.almuradev.sgcraft.mod.energy.SGGateEnergy;
import com.almuradev.sgcraft.mod.event.SGEventBus;
import com.almuradev.sgcraft.mod.event.SGEvents;
import com.almuradev.sgcraft.mod.gate.GateStore.GateRecord;
import com.almuradev.sgcraft.mod.sound.SGSounds;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * A formed gate on a running server. Its lasting details live in the {@link GateStore}; what it is doing lives here,
 * and is copied to its base's block entity whenever the base is loaded, for the client to draw.
 *
 * <p>Every action checks the cause's rights first, through {@link #access()}, and fires its events on the
 * {@link SGEventBus}.</p>
 */
public final class SGStargate implements Stargate {

    // How many symbols the shortest address has, and how long symbols locked one at a time stay locked with no more
    private static final int SHORTEST_ADDRESS = 7;
    private static final int ENGAGE_TIMEOUT_TICKS = 600;
    // Whether the iris came with the gate as it formed, in what the gate holds between restarts
    private static final String IRIS_INCLUDED = "iris_included";
    // The properties that decide who may do what with the gate, which only those who manage it change
    private static final Set<GateProperty<?>> MANAGED = Set.of(GateProperties.DEFAULT_RIGHTS,
            GateProperties.PLAYER_RIGHTS, GateProperties.BREAKABLE, GateProperties.GENERATED);

    private final SGStargates stargates;
    private final GlobalPos position;
    private final SGGateAccess access = new SGGateAccess(this);
    private final SGIris iris = new SGIris(this);
    private final SGGateEnergy energy = new SGGateEnergy(this);
    private DialView view = DialView.IDLE;
    private @Nullable SGConnection connection;
    // Symbols locked one at a time, as a DHD does, and when the last was
    private final List<Symbol> engaged = new ArrayList<>();
    private long lastEngaged;

    SGStargate(final SGStargates stargates, final GlobalPos position) {
        this.stargates = stargates;
        this.position = position;
    }

    private Optional<GateRecord> record() {
        return this.stargates.store().gate(this.position);
    }

    public SGStargates stargates() {
        return this.stargates;
    }

    public MinecraftServer server() {
        return this.stargates.server();
    }

    public GateStore store() {
        return this.stargates.store();
    }

    /** @return what the gate holds between restarts, such as its iris and energy; a copy */
    public CompoundTag saved() {
        return this.record().map(GateRecord::state).orElseGet(CompoundTag::new);
    }

    public void setSaved(final CompoundTag state) {
        this.record().ifPresent(record -> this.store().put(record.withState(state)));
    }

    /** @return the gate's world, if it is loaded */
    @Nullable ServerLevel level() {
        return this.stargates.server().getLevel(this.position.dimension());
    }

    /** @return the gate's own frame, which its horizon and travel are measured in */
    public GateFrame frame() {
        return new GateFrame(this.position.pos(), this.facing(), this.lying(), this.shape());
    }

    /** @return which way the gate's base faces, which is the gate's front only while it stands */
    public Direction facing() {
        return this.record().map(GateRecord::facing).orElse(Direction.NORTH);
    }

    /** @return whether the gate stands or lies flat */
    public GateOrientation lying() {
        return this.record().map(GateRecord::orientation).orElse(GateOrientation.STANDING);
    }

    /** @return the key of the gate's type */
    public ResourceKey<GateType> typeKey() {
        return this.record().map(GateRecord::type).orElse(GateTypes.MILKY_WAY);
    }

    /** @return the game time, which the gate's timings are counted in */
    long now() {
        return this.stargates.server().overworld().getGameTime();
    }

    @Override
    public GlobalPos position() {
        return this.position;
    }

    @Override
    public FrontAndTop orientation() {
        return this.lying().frontAndTop(this.facing());
    }

    @Override
    public Holder<GateType> type() {
        final Registry<GateType> types = this.stargates.server().registryAccess()
                .lookupOrThrow(SGCraftRegistries.GATE_TYPE);
        return this.record().flatMap(gate -> types.get(gate.type()))
                .map(holder -> (Holder<GateType>) holder)
                .orElseGet(() -> types.listElements().findFirst().orElseThrow());
    }

    @Override
    public GateSize size() {
        return this.shape().size();
    }

    /** @return how big the gate is and how it is laid out */
    public GateShape shape() {
        return this.record().map(GateRecord::shape).orElse(GateShape.STANDARD);
    }

    @Override
    public int chevrons() {
        return this.record().map(GateRecord::chevrons).orElse(0);
    }

    @Override
    public boolean exists() {
        return this.record().isPresent();
    }

    @Override
    public GateState state() {
        return SGStargate.stateOf(this.view);
    }

    private static GateState stateOf(final DialView view) {
        return switch (view.phase()) {
            case IDLE -> GateState.IDLE;
            case DIALING, INTER_DIALING, SYNC_AWAIT -> view.initiator() ? GateState.DIALING : GateState.INCOMING;
            case ESTABLISHING -> GateState.OPENING;
            case TRANSIENT, CONNECTED -> GateState.CONNECTED;
            case DISCONNECTING -> GateState.CLOSING;
        };
    }

    /** @return what the gate is doing, as its base shows it */
    public DialView view() {
        return this.view;
    }

    // Shows what the gate is doing, and tells listeners if that changes its state
    private void show(final DialView view, final Cause cause) {
        final GateState before = this.state();
        this.view = view;
        this.sync();
        final GateState after = this.state();
        if (before != after) {
            SGEventBus.get().post(new SGEvents.StateChange(cause, this, before, after));
        }
    }

    @Override
    public Optional<Connection> connection() {
        return Optional.ofNullable(this.connection);
    }

    /** @return the connection the gate is part of, if any */
    public Optional<SGConnection> liveConnection() {
        return Optional.ofNullable(this.connection);
    }

    @Override
    public Optional<Iris> iris() {
        return this.get(GateProperties.IRIS).isPresent() ? Optional.of(this.iris) : Optional.empty();
    }

    /** @return the gate's iris, whether or not one is fitted */
    public SGIris irisState() {
        return this.iris;
    }

    /** @return whether the gate has an iris fitted that stops travelers now */
    public boolean irisBlocks() {
        return this.get(GateProperties.IRIS).isPresent() && this.iris.blocks();
    }

    @Override
    public SGGateEnergy energy() {
        return this.energy;
    }

    @Override
    public GateAccess access() {
        return this.access;
    }

    @Override
    public <T> Optional<T> get(final GateProperty<T> property) {
        return this.stargates.property(this.position, property);
    }

    @Override
    public Optional<UUID> owner() {
        return this.stargates.owner(this.position);
    }

    @Override
    public ActionResult setOwner(final Optional<UUID> owner, final Cause cause) {
        if (!this.exists()) {
            return ActionResult.NOT_POSSIBLE;
        }
        final Optional<UUID> old = this.owner();
        if (old.equals(owner)) {
            return ActionResult.NO_CHANGE;
        }
        if (!this.access.mayManage(cause)) {
            return ActionResult.DENIED;
        }
        if (!SGEventBus.get().allows(new SGEvents.OwnerPre(cause, this, old, owner))) {
            return ActionResult.CANCELED;
        }
        this.stargates.setOwner(this.position, owner);
        SGEventBus.get().post(new SGEvents.OwnerPost(cause, this, old, owner));
        return ActionResult.DONE;
    }

    /** @return whether the gate's iris came with it as it formed, so taking it off gives no iris upgrade back */
    public boolean irisIncluded() {
        return this.saved().getBooleanOr(SGStargate.IRIS_INCLUDED, false);
    }

    void includeIris(final boolean included) {
        final CompoundTag state = this.saved();
        if (included) {
            state.putBoolean(SGStargate.IRIS_INCLUDED, true);
        } else {
            state.remove(SGStargate.IRIS_INCLUDED);
        }
        this.setSaved(state);
    }

    @Override
    public CompletionStage<DialResult> dial(final Address address, final DialMode mode, final Cause cause) {
        if (!this.exists()) {
            return CompletableFuture.completedFuture(DialResult.GATE_GONE);
        }
        if (this.state() != GateState.IDLE) {
            return CompletableFuture.completedFuture(DialResult.BUSY);
        }
        if (!this.access.allows(cause, GateRight.USE)) {
            return this.refused(address, cause, DialResult.DENIED, null);
        }
        if (this.stargates.addressing().reachOf(address).isEmpty()) {
            return this.refused(address, cause, DialResult.INVALID_ADDRESS, null);
        }
        if (address.size() > this.chevrons()) {
            return this.refused(address, cause, DialResult.NOT_ENOUGH_CHEVRONS, null);
        }
        final Optional<AddressTarget> target = this.stargates.addressing().resolve(address, this.position.dimension());
        final Optional<SGStargate> gate = target.flatMap(this.stargates::inCell);
        if (gate.isEmpty()) {
            return this.refused(address, cause, DialResult.NO_GATE, null);
        }
        if (gate.get() == this) {
            return this.refused(address, cause, DialResult.SAME_GATE, gate.get());
        }
        if (gate.get().state() != GateState.IDLE) {
            return this.refused(address, cause, DialResult.TARGET_BUSY, gate.get());
        }
        if (!this.access.allowsOutgoing(gate.get()) || !gate.get().access().allowsIncoming(this)) {
            return this.refused(address, cause, DialResult.DENIED, gate.get());
        }
        if (!SGEventBus.get().allows(new SGEvents.DialPre(cause, this, address, gate.get()))) {
            return this.refused(address, cause, DialResult.CANCELED, gate.get());
        }
        long chevronCost = 0;
        long openCost = 0;
        double tickCost = 0;
        if (this.get(GateProperties.REQUIRES_POWER).orElse(true) && !this.freeFor(cause)) {
            final DialCosts costs = this.stargates.config().dialCosts().forGate(this);
            final double factor = this.stargates.distanceFactor(this, gate.get());
            final SGEvents.DialCost cost = SGEventBus.get().post(new SGEvents.DialCost(cause, this, address,
                    gate.get(), costs.chevron().scaledWhole(factor), costs.open().scaledWhole(factor),
                    costs.tick().scaled(factor)));
            chevronCost = cost.chevronCost();
            openCost = cost.openCost();
            tickCost = cost.tickCost();
            // Enough for every chevron and the opening, so a dial doesn't start that can't finish
            final long needed = SGConnection.Costs.add(SGConnection.Costs.times(chevronCost, address.size()),
                    openCost);
            if (this.energy.available() < needed) {
                return this.refused(address, cause, DialResult.NOT_ENOUGH_ENERGY, gate.get());
            }
        }
        final SGConnection connection = new SGConnection(this, gate.get(), address,
                this.stargates.addressing().addressOf(this.position, gate.get().position.dimension()),
                this.get(GateProperties.OPEN_TICKS).orElse(0), cause,
                new SGConnection.Costs(chevronCost, openCost, tickCost));
        connection.result().thenAccept(result -> SGEventBus.get().post(
                new SGEvents.DialPost(cause, this, address, result, Optional.of(gate.get()))));
        this.stargates.connect(connection, mode);
        return connection.result();
    }

    // Players in creative mode dial without energy, if the server allows it
    private boolean freeFor(final Cause cause) {
        return this.stargates.config().freeInCreative()
                && cause.first(Player.class).map(Player::isCreative).orElse(false);
    }

    @Override
    public CompletionStage<DialResult> dial(final GlobalPos target, final Cause cause) {
        final Optional<Stargate> gate = this.stargates.mostAppropriate(target, cause);
        if (gate.isEmpty()) {
            return CompletableFuture.completedFuture(DialResult.NO_GATE);
        }
        return this.stargates.addressing().addressOf(gate.get().position(), this.position.dimension())
                .map(address -> this.dial(address, cause))
                .orElseGet(() -> CompletableFuture.completedFuture(DialResult.UNREACHABLE));
    }

    @Override
    public ActionResult engage(final Symbol symbol, final Cause cause) {
        if (!this.exists() || this.connection != null || this.engaged.size() >= this.chevrons()) {
            return ActionResult.NOT_POSSIBLE;
        }
        if (!this.access.allows(cause, GateRight.USE)) {
            return ActionResult.DENIED;
        }
        this.engaged.add(symbol);
        this.lastEngaged = this.now();
        this.play(SGSounds.chevron(DialStyle.of(this.typeKey()), true));
        final StringBuilder address = new StringBuilder();
        this.engaged.forEach(engaged -> address.append(engaged.character()));
        // Until the address is longer, its chevrons light as for the shortest address, the top chevron last
        final int expected = Math.max(SGStargate.SHORTEST_ADDRESS, this.engaged.size());
        this.show(new DialView(DialPhase.DIALING, true, address.toString(), this.engaged.size(), 0, 0, 0, 0,
                this.now(), 0, 0, this.now(), expected), cause);
        this.stargates.engaging(this);
        return ActionResult.DONE;
    }

    @Override
    public List<Symbol> engaged() {
        return List.copyOf(this.engaged);
    }

    @Override
    public CompletionStage<DialResult> dialEngaged(final Cause cause) {
        if (this.engaged.isEmpty()) {
            this.play(SGSounds.fail(DialStyle.of(this.typeKey())));
            return CompletableFuture.completedFuture(DialResult.INVALID_ADDRESS);
        }
        final Address address = new Address(this.engaged);
        this.release(cause);
        return this.dial(address, DialMode.QUICK, cause);
    }

    // Lets go of symbols locked one at a time, without a sound
    private void release(final Cause cause) {
        this.engaged.clear();
        if (this.connection == null) {
            this.show(DialView.IDLE, cause);
        }
    }

    /** Lets go of symbols locked one at a time if none has been locked for a while. Called each tick while any are. */
    boolean releaseIfIdle() {
        if (this.engaged.isEmpty()) {
            return true;
        }
        if (this.now() - this.lastEngaged < SGStargate.ENGAGE_TIMEOUT_TICKS) {
            return false;
        }
        this.play(SGSounds.fail(DialStyle.of(this.typeKey())));
        this.release(Cause.of(this.server()));
        return true;
    }

    @Override
    public ActionResult disconnect(final Cause cause) {
        if (!this.exists()) {
            return ActionResult.NOT_POSSIBLE;
        }
        if (!this.access.allows(cause, GateRight.USE)) {
            return ActionResult.DENIED;
        }
        if (!this.engaged.isEmpty() && this.connection == null) {
            this.play(SGSounds.fail(DialStyle.of(this.typeKey())));
            this.release(cause);
            return ActionResult.DONE;
        }
        if (this.connection == null || this.connection.isClosing()) {
            return ActionResult.NO_CHANGE;
        }
        // The dialed end may close the wormhole only if the dialing gate allows it
        if (this.connection.target() == this
                && !this.connection.dialer().get(GateProperties.CLOSE_FROM_EITHER_END).orElse(true)) {
            return ActionResult.NOT_POSSIBLE;
        }
        if (!SGEventBus.get().allows(new SGEvents.ConnectionDisconnect(cause, this.connection))) {
            return ActionResult.CANCELED;
        }
        this.connection.close(cause);
        return ActionResult.DONE;
    }

    @Override
    public <T> ActionResult set(final GateProperty<T> property, final T value, final Cause cause) {
        return this.change(property, Optional.of(value), cause);
    }

    @Override
    public ActionResult remove(final GateProperty<?> property, final Cause cause) {
        return this.removeTyped(property, cause);
    }

    private <T> ActionResult removeTyped(final GateProperty<T> property, final Cause cause) {
        return this.change(property, Optional.empty(), cause);
    }

    // Sets or removes a property: the rights first, then the access events for access settings, then the
    // property's own events, which see the value as the access listeners left it
    private <T> ActionResult change(final GateProperty<T> property, final Optional<T> value, final Cause cause) {
        if (!this.exists() || !this.stargates.isRegistered(property)) {
            return ActionResult.NOT_POSSIBLE;
        }
        if (SGStargate.MANAGED.contains(property) ? !this.access.mayManage(cause)
                : !this.access.allows(cause, GateRight.CONFIGURE)) {
            return ActionResult.DENIED;
        }
        final Optional<T> own = this.stargates.ownProperty(this.position, property);
        if (value.isEmpty() ? own.isEmpty() : own.equals(value)) {
            return ActionResult.NO_CHANGE;
        }
        final Optional<T> old = this.get(property);
        final List<Runnable> posts = new ArrayList<>();
        final Optional<T> adjusted = AccessChanges.adjust(this, property, old, value, cause, posts);
        final SGEvents.PropertyPre pre = SGEventBus.get().post(
                new SGEvents.PropertyPre(cause, this, property, old, adjusted));
        if (pre.isCanceled()) {
            return ActionResult.CANCELED;
        }
        final Optional<T> wanted = pre.newValue(property);
        final ActionResult result = wanted.isPresent()
                ? this.stargates.setProperty(this.position, property, wanted.get())
                : this.stargates.removeProperty(this.position, property);
        if (result != ActionResult.DONE) {
            return result;
        }
        if (property == GateProperties.IRIS) {
            if (wanted.isPresent() && old.isEmpty()) {
                this.iris.fitted();
            } else if (wanted.isEmpty()) {
                this.iris.removed();
            }
            // Fitted or taken off since, it no longer counts as the one the gate came with
            this.includeIris(false);
        }
        // What the base shows may depend on it
        this.sync();
        posts.forEach(Runnable::run);
        SGEventBus.get().post(new SGEvents.PropertyPost(cause, this, property, old, this.get(property)));
        return ActionResult.DONE;
    }

    @Override
    public ActionResult setType(final GateType type, final Cause cause) {
        return this.stargates.server().registryAccess().lookupOrThrow(SGCraftRegistries.GATE_TYPE)
                .getResourceKey(type)
                .map(key -> this.setType(key, cause))
                .orElse(ActionResult.NOT_POSSIBLE);
    }

    @Override
    public ActionResult setType(final Holder<GateType> type, final Cause cause) {
        return type.unwrapKey().map(key -> this.setType(key, cause)).orElse(ActionResult.NOT_POSSIBLE);
    }

    private ActionResult setType(final ResourceKey<GateType> type, final Cause cause) {
        final Optional<GateRecord> record = this.record();
        if (record.isEmpty() || this.state() != GateState.IDLE) {
            return ActionResult.NOT_POSSIBLE;
        }
        final Optional<? extends Holder<GateType>> holder = this.stargates.server().registryAccess()
                .lookupOrThrow(SGCraftRegistries.GATE_TYPE).get(type);
        if (holder.isEmpty() || !holder.get().value().supports(this.size())) {
            return ActionResult.NOT_POSSIBLE;
        }
        if (record.get().type().equals(type)) {
            return ActionResult.NO_CHANGE;
        }
        if (!this.access.allows(cause, GateRight.CONFIGURE)) {
            return ActionResult.DENIED;
        }
        this.stargates.store().put(record.get().withType(type));
        this.withBase(base -> base.setGateType(type));
        return ActionResult.DONE;
    }

    @Override
    public ActionResult setChevrons(final int chevrons, final Cause cause) {
        final Optional<GateRecord> record = this.record();
        if (record.isEmpty() || chevrons < 1 || this.state() != GateState.IDLE) {
            return ActionResult.NOT_POSSIBLE;
        }
        if (record.get().chevrons() == chevrons) {
            return ActionResult.NO_CHANGE;
        }
        if (!this.access.allows(cause, GateRight.CONFIGURE)) {
            return ActionResult.DENIED;
        }
        this.stargates.store().put(record.get().withChevrons(chevrons));
        this.withBase(base -> base.setChevrons(chevrons));
        return ActionResult.DONE;
    }

    // A gate lying flat turns over where it lies, its ring on the same blocks; one standing can't turn
    @Override
    public ActionResult setOrientation(final FrontAndTop orientation, final Cause cause) {
        final Optional<GateRecord> record = this.record();
        if (record.isEmpty()) {
            return ActionResult.NOT_POSSIBLE;
        }
        if (orientation == this.orientation()) {
            return ActionResult.NO_CHANGE;
        }
        final GateOrientation lying = GateOrientation.of(orientation);
        if (!this.lying().isFlat() || !lying.isFlat() || GateOrientation.facing(orientation) != this.facing()
                || this.state() != GateState.IDLE) {
            return ActionResult.NOT_POSSIBLE;
        }
        if (!this.access.allows(cause, GateRight.CONFIGURE)) {
            return ActionResult.DENIED;
        }
        this.stargates.store().put(record.get().withOrientation(lying));
        this.withBase(base -> base.setOrientation(lying));
        this.sync();
        return ActionResult.DONE;
    }

    @Override
    public ActionResult placeFixtures(final FixtureBuffer fixtures, final PlacementMode mode, final Cause cause) {
        final ServerLevel level = this.level();
        if (!this.exists() || level == null) {
            return ActionResult.NOT_POSSIBLE;
        }
        if (!this.access.allows(cause, GateRight.CONFIGURE)) {
            return ActionResult.DENIED;
        }
        return Fixtures.build(level, this.frame(), fixtures, mode) ? ActionResult.DONE
                : ActionResult.NOT_POSSIBLE;
    }

    void enter(final SGConnection connection, final DialView view, final Cause cause) {
        this.connection = connection;
        this.show(view, cause);
    }

    void leave(final SGConnection connection, final Cause cause) {
        if (this.connection != connection) {
            return;
        }
        this.connection = null;
        this.show(DialView.IDLE, cause);
    }

    /** The connection's wormhole went to another gate: this gate is no longer part of it, and shows it closing. */
    void detach(final SGConnection connection, final DialView closing, final Cause cause) {
        if (this.connection != connection) {
            return;
        }
        this.connection = null;
        this.show(closing, cause);
    }

    /** A wormhole that went to another gate has finished closing on this one. */
    void closed(final Cause cause) {
        if (this.connection == null && this.view.phase() == DialPhase.DISCONNECTING) {
            this.show(DialView.IDLE, cause);
        }
    }

    /** The gate was broken: its connection ends. */
    void gone() {
        if (this.connection != null) {
            this.connection.gone();
        }
    }

    /**
     * Copies what the gate is doing, and its iris, to its base, if the base is loaded, and lights the ring under its
     * lit chevrons.
     */
    public void sync() {
        final Optional<IrisView> iris = this.get(GateProperties.IRIS)
                .map(type -> new IrisView(type, this.iris.state(), this.iris.since()));
        final boolean signal = this.state() != GateState.IDLE && this.get(GateProperties.REDSTONE_OUTPUT).orElse(true);
        final boolean transparent = this.get(GateProperties.HORIZON_TRANSPARENT).orElse(true);
        final boolean irisIncluded = this.irisIncluded();
        this.withBase(base -> {
            base.setDialView(this.view);
            base.setIris(iris.orElse(null));
            base.setIrisIncluded(irisIncluded);
            base.setSignal(signal);
            base.setTransparent(transparent);
            if (base.getLevel() != null) {
                GateLight.showChevrons(base.getLevel(), this.frame(), this.litChevrons());
            }
        });
    }

    // Which chevrons are lit, by position, as the gate's renderer lights them: those locked, or every one of a
    // Universe gate while it is active
    private boolean[] litChevrons() {
        if (!this.typeKey().equals(GateTypes.UNIVERSE)) {
            return ChevronOrder.locked(this.chevrons(), this.view.length(), this.view.engaged());
        }
        final boolean[] lit = new boolean[ChevronOrder.POSITIONS];
        if (this.view.active()) {
            final int first = ChevronOrder.first(this.chevrons());
            for (int position = first; position < Math.min(first + this.chevrons(), lit.length); position++) {
                lit[position] = true;
            }
        }
        return lit;
    }

    /**
     * What happens once the gate is broken, worked out while it still stands: an explosion in the middle of its ring,
     * as in 1.12, if its wormhole is open and its settings give one.
     */
    Runnable breach() {
        final ServerLevel level = this.stargates.server().getLevel(this.position.dimension());
        final float strength = this.get(GateProperties.BREACH_EXPLOSION).orElse(10.0).floatValue();
        if (level == null || this.state() != GateState.CONNECTED || strength <= 0) {
            return () -> { };
        }
        final Vec3 middle = this.middle();
        final boolean fire = this.get(GateProperties.BREACH_FIRE).orElse(true);
        final Level.ExplosionInteraction blocks = this.get(GateProperties.BREACH_BREAKS_BLOCKS).orElse(true)
                ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE;
        return () -> level.explode(null, middle.x, middle.y, middle.z, strength, fire, blocks);
    }

    /** Plays a sound from the middle of the gate's ring, as far as its size carries it, if its world is loaded. */
    public void play(final SoundEvent sound) {
        final ServerLevel level = this.stargates.server().getLevel(this.position.dimension());
        if (level != null) {
            final Vec3 middle = this.middle();
            level.playSound(null, middle.x, middle.y, middle.z, sound, SoundSource.BLOCKS,
                    SGSounds.volume(this.shape()), 1.0F);
        }
    }

    // The middle of the gate's ring, in the world
    private Vec3 middle() {
        return this.frame().toWorld(new Vec3(0, this.shape().centerY(), 0));
    }

    // A dial refused at the start: the gate sounds its failure, unless it is busy with another
    private CompletionStage<DialResult> refused(final Address address, final Cause cause, final DialResult result,
            final @Nullable SGStargate target) {
        this.play(SGSounds.fail(DialStyle.of(this.typeKey())));
        SGEventBus.get().post(new SGEvents.DialPost(cause, this, address, result, Optional.ofNullable(target)));
        return CompletableFuture.completedFuture(result);
    }

    void keepLoaded() {
        final ServerLevel level = this.stargates.server().getLevel(this.position.dimension());
        if (level != null) {
            // The server's range of chunks, out from the gate's edges, which a supergate's reach further, and a flat
            // one's further still
            final int reach = this.lying().isFlat() ? this.shape().height() : this.shape().radius();
            final int radius = this.stargates.config().chunkLoadingRange() + (reach + 15) / 16;
            level.getChunkSource().addTicketWithRadius(TicketType.PORTAL, ChunkPos.containing(this.position.pos()),
                    radius);
        }
    }

    /** Shows fluid pouring out of the gate, or none. */
    void showSpill(final @Nullable FluidSpill spill) {
        this.withBase(base -> base.setSpill(spill));
    }

    private void withBase(final Consumer<StargateBaseBlockEntity> action) {
        final ServerLevel level = this.stargates.server().getLevel(this.position.dimension());
        if (level != null && level.isLoaded(this.position.pos())
                && level.getBlockEntity(this.position.pos()) instanceof StargateBaseBlockEntity base) {
            action.accept(base);
        }
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof SGStargate gate && gate.position.equals(this.position);
    }

    @Override
    public int hashCode() {
        return this.position.hashCode();
    }

    @Override
    public String toString() {
        return "Stargate[" + this.position + ", " + this.view.phase() + "]";
    }
}
