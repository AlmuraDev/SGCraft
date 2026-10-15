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

package com.almuradev.sgcraft.mod.gate;

import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.SGCraftRegistries;
import com.almuradev.sgcraft.api.address.AddressTarget;
import com.almuradev.sgcraft.api.gate.FixtureBuffer;
import com.almuradev.sgcraft.api.gate.GatePlacement;
import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.api.gate.GateTypes;
import com.almuradev.sgcraft.api.gate.PlacementMode;
import com.almuradev.sgcraft.api.gate.PlacementResult;
import com.almuradev.sgcraft.api.gate.PlacementRule;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.block.RingBlock;
import com.almuradev.sgcraft.mod.block.SGBlocks;
import com.almuradev.sgcraft.mod.block.StargateBaseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Builds a whole gate where a {@link GatePlacement} asks, or at the nearest spot within its search radius where it
 * fits: the ring, the base, which forms the gate, and the fixtures. The gate stands or lies flat as the placement's
 * orientation says; a flat gate's ring runs out from the base towards the top of its ring.
 */
final class GatePlacer {

    private final SGStargates stargates;
    private final Cause cause;

    GatePlacer(final SGStargates stargates, final Cause cause) {
        this.stargates = stargates;
        this.cause = cause;
    }

    PlacementResult place(final GatePlacement placement) {
        final ServerLevel level = this.stargates.server().getLevel(placement.position().dimension());
        if (level == null) {
            return Result.of(PlacementResult.Status.INVALID_POSITION);
        }
        final Optional<ResourceKey<GateType>> type = this.stargates.server().registryAccess()
                .lookupOrThrow(SGCraftRegistries.GATE_TYPE).getResourceKey(placement.type());
        if (type.isEmpty() || !placement.type().supports(placement.size())) {
            return Result.of(PlacementResult.Status.UNSUPPORTED_SIZE);
        }
        final Optional<Player> player = this.cause.first(Player.class);
        if (player.isPresent() && !level.mayInteract(player.get(), placement.position().pos())) {
            return Result.of(PlacementResult.Status.DENIED);
        }
        final Direction facing = GateOrientation.facing(placement.orientation());
        final GateOrientation lying = GateOrientation.of(placement.orientation());
        PlacementResult.Status worst = PlacementResult.Status.INVALID_POSITION;
        Optional<Component> firstReason = Optional.empty();
        for (final BlockPos candidate : GatePlacer.candidates(placement.position().pos(), placement.searchRadius())) {
            final GatePlacement here = placement.withPosition(GlobalPos.of(level.dimension(), candidate));
            final PlacementResult.Status status = this.check(level, here, facing, lying);
            if (status == null) {
                final Optional<Component> reason = GatePlacer.rules(level, here);
                if (reason.isEmpty()) {
                    return this.build(level, here, facing, lying, type.get());
                }
                if (firstReason.isEmpty()) {
                    firstReason = reason;
                }
                worst = GatePlacer.worse(worst, PlacementResult.Status.RULE_FAILED);
                continue;
            }
            worst = GatePlacer.worse(worst, status);
        }
        if (worst == PlacementResult.Status.RULE_FAILED && firstReason.isPresent()) {
            return Result.failed(firstReason.get());
        }
        return Result.of(worst);
    }

