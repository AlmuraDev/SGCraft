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

package com.almuradev.sgcraft.mod.client.dhd;

import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.address.Symbol;
import com.almuradev.sgcraft.api.gate.GateTypes;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.block.entity.DhdBlockEntity;
import com.almuradev.sgcraft.mod.block.entity.StargateBaseBlockEntity;
import com.almuradev.sgcraft.mod.client.ClientPlatform;
import com.almuradev.sgcraft.mod.dhd.DhdDial;
import com.almuradev.sgcraft.mod.dhd.DhdPress;
import com.almuradev.sgcraft.mod.gate.DialStyle;
import com.almuradev.sgcraft.mod.gate.DialView;
import com.almuradev.sgcraft.mod.sound.SGSounds;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A DHD seen face on: an outer ring of 26 symbols, an inner ring of 13, and the dome in the middle.
 *
 * <p>Symbols pressed light up, and a right click takes back the last. The keyboard works too: a letter, digit,
 * {@code @}, {@code +} or {@code .} presses its symbol, whatever the case, Backspace takes one back and Enter presses
 * the dome. A key with no symbol shows in red. The dome then dials them: the gate turns its ring for each symbol, as
 * the original's did, or locks every chevron at once when its {@code chevrons_lock_on_dial} property is on. While the
 * gate is active the buttons show the address it dialed, and the dome shuts it down. A button pressed sinks into the
 * face for a moment.</p>
 */
public final class DhdScreen extends Screen {

    // The rings, as fractions of the face's radius, as the face's art draws them
    private static final double OUTER = 0.94;
    private static final double BETWEEN = 0.58;
    private static final double INNER = 0.28;
    private static final int OUTER_BUTTONS = 26;
    private static final int INNER_BUTTONS = Symbol.COUNT - DhdScreen.OUTER_BUTTONS;
    // The most symbols any address has, entered when the linked gate isn't loaded here to say how many it can dial
    private static final int MAX_SYMBOLS = 9;
    // The face's size on screen, at most, and the room left round it
    private static final int MAX_SIZE = 460;
    private static final int MARGIN = 44;

    private static final Identifier GLOW = DhdScreen.texture("glow");
    private static final Identifier DOME = DhdScreen.texture("dome");
    private static final Identifier OUTLINE_OUTER = DhdScreen.texture("outline_outer");
    private static final Identifier OUTLINE_INNER = DhdScreen.texture("outline_inner");

    // How long a button stays pressed in after a click, at least, in milliseconds
    private static final long PRESS_MILLIS = 160;
    private static final int NONE = -2;
    // How long a key with no symbol shows in red, in milliseconds
    private static final long REJECT_MILLIS = 600;

    private final BlockPos pos;
    private final List<Symbol> entered = new ArrayList<>();
    private int size;
    private int centerX;
    private int centerY;
    // The button pressed in, when it was pressed, and whether the mouse still holds it
    private int pressed = DhdScreen.NONE;
    private long pressedAt;
    private boolean held;
    // The last key typed that has no symbol, and when
    private @Nullable String rejected;
    private long rejectedAt;

