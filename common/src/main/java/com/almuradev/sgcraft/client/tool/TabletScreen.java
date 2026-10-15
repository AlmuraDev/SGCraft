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
import com.almuradev.sgcraft.api.gate.IrisState;
import com.almuradev.sgcraft.client.ClientPlatform;
import com.almuradev.sgcraft.gate.DialStyle;
import com.almuradev.sgcraft.gate.GlyphText;
import com.almuradev.sgcraft.tool.ToolPayloads.BookEntry;
import com.almuradev.sgcraft.tool.ToolPayloads.GateInfo;
import com.almuradev.sgcraft.tool.ToolPayloads.TabletAction;
import com.almuradev.sgcraft.tool.ToolPayloads.TabletGateInfo;
import com.almuradev.sgcraft.tool.ToolPayloads.TabletState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * The gate control tablet: a PDD for many gates. On the left, the gates on its list, each with what it is doing or
 * that it is gone; on the right, the addresses kept on the tablet, each with what the gate it reaches is doing, as the
 * chosen gate sees it. Choosing a gate and an address and pressing Dial dials one from the other, wherever the gate
 * is.
 */
public final class TabletScreen extends Screen {

    private static final int SIDE = 76;
    private static final int PAD = 4;
    private static final int GAP = 10;
    private static final int LISTS = 410;
    private static final int ROW = 24;
    private static final int STEP = 22;
    private static final int REFRESH_TICKS = 20;
    // The screen's bottom margin, the space the boxes and buttons take below the lists, and the strip above the boxes
    // where each list's count is written
    private static final int BOTTOM = 84;
    private static final int COUNT_HEIGHT = 12;

    private TabletState state;
    private int selected;
    private @Nullable Gates gates;
    private @Nullable Entries entries;
    private @Nullable EditBox gateName;
    private @Nullable EditBox name;
    private @Nullable EditBox address;
    private @Nullable Button addGate;
    private @Nullable Button renameGate;
    private @Nullable Button removeGate;
    private @Nullable Button dial;
    private @Nullable Button disconnect;
    private @Nullable Button configure;
    private @Nullable Button openIris;
    private @Nullable Button closeIris;
    private int ticks;

    public TabletScreen(final TabletState state) {
        super(Component.translatableWithFallback("screen.sgcraft.tablet", "Gate Control Tablet"));
        this.state = state;
        this.selected = state.selected() < 0 && !state.gates().isEmpty() ? 0 : state.selected();
    }

    /** Shows a newer state from the server, keeping what the player has typed. */
    public void update(final TabletState state) {
        this.state = state;
        this.selected = state.selected() < 0 && !state.gates().isEmpty() ? 0 : state.selected();
        if (this.gates != null) {
            this.gates.fill(state.gates(), this.selected);
        }
        if (this.entries != null) {
            final String chosen = this.entries.getSelected() == null ? "" : this.entries.getSelected().entry.address();
            this.entries.fill(this.shown(), chosen);
        }
        this.updateButtons();
    }

