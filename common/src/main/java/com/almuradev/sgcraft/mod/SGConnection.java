/*
 * This file is part of SGCraft, licensed under the MIT License (MIT).
 * It is based on the original SGCraft by gcewing.
 *
 * Copyright (c) AlmuraDev <https://github.com/AlmuraDev>
 * Copyright (c) 2016 gcewing <https://github.com/gcewing>
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

import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.address.Symbol;
import com.almuradev.sgcraft.api.gate.Connection;
import com.almuradev.sgcraft.api.gate.DialMode;
import com.almuradev.sgcraft.api.gate.DialResult;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.block.EventHorizonBlock;
import com.almuradev.sgcraft.gate.ChevronOrder;
import com.almuradev.sgcraft.gate.DialPhase;
import com.almuradev.sgcraft.gate.DialStyle;
import com.almuradev.sgcraft.gate.DialView;
import com.almuradev.sgcraft.gate.FluidSpill;
import com.almuradev.sgcraft.gate.GateLight;
import com.almuradev.sgcraft.mod.event.SGEventBus;
import com.almuradev.sgcraft.mod.event.SGEvents;
import com.almuradev.sgcraft.sound.SGSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * A link between two gates, from the first symbol to the wormhole closing, with the timings of the original
 * SGCraft. The server ticks it once a tick, and it moves both gates through the {@linkplain DialPhase steps of a
 * dial}, the dialing gate's type setting the pace. Both gates lock each chevron in the same tick.
 */
public final class SGConnection implements Connection {

    // How long the gates wait on each other once every chevron has locked, how long the kawoosh lasts, and how long
    // the wormhole takes to shut down
    private static final int SYNC_AWAIT_TICKS = 10;
    private static final int TRANSIENT_TICKS = 20;
    private static final int DISCONNECTING_TICKS = 40;
    // How long a dialed Pegasus gate takes to fill with light, at most
    private static final int PEGASUS_FILL_TICKS = 90;
    // How often the two gates' chunks are kept loaded; less than the loading ticket's lifetime
    private static final int KEEP_LOADED_TICKS = 100;
    // How long into the wormhole forming its sound starts, so the sound's burst meets the kawoosh
    private static final int MILKY_WAY_OPEN_SOUND_TICKS = 5;

    private final SGStargate dialer;
    private final SGStargate target;
    private final Address address;
    private final String homeAddress;
    private final int openTicks;
    private final DialStyle style;
    private final int[] positions;
    // How fast the dialing gate dials, in degrees a tick
    private final double speed;
    private final CompletableFuture<DialResult> result = new CompletableFuture<>();
    private final GateTravel dialerTravel;
    private final GateTravel targetTravel;
    // Who dialed, and who or what is closing it, if anyone has asked; events about the connection carry these
    private final Cause cause;
    private Cause closeCause;
    // What the wormhole costs the dialing gate as each chevron locks, to open, and each tick it stays open
    private final Costs costs;
    // The part of a unit of the tick cost not paid yet, paid once the fractions add up to a whole unit
    private double tickOwed;
    private DialPhase phase = DialPhase.DIALING;
    // Ticks left in the current phase; the phase ends on the tick after this reaches 0, as in the original
    private int timeout;
    private int phaseTicks;
    private int elapsed;
    private int symbol;
    private int engaged;
    private int age;
    private int ticksOpen;
    private float ringAngle;
    private DialStyle.Move move = new DialStyle.Move(0, 0, 0);
    private long phaseStart;
    private long lockTick;
    private long fillStart;
    private long fillEnd;

    SGConnection(final SGStargate dialer, final SGStargate target, final Address address,
            final Optional<Address> homeAddress, final int openTicks, final Cause cause, final Costs costs) {
        this.dialer = dialer;
        this.target = target;
        this.cause = cause;
        this.closeCause = cause;
        this.costs = costs;
        this.address = address;
        this.homeAddress = homeAddress.map(SGConnection::characters).orElse(SGConnection.characters(address));
        this.openTicks = openTicks;
        this.style = DialStyle.of(dialer.typeKey());
        this.positions = ChevronOrder.positions(dialer.chevrons(), address.size());
        this.speed = dialer.get(GateProperties.RING_SPEED).orElse(this.style.defaultSpeed());
        this.dialerTravel = new GateTravel(dialer);
        this.targetTravel = new GateTravel(target);
    }

