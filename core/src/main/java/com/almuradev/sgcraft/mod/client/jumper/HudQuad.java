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

import com.almuradev.sgcraft.mod.mixin.client.GuiGraphicsExtractorAccessor;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

/**
 * A filled piece of the HUD with four corners anywhere, as a rectangle on a turned pane is seen: its two upright edges
 * where the pane's perspective puts them, the top and bottom running straight between. One of these stands for a fill
 * the pane would otherwise draw as many thin upright strips. The HUD draws with no scissor, so this has none.
 *
 * @param pose the HUD's place and scale on the screen
 * @param corners the four corners, x then y, in the order the game's own filled rectangles give theirs
 */
record HudQuad(Matrix3x2fc pose, float[] corners, int color, @Nullable ScreenRectangle bounds)
        implements GuiElementRenderState {

    /**
     * Draws a piece between two upright edges, each given by where it stands across and its top and bottom.
     */
    static void add(final GuiGraphicsExtractor graphics, final float x0, final float top0, final float bottom0,
            final float x1, final float top1, final float bottom1, final int color) {
        if (x0 == x1) {
            return;
        }
        // The right edge first, bottom then top, then the left edge top then bottom, as the game's fills go round
        final boolean firstRight = x0 > x1;
        final float right = firstRight ? x0 : x1;
        final float rightTop = Math.min(firstRight ? top0 : top1, firstRight ? bottom0 : bottom1);
        final float rightBottom = Math.max(firstRight ? top0 : top1, firstRight ? bottom0 : bottom1);
        final float left = firstRight ? x1 : x0;
        final float leftTop = Math.min(firstRight ? top1 : top0, firstRight ? bottom1 : bottom0);
        final float leftBottom = Math.max(firstRight ? top1 : top0, firstRight ? bottom1 : bottom0);
        final Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        final int minY = Mth.floor(Math.min(rightTop, leftTop));
        final int maxY = Mth.ceil(Math.max(rightBottom, leftBottom));
        final ScreenRectangle bounds = new ScreenRectangle(Mth.floor(left), minY, Mth.ceil(right) - Mth.floor(left),
                maxY - minY).transformMaxBounds(pose);
        ((GuiGraphicsExtractorAccessor) graphics).sgcraft$guiRenderState().addGuiElement(new HudQuad(pose,
                new float[] {right, rightBottom, right, rightTop, left, leftTop, left, leftBottom}, color, bounds));
    }

    @Override
    public void buildVertices(final VertexConsumer vertexConsumer) {
        for (int i = 0; i < 8; i += 2) {
            vertexConsumer.addVertexWith2DPose(this.pose, this.corners[i], this.corners[i + 1]).setColor(this.color);
        }
    }

    @Override
    public RenderPipeline pipeline() {
        return RenderPipelines.GUI;
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    @Override
    public @Nullable ScreenRectangle scissorArea() {
        return null;
    }
}
