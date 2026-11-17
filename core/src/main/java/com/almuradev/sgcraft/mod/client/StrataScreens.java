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

package com.almuradev.sgcraft.mod.client;

import com.almuradev.sgcraft.mod.Platform;

/**
 * Whether SGCraft's screens are drawn with Strata, laid out as SGCraft 2.3's MalisisCore screens were, rather than the
 * standard ones: when Strata is installed and the player turned them on ({@link SGClientConfig#strataScreens()}).
 *
 * <p>Strata is optional, so nothing here names its classes. They live in {@code client.strata} and are reached only
 * once this says to use them, so a client without Strata never loads them.</p>
 */
public final class StrataScreens {

    /** Strata's mod id. */
    public static final String MOD_ID = "strata";

    private static Boolean installed;

    private StrataScreens() {
    }

    /** @return whether Strata is installed */
    public static boolean installed() {
        if (StrataScreens.installed == null) {
            StrataScreens.installed = Platform.get().isModLoaded(StrataScreens.MOD_ID);
        }
        return StrataScreens.installed;
    }

    /** @return whether to open the Strata screens: Strata is installed and the player turned them on */
    public static boolean enabled() {
        return StrataScreens.installed() && SGClientConfig.get().strataScreens();
    }
}
