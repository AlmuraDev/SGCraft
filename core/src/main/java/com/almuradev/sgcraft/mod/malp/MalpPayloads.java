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

package com.almuradev.sgcraft.mod.malp;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.network.SGPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * What the screen of a MALP console or MALP tablet and the server tell each other, and a MALP's configurator screen
 * and the server. The server sends the state of the console or tablet when it is used and about twice a second while
 * its screen is open; the screen sends what its buttons do, and the controls while they are held, which the server
 * takes only from the player driving the MALP, not from those who only watch. The server sends a MALP's settings when a
 * configurator is used on it and after each change; the configurator's screen sends the settings to change to, or
 * asks for the MALP to be unpaired from its console.
 */
public final class MalpPayloads {

    private MalpPayloads() {
    }

    /** What the feed shows. */
    public enum Feed {
        /** A picture, with the MALP's readouts. */
        LIVE,
        /** No MALP is chosen, or the console or tablet doesn't reach the chosen one. */
        NO_SIGNAL,
        /** The console or tablet reaches the MALP, but it needs power and its battery is flat or missing. */
        NO_POWER,
        /** The feed is powered down from the console or tablet. */
        OFF,
        /** The tablet's own battery is flat or missing, so it shows no feed at all. */
        NO_TABLET_POWER
    }

    /** How a player with a console's or tablet's screen open takes part in a MALP's feed. */
    public enum Role {
        /** They drive the chosen MALP, as the first to choose it. */
        DRIVER,
        /**
         * They watch the feed of the player who came to the same console first, and may do nothing but close the
         * screen.
         */
        CONSOLE_WATCHER,
        /**
         * They watch the feed of the player driving the MALP they chose, and may choose another MALP or close the
         * screen.
         */
        MALP_WATCHER
    }

    /** How other players see a player with a console's or tablet's screen open. */
    public enum Use {
        /** As usual. */
        NONE,
        /** Working a console's controls. */
        CONSOLE,
        /** Holding a tablet out in front of them with both hands, looking down at it. */
        TABLET
    }

    /**
     * A MALP paired with the console or tablet, as its screen lists it.
     *
     * @param world its world, by identifier
     * @param battery its battery's charge, in percent, or -1 for no battery
     */
    public record MalpInfo(UUID id, String name, String world, BlockPos pos, int battery) {

        public MalpInfo(final FriendlyByteBuf buf) {
            this(buf.readUUID(), buf.readUtf(), buf.readUtf(), buf.readBlockPos(), buf.readVarInt());
        }

        public void write(final FriendlyByteBuf buf) {
            buf.writeUUID(this.id);
            buf.writeUtf(this.name);
            buf.writeUtf(this.world);
            buf.writeBlockPos(this.pos);
            buf.writeVarInt(this.battery);
        }
    }

    /**
     * What the chosen MALP sees and reports, along the bottom of the feed.
     *
     * @param signal how the console or tablet reaches it, a {@link MalpSignal.Kind}'s name
     * @param relay the addresses of the gates the signal goes through, or empty when it goes directly
     * @param world its world, by identifier
     * @param biome the biome it is in, by identifier
     * @param temperature the biome's temperature, as Minecraft measures it
     * @param time the time of day in its world, in ticks from dawn
     * @param light the light level where it stands
     * @param hostiles how many hostile creatures are within 32 blocks of it
     * @param battery its battery's charge, in percent
     * @param lamp whether its lamp is on
     * @param powerRequired whether it runs on its battery; when false, {@code battery} means nothing
     */
    public record Readout(String signal, String relay, String world, String biome, float temperature, int time,
            int light, int hostiles, int battery, boolean lamp, boolean powerRequired) {

        public static final Readout NONE = new Readout(MalpSignal.Kind.NONE.name(), "", "", "", 0, 0, 0, 0, 0, false,
                true);

        public Readout(final FriendlyByteBuf buf) {
            this(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readFloat(), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean());
        }

        public void write(final FriendlyByteBuf buf) {
            buf.writeUtf(this.signal);
            buf.writeUtf(this.relay);
            buf.writeUtf(this.world);
            buf.writeUtf(this.biome);
            buf.writeFloat(this.temperature);
            buf.writeVarInt(this.time);
            buf.writeVarInt(this.light);
            buf.writeVarInt(this.hostiles);
            buf.writeVarInt(this.battery);
            buf.writeBoolean(this.lamp);
            buf.writeBoolean(this.powerRequired);
        }
    }

