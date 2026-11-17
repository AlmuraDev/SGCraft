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

package com.almuradev.sgcraft.mod.client.strata.obelisk;

import com.almuradev.sgcraft.mod.client.ClientPlatform;
import com.almuradev.sgcraft.mod.client.strata.SGStrata;
import com.almuradev.sgcraft.mod.client.strata.StrataHost;
import com.almuradev.sgcraft.mod.client.strata.StrataMenuScreen;
import com.almuradev.sgcraft.mod.obelisk.ObeliskPayloads;
import com.almuradev.sgcraft.mod.obelisk.ObeliskPayloads.Entry;
import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.attribute.item.Selection;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.RadioButton;
import org.inspirenxe.strata.api.control.button.RadioGroup;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.text.Text;
import org.jspecify.annotations.Nullable;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * The screen of an Asgard obelisk or a ring platform, opened with the configurator, drawn with Strata and laid out as
 * SGCraft 2.3's MalisisCore one was: what it is linked to, with Unlink beside it, its owner and a ring platform's
 * power at the top; the players on its list on the left, under a name box with + to add one (by name, while they are
 * online) and - to take the chosen one off; on the right whether the chosen player may use it and configure it, and
 * whether anyone may use it or only those listed; Save and Close at the bottom. Changes stay here until saved.
 */
public final class StrataObeliskScreen implements Screen {

    private static final float WIDTH = 360f;
    private static final float HEIGHT = 228f;
    // How much lower everything under the owner sits when there are two lines of power to show
    private static final float POWER_ROOM = 22f;
    private static final float PLAYERS_WIDTH = 165f;
    private static final float OPTIONS_WIDTH = 180f;
    private static final float BOX_HEIGHT = 160f;
    private static final float LIST_HEIGHT = 112f;
    private static final float ROW_HEIGHT = 15f;

    private ObeliskPayloads.State state;
    private boolean everyone;
    private final List<Entry> entries = new ArrayList<>();
    // While this screen sets the radio buttons itself, so what they report is not taken as the player's change
    private boolean updating;
    private @Nullable ListView<Entry> list;
    private @Nullable Label nobody;
    private @Nullable Label link;
    private @Nullable Label owner;
    private @Nullable Label energy;
    private @Nullable Label naquadah;
    private @Nullable Label selected;
    private @Nullable Label hint;
    private @Nullable Label message;
    private @Nullable Button unlink;
    private @Nullable Button remove;
    private @Nullable RadioGroup<Boolean> use;
    private @Nullable RadioGroup<Boolean> admin;
    private @Nullable RadioGroup<Boolean> everyoneGroup;

    public StrataObeliskScreen(final ObeliskPayloads.State state) {
        this.state = state;
        this.everyone = state.everyone();
        this.entries.addAll(state.entries());
    }

    @Override
    public Text title() {
        return this.state.power().isPresent()
                ? SGStrata.text("screen.sgcraft.rings", "Transport Rings")
                : SGStrata.text("screen.sgcraft.obelisk", "Asgard Obelisk");
    }

    /** Shows the settings as the server now has them, after saving, unlinking or adding a player. */
    public void update(final ObeliskPayloads.State state) {
        final Optional<UUID> chosen = this.chosen().map(Entry::player);
        this.state = state;
        this.everyone = state.everyone();
        this.entries.clear();
        this.entries.addAll(state.entries());
        this.refresh();
        this.refreshList(chosen.orElse(null));
    }

