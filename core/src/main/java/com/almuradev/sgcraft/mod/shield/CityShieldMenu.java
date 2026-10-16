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

package com.almuradev.sgcraft.mod.shield;

import com.almuradev.sgcraft.mod.zpm.ZpmItem;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A city shield generator's ZPM slots, as the original's, above the player's inventory. The shield's size and state
 * come to the screen beside the menu, as {@link ShieldPayloads.CityState}.
 */
public final class CityShieldMenu extends AbstractContainerMenu {

    /** How many ZPMs a generator holds. */
    public static final int SLOTS = 3;
    /** Where the ZPM slots and the player's inventory are on the screen. */
    public static final int SLOTS_X = 103;
    public static final int SLOTS_Y = 184;
    public static final int INVENTORY_X = 49;
    public static final int INVENTORY_Y = 212;

    /** The menu's type; set when the loader registers menus. */
    public static MenuType<CityShieldMenu> TYPE;

    private final Container zpms;

    /** The client's menu, which the server fills. */
    public CityShieldMenu(final int id, final Inventory inventory) {
        this(id, inventory, new SimpleContainer(CityShieldMenu.SLOTS));
    }

    public CityShieldMenu(final int id, final Inventory inventory, final Container zpms) {
        super(CityShieldMenu.TYPE, id);
        AbstractContainerMenu.checkContainerSize(zpms, CityShieldMenu.SLOTS);
        this.zpms = zpms;
        for (int slot = 0; slot < CityShieldMenu.SLOTS; slot++) {
            this.addSlot(new Slot(zpms, slot, CityShieldMenu.SLOTS_X + slot * 18, CityShieldMenu.SLOTS_Y) {
                @Override
                public boolean mayPlace(final ItemStack stack) {
                    return ZpmItem.is(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        this.addStandardInventorySlots(inventory, CityShieldMenu.INVENTORY_X, CityShieldMenu.INVENTORY_Y);
    }

    @Override
    public boolean stillValid(final Player player) {
        return this.zpms.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        final Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        final ItemStack moved = stack.copy();
        if (index < CityShieldMenu.SLOTS) {
            if (!this.moveItemStackTo(stack, CityShieldMenu.SLOTS, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!ZpmItem.is(stack) || !this.moveItemStackTo(stack, 0, CityShieldMenu.SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moved;
    }
}
