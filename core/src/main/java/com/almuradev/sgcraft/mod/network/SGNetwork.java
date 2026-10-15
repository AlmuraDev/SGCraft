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

package com.almuradev.sgcraft.mod.network;

import com.almuradev.sgcraft.mod.Platform;
import com.almuradev.sgcraft.mod.SGCraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Every payload SGCraft sends, declared once here so each loader registers the same set its own way. Payloads sent
 * to the server carry their handler; payloads sent to clients are handled by whatever the client side sets with
 * {@link #onClient}, so this class names no client code.
 */
public final class SGNetwork {

    /** The protocol version; both sides must agree. */
    public static final String VERSION = "1";

    private static final List<Serverbound<?>> SERVERBOUND = new ArrayList<>();
    private static final List<Clientbound<?>> CLIENTBOUND = new ArrayList<>();
    private static final Map<CustomPacketPayload.Type<?>, Consumer<?>> CLIENT_HANDLERS = new ConcurrentHashMap<>();

    private SGNetwork() {
    }

    /** A payload a client sends, and what the server does with it, on the server thread. */
    public record Serverbound<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<ServerPlayer, T> handler) {
    }

    /** A payload the server sends to a client. */
    public record Clientbound<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
    }

    /** Declares a packet clients send, read back with {@code reader}. */
    public static <T extends SGPacket> void serverbound(final CustomPacketPayload.Type<T> type,
            final Function<FriendlyByteBuf, T> reader, final BiConsumer<ServerPlayer, T> handler) {
        SGNetwork.SERVERBOUND.add(new Serverbound<>(type, SGNetwork.codec(reader), handler));
    }

    /** Declares a packet the server sends, read back with {@code reader}. */
    public static <T extends SGPacket> void clientbound(final CustomPacketPayload.Type<T> type,
            final Function<FriendlyByteBuf, T> reader) {
        SGNetwork.CLIENTBOUND.add(new Clientbound<>(type, SGNetwork.codec(reader)));
    }

    // The loaders' packet APIs take a stream codec; this is the only place SGCraft makes one
    private static <T extends SGPacket> StreamCodec<FriendlyByteBuf, T> codec(
            final Function<FriendlyByteBuf, T> reader) {
        return StreamCodec.ofMember(SGPacket::write, reader::apply);
    }

    /** @return every payload clients send, for the loader to register */
    public static List<Serverbound<?>> serverbound() {
        SGPayloads.declare();
        return List.copyOf(SGNetwork.SERVERBOUND);
    }

    /** @return every payload the server sends, for the loader to register */
    public static List<Clientbound<?>> clientbound() {
        SGPayloads.declare();
        return List.copyOf(SGNetwork.CLIENTBOUND);
    }

    /** Sets what a client does with a payload from the server. Only the client side calls this. */
    public static <T extends CustomPacketPayload> void onClient(final CustomPacketPayload.Type<T> type,
            final Consumer<T> handler) {
        SGNetwork.CLIENT_HANDLERS.put(type, handler);
    }

    /** Hands a payload from the server to its client handler. Loaders call this on the client's main thread. */
    @SuppressWarnings("unchecked")
    public static <T extends CustomPacketPayload> void handleOnClient(final T payload) {
        final Consumer<T> handler = (Consumer<T>) SGNetwork.CLIENT_HANDLERS.get(payload.type());
        if (handler == null) {
            SGCraft.LOG.warn("No client handler for {}", payload.type().id());
            return;
        }
        handler.accept(payload);
    }

    /** Sends a payload to one player, if their client can take it; a client without the mod gets nothing. */
    public static void send(final ServerPlayer player, final CustomPacketPayload payload) {
        if (Platform.get().canSend(player, payload.type())) {
            player.connection.send(new ClientboundCustomPayloadPacket(payload));
        }
    }
}
