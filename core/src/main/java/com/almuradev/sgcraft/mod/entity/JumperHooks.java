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

import net.minecraft.world.entity.Entity;

import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * What the puddle jumper needs from the client, set by the client's setup, so the jumper can ask for it without
 * naming client classes, which a dedicated server doesn't have. Until they are set, no one flies it and no one is
 * inside it.
 */
public final class JumperHooks {

    private static BiFunction<PuddleJumperEntity, Entity, JumperInput> input = (jumper, pilot) -> JumperInput.NONE;
    private static Predicate<PuddleJumperEntity> viewerInside = jumper -> false;

    private JumperHooks() {
    }

    /**
     * @return the controls the pilot holds, or none if the pilot isn't this client's player. Only call this on a
     *     client.
     */
    public static JumperInput input(final PuddleJumperEntity jumper, final Entity pilot) {
        return JumperHooks.input.apply(jumper, pilot);
    }

    /** @return whether this client's player is inside the jumper, seated or standing. Only call this on a client. */
    public static boolean viewerInside(final PuddleJumperEntity jumper) {
        return JumperHooks.viewerInside.test(jumper);
    }

    /** Sets how the pilot's controls are read and who is inside. The client's setup calls this. */
    public static void set(final BiFunction<PuddleJumperEntity, Entity, JumperInput> input,
            final Predicate<PuddleJumperEntity> viewerInside) {
        JumperHooks.input = input;
        JumperHooks.viewerInside = viewerInside;
    }
}
