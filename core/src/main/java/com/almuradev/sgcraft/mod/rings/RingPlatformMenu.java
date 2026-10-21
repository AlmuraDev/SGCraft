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

package com.almuradev.sgcraft.mod.rings;

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
 * A ring platform's screen: its four naquadah slots over the player's inventory, the gate energy it holds, and how
 * many transports it has left. The game sends a menu's numbers sixteen bits at a time, so each is sent as
 * {@link #PARTS} parts: its store, its capacity and its transports left, then whether it is linked.
 */
public final class RingPlatformMenu extends AbstractContainerMenu {

    /** How many sixteen-bit parts each number is sent as. */
    public static final int PARTS = 3;
    /** How many numbers the menu holds. */
    public static final int DATA = 3 * RingPlatformMenu.PARTS + 1;
    /** Where the fuel slots are on the screen: a square of two by two. */
    public static final int FUEL_X = 26;
    public static final int FUEL_Y = 25;
    /** Where the player's inventory starts on the screen. */
    public static final int INVENTORY_Y = 84;

    /** The menu's type; set when the loader registers menus. */
    public static MenuType<RingPlatformMenu> TYPE;

    private final Container fuel;
    private final ContainerData data;

    /** The client's menu, which the server fills. */
    public RingPlatformMenu(final int id, final Inventory inventory) {
        this(id, inventory, new SimpleContainer(RingPlatformBlockEntity.SLOTS),
                new SimpleContainerData(RingPlatformMenu.DATA));
    }

    public RingPlatformMenu(final int id, final Inventory inventory, final Container fuel, final ContainerData data) {
        super(RingPlatformMenu.TYPE, id);
        this.fuel = fuel;
        this.data = data;
        for (int slot = 0; slot < RingPlatformBlockEntity.SLOTS; slot++) {
            this.addSlot(new Slot(fuel, slot, RingPlatformMenu.FUEL_X + (slot % 2) * 18,
                    RingPlatformMenu.FUEL_Y + (slot / 2) * 18) {
                @Override
                public boolean mayPlace(final ItemStack stack) {
                    return RingPlatformBlockEntity.isFuel(stack);
                }
            });
        }
        this.addStandardInventorySlots(inventory, 8, RingPlatformMenu.INVENTORY_Y);
        this.addDataSlots(data);
    }

    /** @return one sixteen-bit part of a number, by the index of the part in the menu's data */
    public static int part(final long value, final int index) {
        return (int) (Math.min(value, (1L << (16 * RingPlatformMenu.PARTS)) - 1)
                >>> (16 * (index % RingPlatformMenu.PARTS)) & 0xFFFF);
    }

    private long read(final int number) {
        long value = 0;
        for (int part = 0; part < RingPlatformMenu.PARTS; part++) {
            value |= (long) (this.data.get(number * RingPlatformMenu.PARTS + part) & 0xFFFF) << (16 * part);
        }
        return value;
    }

    /** @return the gate energy it holds */
    public long stored() {
        return this.read(0);
    }

    /** @return the most it holds from cables */
    public long capacity() {
        return this.read(1);
    }

    /** @return how many transports to its partner it has left, counting its naquadah */
    public long usesLeft() {
        return this.read(2);
    }

    /** @return whether it is linked to another platform */
    public boolean linked() {
        return this.data.get(3 * RingPlatformMenu.PARTS) != 0;
    }

    @Override
    public boolean stillValid(final Player player) {
        return this.fuel.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        final Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        final ItemStack moved = stack.copy();
        final int slots = RingPlatformBlockEntity.SLOTS;
        if (index < slots) {
            if (!this.moveItemStackTo(stack, slots, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!RingPlatformBlockEntity.isFuel(stack) || !this.moveItemStackTo(stack, 0, slots, false)) {
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
