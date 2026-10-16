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
import com.almuradev.sgcraft.mod.malp.MalpConfigurator;
import com.almuradev.sgcraft.mod.malp.MalpLinks;
import com.almuradev.sgcraft.mod.malp.MalpMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A MALP tablet: a MALP console in the hand. Used in the air, it opens the same feed screen a console does, listing
 * the MALPs paired with the tablet; used in the air while sneaking, it opens its battery slot. Used on a MALP while
 * sneaking, by the MALP's owner or an operator, it pairs that MALP with the tablet, and the MALP stays paired with its
 * console too. Any number of tablets can be paired with one MALP.
 *
 * <p>The tablet keeps the MALPs it is paired with in {@link SGDataComponents#TABLET_MALPS} and its battery in
 * {@link DataComponents#CONTAINER}. While its screen shows a live feed it uses gate energy from that battery, as much
 * each tick as the server's settings say; with a flat battery or none it shows no feed.</p>
 */
public final class MalpTabletItem extends Item {

    public MalpTabletItem(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            final ItemStack tablet = player.getItemInHand(hand);
            if (player.isSecondaryUseActive()) {
                MalpLinks.close(serverPlayer);
                serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> new MalpMenu(id, inventory, tablet,
                        hand), tablet.getHoverName()));
            } else {
                MalpLinks.openTablet(serverPlayer, hand);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * A player used a tablet on a MALP while sneaking: the MALP is paired with the tablet if the player may configure
     * the MALP, and the player is told how it went.
     */
    public static void pairOnUse(final ServerPlayer player, final ItemStack tablet, final MalpEntity malp) {
        if (!MalpConfigurator.mayConfigure(player, malp)) {
            player.sendOverlayMessage(Component.translatableWithFallback("message.sgcraft.malp.cannot_pair_tablet",
                    "You aren't allowed to pair this MALP").withStyle(ChatFormatting.RED));
            return;
        }
        if (MalpTabletItem.pair(tablet, malp.getUUID())) {
            player.sendOverlayMessage(Component.translatableWithFallback("message.sgcraft.malp.tablet_paired",
                    "%s is paired with this tablet", malp.displayName()).withStyle(ChatFormatting.GREEN));
        } else {
            player.sendOverlayMessage(Component.translatableWithFallback("message.sgcraft.malp.tablet_already_paired",
                    "%s is already paired with this tablet", malp.displayName()).withStyle(ChatFormatting.GRAY));
        }
    }

    /** @return the MALPs a tablet is paired with, by UUID, in the order they were paired */
    public static List<UUID> malps(final ItemStack tablet) {
        return tablet.getOrDefault(SGDataComponents.TABLET_MALPS, List.of());
    }

    /**
     * Pairs a MALP with a tablet, after those it is paired with already.
     *
     * @return whether the MALP wasn't paired with the tablet before
     */
    public static boolean pair(final ItemStack tablet, final UUID malp) {
        final List<UUID> malps = MalpTabletItem.malps(tablet);
        if (malps.contains(malp)) {
            return false;
        }
        final List<UUID> more = new ArrayList<>(malps);
        more.add(malp);
        tablet.set(SGDataComponents.TABLET_MALPS, List.copyOf(more));
        return true;
    }

    /**
     * Takes a MALP off a tablet's list.
     *
     * @return whether the MALP was paired with the tablet
     */
    public static boolean unpair(final ItemStack tablet, final UUID malp) {
        final List<UUID> malps = MalpTabletItem.malps(tablet);
        if (!malps.contains(malp)) {
            return false;
        }
        final List<UUID> fewer = malps.stream().filter(id -> !id.equals(malp)).toList();
        if (fewer.isEmpty()) {
            tablet.remove(SGDataComponents.TABLET_MALPS);
        } else {
            tablet.set(SGDataComponents.TABLET_MALPS, fewer);
        }
        return true;
    }

    /** @return a copy of the battery in a tablet, or an empty stack if it has none */
    public static ItemStack battery(final ItemStack tablet) {
        return tablet.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyOne();
    }

    /** Puts a battery in a tablet, in place of any it had; an empty stack leaves it with none. */
    public static void setBattery(final ItemStack tablet, final ItemStack battery) {
        if (battery.isEmpty()) {
            tablet.remove(DataComponents.CONTAINER);
        } else {
            tablet.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(battery.copyWithCount(1))));
        }
    }

    /** @return the gate energy in a tablet's battery, or 0 if it has none */
    public static double energy(final ItemStack tablet) {
        final ItemStack battery = MalpTabletItem.battery(tablet);
        return battery.isEmpty() ? 0 : BatteryItem.energy(battery);
    }

    /**
     * Uses gate energy from a tablet's battery, as much as the battery holds.
     *
     * @return how much it used
     */
    public static double drain(final ItemStack tablet, final double amount) {
        final ItemStack battery = MalpTabletItem.battery(tablet);
        final double used = BatteryItem.extract(battery, amount, false);
        if (used > 0) {
            MalpTabletItem.setBattery(tablet, battery);
        }
        return used;
    }

    /** @return how full a tablet's battery is, from 0 to 100, or -1 if it has none */
    public static int percent(final ItemStack tablet) {
        final ItemStack battery = MalpTabletItem.battery(tablet);
        return battery.isEmpty() ? -1 : BatteryItem.percent(battery);
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final TooltipDisplay display,
            final Consumer<Component> builder, final TooltipFlag flag) {
        builder.accept(Component.translatableWithFallback("item.sgcraft.malp_tablet.malps", "Paired MALPs: %s",
                MalpTabletItem.malps(stack).size()).withStyle(ChatFormatting.GRAY));
        final int percent = MalpTabletItem.percent(stack);
        builder.accept((percent < 0
                ? Component.translatableWithFallback("item.sgcraft.malp.no_battery", "No battery")
                : Component.translatableWithFallback("item.sgcraft.malp.battery", "Battery: %s%%", percent))
                .withStyle(ChatFormatting.GRAY));
    }
}
