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

import com.almuradev.sgcraft.mod.malp.MalpLinks;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.Use;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Pose;

/**
 * How a player at a MALP console or looking at a MALP tablet is drawn. At a console: body and head turned to the
 * console, head tilted down at it, and both arms reaching forward onto its controls. At a tablet: body turned the way
 * the head faces, head tilted further down, and both arms holding the tablet out in front of the chest. Only a player
 * standing or crouching is posed, so one asleep, swimming or gliding is drawn as usual.
 */
public final class MalpConsolePose {

    // How far down, in degrees, a player looks at a tablet held in front of them
    private static final float TABLET_TILT = 40;
    // How far forward the arms reach, in radians below level; at a console the hands rest on its top, in front of the
    // player's waist, and with a tablet they hold it up in front of the chest
    private static final float CONSOLE_REACH = -1.1F;
    private static final float TABLET_REACH = -0.95F;
    // How far in each arm turns, toward the other, in radians; a tablet is held in both hands close together
    private static final float CONSOLE_IN = 0.2F;
    private static final float TABLET_IN = 0.45F;

    private MalpConsolePose() {
    }

    /**
     * Turns the body to face where the head does, and tilts the head down, if the player is at a console or looking
     * at a tablet.
     */
    public static void extract(final AvatarRenderState state) {
        final Use use = MalpConsolePose.use(state);
        if (use == Use.NONE) {
            return;
        }
        state.bodyRot += state.yRot;
        state.yRot = 0;
        state.xRot = use == Use.TABLET ? MalpConsolePose.TABLET_TILT : MalpLinks.CONSOLE_TILT;
        state.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        state.rightArmPose = HumanoidModel.ArmPose.EMPTY;
    }

    /**
     * Puts both arms forward, onto a console's controls or around a tablet, once the model has set its own pose, if
     * the player is at a console or looking at a tablet.
     */
    public static void arms(final PlayerModel model, final AvatarRenderState state) {
        final Use use = MalpConsolePose.use(state);
        if (use == Use.NONE) {
            return;
        }
        final float reach = use == Use.TABLET ? MalpConsolePose.TABLET_REACH : MalpConsolePose.CONSOLE_REACH;
        final float in = use == Use.TABLET ? MalpConsolePose.TABLET_IN : MalpConsolePose.CONSOLE_IN;
        model.rightArm.xRot = reach;
        model.rightArm.yRot = -in;
        model.rightArm.zRot = 0;
        model.leftArm.xRot = reach;
        model.leftArm.yRot = in;
        model.leftArm.zRot = 0;
    }

    private static Use use(final AvatarRenderState state) {
        return state.pose == Pose.STANDING || state.pose == Pose.CROUCHING ? MalpConsoleUsers.use(state.id)
                : Use.NONE;
    }
}
