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

package com.almuradev.sgcraft.mod.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Locale;
import java.util.Optional;

/**
 * How a gate is added to one kind of structure: the template it is built from, how often a structure gets one,
 * and where it stands. Built {@linkplain Placement#INSIDE inside}, the template replaces the middle of the
 * structure's first piece, turned the way that piece is, as the original built its gates into pyramids, temples,
 * fortresses and monuments. Built {@linkplain Placement#BESIDE beside}, it goes a random distance off the first
 * piece along each axis, turned a random way, somewhere none of the structure's pieces are.
 *
 * @param template the structure template the gate and its surroundings are built from
 * @param chance how many of these structures in a hundred get a gate
 * @param placement whether the gate is built into the structure or beside it
 * @param minDistance beside the structure, the fewest blocks the gate stands from its first piece, along each axis
 * @param maxDistance beside the structure, the most blocks the gate stands from its first piece, along each axis
 * @param ground what the template's bottom layer is set on
 * @param yOffset blocks to raise the template by from where its ground puts it, or lower it by if negative
 * @param foundation what is built under the template's bottom layer, down to the ground
 * @param foundationBlock the block a foundation is built of
 * @param chevronUpgradeChance how many gates in a hundred have nine chevrons in place of seven
 * @param tokra whether a Tok'ra stands in front of the gate once it is built
 */
public record StructureGate(Identifier template, int chance, Placement placement, int minDistance,
        int maxDistance, Ground ground, int yOffset, Foundation foundation, BlockState foundationBlock,
        int chevronUpgradeChance, boolean tokra) {

    public static final Codec<StructureGate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("template").forGetter(StructureGate::template),
            Codec.intRange(0, 100).optionalFieldOf("chance", 25).forGetter(StructureGate::chance),
            Placement.CODEC.optionalFieldOf("placement", Placement.BESIDE).forGetter(StructureGate::placement),
            Codec.intRange(0, 96).optionalFieldOf("min_distance", 10).forGetter(StructureGate::minDistance),
            Codec.intRange(0, 96).optionalFieldOf("max_distance", 30).forGetter(StructureGate::maxDistance),
            Ground.CODEC.optionalFieldOf("ground", Ground.SURFACE).forGetter(StructureGate::ground),
            Codec.INT.optionalFieldOf("y_offset", 0).forGetter(StructureGate::yOffset),
            Foundation.CODEC.optionalFieldOf("foundation", Foundation.FULL).forGetter(StructureGate::foundation),
            BlockState.CODEC.optionalFieldOf("foundation_block", Blocks.STONE_BRICKS.defaultBlockState())
                    .forGetter(StructureGate::foundationBlock),
            Codec.intRange(0, 100).optionalFieldOf("chevron_upgrade_chance", 25)
                    .forGetter(StructureGate::chevronUpgradeChance),
            Codec.BOOL.optionalFieldOf("tokra", true).forGetter(StructureGate::tokra)
    ).apply(instance, StructureGate::new));

    /** Where a gate goes against its structure. */
    public enum Placement implements StringRepresentable {
        /** In the middle of the structure's first piece, in place of the blocks there. */
        INSIDE,
        /** Off to one side, clear of every piece of the structure. */
        BESIDE;

        public static final Codec<Placement> CODEC = StringRepresentable.fromEnum(Placement::values);

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    /** What a gate's template is set on. */
    public enum Ground implements StringRepresentable {
        /** The top of the land or of any water on it, as the world generator first shapes it. */
        SURFACE(Heightmap.Types.WORLD_SURFACE_WG, false),
        /** The floor under any water. */
        OCEAN_FLOOR(Heightmap.Types.OCEAN_FLOOR_WG, false),
        /**
         * The bottom of the structure's first piece. A structure that settles onto the ground once it is built, as
         * pyramids and temples do, takes the gate with it.
         */
        STRUCTURE(null, false),
        /** The top of the structure's first piece, which takes the gate with it as it settles. */
        STRUCTURE_TOP(null, true);

        public static final Codec<Ground> CODEC = StringRepresentable.fromEnum(Ground::values);

        private final Heightmap.Types heightmap;
        private final boolean top;

        Ground(final Heightmap.Types heightmap, final boolean top) {
            this.heightmap = heightmap;
            this.top = top;
        }

        /** @return the heightmap that finds this ground, or empty if the structure sets it */
        public Optional<Heightmap.Types> heightmap() {
            return Optional.ofNullable(this.heightmap);
        }

        /** @return whether this is the top of the structure's first piece, not its bottom */
        public boolean top() {
            return this.top;
        }

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    /** What holds a gate's template up. */
    public enum Foundation implements StringRepresentable {
        /** Nothing: the template rests on what is under it. */
        NONE,
        /** The whole of the template's bottom layer, filled down to solid ground. */
        FULL,
        /** A pillar under each corner of the template, two blocks square, down to solid ground. */
        CORNERS;

        public static final Codec<Foundation> CODEC = StringRepresentable.fromEnum(Foundation::values);

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}
