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

package com.almuradev.sgcraft.mod.malp;

import com.almuradev.sgcraft.mod.SGServer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The server's settings for the MALP and its batteries, kept under {@code malp} in the server's settings file.
 *
 * @param batteryCapacity the most FE a battery holds
 * @param powerUnitChargeRate how much FE a power unit puts into a battery in its slot each tick
 * @param chargerChargeRate how much FE a battery charger puts into its battery each tick
 * @param fePerNaquadah how much FE one naquadah gives a battery, whether crafted in or burned in a charger
 * @param idleDrain how much FE a MALP uses each tick while someone watches its feed
 * @param driveDrain how much FE a MALP uses for each block it drives
 * @param signalRange how far, in blocks, a console or tablet reaches a MALP directly, and reaches a gate to relay
 *     through
 * @param lingerTicks how long a MALP's chunk stays loaded after the last person stops watching it, in ticks
 * @param consoleDamageInterrupts whether a player at a MALP console who takes damage is taken off it, as though they
 *     had closed its screen
 * @param consoleSpectators how many of the players near a console someone is driving at, nearest first, see the
 *     feed on its monitor; 0 shows it to none
 * @param spectatorRange how near a console, in blocks, a player must be to see the feed on its monitor
 * @param tabletDrain how much FE a MALP tablet uses from its own battery each tick while its screen shows a live feed
 */
public record MalpSettings(long batteryCapacity, long powerUnitChargeRate, long chargerChargeRate, long fePerNaquadah,
        long idleDrain, long driveDrain, int signalRange, int lingerTicks, boolean consoleDamageInterrupts,
        int consoleSpectators, int spectatorRange, long tabletDrain) {

    public static final MalpSettings DEFAULTS = new MalpSettings(1_000_000L, 1000L, 1000L, 100_000L, 10L, 100L, 256,
            600, true, 4, 16, 2L);

    public static final Codec<MalpSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("battery_capacity", MalpSettings.DEFAULTS.batteryCapacity)
                    .forGetter(MalpSettings::batteryCapacity),
            Codec.LONG.optionalFieldOf("power_unit_charge_rate", MalpSettings.DEFAULTS.powerUnitChargeRate)
                    .forGetter(MalpSettings::powerUnitChargeRate),
            Codec.LONG.optionalFieldOf("charger_charge_rate", MalpSettings.DEFAULTS.chargerChargeRate)
                    .forGetter(MalpSettings::chargerChargeRate),
            Codec.LONG.optionalFieldOf("fe_per_naquadah", MalpSettings.DEFAULTS.fePerNaquadah)
                    .forGetter(MalpSettings::fePerNaquadah),
            Codec.LONG.optionalFieldOf("idle_drain", MalpSettings.DEFAULTS.idleDrain)
                    .forGetter(MalpSettings::idleDrain),
            Codec.LONG.optionalFieldOf("drive_drain", MalpSettings.DEFAULTS.driveDrain)
                    .forGetter(MalpSettings::driveDrain),
            Codec.INT.optionalFieldOf("signal_range", MalpSettings.DEFAULTS.signalRange)
                    .forGetter(MalpSettings::signalRange),
            Codec.INT.optionalFieldOf("linger_ticks", MalpSettings.DEFAULTS.lingerTicks)
                    .forGetter(MalpSettings::lingerTicks),
            Codec.BOOL.optionalFieldOf("console_damage_interrupts", MalpSettings.DEFAULTS.consoleDamageInterrupts)
                    .forGetter(MalpSettings::consoleDamageInterrupts),
            Codec.intRange(0, 64).optionalFieldOf("console_spectators", MalpSettings.DEFAULTS.consoleSpectators)
                    .forGetter(MalpSettings::consoleSpectators),
            Codec.intRange(1, 64).optionalFieldOf("spectator_range", MalpSettings.DEFAULTS.spectatorRange)
                    .forGetter(MalpSettings::spectatorRange),
            Codec.LONG.optionalFieldOf("tablet_drain", MalpSettings.DEFAULTS.tabletDrain)
                    .forGetter(MalpSettings::tabletDrain)
    ).apply(instance, MalpSettings::new));

    /** @return the running server's MALP settings, or the defaults if no server is running */
    public static MalpSettings current() {
        return SGServer.current().map(server -> server.config().malp())
                .orElse(MalpSettings.DEFAULTS);
    }

    /** @return these settings with damage taking players off their consoles, or not */
    public MalpSettings withConsoleDamageInterrupts(final boolean interrupts) {
        return new MalpSettings(this.batteryCapacity, this.powerUnitChargeRate, this.chargerChargeRate,
                this.fePerNaquadah, this.idleDrain, this.driveDrain, this.signalRange, this.lingerTicks, interrupts,
                this.consoleSpectators, this.spectatorRange, this.tabletDrain);
    }

    /** @return these settings with as many players near a driven console seeing the feed on its monitor */
    public MalpSettings withConsoleSpectators(final int spectators) {
        return new MalpSettings(this.batteryCapacity, this.powerUnitChargeRate, this.chargerChargeRate,
                this.fePerNaquadah, this.idleDrain, this.driveDrain, this.signalRange, this.lingerTicks,
                this.consoleDamageInterrupts, spectators, this.spectatorRange, this.tabletDrain);
    }

    /** @return these settings with players this near a driven console seeing the feed on its monitor */
    public MalpSettings withSpectatorRange(final int range) {
        return new MalpSettings(this.batteryCapacity, this.powerUnitChargeRate, this.chargerChargeRate,
                this.fePerNaquadah, this.idleDrain, this.driveDrain, this.signalRange, this.lingerTicks,
                this.consoleDamageInterrupts, this.consoleSpectators, range, this.tabletDrain);
    }

    /** @return these settings with MALP tablets using this much FE each tick while they show a live feed */
    public MalpSettings withTabletDrain(final long drain) {
        return new MalpSettings(this.batteryCapacity, this.powerUnitChargeRate, this.chargerChargeRate,
                this.fePerNaquadah, this.idleDrain, this.driveDrain, this.signalRange, this.lingerTicks,
                this.consoleDamageInterrupts, this.consoleSpectators, this.spectatorRange, drain);
    }
}
