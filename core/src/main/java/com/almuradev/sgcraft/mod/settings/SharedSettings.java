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

import com.almuradev.sgcraft.mod.SGConfig;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.malp.MalpSettings;
import com.almuradev.sgcraft.mod.network.SGNetwork;
import com.almuradev.sgcraft.mod.network.SGPacket;
import com.almuradev.sgcraft.mod.zat.ZatSettings;
import com.almuradev.sgcraft.mod.zpm.ZpmSettings;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * The server's settings a client reads for itself: the ZPM's, the battery's (in the MALP settings) and the Zat's, for
 * item bars and tooltips, the creative tab's full battery and the Zat's cooldown. A dedicated server's players never
 * read its settings file, so the server sends these as each player joins and again whenever they change, and the
 * settings holders fall back to what was sent where no server runs. A client that leaves a server goes back to the
 * defaults.
 */
public final class SharedSettings {

    /** What a client is sent. */
    public record Values(ZpmSettings zpm, MalpSettings malp, ZatSettings zat) {

        public static final Values DEFAULTS = new Values(ZpmSettings.DEFAULTS, MalpSettings.DEFAULTS,
                ZatSettings.DEFAULTS);

        static final Codec<Values> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ZpmSettings.CODEC.optionalFieldOf("zpm", ZpmSettings.DEFAULTS).forGetter(Values::zpm),
                MalpSettings.CODEC.optionalFieldOf("malp", MalpSettings.DEFAULTS).forGetter(Values::malp),
                ZatSettings.CODEC.optionalFieldOf("zat", ZatSettings.DEFAULTS).forGetter(Values::zat)
        ).apply(instance, Values::new));

        /** @return the values a server with these settings sends */
        public static Values of(final SGConfig config) {
            return new Values(config.options().zpm(), config.malp(), config.options().zat());
        }
    }

    /** The packet that carries them, as JSON, so it reads whatever the settings' codecs read. */
    public record Packet(Values values) implements SGPacket {

        public static final Type<Packet> TYPE = new Type<>(SGCraft.id("shared_settings"));

        public Packet(final FriendlyByteBuf buf) {
            this(SharedSettings.read(buf.readUtf()));
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeUtf(Values.CODEC.encodeStart(JsonOps.INSTANCE, this.values).getOrThrow().toString());
        }

        @Override
        public Type<Packet> type() {
            return Packet.TYPE;
        }
    }

    // What the server last sent this client. In single player the holders read the server itself, not this
    private static volatile Values received = Values.DEFAULTS;
    // What the running server last sent its players, to send again when it changes; server thread only
    private static @Nullable Values sent;

    private SharedSettings() {
    }

    private static Values read(final String json) {
        try {
            return Values.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                    .resultOrPartial(error -> SGCraft.LOG.warn("SGCraft couldn't read the server's settings: {}",
                            error))
                    .orElse(Values.DEFAULTS);
        } catch (final JsonParseException e) {
            SGCraft.LOG.warn("SGCraft couldn't read the server's settings", e);
            return Values.DEFAULTS;
        }
    }

    /** @return the ZPM settings the server sent, or the defaults if none has */
    public static ZpmSettings zpm() {
        return SharedSettings.received.zpm();
    }

    /** @return the MALP settings the server sent, or the defaults if none has */
    public static MalpSettings malp() {
        return SharedSettings.received.malp();
    }

    /** @return the Zat settings the server sent, or the defaults if none has */
    public static ZatSettings zat() {
        return SharedSettings.received.zat();
    }

    /** Takes up what the server sent. The client calls this as each packet arrives. */
    public static void receive(final Values values) {
        SharedSettings.received = values;
    }

    /** Goes back to the defaults. The client calls this as it leaves a server. */
    public static void forget() {
        SharedSettings.received = Values.DEFAULTS;
    }

    /** Sends a player who has just joined the server's values. */
    public static void joined(final SGServer server, final ServerPlayer player) {
        final Values values = Values.of(server.config());
        SharedSettings.sent = values;
        SGNetwork.send(player, new Packet(values));
    }

    /** Sends every player the server's values when they have changed since they were last sent. */
    public static void tick(final SGServer server) {
        final Values values = Values.of(server.config());
        if (values.equals(SharedSettings.sent)) {
            return;
        }
        SharedSettings.sent = values;
        for (final ServerPlayer player : server.server().getPlayerList().getPlayers()) {
            SGNetwork.send(player, new Packet(values));
        }
    }

    /** Forgets what the server sent, as it stops, so the next server sends its own. */
    public static void stopped() {
        SharedSettings.sent = null;
    }
}
