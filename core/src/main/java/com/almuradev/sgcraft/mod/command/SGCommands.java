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

import com.almuradev.sgcraft.api.ActionResult;
import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.SGCraftApi;
import com.almuradev.sgcraft.api.SGCraftRegistries;
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.address.Addressing;
import com.almuradev.sgcraft.api.address.WorldGroup;
import com.almuradev.sgcraft.api.gate.DialMode;
import com.almuradev.sgcraft.api.gate.DialResult;
import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.gate.DialStyle;
import com.almuradev.sgcraft.mod.gate.GateTargeting;
import com.almuradev.sgcraft.mod.gate.GlyphText;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
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
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The {@code /sgcraft} commands, which work gates without any screens, so a server's admins can run gates from the
 * console or chat. They go through the API like any add-on would.
 *
 * <p>{@code address} and {@code info} show the gate the caller is looking at, or with {@code --selected}, the gate
 * they selected; {@code selected} shows that too. {@code /sgcraft select} selects the gate the caller is looking at,
 * or, given a position, the gate in the address region holding it. Commands that act on a gate use the selected gate,
 * from anywhere, or with none selected, the gate the caller is looking at. A selection lasts until it is cleared or
 * the server stops. Admins can name any gate by a block of it with {@code gate <pos>}, and list every gate in a
 * world.</p>
 */
public final class SGCommands {

    // How far a gate can be looked at to select it, and how finely the view is followed
    private static final int LOOK_BLOCKS = 64;
    private static final double LOOK_STEP = 0.25;
    // What goes on the end of an address to dial it quickly, locking every chevron at once
    private static final String QUICK_FLAG = "--quick";
    private static final String QUICK_SHORT = "-q";

    private static final SimpleCommandExceptionType NOTHING_SELECTED = new SimpleCommandExceptionType(
            SGCommands.text("nothing_selected", "You have no gate selected. Look at one and use /sgcraft select"));
    private static final SimpleCommandExceptionType NO_GATE_THERE = new SimpleCommandExceptionType(
            SGCommands.text("no_gate_there", "There is no gate there"));
    private static final SimpleCommandExceptionType NOT_AN_ADDRESS = new SimpleCommandExceptionType(
            SGCommands.text("not_an_address", "That isn't an address. Use letters, digits and @, such as AB12-CDE"));
    private static final SimpleCommandExceptionType LOOK_OR_POSITION = new SimpleCommandExceptionType(
            SGCommands.text("look_or_position", "Look at a gate to select it, or give a position"));
    private static final SimpleCommandExceptionType NOT_LOOKING = new SimpleCommandExceptionType(
            SGCommands.text("not_looking", "You aren't looking at a gate within %s blocks", SGCommands.LOOK_BLOCKS));
    private static final DynamicCommandExceptionType NO_GATE_IN_CELL = new DynamicCommandExceptionType(
            cell -> SGCommands.text("no_gate_in_cell", "There is no gate in the address cell that holds that "
                    + "position. Cells are %s chunks across", cell));
    private static final SimpleCommandExceptionType SELECTION_GONE = new SimpleCommandExceptionType(
            SGCommands.text("selection_gone", "Your selected gate is gone, so it's no longer selected"));
    private static final SimpleCommandExceptionType UNKNOWN_TYPE = new SimpleCommandExceptionType(
            SGCommands.text("unknown_type", "There is no gate type by that name"));

