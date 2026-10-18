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

package com.almuradev.sgcraft.mod.computer;

import com.almuradev.sgcraft.mod.SGServer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The server's settings for dialing computers, and for the power of both dialing computers and MALP consoles, kept
 * under {@code computers} in the server's settings file.
 *
 * @param linkRange how far from a dialing computer the base of the gate it dials may be, in blocks; a computer
 *     links to the nearest gate in reach
 * @param chairReach how far in front of a dialing computer its chair may stand, in blocks
 * @param requiresPower whether dialing computers and MALP consoles need power to run
 * @param energyPerTick how much gate energy a computer switched on uses each tick, paid once a second; it may be a
 *     fraction
 * @param powerReach how far from a computer, in blocks along each axis, the power units and ZPM blocks it draws on may
 *     stand
 * @param historySize how many of the addresses last dialed from a dialing computer it keeps; 0 keeps none
 */
public record ComputerSettings(int linkRange, int chairReach, boolean requiresPower, double energyPerTick,
        int powerReach, int historySize) {

    /** The most a computer's link range may be set to. */
    public static final int MAX_LINK_RANGE = 64;
    /** The most a computer's chair reach may be set to. */
    public static final int MAX_CHAIR_REACH = 4;
    /** The most a computer's power reach may be set to. */
    public static final int MAX_POWER_REACH = 16;
    /** The most addresses a computer's history may be set to keep. */
    public static final int MAX_HISTORY_SIZE = 10;

    public static final ComputerSettings DEFAULTS = new ComputerSettings(32, 2, true, 0.05, 6, 10);

    public static final Codec<ComputerSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, ComputerSettings.MAX_LINK_RANGE).optionalFieldOf("link_range",
                    ComputerSettings.DEFAULTS.linkRange).forGetter(ComputerSettings::linkRange),
            Codec.intRange(1, ComputerSettings.MAX_CHAIR_REACH).optionalFieldOf("chair_reach",
                    ComputerSettings.DEFAULTS.chairReach).forGetter(ComputerSettings::chairReach),
            Codec.BOOL.optionalFieldOf("requires_power", ComputerSettings.DEFAULTS.requiresPower)
                    .forGetter(ComputerSettings::requiresPower),
            Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("energy_per_tick",
                    ComputerSettings.DEFAULTS.energyPerTick).forGetter(ComputerSettings::energyPerTick),
            Codec.intRange(1, ComputerSettings.MAX_POWER_REACH).optionalFieldOf("power_reach",
                    ComputerSettings.DEFAULTS.powerReach).forGetter(ComputerSettings::powerReach),
            Codec.intRange(0, ComputerSettings.MAX_HISTORY_SIZE).optionalFieldOf("history_size",
                    ComputerSettings.DEFAULTS.historySize).forGetter(ComputerSettings::historySize)
    ).apply(instance, ComputerSettings::new));

    /** @return the running server's dialing computer settings, or the defaults if no server is running */
    public static ComputerSettings current() {
        return SGServer.current().map(server -> server.config().options().computers())
                .orElse(ComputerSettings.DEFAULTS);
    }

    /** @return these settings with one setting changed, by its name in the settings file, if it is one */
    public ComputerSettings with(final String name, final int value) {
        return new ComputerSettings(
                name.equals("link_range") ? Math.max(1, Math.min(ComputerSettings.MAX_LINK_RANGE, value))
                        : this.linkRange,
                name.equals("chair_reach") ? Math.max(1, Math.min(ComputerSettings.MAX_CHAIR_REACH, value))
                        : this.chairReach,
                this.requiresPower, this.energyPerTick,
                name.equals("power_reach") ? Math.max(1, Math.min(ComputerSettings.MAX_POWER_REACH, value))
                        : this.powerReach,
                name.equals("history_size") ? Math.max(0, Math.min(ComputerSettings.MAX_HISTORY_SIZE, value))
                        : this.historySize);
    }

    /** @return these settings with computers needing power to run, or not */
    public ComputerSettings withRequiresPower(final boolean requires) {
        return new ComputerSettings(this.linkRange, this.chairReach, requires, this.energyPerTick, this.powerReach,
                this.historySize);
    }

    /** @return these settings with computers using this much gate energy a tick */
    public ComputerSettings withEnergyPerTick(final double energy) {
        return new ComputerSettings(this.linkRange, this.chairReach, this.requiresPower, Math.max(0, energy),
                this.powerReach, this.historySize);
    }
}
