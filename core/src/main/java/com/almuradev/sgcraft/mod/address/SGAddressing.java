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

import com.almuradev.sgcraft.api.SGCraftRegistries;
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.address.AddressPartition;
import com.almuradev.sgcraft.api.address.AddressReach;
import com.almuradev.sgcraft.api.address.AddressTarget;
import com.almuradev.sgcraft.api.address.Addressing;
import com.almuradev.sgcraft.api.address.Symbol;
import com.almuradev.sgcraft.api.address.WorldGroup;
import com.almuradev.sgcraft.mod.SGConfig;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.address.AddressPlan.Problem;
import com.almuradev.sgcraft.mod.address.AddressPlan.Recorded;
import com.almuradev.sgcraft.mod.gate.GateStore;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Addresses on a running server.
 *
 * <p>Each world is divided into cells as the server's {@code addresses} settings lay it out, for every world or for
 * that one. The settings sort worlds into groups, as {@link AddressPlan} describes; a world they put in none is in the
 * default group, whose symbol is {@code @} unless they give it another.</p>
 */
public final class SGAddressing implements Addressing {

    private static final String RULE = "*".repeat(100);

    private final MinecraftServer server;
    private final AddressSettings settings;
    private final Map<ResourceKey<Level>, AddressPartition> partitions = new ConcurrentHashMap<>();
    private final List<AddressReach> reaches;
    private final Map<String, PlannedGroup> groups = new LinkedHashMap<>();
    // What changed or couldn't be followed at this start, for operators who join
    private final List<Component> notices = new ArrayList<>();
    private final AddressPlan plan;

    public SGAddressing(final MinecraftServer server, final SGConfig config) {
        this.server = server;
        this.settings = config.addresses();
        this.plan = this.recordAddresses();
        for (final AddressPlan.Group group : this.plan.groups()) {
            this.groups.put(group.name(), new PlannedGroup(group));
        }
        this.reaches = server.registryAccess().lookupOrThrow(SGCraftRegistries.ADDRESS_REACH).stream()
                .sorted(Comparator.comparingInt(AddressReach::symbolCount))
                .toList();
    }

    /** @return the symbols the server gave when it last started, as the store recorded them */
    public static Recorded recorded(final GateStore store) {
        final Map<String, Symbol> groupSymbols = new LinkedHashMap<>(store.groupSymbols());
        // Saved before there were groups, when every world was in the default group, named by @
        groupSymbols.putIfAbsent(AddressSettings.DEFAULT_GROUP, Symbol.GIZA);
        return new Recorded(groupSymbols, store.worldSymbols(), store.worldGroups());
    }

    // Works out the groups and symbols, records them and the layout each world's addresses are made with, and warns
    // loudly about each world with gates whose addresses aren't the ones they were made with
    private AddressPlan recordAddresses() {
        final GateStore store = this.store();
        final List<ResourceKey<Level>> worlds = new ArrayList<>();
        this.server.getAllLevels().forEach(level -> worlds.add(level.dimension()));
        final Recorded recorded = SGAddressing.recorded(store);
        final AddressPlan plan = AddressPlan.of(this.settings, recorded, worlds);
        for (final Problem problem : plan.problems()) {
            if (problem.error()) {
                SGCraft.LOG.error(problem.english());
            } else {
                SGCraft.LOG.warn(problem.english());
            }
            this.notices.add(problem.text());
        }
        for (final AddressPlan.Group group : plan.groups()) {
            if (!group.symbol().equals(recorded.groupSymbols().get(group.name()))) {
                SGCraft.LOG.info("Group {} has the symbol {}", group.name(), group.symbol());
            }
        }
        final List<String> changed = new ArrayList<>();
        for (final ResourceKey<Level> world : worlds) {
            final Optional<WorldAddress> before = WorldAddress.recorded(store.layout(world), recorded, world);
            final WorldAddress after = WorldAddress.of(this.settings.layoutOf(world), plan, world);
            final Optional<String> change = SGAddressing.describeChange(world.identifier(), before, after,
                    store.gateCount(world));
            if (change.isPresent()) {
                changed.add(change.get());
            } else if (before.isPresent() && !before.get().equals(after)) {
                SGCraft.LOG.info("World {} has no gates, so its new addresses change nothing: {}",
                        world.identifier(), after.describe());
            }
            if (!plan.symbolOf(world).equals(Optional.ofNullable(recorded.worldSymbols().get(world)))) {
                plan.symbolOf(world).ifPresent(symbol -> SGCraft.LOG.info("World {} has the symbol {} in group {}",
                        world.identifier(), symbol, plan.groupOf(world)));
            }
            store.putLayout(world, after.layout());
            store.putWorldSymbol(world, plan.groupOf(world), plan.symbolOf(world));
        }
        store.putGroupSymbols(plan.recorded().groupSymbols());
        store.dropLegacyOrigin();
        if (!changed.isEmpty()) {
            SGAddressing.warnChanged(changed);
            this.notices.add(Component.translatableWithFallback("message.sgcraft.addresses.changed_at_start",
                    "Stargate addresses changed when the server started, in %s worlds with gates. The server log "
                            + "names them", changed.size()));
        }
        return plan;
    }

