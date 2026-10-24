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

package com.almuradev.sgcraft.mod.zpm;

import com.almuradev.sgcraft.mod.access.SGGateAccess;
import com.almuradev.sgcraft.mod.energy.GeStore;
import com.almuradev.sgcraft.mod.block.entity.SGBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * A ZPM console, interface cart or hub, as in the original: it holds ZPMs and gives their gate energy to the gates near
 * it, and to an energy converter against it, which turns it into FE for other mods. Its ZPMs keep
 * what is left in them when they are taken out or the block is broken. Drawing on a tainted ZPM sets it off.
 *
 * <p>It belongs to the player who put it down, who, with operators, alone links it to a gate (see {@link ZpmLinks});
 * anyone links a device put down before devices had owners.</p>
 */
public final class ZpmDeviceBlockEntity extends BlockEntity implements GeStore {

    private final SimpleContainer zpms;
    private @Nullable UUID owner;

    public ZpmDeviceBlockEntity(final BlockPos pos, final BlockState state) {
        super(SGBlockEntityTypes.ZPM_DEVICE, pos, state);
        this.zpms = new SimpleContainer(ZpmDeviceBlockEntity.device(state).slots()) {
            @Override
            public boolean stillValid(final Player player) {
                return Container.stillValidBlockEntity(ZpmDeviceBlockEntity.this, player);
            }

            @Override
            public boolean canPlaceItem(final int slot, final ItemStack stack) {
                return ZpmItem.is(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                ZpmDeviceBlockEntity.this.changed();
            }
        };
    }

    /** @return which device a block is */
    public static ZpmDevice device(final BlockState state) {
        return state.getBlock() instanceof final ZpmDeviceBlock block ? block.device() : ZpmDevice.HUB;
    }

    public ZpmDevice device() {
        return ZpmDeviceBlockEntity.device(this.getBlockState());
    }

    /** @return the device's ZPM slots */
    public Container zpms() {
        return this.zpms;
    }

    /** @return how many of its ZPMs still hold energy */
    public int loaded() {
        return Zpms.charged(this.zpms).size();
    }

    /** @return how much gate energy its ZPMs hold between them */
    public double stored() {
        return Zpms.energy(Zpms.charged(this.zpms));
    }

    /** @return how much whole gate energy it could give now; a hub's goes as much further as it holds ZPMs */
    public long available() {
        return (long) Math.floor(this.stored() * this.device().efficiency(this.loaded()));
    }

    /**
     * Gives gate energy from its ZPMs, evenly.
     *
     * @return how much it gave, or would give
     */
    public long draw(final long wanted, final boolean simulate) {
        final long given = Math.max(0, Math.min(wanted, this.available()));
        if (!simulate && given > 0) {
            this.drain((double) given / this.device().efficiency(this.loaded()));
        }
        return given;
    }

    @Override
    public double givable() {
        return this.stored() * this.device().efficiency(this.loaded());
    }

    @Override
    public double give(final double wanted) {
        final double given = Math.max(0, Math.min(wanted, this.givable()));
        if (given > 0) {
            this.drain(given / this.device().efficiency(this.loaded()));
        }
        return given;
    }

    // Takes gate energy from the ZPMs, and sets off a tainted one
    private void drain(final double energy) {
        final List<ItemStack> charged = Zpms.charged(this.zpms);
        final boolean tainted = charged.stream().anyMatch(ZpmItem::tainted);
        Zpms.drain(charged, energy);
        this.zpms.setChanged();
        if (tainted && this.level instanceof final ServerLevel level) {
            final float power = ZpmSettings.current().taintedExplosion();
            if (power > 0) {
                level.explode(null, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5,
                        this.worldPosition.getZ() + 0.5, power, true, Level.ExplosionInteraction.BLOCK);
            }
        }
    }

    // Saves the change, and shows on the block whether it holds ZPMs, and how many
    private void changed() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()
                && this.getBlockState().getBlock() instanceof final ZpmDeviceBlock block) {
            final BlockState shown = block.showing(this.getBlockState(), this.held());
            if (shown != this.getBlockState()) {
                this.level.setBlock(this.worldPosition, shown, 3);
            }
        }
    }

    // How many ZPMs it holds, charged or not
    private int held() {
        int held = 0;
        for (int slot = 0; slot < this.zpms.getContainerSize(); slot++) {
            if (!this.zpms.getItem(slot).isEmpty()) {
                held++;
            }
        }
        return held;
    }

    /** Shows its ZPMs and their energy to a player. */
    public void open(final ServerPlayer player) {
        final ContainerData data = new ContainerData() {
            @Override
            public int get(final int index) {
                if (index == ZpmMenu.LOADED) {
                    return ZpmDeviceBlockEntity.this.loaded();
                }
                return (int) ((long) ZpmDeviceBlockEntity.this.stored() >>> (16 * index) & 0xFFFF);
            }

            @Override
            public void set(final int index, final int value) {
            }

            @Override
            public int getCount() {
                return ZpmMenu.DATA;
            }
        };
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new ZpmMenu(id, inventory, this.zpms, data),
                this.getBlockState().getBlock().getName()));
    }

    @Override
    public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
        if (this.level != null) {
            Containers.dropContents(this.level, pos, this.zpms);
            if (this.level.getServer() != null) {
                ZpmLinks.unlink(this.level.getServer(), GlobalPos.of(this.level.dimension(), pos));
            }
        }
    }

    /** @return the player who put it down, if it has an owner */
    public @Nullable UUID owner() {
        return this.owner;
    }

    public void setOwner(final @Nullable UUID owner) {
        this.owner = owner;
        this.setChanged();
    }

    /** @return whether a player may link it to a gate, or unlink it: its owner or an operator, or anyone if unowned */
    public boolean mayLink(final Player player) {
        return this.owner == null || this.owner.equals(player.getUUID()) || SGGateAccess.isOperator(player);
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        this.owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        final NonNullList<ItemStack> items = NonNullList.withSize(this.zpms.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        for (int slot = 0; slot < items.size(); slot++) {
            this.zpms.setItem(slot, items.get(slot));
        }
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        final NonNullList<ItemStack> items = NonNullList.withSize(this.zpms.getContainerSize(), ItemStack.EMPTY);
        for (int slot = 0; slot < items.size(); slot++) {
            items.set(slot, this.zpms.getItem(slot));
        }
        ContainerHelper.saveAllItems(output, items);
        if (this.owner != null) {
            output.store("owner", UUIDUtil.CODEC, this.owner);
        }
    }
}
