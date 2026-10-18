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

import com.almuradev.sgcraft.mod.computer.ComputerPower;
import com.almuradev.sgcraft.mod.computer.PoweredComputer;
import com.almuradev.sgcraft.mod.malp.MalpLinks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * A MALP console's middle lower part. It keeps whether the console is switched on and has power, and the MALP last
 * chosen at the console, whose feed its monitor shows the players near it while no one is at it, and is there so
 * clients can draw the feed on the monitor. Clients are told whether it is on and has power.
 */
public final class MalpConsoleBlockEntity extends BlockEntity implements PoweredComputer {

    private @Nullable UUID shown;
    // Whether the feed was powered down at the console, which its monitor then keeps to with no one at it
    private boolean feedOff;
    private final ComputerPower power = new ComputerPower();

    public MalpConsoleBlockEntity(final BlockPos pos, final BlockState state) {
        super(SGBlockEntityTypes.MALP_CONSOLE, pos, state);
    }

    /** @return the MALP last chosen at the console, if one was */
    public @Nullable UUID shown() {
        return this.shown;
    }

    /** Remembers the MALP chosen at the console, for its monitor to show once no one is at it. */
    public void setShown(final @Nullable UUID malp) {
        if (!Objects.equals(this.shown, malp)) {
            this.shown = malp;
            this.setChanged();
        }
    }

    /** @return whether the feed was powered down at the console, and not powered up since */
    public boolean feedOff() {
        return this.feedOff;
    }

    /** Remembers whether the feed was powered down at the console. */
    public void setFeedOff(final boolean off) {
        if (this.feedOff != off) {
            this.feedOff = off;
            this.setChanged();
        }
    }

    @Override
    public ComputerPower power() {
        return this.power;
    }

    @Override
    public void setOn(final boolean on) {
        if (this.level == null) {
            return;
        }
        this.power.setOn(this.level, this.worldPosition, on);
        this.changed();
        if (!this.running() && this.level instanceof final ServerLevel level) {
            MalpLinks.switchedOff(level.getServer(), GlobalPos.of(level.dimension(), this.worldPosition));
        }
    }

    /**
     * Pays for the console's running, taking everyone at it off it if it loses power, and keeps its monitor up to
     * date.
     */
    public static void serverTick(final ServerLevel level, final BlockPos pos, final MalpConsoleBlockEntity console) {
        if (level.getGameTime() % ComputerPower.CHECK_TICKS == 0 && console.power.pay(level, pos)) {
            console.changed();
            if (!console.running()) {
                MalpLinks.switchedOff(level.getServer(), GlobalPos.of(level.dimension(), pos));
            }
        }
        MalpLinks.monitor(level, pos, console);
    }

    // Saves the console, and tells clients
    private void changed() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    // The monitor's feed stops for those near it when the console goes, or its chunk unloads
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level instanceof final ServerLevel level) {
            MalpLinks.monitorGone(level.getServer(), GlobalPos.of(level.dimension(), this.worldPosition));
        }
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        this.shown = input.read("shown", UUIDUtil.CODEC).orElse(null);
        this.feedOff = input.getBooleanOr("feed_off", false);
        this.power.load(input);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        if (this.shown != null) {
            output.store("shown", UUIDUtil.CODEC, this.shown);
        }
        output.putBoolean("feed_off", this.feedOff);
        this.power.save(output);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // Clients are told only whether it is on and has power
    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        this.power.write(tag);
        return tag;
    }
}
