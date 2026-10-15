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
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.Iris;
import com.almuradev.sgcraft.api.gate.IrisState;
import com.almuradev.sgcraft.api.gate.IrisType;
import com.almuradev.sgcraft.api.gate.IrisTypes;
import com.almuradev.sgcraft.mod.event.SGEventBus;
import com.almuradev.sgcraft.mod.event.SGEvents;
import com.almuradev.sgcraft.sound.SGSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;

import java.util.Locale;

/**
 * A gate's iris, as in the original SGCraft: it takes {@link #MOVE_TICKS} to open or close, and stops travelers once
 * it is half shut. Where it is lives in the gate's saved state, as the state it is in and the game time it got there,
 * so it carries on across restarts.
 */
public final class SGIris implements Iris {

    /** How long the iris takes to open or close, in ticks. */
    public static final int MOVE_TICKS = 70;

    private static final String STATE = "iris_state";
    private static final String SINCE = "iris_since";
    private static final String BEFORE = "iris_before";

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
     * @param state where the iris is in its movement
     * @param since when it got there
     * @param time the time to ask about, in ticks, which may have a fraction
     */
    public static float aperture(final IrisState state, final long since, final double time) {
        final double moved = Math.max(0, Math.min(1, (time - since) / SGIris.MOVE_TICKS));
        return (float) switch (state) {
            case OPEN -> 1;
            case CLOSED -> 0;
            case OPENING -> moved;
            case CLOSING -> 1 - moved;
        };
    }

    /** @return whether the iris stops travelers now: once it is half shut, as in the original */
    public boolean blocks() {
        return SGIris.aperture(this.state(), this.since(), this.gate.now()) <= 0.5F;
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
        if (!SGEventBus.get().allows(new SGEvents.IrisPre(cause, this.gate, this, target))) {
            return ActionResult.CANCELED;
        }
        // Turning back part way starts from where the iris has got to, so it doesn't jump
        final float aperture = SGIris.aperture(now, this.since(), this.gate.now());
        final float done = target == IrisState.OPEN ? aperture : 1 - aperture;
        final long since = this.gate.now() - Math.round(done * SGIris.MOVE_TICKS);
        this.set(moving, since);
        this.gate.play(target == IrisState.OPEN ? SGSounds.IRIS_OPEN : SGSounds.IRIS_CLOSE);
        this.gate.stargates().moving(this.gate, cause);
        return ActionResult.DONE;
    }

    /**
     * Moves the iris on: one still moving that has had its time stops where it was going.
     *
     * @return whether the iris has stopped
     */
    boolean tick(final Cause cause) {
        final IrisState now = this.state();
        if (now != IrisState.OPENING && now != IrisState.CLOSING) {
            return true;
        }
        if (this.gate.now() - this.since() < SGIris.MOVE_TICKS) {
            return false;
        }
        final IrisState done = now == IrisState.OPENING ? IrisState.OPEN : IrisState.CLOSED;
        this.set(done, this.gate.now());
        SGEventBus.get().post(new SGEvents.IrisPost(cause, this.gate, this, done));
        return true;
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
