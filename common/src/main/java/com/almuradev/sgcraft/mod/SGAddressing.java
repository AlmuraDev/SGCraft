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

package com.almuradev.sgcraft.mod;

import com.almuradev.sgcraft.SGCraft;
import com.almuradev.sgcraft.api.SGCraftRegistries;
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.address.AddressPartition;
import com.almuradev.sgcraft.api.address.AddressReach;
import com.almuradev.sgcraft.api.address.AddressTarget;
import com.almuradev.sgcraft.api.address.Addressing;
import com.almuradev.sgcraft.api.address.Symbol;
import com.almuradev.sgcraft.api.address.WorldGroup;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Addresses on a running server.
 *
 * <p>Until the server's configuration exists, every world is in the default group, whose symbol is {@code @}, and
 * every world is divided into cells one chunk across, with no point of origin.</p>
 */
public final class SGAddressing implements Addressing {

    private static final AddressPartition PARTITION = new SimplePartition(1, Optional.empty());

    private final MinecraftServer server;
    private final List<AddressReach> reaches;
    private final DefaultGroup defaultGroup = new DefaultGroup();

    public SGAddressing(final MinecraftServer server) {
        this.server = server;
        this.reaches = server.registryAccess().lookupOrThrow(SGCraftRegistries.ADDRESS_REACH).stream()
                .sorted(Comparator.comparingInt(AddressReach::symbolCount))
                .toList();
        this.assignWorldSymbols();
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

    @Override
    public Optional<AddressTarget> resolve(final Address address, final ResourceKey<Level> from) {
        return this.reachOf(address).flatMap(reach -> reach.resolve(address, from));
    }

    @Override
    public AddressPartition partition(final ResourceKey<Level> world) {
        return SGAddressing.PARTITION;
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
