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

import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.addressbook.AddressBook;
import com.almuradev.sgcraft.api.addressbook.AddressBookEntry;
import com.almuradev.sgcraft.api.addressbook.AddressBookSource;
import com.almuradev.sgcraft.item.SGDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The book kept on an item, such as a PDD or a gate control tablet: its entries go wherever the item goes, so a PDD
 * handed to another player carries its addresses with it.
 */
public final class ItemAddressBook extends ListAddressBook {

    private final ItemStack stack;

    private ItemAddressBook(final ItemStack stack) {
        this.stack = stack;
    }

    /** @return the book on this stack, which changes the stack itself */
    public static AddressBook of(final ItemStack stack) {
        return new ItemAddressBook(stack);
    }

    @Override
    public AddressBookSource source() {
        return new AddressBookSource(AddressBookSource.ITEM, Component.translatableWithFallback(
                "address_book.sgcraft.item_named", "The addresses on this %s", this.stack.getHoverName()));
    }

    @Override
    protected List<AddressBookEntry> entries() {
        return this.stack.getOrDefault(SGDataComponents.ADDRESSES, List.of());
    }

    /** @return whether the entry for {@code address} came locked with the server's default addresses */
    @Override
    public boolean isLocked(final Address address) {
        return this.stack.getOrDefault(SGDataComponents.LOCKED_ADDRESSES, List.of()).contains(address);
    }

    @Override
    protected void save(final List<AddressBookEntry> entries) {
        // Kept even when empty, so an emptied book doesn't take the default addresses again
        this.stack.set(SGDataComponents.ADDRESSES, List.copyOf(entries));
    }
}
