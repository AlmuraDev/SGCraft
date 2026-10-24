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
import com.almuradev.sgcraft.mod.client.ClientPlatform;
import com.almuradev.sgcraft.mod.gate.DialStyle;
import com.almuradev.sgcraft.mod.gate.GlyphText;
import com.almuradev.sgcraft.mod.tool.ExplorerPayloads.Action;
import com.almuradev.sgcraft.mod.tool.ExplorerPayloads.Listed;
import com.almuradev.sgcraft.mod.tool.ExplorerPayloads.State;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.GateInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * The explorer dialer's screen, as in SGCraft 2.2.0: every gate world generation built, by world, each with the name
 * its holder gave it, its address and whether they have visited it. Choosing one puts its name in the box below, to
 * rename it, and it dials from the gate near the holder. The list keeps up while the screen is open, and the search box
 * above it narrows it.
 */
public final class ExplorerScreen extends Screen {

    private static final int LIST_WIDTH = 340;
    private static final int ROW = 24;
    private static final int REFRESH_TICKS = 60;
    private static final int BOTTOM = 76;
    // Where the search box is, and the list below it
    private static final int SEARCH_TOP = 42;
    private static final int LIST_TOP = ExplorerScreen.SEARCH_TOP + ListSearch.HEIGHT + 4;

    private State state;
    private final ListSearch search = new ListSearch();
    private @Nullable Entries list;
    private @Nullable EditBox name;
    private @Nullable Button dial;
    private @Nullable Button disconnect;
    private @Nullable Button rename;
    private int ticks;

    public ExplorerScreen(final State state) {
        super(Component.translatableWithFallback("item.sgcraft.edd", "Explorer Dialer Device"));
        this.state = state;
    }

    /** Shows a newer state from the server, keeping the chosen gate and what the player has typed. */
    public void update(final State state) {
        this.state = state;
        this.refill();
        this.updateButtons();
    }

    // Shows the gates that match the search, keeping the chosen one chosen while it is shown
    private void refill() {
        if (this.list != null) {
            this.list.fill(this.state.entries(), this.chosen().map(Listed::gate).orElse(""));
        }
    }

    @Override
    protected void init() {
        final int left = (this.width - ExplorerScreen.LIST_WIDTH) / 2;
        final int bottom = this.height - ExplorerScreen.BOTTOM;
        this.addRenderableWidget(this.search.box(this.font, left, ExplorerScreen.SEARCH_TOP, ExplorerScreen.LIST_WIDTH,
                () -> {
                    this.refill();
                    this.updateButtons();
                }));
        this.addRenderableWidget(this.search.clear(left, ExplorerScreen.SEARCH_TOP, ExplorerScreen.LIST_WIDTH));
        this.list = this.addRenderableWidget(new Entries(this.minecraft, ExplorerScreen.LIST_WIDTH,
                bottom - ExplorerScreen.LIST_TOP, ExplorerScreen.LIST_TOP));
        this.list.setX(left);
        this.list.fill(this.state.entries(), "");
        this.name = this.addRenderableWidget(new EditBox(this.font, left, bottom + 6, 250, 20,
                Component.translatableWithFallback("screen.sgcraft.explorer.name", "Name")));
        this.name.setMaxLength(40);
        this.name.setHint(Component.translatableWithFallback("screen.sgcraft.explorer.name", "Name"));
        this.rename = this.addRenderableWidget(ExplorerScreen.button("screen.sgcraft.pdd.rename", "Rename",
                left + 254, bottom + 6, 86, () -> this.send(Action.Kind.RENAME)));
        final int row = bottom + 32;
        this.dial = this.addRenderableWidget(ExplorerScreen.button("screen.sgcraft.pdd.dial", "Dial", left, row,
                110, () -> this.send(Action.Kind.DIAL)));
        this.disconnect = this.addRenderableWidget(ExplorerScreen.button("screen.sgcraft.pdd.disconnect",
                "Disconnect", left + 115, row, 110, () -> this.send(Action.Kind.DISCONNECT)));
        this.addRenderableWidget(ExplorerScreen.button("screen.sgcraft.close", "Close", left + 230, row, 110,
                this::onClose));
        this.updateButtons();
    }

    private static Button button(final String key, final String fallback, final int x, final int y, final int width,
            final Runnable action) {
        return Button.builder(Component.translatableWithFallback(key, fallback), b -> action.run())
                .bounds(x, y, width, 20).build();
    }

    private Optional<Listed> chosen() {
        return this.list == null || this.list.getSelected() == null ? Optional.empty()
                : Optional.of(this.list.getSelected().entry);
    }

    private void updateButtons() {
        final Optional<GateInfo> gate = this.state.gate();
        final boolean idle = gate.map(g -> g.state().equals("idle")).orElse(false);
        final Optional<Listed> chosen = this.chosen();
        if (this.dial != null) {
            this.dial.active = idle && chosen.map(entry -> !entry.address().isEmpty()).orElse(false);
        }
        if (this.disconnect != null) {
            this.disconnect.active = gate.isPresent() && !idle;
        }
        if (this.rename != null) {
            this.rename.active = chosen.isPresent();
        }
    }

    private void send(final Action.Kind kind) {
        final String gate = this.chosen().map(Listed::gate).orElse("");
        final String name = this.name == null ? "" : this.name.getValue();
        ClientPlatform.get().sendToServer(new Action(kind, gate, name));
    }

    // Choosing a gate puts its name in the box, ready to rename it
    void choose(final Listed entry) {
        if (this.name != null) {
            this.name.setValue(entry.name());
        }
        this.updateButtons();
    }

