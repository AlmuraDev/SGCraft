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

import com.almuradev.sgcraft.api.SGCraftApi;
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.gate.DialMode;
import com.almuradev.sgcraft.api.gate.DialResult;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.gate.GateTargeting;
import com.almuradev.sgcraft.mod.gate.SGStargate;
import com.almuradev.sgcraft.mod.item.SGItems;
import com.almuradev.sgcraft.mod.network.SGNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * The explorer dialer, as in SGCraft 2.2.0: it lists every gate world generation built, in every world, with whether
 * its holder has visited it, and dials any of them from the gate near its holder. Its holder can name the gates it
 * lists, for themselves. Using it at a generated gate counts as a visit, as going through one does.
 */
public final class ExplorerDialer {

    /**
     * The most gates the screen is sent, so the list stays well within what one packet holds; those the player
     * visited or named come first.
     */
    public static final int MAX_LISTED = 4000;

    // Each answer lists every generated gate, so a screen's refreshes are answered at most once a second, as in 1.12,
    // and its buttons a few times a second
    // The order gates are listed in: by world, then by address
    private static final Comparator<ExplorerPayloads.Listed> ORDER = Comparator.comparing(
            ExplorerPayloads.Listed::world).thenComparing(ExplorerPayloads.Listed::address);
    private static final ActionThrottle THROTTLE = new ActionThrottle(20, 4);

    private ExplorerDialer() {
    }

    /** A player used an explorer dialer: a generated gate near them is visited, and its screen opens. */
    public static void open(final ServerPlayer player) {
        GateTargeting.find(player, ToolActions.range()).ifPresent(gate -> ExplorerData.visit(player,
                gate.position()));
        ExplorerDialer.send(player, true, "");
    }

    /** Something was done on the explorer dialer's screen. */
    public static void action(final ServerPlayer player, final ExplorerPayloads.Action action) {
        if (!ExplorerDialer.THROTTLE.allows(player, action.kind() == ExplorerPayloads.Action.Kind.REFRESH)) {
            return;
        }
        if (!ExplorerDialer.holding(player)) {
            ExplorerDialer.send(player, false, "You aren't holding an explorer dialer");
            return;
        }
        final Optional<GlobalPos> listed = ExplorerDialer.parse(action.gate());
        String message = "";
        switch (action.kind()) {
            case DIAL -> {
                final Optional<Stargate> gate = GateTargeting.find(player, ToolActions.range());
                final Optional<Address> address = listed.flatMap(target -> SGCraftApi.get().addressing()
                        .addressOf(target, gate.map(near -> near.position().dimension())
                                .orElse(player.level().dimension())));
                if (gate.isEmpty()) {
                    message = "There is no gate near you";
                } else if (address.isEmpty()) {
                    message = "No address reaches that gate from here";
                } else {
                    gate.get().dial(address.get(), gate.get().get(GateProperties.CHEVRONS_LOCK_ON_DIAL).orElse(false)
                            ? DialMode.QUICK : DialMode.STANDARD, ToolActions.cause(player)).thenAccept(result -> {
                        if (result != DialResult.CONNECTED && player.connection != null) {
                            ExplorerDialer.send(player, false, ToolActions.explain(result));
                        }
                    });
                }
            }
            case DISCONNECT -> message = GateTargeting.find(player, ToolActions.range())
                    .map(gate -> ToolActions.explain(gate.disconnect(ToolActions.cause(player))))
                    .orElse("There is no gate near you");
            case RENAME -> listed.ifPresent(gate -> ExplorerData.rename(player.level().getServer(),
                    player.getUUID(), gate, action.name()));
            default -> {
            }
        }
        ExplorerDialer.send(player, false, message);
    }

    // Whether the player holds an explorer dialer, in either hand
    private static boolean holding(final ServerPlayer player) {
        for (final InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).is(SGItems.EDD)) {
                return true;
            }
        }
        return false;
    }

    private static void send(final ServerPlayer player, final boolean open, final String message) {
        final Optional<Stargate> near = GateTargeting.find(player, ToolActions.range());
        final List<ExplorerPayloads.Listed> entries = ExplorerDialer.entries(player, near);
        SGNetwork.send(player, new ExplorerPayloads.State(open, near.map(gate -> ToolActions.info(player, gate)),
                ExplorerDialer.listed(entries), entries.size(), message));
    }

    /**
     * @return as many of the gates as the screen is sent, at most {@link #MAX_LISTED}: those visited or named first,
     *     then the rest, by world then address
     */
    public static List<ExplorerPayloads.Listed> listed(final List<ExplorerPayloads.Listed> entries) {
        if (entries.size() <= ExplorerDialer.MAX_LISTED) {
            return entries;
        }
        final List<ExplorerPayloads.Listed> kept = new ArrayList<>(entries);
        kept.sort(Comparator.comparing((ExplorerPayloads.Listed entry) -> !entry.visited() && entry.name().isEmpty()));
        final List<ExplorerPayloads.Listed> listed = new ArrayList<>(kept.subList(0, ExplorerDialer.MAX_LISTED));
        listed.sort(ExplorerDialer.ORDER);
        return listed;
    }

    /** @return every generated gate, by world then address, as the player sees it from the gate near them */
    public static List<ExplorerPayloads.Listed> entries(final ServerPlayer player) {
        return ExplorerDialer.entries(player, GateTargeting.find(player, ToolActions.range()));
    }

    // Every generated gate, by world then address, as the player sees it from the gate near them, or their own world
    private static List<ExplorerPayloads.Listed> entries(final ServerPlayer player, final Optional<Stargate> near) {
        final ResourceKey<Level> from = near.map(gate -> gate.position().dimension())
                .orElse(player.level().dimension());
        final Set<GlobalPos> visited = ExplorerData.visited(player.level().getServer(), player.getUUID());
        final List<ExplorerPayloads.Listed> entries = new ArrayList<>();
        SGServer.current().ifPresent(server -> server.stargates().store().gates().forEach(record -> {
            final Optional<SGStargate> gate = server.stargates().gate(record.position());
            if (gate.isEmpty() || !gate.get().get(GateProperties.GENERATED).orElse(false)) {
                return;
            }
            final String address = gate.get().get(GateProperties.ADDRESS_VISIBLE).orElse(true)
                    ? SGCraftApi.get().addressing().addressOf(record.position(), from).map(Address::toString)
                    .orElse("") : "";
            entries.add(new ExplorerPayloads.Listed(ExplorerDialer.key(record.position()),
                    ExplorerData.name(player.level().getServer(), player.getUUID(), record.position()).orElse(""),
                    address, record.position().dimension().identifier().toString(),
                    visited.contains(record.position()), gate.get().state().name().toLowerCase(Locale.ROOT)));
        }));
        entries.sort(ExplorerDialer.ORDER);
        return entries;
    }

    /** @return a gate's place, as the screen names it back: its world, then its base's x, y and z */
    static String key(final GlobalPos gate) {
        return gate.dimension().identifier() + " " + gate.pos().getX() + " " + gate.pos().getY() + " "
                + gate.pos().getZ();
    }

    private static Optional<GlobalPos> parse(final String key) {
        final String[] parts = key.split(" ");
        if (parts.length != 4) {
            return Optional.empty();
        }
        final Identifier world = Identifier.tryParse(parts[0]);
        try {
            return world == null ? Optional.empty() : Optional.of(GlobalPos.of(ResourceKey.create(
                    Registries.DIMENSION, world), new BlockPos(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]))));
        } catch (final NumberFormatException e) {
            return Optional.empty();
        }
    }
}
