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

package com.almuradev.sgcraft.mod.dhd;

import com.almuradev.sgcraft.api.energy.EnergySource;
import com.almuradev.sgcraft.api.energy.EnergySourceProvider;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.block.entity.DhdBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Offers a gate the naquadah in the DHDs linked to it, those within a DHD's link range of its base, unless the gate
 * is set not to take it.
 */
public final class DhdFuelProvider implements EnergySourceProvider {

    @Override
    public List<EnergySource> sourcesFor(final Stargate gate) {
        final Optional<ServerLevel> level = DhdFuelProvider.level(gate);
        if (level.isEmpty() || !gate.get(GateProperties.DHD_FUEL).orElse(true)) {
            return List.of();
        }
        final BlockPos base = gate.position().pos();
        final List<DhdBlockEntity> dhds = new ArrayList<>();
        final int reach = (int) Math.ceil(DhdBlockEntity.range(gate));
        for (final BlockPos pos : BlockPos.betweenClosed(base.offset(-reach, -2, -reach),
                base.offset(reach, 2, reach))) {
            if (level.get().getBlockEntity(pos) instanceof DhdBlockEntity dhd
                    && dhd.gate().filter(linked -> linked.position().pos().equals(base)).isPresent()) {
                dhds.add(dhd);
            }
        }
        // The nearest first
        dhds.sort(Comparator.comparingDouble(dhd -> dhd.getBlockPos().distSqr(base)));
        final List<EnergySource> sources = new ArrayList<>();
        for (final DhdBlockEntity dhd : dhds) {
            sources.add(new EnergySource() {
                @Override
                public long available() {
                    return dhd.isRemoved() ? 0 : dhd.available();
                }

                @Override
                public long draw(final long wanted, final boolean simulate) {
                    return dhd.isRemoved() ? 0 : dhd.draw(wanted, simulate);
                }
            });
        }
        return sources;
    }

    private static Optional<ServerLevel> level(final Stargate gate) {
        return SGServer.current().map(server -> server.server().getLevel(gate.position().dimension()));
    }
}
