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

package com.almuradev.sgcraft.mod.shield;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The city shields standing on the server now, which explosions ask about: an explosion outside a shield doesn't
 * touch the blocks or the things inside it. A shield is counted while it is raised and its generator is loaded.
 */
public final class CityShields {

    private static final Set<CityShieldBlockEntity> RAISED = Collections.synchronizedSet(
            Collections.newSetFromMap(new WeakHashMap<>()));

    private CityShields() {
    }

    static void raised(final CityShieldBlockEntity shield) {
        if (shield.getLevel() != null && !shield.getLevel().isClientSide()) {
            CityShields.RAISED.add(shield);
        }
    }

    static void lowered(final CityShieldBlockEntity shield) {
        CityShields.RAISED.remove(shield);
    }

    /**
     * @param center where an explosion is
     * @param point where a block or thing it would reach is
     * @return whether a raised shield in that world keeps the explosion off the point: the point is inside it, and
     *     the explosion outside it
     */
    public static boolean protects(final Level level, final Vec3 center, final Vec3 point) {
        synchronized (CityShields.RAISED) {
            for (final CityShieldBlockEntity shield : CityShields.RAISED) {
                if (shield.getLevel() == level && !shield.isRemoved() && shield.active() && shield.inside(point)
                        && !shield.inside(center)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** @return whether any shield is raised, so explosions need not ask otherwise */
    public static boolean any() {
        return !CityShields.RAISED.isEmpty();
    }
}
