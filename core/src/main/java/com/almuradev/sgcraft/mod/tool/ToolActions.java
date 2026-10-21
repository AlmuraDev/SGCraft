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

package com.almuradev.sgcraft.mod.tool;

import com.almuradev.sgcraft.api.ActionResult;
import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.SGCraftApi;
import com.almuradev.sgcraft.api.SGCraftRegistries;
import com.almuradev.sgcraft.api.access.GateRight;
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.addressbook.AddressBook;
import com.almuradev.sgcraft.api.gate.DialMode;
import com.almuradev.sgcraft.api.gate.DialResult;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.GateProperty;
import com.almuradev.sgcraft.api.gate.GateState;
import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.api.gate.GateTypes;
import com.almuradev.sgcraft.api.gate.Iris;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.SGConfig;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.access.SGGateAccess;
import com.almuradev.sgcraft.mod.addressbook.DefaultAddresses;
import com.almuradev.sgcraft.mod.addressbook.ItemAddressBook;
import com.almuradev.sgcraft.mod.addressbook.ListAddressBook;
import com.almuradev.sgcraft.mod.block.entity.PowerUnitBlockEntity;
import com.almuradev.sgcraft.mod.energy.DialCosts;
import com.almuradev.sgcraft.mod.gate.DialStyle;
import com.almuradev.sgcraft.mod.gate.GateOrientation;
import com.almuradev.sgcraft.mod.gate.GateTargeting;
import com.almuradev.sgcraft.mod.gate.SGIris;
import com.almuradev.sgcraft.mod.gate.SGStargate;
import com.almuradev.sgcraft.mod.item.IrisUpgradeItem;
import com.almuradev.sgcraft.mod.item.SGDataComponents;
import com.almuradev.sgcraft.mod.item.SGItems;
import com.almuradev.sgcraft.mod.network.SGNetwork;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.BookEntry;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.Change;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.ConfiguratorApply;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.ConfiguratorState;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.GateInfo;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.GdoAction;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.GdoState;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.Named;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.PddAction;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.PddState;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.Property;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.TabletAction;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.TabletGateInfo;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.TabletState;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.Prediction;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What the server does for the handheld tools: the PDD, which dials from a player's address book; the GDO, which
 * works the irises at both ends of a wormhole; and the configurator, which sets everything about a gate. Every action
 * goes through the API, so the gate's access rules and events apply, and every action answers with the tool's state.
 */
public final class ToolActions {

    // How much further than the tool's range a player may wander with its screen open
    private static final int SLACK = 4;
    private static final int MAX_NAME = 48;
    // Players with a name lookup under way for the configurator, so each has one at a time
    private static final Set<UUID> LOOKING_UP = ConcurrentHashMap.newKeySet();

    private ToolActions() {
    }

    static int range() {
        return SGServer.current().map(server -> server.config().toolRange()).orElse(6);
    }

    public static Cause cause(final ServerPlayer player) {
        return Cause.of(player);
    }

    /** @return the gate as a tool shows it to the player, its address as dialed from the player's world */
    public static GateInfo info(final ServerPlayer player, final Stargate gate) {
        final boolean visible = gate.get(GateProperties.ADDRESS_VISIBLE).orElse(true);
        final String address = visible ? SGCraftApi.get().addressing()
                .addressOf(gate.position(), player.level().dimension()).map(Address::toString).orElse("") : "";
        final String type = gate.type().unwrapKey().map(key -> key.identifier().toString()).orElse("");
        final int iris = gate.iris().map(i -> i.state().ordinal()).orElse(GateInfo.NO_IRIS);
        final boolean shield = gate.iris().map(i -> SGIris.isShield(i.type())).orElse(false);
        return new GateInfo(gate.position().pos(), address, type, gate.state().name().toLowerCase(Locale.ROOT), iris,
                shield);
    }

    // The gate at a position the player's screen names, if it is still there and the player is still near it
    private static Optional<Stargate> near(final ServerPlayer player, final net.minecraft.core.BlockPos pos) {
        if (!pos.closerToCenterThan(player.position(), ToolActions.range() + 6 + ToolActions.SLACK)) {
            return Optional.empty();
        }
        return SGServer.current().flatMap(server -> server.stargates().gate(GlobalPos.of(player.level().dimension(),
                pos))).map(Stargate.class::cast);
    }

    private static void tell(final ServerPlayer player, final String key, final String fallback) {
        player.sendOverlayMessage(Component.translatableWithFallback(key, fallback).withStyle(ChatFormatting.RED));
    }

    /** @return what to tell a player about an action's result; empty when it worked */
    static String explain(final ActionResult result) {
        return switch (result) {
            case DONE -> "";
            case NO_CHANGE -> "Nothing changed";
            case DENIED -> "You aren't allowed to do that";
            case CANCELED -> "Something stopped it";
            default -> "That isn't possible now";
        };
    }

    // A change to a book's entry, which a locked entry refuses, as a full book refuses a new one
    private static String explain(final AddressBook book, final Address address, final ActionResult result) {
        if (result == ActionResult.DENIED && book instanceof final ItemAddressBook item && item.isLocked(address)) {
            return "That address came with the server's defaults and is locked";
        }
        if (result == ActionResult.NOT_POSSIBLE && book.find(address).isEmpty()
                && book.size() >= ListAddressBook.MAX_ENTRIES) {
            return "The book is full, at " + ListAddressBook.MAX_ENTRIES + " addresses";
        }
        return ToolActions.explain(result);
    }

