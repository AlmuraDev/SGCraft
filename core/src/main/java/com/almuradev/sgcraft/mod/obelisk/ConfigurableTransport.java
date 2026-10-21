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

package com.almuradev.sgcraft.mod.obelisk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Optional;

/**
 * Something that carries players to its partner and opens the obelisk screen with the configurator, as 2.2.0's
 * Asgard obelisks and transport rings do: its link, its owner, who may use and configure it, and its power if it
 * needs any.
 */
public interface ConfigurableTransport {

    /** @return where it is */
    BlockPos getBlockPos();

    /** @return who owns it and who may use and configure it */
    TransportAccess transportAccess();

    /** Changes who may use and configure it, and saves it. */
    void setAccess(final boolean everyone, final List<ObeliskAccess> entries);

    /** @return whether a player may link it and change who may use it */
    default boolean mayConfigure(final Player player) {
        return this.transportAccess().mayConfigure(player);
    }

    /** Tells a player they may not configure it. */
    void refuseConfigure(final ServerPlayer player);

    /** @return where its partner is, if it is linked */
    Optional<GlobalPos> link();

    /** Unlinks it, and its partner from it. */
    void unlinkBoth();

    /** @return whether its screen shows power, as a ring platform's does */
    default boolean powered() {
        return false;
    }

    /** @return the gate energy it holds, in whole units */
    default long energy() {
        return 0;
    }

    /** @return the most gate energy it holds */
    default long energyCapacity() {
        return 0;
    }

    /** @return how many pieces of naquadah it holds to burn */
    default int naquadah() {
        return 0;
    }

    /** @return how many times it could send to its partner on what it holds and its naquadah */
    default long usesLeft() {
        return 0;
    }
}
