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

import com.almuradev.sgcraft.mod.network.SGNetwork;
import com.almuradev.sgcraft.mod.obelisk.ObeliskPayloads;
import com.almuradev.sgcraft.mod.sound.SGSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Goa'uld transport rings, as 2.2.0's: two ring platforms in one world, linked in pairs as Asgard obelisks are.
 * Started from a ring control panel near one, or a ring remote, the five rings of the one started rise from the floor,
 * or drop from the ceiling, around whoever stands inside, light up and go back; then its partner's do the same, and
 * everything that was inside one set is swapped with everything inside the other as they set off. Players inside are
 * held still and safe from damage until it is over. The platform that starts it pays, in gate energy, more the further
 * it sends.
 */
public final class Rings {

    // A transport runs in two halves, one after the other, each timed to the ring sound, 8.9 seconds long, which
    // plays at each platform in turn. The sender's rings rise and lock over 3.2 seconds; a quarter of a second later a
    // column of white light snaps on around those inside, holds, fades, and the rings drop, home as the half ends.
    // Then the receiver's rings do the same, and those in transit arrive in its light. Ticks are from the start of
    // each platform's half
    /** How long the rings take to rise and lock, in ticks. */
    public static final int STACK_TICKS = 64;
    /** When the column of light snaps on inside the rings, and those inside start to leave, in ticks. */
    public static final int DEPART_TICK = Rings.STACK_TICKS + 5;
    /** When the column of light starts to fade, in ticks. */
    public static final int LIGHT_FADE_TICK = Rings.DEPART_TICK + 28;
    /** When the column of light is gone and the rings start back, in ticks. */
    public static final int LIGHT_END_TICK = Rings.LIGHT_FADE_TICK + 12;
    /** How long each platform's half takes, its rings home at the end, in ticks. */
    public static final int HALF_TICKS = Rings.LIGHT_END_TICK + 32;
    /** How long a whole transport takes, both halves, in ticks after the sender's rings set off. */
    public static final int DONE_TICKS = 2 * Rings.HALF_TICKS;
    /**
     * When those in transit arrive in the receiver's light, in ticks into its half. They are moved as its rings set
     * off, while their screens are black, so they hear its sound from the start.
     */
    public static final int ARRIVE_TICK = Rings.DEPART_TICK + 2;
    /** How long before the swap those in the receiver's rings are held and see a white flash, in ticks. */
    public static final int RETURN_FLASH_TICKS = 14;
    /** How far below a ceiling platform the floor its rings drop to can be, in blocks. */
    public static final int MAX_DROP = 12;
    /** How far a ring remote reaches to find a platform, in blocks. */
    public static final double REMOTE_REACH = 8;
    /** How far a ring control panel reaches to find a platform, in blocks. */
    public static final double PANEL_REACH = 16;
    // How far from a platform the ring sound can be heard, for stopping it as its half ends, in blocks
    private static final double HEARING = 64;

    // The platform each player started linking from, kept by place, as its chunk may unload before the next click
    private static final Map<UUID, GlobalPos> LINKING = new ConcurrentHashMap<>();

    private Rings() {
    }

    /** Forgets the link a player had started, as they leave the server. */
    public static void forget(final UUID player) {
        Rings.LINKING.remove(player);
    }

    /** Forgets every link started, as the server stops. */
    public static void clear() {
        Rings.LINKING.clear();
    }

    /** @return the space a platform's rings close around, from the floor they stack on up */
    public static AABB volume(final BlockPos center, final double floorY) {
        return new AABB(center.getX() - 1, floorY, center.getZ() - 1, center.getX() + 2, floorY + 2.6,
                center.getZ() + 2);
    }

    // ---- Linking ----

