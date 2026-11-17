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
import com.almuradev.sgcraft.mod.client.ClientPlatform;
import com.almuradev.sgcraft.mod.client.strata.SGStrata;
import com.almuradev.sgcraft.mod.client.strata.StrataHost;
import com.almuradev.sgcraft.mod.client.tool.ExplorerScreen;
import com.almuradev.sgcraft.mod.client.tool.GateStatusText;
import com.almuradev.sgcraft.mod.client.tool.PddScreen;
import com.almuradev.sgcraft.mod.gate.DialStyle;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.BookEntry;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.GateInfo;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.PddAction;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.PddState;
import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.attribute.item.Activatable;
import org.inspirenxe.strata.api.control.attribute.item.Selection;
import org.inspirenxe.strata.api.control.attribute.text.TextInput;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.text.Text;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The PDD drawn with Strata, laid out as SGCraft 2.3's MalisisCore one was: the addresses on the left, under a search
 * box, with 2.3's green +, red - and yellow ? below them to add, remove and rename an entry, and arrows to move it; this
 * gate's address and what it is doing on the right, large; Dial, Disconnect and Close along the bottom, and what the
 * server last said beside them. It asks the server for the PDD's state each second, as the standard screen does.
 */
public final class StrataPddScreen implements Screen {

    private static final float WIDTH = 430f;
    private static final float HEIGHT = 262f;
    private static final float LIST_WIDTH = 250f;
    private static final float ROW_HEIGHT = 24f;
    private static final long REFRESH_MILLIS = 1000L;

    private PddState state;
    // Whether the server's state asked to open with the gate's address ready: done once
    private boolean filled;
    private String search = "";
    private String message;
    private @Nullable ComponentFactory factory;
    private @Nullable ListView<BookEntry> list;
    private @Nullable Label count;
    private @Nullable Label gate;
    private @Nullable Label status;
    private @Nullable Label glyphs;
    private @Nullable Label feedback;
    private @Nullable Button dial;
    private @Nullable Button disconnect;
    private @Nullable Button up;
    private @Nullable Button down;

    public StrataPddScreen(final PddState state) {
        this.state = state;
        this.message = state.message();
    }

    @Override
    public Text title() {
        return SGStrata.text("screen.sgcraft.pdd", "Personal Dialing Device");
    }

    /** Shows a newer state from the server, keeping the chosen entry chosen. */
    public void update(final PddState state) {
        this.state = state;
        if (!state.message().isEmpty()) {
            this.message = state.message();
        }
        this.refresh();
    }

