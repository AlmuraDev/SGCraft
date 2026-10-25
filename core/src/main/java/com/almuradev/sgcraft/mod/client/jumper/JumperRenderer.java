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
import com.almuradev.sgcraft.mod.client.computer.ComputerDisplays;
import com.almuradev.sgcraft.mod.entity.JumperHull;
import com.almuradev.sgcraft.mod.entity.JumperSeat;
import com.almuradev.sgcraft.mod.entity.PuddleJumperEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws a puddle jumper: its hull turned the way it faces, banking into its turns and tipping its nose as it climbs or
 * descends, rocking when hit as a boat does, with its cargo door, drive pods and headlamps as they are; its windows over
 * it, but not from inside, and its lights glowing. The computer aboard shows its dialing program on the screen in front
 * of the right seat, as a dialing computer's monitor does, to anyone near.
 */
public final class JumperRenderer extends EntityRenderer<PuddleJumperEntity, JumperRenderState> {

    private static final Identifier TEXTURE = SGCraft.id("textures/entity/puddle_jumper.png");
    // The middle of each drive pod's engine's back, how far up the engine from its base, in pixels, as
    // JumperModel.engineBack takes it, and how far back the back is; and how far behind it its glow hangs, clear of the
    // engine
    private static final float ENGINE_ALONG = 7;
    private static final float ENGINE_BACK = 44.9F;
    private static final float GLOW_BEHIND = 3;
    // The engines' glow: light added to whatever is behind it, a soft blue disc, a white hot core fading through the
    // drive's blue to a deep blue rim, in blocks across, idling and flat out; a wide bloom round it; and, as they near
    // flat out, the core burning hotter, drawn again over itself, up to so many times more
    private static final float GLOW_SIZE = 0.8F;
    private static final float GLOW_FULL = 1.4F;
    private static final float BLOOM_SIZE = 1.4F;
    private static final float BLOOM_FULL = 2.4F;
    private static final int OVERDRIVE = 0;
    // The disc's rings, from its middle out, as fractions of its radius, and their colours, alpha as a share of the
    // glow's brightness
    private static final float[] RINGS = {0, 0.18F, 0.45F, 1};
    private static final int[] RING_COLORS = {0xFFEAF4FF, 0xE68CC0FF, 0x704C8CFF, 0x002A5CFF};
    private static final int[] BLOOM_COLORS = {0x24A0CCFF, 0x166EA4FF, 0x08406CFF, 0x00203CFF};
    private static final int SEGMENTS = 20;
    // The plume trailing behind each engine as it revs up: how many glows long it is flat out, and how far apart
    // they are, in pixels
    private static final int PLUME = 5;
    private static final float PLUME_STEP = 6;
    // The plume's colours from its middle out: a hot white core through a rich blue to a deep indigo rim
    private static final int[] PLUME_COLORS = {0xFFE2F0FF, 0xF058A6FF, 0xB02458FF, 0x00081FFF};
    // The headlamps' beams, a shaft of light from each lamp under the nose that shows whatever it falls on or not, as
    // in water, rain or the dark: where the lamps are, in blocks in the model's space, across and down from the
    // hull's middle and ahead of it; how far the beams fall below level, how far they reach and how wide they open, in
    // degrees; how far apart the soft glows it is drawn as are, as a share of their width, so they always overlap and
    // no one shows as an orb, how bright each is for the beam's length it covers, fading out along the beam, and the
    // colours across each, flat in the middle; and the lamp's own glow, how wide and how bright
    private static final float LAMP_ACROSS = 7.5F / 16;
    private static final float LAMP_DOWN = -6.5F / 16;
    private static final float LAMP_AHEAD = -53.7F / 16;
    private static final float BEAM_DOWN = 9;
    private static final float BEAM_REACH = 30;
    private static final float BEAM_SPREAD = 14;
    private static final float BEAM_STEP = 0.6F;
    private static final float BEAM_BRIGHTNESS = 0.22F;
    private static final int BEAM_SEGMENTS = 12;
    // A disc's own middle and the two ways it lies, facing along z
    private static final Vector3fc ORIGIN = new Vector3f();
    private static final Vector3fc ACROSS = new Vector3f(1, 0, 0);
    private static final Vector3fc UP = new Vector3f(0, 1, 0);
    private static final int[] BEAM_COLORS = {0xFFFFF6E8, 0xE0FFF4E4, 0x60F0F0FF, 0x00E8ECFF};
    private static final float[] BEAM_REACHES = JumperRenderer.beamReaches();
    private static final float LENS = 0.35F;
    // Where the lamps are in the jumper's frame, from its middle underside, in blocks, as the beams are cast from
    private static final Vec3 LAMPS = new Vec3(0, 0.45, 3.6);
    // How bright the beams are seen from inside the hull, looking along them, as a share of how bright from outside
    private static final float FROM_INSIDE = 0.28F;
    private static final int[] LENS_COLORS = {0xFFFFFFFF, 0xC0FFF6E8, 0x40FFE8D0, 0x00FFE8D0};
    // How near the camera must be for the computer's screen to be drawn
    private static final double SCREEN_DISTANCE = 32;
    // The screen, in blocks in the model's space: across from its left to its right as its operator sees it, from its
    // top to its bottom, and just behind the face of its case
    private static final float SCREEN_LEFT = -2.4F / 16;
    private static final float SCREEN_RIGHT = -15F / 16;
    private static final float SCREEN_TOP = -21F / 16;
    private static final float SCREEN_BOTTOM = -16.2F / 16;
    private static final float SCREEN_FACE = -45.45F / 16;
    private static final int WHITE = 0xFFFFFFFF;
    // The windows seen from inside: how strongly, as a share of how they show from outside, their tint, and over how
    // far into the hull they ease from one to the other, in blocks
    private static final float GLASS_INSIDE = 0.22F;
    private static final int GLASS_TINT = 0xDCEAFF;
    private static final double GLASS_FADE = 0.5;
    // The cloak: how much it hides from a rider looking on from behind in third person, how bright the shimmer of a
    // cloaked jumper moving at its top speed is, beyond the faint trace it always shows, and the flicker as the cloak comes on or goes off, the speed that
    // shimmer is full at, in blocks a tick, and the pale blue it is tinted as it fades
    private static final float CLOAK_ABOARD = 0.55F;
    private static final float SHIMMER = 0.2F;
    private static final float SHIMMER_REST = 0.035F;
    private static final float CLOAK_FLICKER = 0.35F;
    private static final double SHIMMER_SPEED = 0.55;
    private static final int CLOAK_TINT = 0xBFDCFF;

