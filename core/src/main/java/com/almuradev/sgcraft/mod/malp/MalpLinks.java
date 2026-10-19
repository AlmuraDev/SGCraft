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

package com.almuradev.sgcraft.mod.malp;

import com.almuradev.sgcraft.api.SGCraftApi;
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.block.entity.MalpConsoleBlockEntity;
import com.almuradev.sgcraft.mod.computer.DialingComputers;
import com.almuradev.sgcraft.mod.entity.MalpEntity;
import com.almuradev.sgcraft.mod.entity.PuddleJumperEntity;
import com.almuradev.sgcraft.mod.item.BatteryItem;
import com.almuradev.sgcraft.mod.item.MalpTabletItem;
import com.almuradev.sgcraft.mod.item.SGItems;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.ConsoleAction;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.ConsoleState;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.ConsoleUse;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.Drive;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.Feed;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.MalpInfo;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.MonitorState;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.NearGate;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.Readout;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.Role;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.Use;
import com.almuradev.sgcraft.mod.network.SGNetwork;
import com.almuradev.sgcraft.mod.registry.RegistrySink;
import com.almuradev.sgcraft.mod.shield.CityShieldBlockEntity;
import com.almuradev.sgcraft.mod.view.RemoteViewConfig;
import com.almuradev.sgcraft.mod.view.RemoteViews;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The links between players and the MALPs they watch, from MALP consoles and MALP tablets. A link lasts while the
 * player has the screen of their console or tablet open and stays with it: within reach of the console, or holding
 * the tablet in the hand they used it from, in the world they used it in. While a link drives a MALP, the MALP's
 * chunk is kept loaded, and for a while after, so it keeps ticking wherever it is. While the console or tablet
 * reaches the MALP, a tablet has gate energy in its own battery, and the MALP has power, its battery holding gate
 * energy or it needing none, the feed is live: the player sees what the MALP sees, the MALP uses gate energy if it
 * needs power, a tablet uses gate energy from its own battery, and the MALP takes the driver's controls.
 *
 * <p>A player works a console from an office chair in front of it, or standing at it, as at a dialing computer: one
 * who stands steps up to its front, if there is safe ground there within their reach, and turns to its monitor, and
 * every client draws them with their hands on its keyboard; getting up from the chair closes the screen. A player with
 * a tablet's screen open is drawn holding it out in front of them, looking down at it. If the server's settings say
 * so, damage takes them off either.</p>
 *
 * <p>One player at a time drives a MALP: the first to choose it, from a console or a tablet. A player who chooses a
 * MALP someone else drives only watches it, while their own console or tablet reaches it, and may choose another
 * MALP. One player at a time is at a console: a player who opens a console someone else is at only watches what that
 * player sees, may do nothing but close the screen, and neither steps up to the console nor is drawn at it. When the
 * player someone watches leaves, or turns to another MALP, the watcher sees the feed go offline.</p>
 *
 * <p>While the feed of the player at a console is live, the players nearest it who aren't at it see the feed on its
 * monitor, as many as the server's settings allow, within the settings' range. With no one at a console, its monitor
 * shows those players the feed of the MALP last chosen at it, or else of the first MALP paired with it, while the
 * console reaches it and it has power: while anyone is near enough to see it, the MALP's chunk is kept loaded and the
 * MALP uses its idle gate energy, unless someone drives it, whose feed pays for it. With no one near, nothing is sent
 * and nothing is used. A tablet has no monitor. Watching, on a screen or a monitor, uses none of the MALP's gate
 * energy beyond what the driver's feed uses.</p>
 */
public final class MalpLinks {

