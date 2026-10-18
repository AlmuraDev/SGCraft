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

package com.almuradev.sgcraft.mod.computer;

import com.almuradev.sgcraft.api.gate.DialMode;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * How a dialing computer dials: as its gate's {@code chevrons_lock_on_dial} property says, or slowly or quickly
 * whatever the gate says. Each computer keeps its own.
 */
public enum ComputerDialMode implements StringRepresentable {
    /** As the gate's {@code chevrons_lock_on_dial} property says, as a DHD dials it. */
    GATE,
    /** Slowly: the ring turns to each symbol and each chevron locks in turn, as the SGC dials. */
    SLOW,
    /** Quickly: every chevron locks at once. */
    FAST;

    public static final Codec<ComputerDialMode> CODEC = StringRepresentable.fromEnum(ComputerDialMode::values);

    /** @return how a gate is dialed in this mode */
    public DialMode dialMode(final Stargate gate) {
        return switch (this) {
            case GATE -> gate.get(GateProperties.CHEVRONS_LOCK_ON_DIAL).orElse(false) ? DialMode.QUICK
                    : DialMode.STANDARD;
            case SLOW -> DialMode.STANDARD;
            case FAST -> DialMode.QUICK;
        };
    }

    /** @return the next mode, as the screen's button steps through them */
    public ComputerDialMode next() {
        return ComputerDialMode.values()[(this.ordinal() + 1) % ComputerDialMode.values().length];
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
