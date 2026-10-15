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

package com.almuradev.sgcraft.mod.gate;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.network.SGPacket;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.GateInfo;
import net.minecraft.network.FriendlyByteBuf;

/**
 * What a gate's base screen shows: the gate's address and type. The server sends it just after opening the base's
 * menu, or on its own to a player who may only look at the address.
 *
 * @param menu the id of the menu it goes with, or {@link #NO_MENU} for a screen showing only the address
 * @param gate the gate; its address is empty when the gate hides it
 */
public record GateBaseInfo(int menu, GateInfo gate) implements SGPacket {

    /** {@link #menu()} when the player may only look at the address. */
    public static final int NO_MENU = -1;

    public static final Type<GateBaseInfo> TYPE = new Type<>(SGCraft.id("gate_base_info"));

    public GateBaseInfo(final FriendlyByteBuf buf) {
        this(buf.readVarInt(), new GateInfo(buf));
    }

    @Override
    public void write(final FriendlyByteBuf buf) {
        buf.writeVarInt(this.menu);
        this.gate.write(buf);
    }

    @Override
    public Type<GateBaseInfo> type() {
        return GateBaseInfo.TYPE;
    }
}
