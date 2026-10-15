/*
 * This file is part of SGCraft, licensed under the MIT License (MIT).
 * It is based on the original SGCraft by gcewing.
 *
 * Copyright (c) AlmuraDev <https://github.com/AlmuraDev>
 * Copyright (c) 2016 gcewing <https://github.com/gcewing>
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

package com.almuradev.sgcraft.gate;

import com.almuradev.sgcraft.block.entity.StargateBaseBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A gate's base: five slots of camouflage, one block each, for the five blocks of the gate's bottom row from left to
 * right as seen from in front, above the player's inventory. Laid out as the original's.
 */
public final class GateBaseMenu extends AbstractContainerMenu {

    /** How many slots of camouflage the menu has. */
    public static final int SLOTS = StargateBaseBlockEntity.CAMOUFLAGE_SLOTS;
    /** Where the slots and the player's inventory are, as the screen's art draws them. */
    public static final int SLOTS_X = 48;
    public static final int SLOTS_Y = 104;
    public static final int INVENTORY_Y = 126;

    /** The menu's type; set when the loader registers menus. */
    public static MenuType<GateBaseMenu> TYPE;

    private final Container camouflage;

    /** The client's menu, which the server fills. */
    public GateBaseMenu(final int id, final Inventory inventory) {
        this(id, inventory, new SimpleContainer(GateBaseMenu.SLOTS) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
    }

    public GateBaseMenu(final int id, final Inventory inventory, final Container camouflage) {
        super(GateBaseMenu.TYPE, id);
        AbstractContainerMenu.checkContainerSize(camouflage, GateBaseMenu.SLOTS);
        this.camouflage = camouflage;
        camouflage.startOpen(inventory.player);
        for (int x = 0; x < GateBaseMenu.SLOTS; x++) {
            this.addSlot(new Slot(camouflage, x, GateBaseMenu.SLOTS_X + x * 18, GateBaseMenu.SLOTS_Y) {
                @Override
                public boolean mayPlace(final ItemStack stack) {
                    return StargateBaseBlockEntity.isCamouflage(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        this.addStandardInventorySlots(inventory, GateBaseMenu.SLOTS_X, GateBaseMenu.INVENTORY_Y);
    }

    @Override
    public boolean stillValid(final Player player) {
        return this.camouflage.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        final Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        final ItemStack moved = stack.copy();
        if (index < GateBaseMenu.SLOTS) {
            if (!this.moveItemStackTo(stack, GateBaseMenu.SLOTS, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!StargateBaseBlockEntity.isCamouflage(stack)
                || !this.moveItemStackTo(stack, 0, GateBaseMenu.SLOTS, false)) {
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
        this.camouflage.stopOpen(player);
    }
}