    static String explain(final DialResult result) {
        return switch (result) {
            case CONNECTED -> "";
            case INVALID_ADDRESS -> "That isn't a whole address";
            case UNREACHABLE -> "No address reaches that gate from here";
            case NOT_ENOUGH_CHEVRONS -> "This gate doesn't have enough chevrons for that address";
            case NO_GATE -> "There is no gate at that address";
            case SAME_GATE -> "That is this gate's own address";
            case BUSY -> "This gate is busy";
            case TARGET_BUSY -> "The gate at that address is busy";
            case INCOMPATIBLE -> "This gate can't connect to that type of gate";
            case NOT_ENOUGH_ENERGY -> "This gate doesn't have the energy";
            case DENIED -> "You aren't allowed to dial that";
            case CANCELED -> "Something stopped the dial";
            case INTERRUPTED -> "The dial was interrupted";
            default -> "The dial failed";
        };
    }

    // The PDD

    /** A player used a PDD: its screen opens, with the gate near them, if any, and the addresses the PDD holds. */
    public static void openPdd(final ServerPlayer player) {
        ToolActions.fillDefaults(player, SGItems.PDD);
        ToolActions.sendPdd(player, true, "");
    }

    private static void sendPdd(final ServerPlayer player, final boolean open, final String message) {
        final Optional<Stargate> gate = GateTargeting.find(player, ToolActions.range());
        final Optional<AddressBook> book = ToolActions.pddBook(player);
        // Each address as the gate it would be dialed from sees it, or as the holder's world does
        final ResourceKey<Level> from = gate.map(g -> g.position().dimension()).orElse(player.level().dimension());
        final Optional<GlobalPos> dialer = gate.map(Stargate::position);
        final List<BookEntry> entries = book.map(b -> ToolActions.entries(b, from, dialer)).orElse(List.of());
        SGNetwork.send(player, new PddState(open, gate.map(g -> ToolActions.info(player, g)), entries,
                book.isPresent(), book.isPresent() || !message.isEmpty() ? message : "You aren't holding a PDD"));
    }

    // The book on the PDD the player holds, main hand first; it travels with the PDD, as in the original
    private static Optional<AddressBook> pddBook(final ServerPlayer player) {
        for (final InteractionHand hand : InteractionHand.values()) {
            final ItemStack stack = player.getItemInHand(hand);
            if (stack.is(SGItems.PDD)) {
                return Optional.of(ItemAddressBook.of(stack));
            }
        }
        return Optional.empty();
    }

    public static void pdd(final ServerPlayer player, final PddAction action) {
        final Optional<AddressBook> held = ToolActions.pddBook(player);
        if (held.isEmpty()) {
            ToolActions.sendPdd(player, false, "You aren't holding a PDD");
            return;
        }
        final AddressBook book = held.get();
        final Optional<Address> address = Address.parse(action.address());
        final String name = action.name().strip();
        String message = "";
        switch (action.kind()) {
            case REFRESH -> {
            }
            case DIAL -> {
                final Optional<Stargate> gate = GateTargeting.find(player, ToolActions.range());
                if (gate.isEmpty()) {
                    message = "There is no gate near you";
                } else if (address.isEmpty()) {
                    message = "That isn't an address";
                } else {
                    gate.get().dial(ToolActions.toDial(book, address.get(), gate.get()),
                            gate.get().get(GateProperties.CHEVRONS_LOCK_ON_DIAL)
                            .orElse(false) ? DialMode.QUICK : DialMode.STANDARD, ToolActions.cause(player))
                            .thenAccept(result -> {
                        if (result != DialResult.CONNECTED && player.connection != null) {
                            ToolActions.sendPdd(player, false, ToolActions.explain(result));
                        }
                    });
                }
            }
            case DISCONNECT -> message = GateTargeting.find(player, ToolActions.range())
                    .map(gate -> ToolActions.explain(gate.disconnect(ToolActions.cause(player))))
                    .orElse("There is no gate near you");
            case ADD -> {
                if (address.isEmpty()) {
                    message = "That isn't an address";
                } else {
                    // Kept in the form that names its world, so it dials from any world
                    final Address kept = SGServer.require().addressing().lasting(address.get(),
                            player.level().dimension());
                    message = ToolActions.explain(book, kept, book.add(kept,
                            Component.literal(ToolActions.name(name, kept)), ToolActions.cause(player)));
                    if (message.equals("Nothing changed")) {
                        message = "This PDD already has that address";
                    }
                }
            }
            case RENAME -> message = address.map(a -> ToolActions.explain(book, a, book.rename(a,
                    Component.literal(ToolActions.name(name, a)), ToolActions.cause(player))))
                    .orElse("That isn't an address");
            case REMOVE -> message = address.map(a -> ToolActions.explain(book, a,
                    book.remove(a, ToolActions.cause(player)))).orElse("That isn't an address");
            case MOVE -> message = address.map(a -> ToolActions.explain(book.move(a, action.index(),
                    ToolActions.cause(player)))).orElse("That isn't an address");
            default -> {
            }
        }
        ToolActions.sendPdd(player, false, message);
    }

