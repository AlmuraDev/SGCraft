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

package com.almuradev.sgcraft.mod.client.computer;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.computer.DialingTerminal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The pictures on the dialing computers' screens, one for each computer in view, painted again up to
 * {@value #FRAMES_PER_SECOND} times a second while something shows it: the monitor in the world, or the screen of the
 * player working it. A computer whose gate is idle is painted only a few times a second, as only its stream of data
 * moves, and at once whenever what it shows changes. A picture nothing has shown for a few seconds is let go.
 */
public final class ComputerDisplays {

    private static final int FRAMES_PER_SECOND = 20;
    private static final long FRAME_NANOS = 1_000_000_000L / ComputerDisplays.FRAMES_PER_SECOND;
    private static final long IDLE_FRAME_NANOS = 250_000_000L;
    private static final long FORGET_NANOS = 5_000_000_000L;

    private static final Map<String, Display> DISPLAYS = new HashMap<>();
    private static WeakReference<@Nullable ClientLevel> session = new WeakReference<>(null);

    private ComputerDisplays() {
    }

    /**
     * @return the texture showing a computer's screen as it is now, painted again if a frame has passed since it last
     *     was
     */
    public static Identifier texture(final DialingTerminal computer, final float partialTick) {
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        if (ComputerDisplays.session.get() != level) {
            ComputerDisplays.clear();
            ComputerDisplays.session = new WeakReference<>(level);
        }
        final long now = System.nanoTime();
        ComputerDisplays.forgetOld(now);
        final Display display = ComputerDisplays.DISPLAYS.computeIfAbsent(computer.pictureName(), Display::new);
        display.asked = now;
        if (level == null) {
            return display.id;
        }
        final ComputerDisplay.View view = ComputerDisplay.View.of(level, computer, partialTick);
        final ComputerDisplay.View still = view.still();
        final long every = view.busy() ? ComputerDisplays.FRAME_NANOS : ComputerDisplays.IDLE_FRAME_NANOS;
        if (now - display.painted >= every || !still.equals(display.shown)) {
            display.painted = now;
            display.shown = still;
            ComputerDisplay.paint(display.canvas, view);
            display.canvas.copyTo(display.texture.getPixels());
            display.texture.upload();
            if (!display.registered) {
                minecraft.getTextureManager().register(display.id, display.texture);
                display.registered = true;
            }
        }
        return display.id;
    }

    private static void forgetOld(final long now) {
        final Iterator<Display> displays = ComputerDisplays.DISPLAYS.values().iterator();
        while (displays.hasNext()) {
            final Display display = displays.next();
            if (now - display.asked > ComputerDisplays.FORGET_NANOS) {
                display.free();
                displays.remove();
            }
        }
    }

    /** Lets every picture go, as the client leaves a world or its resources reload. */
    public static void clear() {
        for (final Display display : ComputerDisplays.DISPLAYS.values()) {
            display.free();
        }
        ComputerDisplays.DISPLAYS.clear();
        ComputerCanvas.forgetFonts();
    }

    // One computer's picture: painted in memory, then handed to its texture
    private static final class Display {

        final Identifier id;
        final ComputerCanvas canvas = new ComputerCanvas(ComputerDisplay.WIDTH, ComputerDisplay.HEIGHT);
        final DynamicTexture texture;
        long painted = Long.MIN_VALUE / 2;
        // What it showed when last painted, but for its moving parts
        ComputerDisplay.@Nullable View shown;
        long asked;
        boolean registered;

        Display(final String name) {
            this.id = SGCraft.id("dynamic/dialing_computer_" + name);
            this.texture = new DynamicTexture(() -> "SGCraft dialing computer " + name, ComputerDisplay.WIDTH,
                    ComputerDisplay.HEIGHT, true);
        }

        void free() {
            if (this.registered) {
                // Closes the texture too
                Minecraft.getInstance().getTextureManager().release(this.id);
            } else {
                this.texture.close();
            }
        }
    }
}
