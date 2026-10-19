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
import com.almuradev.sgcraft.api.gate.DialResult;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.GateState;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.computer.ComputerPayloads;
import com.almuradev.sgcraft.mod.computer.DialerRef;
import com.almuradev.sgcraft.mod.entity.DroneEntity;
import com.almuradev.sgcraft.mod.entity.JumperHull;
import com.almuradev.sgcraft.mod.entity.JumperSeat;
import com.almuradev.sgcraft.mod.entity.PuddleJumperEntity;
import com.almuradev.sgcraft.mod.entity.SGEntityTypes;
import com.almuradev.sgcraft.mod.gate.GateFrame;
import com.almuradev.sgcraft.mod.gate.SGIris;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/** Game tests of the puddle jumper: its seats, its computer, its length meeting the world, and going through a gate. */
final class JumperGameTestsNeoForge {

    private JumperGameTestsNeoForge() {
    }

    /** Adds the tests to the list {@link SGGameTestsNeoForge} registers. */
    static void register(final Map<String, BiConsumer<GameTestHelper, BlockPos>> tests) {
        tests.put("jumper_seats_helm_first", JumperGameTestsNeoForge::seatsHelmFirst);
        tests.put("jumper_rider_moves_to_seat_looked_at", JumperGameTestsNeoForge::movesToSeat);
        tests.put("jumper_pilot_alone_dials", JumperGameTestsNeoForge::pilotAloneDials);
        tests.put("jumper_dialer_seat_takes_computer", JumperGameTestsNeoForge::dialerSeatTakesComputer);
        tests.put("jumper_nose_and_tail_stop_against_blocks", JumperGameTestsNeoForge::lengthMeetsWorld);
        tests.put("jumper_carries_riders_through_gate", JumperGameTestsNeoForge::throughGate);
        tests.put("jumper_door_lowers_from_outside", JumperGameTestsNeoForge::doorFromOutside);
        tests.put("jumper_rider_stands_up_inside", JumperGameTestsNeoForge::standsUpInside);
        tests.put("jumper_hull_is_solid_where_built", JumperGameTestsNeoForge::hullSolid);
        tests.put("jumper_lowered_hatch_holds_walkers", JumperGameTestsNeoForge::rampHolds);
        tests.put("jumper_keeps_water_out", JumperGameTestsNeoForge::keepsWaterOut);
        tests.put("jumper_drone_strikes_hostile", JumperGameTestsNeoForge::droneStrikes);
        tests.put("jumper_copilot_works_weapons", JumperGameTestsNeoForge::copilotWeapons);
        tests.put("jumper_seats_only_those_inside", JumperGameTestsNeoForge::seatsOnlyInside);
    }

    // Used from outside, the jumper lowers its cargo door, and raises it again; nobody boards
    private static void doorFromOutside(final GameTestHelper helper, final BlockPos origin) {
        final PuddleJumperEntity jumper = JumperGameTestsNeoForge.place(helper.getLevel(),
                Vec3.atBottomCenterOf(origin), 0);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        // Behind it, looking at its tail
        player.snapTo(jumper.position().add(0, 0, -4), 0, 0);
        helper.assertTrue(!jumper.hatchOpen(), "A new jumper's cargo door is down");
        helper.runAfterDelay(10, () -> {
            jumper.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
            helper.assertTrue(jumper.hatchOpen(), "Using the jumper from outside didn't lower its cargo door");
            helper.assertTrue(player.getVehicle() == null, "Using the jumper from outside boarded it");
            jumper.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
            helper.assertTrue(!jumper.hatchOpen(), "Using the jumper again didn't raise its cargo door");
            helper.succeed();
        });
    }

    // A rider getting up while it stands still stands inside it, in the aisle, rather than outside
    private static void standsUpInside(final GameTestHelper helper, final BlockPos origin) {
        final PuddleJumperEntity jumper = JumperGameTestsNeoForge.place(helper.getLevel(),
                Vec3.atBottomCenterOf(origin), 0);
        final ServerPlayer passenger = helper.makeMockServerPlayerInLevel();
        passenger.snapTo(jumper.position(), 0, 0);
        helper.assertTrue(passenger.startRiding(jumper), "The player couldn't sit");
        final Vec3 at = jumper.getDismountLocationForPassenger(passenger);
        helper.assertTrue(jumper.inside(at.add(0, 0.5, 0)), "A rider got up outside the jumper, at " + at);
        helper.succeed();
    }

