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

package com.almuradev.sgcraft.mod.client.dev;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.entity.MalpEntity;
import com.almuradev.sgcraft.mod.entity.SGEntityTypes;
import com.almuradev.sgcraft.mod.villager.SGVillagers;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Saves every item's icon as the game draws it in an inventory, for the wiki. It only runs when the client starts with
 * the system property {@value #PROPERTY} naming a folder: it then opens a flat creative world of its own, as items
 * only have their parts once a world is loaded, and draws the items a page at a time, first on black and then on
 * white. From the two pictures it works out each pixel's color and how see-through it is, so glass and glints come
 * out right. It saves one image per item, as
 * {@code <namespace>/<path>.png}, and every item's name in {@code names.json}. Then it draws the entities the wiki
 * shows, a Tok'ra and a MALP, the same way, saves them in {@code scenes}, and hands over to the {@link SceneExport}, which
 * pictures gates in the world and closes the game.
 *
 * <p>Items in an inventory are drawn at 16 pixels for each step of the GUI scale, so the client must run at GUI scale
 * {@value #GUI_SCALE}, in a window large enough for it, for the icons to be {@value #ICON} pixels across.</p>
 */
public final class IconExportScreen extends Screen {

    /** The system property naming the folder the icons are saved in. */
    public static final String PROPERTY = "sgcraft.exportIcons";

    private static final int GUI_SCALE = 4;
    private static final int ICON = 16 * IconExportScreen.GUI_SCALE;
    // Each item's cell on the screen, in GUI units: the item and a gap round it
    private static final int CELL = 18;
    // Frames drawn with a page before it is read back, so the picture read is of that page
    private static final int SETTLE_FRAMES = 3;
    // The box an entity is drawn in, in GUI units, and how many GUI units a block of it takes
    private static final int ENTITY_WIDTH = 96;
    private static final int ENTITY_HEIGHT = 128;
    private static final int ENTITY_SCALE = 54;
    // How far an entity is turned from facing the viewer, and how far the view looks down on it, in degrees
    private static final float ENTITY_TURN = 13.0F;
    private static final float ENTITY_TILT = 9.0F;
    // A MALP is low and long, so it is turned further, to show its side and wheels, and drawn smaller to fit
    private static final float MALP_TURN = 35.0F;
    private static final int MALP_SCALE = 38;
    // The world the export runs in, kept in the export's own game folder
    private static final String WORLD = "icon-export";

    // The folder icons are saved in, once an export has started, and whether the exporter has opened
    private static @Nullable Path folder;
    private static boolean opened;

    private final Path out;
    private final List<Item> items;
    // The entities drawn after the items, each with the name its picture is saved as
    private final List<Shot> shots = new ArrayList<>();
    private final JsonObject names = new JsonObject();
    private int columns;
    private int perPage;
    private int page;
    private boolean white;
    private int frames;
    private boolean reading;
    private @Nullable NativeImage onBlack;

    private IconExportScreen(final Path out) {
        super(Component.literal("Exporting icons"));
        this.out = out;
        this.items = BuiltInRegistries.ITEM.stream().filter(item -> item != Items.AIR).toList();
    }

    /**
     * Starts the export, if the client was started to export icons: items only have their parts once a world is
     * loaded, so this makes the exporter's own flat creative world, new each time. Call this from the title
     * screen, on the render thread.
     */
    public static void startIfRequested(final Minecraft minecraft, final Screen titleScreen) {
        final String folder = System.getProperty(IconExportScreen.PROPERTY);
        if (folder == null || IconExportScreen.folder != null) {
            return;
        }
        IconExportScreen.folder = Path.of(folder);
        // A new world each time, so the gates the scene export builds are the only ones in it
        if (minecraft.getLevelSource().levelExists(IconExportScreen.WORLD)) {
            try (Stream<Path> files = Files.walk(minecraft.getLevelSource().getLevelPath(IconExportScreen.WORLD))) {
                files.sorted(Comparator.reverseOrder()).forEach(file -> file.toFile().delete());
            } catch (final IOException e) {
                SGCraft.LOG.error("Couldn't delete the export's old world", e);
            }
        }
        minecraft.createWorldOpenFlows().createFreshLevel(IconExportScreen.WORLD, new LevelSettings(
                IconExportScreen.WORLD, GameType.CREATIVE, new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,
                false, false), true, WorldDataConfiguration.DEFAULT), WorldOptions.defaultWithRandomSeed(),
                lookup -> lookup.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value()
                        .createWorldDimensions(), titleScreen);
    }

    /** Opens the exporter once the player is in the export's world. Call this once a client tick. */
    public static void tickClient(final Minecraft minecraft) {
        if (IconExportScreen.folder != null && !IconExportScreen.opened && minecraft.player != null
                && minecraft.gui.screen() == null) {
            IconExportScreen.opened = true;
            minecraft.options.guiScale().set(IconExportScreen.GUI_SCALE);
            minecraft.resizeGui();
            minecraft.gui.setScreen(new IconExportScreen(IconExportScreen.folder));
        }
    }

    @Override
    protected void init() {
        this.columns = Math.max(1, this.width / IconExportScreen.CELL);
        this.perPage = this.columns * Math.max(1, this.height / IconExportScreen.CELL);
        if (this.minecraft.getWindow().getGuiScale() != IconExportScreen.GUI_SCALE) {
            SGCraft.LOG.error("Icon export needs GUI scale {}, but the window allows only {}; make the window larger",
                    IconExportScreen.GUI_SCALE, this.minecraft.getWindow().getGuiScale());
        }
        if (this.shots.isEmpty() && this.minecraft.level != null) {
            final Villager tokra = new Villager(EntityTypes.VILLAGER, this.minecraft.level);
            tokra.setVillagerData(tokra.getVillagerData().withProfession(this.minecraft.level.registryAccess(),
                    SGVillagers.TOKRA));
            // Drawing an entity reads its ID, which one never added to the world doesn't have
            tokra.setId(-1);
            this.shots.add(new Shot("tokra", tokra, IconExportScreen.ENTITY_SCALE));
            final MalpEntity malp = new MalpEntity(SGEntityTypes.MALP, this.minecraft.level);
            // Its renderer turns it half round, so this faces its front, headlights and claw, to the viewer
            malp.setYRot(180.0F - IconExportScreen.MALP_TURN);
            malp.yRotO = malp.getYRot();
            malp.setId(-2);
            this.shots.add(new Shot("malp", malp, IconExportScreen.MALP_SCALE));
        }
    }

    // How many pages of items there are; the entities follow, one a page
    private int itemPages() {
        return (this.items.size() + this.perPage - 1) / this.perPage;
    }

    private @Nullable Shot shot() {
        final int index = this.page - this.itemPages();
        return index >= 0 && index < this.shots.size() ? this.shots.get(index) : null;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
            final float partialTick) {
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
            final float partialTick) {
        this.frames++;
        graphics.fill(0, 0, this.width, this.height, this.white ? 0xFFFFFFFF : 0xFF000000);
        final Shot shot = this.shot();
        if (shot != null) {
            IconExportScreen.extractEntity(graphics, shot);
            return;
        }
        final int first = this.page * this.perPage;
        for (int i = first; i < Math.min(first + this.perPage, this.items.size()); i++) {
            final int cell = i - first;
            graphics.fakeItem(new ItemStack(this.items.get(i)), cell % this.columns * IconExportScreen.CELL + 1,
                    cell / this.columns * IconExportScreen.CELL + 1);
        }
    }

    // Draws an entity in the export's box as an inventory draws a player, seen a little from above. A living entity
    // is turned a little to the viewer's left, looking just above them; any other keeps the turn it was given
    private static void extractEntity(final GuiGraphicsExtractor graphics, final Shot shot) {
        final Entity entity = shot.entity();
        final Minecraft minecraft = Minecraft.getInstance();
        final EntityRenderState state = minecraft.getEntityRenderDispatcher().getRenderer(entity)
                .createRenderState(entity, 1.0F);
        state.shadowPieces.clear();
        state.outlineColor = 0;
        if (state instanceof final LivingEntityRenderState living) {
            living.bodyRot = 180.0F + IconExportScreen.ENTITY_TURN;
            living.yRot = IconExportScreen.ENTITY_TURN;
            living.xRot = -IconExportScreen.ENTITY_TILT;
            living.boundingBoxWidth = living.boundingBoxWidth / living.scale;
            living.boundingBoxHeight = living.boundingBoxHeight / living.scale;
            living.scale = 1.0F;
        }
        final Quaternionf tilt = new Quaternionf().rotateX((float) Math.toRadians(IconExportScreen.ENTITY_TILT));
        final Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI).mul(tilt);
        final Vector3f translation = new Vector3f(0.0F, state.boundingBoxHeight / 2.0F + 0.0625F, 0.0F);
        graphics.entity(state, shot.scale(), translation, rotation, tilt, 0, 0,
                IconExportScreen.ENTITY_WIDTH, IconExportScreen.ENTITY_HEIGHT);
    }

    // Reads back the last frame drawn once the page has settled, at the start of a tick, before the next is drawn
    @Override
    public void tick() {
        if (this.reading || this.frames < IconExportScreen.SETTLE_FRAMES) {
            return;
        }
        this.reading = true;
        Screenshot.takeScreenshot(this.minecraft.gameRenderer.mainRenderTarget(), image -> this.minecraft.execute(() ->
                this.read(image)));
    }

    private void read(final NativeImage image) {
        if (!this.white) {
            this.onBlack = image;
            this.white = true;
        } else {
            final NativeImage black = this.onBlack;
            this.onBlack = null;
            try {
                if (black != null) {
                    this.save(black, image);
                }
            } finally {
                image.close();
                if (black != null) {
                    black.close();
                }
            }
            this.white = false;
            this.page++;
            if (this.page >= this.itemPages() + this.shots.size()) {
                this.finish();
                return;
            }
        }
        this.frames = 0;
        this.reading = false;
    }

    // Saves the icons of the page just drawn on black and on white, or the entity drawn on it
    private void save(final NativeImage black, final NativeImage white) {
        final Shot shot = this.shot();
        if (shot != null) {
            final int width = IconExportScreen.ENTITY_WIDTH * IconExportScreen.GUI_SCALE;
            final int height = IconExportScreen.ENTITY_HEIGHT * IconExportScreen.GUI_SCALE;
            try (NativeImage picture = new NativeImage(width, height, true)) {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        picture.setPixel(x, y, IconExportScreen.unmix(black.getPixel(x, y), white.getPixel(x, y)));
                    }
                }
                final Path file = this.out.resolve("scenes").resolve(shot.name() + ".png");
                Files.createDirectories(file.getParent());
                picture.writeToFile(file);
            } catch (final IOException e) {
                SGCraft.LOG.error("Couldn't save the picture of {}", shot.name(), e);
            }
            return;
        }
        final int first = this.page * this.perPage;
        for (int i = first; i < Math.min(first + this.perPage, this.items.size()); i++) {
            final int cell = i - first;
            final int left = (cell % this.columns * IconExportScreen.CELL + 1) * IconExportScreen.GUI_SCALE;
            final int top = (cell / this.columns * IconExportScreen.CELL + 1) * IconExportScreen.GUI_SCALE;
            final Item item = this.items.get(i);
            final Identifier id = BuiltInRegistries.ITEM.getKey(item);
            this.names.addProperty(id.toString(), new ItemStack(item).getHoverName().getString());
            try (NativeImage icon = new NativeImage(IconExportScreen.ICON, IconExportScreen.ICON, true)) {
                for (int y = 0; y < IconExportScreen.ICON; y++) {
                    for (int x = 0; x < IconExportScreen.ICON; x++) {
                        icon.setPixel(x, y, IconExportScreen.unmix(black.getPixel(left + x, top + y),
                                white.getPixel(left + x, top + y)));
                    }
                }
                final Path file = this.out.resolve(id.getNamespace()).resolve(id.getPath() + ".png");
                Files.createDirectories(file.getParent());
                icon.writeToFile(file);
            } catch (final IOException e) {
                SGCraft.LOG.error("Couldn't save the icon of {}", id, e);
            }
        }
    }

    // A pixel drawn over black shows its color times its opacity; over white it shows that plus what it lets through
    // of the white. So the difference between the two is how see-through it is, and the color follows from that
    private static int unmix(final int onBlack, final int onWhite) {
        final int through = (ARGB.red(onWhite) - ARGB.red(onBlack) + ARGB.green(onWhite) - ARGB.green(onBlack)
                + ARGB.blue(onWhite) - ARGB.blue(onBlack)) / 3;
        final int alpha = Math.clamp(255 - through, 0, 255);
        if (alpha == 0) {
            return 0;
        }
        return ARGB.color(alpha, IconExportScreen.undo(ARGB.red(onBlack), alpha),
                IconExportScreen.undo(ARGB.green(onBlack), alpha), IconExportScreen.undo(ARGB.blue(onBlack), alpha));
    }

    private static int undo(final int channel, final int alpha) {
        return Math.min(255, channel * 255 / alpha);
    }

    private void finish() {
        try {
            Files.createDirectories(this.out);
            Files.writeString(this.out.resolve("names.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(this.names), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            SGCraft.LOG.error("Couldn't save the item names", e);
        }
        SGCraft.LOG.info("Saved the icons of {} items to {}", this.items.size(), this.out);
        SceneExport.start(this.minecraft, this.out.resolve("scenes"));
    }

    // An entity the wiki shows, the name its picture is saved as, and how many GUI units a block of it takes
    private record Shot(String name, Entity entity, int scale) {
    }
}
