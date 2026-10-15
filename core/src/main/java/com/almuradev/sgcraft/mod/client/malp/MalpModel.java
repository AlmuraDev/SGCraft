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
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The MALP, after the show's Mark II: a low, boxy chassis on six knobby tires, three a side, with sloped panels in
 * hazard stripes front and back and diamond plate between them. A camera on a mast pans and tilts in the middle; a
 * dish antenna on a short post leans up and back from a rear corner; a jointed grapple arm reaches forward over the
 * front to a small claw; and two headlights sit above the front bumper.
 *
 * <p>Sizes are in pixels, a sixteenth of a block. The model stands on y = 0 with up as negative y and its front
 * towards negative z, as entity models do. Its texture is 128 by 128.</p>
 */
public final class MalpModel extends EntityModel<MalpRenderState> {

    /** The MALP's model layer. */
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(SGCraft.id("malp"), "main");
    /** The layer of the headlights' glow, drawn over the headlights when the lamp is on. */
    public static final ModelLayerLocation GLOW_LAYER = new ModelLayerLocation(SGCraft.id("malp"), "glow");

    private static final int TEXTURE_SIZE = 128;
    // The tires' middles: how far out from the middle, how high, and how far along
    private static final float WHEEL_X = 7.5F;
    private static final float WHEEL_Y = -4;
    private static final float[] WHEEL_Z = {-8, 0, 8};
    // The slopes fall three pixels over five
    private static final float SLOPE = (float) Math.atan2(3, 5);
    // Each tread bar spans the tire, its ends two opposite sides of a twelve sided tread whose flats are four pixels
    // from the axle; each hub bar makes two sides of an eight sided hub, its flats two pixels from the axle, standing
    // a quarter pixel proud of the tread on each side
    private static final int TREAD_BARS = 6;
    private static final CubeDeformation TREAD_WIDTH = new CubeDeformation(0, 0,
            4 * (float) Math.tan(Math.PI / 12) - 1);
    private static final int HUB_BARS = 4;
    private static final CubeDeformation HUB_WIDTH = new CubeDeformation(0.25F, 0,
            2 * (float) Math.tan(Math.PI / 8) - 1);
    private static final CubeDeformation CAP_SIZE = new CubeDeformation(0.4F, 0.25F, 0.25F);
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
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("chassis", CubeListBuilder.create().texOffs(0, 0).addBox(-6, -9, -11, 12, 5, 22),
                PartPose.ZERO);
        root.addOrReplaceChild("deck", CubeListBuilder.create().texOffs(0, 27).addBox(-6, -12, -6, 12, 3, 12),
                PartPose.ZERO);
        root.addOrReplaceChild("front_slope", CubeListBuilder.create().texOffs(48, 27).addBox(-6, 0, -6, 12, 1, 6),
                PartPose.offsetAndRotation(0, -12, -6, MalpModel.SLOPE, 0, 0));
        final PartDefinition rearSlope = root.addOrReplaceChild("rear_slope", CubeListBuilder.create()
                .texOffs(48, 34).addBox(-6, 0, 0, 12, 1, 6), PartPose.offsetAndRotation(0, -12, 6, -MalpModel.SLOPE,
                0, 0));
        rearSlope.addOrReplaceChild("rear_light", CubeListBuilder.create().texOffs(108, 27).addBox(-1.5F, -1, 2, 3, 1,
                2), PartPose.ZERO);
        root.addOrReplaceChild("front_bumper", CubeListBuilder.create().texOffs(66, 42).addBox(-6, -6, -12, 12, 2, 1),
                PartPose.ZERO);
        root.addOrReplaceChild("rear_bumper", CubeListBuilder.create().texOffs(66, 42).addBox(-6, -6, 11, 12, 2, 1),
                PartPose.ZERO);
        MalpModel.addHeadlights(root);
        for (int i = 0; i < MalpModel.WHEELS.length; i++) {
            final float x = i < 3 ? MalpModel.WHEEL_X : -MalpModel.WHEEL_X;
            MalpModel.addWheel(root.addOrReplaceChild(MalpModel.WHEELS[i], CubeListBuilder.create(),
                    PartPose.offset(x, MalpModel.WHEEL_Y, MalpModel.WHEEL_Z[i % 3])));
        }
        // The camera: a post that pans, and a head on it that tilts, its lens looking ahead
        final PartDefinition mast = root.addOrReplaceChild("mast", CubeListBuilder.create().texOffs(110, 0)
                .addBox(-1, -5, -1, 2, 5, 2), PartPose.offset(0, -12, 1));
        mast.addOrReplaceChild("head", CubeListBuilder.create().texOffs(90, 0).addBox(-2.5F, -1.5F, -2.5F, 5, 3, 5)
                .texOffs(118, 0).addBox(-1, -1, -3.5F, 2, 2, 1), PartPose.offset(0, -6.5F, 0));
        // The dish, half a block across, its face leaning up and back
        final PartDefinition dishPost = root.addOrReplaceChild("dish_post", CubeListBuilder.create().texOffs(124, 0)
                .addBox(-0.5F, -3, -0.5F, 1, 3, 1), PartPose.offset(4, -12, 8));
        dishPost.addOrReplaceChild("dish", CubeListBuilder.create().texOffs(84, 27).addBox(-4, -4, -0.5F, 8, 8, 1)
                .texOffs(102, 27).addBox(-0.5F, -0.5F, 0.5F, 1, 1, 2), PartPose.offsetAndRotation(0, -3, 0,
                50 * Mth.DEG_TO_RAD, 0, 0));
        // The grapple arm: a base at the back, a long upper arm forward, a forearm bending down over the front, and
        // the claw's two prongs
        final PartDefinition arm = root.addOrReplaceChild("arm", CubeListBuilder.create().texOffs(46, 42)
                .addBox(-1.5F, -2, -1.5F, 3, 2, 3), PartPose.offset(-3, -12, 4));
        final PartDefinition upper = arm.addOrReplaceChild("upper_arm", CubeListBuilder.create().texOffs(0, 42)
                .addBox(-1, -1, -11, 2, 2, 11), PartPose.offsetAndRotation(0, -2, 0, -0.15F, 0, 0));
        final PartDefinition forearm = upper.addOrReplaceChild("forearm", CubeListBuilder.create().texOffs(26, 42)
                .addBox(-1, -1, -8, 2, 2, 8), PartPose.offsetAndRotation(0, 0, -11, 0.9F, 0, 0));
        forearm.addOrReplaceChild("claw_left", CubeListBuilder.create().texOffs(58, 42).addBox(-0.5F, -0.5F, -3, 1,
                1, 3), PartPose.offsetAndRotation(0.75F, 0, -8, 0, -0.35F, 0));
        forearm.addOrReplaceChild("claw_right", CubeListBuilder.create().texOffs(58, 42).addBox(-0.5F, -0.5F, -3, 1,
                1, 3), PartPose.offsetAndRotation(-0.75F, 0, -8, 0, 0.35F, 0));
        return LayerDefinition.create(mesh, MalpModel.TEXTURE_SIZE, MalpModel.TEXTURE_SIZE);
    }

    // A tire, half a block across, round its axle, which runs across the MALP: six bars through the middle, each
    // turned 30 degrees on from the last, whose ends make a twelve sided tread; and on each side an eight sided gray
    // hub of four bars, with a dark cap on the axle
    private static void addWheel(final PartDefinition wheel) {
        for (int i = 0; i < MalpModel.TREAD_BARS; i++) {
            wheel.addOrReplaceChild("tread_" + i, CubeListBuilder.create().texOffs(68, 0).addBox(-1.5F, -4, -1, 3, 8, 2,
                    MalpModel.TREAD_WIDTH), PartPose.rotation(i * Mth.PI / MalpModel.TREAD_BARS, 0, 0));
        }
        for (int i = 0; i < MalpModel.HUB_BARS; i++) {
            wheel.addOrReplaceChild("hub_" + i, CubeListBuilder.create().texOffs(78, 0).addBox(-1.5F, -2, -1, 3, 4, 2,
                    MalpModel.HUB_WIDTH), PartPose.rotation(i * Mth.PI / MalpModel.HUB_BARS, 0, 0));
        }
        wheel.addOrReplaceChild("cap", CubeListBuilder.create().texOffs(68, 10).addBox(-1.5F, -0.5F, -0.5F, 3, 1, 1,
                MalpModel.CAP_SIZE), PartPose.ZERO);
    }

    /** @return the headlights alone, to glow over the model's own when the lamp is on */
    public static LayerDefinition createGlowLayer() {
        final MeshDefinition mesh = new MeshDefinition();
        MalpModel.addHeadlights(mesh.getRoot());
        return LayerDefinition.create(mesh, MalpModel.TEXTURE_SIZE, MalpModel.TEXTURE_SIZE);
    }

    private static void addHeadlights(final PartDefinition root) {
        root.addOrReplaceChild("headlights", CubeListBuilder.create().texOffs(118, 27).addBox(-5, -9, -12, 2, 2, 1)
                .texOffs(118, 27).addBox(3, -9, -12, 2, 2, 1), PartPose.ZERO);
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
