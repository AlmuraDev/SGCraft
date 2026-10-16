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

package com.almuradev.sgcraft.mod.energy;

import com.almuradev.sgcraft.api.SGCraftApi;
import com.almuradev.sgcraft.mod.SGConfig;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.block.entity.PowerUnitBlockEntity;

/**
 * Where FE meets gate energy (GE). Everything inside SGCraft holds and spends GE; FE is only what other mods' cables
 * and machines see. FE coming in, from cables and other mods' chargers, buys GE at the server's
 * {@code energy.bridge_rates.fe_in_per_ge}, the {@link PowerUnitBlockEntity#FORGE_ENERGY} bridge's rate; GE going out
 * as FE, from an energy converter or a battery other mods draw on, gives {@code energy.bridge_rates.fe_out_per_ge}.
 * Both are 80 FE to one GE unless set.
 */
public final class FeRate {

    private FeRate() {
    }

    /** @return how many FE buy one GE coming in */
    public static double perGeIn() {
        final double rate = SGCraftApi.get().energy().rate(PowerUnitBlockEntity.FORGE_ENERGY);
        return rate > 0 ? 1 / rate : Double.POSITIVE_INFINITY;
    }

    /** @return how many FE one GE gives going out */
    public static double perGeOut() {
        return SGServer.current().map(server -> server.config().fePerGeOut()).orElse(SGConfig.DEFAULTS.fePerGeOut());
    }

    /** @return the whole FE an amount of GE is worth coming in, rounded down */
    public static long toFeIn(final double ge) {
        return FeRate.whole(ge * FeRate.perGeIn());
    }

    /** @return the GE an amount of FE buys coming in */
    public static double toGeIn(final double fe) {
        return fe / FeRate.perGeIn();
    }

    /** @return the whole FE an amount of GE gives going out, rounded down */
    public static long toFeOut(final double ge) {
        return FeRate.whole(ge * FeRate.perGeOut());
    }

    /** @return the GE that gives an amount of FE going out */
    public static double toGeOut(final double fe) {
        return fe / FeRate.perGeOut();
    }

    private static long whole(final double fe) {
        return fe >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) Math.floor(fe + 1e-6);
    }
}
