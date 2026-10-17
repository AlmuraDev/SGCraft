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

import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.GateState;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.block.DialingComputerBlock;
import com.almuradev.sgcraft.mod.block.SGBlocks;
import com.almuradev.sgcraft.mod.block.entity.DialingComputerBlockEntity;
import com.almuradev.sgcraft.mod.block.entity.StargateBaseBlockEntity;
import com.almuradev.sgcraft.mod.computer.ComputerPayloads;
import com.almuradev.sgcraft.mod.computer.DialingComputers;
import com.almuradev.sgcraft.mod.entity.SeatEntity;
import com.almuradev.sgcraft.mod.item.SGItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The SGC's dialing computer and the office chair its operator sits in.
 */
final class ComputerGameTestsNeoForge {

    // Where the computer stands from its gate: north of it, its screen facing north, away from the gate, as in a
    // control room looking down on the gate room
    private static final int COMPUTER_NORTH = 12;

    private ComputerGameTestsNeoForge() {
    }

    /** Adds the tests to the list {@link SGGameTestsNeoForge} registers. */
    static void register(final Map<String, BiConsumer<GameTestHelper, BlockPos>> tests) {
        tests.put("dialing_computer_places_and_breaks_whole", ComputerGameTestsNeoForge::placesAndBreaksWhole);
        tests.put("dialing_computer_links_nearest_gate", ComputerGameTestsNeoForge::linksNearestGate);
        tests.put("dialing_computer_dials_slowly", (helper, origin) ->
                ComputerGameTestsNeoForge.dials(helper, origin, false));
        tests.put("dialing_computer_dials_quickly", (helper, origin) ->
                ComputerGameTestsNeoForge.dials(helper, origin, true));
        tests.put("office_chair_seats_player_at_computer", ComputerGameTestsNeoForge::chairSeats);
    }

    // Places a computer as its item does, its middle lower part at a place, its screen facing north
    private static BlockPos placeComputer(final ServerLevel level, final BlockPos at) {
        final BlockState state = SGBlocks.DIALING_COMPUTER.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        level.setBlock(at, state, Block.UPDATE_ALL);
        SGBlocks.DIALING_COMPUTER.setPlacedBy(level, at, state, null, new ItemStack(SGItems.DIALING_COMPUTER));
        return at;
    }

