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

package com.almuradev.sgcraft.neoforge.test;

import com.almuradev.sgcraft.api.SGCraftApi;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.block.entity.GateSpawnerBlockEntity;
import com.almuradev.sgcraft.mod.villager.SGVillagers;
import com.almuradev.sgcraft.mod.worldgen.StructureGates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;

/**
 * Game tests of the server's settings for generated gates. They run with {@link SGGameTestsNeoForge}, which registers
 * them.
 */
final class SGSettingGameTestsNeoForge {

    private SGSettingGameTestsNeoForge() {
    }

    // A world set off has no Tok'ra at its generated gates, though the gate's structure has one; a world left out
    // of the settings has them
    static void tokraWorlds(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final Map<Identifier, Boolean> before = StructureGates.tokraWorlds();
        helper.assertTrue(StructureGates.tokraIn(ResourceKey.create(Registries.DIMENSION, SGCraft.id("unlisted"))),
                "A world left out of the settings has no Tok'ra");
        final Map<Identifier, Boolean> worlds = new HashMap<>(before);
        worlds.put(level.dimension().identifier(), false);
        StructureGates.tokraWorlds(worlds);
        helper.assertTrue(!StructureGates.tokraIn(level.dimension()), "A world set off has Tok'ra");
        final ChunkPos chunk = ChunkPos.containing(origin);
        // The spawner builds its gate when its block entity ticks, which needs the chunk kept running
        level.setChunkForced(chunk.x(), chunk.z(), true);
        level.getChunk(origin);
        final boolean placed = level.getStructureTemplateManager().getOrCreate(SGCraft.id("stargate/swamp_platform"))
                .placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.getRandom(),
                        Block.UPDATE_CLIENTS);
        helper.assertTrue(placed, "The template wasn't placed");
        final BlockPos base = origin.offset(4, 1, 6);
        if (!(level.getBlockEntity(base) instanceof GateSpawnerBlockEntity spawner)) {
            throw helper.assertionException(Component.literal("The template has no spawner at its base"));
        }
        spawner.tokra(true);
        helper.succeedWhen(() -> {
            helper.assertTrue(SGCraftApi.get().stargates().at(GlobalPos.of(level.dimension(), base)).isPresent(),
                    "The template built no gate");
            StructureGates.tokraWorlds(before);
            level.setChunkForced(chunk.x(), chunk.z(), false);
            helper.assertTrue(level.getEntitiesOfClass(Villager.class, new AABB(base).inflate(16),
                    villager -> villager.getVillagerData().profession().is(SGVillagers.TOKRA)).isEmpty(),
                    "A Tok'ra stands at a gate in a world set off");
        });
    }
}