    /**
     * @return a line for the warning about a world whose addresses changed, naming it, its gates, and what changed;
     *     empty when nothing was recorded for it, nothing changed, or it has no gates
     */
    public static Optional<String> describeChange(final Identifier world, final Optional<WorldAddress> before,
            final WorldAddress after, final long gates) {
        if (before.isEmpty() || before.get().equals(after) || gates <= 0) {
            return Optional.empty();
        }
        return Optional.of(world + " (" + gates + (gates == 1 ? " gate" : " gates") + "): "
                + before.get().changesTo(after));
    }

    private static void warnChanged(final List<String> worlds) {
        SGCraft.LOG.warn(SGAddressing.RULE);
        SGCraft.LOG.warn(SGAddressing.RULE);
        SGCraft.LOG.warn("**");
        SGCraft.LOG.warn("**  SGCRAFT: STARGATE ADDRESSES HAVE CHANGED");
        SGCraft.LOG.warn("**");
        SGCraft.LOG.warn("**  The address settings changed for worlds that already have gates:");
        SGCraft.LOG.warn("**");
        for (final String world : worlds) {
            SGCraft.LOG.warn("**    {}", world);
        }
        SGCraft.LOG.warn("**");
        SGCraft.LOG.warn("**  A new cell size or point of origin changes every address in the world. A new group,");
        SGCraft.LOG.warn("**  group symbol or world symbol changes its addresses of 8 and 9 symbols.");
        SGCraft.LOG.warn("**");
        SGCraft.LOG.warn("**  Address books, default addresses, PDDs, tablets, signs and anything else that holds an");
        SGCraft.LOG.warn("**  old address now reach another gate or none. A bigger cell can also put two gates in one");
        SGCraft.LOG.warn("**  cell, and then only one of them can be dialed.");
        SGCraft.LOG.warn("**");
        SGCraft.LOG.warn("**  To get the old addresses back, stop the server, put the addresses section of");
        SGCraft.LOG.warn("**  config/sgcraft-server.json back as it was, and start the server again. This warning");
        SGCraft.LOG.warn("**  shows once, because the server has now recorded the new addresses.");
        SGCraft.LOG.warn("**");
        SGCraft.LOG.warn(SGAddressing.RULE);
        SGCraft.LOG.warn(SGAddressing.RULE);
    }

    /** @return what changed or couldn't be followed when the server started, to tell operators as they join */
    public List<Component> notices() {
        return List.copyOf(this.notices);
    }

    /** @return the groups and symbols the server is running with */
    public AddressPlan plan() {
        return this.plan;
    }

    /** @return the server's address settings as it is running with them */
    public AddressSettings settings() {
        return this.settings;
    }

    /** @return what a world's addresses are made with while the server runs */
    public WorldAddress worldAddress(final ResourceKey<Level> world) {
        return WorldAddress.of(this.layoutOf(world), this.plan, world);
    }

    private GateStore store() {
        return this.server.getDataStorage().computeIfAbsent(GateStore.TYPE);
    }

    @Override
    public List<AddressReach> reaches() {
        return this.reaches;
    }

    @Override
    public Optional<AddressReach> reachOf(final int symbolCount) {
        return this.reaches.stream().filter(reach -> reach.symbolCount() == symbolCount).findFirst();
    }

