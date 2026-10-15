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

package com.almuradev.sgcraft.mod.villager;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.block.SGBlocks;
import com.almuradev.sgcraft.mod.registry.RegistrySink;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * The Tok'ra, as the original SGCraft had them: villagers who keep watch over a gate a structure left, and trade the
 * crystals a gate is built from. A Tok'ra works at a DHD, which no other villager takes for a job, and its trades are
 * the trade set {@code sgcraft:tokra/level_1}, which a datapack can change.
 */
public final class SGVillagers {

    /** A DHD, as the place a Tok'ra works. */
    public static final ResourceKey<PoiType> DHD = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE,
            SGCraft.id("dhd"));
    /** The Tok'ra. */
    public static final ResourceKey<VillagerProfession> TOKRA = ResourceKey.create(Registries.VILLAGER_PROFESSION,
            SGCraft.id("tokra"));

    private static final ResourceKey<TradeSet> TOKRA_TRADES = ResourceKey.create(Registries.TRADE_SET,
            SGCraft.id("tokra/level_1"));
    // How far out in front of a gate a Tok'ra looks for room to stand, and how far up
    private static final int NEAREST = 2;
    private static final int FURTHEST = 5;
    private static final int RISE = 2;

    private SGVillagers() {
    }

    /** @return every block state a Tok'ra can work at */
    public static Set<BlockState> dhdStates() {
        final Set<BlockState> states = new HashSet<>();
        for (final Block block : new Block[] {SGBlocks.DHD, SGBlocks.DHD_PEGASUS}) {
            states.addAll(block.getStateDefinition().getPossibleStates());
        }
        return Set.copyOf(states);
    }

    /** Registers the DHD as a place to work, for a loader whose registry fills in the block states itself. */
    public static void registerPoiTypes(final RegistrySink<PoiType> sink) {
        sink.register(SGVillagers.DHD, new PoiType(SGVillagers.dhdStates(), 1, 1));
    }

    /** Registers the Tok'ra. */
    public static void registerProfessions(final RegistrySink<VillagerProfession> sink) {
        sink.register(SGVillagers.TOKRA, new VillagerProfession(
                Component.translatable("entity.sgcraft.villager.tokra"),
                poi -> poi.is(SGVillagers.DHD), poi -> poi.is(SGVillagers.DHD), ImmutableSet.of(), ImmutableSet.of(),
                null, Int2ObjectMap.ofEntries(Int2ObjectMap.entry(1, SGVillagers.TOKRA_TRADES))));
    }

    /**
     * Puts a Tok'ra in front of a gate, as the original did beside the gates it built.
     *
     * @param base the gate's base
     * @param facing the way the gate faces
     * @return the Tok'ra, if there was room for one
     */
    public static Optional<Villager> spawnTokra(final ServerLevel level, final BlockPos base, final Direction facing) {
        final Optional<BlockPos> spot = SGVillagers.standingRoom(level, base, facing);
        if (spot.isEmpty()) {
            return Optional.empty();
        }
        final Villager tokra = EntityTypes.VILLAGER.create(level, EntitySpawnReason.STRUCTURE);
        if (tokra == null) {
            return Optional.empty();
        }
        tokra.snapTo(spot.get(), facing.getOpposite().toYRot(), 0);
        tokra.finalizeSpawn(level, level.getCurrentDifficultyAt(spot.get()), EntitySpawnReason.STRUCTURE, null);
        tokra.setVillagerData(tokra.getVillagerData().withProfession(level.registryAccess(), SGVillagers.TOKRA));
        // A villager with no experience and no place to work goes back to having no job before it finds the DHD
        tokra.setVillagerXp(1);
        tokra.setPersistenceRequired();
        tokra.refreshBrain(level);
        level.addFreshEntity(tokra);
        return Optional.of(tokra);
    }

    // The nearest spot in front of the gate with ground under it and room for a villager
    private static Optional<BlockPos> standingRoom(final ServerLevel level, final BlockPos base,
            final Direction facing) {
        for (int out = SGVillagers.NEAREST; out <= SGVillagers.FURTHEST; out++) {
            for (int up = 0; up <= SGVillagers.RISE; up++) {
                final BlockPos feet = base.relative(facing, out).above(up);
                if (level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                        && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                        && level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)) {
                    return Optional.of(feet);
                }
            }
        }
        return Optional.empty();
    }
}
