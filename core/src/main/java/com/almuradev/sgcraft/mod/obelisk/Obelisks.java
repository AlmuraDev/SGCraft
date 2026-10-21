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

import com.almuradev.sgcraft.mod.network.SGNetwork;
import com.almuradev.sgcraft.mod.sound.SGSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Linking Asgard obelisks in pairs, and beaming players between them, as in 2.2.0. Beaming is free and reaches
 * across worlds. The player touches the gem and must stand still while the beam comes, two and a half seconds later,
 * their screen going white; then they arrive two blocks out in front of the other obelisk, facing it.
 */
public final class Obelisks {

    /** How long after the gem is touched the beam comes, in ticks. */
    public static final int BEAM_TICKS = 50;
    /** How far a player may drift from where they stood before the beam is called off, in blocks. */
    public static final double MAX_DRIFT = 0.05;
    // How near the gem a player must be, how far out from the far obelisk they arrive, and how long after arriving
    // they can't beam again, in ticks
    private static final double REACH = 4;
    private static final int ARRIVE = 2;
    private static final int COOLDOWN_TICKS = 30;

    // The obelisk each player started linking from, kept by place, as its chunk may unload before the next click
    private static final Map<UUID, GlobalPos> LINKING = new ConcurrentHashMap<>();
    // When each player last arrived, in server ticks
    private static final Map<UUID, Integer> ARRIVED = new ConcurrentHashMap<>();

    private Obelisks() {
    }

    // ---- Linking ----

    /**
     * The first obelisk a player sneak-uses is remembered; sneak-using a second of the same kind links the two, each to
     * the other. Linking sends everyone who uses either somewhere new, so it is for those who may configure both.
     */
    public static void link(final ServerPlayer player, final AsgardObeliskBlockEntity obelisk) {
        if (!obelisk.mayConfigure(player)) {
            Obelisks.tell(player, "no_configure", "You aren't allowed to configure this obelisk", true);
            return;
        }
        final GlobalPos here = Obelisks.place(obelisk);
        final GlobalPos start = Obelisks.LINKING.remove(player.getUUID());
        if (here.equals(start)) {
            Obelisks.tell(player, "link_canceled", "Linking canceled", false);
            return;
        }
        final AsgardObeliskBlockEntity first = start == null ? null
                : Obelisks.at(player.level().getServer(), start).orElse(null);
        if (first != null && first.kind() != obelisk.kind()) {
            Obelisks.LINKING.put(player.getUUID(), start);
            Obelisks.tell(player, "wrong_kind", "An obelisk links only with one of its own kind", true);
        } else if (first != null && !first.mayConfigure(player)) {
            Obelisks.tell(player, "no_configure", "You aren't allowed to configure this obelisk", true);
        } else if (first != null) {
            Obelisks.unlinkPartner(first);
            Obelisks.unlinkPartner(obelisk);
            first.setLink(here);
            obelisk.setLink(Obelisks.place(first));
            Obelisks.tell(player, "linked", "The obelisks are linked", false);
        } else {
            Obelisks.LINKING.put(player.getUUID(), here);
            Obelisks.tell(player, "link_start", "Now sneak-use another obelisk of this kind to link the two, or this "
                    + "one again to cancel", false);
        }
    }

    /** Unlinks an obelisk, and the one it was linked to if that one is linked back. */
    static void unlinkPartner(final AsgardObeliskBlockEntity obelisk) {
        Obelisks.partner(obelisk).ifPresent(partner -> partner.setLink(null));
        obelisk.setLink(null);
    }

    // The obelisk this one is linked to, if it is still there and linked back
    static Optional<AsgardObeliskBlockEntity> partner(final AsgardObeliskBlockEntity obelisk) {
        final MinecraftServer server = obelisk.getLevel() == null ? null : obelisk.getLevel().getServer();
        if (server == null || obelisk.link().isEmpty()) {
            return Optional.empty();
        }
        final GlobalPos here = Obelisks.place(obelisk);
        return Obelisks.at(server, obelisk.link().get())
                .filter(partner -> partner.link().filter(here::equals).isPresent());
    }

