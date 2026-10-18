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

package com.almuradev.sgcraft.mod.client.malp;

import com.almuradev.sgcraft.mod.client.ClientPlatform;
import com.almuradev.sgcraft.mod.entity.MalpEntity;
import com.almuradev.sgcraft.mod.malp.MalpOptions;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.ConfigApply;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.ConfigState;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.ConfigUnpair;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * A MALP's settings, opened by using a configurator on it, laid out as the gate configurator lays out a gate's: two
 * columns of settings, each explained when the pointer rests on it, the MALP's name among them, with Save, Revert,
 * Unpair and Close along the bottom. Changes wait until Save, which sends every setting; the server answers with the
 * MALP's settings as they then are. Unpair unpairs the MALP from the console the screen shows it paired with, at
 * once, keeping unsaved changes.
 */
public final class MalpConfigScreen extends Screen {

    // Where the settings start, under the MALP's name, its owner and its console
    private static final int TOP = 64;
    // The width of each button along the bottom, and the gap between them
    private static final int BUTTON = 74;
    private static final int GAP = 4;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int FAINT = 0xFFA0A0A0;
    private static final int SAVED = 0xFF80FF80;

    private ConfigState state;
    // The settings being edited, and the name
    private MalpOptions options;
    private String name;
    // Whether the settings shown are the ones just saved
    private boolean saved;
    // Whether the next state answers Unpair, so the settings being edited stay
    private boolean unpairing;

    public MalpConfigScreen(final ConfigState state) {
        super(Component.translatableWithFallback("screen.sgcraft.malp_config", "MALP Configurator"));
        this.state = state;
        this.options = state.options();
        this.name = state.name();
        this.saved = state.saved();
    }

    /**
     * Shows the MALP's settings and console as the server now has them, dropping unsaved changes unless this answers
     * Unpair.
     */
    public void update(final ConfigState state) {
        this.state = state;
        if (this.unpairing) {
            this.unpairing = false;
        } else {
            this.options = state.options();
            this.name = state.name();
            this.saved = state.saved();
        }
        this.rebuildWidgets();
    }

