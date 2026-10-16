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

import com.almuradev.sgcraft.api.SGCraftApi;
import com.almuradev.sgcraft.api.gate.DialMode;
import com.almuradev.sgcraft.api.gate.DialResult;
import com.almuradev.sgcraft.api.gate.GatePlacement;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.GateSize;
import com.almuradev.sgcraft.api.gate.GateTypes;
import com.almuradev.sgcraft.api.gate.PlacementMode;
import com.almuradev.sgcraft.api.gate.PlacementResult;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.SGConfig;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.block.SGBlocks;
import com.almuradev.sgcraft.mod.block.entity.EnergyConverterBlockEntity;
import com.almuradev.sgcraft.mod.block.entity.PowerUnitBlockEntity;
import com.almuradev.sgcraft.mod.block.entity.StargateBaseBlockEntity;
import com.almuradev.sgcraft.mod.energy.FeRate;
import com.almuradev.sgcraft.mod.energy.SGGateEnergy;
import com.almuradev.sgcraft.mod.gate.SGStargates;
import com.almuradev.sgcraft.mod.shield.CityShieldBlockEntity;
import com.almuradev.sgcraft.mod.zpm.ZpmDeviceBlockEntity;
import com.almuradev.sgcraft.mod.zpm.ZpmItem;
import com.almuradev.sgcraft.mod.zpm.Zpms;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/**
 * ZPMs as energy: supergates drawing on them or on any other power, the console, interface cart and hub, the order
 * gates draw on their sources in, what an eighth chevron costs, and the city shield drawing on ZPMs.
 */
final class ZpmGameTestsNeoForge {

    private ZpmGameTestsNeoForge() {
    }

    /** Adds the tests to the list {@link SGGameTestsNeoForge} registers. */
    static void register(final Map<String, BiConsumer<GameTestHelper, BlockPos>> tests) {
        tests.put("zpm_supergate_draws_on_zpms", ZpmGameTestsNeoForge::supergateDrawsOnZpms);
        tests.put("supergate_takes_any_power", ZpmGameTestsNeoForge::supergateTakesAnyPower);
        tests.put("gate_draws_nearest_first", ZpmGameTestsNeoForge::drawsNearestFirst);
        tests.put("zpm_console_powers_gate", ZpmGameTestsNeoForge::consolePowersGate);
        tests.put("energy_converter_gives_fe", ZpmGameTestsNeoForge::converterGivesFe);
        tests.put("zpm_console_gives_no_fe", ZpmGameTestsNeoForge::consoleGivesNoFe);
        tests.put("zpm_hub_goes_further", ZpmGameTestsNeoForge::hubGoesFurther);
        tests.put("eighth_chevron_costs_a_lot", ZpmGameTestsNeoForge::eighthChevronCostsALot);
        tests.put("zpm_city_shield_runs_on_zpms", ZpmGameTestsNeoForge::cityShieldRunsOnZpms);
        tests.put("settings_file_sections", ZpmGameTestsNeoForge::settingsFileSections);
    }

    /**
     * Keeps the chunk round a place loaded and running for a test, as the tests' gates are built away from the test's
     * own area, where nothing ticks. The test world is thrown away after the run, so it stays forced.
     */
    static void ticking(final GameTestHelper helper, final BlockPos at) {
        final ChunkPos chunk = ChunkPos.containing(at);
        helper.getLevel().setChunkForced(chunk.x(), chunk.z(), true);
    }

    /** Fills a supergate's base with full ZPMs, and returns its ZPM slots. */
    static Container fillBase(final GameTestHelper helper, final Stargate gate) {
        if (!(helper.getLevel().getBlockEntity(gate.position().pos()) instanceof final StargateBaseBlockEntity base)) {
            throw helper.assertionException(Component.literal("There is no base"));
        }
        for (int slot = 0; slot < base.zpms().getContainerSize(); slot++) {
            base.zpms().setItem(slot, ZpmItem.full(false));
        }
        return base.zpms();
    }

    // A ZPM device of a kind beside a place, with full ZPMs in every slot
    private static ZpmDeviceBlockEntity device(final GameTestHelper helper, final BlockPos at, final Block block) {
        final ServerLevel level = helper.getLevel();
        level.getChunk(at);
        level.setBlock(at, block.defaultBlockState(), Block.UPDATE_ALL);
        if (!(level.getBlockEntity(at) instanceof final ZpmDeviceBlockEntity device)) {
            throw helper.assertionException(Component.literal("No ZPM device was placed"));
        }
        for (int slot = 0; slot < device.zpms().getContainerSize(); slot++) {
            device.zpms().setItem(slot, ZpmItem.full(false));
        }
        return device;
    }

