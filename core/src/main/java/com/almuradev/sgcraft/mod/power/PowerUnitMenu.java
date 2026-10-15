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
 * A power unit's charge and capacity, in FE, and its battery slot over the player's inventory, for its screen. The
 * game sends a menu's numbers sixteen bits at a time, so each is sent as {@link #PARTS} parts, lowest first: the
 * charge's at index 0, the capacity's from {@link #CAPACITY}; the battery's charge, in percent, follows at
 * {@link #BATTERY}.
 */
public final class PowerUnitMenu extends AbstractContainerMenu {

    /** How many sixteen-bit parts each number is sent as, enough for any charge a unit could hold. */
    public static final int PARTS = 3;
    /** The index the capacity's parts start at. */
    public static final int CAPACITY = PowerUnitMenu.PARTS;
    /** The index of the battery's charge, in percent, or -1 for no battery. */
    public static final int BATTERY = 2 * PowerUnitMenu.PARTS;
    /** Where the battery's slot is on the screen. */
    public static final int BATTERY_X = 80;
    public static final int BATTERY_Y = 72;
    /** Where the player's inventory starts on the screen. */
    public static final int INVENTORY_Y = 98;

    /** The menu's type; set when the loader registers menus. */
    public static MenuType<PowerUnitMenu> TYPE;

    private final Container battery;
    private final ContainerData data;

    /** The client's menu, which the server fills. */
    public PowerUnitMenu(final int id, final Inventory inventory) {
        this(id, inventory, new SimpleContainer(1), new SimpleContainerData(PowerUnitMenu.BATTERY + 1));
    }

    public PowerUnitMenu(final int id, final Inventory inventory, final Container battery,
            final ContainerData data) {
        super(PowerUnitMenu.TYPE, id);
        this.battery = battery;
        this.data = data;
        this.addSlot(new Slot(battery, 0, PowerUnitMenu.BATTERY_X, PowerUnitMenu.BATTERY_Y) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return stack.is(SGItems.BATTERY);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        this.addStandardInventorySlots(inventory, 8, PowerUnitMenu.INVENTORY_Y);
        this.addDataSlots(data);
    }

    /** @return the battery's charge, in percent, or -1 for no battery */
    public int batteryPercent() {
        return this.data.get(PowerUnitMenu.BATTERY);
    }

    /** @return one sixteen-bit part of a number, as the menu sends it */
    public static int part(final long value, final int part) {
        return (int) (value >>> (16 * part) & 0xFFFF);
    }

    /** @return the FE the unit holds */
    public long stored() {
        return this.read(0);
    }

    /** @return the most FE the unit holds */
    public long capacity() {
        return this.read(PowerUnitMenu.CAPACITY);
    }

    private long read(final int from) {
        long value = 0;
        for (int part = 0; part < PowerUnitMenu.PARTS; part++) {
            value |= (long) (this.data.get(from + part) & 0xFFFF) << (16 * part);
        }
        return value;
    }

    @Override
    public boolean stillValid(final Player player) {
        return this.battery.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        final Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        final ItemStack moved = stack.copy();
        if (index == 0) {
            if (!this.moveItemStackTo(stack, 1, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!stack.is(SGItems.BATTERY) || !this.moveItemStackTo(stack, 0, 1, false)) {
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
