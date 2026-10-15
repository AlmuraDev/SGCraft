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

import com.almuradev.sgcraft.api.SGCraftRegistries;
import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.api.gate.GateTypes;
import com.almuradev.sgcraft.block.DhdBlock;
import com.almuradev.sgcraft.block.RingBlock;
import com.almuradev.sgcraft.block.StargateBaseBlock;
import com.almuradev.sgcraft.client.gate.GateLoopSounds;
import com.almuradev.sgcraft.entity.IrisPlateEntity;
import com.almuradev.sgcraft.gate.DialPhase;
import com.almuradev.sgcraft.gate.DialView;
import com.almuradev.sgcraft.gate.EventHorizon;
import com.almuradev.sgcraft.gate.FluidSpill;
import com.almuradev.sgcraft.gate.GateFormation;
import com.almuradev.sgcraft.gate.GateFrame;
import com.almuradev.sgcraft.gate.GateOrientation;
import com.almuradev.sgcraft.gate.GateShape;
import com.almuradev.sgcraft.gate.IrisView;
import com.almuradev.sgcraft.item.SGItems;
import com.almuradev.sgcraft.mod.SGIris;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.SGStargate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The state of a gate, held by its base block.
 */
public final class StargateBaseBlockEntity extends BlockEntity {


    // How many chevrons a gate has when it first forms
    private static final int DEFAULT_CHEVRONS = 7;
    // How often a formed base checks the server knows of its gate and shows what the gate is doing
    private static final int CHECK_TICKS = 20;

    /** How many blocks of camouflage a base holds: one for each block of the gate's bottom row, left to right. */
    public static final int CAMOUFLAGE_SLOTS = 5;

    private boolean formed;
    private final Camouflage camouflage = new Camouflage();
    private ResourceKey<GateType> gateType;
    private int chevrons = StargateBaseBlockEntity.DEFAULT_CHEVRONS;
    private GateShape shape = GateShape.STANDARD;
    // Whether the gate stands or lies flat; kept while it is broken, so a flat ring turned face down forms so again
    private GateOrientation orientation = GateOrientation.STANDING;
    // The fluid pouring out of the gate, from a gate standing in it at the other end of its wormhole; only shown
    private @Nullable FluidSpill spill;
    // What the gate is doing, sent to clients but not saved: the server's gates are idle when it starts
    private DialView dialView = DialView.IDLE;
    // The gate's iris, if one is fitted, sent to clients but not saved: the server's gates hold their own
    private @Nullable IrisView iris;
    // The plates across the opening while the iris is shut, which stop what walks into it; not saved, but put back
    private final List<IrisPlateEntity> plates = new ArrayList<>();
    // Whether redstone reached the base when last checked, and whether the base gives a signal out now
    private boolean powered;
    private boolean signal;
    // Whether the open wormhole's surface lets a little show through, sent to clients but not saved: the gate holds it
    private boolean transparent = true;
    // On clients, the open wormhole's surface, and the phase it was last ticked in
    private @Nullable EventHorizon horizon;
    private DialPhase horizonPhase = DialPhase.IDLE;

    public StargateBaseBlockEntity(final BlockPos pos, final BlockState state) {
        super(SGBlockEntityTypes.STARGATE_BASE, pos, state);
        this.gateType = state.getBlock() instanceof StargateBaseBlock base ? base.gateType() : GateTypes.MILKY_WAY;
    }

    /** @return the gate's type */
    public ResourceKey<GateType> gateType() {
        return this.gateType;
    }

