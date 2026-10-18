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

package com.almuradev.sgcraft.mod.client.view;

import com.almuradev.sgcraft.mod.view.RemoteViewPackets;
import com.almuradev.sgcraft.mod.view.RemoteViewSections;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The client's copy of the world around a remote view's source, as the server has streamed it: blocks, fluids,
 * light, biomes and block entities, for the block and fluid renderers to read, and where rain stops in each column,
 * for the weather. A place the server hasn't sent is air, open to the sky. The source's lamp, while it is on, adds
 * block light in front of the camera.
 */
final class RemoteViewLevel implements BlockAndTintGetter {

    /** How far the lamp reaches, in blocks. */
    static final double LAMP_REACH = 12;
    // The lamp lights a cone this wide, by the cosine of its half angle, and all round the camera close up
    private static final double LAMP_CONE = 0.55;
    private static final double LAMP_NEAR = 2.5;

    private final Long2ObjectMap<RemoteViewSections.Decoded> sections = new Long2ObjectOpenHashMap<>();
    // Where rain stops in each column of each chunk the server has sent them for, by x + z * 16 within the chunk
    private final Long2ObjectMap<short[]> heights = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<BlockEntity> blockEntities;
    private final boolean hasSkyLight;
    private final CardinalLighting cardinalLighting;
    private final int minY;
    private final int height;
    private final int seaLevel;
    private boolean lamp;
    private Vec3 lampPos = Vec3.ZERO;
    private Vec3 lampDirection = new Vec3(0, 0, 1);

    RemoteViewLevel(final RemoteViewPackets.Begin begin, final Long2ObjectMap<BlockEntity> blockEntities) {
        this.blockEntities = blockEntities;
        this.hasSkyLight = begin.hasSkyLight();
        final CardinalLighting.Type[] types = CardinalLighting.Type.values();
        this.cardinalLighting = types[Mth.clamp(begin.cardinalLighting(), 0, types.length - 1)].get();
        this.minY = begin.minY();
        this.height = begin.height();
        this.seaLevel = begin.seaLevel();
    }

    void put(final long pos, final RemoteViewSections.Decoded section) {
        this.sections.put(pos, section);
    }

    void remove(final long pos) {
        this.sections.remove(pos);
    }

    boolean has(final long pos) {
        return this.sections.containsKey(pos);
    }

    void putHeights(final long chunk, final short[] heights) {
        this.heights.put(chunk, heights);
    }

    void removeHeights(final long chunk) {
        this.heights.remove(chunk);
    }

    /**
     * @return where rain stops falling in a block column: one above the highest block that blocks movement or holds
     *     a fluid; or {@link Integer#MIN_VALUE} if the server hasn't said
     */
    int rainHeight(final int x, final int z) {
        final short[] chunk = this.heights.get(ChunkPos.pack(SectionPos.blockToSectionCoord(x),
                SectionPos.blockToSectionCoord(z)));
        return chunk == null ? Integer.MIN_VALUE : chunk[(x & 15) + (z & 15) * 16];
    }

    /** @return the biome at a block, if the server has sent its section */
    @Nullable Holder<Biome> biome(final BlockPos pos) {
        final RemoteViewSections.Decoded section = this.section(pos.getX(), pos.getY(), pos.getZ());
        return section == null ? null
                : section.blocks().getNoiseBiome(pos.getX() >> 2 & 3, pos.getY() >> 2 & 3, pos.getZ() >> 2 & 3);
    }

    /** @return the world's sea level, above which a biome grows colder */
    int seaLevel() {
        return this.seaLevel;
    }

    /** Sets where the lamp is and which way it points; a lamp that is off lights nothing. */
    void setLamp(final boolean on, final Vec3 pos, final Vec3 direction) {
        this.lamp = on;
        this.lampPos = pos;
        this.lampDirection = direction;
    }

    private RemoteViewSections.@Nullable Decoded section(final int x, final int y, final int z) {
        return this.sections.get(SectionPos.asLong(SectionPos.blockToSectionCoord(x),
                SectionPos.blockToSectionCoord(y), SectionPos.blockToSectionCoord(z)));
    }

    @Override
    public BlockState getBlockState(final BlockPos pos) {
        final RemoteViewSections.Decoded section = this.section(pos.getX(), pos.getY(), pos.getZ());
        return section == null ? Blocks.AIR.defaultBlockState()
                : section.blocks().getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
    }

    @Override
    public FluidState getFluidState(final BlockPos pos) {
        return this.getBlockState(pos).getFluidState();
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(final BlockPos pos) {
        return this.blockEntities.get(pos.asLong());
    }

    @Override
    public int getBrightness(final LightLayer layer, final BlockPos pos) {
        final RemoteViewSections.Decoded section = this.section(pos.getX(), pos.getY(), pos.getZ());
        if (layer == LightLayer.SKY) {
            if (!this.hasSkyLight) {
                return 0;
            }
            return section == null ? 15 : section.skyLight().get(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
        }
        final int stored = section == null ? 0
                : section.blockLight().get(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
        return Math.max(stored, this.lampLight(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
    }

    @Override
    public int getRawBrightness(final BlockPos pos, final int darkening) {
        return Math.max(this.getBrightness(LightLayer.SKY, pos) - darkening, this.getBrightness(LightLayer.BLOCK, pos));
    }

    /** @return the lamp's light at a point, from 0 to 14 */
    int lampLight(final double x, final double y, final double z) {
        if (!this.lamp) {
            return 0;
        }
        final double dx = x - this.lampPos.x;
        final double dy = y - this.lampPos.y;
        final double dz = z - this.lampPos.z;
        final double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance >= RemoteViewLevel.LAMP_REACH) {
            return 0;
        }
        double strength = 1 - distance / RemoteViewLevel.LAMP_REACH;
        if (distance > RemoteViewLevel.LAMP_NEAR) {
            final double cosine = (dx * this.lampDirection.x + dy * this.lampDirection.y
                    + dz * this.lampDirection.z) / distance;
            if (cosine < RemoteViewLevel.LAMP_CONE) {
                return 0;
            }
            // Soft at the edge of the beam
            strength *= Math.min(1, (cosine - RemoteViewLevel.LAMP_CONE) / 0.15);
        }
        return Mth.clamp((int) Math.round(14 * strength), 0, 14);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        // Light is read from the streamed layers by getBrightness, never from an engine
        return LevelLightEngine.EMPTY;
    }

    @Override
    public CardinalLighting cardinalLighting() {
        return this.cardinalLighting;
    }

    // Biome colors blended over the biomes about a block, as the client's own world blends them
    @Override
    public int getBlockTint(final BlockPos pos, final ColorResolver resolver) {
        int red = 0;
        int green = 0;
        int blue = 0;
        int count = 0;
        for (int dx = -4; dx <= 4; dx += 4) {
            for (int dz = -4; dz <= 4; dz += 4) {
                final int x = pos.getX() + dx;
                final int z = pos.getZ() + dz;
                final RemoteViewSections.Decoded section = this.section(x, pos.getY(), z);
                if (section == null) {
                    continue;
                }
                final Holder<Biome> biome = section.blocks().getNoiseBiome(x >> 2 & 3, pos.getY() >> 2 & 3,
                        z >> 2 & 3);
                final int color = resolver.getColor(biome.value(), x, z);
                red += color >> 16 & 255;
                green += color >> 8 & 255;
                blue += color & 255;
                count++;
            }
        }
        if (count == 0) {
            return -1;
        }
        return 0xFF000000 | red / count << 16 | green / count << 8 | blue / count;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public int getMinY() {
        return this.minY;
    }
}
