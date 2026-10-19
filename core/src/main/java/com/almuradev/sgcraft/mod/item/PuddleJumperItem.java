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

package com.almuradev.sgcraft.mod.item;

import com.almuradev.sgcraft.mod.entity.PuddleJumperEntity;
import com.almuradev.sgcraft.mod.entity.SGEntityTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/**
 * A puddle jumper, packed up: used on a block, it sets the jumper down on it, facing the way the player faces, if there
 * is room for the whole of it, nose to tail.
 */
public final class PuddleJumperItem extends Item {

    public PuddleJumperItem(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level level = context.getLevel();
        final ItemStack stack = context.getItemInHand();
        final BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        final PuddleJumperEntity jumper = SGEntityTypes.PUDDLE_JUMPER.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (jumper == null) {
            return InteractionResult.FAIL;
        }
        final Vec3 at = Vec3.atBottomCenterOf(pos);
        jumper.snapTo(at.x, at.y, at.z, context.getRotation(), 0);
        if (!jumper.fits(jumper.getYRot())) {
            if (context.getPlayer() != null && !level.isClientSide()) {
                context.getPlayer().sendOverlayMessage(Component.translatableWithFallback(
                        "message.sgcraft.puddle_jumper.no_room", "There is no room here for a puddle jumper")
                        .withStyle(ChatFormatting.RED));
            }
            return InteractionResult.FAIL;
        }
        if (level instanceof final ServerLevel serverLevel) {
            EntityType.<PuddleJumperEntity>createDefaultStackConfig(serverLevel, stack, context.getPlayer())
                    .apply(jumper);
            serverLevel.addFreshEntity(jumper);
            serverLevel.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, at);
            stack.consume(1, context.getPlayer());
        }
        return InteractionResult.SUCCESS;
    }
}