    // Its hull's parts stand where it is built: floor under the cabin, walls at the sides, open past the door frame
    private static void hullSolid(final GameTestHelper helper, final BlockPos origin) {
        final PuddleJumperEntity jumper = JumperGameTestsNeoForge.place(helper.getLevel(),
                Vec3.atBottomCenterOf(origin), 37);
        final double angle = jumper.getYRot() * Mth.DEG_TO_RAD;
        final Predicate<Vec3> solid = local -> {
            final Vec3 world = jumper.position().add(JumperHull.toWorld(local, angle));
            return jumper.hull().stream().anyMatch(shape -> shape.bounds().contains(world));
        };
        helper.assertTrue(solid.test(new Vec3(0, 0.1, -1)), "There is no floor under the cabin");
        helper.assertTrue(solid.test(new Vec3(1.3, 1.2, -1)), "There is no wall beside the cabin");
        helper.assertTrue(!solid.test(new Vec3(0, 1.2, -1)), "The cabin's aisle is solid");
        helper.assertTrue(!solid.test(new Vec3(0, 1.2, 0.25)), "The bulkhead's doorway is solid");
        // Turned between the compass points, its walls and consoles reach no further than they are
        helper.assertTrue(!solid.test(new Vec3(0.3, 1, 0.69)) && !solid.test(new Vec3(-0.3, 1, 0.69)),
                "The bulkhead's doorway is narrowed");
        helper.assertTrue(!solid.test(new Vec3(0.75, 1, 1.75)), "The way up to the helm is blocked");
        helper.assertTrue(!solid.test(new Vec3(0, 0.1, -4)), "The shut hatch is solid where it lies lowered");
        helper.succeed();
    }

    // Its hull is sealed: under water, a rider's eyes, and those of someone standing inside, aren't in the water, so
    // they breathe; someone in the water beside it is
    private static void keepsWaterOut(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        for (final BlockPos pos : BlockPos.betweenClosed(origin.offset(-3, 0, -5), origin.offset(5, 4, 5))) {
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        }
        final PuddleJumperEntity jumper = JumperGameTestsNeoForge.place(level, Vec3.atBottomCenterOf(origin), 0);
        final ServerPlayer rider = JumperGameTestsNeoForge.board(helper, jumper);
        final ServerPlayer stander = helper.makeMockServerPlayerInLevel();
        stander.snapTo(jumper.position().add(0, JumperSeat.FLOOR, -1), 0, 0);
        final ServerPlayer swimmer = helper.makeMockServerPlayerInLevel();
        swimmer.snapTo(jumper.position().add(4, 0, 0), 0, 0);
        helper.runAfterDelay(2, () -> {
            jumper.positionRider(rider);
            for (final ServerPlayer player : new ServerPlayer[] {rider, stander, swimmer}) {
                player.baseTick();
            }
            helper.assertTrue(!rider.isEyeInFluid(FluidTags.WATER), "A rider is under water in a jumper");
            helper.assertTrue(!stander.isEyeInFluid(FluidTags.WATER), "Someone inside a jumper is under water");
            helper.assertTrue(swimmer.isEyeInFluid(FluidTags.WATER), "Someone in the water beside it isn't");
            helper.succeed();
        });
    }

