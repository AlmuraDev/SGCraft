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

package com.almuradev.sgcraft.mod;

import com.almuradev.sgcraft.api.ActionResult;
import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.SGCraftRegistries;
import com.almuradev.sgcraft.api.access.GateRight;
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.address.AddressTarget;
import com.almuradev.sgcraft.api.gate.DialMode;
import com.almuradev.sgcraft.api.gate.GatePlacement;
import com.almuradev.sgcraft.api.gate.GateProperty;
import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.api.gate.IrisState;
import com.almuradev.sgcraft.api.gate.PlacementResult;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.api.gate.Stargates;
import com.almuradev.sgcraft.gate.GateFormation;
import com.almuradev.sgcraft.gate.GateShape;
import com.almuradev.sgcraft.mod.GateStore.GateRecord;
import com.almuradev.sgcraft.mod.event.SGEventBus;
import com.almuradev.sgcraft.mod.event.SGEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The gates on a running server: the formed gates in the {@link GateStore}, each with a live {@link SGStargate} once
 * it is asked for, and the connections between them, which it ticks.
 */
public final class SGStargates implements Stargates {

    private final MinecraftServer server;
    private final SGAddressing addressing;
    private SGConfig config;
    // Gates whose iris is moving, ticked until it stops, with why it moved
    private final Map<SGStargate, Cause> irises = new HashMap<>();
    // While a gate is being placed whole, its forming fires no forming events
    private boolean placing;
    private final Map<GlobalPos, SGStargate> live = new HashMap<>();
    private final List<SGConnection> connections = new ArrayList<>();
    // Gates with symbols locked one at a time, as a DHD does, watched so they let go if left
    private final Set<SGStargate> engaging = new HashSet<>();

    public SGStargates(final MinecraftServer server, final SGAddressing addressing, final SGConfig config) {
        this.server = server;
        this.addressing = addressing;
        this.config = config;
    }

    SGConfig config() {
        return this.config;
    }

    /**
     * Changes what wormholes cost from now on, and writes it to the settings file. Wormholes already dialing or open
     * keep what they cost when they were dialed.
     *
     * @return whether the settings file was written
     */
    public boolean setDialCosts(final DialCosts costs) {
        this.config = this.config.withDialCosts(costs);
        return SGConfig.saveDialCosts(costs);
    }

    /** @return whether a gate is being placed whole, so its forming isn't a player building it */
    public boolean isPlacing() {
        return this.placing;
    }

    /**
     * How many times more a wormhole between two gates costs than the base cost, as in the original: more the further
     * apart they are, more again between worlds, and more again to or from a supergate, whichever end dials.
     */
    double distanceFactor(final Stargate from, final Stargate to) {
        final BlockPos a = from.position().pos();
        final BlockPos b = to.position().pos();
        final double d = Math.hypot(a.getX() - b.getX(), a.getZ() - b.getZ());
        final double scale = Math.log(0.05 * d + 1) / Math.log(0.05 * 16 * 279935);
        double factor = 1 + 14 * this.config.distanceFactorMultiplier() * scale * scale;
        if (!from.position().dimension().equals(to.position().dimension())) {
            factor *= this.config.interWorldMultiplier();
        }
        if (from.size().radius() > GateShape.STANDARD.radius() || to.size().radius() > GateShape.STANDARD.radius()) {
            factor *= this.config.supergateEnergyMultiplier();
        }
        return factor;
    }

    void moving(final SGStargate gate, final Cause cause) {
        this.irises.put(gate, cause);
    }

    MinecraftServer server() {
        return this.server;
    }

    SGAddressing addressing() {
        return this.addressing;
    }

    GateStore store() {
        return this.server.getDataStorage().computeIfAbsent(GateStore.TYPE);
    }

