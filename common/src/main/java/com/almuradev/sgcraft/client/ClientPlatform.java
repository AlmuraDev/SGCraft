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

package com.almuradev.sgcraft.client;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ServiceLoader;

/**
 * What common client code needs from the mod loader it runs on. Each loader project provides one implementation,
 * named in its META-INF/services file.
 */
public interface ClientPlatform {

    static ClientPlatform get() {
        return Holder.INSTANCE;
    }

    /**
     * Submits a node to the phase that draws the level's solid features. Its feature renderer must be registered
     * with the loader.
     */
    void submitSolid(final SubmitNodeCollector collector, final SubmitNode node);

    /** Sends a payload from this client to the server, which must have registered it with the loader. */
    void sendToServer(final CustomPacketPayload payload);

    final class Holder {

        private static final ClientPlatform INSTANCE = ServiceLoader.load(ClientPlatform.class,
                        ClientPlatform.class.getClassLoader())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No " + ClientPlatform.class.getName()
                        + " for this mod loader"));

        private Holder() {
        }
    }
}
