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

package com.almuradev.sgcraft.mod.gate;

import com.almuradev.sgcraft.mod.block.EventHorizonBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

/**
 * Fluid carried through a wormhole: a gate standing in fluid sends it to the gate at the other end, which pours it out
 * of its opening, more the deeper the first gate is in it.
 *
 * @param fluid the fluid, as its source
 * @param amount how much of the first gate's opening the fluid covers, and so of the far gate's opening it pours
 *     from, from its floor up: 0 none, 1 all of it
 */
public record FluidSpill(Fluid fluid, float amount) {

    /** The most source blocks a body of fluid can have and still drain; a bigger one, such as the sea, is endless. */
    public static final int MOST_DRAINED = 4096;

    /**
     * The fluid a gate's opening is full of, if any: whichever fills most of it, and how much of it that is. A block of
     * the opening counts as much as it is full, so flowing fluid only part of the way up a block counts for that part.
     * Where the opening is the horizon of a gate sending through the wormhole, the fluid running into it from the front
     * counts instead.
     */
    public static Optional<FluidSpill> in(final Level level, final GateFrame frame) {
        final GateShape shape = frame.shape();
        final Map<Fluid, Float> counts = new HashMap<>();
        int cells = 0;
        for (int y = 1; y < shape.height() - 1; y++) {
            for (int x = -shape.radius(); x <= shape.radius(); x++) {
                if (!FluidSpill.inOpening(shape, x, y)) {
                    continue;
                }
                cells++;
                final FluidState state = level.getFluidState(FluidSpill.measured(level, frame, x, y));
                if (!state.isEmpty()) {
                    counts.merge(FluidSpill.source(state.getType()), state.getAmount() / (float) FluidSpill.FULL,
                            Float::sum);
                }
            }
        }
        final int total = cells;
        return counts.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(most -> new FluidSpill(most.getKey(), total == 0 ? 0 : Math.min(1, most.getValue() / total)));
    }

    // How much fluid fills a block: a source's amount
    private static final int FULL = 8;

    // Where a block of the opening is measured: the block, or in front of it for the horizon of a gate sending
    private static BlockPos measured(final Level level, final GateFrame frame, final int x, final int y) {
        final BlockPos pos = frame.block(x, y, 0);
        return EventHorizonBlock.isSending(level.getBlockState(pos)) ? frame.block(x, y, 1) : pos;
    }

    /** @return whether a block of the gate's frame is in its opening, where fluid is measured and poured */
    public static boolean inOpening(final GateShape shape, final int x, final int y) {
        return shape.isSupergate() ? shape.inOpening(x, y) : Math.abs(x) < shape.radius() && y >= 1
                && y < shape.height() - 1;
    }

    /** @return the height above the base's center up to which the opening pours */
    public double top(final GateShape shape) {
        return shape.openingFloor() + this.amount * (shape.openingTop() - shape.openingFloor());
    }

    /** @return the source of a fluid, such as water for flowing water */
    public static Fluid source(final Fluid fluid) {
        return fluid instanceof FlowingFluid flowing ? flowing.getSource() : fluid;
    }

    /** Throws the fluid out of the gate's front for a tick, as particles, for clients to see. */
    public void spray(final Level level, final GateFrame frame, final RandomSource random) {
        final GateShape shape = frame.shape();
        final double top = this.top(shape);
        // More the more it pours and the wider the gate, but never so many a big gate slows the client
        final int count = Math.min(60, 1 + (int) (this.amount * shape.openingHalfWidth() * 6));
        for (int i = 0; i < count; i++) {
            final double y = shape.openingFloor() + random.nextDouble() * (top - shape.openingFloor());
            final double half = shape.openingHalfWidthAt(y);
            final double x = (random.nextDouble() * 2 - 1) * half;
            final Vec3 at = frame.toWorld(new Vec3(x, y, 0.15 + random.nextDouble() * 0.3));
            // Out of the front, faster from deeper down in the opening, as though pushed by the fluid above it
            final double push = 0.15 + 0.35 * (top - y) / Math.max(0.5, top - shape.openingFloor());
            final Vec3 motion = frame.directionToWorld(new Vec3((random.nextDouble() - 0.5) * 0.1, 0.05, push));
            level.addParticle(this.particle(random), at.x, at.y, at.z, motion.x, motion.y, motion.z);
        }
    }