    // Armed, its pilot launches a drone at a hostile ahead, which seeks it out and bursts on it; unarmed, none goes
    private static void droneStrikes(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final PuddleJumperEntity jumper = JumperGameTestsNeoForge.place(level, Vec3.atBottomCenterOf(origin), 0);
        jumper.setPowered(true);
        // Mock players load no chunks, so the test's chunk is held loaded while the drone flies
        final ChunkPos chunk = ChunkPos.containing(jumper.blockPosition());
        level.setChunkForced(chunk.x(), chunk.z(), true);
        final ServerPlayer pilot = JumperGameTestsNeoForge.board(helper, jumper);
        final Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE,
                helper.relativeVec(jumper.position().add(0, 4, 0)));
        zombie.setPersistenceRequired();
        jumper.launchDrone(pilot, zombie.getId());
        helper.assertTrue(level.getEntitiesOfClass(DroneEntity.class, jumper.getBoundingBox().inflate(8)).isEmpty(),
                "A drone launched with the bays in");
        jumper.press(pilot, PuddleJumperEntity.Button.WEAPONS);
        // Armed, its drone bays slide out
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(zombie.isAlive(), "The zombie is gone: " + zombie.getRemovalReason());
            helper.assertTrue(jumper.bays(1) >= 1, "The bays are only " + jumper.bays(1) + " out");
            final int before = jumper.drones();
            jumper.launchDrone(pilot, zombie.getId());
            helper.assertTrue(!level.getEntitiesOfClass(DroneEntity.class, jumper.getBoundingBox().inflate(8))
                    .isEmpty(), "No drone launched with the bays out");
            helper.assertTrue(pilot.isCreative() ? jumper.drones() == before : jumper.drones() == before - 1,
                    "The jumper carries " + jumper.drones() + " drones after launching one of " + before);
            helper.succeedWhen(() -> {
                helper.assertTrue(!zombie.isAlive() || zombie.getHealth() < zombie.getMaxHealth(),
                        "The drone hasn't struck the zombie");
                level.setChunkForced(chunk.x(), chunk.z(), false);
            });
        });
    }

    // As it rises, someone standing in its cabin is sat down, but not someone on its roof, its roof come up round
    // their feet
    private static void seatsOnlyInside(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final PuddleJumperEntity jumper = JumperGameTestsNeoForge.place(level, Vec3.atBottomCenterOf(origin.above(2)),
                0);
        final ChunkPos chunk = ChunkPos.containing(jumper.blockPosition());
        level.setChunkForced(chunk.x(), chunk.z(), true);
        final ServerPlayer inside = helper.makeMockServerPlayerInLevel();
        inside.snapTo(jumper.position().add(0, JumperSeat.FLOOR, -1), 0, 0);
        final ServerPlayer onRoof = helper.makeMockServerPlayerInLevel();
        onRoof.snapTo(jumper.position().add(0, PuddleJumperEntity.HEIGHT - 0.4, -1), 0, 0);
        helper.onEachTick(() -> jumper.setPos(jumper.position().add(0, 0.02, 0)));
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(inside.getVehicle() == jumper, "Someone standing in the cabin wasn't sat down");
            helper.assertTrue(onRoof.getVehicle() == null, "Someone on the roof was pulled into the jumper");
            level.setChunkForced(chunk.x(), chunk.z(), false);
            helper.succeed();
        });
    }

    // With a co-pilot aboard the weapons are theirs: the pilot can neither arm them nor launch, and the co-pilot's
    // drone, with nothing locked, flies straight ahead
    private static void copilotWeapons(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final PuddleJumperEntity jumper = JumperGameTestsNeoForge.place(level, Vec3.atBottomCenterOf(origin), 0);
        jumper.setPowered(true);
        final ChunkPos chunk = ChunkPos.containing(jumper.blockPosition());
        level.setChunkForced(chunk.x(), chunk.z(), true);
        final ServerPlayer pilot = JumperGameTestsNeoForge.board(helper, jumper);
        final ServerPlayer copilot = JumperGameTestsNeoForge.board(helper, jumper);
        helper.assertTrue(jumper.weaponsOperator() == copilot, "The co-pilot doesn't work the weapons");
        jumper.press(pilot, PuddleJumperEntity.Button.WEAPONS);
        helper.assertTrue(!jumper.armed(), "The pilot armed the drones with a co-pilot aboard");
        jumper.press(copilot, PuddleJumperEntity.Button.WEAPONS);
        helper.assertTrue(jumper.armed(), "The co-pilot couldn't arm the drones");
        helper.runAfterDelay(50, () -> {
            jumper.launchDrone(pilot, -1);
            helper.assertTrue(level.getEntitiesOfClass(DroneEntity.class, jumper.getBoundingBox().inflate(8))
                    .isEmpty(), "The pilot launched a drone with a co-pilot aboard");
            jumper.launchDrone(copilot, -1);
            final List<DroneEntity> drones = level.getEntitiesOfClass(DroneEntity.class,
                    jumper.getBoundingBox().inflate(8));
            helper.assertTrue(drones.size() == 1, "The co-pilot launched " + drones.size() + " drones");
            final Vec3 ahead = Vec3.directionFromRotation(0, jumper.getYRot());
            helper.assertTrue(drones.get(0).getDeltaMovement().normalize().dot(ahead) > 0.9,
                    "The drone flies " + drones.get(0).getDeltaMovement() + ", not ahead");
            level.setChunkForced(chunk.x(), chunk.z(), false);
            helper.succeed();
        });
    }

    // Its lowered hatch holds up what lands on it, though the jumper hangs in the air
    private static void rampHolds(final GameTestHelper helper, final BlockPos origin) {
        final PuddleJumperEntity jumper = JumperGameTestsNeoForge.place(helper.getLevel(),
                Vec3.atBottomCenterOf(origin.above(3)), 0);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(jumper.position().add(0, 0, -7), 0, 0);
        helper.runAfterDelay(5, () -> {
            jumper.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
            helper.assertTrue(jumper.hatchOpen(), "Using the jumper from outside didn't lower its cargo door");
        });
        // Once it is down, something dropped over its far end lands on it
        helper.runAfterDelay(30, () -> {
            final Vec3 over = jumper.position().add(JumperHull.toWorld(new Vec3(0, 1, -4.5),
                    jumper.getYRot() * Mth.DEG_TO_RAD));
            final ItemEntity item = new ItemEntity(helper.getLevel(), over.x, over.y, over.z,
                    new ItemStack(Items.STONE));
            item.setDeltaMovement(Vec3.ZERO);
            helper.getLevel().addFreshEntity(item);
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(item.getY() > jumper.getY() + 0.1, "Something dropped on the lowered hatch fell"
                        + " through it, to " + item.getY() + " from the jumper's " + jumper.getY());
                helper.succeed();
            });
        });
    }

    // A jumper facing south, as entities with no yaw do, standing on the ground at a place
    private static PuddleJumperEntity place(final ServerLevel level, final Vec3 at, final float yaw) {
        final PuddleJumperEntity jumper = SGEntityTypes.PUDDLE_JUMPER.create(level, EntitySpawnReason.COMMAND);
        jumper.snapTo(at.x, at.y, at.z, yaw, 0);
        level.addFreshEntity(jumper);
        return jumper;
    }

    private static ServerPlayer board(final GameTestHelper helper, final PuddleJumperEntity jumper) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(jumper.position().add(0, 0, -3), 0, 0);
        helper.assertTrue(player.startRiding(jumper), "A player couldn't board the jumper");
        return player;
    }

    // The first aboard takes the helm, the next the dialer's seat, and the rest the cockpit's others, then the cabin's
    private static void seatsHelmFirst(final GameTestHelper helper, final BlockPos origin) {
        final PuddleJumperEntity jumper = JumperGameTestsNeoForge.place(helper.getLevel(),
                Vec3.atBottomCenterOf(origin), 0);
        final ServerPlayer pilot = JumperGameTestsNeoForge.board(helper, jumper);
        final ServerPlayer operator = JumperGameTestsNeoForge.board(helper, jumper);
        final ServerPlayer passenger = JumperGameTestsNeoForge.board(helper, jumper);
        helper.assertTrue(jumper.seatOf(pilot) == JumperSeat.HELM, "The first aboard sits " + jumper.seatOf(pilot));
        helper.assertTrue(jumper.seatOf(operator) == JumperSeat.DIALER,
                "The second aboard sits " + jumper.seatOf(operator));
        helper.assertTrue(jumper.seatOf(passenger) == JumperSeat.COCKPIT_LEFT,
                "The third aboard sits " + jumper.seatOf(passenger));
        helper.assertTrue(jumper.getControllingPassenger() == pilot, "The pilot doesn't fly the jumper");
        // The pilot gets out and the operator takes the helm, which is then theirs to fly from
        pilot.stopRiding();
        jumper.positionRider(operator);
        operator.lookAt(EntityAnchorArgument.Anchor.EYES, JumperGameTestsNeoForge.seatMiddle(jumper, JumperSeat.HELM));
        jumper.interact(operator, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(jumper.seatOf(operator) == JumperSeat.HELM && jumper.getControllingPassenger() == operator,
                "The operator didn't take the helm: " + jumper.seatOf(operator));
        helper.succeed();
    }

    // Where a sitter's middle is in a seat
    private static Vec3 seatMiddle(final PuddleJumperEntity jumper, final JumperSeat seat) {
        return jumper.position().add(seat.offset().add(0, 0.5, 0).yRot(-jumper.getYRot() * Mth.DEG_TO_RAD));
    }

    // A rider using the jumper while looking at a free seat moves there, and stays put looking at a taken one
    private static void movesToSeat(final GameTestHelper helper, final BlockPos origin) {
        final PuddleJumperEntity jumper = JumperGameTestsNeoForge.place(helper.getLevel(),
                Vec3.atBottomCenterOf(origin), 90);
        final ServerPlayer pilot = JumperGameTestsNeoForge.board(helper, jumper);
        final ServerPlayer operator = JumperGameTestsNeoForge.board(helper, jumper);
        jumper.positionRider(pilot);
        pilot.lookAt(EntityAnchorArgument.Anchor.EYES,
                JumperGameTestsNeoForge.seatMiddle(jumper, JumperSeat.CABIN_RIGHT_AFT));
        jumper.interact(pilot, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(jumper.seatOf(pilot) == JumperSeat.CABIN_RIGHT_AFT,
                "The pilot didn't move to the seat they looked at: " + jumper.seatOf(pilot));
        jumper.positionRider(pilot);
        pilot.lookAt(EntityAnchorArgument.Anchor.EYES, JumperGameTestsNeoForge.seatMiddle(jumper, JumperSeat.DIALER));
        jumper.interact(pilot, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(jumper.seatOf(pilot) == JumperSeat.CABIN_RIGHT_AFT
                && jumper.seatOf(operator) == JumperSeat.DIALER, "A rider took a seat someone sits in");
        helper.assertTrue(jumper.getControllingPassenger() == null, "Someone flies the jumper with no one at the helm");
        helper.succeed();
    }

    // Two gates, the first's dialing as quick as can be, and a jumper beside it
    private record Setup(Stargate a, Stargate b, PuddleJumperEntity jumper) {
    }

    private static Setup setUp(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = SGGameTestsNeoForge.place(helper, origin, 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.east(128), 9);
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.OPENING_DAMAGE, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.CHEVRONS_LOCK_ON_DIAL, true, SGGameTestsNeoForge.server(helper));
        final PuddleJumperEntity jumper = JumperGameTestsNeoForge.place(helper.getLevel(),
                Vec3.atBottomCenterOf(a.position().pos().north(8)), 0);
        return new Setup(a, b, jumper);
    }

    private static ComputerPayloads.Action dial(final Setup setup) {
        return new ComputerPayloads.Action(DialerRef.ABOARD, ComputerPayloads.Action.Kind.DIAL,
                SGGameTestsNeoForge.addressOf(setup.b(), setup.a()).toString());
    }

    // Alone aboard, the pilot dials the nearest gate from the helm, and the dial heads the computer's history
    private static void pilotAloneDials(final GameTestHelper helper, final BlockPos origin) {
        final Setup setup = JumperGameTestsNeoForge.setUp(helper, origin);
        // The computer aboard runs only with the drive on
        setup.jumper().setPowered(true);
        final ServerPlayer pilot = JumperGameTestsNeoForge.board(helper, setup.jumper());
        setup.jumper().dialerAction(pilot, JumperGameTestsNeoForge.dial(setup));
        helper.assertTrue(setup.a().state() == GateState.DIALING, "The pilot's dial didn't start: "
                + setup.a().state());
        helper.assertTrue(setup.jumper().dialer().linked().map(setup.a().position().pos()::equals).orElse(false),
                "The computer aboard linked to " + setup.jumper().dialer().linked());
        helper.succeedWhen(() -> helper.assertTrue(!setup.jumper().dialer().history().isEmpty()
                && setup.jumper().dialer().history().getFirst().connected(), "The dial isn't in the history"));
    }

    // With someone in the dialer's seat, the computer is theirs, not the pilot's
    private static void dialerSeatTakesComputer(final GameTestHelper helper, final BlockPos origin) {
        final Setup setup = JumperGameTestsNeoForge.setUp(helper, origin);
        // The computer aboard runs only with the drive on
        setup.jumper().setPowered(true);
        final ServerPlayer pilot = JumperGameTestsNeoForge.board(helper, setup.jumper());
        final ServerPlayer operator = JumperGameTestsNeoForge.board(helper, setup.jumper());
        setup.jumper().dialerAction(pilot, JumperGameTestsNeoForge.dial(setup));
        helper.assertTrue(setup.a().state() == GateState.IDLE, "The pilot dialed with the operator beside them");
        setup.jumper().dialerAction(operator, JumperGameTestsNeoForge.dial(setup));
        helper.assertTrue(setup.a().state() == GateState.DIALING, "The operator's dial didn't start: "
                + setup.a().state());
        helper.succeed();
    }

    // Its nose stops against a wall as its hitbox would, and it won't turn where its tail would go into one
    private static void lengthMeetsWorld(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final PuddleJumperEntity jumper = JumperGameTestsNeoForge.place(level, Vec3.atBottomCenterOf(origin), 0);
        // A wall across its way, south, a block past where its nose reaches now
        final int wallZ = Mth.floor(jumper.getZ() + PuddleJumperEntity.LENGTH / 2) + 1;
        for (int x = -3; x <= 3; x++) {
            for (int y = 0; y < 3; y++) {
                level.setBlock(new BlockPos(origin.getX() + x, origin.getY() + y, wallZ),
                        Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        jumper.move(MoverType.SELF, new Vec3(0, 0, 3));
        final double nose = jumper.getZ() + PuddleJumperEntity.LENGTH / 2;
        helper.assertTrue(nose <= wallZ + 1.0E-5 && nose > wallZ - 0.1,
                "The nose stopped at " + nose + ", not at the wall at " + wallZ);
        // Hard against the wall, a quarter turn would swing its nose and tail out sideways, which there is room for,
        // but a wall alongside stops that too
        helper.assertTrue(jumper.fits(90), "The jumper has no room to turn in the open");
        for (int z = -3; z <= 3; z++) {
            for (int y = 0; y < 3; y++) {
                level.setBlock(BlockPos.containing(jumper.getX() + 2, origin.getY() + y, jumper.getZ() + z),
                        Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        helper.assertTrue(!jumper.fits(90), "The jumper would turn its nose into a wall");
        helper.assertTrue(jumper.fits(0), "The jumper doesn't fit where it is");
        helper.succeed();
    }

    // A jumper flown through an open gate comes out of the other with its riders in their seats
    private static void throughGate(final GameTestHelper helper, final BlockPos origin) {
        final Setup setup = JumperGameTestsNeoForge.setUp(helper, origin);
        final PuddleJumperEntity jumper = setup.jumper();
        final ServerPlayer pilot = JumperGameTestsNeoForge.board(helper, jumper);
        final ServerPlayer passenger = JumperGameTestsNeoForge.board(helper, jumper);
        final AtomicReference<DialResult> result = new AtomicReference<>();
        setup.a().dial(SGGameTestsNeoForge.addressOf(setup.b(), setup.a()), DialMode.QUICK,
                SGGameTestsNeoForge.server(helper)).thenAccept(result::set);
        final GateFrame frame = new GateFrame(setup.a().position().pos(), Direction.NORTH);
        // Once the wormhole is open, put it nose first in front of the horizon, then just behind it
        helper.runAfterDelay(SGIris.MOVE_TICKS + 60, () -> {
            helper.assertTrue(result.get() == DialResult.CONNECTED, "The dial ended " + result.get());
            final Vec3 front = frame.toWorld(new Vec3(0, 0.55, 0.8));
            jumper.snapTo(front.x, front.y, front.z, 0, 0);
        });
        helper.runAfterDelay(SGIris.MOVE_TICKS + 63, () -> {
            final Vec3 behind = frame.toWorld(new Vec3(0, 0.55, -0.4));
            jumper.setPos(behind.x, behind.y, behind.z);
        });
        helper.runAfterDelay(SGIris.MOVE_TICKS + 70, () -> {
            final AABB nearB = new AABB(setup.b().position().pos()).inflate(6);
            final PuddleJumperEntity arrived = helper.getLevel().getEntitiesOfClass(PuddleJumperEntity.class, nearB)
                    .stream().findFirst().orElse(null);
            helper.assertTrue(arrived != null, "The jumper didn't come out of gate B");
            helper.assertTrue(pilot.getVehicle() == arrived && passenger.getVehicle() == arrived,
                    "The riders didn't come through in the jumper");
            helper.assertTrue(arrived.seatOf(pilot) == JumperSeat.HELM && arrived.seatOf(passenger)
                    == JumperSeat.DIALER, "The riders lost their seats");
            helper.succeed();
        });
    }
}
