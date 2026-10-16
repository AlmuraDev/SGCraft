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

package com.almuradev.sgcraft.mod.address;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Optional;

/**
 * The server's address layout settings, kept under {@code addresses}: the cell size and point of origin every world
 * has, and any world's own.
 *
 * @param cellChunks how many chunks wide and long each addressed cell is in worlds without their own
 * @param pointOfOrigin the symbol addresses end with in worlds without their own, or {@code ""} for none
 * @param worlds single worlds' own values, by the world's identifier
 */
public record AddressSettings(int cellChunks, String pointOfOrigin, Map<Identifier, WorldLayout> worlds) {

    public static final AddressSettings DEFAULTS = new AddressSettings(AddressLayout.DEFAULTS.cellChunks(),
            AddressLayout.DEFAULTS.pointOfOrigin(), Map.of());

    public static final Codec<AddressSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AddressLayout.CELL_CHUNKS_CODEC.optionalFieldOf("cell_chunks", AddressSettings.DEFAULTS.cellChunks)
                    .forGetter(AddressSettings::cellChunks),
            AddressLayout.ORIGIN_CODEC.optionalFieldOf("point_of_origin", AddressSettings.DEFAULTS.pointOfOrigin)
                    .forGetter(AddressSettings::pointOfOrigin),
            Codec.unboundedMap(Identifier.CODEC, WorldLayout.CODEC).optionalFieldOf("worlds", Map.of())
                    .forGetter(AddressSettings::worlds)
    ).apply(instance, AddressSettings::new));

    public AddressSettings {
        worlds = Map.copyOf(worlds);
    }

    /** @return the layout a world has: its own values where it has them, and the server's elsewhere */
    public AddressLayout layoutOf(final ResourceKey<Level> world) {
        final Optional<WorldLayout> own = Optional.ofNullable(this.worlds.get(world.identifier()));
        return new AddressLayout(own.flatMap(WorldLayout::cellChunks).orElse(this.cellChunks),
                own.flatMap(WorldLayout::pointOfOrigin).orElse(this.pointOfOrigin));
    }

    /**
     * One world's own address layout values; a value left out is the server's.
     *
     * @param cellChunks how many chunks wide and long each addressed cell in the world is
     * @param pointOfOrigin the symbol addresses dialed from the world end with, or {@code ""} for none
     */
    public record WorldLayout(Optional<Integer> cellChunks, Optional<String> pointOfOrigin) {

        public static final Codec<WorldLayout> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                AddressLayout.CELL_CHUNKS_CODEC.optionalFieldOf("cell_chunks").forGetter(WorldLayout::cellChunks),
                AddressLayout.ORIGIN_CODEC.optionalFieldOf("point_of_origin").forGetter(WorldLayout::pointOfOrigin)
        ).apply(instance, WorldLayout::new));
    }
}
