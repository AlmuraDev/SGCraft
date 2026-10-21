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

package com.almuradev.sgcraft.mod.client.obelisk;

import com.almuradev.sgcraft.mod.client.ClientPlatform;
import com.almuradev.sgcraft.mod.obelisk.ObeliskPayloads;
import com.almuradev.sgcraft.mod.obelisk.ObeliskPayloads.Entry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The screen of an Asgard obelisk or a ring platform, opened with the configurator, as 2.2.0's: what it is linked to,
 * with a button to unlink it, its owner, a ring platform's power, whether everyone may use it, and the players on its
 * list, each allowed or not and each able to configure it or not. Players are added by name while they are online.
 */
public final class ObeliskScreen extends Screen {

    private static final int WIDTH = 300;
    private static final int ROWS = 5;
    private static final int ROW = 22;
    // How much lower everything under the owner sits when there is a line of power to show
    private static final int POWER_LINE = 12;

    private ObeliskPayloads.State state;
    private boolean everyone;
    private final List<Entry> entries = new ArrayList<>();
    private int scroll;
    private EditBox name;

    public ObeliskScreen(final ObeliskPayloads.State state) {
        super(state.power().isPresent()
                ? Component.translatableWithFallback("screen.sgcraft.rings", "Transport Rings")
                : Component.translatableWithFallback("screen.sgcraft.obelisk", "Asgard Obelisk"));
        this.update(state);
    }

    public void update(final ObeliskPayloads.State state) {
        this.state = state;
        this.everyone = state.everyone();
        this.entries.clear();
        this.entries.addAll(state.entries());
        if (this.minecraft != null) {
            this.rebuildWidgets();
        }
    }

