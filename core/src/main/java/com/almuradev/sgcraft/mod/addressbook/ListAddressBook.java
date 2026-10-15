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

import com.almuradev.sgcraft.api.ActionResult;
import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.addressbook.AddressBook;
import com.almuradev.sgcraft.api.addressbook.AddressBookEntry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * An editable book kept as a plain list of entries somewhere: read whole each time, and written whole after each
 * change.
 */
public abstract class ListAddressBook implements AddressBook {

    /** One entry, as saved. */
    public static final Codec<AddressBookEntry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Address.CODEC.fieldOf("address").forGetter(AddressBookEntry::address),
            ComponentSerialization.CODEC.fieldOf("name").forGetter(AddressBookEntry::name)
    ).apply(instance, AddressBookEntry::new));

    /** @return the entries as kept now */
    protected abstract List<AddressBookEntry> entries();

    /** Keeps these entries in place of the old ones. */
    protected abstract void save(final List<AddressBookEntry> entries);

    /** @return whether the entry for {@code address} may not be renamed or removed; none is, unless a book says so */
    protected boolean isLocked(final Address address) {
        return false;
    }

    @Override
    public boolean isEditable() {
        return true;
    }

    @Override
    public int size() {
        return this.entries().size();
    }

    @Override
    public Stream<AddressBookEntry> stream() {
        return List.copyOf(this.entries()).stream();
    }

    @Override
    public Iterator<AddressBookEntry> iterator() {
        return List.copyOf(this.entries()).iterator();
    }

    @Override
    public Optional<AddressBookEntry> find(final Address address) {
        return this.entries().stream().filter(entry -> entry.address().equals(address)).findFirst();
    }

    @Override
    public ActionResult add(final Address address, final Component name, final Cause cause) {
        if (this.find(address).isPresent()) {
            return ActionResult.NO_CHANGE;
        }
        final List<AddressBookEntry> entries = new ArrayList<>(this.entries());
        entries.add(new AddressBookEntry(address, name));
        this.save(entries);
        return ActionResult.DONE;
    }

    @Override
    public ActionResult rename(final Address address, final Component name, final Cause cause) {
        if (this.isLocked(address)) {
            return ActionResult.DENIED;
        }
        final List<AddressBookEntry> entries = new ArrayList<>(this.entries());
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).address().equals(address)) {
                if (entries.get(i).name().equals(name)) {
                    return ActionResult.NO_CHANGE;
                }
                entries.set(i, new AddressBookEntry(address, name));
                this.save(entries);
                return ActionResult.DONE;
            }
        }
        return ActionResult.NOT_POSSIBLE;
    }

    @Override
    public ActionResult remove(final Address address, final Cause cause) {
        if (this.isLocked(address)) {
            return ActionResult.DENIED;
        }
        final List<AddressBookEntry> entries = new ArrayList<>(this.entries());
        if (!entries.removeIf(entry -> entry.address().equals(address))) {
            return ActionResult.NO_CHANGE;
        }
        this.save(entries);
        return ActionResult.DONE;
    }

    @Override
    public ActionResult move(final Address address, final int index, final Cause cause) {
        final List<AddressBookEntry> entries = new ArrayList<>(this.entries());
        final Optional<AddressBookEntry> entry = this.find(address);
        if (entry.isEmpty() || index < 0 || index >= entries.size()) {
            return ActionResult.NOT_POSSIBLE;
        }
        final int from = entries.indexOf(entry.get());
        if (from == index) {
            return ActionResult.NO_CHANGE;
        }
        entries.remove(from);
        entries.add(index, entry.get());
        this.save(entries);
        return ActionResult.DONE;
    }
}
