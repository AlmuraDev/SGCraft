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

package com.almuradev.sgcraft.mod.client.malp;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.client.computer.ComputerCanvas;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.Feed;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.MalpInfo;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.MonitorState;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.Readout;
import com.almuradev.sgcraft.mod.malp.MalpSignal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * What the monitors of the MALP consoles near the client show around their feed, painted after a console's screen: on
 * the left, the MALPs paired with the console, the one shown marked; on the right, the feed in a teal-green frame with
 * corner brackets, its header naming the MALP shown, and over the feed the MALP's readouts and a crosshair while it is
 * live, or what keeps it from being live: FEED OFFLINE with no MALP to show or the feed powered down, SIGNAL LOST when
 * the console doesn't reach the MALP, and NO POWER when the MALP's battery is flat. The feed itself is left clear, for
 * the console's renderer to put the picture or static behind it.
 *
 * <p>The server says what each monitor shows a couple of times a second, to the players near it; a monitor it hasn't
 * spoken of lately shows nothing but its frame. A picture is painted again when what it shows changes, and a few
 * times a second besides, and let go once nothing has shown it for a few seconds. Only the render thread uses this.</p>
 */
public final class MalpMonitorDisplays {

    /** How wide a monitor's picture is, in pixels, across the console's three columns, as many to a block as up it. */
    public static final int WIDTH = 460;
    /** How high a monitor's picture is, in pixels. */
    public static final int HEIGHT = 170;
    /** Where the feed is in the picture, in pixels, at the feed's shape, to the right of the list of MALPs. */
    public static final int FEED_LEFT = 164;
    public static final int FEED_TOP = 3;
    public static final int FEED_RIGHT = 456;
    public static final int FEED_BOTTOM = 167;

    private static final int PANEL_RIGHT = 156;
    private static final int ROW = 12;
    private static final int BRACKET = 12;
    private static final int BACKGROUND = 0xFF040A09;
    private static final int FRAME = 0xFF2FBF9F;
    private static final int FRAME_DIM = 0x802FBF9F;
    private static final int SELECTED = 0x402FBF9F;
    private static final int TEXT = 0xFF7FFFD4;
    private static final int GRAY = 0xFF909090;
    private static final int WARNING = 0xFFFF5040;
    private static final int BACKDROP = 0xC0000000;
    private static final int HOSTILE_RANGE = 32;

    // How long what the server said of a monitor holds, how often a picture is painted again while it shows the
    // same, and how long a picture nothing shows is kept
    private static final long STALE_NANOS = 2_000_000_000L;
    private static final long REPAINT_NANOS = 250_000_000L;
    private static final long FORGET_NANOS = 5_000_000_000L;

    private static final Map<BlockPos, Received> STATES = new HashMap<>();
    private static final Map<BlockPos, Display> DISPLAYS = new HashMap<>();
    private static WeakReference<@Nullable ClientLevel> session = new WeakReference<>(null);

    private MalpMonitorDisplays() {
    }

    private record Received(MonitorState state, long at) {
    }

    /** Takes what the server says a console's monitor shows. */
    public static void update(final MonitorState state) {
        MalpMonitorDisplays.checkSession();
        MalpMonitorDisplays.STATES.put(state.console().immutable(), new Received(state, System.nanoTime()));
    }

    /**
     * @param picture whether the monitor shows the feed's picture, rather than static
     * @return the texture to draw over the monitor's screen, painted again if what it shows has changed, or it is due
     */
    public static Identifier texture(final BlockPos console, final boolean picture) {
        MalpMonitorDisplays.checkSession();
        final long now = System.nanoTime();
        MalpMonitorDisplays.forgetOld(now);
        final BlockPos pos = console.immutable();
        final Received received = MalpMonitorDisplays.STATES.get(pos);
        final MonitorState state = received == null || now - received.at > MalpMonitorDisplays.STALE_NANOS ? null
                : received.state;
        final Display display = MalpMonitorDisplays.DISPLAYS.computeIfAbsent(pos, Display::new);
        display.asked = now;
        if (state != display.state || picture != display.picture
                || now - display.painted >= MalpMonitorDisplays.REPAINT_NANOS) {
            display.state = state;
            display.picture = picture;
            display.painted = now;
            MalpMonitorDisplays.paint(display.canvas, state, picture);
            display.canvas.copyTo(display.texture.getPixels());
            display.texture.upload();
            if (!display.registered) {
                Minecraft.getInstance().getTextureManager().register(display.id, display.texture);
                display.registered = true;
            }
        }
        return display.id;
    }