    private final JumperModel model;
    private final JumperModel glass;
    private final JumperModel glow;

    public JumperRenderer(final EntityRendererProvider.Context context) {
        super(context);
        this.model = new JumperModel(context.bakeLayer(JumperModel.LAYER), RenderTypes::entityCutout);
        this.glass = new JumperModel(context.bakeLayer(JumperModel.GLASS_LAYER), RenderTypes::entityTranslucent);
        this.glow = new JumperModel(context.bakeLayer(JumperModel.GLOW_LAYER), RenderTypes::eyes);
        this.shadowRadius = 1.6F;
    }

    // The nose and tail reach beyond the hitbox, which is as wide as the hull
    @Override
    protected AABB getBoundingBoxForCulling(final PuddleJumperEntity entity, final float partialTicks) {
        final double reach = (PuddleJumperEntity.LENGTH - PuddleJumperEntity.WIDTH) / 2;
        return super.getBoundingBoxForCulling(entity, partialTicks).inflate(reach, 0, reach);
    }

    // The cloak hides its shadow from those outside as it hides the jumper
    @Override
    protected float getShadowRadius(final JumperRenderState state) {
        return super.getShadowRadius(state) * (state.viewerInside ? 1 : 1 - state.cloak);
    }

    @Override
    public JumperRenderState createRenderState() {
        return new JumperRenderState();
    }

