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

import com.almuradev.sgcraft.api.ActionResult;
import com.almuradev.sgcraft.api.gate.DialMode;
import com.almuradev.sgcraft.api.gate.DialResult;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.IrisState;
import com.almuradev.sgcraft.api.gate.IrisTypes;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.block.SGBlocks;
import com.almuradev.sgcraft.mod.block.entity.PowerUnitBlockEntity;
import com.almuradev.sgcraft.mod.gate.GateFrame;
import com.almuradev.sgcraft.mod.gate.SGIris;
import com.almuradev.sgcraft.mod.item.SGItems;
import com.almuradev.sgcraft.mod.shield.CityShieldBlockEntity;
import com.almuradev.sgcraft.mod.shield.ShieldConsoles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/**
 * The gate shield, the shield console and the city shield.
 */
final class ShieldGameTestsNeoForge {

    private ShieldGameTestsNeoForge() {
    }

    /** Adds the tests to the list {@link SGGameTestsNeoForge} registers. */
    static void register(final Map<String, BiConsumer<GameTestHelper, BlockPos>> tests) {
        tests.put("shield_takes_energy", ShieldGameTestsNeoForge::shieldTakesEnergy);
        tests.put("shield_needs_energy_to_rise", ShieldGameTestsNeoForge::shieldNeedsEnergy);
        tests.put("shield_replaces_iris", ShieldGameTestsNeoForge::shieldReplacesIris);
        tests.put("shield_stops_travel", ShieldGameTestsNeoForge::shieldStopsTravel);
        tests.put("shield_console_finds_gate", ShieldGameTestsNeoForge::consoleFindsGate);
        tests.put("city_shield_needs_energy", ShieldGameTestsNeoForge::cityNeedsEnergy);
        tests.put("city_shield_stops_explosions", ShieldGameTestsNeoForge::cityStopsExplosions);
        tests.put("city_shield_pushes_back", ShieldGameTestsNeoForge::cityPushesBack);
    }

    // A raised shield takes its energy each tick, and falls once the gate has none left
    private static void shieldTakesEnergy(final GameTestHelper helper, final BlockPos origin) {
        final Stargate gate = SGGameTestsNeoForge.place(helper, origin, 9);
        gate.set(GateProperties.IRIS, IrisTypes.SHIELD, SGGameTestsNeoForge.server(helper));
        gate.set(GateProperties.ENERGY_CAPACITY, 1000L, SGGameTestsNeoForge.server(helper));
        gate.energy().insert(30, false);
        helper.assertTrue(gate.iris().orElseThrow().close(SGGameTestsNeoForge.server(helper)) == ActionResult.DONE,
                "Raising the shield failed");
        helper.runAfterDelay(SGIris.SHIELD_TICKS + 5, () -> {
            helper.assertTrue(gate.iris().orElseThrow().state() == IrisState.CLOSED, "The shield didn't rise");
            helper.assertTrue(gate.energy().stored() < 30, "The raised shield took no energy");
        });
        helper.runAfterDelay(30 + 2 * SGIris.SHIELD_TICKS + 10, () -> {
            helper.assertTrue(gate.energy().stored() == 0, "The shield left " + gate.energy().stored());
            helper.assertTrue(gate.iris().orElseThrow().state() == IrisState.OPEN, "The shield didn't fall: "
                    + gate.iris().orElseThrow().state());
            helper.succeed();
        });
    }

    // Without energy a shield won't rise, unless the gate needs none
    private static void shieldNeedsEnergy(final GameTestHelper helper, final BlockPos origin) {
        final Stargate gate = SGGameTestsNeoForge.place(helper, origin, 9);
        gate.set(GateProperties.IRIS, IrisTypes.SHIELD, SGGameTestsNeoForge.server(helper));
        helper.assertTrue(gate.iris().orElseThrow().close(SGGameTestsNeoForge.server(helper))
                == ActionResult.NOT_POSSIBLE, "A shield rose with no energy");
        gate.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        helper.assertTrue(gate.iris().orElseThrow().close(SGGameTestsNeoForge.server(helper)) == ActionResult.DONE,
                "A shield on a gate that needs no power didn't rise");
        helper.succeed();
    }

