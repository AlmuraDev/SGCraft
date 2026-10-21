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
import com.almuradev.sgcraft.mod.obelisk.AsgardObeliskBlock;
import com.almuradev.sgcraft.mod.obelisk.AsgardObeliskBlockEntity;
import com.almuradev.sgcraft.mod.obelisk.Obelisks;
import com.almuradev.sgcraft.mod.zat.Zat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Game tests of the Asgard obelisks and the Zat, which came with them. They run with {@link SGGameTestsNeoForge},
 * which registers them.
 */
final class ObeliskGameTestsNeoForge {

    private ObeliskGameTestsNeoForge() {
    }

    static void register(final Map<String, BiConsumer<GameTestHelper, BlockPos>> tests) {
        tests.put("obelisks_link_and_beam", ObeliskGameTestsNeoForge::linkAndBeam);
        tests.put("obelisk_halves_go_together", ObeliskGameTestsNeoForge::halves);
        tests.put("zat_stuns_then_kills", ObeliskGameTestsNeoForge::zat);
    }

    // An obelisk of a kind, facing south, with both its halves, owned by a player
    private static AsgardObeliskBlockEntity obelisk(final GameTestHelper helper, final BlockPos at, final Block block,
            final ServerPlayer owner) {
        final ServerLevel level = helper.getLevel();
        level.getChunk(at);
        final BlockState lower = block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH);
        level.setBlock(at, lower, Block.UPDATE_ALL);
        level.setBlock(at.above(), lower.setValue(AsgardObeliskBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        if (!(level.getBlockEntity(at) instanceof final AsgardObeliskBlockEntity obelisk)) {
            throw helper.assertionException(Component.literal("No obelisk was placed"));
        }
        obelisk.setOwner(owner.getUUID(), "owner");
        return obelisk;
    }

    // Sneak-using two obelisks of a kind links them both ways; one of another kind won't link to them; touching the
    // gem and standing still beams the player out in front of the other one, facing it
    private static void linkAndBeam(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final BlockPos aPos = origin.offset(1, 0, 1);
        final BlockPos bPos = origin.offset(12, 0, 1);
        final AsgardObeliskBlockEntity a = ObeliskGameTestsNeoForge.obelisk(helper, aPos, SGBlocks.ASGARD_OBELISK,
                player);
        final AsgardObeliskBlockEntity b = ObeliskGameTestsNeoForge.obelisk(helper, bPos, SGBlocks.ASGARD_OBELISK,
                player);
        final AsgardObeliskBlockEntity knotwork = ObeliskGameTestsNeoForge.obelisk(helper, origin.offset(6, 0, 6),
                SGBlocks.ASGARD_OBELISK_KNOTWORK, player);
        Obelisks.link(player, a);
        Obelisks.link(player, knotwork);
        helper.assertTrue(knotwork.link().isEmpty() && a.link().isEmpty(), "Obelisks of two kinds linked");
        Obelisks.link(player, b);
        helper.assertTrue(a.link().equals(Optional.of(GlobalPos.of(level.dimension(), bPos)))
                && b.link().equals(Optional.of(GlobalPos.of(level.dimension(), aPos))),
                "The obelisks didn't link both ways");
        player.snapTo(aPos.getX() + 0.5, aPos.getY(), aPos.getZ() + 2.5, 0, 0);
        Obelisks.transport(player, a);
        for (int tick = 0; tick < Obelisks.BEAM_TICKS; tick++) {
            a.serverTick(level);
        }
        final BlockPos arrived = player.blockPosition();
        helper.assertTrue(arrived.equals(bPos.south(2)), "The player arrived at " + arrived + ", not in front of B at "
                + bPos.south(2));
        helper.succeed();
    }

    // Breaking either half of an obelisk takes the other with it
    private static void halves(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final BlockPos at = origin.offset(2, 0, 2);
        ObeliskGameTestsNeoForge.obelisk(helper, at, SGBlocks.ASGARD_OBELISK, player);
        level.removeBlock(at.above(), false);
        helper.assertTrue(level.getBlockState(at).isAir(), "The bottom half stayed without its top");
        ObeliskGameTestsNeoForge.obelisk(helper, at, SGBlocks.ASGARD_OBELISK, player);
        level.removeBlock(at, false);
        helper.assertTrue(level.getBlockState(at.above()).isAir(), "The top half stayed without its bottom");
        helper.succeed();
    }
    // A Zat's first shot stuns a pig, slowing it, and a second while it is stunned kills it
    private static void zat(final GameTestHelper helper, final BlockPos origin) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, BlockPos.ZERO.above());
        Zat.hit(player, pig);
        helper.assertTrue(pig.isAlive() && Zat.stunned(pig) && pig.hasEffect(MobEffects.SLOWNESS),
                "The first shot didn't stun the pig");
        Zat.hit(player, pig);
        helper.assertTrue(!pig.isAlive(), "The second shot didn't kill the stunned pig");
        helper.succeed();
    }
}