    /**
     * @return whether the server last said a console's feed is offline, or that it has no MALPs paired, so its feed is
     *     plain dark rather than static
     */
    public static boolean feedDark(final BlockPos console) {
        final Received received = MalpMonitorDisplays.STATES.get(console);
        return received != null && (received.state.malps().isEmpty() || received.state.selected().isEmpty()
                || received.state.feed() == Feed.OFF);
    }

    // Everything is let go as the client changes worlds
    private static void checkSession() {
        final ClientLevel level = Minecraft.getInstance().level;
        if (MalpMonitorDisplays.session.get() != level) {
            MalpMonitorDisplays.clear();
            MalpMonitorDisplays.session = new WeakReference<>(level);
        }
    }

    private static void forgetOld(final long now) {
        final Iterator<Display> displays = MalpMonitorDisplays.DISPLAYS.values().iterator();
        while (displays.hasNext()) {
            final Display display = displays.next();
            if (now - display.asked > MalpMonitorDisplays.FORGET_NANOS) {
                display.free();
                displays.remove();
            }
        }
        MalpMonitorDisplays.STATES.values().removeIf(received -> now - received.at > MalpMonitorDisplays.FORGET_NANOS);
    }

    /** Lets every picture go, and forgets what the server said. */
    public static void clear() {
        for (final Display display : MalpMonitorDisplays.DISPLAYS.values()) {
            display.free();
        }
        MalpMonitorDisplays.DISPLAYS.clear();
        MalpMonitorDisplays.STATES.clear();
    }

    private static void paint(final ComputerCanvas canvas, final @Nullable MonitorState state, final boolean picture) {
        canvas.clear(MalpMonitorDisplays.BACKGROUND);
        MalpMonitorDisplays.panel(canvas, state);
        final int l = MalpMonitorDisplays.FEED_LEFT;
        final int t = MalpMonitorDisplays.FEED_TOP;
        final int r = MalpMonitorDisplays.FEED_RIGHT;
        final int b = MalpMonitorDisplays.FEED_BOTTOM;
        canvas.set(l, t, r - l, b - t, 0);
        MalpMonitorDisplays.frame(canvas);
        if (state == null) {
            MalpMonitorDisplays.header(canvas, MalpMonitorDisplays.text("screen.sgcraft.malp_console.systems",
                    "MALP SYSTEMS ONLINE"), null);
            return;
        }
        final String name = state.selected().flatMap(id -> state.malps().stream()
                .filter(malp -> malp.id().equals(id)).findFirst()).map(MalpInfo::name).orElse(null);
        final boolean live = state.feed() == Feed.LIVE && state.selected().isPresent();
        MalpMonitorDisplays.header(canvas, name != null ? name : MalpMonitorDisplays.text(
                "screen.sgcraft.malp_console.systems", "MALP SYSTEMS ONLINE"), live && picture);
        if (live) {
            MalpMonitorDisplays.crosshair(canvas);
            MalpMonitorDisplays.readouts(canvas, state.readout());
        } else {
            MalpMonitorDisplays.warning(canvas, state);
        }
    }

