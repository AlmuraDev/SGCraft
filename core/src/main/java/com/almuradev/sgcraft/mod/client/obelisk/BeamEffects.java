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

package com.almuradev.sgcraft.mod.client.obelisk;

import com.almuradev.sgcraft.mod.obelisk.ObeliskPayloads;
import com.almuradev.sgcraft.mod.sound.SGSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

/**
 * What the player being beamed by an Asgard obelisk hears and sees, as in 2.2.0: the beam's sound, played to them alone
 * and from nowhere, so it carries on through the beam; and their screen fading to white as the beam comes, then back.
 */
public final class BeamEffects {

    // How long the screen takes to go white before the beam, stays white, and takes to clear, in milliseconds
    private static final long FADE_IN = 700;
    private static final long HOLD = 250;
    private static final long FADE_OUT = 900;

    private static @Nullable SoundInstance sound;
    // When the beam started, in milliseconds, or 0, and how long after that it comes
    private static long start;
    private static long beamAt;

    private BeamEffects() {
    }

    /** A beam started for this player, or was called off. */
    public static void handle(final ObeliskPayloads.Beam beam) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (BeamEffects.sound != null) {
            minecraft.getSoundManager().stop(BeamEffects.sound);
            BeamEffects.sound = null;
        }
        if (!beam.start()) {
            BeamEffects.start = 0;
            return;
        }
        BeamEffects.start = System.currentTimeMillis();
        BeamEffects.beamAt = beam.ticks() * 50L;
        BeamEffects.sound = new SimpleSoundInstance(SGSounds.OBELISK_BEAM.location(), SoundSource.BLOCKS, 1.0F, 1.0F,
                RandomSource.create(), false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true);
        minecraft.getSoundManager().play(BeamEffects.sound);
    }

    /** Whitens the screen as the beam comes. The game's own screen drawing calls this, last. */
    public static void extract(final GuiGraphicsExtractor graphics) {
        final float white = BeamEffects.whiteness();
        if (white > 0) {
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), ((int) (white * 255) << 24) | 0xFFFFFF);
        }
    }

    private static float whiteness() {
        if (BeamEffects.start == 0) {
            return 0;
        }
        final long t = System.currentTimeMillis() - BeamEffects.start;
        if (t < BeamEffects.beamAt - BeamEffects.FADE_IN) {
            return 0;
        }
        if (t < BeamEffects.beamAt) {
            return (float) (t - (BeamEffects.beamAt - BeamEffects.FADE_IN)) / BeamEffects.FADE_IN;
        }
        if (t < BeamEffects.beamAt + BeamEffects.HOLD) {
            return 1;
        }
        if (t < BeamEffects.beamAt + BeamEffects.HOLD + BeamEffects.FADE_OUT) {
            return 1 - (float) (t - BeamEffects.beamAt - BeamEffects.HOLD) / BeamEffects.FADE_OUT;
        }
        BeamEffects.start = 0;
        return 0;
    }
}
