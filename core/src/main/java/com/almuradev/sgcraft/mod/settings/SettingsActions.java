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

package com.almuradev.sgcraft.mod.settings;

import com.almuradev.sgcraft.api.address.Symbol;
import com.almuradev.sgcraft.mod.SGConfig;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.access.SGGateAccess;
import com.almuradev.sgcraft.mod.address.AddressChanges.Outcome;
import com.almuradev.sgcraft.mod.address.AddressChanges;
import com.almuradev.sgcraft.mod.address.AddressPlan;
import com.almuradev.sgcraft.mod.address.AddressSettings.WorldLayout;
import com.almuradev.sgcraft.mod.address.AddressSettings;
import com.almuradev.sgcraft.mod.address.SGAddressing;
import com.almuradev.sgcraft.mod.address.WorldAddress;
import com.almuradev.sgcraft.mod.gate.GateStore;
import com.almuradev.sgcraft.mod.network.SGNetwork;
import com.almuradev.sgcraft.mod.settings.SettingsPayloads.Change;
import com.almuradev.sgcraft.mod.settings.SettingsPayloads.Confirm;
import com.almuradev.sgcraft.mod.settings.SettingsPayloads.Edits;
import com.almuradev.sgcraft.mod.settings.SettingsPayloads.Group;
import com.almuradev.sgcraft.mod.settings.SettingsPayloads.Option;
import com.almuradev.sgcraft.mod.settings.SettingsPayloads.Request;
import com.almuradev.sgcraft.mod.settings.SettingsPayloads.SetOption;
import com.almuradev.sgcraft.mod.settings.SettingsPayloads.State;
import com.almuradev.sgcraft.mod.settings.SettingsPayloads.World;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The server side of the settings screen. Only operators may see or change the settings; anyone else is told so.
 * A screen holds its changes until the player saves them, sending them to be previewed as they are made: the answer
 * shows the settings as they would be, without saving anything. Saving makes them, then sends every other operator
 * online the settings again on the next tick, so a screen they have open shows the change.
 */
public final class SettingsActions {

    // The tick each player was last sent the settings on, so the one who saved isn't sent them twice
    private static final Map<UUID, Integer> SENT = new HashMap<>();
    // The tick changes were last saved on, while a refresh is waiting for the next tick
    private static int changedOn = -1;

    private SettingsActions() {
    }

    /** Opens the settings screen for a player, as {@code /sgcraft settings screen} does. */
    public static void open(final ServerPlayer player) {
        SettingsActions.send(player, true, List.of(), json -> { }, 0, -1, -1, false, false);
    }

    /** Answers a screen that has just opened, or wants the settings again. */
    public static void request(final ServerPlayer player, final Request request) {
        SettingsActions.send(player, false, List.of(), json -> { }, 0, -1, -1, false, false);
    }

    /**
     * Previews or saves the changes a screen holds. The first that isn't allowed is sent back refused, for the
     * screen to drop. Changes that give gates new addresses are sent back to be confirmed before they are saved,
     * unless they already have been.
     */
    public static void edits(final ServerPlayer player, final Edits edits) {
        if (!SGGateAccess.isOperator(player)) {
            SettingsActions.send(player, false, List.of(), json -> { }, 0, -1, -1, false, false);
            return;
        }
        final MinecraftServer server = player.level().getServer();
        final List<AddressChanges.Change> changes = edits.changes().stream().map(SettingsActions::change).toList();
        final Preview preview = SettingsActions.preview(server, changes, edits.options());
        if (!edits.save() || preview.refusal().isPresent()) {
            SettingsActions.send(player, false, preview.refusal().stream().toList(), preview.edit(),
                    edits.sequence(), preview.refused(), preview.refusedOption(), false, false);
            return;
        }
        if (!preview.warnings().isEmpty() && !edits.confirmed()) {
            SGNetwork.send(player, new Confirm(edits, SettingsActions.json(preview.warnings())));
            return;
        }
        final List<Component> messages = new ArrayList<>();
        if (!changes.isEmpty()) {
            final Outcome outcome = AddressChanges.batch(server, changes, true).outcome();
            outcome.refusal().ifPresent(messages::add);
            messages.addAll(outcome.lines());
            messages.addAll(outcome.warnings());
        }
        for (final SetOption option : edits.options()) {
            messages.addAll(SettingsCatalog.set(SGServer.require(), option.key(), option.value()).messages());
        }
        SettingsActions.send(player, false, messages, json -> { }, edits.sequence(), -1, -1, true, false);
        SettingsActions.changed();
    }

    // What changes would do: the edit to the settings file that makes them, or the first that isn't allowed and why,
    // with the edit that makes those before it, and the warnings about worlds whose gates get new addresses
    private record Preview(int refused, int refusedOption, Optional<Component> refusal, Consumer<JsonObject> edit,
            List<Component> warnings) {
    }

    private static Preview preview(final MinecraftServer server, final List<AddressChanges.Change> changes,
            final List<SetOption> options) {
        final AddressChanges.Batch batch = AddressChanges.batch(server, changes, false);
        if (batch.refused() >= 0) {
            return new Preview(batch.refused(), -1, batch.outcome().refusal(),
                    AddressChanges.edits(changes.subList(0, batch.refused())), List.of());
        }
        Consumer<JsonObject> edit = AddressChanges.edits(changes);
        for (int index = 0; index < options.size(); index++) {
            final SetOption option = options.get(index);
            final SettingsCatalog.Checked checked = SettingsCatalog.check(SGConfig.previewEdit(edit), option.key(),
                    option.value());
            if (checked.refusal().isPresent()) {
                return new Preview(-1, index, checked.refusal(), edit, batch.outcome().warnings());
            }
            edit = edit.andThen(checked.edit());
        }
        return new Preview(-1, -1, Optional.empty(), edit, batch.outcome().warnings());
    }