    private static String characters(final Address address) {
        final StringBuilder text = new StringBuilder(address.size());
        address.forEach(symbol -> text.append(symbol.character()));
        return text.toString();
    }

    CompletableFuture<DialResult> result() {
        return this.result;
    }

    private long now() {
        return this.dialer.server().overworld().getGameTime();
    }

    void start(final DialMode mode) {
        this.dialer.keepLoaded();
        this.target.keepLoaded();
        this.dialer.irisState().remember();
        this.target.irisState().remember();
        if (mode == DialMode.QUICK) {
            // Every chevron locks at once, with no ring moving, as the original's "chevrons lock on dial" did, and
            // they are paid for together
            if (!this.dialer.energy().pay(Costs.times(this.costs.chevron(), this.address.size()))) {
                this.fail(DialResult.NOT_ENOUGH_ENERGY);
                return;
            }
            this.engaged = this.address.size();
            for (int i = 0; i < this.engaged; i++) {
                SGEventBus.get().post(new SGEvents.DialLockChevron(this.cause, this.dialer, this.address, i));
            }
            this.lockTick = this.now();
            this.fillStart = this.now();
            this.fillEnd = this.now();
            this.target.play(SGSounds.chevron(DialStyle.of(this.target.typeKey()), false));
            this.enter(DialPhase.SYNC_AWAIT, SGConnection.SYNC_AWAIT_TICKS);
            return;
        }
        this.scheduleFill();
        this.startSymbol();
    }

    // When a dialed Pegasus gate fills with light: so it is full as the last chevron locks
    private void scheduleFill() {
        DialStyle.Move move = new DialStyle.Move(0, 0, 0);
        float angle = 0;
        long ticks = 0;
        for (int i = 0; i < this.address.size(); i++) {
            move = this.style.move(angle, this.address.get(i), i, this.positions, this.speed);
            angle = move.to();
            ticks += move.ticks() + 1;
            if (i + 1 < this.address.size()) {
                ticks += this.style.interDialingTicks() + 1;
            }
        }
        this.fillEnd = this.now() + ticks;
        this.fillStart = Math.max(this.now(), this.fillEnd - SGConnection.PEGASUS_FILL_TICKS);
    }

    private void startSymbol() {
        final Symbol next = this.address.get(this.symbol);
        this.move = this.style.move(this.ringAngle, next, this.symbol, this.positions, this.speed);
        this.ringAngle = this.move.to();
        this.enter(DialPhase.DIALING, this.move.ticks());
    }

    private void enter(final DialPhase phase, final int ticks) {
        this.phase = phase;
        this.timeout = ticks;
        this.phaseTicks = ticks;
        this.elapsed = 0;
        this.phaseStart = this.now();
        this.sync();
        this.dialer.store().putConnection(this.dialer.position(), this.save());
    }

    // How often the fluid each end pours out of the other is looked at again, in ticks
    private static final int SPILL_TICKS = 10;
    // Fluid going each way through the wormhole
    private final Flow toTarget = new Flow();
    private final Flow toDialer = new Flow();

