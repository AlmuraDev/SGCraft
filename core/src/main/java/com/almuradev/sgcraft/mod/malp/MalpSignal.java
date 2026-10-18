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

package com.almuradev.sgcraft.mod.malp;

import com.almuradev.sgcraft.api.SGCraftApi;
import com.almuradev.sgcraft.api.gate.Connection;
import com.almuradev.sgcraft.api.gate.GateState;
import com.almuradev.sgcraft.api.gate.Stargate;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * How the radio of a console, or of a tablet in a player's hands, reaches a MALP, as on the show: directly, when both
 * are in one world and within range of each other, or relayed through an open wormhole, when a gate within range of
 * the radio is connected to a gate in the MALP's world within range of the MALP. The signal goes through a wormhole
 * either way, whichever end dialed, and through a shut iris, as the show's codes did; when the wormhole shuts, it is
 * lost.
 *
 * @param kind how the signal reaches the MALP
 * @param near the gate by the radio that the signal goes into, when relayed
 * @param far the gate by the MALP that the signal comes out of, when relayed
 */
public record MalpSignal(Kind kind, Optional<GlobalPos> near, Optional<GlobalPos> far) {

    /** No signal at all. */
    public static final MalpSignal NONE = new MalpSignal(Kind.NONE, Optional.empty(), Optional.empty());

    public enum Kind {
        NONE, DIRECT, RELAYED
    }

    /** @return whether the radio reaches the MALP */
    public boolean reaches() {
        return this.kind != Kind.NONE;
    }

    /**
     * Finds how a console reaches a MALP, directly if it can.
     *
     * @param console where the console is
     * @param world the MALP's world
     * @param malp where the MALP is
     * @param range how far the radio reaches, in blocks, from the console and from the far gate
     */
    public static MalpSignal between(final GlobalPos console, final ResourceKey<Level> world, final Vec3 malp,
            final int range) {
        return MalpSignal.between(console.dimension(), Vec3.atCenterOf(console.pos()), world, malp, range);
    }

    /**
     * Finds how a radio at any point, such as a tablet in a player's hands, reaches a MALP, directly if it can.
     *
     * @param radioWorld the radio's world
     * @param from where the radio is
     * @param world the MALP's world
     * @param malp where the MALP is
     * @param range how far the radio reaches, in blocks, from the radio and from the far gate
     */
    public static MalpSignal between(final ResourceKey<Level> radioWorld, final Vec3 from,
            final ResourceKey<Level> world, final Vec3 malp, final int range) {
        final double reach = (double) range * range;
        if (radioWorld.equals(world) && from.distanceToSqr(malp) <= reach) {
            return new MalpSignal(Kind.DIRECT, Optional.empty(), Optional.empty());
        }
        for (final Stargate gate : SGCraftApi.get().stargates().in(radioWorld)) {
            // Only a steady wormhole carries the signal: not while it forms, nor through its kawoosh
            if (gate.state() != GateState.CONNECTED || gate.connection().map(Connection::ticksOpen).orElse(0) <= 0
                    || from.distanceToSqr(Vec3.atCenterOf(gate.position().pos())) > reach) {
                continue;
            }
            final Optional<Stargate> other = gate.connection().flatMap(connection -> connection.other(gate));
            if (other.isPresent() && other.get().position().dimension().equals(world)
                    && malp.distanceToSqr(Vec3.atCenterOf(other.get().position().pos())) <= reach) {
                return new MalpSignal(Kind.RELAYED, Optional.of(gate.position()),
                        Optional.of(other.get().position()));
            }
        }
        return MalpSignal.NONE;
    }
}