    // Spots to try, nearest first: the position itself, then outwards through the search radius
    private static List<BlockPos> candidates(final BlockPos origin, final int radius) {
        final List<BlockPos> spots = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    spots.add(origin.offset(dx, dy, dz));
                }
            }
        }
        spots.sort(Comparator.comparingDouble(pos -> pos.distSqr(origin)));
        return spots;
    }

    // Why the gate can't go at a spot, or null if it can, rules aside
    private PlacementResult.Status check(final ServerLevel level, final GatePlacement here, final Direction facing,
            final GateOrientation lying) {
        final BlockPos base = here.position().pos();
        final GateShape shape = GateShape.of(here.size()).orElse(GateShape.STANDARD);
        final GateFrame frame = new GateFrame(base, facing, lying, shape);
        final BlockPos top = frame.block(0, shape.height() - 1, 0);
        if (!level.isInWorldBounds(base) || !level.isInWorldBounds(top)
                || !level.getWorldBorder().isWithinBounds(base) || !level.getWorldBorder().isWithinBounds(top)) {
            return PlacementResult.Status.INVALID_POSITION;
        }
        final Optional<AddressTarget> address = this.stargates.addressing()
                .addressOf(here.position(), level.dimension())
                .flatMap(a -> this.stargates.addressing().resolve(a, level.dimension()));
        if (address.isEmpty()) {
            return PlacementResult.Status.INVALID_POSITION;
        }
        // A gate may share its address cell with another, as in the original; the address then reaches the first
        final List<BlockPos> spots = new ArrayList<>(GateFormation.footprint(frame));
        // Fixtures go round the base as for a standing gate, whichever way the gate lies
        final GateFrame upright = frame.upright();
        for (final BlockPos offset : here.fixtures().states().keySet()) {
            spots.add(upright.block(offset.getX(), offset.getY(), offset.getZ()));
        }
        boolean obstructed = false;
        for (final BlockPos pos : spots) {
            final BlockState state = level.getBlockState(pos);
            // Only a finished gate is in the way; the blocks of one not finished, or of one broken, are in the way
            // as any block is
            if (state.getBlock() instanceof RingBlock ring && GateFormation.baseOf(level, pos, ring.isSupergate())
                    .isPresent() || state.getBlock() instanceof StargateBaseBlock
                    && state.getValue(StargateBaseBlock.FORMED)) {
                return PlacementResult.Status.GATE_IN_THE_WAY;
            }
            if (!state.canBeReplaced() || state.getDestroySpeed(level, pos) < 0) {
                obstructed = true;
            }
        }
        if (obstructed && here.mode() == PlacementMode.ATTEMPT) {
            return PlacementResult.Status.OBSTRUCTED;
        }
        return null;
    }

    private static Optional<Component> rules(final ServerLevel level, final GatePlacement here) {
        for (final PlacementRule rule : here.type().placementRules()) {
            final Optional<Component> reason = rule.check(level, here);
            if (reason.isPresent()) {
                return reason;
            }
        }
        return Optional.empty();
    }

    private PlacementResult build(final ServerLevel level, final GatePlacement here, final Direction facing,
            final GateOrientation lying, final ResourceKey<GateType> type) {
        final BlockPos base = here.position().pos();
        level.getChunk(base);
        final GateShape shape = GateShape.of(here.size()).orElse(GateShape.STANDARD);
        final GateFrame frame = new GateFrame(base, facing, lying, shape);
        GateFormation.buildRing(level, frame);
        final StargateBaseBlock baseBlock = shape.isSupergate() ? SGBlocks.SUPERGATE_BASE : GatePlacer.baseBlock(type);
        level.setBlock(base, baseBlock.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing)
                .setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(base).is(Fluids.WATER)),
                Block.UPDATE_ALL);
        final Optional<SGStargate> gate = this.stargates.gate(here.position());
        if (gate.isEmpty()) {
            // Built but not formed: say which block got in the way, as it is likely one the world put back
            final String why = GateFormation.mismatch(level, frame)
                    .orElse("the base at " + base.toShortString() + " is " + level.getBlockState(base));
            SGCraft.LOG.warn("A gate built at {} didn't form: {}", base.toShortString(), why);
            return Result.failed(Component.translatableWithFallback("placement.sgcraft.not_formed",
                    "The gate was built but didn't form: %s", why));
        }
        // A type without a base of its own forms as a Milky Way gate and takes its type after
        if (!gate.get().typeKey().equals(type)) {
            this.stargates.server().registryAccess().lookupOrThrow(SGCraftRegistries.GATE_TYPE).get(type)
                    .ifPresent(holder -> gate.get().setType(holder, Cause.of(this.stargates.server())));
        }
        gate.get().setChevrons(here.chevrons(), Cause.of(this.stargates.server()));
        // A flat ring forms facing up, and is turned over after if it is to face down
        gate.get().setOrientation(here.orientation(), Cause.of(this.stargates.server()));
        final FixtureBuffer fixtures = here.fixtures();
        if (!fixtures.isEmpty()) {
            Fixtures.build(level, gate.get().frame(), fixtures, PlacementMode.FORCE);
        }
        return new Result(PlacementResult.Status.PLACED, Optional.of(gate.get()), Optional.empty());
    }

    private static StargateBaseBlock baseBlock(final ResourceKey<GateType> type) {
        final Map<ResourceKey<GateType>, StargateBaseBlock> bases = Map.of(
                GateTypes.MILKY_WAY, SGBlocks.STARGATE_BASE,
                GateTypes.PEGASUS, SGBlocks.STARGATE_BASE_PEGASUS,
                GateTypes.UNIVERSE, SGBlocks.STARGATE_BASE_UNIVERSE);
        return bases.getOrDefault(type, SGBlocks.STARGATE_BASE);
    }

    // Which failure to report when no spot works: the one closest to working
    private static PlacementResult.Status worse(final PlacementResult.Status so, final PlacementResult.Status now) {
        final List<PlacementResult.Status> order = List.of(PlacementResult.Status.INVALID_POSITION,
                PlacementResult.Status.GATE_IN_THE_WAY, PlacementResult.Status.OBSTRUCTED,
                PlacementResult.Status.RULE_FAILED);
        return order.indexOf(now) > order.indexOf(so) ? now : so;
    }

    /** The outcome of a placement. */
    record Result(Status status, Optional<Stargate> gate, Optional<Component> reason) implements PlacementResult {

        static Result of(final Status status) {
            return new Result(status, Optional.empty(), Optional.empty());
        }

        static Result failed(final Component reason) {
            return new Result(Status.RULE_FAILED, Optional.empty(), Optional.of(reason));
        }
    }
}
