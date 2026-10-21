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

package com.almuradev.sgcraft.mod.zpm;

import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.access.GateRight;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.SGServer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ZPM consoles, interface carts and hubs linked straight to a gate, as in 2.2.0: the gate draws on a linked device's
 * ZPMs from anywhere in the same world while the device is loaded, as well as on the devices within the server's
 * search range of it. A gate of any size may have devices linked to it.
 *
 * <p>A player sneak-uses a device with an empty hand, then uses any block of a formed gate in the same world, on which
 * they have the {@code configure} right, within a minute. Sneak-using the same device again cancels, or, if it is
 * linked, unlinks it. A device broken is unlinked. The links are kept with the world.</p>
 */
public final class ZpmLinks {

    // How long a link started waits for its gate, in ticks
    private static final int PENDING_TICKS = 1200;
    // The links started and not yet finished, by player
    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    private ZpmLinks() {
    }

    // A link a player started at a device, and the server tick it lapses at
    private record Pending(GlobalPos device, int until) {
    }

    /** A player sneak-used a device: starts linking it, or cancels or unlinks it. */
    public static void sneakUse(final ServerPlayer player, final BlockPos pos) {
        final MinecraftServer server = player.level().getServer();
        final GlobalPos device = GlobalPos.of(player.level().dimension(), pos);
        final Pending pending = ZpmLinks.PENDING.remove(player.getUUID());
        if (pending != null && pending.device().equals(device) && pending.until() >= server.getTickCount()) {
            if (ZpmLinks.unlink(server, device)) {
                ZpmLinks.tell(player, "message.sgcraft.zpm_link.unlinked", "This ZPM device is no longer linked to a "
                        + "gate", false);
            } else {
                ZpmLinks.tell(player, "message.sgcraft.zpm_link.canceled", "Linking canceled", false);
            }
            return;
        }
        ZpmLinks.PENDING.put(player.getUUID(), new Pending(device, server.getTickCount() + ZpmLinks.PENDING_TICKS));
        final Optional<GlobalPos> linked = ZpmLinks.gateOf(server, device);
        if (linked.isPresent()) {
            ZpmLinks.tell(player, "message.sgcraft.zpm_link.start_linked", "Linked to the gate at %s. Use a block of "
                    + "another gate to link it there, or sneak-use this device again to unlink it", false,
                    linked.get().pos().toShortString());
        } else {
            ZpmLinks.tell(player, "message.sgcraft.zpm_link.start", "Now use any block of a gate to link this ZPM "
                    + "device to it, or sneak-use this device again to cancel", false);
        }
    }

    /**
     * A player used a block of a gate with an empty hand: finishes the link they started, if any.
     *
     * @return whether it finished or refused a link, so the block does nothing else
     */
    public static boolean complete(final ServerPlayer player, final BlockPos pos) {
        final MinecraftServer server = player.level().getServer();
        final Pending pending = ZpmLinks.PENDING.get(player.getUUID());
        if (pending == null) {
            return false;
        }
        if (pending.until() < server.getTickCount()) {
            ZpmLinks.PENDING.remove(player.getUUID());
            return false;
        }
        final Optional<Stargate> gate = SGServer.current().flatMap(sg -> sg.stargates()
                .at(GlobalPos.of(player.level().dimension(), pos)));
        if (gate.isEmpty()) {
            return false;
        }
        ZpmLinks.PENDING.remove(player.getUUID());
        if (!pending.device().dimension().equals(gate.get().position().dimension())) {
            ZpmLinks.tell(player, "message.sgcraft.zpm_link.other_world", "A ZPM device links only to a gate in its "
                    + "own world", true);
            return true;
        }
        if (!gate.get().access().allows(Cause.of(player), GateRight.CONFIGURE)) {
            ZpmLinks.tell(player, "message.sgcraft.zpm_link.denied", "You aren't allowed to configure this gate",
                    true);
            return true;
        }
        if (!(player.level().getBlockEntity(pending.device().pos()) instanceof ZpmDeviceBlockEntity)) {
            ZpmLinks.tell(player, "message.sgcraft.zpm_link.device_gone", "The ZPM device is no longer there", true);
            return true;
        }
        ZpmLinks.link(server, pending.device(), gate.get().position());
        ZpmLinks.tell(player, "message.sgcraft.zpm_link.linked", "Linked. The gate draws on this ZPM device from "
                + "anywhere in this world while it's loaded", false);
        return true;
    }

    /** @return the gate a device is linked to, if any */
    public static Optional<GlobalPos> gateOf(final MinecraftServer server, final GlobalPos device) {
        return Optional.ofNullable(ZpmLinks.store(server).links.get(device));
    }

    /** @return the devices linked to a gate */
    public static List<GlobalPos> devicesOf(final MinecraftServer server, final GlobalPos gate) {
        final List<GlobalPos> devices = new ArrayList<>();
        ZpmLinks.store(server).links.forEach((device, linked) -> {
            if (linked.equals(gate)) {
                devices.add(device);
            }
        });
        return devices;
    }

    /** Links a device to a gate, in place of any gate it was linked to. */
    public static void link(final MinecraftServer server, final GlobalPos device, final GlobalPos gate) {
        final Store store = ZpmLinks.store(server);
        if (!gate.equals(store.links.put(device, gate))) {
            store.setDirty();
        }
    }

    /** @return whether the device was linked, and is now not */
    public static boolean unlink(final MinecraftServer server, final GlobalPos device) {
        final Store store = ZpmLinks.store(server);
        if (store.links.remove(device) == null) {
            return false;
        }
        store.setDirty();
        return true;
    }

    private static void tell(final ServerPlayer player, final String key, final String fallback, final boolean bad,
            final Object... args) {
        final Component text = Component.translatableWithFallback(key, fallback, args);
        player.sendOverlayMessage(bad ? text.copy().withStyle(ChatFormatting.RED) : text);
    }

    private static Store store(final MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(Store.TYPE);
    }

    // Every link, by device, as saved
    static final class Store extends SavedData {

        private static final Codec<Map.Entry<GlobalPos, GlobalPos>> LINK_CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        GlobalPos.CODEC.fieldOf("device").forGetter(Map.Entry::getKey),
                        GlobalPos.CODEC.fieldOf("gate").forGetter(Map.Entry::getValue)
                ).apply(instance, Map::entry));

        private static final Codec<Store> CODEC = Store.LINK_CODEC.listOf()
                .xmap(Store::new, store -> new ArrayList<>(store.links.entrySet()));

        static final SavedDataType<Store> TYPE = new SavedDataType<>(SGCraft.id("zpm_links"), Store::new,
                Store.CODEC, null);

        private final Map<GlobalPos, GlobalPos> links = new HashMap<>();

        private Store() {
        }

        private Store(final List<Map.Entry<GlobalPos, GlobalPos>> links) {
            links.forEach(entry -> this.links.put(entry.getKey(), entry.getValue()));
        }
    }
}
