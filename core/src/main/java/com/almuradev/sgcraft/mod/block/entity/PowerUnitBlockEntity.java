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

import com.almuradev.sgcraft.api.energy.EnergyBridge;
import com.almuradev.sgcraft.mod.SGConfig;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.energy.FeRate;
import com.almuradev.sgcraft.mod.energy.FeSink;
import com.almuradev.sgcraft.mod.energy.GeStore;
import com.almuradev.sgcraft.mod.item.BatteryItem;
import com.almuradev.sgcraft.mod.item.SGDataComponents;
import com.almuradev.sgcraft.mod.item.SGItems;
import com.almuradev.sgcraft.mod.malp.MalpSettings;
import com.almuradev.sgcraft.mod.power.PowerUnitMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
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
 * A stargate power unit, as the original's: a store of gate energy (GE) that cables fill and nothing drains but the
 * gates near it, and the battery in its slot. Cables put FE in, which it keeps as the GE it is worth at the
 * {@link #FORGE_ENERGY} bridge's rate; gates draw its GE through
 * {@link com.almuradev.sgcraft.mod.power.PowerUnitProvider}. It keeps its charge when broken, and drops its battery.
 */
public final class PowerUnitBlockEntity extends BlockEntity implements GeStore, FeSink {

    /**
     * The bridge between FE and gate energy, whose rate is how much gate energy one FE buys coming in: one over the
     * server's {@code energy.bridge_rates.fe_in_per_ge}, the original's 80 FE to a unit unless set.
     */
    public static final EnergyBridge FORGE_ENERGY = () -> 1.0 / SGServer.current()
            .map(server -> server.config().fePerGeIn()).orElse(SGConfig.DEFAULTS.fePerGeIn());

    private final SimpleContainer battery = new SimpleContainer(1) {
        @Override
        public boolean stillValid(final Player player) {
            return Container.stillValidBlockEntity(PowerUnitBlockEntity.this, player);
        }

        @Override
        public boolean canPlaceItem(final int slot, final ItemStack stack) {
            return stack.is(SGItems.BATTERY);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            PowerUnitBlockEntity.this.setChanged();
        }
    };
    private double energy;
    // What the loader hands cables, made once so a transaction always meets the same one
    private @Nullable Object handler;

    public PowerUnitBlockEntity(final BlockPos pos, final BlockState state) {
        super(SGBlockEntityTypes.POWER_UNIT, pos, state);
    }

    /** @return the gate energy the unit holds */
    @Override
    public double stored() {
        return this.energy;
    }

    /** @return the most gate energy a unit holds, as the server's settings say */
    public static long capacity() {
        return SGServer.current().map(server -> server.config().powerUnitCapacity()).orElse(50_000L);
    }

    /**
     * Sets the gate energy the unit holds, kept between none and its capacity; for loaders putting back a charge when
     * a transaction is undone, and for commands.
     */
    @Override
    public void setStored(final double energy) {
        final double kept = Math.max(0, Math.min(energy, PowerUnitBlockEntity.capacity()));
        if (kept != this.energy) {
            this.energy = kept;
            this.setChanged();
        }
    }

    /** @return the unit's gate energy as cables see it, in FE */
    public long feStored() {
        return FeRate.toFeIn(this.energy);
    }

    /** @return the most a unit holds as cables see it, in FE */
    public static long feCapacity() {
        return FeRate.toFeIn(PowerUnitBlockEntity.capacity());
    }

    /**
     * Takes in FE from a cable, as much as there is room for, and keeps the gate energy it is worth.
     *
     * @return how much FE it took, or would take
     */
    @Override
    public long insertFe(final long fe, final boolean simulate) {
        final long taken = Math.max(0, Math.min(fe, FeRate.toFeIn(PowerUnitBlockEntity.capacity() - this.energy)));
        if (!simulate && taken > 0) {
            this.setStored(this.energy + FeRate.toGeIn(taken));
        }
        return taken;
    }

    @Override
    public long feAmount() {
        return this.feStored();
    }

    @Override
    public long feLimit() {
        return PowerUnitBlockEntity.feCapacity();
    }

    /** @return how much gate energy the unit could give now, in whole units */
    public long available() {
        return (long) Math.floor(this.energy);
    }

    /**
     * Gives gate energy.
     *
     * @return how much it gave, or would give
     */
    public long draw(final long wanted, final boolean simulate) {
        final long given = Math.max(0, Math.min(wanted, this.available()));
        if (!simulate && given > 0) {
            this.setStored(this.energy - given);
        }
        return given;
    }

    @Override
    public double givable() {
        return this.energy;
    }

    @Override
    public double give(final double wanted) {
        final double given = Math.max(0, Math.min(wanted, this.energy));
        if (given > 0) {
            this.setStored(this.energy - given);
        }
        return given;
    }

    /**
     * The loader's handler for cables, made the first time it is asked for and kept, so every cable meets the same
     * one.
     */
    @SuppressWarnings("unchecked")
    public <T> T handler(final Function<PowerUnitBlockEntity, T> make) {
        if (this.handler == null) {
            this.handler = make.apply(this);
        }
        return (T) this.handler;
    }

    /** @return the unit's battery slot */
    public Container battery() {
        return this.battery;
    }

    /** Charges the battery in the unit's slot from the unit's store. */
    public static void serverTick(final ServerLevel level, final BlockPos pos, final PowerUnitBlockEntity unit) {
        final double rate = Math.min(MalpSettings.current().powerUnitChargeRate(), unit.energy);
        final double taken = BatteryItem.insert(unit.battery.getItem(0), rate, false);
        if (taken > 0) {
            unit.battery.setChanged();
            unit.setStored(unit.energy - taken);
        }
    }

    /** Shows the unit's charge and its battery slot to a player. */
    public void open(final ServerPlayer player) {
        final ContainerData data = new ContainerData() {
            @Override
            public int get(final int index) {
                if (index == PowerUnitMenu.BATTERY) {
                    final ItemStack battery = PowerUnitBlockEntity.this.battery.getItem(0);
                    return battery.isEmpty() ? -1 : BatteryItem.percent(battery);
                }
                return PowerUnitMenu.part(index < PowerUnitMenu.CAPACITY ? PowerUnitBlockEntity.this.available()
                        : PowerUnitBlockEntity.capacity(), index % PowerUnitMenu.PARTS);
            }

            @Override
            public void set(final int index, final int value) {
            }

            @Override
            public int getCount() {
                return PowerUnitMenu.BATTERY + 1;
            }
        };
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new PowerUnitMenu(id, inventory, this.battery,
                data), Component.translatableWithFallback("container.sgcraft.power_unit", "Stargate Power Unit")));
    }

    @Override
    public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
        if (this.level != null) {
            Containers.dropContents(this.level, pos, this.battery);
        }
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        this.energy = input.getDoubleOr("energy", 0);
        this.battery.setItem(0, input.read("battery", ItemStack.CODEC).orElse(ItemStack.EMPTY));
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("energy", this.energy);
        if (!this.battery.getItem(0).isEmpty()) {
            output.store("battery", ItemStack.CODEC, this.battery.getItem(0));
        }
    }

    // The charge goes with the unit when it is broken, and comes back when it is placed
    @Override
    protected void applyImplicitComponents(final DataComponentGetter components) {
        super.applyImplicitComponents(components);
        this.energy = components.getOrDefault(SGDataComponents.ENERGY, 0.0);
    }

    @Override
    protected void collectImplicitComponents(final DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (this.energy > 0) {
            components.set(SGDataComponents.ENERGY, this.energy);
        }
    }

    @Override
    public void removeComponentsFromTag(final ValueOutput output) {
        output.discard("energy");
    }
}
