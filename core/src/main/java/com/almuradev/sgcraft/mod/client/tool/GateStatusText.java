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

package com.almuradev.sgcraft.mod.client.tool;

import com.almuradev.sgcraft.mod.tool.ToolPayloads.BookEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * What a gate is doing, as the tools' screens show it: a word in a color that says at a glance whether it can be
 * dialed, is busy, or is gone.
 */
final class GateStatusText {

    private GateStatusText() {
    }

    /** @param status {@link BookEntry#NO_GATE}, or a gate state's name in lower case */
    static Component of(final String status) {
        final String text;
        final ChatFormatting color;
        switch (status) {
            case BookEntry.NO_GATE -> {
                text = "No gate";
                color = ChatFormatting.RED;
            }
            case "idle" -> {
                text = "Idle";
                color = ChatFormatting.GREEN;
            }
            case "dialing" -> {
                text = "Dialing";
                color = ChatFormatting.YELLOW;
            }
            case "incoming" -> {
                text = "Incoming";
                color = ChatFormatting.GOLD;
            }
            case "opening" -> {
                text = "Opening";
                color = ChatFormatting.YELLOW;
            }
            case "connected" -> {
                text = "Connected";
                color = ChatFormatting.AQUA;
            }
            case "closing" -> {
                text = "Closing";
                color = ChatFormatting.GRAY;
            }
            default -> {
                text = status;
                color = ChatFormatting.GRAY;
            }
        }
        return Component.translatableWithFallback("screen.sgcraft.status." + status, text).withStyle(color);
    }
}
