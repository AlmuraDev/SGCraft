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

package com.almuradev.sgcraft.mod.address;

import com.almuradev.sgcraft.api.address.Symbol;
import com.almuradev.sgcraft.mod.SGConfig;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.address.AddressPlan.Problem;
import com.almuradev.sgcraft.mod.address.AddressPlan.Recorded;
import com.almuradev.sgcraft.mod.gate.GateStore;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Changes to the address settings, from a command or the settings screen. Each change is checked against the limits,
 * a server's {@link Symbol#COUNT} groups and a group's {@link Symbol#COUNT} worlds among them, then written to the
 * settings file. It takes effect when the server next starts, as it can change every address in the worlds it reaches.
 * Before a change is saved it can be looked at, to warn about each world whose gates' addresses it changes.
 */
public final class AddressChanges {

    /** What a change does. */
    public enum Kind {
        /** Sets the cell size, for every world or for one. */
        CELL_CHUNKS,
        /** Sets the point of origin, for every world or for one; an empty value means addresses without one. */
        POINT_OF_ORIGIN,
        /** Has a world use the server's point of origin again. */
        CLEAR_ORIGIN,
        /** Has a world use the server's cell size again. */
        CLEAR_CELL,
        /** Has a world use the server's cell size and point of origin again. */
        CLEAR_WORLD,
        /** Puts a world in a group. */
        WORLD_GROUP,
        /** Makes a group, with a symbol or, when the value is empty, one SGCraft chooses. */
        CREATE_GROUP,
        /** Removes a group; its worlds go to the default group. */
        REMOVE_GROUP,
        /** Sets a group's symbol, or, when the value is empty, has SGCraft choose one. */
        GROUP_SYMBOL,
        /** Turns the secret layout on or off for every world, by a value of {@code true} or {@code false}. */
        SECRET_LAYOUT
    }

    /**
     * One change.
     *
     * @param kind what it does
     * @param world the world it changes, or empty for every world
     * @param group the group it names, or {@code ""}
     * @param value its value: a number of chunks, one symbol, {@code true} or {@code false}, or {@code ""}
     */
    public record Change(Kind kind, Optional<Identifier> world, String group, String value) {
    }

    /**
     * What came of a change.
     *
     * @param saved whether the settings file has it
     * @param refusal why it wasn't allowed, if it wasn't
     * @param lines what it does when the server next starts
     * @param warnings for each world whose gates get new addresses from it, what that breaks
     */
    public record Outcome(boolean saved, Optional<Component> refusal, List<Component> lines,
            List<Component> warnings) {

        static Outcome refused(final Component why) {
            return new Outcome(false, Optional.of(why), List.of(), List.of());
        }
    }

    private AddressChanges() {
    }

    /**
     * Checks a change and works out what it does, then writes it to the settings file if {@code save} is set.
     *
     * @param save whether to write it; when not, the outcome only says what it would do
     */
    public static Outcome run(final MinecraftServer server, final Change change, final boolean save) {
        return AddressChanges.batch(server, List.of(change), save).outcome();
    }

    /**
     * What came of a batch of changes.
     *
     * @param refused the index of the change that wasn't allowed, or -1 if every one was
     * @param outcome what the changes do together, or why the one refused wasn't allowed
     */
    public record Batch(int refused, Outcome outcome) {
    }

    /**
     * Checks changes made one after another, as the settings screen saves them, and works out what they do
     * together, then writes them all to the settings file if {@code save} is set. Each is checked against the
     * settings as the ones before it leave them; the first refused stops the rest.
     *
     * @param save whether to write them; when not, the outcome only says what they would do
     */
    public static Batch batch(final MinecraftServer server, final List<Change> changes, final boolean save) {
        final SGAddressing addressing = SGServer.require().addressing();
        final GateStore store = server.getDataStorage().computeIfAbsent(GateStore.TYPE);
        final Recorded recorded = SGAddressing.recorded(store);
        final List<ResourceKey<Level>> worlds = new ArrayList<>();
        server.getAllLevels().forEach(level -> worlds.add(level.dimension()));
        final AddressSettings saved = SGConfig.load().addresses();
        final AddressPlan first = AddressPlan.of(saved, recorded, worlds);
        Consumer<JsonObject> edits = json -> { };
        AddressSettings pending = saved;
        AddressPlan before = first;
        for (int index = 0; index < changes.size(); index++) {
            final Change change = changes.get(index);
            final Optional<Component> refusal = AddressChanges.refusal(change, pending, before);
            if (refusal.isPresent()) {
                return new Batch(index, Outcome.refused(refusal.get()));
            }
            final Consumer<JsonObject> with = edits.andThen(json -> AddressChanges.edit(json, change));
            final AddressSettings next = SGConfig.previewEdit(with).addresses();
            final AddressPlan after = AddressPlan.of(next, recorded, worlds);
            // Anything the change makes impossible, such as a group past the limit, is refused
            for (final Problem problem : after.problems()) {
                if (!before.problems().contains(problem)) {
                    return new Batch(index, Outcome.refused(problem.text()));
                }
            }
            edits = with;
            pending = next;
            before = after;
        }
        final List<Component> lines = new ArrayList<>();
        final List<Component> warnings = new ArrayList<>();
        for (final ResourceKey<Level> world : worlds) {
            final WorldAddress was = WorldAddress.of(saved.layoutOf(world), first, world);
            final WorldAddress will = WorldAddress.of(pending.layoutOf(world), before, world);
            if (was.equals(will)) {
                continue;
            }
            final WorldAddress now = addressing.worldAddress(world);
            final String id = world.identifier().toString();
            if (will.equals(now)) {
                lines.add(AddressChanges.text("keeps", "%s keeps the addresses it has now", id));
                continue;
            }
            lines.add(AddressChanges.text("next", "%s will have %s when the server next starts", id,
                    will.describe()));
            final long gates = store.gateCount(world);
            if (gates > 0) {
                warnings.add(AddressChanges.warning(id, gates, now.changesEveryAddress(will)));
                if (save) {
                    SGCraft.LOG.warn("Gate addresses in {} ({} gates) change when the server next starts: {}", id,
                            gates, now.changesTo(will));
                }
            }
        }
        changes.forEach(change -> AddressChanges.groupLines(change, lines));
        if (save && !changes.isEmpty() && !SGConfig.edit(edits)) {
            return new Batch(-1, Outcome.refused(AddressChanges.text("not_saved", "The settings file couldn't be "
                    + "written. The server log says why")));
        }
        return new Batch(-1, new Outcome(save, Optional.empty(), lines, warnings));
    }

    /** @return the edit to the settings file's contents that makes changes, one after another */
    public static Consumer<JsonObject> edits(final List<Change> changes) {
        return json -> changes.forEach(change -> AddressChanges.edit(json, change));
    }

    private static Component warning(final String world, final long gates, final boolean every) {
        final MutableComponent text = every
                ? AddressChanges.text("warning", "Warning: after a restart, every gate in %s (%s gates) gets a new "
                        + "address. Address books and saved addresses will stop reaching them. Change it back before "
                        + "the server restarts to keep the old addresses", world, gates)
                : AddressChanges.text("warning_naming", "Warning: after a restart, the gates in %s (%s gates) get new "
                        + "addresses of 8 and 9 symbols. Address books keep those, so they will stop reaching these "
                        + "gates. Change it back before the server restarts to keep the old addresses", world, gates);
        return text.withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
    }

    // What a change to a group does, besides what it does to its worlds
    private static void groupLines(final Change change, final List<Component> lines) {
        switch (change.kind()) {
            case CREATE_GROUP -> lines.add(change.value().isEmpty()
                    ? AddressChanges.text("group_created", "Group %s will exist when the server next starts, with "
                            + "a symbol SGCraft chooses", change.group())
                    : AddressChanges.text("group_created_symbol", "Group %s will exist when the server next starts, "
                            + "with the symbol %s", change.group(), change.value()));
            case REMOVE_GROUP -> lines.add(AddressChanges.text("group_removed", "Group %s will be gone when the "
                    + "server next starts. Its worlds go to the default group", change.group()));
            case GROUP_SYMBOL -> lines.add(change.value().isEmpty()
                    ? AddressChanges.text("group_symbol_auto", "Group %s will have a symbol SGCraft chooses when the "
                            + "server next starts", change.group())
                    : AddressChanges.text("group_symbol", "Group %s will have the symbol %s when the server next "
                            + "starts", change.group(), change.value()));
            default -> {
            }
        }
    }

    // Why a change can't be made, checked against the settings as the server would next start with them
    private static Optional<Component> refusal(final Change change, final AddressSettings pending,
            final AddressPlan plan) {
        final String group = change.group();
        final boolean exists = group.equals(AddressSettings.DEFAULT_GROUP) || pending.groups().containsKey(group);
        switch (change.kind()) {
            case CELL_CHUNKS -> {
                return AddressChanges.cellChunks(change.value()).isPresent() ? Optional.empty()
                        : Optional.of(AddressChanges.text("bad_cell_chunks", "A cell is 1 to %s chunks across",
                                AddressLayout.MAX_CELL_CHUNKS));
            }
            case CREATE_GROUP -> {
                if (!AddressSettings.isGroupName(group)) {
                    return Optional.of(AddressChanges.text("bad_group_name", "A group's name is 1 to %s lowercase "
                            + "letters, digits and underscores", AddressSettings.MAX_GROUP_NAME));
                }
                if (exists) {
                    return Optional.of(AddressChanges.text("group_exists", "There is already a group named %s",
                            group));
                }
                if (plan.groups().size() >= Symbol.COUNT) {
                    return Optional.of(AddressChanges.text("group_limit", "A server can have only %s groups, and "
                            + "this one has them all", Symbol.COUNT));
                }
                return AddressChanges.symbolTaken(change.value(), group, plan);
            }
            case GROUP_SYMBOL -> {
                if (!exists) {
                    return Optional.of(AddressChanges.noGroup(group));
                }
                if (group.equals(AddressSettings.DEFAULT_GROUP) && change.value().isEmpty()) {
                    return Optional.of(AddressChanges.text("default_needs_symbol", "The default group always has a "
                            + "symbol. Give it one; @ is its usual symbol"));
                }
                return AddressChanges.symbolTaken(change.value(), group, plan);
            }
            case REMOVE_GROUP -> {
                if (group.equals(AddressSettings.DEFAULT_GROUP)) {
                    return Optional.of(AddressChanges.text("default_stays", "The default group can't be removed"));
                }
                return exists ? Optional.empty() : Optional.of(AddressChanges.noGroup(group));
            }
            case SECRET_LAYOUT -> {
                return change.value().equals("true") || change.value().equals("false") ? Optional.empty()
                        : Optional.of(AddressChanges.text("bad_secret_layout", "The secret layout is true or false"));
            }
            case WORLD_GROUP -> {
                if (!exists) {
                    return Optional.of(AddressChanges.noGroup(group));
                }
                final List<ResourceKey<Level>> members = plan.worldsIn(group);
                final boolean member = change.world().isPresent() && members.stream()
                        .anyMatch(world -> world.identifier().equals(change.world().get()));
                if (!member && members.size() >= Symbol.COUNT) {
                    return Optional.of(AddressChanges.text("group_at_limit", "Group %s already has %s worlds, the "
                            + "most a group can have", group, Symbol.COUNT));
                }
                return Optional.empty();
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    // A cell size as a change gives it, if it is a whole number of chunks a cell can be
    private static Optional<Integer> cellChunks(final String value) {
        try {
            final int chunks = Integer.parseInt(value);
            return chunks >= 1 && chunks <= AddressLayout.MAX_CELL_CHUNKS ? Optional.of(chunks) : Optional.empty();
        } catch (final NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Optional<Component> symbolTaken(final String value, final String group, final AddressPlan plan) {
        if (value.isEmpty()) {
            return Optional.empty();
        }
        return plan.groups().stream()
                .filter(other -> !other.name().equals(group)
                        && String.valueOf(other.symbol().character()).equals(value))
                .findFirst()
                .map(other -> AddressChanges.text("symbol_in_use", "Group %s already has the symbol %s", other.name(),
                        value));
    }

    private static Component noGroup(final String group) {
        return AddressChanges.text("no_group", "There is no group named %s", group);
    }

    private static MutableComponent text(final String key, final String fallback, final Object... args) {
        return Component.translatableWithFallback("message.sgcraft.addresses." + key, fallback, args);
    }

    /** Makes a change to the settings file's contents. */
    public static void edit(final JsonObject root, final Change change) {
        final JsonObject addresses = AddressChanges.section(root, "addresses");
        final Optional<JsonObject> world = change.world()
                .map(id -> AddressChanges.section(AddressChanges.section(addresses, "worlds"), id.toString()));
        switch (change.kind()) {
            case CELL_CHUNKS -> world.orElse(addresses).addProperty("cell_chunks", Integer.parseInt(change.value()));
            case POINT_OF_ORIGIN -> world.orElse(addresses).addProperty("point_of_origin", change.value());
            case CLEAR_ORIGIN -> world.ifPresent(own -> own.remove("point_of_origin"));
            case CLEAR_CELL -> world.ifPresent(own -> own.remove("cell_chunks"));
            case CLEAR_WORLD -> world.ifPresent(own -> {
                own.remove("cell_chunks");
                own.remove("point_of_origin");
            });
            case WORLD_GROUP -> world.ifPresent(own -> {
                if (change.group().equals(AddressSettings.DEFAULT_GROUP)) {
                    own.remove("group");
                } else {
                    own.addProperty("group", change.group());
                }
            });
            case CREATE_GROUP -> {
                final JsonObject group = new JsonObject();
                if (!change.value().isEmpty()) {
                    group.addProperty("symbol", change.value());
                }
                AddressChanges.section(addresses, "groups").add(change.group(), group);
            }
            case REMOVE_GROUP -> {
                AddressChanges.section(addresses, "groups").remove(change.group());
                // Its worlds go to the default group, rather than naming a group that is gone
                for (final Map.Entry<String, JsonElement> entry : AddressChanges.section(addresses, "worlds")
                        .entrySet()) {
                    if (entry.getValue() instanceof final JsonObject own && own.has("group")
                            && own.get("group").getAsString().equals(change.group())) {
                        own.remove("group");
                    }
                }
            }
            case SECRET_LAYOUT -> addresses.addProperty("secret_layout", Boolean.parseBoolean(change.value()));
            case GROUP_SYMBOL -> {
                final JsonObject group = AddressChanges.section(AddressChanges.section(addresses, "groups"),
                        change.group());
                if (change.value().isEmpty()) {
                    group.remove("symbol");
                } else {
                    group.addProperty("symbol", change.value());
                }
            }
        }
        AddressChanges.prune(addresses, "worlds");
        AddressChanges.prune(addresses, "groups");
    }

    // A section of the file, made if it isn't there
    private static JsonObject section(final JsonObject in, final String name) {
        if (in.get(name) instanceof final JsonObject own) {
            return own;
        }
        final JsonObject made = new JsonObject();
        in.add(name, made);
        return made;
    }

    // Drops worlds left with no values of their own, and the section if nothing is left in it. An empty group stays,
    // as a group without a symbol of its own is still a group
    private static void prune(final JsonObject addresses, final String name) {
        if (!(addresses.get(name) instanceof final JsonObject section)) {
            return;
        }
        if (name.equals("worlds")) {
            section.entrySet().removeIf(entry -> entry.getValue() instanceof final JsonObject own && own.isEmpty());
        }
        if (section.isEmpty()) {
            addresses.remove(name);
        }
    }
}
