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

package com.almuradev.sgcraft.mod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;

/**
 * Whether something reaching far past its block is in the player's view. A block entity drawn though its own block is
 * off screen, as a gate is round its base, is culled by its whole box on NeoForge, but the game itself culls such
 * block entities by distance alone, so on Fabric they would be drawn behind the camera too; each such renderer asks
 * this from {@code shouldRender}. Remote views draw block entities themselves and don't ask.
 */
public final class ViewCulling {

    private ViewCulling() {
    }

    /** @return whether any of a box is in the main camera's view this frame */
    public static boolean visible(final AABB box) {
        return Minecraft.getInstance().gameRenderer.mainCamera().getCullFrustum().isVisible(box);
    }
}
