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

package com.almuradev.sgcraft.mod.client.generator;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.generator.NaquadahGeneratorMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * A Naquadah Generator's screen, as 2.2.0's: whether it runs, the gate energy it holds and makes, how long its fuel
 * lasts, a button to switch it on or off, and its four fuel slots over the player's inventory.
 */
public final class GeneratorScreen extends AbstractContainerScreen<NaquadahGeneratorMenu> {

    private static final Identifier BACKGROUND = SGCraft.id("textures/gui/naquadah_generator.png");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 166;
    // The store's gauge, filling upwards, and where the readouts start
    private static final int GAUGE_X = 8;
    private static final int GAUGE_Y = 18;
    private static final int GAUGE_WIDTH = 10;
    private static final int GAUGE_HEIGHT = 44;
    private static final int TEXT_X = 70;
    private static final int TEXT_Y = 18;
    private static final int LINE = 10;

    private @Nullable Button toggle;

    public GeneratorScreen(final NaquadahGeneratorMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, GeneratorScreen.WIDTH, GeneratorScreen.HEIGHT);
        this.inventoryLabelY = NaquadahGeneratorMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        this.toggle = this.addRenderableWidget(Button.builder(Component.empty(), b -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId,
                        NaquadahGeneratorMenu.TOGGLE);
            }
        }).bounds(this.leftPos + 116, this.topPos + 57, 52, 14).build());
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
            final float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, GeneratorScreen.BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F,
                GeneratorScreen.WIDTH, GeneratorScreen.HEIGHT, 256, 256);
        final long capacity = Math.max(1, this.menu.capacity());
        final int filled = (int) Math.round(GeneratorScreen.GAUGE_HEIGHT * Math.min(1.0,
                (double) this.menu.stored() / capacity));
        final int x = this.leftPos + GeneratorScreen.GAUGE_X;
        final int bottom = this.topPos + GeneratorScreen.GAUGE_Y + GeneratorScreen.GAUGE_HEIGHT;
        graphics.fill(x, bottom - filled, x + GeneratorScreen.GAUGE_WIDTH, bottom, 0xFF4FA3E0);
        if (this.toggle != null) {
            this.toggle.setMessage(this.menu.on()
                    ? Component.translatableWithFallback("container.sgcraft.naquadah_generator.switch_off", "Off")
                    : Component.translatableWithFallback("container.sgcraft.naquadah_generator.switch_on", "On"));
        }
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int xm, final int ym) {
        super.extractLabels(graphics, xm, ym);
        final NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.ROOT);
        final boolean on = this.menu.on();
        final boolean running = on && (this.menu.stored() > 0 || this.menu.ticksLeft() > 0);
        final Component status = !on
                ? Component.translatableWithFallback("container.sgcraft.naquadah_generator.off", "Switched off")
                : running ? Component.translatableWithFallback("container.sgcraft.naquadah_generator.running", "Running")
                : Component.translatableWithFallback("container.sgcraft.naquadah_generator.no_fuel", "No fuel");
        int y = GeneratorScreen.TEXT_Y;
        graphics.text(this.font, status, GeneratorScreen.TEXT_X, y, running ? 0xFF207020 : 0xFF902020, false);
        y += GeneratorScreen.LINE;
        graphics.text(this.font, Component.translatableWithFallback("container.sgcraft.naquadah_generator.stored",
                "%s/%s GE", numbers.format(this.menu.stored()), numbers.format(this.menu.capacity())),
                GeneratorScreen.TEXT_X, y, 0xFF404040, false);
        y += GeneratorScreen.LINE;
        graphics.text(this.font, Component.translatableWithFallback("container.sgcraft.naquadah_generator.output",
                "Makes %s GE a tick", GeneratorScreen.decimal(this.menu.perTick())), GeneratorScreen.TEXT_X, y,
                0xFF404040, false);
        y += GeneratorScreen.LINE;
        final long seconds = this.menu.ticksLeft() / 20;
        graphics.text(this.font, Component.translatableWithFallback("container.sgcraft.naquadah_generator.lasts",
                "Fuel lasts %s", GeneratorScreen.duration(seconds)), GeneratorScreen.TEXT_X, y, 0xFF404040, false);
    }

    private static String decimal(final double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : String.format(Locale.ROOT, "%.2f", value);
    }

    // As hours, minutes and seconds, leaving out what is none
    private static String duration(final long seconds) {
        final long hours = seconds / 3600;
        final long minutes = seconds / 60 % 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes > 0 ? minutes + "m " + seconds % 60 + "s" : seconds + "s";
    }
}
