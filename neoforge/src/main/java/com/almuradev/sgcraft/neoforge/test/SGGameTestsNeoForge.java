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
import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.SGCraftApi;
import com.almuradev.sgcraft.api.SGCraftRegistries;
import com.almuradev.sgcraft.api.access.AddressRule;
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.addressbook.AddressBook;
import com.almuradev.sgcraft.api.addressbook.AddressBookEntry;
import com.almuradev.sgcraft.api.event.EventBus;
import com.almuradev.sgcraft.api.event.gate.ChangeGatePropertyEvent;
import com.almuradev.sgcraft.api.event.gate.ConnectionEvent;
import com.almuradev.sgcraft.api.event.gate.DialEvent;
import com.almuradev.sgcraft.api.event.gate.IrisEvent;
import com.almuradev.sgcraft.api.event.gate.TravelEvent;
import com.almuradev.sgcraft.api.gate.Connection;
import com.almuradev.sgcraft.api.gate.DialMode;
import com.almuradev.sgcraft.api.gate.DialResult;
import com.almuradev.sgcraft.api.gate.FixtureBuffer;
import com.almuradev.sgcraft.api.gate.GatePlacement;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.GateSize;
import com.almuradev.sgcraft.api.gate.GateState;
import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.api.gate.GateTypes;
import com.almuradev.sgcraft.api.gate.IrisState;
import com.almuradev.sgcraft.api.gate.IrisTypes;
import com.almuradev.sgcraft.api.gate.PlacementMode;
import com.almuradev.sgcraft.api.gate.PlacementResult;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.address.SGAddressing;
import com.almuradev.sgcraft.mod.addressbook.DefaultAddresses;
import com.almuradev.sgcraft.mod.addressbook.ItemAddressBook;
import com.almuradev.sgcraft.mod.block.EventHorizonBlock;
import com.almuradev.sgcraft.mod.block.GateSpawnerBlock;
import com.almuradev.sgcraft.mod.block.RingBlock;
import com.almuradev.sgcraft.mod.block.SGBlocks;
import com.almuradev.sgcraft.mod.block.entity.DhdBlockEntity;
import com.almuradev.sgcraft.mod.block.entity.StargateBaseBlockEntity;
import com.almuradev.sgcraft.mod.entity.IrisPlateEntity;
import com.almuradev.sgcraft.mod.gate.ChevronOrder;
import com.almuradev.sgcraft.mod.gate.ChevronSpacing;
import com.almuradev.sgcraft.mod.gate.FluidSpill;
import com.almuradev.sgcraft.mod.gate.GateFormation;
import com.almuradev.sgcraft.mod.gate.GateFrame;
import com.almuradev.sgcraft.mod.gate.GateLight;
import com.almuradev.sgcraft.mod.gate.GateOrientation;
import com.almuradev.sgcraft.mod.gate.GateShape;
import com.almuradev.sgcraft.mod.gate.SGIris;
import com.almuradev.sgcraft.mod.gate.SGStargate;
import com.almuradev.sgcraft.mod.item.SGDataComponents;
import com.almuradev.sgcraft.mod.item.SGItems;
import com.almuradev.sgcraft.mod.tool.TabletGate;
import com.almuradev.sgcraft.mod.tool.ToolActions;
import com.almuradev.sgcraft.mod.tool.ToolPayloads;
import com.almuradev.sgcraft.mod.villager.SGVillagers;
import com.almuradev.sgcraft.mod.worldgen.NaquadahOre;
import com.almuradev.sgcraft.mod.worldgen.NaquadahOreFeature;
import com.almuradev.sgcraft.mod.worldgen.NaquadahOreSettings;
import com.almuradev.sgcraft.mod.worldgen.OreChunks;
import com.almuradev.sgcraft.mod.worldgen.StructureGates;
import com.almuradev.sgcraft.mod.zpm.Zpms;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.PlaceCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Game tests of the API as SGCraft implements it: placing, dialing, energy, the DHD's fuel, the iris, travel,
 * access and events, and address books. Each runs in an empty box wide enough for two gates in different address
 * cells, gate A near one end and gate B near the other, both facing north. Run them with
 * {@code ./gradlew :neoforge:runGameTestServer}.
 */
public final class SGGameTestsNeoForge {

    private static final Identifier STRUCTURE = SGCraft.id("gate_test");
    // Gate A and gate B, from a test's origin: eight chunks apart, so in different address cells
    private static final BlockPos A = BlockPos.ZERO;
    private static final BlockPos B = new BlockPos(128, 0, 0);
    // A third gate, three chunks along the other way, for wormholes that go astray
    private static final BlockPos C = new BlockPos(0, 0, 48);
    // Tests run far out, where gates have no address, so each builds its gates near the world's center instead, high
    // up, a little apart from the others
    private static final int SPACING = 256;
    private static final int HEIGHT = 200;
    private static final Map<String, BiConsumer<GameTestHelper, BlockPos>> TESTS = new LinkedHashMap<>();

    static {
        SGGameTestsNeoForge.TESTS.put("placement", SGGameTestsNeoForge::placement);
        SGGameTestsNeoForge.TESTS.put("dial_needs_energy", SGGameTestsNeoForge::dialNeedsEnergy);
        SGGameTestsNeoForge.TESTS.put("dial_reaches_unloaded_gate", SGGameTestsNeoForge::dialReachesUnloadedGate);
        SGGameTestsNeoForge.TESTS.put("kept_address_dials_from_any_world", SGGameTestsNeoForge::keptAddressDials);
        SGGameTestsNeoForge.TESTS.put("dial_with_stored_energy", SGGameTestsNeoForge::dialWithStoredEnergy);
        SGGameTestsNeoForge.TESTS.put("dhd_fuel", SGGameTestsNeoForge::dhdFuel);
        SGGameTestsNeoForge.TESTS.put("iris_moves", SGGameTestsNeoForge::irisMoves);
        SGGameTestsNeoForge.TESTS.put("dial_canceled_by_event", SGGameTestsNeoForge::dialCanceled);
        SGGameTestsNeoForge.TESTS.put("rights_deny_dial", SGGameTestsNeoForge::rightsDenyDial);
        SGGameTestsNeoForge.TESTS.put("address_rules", SGGameTestsNeoForge::addressRules);
        SGGameTestsNeoForge.TESTS.put("address_book", SGGameTestsNeoForge::addressBook);
        SGGameTestsNeoForge.TESTS.put("travel", SGGameTestsNeoForge::travel);
        SGGameTestsNeoForge.TESTS.put("iris_stops_travel", SGGameTestsNeoForge::irisStopsTravel);
        SGGameTestsNeoForge.TESTS.put("phase_shift_passes_iris", SGGameTestsNeoForge::phaseShift);
        SGGameTestsNeoForge.TESTS.put("iris_can_stop_phase_shift", SGGameTestsNeoForge::phaseShiftStopped);
        SGGameTestsNeoForge.TESTS.put("fixtures", SGGameTestsNeoForge::fixtures);
        SGGameTestsNeoForge.TESTS.put("property_event_changes_value", SGGameTestsNeoForge::propertyEvent);
        SGGameTestsNeoForge.TESTS.put("pdd_book_actions", SGGameTestsNeoForge::pddBook);
        SGGameTestsNeoForge.TESTS.put("configurator_apply", SGGameTestsNeoForge::configuratorApply);
        SGGameTestsNeoForge.TESTS.put("gdo_works_iris", SGGameTestsNeoForge::gdoIris);
        SGGameTestsNeoForge.TESTS.put("camouflage", SGGameTestsNeoForge::camouflage);
        SGGameTestsNeoForge.TESTS.put("supergate_placed", SGGameTestsNeoForge::supergatePlaced);
        SGGameTestsNeoForge.TESTS.put("supergate_built_by_hand", SGGameTestsNeoForge::supergateByHand);
        SGGameTestsNeoForge.TESTS.put("supergate_costs_more", SGGameTestsNeoForge::supergateCostsMore);
        SGGameTestsNeoForge.TESTS.put("spawners_build_gates", SGGameTestsNeoForge::spawners);
        SGGameTestsNeoForge.TESTS.put("spawner_rebuilds_broken_gate", SGGameTestsNeoForge::spawnerRebuilds);
        SGGameTestsNeoForge.TESTS.put("gate_in_water", SGGameTestsNeoForge::gateInWater);
        SGGameTestsNeoForge.TESTS.put("fluid_pours_through", SGGameTestsNeoForge::fluidPours);
        SGGameTestsNeoForge.TESTS.put("pond_drains", SGGameTestsNeoForge::pondDrains);
        SGGameTestsNeoForge.TESTS.put("pours_as_much_as_covered", SGGameTestsNeoForge::poursAsMuchAsCovered);
        SGGameTestsNeoForge.TESTS.put("pours_out_of_the_front", SGGameTestsNeoForge::poursOutOfTheFront);
        SGGameTestsNeoForge.TESTS.put("horizon_takes_fluid_in", SGGameTestsNeoForge::horizonTakesFluidIn);
        SGGameTestsNeoForge.TESTS.put("buckets_keep_the_horizon", SGGameTestsNeoForge::bucketsKeepTheHorizon);
        SGGameTestsNeoForge.TESTS.put("spawner_in_water", SGGameTestsNeoForge::spawnerInWater);
        SGGameTestsNeoForge.TESTS.put("structure_template_builds_gate", SGGameTestsNeoForge::structureTemplate);
        SGGameTestsNeoForge.TESTS.put("gate_beside_swamp_hut", SGGameTestsNeoForge::gateBesideSwampHut);
        SGGameTestsNeoForge.TESTS.put("gate_in_desert_pyramid", SGGameTestsNeoForge::gateInPyramid);
        SGGameTestsNeoForge.TESTS.put("village_pools_have_plazas", SGGameTestsNeoForge::villagePools);
        SGGameTestsNeoForge.TESTS.put("tokra_works_at_the_dhd", SGGameTestsNeoForge::tokra);
        SGGameTestsNeoForge.TESTS.put("chevron_upgrade_adds_one", SGGameTestsNeoForge::chevronUpgrade);
        SGGameTestsNeoForge.TESTS.put("shut_iris_is_solid", SGGameTestsNeoForge::shutIrisIsSolid);
        SGGameTestsNeoForge.TESTS.put("orphan_iris_plate_goes", SGGameTestsNeoForge::orphanIrisPlateGoes);
        SGGameTestsNeoForge.TESTS.put("power_unit_powers_gate", SGGameTestsNeoForge::powerUnit);
        SGGameTestsNeoForge.TESTS.put("chevrons_cost_as_they_lock", SGGameTestsNeoForge::chevronsCostAsTheyLock);
        SGGameTestsNeoForge.TESTS.put("dial_needs_energy_for_chevrons",
                SGGameTestsNeoForge::dialNeedsEnergyForChevrons);
        SGGameTestsNeoForge.TESTS.put("gate_costs_override_server", SGGameTestsNeoForge::gateCostsOverrideServer);
        SGGameTestsNeoForge.TESTS.put("gate_can_refuse_dhd_fuel", SGGameTestsNeoForge::gateCanRefuseDhdFuel);
        SGGameTestsNeoForge.TESTS.put("ring_speed_sets_dial_time", SGGameTestsNeoForge::ringSpeed);
        SGGameTestsNeoForge.TESTS.put("dhd_range_is_the_gates", SGGameTestsNeoForge::dhdRange);
        SGGameTestsNeoForge.TESTS.put("camouflage_draws_chevrons_together", SGGameTestsNeoForge::chevronSpacing);
        SGGameTestsNeoForge.TESTS.put("tablet_dials_its_gates", SGGameTestsNeoForge::tabletDials);
        SGGameTestsNeoForge.TESTS.put("tools_tell_a_gate_is_gone", SGGameTestsNeoForge::gateGone);
        SGGameTestsNeoForge.TESTS.put("chests_hold_sgcraft_loot", SGGameTestsNeoForge::chestLoot);
        SGGameTestsNeoForge.TESTS.put("broken_base_drops_upgrades", SGGameTestsNeoForge::baseDropsUpgrades);
        SGGameTestsNeoForge.TESTS.put("new_pdds_take_default_addresses", SGGameTestsNeoForge::defaultAddresses);
        SGGameTestsNeoForge.TESTS.put("breaking_an_open_gate_explodes", SGGameTestsNeoForge::breach);
        SGGameTestsNeoForge.TESTS.put("wormhole_jumps_to_cell_mate", SGGameTestsNeoForge::wormholeJumps);
        SGGameTestsNeoForge.TESTS.put("type_upgrade_changes_gate", SGGameTestsNeoForge::typeUpgrade);
        SGGameTestsNeoForge.TESTS.put("open_gate_gives_light", SGGameTestsNeoForge::gateGivesLight);
        SGGameTestsNeoForge.TESTS.put("flat_gate_forms_face_up", SGGameTestsNeoForge::flatGateForms);
        SGGameTestsNeoForge.TESTS.put("flat_gate_turns_face_down", SGGameTestsNeoForge::flatGateTurns);
        SGGameTestsNeoForge.TESTS.put("flat_gate_travels_to_standing_gate", SGGameTestsNeoForge::flatToStanding);
        SGGameTestsNeoForge.TESTS.put("standing_gate_travels_to_flat_gate", SGGameTestsNeoForge::standingToFlat);
        SGGameTestsNeoForge.TESTS.put("face_down_gate_takes_heads_first", SGGameTestsNeoForge::headFirst);
        SGGameTestsNeoForge.TESTS.put("horizon_lies_in_flat_opening", SGGameTestsNeoForge::flatHorizon);
        SGGameTestsNeoForge.TESTS.put("kawoosh_can_break_unbreakable", SGGameTestsNeoForge::kawooshBreaksUnbreakable);
        SGGameTestsNeoForge.TESTS.put("tokra_worlds_set_off_have_none", SGSettingGameTestsNeoForge::tokraWorlds);
        SGGameTestsNeoForge.TESTS.put("address_settings_by_world", SGSettingGameTestsNeoForge::addressSettings);
        SGGameTestsNeoForge.TESTS.put("cell_size_groups_chunks", SGSettingGameTestsNeoForge::cellSize);
        AddressGroupGameTestsNeoForge.register(SGGameTestsNeoForge.TESTS);
        SettingsCatalogGameTestsNeoForge.register(SGGameTestsNeoForge.TESTS);
        SGGameTestsNeoForge.TESTS.put("layout_change_warns_with_gates",
                SGSettingGameTestsNeoForge::layoutChangeWarning);
        SGGameTestsNeoForge.TESTS.put("gate_defaults_fill_missing_values", SGAccessGameTestsNeoForge::gateDefaults);
        SGGameTestsNeoForge.TESTS.put("gate_defaults_reach_new_gates", SGAccessGameTestsNeoForge::formationDefaults);
        SGGameTestsNeoForge.TESTS.put("builder_owns_gate", SGAccessGameTestsNeoForge::builderOwns);
        SGGameTestsNeoForge.TESTS.put("configure_needs_owner_or_right", SGAccessGameTestsNeoForge::configureRights);
        SGGameTestsNeoForge.TESTS.put("breaking_follows_owner_and_settings", SGAccessGameTestsNeoForge::breaking);
        SGGameTestsNeoForge.TESTS.put("storm_sends_wormhole_astray", SGGameTestsNeoForge::stormReroutes);
        SGGameTestsNeoForge.TESTS.put("canceled_disconnect_keeps_wormhole", SGGameTestsNeoForge::stormRerouteCanceled);
        SGGameTestsNeoForge.TESTS.put("gate_energy_settings_override_server", SGGameTestsNeoForge::gateEnergySettings);
        SGGameTestsNeoForge.TESTS.put("naquadah_ore_under_lava", SGGameTestsNeoForge::oreUnderLava);
        SGGameTestsNeoForge.TESTS.put("naquadah_ore_isolated_nodes", SGGameTestsNeoForge::oreIsolatedNodes);
        SGGameTestsNeoForge.TESTS.put("naquadah_ore_added_to_old_chunks_once",
                SGGameTestsNeoForge::oreAddedToOldChunks);
        MalpGameTestsNeoForge.register(SGGameTestsNeoForge.TESTS);
        ShieldGameTestsNeoForge.register(SGGameTestsNeoForge.TESTS);
        OriGameTestsNeoForge.register(SGGameTestsNeoForge.TESTS);
        ZpmGameTestsNeoForge.register(SGGameTestsNeoForge.TESTS);
    }

