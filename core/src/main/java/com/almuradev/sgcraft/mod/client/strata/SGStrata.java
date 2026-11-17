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

import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.gate.DialStyle;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.attribute.item.Selection;
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.text.FontId;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Border;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.ThemeId;
import org.inspirenxe.strata.api.theme.ThemeRef;

/**
 * What SGCraft's Strata screens share, so they look as SGCraft 2.3's MalisisCore screens did: a dark window with a
 * thin white border, boxes inside it bordered the same, MalisisCore's colours, its small coloured + - ? buttons and its
 * lists of black rows. Only reached once {@code StrataScreens} says Strata is installed and turned on.
 */
public final class SGStrata {

    /** The theme: MalisisCore's window and buttons, on Minecraft's vanilla theme (assets/sgcraft/strata/themes). */
    public static final ThemeRef THEME = ThemeRef.of(ThemeId.of(SGCraft.MOD_ID, "in_game"));

    // MalisisCore's text colours, as 2.3 used them
    public static final int WHITE = 0xFFFFFFFF;
    public static final int GRAY = 0xFFAAAAAA;
    public static final int DARK_GRAY = 0xFF555555;
    public static final int BLUE = 0xFF5555FF;
    public static final int AQUA = 0xFF55FFFF;
    public static final int GREEN = 0xFF55FF55;
    public static final int RED = 0xFFFF5555;
    public static final int YELLOW = 0xFFFFFF55;
    public static final int GOLD = 0xFFFFAA00;

    /** A box within a window, as a MalisisCore form within a form: see-through, bordered white at 185. */
    public static final Paint BOX = Paint.solid(0x23FFFFFF);
    public static final Border BOX_BORDER = Border.of(0xB9FFFFFF, 1f);
    /** A thin line across a box, as MalisisCore's separator. */
    public static final Paint LINE = Paint.solid(0xB9FFFFFF);
    /** A list's rows: black, and grey when chosen, each bordered. */
    public static final Paint ROW = Paint.solid(0xFF000000);
    public static final Paint ROW_SELECTED = Paint.solid(0xFF4A4A4A);
    public static final Border ROW_BORDER = Border.of(0xFFA0A0A0, 1f);

    /** How tall a button is, and the small + - ? ones. */
    public static final float BUTTON_HEIGHT = 16f;
    public static final float SMALL_WIDTH = 12f;
    /** How tall the bar along a window's bottom is: a button, with as much room above it as below */
    public static final float BAR_HEIGHT = 22f;
    public static final float SMALL_HEIGHT = 16f;
    /** How big a checkbox's box is: 30% smaller than Strata's own, which is its caption's height and 8 more (17). */
    public static final float CHECKBOX_SIZE = 12f;
    // Room kept round a window on a small screen
    private static final float MARGIN = 10f;

    private SGStrata() {
    }

    /** @return text in this player's language, as Minecraft translates it, with English when it has no translation */
    public static Text text(final String key, final String fallback, final Object... args) {
        return Text.of(net.minecraft.network.chat.Component.translatableWithFallback(key, fallback, args).getString());
    }

    /** @return a Minecraft text as Strata text, in the colour it is written in, or white */
    public static Text of(final net.minecraft.network.chat.Component text) {
        final net.minecraft.network.chat.TextColor colour = text.getStyle().getColor();
        return Text.of(text.getString()).color(colour == null ? SGStrata.WHITE : 0xFF000000 | colour.getValue());
    }

    /** @return an address in a gate type's glyphs, in the groups it is written in */
    public static Text glyphs(final Address address, final DialStyle style) {
        final TextStyle glyph = TextStyle.DEFAULT.withFont(FontId.of(SGCraft.MOD_ID, switch (style) {
            case PEGASUS -> "glyphs_pegasus";
            case UNIVERSE -> "glyphs_universe";
            default -> "glyphs_milkyway";
        })).withColor(style == DialStyle.PEGASUS ? SGStrata.AQUA : SGStrata.WHITE);
        Text text = Text.empty();
        for (int i = 0; i < address.size(); i++) {
            if (i == 4 || i == 7) {
                text = text.append(" ");
            }
            text = text.append(address.get(i).toString(), glyph);
        }
        return text;
    }

