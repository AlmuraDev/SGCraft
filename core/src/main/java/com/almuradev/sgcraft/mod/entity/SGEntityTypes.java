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

package com.almuradev.sgcraft.mod.entity;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.registry.RegistrySink;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** The mod's entity types. They exist once the loader has called {@link #register(RegistrySink)}. */
public final class SGEntityTypes {

    public static EntityType<IrisPlateEntity> IRIS_PLATE;
    public static EntityType<MalpEntity> MALP;

    private SGEntityTypes() {
    }

    public static void register(final RegistrySink<EntityType<?>> sink) {
        final ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, SGCraft.id("iris_plate"));
        SGEntityTypes.IRIS_PLATE = EntityType.Builder.<IrisPlateEntity>of(IrisPlateEntity::new, MobCategory.MISC)
                .noLootTable().noSave().noSummon().fireImmune().sized(0F, 0F).clientTrackingRange(10)
                .build(key);
        sink.register(key, SGEntityTypes.IRIS_PLATE);
        // The MALP, a block wide and a little under one tall; its model reaches further ahead and behind
        final ResourceKey<EntityType<?>> malp = ResourceKey.create(Registries.ENTITY_TYPE, SGCraft.id("malp"));
        SGEntityTypes.MALP = EntityType.Builder.<MalpEntity>of(MalpEntity::new, MobCategory.MISC)
                .noLootTable().sized(1.0F, 0.9F).clientTrackingRange(10).build(malp);
        sink.register(malp, SGEntityTypes.MALP);
    }
}
