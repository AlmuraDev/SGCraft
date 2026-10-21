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

import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.block.EventHorizonLightBlock;
import com.almuradev.sgcraft.mod.block.SGBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * The light behind a gate whose wormhole is open and whose iris is shut, as in 2.2.0: the iris covers the horizon, so
 * {@link EventHorizonLightBlock}s in the air just behind the gate's opening light its back instead, at the server's
 * horizon light. A raised shield hides no light, so lights none.
 */
public final class GateBackLight {

    private GateBackLight() {
    }

    /** Lights the back of a gate, or puts the light out, as its wormhole and iris now want. */
    public static void update(final ServerLevel level, final SGStargate gate) {
        final int light = GateBackLight.light(gate);
        if (light == 0) {
            GateBackLight.clear(level, gate.frame());
            return;
        }
        final BlockState lit = SGBlocks.EVENT_HORIZON_LIGHT.defaultBlockState()
                .setValue(EventHorizonLightBlock.LEVEL, light);
        GateBackLight.forEachBehind(gate.frame(), pos -> {
            if (!level.isLoaded(pos)) {
                return;
            }
            final BlockState state = level.getBlockState(pos);
            if ((state.isAir() || state.getBlock() instanceof EventHorizonLightBlock) && state != lit) {
                level.setBlock(pos, lit, Block.UPDATE_ALL);
            }
        });
    }

    /** Takes the light from behind a gate away. */
    public static void clear(final Level level, final GateFrame frame) {
        GateBackLight.forEachBehind(frame, pos -> {
            if (level.isLoaded(pos) && level.getBlockState(pos).getBlock() instanceof EventHorizonLightBlock) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        });
    }

    /** @return whether a gate in the world wants a light at a place behind it, now */
    public static boolean wanted(final ServerLevel level, final BlockPos pos) {
        final Optional<SGServer> server = SGServer.current();
        if (server.isEmpty()) {
            return false;
        }
        for (final GateStore.GateRecord record : server.get().stargates().store().gates()) {
            if (!record.position().dimension().equals(level.dimension())) {
                continue;
            }
            final Optional<SGStargate> gate = server.get().stargates().gate(GlobalPos.of(level.dimension(),
                    record.position().pos()));
            if (gate.isPresent() && GateBackLight.light(gate.get()) > 0
                    && GateBackLight.isBehind(gate.get().frame(), pos)) {
                return true;
            }
        }
        return false;
    }

    // How bright the back of a gate is lit: the server's horizon light while its wormhole is open behind a shut iris,
    // and none otherwise
    private static int light(final SGStargate gate) {
        if (gate.connection().isEmpty() || !gate.irisBlocks() || gate.irisState().shieldRaised()) {
            return 0;
        }
        return gate.stargates().config().eventHorizonLight();
    }

    private static boolean isBehind(final GateFrame frame, final BlockPos pos) {
        final BlockPos local = frame.local(pos);
        return local.getZ() == -1 && FluidSpill.inOpening(frame.shape(), local.getX(), local.getY())
                && GateLight.givesLight(frame.shape(), local.getX(), local.getY());
    }

    // The blocks just behind a gate's opening that light its back, as many as its horizon has that give light
    private static void forEachBehind(final GateFrame frame, final Consumer<BlockPos> action) {
        final GateShape shape = frame.shape();
        for (int y = 1; y < shape.height() - 1; y++) {
            for (int x = -shape.radius(); x <= shape.radius(); x++) {
                if (FluidSpill.inOpening(shape, x, y) && GateLight.givesLight(shape, x, y)) {
                    action.accept(frame.block(x, y, -1));
                }
            }
        }
    }
}
