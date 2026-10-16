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

package com.almuradev.sgcraft.mod.item;

import com.almuradev.sgcraft.api.ActionResult;
import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.IrisState;
import com.almuradev.sgcraft.api.gate.IrisType;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.gate.GateTargeting;
import com.almuradev.sgcraft.mod.gate.SGIris;
import com.almuradev.sgcraft.mod.gate.SGStargate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Fits an iris to a gate, used on any of its blocks or while looking at its ring, as in the original: the metal iris,
 * or a shield in its place. The item is used up. A gate with the other kind has it replaced, once it is open, and
 * that one's upgrade comes back unless the gate came with it. The configurator takes an iris off again and gives the
 * upgrade back.
 */
public final class IrisUpgradeItem extends Item implements GateItem {

    private final ResourceKey<IrisType> type;

    /**
     * @param type the kind of iris the item fits
     */
    public IrisUpgradeItem(final Properties properties, final ResourceKey<IrisType> type) {
        super(properties);
        this.type = type;
    }

    /** @return the upgrade that fits that kind of iris, if there is one */
    public static Optional<IrisUpgradeItem> forType(final ResourceKey<IrisType> type) {
        for (final Item item : new Item[] {SGItems.IRIS_UPGRADE, SGItems.SHIELD_UPGRADE}) {
            if (item instanceof final IrisUpgradeItem upgrade && upgrade.type.identifier().equals(type.identifier())) {
                return Optional.of(upgrade);
            }
        }
        return Optional.empty();
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
        return this.fit(player, gate.get(), context.getItemInHand());
    }

    @Override
    public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        return GateTargeting.lookedAt(player, 6)
                .map(gate -> this.fit(serverPlayer, gate, player.getItemInHand(hand)))
                .orElse(InteractionResult.PASS);
    }

    private InteractionResult fit(final ServerPlayer player, final Stargate gate, final ItemStack stack) {
        final String kind = SGIris.isShield(this.type) ? "shield" : "iris";
        final Optional<ResourceKey<IrisType>> fitted = gate.get(GateProperties.IRIS);
        if (fitted.isPresent() && fitted.get().identifier().equals(this.type.identifier())) {
            return IrisUpgradeItem.refuse(player, "message.sgcraft." + kind + "_fitted_already",
                    SGIris.isShield(this.type) ? "This gate already has a shield" : "This gate already has an iris");
        }
        if (!gate.type().value().acceptsIris(this.type)) {
            return IrisUpgradeItem.refuse(player, "message.sgcraft.iris_not_accepted",
                    "This type of gate takes no iris or shield");
        }
        // One kind replaces the other only once it is out of the way, open or lowered
        if (fitted.isPresent() && gate.iris().map(iris -> iris.state() != IrisState.OPEN).orElse(false)) {
            return IrisUpgradeItem.refuse(player, "message.sgcraft.open_before_replacing",
                    "Open the iris or lower the shield before replacing it");
        }
        final boolean included = gate instanceof final SGStargate built && built.irisIncluded();
        final ActionResult result = gate.set(GateProperties.IRIS, this.type, Cause.of(stack.copy(), player));
        if (result != ActionResult.DONE) {
            return IrisUpgradeItem.refuse(player, "message.sgcraft." + kind + "_not_fitted",
                    SGIris.isShield(this.type) ? "You can't fit a shield to this gate"
                            : "You can't fit an iris to this gate");
        }
        stack.consume(1, player);
        if (fitted.isPresent() && !included) {
            IrisUpgradeItem.forType(fitted.get()).ifPresent(upgrade -> player.getInventory()
                    .placeItemBackInInventory(new ItemStack(upgrade), Prediction.SERVER_ONLY));
        }
        player.sendOverlayMessage(Component.translatableWithFallback("message.sgcraft." + kind + "_fitted",
                SGIris.isShield(this.type) ? "Shield fitted" : "Iris fitted"));
        return InteractionResult.SUCCESS_SERVER;
    }

    private static InteractionResult refuse(final ServerPlayer player, final String key, final String fallback) {
        player.sendOverlayMessage(Component.translatableWithFallback(key, fallback).withStyle(ChatFormatting.RED));
        return InteractionResult.FAIL;
    }
}
