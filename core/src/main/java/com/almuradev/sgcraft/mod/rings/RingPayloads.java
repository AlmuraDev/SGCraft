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

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.network.SGPacket;
import net.minecraft.network.FriendlyByteBuf;

/** What transport rings send between the server and clients, beyond the screens they share with obelisks. */
public final class RingPayloads {

    private RingPayloads() {
    }

    /**
     * A ring transport holding the player still, or letting them go. While held, their movement keys don't move them;
     * they can still look around.
     *
     * @param held whether they are held, rather than let go
     */
    public record Hold(boolean held) implements SGPacket {

        public static final Type<Hold> TYPE = new Type<>(SGCraft.id("ring_hold"));

        public Hold(final FriendlyByteBuf buf) {
            this(buf.readBoolean());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeBoolean(this.held);
        }

        @Override
        public Type<Hold> type() {
            return Hold.TYPE;
        }
    }
}
