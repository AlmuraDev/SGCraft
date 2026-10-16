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

package com.almuradev.sgcraft.mod.malp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;

/**
 * A MALP's own settings, which its owner and operators change with a configurator or a command. The MALP keeps them
 * when it goes through a gate and when it is packed up as an item. Each setting has a default, which a MALP saved
 * before the setting existed takes.
 *
 * @param powerRequired whether the MALP runs on its battery; when false it uses no gate energy and works with a flat
 *     battery or none
 */
public record MalpOptions(boolean powerRequired) {

    /** The settings of a new MALP. */
    public static final MalpOptions DEFAULTS = new MalpOptions(true);

    public static final Codec<MalpOptions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("power_required", MalpOptions.DEFAULTS.powerRequired)
                    .forGetter(MalpOptions::powerRequired)
    ).apply(instance, MalpOptions::new));

    public MalpOptions(final FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

    public void write(final FriendlyByteBuf buf) {
        buf.writeBoolean(this.powerRequired);
    }

    /** @return these settings with {@link #powerRequired()} changed */
    public MalpOptions withPowerRequired(final boolean powerRequired) {
        return new MalpOptions(powerRequired);
    }
}
