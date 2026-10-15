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

package com.almuradev.sgcraft.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A packet SGCraft sends: it writes itself to a buffer, and a constructor taking a buffer reads it back, field for
 * field in the same order. {@link SGNetwork} turns the pair into whatever each loader needs.
 */
public interface SGPacket extends CustomPacketPayload {

    /** Writes this packet's fields, in the order its reading constructor reads them. */
    void write(final FriendlyByteBuf buf);

    /** Writes a list: its size, then each element. */
    static <T> void writeList(final FriendlyByteBuf buf, final List<T> list,
            final BiConsumer<FriendlyByteBuf, T> writer) {
        buf.writeVarInt(list.size());
        for (final T element : list) {
            writer.accept(buf, element);
        }
    }

    /** Reads a list {@link #writeList} wrote. */
    static <T> List<T> readList(final FriendlyByteBuf buf, final Function<FriendlyByteBuf, T> reader) {
        final int size = buf.readVarInt();
        final List<T> list = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            list.add(reader.apply(buf));
        }
        return list;
    }
}
