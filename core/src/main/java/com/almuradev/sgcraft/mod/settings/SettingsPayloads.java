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

package com.almuradev.sgcraft.mod.settings;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.address.AddressChanges;
import com.almuradev.sgcraft.mod.network.SGPacket;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

/**
 * The packets of the settings screen. The client asks for the settings with {@link Request}; the server answers
 * with {@link State}, or, to an operator who asked for the screen with {@code /sgcraft settings screen}, opens it
 * with one. The screen holds its changes until the player saves them: each edit sends them all as {@link Edits} to be
 * previewed, and the server answers with a {@link State} showing them. Saving sends them once more to be saved. Edits
 * that give gates new addresses come back first as a {@link Confirm}, for the player to send again with
 * {@code confirmed} set. Messages are text components written as JSON.
 */
public final class SettingsPayloads {

    private SettingsPayloads() {
    }

    /** Asks for the settings as the server has them. */
    public record Request() implements SGPacket {

        public static final Type<Request> TYPE = new Type<>(SGCraft.id("settings_request"));

        public Request(final FriendlyByteBuf buf) {
            this();
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
        }

        @Override
        public Type<Request> type() {
            return Request.TYPE;
        }
    }

    /**
     * One world, as it is now and as it will be after a restart.
     *
     * @param id the world's identifier
     * @param gates how many formed gates it has
     * @param ownOrigin whether it will have a point of origin of its own, rather than the server's
     * @param originNow its point of origin now, or {@code ""} for none
     * @param originNext its point of origin after a restart, or {@code ""} for none
     * @param groupNow the group it is in now
     * @param groupNext the group it will be in after a restart
     * @param symbolNow the symbol that names it in its group now, or {@code ""} for none
     * @param symbolNext the symbol that will name it after a restart, or {@code ""} for none
     * @param ownCell whether it will have a cell size of its own, rather than the server's
     * @param cellNow how many chunks across its cells are now
     * @param cellNext how many chunks across its cells will be after a restart
     */
    public record World(String id, long gates, boolean ownOrigin, String originNow, String originNext,
            String groupNow, String groupNext, String symbolNow, String symbolNext, boolean ownCell, int cellNow,
            int cellNext) {

        World(final FriendlyByteBuf buf) {
            this(buf.readUtf(), buf.readVarLong(), buf.readBoolean(), buf.readUtf(), buf.readUtf(), buf.readUtf(),
                    buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readBoolean(), buf.readVarInt(),
                    buf.readVarInt());
        }

        void write(final FriendlyByteBuf buf) {
            buf.writeUtf(this.id);
            buf.writeVarLong(this.gates);
            buf.writeBoolean(this.ownOrigin);
            buf.writeUtf(this.originNow);
            buf.writeUtf(this.originNext);
            buf.writeUtf(this.groupNow);
            buf.writeUtf(this.groupNext);
            buf.writeUtf(this.symbolNow);
            buf.writeUtf(this.symbolNext);
            buf.writeBoolean(this.ownCell);
            buf.writeVarInt(this.cellNow);
            buf.writeVarInt(this.cellNext);
        }
    }

    /**
     * One group, as it is now and as it will be after a restart.
     *
     * @param name its name
     * @param symbolNow its symbol now, or {@code ""} if it doesn't exist until a restart
     * @param symbolNext its symbol after a restart, or {@code ""} if it is gone then
     * @param automatic whether SGCraft chooses its symbol, as the settings give it none
     * @param worlds how many worlds it will have after a restart
     */
    public record Group(String name, String symbolNow, String symbolNext, boolean automatic, int worlds) {

        Group(final FriendlyByteBuf buf) {
            this(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readBoolean(), buf.readVarInt());
        }

        void write(final FriendlyByteBuf buf) {
            buf.writeUtf(this.name);
            buf.writeUtf(this.symbolNow);
            buf.writeUtf(this.symbolNext);
            buf.writeBoolean(this.automatic);
            buf.writeVarInt(this.worlds);
        }
    }

