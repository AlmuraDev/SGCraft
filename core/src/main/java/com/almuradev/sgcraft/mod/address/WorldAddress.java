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
import com.almuradev.sgcraft.mod.address.AddressPlan.Recorded;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Everything one world's addresses are made with. A new layout changes every address in the world; a new group,
 * group symbol or world symbol changes only the addresses of 8 and 9 symbols, which name the world.
 *
 * @param layout the world's cell size and point of origin
 * @param group the name of the group the world is in
 * @param groupSymbol the symbol that names the group, if it is known
 * @param symbol the symbol that names the world within its group, if it has one
 */
public record WorldAddress(AddressLayout layout, String group, Optional<Symbol> groupSymbol,
        Optional<Symbol> symbol) {

    /** @return what a world's addresses are made with under a plan */
    public static WorldAddress of(final AddressLayout layout, final AddressPlan plan, final ResourceKey<Level> world) {
        final String group = plan.groupOf(world);
        return new WorldAddress(layout, group, plan.group(group).map(AddressPlan.Group::symbol),
                plan.symbolOf(world));
    }

    /** @return what a world's addresses were made with when the server last started, if anything was recorded */
    public static Optional<WorldAddress> recorded(final Optional<AddressLayout> layout, final Recorded recorded,
            final ResourceKey<Level> world) {
        return layout.map(had -> {
            final String group = recorded.worldGroups().getOrDefault(world, AddressSettings.DEFAULT_GROUP);
            return new WorldAddress(had, group, Optional.ofNullable(recorded.groupSymbols().get(group)),
                    Optional.ofNullable(recorded.worldSymbols().get(world)));
        });
    }

    /** @return whether every address in the world differs from {@code other}'s, not only those that name it */
    public boolean changesEveryAddress(final WorldAddress other) {
        return !this.layout.equals(other.layout);
    }

    /** @return what the world's addresses are made with, as people read it */
    public String describe() {
        return this.layout.describe() + ", group " + this.groupText() + ", "
                + this.symbol.map(world -> "world symbol " + world).orElse("no world symbol");
    }

    /**
     * @return how the world's addresses change from these to {@code after}, as people read it, such as
     *     {@code group default (@) is now group pegasus (P), so its addresses of 8 and 9 symbols change}
     */
    public String changesTo(final WorldAddress after) {
        final List<String> parts = new ArrayList<>();
        if (!this.layout.equals(after.layout)) {
            parts.add(this.layout.describe() + " is now " + after.layout.describe());
        }
        if (!this.group.equals(after.group) || !this.groupSymbol.equals(after.groupSymbol)) {
            parts.add("group " + this.groupText() + " is now group " + after.groupText());
        }
        if (!this.symbol.equals(after.symbol)) {
            parts.add(WorldAddress.symbolText(this.symbol) + " is now " + WorldAddress.symbolText(after.symbol));
        }
        return String.join("; ", parts) + (this.changesEveryAddress(after)
                ? ", so every address changes" : ", so its addresses of 8 and 9 symbols change");
    }

    private String groupText() {
        return this.group + this.groupSymbol.map(symbol -> " (" + symbol + ")").orElse("");
    }

    private static String symbolText(final Optional<Symbol> symbol) {
        return symbol.map(world -> "world symbol " + world).orElse("no world symbol");
    }
}
