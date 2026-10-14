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

import com.almuradev.sgcraft.api.SGCraftApi;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Objects;

/**
 * Where an address book comes from, so that screens can group and label books.
 *
 * @param kind what holds the book, such as {@link #PLAYER}, or a kind an add-on defines in its own namespace
 * @param label what players see the book called
 * @since 0.1.0
 */
public record AddressBookSource(Identifier kind, Component label) {

    /** A player's own book, which belongs to the player rather than to anything they carry. */
    public static final Identifier PLAYER = Identifier.fromNamespaceAndPath(SGCraftApi.NAMESPACE, "player");

    /** A book held in an item. */
    public static final Identifier ITEM = Identifier.fromNamespaceAndPath(SGCraftApi.NAMESPACE, "item");

    /** A book held in a block. */
    public static final Identifier BLOCK = Identifier.fromNamespaceAndPath(SGCraftApi.NAMESPACE, "block");

    public AddressBookSource {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(label, "label");
    }
}
