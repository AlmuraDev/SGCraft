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

import com.almuradev.sgcraft.mod.block.SGBlocks;
import com.almuradev.sgcraft.mod.item.SGItems;
import com.almuradev.sgcraft.mod.rings.RingPlatformBlockEntity;
import com.almuradev.sgcraft.mod.rings.RingSettings;
import com.almuradev.sgcraft.mod.rings.Rings;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Game tests of the Goa'uld transport rings. They run with {@link SGGameTestsNeoForge}, which registers them.
 */
final class RingGameTestsNeoForge {

    private RingGameTestsNeoForge() {
    }

    static void register(final Map<String, BiConsumer<GameTestHelper, BlockPos>> tests) {
        tests.put("rings_link_and_swap", RingGameTestsNeoForge::linkAndSwap);
        tests.put("rings_need_power", RingGameTestsNeoForge::needPower);
        tests.put("ceiling_rings_drop_to_the_floor", RingGameTestsNeoForge::ceiling);
    }

    // A platform of a kind, owned by a player
    private static RingPlatformBlockEntity platform(final GameTestHelper helper, final BlockPos at, final Block block,
            final ServerPlayer owner) {
        final ServerLevel level = helper.getLevel();
        level.getChunk(at);
        level.setBlock(at, block.defaultBlockState(), Block.UPDATE_ALL);
        if (!(level.getBlockEntity(at) instanceof final RingPlatformBlockEntity platform)) {
            throw helper.assertionException(Component.literal("No ring platform was placed"));
        }
        platform.setOwner(owner.getUUID(), "owner");
        return platform;
    }

    // A pig standing in the middle of a platform's rings
    private static Pig pig(final GameTestHelper helper, final RingPlatformBlockEntity on) {
        return helper.spawnWithNoFreeWill(EntityTypes.PIG, helper.relativeVec(on.floorCenter()));
    }

    private static void run(final ServerLevel level, final RingPlatformBlockEntity... platforms) {
        for (int tick = 0; tick <= Rings.DONE_TICKS; tick++) {
            for (final RingPlatformBlockEntity platform : platforms) {
                RingPlatformBlockEntity.serverTick(level, platform.getBlockPos(), platform.getBlockState(), platform);
            }
        }
    }

    // Sneak-using two platforms links them both ways; started, a platform burns its naquadah for the cost, and what
    // stands inside each set of rings ends up inside the other
    private static void linkAndSwap(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        // In the test's own box, whose chunks run, as only there can the creatures inside the rings be found
        final BlockPos aPos = helper.absolutePos(new BlockPos(3, 1, 8));
        final BlockPos bPos = helper.absolutePos(new BlockPos(13, 1, 8));
        level.setBlock(aPos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(bPos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        final RingPlatformBlockEntity a = RingGameTestsNeoForge.platform(helper, aPos, SGBlocks.RING_PLATFORM, player);
        final RingPlatformBlockEntity b = RingGameTestsNeoForge.platform(helper, bPos, SGBlocks.RING_PLATFORM, player);
        Rings.link(player, a);
        Rings.link(player, b);
        helper.assertTrue(a.linkPos().equals(Optional.of(bPos)) && b.linkPos().equals(Optional.of(aPos)),
                "The platforms didn't link both ways");
        a.setItem(0, new ItemStack(SGItems.NAQUADAH, 2));
        final Pig first = RingGameTestsNeoForge.pig(helper, a);
        final Pig second = RingGameTestsNeoForge.pig(helper, b);
        Rings.activate(player, Optional.of(a));
        helper.assertTrue(a.running() && b.running(), "The rings didn't start");
        final double cost = RingSettings.current().cost(10);
        helper.assertTrue(a.getItem(0).getCount() == 1
                && Math.abs(a.stored() - (RingSettings.naquadahEnergy() - cost)) < 1e-6,
                "The platform holds " + a.stored() + " GE and " + a.getItem(0).getCount()
                        + " naquadah, not one piece's worth less the cost");
        RingGameTestsNeoForge.run(level, a, b);
        helper.assertTrue(!a.running() && !b.running(), "The rings didn't come to rest");
        helper.assertTrue(b.volume().contains(first.position()), "The pig in A's rings is at " + first.position());
        helper.assertTrue(a.volume().contains(second.position()), "The pig in B's rings is at " + second.position());
        helper.succeed();
    }

    // A platform with nothing to burn won't start, and nor will one that isn't linked
    private static void needPower(final GameTestHelper helper, final BlockPos origin) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final RingPlatformBlockEntity a = RingGameTestsNeoForge.platform(helper, origin.offset(3, 0, 3),
                SGBlocks.RING_PLATFORM, player);
        final RingPlatformBlockEntity b = RingGameTestsNeoForge.platform(helper, origin.offset(13, 0, 3),
                SGBlocks.RING_PLATFORM, player);
        b.setStored(RingSettings.current().capacity());
        Rings.activate(player, Optional.of(b));
        helper.assertTrue(!b.running(), "Rings that aren't linked started");
        Rings.linkPair(a, b);
        Rings.activate(player, Optional.of(a));
        helper.assertTrue(!a.running() && !b.running(), "Rings with no energy started");
        Rings.activate(player, Optional.of(b));
        helper.assertTrue(a.running() && b.running(), "Rings with energy didn't start");
        helper.succeed();
    }

    // A ceiling platform's rings stack on the first block below it that can be stood on
    private static void ceiling(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final BlockPos floor = origin.offset(3, 0, 3);
        level.setBlock(floor, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        final RingPlatformBlockEntity platform = RingGameTestsNeoForge.platform(helper, floor.above(5),
                SGBlocks.RING_PLATFORM_CEILING, player);
        helper.assertTrue(platform.ceiling() && platform.findFloor() == floor.getY() + 1,
                "The ceiling platform's rings stack at " + platform.findFloor() + ", not " + (floor.getY() + 1));
        helper.succeed();
    }
}