    @Override
    protected void init() {
        // Each list with its own buttons beside it, the gates' on the left and the addresses' on the right; what
        // works the chosen gate with an address along the bottom. The lists share what width the screen has.
        final int lists = Math.min(TabletScreen.LISTS,
                this.width - 16 - 2 * (TabletScreen.SIDE + TabletScreen.PAD) - TabletScreen.GAP);
        final int gatesWidth = lists * 2 / 5;
        final int entriesWidth = lists - gatesWidth;
        final int total = 2 * (TabletScreen.SIDE + TabletScreen.PAD) + TabletScreen.GAP + lists;
        final int gateColumn = (this.width - total) / 2;
        final int left = gateColumn + TabletScreen.SIDE + TabletScreen.PAD;
        final int right = left + gatesWidth + TabletScreen.GAP;
        final int entryColumn = right + entriesWidth + TabletScreen.PAD;
        final int top = 40;
        final int bottom = this.height - TabletScreen.BOTTOM;
        final int listHeight = bottom - top - TabletScreen.COUNT_HEIGHT;
        this.gates = this.addRenderableWidget(new Gates(this.minecraft, gatesWidth, listHeight, top));
        this.gates.setX(left);
        this.gates.fill(this.state.gates(), this.selected);
        this.entries = this.addRenderableWidget(new Entries(this.minecraft, entriesWidth, listHeight, top));
        this.entries.setX(right);
        this.entries.fill(this.shown(), "");
        // Under each list, its boxes
        this.gateName = this.addRenderableWidget(TabletScreen.box(this.font, left, bottom + 6, gatesWidth,
                "screen.sgcraft.tablet.gate_name", "Gate name", 48));
        final int addressWidth = 72;
        this.name = this.addRenderableWidget(TabletScreen.box(this.font, right, bottom + 6,
                entriesWidth - addressWidth - 4, "screen.sgcraft.pdd.name", "Name", 48));
        this.address = this.addRenderableWidget(TabletScreen.box(this.font, right + entriesWidth - addressWidth,
                bottom + 6, addressWidth, "screen.sgcraft.pdd.address", "Address", 11));
        this.address.setResponder(text -> this.updateButtons());
        // Beside the gates: the list's own buttons, then what works the chosen gate itself
        int y = top;
        this.addGate = this.addRenderableWidget(TabletScreen.button("screen.sgcraft.tablet.add_gate", "Add this gate",
                gateColumn, y, TabletScreen.SIDE, () -> this.send(TabletAction.Kind.ADD_GATE, this.gateName, 0),
                "Adds the gate you are near to the list, named as the box says"));
        this.renameGate = this.addRenderableWidget(TabletScreen.button("screen.sgcraft.tablet.rename_gate", "Rename",
                gateColumn, y += TabletScreen.STEP, TabletScreen.SIDE,
                () -> this.send(TabletAction.Kind.RENAME_GATE, this.gateName, 0),
                "Names the chosen gate as the box says"));
        this.removeGate = this.addRenderableWidget(TabletScreen.button("screen.sgcraft.pdd.remove", "Remove",
                gateColumn, y += TabletScreen.STEP, TabletScreen.SIDE,
                () -> this.send(TabletAction.Kind.REMOVE_GATE, this.gateName, 0),
                "Takes the chosen gate off the list"));
        this.addRenderableWidget(TabletScreen.button("screen.sgcraft.pdd.up", "Up", gateColumn,
                y += TabletScreen.STEP, TabletScreen.SIDE, () -> this.moveGate(-1), null));
        this.addRenderableWidget(TabletScreen.button("screen.sgcraft.pdd.down", "Down", gateColumn,
                y += TabletScreen.STEP, TabletScreen.SIDE, () -> this.moveGate(1), null));
        y += 8;
        this.addRenderableWidget(TabletScreen.button("screen.sgcraft.tablet.gate_to_addresses", "Save address",
                gateColumn, y += TabletScreen.STEP, TabletScreen.SIDE, this::gateToAddresses,
                "Adds the chosen gate's address to the addresses on the right, under its name"));
        this.configure = this.addRenderableWidget(TabletScreen.button("screen.sgcraft.tablet.configure", "Configure",
                gateColumn, y += TabletScreen.STEP, TabletScreen.SIDE,
                () -> this.send(TabletAction.Kind.CONFIGURE, null, 0),
                "Opens the chosen gate's configurator, wherever the gate is"));
        this.openIris = this.addRenderableWidget(TabletScreen.button("screen.sgcraft.gdo.open_local", "Open iris",
                gateColumn, y += TabletScreen.STEP, TabletScreen.SIDE,
                () -> this.send(TabletAction.Kind.OPEN_IRIS, null, 0), "Opens the chosen gate's iris"));
        this.closeIris = this.addRenderableWidget(TabletScreen.button("screen.sgcraft.gdo.close_local", "Close iris",
                gateColumn, y += TabletScreen.STEP, TabletScreen.SIDE,
                () -> this.send(TabletAction.Kind.CLOSE_IRIS, null, 0), "Shuts the chosen gate's iris"));
        // Beside the addresses: adding, renaming, removing and moving them
        final String[][] entryButtons = {
                {"screen.sgcraft.pdd.add", "Add"}, {"screen.sgcraft.pdd.rename", "Rename"},
                {"screen.sgcraft.pdd.remove", "Remove"}, {"screen.sgcraft.pdd.up", "Up"},
                {"screen.sgcraft.pdd.down", "Down"},
        };
        final Runnable[] entryActions = {
                () -> this.send(TabletAction.Kind.ADD, this.name, 0),
                () -> this.send(TabletAction.Kind.RENAME, this.name, 0),
                () -> this.send(TabletAction.Kind.REMOVE, this.name, 0),
                () -> this.moveEntry(-1),
                () -> this.moveEntry(1),
        };
        for (int i = 0; i < entryButtons.length; i++) {
            this.addRenderableWidget(TabletScreen.button(entryButtons[i][0], entryButtons[i][1], entryColumn,
                    top + i * TabletScreen.STEP, TabletScreen.SIDE, entryActions[i], null));
        }
        // Along the bottom: the chosen gate and the chosen address together
        final int row = bottom + 32;
        final int third = (total - 8) / 3;
        this.dial = this.addRenderableWidget(TabletScreen.button("screen.sgcraft.pdd.dial", "Dial", gateColumn, row,
                third, () -> this.send(TabletAction.Kind.DIAL, this.name, 0),
                "Dials the address in the box from the chosen gate"));
        this.disconnect = this.addRenderableWidget(TabletScreen.button("screen.sgcraft.pdd.disconnect", "Disconnect",
                gateColumn + third + 4, row, third, () -> this.send(TabletAction.Kind.DISCONNECT, this.name, 0),
                "Shuts the chosen gate's wormhole, or stops its dial"));
        this.addRenderableWidget(TabletScreen.button("screen.sgcraft.close", "Close", gateColumn + 2 * (third + 4),
                row, third, this::onClose, null));
        // The top gate, or the one the server chose, is ready to work from the start
        this.chosen().ifPresent(gate -> this.gateName.setValue(gate.name()));
        this.updateButtons();
    }

