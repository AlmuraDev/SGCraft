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
import com.almuradev.sgcraft.mod.tool.ExplorerPayloads.Action;
import com.almuradev.sgcraft.mod.tool.ExplorerPayloads.Listed;
import com.almuradev.sgcraft.mod.tool.ExplorerPayloads.State;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.GateInfo;
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
 * The explorer dialer drawn with Strata, laid out as SGCraft 2.3's was, which was the PDD's screen in explorer mode:
 * the gates world generation built on the left, under a search box, each with the name its holder gave it, its address
 * and whether they have visited it, and 2.3's yellow ? below them to rename one (the server decides which gates are
 * listed, so there is no + or -); the gate it dials from and what that is doing on the right, large; Dial, Disconnect
 * and Close along the bottom. It asks the server for its state every three seconds, as the standard screen does.
 */
public final class StrataExplorerScreen implements Screen {

    private static final float WIDTH = 430f;
    private static final float HEIGHT = 262f;
    private static final float LIST_WIDTH = 250f;
    private static final float ROW_HEIGHT = 24f;
    private static final long REFRESH_MILLIS = 3000L;
    private static final int MAX_NAME = 40;
    private static final String HIDDEN = "????-???-??";

    private State state;
    private String search = "";
    private @Nullable ComponentFactory factory;
    private @Nullable ListView<Listed> list;
    private @Nullable Label count;
    private @Nullable Label gate;
    private @Nullable Label status;
    private @Nullable Label glyphs;
    private @Nullable Label feedback;
    private @Nullable Button dial;
    private @Nullable Button disconnect;
    private @Nullable Button rename;

    public StrataExplorerScreen(final State state) {
        this.state = state;
    }

    @Override
    public Text title() {
        return SGStrata.text("item.sgcraft.edd", "Explorer Dialer Device");
    }

    /** Shows a newer state from the server, keeping the chosen gate chosen. */
    public void update(final State state) {
        this.state = state;
        this.refresh();
    }

    @Override
    public Component<?> build(final ComponentFactory factory) {
        this.factory = factory;
        // The generated gates, under a search box with 2.3's red x to clear it
        final Label generated = factory.label(SGStrata.text("screen.sgcraft.explorer.generated",
                "Generated Addresses").append(":").color(SGStrata.WHITE));
        final Label searchLabel = factory.label(SGStrata.text("screen.sgcraft.search", "Search").append(":"));
        final TextBox search = factory.textBox().setMaxLength(32);
        search.setHeight(SGStrata.BUTTON_HEIGHT);
        search.onEvent(TextInput.EDITED, event -> {
            this.search = event.value();
            this.refresh();
        });
        final Button clear = SGStrata.small(factory, "x", SGStrata.RED,
                SGStrata.text("screen.sgcraft.search.clear", "Clear the search"), () -> search.setText(""));
        final ListView<Listed> list = SGStrata.rows(factory.<Listed>listView(entry -> this.row(factory, entry)),
                StrataExplorerScreen.ROW_HEIGHT);
        list.onEvent(Selection.SELECTED, event -> this.updateButtons());
        // A double click dials, as 2.3's did
        list.onEvent(Activatable.ACTIVATED, event -> this.dial());
        this.list = list;
        this.count = factory.label("").setTextScale(0.8f);
        // Only the name of a generated gate can change, so only 2.3's ?, where its + and - would be
        this.rename = SGStrata.small(factory, "?", SGStrata.YELLOW,
                SGStrata.text("screen.sgcraft.pdd.rename", "Rename"), this::renameDialog);
        final AnchorPanel addresses = SGStrata.box(factory)
                .add(generated.setAnchor(Anchor.TOP_LEFT).setMargin(3f, 1f, 0f, 0f))
                .add(searchLabel.setAnchor(Anchor.TOP_LEFT).setMargin(3f, 18f, 0f, 0f))
                .add(search.setAnchor(Anchor.FILL).setMargin(44f, 14f, SGStrata.SMALL_WIDTH + 2f, 0f)
                        .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP))
                .add(clear.setHeight(SGStrata.BUTTON_HEIGHT).setAnchor(Anchor.TOP_RIGHT).setMargin(0f, 14f, 0f, 0f))
                .add(SGStrata.box(factory).add(list.setAnchor(Anchor.FILL)).setAnchor(Anchor.FILL)
                        .setMargin(0f, 33f, 0f, SGStrata.SMALL_HEIGHT + 4f))
                .add(this.rename.setAnchor(Anchor.BOTTOM_LEFT).setMargin(0f, 0f, 0f, 1f))
                .add(this.count.setAnchor(Anchor.BOTTOM_RIGHT).setMargin(0f, 0f, 3f, 4f));

