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

package com.almuradev.sgcraft.mod.client.view;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.block.entity.StargateBaseBlockEntity;
import com.almuradev.sgcraft.mod.view.RemoteViewPackets;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * The block entities in a remote view, as copies made on the client from the data their server sends its clients,
 * and drawn with their own renderers. A copy is made once the block it belongs to has arrived, and is loaded again
 * in place as its data changes, so that whatever it animates carries on. A renderer that fails leaves its type out of
 * the view from then on.
 */
final class RemoteViewBlockEntities {

    // Block entities the stream has sent, by position, and the copies made of those whose blocks are here
    private final Long2ObjectMap<RemoteViewPackets.BlockEntityState> sent = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<BlockEntity> copies = new Long2ObjectOpenHashMap<>();
    private final Set<BlockEntityType<?>> failedTypes = new HashSet<>();
    private final RandomSource random = RandomSource.create();

    /** @return the copies, by position, for the level to hand to the block renderers */
    Long2ObjectMap<BlockEntity> copies() {
        return this.copies;
    }

    void update(final RemoteViewPackets.BlockEntityState state, final RemoteViewLevel level) {
        this.sent.put(state.pos(), state);
        final BlockEntity copy = this.copies.get(state.pos());
        if (copy != null && BuiltInRegistries.BLOCK_ENTITY_TYPE.getId(copy.getType()) == state.type()) {
            RemoteViewBlockEntities.load(copy, state);
        } else {
            this.copies.remove(state.pos());
            this.place(state, level);
        }
    }

    void remove(final long pos) {
        this.sent.remove(pos);
        this.copies.remove(pos);
    }

    /** Drops the block entities of a section the view no longer has. */
    void removeSection(final long section) {
        final LongIterator iterator = this.sent.keySet().iterator();
        while (iterator.hasNext()) {
            final long pos = iterator.nextLong();
            if (SectionPos.blockToSection(pos) == section) {
                iterator.remove();
                this.copies.remove(pos);
            }
        }
    }

    /** Makes copies for the block entities in a section whose blocks have just arrived or changed. */
    void sectionArrived(final long section, final RemoteViewLevel level) {
        for (final RemoteViewPackets.BlockEntityState state : this.sent.values()) {
            if (SectionPos.blockToSection(state.pos()) != section) {
                continue;
            }
            final BlockEntity copy = this.copies.get(state.pos());
            if (copy == null || copy.getBlockState() != level.getBlockState(BlockPos.of(state.pos()))) {
                this.copies.remove(state.pos());
                this.place(state, level);
            }
        }
    }

    void clear() {
        this.sent.clear();
        this.copies.clear();
    }

    // A copy is made only where the block it belongs to is, as a block entity is only valid on its own blocks
    private void place(final RemoteViewPackets.BlockEntityState state, final RemoteViewLevel level) {
        final ClientLevel world = Minecraft.getInstance().level;
        final BlockEntityType<?> type = BuiltInRegistries.BLOCK_ENTITY_TYPE.byId(state.type());
        final BlockPos pos = BlockPos.of(state.pos());
        final BlockState block = level.getBlockState(pos);
        if (world == null || type == null || this.failedTypes.contains(type) || !type.isValid(block)) {
            return;
        }
        try {
            final BlockEntity copy = type.create(pos, block);
            if (copy == null) {
                return;
            }
            // Renderers read the game time and such from a block entity's world; the client's own serves, as the
            // copy is never added to it
            copy.setLevel(world);
            RemoteViewBlockEntities.load(copy, state);
            this.copies.put(state.pos(), copy);
        } catch (final RuntimeException e) {
            SGCraft.LOG.debug("Couldn't copy block entity {} for a remote view", type, e);
            this.failedTypes.add(type);
        }
    }

    private static void load(final BlockEntity copy, final RemoteViewPackets.BlockEntityState state) {
        final ClientLevel world = Minecraft.getInstance().level;
        if (world == null) {
            return;
        }
        copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, world.registryAccess(),
                state.tag()));
    }

    /** Moves on by a tick what the copies animate themselves, such as an open gate's wormhole. */
    void tick() {
        for (final BlockEntity copy : this.copies.values()) {
            if (copy instanceof StargateBaseBlockEntity gate) {
                StargateBaseBlockEntity.tickHorizon(gate, this.random);
            }
        }
    }

    /** Submits every copy that has a renderer for drawing, relative to the camera. */
    void submit(final RemoteViewLevel level, final Vec3 camera, final CameraRenderState cameraState,
            final PoseStack poseStack, final SubmitNodeCollector collector, final float partialTick) {
        final BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        for (final BlockEntity copy : this.copies.values()) {
            if (!this.failedTypes.contains(copy.getType())) {
                this.submit(dispatcher, copy, level, camera, cameraState, poseStack, collector, partialTick);
            }
        }
    }

    private <E extends BlockEntity, S extends BlockEntityRenderState> void submit(
            final BlockEntityRenderDispatcher dispatcher, final E copy, final RemoteViewLevel level,
            final Vec3 camera, final CameraRenderState cameraState, final PoseStack poseStack,
            final SubmitNodeCollector collector, final float partialTick) {
        final @Nullable BlockEntityRenderer<E, S> renderer = dispatcher.getRenderer(copy);
        if (renderer == null) {
            return;
        }
        final BlockPos pos = copy.getBlockPos();
        poseStack.pushPose();
        try {
            final S state = renderer.createRenderState();
            renderer.extractRenderState(copy, state, partialTick, camera, null);
            // Lit by the view's light rather than the client's own world's; a gate as the middle of its ring is
            final BlockPos lightAt = copy instanceof StargateBaseBlockEntity gate
                    ? gate.frame().block(0, gate.shape().radius(), 0) : pos;
            state.lightCoords = LightCoordsUtil.getLightCoords(level, lightAt);
            poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            dispatcher.submit(state, poseStack, collector, cameraState);
        } catch (final RuntimeException e) {
            SGCraft.LOG.debug("Couldn't draw {} in a remote view; leaving its type out", copy, e);
            this.failedTypes.add(copy.getType());
        } finally {
            poseStack.popPose();
        }
    }
}