    @Override
    public void tick() {
        if (++this.ticks % ExplorerScreen.REFRESH_TICKS == 0) {
            ClientPlatform.get().sendToServer(new Action(Action.Kind.REFRESH, "", ""));
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
        final long visited = this.state.entries().stream().filter(Listed::visited).count();
        final MutableComponent count = Component.translatableWithFallback("screen.sgcraft.explorer.count",
                "Generated Addresses: (%s)", this.state.total())
                .append(Component.translatableWithFallback("screen.sgcraft.explorer.visited_count",
                        " - Visited: %s", visited).withStyle(ChatFormatting.GRAY));
        // A server with more gates than one packet holds sends those visited or named first
        if (this.state.entries().size() < this.state.total()) {
            count.append(Component.translatableWithFallback("screen.sgcraft.explorer.shown", " - Showing %s",
                    this.state.entries().size()).withStyle(ChatFormatting.GRAY));
        }
        graphics.centeredText(this.font, count, this.width / 2, 20, 0xFFFFFFFF);
        final Component gate = this.state.gate().map(g -> Component.translatableWithFallback(
                "screen.sgcraft.explorer.from", "Dialing from %s", g.address().isEmpty() ? "????-???-??"
                        : g.address())).orElse(Component.translatableWithFallback("screen.sgcraft.explorer.no_gate",
                "No gate near you to dial from"));
        graphics.centeredText(this.font, gate, this.width / 2, 31, 0xFFAAAAAA);
        if (!this.state.message().isEmpty()) {
            graphics.centeredText(this.font, this.state.message(), this.width / 2, this.height - 14, 0xFFFF6060);
        }
    }

    /** The generated gates, one a row: its name or world, its address in letters and glyphs, and if it was visited. */
    final class Entries extends ObjectSelectionList<Entries.Row> {

        Entries(final Minecraft minecraft, final int width, final int height, final int y) {
            super(minecraft, width, height, y, ExplorerScreen.ROW);
        }

        void fill(final List<Listed> entries, final String selected) {
            this.clearEntries();
            for (final Listed entry : entries) {
                if (!ExplorerScreen.this.search.matches(entry.name(), entry.world(), entry.address())) {
                    continue;
                }
                final Row row = new Row(entry);
                this.addEntry(row);
                if (entry.gate().equals(selected)) {
                    this.setSelected(row);
                }
            }
        }

        @Override
        public int getRowWidth() {
            return ExplorerScreen.LIST_WIDTH - 12;
        }

        final class Row extends ObjectSelectionList.Entry<Row> {

            private final Listed entry;

            Row(final Listed entry) {
                this.entry = entry;
            }

            private Component title() {
                return this.entry.name().isEmpty() ? Component.translatableWithFallback(
                        "screen.sgcraft.explorer.unnamed", "Unnamed gate").withStyle(ChatFormatting.GRAY)
                        : Component.literal(this.entry.name());
            }

            @Override
            public Component getNarration() {
                return Component.empty().append(this.title()).append(", " + this.entry.address());
            }

            @Override
            public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
                    final boolean hovered, final float a) {
                final ExplorerScreen screen = ExplorerScreen.this;
                graphics.text(screen.font, this.title(), this.getContentX() + 2, this.getContentY() + 1, 0xFFFFFFFF);
                final String address = this.entry.address().isEmpty() ? "????-???-??" : this.entry.address();
                graphics.text(screen.font, address, this.getContentX() + 2, this.getContentY() + 11, 0xFF8FB8FF);
                int x = this.getContentX() + 6 + screen.font.width(address);
                final Optional<Address> parsed = Address.parse(this.entry.address());
                if (parsed.isPresent()) {
                    final Component glyphs = GlyphText.of(parsed.get(), DialStyle.MILKY_WAY);
                    graphics.text(screen.font, glyphs, x, this.getContentY() + 11, 0xFFFFFFFF);
                    x += screen.font.width(glyphs) + 4;
                }
                graphics.text(screen.font, ExplorerScreen.worldName(this.entry.world()), x, this.getContentY() + 11,
                        0xFF808080);
                final Component visited = this.entry.visited()
                        ? Component.translatableWithFallback("screen.sgcraft.explorer.visited", "Visited")
                                .withStyle(ChatFormatting.GREEN)
                        : Component.translatableWithFallback("screen.sgcraft.explorer.not_visited", "Not visited")
                                .withStyle(ChatFormatting.DARK_GRAY);
                graphics.text(screen.font, visited, this.getContentRight() - screen.font.width(visited) - 2,
                        this.getContentY() + 1, 0xFFFFFFFF);
            }

            @Override
            public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
                Entries.this.setSelected(this);
                ExplorerScreen.this.choose(this.entry);
                if (doubleClick && ExplorerScreen.this.dial != null && ExplorerScreen.this.dial.active) {
                    ExplorerScreen.this.send(Action.Kind.DIAL);
                }
                return true;
            }
        }
    }

    // A world's name as players read it: the vanilla worlds by name, others by their identifier's path
    static Component worldName(final String world) {
        return switch (world) {
            case "minecraft:overworld" -> Component.translatableWithFallback("screen.sgcraft.world.overworld",
                    "Overworld");
            case "minecraft:the_nether" -> Component.translatableWithFallback("screen.sgcraft.world.nether",
                    "The Nether");
            case "minecraft:the_end" -> Component.translatableWithFallback("screen.sgcraft.world.end", "The End");
            default -> Component.literal(world.substring(world.indexOf(':') + 1));
        };
    }
}
