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

package com.almuradev.sgcraft.mod.client.gate;

import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.api.gate.GateTypes;
import com.almuradev.sgcraft.mod.block.entity.StargateBaseBlockEntity;
import com.almuradev.sgcraft.mod.gate.ChevronOrder;
import com.almuradev.sgcraft.mod.gate.ChevronSpacing;
import com.almuradev.sgcraft.mod.gate.DialView;
import com.almuradev.sgcraft.mod.gate.GateOrientation;
import com.almuradev.sgcraft.mod.gate.GateShape;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;

/**
 * What the gate renderer needs to know about a gate for one frame.
 */
public final class StargateBaseRenderState extends BlockEntityRenderState {

    public boolean formed;
    // How big the gate is, which its drawing is scaled to
    public GateShape shape = GateShape.STANDARD;
    public ResourceKey<GateType> gateType = GateTypes.MILKY_WAY;
    public Direction facing = Direction.NORTH;
    // Whether the gate stands or lies flat, which turns its drawing from the base's frame into its own
    public GateOrientation orientation = GateOrientation.STANDING;
    public int chevrons = 7;
    // Whether each chevron position's chevron is locked; see ChevronOrder
    public boolean[] locked = new boolean[ChevronOrder.POSITIONS];
    // What the gate is doing, and the time to draw it at, in ticks with the partial tick
    public DialView view = DialView.IDLE;
    public double time;
    // How far forward of the gate each point of the open wormhole's surface is, by spoke and ring; null while closed
    public double @Nullable [][] horizon;
    // How far open the iris is, from 0 shut to 1 open; 1 when there is no iris
    public float irisAperture = 1;
    // Whether the iris is a shield, which is drawn as light, and lets the horizon show through it
    public boolean shield;
    // Whether the open wormhole's surface lets a little show through
    public boolean transparent = true;
    // How far apart the chevrons stand, in degrees, which camouflage over the bottom row's corners can draw closer
    public double spacing = ChevronSpacing.STANDARD;
    // The camouflage over the bottom row, from left to right as seen from in front, and where each is from the base;
    // a slot is null when it has none
    public final @Nullable MovingBlockRenderState[] camouflage =
            new MovingBlockRenderState[StargateBaseBlockEntity.CAMOUFLAGE_SLOTS];
    public final BlockPos[] camouflageOffsets = new BlockPos[StargateBaseBlockEntity.CAMOUFLAGE_SLOTS];
    // The colored glows round the gate's lit parts, and over its open horizon, filled again each frame
    public final GateGlow.Glows glows = new GateGlow.Glows();
    public final GateGlow.Glows horizonGlow = new GateGlow.Glows();
}
