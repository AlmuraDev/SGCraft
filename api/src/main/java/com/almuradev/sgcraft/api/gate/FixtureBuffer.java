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

package com.almuradev.sgcraft.api.gate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A set of blocks to build around a gate, such as a DHD and the platform it stands on. Callers can change a buffer
 * freely before handing it over to be built.
 *
 * <p>Blocks are laid out relative to the gate's base block, for a gate standing upright and facing north
 * ({@link net.minecraft.core.FrontAndTop#NORTH_UP}): x runs to the gate's right as seen from in front, y towards
 * the top of the ring, and z out of the gate's front. Implementations must turn every offset and block state to
 * match the gate's actual orientation when they build the buffer. The order blocks are built in is up to the
 * implementation, which must build blocks that need support, such as torches or doors, after the blocks they rest
 * on.</p>
 *
 * <p>This class isn't thread safe.</p>
 *
 * @since 0.1.0
 */
public final class FixtureBuffer {

    private final Map<BlockPos, Entry> entries = new HashMap<>();

    /** Makes an empty buffer. */
    public FixtureBuffer() {
    }

    /**
     * Sets the block at an offset, replacing any already there.
     *
     * @param offset where the block goes, relative to the gate's base block
     * @param state the block
     * @return this buffer
     */
    public FixtureBuffer set(final Vec3i offset, final BlockState state) {
        return this.set(offset, state, null);
    }

    /**
     * Sets the block at an offset, with data for its block entity, replacing any already there.
     *
     * @param offset where the block goes, relative to the gate's base block
     * @param state the block
     * @param blockEntityData data to load into the block's block entity, or null for none
     * @return this buffer
     */
    public FixtureBuffer set(final Vec3i offset, final BlockState state, final CompoundTag blockEntityData) {
        final CompoundTag data = blockEntityData == null ? null : blockEntityData.copy();
        this.entries.put(FixtureBuffer.key(offset), new Entry(Objects.requireNonNull(state, "state"), data));
        return this;
    }

    /**
     * @param offset an offset
     * @return this buffer, without the block at that offset
     */
    public FixtureBuffer remove(final Vec3i offset) {
        this.entries.remove(FixtureBuffer.key(offset));
        return this;
    }

    /** @return this buffer, emptied */
    public FixtureBuffer clear() {
        this.entries.clear();
        return this;
    }

    /**
     * @param offset an offset
     * @return the block at that offset, if one is set
     */
    public Optional<BlockState> state(final Vec3i offset) {
        final Entry entry = this.entries.get(FixtureBuffer.key(offset));
        return entry == null ? Optional.empty() : Optional.of(entry.state());
    }

    /**
     * @param offset an offset
     * @return a copy of the block entity data at that offset, if any is set
     */
    public Optional<CompoundTag> blockEntityData(final Vec3i offset) {
        final Entry entry = this.entries.get(FixtureBuffer.key(offset));
        if (entry == null || entry.blockEntityData() == null) {
            return Optional.empty();
        }
        return Optional.of(entry.blockEntityData().copy());
    }

    /** @return every block set, by offset */
    public Map<BlockPos, BlockState> states() {
        final Map<BlockPos, BlockState> states = new HashMap<>();
        this.entries.forEach((offset, entry) -> states.put(offset, entry.state()));
        return Collections.unmodifiableMap(states);
    }

    /** @return whether no blocks are set */
    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    /** @return a separate buffer with the same blocks */
    public FixtureBuffer copy() {
        final FixtureBuffer copy = new FixtureBuffer();
        this.entries.forEach((offset, entry) -> copy.set(offset, entry.state(), entry.blockEntityData()));
        return copy;
    }

    private static BlockPos key(final Vec3i offset) {
        Objects.requireNonNull(offset, "offset");
        return new BlockPos(offset.getX(), offset.getY(), offset.getZ());
    }

    private record Entry(BlockState state, CompoundTag blockEntityData) {
    }
}
