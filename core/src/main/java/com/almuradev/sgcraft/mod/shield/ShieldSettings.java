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

package com.almuradev.sgcraft.mod.shield;

import com.almuradev.sgcraft.mod.SGServer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The server's settings for gate shields and city shields, kept under {@code shields} in the server's settings file.
 * Energy is in SGCraft's own units, as gates use it.
 *
 * @param gateEnergyPerTick how much energy a gate's shield takes each tick while it is raised, which may be a fraction
 * @param consoleRange how far from a shield console, in blocks, the shielded gate it works may be
 * @param cityEnergyPerMillionBlocks how much energy a city shield takes each tick for each million blocks it holds
 * @param cityMaxRadius the most blocks a city shield reaches from its generator, each way
 * @param cityPowerReach how far from a city shield generator, in blocks along each axis, the power units it draws
 *     from may be
 * @param cityKeepsChunksLoaded whether a raised city shield keeps every chunk under it loaded
 */
public record ShieldSettings(double gateEnergyPerTick, int consoleRange, double cityEnergyPerMillionBlocks,
        int cityMaxRadius, int cityPowerReach, boolean cityKeepsChunksLoaded) {

    /** The least a city shield reaches from its generator, each way. */
    public static final int CITY_MIN_RADIUS = 3;
    /** The most a city shield may be allowed to reach, so its chunks stay within reason. */
    public static final int CITY_LIMIT = 256;

    public static final ShieldSettings DEFAULTS = new ShieldSettings(1.0, 40, 0.333, 128, 6, true);

    public static final Codec<ShieldSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(0, Double.MAX_VALUE)
                    .optionalFieldOf("gate_energy_per_tick", ShieldSettings.DEFAULTS.gateEnergyPerTick)
                    .forGetter(ShieldSettings::gateEnergyPerTick),
            Codec.intRange(1, 256).optionalFieldOf("console_range", ShieldSettings.DEFAULTS.consoleRange)
                    .forGetter(ShieldSettings::consoleRange),
            Codec.doubleRange(0, Double.MAX_VALUE)
                    .optionalFieldOf("city_energy_per_million_blocks",
                            ShieldSettings.DEFAULTS.cityEnergyPerMillionBlocks)
                    .forGetter(ShieldSettings::cityEnergyPerMillionBlocks),
            Codec.intRange(ShieldSettings.CITY_MIN_RADIUS, ShieldSettings.CITY_LIMIT)
                    .optionalFieldOf("city_max_radius", ShieldSettings.DEFAULTS.cityMaxRadius)
                    .forGetter(ShieldSettings::cityMaxRadius),
            Codec.intRange(1, 16).optionalFieldOf("city_power_reach", ShieldSettings.DEFAULTS.cityPowerReach)
                    .forGetter(ShieldSettings::cityPowerReach),
            Codec.BOOL.optionalFieldOf("city_keeps_chunks_loaded", ShieldSettings.DEFAULTS.cityKeepsChunksLoaded)
                    .forGetter(ShieldSettings::cityKeepsChunksLoaded)
    ).apply(instance, ShieldSettings::new));

    /** @return the running server's shield settings, or the defaults if no server is running */
    public static ShieldSettings current() {
        return SGServer.current().map(server -> server.config().options().shields()).orElse(ShieldSettings.DEFAULTS);
    }

    /** @return these settings with a gate's shield taking this much energy a tick */
    public ShieldSettings withGateEnergyPerTick(final double energy) {
        return new ShieldSettings(energy, this.consoleRange, this.cityEnergyPerMillionBlocks, this.cityMaxRadius,
                this.cityPowerReach, this.cityKeepsChunksLoaded);
    }

    /** @return these settings with shield consoles reaching this far */
    public ShieldSettings withConsoleRange(final int range) {
        return new ShieldSettings(this.gateEnergyPerTick, range, this.cityEnergyPerMillionBlocks, this.cityMaxRadius,
                this.cityPowerReach, this.cityKeepsChunksLoaded);
    }

    /** @return these settings with city shields taking this much energy a tick for each million blocks */
    public ShieldSettings withCityEnergyPerMillionBlocks(final double energy) {
        return new ShieldSettings(this.gateEnergyPerTick, this.consoleRange, energy, this.cityMaxRadius,
                this.cityPowerReach, this.cityKeepsChunksLoaded);
    }

    /** @return these settings with city shields reaching at most this far */
    public ShieldSettings withCityMaxRadius(final int radius) {
        return new ShieldSettings(this.gateEnergyPerTick, this.consoleRange, this.cityEnergyPerMillionBlocks, radius,
                this.cityPowerReach, this.cityKeepsChunksLoaded);
    }

    /** @return these settings with city shield generators drawing from power units this far away */
    public ShieldSettings withCityPowerReach(final int reach) {
        return new ShieldSettings(this.gateEnergyPerTick, this.consoleRange, this.cityEnergyPerMillionBlocks,
                this.cityMaxRadius, reach, this.cityKeepsChunksLoaded);
    }

    /** @return these settings with raised city shields keeping their chunks loaded, or not */
    public ShieldSettings withCityKeepsChunksLoaded(final boolean keeps) {
        return new ShieldSettings(this.gateEnergyPerTick, this.consoleRange, this.cityEnergyPerMillionBlocks,
                this.cityMaxRadius, this.cityPowerReach, keeps);
    }
}
