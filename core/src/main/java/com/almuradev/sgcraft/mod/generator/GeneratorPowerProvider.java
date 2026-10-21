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

package com.almuradev.sgcraft.mod.generator;

import com.almuradev.sgcraft.api.energy.EnergySource;
import com.almuradev.sgcraft.api.energy.EnergySourceProvider;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.power.PowerUnitProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The Naquadah Generators near a gate, within {@link PowerUnitProvider#REACH} blocks of its base each way, as 2.2.0
 * looked for them, the nearest first; and so its shield's too, which it pays for as it pays for anything else.
 */
public final class GeneratorPowerProvider implements EnergySourceProvider {

    @Override
    public List<EnergySource> sourcesFor(final Stargate gate) {
        final ServerLevel level = SGServer.current().map(server -> server.server().getLevel(gate.position()
                .dimension())).orElse(null);
        if (level == null) {
            return List.of();
        }
        final BlockPos base = gate.position().pos();
        final int reach = PowerUnitProvider.REACH;
        final List<NaquadahGeneratorBlockEntity> generators = new ArrayList<>();
        for (final BlockPos pos : BlockPos.betweenClosed(base.offset(-reach, -reach, -reach),
                base.offset(reach, reach, reach))) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof final NaquadahGeneratorBlockEntity found) {
                generators.add(found);
            }
        }
        generators.sort(Comparator.comparingDouble(found -> found.getBlockPos().distSqr(base)));
        final List<EnergySource> sources = new ArrayList<>();
        for (final NaquadahGeneratorBlockEntity generator : generators) {
            sources.add(new EnergySource() {
                @Override
                public long available() {
                    return generator.available();
                }

                @Override
                public long draw(final long wanted, final boolean simulate) {
                    return generator.draw(wanted, simulate);
                }
            });
        }
        return sources;
    }
}