    @Override
    public void extractRenderState(final PuddleJumperEntity entity, final JumperRenderState state,
            final float partialTick) {
        // Before the shadow is worked out, which the cloak hides
        state.cloak = entity.cloak(partialTick);
        state.viewerInside = JumperControls.viewerInside(entity);
        super.extractRenderState(entity, state, partialTick);
        state.shimmer = (float) Mth.clamp(Math.sqrt(Mth.square(entity.getX() - entity.xo)
                + Mth.square(entity.getY() - entity.yo) + Mth.square(entity.getZ() - entity.zo))
                / JumperRenderer.SHIMMER_SPEED, 0, 1);
        state.time = entity.level().getGameTime() + partialTick;
        state.hits = JumperShield.of(entity);
        state.shieldAge = entity.shieldChanged() == Long.MIN_VALUE ? Float.MAX_VALUE
                : entity.level().getGameTime() - entity.shieldChanged() + partialTick;
        state.shieldRose = entity.shieldRose();
        if (state.cloak > 0.5F && !state.viewerInside) {
            state.nameTag = null;
        }
        state.yRot = entity.getYRot(partialTick);
        state.hurtTime = entity.getHurtTime() - partialTick;
        state.hurtDir = entity.getHurtDir();
        state.damageTime = Math.max(entity.getDamage() - partialTick, 0.0F);
        state.bank = entity.bank(partialTick);
        state.tilt = entity.tilt(partialTick);
        state.hatch = entity.hatch(partialTick);
        state.pods = entity.pods(partialTick);
        state.bays = entity.bays(partialTick);
        state.drones = Math.min(entity.drones(), PuddleJumperEntity.DRONES);
        state.thrust = entity.thrust(partialTick);
        state.headlights = entity.headlights() && JumperLights.needed(entity);
        state.inWater = entity.isInWater();
        state.beamEnd = state.headlights ? JumperRenderer.beamEnd(entity, partialTick) : 0;
        state.powered = entity.powered();
        state.picture = state.powered
                && state.distanceToCameraSq < JumperRenderer.SCREEN_DISTANCE * JumperRenderer.SCREEN_DISTANCE
                ? ComputerDisplays.texture(entity.dialer(), partialTick) : null;
    }

