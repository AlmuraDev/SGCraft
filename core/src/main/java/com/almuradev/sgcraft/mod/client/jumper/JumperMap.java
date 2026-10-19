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

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * The scanner on the jumper's console: a round map of the ground round the jumper, turned so the way it faces is up,
 * drawn from the chunks the client has. A sweep turns round it, and the ground it has just passed shines, then dims
 * until it comes round again; ground the client has not been sent yet stays dark, so the map fills in as the world
 * arrives. The gate the computer links to shows as a diamond, at the rim if it is further than the map reaches.
 *
 * <p>The ground is read a block a column, north up, into a grid round the jumper, a few rows each tick, so a whole grid
 * is read every few ticks and shown while the next is read. The map is painted from it at the screen's own resolution
 * some thirty times a second, turned and moved smoothly with the jumper, its rim and sweep drawn smooth too.</p>
 */
final class JumperMap {

    /** The map's radius, in the HUD's pixels. */
    static final int RADIUS = 24;
    // How many blocks the map reaches to its rim, and the grid it reads the ground into, a block a cell, wide enough
    // for the map turned any way and the jumper's flight on the turbo while one grid is read and then shown
    private static final int RANGE = 72;
    private static final int GRID = 192;
    // How many ticks reading a whole grid takes, and how many degrees the sweep turns each tick
    private static final int SCAN_TICKS = 5;
    private static final int ROWS_PER_TICK = Mth.positiveCeilDiv(JumperMap.GRID, JumperMap.SCAN_TICKS);
    // The map is painted again at most this often, in nanoseconds
    private static final long PAINT_NANOS = 33_000_000L;
    private static final float SWEEP = 9;
    // How many blocks above or below the jumper the ground is shaded for, and how far down a cavern is searched
    private static final int RELIEF = 32;
    private static final int CAVERN = 48;
    // The most pixels across the map is painted at
    private static final int MOST_PIXELS = 320;

    private static final int UNKNOWN = 0xFF0A1A20;
    private static final int RIM = 0xFF3FB8C9;
    private static final int RIM_FAINT = 0x663FB8C9;
    private static final int SWEEP_LINE = 0xFFBFF3FA;
    private static final int GATE = 0xFF5BE38A;
    private static final int GATE_EDGE = 0xFF0F3A22;

    private static final HudCanvas CANVAS = new HudCanvas("sgcraft jumper scanner");
    // The grid shown, and the block at its first corner, its least x and z; and the grid being read, its corner, the
    // height it is read from, and how many of its rows are read
    private static int[] ground = new int[JumperMap.GRID * JumperMap.GRID];
    private static int gridX;
    private static int gridZ;
    private static int[] reading = new int[JumperMap.GRID * JumperMap.GRID];
    private static int readingX;
    private static int readingZ;
    private static int readingHeight;
    private static int rowsRead = JumperMap.GRID;
    private static @Nullable ResourceKey<Level> scannedIn;
    private static long scannedAt = Long.MIN_VALUE;
    // When the map was last painted, and how big
    private static long paintedAt;
    private static int paintedSize;

    private JumperMap() {
    }

    /**
     * Paints the map, scanning first if it is time to, for the HUD to draw.
     *
     * @param pixels how many of the screen's pixels each of the HUD's spans
     * @return the map's picture, a circle {@link #RADIUS} across each way from its middle
     */
    static HudCanvas update(final float pixels, final Level level, final Vec3 position, final float yaw,
            final Optional<BlockPos> gate, final float partialTick) {
        final long time = level.getGameTime();
        if (!level.dimension().equals(JumperMap.scannedIn) || time < JumperMap.scannedAt) {
            // In a new world the whole grid is read at once, so the map doesn't start dark
            JumperMap.start(position);
            JumperMap.read(level, JumperMap.GRID);
            JumperMap.start(position);
            JumperMap.scannedIn = level.dimension();
            JumperMap.scannedAt = time;
        } else if (time > JumperMap.scannedAt) {
            JumperMap.read(level, (int) Math.min(time - JumperMap.scannedAt, JumperMap.SCAN_TICKS)
                    * JumperMap.ROWS_PER_TICK);
            JumperMap.scannedAt = time;
            if (JumperMap.rowsRead >= JumperMap.GRID) {
                JumperMap.start(position);
            }
        }
        final int size = Mth.clamp(Math.round(JumperMap.RADIUS * 2 * pixels), 32, JumperMap.MOST_PIXELS);
        final long now = System.nanoTime();
        if (size != JumperMap.paintedSize || now - JumperMap.paintedAt >= JumperMap.PAINT_NANOS) {
            JumperMap.paint(JumperMap.CANVAS.pixels(size, size), size, size / (JumperMap.RADIUS * 2F), position, yaw,
                    gate, Mth.positiveModulo((time + partialTick) * JumperMap.SWEEP, 360));
            JumperMap.CANVAS.upload();
            JumperMap.paintedAt = now;
            JumperMap.paintedSize = size;
        }
        return JumperMap.CANVAS;
    }

