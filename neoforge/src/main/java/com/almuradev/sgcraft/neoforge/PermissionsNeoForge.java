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

package com.almuradev.sgcraft.neoforge;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.access.SGPermissions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.exceptions.UnregisteredPermissionException;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SGCraft's permission nodes in NeoForge's permission API, which the server's permission handler answers: LuckPerms
 * once {@code permissionHandler} in {@code neoforge-server.toml} names it. A node the handler says nothing of falls to
 * its default.
 */
final class PermissionsNeoForge {

    // The nodes given NeoForge as the server started, by name
    private static final Map<String, PermissionNode<Boolean>> NODES = new ConcurrentHashMap<>();

    private PermissionsNeoForge() {
    }

    /** Gives NeoForge every node, as the server starts. */
    static void gather(final PermissionGatherEvent.Nodes event) {
        for (final SGPermissions.Node node : SGPermissions.all()) {
            final PermissionNode<Boolean> permission = new PermissionNode<>(SGCraft.MOD_ID, node.path(),
                    PermissionTypes.BOOLEAN, (player, uuid, context) -> player != null
                            ? SGPermissions.fallback(player, node) : node.fallback() == SGPermissions.Default.EVERYONE);
            permission.setInformation(Component.literal(node.name()), Component.literal(node.description()));
            PermissionsNeoForge.NODES.put(node.name(), permission);
            event.addNodes(permission);
        }
    }

    static boolean has(final ServerPlayer player, final SGPermissions.Node node) {
        final PermissionNode<Boolean> permission = PermissionsNeoForge.NODES.get(node.name());
        if (permission == null) {
            return SGPermissions.fallback(player, node);
        }
        try {
            return PermissionAPI.getPermission(player, permission);
        } catch (final UnregisteredPermissionException e) {
            return SGPermissions.fallback(player, node);
        }
    }
}
