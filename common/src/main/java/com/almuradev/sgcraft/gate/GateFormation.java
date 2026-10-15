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

package com.almuradev.sgcraft.gate;

import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.SGCraftRegistries;
import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.block.EventHorizonBlock;
import com.almuradev.sgcraft.block.RingBlock;
import com.almuradev.sgcraft.block.SGBlocks;
import com.almuradev.sgcraft.block.StargateBaseBlock;
import com.almuradev.sgcraft.block.entity.StargateBaseBlockEntity;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.event.SGEventBus;
import com.almuradev.sgcraft.mod.event.SGEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Forms a gate from a base block and a complete ring around it, and breaks it apart again.
 *
 * <p>A standard gate stands upright, five blocks wide and tall, with its base at the bottom center. A supergate's ring
 * follows a circle, as big as the ring its base finds; see {@link GateShape}. Positions are in the gate's own frame:
 * x runs to the gate's right as seen from in front, and y runs up from the base.</p>
 */
public final class GateFormation {

    private GateFormation() {
    }

    /**
     * Forms the gate on the base at {@code basePos} if the ring round it is complete and not already part of a gate. A
     * supergate's base looks for the smallest complete ring of supergate blocks round it.
     *
     * @return whether a gate formed
     */
    public static boolean tryForm(final ServerLevel level, final BlockPos basePos) {
        final BlockState baseState = level.getBlockState(basePos);
        if (!(baseState.getBlock() instanceof StargateBaseBlock baseBlock)
                || !(level.getBlockEntity(basePos) instanceof StargateBaseBlockEntity base)
                || base.isFormed()) {
            return false;
        }
        final Direction facing = baseState.getValue(HorizontalDirectionalBlock.FACING);
        final Optional<GateShape> shape = GateFormation.ringAround(level, basePos, facing, baseBlock.isSupergate());
        if (shape.isEmpty()) {
            return false;
        }
        // A supergate keeps the type it was given; a standard gate is its base's type
        final ResourceKey<GateType> type = baseBlock.isSupergate() ? base.gateType() : baseBlock.gateType();
        final Optional<SGServer> server = SGServer.current();
        final boolean built = server.isPresent() && !server.get().stargates().isPlacing();
        if (built) {
            final Optional<GateType> value = level.registryAccess().lookupOrThrow(SGCraftRegistries.GATE_TYPE)
                    .getOptional(type);
            if (value.isPresent() && !SGEventBus.get().allows(new SGEvents.FormPre(Cause.of(level),
                    GlobalPos.of(level.dimension(), basePos), FrontAndTop.fromFrontAndTop(facing, Direction.UP),
                    value.get(), shape.get().size()))) {
                return false;
            }
        }
        GateFormation.forEachRingBlock(basePos, facing, shape.get(), ringPos -> {
            final BlockState ringState = level.getBlockState(ringPos);
            level.setBlock(ringPos, ringState.setValue(RingBlock.MERGED, true).setValue(RingBlock.LIT, false),
                    Block.UPDATE_ALL);
        });
        level.setBlock(basePos, baseState.setValue(StargateBaseBlock.FORMED, true), Block.UPDATE_ALL);
        base.setGateType(type);
        base.setShape(shape.get());
        base.setFormed(true);
        GateFormation.register(level, basePos, base, facing);
        if (built) {
            server.get().stargates().gate(GlobalPos.of(level.dimension(), basePos)).ifPresent(gate ->
                    SGEventBus.get().post(new SGEvents.FormPost(Cause.of(level), gate)));
        }
        return true;
    }

    // The shape of the complete ring round a base, if there is one: a standard gate's, or the smallest supergate's
    private static Optional<GateShape> ringAround(final ServerLevel level, final BlockPos basePos,
            final Direction facing, final boolean supergate) {
        if (!supergate) {
            return GateFormation.ringMatches(level, basePos, facing, GateShape.STANDARD)
                    ? Optional.of(GateShape.STANDARD) : Optional.empty();
        }
        for (int radius = GateShape.MIN_SUPERGATE_RADIUS; radius <= GateShape.MAX_SUPERGATE_RADIUS; radius++) {
            final GateShape shape = GateShape.supergate(radius);
            if (GateFormation.ringMatches(level, basePos, facing, shape)) {
                return Optional.of(shape);
            }
        }
        return Optional.empty();
    }

