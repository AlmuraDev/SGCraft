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

package com.almuradev.sgcraft.mod.malp;

import com.almuradev.sgcraft.mod.block.MalpConsoleBlock;
import com.almuradev.sgcraft.mod.entity.MalpEntity;
import com.almuradev.sgcraft.mod.item.MalpTabletItem;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * What a player drives and watches MALPs from: a MALP console, or a MALP tablet in their hand. Each lists the MALPs
 * paired with it, has a radio whose signal reaches them, and stays in use while the player stays with it.
 */
public sealed interface MalpController {

    /** @return the MALPs paired with this, as last seen, by name */
    List<MalpRegistry.Entry> malps(MinecraftServer server);

    /**
     * @param malp the MALP's UUID
     * @param loaded the MALP itself, if its chunk is loaded
     * @return whether a MALP is still paired with this; a console takes a MALP that isn't loaded to be paired with it
     */
    boolean lists(UUID malp, @Nullable MalpEntity loaded);

    /**
     * @param player the player using this
     * @param world the MALP's world
     * @param malp where the MALP is
     * @param range how far the radio reaches, in blocks
     * @return how this radio reaches a MALP
     */
    MalpSignal signal(ServerPlayer player, ResourceKey<Level> world, Vec3 malp, int range);

    /**
     * @param player the player using this, who is still there and alive
     * @return whether the player is still with this: within reach of a console, or holding a tablet as they were
     */
    boolean valid(ServerPlayer player);

    /**
     * @param owed the gate energy used since the battery was last drained
     * @return whether this has the power to show a feed
     */
    boolean powered(double owed);

    /** A console, by where it is. */
    record Console(GlobalPos pos) implements MalpController {

        // How far past the player's reach they can step back with the screen open
        private static final double SLACK = 2;

        @Override
        public List<MalpRegistry.Entry> malps(final MinecraftServer server) {
            return MalpRegistry.get(server).pairedWith(this.pos);
        }

        @Override
        public boolean lists(final UUID malp, final @Nullable MalpEntity loaded) {
            return loaded == null || this.pos.equals(loaded.console());
        }

        @Override
        public MalpSignal signal(final ServerPlayer player, final ResourceKey<Level> world, final Vec3 malp,
                final int range) {
            return MalpSignal.between(this.pos, world, malp, range);
        }

        @Override
        public boolean valid(final ServerPlayer player) {
            return player.level().dimension().equals(this.pos.dimension())
                    && player.level().getBlockState(this.pos.pos()).getBlock() instanceof MalpConsoleBlock
                    && player.isWithinBlockInteractionRange(this.pos.pos(), Console.SLACK);
        }

        @Override
        public boolean powered(final double owed) {
            return true;
        }
    }

    /**
     * A tablet in a player's hand. It is the same tablet only while it is the very same stack, in the same hand, in
     * the same world.
     *
     * @param hand the hand the tablet was used from
     * @param stack the tablet
     * @param world the world the player was in when they used it
     */
    record Tablet(InteractionHand hand, ItemStack stack, ResourceKey<Level> world) implements MalpController {

        @Override
        public List<MalpRegistry.Entry> malps(final MinecraftServer server) {
            final Set<UUID> paired = new HashSet<>(MalpTabletItem.malps(this.stack));
            return MalpRegistry.get(server).all().stream().filter(entry -> paired.contains(entry.id())).toList();
        }

        @Override
        public boolean lists(final UUID malp, final @Nullable MalpEntity loaded) {
            return MalpTabletItem.malps(this.stack).contains(malp);
        }

        @Override
        public MalpSignal signal(final ServerPlayer player, final ResourceKey<Level> world, final Vec3 malp,
                final int range) {
            return MalpSignal.between(player.level().dimension(), player.position(), world, malp, range);
        }

        @Override
        public boolean valid(final ServerPlayer player) {
            return player.level().dimension().equals(this.world) && player.getItemInHand(this.hand) == this.stack;
        }

        @Override
        public boolean powered(final double owed) {
            return MalpTabletItem.energy(this.stack) > owed;
        }

        @Override
        public boolean equals(final Object other) {
            return other instanceof Tablet tablet && tablet.hand == this.hand && tablet.stack == this.stack
                    && tablet.world.equals(this.world);
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this.stack);
        }
    }
}
