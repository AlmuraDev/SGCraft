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

import com.almuradev.sgcraft.mod.entity.JumperHull;
import com.almuradev.sgcraft.mod.entity.JumperPayloads;
import com.almuradev.sgcraft.mod.entity.PuddleJumperEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A puddle jumper's shield as its clients see it: never standing as a whole bubble, only rising, falling, and where
 * it is struck. Each blow lights a
 * patch of the shield's shell about the point it fell on, a bright flash fading as a ripple runs out from it across the
 * shell, wider the harder the blow; a blow that knocks the shield down flares across far more of it. The shell is an
 * egg shape round the hull, and the patches move with the jumper. Only clients call this.
 */
public final class JumperShield {

    // How long a blow's patch lasts, in ticks, and a knockdown's
    private static final float LIFE = 14;
    private static final float COLLAPSE_LIFE = 24;
    // How far the shell stands off the hull, round its width, height and length, in blocks
    private static final float AROUND = 0.6F;
    private static final float ACROSS = PuddleJumperEntity.WIDTH / 2 + JumperShield.AROUND;
    private static final float UP = PuddleJumperEntity.HEIGHT / 2 + JumperShield.AROUND;
    private static final float ALONG = (float) PuddleJumperEntity.LENGTH / 2 + JumperShield.AROUND;
    // How far round the shell a patch reaches, in radians, for the lightest blow and the hardest, and for a knockdown
    private static final float LIGHT_REACH = 0.55F;
    private static final float HARD_REACH = 1.1F;
    private static final float COLLAPSE_REACH = 1.9F;
    // How finely a patch is cut: rings out from its middle, and spokes round it
    private static final int RINGS = 14;
    private static final int SPOKES = 32;
    // The shield rising: how long it takes to spread over the whole shell and how long the shell then takes to fade, how
    // long falling takes, in ticks, how bright the shell glows behind the spreading edge, and how finely the shell is
    // cut, bands from its top to its foot and slices round it
    private static final float RISE_TICKS = 6;
    private static final float RISE_FADE = 8;
    private static final float FALL_TICKS = 6;
    private static final float SWEEP_GLOW = 0.28F;
    private static final int BANDS = 16;
    private static final int SLICES = 32;
    // The patch's colors: a pale core, the shield's blue, and a knockdown's whiter flare
    private static final int CORE = 0xE6F4FF;
    private static final int BLUE = 0x5CB4FF;
    private static final int FLARE = 0xC8E4FF;

    // The blows still showing, by the jumper's entity id, in the world they were seen in
    private static final Map<Integer, List<Hit>> HITS = new HashMap<>();
    private static @Nullable ClientLevel level;

    private JumperShield() {
    }

    // A blow: where on the unit sphere the shell is struck, how hard, whether it knocked the shield down, and when
    record Hit(Vector3fc at, float strength, boolean collapsed, long time) {

        float life() {
            return this.collapsed ? JumperShield.COLLAPSE_LIFE : JumperShield.LIFE;
        }
    }

    /** The server says a jumper's shield was struck. */
    public static void handle(final JumperPayloads.ShieldHit hit) {
        final ClientLevel now = Minecraft.getInstance().level;
        if (now == null) {
            return;
        }
        if (now != JumperShield.level) {
            JumperShield.HITS.clear();
            JumperShield.level = now;
        }
        final long time = now.getGameTime();
        // Those long gone, of jumpers no longer drawn too, go as others come
        JumperShield.HITS.values().removeIf(hits -> {
            hits.removeIf(old -> time - old.time() > old.life());
            return hits.isEmpty();
        });
        // Where the line from the shell's middle towards the blow meets the shell, on the sphere the shell is
        // stretched from
        final Vector3f at = new Vector3f(hit.way().x() / JumperShield.ACROSS, hit.way().y() / JumperShield.UP,
                hit.way().z() / JumperShield.ALONG).normalize();
        JumperShield.HITS.computeIfAbsent(hit.jumper(), id -> new ArrayList<>())
                .add(new Hit(at, hit.strength(), hit.collapsed(), time));
    }

    /** @return the blows showing on a jumper's shield, for a frame; an empty list for none */
    static List<Hit> of(final PuddleJumperEntity jumper) {
        final List<Hit> hits = JumperShield.HITS.get(jumper.getId());
        if (hits == null || jumper.level() != JumperShield.level) {
            return List.of();
        }
        final long time = jumper.level().getGameTime();
        hits.removeIf(hit -> time - hit.time() > hit.life());
        return hits.isEmpty() ? List.of() : List.copyOf(hits);
    }

    /** Forgets every blow, as the client leaves its world. */
    public static void clear() {
        JumperShield.HITS.clear();
        JumperShield.level = null;
    }