    /**
     * The first platform a player sneak-uses is remembered; sneak-using a second in the same world links the two, each
     * to the other. Linking sends everyone who uses either somewhere new, so it is for those who may configure both:
     * with only use asked of them, anyone could link a public platform to one of their own and pull those standing in
     * it into a trap.
     */
    public static void link(final ServerPlayer player, final RingPlatformBlockEntity platform) {
        if (!platform.mayConfigure(player)) {
            platform.refuseConfigure(player);
            return;
        }
        final Level level = platform.getLevel();
        if (level == null) {
            return;
        }
        final GlobalPos here = GlobalPos.of(level.dimension(), platform.getBlockPos());
        final GlobalPos start = Rings.LINKING.remove(player.getUUID());
        if (here.equals(start)) {
            Rings.tell(player, "link_canceled", "Linking canceled", false);
            return;
        }
        if (start != null && !start.dimension().equals(here.dimension())) {
            Rings.tell(player, "link_other_world", "Ring platforms can only be linked within one world", true);
        }
        final RingPlatformBlockEntity first = start == null || !start.dimension().equals(here.dimension()) ? null
                : Rings.at(level, start.pos()).orElse(null);
        if (first == null) {
            Rings.LINKING.put(player.getUUID(), here);
            Rings.tell(player, "link_start", "Now sneak-use another ring platform to link the two, or this one "
                    + "again to cancel", false);
            return;
        }
        if (!first.mayConfigure(player)) {
            first.refuseConfigure(player);
            return;
        }
        // Not while either runs: the far end would be swapped partway for one whose rings never rose
        if (first.running() || platform.running()) {
            Rings.tell(player, "busy", "The linked ring platform is in use", true);
            return;
        }
        final int most = RingSettings.current().maxDistance();
        if (most > 0 && first.getBlockPos().distSqr(platform.getBlockPos()) > (double) most * most) {
            Rings.tell(player, "too_far", "Ring platforms can be at most %s blocks apart", true, most);
            return;
        }
        Rings.unlink(first);
        Rings.unlink(platform);
        Rings.linkPair(first, platform);
        Rings.tell(player, "linked", "The ring platforms are linked", false);
    }

    /** Links two unlinked platforms in the same world, each to the other. */
    public static void linkPair(final RingPlatformBlockEntity a, final RingPlatformBlockEntity b) {
        a.setLink(b.getBlockPos());
        b.setLink(a.getBlockPos());
    }

    /** Unlinks a platform, and its partner from it if that one is linked back. */
    public static void unlink(final RingPlatformBlockEntity platform) {
        Rings.partner(platform).ifPresent(partner -> partner.setLink(null));
        platform.setLink(null);
    }

    /** @return the platform a platform is linked to, if it is still there and linked back */
    public static Optional<RingPlatformBlockEntity> partner(final RingPlatformBlockEntity platform) {
        final Level level = platform.getLevel();
        if (level == null || platform.linkPos().isEmpty()) {
            return Optional.empty();
        }
        return Rings.at(level, platform.linkPos().get())
                .filter(partner -> partner.linkPos().filter(platform.getBlockPos()::equals).isPresent());
    }

    /** @return the platform at a place, its chunk loaded if need be */
    public static Optional<RingPlatformBlockEntity> at(final Level level, final BlockPos pos) {
        return level.getBlockEntity(pos) instanceof final RingPlatformBlockEntity platform ? Optional.of(platform)
                : Optional.empty();
    }

    // ---- Starting ----

    /**
     * @return the platform a panel or a remote at a place starts: the one whose rings the player stands in, else the
     *     nearest within the given reach, in blocks, among those already loaded
     */
    public static Optional<RingPlatformBlockEntity> near(final Level level, final Vec3 at,
            final @Nullable Player player, final double within) {
        RingPlatformBlockEntity nearest = null;
        double best = within * within;
        final int reach = (int) Math.ceil(within);
        final int x0 = SectionPos.blockToSectionCoord(at.x - reach);
        final int x1 = SectionPos.blockToSectionCoord(at.x + reach);
        final int z0 = SectionPos.blockToSectionCoord(at.z - reach);
        final int z1 = SectionPos.blockToSectionCoord(at.z + reach);
        for (int cx = x0; cx <= x1; cx++) {
            for (int cz = z0; cz <= z1; cz++) {
                final LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (final BlockEntity entity : List.copyOf(chunk.getBlockEntities().values())) {
                    if (!(entity instanceof final RingPlatformBlockEntity platform) || platform.isRemoved()) {
                        continue;
                    }
                    if (player != null && platform.volume().contains(player.position().add(0, 0.1, 0))) {
                        return Optional.of(platform);
                    }
                    final double distance = at.distanceToSqr(platform.center());
                    if (distance < best) {
                        best = distance;
                        nearest = platform;
                    }
                }
            }
        }
        return Optional.ofNullable(nearest);
    }