    /** Tries to form a gate on every base whose ring could include the ring block at {@code ringPos}. */
    public static void formAround(final ServerLevel level, final BlockPos ringPos, final boolean supergate) {
        GateFormation.forEachBaseAround(level, ringPos, supergate, basePos -> GateFormation.tryForm(level, basePos));
    }

    /** Breaks apart any formed gate whose ring included the block that was at {@code ringPos}. */
    public static void breakAround(final ServerLevel level, final BlockPos ringPos, final boolean supergate) {
        GateFormation.forEachBaseAround(level, ringPos, supergate, basePos -> {
            final BlockState baseState = level.getBlockState(basePos);
            if (level.getBlockEntity(basePos) instanceof StargateBaseBlockEntity base && base.isFormed()) {
                final Direction facing = baseState.getValue(HorizontalDirectionalBlock.FACING);
                if (GateFormation.isInRing(basePos, facing, base.shape(), ringPos)) {
                    level.setBlock(basePos, baseState.setValue(StargateBaseBlock.FORMED, false), Block.UPDATE_ALL);
                    base.setFormed(false);
                    GateFormation.unregister(level, basePos);
                    GateFormation.releaseRing(level, basePos, facing, base.shape());
                }
            }
        });
    }

    /**
     * The base of the formed gate whose ring has the block at {@code pos}, as the server or a client sees it.
     *
     * @param supergate whether the block is a supergate's
     */
    public static Optional<StargateBaseBlockEntity> baseOf(final Level level, final BlockPos pos,
            final boolean supergate) {
        final List<StargateBaseBlockEntity> found = new ArrayList<>();
        GateFormation.forEachBaseAround(level, pos, supergate, basePos -> {
            if (found.isEmpty() && level.getBlockEntity(basePos) instanceof StargateBaseBlockEntity base
                    && base.isFormed() && GateFormation.isInRing(basePos,
                            level.getBlockState(basePos).getValue(HorizontalDirectionalBlock.FACING), base.shape(),
                            pos)) {
                found.add(base);
            }
        });
        return found.stream().findFirst();
    }

    /** Un-merges every ring block of a gate of this shape whose base faced {@code facing}, such as after it broke. */
    public static void releaseRing(final ServerLevel level, final BlockPos basePos, final Direction facing,
            final GateShape shape) {
        GateFormation.forEachRingBlock(basePos, facing, shape, ringPos -> {
            final BlockState ringState = level.getBlockState(ringPos);
            if (ringState.getBlock() instanceof RingBlock && ringState.getValue(RingBlock.MERGED)) {
                level.setBlock(ringPos, ringState.setValue(RingBlock.MERGED, false).setValue(RingBlock.LIT, false),
                        Block.UPDATE_ALL);
            }
        });
    }

    /** Records a formed gate with the server's gates, so it can be found and dialed. */
    public static void register(final ServerLevel level, final BlockPos basePos, final StargateBaseBlockEntity base,
            final Direction facing) {
        SGServer.current().ifPresent(server -> server.stargates().formed(GlobalPos.of(level.dimension(), basePos),
                base.gateType(), facing, base.chevrons(), base.shape()));
    }

    /** Forgets a gate that is no longer formed. */
    public static void unregister(final ServerLevel level, final BlockPos basePos) {
        SGServer.current().ifPresent(server -> server.stargates().broken(GlobalPos.of(level.dimension(), basePos),
                Cause.of(level)));
    }

