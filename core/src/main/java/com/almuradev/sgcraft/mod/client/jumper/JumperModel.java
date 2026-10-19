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

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.client.model.CubeModels;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The puddle jumper, after the show's gate ship: a long tube, straight sided under a broad arch, its nose and its tail
 * raked and set in behind a lip, in blue gray gunmetal plating, raised panels with grooves and slots cut between them
 * as the Ancients' are, over a gray underside; a broad windshield in a bronze rim in the face of its nose, with two
 * headlamps under it; a cargo door filling its back, with a small window, which lowers to the ground as a ramp; and in
 * the lower sides of its rear half the drive pods, fins of the hull carrying ribbed engines, their backs grids of blue
 * lights, which a pair of arms push out for flight and draw back in against the hull, the fins tipping down past flat
 * once out so the engines ride on top. Inside, dark bronze cut by angular grooves,
 * with lamps along the walls and frosted panels lit from behind, are the dashboard, with the pilot's hand controls and
 * the computer's screen, the center console with the dialer's orange tiles, four gray padded chairs, the bulkhead with
 * its doorway and its two consoles, and a bench of three seats along each wall of the rear compartment.
 *
 * <p>It is drawn in three layers on one texture, each this model: the solid hull, the windows, which are see-through,
 * and the lights, which glow in the dark, the headlamps among them only while the headlights are on. Each layer moves
 * whichever of the cargo door, the pods, their arms and the headlamps it has. Its cubes, in
 * {@code entity_models/puddle_jumper.json}, and its texture both come from {@code tools/jumper/make_jumper_art.py}.
 * Sizes are in pixels, a sixteenth of a block; the model stands on y = 0 with up as negative y, its nose towards
 * negative z and its left towards positive x.</p>
 */
public final class JumperModel extends EntityModel<JumperRenderState> {

    /** The hull. */
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(SGCraft.id("puddle_jumper"), "main");
    /** The windows. */
    public static final ModelLayerLocation GLASS_LAYER = new ModelLayerLocation(SGCraft.id("puddle_jumper"), "glass");
    /** The lights. */
    public static final ModelLayerLocation GLOW_LAYER = new ModelLayerLocation(SGCraft.id("puddle_jumper"), "glow");

    private static final String MODEL = "/assets/sgcraft/entity_models/puddle_jumper.json";
    // How far the cargo door turns about its hinge, shut against the hull and lowered to the ground, in radians
    private static final float DOOR_SHUT = (float) Math.atan2(10, 40);
    private static final float DOOR_LOWERED = -1.52F;
    // The drive pods, as the show's, each an arm curving out of the hull's side and up, its engine standing on the end
    // of the curve leaning in towards the hull, as tools/jumper/make_jumper_art.py builds them. Going out, the door
    // over the pod's recess swings down out of the way, the arm slides out of the hull, and the engine rises off the
    // arm's end; folding, the engine tips down to lie facing the hull, the arm draws back in, and the door shuts. How
    // far the door swings, in radians; how far the arm slides, in pixels; how far the engine leans in out and lies
    // in folded, in radians from upright; where the arm leaves the hull, and where its engine stands, out, in pixels
    // from the middle and the underside, the left one's; and where each push arm's base is, at the back of the recess
    private static final float DOOR_SWING = (float) Math.toRadians(95);
    private static final float SLIDE = 17;
    private static final float ENGINE_OUT = (float) Math.toRadians(-22.5);
    private static final float ENGINE_IN = (float) Math.toRadians(-90);
    private static final float ARM_LEAVES_SIDE = 22;
    private static final float ARM_LEAVES_HEIGHT = -8;
    private static final float ENGINE_SIDE = 39;
    private static final float ENGINE_HEIGHT = -19;
    private static final float ARM_SIDE = 15;
    private static final float ARM_HEIGHT = -12;
    // The drone bays, each a smaller curved arm on a lower side of the front cabin, the drones lying in its curve: how
    // far it slides out of the hull as the drones are armed, in pixels
    private static final float BAY_SLIDE = 9;

