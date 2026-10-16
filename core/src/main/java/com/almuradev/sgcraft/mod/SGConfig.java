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
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.address.AddressSettings;
import com.almuradev.sgcraft.mod.energy.DialCosts;
import com.almuradev.sgcraft.mod.malp.MalpSettings;
import com.almuradev.sgcraft.mod.shield.ShieldSettings;
import com.almuradev.sgcraft.mod.view.RemoteViewConfig;
import com.almuradev.sgcraft.mod.worldgen.NaquadahOreSettings;
import com.almuradev.sgcraft.mod.worldgen.StructureGate;
import com.almuradev.sgcraft.mod.worldgen.StructureGates;
import com.almuradev.sgcraft.mod.worldgen.VillageGates;
import com.almuradev.sgcraft.mod.zpm.ZpmSettings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The server's settings, read from {@code config/sgcraft-server.json} as the server starts. A missing file is written
 * with the defaults, and a missing setting takes its default. The energy settings are the original SGCraft's.
 *
 * <p>The file keeps its settings in sections: {@code energy} (naquadah, power units, the energy bridges' rates, dial
 * costs and their {@code multipliers}), {@code addresses}, {@code gates}, {@code tools}, {@code worldgen},
 * {@code remote_view}, {@code malp}, {@code shields} and {@code zpm}. The records here keep the shape the rest of
 * the mod reads them in.</p>
 *
 * @param energyPerNaquadah how much energy one naquadah gives
 * @param dialCosts what a wormhole costs the gate that dials it, before distance: as each chevron locks, as it opens,
 *     and each tick it stays open
 * @param distanceFactorMultiplier how much more a wormhole costs the further it reaches
 * @param interWorldMultiplier how many times more a wormhole costs with an eighth chevron, which reaches another
 *     world
 * @param freeInCreative whether players in creative mode dial without energy; off unless set, as the original
 *     charged them like anyone else
 * @param fePerGeIn how many FE buy one gate energy coming in, from cables and other mods' chargers; 80 unless set
 * @param fePerGeOut how many FE one gate energy gives going out, from an energy converter or a battery other mods draw
 *     on; 80 unless set
 * @param converterRate the most gate energy an energy converter turns into FE each tick
 * @param bridgeRates how many of SGCraft's energy units one unit of each other bridged energy system is worth, by the
 *     bridge's registry identifier, in place of the bridge's own rate
 * @param toolRange how far from a player the handheld tools look for a gate, in blocks
 * @param supergateEnergyMultiplier how many times more a wormhole to or from a supergate costs, as in the original
 * @param addresses how worlds are laid out into addresses: the size of each addressed cell and the point of origin
 *     addresses end with, for every world and for single worlds
 * @param structureGates the gates added beside generated structures, by the structure's identifier; a structure
 *     left out gets none
 * @param villageGates the gate plazas villages build
 * @param powerUnitCapacity the most gate energy a power unit holds, the original's four million FE's worth unless
 *     set
 * @param naquadahOre how naquadah ore generates, and whether chunks made without it get it, as read when the server
 *     started; {@link com.almuradev.sgcraft.mod.worldgen.NaquadahOre} keeps them as changed since
 * @param remoteView how much of the world around a remote view's source its viewer is sent
 * @param malp the settings for the MALP and its batteries
 * @param options the server's other settings, kept in the same file beside these
 */
