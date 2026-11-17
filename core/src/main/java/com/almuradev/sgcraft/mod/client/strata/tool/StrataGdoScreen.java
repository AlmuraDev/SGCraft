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

package com.almuradev.sgcraft.mod.client.strata.tool;

import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.gate.IrisState;
import com.almuradev.sgcraft.mod.client.ClientPlatform;
import com.almuradev.sgcraft.mod.client.strata.SGStrata;
import com.almuradev.sgcraft.mod.client.strata.StrataHost;
import com.almuradev.sgcraft.mod.client.tool.GateStatusText;
import com.almuradev.sgcraft.mod.client.tool.PddScreen;
import com.almuradev.sgcraft.mod.gate.SGIris;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.GateInfo;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.GdoAction;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.GdoState;
import net.minecraft.client.Minecraft;
import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.text.Text;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * The GDO drawn with Strata, laid out as SGCraft 2.3's MalisisCore one was: a panel for the gate near the player and,
 * while its wormhole is open, one beside it for the gate at the other end, each with Open Iris at the bottom left and
 * Close Iris at the bottom right, and on the near gate's Disconnect between them while it is connected. 2.3 drew each
 * gate where this says in words what it is doing, its glyphs and its iris. It asks the server for the GDO's state twice
 * a second, as the standard screen does.
 */
public final class StrataGdoScreen implements Screen {

    // 2.3's panels, as wide as their three buttons need: the shield's labels are longer than the iris's
    private static final float PANEL = 195f;
    private static final float SHIELD_PANEL = 266f;
    private static final float GAP = 6f;
    // What the window's frame takes of its width, a little each side, as measured in game
    private static final float FRAME = 4f;
    private static final float HEIGHT = 230f;
    private static final float SIDE_BUTTON = 60f;
    private static final float SHIELD_BUTTON = 97f;
    private static final float MIDDLE_BUTTON = 64f;
    private static final float MARGIN = 10f;
    private static final long REFRESH_MILLIS = 500L;
    private static final String HIDDEN = "????-???-??";

    private GdoState state;
    private @Nullable Window window;
    private @Nullable Panel local;
    private @Nullable Panel remote;
    private @Nullable Button disconnect;
    private @Nullable Label feedback;

    public StrataGdoScreen(final GdoState state) {
        this.state = state;
    }

    @Override
    public Text title() {
        return SGStrata.text("screen.sgcraft.gdo", "GDO");
    }

    /** Shows a newer state from the server. */
    public void update(final GdoState state) {
        this.state = state;
        this.refresh();
    }

    @Override
    public Component<?> build(final ComponentFactory factory) {
        this.disconnect = SGStrata.button(factory, SGStrata.text("screen.sgcraft.gdo.disconnect", "Disconnect"),
                StrataGdoScreen.MIDDLE_BUTTON, () -> this.send(GdoAction.Kind.DISCONNECT));
        this.local = new Panel(factory, SGStrata.text("screen.sgcraft.gdo.local", "This gate"),
                GdoAction.Kind.OPEN_LOCAL, GdoAction.Kind.CLOSE_LOCAL);
        this.local.box.add(this.disconnect.setAnchor(Anchor.BOTTOM_CENTER));
        this.remote = new Panel(factory, SGStrata.text("screen.sgcraft.gdo.remote", "The other end"),
                GdoAction.Kind.OPEN_REMOTE, GdoAction.Kind.CLOSE_REMOTE);
        final Button close = SGStrata.button(factory, SGStrata.text("screen.sgcraft.close", "Close"), 40f,
                StrataHost::close);
        this.feedback = factory.label("").setTextScale(0.8f);
        final float bottom = SGStrata.BUTTON_HEIGHT + 3f;
        final AnchorPanel content = factory.anchorPanel()
                .add(this.local.box.setAnchor(Anchor.FILL).setMargin(0f, 0f, 0f, bottom)
                        .setAlignment(HorizontalAlignment.LEFT, VerticalAlignment.STRETCH))
                .add(this.remote.box.setAnchor(Anchor.FILL).setMargin(0f, 0f, 0f, bottom)
                        .setAlignment(HorizontalAlignment.RIGHT, VerticalAlignment.STRETCH))
                .add(this.feedback.setAnchor(Anchor.BOTTOM_LEFT).setMargin(2f, 0f, 0f, 4f))
                .add(close.setAnchor(Anchor.BOTTOM_RIGHT));
        this.window = SGStrata.window(factory, this.title(), content, StrataGdoScreen.PANEL + StrataGdoScreen.FRAME,
                StrataGdoScreen.HEIGHT);
        this.refresh();
        return factory.anchorPanel().add(this.window).setAnchor(Anchor.FILL);
    }

    @Override
    public void opened(final ScreenHandle handle) {
        handle.every(StrataGdoScreen.REFRESH_MILLIS, () -> this.send(GdoAction.Kind.REFRESH));
    }

    private void send(final GdoAction.Kind kind) {
        ClientPlatform.get().sendToServer(new GdoAction(this.state.local().pos(), kind));
    }

