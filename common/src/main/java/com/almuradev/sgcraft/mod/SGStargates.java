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
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.address.AddressTarget;
import com.almuradev.sgcraft.api.gate.GatePlacement;
import com.almuradev.sgcraft.api.gate.GateProperty;
import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.api.gate.PlacementResult;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.api.gate.Stargates;
import com.almuradev.sgcraft.gate.GateFormation;
import com.almuradev.sgcraft.mod.GateStore.GateRecord;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The gates on a running server: the formed gates in the {@link GateStore}, each with a live {@link SGStargate} once
 * it is asked for, and the connections between them, which it ticks.
 */
public final class SGStargates implements Stargates {

    private final MinecraftServer server;
    private final SGAddressing addressing;
    private final Map<GlobalPos, SGStargate> live = new HashMap<>();
    private final List<SGConnection> connections = new ArrayList<>();

    public SGStargates(final MinecraftServer server, final SGAddressing addressing) {
        this.server = server;
        this.addressing = addressing;
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
            final int chevrons) {
        final GateStore store = this.store();
        final Optional<GateRecord> known = store.gate(position);
        store.put(new GateRecord(position, type, facing, known.map(GateRecord::chevrons).orElse(chevrons),
                known.map(GateRecord::properties).orElseGet(CompoundTag::new)));
        this.gate(position).ifPresent(SGStargate::sync);
    }

    /** Forgets a gate that has been broken apart, ending its connection. */
    public void broken(final GlobalPos position) {
        final SGStargate gate = this.live.remove(position);
        if (gate != null) {
            gate.gone();
        }
        this.store().remove(position);
    }

    void connect(final SGConnection connection) {
        this.connections.add(connection);
        connection.start();
    }

    /** Moves every connection on by a tick. */
    public void tick() {
        this.connections.removeIf(SGConnection::tick);
    }

    /** Ends every connection, as the server stops. */
    public void stop() {
        new ArrayList<>(this.connections).forEach(SGConnection::gone);
        this.connections.clear();
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
        final Optional<Identifier> id = this.propertyId(property);
        if (id.isEmpty()) {
            return Optional.empty();
        }
        final Optional<Tag> tag = this.store().gate(position)
                .flatMap(gate -> Optional.ofNullable(gate.properties().get(id.get().toString())));
        if (tag.isPresent()) {
            final Optional<T> value = property.codec().parse(NbtOps.INSTANCE, tag.get()).result();
            if (value.isPresent()) {
                return value;
            }
        }
        return property.defaultValue();
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
                    && GateFormation.isPartOfGate(gate.position().pos(), gate.facing(), position.pos())) {
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

    @Override
    public Optional<Stargate> mostAppropriate(final GlobalPos position, final Cause cause) {
        return this.mostAppropriate(position);
    }

    @Override
    public PlacementResult place(final GatePlacement placement, final Cause cause) {
        throw new UnsupportedOperationException("Placing gates isn't supported yet");
    }
}
