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

package com.almuradev.sgcraft.mod.client.computer;

import com.almuradev.sgcraft.mod.SGCraft;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A picture painted pixel by pixel in memory, in ARGB, with the drawing a dialing computer's screen needs: lines,
 * circles, the game's own pixel font, and the gates' glyphs at any size. Colors are blended by their alpha.
 */
final class ComputerCanvas {

    // The game's pixel font: sixteen rows of sixteen characters, eight pixels square, by character code
    private static final Identifier FONT = Identifier.withDefaultNamespace("textures/font/ascii.png");
    private static final int CELL = 8;
    // The glyph fonts: ten glyphs to a row, each 64 pixels square, in the order of the address characters
    private static final int GLYPH_CELL = 64;
    private static final int GLYPH_COLUMNS = 10;
    private static final String GLYPH_ORDER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@";

    // Each character's shape, and how wide it is, read once from the font
    private static boolean @Nullable [][] font;
    private static final int[] WIDTHS = new int[256];
    // Each glyph font's glyphs, as how covered each pixel is, and the glyphs scaled to the sizes asked for
    private static final Map<Identifier, float[][]> GLYPHS = new HashMap<>();
    private static final Map<String, float[]> SCALED = new HashMap<>();

    final int width;
    final int height;
    final int[] pixels;

    ComputerCanvas(final int width, final int height) {
        this.width = width;
        this.height = height;
        this.pixels = new int[width * height];
    }

    /** Forgets the fonts read, so they are read again after the resources reload. */
    static void forgetFonts() {
        ComputerCanvas.font = null;
        ComputerCanvas.GLYPHS.clear();
        ComputerCanvas.SCALED.clear();
    }

    void clear(final int color) {
        Arrays.fill(this.pixels, color);
    }

    /** Blends a color into a pixel, by the color's alpha, and by {@code cover} on top. */
    void blend(final int x, final int y, final int color, final float cover) {
        if (x < 0 || y < 0 || x >= this.width || y >= this.height) {
            return;
        }
        final float alpha = ((color >>> 24) / 255F) * Math.min(1, Math.max(0, cover));
        if (alpha <= 0) {
            return;
        }
        final int index = y * this.width + x;
        final int under = this.pixels[index];
        final int r = (int) (((color >> 16) & 0xFF) * alpha + ((under >> 16) & 0xFF) * (1 - alpha));
        final int g = (int) (((color >> 8) & 0xFF) * alpha + ((under >> 8) & 0xFF) * (1 - alpha));
        final int b = (int) ((color & 0xFF) * alpha + (under & 0xFF) * (1 - alpha));
        this.pixels[index] = 0xFF000000 | r << 16 | g << 8 | b;
    }

