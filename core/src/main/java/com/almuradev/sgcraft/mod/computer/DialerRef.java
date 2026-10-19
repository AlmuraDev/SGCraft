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

import com.almuradev.sgcraft.mod.block.entity.DialingComputerBlockEntity;
import com.almuradev.sgcraft.mod.entity.PuddleJumperEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * Which dialing computer a screen works, as the server and its screen name it to each other: one standing in the
 * world, by its middle lower part, or the one aboard the puddle jumper the player rides. The jumper isn't named by its
 * entity, which changes as it goes through a gate to another world; the player's own seat says which it is.
 */
public sealed interface DialerRef {

    /** The computer aboard the puddle jumper the player rides. */
    DialerRef ABOARD = new Aboard();

    /** A dialing computer standing in the world, by its middle lower part. */
    record Block(BlockPos pos) implements DialerRef {
    }

    /** The computer aboard the puddle jumper the player rides. */
    record Aboard() implements DialerRef {
    }

    /** @return the computer standing at a place */
    static DialerRef of(final BlockPos pos) {
        return new Block(pos.immutable());
    }

    /**
     * @param player the player working the computer, whose seat names a jumper's computer
     * @return the computer, if it is there, in the player's world
     */
    default @Nullable DialingTerminal resolve(final Player player) {
        return switch (this) {
            case Block(final BlockPos pos) -> player.level().getBlockEntity(pos)
                    instanceof final DialingComputerBlockEntity computer ? computer : null;
            case Aboard() -> player.getVehicle() instanceof final PuddleJumperEntity jumper ? jumper.dialer() : null;
        };
    }

    static DialerRef read(final FriendlyByteBuf buf) {
        return buf.readBoolean() ? DialerRef.of(buf.readBlockPos()) : DialerRef.ABOARD;
    }

    default void write(final FriendlyByteBuf buf) {
        switch (this) {
            case Block(final BlockPos pos) -> {
                buf.writeBoolean(true);
                buf.writeBlockPos(pos);
            }
            case Aboard() -> buf.writeBoolean(false);
        }
    }
}