    /**
     * The source blocks of the body of fluid in a gate's opening, joined to the opening through one another, if it is
     * small enough to drain: those of the opening, or in front of it where it is the horizon of a gate sending, and
     * every source of the same fluid touching them, blocks holding it included.
     *
     * @param skip blocks that don't count, such as fluid the gate poured itself
     * @return the body's sources, or empty if it is endless: bigger than {@link #MOST_DRAINED}, or reaching where the
     *     world isn't loaded
     */
    public Optional<Set<BlockPos>> body(final Level level, final GateFrame frame, final Collection<BlockPos> skip) {
        final GateShape shape = frame.shape();
        final Set<BlockPos> body = new HashSet<>();
        final Queue<BlockPos> next = new ArrayDeque<>();
        for (int y = 1; y < shape.height() - 1; y++) {
            for (int x = -shape.radius(); x <= shape.radius(); x++) {
                if (!FluidSpill.inOpening(shape, x, y)) {
                    continue;
                }
                final BlockPos pos = FluidSpill.measured(level, frame, x, y);
                if (this.isSource(level, pos, skip) && body.add(pos)) {
                    next.add(pos);
                }
            }
        }
        while (!next.isEmpty()) {
            final BlockPos at = next.remove();
            for (final Direction direction : Direction.values()) {
                final BlockPos pos = at.relative(direction);
                if (body.contains(pos)) {
                    continue;
                }
                if (!level.isLoaded(pos)) {
                    return Optional.empty();
                }
                if (this.isSource(level, pos, skip)) {
                    body.add(pos);
                    if (body.size() > FluidSpill.MOST_DRAINED) {
                        return Optional.empty();
                    }
                    next.add(pos);
                }
            }
        }
        return Optional.of(body);
    }

    private boolean isSource(final Level level, final BlockPos pos, final Collection<BlockPos> skip) {
        final FluidState state = level.getFluidState(pos);
        return state.isSource() && state.getType().isSame(this.fluid) && !skip.contains(pos);
    }

    /** @return the top layer of a body of fluid: its sources at the height of the highest */
    public static List<BlockPos> topLayer(final Set<BlockPos> body) {
        final int top = body.stream().mapToInt(BlockPos::getY).max().orElse(0);
        return body.stream().filter(pos -> pos.getY() == top).toList();
    }

    /**
     * Takes the fluid out of these blocks, as a bucket would: a fluid block goes, and a block holding fluid keeps only
     * itself. A block that lives in fluid without being able to give it up, such as seagrass, goes with it.
     */
    public static void drain(final Level level, final List<BlockPos> blocks) {
        for (final BlockPos pos : blocks) {
            final BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BucketPickup pickup
                    && !pickup.pickupBlock(null, level, pos, state).isEmpty()) {
                continue;
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private ParticleOptions particle(final RandomSource random) {
        if (this.fluid.is(FluidTags.WATER)) {
            return random.nextInt(3) == 0 ? ParticleTypes.FALLING_WATER : ParticleTypes.SPLASH;
        }
        if (this.fluid.is(FluidTags.LAVA)) {
            return random.nextInt(4) == 0 ? ParticleTypes.LAVA : ParticleTypes.FALLING_LAVA;
        }
        // Any other fluid, by its own texture
        return new BlockParticleOption(ParticleTypes.BLOCK, this.fluid.defaultFluidState().createLegacyBlock());
    }

    /**
     * @return the blocks in front of a gate the fluid is placed in, when the gate places real fluid: as many of its
     *     opening's as the fluid covers of the first gate's, at least one, filled from the bottom row up and each row
     *     from the middle out, one block out from its face
     */
    public List<BlockPos> blocks(final GateFrame frame) {
        final GateShape shape = frame.shape();
        final List<int[]> cells = new ArrayList<>();
        for (int y = 1; y < shape.height() - 1; y++) {
            for (int x = -shape.radius(); x <= shape.radius(); x++) {
                if (FluidSpill.inOpening(shape, x, y)) {
                    cells.add(new int[] {x, y});
                }
            }
        }
        cells.sort(Comparator.<int[]>comparingInt(cell -> cell[1]).thenComparingInt(cell -> Math.abs(cell[0])));
        final int count = this.amount <= 0 ? 0 : Math.max(1, Math.round(this.amount * cells.size()));
        final List<BlockPos> blocks = new ArrayList<>();
        for (final int[] cell : cells.subList(0, Math.min(count, cells.size()))) {
            blocks.add(frame.block(cell[0], cell[1], 1));
        }
        return blocks;
    }
}
