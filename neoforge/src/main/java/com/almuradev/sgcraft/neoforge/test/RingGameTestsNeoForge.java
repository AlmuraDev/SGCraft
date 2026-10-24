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
import com.almuradev.sgcraft.mod.rings.RingHolds;
import com.almuradev.sgcraft.mod.rings.RingPlatformBlockEntity;
import com.almuradev.sgcraft.mod.rings.RingSettings;
import com.almuradev.sgcraft.mod.rings.Rings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

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
        tests.put("rings_run_in_two_halves", RingGameTestsNeoForge::twoHalves);
        tests.put("rings_hold_players_unharmed", RingGameTestsNeoForge::holdPlayers);
        tests.put("ring_panel_reaches_further_than_remote", RingGameTestsNeoForge::panelReach);
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

    // Two linked floor platforms in the test's own box, whose chunks run, as only there can those inside the rings be
    // found, with the first owned by a player and holding enough to send
    private static RingPlatformBlockEntity[] pair(final GameTestHelper helper, final ServerPlayer owner) {
        final ServerLevel level = helper.getLevel();
        final BlockPos aPos = helper.absolutePos(new BlockPos(3, 1, 8));
        final BlockPos bPos = helper.absolutePos(new BlockPos(13, 1, 8));
        level.setBlock(aPos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(bPos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        final RingPlatformBlockEntity a = RingGameTestsNeoForge.platform(helper, aPos, SGBlocks.RING_PLATFORM, owner);
        final RingPlatformBlockEntity b = RingGameTestsNeoForge.platform(helper, bPos, SGBlocks.RING_PLATFORM, owner);
        Rings.linkPair(a, b);
        a.setStored(RingSettings.current().capacity());
        return new RingPlatformBlockEntity[] {a, b};
    }

    // A transport runs in two halves: the receiver's rings set off once the sender's half is over, and what is inside
    // each set is swapped only then; both are busy until the receiver's half is over
    private static void twoHalves(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final RingPlatformBlockEntity[] pair = RingGameTestsNeoForge.pair(helper, player);
        final RingPlatformBlockEntity a = pair[0];
        final RingPlatformBlockEntity b = pair[1];
        final Pig first = RingGameTestsNeoForge.pig(helper, a);
        final Pig second = RingGameTestsNeoForge.pig(helper, b);
        final long now = level.getGameTime();
        Rings.activate(player, Optional.of(a));
        helper.assertTrue(a.startTime() == now && b.startTime() == now + Rings.HALF_TICKS,
                "The rings set off at " + a.startTime() + " and " + b.startTime() + ", not " + now + " and "
                        + (now + Rings.HALF_TICKS));
        for (int tick = 1; tick <= Rings.DONE_TICKS; tick++) {
            RingPlatformBlockEntity.serverTick(level, a.getBlockPos(), a.getBlockState(), a);
            RingPlatformBlockEntity.serverTick(level, b.getBlockPos(), b.getBlockState(), b);
            if (tick == Rings.HALF_TICKS - 1) {
                helper.assertTrue(a.volume().contains(first.position()) && b.volume().contains(second.position()),
                        "What was inside the rings was swapped before the receiver's half");
            } else if (tick == Rings.HALF_TICKS) {
                helper.assertTrue(b.volume().contains(first.position()) && a.volume().contains(second.position()),
                        "What was inside the rings wasn't swapped as the receiver's half started");
            } else if (tick == Rings.DONE_TICKS - 1) {
                helper.assertTrue(a.running() && b.running(), "The rings came to rest before the receiver's half "
                        + "was over");
            }
        }
        helper.assertTrue(!a.running() && !b.running(), "The rings didn't come to rest");
        helper.succeed();
    }

    // Players inside the sender's rings are held from the start, and those inside the receiver's from just before the
    // swap, until the receiver's rings are home: put back should they drift, looking where they like, moved with the
    // swap and held where they arrive, and taking no damage until they are let go
    private static void holdPlayers(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        final RingPlatformBlockEntity[] pair = RingGameTestsNeoForge.pair(helper, owner);
        final RingPlatformBlockEntity a = pair[0];
        final RingPlatformBlockEntity b = pair[1];
        final ServerPlayer leaving = RingGameTestsNeoForge.standIn(helper, a);
        final ServerPlayer returning = RingGameTestsNeoForge.standIn(helper, b);
        leaving.setYRot(90);
        final Vec3 stood = leaving.position();
        Rings.activate(owner, Optional.of(a));
        helper.assertTrue(RingHolds.held(leaving), "The player inside the sender's rings isn't held");
        helper.assertTrue(!RingHolds.held(returning), "The player inside the receiver's rings is held already");
        helper.assertTrue(!leaving.hurtServer(level, level.damageSources().fellOutOfWorld(), 1)
                && leaving.getHealth() == leaving.getMaxHealth(), "The held player took damage");
        leaving.teleportTo(stood.x + 0.6, stood.y, stood.z);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(leaving.position().distanceToSqr(stood) < 0.01,
                    "The held player wasn't put back: they are at " + leaving.position() + ", not " + stood);
            helper.assertTrue(leaving.getYRot() == 90, "Putting the held player back turned them to "
                    + leaving.getYRot());
        });
        helper.runAfterDelay(Rings.HALF_TICKS - 5, () -> {
            helper.assertTrue(a.volume().contains(leaving.position()), "The player left before the receiver's half");
            helper.assertTrue(RingHolds.held(returning), "The player inside the receiver's rings isn't held before "
                    + "the swap");
        });
        helper.runAfterDelay(Rings.HALF_TICKS + 5, () -> {
            helper.assertTrue(b.volume().contains(leaving.position()) && a.volume().contains(returning.position()),
                    "The players weren't swapped as the receiver's half started");
            helper.assertTrue(RingHolds.held(leaving) && RingHolds.held(returning), "The players were let go at the "
                    + "swap");
            helper.assertTrue(!returning.hurtServer(level, level.damageSources().fellOutOfWorld(), 1),
                    "The held player took damage after the swap");
        });
        helper.runAfterDelay(Rings.DONE_TICKS - 5, () -> {
            helper.assertTrue(a.running() && b.running(), "The rings came to rest before the receiver's half was over");
            helper.assertTrue(RingHolds.held(leaving) && RingHolds.held(returning),
                    "The players were let go before the receiver's rings were home");
            helper.assertTrue(b.volume().contains(leaving.position()), "The arrived player wasn't held where they "
                    + "arrived");
        });
        helper.runAfterDelay(Rings.DONE_TICKS + 5, () -> {
            helper.assertTrue(!a.running() && !b.running(), "The rings didn't come to rest");
            helper.assertTrue(!RingHolds.held(leaving) && !RingHolds.held(returning),
                    "The players weren't let go once it was over");
            helper.assertTrue(leaving.hurtServer(level, level.damageSources().fellOutOfWorld(), 1),
                    "The player let go took no damage");
            helper.succeed();
        });
    }

    // A player standing in the middle of a platform's rings, safe from damage only as a player whose client has loaded
    // is
    private static ServerPlayer standIn(final GameTestHelper helper, final RingPlatformBlockEntity platform) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.connection.markClientLoaded();
        final Vec3 at = platform.floorCenter();
        player.teleportTo(at.x, at.y, at.z);
        return player;
    }

    // A control panel starts a platform up to 16 blocks away; a remote reaches only 8
    private static void panelReach(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final BlockPos aPos = helper.absolutePos(new BlockPos(3, 1, 8));
        level.setBlock(aPos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        final RingPlatformBlockEntity a = RingGameTestsNeoForge.platform(helper, aPos, SGBlocks.RING_PLATFORM, player);
        // The partner far off, beyond either's reach
        final RingPlatformBlockEntity b = RingGameTestsNeoForge.platform(helper,
                helper.absolutePos(new BlockPos(44, 1, 8)), SGBlocks.RING_PLATFORM, player);
        Rings.linkPair(a, b);
        a.setStored(RingSettings.current().capacity());
        final BlockPos panel = helper.absolutePos(new BlockPos(17, 2, 8));
        level.setBlock(panel, SGBlocks.RING_PANEL.defaultBlockState(), Block.UPDATE_ALL);
        final double distance = Vec3.atCenterOf(panel).distanceTo(a.center());
        helper.assertTrue(distance > Rings.REMOTE_REACH && distance < Rings.PANEL_REACH,
                "The panel is " + distance + " blocks from the platform");
        player.teleportTo(panel.getX() + 0.5, panel.getY() - 1, panel.getZ() + 0.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SGItems.RING_REMOTE));
        player.getItemInHand(InteractionHand.MAIN_HAND).use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!a.running(), "A remote started rings " + distance + " blocks away");
        level.getBlockState(panel).useWithoutItem(level, player,
                new BlockHitResult(Vec3.atCenterOf(panel), Direction.NORTH, panel, false));
        helper.assertTrue(a.running() && b.running(), "The panel didn't start rings " + distance + " blocks away");
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
