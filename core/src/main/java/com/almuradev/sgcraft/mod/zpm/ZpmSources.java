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

package com.almuradev.sgcraft.mod.zpm;

import com.almuradev.sgcraft.api.energy.EnergySource;
import com.almuradev.sgcraft.api.energy.EnergySourceProvider;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.block.entity.StargateBaseBlockEntity;
import com.almuradev.sgcraft.mod.gate.GateShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The ZPMs a gate draws on: those in its base's own ZPM slots if it has them, then those in the ZPM consoles,
 * interface carts and hubs within the server's search range of its base and those linked to it (see
 * {@link ZpmLinks}) that are loaded, the nearest first. A ZPM is only a great
 * deal of energy, so any gate may draw on one, and a supergate needs no more of them than any other energy.
 */
public final class ZpmSources implements EnergySourceProvider {

    @Override
    public List<EnergySource> sourcesFor(final Stargate gate) {
        final List<EnergySource> sources = new ArrayList<>();
        ZpmSources.base(gate).filter(base -> !Zpms.charged(base.zpms()).isEmpty()).ifPresent(base ->
                sources.add(new EnergySource() {
                    @Override
                    public long available() {
                        return Zpms.available(Zpms.charged(base.zpms()));
                    }

                    @Override
                    public long draw(final long wanted, final boolean simulate) {
                        if (simulate) {
                            return Math.max(0, Math.min(wanted, this.available()));
                        }
                        final long given = Zpms.drawEnergy(Zpms.charged(base.zpms()), wanted);
                        base.zpms().setChanged();
                        return given;
                    }
                }));
        for (final ZpmDeviceBlockEntity device : ZpmSources.devices(gate)) {
            sources.add(new EnergySource() {
                @Override
                public long available() {
                    return device.isRemoved() ? 0 : device.available();
                }

                @Override
                public long draw(final long wanted, final boolean simulate) {
                    return device.isRemoved() ? 0 : device.draw(wanted, simulate);
                }
            });
        }
        return sources;
    }

    /**
     * @return the ZPM devices within the server's search range of a gate's base, or linked to it and loaded, that
     *     hold charged ZPMs, the nearest first
     */
    public static List<ZpmDeviceBlockEntity> devices(final Stargate gate) {
        final Optional<ServerLevel> level = ZpmSources.level(gate);
        final List<ZpmDeviceBlockEntity> devices = new ArrayList<>();
        if (level.isEmpty()) {
            return devices;
        }
        final BlockPos base = gate.position().pos();
        final int reach = ZpmSettings.current().searchRange();
        for (final BlockPos pos : BlockPos.betweenClosed(base.offset(-reach, -reach, -reach),
                base.offset(reach, reach, reach))) {
            if (level.get().isLoaded(pos) && level.get().getBlockEntity(pos)
                    instanceof final ZpmDeviceBlockEntity device && device.loaded() > 0) {
                devices.add(device);
            }
        }
        for (final GlobalPos linked : ZpmLinks.devicesOf(level.get().getServer(), gate.position())) {
            if (linked.dimension().equals(level.get().dimension()) && level.get().isLoaded(linked.pos())
                    && level.get().getBlockEntity(linked.pos()) instanceof final ZpmDeviceBlockEntity device
                    && device.loaded() > 0 && !devices.contains(device)) {
                devices.add(device);
            }
        }
        devices.sort(Comparator.comparingDouble(device -> device.getBlockPos().distSqr(base)));
        return devices;
    }

    /** @return a supergate's base, whose ZPM slots it draws on; none for a standard gate */
    public static Optional<StargateBaseBlockEntity> base(final Stargate gate) {
        if (!ZpmSources.supergate(gate)) {
            return Optional.empty();
        }
        return ZpmSources.level(gate).filter(level -> level.isLoaded(gate.position().pos()))
                .map(level -> level.getBlockEntity(gate.position().pos()))
                .filter(StargateBaseBlockEntity.class::isInstance).map(StargateBaseBlockEntity.class::cast);
    }

    /** @return how many charged ZPMs a gate can draw on */
    public static int count(final Stargate gate) {
        int count = ZpmSources.base(gate).map(base -> Zpms.charged(base.zpms()).size()).orElse(0);
        for (final ZpmDeviceBlockEntity device : ZpmSources.devices(gate)) {
            count += device.loaded();
        }
        return count;
    }

    /** @return how much whole gate energy the ZPMs a gate can draw on hold between them */
    public static long stored(final Stargate gate) {
        long units = ZpmSources.base(gate).map(base -> Zpms.available(Zpms.charged(base.zpms()))).orElse(0L);
        for (final ZpmDeviceBlockEntity device : ZpmSources.devices(gate)) {
            units += (long) device.stored();
        }
        return units;
    }

    /** @return whether a gate is a supergate */
    public static boolean supergate(final Stargate gate) {
        return gate.size().radius() > GateShape.STANDARD.radius();
    }

    private static Optional<ServerLevel> level(final Stargate gate) {
        return SGServer.current().map(server -> server.server().getLevel(gate.position().dimension()));
    }
}