    @Override
    public void submit(final JumperRenderState state, final PoseStack poseStack, final SubmitNodeCollector collector,
            final CameraRenderState camera) {
        final Matrix4f base = new Matrix4f(poseStack.last().pose());
        final Vector3f[][] engines = new Vector3f[2][JumperRenderer.PLUME + 1];
        poseStack.pushPose();
        poseStack.rotateDegrees(Axis.YP, 180.0F - state.yRot);
        if (state.hurtTime > 0.0F) {
            poseStack.rotateDegrees(Axis.XP, Mth.sin(state.hurtTime) * state.hurtTime * state.damageTime / 10.0F
                    * state.hurtDir);
        }
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        // It banks and tips about its riders' hips, so they stay in their seats as it does
        final float pivot = (float) -JumperSeat.HEIGHT;
        poseStack.translate(0, pivot, 0);
        // The model's space is mirrored left to right and upside down from the world's, so a bank to the right, or the
        // nose tipping up, turns the other way about its axes
        poseStack.rotateDegrees(Axis.ZP, -state.bank);
        poseStack.rotateDegrees(Axis.XP, -state.tilt);
        poseStack.translate(0, -pivot, 0);
        final Vec3 cameraOffset = camera.pos.subtract(state.x, state.y, state.z);
        final double depth = JumperHull.depthInside(JumperHull.toLocal(cameraOffset, state.yRot));
        final boolean fromInside = depth > 0;
        // How much the cloak hides it from the camera: from inside the hull, not at all; from behind a rider aboard,
        // in third person, only partly, so they see it is on; from outside, wholly once it has come on
        final float hidden = fromInside ? 0 : state.viewerInside ? state.cloak * JumperRenderer.CLOAK_ABOARD
                : state.cloak;
        final boolean shown = hidden < 0.5F;
        if (hidden <= 0) {
            collector.submitModel(this.model, state, poseStack, JumperRenderer.TEXTURE, state.lightCoords,
                    OverlayTexture.NO_OVERLAY, state.outlineColor);
        } else {
            this.cloaked(state, poseStack, collector, hidden);
        }
        if (shown) {
            // The windows: as they are from outside; from inside, a faint blue tint, so they read as glass without
            // darkening the view or hiding the water and weather beyond, easing from one to the other as the camera
            // crosses into the hull
            final float inward = (float) Mth.clamp(depth / JumperRenderer.GLASS_FADE, 0, 1);
            final int tint = ARGB.color(Math.round(255 * Mth.lerp(inward, 1, JumperRenderer.GLASS_INSIDE)
                    * (1 - hidden)), fromInside ? JumperRenderer.GLASS_TINT : 0xFFFFFF);
            collector.submitModel(this.glass, state, poseStack, this.glass.renderType(JumperRenderer.TEXTURE),
                    state.lightCoords, OverlayTexture.NO_OVERLAY, tint, null, state.outlineColor);
        }
        // Its lights, inside and out, and the computer's screen go dark with its drive off
        if (state.powered && shown) {
            collector.submitModel(this.glow, state, poseStack, JumperRenderer.TEXTURE,
                    LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
        if (state.picture != null && shown) {
            collector.submitCustomGeometry(poseStack, RenderTypes.textPolygonOffset(state.picture),
                    JumperRenderer::screen);
        }
        // Where the engines are, as the hull turns and banks, from where it is drawn
        final Matrix4f toBase = new Matrix4f(base).invert().mul(poseStack.last().pose());
        // Each pod pushed out on its arms and its fin tipped, as JumperModel moves it, and its plume trailing back
        for (int i = 0; i < 2; i++) {
            final float[] at = JumperModel.engineBack(i == 0, state.pods, JumperRenderer.ENGINE_ALONG);
            for (int step = 0; step <= JumperRenderer.PLUME; step++) {
                engines[i][step] = toBase.transformPosition(at[0] / 16, at[1] / 16, (JumperRenderer.ENGINE_BACK
                        + JumperRenderer.GLOW_BEHIND + step * JumperRenderer.PLUME_STEP) / 16, new Vector3f());
            }
        }
        // Where the beams' glows are, along each from its lamp, as the hull turns and banks
        // From inside, looking along them, the beams' glows stack up into flat, angular patches over the lit ground;
        // there the light on the ground shows the beams, but for in water, where nothing else would
        final Vector3f[][] beams = state.headlights && shown && (!fromInside || state.inWater)
                ? JumperRenderer.beamPoints(toBase) : null;
        poseStack.popPose();
        if (beams != null) {
            JumperRenderer.beams(poseStack, collector, camera, beams, fromInside, state.beamEnd);
        }
        // From inside the hull its walls hide the engines, and the glows, standing out beyond them, would show over
        // the walls' inner faces
        if (state.pods > 0.05F && state.thrust > 0.01F && !fromInside && shown) {
            JumperRenderer.halos(state, poseStack, collector, camera, engines, cameraOffset);
        }
        JumperShield.submit(state.hits, state.yRot, state.time, poseStack, collector);
        JumperShield.sweep(state.shieldAge, state.shieldRose, state.yRot, state.time, poseStack, collector);
        super.submit(state, poseStack, collector, camera);
    }

    // The hull as the cloak leaves it: fading out as the cloak comes on, and once it is on, gone but for a faint,
    // flickering shimmer of its shape while it moves, stronger the faster, and a flicker as the cloak comes on or goes
    // off. Its sounds are left as they are
    private void cloaked(final JumperRenderState state, final PoseStack poseStack,
            final SubmitNodeCollector collector, final float hidden) {
        final float flicker = 0.75F + 0.25F * Mth.sin(state.time * 0.9F) * Mth.sin(state.time * 0.37F + 1);
        float alpha = 1 - hidden;
        if (!state.viewerInside) {
            // Never quite gone, so a jumper left cloaked can still be found by someone who looks hard
            final float moving = state.cloak * (JumperRenderer.SHIMMER_REST + state.shimmer * JumperRenderer.SHIMMER)
                    * flicker;
            final float changing = 4 * state.cloak * (1 - state.cloak) * JumperRenderer.CLOAK_FLICKER * flicker;
            alpha = Math.max(alpha, Math.max(moving, changing));
        }
        if (alpha < 0.004F) {
            return;
        }
        final int tint = ARGB.srgbLerp(Mth.clamp(hidden * 1.5F, 0, 1), 0xFFFFFF, JumperRenderer.CLOAK_TINT);
        collector.submitModel(this.model, state, poseStack, RenderTypes.entityTranslucent(JumperRenderer.TEXTURE),
                state.lightCoords, OverlayTexture.NO_OVERLAY, ARGB.color(alpha, tint), null,
                hidden < 0.5F ? state.outlineColor : 0);
    }

    // The glow behind each engine, facing the camera: light added to whatever lies behind it, a soft blue disc with a
    // white hot core in a wide bloom, small and dim while they idle, swelling and blazing brighter as they rev up, the
    // core burning hotter still near flat out, and a plume of fading glows trailing out behind. Each glow is a disc
    // facing the camera, all at the depth of its middle, so where the hull stands just behind that, the disc would
    // show over it; the bloom is drawn back from the camera by all its width, and the plume's glows by half theirs,
    // grown to look the same size, so the hull before them hides them as it would the light they stand for
    private static void halos(final JumperRenderState state, final PoseStack poseStack,
            final SubmitNodeCollector collector, final CameraRenderState camera, final Vector3f[][] engines,
            final Vec3 eye) {
        final float thrust = state.thrust;
        final float shown = Mth.clamp(state.pods * 1.5F - 0.5F, 0, 1) * Mth.lerp(thrust, 0.2F, 0.6F);
        final float glow = Mth.lerp(thrust, JumperRenderer.GLOW_SIZE, JumperRenderer.GLOW_FULL) / 2;
        final float bloom = Mth.lerp(thrust, JumperRenderer.BLOOM_SIZE, JumperRenderer.BLOOM_FULL) / 2;
        // The plume grows out behind once the engines pass a third of their thrust
        final float plume = Mth.clamp((thrust - 0.15F) / 0.85F, 0, 1) * JumperRenderer.PLUME;
        // Past half thrust the core is drawn over itself again, adding light, so it blazes
        final float overdrive = Mth.clamp((thrust - 0.5F) * 2, 0, 1) * JumperRenderer.OVERDRIVE;
        for (final Vector3f[] engine : engines) {
            for (int step = (int) Math.ceil(plume); step >= 0; step--) {
                final float fade = step == 0 ? 1
                        : Math.min(1, plume - step + 1) * (float) Math.pow(1 - step / (plume + 1), 0.8) * 0.35F;
                final float size = step == 0 ? 1 : 0.8F - step * 0.1F;
                final float brightness = shown * fade;
                final boolean first = step == 0;
                if (first) {
                    JumperRenderer.glowAt(poseStack, collector, camera, engine[0], eye, bloom, (pose, buffer) ->
                            JumperRenderer.disc(pose, buffer, bloom, JumperRenderer.BLOOM_COLORS, brightness));
                }
                JumperRenderer.glowAt(poseStack, collector, camera, engine[step], eye, first ? 0 : glow * size / 2,
                        (pose, buffer) -> {
                            JumperRenderer.disc(pose, buffer, glow * size,
                                    first ? JumperRenderer.RING_COLORS : JumperRenderer.PLUME_COLORS, brightness);
                            if (first) {
                                for (int again = 0; again < Math.ceil(overdrive); again++) {
                                    JumperRenderer.disc(pose, buffer, glow * 0.7F, JumperRenderer.RING_COLORS,
                                            brightness * Math.min(1, overdrive - again));
                                }
                            }
                        });
            }
        }
    }

    // Draws a glow facing the camera about a point, set back from the camera by so far, in blocks, and grown to match
    private static void glowAt(final PoseStack poseStack, final SubmitNodeCollector collector,
            final CameraRenderState camera, final Vector3f at, final Vec3 eye, final float back,
            final SubmitNodeCollector.CustomGeometryRenderer draw) {
        final double dx = at.x - eye.x;
        final double dy = at.y - eye.y;
        final double dz = at.z - eye.z;
        final double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        final float stretch = distance < 0.01 ? 1 : (float) ((distance + back) / distance);
        poseStack.pushPose();
        poseStack.translate((float) (eye.x + dx * stretch), (float) (eye.y + dy * stretch),
                (float) (eye.z + dz * stretch));
        poseStack.rotate(camera.orientation);
        poseStack.scale(stretch, stretch, stretch);
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), draw);
        poseStack.popPose();
    }

