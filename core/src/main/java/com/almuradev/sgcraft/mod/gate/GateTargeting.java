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

import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.block.entity.DhdBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

/**
 * Finds the gate a player means, as the original SGCraft's tools did: the gate they are looking at, else the gate
 * whose base or DHD is nearest them, else the gate nearest them, each within a range.
 */
public final class GateTargeting {

    // How finely a look is followed, and how far past what it hits, in blocks
    private static final double STEP = 0.25;

    private GateTargeting() {
    }

    /** @return the gate a player using a tool means, within {@code range} blocks */
    public static Optional<Stargate> find(final Entity entity, final int range) {
        final Optional<Stargate> looked = GateTargeting.lookedAt(entity, Math.max(range, 6));
        if (looked.isPresent()) {
            return looked;
        }
        if (!(entity.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }
        final Optional<SGServer> server = SGServer.current();
        if (server.isEmpty()) {
            return Optional.empty();
        }
        // The nearest base or DHD round the player's feet; a DHD stands for its gate
        final BlockPos feet = entity.blockPosition();
        Optional<Stargate> best = Optional.empty();
        double bestDistance = Double.MAX_VALUE;
        for (final BlockPos pos : BlockPos.betweenClosed(feet.offset(-range, -range, -range),
                feet.offset(range, range, range))) {
            final Optional<Stargate> gate;
            if (level.getBlockEntity(pos) instanceof DhdBlockEntity dhd) {
                gate = dhd.gate();
            } else {
                gate = server.get().stargates().gate(GlobalPos.of(level.dimension(), pos)).map(Stargate.class::cast);
            }
            final double distance = pos.distSqr(feet);
            if (gate.isPresent() && distance < bestDistance) {
                best = gate;
                bestDistance = distance;
            }
        }
        if (best.isPresent()) {
            return best;
        }
        // The gate whose ring or opening is nearest the player's eyes
        final Vec3 eye = entity.getEyePosition();
        return server.get().stargates().in(level.dimension()).stream()
                .filter(gate -> GateTargeting.distance(gate, eye) <= range)
                .min(Comparator.comparingDouble(gate -> GateTargeting.distance(gate, eye)));
    }

    /** @return the first gate along the entity's view, through its ring or its opening, before anything solid */
    public static Optional<Stargate> lookedAt(final Entity entity, final double distance) {
        final Vec3 eye = entity.getEyePosition();
        final Vec3 view = entity.getViewVector(1.0F);
        final HitResult hit = entity.level().clip(new ClipContext(eye, eye.add(view.scale(distance)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        // Past the block it hits, so a gate's own ring counts
        final double reach = hit.getType() == HitResult.Type.MISS ? distance
                : hit.getLocation().distanceTo(eye) + GateTargeting.STEP;
        final Optional<SGServer> server = SGServer.current();
        if (server.isEmpty()) {
            return Optional.empty();
        }
        final Collection<Stargate> gates = server.get().stargates().in(entity.level().dimension());
        for (double d = 0; d <= reach; d += GateTargeting.STEP) {
            final Vec3 point = eye.add(view.scale(d));
            for (final Stargate gate : gates) {
                if (GateTargeting.covers(gate, point)) {
                    return Optional.of(gate);
                }
            }
        }
        return Optional.empty();
    }

    /** @return whether a point is in the gate's ring or its opening: the blocks across and up from its base */
    public static boolean covers(final Stargate gate, final Vec3 point) {
        final Vec3 local = GateTargeting.frame(gate).toLocal(point);
        final double half = GateTargeting.halfWidth(gate);
        return Math.abs(local.x) <= half && local.y >= -0.5 && local.y <= half * 2 - 0.5
                && Math.abs(local.z) <= 0.5;
    }

    // The gate's frame, standing or lying flat; only its orientation counts here, not its shape
    private static GateFrame frame(final Stargate gate) {
        return GateFrame.of(gate.position().pos(), gate.orientation(), GateShape.STANDARD);
    }

    // Half the gate's width, to the outside of its ring, whatever its size
    private static double halfWidth(final Stargate gate) {
        return gate.size().radius() + 0.5;
    }

    // How far a point is from the gate's ring and opening
    private static double distance(final Stargate gate, final Vec3 point) {
        final Vec3 local = GateTargeting.frame(gate).toLocal(point);
        final double half = GateTargeting.halfWidth(gate);
        final double dx = Math.max(0, Math.abs(local.x) - half);
        final double dy = Math.max(0, Math.max(-0.5 - local.y, local.y - (half * 2 - 0.5)));
        final double dz = Math.max(0, Math.abs(local.z) - 0.5);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