    // A book's entries, each with what the gate its address reaches from a world is doing, and whether that is the
    // gate dialing, matched by place so an address in any of its forms counts
    private static List<BookEntry> entries(final AddressBook book, final ResourceKey<Level> from,
            final Optional<GlobalPos> dialer) {
        final List<BookEntry> entries = new ArrayList<>();
        book.forEach(entry -> {
            final Optional<Stargate> target = SGCraftApi.get().stargates().byAddress(entry.address(), from);
            final boolean own = dialer.isPresent() && target.map(Stargate::position).equals(dialer);
            entries.add(new BookEntry(entry.name().getString(), entry.address().toString(),
                    ToolActions.status(target), own));
        });
        return entries;
    }

    /**
     * @return what the gate an address reaches from a world is doing, as the tools' screens show it:
     *     {@link BookEntry#NO_GATE}, or its state's name in lower case
     */
    // A book keeps an address in the form that names its world; from a gate in that world it is dialed as the
    // shortest address that reaches the same place, as a gate with fewer chevrons can't dial the longer one. An
    // address typed in is dialed as it is
    private static Address toDial(final AddressBook book, final Address address, final Stargate gate) {
        return book.find(address).isPresent()
                ? SGServer.require().addressing().shortest(address, gate.position().dimension()) : address;
    }

    public static String status(final Address address, final ResourceKey<Level> from) {
        return ToolActions.status(SGCraftApi.get().stargates().byAddress(address, from));
    }

    // What a gate is doing, for the tools' screens, or that there is none
    private static String status(final Optional<Stargate> gate) {
        return gate.filter(Stargate::exists).map(g -> g.state().name().toLowerCase(Locale.ROOT))
                .orElse(BookEntry.NO_GATE);
    }

    private static String name(final String name, final Address address) {
        final String trimmed = name.length() > ToolActions.MAX_NAME ? name.substring(0, ToolActions.MAX_NAME) : name;
        return trimmed.isEmpty() ? address.toString() : trimmed;
    }

    // The gate control tablet

    /** A player used a gate control tablet: its screen opens, on the first gate of its list. */
    public static void openTablet(final ServerPlayer player) {
        ToolActions.fillDefaults(player, SGItems.GATE_CONTROL_TABLET);
        ToolActions.sendTablet(player, true, 0, "");
    }

    // A PDD or tablet used for the first time takes the server's default addresses
    private static void fillDefaults(final ServerPlayer player, final Item item) {
        SGServer.current().ifPresent(server -> {
            for (final InteractionHand hand : InteractionHand.values()) {
                if (player.getItemInHand(hand).is(item)) {
                    DefaultAddresses.fill(player.getItemInHand(hand), server.defaultAddresses());
                    return;
                }
            }
        });
    }

    private static void sendTablet(final ServerPlayer player, final boolean open, final int chosen,
            final String message) {
        final Optional<ItemStack> tablet = ToolActions.tablet(player);
        final List<TabletGate> listed = tablet.map(ToolActions::tabletGates).orElse(List.of());
        final List<TabletGateInfo> gates = new ArrayList<>();
        for (final TabletGate listedGate : listed) {
            gates.add(ToolActions.tabletInfo(player, listedGate));
        }
        final int selected = listed.isEmpty() ? -1 : Math.max(0, Math.min(chosen, listed.size() - 1));
        // The addresses as the chosen gate sees them, or as the holder's world does
        final ResourceKey<Level> from = selected < 0 ? player.level().dimension()
                : listed.get(selected).position().dimension();
        final Optional<GlobalPos> dialer = selected < 0 ? Optional.empty()
                : Optional.of(listed.get(selected).position());
        final List<BookEntry> entries = tablet.map(stack -> ToolActions.entries(ItemAddressBook.of(stack), from,
                dialer)).orElse(List.of());
        final Optional<GateInfo> near = GateTargeting.find(player, ToolActions.range())
                .map(gate -> ToolActions.info(player, gate));
        SGNetwork.send(player, new TabletState(open, gates, selected, entries, near, tablet.isPresent(),
                tablet.isPresent() || !message.isEmpty() ? message : "You aren't holding a gate control tablet"));
    }

    private static TabletGateInfo tabletInfo(final ServerPlayer player, final TabletGate listed) {
        final Optional<Stargate> gate = SGCraftApi.get().stargates().at(listed.position()).filter(Stargate::exists);
        final String address = gate.filter(g -> g.get(GateProperties.ADDRESS_VISIBLE).orElse(true))
                .flatMap(g -> SGCraftApi.get().addressing().addressOf(g.position(), player.level().dimension()))
                .map(Address::toString).orElse("");
        final String type = gate.flatMap(g -> g.type().unwrapKey()).map(key -> key.identifier().toString())
                .orElse("");
        final int iris = gate.flatMap(Stargate::iris).map(i -> i.state().ordinal()).orElse(GateInfo.NO_IRIS);
        final boolean shield = gate.flatMap(Stargate::iris).map(i -> SGIris.isShield(i.type())).orElse(false);
        return new TabletGateInfo(listed.name(), listed.position().dimension().identifier().toString(),
                listed.position().pos(), address, type, ToolActions.status(gate), iris, shield);
    }