    /** @return whether the connection is over and can be forgotten */
    boolean tick() {
        if (++this.age % SGConnection.KEEP_LOADED_TICKS == 0) {
            this.dialer.keepLoaded();
            this.target.keepLoaded();
        }
        if (this.phase == DialPhase.CONNECTED) {
            this.ticksOpen++;
            this.tickOwed += this.costs.tick();
            final long due = (long) this.tickOwed;
            this.tickOwed -= due;
            if (!this.dialer.energy().pay(due)) {
                // Out of energy: the wormhole shuts
                this.close(Cause.of(this.dialer.server()));
                return false;
            }
            this.travel();
            if (this.ticksOpen % SGConnection.SPILL_TICKS == 1) {
                this.spill();
            }
            if (this.ticksOpen % GateLight.HORIZON_TICKS == 0) {
                SGConnection.shimmer(this.dialer);
                SGConnection.shimmer(this.target);
            }
        }
        if (this.phase == DialPhase.TRANSIENT) {
            this.dialerTravel.kawoosh();
            this.targetTravel.kawoosh();
        }
        if (this.phase == DialPhase.ESTABLISHING && this.elapsed == (this.style == DialStyle.PEGASUS ? 0
                : SGConnection.MILKY_WAY_OPEN_SOUND_TICKS)) {
            this.dialer.play(SGSounds.open(this.style));
            this.target.play(SGSounds.open(this.style));
        }
        this.elapsed++;
        if (this.timeout > 0) {
            this.timeout--;
            return false;
        }
        switch (this.phase) {
            case DIALING -> {
                // Each chevron is paid for as it locks
                if (!this.dialer.energy().pay(this.costs.chevron())) {
                    this.fail(DialResult.NOT_ENOUGH_ENERGY);
                    return true;
                }
                this.engaged++;
                this.lockTick = this.now();
                SGEventBus.get().post(new SGEvents.DialLockChevron(this.cause, this.dialer, this.address,
                        this.engaged - 1));
                this.dialer.play(SGSounds.chevron(this.style, true));
                this.target.play(SGSounds.chevron(DialStyle.of(this.target.typeKey()), false));
                if (this.engaged < this.address.size()) {
                    this.enter(DialPhase.INTER_DIALING, this.style.interDialingTicks());
                } else {
                    this.enter(DialPhase.SYNC_AWAIT, SGConnection.SYNC_AWAIT_TICKS);
                }
            }
            case INTER_DIALING -> {
                this.symbol++;
                this.startSymbol();
            }
            case SYNC_AWAIT -> {
                // The wormhole is paid for as it starts to form, as in the original
                if (!this.dialer.energy().pay(this.costs.open())) {
                    this.fail(DialResult.NOT_ENOUGH_ENERGY);
                    return true;
                }
                this.enter(DialPhase.ESTABLISHING, this.style.establishingTicks());
            }
            case ESTABLISHING -> {
                this.enter(DialPhase.TRANSIENT, SGConnection.TRANSIENT_TICKS);
                this.result.complete(DialResult.CONNECTED);
                SGEventBus.get().post(new SGEvents.ConnectionOpen(this.cause, this));
            }
            case TRANSIENT -> this.enter(DialPhase.CONNECTED, this.openTicks > 0 ? this.openTicks : Integer.MAX_VALUE);
            case CONNECTED -> this.close(Cause.of(this.dialer.server()));
            case DISCONNECTING -> this.finish();
            default -> {
            }
        }
        return this.phase == DialPhase.IDLE;
    }

    /**
     * Shuts the connection down from either end. An open wormhole takes a moment to close; a dial still in progress
     * stops at once and ends {@link DialResult#INTERRUPTED}.
     */
    void close(final Cause cause) {
        this.closeCause = cause;
        switch (this.phase) {
            case TRANSIENT, CONNECTED -> {
                this.dialerTravel.clear();
                this.targetTravel.clear();
                this.stopSpilling();
                this.enter(DialPhase.DISCONNECTING, SGConnection.DISCONNECTING_TICKS);
                this.dialer.play(SGSounds.CLOSE);
                this.target.play(SGSounds.CLOSE);
            }
            case DISCONNECTING, IDLE -> {
            }
            default -> this.fail(DialResult.INTERRUPTED);
        }
    }

    // A dial that ends before the wormhole opens
    private void fail(final DialResult result) {
        this.result.complete(result);
        this.dialer.play(SGSounds.fail(this.style));
        this.target.play(SGSounds.fail(DialStyle.of(this.target.typeKey())));
        this.finish();
    }

    /** One of the gates was broken: the other goes back to idle at once. */
    void gone() {
        this.result.complete(DialResult.GATE_GONE);
        this.finish();
    }

    /** @return who dialed */
    public Cause cause() {
        return this.cause;
    }

    /** @return the gate that dialed */
    SGStargate dialerGate() {
        return this.dialer;
    }

    boolean isClosing() {
        return this.phase == DialPhase.DISCONNECTING || this.phase == DialPhase.IDLE;
    }

    DialPhase phase() {
        return this.phase;
    }

    // Travelers go from the dialing gate; from the dialed gate too unless travel is one way, which it is unless
    // the dialing gate says otherwise
    private void travel() {
        final boolean oneWay = this.dialer.get(GateProperties.ONE_WAY_TRAVEL).orElse(true);
        final boolean reverseKills = this.dialer.get(GateProperties.REVERSE_TRAVEL_KILLS).orElse(false);
        this.dialerTravel.tick(this.target, true, false);
        this.targetTravel.tick(this.dialer, !oneWay, reverseKills);
    }

