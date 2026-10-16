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

import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.zpm.ZpmItem;
import com.almuradev.sgcraft.mod.zpm.ZpmSettings;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * ZPMs, without finding one: the server's ZPM settings, and ZPMs for operators.
 *
 * <ul>
 *     <li>{@code /sgcraft zpm} shows the settings; {@code /sgcraft zpm <name> <value>} sets one, such as
 *     {@code capacity}, {@code search_range} or
 *     {@code chest_chance}.</li>
 *     <li>{@code /sgcraft zpm give [tainted]} gives the caller a full ZPM, and {@code /sgcraft zpm energy <ge>}
 *     sets the energy of the ZPM they hold.</li>
 * </ul>
 */
final class SGZpmCommands {

    private static final List<String> NUMBERS = List.of("capacity",
            "search_range", "tainted_explosion", "chest_chance", "tainted_chance");

    private SGZpmCommands() {
    }

    static ArgumentBuilder<CommandSourceStack, ?> zpm() {
        final ArgumentBuilder<CommandSourceStack, ?> zpm = Commands.literal("zpm")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> SGZpmCommands.show(context.getSource()))
                .then(Commands.literal("give")
                        .executes(context -> SGZpmCommands.give(context.getSource(), false))
                        .then(Commands.literal("tainted")
                                .executes(context -> SGZpmCommands.give(context.getSource(), true))))
                .then(Commands.literal("energy")
                        .then(Commands.argument("ge", DoubleArgumentType.doubleArg(0))
                                .executes(context -> SGZpmCommands.energy(context.getSource(),
                                        DoubleArgumentType.getDouble(context, "ge")))));
        for (final String name : SGZpmCommands.NUMBERS) {
            zpm.then(Commands.literal(name).then(Commands.argument("value", DoubleArgumentType.doubleArg(0))
                    .executes(context -> SGZpmCommands.set(context.getSource(), name,
                            DoubleArgumentType.getDouble(context, "value")))));
        }
        return zpm;
    }

    private static int show(final CommandSourceStack source) {
        final ZpmSettings settings = ZpmSettings.current();
        source.sendSuccess(() -> SGCommands.text("zpm.header", "The server's ZPM settings:"), false);
        source.sendSuccess(() -> SGZpmCommands.line("capacity", settings.capacity()), false);
        source.sendSuccess(() -> SGZpmCommands.line("search_range", settings.searchRange()), false);
        source.sendSuccess(() -> SGZpmCommands.line("tainted_explosion", settings.taintedExplosion()), false);
        source.sendSuccess(() -> SGZpmCommands.line("chest_chance", settings.chestChance()), false);
        source.sendSuccess(() -> SGZpmCommands.line("tainted_chance", settings.taintedChance()), false);
        return 1;
    }

    private static int set(final CommandSourceStack source, final String name, final double value) {
        final boolean saved = SGServer.require().stargates().setZpm(ZpmSettings.current().with(name, value));
        SGZpmCommands.show(source);
        SGZpmCommands.reportSaved(source, saved);
        return 1;
    }

    private static int give(final CommandSourceStack source, final boolean tainted) throws CommandSyntaxException {
        final ServerPlayer player = source.getPlayerOrException();
        player.getInventory().placeItemBackInInventory(ZpmItem.full(tainted), Prediction.SERVER_ONLY);
        source.sendSuccess(() -> tainted ? SGCommands.text("zpm.given_tainted", "Gave a tainted ZPM")
                : SGCommands.text("zpm.given", "Gave a full ZPM"), true);
        return 1;
    }

    private static int energy(final CommandSourceStack source, final double ge) throws CommandSyntaxException {
        final ItemStack held = source.getPlayerOrException().getItemInHand(InteractionHand.MAIN_HAND);
        if (!ZpmItem.is(held)) {
            source.sendFailure(SGCommands.text("zpm.not_held", "Hold a ZPM in your main hand"));
            return 0;
        }
        ZpmItem.setEnergy(held, ge);
        source.sendSuccess(() -> SGCommands.text("zpm.energy", "The ZPM holds %s GE",
                NumberFormat.getIntegerInstance(Locale.US).format(ZpmItem.energy(held))), true);
        return 1;
    }

    private static Component line(final String name, final Object value) {
        return Component.literal("  ").append(Component.literal(name).withStyle(ChatFormatting.AQUA))
                .append(" = " + value);
    }

    private static void reportSaved(final CommandSourceStack source, final boolean saved) {
        if (!saved) {
            source.sendFailure(SGCommands.text("settings.not_saved",
                    "The change applies until the server stops, but the settings file couldn't be written"));
        }
    }
}