    // Places a chair north of a computer whose screen faces north, the chair facing south, toward it
    private static BlockPos placeChair(final ServerLevel level, final BlockPos computer) {
        final BlockPos chair = computer.north(2);
        level.setBlock(chair, SGBlocks.OFFICE_CHAIR.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
                Direction.SOUTH), Block.UPDATE_ALL);
        return chair;
    }

    // All six parts are placed together, and breaking any one of them breaks them all
    private static void placesAndBreaksWhole(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos main = ComputerGameTestsNeoForge.placeComputer(level, origin);
        final Direction right = DialingComputerBlock.right(Direction.NORTH);
        for (final DialingComputerBlock.Part part : DialingComputerBlock.Part.values()) {
            final BlockPos pos = main.relative(right, part.column()).above(part.row());
            final BlockState state = level.getBlockState(pos);
            helper.assertTrue(state.is(SGBlocks.DIALING_COMPUTER) && state.getValue(DialingComputerBlock.PART) == part,
                    "The computer's " + part.getSerializedName() + " part is missing");
            helper.assertTrue(DialingComputerBlock.main(pos, state).equals(main),
                    "The " + part.getSerializedName() + " part doesn't find the middle");
        }
        level.setBlock(main.relative(right, -1).above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        for (final DialingComputerBlock.Part part : DialingComputerBlock.Part.values()) {
            final BlockPos pos = main.relative(right, part.column()).above(part.row());
            helper.assertTrue(!level.getBlockState(pos).is(SGBlocks.DIALING_COMPUTER),
                    "The computer's " + part.getSerializedName() + " part was left behind");
        }
        helper.succeed();
    }

    // A computer links to the gate in reach, and tells clients which it is
    private static void linksNearestGate(final GameTestHelper helper, final BlockPos origin) {
        final Stargate gate = SGGameTestsNeoForge.place(helper, origin, 9);
        final BlockPos main = ComputerGameTestsNeoForge.placeComputer(helper.getLevel(),
                gate.position().pos().north(ComputerGameTestsNeoForge.COMPUTER_NORTH));
        final DialingComputerBlockEntity computer = (DialingComputerBlockEntity) helper.getLevel()
                .getBlockEntity(main);
        helper.succeedWhen(() -> {
            helper.assertTrue(computer.gate().isPresent(), "The computer didn't link to the gate");
            helper.assertTrue(computer.linked().map(gate.position().pos()::equals).orElse(false),
                    "The computer linked to " + computer.linked());
        });
    }

    // A player in the chair dials from the computer: slowly, a chevron at a time, or quickly, all at once
    private static void dials(final GameTestHelper helper, final BlockPos origin, final boolean fast) {
        final ServerLevel level = helper.getLevel();
        final Stargate a = SGGameTestsNeoForge.place(helper, origin, 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.east(128), 9);
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.OPENING_DAMAGE, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.RING_SPEED, 30.0, SGGameTestsNeoForge.server(helper));
        final BlockPos main = ComputerGameTestsNeoForge.placeComputer(level,
                a.position().pos().north(ComputerGameTestsNeoForge.COMPUTER_NORTH));
        final BlockPos chair = ComputerGameTestsNeoForge.placeChair(level, main);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(Vec3.atCenterOf(chair.north()), 0, 0);
        DialingComputers.sit(player, chair);
        helper.assertTrue(player.getVehicle() instanceof SeatEntity, "The player wasn't seated");
        DialingComputers.action(player, new ComputerPayloads.Action(main, fast
                ? ComputerPayloads.Action.Kind.DIAL_FAST : ComputerPayloads.Action.Kind.DIAL_SLOW,
                SGGameTestsNeoForge.addressOf(b, a).toString()));
        helper.assertTrue(a.state() == GateState.DIALING, "The gate didn't start dialing: " + a.state());
        final StargateBaseBlockEntity base = (StargateBaseBlockEntity) level.getBlockEntity(a.position().pos());
        helper.runAfterDelay(3, () -> {
            final int engaged = base.dialView().engaged();
            final int length = base.dialView().length();
            if (fast) {
                helper.assertTrue(engaged == length, "A fast dial locked " + engaged + " of " + length);
            } else {
                helper.assertTrue(engaged < length, "A slow dial locked every chevron at once");
            }
        });
        helper.runAfterDelay(4, () -> helper.succeedWhen(() -> helper.assertTrue(a.state() == GateState.CONNECTED,
                "The gate didn't open: " + a.state())));
    }

    // Using the computer seats the player in its chair and opens its screen; standing up closes it, and the seat
    // goes
    private static void chairSeats(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos main = ComputerGameTestsNeoForge.placeComputer(level, origin);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(Vec3.atCenterOf(main.north(3)), 180, 0);
        DialingComputers.use(player, main);
        helper.assertTrue(!player.isPassenger(), "The player sat without a chair");
        final BlockPos chair = ComputerGameTestsNeoForge.placeChair(level, main);
        DialingComputers.use(player, main);
        helper.assertTrue(player.getVehicle() instanceof SeatEntity seat && seat.chair().equals(chair),
                "Using the computer didn't seat the player in its chair");
        helper.assertTrue(DialingComputers.session(player).map(at -> at.pos().equals(main)).orElse(false),
                "The computer's screen wasn't opened");
        final SeatEntity seat = (SeatEntity) player.getVehicle();
        player.stopRiding();
        helper.assertTrue(DialingComputers.session(player).isEmpty(), "Standing up didn't close the screen");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(seat.isRemoved(), "The empty seat stayed");
            helper.succeed();
        });
    }
}