    /**
     * One of the other settings, as {@link SettingsCatalog.Setting} describes it.
     *
     * @param key where it is in the settings file
     * @param page the page it is on
     * @param pageTitle the page's title
     * @param section the heading it is under
     * @param label its name
     * @param tip what it does
     * @param type the kind of value it holds
     * @param min the least value it can have, for a number
     * @param aboveMin whether it must be above {@code min} rather than at least {@code min}
     * @param max the most value it can have, for a number
     * @param live whether a change applies at once, rather than when the server next starts
     * @param now its value now
     * @param next its value after a restart, as the settings file has it
     * @param defaultValue its value when the file doesn't set it
     */
    public record Option(String key, String page, String pageTitle, String section, String label, String tip,
            SettingsCatalog.Type type, double min, boolean aboveMin, double max, boolean live, String now,
            String next, String defaultValue) {

        Option(final FriendlyByteBuf buf) {
            this(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(),
                    buf.readEnum(SettingsCatalog.Type.class), buf.readDouble(), buf.readBoolean(), buf.readDouble(),
                    buf.readBoolean(), buf.readUtf(), buf.readUtf(), buf.readUtf());
        }

        void write(final FriendlyByteBuf buf) {
            buf.writeUtf(this.key);
            buf.writeUtf(this.page);
            buf.writeUtf(this.pageTitle);
            buf.writeUtf(this.section);
            buf.writeUtf(this.label);
            buf.writeUtf(this.tip);
            buf.writeEnum(this.type);
            buf.writeDouble(this.min);
            buf.writeBoolean(this.aboveMin);
            buf.writeDouble(this.max);
            buf.writeBoolean(this.live);
            buf.writeUtf(this.now);
            buf.writeUtf(this.next);
            buf.writeUtf(this.defaultValue);
        }
    }

    /**
     * The settings as the server has them.
     *
     * @param open whether to open the screen, rather than only fill one already open
     * @param allowed whether the player may change them; when not, the rest is empty
     * @param originNow the server's point of origin now, or {@code ""} for none
     * @param originNext the server's point of origin after a restart, or {@code ""} for none
     * @param cellNow how many chunks across the server's cells are now
     * @param cellNext how many chunks across the server's cells will be after a restart
     * @param worlds the worlds
     * @param groups the groups, the default one first, then those gone after a restart
     * @param options the other settings, in the order the screen shows them
     * @param messages what the last change does, and any problems with the settings
     * @param reply what this answers: the {@link Edits#sequence()} of the edits it shows, or 0 for the settings as
     *     saved
     * @param refused the index of the address change in those edits that wasn't allowed, or -1
     * @param refusedOption the index of the other setting in those edits that wasn't allowed, or -1
     * @param saved whether those edits were saved, so the screen holds none now
     * @param refresh whether another operator saved changes, so a screen holding edits of its own previews them again
     */
    public record State(boolean open, boolean allowed, String originNow, String originNext, int cellNow,
            int cellNext, List<World> worlds, List<Group> groups, List<Option> options, List<String> messages,
            int reply, int refused, int refusedOption, boolean saved, boolean refresh) implements SGPacket {

        public static final Type<State> TYPE = new Type<>(SGCraft.id("settings_state"));

        public State(final FriendlyByteBuf buf) {
            this(buf.readBoolean(), buf.readBoolean(), buf.readUtf(), buf.readUtf(), buf.readVarInt(),
                    buf.readVarInt(), SGPacket.readList(buf, World::new), SGPacket.readList(buf, Group::new),
                    SGPacket.readList(buf, Option::new), SGPacket.readList(buf, FriendlyByteBuf::readUtf),
                    buf.readVarInt(), buf.readInt(), buf.readInt(), buf.readBoolean(), buf.readBoolean());
        }

        /** @return the settings for a player who may not change them */
        public static State refused(final boolean open) {
            return new State(open, false, "", "", 0, 0, List.of(), List.of(), List.of(), List.of(), 0, -1, -1,
                    false, false);
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeBoolean(this.open);
            buf.writeBoolean(this.allowed);
            buf.writeUtf(this.originNow);
            buf.writeUtf(this.originNext);
            buf.writeVarInt(this.cellNow);
            buf.writeVarInt(this.cellNext);
            SGPacket.writeList(buf, this.worlds, (out, world) -> world.write(out));
            SGPacket.writeList(buf, this.groups, (out, group) -> group.write(out));
            SGPacket.writeList(buf, this.options, (out, option) -> option.write(out));
            SGPacket.writeList(buf, this.messages, FriendlyByteBuf::writeUtf);
            buf.writeVarInt(this.reply);
            buf.writeInt(this.refused);
            buf.writeInt(this.refusedOption);
            buf.writeBoolean(this.saved);
            buf.writeBoolean(this.refresh);
        }

        @Override
        public Type<State> type() {
            return State.TYPE;
        }
    }