public record SGConfig(double energyPerNaquadah, DialCosts dialCosts, double distanceFactorMultiplier,
        double interWorldMultiplier, boolean freeInCreative,
        double fePerGeIn, double fePerGeOut, double converterRate, Map<Identifier, Double> bridgeRates, int toolRange,
        double supergateEnergyMultiplier,
        AddressSettings addresses, Map<Identifier, StructureGate> structureGates, VillageGates villageGates,
        long powerUnitCapacity, NaquadahOreSettings naquadahOre, RemoteViewConfig remoteView, MalpSettings malp,
        Options options) {

    /** The most chunks around a gate's own that can be kept loaded. */
    public static final int MAX_CHUNK_LOADING_RANGE = 8;

    public static final SGConfig DEFAULTS = new SGConfig(96000, DialCosts.DEFAULTS, 1.0, 10.0, false, 80.0, 80.0, 62.5,
            Map.of(), 6,
            8.0, AddressSettings.DEFAULTS, StructureGates.DEFAULT_STRUCTURES, StructureGates.DEFAULT_VILLAGES, 50_000L,
            NaquadahOreSettings.DEFAULTS, RemoteViewConfig.DEFAULTS, MalpSettings.DEFAULTS, Options.DEFAULTS);

    private static final String FILE = "sgcraft-server.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Declared before the codecs that use it, which are built as the class loads
    private static final Codec<Map<Identifier, JsonElement>> GATE_DEFAULTS_CODEC =
            Codec.unboundedMap(Identifier.CODEC, ExtraCodecs.JSON);

    public static final Codec<SGConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EnergySection.CODEC.optionalFieldOf("energy", EnergySection.of(SGConfig.DEFAULTS))
                    .forGetter(EnergySection::of),
            AddressSettings.CODEC.optionalFieldOf("addresses", SGConfig.DEFAULTS.addresses)
                    .forGetter(SGConfig::addresses),
            GatesSection.CODEC.optionalFieldOf("gates", GatesSection.of(SGConfig.DEFAULTS))
                    .forGetter(GatesSection::of),
            ToolsSection.CODEC.optionalFieldOf("tools", ToolsSection.of(SGConfig.DEFAULTS))
                    .forGetter(ToolsSection::of),
            WorldgenSection.CODEC.optionalFieldOf("worldgen", WorldgenSection.of(SGConfig.DEFAULTS))
                    .forGetter(WorldgenSection::of),
            RemoteViewConfig.CODEC.optionalFieldOf("remote_view", SGConfig.DEFAULTS.remoteView)
                    .forGetter(SGConfig::remoteView),
            MalpSettings.CODEC.optionalFieldOf("malp", SGConfig.DEFAULTS.malp).forGetter(SGConfig::malp),
            ShieldSettings.CODEC.optionalFieldOf("shields", SGConfig.DEFAULTS.options.shields())
                    .forGetter(config -> config.options.shields()),
            ZpmSettings.CODEC.optionalFieldOf("zpm", SGConfig.DEFAULTS.options.zpm())
                    .forGetter(config -> config.options.zpm())
    ).apply(instance, (energy, addresses, gates, tools, worldgen, remoteView, malp, shields, zpm) -> new SGConfig(
            energy.energyPerNaquadah(), energy.dialCosts(), energy.multipliers().distanceFactor(),
            energy.multipliers().interWorld(), energy.freeInCreative(), energy.rates().fePerGeIn(),
            energy.rates().fePerGeOut(),
            energy.converterRate(), energy.rates().others(),
            tools.range(),
            energy.multipliers().supergate(), addresses, worldgen.structureGates(), worldgen.villageGates(),
            energy.powerUnitCapacity(), worldgen.naquadahOre(), remoteView, malp,
            new Options(gates.kawooshBreaksUnbreakable(), gates.chunkLoadingRange(), gates.logEvents(),
                    worldgen.tokraWorlds(), gates.defaults(), gates.ownersBreakGates(),
                    gates.generatedGatesBreakable(), shields, zpm, energy.multipliers().otherGroup(),
                    gates.wormholesJump()))));

    // The file's energy section: what naquadah and power units hold, the energy bridges' rates, and what dials cost
    private record EnergySection(double energyPerNaquadah, long powerUnitCapacity, BridgeRates rates,
            double converterRate, boolean freeInCreative, DialCosts dialCosts, MultiplierSection multipliers) {

        static final Codec<EnergySection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.optionalFieldOf("per_naquadah", SGConfig.DEFAULTS.energyPerNaquadah)
                        .forGetter(EnergySection::energyPerNaquadah),
                Codec.LONG.optionalFieldOf("power_unit_capacity", SGConfig.DEFAULTS.powerUnitCapacity)
                        .forGetter(EnergySection::powerUnitCapacity),
                BridgeRates.CODEC.optionalFieldOf("bridge_rates", BridgeRates.of(SGConfig.DEFAULTS))
                        .forGetter(EnergySection::rates),
                Codec.doubleRange(0, Double.MAX_VALUE)
                        .optionalFieldOf("converter_rate", SGConfig.DEFAULTS.converterRate)
                        .forGetter(EnergySection::converterRate),
                Codec.BOOL.optionalFieldOf("free_in_creative", SGConfig.DEFAULTS.freeInCreative)
                        .forGetter(EnergySection::freeInCreative),
                DialCosts.CODEC.optionalFieldOf("dial_costs", SGConfig.DEFAULTS.dialCosts)
                        .forGetter(EnergySection::dialCosts),
                MultiplierSection.CODEC.optionalFieldOf("multipliers", MultiplierSection.of(SGConfig.DEFAULTS))
                        .forGetter(EnergySection::multipliers)
        ).apply(instance, EnergySection::new));

        static EnergySection of(final SGConfig config) {
            return new EnergySection(config.energyPerNaquadah, config.powerUnitCapacity, BridgeRates.of(config),
                    config.converterRate,
                    config.freeInCreative, config.dialCosts, MultiplierSection.of(config));
        }
    }

    // The energy bridges' rates: FE's, as how many FE buy one gate energy coming in and how many it gives going out,
    // and any other bridge's by its identifier, as how much gate energy one of its units is worth
    private record BridgeRates(double fePerGeIn, double fePerGeOut, Map<Identifier, Double> others) {

        static final String FE_IN = "fe_in_per_ge";
        static final String FE_OUT = "fe_out_per_ge";

        static BridgeRates of(final SGConfig config) {
            return new BridgeRates(config.fePerGeIn, config.fePerGeOut, config.bridgeRates);
        }

        static final Codec<BridgeRates> CODEC = Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).comapFlatMap(
                BridgeRates::read, BridgeRates::write);

        private static DataResult<BridgeRates> read(final Map<String, Double> rates) {
            final double in = rates.getOrDefault(BridgeRates.FE_IN, SGConfig.DEFAULTS.fePerGeIn);
            final double out = rates.getOrDefault(BridgeRates.FE_OUT, SGConfig.DEFAULTS.fePerGeOut);
            if (in <= 0 || out <= 0) {
                return DataResult.error(() -> "fe_in_per_ge and fe_out_per_ge must be above 0: " + in + ", " + out);
            }
            final Map<Identifier, Double> others = new LinkedHashMap<>();
            for (final Map.Entry<String, Double> rate : rates.entrySet()) {
                if (rate.getKey().equals(BridgeRates.FE_IN) || rate.getKey().equals(BridgeRates.FE_OUT)) {
                    continue;
                }
                final Identifier id = Identifier.tryParse(rate.getKey());
                if (id == null) {
                    return DataResult.error(() -> "Not a bridge's identifier: " + rate.getKey());
                }
                if (id.equals(SGBuiltIns.FORGE_ENERGY_BRIDGE)) {
                    return DataResult.error(() -> "Set FE's rates with fe_in_per_ge and fe_out_per_ge, not "
                            + rate.getKey());
                }
                others.put(id, rate.getValue());
            }
            return DataResult.success(new BridgeRates(in, out, Map.copyOf(others)));
        }

        private Map<String, Double> write() {
            final Map<String, Double> rates = new LinkedHashMap<>();
            rates.put(BridgeRates.FE_IN, this.fePerGeIn);
            rates.put(BridgeRates.FE_OUT, this.fePerGeOut);
            this.others.forEach((id, rate) -> rates.put(id.toString(), rate));
            return rates;
        }
    }

    // How many times more a wormhole costs: further away, with an eighth chevron, with a ninth, and to a supergate
    private record MultiplierSection(double distanceFactor, double interWorld, double otherGroup, double supergate) {

        static final Codec<MultiplierSection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.optionalFieldOf("distance_factor", SGConfig.DEFAULTS.distanceFactorMultiplier)
                        .forGetter(MultiplierSection::distanceFactor),
                Codec.DOUBLE.optionalFieldOf("inter_world", SGConfig.DEFAULTS.interWorldMultiplier)
                        .forGetter(MultiplierSection::interWorld),
                Codec.doubleRange(0, Double.MAX_VALUE)
                        .optionalFieldOf("other_group", SGConfig.DEFAULTS.options.otherGroupMultiplier())
                        .forGetter(MultiplierSection::otherGroup),
                Codec.DOUBLE.optionalFieldOf("supergate", SGConfig.DEFAULTS.supergateEnergyMultiplier)
                        .forGetter(MultiplierSection::supergate)
        ).apply(instance, MultiplierSection::new));

        static MultiplierSection of(final SGConfig config) {
            return new MultiplierSection(config.distanceFactorMultiplier, config.interWorldMultiplier,
                    config.options.otherGroupMultiplier(), config.supergateEnergyMultiplier);
        }
    }

    // The file's gates section: chunk loading, the kawoosh, breaking gates, jumping wormholes, logging, and gates'
    // defaults
    private record GatesSection(int chunkLoadingRange, boolean kawooshBreaksUnbreakable,
            boolean ownersBreakGates, boolean generatedGatesBreakable, boolean wormholesJump, boolean logEvents,
            Map<Identifier, JsonElement> defaults) {

        static final Codec<GatesSection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(0, SGConfig.MAX_CHUNK_LOADING_RANGE)
                        .optionalFieldOf("chunk_loading_range", SGConfig.DEFAULTS.chunkLoadingRange())
                        .forGetter(GatesSection::chunkLoadingRange),
                Codec.BOOL.optionalFieldOf("kawoosh_breaks_unbreakable", SGConfig.DEFAULTS.kawooshBreaksUnbreakable())
                        .forGetter(GatesSection::kawooshBreaksUnbreakable),
                Codec.BOOL.optionalFieldOf("owners_break_gates", SGConfig.DEFAULTS.ownersBreakGates())
                        .forGetter(GatesSection::ownersBreakGates),
                Codec.BOOL.optionalFieldOf("generated_gates_breakable", SGConfig.DEFAULTS.generatedGatesBreakable())
                        .forGetter(GatesSection::generatedGatesBreakable),
                Codec.BOOL.optionalFieldOf("wormholes_jump", SGConfig.DEFAULTS.wormholesJump())
                        .forGetter(GatesSection::wormholesJump),
                Codec.BOOL.optionalFieldOf("log_events", SGConfig.DEFAULTS.logStargateEvents())
                        .forGetter(GatesSection::logEvents),
                SGConfig.GATE_DEFAULTS_CODEC.optionalFieldOf("defaults", SGConfig.DEFAULTS.gateDefaults())
                        .forGetter(GatesSection::defaults)
        ).apply(instance, GatesSection::new));

        static GatesSection of(final SGConfig config) {
            return new GatesSection(config.chunkLoadingRange(), config.kawooshBreaksUnbreakable(),
                    config.ownersBreakGates(), config.generatedGatesBreakable(), config.wormholesJump(),
                    config.logStargateEvents(), config.gateDefaults());
        }
    }

    // The file's tools section: how far the handheld tools look for a gate
    private record ToolsSection(int range) {

        static final Codec<ToolsSection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("range", SGConfig.DEFAULTS.toolRange).forGetter(ToolsSection::range)
        ).apply(instance, ToolsSection::new));

        static ToolsSection of(final SGConfig config) {
            return new ToolsSection(config.toolRange);
        }
    }

    // The file's worldgen section: naquadah ore, the gates beside structures and in villages, and the Tok'ra
    private record WorldgenSection(NaquadahOreSettings naquadahOre, Map<Identifier, StructureGate> structureGates,
            VillageGates villageGates, Map<Identifier, Boolean> tokraWorlds) {

        static final Codec<WorldgenSection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                NaquadahOreSettings.CODEC.optionalFieldOf("naquadah_ore", SGConfig.DEFAULTS.naquadahOre)
                        .forGetter(WorldgenSection::naquadahOre),
                Codec.unboundedMap(Identifier.CODEC, StructureGate.CODEC)
                        .optionalFieldOf("structure_gates", SGConfig.DEFAULTS.structureGates)
                        .forGetter(WorldgenSection::structureGates),
                VillageGates.CODEC.optionalFieldOf("village_gates", SGConfig.DEFAULTS.villageGates)
                        .forGetter(WorldgenSection::villageGates),
                Codec.unboundedMap(Identifier.CODEC, Codec.BOOL)
                        .optionalFieldOf("tokra_worlds", SGConfig.DEFAULTS.tokraWorlds())
                        .forGetter(WorldgenSection::tokraWorlds)
        ).apply(instance, WorldgenSection::new));

        static WorldgenSection of(final SGConfig config) {
            return new WorldgenSection(config.naquadahOre, config.structureGates, config.villageGates,
                    config.tokraWorlds());
        }
    }

    /**
     * The server's settings beyond energy and worldgen gates, kept apart only so the settings record stays small; the
     * file keeps each in its own section.
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
     * @param shields the settings for gate shields and city shields, kept under {@code shields}
     * @param zpm the settings for ZPMs, kept under {@code zpm}
     * @param otherGroupMultiplier how many times more a wormhole costs again with a ninth chevron, which reaches
     *     another group of worlds, on top of the eighth's
     * @param wormholesJump whether an open wormhole jumps to another gate in the same address cell when the gate it
     *     reaches is broken, as on the show; on unless set
     */
    public record Options(boolean kawooshBreaksUnbreakable, int chunkLoadingRange, boolean logStargateEvents,
            Map<Identifier, Boolean> tokraWorlds, Map<Identifier, JsonElement> gateDefaults, boolean ownersBreakGates,
            boolean generatedGatesBreakable, ShieldSettings shields, ZpmSettings zpm,
            double otherGroupMultiplier, boolean wormholesJump) {

        public static final Options DEFAULTS = new Options(false, 1, false,
                Map.of(Level.OVERWORLD.identifier(), true), Map.of(), true, false, ShieldSettings.DEFAULTS,
                ZpmSettings.DEFAULTS, 10.0, true);

        public Options {
            gateDefaults = Map.copyOf(gateDefaults);
        }

        Options withKawooshBreaksUnbreakable(final boolean breaks) {
            return new Options(breaks, this.chunkLoadingRange, this.logStargateEvents, this.tokraWorlds,
                    this.gateDefaults, this.ownersBreakGates, this.generatedGatesBreakable, this.shields,
                    this.zpm, this.otherGroupMultiplier, this.wormholesJump);
        }

        Options withChunkLoadingRange(final int range) {
            return new Options(this.kawooshBreaksUnbreakable, range, this.logStargateEvents, this.tokraWorlds,
                    this.gateDefaults, this.ownersBreakGates, this.generatedGatesBreakable, this.shields,
                    this.zpm, this.otherGroupMultiplier, this.wormholesJump);
        }

        Options withLogStargateEvents(final boolean log) {
            return new Options(this.kawooshBreaksUnbreakable, this.chunkLoadingRange, log, this.tokraWorlds,
                    this.gateDefaults, this.ownersBreakGates, this.generatedGatesBreakable, this.shields,
                    this.zpm, this.otherGroupMultiplier, this.wormholesJump);
        }

        Options withGateDefaults(final Map<Identifier, JsonElement> defaults) {
            return new Options(this.kawooshBreaksUnbreakable, this.chunkLoadingRange, this.logStargateEvents,
                    this.tokraWorlds, defaults, this.ownersBreakGates, this.generatedGatesBreakable, this.shields,
                    this.zpm, this.otherGroupMultiplier, this.wormholesJump);
        }

        Options withBreaking(final boolean owners, final boolean generated) {
            return new Options(this.kawooshBreaksUnbreakable, this.chunkLoadingRange, this.logStargateEvents,
                    this.tokraWorlds, this.gateDefaults, owners, generated, this.shields, this.zpm,
                    this.otherGroupMultiplier, this.wormholesJump);
        }

        Options withZpm(final ZpmSettings zpm) {
            return new Options(this.kawooshBreaksUnbreakable, this.chunkLoadingRange, this.logStargateEvents,
                    this.tokraWorlds, this.gateDefaults, this.ownersBreakGates, this.generatedGatesBreakable,
                    this.shields, zpm, this.otherGroupMultiplier,
                    this.wormholesJump);
        }

        Options withOtherGroupMultiplier(final double multiplier) {
            return new Options(this.kawooshBreaksUnbreakable, this.chunkLoadingRange, this.logStargateEvents,
                    this.tokraWorlds, this.gateDefaults, this.ownersBreakGates, this.generatedGatesBreakable,
                    this.shields, this.zpm, multiplier, this.wormholesJump);
        }

        Options withWormholesJump(final boolean jump) {
            return new Options(this.kawooshBreaksUnbreakable, this.chunkLoadingRange, this.logStargateEvents,
                    this.tokraWorlds, this.gateDefaults, this.ownersBreakGates, this.generatedGatesBreakable,
                    this.shields, this.zpm, this.otherGroupMultiplier, jump);
        }

        Options withShields(final ShieldSettings shields) {
            return new Options(this.kawooshBreaksUnbreakable, this.chunkLoadingRange, this.logStargateEvents,
                    this.tokraWorlds, this.gateDefaults, this.ownersBreakGates, this.generatedGatesBreakable, shields,
                    this.zpm, this.otherGroupMultiplier, this.wormholesJump);
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

    /** @return whether an open wormhole jumps to another gate in the same cell when the gate it reaches breaks */
    public boolean wormholesJump() {
        return this.options.wormholesJump();
    }

    /** @return these settings with wormholes jumping to another gate in the cell, or not */
    public SGConfig withWormholesJump(final boolean jump) {
        return this.withOptions(this.options.withWormholesJump(jump));
    }

    /**
     * Writes whether open wormholes jump to another gate in the cell to the settings file, leaving the rest of the
     * file as it is.
     *
     * @return whether the file was written
     */
    public static boolean saveWormholesJump(final boolean jump) {
        return SGConfig.save(Codec.BOOL, jump, "gates", "wormholes_jump");
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

    /** @return these settings with another multiplier for an eighth chevron and another for a ninth */
    public SGConfig withChevronMultipliers(final double interWorld, final double otherGroup) {
        return new SGConfig(this.energyPerNaquadah, this.dialCosts, this.distanceFactorMultiplier, interWorld,
                this.freeInCreative, this.fePerGeIn, this.fePerGeOut, this.converterRate, this.bridgeRates,
                this.toolRange, this.supergateEnergyMultiplier,
                this.addresses, this.structureGates, this.villageGates, this.powerUnitCapacity, this.naquadahOre,
                this.remoteView, this.malp, this.options.withOtherGroupMultiplier(otherGroup));
    }

    /** @return how many times more a wormhole costs again with a ninth chevron, which reaches another group */
    public double otherGroupMultiplier() {
        return this.options.otherGroupMultiplier();
    }

    /** @return these settings with other rates for FE coming in and going out, and another converter rate */
    public SGConfig withFeRates(final double fePerGeIn, final double fePerGeOut, final double converterRate) {
        return new SGConfig(this.energyPerNaquadah, this.dialCosts, this.distanceFactorMultiplier,
                this.interWorldMultiplier, this.freeInCreative, fePerGeIn, fePerGeOut, converterRate,
                this.bridgeRates, this.toolRange, this.supergateEnergyMultiplier, this.addresses,
                this.structureGates, this.villageGates, this.powerUnitCapacity, this.naquadahOre, this.remoteView,
                this.malp, this.options);
    }

    /**
     * Writes the rates for FE coming in and going out, and the converter rate, to the settings file, leaving the rest
     * of the file as it is.
     *
     * @return whether the file was written
     */
    public static boolean saveFeRates(final double fePerGeIn, final double fePerGeOut, final double converterRate) {
        return SGConfig.save(Codec.DOUBLE, fePerGeIn, "energy", "bridge_rates", BridgeRates.FE_IN)
                & SGConfig.save(Codec.DOUBLE, fePerGeOut, "energy", "bridge_rates", BridgeRates.FE_OUT)
                & SGConfig.save(Codec.DOUBLE, converterRate, "energy", "converter_rate");
    }

    /** @return these settings with other dial costs */
    public SGConfig withDialCosts(final DialCosts costs) {
        return new SGConfig(this.energyPerNaquadah, costs, this.distanceFactorMultiplier,
                this.interWorldMultiplier, this.freeInCreative, this.fePerGeIn, this.fePerGeOut, this.converterRate,
                this.bridgeRates,
                this.toolRange, this.supergateEnergyMultiplier, this.addresses, this.structureGates,
                this.villageGates, this.powerUnitCapacity, this.naquadahOre, this.remoteView, this.malp,
                this.options);
    }

    /** @return these settings with other options */
    public SGConfig withOptions(final Options options) {
        return new SGConfig(this.energyPerNaquadah, this.dialCosts, this.distanceFactorMultiplier,
                this.interWorldMultiplier, this.freeInCreative, this.fePerGeIn, this.fePerGeOut, this.converterRate,
                this.bridgeRates,
                this.toolRange, this.supergateEnergyMultiplier, this.addresses, this.structureGates,
                this.villageGates, this.powerUnitCapacity, this.naquadahOre, this.remoteView, this.malp, options);
    }

    /** @return these settings with other MALP settings */
    public SGConfig withMalp(final MalpSettings malp) {
        return new SGConfig(this.energyPerNaquadah, this.dialCosts, this.distanceFactorMultiplier,
                this.interWorldMultiplier, this.freeInCreative, this.fePerGeIn, this.fePerGeOut, this.converterRate,
                this.bridgeRates,
                this.toolRange, this.supergateEnergyMultiplier, this.addresses, this.structureGates,
                this.villageGates, this.powerUnitCapacity, this.naquadahOre, this.remoteView, malp, this.options);
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

    /** @return these settings with other shield settings */
    public SGConfig withShields(final ShieldSettings shields) {
        return this.withOptions(this.options.withShields(shields));
    }

    /** @return these settings with other ZPM settings */
    public SGConfig withZpm(final ZpmSettings zpm) {
        return this.withOptions(this.options.withZpm(zpm));
    }

    /**
     * Writes the ZPM settings to the settings file, leaving the rest of the file as it is.
     *
     * @return whether the file was written
     */
    public static boolean saveZpm(final ZpmSettings zpm) {
        return SGConfig.save(ZpmSettings.CODEC, zpm, "zpm");
    }

    /**
     * Writes the shield settings to the settings file, leaving the rest of the file as it is.
     *
     * @return whether the file was written
     */
    public static boolean saveShields(final ShieldSettings shields) {
        return SGConfig.save(ShieldSettings.CODEC, shields, "shields");
    }

    /**
     * Writes the values for gates without their own to the settings file, leaving the rest of the file as it is.
     *
     * @return whether the file was written
     */
    public static boolean saveGateDefaults(final Map<Identifier, JsonElement> defaults) {
        return SGConfig.save(SGConfig.GATE_DEFAULTS_CODEC, defaults, "gates", "defaults");
    }

    /**
     * Writes whether owners may break their gates to the settings file, leaving the rest of the file as it is.
     *
     * @return whether the file was written
     */
    public static boolean saveOwnersBreakGates(final boolean on) {
        return SGConfig.save(Codec.BOOL, on, "gates", "owners_break_gates");
    }

    /**
     * Writes whether generated gates may be broken to the settings file, leaving the rest of the file as it is.
     *
     * @return whether the file was written
     */
    public static boolean saveGeneratedGatesBreakable(final boolean on) {
        return SGConfig.save(Codec.BOOL, on, "gates", "generated_gates_breakable");
    }

    /**
     * Writes dial costs to the settings file, leaving the rest of the file as it is.
     *
     * @return whether the file was written
     */
    public static boolean saveDialCosts(final DialCosts costs) {
        return SGConfig.save(DialCosts.CODEC, costs, "energy", "dial_costs");
    }

    /**
     * Writes the multipliers for an eighth and a ninth chevron to the settings file, leaving the rest of the file as
     * it is.
     *
     * @return whether the file was written
     */
    public static boolean saveChevronMultipliers(final double interWorld, final double otherGroup) {
        return SGConfig.save(Codec.DOUBLE, interWorld, "energy", "multipliers", "inter_world")
                & SGConfig.save(Codec.DOUBLE, otherGroup, "energy", "multipliers", "other_group");
    }

    /**
     * Writes whether a kawoosh clears blocks that can't be broken to the settings file, leaving the rest of the file
     * as it is.
     *
     * @return whether the file was written
     */
    public static boolean saveKawooshBreaksUnbreakable(final boolean breaks) {
        return SGConfig.save(Codec.BOOL, breaks, "gates", "kawoosh_breaks_unbreakable");
    }

    /**
     * Writes the chunk loading range to the settings file, leaving the rest of the file as it is.
     *
     * @return whether the file was written
     */
    public static boolean saveChunkLoadingRange(final int range) {
        return SGConfig.save(Codec.INT, range, "gates", "chunk_loading_range");
    }

    /**
     * Writes whether gate events are logged to the settings file, leaving the rest of the file as it is.
     *
     * @return whether the file was written
     */
    public static boolean saveLogStargateEvents(final boolean log) {
        return SGConfig.save(Codec.BOOL, log, "gates", "log_events");
    }

    /**
     * Writes the worlds Tok'ra stand in to the settings file, leaving the rest of the file as it is.
     *
     * @return whether the file was written
     */
    public static boolean saveTokraWorlds(final Map<Identifier, Boolean> worlds) {
        return SGConfig.save(Codec.unboundedMap(Identifier.CODEC, Codec.BOOL), worlds, "worldgen", "tokra_worlds");
    }

    /**
     * Writes the naquadah ore settings to the settings file, leaving the rest of the file as it is.
     *
     * @return whether the file was written
     */
    public static boolean saveNaquadahOre(final NaquadahOreSettings settings) {
        return SGConfig.save(NaquadahOreSettings.CODEC, settings, "worldgen", "naquadah_ore");
    }

    /**
     * Writes whether damage takes players off MALP consoles to the settings file, leaving the rest of the file as it
     * is.
     *
     * @return whether the file was written
     */
    public static boolean saveConsoleDamageInterrupts(final boolean on) {
        return SGConfig.save(Codec.BOOL, on, "malp", "console_damage_interrupts");
    }

    /**
     * Writes how many players near a driven MALP console see the feed on its monitor to the settings file, leaving
     * the rest of the file as it is.
     *
     * @return whether the file was written
     */
    public static boolean saveConsoleSpectators(final int spectators) {
        return SGConfig.save(Codec.INT, spectators, "malp", "console_spectators");
    }

    /**
     * Writes how near a driven MALP console players see the feed on its monitor to the settings file, leaving the
     * rest of the file as it is.
     *
     * @return whether the file was written
     */
    public static boolean saveSpectatorRange(final int range) {
        return SGConfig.save(Codec.INT, range, "malp", "spectator_range");
    }

    /**
     * Writes how much gate energy a MALP tablet uses each tick while it shows a live feed to the settings file, leaving
     * the rest of the file as it is.
     *
     * @return whether the file was written
     */
    public static boolean saveTabletDrain(final double drain) {
        return SGConfig.save(Codec.DOUBLE, drain, "malp", "tablet_drain");
    }

    /**
     * Writes how many chunks wide and long each addressed cell is to the settings file, for every world or for one,
     * leaving the rest of the file as it is. It takes effect when the server next starts.
     *
     * @return whether the file was written
     */
    public static boolean saveCellChunks(final Optional<Identifier> world, final int chunks) {
        return SGConfig.save(Codec.INT, chunks, SGConfig.addressPath(world, "cell_chunks"));
    }

    /**
     * Writes the point of origin to the settings file, for every world or for one, leaving the rest of the file as it
     * is. It takes effect when the server next starts.
     *
     * @return whether the file was written
     */
    public static boolean savePointOfOrigin(final Optional<Identifier> world, final String origin) {
        return SGConfig.save(Codec.STRING, origin, SGConfig.addressPath(world, "point_of_origin"));
    }

    /**
     * Removes a world's own address layout from the settings file, so it takes the server's when the server next
     * starts, leaving the rest of the file as it is.
     *
     * @return whether the file was written
     */
    public static boolean clearWorldLayout(final Identifier world) {
        return SGConfig.write(null, "addresses", "worlds", world.toString());
    }

    private static String[] addressPath(final Optional<Identifier> world, final String key) {
        return world.map(id -> new String[] {"addresses", "worlds", id.toString(), key})
                .orElseGet(() -> new String[] {"addresses", key});
    }

    // Writes one setting at its place in the file's sections, leaving the rest of the file as it is
    private static <T> boolean save(final Codec<T> codec, final T value, final String... path) {
        try {
            return SGConfig.write(codec.encodeStart(JsonOps.INSTANCE, value).getOrThrow(), path);
        } catch (final RuntimeException e) {
            SGCraft.LOG.error("Couldn't write {}", String.join(".", path), e);
            return false;
        }
    }

    // Puts a value at its place in the file's sections, or removes what is there when the value is null
    private static boolean write(final JsonElement value, final String... path) {
        final Path file = Platform.get().configDir().resolve(SGConfig.FILE);
        try {
            final JsonObject json = Files.exists(file) && JsonParser.parseString(Files.readString(file))
                    instanceof JsonObject existing ? existing : new JsonObject();
            JsonObject into = json;
            for (int part = 0; part < path.length - 1; part++) {
                final JsonObject section = into.get(path[part]) instanceof JsonObject own ? own : new JsonObject();
                into.add(path[part], section);
                into = section;
            }
            if (value == null) {
                into.remove(path[path.length - 1]);
            } else {
                into.add(path[path.length - 1], value);
            }
            Files.createDirectories(file.getParent());
            Files.writeString(file, SGConfig.GSON.toJson(json));
            return true;
        } catch (final IOException | RuntimeException e) {
            SGCraft.LOG.error("Couldn't write {} to {}", String.join(".", path), file, e);
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
