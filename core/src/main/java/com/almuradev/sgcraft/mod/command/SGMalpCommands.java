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
import com.almuradev.sgcraft.mod.block.MalpConsoleBlock;
import com.almuradev.sgcraft.mod.entity.MalpEntity;
import com.almuradev.sgcraft.mod.item.BatteryItem;
import com.almuradev.sgcraft.mod.item.MalpTabletItem;
import com.almuradev.sgcraft.mod.item.SGItems;
import com.almuradev.sgcraft.mod.malp.MalpConfigurator;
import com.almuradev.sgcraft.mod.malp.MalpLinks;
import com.almuradev.sgcraft.mod.malp.MalpRegistry;
import com.almuradev.sgcraft.mod.malp.MalpSettings;
import com.almuradev.sgcraft.mod.malp.MalpSignal;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * {@code /sgcraft malp ...} and {@code /sgcraft battery ...}: everything about MALPs an admin might need without a
 * console or tablet. MALPs are named by their name or the start of their UUID, as {@code list} shows them.
 * <ul>
 *     <li>{@code malp list}: every MALP, where it was last seen, its battery or that it needs no power, its console
 *     and its owner</li>
 *     <li>{@code malp pair <malp> <pos>} and {@code malp unpair <malp>}: pairs a MALP with the console at a
 *     position in the caller's world, or with none</li>
 *     <li>{@code malp battery <malp> <ge>}: sets the charge of a MALP's battery, in gate energy, giving it one if it
 *     has none</li>
 *     <li>{@code malp power_required <malp> [<true|false>]}: whether a MALP runs on its battery, or sets it; one
 *     that doesn't uses no gate energy and works without a battery</li>
 *     <li>{@code malp poweroff <malp>}: powers down the feed of everyone watching a MALP</li>
 *     <li>{@code malp recall <malp>}: brings a MALP to the caller</li>
 *     <li>{@code malp center <malp>}: turns a MALP's camera back to face the way it drives</li>
 *     <li>{@code malp signal <malp> [<pos>]}: how its console, or the console at a position, reaches a MALP</li>
 *     <li>{@code malp console_damage_interrupts [<true|false>]}: whether a player at a console who takes damage is
 *     taken off it, or sets it</li>
 *     <li>{@code malp console_spectators [<count>]}: how many of the players near a console someone is driving at,
 *     nearest first, see the feed on its monitor, or sets it; 0 shows it to none</li>
 *     <li>{@code malp spectator_range [<blocks>]}: how near a driven console players see the feed on its monitor,
 *     or sets it</li>
 *     <li>{@code malp tablet_drain [<ge>]}: how much gate energy a MALP tablet uses from its battery each tick while it
 *     shows a live feed, or sets it</li>
 *     <li>{@code malp pair_tablet <malp>} and {@code malp unpair_tablet <malp>}: adds a MALP to, or takes it off,
 *     the MALP tablet in the caller's hand, whoever the MALP belongs to</li>
 *     <li>{@code battery} and {@code battery set <ge>}: the charge of the battery in the caller's hand, in gate
 *     energy</li>
 * </ul>
 * A command about a MALP whose chunk isn't loaded loads it and asks to be run again.
 */
final class SGMalpCommands {

    private static final DynamicCommandExceptionType NO_MALP = new DynamicCommandExceptionType(name ->
            SGCommands.text("malp.no_malp", "There is no MALP called %s", name));
    private static final DynamicCommandExceptionType NOT_LOADED = new DynamicCommandExceptionType(name ->
            SGCommands.text("malp.not_loaded", "%s isn't loaded. Its chunk is loading now, so run the command "
                    + "again in a moment", name));
    private static final SimpleCommandExceptionType NOT_A_CONSOLE = new SimpleCommandExceptionType(
            SGCommands.text("malp.not_a_console", "There is no MALP console there"));
    private static final SimpleCommandExceptionType NO_BATTERY = new SimpleCommandExceptionType(
            SGCommands.text("battery.not_held", "You aren't holding a battery"));
    private static final SimpleCommandExceptionType NO_TABLET = new SimpleCommandExceptionType(
            SGCommands.text("malp.tablet.not_held", "You aren't holding a MALP tablet"));

    private SGMalpCommands() {
    }

