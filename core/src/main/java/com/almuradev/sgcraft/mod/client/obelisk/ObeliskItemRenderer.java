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

package com.almuradev.sgcraft.mod.client.obelisk;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.obelisk.ObeliskKind;
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

/** Draws an Asgard obelisk's item as the whole obelisk. */
public final class ObeliskItemRenderer implements NoDataSpecialModelRenderer {

    /** The renderer's type, as the obelisks' item models name it. */
    public static final Identifier TYPE = SGCraft.id("asgard_obelisk");

    private final ObeliskKind kind;

    private ObeliskItemRenderer(final ObeliskKind kind) {
        this.kind = kind;
    }

    @Override
    public void submit(final PoseStack poseStack, final SubmitNodeCollector collector, final int lightCoords,
            final int overlayCoords, final boolean hasFoil, final int outlineColor) {
        ObeliskRenderer.submitModel(poseStack, collector, this.kind, 0, lightCoords);
    }

    @Override
    public void getExtents(final Consumer<Vector3fc> output) {
        // Two blocks tall
        for (int corner = 0; corner < 8; corner++) {
            output.accept(new Vector3f(corner & 1, ((corner >> 1) & 1) * 2, (corner >> 2) & 1));
        }
    }

    /** @param knotwork whether it is the knotwork obelisk */
    public record Unbaked(boolean knotwork) implements NoDataSpecialModelRenderer.Unbaked {

        public static final MapCodec<Unbaked> CODEC = Codec.BOOL.optionalFieldOf("knotwork", false)
                .xmap(Unbaked::new, Unbaked::knotwork);

        @Override
        public MapCodec<Unbaked> type() {
            return Unbaked.CODEC;
        }

        @Override
        public SpecialModelRenderer<Void> bake(final SpecialModelRenderer.BakingContext context) {
            return new ObeliskItemRenderer(this.knotwork ? ObeliskKind.KNOTWORK : ObeliskKind.HAMMER);
        }
    }
}
