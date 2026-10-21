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

package com.almuradev.sgcraft.mod.client.generator;

import com.almuradev.sgcraft.mod.generator.NaquadahGeneratorBlock;
import com.almuradev.sgcraft.mod.generator.NaquadahGeneratorBlockEntity;
import com.almuradev.sgcraft.mod.sound.SGSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.util.Map;
import java.util.WeakHashMap;

/** A running Naquadah Generator's hum, looping at half volume while it runs, as 2.2.0's. */
public final class GeneratorHum {

    private static final Map<NaquadahGeneratorBlockEntity, Loop> PLAYING = new WeakHashMap<>();

    private GeneratorHum() {
    }

    /** Starts a running generator's hum if it isn't playing. Call each client tick. */
    public static void tick(final NaquadahGeneratorBlockEntity generator) {
        if (!GeneratorHum.running(generator)) {
            return;
        }
        final Loop loop = GeneratorHum.PLAYING.get(generator);
        if (loop != null && !loop.isStopped()) {
            return;
        }
        final Loop started = new Loop(generator);
        GeneratorHum.PLAYING.put(generator, started);
        Minecraft.getInstance().getSoundManager().play(started);
    }

    private static boolean running(final NaquadahGeneratorBlockEntity generator) {
        return !generator.isRemoved() && generator.getBlockState().getBlock() instanceof NaquadahGeneratorBlock
                && generator.getBlockState().getValue(NaquadahGeneratorBlock.RUNNING);
    }

    private static final class Loop extends AbstractTickableSoundInstance {

        private final NaquadahGeneratorBlockEntity generator;

        Loop(final NaquadahGeneratorBlockEntity generator) {
            super(SGSounds.GENERATOR_HUM, SoundSource.BLOCKS, RandomSource.create());
            this.generator = generator;
            this.looping = true;
            this.volume = 0.5F;
            this.x = generator.getBlockPos().getX() + 0.5;
            this.y = generator.getBlockPos().getY() + 0.25;
            this.z = generator.getBlockPos().getZ() + 0.5;
        }

        @Override
        public void tick() {
            if (!GeneratorHum.running(this.generator)) {
                this.stop();
            }
        }
    }
}
