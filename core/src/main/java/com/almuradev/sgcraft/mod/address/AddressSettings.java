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
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The server's address settings, kept under {@code addresses}: the cell size and point of origin every world has,
 * the groups worlds are sorted into, and any world's own values.
 *
 * @param cellChunks how many chunks wide and long each addressed cell is in worlds without their own
 * @param pointOfOrigin the symbol addresses end with in worlds without their own, or {@code ""} for none
 * @param groups the groups of worlds, by name; {@value #DEFAULT_GROUP} is the group of every world not put in another,
 *     and is there whether it is listed or not
 * @param worlds single worlds' own values, by the world's identifier
 */
public record AddressSettings(int cellChunks, String pointOfOrigin, Map<String, GroupSettings> groups,
        Map<Identifier, WorldLayout> worlds) {

    /** The name of the group every world is in unless the settings put it in another. */
    public static final String DEFAULT_GROUP = "default";

    /** The longest a group's name can be. */
    public static final int MAX_GROUP_NAME = 32;

    private static final Pattern GROUP_NAME = Pattern.compile("[a-z0-9_]{1," + AddressSettings.MAX_GROUP_NAME + "}");

    public static final AddressSettings DEFAULTS = new AddressSettings(AddressLayout.DEFAULTS.cellChunks(),
            AddressLayout.DEFAULTS.pointOfOrigin(), Map.of(), Map.of());

    /** Reads a group's name: lowercase letters, digits and underscores, up to {@value #MAX_GROUP_NAME} of them. */
    public static final Codec<String> GROUP_NAME_CODEC = Codec.STRING.validate(name -> AddressSettings.isGroupName(name)
            ? DataResult.success(name)
            : DataResult.error(() -> "A group's name is 1 to " + AddressSettings.MAX_GROUP_NAME
                    + " lowercase letters, digits and underscores: " + name));

    public static final Codec<AddressSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AddressLayout.CELL_CHUNKS_CODEC.optionalFieldOf("cell_chunks", AddressSettings.DEFAULTS.cellChunks)
                    .forGetter(AddressSettings::cellChunks),
            AddressLayout.ORIGIN_CODEC.optionalFieldOf("point_of_origin", AddressSettings.DEFAULTS.pointOfOrigin)
                    .forGetter(AddressSettings::pointOfOrigin),
            Codec.unboundedMap(AddressSettings.GROUP_NAME_CODEC, GroupSettings.CODEC)
                    .optionalFieldOf("groups", Map.of())
                    .forGetter(AddressSettings::groups),
            Codec.unboundedMap(Identifier.CODEC, WorldLayout.CODEC).optionalFieldOf("worlds", Map.of())
                    .forGetter(AddressSettings::worlds)
    ).apply(instance, AddressSettings::new));

    public AddressSettings {
        // Kept in the order the file lists them, which is the order groups without a symbol are given one
        groups = Collections.unmodifiableMap(new LinkedHashMap<>(groups));
        worlds = Map.copyOf(worlds);
    }

    /** @return whether a text is a group's name: lowercase letters, digits and underscores, up to 32 of them */
    public static boolean isGroupName(final String name) {
        return AddressSettings.GROUP_NAME.matcher(name).matches();
    }

    /** @return the layout a world has: its own values where it has them, and the server's elsewhere */
    public AddressLayout layoutOf(final ResourceKey<Level> world) {
        final Optional<WorldLayout> own = Optional.ofNullable(this.worlds.get(world.identifier()));
        return new AddressLayout(own.flatMap(WorldLayout::cellChunks).orElse(this.cellChunks),
                own.flatMap(WorldLayout::pointOfOrigin).orElse(this.pointOfOrigin));
    }

    /** @return the name of the group the settings put a world in, which is {@value #DEFAULT_GROUP} unless set */
    public String groupOf(final ResourceKey<Level> world) {
        return Optional.ofNullable(this.worlds.get(world.identifier())).flatMap(WorldLayout::group)
                .orElse(AddressSettings.DEFAULT_GROUP);
    }

    /**
     * One group's settings.
     *
     * @param symbol the symbol that names the group, as one character; left out, the group is given one that no other
     *     group has when the server first sees it, and keeps it. For the default group, left out means {@code @}
     */
    public record GroupSettings(Optional<String> symbol) {

        public static final GroupSettings AUTOMATIC = new GroupSettings(Optional.empty());

        public static final Codec<GroupSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                AddressLayout.ORIGIN_CODEC.optionalFieldOf("symbol").forGetter(GroupSettings::symbol)
        ).apply(instance, GroupSettings::new));
    }

    /**
     * One world's own address values; a value left out is the server's.
     *
     * @param cellChunks how many chunks wide and long each addressed cell in the world is
     * @param pointOfOrigin the symbol addresses dialed from the world end with, or {@code ""} for none
     * @param group the name of the group the world is in; left out, it is in {@value AddressSettings#DEFAULT_GROUP}
     */
    public record WorldLayout(Optional<Integer> cellChunks, Optional<String> pointOfOrigin, Optional<String> group) {

        public static final Codec<WorldLayout> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                AddressLayout.CELL_CHUNKS_CODEC.optionalFieldOf("cell_chunks").forGetter(WorldLayout::cellChunks),
                AddressLayout.ORIGIN_CODEC.optionalFieldOf("point_of_origin").forGetter(WorldLayout::pointOfOrigin),
                AddressSettings.GROUP_NAME_CODEC.optionalFieldOf("group").forGetter(WorldLayout::group)
        ).apply(instance, WorldLayout::new));
    }
}
