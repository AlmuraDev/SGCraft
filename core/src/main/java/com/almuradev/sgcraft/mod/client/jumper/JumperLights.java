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
import com.almuradev.sgcraft.mod.entity.JumperIndex;
import com.almuradev.sgcraft.mod.entity.PuddleJumperEntity;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMaps;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongLists;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Puddle jumpers' headlights, as the client draws them: a cone of light thrown out ahead of the lamps under the nose,
 * lighting the ground, walls and creatures in front of the jumper and nothing behind or under it.
 *
 * <p>The game has no light that moves, so as the lamps move the cone is cast again, as rays fanned out from the lamps
 * that stop at whatever they meet, brightest close in and along the middle, fading towards the cone's edges and its
 * reach, and spilling a little round where they pass. The light is added to the world's own as the world is drawn: to
 * the blocks, whose chunk sections are drawn again where the cone has come or gone, and to the creatures in it. Nothing
 * is placed in the world, and it is only this client's; each client casts every jumper's cone it sees. Only clients
 * call this.</p>
 */
public final class JumperLights {

    // How often the cones may be cast, in ticks; how far round the player jumpers' lights are cast, in blocks
    private static final int CAST_TICKS = 2;
    // A cone is cast from the middle of the block its lamps are in, turned to the nearest few degrees, so it is cast
    // again, and the world round it drawn again, only once the lamps have moved into another block or turned that far,
    // and otherwise only this often, in ticks, to light what has been built or broken in it since
    private static final float TURN_STEP = 5;
    private static final int RECAST_TICKS = 20;
    private static final double SEEN = 96;
    // The cone: how far it reaches, how far it falls below level ahead, and how wide and tall it spreads, in degrees
    // from its middle; how many rays fan out across and up it; and the steps they march in, in blocks
    private static final double REACH = 36;
    private static final float DOWN = 9;
    private static final float SPREAD = 26;
    private static final float RISE = 15;
    private static final int ACROSS = 23;
    private static final int UP = 13;
    private static final double STEP = 0.5;
    // Where the lamps are, in the jumper's frame from its middle underside, in blocks: under the nose
    private static final Vec3 LAMPS = new Vec3(0, 0.45, 3.6);
    // How bright the light is close in along the middle, and how much it loses spilling to the blocks round a ray
    private static final int BRIGHTEST = 15;
    private static final int SPILL = 3;
    // The sky's light at the jumper, from which the headlights are not needed, and so go out
    private static final int DAYLIGHT = 12;
    private static final Direction[] SIDES = Direction.values();

    // The light each block gets from the cones, by its packed position, for any thread drawing the world to read;
    // replaced whole, never changed
    private static volatile Long2ByteMap lit = Long2ByteMaps.EMPTY_MAP;
    private static int sinceCast;
    // Where the cones were last cast from, and which way, two numbers for each jumper, and how long ago
    private static LongList castFrom = LongLists.EMPTY_LIST;
    private static int sinceRecast;

    private JumperLights() {
    }

    /** @return whether the world round a jumper is dark enough for its headlights to show, if they are on */
    public static boolean needed(final PuddleJumperEntity jumper) {
        // Worlds with no day, as the End and the Nether, are never bright enough to go without them
        final DimensionType type = jumper.level().dimensionType();
        if (type.hasFixedTime() || !type.hasSkyLight()) {
            return true;
        }
        return jumper.level().getEffectiveSkyBrightness(BlockPos.containing(jumper.getX(), jumper.getEyeY(),
                jumper.getZ())) < JumperLights.DAYLIGHT;
    }

    /** @return packed light, with any jumper's headlights' added to its block light */
    public static int brighten(final BlockPos pos, final int packed) {
        final Long2ByteMap now = JumperLights.lit;
        if (now.isEmpty()) {
            return packed;
        }
        final int light = now.get(pos.asLong());
        return light > LightCoordsUtil.block(packed) ? LightCoordsUtil.withBlock(packed, light) : packed;
    }

    /** @return an entity's packed light, with any jumper's headlights' added, unless it is a jumper */
    public static int brighten(final Entity entity, final float partialTick, final int packed) {
        return entity instanceof PuddleJumperEntity || JumperLights.lit.isEmpty() ? packed
                : JumperLights.brighten(BlockPos.containing(entity.getLightProbePosition(partialTick)), packed);
    }

