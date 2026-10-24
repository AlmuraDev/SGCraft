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

package com.almuradev.sgcraft.mod.zpm;

import com.almuradev.sgcraft.mod.item.SGDataComponents;
import com.almuradev.sgcraft.mod.item.SGItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * A Zero Point Module, as in the original: a great store of gate energy (GE) that gates, ZPM consoles, interface
 * carts and hubs, and city shields draw on. A ZPM holds the server's {@link #capacity()} when found, and keeps what is
 * left as it is moved about; it is never used up, only emptied. A tainted ZPM, which shines like an enchanted item and
 * never shows its power, explodes when a console, cart or hub draws on it.
 */
public final class ZpmItem extends Item {

    /** @return how much gate energy a full ZPM holds, as the server's settings say */
    public static long capacity() {
        return ZpmSettings.current().capacity();
    }

    public ZpmItem(final Properties properties) {
        super(properties);
    }

    /** @return a full ZPM, tainted or not */
    public static ItemStack full(final boolean tainted) {
        final ItemStack stack = new ItemStack(SGItems.ZPM);
        stack.set(SGDataComponents.ZPM_ENERGY, (double) ZpmItem.capacity());
        if (tainted) {
            ZpmItem.taint(stack);
        }
        return stack;
    }

    /** @return whether a stack is a ZPM */
    public static boolean is(final ItemStack stack) {
        return stack.getItem() instanceof ZpmItem;
    }

    /** @return the gate energy a ZPM holds; one that has never been charged or drawn on is full */
    public static double energy(final ItemStack stack) {
        return ZpmItem.is(stack) ? Math.max(0, Math.min(ZpmItem.capacity(),
                stack.getOrDefault(SGDataComponents.ZPM_ENERGY, (double) ZpmItem.capacity()))) : 0;
    }

    /** Sets the gate energy a ZPM holds, kept between none and a full ZPM's. */
    public static void setEnergy(final ItemStack stack, final double energy) {
        stack.set(SGDataComponents.ZPM_ENERGY, Math.max(0, Math.min((double) ZpmItem.capacity(), energy)));
    }

    /** @return whether a ZPM is tainted */
    public static boolean tainted(final ItemStack stack) {
        return stack.has(SGDataComponents.ZPM_TAINTED);
    }

    /** Taints a ZPM: it shines, and explodes when drawn on in a console, cart or hub. */
    public static void taint(final ItemStack stack) {
        stack.set(SGDataComponents.ZPM_TAINTED, Unit.INSTANCE);
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
    }

    // A tainted ZPM passes for a sound one: it never shows its power, by bar or tooltip
    @Override
    public boolean isBarVisible(final ItemStack stack) {
        return !ZpmItem.tainted(stack) && ZpmItem.energy(stack) < ZpmItem.capacity();
    }

    @Override
    public int getBarWidth(final ItemStack stack) {
        return (int) Math.round(13.0 * ZpmItem.energy(stack) / ZpmItem.capacity());
    }

    @Override
    public int getBarColor(final ItemStack stack) {
        return 0xFFFF9900;
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final TooltipDisplay display,
            final Consumer<Component> builder, final TooltipFlag flag) {
        if (ZpmItem.tainted(stack)) {
            return;
        }
        builder.accept(Component.translatableWithFallback("item.sgcraft.zpm.power", "Power: %s GE",
                NumberFormat.getIntegerInstance(Locale.US).format(Math.floor(ZpmItem.energy(stack))))
                .withStyle(ChatFormatting.GRAY));
    }
}
