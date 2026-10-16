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
import com.almuradev.sgcraft.api.SGCraftRegistries;
import com.almuradev.sgcraft.api.gate.GateState;
import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.gate.GateTargeting;
import com.almuradev.sgcraft.mod.gate.OriGate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
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
 * Turns a gate into another type, as 1.12's Pegasus and Universe upgrades did, used on any of its blocks or while
 * looking at its ring. The item is used up; the gate must be idle, of a size the type suits, and the player must have
 * the right to configure it.
 */
public final class GateTypeUpgradeItem extends Item implements GateItem {

    private final ResourceKey<GateType> type;

    public GateTypeUpgradeItem(final Properties properties, final ResourceKey<GateType> type) {
        super(properties);
        this.type = type;
    }

    /** @return the type this upgrade turns a gate into */
    public ResourceKey<GateType> type() {
        return this.type;
    }

    /** @return the upgrade that turns a gate into a type, if there is one */
    public static Optional<GateTypeUpgradeItem> forType(final ResourceKey<GateType> type) {
        for (final Item item : new Item[] {SGItems.PEGASUS_UPGRADE, SGItems.UNIVERSE_UPGRADE}) {
            if (item instanceof final GateTypeUpgradeItem upgrade && upgrade.type.equals(type)) {
                return Optional.of(upgrade);
            }
        }
        return Optional.empty();
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        if (!(context.getPlayer() instanceof final ServerPlayer player)) {
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
        if (!(player instanceof final ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        return GateTargeting.lookedAt(player, 6)
                .map(gate -> this.fit(serverPlayer, gate, player.getItemInHand(hand)))
                .orElse(InteractionResult.PASS);
    }

    private InteractionResult fit(final ServerPlayer player, final Stargate gate, final ItemStack stack) {
        final Optional<Holder.Reference<GateType>> type = player.level().registryAccess()
                .lookupOrThrow(SGCraftRegistries.GATE_TYPE).get(this.type);
        if (type.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (gate.type().unwrapKey().map(OriGate::is).orElse(false)) {
            player.sendOverlayMessage(Component.translatableWithFallback("message.sgcraft.ori_no_upgrades",
                    "An Ori supergate takes no upgrades").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        final Component name = type.get().value().name();
        final ActionResult result = gate.setType(type.get(), Cause.of(stack.copy(), player));
        final Component message = switch (result) {
            case DONE -> Component.translatableWithFallback("message.sgcraft.type_fitted", "The gate is now a %s gate",
                    name);
            case NO_CHANGE -> Component.translatableWithFallback("message.sgcraft.type_same",
                    "This gate is already a %s gate", name).withStyle(ChatFormatting.RED);
            case DENIED -> Component.translatableWithFallback("message.sgcraft.type_denied",
                    "You aren't allowed to change this gate").withStyle(ChatFormatting.RED);
            default -> gate.state() != GateState.IDLE
                    ? Component.translatableWithFallback("message.sgcraft.type_busy",
                            "You can't change this gate while it's in use").withStyle(ChatFormatting.RED)
                    : Component.translatableWithFallback("message.sgcraft.type_unsuited",
                            "A %s gate can't be this size", name).withStyle(ChatFormatting.RED);
        };
        player.sendOverlayMessage(message);
        if (result != ActionResult.DONE) {
            return InteractionResult.FAIL;
        }
        stack.consume(1, player);
        return InteractionResult.SUCCESS_SERVER;
    }
}
