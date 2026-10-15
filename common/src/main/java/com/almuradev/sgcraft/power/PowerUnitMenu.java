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

package com.almuradev.sgcraft.power;

import com.almuradev.sgcraft.block.SGBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A power unit's charge and capacity, in FE, for its screen. The game sends a menu's numbers sixteen bits at a time,
 * so each is sent as {@link #PARTS} parts, lowest first: the charge's at index 0, the capacity's from
 * {@link #CAPACITY}.
 */
public final class PowerUnitMenu extends AbstractContainerMenu {

    /** How many sixteen-bit parts each number is sent as, enough for any charge a unit could hold. */
    public static final int PARTS = 3;
    /** The index the capacity's parts start at. */
    public static final int CAPACITY = PowerUnitMenu.PARTS;

    /** The menu's type; set when the loader registers menus. */
    public static MenuType<PowerUnitMenu> TYPE;

    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** The client's menu, which the server fills. */
    public PowerUnitMenu(final int id, final Inventory inventory) {
        this(id, inventory, new SimpleContainerData(2 * PowerUnitMenu.PARTS), null);
    }

    public PowerUnitMenu(final int id, final Inventory inventory, final ContainerData data,
            final @Nullable BlockPos pos) {
        super(PowerUnitMenu.TYPE, id);
        this.data = data;
        this.access = pos == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(inventory.player.level(),
                pos);
        this.addDataSlots(data);
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
        return AbstractContainerMenu.stillValid(this.access, player, SGBlocks.POWER_UNIT);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        return ItemStack.EMPTY;
    }
}
