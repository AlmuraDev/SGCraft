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

package com.almuradev.sgcraft.mod.client.dev;

import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.SGCraftApi;
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.gate.DialMode;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.GateState;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.SGCraft;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Takes the wiki's pictures of finished gates in the world, idle and with their wormhole open, by day and by night,
 * after the icon export. It runs the export's own flat world through commands, as a player would: it builds each gate
 * far from spawn, stands the player in front of it with the game's interface hidden, sets the time, waits for the
 * world to draw, and saves the frame as {@code <name>-day.png} and {@code <name>-night.png}. Then it builds a second
 * gate behind the player, dials it from the first, and once the wormhole is open saves {@code <name>-open-day.png}
 * and {@code <name>-open-night.png}. When the last is saved it closes the game.
 */
public final class SceneExport {

    // How long to wait after moving, for the chunks around the player to load
    private static final int LOAD_TICKS = 60;
    // How long to wait after changing the time, for the sky and light to settle
    private static final int SETTLE_TICKS = 20;
    private static final int DAY = 6000;
    private static final int NIGHT = 18000;
    // The ground of the export's flat world: the player stands at this height
    private static final int GROUND = -60;
    // Where the gates are built, away from spawn, one every SPACING blocks east
    private static final int ORIGIN_X = 2000;
    private static final int ORIGIN_Z = 2000;
    private static final int SPACING = 64;
    // How far behind the player the gate dialed is built, out of the picture
    private static final int BEHIND = 40;
    // The longest a dial may take before the export gives up on that gate's open pictures
    private static final int DIAL_TICKS = 1200;
    // How far from where it was asked for a gate may be built and still be found
    private static final int NEAR = 8;
    // How far round a scene the world is cleared to open grass, past the gates and the player
    private static final int CLEAR = 24;

    private static @Nullable SceneExport running;

    private final Path out;
    private final ArrayDeque<Step> steps = new ArrayDeque<>();
    private int waiting;
    private boolean shooting;

    private SceneExport(final Path out) {
        this.out = out;
        this.command("gamerule advance_time false");
        this.command("gamerule advance_weather false");
        this.command("gamerule spawn_mobs false");
        this.command("gamerule send_command_feedback false");
        this.command("gamerule max_block_modifications 1000000");
        this.command("weather clear");
        this.scene(0, "milky-way", "sgcraft:milky_way", 2, "sgcraft:dhd");
        this.scene(1, "pegasus", "sgcraft:pegasus", 2, "sgcraft:dhd_pegasus");
        this.scene(2, "universe", "sgcraft:universe", 2, null);
        this.scene(3, "supergate", "sgcraft:milky_way", 6, null);
    }

    /** Starts taking the pictures, saved in {@code out}. Call this on the render thread, in the export's world. */
    public static void start(final Minecraft minecraft, final Path out) {
        SceneExport.running = new SceneExport(out);
        minecraft.gui.setScreen(null);
        if (!minecraft.gui.hud.isHidden()) {
            minecraft.gui.hud.toggle();
        }
    }

    /** Takes the next step of a running export. Call this once a client tick. */
    public static void tickClient(final Minecraft minecraft) {
        if (SceneExport.running != null) {
            SceneExport.running.tick(minecraft);
        }
    }