    private SGGameTestsNeoForge() {
    }

    /** Registers the tests' functions, and the tests themselves when game tests are enabled. */
    public static void register(final IEventBus modBus) {
        modBus.addListener((final RegisterEvent event) -> event.register(Registries.TEST_FUNCTION, helper -> {
            int index = 0;
            for (final Map.Entry<String, BiConsumer<GameTestHelper,
                    BlockPos>> test : SGGameTestsNeoForge.TESTS.entrySet()) {
                final BlockPos origin = new BlockPos(16 + index++ * SGGameTestsNeoForge.SPACING,
                        SGGameTestsNeoForge.HEIGHT, 16);
                final Consumer<GameTestHelper> function = h -> test.getValue().accept(h, origin);
                helper.register(SGCraft.id(test.getKey()), function);
            }
        }));
        modBus.addListener((final RegisterGameTestsEvent event) -> {
            final Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(SGCraft.id("default"),
                    new TestEnvironmentDefinition.AllOf(List.of()));
            SGGameTestsNeoForge.TESTS.keySet().forEach(name -> event.registerTest(SGCraft.id(name),
                    new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, SGCraft.id(name)),
                            new TestData<>(environment, SGGameTestsNeoForge.STRUCTURE, 600, 0, true))));
        });
    }

    static Cause server(final GameTestHelper helper) {
        return Cause.of(helper.getLevel().getServer());
    }

    static Stargate place(final GameTestHelper helper, final BlockPos at, final int chevrons) {
        final GlobalPos position = GlobalPos.of(helper.getLevel().dimension(), at);
        final PlacementResult result = SGCraftApi.get().stargates().place(GatePlacement.builder()
                .position(position).orientation(FrontAndTop.NORTH_UP)
                .type(SGGameTestsNeoForge.type(helper, GateTypes.MILKY_WAY))
                .chevrons(chevrons).mode(PlacementMode.FORCE).noFixtures().build(), SGGameTestsNeoForge.server(helper));
        if (result.status() != PlacementResult.Status.PLACED) {
            throw helper.assertionException(Component.literal("Placing a gate failed: " + result.status()));
        }
        return result.gate().orElseThrow();
    }

    static Holder<GateType> type(final GameTestHelper helper, final ResourceKey<GateType> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(SGCraftRegistries.GATE_TYPE).getOrThrow(key);
    }

    static Address addressOf(final Stargate gate, final Stargate from) {
        return SGCraftApi.get().addressing().addressOf(gate.position(), from.position().dimension()).orElseThrow();
    }

    // A gate whose chunks have unloaded, in another world, is still dialed: dialing it loads them and it opens
    private static void dialReachesUnloadedGate(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        if (nether == null) {
            throw helper.assertionException(Component.literal("There is no Nether"));
        }
        final Stargate a = SGGameTestsNeoForge.place(helper, origin, 9);
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.OPENING_DAMAGE, false, SGGameTestsNeoForge.server(helper));
        final BlockPos far = new BlockPos(SectionPos.blockToSectionCoord(origin.getX() + 12288) * 16 + 8, 100,
                SectionPos.blockToSectionCoord(origin.getZ()) * 16 + 8);
        final PlacementResult placed = SGCraftApi.get().stargates().place(GatePlacement.builder()
                .position(GlobalPos.of(Level.NETHER, far)).orientation(FrontAndTop.NORTH_UP)
                .type(SGGameTestsNeoForge.type(helper, GateTypes.MILKY_WAY)).chevrons(9)
                .mode(PlacementMode.FORCE).noFixtures().build(), SGGameTestsNeoForge.server(helper));
        if (placed.status() != PlacementResult.Status.PLACED) {
            throw helper.assertionException(Component.literal("Placing gate B failed: " + placed.status()));
        }
        final Stargate b = placed.gate().orElseThrow();
        final ChunkPos chunk = ChunkPos.containing(far);
        final AtomicReference<DialResult> result = new AtomicReference<>();
        final AtomicInteger dialedAt = new AtomicInteger(-1);
        final AtomicInteger ticks = new AtomicInteger();
        helper.onEachTick(() -> {
            final int now = ticks.incrementAndGet();
            if (dialedAt.get() < 0 && !nether.getChunkSource().hasChunk(chunk.x(), chunk.z())) {
                dialedAt.set(now);
                a.dial(SGGameTestsNeoForge.addressOf(b, a), DialMode.QUICK, SGGameTestsNeoForge.server(helper))
                        .thenAccept(result::set);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(dialedAt.get() >= 0, "Gate B's chunk never unloaded");
            helper.assertTrue(result.get() != null, "The dial hasn't finished");
            helper.assertTrue(result.get() == DialResult.CONNECTED, "The dial ended " + result.get());
            helper.assertTrue(b.state() == GateState.CONNECTED, "Gate B isn't open");
            a.disconnect(SGGameTestsNeoForge.server(helper));
        });
    }

    // An address kept in a book names its world, so it reaches the same gate from another world. A gate never dials
    // an address longer than it has chevrons for, so a 7 chevron gate in that world can't dial it; a tool dials it as
    // the short address instead
    private static void keptAddressDials(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = SGGameTestsNeoForge.place(helper, origin, 7);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.east(128), 7);
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.OPENING_DAMAGE, false, SGGameTestsNeoForge.server(helper));
        final SGAddressing addressing = SGServer.require().addressing();
        final ResourceKey<Level> here = helper.getLevel().dimension();
        final Address nearby = SGGameTestsNeoForge.addressOf(b, a);
        final Address kept = addressing.lasting(nearby, here);
        helper.assertTrue(kept.size() > nearby.size(), "The kept address " + kept + " doesn't name its world");
        helper.assertTrue(addressing.resolve(kept, Level.NETHER).equals(addressing.resolve(nearby, here)),
                "The kept address points elsewhere from the Nether");
        helper.assertTrue(addressing.resolve(nearby, Level.NETHER).map(target -> target.world().equals(here))
                .orElse(false) == false, "The short address reaches the overworld from the Nether");
        helper.assertTrue(addressing.shortest(kept, here).equals(nearby), "The kept address doesn't shorten to "
                + nearby);
        SGGameTestsNeoForge.expectDialTo(helper, a, kept, DialResult.NOT_ENOUGH_CHEVRONS, () -> {
        });
    }

    // Gate A dials an address quickly and the test passes when the result is the one expected
    private static void expectDialTo(final GameTestHelper helper, final Stargate a, final Address address,
            final DialResult expected, final Runnable after) {
        final AtomicReference<DialResult> result = new AtomicReference<>();
        a.dial(address, DialMode.QUICK, SGGameTestsNeoForge.server(helper)).thenAccept(result::set);
        helper.succeedWhen(() -> {
            helper.assertTrue(result.get() == expected, "The dial ended " + result.get() + ", not " + expected);
            after.run();
        });
    }

    // Gate A dials gate B quickly and the test passes when the result is the one expected
    static void expectDial(final GameTestHelper helper, final Stargate a, final Stargate b, final Cause cause,
            final DialResult expected, final Runnable after) {
        final AtomicReference<DialResult> result = new AtomicReference<>();
        a.dial(SGGameTestsNeoForge.addressOf(b, a), DialMode.QUICK, cause).thenAccept(result::set);
        helper.succeedWhen(() -> {
            helper.assertTrue(result.get() != null, "The dial hasn't finished");
            helper.assertTrue(result.get() == expected, "The dial ended " + result.get() + ", not " + expected);
            after.run();
        });
    }

    private static void placement(final GameTestHelper helper, final BlockPos origin) {
        final Stargate gate = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        helper.assertTrue(gate.exists(), "The gate doesn't exist");
        helper.assertTrue(gate.chevrons() == 9, "The gate has " + gate.chevrons() + " chevrons, not 9");
        helper.assertTrue(helper.getLevel().getBlockState(gate.position().pos()).is(SGBlocks.STARGATE_BASE),
                "There is no base block");
        final PlacementResult again = SGCraftApi.get().stargates().place(GatePlacement.builder()
                .position(gate.position()).orientation(FrontAndTop.NORTH_UP)
                .type(SGGameTestsNeoForge.type(helper, GateTypes.PEGASUS)).chevrons(7).build(),
                        SGGameTestsNeoForge.server(helper));
        helper.assertTrue(again.status() == PlacementResult.Status.GATE_IN_THE_WAY,
                "Placing over a gate ended " + again.status());
        helper.succeed();
    }

    private static void dialNeedsEnergy(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.B), 9);
        SGGameTestsNeoForge.expectDial(helper, a, b, SGGameTestsNeoForge.server(helper),
                DialResult.NOT_ENOUGH_ENERGY, () -> {
        });
    }

    private static void dialWithStoredEnergy(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.B), 9);
        a.set(GateProperties.ENERGY_CAPACITY, 100_000L, SGGameTestsNeoForge.server(helper));
        a.energy().insert(100_000, false);
        SGGameTestsNeoForge.expectDial(helper, a, b, SGGameTestsNeoForge.server(helper), DialResult.CONNECTED, () -> {
            helper.assertTrue(a.energy().stored() <= 100_000 - 4000,
                    "Opening took too little: " + a.energy().stored() + " left");
            helper.assertTrue(b.connection().isPresent(), "Gate B isn't connected");
        });
    }

    // Energy goes as each chevron locks, before the wormhole forms
    private static void chevronsCostAsTheyLock(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.B), 9);
        a.set(GateProperties.ENERGY_CAPACITY, 100_000L, SGGameTestsNeoForge.server(helper));
        a.energy().insert(100_000, false);
        a.dial(SGGameTestsNeoForge.addressOf(b, a), DialMode.STANDARD, SGGameTestsNeoForge.server(helper));
        helper.succeedWhen(() -> {
            helper.assertTrue(a.state() == GateState.DIALING, "The gate is " + a.state() + ", not dialing");
            helper.assertTrue(a.energy().stored() < 100_000, "No chevron has been paid for yet");
            helper.assertTrue(a.energy().stored() > 100_000 - 4000, "The opening was paid for while dialing");
        });
    }

    // Enough for the opening but not the chevrons as well: the dial doesn't start
    private static void dialNeedsEnergyForChevrons(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.B), 9);
        a.set(GateProperties.ENERGY_CAPACITY, 4100L, SGGameTestsNeoForge.server(helper));
        a.energy().insert(4100, false);
        SGGameTestsNeoForge.expectDial(helper, a, b, SGGameTestsNeoForge.server(helper),
                DialResult.NOT_ENOUGH_ENERGY, () ->
                helper.assertTrue(a.energy().stored() == 4100, "A refused dial took energy"));
    }

    // A gate with every cost turned off on itself dials with no energy, whatever the server's costs are
    private static void gateCostsOverrideServer(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.B), 9);
        a.set(GateProperties.CHEVRON_COST_ENABLED, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.OPEN_COST_ENABLED, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.TICK_COST_ENABLED, false, SGGameTestsNeoForge.server(helper));
        SGGameTestsNeoForge.expectDial(helper, a, b, SGGameTestsNeoForge.server(helper), DialResult.CONNECTED, () -> {
        });
    }

    // A gate set not to take DHD fuel can't dial on a fuelled DHD alone, and leaves the naquadah be
    private static void gateCanRefuseDhdFuel(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.B), 9);
        a.set(GateProperties.DHD_FUEL, false, SGGameTestsNeoForge.server(helper));
        final BlockPos dhd = a.position().pos().north(4);
        helper.getLevel().setBlock(dhd, SGBlocks.DHD.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
                Direction.NORTH), Block.UPDATE_ALL);
        final DhdBlockEntity entity = (DhdBlockEntity) helper.getLevel().getBlockEntity(dhd);
        entity.fuel().setItem(0, new ItemStack(SGItems.NAQUADAH, 1));
        final AtomicReference<DialResult> result = new AtomicReference<>();
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(entity.gate().isPresent(), "The DHD didn't link to its gate");
            a.dial(SGGameTestsNeoForge.addressOf(b, a), DialMode.QUICK, SGGameTestsNeoForge.server(helper))
                    .thenAccept(result::set);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(result.get() == DialResult.NOT_ENOUGH_ENERGY, "The dial ended " + result.get());
            helper.assertTrue(!entity.fuel().getItem(0).isEmpty(), "The naquadah was used");
        });
    }

    // A ring turning a whole turn a tick dials a full address, one symbol at a time, well within the test's time
    private static void ringSpeed(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.B), 9);
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.RING_SPEED, 360.0, SGGameTestsNeoForge.server(helper));
        final AtomicReference<DialResult> result = new AtomicReference<>();
        a.dial(SGGameTestsNeoForge.addressOf(b, a), DialMode.STANDARD, SGGameTestsNeoForge.server(helper))
                .thenAccept(result::set);
        helper.succeedWhen(() -> helper.assertTrue(result.get() == DialResult.CONNECTED,
                "The dial ended " + result.get()));
    }

    // A DHD further than the default reach links once the gate's reach covers it
    private static void dhdRange(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final BlockPos dhd = a.position().pos().north(9);
        helper.getLevel().setBlock(dhd, SGBlocks.DHD.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
                Direction.NORTH), Block.UPDATE_ALL);
        final DhdBlockEntity entity = (DhdBlockEntity) helper.getLevel().getBlockEntity(dhd);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(entity.gate().isEmpty(), "The DHD linked from beyond the default reach");
            a.set(GateProperties.DHD_RANGE, 12.0, SGGameTestsNeoForge.server(helper));
        });
        helper.runAfterDelay(31, () -> helper.succeedWhen(() -> helper.assertTrue(entity.gate().isPresent(),
                "The DHD didn't link within the gate's reach")));
    }

    // Camouflage over the bottom row's corners draws a nine chevron gate's chevrons together, as the original did:
    // 33 degrees apart over slabs, 30 over full blocks; a seven chevron gate's are never covered, so stay put
    private static void chevronSpacing(final GameTestHelper helper, final BlockPos origin) {
        final SimpleContainer camouflage = new SimpleContainer(5);
        helper.assertTrue(ChevronSpacing.of(9, camouflage) == 40, "Bare, the chevrons moved");
        camouflage.setItem(4, new ItemStack(Items.STONE_SLAB));
        helper.assertTrue(ChevronSpacing.of(9, camouflage) == 33, "Over a slab: " + ChevronSpacing.of(9, camouflage));
        camouflage.setItem(0, new ItemStack(Items.STONE));
        helper.assertTrue(ChevronSpacing.of(9, camouflage) == 30, "Over stone: " + ChevronSpacing.of(9, camouflage));
        helper.assertTrue(ChevronSpacing.of(7, camouflage) == 40, "A seven chevron gate's chevrons moved");
        // Each chevron's place lands where the chevron stands, the top and the bottom stay put, and turns carry over
        for (int position = 0; position < ChevronOrder.POSITIONS; position++) {
            final double moved = ChevronSpacing.squeeze(ChevronSpacing.angle(position, 40), 30);
            helper.assertTrue(Math.abs(moved - ChevronSpacing.angle(position, 30)) < 1e-9,
                    "Chevron " + position + " landed at " + moved);
        }
        helper.assertTrue(ChevronSpacing.squeeze(90, 30) == 90, "The top moved");
        helper.assertTrue(Math.abs(ChevronSpacing.squeeze(-90, 30) + 90) < 1e-9, "The bottom moved");
        helper.assertTrue(Math.abs(ChevronSpacing.squeeze(450, 30) - 450) < 1e-9, "A whole turn didn't carry over");
        helper.succeed();
    }

    // A tablet with gate A on its list dials gate B from A, though its holder isn't near A
    // Vanilla's chests roll SGCraft's tables too: a stronghold library gives crystals three times in four
    private static void chestLoot(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final LootTable library = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(
                Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/stronghold_library")));
        final LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(origin)))
                .create(LootContextParamSets.CHEST);
        int crystals = 0;
        for (int roll = 0; roll < 100; roll++) {
            crystals += (int) library.getRandomItems(params).stream().filter(stack -> stack.is(SGItems.CORE_CRYSTAL)
                    || stack.is(SGItems.CONTROLLER_CRYSTAL)).count();
        }
        helper.assertTrue(crystals > 0, "A hundred stronghold libraries held no crystals");
        helper.succeed();
    }

    private static void tabletDials(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.B), 9);
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        final ServerPlayer player = SGGameTestsNeoForge.playerAt(helper, b);
        final ItemStack tablet = new ItemStack(SGItems.GATE_CONTROL_TABLET);
        tablet.set(SGDataComponents.TABLET_GATES, List.of(new TabletGate(a.position(), "Home")));
        player.setItemInHand(InteractionHand.MAIN_HAND, tablet);
        ToolActions.tablet(player, new ToolPayloads.TabletAction(ToolPayloads.TabletAction.Kind.DIAL, 0,
                SGGameTestsNeoForge.addressOf(b, a).toString(), "", 0));
        helper.succeedWhen(() -> helper.assertTrue(a.state() != GateState.IDLE && b.state() != GateState.IDLE,
                "The tablet didn't dial: A is " + a.state() + ", B is " + b.state()));
    }

    // An address whose gate is broken shows as no gate
    private static void gateGone(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.B), 9);
        final Address address = SGGameTestsNeoForge.addressOf(b, a);
        final ResourceKey<Level> from = a.position().dimension();
        helper.assertTrue(ToolActions.status(address, from).equals("idle"),
                "The gate shows as " + ToolActions.status(address, from));
        helper.getLevel().destroyBlock(b.position().pos(), false);
        helper.succeedWhen(() -> helper.assertTrue(ToolActions.status(address, from)
                .equals(ToolPayloads.BookEntry.NO_GATE), "The broken gate shows as " + ToolActions.status(address,
                from)));
    }

    private static void dhdFuel(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.B), 9);
        final BlockPos dhd = a.position().pos().north(4);
        helper.getLevel().setBlock(dhd, SGBlocks.DHD.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
                Direction.NORTH), Block.UPDATE_ALL);
        final DhdBlockEntity entity = (DhdBlockEntity) helper.getLevel().getBlockEntity(dhd);
        // The DHD links on its own within a second; it is filled with a piece of naquadah first
        final Container fuel = entity.fuel();
        fuel.setItem(0, new ItemStack(SGItems.NAQUADAH, 1));
        final AtomicReference<DialResult> result = new AtomicReference<>();
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(entity.gate().isPresent(), "The DHD didn't link to its gate");
            helper.assertTrue(entity.available() > 0, "The DHD offers no energy: " + entity.available());
            final int providers = helper.getLevel().registryAccess()
                    .lookupOrThrow(SGCraftRegistries.ENERGY_SOURCE_PROVIDER).size();
            helper.assertTrue(providers > 0, "No energy source providers are registered");
            final long offered = helper.getLevel().registryAccess()
                    .lookupOrThrow(SGCraftRegistries.ENERGY_SOURCE_PROVIDER).stream()
                    .flatMap(provider -> provider.sourcesFor(a).stream()).mapToLong(source -> source.available()).sum();
            helper.assertTrue(offered > 0, "The gate is offered no energy; the DHD has " + entity.available()
                    + ", DHD at " + dhd + " linked " + entity.linked() + ", gate at " + a.position().pos()
                    + ", loaded " + helper.getLevel().isLoaded(dhd) + ", found "
                    + helper.getLevel().getBlockEntity(dhd.mutable()));
            a.dial(SGGameTestsNeoForge.addressOf(b, a), DialMode.QUICK, SGGameTestsNeoForge.server(helper))
                    .thenAccept(result::set);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(result.get() == DialResult.CONNECTED, "The dial ended " + result.get());
            helper.assertTrue(fuel.getItem(0).isEmpty(), "The naquadah wasn't used");
            helper.assertTrue(entity.available() > 0, "The rest of the naquadah's energy wasn't kept");
        });
    }

    private static void irisMoves(final GameTestHelper helper, final BlockPos origin) {
        final Stargate gate = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final AtomicInteger posts = new AtomicInteger();
        final EventBus.Registration listener = SGCraftApi.get().eventBus().register(IrisEvent.Post.class, event -> {
            if (event.gate().equals(gate)) {
                posts.incrementAndGet();
            }
        });
        helper.assertTrue(gate.set(GateProperties.IRIS, IrisTypes.METAL, SGGameTestsNeoForge.server(helper))
                == ActionResult.DONE, "Fitting the iris failed");
        helper.assertTrue(gate.iris().orElseThrow().state() == IrisState.OPEN, "A fitted iris isn't open");
        helper.assertTrue(gate.iris().orElseThrow().close(SGGameTestsNeoForge.server(helper)) == ActionResult.DONE,
                "Closing the iris failed");
        helper.assertTrue(gate.iris().orElseThrow().state() == IrisState.CLOSING, "The iris isn't closing");
        helper.runAfterDelay(SGIris.MOVE_TICKS + 5, () -> {
            listener.unregister();
            helper.assertTrue(gate.iris().orElseThrow().state() == IrisState.CLOSED, "The iris didn't shut");
            helper.assertTrue(posts.get() == 1, "IrisEvent.Post fired " + posts.get() + " times");
            helper.succeed();
        });
    }

    private static void dialCanceled(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.B), 9);
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        final EventBus.Registration listener = SGCraftApi.get().eventBus().register(DialEvent.Pre.class, event -> {
            if (event.gate().equals(a)) {
                event.setCanceled(true);
            }
        });
        SGGameTestsNeoForge.expectDial(helper, a, b, SGGameTestsNeoForge.server(helper), DialResult.CANCELED,
                listener::unregister);
    }

    private static void rightsDenyDial(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.B), 9);
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.DEFAULT_RIGHTS, Set.of(), SGGameTestsNeoForge.server(helper));
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SGGameTestsNeoForge.expectDial(helper, a, b, Cause.of(player), DialResult.DENIED, () -> {
        });
    }

    private static void addressRules(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.B), 9);
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        b.set(GateProperties.ALLOW_INCOMING, false, SGGameTestsNeoForge.server(helper));
        final AtomicReference<DialResult> first = new AtomicReference<>();
        a.dial(SGGameTestsNeoForge.addressOf(b, a), DialMode.QUICK, SGGameTestsNeoForge.server(helper))
                .thenAccept(first::set);
        helper.assertTrue(first.get() == DialResult.DENIED, "A gate refusing incoming was dialed: " + first.get());
        // A rule for gate A's address lets it in again
        b.set(GateProperties.ADDRESS_RULES, Map.of(SGGameTestsNeoForge.addressOf(a, b), new AddressRule(true, true)),
                SGGameTestsNeoForge.server(helper));
        SGGameTestsNeoForge.expectDial(helper, a, b, SGGameTestsNeoForge.server(helper), DialResult.CONNECTED, () -> {
        });
    }

    private static void addressBook(final GameTestHelper helper, final BlockPos origin) {
        final AddressBook book = SGCraftApi.get().addressBooks().forPlayer(UUID.randomUUID());
        final Cause cause = SGGameTestsNeoForge.server(helper);
        final Address one = Address.parse("ABCD-EFG").orElseThrow();
        final Address two = Address.parse("HIJK-LMN").orElseThrow();
        helper.assertTrue(book.add(one, Component.literal("One"), cause) == ActionResult.DONE, "Adding failed");
        helper.assertTrue(book.add(one, Component.literal("Again"), cause) == ActionResult.NO_CHANGE,
                "Adding twice changed the book");
        helper.assertTrue(book.add(two, Component.literal("Two"), cause) == ActionResult.DONE, "Adding failed");
        helper.assertTrue(book.move(two, 0, cause) == ActionResult.DONE, "Moving failed");
        helper.assertTrue(book.stream().findFirst().orElseThrow().address().equals(two), "Moving didn't move it");
        helper.assertTrue(book.rename(one, Component.literal("Uno"), cause) == ActionResult.DONE, "Renaming failed");
        helper.assertTrue(book.find(one).orElseThrow().name().getString().equals("Uno"), "Renaming didn't rename");
        helper.assertTrue(book.remove(one, cause) == ActionResult.DONE && book.size() == 1, "Removing failed");
        helper.succeed();
    }

    // A pig crossing gate A's horizon comes out of gate B
    private static void travel(final GameTestHelper helper, final BlockPos origin) {
        SGGameTestsNeoForge.crossing(helper, origin, false, false, true);
    }

    // A traveler holding a Tollan phase shift device in its off hand goes through a shut iris
    private static void phaseShift(final GameTestHelper helper, final BlockPos origin) {
        SGGameTestsNeoForge.crossing(helper, origin, true, true, true);
    }

    // Unless the far gate says its iris stops even that
    private static void phaseShiftStopped(final GameTestHelper helper, final BlockPos origin) {
        SGGameTestsNeoForge.crossing(helper, origin, true, true, false);
    }

    private static void irisStopsTravel(final GameTestHelper helper, final BlockPos origin) {
        SGGameTestsNeoForge.crossing(helper, origin, true, false, true);
    }

    // A pig, or with a phase shift device a zombie holding one, walked through gate A to gate B
    private static void crossing(final GameTestHelper helper, final BlockPos origin, final boolean irisShut,
            final boolean phaseShift, final boolean irisLetsPhaseThrough) {
        final Stargate a = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.B), 9);
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.OPENING_DAMAGE, false, SGGameTestsNeoForge.server(helper));
        if (irisShut) {
            b.set(GateProperties.IRIS, IrisTypes.METAL, SGGameTestsNeoForge.server(helper));
            b.iris().orElseThrow().close(SGGameTestsNeoForge.server(helper));
        }
        b.set(GateProperties.PHASE_SHIFT_PASSES_IRIS, irisLetsPhaseThrough, SGGameTestsNeoForge.server(helper));
        final boolean passes = !irisShut || phaseShift && irisLetsPhaseThrough;
        final Class<? extends Mob> kind = phaseShift ? Zombie.class : Pig.class;
        final AtomicReference<DialResult> result = new AtomicReference<>();
        final AtomicInteger arrived = new AtomicInteger();
        final EventBus.Registration listener = SGCraftApi.get().eventBus().register(TravelEvent.Post.class,
                event -> {
                    if (event.gate().equals(a)) {
                        arrived.incrementAndGet();
                    }
                });
        a.dial(SGGameTestsNeoForge.addressOf(b, a), DialMode.QUICK, SGGameTestsNeoForge.server(helper))
                .thenAccept(result::set);
        final GateFrame frame = new GateFrame(a.position().pos(), Direction.NORTH);
        final AtomicReference<Mob> pig = new AtomicReference<>();
        // Once the wormhole is open and the kawoosh has passed, walk a pig through gate A
        helper.runAfterDelay(SGIris.MOVE_TICKS + 60, () -> {
            helper.assertTrue(result.get() == DialResult.CONNECTED, "The dial ended " + result.get());
            final Mob spawned = phaseShift ? helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, BlockPos.ZERO.above())
                    : helper.spawnWithNoFreeWill(EntityTypes.PIG, BlockPos.ZERO.above());
            if (phaseShift) {
                spawned.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(SGItems.TOLLAN_PHASE_SHIFT_DEVICE));
            }
            spawned.setNoGravity(true);
            final Vec3 front = frame.toWorld(new Vec3(0, 1, 0.8));
            spawned.setPos(front.x, front.y, front.z);
            pig.set(spawned);
        });
        helper.runAfterDelay(SGIris.MOVE_TICKS + 63, () -> {
            final Vec3 behind = frame.toWorld(new Vec3(0, 1, -0.4));
            pig.get().setPos(behind.x, behind.y, behind.z);
        });
        helper.runAfterDelay(SGIris.MOVE_TICKS + 70, () -> {
            listener.unregister();
            final AABB nearB = new AABB(b.position().pos()).inflate(4);
            final boolean atB = !helper.getLevel().getEntitiesOfClass(kind, nearB).isEmpty();
            if (passes) {
                helper.assertTrue(atB && arrived.get() == 1, "The traveler didn't come out of gate B");
            } else {
                helper.assertTrue(!atB && arrived.get() == 0, "The traveler went through a shut iris");
                helper.assertTrue(!pig.get().isAlive(), "The traveler didn't die against the iris");
            }
            helper.succeed();
        });
    }

    private static void fixtures(final GameTestHelper helper, final BlockPos origin) {
        final Stargate gate = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final FixtureBuffer buffer = new FixtureBuffer().set(new BlockPos(0, 0, 4),
                SGBlocks.DHD.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        helper.assertTrue(gate.placeFixtures(buffer, PlacementMode.ATTEMPT, SGGameTestsNeoForge.server(helper))
                == ActionResult.DONE, "Building the fixtures failed");
        // The gate faces north, so 4 out of its front is 4 blocks north
        helper.assertTrue(helper.getLevel().getBlockState(gate.position().pos().north(4)).is(SGBlocks.DHD),
                "The DHD isn't in front of the gate");
        helper.succeed();
    }

    private static void propertyEvent(final GameTestHelper helper, final BlockPos origin) {
        final Stargate gate = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final EventBus.Registration listener = SGCraftApi.get().eventBus().register(
                ChangeGatePropertyEvent.Pre.class, event -> {
                    if (event.gate().equals(gate)) {
                        event.setNewValue(GateProperties.OPEN_TICKS, Optional.of(1234));
                    }
                });
        gate.set(GateProperties.OPEN_TICKS, 100, SGGameTestsNeoForge.server(helper));
        listener.unregister();
        helper.assertTrue(gate.get(GateProperties.OPEN_TICKS).orElse(0) == 1234,
                "The listener's value wasn't kept: " + gate.get(GateProperties.OPEN_TICKS));
        helper.succeed();
    }

    // A player standing in front of a gate, for the tools, which act on the gate near them
    private static ServerPlayer playerAt(final GameTestHelper helper, final Stargate gate) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final Vec3 front = new GateFrame(gate.position().pos(), Direction.NORTH).toWorld(new Vec3(0, 0, 3));
        player.teleportTo(front.x, front.y, front.z);
        return player;
    }

    private static void pddBook(final GameTestHelper helper, final BlockPos origin) {
        final Stargate gate = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final ServerPlayer player = SGGameTestsNeoForge.playerAt(helper, gate);
        // The addresses go on the PDD held, not on the player
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SGItems.PDD));
        ToolActions.pdd(player, new ToolPayloads.PddAction(ToolPayloads.PddAction.Kind.ADD, "ABCD-EFG", "Home", 0));
        ToolActions.pdd(player, new ToolPayloads.PddAction(ToolPayloads.PddAction.Kind.ADD, "HIJK-LMN", "", 0));
        ToolActions.pdd(player, new ToolPayloads.PddAction(ToolPayloads.PddAction.Kind.RENAME, "ABCD-EFG", "House",
                0));
        ToolActions.pdd(player, new ToolPayloads.PddAction(ToolPayloads.PddAction.Kind.MOVE, "HIJK-LMN", "", 0));
        final AddressBook book = ItemAddressBook.of(player.getItemInHand(InteractionHand.MAIN_HAND));
        helper.assertTrue(book.size() == 2, "The book has " + book.size() + " entries");
        helper.assertTrue(SGCraftApi.get().addressBooks().forPlayer(player.getUUID()).isEmpty(),
                "The addresses went into the player's own book");
        helper.assertTrue(ItemAddressBook.of(player.getItemInHand(InteractionHand.MAIN_HAND).copy()).size() == 2,
                "A PDD given away doesn't carry its addresses");
        helper.assertTrue(book.stream().findFirst().orElseThrow().name().getString().equals("HIJK-LMN"),
                "An entry with no name isn't named by its address, or moving failed");
        helper.assertTrue(book.find(Address.parse("ABCD-EFG").orElseThrow()).orElseThrow().name().getString()
                .equals("House"), "Renaming failed");
        ToolActions.pdd(player, new ToolPayloads.PddAction(ToolPayloads.PddAction.Kind.REMOVE, "ABCD-EFG", "", 0));
        helper.assertTrue(book.size() == 1, "Removing failed");
        helper.succeed();
    }

    private static void configuratorApply(final GameTestHelper helper, final BlockPos origin) {
        final Stargate gate = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final ServerPlayer player = SGGameTestsNeoForge.playerAt(helper, gate);
        // Only the owner, or a player they let, changes a gate's settings. The mock player is in creative mode, so
        // changes the type, chevrons and iris too, without upgrade items
        gate.setOwner(Optional.of(player.getUUID()), SGGameTestsNeoForge.server(helper));
        ToolActions.configurator(player, new ToolPayloads.ConfiguratorApply(
                gate.position().dimension().identifier().toString(),
                gate.position().pos(), "sgcraft:pegasus",
                8, List.of(new ToolPayloads.Change("sgcraft:open_ticks", Optional.of("1234")),
                        new ToolPayloads.Change("sgcraft:iris", Optional.of("\"sgcraft:metal\"")),
                        new ToolPayloads.Change("sgcraft:one_way_travel", Optional.of("false"))), "", false, ""));
        helper.assertTrue(gate.type().is(GateTypes.PEGASUS), "The type didn't change");
        helper.assertTrue(gate.chevrons() == 8, "The chevrons didn't change");
        helper.assertTrue(gate.get(GateProperties.OPEN_TICKS).orElse(0) == 1234, "The open time didn't change");
        helper.assertTrue(gate.iris().isPresent(), "No iris was fitted");
        helper.assertTrue(!gate.get(GateProperties.ONE_WAY_TRAVEL).orElse(true), "One way travel wasn't turned off");
        // Clearing a value puts the default back
        ToolActions.configurator(player, new ToolPayloads.ConfiguratorApply(
                gate.position().dimension().identifier().toString(),
                gate.position().pos(), "", 0,
                List.of(new ToolPayloads.Change("sgcraft:open_ticks", Optional.empty())), "", false, ""));
        helper.assertTrue(gate.get(GateProperties.OPEN_TICKS).orElse(0) == 6000, "Clearing didn't restore the default");
        helper.succeed();
    }

    private static Stargate placeSupergate(final GameTestHelper helper, final BlockPos at, final int radius) {
        final GlobalPos position = GlobalPos.of(helper.getLevel().dimension(), at);
        final PlacementResult result = SGCraftApi.get().stargates().place(GatePlacement.builder()
                .position(position).orientation(FrontAndTop.NORTH_UP)
                .type(SGGameTestsNeoForge.type(helper, GateTypes.MILKY_WAY)).size(new GateSize(radius))
                .chevrons(9).mode(PlacementMode.FORCE).noFixtures().build(), SGGameTestsNeoForge.server(helper));
        if (result.status() != PlacementResult.Status.PLACED) {
            throw helper.assertionException(Component.literal("Placing a supergate failed: " + result.status()));
        }
        return result.gate().orElseThrow();
    }

    private static void supergatePlaced(final GameTestHelper helper, final BlockPos origin) {
        final Stargate gate = SGGameTestsNeoForge.placeSupergate(helper, origin.offset(SGGameTestsNeoForge.A), 6);
        helper.assertTrue(gate.size().radius() == 6, "The supergate's radius is " + gate.size().radius());
        final BlockPos base = gate.position().pos();
        helper.assertTrue(helper.getLevel().getBlockState(base).is(SGBlocks.SUPERGATE_BASE),
                "The base isn't a supergate base");
        // The top of the ring, straight above the base, is one of its blocks, and merged
        final BlockPos top = base.above(12);
        final BlockState ring = helper.getLevel().getBlockState(top);
        helper.assertTrue(ring.getBlock() instanceof RingBlock block && block.isSupergate()
                && ring.getValue(RingBlock.MERGED), "The top of the ring isn't a merged supergate block: " + ring);
        helper.assertTrue(SGCraftApi.get().stargates().at(GlobalPos.of(helper.getLevel().dimension(), top))
                .isPresent(), "The ring block isn't found as part of the gate");
        // Breaking a block of the ring breaks the gate apart
        helper.getLevel().setBlock(top, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertTrue(SGCraftApi.get().stargates().at(gate.position()).isEmpty(),
                "The supergate is still formed with a block of its ring gone");
        helper.assertTrue(!helper.getLevel().getBlockState(base.above(6).west(6)).getValue(RingBlock.MERGED),
                "The rest of the ring is still merged");
        helper.succeed();
    }

    private static void supergateByHand(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos base = origin.offset(SGGameTestsNeoForge.A);
        level.getChunk(base);
        GateFormation.buildRing(level, new GateFrame(base, Direction.NORTH, GateShape.supergate(5)));
        level.setBlock(base, SGBlocks.SUPERGATE_BASE.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
        final Optional<Stargate> gate = SGCraftApi.get().stargates().at(GlobalPos.of(level.dimension(), base));
        helper.assertTrue(gate.isPresent(), "The supergate didn't form");
        helper.assertTrue(gate.get().size().radius() == 5, "It formed at radius " + gate.get().size().radius());
        helper.succeed();
    }

    private static void supergateCostsMore(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = SGGameTestsNeoForge.placeSupergate(helper, origin.offset(SGGameTestsNeoForge.A), 4);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.B), 9);
        // A supergate runs on the ZPMs in its base
        final Container zpms = ZpmGameTestsNeoForge.fillBase(helper, a);
        final double before = Zpms.energy(Zpms.charged(zpms));
        // Eight times a standard gate's opening, which is at least 4000 gate energy
        SGGameTestsNeoForge.expectDial(helper, a, b, SGGameTestsNeoForge.server(helper), DialResult.CONNECTED, () ->
                helper.assertTrue(before - Zpms.energy(Zpms.charged(zpms)) >= 8 * 4000,
                        "Opening a supergate took too little: " + (before - Zpms.energy(Zpms.charged(zpms)))));
    }

    // A spawner placed by no one builds its gate facing north, at its default size, and picking it gives the spawner
    private static void spawners(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final GateSpawnerBlock stargate = GateSpawnerBlock.forGate(GateTypes.MILKY_WAY, false).orElseThrow();
        final GateSpawnerBlock supergate = GateSpawnerBlock.forGate(GateTypes.PEGASUS, true).orElseThrow();
        final BlockPos a = origin.offset(SGGameTestsNeoForge.A);
        final BlockPos b = origin.offset(SGGameTestsNeoForge.B);
        for (final BlockPos pos : List.of(a, b)) {
            level.getChunk(pos);
            final GateSpawnerBlock spawner = pos.equals(a) ? stargate : supergate;
            level.setBlock(pos, spawner.defaultBlockState(), Block.UPDATE_ALL);
            spawner.setPlacedBy(level, pos, spawner.defaultBlockState(), null, new ItemStack(spawner));
        }
        // A spawner builds its gate on the next tick
        helper.runAfterDelay(2, () -> SGGameTestsNeoForge.spawnersBuilt(helper, level, a, b, supergate));
    }

    private static void spawnersBuilt(final GameTestHelper helper, final ServerLevel level, final BlockPos a,
            final BlockPos b, final GateSpawnerBlock supergate) {
        final Stargate built = SGCraftApi.get().stargates().at(GlobalPos.of(level.dimension(), a)).orElseThrow(() ->
                helper.assertionException(Component.literal("The stargate spawner built nothing")));
        helper.assertTrue(built.size().radius() == GateShape.STANDARD.radius() && built.type().is(GateTypes.MILKY_WAY),
                "The stargate spawner built the wrong gate");
        final Stargate big = SGCraftApi.get().stargates().at(GlobalPos.of(level.dimension(), b)).orElseThrow(() ->
                helper.assertionException(Component.literal("The supergate spawner built nothing")));
        helper.assertTrue(big.size().radius() == GateSpawnerBlock.SUPERGATE_RADIUS && big.type().is(GateTypes.PEGASUS),
                "The supergate spawner built the wrong gate: radius " + big.size().radius());
        final ItemStack picked = level.getBlockState(b.above(2 * GateSpawnerBlock.SUPERGATE_RADIUS))
                .getCloneItemStack(level, b.above(2 * GateSpawnerBlock.SUPERGATE_RADIUS), false);
        helper.assertTrue(picked.is(supergate.asItem()), "Picking the supergate's ring gave " + picked);
        helper.succeed();
    }

    // A gate's template, placed as a structure places it, builds its gate once the world runs: facing as the template
    // has it, its bottom row bare so the ring can turn, and with its DHD fuelled with three naquadah, as the original's
    private static void structureTemplate(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = origin.offset(SGGameTestsNeoForge.A);
        // The spawner builds its gate when its block entity ticks, which needs the chunk kept running
        level.setChunkForced(ChunkPos.containing(at).x(), ChunkPos.containing(at).z(), true);
        level.getChunk(at);
        final boolean placed = level.getStructureTemplateManager().getOrCreate(SGCraft.id("stargate/swamp_platform"))
                .placeInWorld(level, at, at, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS);
        helper.assertTrue(placed, "The template wasn't placed");
        final BlockPos base = at.offset(4, 1, 6);
        final BlockPos dhd = at.offset(6, 1, 2);
        helper.succeedWhen(() -> {
            final Stargate gate = SGCraftApi.get().stargates().at(GlobalPos.of(level.dimension(), base))
                    .orElseThrow(() -> helper.assertionException(Component.literal("The template built no gate: "
                            + level.getBlockState(base))));
            level.setChunkForced(ChunkPos.containing(at).x(), ChunkPos.containing(at).z(), false);
            helper.assertTrue(gate.orientation().front() == Direction.NORTH, "The gate faces " + gate.orientation());
            helper.assertTrue(gate.get(GateProperties.GENERATED).orElse(false), "The gate isn't marked generated");
            helper.assertTrue(level.getBlockEntity(base) instanceof StargateBaseBlockEntity entity
                    && entity.camouflage().isEmpty(), "The gate is camouflaged");
            helper.assertTrue(level.getBlockEntity(dhd) instanceof DhdBlockEntity entity
                    && entity.fuel().getItem(0).is(SGItems.NAQUADAH) && entity.fuel().getItem(0).getCount() == 3,
                    "The DHD doesn't hold three naquadah");
        });
    }

    // A swamp hut set to have a gate gets one a little way off, built once the world runs, with three naquadah in
    // its DHD
    private static void gateBesideSwampHut(final GameTestHelper helper, final BlockPos origin) {
        SGGameTestsNeoForge.structureGate(helper, origin, BuiltinStructures.SWAMP_HUT, 64, (level, base) ->
                helper.assertTrue(BlockPos.betweenClosedStream(base.offset(-8, -2, -8), base.offset(8, 2, 8))
                        .anyMatch(pos -> level.getBlockEntity(pos) instanceof DhdBlockEntity entity
                                && entity.fuel().countItem(SGItems.NAQUADAH) == 3),
                        "The swamp hut's gate has no DHD holding three naquadah"));
    }

    // A desert pyramid set to have a gate gets one in its hall, standing where the pyramid settled onto the ground,
    // its base on the hall's floor
    private static void gateInPyramid(final GameTestHelper helper, final BlockPos origin) {
        SGGameTestsNeoForge.structureGate(helper, origin, BuiltinStructures.DESERT_PYRAMID, 32, (level, base) ->
                helper.assertTrue(level.getBlockState(base.below()).is(Blocks.SMOOTH_SANDSTONE),
                        "The pyramid's gate doesn't stand on the pyramid: " + level.getBlockState(base.below())));
    }

    // Places a structure set to have a gate, and passes once a new gate stands within so many blocks of it
    private static void structureGate(final GameTestHelper helper, final BlockPos origin,
            final ResourceKey<Structure> structure, final int far, final BiConsumer<ServerLevel, BlockPos> check) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = origin.offset(SGGameTestsNeoForge.A);
        final ChunkPos center = ChunkPos.containing(at);
        final int reach = 4;
        // The spawner builds its gate when its block entity ticks, which needs the chunks kept running
        for (int x = -reach; x <= reach; x++) {
            for (int z = -reach; z <= reach; z++) {
                level.setChunkForced(center.x() + x, center.z() + z, true);
            }
        }
        final Set<GlobalPos> before = new HashSet<>();
        SGCraftApi.get().stargates().in(level.dimension()).forEach(gate -> before.add(gate.position()));
        StructureGates.always(true);
        try {
            PlaceCommand.placeStructure(level.getServer().createCommandSourceStack().withLevel(level)
                    .withSuppressedOutput(), level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
                    .getOrThrow(structure), at);
        } catch (final CommandSyntaxException e) {
            throw helper.assertionException(Component.literal(structure.identifier() + " wasn't placed: "
                    + e.getMessage()));
        } finally {
            StructureGates.always(false);
        }
        helper.succeedWhen(() -> {
            final Optional<Stargate> gate = SGCraftApi.get().stargates().in(level.dimension()).stream()
                    .filter(found -> !before.contains(found.position()))
                    .filter(found -> Math.abs(found.position().pos().getX() - at.getX()) <= far
                            && Math.abs(found.position().pos().getZ() - at.getZ()) <= far)
                    .findFirst();
            helper.assertTrue(gate.isPresent(), "No gate was built with " + structure.identifier());
            check.accept(level, gate.get().position().pos());
            for (int x = -reach; x <= reach; x++) {
                for (int z = -reach; z <= reach; z++) {
                    level.setChunkForced(center.x() + x, center.z() + z, false);
                }
            }
        });
    }

    // The village pools of houses hold a gate plaza as one more piece, beside their own
    private static void villagePools(final GameTestHelper helper, final BlockPos origin) {
        final Registry<StructureTemplatePool> pools = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.TEMPLATE_POOL);
        for (final String biome : List.of("plains", "desert", "savanna", "snowy", "taiga")) {
            final StructureTemplatePool pool = pools.getValue(Identifier.withDefaultNamespace("village/" + biome
                    + "/houses"));
            helper.assertTrue(pool != null, "There is no " + biome + " houses pool");
            final String plaza = SGCraft.id("stargate/village_" + biome).toString();
            helper.assertTrue(pool.getTemplates().stream().anyMatch(entry -> entry.getFirst().toString()
                    .contains(plaza)), "The " + biome + " houses pool has no gate plaza");
            helper.assertTrue(pool.getTemplates().size() > 1, "The " + biome + " houses pool lost its houses");
        }
        helper.succeed();
    }

    // A gate whose base broke leaves its ring standing loose, which a spawner in the base's place builds over; a
    // finished gate is still in the way of another
    private static void spawnerRebuilds(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = origin.offset(SGGameTestsNeoForge.A);
        level.getChunk(at);
        SGGameTestsNeoForge.place(helper, at, 9);
        final PlacementResult blocked = SGCraftApi.get().stargates().place(GatePlacement.builder()
                .position(GlobalPos.of(level.dimension(), at)).orientation(FrontAndTop.NORTH_UP)
                .type(SGGameTestsNeoForge.type(helper, GateTypes.MILKY_WAY)).chevrons(9).mode(PlacementMode.FORCE)
                .noFixtures().build(), SGGameTestsNeoForge.server(helper));
        helper.assertTrue(blocked.status() == PlacementResult.Status.GATE_IN_THE_WAY,
                "A finished gate wasn't in the way: " + blocked.status());
        level.destroyBlock(at, false);
        helper.assertTrue(SGCraftApi.get().stargates().at(GlobalPos.of(level.dimension(), at)).isEmpty(),
                "The gate is still known after its base broke");
        final GateSpawnerBlock spawner = GateSpawnerBlock.forGate(GateTypes.MILKY_WAY, false).orElseThrow();
        level.setBlock(at, spawner.defaultBlockState(), Block.UPDATE_ALL);
        spawner.setPlacedBy(level, at, spawner.defaultBlockState(), null, new ItemStack(spawner));
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(SGCraftApi.get().stargates().at(GlobalPos.of(level.dimension(), at)).isPresent(),
                    "The spawner didn't build over the broken gate");
            helper.succeed();
        });
    }

    // A gate placed under water keeps the water: its blocks hold it, and the space inside it stays full
    private static void gateInWater(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = origin.offset(SGGameTestsNeoForge.A);
        level.getChunk(at);
        for (final BlockPos pos : BlockPos.betweenClosed(at.offset(-3, 0, -2), at.offset(3, 5, 2))) {
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        final Stargate gate = SGGameTestsNeoForge.place(helper, at, 9);
        helper.assertTrue(level.getBlockState(gate.position().pos()).getValue(BlockStateProperties.WATERLOGGED),
                "The base isn't waterlogged");
        helper.assertTrue(level.getBlockState(at.above(4)).getValue(BlockStateProperties.WATERLOGGED),
                "The top of the ring isn't waterlogged");
        helper.assertTrue(level.getBlockState(at.above(2)).is(Blocks.WATER), "The inside of the gate lost its water");
        helper.succeed();
    }

    // A gate under water dialing a dry one pours water out of it; real water, as the dry gate allows it, which goes
    // again once the wormhole closes
    private static void fluidPours(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = origin.offset(SGGameTestsNeoForge.A);
        level.getChunk(at);
        for (final BlockPos pos : BlockPos.betweenClosed(at.offset(-3, 0, -2), at.offset(3, 5, 2))) {
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        final Stargate a = SGGameTestsNeoForge.place(helper, at, 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.B), 9);
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.OPENING_DAMAGE, false, SGGameTestsNeoForge.server(helper));
        b.set(GateProperties.SPILLS_FLUID, true, SGGameTestsNeoForge.server(helper));
        final StargateBaseBlockEntity base = (StargateBaseBlockEntity) level.getBlockEntity(b.position().pos());
        // In front of B's opening, a block out, at the bottom
        final BlockPos front = new GateFrame(b.position().pos(), Direction.NORTH).block(0, 1, 1);
        final AtomicReference<DialResult> result = new AtomicReference<>();
        final AtomicBoolean closed = new AtomicBoolean();
        a.dial(SGGameTestsNeoForge.addressOf(b, a), DialMode.QUICK, SGGameTestsNeoForge.server(helper))
                .thenAccept(result::set);
        helper.succeedWhen(() -> {
            if (!closed.get()) {
                helper.assertTrue(result.get() == DialResult.CONNECTED, "The gates haven't connected");
                helper.assertTrue(base.spill().map(s -> s.fluid() == Fluids.WATER && s.amount() > 0.9F).orElse(false),
                        "B isn't pouring water: " + base.spill());
                helper.assertTrue(level.getFluidState(front).is(Fluids.WATER), "No water was put in front of B");
                a.disconnect(SGGameTestsNeoForge.server(helper));
                closed.set(true);
            }
            helper.assertTrue(base.spill().isEmpty(), "B still shows water pouring");
            helper.assertTrue(!level.getFluidState(front).isSource(), "The water in front of B is still there");
        });
    }

    // A gate standing in a pond drains it through the wormhole, down to the floor of its opening, and the far gate
    // stops pouring once it is dry; the water below the opening stays
    private static void pondDrains(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = origin.offset(SGGameTestsNeoForge.A);
        level.getChunk(at);
        for (final BlockPos pos : BlockPos.betweenClosed(at.offset(-4, -1, -2), at.offset(4, 5, 2))) {
            final boolean inside = Math.abs(pos.getX() - at.getX()) < 4 && Math.abs(pos.getZ() - at.getZ()) < 2
                    && pos.getY() >= at.getY() && pos.getY() < at.getY() + 5;
            level.setBlock(pos, (inside ? Blocks.WATER : Blocks.GLASS).defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        final Stargate a = SGGameTestsNeoForge.place(helper, at, 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.B), 9);
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.OPENING_DAMAGE, false, SGGameTestsNeoForge.server(helper));
        final GateFrame frame = new GateFrame(a.position().pos(), Direction.NORTH);
        final BlockPos front = new GateFrame(b.position().pos(), Direction.NORTH).block(0, 1, 1);
        final AtomicReference<DialResult> result = new AtomicReference<>();
        final AtomicBoolean poured = new AtomicBoolean();
        a.dial(SGGameTestsNeoForge.addressOf(b, a), DialMode.QUICK, SGGameTestsNeoForge.server(helper))
                .thenAccept(result::set);
        helper.succeedWhen(() -> {
            helper.assertTrue(result.get() == DialResult.CONNECTED, "The gates haven't connected");
            if (level.getFluidState(front).isSource()) {
                poured.set(true);
            }
            helper.assertTrue(poured.get(), "B never poured the pond's water");
            helper.assertTrue(FluidSpill.in(level, frame).isEmpty(), "A's opening still has water: "
                    + FluidSpill.in(level, frame));
            helper.assertTrue(!level.getFluidState(front).isSource(), "B still pours water");
            helper.assertTrue(level.getFluidState(at.offset(-3, 0, 0)).isSource(),
                    "The water below A's opening drained too");
            a.disconnect(SGGameTestsNeoForge.server(helper));
        });
    }

    // A gate whose opening is a third under water pours out of a third of the far gate's: its bottom row
    private static void poursAsMuchAsCovered(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = origin.offset(SGGameTestsNeoForge.A);
        level.getChunk(at);
        for (final BlockPos pos : BlockPos.betweenClosed(at.offset(-4, -1, -2), at.offset(4, 5, 2))) {
            final boolean inside = Math.abs(pos.getX() - at.getX()) < 4 && Math.abs(pos.getZ() - at.getZ()) < 2
                    && pos.getY() >= at.getY();
            level.setBlock(pos, (!inside ? Blocks.GLASS : pos.getY() < at.getY() + 2 ? Blocks.WATER : Blocks.AIR)
                    .defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        final Stargate a = SGGameTestsNeoForge.place(helper, at, 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.B), 9);
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.OPENING_DAMAGE, false, SGGameTestsNeoForge.server(helper));
        // A keeps its water, so the depth stays as it is
        a.set(GateProperties.SPILLS_FLUID, false, SGGameTestsNeoForge.server(helper));
        final GateFrame far = new GateFrame(b.position().pos(), Direction.NORTH);
        final AtomicReference<DialResult> result = new AtomicReference<>();
        a.dial(SGGameTestsNeoForge.addressOf(b, a), DialMode.QUICK, SGGameTestsNeoForge.server(helper))
                .thenAccept(result::set);
        helper.succeedWhen(() -> {
            helper.assertTrue(result.get() == DialResult.CONNECTED, "The gates haven't connected");
            for (int x = -1; x <= 1; x++) {
                helper.assertTrue(level.getFluidState(far.block(x, 1, 1)).isSource(),
                        "B's bottom row isn't pouring at " + x);
                helper.assertTrue(!level.getFluidState(far.block(x, 2, 1)).isSource(),
                        "B pours from its second row at " + x + ", though only a third of A is under water");
            }
            a.disconnect(SGGameTestsNeoForge.server(helper));
        });
    }

    // Fluid pours out of the front of a gate only: the event horizon fills its opening while the wormhole is open, so
    // none runs through to the back, and goes again once it closes
    private static void poursOutOfTheFront(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = origin.offset(SGGameTestsNeoForge.A);
        final BlockPos dry = origin.offset(SGGameTestsNeoForge.B);
        level.getChunk(at);
        level.getChunk(dry);
        for (final BlockPos pos : BlockPos.betweenClosed(at.offset(-3, 0, -2), at.offset(3, 5, 2))) {
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        for (final BlockPos pos : BlockPos.betweenClosed(dry.offset(-10, -1, -10), dry.offset(10, -1, 10))) {
            level.setBlock(pos, Blocks.GLASS.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        final Stargate a = SGGameTestsNeoForge.place(helper, at, 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, dry, 9);
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.OPENING_DAMAGE, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.SPILLS_FLUID, false, SGGameTestsNeoForge.server(helper));
        final GateFrame far = new GateFrame(b.position().pos(), Direction.NORTH);
        final AtomicReference<DialResult> result = new AtomicReference<>();
        final AtomicBoolean closed = new AtomicBoolean();
        final AtomicInteger pouring = new AtomicInteger();
        a.dial(SGGameTestsNeoForge.addressOf(b, a), DialMode.QUICK, SGGameTestsNeoForge.server(helper))
                .thenAccept(result::set);
        helper.succeedWhen(() -> {
            if (!closed.get()) {
                helper.assertTrue(result.get() == DialResult.CONNECTED, "The gates haven't connected");
                helper.assertTrue(level.getFluidState(far.block(0, 1, 1)).isSource(), "B isn't pouring");
                for (int x = -1; x <= 1; x++) {
                    for (int y = 1; y <= 3; y++) {
                        helper.assertTrue(level.getBlockState(far.block(x, y, 0)).is(SGBlocks.EVENT_HORIZON),
                                "B's opening at " + x + ", " + y + " isn't the event horizon");
                        helper.assertTrue(level.getFluidState(far.block(x, y, -1)).isEmpty(),
                                "Water ran out of the back of B at " + x + ", " + y);
                    }
                }
                // Long enough for water to have run through, had it been able to
                helper.assertTrue(pouring.incrementAndGet() > 60, "Still watching B pour");
                a.disconnect(SGGameTestsNeoForge.server(helper));
                closed.set(true);
            }
            helper.assertTrue(b.state() == GateState.IDLE, "B hasn't closed");
            helper.assertTrue(level.getBlockState(far.block(0, 2, 0)).isAir(), "B's opening wasn't emptied");
        });
    }

    // Water running into the front of an open gate's horizon goes through and pours out of the far gate, and its
    // source drains away; water at the back of the gate stays where it is
    private static void horizonTakesFluidIn(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = origin.offset(SGGameTestsNeoForge.A);
        final BlockPos dry = origin.offset(SGGameTestsNeoForge.B);
        level.getChunk(at);
        level.getChunk(dry);
        for (final BlockPos floor : List.of(at, dry)) {
            for (final BlockPos pos : BlockPos.betweenClosed(floor.offset(-10, -1, -10), floor.offset(10, -1, 10))) {
                level.setBlock(pos, Blocks.GLASS.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        final Stargate a = SGGameTestsNeoForge.place(helper, at, 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, dry, 9);
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.OPENING_DAMAGE, false, SGGameTestsNeoForge.server(helper));
        final GateFrame near = new GateFrame(a.position().pos(), Direction.NORTH);
        final GateFrame far = new GateFrame(b.position().pos(), Direction.NORTH);
        final BlockPos front = near.block(0, 1, 1);
        final BlockPos back = near.block(0, 1, -1);
        final AtomicReference<DialResult> result = new AtomicReference<>();
        final AtomicBoolean placed = new AtomicBoolean();
        final AtomicBoolean poured = new AtomicBoolean();
        a.dial(SGGameTestsNeoForge.addressOf(b, a), DialMode.QUICK, SGGameTestsNeoForge.server(helper))
                .thenAccept(result::set);
        helper.succeedWhen(() -> {
            helper.assertTrue(result.get() == DialResult.CONNECTED, "The gates haven't connected");
            helper.assertTrue(EventHorizonBlock.isSending(level.getBlockState(near.block(0, 1, 0))),
                    "A's horizon isn't open to fluid");
            if (!placed.getAndSet(true)) {
                // Water on a step in front of the horizon, and on one behind it
                for (final BlockPos pos : List.of(front, back)) {
                    level.setBlock(pos.below(), Blocks.GLASS.defaultBlockState(), Block.UPDATE_ALL);
                    level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
            if (level.getFluidState(far.block(0, 1, 1)).isSource()) {
                poured.set(true);
            }
            helper.assertTrue(poured.get(), "B never poured the water that ran into A");
            helper.assertTrue(!level.getFluidState(front).isSource(), "The water in front of A didn't drain");
            helper.assertTrue(level.getFluidState(back).isSource(), "The water behind A went too");
            a.disconnect(SGGameTestsNeoForge.server(helper));
        });
    }

    // A bucket never replaces the event horizon: emptied into the front of the dialing gate it pours in front, to run
    // in from there; into its back, or into the dialed gate, which doesn't send, it is refused
    private static void bucketsKeepTheHorizon(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = origin.offset(SGGameTestsNeoForge.A);
        final BlockPos dry = origin.offset(SGGameTestsNeoForge.B);
        level.getChunk(at);
        level.getChunk(dry);
        final Stargate a = SGGameTestsNeoForge.place(helper, at, 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, dry, 9);
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.OPENING_DAMAGE, false, SGGameTestsNeoForge.server(helper));
        final GateFrame near = new GateFrame(a.position().pos(), Direction.NORTH);
        final GateFrame far = new GateFrame(b.position().pos(), Direction.NORTH);
        final BucketItem bucket = (BucketItem) Items.WATER_BUCKET;
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final AtomicReference<DialResult> result = new AtomicReference<>();
        a.dial(SGGameTestsNeoForge.addressOf(b, a), DialMode.QUICK, SGGameTestsNeoForge.server(helper))
                .thenAccept(result::set);
        helper.succeedWhen(() -> {
            helper.assertTrue(result.get() == DialResult.CONNECTED, "The gates haven't connected");
            helper.assertTrue(EventHorizonBlock.isSending(level.getBlockState(near.block(0, 2, 0))),
                    "A's horizon isn't open to fluid");
            player.snapTo(Vec3.atCenterOf(far.block(0, 2, 2)));
            helper.assertFalse(bucket.emptyContents(player, level, far.block(0, 2, 0), null),
                    "A bucket was emptied into B, which doesn't send");
            player.snapTo(Vec3.atCenterOf(near.block(0, 2, -2)));
            helper.assertFalse(bucket.emptyContents(player, level, near.block(0, 2, 0), null),
                    "A bucket was emptied into the back of A");
            player.snapTo(Vec3.atCenterOf(near.block(0, 2, 3)));
            helper.assertTrue(bucket.emptyContents(player, level, near.block(0, 2, 0), null),
                    "A bucket couldn't be emptied into the front of A");
            for (final GateFrame frame : List.of(near, far)) {
                helper.assertTrue(level.getBlockState(frame.block(0, 2, 0)).is(SGBlocks.EVENT_HORIZON),
                        "A bucket replaced the event horizon");
            }
            helper.assertTrue(level.getFluidState(near.block(0, 2, 1)).is(Fluids.WATER),
                    "The bucket emptied into A didn't pour in front of it");
            helper.assertTrue(level.getFluidState(near.block(0, 2, -1)).isEmpty(), "Water got behind A");
            a.disconnect(SGGameTestsNeoForge.server(helper));
        });
    }

    // A spawner placed on the floor of a sea, with plants growing in it, builds its gate there
    private static void spawnerInWater(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = origin.offset(SGGameTestsNeoForge.A);
        level.getChunk(at);
        for (final BlockPos pos : BlockPos.betweenClosed(at.offset(-4, -1, -4), at.offset(4, 7, 4))) {
            level.setBlock(pos, pos.getY() < at.getY() ? Blocks.SAND.defaultBlockState()
                    : Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.setBlock(at.east(), Blocks.SEAGRASS.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(at.above().west(), Blocks.SEAGRASS.defaultBlockState(), Block.UPDATE_ALL);
        final GateSpawnerBlock spawner = GateSpawnerBlock.forGate(GateTypes.MILKY_WAY, false).orElseThrow();
        // Placed as a player places it, clicking the sea floor
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(Vec3.atCenterOf(at.north(3)), 0, 30);
        final ItemStack stack = new ItemStack(spawner);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(at.below()).add(0, 0.5, 0), Direction.UP, at.below(), false));
        helper.runAfterDelay(2, () -> SGGameTestsNeoForge.spawnedInWater(helper, level, at));
    }

    private static void spawnedInWater(final GameTestHelper helper, final ServerLevel level, final BlockPos at) {
        helper.assertTrue(SGCraftApi.get().stargates().at(GlobalPos.of(level.dimension(), at)).isPresent(),
                "The spawner built no gate in water: " + SGGameTestsNeoForge.describe(level, at));
        helper.assertTrue(level.getFluidState(at).is(Fluids.WATER),
                "The base of a gate built in water holds no water: " + level.getBlockState(at));
        helper.succeed();
    }

    // What is in each block of a standard gate's square, for a failure message
    private static String describe(final ServerLevel level, final BlockPos base) {
        final StringBuilder out = new StringBuilder();
        for (int y = 4; y >= 0; y--) {
            for (int x = -2; x <= 2; x++) {
                out.append(level.getBlockState(base.relative(Direction.WEST, x).above(y))).append(' ');
            }
            out.append('/');
        }
        return out.toString();
    }

    private static void camouflage(final GameTestHelper helper, final BlockPos origin) {
        final Stargate gate = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final StargateBaseBlockEntity base = (StargateBaseBlockEntity) helper.getLevel()
                .getBlockEntity(gate.position().pos());
        helper.assertTrue(base != null, "The gate has no base");
        final Container camouflage = base.camouflage();
        helper.assertTrue(camouflage.canPlaceItem(0, new ItemStack(Items.STONE_BRICKS)), "Stone bricks aren't allowed");
        helper.assertTrue(!camouflage.canPlaceItem(0, new ItemStack(Items.STICK)), "A stick is allowed");
        helper.assertTrue(!camouflage.canPlaceItem(0, new ItemStack(SGBlocks.STARGATE_RING)),
                "A ring block is allowed");
        camouflage.setItem(4, new ItemStack(Items.STONE_BRICKS));
        helper.assertTrue(base.camouflageState(4).map(state -> state.is(Blocks.STONE_BRICKS)).orElse(false),
                "The camouflage isn't drawn as stone bricks");
        helper.assertTrue(base.camouflageState(0).isEmpty(), "An empty slot has camouflage");
        helper.succeed();
    }

    private static void gdoIris(final GameTestHelper helper, final BlockPos origin) {
        final Stargate gate = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        gate.set(GateProperties.IRIS, IrisTypes.METAL, SGGameTestsNeoForge.server(helper));
        final ServerPlayer player = SGGameTestsNeoForge.playerAt(helper, gate);
        ToolActions.gdo(player, new ToolPayloads.GdoAction(gate.position().pos(),
                ToolPayloads.GdoAction.Kind.CLOSE_LOCAL));
        helper.assertTrue(gate.iris().orElseThrow().state() == IrisState.CLOSING, "The GDO didn't close the iris");
        helper.succeed();
    }

    // A Tok'ra put at a gate keeps its job and trades the original's crystals, and a DHD is a place only a Tok'ra
    // works at
    private static void tokra(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = origin.offset(SGGameTestsNeoForge.A);
        level.getChunk(at);
        final Stargate gate = SGGameTestsNeoForge.place(helper, at, 9);
        for (int x = -3; x <= 3; x++) {
            for (int z = -6; z <= -1; z++) {
                level.setBlock(at.offset(x, -1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        final BlockPos dhd = at.offset(2, 0, -4);
        level.setBlock(dhd, SGBlocks.DHD.defaultBlockState(), Block.UPDATE_ALL);
        final Villager tokra = SGVillagers.spawnTokra(level, at, gate.orientation().front())
                .orElseThrow(() -> helper.assertionException(Component.literal("There was no room for the Tok'ra")));
        helper.assertTrue(tokra.getVillagerData().profession().is(SGVillagers.TOKRA),
                "The villager is a " + tokra.getVillagerData().profession());
        final MerchantOffers offers = tokra.getOffers();
        helper.assertTrue(offers.stream().anyMatch(offer -> offer.getResult().is(SGItems.CORE_CRYSTAL))
                        && offers.stream().anyMatch(offer -> offer.getResult().is(SGItems.CONTROLLER_CRYSTAL)),
                "The Tok'ra doesn't trade both crystals: " + offers.size() + " trades");
        final Holder<PoiType> poi = level.getPoiManager().getType(dhd)
                .orElseThrow(() -> helper.assertionException(Component.literal("The DHD isn't a place to work")));
        helper.assertTrue(poi.is(SGVillagers.DHD), "The DHD is a " + poi);
        helper.assertTrue(tokra.getVillagerData().profession().value().acquirableJobSite().test(poi),
                "A Tok'ra can't work at a DHD");
        helper.assertTrue(!VillagerProfession.ALL_ACQUIRABLE_JOBS.test(poi), "Any villager can work at a DHD");
        helper.succeedWhen(() -> {
            helper.assertTrue(tokra.getVillagerData().profession().is(SGVillagers.TOKRA), "The Tok'ra lost its job");
            tokra.discard();
        });
    }

    // A chevron upgrade used on a gate's ring adds one chevron, seven to eight and eight to nine, and no more
    // Breaking a gate with its wormhole open sets off an explosion in its ring; a small one here, that breaks
    // nothing, harms a pig in front of it
    private static void breach(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.B), 9);
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.OPENING_DAMAGE, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.BREACH_EXPLOSION, 3.0, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.BREACH_FIRE, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.BREACH_BREAKS_BLOCKS, false, SGGameTestsNeoForge.server(helper));
        final AtomicReference<DialResult> result = new AtomicReference<>();
        a.dial(SGGameTestsNeoForge.addressOf(b, a), DialMode.QUICK, SGGameTestsNeoForge.server(helper))
                .thenAccept(result::set);
        helper.runAfterDelay(SGIris.MOVE_TICKS + 60, () -> {
            helper.assertTrue(result.get() == DialResult.CONNECTED, "The dial ended " + result.get());
            final Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, BlockPos.ZERO.above());
            pig.setNoGravity(true);
            final Vec3 front = new GateFrame(a.position().pos(), Direction.NORTH).toWorld(new Vec3(0, 2.5, 2));
            pig.setPos(front.x, front.y, front.z);
            helper.getLevel().destroyBlock(a.position().pos(), false);
            helper.assertTrue(!pig.isAlive() || pig.getHealth() < pig.getMaxHealth(),
                    "Breaking the open gate didn't harm the pig in front of it");
            helper.assertTrue(b.state() == GateState.IDLE || b.state() == GateState.CLOSING,
                    "Gate B is still " + b.state());
            helper.succeed();
        });
    }

    // Breaking the gate an open wormhole reaches sends the wormhole to the other gate in its address cell, which
    // opens without a new dial; the dialing gate stays connected
    private static void wormholeJumps(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final BlockPos atB = origin.offset(SGGameTestsNeoForge.B);
        final Stargate b = SGGameTestsNeoForge.place(helper, atB, 9);
        // Four chunks away along z, in the same cell as gate B
        final int cell = SGCraftApi.get().addressing().partition(helper.getLevel().dimension()).cellChunks();
        final int chunkZ = SectionPos.blockToSectionCoord(atB.getZ());
        final int cellZ = Math.floorDiv(chunkZ, cell) * cell;
        final int otherZ = cellZ + Math.floorMod(chunkZ - cellZ + cell / 2, cell);
        final Stargate c = SGGameTestsNeoForge.place(helper,
                new BlockPos(atB.getX(), atB.getY(), otherZ * 16 + Math.floorMod(atB.getZ(), 16)), 9);
        helper.assertTrue(SGGameTestsNeoForge.addressOf(b, a).equals(SGGameTestsNeoForge.addressOf(c, a)),
                "Gates B and C aren't in one cell");
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.OPENING_DAMAGE, false, SGGameTestsNeoForge.server(helper));
        b.set(GateProperties.BREACH_EXPLOSION, 0.0, SGGameTestsNeoForge.server(helper));
        c.set(GateProperties.BREACH_EXPLOSION, 0.0, SGGameTestsNeoForge.server(helper));
        final AtomicReference<DialResult> result = new AtomicReference<>();
        a.dial(SGGameTestsNeoForge.addressOf(b, a), DialMode.QUICK, SGGameTestsNeoForge.server(helper))
                .thenAccept(result::set);
        helper.runAfterDelay(SGIris.MOVE_TICKS + 60, () -> {
            helper.assertTrue(result.get() == DialResult.CONNECTED, "The dial ended " + result.get());
            final Stargate reached = b.state() == GateState.CONNECTED ? b : c;
            final Stargate other = reached == b ? c : b;
            helper.getLevel().destroyBlock(reached.position().pos(), false);
            helper.assertTrue(a.state() == GateState.CONNECTED, "The dialing gate is " + a.state());
            helper.assertTrue(other.state() == GateState.CONNECTED, "The other gate in the cell is "
                    + other.state());
            helper.assertTrue(a.connection().map(connection -> connection.target().equals(other)).orElse(false),
                    "The wormhole doesn't reach the other gate");
            a.disconnect(SGGameTestsNeoForge.server(helper));
            helper.succeed();
        });
    }

    // A new PDD takes the server's default addresses when first used, keeps a locked one, and stays empty once
    // emptied
    private static void defaultAddresses(final GameTestHelper helper, final BlockPos origin) {
        final SGServer server = SGServer.require();
        final List<DefaultAddresses.Entry> before = server.defaultAddresses();
        final AddressBookEntry home = new AddressBookEntry(Address.parse("ABCD-EFG").orElseThrow(),
                Component.literal("Home"));
        server.setDefaultAddresses(List.of(new DefaultAddresses.Entry(home, true)));
        try {
            final ServerPlayer player = helper.makeMockServerPlayerInLevel();
            final ItemStack pdd = new ItemStack(SGItems.PDD);
            player.setItemInHand(InteractionHand.MAIN_HAND, pdd);
            ToolActions.openPdd(player);
            helper.assertTrue(pdd.getOrDefault(SGDataComponents.ADDRESSES, List.of()).equals(List.of(home)),
                    "The new PDD holds " + pdd.get(SGDataComponents.ADDRESSES));
            helper.assertTrue(ItemAddressBook.of(pdd).remove(home.address(), SGGameTestsNeoForge.server(helper))
                    == ActionResult.DENIED, "The locked address could be removed");
            pdd.set(SGDataComponents.ADDRESSES, List.of());
            ToolActions.openPdd(player);
            helper.assertTrue(pdd.getOrDefault(SGDataComponents.ADDRESSES, List.of()).isEmpty(),
                    "The emptied PDD took the default addresses again");
        } finally {
            server.setDefaultAddresses(before);
        }
        helper.succeed();
    }

    // A broken base gives back the upgrades fitted with items: two chevron upgrades used on a seven chevron gate, and
    // an iris upgrade. Chevrons and an iris set without the items, as commands and operators set them, give nothing
    // back.
    private static void baseDropsUpgrades(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = origin.offset(SGGameTestsNeoForge.A);
        level.getChunk(at);
        final Stargate gate = SGGameTestsNeoForge.place(helper, at, 7);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(Vec3.atCenterOf(at.north(3)), 180, 0);
        gate.setOwner(Optional.of(player.getUUID()), SGGameTestsNeoForge.server(helper));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SGItems.CHEVRON_UPGRADE));
        for (int use = 0; use < 2; use++) {
            SGGameTestsNeoForge.useOnRing(player, level, at);
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SGItems.IRIS_UPGRADE));
        SGGameTestsNeoForge.useOnRing(player, level, at);
        helper.assertTrue(gate.chevrons() == 9 && gate.iris().isPresent(), "The upgrades weren't fitted");
        final BlockPos base = gate.position().pos();
        level.destroyBlock(base, false);
        final int chevrons = SGGameTestsNeoForge.dropped(level, base, SGItems.CHEVRON_UPGRADE);
        final int irises = SGGameTestsNeoForge.dropped(level, base, SGItems.IRIS_UPGRADE);
        helper.assertTrue(chevrons == 2 && irises == 1, "The base dropped " + chevrons + " chevron upgrades and "
                + irises + " iris upgrades");
        final BlockPos otherAt = origin.offset(SGGameTestsNeoForge.B);
        level.getChunk(otherAt);
        final Stargate given = SGGameTestsNeoForge.place(helper, otherAt, 7);
        given.setChevrons(9, SGGameTestsNeoForge.server(helper));
        given.set(GateProperties.IRIS, IrisTypes.METAL, SGGameTestsNeoForge.server(helper));
        final BlockPos givenBase = given.position().pos();
        level.destroyBlock(givenBase, false);
        final int givenChevrons = SGGameTestsNeoForge.dropped(level, givenBase, SGItems.CHEVRON_UPGRADE);
        final int givenIrises = SGGameTestsNeoForge.dropped(level, givenBase, SGItems.IRIS_UPGRADE);
        helper.assertTrue(givenChevrons == 0 && givenIrises == 0, "A gate given its chevrons and iris dropped "
                + givenChevrons + " chevron upgrades and " + givenIrises + " iris upgrades");
        helper.succeed();
    }

    // Uses the held item on the ring beside a gate's base, as a player would
    private static void useOnRing(final ServerPlayer player, final ServerLevel level, final BlockPos base) {
        player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(base.east()), Direction.NORTH, base.east(), false));
    }

    // How many of an item lie dropped around a broken base
    private static int dropped(final ServerLevel level, final BlockPos base, final Item item) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(base).inflate(2)).stream()
                .filter(drop -> drop.getItem().is(item)).mapToInt(drop -> drop.getItem().getCount()).sum();
    }

    // A Pegasus upgrade used on a Milky Way gate turns it Pegasus and is used up; a second does nothing
    private static void typeUpgrade(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = origin.offset(SGGameTestsNeoForge.A);
        level.getChunk(at);
        final Stargate gate = SGGameTestsNeoForge.place(helper, at, 9);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(Vec3.atCenterOf(at.north(3)), 180, 0);
        gate.setOwner(Optional.of(player.getUUID()), SGGameTestsNeoForge.server(helper));
        // In survival, where using an upgrade uses it up
        player.setGameMode(GameType.SURVIVAL);
        final ItemStack stack = new ItemStack(SGItems.PEGASUS_UPGRADE, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        for (int use = 0; use < 2; use++) {
            player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(at.east()), Direction.NORTH, at.east(), false));
        }
        helper.assertTrue(gate.type().is(GateTypes.PEGASUS), "The gate is " + gate.type());
        helper.assertTrue(stack.getCount() == 1, "The upgrades left are " + stack.getCount() + ", not 1");
        // The ring broken and built again forms the gate it was, Pegasus
        final BlockState ring = level.getBlockState(at.east());
        level.destroyBlock(at.east(), false);
        level.setBlock(at.east(), ring.getBlock().defaultBlockState(), Block.UPDATE_ALL);
        final Optional<Stargate> formed = SGCraftApi.get().stargates().at(gate.position());
        helper.assertTrue(formed.isPresent(), "The gate didn't form again");
        helper.assertTrue(formed.get().type().is(GateTypes.PEGASUS), "The gate formed again as "
                + formed.get().type());
        // The base gives the upgrade back
        level.destroyBlock(gate.position().pos(), false);
        final int upgrades = SGGameTestsNeoForge.dropped(level, gate.position().pos(), SGItems.PEGASUS_UPGRADE);
        helper.assertTrue(upgrades == 1, "The base dropped " + upgrades + " Pegasus upgrades, not 1");
        helper.succeed();
    }

    private static void chevronUpgrade(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = origin.offset(SGGameTestsNeoForge.A);
        level.getChunk(at);
        final Stargate gate = SGGameTestsNeoForge.place(helper, at, 7);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(Vec3.atCenterOf(at.north(3)), 180, 0);
        gate.setOwner(Optional.of(player.getUUID()), SGGameTestsNeoForge.server(helper));
        final ItemStack stack = new ItemStack(SGItems.CHEVRON_UPGRADE, 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        for (final int expected : new int[] {8, 9, 9}) {
            player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(at.east()), Direction.NORTH, at.east(), false));
            helper.assertTrue(gate.chevrons() == expected, "The gate has " + gate.chevrons() + " chevrons, not "
                    + expected);
        }
        helper.succeed();
    }

    // A shut iris is solid across the gate's opening, and an open one isn't
    private static void shutIrisIsSolid(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final Stargate gate = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        // The base puts the plates up as it ticks, which needs its chunk kept running
        final ChunkPos chunk = ChunkPos.containing(gate.position().pos());
        level.setChunkForced(chunk.x(), chunk.z(), true);
        gate.set(GateProperties.IRIS, IrisTypes.METAL, SGGameTestsNeoForge.server(helper));
        gate.iris().orElseThrow().close(SGGameTestsNeoForge.server(helper));
        final GateFrame frame = new GateFrame(gate.position().pos(), Direction.NORTH);
        // A box where a traveler's middle would be, just through the horizon
        final AABB middle = frame.box(new Vec3(-0.3, 1.5, -0.3), new Vec3(0.3, 2.5, 0.3));
        helper.runAfterDelay(SGIris.MOVE_TICKS + 5, () -> {
            helper.assertTrue(!level.getEntityCollisions(null, middle).isEmpty(), "The shut iris isn't solid");
            gate.iris().orElseThrow().open(SGGameTestsNeoForge.server(helper));
        });
        helper.runAfterDelay(2 * SGIris.MOVE_TICKS + 10, () -> {
            level.setChunkForced(chunk.x(), chunk.z(), false);
            helper.assertTrue(level.getEntityCollisions(null, middle).isEmpty(), "The open iris is still solid");
            helper.succeed();
        });
    }

    // A plate its gate doesn't hold, such as one left loaded while the gate unloaded and put down new plates, goes
    // away by itself, so opening the iris leaves no plate standing
    private static void orphanIrisPlateGoes(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final Stargate gate = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final BlockPos base = gate.position().pos();
        // The base and its plates tick only in a running chunk
        final ChunkPos chunk = ChunkPos.containing(base);
        level.setChunkForced(chunk.x(), chunk.z(), true);
        gate.set(GateProperties.IRIS, IrisTypes.METAL, SGGameTestsNeoForge.server(helper));
        gate.iris().orElseThrow().close(SGGameTestsNeoForge.server(helper));
        final GateFrame frame = new GateFrame(base, Direction.NORTH);
        final AABB around = new AABB(base).inflate(8);
        final AtomicReference<IrisPlateEntity> orphan = new AtomicReference<>();
        helper.runAfterDelay(SGIris.MOVE_TICKS + 5, () -> {
            final List<IrisPlateEntity> held = level.getEntitiesOfClass(IrisPlateEntity.class, around);
            helper.assertTrue(!held.isEmpty(), "The shut iris put up no plates");
            if (!(level.getBlockEntity(base) instanceof StargateBaseBlockEntity entity)) {
                throw helper.assertionException(Component.literal("The gate has no base"));
            }
            final IrisPlateEntity extra = IrisPlateEntity.create(level, frame.toWorld(new Vec3(0, 2.5, 0.05)),
                    frame.front(), frame.top(), 1, 1, base);
            level.addFreshEntity(extra);
            helper.assertTrue(!entity.holdsPlate(extra), "The base holds a plate it never put up");
            orphan.set(extra);
            gate.iris().orElseThrow().open(SGGameTestsNeoForge.server(helper));
        });
        helper.runAfterDelay(2 * SGIris.MOVE_TICKS + 45, () -> {
            level.setChunkForced(chunk.x(), chunk.z(), false);
            helper.assertTrue(orphan.get().isRemoved(), "The plate the base doesn't hold is still there");
            final List<IrisPlateEntity> left = level.getEntitiesOfClass(IrisPlateEntity.class, around);
            helper.assertTrue(left.isEmpty(), left.size() + " plates still stand across the open iris");
            helper.succeed();
        });
    }

    // A power unit beside a gate takes FE from a cable, as NeoForge's energy reaches it, keeps it if the cable takes
    // it back, and pays for the gate's dialing from it
    private static void powerUnit(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final Stargate a = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.B), 9);
        final BlockPos at = a.position().pos().north(2).east(3);
        level.setBlock(at, SGBlocks.POWER_UNIT.defaultBlockState(), Block.UPDATE_ALL);
        final EnergyHandler cable = level.getCapability(Capabilities.Energy.BLOCK, at, Direction.UP);
        helper.assertTrue(cable != null, "The power unit takes no FE");
        try (Transaction undone = Transaction.openRoot()) {
            cable.insert(1000, undone);
        }
        helper.assertTrue(cable.getAmountAsLong() == 0, "An undone transfer left " + cable.getAmountAsLong() + " FE");
        try (Transaction transfer = Transaction.openRoot()) {
            helper.assertTrue(cable.insert(1_000_000, transfer) == 1_000_000, "The unit refused FE");
            transfer.commit();
        }
        helper.assertTrue(cable.getAmountAsLong() == 1_000_000, "The unit holds " + cable.getAmountAsLong() + " FE");
        try (Transaction taken = Transaction.openRoot()) {
            helper.assertTrue(cable.extract(1000, taken) == 0, "A cable drew FE back out of the unit");
        }
        SGGameTestsNeoForge.expectDial(helper, a, b, SGGameTestsNeoForge.server(helper), DialResult.CONNECTED, () ->
                helper.assertTrue(cable.getAmountAsLong() < 1_000_000,
                        "Dialing took nothing from the unit: " + cable.getAmountAsLong() + " FE left"));
    }

    // An open gate lights the world round it: the ring under its lit chevrons, and its horizon, whose light rises
    // and falls; once it shuts, the light goes
    private static void gateGivesLight(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final Stargate a = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.B), 9);
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.OPENING_DAMAGE, false, SGGameTestsNeoForge.server(helper));
        final GateFrame frame = new GateFrame(a.position().pos(), Direction.NORTH);
        final BlockPos top = frame.block(0, 4, 0);
        final BlockPos middle = frame.block(0, 2, 0);
        helper.assertTrue(level.getBlockState(top).getLightEmission(level, top) == 0,
                "A's ring gives light before it dials");
        final AtomicReference<DialResult> result = new AtomicReference<>();
        final AtomicBoolean closed = new AtomicBoolean();
        final Set<Integer> levels = new HashSet<>();
        a.dial(SGGameTestsNeoForge.addressOf(b, a), DialMode.QUICK, SGGameTestsNeoForge.server(helper))
                .thenAccept(result::set);
        helper.succeedWhen(() -> {
            if (!closed.get()) {
                helper.assertTrue(result.get() == DialResult.CONNECTED, "The gates haven't connected");
                helper.assertTrue(level.getBlockState(top).getLightEmission(level, top) == GateLight.CHEVRON,
                        "A's top chevron doesn't light the ring");
                final BlockState horizon = level.getBlockState(middle);
                helper.assertTrue(horizon.is(SGBlocks.EVENT_HORIZON), "A's opening isn't the event horizon");
                levels.add(horizon.getLightEmission(level, middle));
                helper.assertTrue(levels.size() >= 3, "A's horizon hasn't wavered, only " + levels);
                helper.assertTrue(level.getBrightness(LightLayer.BLOCK, frame.block(0, 2, 2)) > 0,
                        "Nothing in front of A is lit");
                a.disconnect(SGGameTestsNeoForge.server(helper));
                closed.set(true);
            }
            helper.assertTrue(a.state() == GateState.IDLE, "A hasn't closed");
            helper.assertTrue(level.getBlockState(top).getLightEmission(level, top) == 0,
                    "A's ring still gives light");
            helper.assertTrue(level.getBlockState(middle).isAir(), "A's opening wasn't emptied");
        });
    }

    // A gate lying flat at a base facing north, its ring running south from it
    private static Stargate placeFlat(final GameTestHelper helper, final BlockPos at, final FrontAndTop orientation) {
        final PlacementResult result = SGCraftApi.get().stargates().place(GatePlacement.builder()
                .position(GlobalPos.of(helper.getLevel().dimension(), at)).orientation(orientation)
                .type(SGGameTestsNeoForge.type(helper, GateTypes.MILKY_WAY))
                .chevrons(9).mode(PlacementMode.FORCE).noFixtures().build(), SGGameTestsNeoForge.server(helper));
        if (result.status() != PlacementResult.Status.PLACED) {
            throw helper.assertionException(Component.literal("Placing a flat gate failed: " + result.status()
                    + " " + result.reason().map(Component::getString).orElse("")));
        }
        return result.gate().orElseThrow();
    }

    // A ring laid flat by hand behind a base forms facing up, its ring at the base's height
    private static void flatGateForms(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos base = origin.offset(SGGameTestsNeoForge.A);
        level.getChunk(base);
        GateFormation.buildRing(level, new GateFrame(base, Direction.NORTH, GateOrientation.FACE_UP,
                GateShape.STANDARD));
        level.setBlock(base, SGBlocks.STARGATE_BASE.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
        final Optional<Stargate> gate = SGCraftApi.get().stargates().at(GlobalPos.of(level.dimension(), base));
        helper.assertTrue(gate.isPresent(), "The flat ring didn't form");
        helper.assertTrue(gate.get().orientation() == FrontAndTop.UP_SOUTH,
                "The flat gate formed " + gate.get().orientation() + ", not facing up");
        // The top of the ring is four blocks behind the base, at its height
        final BlockState top = level.getBlockState(base.south(4));
        helper.assertTrue(top.is(SGBlocks.STARGATE_CHEVRON) && top.getValue(RingBlock.MERGED),
                "The top of the ring isn't a merged chevron: " + top);
        helper.assertTrue(SGCraftApi.get().stargates().at(GlobalPos.of(level.dimension(), base.south(4).west(2)))
                .isPresent(), "The flat ring's corner isn't part of the gate");
        helper.assertTrue(level.getBlockEntity(base) instanceof StargateBaseBlockEntity entity
                && entity.orientation() == GateOrientation.FACE_UP, "The base doesn't hold the gate lying face up");
        helper.succeed();
    }

    // The configurator, the command and the API turn a flat gate face down and up; one standing doesn't turn, and a
    // flat ring broken and mended forms as it was turned
    private static void flatGateTurns(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final Stargate gate = SGGameTestsNeoForge.placeFlat(helper, origin.offset(SGGameTestsNeoForge.A),
                FrontAndTop.UP_SOUTH);
        final BlockPos base = gate.position().pos();
        final ServerPlayer player = SGGameTestsNeoForge.playerAt(helper, gate);
        gate.setOwner(Optional.of(player.getUUID()), SGGameTestsNeoForge.server(helper));
        ToolActions.configurator(player, new ToolPayloads.ConfiguratorApply(
                gate.position().dimension().identifier().toString(), base, "", 0, List.of(), "", false,
                "face_down"));
        helper.assertTrue(gate.orientation() == FrontAndTop.DOWN_SOUTH,
                "The configurator didn't turn the gate face down: " + gate.orientation());
        helper.assertTrue(level.getBlockEntity(base) instanceof StargateBaseBlockEntity entity
                && entity.orientation() == GateOrientation.FACE_DOWN, "The base doesn't hold the gate face down");
        level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack()
                .withLevel(level).withSuppressedOutput(), "sgcraft gate " + base.getX() + " " + base.getY() + " "
                + base.getZ() + " orientation face_up");
        helper.assertTrue(gate.orientation() == FrontAndTop.UP_SOUTH,
                "The command didn't turn the gate face up: " + gate.orientation());
        final Cause cause = SGGameTestsNeoForge.server(helper);
        helper.assertTrue(gate.setOrientation(FrontAndTop.DOWN_SOUTH, cause) == ActionResult.DONE,
                "The API didn't turn the gate face down");
        helper.assertTrue(gate.setOrientation(FrontAndTop.DOWN_SOUTH, cause) == ActionResult.NO_CHANGE,
                "Turning the gate the way it lies changed it");
        helper.assertTrue(gate.setOrientation(FrontAndTop.DOWN_NORTH, cause) == ActionResult.NOT_POSSIBLE
                && gate.setOrientation(FrontAndTop.SOUTH_UP, cause) == ActionResult.NOT_POSSIBLE,
                "The flat gate turned off its ring");
        final Stargate standing = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.B), 9);
        helper.assertTrue(standing.setOrientation(FrontAndTop.UP_SOUTH, cause) == ActionResult.NOT_POSSIBLE,
                "A standing gate lay down");
        final BlockPos top = base.south(4);
        level.destroyBlock(top, false);
        helper.assertTrue(SGCraftApi.get().stargates().at(gate.position()).isEmpty(),
                "The gate is still known after its ring broke");
        level.setBlock(top, SGBlocks.STARGATE_CHEVRON.defaultBlockState(), Block.UPDATE_ALL);
        final Optional<Stargate> mended = SGCraftApi.get().stargates().at(gate.position());
        helper.assertTrue(mended.isPresent() && mended.get().orientation() == FrontAndTop.DOWN_SOUTH,
                "The mended ring didn't form face down: " + mended.map(Stargate::orientation));
        final Stargate down = SGGameTestsNeoForge.placeFlat(helper, origin.offset(SGGameTestsNeoForge.B).offset(0, 0,
                16),
                FrontAndTop.DOWN_SOUTH);
        helper.assertTrue(down.orientation() == FrontAndTop.DOWN_SOUTH,
                "A gate placed face down lies " + down.orientation());
        helper.succeed();
    }

    // A pig dropped into flat gate A comes out of standing gate B
    private static void flatToStanding(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = SGGameTestsNeoForge.placeFlat(helper, origin.offset(SGGameTestsNeoForge.A),
                FrontAndTop.UP_SOUTH);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.B), 9);
        SGGameTestsNeoForge.crossingBetween(helper, a, b, new GateFrame(a.position().pos(), Direction.NORTH,
                GateOrientation.FACE_UP, GateShape.STANDARD), new Vec3(0, 2, 0.8), new Vec3(0, 2, -0.4));
    }

    // A pig walked into standing gate A comes out under gate B, which lies face down, head first
    private static void standingToFlat(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final Stargate b = SGGameTestsNeoForge.placeFlat(helper, origin.offset(SGGameTestsNeoForge.B),
                FrontAndTop.DOWN_SOUTH);
        SGGameTestsNeoForge.crossingBetween(helper, a, b, new GateFrame(a.position().pos(), Direction.NORTH),
                new Vec3(0, 1, 0.8), new Vec3(0, 1, -0.4));
    }

    // A pig rising into gate A, which lies face down, goes through once its head is past the horizon, its feet still
    // below it
    private static void headFirst(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = SGGameTestsNeoForge.placeFlat(helper, origin.offset(SGGameTestsNeoForge.A),
                FrontAndTop.DOWN_SOUTH);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.B), 9);
        SGGameTestsNeoForge.crossingBetween(helper, a, b, new GateFrame(a.position().pos(), Direction.NORTH,
                GateOrientation.FACE_DOWN, GateShape.STANDARD), new Vec3(0, 2, 2.0), new Vec3(0, 2, 0.6));
    }

    // A pig set in front of gate A, then just past its horizon, once the wormhole to B is open, comes out of B's
    // front: above a gate facing up, below one facing down. It flies on as fast as it was moved, so where it came out
    // is taken as it arrives.
    private static void crossingBetween(final GameTestHelper helper, final Stargate a, final Stargate b,
            final GateFrame near, final Vec3 front, final Vec3 behind) {
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.OPENING_DAMAGE, false, SGGameTestsNeoForge.server(helper));
        final AtomicReference<DialResult> result = new AtomicReference<>();
        final AtomicInteger arrived = new AtomicInteger();
        final AtomicReference<Vec3> arrival = new AtomicReference<>();
        final EventBus.Registration listener = SGCraftApi.get().eventBus().register(TravelEvent.Post.class,
                event -> {
                    if (event.gate().equals(a)) {
                        arrived.incrementAndGet();
                        arrival.set(event.traveler().position());
                    }
                });
        a.dial(SGGameTestsNeoForge.addressOf(b, a), DialMode.QUICK, SGGameTestsNeoForge.server(helper))
                .thenAccept(result::set);
        final AtomicReference<Mob> pig = new AtomicReference<>();
        helper.runAfterDelay(SGIris.MOVE_TICKS + 60, () -> {
            helper.assertTrue(result.get() == DialResult.CONNECTED, "The dial ended " + result.get());
            final Mob spawned = helper.spawnWithNoFreeWill(EntityTypes.PIG, BlockPos.ZERO.above());
            spawned.setNoGravity(true);
            final Vec3 at = near.toWorld(front);
            spawned.setPos(at.x, at.y, at.z);
            pig.set(spawned);
        });
        helper.runAfterDelay(SGIris.MOVE_TICKS + 63, () -> {
            final Vec3 at = near.toWorld(behind);
            pig.get().setPos(at.x, at.y, at.z);
        });
        helper.runAfterDelay(SGIris.MOVE_TICKS + 70, () -> {
            listener.unregister();
            helper.assertTrue(arrived.get() == 1 && pig.get().isAlive() && new AABB(b.position().pos()).inflate(4)
                    .contains(arrival.get()), "The traveler didn't come out of gate B: " + arrived.get()
                    + " arrived, at " + arrival.get() + ", B at " + b.position().pos().toShortString());
            final double baseY = b.position().pos().getY() + 0.5;
            if (b.orientation() == FrontAndTop.DOWN_SOUTH) {
                helper.assertTrue(arrival.get().y + pig.get().getBbHeight() < baseY,
                        "The traveler came out of B's back: at " + arrival.get());
            } else {
                helper.assertTrue(new GateFrame(b.position().pos(), Direction.NORTH).toLocal(arrival.get()).z > 0,
                        "The traveler came out of B's back: at " + arrival.get());
            }
            helper.succeed();
        });
    }

    // Open, a flat gate's horizon fills its opening at the base's height, facing up or down as the gate does, and
    // lights it and the ring under its chevrons; closed, the opening is empty again
    private static void flatHorizon(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final Stargate a = SGGameTestsNeoForge.placeFlat(helper, origin.offset(SGGameTestsNeoForge.A),
                FrontAndTop.UP_SOUTH);
        final Stargate b = SGGameTestsNeoForge.placeFlat(helper, origin.offset(SGGameTestsNeoForge.B),
                FrontAndTop.DOWN_SOUTH);
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.OPENING_DAMAGE, false, SGGameTestsNeoForge.server(helper));
        final GateFrame near = new GateFrame(a.position().pos(), Direction.NORTH, GateOrientation.FACE_UP,
                GateShape.STANDARD);
        final GateFrame far = new GateFrame(b.position().pos(), Direction.NORTH, GateOrientation.FACE_DOWN,
                GateShape.STANDARD);
        final AtomicReference<DialResult> result = new AtomicReference<>();
        final AtomicBoolean closed = new AtomicBoolean();
        a.dial(SGGameTestsNeoForge.addressOf(b, a), DialMode.QUICK, SGGameTestsNeoForge.server(helper))
                .thenAccept(result::set);
        helper.succeedWhen(() -> {
            if (!closed.get()) {
                helper.assertTrue(result.get() == DialResult.CONNECTED, "The gates haven't connected");
                for (final GateFrame frame : List.of(near, far)) {
                    final Direction faces = frame == near ? Direction.UP : Direction.DOWN;
                    for (int x = -1; x <= 1; x++) {
                        for (int y = 1; y <= 3; y++) {
                            final BlockPos pos = frame.block(x, y, 0);
                            final BlockState horizon = level.getBlockState(pos);
                            helper.assertTrue(pos.getY() == frame.block(0, 0, 0).getY()
                                    && horizon.is(SGBlocks.EVENT_HORIZON)
                                    && horizon.getValue(BlockStateProperties.FACING) == faces,
                                    "The opening at " + pos.toShortString() + " isn't a horizon facing " + faces
                                            + ": " + horizon);
                        }
                    }
                    helper.assertTrue(!level.getBlockState(frame.block(0, 2, 1)).is(SGBlocks.EVENT_HORIZON),
                            "The horizon reaches out of the flat opening");
                }
                final BlockPos top = near.block(0, 4, 0);
                helper.assertTrue(level.getBlockState(top).getLightEmission(level, top) == GateLight.CHEVRON,
                        "A's top chevron doesn't light its flat ring");
                helper.assertTrue(level.getBrightness(LightLayer.BLOCK, near.block(0, 2, 2)) > 0,
                        "Nothing above A is lit");
                a.disconnect(SGGameTestsNeoForge.server(helper));
                closed.set(true);
            }
            helper.assertTrue(a.state() == GateState.IDLE, "A hasn't closed");
            helper.assertTrue(level.getBlockState(near.block(0, 2, 0)).isAir(), "A's opening wasn't emptied");
        });
    }

    // With the server set to, the kawoosh clears bedrock in front of the gate, but not the gate's own horizon
    private static void kawooshBreaksUnbreakable(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final Stargate a = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.B), 9);
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        final GateFrame frame = new GateFrame(a.position().pos(), Direction.NORTH);
        final BlockPos front = frame.block(0, 2, 1);
        level.setBlock(front, Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_ALL);
        SGServer.require().stargates().setKawooshBreaksUnbreakable(true);
        final AtomicReference<DialResult> result = new AtomicReference<>();
        a.dial(SGGameTestsNeoForge.addressOf(b, a), DialMode.QUICK, SGGameTestsNeoForge.server(helper))
                .thenAccept(result::set);
        helper.succeedWhen(() -> {
            helper.assertTrue(result.get() == DialResult.CONNECTED, "The gates haven't connected");
            helper.assertTrue(level.getBlockState(front).isAir(), "The kawoosh left the bedrock");
            helper.assertTrue(level.getBlockState(frame.block(0, 2, 0)).is(SGBlocks.EVENT_HORIZON),
                    "The kawoosh cleared A's horizon");
            SGServer.require().stargates().setKawooshBreaksUnbreakable(false);
        });
    }

    // A wormhole a storm sends astray goes to gate C: B shuts down, C opens at once, and the connection doesn't open
    // a second time
    private static void stormReroutes(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.B), 9);
        final Stargate c = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.C), 9);
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        final AtomicInteger opened = new AtomicInteger();
        final EventBus.Registration listener = SGCraftApi.get().eventBus().register(ConnectionEvent.Open.class,
                event -> {
                    if (event.connection().dialer().equals(a)) {
                        opened.incrementAndGet();
                    }
                });
        a.dial(SGGameTestsNeoForge.addressOf(b, a), DialMode.QUICK, SGGameTestsNeoForge.server(helper));
        final AtomicBoolean rerouted = new AtomicBoolean();
        helper.succeedWhen(() -> {
            helper.assertTrue(a.connection().map(Connection::ticksOpen).orElse(0) > 0, "The wormhole isn't open");
            if (!rerouted.getAndSet(true)) {
                helper.assertTrue(((SGStargate) a).liveConnection().orElseThrow()
                        .reroute((SGStargate) c, SGGameTestsNeoForge.server(helper)), "The wormhole didn't go to C");
                helper.assertTrue(b.state() == GateState.CLOSING, "B is " + b.state() + ", not closing");
                helper.assertTrue(b.connection().isEmpty(), "B is still part of the connection");
            }
            helper.assertTrue(a.connection().orElseThrow().target().equals(c), "The wormhole doesn't reach C");
            helper.assertTrue(c.state() == GateState.CONNECTED, "C is " + c.state());
            helper.assertTrue(c.connection().isPresent(), "C isn't part of the connection");
            helper.assertTrue(b.state() == GateState.IDLE, "B hasn't finished closing");
            helper.assertTrue(opened.get() == 1, "The connection opened " + opened.get() + " times");
            listener.unregister();
            a.disconnect(SGGameTestsNeoForge.server(helper));
        });
    }

    // Canceling the disconnect keeps the wormhole on the gate it reached
    private static void stormRerouteCanceled(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.B), 9);
        final Stargate c = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.C), 9);
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        final EventBus.Registration listener = SGCraftApi.get().eventBus().register(
                ConnectionEvent.Disconnect.class, event -> {
                    if (event.connection().dialer().equals(a)) {
                        event.setCanceled(true);
                    }
                });
        a.dial(SGGameTestsNeoForge.addressOf(b, a), DialMode.QUICK, SGGameTestsNeoForge.server(helper));
        helper.succeedWhen(() -> {
            helper.assertTrue(a.connection().map(Connection::ticksOpen).orElse(0) > 0, "The wormhole isn't open");
            final boolean moved = ((SGStargate) a).liveConnection().orElseThrow()
                    .reroute((SGStargate) c, SGGameTestsNeoForge.server(helper));
            listener.unregister();
            helper.assertTrue(!moved, "The wormhole went to C anyway");
            helper.assertTrue(a.connection().orElseThrow().target().equals(b), "The wormhole doesn't reach B");
            helper.assertTrue(c.state() == GateState.IDLE, "C is " + c.state());
            a.disconnect(SGGameTestsNeoForge.server(helper));
        });
    }

    // A gate's own distance multiplier of 0 makes distance cost nothing, and its own naquadah energy is what the
    // DHD linked to it gives
    private static void gateEnergySettings(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.A), 9);
        final Stargate b = SGGameTestsNeoForge.place(helper, origin.offset(SGGameTestsNeoForge.B), 9);
        a.set(GateProperties.ENERGY_CAPACITY, 100_000L, SGGameTestsNeoForge.server(helper));
        a.energy().insert(100_000, false);
        a.set(GateProperties.DISTANCE_FACTOR_MULTIPLIER, 0.0, SGGameTestsNeoForge.server(helper));
        a.set(GateProperties.ENERGY_PER_NAQUADAH, 1000.0, SGGameTestsNeoForge.server(helper));
        final AtomicLong openCost = new AtomicLong(-1);
        final EventBus.Registration listener = SGCraftApi.get().eventBus().register(DialEvent.Cost.class, event -> {
            if (event.gate().equals(a)) {
                openCost.set(event.openCost());
            }
        });
        final BlockPos dhd = a.position().pos().north(4);
        helper.getLevel().setBlock(dhd, SGBlocks.DHD.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
                Direction.NORTH), Block.UPDATE_ALL);
        final DhdBlockEntity entity = (DhdBlockEntity) helper.getLevel().getBlockEntity(dhd);
        entity.fuel().setItem(0, new ItemStack(SGItems.NAQUADAH, 2));
        final double serverOpen = SGServer.require().config().dialCosts().open().energy();
        // The DHD links within a second; what it offers is looked at before the dial draws on it
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(entity.gate().isPresent(), "The DHD didn't link to its gate");
            helper.assertTrue(entity.available() == 2000, "Two naquadah give " + entity.available() + ", not 2000");
            a.dial(SGGameTestsNeoForge.addressOf(b, a), DialMode.QUICK, SGGameTestsNeoForge.server(helper));
            listener.unregister();
            helper.assertTrue(openCost.get() == Math.round(serverOpen), "Opening cost " + openCost.get()
                    + " with no distance factor, not " + serverOpen);
            a.disconnect(SGGameTestsNeoForge.server(helper));
            helper.succeed();
        });
    }

    // The feature, as SGCraft's data defines it, puts ore under lava lying on the floor of the test's chunk, in the
    // flat world's only layer below sea level, and nothing in a world it isn't set to generate in
    private static void oreUnderLava(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final ChunkPos chunk = ChunkPos.containing(origin);
        final NaquadahOreSettings before = NaquadahOre.settings();
        final Runnable restore = SGGameTestsNeoForge.lavaFloor(level, chunk);
        try {
            final Feature feature = level.registryAccess().lookupOrThrow(Registries.FEATURE)
                    .getValue(NaquadahOre.FEATURE);
            helper.assertTrue(feature instanceof NaquadahOreFeature, "The ore's feature is " + feature);
            final BlockPos corner = chunk.getBlockAt(0, level.getMinY(), 0);
            final NaquadahOreSettings lavaOnly =
                    NaquadahOreSettings.Count.GEN_ISOLATED_ODDS.with(NaquadahOreSettings.DEFAULTS, 0);
            NaquadahOre.use(lavaOnly.withDimension(level.dimension(), false));
            helper.assertFalse(feature.place(level, level.getChunkSource().getGenerator(), RandomSource.create(1),
                    corner), "The feature generated in a world it isn't set to");
            helper.assertTrue(SGGameTestsNeoForge.oreIn(level, chunk) == 0, "Ore generated in a world it isn't set to");
            NaquadahOre.use(lavaOnly.withDimension(level.dimension(), true));
            feature.place(level, level.getChunkSource().getGenerator(), RandomSource.create(1), corner);
            final int floor = SGGameTestsNeoForge.oreIn(level, chunk, level.getMinY() + 1);
            helper.assertTrue(floor > 0, "No ore generated under the lava");
            helper.assertTrue(floor == SGGameTestsNeoForge.oreIn(level, chunk), "Ore generated away from the lava");
        } finally {
            NaquadahOre.use(before);
            restore.run();
        }
        helper.succeed();
    }

    // Away from lava, nodes of up to two blocks each way start 16 to 31 blocks above the bottom given
    private static void oreIsolatedNodes(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final ChunkPos chunk = ChunkPos.containing(origin);
        final int bottom = origin.getY() - 20;
        final List<BlockPos> box = BlockPos.betweenClosedStream(chunk.getBlockAt(0, bottom + 16, 0),
                chunk.getBlockAt(15, bottom + 32, 15)).map(BlockPos::immutable).toList();
        box.forEach(pos -> level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS));
        try {
            final NaquadahOreFeature feature = (NaquadahOreFeature) level.registryAccess()
                    .lookupOrThrow(Registries.FEATURE).getValue(NaquadahOre.FEATURE);
            final NaquadahOreSettings isolatedOnly = NaquadahOreSettings.Count.GEN_UNDER_LAVA_ODDS
                    .with(NaquadahOreSettings.DEFAULTS, 0);
            final int placed = feature.generate(level, chunk, RandomSource.create(2), isolatedOnly, bottom,
                    bottom + 100);
            final long ore = box.stream().filter(pos -> level.getBlockState(pos).is(SGBlocks.NAQUADAH_ORE)).count();
            final int most = isolatedOnly.maxIsolatedNodes() * 8;
            helper.assertTrue(placed > 0 && placed <= most, placed + " blocks were turned to ore, not 1 to " + most);
            helper.assertTrue(ore == placed, ore + " blocks of the stone are ore, not the " + placed + " placed");
            helper.assertTrue(feature.generate(level, chunk, RandomSource.create(2),
                    NaquadahOreSettings.Count.GEN_ISOLATED_ODDS.with(isolatedOnly, 0), bottom, bottom + 100) == 0,
                    "Nodes generated with no odds of them");
        } finally {
            box.forEach(pos -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS));
        }
        helper.succeed();
    }

    // A chunk that loads just generated is marked as having had its ore; an old one waits for a tick, gets ore and is
    // marked, and loading it again gives it no more
    private static void oreAddedToOldChunks(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final LevelChunk chunk = level.getChunkAt(origin);
        final ChunkPos pos = chunk.getPos();
        final OreChunks marks = OreChunks.of(level);
        final boolean wasMarked = marks.marked(pos);
        final NaquadahOreSettings before = NaquadahOre.settings();
        final Runnable restore = SGGameTestsNeoForge.lavaFloor(level, pos);
        try {
            NaquadahOre.use(NaquadahOreSettings.Count.GEN_ISOLATED_ODDS.with(NaquadahOreSettings.DEFAULTS, 0)
                    .withDimension(level.dimension(), true).withAddOresToExistingWorlds(true));
            marks.unmark(pos);
            NaquadahOre.onChunkLoad(level, chunk, true);
            helper.assertTrue(marks.marked(pos), "A chunk just generated wasn't marked");
            helper.assertFalse(NaquadahOre.pending(level, pos), "A chunk just generated waits for ore");
            helper.assertTrue(SGGameTestsNeoForge.oreIn(level, pos) == 0, "A chunk just generated got ore again");
            marks.unmark(pos);
            NaquadahOre.onChunkLoad(level, chunk, false);
            helper.assertTrue(NaquadahOre.pending(level, pos), "An old chunk isn't waiting for ore");
            helper.assertTrue(SGGameTestsNeoForge.oreIn(level, pos) == 0, "An old chunk got ore before its tick");
            NaquadahOre.tick(level.getServer());
            final int ore = SGGameTestsNeoForge.oreIn(level, pos);
            helper.assertTrue(marks.marked(pos), "An old chunk wasn't marked after getting ore");
            helper.assertTrue(ore > 0, "An old chunk got no ore");
            NaquadahOre.onChunkLoad(level, chunk, false);
            helper.assertFalse(NaquadahOre.pending(level, pos), "A marked chunk waits for ore again");
            NaquadahOre.tick(level.getServer());
            helper.assertTrue(SGGameTestsNeoForge.oreIn(level, pos) == ore, "A marked chunk got ore again");
            final String chunkCommand = "sgcraft ore chunk " + origin.getX() + " " + origin.getZ();
            SGGameTestsNeoForge.command(level, chunkCommand + " unmark");
            helper.assertFalse(marks.marked(pos), "The command didn't unmark the chunk");
            SGGameTestsNeoForge.command(level, chunkCommand + " generate");
            helper.assertTrue(marks.marked(pos), "The command didn't mark the chunk it gave ore");
        } finally {
            NaquadahOre.use(before);
            restore.run();
            if (!wasMarked) {
                marks.unmark(pos);
            }
        }
        helper.succeed();
    }

    private static void command(final ServerLevel level, final String command) {
        level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack()
                .withLevel(level).withSuppressedOutput(), command);
    }

    // Lava lying on stone across a chunk, in the flat world's layer above its bottom, which is its sea level; the
    // runnable puts the blocks back
    private static Runnable lavaFloor(final ServerLevel level, final ChunkPos chunk) {
        final int floor = level.getMinY() + 1;
        final Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = floor; y <= floor + 1; y++) {
                    final BlockPos pos = chunk.getBlockAt(x, y, z);
                    saved.put(pos, level.getBlockState(pos));
                    level.setBlock(pos, y == floor ? Blocks.STONE.defaultBlockState()
                            : Blocks.LAVA.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ON_PLACE);
                }
            }
        }
        return () -> saved.forEach((pos, state) -> level.setBlock(pos, state, Block.UPDATE_CLIENTS));
    }

    // How many blocks of a chunk are naquadah ore, low in the world or at one height
    private static int oreIn(final ServerLevel level, final ChunkPos chunk) {
        int ore = 0;
        for (int y = level.getMinY(); y < level.getMinY() + 48; y++) {
            ore += SGGameTestsNeoForge.oreIn(level, chunk, y);
        }
        return ore;
    }

    private static int oreIn(final ServerLevel level, final ChunkPos chunk, final int y) {
        int ore = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (level.getBlockState(chunk.getBlockAt(x, y, z)).is(SGBlocks.NAQUADAH_ORE)) {
                    ore++;
                }
            }
        }
        return ore;
    }
}
