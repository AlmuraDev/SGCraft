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
import com.almuradev.sgcraft.mod.view.RemoteViewPackets;
import com.almuradev.sgcraft.mod.view.RemoteViewSections;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Everything the client has of one remote view: the copy of the world around the source, its meshes, its entities and
 * block entities, the camera and the atmosphere. Packets for the view go into it as they arrive, and the render
 * thread brings its meshes up to date as it draws. Only the client's main thread uses this.
 */
final class RemoteViewScene implements AutoCloseable {

    // Meshes are dropped when the view hasn't been drawn for this long, and built again if it is drawn again
    private static final long IDLE_NANOS = 2_000_000_000L;
    // The view counts as loaded once everything this close to the camera is built
    private static final double LOADED_DISTANCE = 24;
    // The lamp's light is worked out again when the camera has moved or turned this much
    private static final double LAMP_MOVE = 1.0;
    private static final float LAMP_TURN = 10.0F;
    // A camera that moves further than this in a tick has jumped, rather than moved
    private static final double JUMP = 8.0;

    final int id;
    // How the view began, with whether it is the client's own as it now is
    RemoteViewPackets.Begin world;
    final RemoteViewLevel level;
    final RemoteViewMeshes meshes = new RemoteViewMeshes();
    final RemoteViewEntities entities = new RemoteViewEntities();
    final RemoteViewBlockEntities blockEntities = new RemoteViewBlockEntities();
    private final RemoteViewSmoothing x = new RemoteViewSmoothing(1);
    private final RemoteViewSmoothing y = new RemoteViewSmoothing(1);
    private final RemoteViewSmoothing z = new RemoteViewSmoothing(1);
    private final RemoteViewSmoothing yaw = new RemoteViewSmoothing(1);
    private final RemoteViewSmoothing pitch = new RemoteViewSmoothing(1);
    private RemoteViewPackets.@Nullable Frame frame;
    private long lastDrawn = System.nanoTime();
    private boolean loaded;
    // Where the lamp was when its light was last worked out
    private boolean lampOn;
    private Vec3 lampPos = Vec3.ZERO;
    private float lampYaw;
    private float lampPitch;

    RemoteViewScene(final RemoteViewPackets.Begin begin) {
        this.id = begin.view();
        this.world = begin;
        this.level = new RemoteViewLevel(begin, this.blockEntities.copies());
    }

    void sections(final RemoteViewPackets.Sections packet) {
        final ClientLevel client = Minecraft.getInstance().level;
        if (client == null) {
            return;
        }
        for (final long pos : packet.forgotten()) {
            this.level.remove(pos);
            this.meshes.forget(pos);
            this.meshes.changed(pos);
            this.blockEntities.removeSection(pos);
        }
        for (final RemoteViewPackets.Section section : packet.sections()) {
            final RemoteViewSections.Decoded decoded;
            try {
                decoded = RemoteViewSections.decode(section.data(), client.registryAccess());
            } catch (final RuntimeException e) {
                SGCraft.LOG.warn("Couldn't read a section of a remote view at {}", SectionPos.of(section.pos()), e);
                continue;
            }
            this.level.put(section.pos(), decoded);
            this.meshes.changed(section.pos());
            this.blockEntities.sectionArrived(section.pos(), this.level);
        }
    }

    void heights(final RemoteViewPackets.Heights packet) {
        for (final long chunk : packet.forgotten()) {
            this.level.removeHeights(chunk);
        }
        for (final RemoteViewPackets.RainHeights chunk : packet.chunks()) {
            if (chunk.heights().length == RemoteViewPackets.RainHeights.COLUMNS) {
                this.level.putHeights(chunk.chunk(), chunk.heights());
            }
        }
    }

    void blockEntities(final RemoteViewPackets.BlockEntities packet) {
        for (final long pos : packet.removed()) {
            this.blockEntities.remove(pos);
        }
        for (final RemoteViewPackets.BlockEntityState state : packet.changed()) {
            this.blockEntities.update(state, this.level);
        }
    }

    void entities(final RemoteViewPackets.Entities packet) {
        for (final int id : packet.removed()) {
            this.entities.remove(id);
        }
        for (final RemoteViewPackets.EntityLooks looks : packet.changed()) {
            this.entities.looks(looks);
        }
    }

