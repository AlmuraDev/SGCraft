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

import com.almuradev.sgcraft.mod.malp.MalpSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * A battery: a store of FE that powers a MALP. Its charge is kept on the stack, in {@link SGDataComponents#ENERGY},
 * and its capacity is the server's. Power units, battery chargers, naquadah and other mods' chargers fill it.
 */
public final class BatteryItem extends Item {

    private static final int BAR_WIDTH = 13;
    private static final int BAR_COLOR = 0x3FD07F;

    public BatteryItem(final Properties properties) {
        super(properties);
    }

    /** @return the most FE a battery holds, as the server's settings say */
    public static long capacity() {
        return MalpSettings.current().batteryCapacity();
    }

    /** @return the FE a battery holds */
    public static long energy(final DataComponentGetter battery) {
        return Math.max(0, Math.min(battery.getOrDefault(SGDataComponents.ENERGY, 0L), BatteryItem.capacity()));
    }

    /** @return a new battery holding as much FE as a battery holds */
    public static ItemStack full() {
        final ItemStack battery = new ItemStack(SGItems.BATTERY);
        BatteryItem.setEnergy(battery, BatteryItem.capacity());
        return battery;
    }

    /** Sets the FE a battery holds, kept between none and its capacity. */
    public static void setEnergy(final ItemStack battery, final long energy) {
        battery.set(SGDataComponents.ENERGY, Math.max(0, Math.min(energy, BatteryItem.capacity())));
    }

    /**
     * Puts FE into a battery, as much as there is room for.
     *
     * @return how much it took, or would take
     */
    public static long insert(final ItemStack battery, final long amount, final boolean simulate) {
        if (!battery.is(SGItems.BATTERY)) {
            return 0;
        }
        final long stored = BatteryItem.energy(battery);
        final long taken = Math.max(0, Math.min(amount, BatteryItem.capacity() - stored));
        if (!simulate && taken > 0) {
            BatteryItem.setEnergy(battery, stored + taken);
        }
        return taken;
    }

    /**
     * Takes FE out of a battery, as much as it holds.
     *
     * @return how much it gave, or would give
     */
    public static long extract(final ItemStack battery, final long amount, final boolean simulate) {
        if (!battery.is(SGItems.BATTERY)) {
            return 0;
        }
        final long stored = BatteryItem.energy(battery);
        final long given = Math.max(0, Math.min(amount, stored));
        if (!simulate && given > 0) {
            BatteryItem.setEnergy(battery, stored - given);
        }
        return given;
    }

    /** @return how full a battery is, from 0 to 100 */
    public static int percent(final DataComponentGetter battery) {
        final long capacity = BatteryItem.capacity();
        return capacity <= 0 ? 0 : (int) Math.round(100.0 * BatteryItem.energy(battery) / capacity);
    }

    @Override
    public boolean isBarVisible(final ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(final ItemStack stack) {
        final long capacity = BatteryItem.capacity();
        return capacity <= 0 ? 0 : Mth.clamp(Math.round((float) BatteryItem.BAR_WIDTH * BatteryItem.energy(stack)
                / capacity), 0, BatteryItem.BAR_WIDTH);
    }

    @Override
    public int getBarColor(final ItemStack stack) {
        return BatteryItem.BAR_COLOR;
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final TooltipDisplay display,
            final Consumer<Component> builder, final TooltipFlag flag) {
        final NumberFormat format = NumberFormat.getIntegerInstance(Locale.US);
        builder.accept(Component.translatableWithFallback("item.sgcraft.battery.charge", "%s / %s FE (%s%%)",
                format.format(BatteryItem.energy(stack)), format.format(BatteryItem.capacity()),
                BatteryItem.percent(stack)).withStyle(ChatFormatting.GRAY));
    }
}
