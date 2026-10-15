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

import com.almuradev.sgcraft.mod.SGCraft;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.textures.GpuTexture;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Takes a picture of the client's own remote view, drawn on its own, apart from any screen, at the window's full
 * resolution. A picture asked for is drawn at the next frame, where the views on displays are drawn, and read back
 * from the GPU a frame or so later. Only the render thread uses this.
 */
public final class RemoteViewSnapshot {

    // The picture asked for, if one is: its width over its height, and what it is handed to
    private static float aspect;
    private static @Nullable Consumer<@Nullable NativeImage> pending;

    private RemoteViewSnapshot() {
    }

    /**
     * Asks for a picture of the client's own remote view, as large as fits the window's framebuffer at the given
     * width over height. The picture is handed over on the render thread, and belongs to whoever it is handed to,
     * who closes it; null is handed over if there is no view to draw by then, or it couldn't be read back. A picture
     * asked for while another waits to be drawn replaces it, and the one replaced is handed null.
     *
     * @param aspect the picture's width over its height
     */
    public static void request(final float aspect, final Consumer<@Nullable NativeImage> done) {
        final Consumer<@Nullable NativeImage> before = RemoteViewSnapshot.pending;
        RemoteViewSnapshot.aspect = aspect;
        RemoteViewSnapshot.pending = done;
        if (before != null) {
            before.accept(null);
        }
    }

    /** Draws the picture asked for, if one is. The client calls this once a frame, after the world is drawn. */
    public static void render() {
        final Consumer<@Nullable NativeImage> done = RemoteViewSnapshot.pending;
        if (done == null) {
            return;
        }
        RemoteViewSnapshot.pending = null;
        final Minecraft minecraft = Minecraft.getInstance();
        final RemoteViewScene scene = RemoteViewClient.scene();
        final int[] size = RemoteViewSnapshot.size(minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight(),
                RemoteViewSnapshot.aspect);
        if (scene == null || !scene.loaded() || size[0] <= 0 || size[1] <= 0) {
            done.accept(null);
            return;
        }
        final RemoteViewTarget target = new RemoteViewTarget("SGCraft remote view snapshot");
        final float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        final GpuTexture texture;
        try {
            texture = target.draw(scene, size[0], size[1], minecraft.gameRenderer.featureRenderDispatcher(),
                    partialTick) ? target.color() : null;
        } catch (final RuntimeException e) {
            SGCraft.LOG.error("Couldn't draw a remote view snapshot", e);
            target.close();
            done.accept(null);
            return;
        }
        if (texture == null) {
            target.close();
            done.accept(null);
            return;
        }
        RemoteViewSnapshot.readBack(target, texture, size[0], size[1], done);
    }

    // Copies the texture into a buffer and, once the GPU has, into a picture, the right way up
    private static void readBack(final RemoteViewTarget target, final GpuTexture texture, final int width,
            final int height, final Consumer<@Nullable NativeImage> done) {
        final GpuDevice device = RenderSystem.getDevice();
        final int pixel = texture.getFormat().blockSize();
        final GpuBuffer buffer = device.createBuffer(() -> "SGCraft remote view snapshot",
                GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST, (long) width * height * pixel);
        device.createCommandEncoder().copyTextureToBuffer(texture, buffer, 0L, () -> {
            NativeImage image = null;
            try (GpuBufferSlice.MappedView read = buffer.map(true, false)) {
                image = new NativeImage(width, height, false);
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        // The texture's rows run from the bottom up
                        image.setPixelABGR(x, height - y - 1, read.data().getInt((x + y * width) * pixel)
                                | 0xFF000000);
                    }
                }
            } catch (final RuntimeException e) {
                SGCraft.LOG.error("Couldn't read back a remote view snapshot", e);
                if (image != null) {
                    image.close();
                    image = null;
                }
            } finally {
                buffer.close();
                target.close();
            }
            done.accept(image);
        }, 0);
    }

    // The largest width and height at the aspect that fit the framebuffer
    private static int[] size(final int width, final int height, final float aspect) {
        if (width <= 0 || height <= 0 || !(aspect > 0) || !Float.isFinite(aspect)) {
            return new int[] {0, 0};
        }
        final int tall = Math.round(width / aspect);
        if (tall <= height) {
            return new int[] {width, Math.max(1, tall)};
        }
        return new int[] {Math.max(1, Math.round(height * aspect)), height};
    }
}