    private static AddressChanges.Change change(final Change change) {
        final Optional<Identifier> world = change.world().isEmpty() ? Optional.empty()
                : Optional.ofNullable(Identifier.tryParse(change.world()));
        return new AddressChanges.Change(change.kind(), world, change.group(), change.value());
    }

    // Has every operator online sent the settings on the next tick, but the player who saved, who has been sent them
    // already with what the save did. Saves in one tick are sent once
    private static void changed() {
        final Optional<SGServer> current = SGServer.current();
        if (current.isEmpty() || SettingsActions.changedOn >= 0) {
            return;
        }
        final MinecraftServer server = current.get().server();
        SettingsActions.changedOn = server.getTickCount();
        current.get().nextTick(() -> {
            final int tick = SettingsActions.changedOn;
            SettingsActions.changedOn = -1;
            for (final ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
                final Integer sent = SettingsActions.SENT.get(player.getUUID());
                if (SGGateAccess.isOperator(player) && (sent == null || sent != tick)) {
                    SettingsActions.send(player, false, List.of(), json -> { }, 0, -1, -1, false, true);
                }
            }
            SettingsActions.SENT.clear();
        });
    }

    // Sends the settings as they are now, and as they will be after a restart with the edit made
    private static void send(final ServerPlayer player, final boolean open, final List<Component> messages,
            final Consumer<JsonObject> edit, final int reply, final int refused, final int refusedOption,
            final boolean saved, final boolean refresh) {
        SettingsActions.SENT.put(player.getUUID(), player.level().getServer().getTickCount());
        if (!SGGateAccess.isOperator(player)) {
            SGNetwork.send(player, State.refused(open));
            return;
        }
        final MinecraftServer server = player.level().getServer();
        final SGAddressing addressing = SGServer.require().addressing();
        final GateStore store = server.getDataStorage().computeIfAbsent(GateStore.TYPE);
        final SGConfig file = SGConfig.previewEdit(edit);
        final AddressSettings pending = file.addresses();
        final List<ResourceKey<Level>> keys = new ArrayList<>();
        server.getAllLevels().forEach(level -> keys.add(level.dimension()));
        final AddressPlan next = AddressPlan.of(pending, SGAddressing.recorded(store), keys);
        final List<World> worlds = new ArrayList<>();
        for (final ServerLevel level : server.getAllLevels()) {
            final ResourceKey<Level> key = level.dimension();
            final WorldAddress now = addressing.worldAddress(key);
            final WorldAddress after = WorldAddress.of(pending.layoutOf(key), next, key);
            final Optional<WorldLayout> layout = Optional.ofNullable(pending.worlds().get(key.identifier()));
            worlds.add(new World(key.identifier().toString(), store.gateCount(key),
                    layout.flatMap(WorldLayout::pointOfOrigin).isPresent(), now.layout().pointOfOrigin(),
                    after.layout().pointOfOrigin(), now.group(), after.group(), SettingsActions.text(now.symbol()),
                    SettingsActions.text(after.symbol()), layout.flatMap(WorldLayout::cellChunks).isPresent(),
                    now.layout().cellChunks(), after.layout().cellChunks()));
        }
        final AddressPlan running = addressing.plan();
        final List<Group> groups = new ArrayList<>();
        for (final AddressPlan.Group group : next.groups()) {
            groups.add(new Group(group.name(), SettingsActions.text(running.group(group.name())
                    .map(AddressPlan.Group::symbol)), String.valueOf(group.symbol().character()), group.automatic(),
                    next.worldsIn(group.name()).size()));
        }
        for (final AddressPlan.Group group : running.groups()) {
            if (next.group(group.name()).isEmpty()) {
                groups.add(new Group(group.name(), String.valueOf(group.symbol().character()), "", group.automatic(),
                        0));
            }
        }
        final List<Component> all = new ArrayList<>(messages);
        next.problems().forEach(problem -> all.add(problem.text()));
        SGNetwork.send(player, new State(open, true, addressing.settings().pointOfOrigin(), pending.pointOfOrigin(),
                addressing.settings().cellChunks(), pending.cellChunks(), worlds, groups,
                SettingsActions.options(SettingsCatalog.running(SGServer.require()), file),
                SettingsActions.json(all), reply, refused, refusedOption, saved, refresh));
    }

    private static List<Option> options(final SGConfig running, final SGConfig file) {
        final List<Option> options = new ArrayList<>();
        for (final SettingsCatalog.Setting setting : SettingsCatalog.all()) {
            options.add(new Option(setting.key(), setting.page().id(), SettingsActions.json(setting.page().title()),
                    SettingsActions.json(setting.section()), SettingsActions.json(setting.label()),
                    SettingsActions.json(setting.tip()), setting.type(), setting.min(), setting.aboveMin(),
                    setting.max(), setting.live(), setting.valueIn(running), setting.valueIn(file),
                    setting.defaultValue()));
        }
        return options;
    }

    private static String json(final Component component) {
        return SettingsActions.json(List.of(component)).getFirst();
    }

    private static String text(final Optional<Symbol> symbol) {
        return symbol.map(each -> String.valueOf(each.character())).orElse("");
    }

    private static List<String> json(final List<Component> components) {
        return components.stream()
                .map(component -> ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, component)
                        .result().map(Object::toString).orElse(component.getString()))
                .toList();
    }
}
