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

package com.almuradev.sgcraft.mod.obelisk;

import com.almuradev.sgcraft.mod.block.entity.SGBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The bottom half of an Asgard obelisk: the obelisk it is linked to, who owns it and who may use and configure it, and
 * the players it is about to beam away. Clients hear only when its gem should shine.
 */
public final class AsgardObeliskBlockEntity extends BlockEntity implements ConfigurableTransport {

    private @Nullable GlobalPos link;
    private final TransportAccess access = new TransportAccess();
    // When it last started beaming someone, in game time, for its gem to shine
    private long beamStart = Long.MIN_VALUE / 2;
    // The players about to be beamed, and where each stood when they touched the gem
    private final List<Beam> beams = new ArrayList<>();

    // A player about to be beamed, where they stood when they touched the gem, and how many ticks they have waited
    private static final class Beam {

        private final ServerPlayer player;
        private final Vec3 at;
        private int ticks;

        Beam(final ServerPlayer player, final Vec3 at) {
            this.player = player;
            this.at = at;
        }
    }

    public AsgardObeliskBlockEntity(final BlockPos pos, final BlockState state) {
        super(SGBlockEntityTypes.ASGARD_OBELISK, pos, state);
    }

    /** @return its kind */
    public ObeliskKind kind() {
        return this.getBlockState().getBlock() instanceof final AsgardObeliskBlock block ? block.kind()
                : ObeliskKind.HAMMER;
    }

    /** @return the obelisk it is linked to, if any */
    @Override
    public Optional<GlobalPos> link() {
        return Optional.ofNullable(this.link);
    }

    void setLink(final @Nullable GlobalPos link) {
        this.link = link;
        this.setChanged();
    }

    /** @return its owner, if it has one */
    public Optional<UUID> owner() {
        return this.access.owner();
    }

    /** @return its owner's name when they placed it */
    public String ownerName() {
        return this.access.ownerName();
    }

    public void setOwner(final UUID owner, final String name) {
        this.access.setOwner(owner, name);
        this.setChanged();
    }

    /** @return whether anyone may use it, save those listed as not allowed */
    public boolean everyone() {
        return this.access.everyone();
    }

    /** @return the players on its list */
    public List<ObeliskAccess> access() {
        return this.access.entries();
    }

    @Override
    public TransportAccess transportAccess() {
        return this.access;
    }

    @Override
    public void setAccess(final boolean everyone, final List<ObeliskAccess> access) {
        this.access.set(everyone, access);
        this.setChanged();
    }

    /** @return whether a player may beam with it: its owner, an operator, or anyone the list or the default lets */
    public boolean mayUse(final Player player) {
        return this.access.mayUse(player);
    }

    @Override
    public void refuseConfigure(final ServerPlayer player) {
        Obelisks.tell(player, "no_configure", "You aren't allowed to configure this obelisk", true);
    }

    @Override
    public void unlinkBoth() {
        Obelisks.unlinkPartner(this);
    }

    // ---- Beaming ----

    /** @return whether a player is already about to be beamed by it */
    boolean beaming(final ServerPlayer player) {
        return this.beams.stream().anyMatch(beam -> beam.player == player);
    }

    void startBeam(final ServerPlayer player) {
        final long now = this.level == null ? 0 : this.level.getGameTime();
        this.beams.add(new Beam(player, player.position()));
        this.beamStart = now;
        this.sync();
    }

    // Each player still on their spot is beamed once the wait is over; one who moved off it is let go
    public void serverTick(final ServerLevel level) {
        if (this.beams.isEmpty()) {
            return;
        }
        final Iterator<Beam> each = this.beams.iterator();
        boolean stopped = false;
        while (each.hasNext()) {
            final Beam beam = each.next();
            if (beam.player.isRemoved() || beam.player.hasDisconnected()) {
                each.remove();
                stopped = true;
            } else if (beam.player.position().distanceToSqr(beam.at) > Obelisks.MAX_DRIFT * Obelisks.MAX_DRIFT) {
                each.remove();
                stopped = true;
                Obelisks.cancel(beam.player);
            } else if (++beam.ticks >= Obelisks.BEAM_TICKS) {
                each.remove();
                Obelisks.beam(beam.player, this);
            }
        }
        if (stopped && this.beams.isEmpty()) {
            this.beamStart = Long.MIN_VALUE / 2;
            this.sync();
        }
    }

    /** @return how brightly its gem shines now, from 0 to 1: building up to the beam, then dying away */
    public float shine(final float partialTicks) {
        if (this.level == null) {
            return 0;
        }
        final double ticks = this.level.getGameTime() + partialTicks - this.beamStart;
        final double t = ticks / Obelisks.BEAM_TICKS;
        if (ticks < 0 || t > 1.6) {
            return 0;
        }
        if (t > 1) {
            return (float) (1 - (t - 1) / 0.6);
        }
        final double lead = 15;
        final double u = (ticks + lead) / (Obelisks.BEAM_TICKS + lead);
        return (float) (u * u);
    }

    private void sync() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(),
                    Block.UPDATE_CLIENTS);
        }
    }

    // Clients hear only when its gem should shine; who may use it is the server's business
    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        tag.putLong("beam_start", this.beamStart);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        this.beamStart = input.getLongOr("beam_start", Long.MIN_VALUE / 2);
        this.link = input.read("link", GlobalPos.CODEC).orElse(null);
        this.access.load(input);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        if (this.link != null) {
            output.store("link", GlobalPos.CODEC, this.link);
        }
        this.access.save(output);
    }
}
