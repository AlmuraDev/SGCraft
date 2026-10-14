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

package com.almuradev.sgcraft.api.addressbook;

import com.almuradev.sgcraft.api.ActionResult;
import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.address.Address;
import net.minecraft.network.chat.Component;

import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A list of addresses, each with a name, in the order players see them.
 *
 * <p>An address appears at most once in a book. Iterating gives the entries in display order, from a snapshot taken
 * when iteration starts, so changing the book while iterating is safe. The iterator doesn't support
 * {@link Iterator#remove()}.</p>
 *
 * <p>Some books can't be changed, such as one an add-on builds from data it owns. The changing methods on those
 * return {@link ActionResult#NOT_POSSIBLE}.</p>
 *
 * @since 0.1.0
 */
public interface AddressBook extends Iterable<AddressBookEntry> {

    /** @return where this book comes from */
    AddressBookSource source();

    /** @return whether callers can change this book */
    boolean isEditable();

    /** @return how many entries the book has */
    int size();

    /** @return whether the book has no entries */
    default boolean isEmpty() {
        return this.size() == 0;
    }

    /** @return the entries in display order */
    Stream<AddressBookEntry> stream();

    /**
     * @param address an address
     * @return its entry, if the book has one
     */
    Optional<AddressBookEntry> find(final Address address);

    /**
     * Adds an address at the end of the book.
     *
     * @param address the address
     * @param name the name to list it under
     * @param cause who is adding it
     * @return {@link ActionResult#NO_CHANGE} if the book already has the address
     */
    ActionResult add(final Address address, final Component name, final Cause cause);

    /**
     * @param address an address in the book
     * @param name its new name
     * @param cause who is renaming it
     * @return {@link ActionResult#NOT_POSSIBLE} if the book doesn't have the address
     */
    ActionResult rename(final Address address, final Component name, final Cause cause);

    /**
     * @param address an address in the book
     * @param cause who is removing it
     * @return {@link ActionResult#NO_CHANGE} if the book doesn't have the address
     */
    ActionResult remove(final Address address, final Cause cause);

    /**
     * Moves an entry to another place in the display order.
     *
     * @param address an address in the book
     * @param index where it goes, from 0 to {@link #size()} - 1
     * @param cause who is moving it
     * @return {@link ActionResult#NOT_POSSIBLE} if the book doesn't have the address or {@code index} is out of
     *     range
     */
    ActionResult move(final Address address, final int index, final Cause cause);
}
