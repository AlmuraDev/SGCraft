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

import com.almuradev.sgcraft.mod.rings.RingPlatformBlockEntity;
import com.almuradev.sgcraft.mod.rings.RingSettings;
import com.almuradev.sgcraft.mod.rings.Rings;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;

import java.util.Optional;

/**
 * {@code /sgcraft rings <pos>}: a ring platform's link, energy and transports left; {@code link <pos> <other>} links
 * two platforms in the caller's world, whatever the distance limit; {@code unlink} unlinks one and its partner;
 * {@code start} starts its rings and its partner's, paid for as a panel would; {@code energy <ge>} sets the gate
 * energy it holds.
 */
final class SGRingCommands {

    private static final SimpleCommandExceptionType NOT_A_PLATFORM = new SimpleCommandExceptionType(
            SGCommands.text("rings.not_a_platform", "There is no ring platform there"));
    private static final SimpleCommandExceptionType SAME = new SimpleCommandExceptionType(
            SGCommands.text("rings.same", "A ring platform can't be linked to itself"));
    private static final SimpleCommandExceptionType NOT_LINKED = new SimpleCommandExceptionType(
            SGCommands.text("rings.not_linked", "That ring platform isn't linked to another"));
    private static final SimpleCommandExceptionType BUSY = new SimpleCommandExceptionType(
            SGCommands.text("rings.busy", "Those rings are already running"));
    private static final SimpleCommandExceptionType NO_POWER = new SimpleCommandExceptionType(
            SGCommands.text("rings.no_power", "That ring platform doesn't have the energy, with its naquadah, to send"));

    private SGRingCommands() {
    }

    static ArgumentBuilder<CommandSourceStack, ?> rings() {
        return Commands.literal("rings")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(context -> SGRingCommands.show(context.getSource(),
                                SGRingCommands.platform(context, "pos")))
                        .then(Commands.literal("link")
                                .then(Commands.argument("other", BlockPosArgument.blockPos())
                                        .executes(context -> SGRingCommands.link(context.getSource(),
                                                SGRingCommands.platform(context, "pos"),
                                                SGRingCommands.platform(context, "other")))))
                        .then(Commands.literal("unlink")
                                .executes(context -> SGRingCommands.unlink(context.getSource(),
                                        SGRingCommands.platform(context, "pos"))))
                        .then(Commands.literal("start")
                                .executes(context -> SGRingCommands.start(context.getSource(),
                                        SGRingCommands.platform(context, "pos"))))
                        .then(Commands.literal("energy")
                                .then(Commands.argument("ge", DoubleArgumentType.doubleArg(0))
                                        .executes(context -> SGRingCommands.energy(context.getSource(),
                                                SGRingCommands.platform(context, "pos"),
                                                DoubleArgumentType.getDouble(context, "ge"))))));
    }

    private static RingPlatformBlockEntity platform(final CommandContext<CommandSourceStack> context,
            final String argument) throws CommandSyntaxException {
        final BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, argument);
        return Rings.at(context.getSource().getLevel(), pos).orElseThrow(SGRingCommands.NOT_A_PLATFORM::create);
    }

    private static int show(final CommandSourceStack source, final RingPlatformBlockEntity platform) {
        final String link = platform.linkPos().map(BlockPos::toShortString).orElse("nothing");
        source.sendSuccess(() -> SGCommands.text("rings.show",
                "The ring platform at %s is linked to %s, holds %s of %s GE and %s naquadah, and has %s transports "
                        + "left", platform.getBlockPos().toShortString(), link, platform.energy(),
                platform.energyCapacity(), platform.naquadah(), platform.usesLeft()), false);
        return (int) Math.min(Integer.MAX_VALUE, platform.usesLeft());
    }

    private static int link(final CommandSourceStack source, final RingPlatformBlockEntity platform,
            final RingPlatformBlockEntity other) throws CommandSyntaxException {
        if (platform == other) {
            throw SGRingCommands.SAME.create();
        }
        if (platform.running() || other.running()) {
            throw SGRingCommands.BUSY.create();
        }
        Rings.unlink(platform);
        Rings.unlink(other);
        Rings.linkPair(platform, other);
        source.sendSuccess(() -> SGCommands.text("rings.linked", "Linked the ring platforms at %s and %s",
                platform.getBlockPos().toShortString(), other.getBlockPos().toShortString()), true);
        return 1;
    }

    private static int unlink(final CommandSourceStack source, final RingPlatformBlockEntity platform)
            throws CommandSyntaxException {
        if (platform.linkPos().isEmpty()) {
            throw SGRingCommands.NOT_LINKED.create();
        }
        Rings.unlink(platform);
        source.sendSuccess(() -> SGCommands.text("rings.unlinked", "Unlinked the ring platform at %s",
                platform.getBlockPos().toShortString()), true);
        return 1;
    }

    private static int start(final CommandSourceStack source, final RingPlatformBlockEntity platform)
            throws CommandSyntaxException {
        final Optional<RingPlatformBlockEntity> partner = Rings.partner(platform);
        if (partner.isEmpty()) {
            throw SGRingCommands.NOT_LINKED.create();
        }
        if (platform.running() || partner.get().running()) {
            throw SGRingCommands.BUSY.create();
        }
        if (platform.energyAvailable() < Rings.cost(platform, partner.get())) {
            throw SGRingCommands.NO_POWER.create();
        }
        Rings.start(platform, partner.get());
        source.sendSuccess(() -> SGCommands.text("rings.started", "Started the rings at %s and %s",
                platform.getBlockPos().toShortString(), partner.get().getBlockPos().toShortString()), true);
        return 1;
    }

    private static int energy(final CommandSourceStack source, final RingPlatformBlockEntity platform,
            final double ge) {
        platform.setStored(Math.min(ge, RingSettings.current().capacity()));
        source.sendSuccess(() -> SGCommands.text("rings.energy", "The ring platform at %s now holds %s GE",
                platform.getBlockPos().toShortString(), platform.energy()), true);
        return 1;
    }
}
