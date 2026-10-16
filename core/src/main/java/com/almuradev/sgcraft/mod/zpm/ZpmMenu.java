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
 * A ZPM console's, interface cart's or hub's slots, laid out as the original's screens: the ZPM slots in a row by the
 * picture of a ZPM, and the player's inventory below. The device's energy is sent alongside.
 */
public final class ZpmMenu extends AbstractContainerMenu {

    /** How many sixteen-bit parts the energy is sent as, enough for three full ZPMs. */
    public static final int PARTS = 3;
    /** The index of how many of the device's ZPMs hold energy. */
    public static final int LOADED = ZpmMenu.PARTS;
    /** How many numbers the menu sends. */
    public static final int DATA = ZpmMenu.LOADED + 1;
    /** Where the first ZPM slot is, and how far apart the slots are. */
    public static final int SLOT_X = 120;
    public static final int SLOT_Y = 54;
    public static final int SLOT_STEP = 19;
    /** Where the player's inventory starts. */
    public static final int INVENTORY_X = 48;
    public static final int INVENTORY_Y = 124;

    /** The menu types, for a device with one ZPM slot and for a hub; set when the loader registers menus. */
    public static MenuType<ZpmMenu> SINGLE;
    public static MenuType<ZpmMenu> HUB;

    private final Container zpms;
    private final ContainerData data;

    /** The client's menu for a console or cart, which the server fills. */
    public static ZpmMenu single(final int id, final Inventory inventory) {
        return new ZpmMenu(id, inventory, new SimpleContainer(1), new SimpleContainerData(ZpmMenu.DATA));
    }

    /** The client's menu for a hub, which the server fills. */
    public static ZpmMenu hub(final int id, final Inventory inventory) {
        return new ZpmMenu(id, inventory, new SimpleContainer(ZpmDevice.HUB.slots()),
                new SimpleContainerData(ZpmMenu.DATA));
    }

    /** A device's menu, of the type its number of slots calls for. */
    public ZpmMenu(final int id, final Inventory inventory, final Container zpms, final ContainerData data) {
        super(zpms.getContainerSize() > 1 ? ZpmMenu.HUB : ZpmMenu.SINGLE, id);
        this.zpms = zpms;
        this.data = data;
        for (int slot = 0; slot < zpms.getContainerSize(); slot++) {
            this.addSlot(new Slot(zpms, slot, ZpmMenu.SLOT_X + slot * ZpmMenu.SLOT_STEP, ZpmMenu.SLOT_Y) {
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
        this.addStandardInventorySlots(inventory, ZpmMenu.INVENTORY_X, ZpmMenu.INVENTORY_Y);
        this.addDataSlots(data);
    }

    /** @return how many ZPM slots the device has */
    public int zpmSlots() {
        return this.zpms.getContainerSize();
    }

    /** @return how much gate energy the device's ZPMs hold */
    public long stored() {
        long value = 0;
        for (int part = 0; part < ZpmMenu.PARTS; part++) {
            value |= (long) (this.data.get(part) & 0xFFFF) << (16 * part);
        }
        return value;
    }

    /** @return how many of the device's ZPMs hold energy */
    public int loaded() {
        return this.data.get(ZpmMenu.LOADED);
    }

    @Override
    public boolean stillValid(final Player player) {
        return this.zpms.stillValid(player);
    }

    // A ZPM moves into the first free ZPM slot, and out of them into the inventory
    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        final Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        final ItemStack moved = stack.copy();
        final int own = this.zpms.getContainerSize();
        if (index < own) {
            if (!this.moveItemStackTo(stack, own, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!ZpmItem.is(stack) || !this.moveItemStackTo(stack, 0, own, false)) {
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
