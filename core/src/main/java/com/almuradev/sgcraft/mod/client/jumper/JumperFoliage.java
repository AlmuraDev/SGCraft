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
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Grass, flowers, saplings and the like the hull of a puddle jumper stands in, as the client draws them: not at all,
 * or as the water they stand in, so they don't poke up through its floor or show in its cabin. Only blocks with
 * nothing to collide with are hidden, which the hull stands in rather than on. Each tick the blocks within each
 * jumper near the player are found again, and the world is drawn again where they changed. Nothing in the world is
 * changed. Only clients call this.
 */
public final class JumperFoliage {

    // How far round the player jumpers' hulls hide what they stand in, in blocks
    private static final double SEEN = 96;
    // How far beyond the hull's sides a block's middle may be and still be hidden, as a plant's leaves reach out of
    // its block, in blocks
    private static final double SLACK = 0.3;

    // The hidden blocks, by packed position, for any thread drawing the world to read; replaced whole, never changed
    private static volatile LongSet hidden = LongSets.EMPTY_SET;

    private JumperFoliage() {
    }

    /** @return whether a block is hidden within a jumper's hull */
    public static boolean hidden(final BlockPos pos) {
        final LongSet now = JumperFoliage.hidden;
        return !now.isEmpty() && now.contains(pos.asLong());
    }

    /** Each tick: finds the blocks within the jumpers' hulls, and has the world drawn again where they changed. */
    public static void tick(final Minecraft minecraft) {
        final ClientLevel level = minecraft.level;
        final LongSet found = new LongOpenHashSet();
        if (level != null && minecraft.player != null) {
            for (final PuddleJumperEntity jumper : JumperIndex.near(level,
                    minecraft.player.getBoundingBox().inflate(JumperFoliage.SEEN))) {
                JumperFoliage.find(level, jumper, found);
            }
        }
        final LongSet before = JumperFoliage.hidden;
        if (found.equals(before)) {
            return;
        }
        JumperFoliage.hidden = found.isEmpty() ? LongSets.EMPTY_SET : found;
        final LongSet sections = new LongOpenHashSet();
        JumperFoliage.changed(before, found, sections);
        JumperFoliage.changed(found, before, sections);
        final LongIterator each = sections.iterator();
        while (each.hasNext()) {
            final long section = each.nextLong();
            minecraft.levelExtractor.setSectionDirty(SectionPos.x(section), SectionPos.y(section),
                    SectionPos.z(section));
        }
    }

    // Adds the sections of the blocks in one set but not the other
    private static void changed(final LongSet from, final LongSet to, final LongSet sections) {
        final LongIterator each = from.iterator();
        while (each.hasNext()) {
            final long key = each.nextLong();
            if (!to.contains(key)) {
                sections.add(SectionPos.asLong(BlockPos.of(key)));
            }
        }
    }

    // The blocks with nothing to collide with whose middles lie within a jumper's hull
    private static void find(final ClientLevel level, final PuddleJumperEntity jumper, final LongSet found) {
        final double reach = PuddleJumperEntity.LENGTH / 2 + JumperFoliage.SLACK;
        final AABB around = new AABB(jumper.getX() - reach, jumper.getY(), jumper.getZ() - reach,
                jumper.getX() + reach, jumper.getY() + PuddleJumperEntity.HEIGHT, jumper.getZ() + reach);
        final double halfWidth = PuddleJumperEntity.WIDTH / 2.0 + JumperFoliage.SLACK;
        for (final BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(around.minX, around.minY, around.minZ),
                BlockPos.containing(around.maxX, around.maxY, around.maxZ))) {
            final Vec3 local = JumperHull.toLocal(Vec3.atCenterOf(pos).subtract(jumper.position()), jumper.getYRot());
            if (Math.abs(local.x) > halfWidth || Math.abs(local.z) > reach || local.y > PuddleJumperEntity.HEIGHT) {
                continue;
            }
            final BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.getRenderShape() != RenderShape.MODEL
                    || !state.getCollisionShape(level, pos).isEmpty()) {
                continue;
            }
            found.add(pos.asLong());
        }
    }
}
