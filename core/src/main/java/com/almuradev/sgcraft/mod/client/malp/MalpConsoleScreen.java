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
import com.almuradev.sgcraft.mod.client.ClientPlatform;
import com.almuradev.sgcraft.mod.client.view.RemoteViewRenderer;
import com.almuradev.sgcraft.mod.client.view.RemoteViewSnapshot;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.ConsoleAction;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.ConsoleState;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.Drive;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.Feed;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.MalpInfo;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.Readout;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.Role;
import com.almuradev.sgcraft.mod.malp.MalpSignal;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The screen of a MALP console or MALP tablet, after the show's MALP feed: a large feed in a teal-green frame with
 * corner brackets, a crosshair, and the MALP's readouts along the bottom; beside it, the MALPs paired with the console
 * or tablet, and buttons for the lamp and the feed. W and S drive, A and D turn, dragging on the feed pans and tilts
 * the camera, and L works the lamp, using the player's own movement keys. Without a signal the feed shows static. A
 * tablet's screen also shows the charge of the tablet's own battery, shows nothing but NO TABLET POWER while that
 * battery is flat or missing, and has a button beside each MALP to take it off the tablet's list.
 *
 * <p>P, or the Snapshot button, saves the MALP's picture alone, without the screen around it, to
 * {@code screenshots/malp} in the game's folder, and says so in chat. The feed flashes and its header says SNAPSHOT
 * SAVED for it, and for a screenshot taken with the game's own screenshot key, whose message in chat the screen
 * hides; with no picture to save, the header says NO PICTURE.</p>
 *
 * <p>A player who opens a console someone else is at only watches: the screen is marked VIEW ONLY, shows that
 * player's feed and readouts, and has nothing to press but Snapshot and Close. A player who chooses a MALP someone
 * else drives watches it the same way, but may still choose another MALP. Once the player they watch leaves, the feed
 * shows static until the screen is closed or another MALP is chosen.</p>
 */
public final class MalpConsoleScreen extends Screen {

    private static final Identifier STATIC = SGCraft.id("textures/gui/malp_static.png");
    private static final int STATIC_SIZE = 128;
    private static final int STATIC_TILE = 64;
    private static final int MARGIN = 8;
    private static final int PANEL = 112;
    private static final int ROW = 20;
    private static final int FRAME = 0xFF2FBF9F;
    private static final int FRAME_DIM = 0x802FBF9F;
    private static final int TEXT = 0xFF7FFFD4;
    private static final int WARNING = 0xFFFF5040;
    private static final int VIEW_ONLY = 0xFFFFC040;
    private static final int BACKDROP = 0xC0000000;
    private static final int STATIC_TINT = 0xFFB0D8C8;
    private static final int BRACKET = 12;
    // How far apart the readouts' lines are, and the space between the parts on a line
    private static final int READOUT_LINE = 11;
    private static final String READOUT_GAP = "   ";
    private static final String[] POINTS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
    // How far the camera turns for each pixel the mouse is dragged
    private static final float AIM_PER_PIXEL = 0.6F;
    private static final int HOSTILE_RANGE = 32;
    // How long the feed's flash takes to fade, and how long the header says a snapshot was saved
    private static final long FLASH_MILLIS = 300;
    private static final long NOTICE_MILLIS = 2000;
    private static final String SNAPSHOT_FOLDER = "malp";

    // The width of the button beside each MALP on a tablet's list that takes it off the list
    private static final int REMOVE_WIDTH = 18;

    private final RandomSource noise = RandomSource.create();
    private ConsoleState state;
    private @Nullable Button center;
    private @Nullable Button lamp;
    private @Nullable Button feed;
    private boolean forward;
    private boolean back;
    private boolean left;
    private boolean right;
    private boolean dragging;
    private float pan;
    private float tilt;
    private int feedLeft;
    private int feedTop;
    private int feedRight;
    private int feedBottom;
    // Whether the feed showed a picture in the last frame
    private boolean picture;
    // Whether the flash starts in the next frame, and when it started, or -1 before the first
    private boolean flashNext;
    private long flashStart = -1;
    // What the header says in place of the feed's state, in what color, and until when
    private @Nullable Component notice;
    private int noticeColor;
    private long noticeUntil;

