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

package com.almuradev.sgcraft.mod.generator;

import com.almuradev.sgcraft.mod.SGServer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The server's settings for Naquadah Generators, kept under {@code generators} in the server's settings file, as
 * 2.2.0's {@code naquadah-generator} section. Energy is in gate energy (GE).
 *
 * @param naquadahEnergy how much gate energy a piece of naquadah burned in a generator is worth, before its mark's
 *     efficiency
 * @param ingotEnergy how much a naquadah alloy ingot is worth, before its mark's efficiency
 * @param markOne the Mark I's output, store and efficiency
 * @param markTwo the Mark II's
 */
public record GeneratorSettings(double naquadahEnergy, double ingotEnergy, Mark markOne, Mark markTwo) {

    /**
     * One mark of generator's settings.
     *
     * @param energyPerTick how much gate energy it makes each tick while it runs
     * @param capacity how much it holds ready for gates
     * @param efficiency the share of its fuel's worth it gets
     */
    public record Mark(double energyPerTick, double capacity, double efficiency) {

        static Codec<Mark> codec(final Mark defaults) {
            return RecordCodecBuilder.create(instance -> instance.group(
                    Codec.doubleRange(0, 1_000_000).optionalFieldOf("energy_per_tick", defaults.energyPerTick)
                            .forGetter(Mark::energyPerTick),
                    Codec.doubleRange(1, 1_000_000_000).optionalFieldOf("capacity", defaults.capacity)
                            .forGetter(Mark::capacity),
                    Codec.doubleRange(0, 100).optionalFieldOf("efficiency", defaults.efficiency)
                            .forGetter(Mark::efficiency)
            ).apply(instance, Mark::new));
        }
    }

    public static final GeneratorSettings DEFAULTS = new GeneratorSettings(96_000, 384_000,
            new Mark(4, 20_000, 1.0), new Mark(24, 60_000, 0.5));

    public static final Codec<GeneratorSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("naquadah_energy",
                    GeneratorSettings.DEFAULTS.naquadahEnergy).forGetter(GeneratorSettings::naquadahEnergy),
            Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("ingot_energy",
                    GeneratorSettings.DEFAULTS.ingotEnergy).forGetter(GeneratorSettings::ingotEnergy),
            Mark.codec(GeneratorSettings.DEFAULTS.markOne).optionalFieldOf("mark_1", GeneratorSettings.DEFAULTS.markOne)
                    .forGetter(GeneratorSettings::markOne),
            Mark.codec(GeneratorSettings.DEFAULTS.markTwo).optionalFieldOf("mark_2", GeneratorSettings.DEFAULTS.markTwo)
                    .forGetter(GeneratorSettings::markTwo)
    ).apply(instance, GeneratorSettings::new));

    /** @return the running server's generator settings, or the defaults if no server is running */
    public static GeneratorSettings current() {
        return SGServer.current().map(server -> server.config().options().generators())
                .orElse(GeneratorSettings.DEFAULTS);
    }
}