    // Fluid goes the way travelers do: from the dialing gate, and back from the dialed one unless travel is one way
    private void spill() {
        // The event horizon fills what is empty of each opening first, so fluid goes into and out of a gate's front
        // only. A gate takes fluid in where it sends, so long as no iris is shut and it isn't pouring out what came
        // the other way
        final boolean oneWay = this.dialer.get(GateProperties.ONE_WAY_TRAVEL).orElse(true);
        final boolean open = !this.dialer.irisBlocks() && !this.target.irisBlocks();
        final List<BlockPos> toTarget = List.copyOf(this.toTarget.spilled);
        final List<BlockPos> toDialer = List.copyOf(this.toDialer.spilled);
        SGConnection.seal(this.dialer, open && this.toDialer.spilled.isEmpty());
        SGConnection.seal(this.target, open && !oneWay && this.toTarget.spilled.isEmpty());
        SGConnection.pour(this.dialer, this.target, this.toTarget, this.toDialer, true);
        SGConnection.pour(this.target, this.dialer, this.toDialer, this.toTarget, !oneWay);
        if (!toTarget.equals(this.toTarget.spilled) || !toDialer.equals(this.toDialer.spilled)) {
            // Saved again, so the fluid put out still goes once the wormhole closes after a restart
            this.dialer.store().putConnection(this.dialer.position(), this.save());
        }
    }

    private static void seal(final SGStargate gate, final boolean sending) {
        final ServerLevel level = gate.level();
        if (level != null) {
            EventHorizonBlock.seal(level, gate.frame(), sending, gate.now());
        }
    }

    // The horizon's light moves on with its waves; behind a shut iris it gives none
    private static void shimmer(final SGStargate gate) {
        final ServerLevel level = gate.level();
        if (level != null) {
            EventHorizonBlock.shimmer(level, gate.frame(), gate.now(), gate.irisBlocks());
        }
    }

    // A gate standing in fluid pours it out of the other gate, as much as it is deep in it, unless an iris is shut or
    // the other gate stands in fluid of its own; fluid it poured itself doesn't count
    private static void pour(final SGStargate from, final SGStargate to, final Flow flow, final Flow back,
            final boolean allowed) {
        final List<BlockPos> spilled = flow.spilled;
        final ServerLevel fromLevel = from.level();
        final ServerLevel toLevel = to.level();
        Optional<FluidSpill> spill = Optional.empty();
        if (allowed && fromLevel != null && toLevel != null && !from.irisBlocks() && !to.irisBlocks()) {
            spill = FluidSpill.in(fromLevel, from.frame()).filter(s -> s.amount() > 0);
            if (spill.isPresent() && spilled.isEmpty() && FluidSpill.in(toLevel, to.frame()).isPresent()) {
                spill = Optional.empty();
            }
        }
        to.showSpill(spill.orElse(null));
        if (toLevel == null) {
            return;
        }
        if (spill.isPresent() && from.get(GateProperties.SPILLS_FLUID).orElse(true)) {
            SGConnection.draw(fromLevel, from, spill.get(), flow, back);
        } else {
            flow.drawn = 0;
        }
        if (spill.isEmpty() || !to.get(GateProperties.SPILLS_FLUID).orElse(true)) {
            SGConnection.clear(toLevel, spilled);
            return;
        }
        // Real fluid, as many blocks as it covers of the near gate's opening, put only where there is nothing; those
        // it no longer covers go again
        final List<BlockPos> pouring = spill.get().blocks(to.frame());
        final List<BlockPos> stopped = new ArrayList<>(spilled);
        stopped.removeAll(pouring);
        spilled.removeAll(stopped);
        SGConnection.clear(toLevel, stopped);
        final BlockState state = spill.get().fluid().defaultFluidState().createLegacyBlock();
        for (final BlockPos pos : pouring) {
            if (!spilled.contains(pos) && toLevel.getBlockState(pos).isAir()) {
                toLevel.setBlock(pos, state, Block.UPDATE_ALL);
                spilled.add(pos);
            }
        }
    }

    // The near side gives up what goes through, from the top of its body of fluid down, a whole layer at a time so the
    // rest can't fill it again; the sea and other endless bodies give it up without losing any
    private static void draw(final ServerLevel level, final SGStargate from, final FluidSpill spill, final Flow flow,
            final Flow back) {
        final Optional<Set<BlockPos>> body = spill.body(level, from.frame(), back.spilled);
        if (body.isEmpty() || body.get().isEmpty()) {
            flow.drawn = 0;
            return;
        }
        flow.drawn += spill.blocks(from.frame()).size();
        final List<BlockPos> layer = FluidSpill.topLayer(body.get());
        if (flow.drawn >= layer.size()) {
            flow.drawn -= layer.size();
            FluidSpill.drain(level, layer);
        }
    }

