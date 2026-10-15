/*
 * This file is part of SGCraft, licensed under the MIT License (MIT).
 * It is based on the original SGCraft by gcewing.
 *
 * Copyright (c) AlmuraDev <https://github.com/AlmuraDev>
 * Copyright (c) 2016 gcewing <https://github.com/gcewing>
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

package com.almuradev.sgcraft.worldgen;

import com.almuradev.sgcraft.SGCraft;
import com.almuradev.sgcraft.mixin.StructurePiecesBuilderAccessor;
import com.almuradev.sgcraft.mixin.StructureTemplatePoolAccessor;
import com.almuradev.sgcraft.mod.SGConfig;
import com.almuradev.sgcraft.registry.RegistrySink;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Gates in generated structures, as the original SGCraft added them: built into desert pyramids, jungle temples,
 * ocean monuments and nether fortresses, beside swamp huts and igloos, and among a village's houses, where a plaza
 * is one more piece the village's pools can choose.
 */
public final class StructureGates {

    /** The gates added beside vanilla's structures unless the server's settings say otherwise. */
    public static final Map<Identifier, StructureGate> DEFAULT_STRUCTURES = StructureGates.defaultStructures();
    /** The plazas villages build unless the server's settings say otherwise. */
    public static final VillageGates DEFAULT_VILLAGES = StructureGates.defaultVillages();

    // Tries at finding a spot clear of the structure's pieces before giving up on the gate
    private static final int TRIES = 8;
    // How many gates in a hundred stand a few blocks lower, half buried, as in the original
    private static final int BURIED_CHANCE = 10;
    private static final int MAX_BURIED = 3;

    private static volatile Map<Identifier, StructureGate> structures = Map.of();
    private static volatile VillageGates villages = StructureGates.DEFAULT_VILLAGES;
    private static volatile boolean always;

    private StructureGates() {
    }

    /** Registers the gate's structure piece. */
    public static void register(final RegistrySink<StructurePieceType> sink) {
        sink.register(ResourceKey.create(Registries.STRUCTURE_PIECE, SGCraft.id("stargate")), StargatePiece.TYPE);
    }

