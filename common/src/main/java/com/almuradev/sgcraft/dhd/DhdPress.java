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

package com.almuradev.sgcraft.dhd;

import com.almuradev.sgcraft.SGCraft;
import com.almuradev.sgcraft.network.SGPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

/**
 * A player pressing a button of a DHD while its gate is active. Only the center does anything then: it shuts the
 * gate down.
 *
 * @param dhd where the DHD is
 * @param button the symbol's index, or {@link #CENTER}
 */
public record DhdPress(BlockPos dhd, int button) implements SGPacket {

    /** The button in the middle of the DHD. */
    public static final int CENTER = -1;

    public static final Type<DhdPress> TYPE = new Type<>(SGCraft.id("dhd_press"));

    public DhdPress(final FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readVarInt());
    }

    @Override
    public void write(final FriendlyByteBuf buf) {
        buf.writeBlockPos(this.dhd);
        buf.writeVarInt(this.button);
    }

    @Override
    public Type<DhdPress> type() {
        return DhdPress.TYPE;
    }
}
