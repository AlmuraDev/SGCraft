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
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

/**
 * The puddle jumper's own keys, beside the movement keys, jump and sprint that fly it: descending, and switching its
 * headlights and working its cargo door, as the dashboard's buttons do. Each loader registers them, in their own
 * category of the controls.
 */
public final class JumperKeys {

    /** The keys' category, which each loader registers as it registers categories. */
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(SGCraft.id("puddle_jumper"));
    public static final KeyMapping DESCEND = new KeyMapping("key.sgcraft.jumper_descend", InputConstants.KEY_LALT,
            JumperKeys.CATEGORY);
    public static final KeyMapping HEADLIGHTS = new KeyMapping("key.sgcraft.jumper_headlights", InputConstants.KEY_H,
            JumperKeys.CATEGORY);
    public static final KeyMapping POWER = new KeyMapping("key.sgcraft.jumper_power", InputConstants.KEY_I,
            JumperKeys.CATEGORY);
    public static final KeyMapping PODS = new KeyMapping("key.sgcraft.jumper_pods", InputConstants.KEY_R,
            JumperKeys.CATEGORY);
    public static final KeyMapping DOOR = new KeyMapping("key.sgcraft.jumper_door", InputConstants.KEY_G,
            JumperKeys.CATEGORY);
    // The drones: the bays swung out or in, the next hostile locked as the target, and a drone launched at it
    public static final KeyMapping ARM = new KeyMapping("key.sgcraft.jumper_arm", InputConstants.KEY_Y,
            JumperKeys.CATEGORY);
    public static final KeyMapping TARGET = new KeyMapping("key.sgcraft.jumper_target", InputConstants.KEY_Z,
            JumperKeys.CATEGORY);
    public static final KeyMapping FIRE = new KeyMapping("key.sgcraft.jumper_fire", InputConstants.KEY_V,
            JumperKeys.CATEGORY);
    public static final KeyMapping FRIENDLY_FIRE = new KeyMapping("key.sgcraft.jumper_friendly_fire",
            InputConstants.KEY_N, JumperKeys.CATEGORY);
    // Opens the computer aboard, from the pilot's seat or the one beside it
    public static final KeyMapping COMPUTER = new KeyMapping("key.sgcraft.jumper_computer", InputConstants.KEY_K,
            JumperKeys.CATEGORY);
    // The cloak generator, which the pilot runs as a shield or as a cloak, one at a time
    public static final KeyMapping SHIELD = new KeyMapping("key.sgcraft.jumper_shield", InputConstants.KEY_J,
            JumperKeys.CATEGORY);
    public static final KeyMapping CLOAK = new KeyMapping("key.sgcraft.jumper_cloak", InputConstants.KEY_X,
            JumperKeys.CATEGORY);

    private JumperKeys() {
    }
}