    private static boolean holding(final ServerPlayer player, final Item item) {
        return player.getMainHandItem().is(item) || player.getOffhandItem().is(item);
    }

    // The tablet the player holds, main hand first
    private static Optional<ItemStack> tablet(final ServerPlayer player) {
        for (final InteractionHand hand : InteractionHand.values()) {
            final ItemStack stack = player.getItemInHand(hand);
            if (stack.is(SGItems.GATE_CONTROL_TABLET)) {
                return Optional.of(stack);
            }
        }
        return Optional.empty();
    }

    private static List<TabletGate> tabletGates(final ItemStack stack) {
        return stack.getOrDefault(SGDataComponents.TABLET_GATES, List.of());
    }

    private static void setTabletGates(final ItemStack stack, final List<TabletGate> gates) {
        if (gates.isEmpty()) {
            stack.remove(SGDataComponents.TABLET_GATES);
        } else {
            stack.set(SGDataComponents.TABLET_GATES, List.copyOf(gates));
        }
    }

    public static void tablet(final ServerPlayer player, final TabletAction action) {
        final Optional<ItemStack> held = ToolActions.tablet(player);
        if (held.isEmpty()) {
            ToolActions.sendTablet(player, false, action.gate(), "You aren't holding a gate control tablet");
            return;
        }
        final ItemStack stack = held.get();
        final AddressBook book = ItemAddressBook.of(stack);
        final List<TabletGate> gates = new ArrayList<>(ToolActions.tabletGates(stack));
        final Optional<TabletGate> chosen = action.gate() >= 0 && action.gate() < gates.size()
                ? Optional.of(gates.get(action.gate())) : Optional.empty();
        final Optional<Stargate> chosenGate = chosen.flatMap(listed -> SGCraftApi.get().stargates()
                .at(listed.position())).filter(Stargate::exists);
        final Optional<Address> address = Address.parse(action.address());
        final String name = action.name().strip();
        int selected = action.gate();
        String message = "";
        switch (action.kind()) {
            case REFRESH -> {
            }
            case ADD_GATE -> {
                final Optional<Stargate> near = GateTargeting.find(player, ToolActions.range());
                if (near.isEmpty()) {
                    message = "There is no gate near you";
                } else if (!near.get().access().allows(ToolActions.cause(player), GateRight.USE)) {
                    message = "You aren't allowed to use this gate";
                } else if (gates.stream().anyMatch(g -> g.position().equals(near.get().position()))) {
                    message = "This tablet already has that gate";
                } else {
                    final String fallback = SGCraftApi.get().addressing()
                            .addressOf(near.get().position(), player.level().dimension()).map(Address::toString)
                            .orElse(near.get().position().pos().toShortString());
                    gates.add(new TabletGate(near.get().position(), ToolActions.gateName(name, fallback)));
                    ToolActions.setTabletGates(stack, gates);
                    selected = gates.size() - 1;
                }
            }
            case RENAME_GATE -> {
                if (chosen.isEmpty()) {
                    message = "Choose a gate first";
                } else {
                    gates.set(action.gate(), new TabletGate(chosen.get().position(),
                            ToolActions.gateName(name, chosen.get().name())));
                    ToolActions.setTabletGates(stack, gates);
                }
            }
            case REMOVE_GATE -> {
                if (chosen.isEmpty()) {
                    message = "Choose a gate first";
                } else {
                    gates.remove(action.gate());
                    ToolActions.setTabletGates(stack, gates);
                    selected = Math.max(0, action.gate() - 1);
                }
            }
            case MOVE_GATE -> {
                if (chosen.isPresent() && action.index() >= 0 && action.index() < gates.size()) {
                    gates.add(action.index(), gates.remove(action.gate()));
                    ToolActions.setTabletGates(stack, gates);
                    selected = action.index();
                }
            }
            case DIAL -> {
                if (chosen.isEmpty()) {
                    message = "Choose a gate first";
                } else if (chosenGate.isEmpty()) {
                    message = "That gate is gone";
                } else if (address.isEmpty()) {
                    message = "That isn't an address";
                } else {
                    final int replyTo = selected;
                    final Stargate gate = chosenGate.get();
                    gate.dial(ToolActions.toDial(book, address.get(), gate),
                            gate.get(GateProperties.CHEVRONS_LOCK_ON_DIAL).orElse(false)
                            ? DialMode.QUICK : DialMode.STANDARD, ToolActions.cause(player)).thenAccept(result -> {
                                if (result != DialResult.CONNECTED && player.connection != null) {
                                    ToolActions.sendTablet(player, false, replyTo, ToolActions.explain(result));
                                }
                            });
                }
            }
            case OPEN_IRIS, CLOSE_IRIS -> message = chosen.isEmpty() ? "Choose a gate first" : chosenGate
                    .map(gate -> ToolActions.iris(gate, action.kind() == TabletAction.Kind.OPEN_IRIS,
                            ToolActions.cause(player)))
                    .orElse("That gate is gone");
            case CONFIGURE -> {
                if (chosen.isEmpty()) {
                    message = "Choose a gate first";
                } else if (chosenGate.isEmpty()) {
                    message = "That gate is gone";
                } else if (!chosenGate.get().access().allows(ToolActions.cause(player), GateRight.CONFIGURE)) {
                    message = "You aren't allowed to configure this gate";
                } else {
                    // Its configurator opens in place of the tablet's screen
                    ToolActions.sendConfigurator(player, chosenGate.get(), true, "");
                    return;
                }
            }
            case DISCONNECT -> message = chosen.isEmpty() ? "Choose a gate first" : chosenGate
                    .map(gate -> ToolActions.explain(gate.disconnect(ToolActions.cause(player))))
                    .orElse("That gate is gone");
            case ADD -> {
                if (address.isEmpty()) {
                    message = "That isn't an address";
                } else {
                    // Kept in the form that names its world, so it dials from any world
                    final Address kept = SGServer.require().addressing().lasting(address.get(),
                            player.level().dimension());
                    message = ToolActions.explain(book, kept, book.add(kept,
                            Component.literal(ToolActions.name(name, kept)), ToolActions.cause(player)));
                    if (message.equals("Nothing changed")) {
                        message = "This tablet already has that address";
                    }
                }
            }
            case RENAME -> message = address.map(a -> ToolActions.explain(book, a, book.rename(a,
                    Component.literal(ToolActions.name(name, a)), ToolActions.cause(player))))
                    .orElse("That isn't an address");
            case REMOVE -> message = address.map(a -> ToolActions.explain(book, a,
                    book.remove(a, ToolActions.cause(player)))).orElse("That isn't an address");
            case MOVE -> message = address.map(a -> ToolActions.explain(book.move(a, action.index(),
                    ToolActions.cause(player)))).orElse("That isn't an address");
            default -> {
            }
        }
        ToolActions.sendTablet(player, false, selected, message);
    }

