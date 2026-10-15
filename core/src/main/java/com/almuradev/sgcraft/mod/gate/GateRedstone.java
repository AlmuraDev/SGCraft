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

import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.Iris;
import com.almuradev.sgcraft.mod.SGServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Redstone reaching a gate's base: a signal arriving shuts the iris and one going opens it, as in the original, if
 * the gate's {@link GateProperties#REDSTONE_INPUT} allows it.
 */
public final class GateRedstone {

    private GateRedstone() {
    }

    public static void powered(final ServerLevel level, final BlockPos base, final boolean powered) {
        SGServer.current().flatMap(server -> server.stargates().gate(GlobalPos.of(level.dimension(), base)))
                .filter(gate -> gate.get(GateProperties.REDSTONE_INPUT).orElse(true))
                .flatMap(SGStargate::iris)
                .ifPresent(iris -> GateRedstone.work(iris, level, base, powered));
    }

    private static void work(final Iris iris, final ServerLevel level, final BlockPos base, final boolean powered) {
        final Cause cause = Cause.of(level.getBlockState(base), level);
        if (powered) {
            iris.close(cause);
        } else {
            iris.open(cause);
        }
    }
}
