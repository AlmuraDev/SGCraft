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

package com.almuradev.sgcraft.client.dhd;

import com.almuradev.sgcraft.SGCraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

/**
 * A DHD as an item, in the hand and in the inventory: the same model the block is drawn with, its dome unlit, as a
 * DHD with no gate. Each loader registers {@link #TYPE} with {@link Unbaked#CODEC}.
 */
public final class DhdItemRenderer implements NoDataSpecialModelRenderer {

    /** The renderer's type, as the DHDs' item models name it. */
    public static final Identifier TYPE = SGCraft.id("dhd");

    private final boolean pegasus;

    private DhdItemRenderer(final boolean pegasus) {
        this.pegasus = pegasus;
    }

    @Override
    public void submit(final PoseStack poseStack, final SubmitNodeCollector collector, final int lightCoords,
            final int overlayCoords, final boolean hasFoil, final int outlineColor) {
        DhdRenderer.submitModel(poseStack, collector, this.pegasus, 0, lightCoords);
    }

    @Override
    public void getExtents(final Consumer<Vector3fc> output) {
        // The block's own space, which the model fills
        for (int corner = 0; corner < 8; corner++) {
            output.accept(new Vector3f(corner & 1, (corner >> 1) & 1, (corner >> 2) & 1));
        }
    }

    /** @param pegasus whether it is a Pegasus DHD, whose dome is drawn in its colors */
    public record Unbaked(boolean pegasus) implements NoDataSpecialModelRenderer.Unbaked {

        public static final MapCodec<Unbaked> CODEC = Codec.BOOL.optionalFieldOf("pegasus", false)
                .xmap(Unbaked::new, Unbaked::pegasus);

        @Override
        public MapCodec<Unbaked> type() {
            return Unbaked.CODEC;
        }

        @Override
        public SpecialModelRenderer<Void> bake(final SpecialModelRenderer.BakingContext context) {
            return new DhdItemRenderer(this.pegasus);
        }
    }
}
