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
import com.almuradev.sgcraft.api.access.GateAccess;
import com.almuradev.sgcraft.api.access.GateRight;
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.address.AddressTarget;
import com.almuradev.sgcraft.api.energy.GateEnergy;
import com.almuradev.sgcraft.api.gate.Connection;
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
import com.almuradev.sgcraft.block.entity.StargateBaseBlockEntity;
import com.almuradev.sgcraft.gate.DialStyle;
import com.almuradev.sgcraft.gate.DialView;
import com.almuradev.sgcraft.sound.SGSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import com.almuradev.sgcraft.mod.GateStore.GateRecord;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * A formed gate on a running server. Its lasting details live in the {@link GateStore}; what it is doing lives here,
 * and is copied to its base's block entity whenever the base is loaded, for the client to draw.
 */
public final class SGStargate implements Stargate {

    private final SGStargates stargates;
    private final GlobalPos position;
    private DialView view = DialView.IDLE;
    private @Nullable SGConnection connection;

    SGStargate(final SGStargates stargates, final GlobalPos position) {
        this.stargates = stargates;
        this.position = position;
    }

    private Optional<GateRecord> record() {
        return this.stargates.store().gate(this.position);
    }

    MinecraftServer server() {
        return this.stargates.server();
    }

    /** @return the key of the gate's type */
    public ResourceKey<GateType> typeKey() {
        return this.record().map(GateRecord::type).orElse(GateTypes.MILKY_WAY);
    }

    @Override
    public GlobalPos position() {
        return this.position;
    }

    @Override
    public FrontAndTop orientation() {
        return FrontAndTop.fromFrontAndTop(this.record().map(GateRecord::facing).orElse(Direction.NORTH),
                Direction.UP);
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
        return GateSize.STANDARD;
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
        return switch (this.view.phase()) {
            case IDLE -> GateState.IDLE;
            case DIALLING, INTER_DIALLING, SYNC_AWAIT -> this.view.initiator() ? GateState.DIALLING
                    : GateState.INCOMING;
            case ESTABLISHING -> GateState.OPENING;
            case TRANSIENT, CONNECTED -> GateState.CONNECTED;
            case DISCONNECTING -> GateState.CLOSING;
        };
    }

    /** @return what the gate is doing, as its base shows it */
    public DialView view() {
        return this.view;
    }

    @Override
    public Optional<Connection> connection() {
        return Optional.ofNullable(this.connection);
    }

    @Override
    public Optional<Iris> iris() {
        return Optional.empty();
    }

    @Override
    public GateEnergy energy() {
        return Unmetered.ENERGY;
    }

    @Override
    public GateAccess access() {
        return Unmetered.ACCESS;
    }

    @Override
    public <T> Optional<T> get(final GateProperty<T> property) {
        return this.stargates.property(this.position, property);
    }

