/*
 * This file is part of SGCraft, licensed under the MIT License (MIT).
 * It is based on the original SGCraft by gcewing.
 *
 * Copyright (c) AlmuraDev <https://github.com/AlmuraDev>
 * Copyright (c) 2016 gcewing <https://github.com/gcewing>
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

package com.almuradev.sgcraft.mod.client.gate;

import com.almuradev.sgcraft.api.SGCraftRegistries;
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.mod.client.SGClientConfig;
import com.almuradev.sgcraft.mod.gate.DialStyle;
import com.almuradev.sgcraft.mod.gate.GlyphText;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.GateInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * The top of a gate's base screen, as the original draws it: its title, the gate's address in large glyphs in the
 * panel, and the address in letters below it. Clicking the panel copies the address, and so does opening the screen
 * for a player who sets {@link SGClientConfig#copyAddressOnOpen()}.
 */
final class GateAddressPanel {

    /** The screens' width, and the panel's place in them, as the art draws it. */
    static final int WIDTH = 256;
    private static final int PANEL_LEFT = 10;
    private static final int PANEL_TOP = 24;
    private static final int PANEL_RIGHT = 246;
    private static final int PANEL_BOTTOM = 68;
    private static final int GLYPH_SCALE = 2;
    // The original's text color
    private static final int TEXT = 0xFF004C66;

    private @Nullable GateInfo gate;
    private long copiedAt = Long.MIN_VALUE;

    // Copies the address the first time the screen learns the gate, if the player's settings say to
    void set(final GateInfo gate) {
        final boolean first = this.gate == null;
        this.gate = gate;
        final Optional<Address> address = this.address();
        if (first && address.isPresent() && SGClientConfig.get().copyAddressOnOpen()) {
            Minecraft.getInstance().keyboardHandler.setClipboard(address.get().toString());
        }
    }

    private Optional<Address> address() {
        return this.gate == null ? Optional.empty() : Address.parse(this.gate.address());
    }

    void draw(final GuiGraphicsExtractor graphics, final Font font, final int left, final int top,
            final Component title) {
        final int middle = left + GateAddressPanel.WIDTH / 2;
        GateAddressPanel.centered(graphics, font, title, middle, top + 8);
        if (this.gate == null) {
            return;
        }
        final Optional<Address> address = this.address();
        if (address.isEmpty()) {
            GateAddressPanel.centered(graphics, font, Component.translatableWithFallback(
                    "screen.sgcraft.gate_base.hidden", "... Gate Address Hidden ..."), middle, top + 42);
            return;
        }
        // Dark on the pale panel, as the original draws them; the glyph text's own color is for chat
        final Component glyphs = GlyphText.of(address.get(), this.style()).withColor(GateAddressPanel.TEXT);
        final int scale = GateAddressPanel.GLYPH_SCALE;
        graphics.pose().pushMatrix();
        graphics.pose().translate(middle - font.width(glyphs) * scale / 2.0F, top + 46 - 5 * scale);
        graphics.pose().scale(scale, scale);
        graphics.text(font, glyphs, 0, 0, 0xFFFFFFFF, false);
        graphics.pose().popMatrix();
        final boolean copied = Util.getMillis() - this.copiedAt < 1500;
        GateAddressPanel.centered(graphics, font, copied ? Component.translatableWithFallback(
                "screen.sgcraft.gate_base.copied", "Copied") : Component.literal(address.get().toString()), middle,
                top + 72);
    }

    /** Draws text centered on {@code middle} in the screen's color, with no shadow, which smudges dark text. */
    static void centered(final GuiGraphicsExtractor graphics, final Font font, final Component text, final int middle,
            final int y) {
        graphics.text(font, text, middle - font.width(text) / 2, y, GateAddressPanel.TEXT, false);
    }

    /** Copies the address if the click is on the panel; @return whether it was */
    boolean click(final double x, final double y, final int left, final int top) {
        final Optional<Address> address = this.address();
        if (address.isEmpty() || x < left + GateAddressPanel.PANEL_LEFT || x > left + GateAddressPanel.PANEL_RIGHT
                || y < top + GateAddressPanel.PANEL_TOP || y > top + GateAddressPanel.PANEL_BOTTOM) {
            return false;
        }
        Minecraft.getInstance().keyboardHandler.setClipboard(address.get().toString());
        this.copiedAt = Util.getMillis();
        return true;
    }

    private DialStyle style() {
        final Identifier type = this.gate == null ? null : Identifier.tryParse(this.gate.type());
        return type == null ? DialStyle.MILKY_WAY : DialStyle.of(ResourceKey.create(SGCraftRegistries.GATE_TYPE, type));
    }
}
