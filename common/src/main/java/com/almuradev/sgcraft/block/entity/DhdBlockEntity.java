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

import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.api.gate.GateTypes;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.block.DhdBlock;
import com.almuradev.sgcraft.dhd.DhdFuelMenu;
import com.almuradev.sgcraft.item.SGItems;
import com.almuradev.sgcraft.mod.SGServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
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

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

/**
 * A DHD's link to its gate: the nearest gate whose type accepts the DHD's style, within the gate's DHD range, or
 * {@link #LINK_RANGE} blocks if it has none.
 * It finds one when placed, and finds another if its gate is broken.
 *
 * <p>A DHD also holds naquadah, which fuels its gate as in the original: a piece is used up when the gate needs more
 * than the DHD has left of the last, and what that piece gives beyond the gate's need stays in the DHD.</p>
 */
public final class DhdBlockEntity extends BlockEntity {

    /** How far a DHD's gate can be, in blocks from the DHD to the gate's base. */
    public static final double LINK_RANGE = 6;
    // How often a DHD checks its link
    private static final int CHECK_TICKS = 20;

    private @Nullable BlockPos linked;
    // The naquadah, and the energy left of the last piece used
    private final SimpleContainer fuel = new SimpleContainer(DhdFuelMenu.SLOTS) {
        @Override
        public boolean stillValid(final Player player) {
            return Container.stillValidBlockEntity(DhdBlockEntity.this, player);
        }

        @Override
        public boolean canPlaceItem(final int slot, final ItemStack stack) {
            return stack.is(SGItems.NAQUADAH);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            DhdBlockEntity.this.setChanged();
        }
    };
    private long buffer;

    public DhdBlockEntity(final BlockPos pos, final BlockState state) {
        super(SGBlockEntityTypes.DHD, pos, state);
    }

    /** @return the DHD's naquadah */
    public Container fuel() {
        return this.fuel;
    }

    /** Opens the DHD's fuel for a player. */
    public void openFuel(final ServerPlayer player) {
        final ContainerData data = new ContainerData() {
            @Override
            public int get(final int index) {
                return switch (index) {
                    case DhdFuelMenu.GATE_PERCENT -> DhdBlockEntity.this.gate()
                            .map(gate -> gate.energy().capacity() <= 0 ? 100
                                    : (int) (100 * gate.energy().stored() / gate.energy().capacity()))
                            .orElse(-1);
                    case DhdFuelMenu.BUFFER_PERCENT -> {
                        final double piece = DhdBlockEntity.energyPerPiece();
                        yield piece <= 0 ? 0 : (int) Math.min(100, 100 * DhdBlockEntity.this.buffer / piece);
                    }
                    default -> 0;
                };
            }

            @Override
            public void set(final int index, final int value) {
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new DhdFuelMenu(id, inventory, this.fuel, data),
                Component.translatableWithFallback("container.sgcraft.dhd_fuel", "DHD Fuel")));
    }

    private static double energyPerPiece() {
        return SGServer.current().map(server -> server.config().energyPerNaquadah()).orElse(0.0);
    }

    /** @return how much energy the DHD could give its gate now: what is left of the last piece, and the rest */
    public long available() {
        long pieces = 0;
        for (final ItemStack stack : this.fuel) {
            if (stack.is(SGItems.NAQUADAH)) {
                pieces += stack.getCount();
            }
        }
        return this.buffer + (long) (pieces * DhdBlockEntity.energyPerPiece());
    }

    /**
     * Gives energy to the gate, using up naquadah, last slot first, as needed.
     *
     * @return how much was, or would be, given
     */
    public long draw(final long wanted, final boolean simulate) {
        if (simulate) {
            return Math.min(wanted, this.available());
        }
        long given = 0;
        while (given < wanted) {
            if (this.buffer <= 0 && !this.burn()) {
                break;
            }
            final long take = Math.min(this.buffer, wanted - given);
            this.buffer -= take;
            given += take;
        }
        this.setChanged();
        return given;
    }

