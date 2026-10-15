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

package com.almuradev.sgcraft.client.tool;

import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.client.ClientPlatform;
import com.almuradev.sgcraft.gate.DialStyle;
import com.almuradev.sgcraft.gate.GlyphText;
import com.almuradev.sgcraft.tool.ToolPayloads.BookEntry;
import com.almuradev.sgcraft.tool.ToolPayloads.GateInfo;
import com.almuradev.sgcraft.tool.ToolPayloads.PddAction;
import com.almuradev.sgcraft.tool.ToolPayloads.PddState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * The PDD, the personal dialing device: the addresses kept on the PDD itself, dialed from the gate near its holder,
 * each with what the gate it reaches is doing, or that it is gone. Choosing an entry fills the boxes below the list,
 * which add, rename and remove entries; the gate's own address can be added with a click.
 */
public final class PddScreen extends Screen {

    private static final int LIST_WIDTH = 320;
    private static final int ROW = 24;
    private static final int REFRESH_TICKS = 20;
    // The screen's bottom margin, the space the boxes and buttons take below the list, and the strip above the boxes
    // where the list's count is written
    private static final int BOTTOM = 100;
    private static final int COUNT_HEIGHT = 12;

    private PddState state;
    private @Nullable Entries list;
    private @Nullable EditBox name;
    private @Nullable EditBox address;
    private @Nullable Button dial;
    private @Nullable Button disconnect;
    private @Nullable Button addHere;
    private int ticks;

    public PddScreen(final PddState state) {
        super(Component.translatableWithFallback("screen.sgcraft.pdd", "Personal Dialing Device"));
        this.state = state;
    }

    /** Shows a newer state from the server, keeping what the player has typed. */
    public void update(final PddState state) {
        this.state = state;
        if (this.list != null) {
            final String selected = this.list.getSelected() == null ? "" : this.list.getSelected().entry.address();
            this.list.fill(state.entries(), selected);
        }
        this.updateButtons();
    }

    @Override
    protected void init() {
        final int left = (this.width - PddScreen.LIST_WIDTH) / 2;
        final int bottom = this.height - PddScreen.BOTTOM;
        this.list = this.addRenderableWidget(new Entries(this.minecraft, PddScreen.LIST_WIDTH,
                bottom - 40 - PddScreen.COUNT_HEIGHT, 40));
        this.list.setX(left);
        this.list.fill(this.state.entries(), "");
        this.name = this.addRenderableWidget(new EditBox(this.font, left, bottom + 6, 180, 20,
                Component.translatableWithFallback("screen.sgcraft.pdd.name", "Name")));
        this.name.setMaxLength(48);
        this.name.setHint(Component.translatableWithFallback("screen.sgcraft.pdd.name", "Name"));
        this.address = this.addRenderableWidget(new EditBox(this.font, left + 186, bottom + 6, 134, 20,
                Component.translatableWithFallback("screen.sgcraft.pdd.address", "Address")));
        this.address.setMaxLength(11);
        this.address.setHint(Component.translatableWithFallback("screen.sgcraft.pdd.address", "Address"));
        this.address.setResponder(text -> this.updateButtons());
        final int row1 = bottom + 32;
        final int w = 62;
        this.addRenderableWidget(PddScreen.button("screen.sgcraft.pdd.add", "Add", left, row1, w,
                () -> this.send(PddAction.Kind.ADD, 0)));
        this.addRenderableWidget(PddScreen.button("screen.sgcraft.pdd.rename", "Rename", left + 64, row1, w,
                () -> this.send(PddAction.Kind.RENAME, 0)));
        this.addRenderableWidget(PddScreen.button("screen.sgcraft.pdd.remove", "Remove", left + 128, row1, w,
                () -> this.send(PddAction.Kind.REMOVE, 0)));
        this.addRenderableWidget(PddScreen.button("screen.sgcraft.pdd.up", "Up", left + 192, row1, w,
                () -> this.move(-1)));
        this.addRenderableWidget(PddScreen.button("screen.sgcraft.pdd.down", "Down", left + 256, row1, 64,
                () -> this.move(1)));
        final int row2 = bottom + 56;
        this.dial = this.addRenderableWidget(PddScreen.button("screen.sgcraft.pdd.dial", "Dial", left, row2, 78,
                () -> this.send(PddAction.Kind.DIAL, 0)));
        this.disconnect = this.addRenderableWidget(PddScreen.button("screen.sgcraft.pdd.disconnect", "Disconnect",
                left + 80, row2, 78, () -> this.send(PddAction.Kind.DISCONNECT, 0)));
        this.addHere = this.addRenderableWidget(PddScreen.button("screen.sgcraft.pdd.add_here", "Add this gate",
                left + 160, row2, 78, this::addHere));
        this.addRenderableWidget(PddScreen.button("screen.sgcraft.close", "Close", left + 240, row2, 80,
                this::onClose));
        this.updateButtons();
    }

    private static Button button(final String key, final String fallback, final int x, final int y, final int width,
            final Runnable action) {
        return Button.builder(Component.translatableWithFallback(key, fallback), b -> action.run())
                .bounds(x, y, width, 20).build();
    }

    private void updateButtons() {
        final Optional<GateInfo> gate = this.state.gate();
        final boolean idle = gate.map(g -> g.state().equals("idle")).orElse(false);
        if (this.dial != null && this.address != null) {
            this.dial.active = idle && Address.parse(this.address.getValue()).isPresent();
        }
        if (this.disconnect != null) {
            this.disconnect.active = gate.isPresent() && !idle;
        }
        if (this.addHere != null) {
            this.addHere.active = gate.map(g -> !g.address().isEmpty()).orElse(false);
        }
    }