    /**
     * The state of a console or tablet.
     *
     * @param open whether to open the screen, rather than only update it if it is still open
     * @param close whether to close the screen, such as when the player has walked away
     * @param console where the console is, or empty for a tablet
     * @param malps the MALPs paired with it, by name
     * @param selected the MALP being watched, if any
     * @param role whether the player drives the MALP or only watches it, and whom they watch
     * @param tabletBattery the charge of a tablet's battery, in percent, or -1 for no battery or for a console
     */
    public record ConsoleState(boolean open, boolean close, Optional<BlockPos> console, List<MalpInfo> malps,
            Optional<UUID> selected, Feed feed, Readout readout, Role role, int tabletBattery) implements SGPacket {

        public static final Type<ConsoleState> TYPE = new Type<>(SGCraft.id("malp_console_state"));

        public ConsoleState(final FriendlyByteBuf buf) {
            this(buf.readBoolean(), buf.readBoolean(), buf.readOptional(BlockPos.STREAM_CODEC),
                    SGPacket.readList(buf, MalpInfo::new), buf.readOptional(UUIDUtil.STREAM_CODEC),
                    buf.readEnum(Feed.class), new Readout(buf), buf.readEnum(Role.class), buf.readVarInt());
        }

        /** @return whether this is a tablet's state rather than a console's */
        public boolean tablet() {
            return this.console.isEmpty();
        }

        /** @return whether the player only watches, and may not drive or work the MALP's lamp, camera or feed */
        public boolean viewOnly() {
            return this.role != Role.DRIVER;
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeBoolean(this.open);
            buf.writeBoolean(this.close);
            buf.writeOptional(this.console, BlockPos.STREAM_CODEC);
            SGPacket.writeList(buf, this.malps, (out, malp) -> malp.write(out));
            buf.writeOptional(this.selected, UUIDUtil.STREAM_CODEC);
            buf.writeEnum(this.feed);
            this.readout.write(buf);
            buf.writeEnum(this.role);
            buf.writeVarInt(this.tabletBattery);
        }

        @Override
        public Type<ConsoleState> type() {
            return ConsoleState.TYPE;
        }
    }

    /**
     * A button on a console's or tablet's screen.
     *
     * @param malp the MALP to watch, for {@link Kind#SELECT}, or to take off the tablet, for {@link Kind#REMOVE}
     */
    public record ConsoleAction(Kind kind, Optional<UUID> malp) implements SGPacket {

        /**
         * What the button does: chooses the MALP to watch, turns its lamp on or off, powers the feed up or down,
         * turns its camera back to face the way it drives, closes the screen, or takes a MALP off the tablet's list.
         */
        public enum Kind {
            SELECT, LAMP, FEED, CENTER, CLOSE, REMOVE
        }

        public static final Type<ConsoleAction> TYPE = new Type<>(SGCraft.id("malp_console_action"));

        public ConsoleAction(final FriendlyByteBuf buf) {
            this(buf.readEnum(Kind.class), buf.readOptional(UUIDUtil.STREAM_CODEC));
        }

        public static ConsoleAction of(final Kind kind) {
            return new ConsoleAction(kind, Optional.empty());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeEnum(this.kind);
            buf.writeOptional(this.malp, UUIDUtil.STREAM_CODEC);
        }

        @Override
        public Type<ConsoleAction> type() {
            return ConsoleAction.TYPE;
        }
    }

    /**
     * The controls of the MALP being watched, sent while they are held.
     *
     * @param forward from -1 for full reverse to 1 for full ahead
     * @param turn from -1 for full left to 1 for full right
     * @param pan how far to pan the camera, in degrees, positive to the right
     * @param tilt how far to tilt the camera, in degrees, positive down
     */
    public record Drive(float forward, float turn, float pan, float tilt) implements SGPacket {

