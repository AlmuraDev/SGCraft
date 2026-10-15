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

import com.almuradev.sgcraft.api.address.Symbol;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.energy.DialCosts;
import com.almuradev.sgcraft.mod.malp.MalpSettings;
import com.almuradev.sgcraft.mod.view.RemoteViewConfig;
import com.almuradev.sgcraft.mod.worldgen.NaquadahOreSettings;
import com.almuradev.sgcraft.mod.worldgen.StructureGate;
import com.almuradev.sgcraft.mod.worldgen.StructureGates;
import com.almuradev.sgcraft.mod.worldgen.VillageGates;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

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
 * @param naquadahOre how naquadah ore generates, and whether chunks made without it get it, as read when the server
 *     started; {@link com.almuradev.sgcraft.mod.worldgen.NaquadahOre} keeps them as changed since
 * @param remoteView how much of the world around a remote view's source its viewer is sent
 * @param malp the settings for the MALP and its batteries
 * @param options the server's other settings, kept in the same file beside these
 */
public record SGConfig(double energyPerNaquadah, DialCosts dialCosts, double distanceFactorMultiplier,
        double interWorldMultiplier, boolean freeInCreative,
        Map<Identifier, Double> bridgeRates, int toolRange, double supergateEnergyMultiplier,
        String pointOfOrigin, Map<Identifier, StructureGate> structureGates, VillageGates villageGates,
        long powerUnitCapacity, NaquadahOreSettings naquadahOre, RemoteViewConfig remoteView, MalpSettings malp,
        Options options) {

    /** The most chunks around a gate's own that can be kept loaded. */
    public static final int MAX_CHUNK_LOADING_RANGE = 8;

    public static final SGConfig DEFAULTS = new SGConfig(96000, DialCosts.DEFAULTS, 1.0, 4.0, false, Map.of(), 6,
            8.0, "@", StructureGates.DEFAULT_STRUCTURES, StructureGates.DEFAULT_VILLAGES, 4_000_000L,
            NaquadahOreSettings.DEFAULTS, RemoteViewConfig.DEFAULTS, MalpSettings.DEFAULTS, Options.DEFAULTS);

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
                    .forGetter(SGConfig::powerUnitCapacity),
            NaquadahOreSettings.CODEC.optionalFieldOf("naquadah_ore", SGConfig.DEFAULTS.naquadahOre)
                    .forGetter(SGConfig::naquadahOre),
            RemoteViewConfig.CODEC.optionalFieldOf("remote_view", SGConfig.DEFAULTS.remoteView)
                    .forGetter(SGConfig::remoteView),
            MalpSettings.CODEC.optionalFieldOf("malp", SGConfig.DEFAULTS.malp).forGetter(SGConfig::malp),
            Options.MAP_CODEC.forGetter(SGConfig::options)
    ).apply(instance, SGConfig::new));

    /**
     * The server's settings beyond energy and worldgen gates. They sit in the same file at the top level, beside the
     * rest; the record only keeps the settings record within the codec's field limit.
     *
     * @param kawooshBreaksUnbreakable whether a kawoosh clears blocks that can't be broken, such as bedrock; off
     *     unless set, as in the original
     * @param chunkLoadingRange how many chunks around a gate's own stay loaded while it is busy, as the original's
     *     {@code chunkLoadingRange}; 1 unless set
     * @param logStargateEvents whether gates forming and breaking, and wormholes opening, closing and going astray,
     *     are written to the server log; off unless set, as in the original
     * @param tokraWorlds whether Tok'ra stand at the gates generated in each world, by the world's identifier, as
     *     the original's {@code tokra.yml}; a world set off has none, and a world left out has them wherever its
     *     structures' settings say
     * @param gateDefaults the values gates have for a property when they have none of their own, in place of the
     *     property's default, by the property's identifier, written as JSON; the iris, and {@link #CHEVRONS} for how
     *     many chevrons, are given to gates as they first form instead, as the original's {@code irisUpgrade} and
     *     {@code chevronUpgrade}
     * @param ownersBreakGates whether a gate's owner may break it whatever its {@code breakable} property says, as
     *     the original's {@code canPlayerBreakPlayerGate}; on unless set
     * @param generatedGatesBreakable whether players may break generated gates that have no {@code breakable} value
     *     of their own, as the original's {@code canPlayerBreakGeneratedGate}; off unless set
     */
    public record Options(boolean kawooshBreaksUnbreakable, int chunkLoadingRange, boolean logStargateEvents,
            Map<Identifier, Boolean> tokraWorlds, Map<Identifier, JsonElement> gateDefaults, boolean ownersBreakGates,
            boolean generatedGatesBreakable) {

        public static final Options DEFAULTS = new Options(false, 1, false,
                Map.of(Level.OVERWORLD.identifier(), true), Map.of(), true, false);

        private static final Codec<Map<Identifier, JsonElement>> GATE_DEFAULTS_CODEC =
                Codec.unboundedMap(Identifier.CODEC, ExtraCodecs.JSON);

        public static final MapCodec<Options> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("kawoosh_breaks_unbreakable", Options.DEFAULTS.kawooshBreaksUnbreakable)
                        .forGetter(Options::kawooshBreaksUnbreakable),
                Codec.intRange(0, SGConfig.MAX_CHUNK_LOADING_RANGE)
                        .optionalFieldOf("chunk_loading_range", Options.DEFAULTS.chunkLoadingRange)
                        .forGetter(Options::chunkLoadingRange),
                Codec.BOOL.optionalFieldOf("log_stargate_events", Options.DEFAULTS.logStargateEvents)
                        .forGetter(Options::logStargateEvents),
                Codec.unboundedMap(Identifier.CODEC, Codec.BOOL)
                        .optionalFieldOf("tokra_worlds", Options.DEFAULTS.tokraWorlds)
                        .forGetter(Options::tokraWorlds),
                Options.GATE_DEFAULTS_CODEC.optionalFieldOf("gate_defaults", Options.DEFAULTS.gateDefaults)
                        .forGetter(Options::gateDefaults),
                Codec.BOOL.optionalFieldOf("owners_break_gates", Options.DEFAULTS.ownersBreakGates)
                        .forGetter(Options::ownersBreakGates),
                Codec.BOOL.optionalFieldOf("generated_gates_breakable", Options.DEFAULTS.generatedGatesBreakable)
                        .forGetter(Options::generatedGatesBreakable)
        ).apply(instance, Options::new));

        public Options {
            gateDefaults = Map.copyOf(gateDefaults);
        }

        Options withKawooshBreaksUnbreakable(final boolean breaks) {
            return new Options(breaks, this.chunkLoadingRange, this.logStargateEvents, this.tokraWorlds,
                    this.gateDefaults, this.ownersBreakGates, this.generatedGatesBreakable);
        }

        Options withChunkLoadingRange(final int range) {
            return new Options(this.kawooshBreaksUnbreakable, range, this.logStargateEvents, this.tokraWorlds,
                    this.gateDefaults, this.ownersBreakGates, this.generatedGatesBreakable);
        }

        Options withLogStargateEvents(final boolean log) {
            return new Options(this.kawooshBreaksUnbreakable, this.chunkLoadingRange, log, this.tokraWorlds,
                    this.gateDefaults, this.ownersBreakGates, this.generatedGatesBreakable);
        }

        Options withGateDefaults(final Map<Identifier, JsonElement> defaults) {
            return new Options(this.kawooshBreaksUnbreakable, this.chunkLoadingRange, this.logStargateEvents,
                    this.tokraWorlds, defaults, this.ownersBreakGates, this.generatedGatesBreakable);
        }

        Options withBreaking(final boolean owners, final boolean generated) {
            return new Options(this.kawooshBreaksUnbreakable, this.chunkLoadingRange, this.logStargateEvents,
                    this.tokraWorlds, this.gateDefaults, owners, generated);
        }
    }

    /** The key in {@link Options#gateDefaults()} for how many chevrons gates have as they first form. */
    public static final Identifier CHEVRONS = SGCraft.id("chevrons");

    /** @return the values gates have for a property when they have none of their own, by property identifier */
    public Map<Identifier, JsonElement> gateDefaults() {
        return this.options.gateDefaults();
    }

    /** @return whether a gate's owner may break it whatever its {@code breakable} property says */
    public boolean ownersBreakGates() {
        return this.options.ownersBreakGates();
    }

    /** @return whether players may break generated gates with no {@code breakable} value of their own */
    public boolean generatedGatesBreakable() {
        return this.options.generatedGatesBreakable();
    }

    /** @return the symbol every address ends with, if addresses have one; a setting that names none has none */
    public Optional<Symbol> originSymbol() {
        return this.pointOfOrigin.length() == 1 ? Symbol.of(this.pointOfOrigin.charAt(0)) : Optional.empty();
    }

    /** @return whether a kawoosh clears blocks that can't be broken */
    public boolean kawooshBreaksUnbreakable() {
        return this.options.kawooshBreaksUnbreakable();
    }

    /** @return how many chunks around a gate's own stay loaded while it is busy */
    public int chunkLoadingRange() {
        return this.options.chunkLoadingRange();
    }

    /** @return whether gate events are written to the server log */
    public boolean logStargateEvents() {
        return this.options.logStargateEvents();
    }

    /** @return whether Tok'ra stand at the gates generated in each world, by the world's identifier */
    public Map<Identifier, Boolean> tokraWorlds() {
        return this.options.tokraWorlds();
    }

    /** @return how much energy one naquadah gives a gate: its own setting, or the server's */
    public double energyPerNaquadah(final Stargate gate) {
        return gate.get(GateProperties.ENERGY_PER_NAQUADAH).orElse(this.energyPerNaquadah);
    }

    /** @return how much more a wormhole a gate dials costs the further it reaches: its own setting, or the server's */
    public double distanceFactorMultiplier(final Stargate gate) {
        return gate.get(GateProperties.DISTANCE_FACTOR_MULTIPLIER).orElse(this.distanceFactorMultiplier);
    }

    /** @return how many times more a wormhole a gate dials to another world costs: its own setting, or the server's */
    public double interWorldMultiplier(final Stargate gate) {
        return gate.get(GateProperties.INTER_WORLD_MULTIPLIER).orElse(this.interWorldMultiplier);
    }

    /** @return these settings with other dial costs */
    public SGConfig withDialCosts(final DialCosts costs) {
        return new SGConfig(this.energyPerNaquadah, costs, this.distanceFactorMultiplier, this.interWorldMultiplier,
                this.freeInCreative, this.bridgeRates, this.toolRange, this.supergateEnergyMultiplier,
                this.pointOfOrigin, this.structureGates, this.villageGates, this.powerUnitCapacity, this.naquadahOre,
                this.remoteView, this.malp, this.options);
    }

    /** @return these settings with other options */
    public SGConfig withOptions(final Options options) {
        return new SGConfig(this.energyPerNaquadah, this.dialCosts, this.distanceFactorMultiplier,
                this.interWorldMultiplier, this.freeInCreative, this.bridgeRates, this.toolRange,
                this.supergateEnergyMultiplier, this.pointOfOrigin, this.structureGates, this.villageGates,
                this.powerUnitCapacity, this.naquadahOre, this.remoteView, this.malp, options);
    }

    /** @return these settings with other MALP settings */
    public SGConfig withMalp(final MalpSettings malp) {
        return new SGConfig(this.energyPerNaquadah, this.dialCosts, this.distanceFactorMultiplier,
                this.interWorldMultiplier, this.freeInCreative, this.bridgeRates, this.toolRange,
                this.supergateEnergyMultiplier, this.pointOfOrigin, this.structureGates, this.villageGates,
                this.powerUnitCapacity, this.naquadahOre, this.remoteView, malp, this.options);
    }

    /** @return these settings with the kawoosh clearing blocks that can't be broken, or not */
    public SGConfig withKawooshBreaksUnbreakable(final boolean breaks) {
        return this.withOptions(this.options.withKawooshBreaksUnbreakable(breaks));
    }

    /** @return these settings with another chunk loading range */
    public SGConfig withChunkLoadingRange(final int range) {
        return this.withOptions(this.options.withChunkLoadingRange(range));
    }

    /** @return these settings with gate events logged or not */
    public SGConfig withLogStargateEvents(final boolean log) {
        return this.withOptions(this.options.withLogStargateEvents(log));
    }

    /** @return these settings with other values for gates without their own */
    public SGConfig withGateDefaults(final Map<Identifier, JsonElement> defaults) {
        return this.withOptions(this.options.withGateDefaults(defaults));
    }

    /** @return these settings with owners and generated gates breakable or not */
    public SGConfig withBreaking(final boolean ownersBreakGates, final boolean generatedGatesBreakable) {
        return this.withOptions(this.options.withBreaking(ownersBreakGates, generatedGatesBreakable));
    }

    /**
     * Writes the values for gates without their own to the settings file, leaving the rest of the file as it is.
     *
     * @return whether the file was written
     */
    public static boolean saveGateDefaults(final Map<Identifier, JsonElement> defaults) {
        return SGConfig.save("gate_defaults", Codec.unboundedMap(Identifier.CODEC, ExtraCodecs.JSON), defaults);
    }

    /**
     * Writes whether owners may break their gates to the settings file, leaving the rest of the file as it is.
     *
     * @return whether the file was written
     */
    public static boolean saveOwnersBreakGates(final boolean on) {
        return SGConfig.save("owners_break_gates", Codec.BOOL, on);
    }

    /**
     * Writes whether generated gates may be broken to the settings file, leaving the rest of the file as it is.
     *
     * @return whether the file was written
     */
    public static boolean saveGeneratedGatesBreakable(final boolean on) {
        return SGConfig.save("generated_gates_breakable", Codec.BOOL, on);
    }

    /**
     * Writes dial costs to the settings file, leaving the rest of the file as it is.
     *
     * @return whether the file was written
     */
    public static boolean saveDialCosts(final DialCosts costs) {
        return SGConfig.save("dial_costs", DialCosts.CODEC, costs);
    }

    /**
     * Writes whether a kawoosh clears blocks that can't be broken to the settings file, leaving the rest of the file
     * as it is.
     *
     * @return whether the file was written
     */
    public static boolean saveKawooshBreaksUnbreakable(final boolean breaks) {
        return SGConfig.save("kawoosh_breaks_unbreakable", Codec.BOOL, breaks);
    }

    /**
     * Writes the chunk loading range to the settings file, leaving the rest of the file as it is.
     *
     * @return whether the file was written
     */
    public static boolean saveChunkLoadingRange(final int range) {
        return SGConfig.save("chunk_loading_range", Codec.INT, range);
    }

    /**
     * Writes whether gate events are logged to the settings file, leaving the rest of the file as it is.
     *
     * @return whether the file was written
     */
    public static boolean saveLogStargateEvents(final boolean log) {
        return SGConfig.save("log_stargate_events", Codec.BOOL, log);
    }

    /**
     * Writes the worlds Tok'ra stand in to the settings file, leaving the rest of the file as it is.
     *
     * @return whether the file was written
     */
    public static boolean saveTokraWorlds(final Map<Identifier, Boolean> worlds) {
        return SGConfig.save("tokra_worlds", Codec.unboundedMap(Identifier.CODEC, Codec.BOOL), worlds);
    }

    /**
     * Writes the naquadah ore settings to the settings file, leaving the rest of the file as it is.
     *
     * @return whether the file was written
     */
    public static boolean saveNaquadahOre(final NaquadahOreSettings settings) {
        return SGConfig.save("naquadah_ore", NaquadahOreSettings.CODEC, settings);
    }

    /**
     * Writes whether damage takes players off MALP consoles to the settings file, leaving the rest of the file as it
     * is.
     *
     * @return whether the file was written
     */
    public static boolean saveConsoleDamageInterrupts(final boolean on) {
        return SGConfig.save("malp", "console_damage_interrupts", Codec.BOOL, on);
    }

    /**
     * Writes how many players near a driven MALP console see the feed on its monitor to the settings file, leaving
     * the rest of the file as it is.
     *
     * @return whether the file was written
     */
    public static boolean saveConsoleSpectators(final int spectators) {
        return SGConfig.save("malp", "console_spectators", Codec.INT, spectators);
    }

    /**
     * Writes how near a driven MALP console players see the feed on its monitor to the settings file, leaving the
     * rest of the file as it is.
     *
     * @return whether the file was written
     */
    public static boolean saveSpectatorRange(final int range) {
        return SGConfig.save("malp", "spectator_range", Codec.INT, range);
    }

    /**
     * Writes how much FE a MALP tablet uses each tick while it shows a live feed to the settings file, leaving the rest
     * of the file as it is.
     *
     * @return whether the file was written
     */
    public static boolean saveTabletDrain(final long drain) {
        return SGConfig.save("malp", "tablet_drain", Codec.LONG, drain);
    }

    // Writes one setting, leaving the rest of the file as it is
    private static <T> boolean save(final String key, final Codec<T> codec, final T value) {
        return SGConfig.save(null, key, codec, value);
    }

    // Writes one setting at the top level, or within a section, leaving the rest of the file as it is
    private static <T> boolean save(final @Nullable String section, final String key, final Codec<T> codec,
            final T value) {
        final Path path = Platform.get().configDir().resolve(SGConfig.FILE);
        try {
            final JsonObject json = Files.exists(path) && JsonParser.parseString(Files.readString(path))
                    instanceof JsonObject existing ? existing : new JsonObject();
            JsonObject into = json;
            if (section != null) {
                into = json.get(section) instanceof JsonObject own ? own : new JsonObject();
                json.add(section, into);
            }
            into.add(key, codec.encodeStart(JsonOps.INSTANCE, value).getOrThrow());
            Files.createDirectories(path.getParent());
            Files.writeString(path, SGConfig.GSON.toJson(json));
            return true;
        } catch (final IOException | RuntimeException e) {
            SGCraft.LOG.error("Couldn't write {} to {}", key, path, e);
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
