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
import com.almuradev.sgcraft.mod.address.AddressSettings;
import com.almuradev.sgcraft.mod.address.GridReach;
import com.almuradev.sgcraft.mod.address.LaidOutPartition;
import com.almuradev.sgcraft.mod.address.SGAddressing;
import com.almuradev.sgcraft.mod.address.SecretOrder;
import com.almuradev.sgcraft.mod.address.WorldAddress;
import com.almuradev.sgcraft.mod.block.entity.GateSpawnerBlockEntity;
import com.almuradev.sgcraft.mod.gate.DialStyle;
import com.almuradev.sgcraft.mod.gate.GateStore;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

    // There are 39 symbols, as on the Milky Way ring: + and . are written and read like the others, are position
    // symbols as every symbol but @ is, and each has a glyph of its own on the Milky Way ring and the Universe gate's
    static void thirtyNineSymbols(final GameTestHelper helper, final BlockPos origin) {
        helper.assertTrue(Symbol.COUNT == 39, "There are " + Symbol.COUNT + " symbols, not 39");
        helper.assertTrue(Symbol.of('+').orElseThrow().index() == 37 && Symbol.of('.').orElseThrow().index() == 38,
                "+ and . aren't the last two symbols");
        helper.assertTrue(Symbol.of('@').orElseThrow().equals(Symbol.GIZA), "@ isn't the point of origin");
        // Every cell's position symbols name it again, and between them they use every symbol but @
        final AddressPartition one = SGSettingGameTestsNeoForge.partition(1);
        final Set<Symbol> used = new HashSet<>();
        for (int x = -40; x < 40; x += 3) {
            for (int z = -40; z < 40; z += 3) {
                final List<Symbol> symbols = GridReach.encode(new ChunkPos(x, z), one, 6).orElseThrow();
                used.addAll(symbols);
                helper.assertTrue(GridReach.decode(symbols, one).orElseThrow().equals(new ChunkPos(x, z)),
                        "The address of chunk " + x + ", " + z + " names another");
            }
        }
        helper.assertTrue(!used.contains(Symbol.GIZA), "@ is a position symbol");
        helper.assertTrue(used.size() == Symbol.COUNT - 1, "The positions use " + used.size() + " symbols, not "
                + (Symbol.COUNT - 1));
        // Each symbol has its own glyph on the Milky Way ring, @ the first, and on the Universe gate all but @ do
        final Set<Integer> glyphs = new HashSet<>();
        final Set<Long> arches = new HashSet<>();
        for (int index = 0; index < Symbol.COUNT; index++) {
            final Symbol symbol = new Symbol(index);
            final int glyph = DialStyle.milkyWayGlyph(symbol);
            helper.assertTrue(DialStyle.milkyWaySymbol(glyph).equals(symbol), "Glyph " + glyph + " isn't " + symbol);
            glyphs.add(glyph);
            final double offset = DialStyle.universeOffset(index);
            helper.assertTrue(symbol.equals(Symbol.GIZA) == (offset == 0), "The Universe gate's " + symbol + " is at "
                    + offset);
            arches.add(Math.round(offset * 100));
        }
        helper.assertTrue(glyphs.size() == Symbol.COUNT && DialStyle.milkyWayGlyph(Symbol.GIZA) == 0,
                "The Milky Way ring's glyphs aren't one to a symbol, @ first");
        helper.assertTrue(arches.size() == Symbol.COUNT, "The Universe gate's arches aren't one to a symbol");
        helper.succeed();
    }

    // A secret layout gives every cell one address of its own that names it again, in an order that differs from the
    // public one and from another key's; it is off unless the settings turn it on, and the server keeps its key
    static void secretLayout(final GameTestHelper helper, final BlockPos origin) {
        final byte[] key = new byte[SecretOrder.KEY_BYTES];
        final byte[] other = new byte[SecretOrder.KEY_BYTES];
        Arrays.fill(other, (byte) 7);
        final SecretOrder order = new SecretOrder(key);
        // Over a small grid, every code is used exactly once and leads back to its cell
        final long codes = 38 * 38;
        final Set<Long> seen = new HashSet<>();
        for (long cell = 0; cell < codes; cell++) {
            final long code = order.scramble(cell, codes);
            helper.assertTrue(code >= 0 && code < codes, "Cell " + cell + " got the code " + code);
            helper.assertTrue(seen.add(code), "Two cells got the code " + code);
            helper.assertTrue(order.unscramble(code, codes) == cell, "Code " + code + " leads to another cell");
        }
        final AddressPartition open = SGSettingGameTestsNeoForge.partition(8);
        final AddressPartition secret = new LaidOutPartition(8, Optional.of(Symbol.GIZA), Optional.of(order));
        final AddressPartition otherKey = new LaidOutPartition(8, Optional.of(Symbol.GIZA),
                Optional.of(new SecretOrder(other)));
        int same = 0;
        int sameKeys = 0;
        for (int x = -2000; x < 2000; x += 97) {
            for (int z = -2000; z < 2000; z += 89) {
                final ChunkPos chunk = new ChunkPos(x, z);
                final ChunkPos cell = new ChunkPos(Math.floorDiv(x, 8) * 8, Math.floorDiv(z, 8) * 8);
                final List<Symbol> symbols = GridReach.encode(chunk, secret, 6).orElseThrow();
                helper.assertTrue(GridReach.decode(symbols, secret).orElseThrow().equals(cell),
                        "The secret address of chunk " + x + ", " + z + " names another cell");
                same += symbols.equals(GridReach.encode(chunk, open, 6).orElseThrow()) ? 1 : 0;
                sameKeys += symbols.equals(GridReach.encode(chunk, otherKey, 6).orElseThrow()) ? 1 : 0;
            }
        }
        helper.assertTrue(same == 0, same + " cells have the same address with and without the secret layout");
        helper.assertTrue(sameKeys == 0, sameKeys + " cells have the same address with two keys");
        // Off by default, on for every world when set
        helper.assertTrue(!SGSettingGameTestsNeoForge.parse("{}").orElseThrow().addresses().layoutOf(Level.NETHER)
                .secret(), "The secret layout is on by default");
        SGSettingGameTestsNeoForge.layoutIs(helper, SGSettingGameTestsNeoForge.parse(
                "{\"addresses\": {\"secret_layout\": true}}").orElseThrow(), Level.NETHER,
                new AddressLayout(8, "@", true));
        helper.assertTrue(new WorldAddress(AddressLayout.DEFAULTS, AddressSettings.DEFAULT_GROUP, Optional.empty(),
                Optional.empty()).changesEveryAddress(new WorldAddress(new AddressLayout(8, "@", true),
                AddressSettings.DEFAULT_GROUP, Optional.empty(), Optional.empty())),
                "Turning on the secret layout doesn't change every address");
        // The server makes its key once and keeps it
        final GateStore store = helper.getLevel().getServer().getDataStorage().computeIfAbsent(GateStore.TYPE);
        helper.assertTrue(Arrays.equals(store.addressKey(), store.addressKey()), "The server's key changed");
        helper.succeed();
    }

    // Only a world with gates whose recorded addresses differ from its addresses now gets a line in the warning
    static void layoutChangeWarning(final GameTestHelper helper, final BlockPos origin) {
        final Identifier world = Level.OVERWORLD.identifier();
        final WorldAddress before = new WorldAddress(AddressLayout.DEFAULTS, AddressSettings.DEFAULT_GROUP,
                Optional.of(Symbol.GIZA), Optional.of(new Symbol(0)));
        final WorldAddress after = new WorldAddress(new AddressLayout(4, "@"), AddressSettings.DEFAULT_GROUP,
                Optional.of(Symbol.GIZA), Optional.of(new Symbol(0)));
        helper.assertTrue(SGAddressing.describeChange(world, Optional.of(before), after, 3).isPresent(),
                "A changed world with gates isn't warned about");
        helper.assertTrue(SGAddressing.describeChange(world, Optional.of(before), after, 0).isEmpty(),
                "A changed world without gates is warned about");
        helper.assertTrue(SGAddressing.describeChange(world, Optional.of(before), before, 3).isEmpty(),
                "An unchanged world is warned about");
        helper.assertTrue(SGAddressing.describeChange(world, Optional.empty(), after, 3).isEmpty(),
                "A world with nothing recorded is warned about");
        helper.assertTrue(before.changesEveryAddress(after), "A new cell size doesn't change every address");
        final WorldAddress moved = new WorldAddress(AddressLayout.DEFAULTS, "pegasus", Optional.of(new Symbol(15)),
                Optional.of(new Symbol(0)));
        helper.assertTrue(SGAddressing.describeChange(world, Optional.of(before), moved, 3).isPresent(),
                "A world moved to another group isn't warned about");
        helper.assertTrue(!before.changesEveryAddress(moved),
                "A new group changes every address, not only those of 8 and 9 symbols");
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
