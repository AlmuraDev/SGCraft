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

package com.almuradev.sgcraft.mod.client.jumper;

import com.almuradev.sgcraft.mod.entity.JumperIndex;
import com.almuradev.sgcraft.mod.entity.PuddleJumperEntity;
import com.almuradev.sgcraft.mod.sound.SGSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The hum of a puddle jumper's drive, as a client hears it: looping from the jumper while its drive is on, rising in
 * pitch and growing louder as its engines run harder, and stopping once the drive goes off, the jumper goes, or the
 * player is out of earshot. Only clients call this.
 */
public final class JumperSounds {

    // How far round the player jumpers are listened for, in blocks
    private static final double HEARD = 40;
    // The hum's volume and pitch, idling and flat out
    private static final float IDLE_VOLUME = 0.2F;
    private static final float FULL_VOLUME = 0.4F;
    private static final float IDLE_PITCH = 0.9F;
    private static final float FULL_PITCH = 1.2F;

    private static final Map<PuddleJumperEntity, Hum> PLAYING = new WeakHashMap<>();

    private JumperSounds() {
    }

    /** Each tick: starts the hum of each powered jumper near the player that isn't humming yet. */
    public static void tick(final Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            JumperSounds.PLAYING.clear();
            return;
        }
        for (final PuddleJumperEntity jumper : JumperIndex.near(minecraft.level,
                minecraft.player.getBoundingBox().inflate(JumperSounds.HEARD))) {
            if (!jumper.powered()) {
                continue;
            }
            final Hum hum = JumperSounds.PLAYING.get(jumper);
            if (hum != null && !hum.isStopped()) {
                continue;
            }
            final Hum started = new Hum(jumper);
            JumperSounds.PLAYING.put(jumper, started);
            minecraft.getSoundManager().play(started);
        }
    }

    // A jumper's hum, following it as it flies
    private static final class Hum extends AbstractTickableSoundInstance {

        private final PuddleJumperEntity jumper;

        Hum(final PuddleJumperEntity jumper) {
            super(SGSounds.JUMPER_HUM, SoundSource.NEUTRAL, RandomSource.create());
            this.jumper = jumper;
            this.looping = true;
            this.delay = 0;
            this.follow();
        }

        @Override
        public void tick() {
            final Minecraft minecraft = Minecraft.getInstance();
            if (this.jumper.isRemoved() || !this.jumper.powered() || minecraft.player == null
                    || minecraft.player.distanceToSqr(this.jumper) > JumperSounds.HEARD * JumperSounds.HEARD) {
                this.stop();
                return;
            }
            this.follow();
        }

        private void follow() {
            final float thrust = this.jumper.thrust(1);
            this.x = this.jumper.getX();
            this.y = this.jumper.getY() + PuddleJumperEntity.HEIGHT / 2;
            this.z = this.jumper.getZ();
            this.volume = Mth.lerp(thrust, JumperSounds.IDLE_VOLUME, JumperSounds.FULL_VOLUME);
            this.pitch = Mth.lerp(thrust, JumperSounds.IDLE_PITCH, JumperSounds.FULL_PITCH);
        }
    }
}