    public MalpConsoleScreen(final ConsoleState state) {
        super(state.tablet() ? Component.translatableWithFallback("screen.sgcraft.malp_tablet", "MALP Tablet")
                : Component.translatableWithFallback("screen.sgcraft.malp_console", "MALP Console"));
        this.state = state;
    }

    /** Shows a newer state, closing the screen if the server says to. */
    public void update(final ConsoleState state) {
        final boolean listChanged = !state.malps().equals(this.state.malps())
                || !state.selected().equals(this.state.selected()) || state.role() != this.state.role();
        this.state = state;
        if (state.close()) {
            this.minecraft.gui.setScreen(null);
            return;
        }
        if (listChanged) {
            this.rebuildWidgets();
        } else {
            this.updateButtons();
        }
    }

    @Override
    protected void init() {
        this.feedLeft = MalpConsoleScreen.MARGIN * 2 + MalpConsoleScreen.PANEL;
        this.feedTop = MalpConsoleScreen.MARGIN;
        this.feedRight = this.width - MalpConsoleScreen.MARGIN;
        this.feedBottom = this.height - MalpConsoleScreen.MARGIN;
        final int x = MalpConsoleScreen.MARGIN;
        int y = MalpConsoleScreen.MARGIN + 14;
        // A tablet's list has a button beside each MALP to take it off, and a console's, for its driver, to unpair it
        final boolean removable = this.state.tablet() || this.state.role() == Role.DRIVER;
        final int width = MalpConsoleScreen.PANEL - (removable ? MalpConsoleScreen.REMOVE_WIDTH + 2 : 0);
        for (final MalpInfo malp : this.state.malps()) {
            // The list stops above the help and the buttons along the bottom
            if (y + 18 > this.listBottom()) {
                break;
            }
            final boolean chosen = this.state.selected().filter(malp.id()::equals).isPresent();
            final Component label = Component.literal((chosen ? "> " : "") + malp.name());
            final Button button = this.addRenderableWidget(Button.builder(label,
                    b -> this.send(new ConsoleAction(ConsoleAction.Kind.SELECT, Optional.of(malp.id()))))
                    .bounds(x, y, width, 18).build());
            button.active = !chosen && this.state.role() != Role.CONSOLE_WATCHER;
            if (removable) {
                this.addRenderableWidget(Button.builder(Component.literal("x"),
                        b -> this.send(new ConsoleAction(ConsoleAction.Kind.REMOVE, Optional.of(malp.id()))))
                        .bounds(x + MalpConsoleScreen.PANEL - MalpConsoleScreen.REMOVE_WIDTH, y,
                                MalpConsoleScreen.REMOVE_WIDTH, 18)
                        .tooltip(Tooltip.create(this.state.tablet()
                                ? Component.translatableWithFallback("screen.sgcraft.malp_tablet.remove",
                                        "Take %s off this tablet", malp.name())
                                : Component.translatableWithFallback("screen.sgcraft.malp_console.unpair",
                                        "Unpair %s from this console. Only its owner and operators can",
                                        malp.name())))
                        .build());
            }
            y += MalpConsoleScreen.ROW;
        }
        final int bottom = this.height - MalpConsoleScreen.MARGIN;
        this.addRenderableWidget(Button.builder(Component.translatableWithFallback("screen.sgcraft.close", "Close"),
                b -> this.onClose()).bounds(x, bottom - MalpConsoleScreen.ROW, MalpConsoleScreen.PANEL, 18).build());
        // Watchers take snapshots too, so theirs sits over Close, and the driver's over the console's own buttons
        this.addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "screen.sgcraft.malp_console.snapshot", "Snapshot"), b -> this.snapshot())
                .bounds(x, bottom - (this.state.viewOnly() ? 2 : 5) * MalpConsoleScreen.ROW, MalpConsoleScreen.PANEL,
                        18).build());
        if (this.state.viewOnly()) {
            // Only the driver works the console
            this.center = null;
            this.lamp = null;
            this.feed = null;
            return;
        }
        this.center = this.addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "screen.sgcraft.malp_console.center", "Center Camera"),
                b -> this.send(ConsoleAction.of(ConsoleAction.Kind.CENTER)))
                .bounds(x, bottom - 4 * MalpConsoleScreen.ROW, MalpConsoleScreen.PANEL, 18).build());
        this.lamp = this.addRenderableWidget(Button.builder(Component.empty(),
                b -> this.send(ConsoleAction.of(ConsoleAction.Kind.LAMP)))
                .bounds(x, bottom - 3 * MalpConsoleScreen.ROW, MalpConsoleScreen.PANEL, 18).build());
        this.feed = this.addRenderableWidget(Button.builder(Component.empty(),
                b -> this.send(ConsoleAction.of(ConsoleAction.Kind.FEED)))
                .bounds(x, bottom - 2 * MalpConsoleScreen.ROW, MalpConsoleScreen.PANEL, 18).build());
        this.updateButtons();
    }

    private void updateButtons() {
        if (this.center != null) {
            this.center.active = this.state.feed() == Feed.LIVE;
        }
        if (this.lamp != null) {
            this.lamp.setMessage(this.state.readout().lamp()
                    ? Component.translatableWithFallback("screen.sgcraft.malp_console.lamp_off", "Lamp Off")
                    : Component.translatableWithFallback("screen.sgcraft.malp_console.lamp_on", "Lamp On"));
            this.lamp.active = this.state.feed() == Feed.LIVE;
        }
        if (this.feed != null) {
            this.feed.setMessage(this.state.feed() == Feed.OFF
                    ? Component.translatableWithFallback("screen.sgcraft.malp_console.feed_on", "Power Up Feed")
                    : Component.translatableWithFallback("screen.sgcraft.malp_console.feed_off", "Power Down Feed"));
            this.feed.active = this.state.selected().isPresent();
        }
    }

    private void send(final ConsoleAction action) {
        ClientPlatform.get().sendToServer(action);
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        if (!this.state.viewOnly()) {
            if (this.movementKey(event, true)) {
                return true;
            }
            if (event.key() == InputConstants.KEY_L) {
                this.send(ConsoleAction.of(ConsoleAction.Kind.LAMP));
                return true;
            }
            if (event.key() == InputConstants.KEY_C) {
                this.send(ConsoleAction.of(ConsoleAction.Kind.CENTER));
                return true;
            }
        }
        if (event.key() == InputConstants.KEY_P) {
            this.snapshot();
            return true;
        }
        return super.keyPressed(event);
    }

    /**
     * Shows on an open MALP console's or tablet's screen that a screenshot was taken with the game's screenshot key:
     * the feed flashes from the next frame, after the screenshot has been read, and the header says SNAPSHOT SAVED.
     */
    public static void screenshotTaken(final Minecraft minecraft) {
        if (minecraft.gui.screen() instanceof MalpConsoleScreen screen) {
            screen.snapped();
        }
    }

    private void snapped() {
        this.flashNext = true;
        this.notice(Component.translatableWithFallback("screen.sgcraft.malp_console.snapshot_saved",
                "SNAPSHOT SAVED"), MalpConsoleScreen.TEXT);
    }

    private void notice(final Component text, final int color) {
        this.notice = text;
        this.noticeColor = color;
        this.noticeUntil = Util.getMillis() + MalpConsoleScreen.NOTICE_MILLIS;
    }

    // Saves the MALP's picture alone, at the feed's shape, if the feed shows one
    private void snapshot() {
        final int width = this.feedRight - this.feedLeft - 4;
        final int height = this.feedBottom - this.feedTop - 4;
        if (!this.picture || width <= 0 || height <= 0) {
            this.noPicture();
            return;
        }
        RemoteViewSnapshot.request((float) width / height, MalpConsoleScreen::saveSnapshot);
    }

    private void noPicture() {
        this.notice(Component.translatableWithFallback("screen.sgcraft.malp_console.no_picture", "NO PICTURE"),
                MalpConsoleScreen.WARNING);
    }

    // Writes the picture as a screenshot is written, named for the time it was taken, and says so in chat with a
    // link to the file; the screen, if still open, shows it was taken
    private static void saveSnapshot(final @Nullable NativeImage image) {
        final Minecraft minecraft = Minecraft.getInstance();
        final MalpConsoleScreen screen = minecraft.gui.screen() instanceof MalpConsoleScreen open ? open : null;
        if (image == null) {
            if (screen != null) {
                screen.noPicture();
            }
            return;
        }
        if (screen != null) {
            screen.snapped();
        }
        final File folder = new File(new File(minecraft.gameDirectory, Screenshot.SCREENSHOT_DIR),
                MalpConsoleScreen.SNAPSHOT_FOLDER);
        folder.mkdirs();
        final File file = MalpConsoleScreen.snapshotFile(folder);
        Util.ioPool().execute(() -> {
            try (image) {
                image.writeToFile(file);
                final Component name = Component.literal(file.getName()).withStyle(ChatFormatting.UNDERLINE)
                        .withStyle(style -> style.withClickEvent(new ClickEvent.OpenFile(file.getAbsoluteFile())));
                minecraft.execute(() -> minecraft.showDebugChat(Component.translatableWithFallback(
                        "message.sgcraft.malp.snapshot", "Saved MALP snapshot as %s", name)));
            } catch (final IOException | RuntimeException e) {
                SGCraft.LOG.warn("Couldn't save a MALP snapshot", e);
                minecraft.execute(() -> minecraft.showDebugChat(Component.translatableWithFallback(
                        "message.sgcraft.malp.snapshot_failed", "The MALP snapshot couldn't be saved: %s",
                        String.valueOf(e.getMessage()))));
            }
        });
    }

    // A file not yet there, named for the date and time as the game names screenshots
    private static File snapshotFile(final File folder) {
        final String name = Util.getFilenameFormattedDateTime();
        for (int count = 1; ; count++) {
            final File file = new File(folder, name + (count == 1 ? "" : "_" + count) + ".png");
            if (!file.exists()) {
                return file;
            }
        }
    }

    @Override
    public boolean keyReleased(final KeyEvent event) {
        return !this.state.viewOnly() && this.movementKey(event, false) || super.keyReleased(event);
    }

    // The player's own forward, back, left and right keys drive, so they follow the player's key settings
    private boolean movementKey(final KeyEvent event, final boolean down) {
        final Options options = this.minecraft.options;
        if (options.keyUp.matches(event)) {
            this.forward = down;
        } else if (options.keyDown.matches(event)) {
            this.back = down;
        } else if (options.keyLeft.matches(event)) {
            this.left = down;
        } else if (options.keyRight.matches(event)) {
            this.right = down;
        } else {
            return false;
        }
        return true;
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (!this.state.viewOnly() && event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && this.inFeed(event.x(), event.y())) {
            this.dragging = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dx, final double dy) {
        if (this.dragging) {
            this.pan += (float) dx * MalpConsoleScreen.AIM_PER_PIXEL;
            this.tilt += (float) dy * MalpConsoleScreen.AIM_PER_PIXEL;
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        this.dragging = false;
        return super.mouseReleased(event);
    }

    private boolean inFeed(final double x, final double y) {
        return x >= this.feedLeft && x < this.feedRight && y >= this.feedTop && y < this.feedBottom;
    }

    // Sends the controls each tick while any are held, which is twenty times a second
    @Override
    public void tick() {
        if (this.state.viewOnly()) {
            return;
        }
        final float drive = (this.forward ? 1 : 0) - (this.back ? 1 : 0);
        final float turn = (this.right ? 1 : 0) - (this.left ? 1 : 0);
        if (drive != 0 || turn != 0 || this.pan != 0 || this.tilt != 0) {
            ClientPlatform.get().sendToServer(new Drive(drive, turn, this.pan, this.tilt));
            this.pan = 0;
            this.tilt = 0;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        this.send(ConsoleAction.of(ConsoleAction.Kind.CLOSE));
        super.removed();
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
            final float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.panel(graphics);
        final boolean live = this.state.feed() == Feed.LIVE;
        final boolean picture = live && RemoteViewRenderer.extract(graphics, this.feedLeft + 2, this.feedTop + 2,
                this.feedRight - 2, this.feedBottom - 2, partialTick);
        this.picture = picture;
        if (!picture) {
            graphics.fill(this.feedLeft, this.feedTop, this.feedRight, this.feedBottom, 0xFF000000);
            // Static while the feed looks for a signal or power; an offline feed, a tablet with no power, or no MALPs
            // paired at all shows nothing
            if (this.state.feed() != Feed.NO_TABLET_POWER && this.state.feed() != Feed.OFF
                    && !this.state.malps().isEmpty()) {
                this.staticNoise(graphics);
            }
        }
        this.frame(graphics, live);
        if (live) {
            this.crosshair(graphics);
            this.readouts(graphics);
        } else {
            this.warning(graphics);
        }
        this.flash(graphics);
    }

    // A white flash over the feed, fading out
    private void flash(final GuiGraphicsExtractor graphics) {
        final long now = Util.getMillis();
        if (this.flashNext) {
            this.flashNext = false;
            this.flashStart = now;
        }
        final long since = now - this.flashStart;
        if (this.flashStart < 0 || since >= MalpConsoleScreen.FLASH_MILLIS) {
            return;
        }
        final int alpha = (int) (255 * (1 - (float) since / MalpConsoleScreen.FLASH_MILLIS));
        graphics.fill(this.feedLeft, this.feedTop, this.feedRight, this.feedBottom, alpha << 24 | 0xFFFFFF);
    }

    private void panel(final GuiGraphicsExtractor graphics) {
        graphics.text(this.font, Component.translatableWithFallback("screen.sgcraft.malp_console.malps",
                "PAIRED MALPS"), MalpConsoleScreen.MARGIN, MalpConsoleScreen.MARGIN + 2, MalpConsoleScreen.TEXT);
        if (this.state.malps().isEmpty()) {
            graphics.textWithWordWrap(this.font, this.state.tablet()
                    ? Component.translatableWithFallback("screen.sgcraft.malp_tablet.none",
                            "No MALPs are paired. Sneak and use this tablet on a MALP to pair it")
                    : Component.translatableWithFallback("screen.sgcraft.malp_console.none",
                            "No MALPs are paired. Use a MALP on this console to pair it"),
                    MalpConsoleScreen.MARGIN, MalpConsoleScreen.MARGIN + 16, MalpConsoleScreen.PANEL, 0xFFAAAAAA);
        }
        if (this.state.tablet()) {
            this.tabletBattery(graphics);
        }
        if (this.state.viewOnly()) {
            graphics.textWithWordWrap(this.font, this.state.role() == Role.MALP_WATCHER
                    ? Component.translatableWithFallback("screen.sgcraft.malp_console.malp_view_only_help",
                            "Someone else is driving this MALP. You can watch, or choose another MALP.")
                    : Component.translatableWithFallback("screen.sgcraft.malp_console.view_only_help",
                            "Someone else is driving at this console. You can watch, but not drive"),
                    MalpConsoleScreen.MARGIN, this.helpTop(), MalpConsoleScreen.PANEL, 0xFF909090);
            return;
        }
        int y = this.helpTop();
        for (final Component line : MalpConsoleScreen.help()) {
            graphics.text(this.font, line, MalpConsoleScreen.MARGIN, y, 0xFF909090);
            y += 12;
        }
    }

    private static List<Component> help() {
        return List.of(
                Component.translatableWithFallback("screen.sgcraft.malp_console.help_drive", "W S: drive"),
                Component.translatableWithFallback("screen.sgcraft.malp_console.help_turn", "A D: turn"),
                Component.translatableWithFallback("screen.sgcraft.malp_console.help_camera", "Drag: camera"),
                Component.translatableWithFallback("screen.sgcraft.malp_console.help_center", "C: center camera"),
                Component.translatableWithFallback("screen.sgcraft.malp_console.help_lamp", "L: lamp"),
                Component.translatableWithFallback("screen.sgcraft.malp_console.help_snapshot", "P: snapshot"));
    }

    // The charge of a tablet's own battery, just above the help
    private void tabletBattery(final GuiGraphicsExtractor graphics) {
        final int percent = this.state.tabletBattery();
        final Component text = percent < 0
                ? Component.translatableWithFallback("screen.sgcraft.malp_tablet.no_battery", "NO TABLET BATTERY")
                : Component.translatableWithFallback("screen.sgcraft.malp_tablet.battery", "TABLET BATTERY %s%%",
                        percent);
        graphics.text(this.font, text, MalpConsoleScreen.MARGIN, this.helpTop() - 12, percent <= 10
                ? MalpConsoleScreen.WARNING : MalpConsoleScreen.TEXT);
    }

    // Where the list of MALPs stops: above the help, and above a tablet's battery
    private int listBottom() {
        return this.helpTop() - 2 - (this.state.tablet() ? 12 : 0);
    }

    // Where the help starts, above the buttons along the bottom: the driver's keys, or a watcher's few words
    private int helpTop() {
        final int bottom = this.height - MalpConsoleScreen.MARGIN;
        return this.state.viewOnly() ? bottom - 2 * MalpConsoleScreen.ROW - 40
                : bottom - 5 * MalpConsoleScreen.ROW - 12 * MalpConsoleScreen.help().size() - 4;
    }

    // Snow, moved about each frame, tinted a little green
    private void staticNoise(final GuiGraphicsExtractor graphics) {
        for (int y = this.feedTop; y < this.feedBottom; y += MalpConsoleScreen.STATIC_TILE) {
            for (int x = this.feedLeft; x < this.feedRight; x += MalpConsoleScreen.STATIC_TILE) {
                final int width = Math.min(MalpConsoleScreen.STATIC_TILE, this.feedRight - x);
                final int height = Math.min(MalpConsoleScreen.STATIC_TILE, this.feedBottom - y);
                final int u = this.noise.nextInt(MalpConsoleScreen.STATIC_SIZE - MalpConsoleScreen.STATIC_TILE);
                final int v = this.noise.nextInt(MalpConsoleScreen.STATIC_SIZE - MalpConsoleScreen.STATIC_TILE);
                graphics.blit(RenderPipelines.GUI_TEXTURED, MalpConsoleScreen.STATIC, x, y, u, v, width, height,
                        MalpConsoleScreen.STATIC_SIZE, MalpConsoleScreen.STATIC_SIZE, MalpConsoleScreen.STATIC_TINT);
            }
        }
    }

    // The frame, its corner brackets, and the header along its top
    private void frame(final GuiGraphicsExtractor graphics, final boolean live) {
        final int l = this.feedLeft;
        final int t = this.feedTop;
        final int r = this.feedRight;
        final int b = this.feedBottom;
        graphics.outline(l, t, r - l, b - t, MalpConsoleScreen.FRAME_DIM);
        final int k = MalpConsoleScreen.BRACKET;
        final int c = MalpConsoleScreen.FRAME;
        graphics.fill(l - 2, t - 2, l + k, t, c);
        graphics.fill(l - 2, t, l, t + k, c);
        graphics.fill(r - k, t - 2, r + 2, t, c);
        graphics.fill(r, t, r + 2, t + k, c);
        graphics.fill(l - 2, b, l + k, b + 2, c);
        graphics.fill(l - 2, b - k, l, b, c);
        graphics.fill(r - k, b, r + 2, b + 2, c);
        graphics.fill(r, b - k, r + 2, b, c);
        graphics.fill(l + 1, t + 1, r - 1, t + 14, MalpConsoleScreen.BACKDROP);
        // A watcher's header says they only watch, where the driver's says the systems are online
        if (this.state.viewOnly()) {
            graphics.text(this.font, Component.translatableWithFallback("screen.sgcraft.malp_console.view_only",
                    "VIEW ONLY"), l + 6, t + 4, MalpConsoleScreen.VIEW_ONLY);
        } else {
            graphics.text(this.font, Component.translatableWithFallback("screen.sgcraft.malp_console.systems",
                    "MALP SYSTEMS ONLINE"), l + 6, t + 4, MalpConsoleScreen.TEXT);
        }
        // A snapshot just taken, or none to take, is told of in place of the feed's state for a moment
        if (this.notice != null && Util.getMillis() < this.noticeUntil) {
            graphics.text(this.font, this.notice, r - 6 - this.font.width(this.notice), t + 4, this.noticeColor);
            return;
        }
        final Component video = live
                ? Component.translatableWithFallback("screen.sgcraft.malp_console.video", "VIDEO FEED ONLINE")
                : Component.translatableWithFallback("screen.sgcraft.malp_console.video_lost", "VIDEO FEED LOST");
        graphics.text(this.font, video, r - 6 - this.font.width(video), t + 4, live ? MalpConsoleScreen.TEXT
                : MalpConsoleScreen.WARNING);
    }

    private void crosshair(final GuiGraphicsExtractor graphics) {
        final int x = (this.feedLeft + this.feedRight) / 2;
        final int y = (this.feedTop + this.feedBottom) / 2;
        final int c = MalpConsoleScreen.FRAME;
        graphics.horizontalLine(x - 12, x - 4, y, c);
        graphics.horizontalLine(x + 4, x + 12, y, c);
        graphics.verticalLine(x, y - 12, y - 4, c);
        graphics.verticalLine(x, y + 4, y + 12, c);
    }

    // NO SIGNAL, NO POWER, FEED OFFLINE or NO TABLET POWER, large in the middle of the feed
    private void warning(final GuiGraphicsExtractor graphics) {
        final Component text = this.state.malps().isEmpty()
                ? Component.translatableWithFallback("screen.sgcraft.malp_console.no_feeds", "NO FEEDS AVAILABLE")
                : switch (this.state.feed()) {
                    case NO_POWER -> Component.translatableWithFallback("screen.sgcraft.malp_console.no_power",
                            "NO POWER");
                    case NO_TABLET_POWER -> Component.translatableWithFallback("screen.sgcraft.malp_tablet.no_power",
                            "NO TABLET POWER");
                    case OFF -> Component.translatableWithFallback("screen.sgcraft.malp_console.off",
                            "FEED OFFLINE");
                    default -> Component.translatableWithFallback("screen.sgcraft.malp_console.no_signal",
                            "NO SIGNAL");
                };
        final int x = (this.feedLeft + this.feedRight) / 2;
        final int y = (this.feedTop + this.feedBottom) / 2;
        final int width = this.font.width(text);
        graphics.fill(x - width - 8, y - 12, x + width + 8, y + 12, MalpConsoleScreen.BACKDROP);
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y - 7);
        graphics.pose().scale(2, 2);
        graphics.centeredText(this.font, text, 0, 0, MalpConsoleScreen.WARNING);
        graphics.pose().popMatrix();
    }

    // The MALP's readouts along the bottom of the feed: where it is and the gate nearest it, where that is, what is
    // about it, and the gates its signal goes through, if relayed, run on together as far as the feed is wide; and
    // which way it faces, in the feed's top corner
    private void readouts(final GuiGraphicsExtractor graphics) {
        final Readout readout = this.state.readout();
        final int l = this.feedLeft;
        final int r = this.feedRight;
        final int b = this.feedBottom;
        final boolean relayed = MalpSignal.Kind.RELAYED.name().equals(readout.signal());
        final List<Component> lines = new ArrayList<>();
        final BlockPos pos = readout.pos();
        final Component gate = readout.gate().map(near -> Component.translatableWithFallback(
                "screen.sgcraft.malp_console.readout_gate", "GATE %s %sm %s", near.address().isEmpty()
                        ? Component.translatableWithFallback("screen.sgcraft.malp_console.readout_gate_hidden",
                                "HIDDEN")
                        : near.address(), near.distance(), MalpConsoleScreen.compass(near.yaw(), false)))
                .orElseGet(() -> Component.translatableWithFallback("screen.sgcraft.malp_console.readout_no_gate",
                        "NO GATE IN RANGE"));
        lines.add(Component.translatableWithFallback("screen.sgcraft.malp_console.readout_position",
                "POS X %s Y %s Z %s   CHUNK %s, %s   %s", pos.getX(), pos.getY(), pos.getZ(),
                SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()), gate));
        lines.add(Component.translatableWithFallback("screen.sgcraft.malp_console.readout_place",
                "WORLD %s   BIOME %s   TEMP %s C   TIME %s", MalpConsoleScreen.path(readout.world()),
                MalpConsoleScreen.path(readout.biome()), Math.round(readout.temperature() * 25),
                MalpConsoleScreen.clock(readout.time())));
        final Component signal = relayed
                ? Component.translatableWithFallback("screen.sgcraft.malp_console.signal_relay", "RELAYED")
                : Component.translatableWithFallback("screen.sgcraft.malp_console.signal_direct", "DIRECT");
        // A MALP that needs no power says so in place of its battery's charge
        final Component battery = readout.powerRequired()
                ? Component.translatableWithFallback("screen.sgcraft.malp_console.readout_battery", "BATTERY %s%%",
                        readout.battery())
                : Component.translatableWithFallback("screen.sgcraft.malp_console.readout_no_power",
                        "NEEDS NO POWER");
        lines.add(Component.translatableWithFallback("screen.sgcraft.malp_console.readout_status",
                "LIGHT %s   HOSTILES %s/%sm   %s   SIGNAL %s", readout.light(), readout.hostiles(),
                MalpConsoleScreen.HOSTILE_RANGE, battery, signal));
        if (relayed) {
            lines.add(Component.translatableWithFallback("screen.sgcraft.malp_console.readout_route", "ROUTE %s",
                    readout.relay()));
        }
        // Each part goes on the line before it while it fits
        final int width = r - l - 12;
        final int gap = this.font.width(MalpConsoleScreen.READOUT_GAP);
        final List<Integer> rows = new ArrayList<>();
        int used = width;
        for (final Component part : lines) {
            final int partWidth = this.font.width(part);
            if (used + gap + partWidth > width) {
                rows.add(1);
                used = partWidth;
            } else {
                rows.set(rows.size() - 1, rows.getLast() + 1);
                used += gap + partWidth;
            }
        }
        final int line = MalpConsoleScreen.READOUT_LINE;
        final int top = b - 1 - rows.size() * line - 3;
        graphics.fill(l + 1, top, r - 1, b - 1, MalpConsoleScreen.BACKDROP);
        int part = 0;
        for (int row = 0; row < rows.size(); row++) {
            int x = l + 6;
            for (int i = 0; i < rows.get(row); i++, part++) {
                // The status warns of a low battery
                final int color = part == 2 && readout.powerRequired() && readout.battery() <= 10
                        ? MalpConsoleScreen.WARNING : MalpConsoleScreen.TEXT;
                graphics.text(this.font, lines.get(part), x, top + 3 + row * line, color);
                x += this.font.width(lines.get(part)) + gap;
            }
        }
        this.heading(graphics, readout);
    }

    // Which way the MALP faces, boxed in the feed's top corner, under its header
    private void heading(final GuiGraphicsExtractor graphics, final Readout readout) {
        final Component label = Component.translatableWithFallback("screen.sgcraft.malp_console.heading", "HEADING");
        final String facing = MalpConsoleScreen.compass(readout.yaw(), true);
        final int width = Math.max(this.font.width(label), this.font.width(facing)) + 8;
        final int height = 2 * MalpConsoleScreen.READOUT_LINE + 4;
        final int right = this.feedRight - 4;
        final int top = this.feedTop + 18;
        graphics.fill(right - width, top, right, top + height, MalpConsoleScreen.BACKDROP);
        graphics.outline(right - width, top, width, height, MalpConsoleScreen.FRAME_DIM);
        graphics.text(this.font, label, right - width + 4, top + 3, MalpConsoleScreen.FRAME);
        graphics.text(this.font, facing, right - width + 4, top + 3 + MalpConsoleScreen.READOUT_LINE,
                MalpConsoleScreen.TEXT);
    }

    /**
     * @return a way to face, as Minecraft turns an entity, as the compass point it is nearest, and, if asked, its
     *     bearing in degrees from north
     */
    static String compass(final float yaw, final boolean degrees) {
        final int bearing = Math.floorMod(Math.round(yaw) + 180, 360);
        final String point = MalpConsoleScreen.POINTS[Math.floorMod(Math.round(bearing / 45.0F), 8)];
        return degrees ? point + " " + bearing : point;
    }

    // An identifier's path, in capitals, as the show's readouts are
    static String path(final String id) {
        final int colon = id.indexOf(':');
        return (colon < 0 ? id : id.substring(colon + 1)).toUpperCase(Locale.ROOT).replace('_', ' ');
    }

    // Minecraft's day starts at six in the morning
    static String clock(final int ticks) {
        final int hours = (ticks / 1000 + 6) % 24;
        final int minutes = ticks % 1000 * 60 / 1000;
        return String.format(Locale.ROOT, "%02d:%02d", hours, minutes);
    }
}