    // A shield upgrade used on a gate with an iris fitted from its upgrade takes its place, and gives that back;
    // not while the iris is shut
    private static void shieldReplacesIris(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        level.getChunk(origin);
        final Stargate gate = SGGameTestsNeoForge.place(helper, origin, 9);
        gate.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(Vec3.atCenterOf(origin.north(3)), 180, 0);
        player.setGameMode(GameType.SURVIVAL);
        gate.setOwner(Optional.of(player.getUUID()), SGGameTestsNeoForge.server(helper));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SGItems.IRIS_UPGRADE));
        ShieldGameTestsNeoForge.useOnRing(player, level, origin);
        helper.assertTrue(gate.get(GateProperties.IRIS).map(IrisTypes.METAL::equals).orElse(false),
                "The iris wasn't fitted");
        gate.iris().orElseThrow().close(SGGameTestsNeoForge.server(helper));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SGItems.SHIELD_UPGRADE));
        ShieldGameTestsNeoForge.useOnRing(player, level, origin);
        helper.assertTrue(gate.get(GateProperties.IRIS).map(IrisTypes.METAL::equals).orElse(false),
                "A shield replaced a shut iris");
        gate.iris().orElseThrow().open(SGGameTestsNeoForge.server(helper));
        helper.runAfterDelay(SGIris.MOVE_TICKS + 5, () -> {
            ShieldGameTestsNeoForge.useOnRing(player, level, origin);
            helper.assertTrue(gate.get(GateProperties.IRIS).map(IrisTypes.SHIELD::equals).orElse(false),
                    "The shield didn't replace the iris");
            helper.assertTrue(player.getInventory().countItem(SGItems.SHIELD_UPGRADE) == 0,
                    "The shield upgrade wasn't used up");
            helper.assertTrue(player.getInventory().countItem(SGItems.IRIS_UPGRADE) == 1,
                    "The iris upgrade didn't come back");
            helper.assertTrue(gate.iris().orElseThrow().state() == IrisState.OPEN, "The new shield isn't lowered");
            helper.succeed();
        });
    }

    // Uses the held item on the ring beside a gate's base, as a player would
    private static void useOnRing(final ServerPlayer player, final ServerLevel level, final BlockPos base) {
        player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(base.east()), Direction.NORTH, base.east(), false));
    }

    // A pig walked into a wormhole whose far end has its shield raised dies against it
    private static void shieldStopsTravel(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = SGGameTestsNeoForge.place(helper, origin, 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(128, 0, 0), 9);
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.OPENING_DAMAGE, false, SGGameTestsNeoForge.server(helper));
        b.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        b.set(GateProperties.IRIS, IrisTypes.SHIELD, SGGameTestsNeoForge.server(helper));
        b.iris().orElseThrow().close(SGGameTestsNeoForge.server(helper));
        final AtomicReference<DialResult> result = new AtomicReference<>();
        a.dial(SGGameTestsNeoForge.addressOf(b, a), DialMode.QUICK, SGGameTestsNeoForge.server(helper))
                .thenAccept(result::set);
        final GateFrame frame = new GateFrame(a.position().pos(), Direction.NORTH);
        final AtomicReference<Mob> pig = new AtomicReference<>();
        // Once the wormhole is open and the kawoosh has passed, walk a pig through gate A
        helper.runAfterDelay(90, () -> {
            helper.assertTrue(result.get() == DialResult.CONNECTED, "The dial ended " + result.get());
            final Mob spawned = helper.spawnWithNoFreeWill(EntityTypes.PIG, BlockPos.ZERO.above());
            spawned.setNoGravity(true);
            final Vec3 front = frame.toWorld(new Vec3(0, 1, 0.8));
            spawned.setPos(front.x, front.y, front.z);
            pig.set(spawned);
        });
        helper.runAfterDelay(93, () -> {
            final Vec3 behind = frame.toWorld(new Vec3(0, 1, -0.4));
            pig.get().setPos(behind.x, behind.y, behind.z);
        });
        helper.runAfterDelay(100, () -> {
            final boolean atB = !helper.getLevel().getEntitiesOfClass(Mob.class,
                    new AABB(b.position().pos()).inflate(4)).isEmpty();
            helper.assertTrue(!atB, "The pig went through a raised shield");
            helper.assertTrue(!pig.get().isAlive(), "The pig didn't die against the shield");
            helper.succeed();
        });
    }

    // A console finds the nearest gate with a shield, within its range, and only one with a shield
    private static void consoleFindsGate(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final Stargate gate = SGGameTestsNeoForge.place(helper, origin, 9);
        final BlockPos console = origin.north(6);
        level.setBlock(console, SGBlocks.SHIELD_CONSOLE.defaultBlockState(), Block.UPDATE_ALL);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(Vec3.atCenterOf(console.north()), 180, 0);
        helper.assertTrue(ShieldConsoles.gateFor(player, console).isEmpty(), "A console found a gate with no shield");
        gate.set(GateProperties.IRIS, IrisTypes.SHIELD, SGGameTestsNeoForge.server(helper));
        helper.assertTrue(ShieldConsoles.gateFor(player, console).map(found -> found.position()
                .equals(gate.position())).orElse(false), "The console didn't find the shielded gate");
        helper.succeed();
    }

    // A generator with too little energy round it won't raise its shield; with enough it does, and lowers it again
    private static void cityNeedsEnergy(final GameTestHelper helper, final BlockPos origin) {
        ZpmGameTestsNeoForge.ticking(helper, origin);
        final CityShieldBlockEntity shield = ShieldGameTestsNeoForge.generator(helper, origin, false);
        helper.assertTrue(!shield.raise(), "A shield rose with no energy");
        ShieldGameTestsNeoForge.charge(helper, origin);
        helper.assertTrue(shield.raise() && shield.active(), "A shield didn't rise with energy");
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(shield.active(), "The shield fell");
            shield.lower();
            helper.assertTrue(!shield.active(), "The shield didn't lower");
            helper.succeed();
        });
    }

    // An explosion outside a raised shield leaves the blocks inside it be; inside, it doesn't
    private static void cityStopsExplosions(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final CityShieldBlockEntity shield = ShieldGameTestsNeoForge.generator(helper, origin, true);
        shield.resize(8, 8, 8);
        helper.assertTrue(shield.raise(), "The shield didn't rise");
        // A block just inside the shield's edge, another outside, and an explosion just outside between them
        final BlockPos inside = origin.east(7);
        final BlockPos alsoOutside = origin.east(10);
        level.setBlock(inside, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(alsoOutside, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        level.explode(null, origin.getX() + 8.9, origin.getY() + 0.5, origin.getZ() + 0.5, 3.0F,
                Level.ExplosionInteraction.TNT);
        helper.assertTrue(level.getBlockState(inside).is(Blocks.DIRT), "The explosion broke a block inside");
        helper.assertTrue(level.getBlockState(alsoOutside).isAir(), "The explosion didn't break a block outside");
        helper.succeed();
    }

    // A pig moving into a raised shield is pushed back out
    private static void cityPushesBack(final GameTestHelper helper, final BlockPos origin) {
        ZpmGameTestsNeoForge.ticking(helper, origin);
        final CityShieldBlockEntity shield = ShieldGameTestsNeoForge.generator(helper, origin, true);
        shield.resize(6, 6, 6);
        helper.assertTrue(shield.raise(), "The shield didn't rise");
        final Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, BlockPos.ZERO.above());
        pig.setNoGravity(true);
        pig.setPos(origin.getX() + 7.0, origin.getY(), origin.getZ() + 0.5);
        helper.runAfterDelay(2, () -> pig.setDeltaMovement(-0.4, 0, 0));
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(!shield.inside(pig.position().add(0, pig.getBbHeight() / 2, 0)),
                    "The pig got into the shield, at " + pig.position());
            helper.succeed();
        });
    }

    // A city shield generator at a place, with a full power unit under it if it is to be charged
    private static CityShieldBlockEntity generator(final GameTestHelper helper, final BlockPos at,
            final boolean charged) {
        final ServerLevel level = helper.getLevel();
        level.getChunk(at);
        level.setBlock(at, SGBlocks.CITY_SHIELD_GENERATOR.defaultBlockState(), Block.UPDATE_ALL);
        if (charged) {
            ShieldGameTestsNeoForge.charge(helper, at);
        }
        if (level.getBlockEntity(at) instanceof final CityShieldBlockEntity shield) {
            return shield;
        }
        throw helper.assertionException(Component.literal("No generator was placed"));
    }

    // Puts a full power unit beside a generator
    private static void charge(final GameTestHelper helper, final BlockPos at) {
        final ServerLevel level = helper.getLevel();
        level.setBlock(at.below(), SGBlocks.POWER_UNIT.defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(at.below()) instanceof final PowerUnitBlockEntity unit) {
            unit.setStored(PowerUnitBlockEntity.capacity());
        }
    }
}