    private static EditBox box(final Font font, final int x, final int y, final int width,
            final String key, final String fallback, final int length) {
        final EditBox box = new EditBox(font, x, y, width, 20, Component.translatableWithFallback(key, fallback));
        box.setMaxLength(length);
        box.setHint(Component.translatableWithFallback(key, fallback));
        return box;
    }

    private static Button button(final String key, final String fallback, final int x, final int y, final int width,
            final Runnable action, final @Nullable String tip) {
        final Button button = Button.builder(Component.translatableWithFallback(key, fallback), b -> action.run())
                .bounds(x, y, width, 20).build();
        if (tip != null) {
            button.setTooltip(Tooltip.create(Component.translatableWithFallback(key + ".tip", tip)));
        }
        return button;
    }

    private Optional<TabletGateInfo> chosen() {
        return this.selected >= 0 && this.selected < this.state.gates().size()
                ? Optional.of(this.state.gates().get(this.selected)) : Optional.empty();
    }

    // The addresses, less the chosen gate's own: a gate cannot dial itself
    private List<BookEntry> shown() {
        final Optional<Address> own = this.chosen().flatMap(gate -> Address.parse(gate.address()));
        if (own.isEmpty()) {
            return this.state.entries();
        }
        return this.state.entries().stream()
                .filter(entry -> !Address.parse(entry.address()).equals(own))
                .toList();
    }

    private void updateButtons() {
        final Optional<TabletGateInfo> gate = this.chosen();
        final String status = gate.map(TabletGateInfo::status).orElse(BookEntry.NO_GATE);
        if (this.dial != null && this.address != null) {
            this.dial.active = status.equals("idle") && Address.parse(this.address.getValue()).isPresent();
        }
        if (this.disconnect != null) {
            this.disconnect.active = !status.equals("idle") && !status.equals(BookEntry.NO_GATE);
        }
        if (this.addGate != null) {
            this.addGate.active = this.state.near().isPresent();
        }
        if (this.renameGate != null) {
            this.renameGate.active = gate.isPresent();
        }
        if (this.configure != null) {
            this.configure.active = !status.equals(BookEntry.NO_GATE);
        }
        final boolean iris = gate.map(g -> g.iris() != GateInfo.NO_IRIS).orElse(false);
        if (this.openIris != null) {
            this.openIris.active = iris;
        }
        if (this.closeIris != null) {
            this.closeIris.active = iris;
        }
        if (this.removeGate != null) {
            this.removeGate.active = gate.isPresent();
        }
    }