    /** Starts a platform's rings and its partner's, if the player may and the platform can pay. */
    public static void activate(final ServerPlayer player, final Optional<RingPlatformBlockEntity> found) {
        if (found.isEmpty()) {
            Rings.tell(player, "no_platform", "No ring platform within reach", true);
            return;
        }
        final RingPlatformBlockEntity platform = found.get();
        if (platform.running()) {
            return;
        }
        if (!platform.mayUse(player)) {
            Rings.tell(player, "no_access", "You aren't allowed to use these rings", true);
            return;
        }
        if (platform.linkPos().isEmpty()) {
            Rings.tell(player, "not_linked", "This ring platform isn't linked to another", true);
            return;
        }
        final Optional<RingPlatformBlockEntity> partner = Rings.partner(platform);
        if (partner.isEmpty()) {
            Rings.unlink(platform);
            Rings.tell(player, "link_broken", "The ring platform this one was linked to is gone", true);
            return;
        }
        if (partner.get().running()) {
            Rings.tell(player, "busy", "The linked ring platform is in use", true);
            return;
        }
        final double cost = Rings.cost(platform, partner.get());
        if (platform.energyAvailable() < cost) {
            Rings.tell(player, "no_power", "Not enough power: the rings have %s GE, with their naquadah, of the %s "
                    + "they need", true, (long) platform.energyAvailable(), (long) Math.ceil(cost));
            return;
        }
        Rings.start(platform, partner.get());
    }

    /** @return what a transport between two platforms costs, in gate energy */
    public static double cost(final RingPlatformBlockEntity from, final RingPlatformBlockEntity to) {
        return RingSettings.current().cost(Math.sqrt(from.getBlockPos().distSqr(to.getBlockPos())));
    }

    /**
     * Pays for a transport and starts both platforms' rings: the first's half now, and the partner's once it is over.
     * The first runs the whole transport, and those inside its rings are held still and safe until it is over.
     */
    public static void start(final RingPlatformBlockEntity platform, final RingPlatformBlockEntity partner) {
        platform.pay(Rings.cost(platform, partner));
        platform.start(true);
        partner.start(false);
        RingHolds.hold(platform, Rings.DONE_TICKS);
    }

    // ---- The transport ----

    /** @return whether a transport does something this many ticks into it, for the platform that started it */
    static boolean steps(final int ticks) {
        return ticks == Rings.DEPART_TICK || ticks == Rings.HALF_TICKS - Rings.RETURN_FLASH_TICKS
                || ticks == Rings.HALF_TICKS || ticks == Rings.HALF_TICKS + Rings.ARRIVE_TICK;
    }

    /** Runs a transport on, a tick into it, from the platform that started it. */
    static void step(final RingPlatformBlockEntity sender, final RingPlatformBlockEntity receiver, final int ticks) {
        if (ticks == Rings.DEPART_TICK) {
            // As its light snaps on, those inside go white, then black in transit, and white again as they arrive
            Rings.transitScreen(sender);
        } else if (ticks == Rings.HALF_TICKS - Rings.RETURN_FLASH_TICKS) {
            // Those in the receiver's rings are held as the sender's were, and flash white as they are sent back
            RingHolds.hold(receiver, Rings.DONE_TICKS - ticks);
            Rings.returnScreen(receiver);
        } else if (ticks == Rings.HALF_TICKS) {
            // The receiver's half: its rings set off with the sound again, the sender's stopped, and those in transit
            // are moved into them while their screens are black
            Rings.stopSound(sender);
            Rings.swap(sender, receiver);
            receiver.playSound();
        } else if (ticks == Rings.HALF_TICKS + Rings.ARRIVE_TICK && receiver.getLevel() != null
                && !receiver.getLevel().getEntitiesOfClass(Entity.class, receiver.volume(), Entity::isAlive)
                        .isEmpty()) {
            // Its light snaps on around those who arrived, their screens clearing to show it
            Rings.flash(receiver);
        }
    }

    // What those leaving see, from the sender's light snapping on: their screens fade to white, hold, fade to black
    // and stay so in transit, fade back to white as they arrive in the receiver's light, and clear to show it
    private static void transitScreen(final RingPlatformBlockEntity platform) {
        final int arrive = (Rings.HALF_TICKS + Rings.ARRIVE_TICK - Rings.DEPART_TICK) * 50;
        Rings.screen(platform, List.of(new ObeliskPayloads.FadeStep(0, 0, 1),
                new ObeliskPayloads.FadeStep(700, 1, 1), new ObeliskPayloads.FadeStep(950, 1, 1),
                new ObeliskPayloads.FadeStep(1950, 1, 0), new ObeliskPayloads.FadeStep(arrive - 800, 1, 0),
                new ObeliskPayloads.FadeStep(arrive, 1, 1), new ObeliskPayloads.FadeStep(arrive + 200, 1, 1),
                new ObeliskPayloads.FadeStep(arrive + 1100, 0, 1)));
    }

