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

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.jspecify.annotations.Nullable;

/**
 * Draws the viewer's own remote view into a texture of its own, as a picture in picture, and puts the texture on the
 * screen, drawn as {@link RemoteViewTarget} draws it.
 *
 * <p>Its loader registers it for {@link State}.</p>
 */
public final class RemoteViewPicture extends PictureInPictureRenderer<RemoteViewPicture.State> {

    private final RemoteViewTarget target = new RemoteViewTarget("SGCraft remote view");

    /**
     * A remote view to be drawn into a rectangle of the screen, in GUI coordinates.
     *
     * @param partialTick how far the client is through its tick, for animations
     */
    public record State(int x0, int y0, int x1, int y1, float partialTick, @Nullable ScreenRectangle scissorArea,
            @Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {

        public State(final int x0, final int y0, final int x1, final int y1, final float partialTick,
                final @Nullable ScreenRectangle scissorArea) {
            this(x0, y0, x1, y1, partialTick, scissorArea,
                    PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
        }

        @Override
        public float scale() {
            return 1.0F;
        }
    }

    @Override
    public Class<State> getRenderStateClass() {
        return State.class;
    }

    @Override
    protected String getTextureLabel() {
        return "sgcraft_remote_view";
    }

    @Override
    protected void renderToTexture(final State state, final PoseStack poseStack, final SubmitNodeCollector collector) {
        // Never called: prepare draws the whole view itself
    }

    @Override
    public void prepare(final State state, final GuiRenderState gui, final FeatureRenderDispatcher features,
            final int guiScale) {
        final RemoteViewScene scene = RemoteViewClient.scene();
        if (scene == null || !this.target.draw(scene, (state.x1() - state.x0()) * guiScale,
                (state.y1() - state.y0()) * guiScale, features, state.partialTick())) {
            return;
        }
        final GpuTextureView picture = this.target.colorView();
        if (picture == null) {
            return;
        }
        gui.addBlitToCurrentLayer(new BlitRenderState(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                TextureSetup.singleTexture(picture, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
                state.pose(), state.x0(), state.y0(), state.x1(), state.y1(), 0.0F, 1.0F, 1.0F, 0.0F, -1,
                state.scissorArea(), null));
    }

    @Override
    public void close() {
        this.target.close();
        super.close();
    }
}
