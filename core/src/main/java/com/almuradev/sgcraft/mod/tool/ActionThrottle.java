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

package com.almuradev.sgcraft.mod.tool;

import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * How often a tool's screen has its actions answered for each player. An answer lists every entry with the gate it
 * reaches, which looks through every gate the server knows of, so however often a client sends them, a screen's
 * refreshes are answered at most once each {@code refreshTicks}, and at most {@code buttonsPerSecond} of its buttons
 * in any second. An action beyond that is dropped; the screen's next refresh catches up. Used on the server thread
 * only.
 */
public final class ActionThrottle {

    private static final int SECOND = 20;

    // Every throttle, so a player who leaves, or a server that stops, is forgotten by each
    private static final List<ActionThrottle> ALL = new CopyOnWriteArrayList<>();

    private final int refreshTicks;
    private final int buttonsPerSecond;
    // What each player's screen has had answered, in server ticks
    private final Map<UUID, Answered> answered = new HashMap<>();

    public ActionThrottle(final int refreshTicks, final int buttonsPerSecond) {
        this.refreshTicks = refreshTicks;
        this.buttonsPerSecond = buttonsPerSecond;
        ActionThrottle.ALL.add(this);
    }

    /**
     * Notes an action from a player's screen.
     *
     * @param refresh whether it only asks for the screen's state again
     * @return whether to answer it
     */
    public boolean allows(final ServerPlayer player, final boolean refresh) {
        final int now = player.level().getServer().getTickCount();
        final Answered last = this.answered.computeIfAbsent(player.getUUID(), id -> new Answered());
        if (refresh) {
            if (last.refresh != null && now >= last.refresh && now - last.refresh < this.refreshTicks) {
                return false;
            }
            last.refresh = now;
            return true;
        }
        if (last.second == null || now < last.second || now - last.second >= ActionThrottle.SECOND) {
            last.second = now;
            last.buttons = 0;
        }
        return ++last.buttons <= this.buttonsPerSecond;
    }

    /** Forgets a player, as they leave the server. */
    public static void forget(final UUID player) {
        ActionThrottle.ALL.forEach(throttle -> throttle.answered.remove(player));
    }

    /** Forgets every player, as the server stops, since the next server counts its ticks from 0 again. */
    public static void clear() {
        ActionThrottle.ALL.forEach(throttle -> throttle.answered.clear());
    }

    // When a player's screen last had a refresh answered, and how many buttons in the second that began when
    private static final class Answered {

        private @Nullable Integer refresh;
        private @Nullable Integer second;
        private int buttons;
    }
}
