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

package com.almuradev.sgcraft.api.gate;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

/**
 * A condition a gate type puts on where its gates can be placed, such as only underground or only in certain
 * biomes. A gate type lists its rules in {@link GateType#placementRules()}.
 *
 * <p>Implementations must check every rule of the type at every spot they consider, in both
 * {@linkplain PlacementMode placement modes}. A spot where any rule fails is skipped, and if no spot passes, the
 * placement fails with {@link PlacementResult.Status#RULE_FAILED} and the first failure's reason. A rule must not
 * change the world.</p>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface PlacementRule {

    /**
     * @param level the world the gate would go in
     * @param placement the placement, at the spot being considered
     * @return why the gate can't go there, or empty if it can
     */
    Optional<Component> check(final ServerLevel level, final GatePlacement placement);
}
