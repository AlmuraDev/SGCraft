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
import com.almuradev.sgcraft.api.addressbook.AddressBook;
import com.almuradev.sgcraft.api.addressbook.AddressBookEntry;
import com.almuradev.sgcraft.api.addressbook.AddressBookProvider;
import com.almuradev.sgcraft.api.addressbook.AddressBookSource;
import com.almuradev.sgcraft.api.addressbook.AddressBooks;
import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Players' own address books, kept with the server's data so they last whether or not the player is online, and the
 * books the registered providers offer.
 */
public final class SGAddressBooks implements AddressBooks {

    private final MinecraftServer server;

    public SGAddressBooks(final MinecraftServer server) {
        this.server = server;
    }

    private Store store() {
        this.checkThread();
        return this.server.getDataStorage().computeIfAbsent(Store.TYPE);
    }

    private void checkThread() {
        if (!this.server.isSameThread()) {
            throw new IllegalStateException("Address books are only used on the server thread");
        }
    }

    @Override
    public AddressBook forPlayer(final UUID player) {
        return new PlayerBook(player);
    }

    @Override
    public List<AddressBook> available(final ServerPlayer player) {
        final List<AddressBook> books = new ArrayList<>();
        for (final AddressBookProvider provider : this.server.registryAccess()
                .lookupOrThrow(SGCraftRegistries.ADDRESS_BOOK_PROVIDER)) {
            books.addAll(provider.booksFor(player));
        }
        return books;
    }

    /** One player's own book, read from and written to the store each time. */
    private final class PlayerBook extends ListAddressBook {

        private static final AddressBookSource SOURCE = new AddressBookSource(AddressBookSource.PLAYER,
                Component.translatableWithFallback("address_book.sgcraft.player", "Your addresses"));

        private final UUID player;

        PlayerBook(final UUID player) {
            this.player = player;
        }

        @Override
        protected List<AddressBookEntry> entries() {
            return SGAddressBooks.this.store().books.getOrDefault(this.player, List.of());
        }

        @Override
        protected void save(final List<AddressBookEntry> entries) {
            final Store store = SGAddressBooks.this.store();
            if (entries.isEmpty()) {
                store.books.remove(this.player);
            } else {
                store.books.put(this.player, List.copyOf(entries));
            }
            store.setDirty();
        }

        @Override
        public AddressBookSource source() {
            return PlayerBook.SOURCE;
        }
    }

    /** Every player's own book, as saved. */
    static final class Store extends SavedData {

        private static final Codec<Store> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC,
                ListAddressBook.ENTRY_CODEC.listOf()).xmap(Store::new, store -> store.books);

        static final SavedDataType<Store> TYPE = new SavedDataType<>(SGCraft.id("address_books"), Store::new,
                Store.CODEC, null);

        private final Map<UUID, List<AddressBookEntry>> books = new HashMap<>();

        private Store() {
        }

        private Store(final Map<UUID, List<AddressBookEntry>> books) {
            this.books.putAll(books);
        }
    }
}
