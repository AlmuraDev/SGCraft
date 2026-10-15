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

package com.almuradev.sgcraft.mod.block.entity;

import com.almuradev.sgcraft.mod.item.BatteryItem;
import com.almuradev.sgcraft.mod.item.SGItems;
import com.almuradev.sgcraft.mod.malp.MalpSettings;
import com.almuradev.sgcraft.mod.power.BatteryChargerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * A battery charger: a store of FE that cables fill and naquadah burned in it tops up, which it passes on to the
 * battery in its bay. It burns a piece of naquadah whenever its store has room for all of it.
 */
public final class BatteryChargerBlockEntity extends BlockEntity {

    /** The slot the battery goes in. */
    public static final int BATTERY = 0;
    /** The slot the naquadah goes in. */
    public static final int FUEL = 1;

    private final SimpleContainer items = new SimpleContainer(2) {
        @Override
        public boolean stillValid(final Player player) {
            return Container.stillValidBlockEntity(BatteryChargerBlockEntity.this, player);
        }

        @Override
        public boolean canPlaceItem(final int slot, final ItemStack stack) {
            return slot == BatteryChargerBlockEntity.BATTERY ? stack.is(SGItems.BATTERY) : stack.is(SGItems.NAQUADAH);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            BatteryChargerBlockEntity.this.setChanged();
        }
    };
    private long energy;
    // What the loader hands cables, made once so a transaction always meets the same one
    private @Nullable Object handler;

    public BatteryChargerBlockEntity(final BlockPos pos, final BlockState state) {
        super(SGBlockEntityTypes.BATTERY_CHARGER, pos, state);
    }

    /** @return the charger's battery bay and naquadah slot */
    public Container items() {
        return this.items;
    }

    /** @return the FE the charger holds */
    public long stored() {
        return this.energy;
    }

    /** @return the most FE a charger holds: two pieces of naquadah's worth */
    public static long capacity() {
        return 2 * MalpSettings.current().fePerNaquadah();
    }

    /** Sets the FE the charger holds, kept between none and its capacity. */
    public void setStored(final long energy) {
        final long kept = Math.max(0, Math.min(energy, BatteryChargerBlockEntity.capacity()));
        if (kept != this.energy) {
            this.energy = kept;
            this.setChanged();
        }
    }

    /**
     * Takes in FE, as much as there is room for.
     *
     * @return how much it took, or would take
     */
    public long insert(final long amount, final boolean simulate) {
        final long taken = Math.max(0, Math.min(amount, BatteryChargerBlockEntity.capacity() - this.energy));
        if (!simulate && taken > 0) {
            this.setStored(this.energy + taken);
        }
        return taken;
    }

    /**
     * The loader's handler for cables, made the first time it is asked for and kept, so every cable meets the same
     * one.
     */
    @SuppressWarnings("unchecked")
    public <T> T handler(final Function<BatteryChargerBlockEntity, T> make) {
        if (this.handler == null) {
            this.handler = make.apply(this);
        }
        return (T) this.handler;
    }

    /** Burns naquadah when there is room for its FE, and charges the battery from the store. */
    public static void serverTick(final ServerLevel level, final BlockPos pos,
            final BatteryChargerBlockEntity charger) {
        final MalpSettings settings = MalpSettings.current();
        final ItemStack fuel = charger.items.getItem(BatteryChargerBlockEntity.FUEL);
        if (fuel.is(SGItems.NAQUADAH) && settings.fePerNaquadah() > 0
                && charger.energy + settings.fePerNaquadah() <= BatteryChargerBlockEntity.capacity()) {
            fuel.shrink(1);
            charger.items.setChanged();
            charger.setStored(charger.energy + settings.fePerNaquadah());
        }
        final ItemStack battery = charger.items.getItem(BatteryChargerBlockEntity.BATTERY);
        final long taken = BatteryItem.insert(battery, Math.min(settings.chargerChargeRate(), charger.energy), false);
        if (taken > 0) {
            charger.items.setChanged();
            charger.setStored(charger.energy - taken);
        }
    }

    /** Opens the charger for a player. */
    public void open(final ServerPlayer player) {
        final ContainerData data = new ContainerData() {
            @Override
            public int get(final int index) {
                if (index == BatteryChargerMenu.STORE_PERCENT) {
                    final long capacity = BatteryChargerBlockEntity.capacity();
                    return capacity <= 0 ? 0 : (int) (100 * BatteryChargerBlockEntity.this.energy / capacity);
                }
                final ItemStack battery = BatteryChargerBlockEntity.this.items
                        .getItem(BatteryChargerBlockEntity.BATTERY);
                return battery.isEmpty() ? -1 : BatteryItem.percent(battery);
            }

            @Override
            public void set(final int index, final int value) {
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new BatteryChargerMenu(id, inventory, this.items,
                data), Component.translatableWithFallback("container.sgcraft.battery_charger", "Battery Charger")));
    }

    @Override
    public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
        if (this.level != null) {
            Containers.dropContents(this.level, pos, this.items);
        }
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        this.energy = input.getLongOr("energy", 0);
        final NonNullList<ItemStack> stacks = NonNullList.withSize(this.items.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, stacks);
        for (int slot = 0; slot < stacks.size(); slot++) {
            this.items.setItem(slot, stacks.get(slot));
        }
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("energy", this.energy);
        final NonNullList<ItemStack> stacks = NonNullList.withSize(this.items.getContainerSize(), ItemStack.EMPTY);
        for (int slot = 0; slot < stacks.size(); slot++) {
            stacks.set(slot, this.items.getItem(slot));
        }
        ContainerHelper.saveAllItems(output, stacks);
    }
}