        public static final Type<Drive> TYPE = new Type<>(SGCraft.id("malp_drive"));

        public Drive(final FriendlyByteBuf buf) {
            this(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeFloat(this.forward);
            buf.writeFloat(this.turn);
            buf.writeFloat(this.pan);
            buf.writeFloat(this.tilt);
        }

        @Override
        public Type<Drive> type() {
            return Drive.TYPE;
        }
    }

    /**
     * How other players see a player with a MALP console's or tablet's screen open, so clients draw them working the
     * console's controls or holding the tablet. The server sends it to every player when someone starts or stops, and
     * to a player who starts tracking someone already doing so.
     *
     * @param player the player's entity id
     * @param use how to draw them
     */
    public record ConsoleUse(int player, Use use) implements SGPacket {

        public static final Type<ConsoleUse> TYPE = new Type<>(SGCraft.id("malp_console_use"));

        public ConsoleUse(final FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readEnum(Use.class));
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeVarInt(this.player);
            buf.writeEnum(this.use);
        }

        @Override
        public Type<ConsoleUse> type() {
            return ConsoleUse.TYPE;
        }
    }

    /**
     * A MALP's settings, for its configurator's screen.
     *
     * @param open whether to open the screen, rather than only update it if it is still open
     * @param malp the MALP's UUID
     * @param name the MALP's name
     * @param owner the name of the player it belongs to, when last seen, or empty if it belongs to no one
     * @param console the console it is paired with, or empty if it is paired with none
     * @param options its settings as they now are
     * @param saved whether this answers changes that were made
     */
    public record ConfigState(boolean open, UUID malp, String name, Optional<String> owner,
            Optional<GlobalPos> console, MalpOptions options, boolean saved) implements SGPacket {

        public static final Type<ConfigState> TYPE = new Type<>(SGCraft.id("malp_config_state"));

        public ConfigState(final FriendlyByteBuf buf) {
            this(buf.readBoolean(), buf.readUUID(), buf.readUtf(), buf.readOptional(FriendlyByteBuf::readUtf),
                    buf.readOptional(GlobalPos.STREAM_CODEC), new MalpOptions(buf), buf.readBoolean());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeBoolean(this.open);
            buf.writeUUID(this.malp);
            buf.writeUtf(this.name);
            buf.writeOptional(this.owner, FriendlyByteBuf::writeUtf);
            buf.writeOptional(this.console, GlobalPos.STREAM_CODEC);
            this.options.write(buf);
            buf.writeBoolean(this.saved);
        }

        @Override
        public Type<ConfigState> type() {
            return ConfigState.TYPE;
        }
    }

    /**
     * The settings a MALP's configurator screen asks for. The server checks again that the player may change them.
     *
     * @param malp the MALP's UUID
     * @param refresh true to change nothing and only send the settings again
     * @param options every setting, as it should be
     */
    public record ConfigApply(UUID malp, boolean refresh, MalpOptions options) implements SGPacket {

        public static final Type<ConfigApply> TYPE = new Type<>(SGCraft.id("malp_config_apply"));

        public ConfigApply(final FriendlyByteBuf buf) {
            this(buf.readUUID(), buf.readBoolean(), new MalpOptions(buf));
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeUUID(this.malp);
            buf.writeBoolean(this.refresh);
            this.options.write(buf);
        }

        @Override
        public Type<ConfigApply> type() {
            return ConfigApply.TYPE;
        }
    }

    /**
     * A MALP's configurator screen asks for the MALP to be unpaired from its console. The server checks again that the
     * player may change the MALP's settings.
     *
     * @param malp the MALP's UUID
     */
    public record ConfigUnpair(UUID malp) implements SGPacket {

        public static final Type<ConfigUnpair> TYPE = new Type<>(SGCraft.id("malp_config_unpair"));

        public ConfigUnpair(final FriendlyByteBuf buf) {
            this(buf.readUUID());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeUUID(this.malp);
        }

        @Override
        public Type<ConfigUnpair> type() {
            return ConfigUnpair.TYPE;
        }
    }
}
