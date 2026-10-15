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

package com.almuradev.sgcraft.mod.command;

import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.addressbook.AddressBookEntry;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.addressbook.DefaultAddresses;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /sgcraft default_addresses}: the addresses every new PDD and gate control tablet starts with. On its own it
 * lists them; {@code add <name> <address>} adds one, or renames it if it is there already;
 * {@code remove <address>} takes one off; {@code lock <address>} and {@code unlock <address>} set whether holders
 * may rename or remove it. Changes apply to PDDs and tablets used for the first time from then on, and are written to
 * {@code sgcraft-default-addresses.json}.
 */
final class SGDefaultAddressCommands {

    private static final SimpleCommandExceptionType NOT_AN_ADDRESS = new SimpleCommandExceptionType(
            SGCommands.text("not_an_address", "That isn't an address. Use letters, digits and @, such as AB12-CDE"));
    private static final SimpleCommandExceptionType NOT_LISTED = new SimpleCommandExceptionType(
            SGCommands.text("default_addresses.not_listed", "That address isn't one of the default addresses"));

    private SGDefaultAddressCommands() {
    }

    static ArgumentBuilder<CommandSourceStack, ?> defaultAddresses() {
        return Commands.literal("default_addresses")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> SGDefaultAddressCommands.show(context.getSource()))
                .then(Commands.literal("add")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("address", StringArgumentType.greedyString())
                                        .executes(SGDefaultAddressCommands::add))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("address", StringArgumentType.greedyString())
                                .executes(SGDefaultAddressCommands::remove)))
                .then(Commands.literal("lock")
                        .then(Commands.argument("address", StringArgumentType.greedyString())
                                .executes(context -> SGDefaultAddressCommands.lock(context, true))))
                .then(Commands.literal("unlock")
                        .then(Commands.argument("address", StringArgumentType.greedyString())
                                .executes(context -> SGDefaultAddressCommands.lock(context, false))));
    }

    private static int show(final CommandSourceStack source) {
        final List<DefaultAddresses.Entry> addresses = SGServer.require().defaultAddresses();
        if (addresses.isEmpty()) {
            source.sendSuccess(() -> SGCommands.text("default_addresses.none",
                    "New PDDs and tablets start with no addresses"), false);
            return 0;
        }
        source.sendSuccess(() -> SGCommands.text("default_addresses.header",
                "New PDDs and tablets start with these addresses:"), false);
        for (final DefaultAddresses.Entry entry : addresses) {
            final Component line = Component.literal("  ").append(Component.literal(entry.entry().address()
                    .toString()).withStyle(ChatFormatting.GOLD)).append(" ").append(entry.entry().name());
            source.sendSuccess(() -> entry.locked() ? line.copy().append(" ").append(SGCommands.text(
                    "default_addresses.locked", "(locked)").withStyle(ChatFormatting.GRAY)) : line, false);
        }
        return addresses.size();
    }

    private static int add(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final Address address = SGDefaultAddressCommands.address(context);
        final String name = StringArgumentType.getString(context, "name");
        final List<DefaultAddresses.Entry> addresses = new ArrayList<>(SGServer.require().defaultAddresses());
        final int index = SGDefaultAddressCommands.indexOf(addresses, address);
        final AddressBookEntry entry = new AddressBookEntry(address, Component.literal(name));
        if (index < 0) {
            addresses.add(new DefaultAddresses.Entry(entry, false));
        } else {
            addresses.set(index, new DefaultAddresses.Entry(entry, addresses.get(index).locked()));
        }
        return SGDefaultAddressCommands.save(context.getSource(), addresses, SGCommands.text(
                "default_addresses.added", "New PDDs and tablets will start with %s as %s", address.toString(), name));
    }

    private static int remove(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final Address address = SGDefaultAddressCommands.address(context);
        final List<DefaultAddresses.Entry> addresses = new ArrayList<>(SGServer.require().defaultAddresses());
        addresses.remove(SGDefaultAddressCommands.listed(addresses, address));
        return SGDefaultAddressCommands.save(context.getSource(), addresses, SGCommands.text(
                "default_addresses.removed", "New PDDs and tablets won't start with %s", address.toString()));
    }

    private static int lock(final CommandContext<CommandSourceStack> context, final boolean locked)
            throws CommandSyntaxException {
        final Address address = SGDefaultAddressCommands.address(context);
        final List<DefaultAddresses.Entry> addresses = new ArrayList<>(SGServer.require().defaultAddresses());
        final int index = SGDefaultAddressCommands.listed(addresses, address);
        addresses.set(index, new DefaultAddresses.Entry(addresses.get(index).entry(), locked));
        return SGDefaultAddressCommands.save(context.getSource(), addresses, locked
                ? SGCommands.text("default_addresses.now_locked",
                        "New PDDs and tablets will hold %s locked, so their holders can't rename or remove it",
                        address.toString())
                : SGCommands.text("default_addresses.now_unlocked",
                        "Holders of new PDDs and tablets may rename or remove %s", address.toString()));
    }

    private static Address address(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return Address.parse(StringArgumentType.getString(context, "address"))
                .orElseThrow(SGDefaultAddressCommands.NOT_AN_ADDRESS::create);
    }

    private static int indexOf(final List<DefaultAddresses.Entry> addresses, final Address address) {
        for (int i = 0; i < addresses.size(); i++) {
            if (addresses.get(i).entry().address().equals(address)) {
                return i;
            }
        }
        return -1;
    }

    // Where the address is in the list, which it must be
    private static int listed(final List<DefaultAddresses.Entry> addresses, final Address address)
            throws CommandSyntaxException {
        final int index = SGDefaultAddressCommands.indexOf(addresses, address);
        if (index < 0) {
            throw SGDefaultAddressCommands.NOT_LISTED.create();
        }
        return index;
    }

    private static int save(final CommandSourceStack source, final List<DefaultAddresses.Entry> addresses,
            final Component done) {
        final boolean saved = SGServer.require().setDefaultAddresses(addresses);
        source.sendSuccess(() -> done, true);
        if (!saved) {
            source.sendFailure(SGCommands.text("default_addresses.not_saved",
                    "The change applies until the server stops, but the file couldn't be written"));
        }
        return 1;
    }
}
