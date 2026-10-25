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

import com.almuradev.sgcraft.mod.client.ClientPlatform;
import com.almuradev.sgcraft.mod.entity.JumperHull;
import com.almuradev.sgcraft.mod.entity.JumperInput;
import com.almuradev.sgcraft.mod.entity.JumperPayloads;
import com.almuradev.sgcraft.mod.entity.JumperShelter;
import com.almuradev.sgcraft.mod.entity.PuddleJumperEntity;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The keys a puddle jumper's pilot flies it with, read on their own client, where it is flown: forward and back,
 * left and right, jump to climb and {@link JumperKeys#DESCEND} to descend, all as the player has them bound, and
 * sprint for the turbo. The turbo and the headlights are the server's to work, so the pilot's client tells it when the
 * pilot wants the drive pods out for the turbo, when they fold them in or let them out, and when they switch the
 * headlights. Only clients call this.
 */
public final class JumperControls {

    // How long a rider must be out of their seat before the view comes back to their own eyes: long enough that
    // going through a gate into another world, which takes them out of their seat and puts them back, doesn't
    private static final int AWAY_TICKS = 10;
    // How long, if the jumper they left is gone from their client, as a gate's jump can take it for a moment, before
    // it comes back with them in their seat; till then the view stays as it was, third person and all
    private static final int GONE_TICKS = 200;
    // How far into a jumper's roof a player standing on it may have sunk, as it rises under them, and still be stood
    // back on top, in blocks
    private static final double ROOF_SINK = 0.7;
    // The longest a rider out of their seat is held waiting for the world round them, in ticks
    private static final int HOLD_TICKS = 200;

    // The field of view third person from a seat widens to, in degrees, if the player's own is narrower, and how much of
    // the way it goes each tick
    private static final float WIDE_FOV = 100;
    private static final float WIDEN_STEP = 0.1F;

    // Whether this client last told the server it wants the drive pods out
    private static boolean podsWanted;
    // The jumper this client flew last, by its entity's id
    private static int flown = -1;
    // The target this client last told the server its player locked, by its entity's id
    private static int sharedTarget = -1;
    // Whether this client's player sat in a jumper's seat, and for how many ticks they haven't since
    private static boolean rode;
    private static int awayTicks;
    // The jumper this client's player last sat in
    private static @Nullable PuddleJumperEntity lastRidden;
    // How this client's player last saw from a jumper's seat, first or third person, which they see by again the
    // next time they sit in one; none until they first have, this time the game runs
    private static @Nullable CameraType seatedView;
    // Where this client's player is held, out of a jumper over a world that hasn't reached them, and for how long
    private static @Nullable Vec3 heldAt;
    private static int heldTicks;
    // How far the view has widened for third person from a seat, from 0 to 1, last tick and this
    private static float wideO;
    private static float wide;

    private JumperControls() {
    }

    /**
     * Each tick: a player sitting down in a jumper sees as they last did from a seat in one, and one who has left a
     * jumper's seat sees from their own eyes again, out of third person; and a
     * player standing on a jumper's roof as it rises is stood back on top of it, rather than left to sink through it.
     */
    public static void tickClient(final Minecraft minecraft) {
        final LocalPlayer player = minecraft.player;
        final boolean seated = player != null && player.getVehicle() instanceof PuddleJumperEntity;
        JumperControls.wideO = JumperControls.wide;
        JumperControls.wide = Mth.approach(JumperControls.wide,
                seated && !minecraft.options.getCameraType().isFirstPerson() ? 1 : 0, JumperControls.WIDEN_STEP);
        if (player != null && player.getVehicle() instanceof final PuddleJumperEntity jumper) {
            if (!JumperControls.rode && JumperControls.seatedView != null) {
                minecraft.options.setCameraType(JumperControls.seatedView);
            }
            JumperControls.seatedView = minecraft.options.getCameraType();
            // The computer's key, from either front seat; the server says if the seat may work it
            while (JumperKeys.COMPUTER.consumeClick()) {
                ClientPlatform.get().sendToServer(new JumperPayloads.Computer());
            }
            JumperControls.weapons(player, jumper);
            JumperControls.rode = true;
            JumperControls.lastRidden = jumper;
            JumperControls.awayTicks = 0;
            JumperControls.heldAt = null;
            return;
        }
        if (player != null && JumperControls.hold(player)) {
            return;
        }
        if (player != null && !player.isSpectator() && !player.getAbilities().flying) {
            JumperControls.standOnRoof(player);
        }
        final PuddleJumperEntity left = JumperControls.lastRidden;
        final boolean gone = left == null || left.isRemoved() || player == null || left.level() != player.level();
        if (JumperControls.rode && ++JumperControls.awayTicks >= (gone ? JumperControls.GONE_TICKS
                : JumperControls.AWAY_TICKS)) {
            JumperControls.rode = false;
            JumperControls.lastRidden = null;
            if (!minecraft.options.getCameraType().isFirstPerson()) {
                minecraft.options.setCameraType(CameraType.FIRST_PERSON);
            }
        }
    }

    /**
     * @param fov the field of view the game would use, the player's own
     * @return that, widened while the player sees from third person in a jumper's seat, so the craft fills less of the
     *     view; their own again once they get up or look from inside, as it is only worked out here, never saved
     */
    public static float fov(final float fov, final float partialTicks) {
        return Mth.lerp(Mth.lerp(partialTicks, JumperControls.wideO, JumperControls.wide), fov,
                Math.max(fov, JumperControls.WIDE_FOV));
    }

    // A rider taken out of their seat, as their client loses sight of the jumper for a moment after a long jump through
    // a gate, before the world there has reached it, is held where they are rather than left to fall through the
    // empty world, until they sit again, the world under them arrives, or a few seconds pass
    private static boolean hold(final LocalPlayer player) {
        final boolean arrived = player.level().hasChunk(SectionPos.blockToSectionCoord(player.getX()),
                SectionPos.blockToSectionCoord(player.getZ()));
        if (JumperControls.heldAt == null) {
            if (!JumperControls.rode || JumperControls.awayTicks > 0 || arrived) {
                return false;
            }
            JumperControls.heldAt = player.position();
            JumperControls.heldTicks = 0;
        }
        if (arrived || ++JumperControls.heldTicks > JumperControls.HOLD_TICKS) {
            JumperControls.heldAt = null;
            return false;
        }
        player.setPos(JumperControls.heldAt);
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        return true;
    }

    // A jumper rising under a player on its roof lifts them with it: their feet, sunk into the roof, are put back on
    // top. The game moves nothing that stands on a moving thing, and the roof, risen past their feet, wouldn't hold
    // them up, so they would fall through it into the cabin
    private static void standOnRoof(final LocalPlayer player) {
        for (final PuddleJumperEntity jumper : JumperShelter.near(player.level(), player.position(), 1)) {
            final double top = jumper.getY() + PuddleJumperEntity.HEIGHT;
            final double feet = player.getY();
            if (feet >= top || feet < top - JumperControls.ROOF_SINK || jumper.getY() <= jumper.yo) {
                continue;
            }
            final Vec3 local = JumperHull.toLocal(player.position().subtract(jumper.position()), jumper.getYRot());
            if (Math.abs(local.x) < PuddleJumperEntity.WIDTH / 2.0 && Math.abs(local.z) < PuddleJumperEntity.LENGTH / 2
                    && !jumper.inside(player.getEyePosition())) {
                player.setPos(player.getX(), top, player.getZ());
                player.setDeltaMovement(player.getDeltaMovement().x, Math.max(0, player.getDeltaMovement().y),
                        player.getDeltaMovement().z);
                player.setOnGround(true);
                player.resetFallDistance();
                return;
            }
        }
    }

    // The weapons' keys, for its weapons' operator: the co-pilot if anyone sits there, or else the pilot. The bays
    // swung out or in, friendly fire let or stopped, the next target locked, and a drone launched at the one locked,
    // or straight ahead with none
    private static void weapons(final LocalPlayer player, final PuddleJumperEntity jumper) {
        if (jumper.weaponsOperator() != player) {
            JumperTargets.clear();
            JumperControls.sharedTarget = Integer.MIN_VALUE;
            for (final KeyMapping key : new KeyMapping[] {JumperKeys.ARM, JumperKeys.FRIENDLY_FIRE, JumperKeys.TARGET,
                    JumperKeys.FIRE}) {
                // Not theirs to work: their presses are let go
                while (key.consumeClick()) {
                    JumperTargets.clear();
                }
            }
            return;
        }
        // The targeting runs only while the drones are armed: disarmed, nothing is marked or locked
        if (jumper.armed()) {
            JumperTargets.tick(player, jumper);
        } else {
            JumperTargets.clear();
        }
        while (JumperKeys.ARM.consumeClick()) {
            ClientPlatform.get().sendToServer(new JumperPayloads.Press(PuddleJumperEntity.Button.WEAPONS));
        }
        while (JumperKeys.FRIENDLY_FIRE.consumeClick()) {
            ClientPlatform.get().sendToServer(new JumperPayloads.Press(PuddleJumperEntity.Button.FRIENDLY_FIRE));
        }
        while (JumperKeys.TARGET.consumeClick()) {
            if (jumper.armed()) {
                JumperTargets.cycle(player, jumper);
            }
        }
        while (JumperKeys.FIRE.consumeClick()) {
            final Entity target = JumperTargets.locked(jumper);
            ClientPlatform.get().sendToServer(new JumperPayloads.Fire(target == null ? -1 : target.getId()));
        }
        // The lock, told the server as it changes, for the rider beside them to see
        final Entity locked = JumperTargets.locked(jumper);
        final int shared = locked == null ? -1 : locked.getId();
        if (shared != JumperControls.sharedTarget) {
            JumperControls.sharedTarget = shared;
            ClientPlatform.get().sendToServer(new JumperPayloads.Target(shared));
        }
    }

    /** @return whether this client's player is inside a jumper, seated or standing */
    public static boolean viewerInside(final PuddleJumperEntity jumper) {
        final LocalPlayer player = Minecraft.getInstance().player;
        return player != null && (player.getVehicle() == jumper || jumper.inside(player.getEyePosition()));
    }

    /** @return the controls the pilot holds, or none if the pilot isn't this client's player */
    public static JumperInput input(final PuddleJumperEntity jumper, final Entity pilot) {
        if (!(pilot instanceof final LocalPlayer player)) {
            return JumperInput.NONE;
        }
        final Input keys = player.input.keyPresses;
        final boolean turbo = keys.sprint() && keys.forward();
        // At another jumper's helm, the server knows nothing yet of what this client wants
        if (jumper.getId() != JumperControls.flown) {
            JumperControls.flown = jumper.getId();
            JumperControls.podsWanted = !turbo;
        }
        if (turbo != JumperControls.podsWanted) {
            JumperControls.podsWanted = turbo;
            ClientPlatform.get().sendToServer(new JumperPayloads.Pods(turbo));
        }
        while (JumperKeys.HEADLIGHTS.consumeClick()) {
            ClientPlatform.get().sendToServer(new JumperPayloads.Press(PuddleJumperEntity.Button.HEADLIGHTS));
        }
        while (JumperKeys.POWER.consumeClick()) {
            ClientPlatform.get().sendToServer(new JumperPayloads.Press(PuddleJumperEntity.Button.POWER));
        }
        while (JumperKeys.PODS.consumeClick()) {
            ClientPlatform.get().sendToServer(new JumperPayloads.Press(PuddleJumperEntity.Button.PODS));
        }
        while (JumperKeys.DOOR.consumeClick()) {
            ClientPlatform.get().sendToServer(new JumperPayloads.Press(PuddleJumperEntity.Button.DOOR));
        }
        while (JumperKeys.SHIELD.consumeClick()) {
            ClientPlatform.get().sendToServer(new JumperPayloads.Press(PuddleJumperEntity.Button.SHIELD));
        }
        while (JumperKeys.CLOAK.consumeClick()) {
            ClientPlatform.get().sendToServer(new JumperPayloads.Press(PuddleJumperEntity.Button.CLOAK));
        }

        return new JumperInput(keys.forward(), keys.backward(), keys.left(), keys.right(), keys.jump(),
                JumperKeys.DESCEND.isDown(), turbo);
    }
}
