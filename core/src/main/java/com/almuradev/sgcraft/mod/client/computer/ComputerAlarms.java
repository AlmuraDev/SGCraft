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

package com.almuradev.sgcraft.mod.client.computer;

import com.almuradev.sgcraft.mod.block.entity.DialingComputerBlockEntity;
import com.almuradev.sgcraft.mod.sound.SGSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The alarm a dialing computer sounds on a client while a wormhole comes in to its gate, from the first chevron until
 * the wormhole shuts, as the SGC's does, unless someone at the computer has turned it off. It loops until then,
 * or until it is turned off.
 */
public final class ComputerAlarms {

    private static final Map<DialingComputerBlockEntity, Alarm> PLAYING = new WeakHashMap<>();

    private ComputerAlarms() {
    }

    /** Starts a computer's alarm if it should be sounding and isn't. Call each client tick. */
    public static void tick(final DialingComputerBlockEntity computer) {
        if (!computer.alarming()) {
            return;
        }
        final Alarm alarm = ComputerAlarms.PLAYING.get(computer);
        if (alarm != null && !alarm.isStopped()) {
            return;
        }
        final Alarm started = new Alarm(computer);
        ComputerAlarms.PLAYING.put(computer, started);
        Minecraft.getInstance().getSoundManager().play(started);
    }

    // The alarm's loop, from the computer, stopping itself once the computer no longer sounds it
    private static final class Alarm extends AbstractTickableSoundInstance {

        private final DialingComputerBlockEntity computer;

        Alarm(final DialingComputerBlockEntity computer) {
            super(SGSounds.COMPUTER_ALARM, SoundSource.BLOCKS, RandomSource.create());
            this.computer = computer;
            this.looping = true;
            this.x = computer.getBlockPos().getX() + 0.5;
            this.y = computer.getBlockPos().getY() + 1.0;
            this.z = computer.getBlockPos().getZ() + 0.5;
        }

        @Override
        public void tick() {
            if (this.computer.isRemoved() || !this.computer.alarming()) {
                this.stop();
            }
        }
    }
}