    private static String gateName(final String name, final String fallback) {
        final String trimmed = name.length() > ToolActions.MAX_NAME ? name.substring(0, ToolActions.MAX_NAME) : name;
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    // The GDO

    /** A player used a GDO: its screen opens on the gate near them. */
    public static void openGdo(final ServerPlayer player) {
        final Optional<Stargate> gate = GateTargeting.find(player, ToolActions.range());
        if (gate.isEmpty()) {
            ToolActions.tell(player, "message.sgcraft.no_gate_near", "There is no gate near you");
            return;
        }
        ToolActions.sendGdo(player, gate.get(), true, "");
    }

    private static void sendGdo(final ServerPlayer player, final Stargate gate, final boolean open,
            final String message) {
        final Optional<Stargate> remote = ToolActions.remote(gate);
        final boolean canDisconnect = gate.state() != GateState.IDLE && gate.state() != GateState.CLOSING;
        SGNetwork.send(player, new GdoState(open, ToolActions.info(player, gate),
                remote.map(other -> ToolActions.info(player, other)), canDisconnect, message));
    }

    // The gate at the other end of the wormhole, once it is open
    private static Optional<Stargate> remote(final Stargate gate) {
        if (gate.state() != GateState.CONNECTED) {
            return Optional.empty();
        }
        return gate.connection().flatMap(connection -> connection.other(gate));
    }

    public static void gdo(final ServerPlayer player, final GdoAction action) {
        final Optional<Stargate> gate = ToolActions.near(player, action.gate());
        if (gate.isEmpty()) {
            ToolActions.tell(player, "message.sgcraft.no_gate_near", "There is no gate near you");
            return;
        }
        final Cause cause = ToolActions.cause(player);
        final Optional<Stargate> remote = ToolActions.remote(gate.get());
        final String message = switch (action.kind()) {
            case OPEN_LOCAL -> ToolActions.iris(gate.get(), true, cause);
            case CLOSE_LOCAL -> ToolActions.iris(gate.get(), false, cause);
            case OPEN_REMOTE -> remote.map(other -> ToolActions.iris(other, true, cause))
                    .orElse("The wormhole isn't open");
            case CLOSE_REMOTE -> remote.map(other -> ToolActions.iris(other, false, cause))
                    .orElse("The wormhole isn't open");
            case DISCONNECT -> ToolActions.explain(gate.get().disconnect(cause));
            default -> "";
        };
        ToolActions.sendGdo(player, gate.get(), false, message);
    }

    /**
     * Opens or closes a gate's iris, which for a shield lowers or raises it.
     *
     * @return what went wrong, or nothing
     */
    public static String iris(final Stargate gate, final boolean open, final Cause cause) {
        final Optional<Iris> iris = gate.iris();
        if (iris.isEmpty()) {
            return "That gate has no iris";
        }
        final ActionResult result = open ? iris.get().open(cause) : iris.get().close(cause);
        if (result == ActionResult.NOT_POSSIBLE && !open && SGIris.isShield(iris.get().type())) {
            return "The gate doesn't have the energy to raise its shield";
        }
        return ToolActions.explain(result);
    }

    // The configurator

    /** A player used a configurator: its screen opens on the gate near them, if they may configure it. */
    public static void openConfigurator(final ServerPlayer player) {
        final Optional<Stargate> gate = GateTargeting.find(player, ToolActions.range());
        if (gate.isEmpty()) {
            ToolActions.tell(player, "message.sgcraft.no_gate_near", "There is no gate near you");
            return;
        }
        if (!gate.get().access().allows(ToolActions.cause(player), GateRight.CONFIGURE)) {
            ToolActions.tell(player, "message.sgcraft.cannot_configure", "You aren't allowed to configure this gate");
            return;
        }
        ToolActions.sendConfigurator(player, gate.get(), true, "");
    }

    // A name as JSON, as the screens read names back
    private static String json(final Component name) {
        return ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, name).result().map(JsonElement::toString)
                .orElse("");
    }