    void fill(final int x, final int y, final int w, final int h, final int color) {
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                this.blend(i, j, color, 1);
            }
        }
    }

    void rect(final int x, final int y, final int w, final int h, final int color) {
        this.fill(x, y, w, 1, color);
        this.fill(x, y + h - 1, w, 1, color);
        this.fill(x, y + 1, 1, h - 2, color);
        this.fill(x + w - 1, y + 1, 1, h - 2, color);
    }

    /** Draws a line a pixel wide, smoothed along its length. */
    void line(final double x0, final double y0, final double x1, final double y1, final int color) {
        final double length = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        final int steps = Math.max(1, (int) Math.ceil(length * 2));
        for (int i = 0; i <= steps; i++) {
            final double t = (double) i / steps;
            this.dot(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, color);
        }
    }

    // A point between pixels, shared among the four around it
    private void dot(final double x, final double y, final int color) {
        final int ix = (int) Math.floor(x);
        final int iy = (int) Math.floor(y);
        final float fx = (float) (x - ix);
        final float fy = (float) (y - iy);
        this.blend(ix, iy, color, (1 - fx) * (1 - fy) * 0.5F);
        this.blend(ix + 1, iy, color, fx * (1 - fy) * 0.5F);
        this.blend(ix, iy + 1, color, (1 - fx) * fy * 0.5F);
        this.blend(ix + 1, iy + 1, color, fx * fy * 0.5F);
    }

    /** Draws a circle's outline, about a pixel wide. */
    void circle(final double cx, final double cy, final double radius, final int color) {
        this.arc(cx, cy, radius, 0, 360, color);
    }

    /** Draws part of a circle's outline, from one angle to another, in degrees clockwise from the top. */
    void arc(final double cx, final double cy, final double radius, final double from, final double to,
            final int color) {
        final int steps = Math.max(8, (int) (Math.abs(to - from) / 360 * radius * Math.PI * 4));
        for (int i = 0; i <= steps; i++) {
            final double angle = Math.toRadians(from + (to - from) * i / steps);
            this.dot(cx + Math.sin(angle) * radius, cy - Math.cos(angle) * radius, color);
        }
    }

    /** Fills a disc. */
    void disc(final double cx, final double cy, final double radius, final int color) {
        for (int y = (int) (cy - radius - 1); y <= cy + radius + 1; y++) {
            for (int x = (int) (cx - radius - 1); x <= cx + radius + 1; x++) {
                final double distance = Math.hypot(x + 0.5 - cx, y + 0.5 - cy);
                this.blend(x, y, color, (float) (radius - distance + 0.5));
            }
        }
    }

    /** @return how wide a line of text is in the pixel font, at a scale */
    int textWidth(final String text, final int scale) {
        ComputerCanvas.loadFont();
        int width = 0;
        for (final char character : text.toCharArray()) {
            width += (ComputerCanvas.WIDTHS[character & 0xFF] + 1) * scale;
        }
        return Math.max(0, width - scale);
    }

    /** Draws text in the pixel font, its top left at a place, each font pixel {@code scale} pixels square. */
    void text(final String text, final int x, final int y, final int scale, final int color) {
        ComputerCanvas.loadFont();
        final boolean[][] shapes = ComputerCanvas.font;
        if (shapes == null) {
            return;
        }
        int left = x;
        for (final char character : text.toCharArray()) {
            final int code = character & 0xFF;
            final boolean[] shape = shapes[code];
            for (int j = 0; j < ComputerCanvas.CELL; j++) {
                for (int i = 0; i < ComputerCanvas.CELL; i++) {
                    if (shape[j * ComputerCanvas.CELL + i]) {
                        this.fill(left + i * scale, y + j * scale, scale, scale, color);
                    }
                }
            }
            left += (ComputerCanvas.WIDTHS[code] + 1) * scale;
        }
    }

    /** Draws text centered on a place. */
    void centeredText(final String text, final int x, final int y, final int scale, final int color) {
        this.text(text, x - this.textWidth(text, scale) / 2, y, scale, color);
    }

    /**
     * Draws one of a gate's glyphs, centered on a place.
     *
     * @param glyphs the glyph font to draw from
     * @param character the address character the glyph stands for
     * @param size how many pixels across the glyph's square is
     */
    void glyph(final Identifier glyphs, final char character, final double cx, final double cy, final int size,
            final int color) {
        final float[] mask = ComputerCanvas.scaled(glyphs, character, size);
        if (mask == null) {
            return;
        }
        final int left = (int) Math.round(cx - size / 2.0);
        final int top = (int) Math.round(cy - size / 2.0);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                final float cover = mask[y * size + x];
                if (cover > 0) {
                    this.blend(left + x, top + y, color, cover);
                }
            }
        }
    }

    /** Copies the picture into an image of the same size. */
    void copyTo(final NativeImage image) {
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                image.setPixel(x, y, this.pixels[y * this.width + x]);
            }
        }
    }

    // A glyph scaled down to a size, each pixel covered by the share of the glyph's own pixels that fall in it
    private static float @Nullable [] scaled(final Identifier glyphs, final char character, final int size) {
        final int index = ComputerCanvas.GLYPH_ORDER.indexOf(Character.toUpperCase(character));
        if (index < 0 || size <= 0) {
            return null;
        }
        final String key = glyphs + "/" + index + "/" + size;
        final float[] cached = ComputerCanvas.SCALED.get(key);
        if (cached != null) {
            return cached;
        }
        final float[][] all = ComputerCanvas.GLYPHS.computeIfAbsent(glyphs, ComputerCanvas::readGlyphs);
        if (index >= all.length) {
            return null;
        }
        final float[] source = all[index];
        final int cell = ComputerCanvas.GLYPH_CELL;
        final float[] mask = new float[size * size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                final int x0 = x * cell / size;
                final int x1 = Math.max(x0 + 1, (x + 1) * cell / size);
                final int y0 = y * cell / size;
                final int y1 = Math.max(y0 + 1, (y + 1) * cell / size);
                float sum = 0;
                for (int j = y0; j < y1; j++) {
                    for (int i = x0; i < x1; i++) {
                        sum += source[j * cell + i];
                    }
                }
                // Thin strokes are brightened, so a small glyph doesn't fade away
                mask[y * size + x] = Math.min(1, 1.6F * sum / ((x1 - x0) * (y1 - y0)));
            }
        }
        ComputerCanvas.SCALED.put(key, mask);
        return mask;
    }

    private static float[][] readGlyphs(final Identifier glyphs) {
        final int count = ComputerCanvas.GLYPH_ORDER.length();
        final float[][] all = new float[count][ComputerCanvas.GLYPH_CELL * ComputerCanvas.GLYPH_CELL];
        try (NativeImage image = ComputerCanvas.read(glyphs)) {
            if (image == null) {
                return all;
            }
            for (int index = 0; index < count; index++) {
                final int left = index % ComputerCanvas.GLYPH_COLUMNS * ComputerCanvas.GLYPH_CELL;
                final int top = index / ComputerCanvas.GLYPH_COLUMNS * ComputerCanvas.GLYPH_CELL;
                for (int y = 0; y < ComputerCanvas.GLYPH_CELL; y++) {
                    for (int x = 0; x < ComputerCanvas.GLYPH_CELL; x++) {
                        all[index][y * ComputerCanvas.GLYPH_CELL + x] = (image.getPixel(left + x, top + y) >>> 24)
                                / 255F;
                    }
                }
            }
        }
        return all;
    }

    private static void loadFont() {
        if (ComputerCanvas.font != null) {
            return;
        }
        final boolean[][] shapes = new boolean[256][ComputerCanvas.CELL * ComputerCanvas.CELL];
        try (NativeImage image = ComputerCanvas.read(ComputerCanvas.FONT)) {
            if (image != null) {
                final int cell = image.getWidth() / 16;
                for (int code = 0; code < 256; code++) {
                    int widest = code == ' ' ? 3 : 0;
                    for (int y = 0; y < ComputerCanvas.CELL; y++) {
                        for (int x = 0; x < ComputerCanvas.CELL; x++) {
                            final int pixel = image.getPixel(code % 16 * cell + x * cell / ComputerCanvas.CELL,
                                    code / 16 * cell + y * cell / ComputerCanvas.CELL);
                            if ((pixel >>> 24) > 127) {
                                shapes[code][y * ComputerCanvas.CELL + x] = true;
                                widest = Math.max(widest, x + 1);
                            }
                        }
                    }
                    ComputerCanvas.WIDTHS[code] = widest;
                }
            }
        }
        ComputerCanvas.font = shapes;
    }

    private static @Nullable NativeImage read(final Identifier texture) {
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(texture)) {
            return NativeImage.read(in);
        } catch (final IOException e) {
            SGCraft.LOG.error("Couldn't read {} for the dialing computer's screen", texture, e);
            return null;
        }
    }
}