    void frame(final RemoteViewPackets.Frame packet) {
        final long now = System.nanoTime();
        final RemoteViewPackets.Camera camera = packet.camera();
        // A camera that has jumped, as through a gate, is put straight where it is
        final boolean snap = this.frame == null || new Vec3(camera.x(), camera.y(), camera.z()).distanceToSqr(
                this.x.target(), this.y.target(), this.z.target()) > RemoteViewScene.JUMP * RemoteViewScene.JUMP;
        this.x.set(camera.x(), now, snap);
        this.y.set(camera.y(), now, snap);
        this.z.set(camera.z(), now, snap);
        this.yaw.setAngle(camera.yaw(), now, snap);
        this.pitch.set(camera.pitch(), now, snap);
        this.frame = packet;
        this.entities.moves(packet.moves(), now);
        this.entities.tick();
        this.blockEntities.tick();
        if (now - this.lastDrawn > RemoteViewScene.IDLE_NANOS) {
            this.meshes.release();
            this.loaded = false;
        }
    }

    /** @return the latest frame, or null before the first arrives */
    RemoteViewPackets.@Nullable Frame latest() {
        return this.frame;
    }

    /** @return where the camera is to be drawn now */
    Vec3 cameraPosition(final long now) {
        return new Vec3(this.x.value(now), this.y.value(now), this.z.value(now));
    }

    float cameraYaw(final long now) {
        return (float) this.yaw.value(now);
    }

    float cameraPitch(final long now) {
        return (float) this.pitch.value(now);
    }

    /**
     * @return whether there is enough of the view to show: a frame, and the world near the camera built. Once it
     *     has been, it stays so, as later changes are built while the view shows what it had.
     */
    boolean loaded() {
        if (this.loaded || this.frame == null) {
            return this.loaded;
        }
        final Vec3 camera = new Vec3(this.x.target(), this.y.target(), this.z.target());
        // A camera above or below the world is in no section, so there is none to wait for
        final boolean inWorld = camera.y >= this.world.minY() && camera.y < this.world.minY() + this.world.height();
        this.loaded = (!inWorld || this.level.has(SectionPos.asLong(SectionPos.blockToSectionCoord(camera.x),
                SectionPos.blockToSectionCoord(camera.y), SectionPos.blockToSectionCoord(camera.z))))
                && !this.meshes.pendingNear(this.level, camera, RemoteViewScene.LOADED_DISTANCE);
        return this.loaded;
    }

    /** Brings the meshes up to date before the view is drawn. Only the render thread calls this. */
    void prepare(final Vec3 camera) {
        this.lastDrawn = System.nanoTime();
        final RemoteViewPackets.Frame latest = this.frame;
        if (latest != null) {
            this.updateLamp(latest.camera());
        }
        this.meshes.build(this.level, camera);
        this.meshes.resort(camera);
    }

    // The lamp's light is part of the meshes, so the sections it reaches are built again when it moves enough
    private void updateLamp(final RemoteViewPackets.Camera camera) {
        final Vec3 pos = new Vec3(camera.x(), camera.y(), camera.z());
        final boolean moved = camera.lamp() != this.lampOn || camera.lamp()
                && (pos.distanceToSqr(this.lampPos) > RemoteViewScene.LAMP_MOVE * RemoteViewScene.LAMP_MOVE
                || Math.abs(Mth.wrapDegrees(camera.yaw() - this.lampYaw)) > RemoteViewScene.LAMP_TURN
                || Math.abs(camera.pitch() - this.lampPitch) > RemoteViewScene.LAMP_TURN);
        if (!moved) {
            return;
        }
        final LongList before = this.lampOn ? this.meshes.near(this.lampPos, RemoteViewLevel.LAMP_REACH)
                : new LongArrayList();
        this.lampOn = camera.lamp();
        this.lampPos = pos;
        this.lampYaw = camera.yaw();
        this.lampPitch = camera.pitch();
        this.level.setLamp(this.lampOn, pos, Vec3.directionFromRotation(this.lampPitch, this.lampYaw));
        this.meshes.relight(before);
        if (this.lampOn) {
            this.meshes.relight(this.meshes.near(pos, RemoteViewLevel.LAMP_REACH));
        }
    }

    @Override
    public void close() {
        this.meshes.close();
        this.entities.clear();
        this.blockEntities.clear();
    }
}
