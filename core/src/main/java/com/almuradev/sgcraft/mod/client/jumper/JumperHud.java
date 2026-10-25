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

package com.almuradev.sgcraft.mod.client.jumper;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.entity.JumperSeat;
import com.almuradev.sgcraft.mod.entity.PuddleJumperEntity;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * What a puddle jumper's riders see: its console, drawn as the Atlantis consoles are, in the city's blue, with panels
 * cut off at the corners.
 *
 * <p>Two panes stand either side of the screen, angled in like the cockpit's windows. The left one holds the world,
 * the biome and the time of day, the scanner, a round map of the ground round the jumper that fills in as the world
 * arrives (see {@link JumperMap}), the jumper's position, the distance to the gate the computer links to, and how full
 * the cloak generator is, which the shield and the cloak run on, with the seconds till the shield may rise again while
 * it is knocked down. The right one holds the speed and height dials, the height above the ground, the climb, whether the drive is on
 * or off, and the jumper's systems, each lit while it is on, the drive pods' lamp yellow while their arms move them and
 * green once they are out. Between them the
 * hotbar sits on a console strip of its own, its slots cut at the corners too. Where the linked gate is in view, a
 * diamond marks it. The pilot also sees warnings under the top of the screen, while the cargo door is down or while the
 * jumper holds for the world round it to arrive, and a small reticle in first person.</p>
 *
 * <p>In first person the panes are solid, as the jumper's own. In third person they are see-through, so they hide
 * less of the jumper and the world, and a chevron over the jumper's nose shows which way it faces. The hotbar is drawn
 * in place of the game's, under the health and food, and the rest as a layer of the HUD above the boss bars, so all of
 * it hides with the rest of the HUD. Only clients call this.</p>
 */
public final class JumperHud {

    /** The display's id, as a layer of the HUD. */
    public static final Identifier ID = SGCraft.id("jumper_panel");

    // The city's colours: its blue, its pale lettering, the console's dark glass, solid and see-through, unlit
    // segments, and the green and red of lit lamps
    private static final int BLUE = 0xFF3FB8C9;
    private static final int PALE = 0xFFBFF3FA;
    private static final int FAINT = 0x663FB8C9;
    private static final int SELECTED = 0x553FB8C9;
    private static final int CONSOLE = 0xF206202A;
    private static final int CONSOLE_SEE_THROUGH = 0x9906202A;
    private static final int BAR = 0xCC06202A;
    private static final int UNLIT = 0xFF16323C;
    private static final int GREEN = 0xFF5BE38A;
    private static final int GREEN_GLASS = 0xCC0F3A22;
    private static final int RED = 0xFFE8463C;
    private static final int RED_GLASS = 0xCC3A0D0B;
    private static final int YELLOW = 0xFFF0CC3C;
    private static final int YELLOW_GLASS = 0xCC3A300B;
    // The hostiles' boxes, and the locked target's
    private static final int TARGET_FAINT = 0xB0E8463C;
    private static final int TARGET = 0xFFE8463C;
    private static final int LOCKED = 0xFFFF7A6A;
    private static final int FRIEND_FAINT = 0xB04C9BE8;
    private static final int FRIEND = 0xFF4C9BE8;
    private static final int FRIEND_LOCKED = 0xFF8CC4FF;
    private static final int PLAYER_FAINT = 0xB0B05CE8;
    private static final int PLAYER = 0xFFB05CE8;
    private static final int PLAYER_LOCKED = 0xFFD39CFF;
    // The locked target's health under the compass: how wide, how far from the top, and in how many segments
    private static final int HEALTH_WIDTH = 160;
    private static final int HEALTH_TOP = 40;
    private static final int HEALTH_SEGMENTS = 20;
    private static final int LINE = 10;
    // How much larger than the game's GUI the panes are drawn, all but the hotbar's slots, which stay where the game's
    // are, with the health and food over them
    private static final float SCALE = 1.5F;
    // The panes: how wide they are laid flat, how tall at the edge on the screen's side, and how far from that side;
    // each is hinged at that edge, like a door pushed open away from the viewer by so many degrees, and seen in
    // perspective from the screen's middle, from so far away, as a share of half the screen's width
    private static final int PANE_WIDTH = 104;
    private static final int PANE_OUTER = 66;
    private static final int PANE_MARGIN = 4;
    private static final float PANE_SWING = 55;
    private static final float PANE_VIEW = 0.75F;
    // The dials: how many segments, how tall, and what each stands for: meters a second of speed, and blocks above the
    // ground
    private static final int SEGMENTS = 8;
    private static final int SEGMENT_HEIGHT = 5;
    private static final double SEGMENT_SPEED = 5;
    private static final double SEGMENT_GROUND = 4;
    // The system lamps' size
    private static final int LAMP_WIDTH = 44;
    private static final int LAMP_HEIGHT = 11;
    // The cloak generator's charge: how wide each of its segments, and below what share it shows low
    private static final int GENERATOR_SEGMENT = 4;
    private static final float GENERATOR_LOW = 0.3F;
    // How far down the height dial looks for the ground, in blocks
    private static final int GROUND_REACH = 128;
    // How far back the camera is in third person, as CameraMixin puts it, in blocks
    private static final double CAMERA_BACK = 10;
    // The hotbar's size and its slots', as the game's is, so the health and food above it sit as they always do
    private static final int HOTBAR_WIDTH = 182;
    private static final int SLOT = 20;
    // The player's own readings over the hotbar: the strip's height, how wide each reading's name is, how many
    // segments its bar has and how wide each is, and how wide its reading after it
    private static final int VITALS_HEIGHT = 25;
    private static final int VITAL_LABEL = 30;
    private static final int VITAL_SEGMENTS = 10;
    private static final int VITAL_SEGMENT = 4;
    private static final int VITAL_READING = 16;
    // The compass tape along the top: how wide and tall, how far from the top, how many degrees it shows either side
    // of the heading, and how far apart its ticks are, in degrees
    private static final int COMPASS_WIDTH = 200;
    private static final int COMPASS_HEIGHT = 22;
    private static final int COMPASS_TOP = 3;
    private static final float COMPASS_SPAN = 50;
    private static final int COMPASS_TICK = 5;
    // The pilot's key card in the bottom left, at the game's own GUI size: how wide each key's cap and its column,
    // how tall each row, and how far in from the screen's edges
    private static final int KEY_CAP = 40;
    private static final int KEY_COLUMN = 104;
    private static final int KEY_ROW = 11;
    private static final int KEY_MARGIN = 4;

    // The panes' picture, and the shape and glass it was painted for
    private static final HudCanvas PANE = new HudCanvas("sgcraft jumper pane");
    private static long paneLook;

    private JumperHud() {
    }

    // The jumper this client's player rides, if any
    private static @Nullable PuddleJumperEntity ridden(final @Nullable LocalPlayer player) {
        return player != null && !player.isSpectator() && player.getVehicle() instanceof final PuddleJumperEntity
                jumper ? jumper : null;
    }