    // Uses up one piece of naquadah, adding its energy to what is left
    private boolean burn() {
        for (int slot = this.fuel.getContainerSize() - 1; slot >= 0; slot--) {
            final ItemStack stack = this.fuel.getItem(slot);
            if (stack.is(SGItems.NAQUADAH)) {
                stack.shrink(1);
                this.fuel.setChanged();
                this.buffer += (long) DhdBlockEntity.energyPerPiece();
                return this.buffer > 0;
            }
        }
        return false;
    }

    @Override
    public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
        if (this.level != null) {
            Containers.dropContents(this.level, pos, this.fuel);
        }
    }

    /** @return the type of gate the DHD is made for */
    public ResourceKey<GateType> style() {
        return this.getBlockState().getBlock() instanceof DhdBlock dhd ? dhd.style() : GateTypes.MILKY_WAY;
    }

    /** @return the base of the gate the DHD dials, in the DHD's world, if it is linked */
    public Optional<BlockPos> linked() {
        return Optional.ofNullable(this.linked);
    }

    /**
     * The gate the DHD dials: its linked gate while that still accepts it, or else the nearest that does, which it
     * links to then. Only on the server.
     *
     * @return the gate, if there is one in reach
     */
    public Optional<Stargate> gate() {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }
        final Optional<Stargate> linked = this.linkedGate();
        if (linked.isPresent()) {
            return linked;
        }
        this.relink(serverLevel);
        return this.linkedGate();
    }

    private Optional<Stargate> linkedGate() {
        if (this.linked == null || this.level == null) {
            return Optional.empty();
        }
        return SGServer.current()
                .flatMap(server -> server.stargates().gate(GlobalPos.of(this.level.dimension(), this.linked)))
                .filter(gate -> gate.type().value().acceptsDhd(this.style()))
                .map(Stargate.class::cast);
    }

    /** Keeps the DHD linked: to its gate while that lasts, or else to the nearest gate that accepts it. */
    public static void serverTick(final ServerLevel level, final BlockPos pos, final DhdBlockEntity dhd) {
        if (level.getGameTime() % DhdBlockEntity.CHECK_TICKS == 0) {
            dhd.gate();
        }
    }

    private void relink(final ServerLevel level) {
        final BlockPos pos = this.worldPosition;
        final BlockPos nearest = SGServer.current()
                .flatMap(server -> server.stargates().in(level.dimension()).stream()
                        .filter(gate -> gate.type().value().acceptsDhd(this.style()))
                        .filter(gate -> gate.position().pos().closerThan(pos, DhdBlockEntity.range(gate)))
                        .map(gate -> gate.position().pos())
                        .min(Comparator.comparingDouble(gate -> gate.distSqr(pos))))
                .orElse(null);
        this.setLinked(nearest);
    }

    /** @return how far from a gate's base a DHD can stand and still link to it */
    public static double range(final Stargate gate) {
        return gate.get(GateProperties.DHD_RANGE).orElse(DhdBlockEntity.LINK_RANGE);
    }

    private void setLinked(final @Nullable BlockPos linked) {
        if (Objects.equals(this.linked, linked)) {
            return;
        }
        this.linked = linked;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        this.linked = input.read("linked", BlockPos.CODEC).orElse(null);
        this.fuel.clearContent();
        final NonNullList<ItemStack> items = NonNullList.withSize(DhdFuelMenu.SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        for (int i = 0; i < items.size(); i++) {
            this.fuel.setItem(i, items.get(i));
        }
        this.buffer = input.getLongOr("buffer", 0);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        if (this.linked != null) {
            output.store("linked", BlockPos.CODEC, this.linked);
        }
        final NonNullList<ItemStack> items = NonNullList.withSize(DhdFuelMenu.SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            items.set(i, this.fuel.getItem(i));
        }
        ContainerHelper.saveAllItems(output, items);
        output.putLong("buffer", this.buffer);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // Clients only need the link, not the fuel
    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        if (this.linked != null) {
            tag.store("linked", BlockPos.CODEC, this.linked);
        }
        return tag;
    }
}
