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

import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.SGCraftApi;
import com.almuradev.sgcraft.api.SGCraftRegistries;
import com.almuradev.sgcraft.api.access.AddressRule;
import com.almuradev.sgcraft.api.access.GateRight;
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.addressbook.AddressBook;
import com.almuradev.sgcraft.api.gate.GatePlacement;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.GateProperty;
import com.almuradev.sgcraft.api.gate.GateSize;
import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.api.gate.Iris;
import com.almuradev.sgcraft.api.gate.IrisType;
import com.almuradev.sgcraft.api.gate.IrisTypes;
import com.almuradev.sgcraft.api.gate.PlacementResult;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.block.GateSpawnerBlock;
import com.almuradev.sgcraft.mod.block.entity.StargateBaseBlockEntity;
import com.almuradev.sgcraft.mod.command.SGCommands.GateFinder;
import com.almuradev.sgcraft.mod.gate.DialStyle;
import com.almuradev.sgcraft.mod.gate.GateOrientation;
import com.almuradev.sgcraft.mod.gate.GateShape;
import com.almuradev.sgcraft.mod.gate.GlyphText;
import com.almuradev.sgcraft.mod.gate.SGIris;
import com.almuradev.sgcraft.mod.villager.SGVillagers;
import com.google.gson.JsonParser;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The {@code /sgcraft} commands for each feature beyond dialing: the iris, energy, every property, access, the
 * owner, placing gates, and the caller's address book. Those that act on a gate use the same gate as the dialing
 * commands: the selected gate, or the gate looked at, or with {@code gate <pos>}, the gate at a position.
 */
final class SGGateCommands {

    private static final SimpleCommandExceptionType UNKNOWN_PROPERTY = new SimpleCommandExceptionType(
            SGCommands.text("unknown_property", "There is no gate property by that name"));
    private static final SimpleCommandExceptionType BAD_VALUE = new SimpleCommandExceptionType(
            SGCommands.text("bad_value", "That isn't a valid value for the property, written as JSON"));
    private static final SimpleCommandExceptionType UNKNOWN_PLAYER = new SimpleCommandExceptionType(
            SGCommands.text("unknown_player", "This server has never seen a player by that name"));
    private static final SimpleCommandExceptionType NOT_AN_ADDRESS = new SimpleCommandExceptionType(
            SGCommands.text("not_an_address", "That isn't an address. Use letters, digits and @, such as AB12-CDE"));
    private static final SimpleCommandExceptionType PLAYERS_ONLY = new SimpleCommandExceptionType(
            SGCommands.text("players_only", "Only players have address books"));

    private SGGateCommands() {
    }

    /** The subcommands that act on a gate, for {@code /sgcraft} and for {@code /sgcraft gate <pos>}. */
    static void gateCommands(final ArgumentBuilder<CommandSourceStack, ?> parent, final GateFinder finder) {
        parent.then(SGGateCommands.iris(finder))
                .then(SGGateCommands.energy(finder))
                .then(SGGateCommands.property(finder))
                .then(SGGateCommands.rights(finder))
                .then(SGGateCommands.owner(finder))
                .then(SGGateCommands.rule(finder))
                .then(SGGateCommands.camouflage(finder))
                .then(SGGateCommands.orientation(finder))
                .then(SGGateCommands.tokra(finder));
    }

    // orientation [face_up | face_down]; only a gate lying flat turns, where it lies
    private static ArgumentBuilder<CommandSourceStack, ?> orientation(final GateFinder finder) {
        final ArgumentBuilder<CommandSourceStack, ?> orientation = Commands.literal("orientation")
                .executes(context -> SGGateCommands.showOrientation(context.getSource(), finder.find(context)));
        for (final GateOrientation flat : new GateOrientation[] {GateOrientation.FACE_UP, GateOrientation.FACE_DOWN}) {
            orientation.then(Commands.literal(flat.getSerializedName())
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .executes(context -> SGGateCommands.turn(context.getSource(), finder.find(context), flat)));
        }
        return orientation;
    }

    // Its result numbers the orientations as the original did: 1 standing, 2 face up, 3 face down
    private static int showOrientation(final CommandSourceStack source, final Stargate gate) {
        final GateOrientation lying = GateOrientation.of(gate.orientation());
        final String facing = GateOrientation.facing(gate.orientation()).getSerializedName();
        source.sendSuccess(() -> switch (lying) {
            case STANDING -> SGCommands.text("orientation.standing", "The gate at %s stands upright, facing %s",
                    SGCommands.position(gate), facing);
            case FACE_UP -> SGCommands.text("orientation.face_up",
                    "The gate at %s lies flat, face up, its base facing %s", SGCommands.position(gate), facing);
            case FACE_DOWN -> SGCommands.text("orientation.face_down",
                    "The gate at %s lies flat, face down, its base facing %s", SGCommands.position(gate), facing);
        }, false);
        return lying.ordinal() + 1;
    }