    // Both panels as wide as the wider needs; the window holds the near gate's panel, or both side by side
    private void refresh() {
        if (this.window == null || this.local == null || this.remote == null || this.disconnect == null
                || this.feedback == null) {
            return;
        }
        final GateInfo remote = this.state.remote().orElse(null);
        final boolean shield = this.state.local().shield() || remote != null && remote.shield();
        final float panel = shield ? StrataGdoScreen.SHIELD_PANEL : StrataGdoScreen.PANEL;
        final float side = shield ? StrataGdoScreen.SHIELD_BUTTON : StrataGdoScreen.SIDE_BUTTON;
        this.local.show(this.state.local(), panel, side);
        this.remote.show(remote, panel, side);
        // Alone, the near gate's panel fills the window, so its sides are as far from the window's whatever the frame
        if (remote == null) {
            this.local.box.setWidth(Float.NaN).setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.STRETCH);
        } else {
            this.local.box.setAlignment(HorizontalAlignment.LEFT, VerticalAlignment.STRETCH);
        }
        final float width = (remote == null ? panel : 2 * panel + StrataGdoScreen.GAP) + StrataGdoScreen.FRAME;
        this.window.setWidth(Math.min(width,
                Minecraft.getInstance().getWindow().getGuiScaledWidth() - StrataGdoScreen.MARGIN));
        // Shown only while the wormhole is open, as in 2.3, not while dialing or closing
        final String gate = this.state.local().state();
        this.disconnect.setVisible(gate.equals("opening") || gate.equals("connected"));
        this.disconnect.setEnabled(this.state.canDisconnect());
        this.feedback.setText(Text.of(this.state.message()).color(SGStrata.RED));
    }

    /** One gate's panel: its heading, what it is doing, its glyphs and iris, its address, and its iris buttons. */
    private final class Panel {

        final AnchorPanel box;
        private final Label status;
        private final Label glyphs;
        private final Label iris;
        private final Label address;
        private final Button open;
        private final Button close;

        Panel(final ComponentFactory factory, final Text heading, final GdoAction.Kind open,
                final GdoAction.Kind close) {
            this.status = factory.label("").setTextScale(1.8f);
            this.glyphs = factory.label("").setTextScale(1.4f);
            this.iris = factory.label("");
            this.address = factory.label("").setTextScale(1.3f);
            this.open = SGStrata.button(factory, SGStrata.text("screen.sgcraft.gdo.open", "Open Iris"),
                    StrataGdoScreen.SIDE_BUTTON, () -> StrataGdoScreen.this.send(open));
            this.close = SGStrata.button(factory, SGStrata.text("screen.sgcraft.gdo.close", "Close Iris"),
                    StrataGdoScreen.SIDE_BUTTON, () -> StrataGdoScreen.this.send(close));
            this.box = SGStrata.box(factory)
                    .add(factory.label(heading.color(SGStrata.WHITE)).setTextScale(1.1f).setAnchor(Anchor.TOP_CENTER)
                            .setMargin(0f, 1f, 0f, 0f))
                    .add(SGStrata.line(factory, 10f, 14f, 10f))
                    .add(this.status.setAnchor(Anchor.TOP_CENTER).setMargin(0f, 36f, 0f, 0f))
                    .add(this.glyphs.setAnchor(Anchor.TOP_CENTER).setMargin(0f, 66f, 0f, 0f))
                    .add(this.iris.setAnchor(Anchor.TOP_CENTER).setMargin(0f, 96f, 0f, 0f))
                    .add(this.address.setAnchor(Anchor.BOTTOM_CENTER).setMargin(0f, 0f, 0f, 26f))
                    .add(this.open.setAnchor(Anchor.BOTTOM_LEFT))
                    .add(this.close.setAnchor(Anchor.BOTTOM_RIGHT));
        }

        // An iris that is moving takes no orders until it stops; one that is shut can only open, and the other way. A
        // shield is raised by closing it and lowered by opening it, and its buttons say so
        void show(final @Nullable GateInfo gate, final float width, final float buttons) {
            this.box.setVisible(gate != null);
            this.box.setWidth(width);
            this.open.setWidth(buttons);
            this.close.setWidth(buttons);
            if (gate == null) {
                return;
            }
            this.status.setText(SGStrata.of(GateStatusText.of(gate.state())));
            this.glyphs.setText(Address.parse(gate.address()).map(a -> SGStrata.glyphs(a, PddScreen.style(gate)))
                    .orElse(Text.empty()));
            final boolean none = gate.iris() == GateInfo.NO_IRIS;
            final Text iris = none ? SGStrata.text("screen.sgcraft.gdo.no_iris", "No iris")
                    : gate.shield() ? SGStrata.of(SGIris.shieldText(IrisState.values()[gate.iris()]))
                    : SGStrata.text("screen.sgcraft.gdo.iris", "Iris: %s",
                            IrisState.values()[gate.iris()].name().toLowerCase(Locale.ROOT));
            this.iris.setText(iris.color(gate.iris() == IrisState.CLOSED.ordinal() ? 0xFFFF8040 : 0xFFA0FFA0));
            this.address.setText(Text.of(gate.address().isEmpty() ? StrataGdoScreen.HIDDEN : gate.address())
                    .color(SGStrata.BLUE));
            this.open.setText(gate.shield() ? SGStrata.text("screen.sgcraft.gdo.lower_shield", "Deactivate Shield")
                    : SGStrata.text("screen.sgcraft.gdo.open", "Open Iris"));
            this.close.setText(gate.shield() ? SGStrata.text("screen.sgcraft.gdo.raise_shield", "Activate Shield")
                    : SGStrata.text("screen.sgcraft.gdo.close", "Close Iris"));
            this.open.setEnabled(gate.iris() == IrisState.CLOSED.ordinal());
            this.close.setEnabled(gate.iris() == IrisState.OPEN.ordinal());
        }
    }
}