    /**
     * Draws the patches of a jumper's shield its blows light, light added to whatever is behind them.
     *
     * @param hits the blows, as {@link #of} gives them
     * @param yaw which way the jumper faces, in degrees
     * @param time the game time, in ticks, with the part tick
     */
    static void submit(final List<Hit> hits, final float yaw, final float time, final PoseStack poseStack,
            final SubmitNodeCollector collector) {
        if (hits.isEmpty()) {
            return;
        }
        final double angle = yaw * Mth.DEG_TO_RAD;
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, buffer) -> {
            for (final Hit hit : hits) {
                final float age = Mth.clamp((time - hit.time()) / hit.life(), 0, 1);
                JumperShield.patch(pose, buffer, hit, age, angle);
            }
        });
    }

    // One blow's patch: rings round the point struck out to the patch's reach, each lit by the flash at the middle,
    // fading as it ages, and by the ripple running out across it
    private static void patch(final PoseStack.Pose pose, final VertexConsumer buffer, final Hit hit, final float age,
            final double angle) {
        final float reach = hit.collapsed() ? JumperShield.COLLAPSE_REACH
                : Mth.lerp(hit.strength(), JumperShield.LIGHT_REACH, JumperShield.HARD_REACH);
        // Two ways across the shell at the point struck
        final Vector3fc at = hit.at();
        final Vector3f side = Math.abs(at.y()) < 0.9F ? new Vector3f(0, 1, 0).cross(at).normalize()
                : new Vector3f(1, 0, 0).cross(at).normalize();
        final Vector3f other = new Vector3f(at).cross(side).normalize();
        final Vec3[][] corners = new Vec3[JumperShield.RINGS + 1][JumperShield.SPOKES + 1];
        final int[][] colors = new int[JumperShield.RINGS + 1][JumperShield.SPOKES + 1];
        for (int ring = 0; ring <= JumperShield.RINGS; ring++) {
            final float out = reach * ring / JumperShield.RINGS;
            for (int spoke = 0; spoke <= JumperShield.SPOKES; spoke++) {
                final float round = Mth.TWO_PI * spoke / JumperShield.SPOKES;
                final Vector3f way = new Vector3f(at).mul(Mth.cos(out))
                        .add(new Vector3f(side).mul(Mth.sin(out) * Mth.cos(round)))
                        .add(new Vector3f(other).mul(Mth.sin(out) * Mth.sin(round)));
                final Vec3 local = new Vec3(way.x * JumperShield.ACROSS,
                        way.y * JumperShield.UP + PuddleJumperEntity.SHIELD_MIDDLE, way.z * JumperShield.ALONG);
                corners[ring][spoke] = JumperHull.toWorld(local, angle);
                colors[ring][spoke] = JumperShield.light(hit, out / reach, round, age);
            }
        }
        for (int ring = 0; ring < JumperShield.RINGS; ring++) {
            for (int spoke = 0; spoke < JumperShield.SPOKES; spoke++) {
                // Both faces, so it shows from inside the shell as well as out
                JumperShield.corner(pose, buffer, corners[ring][spoke], colors[ring][spoke]);
                JumperShield.corner(pose, buffer, corners[ring + 1][spoke], colors[ring + 1][spoke]);
                JumperShield.corner(pose, buffer, corners[ring + 1][spoke + 1], colors[ring + 1][spoke + 1]);
                JumperShield.corner(pose, buffer, corners[ring][spoke + 1], colors[ring][spoke + 1]);
                JumperShield.corner(pose, buffer, corners[ring][spoke + 1], colors[ring][spoke + 1]);
                JumperShield.corner(pose, buffer, corners[ring + 1][spoke + 1], colors[ring + 1][spoke + 1]);
                JumperShield.corner(pose, buffer, corners[ring + 1][spoke], colors[ring + 1][spoke]);
                JumperShield.corner(pose, buffer, corners[ring][spoke], colors[ring][spoke]);
            }
        }
    }

    // How a point of a patch is lit, so far out from its middle as a share of its reach, so far round it, and at an
    // age as a share of the patch's life: the flash at the middle, the ripple's ring, and a faint weave of cells over
    // both, as the show's shields have, all dying away to nothing at the patch's edge
    private static int light(final Hit hit, final float out, final float round, final float age) {
        final float fading = (1 - age) * (1 - age);
        final float flash = (float) Math.exp(-out * out / 0.06F) * fading;
        final float ripple = age * 0.9F + 0.08F;
        final float ring = (float) Math.exp(-Mth.square((out - ripple) / 0.12F)) * (1 - age) * 0.8F;
        final float weave = 0.75F + 0.25F * Mth.sin(round * 6 + out * 18) * Mth.sin(out * 25 - age * 6);
        final float edge = 1 - Mth.clamp((out - 0.7F) / 0.3F, 0, 1);
        final float strength = hit.collapsed() ? 1 : Mth.lerp(hit.strength(), 0.55F, 1);
        final float brightness = Mth.clamp((flash + ring) * weave * edge * strength, 0, 1);
        final int color = hit.collapsed() ? JumperShield.FLARE : ARGB.srgbLerp(Mth.clamp(flash * 1.5F, 0, 1),
                JumperShield.BLUE, JumperShield.CORE);
        return ARGB.color(Math.round(255 * brightness), color & 0xFFFFFF);
    }

    /**
     * Draws the shield rising or falling, as a city shield's dome spreads, but far quicker and round the jumper: rising,
     * it spreads over the shell from its top and its foot, a bright edge leading, until the two meet round its middle,
     * then the whole shell fades away; falling, it flickers and draws back from its middle to nothing. It is drawn
     * where the jumper is drawn, turned with it, so it keeps up as the jumper flies.
     *
     * @param age how long ago the shield rose or fell, in ticks, with the part tick
     * @param rose whether it rose, rather than fell
     * @param yaw which way the jumper faces, in degrees
     * @param time the game time, in ticks, with the part tick
     */
    static void sweep(final float age, final boolean rose, final float yaw, final float time,
            final PoseStack poseStack, final SubmitNodeCollector collector) {
        if (age < 0 || age >= (rose ? JumperShield.RISE_TICKS + JumperShield.RISE_FADE : JumperShield.FALL_TICKS)) {
            return;
        }
        final double angle = yaw * Mth.DEG_TO_RAD;
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, buffer) -> {
            final Vec3[][] corners = new Vec3[JumperShield.BANDS + 1][JumperShield.SLICES + 1];
            final int[][] colors = new int[JumperShield.BANDS + 1][JumperShield.SLICES + 1];
            for (int band = 0; band <= JumperShield.BANDS; band++) {
                final float down = Mth.PI * band / JumperShield.BANDS;
                for (int slice = 0; slice <= JumperShield.SLICES; slice++) {
                    final float round = Mth.TWO_PI * slice / JumperShield.SLICES;
                    final Vec3 local = new Vec3(Mth.sin(down) * Mth.cos(round) * JumperShield.ACROSS,
                            Mth.cos(down) * JumperShield.UP + PuddleJumperEntity.SHIELD_MIDDLE,
                            Mth.sin(down) * Mth.sin(round) * JumperShield.ALONG);
                    corners[band][slice] = JumperHull.toWorld(local, angle);
                    colors[band][slice] = JumperShield.sweepLight(age, rose, down, round, time);
                }
            }
            for (int band = 0; band < JumperShield.BANDS; band++) {
                for (int slice = 0; slice < JumperShield.SLICES; slice++) {
                    JumperShield.corner(pose, buffer, corners[band][slice], colors[band][slice]);
                    JumperShield.corner(pose, buffer, corners[band + 1][slice], colors[band + 1][slice]);
                    JumperShield.corner(pose, buffer, corners[band + 1][slice + 1], colors[band + 1][slice + 1]);
                    JumperShield.corner(pose, buffer, corners[band][slice + 1], colors[band][slice + 1]);
                    JumperShield.corner(pose, buffer, corners[band][slice + 1], colors[band][slice + 1]);
                    JumperShield.corner(pose, buffer, corners[band + 1][slice + 1], colors[band + 1][slice + 1]);
                    JumperShield.corner(pose, buffer, corners[band + 1][slice], colors[band + 1][slice]);
                    JumperShield.corner(pose, buffer, corners[band][slice], colors[band][slice]);
                }
            }
        });
    }

    // How a point of the shell is lit as the shield rises or falls, so far down from its top and round it: lit within
    // the front spreading from the top and the foot, brightest along it, with a shimmer running round the shell
    private static float sweepBrightness(final float age, final boolean rose, final float down, final float round,
            final float time) {
        // How far the point is from the nearer of the top and the foot, from 0 to a quarter turn round its middle
        final float fromEnd = Math.min(down, Mth.PI - down);
        final float shimmer = 0.75F + 0.25F * Mth.sin(round * 5 + down * 8 - time * 0.4F);
        if (rose) {
            final float spread = Mth.clamp(age / JumperShield.RISE_TICKS, 0, 1) * Mth.HALF_PI;
            final float fading = age <= JumperShield.RISE_TICKS ? 1
                    : 1 - (age - JumperShield.RISE_TICKS) / JumperShield.RISE_FADE;
            if (fromEnd > spread + 0.05F) {
                return 0;
            }
            final float edge = spread >= Mth.HALF_PI ? 0
                    : (float) Math.exp(-Mth.square((fromEnd - spread) / 0.12F)) * 0.9F;
            return Mth.clamp((JumperShield.SWEEP_GLOW * shimmer + edge) * fading, 0, 1);
        }
        final float left = 1 - age / JumperShield.FALL_TICKS;
        if (fromEnd > left * Mth.HALF_PI) {
            return 0;
        }
        final float flicker = 0.6F + 0.4F * Mth.sin(time * 2.3F);
        return Mth.clamp(JumperShield.SWEEP_GLOW * 1.5F * shimmer * flicker * left, 0, 1);
    }

    private static int sweepLight(final float age, final boolean rose, final float down, final float round,
            final float time) {
        final float brightness = JumperShield.sweepBrightness(age, rose, down, round, time);
        return ARGB.color(Math.round(255 * brightness), ARGB.srgbLerp(brightness, JumperShield.BLUE,
                JumperShield.CORE) & 0xFFFFFF);
    }

    private static void corner(final PoseStack.Pose pose, final VertexConsumer buffer, final Vec3 at,
            final int color) {
        buffer.addVertex(pose, (float) at.x, (float) at.y, (float) at.z).setColor(color);
    }
}