    // What those in the receiver's rings see as they are sent the other way: a white flash, full as they go
    private static void returnScreen(final RingPlatformBlockEntity platform) {
        final int go = Rings.RETURN_FLASH_TICKS * 50;
        Rings.screen(platform, List.of(new ObeliskPayloads.FadeStep(0, 0, 1),
                new ObeliskPayloads.FadeStep(go, 1, 1), new ObeliskPayloads.FadeStep(go + 200, 1, 1),
                new ObeliskPayloads.FadeStep(go + 1100, 0, 1)));
    }

    private static void screen(final RingPlatformBlockEntity platform, final List<ObeliskPayloads.FadeStep> steps) {
        if (platform.getLevel() == null) {
            return;
        }
        for (final ServerPlayer player : platform.getLevel().getEntitiesOfClass(ServerPlayer.class,
                platform.volume())) {
            SGNetwork.send(player, new ObeliskPayloads.Fade(steps));
        }
    }

    // Stops the ring sound for those within hearing of a platform whose half is over, as the other's starts: the
    // sound runs on past it
    private static void stopSound(final RingPlatformBlockEntity platform) {
        if (!(platform.getLevel() instanceof final ServerLevel level)) {
            return;
        }
        final Vec3 at = platform.center();
        final ClientboundStopSoundPacket stop = new ClientboundStopSoundPacket(SGSounds.RING_TRANSPORT.location(),
                SoundSource.BLOCKS);
        for (final ServerPlayer player : level.players()) {
            if (player.distanceToSqr(at) < Rings.HEARING * Rings.HEARING) {
                player.connection.send(stop);
            }
        }
    }

    // The light snapping on inside a platform's rings around what arrives: white sparkles and sparks, as an
    // obelisk's beam gives
    private static void flash(final RingPlatformBlockEntity platform) {
        if (platform.getLevel() instanceof final ServerLevel level) {
            final Vec3 at = platform.center();
            level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 90, 1.1, 1.0, 1.1, 0.04);
            level.sendParticles(ParticleTypes.FIREWORK, at.x, at.y, at.z, 60, 1.0, 0.9, 1.0, 0.02);
        }
    }

    /**
     * Swaps everything inside one platform's rings with everything inside the other's, each keeping its place
     * relative to the rings and the way it faces.
     */
    static void swap(final RingPlatformBlockEntity a, final RingPlatformBlockEntity b) {
        if (!(a.getLevel() instanceof final ServerLevel level)) {
            return;
        }
        final List<Entity> fromA = level.getEntitiesOfClass(Entity.class, a.volume(), Entity::isAlive);
        final List<Entity> fromB = level.getEntitiesOfClass(Entity.class, b.volume(), Entity::isAlive);
        // Done in the dark, as the receiver's rings set off: what goes there sparkles as its light comes on (see
        // step); what is sent back to the sender, whose rings are home, sparkles as it lands
        if (!fromB.isEmpty()) {
            Rings.flash(a);
        }
        final Vec3 offset = b.floorCenter().subtract(a.floorCenter());
        // Riders are let go first, so each is carried on its own
        fromA.forEach(Entity::stopRiding);
        fromB.forEach(Entity::stopRiding);
        fromA.forEach(entity -> Rings.move(level, entity, offset));
        fromB.forEach(entity -> Rings.move(level, entity, offset.reverse()));
    }

    private static void move(final ServerLevel level, final Entity entity, final Vec3 by) {
        entity.teleport(new TeleportTransition(level, entity.position().add(by), entity.getDeltaMovement(),
                entity.getYRot(), entity.getXRot(), TeleportTransition.DO_NOTHING));
        entity.resetFallDistance();
        // A held player is held where they arrive
        RingHolds.moved(entity);
    }

    /** Tells a player something about rings, over their hotbar. */
    public static void tell(final ServerPlayer player, final String key, final String fallback, final boolean bad,
            final Object... args) {
        final Component text = Component.translatableWithFallback("message.sgcraft.rings." + key, fallback, args);
        player.sendOverlayMessage(bad ? text.copy().withStyle(ChatFormatting.RED) : text);
    }
}
