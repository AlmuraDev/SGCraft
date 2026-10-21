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

package com.almuradev.sgcraft.mod.zat;

import com.almuradev.sgcraft.mod.SGServer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The server's settings for the Zat, kept under {@code zat} in the server's settings file, as 2.2.0's.
 *
 * @param range how far a shot reaches, in blocks
 * @param cooldownTicks how long after firing before it fires again, in ticks
 * @param stunTicks how long a shot stuns for, and so how long a second shot kills in, in ticks
 * @param stunDamage the harm a stunning shot does
 * @param secondShotKills whether a second shot while the target is still stunned kills it
 */
public record ZatSettings(double range, int cooldownTicks, int stunTicks, float stunDamage,
        boolean secondShotKills) {

    public static final ZatSettings DEFAULTS = new ZatSettings(30, 20, 200, 4, true);

    public static final Codec<ZatSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(1, 256).optionalFieldOf("range", ZatSettings.DEFAULTS.range)
                    .forGetter(ZatSettings::range),
            Codec.intRange(1, 72_000).optionalFieldOf("cooldown_ticks", ZatSettings.DEFAULTS.cooldownTicks)
                    .forGetter(ZatSettings::cooldownTicks),
            Codec.intRange(1, 72_000).optionalFieldOf("stun_ticks", ZatSettings.DEFAULTS.stunTicks)
                    .forGetter(ZatSettings::stunTicks),
            Codec.floatRange(0, 1000).optionalFieldOf("stun_damage", ZatSettings.DEFAULTS.stunDamage)
                    .forGetter(ZatSettings::stunDamage),
            Codec.BOOL.optionalFieldOf("second_shot_kills", ZatSettings.DEFAULTS.secondShotKills)
                    .forGetter(ZatSettings::secondShotKills)
    ).apply(instance, ZatSettings::new));

    /** @return the running server's Zat settings, or the defaults if no server is running */
    public static ZatSettings current() {
        return SGServer.current().map(server -> server.config().options().zat()).orElse(ZatSettings.DEFAULTS);
    }
}
