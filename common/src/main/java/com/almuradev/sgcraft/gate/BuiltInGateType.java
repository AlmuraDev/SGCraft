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

import com.almuradev.sgcraft.api.gate.FixtureBuffer;
import com.almuradev.sgcraft.api.gate.GateSize;
import com.almuradev.sgcraft.api.gate.GateType;
import net.minecraft.network.chat.Component;

import java.util.Objects;

/**
 * One of the gate types SGCraft registers itself. For now each is built at the standard size only, with no fixtures.
 */
public final class BuiltInGateType implements GateType {

    private final Component name;

    public BuiltInGateType(final Component name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    @Override
    public Component name() {
        return this.name;
    }

    @Override
    public boolean supports(final GateSize size) {
        return size.equals(GateSize.STANDARD);
    }

    @Override
    public FixtureBuffer fixtures(final GateSize size) {
        return new FixtureBuffer();
    }
}