    /** Each tick: casts the cones again every few ticks, and has the world drawn again where they changed. */
    public static void tick(final Minecraft minecraft) {
        if (++JumperLights.sinceCast < JumperLights.CAST_TICKS) {
            return;
        }
        JumperLights.sinceCast = 0;
        final ClientLevel level = minecraft.level;
        final List<PuddleJumperEntity> casting = new ArrayList<>();
        final LongList from = new LongArrayList();
        if (level != null && minecraft.player != null) {
            for (final PuddleJumperEntity jumper : JumperIndex.near(level,
                    minecraft.player.getBoundingBox().inflate(JumperLights.SEEN))) {
                if (jumper.headlights() && JumperLights.needed(jumper)) {
                    casting.add(jumper);
                    from.add(JumperLights.lampBlock(jumper).asLong());
                    from.add(JumperLights.turned(jumper));
                }
            }
        }
        JumperLights.sinceRecast += JumperLights.CAST_TICKS;
        if (from.equals(JumperLights.castFrom) && (from.isEmpty()
                || JumperLights.sinceRecast < JumperLights.RECAST_TICKS)) {
            return;
        }
        JumperLights.castFrom = from;
        JumperLights.sinceRecast = 0;
        final Long2ByteMap cast = new Long2ByteOpenHashMap();
        if (level != null) {
            for (final PuddleJumperEntity jumper : casting) {
                JumperLights.cast(level, jumper, cast);
            }
        }
        final Long2ByteMap before = JumperLights.lit;
        if (cast.equals(before)) {
            return;
        }
        JumperLights.lit = cast.isEmpty() ? Long2ByteMaps.EMPTY_MAP : cast;
        // The sections whose light changed, and their neighbours, whose faces against them it lights
        final LongSet sections = new LongOpenHashSet();
        JumperLights.changed(before, cast, sections);
        JumperLights.changed(cast, before, sections);
        final LongIterator each = sections.iterator();
        while (each.hasNext()) {
            final long section = each.nextLong();
            minecraft.levelExtractor.setSectionDirty(SectionPos.x(section), SectionPos.y(section),
                    SectionPos.z(section));
        }
    }

    // Adds the sections round each block lit differently in one map than the other
    private static void changed(final Long2ByteMap from, final Long2ByteMap to, final LongSet sections) {
        for (final Long2ByteMap.Entry entry : from.long2ByteEntrySet()) {
            if (to.get(entry.getLongKey()) == entry.getByteValue()) {
                continue;
            }
            final BlockPos pos = BlockPos.of(entry.getLongKey());
            sections.add(SectionPos.asLong(pos));
            for (final Direction side : JumperLights.SIDES) {
                sections.add(SectionPos.asLong(pos.relative(side)));
            }
        }
    }

    // The block a jumper's lamps are in
    private static BlockPos lampBlock(final PuddleJumperEntity jumper) {
        return BlockPos.containing(jumper.position().add(JumperHull.toWorld(JumperLights.LAMPS,
                jumper.getYRot() * Mth.DEG_TO_RAD)));
    }

    // Which way a jumper faces, in steps of the cone's turn
    private static int turned(final PuddleJumperEntity jumper) {
        return Math.round(Mth.wrapDegrees(jumper.getYRot()) / JumperLights.TURN_STEP);
    }

    // Casts one jumper's cone of rays, keeping the brightest light each block gets
    private static void cast(final ClientLevel level, final PuddleJumperEntity jumper, final Long2ByteMap cast) {
        final float yaw = JumperLights.turned(jumper) * JumperLights.TURN_STEP;
        final Vec3 lamps = Vec3.atCenterOf(JumperLights.lampBlock(jumper));
        final AABB hull = jumper.getBoundingBox().inflate(PuddleJumperEntity.LENGTH / 2, 0,
                PuddleJumperEntity.LENGTH / 2);
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < JumperLights.ACROSS; i++) {
            final float across = (i / (JumperLights.ACROSS - 1F)) * 2 - 1;
            for (int j = 0; j < JumperLights.UP; j++) {
                final float up = (j / (JumperLights.UP - 1F)) * 2 - 1;
                // Brightest along the middle, fading to nothing at the cone's edge
                final float edge = Mth.clamp(1 - (across * across + up * up), 0, 1);
                if (edge <= 0) {
                    continue;
                }
                final Vec3 way = Vec3.directionFromRotation(JumperLights.DOWN - up * JumperLights.RISE,
                        yaw + across * JumperLights.SPREAD);
                JumperLights.ray(level, lamps, way, edge, hull, pos, cast);
            }
        }
    }

    // Marches a ray out from the lamps till it meets something that stops light, or its reach
    private static void ray(final ClientLevel level, final Vec3 from, final Vec3 way, final float edge,
            final AABB hull, final BlockPos.MutableBlockPos pos, final Long2ByteMap cast) {
        long last = Long.MIN_VALUE;
        for (double along = JumperLights.STEP; along <= JumperLights.REACH; along += JumperLights.STEP) {
            final double x = from.x + way.x * along;
            final double y = from.y + way.y * along;
            final double z = from.z + way.z * along;
            pos.set(Mth.floor(x), Mth.floor(y), Mth.floor(z));
            final long key = pos.asLong();
            if (key == last) {
                continue;
            }
            last = key;
            if (!level.isLoaded(pos)) {
                return;
            }
            final float fall = (float) (1 - along / JumperLights.REACH);
            final int light = Math.round(JumperLights.BRIGHTEST * edge * (0.35F + 0.65F * fall));
            if (light <= 0) {
                return;
            }
            final BlockState state = level.getBlockState(pos);
            // Not on the jumper itself, whose own light would change with every cast and flicker its hull
            if (!hull.contains(x, y, z)) {
                JumperLights.light(cast, key, light);
                for (final Direction side : Direction.values()) {
                    JumperLights.light(cast, BlockPos.offset(key, side), light - JumperLights.SPILL);
                }
            }
            if (state.canOcclude()) {
                return;
            }
        }
    }

    private static void light(final Long2ByteMap cast, final long key, final int light) {
        if (light > 0 && light > cast.get(key)) {
            cast.put(key, (byte) light);
        }
    }
}
