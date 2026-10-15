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

import com.almuradev.sgcraft.mod.SGCraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.List;
import java.util.Map;

/**
 * SGCraft's loot in vanilla's chests, as in 1.12: naquadah, crystals and ender pearls in dungeons, stronghold
 * libraries, village smiths and the bonus chest. Each SGCraft table under {@code sgcraft:chests} rolls alongside the
 * vanilla chest it is listed against, so a datapack empties or changes SGCraft's share by replacing its table.
 * NeoForge reads the same pairs from its loot modifiers.
 */
public final class ChestLoot {

    private static final Map<ResourceKey<LootTable>, ResourceKey<LootTable>> ADDITIONS = Map.of(
            ChestLoot.vanilla("chests/simple_dungeon"), ChestLoot.sgcraft("simple_dungeon"),
            ChestLoot.vanilla("chests/spawn_bonus_chest"), ChestLoot.sgcraft("spawn_bonus_chest"),
            ChestLoot.vanilla("chests/stronghold_library"), ChestLoot.sgcraft("stronghold_library"),
            // 1.12's village blacksmith is now the weaponsmith and the toolsmith
            ChestLoot.vanilla("chests/village/village_weaponsmith"), ChestLoot.sgcraft("village_blacksmith"),
            ChestLoot.vanilla("chests/village/village_toolsmith"), ChestLoot.sgcraft("village_blacksmith"));

    private ChestLoot() {
    }

    /** Rolls SGCraft's table for the chest {@code table} into {@code drops}, if the chest has one. */
    public static void addTo(final Holder<LootTable> table, final LootContext context, final List<ItemStack> drops) {
        ChestLoot.ADDITIONS.entrySet().stream().filter(entry -> table.is(entry.getKey())).findFirst()
                .flatMap(entry -> context.getResolver().lookupOrThrow(Registries.LOOT_TABLE).get(entry.getValue()))
                .ifPresent(extra -> extra.value().getRandomItemsRaw(context,
                        LootTable.createStackSplitter(context.getLevel(), drops::add)));
    }

    private static ResourceKey<LootTable> vanilla(final String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace(path));
    }

    private static ResourceKey<LootTable> sgcraft(final String name) {
        return ResourceKey.create(Registries.LOOT_TABLE, SGCraft.id("chests/" + name));
    }
}