    // The chosen gate's address, under the gate's name, added to the addresses
    private void gateToAddresses() {
        this.chosen().filter(gate -> !gate.address().isEmpty()).ifPresent(gate -> {
            if (this.name != null && this.address != null) {
                this.name.setValue(gate.name());
                this.address.setValue(gate.address());
                this.send(TabletAction.Kind.ADD, this.name, 0);
            }
        });
    }

    private void moveEntry(final int by) {
        if (this.entries == null || this.entries.getSelected() == null) {
            return;
        }
        // Past the shown neighbor, so a hidden entry between them does not swallow the move
        final List<BookEntry> shown = this.shown();
        final int index = shown.indexOf(this.entries.getSelected().entry) + by;
        if (index >= 0 && index < shown.size()) {
            this.send(TabletAction.Kind.MOVE, this.name, this.state.entries().indexOf(shown.get(index)));
        }
    }

    private void moveGate(final int by) {
        final int index = this.selected + by;
        if (this.chosen().isPresent() && index >= 0 && index < this.state.gates().size()) {
            this.send(TabletAction.Kind.MOVE_GATE, this.gateName, index);
        }
    }

    private void send(final TabletAction.Kind kind, final @Nullable EditBox nameBox, final int index) {
        final String address = this.address == null ? "" : this.address.getValue();
        final String name = nameBox == null ? "" : nameBox.getValue();
        ClientPlatform.get().sendToServer(new TabletAction(kind, this.selected, address, name, index));
    }

    // Choosing a gate shows the addresses as it sees them, and puts its name in the box to rename it
    void chooseGate(final int index) {
        this.selected = index;
        this.chosen().ifPresent(gate -> {
            if (this.gateName != null) {
                this.gateName.setValue(gate.name());
            }
        });
        if (this.entries != null) {
            final String chosen = this.entries.getSelected() == null ? "" : this.entries.getSelected().entry.address();
            this.entries.fill(this.shown(), chosen);
        }
        this.send(TabletAction.Kind.REFRESH, null, 0);
        this.updateButtons();
    }

    // Choosing an entry puts it in the boxes, ready to dial, rename or remove
    void chooseEntry(final BookEntry entry) {
        if (this.name != null && this.address != null) {
            this.name.setValue(entry.name());
            this.address.setValue(entry.address());
        }
        this.updateButtons();
    }