    private final @Nullable ModelPart door;
    private final @Nullable ModelPart leftPod;
    private final @Nullable ModelPart rightPod;
    private final @Nullable ModelPart headlamps;
    private final List<ModelPart> leftArms = new ArrayList<>();
    private final List<ModelPart> rightArms = new ArrayList<>();
    private final @Nullable ModelPart leftEngine;
    private final @Nullable ModelPart rightEngine;
    private final List<ModelPart> leftArc = new ArrayList<>();
    private final List<ModelPart> rightArc = new ArrayList<>();
    private final List<ModelPart> leftBay = new ArrayList<>();
    private final List<ModelPart> rightBay = new ArrayList<>();
    private final List<ModelPart> leftDrones = new ArrayList<>();
    private final List<ModelPart> rightDrones = new ArrayList<>();

    public JumperModel(final ModelPart root, final Function<Identifier, RenderType> renderType) {
        super(root, renderType);
        this.door = JumperModel.part(root, "cargo_door");
        this.leftPod = JumperModel.part(root, "left_pod");
        this.rightPod = JumperModel.part(root, "right_pod");
        this.headlamps = JumperModel.part(root, "headlamps");
        for (int i = 0; root.hasChild("left_arm_" + i); i++) {
            this.leftArms.add(root.getChild("left_arm_" + i));
            this.rightArms.add(root.getChild("right_arm_" + i));
        }
        this.leftEngine = JumperModel.part(root, "left_engine");
        this.rightEngine = JumperModel.part(root, "right_engine");
        for (int i = 0; root.hasChild("left_arc_" + i); i++) {
            this.leftArc.add(root.getChild("left_arc_" + i));
            this.rightArc.add(root.getChild("right_arc_" + i));
        }
        if (root.hasChild("left_arc_run")) {
            this.leftArc.add(root.getChild("left_arc_run"));
            this.rightArc.add(root.getChild("right_arc_run"));
        }
        for (int i = 0; root.hasChild("left_bay_" + i); i++) {
            this.leftBay.add(root.getChild("left_bay_" + i));
            this.rightBay.add(root.getChild("right_bay_" + i));
        }
        if (root.hasChild("left_bay_run")) {
            this.leftBay.add(root.getChild("left_bay_run"));
            this.rightBay.add(root.getChild("right_bay_run"));
        }
        for (int i = 0; root.hasChild("left_drone_" + i); i++) {
            this.leftDrones.add(root.getChild("left_drone_" + i));
            this.rightDrones.add(root.getChild("right_drone_" + i));
        }
    }

    private static float smooth(final float t) {
        final float clamped = Mth.clamp(t, 0, 1);
        return clamped * clamped * (3 - 2 * clamped);
    }

    // How far, from 0 to 1, as the pods go out from 0 to 1: the doors swing open, the arms slide out, the engines rise
    private static float door(final float pods) {
        return JumperModel.smooth(pods / 0.3F);
    }

    private static float slide(final float pods) {
        return JumperModel.smooth((pods - 0.2F) / 0.5F);
    }

    private static float rise(final float pods) {
        return JumperModel.smooth((pods - 0.65F) / 0.35F);
    }

    // How far a pod's arm is from out, across, in pixels, drawn in towards the hull
    private static float drawnIn(final boolean left, final float pods) {
        return -(1 - JumperModel.slide(pods)) * JumperModel.SLIDE * (left ? 1 : -1);
    }

    // How far a pod's engine leans from upright, in radians, in towards the hull
    private static float lean(final boolean left, final float pods) {
        return Mth.lerp(JumperModel.rise(pods), JumperModel.ENGINE_IN, JumperModel.ENGINE_OUT) * (left ? 1 : -1);
    }

    /**
     * Where the middle of a drive pod's engine's back is, as the pod goes out or folds.
     *
     * @param left whether it is the left pod
     * @param pods how far out the pods are, from 0 to 1
     * @param along how far up the engine from its base its back's middle is, in pixels
     * @return the point, x and y, in pixels in the model's space
     */
    public static float[] engineBack(final boolean left, final float pods, final float along) {
        final float side = left ? 1 : -1;
        final float angle = JumperModel.lean(left, pods);
        return new float[] {
            JumperModel.ENGINE_SIDE * side + JumperModel.drawnIn(left, pods) + along * Mth.sin(angle),
            JumperModel.ENGINE_HEIGHT - along * Mth.cos(angle)
        };
    }