    /** Draws the top bar, the gate's marker and the pilot's warnings and reticle, if this client's player rides one. */
    public static void extract(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
        final Minecraft minecraft = Minecraft.getInstance();
        final LocalPlayer player = minecraft.player;
        final PuddleJumperEntity jumper = JumperHud.ridden(player);
        if (player == null || jumper == null) {
            return;
        }
        final Font font = minecraft.font;
        final float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        final boolean firstPerson = minecraft.options.getCameraType().isFirstPerson();
        final Optional<BlockPos> gate = jumper.dialer().linked();
        final Projection view = new Projection(minecraft, player, graphics, firstPerson, partialTick);
        graphics.pose().pushMatrix();
        graphics.pose().scale(JumperHud.SCALE, JumperHud.SCALE);
        final int scaledWidth = Math.round(graphics.guiWidth() / JumperHud.SCALE);
        final int scaledHeight = Math.round(graphics.guiHeight() / JumperHud.SCALE);
        final int glass = firstPerson ? JumperHud.CONSOLE : JumperHud.CONSOLE_SEE_THROUGH;
        final int center = scaledHeight / 2;
        final int outer = Math.min(JumperHud.PANE_OUTER, scaledHeight / 2 - 4);
        // How many of the screen's pixels each of the panes' spans
        final float pixels = JumperHud.SCALE * minecraft.getWindow().getGuiScale();
        final Tilt left = new Tilt(graphics, JumperHud.PANE_MARGIN, 1, scaledWidth, center);
        final Tilt right = new Tilt(graphics, scaledWidth - JumperHud.PANE_MARGIN, -1, scaledWidth, center);
        JumperHud.panes(graphics, left, right, center, outer, glass, pixels);
        JumperHud.scanner(font, left, jumper, gate, JumperHud.PANE_MARGIN + 6, center - outer + 10, pixels,
                partialTick);
        JumperHud.instruments(font, right, minecraft, jumper, scaledWidth - JumperHud.PANE_MARGIN
                - JumperHud.PANE_WIDTH + 10, center - outer + 10);
        JumperHud.compass(graphics, font, jumper, scaledWidth / 2, partialTick);
        final boolean operator = jumper.weaponsOperator() == player;
        // The operator's own lock; the other front seat sees it as the operator shares it
        final JumperSeat seat = jumper.seatOf(player);
        final boolean front = seat == JumperSeat.HELM || seat == JumperSeat.DIALER;
        final LivingEntity locked = operator ? JumperTargets.locked(jumper) : front ? JumperHud.shared(jumper) : null;
        if (locked != null) {
            JumperHud.targetHealth(graphics, font, locked, scaledWidth / 2);
        }
        if (jumper.seatOf(player) == JumperSeat.HELM) {
            final long time = minecraft.level == null ? 0 : minecraft.level.getGameTime();
            JumperHud.warnings(graphics, font, jumper, time / 10 % 2 == 0, scaledWidth);
        }
        graphics.pose().popMatrix();
        gate.ifPresent(pos -> JumperHud.gateMarker(graphics, font, jumper, view, pos));
        if (locked != null || operator) {
            JumperHud.targets(graphics, font, jumper, operator ? JumperTargets.marked() : List.of(), locked, view,
                    partialTick);
        }
        if (front) {
            JumperHud.keys(graphics, font, minecraft, seat == JumperSeat.HELM, operator);
        }
        if (seat == JumperSeat.HELM) {
            if (firstPerson) {
                JumperHud.reticle(graphics);
            } else {
                JumperHud.facing(graphics, jumper, view, partialTick);
            }
        }
    }

    // Where a point in the world falls on the screen, as the camera sees it: from the rider's eyes in first person, or
    // from behind them in third
    private static final class Projection {

        private final Vec3 eye;
        private final Vec3 forward;
        private final Vec3 right;
        private final Vec3 up;
        private final double tanY;
        private final double tanX;
        private final int halfWidth;
        private final int halfHeight;

        Projection(final Minecraft minecraft, final LocalPlayer player, final GuiGraphicsExtractor graphics,
                final boolean firstPerson, final float partialTick) {
            this.forward = player.getViewVector(partialTick);
            final Vec3 eyes = player.getEyePosition(partialTick);
            this.eye = firstPerson ? eyes : eyes.subtract(this.forward.scale(JumperHud.CAMERA_BACK));
            final Vec3 across = this.forward.cross(new Vec3(0, 1, 0));
            this.right = across.lengthSqr() < 1.0E-6 ? new Vec3(-1, 0, 0) : across.normalize();
            this.up = this.right.cross(this.forward);
            this.halfWidth = graphics.guiWidth() / 2;
            this.halfHeight = graphics.guiHeight() / 2;
            this.tanY = Math.tan(Math.toRadians(JumperControls.fov(minecraft.options.fov().get(), partialTick)) / 2);
            this.tanX = this.tanY * graphics.guiWidth() / graphics.guiHeight();
        }

        // Which way a point lies from the middle of the view, across and up, as a length of one on the screen
        double[] bearing(final Vec3 point) {
            final Vec3 to = point.subtract(this.eye);
            final double across = to.dot(this.right);
            final double up = to.dot(this.up);
            final double length = Math.max(1.0E-6, Math.hypot(across, up));
            return new double[] {across / length, up / length};
        }

        // The point's place on the screen, or null if it is behind the camera
        int @Nullable [] at(final Vec3 point) {
            final Vec3 to = point.subtract(this.eye);
            final double ahead = to.dot(this.forward);
            if (ahead < 0.1) {
                return null;
            }
            final int x = this.halfWidth + (int) Math.round(to.dot(this.right) / ahead / this.tanX * this.halfWidth);
            final int y = this.halfHeight - (int) Math.round(to.dot(this.up) / ahead / this.tanY * this.halfHeight);
            return new int[] {x, y};
        }
    }

    // Something the HUD draws on: the screen as it is, or a turned pane
    private interface Surface {

        void fill(int x0, int y0, int x1, int y1, int color);

        void text(Font font, String text, int x, int y, int color);

        default void centeredText(final Font font, final String text, final int x, final int y, final int color) {
            this.text(font, text, x - font.width(text) / 2, y, color);
        }

        void blit(HudCanvas canvas, int x0, int y0, int x1, int y1);
    }

    // The screen as it is
    private record Flat(GuiGraphicsExtractor graphics) implements Surface {

        @Override
        public void fill(final int x0, final int y0, final int x1, final int y1, final int color) {
            this.graphics.fill(x0, y0, x1, y1, color);
        }

        @Override
        public void text(final Font font, final String text, final int x, final int y, final int color) {
            this.graphics.text(font, text, x, y, color, false);
        }

        @Override
        public void blit(final HudCanvas canvas, final int x0, final int y0, final int x1, final int y1) {
            canvas.draw(this.graphics, x0, y0, x1, y1, false);
        }
    }

    // A pane's tilt: it is laid out flat, hinged at the edge on the screen's side, then pushed open away from the
    // viewer like a door, and seen in perspective from the middle of the screen. What is drawn on it is drawn as the
    // turned pane shows it, narrower and smaller the further from the hinge: text a letter at a time and pictures in
    // thin strips, each placed and squeezed as the pane is where it lies, and fills as the four sided shapes the pane
    // turns them into, so lines and shapes keystone across the pane as on a real one
    private static final class Tilt implements Surface {

        // How many strips a picture is cut into
        private static final int STRIPS = 24;

        private final GuiGraphicsExtractor graphics;
        private final float hinge;
        private final int inward;
        private final float middleX;
        private final float middleY;
        private final float view;
        private final float cos;
        private final float sin;

        // The pane hinged at so far across the screen, opening inward to the right, or to the left
        Tilt(final GuiGraphicsExtractor graphics, final float hinge, final int inward, final int screenWidth,
                final int middleY) {
            this.graphics = graphics;
            this.hinge = hinge;
            this.inward = inward;
            this.middleX = screenWidth / 2F;
            this.middleY = middleY;
            this.view = this.middleX * JumperHud.PANE_VIEW;
            final float swing = (float) Math.toRadians(JumperHud.PANE_SWING);
            this.cos = Mth.cos(swing);
            this.sin = Mth.sin(swing);
        }

        // How much nearer things look at a point so far along the pane from its hinge
        float nearness(final float along) {
            return this.view / (this.view + along * this.sin);
        }

        // Where on the screen a point on the flat pane falls, across
        float screenX(final float x) {
            final float along = (x - this.hinge) * this.inward;
            return this.middleX + (this.hinge - this.middleX + this.inward * along * this.cos) * this.nearness(along);
        }

