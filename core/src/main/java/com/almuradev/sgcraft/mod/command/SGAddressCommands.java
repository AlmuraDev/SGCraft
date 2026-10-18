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

import com.almuradev.sgcraft.api.address.Symbol;
import com.almuradev.sgcraft.mod.SGConfig;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.address.AddressChanges.Change;
import com.almuradev.sgcraft.mod.address.AddressChanges.Kind;
import com.almuradev.sgcraft.mod.address.AddressChanges.Outcome;
import com.almuradev.sgcraft.mod.address.AddressChanges;
import com.almuradev.sgcraft.mod.address.AddressLayout;
import com.almuradev.sgcraft.mod.address.AddressPlan;
import com.almuradev.sgcraft.mod.address.AddressSettings;
import com.almuradev.sgcraft.mod.address.SGAddressing;
import com.almuradev.sgcraft.mod.address.WorldAddress;
import com.almuradev.sgcraft.mod.gate.GateStore;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The address settings: how many chunks wide each addressed cell is and the point of origin addresses end with, for
 * every world or for one, and the groups worlds are sorted into. {@code /sgcraft addresses} shows each world's
 * addresses, now and after a restart, and {@code groups} lists the groups. {@code cell_chunks <chunks> [<world>]},
 * {@code point_of_origin <symbol> [<world>]}, {@code clear <world> [cell_chunks|point_of_origin]},
 * {@code group create <name> [<symbol>]},
 * {@code group remove <name>}, {@code group symbol <name> <symbol>}, {@code group assign <world> <group>} and
 * {@code secret_layout <true|false>} change them. Changes are written to the settings file and take effect when the
 * server next starts, as they change addresses in the worlds they reach; each warns about the worlds whose gates get
 * new addresses.
 */
final class SGAddressCommands {

    private static final DynamicCommandExceptionType NOT_A_SYMBOL = new DynamicCommandExceptionType(
            text -> SGCommands.text("addresses.not_a_symbol",
                    "%s isn't a point of origin. Give one letter, digit, + or ., giza or \"@\" for @, or none", text));
    private static final DynamicCommandExceptionType NOT_A_GROUP_SYMBOL = new DynamicCommandExceptionType(
            text -> SGCommands.text("addresses.not_a_group_symbol",
                    "%s isn't a group symbol. Give one letter, digit, + or ., giza or \"@\" for @, or auto", text));

    // The groups the server will next start with, to suggest
    private static final SuggestionProvider<CommandSourceStack> GROUPS = (context, builder) ->
            SharedSuggestionProvider.suggest(Stream.concat(Stream.of(AddressSettings.DEFAULT_GROUP),
                    SGConfig.load().addresses().groups().keySet().stream()).distinct(), builder);

    private SGAddressCommands() {
    }

