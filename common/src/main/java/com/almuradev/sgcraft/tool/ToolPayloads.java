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

package com.almuradev.sgcraft.tool;

import com.almuradev.sgcraft.SGCraft;
import com.almuradev.sgcraft.network.SGPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;
import java.util.Optional;

/**
 * What the handheld tools' screens and the server tell each other. The server sends a tool's state when the player
 * uses it and after each action; the screen sends actions, naming the gate it is about, which the server checks
 * again.
 */
public final class ToolPayloads {

    private ToolPayloads() {
    }

    /** A gate as a tool sees it. */
    public record GateInfo(BlockPos pos, String address, String type, String state, int iris) {

        /** {@link #iris()} for a gate with no iris; otherwise it is the ordinal of its {@code IrisState}. */
        public static final int NO_IRIS = -1;

        public GateInfo(final FriendlyByteBuf buf) {
            this(buf.readBlockPos(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readVarInt());
        }

        public void write(final FriendlyByteBuf buf) {
            buf.writeBlockPos(this.pos);
            buf.writeUtf(this.address);
            buf.writeUtf(this.type);
            buf.writeUtf(this.state);
            buf.writeVarInt(this.iris);
        }
    }

    /**
     * One entry of an address book, as text, with what the gate its address reaches is doing: one of
     * {@link #NO_GATE} or a gate state's name in lower case.
     */
    public record BookEntry(String name, String address, String status) {

        /** {@link #status()} for an address no gate answers to. */
        public static final String NO_GATE = "no_gate";

        public BookEntry(final FriendlyByteBuf buf) {
            this(buf.readUtf(), buf.readUtf(), buf.readUtf());
        }

        public void write(final FriendlyByteBuf buf) {
            buf.writeUtf(this.name);
            buf.writeUtf(this.address);
            buf.writeUtf(this.status);
        }
    }

    /**
     * A gate on a gate control tablet's list, as its screen shows it.
     *
     * @param world the world it is in, by identifier
     * @param address its address from the holder's world, or empty if it is hidden or gone
     * @param status what it is doing: {@link BookEntry#NO_GATE} if it is gone, or a gate state's name in lower case
     * @param iris its iris, as {@link GateInfo#iris()} gives it
     */
    public record TabletGateInfo(String name, String world, BlockPos pos, String address, String type,
            String status, int iris) {

        public TabletGateInfo(final FriendlyByteBuf buf) {
            this(buf.readUtf(), buf.readUtf(), buf.readBlockPos(), buf.readUtf(), buf.readUtf(), buf.readUtf(),
                    buf.readVarInt());
        }

        public void write(final FriendlyByteBuf buf) {
            buf.writeUtf(this.name);
            buf.writeUtf(this.world);
            buf.writeBlockPos(this.pos);
            buf.writeUtf(this.address);
            buf.writeUtf(this.type);
            buf.writeUtf(this.status);
            buf.writeVarInt(this.iris);
        }
    }

    /**
     * The gate control tablet's state: its gates, the one chosen, and its addresses, as the chosen gate sees them.
     *
     * @param open whether to open the screen, rather than only update it if it is still open
     * @param selected the chosen gate's place in the list, or -1 for none
     * @param near the gate near the holder, which can be added to the list
     * @param holding whether the player still holds a tablet
     * @param message something to tell the player, or empty
     */
    public record TabletState(boolean open, List<TabletGateInfo> gates, int selected, List<BookEntry> entries,
            Optional<GateInfo> near, boolean holding, String message) implements SGPacket {

        public static final Type<TabletState> TYPE = new Type<>(SGCraft.id("tablet_state"));

        public TabletState(final FriendlyByteBuf buf) {
            this(buf.readBoolean(), SGPacket.readList(buf, TabletGateInfo::new), buf.readVarInt(),
                    SGPacket.readList(buf, BookEntry::new), buf.readOptional(GateInfo::new), buf.readBoolean(),
                    buf.readUtf());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeBoolean(this.open);
            SGPacket.writeList(buf, this.gates, (out, gate) -> gate.write(out));
            buf.writeVarInt(this.selected);
            SGPacket.writeList(buf, this.entries, (out, entry) -> entry.write(out));
            buf.writeOptional(this.near, (out, gate) -> gate.write(out));
            buf.writeBoolean(this.holding);
            buf.writeUtf(this.message);
        }

        @Override
        public Type<TabletState> type() {
            return TabletState.TYPE;
        }
    }

    /**
     * Something done on the gate control tablet's screen.
     *
     * @param gate the chosen gate's place in the list, or -1 for none
     * @param index where to move a gate or an entry to
     */
    public record TabletAction(Kind kind, int gate, String address, String name, int index) implements SGPacket {