        // Draws about a point of the flat pane, placed and squeezed as the turned pane shows it there
        private void at(final float x, final float y, final Runnable draw) {
            final float across = Math.abs(this.screenX(x + 0.5F) - this.screenX(x - 0.5F));
            final float down = this.nearness((x - this.hinge) * this.inward);
            this.graphics.pose().pushMatrix();
            this.graphics.pose().translate(this.screenX(x), this.middleY + (y - this.middleY) * down);
            this.graphics.pose().scale(across, down);
            this.graphics.pose().translate(-x, -y);
            draw.run();
            this.graphics.pose().popMatrix();
        }

        // Where on the screen a point on the flat pane falls, down, at a point so far across it
        private float screenY(final float x, final float y) {
            return this.middleY + (y - this.middleY) * this.nearness((x - this.hinge) * this.inward);
        }

        // A rectangle on the pane is seen as a shape with upright sides and straight top and bottom, as the pane turns
        // only about an upright hinge, so it is drawn whole, its corners where the pane puts them
        @Override
        public void fill(final int x0, final int y0, final int x1, final int y1, final int color) {
            HudQuad.add(this.graphics, this.screenX(x0), this.screenY(x0, y0), this.screenY(x0, y1), this.screenX(x1),
                    this.screenY(x1, y0), this.screenY(x1, y1), color);
        }

        @Override
        public void text(final Font font, final String text, final int x, final int y, final int color) {
            int along = x;
            for (int i = 0; i < text.length(); ) {
                final int code = text.codePointAt(i);
                final String letter = new String(Character.toChars(code));
                final int width = font.width(letter);
                final int left = along;
                this.at(left + width / 2F, y + 4, () -> this.graphics.text(font, letter, left, y, color, false));
                along += width;
                i += Character.charCount(code);
            }
        }

        @Override
        public void blit(final HudCanvas canvas, final int x0, final int y0, final int x1, final int y1) {
            final float width = x1 - x0;
            for (int strip = 0; strip < Tilt.STRIPS; strip++) {
                final float u0 = (float) strip / Tilt.STRIPS;
                final float u1 = (float) (strip + 1) / Tilt.STRIPS;
                final int from = Math.round(x0 + width * u0);
                final int to = Math.round(x0 + width * u1);
                this.at((from + to) / 2F, (y0 + y1) / 2F, () -> canvas.drawPart(this.graphics, from, y0, to, y1,
                        u0, u1));
            }
        }
    }

    private static String compassPoint(final int heading) {
        return switch (Math.floorMod(Math.round(heading / 45F), 8)) {
            case 0 -> "N";
            case 1 -> "NE";
            case 2 -> "E";
            case 3 -> "SE";
            case 4 -> "S";
            case 5 -> "SW";
            case 6 -> "W";
            default -> "NW";
        };
    }

    // The panes, both painted once at the screen's resolution, the right one drawn turned: each as its tilt shows it,
    // its edge on the screen's side tall and its far edge towards the middle shorter, framed in the city's blue with
    // a faint second line inside the frame
    private static void panes(final GuiGraphicsExtractor graphics, final Tilt left, final Tilt right,
            final int center, final int outer, final int glass, final float pixels) {
        // As the turned pane shows on the screen: from its hinge to its far edge, which looks the shorter
        final float far = left.screenX(JumperHud.PANE_MARGIN + JumperHud.PANE_WIDTH);
        final float shown = far - JumperHud.PANE_MARGIN;
        final float inner = outer * left.nearness(JumperHud.PANE_WIDTH);
        final int width = Math.round(shown * pixels);
        final int height = Math.round(outer * 2 * pixels);
        final long look = (long) glass << 32 | (long) width << 16 | outer;
        if (!JumperHud.PANE.sized(width, height) || JumperHud.paneLook != look) {
            JumperHud.paintPane(JumperHud.PANE.pixels(width, height), width, height, outer * pixels,
                    inner * pixels, glass, pixels);
            JumperHud.PANE.upload();
            JumperHud.paneLook = look;
        }
        final int span = Math.round(shown);
        JumperHud.PANE.draw(graphics, JumperHud.PANE_MARGIN, center - outer, JumperHud.PANE_MARGIN + span,
                center + outer, false);
        final int rightHinge = Math.round(right.hinge);
        JumperHud.PANE.draw(graphics, rightHinge - span, center - outer, rightHinge, center + outer, true);
    }

