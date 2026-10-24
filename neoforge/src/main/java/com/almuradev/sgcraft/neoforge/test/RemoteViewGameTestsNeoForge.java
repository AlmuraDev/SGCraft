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

import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.SGCraftApi;
import com.almuradev.sgcraft.api.SGCraftRegistries;
import com.almuradev.sgcraft.api.gate.Connection;
import com.almuradev.sgcraft.api.gate.DialMode;
import com.almuradev.sgcraft.api.gate.DialResult;
import com.almuradev.sgcraft.api.gate.GatePlacement;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.api.gate.GateTypes;
import com.almuradev.sgcraft.api.gate.PlacementMode;
import com.almuradev.sgcraft.api.gate.PlacementResult;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.block.entity.StargateBaseBlockEntity;
import com.almuradev.sgcraft.mod.gate.DialPhase;
import com.almuradev.sgcraft.mod.gate.GateFrame;
import com.almuradev.sgcraft.mod.view.RemoteViewConfig;
import com.almuradev.sgcraft.mod.view.RemoteViewPackets;
import com.almuradev.sgcraft.mod.view.RemoteViewSections;
import com.almuradev.sgcraft.mod.view.RemoteViewSource;
import com.almuradev.sgcraft.mod.view.RemoteViews;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Game tests of remote views on the server: what a viewer is sent of the world around a source, as it changes, as
 * the source moves, and as the source goes through a gate, into another world, or away altogether. Each test streams
 * into a list it can look at, in place of a client. Run them with {@code ./gradlew :neoforge:runGameTestServer}.
 */
public final class RemoteViewGameTestsNeoForge {

    private static final Identifier STRUCTURE = SGCraft.id("gate_test");
    // A row of its own, apart from the other tests, each well apart from the next so their loaded chunks never meet;
    // near the world's center, where gates have addresses, and in the middle of a section, so what a test builds
    // about its origin is in the origin's section
    private static final int SPACING = 256;
    private static final int HEIGHT = 168;
    private static final int ROW_Z = 1048;
    private static final RemoteViewConfig REACH = RemoteViewConfig.DEFAULTS;
    private static final int TICKS = 600;
    // The test walking a probe through a gate, the second gate's place from the first, and how long it may take. A
    // forced chunk loads its entities only once the server's disk gets to them, which, while the other tests load
    // and save chunks all round the world, has taken from a few hundred ticks to well over a thousand; so its gates'
    // chunks are forced as the server starts, while the disk is quiet, and it has longer than the rest
    private static final String THROUGH_GATE = "remote_view_follows_through_gate";
    private static final int GATE_APART = 128;
    private static final int THROUGH_GATE_TICKS = 2400;
    private static final int THROUGH_GATE_ATTEMPTS = 3;
    private static final Map<String, BiConsumer<GameTestHelper, BlockPos>> TESTS = new LinkedHashMap<>();

    static {
        RemoteViewGameTestsNeoForge.TESTS.put("remote_view_sections_round_trip",
                RemoteViewGameTestsNeoForge::roundTrip);
        RemoteViewGameTestsNeoForge.TESTS.put("remote_view_sends_sections_around_source",
                RemoteViewGameTestsNeoForge::sendsAround);
        RemoteViewGameTestsNeoForge.TESTS.put("remote_view_resends_changes",
                RemoteViewGameTestsNeoForge::resendsChanges);
        RemoteViewGameTestsNeoForge.TESTS.put("remote_view_forgets_out_of_range",
                RemoteViewGameTestsNeoForge::forgetsOutOfRange);
        RemoteViewGameTestsNeoForge.TESTS.put("remote_view_never_loads_chunks",
                RemoteViewGameTestsNeoForge::neverLoadsChunks);
        RemoteViewGameTestsNeoForge.TESTS.put("remote_view_follows_through_gate",
                RemoteViewGameTestsNeoForge::followsThroughGate);
        RemoteViewGameTestsNeoForge.TESTS.put("remote_view_follows_to_other_world",
                RemoteViewGameTestsNeoForge::followsToOtherWorld);
        RemoteViewGameTestsNeoForge.TESTS.put("remote_view_stops_when_source_removed",
                RemoteViewGameTestsNeoForge::stopsWhenRemoved);
        RemoteViewGameTestsNeoForge.TESTS.put("remote_view_shows_open_universe_gate",
                RemoteViewGameTestsNeoForge::showsOpenUniverseGate);
        RemoteViewGameTestsNeoForge.TESTS.put("remote_view_sends_rain_heights",
                RemoteViewGameTestsNeoForge::sendsRainHeights);
    }

