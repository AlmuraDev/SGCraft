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

package com.almuradev.sgcraft.mod.client.strata.shield;

import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.gate.IrisState;
import com.almuradev.sgcraft.mod.client.ClientPlatform;
import com.almuradev.sgcraft.mod.client.strata.SGStrata;
import com.almuradev.sgcraft.mod.client.tool.PddScreen;
import com.almuradev.sgcraft.mod.gate.SGIris;
import com.almuradev.sgcraft.mod.shield.ShieldPayloads.ConsoleAction;
import com.almuradev.sgcraft.mod.shield.ShieldPayloads.ConsoleState;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.GateInfo;
import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.text.Text;
import org.jspecify.annotations.Nullable;

/**
 * A shield console drawn with Strata, laid out as SGCraft 2.3's MalisisCore one was: the shielded gate's address,
 * large and blue, with its glyphs under it, whether the shield is up below them, what the server last said, and
 * Activate and Deactivate in the bottom corners. It asks the server for the console's state twice a second, as the
 * standard screen does.
 */
public final class StrataShieldConsoleScreen implements Screen {

    private static final float WIDTH = 230f;
    private static final float HEIGHT = 124f;
    private static final float BUTTON_WIDTH = 100f;
    private static final long REFRESH_MILLIS = 500L;

    private ConsoleState state;
    private @Nullable Label address;
    private @Nullable Label glyphs;
    private @Nullable Label status;
    private @Nullable Label message;
    private @Nullable Button raise;
    private @Nullable Button lower;

    public StrataShieldConsoleScreen(final ConsoleState state) {
        this.state = state;
    }

    @Override
    public Text title() {
        return SGStrata.text("block.sgcraft.shield_console", "Shield Console");
    }

    /** Shows a newer state from the server. */
    public void update(final ConsoleState state) {
        this.state = state;
        this.refresh();
    }

    @Override
    public Component<?> build(final ComponentFactory factory) {
        this.address = factory.label("").setTextScale(1.3f).setTextAlignment(HorizontalAlignment.CENTER);
        this.glyphs = factory.label("").setTextAlignment(HorizontalAlignment.CENTER);
        this.status = factory.label("").setTextScale(1.1f).setTextAlignment(HorizontalAlignment.CENTER);
        this.message = factory.label("").setTextScale(0.8f).setWrap(true)
                .setTextAlignment(HorizontalAlignment.CENTER);
        this.raise = SGStrata.button(factory, SGStrata.text("screen.sgcraft.gdo.raise_shield", "Activate Shield"),
                StrataShieldConsoleScreen.BUTTON_WIDTH, () -> this.send(ConsoleAction.Kind.RAISE));
        this.lower = SGStrata.button(factory, SGStrata.text("screen.sgcraft.gdo.lower_shield", "Deactivate Shield"),
                StrataShieldConsoleScreen.BUTTON_WIDTH, () -> this.send(ConsoleAction.Kind.LOWER));
        final float inner = StrataShieldConsoleScreen.WIDTH - 8f;
        final AnchorPanel content = factory.anchorPanel()
                .add(this.address.setAnchor(Anchor.TOP_CENTER).setMargin(0f, 4f, 0f, 0f).setWidth(inner))
                .add(this.glyphs.setAnchor(Anchor.TOP_CENTER).setMargin(0f, 20f, 0f, 0f).setWidth(inner))
                .add(this.status.setAnchor(Anchor.TOP_CENTER).setMargin(0f, 36f, 0f, 0f).setWidth(inner))
                .add(this.message.setAnchor(Anchor.TOP_CENTER).setMargin(0f, 54f, 0f, 0f).setWidth(inner))
                .add(this.raise.setAnchor(Anchor.BOTTOM_LEFT))
                .add(this.lower.setAnchor(Anchor.BOTTOM_RIGHT));
        final Component<?> window = SGStrata.window(factory, this.title(), content, StrataShieldConsoleScreen.WIDTH,
                StrataShieldConsoleScreen.HEIGHT);
        this.refresh();
        return factory.anchorPanel().add(window).setAnchor(Anchor.FILL);
    }

    @Override
    public void opened(final ScreenHandle handle) {
        handle.every(StrataShieldConsoleScreen.REFRESH_MILLIS, () -> this.send(ConsoleAction.Kind.REFRESH));
    }

    private void send(final ConsoleAction.Kind kind) {
        ClientPlatform.get().sendToServer(new ConsoleAction(this.state.console(), kind));
    }

    // The gate's address and glyphs if they are shown, the shield's state as 2.3 coloured it, and the buttons: a
    // lowered shield can only be raised, and a raised one lowered; one moving waits until it stops
    private void refresh() {
        if (this.address == null || this.glyphs == null || this.status == null || this.message == null
                || this.raise == null || this.lower == null) {
            return;
        }
        final GateInfo gate = this.state.gate().orElse(null);
        if (gate == null) {
            this.address.setText(Text.empty());
            this.glyphs.setText(Text.empty());
            this.status.setText(SGStrata.text("screen.sgcraft.shield.no_gate",
                    "No gate with a shield within %s blocks", this.state.range()).color(SGStrata.GRAY));
        } else {
            this.address.setText(Text.of(gate.address()).color(SGStrata.BLUE));
            this.glyphs.setText(Address.parse(gate.address()).map(parsed -> SGStrata.glyphs(parsed,
                    PddScreen.style(gate))).orElse(Text.empty()));
            final IrisState iris = IrisState.values()[Math.max(0, gate.iris())];
            if (!this.state.control()) {
                this.status.setText(SGStrata.text("screen.sgcraft.shield.no_access",
                        "You don't have access to this gate's shield").color(SGStrata.RED));
            } else {
                this.status.setText(SGStrata.of(SGIris.shieldText(iris)).color(
                        iris == IrisState.CLOSED || iris == IrisState.CLOSING ? SGStrata.AQUA : SGStrata.GRAY));
            }
        }
        this.message.setText(Text.of(this.state.message()).color(SGStrata.RED));
        final int iris = gate == null ? GateInfo.NO_IRIS : gate.iris();
        this.raise.setEnabled(this.state.control() && iris == IrisState.OPEN.ordinal());
        this.lower.setEnabled(this.state.control() && iris == IrisState.CLOSED.ordinal());
    }
}