    private void stopSpilling() {
        for (final SGStargate gate : List.of(this.dialer, this.target)) {
            gate.showSpill(null);
            final ServerLevel level = gate.level();
            if (level != null) {
                SGConnection.clear(level, gate == this.dialer ? this.toDialer.spilled : this.toTarget.spilled);
                EventHorizonBlock.unseal(level, gate.frame());
            }
        }
        this.toTarget.drawn = 0;
        this.toDialer.drawn = 0;
    }

    // Takes away the fluid a gate put out, where it is still the source it put there
    private static void clear(final ServerLevel level, final List<BlockPos> spilled) {
        for (final BlockPos pos : spilled) {
            final BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof LiquidBlock && state.getFluidState().isSource()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        spilled.clear();
    }

    private void finish() {
        this.stopSpilling();
        final boolean opened = this.result.isDone() && this.result.join() == DialResult.CONNECTED;
        this.phase = DialPhase.IDLE;
        this.dialer.store().removeConnection(this.dialer.position());
        this.dialer.leave(this, this.closeCause);
        this.target.leave(this, this.closeCause);
        this.dialer.irisState().restore(this.closeCause);
        this.target.irisState().restore(this.closeCause);
        if (opened) {
            SGEventBus.get().post(new SGEvents.ConnectionClose(this.closeCause, this));
        }
    }

    private void sync() {
        final long ringEnd = this.move.ticks() > 0 && this.phase == DialPhase.DIALING
                ? this.phaseStart + this.move.ticks() : this.phaseStart;
        final DialView dialerView = new DialView(this.phase, true, SGConnection.characters(this.address),
                this.engaged, this.move.from(), this.move.to(), this.phaseStart, ringEnd, this.lockTick,
                this.fillStart, this.fillEnd, this.phaseStart, 0);
        final DialView targetView = new DialView(this.phase, false, this.homeAddress, this.engaged, 0, 0, 0, 0,
                this.lockTick, this.fillStart, this.fillEnd, this.phaseStart, 0);
        this.dialer.enter(this, dialerView, this.closeCause);
        this.target.enter(this, targetView, this.closeCause);
    }

    MinecraftServer server() {
        return this.dialer.server();
    }

    /** @return the connection as it is saved: where it is in its dial, by game time, so it can carry on */
    CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.store("dialer", GlobalPos.CODEC, this.dialer.position());
        tag.store("target", GlobalPos.CODEC, this.target.position());
        tag.store("address", Address.CODEC, this.address);
        tag.putString("home_address", this.homeAddress);
        tag.putInt("open_ticks", this.openTicks);
        tag.putString("phase", this.phase.name());
        tag.putLong("phase_start", this.phaseStart);
        tag.putInt("phase_ticks", this.phaseTicks);
        tag.putInt("symbol", this.symbol);
        tag.putInt("engaged", this.engaged);
        tag.putFloat("ring_angle", this.ringAngle);
        tag.putFloat("move_from", this.move.from());
        tag.putFloat("move_to", this.move.to());
        tag.putInt("move_ticks", this.move.ticks());
        tag.putLong("lock_tick", this.lockTick);
        tag.putLong("fill_start", this.fillStart);
        tag.putLong("fill_end", this.fillEnd);
        tag.putLong("chevron_cost", this.costs.chevron());
        tag.putLong("open_cost", this.costs.open());
        tag.putDouble("tick_cost", this.costs.tick());
        tag.putDouble("tick_owed", this.tickOwed);
        tag.putLongArray("spilled_to_target", this.toTarget.save());
        tag.putLongArray("spilled_to_dialer", this.toDialer.save());
        return tag;
    }

