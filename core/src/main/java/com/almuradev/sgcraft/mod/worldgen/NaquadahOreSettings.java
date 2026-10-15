/*
 * This file is part of SGCraft, licensed under the MIT License (MIT).
 * It is based on the original SGCraft by gcewing.
 *
 * Copyright (c) AlmuraDev <https://github.com/AlmuraDev>
 * Copyright (c) 2016 gcewing <https://github.com/gcewing>
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

package com.almuradev.sgcraft.mod.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

/**
 * How naquadah ore generates, with the original SGCraft's options and defaults. Each chunk of a listed world can get
 * clusters on the floor of lava below sea level and small nodes deep down.
 *
 * @param enableNaquadahOre whether naquadah ore generates at all
 * @param genUnderLavaOdds one chunk in this many gets clusters under lava; 0 gives none
 * @param maxNodesUnderLava the most columns of a chunk searched for lava, each giving a cluster wherever lava lies on
 *     a solid block
 * @param genIsolatedOdds one chunk in this many gets nodes away from lava; 0 gives none
 * @param maxIsolatedNodes the most nodes such a chunk gets
 * @param dimensionIds the worlds naquadah ore generates in
 * @param addOresToExistingWorlds whether chunks made without naquadah ore, such as before SGCraft was added, get it
 *     when they load
 */
public record NaquadahOreSettings(boolean enableNaquadahOre, int genUnderLavaOdds, int maxNodesUnderLava,
        int genIsolatedOdds, int maxIsolatedNodes, List<ResourceKey<Level>> dimensionIds,
        boolean addOresToExistingWorlds) {

    public static final NaquadahOreSettings DEFAULTS = new NaquadahOreSettings(true, 1, 16, 1, 4,
            List.of(Level.OVERWORLD), false);

    public static final Codec<NaquadahOreSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enable_naquadah_ore", NaquadahOreSettings.DEFAULTS.enableNaquadahOre)
                    .forGetter(NaquadahOreSettings::enableNaquadahOre),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("gen_under_lava_odds",
                    NaquadahOreSettings.DEFAULTS.genUnderLavaOdds).forGetter(NaquadahOreSettings::genUnderLavaOdds),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("max_nodes_under_lava",
                    NaquadahOreSettings.DEFAULTS.maxNodesUnderLava).forGetter(NaquadahOreSettings::maxNodesUnderLava),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("gen_isolated_odds",
                    NaquadahOreSettings.DEFAULTS.genIsolatedOdds).forGetter(NaquadahOreSettings::genIsolatedOdds),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("max_isolated_nodes",
                    NaquadahOreSettings.DEFAULTS.maxIsolatedNodes).forGetter(NaquadahOreSettings::maxIsolatedNodes),
            ResourceKey.codec(Registries.DIMENSION).listOf().optionalFieldOf("dimension_ids",
                    NaquadahOreSettings.DEFAULTS.dimensionIds).forGetter(NaquadahOreSettings::dimensionIds),
            Codec.BOOL.optionalFieldOf("add_ores_to_existing_worlds",
                    NaquadahOreSettings.DEFAULTS.addOresToExistingWorlds)
                    .forGetter(NaquadahOreSettings::addOresToExistingWorlds)
    ).apply(instance, NaquadahOreSettings::new));

    public NaquadahOreSettings {
        dimensionIds = List.copyOf(dimensionIds);
    }

    /** @return whether naquadah ore generates in a world */
    public boolean generatesIn(final ResourceKey<Level> world) {
        return this.enableNaquadahOre && this.dimensionIds.contains(world);
    }

    public NaquadahOreSettings withEnableNaquadahOre(final boolean enable) {
        return new NaquadahOreSettings(enable, this.genUnderLavaOdds, this.maxNodesUnderLava, this.genIsolatedOdds,
                this.maxIsolatedNodes, this.dimensionIds, this.addOresToExistingWorlds);
    }

    public NaquadahOreSettings withAddOresToExistingWorlds(final boolean add) {
        return new NaquadahOreSettings(this.enableNaquadahOre, this.genUnderLavaOdds, this.maxNodesUnderLava,
                this.genIsolatedOdds, this.maxIsolatedNodes, this.dimensionIds, add);
    }

    /** @return these settings with a world added to or taken from the worlds naquadah ore generates in */
    public NaquadahOreSettings withDimension(final ResourceKey<Level> world, final boolean listed) {
        final List<ResourceKey<Level>> worlds = new ArrayList<>(this.dimensionIds);
        worlds.remove(world);
        if (listed) {
            worlds.add(world);
        }
        return new NaquadahOreSettings(this.enableNaquadahOre, this.genUnderLavaOdds, this.maxNodesUnderLava,
                this.genIsolatedOdds, this.maxIsolatedNodes, worlds, this.addOresToExistingWorlds);
    }

    /** The settings that are counts, by their names in the settings file. */
    public enum Count {
        GEN_UNDER_LAVA_ODDS("gen_under_lava_odds", NaquadahOreSettings::genUnderLavaOdds,
                (settings, value) -> new NaquadahOreSettings(settings.enableNaquadahOre, value,
                        settings.maxNodesUnderLava, settings.genIsolatedOdds, settings.maxIsolatedNodes,
                        settings.dimensionIds, settings.addOresToExistingWorlds)),
        MAX_NODES_UNDER_LAVA("max_nodes_under_lava", NaquadahOreSettings::maxNodesUnderLava,
                (settings, value) -> new NaquadahOreSettings(settings.enableNaquadahOre, settings.genUnderLavaOdds,
                        value, settings.genIsolatedOdds, settings.maxIsolatedNodes, settings.dimensionIds,
                        settings.addOresToExistingWorlds)),
        GEN_ISOLATED_ODDS("gen_isolated_odds", NaquadahOreSettings::genIsolatedOdds,
                (settings, value) -> new NaquadahOreSettings(settings.enableNaquadahOre, settings.genUnderLavaOdds,
                        settings.maxNodesUnderLava, value, settings.maxIsolatedNodes, settings.dimensionIds,
                        settings.addOresToExistingWorlds)),
        MAX_ISOLATED_NODES("max_isolated_nodes", NaquadahOreSettings::maxIsolatedNodes,
                (settings, value) -> new NaquadahOreSettings(settings.enableNaquadahOre, settings.genUnderLavaOdds,
                        settings.maxNodesUnderLava, settings.genIsolatedOdds, value, settings.dimensionIds,
                        settings.addOresToExistingWorlds));

        private final String id;
        private final ToIntFunction<NaquadahOreSettings> getter;
        private final BiFunction<NaquadahOreSettings, Integer, NaquadahOreSettings> setter;

        Count(final String id, final ToIntFunction<NaquadahOreSettings> getter,
                final BiFunction<NaquadahOreSettings, Integer, NaquadahOreSettings> setter) {
            this.id = id;
            this.getter = getter;
            this.setter = setter;
        }

        public String id() {
            return this.id;
        }

        public int get(final NaquadahOreSettings settings) {
            return this.getter.applyAsInt(settings);
        }

        public NaquadahOreSettings with(final NaquadahOreSettings settings, final int value) {
            return this.setter.apply(settings, value);
        }
    }
}