    private void addHere() {
        this.state.gate().ifPresent(gate -> {
            if (this.address != null && this.name != null) {
                this.address.setValue(gate.address());
                if (this.name.getValue().isBlank()) {
                    this.name.setValue(gate.address());
                }
                this.send(PddAction.Kind.ADD, 0);
            }
        });
    }

    private void move(final int by) {
        if (this.list == null || this.list.getSelected() == null) {
            return;
        }
        final int index = this.state.entries().indexOf(this.list.getSelected().entry) + by;
        if (index >= 0 && index < this.state.entries().size()) {
            this.send(PddAction.Kind.MOVE, index);
        }
    }

    private void send(final PddAction.Kind kind, final int index) {
        final String address = this.address == null ? "" : this.address.getValue();
        final String name = this.name == null ? "" : this.name.getValue();
        ClientPlatform.get().sendToServer(new PddAction(kind, address, name, index));
    }

    // Choosing an entry puts it in the boxes, ready to dial, rename or remove
    void choose(final BookEntry entry) {
        if (this.name != null && this.address != null) {
            this.name.setValue(entry.name());
            this.address.setValue(entry.address());
        }
        this.updateButtons();
    }

    @Override
    public void tick() {
        if (++this.ticks % PddScreen.REFRESH_TICKS == 0) {
            ClientPlatform.get().sendToServer(PddAction.of(PddAction.Kind.REFRESH));
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
        final Component gate = this.state.gate().map(PddScreen::describe).orElse(
                Component.translatableWithFallback("screen.sgcraft.pdd.no_gate",
                        "No gate near you. You can still change its addresses"));
        graphics.centeredText(this.font, gate, this.width / 2, 22, 0xFFAAAAAA);
        if (!this.state.message().isEmpty()) {
            graphics.centeredText(this.font, this.state.message(), this.width / 2, this.height - 16, 0xFFFF6060);
        }
        if (this.list != null) {
            final Component count = Component.translatableWithFallback("screen.sgcraft.pdd.addresses_count",
                    "Addresses: %s", this.state.entries().size());
            graphics.text(this.font, count, this.list.getX() + 2,
                    this.height - PddScreen.BOTTOM - PddScreen.COUNT_HEIGHT + 2, 0xFF808080);
        }
    }

    private static Component describe(final GateInfo gate) {
        final Component address = Address.parse(gate.address())
                .<Component>map(a -> Component.literal(gate.address() + " ")
                        .append(GlyphText.of(a, PddScreen.style(gate))))
                .orElse(Component.translatableWithFallback("screen.sgcraft.pdd.hidden", "address hidden"));
        return Component.translatableWithFallback("screen.sgcraft.pdd.gate", "This gate: %s, %s", address,
                GateStatusText.of(gate.state()));
    }

    static DialStyle style(final GateInfo gate) {
        if (gate.type().equals("sgcraft:pegasus")) {
            return DialStyle.PEGASUS;
        }
        return gate.type().equals("sgcraft:universe") ? DialStyle.UNIVERSE : DialStyle.MILKY_WAY;
    }

    /** The address book, one entry a row: its name, and its address in letters and glyphs. */
    final class Entries extends ObjectSelectionList<Entries.Row> {

        Entries(final Minecraft minecraft, final int width, final int height, final int y) {
            super(minecraft, width, height, y, PddScreen.ROW);
        }

        void fill(final List<BookEntry> entries, final String selected) {
            this.clearEntries();
            for (final BookEntry entry : entries) {
                final Row row = new Row(entry);
                this.addEntry(row);
                if (entry.address().equals(selected)) {
                    this.setSelected(row);
                }
            }
        }

        @Override
        public int getRowWidth() {
            return PddScreen.LIST_WIDTH - 12;
        }

        final class Row extends ObjectSelectionList.Entry<Row> {

            private final BookEntry entry;

            Row(final BookEntry entry) {
                this.entry = entry;
            }

            @Override
            public Component getNarration() {
                return Component.literal(this.entry.name() + ", " + this.entry.address());
            }

            @Override
            public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
                    final boolean hovered, final float a) {
                final PddScreen screen = PddScreen.this;
                graphics.text(screen.font, this.entry.name(), this.getContentX() + 2, this.getContentY() + 1,
                        0xFFFFFFFF);
                graphics.text(screen.font, this.entry.address(), this.getContentX() + 2, this.getContentY() + 11,
                        0xFF8FB8FF);
                // What the gate it reaches is doing, or that it is gone
                graphics.text(screen.font, GateStatusText.of(this.entry.status()),
                        this.getContentX() + 6 + screen.font.width(this.entry.address()), this.getContentY() + 11,
                        0xFFFFFFFF);
                final DialStyle style = screen.state.gate().map(PddScreen::style).orElse(DialStyle.MILKY_WAY);
                Address.parse(this.entry.address()).ifPresent(address -> {
                    final Component glyphs = GlyphText.of(address, style);
                    graphics.text(screen.font, glyphs, this.getContentRight() - screen.font.width(glyphs) - 2,
                            this.getContentY() + 5, 0xFFFFFFFF);
                });
            }

            @Override
            public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
                Entries.this.setSelected(this);
                PddScreen.this.choose(this.entry);
                if (doubleClick && PddScreen.this.dial != null && PddScreen.this.dial.active) {
                    PddScreen.this.send(PddAction.Kind.DIAL, 0);
                }
                return true;
            }
        }
    }
}
