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

package com.almuradev.sgcraft.mod.view;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.level.lighting.LevelLightEngine;

import java.util.Arrays;

/**
 * Encodes a section of blocks for a remote view, and reads it back. A section is encoded as a chunk packet encodes
 * one, its block states and biomes each in a palette, followed by its block light and its sky light. Each light layer
 * is either one value for the whole section or all 4096 values, four bits each.
 */
public final class RemoteViewSections {

    private static final int UNIFORM = 0;
    private static final int FULL = 1;

    private RemoteViewSections() {
    }

    /** A section read back: its blocks and biomes, and its two layers of light. */
    public record Decoded(LevelChunkSection blocks, DataLayer blockLight, DataLayer skyLight) {
    }

    /**
     * Encodes a section of a loaded chunk.
     *
     * @param hasSkyLight whether the chunk's world has sky light; one without is sent as having none
     */
    public static byte[] encode(final LevelChunk chunk, final int sectionY, final LevelLightEngine light,
            final boolean hasSkyLight) {
        final LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(sectionY));
        final SectionPos pos = SectionPos.of(chunk.getPos(), sectionY);
        final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(section.getSerializedSize() + 64));
        section.write(buf);
        RemoteViewSections.writeLight(buf, light.getLayerListener(LightLayer.BLOCK), pos, false);
        if (hasSkyLight) {
            RemoteViewSections.writeLight(buf, light.getLayerListener(LightLayer.SKY), pos, true);
        } else {
            buf.writeByte(RemoteViewSections.UNIFORM);
            buf.writeByte(0);
        }
        final byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return bytes;
    }

    /** Reads back a section {@link #encode} wrote, with the registries of the side reading it. */
    public static Decoded decode(final byte[] data, final RegistryAccess registries) {
        final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        final LevelChunkSection blocks = new LevelChunkSection(PalettedContainerFactory.create(registries));
        blocks.read(buf);
        final DataLayer blockLight = RemoteViewSections.readLight(buf);
        final DataLayer skyLight = RemoteViewSections.readLight(buf);
        return new Decoded(blocks, blockLight, skyLight);
    }

    /** @return a hash of an encoded section, to tell cheaply whether it has changed since it was sent */
    public static long hash(final byte[] data) {
        return ((long) Arrays.hashCode(data) << 32) ^ data.length;
    }

    // A section the light engine keeps no layer for has, for block light, none at all; for sky light, what the
    // engine works out from above, which is read block by block
    private static void writeLight(final FriendlyByteBuf buf, final LayerLightEventListener listener,
            final SectionPos pos, final boolean sky) {
        DataLayer layer = listener.getDataLayerData(pos);
        if (layer == null) {
            if (!sky) {
                buf.writeByte(RemoteViewSections.UNIFORM);
                buf.writeByte(0);
                return;
            }
            layer = RemoteViewSections.sample(listener, pos);
        }
        final byte[] bytes = layer.getData();
        final byte first = bytes[0];
        boolean uniform = (first & 15) == (first >> 4 & 15);
        for (int i = 1; uniform && i < bytes.length; i++) {
            uniform = bytes[i] == first;
        }
        if (uniform) {
            buf.writeByte(RemoteViewSections.UNIFORM);
            buf.writeByte(first & 15);
        } else {
            buf.writeByte(RemoteViewSections.FULL);
            buf.writeBytes(bytes);
        }
    }

    private static DataLayer sample(final LayerLightEventListener listener, final SectionPos pos) {
        final DataLayer layer = new DataLayer();
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    at.set(pos.minBlockX() + x, pos.minBlockY() + y, pos.minBlockZ() + z);
                    layer.set(x, y, z, listener.getLightValue(at));
                }
            }
        }
        return layer;
    }

    private static DataLayer readLight(final FriendlyByteBuf buf) {
        final int mode = buf.readByte();
        if (mode == RemoteViewSections.UNIFORM) {
            return new DataLayer(buf.readByte());
        }
        final byte[] bytes = new byte[DataLayer.SIZE];
        buf.readBytes(bytes);
        return new DataLayer(bytes);
    }
}