    public void setGateType(final ResourceKey<GateType> gateType) {
        if (this.gateType.equals(gateType)) {
            return;
        }
        this.gateType = gateType;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    /** @return how big the gate is and how it is laid out; a standard gate until a supergate forms */
    public GateShape shape() {
        return this.shape;
    }

    public void setShape(final GateShape shape) {
        if (this.shape == shape) {
            return;
        }
        this.shape = shape;
        this.setChanged();
        this.sendUpdate();
    }

    /** @return whether the gate stands or lies flat, or did when it last formed */
    public GateOrientation orientation() {
        return this.orientation;
    }

    /** Stands the gate up or lays it flat, such as turning a flat gate face down; its iris plates go with it. */
    public void setOrientation(final GateOrientation orientation) {
        if (this.orientation == orientation) {
            return;
        }
        this.orientation = orientation;
        this.removePlates();
        this.setChanged();
        this.sendUpdate();
    }

    /** @return the gate's own frame, as its base faces and the gate lies */
    public GateFrame frame() {
        return new GateFrame(this.worldPosition, this.getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                this.orientation, this.shape);
    }

    /** @return the fluid pouring out of the gate, if any */
    public Optional<FluidSpill> spill() {
        return Optional.ofNullable(this.spill);
    }

    /** Shows fluid pouring out of the gate, or none; small changes aren't sent on, as they look the same. */
    public void setSpill(final @Nullable FluidSpill spill) {
        final boolean same = spill == null ? this.spill == null : this.spill != null
                && this.spill.fluid() == spill.fluid() && Math.abs(this.spill.amount() - spill.amount()) < 0.05F;
        if (!same) {
            this.spill = spill;
            this.sendUpdate();
        }
    }

    /** @return how many chevrons the gate has */
    public int chevrons() {
        return this.chevrons;
    }

    public void setChevrons(final int chevrons) {
        if (this.chevrons == chevrons) {
            return;
        }
        this.chevrons = chevrons;
        this.setChanged();
        this.sendUpdate();
    }

    /** @return what the gate is doing */
    public DialView dialView() {
        return this.dialView;
    }

    /** Shows what the gate is doing, for clients to draw. */
    public void setDialView(final DialView view) {
        if (this.dialView.equals(view)) {
            return;
        }
        this.dialView = view;
        this.sendUpdate();
    }

    /** @return whether the redstone reaching the base changed; remembers it */
    public boolean setPowered(final boolean powered) {
        if (this.powered == powered) {
            return false;
        }
        this.powered = powered;
        this.setChanged();
        return true;
    }

    /** @return whether the base gives out a redstone signal */
    public boolean signal() {
        return this.signal;
    }

    /** Sets whether the base gives out a redstone signal, telling the blocks round it if that changes. */
    public void setSignal(final boolean signal) {
        if (this.signal == signal) {
            return;
        }
        this.signal = signal;
        if (this.level != null) {
            this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
        }
    }

    /** @return whether the open wormhole's surface lets a little of what is behind it show through */
    public boolean transparent() {
        return this.transparent;
    }

    public void setTransparent(final boolean transparent) {
        if (this.transparent == transparent) {
            return;
        }
        this.transparent = transparent;
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    /** @return the gate's iris, if one is fitted */
    public @Nullable IrisView iris() {
        return this.iris;
    }

    /** Shows the gate's iris, or that it has none, for clients to draw. */
    public void setIris(final @Nullable IrisView iris) {
        if (java.util.Objects.equals(this.iris, iris)) {
            return;
        }
        this.iris = iris;
        this.sendUpdate();
    }

    /** @return the camouflage over the gate's bottom row, which a menu can show */
    public Container camouflage() {
        return this.camouflage;
    }

    /**
     * @param slot a slot, from 0 at the left of the bottom row as seen from in front to 4 at the right
     * @return the block drawn over that block of the bottom row, if any
     */
    public Optional<BlockState> camouflageState(final int slot) {
        final ItemStack stack = this.camouflage.getItem(slot);
        return stack.getItem() instanceof BlockItem item ? Optional.of(item.getBlock().defaultBlockState())
                : Optional.empty();
    }

    /** @return whether a stack can camouflage a gate: any block but a gate's or a DHD's own */
    public static boolean isCamouflage(final ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && !(item.getBlock() instanceof RingBlock
                || item.getBlock() instanceof StargateBaseBlock || item.getBlock() instanceof DhdBlock);
    }

    @Override
    public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
        this.removePlates();
        if (this.level != null) {
            Containers.dropContents(this.level, pos, this.camouflage);
            // The upgrades fitted to it, as in 1.12: a chevron upgrade for each chevron past seven, and the iris
            for (int i = StargateBaseBlockEntity.DEFAULT_CHEVRONS; i < this.chevrons; i++) {
                Containers.dropItemStack(this.level, pos.getX(), pos.getY(), pos.getZ(),
                        new ItemStack(SGItems.CHEVRON_UPGRADE));
            }
            if (this.iris != null) {
                Containers.dropItemStack(this.level, pos.getX(), pos.getY(), pos.getZ(),
                        new ItemStack(SGItems.IRIS_UPGRADE));
            }
        }
    }

    // The camouflage, one block to a slot; a change is saved and shown to clients at once
    private final class Camouflage extends SimpleContainer {

        Camouflage() {
            super(StargateBaseBlockEntity.CAMOUFLAGE_SLOTS);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean canPlaceItem(final int slot, final ItemStack stack) {
            return StargateBaseBlockEntity.isCamouflage(stack);
        }

        @Override
        public boolean stillValid(final Player player) {
            return Container.stillValidBlockEntity(StargateBaseBlockEntity.this, player);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            StargateBaseBlockEntity.this.setChanged();
            StargateBaseBlockEntity.this.sendUpdate();
        }
    }

    private void sendUpdate() {
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    /**
     * Makes sure the server knows of a formed gate, such as one formed before the server kept a list of gates, and
     * shows what it is doing.
     */
    public static void serverTick(final ServerLevel level, final BlockPos pos, final StargateBaseBlockEntity base) {
        StargateBaseBlockEntity.irisPlates(level, pos, base);
        if (!base.formed || level.getGameTime() % StargateBaseBlockEntity.CHECK_TICKS != 0) {
            return;
        }
        SGServer.current().ifPresent(server -> {
            final GlobalPos position = GlobalPos.of(level.dimension(), pos);
            final Optional<SGStargate> gate = server.stargates().gate(position);
            if (gate.isPresent()) {
                gate.get().sync();
            } else {
                GateFormation.register(level, pos, base);
            }
        });
    }

    // How shut the iris is when it starts stopping travelers, and so walkers too, and how big a plate of it is at most
    private static final float IRIS_STOPS = 0.5F;
    private static final double PLATE = 2.0;
    // Where the plates are through the gate: about the blades, a little in front of the horizon
    private static final double PLATE_DEPTH = 0.05;

    // Puts the iris's plates across the opening once it is half shut, as it starts stopping travelers, and takes
    // them away as it opens; a plate lost as its chunk unloads is put back
    private static void irisPlates(final ServerLevel level, final BlockPos pos, final StargateBaseBlockEntity base) {
        final IrisView iris = base.iris;
        final boolean shut = base.formed && iris != null && SGIris.aperture(iris.state(), iris.since(),
                level.getServer().overworld().getGameTime()) <= StargateBaseBlockEntity.IRIS_STOPS;
        final boolean standing = !base.plates.isEmpty() && base.plates.stream().noneMatch(Entity::isRemoved);
        if (shut == standing) {
            return;
        }
        base.removePlates();
        if (!shut) {
            return;
        }
        final GateShape shape = base.shape();
        final GateFrame frame = base.frame();
        final double width = 2 * shape.openingHalfWidth();
        final double height = shape.openingTop() - shape.openingFloor();
        final int across = (int) Math.ceil(width / StargateBaseBlockEntity.PLATE);
        final int up = (int) Math.ceil(height / StargateBaseBlockEntity.PLATE);
        for (int i = 0; i < across; i++) {
            for (int j = 0; j < up; j++) {
                final Vec3 middle = frame.toWorld(new Vec3(-shape.openingHalfWidth() + (i + 0.5) * width / across,
                        shape.openingFloor() + (j + 0.5) * height / up, StargateBaseBlockEntity.PLATE_DEPTH));
                final IrisPlateEntity plate = IrisPlateEntity.create(level, middle, frame.front(), frame.top(),
                        width / across / 2, height / up / 2, pos);
                level.addFreshEntity(plate);
                base.plates.add(plate);
            }
        }
    }

    private void removePlates() {
        this.plates.forEach(Entity::discard);
        this.plates.clear();
    }

    /**
     * Keeps the gate's sounds going, and moves an open wormhole's surface on by a tick, throwing it forward as it
     * opens and stirring it as it shuts. Only clients call this.
     */
    public static void clientTick(final Level level, final BlockPos pos, final StargateBaseBlockEntity base) {
        GateLoopSounds.tick(base);
        final DialPhase phase = base.dialView.phase();
        if (!StargateBaseBlockEntity.showsHorizon(phase)) {
            base.horizon = null;
            base.horizonPhase = phase;
            return;
        }
        if (base.horizon == null) {
            base.horizon = new EventHorizon();
        }
        if (phase != base.horizonPhase) {
            if (phase == DialPhase.TRANSIENT) {
                base.horizon.open(level.getRandom());
            } else if (phase == DialPhase.DISCONNECTING) {
                base.horizon.close(level.getRandom());
            }
            base.horizonPhase = phase;
        }
        base.horizon.tick(level.getRandom());
        if (base.spill != null && phase == DialPhase.CONNECTED) {
            base.spill.spray(level, base.frame(), level.getRandom());
        }
    }

    /** @return whether a gate shows its wormhole in this phase: from the kawoosh until it has shut down */
    public static boolean showsHorizon(final DialPhase phase) {
        return phase == DialPhase.TRANSIENT || phase == DialPhase.CONNECTED || phase == DialPhase.DISCONNECTING;
    }

    /** @return the open wormhole's surface, on a client while the gate shows it */
    public @Nullable EventHorizon horizon() {
        return this.horizon;
    }

    /** @return whether the ring round this base is complete and merged into a gate */
    public boolean isFormed() {
        return this.formed;
    }

    public void setFormed(final boolean formed) {
        if (this.formed == formed) {
            return;
        }
        this.formed = formed;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        this.formed = input.getBooleanOr("formed", false);
        input.read("gate_type", ResourceKey.codec(SGCraftRegistries.GATE_TYPE)).ifPresent(type -> this.gateType = type);
        this.chevrons = input.getIntOr("chevrons", StargateBaseBlockEntity.DEFAULT_CHEVRONS);
        this.shape = GateShape.ofRadius(input.getIntOr("radius", GateShape.STANDARD.radius()));
        this.orientation = input.read("orientation", GateOrientation.CODEC).orElse(GateOrientation.STANDING);
        this.spill = input.getString("spill_fluid").flatMap(id -> Optional.ofNullable(Identifier.tryParse(id)))
                .flatMap(BuiltInRegistries.FLUID::getOptional)
                .map(fluid -> new FluidSpill(fluid, input.getFloatOr("spill", 0))).orElse(null);
        this.powered = input.getBooleanOr("powered", false);
        this.transparent = input.getBooleanOr("transparent", true);
        this.dialView = input.read("dial", CompoundTag.CODEC).map(DialView::load).orElse(DialView.IDLE);
        this.iris = input.read("iris", CompoundTag.CODEC).flatMap(IrisView::load).orElse(null);
        final NonNullList<ItemStack> items = NonNullList.withSize(StargateBaseBlockEntity.CAMOUFLAGE_SLOTS,
                ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        for (int i = 0; i < items.size(); i++) {
            this.camouflage.getItems().set(i, items.get(i));
        }
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("formed", this.formed);
        output.store("gate_type", ResourceKey.codec(SGCraftRegistries.GATE_TYPE), this.gateType);
        output.putInt("chevrons", this.chevrons);
        output.putInt("radius", this.shape.radius());
        output.store("orientation", GateOrientation.CODEC, this.orientation);
        output.putBoolean("powered", this.powered);
        ContainerHelper.saveAllItems(output, this.camouflage.getItems());
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = this.saveCustomOnly(registries);
        tag.put("dial", this.dialView.save());
        tag.putBoolean("transparent", this.transparent);
        if (this.iris != null) {
            tag.put("iris", this.iris.save());
        }
        if (this.spill != null) {
            tag.putString("spill_fluid", BuiltInRegistries.FLUID.getKey(this.spill.fluid()).toString());
            tag.putFloat("spill", this.spill.amount());
        }
        return tag;
    }
}
