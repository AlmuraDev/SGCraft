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

import com.almuradev.sgcraft.mod.access.SGGateAccess;
import com.almuradev.sgcraft.mod.network.SGNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * What city shield generators' screens do on the server. The generator's owner and operators work it; others only see
 * how it stands. A player must stay near the generator to work it.
 */
public final class CityShieldActions {

    // How near a generator its user must stay, in blocks
    private static final double USE_RANGE = 8;

    private CityShieldActions() {
    }

    /** A player used a generator: its screen opens, and is sent the shield's state. */
    public static void open(final ServerPlayer player, final BlockPos generator) {
        if (player.level().getBlockEntity(generator) instanceof final CityShieldBlockEntity shield) {
            shield.open(player);
            CityShieldActions.send(player, generator, false, "");
        }
    }

    /** Something was done on a generator's screen. */
    public static void action(final ServerPlayer player, final ShieldPayloads.CityAction action) {
        if (!(player.level().getBlockEntity(action.generator()) instanceof final CityShieldBlockEntity shield)
                || !action.generator().closerToCenterThan(player.position(), CityShieldActions.USE_RANGE)) {
            return;
        }
        String message = "";
        if (action.kind() != ShieldPayloads.CityAction.Kind.REFRESH) {
            if (!CityShieldActions.mayWork(player, shield)) {
                message = "Only the generator's owner can work it";
            } else {
                switch (action.kind()) {
                    case RAISE -> {
                        shield.resize(action.width(), action.above(), action.below());
                        if (!shield.raise()) {
                            message = "There isn't enough energy around the generator to raise a shield that size";
                        }
                    }
                    case APPLY -> shield.resize(action.width(), action.above(), action.below());
                    case LOWER -> shield.lower();
                    default -> {
                    }
                }
            }
        }
        CityShieldActions.send(player, action.generator(), false, message);
    }

    /** @return whether a player may work a generator: its owner, an operator, or anyone if it has no owner */
    public static boolean mayWork(final ServerPlayer player, final CityShieldBlockEntity shield) {
        return shield.owner().map(owner -> owner.equals(player.getUUID())).orElse(true)
                || SGGateAccess.isOperator(player);
    }

    private static void send(final ServerPlayer player, final BlockPos generator, final boolean open,
            final String message) {
        if (!(player.level().getBlockEntity(generator) instanceof final CityShieldBlockEntity shield)) {
            return;
        }
        final ShieldSettings settings = ShieldSettings.current();
        SGNetwork.send(player, new ShieldPayloads.CityState(open, generator, shield.active(), shield.width(),
                shield.above(), shield.below(), settings.cityMaxRadius(), settings.cityEnergyPerMillionBlocks(),
                shield.available(), CityShieldActions.mayWork(player, shield), message));
    }
}