    // Paints the map, a pixel at a time: the ground, dimmed behind the sweep, the rings and the sweep's line, the
    // jumper's arrowhead and the gate's diamond
    private static void paint(final NativeImage image, final int size, final float pixels, final Vec3 position,
            final float yaw, final Optional<BlockPos> gate, final float sweep) {
        final float radius = size / 2F;
        final float blocks = JumperMap.RANGE / radius;
        final double radians = Math.toRadians(yaw);
        final double aheadX = -Math.sin(radians);
        final double aheadZ = Math.cos(radians);
        final double rightX = -Math.cos(radians);
        final double rightZ = -Math.sin(radians);
        final float line = Math.max(1, pixels * 0.6F);
        final float sweepRadians = (float) Math.toRadians(sweep);
        final float sweepX = Mth.sin(sweepRadians);
        final float sweepY = Mth.cos(sweepRadians);
        // Where the gate shows, in the map's pixels from its middle, right and up
        float gateX = Float.NaN;
        float gateY = Float.NaN;
        if (gate.isPresent()) {
            final double ox = gate.get().getX() + 0.5 - position.x;
            final double oz = gate.get().getZ() + 0.5 - position.z;
            gateX = (float) ((rightX * ox + rightZ * oz) / blocks);
            gateY = (float) ((aheadX * ox + aheadZ * oz) / blocks);
            final float reach = radius - 3 * pixels;
            final float distance = Mth.sqrt(gateX * gateX + gateY * gateY);
            if (distance > reach) {
                gateX *= reach / distance;
                gateY *= reach / distance;
            }
        }
        final float arrow = 3.5F * pixels;
        final float diamond = 3.5F * pixels;
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                final float x = px + 0.5F - radius;
                final float y = radius - py - 0.5F;
                final float distance = Mth.sqrt(x * x + y * y);
                if (distance > radius + 1) {
                    image.setPixel(px, py, 0);
                    continue;
                }
                // The ground under this pixel
                final double wx = position.x + (aheadX * y + rightX * x) * blocks;
                final double wz = position.z + (aheadZ * y + rightZ * x) * blocks;
                int color = JumperMap.ground(wx - JumperMap.gridX - 0.5, wz - JumperMap.gridZ - 0.5);
                // Dimmer the longer since the sweep passed
                final float angle = Mth.positiveModulo((float) Math.toDegrees(Math.atan2(x, y)), 360);
                color = JumperMap.shade(color, 1 - 0.55F * Mth.positiveModulo(sweep - angle, 360) / 360);
                // The sweep's line, ahead of the middle along the sweep
                final float along = x * sweepX + y * sweepY;
                if (along > 0) {
                    color = HudCanvas.over(color, JumperMap.SWEEP_LINE, line - Math.abs(x * sweepY - y * sweepX));
                }
                // The rings: the rim, and a faint one halfway
                color = HudCanvas.over(color, JumperMap.RIM_FAINT, line - Math.abs(distance - radius / 2));
                color = HudCanvas.over(color, JumperMap.RIM, line * 1.5F - Math.abs(distance - (radius - line)));
                // The jumper, an arrowhead pointing up
                if (y < arrow - Math.abs(x) * 2.3F && y > -arrow * 0.6F + (arrow * 0.7F - Math.abs(x)) * 0.6F) {
                    color = JumperMap.SWEEP_LINE;
                }
                // The gate, a diamond edged dark
                if (!Float.isNaN(gateX)) {
                    final float off = Math.abs(x - gateX) + Math.abs(y - gateY);
                    if (off < diamond) {
                        color = off < diamond - pixels ? JumperMap.GATE : JumperMap.GATE_EDGE;
                    }
                }
                image.setPixel(px, py, HudCanvas.fade(color, radius + 0.5F - distance));
            }
        }
    }

    // The ground's colour at a place on the grid, blended between the four cells round it
    private static int ground(final double gx, final double gz) {
        final int x0 = Mth.floor(gx);
        final int z0 = Mth.floor(gz);
        if (x0 < 0 || z0 < 0 || x0 + 1 >= JumperMap.GRID || z0 + 1 >= JumperMap.GRID) {
            return JumperMap.UNKNOWN;
        }
        final float fx = (float) (gx - x0);
        final float fz = (float) (gz - z0);
        final int[] ground = JumperMap.ground;
        final int a = ground[z0 * JumperMap.GRID + x0];
        final int b = ground[z0 * JumperMap.GRID + x0 + 1];
        final int c = ground[(z0 + 1) * JumperMap.GRID + x0];
        final int d = ground[(z0 + 1) * JumperMap.GRID + x0 + 1];
        int out = 0xFF000000;
        for (int shift = 0; shift <= 16; shift += 8) {
            final float top = Mth.lerp(fx, a >> shift & 0xFF, b >> shift & 0xFF);
            final float bottom = Mth.lerp(fx, c >> shift & 0xFF, d >> shift & 0xFF);
            out |= Math.round(Mth.lerp(fz, top, bottom)) << shift;
        }
        return out;
    }

    // Starts reading a grid round a point
    private static void start(final Vec3 position) {
        JumperMap.readingX = Mth.floor(position.x) - JumperMap.GRID / 2;
        JumperMap.readingZ = Mth.floor(position.z) - JumperMap.GRID / 2;
        JumperMap.readingHeight = Mth.floor(position.y);
        JumperMap.rowsRead = 0;
    }

    // Reads the colour of the ground into the next rows of the grid being read, a chunk at a time along each row;
    // once all are read, it is the grid shown
    private static void read(final Level level, final int rows) {
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final int lastX = JumperMap.readingX + JumperMap.GRID - 1;
        final int begin = JumperMap.rowsRead;
        final int end = Math.min(JumperMap.GRID, begin + rows);
        for (int row = begin; row < end; row++) {
            final int z = JumperMap.readingZ + row;
            for (int chunkX = JumperMap.readingX >> 4; chunkX <= lastX >> 4; chunkX++) {
                final ChunkAccess chunk = level.getChunk(chunkX, z >> 4, ChunkStatus.FULL, false);
                final int x0 = Math.max(JumperMap.readingX, chunkX << 4);
                final int x1 = Math.min(lastX, (chunkX << 4) + 15);
                for (int x = x0; x <= x1; x++) {
                    JumperMap.reading[row * JumperMap.GRID + x - JumperMap.readingX] = chunk == null
                            ? JumperMap.UNKNOWN : JumperMap.sample(level, chunk, pos, x, z, JumperMap.readingHeight);
                }
            }
        }
        JumperMap.rowsRead = end;
        if (begin < JumperMap.GRID && end >= JumperMap.GRID) {
            final int[] shown = JumperMap.ground;
            JumperMap.ground = JumperMap.reading;
            JumperMap.reading = shown;
            JumperMap.gridX = JumperMap.readingX;
            JumperMap.gridZ = JumperMap.readingZ;
        }
    }

    // The colour of the ground at a column
    private static int sample(final Level level, final ChunkAccess chunk, final BlockPos.MutableBlockPos pos,
            final int x, final int z, final int height) {
        int top;
        if (level.dimensionType().hasCeiling()) {
            // Under a ceiling the ground is the first block below the jumper
            top = Math.min(height + 3, level.getMinY() + level.getHeight() - 1);
            final int floor = Math.max(level.getMinY(), top - JumperMap.CAVERN);
            while (top > floor && chunk.getBlockState(pos.set(x, top, z)).isAir()) {
                top--;
            }
        } else {
            top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15) - 1;
        }
        pos.set(x, top, z);
        final BlockState state = chunk.getBlockState(pos);
        final int color = state.getFluidState().isEmpty() ? state.getMapColor(level, pos).col : MapColor.WATER.col;
        if (color == 0) {
            return JumperMap.UNKNOWN;
        }
        return JumperMap.shade(0xFF000000 | color, 1 + Mth.clamp((top - height) / (float) JumperMap.RELIEF, -1, 1)
                * 0.25F);
    }

    private static int shade(final int color, final float brightness) {
        final int red = Mth.clamp(Math.round((color >> 16 & 0xFF) * brightness), 0, 255);
        final int green = Mth.clamp(Math.round((color >> 8 & 0xFF) * brightness), 0, 255);
        final int blue = Mth.clamp(Math.round((color & 0xFF) * brightness), 0, 255);
        return color & 0xFF000000 | red << 16 | green << 8 | blue;
    }
}
