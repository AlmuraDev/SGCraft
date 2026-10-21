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

package com.almuradev.sgcraft.mod.generator;

import com.almuradev.sgcraft.mod.block.entity.SGBlockEntityTypes;
import com.almuradev.sgcraft.mod.energy.GeStore;
import com.almuradev.sgcraft.mod.item.SGItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.stream.IntStream;

/**
 * A Naquadah Generator's power, as in 2.2.0. Switched on, it burns its fuel, raw naquadah or naquadah alloy ingots, a
 * piece at a time, to fill its store of gate energy as fast as its mark allows, and burns nothing while the store is
 * full. Gates within reach draw on that store (see {@link GeneratorPowerProvider}), and an energy converter against it
 * turns it into FE. Switched off, it gives nothing.
 *
 * <p>Its four fuel slots take only fuel, from its screen or from hoppers, which can't take anything out.</p>
 */
public final class NaquadahGeneratorBlockEntity extends BlockEntity implements GeStore, WorldlyContainer {

    /** How many fuel slots it has. */
    public static final int SLOTS = 4;
    private static final int[] ALL_SLOTS = IntStream.range(0, NaquadahGeneratorBlockEntity.SLOTS).toArray();

    private final NonNullList<ItemStack> fuel = NonNullList.withSize(NaquadahGeneratorBlockEntity.SLOTS,
            ItemStack.EMPTY);
    private double energy;
    // What is left of the piece of fuel it is burning, in gate energy
    private double fuelLeft;

    public NaquadahGeneratorBlockEntity(final BlockPos pos, final BlockState state) {
        super(SGBlockEntityTypes.NAQUADAH_GENERATOR, pos, state);
    }

    /** @return its mark */
    public GeneratorMark mark() {
        return this.getBlockState().getBlock() instanceof final NaquadahGeneratorBlock block ? block.mark()
                : GeneratorMark.ONE;
    }

    /** @return what a stack of fuel is worth a piece, in gate energy before a mark's efficiency; 0 for no fuel */
    public static double fuelValue(final ItemStack stack) {
        final GeneratorSettings settings = GeneratorSettings.current();
        if (stack.is(SGItems.NAQUADAH)) {
            return settings.naquadahEnergy();
        }
        return stack.is(SGItems.NAQUADAH_INGOT) ? settings.ingotEnergy() : 0;
    }

    /** @return whether it is switched on */
    public boolean on() {
        return this.getBlockState().getValue(NaquadahGeneratorBlock.ON);
    }

    /** Switches it on or off, its arms going out or in. */
    public void setOn(final boolean on) {
        if (this.level != null && this.on() != on) {
            this.level.setBlock(this.worldPosition, this.getBlockState().setValue(NaquadahGeneratorBlock.ON, on)
                    .setValue(NaquadahGeneratorBlock.RUNNING, on && this.hasPower()), Block.UPDATE_ALL);
        }
    }

    /** @return the gate energy it holds ready */
    public double stored() {
        return this.energy;
    }

    /** @return the most it holds */
    public double capacity() {
        return this.mark().settings().capacity();
    }

    // Whether it has energy or fuel to give
    private boolean hasPower() {
        return this.energy > 0 || this.fuelLeft > 0 || this.fuelInSlots() > 0;
    }

    /** @return what the fuel in its slots, and what is left of the piece it burns, is worth to it, in gate energy */
    public double fuelInSlots() {
        double total = 0;
        for (final ItemStack stack : this.fuel) {
            total += NaquadahGeneratorBlockEntity.fuelValue(stack) * stack.getCount();
        }
        return total * this.mark().settings().efficiency();
    }

    /** @return how many ticks of running at full output its fuel lasts, counting what it holds ready */
    public long ticksLeft() {
        final double perTick = this.mark().settings().energyPerTick();
        return perTick <= 0 ? 0 : (long) ((this.fuelLeft + this.fuelInSlots()) / perTick);
    }

    /** Burns fuel to fill its store at its mark's rate, while switched on. */
    public static void serverTick(final ServerLevel level, final BlockPos pos, final BlockState state,
            final NaquadahGeneratorBlockEntity generator) {
        if (state.getValue(NaquadahGeneratorBlock.ON)) {
            generator.generate();
        }
        final boolean running = state.getValue(NaquadahGeneratorBlock.ON) && generator.hasPower();
        if (running != state.getValue(NaquadahGeneratorBlock.RUNNING)) {
            level.setBlock(pos, state.setValue(NaquadahGeneratorBlock.RUNNING, running), Block.UPDATE_ALL);
        }
    }

