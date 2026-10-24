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
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Remote views on displays in the client's world, such as the monitor of a MALP console someone is driving at. Each
 * display near the camera that shows a view gets a small texture of its own, which the view is drawn into a few
 * times a second, and which the display's renderer puts on the display.
 *
 * <p>A display's renderer asks for its picture each frame it is seen, as the frame's render state is extracted. Once
 * the frame's world is drawn, the pictures asked for are drawn again if they are due, a couple each frame at most;
 * none is drawn while the game is paused or while a screen shows the client's own view, when the displays keep the
 * picture they had. A texture no display has asked for in a while is freed, and the view's meshes with it once the
 * view is drawn nowhere. Only the render thread uses this.</p>
 */
public final class RemoteViewDisplays {

    /** How wide a display's picture is, in pixels. */
    public static final int WIDTH = 384;
    /** How high a display's picture is, in pixels. */
    public static final int HEIGHT = 216;
    // The server streams a display's view only to players near it, as near as its settings say; this only caps how
    // far off a display that has a view may be and still be drawn
    private static final double RANGE = 64;
    // A picture is drawn at most twenty times a second, and half as often on a display further off than this, in
    // blocks, where so fine a picture can't be told from a coarser one
    private static final long FRAME_NANOS = 50_000_000L;
    private static final double NEAR = 24;
    // At most this many displays have pictures at once, and this many are drawn in one frame
    private static final int SLOTS = 8;
    private static final int DRAWS_PER_FRAME = 2;
    // A picture no display has asked for in this long is freed
    private static final long KEEP_NANOS = 1_000_000_000L;
    // How long after a screen last showed the client's own view the displays may be drawn again
    private static final long SCREEN_NANOS = 200_000_000L;

    private static final Slot[] SLOT_LIST = RemoteViewDisplays.slots();
    // The displays asked for this frame, with how far each is from the camera, squared
    private static final Map<BlockPos, Double> ASKED = new HashMap<>();
    private static long screenShown;

    private RemoteViewDisplays() {
    }

    // One picture: the display it is for, the view drawn into it, and its texture once made
    private static final class Slot {

        private final Identifier id;
        private @Nullable BlockPos display;
        private int scene = -1;
        private boolean drawn;
        private long lastDrawn;
        private long lastAsked;
        // How far the display was from the camera when last asked for, squared
        private double distance;
        private @Nullable DisplayTexture texture;
        // Whether the texture is in the texture manager, which closes it when it is released
        private boolean registered;

        private Slot(final int index) {
            this.id = SGCraft.id("dynamic/remote_view_display_" + index);
        }
    }

    // A display's picture, as the texture manager hands it to the game's renderers
    private static final class DisplayTexture extends AbstractTexture {

        private final RemoteViewTarget target;
        private boolean closed;

        private DisplayTexture(final String label) {
            this.target = new RemoteViewTarget(label);
            this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        }

        // Takes the target's texture as drawn, and says whether there is one
        private boolean update() {
            this.texture = this.target.color();
            this.textureView = this.target.colorView();
            return this.textureView != null;
        }

        // The target owns the texture, so it is closed once, by the target
        @Override
        public void close() {
            this.texture = null;
            this.textureView = null;
            if (!this.closed) {
                this.closed = true;
                this.target.close();
            }
        }
    }

