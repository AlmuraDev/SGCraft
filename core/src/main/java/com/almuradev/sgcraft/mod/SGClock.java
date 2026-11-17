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

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The game time that server and clients agree on: the overworld's. Clients are only ever sent the overworld's time,
 * while a world of its own (as Sponge gives each) counts its own, so a time a server sends a client, such as when
 * something started, must be counted in the overworld's or it is never due there.
 */
public final class SGClock {

    private SGClock() {
    }

    /** @return the overworld's game time on a server, or the time a client was sent; 0 without a world */
    public static long now(final @Nullable Level level) {
        if (level == null) {
            return 0;
        }
        final MinecraftServer server = level.getServer();
        return server == null || level.isClientSide() ? level.getGameTime() : server.overworld().getGameTime();
    }
}
