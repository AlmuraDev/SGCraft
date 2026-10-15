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

package com.almuradev.sgcraft.block.entity;

import com.almuradev.sgcraft.api.SGCraftApi;
import com.almuradev.sgcraft.api.energy.EnergyBridge;
import com.almuradev.sgcraft.item.SGDataComponents;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.power.PowerUnitMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * A stargate power unit, as the original's: a store of the loaders' common energy, FE, which cables fill and nothing
 * drains but the gates near it. Gates draw on it through {@link com.almuradev.sgcraft.power.PowerUnitProvider},
 * turning FE into their own energy at the {@link #FORGE_ENERGY} bridge's rate. It keeps its charge when broken.
 */
public final class PowerUnitBlockEntity extends BlockEntity {

    /**
     * The bridge between FE and gate energy, whose rate is how much gate energy one FE is worth: the original's 80 FE
     * to a unit, unless the server's settings say otherwise.
     */
    public static final EnergyBridge FORGE_ENERGY = () -> 1.0 / 80;

    private long energy;
    // What the loader hands cables, made once so a transaction always meets the same one
    private @Nullable Object handler;

    public PowerUnitBlockEntity(final BlockPos pos, final BlockState state) {
        super(SGBlockEntityTypes.POWER_UNIT, pos, state);
    }

    /** @return the FE the unit holds */
    public long stored() {
        return this.energy;
    }

    /** @return the most FE a unit holds, as the server's settings say */
    public static long capacity() {
        return SGServer.current().map(server -> server.config().powerUnitCapacity()).orElse(4_000_000L);
    }

    /**
     * Sets the FE the unit holds, kept between none and its capacity; for loaders putting back a charge when a
     * transaction is undone, and for commands.
     */
    public void setStored(final long energy) {
        final long kept = Math.max(0, Math.min(energy, PowerUnitBlockEntity.capacity()));
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
        final long taken = Math.max(0, Math.min(amount, PowerUnitBlockEntity.capacity() - this.energy));
        if (!simulate && taken > 0) {
            this.setStored(this.energy + taken);
        }
        return taken;
    }

    /** @return how much gate energy the unit could give now */
    public long available() {
        return (long) Math.floor(this.energy * PowerUnitBlockEntity.rate());
    }

    /**
     * Gives gate energy, using up the FE it is worth.
     *
     * @return how much it gave, or would give
     */
    public long draw(final long wanted, final boolean simulate) {
        final long given = Math.max(0, Math.min(wanted, this.available()));
        if (!simulate && given > 0) {
            this.setStored(this.energy - (long) Math.ceil(given / PowerUnitBlockEntity.rate()));
        }
        return given;
    }

    private static double rate() {
        return SGCraftApi.get().energy().rate(PowerUnitBlockEntity.FORGE_ENERGY);
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

    /** Shows the unit's charge to a player. */
    public void open(final ServerPlayer player) {
        final ContainerData data = new ContainerData() {
            @Override
            public int get(final int index) {
                return PowerUnitMenu.part(index < PowerUnitMenu.CAPACITY ? PowerUnitBlockEntity.this.energy
                        : PowerUnitBlockEntity.capacity(), index % PowerUnitMenu.PARTS);
            }

            @Override
            public void set(final int index, final int value) {
            }

            @Override
            public int getCount() {
                return 2 * PowerUnitMenu.PARTS;
            }
        };
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new PowerUnitMenu(id, inventory, data,
                this.getBlockPos()), Component.translatableWithFallback("container.sgcraft.power_unit",
                "Stargate Power Unit")));
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        this.energy = input.getLongOr("energy", 0);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("energy", this.energy);
    }

    // The charge goes with the unit when it is broken, and comes back when it is placed
    @Override
    protected void applyImplicitComponents(final DataComponentGetter components) {
        super.applyImplicitComponents(components);
        this.energy = components.getOrDefault(SGDataComponents.ENERGY, 0L);
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
