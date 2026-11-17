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

package com.almuradev.sgcraft.mod.client.strata;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.minecraft.StrataScreen;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A Strata screen of SGCraft's, shown in the game as a vanilla screen: it doesn't pause the game, the world shows behind
 * it, and Escape or its window's x shuts it.
 */
public final class StrataHost extends StrataScreen {

    private final Screen content;

    private StrataHost(final Screen content) {
        super(Component.empty(), content, SGStrata.THEME);
        this.content = content;
    }

    /** Opens a Strata screen in place of whatever screen is open. */
    public static void open(final Screen content) {
        Minecraft.getInstance().gui.setScreen(new StrataHost(content));
    }

    /** @return the Strata screen of this kind that is open, if one is */
    public static <T extends Screen> Optional<T> current(final Class<T> type) {
        return Minecraft.getInstance().gui.screen() instanceof final StrataHost host && type.isInstance(host.content)
                ? Optional.of(type.cast(host.content)) : Optional.empty();
    }

    /**
     * Updates the Strata screen of this kind that is open, or else opens one made fresh if it should open.
     *
     * @param open whether to open it if it isn't already
     */
    public static <T extends Screen> void show(final Class<T> type, final boolean open, final Supplier<T> make,
            final Consumer<T> update) {
        final Optional<T> shown = StrataHost.current(type);
        if (shown.isPresent()) {
            update.accept(shown.get());
        } else if (open) {
            StrataHost.open(make.get());
        }
    }

    /** Shuts whatever screen is open. */
    public static void close() {
        Minecraft.getInstance().gui.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
            final float a) {
        // The world shows as it is
    }
}
