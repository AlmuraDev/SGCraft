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

package com.almuradev.sgcraft.mod.view;

import com.almuradev.sgcraft.mod.Platform;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.network.SGNetwork;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Remote views: a player watching what a source, such as a probe's camera, sees, while the source is elsewhere,
 * often in another world. The server streams the blocks, light, block entities and entities around the source to the
 * viewer, who draws them from the source's camera; the viewer's own world, position and loaded chunks are left as
 * they are, and no chunk is loaded for a view.
 *
 * <p>A viewer has at most one view of their own at a time, which their screens show, and any number of views shown on
 * displays in their world, such as a console's monitor, at most one for each display. A view ends when it is
 * stopped, when the viewer leaves, or when the source is removed. A source that goes to another world, through a gate
 * or otherwise, is followed by its UUID, as it turns up there as a new entity.</p>
 *
 * <p>Everything here runs on the server thread.</p>
 */
public final class RemoteViews {

    // Each view, by its viewer and the display it is shown on; a viewer's own view has no display in its key
    private static final Map<Key, RemoteView> VIEWS = new HashMap<>();
    // Where the views each redirected viewer starts are sent, in place of their connection
    private static final Map<UUID, Consumer<CustomPacketPayload>> SINKS = new HashMap<>();
    private static int nextId = 1;

    private RemoteViews() {
    }

    private record Key(UUID viewer, @Nullable BlockPos display) {
    }

    /**
     * Starts a viewer's own view of a source, in place of any own view they had. A viewer whose client can't show
     * remote views is not started, unless they are {@linkplain #redirect redirected}.
     */
    public static <T extends Entity & RemoteViewSource> void start(final ServerPlayer viewer, final T source) {
        RemoteViews.start(viewer, source, null);
    }

    /**
     * Starts a viewer's own view of a source, in place of any own view they had, telling their client that a display
     * in their world shows the same view. A viewer whose client can't show remote views is not started, unless they
     * are {@linkplain #redirect redirected}.
     *
     * @param shownAt the display in the viewer's world that shows the view, or null for none
     */
    public static <T extends Entity & RemoteViewSource> void start(final ServerPlayer viewer, final T source,
            final @Nullable BlockPos shownAt) {
        if (!RemoteViews.canView(viewer)) {
            return;
        }
        RemoteViews.begin(new Key(viewer.getUUID(), null), viewer, source, true,
                shownAt == null ? null : shownAt.immutable(), RemoteViews.reach(),
                RemoteViews.SINKS.get(viewer.getUUID()));
    }

    /**
     * Starts a view of a source that a display in a viewer's world shows them, in place of any view that display
     * showed them. Their own view, and the views on other displays, go on as they were. A viewer whose client can't
     * show remote views is not started, unless they are {@linkplain #redirect redirected}.
     */
    public static <T extends Entity & RemoteViewSource> void startOnDisplay(final ServerPlayer viewer,
            final BlockPos display, final T source) {
        if (!RemoteViews.canView(viewer)) {
            return;
        }
        final BlockPos at = display.immutable();
        RemoteViews.begin(new Key(viewer.getUUID(), at), viewer, source, false, at, RemoteViews.reach(),
                RemoteViews.SINKS.get(viewer.getUUID()));
    }

    /** @return whether a viewer can be started on a view: their client shows them, or they are redirected */
    public static boolean canView(final ServerPlayer viewer) {
        return RemoteViews.SINKS.containsKey(viewer.getUUID())
                || Platform.get().canSend(viewer, RemoteViewPackets.Frame.TYPE);
    }

    private static RemoteViewConfig reach() {
        return SGServer.current().map(server -> server.config().remoteView()).orElse(RemoteViewConfig.DEFAULTS);
    }

    /**
     * Sends what every view a viewer is started on from now on streams to {@code sink} in place of the viewer's
     * connection, whether or not their client can show remote views, until {@link #unredirect} is called for them or
     * the server stops. This is for tests that start views the way the game does.
     */
    public static void redirect(final ServerPlayer viewer, final Consumer<CustomPacketPayload> sink) {
        RemoteViews.SINKS.put(viewer.getUUID(), sink);
    }

    /** Sends the views a viewer is started on from now on to their connection again. */
    public static void unredirect(final ServerPlayer viewer) {
        RemoteViews.SINKS.remove(viewer.getUUID());
    }

    /**
     * Starts a viewer's own view of a source with a reach of its own, sending what the view streams to {@code sink}
     * in place of the viewer's connection. This is for tests and tools that look at what a view sends.
     */
    public static <T extends Entity & RemoteViewSource> void start(final ServerPlayer viewer, final T source,
            final RemoteViewConfig reach, final Consumer<CustomPacketPayload> sink) {
        RemoteViews.begin(new Key(viewer.getUUID(), null), viewer, source, true, null, reach, sink);
    }

