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

package com.almuradev.sgcraft.fabric;

import com.almuradev.sgcraft.mod.Platform;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.access.SGPermissions;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jspecify.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.function.BiFunction;

public final class PlatformFabric implements Platform {

    @Override
    public String loaderName() {
        return "Fabric";
    }

    @Override
    public String modVersion() {
        return FabricLoader.getInstance().getModContainer(SGCraft.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("");
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isPart(final Entity entity) {
        return entity instanceof EnderDragonPart;
    }

    @Override
    public boolean isModLoaded(final String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    // The Fabric permissions API's check(Entity, String, boolean), which LuckPerms answers, when a mod has it; found
    // by name, so SGCraft doesn't need it
    private static final @Nullable Method PERMISSIONS_CHECK = PlatformFabric.permissionsCheck();

    private static @Nullable Method permissionsCheck() {
        try {
            return Class.forName("me.lucko.fabric.api.permissions.v0.Permissions")
                    .getMethod("check", Entity.class, String.class, boolean.class);
        } catch (final ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    @Override
    public boolean hasPermission(final ServerPlayer player, final SGPermissions.Node node) {
        final boolean fallback = SGPermissions.fallback(player, node);
        if (PlatformFabric.PERMISSIONS_CHECK == null) {
            return fallback;
        }
        try {
            return (boolean) PlatformFabric.PERMISSIONS_CHECK.invoke(null, player, node.name(), fallback);
        } catch (final ReflectiveOperationException | RuntimeException e) {
            return fallback;
        }
    }

    @Override
    public long offerEnergy(final ServerLevel level, final BlockPos pos, final Direction face, final long amount) {
        final EnergyStorage storage = EnergyStorage.SIDED.find(level, pos, face);
        if (storage == null || !storage.supportsInsertion() || amount <= 0) {
            return 0;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            final long taken = storage.insert(amount, transaction);
            transaction.commit();
            return taken;
        }
    }

    @Override
    public <T extends AbstractContainerMenu> MenuType<T> menuType(final BiFunction<Integer, Inventory, T> factory) {
        return new MenuType<>(factory::apply, FeatureFlags.VANILLA_SET);
    }

    @Override
    public boolean canSend(final ServerPlayer player, final CustomPacketPayload.Type<?> type) {
        return ServerPlayNetworking.canSend(player, type);
    }
}
