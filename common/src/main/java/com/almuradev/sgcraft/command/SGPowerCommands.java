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

package com.almuradev.sgcraft.command;

import com.almuradev.sgcraft.block.entity.PowerUnitBlockEntity;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /sgcraft power [<pos>]}: the charge of the power unit there, or of the one the caller is looking at, in FE;
 * {@code set <fe>} sets it, kept within the unit's capacity.
 */
final class SGPowerCommands {

    private static final int LOOK_BLOCKS = 8;
    private static final SimpleCommandExceptionType NOT_LOOKING = new SimpleCommandExceptionType(
            SGCommands.text("power.not_looking", "Look at a power unit within %s blocks, or give its position",
                    SGPowerCommands.LOOK_BLOCKS));
    private static final SimpleCommandExceptionType NOT_A_UNIT = new SimpleCommandExceptionType(
            SGCommands.text("power.not_a_unit", "There is no power unit there"));

    private SGPowerCommands() {
    }

    static ArgumentBuilder<CommandSourceStack, ?> power() {
        return Commands.literal("power")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> SGPowerCommands.show(context.getSource(),
                        SGPowerCommands.lookedAt(context.getSource())))
                .then(SGPowerCommands.set(context -> SGPowerCommands.lookedAt(context.getSource())))
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(context -> SGPowerCommands.show(context.getSource(), SGPowerCommands.unit(context)))
                        .then(SGPowerCommands.set(SGPowerCommands::unit)));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> set(final UnitFinder finder) {
        return Commands.literal("set")
                .then(Commands.argument("fe", LongArgumentType.longArg(0))
                        .executes(context -> SGPowerCommands.set(context.getSource(), finder.find(context),
                                LongArgumentType.getLong(context, "fe"))));
    }

    private static PowerUnitBlockEntity unit(final CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        return SGPowerCommands.unitAt(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"));
    }

    // The block the caller is looking at, if it's a power unit
    private static PowerUnitBlockEntity lookedAt(final CommandSourceStack source) throws CommandSyntaxException {
        final Entity entity = source.getEntity();
        if (entity == null) {
            throw SGPowerCommands.NOT_LOOKING.create();
        }
        final Vec3 eye = entity.getEyePosition();
        final Vec3 far = eye.add(entity.getViewVector(1.0F).scale(SGPowerCommands.LOOK_BLOCKS));
        final BlockHitResult hit = source.getLevel().clip(new ClipContext(eye, far, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, entity));
        if (hit.getType() == HitResult.Type.MISS) {
            throw SGPowerCommands.NOT_LOOKING.create();
        }
        return SGPowerCommands.unitAt(source, hit.getBlockPos());
    }

    private static PowerUnitBlockEntity unitAt(final CommandSourceStack source, final BlockPos pos)
            throws CommandSyntaxException {
        if (source.getLevel().getBlockEntity(pos) instanceof PowerUnitBlockEntity unit) {
            return unit;
        }
        throw SGPowerCommands.NOT_A_UNIT.create();
    }

    private static int show(final CommandSourceStack source, final PowerUnitBlockEntity unit) {
        source.sendSuccess(() -> SGCommands.text("power.show", "The power unit at %s holds %s of %s FE, %s gate energy",
                unit.getBlockPos().toShortString(), unit.stored(), PowerUnitBlockEntity.capacity(),
                unit.available()), false);
        return (int) Math.min(Integer.MAX_VALUE, unit.stored());
    }

    private static int set(final CommandSourceStack source, final PowerUnitBlockEntity unit, final long fe) {
        unit.setStored(fe);
        source.sendSuccess(() -> SGCommands.text("power.set", "The power unit at %s now holds %s FE",
                unit.getBlockPos().toShortString(), unit.stored()), true);
        return 1;
    }

    @FunctionalInterface
    private interface UnitFinder {

        PowerUnitBlockEntity find(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException;
    }
}