    private static void sendConfigurator(final ServerPlayer player, final Stargate gate, final boolean open,
            final String message) {
        final Registry<GateProperty<?>> properties = player.level().getServer().registryAccess()
                .lookupOrThrow(SGCraftRegistries.GATE_PROPERTY);
        // Who is listed, and with what rights, is only for the owner and operators to see
        final boolean manage = gate.access().mayManage(ToolActions.cause(player));
        final List<Property> values = new ArrayList<>();
        for (final GateProperty<?> property : properties) {
            final Identifier id = properties.getKey(property);
            if (id != null && (manage || property != GateProperties.PLAYER_RIGHTS)) {
                values.add(ToolActions.property(gate, id, property));
            }
        }
        final List<String> types = new ArrayList<>();
        final List<String> typeNames = new ArrayList<>();
        // Only the types a gate of its size can be, as only a supergate can be an Ori gate
        player.level().getServer().registryAccess().lookupOrThrow(SGCraftRegistries.GATE_TYPE).listElements()
                .filter(type -> type.value().supports(gate.size()) || type.value() == gate.type().value())
                .forEach(type -> {
                    types.add(type.key().identifier().toString());
                    typeNames.add(ToolActions.json(type.value().name()));
                });
        final List<String> irisTypes = new ArrayList<>();
        final List<String> irisNames = new ArrayList<>();
        player.level().getServer().registryAccess().lookupOrThrow(SGCraftRegistries.IRIS_TYPE).listElements()
                .filter(iris -> gate.type().value().acceptsIris(iris.key()))
                .forEach(iris -> {
                    irisTypes.add(iris.key().identifier().toString());
                    irisNames.add(ToolActions.json(iris.value().name()));
                });
        final List<Named> players = new ArrayList<>();
        if (manage) {
            for (final UUID uuid : gate.get(GateProperties.PLAYER_RIGHTS).orElse(Map.of()).keySet()) {
                players.add(new Named(uuid.toString(), ToolActions.name(player, uuid)));
            }
        }
        final Optional<Named> owner = gate.owner().map(uuid -> new Named(uuid.toString(),
                ToolActions.name(player, uuid)));
        SGNetwork.send(player, new ConfiguratorState(open, ToolActions.info(player, gate), gate.chevrons(), types,
                gate.position().dimension().identifier().toString(), typeNames, irisTypes, irisNames,
                values, players, message,
                1 / SGCraftApi.get().energy().rate(PowerUnitBlockEntity.FORGE_ENERGY),
                GateOrientation.of(gate.orientation()).getSerializedName(), owner,
                manage, ToolActions.fitsFreely(player)));
    }

    // Operators and players in creative mode change a gate's type, chevrons and iris with the configurator; anyone
    // else fits upgrade items, as in 1.12
    private static boolean fitsFreely(final ServerPlayer player) {
        return player.isCreative() || SGGateAccess.isOperator(player);
    }

    // A player's name when last seen, or their UUID if they never were
    private static String name(final ServerPlayer player, final UUID uuid) {
        return player.level().getServer().services().nameToIdCache().get(uuid).map(NameAndId::name)
                .orElse(uuid.toString());
    }

    private static <T> Property property(final Stargate gate, final Identifier id, final GateProperty<T> property) {
        final Optional<T> own = SGServer.require().stargates().ownProperty(gate.position(), property);
        return new Property(id.toString(), ToolActions.json(property, own), own.isPresent(),
                ToolActions.json(property, ToolActions.fallback(gate, property)));
    }

    private static <T> String json(final GateProperty<T> property, final Optional<T> value) {
        return value.flatMap(v -> property.codec().encodeStart(JsonOps.INSTANCE, v).result())
                .map(JsonElement::toString).orElse("");
    }

