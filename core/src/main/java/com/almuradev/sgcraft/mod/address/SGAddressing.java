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
import com.almuradev.sgcraft.mod.gate.GateStore;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Addresses on a running server.
 *
 * <p>Every world is in the default group, whose symbol is {@code @}. Each world is divided into cells as the server's
 * {@code addresses} settings lay it out, for every world or for that one.</p>
 */
public final class SGAddressing implements Addressing {

    private static final String RULE = "*".repeat(100);

    private final MinecraftServer server;
    private final AddressSettings settings;
    private final Map<ResourceKey<Level>, AddressPartition> partitions = new ConcurrentHashMap<>();
    private final List<AddressReach> reaches;
    private final DefaultGroup defaultGroup = new DefaultGroup();

    public SGAddressing(final MinecraftServer server, final SGConfig config) {
        this.server = server;
        this.settings = config.addresses();
        this.recordLayouts();
        this.reaches = server.registryAccess().lookupOrThrow(SGCraftRegistries.ADDRESS_REACH).stream()
                .sorted(Comparator.comparingInt(AddressReach::symbolCount))
                .toList();
        this.assignWorldSymbols();
    }

    // Records the layout each world's addresses are made with, warning loudly about each world with gates whose layout
    // isn't the one its gates' addresses were made with, since every address there has changed
    private void recordLayouts() {
        final GateStore store = this.store();
        final List<String> changed = new ArrayList<>();
        for (final ServerLevel level : this.server.getAllLevels()) {
            final ResourceKey<Level> world = level.dimension();
            final AddressLayout layout = this.layoutOf(world);
            final Optional<AddressLayout> recorded = store.layout(world);
            final Optional<String> change = SGAddressing.describeChange(world.identifier(), recorded, layout,
                    store.gateCount(world));
            if (change.isPresent()) {
                changed.add(change.get());
            } else if (recorded.isPresent() && !recorded.get().equals(layout)) {
                SGCraft.LOG.info("World {} has no gates, so its new address layout changes nothing: {}",
                        world.identifier(), layout.describe());
            }
            store.putLayout(world, layout);
        }
        store.dropLegacyOrigin();
        if (!changed.isEmpty()) {
            SGAddressing.warnChanged(changed);
        }
    }

    /**
     * @return a line for the warning about a world whose addresses changed, naming it, its gates, and its layout
     *     before and now; empty when nothing was recorded for it, its layout is the recorded one, or it has no gates
     */
    public static Optional<String> describeChange(final Identifier world, final Optional<AddressLayout> recorded,
            final AddressLayout layout, final long gates) {
        if (recorded.isEmpty() || recorded.get().equals(layout) || gates <= 0) {
            return Optional.empty();
        }
        return Optional.of(world + " (" + gates + (gates == 1 ? " gate" : " gates") + "): "
                + recorded.get().describe() + " is now " + layout.describe());
    }

    private static void warnChanged(final List<String> worlds) {
        SGCraft.LOG.warn(SGAddressing.RULE);
        SGCraft.LOG.warn(SGAddressing.RULE);
        SGCraft.LOG.warn("**");
        SGCraft.LOG.warn("**  SGCRAFT: STARGATE ADDRESSES HAVE CHANGED");
        SGCraft.LOG.warn("**");
        SGCraft.LOG.warn("**  The address settings (cell_chunks or point_of_origin) changed for worlds that already");
        SGCraft.LOG.warn("**  have gates. EVERY gate address in these worlds is now different:");
        SGCraft.LOG.warn("**");
        for (final String world : worlds) {
            SGCraft.LOG.warn("**    {}", world);
        }
        SGCraft.LOG.warn("**");
        SGCraft.LOG.warn("**  Address books, default addresses, PDDs, tablets, signs and anything else holding an old");
        SGCraft.LOG.warn("**  address now reach another gate or none. Changing the cell size can also put two gates");
        SGCraft.LOG.warn("**  in one cell, where only one of them can be dialed.");
        SGCraft.LOG.warn("**");
        SGCraft.LOG.warn("**  To get the old addresses back, stop the server, set the addresses section of");
        SGCraft.LOG.warn("**  config/sgcraft-server.json back to the old values and start it again. The new layout");
        SGCraft.LOG.warn("**  has been recorded, so this warning shows once.");
        SGCraft.LOG.warn("**");
        SGCraft.LOG.warn(SGAddressing.RULE);
        SGCraft.LOG.warn(SGAddressing.RULE);
    }

    // Gives each world without a symbol the first one not yet used; a world's symbol never changes once given
    private void assignWorldSymbols() {
        final GateStore store = this.store();
        for (final ServerLevel level : this.server.getAllLevels()) {
            if (store.worldSymbols().containsKey(level.dimension())) {
                continue;
            }
            final Set<Symbol> used = new HashSet<>(store.worldSymbols().values());
            for (int index = 0; index < Symbol.COUNT; index++) {
                final Symbol symbol = new Symbol(index);
                if (!used.contains(symbol)) {
                    store.putWorldSymbol(level.dimension(), symbol);
                    SGCraft.LOG.info("World {} has the symbol {}", level.dimension().identifier(), symbol);
                    break;
                }
            }
        }
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
     *     the address from the cell's own world where no reach names worlds
     */
    public Optional<Address> lastingAddressOf(final GlobalPos target) {
        for (final AddressReach reach : this.reaches) {
            if (reach instanceof GridReach grid) {
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
        return List.of(this.defaultGroup);
    }

    @Override
    public WorldGroup defaultGroup() {
        return this.defaultGroup;
    }

    @Override
    public WorldGroup groupOf(final ResourceKey<Level> world) {
        return this.defaultGroup;
    }

    private record SimplePartition(int cellChunks, Optional<Symbol> pointOfOrigin) implements AddressPartition {
    }

    private final class DefaultGroup implements WorldGroup {

        @Override
        public String name() {
            return "default";
        }

        @Override
        public boolean isDefault() {
            return true;
        }

        @Override
        public Symbol symbol() {
            return Symbol.GIZA;
        }

        @Override
        public Set<ResourceKey<Level>> worlds() {
            return Set.copyOf(SGAddressing.this.store().worldSymbols().keySet());
        }

        @Override
        public Optional<Symbol> symbolOf(final ResourceKey<Level> world) {
            final Map<ResourceKey<Level>, Symbol> symbols = SGAddressing.this.store().worldSymbols();
            return Optional.ofNullable(symbols.get(world));
        }
    }
}
