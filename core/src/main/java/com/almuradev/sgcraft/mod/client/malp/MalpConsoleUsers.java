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

package com.almuradev.sgcraft.mod.client.malp;

import com.almuradev.sgcraft.mod.malp.MalpPayloads.ConsoleUse;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.Use;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * The players this client knows to be at a MALP console or looking at a MALP tablet, by entity id, as the server says.
 * The player renderer draws them working the console's controls or holding the tablet. What it knows is forgotten
 * when the client leaves the server.
 */
public final class MalpConsoleUsers {

    private static final Map<Integer, Use> USERS = new HashMap<>();
    // The connection the ids came over; entity ids mean nothing on any other
    private static WeakReference<@Nullable ClientPacketListener> session = new WeakReference<>(null);

    private MalpConsoleUsers() {
    }

    /** Takes what the server says about a player. */
    public static void update(final ConsoleUse use) {
        final Map<Integer, Use> users = MalpConsoleUsers.users();
        if (use.use() == Use.NONE) {
            users.remove(use.player());
        } else {
            users.put(use.player(), use.use());
        }
    }

    /** @return how to draw the player with an entity id: at a console, holding a tablet, or as usual */
    public static Use use(final int id) {
        return MalpConsoleUsers.USERS.isEmpty() ? Use.NONE : MalpConsoleUsers.users().getOrDefault(id, Use.NONE);
    }

    private static Map<Integer, Use> users() {
        final ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (MalpConsoleUsers.session.get() != connection) {
            MalpConsoleUsers.USERS.clear();
            MalpConsoleUsers.session = new WeakReference<>(connection);
        }
        return MalpConsoleUsers.USERS;
    }
}
