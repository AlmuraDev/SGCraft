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

package com.almuradev.sgcraft.mod.client.tool;

import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.gate.IrisState;
import com.almuradev.sgcraft.mod.client.ClientPlatform;
import com.almuradev.sgcraft.mod.gate.GlyphText;
import com.almuradev.sgcraft.mod.gate.SGIris;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.GateInfo;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.GdoAction;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.GdoState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * The GDO, the garage door opener: works the iris of the gate near the player and, once its wormhole is open, the
 * iris of the gate at the other end, as a GDO sends its code through on the show. It can also shut the wormhole.
 */
public final class GdoScreen extends Screen {

    private static final int PANEL = 180;
    private static final int REFRESH_TICKS = 10;

    private GdoState state;
    private @Nullable Button openLocal;
    private @Nullable Button closeLocal;
    private @Nullable Button disconnect;
    private @Nullable Button openRemote;
    private @Nullable Button closeRemote;
    private int ticks;

    public GdoScreen(final GdoState state) {
        super(Component.translatableWithFallback("screen.sgcraft.gdo", "GDO"));
        this.state = state;
    }

    public void update(final GdoState state) {
        this.state = state;
        this.updateButtons();
    }

    @Override
    protected void init() {
        final int localX = this.width / 2 - GdoScreen.PANEL - 10;
        final int remoteX = this.width / 2 + 10;
        final int y = this.height / 2 + 10;
        this.openLocal = this.addRenderableWidget(this.button("screen.sgcraft.gdo.open", "Open Iris", localX, y,
                GdoAction.Kind.OPEN_LOCAL));
        this.closeLocal = this.addRenderableWidget(this.button("screen.sgcraft.gdo.close", "Close Iris",
                localX + GdoScreen.PANEL / 2 + 2, y, GdoAction.Kind.CLOSE_LOCAL));
        this.disconnect = this.addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "screen.sgcraft.gdo.disconnect", "Disconnect"), b -> this.send(GdoAction.Kind.DISCONNECT))
                .bounds(localX, y + 24, GdoScreen.PANEL, 20).build());
        this.openRemote = this.addRenderableWidget(this.button("screen.sgcraft.gdo.open", "Open Iris", remoteX, y,
                GdoAction.Kind.OPEN_REMOTE));
        this.closeRemote = this.addRenderableWidget(this.button("screen.sgcraft.gdo.close", "Close Iris",
                remoteX + GdoScreen.PANEL / 2 + 2, y, GdoAction.Kind.CLOSE_REMOTE));
        this.addRenderableWidget(Button.builder(Component.translatableWithFallback("screen.sgcraft.close", "Close"),
                b -> this.onClose()).bounds(this.width / 2 - 50, this.height - 28, 100, 20).build());
        this.updateButtons();
    }

    private Button button(final String key, final String fallback, final int x, final int y,
            final GdoAction.Kind kind) {
        return Button.builder(Component.translatableWithFallback(key, fallback), b -> this.send(kind))
                .bounds(x, y, GdoScreen.PANEL / 2 - 2, 20).build();
    }

    private void send(final GdoAction.Kind kind) {
        ClientPlatform.get().sendToServer(new GdoAction(this.state.local().pos(), kind));
    }

    // An iris that is moving takes no orders until it stops; one that is shut can only open, and the other way. A
    // shield is raised by closing it and lowered by opening it, and its buttons say so
    private static void irisButtons(final @Nullable Button open, final @Nullable Button close,
            final @Nullable GateInfo gate) {
        final int iris = gate == null ? GateInfo.NO_IRIS : gate.iris();
        final boolean shield = gate != null && gate.shield();
        if (open != null) {
            open.visible = gate != null;
            open.active = iris == IrisState.CLOSED.ordinal();
            open.setMessage(shield ? Component.translatableWithFallback("screen.sgcraft.gdo.lower_shield",
                    "Deactivate Shield") : Component.translatableWithFallback("screen.sgcraft.gdo.open", "Open Iris"));
        }
        if (close != null) {
            close.visible = gate != null;
            close.active = iris == IrisState.OPEN.ordinal();
            close.setMessage(shield ? Component.translatableWithFallback("screen.sgcraft.gdo.raise_shield",
                    "Activate Shield") : Component.translatableWithFallback("screen.sgcraft.gdo.close", "Close Iris"));
        }
    }

    private void updateButtons() {
        GdoScreen.irisButtons(this.openLocal, this.closeLocal, this.state.local());
        GdoScreen.irisButtons(this.openRemote, this.closeRemote, this.state.remote().orElse(null));
        if (this.disconnect != null) {
            this.disconnect.active = this.state.canDisconnect();
        }
    }

    @Override
    public void tick() {
        if (++this.ticks % GdoScreen.REFRESH_TICKS == 0) {
            this.send(GdoAction.Kind.REFRESH);
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
        graphics.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);
        final int localX = this.width / 2 - GdoScreen.PANEL / 2 - 10;
        final int remoteX = this.width / 2 + GdoScreen.PANEL / 2 + 10;
        final int top = this.height / 2 - 60;
        this.panel(graphics, localX, top, Component.translatableWithFallback("screen.sgcraft.gdo.local",
                "This gate"), this.state.local());
        if (this.state.remote().isPresent()) {
            this.panel(graphics, remoteX, top, Component.translatableWithFallback("screen.sgcraft.gdo.remote",
                    "The other end"), this.state.remote().get());
        } else {
            graphics.centeredText(this.font, Component.translatableWithFallback("screen.sgcraft.gdo.no_wormhole",
                    "No wormhole open"), remoteX, top + 30, 0xFF808080);
        }
        if (!this.state.message().isEmpty()) {
            graphics.centeredText(this.font, this.state.message(), this.width / 2, this.height - 44, 0xFFFF6060);
        }
    }

    private void panel(final GuiGraphicsExtractor graphics, final int x, final int top, final Component heading,
            final GateInfo gate) {
        graphics.centeredText(this.font, heading, x, top, 0xFFFFFFFF);
        graphics.centeredText(this.font, gate.address().isEmpty() ? "????-???-??" : gate.address(), x, top + 14,
                0xFF8FB8FF);
        Address.parse(gate.address()).ifPresent(address ->
                graphics.centeredText(this.font, GlyphText.of(address, PddScreen.style(gate)), x, top + 26, -1));
        final Component iris = gate.iris() == GateInfo.NO_IRIS
                ? Component.translatableWithFallback("screen.sgcraft.gdo.no_iris", "No iris")
                : gate.shield() ? SGIris.shieldText(IrisState.values()[gate.iris()])
                : Component.translatableWithFallback("screen.sgcraft.gdo.iris", "Iris: %s",
                        IrisState.values()[gate.iris()].name().toLowerCase(Locale.ROOT));
        graphics.centeredText(this.font, iris, x, top + 42, gate.iris() == IrisState.CLOSED.ordinal() ? 0xFFFF8040
                : 0xFFA0FFA0);
        graphics.centeredText(this.font, gate.state(), x, top + 54, 0xFFAAAAAA);
    }
}