    /** The ticket that keeps a watched MALP's chunk loaded, and running, for a moment at a time. */
    public static final TicketType TICKET = new TicketType(40L, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION
            | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    // A ticket's level falls by one a chunk out from its center, and entities only tick at two below a full chunk's,
    // so a ticket this many chunks out runs every chunk two fewer out, and loads chunks this far out
    private static final int TICKET_LEVEL_DROP = 2;
    private static final int TICKET_TICKS = 20;
    private static final int SIGNAL_TICKS = 5;
    private static final int STATE_TICKS = 10;
    // How often a tablet's battery pays for the gate energy its feed used, so the tablet isn't changed every tick
    private static final int TABLET_PAY_TICKS = 20;
    // How often a console looks for the players near it who see its monitor
    private static final int SPECTATOR_TICKS = 10;
    // How long a MALP whose feed a monitor shows with no one at the console is kept loaded past each look
    private static final int MONITOR_LOAD_TICKS = 2 * MalpLinks.TICKET_TICKS;
    // How far past the range a player who sees a monitor may step before it stops, so one at the edge doesn't flicker
    private static final double SPECTATOR_MARGIN = 2;
    private static final int HOSTILE_RANGE = 32;
    // The most the camera can be turned by one message, in degrees
    private static final float MAX_AIM = 30;
    // Where the dragon's fight looks for players from, and how far
    private static final Vec3 ARENA_CENTER = new Vec3(0, 128, 0);
    private static final double ARENA_RANGE = 192;

    // Each link, by its player
    private static final Map<UUID, Link> LINKS = new HashMap<>();
    // Each MALP whose chunk is kept loaded, and the tick it may unload after
    private static final Map<UUID, Long> LOADED = new HashMap<>();
    // What the monitor of each console no one is at shows, by the console
    private static final Map<GlobalPos, Monitor> MONITORS = new HashMap<>();

    private MalpLinks() {
    }

    private static final class Link {

        private final ServerPlayer player;
        private final MalpController controller;
        private Role role = Role.DRIVER;
        // The player a watcher follows: the one at their console, or the one driving their MALP
        private @Nullable UUID followed;
        // Whether the player a watcher follows has left, or turned to another MALP
        private boolean orphaned;
        private Use use = Use.NONE;
        private @Nullable UUID selected;
        private boolean feedOn = true;
        private MalpSignal signal = MalpSignal.NONE;
        private Feed feed = Feed.NO_SIGNAL;
        private @Nullable MalpEntity viewing;
        // The players near the console who see this player's feed on its monitor
        private Set<UUID> spectators = new HashSet<>();
        // The gate energy a tablet's feed has used since the tablet's battery last paid for it
        private double owed;

        private Link(final ServerPlayer player, final MalpController controller) {
            this.player = player;
            this.controller = controller;
        }

        private boolean watcher() {
            return this.role != Role.DRIVER;
        }

        // Whether the player is the one at a console, rather than at a tablet or watching someone at a console
        private boolean atConsole() {
            return this.controller instanceof MalpController.Console && this.role != Role.CONSOLE_WATCHER;
        }

        // Drivers first, then those watching a MALP, who may be at a console, then those watching them
        private int order() {
            return switch (this.role) {
                case DRIVER -> 0;
                case MALP_WATCHER -> 1;
                case CONSOLE_WATCHER -> 2;
            };
        }
    }

    // A monitor's feed with no one at its console: the players near it who see it, and the MALP it shows, if any
    private static final class Monitor {

        private Set<UUID> spectators = new HashSet<>();
        private @Nullable MalpEntity malp;
        // The MALP it would show, and what its feed shows
        private @Nullable UUID selected;
        private Feed feed = Feed.OFF;
        private MalpSignal signal = MalpSignal.NONE;
    }

    /** Registers the ticket types: the MALP's, the city shield's, and the puddle jumper's for a gate's far end. */
    public static void registerTickets(final RegistrySink<TicketType> sink) {
        sink.register(ResourceKey.create(Registries.TICKET_TYPE, SGCraft.id("malp")), MalpLinks.TICKET);
        sink.register(ResourceKey.create(Registries.TICKET_TYPE, SGCraft.id("city_shield")),
                CityShieldBlockEntity.TICKET);
        sink.register(ResourceKey.create(Registries.TICKET_TYPE, SGCraft.id("jumper_far_side")),
                PuddleJumperEntity.FAR_SIDE);
    }

    /**
     * Opens a console for a player. If someone else is at it, the player only watches what that player sees.
     * Otherwise the player is at the console, seated in a chair in front of it or standing: one standing steps up to
     * its front, if that spot is within their reach and safe to stand on, and turns to its monitor. They choose a MALP
     * paired with it, as {@link #openTablet} describes, and the console remembers it, to show on its monitor once they
     * leave. Those who saw the monitor's feed go on seeing the feed of the player at it.
     *
     * @param console the console's middle lower part
     */
    public static void open(final ServerPlayer player, final GlobalPos console) {
        if (DialingComputers.startUp(player, console.pos())) {
            MalpLinks.open(player, new MalpController.Console(console));
        }
    }

    /**
     * Opens the screen of the tablet in a player's hand, if they hold one there. MALPs the server no longer has, such
     * as those packed up or destroyed, leave its list first. The player chooses the MALP they watched from it before,
     * if no one else drives it, or else the first MALP on its list no one else drives, and drives it, loading its
     * chunk; if everyone's MALPs are driven by someone else, they watch the first.
     */
    public static void openTablet(final ServerPlayer player, final InteractionHand hand) {
        final ItemStack tablet = player.getItemInHand(hand);
        if (!tablet.is(SGItems.MALP_TABLET)) {
            return;
        }
        final MalpRegistry registry = MalpRegistry.get(player.level().getServer());
        // MALPs that are gone, or that the server never paired the tablet with, come off its list
        final List<UUID> paired = MalpTabletItem.paired(player.level().getServer(), tablet);
        for (final UUID id : MalpTabletItem.malps(tablet)) {
            if (registry.get(id).isEmpty() || !paired.contains(id)) {
                MalpTabletItem.unpair(player.level().getServer(), tablet, id);
            }
        }
        MalpLinks.open(player, new MalpController.Tablet(hand, tablet, player.level().dimension()));
    }

    private static void open(final ServerPlayer player, final MalpController controller) {
        final MinecraftServer server = player.level().getServer();
        final Link before = MalpLinks.LINKS.remove(player.getUUID());
        if (before != null) {
            MalpLinks.stopViewing(before);
            MalpLinks.pay(before);
        }
        final Link link = new Link(player, controller);
        final Link occupant = MalpLinks.occupantAt(controller);
        if (occupant != null) {
            link.role = Role.CONSOLE_WATCHER;
            link.followed = occupant.player.getUUID();
            // Seen on the screen now, rather than on the monitor as well
            if (controller instanceof MalpController.Console console
                    && occupant.spectators.remove(player.getUUID())) {
                RemoteViews.stopOnDisplay(player, console.pos().pos());
            }
        } else {
            UUID keep = null;
            if (before != null && before.role != Role.CONSOLE_WATCHER && before.controller.equals(controller)) {
                keep = before.selected;
                link.feedOn = before.feedOn;
            } else if (controller instanceof MalpController.Console console) {
                // The feed is as it was left at the console, powered down or not
                final MalpConsoleBlockEntity entity = MalpLinks.consoleAt(server, console.pos());
                link.feedOn = entity == null || !entity.feedOff();
            }
            MalpLinks.choose(link, MalpLinks.initial(link, controller.malps(server), keep));
            if (before != null && before.atConsole() && link.atConsole() && before.controller.equals(controller)) {
                // At the same console again, so those around it go on seeing its monitor
                link.spectators = before.spectators;
                before.spectators = new HashSet<>();
            }
            if (link.atConsole() && controller instanceof MalpController.Console console) {
                // Those who saw the monitor's feed with no one at the console go on seeing it, now this player's
                final Monitor monitor = MalpLinks.MONITORS.remove(console.pos());
                if (monitor != null) {
                    link.spectators.addAll(monitor.spectators);
                }
            }
        }
        if (before != null) {
            MalpLinks.stopSpectators(before);
        }
        // Seated in a chair, the player is drawn sitting, as usual
        link.use = link.atConsole() ? player.isPassenger() ? Use.NONE : Use.COMPUTER
                : controller instanceof MalpController.Tablet ? Use.TABLET : Use.NONE;
        MalpLinks.LINKS.put(player.getUUID(), link);
        if (link.atConsole() && controller instanceof MalpController.Console console) {
            DialingComputers.stepUp(player, console.pos().pos());
            MalpLinks.remember(link);
        }
        if (link.use != (before == null ? Use.NONE : before.use)) {
            MalpLinks.broadcastUse(player, link.use);
        }
        MalpLinks.keepLoaded(server, link);
        MalpLinks.refresh(link);
        MalpLinks.send(link, true, false);
        MalpLinks.updateSpectators(link);
        if (before != null) {
            MalpLinks.updateWatchers(player);
        }
    }

    // The player at a console, if anyone is
    private static @Nullable Link occupantAt(final MalpController controller) {
        if (!(controller instanceof MalpController.Console)) {
            return null;
        }
        for (final Link link : MalpLinks.LINKS.values()) {
            if (link.atConsole() && link.controller.equals(controller)) {
                return link;
            }
        }
        return null;
    }

    // The link driving a MALP, other than one link, if any
    private static @Nullable Link driverOf(final UUID malp, final Link except) {
        for (final Link link : MalpLinks.LINKS.values()) {
            if (link != except && link.role == Role.DRIVER && malp.equals(link.selected)) {
                return link;
            }
        }
        return null;
    }

    // The MALP a link starts on: the one given, if it is listed and no one else drives it, or else the first listed
    // that no one else drives, or else, to watch, the one given if listed, or the first
    private static @Nullable UUID initial(final Link link, final List<MalpRegistry.Entry> malps,
            final @Nullable UUID wanted) {
        final boolean listed = wanted != null && malps.stream().anyMatch(entry -> entry.id().equals(wanted));
        if (listed && MalpLinks.driverOf(wanted, link) == null) {
            return wanted;
        }
        for (final MalpRegistry.Entry entry : malps) {
            if (MalpLinks.driverOf(entry.id(), link) == null) {
                return entry.id();
            }
        }
        return listed ? wanted : malps.isEmpty() ? null : malps.getFirst().id();
    }

    // Has a link watch a MALP, or none: it drives the MALP if no one else does, and otherwise watches whoever does
    private static void choose(final Link link, final @Nullable UUID malp) {
        final Link driver = malp == null ? null : MalpLinks.driverOf(malp, link);
        link.selected = malp;
        link.orphaned = false;
        link.role = driver == null ? Role.DRIVER : Role.MALP_WATCHER;
        link.followed = driver == null ? null : driver.player.getUUID();
    }

    // Turns a link to another MALP, or to none, stopping the MALP it drove
    private static void select(final Link link, final @Nullable UUID malp) {
        MalpLinks.stopViewing(link);
        MalpLinks.choose(link, malp);
        MalpLinks.keepLoaded(link.player.level().getServer(), link);
        MalpLinks.remember(link);
    }

    // The console the player is at remembers the MALP they chose, and whether they powered the feed down, for its
    // monitor to keep to once they leave
    private static void remember(final Link link) {
        if (!link.atConsole() || !(link.controller instanceof MalpController.Console console)) {
            return;
        }
        final MalpConsoleBlockEntity entity = MalpLinks.consoleAt(link.player.level().getServer(), console.pos());
        if (entity != null) {
            if (link.selected != null) {
                entity.setShown(link.selected);
            }
            entity.setFeedOff(!link.feedOn);
        }
    }

    // A console's middle lower part, if it is there and loaded
    private static @Nullable MalpConsoleBlockEntity consoleAt(final MinecraftServer server, final GlobalPos console) {
        final ServerLevel level = server.getLevel(console.dimension());
        return level != null && level.isLoaded(console.pos())
                && level.getBlockEntity(console.pos()) instanceof final MalpConsoleBlockEntity entity
                && !entity.isRemoved() ? entity : null;
    }

    /** @return whether a player has the screen of a MALP console or MALP tablet open, driving or only watching */
    public static boolean linked(final ServerPlayer player) {
        return MalpLinks.LINKS.containsKey(player.getUUID());
    }

    /** @return whether a player has a MALP console's screen open, driving at it or only watching */
    public static boolean atConsole(final ServerPlayer player) {
        final Link link = MalpLinks.LINKS.get(player.getUUID());
        return link != null && link.controller instanceof MalpController.Console;
    }

    /** @return whether a player has a MALP tablet's screen open, driving or only watching */
    public static boolean atTablet(final ServerPlayer player) {
        final Link link = MalpLinks.LINKS.get(player.getUUID());
        return link != null && link.controller instanceof MalpController.Tablet;
    }

    /** @return whether a player drives a MALP from the screen of a console or tablet they have open */
    public static boolean driving(final ServerPlayer player) {
        final Link link = MalpLinks.LINKS.get(player.getUUID());
        return link != null && !link.watcher();
    }

    /** @return whether a player has the screen of a console or tablet open only to watch someone else's feed */
    public static boolean viewOnly(final ServerPlayer player) {
        final Link link = MalpLinks.LINKS.get(player.getUUID());
        return link != null && link.watcher();
    }

    /** @return the MALP a player watches from the screen they have open, if they chose one */
    public static Optional<UUID> selected(final ServerPlayer player) {
        return Optional.ofNullable(MalpLinks.LINKS.get(player.getUUID())).map(link -> link.selected);
    }

    /**
     * @return the players who see the feed on the monitor of a console, as the feed of the player at it is live;
     *     empty if no one is at it
     */
    public static Set<UUID> spectators(final GlobalPos console) {
        final Link link = MalpLinks.occupantAt(new MalpController.Console(console));
        return link == null ? Set.of() : Set.copyOf(link.spectators);
    }

    /**
     * Takes a player off their console or tablet when they take damage, as though they had closed its screen, and
     * closes the screen, if the server's settings say damage does.
     */
    public static void hurt(final ServerPlayer player) {
        final Link link = MalpLinks.LINKS.get(player.getUUID());
        if (link == null || !MalpSettings.current().consoleDamageInterrupts()) {
            return;
        }
        MalpLinks.close(player);
        MalpLinks.send(link, false, true);
    }

    /** Tells a player who starts tracking another how to draw the other, if the other is at a console or tablet. */
    public static void startTracking(final ServerPlayer seen, final ServerPlayer viewer) {
        final Link link = MalpLinks.LINKS.get(seen.getUUID());
        if (link != null && link.use != Use.NONE) {
            SGNetwork.send(viewer, new ConsoleUse(seen.getId(), link.use));
        }
    }

    // Tells the players near a player how to draw them, at a console, holding a tablet, or neither; those who come
    // near later are told as they start tracking them
    private static void broadcastUse(final ServerPlayer player, final Use use) {
        SGNetwork.sendNear(player, new ConsoleUse(player.getId(), use));
    }

    /**
     * Does what a button on a console's or tablet's screen does. Only the driver works the MALP's lamp, camera and
     * feed. A player who watches a MALP someone else drives may also choose another MALP; one who watches whoever is
     * at their console may only close the screen. Anyone with a tablet's screen open may take MALPs off its list.
     */
    public static void action(final ServerPlayer player, final ConsoleAction action) {
        final Link link = MalpLinks.LINKS.get(player.getUUID());
        if (link == null) {
            return;
        }
        if (action.kind() == ConsoleAction.Kind.CLOSE) {
            MalpLinks.close(player);
            return;
        }
        final boolean allowed = switch (action.kind()) {
            case SELECT -> link.role != Role.CONSOLE_WATCHER;
            case REMOVE -> link.controller instanceof MalpController.Tablet || link.role == Role.DRIVER;
            default -> link.role == Role.DRIVER;
        };
        if (!allowed) {
            return;
        }
        final MinecraftServer server = player.level().getServer();
        switch (action.kind()) {
            case SELECT -> {
                final Optional<UUID> chosen = action.malp().filter(id -> link.controller.malps(server).stream()
                        .anyMatch(entry -> entry.id().equals(id)));
                if (chosen.isPresent() && (!chosen.get().equals(link.selected) || link.orphaned)) {
                    MalpLinks.select(link, chosen.get());
                }
            }
            case REMOVE -> {
                final UUID gone = action.malp().orElse(null);
                if (gone != null && link.controller instanceof MalpController.Tablet tablet
                        && MalpTabletItem.unpair(server, tablet.stack(), gone) && gone.equals(link.selected)) {
                    MalpLinks.select(link, MalpLinks.initial(link, link.controller.malps(server), null));
                } else if (gone != null && link.controller instanceof MalpController.Console) {
                    MalpLinks.unpairAtConsole(link, gone);
                }
            }
            case LAMP -> {
                if (link.viewing != null) {
                    link.viewing.setLamp(!link.viewing.lamp());
                }
            }
            case FEED -> {
                link.feedOn = !link.feedOn;
                MalpLinks.remember(link);
            }
            case CENTER -> {
                if (link.viewing != null) {
                    link.viewing.centerMast();
                }
            }
            default -> {
            }
        }
        MalpLinks.refresh(link);
        MalpLinks.send(link, false, false);
        MalpLinks.updateSpectators(link);
        MalpLinks.updateWatchers(player);
    }

    /** Drives the MALP a player drives from their screen, if its feed is live. */
    public static void drive(final ServerPlayer player, final Drive drive) {
        final Link link = MalpLinks.LINKS.get(player.getUUID());
        final MalpEntity malp = link == null || link.watcher() ? null : link.viewing;
        if (malp == null || !malp.isAlive() || link.feed != Feed.LIVE || !Float.isFinite(drive.forward())
                || !Float.isFinite(drive.turn()) || !Float.isFinite(drive.pan()) || !Float.isFinite(drive.tilt())) {
            return;
        }
        if (drive.forward() != 0 || drive.turn() != 0) {
            malp.drive(drive.forward(), drive.turn());
        }
        malp.aim(Mth.clamp(drive.pan(), -MalpLinks.MAX_AIM, MalpLinks.MAX_AIM),
                Mth.clamp(drive.tilt(), -MalpLinks.MAX_AIM, MalpLinks.MAX_AIM));
    }

    /**
     * Ends a player's link, such as when they close the screen. A tablet's battery pays for what its feed used. The
     * feed on the console's monitor goes, if they were at a console, and those watching them see it go offline.
     */
    public static void close(final ServerPlayer player) {
        final Link link = MalpLinks.LINKS.remove(player.getUUID());
        if (link == null) {
            return;
        }
        MalpLinks.leaveOnMonitor(link);
        MalpLinks.stopViewing(link);
        MalpLinks.pay(link);
        MalpLinks.stopSpectators(link);
        if (link.use != Use.NONE) {
            MalpLinks.broadcastUse(player, Use.NONE);
        }
        MalpLinks.updateWatchers(player);
    }

    // A player leaving a console whose monitor goes on to show the MALP they saw sees it go on there, as those around
    // it do, without the feed beginning again
    private static void leaveOnMonitor(final Link link) {
        final MalpEntity malp = link.viewing;
        if (!link.atConsole() || link.feed != Feed.LIVE || malp == null
                || !(link.controller instanceof MalpController.Console console)) {
            return;
        }
        final MalpConsoleBlockEntity entity = MalpLinks.consoleAt(link.player.level().getServer(), console.pos());
        if (entity == null || !entity.running() || !malp.getUUID().equals(entity.shown())
                || !RemoteViews.leaveOnDisplay(link.player)) {
            return;
        }
        if (!link.watcher()) {
            malp.stop();
        }
        link.viewing = null;
        MalpLinks.MONITORS.computeIfAbsent(console.pos(), at -> new Monitor()).spectators.add(link.player.getUUID());
    }

    /**
     * A console was switched off: everyone at it, or watching whoever is, is taken off it, as though they had closed
     * its screen, and its monitor's feed stops for those near it.
     */
    public static void switchedOff(final MinecraftServer server, final GlobalPos console) {
        final MalpController.Console controller = new MalpController.Console(console);
        for (final Link link : List.copyOf(MalpLinks.LINKS.values())) {
            if (link.controller.equals(controller) && MalpLinks.LINKS.get(link.player.getUUID()) == link) {
                MalpLinks.close(link.player);
                MalpLinks.send(link, false, true);
            }
        }
        MalpLinks.stopMonitor(server, console);
    }

    /** A player got up from a chair: takes them off the console they sat at, and closes its screen. */
    public static void stoodUp(final ServerPlayer player) {
        final Link link = MalpLinks.LINKS.get(player.getUUID());
        if (link != null && link.controller instanceof MalpController.Console) {
            MalpLinks.close(player);
            MalpLinks.send(link, false, true);
        }
    }

    /** Loads a MALP's chunk for a while, as though someone had just stopped watching it. */
    public static void load(final MinecraftServer server, final UUID malp) {
        MalpLinks.LOADED.put(malp, (long) server.getTickCount() + MalpSettings.current().lingerTicks());
        MalpLinks.ticket(server, malp);
    }

    /**
     * Powers down the feed of everyone driving a MALP, which those watching them see too.
     *
     * @return how many feeds it powered down
     */
    public static int powerDown(final UUID malp) {
        int count = 0;
        for (final Link link : List.copyOf(MalpLinks.LINKS.values())) {
            if (!link.watcher() && malp.equals(link.selected) && link.feedOn) {
                link.feedOn = false;
                MalpLinks.refresh(link);
                MalpLinks.send(link, false, false);
                MalpLinks.updateSpectators(link);
                MalpLinks.updateWatchers(link.player);
                count++;
            }
        }
        return count;
    }

    /**
     * Unpairs a MALP from its console. Everyone with that console's screen open sees the MALP leave its list at once,
     * and anyone watching it there loses its feed as when its signal drops, which those watching them see too. Tablets
     * paired with the MALP keep it.
     *
     * @return whether the MALP was paired with a console
     */
    public static boolean unpair(final MalpEntity malp) {
        final GlobalPos console = malp.console();
        malp.setConsole(null);
        if (console == null) {
            return false;
        }
        final MalpController at = new MalpController.Console(console);
        for (final Link link : List.copyOf(MalpLinks.LINKS.values())) {
            if (link.atConsole() && link.controller.equals(at)) {
                MalpLinks.refresh(link);
                MalpLinks.send(link, false, false);
                MalpLinks.updateSpectators(link);
                MalpLinks.updateWatchers(link.player);
            }
        }
        return true;
    }

    // The player driving at a console unpairs a MALP from it, if they own the MALP or are an operator; a MALP whose
    // chunk isn't loaded is loaded, and they are asked to try again
    private static void unpairAtConsole(final Link link, final UUID gone) {
        final ServerPlayer player = link.player;
        final MinecraftServer server = player.level().getServer();
        if (link.controller.malps(server).stream().noneMatch(entry -> entry.id().equals(gone))) {
            return;
        }
        final MalpEntity malp = MalpRegistry.find(server, gone);
        if (malp == null) {
            MalpLinks.load(server, gone);
            player.sendOverlayMessage(Component.translatableWithFallback("message.sgcraft.malp.loading",
                    "That MALP is loading. Try again in a moment").withStyle(ChatFormatting.YELLOW));
            return;
        }
        if (!MalpConfigurator.mayConfigure(player, malp)) {
            player.sendOverlayMessage(Component.translatableWithFallback("message.sgcraft.malp.cannot_configure",
                    "You aren't allowed to configure this MALP").withStyle(ChatFormatting.RED));
            return;
        }
        if (gone.equals(link.selected)) {
            MalpLinks.select(link, null);
        }
        MalpLinks.unpair(malp);
        if (link.selected == null) {
            MalpLinks.select(link, MalpLinks.initial(link, link.controller.malps(server), null));
        }
    }

    /** @return what a player's feed shows, or empty if they have no console's or tablet's screen open */
    public static Optional<Feed> feed(final ServerPlayer player) {
        return Optional.ofNullable(MalpLinks.LINKS.get(player.getUUID())).map(link -> link.feed);
    }

    /** Ends every link and forgets every loaded chunk, as the server stops. */
    public static void clear() {
        MalpLinks.LINKS.clear();
        MalpLinks.LOADED.clear();
        MalpLinks.MONITORS.clear();
        MalpPresence.clear();
    }

    /** Keeps links up to date, once a tick. */
    public static void tick(final MinecraftServer server) {
        final long now = server.getTickCount();
        final MalpSettings settings = MalpSettings.current();
        // A MALP watched from several consoles and tablets uses its idle gate energy once
        final Set<MalpEntity> drained = new HashSet<>();
        final List<Link> links = new ArrayList<>(MalpLinks.LINKS.values());
        // Drivers first, so those watching them follow what they have this tick
        links.sort(Comparator.comparingInt(Link::order));
        for (final Link link : links) {
            if (MalpLinks.LINKS.get(link.player.getUUID()) != link) {
                continue;
            }
            if (!MalpLinks.valid(link)) {
                MalpLinks.close(link.player);
                MalpLinks.send(link, false, true);
                continue;
            }
            if (!link.watcher() && link.selected != null) {
                MalpLinks.LOADED.put(link.selected, now + settings.lingerTicks());
            }
            boolean refresh = now % MalpLinks.SIGNAL_TICKS == 0;
            if (link.controller instanceof MalpController.Tablet && link.feed == Feed.LIVE) {
                link.owed += settings.tabletDrain();
                // A battery just run flat ends the feed now, rather than at the next look at the signal
                refresh |= !link.controller.powered(link.owed);
            }
            if (now % MalpLinks.TABLET_PAY_TICKS == 0) {
                MalpLinks.pay(link);
            }
            if (refresh) {
                MalpLinks.refresh(link);
            }
            if (!link.watcher() && link.viewing != null && link.feed == Feed.LIVE && drained.add(link.viewing)) {
                link.viewing.drain(settings.idleDrain());
            }
            if (now % MalpLinks.STATE_TICKS == 0) {
                MalpLinks.send(link, false, false);
            }
            if (now % MalpLinks.SPECTATOR_TICKS == 0) {
                MalpLinks.updateSpectators(link);
            }
        }
        MalpPresence.update(server, MalpLinks.LOADED.keySet());
        if (now % MalpLinks.TICKET_TICKS == 0) {
            final Iterator<Map.Entry<UUID, Long>> loaded = MalpLinks.LOADED.entrySet().iterator();
            while (loaded.hasNext()) {
                final Map.Entry<UUID, Long> entry = loaded.next();
                if (entry.getValue() < now) {
                    loaded.remove();
                } else {
                    MalpLinks.ticket(server, entry.getKey());
                }
            }
        }
    }

    // A tablet's battery pays for the gate energy its feed has used
    private static void pay(final Link link) {
        if (link.owed > 0 && link.controller instanceof MalpController.Tablet tablet) {
            MalpTabletItem.drain(tablet.stack(), link.owed);
        }
        link.owed = 0;
    }

    // Loads the chosen MALP's chunk now, rather than at the next refresh, if the link drives it
    private static void keepLoaded(final MinecraftServer server, final Link link) {
        if (!link.watcher() && link.selected != null) {
            MalpLinks.load(server, link.selected);
        }
    }

    // A short ticket at the MALP's chunk, renewed while it is wanted, so it follows the MALP; at the chunk it was last
    // seen in, if it isn't loaded
    private static void ticket(final MinecraftServer server, final UUID id) {
        final MalpEntity malp = MalpRegistry.find(server, id);
        if (malp != null && malp.level() instanceof ServerLevel level) {
            level.getChunkSource().addTicketWithRadius(MalpLinks.TICKET, malp.chunkPosition(),
                    MalpLinks.ticketRadius());
            MalpLinks.arena(level, malp);
            return;
        }
        MalpRegistry.get(server).get(id).ifPresent(entry -> {
            final ServerLevel level = server.getLevel(entry.position().dimension());
            if (level != null) {
                level.getChunkSource().addTicketWithRadius(MalpLinks.TICKET, ChunkPos.containing(
                        entry.position().pos()), MalpLinks.ticketRadius());
            }
        });
    }

    // How far out the ticket at a MALP's chunk reaches: far enough that every chunk the camera's view reaches across
    // runs, so the mobs in sight move, with a chunk to spare for a MALP at the edge of its own
    private static int ticketRadius() {
        final int reach = SGServer.current().map(server -> server.config().remoteView().horizontalRadius())
                .orElse(RemoteViewConfig.DEFAULTS.horizontalRadius());
        return Mth.ceil(reach / 16.0) + 1 + MalpLinks.TICKET_LEVEL_DROP;
    }

    // The dragon's fight runs its island only while a player is on it, so a MALP there runs it as a player would,
    // within the distance the fight looks for players from its center
    private static void arena(final ServerLevel level, final MalpEntity malp) {
        if (level.getDragonFight() != null
                && malp.position().closerThan(MalpLinks.ARENA_CENTER, MalpLinks.ARENA_RANGE)) {
            level.getChunkSource().addTicketWithRadius(MalpLinks.TICKET, new ChunkPos(0, 0),
                    EnderDragonFight.ARENA_TICKET_LEVEL);
        }
    }

    // The player is still there and alive, and still with their console or tablet
    private static boolean valid(final Link link) {
        final ServerPlayer player = link.player;
        return !player.isRemoved() && !player.hasDisconnected() && player.isAlive() && link.controller.valid(player);
    }

    // Works out the signal and what the feed shows, and starts or stops the view to match; a watcher's come from the
    // player they follow
    private static void refresh(final Link link) {
        switch (link.role) {
            case CONSOLE_WATCHER -> MalpLinks.followConsole(link);
            case MALP_WATCHER -> MalpLinks.followMalp(link);
            case DRIVER -> MalpLinks.refreshDriver(link);
        }
    }

    private static void refreshDriver(final Link link) {
        final ServerPlayer player = link.player;
        MalpEntity malp = link.selected == null ? null : MalpRegistry.find(player.level().getServer(), link.selected);
        if (link.selected != null && !link.controller.lists(link.selected, malp)) {
            // Unpaired since
            link.selected = null;
            malp = null;
        }
        final boolean powered = link.controller.powered(link.owed);
        link.signal = malp == null || !powered ? MalpSignal.NONE : link.controller.signal(player,
                malp.level().dimension(), malp.position(), MalpSettings.current().signalRange());
        if (!powered) {
            link.feed = Feed.NO_TABLET_POWER;
        } else if (!link.feedOn) {
            link.feed = Feed.OFF;
        } else if (malp == null || !link.signal.reaches()) {
            link.feed = Feed.NO_SIGNAL;
        } else if (!malp.hasPower()) {
            link.feed = Feed.NO_POWER;
        } else {
            link.feed = Feed.LIVE;
        }
        MalpLinks.view(link, link.feed == Feed.LIVE ? malp : null);
    }

    // Someone watching whoever is at their console sees what that player sees, until that player leaves, when their
    // feed goes offline for good
    private static void followConsole(final Link link) {
        final Link occupant = link.orphaned || link.followed == null ? null : MalpLinks.LINKS.get(link.followed);
        if (occupant == null || !occupant.atConsole() || !occupant.controller.equals(link.controller)) {
            MalpLinks.orphan(link);
            return;
        }
        link.selected = occupant.selected;
        link.signal = occupant.signal;
        link.feed = occupant.feed;
        MalpLinks.view(link, occupant.feed == Feed.LIVE ? occupant.viewing : null);
    }

    // Someone watching a MALP someone else drives sees the driver's feed while their own console or tablet reaches the
    // MALP and has power, until the driver leaves or turns to another MALP, when their feed goes offline
    private static void followMalp(final Link link) {
        final Link driver = link.orphaned || link.followed == null ? null : MalpLinks.LINKS.get(link.followed);
        final MalpEntity malp = link.selected == null ? null
                : MalpRegistry.find(link.player.level().getServer(), link.selected);
        if (driver == null || driver.watcher() || link.selected == null
                || !Objects.equals(driver.selected, link.selected) || !link.controller.lists(link.selected, malp)) {
            MalpLinks.orphan(link);
            return;
        }
        final boolean powered = link.controller.powered(link.owed);
        link.signal = malp == null || !powered ? MalpSignal.NONE : link.controller.signal(link.player,
                malp.level().dimension(), malp.position(), MalpSettings.current().signalRange());
        link.feed = !powered ? Feed.NO_TABLET_POWER : !link.signal.reaches() ? Feed.NO_SIGNAL : driver.feed;
        MalpLinks.view(link, link.feed == Feed.LIVE ? driver.viewing : null);
    }

    private static void orphan(final Link link) {
        link.orphaned = true;
        link.selected = null;
        link.signal = MalpSignal.NONE;
        link.feed = Feed.OFF;
        MalpLinks.stopViewing(link);
    }

    // Starts the player's view of a MALP, or stops it for none
    private static void view(final Link link, final @Nullable MalpEntity malp) {
        if (malp == null) {
            MalpLinks.stopViewing(link);
            return;
        }
        // A MALP that went through a gate is a new entity with the same UUID, which the view follows itself
        if (link.viewing != null && !link.viewing.getUUID().equals(malp.getUUID())) {
            MalpLinks.stopViewing(link);
        }
        link.viewing = malp;
        // Started again if the view ended on its own, such as when the player's client wasn't ready for it; one the
        // console's monitor already showed them is taken from there rather than begun again
        if (!RemoteViews.watching(link.player, malp)) {
            final BlockPos shownAt = link.controller instanceof MalpController.Console console
                    ? console.pos().pos() : null;
            if (shownAt == null || !RemoteViews.takeFromDisplay(link.player, shownAt, malp.getUUID())) {
                RemoteViews.start(link.player, malp, shownAt);
            }
        }
    }

    // Brings everyone watching a player up to date with them, or tells them the player has gone, and then those
    // watching a watcher at a console
    private static void updateWatchers(final ServerPlayer followed) {
        for (final Link link : List.copyOf(MalpLinks.LINKS.values())) {
            if (link.watcher() && followed.getUUID().equals(link.followed)) {
                MalpLinks.refresh(link);
                MalpLinks.send(link, false, false);
                if (link.atConsole()) {
                    MalpLinks.updateSpectators(link);
                    MalpLinks.updateWatchers(link.player);
                }
            }
        }
    }

    /**
     * Keeps the monitor of a console up to date while no one is at it: shows the players nearest it the feed of the
     * MALP last chosen at it, or else of the first MALP paired with it, while the console reaches the MALP and the MALP
     * has power, as many players as the settings allow, within the settings' range. While anyone is that near, the
     * MALP's chunk is kept loaded and the MALP uses its idle gate energy, unless someone drives it. The console calls
     * this each tick.
     *
     * @param pos the console's middle lower part
     */
    public static void monitor(final ServerLevel level, final BlockPos pos, final MalpConsoleBlockEntity console) {
        final MinecraftServer server = level.getServer();
        final GlobalPos at = GlobalPos.of(level.dimension(), pos);
        if (!console.running()) {
            // Switched off, or without power, its monitor shows nothing and no MALP is kept loaded for it
            if (MalpLinks.MONITORS.containsKey(at)) {
                MalpLinks.stopMonitor(server, at);
            }
            return;
        }
        final MalpController.Console controller = new MalpController.Console(at);
        final Link occupant = MalpLinks.occupantAt(controller);
        if (occupant != null) {
            // The monitor shows the feed of the player at the console, to those who saw it before too
            final Monitor monitor = MalpLinks.MONITORS.remove(at);
            if (monitor != null) {
                occupant.spectators.addAll(monitor.spectators);
                MalpLinks.updateSpectators(occupant);
            }
            return;
        }
        if ((server.getTickCount() + pos.hashCode()) % MalpLinks.SPECTATOR_TICKS == 0) {
            MalpLinks.refreshMonitor(level, pos, console);
        }
        // Its idle gate energy, which a driver's feed pays for instead
        final Monitor monitor = MalpLinks.MONITORS.get(at);
        if (monitor != null && monitor.malp != null && monitor.malp.isAlive()
                && MalpLinks.liveDriverOf(monitor.malp.getUUID()) == null) {
            monitor.malp.drain(MalpSettings.current().idleDrain());
        }
    }

    /**
     * Looks again at who is near a console no one is at, and what its monitor can show them, as {@link #monitor}
     * does every few ticks, and starts or stops the feed on its monitor to match.
     *
     * @param pos the console's middle lower part
     */
    public static void refreshMonitor(final ServerLevel level, final BlockPos pos,
            final MalpConsoleBlockEntity console) {
        final MinecraftServer server = level.getServer();
        final GlobalPos at = GlobalPos.of(level.dimension(), pos);
        final MalpController.Console controller = new MalpController.Console(at);
        if (MalpLinks.occupantAt(controller) != null) {
            return;
        }
        Monitor monitor = MalpLinks.MONITORS.get(at);
        final List<ServerPlayer> near = MalpLinks.near(level, pos, monitor == null ? Set.of() : monitor.spectators,
                controller);
        if (near.isEmpty()) {
            if (monitor != null) {
                MalpLinks.stopMonitor(server, at);
            }
            return;
        }
        if (monitor == null) {
            monitor = new Monitor();
            MalpLinks.MONITORS.put(at, monitor);
        }
        MalpLinks.monitored(server, controller, console.shown(), console.feedOff(), monitor);
        MalpLinks.show(server, pos, monitor, monitor.malp == null ? List.of() : near);
        final MalpEntity malp = monitor.malp;
        MalpLinks.sendMonitor(near, pos, controller, monitor.selected, monitor.feed,
                malp == null ? Readout.NONE : MalpLinks.readout(malp, monitor.signal), false);
    }

    // Works out what a console's monitor shows with no one at it: the MALP last chosen at it, if still paired with it,
    // or else the first paired with it, kept loaded, while it is reached by the console's signal and has power; with
    // no MALP paired, or the feed powered down at the console, the feed is offline
    private static void monitored(final MinecraftServer server, final MalpController.Console console,
            final @Nullable UUID shown, final boolean feedOff, final Monitor monitor) {
        final List<MalpRegistry.Entry> paired = console.malps(server);
        final UUID id = shown != null && paired.stream().anyMatch(entry -> entry.id().equals(shown)) ? shown
                : paired.isEmpty() ? null : paired.getFirst().id();
        monitor.selected = id;
        monitor.malp = null;
        monitor.feed = id == null ? Feed.OFF : Feed.NO_SIGNAL;
        monitor.signal = MalpSignal.NONE;
        if (id == null || feedOff) {
            monitor.feed = Feed.OFF;
            return;
        }
        final long until = server.getTickCount() + MalpLinks.MONITOR_LOAD_TICKS;
        MalpLinks.LOADED.merge(id, until, Math::max);
        final MalpEntity malp = MalpRegistry.find(server, id);
        if (malp == null) {
            // Not loaded yet, so loaded now, to show at the next look
            MalpLinks.ticket(server, id);
            return;
        }
        if (!console.lists(id, malp)) {
            return;
        }
        final MalpSignal signal = MalpSignal.between(console.pos(), malp.level().dimension(), malp.position(),
                MalpSettings.current().signalRange());
        if (!signal.reaches()) {
            return;
        }
        monitor.signal = signal;
        if (!malp.hasPower()) {
            monitor.feed = Feed.NO_POWER;
            return;
        }
        monitor.feed = Feed.LIVE;
        monitor.malp = malp;
    }

    // Tells the players near a console what its monitor shows, for their clients to draw around the feed, or in place
    // of it
    private static void sendMonitor(final List<ServerPlayer> near, final BlockPos console,
            final MalpController controller, final @Nullable UUID selected, final Feed feed, final Readout readout,
            final boolean inUse) {
        if (near.isEmpty()) {
            return;
        }
        final MonitorState state = new MonitorState(console, MalpLinks.infos(near.getFirst().level().getServer(),
                controller), Optional.ofNullable(selected), feed, readout, inUse);
        for (final ServerPlayer other : near) {
            SGNetwork.send(other, state);
        }
    }

    // The MALPs paired with a console or tablet, as its screen and its monitor list them
    private static List<MalpInfo> infos(final MinecraftServer server, final MalpController controller) {
        final List<MalpInfo> malps = new ArrayList<>();
        for (final MalpRegistry.Entry entry : controller.malps(server)) {
            final long capacity = BatteryItem.capacity();
            malps.add(new MalpInfo(entry.id(), entry.name(), entry.position().dimension().identifier().toString(),
                    entry.position().pos(), capacity <= 0 ? 0 : (int) Math.round(100.0 * entry.energy() / capacity)));
        }
        return malps;
    }

    // The link whose live feed comes from a MALP, as its driver's, if any
    private static @Nullable Link liveDriverOf(final UUID malp) {
        for (final Link link : MalpLinks.LINKS.values()) {
            if (link.role == Role.DRIVER && link.feed == Feed.LIVE && malp.equals(link.selected)) {
                return link;
            }
        }
        return null;
    }

    // Shows a monitor's MALP to the first of the players given that the settings allow, and stops it for the rest
    private static void show(final MinecraftServer server, final BlockPos at, final Monitor monitor,
            final List<ServerPlayer> near) {
        final Set<UUID> wanted = new HashSet<>();
        final MalpEntity malp = monitor.malp;
        if (malp != null) {
            for (final ServerPlayer other : near.subList(0, Math.min(near.size(),
                    MalpSettings.current().consoleSpectators()))) {
                wanted.add(other.getUUID());
                if (!RemoteViews.watchingOnDisplay(other, at, malp)) {
                    RemoteViews.startOnDisplay(other, at, malp);
                }
            }
        }
        for (final UUID gone : monitor.spectators) {
            final ServerPlayer other = server.getPlayerList().getPlayer(gone);
            if (!wanted.contains(gone) && other != null) {
                RemoteViews.stopOnDisplay(other, at);
            }
        }
        monitor.spectators = wanted;
    }

    // Stops the feed a console's monitor shows with no one at it, for everyone who sees it
    private static void stopMonitor(final MinecraftServer server, final GlobalPos at) {
        final Monitor monitor = MalpLinks.MONITORS.remove(at);
        if (monitor != null) {
            monitor.malp = null;
            MalpLinks.show(server, at.pos(), monitor, List.of());
        }
    }

    /** Stops the feed on a console's monitor for everyone who sees it, as the console goes or its chunk unloads. */
    public static void monitorGone(final MinecraftServer server, final GlobalPos console) {
        MalpLinks.stopMonitor(server, console);
    }

    // The players near a console who may see its monitor, nearest first: alive, able to show a feed, not at it, and
    // within the settings' range of it, or a little past it if they already see it
    private static List<ServerPlayer> near(final ServerLevel level, final BlockPos at, final Set<UUID> seeing,
            final MalpController console) {
        final MalpSettings settings = MalpSettings.current();
        final List<ServerPlayer> near = new ArrayList<>();
        if (settings.consoleSpectators() <= 0) {
            return near;
        }
        final Vec3 center = Vec3.atCenterOf(at);
        for (final ServerPlayer other : level.players()) {
            final double range = settings.spectatorRange()
                    + (seeing.contains(other.getUUID()) ? MalpLinks.SPECTATOR_MARGIN : 0);
            final Link theirs = MalpLinks.LINKS.get(other.getUUID());
            if (other.isAlive() && !other.hasDisconnected()
                    && (theirs == null || !theirs.controller.equals(console))
                    && other.position().distanceToSqr(center) <= range * range && RemoteViews.canView(other)) {
                near.add(other);
            }
        }
        near.sort(Comparator.comparingDouble(other -> other.position().distanceToSqr(center)));
        return near;
    }

    // Shows the live feed of the player at a console on its monitor to the nearest players around it who aren't at
    // it, as many as the settings allow, and stops it for the rest
    private static void updateSpectators(final Link link) {
        if (!link.atConsole() || !(link.controller instanceof MalpController.Console console)) {
            return;
        }
        final MalpSettings settings = MalpSettings.current();
        final MalpEntity malp = link.feed == Feed.LIVE ? link.viewing : null;
        final ServerLevel level = link.player.level().getServer().getLevel(console.pos().dimension());
        final BlockPos at = console.pos().pos();
        final Set<UUID> wanted = new HashSet<>();
        final List<ServerPlayer> near = level == null ? List.of()
                : MalpLinks.near(level, at, link.spectators, link.controller);
        if (malp != null) {
            for (final ServerPlayer other : near.subList(0, Math.min(near.size(), settings.consoleSpectators()))) {
                wanted.add(other.getUUID());
                if (!RemoteViews.watchingOnDisplay(other, at, malp)) {
                    RemoteViews.startOnDisplay(other, at, malp);
                }
            }
        }
        final MalpEntity seen = link.viewing;
        MalpLinks.sendMonitor(near, at, link.controller, link.selected, link.feed,
                seen == null || !link.signal.reaches() ? Readout.NONE : MalpLinks.readout(seen, link.signal), true);
        for (final UUID gone : link.spectators) {
            if (!wanted.contains(gone)) {
                final ServerPlayer other = link.player.level().getServer().getPlayerList().getPlayer(gone);
                if (other != null) {
                    RemoteViews.stopOnDisplay(other, at);
                }
            }
        }
        link.spectators = wanted;
    }

    // Stops the feed on a console's monitor for those who saw the feed of the player at it; with the console still
    // there, they go on seeing it while its monitor shows them the feed it shows with no one at it
    private static void stopSpectators(final Link link) {
        if (link.controller instanceof MalpController.Console console) {
            final MinecraftServer server = link.player.level().getServer();
            if (link.atConsole() && !link.spectators.isEmpty() && MalpLinks.consoleAt(server, console.pos()) != null) {
                MalpLinks.MONITORS.computeIfAbsent(console.pos(), at -> new Monitor()).spectators
                        .addAll(link.spectators);
                link.spectators = new HashSet<>();
                return;
            }
            for (final UUID id : link.spectators) {
                final ServerPlayer other = link.player.level().getServer().getPlayerList().getPlayer(id);
                if (other != null) {
                    RemoteViews.stopOnDisplay(other, console.pos().pos());
                }
            }
        }
        link.spectators = new HashSet<>();
    }

    // Stops the player's view; a driver's MALP stops too, while one a watcher sees is left to its driver
    private static void stopViewing(final Link link) {
        if (link.viewing != null) {
            if (!link.watcher()) {
                link.viewing.stop();
            }
            link.viewing = null;
            RemoteViews.stop(link.player);
        }
    }

    private static void send(final Link link, final boolean open, final boolean close) {
        final MinecraftServer server = link.player.level().getServer();
        final List<MalpInfo> malps = MalpLinks.infos(server, link.controller);
        final MalpEntity malp = link.selected == null ? null : MalpRegistry.find(server, link.selected);
        final Readout readout = malp == null || !link.signal.reaches() ? Readout.NONE
                : MalpLinks.readout(malp, link.signal);
        final Optional<BlockPos> console = link.controller instanceof MalpController.Console at
                ? Optional.of(at.pos().pos()) : Optional.empty();
        final int tabletBattery = link.controller instanceof MalpController.Tablet tablet
                ? MalpTabletItem.percent(tablet.stack()) : -1;
        SGNetwork.send(link.player, new ConsoleState(open, close, console, malps, Optional.ofNullable(link.selected),
                link.feed, readout, link.role, tabletBattery));
    }

    /** @return what a MALP reports of where it is, as the feed of a console or tablet shows it */
    public static Readout readout(final MalpEntity malp, final MalpSignal signal) {
        final ServerLevel level = (ServerLevel) malp.level();
        final BlockPos pos = malp.blockPosition();
        final Holder<Biome> biome = level.getBiome(pos);
        final int hostiles = level.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(MalpLinks.HOSTILE_RANGE),
                mob -> mob instanceof Enemy && mob.isAlive()).size();
        return new Readout(signal.kind().name(), MalpLinks.relay(signal), level.dimension().identifier().toString(),
                biome.unwrapKey().map(key -> key.identifier().toString()).orElse(""),
                biome.value().getBaseTemperature(), (int) (level.getDefaultClockTime() % 24000),
                level.getMaxLocalRawBrightness(pos), hostiles,
                malp.battery().isEmpty() ? 0 : BatteryItem.percent(malp.battery()), malp.lamp(),
                malp.options().powerRequired(), pos, malp.getYRot(), MalpLinks.nearGate(malp));
    }