    // A supergate with no stored energy dials on the ZPMs in its base, and draws on them
    private static void supergateDrawsOnZpms(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = ZpmGameTestsNeoForge.supergate(helper, origin);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(128, 0, 0), 9);
        final Container zpms = ZpmGameTestsNeoForge.fillBase(helper, a);
        SGGameTestsNeoForge.expectDial(helper, a, b, SGGameTestsNeoForge.server(helper), DialResult.CONNECTED, () ->
                helper.assertTrue(Zpms.energy(Zpms.charged(zpms)) < zpms.getContainerSize() * ZpmItem.capacity(),
                        "The ZPMs weren't drawn on"));
    }

    // A supergate with no ZPMs at all dials on any other power it has
    private static void supergateTakesAnyPower(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = ZpmGameTestsNeoForge.supergate(helper, origin);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(128, 0, 0), 9);
        a.set(GateProperties.ENERGY_CAPACITY, 10_000_000L, SGGameTestsNeoForge.server(helper));
        a.energy().insert(10_000_000, false);
        SGGameTestsNeoForge.expectDial(helper, a, b, SGGameTestsNeoForge.server(helper), DialResult.CONNECTED, () ->
                helper.assertTrue(a.energy().stored() < 10_000_000, "The supergate didn't use its energy"));
    }

    private static Stargate supergate(final GameTestHelper helper, final BlockPos origin) {
        final PlacementResult placed = SGCraftApi.get().stargates().place(GatePlacement.builder()
                .position(GlobalPos.of(helper.getLevel().dimension(), origin)).orientation(FrontAndTop.NORTH_UP)
                .type(SGGameTestsNeoForge.type(helper, GateTypes.MILKY_WAY)).size(new GateSize(4)).chevrons(9)
                .mode(PlacementMode.FORCE).noFixtures().build(), SGGameTestsNeoForge.server(helper));
        return placed.gate().orElseThrow();
    }

    // A gate pays from its nearest power unit first, and the next when that runs dry
    private static void drawsNearestFirst(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final Stargate gate = SGGameTestsNeoForge.place(helper, origin, 9);
        gate.set(GateProperties.ENERGY_CAPACITY, 0L, SGGameTestsNeoForge.server(helper));
        final BlockPos far = origin.north(2).east(4);
        final BlockPos near = origin.north(2).east(2);
        // The far one is placed first, so the order the world scans in can't be what decides
        level.setBlock(far, SGBlocks.POWER_UNIT.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(near, SGBlocks.POWER_UNIT.defaultBlockState(), Block.UPDATE_ALL);
        if (!(level.getBlockEntity(near) instanceof final PowerUnitBlockEntity first)
                || !(level.getBlockEntity(far) instanceof final PowerUnitBlockEntity second)) {
            throw helper.assertionException(Component.literal("The power units weren't placed"));
        }
        first.setStored(1000);
        second.setStored(1000);
        helper.assertTrue(((SGGateEnergy) gate.energy()).pay(400), "The gate couldn't pay 400");
        helper.assertTrue(first.stored() == 600 && second.stored() == 1000, "The units hold " + first.stored()
                + " and " + second.stored() + " after 400 was paid, not 600 and 1000");
        helper.assertTrue(((SGGateEnergy) gate.energy()).pay(900), "The gate couldn't pay 900");
        helper.assertTrue(first.stored() == 0 && second.stored() == 700, "The units hold " + first.stored()
                + " and " + second.stored() + " after 1300 was paid, not 0 and 700");
        helper.succeed();
    }

    // A ZPM console near a gate powers its dial
    private static void consolePowersGate(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = SGGameTestsNeoForge.place(helper, origin, 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(128, 0, 0), 9);
        final ZpmDeviceBlockEntity console = ZpmGameTestsNeoForge.device(helper, origin.north(3).east(3),
                SGBlocks.ZPM_CONSOLE);
        SGGameTestsNeoForge.expectDial(helper, a, b, SGGameTestsNeoForge.server(helper), DialResult.CONNECTED, () ->
                helper.assertTrue(console.stored() < ZpmItem.capacity(), "The console's ZPM wasn't drawn on"));
    }

    // An energy converter against a ZPM console turns its gate energy into FE for the power unit at its output, no
    // faster than its rate, and the unit keeps what that FE buys back
    private static void converterGivesFe(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        ZpmGameTestsNeoForge.ticking(helper, origin);
        final ZpmDeviceBlockEntity console = ZpmGameTestsNeoForge.device(helper, origin, SGBlocks.ZPM_CONSOLE);
        level.setBlock(origin.east(), SGBlocks.ENERGY_CONVERTER.defaultBlockState()
                .setValue(DirectionalBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
        level.setBlock(origin.east(2), SGBlocks.POWER_UNIT.defaultBlockState(), Block.UPDATE_ALL);
        helper.runAfterDelay(10, () -> {
            if (!(level.getBlockEntity(origin.east(2)) instanceof final PowerUnitBlockEntity unit)) {
                throw helper.assertionException(Component.literal("There is no power unit"));
            }
            final double drawn = ZpmItem.capacity() - console.stored();
            final double bought = FeRate.toGeIn(FeRate.toFeOut(drawn));
            helper.assertTrue(unit.stored() > 0, "The converter gave the power unit nothing");
            helper.assertTrue(Math.abs(bought - unit.stored()) < 1, "The unit holds " + unit.stored() + " GE for "
                    + drawn + " GE drawn, not " + bought);
            helper.assertTrue(drawn <= 12 * EnergyConverterBlockEntity.rate(), "The converter took " + drawn
                    + " GE in 10 ticks, more than its rate");
            helper.succeed();
        });
    }

    // A ZPM console gives no FE of its own, and a converter whose output faces the console gives it none either
    private static void consoleGivesNoFe(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        ZpmGameTestsNeoForge.ticking(helper, origin);
        ZpmGameTestsNeoForge.device(helper, origin, SGBlocks.ZPM_CONSOLE);
        level.setBlock(origin.west(), SGBlocks.POWER_UNIT.defaultBlockState(), Block.UPDATE_ALL);
        // Its input against a second, charged unit, and its output against the console, which takes no FE
        level.setBlock(origin.east(), SGBlocks.ENERGY_CONVERTER.defaultBlockState()
                .setValue(DirectionalBlock.FACING, Direction.WEST), Block.UPDATE_ALL);
        level.setBlock(origin.east(2), SGBlocks.POWER_UNIT.defaultBlockState(), Block.UPDATE_ALL);
        if (!(level.getBlockEntity(origin.east(2)) instanceof final PowerUnitBlockEntity charged)) {
            throw helper.assertionException(Component.literal("There is no power unit"));
        }
        charged.setStored(1000);
        helper.runAfterDelay(10, () -> {
            if (!(level.getBlockEntity(origin.west()) instanceof final PowerUnitBlockEntity beside)) {
                throw helper.assertionException(Component.literal("There is no power unit"));
            }
            helper.assertTrue(beside.stored() == 0, "The console gave FE: the unit beside it holds " + beside.stored());
            helper.assertTrue(charged.stored() == 1000, "The converter took " + (1000 - charged.stored())
                    + " GE it had nowhere to give");
            helper.succeed();
        });
    }

    // A hub with three ZPMs draws a third as much from them for the same energy
    private static void hubGoesFurther(final GameTestHelper helper, final BlockPos origin) {
        final ZpmDeviceBlockEntity hub = ZpmGameTestsNeoForge.device(helper, origin, SGBlocks.ZPM_HUB);
        final double before = hub.stored();
        final long given = hub.draw(3000, false);
        final double used = before - hub.stored();
        helper.assertTrue(given == 3000 && Math.abs(used - 1000) < 1e-6, "The hub gave " + given + " for " + used
                + " GE from its ZPMs, not 1000");
        helper.succeed();
    }

    // A dial with an eighth chevron, from the Overworld to the End, costs far more than the opening of a plain gate
    private static void eighthChevronCostsALot(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        if (end == null) {
            throw helper.assertionException(Component.literal("There is no End"));
        }
        final Stargate a = SGGameTestsNeoForge.place(helper, origin, 9);
        a.set(GateProperties.ENERGY_CAPACITY, 100_000_000L, SGGameTestsNeoForge.server(helper));
        a.energy().insert(100_000_000, false);
        a.set(GateProperties.OPENING_DAMAGE, false, SGGameTestsNeoForge.server(helper));
        final BlockPos far = new BlockPos(origin.getX() + 2000, 100, origin.getZ());
        end.getChunk(far);
        final PlacementResult placed = SGCraftApi.get().stargates().place(GatePlacement.builder()
                .position(GlobalPos.of(Level.END, far)).orientation(FrontAndTop.NORTH_UP)
                .type(SGGameTestsNeoForge.type(helper, GateTypes.MILKY_WAY)).chevrons(9)
                .mode(PlacementMode.FORCE).noFixtures().build(), SGGameTestsNeoForge.server(helper));
        final Stargate b = placed.gate().orElseThrow();
        final AtomicReference<DialResult> result = new AtomicReference<>();
        a.dial(SGGameTestsNeoForge.addressOf(b, a), DialMode.QUICK, SGGameTestsNeoForge.server(helper))
                .thenAccept(result::set);
        helper.succeedWhen(() -> {
            helper.assertTrue(result.get() == DialResult.CONNECTED, "The dial ended " + result.get());
            // Opening a standard gate takes 4000 at the least; with the eighth chevron's multiplier it takes 10 times
            helper.assertTrue(100_000_000 - a.energy().stored() >= 10 * 4000, "The dial took only "
                    + (100_000_000 - a.energy().stored()));
            a.disconnect(SGGameTestsNeoForge.server(helper));
        });
    }

    // A city shield with a ZPM in its own slot and no power unit rises, and draws on it as it stands
    private static void cityShieldRunsOnZpms(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        level.getChunk(origin);
        level.setBlock(origin, SGBlocks.CITY_SHIELD_GENERATOR.defaultBlockState(), Block.UPDATE_ALL);
        if (!(level.getBlockEntity(origin) instanceof final CityShieldBlockEntity shield)) {
            throw helper.assertionException(Component.literal("No generator was placed"));
        }
        helper.assertTrue(!shield.raise(), "A shield rose with no energy");
        shield.zpms().setItem(0, ZpmItem.full(false));
        // Wide and flat, to take a unit of energy every few ticks without reaching far
        shield.resize(40, 40, 40);
        helper.assertTrue(shield.raise(), "A shield with a ZPM didn't rise");
        for (int tick = 0; tick < 40; tick++) {
            CityShieldBlockEntity.serverTick(level, origin, shield);
        }
        final double left = ZpmItem.energy(shield.zpms().getItem(0));
        shield.lower();
        helper.assertTrue(left < ZpmItem.capacity(), "The shield didn't draw on its ZPM");
        helper.succeed();
    }

    // The settings file is read by section, a setting left out takes its default, and what is written reads back
    private static void settingsFileSections(final GameTestHelper helper, final BlockPos origin) {
        final String file = "{\"energy\": {\"per_naquadah\": 50000,"
                + " \"bridge_rates\": {\"fe_in_per_ge\": 40, \"fe_out_per_ge\": 20},"
                + " \"multipliers\": {\"inter_world\": 3.5}},"
                + " \"gates\": {\"log_events\": true}, \"tools\": {\"range\": 9},"
                + " \"zpm\": {\"capacity\": 1000}}";
        final SGConfig config = SGConfig.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(file)).getOrThrow();
        helper.assertTrue(config.energyPerNaquadah() == 50000, "Naquadah gives " + config.energyPerNaquadah());
        helper.assertTrue(config.interWorldMultiplier() == 3.5, "The eighth chevron is "
                + config.interWorldMultiplier());
        helper.assertTrue(config.otherGroupMultiplier() == SGConfig.DEFAULTS.otherGroupMultiplier(),
                "The ninth chevron didn't take its default");
        helper.assertTrue(config.logStargateEvents() && config.toolRange() == 9, "The gates or tools section was lost");
        helper.assertTrue(config.options().zpm().capacity() == 1000, "A ZPM holds "
                + config.options().zpm().capacity());
        helper.assertTrue(config.dialCosts().equals(SGConfig.DEFAULTS.dialCosts()), "The dial costs changed");
        helper.assertTrue(config.fePerGeIn() == 40 && config.fePerGeOut() == 20, "The FE rates read as "
                + config.fePerGeIn() + " in and " + config.fePerGeOut() + " out");
        // The running server's rates are the ones every FE conversion uses
        final SGStargates stargates = SGServer.require().stargates();
        final SGConfig before = stargates.config();
        stargates.setFeRates(40, 20, before.converterRate());
        final double in = FeRate.perGeIn();
        final double out = FeRate.perGeOut();
        stargates.setFeRates(before.fePerGeIn(), before.fePerGeOut(), before.converterRate());
        helper.assertTrue(Math.abs(in - 40) < 1e-9 && Math.abs(out - 20) < 1e-9, "Setting 40 in and 20 out gave "
                + in + " and " + out);
        final SGConfig again = SGConfig.CODEC.parse(JsonOps.INSTANCE,
                SGConfig.CODEC.encodeStart(JsonOps.INSTANCE, config).getOrThrow()).getOrThrow();
        helper.assertTrue(again.equals(config), "The settings didn't read back as they were written");
        helper.succeed();
    }
}
