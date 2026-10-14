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

package com.almuradev.sgcraft.gate;

import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.api.gate.GateTypes;
import com.almuradev.sgcraft.registry.RegistrySink;
import net.minecraft.network.chat.Component;

/**
 * Registers the gate types the API requires.
 */
public final class SGGateTypes {

    private SGGateTypes() {
    }

    public static void register(final RegistrySink<GateType> sink) {
        sink.register(GateTypes.MILKY_WAY, new BuiltInGateType(Component.translatable("gate_type.sgcraft.milky_way")));
        sink.register(GateTypes.PEGASUS, new BuiltInGateType(Component.translatable("gate_type.sgcraft.pegasus")));
        sink.register(GateTypes.UNIVERSE, new BuiltInGateType(Component.translatable("gate_type.sgcraft.universe")));
    }
}