    private SGCommands() {
    }

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        final LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("sgcraft");
        root
                .then(Commands.literal("address")
                        .executes(context -> SGCommands.address(context.getSource(),
                                SGCommands.lookedAt(context.getSource())))
                        .then(Commands.literal("--selected")
                                .executes(context -> SGCommands.address(context.getSource(),
                                        SGCommands.selected(context.getSource())))))
                .then(Commands.literal("info")
                        .executes(context -> SGCommands.info(context.getSource(),
                                SGCommands.lookedAt(context.getSource())))
                        .then(Commands.literal("--selected")
                                .executes(context -> SGCommands.info(context.getSource(),
                                        SGCommands.selected(context.getSource())))))
                .then(Commands.literal("selected")
                        .executes(context -> SGCommands.info(context.getSource(),
                                SGCommands.selected(context.getSource()))))
                .then(SGCommands.dial(SGCommands::chosen))
                .then(Commands.literal("disconnect")
                        .executes(context -> SGCommands.disconnect(context.getSource(), SGCommands.chosen(context))))
                .then(Commands.literal("select")
                        .executes(context -> SGCommands.select(context.getSource(),
                                SGCommands.lookedAt(context.getSource())))
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(context -> SGCommands.select(context.getSource(),
                                        SGCommands.inCell(context)))))
                .then(Commands.literal("deselect")
                        .executes(context -> SGCommands.deselect(context.getSource())))
                .then(Commands.literal("list")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> SGCommands.list(context.getSource(),
                                context.getSource().getLevel().dimension()))
                        .then(Commands.argument("world", DimensionArgument.dimension())
                                .executes(context -> SGCommands.list(context.getSource(),
                                        DimensionArgument.getDimension(context, "world").dimension()))))
                .then(Commands.literal("chevrons")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(SGCommands.chevrons(SGCommands::chosen)))
                .then(Commands.literal("type")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(SGCommands.type(SGCommands::chosen)))
                .then(SGGateCommands.place())
                .then(SGGateCommands.book())
                .then(SGWorldgenCommands.worldgen())
                .then(SGPowerCommands.power())
                .then(SGViewCommands.view())
                .then(SGCostCommands.costs())
                .then(SGSettingsCommands.settings())
                .then(SGSettingsCommands.gateDefaults())
                .then(SGConfigCommands.config())
                .then(SGOptionCommands.chunkLoadingRange())
                .then(SGOptionCommands.logEvents())
                .then(SGOreCommands.ore())
                .then(SGDefaultAddressCommands.defaultAddresses())
                .then(SGAddressCommands.addresses())
                .then(SGMalpCommands.malp())
                .then(SGMalpCommands.battery())
                .then(SGShieldCommands.shields())
                .then(SGExplorerCommands.explorer())
                .then(SGZpmCommands.zpm())
                .then(SGComputerCommands.computers());
        final RequiredArgumentBuilder<CommandSourceStack, Coordinates> gate =
                Commands.argument("pos", BlockPosArgument.blockPos());
        gate.then(Commands.literal("address")
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
                .then(Commands.literal("type").then(SGCommands.type(SGCommands::at)));
        SGGateCommands.gateCommands(root, SGCommands::chosen);
        SGGateCommands.gateCommands(gate, SGCommands::at);
        root.then(Commands.literal("gate").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(gate));
        dispatcher.register(root);
    }

    @FunctionalInterface
    interface GateFinder {
        Stargate find(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;
    }

    // dial <address> [--quick | -q]; the address is free text, as @ isn't allowed in a plain word, so the flag is
    // read from its end
    private static ArgumentBuilder<CommandSourceStack, ?> dial(final GateFinder finder) {
        return Commands.literal("dial")
                .then(Commands.argument("address", StringArgumentType.greedyString())
                        .suggests((context, builder) -> {
                            final String typed = builder.getRemaining();
                            if (!typed.isBlank() && !SGCommands.quick(typed).quick()) {
                                builder.suggest(typed.stripTrailing() + " " + SGCommands.QUICK_FLAG);
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            final DialText text = SGCommands.quick(StringArgumentType.getString(context, "address"));
                            return SGCommands.dial(context.getSource(), finder.find(context), text.address(),
                                    text.quick() ? DialMode.QUICK : DialMode.STANDARD);
                        }));
    }

    private record DialText(String address, boolean quick) {
    }

    // An address with --quick or -q on its end, which locks every chevron at once
    private static DialText quick(final String text) {
        final String trimmed = text.strip();
        for (final String flag : new String[] {SGCommands.QUICK_FLAG, SGCommands.QUICK_SHORT}) {
            if (trimmed.endsWith(" " + flag) || trimmed.equals(flag)) {
                return new DialText(trimmed.substring(0, trimmed.length() - flag.length()).strip(), true);
            }
        }
        return new DialText(trimmed, false);
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

    // For commands that act on a gate: the gate the caller selected, wherever they are; or, with none selected, the
    // gate they are looking at
    static Stargate chosen(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final CommandSourceStack source = context.getSource();
        if (SGServer.require().selection(SGCommands.caller(source)).isEmpty()) {
            return SGCommands.lookedAt(source);
        }
        return SGCommands.selected(source);
    }

    // The gate the caller selected
    private static Stargate selected(final CommandSourceStack source) throws CommandSyntaxException {
        final Optional<GlobalPos> selected = SGServer.require().selection(SGCommands.caller(source));
        if (selected.isEmpty()) {
            throw SGCommands.NOTHING_SELECTED.create();
        }
        final Optional<Stargate> gate = SGCraftApi.get().stargates().at(selected.get());
        if (gate.isEmpty()) {
            SGServer.require().deselect(SGCommands.caller(source));
            throw SGCommands.SELECTION_GONE.create();
        }
        return gate.get();
    }

    private static int select(final CommandSourceStack source, final Stargate gate) {
        SGServer.require().select(SGCommands.caller(source), gate.position());
        source.sendSuccess(() -> SGCommands.text("select.done",
                "Selected the %s gate at %s. Commands use it from anywhere until you %s",
                gate.type().value().name(), SGCommands.position(gate), SGCommands.deselectLink()), false);
        return 1;
    }

    private static int deselect(final CommandSourceStack source) {
        if (SGServer.require().deselect(SGCommands.caller(source)).isEmpty()) {
            source.sendFailure(SGCommands.text("deselect.none", "You have no gate selected"));
            return 0;
        }
        source.sendSuccess(() -> SGCommands.text("deselect.done",
                "Deselected the gate. Commands use the gate you look at again"), false);
        return 1;
    }

    // Who a selection belongs to: an entity running the command, or the server's console
    private static UUID caller(final CommandSourceStack source) {
        return source.getEntity() != null ? source.getEntity().getUUID() : SGServer.CONSOLE;
    }

    private static Component deselectLink() {
        return SGCommands.text("deselect_link", "/sgcraft deselect").withStyle(style -> style
                .withColor(ChatFormatting.GOLD)
                .withClickEvent(new ClickEvent.RunCommand("/sgcraft deselect")));
    }

    private static Component selectLink(final Stargate gate) {
        final BlockPos pos = gate.position().pos();
        final String command = "/sgcraft select " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
        return SGCommands.text("select_link", "[Select]").withStyle(style -> style
                .withColor(ChatFormatting.GOLD)
                .withClickEvent(new ClickEvent.RunCommand(command))
                .withHoverEvent(new HoverEvent.ShowText(SGCommands.text("hover.select",
                        "Click to select this gate, so commands use it from anywhere"))));
    }

    // The first gate along the caller's view, through its ring or its opening, before anything solid
    private static Stargate lookedAt(final CommandSourceStack source) throws CommandSyntaxException {
        final Entity entity = source.getEntity();
        if (entity == null) {
            throw SGCommands.LOOK_OR_POSITION.create();
        }
        final Level level = source.getLevel();
        final Vec3 eye = entity.getEyePosition();
        final Vec3 view = entity.getViewVector(1.0F);
        final Vec3 far = eye.add(view.scale(SGCommands.LOOK_BLOCKS));
        final HitResult hit = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                entity));
        // Past the block it hits, so a gate's own ring counts
        final double reach = hit.getType() == HitResult.Type.MISS ? SGCommands.LOOK_BLOCKS
                : hit.getLocation().distanceTo(eye) + SGCommands.LOOK_STEP;
        final Collection<Stargate> gates = SGCraftApi.get().stargates().in(level.dimension());
        for (double d = 0; d <= reach; d += SGCommands.LOOK_STEP) {
            final Vec3 point = eye.add(view.scale(d));
            for (final Stargate gate : gates) {
                if (SGCommands.covers(gate, point)) {
                    return gate;
                }
            }
        }
        throw SGCommands.NOT_LOOKING.create();
    }

    // Whether a point is in the gate's ring or its opening, whatever the gate's size
    private static boolean covers(final Stargate gate, final Vec3 point) {
        return GateTargeting.covers(gate, point);
    }

    // The gate whose address cell holds the given position, in the caller's world: the regions addresses name
    private static Stargate inCell(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final ResourceKey<Level> world = context.getSource().getLevel().dimension();
        final ChunkPos chunk = ChunkPos.containing(BlockPosArgument.getBlockPos(context, "pos"));
        final int cell = SGCraftApi.get().addressing().partition(world).cellChunks();
        final int x = Math.floorDiv(chunk.x(), cell);
        final int z = Math.floorDiv(chunk.z(), cell);
        for (final Stargate gate : SGCraftApi.get().stargates().in(world)) {
            final ChunkPos at = ChunkPos.containing(gate.position().pos());
            if (Math.floorDiv(at.x(), cell) == x && Math.floorDiv(at.z(), cell) == z) {
                return gate;
            }
        }
        throw SGCommands.NO_GATE_IN_CELL.create(cell);
    }

    static Stargate at(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
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
                        SGCommands.copyable(address, gate), SGCommands.dialLink(address)), false));
        // From any other world of its group, its address is the same, so the first world that reaches it will do;
        // the same goes for the worlds of other groups
        final WorldGroup group = addressing.groupOf(world);
        final boolean grouped = addressing.groups().size() > 1;
        group.worlds().stream()
                .filter(other -> !other.equals(world))
                .map(other -> addressing.addressOf(gate.position(), other))
                .flatMap(Optional::stream)
                .findFirst()
                .ifPresent(address -> source.sendSuccess(() -> grouped
                        ? SGCommands.text("address.group_world", "  From other worlds of group %s: %s %s",
                                group.name(), SGCommands.copyable(address, gate), SGCommands.dialLink(address))
                        : SGCommands.text("address.other_world", "  From other worlds: %s %s",
                                SGCommands.copyable(address, gate), SGCommands.dialLink(address)), false));
        addressing.groups().stream()
                .filter(other -> !other.equals(group))
                .flatMap(other -> other.worlds().stream())
                .map(other -> addressing.addressOf(gate.position(), other))
                .flatMap(Optional::stream)
                .findFirst()
                .ifPresent(address -> source.sendSuccess(() -> SGCommands.text("address.other_group",
                        "  From other groups: %s %s", SGCommands.copyable(address, gate),
                        SGCommands.dialLink(address)), false));
        return 1;
    }

    private static int info(final CommandSourceStack source, final Stargate gate) {
        final boolean selected = SGServer.require().selection(SGCommands.caller(source))
                .filter(gate.position()::equals).isPresent();
        source.sendSuccess(() -> SGCommands.text(selected ? "info.selected_gate" : "info.gate",
                selected ? "%s gate at %s (selected)" : "%s gate at %s", gate.type().value().name(),
                SGCommands.position(gate)), false);
        source.sendSuccess(() -> SGCommands.text("info.state", "  %s, %s chevrons",
                gate.state().name().toLowerCase(Locale.ROOT), gate.chevrons()), false);
        gate.owner().ifPresent(owner -> source.sendSuccess(() -> SGCommands.text("info.owner", "  Belongs to %s",
                source.getServer().services().nameToIdCache().get(owner).map(NameAndId::name)
                        .orElse(owner.toString())), false));
        gate.connection().ifPresent(connection -> {
            final Stargate other = connection.other(gate).orElseThrow();
            final boolean dialing = connection.dialer().equals(gate);
            source.sendSuccess(() -> SGCommands.text(dialing ? "info.dialed" : "info.dialed_by",
                    dialing ? "  Dialed %s, the gate at %s" : "  Dialed by the gate at %2$s, with %1$s",
                    SGCommands.copyable(connection.address(), gate), SGCommands.position(other)), false);
        });
        return 1;
    }

    private static int dial(final CommandSourceStack source, final Stargate gate, final String text,
            final DialMode mode) throws CommandSyntaxException {
        final Address address = Address.parse(text).orElseThrow(SGCommands.NOT_AN_ADDRESS::create);
        final Cause cause = SGCommands.cause(source);
        source.sendSuccess(() -> SGCommands.text("dial.start", "Dialing %s from the gate at %s",
                SGCommands.copyable(address, gate), SGCommands.position(gate)), false);
        gate.dial(address, mode, cause).whenComplete((result, error) -> {
            if (error != null) {
                source.sendFailure(SGCommands.text("dial.error", "Dialing failed: %s", String.valueOf(error)));
            } else if (result == DialResult.CONNECTED) {
                source.sendSuccess(() -> SGCommands.text("dial.connected", "The wormhole to %s is open",
                        SGCommands.copyable(address, gate)), false);
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
                            line.append(" ").append(SGCommands.copyable(address, gate)));
                    line.append(" ").append(SGCommands.selectLink(gate));
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
        // Some types come only in some sizes, as the Ori supergate only as a supergate
        if (!type.value().supports(gate.size())) {
            source.sendFailure(SGCommands.text("place.unsupported", "%s gates can't be built at that size",
                    type.value().name()));
            return 0;
        }
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

    static int report(final CommandSourceStack source, final ActionResult result, final Component done) {
        switch (result) {
            case DONE -> {
                source.sendSuccess(() -> done, true);
                return 1;
            }
            case NO_CHANGE -> source.sendFailure(SGCommands.text("result.no_change", "Nothing changed"));
            case DENIED -> source.sendFailure(SGCommands.text("result.denied", "You aren't allowed to do that"));
            case CANCELED -> source.sendFailure(SGCommands.text("result.canceled", "Something stopped it"));
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
            case INCOMPATIBLE -> SGCommands.text("dial.incompatible",
                    "This gate can't connect to that type of gate");
            case NOT_ENOUGH_ENERGY -> SGCommands.text("dial.not_enough_energy", "This gate doesn't have the energy");
            case DENIED -> SGCommands.text("dial.denied", "You aren't allowed to dial that");
            case CANCELED -> SGCommands.text("dial.canceled", "Something stopped the dial");
            case INTERRUPTED -> SGCommands.text("dial.interrupted", "The dial was interrupted");
            case GATE_GONE -> SGCommands.text("dial.gate_gone", "A gate was broken while dialing");
            default -> SGCommands.text("dial.failed", "The dial failed: %s", result.name());
        };
    }

    static Cause cause(final CommandSourceStack source) {
        return source.getEntity() != null
                ? Cause.of(source.getEntity(), source.getServer())
                : Cause.of(source.getServer());
    }

    static Component position(final Stargate gate) {
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

    // The address, which copies itself when clicked, followed by its symbols as the Milky Way and Pegasus DHDs
    // show them, and as a Universe gate does when the gate is one
    private static Component copyable(final Address address, final Stargate gate) {
        final MutableComponent text = Component.empty().append(SGCommands.copyable(address))
                .append(" ").append(SGCommands.glyphs(address, DialStyle.MILKY_WAY))
                .append("  ").append(SGCommands.glyphs(address, DialStyle.PEGASUS));
        if (gate.type().unwrapKey().map(DialStyle::of).orElse(DialStyle.MILKY_WAY) == DialStyle.UNIVERSE) {
            text.append("  ").append(SGCommands.glyphs(address, DialStyle.UNIVERSE));
        }
        return text;
    }

    // The address's symbols in one style's glyphs, which name the DHD they are on when hovered
    private static Component glyphs(final Address address, final DialStyle style) {
        final Component hover = SGCommands.text("hover.glyphs." + style.name().toLowerCase(Locale.ROOT),
                switch (style) {
                    case PEGASUS -> "%s on a Pegasus DHD";
                    case UNIVERSE -> "%s on a Universe gate";
                    default -> "%s on a Milky Way DHD";
                }, address.toString());
        return GlyphText.of(address, style).withStyle(s -> s.withHoverEvent(new HoverEvent.ShowText(hover)));
    }

    // A link that puts a dial command for the address in the chat box
    private static Component dialLink(final Address address) {
        return SGCommands.text("dial_link", "[Dial]").withStyle(style -> style
                .withColor(ChatFormatting.GOLD)
                .withClickEvent(new ClickEvent.SuggestCommand("/sgcraft dial " + address))
                .withHoverEvent(new HoverEvent.ShowText(SGCommands.text("hover.dial",
                        "Click to dial it from your selected gate, or the one you look at"))));
    }

    static MutableComponent text(final String key, final String fallback, final Object... args) {
        return Component.translatableWithFallback("command.sgcraft." + key, fallback, args);
    }
}