    // The obelisk at a place, its chunk loaded if need be
    private static Optional<AsgardObeliskBlockEntity> at(final MinecraftServer server, final GlobalPos place) {
        final ServerLevel level = server.getLevel(place.dimension());
        return level != null && level.getBlockEntity(place.pos()) instanceof final AsgardObeliskBlockEntity obelisk
                ? Optional.of(obelisk) : Optional.empty();
    }

    private static GlobalPos place(final AsgardObeliskBlockEntity obelisk) {
        return GlobalPos.of(obelisk.getLevel() == null ? Level.OVERWORLD : obelisk.getLevel().dimension(),
                obelisk.getBlockPos());
    }

    // ---- Beaming ----

    /** A player touched an obelisk's gem: the beam starts, if they may use it and it is linked. */
    public static void transport(final ServerPlayer player, final AsgardObeliskBlockEntity from) {
        final int now = player.level().getServer().getTickCount();
        final Integer arrived = Obelisks.ARRIVED.get(player.getUUID());
        if (arrived != null && now - arrived < Obelisks.COOLDOWN_TICKS || from.beaming(player)) {
            return;
        }
        if (!from.mayUse(player)) {
            Obelisks.tell(player, "no_access", "You aren't allowed to use this obelisk", true);
            return;
        }
        final BlockPos at = from.getBlockPos();
        if (player.distanceToSqr(at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5) > Obelisks.REACH * Obelisks.REACH) {
            Obelisks.tell(player, "too_far", "Stand nearer the obelisk", true);
            return;
        }
        if (from.link().isEmpty()) {
            Obelisks.tell(player, "not_linked", "This obelisk isn't linked to another", true);
            return;
        }
        if (Obelisks.partner(from).isEmpty()) {
            from.setLink(null);
            Obelisks.tell(player, "link_broken", "The obelisk it was linked to is gone", true);
            return;
        }
        from.startBeam(player);
        // The player hears the beam on their own, carried with them, as their screen goes white; others hear it from
        // the obelisk
        SGNetwork.send(player, new ObeliskPayloads.Beam(true, Obelisks.BEAM_TICKS, true));
        player.level().playSound(player, at.getX() + 0.5, at.getY() + 1.5, at.getZ() + 0.5, SGSounds.OBELISK_BEAM,
                SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    // The player moved off their spot
    static void cancel(final ServerPlayer player) {
        SGNetwork.send(player, new ObeliskPayloads.Beam(false, 0, false));
        Obelisks.tell(player, "beam_canceled", "You moved, so the beam was called off", true);
    }

    // The beam comes: out in front of the far obelisk's gem, facing it
    static void beam(final ServerPlayer player, final AsgardObeliskBlockEntity from) {
        final Optional<AsgardObeliskBlockEntity> to = Obelisks.partner(from);
        if (to.isEmpty() || !(to.get().getLevel() instanceof final ServerLevel destination)) {
            from.setLink(null);
            Obelisks.tell(player, "link_broken", "The obelisk it was linked to is gone", true);
            return;
        }
        final Direction facing = to.get().getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        final BlockPos arrive = to.get().getBlockPos().relative(facing, Obelisks.ARRIVE);
        final Vec3 where = new Vec3(arrive.getX() + 0.5, arrive.getY(), arrive.getZ() + 0.5);
        Obelisks.flash(player.level(), player.position());
        player.stopRiding();
        player.teleport(new TeleportTransition(destination, where, Vec3.ZERO, facing.getOpposite().toYRot(),
                player.getXRot(), TeleportTransition.DO_NOTHING));
        player.resetFallDistance();
        Obelisks.ARRIVED.put(player.getUUID(), player.level().getServer().getTickCount());
        Obelisks.flash(destination, where);
    }

    // The white flash of an Asgard beam, where it leaves and arrives
    private static void flash(final ServerLevel level, final Vec3 at) {
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 1, at.z, 40, 0.3, 0.9, 0.3, 0.04);
        level.sendParticles(ParticleTypes.FIREWORK, at.x, at.y + 1, at.z, 25, 0.25, 0.8, 0.25, 0.02);
    }

    public static void tell(final ServerPlayer player, final String key, final String fallback, final boolean bad) {
        final Component text = Component.translatableWithFallback("message.sgcraft.obelisk." + key, fallback);
        player.sendOverlayMessage(bad ? text.copy().withStyle(ChatFormatting.RED) : text);
    }
}