    // What a gate without a value of its own has: the server's value for gates if it sets one, otherwise the
    // property's default, for the dial costs and the other energy settings the server's, and for the ring's speed
    // its type's
    @SuppressWarnings("unchecked")
    private static <T> Optional<T> fallback(final Stargate gate, final GateProperty<T> property) {
        final Optional<T> inherited = SGServer.require().stargates().inherited(gate.position(), property);
        if (inherited.isPresent()) {
            return inherited;
        }
        if (property == GateProperties.RING_SPEED) {
            return Optional.of((T) (Double) DialStyle.of(gate.type().unwrapKey().orElse(GateTypes.MILKY_WAY))
                    .defaultSpeed());
        }
        final SGConfig config = SGServer.require().config();
        if (property == GateProperties.ENERGY_PER_NAQUADAH) {
            return Optional.of((T) (Double) config.energyPerNaquadah());
        }
        if (property == GateProperties.DISTANCE_FACTOR_MULTIPLIER) {
            return Optional.of((T) (Double) config.distanceFactorMultiplier());
        }
        if (property == GateProperties.INTER_WORLD_MULTIPLIER) {
            return Optional.of((T) (Double) config.interWorldMultiplier());
        }
        final DialCosts costs = config.dialCosts();
        for (final DialCosts.Kind kind : DialCosts.Kind.values()) {
            if (property == kind.enabledProperty()) {
                return Optional.of((T) (Boolean) costs.get(kind).enabled());
            }
            if (property == kind.energyProperty()) {
                return Optional.of((T) (Double) costs.get(kind).energy());
            }
        }
        return Optional.empty();
    }

    // The gate a configurator screen names: one near the player holding a configurator, or one on the list of the
    // tablet they hold
    private static Optional<Stargate> configured(final ServerPlayer player, final String world,
            final net.minecraft.core.BlockPos pos) {
        final Identifier id = Identifier.tryParse(world);
        if (ToolActions.holding(player, SGItems.CONFIGURATOR)
                && (id == null || id.equals(player.level().dimension().identifier()))) {
            final Optional<Stargate> near = ToolActions.near(player, pos);
            if (near.isPresent() || id == null) {
                return near;
            }
        }
        final GlobalPos at = GlobalPos.of(ResourceKey.create(Registries.DIMENSION, id), pos);
        return ToolActions.tablet(player).filter(stack -> ToolActions.tabletGates(stack).stream()
                .anyMatch(listed -> listed.position().equals(at)))
                .flatMap(stack -> SGCraftApi.get().stargates().at(at)).filter(Stargate::exists);
    }

    public static void configurator(final ServerPlayer player, final ConfiguratorApply apply) {
        final Optional<Stargate> found = ToolActions.configured(player, apply.world(), apply.gate());
        if (found.isEmpty()) {
            ToolActions.tell(player, "message.sgcraft.no_gate_near", "There is no gate near you");
            return;
        }
        final Stargate gate = found.get();
        final Cause cause = ToolActions.cause(player);
        if (!gate.access().allows(cause, GateRight.CONFIGURE)) {
            ToolActions.tell(player, "message.sgcraft.cannot_configure", "You aren't allowed to configure this gate");
            return;
        }
        if (apply.refresh()) {
            ToolActions.sendConfigurator(player, gate, false, "");
            return;
        }
        final List<String> problems = new ArrayList<>();
        final boolean free = ToolActions.fitsFreely(player);
        // Set when a change only upgrade items may make was asked for, and refused
        boolean upgradesOnly = false;
        if (!apply.gateType().isEmpty()) {
            final Identifier id = Identifier.tryParse(apply.gateType());
            final Optional<Holder.Reference<GateType>> type = id == null ? Optional.empty() : player.level().getServer()
                    .registryAccess().lookupOrThrow(SGCraftRegistries.GATE_TYPE).get(id);
            if (type.isEmpty()) {
                problems.add("There is no gate type " + apply.gateType());
            } else if (!gate.type().equals(type.get())) {
                if (free) {
                    ToolActions.problem(problems, "type", gate.setType(type.get(), cause));
                } else {
                    upgradesOnly = true;
                }
            }
        }
        if (apply.chevrons() > 0 && apply.chevrons() != gate.chevrons()) {
            if (free) {
                ToolActions.problem(problems, "chevrons", gate.setChevrons(apply.chevrons(), cause));
            } else {
                upgradesOnly = true;
            }
        }
        if (!apply.orientation().isEmpty()) {
            ToolActions.turn(gate, apply.orientation(), cause, problems);
        }
        final Registry<GateProperty<?>> properties = player.level().getServer().registryAccess()
                .lookupOrThrow(SGCraftRegistries.GATE_PROPERTY);
        for (final Change change : apply.changes()) {
            final Identifier id = Identifier.tryParse(change.id());
            final GateProperty<?> property = id == null ? null : properties.getValue(id);
            if (property == null) {
                problems.add("There is no property " + change.id());
                continue;
            }
            if ((property == GateProperties.IRIS || property == GateProperties.INVULNERABLE) && !free) {
                upgradesOnly = true;
                continue;
            }
            ToolActions.change(player, gate, property, change, cause, problems);
        }
        if (upgradesOnly) {
            problems.add("Use a gate upgrade, a chevron upgrade, an iris upgrade or a shield upgrade to change the "
                    + "gate's type, chevrons or iris. Only operators and creative players change Invulnerable");
        }
        final String name = apply.addPlayer().strip();
        if (!name.isEmpty()) {
            final ServerPlayer online = player.level().getServer().getPlayerList().getPlayerByName(name);
            if (online != null) {
                ToolActions.addPlayer(gate, online.nameAndId(), cause, problems);
            } else if (ToolActions.LOOKING_UP.add(player.getUUID())) {
                // A player who isn't on may need asking Mojang, which mustn't hold up the server; the screen hears
                // the outcome when the answer comes
                ToolActions.lookUp(player, gate, name, cause, problems);
                return;
            } else {
                problems.add("Still looking up the last player added");
            }
        }
        ToolActions.sendConfigurator(player, gate, false, ToolActions.outcome(problems));
    }

