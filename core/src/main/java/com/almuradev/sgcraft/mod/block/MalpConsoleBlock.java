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
import com.almuradev.sgcraft.mod.block.entity.SGBlockEntityTypes;
import com.almuradev.sgcraft.mod.computer.DialingComputers;
import com.almuradev.sgcraft.mod.entity.SeatEntity;
import com.almuradev.sgcraft.mod.item.SGDataComponents;
import com.almuradev.sgcraft.mod.item.SGItems;
import com.almuradev.sgcraft.mod.malp.MalpLinks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A MALP console, a {@link ComputerDeskBlock computer at a desk}, whose wide monitor shows the feed of a MALP paired
 * with it, and from which players drive them. Used with a MALP item, it pairs that MALP with itself; any number of
 * MALPs can be paired with one console, and pairing a MALP again moves it to the new console.
 *
 * <p>A player works it from an office chair in front of it, or standing at it. Its monitor shows the players near it
 * the feed of the player at it, or, with no one at it, the feed of the MALP last chosen at it; see
 * {@link MalpLinks}.</p>
 *
 * <p>Used sneaking with an empty hand, it is switched off, and its monitor goes dark, or on again; using it to work it
 * switches it on too. It needs power to run, as {@link com.almuradev.sgcraft.mod.computer.ComputerPower} says.
 * Switched off or without power, it shows no feed, and anyone at it is taken off it.</p>
 */
public final class MalpConsoleBlock extends ComputerDeskBlock implements EntityBlock {

    public MalpConsoleBlock(final Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return state.getValue(ComputerDeskBlock.PART) == Part.MIDDLE_LOWER
                ? new MalpConsoleBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
            final BlockEntityType<T> type) {
        if (type != SGBlockEntityTypes.MALP_CONSOLE || level.isClientSide()) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) -> MalpConsoleBlockEntity.serverTick((ServerLevel) tickLevel, pos,
                (MalpConsoleBlockEntity) entity);
    }

    @Override
    protected InteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level,
            final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hit) {
        if (!stack.is(SGItems.MALP)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        if (!level.isClientSide()) {
            MalpConsoleBlock.pair(stack, GlobalPos.of(level.dimension(), ComputerDeskBlock.main(pos, state)));
            player.sendSystemMessage(Component.translatableWithFallback("message.sgcraft.malp.paired",
                    "The MALP is paired with this console").withStyle(ChatFormatting.GREEN));
        }
        return InteractionResult.SUCCESS;
    }

    /** Pairs a packed MALP with a console, so the MALP put down from it reports there. */
    public static void pair(final ItemStack malp, final GlobalPos console) {
        malp.set(SGDataComponents.MALP_CONSOLE, console);
    }

    // Sneaking, switches the console off or on; otherwise seats the player in a free chair in front of the console,
    // which opens its screen, or else opens it with the player standing at it
    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
            final Player player, final BlockHitResult hit) {
        if (player instanceof final ServerPlayer serverPlayer) {
            final ServerLevel serverLevel = serverPlayer.level();
            final BlockPos main = ComputerDeskBlock.main(pos, state);
            if (player.isSecondaryUseActive()) {
                DialingComputers.togglePower(serverPlayer, main);
                return InteractionResult.SUCCESS;
            }
            if (!DialingComputers.startUp(serverPlayer, main)) {
                return InteractionResult.SUCCESS;
            }
            final BlockPos chair = serverPlayer.getVehicle() instanceof SeatEntity ? null
                    : DialingComputers.freeChair(serverLevel, main);
            if (chair != null) {
                DialingComputers.sit(serverPlayer, chair);
            } else {
                MalpLinks.open(serverPlayer, GlobalPos.of(level.dimension(), main));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