    // A soft disc facing the camera, in rings from its middle out, each ring's colour blending into the next
    static void disc(final PoseStack.Pose pose, final VertexConsumer buffer, final float radius,
            final int[] colors, final float brightness) {
        JumperRenderer.disc(pose, buffer, JumperRenderer.ORIGIN, JumperRenderer.ACROSS, JumperRenderer.UP, radius,
                colors, brightness, JumperRenderer.SEGMENTS);
    }

    // A soft disc about a point, lying across two ways, in so many segments round
    private static void disc(final PoseStack.Pose pose, final VertexConsumer buffer, final Vector3fc middle,
            final Vector3fc across, final Vector3fc up, final float radius, final int[] colors, final float brightness,
            final int segments) {
        for (int ring = 0; ring < JumperRenderer.RINGS.length - 1; ring++) {
            final float inner = JumperRenderer.RINGS[ring] * radius;
            final float outer = JumperRenderer.RINGS[ring + 1] * radius;
            final int innerColor = JumperRenderer.dimmed(colors[ring], brightness);
            final int outerColor = JumperRenderer.dimmed(colors[ring + 1], brightness);
            for (int segment = 0; segment < segments; segment++) {
                final float a0 = segment * Mth.TWO_PI / segments;
                final float a1 = (segment + 1) * Mth.TWO_PI / segments;
                final float c0 = Mth.cos(a0);
                final float s0 = Mth.sin(a0);
                final float c1 = Mth.cos(a1);
                final float s1 = Mth.sin(a1);
                // Both faces, so it shows however the camera's turn leaves it wound
                JumperRenderer.corner(pose, buffer, middle, across, up, c0 * inner, s0 * inner, innerColor);
                JumperRenderer.corner(pose, buffer, middle, across, up, c0 * outer, s0 * outer, outerColor);
                JumperRenderer.corner(pose, buffer, middle, across, up, c1 * outer, s1 * outer, outerColor);
                JumperRenderer.corner(pose, buffer, middle, across, up, c1 * inner, s1 * inner, innerColor);
                JumperRenderer.corner(pose, buffer, middle, across, up, c1 * inner, s1 * inner, innerColor);
                JumperRenderer.corner(pose, buffer, middle, across, up, c1 * outer, s1 * outer, outerColor);
                JumperRenderer.corner(pose, buffer, middle, across, up, c0 * outer, s0 * outer, outerColor);
                JumperRenderer.corner(pose, buffer, middle, across, up, c0 * inner, s0 * inner, innerColor);
            }
        }
    }