    @Override
    public Component<?> build(final ComponentFactory factory) {
        this.factory = factory;
        // The addresses, under a search box with 2.3's red x to clear it
        final Label available = factory.label(SGStrata.text("screen.sgcraft.pdd.available", "Available Addresses")
                .color(SGStrata.WHITE));
        final Label searchLabel = factory.label(SGStrata.text("screen.sgcraft.pdd.search", "Search") .append(":"));
        final TextBox search = factory.textBox().setMaxLength(32);
        search.setHeight(SGStrata.BUTTON_HEIGHT);
        search.onEvent(TextInput.EDITED, event -> {
            this.search = event.value();
            this.refresh();
        });
        final Button clear = SGStrata.small(factory, "x", SGStrata.RED,
                SGStrata.text("screen.sgcraft.search.clear", "Clear the search"), () -> search.setText(""));
        final ListView<BookEntry> list = SGStrata.rows(factory.<BookEntry>listView(entry -> this.row(factory, entry)),
                StrataPddScreen.ROW_HEIGHT);
        list.onEvent(Selection.SELECTED, event -> this.updateButtons());
        // A double click dials, as 2.3's did
        list.onEvent(Activatable.ACTIVATED, event -> this.dial());
        this.list = list;
        this.count = factory.label("").setTextScale(0.8f);
        final StackPanel entryButtons = factory.stackPanel(Orientation.HORIZONTAL).setSpacing(2f)
                .add(SGStrata.small(factory, "+", SGStrata.GREEN,
                        SGStrata.text("screen.sgcraft.pdd.add", "Add an address"), () -> this.addDialog("", "")))
                .add(SGStrata.small(factory, "-", SGStrata.RED,
                        SGStrata.text("screen.sgcraft.pdd.remove", "Remove the chosen address"), this::remove))
                .add(SGStrata.small(factory, "?", SGStrata.YELLOW,
                        SGStrata.text("screen.sgcraft.pdd.rename", "Rename the chosen address"), this::renameDialog));
        this.up = SGStrata.small(factory, "▲", SGStrata.WHITE,
                SGStrata.text("screen.sgcraft.pdd.up", "Move up"), () -> this.move(-1));
        this.down = SGStrata.small(factory, "▼", SGStrata.WHITE,
                SGStrata.text("screen.sgcraft.pdd.down", "Move down"), () -> this.move(1));
        final StackPanel moveButtons = factory.stackPanel(Orientation.HORIZONTAL).setSpacing(2f)
                .add(this.up).add(this.down);
        final AnchorPanel addresses = SGStrata.box(factory)
                .add(available.setAnchor(Anchor.TOP_LEFT).setMargin(3f, 1f, 0f, 0f))
                .add(searchLabel.setAnchor(Anchor.TOP_LEFT).setMargin(3f, 18f, 0f, 0f))
                .add(search.setAnchor(Anchor.FILL).setMargin(44f, 14f, SGStrata.SMALL_WIDTH + 2f, 0f)
                        .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP))
                .add(clear.setHeight(SGStrata.BUTTON_HEIGHT).setAnchor(Anchor.TOP_RIGHT).setMargin(0f, 14f, 0f, 0f))
                .add(SGStrata.box(factory).add(list.setAnchor(Anchor.FILL)).setAnchor(Anchor.FILL)
                        .setMargin(0f, 33f, 0f, SGStrata.SMALL_HEIGHT + 4f))
                .add(entryButtons.setAnchor(Anchor.BOTTOM_LEFT).setMargin(0f, 0f, 0f, 1f))
                .add(moveButtons.setAnchor(Anchor.BOTTOM_LEFT).setMargin(47f, 0f, 0f, 1f))
                .add(this.count.setAnchor(Anchor.BOTTOM_RIGHT).setMargin(0f, 0f, 3f, 4f));

