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

package com.almuradev.sgcraft.mod.obelisk;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.network.SGPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** What Asgard obelisks send between the server and clients. */
public final class ObeliskPayloads {

    private ObeliskPayloads() {
    }

    /**
     * A beam starting for the player who gets it, or called off: the beam's sound, and their screen going white.
     *
     * @param start whether it starts, rather than being called off
     * @param ticks how long until the beam comes, in ticks
     * @param sound whether the player hears the obelisk's beam
     */
    public record Beam(boolean start, int ticks, boolean sound) implements SGPacket {

        public static final Type<Beam> TYPE = new Type<>(SGCraft.id("obelisk_beam"));

        public Beam(final FriendlyByteBuf buf) {
            this(buf.readBoolean(), buf.readVarInt(), buf.readBoolean());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeBoolean(this.start);
            buf.writeVarInt(this.ticks);
            buf.writeBoolean(this.sound);
        }

        @Override
        public Type<Beam> type() {
            return Beam.TYPE;
        }
    }

    /**
     * One point of a screen fade: how covered the player's screen is, and how light its cover, a time after the fade
     * starts. Between points the screen goes smoothly from one to the next.
     *
     * @param at when, in milliseconds after the fade starts
     * @param cover how covered the screen is, from 0 (clear) to 1 (fully covered)
     * @param light how light the cover is, from 0 (black) to 1 (white)
     */
    public record FadeStep(int at, float cover, float light) {

        FadeStep(final FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readFloat(), buf.readFloat());
        }

        void write(final FriendlyByteBuf buf) {
            buf.writeVarInt(this.at);
            buf.writeFloat(this.cover);
            buf.writeFloat(this.light);
        }
    }

    /**
     * A ring transport's screen fade for a player inside the rings, starting as it arrives: white as they leave, black
     * in transit, and white again as they arrive, clearing to show where they are.
     *
     * @param steps its points, in time order; the fade ends at the last
     */
    public record Fade(List<FadeStep> steps) implements SGPacket {

        public static final Type<Fade> TYPE = new Type<>(SGCraft.id("screen_fade"));

        public Fade(final FriendlyByteBuf buf) {
            this(SGPacket.readList(buf, FadeStep::new));
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            SGPacket.writeList(buf, this.steps, (out, step) -> step.write(out));
        }

        @Override
        public Type<Fade> type() {
            return Fade.TYPE;
        }
    }

    /**
     * A player on an obelisk's list, as its screen shows them.
     *
     * @param player who they are
     * @param name their name
     * @param use whether they may use it
     * @param admin whether they may configure it
     */
    public record Entry(UUID player, String name, boolean use, boolean admin) {

        Entry(final FriendlyByteBuf buf) {
            this(buf.readUUID(), buf.readUtf(), buf.readBoolean(), buf.readBoolean());
        }

        void write(final FriendlyByteBuf buf) {
            buf.writeUUID(this.player);
            buf.writeUtf(this.name);
            buf.writeBoolean(this.use);
            buf.writeBoolean(this.admin);
        }
    }

    /**
     * A ring platform's power, as its screen shows it.
     *
     * @param energy the gate energy it holds
     * @param capacity the most it holds
     * @param naquadah how many pieces of naquadah it holds to burn
     * @param uses how many times it could send to its partner on all that
     */
    public record Power(long energy, long capacity, int naquadah, long uses) {

        Power(final FriendlyByteBuf buf) {
            this(buf.readVarLong(), buf.readVarLong(), buf.readVarInt(), buf.readVarLong());
        }

        void write(final FriendlyByteBuf buf) {
            buf.writeVarLong(this.energy);
            buf.writeVarLong(this.capacity);
            buf.writeVarInt(this.naquadah);
            buf.writeVarLong(this.uses);
        }
    }

    /**
     * An obelisk's or a ring platform's settings, for its screen.
     *
     * @param open whether to open the screen, rather than only update it
     * @param obelisk where its bottom half is
     * @param link where the obelisk it is linked to is, as its world and its place, or empty
     * @param owner its owner's name, or empty
     * @param everyone whether anyone may use it, save those listed as not allowed
     * @param entries the players on its list
     * @param message something to tell the player, or empty
     * @param power a ring platform's power, or empty for an obelisk
     */
    public record State(boolean open, BlockPos obelisk, String link, String owner, boolean everyone,
            List<Entry> entries, String message, Optional<Power> power) implements SGPacket {

        public static final Type<State> TYPE = new Type<>(SGCraft.id("obelisk_state"));

        public State(final FriendlyByteBuf buf) {
            this(buf.readBoolean(), buf.readBlockPos(), buf.readUtf(), buf.readUtf(), buf.readBoolean(),
                    SGPacket.readList(buf, Entry::new), buf.readUtf(),
                    buf.readBoolean() ? Optional.of(new Power(buf)) : Optional.empty());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeBoolean(this.open);
            buf.writeBlockPos(this.obelisk);
            buf.writeUtf(this.link);
            buf.writeUtf(this.owner);
            buf.writeBoolean(this.everyone);
            SGPacket.writeList(buf, this.entries, (out, entry) -> entry.write(out));
            buf.writeUtf(this.message);
            buf.writeBoolean(this.power.isPresent());
            this.power.ifPresent(power -> power.write(buf));
        }

        @Override
        public Type<State> type() {
            return State.TYPE;
        }
    }

    /**
     * Changes made on an obelisk's screen.
     *
     * @param obelisk where its bottom half is
     * @param everyone whether anyone may use it, save those listed as not allowed
     * @param entries the players on its list, as the screen left them
     * @param addPlayer a player to add to the list by name, or empty
     * @param unlink whether to unlink it
     */
    public record Apply(BlockPos obelisk, boolean everyone, List<Entry> entries, String addPlayer, boolean unlink)
            implements SGPacket {

        public static final Type<Apply> TYPE = new Type<>(SGCraft.id("obelisk_apply"));

        public Apply(final FriendlyByteBuf buf) {
            this(buf.readBlockPos(), buf.readBoolean(), SGPacket.readList(buf, Entry::new), buf.readUtf(48),
                    buf.readBoolean());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeBlockPos(this.obelisk);
            buf.writeBoolean(this.everyone);
            SGPacket.writeList(buf, this.entries, (out, entry) -> entry.write(out));
            buf.writeUtf(this.addPlayer, 48);
            buf.writeBoolean(this.unlink);
        }

        @Override
        public Type<Apply> type() {
            return Apply.TYPE;
        }
    }
}
