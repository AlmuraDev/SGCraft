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

package com.almuradev.sgcraft.mod.dhd;

import com.almuradev.sgcraft.api.ActionResult;
import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.address.Symbol;
import com.almuradev.sgcraft.api.gate.DialMode;
import com.almuradev.sgcraft.api.gate.DialResult;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.GateState;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.block.entity.DhdBlockEntity;
import com.almuradev.sgcraft.mod.gate.LockDials;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * What the server does when a player works a DHD. A player must be within reach of the DHD, and the DHD linked to a
 * gate that accepts it.
 */
public final class DhdActions {

    // How far a player can be from a DHD and still work it, in blocks
    private static final double REACH = 8;

    private DhdActions() {
    }

    /**
     * A button pressed. On a gate whose chevrons lock as dialed, each symbol locks its chevron at once and the center
     * dials those locked; otherwise the symbols are only entered on the screen, and the center shuts an active gate
     * down.
     */
    public static void press(final ServerPlayer player, final DhdPress press) {
        final Cause cause = Cause.of(player.level().getBlockEntity(press.dhd()), player);
        if (press.button() != DhdPress.CENTER) {
            // Quietly for a DHD without a gate, as every key is sent, not just those that lock a chevron
            if (!press.dhd().closerToCenterThan(player.position(), DhdActions.REACH)
                    || !(player.level().getBlockEntity(press.dhd()) instanceof DhdBlockEntity dhd)) {
                return;
            }
            final Optional<Stargate> gate = dhd.gate().filter(LockDials::wanted);
            final Optional<Symbol> symbol = DhdActions.symbol(press.button());
            if (gate.isPresent() && symbol.isPresent()) {
                final ActionResult result = gate.get().engage(symbol.get(), cause);
                if (result == ActionResult.DENIED) {
                    DhdActions.tell(player, "message.sgcraft.dial_failed.denied", "Dialing failed: denied");
                }
            }
            return;
        }
        final Optional<Stargate> gate = DhdActions.gate(player, press.dhd());
        if (gate.isPresent() && !gate.get().engaged().isEmpty()) {
            // The chevrons locked one key at a time: the dome dials them
            DhdActions.report(player, gate.get().dialEngaged(cause));
        } else if (gate.isPresent() && gate.get().state() != GateState.IDLE) {
            gate.get().disconnect(cause);
        }
    }

    /**
     * A whole address entered and dialed: the gate turns its ring for each symbol, as the original's did, or locks
     * every chevron at once if it is set to.
     */
    public static void dial(final ServerPlayer player, final DhdDial dial) {
        final Optional<Stargate> gate = DhdActions.gate(player, dial.dhd());
        final Optional<Address> address = Address.parse(dial.address());
        if (gate.isEmpty() || address.isEmpty()) {
            return;
        }
        final Cause cause = Cause.of(player.level().getBlockEntity(dial.dhd()), player);
        if (!gate.get().engaged().isEmpty()) {
            // Its chevrons locked as the keys were pressed: those are what it dials
            DhdActions.report(player, gate.get().dialEngaged(cause));
            return;
        }
        DhdActions.report(player, gate.get().dial(address.get(), gate.get().get(GateProperties.CHEVRONS_LOCK_ON_DIAL)
                .orElse(false) ? DialMode.QUICK : DialMode.STANDARD, cause));
    }

    // The symbol a DHD button stands for, if it is one
    private static Optional<Symbol> symbol(final int button) {
        return button >= 0 && button < Symbol.COUNT ? Optional.of(new Symbol(button)) : Optional.empty();
    }

    private static void tell(final ServerPlayer player, final String key, final String fallback) {
        player.sendOverlayMessage(Component.translatableWithFallback(key, fallback).withStyle(ChatFormatting.RED));
    }

    private static Optional<Stargate> gate(final ServerPlayer player, final BlockPos pos) {
        if (!pos.closerToCenterThan(player.position(), DhdActions.REACH)
                || !(player.level().getBlockEntity(pos) instanceof DhdBlockEntity dhd)) {
            return Optional.empty();
        }
        final Optional<Stargate> gate = dhd.gate();
        if (gate.isEmpty()) {
            player.sendOverlayMessage(Component.translatableWithFallback("message.sgcraft.dhd_unlinked",
                    "This DHD isn't linked to a gate it can dial").withStyle(ChatFormatting.RED));
        }
        return gate;
    }

    /** Tells the player why a dial failed, over their hotbar, once it has. */
    public static void report(final ServerPlayer player, final CompletionStage<DialResult> result) {
        DhdActions.report(result, (key, fallback) -> player.sendOverlayMessage(
                Component.translatableWithFallback(key, fallback).withStyle(ChatFormatting.RED)));
    }

    /** Says why a dial failed, as a translation key and the text without one, once it has. */
    public static void report(final CompletionStage<DialResult> result, final BiConsumer<String, String> tell) {
        result.thenAccept(outcome -> {
            if (outcome != DialResult.CONNECTED) {
                final String name = outcome.name().toLowerCase(Locale.ROOT);
                tell.accept("message.sgcraft.dial_failed." + name, "Dialing failed: " + name.replace('_', ' '));
            }
        });
    }
}