    private RemoteViewGameTestsNeoForge() {
    }

    /** A probe for the tests to look out of: an armor stand looking out of its eyes, with its lamp on. */
    private static final class Probe extends ArmorStand implements RemoteViewSource {

        Probe(final Level level) {
            super(EntityTypes.ARMOR_STAND, level);
        }

        @Override
        public Vec3 viewPosition() {
            return this.getEyePosition();
        }

        @Override
        public float viewYaw() {
            return this.getYRot();
        }

        @Override
        public float viewPitch() {
            return this.getXRot();
        }

        @Override
        public boolean viewLamp() {
            return true;
        }
    }

    /** Registers the tests' functions, and the tests themselves when game tests are enabled. */
    public static void register(final IEventBus modBus) {
        modBus.addListener((final RegisterEvent event) -> event.register(Registries.TEST_FUNCTION, helper -> {
            for (final Map.Entry<String, BiConsumer<GameTestHelper, BlockPos>> test
                    : RemoteViewGameTestsNeoForge.TESTS.entrySet()) {
                final BlockPos origin = RemoteViewGameTestsNeoForge.origin(test.getKey());
                final Consumer<GameTestHelper> function = h -> test.getValue().accept(h, origin);
                helper.register(SGCraft.id(test.getKey()), function);
            }
        }));
        modBus.addListener((final RegisterGameTestsEvent event) -> {
            final Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                    SGCraft.id("remote_view"), new TestEnvironmentDefinition.AllOf(List.of()));
            // The probe through a gate waits on its forced chunks loading their entities, which with every other
            // test running at once can now and then take longer than it allows, so it gets more than one try
            RemoteViewGameTestsNeoForge.TESTS.keySet().forEach(name -> event.registerTest(SGCraft.id(name),
                    new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, SGCraft.id(name)),
                            name.equals(RemoteViewGameTestsNeoForge.THROUGH_GATE)
                                    ? new TestData<>(environment, Level.OVERWORLD,
                                            RemoteViewGameTestsNeoForge.STRUCTURE,
                                            RemoteViewGameTestsNeoForge.THROUGH_GATE_TICKS, 0, true, Rotation.NONE,
                                            false, RemoteViewGameTestsNeoForge.THROUGH_GATE_ATTEMPTS, 1, false, 0)
                                    : new TestData<>(environment, RemoteViewGameTestsNeoForge.STRUCTURE,
                                            RemoteViewGameTestsNeoForge.TICKS, 0, true))));
            NeoForge.EVENT_BUS.addListener((final ServerStartedEvent started) -> {
                final ServerLevel level = started.getServer().overworld();
                final BlockPos origin = RemoteViewGameTestsNeoForge.origin(RemoteViewGameTestsNeoForge.THROUGH_GATE);
                RemoteViewGameTestsNeoForge.forceLoad(level, origin);
                RemoteViewGameTestsNeoForge.forceLoad(level, origin.east(RemoteViewGameTestsNeoForge.GATE_APART));
            });
        });
    }

    // Where a test builds, in its row, by its place among the tests
    private static BlockPos origin(final String test) {
        final int index = List.copyOf(RemoteViewGameTestsNeoForge.TESTS.keySet()).indexOf(test);
        return new BlockPos(24 + index * RemoteViewGameTestsNeoForge.SPACING, RemoteViewGameTestsNeoForge.HEIGHT,
                RemoteViewGameTestsNeoForge.ROW_Z);
    }

    // Keeps the chunk at a place loaded, and with it the two rings of chunks about it, for the length of a test
    private static void forceLoad(final ServerLevel level, final BlockPos at) {
        level.setChunkForced(SectionPos.blockToSectionCoord(at.getX()), SectionPos.blockToSectionCoord(at.getZ()),
                true);
    }

    private static void release(final ServerLevel level, final BlockPos at) {
        level.setChunkForced(SectionPos.blockToSectionCoord(at.getX()), SectionPos.blockToSectionCoord(at.getZ()),
                false);
    }

    // A floor to stand on, a gold block and a glowstone beside it to look for in what is sent, and a probe facing
    // north over them
    private static Probe probeAt(final GameTestHelper helper, final BlockPos at) {
        final ServerLevel level = helper.getLevel();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlockAndUpdate(at.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            }
        }
        level.setBlockAndUpdate(at.north(2), Blocks.GOLD_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(at.north(2).east(), Blocks.GLOWSTONE.defaultBlockState());
        final Probe probe = new Probe(level);
        probe.snapTo(Vec3.atBottomCenterOf(at), 180.0F, 0.0F);
        probe.setNoGravity(true);
        level.addFreshEntity(probe);
        return probe;
    }

    private static <T extends CustomPacketPayload> List<T> sent(final List<CustomPacketPayload> packets,
            final Class<T> type) {
        final List<T> found = new ArrayList<>();
        for (final CustomPacketPayload packet : packets) {
            if (type.isInstance(packet)) {
                found.add(type.cast(packet));
            }
        }
        return found;
    }

    // The last data sent for a section, or null if it hasn't been sent
    private static byte[] lastSent(final List<CustomPacketPayload> packets, final long section) {
        byte[] data = null;
        for (final RemoteViewPackets.Sections batch : RemoteViewGameTestsNeoForge.sent(packets,
                RemoteViewPackets.Sections.class)) {
            for (final RemoteViewPackets.Section sent : batch.sections()) {
                if (sent.pos() == section) {
                    data = sent.data();
                }
            }
        }
        return data;
    }

    private static boolean inReach(final long section, final Vec3 eye) {
        final double minX = SectionPos.sectionToBlockCoord(SectionPos.x(section));
        final double minY = SectionPos.sectionToBlockCoord(SectionPos.y(section));
        final double minZ = SectionPos.sectionToBlockCoord(SectionPos.z(section));
        final double dx = Math.max(0, Math.max(minX - eye.x, eye.x - (minX + 16)));
        final double dz = Math.max(0, Math.max(minZ - eye.z, eye.z - (minZ + 16)));
        return eye.y + RemoteViewGameTestsNeoForge.REACH.verticalRadius() >= minY
                && eye.y - RemoteViewGameTestsNeoForge.REACH.verticalRadius() <= minY + 16
                && dx * dx + dz * dz <= Math.pow(RemoteViewGameTestsNeoForge.REACH.horizontalRadius(), 2);
    }

    // A section encoded and read back holds the same blocks and light as the world
    private static void roundTrip(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        RemoteViewGameTestsNeoForge.forceLoad(level, origin);
        RemoteViewGameTestsNeoForge.probeAt(helper, origin).discard();
        level.setBlockAndUpdate(origin.above(), Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(origin.above(2), Blocks.OAK_STAIRS.defaultBlockState());
        // Light takes a few ticks to settle
        helper.runAfterDelay(10, () -> {
            final LevelChunk chunk = level.getChunkAt(origin);
            final int sectionY = SectionPos.blockToSectionCoord(origin.getY());
            final byte[] data = RemoteViewSections.encode(chunk, sectionY, level.getLightEngine(), true);
            final RemoteViewSections.Decoded decoded = RemoteViewSections.decode(data, level.registryAccess());
            final SectionPos section = SectionPos.of(origin);
            for (final BlockPos at : BlockPos.betweenClosed(section.origin(), section.origin().offset(15, 15, 15))) {
                final BlockState expected = level.getBlockState(at);
                final BlockState actual = decoded.blocks().getBlockState(at.getX() & 15, at.getY() & 15,
                        at.getZ() & 15);
                helper.assertTrue(expected == actual, "Read back " + actual + " at " + at + ", not " + expected);
                final int block = level.getBrightness(LightLayer.BLOCK, at);
                final int sky = level.getBrightness(LightLayer.SKY, at);
                helper.assertTrue(decoded.blockLight().get(at.getX() & 15, at.getY() & 15, at.getZ() & 15) == block,
                        "Block light at " + at + " read back wrong");
                helper.assertTrue(decoded.skyLight().get(at.getX() & 15, at.getY() & 15, at.getZ() & 15) == sky,
                        "Sky light at " + at + " read back wrong");
            }
            final BlockPos lit = origin.north(2).east().above();
            helper.assertTrue(decoded.blockLight().get(lit.getX() & 15, lit.getY() & 15, lit.getZ() & 15) > 0,
                    "The glowstone's light wasn't sent");
            helper.assertTrue(decoded.blocks().getNoiseBiome(0, 0, 0).equals(level.getNoiseBiome(
                    section.minBlockX() >> 2, section.minBlockY() >> 2, section.minBlockZ() >> 2)),
                    "The biome read back wrong");
            RemoteViewGameTestsNeoForge.release(level, origin);
            helper.succeed();
        });
    }

    // A viewer is sent every loaded section in reach of the source, a frame each tick, and nothing out of reach
    private static void sendsAround(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        RemoteViewGameTestsNeoForge.forceLoad(level, origin);
        final Probe probe = RemoteViewGameTestsNeoForge.probeAt(helper, origin);
        final ServerPlayer viewer = helper.makeMockServerPlayerInLevel();
        final List<CustomPacketPayload> packets = new ArrayList<>();
        RemoteViews.start(viewer, probe, RemoteViewGameTestsNeoForge.REACH, packets::add);
        helper.assertTrue(RemoteViews.watching(viewer, probe), "The viewer isn't watching the probe");
        helper.succeedWhen(() -> {
            final Vec3 eye = probe.viewPosition();
            final LongSet sent = RemoteViews.sentSections(viewer);
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    for (int y = -3; y <= 3; y++) {
                        final long section = SectionPos.asLong(SectionPos.blockToSectionCoord(eye.x) + x,
                                SectionPos.blockToSectionCoord(eye.y) + y, SectionPos.blockToSectionCoord(eye.z) + z);
                        final boolean loaded = level.getChunkSource().getChunkNow(SectionPos.x(section),
                                SectionPos.z(section)) != null;
                        final boolean wanted = loaded && RemoteViewGameTestsNeoForge.inReach(section, eye);
                        helper.assertTrue(wanted == sent.contains(section), "Section " + SectionPos.of(section)
                                + (wanted ? " wasn't sent" : " was sent though out of reach"));
                    }
                }
            }
            final byte[] data = RemoteViewGameTestsNeoForge.lastSent(packets, SectionPos.asLong(origin.north(2)));
            helper.assertTrue(data != null, "The probe's own section wasn't sent");
            final RemoteViewSections.Decoded decoded = RemoteViewSections.decode(data, level.registryAccess());
            final BlockPos gold = origin.north(2);
            helper.assertTrue(decoded.blocks().getBlockState(gold.getX() & 15, gold.getY() & 15, gold.getZ() & 15)
                    .is(Blocks.GOLD_BLOCK), "The gold block wasn't in what was sent");
            helper.assertTrue(packets.getFirst() instanceof RemoteViewPackets.Begin, "The view didn't begin first");
            helper.assertTrue(RemoteViewGameTestsNeoForge.sent(packets, RemoteViewPackets.Frame.class).size() > 5,
                    "Frames aren't sent each tick");
            for (final RemoteViewPackets.Sections batch : RemoteViewGameTestsNeoForge.sent(packets,
                    RemoteViewPackets.Sections.class)) {
                int bytes = 0;
                for (final RemoteViewPackets.Section section : batch.sections()) {
                    bytes += section.data().length;
                }
                helper.assertTrue(bytes <= 48 * 1024, "A packet of sections was " + bytes + " bytes");
            }
            RemoteViews.stop(viewer);
            RemoteViewGameTestsNeoForge.release(level, origin);
        });
    }

    // A section is sent again once a block in it changes
    private static void resendsChanges(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        RemoteViewGameTestsNeoForge.forceLoad(level, origin);
        final Probe probe = RemoteViewGameTestsNeoForge.probeAt(helper, origin);
        final ServerPlayer viewer = helper.makeMockServerPlayerInLevel();
        final List<CustomPacketPayload> packets = new ArrayList<>();
        RemoteViews.start(viewer, probe, RemoteViewGameTestsNeoForge.REACH, packets::add);
        final BlockPos changed = origin.north(2);
        final AtomicInteger before = new AtomicInteger(-1);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(RemoteViews.sentSections(viewer).contains(SectionPos.asLong(changed)),
                    "The section hadn't been sent before it changed");
            before.set(RemoteViews.resends(viewer));
            level.setBlockAndUpdate(changed, Blocks.DIAMOND_BLOCK.defaultBlockState());
        });
        helper.runAfterDelay(70, () -> {
            helper.assertTrue(RemoteViews.resends(viewer) > before.get(), "The changed section wasn't sent again");
            final byte[] data = RemoteViewGameTestsNeoForge.lastSent(packets, SectionPos.asLong(changed));
            final RemoteViewSections.Decoded decoded = RemoteViewSections.decode(data, level.registryAccess());
            helper.assertTrue(decoded.blocks().getBlockState(changed.getX() & 15, changed.getY() & 15,
                    changed.getZ() & 15).is(Blocks.DIAMOND_BLOCK), "The section was sent again without the change");
            RemoteViews.stop(viewer);
            RemoteViewGameTestsNeoForge.release(level, origin);
            helper.succeed();
        });
    }

    // Sections far behind a source that has moved are forgotten
    private static void forgetsOutOfRange(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos away = origin.east(64);
        RemoteViewGameTestsNeoForge.forceLoad(level, origin);
        RemoteViewGameTestsNeoForge.forceLoad(level, away);
        final Probe probe = RemoteViewGameTestsNeoForge.probeAt(helper, origin);
        final ServerPlayer viewer = helper.makeMockServerPlayerInLevel();
        final List<CustomPacketPayload> packets = new ArrayList<>();
        RemoteViews.start(viewer, probe, RemoteViewGameTestsNeoForge.REACH, packets::add);
        final long start = SectionPos.asLong(origin);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(RemoteViews.sentSections(viewer).contains(start), "The first section wasn't sent");
            probe.teleportTo(away.getX() + 0.5, away.getY(), away.getZ() + 0.5);
        });
        helper.runAfterDelay(80, () -> {
            final LongSet sent = RemoteViews.sentSections(viewer);
            helper.assertTrue(!sent.contains(start), "The section left behind wasn't forgotten");
            helper.assertTrue(sent.contains(SectionPos.asLong(away)), "The section moved to wasn't sent");
            boolean told = false;
            for (final RemoteViewPackets.Sections batch : RemoteViewGameTestsNeoForge.sent(packets,
                    RemoteViewPackets.Sections.class)) {
                told |= batch.forgotten().contains(start);
            }
            helper.assertTrue(told, "The viewer wasn't told to forget the section left behind");
            RemoteViews.stop(viewer);
            RemoteViewGameTestsNeoForge.release(level, origin);
            RemoteViewGameTestsNeoForge.release(level, away);
            helper.succeed();
        });
    }

    // A view reads only chunks already loaded: the chunks about a source whose own chunk alone is loaded stay as
    // they were, and nothing of them is sent
    private static void neverLoadsChunks(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final ChunkPos center = ChunkPos.containing(origin);
        // A ticket that loads the chunk fully and its neighbors only part way, for a little longer than the test
        level.getChunkSource().addTicketWithRadius(TicketType.PORTAL, center, 0);
        level.getChunk(center.x(), center.z());
        final Probe probe = RemoteViewGameTestsNeoForge.probeAt(helper, origin);
        final List<ChunkPos> unloaded = new ArrayList<>();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (level.getChunkSource().getChunkNow(center.x() + x, center.z() + z) == null) {
                    unloaded.add(new ChunkPos(center.x() + x, center.z() + z));
                }
            }
        }
        helper.assertTrue(!unloaded.isEmpty(), "Every chunk about the probe was already loaded");
        final ServerPlayer viewer = helper.makeMockServerPlayerInLevel();
        RemoteViews.start(viewer, probe, RemoteViewGameTestsNeoForge.REACH, packet -> {
        });
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(RemoteViews.watching(viewer, probe), "The view stopped");
            for (final ChunkPos chunk : unloaded) {
                helper.assertTrue(level.getChunkSource().getChunkNow(chunk.x(), chunk.z()) == null,
                        "The view loaded chunk " + chunk);
            }
            for (final long section : RemoteViews.sentSections(viewer)) {
                helper.assertTrue(!unloaded.contains(new ChunkPos(SectionPos.x(section), SectionPos.z(section))),
                        "A section of an unloaded chunk was sent");
            }
            helper.assertTrue(RemoteViews.sentSections(viewer).contains(SectionPos.asLong(origin)),
                    "The probe's own loaded section wasn't sent");
            RemoteViews.stop(viewer);
            level.getChunkSource().removeTicketWithRadius(TicketType.PORTAL, center, 0);
            helper.succeed();
        });
    }

    private static Stargate place(final GameTestHelper helper, final BlockPos at) {
        return RemoteViewGameTestsNeoForge.place(helper, helper.getLevel(), at, GateTypes.MILKY_WAY);
    }

    private static Stargate place(final GameTestHelper helper, final ServerLevel level, final BlockPos at,
            final ResourceKey<GateType> type) {
        final PlacementResult result = SGCraftApi.get().stargates().place(GatePlacement.builder()
                .position(GlobalPos.of(level.dimension(), at)).orientation(FrontAndTop.NORTH_UP)
                .type(level.registryAccess().lookupOrThrow(SGCraftRegistries.GATE_TYPE).getOrThrow(type))
                .chevrons(9).mode(PlacementMode.FORCE).noFixtures().build(),
                Cause.of(helper.getLevel().getServer()));
        if (result.status() != PlacementResult.Status.PLACED) {
            throw helper.assertionException(Component.literal("Placing a gate failed: " + result.status()));
        }
        return result.gate().orElseThrow();
    }

    // A probe walked through gate A comes out of gate B, and the view goes with it. Each step waits on the world, not
    // on set ticks: on a busy server the forced chunks can take well over a thousand ticks to load their entities,
    // and until gate A's have, the gate can't see the probe to send it; see THROUGH_GATE
    private static void followsThroughGate(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final BlockPos atB = origin.east(RemoteViewGameTestsNeoForge.GATE_APART);
        RemoteViewGameTestsNeoForge.forceLoad(level, origin);
        RemoteViewGameTestsNeoForge.forceLoad(level, atB);
        final Cause server = Cause.of(level.getServer());
        final Stargate a = RemoteViewGameTestsNeoForge.place(helper, origin);
        final Stargate b = RemoteViewGameTestsNeoForge.place(helper, atB);
        a.set(GateProperties.REQUIRES_POWER, false, server);
        a.set(GateProperties.OPENING_DAMAGE, false, server);
        final AtomicReference<DialResult> result = new AtomicReference<>();
        a.dial(SGCraftApi.get().addressing().addressOf(b.position(), a.position().dimension()).orElseThrow(),
                DialMode.QUICK, server).thenAccept(result::set);
        final GateFrame frame = new GateFrame(a.position().pos(), Direction.NORTH);
        final ServerPlayer viewer = helper.makeMockServerPlayerInLevel();
        final AtomicReference<Probe> probe = new AtomicReference<>();
        helper.startSequence()
                // Travelers go once the kawoosh is over, and a gate sees only entities its chunk has loaded
                .thenWaitUntil(() -> {
                    helper.assertTrue(result.get() == DialResult.CONNECTED, "The dial hasn't connected: "
                            + result.get());
                    helper.assertTrue(a.connection().map(Connection::ticksOpen).orElse(0) > 0,
                            "The wormhole isn't open");
                    helper.assertTrue(level.areEntitiesActuallyLoadedAndTicking(ChunkPos.containing(origin))
                            && level.areEntitiesActuallyLoadedAndTicking(ChunkPos.containing(atB)),
                            "The gates' chunks haven't loaded their entities");
                })
                .thenExecute(() -> {
                    final Probe spawned = new Probe(level);
                    final Vec3 front = frame.toWorld(new Vec3(0, 1, 0.8));
                    spawned.snapTo(front, 180.0F, 0.0F);
                    spawned.setNoGravity(true);
                    level.addFreshEntity(spawned);
                    probe.set(spawned);
                    RemoteViews.start(viewer, spawned, RemoteViewGameTestsNeoForge.REACH, packet -> {
                    });
                })
                // Gate A sees it in front of the horizon before it steps through
                .thenExecuteAfter(3, () -> {
                    final Vec3 behind = frame.toWorld(new Vec3(0, 1, -0.4));
                    probe.get().setPos(behind.x, behind.y, behind.z);
                })
                .thenWaitUntil(() -> {
                    final Probe traveler = probe.get();
                    helper.assertTrue(traveler.position().distanceTo(Vec3.atCenterOf(atB)) < 8,
                            "The probe didn't come out of gate B; it is at " + traveler.position());
                    helper.assertTrue(RemoteViews.watching(viewer, traveler), "The view didn't follow the probe");
                    helper.assertTrue(RemoteViews.sentSections(viewer).contains(SectionPos.asLong(
                            BlockPos.containing(traveler.viewPosition()))),
                            "The view didn't send where the probe came out");
                })
                .thenExecute(() -> {
                    RemoteViews.stop(viewer);
                    RemoteViewGameTestsNeoForge.release(level, origin);
                    RemoteViewGameTestsNeoForge.release(level, atB);
                })
                .thenSucceed();
    }

    // A probe in the End watching a Universe gate there that a gate in the Overworld has dialed is sent the gate's
    // open wormhole: a copy made of the gate from what is sent, as a client makes one, shows its horizon
    private static void showsOpenUniverseGate(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final ServerLevel end = level.getServer().getLevel(Level.END);
        helper.assertTrue(end != null, "There is no End");
        final BlockPos atB = new BlockPos(origin.getX(), 120, origin.getZ());
        RemoteViewGameTestsNeoForge.forceLoad(level, origin);
        end.getChunk(atB);
        final Cause server = Cause.of(level.getServer());
        final Stargate a = RemoteViewGameTestsNeoForge.place(helper, origin);
        final Stargate b = RemoteViewGameTestsNeoForge.place(helper, end, atB, GateTypes.UNIVERSE);
        a.set(GateProperties.REQUIRES_POWER, false, server);
        a.set(GateProperties.OPENING_DAMAGE, false, server);
        final AtomicReference<DialResult> result = new AtomicReference<>();
        a.dial(SGCraftApi.get().addressing().addressOf(b.position(), a.position().dimension()).orElseThrow(),
                DialMode.QUICK, server).thenAccept(result::set);
        final ServerPlayer viewer = helper.makeMockServerPlayerInLevel();
        final List<CustomPacketPayload> packets = new ArrayList<>();
        final AtomicReference<Probe> probe = new AtomicReference<>();
        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(result.get() == DialResult.CONNECTED, "The dial hasn't connected: "
                            + result.get());
                    helper.assertTrue(a.connection().map(Connection::ticksOpen).orElse(0) > 0,
                            "The wormhole isn't open");
                })
                .thenExecute(() -> {
                    // On a floor a few blocks in front of the gate, facing it
                    final Vec3 front = new GateFrame(atB, Direction.NORTH).toWorld(new Vec3(0, 1, 6));
                    final BlockPos floor = BlockPos.containing(front).below();
                    for (int x = -1; x <= 1; x++) {
                        for (int z = -1; z <= 1; z++) {
                            end.setBlockAndUpdate(floor.offset(x, 0, z), Blocks.END_STONE.defaultBlockState());
                        }
                    }
                    final Probe spawned = new Probe(end);
                    spawned.snapTo(front, 0.0F, 0.0F);
                    spawned.setNoGravity(true);
                    end.addFreshEntity(spawned);
                    probe.set(spawned);
                    RemoteViews.start(viewer, spawned, RemoteViewGameTestsNeoForge.REACH, packets::add);
                })
                .thenWaitUntil(() -> {
                    final CompoundTag sent = RemoteViewGameTestsNeoForge.lastBlockEntity(packets, atB);
                    helper.assertTrue(sent != null, "The Universe gate's base wasn't sent");
                    final StargateBaseBlockEntity copy = new StargateBaseBlockEntity(atB, end.getBlockState(atB));
                    copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, end.registryAccess(),
                            sent));
                    helper.assertTrue(copy.isFormed(), "The copy of the gate isn't formed");
                    helper.assertTrue(copy.dialView().phase() == DialPhase.CONNECTED, "The copy of the gate is "
                            + copy.dialView().phase() + "; the gate itself is "
                            + RemoteViewGameTestsNeoForge.baseView(end, atB));
                    StargateBaseBlockEntity.tickHorizon(copy, RandomSource.create());
                    helper.assertTrue(copy.horizon() != null, "The copy of the gate shows no horizon");
                })
                .thenExecute(() -> {
                    RemoteViews.stop(viewer);
                    a.disconnect(server);
                    probe.get().discard();
                    RemoteViewGameTestsNeoForge.release(level, origin);
                })
                .thenSucceed();
    }

    // The tag last sent for the block entity at a position, or null if none was
    private static @Nullable CompoundTag lastBlockEntity(final List<CustomPacketPayload> packets,
            final BlockPos pos) {
        CompoundTag tag = null;
        for (final RemoteViewPackets.BlockEntities batch : RemoteViewGameTestsNeoForge.sent(packets,
                RemoteViewPackets.BlockEntities.class)) {
            for (final RemoteViewPackets.BlockEntityState state : batch.changed()) {
                if (state.pos() == pos.asLong()) {
                    tag = state.tag();
                }
            }
        }
        return tag;
    }

    // What the gate's own base is doing, for a failure to report
    private static String baseView(final ServerLevel level, final BlockPos pos) {
        return level.getBlockEntity(pos) instanceof StargateBaseBlockEntity base ? base.dialView().phase().name()
                : "missing its base";
    }

    // A probe that goes to another world turns up there as a new entity with its UUID, and the view follows it
    private static void followsToOtherWorld(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final ServerLevel nether = level.getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "There is no nether");
        final BlockPos there = new BlockPos(origin.getX() / 8, 64, origin.getZ() / 8);
        RemoteViewGameTestsNeoForge.forceLoad(level, origin);
        RemoteViewGameTestsNeoForge.forceLoad(nether, there);
        // Room for the probe where it arrives
        nether.setBlockAndUpdate(there, Blocks.AIR.defaultBlockState());
        nether.setBlockAndUpdate(there.above(), Blocks.AIR.defaultBlockState());
        final Probe probe = RemoteViewGameTestsNeoForge.probeAt(helper, origin);
        final ServerPlayer viewer = helper.makeMockServerPlayerInLevel();
        final List<CustomPacketPayload> packets = new ArrayList<>();
        RemoteViews.start(viewer, probe, RemoteViewGameTestsNeoForge.REACH, packets::add);
        final AtomicReference<Entity> arrived = new AtomicReference<>();
        helper.runAfterDelay(20, () -> arrived.set(probe.teleport(new TeleportTransition(nether,
                Vec3.atBottomCenterOf(there), Vec3.ZERO, 0.0F, 0.0F, TeleportTransition.DO_NOTHING))));
        helper.runAfterDelay(60, () -> {
            final Entity entity = arrived.get();
            helper.assertTrue(entity != null && entity != probe && entity.level() == nether,
                    "The probe didn't arrive in the nether as a new entity");
            helper.assertTrue(RemoteViews.watching(viewer, entity), "The view didn't follow the probe");
            helper.assertTrue(RemoteViews.world(viewer).orElse(null) == nether, "The view isn't in the nether");
            final List<RemoteViewPackets.Begin> begins = RemoteViewGameTestsNeoForge.sent(packets,
                    RemoteViewPackets.Begin.class);
            helper.assertTrue(begins.size() == 2 && begins.getLast().world().equals(Level.NETHER.identifier()),
                    "The viewer wasn't told the view moved to the nether");
            helper.assertTrue(RemoteViews.sentSections(viewer).contains(SectionPos.asLong(
                    BlockPos.containing(entity.getEyePosition()))), "Nothing of the nether was sent");
            RemoteViews.stop(viewer);
            entity.discard();
            RemoteViewGameTestsNeoForge.release(level, origin);
            RemoteViewGameTestsNeoForge.release(nether, there);
            helper.succeed();
        });
    }

    // The view sends where rain stops in the chunks it sends, as the world's heightmap has it, and sends it again when
    // a block put over a column changes it
    private static void sendsRainHeights(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        RemoteViewGameTestsNeoForge.forceLoad(level, origin);
        final Probe probe = RemoteViewGameTestsNeoForge.probeAt(helper, origin);
        final ServerPlayer viewer = helper.makeMockServerPlayerInLevel();
        final List<CustomPacketPayload> packets = new ArrayList<>();
        RemoteViews.start(viewer, probe, RemoteViewGameTestsNeoForge.REACH, packets::add);
        final BlockPos column = origin.east();
        final BlockPos roof = column.above(4);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    final int expected = level.getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(),
                            column.getZ());
                    final int sent = RemoteViewGameTestsNeoForge.rainHeight(packets, column);
                    helper.assertTrue(sent == expected, "The column's rain height was sent as " + sent + ", not "
                            + expected);
                })
                .thenExecute(() -> level.setBlockAndUpdate(roof, Blocks.STONE.defaultBlockState()))
                .thenWaitUntil(() -> {
                    final int sent = RemoteViewGameTestsNeoForge.rainHeight(packets, column);
                    helper.assertTrue(sent == roof.getY() + 1, "The roofed column's rain height was sent as " + sent
                            + ", not " + (roof.getY() + 1));
                })
                .thenExecute(() -> {
                    RemoteViews.stop(viewer);
                    level.setBlockAndUpdate(roof, Blocks.AIR.defaultBlockState());
                    probe.discard();
                    RemoteViewGameTestsNeoForge.release(level, origin);
                })
                .thenSucceed();
    }

    // The rain height last sent for a block column, or the lowest int if none was
    private static int rainHeight(final List<CustomPacketPayload> packets, final BlockPos column) {
        final long chunk = ChunkPos.containing(column).pack();
        int height = Integer.MIN_VALUE;
        for (final RemoteViewPackets.Heights heights : RemoteViewGameTestsNeoForge.sent(packets,
                RemoteViewPackets.Heights.class)) {
            for (final RemoteViewPackets.RainHeights sent : heights.chunks()) {
                if (sent.chunk() == chunk) {
                    height = sent.heights()[(column.getX() & 15) + (column.getZ() & 15) * 16];
                }
            }
        }
        return height;
    }

    // A view ends, and its viewer is told, once its source is removed
    private static void stopsWhenRemoved(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        RemoteViewGameTestsNeoForge.forceLoad(level, origin);
        final Probe probe = RemoteViewGameTestsNeoForge.probeAt(helper, origin);
        final ServerPlayer viewer = helper.makeMockServerPlayerInLevel();
        final List<CustomPacketPayload> packets = new ArrayList<>();
        RemoteViews.start(viewer, probe, RemoteViewGameTestsNeoForge.REACH, packets::add);
        helper.runAfterDelay(10, probe::discard);
        helper.runAfterDelay(14, () -> {
            helper.assertTrue(!RemoteViews.watching(viewer, probe), "The view didn't stop");
            helper.assertTrue(packets.getLast() instanceof RemoteViewPackets.End, "The viewer wasn't told it ended");
            RemoteViewGameTestsNeoForge.release(level, origin);
            helper.succeed();
        });
    }
}
