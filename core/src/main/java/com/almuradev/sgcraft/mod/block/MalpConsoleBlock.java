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

import com.almuradev.sgcraft.mod.block.entity.MalpConsoleBlockEntity;
import com.almuradev.sgcraft.mod.item.SGDataComponents;
import com.almuradev.sgcraft.mod.item.SGItems;
import com.almuradev.sgcraft.mod.malp.MalpLinks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A MALP console: a desk with a screen that shows the feed of the MALPs paired with it, and drives them. It faces
 * the player who placed it. Used with a MALP item, it pairs that MALP with itself; any number of MALPs can be paired
 * with one console, and pairing a MALP again moves it to the new console. While someone drives at it, its monitor
 * shows their feed to the players around it.
 */
public final class MalpConsoleBlock extends HorizontalDirectionalBlock implements EntityBlock {

    // The desk, ten pixels high; the monitor stands on its far half
    private static final VoxelShape DESK = Block.box(0, 0, 0, 16, 10, 16);

    public MalpConsoleBlock(final Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING,
                Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
        return this.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
                context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
            final CollisionContext context) {
        return Shapes.or(MalpConsoleBlock.DESK, MalpConsoleBlock.monitor(state.getValue(
                HorizontalDirectionalBlock.FACING)));
    }

    // The monitor sits on the half of the desk away from the player using it
    private static VoxelShape monitor(final Direction facing) {
        return switch (facing) {
            case SOUTH -> Block.box(2, 10, 1, 14, 16, 8);
            case WEST -> Block.box(8, 10, 2, 15, 16, 14);
            case EAST -> Block.box(1, 10, 2, 8, 16, 14);
            default -> Block.box(2, 10, 8, 14, 16, 15);
        };
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new MalpConsoleBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level,
            final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hit) {
        if (!stack.is(SGItems.MALP)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            MalpConsoleBlock.pair(stack, GlobalPos.of(level.dimension(), pos));
            player.sendSystemMessage(Component.translatableWithFallback("message.sgcraft.malp.paired",
                    "The MALP is paired with this console").withStyle(ChatFormatting.GREEN));
        }
        return InteractionResult.SUCCESS;
    }

    /** Pairs a packed MALP with a console, so the MALP put down from it reports there. */
    public static void pair(final ItemStack malp, final GlobalPos console) {
        malp.set(SGDataComponents.MALP_CONSOLE, console);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
            final Player player, final BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) {
            MalpLinks.open(serverPlayer, GlobalPos.of(level.dimension(), pos));
        }
        return InteractionResult.SUCCESS;
    }
}
