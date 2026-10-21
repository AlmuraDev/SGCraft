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

package com.almuradev.sgcraft.mod.shield;

import com.almuradev.sgcraft.api.energy.EnergySource;
import com.almuradev.sgcraft.mod.block.entity.PowerUnitBlockEntity;
import com.almuradev.sgcraft.mod.block.entity.SGBlockEntityTypes;
import com.almuradev.sgcraft.mod.sound.SGSounds;
import com.almuradev.sgcraft.mod.zpm.ZpmDeviceBlockEntity;
import com.almuradev.sgcraft.mod.zpm.ZpmItem;
import com.almuradev.sgcraft.mod.zpm.ZpmSettings;
import com.almuradev.sgcraft.mod.zpm.Zpms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A city shield generator, as in SGCraft 2.2.0, run on energy from the power units round it in place of ZPMs: once
 * raised, a dome of energy stands round it, reaching {@link #width} out all round, {@link #above} up and
 * {@link #below} down. Nothing gets in while it stands: things outside are pushed back as they reach it, and shots are
 * stopped. Things inside can leave. An explosion outside it doesn't touch anything inside.
 *
 * <p>It takes energy each tick for every million blocks it holds, and falls when the power units run dry. Raising it
 * needs enough for ten seconds. While it stands, it can keep the chunks under it loaded, so it keeps standing with
 * nobody near; the ticket that does so is saved with the world, so it carries on across restarts.</p>
 */
public final class CityShieldBlockEntity extends BlockEntity {

    /** The ticket that keeps a raised shield's chunks loaded and running, until it falls. */
    public static final TicketType TICKET = new TicketType(0L, TicketType.FLAG_PERSIST | TicketType.FLAG_LOADING
            | TicketType.FLAG_SIMULATION | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    // How many chunks beyond the shield's own the ticket reaches, so entities in all of them run
    private static final int TICKET_MARGIN = 2;
    // How many ticks of energy raising the shield needs
    private static final int RAISE_TICKS = 200;
    // How far round the shield things are looked at, past it, and the furthest a thing can move in a tick and still
    // be stopped, as further is a teleport
    private static final double LOOK_PAST = 2;
    private static final double MOST_MOVED = 10;
    // How loud the shield rising and falling is: heard about 64 blocks away
    private static final float VOLUME = 4.0F;

    private boolean active;
    private int width = 32;
    private int above = 24;
    private int below = 8;
    private @Nullable UUID owner;
    // The part of a unit of energy not paid yet, paid once the fractions add up to a whole unit
    private double owed;
    // The radius of the chunk ticket it holds, in chunks, or 0 for none
    private int ticket;
    // How far up the shield is drawn, from 0 down to 1 standing; clients only
    private float spread;
    // Where each thing near the raised shield was last tick, by its UUID
    private Map<UUID, Vec3> lastSeen = new HashMap<>();
    // The generator's own ZPMs, which the shield draws on beside the power units and ZPM devices round it
    private final SimpleContainer zpms = new SimpleContainer(CityShieldMenu.SLOTS) {
        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean canPlaceItem(final int slot, final ItemStack stack) {
            return ZpmItem.is(stack);
        }

        @Override
        public boolean stillValid(final Player player) {
            return Container.stillValidBlockEntity(CityShieldBlockEntity.this, player);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            CityShieldBlockEntity.this.setChanged();
        }
    };

    public CityShieldBlockEntity(final BlockPos pos, final BlockState state) {
        super(SGBlockEntityTypes.CITY_SHIELD, pos, state);
    }

    /** @return whether the shield is raised */
    public boolean active() {
        return this.active;
    }

    public int width() {
        return this.width;
    }

    public int above() {
        return this.above;
    }

    public int below() {
        return this.below;
    }

    /** @return the player who placed the generator, if one did */
    public Optional<UUID> owner() {
        return Optional.ofNullable(this.owner);
    }

    void setOwner(final UUID owner) {
        this.owner = owner;
        this.setChanged();
    }

    /** @return how far up the shield is drawn, from 0 to 1; clients only */
    public float spread() {
        return this.spread;
    }

    /** @return the middle of the dome: the middle of the generator */
    public Vec3 center() {
        return Vec3.atCenterOf(this.worldPosition);
    }

    /** @return whether a point is inside the dome */
    public boolean inside(final Vec3 point) {
        return CityShieldBlockEntity.inside(this.center(), this.width, this.above, this.below, point);
    }

    /** @return whether a point is inside a dome reaching so far each way from its middle */
    public static boolean inside(final Vec3 center, final int width, final int above, final int below,
            final Vec3 point) {
        final double dy = point.y - center.y;
        final double y = dy / (dy >= 0 ? above : below);
        final double x = (point.x - center.x) / width;
        final double z = (point.z - center.z) / width;
        return x * x + y * y + z * z <= 1;
    }

    /** @return how many blocks a dome of that size holds */
    public static double volume(final int width, final int above, final int below) {
        return 2.0 / 3.0 * Math.PI * width * width * (above + below);
    }

    /** @return how much energy a dome of that size takes each tick */
    public static double energyPerTick(final int width, final int above, final int below) {
        return CityShieldBlockEntity.volume(width, above, below) / 1_000_000
                * ShieldSettings.current().cityEnergyPerMillionBlocks();
    }

    /** @return how much energy the shield takes each tick, at its size */
    public double energyPerTick() {
        return CityShieldBlockEntity.energyPerTick(this.width, this.above, this.below);
    }

    /** @return the power units the generator draws from */
    public List<PowerUnitBlockEntity> powerUnits() {
        final List<PowerUnitBlockEntity> units = new ArrayList<>();
        if (this.level == null) {
            return units;
        }
        final int reach = ShieldSettings.current().cityPowerReach();
        for (final BlockPos pos : BlockPos.betweenClosed(this.worldPosition.offset(-reach, -reach, -reach),
                this.worldPosition.offset(reach, reach, reach))) {
            if (this.level.isLoaded(pos) && this.level.getBlockEntity(pos) instanceof final PowerUnitBlockEntity unit) {
                units.add(unit);
            }
        }
        units.sort(Comparator.comparingDouble(unit -> unit.getBlockPos().distSqr(this.worldPosition)));
        return units;
    }

    /** @return the generator's own ZPM slots */
    public Container zpms() {
        return this.zpms;
    }

    /**
     * @return what the shield draws on, the nearest first: the ZPMs in its own slots, the power units round the
     *     generator, and the ZPM consoles, interface carts and hubs within the server's ZPM search range of it
     */
    public List<EnergySource> sources() {
        final List<EnergySource> sources = new ArrayList<>();
        if (!Zpms.charged(this.zpms).isEmpty()) {
            sources.add(new EnergySource() {
                @Override
                public long available() {
                    return Zpms.available(Zpms.charged(CityShieldBlockEntity.this.zpms));
                }

                @Override
                public long draw(final long wanted, final boolean simulate) {
                    if (simulate) {
                        return Math.max(0, Math.min(wanted, this.available()));
                    }
                    final long given = Zpms.drawEnergy(Zpms.charged(CityShieldBlockEntity.this.zpms), wanted);
                    CityShieldBlockEntity.this.zpms.setChanged();
                    return given;
                }
            });
        }
        for (final PowerUnitBlockEntity unit : this.powerUnits()) {
            sources.add(new EnergySource() {
                @Override
                public long available() {
                    return unit.available();
                }

                @Override
                public long draw(final long wanted, final boolean simulate) {
                    return unit.draw(wanted, simulate);
                }
            });
        }
        if (this.level != null) {
            final int reach = ZpmSettings.current().searchRange();
            final List<ZpmDeviceBlockEntity> devices = new ArrayList<>();
            for (final BlockPos pos : BlockPos.betweenClosed(this.worldPosition.offset(-reach, -reach, -reach),
                    this.worldPosition.offset(reach, reach, reach))) {
                if (this.level.isLoaded(pos) && this.level.getBlockEntity(pos)
                        instanceof final ZpmDeviceBlockEntity device && device.loaded() > 0) {
                    devices.add(device);
                }
            }
            devices.sort(Comparator.comparingDouble(device -> device.getBlockPos().distSqr(this.worldPosition)));
            for (final ZpmDeviceBlockEntity device : devices) {
                sources.add(new EnergySource() {
                    @Override
                    public long available() {
                        return device.available();
                    }

                    @Override
                    public long draw(final long wanted, final boolean simulate) {
                        return device.draw(wanted, simulate);
                    }
                });
            }
        }
        return sources;
    }

    /** @return how much energy the shield could draw on now */
    public long available() {
        long available = 0;
        for (final EnergySource source : this.sources()) {
            available += source.available();
        }
        return available;
    }

    /**
     * Sets how far the shield reaches each way, each kept within the server's limits. A raised shield changes size
     * at once.
     */
    public void resize(final int width, final int above, final int below) {
        final int most = ShieldSettings.current().cityMaxRadius();
        this.width = Math.max(ShieldSettings.CITY_MIN_RADIUS, Math.min(most, width));
        this.above = Math.max(ShieldSettings.CITY_MIN_RADIUS, Math.min(most, above));
        this.below = Math.max(ShieldSettings.CITY_MIN_RADIUS, Math.min(most, below));
        if (this.active) {
            this.holdChunks();
        }
        this.changed();
    }

    /**
     * Raises the shield, if there is enough energy around the generator for it to stand ten seconds.
     *
     * @return whether it was raised, or already was
     */
    public boolean raise() {
        if (this.active) {
            return true;
        }
        if (this.available() < Math.ceil(this.energyPerTick() * CityShieldBlockEntity.RAISE_TICKS)) {
            return false;
        }
        this.active = true;
        this.holdChunks();
        this.play(SGSounds.SHIELD_UP);
        CityShields.raised(this);
        this.changed();
        return true;
    }

    /** Lowers the shield. */
    public void lower() {
        if (!this.active) {
            return;
        }
        this.active = false;
        // Where things were before it fell means nothing once it rises again, and would pull back anyone who has
        // walked in since
        this.lastSeen = new HashMap<>();
        this.releaseChunks();
        this.play(SGSounds.SHIELD_DOWN);
        CityShields.lowered(this);
        this.changed();
    }

    /** Keeps a raised shield up: pays for it, and pushes back whatever reaches it from outside. */
    public static void serverTick(final ServerLevel level, final BlockPos pos, final CityShieldBlockEntity shield) {
        if (!shield.active) {
            return;
        }
        CityShields.raised(shield);
        if (!shield.pay()) {
            shield.lower();
            return;
        }
        if (shield.ticket == 0 && ShieldSettings.current().cityKeepsChunksLoaded()) {
            shield.holdChunks();
        } else if (shield.ticket != 0 && !ShieldSettings.current().cityKeepsChunksLoaded()) {
            shield.releaseChunks();
        }
        shield.stopEntering(level);
    }

    /** Moves the shield's drawing on: it spreads up and over as it rises, and draws back as it falls. */
    public static void clientTick(final CityShieldBlockEntity shield) {
        final float step = 1.0F / Math.max(100, 100 * Math.max(shield.above, shield.below) / 128);
        shield.spread = shield.active ? Math.min(1, shield.spread + step) : Math.max(0, shield.spread - step);
    }

    // Takes the tick's energy, a whole unit at a time as its fractions add up, from the nearest source to the furthest
    private boolean pay() {
        this.owed += this.energyPerTick();
        final long due = (long) Math.floor(this.owed);
        if (due <= 0) {
            return true;
        }
        final List<EnergySource> sources = this.sources();
        long available = 0;
        for (final EnergySource source : sources) {
            available += source.available();
        }
        if (available < due) {
            return false;
        }
        long left = due;
        for (final EnergySource source : sources) {
            if (left <= 0) {
                break;
            }
            left -= source.draw(left, false);
        }
        this.owed -= due - left;
        return left <= 0;
    }

    // Pushes back whatever has just reached the shield from outside, and stops shots
    private void stopEntering(final ServerLevel level) {
        final Vec3 center = this.center();
        final AABB around = new AABB(center.x - this.width - CityShieldBlockEntity.LOOK_PAST,
                center.y - this.below - CityShieldBlockEntity.LOOK_PAST,
                center.z - this.width - CityShieldBlockEntity.LOOK_PAST,
                center.x + this.width + CityShieldBlockEntity.LOOK_PAST,
                center.y + this.above + CityShieldBlockEntity.LOOK_PAST,
                center.z + this.width + CityShieldBlockEntity.LOOK_PAST);
        // Where each thing was last tick, by the shield's own reckoning, as players' last positions aren't kept on
        // the server
        final Map<UUID, Vec3> seen = new HashMap<>();
        for (final Entity entity : level.getEntities((Entity) null, around, Entity::isAlive)) {
            if (entity.isPassenger()) {
                continue;
            }
            final Vec3 feet = entity.position();
            final Vec3 last = this.lastSeen.getOrDefault(entity.getUUID(), new Vec3(entity.xo, entity.yo, entity.zo));
            seen.put(entity.getUUID(), feet);
            final double middle = entity.getBbHeight() / 2;
            final Vec3 now = feet.add(0, middle, 0);
            final Vec3 before = last.add(0, middle, 0);
            if (!this.inside(now) || this.inside(before) || now.distanceTo(before) > CityShieldBlockEntity.MOST_MOVED) {
                continue;
            }
            if (entity instanceof Projectile) {
                entity.discard();
                continue;
            }
            final Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x * -0.5, Math.min(motion.y, 0) * -0.5, motion.z * -0.5);
            entity.resetFallDistance();
            if (entity instanceof final ServerPlayer player) {
                player.connection.teleport(last.x, last.y, last.z, entity.getYRot(), entity.getXRot());
            } else {
                entity.teleportTo(last.x, last.y, last.z);
            }
            entity.needsSync = true;
            seen.put(entity.getUUID(), last);
        }
        this.lastSeen = seen;
    }

    // Keeps the chunks under the shield loaded with one ticket at the generator, reaching past the shield's width
    private void holdChunks() {
        if (!(this.level instanceof final ServerLevel level)) {
            return;
        }
        final int radius = ShieldSettings.current().cityKeepsChunksLoaded()
                ? (this.width + 15) / 16 + CityShieldBlockEntity.TICKET_MARGIN : 0;
        if (radius == this.ticket) {
            return;
        }
        this.releaseChunks();
        if (radius > 0) {
            level.getChunkSource().addTicketWithRadius(CityShieldBlockEntity.TICKET,
                    ChunkPos.containing(this.worldPosition), radius);
            this.ticket = radius;
            this.setChanged();
        }
    }

    private void releaseChunks() {
        if (this.ticket > 0 && this.level instanceof final ServerLevel level) {
            level.getChunkSource().removeTicketWithRadius(CityShieldBlockEntity.TICKET,
                    ChunkPos.containing(this.worldPosition), this.ticket);
        }
        if (this.ticket != 0) {
            this.ticket = 0;
            this.setChanged();
        }
    }

    private void play(final SoundEvent sound) {
        if (this.level != null) {
            final Vec3 center = this.center();
            this.level.playSound(null, center.x, center.y, center.z, sound, SoundSource.BLOCKS,
                    CityShieldBlockEntity.VOLUME, 1.0F);
        }
    }

    // Saves the change and shows it to clients
    private void changed() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
        this.releaseChunks();
        CityShields.lowered(this);
        if (this.level != null) {
            Containers.dropContents(this.level, pos, this.zpms);
        }
    }

    /** Opens the generator's screen for a player: its ZPM slots, and its shield's size and state beside them. */
    public void open(final ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new CityShieldMenu(id, inventory, this.zpms),
                this.getBlockState().getBlock().getName()));
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        CityShields.lowered(this);
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        this.active = input.getBooleanOr("active", false);
        this.width = input.getIntOr("width", 32);
        this.above = input.getIntOr("above", 24);
        this.below = input.getIntOr("below", 8);
        this.owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        this.owed = input.getDoubleOr("owed", 0);
        this.ticket = input.getIntOr("ticket", 0);
        final NonNullList<ItemStack> items = NonNullList.withSize(CityShieldMenu.SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        for (int slot = 0; slot < items.size(); slot++) {
            this.zpms.getItems().set(slot, items.get(slot));
        }
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("active", this.active);
        output.putInt("width", this.width);
        output.putInt("above", this.above);
        output.putInt("below", this.below);
        if (this.owner != null) {
            output.store("owner", UUIDUtil.CODEC, this.owner);
        }
        output.putDouble("owed", this.owed);
        output.putInt("ticket", this.ticket);
        ContainerHelper.saveAllItems(output, this.zpms.getItems());
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }
}
