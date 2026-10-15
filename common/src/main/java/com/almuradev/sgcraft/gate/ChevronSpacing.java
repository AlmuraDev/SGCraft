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

package com.almuradev.sgcraft.gate;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;

/**
 * How far apart a gate's chevrons stand. They are 40 degrees apart, but on a gate with more than seven, whose bottom
 * two stand in its bottom row, camouflage over the bottom row's corners draws them closer together, as the original
 * did, so the camouflage doesn't hide them: 33 degrees apart over slabs and 30 over full blocks.
 *
 * <p>The rest of the gate follows its chevrons: {@link #squeeze} moves every angle round the ring so each chevron's
 * place lands where the chevron now stands, what is between them closes up with them, and the ring's bottom, behind
 * the camouflage, stretches to close the ring. A gate's dialing is worked out at 40 degrees apart, and drawn through
 * the same squeeze.</p>
 */
public final class ChevronSpacing {

    /** How far apart chevrons stand with nothing drawing them together, in degrees. */
    public static final double STANDARD = 40;

    // The spacing over nothing, slabs and full blocks
    private static final double[] SPACINGS = {ChevronSpacing.STANDARD, 33, 30};
    // The camouflage slots at the bottom row's corners
    private static final int LEFT_CORNER = 0;
    private static final int RIGHT_CORNER = 4;
    // How far from the top, either way, the last chevron stands at the standard spacing
    private static final double LAST = 4 * ChevronSpacing.STANDARD;

    private ChevronSpacing() {
    }

    /**
     * @param chevrons how many chevrons the gate has
     * @param camouflage the blocks over the gate's bottom row, left to right as seen from in front
     * @return how far apart the gate's chevrons stand, in degrees
     */
    public static double of(final int chevrons, final Container camouflage) {
        if (chevrons <= 7 || camouflage.getContainerSize() <= ChevronSpacing.RIGHT_CORNER) {
            return ChevronSpacing.STANDARD;
        }
        final int level = Math.max(ChevronSpacing.level(camouflage.getItem(ChevronSpacing.LEFT_CORNER)),
                ChevronSpacing.level(camouflage.getItem(ChevronSpacing.RIGHT_CORNER)));
        return ChevronSpacing.SPACINGS[level];
    }

    // How much a block of camouflage covers: nothing, a slab, or a full block
    private static int level(final ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item)) {
            return 0;
        }
        final Block block = item.getBlock();
        if (block instanceof SlabBlock) {
            return 1;
        }
        return block.defaultBlockState().isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) ? 2 : 0;
    }

    /** @return the angle a chevron position stands at, in degrees anticlockwise from the right, the top at 90 */
    public static double angle(final int position, final double spacing) {
        return 90 - (position - ChevronOrder.TOP) * spacing;
    }

    /**
     * Where an angle round the gate lands when its chevrons are drawn together. Angles are in degrees anticlockwise
     * from the right, the top at 90, and may be any number of turns round; the top stays where it is.
     *
     * @param angle the angle at the standard spacing
     * @param spacing how far apart the chevrons stand
     * @return the angle at that spacing
     */
    public static double squeeze(final double angle, final double spacing) {
        if (spacing == ChevronSpacing.STANDARD) {
            return angle;
        }
        // From the top, in (-180, 180], and how many turns round that was
        final double turns = Math.floor((angle - 90 + 180) / 360);
        final double fromTop = angle - 90 - turns * 360;
        final double away = Math.abs(fromTop);
        final double squeezed;
        if (away <= ChevronSpacing.LAST) {
            squeezed = away * spacing / ChevronSpacing.STANDARD;
        } else {
            // Past the last chevron, out to the bottom, stretched to close the ring
            final double last = 4 * spacing;
            squeezed = last + (away - ChevronSpacing.LAST) * (180 - last) / (180 - ChevronSpacing.LAST);
        }
        return 90 + Math.copySign(squeezed, fromTop) + turns * 360;
    }
}