    private static void corner(final PoseStack.Pose pose, final VertexConsumer buffer, final Vector3fc middle,
            final Vector3fc across, final Vector3fc up, final float x, final float y, final int color) {
        buffer.addVertex(pose, middle.x() + across.x() * x + up.x() * y, middle.y() + across.y() * x + up.y() * y,
                middle.z() + across.z() * x + up.z() * y).setColor(color);
    }

    private static int dimmed(final int color, final float brightness) {
        return ARGB.color(Math.round(ARGB.alpha(color) * Mth.clamp(brightness, 0, 1)), color & 0xFFFFFF);
    }

    // The middles of the glows along each beam, from its lamp out, from where the jumper is drawn
    private static Vector3f[][] beamPoints(final Matrix4f toBase) {
        // Ahead is towards negative z in the model's space, and down towards positive y
        final float down = (float) Math.toRadians(JumperRenderer.BEAM_DOWN);
        final Vector3f[][] beams = new Vector3f[2][JumperRenderer.BEAM_REACHES.length];
        for (int i = 0; i < 2; i++) {
            final float x = JumperRenderer.LAMP_ACROSS * (i == 0 ? 1 : -1);
            for (int step = 0; step < JumperRenderer.BEAM_REACHES.length; step++) {
                final float along = JumperRenderer.BEAM_REACHES[step];
                beams[i][step] = toBase.transformPosition(x, JumperRenderer.LAMP_DOWN + Mth.sin(down) * along,
                        JumperRenderer.LAMP_AHEAD - Mth.cos(down) * along, new Vector3f());
            }
        }
        return beams;
    }

    // How far the beams reach before they meet a block, along the middle of them, from the lamps
    private static float beamEnd(final PuddleJumperEntity jumper, final float partialTick) {
        final float yaw = jumper.getYRot(partialTick);
        final Vec3 lamps = jumper.getPosition(partialTick).add(JumperHull.toWorld(JumperRenderer.LAMPS,
                yaw * Mth.DEG_TO_RAD));
        final Vec3 way = Vec3.directionFromRotation(JumperRenderer.BEAM_DOWN, yaw);
        final BlockHitResult hit = jumper.level().clip(new ClipContext(lamps,
                lamps.add(way.scale(JumperRenderer.BEAM_REACH)), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE,
                jumper));
        return hit.getType() == HitResult.Type.MISS ? JumperRenderer.BEAM_REACH
                : (float) hit.getLocation().distanceTo(lamps);
    }

    // How wide a beam is so far along it, in blocks from its middle
    private static float beamRadius(final float along) {
        return 0.2F + along * (float) Math.tan(Math.toRadians(JumperRenderer.BEAM_SPREAD));
    }

