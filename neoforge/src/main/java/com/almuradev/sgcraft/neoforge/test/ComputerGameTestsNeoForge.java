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

import com.almuradev.sgcraft.api.gate.DialMode;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.GateState;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.block.DialingComputerBlock;
import com.almuradev.sgcraft.mod.block.SGBlocks;
import com.almuradev.sgcraft.mod.block.entity.DialingComputerBlockEntity;
import com.almuradev.sgcraft.mod.block.entity.PowerUnitBlockEntity;
import com.almuradev.sgcraft.mod.block.entity.StargateBaseBlockEntity;
import com.almuradev.sgcraft.mod.computer.ComputerConfigurator;
import com.almuradev.sgcraft.mod.computer.ComputerDialMode;
import com.almuradev.sgcraft.mod.computer.ComputerPayloads;
import com.almuradev.sgcraft.mod.computer.DialingComputers;
import com.almuradev.sgcraft.mod.computer.PoweredComputer;
import com.almuradev.sgcraft.mod.entity.SeatEntity;
import com.almuradev.sgcraft.mod.gate.DialView;
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

import java.util.List;
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
        tests.put("dialing_computer_follows_gate_dial_setting", ComputerGameTestsNeoForge::followsGate);
        tests.put("office_chair_seats_player_at_computer", ComputerGameTestsNeoForge::chairSeats);
        tests.put("dialing_computer_works_standing", ComputerGameTestsNeoForge::standing);
        tests.put("dialing_computer_alarm_turns_off", ComputerGameTestsNeoForge::alarm);
        tests.put("dialing_computer_needs_power", ComputerGameTestsNeoForge::needsPower);
        tests.put("dialing_computer_switches_off", ComputerGameTestsNeoForge::switchesOff);
        tests.put("dialing_computer_configured_by_owner", ComputerGameTestsNeoForge::configuredByOwner);
        tests.put("dialed_gate_isnt_shown_hidden_caller", ComputerGameTestsNeoForge::hiddenCaller);
        tests.put("dialing_computer_history_keeps_last_ten", ComputerGameTestsNeoForge::historyKeepsLastTen);
    }

    // Places a computer as its item does, its middle lower part at a place, its screen facing north, with a full power
    // unit under it to run on
    private static BlockPos placeComputer(final ServerLevel level, final BlockPos at) {
        final BlockState state = SGBlocks.DIALING_COMPUTER.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        level.setBlock(at, state, Block.UPDATE_ALL);
        SGBlocks.DIALING_COMPUTER.setPlacedBy(level, at, state, null, new ItemStack(SGItems.DIALING_COMPUTER));
        ComputerGameTestsNeoForge.powerUnder(level, at);
        return at;
    }

    /**
     * Puts a full power unit under a computer's middle lower part, a dialing computer's or a MALP console's, and
     * switches the computer on, so it finds its power at once, as the test's chunks needn't tick.
     *
     * @return the power unit
     */
    static PowerUnitBlockEntity powerUnder(final ServerLevel level, final BlockPos computer) {
        final BlockPos below = computer.below();
        level.setBlock(below, SGBlocks.POWER_UNIT.defaultBlockState(), Block.UPDATE_ALL);
        final PowerUnitBlockEntity unit = (PowerUnitBlockEntity) level.getBlockEntity(below);
        unit.setStored(PowerUnitBlockEntity.capacity());
        ((PoweredComputer) level.getBlockEntity(computer)).setOn(true);
        return unit;
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

    // A player in the chair dials from the computer set to dial slowly, a chevron at a time, or quickly, all at once,
    // whatever the gate's own setting says
    private static void dials(final GameTestHelper helper, final BlockPos origin, final boolean fast) {
        final ServerLevel level = helper.getLevel();
        final Stargate a = SGGameTestsNeoForge.place(helper, origin, 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.east(128), 9);
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.OPENING_DAMAGE, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.RING_SPEED, 30.0, SGGameTestsNeoForge.server(helper));
        // The gate's own setting is the other way, so the computer overrides it
        a.set(GateProperties.CHEVRONS_LOCK_ON_DIAL, !fast, SGGameTestsNeoForge.server(helper));
        final BlockPos main = ComputerGameTestsNeoForge.placeComputer(level,
                a.position().pos().north(ComputerGameTestsNeoForge.COMPUTER_NORTH));
        final BlockPos chair = ComputerGameTestsNeoForge.placeChair(level, main);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(Vec3.atCenterOf(chair.north()), 0, 0);
        DialingComputers.sit(player, chair);
        helper.assertTrue(player.getVehicle() instanceof SeatEntity, "The player wasn't seated");
        DialingComputers.action(player, new ComputerPayloads.Action(main, ComputerPayloads.Action.Kind.MODE,
                (fast ? ComputerDialMode.FAST : ComputerDialMode.SLOW).getSerializedName()));
        DialingComputers.action(player, new ComputerPayloads.Action(main, ComputerPayloads.Action.Kind.DIAL,
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
        // Once open, the address heads the computer's history, as connected
        final String address = SGGameTestsNeoForge.addressOf(b, a).toString();
        helper.runAfterDelay(4, () -> helper.succeedWhen(() -> {
            helper.assertTrue(a.state() == GateState.CONNECTED, "The gate didn't open: " + a.state());
            final DialingComputerBlockEntity computer = (DialingComputerBlockEntity) level.getBlockEntity(main);
            helper.assertTrue(computer != null && computer.history().equals(List.of(
                    new DialingComputerBlockEntity.Dialed(address, true))), "The computer's history is "
                    + (computer == null ? "missing" : computer.history()));
        }));
    }

    // A computer's history keeps the last ten addresses dialed, newest first, and one dialed again moves to the top
    // rather than standing twice
    private static void historyKeepsLastTen(final GameTestHelper helper, final BlockPos origin) {
        final DialingComputerBlockEntity computer = new DialingComputerBlockEntity(origin,
                SGBlocks.DIALING_COMPUTER.defaultBlockState());
        for (int i = 0; i < 12; i++) {
            computer.remember("ADDRESS" + i, i % 2 == 0);
        }
        helper.assertTrue(computer.history().size() == 10, "The history kept " + computer.history().size());
        helper.assertTrue(computer.history().getFirst().address().equals("ADDRESS11")
                && computer.history().getLast().address().equals("ADDRESS2"), "The history is " + computer.history());
        computer.remember("ADDRESS5", true);
        helper.assertTrue(computer.history().size() == 10
                && computer.history().getFirst().equals(new DialingComputerBlockEntity.Dialed("ADDRESS5", true))
                && computer.history().stream().filter(dialed -> dialed.address().equals("ADDRESS5")).count() == 1,
                "Dialing again didn't move the address to the top: " + computer.history());
        helper.succeed();
    }

    // Set to the gate's setting, the computer dials as the gate's chevrons_lock_on_dial says: here, all at once
    private static void followsGate(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final Stargate a = SGGameTestsNeoForge.place(helper, origin, 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.east(128), 9);
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.OPENING_DAMAGE, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.CHEVRONS_LOCK_ON_DIAL, true, SGGameTestsNeoForge.server(helper));
        final BlockPos main = ComputerGameTestsNeoForge.placeComputer(level,
                a.position().pos().north(ComputerGameTestsNeoForge.COMPUTER_NORTH));
        final DialingComputerBlockEntity computer = (DialingComputerBlockEntity) level.getBlockEntity(main);
        helper.assertTrue(computer.mode() == ComputerDialMode.GATE, "A new computer doesn't follow its gate");
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(Vec3.atCenterOf(main.north(2)), 180, 0);
        DialingComputers.use(player, main);
        DialingComputers.action(player, new ComputerPayloads.Action(main, ComputerPayloads.Action.Kind.DIAL,
                SGGameTestsNeoForge.addressOf(b, a).toString()));
        final StargateBaseBlockEntity base = (StargateBaseBlockEntity) level.getBlockEntity(a.position().pos());
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(base.dialView().engaged() == base.dialView().length(),
                    "The computer didn't lock every chevron at once, as its gate says to");
            helper.succeed();
        });
    }

    // With no chair, using the computer stands the player at its front and opens its screen; walking away closes it
    private static void standing(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos main = ComputerGameTestsNeoForge.placeComputer(level, origin);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(Vec3.atBottomCenterOf(main.north(2)), 180, 0);
        DialingComputers.use(player, main);
        helper.assertTrue(!player.isPassenger(), "The player sat without a chair");
        helper.assertTrue(DialingComputers.standing(player), "The player isn't standing at the computer");
        helper.assertTrue(DialingComputers.session(player).map(at -> at.pos().equals(main)).orElse(false),
                "The computer's screen wasn't opened");
        player.snapTo(Vec3.atBottomCenterOf(main.north(8)), 180, 0);
        // As the computer's tick does; the test's chunks needn't tick
        DialingComputers.tick(level, main);
        helper.assertTrue(DialingComputers.session(player).isEmpty(), "Walking away didn't close the screen");
        helper.succeed();
    }

    // A computer with no power near it won't open, and tells the player so; with a power unit under it, it opens, and
    // pays for running from the unit
    private static void needsPower(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos main = ComputerGameTestsNeoForge.placeComputer(level, origin);
        level.setBlock(main.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        // As the computer's next look for power does; the test's chunks needn't tick
        ((DialingComputerBlockEntity) level.getBlockEntity(main)).power().pay(level, main);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(Vec3.atBottomCenterOf(main.north(2)), 180, 0);
        DialingComputers.use(player, main);
        final DialingComputerBlockEntity computer = (DialingComputerBlockEntity) level.getBlockEntity(main);
        helper.assertTrue(!computer.running(), "The computer runs with no power");
        helper.assertTrue(DialingComputers.session(player).isEmpty(), "The computer opened with no power");
        final PowerUnitBlockEntity unit = ComputerGameTestsNeoForge.powerUnder(level, main);
        DialingComputers.use(player, main);
        helper.assertTrue(computer.running(), "The computer doesn't run on its power unit");
        helper.assertTrue(DialingComputers.session(player).isPresent(), "The powered computer didn't open");
        helper.assertTrue(unit.stored() < PowerUnitBlockEntity.capacity(), "The computer used no gate energy");
        helper.succeed();
    }

    // Switching a computer off closes the screen of the player at it, and it won't open again until switched on;
    // using it switches it on
    private static void switchesOff(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos main = ComputerGameTestsNeoForge.placeComputer(level, origin);
        final DialingComputerBlockEntity computer = (DialingComputerBlockEntity) level.getBlockEntity(main);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(Vec3.atBottomCenterOf(main.north(2)), 180, 0);
        DialingComputers.use(player, main);
        helper.assertTrue(DialingComputers.session(player).isPresent(), "The computer's screen wasn't opened");
        DialingComputers.togglePower(player, main);
        helper.assertTrue(!computer.power().on(), "The computer wasn't switched off");
        helper.assertTrue(DialingComputers.session(player).isEmpty(), "Switching off didn't close the screen");
        DialingComputers.use(player, main);
        helper.assertTrue(computer.running(), "Using the computer didn't switch it on");
        helper.assertTrue(DialingComputers.session(player).isPresent(), "The computer didn't open once on");
        helper.succeed();
    }

    // A computer belongs to the player who placed it: its owner, and no one else, turns off its need for power with a
    // configurator, and it then runs with no power near it
    private static void configuredByOwner(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos main = ComputerGameTestsNeoForge.placeComputer(level, origin);
        level.setBlock(main.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        final DialingComputerBlockEntity computer = (DialingComputerBlockEntity) level.getBlockEntity(main);
        computer.power().pay(level, main);
        helper.assertTrue(!computer.running(), "The computer runs with no power");
        final ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        owner.snapTo(Vec3.atBottomCenterOf(main.north(2)), 180, 0);
        final ServerPlayer other = helper.makeMockServerPlayerInLevel();
        other.snapTo(Vec3.atBottomCenterOf(main.north(2)), 180, 0);
        computer.power().setOwner(owner.getUUID());
        ComputerConfigurator.apply(other, new ComputerPayloads.ConfigApply(main, false, false));
        helper.assertTrue(computer.power().requiresPower(), "Someone else changed the computer's settings");
        ComputerConfigurator.apply(owner, new ComputerPayloads.ConfigApply(main, false, false));
        helper.assertTrue(!computer.power().requiresPower(), "The owner couldn't turn off the need for power");
        helper.assertTrue(computer.running(), "The computer doesn't run once it needs no power");
        helper.succeed();
    }

    // The alarm turns off or on whether a wormhole is coming in or not, and stays that way: turned off before a
    // wormhole, it stays quiet for it and after it; turned on again, it sounds while one comes in
    private static void alarm(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final Stargate a = SGGameTestsNeoForge.place(helper, origin, 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.east(128), 9);
        b.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        b.set(GateProperties.OPENING_DAMAGE, false, SGGameTestsNeoForge.server(helper));
        final BlockPos main = ComputerGameTestsNeoForge.placeComputer(level,
                a.position().pos().north(ComputerGameTestsNeoForge.COMPUTER_NORTH));
        final DialingComputerBlockEntity computer = (DialingComputerBlockEntity) level.getBlockEntity(main);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(Vec3.atBottomCenterOf(main.north(2)), 180, 0);
        final ComputerPayloads.Action toggle = new ComputerPayloads.Action(main, ComputerPayloads.Action.Kind.ALARM,
                "");
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(computer.gate().isPresent(), "The computer didn't link to the gate");
            DialingComputers.use(player, main);
            DialingComputers.action(player, toggle);
            helper.assertTrue(computer.alarmOff(), "The alarm didn't turn off with no wormhole coming in");
            b.dial(SGGameTestsNeoForge.addressOf(a, b), DialMode.QUICK, SGGameTestsNeoForge.server(helper));
        });
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(computer.incoming() && !computer.alarming(),
                    "The alarm sounded for the incoming wormhole though it was turned off");
            DialingComputers.action(player, toggle);
            helper.assertTrue(computer.alarming(), "The alarm didn't sound once turned on again");
            DialingComputers.action(player, toggle);
            b.disconnect(SGGameTestsNeoForge.server(helper));
        });
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(computer.alarmOff(), "The alarm was turned on again once the wormhole shut");
            DialingComputers.action(player, toggle);
            helper.assertTrue(!computer.alarmOff(), "The alarm didn't turn on with no wormhole coming in");
            helper.succeed();
        });
    }

    // A gate that hides its address shows none of its symbols to the gate it dials, only how many there are, so
    // no computer there can show it
    private static void hiddenCaller(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final Stargate a = SGGameTestsNeoForge.place(helper, origin, 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.east(128), 9);
        b.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        b.set(GateProperties.OPENING_DAMAGE, false, SGGameTestsNeoForge.server(helper));
        b.set(GateProperties.ADDRESS_VISIBLE, false, SGGameTestsNeoForge.server(helper));
        final StargateBaseBlockEntity base = (StargateBaseBlockEntity) level.getBlockEntity(a.position().pos());
        final int length = SGGameTestsNeoForge.addressOf(b, a).size();
        b.dial(SGGameTestsNeoForge.addressOf(a, b), DialMode.QUICK, SGGameTestsNeoForge.server(helper));
        helper.runAfterDelay(4, () -> {
            final DialView view = base.dialView();
            helper.assertTrue(view.active() && !view.initiator(), "The gate wasn't dialed");
            helper.assertTrue(view.addressHidden() && view.symbols().isEmpty(),
                    "The dialed gate was shown the hidden caller's symbols: " + view.address());
            helper.assertTrue(view.length() == length, "The dialed gate wasn't shown how long the address is");
            b.disconnect(SGGameTestsNeoForge.server(helper));
            helper.succeed();
        });
    }

    // Using the computer seats the player in its chair and opens its screen; standing up closes it, and the seat
    // goes
    private static void chairSeats(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos main = ComputerGameTestsNeoForge.placeComputer(level, origin);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(Vec3.atCenterOf(main.north(3)), 180, 0);
        final BlockPos chair = ComputerGameTestsNeoForge.placeChair(level, main);
        // A floor round the chair to get up onto
        BlockPos.betweenClosed(chair.below().offset(-1, 0, -1), chair.below().offset(1, 0, 1))
                .forEach(pos -> level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL));
        DialingComputers.use(player, main);
        helper.assertTrue(player.getVehicle() instanceof SeatEntity seat && seat.chair().equals(chair),
                "Using the computer didn't seat the player in its chair");
        helper.assertTrue(DialingComputers.session(player).map(at -> at.pos().equals(main)).orElse(false),
                "The computer's screen wasn't opened");
        final SeatEntity seat = (SeatEntity) player.getVehicle();
        player.stopRiding();
        helper.assertTrue(DialingComputers.session(player).isEmpty(), "Standing up didn't close the screen");
        helper.assertTrue(!player.blockPosition().equals(chair) && player.getY() < chair.getY() + 0.5,
                "The player got up on top of the chair rather than beside it");
        // The test's chunk doesn't tick its entities, so the seat's tick is run here
        seat.tick();
        helper.assertTrue(seat.isRemoved(), "The empty seat stayed");
        helper.succeed();
    }
}
