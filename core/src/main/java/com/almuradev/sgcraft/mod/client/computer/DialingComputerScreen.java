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

import com.almuradev.sgcraft.api.address.Symbol;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.block.entity.DialingComputerBlockEntity;
import com.almuradev.sgcraft.mod.client.ClientPlatform;
import com.almuradev.sgcraft.mod.computer.ComputerDialMode;
import com.almuradev.sgcraft.mod.computer.ComputerPayloads;
import com.almuradev.sgcraft.mod.gate.DialPhase;
import com.almuradev.sgcraft.mod.gate.IrisView;
import com.almuradev.sgcraft.mod.sound.SGSounds;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The screen of a player working a dialing computer: the dialing program as the computer's monitor shows it, large,
 * and under it the computer's keyboard, in the program's style. Its keys type the address to dial, each showing its
 * gate's glyph over the key it is typed with, and work the computer: how it dials (as its gate says, slowly, the ring
 * turning to each symbol and each chevron locking in turn as on the show, or quickly, every chevron locking at once),
 * dialing, shutting the gate down, working the iris, and turning the alarm off or on. A key can be clicked or typed,
 * and lights either way; an address can be pasted whole. Beside them the computer's dialing history lists the
 * addresses last dialed from it: a double click on one dials it again, as does choosing one with Tab or the arrow keys
 * and pressing Enter. Closing the screen gets the player up from the chair, or away from the computer.
 */
public final class DialingComputerScreen extends Screen {

    // The most symbols an address has, as for a computer with no gate
    private static final int LIMIT = 9;
    // Where the keyboard starts under the program, and how tall both are, in the program's pixels
    private static final int KEYBOARD_TOP = ComputerDisplay.HEIGHT + 4;
    private static final int TALL = DialingComputerScreen.KEYBOARD_TOP + ComputerKeyboard.HEIGHT;
    // Where the history starts beside the program, and how wide all of it is, in the program's pixels
    private static final int HISTORY_LEFT = ComputerDisplay.WIDTH + 4;
    private static final int WIDE = DialingComputerScreen.HISTORY_LEFT + ComputerHistory.WIDTH;
    // How soon a second click on a history row counts as a double click
    private static final long DOUBLE_CLICK_MILLIS = 400;
    private static final long BLINK_MILLIS = 500;
    // How long a notice, such as why a dial failed, shows, and its colors
    private static final long NOTICE_MILLIS = 4000;
    private static final int NOTICE_BACKDROP = 0xE0040E1A;
    private static final int NOTICE_EDGE = 0xFFE8463C;

    private final BlockPos computer;
    private final List<Symbol> typed = new ArrayList<>();
    private final ComputerKeyboard keyboard = new ComputerKeyboard();
    private final Picture keyboardPicture;
    private final Picture historyPicture;
    // The history row chosen to dial again, or -1, and the last row clicked, and when
    private int selected = -1;
    private int clicked = -1;
    private long clickedAt;
    private @Nullable Component notice;
    private long noticeUntil;
    // Where the program and the keyboard are drawn, and how large each pixel of theirs is
    private int left;
    private int top;
    private float scale;

    public DialingComputerScreen(final ComputerPayloads.Screen open) {
        super(Component.translatableWithFallback("screen.sgcraft.dialing_computer", "Dialing Computer"));
        this.computer = open.computer();
        final String pos = Long.toHexString(this.computer.asLong()).toLowerCase(Locale.ROOT);
        this.keyboardPicture = new Picture("keyboard_" + pos, ComputerKeyboard.WIDTH, ComputerKeyboard.HEIGHT);
        this.historyPicture = new Picture("history_" + pos, ComputerHistory.WIDTH, ComputerHistory.HEIGHT);
    }

    /** @return the computer the screen works */
    public BlockPos computer() {
        return this.computer;
    }

