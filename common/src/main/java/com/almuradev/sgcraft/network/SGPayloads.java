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

package com.almuradev.sgcraft.network;

import com.almuradev.sgcraft.dhd.DhdActions;
import com.almuradev.sgcraft.dhd.DhdDial;
import com.almuradev.sgcraft.dhd.DhdPress;
import com.almuradev.sgcraft.gate.GateBaseInfo;
import com.almuradev.sgcraft.tool.ToolActions;
import com.almuradev.sgcraft.tool.ToolPayloads;

/**
 * Declares every payload with {@link SGNetwork}, once.
 */
final class SGPayloads {

    private static boolean declared;

    private SGPayloads() {
    }

    static synchronized void declare() {
        if (SGPayloads.declared) {
            return;
        }
        SGPayloads.declared = true;
        // The DHD's screen
        SGNetwork.serverbound(DhdPress.TYPE, DhdPress::new, DhdActions::press);
        SGNetwork.serverbound(DhdDial.TYPE, DhdDial::new, DhdActions::dial);
        // A gate's base screen
        SGNetwork.clientbound(GateBaseInfo.TYPE, GateBaseInfo::new);
        // The handheld tools: their states out, their actions in
        SGNetwork.clientbound(ToolPayloads.PddState.TYPE, ToolPayloads.PddState::new);
        SGNetwork.clientbound(ToolPayloads.GdoState.TYPE, ToolPayloads.GdoState::new);
        SGNetwork.clientbound(ToolPayloads.ConfiguratorState.TYPE, ToolPayloads.ConfiguratorState::new);
        SGNetwork.clientbound(ToolPayloads.TabletState.TYPE, ToolPayloads.TabletState::new);
        SGNetwork.serverbound(ToolPayloads.PddAction.TYPE, ToolPayloads.PddAction::new, ToolActions::pdd);
        SGNetwork.serverbound(ToolPayloads.GdoAction.TYPE, ToolPayloads.GdoAction::new, ToolActions::gdo);
        SGNetwork.serverbound(ToolPayloads.TabletAction.TYPE, ToolPayloads.TabletAction::new, ToolActions::tablet);
        SGNetwork.serverbound(ToolPayloads.ConfiguratorApply.TYPE, ToolPayloads.ConfiguratorApply::new,
                ToolActions::configurator);
    }
}
