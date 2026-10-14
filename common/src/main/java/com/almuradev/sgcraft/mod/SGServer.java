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

import com.almuradev.sgcraft.SGCraft;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * What SGCraft keeps while a server runs. Each loader calls {@link #starting}, {@link #tick} and {@link #stopped}
 * from its own server events.
 */
public final class SGServer {

    /** Who stands for the server's console, as the owner of a selection. */
    public static final UUID CONSOLE = new UUID(0, 0);

    private static @Nullable SGServer current;

    private final MinecraftServer server;
    private final SGAddressing addressing;
    private final SGStargates stargates;
    // The gate each player, or the console, has selected for commands, until the server stops
    private final Map<UUID, GlobalPos> selections = new HashMap<>();

    private SGServer(final MinecraftServer server) {
        this.server = server;
        this.addressing = new SGAddressing(server);
        this.stargates = new SGStargates(server, this.addressing);
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

    public static void starting(final MinecraftServer server) {
        SGServer.current = new SGServer(server);
        SGServer.current.stargates.restore();
        SGCraft.LOG.info("{} knows of {} gates", SGCraft.MOD_NAME,
                SGServer.current.stargates.store().gates().size());
    }

    public static void tick(final MinecraftServer server) {
        final SGServer current = SGServer.current;
        if (current != null && current.server == server) {
            current.stargates.tick();
        }
    }

    public static void stopped(final MinecraftServer server) {
        final SGServer current = SGServer.current;
        if (current != null && current.server == server) {
            current.stargates.stop();
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