        public enum Kind {
            REFRESH, ADD_GATE, RENAME_GATE, REMOVE_GATE, MOVE_GATE, DIAL, DISCONNECT, ADD, RENAME, REMOVE, MOVE,
            CONFIGURE, OPEN_IRIS, CLOSE_IRIS
        }

        public static final Type<TabletAction> TYPE = new Type<>(SGCraft.id("tablet_action"));

        public TabletAction(final FriendlyByteBuf buf) {
            this(buf.readEnum(Kind.class), buf.readVarInt(), buf.readUtf(), buf.readUtf(), buf.readVarInt());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeEnum(this.kind);
            buf.writeVarInt(this.gate);
            buf.writeUtf(this.address);
            buf.writeUtf(this.name);
            buf.writeVarInt(this.index);
        }

        @Override
        public Type<TabletAction> type() {
            return TabletAction.TYPE;
        }
    }

    /**
     * The PDD's state: the gate it dials from, if one is near, and the player's address book.
     *
     * @param open whether to open the screen, rather than only update it if it is still open
     * @param message something to tell the player, or empty
     */
    public record PddState(boolean open, Optional<GateInfo> gate, List<BookEntry> entries, boolean editable,
            String message) implements SGPacket {

        public static final Type<PddState> TYPE = new Type<>(SGCraft.id("pdd_state"));

        public PddState(final FriendlyByteBuf buf) {
            this(buf.readBoolean(), buf.readOptional(GateInfo::new), SGPacket.readList(buf, BookEntry::new),
                    buf.readBoolean(), buf.readUtf());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeBoolean(this.open);
            buf.writeOptional(this.gate, (out, gate) -> gate.write(out));
            SGPacket.writeList(buf, this.entries, (out, entry) -> entry.write(out));
            buf.writeBoolean(this.editable);
            buf.writeUtf(this.message);
        }

        @Override
        public Type<PddState> type() {
            return PddState.TYPE;
        }
    }

    /** Something done on the PDD's screen. */
    public record PddAction(Kind kind, String address, String name, int index) implements SGPacket {

        public enum Kind {
            REFRESH, DIAL, DISCONNECT, ADD, RENAME, REMOVE, MOVE
        }

        public static final Type<PddAction> TYPE = new Type<>(SGCraft.id("pdd_action"));

        public PddAction(final FriendlyByteBuf buf) {
            this(buf.readEnum(Kind.class), buf.readUtf(), buf.readUtf(), buf.readVarInt());
        }

        public static PddAction of(final Kind kind) {
            return new PddAction(kind, "", "", 0);
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeEnum(this.kind);
            buf.writeUtf(this.address);
            buf.writeUtf(this.name);
            buf.writeVarInt(this.index);
        }

        @Override
        public Type<PddAction> type() {
            return PddAction.TYPE;
        }
    }

    /**
     * The GDO's state: the gate it works, and the gate at the other end of its wormhole, if it is open.
     *
     * @param open whether to open the screen, rather than only update it if it is still open
     * @param message something to tell the player, or empty
     */
    public record GdoState(boolean open, GateInfo local, Optional<GateInfo> remote, boolean canDisconnect,
            String message) implements SGPacket {

        public static final Type<GdoState> TYPE = new Type<>(SGCraft.id("gdo_state"));

        public GdoState(final FriendlyByteBuf buf) {
            this(buf.readBoolean(), new GateInfo(buf), buf.readOptional(GateInfo::new), buf.readBoolean(),
                    buf.readUtf());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeBoolean(this.open);
            this.local.write(buf);
            buf.writeOptional(this.remote, (out, gate) -> gate.write(out));
            buf.writeBoolean(this.canDisconnect);
            buf.writeUtf(this.message);
        }

        @Override
        public Type<GdoState> type() {
            return GdoState.TYPE;
        }
    }

    /** Something done on the GDO's screen, to the gate at {@code gate}. */
    public record GdoAction(BlockPos gate, Kind kind) implements SGPacket {

        public enum Kind {
            REFRESH, OPEN_LOCAL, CLOSE_LOCAL, DISCONNECT, OPEN_REMOTE, CLOSE_REMOTE
        }

        public static final Type<GdoAction> TYPE = new Type<>(SGCraft.id("gdo_action"));

        public GdoAction(final FriendlyByteBuf buf) {
            this(buf.readBlockPos(), buf.readEnum(Kind.class));
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeBlockPos(this.gate);
            buf.writeEnum(this.kind);
        }

        @Override
        public Type<GdoAction> type() {
            return GdoAction.TYPE;
        }
    }

    /**
     * One property of a gate, for the configurator: its identifier, its value as JSON, whether the gate has a value
     * of its own rather than the default, and as JSON what applies when the gate has none, if anything does.
     */
    public record Property(String id, String json, boolean own, String fallback) {