        // This gate: its address, and what it is doing, large, as 2.3's were
        this.gate = factory.label("");
        this.status = factory.label("").setTextScale(1.8f).setTextAlignment(HorizontalAlignment.CENTER);
        this.glyphs = factory.label("").setTextScale(1.4f).setTextAlignment(HorizontalAlignment.CENTER);
        final float right = LIST_WIDTH + 6f;
        this.dial = SGStrata.button(factory, SGStrata.text("screen.sgcraft.pdd.dial", "Dial"), 60f, this::dial);
        this.disconnect = SGStrata.button(factory, SGStrata.text("screen.sgcraft.pdd.disconnect", "Disconnect"),
                60f, () -> this.send(PddAction.Kind.DISCONNECT, "", "", 0));
        final Button close = SGStrata.button(factory, SGStrata.text("screen.sgcraft.close", "Close"), 40f,
                StrataHost::close);
        this.feedback = factory.label("").setTextScale(0.8f).setWrap(true);
        final AnchorPanel content = factory.anchorPanel()
                .add(addresses.setAnchor(Anchor.FILL).setMargin(0f, 0f, 0f, 0f)
                        .setAlignment(HorizontalAlignment.LEFT, VerticalAlignment.STRETCH).setWidth(LIST_WIDTH))
                .add(this.gate.setAnchor(Anchor.TOP_LEFT).setMargin(right, 3f, 0f, 0f))
                .add(this.status.setAnchor(Anchor.FILL).setMargin(right, 40f, 0f, 0f)
                        .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP))
                .add(this.glyphs.setAnchor(Anchor.FILL).setMargin(right, 66f, 0f, 0f)
                        .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP))
                .add(SGStrata.line(factory, right + 10f, 88f, 10f))
                .add(this.feedback.setAnchor(Anchor.FILL).setMargin(right, 96f, 0f, 0f)
                        .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP))
                .add(this.dial.setAnchor(Anchor.BOTTOM_LEFT).setMargin(right, 0f, 0f, 0f))
                .add(this.disconnect.setAnchor(Anchor.BOTTOM_LEFT).setMargin(right + 62f, 0f, 0f, 0f))
                .add(close.setAnchor(Anchor.BOTTOM_RIGHT));
        final Window window = SGStrata.window(factory, this.title(), content, StrataPddScreen.WIDTH,
                StrataPddScreen.HEIGHT);
        this.refresh();
        return factory.anchorPanel().add(window).setAnchor(Anchor.FILL);
    }

    @Override
    public void opened(final ScreenHandle handle) {
        handle.every(StrataPddScreen.REFRESH_MILLIS,
                () -> ClientPlatform.get().sendToServer(PddAction.of(PddAction.Kind.REFRESH)));
        // Opened by sneaking and using the PDD on a gate: that gate's address, ready to add, or its entry chosen. Once
        // open, as a dialog opened while the screen is still being built never shows
        if (this.state.fillAddress() && !this.filled && this.list != null) {
            this.filled = true;
            final String here = this.state.gate().map(GateInfo::address).orElse("");
            final Optional<BookEntry> existing = this.state.entries().stream().filter(BookEntry::own).findFirst();
            if (existing.isPresent()) {
                this.list.selectItem(existing.get());
                this.message = net.minecraft.network.chat.Component.translatableWithFallback(
                        "screen.sgcraft.pdd.already_listed", "%s is already in this PDD, as %s",
                        existing.get().address(), existing.get().name()).getString();
                this.refresh();
            } else if (!here.isEmpty()) {
                this.addDialog("", here);
            }
        }
    }

    // One address: its name, and its world beside it, over the address in letters, what its gate is doing and its
    // glyphs; grey for this gate's own, which it can't dial
    private Component<?> row(final ComponentFactory factory, final BookEntry entry) {
        final boolean chosen = this.list != null && this.list.selected().filter(entry::equals).isPresent();
        Text name = Text.of(entry.name()).color(entry.own() ? SGStrata.DARK_GRAY : SGStrata.WHITE);
        if (!entry.world().isEmpty()) {
            name = name.append(Text.of("  " + ExplorerScreen.worldName(entry.world()).getString()).color(SGStrata.GRAY));
        }
        final Text address = Text.of(entry.address() + " ").color(entry.own() ? SGStrata.DARK_GRAY : SGStrata.BLUE)
                .append(SGStrata.of(GateStatusText.of(entry.status())));
        final StackPanel lines = factory.stackPanel().setSpacing(1f)
                .add(factory.label(name))
                .add(factory.label(address).setTextScale(0.8f));
        lines.setHitTestVisible(false);
        final AnchorPanel row = factory.anchorPanel().add(lines.setAnchor(Anchor.MIDDLE_LEFT).setMargin(3f, 0f, 0f, 0f));
        Address.parse(entry.address()).ifPresent(parsed -> {
            final Label glyphs = factory.label(SGStrata.glyphs(parsed, this.style()));
            glyphs.setHitTestVisible(false);
            row.add(glyphs.setAnchor(Anchor.MIDDLE_RIGHT).setMargin(0f, 0f, 3f, 0f));
        });
        return SGStrata.row(row, chosen).setAnchor(Anchor.FILL)
                .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.STRETCH);
    }

    // The entries that match the search, the chosen one kept chosen; this gate's address and state; the buttons
    private void refresh() {
        if (this.list == null || this.gate == null || this.status == null || this.glyphs == null
                || this.feedback == null || this.count == null) {
            return;
        }
        final Optional<BookEntry> chosen = this.list.selected();
        final String needle = this.search.toLowerCase(Locale.ROOT).replace("-", "");
        final List<BookEntry> shown = this.state.entries().stream().filter(entry -> needle.isEmpty()
                || entry.name().toLowerCase(Locale.ROOT).contains(needle)
                || entry.world().toLowerCase(Locale.ROOT).contains(needle)
                || entry.address().toLowerCase(Locale.ROOT).replace("-", "").contains(needle)).toList();
        this.list.setItems(shown);
        chosen.flatMap(was -> shown.stream().filter(entry -> entry.address().equals(was.address())).findFirst())
                .ifPresent(this.list::selectItem);
        this.list.refresh();
        this.count.setText(SGStrata.text("screen.sgcraft.pdd.addresses_count", "Addresses: %s",
                this.state.entries().size()).color(SGStrata.GRAY));
        final Optional<GateInfo> gate = this.state.gate();
        this.gate.setText(gate.map(g -> SGStrata.text("screen.sgcraft.pdd.this_gate", "This gate: %s",
                        g.address().isEmpty() ? net.minecraft.network.chat.Component.translatableWithFallback(
                                "screen.sgcraft.pdd.hidden", "address hidden").getString() : g.address())
                        .color(SGStrata.BLUE))
                .orElse(SGStrata.text("screen.sgcraft.pdd.no_gate",
                        "No gate near you. You can still change its addresses").color(SGStrata.GRAY)));
        this.status.setText(gate.map(g -> SGStrata.of(GateStatusText.of(g.state()))).orElse(Text.empty()));
        this.glyphs.setText(gate.flatMap(g -> Address.parse(g.address())).map(a -> SGStrata.glyphs(a, this.style()))
                .orElse(Text.empty()));
        this.feedback.setText(Text.of(this.message).color(SGStrata.RED));
        this.updateButtons();
    }

    private void updateButtons() {
        if (this.list == null || this.dial == null || this.disconnect == null || this.up == null
                || this.down == null) {
            return;
        }
        final Optional<GateInfo> gate = this.state.gate();
        final boolean idle = gate.map(g -> g.state().equals("idle")).orElse(false);
        final Optional<BookEntry> chosen = this.list.selected();
        // A gate can't dial itself
        this.dial.setEnabled(idle && chosen.filter(entry -> !entry.own()).isPresent());
        this.disconnect.setEnabled(gate.isPresent() && !idle);
        // Not while searching: an entry would swap places with one that isn't shown
        this.up.setEnabled(chosen.isPresent() && this.search.isEmpty());
        this.down.setEnabled(chosen.isPresent() && this.search.isEmpty());
    }

    private DialStyle style() {
        return this.state.gate().map(PddScreen::style).orElse(DialStyle.MILKY_WAY);
    }

    // Dials the chosen entry, and closes the screen at once; why a dial fails shows over the hotbar
    private void dial() {
        if (this.list == null || this.dial == null || !this.dial.isEnabled()) {
            return;
        }
        this.list.selected().ifPresent(entry -> {
            this.send(PddAction.Kind.DIAL, entry.address(), entry.name(), 0);
            StrataHost.close();
        });
    }

    private void remove() {
        if (this.list != null) {
            this.list.selected().ifPresent(entry -> this.send(PddAction.Kind.REMOVE, entry.address(), "", 0));
        }
    }

    private void move(final int by) {
        if (this.list == null || !this.search.isEmpty()) {
            return;
        }
        this.list.selected().ifPresent(entry -> {
            final int index = this.state.entries().indexOf(entry) + by;
            if (index >= 0 && index < this.state.entries().size()) {
                this.send(PddAction.Kind.MOVE, entry.address(), entry.name(), index);
            }
        });
    }

    private void send(final PddAction.Kind kind, final String address, final String name, final int index) {
        this.message = "";
        ClientPlatform.get().sendToServer(new PddAction(kind, address, name, index));
    }

    // 2.3's entry dialog: a name and an address, added together
    private void addDialog(final String name, final String address) {
        if (this.factory == null) {
            return;
        }
        final ComponentFactory factory = this.factory;
        final TextBox nameBox = factory.textBox().setMaxLength(48).setText(name);
        nameBox.setSize(180f, SGStrata.BUTTON_HEIGHT);
        final TextBox addressBox = factory.textBox().setMaxLength(11).setText(address);
        addressBox.setSize(180f, SGStrata.BUTTON_HEIGHT);
        final Overlay[] shown = new Overlay[1];
        final Runnable save = () -> {
            this.send(PddAction.Kind.ADD, addressBox.text().strip(), nameBox.text().strip(), 0);
            shown[0].close();
        };
        nameBox.onEvent(TextBox.SUBMITTED, event -> save.run());
        addressBox.onEvent(TextBox.SUBMITTED, event -> save.run());
        shown[0] = factory.overlays().dialog(this.dialog(factory,
                SGStrata.text("screen.sgcraft.pdd.add_title", "Add Address"), nameBox, addressBox, save,
                () -> shown[0].close()));
    }

    // Renames the chosen entry
    private void renameDialog() {
        if (this.factory == null || this.list == null || this.list.selected().isEmpty()) {
            return;
        }
        final ComponentFactory factory = this.factory;
        final BookEntry entry = this.list.selected().get();
        final TextBox nameBox = factory.textBox().setMaxLength(48).setText(entry.name());
        nameBox.setSize(180f, SGStrata.BUTTON_HEIGHT);
        final TextBox addressBox = factory.textBox().setText(entry.address()).setReadOnly(true);
        addressBox.setSize(180f, SGStrata.BUTTON_HEIGHT);
        final Overlay[] shown = new Overlay[1];
        final Runnable save = () -> {
            this.send(PddAction.Kind.RENAME, entry.address(), nameBox.text().strip(), 0);
            shown[0].close();
        };
        nameBox.onEvent(TextBox.SUBMITTED, event -> save.run());
        shown[0] = factory.overlays().dialog(this.dialog(factory,
                SGStrata.text("screen.sgcraft.pdd.rename_title", "Rename Address"), nameBox, addressBox, save,
                () -> shown[0].close()));
    }

    private Component<?> dialog(final ComponentFactory factory, final Text title, final TextBox name,
            final TextBox address, final Runnable save, final Runnable cancel) {
        final AnchorPanel content = factory.anchorPanel()
                .add(factory.label(SGStrata.text("screen.sgcraft.pdd.name", "Name").append(":"))
                        .setAnchor(Anchor.TOP_LEFT).setMargin(2f, 5f, 0f, 0f))
                .add(name.setAnchor(Anchor.TOP_LEFT).setMargin(52f, 1f, 0f, 0f))
                .add(factory.label(SGStrata.text("screen.sgcraft.pdd.address", "Address").append(":"))
                        .setAnchor(Anchor.TOP_LEFT).setMargin(2f, 25f, 0f, 0f))
                .add(address.setAnchor(Anchor.TOP_LEFT).setMargin(52f, 21f, 0f, 0f))
                .add(SGStrata.button(factory, SGStrata.text("screen.sgcraft.save", "Save"), 50f, save)
                        .setAnchor(Anchor.BOTTOM_RIGHT).setMargin(0f, 0f, 54f, 0f))
                .add(SGStrata.button(factory, SGStrata.text("screen.sgcraft.cancel", "Cancel"), 50f, cancel)
                        .setAnchor(Anchor.BOTTOM_RIGHT));
        return SGStrata.closesWith(factory.window(title).setMovable(true).setResizable(false).setCollapsible(false)
                .setMaximizable(false).add(content.setAnchor(Anchor.FILL)).setSize(250f, 82f)
                .setAnchor(Anchor.CENTER), cancel);
    }
}
