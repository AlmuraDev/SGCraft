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

package com.almuradev.sgcraft.mod.network;

import com.almuradev.sgcraft.mod.computer.ComputerConfigurator;
import com.almuradev.sgcraft.mod.computer.ComputerPayloads;
import com.almuradev.sgcraft.mod.computer.DialingComputers;
import com.almuradev.sgcraft.mod.dhd.DhdActions;
import com.almuradev.sgcraft.mod.dhd.DhdDial;
import com.almuradev.sgcraft.mod.dhd.DhdPress;
import com.almuradev.sgcraft.mod.gate.GateBaseInfo;
import com.almuradev.sgcraft.mod.malp.MalpConfigurator;
import com.almuradev.sgcraft.mod.malp.MalpLinks;
import com.almuradev.sgcraft.mod.malp.MalpPayloads;
import com.almuradev.sgcraft.mod.obelisk.ObeliskActions;
import com.almuradev.sgcraft.mod.obelisk.ObeliskPayloads;
import com.almuradev.sgcraft.mod.settings.SettingsActions;
import com.almuradev.sgcraft.mod.settings.SettingsPayloads;
import com.almuradev.sgcraft.mod.shield.CityShieldActions;
import com.almuradev.sgcraft.mod.shield.ShieldConsoles;
import com.almuradev.sgcraft.mod.shield.ShieldPayloads;
import com.almuradev.sgcraft.mod.tool.ExplorerDialer;
import com.almuradev.sgcraft.mod.tool.ExplorerPayloads;
import com.almuradev.sgcraft.mod.tool.ToolActions;
import com.almuradev.sgcraft.mod.tool.ToolPayloads;
import com.almuradev.sgcraft.mod.view.RemoteViewPackets;

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
        // Remote views, streamed to the player watching one
        SGNetwork.clientbound(RemoteViewPackets.Begin.TYPE, RemoteViewPackets.Begin::new);
        SGNetwork.clientbound(RemoteViewPackets.Sections.TYPE, RemoteViewPackets.Sections::new);
        SGNetwork.clientbound(RemoteViewPackets.Heights.TYPE, RemoteViewPackets.Heights::new);
        SGNetwork.clientbound(RemoteViewPackets.BlockEntities.TYPE, RemoteViewPackets.BlockEntities::new);
        SGNetwork.clientbound(RemoteViewPackets.Entities.TYPE, RemoteViewPackets.Entities::new);
        SGNetwork.clientbound(RemoteViewPackets.Frame.TYPE, RemoteViewPackets.Frame::new);
        SGNetwork.clientbound(RemoteViewPackets.Own.TYPE, RemoteViewPackets.Own::new);
        SGNetwork.clientbound(RemoteViewPackets.End.TYPE, RemoteViewPackets.End::new);
        // The MALP console: its state out, its buttons and controls in
        SGNetwork.clientbound(MalpPayloads.ConsoleState.TYPE, MalpPayloads.ConsoleState::new);
        SGNetwork.serverbound(MalpPayloads.ConsoleAction.TYPE, MalpPayloads.ConsoleAction::new, MalpLinks::action);
        SGNetwork.serverbound(MalpPayloads.Drive.TYPE, MalpPayloads.Drive::new, MalpLinks::drive);
        SGNetwork.clientbound(MalpPayloads.ConsoleUse.TYPE, MalpPayloads.ConsoleUse::new);
        SGNetwork.clientbound(MalpPayloads.MonitorState.TYPE, MalpPayloads.MonitorState::new);
        // A MALP's configurator: its settings out, the changes and unpairing in
        SGNetwork.clientbound(MalpPayloads.ConfigState.TYPE, MalpPayloads.ConfigState::new);
        SGNetwork.serverbound(MalpPayloads.ConfigApply.TYPE, MalpPayloads.ConfigApply::new, MalpConfigurator::apply);
        SGNetwork.serverbound(MalpPayloads.ConfigUnpair.TYPE, MalpPayloads.ConfigUnpair::new,
                MalpConfigurator::unpair);
        // The dialing computer: its screen opened and closed, its buttons in
        SGNetwork.clientbound(ComputerPayloads.Screen.TYPE, ComputerPayloads.Screen::new);
        SGNetwork.clientbound(ComputerPayloads.Notice.TYPE, ComputerPayloads.Notice::new);
        SGNetwork.serverbound(ComputerPayloads.Action.TYPE, ComputerPayloads.Action::new, DialingComputers::action);
        SGNetwork.clientbound(ComputerPayloads.ConfigState.TYPE, ComputerPayloads.ConfigState::new);
        SGNetwork.serverbound(ComputerPayloads.ConfigApply.TYPE, ComputerPayloads.ConfigApply::new,
                ComputerConfigurator::apply);
        // The explorer dialer: its list out, its buttons in
        SGNetwork.clientbound(ExplorerPayloads.State.TYPE, ExplorerPayloads.State::new);
        SGNetwork.serverbound(ExplorerPayloads.Action.TYPE, ExplorerPayloads.Action::new, ExplorerDialer::action);
        // The shield console: its state out, its buttons in
        SGNetwork.clientbound(ObeliskPayloads.Beam.TYPE, ObeliskPayloads.Beam::new);
        SGNetwork.clientbound(ObeliskPayloads.State.TYPE, ObeliskPayloads.State::new);
        SGNetwork.serverbound(ObeliskPayloads.Apply.TYPE, ObeliskPayloads.Apply::new, ObeliskActions::apply);
        SGNetwork.clientbound(ShieldPayloads.ConsoleState.TYPE, ShieldPayloads.ConsoleState::new);
        SGNetwork.serverbound(ShieldPayloads.ConsoleAction.TYPE, ShieldPayloads.ConsoleAction::new,
                ShieldConsoles::action);
        // The city shield generator: its state out, its sizes and buttons in
        SGNetwork.clientbound(ShieldPayloads.CityState.TYPE, ShieldPayloads.CityState::new);
        SGNetwork.serverbound(ShieldPayloads.CityAction.TYPE, ShieldPayloads.CityAction::new,
                CityShieldActions::action);
        // The settings screen: the settings out, the edits it holds in, and edits to confirm out
        SGNetwork.clientbound(SettingsPayloads.State.TYPE, SettingsPayloads.State::new);
        SGNetwork.clientbound(SettingsPayloads.Confirm.TYPE, SettingsPayloads.Confirm::new);
        SGNetwork.serverbound(SettingsPayloads.Request.TYPE, SettingsPayloads.Request::new, SettingsActions::request);
        SGNetwork.serverbound(SettingsPayloads.Edits.TYPE, SettingsPayloads.Edits::new, SettingsActions::edits);
    }
}
