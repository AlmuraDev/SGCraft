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

package com.almuradev.sgcraft.dhd;

import com.almuradev.sgcraft.item.SGItems;
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
 * A DHD's fuel: five slots that take only naquadah, laid out as a hopper's, above the player's inventory. It also
 * shows how full the gate's energy store is and how much is left of the naquadah the DHD has started on.
 */
public final class DhdFuelMenu extends AbstractContainerMenu {

    /** How many slots of fuel a DHD has. */
    public static final int SLOTS = 5;
    /** {@link #data()} index of how full the gate's store is, in percent, or -1 when the DHD has no gate. */
    public static final int GATE_PERCENT = 0;
    /** {@link #data()} index of how much is left of the naquadah the DHD has started on, in percent. */
    public static final int BUFFER_PERCENT = 1;

    /** The menu's type; set when the loader registers menus. */
    public static MenuType<DhdFuelMenu> TYPE;

    private final Container fuel;
    private final ContainerData data;

    /** The client's menu, which the server fills. */
    public DhdFuelMenu(final int id, final Inventory inventory) {
        this(id, inventory, new SimpleContainer(DhdFuelMenu.SLOTS), new SimpleContainerData(2));
    }

    public DhdFuelMenu(final int id, final Inventory inventory, final Container fuel, final ContainerData data) {
        super(DhdFuelMenu.TYPE, id);
        AbstractContainerMenu.checkContainerSize(fuel, DhdFuelMenu.SLOTS);
        this.fuel = fuel;
        this.data = data;
        fuel.startOpen(inventory.player);
        for (int x = 0; x < DhdFuelMenu.SLOTS; x++) {
            this.addSlot(new Slot(fuel, x, 44 + x * 18, 20) {
                @Override
                public boolean mayPlace(final ItemStack stack) {
                    return stack.is(SGItems.NAQUADAH);
                }
            });
        }
        this.addStandardInventorySlots(inventory, 8, 51);
        this.addDataSlots(data);
    }

    /** @return what the menu shows besides the slots; see {@link #GATE_PERCENT} and {@link #BUFFER_PERCENT} */
    public ContainerData data() {
        return this.data;
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
        if (index < DhdFuelMenu.SLOTS) {
            if (!this.moveItemStackTo(stack, DhdFuelMenu.SLOTS, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!stack.is(SGItems.NAQUADAH) || !this.moveItemStackTo(stack, 0, DhdFuelMenu.SLOTS, false)) {
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
        this.fuel.stopOpen(player);
    }
}