    // Builds a gate of a type and radius, with a DHD to its side if one is named, and pictures it by day and night.
    // The gate is built facing north, and the player stands north of it looking south at its face, far enough back
    // that the whole ring shows
    private void scene(final int index, final String name, final String type, final int radius,
            final @Nullable String dhd) {
        final int x = SceneExport.ORIGIN_X + index * SceneExport.SPACING;
        final int z = SceneExport.ORIGIN_Z;
        final int across = radius * 2 + 1;
        final int back = Math.max(8, across + 5);
        // A supergate is tall, so the player stands raised on a barrier, level with its middle
        final int rise = Math.max(0, radius - 2);
        final int standZ = z + 3 - back;
        this.command(String.format(Locale.ROOT, "tp @s %d.5 %d %d.5 0 0", x, SceneExport.GROUND, standZ));
        this.waitTicks(SceneExport.LOAD_TICKS);
        // Whatever the world built there, such as a village, gives way to open grass
        final int farZ = standZ - SceneExport.BEHIND;
        this.command(String.format(Locale.ROOT, "fill %d %d %d %d %d %d air", x - SceneExport.CLEAR,
                SceneExport.GROUND, farZ - SceneExport.CLEAR, x + SceneExport.CLEAR, SceneExport.GROUND + 2 * radius
                + SceneExport.CLEAR, z + SceneExport.CLEAR));
        this.command(String.format(Locale.ROOT, "fill %d %d %d %d %d %d grass_block", x - SceneExport.CLEAR,
                SceneExport.GROUND - 1, farZ - SceneExport.CLEAR, x + SceneExport.CLEAR, SceneExport.GROUND - 1,
                z + SceneExport.CLEAR));
        this.command("kill @e[type=!player]");
        this.command(String.format(Locale.ROOT, "execute positioned %d %d %d rotated 0 0 run sgcraft place %s 9 %d",
                x, SceneExport.GROUND, z, type, radius));
        if (dhd != null) {
            this.command(String.format(Locale.ROOT, "setblock %d %d %d %s[facing=north]", x + 2, SceneExport.GROUND,
                    z, dhd));
        }
        if (rise > 0) {
            this.command(String.format(Locale.ROOT, "setblock %d %d %d barrier", x, SceneExport.GROUND + rise - 1,
                    standZ));
        }
        final float pitch = rise > 0 ? 0 : -4;
        this.command(String.format(Locale.ROOT, "tp @s %d.5 %d %d.5 0 %s", x, SceneExport.GROUND + rise, standZ,
                pitch));
        this.waitTicks(SceneExport.LOAD_TICKS);
        this.shots(name);
        // The far gate stands behind the player, facing away, so neither it nor its wormhole shows
        this.command(String.format(Locale.ROOT, "execute positioned %d %d %d rotated 180 0 run sgcraft place %s 9",
                x, SceneExport.GROUND, farZ, type));
        final BlockPos near = new BlockPos(x, SceneExport.GROUND, z + 3);
        final BlockPos far = new BlockPos(x, SceneExport.GROUND, farZ - 3);
        // The command reaches the server as a packet, so give it time to build the gate before dialing
        this.waitTicks(SceneExport.SETTLE_TICKS);
        this.steps.add(Step.run(server -> SceneExport.open(server, near, far)));
        this.steps.add(Step.until(server -> SceneExport.gate(server, near).map(Stargate::state)
                .filter(state -> state == GateState.CONNECTED).isPresent()));
        this.waitTicks(SceneExport.SETTLE_TICKS);
        this.shots(name + "-open");
    }

    // Pictures the scene by day and by night
    private void shots(final String name) {
        for (final boolean day : new boolean[] {true, false}) {
            this.command("time set " + (day ? SceneExport.DAY : SceneExport.NIGHT));
            this.waitTicks(SceneExport.SETTLE_TICKS);
            this.steps.add(new Step(Step.Kind.SHOT, name + (day ? "-day" : "-night"), 0, null, null));
        }
    }

    // The gate built nearest a place, within a few blocks: the place command may move a gate a little to fit it
    private static Optional<Stargate> gate(final MinecraftServer server, final BlockPos near) {
        return SGCraftApi.get().stargates().in(Level.OVERWORLD).stream()
                .filter(gate -> gate.position().pos().distSqr(near) <= SceneExport.NEAR * SceneExport.NEAR)
                .min(Comparator.comparingDouble(gate -> gate.position().pos().distSqr(near)));
    }

