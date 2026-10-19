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

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The puddle jumpers in each world, server's and client's alike, kept as they join and leave it. Jumpers are few, so
 * looking through a world's own is far cheaper than searching its entities, and costs nothing in a world with none:
 * the hooks every entity passes through each tick, to collide with hulls or keep dry inside them, ask here first.
 */
public final class JumperIndex {

    private static final Map<Level, Set<PuddleJumperEntity>> JUMPERS = new ConcurrentHashMap<>();

    private JumperIndex() {
    }

    // A jumper has joined its world
    static void add(final PuddleJumperEntity jumper) {
        JumperIndex.JUMPERS.computeIfAbsent(jumper.level(), level -> ConcurrentHashMap.newKeySet()).add(jumper);
    }

    // A jumper has left its world, so the world is let go once it has none
    static void remove(final PuddleJumperEntity jumper) {
        JumperIndex.JUMPERS.computeIfPresent(jumper.level(), (level, jumpers) -> {
            jumpers.remove(jumper);
            return jumpers.isEmpty() ? null : jumpers;
        });
    }

    /** @return whether a world has any jumper in it */
    public static boolean any(final Level level) {
        return JumperIndex.JUMPERS.containsKey(level);
    }

    /** @return the jumpers in a world whose boxes meet a box, which is empty if there are none */
    public static List<PuddleJumperEntity> near(final Level level, final AABB box) {
        final Set<PuddleJumperEntity> jumpers = JumperIndex.JUMPERS.get(level);
        if (jumpers == null) {
            return List.of();
        }
        List<PuddleJumperEntity> near = List.of();
        for (final PuddleJumperEntity jumper : jumpers) {
            if (!jumper.isRemoved() && jumper.getBoundingBox().intersects(box)) {
                if (near.isEmpty()) {
                    near = new ArrayList<>(2);
                }
                near.add(jumper);
            }
        }
        return near;
    }
}
