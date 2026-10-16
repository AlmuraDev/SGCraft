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

package com.almuradev.sgcraft.neoforge.test;

import com.almuradev.sgcraft.api.ActionResult;
import com.almuradev.sgcraft.api.SGCraftApi;
import com.almuradev.sgcraft.api.gate.DialMode;
import com.almuradev.sgcraft.api.gate.DialResult;
import com.almuradev.sgcraft.api.gate.GatePlacement;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.GateSize;
import com.almuradev.sgcraft.api.gate.GateTypes;
import com.almuradev.sgcraft.api.gate.IrisTypes;
import com.almuradev.sgcraft.api.gate.PlacementMode;
import com.almuradev.sgcraft.api.gate.PlacementResult;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.tool.ExplorerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/**
 * The Ori supergate, and the explorer dialer's record of the generated gates a player has visited.
 */
final class OriGameTestsNeoForge {

    private OriGameTestsNeoForge() {
    }

    /** Adds the tests to the list {@link SGGameTestsNeoForge} registers. */
    static void register(final Map<String, BiConsumer<GameTestHelper, BlockPos>> tests) {
        tests.put("ori_only_ori", OriGameTestsNeoForge::onlyOri);
        tests.put("ori_dials_ori", OriGameTestsNeoForge::dialsOri);
        tests.put("ori_takes_no_upgrades", OriGameTestsNeoForge::noUpgrades);
        tests.put("explorer_counts_visits", OriGameTestsNeoForge::explorerVisits);
    }

    private static Stargate placeOri(final GameTestHelper helper, final BlockPos at) {
        final PlacementResult result = SGCraftApi.get().stargates().place(GatePlacement.builder()
                .position(GlobalPos.of(helper.getLevel().dimension(), at)).orientation(FrontAndTop.NORTH_UP)
                .type(SGGameTestsNeoForge.type(helper, GateTypes.ORI)).size(new GateSize(4)).chevrons(9)
                .mode(PlacementMode.FORCE).noFixtures().build(), SGGameTestsNeoForge.server(helper));
        if (result.status() != PlacementResult.Status.PLACED) {
            throw helper.assertionException(Component.literal("Placing an Ori supergate failed: " + result.status()));
        }
        return result.gate().orElseThrow();
    }

    // An Ori supergate and a Milky Way gate can't connect, either way; and an Ori gate can't be a standard gate
    private static void onlyOri(final GameTestHelper helper, final BlockPos origin) {
        final Stargate ori = OriGameTestsNeoForge.placeOri(helper, origin);
        final Stargate milkyWay = SGGameTestsNeoForge.place(helper, origin.offset(48, 0, 0), 9);
        ori.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        milkyWay.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        final AtomicReference<DialResult> out = new AtomicReference<>();
        final AtomicReference<DialResult> in = new AtomicReference<>();
        ori.dial(SGGameTestsNeoForge.addressOf(milkyWay, ori), DialMode.STANDARD, SGGameTestsNeoForge.server(helper))
                .thenAccept(out::set);
        milkyWay.dial(SGGameTestsNeoForge.addressOf(ori, milkyWay), DialMode.QUICK,
                SGGameTestsNeoForge.server(helper)).thenAccept(in::set);
        helper.assertTrue(milkyWay.setType(SGGameTestsNeoForge.type(helper, GateTypes.ORI),
                SGGameTestsNeoForge.server(helper)) == ActionResult.NOT_POSSIBLE,
                "A standard gate became an Ori gate");
        helper.succeedWhen(() -> {
            helper.assertTrue(out.get() == DialResult.INCOMPATIBLE, "The Ori gate's dial ended " + out.get());
            helper.assertTrue(in.get() == DialResult.INCOMPATIBLE, "The Milky Way gate's dial ended " + in.get());
        });
    }

