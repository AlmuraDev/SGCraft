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
import com.almuradev.sgcraft.api.gate.GateState;
import com.almuradev.sgcraft.api.gate.GateTypes;
import com.almuradev.sgcraft.api.gate.IrisState;
import com.almuradev.sgcraft.api.gate.IrisTypes;
import com.almuradev.sgcraft.api.gate.PlacementMode;
import com.almuradev.sgcraft.api.gate.PlacementResult;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.block.SGBlocks;
import com.almuradev.sgcraft.mod.block.entity.BatteryChargerBlockEntity;
import com.almuradev.sgcraft.mod.block.entity.PowerUnitBlockEntity;
import com.almuradev.sgcraft.mod.entity.MalpEntity;
import com.almuradev.sgcraft.mod.entity.SGEntityTypes;
import com.almuradev.sgcraft.mod.gate.GateFrame;
import com.almuradev.sgcraft.mod.gate.SGIris;
import com.almuradev.sgcraft.mod.gate.SGStargates;
import com.almuradev.sgcraft.mod.item.BatteryItem;
import com.almuradev.sgcraft.mod.item.MalpItem;
import com.almuradev.sgcraft.mod.item.MalpTabletItem;
import com.almuradev.sgcraft.mod.item.SGDataComponents;
import com.almuradev.sgcraft.mod.item.SGItems;
import com.almuradev.sgcraft.mod.malp.MalpConfigurator;
import com.almuradev.sgcraft.mod.malp.MalpController;
import com.almuradev.sgcraft.mod.malp.MalpLinks;
import com.almuradev.sgcraft.mod.malp.MalpMenu;
import com.almuradev.sgcraft.mod.malp.MalpOptions;
import com.almuradev.sgcraft.mod.malp.MalpPayloads;
import com.almuradev.sgcraft.mod.malp.MalpRegistry;
import com.almuradev.sgcraft.mod.malp.MalpSettings;
import com.almuradev.sgcraft.mod.malp.MalpSignal;
import com.almuradev.sgcraft.mod.view.RemoteViewPackets;
import com.almuradev.sgcraft.mod.view.RemoteViews;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/**
 * Game tests of the MALP and its batteries: charging, the creative tab's batteries, the console's pairing and signal,
 * a watched MALP's battery, a MALP that needs no power, who may configure a MALP and unpair it from its configurator,
 * a player at a console and damage taking them off it, a second player at a console only watching, the players near a
 * driven console seeing its monitor, a MALP going through a gate to another world, and the MALP tablet: its pairing,
 * battery, signal, sharing a MALP with consoles and other tablets, and its screen closing once it leaves the hand.
 */
final class MalpGameTestsNeoForge {

    // The test box's size across and from front to back
    private static final int BOX_WIDTH = 48;
    private static final int BOX_DEPTH = 16;
    private static final UUID OWNER = UUID.fromString("5a1c0de0-0000-4000-8000-000000000001");

    private MalpGameTestsNeoForge() {
    }

    /** Adds the tests to the list {@link SGGameTestsNeoForge} registers. */
    static void register(final Map<String, BiConsumer<GameTestHelper, BlockPos>> tests) {
        tests.put("battery_charges_in_power_unit", MalpGameTestsNeoForge::powerUnitCharges);
        tests.put("charger_burns_naquadah", MalpGameTestsNeoForge::chargerBurns);
        tests.put("naquadah_tops_up_battery", MalpGameTestsNeoForge::recipeTopsUp);
        tests.put("watched_malp_drains_and_stops", MalpGameTestsNeoForge::watchedDrains);
        tests.put("watched_malp_streams_until_signal_lost", MalpGameTestsNeoForge::streamsUntilSignalLost);
        tests.put("malp_pairs_on_console", MalpGameTestsNeoForge::pairsOnConsole);
        tests.put("malp_signal_direct_within_range", MalpGameTestsNeoForge::directSignal);
        tests.put("malp_signal_relays_through_gates", MalpGameTestsNeoForge::relayedSignal);
        tests.put("malp_keeps_itself_through_gate", MalpGameTestsNeoForge::keepsItself);
        tests.put("malp_drives_on_far_from_anyone", MalpGameTestsNeoForge::drivesFarAway);
        tests.put("malp_keeps_mobs_near_it_going", MalpGameTestsNeoForge::keepsMobsGoing);
        tests.put("malp_runs_the_end_arena", MalpGameTestsNeoForge::runsEndArena);
        tests.put("malp_keeps_mobs_near_it_from_despawning", MalpGameTestsNeoForge::keepsMobsFromDespawning);
        tests.put("malp_lets_mobs_spawn_near_it", MalpGameTestsNeoForge::spawnsNearMalp);
        tests.put("malp_burns_in_lava", MalpGameTestsNeoForge::burnsInLava);
        tests.put("malp_pricked_by_cactus", MalpGameTestsNeoForge::prickedByCactus);
        tests.put("malp_destroyed_by_explosion", MalpGameTestsNeoForge::explodes);
        tests.put("malp_without_power_drives_on_flat_battery", (helper, origin) ->
                MalpGameTestsNeoForge.drivesWithoutPower(helper, MalpGameTestsNeoForge.battery(0)));
        tests.put("malp_without_power_drives_without_battery", (helper, origin) ->
                MalpGameTestsNeoForge.drivesWithoutPower(helper, ItemStack.EMPTY));
        tests.put("malp_configured_by_owner_and_operators", MalpGameTestsNeoForge::configuredByOwner);
        tests.put("malp_unpaired_by_owner_from_configurator", MalpGameTestsNeoForge::unpairedByOwner);
        tests.put("creative_tab_has_full_battery", MalpGameTestsNeoForge::fullBatteryInTab);
        tests.put("malp_console_user_flagged", MalpGameTestsNeoForge::consoleUserFlagged);
        tests.put("malp_console_steps_player_up", (helper, origin) -> MalpGameTestsNeoForge.consoleStepsUp(helper,
                false));
        tests.put("malp_console_blocked_front_stays", (helper, origin) -> MalpGameTestsNeoForge.consoleStepsUp(helper,
                true));
        tests.put("malp_console_damage_interrupts", MalpGameTestsNeoForge::consoleDamageInterrupts);
        tests.put("malp_console_centers_camera", MalpGameTestsNeoForge::consoleCentersCamera);
        tests.put("malp_console_second_player_views_only", MalpGameTestsNeoForge::consoleViewOnly);
        tests.put("malp_console_monitor_spectators_come_and_go", MalpGameTestsNeoForge::consoleSpectators);
        tests.put("malp_console_monitor_spectators_capped", MalpGameTestsNeoForge::consoleSpectatorCap);
        tests.put("malp_tablet_paired_by_owner_and_operators", MalpGameTestsNeoForge::tabletPairs);
        tests.put("malp_tablet_drives_within_range", MalpGameTestsNeoForge::tabletDrives);
        tests.put("malp_tablet_battery_drains", MalpGameTestsNeoForge::tabletDrains);
        tests.put("malp_tablet_signal_follows_player", MalpGameTestsNeoForge::tabletSignalFollows);
        tests.put("malp_tablet_signal_relays_through_gates", MalpGameTestsNeoForge::tabletSignalRelays);
        tests.put("malp_tablet_first_opener_drives", MalpGameTestsNeoForge::tabletFirstDrives);
        tests.put("malp_tablet_session_ends_off_hand", MalpGameTestsNeoForge::tabletLeavesHand);
        tests.put("malp_tablet_battery_menu", MalpGameTestsNeoForge::tabletBatteryMenu);
    }

    // A spot in the test's own box, whose chunks are kept running, on a floor of stone laid across the box. Blocks
    // tick and creatures move only in running chunks, which the far places gates are built at aren't
    private static BlockPos inside(final GameTestHelper helper, final int x, final int z) {
        for (int i = 0; i < MalpGameTestsNeoForge.BOX_WIDTH; i++) {
            for (int k = 0; k < MalpGameTestsNeoForge.BOX_DEPTH; k++) {
                helper.getLevel().setBlock(helper.absolutePos(new BlockPos(i, 0, k)), Blocks.STONE.defaultBlockState(),
                        Block.UPDATE_ALL);
            }
        }
        return helper.absolutePos(new BlockPos(x, 1, z));
    }

    private static ItemStack battery(final double energy) {
        final ItemStack battery = new ItemStack(SGItems.BATTERY);
        BatteryItem.setEnergy(battery, energy);
        return battery;
    }

    private static MalpEntity malp(final ServerLevel level, final Vec3 at, final double energy,
            final GlobalPos console) {
        final MalpEntity malp = SGEntityTypes.MALP.create(level, EntitySpawnReason.COMMAND);
        malp.snapTo(at.x, at.y, at.z, 0, 0);
        malp.setBattery(MalpGameTestsNeoForge.battery(energy));
        malp.setConsole(console);
        level.addFreshEntity(malp);
        return malp;
    }

    // The power unit fills the battery in its slot from its own store, losing none of the gate energy on the way
    private static void powerUnitCharges(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = MalpGameTestsNeoForge.inside(helper, 4, 4);
        level.setBlock(at, SGBlocks.POWER_UNIT.defaultBlockState(), Block.UPDATE_ALL);
        final PowerUnitBlockEntity unit = (PowerUnitBlockEntity) level.getBlockEntity(at);
        unit.setStored(1_250);
        unit.battery().setItem(0, new ItemStack(SGItems.BATTERY));
        helper.succeedWhen(() -> {
            final double charged = BatteryItem.energy(unit.battery().getItem(0));
            helper.assertTrue(charged >= 250, "The battery holds only " + charged + " GE");
            helper.assertTrue(Math.abs(unit.stored() + charged - 1_250) < 1e-6,
                    "Gate energy was lost or made: the unit holds "
                    + unit.stored() + " and the battery " + charged);
        });
    }

