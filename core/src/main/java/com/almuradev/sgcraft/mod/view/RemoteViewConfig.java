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

package com.almuradev.sgcraft.mod.view;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * How much of the world around a remote view's source is streamed to its viewer.
 *
 * @param horizontalRadius how far out from the camera blocks are sent, in blocks
 * @param verticalRadius how far above and below the camera blocks are sent, in blocks
 * @param bytesPerTick about how much block data one viewer is sent each tick; a fresh view fills in at this rate
 */
public record RemoteViewConfig(int horizontalRadius, int verticalRadius, int bytesPerTick) {

    public static final RemoteViewConfig DEFAULTS = new RemoteViewConfig(32, 24, 48 * 1024);

    public static final Codec<RemoteViewConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(8, 128).optionalFieldOf("horizontal_radius", RemoteViewConfig.DEFAULTS.horizontalRadius)
                    .forGetter(RemoteViewConfig::horizontalRadius),
            Codec.intRange(8, 96).optionalFieldOf("vertical_radius", RemoteViewConfig.DEFAULTS.verticalRadius)
                    .forGetter(RemoteViewConfig::verticalRadius),
            Codec.intRange(4096, 1024 * 1024).optionalFieldOf("bytes_per_tick", RemoteViewConfig.DEFAULTS.bytesPerTick)
                    .forGetter(RemoteViewConfig::bytesPerTick)
    ).apply(instance, RemoteViewConfig::new));
}
