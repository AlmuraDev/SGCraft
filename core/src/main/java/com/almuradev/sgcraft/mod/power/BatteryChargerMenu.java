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

package com.almuradev.sgcraft.mod.power;

import com.almuradev.sgcraft.mod.block.entity.BatteryChargerBlockEntity;
import com.almuradev.sgcraft.mod.item.SGItems;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A battery charger's bay and naquadah slot over the player's inventory, with how full its store and its battery
 * are.
 */
public final class BatteryChargerMenu extends AbstractContainerMenu {

    /** {@link #data()} index of how full the charger's store is, in percent. */
    public static final int STORE_PERCENT = 0;
    /** {@link #data()} index of how full the battery is, in percent, or -1 for no battery. */
    public static final int BATTERY_PERCENT = 1;
    /** Where the battery's slot is on the screen. */
    public static final int BATTERY_X = 116;
    public static final int BATTERY_Y = 35;
    /** Where the naquadah's slot is on the screen. */
    public static final int FUEL_X = 44;
    public static final int FUEL_Y = 35;

    /** The menu's type; set when the loader registers menus. */
    public static MenuType<BatteryChargerMenu> TYPE;

    private final Container items;
    private final ContainerData data;

    /** The client's menu, which the server fills. */
    public BatteryChargerMenu(final int id, final Inventory inventory) {
        this(id, inventory, new SimpleContainer(2), new SimpleContainerData(2));
    }

    public BatteryChargerMenu(final int id, final Inventory inventory, final Container items,
            final ContainerData data) {
        super(BatteryChargerMenu.TYPE, id);
        AbstractContainerMenu.checkContainerSize(items, 2);
        this.items = items;
        this.data = data;
        items.startOpen(inventory.player);
        this.addSlot(new Slot(items, BatteryChargerBlockEntity.BATTERY, BatteryChargerMenu.BATTERY_X,
                BatteryChargerMenu.BATTERY_Y) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return stack.is(SGItems.BATTERY);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        this.addSlot(new Slot(items, BatteryChargerBlockEntity.FUEL, BatteryChargerMenu.FUEL_X,
                BatteryChargerMenu.FUEL_Y) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return stack.is(SGItems.NAQUADAH);
            }
        });
        this.addStandardInventorySlots(inventory, 8, 84);
        this.addDataSlots(data);
    }

    /** @return what the menu shows besides the slots; see {@link #STORE_PERCENT} and {@link #BATTERY_PERCENT} */
    public ContainerData data() {
        return this.data;
    }

    @Override
    public boolean stillValid(final Player player) {
        return this.items.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        final Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        final ItemStack moved = stack.copy();
        if (index < 2) {
            if (!this.moveItemStackTo(stack, 2, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(SGItems.BATTERY)) {
            if (!this.moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!stack.is(SGItems.NAQUADAH) || !this.moveItemStackTo(stack, 1, 2, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moved;
    }

    @Override
    public void removed(final Player player) {
        super.removed(player);
        this.items.stopOpen(player);
    }
}
