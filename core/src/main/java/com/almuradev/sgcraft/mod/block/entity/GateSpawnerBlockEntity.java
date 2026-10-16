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

import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.SGCraftApi;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.block.GateSpawnerBlock;
import com.almuradev.sgcraft.mod.villager.SGVillagers;
import com.almuradev.sgcraft.mod.worldgen.StructureGates;
import com.almuradev.sgcraft.mod.zpm.ZpmChests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;

/**
 * How a spawner a structure left builds its gate. Structure templates set it in the spawner's block data:
 *
 * <ul>
 *     <li>{@code generated}: whether the spawner builds its gate by itself once the world runs, as a structure's
 *     does. A spawner a player places leaves this out and builds its gate at once.</li>
 *     <li>{@code chevrons}: the gate's chevrons, or 0 to choose seven or nine by {@code upgrade_chance}.</li>
 *     <li>{@code upgrade_chance}: how many gates in a hundred have nine chevrons in place of seven.</li>
 *     <li>{@code camouflage}: the blocks over the gate's bottom row, left to right as seen from in front.</li>
 *     <li>{@code unique_within}: if more than 0, the gate isn't built when another stands this many blocks away or
 *     nearer, so a village has one gate however many plazas it builds.</li>
 *     <li>{@code tokra}: whether a Tok'ra stands in front of the gate once it is built, in a world the server's
 *     settings let Tok'ra stand in.</li>
 *     <li>{@code village}: whether the spawner is in a village's plaza, whose Tok'ra the server's village settings
 *     decide in place of {@code tokra}.</li>
 * </ul>
 */
public final class GateSpawnerBlockEntity extends BlockEntity {

    private static final int SEVEN = 7;
    private static final int NINE = 9;

    private boolean generated;
    private int chevrons;
    private int upgradeChance;
    private List<BlockState> camouflage = List.of();
    private int uniqueWithin;
    private boolean tokra;
    private boolean village;

    public GateSpawnerBlockEntity(final BlockPos pos, final BlockState state) {
        super(SGBlockEntityTypes.GATE_SPAWNER, pos, state);
    }

    /** Sets the gate's chevrons, in place of choosing them by chance. */
    public void chevrons(final int chevrons) {
        this.chevrons = chevrons;
        this.setChanged();
    }

    /** Sets whether a Tok'ra stands in front of the gate once it is built. */
    public void tokra(final boolean tokra) {
        this.tokra = tokra;
        this.setChanged();
    }

    public static void serverTick(final ServerLevel level, final BlockPos pos, final BlockState state,
            final GateSpawnerBlockEntity entity) {
        if (!entity.generated || SGServer.current().isEmpty()
                || !(state.getBlock() instanceof GateSpawnerBlock spawner)) {
            return;
        }
        entity.generated = false;
        if (entity.uniqueWithin > 0 && entity.gateNear(level, pos)) {
            GateSpawnerBlock.clear(level, pos, state);
            return;
        }
        final int chevrons = entity.chevrons > 0 ? entity.chevrons
                : level.getRandom().nextInt(100) < entity.upgradeChance ? GateSpawnerBlockEntity.NINE
                        : GateSpawnerBlockEntity.SEVEN;
        final boolean tokra = entity.village ? StructureGates.villages().tokra() : entity.tokra;
        if (!spawner.build(level, pos, null, chevrons, entity.camouflage)) {
            return;
        }
        // The gate is marked as the world's, not a player's
        SGCraftApi.get().stargates().at(GlobalPos.of(level.dimension(), pos)).ifPresent(gate ->
                gate.set(GateProperties.GENERATED, true, Cause.of(level)));
        if (tokra && StructureGates.tokraIn(level.dimension())) {
            SGVillagers.spawnTokra(level, pos, state.getValue(HorizontalDirectionalBlock.FACING));
        }
        // Now and then a chest with a ZPM in it stands by the gate, as in the original
        ZpmChests.roll(level, pos, state.getValue(HorizontalDirectionalBlock.FACING));
    }

    private boolean gateNear(final ServerLevel level, final BlockPos pos) {
        final double reach = (double) this.uniqueWithin * this.uniqueWithin;
        return SGCraftApi.get().stargates().in(level.dimension()).stream()
                .anyMatch(gate -> gate.position().pos().distSqr(pos) <= reach);
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        this.generated = input.getBooleanOr("generated", false);
        this.chevrons = input.getIntOr("chevrons", 0);
        this.upgradeChance = input.getIntOr("upgrade_chance", 0);
        this.camouflage = input.read("camouflage", BlockState.CODEC.listOf()).orElse(List.of());
        this.uniqueWithin = input.getIntOr("unique_within", 0);
        this.tokra = input.getBooleanOr("tokra", false);
        this.village = input.getBooleanOr("village", false);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("generated", this.generated);
        output.putInt("chevrons", this.chevrons);
        output.putInt("upgrade_chance", this.upgradeChance);
        output.store("camouflage", BlockState.CODEC.listOf(), this.camouflage);
        output.putInt("unique_within", this.uniqueWithin);
        output.putBoolean("tokra", this.tokra);
        output.putBoolean("village", this.village);
    }
}
