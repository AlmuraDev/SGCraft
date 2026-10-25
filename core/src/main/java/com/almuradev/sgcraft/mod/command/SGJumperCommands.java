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

import com.almuradev.sgcraft.mod.entity.JumperSettings;
import com.almuradev.sgcraft.mod.entity.PuddleJumperEntity;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * {@code /sgcraft jumper ...}: a puddle jumper's cloak generator, for an admin, without sitting at its helm. Jumpers are
 * picked with an entity selector, such as {@code @e[type=sgcraft:puddle_jumper,sort=nearest,limit=1]}; any other
 * entities it picks are passed over.
 * <ul>
 *     <li>{@code jumper status <jumpers>}: how full each one's generator is, and whether its shield is up, knocked
 *     down or lowered, and its cloak on or off</li>
 *     <li>{@code jumper shield <jumpers> <true|false>}: raises or lowers their shields, as their pilot would</li>
 *     <li>{@code jumper cloak <jumpers> <true|false>}: puts their cloaks on or takes them off, as their pilot would</li>
 *     <li>{@code jumper generator <jumpers> <ge>}: sets how much gate energy their generators hold</li>
 *     <li>{@code jumper recover <jumpers>}: lets knocked down shields be raised again at once</li>
 * </ul>
 * The settings the generator runs by are under {@code /sgcraft settings jumper}.
 */
final class SGJumperCommands {

    private static final SimpleCommandExceptionType NO_JUMPER = new SimpleCommandExceptionType(
            SGCommands.text("jumper.none", "No puddle jumper was picked"));

    private SGJumperCommands() {
    }

    static ArgumentBuilder<CommandSourceStack, ?> jumper() {
        return Commands.literal("jumper")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("status").then(SGJumperCommands.jumpers()
                        .executes(SGJumperCommands::status)))
                .then(Commands.literal("shield").then(SGJumperCommands.jumpers()
                        .then(Commands.argument("up", BoolArgumentType.bool())
                                .executes(context -> SGJumperCommands.each(context, jumper -> jumper.setShield(
                                        BoolArgumentType.getBool(context, "up")))))))
                .then(Commands.literal("cloak").then(SGJumperCommands.jumpers()
                        .then(Commands.argument("on", BoolArgumentType.bool())
                                .executes(context -> SGJumperCommands.each(context, jumper -> jumper.setCloak(
                                        BoolArgumentType.getBool(context, "on")))))))
                .then(Commands.literal("generator").then(SGJumperCommands.jumpers()
                        .then(Commands.argument("ge", DoubleArgumentType.doubleArg(0))
                                .executes(context -> SGJumperCommands.each(context, jumper -> {
                                    jumper.setGeneratorEnergy(DoubleArgumentType.getDouble(context, "ge"));
                                    return null;
                                })))))
                .then(Commands.literal("recover").then(SGJumperCommands.jumpers()
                        .executes(context -> SGJumperCommands.each(context, jumper -> {
                            jumper.clearKnockdown();
                            return null;
                        }))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ?> jumpers() {
        return Commands.argument("jumpers", EntityArgument.entities());
    }

    // The jumpers among the entities picked
    private static List<PuddleJumperEntity> picked(final CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        final List<PuddleJumperEntity> jumpers = new ArrayList<>();
        for (final Entity entity : EntityArgument.getEntities(context, "jumpers")) {
            if (entity instanceof final PuddleJumperEntity jumper) {
                jumpers.add(jumper);
            }
        }
        if (jumpers.isEmpty()) {
            throw SGJumperCommands.NO_JUMPER.create();
        }
        return jumpers;
    }

    // Does something to each jumper picked, which says why it couldn't, if it couldn't; then reports each as it is now
    private static int each(final CommandContext<CommandSourceStack> context,
            final Function<PuddleJumperEntity, Component> action) throws CommandSyntaxException {
        final List<PuddleJumperEntity> jumpers = SGJumperCommands.picked(context);
        int done = 0;
        for (final PuddleJumperEntity jumper : jumpers) {
            final Component refusal = action.apply(jumper);
            if (refusal != null) {
                context.getSource().sendFailure(SGCommands.text("jumper.refused", "%s: %s", jumper.getDisplayName(),
                        refusal));
            } else {
                done++;
                context.getSource().sendSuccess(() -> SGJumperCommands.describe(jumper), true);
            }
        }
        return done;
    }

    private static int status(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final List<PuddleJumperEntity> jumpers = SGJumperCommands.picked(context);
        for (final PuddleJumperEntity jumper : jumpers) {
            context.getSource().sendSuccess(() -> SGJumperCommands.describe(jumper), false);
        }
        return jumpers.size();
    }

    // A jumper's generator, shield and cloak, in a line
    private static Component describe(final PuddleJumperEntity jumper) {
        final long down = jumper.shieldDownTicks();
        final Component shield = jumper.shieldUp() ? SGCommands.text("jumper.shield.up", "up")
                : down > 0 ? SGCommands.text("jumper.shield.knocked_down", "knocked down for %s more seconds",
                        (down + 19) / 20)
                : SGCommands.text("jumper.shield.down", "down");
        final Component cloak = jumper.cloaked() ? SGCommands.text("jumper.cloak.on", "on")
                : SGCommands.text("jumper.cloak.off", "off");
        return SGCommands.text("jumper.status", "%s: generator %s of %s GE, shield %s, cloak %s",
                jumper.getDisplayName(), SGJumperCommands.ge(jumper.generatorEnergy()),
                SGJumperCommands.ge(JumperSettings.current().generatorCapacity()), shield, cloak);
    }

    // Gate energy as a command shows it: whole, or to three places when it is a fraction
    private static String ge(final double energy) {
        return BigDecimal.valueOf(Math.round(energy * 1000) / 1000.0).stripTrailingZeros().toPlainString();
    }
}
