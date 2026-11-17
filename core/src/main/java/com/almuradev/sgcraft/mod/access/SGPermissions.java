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

package com.almuradev.sgcraft.mod.access;

import com.almuradev.sgcraft.api.SGCraftRegistries;
import com.almuradev.sgcraft.api.gate.GateProperty;
import com.almuradev.sgcraft.mod.Platform;
import com.almuradev.sgcraft.mod.gate.SGStargate;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * SGCraft's permission nodes, as SGCraft 2.3 had them, which the server's permissions system (LuckPerms, say) grants
 * and denies. Without one, or for a node it says nothing of, each node has its default: everyone for using the tools,
 * and for the rest the players who hold {@link #ADMIN}, which by default are the server's operators.
 *
 * <ul>
 *   <li>{@code sgcraft.admin}: any gate, obelisk, ring platform, ZPM device or city shield, whatever its owner and
 *   access lists say, and the settings only operators changed</li>
 *   <li>{@code sgcraft.gui.<tool>}: using a tool: configurator, pdd, pdd.edit, edd, gdo, tablet, shieldconsole,
 *   cityshield</li>
 *   <li>{@code sgcraft.configurator.<setting>}: changing one of a gate's settings, named as its property is
 *   ({@code sgcraft.configurator.open_ticks}); the cost settings are for admins, the rest for anyone the gate lets
 *   configure it</li>
 *   <li>{@code sgcraft.command.<command>}: each of the {@code /sgcraft} commands that were for operators</li>
 * </ul>
 */
public final class SGPermissions {

    /** Who has a node that the permissions system says nothing of. */
    public enum Default {
        /** Every player. */
        EVERYONE,
        /** The players who hold {@link #ADMIN}. */
        ADMINS,
        /** The server's operators: only {@link #ADMIN} itself. */
        OPERATORS
    }

    /** A permission node: its whole name, such as {@code sgcraft.gui.pdd}, and who has it by default. */
    public record Node(String name, Default fallback, String description) {

        /** @return the part of the name after {@code sgcraft.} */
        public String path() {
            return this.name.substring(SGPermissions.PREFIX.length());
        }
    }

    private static final String PREFIX = "sgcraft.";
    // Every node made so far, by name; the commands' are made as the commands are
    private static final Map<String, Node> NODES = new LinkedHashMap<>();

    public static final Node ADMIN = SGPermissions.node("admin", Default.OPERATORS,
            "Any gate, obelisk, ring platform, ZPM device or city shield, whatever its owner and access lists say");
    public static final Node CONFIGURATOR = SGPermissions.node("gui.configurator", Default.EVERYONE,
            "Using the configurator");
    public static final Node PDD = SGPermissions.node("gui.pdd", Default.EVERYONE, "Using the PDD");
    public static final Node PDD_EDIT = SGPermissions.node("gui.pdd.edit", Default.EVERYONE,
            "Adding, renaming, removing and moving a PDD's addresses");
    public static final Node EDD = SGPermissions.node("gui.edd", Default.EVERYONE, "Using the explorer's dialer");
    public static final Node GDO = SGPermissions.node("gui.gdo", Default.EVERYONE, "Using the GDO");
    public static final Node TABLET = SGPermissions.node("gui.tablet", Default.EVERYONE,
            "Using the gate control tablet");
    public static final Node SHIELD_CONSOLE = SGPermissions.node("gui.shieldconsole", Default.EVERYONE,
            "Using a shield console");
    public static final Node CITY_SHIELD = SGPermissions.node("gui.cityshield", Default.EVERYONE,
            "Using a city shield generator");

    private SGPermissions() {
    }

    private static synchronized Node node(final String path, final Default fallback, final String description) {
        return SGPermissions.NODES.computeIfAbsent(SGPermissions.PREFIX + path,
                name -> new Node(name, fallback, description));
    }

    /**
     * @return every node: the fixed ones, the commands' made so far, and one for each gate setting, for a
     *     permissions system to list
     */
    public static synchronized Collection<Node> all() {
        SGPermissions.properties().forEach(SGPermissions::configurator);
        return List.copyOf(SGPermissions.NODES.values());
    }

    private static List<Identifier> properties() {
        final Registry<GateProperty<?>> registry = SGPermissions.propertyRegistry();
        return registry == null ? List.of() : new ArrayList<>(registry.keySet());
    }

    // The gate properties' registry, a built-in one, so there from the start
    @SuppressWarnings("unchecked")
    private static Registry<GateProperty<?>> propertyRegistry() {
        return (Registry<GateProperty<?>>) BuiltInRegistries.REGISTRY.getValue(
                SGCraftRegistries.GATE_PROPERTY.identifier());
    }

    /** @return the node for changing a gate setting, or null for a property not in the registry */
    public static @Nullable Node configurator(final GateProperty<?> property) {
        final Registry<GateProperty<?>> registry = SGPermissions.propertyRegistry();
        final Identifier id = registry == null ? null : registry.getKey(property);
        return id == null ? null : SGPermissions.configurator(id);
    }

    /** @return the node for changing a gate setting: the cost settings are for admins, the rest for everyone */
    public static Node configurator(final Identifier property) {
        final Registry<GateProperty<?>> registry = SGPermissions.propertyRegistry();
        final GateProperty<?> value = registry == null ? null : registry.getValue(property);
        final boolean costs = value != null && SGStargate.OPERATORS_ONLY.contains(value);
        return SGPermissions.node("configurator." + property.getPath(), costs ? Default.ADMINS : Default.EVERYONE,
                "Changing a gate's " + property.getPath().replace('_', ' '));
    }

    /** @return the node for one of the commands that were for operators, such as {@code sgcraft.command.power} */
    public static Node command(final String path) {
        return SGPermissions.node("command." + path, Default.ADMINS, "The /sgcraft " + path.replace('.', ' ')
                + " command");
    }

    /** @return a command's {@code requires}: a player needs the node; the console and command blocks as operators */
    public static Predicate<CommandSourceStack> commandCheck(final String path) {
        final Node node = SGPermissions.command(path);
        return source -> {
            final ServerPlayer player = source.getPlayer();
            return player != null ? SGPermissions.has(player, node)
                    : source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        };
    }

    /** @return whether a player has a node, as the server's permissions system says, or by its default */
    public static boolean has(final Player player, final Node node) {
        if (player instanceof final ServerPlayer server) {
            return Platform.get().hasPermission(server, node);
        }
        return SGPermissions.fallback(player, node);
    }

    /** @return whether a player has a node; if not, they are told so over their hotbar */
    public static boolean check(final ServerPlayer player, final Node node) {
        if (SGPermissions.has(player, node)) {
            return true;
        }
        player.sendOverlayMessage(Component.translatableWithFallback("message.sgcraft.no_permission",
                "You don't have permission for that (%s)", node.name()).withStyle(ChatFormatting.RED));
        return false;
    }

    /** @return whether a player has a node the permissions system says nothing of */
    public static boolean fallback(final Player player, final Node node) {
        return switch (node.fallback()) {
            case EVERYONE -> true;
            case ADMINS -> SGPermissions.isAdmin(player);
            case OPERATORS -> SGPermissions.isOperator(player);
        };
    }

    /** @return whether a player is an SGCraft admin, who may do anything with any gate: {@link #ADMIN} */
    public static boolean isAdmin(final Player player) {
        return SGPermissions.has(player, SGPermissions.ADMIN);
    }

    /** @return whether a player is one of the server's operators */
    public static boolean isOperator(final Player player) {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }
}