    @Override
    public Optional<AddressReach> reachOf(final Address address) {
        return this.reachOf(address.size());
    }

    @Override
    public Optional<Address> addressOf(final GlobalPos target, final ResourceKey<Level> from) {
        for (final AddressReach reach : this.reaches) {
            final Optional<Address> address = reach.addressOf(target, from);
            if (address.isPresent()) {
                return address;
            }
        }
        return Optional.empty();
    }

    /**
     * @return the address of a cell that reaches it from every world: the shortest that names the cell's world, or
     *     the address from the cell's own world where no reach names worlds. With more than one group, an address
     *     that names the world but not its group means another world in another group, so it must name both
     */
    public Optional<Address> lastingAddressOf(final GlobalPos target) {
        final boolean grouped = this.groups.size() > 1;
        for (final AddressReach reach : this.reaches) {
            if (reach instanceof GridReach grid && !(grouped && grid.kind() == GridReach.Kind.SAME_GROUP)) {
                final Optional<Address> address = grid.namingAddressOf(target);
                if (address.isPresent()) {
                    return address;
                }
            }
        }
        return this.addressOf(target, target.dimension());
    }

    /**
     * @return an address written in a world, rewritten to reach the same cell from every world, as an address book
     *     keeps it; the address as it is if it points nowhere from there
     */
    public Address lasting(final Address address, final ResourceKey<Level> from) {
        return this.resolve(address, from)
                .flatMap(target -> this.lastingAddressOf(GlobalPos.of(target.world(),
                        target.chunk().getWorldPosition())))
                .orElse(address);
    }

    /**
     * @return an address rewritten as the shortest that reaches the same cell from a world, so a gate dials it with
     *     as few chevrons as it needs; the address as it is if it points nowhere from there
     */
    public Address shortest(final Address address, final ResourceKey<Level> from) {
        return this.resolve(address, from)
                .flatMap(target -> this.addressOf(GlobalPos.of(target.world(), target.chunk().getWorldPosition()),
                        from))
                .orElse(address);
    }

    @Override
    public Optional<AddressTarget> resolve(final Address address, final ResourceKey<Level> from) {
        return this.reachOf(address).flatMap(reach -> reach.resolve(address, from));
    }

    @Override
    public AddressPartition partition(final ResourceKey<Level> world) {
        return this.partitions.computeIfAbsent(world, key -> {
            final AddressLayout layout = this.layoutOf(key);
            return new SimplePartition(layout.cellChunks(), layout.originSymbol());
        });
    }

    /** @return the address layout a world has while the server runs */
    public AddressLayout layoutOf(final ResourceKey<Level> world) {
        return this.settings.layoutOf(world);
    }

    /** @return how many formed gates a world has, each of whose addresses its layout decides */
    public long gateCount(final ResourceKey<Level> world) {
        return this.store().gateCount(world);
    }

    @Override
    public Collection<WorldGroup> groups() {
        return List.copyOf(this.groups.values());
    }

    @Override
    public WorldGroup defaultGroup() {
        return this.groups.get(AddressSettings.DEFAULT_GROUP);
    }

    @Override
    public WorldGroup groupOf(final ResourceKey<Level> world) {
        return this.groups.getOrDefault(this.plan.groupOf(world), this.groups.get(AddressSettings.DEFAULT_GROUP));
    }

    private record SimplePartition(int cellChunks, Optional<Symbol> pointOfOrigin) implements AddressPartition {
    }

    private final class PlannedGroup implements WorldGroup {

        private final AddressPlan.Group group;

        PlannedGroup(final AddressPlan.Group group) {
            this.group = group;
        }

        @Override
        public String name() {
            return this.group.name();
        }

        @Override
        public boolean isDefault() {
            return this.group.isDefault();
        }

        @Override
        public Symbol symbol() {
            return this.group.symbol();
        }

        @Override
        public Set<ResourceKey<Level>> worlds() {
            return Set.copyOf(SGAddressing.this.plan.worldsIn(this.group.name()));
        }

        @Override
        public Optional<Symbol> symbolOf(final ResourceKey<Level> world) {
            return SGAddressing.this.plan.groupOf(world).equals(this.group.name())
                    ? SGAddressing.this.plan.symbolOf(world) : Optional.empty();
        }
    }
}