    /**
     * @return a movable window in the middle of the screen, of the size given or as much of the screen as there is,
     *     whose x closes the screen
     */
    public static Window window(final ComponentFactory factory, final Text title, final Component<?> content,
            final float width, final float height) {
        final Minecraft minecraft = Minecraft.getInstance();
        final Window window = factory.window(title)
                .setMovable(true)
                .setResizable(false)
                .setCollapsible(false)
                .setMaximizable(false)
                .add(content.setAnchor(Anchor.FILL))
                .setSize(Math.min(width, minecraft.getWindow().getGuiScaledWidth() - SGStrata.MARGIN),
                        Math.min(height, minecraft.getWindow().getGuiScaledHeight() - SGStrata.MARGIN))
                .setAnchor(Anchor.CENTER);
        window.onEvent(Closable.CLOSE, event -> {
            // Only the window's own x: a menu or dialog inside it closing asks too, and that reaches the window
            if (event.target() != window) {
                return;
            }
            event.cancel();
            StrataHost.close();
        });
        return window;
    }

    /** @return a window over a screen, given the x at its top right, which does what its Close or Cancel does */
    public static Window closesWith(final Window window, final Runnable close) {
        window.setClosable(true);
        window.onEvent(Closable.CLOSE, event -> {
            // Only the window's own x, not a menu or dialog inside it closing
            if (event.target() != window) {
                return;
            }
            event.cancel();
            close.run();
        });
        return window;
    }

    /** @return the bar along the bottom of its parent, whose children anchored to its middle are centred in it */
    public static AnchorPanel bar(final ComponentFactory factory) {
        // Its middle a unit low: a window keeps more room under its content than there is between a box and the
        // bar, so this centres what is in it between the box and the window's edge
        final AnchorPanel bar = factory.anchorPanel().setPadding(0f, 2f, 0f, 0f);
        bar.setAnchor(Anchor.FILL).setHeight(SGStrata.BAR_HEIGHT);
        bar.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.BOTTOM);
        return bar;
    }

    /** @return a box within a window, bordered as MalisisCore's were */
    public static AnchorPanel box(final ComponentFactory factory) {
        return factory.anchorPanel().setBackground(SGStrata.BOX).setBorder(SGStrata.BOX_BORDER).setPadding(2f);
    }

    /** @return a line across its parent at a height, from a left edge to a right one, as MalisisCore's separator */
    public static Component<?> line(final ComponentFactory factory, final float left, final float top,
            final float right) {
        return factory.anchorPanel().setBackground(SGStrata.LINE).setHeight(1f).setAnchor(Anchor.FILL)
                .setMargin(left, top, right, 0f).setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
    }

    /** @return a button of a width that runs something when pressed */
    public static Button button(final ComponentFactory factory, final Text text, final float width,
            final Runnable press) {
        return factory.button(text).setSize(width, SGStrata.BUTTON_HEIGHT).onEvent(Button.PRESSED,
                event -> press.run());
    }

    /** @return one of MalisisCore's small buttons: a single coloured character, as 2.3's + - ? */
    public static Button small(final ComponentFactory factory, final String label, final int colour,
            final Text tooltip, final Runnable press) {
        // Centred on a capital, 7 tall; a small letter, such as the x that clears a search, is 5 tall on the same
        // baseline, so sits 1 lower, and is lifted by that
        final boolean small = label.equals(label.toLowerCase(Locale.ROOT))
                && !label.equals(label.toUpperCase(Locale.ROOT));
        final Button button = factory.button(Text.of(label).color(colour))
                .setTextPadding(2f, small ? 0f : 2f, 2f, 0f);
        button.setMinSize(0f, 0f);
        button.setSize(SGStrata.SMALL_WIDTH, SGStrata.SMALL_HEIGHT);
        button.setTooltip(tooltip);
        return button.onEvent(Button.PRESSED, event -> press.run());
    }

    /** @return a checkbox with its caption, its box 30% smaller than Strata's own (17 to 12), as all SGCraft's are */
    public static CheckBox checkBox(final ComponentFactory factory, final Text caption) {
        return factory.checkBox(caption).setBoxSize(SGStrata.CHECKBOX_SIZE);
    }

    /** Lays a list out as MalisisCore's: its own rows, no gaps, drawn again as the choice changes. */
    public static <T> ListView<T> rows(final ListView<T> list, final float rowHeight) {
        list.setRowHeight(rowHeight);
        list.setRowBackgrounds(false);
        list.setRowSpacing(0f);
        list.onEvent(Selection.SELECTED, event -> list.refresh());
        return list;
    }

    /** @return a row of a list, black or grey as it is chosen, bordered */
    public static AnchorPanel row(final AnchorPanel row, final boolean selected) {
        return row.setBackground(selected ? SGStrata.ROW_SELECTED : SGStrata.ROW).setBorder(SGStrata.ROW_BORDER);
    }
}