    // The MALPs paired with the console, the one shown marked, and whether someone is at it
    private static void panel(final ComputerCanvas canvas, final @Nullable MonitorState state) {
        canvas.rect(2, 2, MalpMonitorDisplays.PANEL_RIGHT - 2, MalpMonitorDisplays.HEIGHT - 4,
                MalpMonitorDisplays.FRAME_DIM);
        canvas.text(MalpMonitorDisplays.text("screen.sgcraft.malp_console.malps", "PAIRED MALPS"), 8, 8, 1,
                MalpMonitorDisplays.TEXT);
        canvas.fill(8, 18, MalpMonitorDisplays.PANEL_RIGHT - 14, 1, MalpMonitorDisplays.FRAME_DIM);
        final int bottom = MalpMonitorDisplays.HEIGHT - 24;
        if (state != null) {
            if (state.malps().isEmpty()) {
                canvas.text(MalpMonitorDisplays.text("screen.sgcraft.malp_monitor.none", "NO MALPS PAIRED"), 8, 24, 1,
                        MalpMonitorDisplays.GRAY);
            }
            int y = 24;
            for (final MalpInfo malp : state.malps()) {
                if (y + MalpMonitorDisplays.ROW > bottom) {
                    canvas.text("...", 8, y, 1, MalpMonitorDisplays.GRAY);
                    break;
                }
                final boolean shown = state.selected().filter(malp.id()::equals).isPresent();
                if (shown) {
                    canvas.fill(5, y - 2, MalpMonitorDisplays.PANEL_RIGHT - 8, MalpMonitorDisplays.ROW,
                            MalpMonitorDisplays.SELECTED);
                    canvas.fill(5, y - 2, 2, MalpMonitorDisplays.ROW, MalpMonitorDisplays.FRAME);
                }
                final int color = shown ? MalpMonitorDisplays.TEXT : MalpMonitorDisplays.GRAY;
                final String battery = malp.battery() < 0 ? "--" : malp.battery() + "%";
                final int batteryLeft = MalpMonitorDisplays.PANEL_RIGHT - 8 - canvas.textWidth(battery, 1);
                canvas.text(MalpMonitorDisplays.fit(canvas, malp.name(), batteryLeft - 16), 10, y, 1, color);
                canvas.text(battery, batteryLeft, y, 1, color);
                y += MalpMonitorDisplays.ROW;
            }
        }
        canvas.fill(8, bottom + 4, MalpMonitorDisplays.PANEL_RIGHT - 14, 1, MalpMonitorDisplays.FRAME_DIM);
        final boolean inUse = state != null && state.inUse();
        canvas.text(inUse ? MalpMonitorDisplays.text("screen.sgcraft.malp_monitor.in_use", "CONSOLE IN USE")
                : MalpMonitorDisplays.text("screen.sgcraft.malp_monitor.standby", "STANDBY"), 8, bottom + 9, 1,
                inUse ? MalpMonitorDisplays.TEXT : MalpMonitorDisplays.GRAY);
    }

    // The feed's frame and its corner brackets, just outside it
    private static void frame(final ComputerCanvas canvas) {
        final int l = MalpMonitorDisplays.FEED_LEFT;
        final int t = MalpMonitorDisplays.FEED_TOP;
        final int r = MalpMonitorDisplays.FEED_RIGHT;
        final int b = MalpMonitorDisplays.FEED_BOTTOM;
        final int k = MalpMonitorDisplays.BRACKET;
        final int c = MalpMonitorDisplays.FRAME;
        canvas.rect(l, t, r - l, b - t, MalpMonitorDisplays.FRAME_DIM);
        canvas.fill(l - 2, t - 2, k + 2, 2, c);
        canvas.fill(l - 2, t, 2, k, c);
        canvas.fill(r - k, t - 2, k + 2, 2, c);
        canvas.fill(r, t, 2, k, c);
        canvas.fill(l - 2, b, k + 2, 2, c);
        canvas.fill(l - 2, b - k, 2, k, c);
        canvas.fill(r - k, b, k + 2, 2, c);
        canvas.fill(r, b - k, 2, k, c);
    }