    @Override
    protected void init() {
        final int left = this.width / 2 - 155;
        int i = 0;
        this.toggle(this.slot(left, i++), "power_required", "Requires power", "Whether the MALP runs on its "
                + "battery. Off, it uses no energy, and drives and sends its feed with a flat battery or none",
                this.options.powerRequired(), value -> this.options = this.options.withPowerRequired(value));
        final int[] at = this.slot(left, i++);
        final EditBox nameBox = this.addRenderableWidget(new EditBox(this.font, at[0], at[1], 150, 20,
                Component.translatableWithFallback("screen.sgcraft.malp_config.name", "Name")));
        nameBox.setMaxLength(MalpEntity.MAX_NAME);
        nameBox.setValue(this.name);
        nameBox.setHint(Component.translatableWithFallback("screen.sgcraft.malp_config.name", "Name"));
        nameBox.setTooltip(Tooltip.create(Component.translatableWithFallback("screen.sgcraft.malp_config.name.tip",
                "The MALP's name, as consoles, tablets and commands list it. Left blank, it takes its first name "
                        + "back")));
        nameBox.setResponder(value -> {
            if (!value.equals(this.name)) {
                this.name = value;
                this.saved = false;
            }
        });
        this.addRenderableWidget(Button.builder(Component.translatableWithFallback("screen.sgcraft.configurator.save",
                "Save"), b -> this.save()).bounds(this.bottom(0), this.height - 28, MalpConfigScreen.BUTTON, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "screen.sgcraft.configurator.revert", "Revert"), b -> this.revert())
                .bounds(this.bottom(1), this.height - 28, MalpConfigScreen.BUTTON, 20).build());
        final Button unpair = this.addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "screen.sgcraft.malp_config.unpair", "Unpair"), b -> this.unpair())
                .bounds(this.bottom(2), this.height - 28, MalpConfigScreen.BUTTON, 20).build());
        unpair.active = this.state.console().isPresent();
        unpair.setTooltip(Tooltip.create(Component.translatableWithFallback("screen.sgcraft.malp_config.unpair.tip",
                "Unpairs the MALP from its console now, without waiting for Save. Anyone driving it from there "
                        + "loses its feed")));
        this.addRenderableWidget(Button.builder(Component.translatableWithFallback("screen.sgcraft.close", "Close"),
                b -> this.onClose()).bounds(this.bottom(3), this.height - 28, MalpConfigScreen.BUTTON, 20).build());
    }

    // Where a button along the bottom starts, four buttons spanning the width of the two columns
    private int bottom(final int index) {
        return this.width / 2 - 154 + index * (MalpConfigScreen.BUTTON + MalpConfigScreen.GAP);
    }

    // Two columns of settings, each 150 wide
    private int[] slot(final int left, final int index) {
        return new int[] {left + (index % 2) * 160, MalpConfigScreen.TOP + (index / 2) * 24};
    }

    // An on and off switch, explained by a tip
    private void toggle(final int[] at, final String key, final String label, final String tip, final boolean value,
            final Consumer<Boolean> set) {
        final CycleButton<Boolean> button = this.addRenderableWidget(CycleButton.onOffBuilder(value).create(at[0],
                at[1], 150, 20, Component.translatableWithFallback("screen.sgcraft.malp_config." + key, label),
                (b, now) -> {
                    set.accept(now);
                    this.saved = false;
                }));
        button.setTooltip(Tooltip.create(Component.translatableWithFallback(
                "screen.sgcraft.malp_config." + key + ".tip", tip)));
    }

    private void save() {
        // An Unpair the server refused gets no answer, so the next one answers Save
        this.unpairing = false;
        ClientPlatform.get().sendToServer(new ConfigApply(this.state.malp(), false, this.options,
                Optional.of(this.name)));
    }

    private void unpair() {
        this.unpairing = true;
        ClientPlatform.get().sendToServer(new ConfigUnpair(this.state.malp()));
    }

    private void revert() {
        this.options = this.state.options();
        this.name = this.state.name();
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
        graphics.centeredText(this.font, Component.literal(this.title.getString() + ": " + this.state.name()),
                this.width / 2, 20, MalpConfigScreen.WHITE);
        graphics.centeredText(this.font, this.state.owner().map(owner -> Component.translatableWithFallback(
                "screen.sgcraft.malp_config.owner", "Owner: %s", owner))
                .orElseGet(() -> Component.translatableWithFallback("screen.sgcraft.malp_config.no_owner",
                        "Owner: none, so only operators configure it")), this.width / 2, 34, MalpConfigScreen.FAINT);
        graphics.centeredText(this.font, this.console(), this.width / 2, 48, MalpConfigScreen.FAINT);
        if (this.saved) {
            graphics.centeredText(this.font, Component.translatableWithFallback("screen.sgcraft.malp_config.saved",
                    "Changes saved"), this.width / 2, this.height - 44, MalpConfigScreen.SAVED);
        }
    }

    // The console the MALP is paired with, by position, and by world when it isn't the player's
    private Component console() {
        if (this.state.console().isEmpty()) {
            return Component.translatableWithFallback("screen.sgcraft.malp_config.unpaired", "Not paired");
        }
        final GlobalPos console = this.state.console().get();
        if (this.minecraft != null && this.minecraft.level != null
                && this.minecraft.level.dimension().equals(console.dimension())) {
            return Component.translatableWithFallback("screen.sgcraft.malp_config.console",
                    "Paired with the console at %s", console.pos().toShortString());
        }
        return Component.translatableWithFallback("screen.sgcraft.malp_config.console_elsewhere",
                "Paired with the console at %s in %s", console.pos().toShortString(),
                console.dimension().identifier().toString());
    }
}