    /** Shows something the computer tells the player, such as why a dial failed, for a few seconds. */
    public void notice(final Component text) {
        this.notice = text;
        this.noticeUntil = Util.getMillis() + DialingComputerScreen.NOTICE_MILLIS;
    }

    // Each of the program's pixels is drawn as a whole number of the window's, so its pixel font and glyphs stay sharp,
    // unless the window is too small for even one
    @Override
    protected void init() {
        final int tall = DialingComputerScreen.TALL;
        final int wide = DialingComputerScreen.WIDE;
        final int gui = this.minecraft.getWindow().getGuiScale();
        final int pixels = Math.min((this.width - 20) * gui / wide, (this.height - 20) * gui / tall);
        this.scale = pixels >= 1 ? pixels / (float) gui
                : Math.min((this.width - 20) / (float) wide, (this.height - 20) / (float) tall);
        this.left = Math.round((this.width - wide * this.scale) / 2);
        this.top = Math.round((this.height - tall * this.scale) / 2);
        // The window sends typed characters only while something takes text, as a text box does when focused
        this.minecraft.textInputManager().startTextInput(this);
    }

    private @Nullable DialingComputerBlockEntity entity() {
        return this.minecraft == null || this.minecraft.level == null ? null
                : this.minecraft.level.getBlockEntity(this.computer) instanceof final DialingComputerBlockEntity entity
                        ? entity : null;
    }

    // How many symbols the computer's gate can dial: one for each of its chevrons
    private int limit() {
        final DialingComputerBlockEntity entity = this.entity();
        if (entity == null || this.minecraft.level == null) {
            return DialingComputerScreen.LIMIT;
        }
        final ComputerDisplay.View view = ComputerDisplay.View.of(this.minecraft.level, entity, 0);
        return view.linked() ? view.chevrons() : DialingComputerScreen.LIMIT;
    }

    // How the computer dials, as the server last said
    private ComputerDialMode computerMode() {
        final DialingComputerBlockEntity entity = this.entity();
        return entity == null ? ComputerDialMode.GATE : entity.mode();
    }

    // The key under a place on the screen, if any
    private ComputerKeyboard.@Nullable Key keyAt(final double x, final double y) {
        return this.keyboard.keyAt((x - this.left) / this.scale,
                (y - this.top) / this.scale - DialingComputerScreen.KEYBOARD_TOP);
    }

    // The addresses last dialed from the computer, newest first
    private List<DialingComputerBlockEntity.Dialed> history() {
        final DialingComputerBlockEntity entity = this.entity();
        return entity == null ? List.of() : entity.history();
    }

    // The history row under a place on the screen, or -1
    private int rowAt(final double x, final double y) {
        return ComputerHistory.rowAt((x - this.left) / this.scale - DialingComputerScreen.HISTORY_LEFT,
                (y - this.top) / this.scale, this.history().size());
    }

    // Dials an address from the history again: it is typed in, then dialed; while the gate is busy it is only typed
    // in, ready to dial once the gate is free
    private void dialAgain(final int row) {
        final List<DialingComputerBlockEntity.Dialed> history = this.history();
        if (row < 0 || row >= history.size()) {
            return;
        }
        this.paste(history.get(row).address());
        this.selected = -1;
        this.press(ComputerKeyboard.Kind.DIAL, null);
    }

    // Chooses the next row of the history, or the one before, going round
    private void choose(final int step) {
        final int rows = this.history().size();
        if (rows == 0) {
            return;
        }
        this.selected = this.selected < 0 ? (step > 0 ? 0 : rows - 1) : Math.floorMod(this.selected + step, rows);
    }

    // The gate's iris, as its base shows it, or null if it has none
    private @Nullable IrisView iris() {
        final DialingComputerBlockEntity entity = this.entity();
        return entity == null || this.minecraft.level == null ? null
                : ComputerDisplay.View.of(this.minecraft.level, entity, 0).iris();
    }

