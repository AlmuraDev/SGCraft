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

package com.almuradev.sgcraft.mod.client.computer;

import com.almuradev.sgcraft.mod.client.ClientPlatform;
import com.almuradev.sgcraft.mod.computer.ComputerPayloads.ConfigApply;
import com.almuradev.sgcraft.mod.computer.ComputerPayloads.ConfigState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A computer's settings, a dialing computer's or a MALP console's, opened by using a configurator on it, laid out as
 * a MALP's are: its settings, each explained when the pointer rests on it, with Save, Revert and Close along the
 * bottom. Changes wait until Save; the server answers with the computer's settings as they then are.
 */
public final class ComputerConfigScreen extends Screen {

    // Where the settings start, under the computer's owner
    private static final int TOP = 64;
    // The width of each button along the bottom, and the gap between them
    private static final int BUTTON = 74;
    private static final int GAP = 4;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int FAINT = 0xFFA0A0A0;
    private static final int SAVED = 0xFF80FF80;

    private ConfigState state;
    // The setting being edited
    private boolean requiresPower;
    // Whether the settings shown are the ones just saved
    private boolean saved;

    public ComputerConfigScreen(final ConfigState state) {
        super(Component.translatableWithFallback("screen.sgcraft.computer_config", "Computer Configurator"));
        this.state = state;
        this.requiresPower = state.requiresPower();
        this.saved = state.saved();
    }

    /** Shows the computer's settings as the server now has them, dropping unsaved changes. */
    public void update(final ConfigState state) {
        this.state = state;
        this.requiresPower = state.requiresPower();
        this.saved = state.saved();
        this.rebuildWidgets();
    }

    @Override
    protected void init() {
        final CycleButton<Boolean> power = this.addRenderableWidget(CycleButton.onOffBuilder(this.requiresPower)
                .create(this.width / 2 - 155, ComputerConfigScreen.TOP, 150, 20, Component.translatableWithFallback(
                        "screen.sgcraft.computer_config.requires_power", "Requires power"), (b, now) -> {
                            this.requiresPower = now;
                            this.saved = false;
                        }));
        power.setTooltip(Tooltip.create(Component.translatableWithFallback(
                "screen.sgcraft.computer_config.requires_power.tip", "Whether the computer runs on gate energy from "
                        + "the power units and ZPMs near it. Off, it uses no energy and runs without them")));
        this.addRenderableWidget(Button.builder(Component.translatableWithFallback("screen.sgcraft.configurator.save",
                "Save"), b -> this.save()).bounds(this.bottom(0), this.height - 28, ComputerConfigScreen.BUTTON, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "screen.sgcraft.configurator.revert", "Revert"), b -> this.revert())
                .bounds(this.bottom(1), this.height - 28, ComputerConfigScreen.BUTTON, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatableWithFallback("screen.sgcraft.close", "Close"),
                b -> this.onClose()).bounds(this.bottom(2), this.height - 28, ComputerConfigScreen.BUTTON, 20)
                .build());
    }

    // Where a button along the bottom starts, three buttons centered
    private int bottom(final int index) {
        return this.width / 2 - (3 * ComputerConfigScreen.BUTTON + 2 * ComputerConfigScreen.GAP) / 2
                + index * (ComputerConfigScreen.BUTTON + ComputerConfigScreen.GAP);
    }

    private void save() {
        ClientPlatform.get().sendToServer(new ConfigApply(this.state.computer(), false, this.requiresPower));
    }

    private void revert() {
        this.requiresPower = this.state.requiresPower();
        this.saved = false;
        this.rebuildWidgets();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
            final float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 20, ComputerConfigScreen.WHITE);
        graphics.centeredText(this.font, this.state.owner().map(owner -> Component.translatableWithFallback(
                "screen.sgcraft.computer_config.owner", "Owner: %s", owner))
                .orElseGet(() -> Component.translatableWithFallback("screen.sgcraft.computer_config.no_owner",
                        "Owner: none, so only operators configure it")), this.width / 2, 34,
                ComputerConfigScreen.FAINT);
        // The server's own setting can make every computer run free, whatever this one's says
        if (!this.state.serverRequiresPower()) {
            graphics.centeredText(this.font, Component.translatableWithFallback(
                    "screen.sgcraft.computer_config.server_free", "The server runs every computer without power"),
                    this.width / 2, 48, ComputerConfigScreen.FAINT);
        }
        if (this.saved) {
            graphics.centeredText(this.font, Component.translatableWithFallback(
                    "screen.sgcraft.computer_config.saved", "Changes saved"), this.width / 2, this.height - 44,
                    ComputerConfigScreen.SAVED);
        }
    }
}
