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

package com.almuradev.sgcraft.mod.block.entity;

import com.almuradev.sgcraft.mod.Platform;
import com.almuradev.sgcraft.mod.SGConfig;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.block.EnergyConverterBlock;
import com.almuradev.sgcraft.mod.energy.FeRate;
import com.almuradev.sgcraft.mod.energy.GeStore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An energy converter's place in the world. It holds nothing: each tick it takes what gate energy it can, up to the
 * server's {@code energy.converter_rate}, from the store at its input, and gives the block at its output the FE that
 * is worth, taking only as much as that block took.
 */
public final class EnergyConverterBlockEntity extends BlockEntity {

    public EnergyConverterBlockEntity(final BlockPos pos, final BlockState state) {
        super(SGBlockEntityTypes.ENERGY_CONVERTER, pos, state);
    }

    /** @return the most gate energy a converter turns into FE each tick, as the server's settings say */
    public static double rate() {
        return SGServer.current().map(server -> server.config().converterRate())
                .orElse(SGConfig.DEFAULTS.converterRate());
    }

    /** Turns the input's gate energy into FE for the output, as much as the output takes. */
    public static void serverTick(final ServerLevel level, final BlockPos pos, final BlockState state) {
        final Direction output = EnergyConverterBlock.output(state);
        if (!(level.getBlockEntity(pos.relative(output.getOpposite())) instanceof final GeStore store)) {
            return;
        }
        final double most = Math.min(EnergyConverterBlockEntity.rate(), store.givable());
        final long offer = FeRate.toFeOut(most);
        if (offer <= 0) {
            return;
        }
        final long taken = Platform.get().offerEnergy(level, pos.relative(output), output.getOpposite(), offer);
        if (taken > 0) {
            store.give(Math.min(most, FeRate.toGeOut(taken)));
        }
    }
}
