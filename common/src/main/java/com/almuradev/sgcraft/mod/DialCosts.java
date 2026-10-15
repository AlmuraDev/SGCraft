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

package com.almuradev.sgcraft.mod;

import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.GateProperty;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * What a wormhole costs the gate that dials it, before distance: some energy as each chevron locks, some as the
 * wormhole forms, and some each tick it stays open. Each can be turned off on its own. The server's settings hold the
 * costs for every gate, and a gate's own properties override them.
 *
 * @param chevron the energy taken as each chevron locks
 * @param open the energy taken as the wormhole forms
 * @param tick the energy taken each tick the wormhole stays open
 */
public record DialCosts(Cost chevron, Cost open, Cost tick) {

    /**
     * The original's costs for opening and staying open, as a piece of naquadah's 96000 opened 24 wormholes or kept
     * one open for 80 minutes, with a cost for each chevron besides.
     */
    public static final DialCosts DEFAULTS = new DialCosts(new Cost(true, 100), new Cost(true, 4000),
            new Cost(true, 1));

    public static final Codec<DialCosts> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Cost.CODEC.optionalFieldOf("per_chevron", DialCosts.DEFAULTS.chevron).forGetter(DialCosts::chevron),
            Cost.CODEC.optionalFieldOf("open", DialCosts.DEFAULTS.open).forGetter(DialCosts::open),
            Cost.CODEC.optionalFieldOf("per_tick", DialCosts.DEFAULTS.tick).forGetter(DialCosts::tick)
    ).apply(instance, DialCosts::new));

    /** @return what a gate pays: its own properties where it has them, and these costs where it doesn't */
    public DialCosts forGate(final Stargate gate) {
        return new DialCosts(this.forGate(gate, Kind.CHEVRON), this.forGate(gate, Kind.OPEN),
                this.forGate(gate, Kind.TICK));
    }

    private Cost forGate(final Stargate gate, final Kind kind) {
        final Cost server = this.get(kind);
        return new Cost(gate.get(kind.enabledProperty()).orElse(server.enabled()),
                gate.get(kind.energyProperty()).orElse(server.energy()));
    }

    /** @return the cost of one kind */
    public Cost get(final Kind kind) {
        return switch (kind) {
            case CHEVRON -> this.chevron;
            case OPEN -> this.open;
            case TICK -> this.tick;
        };
    }

    /** @return these costs with one kind changed */
    public DialCosts with(final Kind kind, final Cost cost) {
        return switch (kind) {
            case CHEVRON -> new DialCosts(cost, this.open, this.tick);
            case OPEN -> new DialCosts(this.chevron, cost, this.tick);
            case TICK -> new DialCosts(this.chevron, this.open, cost);
        };
    }

    /**
     * One cost.
     *
     * @param enabled whether it is taken at all
     * @param energy how much is taken, before distance
     */
    public record Cost(boolean enabled, double energy) {

        public static final Codec<Cost> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("enabled", true).forGetter(Cost::enabled),
                Codec.DOUBLE.validate(Cost::notNegative).fieldOf("energy").forGetter(Cost::energy)
        ).apply(instance, Cost::new));

        /** @return the energy taken, scaled by a factor, or none if this cost is off */
        public double scaled(final double factor) {
            return this.enabled ? this.energy * factor : 0;
        }

        /** @return the energy taken, scaled by a factor and rounded to a whole unit, or none if this cost is off */
        public long scaledWhole(final double factor) {
            return Math.round(this.scaled(factor));
        }

        private static DataResult<Double> notNegative(final Double energy) {
            return energy >= 0 ? DataResult.success(energy)
                    : DataResult.error(() -> "A cost can't be negative: " + energy);
        }
    }

    /**
     * The kinds of cost, by the names the settings file and commands use, with the gate properties that override
     * them.
     */
    public enum Kind {
        CHEVRON("per_chevron", GateProperties.CHEVRON_COST_ENABLED, GateProperties.CHEVRON_COST),
        OPEN("open", GateProperties.OPEN_COST_ENABLED, GateProperties.OPEN_COST),
        TICK("per_tick", GateProperties.TICK_COST_ENABLED, GateProperties.TICK_COST);

        private final String id;
        private final GateProperty<Boolean> enabledProperty;
        private final GateProperty<Double> energyProperty;

        Kind(final String id, final GateProperty<Boolean> enabledProperty, final GateProperty<Double> energyProperty) {
            this.id = id;
            this.enabledProperty = enabledProperty;
            this.energyProperty = energyProperty;
        }

        /** @return the gate property that turns this cost on or off for one gate */
        public GateProperty<Boolean> enabledProperty() {
            return this.enabledProperty;
        }

        /** @return the gate property that sets this cost's energy for one gate */
        public GateProperty<Double> energyProperty() {
            return this.energyProperty;
        }

        /** @return the kind's name in the settings file and commands */
        public String id() {
            return this.id;
        }
    }
}
