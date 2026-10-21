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

package com.almuradev.sgcraft.mod.rings;

import com.almuradev.sgcraft.mod.SGConfig;
import com.almuradev.sgcraft.mod.SGServer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The server's settings for Goa'uld transport rings, kept under {@code rings} in the server's settings file, as
 * 2.2.0's {@code rings} section, with its Forge Energy restated as gate energy. Naquadah burned in a platform gives
 * what the server's {@code energy.per_naquadah} says, as it does in a DHD.
 *
 * @param energyPerUse what every transport costs the platform that starts it, in gate energy
 * @param energyPerBlock what it costs on top of that for each block between the two platforms, in gate energy
 * @param capacity the most gate energy a platform holds from cables
 * @param maxDistance how far apart two linked platforms may be, in blocks; 0 for no limit
 */
public record RingSettings(double energyPerUse, double energyPerBlock, double capacity, int maxDistance) {

    public static final RingSettings DEFAULTS = new RingSettings(1_000, 1, 50_000, 2_000);

    public static final Codec<RingSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(0, 1_000_000_000).optionalFieldOf("energy_per_use", RingSettings.DEFAULTS.energyPerUse)
                    .forGetter(RingSettings::energyPerUse),
            Codec.doubleRange(0, 1_000_000).optionalFieldOf("energy_per_block", RingSettings.DEFAULTS.energyPerBlock)
                    .forGetter(RingSettings::energyPerBlock),
            Codec.doubleRange(1, 1_000_000_000).optionalFieldOf("capacity", RingSettings.DEFAULTS.capacity)
                    .forGetter(RingSettings::capacity),
            Codec.intRange(0, 30_000_000).optionalFieldOf("max_distance", RingSettings.DEFAULTS.maxDistance)
                    .forGetter(RingSettings::maxDistance)
    ).apply(instance, RingSettings::new));

    /** @return what a transport between two places this far apart costs, in gate energy */
    public double cost(final double distance) {
        return this.energyPerUse + this.energyPerBlock * distance;
    }

    /** @return the running server's ring settings, or the defaults if no server is running */
    public static RingSettings current() {
        return SGServer.current().map(server -> server.config().options().rings()).orElse(RingSettings.DEFAULTS);
    }

    /** @return what a piece of naquadah burned in a platform gives, in gate energy */
    public static double naquadahEnergy() {
        return SGServer.current().map(server -> server.config().energyPerNaquadah())
                .orElse(SGConfig.DEFAULTS.energyPerNaquadah());
    }
}
