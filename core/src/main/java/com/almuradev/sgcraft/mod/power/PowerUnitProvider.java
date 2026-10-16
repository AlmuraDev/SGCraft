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

package com.almuradev.sgcraft.mod.power;

import com.almuradev.sgcraft.api.energy.EnergySource;
import com.almuradev.sgcraft.api.energy.EnergySourceProvider;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.block.entity.PowerUnitBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The power units near a gate, within {@link #REACH} blocks of its base each way, as the original looked for them. */
public final class PowerUnitProvider implements EnergySourceProvider {

    /** How far from a gate's base its power units may be, along each axis. */
    public static final int REACH = 4;

    @Override
    public List<EnergySource> sourcesFor(final Stargate gate) {
        final ServerLevel level = SGServer.current().map(server -> server.server().getLevel(gate.position()
                .dimension())).orElse(null);
        if (level == null) {
            return List.of();
        }
        final BlockPos base = gate.position().pos();
        final int reach = PowerUnitProvider.REACH;
        final List<PowerUnitBlockEntity> units = new ArrayList<>();
        for (final BlockPos pos : BlockPos.betweenClosed(base.offset(-reach, -reach, -reach),
                base.offset(reach, reach, reach))) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof PowerUnitBlockEntity unit) {
                units.add(unit);
            }
        }
        // The nearest first
        units.sort(Comparator.comparingDouble(unit -> unit.getBlockPos().distSqr(base)));
        final List<EnergySource> sources = new ArrayList<>();
        for (final PowerUnitBlockEntity unit : units) {
            sources.add(new EnergySource() {
                @Override
                public long available() {
                    return unit.available();
                }

                @Override
                public long draw(final long wanted, final boolean simulate) {
                    return unit.draw(wanted, simulate);
                }
            });
        }
        return sources;
    }
}
