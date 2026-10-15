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

package com.almuradev.sgcraft.gate;

import com.almuradev.sgcraft.SGCraft;
import com.almuradev.sgcraft.api.address.Address;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;

/**
 * An address written in a gate style's glyphs, through the glyph fonts in the mod's assets: one font for each style,
 * mapping an address's characters to its symbols. Pegasus and Universe borrow the Milky Way's point of origin.
 */
public final class GlyphText {

    private GlyphText() {
    }

    /** @return the address in the style's glyphs, in the groups it is written in */
    public static MutableComponent of(final Address address, final DialStyle style) {
        final FontDescription font = new FontDescription.Resource(SGCraft.id(switch (style) {
            case PEGASUS -> "glyphs_pegasus";
            case UNIVERSE -> "glyphs_universe";
            default -> "glyphs_milkyway";
        }));
        final MutableComponent text = Component.empty().withStyle(style == DialStyle.PEGASUS
                ? ChatFormatting.AQUA : ChatFormatting.WHITE);
        for (int i = 0; i < address.size(); i++) {
            if (i == 4 || i == 7) {
                text.append(" ");
            }
            text.append(Component.literal(address.get(i).toString()).withStyle(s -> s.withFont(font)));
        }
        return text;
    }
}