    // The header along the top of the feed: the MALP shown, and whether its video comes through, if known
    private static void header(final ComputerCanvas canvas, final String title, final @Nullable Boolean video) {
        final int l = MalpMonitorDisplays.FEED_LEFT;
        final int t = MalpMonitorDisplays.FEED_TOP;
        final int r = MalpMonitorDisplays.FEED_RIGHT;
        canvas.fill(l + 1, t + 1, r - l - 2, 12, MalpMonitorDisplays.BACKDROP);
        int titleRight = r - 6;
        if (video != null) {
            final String state = video
                    ? MalpMonitorDisplays.text("screen.sgcraft.malp_console.video", "VIDEO FEED ONLINE")
                    : MalpMonitorDisplays.text("screen.sgcraft.malp_console.video_lost", "VIDEO FEED LOST");
            final int width = canvas.textWidth(state, 1);
            canvas.text(state, r - 6 - width, t + 3, 1, video ? MalpMonitorDisplays.TEXT
                    : MalpMonitorDisplays.WARNING);
            titleRight = r - 6 - width - 12;
        }
        canvas.text(MalpMonitorDisplays.fit(canvas, title, titleRight - l - 6), l + 6, t + 3, 1,
                MalpMonitorDisplays.TEXT);
    }

    private static void crosshair(final ComputerCanvas canvas) {
        final int x = (MalpMonitorDisplays.FEED_LEFT + MalpMonitorDisplays.FEED_RIGHT) / 2;
        final int y = (MalpMonitorDisplays.FEED_TOP + MalpMonitorDisplays.FEED_BOTTOM) / 2;
        final int c = MalpMonitorDisplays.FRAME;
        canvas.fill(x - 12, y, 8, 1, c);
        canvas.fill(x + 5, y, 8, 1, c);
        canvas.fill(x, y - 12, 1, 8, c);
        canvas.fill(x, y + 5, 1, 8, c);
    }

    // The MALP's readouts, in four lines along the bottom of the feed
    private static void readouts(final ComputerCanvas canvas, final Readout readout) {
        final int l = MalpMonitorDisplays.FEED_LEFT;
        final int r = MalpMonitorDisplays.FEED_RIGHT;
        final int b = MalpMonitorDisplays.FEED_BOTTOM;
        canvas.fill(l + 1, b - 44, r - l - 2, 43, MalpMonitorDisplays.BACKDROP);
        final BlockPos pos = readout.pos();
        final String gate = readout.gate().map(near -> MalpMonitorDisplays.text(
                "screen.sgcraft.malp_monitor.readout_gate", "GATE %s %sm %s", near.address().isEmpty()
                        ? MalpMonitorDisplays.text("screen.sgcraft.malp_console.readout_gate_hidden", "HIDDEN")
                        : near.address(), near.distance(), MalpConsoleScreen.compass(near.yaw(), false)))
                .orElseGet(() -> MalpMonitorDisplays.text("screen.sgcraft.malp_console.readout_no_gate",
                        "NO GATE IN RANGE"));
        final String position = MalpMonitorDisplays.text("screen.sgcraft.malp_monitor.readout_position",
                "POS %s %s %s  HEADING %s  %s", pos.getX(), pos.getY(), pos.getZ(),
                MalpConsoleScreen.compass(readout.yaw(), true), gate);
        final String place = MalpMonitorDisplays.text("screen.sgcraft.malp_monitor.readout_place",
                "WORLD %s  BIOME %s", MalpConsoleScreen.path(readout.world()), MalpConsoleScreen.path(readout.biome()));
        final String status = MalpMonitorDisplays.text("screen.sgcraft.malp_monitor.readout_status",
                "TEMP %s C  TIME %s  LIGHT %s  HOSTILES %s/%sm", Math.round(readout.temperature() * 25),
                MalpConsoleScreen.clock(readout.time()), readout.light(), readout.hostiles(),
                MalpMonitorDisplays.HOSTILE_RANGE);
        final String battery = readout.powerRequired()
                ? MalpMonitorDisplays.text("screen.sgcraft.malp_console.readout_battery", "BATTERY %s%%",
                        readout.battery())
                : MalpMonitorDisplays.text("screen.sgcraft.malp_console.readout_no_power", "NEEDS NO POWER");
        final String signal = MalpSignal.Kind.RELAYED.name().equals(readout.signal())
                ? MalpMonitorDisplays.text("screen.sgcraft.malp_console.signal_relayed", "RELAYED %s",
                        readout.relay())
                : MalpMonitorDisplays.text("screen.sgcraft.malp_console.signal_direct", "DIRECT");
        final String power = MalpMonitorDisplays.text("screen.sgcraft.malp_monitor.readout_power",
                "%s  SIGNAL %s", battery, signal);
        final int width = r - l - 12;
        canvas.text(MalpMonitorDisplays.fit(canvas, position, width), l + 6, b - 41, 1, MalpMonitorDisplays.TEXT);
        canvas.text(MalpMonitorDisplays.fit(canvas, place, width), l + 6, b - 31, 1, MalpMonitorDisplays.TEXT);
        canvas.text(MalpMonitorDisplays.fit(canvas, status, width), l + 6, b - 21, 1, MalpMonitorDisplays.TEXT);
        canvas.text(MalpMonitorDisplays.fit(canvas, power, width), l + 6, b - 11, 1,
                readout.powerRequired() && readout.battery() <= 10 ? MalpMonitorDisplays.WARNING
                        : MalpMonitorDisplays.TEXT);
    }

