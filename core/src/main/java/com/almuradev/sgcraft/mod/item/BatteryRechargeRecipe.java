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
import com.mojang.serialization.MapCodec;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * A battery and naquadah in the crafting grid, in any arrangement: the naquadah is used up and the battery comes out
 * holding the gate energy it gave, as much as it has room for. A full battery isn't topped up, so no naquadah is wasted
 * on it.
 */
public final class BatteryRechargeRecipe extends CustomRecipe {

    public static final BatteryRechargeRecipe INSTANCE = new BatteryRechargeRecipe();
    public static final RecipeSerializer<BatteryRechargeRecipe> SERIALIZER = new RecipeSerializer<>(
            MapCodec.unit(BatteryRechargeRecipe.INSTANCE), StreamCodec.unit(BatteryRechargeRecipe.INSTANCE));

    private BatteryRechargeRecipe() {
    }

    @Override
    public boolean matches(final CraftingInput input, final Level level) {
        final ItemStack battery = BatteryRechargeRecipe.battery(input);
        return !battery.isEmpty() && BatteryRechargeRecipe.naquadah(input) > 0
                && BatteryItem.energy(battery) < BatteryItem.capacity();
    }

    @Override
    public ItemStack assemble(final CraftingInput input) {
        final ItemStack battery = BatteryRechargeRecipe.battery(input);
        final int naquadah = BatteryRechargeRecipe.naquadah(input);
        if (battery.isEmpty() || naquadah <= 0) {
            return ItemStack.EMPTY;
        }
        final ItemStack charged = battery.copyWithCount(1);
        BatteryItem.insert(charged, naquadah * MalpSettings.naquadahEnergy(), false);
        return charged;
    }

    // The one battery in the grid, or nothing if there is none, more than one, or anything but naquadah beside it
    private static ItemStack battery(final CraftingInput input) {
        ItemStack battery = ItemStack.EMPTY;
        for (int slot = 0; slot < input.size(); slot++) {
            final ItemStack stack = input.getItem(slot);
            if (stack.is(SGItems.BATTERY)) {
                if (!battery.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                battery = stack;
            } else if (!stack.isEmpty() && !stack.is(SGItems.NAQUADAH)) {
                return ItemStack.EMPTY;
            }
        }
        return battery;
    }

    private static int naquadah(final CraftingInput input) {
        int count = 0;
        for (int slot = 0; slot < input.size(); slot++) {
            if (input.getItem(slot).is(SGItems.NAQUADAH)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public RecipeSerializer<BatteryRechargeRecipe> getSerializer() {
        return BatteryRechargeRecipe.SERIALIZER;
    }
}
