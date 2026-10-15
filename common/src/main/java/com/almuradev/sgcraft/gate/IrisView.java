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

import com.almuradev.sgcraft.api.SGCraftRegistries;
import com.almuradev.sgcraft.api.gate.IrisState;
import com.almuradev.sgcraft.api.gate.IrisType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;

import java.util.Optional;

/**
 * A gate's iris as clients see it: its type, the state it is in, and the game time it got there, from which a client
 * works out how far open it is.
 *
 * @param type the kind of iris
 * @param state where it is in its movement
 * @param since when it got to that state, in game time
 */
public record IrisView(ResourceKey<IrisType> type, IrisState state, long since) {

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.store("type", ResourceKey.codec(SGCraftRegistries.IRIS_TYPE), this.type);
        tag.putString("state", this.state.name());
        tag.putLong("since", this.since);
        return tag;
    }

    public static Optional<IrisView> load(final CompoundTag tag) {
        final Optional<ResourceKey<IrisType>> type = tag.read("type", ResourceKey.codec(SGCraftRegistries.IRIS_TYPE));
        final Optional<IrisState> state = tag.getString("state").flatMap(name -> {
            try {
                return Optional.of(IrisState.valueOf(name));
            } catch (final IllegalArgumentException e) {
                return Optional.empty();
            }
        });
        if (type.isEmpty() || state.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new IrisView(type.get(), state.get(), tag.getLongOr("since", 0)));
    }
}