    /** @return the gate whose base is at {@code position}, if a gate is formed there */
    public Optional<SGStargate> gate(final GlobalPos position) {
        if (this.store().gate(position).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(this.live.computeIfAbsent(position, pos -> new SGStargate(this, pos)));
    }

    /** Records a gate that has just formed, keeping its chevrons and properties if it was known before. */
    public void formed(final GlobalPos position, final ResourceKey<GateType> type, final Direction facing,
            final int chevrons, final GateShape shape) {
        final GateStore store = this.store();
        final Optional<GateRecord> known = store.gate(position);
        store.put(new GateRecord(position, type, facing, known.map(GateRecord::chevrons).orElse(chevrons),
                known.map(GateRecord::properties).orElseGet(CompoundTag::new),
                known.map(GateRecord::state).orElseGet(CompoundTag::new), shape.radius()));
        this.gate(position).ifPresent(SGStargate::sync);
    }

    /** Forgets a gate that has been broken apart, ending its connection. */
    public void broken(final GlobalPos position, final Cause cause) {
        final Optional<SGStargate> gate = this.gate(position);
        final Runnable breach = gate.map(SGStargate::breach).orElse(() -> { });
        this.live.remove(position);
        gate.ifPresent(SGStargate::gone);
        this.store().remove(position);
        gate.ifPresent(broken -> SGEventBus.get().post(new SGEvents.BreakPost(cause, broken)));
        breach.run();
    }

    /** Carries on the irises that were moving when the server last stopped. */
    private void restoreIrises() {
        for (final GateRecord record : this.store().gates()) {
            final IrisState state = SGIris.stateOf(record.state());
            if (state == IrisState.OPENING || state == IrisState.CLOSING) {
                this.gate(record.position()).ifPresent(gate -> this.irises.put(gate, Cause.of(this.server)));
            }
        }
    }

    void connect(final SGConnection connection, final DialMode mode) {
        this.connections.add(connection);
        connection.start(mode);
    }

    /** Moves every connection on by a tick. */
    public void tick() {
        this.connections.removeIf(SGConnection::tick);
        this.engaging.removeIf(SGStargate::releaseIfIdle);
        this.irises.entrySet().removeIf(entry -> !entry.getKey().exists()
                || entry.getKey().irisState().tick(entry.getValue()));
    }

    void engaging(final SGStargate gate) {
        this.engaging.add(gate);
    }

    /**
     * Forgets the connections as the server stops. They are saved as they go, so they carry on when it starts again.
     */
    public void stop() {
        this.connections.clear();
    }

    /** Carries on the connections that were under way when the server last stopped, and the moving irises. */
    public void restore() {
        this.restoreIrises();
        final GateStore store = this.store();
        for (final CompoundTag tag : store.connections()) {
            final Optional<SGConnection> connection = SGConnection.restore(this, tag);
            if (connection.isEmpty()) {
                tag.read("dialer", GlobalPos.CODEC).ifPresent(store::removeConnection);
                continue;
            }
            this.connections.add(connection.get());
            connection.get().resume();
        }
    }

    Optional<SGStargate> inCell(final AddressTarget target) {
        final int cell = this.addressing.partition(target.world()).cellChunks();
        return this.store().gates().stream()
                .filter(gate -> gate.position().dimension().equals(target.world()))
                .filter(gate -> {
                    final ChunkPos chunk = ChunkPos.containing(gate.position().pos());
                    return chunk.x() >= target.chunk().x() && chunk.x() < target.chunk().x() + cell
                            && chunk.z() >= target.chunk().z() && chunk.z() < target.chunk().z() + cell;
                })
                .findFirst()
                .flatMap(gate -> this.gate(gate.position()));
    }

    <T> Optional<T> property(final GlobalPos position, final GateProperty<T> property) {
        if (this.propertyId(property).isEmpty()) {
            return Optional.empty();
        }
        final Optional<T> own = this.ownProperty(position, property);
        return own.isPresent() ? own : property.defaultValue();
    }

    /** @return the gate's own value of a property, without the default */
    <T> Optional<T> ownProperty(final GlobalPos position, final GateProperty<T> property) {
        final Optional<Identifier> id = this.propertyId(property);
        if (id.isEmpty()) {
            return Optional.empty();
        }
        return this.store().gate(position)
                .flatMap(gate -> Optional.ofNullable(gate.properties().get(id.get().toString())))
                .flatMap(tag -> property.codec().parse(NbtOps.INSTANCE, tag).result());
    }

    boolean isRegistered(final GateProperty<?> property) {
        return this.propertyId(property).isPresent();
    }

    <T> ActionResult setProperty(final GlobalPos position, final GateProperty<T> property, final T value) {
        final Optional<Identifier> id = this.propertyId(property);
        final Optional<GateRecord> gate = this.store().gate(position);
        if (id.isEmpty() || gate.isEmpty()) {
            return ActionResult.NOT_POSSIBLE;
        }
        final Optional<Tag> encoded = property.codec().encodeStart(NbtOps.INSTANCE, value).result();
        if (encoded.isEmpty()) {
            return ActionResult.NOT_POSSIBLE;
        }
        final CompoundTag properties = gate.get().properties();
        if (encoded.get().equals(properties.get(id.get().toString()))) {
            return ActionResult.NO_CHANGE;
        }
        properties.put(id.get().toString(), encoded.get());
        this.store().put(gate.get().withProperties(properties));
        return ActionResult.DONE;
    }

    ActionResult removeProperty(final GlobalPos position, final GateProperty<?> property) {
        final Optional<Identifier> id = this.propertyId(property);
        final Optional<GateRecord> gate = this.store().gate(position);
        if (id.isEmpty() || gate.isEmpty()) {
            return ActionResult.NOT_POSSIBLE;
        }
        final CompoundTag properties = gate.get().properties();
        if (!properties.contains(id.get().toString())) {
            return ActionResult.NO_CHANGE;
        }
        properties.remove(id.get().toString());
        this.store().put(gate.get().withProperties(properties));
        return ActionResult.DONE;
    }

    private Optional<Identifier> propertyId(final GateProperty<?> property) {
        return Optional.ofNullable(this.server.registryAccess().lookupOrThrow(SGCraftRegistries.GATE_PROPERTY)
                .getKey(property));
    }

    @Override
    public Optional<Stargate> at(final GlobalPos position) {
        for (final GateRecord gate : this.store().gates()) {
            if (gate.position().dimension().equals(position.dimension())
                    && GateFormation.isPartOfGate(gate.position().pos(), gate.facing(), gate.shape(),
                    position.pos())) {
                return this.gate(gate.position()).map(Stargate.class::cast);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Stargate> byAddress(final Address address, final ResourceKey<Level> from) {
        return this.addressing.resolve(address, from).flatMap(this::inCell).map(Stargate.class::cast);
    }

    @Override
    public Collection<Stargate> in(final ResourceKey<Level> world) {
        final List<Stargate> gates = new ArrayList<>();
        for (final GateRecord gate : this.store().gates()) {
            if (gate.position().dimension().equals(world)) {
                this.gate(gate.position()).ifPresent(gates::add);
            }
        }
        return gates;
    }

    /** Chooses the gate in the same world whose base is nearest. */
    @Override
    public Optional<Stargate> mostAppropriate(final GlobalPos position) {
        final BlockPos pos = position.pos();
        return this.in(position.dimension()).stream()
                .min(Comparator.comparingDouble(gate -> gate.position().pos().distSqr(pos)));
    }

    /** Chooses the nearest gate in the same world that the cause may use. */
    @Override
    public Optional<Stargate> mostAppropriate(final GlobalPos position, final Cause cause) {
        final BlockPos pos = position.pos();
        return this.in(position.dimension()).stream()
                .filter(gate -> gate.access().allows(cause, GateRight.USE))
                .min(Comparator.comparingDouble(gate -> gate.position().pos().distSqr(pos)));
    }

    @Override
    public PlacementResult place(final GatePlacement placement, final Cause cause) {
        final SGEvents.PlacePre pre = SGEventBus.get().post(new SGEvents.PlacePre(cause, placement));
        if (pre.isCanceled()) {
            return GatePlacer.Result.of(PlacementResult.Status.CANCELED);
        }
        this.placing = true;
        try {
            final PlacementResult result = new GatePlacer(this, cause).place(pre.placement());
            result.gate().ifPresent(gate -> SGEventBus.get().post(new SGEvents.PlacePost(cause, gate,
                    pre.placement().withPosition(gate.position()))));
            return result;
        } finally {
            this.placing = false;
        }
    }
}