    @Override
    protected void init() {
        final int left = this.width / 2 - ObeliskScreen.WIDTH / 2;
        int y = this.height / 2 - 90;
        this.addRenderableWidget(Button.builder(Component.translatableWithFallback("screen.sgcraft.obelisk.unlink",
                "Unlink"), b -> this.send(true, "")).bounds(left + ObeliskScreen.WIDTH - 70, y, 70, 20).build())
                .active = !this.state.link().isEmpty();
        y += 26 + this.powerLine();
        this.addRenderableWidget(Button.builder(this.everyoneLabel(), b -> {
            this.everyone = !this.everyone;
            b.setMessage(this.everyoneLabel());
        }).bounds(left, y, ObeliskScreen.WIDTH, 20).build());
        y += 26;
        final int rows = Math.min(ObeliskScreen.ROWS, Math.max(0, this.entries.size() - this.scroll));
        for (int row = 0; row < rows; row++) {
            final int index = this.scroll + row;
            final Entry entry = this.entries.get(index);
            final int at = y + row * ObeliskScreen.ROW;
            this.addRenderableWidget(Button.builder(ObeliskScreen.onOff("Use", entry.use()), b -> {
                final Entry now = this.entries.get(index);
                this.entries.set(index, new Entry(now.player(), now.name(), !now.use(), now.admin()));
                b.setMessage(ObeliskScreen.onOff("Use", !now.use()));
            }).bounds(left + 130, at, 70, 20).build());
            this.addRenderableWidget(Button.builder(ObeliskScreen.onOff("Admin", entry.admin()), b -> {
                final Entry now = this.entries.get(index);
                this.entries.set(index, new Entry(now.player(), now.name(), now.use(), !now.admin()));
                b.setMessage(ObeliskScreen.onOff("Admin", !now.admin()));
            }).bounds(left + 204, at, 70, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("x"), b -> {
                this.entries.remove(index);
                this.scroll = Math.min(this.scroll, Math.max(0, this.entries.size() - ObeliskScreen.ROWS));
                this.rebuildWidgets();
            }).bounds(left + 278, at, 22, 20).build());
        }
        final int pages = y + ObeliskScreen.ROWS * ObeliskScreen.ROW;
        this.addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            this.scroll = Math.max(0, this.scroll - ObeliskScreen.ROWS);
            this.rebuildWidgets();
        }).bounds(left, pages, 20, 20).build()).active = this.scroll > 0;
        this.addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            this.scroll += ObeliskScreen.ROWS;
            this.rebuildWidgets();
        }).bounds(left + 24, pages, 20, 20).build()).active = this.scroll + ObeliskScreen.ROWS < this.entries.size();
        this.name = this.addRenderableWidget(new EditBox(this.font, left + 50, pages, 150, 20,
                Component.translatableWithFallback("screen.sgcraft.obelisk.player_name", "Player name")));
        this.name.setMaxLength(16);
        this.name.setHint(Component.translatableWithFallback("screen.sgcraft.obelisk.player_name", "Player name"));
        this.addRenderableWidget(Button.builder(Component.translatableWithFallback("screen.sgcraft.obelisk.add",
                "Add player"), b -> this.send(false, this.name.getValue())).bounds(left + 204, pages, 96, 20).build());
        final int bottom = pages + 30;
        this.addRenderableWidget(Button.builder(Component.translatableWithFallback("screen.sgcraft.obelisk.save",
                "Save"), b -> this.send(false, "")).bounds(left, bottom, 146, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatableWithFallback("screen.sgcraft.close", "Close"),
                b -> this.onClose()).bounds(left + 154, bottom, 146, 20).build());
    }

    private int powerLine() {
        return this.state.power().isPresent() ? ObeliskScreen.POWER_LINE : 0;
    }

    private Component everyoneLabel() {
        return this.everyone
                ? Component.translatableWithFallback("screen.sgcraft.obelisk.everyone",
                        "Anyone may use it, save those listed as not allowed")
                : Component.translatableWithFallback("screen.sgcraft.obelisk.listed",
                        "Only its owner and those listed as allowed may use it");
    }

    private static Component onOff(final String what, final boolean on) {
        return Component.literal(what + ": " + (on ? "yes" : "no"));
    }

    private void send(final boolean unlink, final String addPlayer) {
        ClientPlatform.get().sendToServer(new ObeliskPayloads.Apply(this.state.obelisk(), this.everyone,
                List.copyOf(this.entries), addPlayer, unlink));
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
            final float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        final int left = this.width / 2 - ObeliskScreen.WIDTH / 2;
        int y = this.height / 2 - 90;
        graphics.centeredText(this.font, this.title, this.width / 2, y - 16, 0xFFFFFFFF);
        final Component link = this.state.link().isEmpty()
                ? Component.translatableWithFallback("screen.sgcraft.obelisk.not_linked", "Not linked")
                : Component.translatableWithFallback("screen.sgcraft.obelisk.linked", "Linked to %s",
                        this.state.link());
        graphics.text(this.font, link, left, y + 1, 0xFFFFFFFF);
        graphics.text(this.font, this.state.owner().isEmpty()
                ? Component.translatableWithFallback("screen.sgcraft.obelisk.no_owner", "Owner: none")
                : Component.translatableWithFallback("screen.sgcraft.obelisk.owner", "Owner: %s", this.state.owner()),
                left, y + 11, 0xFFAAAAAA);
        this.state.power().ifPresent(power -> {
            final NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.ROOT);
            graphics.text(this.font, Component.translatableWithFallback("screen.sgcraft.rings.power",
                    "Energy: %s/%s GE, naquadah: %s, transports left: %s", numbers.format(power.energy()),
                    numbers.format(power.capacity()), power.naquadah(), numbers.format(power.uses())), left,
                    this.height / 2 - 90 + 21, 0xFFAAAAAA);
        });
        y += 52 + this.powerLine();
        final int rows = Math.min(ObeliskScreen.ROWS, Math.max(0, this.entries.size() - this.scroll));
        for (int row = 0; row < rows; row++) {
            graphics.text(this.font, this.entries.get(this.scroll + row).name(), left, y + row * ObeliskScreen.ROW + 6,
                    0xFFFFFFFF);
        }
        if (this.entries.isEmpty()) {
            graphics.text(this.font, Component.translatableWithFallback("screen.sgcraft.obelisk.nobody",
                    "No players are listed"), left, y + 6, 0xFFAAAAAA);
        }
        if (!this.state.message().isEmpty()) {
            graphics.centeredText(this.font, this.state.message(), this.width / 2, this.height - 16, 0xFFFFD060);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