    // How far along its beam each glow is, the lamp's own first: each the next a share of its width on, so they
    // bunch up near the lamp, where the beam is narrow, and spread out as it widens, always overlapping
    private static float[] beamReaches() {
        final List<Float> reaches = new ArrayList<>();
        for (float along = 0; along <= JumperRenderer.BEAM_REACH;
                along += JumperRenderer.BEAM_STEP * JumperRenderer.beamRadius(along)) {
            reaches.add(along);
        }
        final float[] array = new float[reaches.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = reaches.get(i);
        }
        return array;
    }

    // The headlamps' beams, each a row of soft glows facing the camera, light added to whatever lies behind them,
    // widening as the beam opens and fading out along its reach, so together they show a soft shaft of light from any
    // side and from within it, with no edges; and the lamps' own glow. The world's fog thins them with distance
    // and ending where they meet the ground, or anything else, fading out over their last stretch into it, so neither
    // a glow's round edge nor the ground cutting through one shows
    private static void beams(final PoseStack poseStack, final SubmitNodeCollector collector,
            final CameraRenderState camera, final Vector3f[][] beams, final boolean fromInside, final float end) {
        final float fading = JumperRenderer.beamRadius(end) * 1.5F + 1;
        // Seen from the cockpit, looking along the beams, their glows all stack up before the eye, so they are drawn
        // far dimmer, and the lamps' own glows, right before the windshield, not at all
        final float seen = fromInside ? JumperRenderer.FROM_INSIDE : 1;
        // Each glow faces the camera; a beam's glows are all sent at once, the furthest first
        final Vector3fc across = camera.orientation.transform(JumperRenderer.ACROSS, new Vector3f());
        final Vector3fc up = camera.orientation.transform(JumperRenderer.UP, new Vector3f());
        for (final Vector3f[] beam : beams) {
            final List<Glow> glows = new ArrayList<>();
            for (int step = beam.length - 1; step >= 0; step--) {
                final float along = JumperRenderer.BEAM_REACHES[step];
                if (along >= end) {
                    continue;
                }
                final float t = along / JumperRenderer.BEAM_REACH;
                final float radius = JumperRenderer.beamRadius(along);
                // Each as bright as the length of beam it covers, which is the same share of its width all along,
                // so the shaft is even; all fading along the beam, and out to nothing over its last stretch
                final float close = Mth.clamp((end - along) / fading, 0, 1);
                final float brightness = JumperRenderer.BEAM_BRIGHTNESS * (1 - t) * (1 - t) * seen
                        * close * close * (3 - 2 * close);
                final boolean lens = step == 0;
                if (lens && fromInside) {
                    continue;
                }
                glows.add(lens ? new Glow(beam[step], JumperRenderer.LENS, JumperRenderer.LENS_COLORS, 0.85F,
                        JumperRenderer.SEGMENTS) : new Glow(beam[step], radius, JumperRenderer.BEAM_COLORS, brightness,
                        JumperRenderer.BEAM_SEGMENTS));
            }
            if (!glows.isEmpty()) {
                collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, buffer) -> {
                    for (final Glow glow : glows) {
                        JumperRenderer.disc(pose, buffer, glow.middle(), across, up, glow.radius(), glow.colors(),
                                glow.brightness(), glow.segments());
                    }
                });
            }
        }
    }

    // One of a beam's glows
    private record Glow(Vector3fc middle, float radius, int[] colors, float brightness, int segments) {
    }

    // The computer's screen, facing back into the cabin at full brightness, its corners counterclockwise as its
    // operator sees them
    private static void screen(final PoseStack.Pose pose, final VertexConsumer buffer) {
        final int light = LightCoordsUtil.FULL_BRIGHT;
        final float z = JumperRenderer.SCREEN_FACE;
        buffer.addVertex(pose, JumperRenderer.SCREEN_LEFT, JumperRenderer.SCREEN_TOP, z)
                .setColor(JumperRenderer.WHITE).setUv(0, 0).setLight(light);
        buffer.addVertex(pose, JumperRenderer.SCREEN_LEFT, JumperRenderer.SCREEN_BOTTOM, z)
                .setColor(JumperRenderer.WHITE).setUv(0, 1).setLight(light);
        buffer.addVertex(pose, JumperRenderer.SCREEN_RIGHT, JumperRenderer.SCREEN_BOTTOM, z)
                .setColor(JumperRenderer.WHITE).setUv(1, 1).setLight(light);
        buffer.addVertex(pose, JumperRenderer.SCREEN_RIGHT, JumperRenderer.SCREEN_TOP, z)
                .setColor(JumperRenderer.WHITE).setUv(1, 0).setLight(light);
    }
}