    private static void begin(final Key key, final ServerPlayer viewer, final Entity source, final boolean own,
            final @Nullable BlockPos display, final RemoteViewConfig reach,
            final @Nullable Consumer<CustomPacketPayload> sink) {
        if (!(source.level() instanceof ServerLevel) || source.isRemoved()) {
            return;
        }
        RemoteViews.end(key, viewer);
        RemoteViews.VIEWS.put(key, new RemoteView(RemoteViews.nextId++, viewer, source, own, display, reach, sink));
    }

    private static void end(final Key key, final ServerPlayer viewer) {
        final RemoteView view = RemoteViews.VIEWS.remove(key);
        if (view != null) {
            view.end(viewer.level().getServer());
        }
    }

    /** Stops a viewer's own view, leaving the views on displays as they are. */
    public static void stop(final ServerPlayer viewer) {
        RemoteViews.end(new Key(viewer.getUUID(), null), viewer);
    }

    /** Stops the view a display in a viewer's world shows them, leaving their other views as they are. */
    public static void stopOnDisplay(final ServerPlayer viewer, final BlockPos display) {
        RemoteViews.end(new Key(viewer.getUUID(), display), viewer);
    }

    /** Stops every view a viewer has, their own and those on displays. */
    public static void stopAll(final ServerPlayer viewer) {
        for (final Key key : List.copyOf(RemoteViews.VIEWS.keySet())) {
            if (key.viewer().equals(viewer.getUUID())) {
                RemoteViews.end(key, viewer);
            }
        }
    }

    /** @return whether a viewer's own view is of a source, or of an entity that has its UUID */
    public static boolean watching(final ServerPlayer viewer, final Entity source) {
        final RemoteView view = RemoteViews.VIEWS.get(new Key(viewer.getUUID(), null));
        return view != null && view.source.equals(source.getUUID());
    }

    /** @return whether a display shows a viewer a source, or an entity that has its UUID */
    public static boolean watchingOnDisplay(final ServerPlayer viewer, final BlockPos display, final Entity source) {
        final RemoteView view = RemoteViews.VIEWS.get(new Key(viewer.getUUID(), display));
        return view != null && view.source.equals(source.getUUID());
    }

    /** @return the displays that show a viewer a view, in no particular order */
    public static Set<BlockPos> displays(final ServerPlayer viewer) {
        final Set<BlockPos> displays = new HashSet<>();
        for (final Key key : RemoteViews.VIEWS.keySet()) {
            if (key.viewer().equals(viewer.getUUID()) && key.display() != null) {
                displays.add(key.display());
            }
        }
        return displays;
    }

    /**
     * A viewer watching a source.
     *
     * @param world the world the source is in
     * @param display the display in the viewer's world that shows the view, or empty for the viewer's own view
     */
    public record Watch(UUID viewer, UUID source, ServerLevel world, Optional<BlockPos> display) {
    }

    /** @return every view, in no particular order */
    public static List<Watch> views() {
        final List<Watch> views = new ArrayList<>();
        for (final Map.Entry<Key, RemoteView> entry : RemoteViews.VIEWS.entrySet()) {
            final RemoteView view = entry.getValue();
            views.add(new Watch(view.viewer, view.source, view.level(),
                    Optional.ofNullable(entry.getKey().display())));
        }
        return views;
    }

    /** @return the world the source of a viewer's own view is in, if they have one */
    public static Optional<ServerLevel> world(final ServerPlayer viewer) {
        return Optional.ofNullable(RemoteViews.VIEWS.get(new Key(viewer.getUUID(), null))).map(RemoteView::level);
    }

    /**
     * @return the positions of the sections a viewer has been sent in their own view and not told to forget, packed
     *     as {@code SectionPos.asLong} packs them; empty if they have no own view
     */
    public static LongSet sentSections(final ServerPlayer viewer) {
        final RemoteView view = RemoteViews.VIEWS.get(new Key(viewer.getUUID(), null));
        return view == null ? LongSets.emptySet() : view.sentSections();
    }

    /** @return how many times a section has been sent to a viewer again because it changed, in their own view */
    public static int resends(final ServerPlayer viewer) {
        final RemoteView view = RemoteViews.VIEWS.get(new Key(viewer.getUUID(), null));
        return view == null ? 0 : view.resends();
    }

    /** Brings every view up to date by a tick. The server calls this at the end of each tick. */
    public static void tick(final MinecraftServer server) {
        final Iterator<RemoteView> iterator = RemoteViews.VIEWS.values().iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().tick(server)) {
                iterator.remove();
            }
        }
    }

    /** Forgets every view, as the server stops. */
    public static void clear() {
        RemoteViews.VIEWS.clear();
        RemoteViews.SINKS.clear();
    }

    static void send(final ServerPlayer player, final CustomPacketPayload payload) {
        SGNetwork.send(player, payload);
    }
}