    // Whether the gate is dialing, open or shutting down, as its base shows it, so there is something to abort
    private boolean busy() {
        final DialingComputerBlockEntity entity = this.entity();
        return entity != null && this.minecraft.level != null
                && ComputerDisplay.View.of(this.minecraft.level, entity, 0).dial().phase() != DialPhase.IDLE;
    }

    // Presses a key, as a click or a typed key does: it lights, and does what it does; a key that does nothing now,
    // such as the iris key of a gate with no iris, does nothing at all
    private void press(final ComputerKeyboard.Kind kind, final @Nullable Symbol symbol) {
        if (!ComputerKeyboard.works(kind, this.iris(), this.busy(), !this.typed.isEmpty())) {
            return;
        }
        // Typing an address of one's own leaves the history's
        if (kind == ComputerKeyboard.Kind.SYMBOL || kind == ComputerKeyboard.Kind.BACK
                || kind == ComputerKeyboard.Kind.CLEAR) {
            this.selected = -1;
        }
        this.keyboard.light(kind, symbol);
        if (this.minecraft != null) {
            // A mechanical key's tap, a little different each time; the big keys have their own, deeper ones
            final boolean big = kind != ComputerKeyboard.Kind.SYMBOL;
            final float pitch = 1.0F + (float) (Math.random() - 0.5) * 0.06F;
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(big ? SGSounds.COMPUTER_KEY_BIG
                    : SGSounds.COMPUTER_KEY, pitch, 0.6F));
        }
        switch (kind) {
            case SYMBOL -> {
                if (symbol != null && this.typed.size() < this.limit()) {
                    this.typed.add(symbol);
                }
            }
            case BACK -> {
                if (!this.typed.isEmpty()) {
                    this.typed.removeLast();
                }
            }
            case CLEAR -> this.typed.clear();
            case DIAL -> this.dial();
            // The key's label follows once the server has the computer's new mode
            case MODE -> this.send(ComputerPayloads.Action.Kind.MODE, this.computerMode().next().getSerializedName());
            case IRIS -> this.send(ComputerPayloads.Action.Kind.IRIS, "");
            case ALARM -> this.send(ComputerPayloads.Action.Kind.ALARM, "");
            case ABORT -> this.send(ComputerPayloads.Action.Kind.ABORT, "");
            case LEAVE -> this.onClose();
        }
    }

    private void dial() {
        if (this.typed.isEmpty()) {
            return;
        }
        final StringBuilder address = new StringBuilder();
        this.typed.forEach(symbol -> address.append(symbol.character()));
        this.send(ComputerPayloads.Action.Kind.DIAL, address.toString());
    }

    // Types an address pasted whole, past anything that isn't a symbol, such as the dashes it is written with
    private void paste(final String text) {
        this.typed.clear();
        for (final char character : text.toCharArray()) {
            final Optional<Symbol> symbol = Symbol.of(character);
            if (symbol.isPresent() && this.typed.size() < this.limit()) {
                this.typed.add(symbol.get());
                this.keyboard.light(ComputerKeyboard.Kind.SYMBOL, symbol.get());
            }
        }
    }

    private void send(final ComputerPayloads.Action.Kind kind, final String address) {
        ClientPlatform.get().sendToServer(new ComputerPayloads.Action(this.computer, kind, address));
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        final int row = this.rowAt(event.x(), event.y());
        if (row >= 0) {
            final long now = Util.getMillis();
            if (row == this.clicked && now - this.clickedAt <= DialingComputerScreen.DOUBLE_CLICK_MILLIS) {
                this.clicked = -1;
                this.dialAgain(row);
            } else {
                this.clicked = row;
                this.clickedAt = now;
                this.selected = row;
            }
            return true;
        }
        final ComputerKeyboard.Key key = this.keyAt(event.x(), event.y());
        if (key == null) {
            return false;
        }
        this.press(key.kind(), key.symbol());
        return true;
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        if (event.isPaste()) {
            this.paste(this.minecraft.keyboardHandler.getClipboard());
            return true;
        }
        switch (event.key()) {
            case InputConstants.KEY_TAB -> {
                this.choose(event.hasShiftDown() ? -1 : 1);
                return true;
            }
            case InputConstants.KEY_DOWN -> {
                this.choose(1);
                return true;
            }
            case InputConstants.KEY_UP -> {
                this.choose(-1);
                return true;
            }
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
                // Enter dials the history's chosen address, if one is chosen, or else the one typed
                if (this.selected >= 0) {
                    this.dialAgain(this.selected);
                    return true;
                }
            }
            default -> {
            }
        }
        final ComputerKeyboard.Kind kind = switch (event.key()) {
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> ComputerKeyboard.Kind.DIAL;
            case InputConstants.KEY_BACKSPACE -> ComputerKeyboard.Kind.BACK;
            case InputConstants.KEY_DELETE -> ComputerKeyboard.Kind.CLEAR;
            default -> null;
        };
        if (kind != null) {
            this.press(kind, null);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(final CharacterEvent event) {
        if (super.charTyped(event)) {
            return true;
        }
        // Letters need no shift: Symbol reads either case as the capital
        final String character = event.codepointAsString();
        final Optional<Symbol> symbol = character.length() == 1 ? Symbol.of(character.charAt(0)) : Optional.empty();
        if (symbol.isEmpty()) {
            return false;
        }
        this.press(ComputerKeyboard.Kind.SYMBOL, symbol.get());
        return true;
    }


    @Override
    public void tick() {
        // The computer was broken under the player
        if (this.entity() == null) {
            this.onClose();
            return;
        }
        // A history that has grown shorter loses its choice
        if (this.selected >= this.history().size()) {
            this.selected = -1;
        }
        // A gate with fewer chevrons than the address typed for the last one can't dial all of it
        while (this.typed.size() > this.limit()) {
            this.typed.removeLast();
        }
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
            final float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        final DialingComputerBlockEntity entity = this.entity();
        if (entity == null || this.minecraft.level == null) {
            return;
        }
        final Identifier picture = ComputerDisplays.texture(entity, partialTick);
        final ComputerKeyboard.Key hovered = this.keyAt(mouseX, mouseY);
        final ComputerDisplay.View view = ComputerDisplay.View.of(this.minecraft.level, entity, partialTick);
        final Identifier glyphs = ComputerDisplay.glyphs(view.type());
        this.keyboard.paint(this.keyboardPicture.canvas, glyphs, this.typed, this.limit(),
                hovered != null && ComputerKeyboard.works(hovered.kind(), view.iris(), this.busy(),
                        !this.typed.isEmpty()) ? hovered : null,
                entity.mode(), entity.alarmOff(), view.iris(), this.busy(),
                Util.getMillis() / DialingComputerScreen.BLINK_MILLIS % 2 == 0);
        ComputerHistory.paint(this.historyPicture.canvas, glyphs, entity.history(), this.selected,
                this.rowAt(mouseX, mouseY));
        // Drawn in the program's own pixels, scaled as a whole
        graphics.pose().pushMatrix();
        graphics.pose().translate(this.left, this.top);
        graphics.pose().scale(this.scale, this.scale);
        graphics.blit(RenderPipelines.GUI_TEXTURED, picture, 0, 0, 0, 0, ComputerDisplay.WIDTH,
                ComputerDisplay.HEIGHT, ComputerDisplay.WIDTH, ComputerDisplay.HEIGHT, ComputerDisplay.WIDTH,
                ComputerDisplay.HEIGHT);
        this.keyboardPicture.blit(graphics, this.minecraft, 0, DialingComputerScreen.KEYBOARD_TOP);
        this.historyPicture.blit(graphics, this.minecraft, DialingComputerScreen.HISTORY_LEFT, 0);
        graphics.pose().popMatrix();
        this.extractNotice(graphics);
        if (hovered != null) {
            this.tooltip(graphics, hovered.kind(), mouseX, mouseY);
        }
    }

    // A notice, boxed in red across the bottom of the program, over its status line
    private void extractNotice(final GuiGraphicsExtractor graphics) {
        if (this.notice == null || Util.getMillis() >= this.noticeUntil) {
            return;
        }
        final int width = this.font.width(this.notice) + 12;
        final int middle = this.left + Math.round(ComputerDisplay.WIDTH * this.scale / 2);
        final int x = middle - width / 2;
        final int y = this.top + Math.round(ComputerDisplay.HEIGHT * this.scale) - 22;
        graphics.fill(x, y, x + width, y + 18, DialingComputerScreen.NOTICE_BACKDROP);
        graphics.outline(x, y, width, 18, DialingComputerScreen.NOTICE_EDGE);
        graphics.centeredText(this.font, this.notice, middle, y + 5, DialingComputerScreen.NOTICE_EDGE);
    }

    // A picture painted in the program's pixels beside it, such as the keyboard, handed to its texture each frame
    private static final class Picture {

        final ComputerCanvas canvas;
        private final Identifier id;
        private @Nullable DynamicTexture texture;

        Picture(final String name, final int width, final int height) {
            this.canvas = new ComputerCanvas(width, height);
            this.id = SGCraft.id("dynamic/dialing_computer_" + name);
        }

        // Draws the picture as it is now, its top left at a place in the program's pixels
        void blit(final GuiGraphicsExtractor graphics, final Minecraft minecraft, final int x, final int y) {
            final int width = this.canvas.width;
            final int height = this.canvas.height;
            if (this.texture == null) {
                this.texture = new DynamicTexture(() -> "SGCraft dialing computer " + this.id.getPath(), width,
                        height, true);
                minecraft.getTextureManager().register(this.id, this.texture);
            }
            this.canvas.copyTo(this.texture.getPixels());
            this.texture.upload();
            graphics.blit(RenderPipelines.GUI_TEXTURED, this.id, x, y, 0, 0, width, height, width, height, width,
                    height);
        }

        void release(final Minecraft minecraft) {
            if (this.texture != null) {
                // Closes the texture too
                minecraft.getTextureManager().release(this.id);
                this.texture = null;
            }
        }
    }

    // What the keys that need saying do
    private void tooltip(final GuiGraphicsExtractor graphics, final ComputerKeyboard.Kind kind, final int mouseX,
            final int mouseY) {
        final Component tip = switch (kind) {
            case MODE -> Component.translatableWithFallback("screen.sgcraft.dialing_computer.mode.tip",
                    "How this computer dials: as its gate's settings say, slowly (a chevron at a time) or "
                            + "quickly (every chevron at once)");
            case IRIS -> this.iris() == null
                    ? Component.translatableWithFallback("screen.sgcraft.dialing_computer.iris.none",
                            "This gate has no iris")
                    : Component.translatableWithFallback("screen.sgcraft.dialing_computer.iris.tip",
                            "Shuts the gate's iris, or raises its shield, when it is open, and opens it when it is "
                                    + "shut. It shows red while open, since anything can come through");
            case ALARM -> Component.translatableWithFallback("screen.sgcraft.dialing_computer.alarm.tip",
                    "Turns off the alarm that sounds while a wormhole comes in, or turns it on again. It stays that "
                            + "way until changed");
            default -> null;
        };
        if (tip != null) {
            graphics.setTooltipForNextFrame(this.font.split(tip, 200), mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Closing the screen, however it is closed, gets the player up, or away from the computer
    @Override
    public void removed() {
        this.send(ComputerPayloads.Action.Kind.CLOSE, "");
        this.minecraft.textInputManager().stopTextInput(this);
        this.keyboardPicture.release(this.minecraft);
        this.historyPicture.release(this.minecraft);
    }
}