    public DhdScreen(final BlockPos pos) {
        super(Component.translatableWithFallback("screen.sgcraft.dhd", "DHD"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        this.size = Math.min(DhdScreen.MAX_SIZE, Math.min(this.width - 2 * DhdScreen.MARGIN,
                this.height - 2 * DhdScreen.MARGIN));
        this.centerX = this.width / 2;
        this.centerY = this.height / 2;
        // Typed characters only reach a screen that has asked for text input
        this.minecraft.textInputManager().startTextInput(this);
        this.addRenderableWidget(Button.builder(Component.translatableWithFallback("screen.sgcraft.dhd.close",
                "Close"), button -> this.onClose())
                .bounds(this.centerX - 48, this.height - DhdScreen.MARGIN / 2 - 10, 96, 20).build());
    }

    @Override
    public void removed() {
        this.minecraft.textInputManager().stopTextInput(this);
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        // The DHD has gone, or the player has walked away from it
        if (this.minecraft == null || this.minecraft.player == null || this.dhd() == null
                || !this.pos.closerToCenterThan(this.minecraft.player.position(), 8)) {
            this.onClose();
        }
    }

    private @Nullable DhdBlockEntity dhd() {
        return this.minecraft != null && this.minecraft.level != null
                && this.minecraft.level.getBlockEntity(this.pos) instanceof DhdBlockEntity dhd ? dhd : null;
    }

    private DialStyle style() {
        final DhdBlockEntity dhd = this.dhd();
        return DialStyle.of(dhd != null ? dhd.style() : GateTypes.MILKY_WAY);
    }

    // What the linked gate is doing, as its base shows it here
    private DialView gateView() {
        final StargateBaseBlockEntity gate = this.gate();
        return gate != null ? gate.dialView() : DialView.IDLE;
    }

    // The base of the gate the DHD is linked to, if it is loaded here
    private @Nullable StargateBaseBlockEntity gate() {
        final DhdBlockEntity dhd = this.dhd();
        if (dhd == null || dhd.linked().isEmpty() || this.minecraft == null || this.minecraft.level == null
                || !(this.minecraft.level.getBlockEntity(dhd.linked().get()) instanceof StargateBaseBlockEntity gate)) {
            return null;
        }
        return gate;
    }

    // As many symbols as the linked gate has chevrons, so no more are entered than it can dial
    private int symbolLimit() {
        final StargateBaseBlockEntity gate = this.gate();
        return gate != null ? gate.chevrons() : DhdScreen.MAX_SYMBOLS;
    }

    // The symbols lit on the buttons: the address the gate dialed while it is active, or those entered
    private List<Symbol> lit() {
        final DialView view = this.gateView();
        if (view.active()) {
            return view.initiator() ? view.symbols() : List.of();
        }
        return this.entered;
    }

    // Whether a button is pressed in now
    private boolean isPressed(final int button) {
        return this.pressed == button && (this.held || Util.getMillis() - this.pressedAt < DhdScreen.PRESS_MILLIS);
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
            final float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        final DialStyle style = this.style();
        final boolean pegasus = style == DialStyle.PEGASUS;
        final int half = this.size / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, DhdScreen.texture(pegasus ? "pegasus_face" : "milkyway_face"),
                this.centerX - half, this.centerY - half, 0, 0, this.size, this.size, 512, 512, 512, 512);
        final List<Symbol> lit = this.lit();
        final int glowColor = pegasus ? ARGB.color(255, 60, 170, 255) : ARGB.color(255, 255, 140, 20);
        final int hovered = this.buttonAt(mouseX, mouseY);
        for (int index = 0; index < Symbol.COUNT; index++) {
            final Symbol symbol = new Symbol(index);
            final boolean on = lit.contains(symbol);
            final boolean down = this.isPressed(index);
            if (down) {
                // Pressed in: the button's edges fall into shadow
                this.drawOutline(graphics, index, ARGB.color(230, 0, 0, 0));
            }
            if (on || index == hovered) {
                this.drawOutline(graphics, index, on ? glowColor : ARGB.color(200, 255, 255, 255));
            }
            final double[] at = this.buttonCenter(index);
            final int full = (int) (this.size * (index < DhdScreen.OUTER_BUTTONS ? 0.085 : 0.075));
            // A button pressed in sits lower and further away, so its glyph is a little smaller and lower
            final int glyph = down ? full * 7 / 8 : full;
            final int x = (int) Math.round(at[0]);
            final int y = (int) Math.round(at[1]) + (down ? 1 : 0);
            if (on) {
                final int g = glyph * 2;
                graphics.blit(RenderPipelines.GUI_TEXTURED, DhdScreen.GLOW, x - g / 2, y - g / 2, 0, 0, g, g, 128, 128,
                        128, 128, glowColor);
            }
            // Unlit, as the gate shows its symbols; lit, bright in the style's color
            final int color;
            if (pegasus) {
                color = on ? ARGB.color(255, 170, 245, 255) : ARGB.color(255, 80, 150, 165);
            } else {
                color = on ? ARGB.color(255, 255, 210, 140) : ARGB.color(255, 150, 145, 135);
            }
            this.drawGlyph(graphics, symbol, pegasus, x - glyph / 2, y - glyph / 2, glyph,
                    down ? ARGB.scaleRGB(color, 0.7F) : color);
        }
        // The dome: dim while the gate is idle, lit while it is active
        final boolean active = this.gateView().active() || !this.entered.isEmpty()
                || hovered == DhdPress.CENTER;
        final boolean domeDown = this.isPressed(DhdPress.CENTER);
        final int dome = (int) (this.size * DhdScreen.INNER * (domeDown ? 0.86 : 0.92));
        final int domeTint = pegasus
                ? (active ? ARGB.color(255, 70, 190, 255) : ARGB.color(255, 25, 70, 110))
                : (active ? ARGB.color(255, 255, 150, 30) : ARGB.color(255, 110, 55, 15));
        final int domeColor = domeDown ? ARGB.scaleRGB(domeTint, 0.75F) : domeTint;
        if (active) {
            final int g = (int) (dome * 1.5);
            graphics.blit(RenderPipelines.GUI_TEXTURED, DhdScreen.GLOW, this.centerX - g / 2, this.centerY - g / 2, 0,
                    0, g, g, 128, 128, 128, 128, glowColor);
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, DhdScreen.DOME, this.centerX - dome / 2,
                this.centerY - dome / 2 + (domeDown ? 1 : 0), 0, 0, dome, dome, 128, 128, 128, 128, domeColor);
        // The address so far, above the DHD
        final StringBuilder address = new StringBuilder();
        if (!lit.isEmpty()) {
            address.append(new Address(lit));
        }
        // A key with no symbol shows in red after the address for a moment
        final boolean rejecting = this.rejected != null
                && Util.getMillis() - this.rejectedAt < DhdScreen.REJECT_MILLIS;
        final Component line = Component.literal(address.toString()).append(rejecting
                ? Component.literal(address.isEmpty() ? this.rejected : " " + this.rejected).withColor(0xFFFF5555)
                : Component.empty());
        graphics.centeredText(this.font, line, this.centerX, this.centerY - half - 16, 0xFFFFFFFF);
        graphics.centeredText(this.font, this.title, this.centerX, 12, 0xFFFFFFFF);
    }

    // A button's outline, drawn for the first button of its ring and turned round to this one
    private void drawOutline(final GuiGraphicsExtractor graphics, final int index, final int color) {
        final boolean outer = index < DhdScreen.OUTER_BUTTONS;
        final int count = outer ? DhdScreen.OUTER_BUTTONS : DhdScreen.INNER_BUTTONS;
        final int slot = outer ? index : index - DhdScreen.OUTER_BUTTONS;
        final int half = this.size / 2;
        graphics.pose().pushMatrix();
        graphics.pose().translate(this.centerX, this.centerY);
        graphics.pose().rotate((float) (2 * Math.PI * slot / count));
        graphics.blit(RenderPipelines.GUI_TEXTURED, outer ? DhdScreen.OUTLINE_OUTER : DhdScreen.OUTLINE_INNER, -half,
                -half, 0, 0, this.size, this.size, 512, 512, 512, 512, color);
        graphics.pose().popMatrix();
    }

    // A symbol's glyph, from its style's sheet. Pegasus has no @, + or . of its own, so it borrows the Milky Way's
    private void drawGlyph(final GuiGraphicsExtractor graphics, final Symbol symbol, final boolean pegasus,
            final int x, final int y, final int size, final int color) {
        if (pegasus && symbol.index() < Symbol.GIZA.index()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, DhdScreen.texture("pegasus_glyphs"), x, y,
                    symbol.index() % 6 * 128, symbol.index() / 6 * 128, size, size, 128, 128, 768, 768, color);
            return;
        }
        // The Milky Way sheet: ten to a row, in the order of the symbols
        final int cell = symbol.index();
        graphics.blit(RenderPipelines.GUI_TEXTURED, DhdScreen.texture("milkyway_glyphs"), x, y, cell % 10 * 48,
                cell / 10 * 48, size, size, 48, 48, 512, 256, color);
    }

