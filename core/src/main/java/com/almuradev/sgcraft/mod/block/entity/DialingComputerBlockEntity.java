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

package com.almuradev.sgcraft.mod.block.entity;

import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.ClientTicks;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.computer.ComputerDialMode;
import com.almuradev.sgcraft.mod.computer.ComputerPower;
import com.almuradev.sgcraft.mod.computer.ComputerSettings;
import com.almuradev.sgcraft.mod.computer.DialingComputers;
import com.almuradev.sgcraft.mod.computer.PoweredComputer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A dialing computer's link to its gate: the nearest gate of any type within the server's
 * {@link ComputerSettings#linkRange() link range}. It finds one when placed, and finds another if its gate is
 * broken. Clients are told which gate it is, so the computer's screen can show that gate dialing.
 *
 * <p>It also keeps how it dials, and whether its alarm is off. The alarm sounds while a wormhole comes in to its
 * gate, unless someone at the computer has turned it off; it stays off, or on, until someone changes it, whether a
 * wormhole is coming in or not.</p>
 *
 * <p>It keeps the addresses last dialed from it too, newest first, as many as the server's
 * {@link ComputerSettings#historySize() history size}, for its screen to list and dial again.</p>
 */
public final class DialingComputerBlockEntity extends BlockEntity implements PoweredComputer {

    // How often a computer checks its link
    private static final int CHECK_TICKS = 20;

    private @Nullable BlockPos linked;
    private ComputerDialMode mode = ComputerDialMode.GATE;
    private boolean alarmOff;
    private final ComputerPower power = new ComputerPower();
    private List<Dialed> history = List.of();

    /**
     * An address dialed from the computer.
     *
     * @param address the address, in its characters, as typed
     * @param connected whether the dial opened a wormhole
     */
    public record Dialed(String address, boolean connected) {

        public static final Codec<Dialed> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("address").forGetter(Dialed::address),
                Codec.BOOL.fieldOf("connected").forGetter(Dialed::connected)
        ).apply(instance, Dialed::new));
    }

    public DialingComputerBlockEntity(final BlockPos pos, final BlockState state) {
        super(SGBlockEntityTypes.DIALING_COMPUTER, pos, state);
    }

    /** @return the base of the gate the computer dials, in the computer's world, if it is linked */
    public Optional<BlockPos> linked() {
        return Optional.ofNullable(this.linked);
    }

    /**
     * The gate the computer dials: its linked gate while that lasts and is in reach, or else the nearest in reach,
     * which it links to then. Only on the server.
     *
     * @return the gate, if there is one in reach
     */
    public Optional<Stargate> gate() {
        if (!(this.level instanceof final ServerLevel serverLevel)) {
            return Optional.empty();
        }
        final Optional<Stargate> linked = this.linkedGate();
        if (linked.isPresent()) {
            return linked;
        }
        this.relink(serverLevel);
        return this.linkedGate();
    }

    private Optional<Stargate> linkedGate() {
        if (this.linked == null || this.level == null) {
            return Optional.empty();
        }
        final double range = ComputerSettings.current().linkRange();
        return SGServer.current()
                .flatMap(server -> server.stargates().gate(GlobalPos.of(this.level.dimension(), this.linked)))
                .filter(gate -> gate.position().pos().closerThan(this.worldPosition, range))
                .map(Stargate.class::cast);
    }

    /** @return how the computer dials */
    public ComputerDialMode mode() {
        return this.mode;
    }

    /** Has the computer dial another way from now on. */
    public void setMode(final ComputerDialMode mode) {
        if (this.mode != mode) {
            this.mode = mode;
            this.changed();
        }
    }

    /** @return the addresses last dialed from the computer, newest first */
    public List<Dialed> history() {
        return this.history;
    }

    /**
     * Puts an address dialed from the computer at the top of its history, taking it from lower down if it was there,
     * and lets go of the oldest beyond the server's history size.
     */
    public void remember(final String address, final boolean connected) {
        final List<Dialed> history = new ArrayList<>();
        history.add(new Dialed(address, connected));
        this.history.stream().filter(dialed -> !dialed.address().equals(address)).forEach(history::add);
        this.history = List.copyOf(history.subList(0, Math.min(history.size(),
                ComputerSettings.current().historySize())));
        this.changed();
    }

    /** @return whether a wormhole is coming in to the computer's gate, as the gate's base shows it */
    public boolean incoming() {
        return this.linked != null && this.level != null
                && this.level.getBlockEntity(this.linked) instanceof final StargateBaseBlockEntity base
                && base.isFormed() && base.dialView().active() && !base.dialView().initiator();
    }

    /** @return whether the alarm has been turned off */
    public boolean alarmOff() {
        return this.alarmOff;
    }

    /**
     * @return whether the alarm sounds now: the computer runs, a wormhole is coming in, and no one has turned it off
     */
    public boolean alarming() {
        return this.running() && this.incoming() && !this.alarmOff;
    }

    @Override
    public ComputerPower power() {
        return this.power;
    }

    @Override
    public void setOn(final boolean on) {
        if (this.level == null) {
            return;
        }
        this.power.setOn(this.level, this.worldPosition, on);
        this.changed();
        if (!this.running() && this.level instanceof final ServerLevel serverLevel) {
            DialingComputers.shutDown(serverLevel, this.worldPosition);
        }
    }

    /** Turns the alarm off, or on again; it stays that way until changed. */
    public void setAlarmOff(final boolean off) {
        if (this.alarmOff != off) {
            this.alarmOff = off;
            this.changed();
        }
    }

    /**
     * Keeps the computer linked, to its gate while that lasts or else to the nearest gate in reach, pays for its
     * running, ending everyone's turn at it if it loses power, and ends the turn of anyone who walked away from it.
     */
    public static void serverTick(final ServerLevel level, final BlockPos pos,
            final DialingComputerBlockEntity computer) {
        if (level.getGameTime() % DialingComputerBlockEntity.CHECK_TICKS == 0) {
            computer.gate();
        }
        if (level.getGameTime() % ComputerPower.CHECK_TICKS == 0 && computer.power.pay(level, pos)) {
            computer.changed();
            if (!computer.running()) {
                DialingComputers.shutDown(level, pos);
            }
        }
        DialingComputers.tick(level, pos);
    }

    /** Keeps the alarm sounding while it should. Only clients call this. */
    public static void clientTick(final DialingComputerBlockEntity computer) {
        ClientTicks.computer(computer);
    }

    private void relink(final ServerLevel level) {
        final BlockPos pos = this.worldPosition;
        final double range = ComputerSettings.current().linkRange();
        final BlockPos nearest = SGServer.current()
                .flatMap(server -> server.stargates().in(level.dimension()).stream()
                        .map(gate -> gate.position().pos())
                        .filter(gate -> gate.closerThan(pos, range))
                        .min(Comparator.comparingDouble(gate -> gate.distSqr(pos))))
                .orElse(null);
        this.setLinked(nearest);
    }

    private void setLinked(final @Nullable BlockPos linked) {
        if (Objects.equals(this.linked, linked)) {
            return;
        }
        this.linked = linked;
        this.changed();
    }

    // Saves the computer, and tells clients what changed
    private void changed() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        this.linked = input.read("linked", BlockPos.CODEC).orElse(null);
        this.mode = input.read("mode", ComputerDialMode.CODEC).orElse(ComputerDialMode.GATE);
        this.alarmOff = input.getBooleanOr("alarm_off", false);
        this.power.load(input);
        this.history = input.read("history", Dialed.CODEC.listOf()).orElse(List.of());
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        if (this.linked != null) {
            output.store("linked", BlockPos.CODEC, this.linked);
        }
        output.store("mode", ComputerDialMode.CODEC, this.mode);
        output.putBoolean("alarm_off", this.alarmOff);
        this.power.save(output);
        output.store("history", Dialed.CODEC.listOf(), this.history);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        if (this.linked != null) {
            tag.store("linked", BlockPos.CODEC, this.linked);
        }
        tag.store("mode", ComputerDialMode.CODEC, this.mode);
        tag.putBoolean("alarm_off", this.alarmOff);
        this.power.write(tag);
        tag.store("history", Dialed.CODEC.listOf(), this.history);
        return tag;
    }
}