    private static @Nullable ModelPart part(final ModelPart root, final String name) {
        return root.hasChild(name) ? root.getChild(name) : null;
    }

    @Override
    public void setupAnim(final JumperRenderState state) {
        super.setupAnim(state);
        if (this.door != null) {
            this.door.xRot = Mth.lerp(state.hatch, JumperModel.DOOR_SHUT, JumperModel.DOOR_LOWERED);
        }
        // The pods: their doors, their arms and engines; and the push arms, stretched from the backs of the recesses
        // to the pods' arms
        final boolean out = JumperModel.slide(state.pods) > 0.02F;
        if (this.leftPod != null) {
            this.leftPod.zRot = JumperModel.door(state.pods) * JumperModel.DOOR_SWING;
        }
        if (this.rightPod != null) {
            this.rightPod.zRot = -JumperModel.door(state.pods) * JumperModel.DOOR_SWING;
        }
        JumperModel.slideOut(this.leftArc, this.leftEngine, true, state, out);
        JumperModel.slideOut(this.rightArc, this.rightEngine, false, state, out);
        // The drone bays, slid out as the drones are armed, each with its share of the drones left in its curve
        JumperModel.slideBay(this.leftBay, this.leftDrones, true, state.bays, (state.drones + 1) / 2);
        JumperModel.slideBay(this.rightBay, this.rightDrones, false, state.bays, state.drones / 2);
        JumperModel.reach(this.leftArms, true, state.pods, out);
        JumperModel.reach(this.rightArms, false, state.pods, out);
        if (this.headlamps != null) {
            this.headlamps.visible = state.headlights;
        }
    }

    // A drone bay's arm and its drones slid out of the hull, or drawn back in, as one; only as many drones as it
    // holds show, and only once it is all the way out
    private static void slideBay(final List<ModelPart> bay, final List<ModelPart> drones, final boolean left,
            final float bays, final int held) {
        final float shift = -(1 - JumperModel.smooth(bays)) * JumperModel.BAY_SLIDE * (left ? 1 : -1);
        for (final ModelPart segment : bay) {
            segment.x += shift;
            segment.visible = bays > 0.02F;
        }
        for (int i = 0; i < drones.size(); i++) {
            final ModelPart drone = drones.get(i);
            drone.x += shift;
            drone.visible = bays > 0.9F && i < held;
        }
    }

    // A pod's arm and engine slid out of the hull, or drawn back in, as one, the engine leaning as it rises or lies
    // down
    private static void slideOut(final List<ModelPart> arc, final @Nullable ModelPart engine, final boolean left,
            final JumperRenderState state, final boolean out) {
        final float shift = JumperModel.drawnIn(left, state.pods);
        for (final ModelPart segment : arc) {
            segment.x += shift;
            segment.visible = out;
        }
        if (engine != null) {
            engine.x += shift;
            engine.zRot = JumperModel.lean(left, state.pods);
            engine.visible = out;
        }
    }

    private static void reach(final List<ModelPart> arms, final boolean left, final float pods, final boolean out) {
        final float side = left ? 1 : -1;
        final float dx = JumperModel.ARM_LEAVES_SIDE * side + JumperModel.drawnIn(left, pods)
                - JumperModel.ARM_SIDE * side;
        final float dy = JumperModel.ARM_LEAVES_HEIGHT - JumperModel.ARM_HEIGHT;
        for (final ModelPart arm : arms) {
            arm.visible = out;
            arm.zRot = (float) (left ? Math.atan2(dy, dx) : Math.atan2(-dy, -dx));
            arm.xScale = (float) Math.max(0.01, Math.hypot(dx, dy));
        }
    }

    /** @return the hull, its seats and its dashboard */
    public static LayerDefinition createLayer() {
        return CubeModels.layer(JumperModel.MODEL, "main");
    }

    /** @return the windshield and the cargo door's window */
    public static LayerDefinition createGlassLayer() {
        return CubeModels.layer(JumperModel.MODEL, "glass");
    }

    /** @return the drive pods' engines, the cockpit's lit panels and buttons, the wall lamps and the headlamps */
    public static LayerDefinition createGlowLayer() {
        return CubeModels.layer(JumperModel.MODEL, "glow");
    }
}
