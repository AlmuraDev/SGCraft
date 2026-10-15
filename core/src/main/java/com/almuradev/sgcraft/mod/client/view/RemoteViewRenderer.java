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

import com.almuradev.sgcraft.mod.client.ClientPlatform;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jspecify.annotations.Nullable;

/**
 * Draws the remote view the player is watching into part of a screen, from the source's camera, as a picture in
 * picture: a little world of its own, drawn apart from the world the player is in.
 */
public final class RemoteViewRenderer {

    // The lightmap the game hands out while a view is drawn, so what the game's own renderers draw into the view is
    // lit by the view's light
    private static @Nullable GpuTextureView lightmap;

    private RemoteViewRenderer() {
    }

    /**
     * Draws the client's own live view into a rectangle of the screen, in GUI coordinates. Screens call this as they
     * extract their render state. While a screen shows it, the views on displays in the world aren't drawn again.
     *
     * @return whether a view was drawn; nothing is drawn, and false returned, when there is no view or it is still
     *     loading
     */
    public static boolean extract(final GuiGraphicsExtractor graphics, final int left, final int top, final int right,
            final int bottom, final float partialTick) {
        final RemoteViewScene scene = RemoteViewClient.scene();
        if (scene == null || right <= left || bottom <= top) {
            return false;
        }
        if (!scene.loaded()) {
            // Nothing is drawn while the view loads, so its meshes are built from here until it has
            scene.prepare(scene.cameraPosition(System.nanoTime()));
            if (!scene.loaded()) {
                return false;
            }
        }
        ClientPlatform.get().submitPictureInPicture(graphics, scissor -> new RemoteViewPicture.State(left, top, right,
                bottom, partialTick, scissor));
        RemoteViewDisplays.screenShown();
        return true;
    }

    /** @return the lightmap to use in place of the game's own while a view is drawn, or null at any other time */
    public static @Nullable GpuTextureView lightmapOverride() {
        return RemoteViewRenderer.lightmap;
    }

    static void setLightmapOverride(final @Nullable GpuTextureView lightmap) {
        RemoteViewRenderer.lightmap = lightmap;
    }
}
