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

import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.block.SGBlocks;
import com.almuradev.sgcraft.mod.generator.GeneratorMark;
import com.almuradev.sgcraft.mod.generator.GeneratorPowerProvider;
import com.almuradev.sgcraft.mod.generator.NaquadahGeneratorBlock;
import com.almuradev.sgcraft.mod.generator.NaquadahGeneratorBlockEntity;
import com.almuradev.sgcraft.mod.item.SGItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Game tests of the Naquadah Generator. They run with {@link SGGameTestsNeoForge}, which registers them.
 */
final class GeneratorGameTestsNeoForge {

    private GeneratorGameTestsNeoForge() {
    }

    static void register(final Map<String, BiConsumer<GameTestHelper, BlockPos>> tests) {
        tests.put("generator_burns_fuel_and_powers_gate", GeneratorGameTestsNeoForge::burnsAndPowers);
        tests.put("generator_takes_fuel_from_hoppers", GeneratorGameTestsNeoForge::hoppers);
    }

    private static NaquadahGeneratorBlockEntity generator(final GameTestHelper helper, final BlockPos at,
            final Block block) {
        final ServerLevel level = helper.getLevel();
        level.getChunk(at);
        level.setBlock(at, block.defaultBlockState(), Block.UPDATE_ALL);
        if (!(level.getBlockEntity(at) instanceof final NaquadahGeneratorBlockEntity generator)) {
            throw helper.assertionException(Component.literal("No generator was placed"));
        }
        return generator;
    }

    // Switched off, a Mark I gives nothing; switched on, it burns its naquadah at 4 GE a tick into its store, which a
    // gate beside it draws on. It is ticked here, as the test's chunks may not tick it
    private static void burnsAndPowers(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final Stargate a = SGGameTestsNeoForge.place(helper, origin, 9);
        final BlockPos at = origin.offset(3, 0, 3);
        final NaquadahGeneratorBlockEntity generator = GeneratorGameTestsNeoForge.generator(helper, at,
                SGBlocks.NAQUADAH_GENERATOR_MK1);
        helper.assertTrue(generator.mark() == GeneratorMark.ONE, "The Mark I isn't a Mark I");
        generator.setItem(0, new ItemStack(SGItems.NAQUADAH, 4));
        helper.assertTrue(new GeneratorPowerProvider().sourcesFor(a).size() == 1, "The gate doesn't see the generator");
        GeneratorGameTestsNeoForge.tick(level, generator, 20);
        helper.assertTrue(generator.stored() == 0 && generator.available() == 0,
                "A generator switched off made " + generator.stored() + " GE");
        generator.setOn(true);
        GeneratorGameTestsNeoForge.tick(level, generator, 20);
        helper.assertTrue(generator.stored() == 80, "Twenty ticks running made " + generator.stored() + " GE, not 80");
        helper.assertTrue(level.getBlockState(at).getValue(NaquadahGeneratorBlock.RUNNING), "It isn't running");
        helper.assertTrue(generator.getItem(0).getCount() == 3, "It didn't burn one piece of naquadah");
        helper.assertTrue(new GeneratorPowerProvider().sourcesFor(a).getFirst().draw(10, false) == 10
                && generator.available() == 70, "The gate couldn't draw on the generator");
        generator.setOn(false);
        helper.assertTrue(new GeneratorPowerProvider().sourcesFor(a).getFirst().draw(10, false) == 0,
                "The gate drew on a generator switched off");
        helper.succeed();
    }

    private static void tick(final ServerLevel level, final NaquadahGeneratorBlockEntity generator,
            final int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            NaquadahGeneratorBlockEntity.serverTick(level, generator.getBlockPos(),
                    level.getBlockState(generator.getBlockPos()), generator);
        }
    }

    // A hopper puts naquadah and ingots into a generator's fuel slots, but nothing else, and takes nothing out
    private static void hoppers(final GameTestHelper helper, final BlockPos origin) {
        final NaquadahGeneratorBlockEntity generator = GeneratorGameTestsNeoForge.generator(helper, origin,
                SGBlocks.NAQUADAH_GENERATOR_MK2);
        final SimpleContainer hopper = new SimpleContainer(5);
        helper.assertTrue(HopperBlockEntity.addItem(hopper, generator, new ItemStack(SGItems.NAQUADAH_INGOT, 2),
                Direction.UP).isEmpty(), "A hopper couldn't put ingots in");
        helper.assertTrue(HopperBlockEntity.addItem(hopper, generator, new ItemStack(SGItems.NAQUADAH_BLOCK),
                Direction.UP).getCount() == 1, "A hopper put a block of naquadah in");
        helper.assertTrue(!generator.canTakeItemThroughFace(0, generator.getItem(0), Direction.DOWN),
                "A hopper can take fuel out");
        helper.succeed();
    }
}