    // FEED OFFLINE, SIGNAL LOST or NO POWER, large in the middle of the feed
    private static void warning(final ComputerCanvas canvas, final MonitorState state) {
        final String text = state.malps().isEmpty()
                ? MalpMonitorDisplays.text("screen.sgcraft.malp_console.no_feeds", "NO FEEDS AVAILABLE")
                : state.selected().isEmpty() || state.feed() == Feed.OFF
                ? MalpMonitorDisplays.text("screen.sgcraft.malp_console.off", "FEED OFFLINE")
                : state.feed() == Feed.NO_POWER
                        ? MalpMonitorDisplays.text("screen.sgcraft.malp_console.no_power", "NO POWER")
                        : MalpMonitorDisplays.text("screen.sgcraft.malp_monitor.signal_lost", "SIGNAL LOST");
        final int x = (MalpMonitorDisplays.FEED_LEFT + MalpMonitorDisplays.FEED_RIGHT) / 2;
        final int y = (MalpMonitorDisplays.FEED_TOP + MalpMonitorDisplays.FEED_BOTTOM) / 2;
        final int half = canvas.textWidth(text, 2) / 2;
        canvas.fill(x - half - 8, y - 12, 2 * half + 16, 24, MalpMonitorDisplays.BACKDROP);
        canvas.centeredText(text, x, y - 7, 2, MalpMonitorDisplays.WARNING);
    }

    // Text cut short, with an ellipsis, to fit a width
    private static String fit(final ComputerCanvas canvas, final String text, final int width) {
        if (canvas.textWidth(text, 1) <= width) {
            return text;
        }
        String cut = text;
        while (!cut.isEmpty() && canvas.textWidth(cut + "...", 1) > width) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "...";
    }

    private static String text(final String key, final String fallback, final Object... args) {
        return Component.translatableWithFallback(key, fallback, args).getString();
    }

    // One monitor's picture: painted in memory, then handed to its texture
    private static final class Display {

        final Identifier id;
        final ComputerCanvas canvas = new ComputerCanvas(MalpMonitorDisplays.WIDTH, MalpMonitorDisplays.HEIGHT);
        final DynamicTexture texture;
        @Nullable MonitorState state;
        boolean picture;
        long painted = Long.MIN_VALUE / 2;
        long asked;
        boolean registered;

        Display(final BlockPos pos) {
            this.id = SGCraft.id("dynamic/malp_monitor_" + Long.toHexString(pos.asLong()).toLowerCase(Locale.ROOT));
            this.texture = new DynamicTexture(() -> "SGCraft MALP monitor " + pos.toShortString(),
                    MalpMonitorDisplays.WIDTH, MalpMonitorDisplays.HEIGHT, true);
        }

        void free() {
            if (this.registered) {
                // Closes the texture too
                Minecraft.getInstance().getTextureManager().release(this.id);
            } else {
                this.texture.close();
            }
        }
    }
}
