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

package com.almuradev.sgcraft.mod.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.Map;

/**
 * Gate plazas that villages build among their houses. Each plaza joins a pool of houses as one more piece to choose
 * from, so the pieces already in the pool are still chosen as before. A village builds one gate at most: a plaza
 * near a gate already built stays empty.
 *
 * @param weight how often a plaza is chosen against the pool's other pieces, each of which has its own weight; 0
 *     adds no plazas
 * @param pools the plaza template for each pool of houses
 * @param tokra whether a Tok'ra stands in front of a village's gate once it is built
 */
public record VillageGates(int weight, Map<Identifier, Identifier> pools, boolean tokra) {

    public static final Codec<VillageGates> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, 150).optionalFieldOf("weight", 2).forGetter(VillageGates::weight),
            Codec.unboundedMap(Identifier.CODEC, Identifier.CODEC).optionalFieldOf("pools", Map.of())
                    .forGetter(VillageGates::pools),
            Codec.BOOL.optionalFieldOf("tokra", true).forGetter(VillageGates::tokra)
    ).apply(instance, VillageGates::new));
}
