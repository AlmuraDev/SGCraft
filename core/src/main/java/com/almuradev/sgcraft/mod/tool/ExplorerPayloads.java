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

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.network.SGPacket;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.GateInfo;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;
import java.util.Optional;

/**
 * What the explorer dialer sends between the server and its screen.
 */
public final class ExplorerPayloads {

    private ExplorerPayloads() {
    }

    /**
     * One generated gate, as the explorer dialer lists it.
     *
     * @param gate where it is, as {@link ExplorerDialer#key} writes it, which names it in actions
     * @param name the name the player gave it, or empty
     * @param address its address from the player's world, or empty if it is hidden or out of reach
     * @param world the world it is in, by identifier
     * @param visited whether the player has visited it
     * @param status what it is doing, as a gate state's name in lower case
     */
    public record Listed(String gate, String name, String address, String world, boolean visited, String status) {

        public Listed(final FriendlyByteBuf buf) {
            this(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readBoolean(), buf.readUtf());
        }

        public void write(final FriendlyByteBuf buf) {
            buf.writeUtf(this.gate);
            buf.writeUtf(this.name);
            buf.writeUtf(this.address);
            buf.writeUtf(this.world);
            buf.writeBoolean(this.visited);
            buf.writeUtf(this.status);
        }
    }

    /**
     * The explorer dialer's state: the gate near its holder, which it dials from, and every generated gate.
     *
     * @param open whether to open the screen, rather than only update it if it is still open
     * @param message something to tell the player, or empty
     */
    public record State(boolean open, Optional<GateInfo> gate, List<Listed> entries, String message)
            implements SGPacket {

        public static final Type<State> TYPE = new Type<>(SGCraft.id("explorer_state"));

        public State(final FriendlyByteBuf buf) {
            this(buf.readBoolean(), buf.readOptional(GateInfo::new), SGPacket.readList(buf, Listed::new),
                    buf.readUtf());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeBoolean(this.open);
            buf.writeOptional(this.gate, (out, gate) -> gate.write(out));
            SGPacket.writeList(buf, this.entries, (out, entry) -> entry.write(out));
            buf.writeUtf(this.message);
        }

        @Override
        public Type<State> type() {
            return State.TYPE;
        }
    }

    /**
     * Something done on the explorer dialer's screen, to the listed gate {@code gate} names.
     *
     * @param name the gate's new name, for {@link Kind#RENAME}
     */
    public record Action(Kind kind, String gate, String name) implements SGPacket {

        public enum Kind {
            REFRESH, DIAL, DISCONNECT, RENAME
        }

        public static final Type<Action> TYPE = new Type<>(SGCraft.id("explorer_action"));

        public Action(final FriendlyByteBuf buf) {
            this(buf.readEnum(Kind.class), buf.readUtf(), buf.readUtf());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeEnum(this.kind);
            buf.writeUtf(this.gate);
            buf.writeUtf(this.name);
        }

        @Override
        public Type<Action> type() {
            return Action.TYPE;
        }
    }
}