    /**
     * Takes up the server's settings and adds the plazas to the village pools. Runs as the server starts, before it
     * generates any of its worlds.
     */
    public static void prepare(final MinecraftServer server) {
        final SGConfig config = SGConfig.load();
        StructureGates.structures = Map.copyOf(config.structureGates());
        StructureGates.always = false;
        final VillageGates villages = config.villageGates();
        StructureGates.villages = villages;
        if (villages.weight() <= 0) {
            return;
        }
        final Registry<StructureTemplatePool> pools = server.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);
        villages.pools().forEach((poolId, template) -> pools.getOptional(poolId).ifPresentOrElse(
                pool -> StructureGates.join(pool, template, villages.weight()),
                () -> SGCraft.LOG.warn("There is no pool {} for a gate plaza to join", poolId)));
    }

    private static void join(final StructureTemplatePool pool, final Identifier template, final int weight) {
        final StructurePoolElement plaza = StructurePoolElement.single(template.toString())
                .apply(StructureTemplatePool.Projection.RIGID);
        final StructureTemplatePoolAccessor access = (StructureTemplatePoolAccessor) pool;
        final List<Pair<StructurePoolElement, Integer>> raw = new ArrayList<>(access.sgcraft$rawTemplates());
        raw.add(Pair.of(plaza, weight));
        access.sgcraft$setRawTemplates(raw);
        for (int i = 0; i < weight; i++) {
            access.sgcraft$templates().add(plaza);
        }
    }

    /** @return the gates added beside each kind of structure, by the structure's identifier */
    public static Map<Identifier, StructureGate> structures() {
        return StructureGates.structures;
    }

    /** @return the plazas villages build */
    public static VillageGates villages() {
        return StructureGates.villages;
    }

    /** @return whether every structure set to have a gate gets one, whatever its chance */
    public static boolean always() {
        return StructureGates.always;
    }

    /**
     * Sets whether every structure set to have a gate gets one, whatever its chance, until the server stops. For
     * trying the gates out.
     */
    public static void always(final boolean always) {
        StructureGates.always = always;
    }

    /** Adds a gate beside a structure just laid out, if it is set to have one and there is room. */
    public static void addGate(final Holder<Structure> structure, final Structure.GenerationContext context,
            final StructurePiecesBuilder builder) {
        final List<StructurePiece> pieces = ((StructurePiecesBuilderAccessor) builder).sgcraft$pieces();
        if (pieces.isEmpty()) {
            return;
        }
        final Optional<StructureGate> setting = structure.unwrapKey()
                .map(key -> StructureGates.structures.get(key.identifier()));
        if (setting.isEmpty()) {
            return;
        }
        final StructureGate gate = setting.get();
        final WorldgenRandom random = context.random();
        if (!StructureGates.always && random.nextInt(100) >= gate.chance()) {
            return;
        }
        final Vec3i size = context.structureTemplateManager().getOrCreate(gate.template()).getSize();
        if (size.getX() == 0) {
            SGCraft.LOG.warn("There is no structure template {} for a gate", gate.template());
            return;
        }
        final StructurePiece first = pieces.getFirst();
        final BoundingBox anchor = first.getBoundingBox();
        final int chevrons = random.nextInt(100) < gate.chevronUpgradeChance() ? 9 : 7;
        if (gate.placement() == StructureGate.Placement.INSIDE) {
            final OptionalY y = StructureGates.y(context, gate, anchor, anchor.getCenter().getX() - size.getX() / 2,
                    anchor.getCenter().getZ() - size.getZ() / 2, size);
            if (!y.missing) {
                builder.addPiece(new StargatePiece(context.structureTemplateManager(), gate.template(),
                        new BlockPos(anchor.getCenter().getX() - size.getX() / 2, y.value,
                                anchor.getCenter().getZ() - size.getZ() / 2),
                        StructureGates.rotation(first, random), gate.foundation(), gate.foundationBlock(), chevrons,
                        gate.tokra()).follow(first, gate.ground(), gate.yOffset()));
            }
            return;
        }
        for (int i = 0; i < StructureGates.TRIES; i++) {
            final int x = anchor.getCenter().getX() + StructureGates.offset(random, gate) - size.getX() / 2;
            final int z = anchor.getCenter().getZ() + StructureGates.offset(random, gate) - size.getZ() / 2;
            final Rotation rotation = Rotation.getRandom(random);
            final OptionalY y = StructureGates.y(context, gate, anchor, x, z, size);
            if (y.missing) {
                continue;
            }
            final int bottom = y.value - (random.nextInt(100) < StructureGates.BURIED_CHANCE
                    ? 1 + random.nextInt(StructureGates.MAX_BURIED) : 0);
            final LevelHeightAccessor height = context.heightAccessor();
            if (bottom <= height.getMinY() || bottom + size.getY() > height.getMaxY()) {
                continue;
            }
            final StargatePiece piece = new StargatePiece(context.structureTemplateManager(), gate.template(),
                    new BlockPos(x, bottom, z), rotation, gate.foundation(), gate.foundationBlock(), chevrons,
                    gate.tokra());
            if (!piece.collides(pieces)) {
                builder.addPiece(piece);
                return;
            }
        }
    }

    // Turned as the structure's first piece is, so its front is the template's; the template's front is north
    private static Rotation rotation(final StructurePiece piece, final WorldgenRandom random) {
        final Direction facing = piece.getOrientation();
        if (facing == null) {
            return Rotation.getRandom(random);
        }
        return switch (facing) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    // Blocks along one axis from the structure's first piece, either way
    private static int offset(final WorldgenRandom random, final StructureGate gate) {
        final int distance = gate.minDistance()
                + random.nextInt(Math.max(1, gate.maxDistance() - gate.minDistance() + 1));
        return random.nextBoolean() ? distance : -distance;
    }

    // Where the template's bottom layer goes: in place of the lowest of the ground under its corners, so it sits
    // flush with the ground there
    private static OptionalY y(final Structure.GenerationContext context, final StructureGate gate,
            final BoundingBox anchor, final int x, final int z, final Vec3i size) {
        final Optional<Heightmap.Types> heightmap = gate.ground().heightmap();
        if (heightmap.isEmpty()) {
            return new OptionalY((gate.ground().top() ? anchor.maxY() : anchor.minY()) + gate.yOffset(), false);
        }
        int lowest = Integer.MAX_VALUE;
        for (final int[] corner : new int[][] {{0, 0}, {size.getX() - 1, 0}, {0, size.getZ() - 1},
                {size.getX() - 1, size.getZ() - 1}}) {
            lowest = Math.min(lowest, context.chunkGenerator().getFirstOccupiedHeight(x + corner[0], z + corner[1],
                    heightmap.get(), context.heightAccessor(), context.randomState()));
        }
        return new OptionalY(lowest + gate.yOffset(), lowest <= context.heightAccessor().getMinY());
    }

    private record OptionalY(int value, boolean missing) {
    }

    private static Map<Identifier, StructureGate> defaultStructures() {
        final Map<Identifier, StructureGate> gates = new LinkedHashMap<>();
        // A chamber in the middle of the pyramid's hall, its floor the hall's
        gates.put(Identifier.withDefaultNamespace("desert_pyramid"), StructureGates.inside("pyramid_hall",
                StructureGate.Ground.STRUCTURE, 0, true));
        // On the temple's roof
        gates.put(Identifier.withDefaultNamespace("jungle_pyramid"), StructureGates.inside("jungle_roof",
                StructureGate.Ground.STRUCTURE_TOP, 0, true));
        // A shrine in place of the monument's top, under the sea, where no Tok'ra stands
        gates.put(Identifier.withDefaultNamespace("monument"), StructureGates.inside("ocean_shrine",
                StructureGate.Ground.STRUCTURE, 16, false));
        // A chamber on the fortress's first crossing, its floor the bridges' deck and a doorway onto each bridge;
        // no Tok'ra keeps watch in the Nether, as in the original
        gates.put(Identifier.withDefaultNamespace("fortress"), StructureGates.inside("nether_chamber",
                StructureGate.Ground.STRUCTURE, 4, false));
        // A platform a little way off
        gates.put(Identifier.withDefaultNamespace("swamp_hut"), StructureGates.beside("swamp_platform"));
        gates.put(Identifier.withDefaultNamespace("igloo"), StructureGates.beside("snowy_platform"));
        return Map.copyOf(gates);
    }

    private static StructureGate inside(final String template, final StructureGate.Ground ground,
            final int yOffset, final boolean tokra) {
        return new StructureGate(SGCraft.id("stargate/" + template), 25, StructureGate.Placement.INSIDE, 0, 0,
                ground, yOffset, StructureGate.Foundation.NONE, Blocks.STONE_BRICKS.defaultBlockState(), 25, tokra);
    }

    // As the original: 10 to 29 blocks off along each axis, on a foundation down to the ground
    private static StructureGate beside(final String template) {
        return new StructureGate(SGCraft.id("stargate/" + template), 25, StructureGate.Placement.BESIDE, 10, 29,
                StructureGate.Ground.SURFACE, 0, StructureGate.Foundation.FULL,
                Blocks.STONE_BRICKS.defaultBlockState(), 25, true);
    }

    private static VillageGates defaultVillages() {
        final Map<Identifier, Identifier> pools = new LinkedHashMap<>();
        for (final String biome : List.of("plains", "desert", "savanna", "snowy", "taiga")) {
            final Identifier plaza = SGCraft.id("stargate/village_" + biome);
            pools.put(Identifier.withDefaultNamespace("village/" + biome + "/houses"), plaza);
            pools.put(Identifier.withDefaultNamespace("village/" + biome + "/zombie/houses"), plaza);
        }
        return new VillageGates(2, Map.copyOf(pools), true);
    }
}
