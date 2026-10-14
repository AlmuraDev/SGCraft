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

import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.address.Symbol;
import com.almuradev.sgcraft.api.gate.Connection;
import com.almuradev.sgcraft.api.gate.DialMode;
import com.almuradev.sgcraft.api.gate.DialResult;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.gate.ChevronOrder;
import com.almuradev.sgcraft.gate.DialPhase;
import com.almuradev.sgcraft.gate.DialStyle;
import com.almuradev.sgcraft.gate.DialView;
import com.almuradev.sgcraft.sound.SGSounds;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;

/**
 * A link between two gates, from the first symbol to the wormhole closing, with the timings of the original
 * SGCraft. The server ticks it once a tick, and it moves both gates through the {@linkplain DialPhase steps of a
 * dial}, the dialling gate's type setting the pace. Both gates lock each chevron in the same tick.
 */
public final class SGConnection implements Connection {

    // How long the gates wait on each other once every chevron has locked, how long the kawoosh lasts, and how long
    // the wormhole takes to shut down
    private static final int SYNC_AWAIT_TICKS = 10;
    private static final int TRANSIENT_TICKS = 20;
    private static final int DISCONNECTING_TICKS = 40;
    // How long a dialled Pegasus gate takes to fill with light, at most
    private static final int PEGASUS_FILL_TICKS = 90;
    // How often the two gates' chunks are kept loaded; less than the loading ticket's lifetime
    private static final int KEEP_LOADED_TICKS = 100;
    // How long into the wormhole forming its sound starts, so the sound's burst meets the kawoosh
    private static final int MILKY_WAY_OPEN_SOUND_TICKS = 5;

    private final SGStargate dialler;
    private final SGStargate target;
    private final Address address;
    private final String homeAddress;
    private final int openTicks;
    private final DialStyle style;
    private final int[] positions;
    private final CompletableFuture<DialResult> result = new CompletableFuture<>();
    private final GateTravel diallerTravel;
    private final GateTravel targetTravel;
    private DialPhase phase = DialPhase.DIALLING;
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

    SGConnection(final SGStargate dialler, final SGStargate target, final Address address,
            final Optional<Address> homeAddress, final int openTicks) {
        this.dialler = dialler;
        this.target = target;
        this.address = address;
        this.homeAddress = homeAddress.map(SGConnection::characters).orElse(SGConnection.characters(address));
        this.openTicks = openTicks;
        this.style = DialStyle.of(dialler.typeKey());
        this.positions = ChevronOrder.positions(dialler.chevrons(), address.size());
        this.diallerTravel = new GateTravel(dialler);
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
        return this.dialler.server().overworld().getGameTime();
    }