    // Two Ori supergates connect, their rings linking symbol by symbol, though asked to dial at once
    private static void dialsOri(final GameTestHelper helper, final BlockPos origin) {
        final Stargate a = OriGameTestsNeoForge.placeOri(helper, origin);
        final Stargate b = OriGameTestsNeoForge.placeOri(helper, origin.offset(48, 0, 0));
        a.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        b.set(GateProperties.REQUIRES_POWER, false, SGGameTestsNeoForge.server(helper));
        final AtomicReference<DialResult> result = new AtomicReference<>();
        a.dial(SGGameTestsNeoForge.addressOf(b, a), DialMode.QUICK, SGGameTestsNeoForge.server(helper))
                .thenAccept(result::set);
        helper.runAfterDelay(40, () -> helper.assertTrue(result.get() == null,
                "The Ori dial finished at once, as " + result.get()));
        helper.succeedWhen(() -> {
            helper.assertTrue(result.get() == DialResult.CONNECTED, "The dial ended " + result.get());
            helper.assertTrue(b.connection().isPresent(), "The other Ori gate isn't connected");
        });
    }

    // An Ori supergate takes no iris, has nine chevrons' reach, and loses an iris when it becomes Ori
    private static void noUpgrades(final GameTestHelper helper, final BlockPos origin) {
        final Stargate ori = OriGameTestsNeoForge.placeOri(helper, origin);
        helper.assertTrue(ori.set(GateProperties.IRIS, IrisTypes.METAL, SGGameTestsNeoForge.server(helper))
                == ActionResult.NOT_POSSIBLE, "An Ori gate took an iris");
        helper.assertTrue(ori.chevrons() == 9, "An Ori gate has " + ori.chevrons() + " chevrons");
        helper.assertTrue(ori.setChevrons(7, SGGameTestsNeoForge.server(helper)) == ActionResult.NOT_POSSIBLE,
                "An Ori gate's chevrons changed");
        helper.assertTrue(ori.setType(SGGameTestsNeoForge.type(helper, GateTypes.MILKY_WAY),
                SGGameTestsNeoForge.server(helper)) == ActionResult.DONE, "The Ori gate didn't turn Milky Way");
        ori.set(GateProperties.IRIS, IrisTypes.SHIELD, SGGameTestsNeoForge.server(helper));
        helper.assertTrue(ori.setType(SGGameTestsNeoForge.type(helper, GateTypes.ORI),
                SGGameTestsNeoForge.server(helper)) == ActionResult.DONE, "The gate didn't turn Ori");
        helper.assertTrue(ori.get(GateProperties.IRIS).isEmpty(), "The Ori gate kept its shield");
        helper.succeed();
    }

    // Only gates world generation built count as visits
    private static void explorerVisits(final GameTestHelper helper, final BlockPos origin) {
        final Stargate built = SGGameTestsNeoForge.place(helper, origin, 9);
        final Stargate generated = SGGameTestsNeoForge.place(helper, origin.offset(48, 0, 0), 9);
        generated.set(GateProperties.GENERATED, true, SGGameTestsNeoForge.server(helper));
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ExplorerData.visit(player, built.position());
        ExplorerData.visit(player, generated.position());
        final Set<GlobalPos> visited = ExplorerData.visited(helper.getLevel().getServer(), player.getUUID());
        helper.assertTrue(visited.size() == 1 && visited.contains(generated.position()),
                "The visits are " + visited);
        ExplorerData.rename(helper.getLevel().getServer(), player.getUUID(), generated.position(), "  Abydos  ");
        helper.assertTrue(ExplorerData.name(helper.getLevel().getServer(), player.getUUID(), generated.position())
                .map("Abydos"::equals).orElse(false), "The name wasn't kept");
        ExplorerData.forget(helper.getLevel().getServer(), player.getUUID());
        helper.assertTrue(ExplorerData.visited(helper.getLevel().getServer(), player.getUUID()).isEmpty(),
                "The visits weren't forgotten");
        helper.succeed();
    }
}