    // A piece at a time, and nothing while its store is full
    private void generate() {
        final GeneratorSettings.Mark mark = this.mark().settings();
        final double need = Math.min(mark.energyPerTick(), mark.capacity() - this.energy);
        if (need <= 0) {
            return;
        }
        while (this.fuelLeft < need && this.burn(mark)) {
            // Took another piece
        }
        final double made = Math.min(need, this.fuelLeft);
        if (made > 0) {
            this.energy += made;
            this.fuelLeft -= made;
            this.setChanged();
        }
    }

    private boolean burn(final GeneratorSettings.Mark mark) {
        for (final ItemStack stack : this.fuel) {
            final double worth = NaquadahGeneratorBlockEntity.fuelValue(stack);
            if (worth > 0) {
                stack.shrink(1);
                this.fuelLeft += worth * mark.efficiency();
                this.setChanged();
                return worth * mark.efficiency() > 0;
            }
        }
        return false;
    }

    // ---- Gates and energy converters ----

    /** @return how much gate energy it could give gates now, in whole units; none while switched off */
    public long available() {
        return this.on() ? (long) Math.floor(this.energy) : 0;
    }

    /**
     * Gives gate energy to a gate.
     *
     * @return how much it gave, or would give
     */
    public long draw(final long wanted, final boolean simulate) {
        final long given = Math.max(0, Math.min(wanted, this.available()));
        if (!simulate && given > 0) {
            this.energy -= given;
            this.setChanged();
        }
        return given;
    }

    @Override
    public double givable() {
        return this.on() ? this.energy : 0;
    }

    @Override
    public double give(final double wanted) {
        final double given = Math.max(0, Math.min(wanted, this.givable()));
        if (given > 0) {
            this.energy -= given;
            this.setChanged();
        }
        return given;
    }

    // ---- Its screen ----

    /** Shows its state and its fuel slots to a player. */
    public void open(final ServerPlayer player) {
        final ContainerData data = new ContainerData() {
            @Override
            public int get(final int index) {
                final NaquadahGeneratorBlockEntity generator = NaquadahGeneratorBlockEntity.this;
                return switch (index / NaquadahGeneratorMenu.PARTS) {
                    case 0 -> NaquadahGeneratorMenu.part((long) generator.energy, index);
                    case 1 -> NaquadahGeneratorMenu.part((long) generator.capacity(), index);
                    case 2 -> NaquadahGeneratorMenu.part((long) Math.round(generator.mark().settings()
                            .energyPerTick() * 100), index);
                    case 3 -> NaquadahGeneratorMenu.part(generator.ticksLeft(), index);
                    default -> generator.on() ? 1 : 0;
                };
            }

            @Override
            public void set(final int index, final int value) {
            }

            @Override
            public int getCount() {
                return NaquadahGeneratorMenu.DATA;
            }
        };
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new NaquadahGeneratorMenu(id, inventory, this,
                data), this.getBlockState().getBlock().getName()));
    }

    // ---- Its fuel slots ----

    @Override
    public int getContainerSize() {
        return NaquadahGeneratorBlockEntity.SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return this.fuel.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(final int slot) {
        return this.fuel.get(slot);
    }

    @Override
    public ItemStack removeItem(final int slot, final int count) {
        final ItemStack taken = ContainerHelper.removeItem(this.fuel, slot, count);
        if (!taken.isEmpty()) {
            this.setChanged();
        }
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        return ContainerHelper.takeItem(this.fuel, slot);
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        this.fuel.set(slot, stack);
        stack.limitSize(this.getMaxStackSize(stack));
        this.setChanged();
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        return NaquadahGeneratorBlockEntity.fuelValue(stack) > 0;
    }

    @Override
    public boolean stillValid(final Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.fuel.clear();
    }

    @Override
    public int[] getSlotsForFace(final Direction side) {
        return NaquadahGeneratorBlockEntity.ALL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(final int slot, final ItemStack stack, final @Nullable Direction side) {
        return this.canPlaceItem(slot, stack);
    }

    // Hoppers put fuel in, but take nothing out
    @Override
    public boolean canTakeItemThroughFace(final int slot, final ItemStack stack, final Direction side) {
        return false;
    }

    @Override
    public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
        if (this.level != null) {
            Containers.dropContents(this.level, pos, this);
        }
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        this.energy = input.getDoubleOr("energy", 0);
        this.fuelLeft = input.getDoubleOr("fuel_left", 0);
        this.fuel.clear();
        ContainerHelper.loadAllItems(input, this.fuel);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("energy", this.energy);
        output.putDouble("fuel_left", this.fuelLeft);
        ContainerHelper.saveAllItems(output, this.fuel);
    }
}
