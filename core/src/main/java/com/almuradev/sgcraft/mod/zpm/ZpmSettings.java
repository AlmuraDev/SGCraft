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

package com.almuradev.sgcraft.mod.zpm;

import com.almuradev.sgcraft.mod.SGServer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The server's settings for ZPMs, kept under {@code zpm} in the server's settings file. A ZPM is a great store of gate
 * energy (GE), which gates draw on as on any other source.
 *
 * @param capacity how much gate energy a full ZPM holds: the original's, about 26.8 million
 * @param searchRange how far from a gate or a city shield the ZPMs it draws on may be, in blocks along each axis
 * @param taintedExplosion how big the explosion is when energy is drawn from a tainted ZPM in a console, cart or hub
 * @param chestChance the chance, in percent, that a generated gate has a chest beside it holding a ZPM
 * @param taintedChance the chance, in percent, that a ZPM found in such a chest is tainted
 */
public record ZpmSettings(long capacity, int searchRange,
        float taintedExplosion, int chestChance, int taintedChance) {

    public static final ZpmSettings DEFAULTS = new ZpmSettings(26_843_546L, 6, 250, 15, 10);

    public static final Codec<ZpmSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.validate(ZpmSettings::positive).optionalFieldOf("capacity", ZpmSettings.DEFAULTS.capacity)
                    .forGetter(ZpmSettings::capacity),
            Codec.intRange(1, 16).optionalFieldOf("search_range", ZpmSettings.DEFAULTS.searchRange)
                    .forGetter(ZpmSettings::searchRange),
            Codec.floatRange(0, 1000).optionalFieldOf("tainted_explosion", ZpmSettings.DEFAULTS.taintedExplosion)
                    .forGetter(ZpmSettings::taintedExplosion),
            Codec.intRange(0, 100).optionalFieldOf("chest_chance", ZpmSettings.DEFAULTS.chestChance)
                    .forGetter(ZpmSettings::chestChance),
            Codec.intRange(0, 100).optionalFieldOf("tainted_chance", ZpmSettings.DEFAULTS.taintedChance)
                    .forGetter(ZpmSettings::taintedChance)
    ).apply(instance, ZpmSettings::new));

    /** @return the running server's ZPM settings, or the defaults if no server is running */
    public static ZpmSettings current() {
        return SGServer.current().map(server -> server.config().options().zpm()).orElse(ZpmSettings.DEFAULTS);
    }

    /** @return these settings with one setting changed, by its name in the settings file, if it is one */
    public ZpmSettings with(final String name, final double value) {
        return new ZpmSettings(
                name.equals("capacity") ? (long) Math.max(1, value) : this.capacity,
                name.equals("search_range") ? (int) Math.max(1, Math.min(16, value)) : this.searchRange,
                name.equals("tainted_explosion") ? (float) Math.max(0, Math.min(1000, value)) : this.taintedExplosion,
                name.equals("chest_chance") ? (int) Math.max(0, Math.min(100, value)) : this.chestChance,
                name.equals("tainted_chance") ? (int) Math.max(0, Math.min(100, value)) : this.taintedChance);
    }

    private static DataResult<Long> positive(final Long capacity) {
        return capacity > 0 ? DataResult.success(capacity)
                : DataResult.error(() -> "A ZPM's capacity must be above 0: " + capacity);
    }
}
