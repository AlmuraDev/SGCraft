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

package com.almuradev.sgcraft.block;

import com.almuradev.sgcraft.SGCraft;
import com.almuradev.sgcraft.api.gate.GateTypes;
import com.almuradev.sgcraft.registry.RegistrySink;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Function;

/**
 * The mod's blocks. They exist once the loader has called {@link #register(RegistrySink)}.
 */
public final class SGBlocks {

    public static RingBlock STARGATE_RING;
    public static RingBlock STARGATE_CHEVRON;
    public static StargateBaseBlock STARGATE_BASE;
    public static StargateBaseBlock STARGATE_BASE_PEGASUS;
    public static StargateBaseBlock STARGATE_BASE_UNIVERSE;

    private SGBlocks() {
    }

    public static void register(final RegistrySink<Block> sink) {
        SGBlocks.STARGATE_RING = SGBlocks.register(sink, "stargate_ring", properties -> new RingBlock(properties, false));
        SGBlocks.STARGATE_CHEVRON = SGBlocks.register(sink, "stargate_chevron",
                properties -> new RingBlock(properties, true));
        SGBlocks.STARGATE_BASE = SGBlocks.register(sink, "stargate_base",
                properties -> new StargateBaseBlock(properties, GateTypes.MILKY_WAY));
        SGBlocks.STARGATE_BASE_PEGASUS = SGBlocks.register(sink, "stargate_base_pegasus",
                properties -> new StargateBaseBlock(properties, GateTypes.PEGASUS));
        SGBlocks.STARGATE_BASE_UNIVERSE = SGBlocks.register(sink, "stargate_base_universe",
                properties -> new StargateBaseBlock(properties, GateTypes.UNIVERSE));
    }

    private static <B extends Block> B register(final RegistrySink<Block> sink, final String name,
            final Function<BlockBehaviour.Properties, B> factory) {
        final ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SGCraft.id(name));
        final B block = factory.apply(BlockBehaviour.Properties.of()
                .setId(key)
                .mapColor(MapColor.METAL)
                .sound(SoundType.METAL)
                .strength(1.5F, 6.0F)
                .requiresCorrectToolForDrops());
        sink.register(key, block);
        return block;
    }
}
