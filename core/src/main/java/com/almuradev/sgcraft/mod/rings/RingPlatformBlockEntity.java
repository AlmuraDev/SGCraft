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

package com.almuradev.sgcraft.mod.rings;

import com.almuradev.sgcraft.mod.block.entity.SGBlockEntityTypes;
import com.almuradev.sgcraft.mod.energy.FeRate;
import com.almuradev.sgcraft.mod.energy.FeSink;
import com.almuradev.sgcraft.mod.item.SGItems;
import com.almuradev.sgcraft.mod.obelisk.ConfigurableTransport;
import com.almuradev.sgcraft.mod.obelisk.ObeliskAccess;
import com.almuradev.sgcraft.mod.obelisk.TransportAccess;
import com.almuradev.sgcraft.mod.sound.SGSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * A ring platform, as 2.2.0's: the block in the floor, or in a ceiling, at the middle of its five rings. It holds the
 * platform it is linked to, in the same world; its gate energy, from cables and from the naquadah in its four fuel
 * slots, burned a piece at a time when a transport needs more than it holds; who owns it and who may use it; and,
 * while its rings run, how far along they are. Clients hear only when its rings set off and where they stack.
 */
public final class RingPlatformBlockEntity extends BlockEntity implements ConfigurableTransport, FeSink,
        WorldlyContainer {

    /** How many fuel slots it has. */
    public static final int SLOTS = 4;
    private static final int[] ALL_SLOTS = IntStream.range(0, RingPlatformBlockEntity.SLOTS).toArray();
    private static final long IDLE = Long.MIN_VALUE / 2;

    private final NonNullList<ItemStack> fuel = NonNullList.withSize(RingPlatformBlockEntity.SLOTS, ItemStack.EMPTY);
    private final TransportAccess access = new TransportAccess();
    private @Nullable BlockPos link;
    private double energy;
    // How many ticks its rings have run, or -1 while they rest, and whether it started them, and so swaps the two
    private int runTicks = -1;
    private boolean initiator;
    // When its rings set off, in game time, for clients to draw them; and the floor they stack on
    private long startTime = RingPlatformBlockEntity.IDLE;
    private double floorY;
    // What the loader hands cables, made once so a transaction always meets the same one
    private @Nullable Object handler;

    public RingPlatformBlockEntity(final BlockPos pos, final BlockState state) {
        super(SGBlockEntityTypes.RING_PLATFORM, pos, state);
    }

    // ---- Where its rings go ----

    /** @return whether its rings hang from it and drop to the floor: a ceiling platform, or any with air below it */
    public boolean ceiling() {
        if (this.getBlockState().getBlock() instanceof final RingPlatformBlock block && block.ceiling()) {
            return true;
        }
        return this.level != null && this.level.getBlockState(this.worldPosition.below()).isAir();
    }

    /**
     * @return the height its rings stack from: its own top in a floor; below a ceiling, the top of the first block
     *     under it that can be stood on, within {@link Rings#MAX_DROP} blocks
     */
    public double findFloor() {
        if (!this.ceiling() || this.level == null) {
            return this.worldPosition.getY() + 1;
        }
        for (int down = 1; down <= Rings.MAX_DROP; down++) {
            final BlockPos below = this.worldPosition.below(down);
            if (this.level.getBlockState(below).isFaceSturdy(this.level, below, Direction.UP)) {
                return below.getY() + 1;
            }
        }
        return this.worldPosition.getY() - Rings.MAX_DROP;
    }

    /** @return the height its rings stack from: while they run, where they set off for; otherwise where they would */
    public double floor() {
        return this.running() ? this.floorY : this.findFloor();
    }

    /** @return the space its rings close around */
    public AABB volume() {
        return Rings.volume(this.worldPosition, this.floor());
    }

    /** @return the middle of the floor inside its rings */
    public Vec3 floorCenter() {
        return new Vec3(this.worldPosition.getX() + 0.5, this.floor(), this.worldPosition.getZ() + 0.5);
    }

    /** @return the middle of the space inside its rings, about chest high */
    public Vec3 center() {
        return this.floorCenter().add(0, 1.2, 0);
    }

    // ---- Running ----

    /**
     * @return whether its rings are running. A platform whose chunk stops ticking partway, a far partner most often,
     *     is done once their time is up all the same, so it never stays busy
     */
    public boolean running() {
        return this.runTicks >= 0 && !this.overdue();
    }

    private boolean overdue() {
        return this.level != null && this.level.getGameTime() - this.startTime > Rings.DONE_TICKS;
    }

    /** @return when its rings last set off, in game time, for clients to draw them */
    public long startTime() {
        return this.startTime;
    }

    /** @return the floor its rings stack on while they run, as clients were told */
    public double runFloor() {
        return this.floorY;
    }

    void start(final boolean initiator) {
        this.runTicks = 0;
        this.initiator = initiator;
        this.floorY = this.findFloor();
        this.startTime = this.level == null ? 0 : this.level.getGameTime();
        if (this.level != null) {
            final Vec3 at = this.center();
            this.level.playSound(null, at.x, at.y, at.z, SGSounds.RING_TRANSPORT, SoundSource.BLOCKS, 1.5F, 1.0F);
        }
        this.sync();
    }

    /** Runs its rings on: the one that started them whitens both sets and swaps what is inside them. */
    public static void serverTick(final ServerLevel level, final BlockPos pos, final BlockState state,
            final RingPlatformBlockEntity platform) {
        if (platform.runTicks < 0) {
            return;
        }
        // Its chunk stopped ticking partway, and it is too late now to send anyone
        if (platform.overdue()) {
            platform.runTicks = -1;
            platform.setChanged();
            return;
        }
        final int ticks = ++platform.runTicks;
        if (platform.initiator && (ticks == Rings.TRANSPORT_TICK - Rings.WHITE_OUT_TICKS
                || ticks == Rings.TRANSPORT_TICK)) {
            Rings.partner(platform).ifPresent(partner -> {
                if (ticks == Rings.TRANSPORT_TICK) {
                    Rings.swap(platform, partner);
                } else {
                    Rings.whiteOut(platform);
                    Rings.whiteOut(partner);
                }
            });
        }
        if (ticks >= Rings.DONE_TICKS) {
            platform.runTicks = -1;
            platform.setChanged();
        }
    }

    // ---- Its link ----

    /** @return the platform it is linked to, if any */
    public Optional<BlockPos> linkPos() {
        return Optional.ofNullable(this.link);
    }

    void setLink(final @Nullable BlockPos link) {
        this.link = link == null ? null : link.immutable();
        this.setChanged();
    }

    @Override
    public Optional<GlobalPos> link() {
        return this.level == null || this.link == null ? Optional.empty()
                : Optional.of(GlobalPos.of(this.level.dimension(), this.link));
    }

    @Override
    public void unlinkBoth() {
        Rings.unlink(this);
    }

    // ---- Who may use it ----

    /** @return whether a player may start its rings */
    public boolean mayUse(final Player player) {
        return this.access.mayUse(player);
    }

    public void setOwner(final UUID owner, final String name) {
        this.access.setOwner(owner, name);
        this.setChanged();
    }

    @Override
    public TransportAccess transportAccess() {
        return this.access;
    }

    @Override
    public void setAccess(final boolean everyone, final List<ObeliskAccess> entries) {
        this.access.set(everyone, entries);
        this.setChanged();
    }

    @Override
    public void refuseConfigure(final ServerPlayer player) {
        Rings.tell(player, "no_configure", "Only these rings' owner or their admins may link or configure them",
                true);
    }

    // ---- Energy ----

    /** @return the most gate energy it holds from cables */
    public static double capacity() {
        return RingSettings.current().capacity();
    }

    @Override
    public double stored() {
        return this.energy;
    }

    /** Sets the gate energy it holds, never below none; for loaders undoing a transaction, and for commands. */
    @Override
    public void setStored(final double energy) {
        final double kept = Math.max(0, energy);
        if (kept != this.energy) {
            this.energy = kept;
            this.setChanged();
        }
    }

    @Override
    public long feAmount() {
        return FeRate.toFeIn(this.energy);
    }

    @Override
    public long feLimit() {
        return FeRate.toFeIn(RingPlatformBlockEntity.capacity());
    }

    @Override
    public long insertFe(final long fe, final boolean simulate) {
        final double room = Math.max(0, RingPlatformBlockEntity.capacity() - this.energy);
        final long taken = Math.max(0, Math.min(fe, FeRate.toFeIn(room)));
        if (!simulate && taken > 0) {
            this.setStored(this.energy + FeRate.toGeIn(taken));
        }
        return taken;
    }

    /**
     * The loader's handler for cables, made the first time it is asked for and kept, so every cable meets the same
     * one.
     */
    @SuppressWarnings("unchecked")
    public <T> T handler(final Function<RingPlatformBlockEntity, T> make) {
        if (this.handler == null) {
            this.handler = make.apply(this);
        }
        return (T) this.handler;
    }

    /** @return how many pieces of naquadah it holds to burn */
    @Override
    public int naquadah() {
        int count = 0;
        for (final ItemStack stack : this.fuel) {
            if (RingPlatformBlockEntity.isFuel(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /** @return all the gate energy it could give: what it holds, and what its naquadah would give */
    public double energyAvailable() {
        return this.energy + this.naquadah() * RingSettings.naquadahEnergy();
    }

    /** Takes a transport's cost, burning naquadah a piece at a time for what it lacks. */
    void pay(final double cost) {
        for (final ItemStack stack : this.fuel) {
            while (this.energy < cost && RingPlatformBlockEntity.isFuel(stack)) {
                stack.shrink(1);
                this.energy += RingSettings.naquadahEnergy();
            }
        }
        this.energy = Math.max(0, this.energy - cost);
        this.setChanged();
    }

    @Override
    public boolean powered() {
        return true;
    }

    @Override
    public long energy() {
        return (long) Math.floor(this.energy);
    }

    @Override
    public long energyCapacity() {
        return (long) RingPlatformBlockEntity.capacity();
    }

    @Override
    public long usesLeft() {
        final double cost = this.link == null ? RingSettings.current().energyPerUse()
                : RingSettings.current().cost(Math.sqrt(this.worldPosition.distSqr(this.link)));
        return cost <= 0 ? Long.MAX_VALUE : (long) (this.energyAvailable() / cost);
    }

    // ---- Its screen ----

    /** Shows its power and its fuel slots to a player. */
    public void open(final ServerPlayer player) {
        final ContainerData data = new ContainerData() {
            @Override
            public int get(final int index) {
                final RingPlatformBlockEntity platform = RingPlatformBlockEntity.this;
                return switch (index / RingPlatformMenu.PARTS) {
                    case 0 -> RingPlatformMenu.part(platform.energy(), index);
                    case 1 -> RingPlatformMenu.part(platform.energyCapacity(), index);
                    case 2 -> RingPlatformMenu.part(platform.usesLeft(), index);
                    default -> platform.link == null ? 0 : 1;
                };
            }

            @Override
            public void set(final int index, final int value) {
            }

            @Override
            public int getCount() {
                return RingPlatformMenu.DATA;
            }
        };
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new RingPlatformMenu(id, inventory, this, data),
                this.getBlockState().getBlock().getName()));
    }

    // ---- Its fuel slots: naquadah only, from its screen or from hoppers, which take nothing out ----

    /** @return whether a stack can go in its fuel slots */
    public static boolean isFuel(final ItemStack stack) {
        return stack.is(SGItems.NAQUADAH);
    }

    @Override
    public int getContainerSize() {
        return RingPlatformBlockEntity.SLOTS;
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
        return RingPlatformBlockEntity.isFuel(stack);
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
        return RingPlatformBlockEntity.ALL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(final int slot, final ItemStack stack, final @Nullable Direction side) {
        return this.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(final int slot, final ItemStack stack, final Direction side) {
        return false;
    }

    // Broken, it drops its naquadah and lets its partner go
    @Override
    public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
        if (this.level != null && !this.level.isClientSide()) {
            Containers.dropContents(this.level, pos, this);
            Rings.unlink(this);
        }
    }

    // ---- Saving and syncing ----

    private void sync() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(),
                    Block.UPDATE_CLIENTS);
        }
    }

    // Clients hear only when its rings set off and where they stack; the rest is the server's business
    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        tag.putLong("start_time", this.startTime);
        tag.putDouble("floor_y", this.floorY);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        this.fuel.clear();
        ContainerHelper.loadAllItems(input, this.fuel);
        this.access.load(input);
        this.link = input.read("link", BlockPos.CODEC).orElse(null);
        this.energy = input.getDoubleOr("energy", 0);
        this.runTicks = input.getIntOr("run_ticks", -1);
        this.initiator = input.getBooleanOr("initiator", false);
        this.startTime = input.getLongOr("start_time", RingPlatformBlockEntity.IDLE);
        this.floorY = input.getDoubleOr("floor_y", 0);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.fuel);
        this.access.save(output);
        if (this.link != null) {
            output.store("link", BlockPos.CODEC, this.link);
        }
        output.putDouble("energy", this.energy);
        output.putInt("run_ticks", this.runTicks);
        output.putBoolean("initiator", this.initiator);
        output.putLong("start_time", this.startTime);
        output.putDouble("floor_y", this.floorY);
    }
}