    // Lets both gates dial and open for free, harmlessly and for as long as the pictures take, then dials
    private static void open(final MinecraftServer server, final BlockPos near, final BlockPos far) {
        final Cause cause = Cause.of(server);
        final Optional<Stargate> from = SceneExport.gate(server, near);
        final Optional<Stargate> to = SceneExport.gate(server, far);
        if (from.isEmpty() || to.isEmpty()) {
            SGCraft.LOG.error("The scene's gates weren't built, at {} and {}", near, far);
            return;
        }
        for (final Stargate gate : new Stargate[] {from.get(), to.get()}) {
            gate.set(GateProperties.REQUIRES_POWER, false, cause);
            gate.set(GateProperties.OPENING_DAMAGE, false, cause);
            gate.set(GateProperties.OPEN_TICKS, 0, cause);
        }
        // A quick dial, every chevron locking at once, as a full one turns the ring for each symbol
        final Optional<Address> address = SGCraftApi.get().addressing().addressOf(to.get().position(), Level.OVERWORLD);
        if (address.isEmpty()) {
            SGCraft.LOG.error("The scene's gate at {} has no address", far);
            return;
        }
        SGCraft.LOG.info("The scene's gate at {} dials {}", near, address.get());
        from.get().dial(address.get(), DialMode.QUICK, cause).whenComplete((result, error) ->
                SGCraft.LOG.info("The scene's dial from {} ended: {}", near, error != null ? error : result));
    }

    private void command(final String command) {
        this.steps.add(new Step(Step.Kind.COMMAND, command, 0, null, null));
    }

    private void waitTicks(final int ticks) {
        this.steps.add(new Step(Step.Kind.WAIT, "", ticks, null, null));
    }

    private void tick(final Minecraft minecraft) {
        if (this.shooting || minecraft.player == null) {
            return;
        }
        if (this.waiting > 0) {
            this.waiting--;
            return;
        }
        final Step step = this.steps.poll();
        if (step == null) {
            SceneExport.running = null;
            SGCraft.LOG.info("Saved the scenes to {}", this.out);
            minecraft.stop();
            return;
        }
        switch (step.kind()) {
            case COMMAND -> minecraft.player.connection.sendCommand(step.text());
            case WAIT -> this.waiting = step.ticks();
            case RUN -> {
                final MinecraftServer server = minecraft.getSingleplayerServer();
                if (server != null && step.action() != null) {
                    server.execute(() -> step.action().accept(server));
                }
            }
            case UNTIL -> {
                final MinecraftServer server = minecraft.getSingleplayerServer();
                final Predicate<MinecraftServer> until = step.until();
                final boolean done = server == null || until == null
                        || server.submit(() -> until.test(server)).join();
                if (!done && step.ticks() < SceneExport.DIAL_TICKS) {
                    this.steps.addFirst(new Step(Step.Kind.UNTIL, "", step.ticks() + 1, null, until));
                } else if (!done) {
                    SGCraft.LOG.error("The scene's gate didn't open in time");
                }
            }
            case SHOT -> {
                // The world must be drawn whole before the frame is kept
                if (!minecraft.levelRenderer.hasRenderedAllSections()) {
                    this.steps.addFirst(step);
                    return;
                }
                this.shooting = true;
                Screenshot.takeScreenshot(minecraft.gameRenderer.mainRenderTarget(), image -> minecraft.execute(() ->
                        this.save(step.text(), image)));
            }
        }
    }

    private void save(final String name, final NativeImage image) {
        try (image) {
            final Path file = this.out.resolve(name + ".png");
            Files.createDirectories(file.getParent());
            image.writeToFile(file);
        } catch (final IOException e) {
            SGCraft.LOG.error("Couldn't save the scene {}", name, e);
        }
        this.shooting = false;
    }

    // One thing the export does in turn: send a command, wait some ticks, run something on the server, wait until
    // something is true there (counting the ticks waited), or keep the frame drawn as a picture
    private record Step(Kind kind, String text, int ticks, @Nullable Consumer<MinecraftServer> action,
            @Nullable Predicate<MinecraftServer> until) {

        static Step run(final Consumer<MinecraftServer> action) {
            return new Step(Kind.RUN, "", 0, action, null);
        }

        static Step until(final Predicate<MinecraftServer> until) {
            return new Step(Kind.UNTIL, "", 0, null, until);
        }

        enum Kind {
            COMMAND,
            WAIT,
            RUN,
            UNTIL,
            SHOT
        }
    }
}