    // The middle of a symbol's button: A to Z round the outer ring and 0 to 9, @, + and . round the inner, each
    // clockwise from the top
    private double[] buttonCenter(final int index) {
        final boolean outer = index < DhdScreen.OUTER_BUTTONS;
        final int count = outer ? DhdScreen.OUTER_BUTTONS : DhdScreen.INNER_BUTTONS;
        final int slot = outer ? index : index - DhdScreen.OUTER_BUTTONS;
        final double radius = (outer ? (DhdScreen.BETWEEN + DhdScreen.OUTER) : (DhdScreen.INNER + DhdScreen.BETWEEN))
                / 2 * this.size / 2;
        final double angle = 2 * Math.PI * (slot + 0.5) / count;
        return new double[] {this.centerX + radius * Math.sin(angle), this.centerY - radius * Math.cos(angle)};
    }

    // The button at a point: a symbol's index, DhdPress.CENTER for the dome, or NONE
    private int buttonAt(final double x, final double y) {
        final double dx = x - this.centerX;
        final double dy = y - this.centerY;
        final double r = Math.hypot(dx, dy) / (this.size / 2.0);
        if (r < DhdScreen.INNER) {
            return DhdPress.CENTER;
        }
        if (r > DhdScreen.OUTER) {
            return DhdScreen.NONE;
        }
        final double turn = ((Math.atan2(dx, -dy) / (2 * Math.PI)) % 1 + 1) % 1;
        if (r >= DhdScreen.BETWEEN) {
            return (int) (turn * DhdScreen.OUTER_BUTTONS) % DhdScreen.OUTER_BUTTONS;
        }
        return DhdScreen.OUTER_BUTTONS + (int) (turn * DhdScreen.INNER_BUTTONS) % DhdScreen.INNER_BUTTONS;
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        final int button = this.buttonAt(event.x(), event.y());
        if (button == DhdScreen.NONE) {
            return false;
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            // A right click takes back the last symbol entered
            this.takeBack();
            return true;
        }
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        this.press(button, true);
        return true;
    }

