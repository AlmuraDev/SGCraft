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

import com.almuradev.sgcraft.mod.entity.MalpEntity;
import com.almuradev.sgcraft.mod.entity.SGEntityTypes;
import com.almuradev.sgcraft.mod.malp.MalpOptions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A MALP, packed up: used on a block, it puts the MALP down on it, facing the way the player faces. It keeps the
 * MALP's battery in {@link DataComponents#CONTAINER}, the console it is paired with in
 * {@link SGDataComponents#MALP_CONSOLE}, its owner in {@link SGDataComponents#MALP_OWNER}, its settings in
 * {@link SGDataComponents#MALP_OPTIONS}, and its name, so a MALP picked up and put down again is the same MALP. A
 * MALP put down from an item with no owner belongs to the player who put it down. Used on a MALP console, the
 * console pairs it.
 */
public final class MalpItem extends Item {

    public MalpItem(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level level = context.getLevel();
        final ItemStack stack = context.getItemInHand();
        final BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        final MalpEntity malp = SGEntityTypes.MALP.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (malp == null) {
            return InteractionResult.FAIL;
        }
        final Vec3 at = Vec3.atBottomCenterOf(pos);
        malp.snapTo(at.x, at.y, at.z, context.getRotation(), 0);
        if (!level.noCollision(malp, malp.getBoundingBox())) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel serverLevel) {
            EntityType.<MalpEntity>createDefaultStackConfig(serverLevel, stack, context.getPlayer()).apply(malp);
            MalpItem.battery(stack).ifPresent(malp::setBattery);
            malp.setConsole(stack.get(SGDataComponents.MALP_CONSOLE));
            final UUID owner = stack.get(SGDataComponents.MALP_OWNER);
            malp.setOwner(owner != null ? owner : context.getPlayer() != null ? context.getPlayer().getUUID() : null);
            malp.setOptions(stack.getOrDefault(SGDataComponents.MALP_OPTIONS, MalpOptions.DEFAULTS));
            serverLevel.addFreshEntity(malp);
            serverLevel.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, at);
            stack.consume(1, context.getPlayer());
        }
        return InteractionResult.SUCCESS;
    }

    /** @return the battery a packed MALP holds, if it holds one */
    public static Optional<ItemStack> battery(final ItemStack malp) {
        final ItemStack battery = malp.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyOne();
        return battery.isEmpty() ? Optional.empty() : Optional.of(battery);
    }

    /** Packs up a MALP as an item, keeping its battery, its console, its name, its owner and its settings. */
    public static ItemStack of(final MalpEntity malp) {
        final ItemStack stack = MalpItem.withoutBattery(malp);
        final ItemStack battery = malp.battery();
        if (!battery.isEmpty()) {
            stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(battery.copy())));
        }
        return stack;
    }

    /**
     * Packs up a MALP as an item with no battery in it, keeping its console, its name, its owner and its settings,
     * unless they are the defaults.
     */
    public static ItemStack withoutBattery(final MalpEntity malp) {
        final ItemStack stack = new ItemStack(SGItems.MALP);
        if (malp.console() != null) {
            stack.set(SGDataComponents.MALP_CONSOLE, malp.console());
        }
        stack.set(DataComponents.CUSTOM_NAME, malp.getCustomName());
        if (malp.owner() != null) {
            stack.set(SGDataComponents.MALP_OWNER, malp.owner());
        }
        if (!MalpOptions.DEFAULTS.equals(malp.options())) {
            stack.set(SGDataComponents.MALP_OPTIONS, malp.options());
        }
        return stack;
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final TooltipDisplay display,
            final Consumer<Component> builder, final TooltipFlag flag) {
        final GlobalPos console = stack.get(SGDataComponents.MALP_CONSOLE);
        builder.accept((console == null
                ? Component.translatableWithFallback("item.sgcraft.malp.unpaired", "Not paired with a console")
                : Component.translatableWithFallback("item.sgcraft.malp.paired", "Paired with the console at %s",
                        console.pos().toShortString())).withStyle(ChatFormatting.GRAY));
        MalpItem.battery(stack).ifPresentOrElse(
                battery -> builder.accept(Component.translatableWithFallback("item.sgcraft.malp.battery",
                        "Battery: %s%%", BatteryItem.percent(battery)).withStyle(ChatFormatting.GRAY)),
                () -> builder.accept(Component.translatableWithFallback("item.sgcraft.malp.no_battery",
                        "No battery").withStyle(ChatFormatting.GRAY)));
        if (!stack.getOrDefault(SGDataComponents.MALP_OPTIONS, MalpOptions.DEFAULTS).powerRequired()) {
            builder.accept(Component.translatableWithFallback("item.sgcraft.malp.no_power_needed",
                    "Needs no power").withStyle(ChatFormatting.GRAY));
        }
    }
}