    /**
     * A connection saved under way, carrying on from where it was: the time it had left in its step is what it has
     * left now, as game time stops while the server does.
     *
     * @return the connection, or empty if either gate is gone or the save can't be read
     */
    static Optional<SGConnection> restore(final SGStargates stargates, final CompoundTag tag) {
        final Optional<SGStargate> dialer = tag.read("dialer", GlobalPos.CODEC).flatMap(stargates::gate);
        final Optional<SGStargate> target = tag.read("target", GlobalPos.CODEC).flatMap(stargates::gate);
        final Optional<Address> address = tag.read("address", Address.CODEC);
        final Optional<DialPhase> phase = tag.getString("phase").flatMap(name -> {
            try {
                return Optional.of(DialPhase.valueOf(name));
            } catch (final IllegalArgumentException e) {
                return Optional.empty();
            }
        });
        if (dialer.isEmpty() || target.isEmpty() || address.isEmpty() || phase.isEmpty()
                || phase.get() == DialPhase.IDLE) {
            return Optional.empty();
        }
        final SGConnection connection = new SGConnection(dialer.get(), target.get(), address.get(),
                Address.parse(tag.getStringOr("home_address", "")), tag.getIntOr("open_ticks", 0),
                Cause.of(stargates.server()), new Costs(tag.getLongOr("chevron_cost", 0),
                        tag.getLongOr("open_cost", 0), tag.getDoubleOr("tick_cost", 0)));
        connection.tickOwed = tag.getDoubleOr("tick_owed", 0);
        connection.phase = phase.get();
        connection.phaseStart = tag.getLongOr("phase_start", connection.now());
        connection.phaseTicks = tag.getIntOr("phase_ticks", 0);
        connection.elapsed = (int) Math.max(0, Math.min(Integer.MAX_VALUE, connection.now() - connection.phaseStart));
        connection.timeout = Math.max(0, connection.phaseTicks - connection.elapsed);
        connection.symbol = tag.getIntOr("symbol", 0);
        connection.engaged = tag.getIntOr("engaged", 0);
        connection.ringAngle = tag.getFloatOr("ring_angle", 0);
        connection.move = new DialStyle.Move(tag.getFloatOr("move_from", 0), tag.getFloatOr("move_to", 0),
                tag.getIntOr("move_ticks", 0));
        connection.lockTick = tag.getLongOr("lock_tick", 0);
        connection.fillStart = tag.getLongOr("fill_start", 0);
        connection.fillEnd = tag.getLongOr("fill_end", 0);
        tag.getLongArray("spilled_to_target").ifPresent(connection.toTarget::load);
        tag.getLongArray("spilled_to_dialer").ifPresent(connection.toDialer::load);
        if (connection.phase == DialPhase.CONNECTED) {
            connection.ticksOpen = connection.elapsed;
        }
        return Optional.of(connection);
    }

    /**
     * What a connection costs the dialing gate.
     *
     * @param chevron the energy taken as each chevron locks
     * @param open the energy taken as the wormhole forms
     * @param tick the energy taken each tick the wormhole stays open, which may be a fraction
     */
    record Costs(long chevron, long open, double tick) {

        static long times(final long a, final long b) {
            return b != 0 && a > Long.MAX_VALUE / b ? Long.MAX_VALUE : a * b;
        }

        static long add(final long a, final long b) {
            final long sum = a + b;
            return sum < 0 ? Long.MAX_VALUE : sum;
        }
    }

    /** Picks up a restored connection: both gates show where it is, and their chunks stay loaded. */
    void resume() {
        this.dialer.keepLoaded();
        this.target.keepLoaded();
        this.sync();
    }

    @Override
    public Stargate dialer() {
        return this.dialer;
    }

    @Override
    public Stargate target() {
        return this.target;
    }

    @Override
    public Address address() {
        return this.address;
    }

    @Override
    public Optional<Stargate> other(final Stargate gate) {
        if (gate.equals(this.dialer)) {
            return Optional.of(this.target);
        }
        if (gate.equals(this.target)) {
            return Optional.of(this.dialer);
        }
        return Optional.empty();
    }

    @Override
    public int ticksOpen() {
        return this.ticksOpen;
    }

    @Override
    public OptionalInt ticksRemaining() {
        if (this.openTicks <= 0) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(Math.max(0, this.openTicks - this.ticksOpen));
    }

    // Fluid going one way through the wormhole: what it put out at the far gate, to take away again once the wormhole
    // closes, and how much has gone from the near side since it last lost a layer
    private static final class Flow {
        private final List<BlockPos> spilled = new ArrayList<>();
        private int drawn;

        private long[] save() {
            return this.spilled.stream().mapToLong(BlockPos::asLong).toArray();
        }

        private void load(final long[] positions) {
            for (final long position : positions) {
                this.spilled.add(BlockPos.of(position));
            }
        }
    }
}
