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

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** What a puddle jumper's renderer needs of it for a frame. */
public final class JumperRenderState extends EntityRenderState {

    public float yRot;
    public float hurtTime;
    public int hurtDir;
    public float damageTime;
    /** How far it banks into a turn, and tips its nose up as it climbs, in degrees. */
    public float bank;
    public float tilt;
    /** How far down the cargo door is, and how far out the drive pods are, from 0 to 1. */
    public float hatch;
    public float pods;
    /** How far out the drone bays are, from 0 to 1, and how many drones their racks show. */
    public float bays;
    public int drones;
    /** How hard its engines run, from 0, off, to 1, flat out. */
    public float thrust;
    /** Whether the headlights are on. */
    public boolean headlights;
    /** Whether it is in water. */
    public boolean inWater;
    /** How far its headlamps' beams reach before they meet a block, in blocks. */
    public float beamEnd;
    /** Whether its drive is on, lighting its lamps and the computer's screen. */
    public boolean powered;
    /** The computer's screen as it is now, if the jumper is near enough to the camera to show it. */
    public @Nullable Identifier picture;
}
