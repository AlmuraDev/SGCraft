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

import com.almuradev.sgcraft.api.gate.FixtureBuffer;
import com.almuradev.sgcraft.api.gate.PlacementMode;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.block.RingBlock;
import com.almuradev.sgcraft.mod.block.StargateBaseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Builds a {@link FixtureBuffer} round a gate: each offset and block turned from a gate facing north to the gate's
 * own facing, lower blocks first, and blocks that need support after the blocks they rest on. Round a gate lying flat
 * they are built as round a gate standing on its base, so they stand on the ground beside it.
 */
public final class Fixtures {

    private Fixtures() {
    }

    /**
     * @return whether the fixtures were built; with {@link PlacementMode#ATTEMPT}, nothing is built unless every spot
     *     is clear. A gate's own blocks are never replaced, in either mode
     */
    public static boolean build(final ServerLevel level, final GateFrame gate, final FixtureBuffer fixtures,
            final PlacementMode mode) {
        final GateFrame frame = gate.upright();
        final Rotation rotation = Fixtures.rotation(frame.facing());
        final List<Placed> placed = new ArrayList<>();
        for (final Map.Entry<BlockPos, BlockState> entry : fixtures.states().entrySet()) {
            final BlockPos offset = entry.getKey();
            final BlockPos pos = frame.block(offset.getX(), offset.getY(), offset.getZ());
            final BlockState there = level.getBlockState(pos);
            if (there.getBlock() instanceof RingBlock || there.getBlock() instanceof StargateBaseBlock) {
                return false;
            }
            if (mode == PlacementMode.ATTEMPT && !there.canBeReplaced()) {
                return false;
            }
            placed.add(new Placed(pos, entry.getValue().rotate(rotation), fixtures.blockEntityData(offset)));
        }
        // Lowest first, and blocks that stand on others after the full blocks
        placed.sort(Comparator.<Placed>comparingInt(p -> p.state.isCollisionShapeFullBlock(level, p.pos) ? 0 : 1)
                .thenComparingInt(p -> p.pos.getY()));
        for (final Placed p : placed) {
            level.setBlock(p.pos, p.state, Block.UPDATE_ALL);
            p.data.ifPresent(data -> Fixtures.load(level, p.pos, data));
        }
        return true;
    }

    private static void load(final ServerLevel level, final BlockPos pos, final CompoundTag data) {
        final BlockEntity entity = level.getBlockEntity(pos);
        if (entity == null) {
            SGCraft.LOG.warn("A fixture at {} has data for a block entity but no block entity", pos);
            return;
        }
        entity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data));
        entity.setChanged();
    }

    /** @return the turn from a gate facing north to one facing the given way */
    public static Rotation rotation(final Direction facing) {
        return switch (facing) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    private record Placed(BlockPos pos, BlockState state, Optional<CompoundTag> data) {
    }
}