        public Property(final FriendlyByteBuf buf) {
            this(buf.readUtf(), buf.readUtf(), buf.readBoolean(), buf.readUtf());
        }

        public void write(final FriendlyByteBuf buf) {
            buf.writeUtf(this.id);
            buf.writeUtf(this.json);
            buf.writeBoolean(this.own);
            buf.writeUtf(this.fallback);
        }
    }

    /** A player the gate lists, by UUID, with the name they had when last seen. */
    public record Named(String uuid, String name) {

        public Named(final FriendlyByteBuf buf) {
            this(buf.readUtf(), buf.readUtf());
        }

        public void write(final FriendlyByteBuf buf) {
            buf.writeUtf(this.uuid);
            buf.writeUtf(this.name);
        }
    }

    /**
     * The configurator's state: a gate and everything that can be set on it.
     *
     * @param open whether to open the screen, rather than only update it if it is still open
     * @param types every gate type, by identifier
     * @param typeNames each type's name as players see it, as JSON, in the same order as {@code types}
     * @param message something to tell the player, or empty
     * @param fePerEnergy how many FE a power unit gives for one unit of gate energy
     * @param orientation whether the gate stands or lies flat, by its serialized name
     */
    public record ConfiguratorState(boolean open, GateInfo gate, int chevrons, List<String> types,
            String world, List<String> typeNames, List<Property> properties, List<Named> players, String message,
            double fePerEnergy, String orientation)
            implements SGPacket {

        public static final Type<ConfiguratorState> TYPE = new Type<>(SGCraft.id("configurator_state"));

        public ConfiguratorState(final FriendlyByteBuf buf) {
            this(buf.readBoolean(), new GateInfo(buf), buf.readVarInt(),
                    SGPacket.readList(buf, FriendlyByteBuf::readUtf), buf.readUtf(),
                    SGPacket.readList(buf, FriendlyByteBuf::readUtf),
                    SGPacket.readList(buf, Property::new),
                    SGPacket.readList(buf, Named::new), buf.readUtf(), buf.readDouble(), buf.readUtf());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeBoolean(this.open);
            this.gate.write(buf);
            buf.writeVarInt(this.chevrons);
            SGPacket.writeList(buf, this.types, FriendlyByteBuf::writeUtf);
            buf.writeUtf(this.world);
            SGPacket.writeList(buf, this.typeNames, FriendlyByteBuf::writeUtf);
            SGPacket.writeList(buf, this.properties, (out, property) -> property.write(out));
            SGPacket.writeList(buf, this.players, (out, player) -> player.write(out));
            buf.writeUtf(this.message);
            buf.writeDouble(this.fePerEnergy);
            buf.writeUtf(this.orientation);
        }

        @Override
        public Type<ConfiguratorState> type() {
            return ConfiguratorState.TYPE;
        }
    }

    /** A property to set, as JSON, or to clear back to its default when {@code json} is empty. */
    public record Change(String id, Optional<String> json) {

        public Change(final FriendlyByteBuf buf) {
            this(buf.readUtf(), buf.readOptional(FriendlyByteBuf::readUtf));
        }

        public void write(final FriendlyByteBuf buf) {
            buf.writeUtf(this.id);
            buf.writeOptional(this.json, FriendlyByteBuf::writeUtf);
        }
    }

    /**
     * Changes made on the configurator's screen, to the gate at {@code gate}.
     *
     * @param gateType the gate type to change to, or empty to leave it
     * @param chevrons the chevrons to change to, or 0 to leave them
     * @param addPlayer the name of a player to list with the gate's default rights, or empty
     * @param refresh true to change nothing and only send the state again
     * @param orientation the way to turn a gate lying flat, face up or face down by its serialized name, or empty to
     *     leave it
     */
    public record ConfiguratorApply(String world, BlockPos gate, String gateType, int chevrons, List<Change> changes,
            String addPlayer, boolean refresh, String orientation) implements SGPacket {

        public static final Type<ConfiguratorApply> TYPE = new Type<>(SGCraft.id("configurator_apply"));

        public ConfiguratorApply(final FriendlyByteBuf buf) {
            this(buf.readUtf(), buf.readBlockPos(), buf.readUtf(), buf.readVarInt(),
                    SGPacket.readList(buf, Change::new),
                    buf.readUtf(), buf.readBoolean(), buf.readUtf());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeUtf(this.world);
            buf.writeBlockPos(this.gate);
            buf.writeUtf(this.gateType);
            buf.writeVarInt(this.chevrons);
            SGPacket.writeList(buf, this.changes, (out, change) -> change.write(out));
            buf.writeUtf(this.addPlayer);
            buf.writeBoolean(this.refresh);
            buf.writeUtf(this.orientation);
        }

        @Override
        public Type<ConfiguratorApply> type() {
            return ConfiguratorApply.TYPE;
        }
    }
}
