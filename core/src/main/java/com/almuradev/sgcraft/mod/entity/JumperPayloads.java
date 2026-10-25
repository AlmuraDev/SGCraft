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

package com.almuradev.sgcraft.mod.entity;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.network.SGPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * What a puddle jumper's pilot tells the server beside how the jumper flies, which their client sends as it would a
 * boat's: whether they want the drive pods out, for the turbo, and the dashboard's buttons pressed by their keys. The
 * server takes these only from the player at the helm; and the computer's key, from either front seat. The server
 * tells clients near a jumper where its shield was struck.
 */
public final class JumperPayloads {

    private JumperPayloads() {
    }

    /**
     * The pilot wants the drive pods out, holding the turbo, or in again.
     *
     * @param out whether they want the pods out
     */
    public record Pods(boolean out) implements SGPacket {

        public static final Type<Pods> TYPE = new Type<>(SGCraft.id("jumper_pods"));

        public Pods(final FriendlyByteBuf buf) {
            this(buf.readBoolean());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeBoolean(this.out);
        }

        @Override
        public Type<Pods> type() {
            return Pods.TYPE;
        }

        /** Puts the pilot's jumper's pods out, or in. */
        public static void handle(final ServerPlayer player, final Pods pods) {
            if (player.getVehicle() instanceof final PuddleJumperEntity jumper && jumper.getControllingPassenger()
                    == player) {
                jumper.wantPods(pods.out);
            }
        }
    }

    /**
     * The pilot launched a drone at the target they locked.
     *
     * @param target the target's entity id
     */
    public record Fire(int target) implements SGPacket {

        public static final Type<Fire> TYPE = new Type<>(SGCraft.id("jumper_fire"));

        public Fire(final FriendlyByteBuf buf) {
            this(buf.readVarInt());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeVarInt(this.target);
        }

        @Override
        public Type<Fire> type() {
            return Fire.TYPE;
        }

        /** Launches a drone from the pilot's jumper at the target, if it may. */
        public static void handle(final ServerPlayer player, final Fire fire) {
            if (player.getVehicle() instanceof final PuddleJumperEntity jumper) {
                jumper.launchDrone(player, fire.target);
            }
        }
    }

    /**
     * The weapons' operator locked a target, or let it go, for the riders beside them to see.
     *
     * @param target the target's entity id, or -1 for none
     */
    public record Target(int target) implements SGPacket {

        public static final Type<Target> TYPE = new Type<>(SGCraft.id("jumper_target"));

        public Target(final FriendlyByteBuf buf) {
            this(buf.readVarInt());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeVarInt(this.target);
        }

        @Override
        public Type<Target> type() {
            return Target.TYPE;
        }

        /** Shares the operator's lock with their jumper's riders. */
        public static void handle(final ServerPlayer player, final Target target) {
            if (player.getVehicle() instanceof final PuddleJumperEntity jumper) {
                jumper.shareTarget(player, target.target);
            }
        }
    }

    /**
     * A jumper's shield stopped a blow, which the clients near it show where it fell.
     *
     * @param jumper the jumper's entity id
     * @param way which way from the shield's middle it was struck, in the jumper's own frame, as a unit vector
     * @param strength how hard, from a glancing 0.25 to 1
     * @param collapsed whether the blow emptied the generator, knocking the shield down
     */
    public record ShieldHit(int jumper, Vector3fc way, float strength, boolean collapsed) implements SGPacket {

        public static final Type<ShieldHit> TYPE = new Type<>(SGCraft.id("jumper_shield_hit"));

        public ShieldHit(final FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readVector3f(), buf.readFloat(), buf.readBoolean());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeVarInt(this.jumper);
            buf.writeVector3f(new Vector3f(this.way));
            buf.writeFloat(this.strength);
            buf.writeBoolean(this.collapsed);
        }

        @Override
        public Type<ShieldHit> type() {
            return ShieldHit.TYPE;
        }
    }

    /** A rider in the front seats pressed the computer's key, to open the computer aboard. */
    public record Computer() implements SGPacket {

        public static final Type<Computer> TYPE = new Type<>(SGCraft.id("jumper_computer"));

        public Computer(final FriendlyByteBuf buf) {
            this();
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
        }

        @Override
        public Type<Computer> type() {
            return Computer.TYPE;
        }

        /** Opens the computer aboard the rider's jumper, if their seat may work it. */
        public static void handle(final ServerPlayer player, final Computer computer) {
            if (player.getVehicle() instanceof final PuddleJumperEntity jumper) {
                jumper.openComputer(player);
            }
        }
    }

    /**
     * The pilot pressed one of the dashboard's buttons by its key.
     *
     * @param button the button
     */
    public record Press(PuddleJumperEntity.Button button) implements SGPacket {

        public static final Type<Press> TYPE = new Type<>(SGCraft.id("jumper_press"));

        public Press(final FriendlyByteBuf buf) {
            this(buf.readEnum(PuddleJumperEntity.Button.class));
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeEnum(this.button);
        }

        @Override
        public Type<Press> type() {
            return Press.TYPE;
        }

        /** Presses the button on the pilot's jumper. */
        public static void handle(final ServerPlayer player, final Press press) {
            if (player.getVehicle() instanceof final PuddleJumperEntity jumper) {
                jumper.press(player, press.button);
            }
        }
    }
}