    /**
     * A change to the address settings, as {@link AddressChanges.Change} describes it.
     *
     * @param kind what it does
     * @param world the world it changes, or {@code ""} for every world
     * @param group the group it names, or {@code ""}
     * @param value its value, or {@code ""}
     */
    public record Change(AddressChanges.Kind kind, String world, String group, String value) {

        Change(final FriendlyByteBuf buf) {
            this(buf.readEnum(AddressChanges.Kind.class), buf.readUtf(), buf.readUtf(), buf.readUtf());
        }

        void write(final FriendlyByteBuf buf) {
            buf.writeEnum(this.kind);
            buf.writeUtf(this.world);
            buf.writeUtf(this.group);
            buf.writeUtf(this.value);
        }
    }

    /**
     * A change to one of the other settings.
     *
     * @param key the setting's key
     * @param value its new value, written as text
     */
    public record SetOption(String key, String value) {

        SetOption(final FriendlyByteBuf buf) {
            this(buf.readUtf(), buf.readUtf());
        }

        void write(final FriendlyByteBuf buf) {
            buf.writeUtf(this.key);
            buf.writeUtf(this.value);
        }
    }

    /**
     * The changes a screen holds, to preview or to save. The address changes are made first, in order, then the
     * other settings, in order.
     *
     * @param sequence counts up with each edit the screen sends, so it can tell which an answer is for
     * @param changes the changes to the address settings
     * @param options the changes to the other settings
     * @param save whether to save them, rather than only show what they do
     * @param confirmed whether the player has seen what they do to gates' addresses and wants them anyway
     */
    public record Edits(int sequence, List<Change> changes, List<SetOption> options, boolean save,
            boolean confirmed) implements SGPacket {

        public static final Type<Edits> TYPE = new Type<>(SGCraft.id("settings_edits"));

        public Edits(final FriendlyByteBuf buf) {
            this(buf.readVarInt(), SGPacket.readList(buf, Change::new), SGPacket.readList(buf, SetOption::new),
                    buf.readBoolean(), buf.readBoolean());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeVarInt(this.sequence);
            SGPacket.writeList(buf, this.changes, (out, change) -> change.write(out));
            SGPacket.writeList(buf, this.options, (out, option) -> option.write(out));
            buf.writeBoolean(this.save);
            buf.writeBoolean(this.confirmed);
        }

        /** @return these edits, set to be saved whatever they do to gates' addresses */
        public Edits confirm() {
            return new Edits(this.sequence, this.changes, this.options, true, true);
        }

        @Override
        public Type<Edits> type() {
            return Edits.TYPE;
        }
    }

    /**
     * Edits that give gates new addresses, waiting for the player to confirm them before they are saved.
     *
     * @param edits the edits, to send again confirmed
     * @param warnings what they break, for each world whose gates get new addresses
     */
    public record Confirm(Edits edits, List<String> warnings) implements SGPacket {

        public static final Type<Confirm> TYPE = new Type<>(SGCraft.id("settings_confirm"));

        public Confirm(final FriendlyByteBuf buf) {
            this(new Edits(buf), SGPacket.readList(buf, FriendlyByteBuf::readUtf));
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            this.edits.write(buf);
            SGPacket.writeList(buf, this.warnings, FriendlyByteBuf::writeUtf);
        }

        @Override
        public Type<Confirm> type() {
            return Confirm.TYPE;
        }
    }
}