    @Override
    public CompletionStage<DialResult> dial(final Address address, final Cause cause) {
        if (!this.exists()) {
            return CompletableFuture.completedFuture(DialResult.GATE_GONE);
        }
        if (this.state() != GateState.IDLE) {
            return CompletableFuture.completedFuture(DialResult.BUSY);
        }
        if (this.stargates.addressing().reachOf(address).isEmpty()) {
            return this.refused(DialResult.INVALID_ADDRESS);
        }
        if (address.size() > this.chevrons()) {
            return this.refused(DialResult.NOT_ENOUGH_CHEVRONS);
        }
        final Optional<AddressTarget> target = this.stargates.addressing().resolve(address, this.position.dimension());
        final Optional<SGStargate> gate = target.flatMap(this.stargates::inCell);
        if (gate.isEmpty()) {
            return this.refused(DialResult.NO_GATE);
        }
        if (gate.get() == this) {
            return this.refused(DialResult.SAME_GATE);
        }
        if (gate.get().state() != GateState.IDLE) {
            return this.refused(DialResult.TARGET_BUSY);
        }
        final SGConnection connection = new SGConnection(this, gate.get(), address,
                this.stargates.addressing().addressOf(this.position, gate.get().position.dimension()),
                this.get(GateProperties.OPEN_TICKS).orElse(0));
        this.stargates.connect(connection);
        return connection.result();
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
    public ActionResult disconnect(final Cause cause) {
        if (!this.exists()) {
            return ActionResult.NOT_POSSIBLE;
        }
        if (this.connection == null || this.connection.isClosing()) {
            return ActionResult.NO_CHANGE;
        }
        this.connection.close();
        return ActionResult.DONE;
    }

    @Override
    public <T> ActionResult set(final GateProperty<T> property, final T value, final Cause cause) {
        if (!this.exists()) {
            return ActionResult.NOT_POSSIBLE;
        }
        return this.stargates.setProperty(this.position, property, value);
    }

    @Override
    public ActionResult remove(final GateProperty<?> property, final Cause cause) {
        if (!this.exists()) {
            return ActionResult.NOT_POSSIBLE;
        }
        return this.stargates.removeProperty(this.position, property);
    }

    @Override
    public ActionResult setType(final GateType type, final Cause cause) {
        return this.stargates.server().registryAccess().lookupOrThrow(SGCraftRegistries.GATE_TYPE)
                .getResourceKey(type)
                .map(key -> this.setType(key))
                .orElse(ActionResult.NOT_POSSIBLE);
    }

    @Override
    public ActionResult setType(final Holder<GateType> type, final Cause cause) {
        return type.unwrapKey().map(this::setType).orElse(ActionResult.NOT_POSSIBLE);
    }

    private ActionResult setType(final ResourceKey<GateType> type) {
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
        this.stargates.store().put(record.get().withChevrons(chevrons));
        this.withBase(base -> base.setChevrons(chevrons));
        return ActionResult.DONE;
    }

    @Override
    public ActionResult placeFixtures(final FixtureBuffer fixtures, final PlacementMode mode, final Cause cause) {
        return ActionResult.NOT_POSSIBLE;
    }

    void enter(final SGConnection connection, final DialView view) {
        this.connection = connection;
        this.view = view;
        this.sync();
    }

    void leave(final SGConnection connection) {
        if (this.connection != connection) {
            return;
        }
        this.connection = null;
        this.view = DialView.IDLE;
        this.sync();
    }

    /** The gate was broken: its connection ends. */
    void gone() {
        if (this.connection != null) {
            this.connection.gone();
        }
    }

    /** Copies what the gate is doing to its base, if the base is loaded. */
    public void sync() {
        this.withBase(base -> base.setDialView(this.view));
    }

    /** Plays a sound from the middle of the gate's ring, if its world is loaded. */
    void play(final SoundEvent sound) {
        final ServerLevel level = this.stargates.server().getLevel(this.position.dimension());
        if (level != null) {
            final BlockPos pos = this.position.pos();
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 2.5, pos.getZ() + 0.5, sound, SoundSource.BLOCKS,
                    1.0F, 1.0F);
        }
    }

    // A dial refused at the start: the gate sounds its failure, unless it is busy with another
    private CompletionStage<DialResult> refused(final DialResult result) {
        if (result != DialResult.BUSY) {
            this.play(SGSounds.fail(DialStyle.of(this.typeKey())));
        }
        return CompletableFuture.completedFuture(result);
    }

    void keepLoaded() {
        final ServerLevel level = this.stargates.server().getLevel(this.position.dimension());
        if (level != null) {
            level.getChunkSource().addTicketWithRadius(TicketType.PORTAL, ChunkPos.containing(this.position.pos()), 1);
        }
    }

    private void withBase(final Consumer<StargateBaseBlockEntity> action) {
        final ServerLevel level = this.stargates.server().getLevel(this.position.dimension());
        if (level != null && level.isLoaded(this.position.pos())
                && level.getBlockEntity(this.position.pos()) instanceof StargateBaseBlockEntity base) {
            action.accept(base);
        }
    }

    @Override
    public String toString() {
        return "Stargate[" + this.position + ", " + this.view.phase() + "]";
    }

    /** Stand-ins until gates have energy and access control. */
    private static final class Unmetered {

        static final GateEnergy ENERGY = new GateEnergy() {
            @Override
            public long stored() {
                return 0;
            }

            @Override
            public long capacity() {
                return 0;
            }

            @Override
            public long insert(final long amount, final boolean simulate) {
                return 0;
            }

            @Override
            public long extract(final long amount, final boolean simulate) {
                return 0;
            }
        };

        static final GateAccess ACCESS = new GateAccess() {
            @Override
            public boolean allowsIncoming(final Stargate from) {
                return true;
            }

            @Override
            public boolean allowsOutgoing(final Stargate to) {
                return true;
            }

            @Override
            public boolean allows(final UUID player, final GateRight right) {
                return true;
            }

            @Override
            public boolean allows(final Cause cause, final GateRight right) {
                return true;
            }
        };

        private Unmetered() {
        }
    }
}
