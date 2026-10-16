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

package com.almuradev.sgcraft.mod.block;

import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.SGCraftApi;
import com.almuradev.sgcraft.api.SGCraftRegistries;
import com.almuradev.sgcraft.api.gate.GatePlacement;
import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.api.gate.PlacementMode;
import com.almuradev.sgcraft.api.gate.PlacementResult;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.block.entity.GateSpawnerBlockEntity;
import com.almuradev.sgcraft.mod.block.entity.SGBlockEntityTypes;
import com.almuradev.sgcraft.mod.block.entity.StargateBaseBlockEntity;
import com.almuradev.sgcraft.mod.gate.GateShape;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Builds a finished gate in its place, as the original's gate spawner does: the base goes where the block was placed,
 * facing the player as a base they placed would, and the ring rises from it. A supergate's spawner builds one of the
 * original's default size. A creative build tool, so it is only in the creative tab and has no recipe;
 * {@code /sgcraft place} does the same with a choice of size. Picking a block of a finished gate gives its spawner.
 * Spawners always build: no server setting turns them off.
 *
 * <p>Structures hold spawners too, which build their gates once the world runs, facing as the structure turned them;
 * their block entity says how. See {@link GateSpawnerBlockEntity}.</p>
 */
public final class GateSpawnerBlock extends Block implements SimpleWaterloggedBlock, EntityBlock {

    /** The radius of the supergates spawners build, as the original's default. */
    public static final int SUPERGATE_RADIUS = 7;

    // Every spawner, so that a finished gate can find its own
    private static final List<GateSpawnerBlock> SPAWNERS = new ArrayList<>();

    private final ResourceKey<GateType> gateType;
    private final boolean supergate;

    public GateSpawnerBlock(final Properties properties, final ResourceKey<GateType> gateType,
            final boolean supergate) {
        super(properties);
        this.gateType = gateType;
        this.supergate = supergate;
        GateSpawnerBlock.SPAWNERS.add(this);
        this.registerDefaultState(this.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false)
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.WATERLOGGED, HorizontalDirectionalBlock.FACING);
    }

    @Override
    protected BlockState rotate(final BlockState state, final Rotation rotation) {
        return state.setValue(HorizontalDirectionalBlock.FACING,
                rotation.rotate(state.getValue(HorizontalDirectionalBlock.FACING)));
    }

    @Override
    protected BlockState mirror(final BlockState state, final Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(HorizontalDirectionalBlock.FACING)));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new GateSpawnerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
            final BlockEntityType<T> type) {
        if (level.isClientSide() || type != SGBlockEntityTypes.GATE_SPAWNER) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) -> GateSpawnerBlockEntity.serverTick((ServerLevel) tickLevel, pos,
                tickState, (GateSpawnerBlockEntity) entity);
    }

    // Placed in water, it keeps the water, so the gate it builds stands in water as the sea round it. The base faces
    // whoever placed it, as a base they placed would
    @Override
    public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
        return this.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,
                context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER))
                .setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected FluidState getFluidState(final BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    /** @return the spawner that builds a gate of this type and kind, if there is one */
    public static Optional<GateSpawnerBlock> forGate(final ResourceKey<GateType> type, final boolean supergate) {
        return GateSpawnerBlock.SPAWNERS.stream()
                .filter(spawner -> spawner.gateType.equals(type) && spawner.supergate == supergate)
                .findFirst();
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state,
            final @Nullable LivingEntity by, final ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        // A loader may hold back the blocks' own placing until a player's placement ends, and a base only forms
        // once it is placed, so the gate is built after that
        SGServer.current().ifPresentOrElse(
                server -> server.nextTick(() -> this.build(serverLevel, pos, by, 9, List.of())),
                () -> this.build(serverLevel, pos, by, 9, List.of()));
    }

    /** Takes the spawner away, leaving the water it stood in if it stood in any. */
    public static void clear(final ServerLevel level, final BlockPos pos, final BlockState state) {
        level.setBlock(pos, (state.getValue(BlockStateProperties.WATERLOGGED) ? Blocks.WATER : Blocks.AIR)
                .defaultBlockState(), Block.UPDATE_ALL);
    }

    /**
     * Builds the gate in the spawner's place.
     *
     * @param by who placed the spawner, if anyone did
     * @param chevrons the gate's chevrons
     * @param camouflage the blocks over the gate's bottom row, left to right as seen from in front
     * @return whether the gate was built
     */
    public boolean build(final ServerLevel level, final BlockPos pos, final @Nullable LivingEntity by,
            final int chevrons, final List<BlockState> camouflage) {
        final BlockState state = level.getBlockState(pos);
        if (!state.is(this)) {
            return false;
        }
        final Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        GateSpawnerBlock.clear(level, pos, state);
        final Optional<Holder.Reference<GateType>> type = level.registryAccess()
                .lookupOrThrow(SGCraftRegistries.GATE_TYPE).get(this.gateType);
        if (type.isEmpty()) {
            return false;
        }
        final GateShape shape = this.supergate ? GateShape.supergate(GateSpawnerBlock.SUPERGATE_RADIUS)
                : GateShape.STANDARD;
        final Cause cause = by != null ? Cause.of(by) : Cause.of(level);
        final PlacementResult result = SGCraftApi.get().stargates().place(GatePlacement.builder()
                .position(GlobalPos.of(level.dimension(), pos))
                .orientation(FrontAndTop.fromFrontAndTop(facing, Direction.UP))
                .type(type.get()).size(shape.size()).chevrons(chevrons).mode(PlacementMode.FORCE).noFixtures()
                .build(), cause);
        if (result.status() == PlacementResult.Status.PLACED
                && level.getBlockEntity(pos) instanceof StargateBaseBlockEntity base) {
            for (int i = 0; i < Math.min(camouflage.size(), base.camouflage().getContainerSize()); i++) {
                base.camouflage().setItem(i, new ItemStack(camouflage.get(i).getBlock()));
            }
        }
        if (result.status() != PlacementResult.Status.PLACED && by == null) {
            SGCraft.LOG.warn("The gate at {} couldn't be built: {} {}", pos, result.status(),
                    result.reason().map(Component::getString).orElse(""));
        }
        if (result.status() != PlacementResult.Status.PLACED && by instanceof Player player) {
            player.sendSystemMessage(result.reason().orElseGet(() -> Component.translatableWithFallback(
                    "message.sgcraft.spawner_failed", "The gate couldn't be built: %s",
                    result.status().name().toLowerCase(Locale.ROOT).replace('_', ' ')))
                    .copy().withStyle(ChatFormatting.RED));
        }
        return result.status() == PlacementResult.Status.PLACED;
    }
}
