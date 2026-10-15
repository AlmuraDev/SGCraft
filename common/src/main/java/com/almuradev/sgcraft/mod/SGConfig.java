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

import com.almuradev.sgcraft.Platform;
import com.almuradev.sgcraft.SGCraft;
import com.almuradev.sgcraft.api.address.Symbol;
import com.almuradev.sgcraft.worldgen.StructureGate;
import com.almuradev.sgcraft.worldgen.StructureGates;
import com.almuradev.sgcraft.worldgen.VillageGates;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * The server's settings, read from {@code config/sgcraft-server.json} as the server starts. A missing file is written
 * with the defaults, and a missing setting takes its default. The energy settings are the original SGCraft's.
 *
 * @param energyPerNaquadah how much energy one naquadah gives
 * @param dialCosts what a wormhole costs the gate that dials it, before distance: as each chevron locks, as it opens,
 *     and each tick it stays open
 * @param distanceFactorMultiplier how much more a wormhole costs the further it reaches
 * @param interWorldMultiplier how many times more a wormhole to another world costs
 * @param freeInCreative whether players in creative mode dial without energy; off unless set, as the original
 *     charged them like anyone else
 * @param bridgeRates how many of SGCraft's energy units one unit of each bridged energy system is worth, by the
 *     bridge's registry identifier, in place of the bridge's own rate
 * @param toolRange how far from a player the handheld tools look for a gate, in blocks
 * @param supergateEnergyMultiplier how many times more a wormhole to or from a supergate costs, as in the original
 * @param pointOfOrigin the symbol every address ends with, which the last chevron locks, as on the show; {@code @}
 *     (Giza) by default, or empty for addresses without one
 * @param structureGates the gates added beside generated structures, by the structure's identifier; a structure
 *     left out gets none
 * @param villageGates the gate plazas villages build
 * @param powerUnitCapacity the most FE a power unit holds, the original's four million unless set
 */
public record SGConfig(double energyPerNaquadah, DialCosts dialCosts, double distanceFactorMultiplier,
        double interWorldMultiplier, boolean freeInCreative,
        Map<Identifier, Double> bridgeRates, int toolRange, double supergateEnergyMultiplier,
        String pointOfOrigin, Map<Identifier, StructureGate> structureGates, VillageGates villageGates,
        long powerUnitCapacity) {

    public static final SGConfig DEFAULTS = new SGConfig(96000, DialCosts.DEFAULTS, 1.0, 4.0, false, Map.of(), 6,
            8.0, "@", StructureGates.DEFAULT_STRUCTURES, StructureGates.DEFAULT_VILLAGES, 4_000_000L);

    private static final String FILE = "sgcraft-server.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final Codec<SGConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("energy_per_naquadah", SGConfig.DEFAULTS.energyPerNaquadah)
                    .forGetter(SGConfig::energyPerNaquadah),
            DialCosts.CODEC.optionalFieldOf("dial_costs", SGConfig.DEFAULTS.dialCosts)
                    .forGetter(SGConfig::dialCosts),
            Codec.DOUBLE.optionalFieldOf("distance_factor_multiplier", SGConfig.DEFAULTS.distanceFactorMultiplier)
                    .forGetter(SGConfig::distanceFactorMultiplier),
            Codec.DOUBLE.optionalFieldOf("inter_world_multiplier", SGConfig.DEFAULTS.interWorldMultiplier)
                    .forGetter(SGConfig::interWorldMultiplier),
            Codec.BOOL.optionalFieldOf("free_in_creative", SGConfig.DEFAULTS.freeInCreative)
                    .forGetter(SGConfig::freeInCreative),
            Codec.unboundedMap(Identifier.CODEC, Codec.DOUBLE).optionalFieldOf("bridge_rates", Map.of())
                    .forGetter(SGConfig::bridgeRates),
            Codec.INT.optionalFieldOf("tool_range", SGConfig.DEFAULTS.toolRange).forGetter(SGConfig::toolRange),
            Codec.DOUBLE.optionalFieldOf("supergate_energy_multiplier", SGConfig.DEFAULTS.supergateEnergyMultiplier)
                    .forGetter(SGConfig::supergateEnergyMultiplier),
            Codec.STRING.optionalFieldOf("point_of_origin", SGConfig.DEFAULTS.pointOfOrigin)
                    .forGetter(SGConfig::pointOfOrigin),
            Codec.unboundedMap(Identifier.CODEC, StructureGate.CODEC)
                    .optionalFieldOf("structure_gates", SGConfig.DEFAULTS.structureGates)
                    .forGetter(SGConfig::structureGates),
            VillageGates.CODEC.optionalFieldOf("village_gates", SGConfig.DEFAULTS.villageGates)
                    .forGetter(SGConfig::villageGates),
            Codec.LONG.optionalFieldOf("power_unit_capacity", SGConfig.DEFAULTS.powerUnitCapacity)
                    .forGetter(SGConfig::powerUnitCapacity)
    ).apply(instance, SGConfig::new));

    /** @return the symbol every address ends with, if addresses have one; a setting that names none has none */
    public Optional<Symbol> originSymbol() {
        return this.pointOfOrigin.length() == 1 ? Symbol.of(this.pointOfOrigin.charAt(0)) : Optional.empty();
    }

    /** @return these settings with other dial costs */
    public SGConfig withDialCosts(final DialCosts costs) {
        return new SGConfig(this.energyPerNaquadah, costs, this.distanceFactorMultiplier, this.interWorldMultiplier,
                this.freeInCreative, this.bridgeRates, this.toolRange, this.supergateEnergyMultiplier,
                this.pointOfOrigin, this.structureGates, this.villageGates, this.powerUnitCapacity);
    }

    /**
     * Writes dial costs to the settings file, leaving the rest of the file as it is.
     *
     * @return whether the file was written
     */
    public static boolean saveDialCosts(final DialCosts costs) {
        final Path path = Platform.get().configDir().resolve(SGConfig.FILE);
        try {
            final JsonObject json = Files.exists(path) && JsonParser.parseString(Files.readString(path))
                    instanceof JsonObject existing ? existing : new JsonObject();
            json.add("dial_costs", DialCosts.CODEC.encodeStart(JsonOps.INSTANCE, costs).getOrThrow());
            Files.createDirectories(path.getParent());
            Files.writeString(path, SGConfig.GSON.toJson(json));
            return true;
        } catch (final IOException | RuntimeException e) {
            SGCraft.LOG.error("Couldn't write the dial costs to {}", path, e);
            return false;
        }
    }

    /** Reads the settings, writing the file with the defaults first if there is none. */
    public static SGConfig load() {
        final Path path = Platform.get().configDir().resolve(SGConfig.FILE);
        try {
            if (Files.notExists(path)) {
                Files.createDirectories(path.getParent());
                final JsonElement json = SGConfig.CODEC.encodeStart(JsonOps.INSTANCE, SGConfig.DEFAULTS).getOrThrow();
                Files.writeString(path, SGConfig.GSON.toJson(json));
                return SGConfig.DEFAULTS;
            }
            final JsonElement json = JsonParser.parseString(Files.readString(path));
            return SGConfig.CODEC.parse(JsonOps.INSTANCE, json).resultOrPartial(error ->
                    SGCraft.LOG.error("{} has a mistake, so some settings take their defaults: {}", path, error))
                    .orElse(SGConfig.DEFAULTS);
        } catch (final IOException | RuntimeException e) {
            SGCraft.LOG.error("Couldn't read {}, so every setting takes its default", path, e);
            return SGConfig.DEFAULTS;
        }
    }
}
