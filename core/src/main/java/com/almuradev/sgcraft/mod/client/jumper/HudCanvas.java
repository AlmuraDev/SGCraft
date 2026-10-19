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
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * A picture the HUD paints itself, a pixel at a time, at the screen's own resolution rather than the GUI's, so its
 * curves and slants are smooth: the jumper's scanner and its panes. It is remade whenever the size it is asked for
 * changes.
 */
final class HudCanvas {

    private final String label;
    private @Nullable DynamicTexture texture;

    HudCanvas(final String label) {
        this.label = label;
    }

    /** @return the canvas's pixels, at a size, new and clear if it was not that size already */
    NativeImage pixels(final int width, final int height) {
        if (this.texture == null || this.texture.getPixels().getWidth() != width
                || this.texture.getPixels().getHeight() != height) {
            if (this.texture != null) {
                this.texture.close();
            }
            this.texture = new DynamicTexture(() -> this.label, width, height, true);
        }
        return this.texture.getPixels();
    }

    /** @return whether the canvas is that size already */
    boolean sized(final int width, final int height) {
        return this.texture != null && this.texture.getPixels().getWidth() == width
                && this.texture.getPixels().getHeight() == height;
    }

    /** Sends what was painted to the graphics card. */
    void upload() {
        if (this.texture != null) {
            this.texture.upload();
        }
    }

    /** Draws the canvas over a rectangle of the GUI, turned left to right if asked. */
    void draw(final GuiGraphicsExtractor graphics, final int x0, final int y0, final int x1, final int y1,
            final boolean mirrored) {
        if (this.texture != null) {
            graphics.blit(this.texture.getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(
                    FilterMode.LINEAR), x0, y0, x1, y1, mirrored ? 1 : 0, mirrored ? 0 : 1, 0, 1);
        }
    }

    /** Draws a strip of the canvas, between two fractions of its width, over a rectangle of the GUI. */
    void drawPart(final GuiGraphicsExtractor graphics, final int x0, final int y0, final int x1, final int y1,
            final float u0, final float u1) {
        if (this.texture != null) {
            graphics.blit(this.texture.getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(
                    FilterMode.LINEAR), x0, y0, x1, y1, u0, u1, 0, 1);
        }
    }

    /** @return one colour laid over another, by how much of it shows, from 0 to 1 */
    static int over(final int under, final int top, final float amount) {
        if (under >>> 24 == 0) {
            return HudCanvas.fade(top, amount);
        }
        final float t = Mth.clamp(amount, 0, 1) * (top >>> 24) / 255F;
        final int alpha = Math.round((under >>> 24) + ((255 - (under >>> 24)) * t));
        final int red = Math.round(Mth.lerp(t, under >> 16 & 0xFF, top >> 16 & 0xFF));
        final int green = Math.round(Mth.lerp(t, under >> 8 & 0xFF, top >> 8 & 0xFF));
        final int blue = Math.round(Mth.lerp(t, under & 0xFF, top & 0xFF));
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    /** @return a colour with its alpha scaled, from 0 to 1 */
    static int fade(final int color, final float amount) {
        return Math.round((color >>> 24) * Mth.clamp(amount, 0, 1)) << 24 | color & 0xFFFFFF;
    }
}
