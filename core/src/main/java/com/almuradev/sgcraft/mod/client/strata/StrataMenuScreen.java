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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.minecraft.MenuPanel;
import org.inspirenxe.strata.minecraft.SlotRendering;
import org.inspirenxe.strata.minecraft.StrataContainerScreen;

import java.util.Optional;

/**
 * A screen of SGCraft's over a menu with slots, drawn with Strata as 2.3's MalisisCore ones were: a dark movable
 * window, its slots where the menu put them, and what the view lays round them. The window's x closes the menu as
 * Escape does. The view is told each client tick, so it can follow the menu's data as the server syncs it.
 *
 * @param <M> the menu
 * @param <V> what draws it
 */
public final class StrataMenuScreen<M extends AbstractContainerMenu, V extends StrataMenuScreen.View<M>>
        extends StrataContainerScreen<M> {

    private final V view;

    /**
     * @param width the width of the menu's slot area, which its slots are placed in
     * @param height its height
     */
    public StrataMenuScreen(final M menu, final Inventory inventory, final net.minecraft.network.chat.Component title,
            final int width, final int height, final V view) {
        super(menu, inventory, title, width, height, view::build, SGStrata.THEME, SlotRendering.VANILLA);
        this.view = view;
        view.attach(this);
    }

    /** @return the view of the menu screen of this kind that is open, if one is */
    public static <V extends View<?>> Optional<V> current(final Class<V> type) {
        return Minecraft.getInstance().gui.screen() instanceof final StrataMenuScreen<?, ?> screen
                && type.isInstance(screen.view) ? Optional.of(type.cast(screen.view)) : Optional.empty();
    }

    /** @return the view drawing this screen */
    public V view() {
        return this.view;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.view.tick();
    }

    /**
     * @return a movable window of the title, holding the content as it is sized, whose x closes the menu; in the
     *     middle of the screen, with nothing else round it
     */
    public Component<?> window(final ComponentFactory factory, final Text title, final AnchorPanel content) {
        final Window window = factory.window(title)
                .setMovable(true)
                .setResizable(false)
                .setCollapsible(false)
                .setMaximizable(false)
                .add(content.setAnchor(Anchor.TOP_LEFT))
                .setAnchor(Anchor.CENTER);
        window.onEvent(Closable.CLOSE, event -> {
            // Only the window's own x: a popup inside it closing asks too, and that reaches the window
            if (event.target() != window) {
                return;
            }
            event.cancel();
            this.onClose();
        });
        return factory.anchorPanel().add(window).setAnchor(Anchor.FILL);
    }

    /** @return a label of a text at a place in its parent, that a press goes through to the slots under */
    public static Label label(final ComponentFactory factory, final Text text, final float x, final float y) {
        final Label label = factory.label(text);
        label.setHitTestVisible(false);
        label.setAnchor(Anchor.TOP_LEFT).setMargin(x, y, 0f, 0f);
        return label;
    }

    /**
     * @return a Minecraft text as Strata text, each part in the colour it is written in, or the colour given where it
     *     has none, so a translation's values keep their own colours
     */
    public static Text styled(final net.minecraft.network.chat.Component text, final int colour) {
        final Text[] out = {Text.empty()};
        text.visit((style, part) -> {
            final TextColor own = style.getColor();
            out[0] = out[0].append(part, TextStyle.DEFAULT.withColor(own == null ? colour
                    : 0xFF000000 | own.getValue()));
            return Optional.empty();
        }, Style.EMPTY);
        return out[0];
    }

    /** @return a value to give a translation, in a colour of its own */
    public static MutableComponent value(final Object value, final int colour) {
        return net.minecraft.network.chat.Component.literal(String.valueOf(value))
                .withStyle(style -> style.withColor(colour & 0xFFFFFF));
    }

    /** @return a translated text, in a colour, with its values in their own */
    public static Text text(final int colour, final String key, final String fallback, final Object... args) {
        return StrataMenuScreen.styled(net.minecraft.network.chat.Component.translatableWithFallback(key, fallback,
                args), colour);
    }

    /**
     * What a menu screen shows round its slots.
     *
     * @param <M> the menu
     */
    public interface View<M extends AbstractContainerMenu> {

        /** Called once as the screen is made, before it is built: the screen whose window this view fills. */
        void attach(StrataMenuScreen<M, ?> screen);

        /** @return the tree round the slots, made with the screen's {@link #window} */
        Component<?> build(ComponentFactory factory, M menu, MenuPanel slots);

        /** Called each client tick while the screen is open. */
        default void tick() {
        }
    }
}
