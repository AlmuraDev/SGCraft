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
import com.almuradev.sgcraft.mod.shield.CityShieldBlockEntity;
import com.almuradev.sgcraft.mod.shield.ShieldSettings;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.UnaryOperator;

/**
 * The shield settings, shown and changed in place of editing the {@code shields} section of
 * {@code sgcraft-server.json}. A change applies at once and is written to the file.
 *
 * <ul>
 *     <li>{@code /sgcraft shields} shows them.</li>
 *     <li>{@code /sgcraft shields gate_energy_per_tick <energy>} sets how much energy a raised gate shield takes
 *     each tick, and {@code console_range <blocks>} how far shield consoles reach.</li>
 *     <li>{@code /sgcraft shields city_energy_per_million_blocks <energy>}, {@code city_max_radius <blocks>},
 *     {@code city_power_reach <blocks>} and {@code city_keeps_chunks_loaded <true|false>} set the city shields'.</li>
 *     <li>{@code /sgcraft shields city <pos>} shows the city shield generator there; {@code ... raise} and
 *     {@code ... lower} raise and lower its shield, and {@code ... size <width> <above> <below>} sizes it.</li>
 * </ul>
 */
final class SGShieldCommands {

    private static final SimpleCommandExceptionType NO_GENERATOR = new SimpleCommandExceptionType(
            SGCommands.text("shields.city.none", "There is no city shield generator there"));

    private SGShieldCommands() {
    }

    static ArgumentBuilder<CommandSourceStack, ?> shields() {
        return Commands.literal("shields").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> SGShieldCommands.show(context.getSource()))
                .then(Commands.literal("gate_energy_per_tick")
                        .then(Commands.argument("energy", DoubleArgumentType.doubleArg(0))
                                .executes(context -> SGShieldCommands.set(context.getSource(), settings -> settings
                                        .withGateEnergyPerTick(DoubleArgumentType.getDouble(context, "energy"))))))
                .then(Commands.literal("console_range")
                        .then(Commands.argument("blocks", IntegerArgumentType.integer(1, 256))
                                .executes(context -> SGShieldCommands.set(context.getSource(), settings -> settings
                                        .withConsoleRange(IntegerArgumentType.getInteger(context, "blocks"))))))
                .then(Commands.literal("city_energy_per_million_blocks")
                        .then(Commands.argument("energy", DoubleArgumentType.doubleArg(0))
                                .executes(context -> SGShieldCommands.set(context.getSource(), settings -> settings
                                        .withCityEnergyPerMillionBlocks(DoubleArgumentType.getDouble(context,
                                                "energy"))))))
                .then(Commands.literal("city_max_radius")
                        .then(Commands.argument("blocks", IntegerArgumentType.integer(ShieldSettings.CITY_MIN_RADIUS,
                                ShieldSettings.CITY_LIMIT))
                                .executes(context -> SGShieldCommands.set(context.getSource(), settings -> settings
                                        .withCityMaxRadius(IntegerArgumentType.getInteger(context, "blocks"))))))
                .then(Commands.literal("city_power_reach")
                        .then(Commands.argument("blocks", IntegerArgumentType.integer(1, 16))
                                .executes(context -> SGShieldCommands.set(context.getSource(), settings -> settings
                                        .withCityPowerReach(IntegerArgumentType.getInteger(context, "blocks"))))))
                .then(Commands.literal("city_keeps_chunks_loaded")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(context -> SGShieldCommands.set(context.getSource(), settings -> settings
                                        .withCityKeepsChunksLoaded(BoolArgumentType.getBool(context, "value"))))))
                .then(Commands.literal("city").then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(context -> SGShieldCommands.city(context.getSource(),
                                SGShieldCommands.generator(context)))
                        .then(Commands.literal("raise").executes(context -> {
                            final CityShieldBlockEntity shield = SGShieldCommands.generator(context);
                            if (!shield.raise()) {
                                context.getSource().sendFailure(SGCommands.text("shields.city.no_power",
                                        "There isn't enough energy around the generator to raise a shield that size"));
                                return 0;
                            }
                            return SGShieldCommands.city(context.getSource(), shield);
                        }))
                        .then(Commands.literal("lower").executes(context -> {
                            final CityShieldBlockEntity shield = SGShieldCommands.generator(context);
                            shield.lower();
                            return SGShieldCommands.city(context.getSource(), shield);
                        }))
                        .then(Commands.literal("size")
                                .then(Commands.argument("width", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("above", IntegerArgumentType.integer(1))
                                                .then(Commands.argument("below", IntegerArgumentType.integer(1))
                                                        .executes(context -> {
                                                            final CityShieldBlockEntity shield =
                                                                    SGShieldCommands.generator(context);
                                                            shield.resize(
                                                                    IntegerArgumentType.getInteger(context, "width"),
                                                                    IntegerArgumentType.getInteger(context, "above"),
                                                                    IntegerArgumentType.getInteger(context, "below"));
                                                            return SGShieldCommands.city(context.getSource(), shield);
                                                        })))))));
    }

    private static CityShieldBlockEntity generator(final CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        final BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        if (context.getSource().getLevel().getBlockEntity(pos) instanceof final CityShieldBlockEntity shield) {
            return shield;
        }
        throw SGShieldCommands.NO_GENERATOR.create();
    }

    private static int city(final CommandSourceStack source, final CityShieldBlockEntity shield) {
        source.sendSuccess(() -> SGCommands.text("shields.city.state", "The city shield at %s is %s, reaching %s "
                + "all around, %s up and %s down; it takes %s energy a tick, and can draw on %s",
                shield.getBlockPos().toShortString(), shield.active()
                        ? SGCommands.text("shields.city.raised", "raised")
                        : SGCommands.text("shields.city.lowered", "lowered"),
                shield.width(), shield.above(), shield.below(),
                String.format(Locale.ROOT, "%.3f", shield.energyPerTick()), shield.available()), false);
        return 1;
    }

    private static int show(final CommandSourceStack source) {
        final ShieldSettings settings = ShieldSettings.current();
        source.sendSuccess(() -> SGCommands.text("shields.header", "The server's shield settings:"), false);
        source.sendSuccess(() -> SGShieldCommands.line("gate_energy_per_tick", settings.gateEnergyPerTick()), false);
        source.sendSuccess(() -> SGShieldCommands.line("console_range", settings.consoleRange()), false);
        source.sendSuccess(() -> SGShieldCommands.line("city_energy_per_million_blocks",
                settings.cityEnergyPerMillionBlocks()), false);
        source.sendSuccess(() -> SGShieldCommands.line("city_max_radius", settings.cityMaxRadius()), false);
        source.sendSuccess(() -> SGShieldCommands.line("city_power_reach", settings.cityPowerReach()), false);
        source.sendSuccess(() -> SGShieldCommands.line("city_keeps_chunks_loaded", settings.cityKeepsChunksLoaded()),
                false);
        return 1;
    }

    private static int set(final CommandSourceStack source, final UnaryOperator<ShieldSettings> change) {
        final boolean saved = SGServer.require().stargates().setShields(change.apply(ShieldSettings.current()));
        SGShieldCommands.show(source);
        if (!saved) {
            source.sendFailure(SGCommands.text("settings.not_saved",
                    "The settings file couldn't be written, so the change lasts only until the server stops"));
        }
        return 1;
    }

    private static Component line(final String name, final Object value) {
        return Component.literal("  ").append(Component.literal(name).withStyle(ChatFormatting.AQUA))
                .append(" = " + value);
    }
}
