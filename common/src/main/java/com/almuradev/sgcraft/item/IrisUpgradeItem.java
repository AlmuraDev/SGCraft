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

package com.almuradev.sgcraft.item;

import com.almuradev.sgcraft.api.ActionResult;
import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.IrisTypes;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.GateTargeting;
import com.almuradev.sgcraft.mod.SGServer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Fits a metal iris to a gate, used on any of its blocks or while looking at its ring, as in the original. The item is
 * used up. The configurator takes an iris off again and gives the upgrade back.
 */
public final class IrisUpgradeItem extends Item implements GateItem {

    public IrisUpgradeItem(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }
        final Optional<Stargate> gate = SGServer.current().flatMap(server -> server.stargates()
                .at(GlobalPos.of(context.getLevel().dimension(), context.getClickedPos())));
        if (gate.isEmpty()) {
            return this.use(context.getLevel(), player, context.getHand());
        }
        return IrisUpgradeItem.fit(player, gate.get(), context.getItemInHand());
    }

    @Override
    public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        return GateTargeting.lookedAt(player, 6)
                .map(gate -> IrisUpgradeItem.fit(serverPlayer, gate, player.getItemInHand(hand)))
                .orElse(InteractionResult.PASS);
    }

    private static InteractionResult fit(final ServerPlayer player, final Stargate gate, final ItemStack stack) {
        if (gate.get(GateProperties.IRIS).isPresent()) {
            player.sendOverlayMessage(Component.translatableWithFallback("message.sgcraft.iris_fitted_already",
                    "This gate already has an iris").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        final ActionResult result = gate.set(GateProperties.IRIS, IrisTypes.METAL, Cause.of(stack.copy(), player));
        if (result != ActionResult.DONE) {
            player.sendOverlayMessage(Component.translatableWithFallback("message.sgcraft.iris_not_fitted",
                    "You can't fit an iris to this gate").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        stack.consume(1, player);
        player.sendOverlayMessage(Component.translatableWithFallback("message.sgcraft.iris_fitted",
                "Iris fitted"));
        return InteractionResult.SUCCESS_SERVER;
    }
}
