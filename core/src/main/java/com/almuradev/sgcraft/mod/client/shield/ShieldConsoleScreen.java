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

package com.almuradev.sgcraft.mod.client.shield;

import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.gate.IrisState;
import com.almuradev.sgcraft.mod.client.ClientPlatform;
import com.almuradev.sgcraft.mod.client.tool.PddScreen;
import com.almuradev.sgcraft.mod.gate.GlyphText;
import com.almuradev.sgcraft.mod.gate.SGIris;
import com.almuradev.sgcraft.mod.shield.ShieldPayloads.ConsoleAction;
import com.almuradev.sgcraft.mod.shield.ShieldPayloads.ConsoleState;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.GateInfo;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * A shield console's screen, as the original's: the shielded gate it works, its address if that is shown, whether the
 * shield is up, and buttons to raise and lower it. It keeps up with the shield while it is open.
 */
public final class ShieldConsoleScreen extends Screen {

    private static final int WIDTH = 200;
    private static final int REFRESH_TICKS = 10;

    private ConsoleState state;
    private @Nullable Button raise;
    private @Nullable Button lower;
    private int ticks;

    public ShieldConsoleScreen(final ConsoleState state) {
        super(Component.translatableWithFallback("block.sgcraft.shield_console", "Shield Console"));
        this.state = state;
    }

    public void update(final ConsoleState state) {
        this.state = state;
        this.updateButtons();
    }

    @Override
    protected void init() {
        final int left = this.width / 2 - ShieldConsoleScreen.WIDTH / 2;
        final int y = this.height / 2 + 16;
        this.raise = this.addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "screen.sgcraft.gdo.raise_shield", "Activate Shield"), b -> this.send(ConsoleAction.Kind.RAISE))
                .bounds(left, y, ShieldConsoleScreen.WIDTH / 2 - 2, 20).build());
        this.lower = this.addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "screen.sgcraft.gdo.lower_shield", "Deactivate Shield"), b -> this.send(ConsoleAction.Kind.LOWER))
                .bounds(left + ShieldConsoleScreen.WIDTH / 2 + 2, y, ShieldConsoleScreen.WIDTH / 2 - 2, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatableWithFallback("screen.sgcraft.close", "Close"),
                b -> this.onClose()).bounds(this.width / 2 - 50, y + 28, 100, 20).build());
        this.updateButtons();
    }

    private void send(final ConsoleAction.Kind kind) {
        ClientPlatform.get().sendToServer(new ConsoleAction(this.state.console(), kind));
    }

    // A lowered shield can only be raised, and a raised one lowered; one moving waits until it stops
    private void updateButtons() {
        final int iris = this.state.gate().map(GateInfo::iris).orElse(GateInfo.NO_IRIS);
        if (this.raise != null) {
            this.raise.active = this.state.control() && iris == IrisState.OPEN.ordinal();
        }
        if (this.lower != null) {
            this.lower.active = this.state.control() && iris == IrisState.CLOSED.ordinal();
        }
    }

    @Override
    public void tick() {
        if (++this.ticks % ShieldConsoleScreen.REFRESH_TICKS == 0) {
            this.send(ConsoleAction.Kind.REFRESH);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
            final float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        final int x = this.width / 2;
        final int top = this.height / 2 - 50;
        graphics.centeredText(this.font, this.title, x, top, 0xFFFFFFFF);
        if (this.state.gate().isEmpty()) {
            graphics.centeredText(this.font, Component.translatableWithFallback("screen.sgcraft.shield.no_gate",
                    "No shielded Stargate within %s blocks", this.state.range()), x, top + 30, 0xFFFF6060);
        } else {
            final GateInfo gate = this.state.gate().get();
            if (!gate.address().isEmpty()) {
                graphics.centeredText(this.font, gate.address(), x, top + 14, 0xFF8FB8FF);
                Address.parse(gate.address()).ifPresent(address -> graphics.centeredText(this.font,
                        GlyphText.of(address, PddScreen.style(gate)), x, top + 26, -1));
            }
            final IrisState iris = IrisState.values()[Math.max(0, gate.iris())];
            final Component status = this.state.control() ? SGIris.shieldText(iris)
                    : Component.translatableWithFallback("screen.sgcraft.shield.no_access",
                            "You don't have access to this Stargate's shield");
            final int color = !this.state.control() ? 0xFFFF6060 : iris == IrisState.CLOSED ? 0xFF55FFFF
                    : 0xFFAAAAAA;
            graphics.centeredText(this.font, status, x, top + 42, color);
        }
        if (!this.state.message().isEmpty()) {
            graphics.centeredText(this.font, this.state.message(), x, this.height - 28, 0xFFFF6060);
        }
    }
}