        // The gate it dials from: its address, and what it is doing, large, as 2.3's were
        this.gate = factory.label("").setWrap(true);
        this.status = factory.label("").setTextScale(1.8f).setTextAlignment(HorizontalAlignment.CENTER);
        this.glyphs = factory.label("").setTextScale(1.4f).setTextAlignment(HorizontalAlignment.CENTER);
        final float right = LIST_WIDTH + 6f;
        this.dial = SGStrata.button(factory, SGStrata.text("screen.sgcraft.pdd.dial", "Dial"), 60f, this::dial);
        this.disconnect = SGStrata.button(factory, SGStrata.text("screen.sgcraft.pdd.disconnect", "Disconnect"),
                60f, () -> this.send(Action.Kind.DISCONNECT, ""));
        final Button close = SGStrata.button(factory, SGStrata.text("screen.sgcraft.close", "Close"), 40f,
                StrataHost::close);
        this.feedback = factory.label("").setTextScale(0.8f).setWrap(true);
        final AnchorPanel content = factory.anchorPanel()
                .add(addresses.setAnchor(Anchor.FILL).setMargin(0f, 0f, 0f, 0f)
                        .setAlignment(HorizontalAlignment.LEFT, VerticalAlignment.STRETCH).setWidth(LIST_WIDTH))
                .add(this.gate.setAnchor(Anchor.FILL).setMargin(right, 3f, 0f, 0f)
                        .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP))
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
        final Window window = SGStrata.window(factory, this.title(), content, StrataExplorerScreen.WIDTH,
                StrataExplorerScreen.HEIGHT);
        this.refresh();
        return factory.anchorPanel().add(window).setAnchor(Anchor.FILL);
    }

    @Override
    public void opened(final ScreenHandle handle) {
        handle.every(StrataExplorerScreen.REFRESH_MILLIS,
                () -> ClientPlatform.get().sendToServer(new Action(Action.Kind.REFRESH, "", "")));
    }

    // One generated gate: its name, or that it has none, and its world beside it, with whether the player has been
    // there at the right, where the PDD has its arrows; below, its address in letters and what its gate is doing, with
    // its glyphs at the right
    private Component<?> row(final ComponentFactory factory, final Listed entry) {
        final boolean chosen = this.list != null && this.list.selected().filter(entry::equals).isPresent();
        Text name = entry.name().isEmpty()
                ? SGStrata.text("screen.sgcraft.explorer.unnamed", "Unnamed gate").color(SGStrata.GRAY)
                : Text.of(entry.name()).color(SGStrata.WHITE);
        if (!entry.world().isEmpty()) {
            name = name.append(Text.of("  " + ExplorerScreen.worldName(entry.world()).getString()).color(SGStrata.GRAY));
        }
        final String letters = entry.address().isEmpty() ? StrataExplorerScreen.HIDDEN : entry.address();
        final Text address = Text.of(letters + " ").color(SGStrata.BLUE).append(SGStrata.of(GateStatusText.of(entry.status())));
        final StackPanel lines = factory.stackPanel().setSpacing(1f)
                .add(factory.label(name))
                .add(factory.label(address).setTextScale(0.8f));
        lines.setHitTestVisible(false);
        final Label visited = factory.label(entry.visited()
                ? SGStrata.text("screen.sgcraft.explorer.visited", "Visited").color(SGStrata.GREEN)
                : SGStrata.text("screen.sgcraft.explorer.not_visited", "Not visited").color(SGStrata.DARK_GRAY))
                .setTextScale(0.8f);
        visited.setHitTestVisible(false);
        final AnchorPanel row = factory.anchorPanel()
                .add(lines.setAnchor(Anchor.MIDDLE_LEFT).setMargin(3f, 0f, 0f, 0f))
                .add(visited.setAnchor(Anchor.TOP_RIGHT).setMargin(0f, 2f, 3f, 0f));
        Address.parse(entry.address()).ifPresent(parsed -> {
            // In Milky Way glyphs, as the standard screen draws them
            final Label glyphs = factory.label(SGStrata.glyphs(parsed, DialStyle.MILKY_WAY)).setTextScale(0.8f);
            glyphs.setHitTestVisible(false);
            row.add(glyphs.setAnchor(Anchor.BOTTOM_RIGHT).setMargin(0f, 0f, 3f, 2f));
        });
        return SGStrata.row(row, chosen).setAnchor(Anchor.FILL)
                .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.STRETCH);
    }

    // The gates that match the search, the chosen one kept chosen; how many there are; the gate it dials from
    private void refresh() {
        if (this.list == null || this.gate == null || this.status == null || this.glyphs == null
                || this.feedback == null || this.count == null) {
            return;
        }
        final Optional<Listed> chosen = this.list.selected();
        final List<Listed> shown = this.state.entries().stream().filter(this::matches).toList();
        this.list.setItems(shown);
        chosen.flatMap(was -> shown.stream().filter(entry -> entry.gate().equals(was.gate())).findFirst())
                .ifPresent(this.list::selectItem);
        this.list.refresh();
        final long visited = this.state.entries().stream().filter(Listed::visited).count();
        Text count = SGStrata.text("screen.sgcraft.explorer.count", "Generated Addresses: (%s)", this.state.total())
                .color(SGStrata.WHITE)
                .append(SGStrata.text("screen.sgcraft.explorer.visited_count", " - Visited: %s", visited)
                        .color(SGStrata.GRAY));
        // A server with more gates than one packet holds sends those visited or named first
        if (this.state.entries().size() < this.state.total()) {
            count = count.append(SGStrata.text("screen.sgcraft.explorer.shown", " - Showing %s",
                    this.state.entries().size()).color(SGStrata.GRAY));
        }
        this.count.setText(count);
        final Optional<GateInfo> gate = this.state.gate();
        this.gate.setText(gate.map(g -> SGStrata.text("screen.sgcraft.explorer.from", "Dialing from %s",
                        g.address().isEmpty() ? StrataExplorerScreen.HIDDEN : g.address()).color(SGStrata.BLUE))
                .orElse(SGStrata.text("screen.sgcraft.explorer.no_gate", "No gate near you to dial from")
                        .color(SGStrata.GRAY)));
        this.status.setText(gate.map(g -> SGStrata.of(GateStatusText.of(g.state()))).orElse(Text.empty()));
        this.glyphs.setText(gate.flatMap(g -> Address.parse(g.address()).map(a -> SGStrata.glyphs(a,
                PddScreen.style(g)))).orElse(Text.empty()));
        this.feedback.setText(Text.of(this.state.message()).color(SGStrata.RED));
        this.updateButtons();
    }

    // As the standard screen's search: by name, by world, or by address with or without its dashes
    private boolean matches(final Listed entry) {
        final String query = this.search.strip().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return true;
        }
        final String symbols = query.replace("-", "");
        return entry.name().toLowerCase(Locale.ROOT).contains(query)
                || !entry.world().isEmpty() && (entry.world().toLowerCase(Locale.ROOT).contains(query)
                        || ExplorerScreen.worldName(entry.world()).getString().toLowerCase(Locale.ROOT)
                                .contains(query))
                || !symbols.isEmpty() && entry.address().replace("-", "").toLowerCase(Locale.ROOT).contains(symbols);
    }

    private void updateButtons() {
        if (this.list == null || this.dial == null || this.disconnect == null || this.rename == null) {
            return;
        }
        final Optional<GateInfo> gate = this.state.gate();
        final boolean idle = gate.map(g -> g.state().equals("idle")).orElse(false);
        final Optional<Listed> chosen = this.list.selected();
        // A gate whose address is hidden or out of reach can't be dialed
        this.dial.setEnabled(idle && chosen.filter(entry -> !entry.address().isEmpty()).isPresent());
        this.disconnect.setEnabled(gate.isPresent() && !idle);
        this.rename.setEnabled(chosen.isPresent());
    }

    // Dials the chosen gate from the one near the player; the screen stays open, as the standard one does
    private void dial() {
        if (this.dial != null && this.dial.isEnabled()) {
            this.send(Action.Kind.DIAL, "");
        }
    }

    private void send(final Action.Kind kind, final String name) {
        final String gate = this.list == null ? "" : this.list.selected().map(Listed::gate).orElse("");
        ClientPlatform.get().sendToServer(new Action(kind, gate, name));
    }

    // 2.3's entry dialog with only the name to change: the rest belongs to the generated gate
    private void renameDialog() {
        if (this.factory == null || this.list == null || this.list.selected().isEmpty()) {
            return;
        }
        final ComponentFactory factory = this.factory;
        final Listed entry = this.list.selected().get();
        final TextBox nameBox = factory.textBox().setMaxLength(StrataExplorerScreen.MAX_NAME).setText(entry.name());
        nameBox.setSize(180f, SGStrata.BUTTON_HEIGHT);
        final TextBox addressBox = factory.textBox()
                .setText(entry.address().isEmpty() ? StrataExplorerScreen.HIDDEN : entry.address()).setReadOnly(true);
        addressBox.setSize(180f, SGStrata.BUTTON_HEIGHT);
        final Overlay[] shown = new Overlay[1];
        final Runnable save = () -> {
            ClientPlatform.get().sendToServer(new Action(Action.Kind.RENAME, entry.gate(), nameBox.text().strip()));
            shown[0].close();
        };
        nameBox.onEvent(TextBox.SUBMITTED, event -> save.run());
        final AnchorPanel content = factory.anchorPanel()
                .add(factory.label(SGStrata.text("screen.sgcraft.explorer.name", "Name").append(":"))
                        .setAnchor(Anchor.TOP_LEFT).setMargin(2f, 5f, 0f, 0f))
                .add(nameBox.setAnchor(Anchor.TOP_LEFT).setMargin(52f, 1f, 0f, 0f))
                .add(factory.label(SGStrata.text("screen.sgcraft.pdd.address", "Address").append(":"))
                        .setAnchor(Anchor.TOP_LEFT).setMargin(2f, 25f, 0f, 0f))
                .add(addressBox.setAnchor(Anchor.TOP_LEFT).setMargin(52f, 21f, 0f, 0f))
                .add(SGStrata.button(factory, SGStrata.text("screen.sgcraft.save", "Save"), 50f, save)
                        .setAnchor(Anchor.BOTTOM_RIGHT).setMargin(0f, 0f, 54f, 0f))
                .add(SGStrata.button(factory, SGStrata.text("screen.sgcraft.cancel", "Cancel"), 50f,
                        () -> shown[0].close()).setAnchor(Anchor.BOTTOM_RIGHT));
        shown[0] = factory.overlays().dialog(SGStrata.closesWith(factory.window(SGStrata.text(
                "screen.sgcraft.pdd.rename_title", "Rename Address")).setMovable(true).setResizable(false)
                .setCollapsible(false).setMaximizable(false).add(content.setAnchor(Anchor.FILL)).setSize(250f, 82f)
                .setAnchor(Anchor.CENTER), () -> shown[0].close()));
    }
}
