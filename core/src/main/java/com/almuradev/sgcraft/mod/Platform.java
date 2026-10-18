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

package com.almuradev.sgcraft.mod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.nio.file.Path;
import java.util.ServiceLoader;
import java.util.function.BiFunction;

/**
 * What common code needs from the mod loader it runs on. Each loader project provides one implementation, named in
 * its META-INF/services file.
 */
public interface Platform {

    static Platform get() {
        return Holder.INSTANCE;
    }

    String loaderName();

    /** @return the version of SGCraft the loader runs, as its mod metadata gives it */
    String modVersion();

    boolean isDevelopmentEnvironment();

    /**
     * Makes a menu type, which vanilla only lets each loader make its own way.
     *
     * @param factory makes the client's menu from its id and the player's inventory
     */
    <T extends AbstractContainerMenu> MenuType<T> menuType(final BiFunction<Integer, Inventory, T> factory);

    /** @return whether a player's client can take a payload of this type, which a client without the mod can't */
    boolean canSend(final ServerPlayer player, final CustomPacketPayload.Type<?> type);

    /** @return the folder the game keeps its mods' settings in */
    Path configDir();

    /**
     * @return whether an entity is a part of another, such as a piece of the ender dragon, which the world lists
     *     beside the entity it belongs to but which is drawn with it rather than on its own
     */
    boolean isPart(final Entity entity);

    /**
     * Offers FE to the block at a place, through one of its faces, as a cable would: the loader's energy system on
     * NeoForge, Team Reborn Energy on Fabric.
     *
     * @param face the face of that block the energy goes in through
     * @param amount the most FE to give
     * @return how much it took, from none to {@code amount}
     */
    long offerEnergy(final ServerLevel level, final BlockPos pos, final Direction face, final long amount);

    final class Holder {

        private static final Platform INSTANCE = ServiceLoader.load(Platform.class, Platform.class.getClassLoader())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No " + Platform.class.getName()
                        + " for this mod loader"));

        private Holder() {
        }
    }
}