    private static Slot[] slots() {
        final Slot[] slots = new Slot[RemoteViewDisplays.SLOTS];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = new Slot(i);
        }
        return slots;
    }

    /**
     * Asks for the picture on a display that is seen this frame. Display renderers call this as they extract their
     * render state, only for a display whose picture the camera can see.
     *
     * @param camera where the camera is
     * @return the texture to draw on the display, or null if the display shows no view, or its picture isn't drawn
     *     yet; the display then shows its own idle look
     */
    public static @Nullable Identifier picture(final BlockPos display, final Vec3 camera) {
        final double distance = Vec3.atCenterOf(display).distanceToSqr(camera);
        if (distance > RemoteViewDisplays.RANGE * RemoteViewDisplays.RANGE) {
            return null;
        }
        final RemoteViewScene scene = RemoteViewClient.sceneOn(display);
        if (scene == null) {
            return null;
        }
        final BlockPos at = display.immutable();
        RemoteViewDisplays.ASKED.put(at, distance);
        final Slot slot = RemoteViewDisplays.slotFor(at);
        return slot != null && slot.drawn && slot.scene == scene.id && slot.texture != null ? slot.id : null;
    }

    /** Notes that a screen shows the client's own view this frame. */
    static void screenShown() {
        RemoteViewDisplays.screenShown = System.nanoTime();
    }

    /**
     * Draws the pictures that were asked for this frame and are due, and frees those no longer asked for. The client
     * calls this once a frame, after the world is drawn and before the screen.
     */
    public static void render() {
        final long now = System.nanoTime();
        for (final Map.Entry<BlockPos, Double> asked : RemoteViewDisplays.ASKED.entrySet()) {
            final Slot slot = RemoteViewDisplays.slotFor(asked.getKey());
            if (slot != null) {
                slot.lastAsked = now;
                slot.distance = asked.getValue();
            }
        }
        for (final Slot slot : RemoteViewDisplays.SLOT_LIST) {
            if (slot.display != null && (now - slot.lastAsked > RemoteViewDisplays.KEEP_NANOS
                    || RemoteViewClient.sceneOn(slot.display) == null)) {
                RemoteViewDisplays.free(slot);
            }
        }
        // The nearest displays without a picture get one, while there are pictures to give
        final List<Map.Entry<BlockPos, Double>> waiting = new ArrayList<>();
        for (final Map.Entry<BlockPos, Double> asked : RemoteViewDisplays.ASKED.entrySet()) {
            if (RemoteViewDisplays.slotFor(asked.getKey()) == null) {
                waiting.add(asked);
            }
        }
        waiting.sort(Map.Entry.comparingByValue());
        for (final Map.Entry<BlockPos, Double> asked : waiting) {
            final Slot slot = RemoteViewDisplays.freeSlot();
            if (slot == null) {
                break;
            }
            slot.display = asked.getKey();
            slot.lastAsked = now;
            slot.lastDrawn = 0;
            slot.drawn = false;
        }
        RemoteViewDisplays.ASKED.clear();
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isPaused() || now - RemoteViewDisplays.screenShown < RemoteViewDisplays.SCREEN_NANOS) {
            return;
        }
        final List<Slot> due = new ArrayList<>();
        for (final Slot slot : RemoteViewDisplays.SLOT_LIST) {
            final long every = slot.distance > RemoteViewDisplays.NEAR * RemoteViewDisplays.NEAR
                    ? RemoteViewDisplays.FRAME_NANOS * 2 : RemoteViewDisplays.FRAME_NANOS;
            if (slot.display != null && slot.lastAsked == now && now - slot.lastDrawn >= every) {
                due.add(slot);
            }
        }
        due.sort(Comparator.comparingLong(slot -> slot.lastDrawn));
        final float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        for (final Slot slot : due.subList(0, Math.min(due.size(), RemoteViewDisplays.DRAWS_PER_FRAME))) {
            RemoteViewDisplays.draw(minecraft, slot, now, partialTick);
        }
    }

    private static void draw(final Minecraft minecraft, final Slot slot, final long now, final float partialTick) {
        final RemoteViewScene scene = slot.display == null ? null : RemoteViewClient.sceneOn(slot.display);
        if (scene == null) {
            return;
        }
        slot.lastDrawn = now;
        if (scene.id != slot.scene) {
            slot.scene = scene.id;
            slot.drawn = false;
        }
        if (!scene.loaded()) {
            // Nothing is shown while the view loads, so its meshes are built from here until it has
            scene.prepare(scene.cameraPosition(now));
            if (!scene.loaded()) {
                return;
            }
        }
        if (slot.texture == null) {
            slot.texture = new DisplayTexture("SGCraft remote view display " + slot.id.getPath());
        }
        final DisplayTexture texture = slot.texture;
        if (texture.target.draw(scene, RemoteViewDisplays.WIDTH, RemoteViewDisplays.HEIGHT,
                minecraft.gameRenderer.featureRenderDispatcher(), partialTick) && texture.update()) {
            // Handed to the texture manager once it has something in it, as the game's renderers find it there
            if (!slot.registered) {
                minecraft.getTextureManager().register(slot.id, texture);
                slot.registered = true;
            }
            slot.drawn = true;
        }
    }

    private static @Nullable Slot slotFor(final BlockPos display) {
        for (final Slot slot : RemoteViewDisplays.SLOT_LIST) {
            if (display.equals(slot.display)) {
                return slot;
            }
        }
        return null;
    }

    private static @Nullable Slot freeSlot() {
        for (final Slot slot : RemoteViewDisplays.SLOT_LIST) {
            if (slot.display == null) {
                return slot;
            }
        }
        return null;
    }

    private static void free(final Slot slot) {
        slot.display = null;
        slot.scene = -1;
        slot.drawn = false;
        if (slot.registered) {
            // Closes the texture, and the target it draws into
            Minecraft.getInstance().getTextureManager().release(slot.id);
        } else if (slot.texture != null) {
            slot.texture.close();
        }
        slot.texture = null;
        slot.registered = false;
    }

    /** Frees every picture, as the client leaves the server. */
    static void clear() {
        for (final Slot slot : RemoteViewDisplays.SLOT_LIST) {
            RemoteViewDisplays.free(slot);
        }
        RemoteViewDisplays.ASKED.clear();
    }
}