    // The gate nearest a MALP in its world, within the signal's range; one that hides its address is named by none
    private static Optional<NearGate> nearGate(final MalpEntity malp) {
        final ResourceKey<Level> world = malp.level().dimension();
        final double range = MalpSettings.current().signalRange();
        @Nullable Stargate nearest = null;
        double best = range * range;
        for (final Stargate gate : SGCraftApi.get().stargates().in(world)) {
            final double distance = malp.position().distanceToSqr(Vec3.atCenterOf(gate.position().pos()));
            if (distance <= best) {
                best = distance;
                nearest = gate;
            }
        }
        if (nearest == null) {
            return Optional.empty();
        }
        final Vec3 to = Vec3.atCenterOf(nearest.position().pos()).subtract(malp.position());
        final String address = nearest.get(GateProperties.ADDRESS_VISIBLE).orElse(true)
                ? MalpLinks.address(nearest.position(), world) : "";
        return Optional.of(new NearGate(address, (int) Math.round(Math.sqrt(best)),
                (float) Math.toDegrees(Math.atan2(-to.x, to.z))));
    }

    /** @return the addresses a relayed signal goes through, each as dialed from the other gate's world */
    public static String relay(final MalpSignal signal) {
        if (signal.near().isEmpty() || signal.far().isEmpty()) {
            return "";
        }
        final GlobalPos near = signal.near().get();
        final GlobalPos far = signal.far().get();
        return MalpLinks.address(near, far.dimension()) + " > " + MalpLinks.address(far, near.dimension());
    }

    private static String address(final GlobalPos gate, final ResourceKey<Level> from) {
        return SGCraftApi.get().addressing().addressOf(gate, from).map(Address::toString).orElse("????");
    }
}
