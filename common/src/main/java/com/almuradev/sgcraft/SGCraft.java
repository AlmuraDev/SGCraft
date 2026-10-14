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

package com.almuradev.sgcraft;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts the mod. Each loader calls {@link #init()} once, from its own entry point.
 */
public final class SGCraft {

    public static final String MOD_ID = "sgcraft";
    public static final String MOD_NAME = "SGCraft";
    public static final Logger LOG = LoggerFactory.getLogger(SGCraft.MOD_NAME);

    private SGCraft() {
    }

    /**
     * @param path a name in the mod's namespace
     * @return {@code sgcraft:<path>}
     */
    public static Identifier id(final String path) {
        return Identifier.fromNamespaceAndPath(SGCraft.MOD_ID, path);
    }

    public static void init() {
        SGCraft.LOG.info("Starting {} on {}", SGCraft.MOD_NAME, Platform.get().loaderName());
    }
}
