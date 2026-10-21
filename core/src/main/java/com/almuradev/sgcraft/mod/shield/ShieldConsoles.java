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

package com.almuradev.sgcraft.mod.shield;

import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.access.GateRight;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.gate.GateShape;
import com.almuradev.sgcraft.mod.gate.GateStore;
import com.almuradev.sgcraft.mod.gate.SGIris;
import com.almuradev.sgcraft.mod.gate.SGStargate;
import com.almuradev.sgcraft.mod.network.SGNetwork;
import com.almuradev.sgcraft.mod.tool.ToolActions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Optional;

/**
 * What shield consoles do on the server: each time it is used, a console finds the nearest gate with a shield within
 * its range, measured to the nearest part of the gate's ring, and its screen raises and lowers that gate's shield.
 * Doing so needs the gate's iris right, as a GDO does.
 */
public final class ShieldConsoles {

    // How near a console its user must stay, in blocks
    private static final double USE_RANGE = 8;

    private ShieldConsoles() {
    }

    /** A player used a console: its screen opens. */
    public static void open(final ServerPlayer player, final BlockPos console) {
        ShieldConsoles.send(player, console, true, "");
    }

    /** Something was done on a console's screen. */
    public static void action(final ServerPlayer player, final ShieldPayloads.ConsoleAction action) {
        // Reach first: looking up a far position would load or generate its chunk
        if (!action.console().closerToCenterThan(player.position(), ShieldConsoles.USE_RANGE)
                || !(player.level().getBlockState(action.console()).getBlock() instanceof ShieldConsoleBlock)) {
            return;
        }
        String message = "";
        if (action.kind() != ShieldPayloads.ConsoleAction.Kind.REFRESH) {
            final Optional<SGStargate> gate = ShieldConsoles.gateFor(player, action.console());
            if (gate.isEmpty()) {
                message = "There is no shielded gate in range";
            } else if (!gate.get().access().allows(ToolActions.cause(player), GateRight.IRIS)) {
                message = "You don't have access to this gate's shield";
            } else {
                message = ToolActions.iris(gate.get(), action.kind() == ShieldPayloads.ConsoleAction.Kind.LOWER,
                        Cause.of(player.level().getBlockState(action.console()), player));
            }
        }
        ShieldConsoles.send(player, action.console(), false, message);
    }

    private static void send(final ServerPlayer player, final BlockPos console, final boolean open,
            final String message) {
        final Optional<SGStargate> gate = ShieldConsoles.gateFor(player, console);
        final boolean control = gate.map(g -> g.access().allows(ToolActions.cause(player), GateRight.IRIS))
                .orElse(false);
        SGNetwork.send(player, new ShieldPayloads.ConsoleState(open, console,
                gate.map(g -> ToolActions.info(player, g)), ShieldSettings.current().consoleRange(), control, message));
    }

    /** @return the nearest gate with a shield within a console's range, in the console's world */
    public static Optional<SGStargate> gateFor(final ServerPlayer player, final BlockPos console) {
        final int range = ShieldSettings.current().consoleRange();
        final Vec3 at = Vec3.atCenterOf(console);
        return SGServer.current().flatMap(server -> server.stargates().store().gates().stream()
                .filter(record -> record.position().dimension().equals(player.level().dimension()))
                .filter(record -> ShieldConsoles.distance(record, at) <= range)
                .sorted(Comparator.comparingDouble(record -> ShieldConsoles.distance(record, at)))
                .map(record -> server.stargates().gate(record.position()))
                .flatMap(Optional::stream)
                .filter(gate -> gate.get(GateProperties.IRIS).map(SGIris::isShield).orElse(false))
                .findFirst());
    }

    // How far a point is from the nearest part of a gate's ring
    private static double distance(final GateStore.GateRecord record, final Vec3 at) {
        final GateShape shape = record.shape();
        final Vec3 local = record.frame().toLocal(at);
        final double across = Math.hypot(local.x, local.y - shape.radius());
        return Math.hypot(Math.max(0, across - shape.radius()), local.z);
    }
}
