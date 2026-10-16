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
import com.almuradev.sgcraft.api.access.GateRight;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.Iris;
import com.almuradev.sgcraft.api.gate.IrisState;
import com.almuradev.sgcraft.api.gate.IrisType;
import com.almuradev.sgcraft.api.gate.IrisTypes;
import com.almuradev.sgcraft.mod.event.SGEventBus;
import com.almuradev.sgcraft.mod.event.SGEvents;
import com.almuradev.sgcraft.mod.sound.SGSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;

import java.util.Locale;

/**
 * A gate's iris, as in the original SGCraft: it takes {@link #MOVE_TICKS} to open or close, and stops travelers once
 * it is half shut. Where it is lives in the gate's saved state, as the state it is in and the game time it got there,
 * so it carries on across restarts.
 *
 * <p>A shield works the same way, as in the original: it is closed to raise it and opened to lower it. It rises and
 * falls in {@link #SHIELD_TICKS}, and while it is rising or raised it takes energy from the gate each tick, unless
 * the gate needs none. It can't be raised without enough for a tick, and falls when the energy runs out.</p>
 */
public final class SGIris implements Iris {

    /** How long the iris takes to open or close, in ticks. */
    public static final int MOVE_TICKS = 70;
    /** How long a shield takes to rise or fall, in ticks. */
    public static final int SHIELD_TICKS = 10;

    private static final String STATE = "iris_state";
    private static final String SINCE = "iris_since";
    private static final String BEFORE = "iris_before";
    // The part of a unit of a raised shield's energy not paid yet, paid once the fractions add up to a whole unit
    private static final String OWED = "shield_owed";

    private final SGStargate gate;

    SGIris(final SGStargate gate) {
        this.gate = gate;
    }

    @Override
    public ResourceKey<IrisType> type() {
        return this.gate.get(GateProperties.IRIS).orElse(IrisTypes.METAL);
    }

    @Override
    public IrisState state() {
        return SGIris.stateOf(this.gate.saved());
    }

    /** @return the game time the iris got to its state */
    long since() {
        return this.gate.saved().getLongOr(SGIris.SINCE, 0);
    }

    /** @return how long an iris of the type takes to open or close, in ticks */
    public static int moveTicks(final ResourceKey<IrisType> type) {
        return SGIris.isShield(type) ? SGIris.SHIELD_TICKS : SGIris.MOVE_TICKS;
    }

    /** @return whether the type is a shield, which rises and falls rather than closing and opening */
    public static boolean isShield(final ResourceKey<IrisType> type) {
        return type.identifier().equals(IrisTypes.SHIELD.identifier());
    }

    /** @return a shield's state as players read it: raised, rising, lowered or falling */
    public static Component shieldText(final IrisState state) {
        return switch (state) {
            case CLOSED -> Component.translatableWithFallback("screen.sgcraft.shield.active", "Shield Active");
            case CLOSING -> Component.translatableWithFallback("screen.sgcraft.shield.raising", "Raising Shield...");
            case OPENING -> Component.translatableWithFallback("screen.sgcraft.shield.lowering", "Lowering Shield...");
            case OPEN -> Component.translatableWithFallback("screen.sgcraft.shield.inactive", "Shield Inactive");
        };
    }

    static IrisState stateOf(final CompoundTag state) {
        return state.getString(SGIris.STATE).flatMap(name -> {
            try {
                return java.util.Optional.of(IrisState.valueOf(name));
            } catch (final IllegalArgumentException e) {
                return java.util.Optional.empty();
            }
        }).orElse(IrisState.OPEN);
    }

    /**
     * How far open the iris is at a time, from 0 shut to 1 open.
     *
     * @param type the kind of iris, which says how long it takes to move
     * @param state where the iris is in its movement
     * @param since when it got there
     * @param time the time to ask about, in ticks, which may have a fraction
     */
    public static float aperture(final ResourceKey<IrisType> type, final IrisState state, final long since,
            final double time) {
        final double moved = Math.max(0, Math.min(1, (time - since) / SGIris.moveTicks(type)));
        return (float) switch (state) {
            case OPEN -> 1;
            case CLOSED -> 0;
            case OPENING -> moved;
            case CLOSING -> 1 - moved;
        };
    }

    /** @return whether the iris stops travelers now: once it is half shut, as in the original */
    public boolean blocks() {
        return SGIris.aperture(this.type(), this.state(), this.since(), this.gate.now()) <= 0.5F;
    }

    @Override
    public ActionResult open(final Cause cause) {
        return this.move(IrisState.OPEN, IrisState.OPENING, cause);
    }

    @Override
    public ActionResult close(final Cause cause) {
        return this.move(IrisState.CLOSED, IrisState.CLOSING, cause);
    }

    private ActionResult move(final IrisState target, final IrisState moving, final Cause cause) {
        if (!this.gate.exists() || this.gate.get(GateProperties.IRIS).isEmpty()) {
            return ActionResult.NOT_POSSIBLE;
        }
        final IrisState now = this.state();
        if (now == target || now == moving) {
            return ActionResult.NO_CHANGE;
        }
        if (!this.gate.access().allows(cause, GateRight.IRIS)) {
            return ActionResult.DENIED;
        }
        final boolean shield = SGIris.isShield(this.type());
        // A shield can't be raised without the energy to hold it up for a tick
        if (shield && target == IrisState.CLOSED && !this.canRaise()) {
            return ActionResult.NOT_POSSIBLE;
        }
        if (!SGEventBus.get().allows(new SGEvents.IrisPre(cause, this.gate, this, target))) {
            return ActionResult.CANCELED;
        }
        // Turning back part way starts from where the iris has got to, so it doesn't jump
        final float aperture = SGIris.aperture(this.type(), now, this.since(), this.gate.now());
        final float done = target == IrisState.OPEN ? aperture : 1 - aperture;
        final long since = this.gate.now() - Math.round(done * SGIris.moveTicks(this.type()));
        this.set(moving, since);
        if (shield) {
            this.gate.play(target == IrisState.OPEN ? SGSounds.SHIELD_DOWN : SGSounds.SHIELD_UP);
        } else {
            this.gate.play(target == IrisState.OPEN ? SGSounds.IRIS_OPEN : SGSounds.IRIS_CLOSE);
        }
        this.gate.stargates().moving(this.gate, cause);
        return ActionResult.DONE;
    }

