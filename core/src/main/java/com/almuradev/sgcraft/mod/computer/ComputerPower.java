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

package com.almuradev.sgcraft.mod.computer;

import com.almuradev.sgcraft.mod.block.entity.PowerUnitBlockEntity;
import com.almuradev.sgcraft.mod.energy.GeStore;
import com.almuradev.sgcraft.mod.zpm.ZpmDeviceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Whether a computer, a dialing computer or a MALP console, is switched on, and whether it has the power to run. A
 * computer switched on pays for running once a second, in gate energy, from the power units within the server's
 * computer power reach of it, nearest first, and then from the ZPM consoles, interface carts and hubs within that
 * reach, nearest first. While they can't pay, it has no power, and goes dark as though switched off, until they can
 * again. With the server's settings saying computers need no power, or the computer's own setting, which its owner
 * and operators change with a configurator, a computer switched on always runs.
 *
 * <p>The computer belongs to the player who placed it, if anyone did. The computer keeps this in its block entity,
 * which saves it, and tells clients whether it is on and powered.</p>
 */
public final class ComputerPower {

    /** How often, in ticks, a computer pays for running. */
    public static final int CHECK_TICKS = 20;

    private boolean on = true;
    private boolean powered;
    private boolean requiresPower = true;
    private @Nullable UUID owner;

    /** @return whether the computer is switched on */
    public boolean on() {
        return this.on;
    }

    /** @return whether the computer had the power to run when it last paid, or needs none */
    public boolean powered() {
        return this.powered;
    }

    /** @return whether the computer's own setting says it needs power; the server's settings may say none does */
    public boolean requiresPower() {
        return this.requiresPower;
    }

    /** Sets whether the computer needs power; it pays for running at once, if switched on. */
    public void setRequiresPower(final Level level, final BlockPos at, final boolean requires) {
        this.requiresPower = requires;
        this.pay(level, at);
    }

    /** @return the player who placed the computer, if anyone did */
    public @Nullable UUID owner() {
        return this.owner;
    }

    /** Sets who the computer belongs to. */
    public void setOwner(final @Nullable UUID owner) {
        this.owner = owner;
    }

    /** @return whether the computer runs: switched on, with power */
    public boolean running() {
        return this.on && this.powered;
    }

    /** Switches the computer on or off; it pays for running at once when switched on. */
    public void setOn(final Level level, final BlockPos at, final boolean on) {
        this.on = on;
        if (on) {
            this.pay(level, at);
        }
    }

    /**
     * Pays for a second's running, if the computer is switched on, and works out whether it has power.
     *
     * @return whether it has power now, where it had none before, or the other way round
     */
    public boolean pay(final Level level, final BlockPos at) {
        if (!this.on) {
            return false;
        }
        final ComputerSettings settings = ComputerSettings.current();
        final boolean was = this.powered;
        final double due = settings.energyPerTick() * ComputerPower.CHECK_TICKS;
        if (!settings.requiresPower() || !this.requiresPower || due <= 0) {
            this.powered = true;
        } else {
            final List<GeStore> stores = ComputerPower.stores(level, at, settings.powerReach());
            double givable = 0;
            for (final GeStore store : stores) {
                givable += store.givable();
            }
            this.powered = givable >= due;
            if (this.powered) {
                double left = due;
                for (final GeStore store : stores) {
                    if (left <= 0) {
                        break;
                    }
                    left -= store.give(left);
                }
            }
        }
        return this.powered != was;
    }

    // The power units within reach of a computer, nearest first, and then the ZPM blocks holding ZPMs, nearest first
    private static List<GeStore> stores(final Level level, final BlockPos at, final int reach) {
        final List<BlockEntity> units = new ArrayList<>();
        final List<BlockEntity> zpms = new ArrayList<>();
        for (final BlockPos pos : BlockPos.betweenClosed(at.offset(-reach, -reach, -reach),
                at.offset(reach, reach, reach))) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            final BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof PowerUnitBlockEntity) {
                units.add(entity);
            } else if (entity instanceof final ZpmDeviceBlockEntity device && device.loaded() > 0) {
                zpms.add(entity);
            }
        }
        final Comparator<BlockEntity> nearest = Comparator.comparingDouble(entity -> entity.getBlockPos().distSqr(at));
        units.sort(nearest);
        zpms.sort(nearest);
        final List<GeStore> stores = new ArrayList<>();
        for (final BlockEntity entity : units) {
            stores.add((GeStore) entity);
        }
        for (final BlockEntity entity : zpms) {
            stores.add((GeStore) entity);
        }
        return stores;
    }

    /** Reads what was saved, or what clients were told. */
    public void load(final ValueInput input) {
        this.on = input.getBooleanOr("on", true);
        this.powered = input.getBooleanOr("powered", false);
        this.requiresPower = input.getBooleanOr("requires_power", true);
        this.owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
    }

    /** Saves it. */
    public void save(final ValueOutput output) {
        output.putBoolean("on", this.on);
        output.putBoolean("powered", this.powered);
        output.putBoolean("requires_power", this.requiresPower);
        if (this.owner != null) {
            output.store("owner", UUIDUtil.CODEC, this.owner);
        }
    }

    /** Tells clients whether the computer is on and powered. */
    public void write(final CompoundTag tag) {
        tag.putBoolean("on", this.on);
        tag.putBoolean("powered", this.powered);
    }
}
