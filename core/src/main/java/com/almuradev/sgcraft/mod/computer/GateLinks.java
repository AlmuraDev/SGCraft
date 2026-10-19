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

package com.almuradev.sgcraft.mod.computer;

import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.SGServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.Optional;

/**
 * How a dialing computer finds the gate it dials: a gate of any type whose base is within the server's
 * {@link ComputerSettings#linkRange() link range} of the computer. A computer keeps its gate while that lasts and
 * stays in reach, and links to the nearest in reach otherwise.
 */
public final class GateLinks {

    private GateLinks() {
    }

    /**
     * @param linked the base of the gate the computer is linked to, or null if it is linked to none
     * @param from where the computer is
     * @return the linked gate, if it still stands in the computer's world and is in reach
     */
    public static Optional<Stargate> inReach(final Level level, final @Nullable BlockPos linked, final BlockPos from) {
        return GateLinks.inReach(level, linked, from, ComputerSettings.current().linkRange());
    }

    /** As {@link #inReach(Level, BlockPos, BlockPos)}, for a reach of the caller's own, in blocks. */
    public static Optional<Stargate> inReach(final Level level, final @Nullable BlockPos linked, final BlockPos from,
            final double range) {
        if (linked == null) {
            return Optional.empty();
        }
        return SGServer.current()
                .flatMap(server -> server.stargates().gate(GlobalPos.of(level.dimension(), linked)))
                .filter(gate -> gate.position().pos().closerThan(from, range))
                .map(Stargate.class::cast);
    }

    /** @return the base of the nearest gate in reach of a place, or null if there is none */
    public static @Nullable BlockPos nearest(final ServerLevel level, final BlockPos from) {
        return GateLinks.nearest(level, from, ComputerSettings.current().linkRange());
    }

    /** As {@link #nearest(ServerLevel, BlockPos)}, for a reach of the caller's own, in blocks. */
    public static @Nullable BlockPos nearest(final ServerLevel level, final BlockPos from, final double range) {
        return SGServer.current()
                .flatMap(server -> server.stargates().in(level.dimension()).stream()
                        .map(gate -> gate.position().pos())
                        .filter(gate -> gate.closerThan(from, range))
                        .min(Comparator.comparingDouble(gate -> gate.distSqr(from))))
                .orElse(null);
    }
}
