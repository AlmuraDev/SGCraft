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

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Naquadah ore as the original SGCraft generated it, one chunk at a time, with the counts and odds of
 * {@link NaquadahOreSettings}. Columns of the chunk are searched from the bottom of the world to sea level for lava
 * lying on a block it may replace, and each such block starts a cluster up to three blocks across and one high. Away
 * from lava, nodes up to two blocks each way start 16 to 31 blocks above the bottom of the world. Nothing is placed
 * outside the chunk.
 *
 * <p>The ore and the blocks it may replace come from the feature's definition, so a data pack can change them; it
 * runs in the chunk holding its origin, so it needs no placement of its own.</p>
 *
 * @param state the ore placed
 * @param replaceable the blocks the ore may replace, which are also the blocks lava must lie on
 */
public record NaquadahOreFeature(BlockState state, BlockPredicate replaceable) implements Feature {

    public static final MapCodec<NaquadahOreFeature> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockState.CODEC.fieldOf("state").forGetter(NaquadahOreFeature::state),
            BlockPredicate.CODEC.fieldOf("replaceable").forGetter(NaquadahOreFeature::replaceable)
    ).apply(instance, NaquadahOreFeature::new));

    // Isolated nodes start this far above the bottom of the world, and within this many blocks more
    private static final int ISOLATED_ABOVE_BOTTOM = 16;
    private static final int ISOLATED_SPREAD = 16;
    // Neither neighbors nor fluids are updated, so nothing outside the chunk is looked at
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    @Override
    public MapCodec<NaquadahOreFeature> codec() {
        return NaquadahOreFeature.CODEC;
    }

    @Override
    public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random,
            final BlockPos origin) {
        final NaquadahOreSettings settings = NaquadahOre.settings();
        if (!settings.generatesIn(level.getLevel().dimension())) {
            return false;
        }
        this.generate(level, ChunkPos.containing(origin), random, settings, level.getMinY(),
                chunkGenerator.getSeaLevel());
        return true;
    }

    /**
     * Generates the ore in one chunk.
     *
     * @param bottom the bottom of the world, which lava is searched above and isolated nodes are placed from
     * @param seaLevel the highest block lava is searched on
     * @return how many blocks were turned to ore
     */
    public int generate(final WorldGenLevel level, final ChunkPos chunk, final RandomSource random,
            final NaquadahOreSettings settings, final int bottom, final int seaLevel) {
        int placed = 0;
        if (settings.maxNodesUnderLava() > 0 && NaquadahOreFeature.odds(random, settings.genUnderLavaOdds())) {
            final int columns = random.nextInt(settings.maxNodesUnderLava()) + 1;
            final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            final BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
            for (int i = 0; i < columns; i++) {
                final int x = random.nextInt(16);
                final int z = random.nextInt(16);
                for (int y = bottom + 1; y <= seaLevel; y++) {
                    pos.set(chunk.getBlockX(x), y, chunk.getBlockZ(z));
                    above.setWithOffset(pos, 0, 1, 0);
                    if (level.getFluidState(above).is(FluidTags.LAVA) && this.replaceable.test(level, pos)) {
                        placed += this.node(level, chunk, random, x, y, z, 3, 1, 3);
                    }
                }
            }
        }
        if (settings.maxIsolatedNodes() > 0 && NaquadahOreFeature.odds(random, settings.genIsolatedOdds())) {
            final int nodes = random.nextInt(settings.maxIsolatedNodes()) + 1;
            for (int i = 0; i < nodes; i++) {
                final int x = random.nextInt(16);
                final int y = bottom + NaquadahOreFeature.ISOLATED_ABOVE_BOTTOM
                        + random.nextInt(NaquadahOreFeature.ISOLATED_SPREAD);
                final int z = random.nextInt(16);
                if (this.replaceable.test(level, chunk.getBlockAt(x, y, z))) {
                    placed += this.node(level, chunk, random, x, y, z, 2, 2, 2);
                }
            }
        }
        return placed;
    }

    // One chance in n, or none when n is 0
    private static boolean odds(final RandomSource random, final int n) {
        return n > 0 && random.nextInt(n) == 0;
    }

    // A box of ore from a corner, up to the given size each way, cut off at the chunk's edge and the world's top
    private int node(final WorldGenLevel level, final ChunkPos chunk, final RandomSource random, final int x,
            final int y, final int z, final int sizeX, final int sizeY, final int sizeZ) {
        final int dx = random.nextInt(sizeX);
        final int dy = random.nextInt(sizeY);
        final int dz = random.nextInt(sizeZ);
        int placed = 0;
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = x; i <= x + dx && i < 16; i++) {
            for (int j = y; j <= y + dy && j <= level.getMaxY(); j++) {
                for (int k = z; k <= z + dz && k < 16; k++) {
                    pos.set(chunk.getBlockX(i), j, chunk.getBlockZ(k));
                    if (!level.getBlockState(pos).equals(this.state) && this.replaceable.test(level, pos)) {
                        level.setBlock(pos, this.state, NaquadahOreFeature.FLAGS);
                        placed++;
                    }
                }
            }
        }
        return placed;
    }
}