    private static int turn(final CommandSourceStack source, final Stargate gate, final GateOrientation lying) {
        if (!GateOrientation.of(gate.orientation()).isFlat()) {
            source.sendFailure(SGCommands.text("orientation.standing_fixed",
                    "The gate at %s stands upright; only a gate lying flat can be turned", SGCommands.position(gate)));
            return 0;
        }
        return SGCommands.report(source, gate.setOrientation(lying.frontAndTop(GateOrientation.facing(
                gate.orientation())), SGCommands.cause(source)), lying == GateOrientation.FACE_UP
                ? SGCommands.text("orientation.turned_up", "Turned the gate at %s face up", SGCommands.position(gate))
                : SGCommands.text("orientation.turned_down", "Turned the gate at %s face down",
                        SGCommands.position(gate)));
    }

    private static final SimpleCommandExceptionType NO_ROOM = new SimpleCommandExceptionType(
            SGCommands.text("tokra.no_room", "There's no room in front of the gate for a Tok'ra to stand"));

    // tokra; a Tok'ra in front of the gate, as one stands at a gate a structure left
    private static ArgumentBuilder<CommandSourceStack, ?> tokra(final GateFinder finder) {
        return Commands.literal("tokra").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> SGGateCommands.tokra(context.getSource(), finder.find(context)));
    }

    private static int tokra(final CommandSourceStack source, final Stargate gate) throws CommandSyntaxException {
        final ServerLevel level = source.getServer().getLevel(gate.position().dimension());
        if (level == null || !level.isLoaded(gate.position().pos())) {
            throw SGGateCommands.NO_BASE.create();
        }
        if (SGVillagers.spawnTokra(level, gate.position().pos(),
                GateOrientation.facing(gate.orientation())).isEmpty()) {
            throw SGGateCommands.NO_ROOM.create();
        }
        source.sendSuccess(() -> SGCommands.text("tokra.spawned", "A Tok'ra stands at the gate at %s",
                SGCommands.position(gate)), true);
        return Command.SINGLE_SUCCESS;
    }

    private static final SimpleCommandExceptionType NOT_CAMOUFLAGE = new SimpleCommandExceptionType(
            SGCommands.text("camouflage.bad_block", "That block can't camouflage a gate"));
    private static final SimpleCommandExceptionType NO_BASE = new SimpleCommandExceptionType(
            SGCommands.text("camouflage.no_base", "The gate's base isn't loaded"));

    // camouflage [set <slot> <block> | clear [<slot>]]; slots run from 0 at the left of the bottom row as seen from in
    // front to 4 at the right
    private static ArgumentBuilder<CommandSourceStack, ?> camouflage(final GateFinder finder) {
        final int last = StargateBaseBlockEntity.CAMOUFLAGE_SLOTS - 1;
        return Commands.literal("camouflage")
                .executes(context -> SGGateCommands.showCamouflage(context.getSource(), finder.find(context)))
                .then(Commands.literal("set").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("slot", IntegerArgumentType.integer(0, last))
                                .then(Commands.argument("block", IdentifierArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                BuiltInRegistries.BLOCK.keySet(), builder))
                                        .executes(context -> SGGateCommands.setCamouflage(context,
                                                finder.find(context), IntegerArgumentType.getInteger(context, "slot"),
                                                IdentifierArgument.getId(context, "block"))))))
                .then(Commands.literal("clear").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> SGGateCommands.clearCamouflage(context.getSource(), finder.find(context),
                                -1))
                        .then(Commands.argument("slot", IntegerArgumentType.integer(0, last))
                                .executes(context -> SGGateCommands.clearCamouflage(context.getSource(),
                                        finder.find(context), IntegerArgumentType.getInteger(context, "slot")))));
    }

    private static StargateBaseBlockEntity base(final CommandSourceStack source, final Stargate gate)
            throws CommandSyntaxException {
        final ServerLevel level = source.getServer().getLevel(gate.position().dimension());
        if (level == null || !level.isLoaded(gate.position().pos())
                || !(level.getBlockEntity(gate.position().pos()) instanceof StargateBaseBlockEntity base)) {
            throw SGGateCommands.NO_BASE.create();
        }
        return base;
    }

    private static int showCamouflage(final CommandSourceStack source, final Stargate gate)
            throws CommandSyntaxException {
        final StargateBaseBlockEntity base = SGGateCommands.base(source, gate);
        source.sendSuccess(() -> SGCommands.text("camouflage.header", "Camouflage of the gate at %s, left to right:",
                SGCommands.position(gate)), false);
        for (int slot = 0; slot < StargateBaseBlockEntity.CAMOUFLAGE_SLOTS; slot++) {
            final ItemStack stack = base.camouflage().getItem(slot);
            final Component block = stack.isEmpty() ? SGCommands.text("camouflage.none", "nothing")
                    : stack.getHoverName();
            final int shown = slot;
            source.sendSuccess(() -> Component.literal("  " + shown + ": ").append(block.copy()
                    .withStyle(ChatFormatting.AQUA)), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int setCamouflage(final CommandContext<CommandSourceStack> context, final Stargate gate,
            final int slot, final Identifier id) throws CommandSyntaxException {
        final ItemStack stack = BuiltInRegistries.BLOCK.getOptional(id).map(block -> new ItemStack(block.asItem()))
                .orElse(ItemStack.EMPTY);
        if (!StargateBaseBlockEntity.isCamouflage(stack)) {
            throw SGGateCommands.NOT_CAMOUFLAGE.create();
        }
        SGGateCommands.base(context.getSource(), gate).camouflage().setItem(slot, stack);
        context.getSource().sendSuccess(() -> SGCommands.text("camouflage.set",
                "Camouflaged block %s of the gate at %s with %s", slot, SGCommands.position(gate),
                stack.getHoverName()), true);
        return Command.SINGLE_SUCCESS;
    }

    // Clears one slot, or every slot when slot is -1; what was there is lost, as with clearing a container
    private static int clearCamouflage(final CommandSourceStack source, final Stargate gate, final int slot)
            throws CommandSyntaxException {
        final StargateBaseBlockEntity base = SGGateCommands.base(source, gate);
        if (slot < 0) {
            base.camouflage().clearContent();
            base.camouflage().setChanged();
        } else {
            base.camouflage().setItem(slot, ItemStack.EMPTY);
        }
        source.sendSuccess(() -> SGCommands.text("camouflage.cleared", "Cleared the camouflage of the gate at %s",
                SGCommands.position(gate)), true);
        return Command.SINGLE_SUCCESS;
    }

    // iris [open | close | fit [metal | shield] | remove]; a shield is raised by closing it and lowered by opening it
    private static ArgumentBuilder<CommandSourceStack, ?> iris(final GateFinder finder) {
        return Commands.literal("iris")
                .executes(context -> SGGateCommands.showIris(context.getSource(), finder.find(context)))
                .then(Commands.literal("open").executes(context -> SGGateCommands.moveIris(context, finder, true)))
                .then(Commands.literal("close").executes(context -> SGGateCommands.moveIris(context, finder, false)))
                .then(Commands.literal("fit").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> SGGateCommands.fitIris(context, finder, IrisTypes.METAL))
                        .then(Commands.literal("metal")
                                .executes(context -> SGGateCommands.fitIris(context, finder, IrisTypes.METAL)))
                        .then(Commands.literal("shield")
                                .executes(context -> SGGateCommands.fitIris(context, finder, IrisTypes.SHIELD))))
                .then(Commands.literal("remove").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> {
                            final Stargate gate = finder.find(context);
                            return SGCommands.report(context.getSource(), gate.remove(GateProperties.IRIS,
                                    SGCommands.cause(context.getSource())),
                                    SGCommands.text("iris.removed", "Took the iris off the gate at %s",
                                            SGCommands.position(gate)));
                        }));
    }

    private static int fitIris(final CommandContext<CommandSourceStack> context, final GateFinder finder,
            final ResourceKey<IrisType> type) throws CommandSyntaxException {
        final Stargate gate = finder.find(context);
        return SGCommands.report(context.getSource(), gate.set(GateProperties.IRIS, type,
                SGCommands.cause(context.getSource())), SGIris.isShield(type)
                ? SGCommands.text("iris.fitted_shield", "Fitted a shield to the gate at %s", SGCommands.position(gate))
                : SGCommands.text("iris.fitted", "Fitted an iris to the gate at %s", SGCommands.position(gate)));
    }

    private static int showIris(final CommandSourceStack source, final Stargate gate) {
        final Optional<Iris> iris = gate.iris();
        if (iris.isEmpty()) {
            source.sendSuccess(() -> SGCommands.text("iris.none", "The gate at %s has no iris",
                    SGCommands.position(gate)), false);
            return 0;
        }
        if (SGIris.isShield(iris.get().type())) {
            source.sendSuccess(() -> SGCommands.text("iris.shield_state", "The gate at %s has a shield: %s",
                    SGCommands.position(gate), SGIris.shieldText(iris.get().state())), false);
            return 1;
        }
        source.sendSuccess(() -> SGCommands.text("iris.state", "The gate at %s has an iris, %s",
                SGCommands.position(gate), iris.get().state().name().toLowerCase(Locale.ROOT)), false);
        return 1;
    }

    private static int moveIris(final CommandContext<CommandSourceStack> context, final GateFinder finder,
            final boolean open) throws CommandSyntaxException {
        final Stargate gate = finder.find(context);
        final Optional<Iris> iris = gate.iris();
        if (iris.isEmpty()) {
            context.getSource().sendFailure(SGCommands.text("iris.none", "The gate at %s has no iris",
                    SGCommands.position(gate)));
            return 0;
        }
        final Cause cause = SGCommands.cause(context.getSource());
        if (SGIris.isShield(iris.get().type())) {
            return SGCommands.report(context.getSource(), open ? iris.get().open(cause) : iris.get().close(cause),
                    open ? SGCommands.text("iris.lowering", "The shield is falling")
                            : SGCommands.text("iris.raising", "The shield is rising"));
        }
        return SGCommands.report(context.getSource(), open ? iris.get().open(cause) : iris.get().close(cause),
                open ? SGCommands.text("iris.opening", "The iris is opening")
                        : SGCommands.text("iris.closing", "The iris is closing"));
    }

    // energy [add <amount> | set <amount>]
    private static ArgumentBuilder<CommandSourceStack, ?> energy(final GateFinder finder) {
        return Commands.literal("energy")
                .executes(context -> {
                    final Stargate gate = finder.find(context);
                    final boolean needs = gate.get(GateProperties.REQUIRES_POWER).orElse(true);
                    context.getSource().sendSuccess(() -> SGCommands.text("energy.show",
                            "The gate at %s holds %s of %s energy%s", SGCommands.position(gate),
                            gate.energy().stored(), gate.energy().capacity(),
                            needs ? "" : SGCommands.text("energy.free", ", and dials without it")), false);
                    return 1;
                })
                .then(Commands.literal("add").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                .executes(context -> {
                                    final Stargate gate = finder.find(context);
                                    final long added = gate.energy().insert(
                                            LongArgumentType.getLong(context, "amount"), false);
                                    context.getSource().sendSuccess(() -> SGCommands.text("energy.added",
                                            "Added %s energy; the gate holds %s", added, gate.energy().stored()),
                                            true);
                                    return 1;
                                })))
                .then(Commands.literal("set").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                .executes(context -> {
                                    final Stargate gate = finder.find(context);
                                    gate.energy().extract(gate.energy().stored(), false);
                                    gate.energy().insert(LongArgumentType.getLong(context, "amount"), false);
                                    context.getSource().sendSuccess(() -> SGCommands.text("energy.set",
                                            "The gate holds %s energy", gate.energy().stored()), true);
                                    return 1;
                                })));
    }

    // property [<id> [set <json> | reset]]
    private static ArgumentBuilder<CommandSourceStack, ?> property(final GateFinder finder) {
        return Commands.literal("property")
                .executes(context -> SGGateCommands.listProperties(context.getSource(), finder.find(context)))
                .then(Commands.argument("property", IdentifierArgument.id())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                SGGateCommands.properties(context.getSource()).keySet(), builder))
                        .executes(context -> SGGateCommands.showProperty(context.getSource(), finder.find(context),
                                SGGateCommands.property(context)))
                        .then(Commands.literal("set").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.argument("value", StringArgumentType.greedyString())
                                        .executes(context -> SGGateCommands.setProperty(context.getSource(),
                                                finder.find(context), SGGateCommands.property(context),
                                                StringArgumentType.getString(context, "value")))))
                        .then(Commands.literal("reset").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(context -> {
                                    final Stargate gate = finder.find(context);
                                    return SGCommands.report(context.getSource(), gate.remove(
                                            SGGateCommands.property(context), SGCommands.cause(context.getSource())),
                                            SGCommands.text("property.reset", "Reset it to its default"));
                                })));
    }

    private static Map<Identifier, GateProperty<?>> properties(final CommandSourceStack source) {
        final Registry<GateProperty<?>> registry = source.getServer().registryAccess()
                .lookupOrThrow(SGCraftRegistries.GATE_PROPERTY);
        final Map<Identifier, GateProperty<?>> byId = new HashMap<>();
        for (final GateProperty<?> property : registry) {
            byId.put(registry.getKey(property), property);
        }
        return byId;
    }

    private static GateProperty<?> property(final CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        final GateProperty<?> property = SGGateCommands.properties(context.getSource())
                .get(IdentifierArgument.getId(context, "property"));
        if (property == null) {
            throw SGGateCommands.UNKNOWN_PROPERTY.create();
        }
        return property;
    }

    private static int listProperties(final CommandSourceStack source, final Stargate gate) {
        source.sendSuccess(() -> SGCommands.text("property.header", "Properties of the gate at %s",
                SGCommands.position(gate)), false);
        SGGateCommands.properties(source).entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> source.sendSuccess(() -> SGGateCommands.line(entry.getKey(), gate,
                        entry.getValue()), false));
        return 1;
    }

    private static int showProperty(final CommandSourceStack source, final Stargate gate,
            final GateProperty<?> property) {
        final Identifier id = SGGateCommands.properties(source).entrySet().stream()
                .filter(entry -> entry.getValue() == property).map(Map.Entry::getKey).findFirst().orElseThrow();
        source.sendSuccess(() -> SGGateCommands.line(id, gate, property), false);
        return 1;
    }

    private static <T> MutableComponent line(final Identifier id, final Stargate gate,
            final GateProperty<T> property) {
        final Optional<T> value = gate.get(property);
        final String text = value.flatMap(v -> property.codec().encodeStart(JsonOps.INSTANCE, v).result())
                .map(Object::toString).orElse("none");
        return Component.literal("  " + id + " = ").append(Component.literal(text).withStyle(ChatFormatting.AQUA));
    }

    private static <T> int setProperty(final CommandSourceStack source, final Stargate gate,
            final GateProperty<T> property, final String json) throws CommandSyntaxException {
        final Optional<T> value;
        try {
            value = property.codec().parse(JsonOps.INSTANCE, JsonParser.parseString(json)).result();
        } catch (final RuntimeException e) {
            throw SGGateCommands.BAD_VALUE.create();
        }
        if (value.isEmpty()) {
            throw SGGateCommands.BAD_VALUE.create();
        }
        return SGCommands.report(source, gate.set(property, value.get(), SGCommands.cause(source)),
                SGCommands.text("property.set", "Set it to %s", json));
    }

    // owner [<player> | --none]; anyone may see a gate's owner, and its owner or an operator hand it over
    private static ArgumentBuilder<CommandSourceStack, ?> owner(final GateFinder finder) {
        return Commands.literal("owner")
                .executes(context -> SGGateCommands.showOwner(context.getSource(), finder.find(context)))
                .then(Commands.literal("--none")
                        .executes(context -> SGGateCommands.setOwner(context.getSource(), finder.find(context),
                                Optional.empty(), "")))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                context.getSource().getOnlinePlayerNames(), builder))
                        .executes(context -> {
                            final String name = StringArgumentType.getString(context, "player");
                            final UUID player = context.getSource().getServer().services().nameToIdCache().get(name)
                                    .map(NameAndId::id).orElseThrow(SGGateCommands.UNKNOWN_PLAYER::create);
                            return SGGateCommands.setOwner(context.getSource(), finder.find(context),
                                    Optional.of(player), name);
                        }));
    }

    private static int showOwner(final CommandSourceStack source, final Stargate gate) {
        source.sendSuccess(() -> gate.owner()
                .map(owner -> SGCommands.text("owner.show", "The gate at %s belongs to %s",
                        SGCommands.position(gate), SGGateCommands.name(source, owner)))
                .orElseGet(() -> SGCommands.text("owner.none", "The gate at %s has no owner, so operators manage it",
                        SGCommands.position(gate))), false);
        return gate.owner().isPresent() ? 1 : 0;
    }

    private static int setOwner(final CommandSourceStack source, final Stargate gate, final Optional<UUID> owner,
            final String name) {
        return SGCommands.report(source, gate.setOwner(owner, SGCommands.cause(source)), owner.isPresent()
                ? SGCommands.text("owner.set", "The gate at %s now belongs to %s", SGCommands.position(gate), name)
                : SGCommands.text("owner.cleared", "The gate at %s no longer has an owner",
                        SGCommands.position(gate)));
    }

    // A player's name when last seen, or their UUID if they never were
    private static String name(final CommandSourceStack source, final UUID player) {
        return source.getServer().services().nameToIdCache().get(player).map(NameAndId::name)
                .orElse(player.toString());
    }

    // rights <player> <use,iris,configure | none | reset>; rights default <...>. The rights are the rest of the line,
    // as a single word can't hold commas. Anyone may look; only the gate's owner or an operator changes them, which
    // the gate checks
    private static ArgumentBuilder<CommandSourceStack, ?> rights(final GateFinder finder) {
        return Commands.literal("rights")
                .executes(context -> SGGateCommands.showRights(context.getSource(), finder.find(context)))
                .then(Commands.literal("default")
                        .then(Commands.argument("rights", StringArgumentType.greedyString())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        new String[] {"use,iris,configure", "use,iris", "use", "none"}, builder))
                                .executes(context -> {
                                    final Stargate gate = finder.find(context);
                                    return SGCommands.report(context.getSource(),
                                            gate.set(GateProperties.DEFAULT_RIGHTS, SGGateCommands.parseRights(
                                                    StringArgumentType.getString(context, "rights")),
                                                    SGCommands.cause(context.getSource())),
                                            SGCommands.text("rights.default", "Set the gate's default rights"));
                                })))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                context.getSource().getOnlinePlayerNames(), builder))
                        .then(Commands.argument("rights", StringArgumentType.greedyString())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        new String[] {"use,iris,configure", "use,iris", "use", "none", "reset"},
                                        builder))
                                .executes(context -> SGGateCommands.setRights(context, finder.find(context)))));
    }

    private static int showRights(final CommandSourceStack source, final Stargate gate) {
        gate.owner().ifPresent(owner -> source.sendSuccess(() -> SGCommands.text("rights.owner",
                "Owner, with every right: %s", SGGateCommands.name(source, owner)), false));
        source.sendSuccess(() -> SGCommands.text("rights.defaults", "Everyone not listed: %s",
                SGGateCommands.rightsText(gate.get(GateProperties.DEFAULT_RIGHTS).orElse(Set.of()))), false);
        gate.get(GateProperties.PLAYER_RIGHTS).orElse(Map.of()).forEach((uuid, rights) -> {
            final String name = SGGateCommands.name(source, uuid);
            source.sendSuccess(() -> Component.literal("  " + name + ": " + SGGateCommands.rightsText(rights)),
                    false);
        });
        return 1;
    }

    private static int setRights(final CommandContext<CommandSourceStack> context, final Stargate gate)
            throws CommandSyntaxException {
        final String name = StringArgumentType.getString(context, "player");
        final UUID player = context.getSource().getServer().services().nameToIdCache().get(name)
                .map(NameAndId::id).orElseThrow(SGGateCommands.UNKNOWN_PLAYER::create);
        final String text = StringArgumentType.getString(context, "rights");
        final Map<UUID, Set<GateRight>> rights = new HashMap<>(gate.get(GateProperties.PLAYER_RIGHTS)
                .orElse(Map.of()));
        if (text.equals("reset")) {
            rights.remove(player);
        } else {
            rights.put(player, SGGateCommands.parseRights(text));
        }
        return SGCommands.report(context.getSource(), gate.set(GateProperties.PLAYER_RIGHTS, Map.copyOf(rights),
                SGCommands.cause(context.getSource())), SGCommands.text("rights.player", "Set %s's rights", name));
    }

    private static Set<GateRight> parseRights(final String text) {
        final Set<GateRight> rights = new HashSet<>();
        for (final String part : text.split(",")) {
            switch (part.strip().toLowerCase(Locale.ROOT)) {
                case "use" -> rights.add(GateRight.USE);
                case "iris" -> rights.add(GateRight.IRIS);
                case "configure" -> rights.add(GateRight.CONFIGURE);
                default -> {
                }
            }
        }
        return Set.copyOf(rights);
    }

    private static String rightsText(final Set<GateRight> rights) {
        if (rights.isEmpty()) {
            return "none";
        }
        return String.join(", ", rights.stream().map(right -> right.id().getPath()).sorted().toList());
    }

    // rule [set <incoming> <outgoing> <address> | remove <address>]
    private static ArgumentBuilder<CommandSourceStack, ?> rule(final GateFinder finder) {
        return Commands.literal("rule").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> {
                    final Stargate gate = finder.find(context);
                    final CommandSourceStack source = context.getSource();
                    source.sendSuccess(() -> SGCommands.text("rule.defaults", "Incoming %s, outgoing %s by default",
                            gate.get(GateProperties.ALLOW_INCOMING).orElse(true) ? "allowed" : "refused",
                            gate.get(GateProperties.ALLOW_OUTGOING).orElse(true) ? "allowed" : "refused"), false);
                    gate.get(GateProperties.ADDRESS_RULES).orElse(Map.of()).forEach((address, rule) ->
                            source.sendSuccess(() -> Component.literal("  " + address + ": incoming "
                                    + (rule.incoming() ? "allowed" : "refused") + ", outgoing "
                                    + (rule.outgoing() ? "allowed" : "refused")), false));
                    return 1;
                })
                .then(Commands.literal("set")
                        .then(Commands.argument("incoming", BoolArgumentType.bool())
                                .then(Commands.argument("outgoing", BoolArgumentType.bool())
                                        .then(Commands.argument("address", StringArgumentType.greedyString())
                                                .executes(context -> SGGateCommands.setRule(context,
                                                        finder.find(context), Optional.of(new AddressRule(
                                                                BoolArgumentType.getBool(context, "incoming"),
                                                                BoolArgumentType.getBool(context, "outgoing")))))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("address", StringArgumentType.greedyString())
                                .executes(context -> SGGateCommands.setRule(context, finder.find(context),
                                        Optional.empty()))));
    }

    private static int setRule(final CommandContext<CommandSourceStack> context, final Stargate gate,
            final Optional<AddressRule> rule) throws CommandSyntaxException {
        final Address address = Address.parse(StringArgumentType.getString(context, "address"))
                .orElseThrow(SGGateCommands.NOT_AN_ADDRESS::create);
        final Map<Address, AddressRule> rules = new HashMap<>(gate.get(GateProperties.ADDRESS_RULES)
                .orElse(Map.of()));
        rule.ifPresentOrElse(r -> rules.put(address, r), () -> rules.remove(address));
        return SGCommands.report(context.getSource(), gate.set(GateProperties.ADDRESS_RULES, Map.copyOf(rules),
                SGCommands.cause(context.getSource())), SGCommands.text("rule.set", "Set the rule for %s",
                address.toString()));
    }

    // place <type> [chevrons] [radius] [standing | face_up | face_down]: a whole gate where the caller stands, facing
    // them, or lying flat with its ring running away from them; a radius of 4 or more makes a supergate of that size
    static ArgumentBuilder<CommandSourceStack, ?> place() {
        final ArgumentBuilder<CommandSourceStack, ?> radius = Commands.argument("radius", IntegerArgumentType.integer(
                        GateShape.STANDARD.radius(), GateShape.MAX_SUPERGATE_RADIUS))
                .executes(context -> SGGateCommands.place(context, IntegerArgumentType.getInteger(context, "chevrons"),
                        IntegerArgumentType.getInteger(context, "radius"), GateOrientation.STANDING));
        for (final GateOrientation lying : GateOrientation.values()) {
            radius.then(Commands.literal(lying.getSerializedName())
                    .executes(context -> SGGateCommands.place(context,
                            IntegerArgumentType.getInteger(context, "chevrons"),
                            IntegerArgumentType.getInteger(context, "radius"), lying)));
        }
        return Commands.literal("place").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("type", IdentifierArgument.id())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                context.getSource().getServer().registryAccess()
                                        .lookupOrThrow(SGCraftRegistries.GATE_TYPE).keySet(), builder))
                        .executes(context -> SGGateCommands.place(context, 9, GateShape.STANDARD.radius(),
                                GateOrientation.STANDING))
                        .then(Commands.argument("chevrons", IntegerArgumentType.integer(1, 9))
                                .executes(context -> SGGateCommands.place(context,
                                        IntegerArgumentType.getInteger(context, "chevrons"),
                                        GateShape.STANDARD.radius(), GateOrientation.STANDING))
                                .then(radius)));
    }

    private static int place(final CommandContext<CommandSourceStack> context, final int chevrons,
            final int radius, final GateOrientation lying) throws CommandSyntaxException {
        final CommandSourceStack source = context.getSource();
        final Optional<Holder.Reference<GateType>> type = source.getServer().registryAccess()
                .lookupOrThrow(SGCraftRegistries.GATE_TYPE).get(IdentifierArgument.getId(context, "type"));
        if (type.isEmpty()) {
            source.sendFailure(SGCommands.text("unknown_type", "There is no gate type by that name"));
            return 0;
        }
        Optional<GateShape> shape = GateSize.of(radius).flatMap(GateShape::of);
        // A type built only as a supergate, as the Ori supergate, is placed at the spawners' size unless one is given
        if (shape.isPresent() && radius == GateShape.STANDARD.radius() && !type.get().value().supports(
                shape.get().size())) {
            shape = Optional.of(GateShape.supergate(GateSpawnerBlock.SUPERGATE_RADIUS));
        }
        if (shape.isPresent() && !type.get().value().supports(shape.get().size())) {
            source.sendFailure(SGCommands.text("place.unsupported", "%s gates can't be built at that size",
                    type.get().value().name()));
            return 0;
        }
        if (shape.isEmpty()) {
            source.sendFailure(SGCommands.text("place.bad_radius", "A gate's radius is %s, or from %s to %s for a "
                    + "supergate", GateShape.STANDARD.radius(), GateShape.MIN_SUPERGATE_RADIUS,
                    GateShape.MAX_SUPERGATE_RADIUS));
            return 0;
        }
        // In front of the caller, facing back at them; the way the command faces, so it works under execute too
        final Direction facing = Direction.fromYRot(source.getRotation().y).getOpposite();
        final GlobalPos position = GlobalPos.of(source.getLevel().dimension(),
                BlockPos.containing(source.getPosition()).relative(facing.getOpposite(), 3));
        final GatePlacement placement = GatePlacement.builder().position(position)
                .orientation(lying.frontAndTop(facing)).type(type.get()).chevrons(chevrons)
                .size(shape.get().size()).searchRadius(3).build();
        final PlacementResult result = SGCraftApi.get().stargates().place(placement, SGCommands.cause(source));
        if (result.status() == PlacementResult.Status.PLACED) {
            final Stargate gate = result.gate().orElseThrow();
            source.sendSuccess(() -> SGCommands.text("place.done", "Placed a %s gate at %s",
                    type.get().value().name(), SGCommands.position(gate)), true);
            return 1;
        }
        source.sendFailure(result.reason().orElseGet(() -> SGCommands.text("place.failed", "The gate couldn't be "
                + "placed: %s", result.status().name().toLowerCase(Locale.ROOT).replace('_', ' '))));
        return 0;
    }

    // book [add <name> <address> | remove <address>]: the caller's own address book
    static ArgumentBuilder<CommandSourceStack, ?> book() {
        return Commands.literal("book")
                .executes(context -> {
                    final AddressBook book = SGGateCommands.book(context.getSource());
                    if (book.isEmpty()) {
                        context.getSource().sendSuccess(() -> SGCommands.text("book.empty",
                                "Your address book is empty"), false);
                        return 0;
                    }
                    book.forEach(entry -> context.getSource().sendSuccess(() -> Component.literal("  ")
                            .append(entry.name()).append(" ")
                            .append(Component.literal(entry.address().toString()).withStyle(ChatFormatting.AQUA))
                            .append(" ").append(GlyphText.of(entry.address(), DialStyle.MILKY_WAY)), false));
                    return book.size();
                })
                .then(Commands.literal("add")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("address", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            // Kept in the form that names its world, so it dials from any world
                                            final Address address = SGServer.require().addressing().lasting(
                                                    Address.parse(StringArgumentType.getString(context, "address"))
                                                            .orElseThrow(SGGateCommands.NOT_AN_ADDRESS::create),
                                                    context.getSource().getLevel().dimension());
                                            return SGCommands.report(context.getSource(),
                                                    SGGateCommands.book(context.getSource()).add(address,
                                                            Component.literal(StringArgumentType.getString(context,
                                                                    "name")), SGCommands.cause(context.getSource())),
                                                    SGCommands.text("book.added", "Added %s to your book",
                                                            address.toString()));
                                        }))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("address", StringArgumentType.greedyString())
                                .executes(context -> {
                                    final Address address = Address.parse(StringArgumentType.getString(context,
                                            "address")).orElseThrow(SGGateCommands.NOT_AN_ADDRESS::create);
                                    return SGCommands.report(context.getSource(),
                                            SGGateCommands.book(context.getSource()).remove(address,
                                                    SGCommands.cause(context.getSource())),
                                            SGCommands.text("book.removed", "Removed %s from your book",
                                                    address.toString()));
                                })));
    }

    private static AddressBook book(final CommandSourceStack source) throws CommandSyntaxException {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            throw SGGateCommands.PLAYERS_ONLY.create();
        }
        return SGCraftApi.get().addressBooks().forPlayer(player.getUUID());
    }
}
