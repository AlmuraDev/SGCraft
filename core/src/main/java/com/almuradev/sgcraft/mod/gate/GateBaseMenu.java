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

package com.almuradev.sgcraft.mod.gate;

import com.almuradev.sgcraft.mod.block.entity.StargateBaseBlockEntity;
import com.almuradev.sgcraft.mod.zpm.ZpmItem;
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
 * A gate's base: five slots of camouflage, one block each, for the five blocks of the gate's bottom row from left to
 * right as seen from in front, above the player's inventory. Laid out as the original's. A supergate's base has three
 * ZPM slots beside them too, as in the original, and the menu sends how many ZPMs the supergate can draw on and how
 * much energy they hold.
 */
public final class GateBaseMenu extends AbstractContainerMenu {

    /** How many slots of camouflage the menu has. */
    public static final int SLOTS = StargateBaseBlockEntity.CAMOUFLAGE_SLOTS;
    /** How many ZPM slots it has, which only a supergate's base uses. */
    public static final int ZPM_SLOTS = StargateBaseBlockEntity.ZPM_SLOTS;
    /** Where the slots and the player's inventory are, as the screen's art draws them. */
    public static final int SLOTS_X = 48;
    public static final int SLOTS_Y = 104;
    public static final int INVENTORY_Y = 126;
    /** Where the ZPM slots are, as the original placed them. */
    public static final int ZPM_X = 166;

    /** What the menu sends: whether the base takes ZPMs and stands, then the ZPMs' count and energy. */
    public static final int FLAGS = 0;
    public static final int COUNT = 1;
    public static final int UNITS = 2;
    public static final int PARTS = 3;
    public static final int DATA = GateBaseMenu.UNITS + GateBaseMenu.PARTS;
    /** The flags' bits: the base takes ZPMs, and the gate stands, so it can be camouflaged. */
    public static final int TAKES_ZPMS = 1;
    public static final int STANDING = 2;

    /** The menu's type; set when the loader registers menus. */
    public static MenuType<GateBaseMenu> TYPE;

    private final Container camouflage;
    private final Container zpms;
    private final ContainerData data;

    /** The client's menu, which the server fills. */
    public GateBaseMenu(final int id, final Inventory inventory) {
        this(id, inventory, new SimpleContainer(GateBaseMenu.SLOTS) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        }, new SimpleContainer(GateBaseMenu.ZPM_SLOTS) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        }, new SimpleContainerData(GateBaseMenu.DATA));
    }

    public GateBaseMenu(final int id, final Inventory inventory, final Container camouflage, final Container zpms,
            final ContainerData data) {
        super(GateBaseMenu.TYPE, id);
        AbstractContainerMenu.checkContainerSize(camouflage, GateBaseMenu.SLOTS);
        AbstractContainerMenu.checkContainerSize(zpms, GateBaseMenu.ZPM_SLOTS);
        this.camouflage = camouflage;
        this.zpms = zpms;
        this.data = data;
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

                // A gate lying flat has no bottom row to camouflage
                @Override
                public boolean isActive() {
                    return GateBaseMenu.this.standing();
                }
            });
        }
        this.addStandardInventorySlots(inventory, GateBaseMenu.SLOTS_X, GateBaseMenu.INVENTORY_Y);
        for (int x = 0; x < GateBaseMenu.ZPM_SLOTS; x++) {
            this.addSlot(new Slot(zpms, x, GateBaseMenu.ZPM_X + x * 18, GateBaseMenu.SLOTS_Y) {
                @Override
                public boolean mayPlace(final ItemStack stack) {
                    return ZpmItem.is(stack) && GateBaseMenu.this.takesZpms();
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean isActive() {
                    return GateBaseMenu.this.takesZpms();
                }
            });
        }
        this.addDataSlots(data);
    }

    /** @return whether the base takes ZPMs: a supergate's does */
    public boolean takesZpms() {
        return (this.data.get(GateBaseMenu.FLAGS) & GateBaseMenu.TAKES_ZPMS) != 0;
    }

    /** @return whether the gate stands, so it can be camouflaged */
    public boolean standing() {
        return (this.data.get(GateBaseMenu.FLAGS) & GateBaseMenu.STANDING) != 0;
    }

    /** @return how many charged ZPMs the supergate can draw on, in its base and near it */
    public int zpmCount() {
        return this.data.get(GateBaseMenu.COUNT);
    }

    /** @return how much gate energy those ZPMs hold */
    public long zpmEnergy() {
        long value = 0;
        for (int part = 0; part < GateBaseMenu.PARTS; part++) {
            value |= (long) (this.data.get(GateBaseMenu.UNITS + part) & 0xFFFF) << (16 * part);
        }
        return value;
    }

    @Override
    public boolean stillValid(final Player player) {
        return this.camouflage.stillValid(player);
    }

    // A ZPM goes to the ZPM slots of a base that takes them, and a block to the camouflage; either comes back out
    // to the inventory
    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        final Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        final ItemStack moved = stack.copy();
        final int inventoryEnd = GateBaseMenu.SLOTS + 36;
        if (index < GateBaseMenu.SLOTS || index >= inventoryEnd) {
            if (!this.moveItemStackTo(stack, GateBaseMenu.SLOTS, inventoryEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (ZpmItem.is(stack) && this.takesZpms()) {
            if (!this.moveItemStackTo(stack, inventoryEnd, inventoryEnd + GateBaseMenu.ZPM_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.standing() || !StargateBaseBlockEntity.isCamouflage(stack)
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
