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

package com.almuradev.sgcraft.mod;

import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.event.SGEventBus;
import com.almuradev.sgcraft.mod.event.SGEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Whether a player may break a block of a formed gate. Each loader asks before a player breaks a block, through its
 * own cancelable event, so the block stays whole on the client too. Only players are stopped this way: explosions
 * and commands still break gates, as {@link com.almuradev.sgcraft.api.event.gate.BreakGateEvent.Pre} warns.
 */
public final class GateProtection {

    private GateProtection() {
    }

    /** @return whether the player may break the block at {@code pos}; false tells the player why */
    public static boolean mayBreak(final ServerPlayer player, final ServerLevel level, final BlockPos pos) {
        final Optional<SGServer> server = SGServer.current();
        if (server.isEmpty()) {
            return true;
        }
        final Optional<Stargate> gate = server.get().stargates().at(GlobalPos.of(level.dimension(), pos));
        if (gate.isEmpty()) {
            return true;
        }
        if (!SGGateAccess.isOperator(player) && !gate.get().get(GateProperties.BREAKABLE).orElse(false)) {
            player.sendOverlayMessage(Component.translatableWithFallback("message.sgcraft.unbreakable",
                    "This gate can't be broken").withStyle(ChatFormatting.RED));
            return false;
        }
        return SGEventBus.get().allows(new SGEvents.BreakPre(Cause.of(player), gate.get()));
    }
}
