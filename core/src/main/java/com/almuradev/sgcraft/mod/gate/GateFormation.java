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

package com.almuradev.sgcraft.mod.gate;

import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.SGCraftRegistries;
import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.block.EventHorizonBlock;
import com.almuradev.sgcraft.mod.block.RingBlock;
import com.almuradev.sgcraft.mod.block.SGBlocks;
import com.almuradev.sgcraft.mod.block.StargateBaseBlock;
import com.almuradev.sgcraft.mod.block.entity.StargateBaseBlockEntity;
import com.almuradev.sgcraft.mod.event.SGEventBus;
import com.almuradev.sgcraft.mod.event.SGEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
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
 * <p>A standard gate is five blocks wide and tall, with its base at the bottom center. A supergate's ring follows a
 * circle, as big as the ring its base finds; see {@link GateShape}. A ring stands upright across the base's front, or,
 * as in the original, lies flat at the base's height, running out from the base along its back. Positions are in the
 * gate's own frame (see {@link GateFrame}): x runs to the gate's right as seen from in front, and y up the ring from
 * the base.</p>
 */
public final class GateFormation {

    private GateFormation() {
    }

    /**
     * Forms the gate on the base at {@code basePos} if the ring round it is complete and not already part of a gate. A
     * supergate's base looks for the smallest complete ring of supergate blocks round it. A standing ring is looked
     * for first, then one lying flat, which faces up unless the base was turned face down before.
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
        final GateOrientation flat = base.orientation() == GateOrientation.FACE_DOWN ? GateOrientation.FACE_DOWN
                : GateOrientation.FACE_UP;
        Optional<GateFrame> frame = GateFormation.ringAround(level, basePos, facing, GateOrientation.STANDING,
                baseBlock.isSupergate());
        if (frame.isEmpty()) {
            frame = GateFormation.ringAround(level, basePos, facing, flat, baseBlock.isSupergate());
        }
        if (frame.isEmpty()) {
            return false;
        }
        // A gate keeps the type its base holds: the base block's own, or the one an upgrade or setting gave it
        final ResourceKey<GateType> type = base.gateType();
        final Optional<SGServer> server = SGServer.current();
        final boolean built = server.isPresent() && !server.get().stargates().isPlacing();
        if (built) {
            final Optional<GateType> value = level.registryAccess().lookupOrThrow(SGCraftRegistries.GATE_TYPE)
                    .getOptional(type);
            if (value.isPresent() && !SGEventBus.get().allows(new SGEvents.FormPre(Cause.of(level),
                    GlobalPos.of(level.dimension(), basePos), frame.get().frontAndTop(), value.get(),
                    frame.get().shape().size()))) {
                return false;
            }
        }
        GateFormation.forEachRingBlock(frame.get(), ringPos -> {
            final BlockState ringState = level.getBlockState(ringPos);
            level.setBlock(ringPos, ringState.setValue(RingBlock.MERGED, true).setValue(RingBlock.LIT, false),
                    Block.UPDATE_ALL);
        });
        level.setBlock(basePos, baseState.setValue(StargateBaseBlock.FORMED, true), Block.UPDATE_ALL);
        base.setGateType(type);
        base.setShape(frame.get().shape());
        base.setOrientation(frame.get().orientation());
        base.setFormed(true);
        final GlobalPos position = GlobalPos.of(level.dimension(), basePos);
        if (GateFormation.register(level, basePos, base)) {
            server.ifPresent(running -> running.stargates().welcome(position, base, built));
        }
        if (built) {
            server.get().stargates().gate(position).ifPresent(gate ->
                    SGEventBus.get().post(new SGEvents.FormPost(Cause.of(level), gate)));
        }
        return true;
    }

    // The frame of the complete ring lying so round a base, if there is one: a standard gate's, or the smallest
    // supergate's
    private static Optional<GateFrame> ringAround(final ServerLevel level, final BlockPos basePos,
            final Direction facing, final GateOrientation orientation, final boolean supergate) {
        if (!supergate) {
            final GateFrame frame = new GateFrame(basePos, facing, orientation, GateShape.STANDARD);
            return GateFormation.ringMatches(level, frame) ? Optional.of(frame) : Optional.empty();
        }
        for (int radius = GateShape.MIN_SUPERGATE_RADIUS; radius <= GateShape.MAX_SUPERGATE_RADIUS; radius++) {
            final GateFrame frame = new GateFrame(basePos, facing, orientation, GateShape.supergate(radius));
            if (GateFormation.ringMatches(level, frame)) {
                return Optional.of(frame);
            }
        }
        return Optional.empty();
    }

    /**
     * Notes the player about to place a gate's block, before it is placed, so a gate the block finishes belongs to
     * them.
     */
    public static void building(final BlockPlaceContext context) {
        if (context.getLevel() instanceof ServerLevel level && context.getPlayer() != null) {
            SGServer.current().ifPresent(server -> server.stargates().building(
                    GlobalPos.of(level.dimension(), context.getClickedPos()), context.getPlayer().getUUID()));
        }
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
                final GateFrame frame = base.frame();
                if (GateFormation.isInRing(frame, ringPos)) {
                    level.setBlock(basePos, baseState.setValue(StargateBaseBlock.FORMED, false), Block.UPDATE_ALL);
                    base.setFormed(false);
                    GateFormation.unregister(level, basePos);
                    GateFormation.releaseRing(level, frame);
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
                    && base.isFormed() && GateFormation.isInRing(base.frame(), pos)) {
                found.add(base);
            }
        });
        return found.stream().findFirst();
    }

    /** Takes away every ring block of a gate in this frame, dropping nothing, as a player in creative mode breaks it. */
    public static void removeRing(final ServerLevel level, final GateFrame frame) {
        GateFormation.forEachRingBlock(frame, ringPos -> {
            if (level.getBlockState(ringPos).getBlock() instanceof RingBlock) {
                level.setBlock(ringPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        });
    }

    /** Un-merges every ring block of a gate in this frame, such as after it broke. */
    public static void releaseRing(final ServerLevel level, final GateFrame frame) {
        GateFormation.forEachRingBlock(frame, ringPos -> {
            final BlockState ringState = level.getBlockState(ringPos);
            if (ringState.getBlock() instanceof RingBlock && ringState.getValue(RingBlock.MERGED)) {
                level.setBlock(ringPos, ringState.setValue(RingBlock.MERGED, false).setValue(RingBlock.LIT, false),
                        Block.UPDATE_ALL);
            }
        });
    }

    /**
     * Records a formed gate with the server's gates, so it can be found and dialed.
     *
     * @return whether the server didn't know of the gate before
     */
    public static boolean register(final ServerLevel level, final BlockPos basePos,
            final StargateBaseBlockEntity base) {
        return SGServer.current().map(server -> server.stargates().formed(GlobalPos.of(level.dimension(), basePos),
                base.gateType(), base.frame(), base.chevrons())).orElse(false);
    }

    /** Forgets a gate that is no longer formed. */
    public static void unregister(final ServerLevel level, final BlockPos basePos) {
        SGServer.current().ifPresent(server -> server.stargates().broken(GlobalPos.of(level.dimension(), basePos),
                Cause.of(level)));
    }

    /**
     * Every block a gate in this frame takes up: its base, its ring and the space inside it.
     *
     * @return the blocks, in the world
     */
    public static List<BlockPos> footprint(final GateFrame frame) {
        final GateShape shape = frame.shape();
        final List<BlockPos> blocks = new ArrayList<>();
        for (int x = -shape.radius(); x <= shape.radius(); x++) {
            for (int y = 0; y < shape.height(); y++) {
                if (GateFormation.inGate(shape, x, y)) {
                    blocks.add(frame.block(x, y, 0));
                }
            }
        }
        return blocks;
    }

    /**
     * Builds a gate's ring round where its base will go, unmerged, and clears the space inside it. Placing the base
     * afterwards forms the gate.
     */
    public static void buildRing(final ServerLevel level, final GateFrame frame) {
        final GateShape shape = frame.shape();
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
                final BlockPos pos = frame.block(x, y, 0);
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

    /** @return whether {@code pos} is the base or a ring block of a gate in this frame */
    public static boolean isPartOfGate(final GateFrame frame, final BlockPos pos) {
        return frame.block(0, 0, 0).equals(pos) || GateFormation.isInRing(frame, pos);
    }

    // Whether a cell is the gate's: a standard gate is all of its square, a supergate its ring and what is inside it
    private static boolean inGate(final GateShape shape, final int x, final int y) {
        return !shape.isSupergate() || shape.cellAt(x, y) != GateShape.EMPTY
                || Math.hypot(x, y - shape.centerY()) < shape.radius();
    }

    /**
     * Why the ring round a base doesn't make a gate in this frame: the first block that isn't what the ring needs
     * there, or empty if the ring is complete.
     */
    public static Optional<String> mismatch(final ServerLevel level, final GateFrame frame) {
        final GateShape shape = frame.shape();
        for (int x = -shape.radius(); x <= shape.radius(); x++) {
            for (int y = 0; y < shape.height(); y++) {
                final int cell = shape.cellAt(x, y);
                if ((x == 0 && y == 0) || (shape.isSupergate() && cell == GateShape.EMPTY)) {
                    continue;
                }
                final BlockPos pos = frame.block(x, y, 0);
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
    private static boolean ringMatches(final ServerLevel level, final GateFrame frame) {
        final GateShape shape = frame.shape();
        for (int x = -shape.radius(); x <= shape.radius(); x++) {
            for (int y = 0; y < shape.height(); y++) {
                final int cell = shape.cellAt(x, y);
                if ((x == 0 && y == 0) || (shape.isSupergate() && cell == GateShape.EMPTY)) {
                    continue;
                }
                final BlockState state = level.getBlockState(frame.block(x, y, 0));
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

    // The ring lies in the plane of the gate's horizon, through the base
    private static boolean isInRing(final GateFrame frame, final BlockPos pos) {
        final BlockPos local = frame.local(pos);
        return local.getZ() == 0 && frame.shape().cellAt(local.getX(), local.getY()) != GateShape.EMPTY;
    }

    private static void forEachRingBlock(final GateFrame frame, final Consumer<BlockPos> action) {
        final GateShape shape = frame.shape();
        for (int x = -shape.radius(); x <= shape.radius(); x++) {
            for (int y = 0; y < shape.height(); y++) {
                if (shape.cellAt(x, y) != GateShape.EMPTY) {
                    action.accept(frame.block(x, y, 0));
                }
            }
        }
    }

    // Bases whose ring could reach pos: for a standing ring up to its width away to either side and its height below,
    // for one lying flat up to its height away at the same level. A supergate's base can be far off, so it is found
    // among the block entities of the chunks round pos, as in the original, rather than block by block.
    private static void forEachBaseAround(final Level level, final BlockPos pos, final boolean supergate,
            final Consumer<BlockPos> action) {
        final List<BlockPos> bases = new ArrayList<>();
        if (!supergate) {
            final int reach = GateShape.STANDARD.height() - 1;
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
            final int reach = 2 * GateShape.MAX_SUPERGATE_RADIUS;
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