    /**
     * Moves the iris on: one still moving that has had its time stops where it was going. A shield rising or raised
     * takes its energy for the tick, and falls if the gate has none left.
     *
     * @return whether the iris has stopped, and so needs no more ticks until it moves again
     */
    boolean tick(final Cause cause) {
        final boolean raised = this.shieldRaised();
        if (raised && !this.payForShield()) {
            this.lower(cause);
            return false;
        }
        final IrisState now = this.state();
        if (now != IrisState.OPENING && now != IrisState.CLOSING) {
            return !raised;
        }
        if (this.gate.now() - this.since() < SGIris.moveTicks(this.type())) {
            return false;
        }
        final IrisState done = now == IrisState.OPENING ? IrisState.OPEN : IrisState.CLOSED;
        this.set(done, this.gate.now());
        SGEventBus.get().post(new SGEvents.IrisPost(cause, this.gate, this, done));
        return !raised;
    }

    /** @return whether the iris is a shield that is rising or raised, and so takes energy each tick */
    public boolean shieldRaised() {
        final IrisState now = this.state();
        return this.gate.get(GateProperties.IRIS).map(SGIris::isShield).orElse(false)
                && (now == IrisState.CLOSING || now == IrisState.CLOSED);
    }

    /** @return whether a shield could be raised now: the gate needs no energy, or has enough for a tick */
    public boolean canRaise() {
        return !this.drawsEnergy() || this.gate.energy().available() >= Math.ceil(this.energyPerTick());
    }

    // Whether the gate pays for its shield, as one that needs no power doesn't
    private boolean drawsEnergy() {
        return this.gate.get(GateProperties.REQUIRES_POWER).orElse(true);
    }

    private double energyPerTick() {
        return Math.max(0, this.gate.stargates().config().options().shields().gateEnergyPerTick());
    }

    // Takes the raised shield's energy for a tick, a whole unit at a time as its fractions add up
    private boolean payForShield() {
        if (!this.drawsEnergy()) {
            return true;
        }
        final double owed = this.gate.saved().getDoubleOr(SGIris.OWED, 0) + this.energyPerTick();
        final long whole = (long) Math.floor(owed);
        if (whole > 0 && !this.gate.energy().pay(whole)) {
            return false;
        }
        final CompoundTag state = this.gate.saved();
        state.putDouble(SGIris.OWED, owed - whole);
        this.gate.setSaved(state);
        return true;
    }

    // Drops a shield whose energy has run out, from wherever it has got to
    private void lower(final Cause cause) {
        final float aperture = SGIris.aperture(this.type(), this.state(), this.since(), this.gate.now());
        this.set(IrisState.OPENING, this.gate.now() - Math.round(aperture * SGIris.moveTicks(this.type())));
        this.gate.play(SGSounds.SHIELD_DOWN);
    }

    /** The iris has just been fitted: it starts open. */
    void fitted() {
        this.set(IrisState.OPEN, this.gate.now());
    }

    /** The iris has been taken off: its state goes. */
    void removed() {
        final CompoundTag state = this.gate.saved();
        state.remove(SGIris.STATE);
        state.remove(SGIris.SINCE);
        state.remove(SGIris.BEFORE);
        state.remove(SGIris.OWED);
        this.gate.setSaved(state);
    }

    /** Remembers whether the iris was shut as a connection starts, so it can go back to that afterwards. */
    void remember() {
        final CompoundTag state = this.gate.saved();
        state.putBoolean(SGIris.BEFORE, this.blocks());
        this.gate.setSaved(state);
    }

    /**
     * Puts the iris back as it was when the connection started, if the gate asks for that, as the original's
     * "return to previous iris state" did: it only ever closes the iris again.
     */
    void restore(final Cause cause) {
        final CompoundTag state = this.gate.saved();
        final boolean wasShut = state.getBooleanOr(SGIris.BEFORE, false);
        state.remove(SGIris.BEFORE);
        this.gate.setSaved(state);
        if (wasShut && this.gate.get(GateProperties.IRIS_RESTORES_STATE).orElse(false)
                && this.gate.get(GateProperties.IRIS).isPresent()) {
            this.close(cause);
        }
    }

    private void set(final IrisState irisState, final long since) {
        final CompoundTag state = this.gate.saved();
        state.putString(SGIris.STATE, irisState.name());
        state.putLong(SGIris.SINCE, since);
        this.gate.setSaved(state);
        this.gate.sync();
    }

    @Override
    public String toString() {
        return "Iris[" + this.type().identifier() + ", " + this.state().name().toLowerCase(Locale.ROOT) + "]";
    }

    static ResourceKey<IrisType> metal() {
        return ResourceKey.create(SGCraftRegistries.IRIS_TYPE, IrisTypes.METAL.identifier());
    }
}
