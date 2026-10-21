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

package com.almuradev.sgcraft.mod.rings;

import com.almuradev.sgcraft.mod.block.entity.SGBlockEntityTypes;
import com.almuradev.sgcraft.mod.item.SGItems;
import com.almuradev.sgcraft.mod.obelisk.ObeliskActions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A ring platform, as 2.2.0's: set in a floor, its rings rise around those standing on the three by three blocks
 * around it; set in a ceiling, they drop to the floor below. Its rings, five blocks across, are drawn by its block
 * entity. Sneak-use two platforms to link them; use one to see its power and fill it with naquadah; use one with the
 * configurator to set who may use it.
 */
public final class RingPlatformBlock extends Block implements EntityBlock {

    private final boolean ceiling;

    public RingPlatformBlock(final Properties properties, final boolean ceiling) {
        super(properties);
        this.ceiling = ceiling;
    }

    /** @return whether it is the ceiling kind, whose rings drop to the floor below */
    public boolean ceiling() {
        return this.ceiling;
    }

    // Whoever placed it owns it
    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state,
            final @Nullable LivingEntity placer, final ItemStack stack) {
        if (placer instanceof final Player player
                && level.getBlockEntity(pos) instanceof final RingPlatformBlockEntity platform) {
            platform.setOwner(player.getUUID(), player.getName().getString());
        }
    }

    // With a configurator, the obelisks' screen opens on it
    @Override
    protected InteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level,
            final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hit) {
        if (!stack.is(SGItems.CONFIGURATOR)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (player instanceof final ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof final RingPlatformBlockEntity platform) {
            ObeliskActions.open(serverPlayer, platform);
        }
        return InteractionResult.SUCCESS;
    }

    // Sneaking links it to another; otherwise its fuel screen opens, for those who may use it
    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
            final Player player, final BlockHitResult hit) {
        if (player instanceof final ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof final RingPlatformBlockEntity platform) {
            if (serverPlayer.isSecondaryUseActive()) {
                Rings.link(serverPlayer, platform);
            } else if (!platform.mayUse(serverPlayer)) {
                Rings.tell(serverPlayer, "no_access", "You aren't allowed to use these rings", true);
            } else {
                platform.open(serverPlayer);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new RingPlatformBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
            final BlockEntityType<T> type) {
        if (level.isClientSide() || type != SGBlockEntityTypes.RING_PLATFORM) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) -> RingPlatformBlockEntity.serverTick((ServerLevel) tickLevel, pos,
                tickState, (RingPlatformBlockEntity) entity);
    }
}
