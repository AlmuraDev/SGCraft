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

import com.almuradev.sgcraft.api.SGCraftApi;
import com.almuradev.sgcraft.api.access.GateRight;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.block.entity.StargateBaseBlockEntity;
import com.almuradev.sgcraft.mod.network.SGNetwork;
import com.almuradev.sgcraft.mod.tool.ToolActions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Opens a gate's base screen, as using the base or any block of a formed gate's ring does in the original. A player
 * who may configure a standing gate gets its camouflage too; anyone else, and anyone at a gate lying flat, sees only
 * its address.
 */
public final class GateBaseActions {

    private GateBaseActions() {
    }

    /** A player used a block of a formed gate at {@code pos}. */
    public static void open(final ServerPlayer player, final BlockPos pos) {
        final Optional<Stargate> gate = SGCraftApi.get().stargates().at(GlobalPos.of(player.level().dimension(), pos));
        if (gate.isEmpty()
                || !(player.level().getBlockEntity(gate.get().position().pos())
                        instanceof StargateBaseBlockEntity base)) {
            return;
        }
        int menu = GateBaseInfo.NO_MENU;
        // A gate lying flat has no bottom row to camouflage, as in the original
        if (!base.orientation().isFlat()
                && gate.get().access().allows(ToolActions.cause(player), GateRight.CONFIGURE)) {
            final OptionalInt opened = player.openMenu(new SimpleMenuProvider((id, inventory, opener) ->
                    new GateBaseMenu(id, inventory, base.camouflage()),
                    Component.translatableWithFallback("screen.sgcraft.gate_base", "Stargate Address")));
            if (opened.isPresent()) {
                menu = opened.getAsInt();
            }
        }
        SGNetwork.send(player, new GateBaseInfo(menu, ToolActions.info(player, gate.get())));
    }
}
