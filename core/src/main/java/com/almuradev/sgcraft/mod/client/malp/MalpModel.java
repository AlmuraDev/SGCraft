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

package com.almuradev.sgcraft.mod.client.malp;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.client.model.CubeModels;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;

/**
 * The MALP, after the show's Mark II: a low, boxy chassis on six knobby tires, three a side, with sloped panels in
 * hazard stripes front and back and diamond plate between them. A camera on a mast pans and tilts in the middle; a
 * dish antenna on a short post leans up and back from a rear corner; a jointed grapple arm reaches forward over the
 * front to a small claw; and two headlights sit above the front bumper.
 *
 * <p>Its cubes are in {@code entity_models/malp.json}. Sizes are in pixels, a sixteenth of a block. The model stands
 * on y = 0 with up as negative y and its front towards negative z, as entity models do. Its texture is 128 by
 * 128.</p>
 */
public final class MalpModel extends EntityModel<MalpRenderState> {

    /** The MALP's model layer. */
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(SGCraft.id("malp"), "main");
    /** The layer of the headlights' glow, drawn over the headlights when the lamp is on. */
    public static final ModelLayerLocation GLOW_LAYER = new ModelLayerLocation(SGCraft.id("malp"), "glow");

    private static final String MODEL = "/assets/sgcraft/entity_models/malp.json";
    private static final String[] WHEELS = {"left_front", "left_middle", "left_back", "right_front", "right_middle",
            "right_back"};

    private final ModelPart[] leftWheels = new ModelPart[3];
    private final ModelPart[] rightWheels = new ModelPart[3];
    private final ModelPart mast;
    private final ModelPart head;

    public MalpModel(final ModelPart root) {
        super(root);
        for (int i = 0; i < 3; i++) {
            this.leftWheels[i] = root.getChild(MalpModel.WHEELS[i]);
            this.rightWheels[i] = root.getChild(MalpModel.WHEELS[i + 3]);
        }
        this.mast = root.getChild("mast");
        this.head = this.mast.getChild("head");
    }

    /** @return the MALP's model: its body, wheels, mast, dish and arm */
    public static LayerDefinition createLayer() {
        return CubeModels.layer(MalpModel.MODEL, "main");
    }

    /** @return the headlights alone, to glow over the model's own when the lamp is on */
    public static LayerDefinition createGlowLayer() {
        return CubeModels.layer(MalpModel.MODEL, "glow");
    }

    @Override
    public void setupAnim(final MalpRenderState state) {
        super.setupAnim(state);
        for (int i = 0; i < 3; i++) {
            this.leftWheels[i].xRot = state.leftWheels;
            this.rightWheels[i].xRot = state.rightWheels;
        }
        this.mast.yRot = state.mastYaw * Mth.DEG_TO_RAD;
        this.head.xRot = state.mastPitch * Mth.DEG_TO_RAD;
    }
}
