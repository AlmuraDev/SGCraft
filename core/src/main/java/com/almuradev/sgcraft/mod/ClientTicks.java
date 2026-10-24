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

package com.almuradev.sgcraft.mod;

import com.almuradev.sgcraft.mod.block.entity.DialingComputerBlockEntity;
import com.almuradev.sgcraft.mod.block.entity.StargateBaseBlockEntity;
import com.almuradev.sgcraft.mod.generator.NaquadahGeneratorBlockEntity;

import java.util.function.Consumer;

/**
 * The sounds blocks keep going on a client, set by the client's setup, so a block's client tick can run them without
 * naming client classes, which a dedicated server doesn't have. Until they are set, ticking does nothing.
 */
public final class ClientTicks {

    private static Consumer<StargateBaseBlockEntity> gate = base -> {
    };
    private static Consumer<DialingComputerBlockEntity> computer = alarm -> {
    };
    private static Consumer<NaquadahGeneratorBlockEntity> generator = hum -> {
    };

    private ClientTicks() {
    }

    /** Keeps a gate's looping sounds going. Only call this on a client. */
    public static void gate(final StargateBaseBlockEntity base) {
        ClientTicks.gate.accept(base);
    }

    /** Keeps a dialing computer's alarm sounding while it should. Only call this on a client. */
    public static void computer(final DialingComputerBlockEntity computer) {
        ClientTicks.computer.accept(computer);
    }

    /** Keeps a naquadah generator's hum going while it runs. Only call this on a client. */
    public static void generator(final NaquadahGeneratorBlockEntity generator) {
        ClientTicks.generator.accept(generator);
    }

    /** Sets what each tick runs. The client's setup calls this. */
    public static void set(final Consumer<StargateBaseBlockEntity> gate,
            final Consumer<DialingComputerBlockEntity> computer,
            final Consumer<NaquadahGeneratorBlockEntity> generator) {
        ClientTicks.gate = gate;
        ClientTicks.computer = computer;
        ClientTicks.generator = generator;
    }
}