    void start(final DialMode mode) {
        this.dialler.keepLoaded();
        this.target.keepLoaded();
        if (mode == DialMode.QUICK) {
            // Every chevron locks at once, with no ring moving, as the original's "chevrons lock on dial" did
            this.engaged = this.address.size();
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

    // When a dialled Pegasus gate fills with light: so it is full as the last chevron locks
    private void scheduleFill() {
        DialStyle.Move move = new DialStyle.Move(0, 0, 0);
        float angle = 0;
        long ticks = 0;
        for (int i = 0; i < this.address.size(); i++) {
            move = this.style.move(angle, this.address.get(i), i, this.positions);
            angle = move.to();
            ticks += move.ticks() + 1;
            if (i + 1 < this.address.size()) {
                ticks += this.style.interDiallingTicks() + 1;
            }
        }
        this.fillEnd = this.now() + ticks;
        this.fillStart = Math.max(this.now(), this.fillEnd - SGConnection.PEGASUS_FILL_TICKS);
    }

    private void startSymbol() {
        final Symbol next = this.address.get(this.symbol);
        this.move = this.style.move(this.ringAngle, next, this.symbol, this.positions);
        this.ringAngle = this.move.to();
        this.enter(DialPhase.DIALLING, this.move.ticks());
    }

    private void enter(final DialPhase phase, final int ticks) {
        this.phase = phase;
        this.timeout = ticks;
        this.phaseTicks = ticks;
        this.elapsed = 0;
        this.phaseStart = this.now();
        this.sync();
        this.dialler.store().putConnection(this.dialler.position(), this.save());
    }

    /** @return whether the connection is over and can be forgotten */
    boolean tick() {
        if (++this.age % SGConnection.KEEP_LOADED_TICKS == 0) {
            this.dialler.keepLoaded();
            this.target.keepLoaded();
        }
        if (this.phase == DialPhase.CONNECTED) {
            this.ticksOpen++;
            this.travel();
        }
        if (this.phase == DialPhase.TRANSIENT) {
            this.diallerTravel.kawoosh();
            this.targetTravel.kawoosh();
        }
        if (this.phase == DialPhase.ESTABLISHING && this.elapsed == (this.style == DialStyle.PEGASUS ? 0
                : SGConnection.MILKY_WAY_OPEN_SOUND_TICKS)) {
            this.dialler.play(SGSounds.open(this.style));
            this.target.play(SGSounds.open(this.style));
        }
        this.elapsed++;
        if (this.timeout > 0) {
            this.timeout--;
            return false;
        }
        switch (this.phase) {
            case DIALLING -> {
                this.engaged++;
                this.lockTick = this.now();
                this.dialler.play(SGSounds.chevron(this.style, true));
                this.target.play(SGSounds.chevron(DialStyle.of(this.target.typeKey()), false));
                if (this.engaged < this.address.size()) {
                    this.enter(DialPhase.INTER_DIALLING, this.style.interDiallingTicks());
                } else {
                    this.enter(DialPhase.SYNC_AWAIT, SGConnection.SYNC_AWAIT_TICKS);
                }
            }
            case INTER_DIALLING -> {
                this.symbol++;
                this.startSymbol();
            }
            case SYNC_AWAIT -> this.enter(DialPhase.ESTABLISHING, this.style.establishingTicks());
            case ESTABLISHING -> {
                this.enter(DialPhase.TRANSIENT, SGConnection.TRANSIENT_TICKS);
                this.result.complete(DialResult.CONNECTED);
            }
            case TRANSIENT -> this.enter(DialPhase.CONNECTED, this.openTicks > 0 ? this.openTicks : Integer.MAX_VALUE);
            case CONNECTED -> this.close();
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
    void close() {
        switch (this.phase) {
            case TRANSIENT, CONNECTED -> {
                this.diallerTravel.clear();
                this.targetTravel.clear();
                this.enter(DialPhase.DISCONNECTING, SGConnection.DISCONNECTING_TICKS);
                this.dialler.play(SGSounds.CLOSE);
                this.target.play(SGSounds.CLOSE);
            }
            case DISCONNECTING, IDLE -> {
            }
            default -> {
                this.result.complete(DialResult.INTERRUPTED);
                this.dialler.play(SGSounds.fail(this.style));
                this.target.play(SGSounds.fail(DialStyle.of(this.target.typeKey())));
                this.finish();
            }
        }
    }

    /** One of the gates was broken: the other goes back to idle at once. */
    void gone() {
        this.result.complete(DialResult.GATE_GONE);
        this.finish();
    }

    boolean isClosing() {
        return this.phase == DialPhase.DISCONNECTING || this.phase == DialPhase.IDLE;
    }

    DialPhase phase() {
        return this.phase;
    }

    // Travellers go from the dialling gate; from the dialled gate too unless travel is one way, which it is unless
    // the dialling gate says otherwise
    private void travel() {
        final boolean oneWay = this.dialler.get(GateProperties.ONE_WAY_TRAVEL).orElse(true);
        final boolean reverseKills = this.dialler.get(GateProperties.REVERSE_TRAVEL_KILLS).orElse(false);
        this.diallerTravel.tick(this.target, true, false);
        this.targetTravel.tick(this.dialler, !oneWay, reverseKills);
    }

    private void finish() {
        this.phase = DialPhase.IDLE;
        this.dialler.store().removeConnection(this.dialler.position());
        this.dialler.leave(this);
        this.target.leave(this);
    }

    private void sync() {
        final long ringEnd = this.move.ticks() > 0 && this.phase == DialPhase.DIALLING
                ? this.phaseStart + this.move.ticks() : this.phaseStart;
        final DialView diallerView = new DialView(this.phase, true, SGConnection.characters(this.address),
                this.engaged, this.move.from(), this.move.to(), this.phaseStart, ringEnd, this.lockTick,
                this.fillStart, this.fillEnd, this.phaseStart);
        final DialView targetView = new DialView(this.phase, false, this.homeAddress, this.engaged, 0, 0, 0, 0,
                this.lockTick, this.fillStart, this.fillEnd, this.phaseStart);
        this.dialler.enter(this, diallerView);
        this.target.enter(this, targetView);
    }

    MinecraftServer server() {
        return this.dialler.server();
    }

    /** @return the connection as it is saved: where it is in its dial, by game time, so it can carry on */
    CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.store("dialler", GlobalPos.CODEC, this.dialler.position());
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
        return tag;
    }

    /**
     * A connection saved under way, carrying on from where it was: the time it had left in its step is what it has
     * left now, as game time stops while the server does.
     *
     * @return the connection, or empty if either gate is gone or the save can't be read
     */
    static Optional<SGConnection> restore(final SGStargates stargates, final CompoundTag tag) {
        final Optional<SGStargate> dialler = tag.read("dialler", GlobalPos.CODEC).flatMap(stargates::gate);
        final Optional<SGStargate> target = tag.read("target", GlobalPos.CODEC).flatMap(stargates::gate);
        final Optional<Address> address = tag.read("address", Address.CODEC);
        final Optional<DialPhase> phase = tag.getString("phase").flatMap(name -> {
            try {
                return Optional.of(DialPhase.valueOf(name));
            } catch (final IllegalArgumentException e) {
                return Optional.empty();
            }
        });
        if (dialler.isEmpty() || target.isEmpty() || address.isEmpty() || phase.isEmpty()
                || phase.get() == DialPhase.IDLE) {
            return Optional.empty();
        }
        final SGConnection connection = new SGConnection(dialler.get(), target.get(), address.get(),
                Address.parse(tag.getStringOr("home_address", "")), tag.getIntOr("open_ticks", 0));
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
        if (connection.phase == DialPhase.CONNECTED) {
            connection.ticksOpen = connection.elapsed;
        }
        return Optional.of(connection);
    }

    /** Picks up a restored connection: both gates show where it is, and their chunks stay loaded. */
    void resume() {
        this.dialler.keepLoaded();
        this.target.keepLoaded();
        this.sync();
    }

    @Override
    public Stargate dialler() {
        return this.dialler;
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
        if (gate.equals(this.dialler)) {
            return Optional.of(this.target);
        }
        if (gate.equals(this.target)) {
            return Optional.of(this.dialler);
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
}
