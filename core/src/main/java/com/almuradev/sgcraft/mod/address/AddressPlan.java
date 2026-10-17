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
import com.almuradev.sgcraft.mod.address.AddressSettings.GroupSettings;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The groups of worlds and the symbols that name them and their worlds, worked out from the server's settings and
 * the symbols given when the server last started.
 *
 * <p>The default group's symbol is the one its settings give, or {@code @}. Groups with a symbol of their own take it
 * next, in the order the settings list them, and then each group without one takes the symbol it had last time, if
 * it is free, or the first symbol no group has. A server has at most {@link Symbol#COUNT} groups. A group past that,
 * or one whose symbol another group already has, is left out, and its worlds are in the default group.</p>
 *
 * <p>Each world keeps the symbol it was given in its group. A world new to its group takes the symbol it had in its
 * old group if that is free, or the first free one. A group names at most {@link Symbol#COUNT} worlds; a world past
 * that has no symbol, so no address of 8 or 9 symbols reaches it. A world the server doesn't have now keeps its
 * symbol, so the world gets it back when it returns.</p>
 *
 * @param groups every group, the default one first, then the rest in the order the settings list them
 * @param worldGroups the name of the group each world is in, for each world the server has or gave a symbol
 * @param worldSymbols the symbol that names each world within its group, for each world that has one
 * @param problems what in the settings couldn't be followed, or was followed in a way the server's owner should know
 */
public record AddressPlan(List<Group> groups, Map<ResourceKey<Level>, String> worldGroups,
        Map<ResourceKey<Level>, Symbol> worldSymbols, List<Problem> problems) {

    /**
     * A group.
     *
     * @param name its name in the settings
     * @param symbol the symbol that names it
     * @param automatic whether SGCraft chose the symbol, as the settings give the group none
     */
    public record Group(String name, Symbol symbol, boolean automatic) {

        /** @return whether this is the group of every world the settings put in no other */
        public boolean isDefault() {
            return this.name.equals(AddressSettings.DEFAULT_GROUP);
        }
    }

    /**
     * Something in the settings that couldn't be followed as written.
     *
     * @param error whether the settings asked for something impossible, as opposed to something allowed that the
     *     server's owner should hear about
     * @param key the message's translation key
     * @param fallback the message in English, with {@code %s} for each argument
     * @param args the message's arguments
     */
    public record Problem(boolean error, String key, String fallback, List<String> args) {

        /** @return the message, to show a player */
        public Component text() {
            return Component.translatableWithFallback(this.key, this.fallback, this.args.toArray());
        }

        /** @return the message in English, for the server log */
        public String english() {
            return String.format(this.fallback, this.args.toArray());
        }
    }

    /**
     * The symbols the server gave when it last started.
     *
     * @param groupSymbols each group's symbol, by the group's name
     * @param worldSymbols each world's symbol within the group it was given in
     * @param worldGroups the group each world's symbol was given in, by the group's name
     */
    public record Recorded(Map<String, Symbol> groupSymbols, Map<ResourceKey<Level>, Symbol> worldSymbols,
            Map<ResourceKey<Level>, String> worldGroups) {

        public static final Recorded NONE = new Recorded(Map.of(), Map.of(), Map.of());
    }

    public AddressPlan {
        groups = List.copyOf(groups);
        worldGroups = Collections.unmodifiableMap(new LinkedHashMap<>(worldGroups));
        worldSymbols = Collections.unmodifiableMap(new LinkedHashMap<>(worldSymbols));
        problems = List.copyOf(problems);
    }

    /**
     * Works out the groups and symbols.
     *
     * @param settings the server's address settings
     * @param recorded the symbols given when the server last started
     * @param worlds the worlds the server has, in the order it first saw them
     */
    public static AddressPlan of(final AddressSettings settings, final Recorded recorded,
            final List<ResourceKey<Level>> worlds) {
        final List<Problem> problems = new ArrayList<>();
        final Map<String, Group> groups = AddressPlan.groups(settings, recorded, problems);
        // Every world the server has, then those it gave a symbol before but doesn't have now
        final Set<ResourceKey<Level>> all = new LinkedHashSet<>(worlds);
        all.addAll(recorded.worldSymbols().keySet());
        final Map<ResourceKey<Level>, String> worldGroups = new LinkedHashMap<>();
        for (final ResourceKey<Level> world : all) {
            final String named = settings.groupOf(world);
            if (groups.containsKey(named)) {
                worldGroups.put(world, named);
                continue;
            }
            worldGroups.put(world, AddressSettings.DEFAULT_GROUP);
            if (worlds.contains(world) && !AddressPlan.leftOut(named, problems)) {
                problems.add(AddressPlan.problem(true, "unknown_group",
                        "World %s is set to group %s, which doesn't exist, so it is in the default group",
                        world.identifier().toString(), named));
            }
        }
        final Map<String, Set<Symbol>> taken = new HashMap<>();
        final Map<ResourceKey<Level>, Symbol> worldSymbols = new LinkedHashMap<>();
        // Worlds still in the group their symbol was given in keep it
        for (final ResourceKey<Level> world : all) {
            final Symbol had = recorded.worldSymbols().get(world);
            final String group = worldGroups.get(world);
            if (had != null && group.equals(recorded.worldGroups().getOrDefault(world, AddressSettings.DEFAULT_GROUP))
                    && taken.computeIfAbsent(group, key -> new HashSet<>()).add(had)) {
                worldSymbols.put(world, had);
            }
        }
        // Worlds new to their group take their old symbol if it is free there, or the first free one
        for (final ResourceKey<Level> world : worlds) {
            if (worldSymbols.containsKey(world)) {
                continue;
            }
            final String group = worldGroups.get(world);
            final Set<Symbol> used = taken.computeIfAbsent(group, key -> new HashSet<>());
            final Optional<Symbol> symbol = Optional.ofNullable(recorded.worldSymbols().get(world))
                    .filter(had -> !used.contains(had))
                    .or(() -> AddressPlan.firstFree(used));
            if (symbol.isPresent()) {
                used.add(symbol.get());
                worldSymbols.put(world, symbol.get());
            } else {
                problems.add(AddressPlan.problem(false, "group_full",
                        "World %s has no symbol because group %s already has %s worlds. Addresses of 8 or 9 "
                                + "symbols can't reach it", world.identifier().toString(), group,
                        String.valueOf(Symbol.COUNT)));
            }
        }
        return new AddressPlan(List.copyOf(groups.values()), worldGroups, worldSymbols, problems);
    }

    // The groups in order: the default one, then those with a symbol of their own, then those without
    private static Map<String, Group> groups(final AddressSettings settings, final Recorded recorded,
            final List<Problem> problems) {
        final Map<String, Group> groups = new LinkedHashMap<>();
        final Map<Symbol, String> owners = new HashMap<>();
        final Optional<String> own = Optional.ofNullable(settings.groups().get(AddressSettings.DEFAULT_GROUP))
                .flatMap(GroupSettings::symbol);
        if (own.isPresent() && own.get().isEmpty()) {
            problems.add(AddressPlan.problem(false, "default_cleared",
                    "The default group's symbol is empty in the settings, so it uses @"));
        }
        final Symbol defaultSymbol = own.flatMap(AddressPlan::symbol).orElse(Symbol.GIZA);
        groups.put(AddressSettings.DEFAULT_GROUP, new Group(AddressSettings.DEFAULT_GROUP, defaultSymbol, false));
        owners.put(defaultSymbol, AddressSettings.DEFAULT_GROUP);
        final List<String> automatic = new ArrayList<>();
        for (final Map.Entry<String, GroupSettings> entry : settings.groups().entrySet()) {
            final String name = entry.getKey();
            if (name.equals(AddressSettings.DEFAULT_GROUP)) {
                continue;
            }
            final Optional<Symbol> symbol = entry.getValue().symbol().flatMap(AddressPlan::symbol);
            if (symbol.isEmpty()) {
                automatic.add(name);
            } else if (groups.size() >= Symbol.COUNT) {
                AddressPlan.tooMany(name, problems);
            } else if (owners.containsKey(symbol.get())) {
                problems.add(AddressPlan.problem(true, "symbol_taken",
                        "Group %s can't have the symbol %s because group %s has it. Its worlds are in the default "
                                + "group until you give it another symbol", name, symbol.get().toString(),
                        owners.get(symbol.get())));
            } else {
                groups.put(name, new Group(name, symbol.get(), false));
                owners.put(symbol.get(), name);
            }
        }
        for (final String name : automatic) {
            final Optional<Symbol> symbol = Optional.ofNullable(recorded.groupSymbols().get(name))
                    .filter(had -> !owners.containsKey(had))
                    .or(() -> AddressPlan.firstFree(owners.keySet()));
            if (groups.size() >= Symbol.COUNT || symbol.isEmpty()) {
                AddressPlan.tooMany(name, problems);
                continue;
            }
            groups.put(name, new Group(name, symbol.get(), true));
            owners.put(symbol.get(), name);
        }
        return groups;
    }

    private static void tooMany(final String name, final List<Problem> problems) {
        problems.add(AddressPlan.problem(true, "too_many_groups",
                "Group %s is left out because a server can have only %s groups. Its worlds are in the default "
                        + "group", name, String.valueOf(Symbol.COUNT)));
    }

    // Whether a group was named in the settings but left out, which a problem already explains
    private static boolean leftOut(final String name, final List<Problem> problems) {
        return problems.stream().anyMatch(problem -> problem.error() && !problem.args().isEmpty()
                && problem.args().getFirst().equals(name));
    }

    private static Optional<Symbol> symbol(final String text) {
        return text.length() == 1 ? Symbol.of(text.charAt(0)) : Optional.empty();
    }

    private static Optional<Symbol> firstFree(final Set<Symbol> used) {
        for (int index = 0; index < Symbol.COUNT; index++) {
            final Symbol symbol = new Symbol(index);
            if (!used.contains(symbol)) {
                return Optional.of(symbol);
            }
        }
        return Optional.empty();
    }

    private static Problem problem(final boolean error, final String key, final String fallback,
            final String... args) {
        return new Problem(error, "message.sgcraft.addresses." + key, fallback, List.of(args));
    }

    /** @return the group with a name, if the plan has it */
    public Optional<Group> group(final String name) {
        return this.groups.stream().filter(group -> group.name().equals(name)).findFirst();
    }

    /** @return the default group */
    public Group defaultGroup() {
        return this.groups.getFirst();
    }

    /** @return the name of the group a world is in */
    public String groupOf(final ResourceKey<Level> world) {
        return this.worldGroups.getOrDefault(world, AddressSettings.DEFAULT_GROUP);
    }

    /** @return the symbol that names a world within its group, if it has one */
    public Optional<Symbol> symbolOf(final ResourceKey<Level> world) {
        return Optional.ofNullable(this.worldSymbols.get(world));
    }

    /** @return the worlds in a group, those the server doesn't have now included */
    public List<ResourceKey<Level>> worldsIn(final String group) {
        return this.worldGroups.entrySet().stream().filter(entry -> entry.getValue().equals(group))
                .map(Map.Entry::getKey).toList();
    }

    /** @return the symbols as they are recorded for the next start */
    public Recorded recorded() {
        final Map<String, Symbol> groupSymbols = new LinkedHashMap<>();
        this.groups.forEach(group -> groupSymbols.put(group.name(), group.symbol()));
        final Map<ResourceKey<Level>, String> groups = new LinkedHashMap<>();
        this.worldSymbols.keySet().forEach(world -> groups.put(world, this.groupOf(world)));
        return new Recorded(groupSymbols, this.worldSymbols, groups);
    }
}
