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

package com.almuradev.sgcraft.mod.entity;

import com.almuradev.sgcraft.mod.SGServer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The server's settings for the puddle jumper's cloak generator, its crashes and its drones, kept under {@code jumper} in the server's settings
 * file. The generator runs either the cloak or the shield, never both, from a charge of its own that refills while it
 * runs neither and the drive is on. Clients are told only how full it is, so none of these is read on a client.
 *
 * @param generatorCapacity the most gate energy the generator holds
 * @param rechargeRate how much gate energy the generator gains each tick while it runs neither the shield nor the
 *     cloak and the drive is on
 * @param shieldDrain how much gate energy the shield uses each tick while it is up, hit or not
 * @param cloakDrain how much gate energy the cloak uses each tick while it is on
 * @param hitCost how much gate energy the shield spends on each point of damage it stops
 * @param knockdownTicks how long the shield stays down once hits or its own drain empty the generator, in ticks
 * @param crashSpeed how fast the jumper may meet a block, in blocks a tick, before it is hurt; a little above its top
 *     speed descending, so it may set down at full speed
 * @param crashDamage how much damage the jumper takes meeting a block, for each block a tick it goes faster than
 *     {@code crashSpeed}; the shield, if it is up, takes it
 * @param droneRefillTicks how long the jumper takes to build each drone it has launched again, in ticks; 0 never does
 */
public record JumperSettings(double generatorCapacity, double rechargeRate, double shieldDrain, double cloakDrain,
        double hitCost, int knockdownTicks, double crashSpeed, double crashDamage, int droneRefillTicks) {

    /** The defaults: a full generator stops about eighty points of damage, or cloaks for about a minute and a half. */
    public static final JumperSettings DEFAULTS = new JumperSettings(2000, 2, 0.5, 1, 25, 200, 0.35, 40, 600);

    public static final Codec<JumperSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(1, Double.MAX_VALUE).optionalFieldOf("generator_capacity",
                    JumperSettings.DEFAULTS.generatorCapacity).forGetter(JumperSettings::generatorCapacity),
            Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("recharge_rate",
                    JumperSettings.DEFAULTS.rechargeRate).forGetter(JumperSettings::rechargeRate),
            Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("shield_drain",
                    JumperSettings.DEFAULTS.shieldDrain).forGetter(JumperSettings::shieldDrain),
            Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("cloak_drain",
                    JumperSettings.DEFAULTS.cloakDrain).forGetter(JumperSettings::cloakDrain),
            Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("hit_cost",
                    JumperSettings.DEFAULTS.hitCost).forGetter(JumperSettings::hitCost),
            Codec.intRange(0, 72_000).optionalFieldOf("knockdown_ticks",
                    JumperSettings.DEFAULTS.knockdownTicks).forGetter(JumperSettings::knockdownTicks),
            Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("crash_speed",
                    JumperSettings.DEFAULTS.crashSpeed).forGetter(JumperSettings::crashSpeed),
            Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("crash_damage",
                    JumperSettings.DEFAULTS.crashDamage).forGetter(JumperSettings::crashDamage),
            Codec.intRange(0, 72_000).optionalFieldOf("drone_refill_ticks",
                    JumperSettings.DEFAULTS.droneRefillTicks).forGetter(JumperSettings::droneRefillTicks)
    ).apply(instance, JumperSettings::new));

    /** @return the running server's jumper settings, or the defaults where no server runs */
    public static JumperSettings current() {
        return SGServer.current().map(server -> server.config().options().jumper()).orElse(JumperSettings.DEFAULTS);
    }
}