    // The charger burns naquadah into its store and passes it on to its battery
    private static void chargerBurns(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = MalpGameTestsNeoForge.inside(helper, 4, 4);
        level.setBlock(at, SGBlocks.BATTERY_CHARGER.defaultBlockState(), Block.UPDATE_ALL);
        final BatteryChargerBlockEntity charger = (BatteryChargerBlockEntity) level.getBlockEntity(at);
        charger.items().setItem(BatteryChargerBlockEntity.FUEL, new ItemStack(SGItems.NAQUADAH, 3));
        charger.items().setItem(BatteryChargerBlockEntity.BATTERY, new ItemStack(SGItems.BATTERY));
        helper.succeedWhen(() -> {
            final double charged = BatteryItem.energy(charger.items().getItem(BatteryChargerBlockEntity.BATTERY));
            helper.assertTrue(charger.items().getItem(BatteryChargerBlockEntity.FUEL).getCount() < 3,
                    "No naquadah was burned");
            helper.assertTrue(charger.stored() > 0, "The burned naquadah gave the charger nothing");
            helper.assertTrue(charged >= 100, "The battery holds only " + charged + " GE");
        });
    }

    // Battery and naquadah in the grid give the battery back with the naquadah's gate energy, but not to a full battery
    // or with anything else in the grid
    private static void recipeTopsUp(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final double perNaquadah = MalpSettings.naquadahEnergy();
        final ItemStack naquadah = new ItemStack(SGItems.NAQUADAH);
        final CraftingInput input = MalpGameTestsNeoForge.grid(MalpGameTestsNeoForge.battery(1000), naquadah, naquadah);
        final ItemStack result = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level)
                .map(recipe -> recipe.value().assemble(input)).orElse(ItemStack.EMPTY);
        helper.assertTrue(result.is(SGItems.BATTERY), "Battery and naquadah made " + result);
        helper.assertTrue(BatteryItem.energy(result) == Math.min(BatteryItem.capacity(), 1000 + 2 * perNaquadah),
                "The battery came out holding " + BatteryItem.energy(result) + " GE");
        final CraftingInput full = MalpGameTestsNeoForge.grid(MalpGameTestsNeoForge.battery(BatteryItem.capacity()),
                naquadah);
        helper.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, full, level).isEmpty(),
                "A full battery was topped up");
        final CraftingInput other = MalpGameTestsNeoForge.grid(MalpGameTestsNeoForge.battery(0), naquadah,
                new ItemStack(Items.IRON_INGOT));
        helper.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, other, level).isEmpty(),
                "A battery was topped up with iron beside the naquadah");
        helper.succeed();
    }

    private static CraftingInput grid(final ItemStack... stacks) {
        final List<ItemStack> items = new ArrayList<>(List.of(stacks));
        while (items.size() < 9) {
            items.add(ItemStack.EMPTY);
        }
        return CraftingInput.of(3, 3, items);
    }

    // A watched MALP drives and uses gate energy until its battery is flat; then its feed shows no power and it won't
    // move
    private static void watchedDrains(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = MalpGameTestsNeoForge.inside(helper, 24, 3);
        level.setBlock(at, SGBlocks.MALP_CONSOLE.defaultBlockState(), Block.UPDATE_ALL);
        final GlobalPos console = GlobalPos.of(level.dimension(), at);
        final MalpEntity malp = MalpGameTestsNeoForge.malp(level, Vec3.atBottomCenterOf(at.south(3)), 3.75, console);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
        MalpLinks.open(player, console);
        helper.assertTrue(MalpLinks.feed(player).orElse(null) == MalpPayloads.Feed.LIVE,
                "The feed isn't live: " + MalpLinks.feed(player));
        final Vec3 start = malp.position();
        helper.onEachTick(() -> MalpLinks.drive(player, new MalpPayloads.Drive(1, 0, 0, 0)));
        final AtomicReference<Vec3> stopped = new AtomicReference<>();
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(malp.energy() == 0, "The battery still holds " + malp.energy() + " GE");
            helper.assertTrue(MalpLinks.feed(player).orElse(null) == MalpPayloads.Feed.NO_POWER,
                    "The feed shows " + MalpLinks.feed(player) + ", not no power");
            helper.assertTrue(malp.position().subtract(start).horizontalDistance() > 0.5, "The MALP never drove");
            stopped.set(malp.position());
        });
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(malp.position().subtract(stopped.get()).horizontalDistance() < 0.01,
                    "The MALP drove on a flat battery");
            MalpLinks.close(player);
            malp.discard();
            helper.succeed();
        });
    }

    // A watched MALP that needs no power has a live feed and drives with a flat battery or none, using no gate energy
    private static void drivesWithoutPower(final GameTestHelper helper, final ItemStack battery) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = MalpGameTestsNeoForge.inside(helper, 24, 3);
        level.setBlock(at, SGBlocks.MALP_CONSOLE.defaultBlockState(), Block.UPDATE_ALL);
        final GlobalPos console = GlobalPos.of(level.dimension(), at);
        final MalpEntity malp = MalpGameTestsNeoForge.malp(level, Vec3.atBottomCenterOf(at.south(3)), 0, console);
        malp.setBattery(battery);
        malp.setOptions(MalpOptions.DEFAULTS.withPowerRequired(false));
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
        MalpLinks.open(player, console);
        helper.assertTrue(MalpLinks.feed(player).orElse(null) == MalpPayloads.Feed.LIVE,
                "The feed isn't live: " + MalpLinks.feed(player));
        helper.assertTrue(!MalpLinks.readout(malp, MalpSignal.between(console, level.dimension(), malp.position(),
                MalpSettings.current().signalRange())).powerRequired(), "The readout says the MALP needs power");
        final Vec3 start = malp.position();
        helper.onEachTick(() -> MalpLinks.drive(player, new MalpPayloads.Drive(1, 0, 0, 0)));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(MalpLinks.feed(player).orElse(null) == MalpPayloads.Feed.LIVE,
                    "The feed shows " + MalpLinks.feed(player) + ", not live");
            helper.assertTrue(malp.powered(), "The MALP says it has no power");
            helper.assertTrue(malp.position().subtract(start).horizontalDistance() > 1, "The MALP never drove");
            helper.assertTrue(malp.energy() == 0, "The battery holds " + malp.energy() + " GE");
            MalpLinks.close(player);
            malp.discard();
            helper.succeed();
        });
    }

    // A MALP belongs to the player who put it down, even once packed up and put down by another; only its owner and
    // operators change its settings, which it keeps when packed up, and the command changes them too
    private static void configuredByOwner(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = MalpGameTestsNeoForge.inside(helper, 24, 3);
        final ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        final ServerPlayer other = helper.makeMockServerPlayerInLevel();
        final ServerPlayer operator = helper.makeMockServerPlayerInLevel();
        for (final ServerPlayer player : List.of(owner, other, operator)) {
            player.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
        }
        final MalpEntity malp = MalpGameTestsNeoForge.place(helper, owner, new ItemStack(SGItems.MALP), at.below());
        helper.assertTrue(owner.getUUID().equals(malp.owner()), "The MALP isn't the placing player's");
        final MalpOptions off = MalpOptions.DEFAULTS.withPowerRequired(false);
        MalpConfigurator.open(other, malp);
        MalpConfigurator.apply(other, new MalpPayloads.ConfigApply(malp.getUUID(), false, off));
        helper.assertTrue(malp.options().powerRequired(), "Another player changed the MALP's settings");
        MalpConfigurator.apply(owner, new MalpPayloads.ConfigApply(malp.getUUID(), false, off));
        helper.assertTrue(!malp.options().powerRequired(), "The owner couldn't change the MALP's settings");
        level.getServer().getPlayerList().op(operator.nameAndId(), Optional.of(LevelBasedPermissionSet.GAMEMASTER),
                Optional.empty());
        try {
            MalpConfigurator.apply(operator, new MalpPayloads.ConfigApply(malp.getUUID(), false,
                    MalpOptions.DEFAULTS));
            helper.assertTrue(malp.options().powerRequired(), "An operator couldn't change the MALP's settings");
        } finally {
            level.getServer().getPlayerList().deop(operator.nameAndId());
        }
        MalpConfigurator.apply(owner, new MalpPayloads.ConfigApply(malp.getUUID(), false, off));
        // Packed up and put down by another player, it keeps its owner and its settings
        final ItemStack packed = MalpItem.of(malp);
        malp.discard();
        helper.assertTrue(owner.getUUID().equals(packed.get(SGDataComponents.MALP_OWNER)), "The item lost the owner");
        final MalpEntity again = MalpGameTestsNeoForge.place(helper, other, packed, at.below().east(3));
        helper.assertTrue(owner.getUUID().equals(again.owner()), "The MALP changed hands when put down again");
        helper.assertTrue(!again.options().powerRequired(), "The MALP lost its settings when packed up");
        final MalpRegistry.Entry entry = MalpRegistry.get(level.getServer()).get(again.getUUID()).orElseThrow();
        helper.assertTrue(entry.owner().equals(Optional.of(owner.getUUID())) && !entry.options().powerRequired(),
                "The registry doesn't have the MALP's owner and settings");
        level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack()
                .withLevel(level).withSuppressedOutput(), "sgcraft malp power_required "
                + again.getUUID().toString().substring(0, 8) + " true");
        helper.assertTrue(again.options().powerRequired(), "The command didn't change the MALP's settings");
        again.discard();
        helper.succeed();
    }

    // Only its owner unpairs a MALP from its configurator; then neither it nor the registry has a console, and the
    // feed of the player driving it at the console it was paired with drops as when the signal does
    private static void unpairedByOwner(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = MalpGameTestsNeoForge.inside(helper, 24, 3);
        level.setBlock(at, SGBlocks.MALP_CONSOLE.defaultBlockState(), Block.UPDATE_ALL);
        final GlobalPos console = GlobalPos.of(level.dimension(), at);
        final MalpEntity malp = MalpGameTestsNeoForge.malp(level, Vec3.atBottomCenterOf(at.south(3)),
                BatteryItem.capacity(),
                console);
        final ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        final ServerPlayer other = helper.makeMockServerPlayerInLevel();
        final ServerPlayer driver = helper.makeMockServerPlayerInLevel();
        for (final ServerPlayer player : List.of(owner, other, driver)) {
            player.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
        }
        malp.setOwner(owner.getUUID());
        final List<CustomPacketPayload> packets = new ArrayList<>();
        RemoteViews.redirect(driver, packets::add);
        MalpLinks.open(driver, console);
        helper.assertTrue(MalpLinks.feed(driver).orElse(null) == MalpPayloads.Feed.LIVE,
                "The feed isn't live: " + MalpLinks.feed(driver));
        helper.assertTrue(RemoteViews.watching(driver, malp), "The driver isn't watching the MALP");
        MalpConfigurator.unpair(other, new MalpPayloads.ConfigUnpair(malp.getUUID()));
        helper.assertTrue(console.equals(malp.console()), "Another player unpaired the MALP");
        helper.assertTrue(MalpLinks.feed(driver).orElse(null) == MalpPayloads.Feed.LIVE,
                "The feed dropped when another player tried to unpair the MALP");
        MalpConfigurator.unpair(owner, new MalpPayloads.ConfigUnpair(malp.getUUID()));
        helper.assertTrue(malp.console() == null, "The owner couldn't unpair the MALP");
        final MalpRegistry registry = MalpRegistry.get(level.getServer());
        helper.assertTrue(registry.get(malp.getUUID()).orElseThrow().console().isEmpty(),
                "The registry still has the MALP's console");
        helper.assertTrue(registry.pairedWith(console).isEmpty(), "The console still lists the MALP");
        helper.assertTrue(MalpLinks.feed(driver).orElse(null) == MalpPayloads.Feed.NO_SIGNAL,
                "The feed shows " + MalpLinks.feed(driver) + ", not no signal");
        helper.assertTrue(!RemoteViews.watching(driver, malp), "The view went on after the MALP was unpaired");
        helper.assertTrue(packets.getLast() instanceof RemoteViewPackets.End, "The view wasn't ended");
        MalpLinks.close(driver);
        RemoteViews.unredirect(driver);
        malp.discard();
        helper.succeed();
    }

    // Puts a MALP down from an item, as a player using it on the top of a block
    private static MalpEntity place(final GameTestHelper helper, final ServerPlayer player, final ItemStack item,
            final BlockPos ground) {
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        item.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(
                Vec3.atCenterOf(ground).add(0, 0.5, 0), Direction.UP, ground, false)));
        final List<MalpEntity> placed = helper.getLevel().getEntitiesOfClass(MalpEntity.class,
                new AABB(ground.above()).inflate(0.5));
        helper.assertTrue(placed.size() == 1, placed.size() + " MALPs were put down");
        return placed.getFirst();
    }

    // The creative tab has a full battery right after the empty one
    private static void fullBatteryInTab(final GameTestHelper helper, final BlockPos origin) {
        final List<ItemStack> tab = SGItems.tabItems();
        int empty = -1;
        for (int i = 0; i < tab.size(); i++) {
            if (tab.get(i).is(SGItems.BATTERY) && BatteryItem.energy(tab.get(i)) == 0) {
                empty = i;
                break;
            }
        }
        helper.assertTrue(empty >= 0, "The tab has no empty battery");
        helper.assertTrue(empty + 1 < tab.size() && tab.get(empty + 1).is(SGItems.BATTERY)
                && BatteryItem.energy(tab.get(empty + 1)) == BatteryItem.capacity(),
                "The empty battery isn't followed by a full one");
        helper.succeed();
    }

    // A watched MALP with a signal and power is streamed to the player at its console; once it goes out of the
    // console's range, the feed shows no signal and the stream stops
    private static void streamsUntilSignalLost(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = MalpGameTestsNeoForge.inside(helper, 24, 3);
        level.setBlock(at, SGBlocks.MALP_CONSOLE.defaultBlockState(), Block.UPDATE_ALL);
        final GlobalPos console = GlobalPos.of(level.dimension(), at);
        final MalpEntity malp = MalpGameTestsNeoForge.malp(level, Vec3.atBottomCenterOf(at.south(3)),
                BatteryItem.capacity(),
                console);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
        final List<CustomPacketPayload> packets = new ArrayList<>();
        RemoteViews.redirect(player, packets::add);
        MalpLinks.open(player, console);
        helper.assertTrue(MalpLinks.feed(player).orElse(null) == MalpPayloads.Feed.LIVE,
                "The feed isn't live: " + MalpLinks.feed(player));
        helper.assertTrue(RemoteViews.watching(player, malp), "The player isn't watching the MALP");
        helper.assertTrue(!packets.isEmpty() && packets.getFirst() instanceof RemoteViewPackets.Begin,
                "The view didn't begin");
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(RemoteViews.watching(player, malp), "The view ended with the signal still good");
            malp.teleportTo(malp.getX(), malp.getY(), malp.getZ() + MalpSettings.current().signalRange() + 32);
        });
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(MalpLinks.feed(player).orElse(null) == MalpPayloads.Feed.NO_SIGNAL,
                    "The feed shows " + MalpLinks.feed(player) + ", not no signal");
            helper.assertTrue(!RemoteViews.watching(player, malp), "The view went on with no signal");
            helper.assertTrue(packets.getLast() instanceof RemoteViewPackets.End, "The view wasn't ended");
            MalpLinks.close(player);
            RemoteViews.unredirect(player);
            malp.discard();
            helper.succeed();
        });
    }

    // A player who opens a console turns to face it, looking a little down, and is at it until they close its screen
    private static void consoleUserFlagged(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = MalpGameTestsNeoForge.inside(helper, 24, 3);
        level.setBlock(at, SGBlocks.MALP_CONSOLE.defaultBlockState(), Block.UPDATE_ALL);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
        player.setYRot(180);
        helper.assertTrue(!MalpLinks.atConsole(player), "The player is at a console before opening one");
        MalpLinks.open(player, GlobalPos.of(level.dimension(), at));
        helper.assertTrue(MalpLinks.atConsole(player), "The player isn't at the console they opened");
        // The console is to the south, which a yaw of 0 faces
        helper.assertTrue(Math.abs(Mth.wrapDegrees(player.getYRot())) < 1, "The player faces " + player.getYRot()
                + ", not the console");
        helper.assertTrue(Math.abs(player.getXRot() - MalpLinks.CONSOLE_TILT) < 1, "The player looks "
                + player.getXRot() + " down, not at the console's controls");
        MalpLinks.action(player, MalpPayloads.ConsoleAction.of(MalpPayloads.ConsoleAction.Kind.CLOSE));
        helper.assertTrue(!MalpLinks.atConsole(player), "The player is still at the console after closing it");
        helper.assertTrue(MalpLinks.feed(player).isEmpty(), "The closed console still has a feed");
        helper.succeed();
    }

    // A player who opens a console from two blocks away steps up to its front, facing it, unless something stands in
    // the way there, which leaves them where they were, still turned to the console
    private static void consoleStepsUp(final GameTestHelper helper, final boolean blocked) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = MalpGameTestsNeoForge.inside(helper, 24, 3);
        level.setBlock(at, SGBlocks.MALP_CONSOLE.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
                Direction.EAST), Block.UPDATE_ALL);
        if (blocked) {
            level.setBlock(at.east().above(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        }
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final Vec3 start = new Vec3(at.getX() + 3, at.getY(), at.getZ() + 1.5);
        player.teleportTo(start.x, start.y, start.z);
        player.setYRot(0);
        MalpLinks.open(player, GlobalPos.of(level.dimension(), at));
        final Vec3 front = Vec3.atBottomCenterOf(at).add(0.85, 0, 0);
        final Vec3 expected = blocked ? start : front;
        helper.assertTrue(player.position().distanceTo(expected) < 0.01, "The player stands at " + player.position()
                + ", not " + expected);
        // The console is to the west, which a yaw of 90 faces, or a little south of west from where they started
        final float yaw = blocked ? (float) Math.toDegrees(Math.atan2(1, 2.5)) + 90 : 90;
        helper.assertTrue(Math.abs(Mth.wrapDegrees(player.getYRot() - yaw)) < 1, "The player faces "
                + player.getYRot() + ", not the console");
        helper.assertTrue(Math.abs(player.getXRot() - MalpLinks.CONSOLE_TILT) < 1, "The player looks "
                + player.getXRot() + " down, not at the console's controls");
        MalpLinks.close(player);
        helper.succeed();
    }

    // Damage takes a player watching a MALP off their console, ending its feed, unless the setting is off
    private static void consoleDamageInterrupts(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = MalpGameTestsNeoForge.inside(helper, 24, 3);
        level.setBlock(at, SGBlocks.MALP_CONSOLE.defaultBlockState(), Block.UPDATE_ALL);
        final GlobalPos console = GlobalPos.of(level.dimension(), at);
        final MalpEntity malp = MalpGameTestsNeoForge.malp(level, Vec3.atBottomCenterOf(at.south(3)),
                BatteryItem.capacity(),
                console);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
        // A mock player's client never says it has loaded, and players are safe from damage until it does
        player.connection.markClientLoaded();
        final List<CustomPacketPayload> packets = new ArrayList<>();
        RemoteViews.redirect(player, packets::add);
        MalpLinks.open(player, console);
        helper.assertTrue(RemoteViews.watching(player, malp), "The player isn't watching the MALP");
        // The setting is turned off and back on within the tick, so no other test sees it off
        final SGStargates stargates = SGServer.require().stargates();
        stargates.setConsoleDamageInterrupts(false);
        final boolean hurt = player.hurtServer(level, level.damageSources().fellOutOfWorld(), 1);
        stargates.setConsoleDamageInterrupts(true);
        helper.assertTrue(hurt, "The player took no damage");
        helper.assertTrue(MalpLinks.atConsole(player), "Damage took the player off the console with the setting off");
        helper.assertTrue(RemoteViews.watching(player, malp), "Damage ended the feed with the setting off");
        helper.runAfterDelay(10, () -> {
            // More than the last hit, which a player still recovering from it takes the difference of
            helper.assertTrue(player.hurtServer(level, level.damageSources().fellOutOfWorld(), 2),
                    "The player took no damage");
            helper.assertTrue(!MalpLinks.atConsole(player), "Damage didn't take the player off the console");
            helper.assertTrue(MalpLinks.feed(player).isEmpty(), "The console still has a feed after damage");
            helper.assertTrue(!RemoteViews.watching(player, malp), "The player still watches the MALP after damage");
            helper.assertTrue(packets.getLast() instanceof RemoteViewPackets.End, "The view wasn't ended");
            RemoteViews.unredirect(player);
            malp.discard();
            helper.succeed();
        });
    }

    // The console's center button, and the command, turn a watched MALP's camera back to face the way it drives
    private static void consoleCentersCamera(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = MalpGameTestsNeoForge.inside(helper, 24, 3);
        level.setBlock(at, SGBlocks.MALP_CONSOLE.defaultBlockState(), Block.UPDATE_ALL);
        final GlobalPos console = GlobalPos.of(level.dimension(), at);
        final MalpEntity malp = MalpGameTestsNeoForge.malp(level, Vec3.atBottomCenterOf(at.south(3)),
                BatteryItem.capacity(),
                console);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
        final List<CustomPacketPayload> packets = new ArrayList<>();
        RemoteViews.redirect(player, packets::add);
        MalpLinks.open(player, console);
        helper.assertTrue(MalpLinks.feed(player).orElse(null) == MalpPayloads.Feed.LIVE,
                "The feed isn't live: " + MalpLinks.feed(player));
        MalpLinks.drive(player, new MalpPayloads.Drive(0, 0, 20, 10));
        helper.assertTrue(malp.mastYaw() != 0 && malp.mastPitch() != 0, "The camera didn't turn");
        MalpLinks.action(player, MalpPayloads.ConsoleAction.of(MalpPayloads.ConsoleAction.Kind.CENTER));
        helper.assertTrue(malp.mastYaw() == 0 && malp.mastPitch() == 0, "The camera pans " + malp.mastYaw()
                + " and tilts " + malp.mastPitch() + " after centering");
        MalpLinks.drive(player, new MalpPayloads.Drive(0, 0, -15, 5));
        level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack()
                .withLevel(level).withSuppressedOutput(), "sgcraft malp center "
                + malp.getUUID().toString().substring(0, 8));
        helper.assertTrue(malp.mastYaw() == 0 && malp.mastPitch() == 0, "The command didn't center the camera");
        MalpLinks.close(player);
        RemoteViews.unredirect(player);
        malp.discard();
        helper.succeed();
    }

    // A console at a spot, and the player who will drive at it, at its front
    private static ServerPlayer driverAt(final GameTestHelper helper, final BlockPos at) {
        helper.getLevel().setBlock(at, SGBlocks.MALP_CONSOLE.defaultBlockState(), Block.UPDATE_ALL);
        final ServerPlayer driver = helper.makeMockServerPlayerInLevel();
        driver.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
        return driver;
    }

    // A second player who opens a console someone is driving at only watches: they stay where they are, the server
    // takes none of their controls or buttons, they see the driver's feed while the driver drives, and it goes offline
    // when the driver leaves; watching uses none of the MALP's gate energy beyond the driver's feed. Opening the
    // console again, now free, makes them its driver
    private static void consoleViewOnly(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = MalpGameTestsNeoForge.inside(helper, 24, 3);
        final ServerPlayer driver = MalpGameTestsNeoForge.driverAt(helper, at);
        final GlobalPos console = GlobalPos.of(level.dimension(), at);
        final MalpEntity malp = MalpGameTestsNeoForge.malp(level, Vec3.atBottomCenterOf(at.south(3)),
                BatteryItem.capacity(),
                console);
        final ServerPlayer watcher = helper.makeMockServerPlayerInLevel();
        final Vec3 stand = new Vec3(at.getX() + 2.5, at.getY(), at.getZ() - 1.5);
        watcher.teleportTo(stand.x, stand.y, stand.z);
        RemoteViews.redirect(watcher, packet -> {
        });
        MalpLinks.open(driver, console);
        MalpLinks.open(watcher, console);
        helper.assertTrue(MalpLinks.driving(driver) && !MalpLinks.viewOnly(driver), "The first player isn't driving");
        helper.assertTrue(MalpLinks.viewOnly(watcher) && !MalpLinks.driving(watcher),
                "The second player isn't only watching");
        helper.assertTrue(watcher.position().distanceTo(stand) < 0.01, "The watcher was stepped up to the console");
        helper.assertTrue(MalpLinks.feed(watcher).orElse(null) == MalpPayloads.Feed.LIVE,
                "The watcher's feed isn't live: " + MalpLinks.feed(watcher));
        helper.assertTrue(RemoteViews.watching(watcher, malp), "The watcher doesn't see the MALP's feed");
        final boolean lamp = malp.lamp();
        final float yaw = malp.mastYaw();
        final float pitch = malp.mastPitch();
        for (final MalpPayloads.ConsoleAction.Kind kind : List.of(MalpPayloads.ConsoleAction.Kind.LAMP,
                MalpPayloads.ConsoleAction.Kind.FEED, MalpPayloads.ConsoleAction.Kind.CENTER)) {
            MalpLinks.action(watcher, MalpPayloads.ConsoleAction.of(kind));
        }
        MalpLinks.action(watcher, new MalpPayloads.ConsoleAction(MalpPayloads.ConsoleAction.Kind.SELECT,
                Optional.of(UUID.randomUUID())));
        MalpLinks.drive(watcher, new MalpPayloads.Drive(0, 0, 20, 10));
        helper.assertTrue(malp.lamp() == lamp, "The watcher worked the lamp");
        helper.assertTrue(MalpLinks.feed(driver).orElse(null) == MalpPayloads.Feed.LIVE,
                "The watcher powered down the feed");
        helper.assertTrue(malp.mastYaw() == yaw && malp.mastPitch() == pitch, "The watcher turned the camera");
        helper.assertTrue(MalpLinks.viewOnly(watcher), "The watcher's buttons took them off the console");
        final Vec3 start = malp.position();
        helper.onEachTick(() -> {
            if (MalpLinks.viewOnly(watcher)) {
                MalpLinks.drive(watcher, new MalpPayloads.Drive(1, 0, 0, 0));
            }
        });
        final AtomicReference<Double> before = new AtomicReference<>();
        helper.runAfterDelay(5, () -> before.set(malp.energy()));
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(malp.position().subtract(start).horizontalDistance() < 0.01,
                    "The watcher drove the MALP");
            final double used = before.get() - malp.energy();
            final double idle = MalpSettings.current().idleDrain();
            helper.assertTrue(used > 0 && used <= 21 * idle, "The MALP used " + used + " GE in 20 ticks, not "
                    + 20 * idle + " for one feed");
            helper.assertTrue(RemoteViews.watching(watcher, malp),
                    "The watcher's feed ended while the driver drives");
            MalpLinks.close(driver);
            helper.assertTrue(MalpLinks.feed(watcher).orElse(null) == MalpPayloads.Feed.OFF,
                    "The watcher's feed shows " + MalpLinks.feed(watcher) + " once the driver left, not offline");
            helper.assertTrue(MalpLinks.viewOnly(watcher), "The watcher stopped being view only when the driver left");
            helper.assertTrue(!RemoteViews.watching(watcher, malp), "The watcher's feed outlived the driver");
            MalpLinks.open(watcher, console);
            helper.assertTrue(MalpLinks.driving(watcher), "Opening the free console didn't make the watcher drive");
            MalpLinks.close(watcher);
            RemoteViews.unredirect(watcher);
            malp.discard();
            helper.succeed();
        });
    }

    // A player near a console someone drives at, who isn't at it, sees the feed on its monitor while the feed is
    // live, loses it when they walk out of range and gets it back when they return, and loses it when the driver stops
    private static void consoleSpectators(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = MalpGameTestsNeoForge.inside(helper, 24, 3);
        final ServerPlayer driver = MalpGameTestsNeoForge.driverAt(helper, at);
        final GlobalPos console = GlobalPos.of(level.dimension(), at);
        final MalpEntity malp = MalpGameTestsNeoForge.malp(level, Vec3.atBottomCenterOf(at.south(3)),
                BatteryItem.capacity(),
                console);
        final ServerPlayer near = helper.makeMockServerPlayerInLevel();
        near.teleportTo(at.getX() + 6.5, at.getY(), at.getZ() + 0.5);
        RemoteViews.redirect(near, packet -> {
        });
        MalpLinks.open(driver, console);
        helper.assertTrue(RemoteViews.watchingOnDisplay(near, at, malp), "The nearby player doesn't see the monitor");
        helper.assertTrue(MalpLinks.spectators(console).contains(near.getUUID()),
                "The console doesn't list the nearby player");
        helper.assertTrue(!RemoteViews.watching(near, malp), "The monitor's feed took the place of the player's own");
        helper.assertTrue(!RemoteViews.displays(driver).contains(at), "The driver sees their own console's monitor");
        near.teleportTo(at.getX() + 0.5 - 30, at.getY(), at.getZ() + 0.5);
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(!RemoteViews.watchingOnDisplay(near, at, malp),
                    "The monitor's feed went on with the player out of range");
            near.teleportTo(at.getX() + 6.5, at.getY(), at.getZ() + 0.5);
        });
        helper.runAfterDelay(24, () -> {
            helper.assertTrue(RemoteViews.watchingOnDisplay(near, at, malp),
                    "The monitor's feed didn't come back with the player");
            MalpLinks.close(driver);
            helper.assertTrue(!RemoteViews.watchingOnDisplay(near, at, malp),
                    "The monitor's feed went on after the driver left");
            helper.assertTrue(MalpLinks.spectators(console).isEmpty(), "The console still lists spectators");
            RemoteViews.unredirect(near);
            malp.discard();
            helper.succeed();
        });
    }

    // Only the nearest players around a driven console, as many as the setting says, see its monitor, and the command
    // changes how many
    private static void consoleSpectatorCap(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = MalpGameTestsNeoForge.inside(helper, 24, 3);
        final ServerPlayer driver = MalpGameTestsNeoForge.driverAt(helper, at);
        final GlobalPos console = GlobalPos.of(level.dimension(), at);
        final MalpEntity malp = MalpGameTestsNeoForge.malp(level, Vec3.atBottomCenterOf(at.south(3)),
                BatteryItem.capacity(),
                console);
        // Nearest first, on alternate sides of the console
        final List<ServerPlayer> crowd = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            final ServerPlayer player = helper.makeMockServerPlayerInLevel();
            player.teleportTo(at.getX() + 0.5 + (i % 2 == 0 ? 1 : -1) * (2 + i), at.getY(), at.getZ() + 0.5);
            RemoteViews.redirect(player, packet -> {
            });
            crowd.add(player);
        }
        final int cap = MalpSettings.current().consoleSpectators();
        helper.assertTrue(cap > 2 && cap < crowd.size(), "The test needs a cap between 3 and " + (crowd.size() - 1)
                + ", not " + cap);
        MalpLinks.open(driver, console);
        MalpGameTestsNeoForge.assertSpectators(helper, console, crowd, malp, cap);
        // The command's change is undone within the tick, so no other test sees it
        try {
            MalpGameTestsNeoForge.command(level, "sgcraft malp console_spectators 2");
            helper.assertTrue(MalpSettings.current().consoleSpectators() == 2, "The command didn't change the cap");
            MalpLinks.open(driver, console);
            MalpGameTestsNeoForge.assertSpectators(helper, console, crowd, malp, 2);
        } finally {
            MalpGameTestsNeoForge.command(level, "sgcraft malp console_spectators " + cap);
        }
        MalpLinks.open(driver, console);
        MalpGameTestsNeoForge.assertSpectators(helper, console, crowd, malp, cap);
        MalpLinks.close(driver);
        crowd.forEach(RemoteViews::unredirect);
        malp.discard();
        helper.succeed();
    }

    // The nearest players, as many as the cap, see the console's monitor, and the rest don't
    private static void assertSpectators(final GameTestHelper helper, final GlobalPos console,
            final List<ServerPlayer> crowd, final MalpEntity malp, final int cap) {
        helper.assertTrue(MalpLinks.spectators(console).size() == cap, MalpLinks.spectators(console).size()
                + " players see the monitor, not " + cap);
        for (int i = 0; i < crowd.size(); i++) {
            helper.assertTrue(RemoteViews.watchingOnDisplay(crowd.get(i), console.pos(), malp) == i < cap,
                    "Player " + i + (i < cap ? " doesn't see" : " sees") + " the monitor with a cap of " + cap);
        }
    }

    private static void command(final ServerLevel level, final String command) {
        level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack()
                .withLevel(level).withSuppressedOutput(), command);
    }

    // A MALP item used on a console pairs with it, and the MALP put down from it is paired too
    private static void pairsOnConsole(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = MalpGameTestsNeoForge.inside(helper, 24, 3);
        level.setBlock(at, SGBlocks.MALP_CONSOLE.defaultBlockState(), Block.UPDATE_ALL);
        final GlobalPos console = GlobalPos.of(level.dimension(), at);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
        final ItemStack item = new ItemStack(SGItems.MALP);
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        level.getBlockState(at).useItemOn(item, level, player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(at), Direction.NORTH, at, false));
        helper.assertTrue(console.equals(item.get(SGDataComponents.MALP_CONSOLE)), "The item wasn't paired");
        final BlockPos ground = at.east(3).below();
        item.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(
                Vec3.atCenterOf(ground).add(0, 0.5, 0), Direction.UP, ground, false)));
        final List<MalpEntity> placed = level.getEntitiesOfClass(MalpEntity.class, new AABB(at).inflate(6));
        helper.assertTrue(placed.size() == 1, placed.size() + " MALPs were put down");
        final MalpEntity malp = placed.getFirst();
        helper.assertTrue(console.equals(malp.console()), "The MALP put down isn't paired");
        helper.assertTrue(MalpRegistry.get(level.getServer()).pairedWith(console).stream()
                .anyMatch(entry -> entry.id().equals(malp.getUUID())), "The console doesn't list the MALP");
        malp.discard();
        helper.succeed();
    }

    // Within range in the same world the console reaches a MALP directly; beyond it, or in another world, not at all.
    // Far away is to the south, away from the other tests' gates, so none of them can relay the signal
    private static void directSignal(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final GlobalPos console = GlobalPos.of(level.dimension(), origin);
        final int range = MalpSettings.current().signalRange();
        final Vec3 near = Vec3.atCenterOf(origin).add(10, 0, 0);
        final Vec3 far = Vec3.atCenterOf(origin).add(0, 0, range + 50);
        helper.assertTrue(MalpSignal.between(console, level.dimension(), near, range).kind()
                == MalpSignal.Kind.DIRECT, "The console doesn't reach a MALP ten blocks away");
        helper.assertTrue(!MalpSignal.between(console, level.dimension(), far, range).reaches(),
                "The console reaches a MALP beyond its range");
        helper.assertTrue(!MalpSignal.between(console, Level.NETHER, near, range).reaches(),
                "The console reaches a MALP in another world with no gate open");
        helper.succeed();
    }

    // With too short a range to reach directly, the signal goes through an open wormhole, even past a shut iris,
    // and is lost when the wormhole shuts
    private static void relayedSignal(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final Stargate a = SGGameTestsNeoForge.place(helper, origin, 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.east(128), 9);
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.OPENING_DAMAGE, false, SGGameTestsNeoForge.server(helper));
        b.set(GateProperties.IRIS, IrisTypes.METAL, SGGameTestsNeoForge.server(helper));
        b.iris().orElseThrow().close(SGGameTestsNeoForge.server(helper));
        final GlobalPos console = GlobalPos.of(level.dimension(), a.position().pos().north(5));
        final Vec3 malp = Vec3.atCenterOf(b.position().pos().north(5));
        final int range = 30;
        helper.assertTrue(!MalpSignal.between(console, level.dimension(), malp, range).reaches(),
                "The console reaches the MALP before any gate is open");
        final AtomicReference<DialResult> result = new AtomicReference<>();
        a.dial(SGGameTestsNeoForge.addressOf(b, a), DialMode.QUICK, SGGameTestsNeoForge.server(helper))
                .thenAccept(result::set);
        helper.runAfterDelay(SGIris.MOVE_TICKS + 40, () -> {
            helper.assertTrue(result.get() == DialResult.CONNECTED, "The dial ended " + result.get());
            helper.assertTrue(b.iris().orElseThrow().state() == IrisState.CLOSED, "Gate B's iris isn't shut");
            final MalpSignal signal = MalpSignal.between(console, level.dimension(), malp, range);
            helper.assertTrue(signal.kind() == MalpSignal.Kind.RELAYED, "The signal is " + signal.kind());
            helper.assertTrue(signal.near().equals(Optional.of(a.position()))
                    && signal.far().equals(Optional.of(b.position())), "The signal went the wrong way");
            a.disconnect(SGGameTestsNeoForge.server(helper));
        });
        helper.runAfterDelay(SGIris.MOVE_TICKS + 41, () -> helper.succeedWhen(() -> {
            helper.assertTrue(a.state() != GateState.CONNECTED, "The gate is still connected");
            helper.assertTrue(!MalpSignal.between(console, level.dimension(), malp, range).reaches(),
                    "The signal outlived the wormhole");
        }));
    }

    // A watched MALP in another world, far from any player or forced chunk, keeps ticking and drives on across
    // several chunks, its ticket following it
    private static void drivesFarAway(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        if (nether == null) {
            throw helper.assertionException(Component.literal("There is no Nether"));
        }
        // A tunnel of stone running east from the middle of a chunk, far from where the other tests build
        final int x0 = SectionPos.blockToSectionCoord(origin.getX() + 4096) * 16 + 8;
        final int z0 = SectionPos.blockToSectionCoord(origin.getZ()) * 16 + 8;
        final int y0 = 100;
        final int length = 3 * 16;
        for (int x = x0 - 4; x <= x0 + length + 8; x++) {
            for (int z = z0 - 2; z <= z0 + 2; z++) {
                for (int y = y0 - 1; y <= y0 + 3; y++) {
                    final boolean shell = z == z0 - 2 || z == z0 + 2 || y == y0 - 1 || y == y0 + 3;
                    nether.setBlock(new BlockPos(x, y, z), shell ? Blocks.STONE.defaultBlockState()
                            : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        final MalpEntity malp = MalpGameTestsNeoForge.malp(nether, new Vec3(x0 + 0.5, y0, z0 + 0.5),
                BatteryItem.capacity(),
                GlobalPos.of(helper.getLevel().dimension(), origin));
        // A yaw of -90 faces east
        malp.setYRot(-90);
        final MinecraftServer server = nether.getServer();
        helper.onEachTick(() -> {
            malp.drive(1, 0);
            // As a console watching it keeps it wanted
            if (nether.getGameTime() % 20 == 0) {
                MalpLinks.load(server, malp.getUUID());
            }
        });
        MalpLinks.load(server, malp.getUUID());
        helper.succeedWhen(() -> {
            helper.assertTrue(malp.getX() >= x0 + length, "The MALP stopped at " + malp.position());
            malp.discard();
        });
    }

    // A mob that no player has been near for a while stops wandering; a watched MALP near it keeps it going as a
    // player would, far from any player
    private static void keepsMobsGoing(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        if (nether == null) {
            throw helper.assertionException(Component.literal("There is no Nether"));
        }
        final int x0 = SectionPos.blockToSectionCoord(origin.getX() + 8192) * 16 + 8;
        final int z0 = SectionPos.blockToSectionCoord(origin.getZ()) * 16 + 8;
        final int y0 = 100;
        for (int x = x0 - 8; x <= x0 + 8; x++) {
            for (int z = z0 - 8; z <= z0 + 8; z++) {
                nether.setBlock(new BlockPos(x, y0 - 1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                for (int y = y0; y <= y0 + 3; y++) {
                    nether.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        final MalpEntity malp = MalpGameTestsNeoForge.malp(nether, new Vec3(x0 + 0.5, y0, z0 + 0.5),
                BatteryItem.capacity(), GlobalPos.of(helper.getLevel().dimension(), origin));
        final Mob mob = EntityTypes.ZOMBIFIED_PIGLIN.create(nether, EntitySpawnReason.COMMAND);
        mob.snapTo(x0 + 6.5, y0, z0 + 0.5, 0, 0);
        mob.setPersistenceRequired();
        mob.setNoActionTime(500);
        nether.addFreshEntity(mob);
        final MinecraftServer server = nether.getServer();
        helper.onEachTick(() -> {
            if (nether.getGameTime() % 20 == 0) {
                MalpLinks.load(server, malp.getUUID());
            }
        });
        MalpLinks.load(server, malp.getUUID());
        helper.succeedWhen(() -> {
            helper.assertTrue(mob.getNoActionTime() < 100, "The mob near the MALP is idle: "
                    + mob.getNoActionTime());
            mob.discard();
            malp.discard();
        });
    }

    // A mob far past its despawn distance from the only player despawns at once; one near a watched MALP stays, as
    // it would beside a player
    private static void keepsMobsFromDespawning(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = MalpGameTestsNeoForge.inside(helper, 4, 4);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.teleportTo(at.getX() + 0.5, at.getY() + 200, at.getZ() + 0.5);
        final MalpEntity malp = MalpGameTestsNeoForge.malp(level, Vec3.atBottomCenterOf(at), BatteryItem.capacity(),
                GlobalPos.of(level.dimension(), origin));
        final Mob near = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.NATURAL);
        near.snapTo(at.getX() + 3.5, at.getY(), at.getZ() + 0.5, 0, 0);
        level.addFreshEntity(near);
        final Mob alone = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.NATURAL);
        alone.snapTo(at.getX() + 3.5, at.getY(), at.getZ() + 6.5, 0, 0);
        final MinecraftServer server = level.getServer();
        helper.onEachTick(() -> {
            if (level.getGameTime() % 20 == 0) {
                MalpLinks.load(server, malp.getUUID());
            }
        });
        MalpLinks.load(server, malp.getUUID());
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(near.isAlive(), "The zombie by the MALP despawned");
            // Without a watched MALP the same zombie goes at once
            near.discard();
            malp.discard();
            level.addFreshEntity(alone);
        });
        helper.runAfterDelay(80, () -> helper.succeedWhen(() ->
                helper.assertTrue(alone.isRemoved(), "The zombie far from the player stayed")));
    }

    // Mobs spawn only within reach of a player; a watched MALP has them spawn around it too, as vanilla's spawner
    // would around a player standing there, while the only player is far away
    private static void spawnsNearMalp(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final int x0 = SectionPos.blockToSectionCoord(origin.getX() + 2048) * 16 + 8;
        final int z0 = SectionPos.blockToSectionCoord(origin.getZ()) * 16 + 8;
        final int y0 = 100;
        // A shut room of stone in the next chunk east of the MALP's, dark inside, more than 24 blocks from it
        final BlockPos room = new BlockPos(x0 + 26, y0, z0);
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                for (int y = -1; y <= 3; y++) {
                    final boolean shell = Math.abs(x) == 4 || Math.abs(z) == 4 || y == -1 || y == 3;
                    level.setBlock(room.offset(x, y, z), shell ? Blocks.STONE.defaultBlockState()
                            : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        level.setBlock(new BlockPos(x0, y0 - 1, z0), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        final MalpEntity malp = MalpGameTestsNeoForge.malp(level, new Vec3(x0 + 0.5, y0, z0 + 0.5),
                BatteryItem.capacity(), GlobalPos.of(level.dimension(), origin));
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.teleportTo(x0 + 300.5, y0, z0 + 0.5);
        final MinecraftServer server = level.getServer();
        final AtomicReference<Mob> spawned = new AtomicReference<>();
        final AtomicInteger ticks = new AtomicInteger();
        helper.onEachTick(() -> {
            if (level.getGameTime() % 20 == 0) {
                MalpLinks.load(server, malp.getUUID());
            }
            // The room's light settles before anything is asked to spawn in it
            if (ticks.incrementAndGet() > 40 && spawned.get() == null) {
                NaturalSpawner.spawnCategoryForPosition(MobCategory.MONSTER, level, level.getChunkAt(room), room,
                        (type, at, pos, chunk) -> true, (mob, chunk) -> spawned.set(mob));
            }
        });
        MalpLinks.load(server, malp.getUUID());
        helper.succeedWhen(() -> {
            helper.assertTrue(spawned.get() != null, "Nothing spawned near the MALP");
            spawned.get().discard();
            malp.discard();
        });
    }

    // Lava burns a MALP as it would anything, and destroys it
    private static void burnsInLava(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = MalpGameTestsNeoForge.inside(helper, 4, 8);
        level.setBlock(at, Blocks.LAVA.defaultBlockState(), Block.UPDATE_ALL);
        final MalpEntity malp = MalpGameTestsNeoForge.malp(level, Vec3.atBottomCenterOf(at), BatteryItem.capacity(),
                GlobalPos.of(level.dimension(), origin));
        helper.succeedWhen(() -> helper.assertTrue(malp.isRemoved(), "The MALP is still whole in lava"));
    }

    // An explosion beside a MALP destroys it
    private static void explodes(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = MalpGameTestsNeoForge.inside(helper, 12, 8);
        final MalpEntity malp = MalpGameTestsNeoForge.malp(level, Vec3.atBottomCenterOf(at), BatteryItem.capacity(),
                GlobalPos.of(level.dimension(), origin));
        helper.runAfterDelay(5, () -> level.explode(null, malp.getX() + 1.5, malp.getY() + 0.5, malp.getZ(), 4,
                Level.ExplosionInteraction.NONE));
        helper.runAfterDelay(6, () -> helper.succeedWhen(() -> helper.assertTrue(malp.isRemoved(),
                "The MALP survived the explosion")));
    }

    // A cactus pricks a MALP that drives into it
    private static void prickedByCactus(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = MalpGameTestsNeoForge.inside(helper, 4, 12);
        level.setBlock(at.below(), Blocks.SAND.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(at, Blocks.CACTUS.defaultBlockState(), Block.UPDATE_ALL);
        final MalpEntity malp = MalpGameTestsNeoForge.malp(level, Vec3.atBottomCenterOf(at.west()),
                BatteryItem.capacity(), GlobalPos.of(level.dimension(), origin));
        // A yaw of -90 faces east, into the cactus
        malp.setYRot(-90);
        helper.onEachTick(() -> malp.drive(1, 0));
        helper.succeedWhen(() -> {
            helper.assertTrue(malp.getDamage() > 0 || malp.isRemoved(), "The cactus didn't hurt the MALP");
            malp.discard();
        });
    }

    // The dragon's fight runs its island only while a player is on it; a watched MALP on it runs it too, far past
    // the chunks the MALP's own ticket runs
    private static void runsEndArena(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        if (end == null || end.getDragonFight() == null) {
            throw helper.assertionException(Component.literal("There is no End with a dragon fight"));
        }
        final MalpEntity malp = MalpGameTestsNeoForge.malp(end, new Vec3(0.5, 80, 40.5), BatteryItem.capacity(),
                GlobalPos.of(helper.getLevel().dimension(), origin));
        final BlockPos far = new BlockPos(100, 64, -100);
        final MinecraftServer server = end.getServer();
        helper.onEachTick(() -> {
            if (end.getGameTime() % 20 == 0) {
                MalpLinks.load(server, malp.getUUID());
            }
        });
        MalpLinks.load(server, malp.getUUID());
        helper.succeedWhen(() -> {
            helper.assertTrue(end.isPositionEntityTicking(far), "The arena isn't running at " + far);
            malp.discard();
        });
    }

    // A charged tablet, paired with a MALP, in a new player's main hand, the player standing at a spot
    private static ServerPlayer tabletHolder(final GameTestHelper helper, final Vec3 at, final double energy,
            final MalpEntity malp) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.teleportTo(at.x, at.y, at.z);
        player.getInventory().setSelectedSlot(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, MalpGameTestsNeoForge.tablet(energy, malp));
        return player;
    }

    private static ItemStack tablet(final double energy, final MalpEntity malp) {
        final ItemStack tablet = new ItemStack(SGItems.MALP_TABLET);
        MalpTabletItem.setBattery(tablet, MalpGameTestsNeoForge.battery(energy));
        MalpTabletItem.pair(tablet, malp.getUUID());
        return tablet;
    }

    // Only a MALP's owner and operators pair it with a tablet, by using the tablet on it while sneaking; it stays
    // paired with its console, any number of tablets may pair it, and the commands pair and unpair the held tablet
    private static void tabletPairs(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = MalpGameTestsNeoForge.inside(helper, 24, 3);
        level.setBlock(at, SGBlocks.MALP_CONSOLE.defaultBlockState(), Block.UPDATE_ALL);
        final GlobalPos console = GlobalPos.of(level.dimension(), at);
        final MalpEntity malp = MalpGameTestsNeoForge.malp(level, Vec3.atBottomCenterOf(at.south(3)),
                BatteryItem.capacity(),
                console);
        final ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        final ServerPlayer other = helper.makeMockServerPlayerInLevel();
        final ServerPlayer operator = helper.makeMockServerPlayerInLevel();
        malp.setOwner(owner.getUUID());
        final ItemStack owners = new ItemStack(SGItems.MALP_TABLET);
        final ItemStack spare = new ItemStack(SGItems.MALP_TABLET);
        final ItemStack others = new ItemStack(SGItems.MALP_TABLET);
        final ItemStack operators = new ItemStack(SGItems.MALP_TABLET);
        MalpGameTestsNeoForge.useTablet(owner, owners, malp);
        MalpGameTestsNeoForge.useTablet(owner, owners, malp);
        MalpGameTestsNeoForge.useTablet(owner, spare, malp);
        MalpGameTestsNeoForge.useTablet(other, others, malp);
        level.getServer().getPlayerList().op(operator.nameAndId(), Optional.of(LevelBasedPermissionSet.GAMEMASTER),
                Optional.empty());
        try {
            MalpGameTestsNeoForge.useTablet(operator, operators, malp);
        } finally {
            level.getServer().getPlayerList().deop(operator.nameAndId());
        }
        helper.assertTrue(MalpTabletItem.malps(owners).equals(List.of(malp.getUUID())),
                "The owner's tablet lists " + MalpTabletItem.malps(owners));
        helper.assertTrue(MalpTabletItem.malps(spare).contains(malp.getUUID()), "A second tablet didn't pair");
        helper.assertTrue(MalpTabletItem.malps(operators).contains(malp.getUUID()), "An operator couldn't pair");
        helper.assertTrue(MalpTabletItem.malps(others).isEmpty(), "Another player paired the MALP");
        helper.assertTrue(console.equals(malp.console()), "Pairing with a tablet unpaired the console");
        helper.assertTrue(MalpRegistry.get(level.getServer()).pairedWith(console).stream()
                .anyMatch(entry -> entry.id().equals(malp.getUUID())), "The console no longer lists the MALP");
        // The commands work on the tablet in the caller's hand, whoever owns the MALP
        other.setItemInHand(InteractionHand.MAIN_HAND, others);
        final String name = malp.getUUID().toString().substring(0, 8);
        MalpGameTestsNeoForge.command(level, other, "sgcraft malp pair_tablet " + name);
        helper.assertTrue(MalpTabletItem.malps(others).contains(malp.getUUID()), "The command didn't pair");
        MalpGameTestsNeoForge.command(level, other, "sgcraft malp unpair_tablet " + name);
        helper.assertTrue(MalpTabletItem.malps(others).isEmpty(), "The command didn't unpair");
        malp.discard();
        helper.succeed();
    }

    // A player uses a tablet on a MALP while sneaking
    private static void useTablet(final ServerPlayer player, final ItemStack tablet, final MalpEntity malp) {
        player.teleportTo(malp.getX() + 1.5, malp.getY(), malp.getZ());
        player.setItemInHand(InteractionHand.MAIN_HAND, tablet);
        player.setShiftKeyDown(true);
        malp.interact(player, InteractionHand.MAIN_HAND, malp.position());
        player.setShiftKeyDown(false);
    }

    private static void command(final ServerLevel level, final ServerPlayer player, final String command) {
        level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack()
                .withLevel(level).withEntity(player).withSuppressedOutput(), command);
    }

    // A player with a charged tablet paired with a MALP in range drives it, with a live feed and the MALP's view; the
    // MALP's console isn't needed
    private static void tabletDrives(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = MalpGameTestsNeoForge.inside(helper, 24, 3);
        final MalpEntity malp = MalpGameTestsNeoForge.malp(level, Vec3.atBottomCenterOf(at.south(3)),
                BatteryItem.capacity(),
                GlobalPos.of(level.dimension(), at));
        final ServerPlayer player = MalpGameTestsNeoForge.tabletHolder(helper, Vec3.atBottomCenterOf(at),
                BatteryItem.capacity(), malp);
        final List<CustomPacketPayload> packets = new ArrayList<>();
        RemoteViews.redirect(player, packets::add);
        MalpLinks.openTablet(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(MalpLinks.atTablet(player) && MalpLinks.driving(player), "The player isn't driving");
        helper.assertTrue(MalpLinks.selected(player).equals(Optional.of(malp.getUUID())), "The MALP isn't chosen");
        helper.assertTrue(MalpLinks.feed(player).orElse(null) == MalpPayloads.Feed.LIVE,
                "The feed isn't live: " + MalpLinks.feed(player));
        helper.assertTrue(RemoteViews.watching(player, malp), "The player isn't watching the MALP");
        final Vec3 start = malp.position();
        helper.onEachTick(() -> MalpLinks.drive(player, new MalpPayloads.Drive(1, 0, 0, 0)));
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(malp.position().subtract(start).horizontalDistance() > 0.5, "The MALP never drove");
            helper.assertTrue(MalpLinks.feed(player).orElse(null) == MalpPayloads.Feed.LIVE,
                    "The feed shows " + MalpLinks.feed(player) + ", not live");
            MalpLinks.close(player);
            helper.assertTrue(!RemoteViews.watching(player, malp), "The view went on after the screen closed");
            RemoteViews.unredirect(player);
            malp.discard();
            helper.succeed();
        });
    }

    // A tablet uses gate energy from its battery while its feed is live, as much each tick as the setting says, which
    // the command changes; once the battery is flat the feed shows no tablet power, as it does with a flat battery or
    // none from the start
    private static void tabletDrains(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = MalpGameTestsNeoForge.inside(helper, 24, 3);
        final MalpEntity malp = MalpGameTestsNeoForge.malp(level, Vec3.atBottomCenterOf(at.south(3)),
                BatteryItem.capacity(),
                GlobalPos.of(level.dimension(), at));
        final double drain = MalpSettings.current().tabletDrain();
        helper.assertTrue(drain > 0, "The test needs tablets to use gate energy, not " + drain);
        try {
            MalpGameTestsNeoForge.command(level, "sgcraft malp tablet_drain " + (drain + 3));
            helper.assertTrue(MalpSettings.current().tabletDrain() == drain + 3, "The command didn't change the drain");
        } finally {
            MalpGameTestsNeoForge.command(level, "sgcraft malp tablet_drain " + drain);
        }
        final Vec3 stand = Vec3.atBottomCenterOf(at);
        final ServerPlayer steady = MalpGameTestsNeoForge.tabletHolder(helper, stand, 125, malp);
        final ServerPlayer flat = MalpGameTestsNeoForge.tabletHolder(helper, stand, 15 * drain, malp);
        final ServerPlayer empty = MalpGameTestsNeoForge.tabletHolder(helper, stand, 0, malp);
        final ServerPlayer none = MalpGameTestsNeoForge.tabletHolder(helper, stand, 0, malp);
        MalpTabletItem.setBattery(none.getMainHandItem(), ItemStack.EMPTY);
        for (final ServerPlayer player : List.of(steady, flat, empty, none)) {
            MalpLinks.openTablet(player, InteractionHand.MAIN_HAND);
        }
        helper.assertTrue(MalpLinks.feed(steady).orElse(null) == MalpPayloads.Feed.LIVE,
                "The charged tablet's feed isn't live: " + MalpLinks.feed(steady));
        for (final ServerPlayer player : List.of(empty, none)) {
            helper.assertTrue(MalpLinks.feed(player).orElse(null) == MalpPayloads.Feed.NO_TABLET_POWER,
                    "A tablet with no charge shows " + MalpLinks.feed(player));
        }
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(MalpLinks.feed(flat).orElse(null) == MalpPayloads.Feed.NO_TABLET_POWER,
                    "The run down tablet shows " + MalpLinks.feed(flat));
            MalpLinks.close(flat);
            helper.assertTrue(MalpTabletItem.energy(flat.getMainHandItem()) == 0, "The run down tablet still holds "
                    + MalpTabletItem.energy(flat.getMainHandItem()) + " GE");
        });
        helper.runAfterDelay(41, () -> {
            MalpLinks.close(steady);
            final double used = 125 - MalpTabletItem.energy(steady.getMainHandItem());
            helper.assertTrue(used >= 38 * drain - 1e-9 && used <= 43 * drain + 1e-9, "The tablet used " + used
                    + " GE in about 40 ticks, not " + 40 * drain);
            MalpLinks.close(empty);
            MalpLinks.close(none);
            malp.discard();
            helper.succeed();
        });
    }

    // A tablet's signal is measured from the player holding it: the feed drops once they walk out of range, and comes
    // back when they return
    private static void tabletSignalFollows(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = MalpGameTestsNeoForge.inside(helper, 24, 3);
        final MalpEntity malp = MalpGameTestsNeoForge.malp(level, Vec3.atBottomCenterOf(at.south(3)),
                BatteryItem.capacity(),
                GlobalPos.of(level.dimension(), at));
        final Vec3 near = Vec3.atBottomCenterOf(at);
        final ServerPlayer player = MalpGameTestsNeoForge.tabletHolder(helper, near, BatteryItem.capacity(), malp);
        MalpLinks.openTablet(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(MalpLinks.feed(player).orElse(null) == MalpPayloads.Feed.LIVE,
                "The feed isn't live: " + MalpLinks.feed(player));
        // Far to the south, away from every test's gates, so none can relay the signal
        player.teleportTo(near.x, near.y, near.z + MalpSettings.current().signalRange() + 2000);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(MalpLinks.feed(player).orElse(null) == MalpPayloads.Feed.NO_SIGNAL,
                    "The feed shows " + MalpLinks.feed(player) + " out of range, not no signal");
            helper.assertTrue(MalpLinks.atTablet(player), "Walking away closed the tablet");
            player.teleportTo(near.x, near.y, near.z);
        });
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(MalpLinks.feed(player).orElse(null) == MalpPayloads.Feed.LIVE,
                    "The feed shows " + MalpLinks.feed(player) + " back in range, not live");
            MalpLinks.close(player);
            malp.discard();
            helper.succeed();
        });
    }

    // With too short a range to reach directly, a tablet's signal goes through an open wormhole from a gate near the
    // player holding it, and is lost when they walk away from that gate
    private static void tabletSignalRelays(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final Stargate a = SGGameTestsNeoForge.place(helper, origin, 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.east(128), 9);
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.OPENING_DAMAGE, false, SGGameTestsNeoForge.server(helper));
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final Vec3 byGate = Vec3.atBottomCenterOf(a.position().pos().north(5));
        player.teleportTo(byGate.x, byGate.y, byGate.z);
        final ItemStack stack = new ItemStack(SGItems.MALP_TABLET);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        final MalpController tablet = new MalpController.Tablet(InteractionHand.MAIN_HAND, stack, level.dimension());
        final Vec3 malp = Vec3.atCenterOf(b.position().pos().north(5));
        final int range = 30;
        helper.assertTrue(!tablet.signal(player, level.dimension(), malp, range).reaches(),
                "The tablet reaches the MALP before any gate is open");
        final AtomicReference<DialResult> result = new AtomicReference<>();
        a.dial(SGGameTestsNeoForge.addressOf(b, a), DialMode.QUICK, SGGameTestsNeoForge.server(helper))
                .thenAccept(result::set);
        helper.runAfterDelay(SGIris.MOVE_TICKS + 40, () -> {
            helper.assertTrue(result.get() == DialResult.CONNECTED, "The dial ended " + result.get());
            final MalpSignal signal = tablet.signal(player, level.dimension(), malp, range);
            helper.assertTrue(signal.kind() == MalpSignal.Kind.RELAYED, "The signal is " + signal.kind());
            helper.assertTrue(signal.near().equals(Optional.of(a.position()))
                    && signal.far().equals(Optional.of(b.position())), "The signal went the wrong way");
            player.teleportTo(byGate.x, byGate.y, byGate.z - range - 10);
            helper.assertTrue(!tablet.signal(player, level.dimension(), malp, range).reaches(),
                    "The signal reached through a gate the player walked away from");
            a.disconnect(SGGameTestsNeoForge.server(helper));
            helper.succeed();
        });
    }

    // The first to open a MALP drives it, from a tablet or a console; anyone opening it after, from a console or
    // another tablet, only watches the driver's feed and can't drive. When the driver leaves, the watchers' feeds go
    // offline, and the next to open it drives
    private static void tabletFirstDrives(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = MalpGameTestsNeoForge.inside(helper, 24, 3);
        final ServerPlayer atConsole = MalpGameTestsNeoForge.driverAt(helper, at);
        final GlobalPos console = GlobalPos.of(level.dimension(), at);
        final MalpEntity malp = MalpGameTestsNeoForge.malp(level, Vec3.atBottomCenterOf(at.south(3)),
                BatteryItem.capacity(),
                console);
        final Vec3 stand = Vec3.atBottomCenterOf(at.east(3));
        final ServerPlayer first = MalpGameTestsNeoForge.tabletHolder(helper, stand, BatteryItem.capacity(), malp);
        final ServerPlayer second = MalpGameTestsNeoForge.tabletHolder(helper, stand, BatteryItem.capacity(), malp);
        MalpLinks.openTablet(first, InteractionHand.MAIN_HAND);
        MalpLinks.open(atConsole, console);
        MalpLinks.openTablet(second, InteractionHand.MAIN_HAND);
        helper.assertTrue(MalpLinks.driving(first), "The first player isn't driving");
        for (final ServerPlayer watcher : List.of(atConsole, second)) {
            helper.assertTrue(MalpLinks.viewOnly(watcher) && !MalpLinks.driving(watcher),
                    "A later player isn't only watching");
            helper.assertTrue(MalpLinks.selected(watcher).equals(Optional.of(malp.getUUID())),
                    "A watcher doesn't watch the driven MALP");
            helper.assertTrue(MalpLinks.feed(watcher).orElse(null) == MalpPayloads.Feed.LIVE,
                    "A watcher's feed isn't live: " + MalpLinks.feed(watcher));
            MalpLinks.drive(watcher, new MalpPayloads.Drive(0, 0, 20, 10));
            MalpLinks.action(watcher, MalpPayloads.ConsoleAction.of(MalpPayloads.ConsoleAction.Kind.LAMP));
        }
        helper.assertTrue(malp.mastYaw() == 0 && malp.mastPitch() == 0, "A watcher turned the camera");
        helper.assertTrue(!malp.lamp(), "A watcher worked the lamp");
        MalpLinks.close(first);
        for (final ServerPlayer watcher : List.of(atConsole, second)) {
            helper.assertTrue(MalpLinks.feed(watcher).orElse(null) == MalpPayloads.Feed.OFF,
                    "A watcher's feed shows " + MalpLinks.feed(watcher) + " once the driver left, not offline");
        }
        // The console now opens first, so the tablet only watches
        MalpLinks.open(atConsole, console);
        MalpLinks.openTablet(second, InteractionHand.MAIN_HAND);
        helper.assertTrue(MalpLinks.driving(atConsole), "Opening the console again didn't make its player drive");
        helper.assertTrue(MalpLinks.viewOnly(second), "The tablet opened after the console isn't only watching");
        MalpLinks.close(atConsole);
        MalpLinks.close(second);
        malp.discard();
        helper.succeed();
    }

    // The tablet's screen closes once the tablet leaves the hand it was used from, by a change of hotbar slot or by
    // being taken away, and when damage interrupts it
    private static void tabletLeavesHand(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = MalpGameTestsNeoForge.inside(helper, 24, 3);
        final MalpEntity malp = MalpGameTestsNeoForge.malp(level, Vec3.atBottomCenterOf(at.south(3)),
                BatteryItem.capacity(),
                GlobalPos.of(level.dimension(), at));
        final ServerPlayer player = MalpGameTestsNeoForge.tabletHolder(helper, Vec3.atBottomCenterOf(at),
                BatteryItem.capacity(), malp);
        final ItemStack tablet = player.getMainHandItem();
        player.connection.markClientLoaded();
        MalpLinks.openTablet(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(MalpLinks.atTablet(player), "The tablet didn't open");
        player.getInventory().setSelectedSlot(1);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(!MalpLinks.linked(player), "Changing slot didn't close the tablet");
            player.getInventory().setSelectedSlot(0);
            MalpLinks.openTablet(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(MalpLinks.atTablet(player), "The tablet didn't open again");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        });
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(!MalpLinks.linked(player), "Taking the tablet away didn't close it");
            player.setItemInHand(InteractionHand.MAIN_HAND, tablet);
            MalpLinks.openTablet(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(player.hurtServer(level, level.damageSources().fellOutOfWorld(), 1),
                    "The player took no damage");
            helper.assertTrue(!MalpLinks.linked(player), "Damage didn't close the tablet");
            malp.discard();
            helper.succeed();
        });
    }

    // Used while sneaking, a tablet opens its battery slot, which writes the battery back to the tablet; the tablet's
    // own slot can't be clicked while it is open
    private static void tabletBatteryMenu(final GameTestHelper helper, final BlockPos origin) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final ItemStack tablet = new ItemStack(SGItems.MALP_TABLET);
        player.getInventory().setSelectedSlot(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, tablet);
        player.setShiftKeyDown(true);
        tablet.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        player.setShiftKeyDown(false);
        helper.assertTrue(player.containerMenu instanceof MalpMenu, "The battery menu didn't open");
        final MalpMenu menu = (MalpMenu) player.containerMenu;
        helper.assertTrue(menu.data().get(MalpMenu.TABLET) == 1, "The menu isn't a tablet's");
        menu.getSlot(0).set(MalpGameTestsNeoForge.battery(BatteryItem.capacity() / 2));
        helper.assertTrue(MalpTabletItem.energy(tablet) == BatteryItem.capacity() / 2, "The tablet holds "
                + MalpTabletItem.energy(tablet) + " GE after a battery was put in");
        helper.assertTrue(menu.data().get(MalpMenu.BATTERY) == 50, "The menu shows "
                + menu.data().get(MalpMenu.BATTERY) + "%");
        // The hotbar comes last, after the battery slot and the rest of the inventory
        final int hotbar = menu.slots.size() - 9;
        menu.clicked(hotbar, 0, ContainerInput.PICKUP, player);
        menu.clicked(0, 0, ContainerInput.SWAP, player);
        helper.assertTrue(player.getMainHandItem() == tablet && menu.getCarried().isEmpty(),
                "The tablet was moved while its menu was open");
        menu.clicked(0, 0, ContainerInput.QUICK_MOVE, player);
        helper.assertTrue(MalpTabletItem.battery(tablet).isEmpty(), "The battery taken out is still in the tablet");
        player.closeContainer();
        helper.succeed();
    }

    // A MALP driven through a gate to the Nether comes out the same MALP: its UUID, battery, console, name, owner and
    // settings
    private static void keepsItself(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final ServerLevel nether = level.getServer().getLevel(Level.NETHER);
        if (nether == null) {
            throw helper.assertionException(Component.literal("There is no Nether"));
        }
        // Gate B stands in the Nether, its chunks held loaded, with room in front of it
        final BlockPos far = new BlockPos(origin.getX(), 80, origin.getZ());
        final ChunkPos chunk = ChunkPos.containing(far);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                nether.setChunkForced(chunk.x() + x, chunk.z() + z, true);
            }
        }
        final PlacementResult placed = SGCraftApi.get().stargates().place(GatePlacement.builder()
                .position(GlobalPos.of(Level.NETHER, far)).orientation(FrontAndTop.NORTH_UP)
                .type(SGGameTestsNeoForge.type(helper, GateTypes.MILKY_WAY)).chevrons(9).mode(PlacementMode.FORCE)
                .noFixtures().build(), SGGameTestsNeoForge.server(helper));
        helper.assertTrue(placed.status() == PlacementResult.Status.PLACED, "Placing gate B failed: "
                + placed.status());
        final Stargate b = placed.gate().orElseThrow();
        final GateFrame there = new GateFrame(far, Direction.NORTH);
        for (int x = -2; x <= 2; x++) {
            for (int y = 1; y <= 4; y++) {
                for (int z = 1; z <= 4; z++) {
                    nether.setBlock(there.block(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        final Stargate a = SGGameTestsNeoForge.place(helper, origin, 9);
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.OPENING_DAMAGE, false, SGGameTestsNeoForge.server(helper));
        final AtomicReference<DialResult> result = new AtomicReference<>();
        a.dial(SGGameTestsNeoForge.addressOf(b, a), DialMode.QUICK, SGGameTestsNeoForge.server(helper))
                .thenAccept(result::set);
        final GateFrame frame = new GateFrame(a.position().pos(), Direction.NORTH);
        final GlobalPos console = GlobalPos.of(level.dimension(), origin.north(6));
        final AtomicReference<MalpEntity> sent = new AtomicReference<>();
        // Once the wormhole is open, a MALP in front of gate A, then just behind its horizon
        helper.runAfterDelay(SGIris.MOVE_TICKS + 60, () -> {
            helper.assertTrue(result.get() == DialResult.CONNECTED, "The dial ended " + result.get());
            final MalpEntity malp = MalpGameTestsNeoForge.malp(level, frame.toWorld(new Vec3(0, 1, 0.8)), 12_345,
                    console);
            malp.setNoGravity(true);
            malp.setCustomName(Component.literal("Test Probe"));
            malp.setOwner(MalpGameTestsNeoForge.OWNER);
            malp.setOptions(MalpOptions.DEFAULTS.withPowerRequired(false));
            sent.set(malp);
        });
        helper.runAfterDelay(SGIris.MOVE_TICKS + 63, () -> {
            final Vec3 behind = frame.toWorld(new Vec3(0, 1, -0.4));
            sent.get().setPos(behind.x, behind.y, behind.z);
        });
        helper.runAfterDelay(SGIris.MOVE_TICKS + 66, () -> helper.succeedWhen(() -> {
            final UUID id = sent.get().getUUID();
            helper.assertTrue(nether.getEntity(id) instanceof MalpEntity, "The MALP isn't in the Nether");
            final MalpEntity arrived = (MalpEntity) nether.getEntity(id);
            helper.assertTrue(arrived.energy() == 12_345, "Its battery holds " + arrived.energy() + " GE");
            helper.assertTrue(console.equals(arrived.console()), "It lost its console: " + arrived.console());
            helper.assertTrue("Test Probe".equals(arrived.displayName()), "It's called " + arrived.displayName());
            helper.assertTrue(MalpGameTestsNeoForge.OWNER.equals(arrived.owner()), "It lost its owner: "
                    + arrived.owner());
            helper.assertTrue(!arrived.options().powerRequired(), "It lost its settings: " + arrived.options());
            helper.assertTrue(MalpRegistry.get(level.getServer()).get(id).map(entry -> entry.position().dimension())
                    .filter(Level.NETHER::equals).isPresent(), "The registry hasn't seen it in the Nether");
            arrived.discard();
            a.disconnect(SGGameTestsNeoForge.server(helper));
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    nether.setChunkForced(chunk.x() + x, chunk.z() + z, false);
                }
            }
        }));
    }
}
