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

package com.almuradev.sgcraft.command;

import com.almuradev.sgcraft.api.ActionResult;
import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.SGCraftApi;
import com.almuradev.sgcraft.api.SGCraftRegistries;
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.address.Addressing;
import com.almuradev.sgcraft.api.gate.DialResult;
import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The {@code /sgcraft} commands, which work gates without any screens, so a server's admins can run gates from the
 * console or chat. They go through the API like any add-on would.
 *
 * <p>Commands without a position use the nearest gate within {@link #NEAR_BLOCKS} blocks. Admins can name any gate
 * by a block of it with {@code /sgcraft gate <pos> ...}, and list every gate in a world.</p>
 */
public final class SGCommands {

    // How far away the nearest gate can be for the commands that use it
    private static final int NEAR_BLOCKS = 32;

    private static final SimpleCommandExceptionType NO_GATE_NEAR = new SimpleCommandExceptionType(
            SGCommands.text("no_gate_near", "There is no gate within %s blocks", SGCommands.NEAR_BLOCKS));
    private static final SimpleCommandExceptionType NO_GATE_THERE = new SimpleCommandExceptionType(
            SGCommands.text("no_gate_there", "There is no gate there"));
    private static final SimpleCommandExceptionType NOT_AN_ADDRESS = new SimpleCommandExceptionType(
            SGCommands.text("not_an_address", "That isn't an address. Use letters, digits and @, such as AB12-CDE"));
    private static final SimpleCommandExceptionType UNKNOWN_TYPE = new SimpleCommandExceptionType(
            SGCommands.text("unknown_type", "There is no gate type by that name"));

    private SGCommands() {
    }

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sgcraft")
                .then(Commands.literal("address")
                        .executes(context -> SGCommands.address(context.getSource(), SGCommands.nearest(context))))
                .then(Commands.literal("info")
                        .executes(context -> SGCommands.info(context.getSource(), SGCommands.nearest(context))))
                .then(SGCommands.dial(SGCommands::nearest))
                .then(Commands.literal("disconnect")
                        .executes(context -> SGCommands.disconnect(context.getSource(), SGCommands.nearest(context))))
                .then(Commands.literal("list")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> SGCommands.list(context.getSource(),
                                context.getSource().getLevel().dimension()))
                        .then(Commands.argument("world", DimensionArgument.dimension())
                                .executes(context -> SGCommands.list(context.getSource(),
                                        DimensionArgument.getDimension(context, "world").dimension()))))
                .then(Commands.literal("chevrons")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(SGCommands.chevrons(SGCommands::nearest)))
                .then(Commands.literal("type")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(SGCommands.type(SGCommands::nearest)))
                .then(Commands.literal("gate")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.literal("address")
                                        .executes(context -> SGCommands.address(context.getSource(),
                                                SGCommands.at(context))))
                                .then(Commands.literal("info")
                                        .executes(context -> SGCommands.info(context.getSource(),
                                                SGCommands.at(context))))
                                .then(SGCommands.dial(SGCommands::at))
                                .then(Commands.literal("disconnect")
                                        .executes(context -> SGCommands.disconnect(context.getSource(),
                                                SGCommands.at(context))))
                                .then(Commands.literal("chevrons").then(SGCommands.chevrons(SGCommands::at)))
                                .then(Commands.literal("type").then(SGCommands.type(SGCommands::at))))));
    }

    @FunctionalInterface
    private interface GateFinder {
        Stargate find(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> dial(final GateFinder finder) {
        return Commands.literal("dial")
                .then(Commands.argument("address", StringArgumentType.greedyString())
                        .executes(context -> SGCommands.dial(context.getSource(), finder.find(context),
                                StringArgumentType.getString(context, "address"))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> chevrons(final GateFinder finder) {
        return Commands.argument("count", IntegerArgumentType.integer(1, 9))
                .executes(context -> SGCommands.setChevrons(context.getSource(), finder.find(context),
                        IntegerArgumentType.getInteger(context, "count")));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> type(final GateFinder finder) {
        return Commands.argument("type", IdentifierArgument.id())
                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                        SGCommands.gateTypes(context.getSource()).keySet(), builder))
                .executes(context -> SGCommands.setType(context.getSource(), finder.find(context),
                        IdentifierArgument.getId(context, "type")));
    }

    private static Stargate nearest(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final CommandSourceStack source = context.getSource();
        final BlockPos pos = BlockPos.containing(source.getPosition());
        return SGCraftApi.get().stargates().mostAppropriate(GlobalPos.of(source.getLevel().dimension(), pos))
                .filter(gate -> gate.position().pos().closerThan(pos, SGCommands.NEAR_BLOCKS))
                .orElseThrow(SGCommands.NO_GATE_NEAR::create);
    }

    private static Stargate at(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
        return SGCraftApi.get().stargates().at(GlobalPos.of(context.getSource().getLevel().dimension(), pos))
                .orElseThrow(SGCommands.NO_GATE_THERE::create);
    }

    private static int address(final CommandSourceStack source, final Stargate gate) {
        final Addressing addressing = SGCraftApi.get().addressing();
        final ResourceKey<Level> world = gate.position().dimension();
        source.sendSuccess(() -> SGCommands.text("address.gate", "%s gate at %s",
                gate.type().value().name(), SGCommands.position(gate)), false);
        addressing.addressOf(gate.position(), world).ifPresent(address ->
                source.sendSuccess(() -> SGCommands.text("address.same_world", "  From this world: %s %s",
                        SGCommands.copyable(address), SGCommands.dialLink(address)), false));
        // From any other world of its group, its address is the same, so the first world that reaches it will do
        addressing.groupOf(world).worlds().stream()
                .filter(other -> !other.equals(world))
                .map(other -> addressing.addressOf(gate.position(), other))
                .flatMap(Optional::stream)
                .findFirst()
                .ifPresent(address -> source.sendSuccess(() -> SGCommands.text("address.other_world",
                        "  From other worlds: %s %s", SGCommands.copyable(address), SGCommands.dialLink(address)),
                        false));
        return 1;
    }

    private static int info(final CommandSourceStack source, final Stargate gate) {
        source.sendSuccess(() -> SGCommands.text("info.gate", "%s gate at %s", gate.type().value().name(),
                SGCommands.position(gate)), false);
        source.sendSuccess(() -> SGCommands.text("info.state", "  %s, %s chevrons",
                gate.state().name().toLowerCase(Locale.ROOT), gate.chevrons()), false);
        gate.connection().ifPresent(connection -> {
            final Stargate other = connection.other(gate).orElseThrow();
            final boolean dialling = connection.dialler().equals(gate);
            source.sendSuccess(() -> SGCommands.text(dialling ? "info.dialled" : "info.dialled_by",
                    dialling ? "  Dialled %s, the gate at %s" : "  Dialled by the gate at %2$s, with %1$s",
                    SGCommands.copyable(connection.address()), SGCommands.position(other)), false);
        });
        return 1;
    }

    private static int dial(final CommandSourceStack source, final Stargate gate, final String text)
            throws CommandSyntaxException {
        final Address address = Address.parse(text).orElseThrow(SGCommands.NOT_AN_ADDRESS::create);
        final Cause cause = SGCommands.cause(source);
        source.sendSuccess(() -> SGCommands.text("dial.start", "Dialling %s from the gate at %s",
                SGCommands.copyable(address), SGCommands.position(gate)), false);
        gate.dial(address, cause).whenComplete((result, error) -> {
            if (error != null) {
                source.sendFailure(SGCommands.text("dial.error", "Dialling failed: %s", String.valueOf(error)));
            } else if (result == DialResult.CONNECTED) {
                source.sendSuccess(() -> SGCommands.text("dial.connected", "The wormhole to %s is open",
                        SGCommands.copyable(address)), false);
            } else {
                source.sendFailure(SGCommands.dialFailure(result));
            }
        });
        return 1;
    }

    private static int disconnect(final CommandSourceStack source, final Stargate gate) {
        final ActionResult result = gate.disconnect(SGCommands.cause(source));
        if (result == ActionResult.DONE) {
            source.sendSuccess(() -> SGCommands.text("disconnect.done", "The gate at %s is shutting down",
                    SGCommands.position(gate)), false);
            return 1;
        }
        source.sendFailure(SGCommands.text("disconnect.idle", "The gate at %s has no connection",
                SGCommands.position(gate)));
        return 0;
    }

    private static int list(final CommandSourceStack source, final ResourceKey<Level> world) {
        final Collection<Stargate> gates = SGCraftApi.get().stargates().in(world);
        source.sendSuccess(() -> SGCommands.text("list.header", "%s gates in %s", gates.size(),
                world.identifier().toString()), false);
        gates.stream()
                .sorted(Comparator.comparing(gate -> gate.position().pos()))
                .forEach(gate -> {
                    final MutableComponent line = SGCommands.text("list.gate", "  %s at %s",
                            gate.type().value().name(), SGCommands.position(gate));
                    SGCraftApi.get().addressing().addressOf(gate.position(), world).ifPresent(address ->
                            line.append(" ").append(SGCommands.copyable(address)));
                    source.sendSuccess(() -> line, false);
                });
        return gates.size();
    }

    private static int setChevrons(final CommandSourceStack source, final Stargate gate, final int count) {
        return SGCommands.report(source, gate.setChevrons(count, SGCommands.cause(source)),
                SGCommands.text("chevrons.done", "The gate at %s has %s chevrons", SGCommands.position(gate),
                        count));
    }

    private static int setType(final CommandSourceStack source, final Stargate gate, final Identifier id)
            throws CommandSyntaxException {
        final Holder<GateType> type = Optional.ofNullable(SGCommands.gateTypes(source).get(id))
                .orElseThrow(SGCommands.UNKNOWN_TYPE::create);
        return SGCommands.report(source, gate.setType(type, SGCommands.cause(source)),
                SGCommands.text("type.done", "The gate at %s is now a %s gate", SGCommands.position(gate),
                        type.value().name()));
    }

    private static Map<Identifier, Holder<GateType>> gateTypes(final CommandSourceStack source) {
        final Registry<GateType> types = source.getServer().registryAccess()
                .lookupOrThrow(SGCraftRegistries.GATE_TYPE);
        final Map<Identifier, Holder<GateType>> byId = new LinkedHashMap<>();
        types.listElements().forEach(holder -> byId.put(holder.key().identifier(), holder));
        return byId;
    }

    private static int report(final CommandSourceStack source, final ActionResult result, final Component done) {
        switch (result) {
            case DONE -> {
                source.sendSuccess(() -> done, true);
                return 1;
            }
            case NO_CHANGE -> source.sendFailure(SGCommands.text("result.no_change", "Nothing changed"));
            case DENIED -> source.sendFailure(SGCommands.text("result.denied", "You aren't allowed to do that"));
            case CANCELLED -> source.sendFailure(SGCommands.text("result.cancelled", "Something stopped it"));
            default -> source.sendFailure(SGCommands.text("result.not_possible",
                    "That isn't possible now. A gate must be idle to change it"));
        }
        return 0;
    }

    private static Component dialFailure(final DialResult result) {
        return switch (result) {
            case INVALID_ADDRESS -> SGCommands.text("dial.invalid_address",
                    "No address has that many symbols. Addresses have 7, 8 or 9");
            case UNREACHABLE -> SGCommands.text("dial.unreachable", "No address reaches that gate from here");
            case NOT_ENOUGH_CHEVRONS -> SGCommands.text("dial.not_enough_chevrons",
                    "This gate doesn't have enough chevrons for that address");
            case NO_GATE -> SGCommands.text("dial.no_gate", "There is no gate at that address");
            case SAME_GATE -> SGCommands.text("dial.same_gate", "That is this gate's own address");
            case BUSY -> SGCommands.text("dial.busy", "This gate is busy");
            case TARGET_BUSY -> SGCommands.text("dial.target_busy", "The gate at that address is busy");
            case NOT_ENOUGH_ENERGY -> SGCommands.text("dial.not_enough_energy", "This gate doesn't have the energy");
            case DENIED -> SGCommands.text("dial.denied", "You aren't allowed to dial that");
            case CANCELLED -> SGCommands.text("dial.cancelled", "Something stopped the dial");
            case INTERRUPTED -> SGCommands.text("dial.interrupted", "The dial was interrupted");
            case GATE_GONE -> SGCommands.text("dial.gate_gone", "A gate was broken while dialling");
            default -> SGCommands.text("dial.failed", "The dial failed: %s", result.name());
        };
    }

    private static Cause cause(final CommandSourceStack source) {
        return source.getEntity() != null
                ? Cause.of(source.getEntity(), source.getServer())
                : Cause.of(source.getServer());
    }

    private static Component position(final Stargate gate) {
        final BlockPos pos = gate.position().pos();
        final String text = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        return Component.literal(text).withStyle(style -> style
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent.SuggestCommand(
                        "/tp @s " + pos.getX() + " " + (pos.getY() + 1) + " " + (pos.getZ() + 3)))
                .withHoverEvent(new HoverEvent.ShowText(SGCommands.text("hover.teleport",
                        "Click to teleport in front of it"))));
    }

    // The address, which copies itself when clicked
    private static Component copyable(final Address address) {
        final String text = address.toString();
        return Component.literal(text).withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent.CopyToClipboard(text))
                .withHoverEvent(new HoverEvent.ShowText(SGCommands.text("hover.copy", "Click to copy"))));
    }

    // A link that puts a dial command for the address in the chat box
    private static Component dialLink(final Address address) {
        return SGCommands.text("dial_link", "[Dial]").withStyle(style -> style
                .withColor(ChatFormatting.GOLD)
                .withClickEvent(new ClickEvent.SuggestCommand("/sgcraft dial " + address))
                .withHoverEvent(new HoverEvent.ShowText(SGCommands.text("hover.dial",
                        "Click to dial it from the nearest gate"))));
    }

    private static MutableComponent text(final String key, final String fallback, final Object... args) {
        return Component.translatableWithFallback("command.sgcraft." + key, fallback, args);
    }
}