    @Override
    public Component<?> build(final ComponentFactory factory) {
        final float room = this.state.power().isPresent() ? StrataObeliskScreen.POWER_ROOM : 0f;
        // ---- What it is linked to, its owner, and a ring platform's power ----
        this.link = factory.label("");
        this.owner = factory.label("");
        this.energy = factory.label("");
        this.naquadah = factory.label("");
        this.unlink = SGStrata.button(factory, SGStrata.text("screen.sgcraft.obelisk.unlink", "Unlink"), 50f,
                () -> this.send(true, ""));
        // ---- The players ----
        final ListView<Entry> list = SGStrata.rows(factory.<Entry>listView(entry -> this.row(factory, entry)),
                StrataObeliskScreen.ROW_HEIGHT);
        list.onEvent(Selection.SELECTED, event -> this.showSelected());
        this.list = list;
        this.nobody = factory.label(SGStrata.text("screen.sgcraft.obelisk.nobody", "No players are listed")
                .color(SGStrata.GRAY)).setTextScale(0.8f);
        this.nobody.setHitTestVisible(false);
        final TextBox name = factory.textBox().setMaxLength(16)
                .setPlaceholder(SGStrata.text("screen.sgcraft.obelisk.player_name", "Player name")
                        .color(SGStrata.DARK_GRAY));
        name.setSize(100f, 14f);
        final Runnable add = () -> {
            final String typed = name.text().strip();
            if (!typed.isEmpty()) {
                this.send(false, typed);
                name.setText("");
            }
        };
        name.onEvent(TextBox.SUBMITTED, event -> add.run());
        this.remove = SGStrata.small(factory, "-", SGStrata.RED,
                SGStrata.text("screen.sgcraft.obelisk.remove", "Take the chosen player off the list"), this::remove);
        final AnchorPanel players = SGStrata.box(factory)
                .add(factory.label(SGStrata.text("screen.sgcraft.obelisk.players", "Player List")
                        .color(SGStrata.WHITE)).setAnchor(Anchor.TOP_CENTER).setMargin(0f, 1f, 0f, 0f))
                .add(SGStrata.box(factory).add(list.setAnchor(Anchor.FILL)).setAnchor(Anchor.FILL)
                        .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP)
                        .setMargin(0f, 12f, 0f, 0f).setHeight(StrataObeliskScreen.LIST_HEIGHT))
                .add(this.nobody.setAnchor(Anchor.TOP_LEFT).setMargin(5f, 17f, 0f, 0f))
                .add(factory.label(SGStrata.text("screen.sgcraft.obelisk.type_name", "Type a player's name")
                        .color(SGStrata.WHITE)).setAnchor(Anchor.BOTTOM_LEFT).setMargin(2f, 0f, 0f, 18f))
                .add(name.setAnchor(Anchor.BOTTOM_LEFT).setMargin(2f, 0f, 0f, 1f))
                .add(SGStrata.small(factory, "+", SGStrata.GREEN,
                        SGStrata.text("screen.sgcraft.obelisk.add", "Add player"), add)
                        .setAnchor(Anchor.BOTTOM_RIGHT).setMargin(0f, 0f, SGStrata.SMALL_WIDTH + 4f, 0f))
                .add(this.remove.setAnchor(Anchor.BOTTOM_RIGHT).setMargin(0f, 0f, 2f, 0f));
        players.setSize(StrataObeliskScreen.PLAYERS_WIDTH, StrataObeliskScreen.BOX_HEIGHT);
        // ---- The chosen player's options, and who else may use it ----
        this.selected = factory.label("");
        final RadioGroup<Boolean> use = factory.radioGroup();
        final RadioGroup<Boolean> admin = factory.radioGroup();
        final RadioGroup<Boolean> everyone = factory.radioGroup();
        this.use = use;
        this.admin = admin;
        this.everyoneGroup = everyone;
        use.onEvent(Selection.SELECTED, event -> this.changeChosen());
        admin.onEvent(Selection.SELECTED, event -> this.changeChosen());
        everyone.onEvent(Selection.SELECTED, event -> {
            if (!this.updating) {
                this.everyone = everyone.selected().orElse(this.everyone);
                this.showHint();
            }
        });
        this.hint = factory.label("").setTextScale(0.8f).setWrap(true);
        final AnchorPanel options = SGStrata.box(factory)
                .add(factory.label(SGStrata.text("screen.sgcraft.obelisk.options", "Options").color(SGStrata.WHITE))
                        .setAnchor(Anchor.TOP_CENTER).setMargin(0f, 1f, 0f, 0f))
                .add(SGStrata.line(factory, 0f, 11f, 0f))
                .add(this.selected.setAnchor(Anchor.TOP_LEFT).setMargin(3f, 15f, 0f, 0f))
                .add(this.optionLabel(factory, "access", "Access", 33f))
                .add(this.radio(factory, use, true, "allow", "Allow", 68f, 29f))
                .add(this.radio(factory, use, false, "deny", "Deny", 118f, 29f))
                .add(this.optionLabel(factory, "admin", "Admin", 51f))
                .add(this.radio(factory, admin, true, "allow", "Allow", 68f, 47f))
                .add(this.radio(factory, admin, false, "deny", "Deny", 118f, 47f))
                .add(SGStrata.line(factory, 8f, 70f, 8f))
                .add(factory.label(SGStrata.text("screen.sgcraft.obelisk.default", "Default access").append(":")
                        .color(SGStrata.WHITE)).setAnchor(Anchor.TOP_LEFT).setMargin(3f, 76f, 0f, 0f))
                .add(this.radio(factory, everyone, true, "default_everyone", "Everyone", 8f, 88f))
                .add(this.radio(factory, everyone, false, "default_listed", "Listed only", 8f, 106f))
                .add(this.hint.setAnchor(Anchor.TOP_LEFT).setMargin(3f, 126f, 0f, 0f)
                        .setWidth(StrataObeliskScreen.OPTIONS_WIDTH - 10f));
        options.setSize(StrataObeliskScreen.OPTIONS_WIDTH, StrataObeliskScreen.BOX_HEIGHT);
        // ---- Saving ----
        this.message = factory.label("").setTextScale(0.8f).setWrap(true);
        final AnchorPanel content = factory.anchorPanel()
                .add(this.link.setAnchor(Anchor.TOP_LEFT).setMargin(3f, 2f, 0f, 0f))
                .add(this.owner.setAnchor(Anchor.TOP_LEFT).setMargin(3f, 12f, 0f, 0f))
                .add(this.unlink.setAnchor(Anchor.TOP_RIGHT).setMargin(0f, 0f, 1f, 0f))
                .add(players.setAnchor(Anchor.TOP_LEFT).setMargin(0f, 23f + room, 0f, 0f))
                .add(options.setAnchor(Anchor.TOP_RIGHT).setMargin(0f, 23f + room, 0f, 0f))
                .add(this.message.setAnchor(Anchor.BOTTOM_LEFT).setMargin(3f, 0f, 0f, 2f)
                        .setWidth(StrataObeliskScreen.WIDTH - 120f))
                .add(SGStrata.button(factory, SGStrata.text("screen.sgcraft.obelisk.save", "Save"), 45f,
                        () -> this.send(false, "")).setAnchor(Anchor.BOTTOM_RIGHT).setMargin(0f, 0f, 50f, 0f))
                .add(SGStrata.button(factory, SGStrata.text("screen.sgcraft.close", "Close"), 45f,
                        StrataHost::close).setAnchor(Anchor.BOTTOM_RIGHT));
        if (room > 0f) {
            content.add(this.energy.setAnchor(Anchor.TOP_LEFT).setMargin(3f, 22f, 0f, 0f))
                    .add(this.naquadah.setAnchor(Anchor.TOP_LEFT).setMargin(3f, 32f, 0f, 0f));
        }
        final Component<?> window = SGStrata.window(factory, this.title(), content, StrataObeliskScreen.WIDTH,
                StrataObeliskScreen.HEIGHT + room);
        this.refresh();
        this.refreshList(null);
        return factory.anchorPanel().add(window).setAnchor(Anchor.FILL);
    }

    private Component<?> optionLabel(final ComponentFactory factory, final String key, final String fallback,
            final float y) {
        return factory.label(SGStrata.text("screen.sgcraft.obelisk." + key, fallback).append(":")
                .color(SGStrata.WHITE)).setAnchor(Anchor.TOP_LEFT).setMargin(3f, y, 0f, 0f);
    }

    private RadioButton radio(final ComponentFactory factory, final RadioGroup<Boolean> group, final boolean value,
            final String key, final String fallback, final float x, final float y) {
        final RadioButton radio = group.add(SGStrata.text("screen.sgcraft.obelisk." + key, fallback)
                .color(SGStrata.WHITE), value);
        radio.setAnchor(Anchor.TOP_LEFT).setMargin(x, y, 0f, 0f);
        return radio;
    }

    // A listed player: their name, and whether they may use it and configure it, as 2.3's tags
    private Component<?> row(final ComponentFactory factory, final Entry entry) {
        final boolean chosen = this.chosen().filter(entry::equals).isPresent();
        Text tags = entry.use()
                ? SGStrata.text("screen.sgcraft.obelisk.tag_use", "Use").color(SGStrata.GREEN)
                : SGStrata.text("screen.sgcraft.obelisk.tag_denied", "Denied").color(SGStrata.RED);
        if (entry.admin()) {
            tags = tags.append(" ").append(SGStrata.text("screen.sgcraft.obelisk.tag_admin", "Admin")
                    .color(SGStrata.GOLD));
        }
        final Label name = factory.label(Text.of(entry.name()).color(SGStrata.WHITE));
        name.setHitTestVisible(false);
        final Label tagLabel = factory.label(tags);
        tagLabel.setHitTestVisible(false);
        final AnchorPanel row = factory.anchorPanel()
                .add(name.setAnchor(Anchor.MIDDLE_LEFT).setMargin(4f, 0f, 0f, 0f))
                .add(tagLabel.setAnchor(Anchor.MIDDLE_RIGHT).setMargin(0f, 0f, 4f, 0f));
        return SGStrata.row(row, chosen).setAnchor(Anchor.FILL)
                .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.STRETCH);
    }

    private Optional<Entry> chosen() {
        return this.list == null ? Optional.empty() : this.list.selected();
    }

    // The list as edited here, with a player chosen: the one given, or else the first, as 2.3 did
    private void refreshList(final @Nullable UUID select) {
        if (this.list == null || this.nobody == null) {
            return;
        }
        this.list.setItems(List.copyOf(this.entries));
        final Optional<Entry> pick = this.entries.stream()
                .filter(entry -> select == null || entry.player().equals(select)).findFirst()
                .or(() -> this.entries.stream().findFirst());
        if (pick.isPresent()) {
            this.list.selectItem(pick.get());
        } else {
            this.list.select(-1);
        }
        this.list.refresh();
        this.nobody.setVisible(this.entries.isEmpty());
        this.showSelected();
    }

    // The chosen player's options, or none
    private void showSelected() {
        if (this.selected == null || this.use == null || this.admin == null || this.remove == null) {
            return;
        }
        final Optional<Entry> chosen = this.chosen();
        this.selected.setText(chosen.map(entry -> Text.of(entry.name()).color(SGStrata.AQUA))
                .orElse(SGStrata.text("screen.sgcraft.obelisk.none_selected", "No player selected")
                        .color(SGStrata.GRAY)));
        this.updating = true;
        try {
            for (final RadioGroup<Boolean> group : List.of(this.use, this.admin)) {
                group.buttons().forEach(button -> button.setEnabled(chosen.isPresent()));
            }
            if (chosen.isPresent()) {
                this.use.selectItem(chosen.get().use());
                this.admin.selectItem(chosen.get().admin());
            } else {
                this.use.select(-1);
                this.admin.select(-1);
            }
        } finally {
            this.updating = false;
        }
        this.remove.setEnabled(chosen.isPresent());
    }

    // The player picked Allow or Deny for the chosen player: its tags in the list show the change
    private void changeChosen() {
        if (this.updating || this.use == null || this.admin == null) {
            return;
        }
        final Optional<Entry> chosen = this.chosen();
        if (chosen.isEmpty()) {
            return;
        }
        final Entry now = chosen.get();
        final int index = this.entries.indexOf(now);
        if (index < 0) {
            return;
        }
        this.entries.set(index, new Entry(now.player(), now.name(), this.use.selected().orElse(now.use()),
                this.admin.selected().orElse(now.admin())));
        this.refreshList(now.player());
    }

    private void remove() {
        this.chosen().ifPresent(entry -> {
            this.entries.remove(entry);
            this.refreshList(null);
        });
    }

    // What it is linked to, its owner and power, who may use it, and what the server last said
    private void refresh() {
        if (this.link == null || this.owner == null || this.energy == null || this.naquadah == null
                || this.unlink == null || this.message == null || this.everyoneGroup == null) {
            return;
        }
        this.link.setText(this.state.link().isEmpty()
                ? SGStrata.text("screen.sgcraft.obelisk.not_linked", "Not linked").color(SGStrata.GRAY)
                : StrataMenuScreen.text(SGStrata.WHITE, "screen.sgcraft.obelisk.linked", "Linked to %s",
                        StrataMenuScreen.value(this.state.link(), SGStrata.AQUA)));
        this.owner.setText(this.state.owner().isEmpty()
                ? SGStrata.text("screen.sgcraft.obelisk.no_owner", "Owner: none").color(SGStrata.GRAY)
                : StrataMenuScreen.text(SGStrata.GRAY, "screen.sgcraft.obelisk.owner", "Owner: %s",
                        StrataMenuScreen.value(this.state.owner(), SGStrata.WHITE)));
        this.state.power().ifPresent(power -> {
            final NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.ROOT);
            // On two lines, as 2.3's: on one it would run past the window
            this.energy.setText(StrataMenuScreen.text(SGStrata.GRAY, "screen.sgcraft.rings.power.energy",
                    "Energy: %s / %s GE    Transports left: %s",
                    StrataMenuScreen.value(numbers.format(power.energy()), SGStrata.WHITE),
                    numbers.format(power.capacity()),
                    StrataMenuScreen.value(numbers.format(power.uses()), SGStrata.YELLOW)));
            this.naquadah.setText(StrataMenuScreen.text(SGStrata.GRAY, "screen.sgcraft.rings.power.naquadah",
                    "Naquadah: %s", StrataMenuScreen.value(numbers.format(power.naquadah()), SGStrata.WHITE)));
        });
        this.unlink.setEnabled(!this.state.link().isEmpty());
        this.updating = true;
        try {
            this.everyoneGroup.selectItem(this.everyone);
        } finally {
            this.updating = false;
        }
        this.showHint();
        this.message.setText(Text.of(this.state.message()).color(SGStrata.YELLOW));
    }

    // What the default chosen means, under it, small, as 2.3's hint
    private void showHint() {
        if (this.hint != null) {
            this.hint.setText((this.everyone
                    ? SGStrata.text("screen.sgcraft.obelisk.everyone",
                            "Anyone may use it, save those listed as not allowed")
                    : SGStrata.text("screen.sgcraft.obelisk.listed",
                            "Only its owner and those listed as allowed may use it")).color(SGStrata.GRAY));
        }
    }

    private void send(final boolean unlink, final String addPlayer) {
        ClientPlatform.get().sendToServer(new ObeliskPayloads.Apply(this.state.obelisk(), this.everyone,
                List.copyOf(this.entries), addPlayer, unlink));
    }
}