    @Override
    public void tick() {
        if (++this.ticks % TabletScreen.REFRESH_TICKS == 0) {
            this.send(TabletAction.Kind.REFRESH, null, 0);
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
        final Component near = this.state.near().<Component>map(gate -> Component.translatableWithFallback(
                "screen.sgcraft.tablet.near", "Near you: %s, %s", gate.address().isEmpty() ? "?" : gate.address(),
                GateStatusText.of(gate.state()))).orElse(Component.translatableWithFallback(
                        "screen.sgcraft.tablet.no_gate_near", "No gate near you to add"));
        graphics.centeredText(this.font, near, this.width / 2, 22, 0xFFAAAAAA);
        if (this.state.gates().isEmpty() && this.gates != null) {
            graphics.text(this.font, Component.translatableWithFallback("screen.sgcraft.tablet.empty",
                    "Stand by a gate and add it"), this.gates.getX() + 4, 46, 0xFF808080);
        }
        if (!this.state.message().isEmpty()) {
            graphics.centeredText(this.font, this.state.message(), this.width / 2, this.height - 16, 0xFFFF6060);
        }
        if (this.gates != null && this.entries != null) {
            final int countY = this.height - TabletScreen.BOTTOM - TabletScreen.COUNT_HEIGHT + 2;
            graphics.text(this.font, Component.translatableWithFallback("screen.sgcraft.tablet.gates_count",
                    "Gates: %s", this.state.gates().size()), this.gates.getX() + 2, countY, 0xFF808080);
            graphics.text(this.font, Component.translatableWithFallback("screen.sgcraft.tablet.addresses_count",
                    "Addresses: %s", this.shown().size()), this.entries.getX() + 2, countY, 0xFF808080);
        }
    }

    static DialStyle style(final String type) {
        if (type.equals("sgcraft:pegasus")) {
            return DialStyle.PEGASUS;
        }
        return type.equals("sgcraft:universe") ? DialStyle.UNIVERSE : DialStyle.MILKY_WAY;
    }

    /** The tablet's gates, one a row: its name, and what it is doing with its address. */
    final class Gates extends ObjectSelectionList<Gates.Row> {

        Gates(final Minecraft minecraft, final int width, final int height, final int y) {
            super(minecraft, width, height, y, TabletScreen.ROW);
        }

        void fill(final List<TabletGateInfo> gates, final int selected) {
            this.clearEntries();
            for (int i = 0; i < gates.size(); i++) {
                final Row row = new Row(gates.get(i), i);
                this.addEntry(row);
                if (i == selected) {
                    this.setSelected(row);
                }
            }
        }

        @Override
        public int getRowWidth() {
            return this.getWidth() - 12;
        }

        final class Row extends ObjectSelectionList.Entry<Row> {

            private final TabletGateInfo gate;
            private final int index;

            Row(final TabletGateInfo gate, final int index) {
                this.gate = gate;
                this.index = index;
            }

            @Override
            public Component getNarration() {
                return Component.literal(this.gate.name());
            }

            @Override
            public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
                    final boolean hovered, final float a) {
                final TabletScreen screen = TabletScreen.this;
                graphics.text(screen.font, this.gate.name(), this.getContentX() + 2, this.getContentY() + 1,
                        0xFFFFFFFF);
                final Component status = GateStatusText.of(this.gate.status());
                graphics.text(screen.font, status, this.getContentX() + 2, this.getContentY() + 11, 0xFFFFFFFF);
                graphics.text(screen.font, this.gate.address(),
                        this.getContentX() + 6 + screen.font.width(status), this.getContentY() + 11, 0xFF8FB8FF);
                // A shut or moving iris shows beside its name
                if (this.gate.iris() != GateInfo.NO_IRIS && this.gate.iris() != IrisState.OPEN.ordinal()) {
                    final Component iris = Component.translatableWithFallback("screen.sgcraft.tablet.iris_shut",
                            "Iris shut").withStyle(ChatFormatting.RED);
                    graphics.text(screen.font, iris, this.getContentRight() - screen.font.width(iris) - 2,
                            this.getContentY() + 1, 0xFFFFFFFF);
                }
            }

            @Override
            public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
                Gates.this.setSelected(this);
                TabletScreen.this.chooseGate(this.index);
                return true;
            }
        }
    }

    /** The tablet's addresses, one a row: its name, its address and what the gate it reaches is doing. */
    final class Entries extends ObjectSelectionList<Entries.Row> {

        Entries(final Minecraft minecraft, final int width, final int height, final int y) {
            super(minecraft, width, height, y, TabletScreen.ROW);
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
            return this.getWidth() - 12;
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
                final TabletScreen screen = TabletScreen.this;
                graphics.text(screen.font, this.entry.name(), this.getContentX() + 2, this.getContentY() + 1,
                        0xFFFFFFFF);
                graphics.text(screen.font, this.entry.address(), this.getContentX() + 2, this.getContentY() + 11,
                        0xFF8FB8FF);
                final Component status = GateStatusText.of(this.entry.status());
                final int textWidth = Math.max(screen.font.width(this.entry.name()),
                        6 + screen.font.width(this.entry.address()) + screen.font.width(status));
                graphics.text(screen.font, status,
                        this.getContentX() + 6 + screen.font.width(this.entry.address()), this.getContentY() + 11,
                        0xFFFFFFFF);
                final DialStyle style = screen.chosen().map(gate -> TabletScreen.style(gate.type()))
                        .orElse(DialStyle.MILKY_WAY);
                Address.parse(this.entry.address()).ifPresent(address -> {
                    final Component glyphs = GlyphText.of(address, style);
                    if (screen.font.width(glyphs) + textWidth + 8 > this.getContentWidth()) {
                        return;
                    }
                    graphics.text(screen.font, glyphs, this.getContentRight() - screen.font.width(glyphs) - 2,
                            this.getContentY() + 5, 0xFFFFFFFF);
                });
            }

            @Override
            public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
                Entries.this.setSelected(this);
                TabletScreen.this.chooseEntry(this.entry);
                if (doubleClick && TabletScreen.this.dial != null && TabletScreen.this.dial.active) {
                    TabletScreen.this.send(TabletAction.Kind.DIAL, TabletScreen.this.name, 0);
                }
                return true;
            }
        }
    }
}
