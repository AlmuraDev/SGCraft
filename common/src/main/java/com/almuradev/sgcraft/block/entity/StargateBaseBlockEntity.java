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
import com.almuradev.sgcraft.block.StargateBaseBlock;
import com.almuradev.sgcraft.client.gate.GateLoopSounds;
import com.almuradev.sgcraft.gate.DialPhase;
import com.almuradev.sgcraft.gate.DialView;
import com.almuradev.sgcraft.gate.EventHorizon;
import com.almuradev.sgcraft.gate.GateFormation;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.SGStargate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * The state of a gate, held by its base block.
 */
public final class StargateBaseBlockEntity extends BlockEntity {


    // How many chevrons a gate has when it first forms
    private static final int DEFAULT_CHEVRONS = 7;
    // How often a formed base checks the server knows of its gate and shows what the gate is doing
    private static final int CHECK_TICKS = 20;

    private boolean formed;
    private ResourceKey<GateType> gateType;
    private int chevrons = StargateBaseBlockEntity.DEFAULT_CHEVRONS;
    // What the gate is doing, sent to clients but not saved: the server's gates are idle when it starts
    private DialView dialView = DialView.IDLE;
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
        if (!base.formed || level.getGameTime() % StargateBaseBlockEntity.CHECK_TICKS != 0) {
            return;
        }
        SGServer.current().ifPresent(server -> {
            final GlobalPos position = GlobalPos.of(level.dimension(), pos);
            final Optional<SGStargate> gate = server.stargates().gate(position);
            if (gate.isPresent()) {
                gate.get().sync();
            } else {
                GateFormation.register(level, pos, base,
                        base.getBlockState().getValue(HorizontalDirectionalBlock.FACING));
            }
        });
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
        this.dialView = input.read("dial", CompoundTag.CODEC).map(DialView::load).orElse(DialView.IDLE);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("formed", this.formed);
        output.store("gate_type", ResourceKey.codec(SGCraftRegistries.GATE_TYPE), this.gateType);
        output.putInt("chevrons", this.chevrons);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = this.saveCustomOnly(registries);
        tag.put("dial", this.dialView.save());
        return tag;
    }
}
