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

package com.almuradev.sgcraft.mod.client.gate;

import com.almuradev.sgcraft.mod.block.entity.SGBlockEntityTypes;
import com.almuradev.sgcraft.mod.block.entity.StargateBaseBlockEntity;
import com.almuradev.sgcraft.mod.gate.DialPhase;
import com.almuradev.sgcraft.mod.gate.DialStyle;
import com.almuradev.sgcraft.mod.gate.DialView;
import com.almuradev.sgcraft.mod.sound.SGSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

/**
 * The sounds a gate keeps up on a client: its ring turning, or its light running, while it dials a symbol, and the
 * hum of its open wormhole. Each stops itself once the gate has moved on.
 */
public final class GateLoopSounds {

    private static final Map<StargateBaseBlockEntity, Loop> ROLLS = new WeakHashMap<>();
    private static final Map<StargateBaseBlockEntity, Loop> HUMS = new WeakHashMap<>();

    private GateLoopSounds() {
    }

    /** Starts whichever of a gate's sounds should be playing and aren't. Call each client tick. */
    public static void tick(final StargateBaseBlockEntity base) {
        final DialStyle style = DialStyle.of(base.gateType());
        GateLoopSounds.keep(GateLoopSounds.ROLLS, base, SGSounds.roll(style), GateLoopSounds::rolling);
        GateLoopSounds.keep(GateLoopSounds.HUMS, base, SGSounds.EVENT_HORIZON,
                view -> view.phase() == DialPhase.CONNECTED);
    }

    // The dialing gate rolls while a symbol is being dialed
    private static boolean rolling(final DialView view) {
        return view.initiator() && view.phase() == DialPhase.DIALING;
    }

    private static void keep(final Map<StargateBaseBlockEntity, Loop> playing, final StargateBaseBlockEntity base,
            final SoundEvent sound, final Predicate<DialView> wanted) {
        if (!wanted.test(base.dialView())) {
            return;
        }
        final Loop loop = playing.get(base);
        if (loop != null && !loop.isStopped()) {
            return;
        }
        final Loop started = new Loop(sound, base, wanted);
        playing.put(base, started);
        Minecraft.getInstance().getSoundManager().play(started);
    }

    private static final class Loop extends AbstractTickableSoundInstance {

        private final StargateBaseBlockEntity base;
        private final Predicate<DialView> wanted;

        Loop(final SoundEvent sound, final StargateBaseBlockEntity base, final Predicate<DialView> wanted) {
            super(sound, SoundSource.BLOCKS, RandomSource.create());
            this.base = base;
            this.wanted = wanted;
            this.looping = true;
            // From the middle of the ring, as far as the gate's size carries it
            this.volume = SGSounds.volume(base.shape());
            final Vec3 middle = base.frame().toWorld(new Vec3(0, base.shape().centerY(), 0));
            this.x = middle.x;
            this.y = middle.y;
            this.z = middle.z;
        }

        @Override
        public void tick() {
            if (this.base.isRemoved() || this.base.getType() != SGBlockEntityTypes.STARGATE_BASE
                    || !this.wanted.test(this.base.dialView())) {
                this.stop();
            }
        }
    }
}