    /**
     * Every block a standing gate of this shape takes up: its base, its ring and the space inside it.
     *
     * @return the blocks, in the world
     */
    public static List<BlockPos> footprint(final BlockPos basePos, final Direction facing, final GateShape shape) {
        final Direction right = facing.getCounterClockWise();
        final List<BlockPos> blocks = new ArrayList<>();
        for (int x = -shape.radius(); x <= shape.radius(); x++) {
            for (int y = 0; y < shape.height(); y++) {
                if (GateFormation.inGate(shape, x, y)) {
                    blocks.add(basePos.relative(right, x).above(y));
                }
            }
        }
        return blocks;
    }

    /**
     * Builds a gate's ring round where its base will go, unmerged, and clears the space inside it. Placing the base
     * afterwards forms the gate.
     */
    public static void buildRing(final ServerLevel level, final BlockPos basePos, final Direction facing,
            final GateShape shape) {
        final Direction right = facing.getCounterClockWise();
        for (int x = -shape.radius(); x <= shape.radius(); x++) {
            for (int y = 0; y < shape.height(); y++) {
                if ((x == 0 && y == 0) || !GateFormation.inGate(shape, x, y)) {
                    continue;
                }
                final Block block = switch (shape.cellAt(x, y)) {
                    case GateShape.RING -> shape.isSupergate() ? SGBlocks.SUPERGATE_RING : SGBlocks.STARGATE_RING;
                    case GateShape.CHEVRON -> shape.isSupergate() ? SGBlocks.SUPERGATE_CHEVRON
                            : SGBlocks.STARGATE_CHEVRON;
                    default -> Blocks.AIR;
                };
                // Fluid stays: the space inside keeps any, and the ring's blocks hold water
                final BlockPos pos = basePos.relative(right, x).above(y);
                final boolean water = level.getFluidState(pos).is(Fluids.WATER);
                if (block == Blocks.AIR && level.getBlockState(pos).getBlock() instanceof LiquidBlock) {
                    continue;
                }
                final BlockState state = block == Blocks.AIR ? (water ? Blocks.WATER : Blocks.AIR).defaultBlockState()
                        : block.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, water);
                level.setBlock(pos, state, Block.UPDATE_ALL);
            }
        }
    }

    /** @return whether {@code pos} is the base or a ring block of a gate of this shape whose base faced that way */
    public static boolean isPartOfGate(final BlockPos basePos, final Direction facing, final GateShape shape,
            final BlockPos pos) {
        return pos.equals(basePos) || GateFormation.isInRing(basePos, facing, shape, pos);
    }

    // Whether a cell is the gate's: a standard gate is all of its square, a supergate its ring and what is inside it
    private static boolean inGate(final GateShape shape, final int x, final int y) {
        return !shape.isSupergate() || shape.cellAt(x, y) != GateShape.EMPTY
                || Math.hypot(x, y - shape.centerY()) < shape.radius();
    }

    /**
     * Why the ring round a base doesn't make a gate of this shape: the first block that isn't what the ring needs
     * there, or empty if the ring is complete.
     */
    public static Optional<String> mismatch(final ServerLevel level, final BlockPos basePos, final Direction facing,
            final GateShape shape) {
        final Direction right = facing.getCounterClockWise();
        for (int x = -shape.radius(); x <= shape.radius(); x++) {
            for (int y = 0; y < shape.height(); y++) {
                final int cell = shape.cellAt(x, y);
                if ((x == 0 && y == 0) || (shape.isSupergate() && cell == GateShape.EMPTY)) {
                    continue;
                }
                final BlockPos pos = basePos.relative(right, x).above(y);
                final BlockState state = level.getBlockState(pos);
                if (GateFormation.kindOf(state, shape.isSupergate()) != cell) {
                    final String wanted = cell == GateShape.RING ? "a ring block" : cell == GateShape.CHEVRON
                            ? "a chevron block" : "nothing";
                    return Optional.of(pos.toShortString() + " has " + state + " where the ring needs " + wanted);
                }
            }
        }
        return Optional.empty();
    }

    // A standard gate's ring needs nothing but air inside it; a supergate's, as in the original, only its ring blocks
    private static boolean ringMatches(final ServerLevel level, final BlockPos basePos, final Direction facing,
            final GateShape shape) {
        final Direction right = facing.getCounterClockWise();
        for (int x = -shape.radius(); x <= shape.radius(); x++) {
            for (int y = 0; y < shape.height(); y++) {
                final int cell = shape.cellAt(x, y);
                if ((x == 0 && y == 0) || (shape.isSupergate() && cell == GateShape.EMPTY)) {
                    continue;
                }
                final BlockState state = level.getBlockState(basePos.relative(right, x).above(y));
                if (GateFormation.kindOf(state, shape.isSupergate()) != cell) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int kindOf(final BlockState state, final boolean supergate) {
        if (state.isAir() || state.getBlock() instanceof LiquidBlock || state.getBlock() instanceof EventHorizonBlock) {
            return GateShape.EMPTY;
        }
        if (state.getBlock() instanceof RingBlock ring && ring.isSupergate() == supergate
                && !state.getValue(RingBlock.MERGED)) {
            return ring.isChevron() ? GateShape.CHEVRON : GateShape.RING;
        }
        return -1;
    }

    private static boolean isInRing(final BlockPos basePos, final Direction facing, final GateShape shape,
            final BlockPos pos) {
        final Direction right = facing.getCounterClockWise();
        final BlockPos d = pos.subtract(basePos);
        // The ring stands in the plane of the base, across its front
        if (d.getX() * facing.getStepX() + d.getZ() * facing.getStepZ() != 0) {
            return false;
        }
        final int x = d.getX() * right.getStepX() + d.getZ() * right.getStepZ();
        return shape.cellAt(x, d.getY()) != GateShape.EMPTY;
    }

    private static void forEachRingBlock(final BlockPos basePos, final Direction facing, final GateShape shape,
            final Consumer<BlockPos> action) {
        final Direction right = facing.getCounterClockWise();
        for (int x = -shape.radius(); x <= shape.radius(); x++) {
            for (int y = 0; y < shape.height(); y++) {
                if (shape.cellAt(x, y) != GateShape.EMPTY) {
                    action.accept(basePos.relative(right, x).above(y));
                }
            }
        }
    }

    // Bases whose ring could reach pos: up to a gate's width away to either side, and up to its height below. A
    // supergate's base can be far off, so it is found among the block entities of the chunks round pos, as in the
    // original, rather than block by block.
    private static void forEachBaseAround(final Level level, final BlockPos pos, final boolean supergate,
            final Consumer<BlockPos> action) {
        final List<BlockPos> bases = new ArrayList<>();
        if (!supergate) {
            final int reach = GateShape.STANDARD.radius();
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    for (int dy = -(GateShape.STANDARD.height() - 1); dy <= 0; dy++) {
                        final BlockPos candidate = pos.offset(dx, dy, dz);
                        if (level.getBlockState(candidate).getBlock() instanceof StargateBaseBlock) {
                            bases.add(candidate);
                        }
                    }
                }
            }
        } else {
            final int reach = GateShape.MAX_SUPERGATE_RADIUS;
            final int below = 2 * GateShape.MAX_SUPERGATE_RADIUS;
            for (int cx = (pos.getX() - reach) >> 4; cx <= (pos.getX() + reach) >> 4; cx++) {
                for (int cz = (pos.getZ() - reach) >> 4; cz <= (pos.getZ() + reach) >> 4; cz++) {
                    final LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                    if (chunk == null) {
                        continue;
                    }
                    for (final BlockPos candidate : chunk.getBlockEntities().keySet()) {
                        final int dy = candidate.getY() - pos.getY();
                        if (Math.abs(candidate.getX() - pos.getX()) <= reach
                                && Math.abs(candidate.getZ() - pos.getZ()) <= reach && dy <= 0 && dy >= -below
                                && level.getBlockState(candidate).getBlock() instanceof StargateBaseBlock) {
                            bases.add(candidate.immutable());
                        }
                    }
                }
            }
        }
        // Collected first, as forming and breaking gates changes the chunks' block entities
        bases.forEach(action);
    }
}
