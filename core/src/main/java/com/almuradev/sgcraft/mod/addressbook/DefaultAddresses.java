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

package com.almuradev.sgcraft.mod.addressbook;

import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.addressbook.AddressBookEntry;
import com.almuradev.sgcraft.mod.Platform;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.item.SGDataComponents;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * The addresses every new PDD and gate control tablet starts with, as in 1.12: the server's own list, kept in
 * {@code sgcraft-default-addresses.json} and set with {@code /sgcraft default_addresses}. A PDD or tablet takes them
 * the first time it is used; after that its addresses are its holder's, and emptying it leaves it empty. A locked
 * address stays: its holder can move it, but not rename or remove it.
 */
public final class DefaultAddresses {

    private static final String FILE = "sgcraft-default-addresses.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * One default address.
     *
     * @param entry the address and the name it is listed under
     * @param locked whether holders can't rename or remove it
     */
    public record Entry(AddressBookEntry entry, boolean locked) {

        // Named in plain text, so the file is easy to write by hand
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Address.CODEC.fieldOf("address").forGetter(entry -> entry.entry().address()),
                Codec.STRING.fieldOf("name").forGetter(entry -> entry.entry().name().getString()),
                Codec.BOOL.optionalFieldOf("locked", false).forGetter(Entry::locked)
        ).apply(instance, (address, name, locked) -> new Entry(new AddressBookEntry(address,
                Component.literal(name)), locked)));
    }

    private static final Codec<List<Entry>> CODEC = Entry.CODEC.listOf();

    private DefaultAddresses() {
    }

    /** Gives {@code stack} the default addresses, and locks the locked ones, if it has never held any. */
    public static void fill(final ItemStack stack, final List<Entry> defaults) {
        if (stack.has(SGDataComponents.ADDRESSES)) {
            return;
        }
        stack.set(SGDataComponents.ADDRESSES, defaults.stream().map(Entry::entry).toList());
        final List<Address> locked = defaults.stream().filter(Entry::locked).map(entry -> entry.entry().address())
                .toList();
        if (!locked.isEmpty()) {
            stack.set(SGDataComponents.LOCKED_ADDRESSES, locked);
        }
    }

    /** Reads the list, writing an empty one first if there is no file. */
    public static List<Entry> load() {
        final Path path = DefaultAddresses.path();
        try {
            if (Files.notExists(path)) {
                DefaultAddresses.save(List.of());
                return List.of();
            }
            final JsonElement json = JsonParser.parseString(Files.readString(path));
            return DefaultAddresses.CODEC.parse(JsonOps.INSTANCE, json).resultOrPartial(error ->
                    SGCraft.LOG.error("{} has a mistake, so new PDDs and tablets start empty: {}", path, error))
                    .map(List::copyOf).orElse(List.of());
        } catch (final IOException | RuntimeException e) {
            SGCraft.LOG.error("Couldn't read {}, so new PDDs and tablets start empty", path, e);
            return List.of();
        }
    }

    /**
     * Writes the list to its file.
     *
     * @return whether the file was written
     */
    public static boolean save(final List<Entry> addresses) {
        final Path path = DefaultAddresses.path();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, DefaultAddresses.GSON.toJson(
                    DefaultAddresses.CODEC.encodeStart(JsonOps.INSTANCE, addresses).getOrThrow()));
            return true;
        } catch (final IOException | RuntimeException e) {
            SGCraft.LOG.error("Couldn't write the default addresses to {}", path, e);
            return false;
        }
    }

    private static Path path() {
        return Platform.get().configDir().resolve(DefaultAddresses.FILE);
    }
}
