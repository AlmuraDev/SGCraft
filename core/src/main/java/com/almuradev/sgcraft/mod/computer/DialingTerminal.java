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

import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.block.entity.DialingComputerBlockEntity;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A dialing computer, wherever it is: one standing on a desk, or the one aboard a puddle jumper. Its screen and the
 * server work any of them the same way: it is linked to a gate, dials it in a {@link ComputerDialMode}, keeps whether
 * its alarm is off, and keeps the addresses last dialed from it.
 */
public interface DialingTerminal {

    /** @return the base of the gate the computer dials, in the computer's world, if it is linked */
    Optional<BlockPos> linked();

    /** @return how the computer dials */
    ComputerDialMode mode();

    /** @return whether the alarm has been turned off */
    boolean alarmOff();

    /** @return the addresses last dialed from the computer, newest first */
    List<DialingComputerBlockEntity.Dialed> history();

    /**
     * @return a name for the computer's picture, unique among the computers a client sees, made only of characters a
     *     texture's path may have
     */
    String pictureName();

    /** @return whether the computer is gone, broken or packed up */
    boolean gone();

    /**
     * The gate the computer dials: its linked gate while that lasts and is in reach, or else the nearest in reach,
     * which it links to then. Only on the server.
     *
     * @return the gate, if there is one in reach
     */
    Optional<Stargate> gate();

    /** Has the computer dial another way from now on. */
    void setMode(ComputerDialMode mode);

    /** Turns the alarm off, or on again; it stays that way until changed. */
    void setAlarmOff(boolean off);

    /**
     * Puts an address dialed from the computer at the top of its history, taking it from lower down if it was there,
     * and lets go of the oldest beyond the server's history size.
     */
    void remember(String address, boolean connected);

    /** @return a history with an address dialed put at its top, as {@link #remember} keeps it */
    static List<DialingComputerBlockEntity.Dialed> remembering(final List<DialingComputerBlockEntity.Dialed> history,
            final String address, final boolean connected) {
        final List<DialingComputerBlockEntity.Dialed> next = new ArrayList<>();
        next.add(new DialingComputerBlockEntity.Dialed(address, connected));
        history.stream().filter(dialed -> !dialed.address().equals(address)).forEach(next::add);
        return List.copyOf(next.subList(0, Math.min(next.size(), ComputerSettings.current().historySize())));
    }
}
