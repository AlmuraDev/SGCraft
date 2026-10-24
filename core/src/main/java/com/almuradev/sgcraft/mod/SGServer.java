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

package com.almuradev.sgcraft.mod;

import com.almuradev.sgcraft.mod.access.SGGateAccess;
import com.almuradev.sgcraft.mod.address.SGAddressing;
import com.almuradev.sgcraft.mod.addressbook.DefaultAddresses;
import com.almuradev.sgcraft.mod.addressbook.SGAddressBooks;
import com.almuradev.sgcraft.mod.computer.DialingComputers;
import com.almuradev.sgcraft.mod.gate.SGStargates;
import com.almuradev.sgcraft.mod.malp.MalpLinks;
import com.almuradev.sgcraft.mod.obelisk.Obelisks;
import com.almuradev.sgcraft.mod.settings.SettingsActions;
import com.almuradev.sgcraft.mod.settings.SharedSettings;
import com.almuradev.sgcraft.mod.tool.ActionThrottle;
import com.almuradev.sgcraft.mod.tool.ToolActions;
import com.almuradev.sgcraft.mod.view.RemoteViews;
import com.almuradev.sgcraft.mod.zat.Zat;
import com.almuradev.sgcraft.mod.zpm.ZpmLinks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;

/**
 * What SGCraft keeps while a server runs. Each loader calls {@link #starting}, {@link #tick} and {@link #stopped}
 * from its own server events.
 */
public final class SGServer {

    /** Who stands for the server's console, as the owner of a selection. */
    public static final UUID CONSOLE = new UUID(0, 0);

    // How often the turns at computers that are gone are looked for, in ticks
    private static final int SWEEP_TICKS = 20;

    private static @Nullable SGServer current;

    private final MinecraftServer server;
    private final SGAddressing addressing;
    private final SGStargates stargates;
    private final SGConfig config;
    private final SGAddressBooks addressBooks;
    // The addresses new PDDs and tablets start with
    private List<DefaultAddresses.Entry> defaultAddresses;
    // The gate each player, or the console, has selected for commands, until the server stops
    private final Map<UUID, GlobalPos> selections = new HashMap<>();
    // Work to do at the start of the next tick, once whatever asked for it has finished
    private final Queue<Runnable> nextTick = new ArrayDeque<>();

    private SGServer(final MinecraftServer server) {
        this.server = server;
        this.config = SGConfig.load();
        this.addressing = new SGAddressing(server, this.config);
        this.stargates = new SGStargates(server, this.addressing, this.config);
        this.addressBooks = new SGAddressBooks(server);
        this.defaultAddresses = DefaultAddresses.load();
    }

    /** @return the running server's state, or empty if no server is running */
    public static Optional<SGServer> current() {
        return Optional.ofNullable(SGServer.current);
    }

    /**
     * @return the running server's state
     * @throws IllegalStateException if no server is running
     */
    public static SGServer require() {
        final SGServer server = SGServer.current;
        if (server == null) {
            throw new IllegalStateException("No server is running");
        }
        return server;
    }

    /** Runs {@code task} on the server thread at the start of the next tick. */
    public void nextTick(final Runnable task) {
        this.nextTick.add(task);
    }

    public static void starting(final MinecraftServer server) {
        SGServer.current = new SGServer(server);
        SGServer.current.stargates.restore();
        SGCraft.LOG.info("{} knows of {} gates", SGCraft.MOD_NAME,
                SGServer.current.stargates.store().gates().size());
    }

    public static void tick(final MinecraftServer server) {
        final SGServer current = SGServer.current;
        if (current != null && current.server == server) {
            for (int i = current.nextTick.size(); i > 0; i--) {
                current.nextTick.remove().run();
            }
            current.stargates.tick();
            SharedSettings.tick(current);
            RemoteViews.tick(server);
            MalpLinks.tick(server);
            Zat.tick(server);
            ToolActions.tickGdo(server);
            if (server.getTickCount() % SGServer.SWEEP_TICKS == 0) {
                DialingComputers.sweep(server);
            }
        }
    }

    /**
     * Sends a player who joins the settings their client reads ({@link SharedSettings}), and tells an operator what
     * changed in the stargate addresses when the server started, or what in the address settings it couldn't follow,
     * so they hear of it without reading the server log.
     */
    public static void joined(final ServerPlayer player) {
        final SGServer current = SGServer.current;
        if (current == null) {
            return;
        }
        SharedSettings.joined(current, player);
        if (!SGGateAccess.isOperator(player)) {
            return;
        }
        for (final Component notice : current.addressing.notices()) {
            player.sendSystemMessage(Component.literal("[SGCraft] ").append(notice).withStyle(ChatFormatting.RED));
        }
    }

    /** Forgets what SGCraft keeps for a player, as they leave the server. */
    public static void left(final ServerPlayer player) {
        if (SGServer.current == null) {
            return;
        }
        DialingComputers.left(player);
        ActionThrottle.forget(player.getUUID());
        Obelisks.forget(player.getUUID());
        ZpmLinks.forget(player.getUUID());
        SettingsActions.forget(player.getUUID());
        ToolActions.forget(player.getUUID());
    }

    public static void stopped(final MinecraftServer server) {
        final SGServer current = SGServer.current;
        if (current != null && current.server == server) {
            current.stargates.stop();
            RemoteViews.clear();
            MalpLinks.clear();
            SharedSettings.stopped();
            // The next server, in single player, counts its ticks from 0 again
            DialingComputers.clear();
            ActionThrottle.clear();
            Obelisks.clear();
            ZpmLinks.clear();
            SettingsActions.clear();
            ToolActions.clear();
            Zat.clear();
            SGServer.current = null;
        }
    }

    public MinecraftServer server() {
        return this.server;
    }

    public SGAddressing addressing() {
        return this.addressing;
    }

    public SGStargates stargates() {
        return this.stargates;
    }

    public SGAddressBooks addressBooks() {
        return this.addressBooks;
    }

    /** @return the server's settings, as read when it started, with any dial costs changed since */
    public SGConfig config() {
        return this.stargates.config();
    }

    /** @return the addresses new PDDs and gate control tablets start with */
    public List<DefaultAddresses.Entry> defaultAddresses() {
        return this.defaultAddresses;
    }

    /**
     * Sets the addresses new PDDs and gate control tablets start with, and writes them to their file.
     *
     * @return whether the file was written
     */
    public boolean setDefaultAddresses(final List<DefaultAddresses.Entry> addresses) {
        this.defaultAddresses = List.copyOf(addresses);
        return DefaultAddresses.save(this.defaultAddresses);
    }

    /** @return the gate someone has selected for commands */
    public Optional<GlobalPos> selection(final UUID who) {
        return Optional.ofNullable(this.selections.get(who));
    }

    public void select(final UUID who, final GlobalPos gate) {
        this.selections.put(who, gate);
    }

    /** @return the gate they had selected */
    public Optional<GlobalPos> deselect(final UUID who) {
        return Optional.ofNullable(this.selections.remove(who));
    }
}
