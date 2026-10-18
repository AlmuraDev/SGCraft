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

import java.util.Optional;

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
     * Something a dialing computer's screen tells the player working it, such as why a dial failed, as a translation
     * key and the text to show without one. A client whose screen isn't open on the computer shows it over the hotbar.
     *
     * @param computer the computer's middle lower part
     */
    public record Notice(BlockPos computer, String key, String fallback) implements SGPacket {

        public static final Type<Notice> TYPE = new Type<>(SGCraft.id("dialing_computer_notice"));

        public Notice(final FriendlyByteBuf buf) {
            this(buf.readBlockPos(), buf.readUtf(), buf.readUtf());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeBlockPos(this.computer);
            buf.writeUtf(this.key);
            buf.writeUtf(this.fallback);
        }

        @Override
        public Type<Notice> type() {
            return Notice.TYPE;
        }
    }

    /**
     * A button on a dialing computer's screen.
     *
     * @param computer the computer's middle lower part
     * @param kind what the button does
     * @param address the address to dial, for {@link Kind#DIAL}, or the mode to dial in, for {@link Kind#MODE}
     */
    public record Action(BlockPos computer, Kind kind, String address) implements SGPacket {

        /**
         * What the button does: dials the address, the way the computer's {@link ComputerDialMode} says; sets that
         * mode; shuts the gate down; opens or shuts the iris; turns the alarm off, or on again; or closes the
         * screen, which gets the player up from the chair or away from the computer.
         */
        public enum Kind {
            DIAL, MODE, ABORT, IRIS, ALARM, CLOSE
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

    /**
     * A computer's settings, a dialing computer's or a MALP console's, for the screen a configurator opens on it.
     *
     * @param open whether to open the screen, rather than only update it if it is still open
     * @param computer the computer's middle lower part
     * @param owner the name of the player it belongs to, when last seen, or empty if it belongs to no one
     * @param requiresPower whether the computer's own setting says it needs power
     * @param serverRequiresPower whether the server's settings say computers need power at all
     * @param saved whether this answers changes that were made
     */
    public record ConfigState(boolean open, BlockPos computer, Optional<String> owner, boolean requiresPower,
            boolean serverRequiresPower, boolean saved) implements SGPacket {

        public static final Type<ConfigState> TYPE = new Type<>(SGCraft.id("computer_config_state"));

        public ConfigState(final FriendlyByteBuf buf) {
            this(buf.readBoolean(), buf.readBlockPos(), buf.readOptional(FriendlyByteBuf::readUtf), buf.readBoolean(),
                    buf.readBoolean(), buf.readBoolean());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeBoolean(this.open);
            buf.writeBlockPos(this.computer);
            buf.writeOptional(this.owner, FriendlyByteBuf::writeUtf);
            buf.writeBoolean(this.requiresPower);
            buf.writeBoolean(this.serverRequiresPower);
            buf.writeBoolean(this.saved);
        }

        @Override
        public Type<ConfigState> type() {
            return ConfigState.TYPE;
        }
    }

    /**
     * The settings a computer's configurator screen asks for. The server checks again that the player may change them.
     *
     * @param computer the computer's middle lower part
     * @param refresh true to change nothing and only send the settings again
     * @param requiresPower whether the computer needs power
     */
    public record ConfigApply(BlockPos computer, boolean refresh, boolean requiresPower) implements SGPacket {

        public static final Type<ConfigApply> TYPE = new Type<>(SGCraft.id("computer_config_apply"));

        public ConfigApply(final FriendlyByteBuf buf) {
            this(buf.readBlockPos(), buf.readBoolean(), buf.readBoolean());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeBlockPos(this.computer);
            buf.writeBoolean(this.refresh);
            buf.writeBoolean(this.requiresPower);
        }

        @Override
        public Type<ConfigApply> type() {
            return ConfigApply.TYPE;
        }
    }
}