    static ArgumentBuilder<CommandSourceStack, ?> malp() {
        return Commands.literal("malp")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("list").executes(context -> SGMalpCommands.list(context.getSource())))
                .then(Commands.literal("pair").then(SGMalpCommands.malpArgument()
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> SGMalpCommands.pair(context, true)))))
                .then(Commands.literal("unpair").then(SGMalpCommands.malpArgument()
                        .executes(context -> SGMalpCommands.pair(context, false))))
                .then(Commands.literal("battery").then(SGMalpCommands.malpArgument()
                        .then(Commands.argument("ge", DoubleArgumentType.doubleArg(0))
                                .executes(SGMalpCommands::battery))))
                .then(Commands.literal("power_required").then(SGMalpCommands.malpArgument()
                        .executes(SGMalpCommands::showPowerRequired)
                        .then(Commands.argument("required", BoolArgumentType.bool())
                                .executes(SGMalpCommands::setPowerRequired))))
                .then(Commands.literal("poweroff").then(SGMalpCommands.malpArgument()
                        .executes(SGMalpCommands::powerOff)))
                .then(Commands.literal("recall").then(SGMalpCommands.malpArgument()
                        .executes(SGMalpCommands::recall)))
                .then(Commands.literal("center").then(SGMalpCommands.malpArgument()
                        .executes(SGMalpCommands::center)))
                .then(Commands.literal("signal").then(SGMalpCommands.malpArgument()
                        .executes(context -> SGMalpCommands.signal(context, false))
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> SGMalpCommands.signal(context, true)))))
                .then(Commands.literal("console_damage_interrupts")
                        .executes(context -> SGMalpCommands.showDamageInterrupts(context.getSource()))
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(context -> SGMalpCommands.setDamageInterrupts(context.getSource(),
                                        BoolArgumentType.getBool(context, "value")))))
                .then(Commands.literal("console_spectators")
                        .executes(context -> SGMalpCommands.showSpectators(context.getSource()))
                        .then(Commands.argument("count", IntegerArgumentType.integer(0, 64))
                                .executes(context -> SGMalpCommands.setSpectators(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "count")))))
                .then(Commands.literal("spectator_range")
                        .executes(context -> SGMalpCommands.showSpectatorRange(context.getSource()))
                        .then(Commands.argument("blocks", IntegerArgumentType.integer(1, 64))
                                .executes(context -> SGMalpCommands.setSpectatorRange(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "blocks")))))
                .then(Commands.literal("tablet_drain")
                        .executes(context -> SGMalpCommands.showTabletDrain(context.getSource()))
                        .then(Commands.argument("ge", DoubleArgumentType.doubleArg(0))
                                .executes(context -> SGMalpCommands.setTabletDrain(context.getSource(),
                                        DoubleArgumentType.getDouble(context, "ge")))))
                .then(Commands.literal("pair_tablet").then(SGMalpCommands.malpArgument()
                        .executes(context -> SGMalpCommands.pairTablet(context, true))))
                .then(Commands.literal("unpair_tablet").then(SGMalpCommands.malpArgument()
                        .executes(context -> SGMalpCommands.pairTablet(context, false))));
    }

    private static int showTabletDrain(final CommandSourceStack source) {
        final double drain = MalpSettings.current().tabletDrain();
        source.sendSuccess(() -> SGCommands.text("malp.tablet_drain.show",
                "A MALP tablet uses %s GE a tick while it shows a live feed", SGMalpCommands.ge(drain)), false);
        return 1;
    }

    private static int setTabletDrain(final CommandSourceStack source, final double drain) {
        final boolean saved = SGServer.require().stargates().setTabletDrain(drain);
        source.sendSuccess(() -> SGCommands.text("malp.tablet_drain.set",
                "A MALP tablet now uses %s GE a tick while it shows a live feed", SGMalpCommands.ge(drain)), true);
        if (!saved) {
            source.sendFailure(SGCommands.text("settings.not_saved",
                    "The settings file couldn't be written, so the change lasts only until the server stops"));
        }
        return 1;
    }

    // Gate energy as a command shows it: whole, or to three places when it is a fraction
    private static String ge(final double energy) {
        return BigDecimal.valueOf(Math.round(energy * 1000) / 1000.0).stripTrailingZeros().toPlainString();
    }

    // Adds a MALP to the tablet in the caller's hand, or takes it off, whoever the MALP belongs to
    private static int pairTablet(final CommandContext<CommandSourceStack> context, final boolean pair)
            throws CommandSyntaxException {
        final MalpRegistry.Entry entry = SGMalpCommands.entry(context);
        final ItemStack tablet = SGMalpCommands.heldTablet(context.getSource());
        final CommandSourceStack source = context.getSource();
        if (pair) {
            if (!MalpTabletItem.pair(tablet, entry.id())) {
                source.sendFailure(SGCommands.text("malp.tablet.already_paired",
                        "%s is already paired with your tablet", entry.name()));
                return 0;
            }
            source.sendSuccess(() -> SGCommands.text("malp.tablet.paired", "%s is paired with your tablet",
                    entry.name()), true);
            return 1;
        }
        if (!MalpTabletItem.unpair(tablet, entry.id())) {
            source.sendFailure(SGCommands.text("malp.tablet.not_paired", "%s isn't paired with your tablet",
                    entry.name()));
            return 0;
        }
        source.sendSuccess(() -> SGCommands.text("malp.tablet.unpaired", "%s is no longer paired with your tablet",
                entry.name()), true);
        return 1;
    }

    // The MALP tablet in the caller's main hand, or else in their off hand
    private static ItemStack heldTablet(final CommandSourceStack source) throws CommandSyntaxException {
        final ServerPlayer player = source.getPlayerOrException();
        for (final InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).is(SGItems.MALP_TABLET)) {
                return player.getItemInHand(hand);
            }
        }
        throw SGMalpCommands.NO_TABLET.create();
    }

    private static int showSpectators(final CommandSourceStack source) {
        final int count = MalpSettings.current().consoleSpectators();
        source.sendSuccess(() -> SGCommands.text("malp.console_spectators.show",
                "Up to %s players near a driven MALP console see the feed on its monitor", count), false);
        return count;
    }

    private static int setSpectators(final CommandSourceStack source, final int count) {
        final boolean saved = SGServer.require().stargates().setConsoleSpectators(count);
        source.sendSuccess(() -> SGCommands.text("malp.console_spectators.set",
                "Up to %s players near a driven MALP console now see the feed on its monitor", count), true);
        if (!saved) {
            source.sendFailure(SGCommands.text("settings.not_saved",
                    "The settings file couldn't be written, so the change lasts only until the server stops"));
        }
        return count;
    }

    private static int showSpectatorRange(final CommandSourceStack source) {
        final int range = MalpSettings.current().spectatorRange();
        source.sendSuccess(() -> SGCommands.text("malp.spectator_range.show",
                "Players within %s blocks of a driven MALP console see the feed on its monitor", range), false);
        return range;
    }

    private static int setSpectatorRange(final CommandSourceStack source, final int range) {
        final boolean saved = SGServer.require().stargates().setSpectatorRange(range);
        source.sendSuccess(() -> SGCommands.text("malp.spectator_range.set",
                "Players within %s blocks of a driven MALP console now see the feed on its monitor", range), true);
        if (!saved) {
            source.sendFailure(SGCommands.text("settings.not_saved",
                    "The settings file couldn't be written, so the change lasts only until the server stops"));
        }
        return range;
    }

    private static int showDamageInterrupts(final CommandSourceStack source) {
        final boolean on = MalpSettings.current().consoleDamageInterrupts();
        source.sendSuccess(() -> on
                ? SGCommands.text("malp.console_damage_interrupts.on",
                        "Damage takes a player at a MALP console off it")
                : SGCommands.text("malp.console_damage_interrupts.off",
                        "Damage doesn't take a player at a MALP console off it"), false);
        return on ? 1 : 0;
    }

    private static int setDamageInterrupts(final CommandSourceStack source, final boolean on) {
        final boolean saved = SGServer.require().stargates().setConsoleDamageInterrupts(on);
        source.sendSuccess(() -> on
                ? SGCommands.text("malp.console_damage_interrupts.set_on",
                        "Damage now takes a player at a MALP console off it")
                : SGCommands.text("malp.console_damage_interrupts.set_off",
                        "Damage no longer takes a player at a MALP console off it"), true);
        if (!saved) {
            source.sendFailure(SGCommands.text("settings.not_saved",
                    "The settings file couldn't be written, so the change lasts only until the server stops"));
        }
        return 1;
    }

    static ArgumentBuilder<CommandSourceStack, ?> battery() {
        return Commands.literal("battery")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> SGMalpCommands.showBattery(context.getSource()))
                .then(Commands.literal("set").then(Commands.argument("ge", DoubleArgumentType.doubleArg(0))
                        .executes(context -> SGMalpCommands.setBattery(context.getSource(),
                                DoubleArgumentType.getDouble(context, "ge")))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> malpArgument() {
        return Commands.argument("malp", StringArgumentType.string()).suggests((context, builder) ->
                SharedSuggestionProvider.suggest(MalpRegistry.get(context.getSource().getServer()).all().stream()
                        .map(entry -> StringArgumentType.escapeIfRequired(entry.name())), builder));
    }

    private static MalpRegistry.Entry entry(final CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        final String name = StringArgumentType.getString(context, "malp");
        return MalpRegistry.get(context.getSource().getServer()).byName(name)
                .orElseThrow(() -> SGMalpCommands.NO_MALP.create(name));
    }

    // The MALP itself, which must be loaded; one that isn't is loaded for next time
    private static MalpEntity loaded(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final MalpRegistry.Entry entry = SGMalpCommands.entry(context);
        final MalpEntity malp = MalpRegistry.find(context.getSource().getServer(), entry.id());
        if (malp == null) {
            MalpLinks.load(context.getSource().getServer(), entry.id());
            throw SGMalpCommands.NOT_LOADED.create(entry.name());
        }
        return malp;
    }

    private static int list(final CommandSourceStack source) {
        final List<MalpRegistry.Entry> entries = MalpRegistry.get(source.getServer()).all();
        source.sendSuccess(() -> SGCommands.text("malp.list.header", "%s MALPs", entries.size()), false);
        for (final MalpRegistry.Entry entry : entries) {
            final long capacity = BatteryItem.capacity();
            final long percent = capacity <= 0 ? 0 : Math.round(100.0 * entry.energy() / capacity);
            final Component power = entry.options().powerRequired()
                    ? SGCommands.text("malp.list.battery", "battery %s%%", percent)
                    : SGCommands.text("malp.list.no_power", "needs no power");
            final Component console = entry.console()
                    .map(pos -> (Component) SGCommands.text("malp.list.console", "console at %s in %s",
                            pos.pos().toShortString(), pos.dimension().identifier().toString()))
                    .orElseGet(() -> SGCommands.text("malp.list.no_console", "no console"));
            final Component owner = entry.owner()
                    .map(id -> (Component) SGCommands.text("malp.list.owner", "owned by %s",
                            MalpConfigurator.ownerName(source.getServer(), id)))
                    .orElseGet(() -> SGCommands.text("malp.list.no_owner", "no owner"));
            source.sendSuccess(() -> SGCommands.text("malp.list.entry", "  %s at %s in %s, %s, %s, %s",
                    entry.name(), entry.position().pos().toShortString(),
                    entry.position().dimension().identifier().toString(), power, console, owner), false);
        }
        return entries.size();
    }

    private static int pair(final CommandContext<CommandSourceStack> context, final boolean pair)
            throws CommandSyntaxException {
        final MalpEntity malp = SGMalpCommands.loaded(context);
        if (!pair) {
            MalpLinks.unpair(malp);
            context.getSource().sendSuccess(() -> SGCommands.text("malp.unpaired", "%s is no longer paired",
                    malp.displayName()), true);
            return 1;
        }
        final BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        if (!(context.getSource().getLevel().getBlockState(pos).getBlock() instanceof MalpConsoleBlock)) {
            throw SGMalpCommands.NOT_A_CONSOLE.create();
        }
        malp.setConsole(GlobalPos.of(context.getSource().getLevel().dimension(), pos));
        context.getSource().sendSuccess(() -> SGCommands.text("malp.paired", "%s is paired with the console at %s",
                malp.displayName(), pos.toShortString()), true);
        return 1;
    }

    private static int battery(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final MalpEntity malp = SGMalpCommands.loaded(context);
        if (malp.battery().isEmpty()) {
            malp.setBattery(new ItemStack(SGItems.BATTERY));
        }
        final ItemStack battery = malp.battery();
        BatteryItem.setEnergy(battery, DoubleArgumentType.getDouble(context, "ge"));
        malp.batterySlot().setChanged();
        malp.record();
        context.getSource().sendSuccess(() -> SGCommands.text("malp.battery", "%s's battery holds %s of %s GE",
                malp.displayName(), SGMalpCommands.ge(BatteryItem.energy(battery)), BatteryItem.capacity()), true);
        return 1;
    }

    private static int showPowerRequired(final CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        final MalpRegistry.Entry entry = SGMalpCommands.entry(context);
        final boolean required = entry.options().powerRequired();
        context.getSource().sendSuccess(() -> required
                ? SGCommands.text("malp.power_required.on", "%s runs on its battery", entry.name())
                : SGCommands.text("malp.power_required.off", "%s needs no power", entry.name()), false);
        return required ? 1 : 0;
    }

    private static int setPowerRequired(final CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        final MalpEntity malp = SGMalpCommands.loaded(context);
        final boolean required = BoolArgumentType.getBool(context, "required");
        malp.setOptions(malp.options().withPowerRequired(required));
        context.getSource().sendSuccess(() -> required
                ? SGCommands.text("malp.power_required.set_on", "%s now runs on its battery", malp.displayName())
                : SGCommands.text("malp.power_required.set_off", "%s now needs no power", malp.displayName()), true);
        return 1;
    }

    private static int powerOff(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final MalpRegistry.Entry entry = SGMalpCommands.entry(context);
        final int count = MalpLinks.powerDown(entry.id());
        context.getSource().sendSuccess(() -> SGCommands.text("malp.poweroff", "Powered down %s feeds of %s", count,
                entry.name()), true);
        return count;
    }

    private static int recall(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final MalpEntity malp = SGMalpCommands.loaded(context);
        final CommandSourceStack source = context.getSource();
        final Vec3 to = source.getPosition();
        malp.stop();
        malp.teleportTo(source.getLevel(), to.x, to.y, to.z, Set.<Relative>of(), malp.getYRot(), malp.getXRot(),
                true);
        source.sendSuccess(() -> SGCommands.text("malp.recalled", "Brought %s to %s", malp.displayName(),
                BlockPos.containing(to).toShortString()), true);
        return 1;
    }

    private static int center(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final MalpEntity malp = SGMalpCommands.loaded(context);
        malp.centerMast();
        context.getSource().sendSuccess(() -> SGCommands.text("malp.centered", "%s's camera faces ahead again",
                malp.displayName()), true);
        return 1;
    }

    private static int signal(final CommandContext<CommandSourceStack> context, final boolean at)
            throws CommandSyntaxException {
        final MalpRegistry.Entry entry = SGMalpCommands.entry(context);
        final CommandSourceStack source = context.getSource();
        final GlobalPos console = at ? GlobalPos.of(source.getLevel().dimension(),
                BlockPosArgument.getLoadedBlockPos(context, "pos")) : entry.console().orElse(null);
        if (console == null) {
            source.sendFailure(SGCommands.text("malp.signal.no_console", "%s isn't paired with a console",
                    entry.name()));
            return 0;
        }
        final MalpEntity malp = MalpRegistry.find(source.getServer(), entry.id());
        final Vec3 where = malp != null ? malp.position() : Vec3.atBottomCenterOf(entry.position().pos());
        final MalpSignal signal = MalpSignal.between(console, malp != null ? malp.level().dimension()
                : entry.position().dimension(), where, MalpSettings.current().signalRange());
        final Component text = switch (signal.kind()) {
            case DIRECT -> SGCommands.text("malp.signal.direct", "The console at %s reaches %s directly",
                    console.pos().toShortString(), entry.name());
            case RELAYED -> SGCommands.text("malp.signal.relayed",
                    "The console at %s reaches %s through the gates %s", console.pos().toShortString(),
                    entry.name(), MalpLinks.relay(signal));
            case NONE -> SGCommands.text("malp.signal.none", "The console at %s doesn't reach %s",
                    console.pos().toShortString(), entry.name());
        };
        source.sendSuccess(() -> text, false);
        return signal.reaches() ? 1 : 0;
    }

    private static ItemStack heldBattery(final CommandSourceStack source) throws CommandSyntaxException {
        final ServerPlayer player = source.getPlayerOrException();
        final ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!held.is(SGItems.BATTERY)) {
            throw SGMalpCommands.NO_BATTERY.create();
        }
        return held;
    }

    private static int showBattery(final CommandSourceStack source) throws CommandSyntaxException {
        final ItemStack battery = SGMalpCommands.heldBattery(source);
        source.sendSuccess(() -> SGCommands.text("battery.show", "Your battery holds %s of %s GE",
                SGMalpCommands.ge(BatteryItem.energy(battery)), BatteryItem.capacity()), false);
        return (int) Math.min(Integer.MAX_VALUE, (long) BatteryItem.energy(battery));
    }

    private static int setBattery(final CommandSourceStack source, final double ge) throws CommandSyntaxException {
        final ItemStack battery = SGMalpCommands.heldBattery(source);
        BatteryItem.setEnergy(battery, ge);
        source.sendSuccess(() -> SGCommands.text("battery.set", "Your battery now holds %s GE",
                SGMalpCommands.ge(BatteryItem.energy(battery))), true);
        return 1;
    }
}
