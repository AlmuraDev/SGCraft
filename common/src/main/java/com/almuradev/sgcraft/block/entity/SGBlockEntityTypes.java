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

package com.almuradev.sgcraft.block.entity;

import com.almuradev.sgcraft.SGCraft;
import com.almuradev.sgcraft.block.SGBlocks;
import com.almuradev.sgcraft.registry.RegistrySink;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

/**
 * The mod's block entity types. They exist once the loader has called {@link #register(RegistrySink)}, after the
 * blocks.
 */
public final class SGBlockEntityTypes {

    public static BlockEntityType<StargateBaseBlockEntity> STARGATE_BASE;
    public static BlockEntityType<DhdBlockEntity> DHD;
    public static BlockEntityType<GateSpawnerBlockEntity> GATE_SPAWNER;
    public static BlockEntityType<PowerUnitBlockEntity> POWER_UNIT;

    private SGBlockEntityTypes() {
    }

    public static void register(final RegistrySink<BlockEntityType<?>> sink) {
        SGBlockEntityTypes.STARGATE_BASE = new BlockEntityType<>(StargateBaseBlockEntity::new,
                Set.of(SGBlocks.STARGATE_BASE, SGBlocks.STARGATE_BASE_PEGASUS, SGBlocks.STARGATE_BASE_UNIVERSE,
                        SGBlocks.SUPERGATE_BASE));
        sink.register(ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, SGCraft.id("stargate_base")),
                SGBlockEntityTypes.STARGATE_BASE);
        SGBlockEntityTypes.DHD = new BlockEntityType<>(DhdBlockEntity::new, Set.of(SGBlocks.DHD, SGBlocks.DHD_PEGASUS));
        sink.register(ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, SGCraft.id("dhd")), SGBlockEntityTypes.DHD);
        SGBlockEntityTypes.GATE_SPAWNER = new BlockEntityType<>(GateSpawnerBlockEntity::new,
                Set.copyOf(SGBlocks.SPAWNERS));
        sink.register(ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, SGCraft.id("gate_spawner")),
                SGBlockEntityTypes.GATE_SPAWNER);
        SGBlockEntityTypes.POWER_UNIT = new BlockEntityType<>(PowerUnitBlockEntity::new, Set.of(SGBlocks.POWER_UNIT));
        sink.register(ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, SGCraft.id("power_unit")),
                SGBlockEntityTypes.POWER_UNIT);
    }
}