    private static void paintPane(final NativeImage image, final int width, final int height, final float outer,
            final float inner, final int glass, final float pixels) {
        final float line = Math.max(1, pixels * 0.6F);
        final float inset = line + Math.max(2, pixels * 2);
        final float slope = (outer - inner) / width;
        final float across = Mth.sqrt(1 + slope * slope);
        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                final float x = px + 0.5F;
                final float half = outer - slope * x;
                // How far in from the slanted top or bottom, and from the sides
                final float edge = (half - Math.abs(py + 0.5F - height / 2F)) / across;
                final float side = Math.min(x, width - x);
                final float in = Math.min(edge, side);
                int color = glass;
                color = HudCanvas.over(color, JumperHud.BLUE, line + 0.5F - in);
                color = HudCanvas.over(color, JumperHud.FAINT, line * 0.75F + 0.5F - Math.abs(in - inset));
                image.setPixel(px, py, HudCanvas.fade(color, edge + 0.5F));
            }
        }
    }

    // What the left pane shows: the world and the time, the biome, the scanner, the position and the gate
    private static void scanner(final Font font, final Surface pane, final PuddleJumperEntity jumper,
            final Optional<BlockPos> gate, final int left, final int top, final float pixels,
            final float partialTick) {
        final Level level = jumper.level();
        final BlockPos at = jumper.blockPosition();
        final int width = JumperHud.PANE_WIDTH - 14;
        final String time = JumperHud.timeOfDay(level);
        pane.text(font, time, left + width - font.width(time), top, JumperHud.BLUE);
        pane.text(font, font.plainSubstrByWidth(JumperHud.worldName(level), width - font.width(time) - 4), left, top,
                JumperHud.PALE);
        pane.text(font, font.plainSubstrByWidth(JumperHud.biomeName(level.getBiome(at)), width), left,
                top + JumperHud.LINE, JumperHud.BLUE);
        final int mapX = left + width / 2;
        final int mapY = top + 22 + JumperMap.RADIUS;
        pane.blit(JumperMap.update(pixels, level, jumper.getPosition(partialTick), jumper.getYRot(partialTick), gate,
                partialTick), mapX - JumperMap.RADIUS, mapY - JumperMap.RADIUS, mapX + JumperMap.RADIUS,
                mapY + JumperMap.RADIUS);
        final int below = top + 22 + JumperMap.RADIUS * 2 + 5;
        pane.text(font, at.getX() + " " + at.getY(), left, below, JumperHud.PALE);
        pane.text(font, Integer.toString(at.getZ()), left, below + JumperHud.LINE, JumperHud.PALE);
        final String gateText = gate.map(pos -> Component.translatableWithFallback("hud.sgcraft.jumper.gate",
                "GATE %s", JumperHud.distance(Math.sqrt(pos.distToCenterSqr(jumper.position())))).getString())
                .orElse(Component.translatableWithFallback("hud.sgcraft.jumper.no_gate", "NO GATE").getString());
        pane.text(font, font.plainSubstrByWidth(gateText, width), left, below + JumperHud.LINE * 2,
                gate.isPresent() ? JumperHud.GREEN : JumperHud.FAINT);
        JumperHud.generator(font, pane, jumper, left, below + JumperHud.LINE * 3 + 3, width);
    }

    // The cloak generator's charge, under the scanner: its name, a row of segments lit as far as it is full, blue, or
    // yellow once low, and how full it is; while the shield is knocked down, the segments are red and the seconds
    // till it may rise again blink in place of how full
    private static void generator(final Font font, final Surface pane, final PuddleJumperEntity jumper,
            final int left, final int top, final int width) {
        final long down = jumper.shieldDownTicks();
        final boolean blink = jumper.level().getGameTime() / 10 % 2 == 0;
        final float charge = jumper.generator();
        pane.text(font, Component.translatableWithFallback("hud.sgcraft.jumper.generator", "GEN").getString(), left,
                top, JumperHud.BLUE);
        final int barLeft = left + 22;
        final int barRight = left + width - 26;
        final int segments = (barRight - barLeft) / JumperHud.GENERATOR_SEGMENT;
        final int lit = (int) Math.ceil(charge * segments - 0.2);
        final int color = down > 0 ? JumperHud.RED : charge < JumperHud.GENERATOR_LOW ? JumperHud.YELLOW
                : JumperHud.BLUE;
        for (int segment = 0; segment < segments; segment++) {
            final int x = barLeft + segment * JumperHud.GENERATOR_SEGMENT;
            pane.fill(x, top + 1, x + JumperHud.GENERATOR_SEGMENT - 1, top + 7, segment < lit ? color
                    : JumperHud.UNLIT);
        }
        final String reading = down > 0
                ? Component.translatableWithFallback("hud.sgcraft.jumper.generator_down", "%sS",
                        (down + 19) / 20).getString()
                : Math.round(charge * 100) + "%";
        if (down <= 0 || blink) {
            pane.text(font, reading, left + width - font.width(reading), top, down > 0 ? JumperHud.RED : color);
        }
    }

    // What the right pane shows: the speed and height dials, the readings beside them, the power and the systems' lamps
    private static void instruments(final Font font, final Surface pane, final Minecraft minecraft,
            final PuddleJumperEntity jumper, final int left, final int top) {
        final double speed = Math.hypot(jumper.getX() - jumper.xo, jumper.getZ() - jumper.zo) * 20;
        final double climb = (jumper.getY() - jumper.yo) * 20;
        final int ground = JumperHud.aboveGround(jumper);
        final int barsTop = top + 12;
        final int barsHeight = JumperHud.SEGMENTS * JumperHud.SEGMENT_HEIGHT;
        JumperHud.bar(pane, left, barsTop, speed / JumperHud.SEGMENT_SPEED);
        JumperHud.bar(pane, left + 16, barsTop, ground < 0 ? JumperHud.SEGMENTS : ground / JumperHud.SEGMENT_GROUND);
        pane.centeredText(font, Integer.toString((int) Math.round(speed)), left + 5, top, JumperHud.PALE);
        pane.centeredText(font, Integer.toString(Mth.floor(jumper.getY())), left + 21, top, JumperHud.PALE);
        pane.centeredText(font, Component.translatableWithFallback("hud.sgcraft.jumper.speed", "M/S").getString(),
                left + 5, barsTop + barsHeight + 2, JumperHud.BLUE);
        pane.centeredText(font, "Y", left + 21, barsTop + barsHeight + 2, JumperHud.BLUE);
        final int text = left + 34;
        pane.text(font, Component.translatableWithFallback("hud.sgcraft.jumper.ground", "AGL %s",
                ground < 0 ? "> " + JumperHud.GROUND_REACH : Integer.toString(ground)).getString(), text, barsTop,
                JumperHud.BLUE);
        pane.text(font, String.format(Locale.ROOT, "%+.1f", climb), text, barsTop + JumperHud.LINE + 2,
                JumperHud.BLUE);
        // Whether its drive is on: off, the jumper doesn't fly, and the pane says so
        pane.text(font, (jumper.powered()
                ? Component.translatableWithFallback("hud.sgcraft.jumper.drive_on", "DRIVE ON")
                : Component.translatableWithFallback("hud.sgcraft.jumper.drive_off", "DRIVE OFF")).getString(), text,
                barsTop + (JumperHud.LINE + 2) * 2 + 2, jumper.powered() ? JumperHud.GREEN : JumperHud.RED);
        // How many drones it carries; in creative, as many as the pilot likes
        final boolean endless = minecraft.player != null && minecraft.player.isCreative();
        pane.text(font, Component.translatableWithFallback("hud.sgcraft.jumper.drones", "DRONES %s",
                endless ? "\u221E" : Integer.toString(jumper.drones())).getString(), text,
                barsTop + (JumperHud.LINE + 2) * 3 + 2, endless || jumper.drones() > 0 ? JumperHud.YELLOW
                        : JumperHud.RED);
        // The systems
        final boolean turbo = jumper.pods(1) >= 1 && minecraft.options.keySprint.isDown()
                && minecraft.options.keyUp.isDown();
        final int upper = barsTop + barsHeight + 14;
        final int lower = upper + JumperHud.LAMP_HEIGHT + 2;
        final int right = left + JumperHud.LAMP_WIDTH + 2;
        // The pods' lamp is yellow while their arms move them out or in, and green once they are out
        final float pods = jumper.pods(1);
        final int podsLamp = jumper.podsOut() ? pods >= 1 ? JumperHud.GREEN : JumperHud.YELLOW
                : pods > 0 ? JumperHud.YELLOW : 0;
        JumperHud.lamp(pane, font, left, upper, Component.translatableWithFallback("hud.sgcraft.jumper.pods",
                "PODS"), podsLamp);
        JumperHud.lamp(pane, font, right, upper, Component.translatableWithFallback("hud.sgcraft.jumper.turbo",
                "TURBO"), turbo ? JumperHud.GREEN : 0);
        JumperHud.lamp(pane, font, left, lower, Component.translatableWithFallback("hud.sgcraft.jumper.headlights",
                "LIGHTS"), jumper.headlights() ? JumperHud.GREEN : 0);
        JumperHud.lamp(pane, font, right, lower, Component.translatableWithFallback("hud.sgcraft.jumper.door",
                "DOOR"), jumper.hatchOpen() ? JumperHud.RED : 0);
        // The drone bays' lamp is yellow while they swing out or in, and red once they are out, its drones armed
        final float bays = jumper.bays(1);
        final int armedLamp = jumper.armed() ? bays >= 1 ? JumperHud.RED : JumperHud.YELLOW
                : bays > 0 ? JumperHud.YELLOW : 0;
        JumperHud.lamp(pane, font, left, lower + JumperHud.LAMP_HEIGHT + 2,
                Component.translatableWithFallback("hud.sgcraft.jumper.armed", "ARMED"), armedLamp);
        JumperHud.lamp(pane, font, right, lower + JumperHud.LAMP_HEIGHT + 2,
                Component.translatableWithFallback("hud.sgcraft.jumper.friendly_fire", "FRIEND"),
                jumper.friendlyFire() ? JumperHud.YELLOW : 0);
        // The cloak generator's: the shield's lamp green while it is up and red while it is knocked down, and the
        // cloak's yellow while it comes on or goes off and green once it is on
        final int generatorRow = lower + (JumperHud.LAMP_HEIGHT + 2) * 2;
        JumperHud.lamp(pane, font, left, generatorRow, Component.translatableWithFallback("hud.sgcraft.jumper.shield",
                "SHIELD"), jumper.shieldUp() ? JumperHud.GREEN : jumper.shieldDownTicks() > 0 ? JumperHud.RED : 0);
        final float cloak = jumper.cloak(1);
        final int cloakLamp = jumper.cloaked() ? cloak >= 1 ? JumperHud.GREEN : JumperHud.YELLOW
                : cloak > 0 ? JumperHud.YELLOW : 0;
        JumperHud.lamp(pane, font, right, generatorRow, Component.translatableWithFallback("hud.sgcraft.jumper.cloak",
                "CLOAK"), cloakLamp);
    }

    // A vertical bar of segments, lit from the bottom as far as a reading, in segments
    private static void bar(final Surface pane, final int left, final int top, final double reading) {
        final int lit = (int) Math.ceil(Math.min(JumperHud.SEGMENTS, reading) - 0.2);
        for (int segment = 0; segment < JumperHud.SEGMENTS; segment++) {
            final int y = top + (JumperHud.SEGMENTS - 1 - segment) * JumperHud.SEGMENT_HEIGHT;
            pane.fill(left, y + 1, left + 10, y + JumperHud.SEGMENT_HEIGHT - 1, segment < lit ? JumperHud.BLUE
                    : JumperHud.UNLIT);
        }
    }

    // The world's name, as the game has it, or else made from its id
    private static String worldName(final Level level) {
        final Identifier id = level.dimension().identifier();
        return Component.translatableWithFallback(Util.makeDescriptionId("dimension", id),
                id.getPath().replace('_', ' ')).getString().toUpperCase(Locale.ROOT);
    }

    // The creatures marked round the jumper, each in a box about it as it shows on the screen, red for a hostile and
    // blue for anything else, marked with friendly fire on; the locked target in a brighter one with brackets at its
    // corners, its distance under it, or, out of view, a marker at the edge of the screen the way it lies
    private static void targets(final GuiGraphicsExtractor graphics, final Font font, final PuddleJumperEntity jumper,
            final List<LivingEntity> marked, final @Nullable LivingEntity locked, final Projection view,
            final float partialTick) {
        boolean lockedShown = false;
        // The marked, and the locked target too though it is chosen and out of the cone
        final List<LivingEntity> shown = new ArrayList<>(marked);
        if (locked != null && !shown.contains(locked)) {
            shown.add(locked);
        }
        for (final LivingEntity hostile : shown) {
            final int[] colors = JumperHud.markColors(hostile);
            final int[] box = JumperHud.screenBox(hostile, view, partialTick);
            if (box == null || box[2] < 0 || box[0] > graphics.guiWidth() || box[3] < 0
                    || box[1] > graphics.guiHeight()) {
                continue;
            }
            if (hostile == locked) {
                lockedShown = true;
                JumperHud.outline(graphics, box[0] - 2, box[1] - 2, box[2] + 2, box[3] + 2,
                        colors[1]);
                JumperHud.brackets(graphics, box[0] - 4, box[1] - 4, box[2] + 4, box[3] + 4,
                        colors[2]);
                final String lock = Component.translatableWithFallback("hud.sgcraft.jumper.lock", "LOCK %s",
                        JumperHud.distance(Math.sqrt(hostile.distanceToSqr(jumper)))).getString();
                graphics.centeredText(font, lock, (box[0] + box[2]) / 2, box[3] + 6,
                        colors[2]);
                graphics.centeredText(font, hostile.getDisplayName(), (box[0] + box[2]) / 2, box[1] - 14,
                        colors[2]);
            } else {
                JumperHud.outline(graphics, box[0], box[1], box[2], box[3],
                        colors[0]);
            }
        }
        if (locked != null && !lockedShown) {
            // Off the screen, or behind: a marker at its edge, the way to turn
            final double[] way = view.bearing(locked.getBoundingBox().getCenter());
            final int x = graphics.guiWidth() / 2 + (int) Math.round(way[0] * (graphics.guiWidth() / 2 - 24));
            final int y = graphics.guiHeight() / 2 - (int) Math.round(way[1] * (graphics.guiHeight() / 2 - 24));
            JumperHud.diamond(graphics, x, y, 7, JumperHud.LOCKED);
            JumperHud.diamond(graphics, x, y, 4, JumperHud.TARGET);
            graphics.centeredText(font, JumperHud.distance(Math.sqrt(locked.distanceToSqr(jumper))), x, y + 10,
                    JumperHud.LOCKED);
        }
    }

    // The locked target's name and health under the compass, as a bar of segments lit as far as it has health left,
    // red for a hostile and blue for anything else, its health read out beside its name
    private static void targetHealth(final GuiGraphicsExtractor graphics, final Font font, final LivingEntity target,
            final int middle) {
        final Flat screen = new Flat(graphics);
        final int lit = JumperHud.markColors(target)[1];
        final int left = middle - JumperHud.HEALTH_WIDTH / 2;
        final int top = JumperHud.HEALTH_TOP;
        JumperHud.cutPanel(screen, left, top, JumperHud.HEALTH_WIDTH, 24, 4, JumperHud.BAR, lit);
        final String health = Math.round(target.getHealth()) + "/" + Math.round(target.getMaxHealth());
        final int room = JumperHud.HEALTH_WIDTH - 14 - font.width(health);
        screen.text(font, font.plainSubstrByWidth(target.getDisplayName().getString().toUpperCase(Locale.ROOT), room),
                left + 6, top + 4, JumperHud.PALE);
        screen.text(font, health, left + JumperHud.HEALTH_WIDTH - 6 - font.width(health), top + 4, lit);
        final float share = Mth.clamp(target.getHealth() / Math.max(1, target.getMaxHealth()), 0, 1);
        final int segments = JumperHud.HEALTH_SEGMENTS;
        final int span = (JumperHud.HEALTH_WIDTH - 12) / segments;
        final int shown = (int) Math.ceil(share * segments - 0.01F);
        for (int i = 0; i < segments; i++) {
            final int x = left + 6 + i * span;
            screen.fill(x, top + 15, x + span - 1, top + 20, i < shown ? lit : JumperHud.UNLIT);
        }
    }

    // A marked creature's colours, faint, lit and locked: red for a hostile, purple for a player, and blue for
    // anything else, marked with friendly fire on
    private static int[] markColors(final Entity entity) {
        if (JumperTargets.hostile(entity)) {
            return new int[] {JumperHud.TARGET_FAINT, JumperHud.TARGET, JumperHud.LOCKED};
        }
        if (entity instanceof Player) {
            return new int[] {JumperHud.PLAYER_FAINT, JumperHud.PLAYER, JumperHud.PLAYER_LOCKED};
        }
        return new int[] {JumperHud.FRIEND_FAINT, JumperHud.FRIEND, JumperHud.FRIEND_LOCKED};
    }

    // The target the weapons' operator shares, if it is still there
    private static @Nullable LivingEntity shared(final PuddleJumperEntity jumper) {
        return jumper.target() >= 0 && jumper.level().getEntity(jumper.target()) instanceof final LivingEntity living
                && living.isAlive() ? living : null;
    }

    // The screen's box round a creature's hitbox, where it is between the last tick and this one, as left, top,
    // right and bottom; or null if any of it is behind the camera
    private static int @Nullable [] screenBox(final Entity entity, final Projection view, final float partialTick) {
        final AABB box = entity.getBoundingBox().move(entity.getPosition(partialTick).subtract(entity.position()));
        int left = Integer.MAX_VALUE;
        int top = Integer.MAX_VALUE;
        int right = Integer.MIN_VALUE;
        int bottom = Integer.MIN_VALUE;
        for (int corner = 0; corner < 8; corner++) {
            final int[] at = view.at(new Vec3((corner & 1) == 0 ? box.minX : box.maxX,
                    (corner & 2) == 0 ? box.minY : box.maxY, (corner & 4) == 0 ? box.minZ : box.maxZ));
            if (at == null) {
                return null;
            }
            left = Math.min(left, at[0]);
            top = Math.min(top, at[1]);
            right = Math.max(right, at[0]);
            bottom = Math.max(bottom, at[1]);
        }
        return new int[] {left, top, right, bottom};
    }

    // A rectangle's outline, a pixel wide
    private static void outline(final GuiGraphicsExtractor graphics, final int x0, final int y0, final int x1,
            final int y1, final int color) {
        graphics.fill(x0, y0, x1, y0 + 1, color);
        graphics.fill(x0, y1 - 1, x1, y1, color);
        graphics.fill(x0, y0 + 1, x0 + 1, y1 - 1, color);
        graphics.fill(x1 - 1, y0 + 1, x1, y1 - 1, color);
    }

    // Brackets at a rectangle's corners, two pixels thick
    private static void brackets(final GuiGraphicsExtractor graphics, final int x0, final int y0, final int x1,
            final int y1, final int color) {
        final int arm = Math.max(4, Math.min(10, Math.min(x1 - x0, y1 - y0) / 4));
        for (final int[] corner : new int[][] {{x0, y0, 1, 1}, {x1, y0, -1, 1}, {x0, y1, 1, -1}, {x1, y1, -1, -1}}) {
            final int x = corner[0];
            final int y = corner[1];
            final int dx = corner[2];
            final int dy = corner[3];
            graphics.fill(Math.min(x, x + dx * arm), Math.min(y, y + dy * 2), Math.max(x, x + dx * arm),
                    Math.max(y, y + dy * 2), color);
            graphics.fill(Math.min(x, x + dx * 2), Math.min(y, y + dy * arm), Math.max(x, x + dx * 2),
                    Math.max(y, y + dy * arm), color);
        }
    }

    // A diamond on the gate the computer links to, where it is in view
    private static void gateMarker(final GuiGraphicsExtractor graphics, final Font font,
            final PuddleJumperEntity jumper, final Projection view, final BlockPos gate) {
        final int[] at = view.at(Vec3.atCenterOf(gate).add(0, 2, 0));
        if (at == null || at[0] < 0 || at[0] > graphics.guiWidth() || at[1] < 0 || at[1] > graphics.guiHeight()) {
            return;
        }
        JumperHud.diamond(graphics, at[0], at[1], 8, JumperHud.BLUE);
        JumperHud.diamond(graphics, at[0], at[1], 6, JumperHud.FAINT);
        graphics.text(font, JumperHud.distance(Math.sqrt(gate.distToCenterSqr(jumper.position()))), at[0] + 12,
                at[1] - 4, JumperHud.PALE, true);
    }

    // A diamond's outline about a point, some pixels from its middle to each corner
    private static void diamond(final GuiGraphicsExtractor graphics, final int x, final int y, final int size,
            final int color) {
        for (int row = -size; row <= size; row++) {
            final int half = size - Math.abs(row);
            graphics.fill(x - half, y + row, x - half + 1, y + row + 1, color);
            graphics.fill(x + half, y + row, x + half + 1, y + row + 1, color);
        }
    }

    // Warnings under the top bar: blinking while the cargo door is down, and steady while the jumper holds for the
    // world round it to arrive
    private static void warnings(final GuiGraphicsExtractor graphics, final Font font,
            final PuddleJumperEntity jumper, final boolean blink, final int screenWidth) {
        final int top = JumperHud.HEALTH_TOP + 28;
        // Only while it could fly: with its drive off it won't anyway
        if (jumper.hatchOpen() && jumper.powered() && blink) {
            graphics.centeredText(font, Component.translatableWithFallback("hud.sgcraft.jumper.door_warning",
                    "DOOR DOWN, RAISE TO FLY"), screenWidth / 2, top, JumperHud.RED);
        }
        if (jumper.awaitingTerrain()) {
            graphics.centeredText(font, Component.translatableWithFallback("hud.sgcraft.jumper.awaiting_terrain",
                    "HOLDING FOR TERRAIN"), screenWidth / 2, top + JumperHud.LINE + 2, JumperHud.PALE);
        }
    }

    // The compass tape along the top of the screen: the heading's ticks scrolling past under a fixed caret, the
    // cardinal points lit, the half points and every fifteen degrees marked, and the heading read out beneath
    private static void compass(final GuiGraphicsExtractor graphics, final Font font,
            final PuddleJumperEntity jumper, final int middle, final float partialTick) {
        final Flat screen = new Flat(graphics);
        final int left = middle - JumperHud.COMPASS_WIDTH / 2;
        final int top = JumperHud.COMPASS_TOP;
        final int bottom = top + JumperHud.COMPASS_HEIGHT;
        JumperHud.cutPanel(screen, left, top, JumperHud.COMPASS_WIDTH, JumperHud.COMPASS_HEIGHT, 5, JumperHud.BAR,
                JumperHud.BLUE);
        // Minecraft's yaw is 0 facing south and grows clockwise from above; a compass's heading is 0 facing north
        final float heading = Mth.positiveModulo(jumper.getYRot(partialTick) + 180, 360);
        final float perDegree = (JumperHud.COMPASS_WIDTH / 2F - 10) / JumperHud.COMPASS_SPAN;
        final int inner = JumperHud.COMPASS_WIDTH / 2 - 8;
        final int first = Mth.floor((heading - JumperHud.COMPASS_SPAN) / JumperHud.COMPASS_TICK)
                * JumperHud.COMPASS_TICK;
        for (int degree = first; degree <= heading + JumperHud.COMPASS_SPAN; degree += JumperHud.COMPASS_TICK) {
            final int x = Math.round(middle + (degree - heading) * perDegree);
            if (Math.abs(x - middle) > inner) {
                continue;
            }
            final int mark = Math.floorMod(degree, 360);
            final boolean cardinal = mark % 90 == 0;
            final boolean half = mark % 45 == 0;
            final boolean labelled = half || mark % 15 == 0;
            final int length = cardinal ? 7 : half ? 6 : labelled ? 4 : 2;
            screen.fill(x, bottom - 3 - length, x + 1, bottom - 3, cardinal ? JumperHud.PALE : labelled
                    ? JumperHud.BLUE : JumperHud.FAINT);
            if (labelled) {
                final String label = half ? JumperHud.compassPoint(mark) : Integer.toString(mark);
                final int width = font.width(label);
                if (Math.abs(x - middle) + width / 2 <= inner) {
                    screen.text(font, label, x - width / 2 + 1, top + 3, cardinal ? JumperHud.PALE : half
                            ? JumperHud.BLUE : JumperHud.FAINT);
                }
            }
        }
        // The caret over the heading, pointing down into the tape, and the heading read out under it
        for (int row = 0; row < 4; row++) {
            screen.fill(middle - 3 + row, top + 1 + row, middle + 4 - row, top + 2 + row, JumperHud.GREEN);
        }
        screen.fill(middle, bottom - 10, middle + 1, bottom - 2, JumperHud.GREEN);
        final int degrees = Math.round(heading) % 360;
        final String readout = String.format(Locale.ROOT, "%03d\u00B0 %s", degrees, JumperHud.compassPoint(degrees));
        final int boxWidth = font.width(readout) + 10;
        JumperHud.cutPanel(screen, middle - boxWidth / 2, bottom - 1, boxWidth, 13, 3, JumperHud.CONSOLE,
                JumperHud.GREEN);
        screen.centeredText(font, readout, middle + 1, bottom + 2, JumperHud.GREEN);
    }

    // The keys of the front seats, as their riders have them bound, on a card in the bottom left: each key on a cap
    // cut at its corners, and what it does beside it, in two columns. The pilot's are the flying keys; the weapons'
    // are whoever works them, the co-pilot if anyone sits there; both have the computer's and standing up
    private static void keys(final GuiGraphicsExtractor graphics, final Font font, final Minecraft minecraft,
            final boolean pilot, final boolean operator) {
        final Options options = minecraft.options;
        final List<String[]> keys = new ArrayList<>();
        if (pilot) {
            keys.addAll(JumperHud.flyingKeys(options));
        }
        keys.add(new String[] {JumperHud.keyName(JumperKeys.COMPUTER), JumperHud.keyLabel("computer", "COMPUTER")});
        if (operator) {
            keys.add(new String[] {JumperHud.keyName(JumperKeys.ARM), JumperHud.keyLabel("arm", "ARM")});
            keys.add(new String[] {JumperHud.keyName(JumperKeys.TARGET), JumperHud.keyLabel("target", "TARGET")});
            keys.add(new String[] {JumperHud.keyName(JumperKeys.FIRE), JumperHud.keyLabel("fire", "FIRE")});
            keys.add(new String[] {JumperHud.keyName(JumperKeys.FRIENDLY_FIRE),
                JumperHud.keyLabel("friendly_fire", "FRIENDLY")});
        }
        keys.add(new String[] {JumperHud.keyName(options.keyShift), JumperHud.keyLabel("stand", "STAND UP")});
        final Flat screen = new Flat(graphics);
        final int rows = (keys.size() + 1) / 2;
        final int width = JumperHud.KEY_COLUMN * 2 + 8;
        final int height = rows * JumperHud.KEY_ROW + 8;
        final int left = JumperHud.KEY_MARGIN;
        final int top = graphics.guiHeight() - JumperHud.KEY_MARGIN - height;
        JumperHud.cutPanel(screen, left, top, width, height, 4, JumperHud.BAR, JumperHud.BLUE);
        for (int i = 0; i < keys.size(); i++) {
            final int x = left + 4 + (i / rows) * JumperHud.KEY_COLUMN;
            final int y = top + 4 + (i % rows) * JumperHud.KEY_ROW;
            JumperHud.cutPanel(screen, x, y, JumperHud.KEY_CAP, JumperHud.KEY_ROW - 1, 2, JumperHud.CONSOLE,
                    JumperHud.FAINT);
            screen.centeredText(font, font.plainSubstrByWidth(keys.get(i)[0], JumperHud.KEY_CAP - 4),
                    x + JumperHud.KEY_CAP / 2, y + 1, JumperHud.PALE);
            screen.text(font, font.plainSubstrByWidth(keys.get(i)[1],
                    JumperHud.KEY_COLUMN - JumperHud.KEY_CAP - 6), x + JumperHud.KEY_CAP + 4, y + 1, JumperHud.BLUE);
        }
    }

    // The pilot's flying keys
    private static List<String[]> flyingKeys(final Options options) {
        return List.of(
            new String[] {JumperHud.keyName(options.keyUp) + "/" + JumperHud.keyName(options.keyDown),
                JumperHud.keyLabel("thrust", "THRUST")},
            new String[] {JumperHud.keyName(options.keyLeft) + "/" + JumperHud.keyName(options.keyRight),
                JumperHud.keyLabel("turn", "TURN")},
            new String[] {JumperHud.keyName(options.keyJump), JumperHud.keyLabel("climb", "CLIMB")},
            new String[] {JumperHud.keyName(JumperKeys.DESCEND), JumperHud.keyLabel("descend", "DESCEND")},
            new String[] {JumperHud.keyName(options.keySprint), JumperHud.keyLabel("turbo", "TURBO")},
            new String[] {JumperHud.keyName(JumperKeys.POWER), JumperHud.keyLabel("drive", "DRIVE")},
            new String[] {JumperHud.keyName(JumperKeys.HEADLIGHTS), JumperHud.keyLabel("lights", "LIGHTS")},
            new String[] {JumperHud.keyName(JumperKeys.PODS), JumperHud.keyLabel("pods", "PODS")},
            new String[] {JumperHud.keyName(JumperKeys.DOOR), JumperHud.keyLabel("door", "DOOR")},
            new String[] {JumperHud.keyName(JumperKeys.SHIELD), JumperHud.keyLabel("shield", "SHIELD")},
            new String[] {JumperHud.keyName(JumperKeys.CLOAK), JumperHud.keyLabel("cloak", "CLOAK")});
    }

    // A key as the player has it bound, short and in capitals
    private static String keyName(final KeyMapping key) {
        return key.getTranslatedKeyMessage().getString().toUpperCase(Locale.ROOT).replace("LEFT ", "L")
                .replace("RIGHT ", "R").replace("CONTROL", "CTRL");
    }

    private static String keyLabel(final String name, final String fallback) {
        return Component.translatableWithFallback("hud.sgcraft.jumper.key." + name, fallback).getString();
    }

    // A small reticle round the crosshair: a ring with wings to either side
    private static void reticle(final GuiGraphicsExtractor graphics) {
        final int x = graphics.guiWidth() / 2;
        final int y = graphics.guiHeight() / 2;
        JumperHud.ring(graphics, x, y, 5, JumperHud.FAINT);
        graphics.fill(x - 16, y, x - 7, y + 1, JumperHud.FAINT);
        graphics.fill(x + 8, y, x + 17, y + 1, JumperHud.FAINT);
    }

    private static void ring(final GuiGraphicsExtractor graphics, final int x, final int y, final int radius,
            final int color) {
        final int steps = radius * 6;
        for (int i = 0; i < steps; i++) {
            final double angle = i * Math.PI * 2 / steps;
            final int px = x + (int) Math.round(Math.cos(angle) * radius);
            final int py = y + (int) Math.round(Math.sin(angle) * radius);
            graphics.fill(px, py, px + 1, py + 1, color);
        }
    }

    // In third person, a chevron over the jumper's nose, pointing the way it faces
    private static void facing(final GuiGraphicsExtractor graphics, final PuddleJumperEntity jumper,
            final Projection view, final float partialTick) {
        final Vec3 ahead = Vec3.directionFromRotation(0, jumper.getYRot(partialTick));
        final int[] at = view.at(jumper.getPosition(partialTick).add(ahead.scale(PuddleJumperEntity.LENGTH))
                .add(0, PuddleJumperEntity.HEIGHT + 0.5, 0));
        if (at != null) {
            for (int i = 0; i < 9; i++) {
                graphics.fill(at[0] - 9 + i, at[1] - i / 2, at[0] - 8 + i, at[1] - i / 2 + 2, JumperHud.BLUE);
                graphics.fill(at[0] + 9 - i, at[1] - i / 2, at[0] + 10 - i, at[1] - i / 2 + 2, JumperHud.BLUE);
            }
        }
    }

    /**
     * Draws the hotbar on a console strip, in place of the game's hotbar, if this client's player rides a jumper;
     * the slots sit where the game's do, and over them, on a strip of their own, the player's health, food, armor and
     * experience, in place of the game's bars, while the player can be hurt.
     *
     * @return whether it drew it, so the game's hotbar needn't be
     */
    public static boolean extractHotbar(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
        final Minecraft minecraft = Minecraft.getInstance();
        final LocalPlayer player = minecraft.player;
        final PuddleJumperEntity jumper = JumperHud.ridden(player);
        if (player == null || jumper == null) {
            return false;
        }
        // The hotbar, on a console strip of its own, at the game's own size
        final int left = graphics.guiWidth() / 2 - JumperHud.HOTBAR_WIDTH / 2;
        final int top = graphics.guiHeight() - 22;
        JumperHud.cutPanel(new Flat(graphics), left - 3, top - 2, JumperHud.HOTBAR_WIDTH + 6, 24, 6, JumperHud.BAR,
                JumperHud.BLUE);
        final int selected = player.getInventory().getSelectedSlot();
        int seed = 1;
        for (int slot = 0; slot < 9; slot++) {
            final int x = left + 1 + slot * JumperHud.SLOT;
            JumperHud.slot(graphics, x, top + 1, slot == selected);
            JumperHud.item(graphics, minecraft, player, player.getInventory().getItem(slot), x + 2, top + 3, seed++);
        }
        final ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty()) {
            final int x = player.getMainArm().getOpposite() == HumanoidArm.LEFT ? left - 26 : left
                    + JumperHud.HOTBAR_WIDTH + 6;
            JumperHud.slot(graphics, x, top + 1, false);
            JumperHud.item(graphics, minecraft, player, offhand, x + 2, top + 3, seed);
        }
        if (JumperHud.vitalsShown()) {
            JumperHud.vitals(graphics, minecraft.font, player, left - 3, top - 2 - JumperHud.VITALS_HEIGHT - 1);
        }
        return true;
    }

    /**
     * @return whether this client's player, seated in a jumper, sees their health, food, armor and experience on the
     *     jumper's console, in place of the game's own bars, which each loader leaves out while this is so
     */
    public static boolean vitalsShown() {
        final Minecraft minecraft = Minecraft.getInstance();
        return JumperHud.ridden(minecraft.player) != null && minecraft.gameMode != null
                && minecraft.gameMode.canHurtPlayer();
    }

    // The player's health, food, armor and experience on a console strip over the hotbar's: each a row of segments
    // lit as far as it is full, two points to a segment as the game's hearts and shanks are, beside its name and its
    // reading; health red, or yellow with absorption over it, food amber, armor blue, and the experience bar green
    // towards the next level, the level its reading
    private static void vitals(final GuiGraphicsExtractor graphics, final Font font, final LocalPlayer player,
            final int left, final int top) {
        final Flat screen = new Flat(graphics);
        final int width = JumperHud.HOTBAR_WIDTH + 6;
        JumperHud.cutPanel(screen, left, top, width, JumperHud.VITALS_HEIGHT, 5, JumperHud.BAR, JumperHud.BLUE);
        final int half = width / 2;
        final int upper = top + 4;
        final int lower = upper + JumperHud.LINE;
        final float absorption = player.getAbsorptionAmount();
        final float health = player.getHealth();
        JumperHud.vital(screen, font, left + 4, upper, "health", "HP", health / Math.max(1, player.getMaxHealth()),
                Integer.toString(Mth.ceil(health + absorption)), absorption > 0 ? JumperHud.YELLOW : JumperHud.RED);
        JumperHud.vital(screen, font, left + half + 2, upper, "food", "FOOD", player.getFoodData().getFoodLevel() / 20F,
                Integer.toString(player.getFoodData().getFoodLevel()), JumperHud.YELLOW);
        JumperHud.vital(screen, font, left + 4, lower, "armor", "ARMOR", player.getArmorValue() / 20F,
                Integer.toString(player.getArmorValue()), JumperHud.PALE);
        JumperHud.vital(screen, font, left + half + 2, lower, "experience", "XP", player.experienceProgress,
                Integer.toString(player.experienceLevel), JumperHud.GREEN);
    }

    // One of the player's readings: its name, ten segments lit as far as it is full, and its reading after them
    private static void vital(final Surface screen, final Font font, final int left, final int top, final String key,
            final String fallback, final float share, final String reading, final int color) {
        screen.text(font, Component.translatableWithFallback("hud.sgcraft.jumper.vital." + key, fallback).getString(),
                left, top, JumperHud.BLUE);
        final int barLeft = left + JumperHud.VITAL_LABEL;
        final int lit = Mth.ceil(Mth.clamp(share, 0, 1) * JumperHud.VITAL_SEGMENTS - 0.05F);
        for (int segment = 0; segment < JumperHud.VITAL_SEGMENTS; segment++) {
            final int x = barLeft + segment * JumperHud.VITAL_SEGMENT;
            screen.fill(x, top + 1, x + JumperHud.VITAL_SEGMENT - 1, top + 7, segment < lit ? color
                    : JumperHud.UNLIT);
        }
        final int end = barLeft + JumperHud.VITAL_SEGMENTS * JumperHud.VITAL_SEGMENT + JumperHud.VITAL_READING;
        screen.text(font, reading, end - font.width(reading), top, JumperHud.PALE);
    }

    // A system's lamp: a panel cut at its corners, lit green, yellow or red, or dark if the color is 0
    private static void lamp(final Surface pane, final Font font, final int left, final int top,
            final Component name, final int color) {
        final int glass = color == JumperHud.GREEN ? JumperHud.GREEN_GLASS : color == JumperHud.RED
                ? JumperHud.RED_GLASS : color == JumperHud.YELLOW ? JumperHud.YELLOW_GLASS : JumperHud.CONSOLE;
        final int lit = color == 0 ? JumperHud.BLUE : color;
        JumperHud.cutPanel(pane, left, top, JumperHud.LAMP_WIDTH, JumperHud.LAMP_HEIGHT, 2, glass, lit);
        pane.centeredText(font, name.getString(), left + JumperHud.LAMP_WIDTH / 2, top + 2,
                color == 0 ? JumperHud.FAINT : color);
    }

    // A hotbar slot: a panel cut at its corners, the selected one lit and filled
    private static void slot(final GuiGraphicsExtractor graphics, final int left, final int top,
            final boolean selected) {
        JumperHud.cutPanel(new Flat(graphics), left, top, JumperHud.SLOT, JumperHud.SLOT, 4, selected
                ? JumperHud.SELECTED
                : JumperHud.CONSOLE, selected ? JumperHud.PALE : JumperHud.BLUE);
    }

    // A panel with its corners cut off, some pixels in: its rows stepping in at the corners, and between them the
    // rows all as wide, drawn at once
    private static void cutPanel(final Surface pane, final int left, final int top, final int width,
            final int height, final int cut, final int fill, final int edge) {
        final int bandTop = Math.max(cut, 1);
        final int bandBottom = height - 1 - bandTop;
        if (bandTop <= bandBottom) {
            pane.fill(left + 1, top + bandTop, left + width - 1, top + bandBottom + 1, fill);
            pane.fill(left, top + bandTop, left + 1, top + bandBottom + 1, edge);
            pane.fill(left + width - 1, top + bandTop, left + width, top + bandBottom + 1, edge);
        }
        for (int row = 0; row < height; row++) {
            if (row >= bandTop && row <= bandBottom) {
                continue;
            }
            final int in = Math.max(0, Math.max(cut - row, cut - (height - 1 - row)));
            if (row == 0 || row == height - 1) {
                pane.fill(left + in, top + row, left + width - in, top + row + 1, edge);
                continue;
            }
            pane.fill(left + in + 1, top + row, left + width - in - 1, top + row + 1, fill);
            pane.fill(left + in, top + row, left + in + 1, top + row + 1, edge);
            pane.fill(left + width - in - 1, top + row, left + width - in, top + row + 1, edge);
        }
    }

    private static void item(final GuiGraphicsExtractor graphics, final Minecraft minecraft, final LocalPlayer player,
            final ItemStack stack, final int x, final int y, final int seed) {
        if (!stack.isEmpty()) {
            graphics.item(player, stack, x, y, seed);
            graphics.itemDecorations(minecraft.font, stack, x, y);
        }
    }

    // A biome's name, as the game has it, or else made from its id
    private static String biomeName(final Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> Component.translatableWithFallback(Util.makeDescriptionId("biome",
                key.identifier()), key.identifier().getPath().replace('_', ' ')).getString())
                .orElse("?").toUpperCase(Locale.ROOT);
    }

    // The time of day on the world's clock, as a 24 hour clock reads it: its day starts at six in the morning
    private static String timeOfDay(final Level level) {
        if (level.dimensionType().defaultClock().isEmpty()) {
            return "--:--";
        }
        final long ticks = Math.floorMod(level.getDefaultClockTime() + 6000, 24000L);
        return String.format(Locale.ROOT, "%02d:%02d", ticks / 1000, ticks % 1000 * 60 / 1000);
    }

    private static String distance(final double meters) {
        return meters < 1000 ? String.format(Locale.ROOT, "%d m", Math.round(meters))
                : String.format(Locale.ROOT, "%.1f km", meters / 1000);
    }

    // How far the jumper's underside is above the ground, or water, below it, in whole blocks; -1 if out of reach
    private static int aboveGround(final PuddleJumperEntity jumper) {
        final Level level = jumper.level();
        final BlockPos.MutableBlockPos pos = jumper.blockPosition().mutable();
        for (int down = 0; down < JumperHud.GROUND_REACH && pos.getY() > level.getMinY(); down++) {
            pos.move(0, -1, 0);
            final BlockState state = level.getBlockState(pos);
            if (!state.getCollisionShape(level, pos).isEmpty() || !state.getFluidState().isEmpty()) {
                return Math.max(0, Mth.floor(jumper.getY()) - pos.getY() - 1);
            }
        }
        return -1;
    }
}
