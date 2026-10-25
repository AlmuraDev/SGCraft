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

import com.almuradev.sgcraft.mod.entity.JumperIndex;
import com.almuradev.sgcraft.mod.entity.JumperShelter;
import com.almuradev.sgcraft.mod.entity.PuddleJumperEntity;
import net.minecraft.world.entity.Entity;

/**
 * What a puddle jumper's cloak hides besides the jumper: as on the show, it hides the whole cabin and everyone in it,
 * hatch open or not, from anyone outside; those inside the same jumper still see one another. Only clients call this.
 */
public final class JumperCloak {

    private JumperCloak() {
    }

    /** @return whether an entity rides in, or stands inside, a cloaked jumper this client's player is outside of */
    public static boolean hides(final Entity entity) {
        // Asked of every entity drawn, each frame, so a world with no jumper answers at once
        if (!JumperIndex.any(entity.level())) {
            return false;
        }
        final PuddleJumperEntity jumper = JumperShelter.shelter(entity);
        return jumper != null && jumper.cloak(1) > 0.5F && !JumperControls.viewerInside(jumper);
    }
}
