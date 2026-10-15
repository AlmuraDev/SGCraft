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

import com.almuradev.sgcraft.mod.SGCraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.LongStream;

/**
 * The chunks of one world that have had their naquadah ore, so none gets it twice, as the original kept a flag with
 * each chunk. Kept with the world's data, a bit for each chunk in regions of 32 by 32 chunks. Used on the server
 * thread only.
 */
public final class OreChunks extends SavedData {

    private static final int REGION = ChunkPos.REGION_SIZE;
    private static final int WORDS = OreChunks.REGION * OreChunks.REGION / Long.SIZE;

    private record Region(int x, int z, long[] chunks) {

        static final Codec<Region> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("x").forGetter(Region::x),
                Codec.INT.fieldOf("z").forGetter(Region::z),
                Codec.LONG_STREAM.xmap(LongStream::toArray, Arrays::stream).fieldOf("chunks")
                        .forGetter(Region::chunks)
        ).apply(instance, Region::new));
    }

    private static final Codec<OreChunks> CODEC = Region.CODEC.listOf().optionalFieldOf("regions", List.of())
            .xmap(OreChunks::new, OreChunks::regions).codec();

    public static final SavedDataType<OreChunks> TYPE =
            new SavedDataType<>(SGCraft.id("naquadah_ore_chunks"), OreChunks::new, OreChunks.CODEC, null);

    private final Long2ObjectMap<long[]> regions = new Long2ObjectOpenHashMap<>();

    private OreChunks() {
    }

    private OreChunks(final List<Region> regions) {
        for (final Region region : regions) {
            if (region.chunks().length == OreChunks.WORDS) {
                this.regions.put(ChunkPos.pack(region.x(), region.z()), region.chunks().clone());
            }
        }
    }

    /** @return the chunks of a world that have had their naquadah ore */
    public static OreChunks of(final ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(OreChunks.TYPE);
    }

    /** @return whether a chunk has had its naquadah ore */
    public boolean marked(final ChunkPos chunk) {
        final long[] bits = this.regions.get(ChunkPos.pack(chunk.getRegionX(), chunk.getRegionZ()));
        final int index = OreChunks.index(chunk);
        return bits != null && (bits[index / Long.SIZE] & (1L << (index % Long.SIZE))) != 0;
    }

    /** @return whether the chunk wasn't marked before */
    public boolean mark(final ChunkPos chunk) {
        final long[] bits = this.regions.computeIfAbsent(ChunkPos.pack(chunk.getRegionX(), chunk.getRegionZ()),
                key -> new long[OreChunks.WORDS]);
        final int index = OreChunks.index(chunk);
        final long bit = 1L << (index % Long.SIZE);
        if ((bits[index / Long.SIZE] & bit) != 0) {
            return false;
        }
        bits[index / Long.SIZE] |= bit;
        this.setDirty();
        return true;
    }

    /** @return whether the chunk was marked before */
    public boolean unmark(final ChunkPos chunk) {
        final long key = ChunkPos.pack(chunk.getRegionX(), chunk.getRegionZ());
        final long[] bits = this.regions.get(key);
        final int index = OreChunks.index(chunk);
        final long bit = 1L << (index % Long.SIZE);
        if (bits == null || (bits[index / Long.SIZE] & bit) == 0) {
            return false;
        }
        bits[index / Long.SIZE] &= ~bit;
        if (Arrays.stream(bits).allMatch(word -> word == 0)) {
            this.regions.remove(key);
        }
        this.setDirty();
        return true;
    }

    private static int index(final ChunkPos chunk) {
        return chunk.getRegionLocalZ() * OreChunks.REGION + chunk.getRegionLocalX();
    }

    private List<Region> regions() {
        final List<Region> regions = new ArrayList<>(this.regions.size());
        for (final Long2ObjectMap.Entry<long[]> entry : this.regions.long2ObjectEntrySet()) {
            regions.add(new Region(ChunkPos.getX(entry.getLongKey()), ChunkPos.getZ(entry.getLongKey()),
                    entry.getValue().clone()));
        }
        return regions;
    }
}
