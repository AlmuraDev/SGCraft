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

package com.almuradev.sgcraft.mod.view;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Encodes how an entity looks for a remote view, and puts it on a copy of the entity. That is its synced data, which
 * holds such things as its pose, whether it is a baby and its colors, and for a living entity what it wears and
 * holds. The data is encoded with the registries of the side writing it and read with those of the side reading it.
 */
public final class RemoteViewEntityData {

    private static final int END = 255;

    private RemoteViewEntityData() {
    }

    /** Encodes how an entity looks. */
    public static byte[] encode(final Entity entity, final RegistryAccess registries) {
        final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(256), registries);
        final List<SynchedEntityData.DataValue<?>> values = entity.getEntityData().getNonDefaultValues();
        if (values != null) {
            for (final SynchedEntityData.DataValue<?> value : values) {
                value.write(buf);
            }
        }
        buf.writeByte(RemoteViewEntityData.END);
        if (entity instanceof LivingEntity living) {
            buf.writeBoolean(true);
            for (final EquipmentSlot slot : EquipmentSlot.VALUES) {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, living.getItemBySlot(slot));
            }
        } else {
            buf.writeBoolean(false);
        }
        final byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return bytes;
    }

    /**
     * Puts how an entity looks, as {@link #encode} wrote it, on a copy of the entity that starts out as its type
     * does by default.
     */
    public static void apply(final byte[] data, final Entity entity, final RegistryAccess registries) {
        final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), registries);
        final List<SynchedEntityData.DataValue<?>> values = new ArrayList<>();
        int id;
        while ((id = buf.readUnsignedByte()) != RemoteViewEntityData.END) {
            values.add(SynchedEntityData.DataValue.read(buf, id));
        }
        if (!values.isEmpty()) {
            entity.getEntityData().assignValues(values);
        }
        if (buf.readBoolean()) {
            for (final EquipmentSlot slot : EquipmentSlot.VALUES) {
                final ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                if (entity instanceof LivingEntity living) {
                    living.setItemSlot(slot, stack);
                }
            }
        }
    }
}