    static ArgumentBuilder<CommandSourceStack, ?> addresses() {
        return Commands.literal("addresses")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> SGAddressCommands.show(context.getSource()))
                .then(Commands.literal("cell_chunks")
                        .then(Commands.argument("chunks", IntegerArgumentType.integer(1, AddressLayout.MAX_CELL_CHUNKS))
                                .executes(context -> SGAddressCommands.change(context, Kind.CELL_CHUNKS,
                                        Optional.empty(), "", SGAddressCommands.chunks(context)))
                                .then(Commands.argument("world", DimensionArgument.dimension())
                                        .executes(context -> SGAddressCommands.change(context, Kind.CELL_CHUNKS,
                                                SGAddressCommands.world(context), "",
                                                SGAddressCommands.chunks(context))))))
                .then(Commands.literal("point_of_origin")
                        .then(Commands.argument("symbol", StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        new String[] {"giza", "none"}, builder))
                                .executes(context -> SGAddressCommands.change(context, Kind.POINT_OF_ORIGIN,
                                        Optional.empty(), "", SGAddressCommands.origin(context)))
                                .then(Commands.argument("world", DimensionArgument.dimension())
                                        .executes(context -> SGAddressCommands.change(context, Kind.POINT_OF_ORIGIN,
                                                SGAddressCommands.world(context), "",
                                                SGAddressCommands.origin(context))))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("world", DimensionArgument.dimension())
                                .executes(context -> SGAddressCommands.change(context, Kind.CLEAR_WORLD,
                                        SGAddressCommands.world(context), "", ""))
                                .then(Commands.literal("cell_chunks")
                                        .executes(context -> SGAddressCommands.change(context, Kind.CLEAR_CELL,
                                                SGAddressCommands.world(context), "", "")))
                                .then(Commands.literal("point_of_origin")
                                        .executes(context -> SGAddressCommands.change(context, Kind.CLEAR_ORIGIN,
                                                SGAddressCommands.world(context), "", "")))))
                .then(Commands.literal("secret_layout")
                        .then(Commands.argument("on", BoolArgumentType.bool())
                                .executes(context -> SGAddressCommands.change(context, Kind.SECRET_LAYOUT,
                                        Optional.empty(), "",
                                        String.valueOf(BoolArgumentType.getBool(context, "on"))))))
                .then(Commands.literal("groups")
                        .executes(context -> SGAddressCommands.groups(context.getSource())))
                .then(Commands.literal("group")
                        .then(Commands.literal("create")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(context -> SGAddressCommands.change(context, Kind.CREATE_GROUP,
                                                Optional.empty(), SGAddressCommands.name(context, "name"), ""))
                                        .then(Commands.argument("symbol", StringArgumentType.string())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        new String[] {"auto"}, builder))
                                                .executes(context -> SGAddressCommands.change(context,
                                                        Kind.CREATE_GROUP, Optional.empty(),
                                                        SGAddressCommands.name(context, "name"),
                                                        SGAddressCommands.groupSymbol(context))))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .suggests(SGAddressCommands.GROUPS)
                                        .executes(context -> SGAddressCommands.change(context, Kind.REMOVE_GROUP,
                                                Optional.empty(), SGAddressCommands.name(context, "name"), ""))))
                        .then(Commands.literal("symbol")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .suggests(SGAddressCommands.GROUPS)
                                        .then(Commands.argument("symbol", StringArgumentType.string())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        new String[] {"auto", "giza"}, builder))
                                                .executes(context -> SGAddressCommands.change(context,
                                                        Kind.GROUP_SYMBOL, Optional.empty(),
                                                        SGAddressCommands.name(context, "name"),
                                                        SGAddressCommands.groupSymbol(context))))))
                        .then(Commands.literal("assign")
                                .then(Commands.argument("world", DimensionArgument.dimension())
                                        .then(Commands.argument("group", StringArgumentType.word())
                                                .suggests(SGAddressCommands.GROUPS)
                                                .executes(context -> SGAddressCommands.change(context,
                                                        Kind.WORLD_GROUP, SGAddressCommands.world(context),
                                                        SGAddressCommands.name(context, "group"), ""))))));
    }

    private static Optional<Identifier> world(final CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        return Optional.of(DimensionArgument.getDimension(context, "world").dimension().identifier());
    }

    private static String chunks(final CommandContext<CommandSourceStack> context) {
        return String.valueOf(IntegerArgumentType.getInteger(context, "chunks"));
    }

    private static String name(final CommandContext<CommandSourceStack> context, final String argument) {
        return StringArgumentType.getString(context, argument).toLowerCase(Locale.ROOT);
    }

    private static int show(final CommandSourceStack source) {
        final MinecraftServer server = source.getServer();
        final SGAddressing addressing = SGServer.require().addressing();
        final AddressSettings pending = SGConfig.load().addresses();
        final AddressPlan next = SGAddressCommands.nextPlan(server, pending);
        final GateStore store = server.getDataStorage().computeIfAbsent(GateStore.TYPE);
        for (final ServerLevel level : server.getAllLevels()) {
            final ResourceKey<Level> world = level.dimension();
            final WorldAddress now = addressing.worldAddress(world);
            final WorldAddress after = WorldAddress.of(pending.layoutOf(world), next, world);
            final long gates = store.gateCount(world);
            source.sendSuccess(() -> SGCommands.text("addresses.world", "%s: %s, %s gates",
                    world.identifier().toString(), now.describe(), gates), false);
            if (!after.equals(now)) {
                source.sendSuccess(() -> SGCommands.text("addresses.pending", "  After a restart: %s",
                        after.describe()).withStyle(gates > 0 ? ChatFormatting.RED : ChatFormatting.YELLOW), false);
            }
        }
        return 1;
    }

    private static int groups(final CommandSourceStack source) {
        final AddressPlan now = SGServer.require().addressing().plan();
        final AddressPlan next = SGAddressCommands.nextPlan(source.getServer(), SGConfig.load().addresses());
        for (final AddressPlan.Group group : next.groups()) {
            final Optional<AddressPlan.Group> running = now.group(group.name());
            source.sendSuccess(() -> SGCommands.text("addresses.group", "%s (%s): %s of %s worlds", group.name(),
                    String.valueOf(group.symbol().character()), next.worldsIn(group.name()).size(), Symbol.COUNT),
                    false);
            if (running.isEmpty()) {
                source.sendSuccess(() -> SGCommands.text("addresses.group_new", "  New. It exists after a restart")
                        .withStyle(ChatFormatting.YELLOW), false);
            } else if (!running.get().symbol().equals(group.symbol())) {
                source.sendSuccess(() -> SGCommands.text("addresses.group_symbol_now", "  Its symbol is %s until "
                        + "a restart", String.valueOf(running.get().symbol().character()))
                        .withStyle(ChatFormatting.YELLOW), false);
            }
        }
        for (final AddressPlan.Group group : now.groups()) {
            if (next.group(group.name()).isEmpty()) {
                source.sendSuccess(() -> SGCommands.text("addresses.group_gone", "%s (%s): gone after a restart",
                        group.name(), String.valueOf(group.symbol().character())).withStyle(ChatFormatting.YELLOW),
                        false);
            }
        }
        next.problems().forEach(problem -> source.sendFailure(problem.text()));
        return 1;
    }

    private static AddressPlan nextPlan(final MinecraftServer server, final AddressSettings pending) {
        final List<ResourceKey<Level>> worlds = new ArrayList<>();
        server.getAllLevels().forEach(level -> worlds.add(level.dimension()));
        return AddressPlan.of(pending, SGAddressing.recorded(server.getDataStorage().computeIfAbsent(GateStore.TYPE)),
                worlds);
    }

    // Reads a point of origin as typed: one symbol, giza for @, or none for addresses without one
    private static String origin(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final String text = StringArgumentType.getString(context, "symbol");
        if (text.toLowerCase(Locale.ROOT).equals("none")) {
            return "";
        }
        return SGAddressCommands.symbol(text).orElseThrow(() -> SGAddressCommands.NOT_A_SYMBOL.create(text));
    }

    // Reads a group's symbol as typed: one symbol, giza for @, or auto for one SGCraft chooses
    private static String groupSymbol(final CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        final String text = StringArgumentType.getString(context, "symbol");
        if (text.toLowerCase(Locale.ROOT).equals("auto")) {
            return "";
        }
        return SGAddressCommands.symbol(text).orElseThrow(() -> SGAddressCommands.NOT_A_GROUP_SYMBOL.create(text));
    }

    private static Optional<String> symbol(final String text) {
        if (text.toLowerCase(Locale.ROOT).equals("giza")) {
            return Optional.of(String.valueOf(Symbol.GIZA.character()));
        }
        final String upper = text.toUpperCase(Locale.ROOT);
        return upper.length() == 1 && Symbol.of(upper.charAt(0)).isPresent() ? Optional.of(upper) : Optional.empty();
    }

    // Saves a change and tells the caller what it does when the server next starts, warning about each world whose
    // gates get new addresses, in chat and in the server log
    private static int change(final CommandContext<CommandSourceStack> context, final Kind kind,
            final Optional<Identifier> world, final String group, final String value) {
        final CommandSourceStack source = context.getSource();
        final Outcome outcome = AddressChanges.run(source.getServer(), new Change(kind, world, group, value), true);
        if (outcome.refusal().isPresent()) {
            source.sendFailure(outcome.refusal().get());
            return 0;
        }
        outcome.lines().forEach(line -> source.sendSuccess(() -> line, true));
        outcome.warnings().forEach(source::sendFailure);
        if (outcome.warnings().isEmpty()) {
            source.sendSuccess(() -> SGCommands.text("addresses.no_gates_changed", "No gate's address changes"),
                    false);
        }
        return 1;
    }
}