    private static String outcome(final List<String> problems) {
        return problems.isEmpty() ? "Changes saved" : String.join("; ", problems);
    }

    // Looks up a player's name away from the server thread, then adds them to the gate's list on it
    private static void lookUp(final ServerPlayer player, final Stargate gate, final String name, final Cause cause,
            final List<String> problems) {
        final MinecraftServer server = player.level().getServer();
        CompletableFuture.supplyAsync(() -> server.services().nameToIdCache().get(name), Util.nonCriticalIoPool())
                .whenCompleteAsync((found, error) -> {
                    ToolActions.LOOKING_UP.remove(player.getUUID());
                    if (error != null || found.isEmpty()) {
                        problems.add("No player called " + name + " has been seen");
                    } else if (gate.exists() && gate.access().allows(cause, GateRight.CONFIGURE)) {
                        ToolActions.addPlayer(gate, found.get(), cause, problems);
                    }
                    if (!player.isRemoved() && gate.exists()) {
                        ToolActions.sendConfigurator(player, gate, false, ToolActions.outcome(problems));
                    }
                }, server);
    }

    // A gate lying flat turns face up or face down, as in the original; one standing stays as it was built
    private static void turn(final Stargate gate, final String name, final Cause cause, final List<String> problems) {
        final Optional<GateOrientation> lying = Arrays.stream(GateOrientation.values())
                .filter(each -> each.getSerializedName().equals(name)).findFirst();
        if (lying.isEmpty() || !lying.get().isFlat()) {
            problems.add("A gate can only be turned face up or face down, not " + name);
            return;
        }
        if (!GateOrientation.of(gate.orientation()).isFlat()) {
            problems.add("Only a gate lying flat can be turned");
            return;
        }
        ToolActions.problem(problems, "orientation", gate.setOrientation(
                lying.get().frontAndTop(GateOrientation.facing(gate.orientation())), cause));
    }

    private static <T> void change(final ServerPlayer player, final Stargate gate, final GateProperty<T> property,
            final Change change, final Cause cause, final List<String> problems) {
        final Optional<T> before = gate.get(property);
        // Only an iris fitted with an iris upgrade gives one back: not one the gate came with, nor one set without
        // the item
        final boolean included = gate instanceof final SGStargate built && built.irisIncluded();
        if (change.json().isEmpty()) {
            final ActionResult result = gate.remove(property, cause);
            if (result != ActionResult.NO_CHANGE) {
                ToolActions.problem(problems, change.id(), result);
            }
            if (!included) {
                ToolActions.irisChanged(player, property, before, gate.get(property));
            }
            return;
        }
        final Optional<T> value;
        try {
            value = property.codec().parse(JsonOps.INSTANCE, JsonParser.parseString(change.json().get())).result();
        } catch (final RuntimeException e) {
            problems.add(change.id() + " isn't valid");
            return;
        }
        if (value.isEmpty()) {
            problems.add(change.id() + " isn't valid");
            return;
        }
        if (value.equals(before)) {
            return;
        }
        ToolActions.problem(problems, change.id(), gate.set(property, value.get(), cause));
        if (!included) {
            ToolActions.irisChanged(player, property, before, gate.get(property));
        }
    }

    // An iris fitted with its upgrade, taken off or swapped for the other kind with the configurator, comes back as
    // the upgrade
    private static void irisChanged(final ServerPlayer player, final GateProperty<?> property, final Optional<?> before,
            final Optional<?> after) {
        if (property == GateProperties.IRIS && before.isPresent() && !after.equals(before) && !player.isCreative()
                && before.get() instanceof final ResourceKey<?> key) {
            key.cast(SGCraftRegistries.IRIS_TYPE).flatMap(IrisUpgradeItem::forType).ifPresent(upgrade -> player
                    .getInventory().placeItemBackInInventory(new ItemStack(upgrade), Prediction.SERVER_ONLY));
        }
    }

    private static void addPlayer(final Stargate gate, final NameAndId found, final Cause cause,
            final List<String> problems) {
        final Map<UUID, Set<GateRight>> rights = new HashMap<>(gate.get(GateProperties.PLAYER_RIGHTS).orElse(Map.of()));
        if (rights.containsKey(found.id())) {
            return;
        }
        if (rights.size() >= GateProperties.MAX_ENTRIES) {
            problems.add("A gate lists at most " + GateProperties.MAX_ENTRIES + " players");
            return;
        }
        rights.put(found.id(), gate.get(GateProperties.DEFAULT_RIGHTS).orElse(Set.of()));
        ToolActions.problem(problems, "player_rights", gate.set(GateProperties.PLAYER_RIGHTS, Map.copyOf(rights),
                cause));
    }

    private static void problem(final List<String> problems, final String what, final ActionResult result) {
        if (result != ActionResult.DONE && result != ActionResult.NO_CHANGE) {
            problems.add(what + ": " + ToolActions.explain(result));
        }
    }
}