    // Presses a button, as a click or a key does
    private void press(final int button, final boolean held) {
        this.pressed = button;
        this.pressedAt = Util.getMillis();
        this.held = held;
        final boolean active = this.gateView().active();
        if (button == DhdPress.CENTER) {
            this.click(true);
            if (active) {
                // The dome shuts an active gate down
                ClientPlatform.get().sendToServer(new DhdPress(this.pos, DhdPress.CENTER));
            } else if (!this.entered.isEmpty()) {
                ClientPlatform.get().sendToServer(new DhdDial(this.pos, new Address(this.entered).toString()));
            }
            this.entered.clear();
            return;
        }
        this.click(false);
        if (!active && this.entered.size() < this.symbolLimit()) {
            this.entered.add(new Symbol(button));
        }
    }

    // Takes back the last symbol entered
    private void takeBack() {
        if (!this.entered.isEmpty() && !this.gateView().active()) {
            this.entered.removeLast();
            this.click(false);
        }
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
            this.press(DhdPress.CENTER, false);
            return true;
        }
        if (event.key() == InputConstants.KEY_BACKSPACE) {
            this.takeBack();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(final CharacterEvent event) {
        if (super.charTyped(event)) {
            return true;
        }
        final String typed = event.codepointAsString();
        if (typed.isBlank()) {
            return false;
        }
        // Letters need no shift: Symbol reads either case as the capital
        final Optional<Symbol> symbol = typed.length() == 1 ? Symbol.of(typed.charAt(0)) : Optional.empty();
        if (symbol.isPresent()) {
            this.press(symbol.get().index(), false);
        } else {
            this.rejected = typed;
            this.rejectedAt = Util.getMillis();
        }
        return true;
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        this.held = false;
        return super.mouseReleased(event);
    }

    private void click(final boolean center) {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SGSounds.dhdPress(this.style(), center),
                    1.0F));
        }
    }

    private static Identifier texture(final String name) {
        return SGCraft.id("textures/gui/dhd/" + name + ".png");
    }
}
