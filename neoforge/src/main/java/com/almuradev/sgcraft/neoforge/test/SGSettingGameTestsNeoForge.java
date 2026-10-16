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
import com.almuradev.sgcraft.api.address.AddressPartition;
import com.almuradev.sgcraft.api.address.Symbol;
import com.almuradev.sgcraft.mod.SGConfig;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.address.AddressLayout;
import com.almuradev.sgcraft.mod.address.GridReach;
import com.almuradev.sgcraft.mod.address.SGAddressing;
import com.almuradev.sgcraft.mod.block.entity.GateSpawnerBlockEntity;
import com.almuradev.sgcraft.mod.villager.SGVillagers;
import com.almuradev.sgcraft.mod.worldgen.StructureGates;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Game tests of the server's settings for addresses and generated gates. They run with {@link SGGameTestsNeoForge},
 * which registers them.
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

    // The addresses section sets the cell size and point of origin for every world, and a world's own values override
    // them one by one; values that would make no addresses are refused
    static void addressSettings(final GameTestHelper helper, final BlockPos origin) {
        final SGConfig config = SGSettingGameTestsNeoForge.parse("{\"addresses\": {\"cell_chunks\": 4,"
                + " \"worlds\": {\"minecraft:the_nether\": {\"point_of_origin\": \"A\"},"
                + " \"minecraft:the_end\": {\"cell_chunks\": 2, \"point_of_origin\": \"\"}}}}").orElseThrow();
        SGSettingGameTestsNeoForge.layoutIs(helper, config, Level.OVERWORLD, new AddressLayout(4, "@"));
        SGSettingGameTestsNeoForge.layoutIs(helper, config, Level.NETHER, new AddressLayout(4, "A"));
        SGSettingGameTestsNeoForge.layoutIs(helper, config, Level.END, new AddressLayout(2, ""));
        helper.assertTrue(SGSettingGameTestsNeoForge.parse("{}").orElseThrow().addresses().layoutOf(Level.OVERWORLD)
                .equals(AddressLayout.DEFAULTS), "A file without an addresses section doesn't have the defaults");
        helper.assertTrue(SGSettingGameTestsNeoForge.parse("{\"addresses\": {\"cell_chunks\": 0}}").isEmpty(),
                "Cells 0 chunks across were taken");
        helper.assertTrue(SGSettingGameTestsNeoForge.parse("{\"addresses\": {\"point_of_origin\": \"AB\"}}")
                .isEmpty(), "A point of origin of two symbols was taken");
        helper.succeed();
    }

    // Every chunk in one cell has the cell's address, which names the cell's lowest chunk, and the next cell's differs
    static void cellSize(final GameTestHelper helper, final BlockPos origin) {
        final AddressPartition four = SGSettingGameTestsNeoForge.partition(4);
        final AddressPartition one = SGSettingGameTestsNeoForge.partition(1);
        final List<Symbol> corner = GridReach.encode(new ChunkPos(0, 0), four, 6).orElseThrow();
        helper.assertTrue(corner.equals(GridReach.encode(new ChunkPos(3, 3), four, 6).orElseThrow()),
                "Two chunks of one 4 chunk cell have different addresses");
        helper.assertTrue(!corner.equals(GridReach.encode(new ChunkPos(4, 0), four, 6).orElseThrow()),
                "The next 4 chunk cell has the same address");
        helper.assertTrue(!corner.equals(GridReach.encode(new ChunkPos(3, 3), one, 6).orElseThrow()),
                "Two 1 chunk cells have the same address");
        final List<Symbol> far = GridReach.encode(new ChunkPos(-9, 13), four, 6).orElseThrow();
        helper.assertTrue(GridReach.decode(far, four).orElseThrow().equals(new ChunkPos(-12, 12)),
                "A 4 chunk cell's address names " + GridReach.decode(far, four).orElseThrow());
        helper.succeed();
    }

    // Only a world with gates whose recorded layout differs from its layout now gets a line in the warning
    static void layoutChangeWarning(final GameTestHelper helper, final BlockPos origin) {
        final Identifier world = Level.OVERWORLD.identifier();
        final AddressLayout before = AddressLayout.DEFAULTS;
        final AddressLayout after = new AddressLayout(4, "@");
        helper.assertTrue(SGAddressing.describeChange(world, Optional.of(before), after, 3).isPresent(),
                "A changed world with gates isn't warned about");
        helper.assertTrue(SGAddressing.describeChange(world, Optional.of(before), after, 0).isEmpty(),
                "A changed world without gates is warned about");
        helper.assertTrue(SGAddressing.describeChange(world, Optional.of(before), before, 3).isEmpty(),
                "An unchanged world is warned about");
        helper.assertTrue(SGAddressing.describeChange(world, Optional.empty(), after, 3).isEmpty(),
                "A world with nothing recorded is warned about");
        helper.succeed();
    }

    private static Optional<SGConfig> parse(final String json) {
        return SGConfig.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).result();
    }

    private static void layoutIs(final GameTestHelper helper, final SGConfig config, final ResourceKey<Level> world,
            final AddressLayout expected) {
        final AddressLayout layout = config.addresses().layoutOf(world);
        helper.assertTrue(layout.equals(expected), world.identifier() + " has " + layout + ", not " + expected);
    }

    private static AddressPartition partition(final int cellChunks) {
        return new AddressPartition() {
            @Override
            public int cellChunks() {
                return cellChunks;
            }

            @Override
            public Optional<Symbol> pointOfOrigin() {
                return Optional.of(Symbol.GIZA);
            }
        };
    }
}
