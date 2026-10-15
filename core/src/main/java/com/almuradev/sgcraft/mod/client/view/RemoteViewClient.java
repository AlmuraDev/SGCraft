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

package com.almuradev.sgcraft.mod.client.view;

import com.almuradev.sgcraft.mod.network.SGNetwork;
import com.almuradev.sgcraft.mod.view.RemoteViewPackets;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

/**
 * The client side of remote views: takes the server's stream into the scene of the view it belongs to, and drops
 * the scene, with everything it holds on the GPU, when the view ends or the client leaves the server. The client has
 * at most one view of its own, which screens show, and any number shown on displays in its world. Only the client's
 * main thread uses this.
 */
public final class RemoteViewClient {

    // Each view being streamed, by its number
    private static final Int2ObjectMap<RemoteViewScene> SCENES = new Int2ObjectOpenHashMap<>();

    private RemoteViewClient() {
    }

    /** Sets up the client's handlers for the stream. The client calls this once. */
    public static void init() {
        SGNetwork.onClient(RemoteViewPackets.Begin.TYPE, RemoteViewClient::begin);
        SGNetwork.onClient(RemoteViewPackets.Sections.TYPE, RemoteViewClient.forScene(
                RemoteViewPackets.Sections::view, RemoteViewScene::sections));
        SGNetwork.onClient(RemoteViewPackets.BlockEntities.TYPE, RemoteViewClient.forScene(
                RemoteViewPackets.BlockEntities::view, RemoteViewScene::blockEntities));
        SGNetwork.onClient(RemoteViewPackets.Entities.TYPE, RemoteViewClient.forScene(
                RemoteViewPackets.Entities::view, RemoteViewScene::entities));
        SGNetwork.onClient(RemoteViewPackets.Frame.TYPE, RemoteViewClient.forScene(
                RemoteViewPackets.Frame::view, RemoteViewScene::frame));
        SGNetwork.onClient(RemoteViewPackets.End.TYPE, end -> RemoteViewClient.close(end.view()));
    }

    /** Drops every view, as the client leaves the server. Each loader calls this as its client disconnects. */
    public static void disconnected() {
        for (final int id : List.copyOf(RemoteViewClient.SCENES.keySet())) {
            RemoteViewClient.close(id);
        }
        RemoteViewDisplays.clear();
    }

    /** @return the client's own view, which screens show, or null if there is none */
    static @Nullable RemoteViewScene scene() {
        for (final RemoteViewScene scene : RemoteViewClient.SCENES.values()) {
            if (scene.world.own()) {
                return scene;
            }
        }
        return null;
    }

    /** @return a view the display at a position in the client's world shows, or null if it shows none */
    static @Nullable RemoteViewScene sceneOn(final BlockPos display) {
        for (final RemoteViewScene scene : RemoteViewClient.SCENES.values()) {
            if (scene.world.display().filter(display::equals).isPresent()) {
                return scene;
            }
        }
        return null;
    }

    // A view beginning replaces what the client had of it, and any other view in the same place: another own view,
    // or another view on the same display
    private static void begin(final RemoteViewPackets.Begin begin) {
        RemoteViewClient.close(begin.view());
        for (final RemoteViewScene scene : List.copyOf(RemoteViewClient.SCENES.values())) {
            final boolean replaced = begin.own() ? scene.world.own()
                    : !scene.world.own() && scene.world.display().equals(begin.display());
            if (replaced) {
                RemoteViewClient.close(scene.id);
            }
        }
        RemoteViewClient.SCENES.put(begin.view(), new RemoteViewScene(begin));
    }

    // A packet left over from a view that has ended, or from before a view began, is dropped
    private static <T> Consumer<T> forScene(final ToIntFunction<T> view,
            final BiConsumer<RemoteViewScene, T> handler) {
        return packet -> {
            final RemoteViewScene scene = RemoteViewClient.SCENES.get(view.applyAsInt(packet));
            if (scene != null) {
                handler.accept(scene, packet);
            }
        };
    }

    private static void close(final int id) {
        final RemoteViewScene scene = RemoteViewClient.SCENES.remove(id);
        if (scene != null) {
            scene.close();
        }
    }
}
