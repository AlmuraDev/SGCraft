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

package com.almuradev.sgcraft.mod.malp;

import com.almuradev.sgcraft.mod.entity.MalpEntity;
import com.almuradev.sgcraft.mod.item.BatteryItem;
import com.almuradev.sgcraft.mod.item.MalpTabletItem;
import com.almuradev.sgcraft.mod.item.SGItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * A MALP's battery slot over the player's inventory, with how charged the battery is, whether the MALP needs power,
 * and the console it is paired with; or a MALP tablet's battery slot, with how charged its battery is and how many
 * MALPs it is paired with. The game sends a menu's numbers sixteen bits at a time, so the console's coordinates go as
 * two halves each.
 *
 * <p>A tablet's menu is open while the tablet stays in the hand it was used from. The tablet's own slot in the
 * player's inventory can't be clicked, nor swapped with a number key, while its menu is open.</p>
 */
public final class MalpMenu extends AbstractContainerMenu {

    /** {@link #data()} index of the battery's charge, in percent, or -1 for no battery. */
    public static final int BATTERY = 0;
    /** {@link #data()} index of whether the MALP has a console: 0 for none, 1 in this world, 2 in another. */
    public static final int CONSOLE = 1;
    /** {@link #data()} index of whether the MALP runs on its battery: 1 if it does, 0 if it needs no power. */
    public static final int POWER_REQUIRED = 8;
    /**
     * {@link #data()} index of what the menu is for: 0 for a MALP, or, for a tablet, one more than the number of MALPs
     * paired with it.
     */
    public static final int TABLET = 9;
    // Where the console's coordinates start, two halves each, lowest first
    private static final int X = 2;
    private static final int Y = 4;
    private static final int Z = 6;
    private static final int COUNT = 10;
    private static final double REACH = 8;

    /** The menu's type; set when the loader registers menus. */
    public static MenuType<MalpMenu> TYPE;

    private final ContainerData data;
    private final Predicate<Player> valid;
    // The slot of the player's inventory that holds the tablet whose menu this is, or -1 for a MALP's menu
    private final int locked;

    /** The client's menu, which the server fills. */
    public MalpMenu(final int id, final Inventory inventory) {
        this(id, inventory, new SimpleContainer(1), new SimpleContainerData(MalpMenu.COUNT), player -> true, -1);
    }

    /** The menu of a MALP, open while the MALP is alive and within reach. */
    public MalpMenu(final int id, final Inventory inventory, final MalpEntity malp) {
        this(id, inventory, malp.batterySlot(), MalpMenu.data(malp, inventory.player),
                player -> malp.isAlive() && player.distanceTo(malp) <= MalpMenu.REACH, -1);
    }

    /** The menu of a tablet in a player's hand, open while the tablet stays in that hand. */
    public MalpMenu(final int id, final Inventory inventory, final ItemStack tablet, final InteractionHand hand) {
        this(id, inventory, MalpMenu.tabletBattery(tablet), MalpMenu.data(tablet),
                player -> player.getItemInHand(hand) == tablet,
                hand == InteractionHand.MAIN_HAND ? inventory.getSelectedSlot() : Inventory.SLOT_OFFHAND);
    }

    private MalpMenu(final int id, final Inventory inventory, final Container battery, final ContainerData data,
            final Predicate<Player> valid, final int locked) {
        super(MalpMenu.TYPE, id);
        this.data = data;
        this.valid = valid;
        this.locked = locked;
        this.addSlot(new Slot(battery, 0, 80, 35) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return stack.is(SGItems.BATTERY);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        this.addStandardInventorySlots(inventory, 8, 84);
        this.addDataSlots(data);
    }

    private static ContainerData data(final MalpEntity malp, final Player player) {
        return new ContainerData() {
            @Override
            public int get(final int index) {
                final GlobalPos console = malp.console();
                if (index == MalpMenu.TABLET) {
                    return 0;
                }
                if (index == MalpMenu.BATTERY) {
                    return malp.battery().isEmpty() ? -1 : BatteryItem.percent(malp.battery());
                }
                if (index == MalpMenu.POWER_REQUIRED) {
                    return malp.options().powerRequired() ? 1 : 0;
                }
                if (index == MalpMenu.CONSOLE) {
                    return console == null ? 0 : console.dimension() == player.level().dimension() ? 1 : 2;
                }
                if (console == null) {
                    return 0;
                }
                final int value = index < MalpMenu.Y ? console.pos().getX() : index < MalpMenu.Z
                        ? console.pos().getY() : console.pos().getZ();
                return (index % 2 == 0 ? value : value >>> 16) & 0xFFFF;
            }

            @Override
            public void set(final int index, final int value) {
            }

            @Override
            public int getCount() {
                return MalpMenu.COUNT;
            }
        };
    }

    // A tablet's battery slot, which writes what is put in it, or taken out, back to the tablet
    private static Container tabletBattery(final ItemStack tablet) {
        final SimpleContainer slot = new SimpleContainer(1) {
            @Override
            public void setChanged() {
                super.setChanged();
                MalpTabletItem.setBattery(tablet, this.getItem(0));
            }
        };
        slot.setItem(0, MalpTabletItem.battery(tablet));
        return slot;
    }

    private static ContainerData data(final ItemStack tablet) {
        return new ContainerData() {
            @Override
            public int get(final int index) {
                return switch (index) {
                    case MalpMenu.BATTERY -> MalpTabletItem.percent(tablet);
                    case MalpMenu.POWER_REQUIRED -> 1;
                    case MalpMenu.TABLET -> 1 + MalpTabletItem.malps(tablet).size();
                    default -> 0;
                };
            }

            @Override
            public void set(final int index, final int value) {
            }

            @Override
            public int getCount() {
                return MalpMenu.COUNT;
            }
        };
    }

    /**
     * @return what the menu shows besides the slot: {@link #BATTERY}, {@link #CONSOLE}, {@link #POWER_REQUIRED} and
     *     {@link #TABLET}
     */
    public ContainerData data() {
        return this.data;
    }

    /** @return where the paired console is, when {@link #CONSOLE} says there is one */
    public BlockPos console() {
        return new BlockPos(this.read(MalpMenu.X), this.read(MalpMenu.Y), this.read(MalpMenu.Z));
    }

    private int read(final int from) {
        return this.data.get(from) & 0xFFFF | (this.data.get(from + 1) & 0xFFFF) << 16;
    }

    @Override
    public boolean stillValid(final Player player) {
        return this.valid.test(player);
    }

    // The tablet whose menu this is stays where it is
    @Override
    public void clicked(final int slot, final int button, final ContainerInput input, final Player player) {
        if (this.locked >= 0 && (input == ContainerInput.SWAP && button == this.locked
                || slot >= 0 && slot < this.slots.size() && this.slots.get(slot).container == player.getInventory()
                && this.slots.get(slot).getContainerSlot() == this.locked)) {
            return;
        }
        super.clicked(slot, button, input, player);
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
