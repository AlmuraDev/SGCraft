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

package com.almuradev.sgcraft.mod.computer;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.network.SGPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

/** What a dialing computer's screen and the server tell each other. */
public final class ComputerPayloads {

    private ComputerPayloads() {
    }

    /**
     * Opens or closes a player's dialing computer screen: opened as they sit down at the computer, and closed as
     * they get up.
     *
     * @param computer the computer's middle lower part
     * @param open whether to open the screen, or close it
     */
    public record Screen(BlockPos computer, boolean open) implements SGPacket {

        public static final Type<Screen> TYPE = new Type<>(SGCraft.id("dialing_computer_screen"));

        public Screen(final FriendlyByteBuf buf) {
            this(buf.readBlockPos(), buf.readBoolean());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeBlockPos(this.computer);
            buf.writeBoolean(this.open);
        }

        @Override
        public Type<Screen> type() {
            return Screen.TYPE;
        }
    }

    /**
     * A button on a dialing computer's screen.
     *
     * @param computer the computer's middle lower part
     * @param kind what the button does
     * @param address the address to dial, for {@link Kind#DIAL_SLOW} and {@link Kind#DIAL_FAST}
     */
    public record Action(BlockPos computer, Kind kind, String address) implements SGPacket {

        /**
         * What the button does: dials the address the slow way, the ring turning to each symbol and each chevron
         * locking in turn, or the fast way, every chevron locking at once; shuts the gate down; opens or shuts the
         * iris; or closes the screen, which gets the player up from the chair.
         */
        public enum Kind {
            DIAL_SLOW, DIAL_FAST, ABORT, IRIS, CLOSE
        }

        public static final Type<Action> TYPE = new Type<>(SGCraft.id("dialing_computer_action"));

        public Action(final FriendlyByteBuf buf) {
            this(buf.readBlockPos(), buf.readEnum(Kind.class), buf.readUtf(32));
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeBlockPos(this.computer);
            buf.writeEnum(this.kind);
            buf.writeUtf(this.address, 32);
        }

        @Override
        public Type<Action> type() {
            return Action.TYPE;
        }
    }
}
