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

import com.almuradev.sgcraft.mod.SGConfig;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.registry.RegistrySink;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * Naquadah ore: its feature, the settings it generates with, and adding it to chunks made without it, as the original
 * did for worlds made before SGCraft was added. Each loader calls {@link #prepare} as the server starts, before it
 * makes its worlds, {@link #onChunkLoad} as a chunk of a server's world is fully loaded, and {@link #tick} each
 * server tick.
 *
 * <p>A chunk that loads already generated in a world naquadah ore generates in is marked in {@link OreChunks} if it
 * was just generated, since the feature gave it ore then. One loaded from before is given ore on a later tick, while
 * it is still loaded, when the settings add ore to existing worlds, and is marked then. A chunk is only changed
 * within itself, so no other chunk is loaded for it.</p>
 */
public final class NaquadahOre {

    public static final ResourceKey<MapCodec<? extends Feature>> FEATURE_TYPE =
            ResourceKey.create(Registries.FEATURE_TYPE, SGCraft.id("naquadah_ore"));
    /** The feature as defined by SGCraft's data, which old chunks are given ore with too. */
    public static final ResourceKey<Feature> FEATURE =
            ResourceKey.create(Registries.FEATURE, SGCraft.id("naquadah_ore"));

    // How many old chunks get ore each tick, so loading many at once doesn't hold the server up
    private static final int CHUNKS_PER_TICK = 32;
    // Where among the features the ore given to old chunks takes its random numbers from
    private static final int FEATURE_SALT = 0x5347;

    private static volatile NaquadahOreSettings settings = NaquadahOreSettings.DEFAULTS;
    // Old chunks waiting for ore, by world; used on the server thread only
    private static final Map<ResourceKey<Level>, LongLinkedOpenHashSet> PENDING = new HashMap<>();

    private NaquadahOre() {
    }

    /** Registers the feature's type. */
    public static void register(final RegistrySink<MapCodec<? extends Feature>> sink) {
        sink.register(NaquadahOre.FEATURE_TYPE, NaquadahOreFeature.CODEC);
    }

    /** Takes up the server's settings. Runs as the server starts, before it generates any of its worlds. */
    public static void prepare(final MinecraftServer server) {
        NaquadahOre.settings = SGConfig.load().naquadahOre();
        NaquadahOre.PENDING.clear();
    }

    /** @return the settings naquadah ore generates with */
    public static NaquadahOreSettings settings() {
        return NaquadahOre.settings;
    }

    /** Generates naquadah ore with other settings until the server stops, without writing them. */
    public static void use(final NaquadahOreSettings settings) {
        NaquadahOre.settings = settings;
    }

    /**
     * Generates naquadah ore with other settings from now on, and writes them to the settings file.
     *
     * @return whether the file was written
     */
    public static boolean save(final NaquadahOreSettings settings) {
        NaquadahOre.use(settings);
        return SGConfig.saveNaquadahOre(settings);
    }

    /**
     * Marks a chunk that was just generated, or has an old chunk given ore on a later tick if the settings say to.
     *
     * @param generated whether the chunk was just generated, rather than loaded as it was saved
     */
    public static void onChunkLoad(final ServerLevel level, final LevelChunk chunk, final boolean generated) {
        final NaquadahOreSettings current = NaquadahOre.settings;
        if (!current.generatesIn(level.dimension())) {
            return;
        }
        final OreChunks chunks = OreChunks.of(level);
        if (chunks.marked(chunk.getPos())) {
            return;
        }
        if (generated) {
            chunks.mark(chunk.getPos());
        } else if (current.addOresToExistingWorlds()) {
            NaquadahOre.PENDING.computeIfAbsent(level.dimension(), key -> new LongLinkedOpenHashSet())
                    .add(chunk.getPos().pack());
        }
    }

    /** Gives ore to some of the old chunks waiting for it that are still loaded. */
    public static void tick(final MinecraftServer server) {
        int budget = NaquadahOre.CHUNKS_PER_TICK;
        final Iterator<Map.Entry<ResourceKey<Level>, LongLinkedOpenHashSet>> worlds =
                NaquadahOre.PENDING.entrySet().iterator();
        while (worlds.hasNext() && budget > 0) {
            final Map.Entry<ResourceKey<Level>, LongLinkedOpenHashSet> world = worlds.next();
            final ServerLevel level = server.getLevel(world.getKey());
            final NaquadahOreSettings current = NaquadahOre.settings;
            if (level == null || !current.generatesIn(world.getKey()) || !current.addOresToExistingWorlds()) {
                worlds.remove();
                continue;
            }
            final LongIterator chunks = world.getValue().iterator();
            while (chunks.hasNext() && budget > 0) {
                final ChunkPos pos = ChunkPos.unpack(chunks.nextLong());
                chunks.remove();
                // A chunk that unloaded meanwhile waits until it loads again
                final LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x(), pos.z());
                if (chunk != null && !OreChunks.of(level).marked(pos)) {
                    NaquadahOre.generate(level, chunk, NaquadahOre.random(level, pos));
                    budget--;
                }
            }
            if (world.getValue().isEmpty()) {
                worlds.remove();
            }
        }
    }

    /** @return whether an old chunk is waiting for ore */
    public static boolean pending(final ServerLevel level, final ChunkPos chunk) {
        final LongLinkedOpenHashSet chunks = NaquadahOre.PENDING.get(level.dimension());
        return chunks != null && chunks.contains(chunk.pack());
    }

    /** @return how many old chunks are waiting for ore, in every world */
    public static int pending() {
        return NaquadahOre.PENDING.values().stream().mapToInt(LongLinkedOpenHashSet::size).sum();
    }

    /**
     * Gives a loaded chunk naquadah ore now with SGCraft's feature, as it would have had if it were generated with
     * it, and marks it, whether or not it was marked before.
     *
     * @return how many blocks were turned to ore, or empty if the feature's data defines another kind of feature
     */
    public static Optional<Integer> generate(final ServerLevel level, final LevelChunk chunk,
            final RandomSource random) {
        final Optional<Feature> feature = level.registryAccess().lookupOrThrow(Registries.FEATURE)
                .getOptional(NaquadahOre.FEATURE);
        OreChunks.of(level).mark(chunk.getPos());
        if (feature.isEmpty() || !(feature.get() instanceof NaquadahOreFeature ore)) {
            return Optional.empty();
        }
        return Optional.of(ore.generate(level, chunk.getPos(), random, NaquadahOre.settings, level.getMinY(),
                level.getChunkSource().getGenerator().getSeaLevel()));
    }

    /** @return random numbers for giving a chunk ore, the same each time for the same world and chunk */
    public static RandomSource random(final ServerLevel level, final ChunkPos chunk) {
        final WorldgenRandom random = new WorldgenRandom(new XoroshiroRandomSource(0));
        final long seed = random.setDecorationSeed(level.getSeed(), chunk.getMinBlockX(), chunk.getMinBlockZ());
        random.setFeatureSeed(seed, NaquadahOre.FEATURE_SALT, GenerationStep.Decoration.UNDERGROUND_ORES.ordinal());
        return random;
    }
}
