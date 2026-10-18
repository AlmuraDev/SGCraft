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

package com.almuradev.sgcraft.mod.client.settings;

import com.almuradev.sgcraft.api.address.Symbol;
import com.almuradev.sgcraft.mod.address.AddressChanges.Kind;
import com.almuradev.sgcraft.mod.address.AddressSettings;
import com.almuradev.sgcraft.mod.client.ClientPlatform;
import com.almuradev.sgcraft.mod.client.SGClientConfig;
import com.almuradev.sgcraft.mod.client.settings.SymbolPickerScreen.Extra;
import com.almuradev.sgcraft.mod.settings.SettingsCatalog;
import com.almuradev.sgcraft.mod.settings.SettingsPayloads.Change;
import com.almuradev.sgcraft.mod.settings.SettingsPayloads.Confirm;
import com.almuradev.sgcraft.mod.settings.SettingsPayloads.Edits;
import com.almuradev.sgcraft.mod.settings.SettingsPayloads.Group;
import com.almuradev.sgcraft.mod.settings.SettingsPayloads.Option;
import com.almuradev.sgcraft.mod.settings.SettingsPayloads.Request;
import com.almuradev.sgcraft.mod.settings.SettingsPayloads.SetOption;
import com.almuradev.sgcraft.mod.settings.SettingsPayloads.State;
import com.almuradev.sgcraft.mod.settings.SettingsPayloads.World;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * SGCraft's settings. Operators see each world's point of origin, cell size and group, the groups themselves, and a
 * page for each part of the server's other settings: gates, energy, shields, MALPs and worldgen. Everyone sees their
 * own client settings, even without a world open. It opens from the mod list's Config button, from Mod Menu on Fabric,
 * or with {@code /sgcraft settings screen}.
 *
 * <p>The screen holds its changes until Save is pressed, showing what they would do as they are made, and Discard
 * drops them; leaving with changes not saved asks first. Saving sends them to the server, which writes them to its
 * settings file and shows them to every other operator with the screen open. The address settings take effect when
 * the server next starts, and saving changes to them that give gates new addresses asks first, saying which worlds'
 * gates they reach. Most other settings apply as they are saved; those marked with a star wait for a restart.</p>
 */
public final class SettingsScreen extends Screen {

    private static final int WIDTH = 320;
    private static final int TOP = 44;
    private static final int ROW = 24;
    // Space under the list for two rows of controls and three lines of messages, above the Done button
    private static final int BOTTOM = 118;
    // Space under a page of settings for three lines of messages, above the Done button
    private static final int OPTIONS_BOTTOM = 66;

    private static final String WORLDS = "worlds";
    private static final String GROUPS = "groups";
    private static final String CLIENT = "client";

    private final @Nullable Screen parent;
    private final TabManager tabs = new TabManager(this::addRenderableWidget, this::removeWidget, this::choose,
            tab -> { });
    private @Nullable MenuTabBar tabBar;
    private @Nullable State state;
    private String page = SettingsScreen.WORLDS;
    private @Nullable Rows list;
    private String selectedWorld = "";
    private String selectedGroup = "";
    private String newGroup = "";
    private List<Component> messages = List.of();
    private boolean relayout;
    // Numbers typed on the pages of settings but not sent yet, by setting, kept while the screen is laid out again
    private final Map<String, String> drafts = new HashMap<>();
    private boolean rebuilding;
    private @Nullable OptionList options;
    // How far the list was scrolled, and on which page, to scroll it back when the page is laid out again
    private double scroll;
    private String scrolledPage = "";
    // The changes held until they are saved: to the addresses, to the server's other settings by key, and to this
    // player's own settings, and the count of the edits sent to be previewed
    private final List<Change> changes = new ArrayList<>();
    private final List<SetOption> pendingOptions = new ArrayList<>();
    private @Nullable SGClientConfig pendingClient;
    private int sequence;

    /** A screen that asks the server for its settings, from the mod list or Mod Menu. */
    public SettingsScreen(final @Nullable Screen parent) {
        super(Component.translatableWithFallback("screen.sgcraft.settings", "SGCraft settings"));
        this.parent = parent;
        if (Minecraft.getInstance().getConnection() != null) {
            ClientPlatform.get().sendToServer(new Request());
        }
    }

    /** A screen opened by the server, with its settings. */
    public SettingsScreen(final State state) {
        super(Component.translatableWithFallback("screen.sgcraft.settings", "SGCraft settings"));
        this.parent = null;
        this.load(state);
    }

    /**
     * Shows the settings as the server has them, with this screen's changes made if it holds any. An answer to edits
     * sent before the last is passed over. A change the server refused is dropped, and one another operator saved
     * has this screen's own changes previewed again on top of it. Settings with no messages leave this screen's
     * messages as they are.
     */
    public void update(final State state) {
        if (state.refresh() && this.unsaved()) {
            this.preview();
            return;
        }
        if (state.reply() != 0 && state.reply() != this.sequence) {
            return;
        }
        if (state.saved()) {
            this.changes.clear();
            this.pendingOptions.clear();
            this.drafts.clear();
        }
        final boolean dropped = state.refused() >= 0 && state.refused() < this.changes.size()
                || state.refusedOption() >= 0 && state.refusedOption() < this.pendingOptions.size();
        if (state.refused() >= 0 && state.refused() < this.changes.size()) {
            this.changes.remove(state.refused());
        } else if (state.refusedOption() >= 0 && state.refusedOption() < this.pendingOptions.size()) {
            this.pendingOptions.remove(state.refusedOption());
        }
        final List<Component> kept = this.messages;
        this.load(state);
        if (state.messages().isEmpty()) {
            this.messages = kept;
        }
        this.rebuildWidgets();
        if (dropped && this.unsaved()) {
            // What was refused is dropped, and the changes after it are previewed again without it
            this.preview();
        }
    }

    // Whether the screen holds changes the server hasn't saved
    private boolean serverUnsaved() {
        return !this.changes.isEmpty() || !this.pendingOptions.isEmpty();
    }

    // Whether the screen holds any changes not saved
    private boolean unsaved() {
        return this.serverUnsaved() || this.pendingClient != null && !this.pendingClient.equals(SGClientConfig.get());
    }

    // Sends the changes held to be previewed
    private void preview() {
        ClientPlatform.get().sendToServer(new Edits(++this.sequence, List.copyOf(this.changes),
                List.copyOf(this.pendingOptions), false, false));
    }

    // Saves the changes held: this player's own at once, and the server's by sending them to be saved
    private void save() {
        if (this.pendingClient != null && !this.pendingClient.equals(SGClientConfig.get())) {
            this.messages = List.of(SGClientConfig.set(this.pendingClient)
                    ? Component.translatableWithFallback("screen.sgcraft.settings.client.saved", "Your settings are "
                            + "saved")
                    : Component.translatableWithFallback("screen.sgcraft.settings.client.not_saved", "Your settings "
                            + "file couldn't be written, so the change lasts only until the game closes. The log says "
                            + "why"));
        }
        this.pendingClient = null;
        if (this.serverUnsaved()) {
            ClientPlatform.get().sendToServer(new Edits(++this.sequence, List.copyOf(this.changes),
                    List.copyOf(this.pendingOptions), true, false));
        }
        this.rebuildWidgets();
    }

    // Drops the changes held, and shows the settings as saved again
    private void discard() {
        final boolean server = this.serverUnsaved();
        this.changes.clear();
        this.pendingOptions.clear();
        this.pendingClient = null;
        this.drafts.clear();
        this.messages = List.of();
        if (server) {
            this.preview();
        }
        this.rebuildWidgets();
    }

    // Holds a change to one of the server's other settings, in place of one held for it already
    private void setOption(final String key, final String value) {
        this.pendingOptions.removeIf(option -> option.key().equals(key));
        this.pendingOptions.add(new SetOption(key, value));
        this.preview();
    }

    // Lays the screen out again where it was: the list scrolled as it was, and numbers being typed kept
    @Override
    protected void rebuildWidgets() {
        if (this.options != null) {
            this.scroll = this.options.scrollAmount();
        } else if (this.list != null) {
            this.scroll = this.list.scrollAmount();
        }
        this.scrolledPage = this.page;
        this.rebuilding = true;
        try {
            super.rebuildWidgets();
        } finally {
            this.rebuilding = false;
        }
    }

    private void load(final State state) {
        this.state = state;
        this.messages = state.messages().stream().map(SettingsScreen::component).toList();
    }

    private boolean allowed() {
        return this.state != null && this.state.allowed();
    }

    // A row chosen in the list lays the page out again on the next tick, once the click is handled
    @Override
    public void tick() {
        if (this.relayout) {
            this.relayout = false;
            this.rebuildWidgets();
        }
    }

    /** Asks whether to save changes that give gates new addresses. */
    public void confirm(final Confirm confirm) {
        final MutableComponent warnings = Component.empty();
        for (final String warning : confirm.warnings()) {
            if (!warnings.getSiblings().isEmpty()) {
                warnings.append("\n\n");
            }
            warnings.append(SettingsScreen.component(warning));
        }
        this.minecraft.gui.setScreen(new ConfirmScreen(yes -> {
            this.minecraft.gui.setScreen(this);
            if (yes) {
                ClientPlatform.get().sendToServer(confirm.edits().confirm());
            } else {
                this.rebuildWidgets();
            }
        }, Component.translatableWithFallback("screen.sgcraft.settings.confirm", "Give these gates new addresses?"),
                warnings, Component.translatableWithFallback("screen.sgcraft.settings.confirm.save", "Save"),
                CommonComponents.GUI_CANCEL));
    }

    private static Component component(final String json) {
        try {
            return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).result()
                    .orElse(Component.literal(json));
        } catch (final RuntimeException e) {
            return Component.literal(json);
        }
    }

    // The pages: the address pages and the server's own for an operator, then the client's
    private Map<String, Component> pages() {
        final Map<String, Component> pages = new LinkedHashMap<>();
        if (this.allowed()) {
            pages.put(SettingsScreen.WORLDS, Component.translatableWithFallback("screen.sgcraft.settings.worlds",
                    "Worlds"));
            pages.put(SettingsScreen.GROUPS, Component.translatableWithFallback("screen.sgcraft.settings.groups",
                    "Groups"));
            for (final Option option : this.state.options()) {
                pages.computeIfAbsent(option.page(), id -> SettingsScreen.component(option.pageTitle()));
            }
        }
        pages.put(SettingsScreen.CLIENT, Component.translatableWithFallback("screen.sgcraft.settings.client",
                "Client"));
        return pages;
    }

    @Override
    protected void init() {
        final Map<String, Component> pages = this.pages();
        if (!pages.containsKey(this.page)) {
            this.page = pages.keySet().iterator().next();
        }
        final MenuTabBar.Builder bar = MenuTabBar.builder(this.tabs, this.width);
        pages.forEach((id, title) -> bar.addTab(new PageTab(id, title)));
        this.tabBar = this.addRenderableWidget(bar.build());
        this.tabBar.selectTab(new ArrayList<>(pages.keySet()).indexOf(this.page), false);
        this.tabBar.arrangeElements(this.width);
        this.list = null;
        this.options = null;
        final int left = (this.width - SettingsScreen.WIDTH) / 2;
        if (this.page.equals(SettingsScreen.WORLDS) || this.page.equals(SettingsScreen.GROUPS)) {
            final int bottom = this.height - SettingsScreen.BOTTOM;
            this.list = this.addRenderableWidget(new Rows(this.minecraft, SettingsScreen.WIDTH,
                    bottom - SettingsScreen.TOP, SettingsScreen.TOP));
            this.list.setX(left);
            if (this.page.equals(SettingsScreen.WORLDS)) {
                this.worldsPage(left, bottom + 6);
            } else {
                this.groupsPage(left, bottom + 6);
            }
            if (this.page.equals(this.scrolledPage)) {
                this.list.setScrollAmount(this.scroll);
            }
        } else {
            this.options = this.addRenderableWidget(new OptionList(this.minecraft, this.width,
                    this.height - SettingsScreen.OPTIONS_BOTTOM - SettingsScreen.TOP, SettingsScreen.TOP,
                    this.drafts, () -> this.rebuilding));
            this.options.addAll(this.page.equals(SettingsScreen.CLIENT) ? this.clientItems() : this.serverItems());
            if (this.page.equals(this.scrolledPage)) {
                this.options.setScrollAmount(this.scroll);
            }
        }
        final boolean unsaved = this.unsaved();
        final int buttons = this.width / 2 - 152;
        final Button save = this.addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "screen.sgcraft.settings.save", "Save"), b -> this.save())
                .bounds(buttons, this.height - 26, 98, 20).build());
        save.active = unsaved;
        save.setTooltip(Tooltip.create(Component.translatableWithFallback("screen.sgcraft.settings.save.tip",
                "Saves the changes made on this screen, and shows them to every other operator with it open")));
        final Button discard = this.addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "screen.sgcraft.settings.discard", "Discard"), b -> this.discard())
                .bounds(buttons + 102, this.height - 26, 98, 20).build());
        discard.active = unsaved;
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
                .bounds(buttons + 204, this.height - 26, 98, 20).build());
    }

    // A page's tab. The page lays out its own widgets, so the tab holds none.
    private static final class PageTab extends GridLayoutTab {

        private final String page;

        PageTab(final String page, final Component title) {
            super(title);
            this.page = page;
        }
    }

    private void choose(final @Nullable Tab tab) {
        if (tab instanceof final PageTab chosen && !chosen.page.equals(this.page)) {
            this.page = chosen.page;
            this.messages = List.of();
            this.rebuildWidgets();
        }
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        return this.tabBar != null && this.tabBar.keyPressed(event) || super.keyPressed(event);
    }

    // The server's settings on this page, each held as it changes, until it is saved
    private List<OptionList.Item> serverItems() {
        final List<OptionList.Item> items = new ArrayList<>();
        for (final Option option : this.state.options()) {
            if (option.page().equals(this.page)) {
                items.add(new OptionList.Item(option.key(), SettingsScreen.component(option.section()),
                        SettingsScreen.component(option.label()), SettingsScreen.component(option.tip()),
                        option.type(), option.live(), option.now(), option.next(), option.defaultValue(),
                        this.pendingOptions.stream().anyMatch(held -> held.key().equals(option.key())),
                        value -> this.setOption(option.key(), value)));
            }
        }
        return items;
    }

    // This player's own settings, held as they change, until they are saved to their settings file
    private List<OptionList.Item> clientItems() {
        final SGClientConfig saved = SGClientConfig.get();
        final SGClientConfig config = this.pendingClient == null ? saved : this.pendingClient;
        final SGClientConfig defaults = SGClientConfig.DEFAULTS;
        final Component section = Component.translatableWithFallback("screen.sgcraft.settings.client.section",
                "Your own settings. They change nothing for other players");
        final Component hdName = Component.translatableWithFallback("screen.sgcraft.settings.client.hd_event_horizon",
                "Detailed event horizon");
        final Component copyName = Component.translatableWithFallback(
                "screen.sgcraft.settings.client.copy_address_on_open", "Copy address on open");
        final Component volumeName = Component.translatableWithFallback(
                "screen.sgcraft.settings.client.sound_volume", "Sound volume");
        return List.of(
                SettingsScreen.clientItem(section, hdName, Component.translatableWithFallback(
                        "screen.sgcraft.settings.client.hd_event_horizon.tip", "Whether the event horizon is drawn "
                                + "with its detailed texture, rather than the original's plain one"),
                        SettingsCatalog.Type.BOOL, config.hdEventHorizon(), defaults.hdEventHorizon(),
                        config.hdEventHorizon() != saved.hdEventHorizon(), value ->
                                this.setClient(hdName, value, text -> new SGClientConfig(Boolean.parseBoolean(text),
                                        config.copyAddressOnOpen(), config.soundVolume()))),
                SettingsScreen.clientItem(section, copyName, Component.translatableWithFallback(
                        "screen.sgcraft.settings.client.copy_address_on_open.tip", "Whether opening a gate's base "
                                + "screen copies the gate's address to the clipboard"),
                        SettingsCatalog.Type.BOOL, config.copyAddressOnOpen(), defaults.copyAddressOnOpen(),
                        config.copyAddressOnOpen() != saved.copyAddressOnOpen(), value ->
                                this.setClient(copyName, value, text -> new SGClientConfig(config.hdEventHorizon(),
                                        Boolean.parseBoolean(text), config.soundVolume()))),
                SettingsScreen.clientItem(section, volumeName, Component.translatableWithFallback(
                        "screen.sgcraft.settings.client.sound_volume.tip", "How loud SGCraft's sounds play, from 0 "
                                + "for silent to 1 for full volume, on top of the game's own volume settings"),
                        SettingsCatalog.Type.DECIMAL, config.soundVolume(), defaults.soundVolume(),
                        config.soundVolume() != saved.soundVolume(), value ->
                                this.setClient(volumeName, value, text -> {
                                    final double volume = Double.parseDouble(text);
                                    if (!(volume >= 0 && volume <= 1)) {
                                        throw new NumberFormatException(text);
                                    }
                                    return new SGClientConfig(config.hdEventHorizon(), config.copyAddressOnOpen(),
                                            volume);
                                })));
    }

    private static OptionList.Item clientItem(final Component section, final Component label, final Component tip,
            final SettingsCatalog.Type type, final Object now, final Object defaultValue, final boolean unsaved,
            final Consumer<String> change) {
        final String value = SettingsCatalog.format(type, now);
        return new OptionList.Item("client." + label.getString(), section, label, tip, type, true, value, value,
                SettingsCatalog.format(type, defaultValue), unsaved, change);
    }

    private void setClient(final Component name, final String value, final Function<String, SGClientConfig> with) {
        final SGClientConfig config;
        try {
            config = with.apply(value.trim());
        } catch (final NumberFormatException e) {
            this.messages = List.of(Component.translatableWithFallback("screen.sgcraft.settings.client.bad_volume",
                    "%s is a number from 0 to 1", name));
            this.relayout = true;
            return;
        }
        this.pendingClient = config;
        this.relayout = true;
    }

    private void worldsPage(final int left, final int top) {
        final State state = this.state;
        final Rows rows = this.list;
        for (final World world : state.worlds()) {
            rows.add(new WorldRow(world), world.id().equals(this.selectedWorld));
        }
        final Optional<World> selected = state.worlds().stream().filter(w -> w.id().equals(this.selectedWorld))
                .findFirst();
        final Button origin = this.addRenderableWidget(Button.builder(selected.map(world -> Component
                .translatableWithFallback(world.ownOrigin() ? "screen.sgcraft.settings.origin"
                        : "screen.sgcraft.settings.origin.server", world.ownOrigin() ? "Point of origin: %s"
                        : "Point of origin: %s (server's)", SettingsScreen.originText(world.originNext())))
                .orElse(Component.translatableWithFallback("screen.sgcraft.settings.origin.none_selected",
                        "Point of origin")), b -> selected.ifPresent(this::pickOrigin))
                .bounds(left, top, 104, 20).build());
        origin.active = selected.isPresent();
        origin.setTooltip(Tooltip.create(Component.translatableWithFallback("screen.sgcraft.settings.origin.tip",
                "The symbol every address dialed from this world ends with. Changing it gives every gate in the "
                        + "world a new address")));
        final List<Group> groups = state.groups().stream().filter(group -> !group.symbolNext().isEmpty()).toList();
        final List<String> names = groups.stream().map(Group::name).toList();
        final String current = selected.map(World::groupNext).filter(names::contains)
                .orElse(AddressSettings.DEFAULT_GROUP);
        final CycleButton<String> group = this.addRenderableWidget(CycleButton.builder(
                (String name) -> Component.literal(name + " (" + this.groupSymbol(name) + ")"), current)
                .withValues(names).create(left + 108, top, 104, 20,
                        Component.translatableWithFallback("screen.sgcraft.settings.group", "Group"),
                        (b, value) -> selected.ifPresent(world -> this.send(Kind.WORLD_GROUP, world.id(), value,
                                ""))));
        group.active = selected.isPresent();
        group.setTooltip(Tooltip.create(Component.translatableWithFallback("screen.sgcraft.settings.group.tip",
                "The group of worlds this world is in. A group holds up to %s worlds. Changing it gives the "
                        + "world's gates new addresses of 8 and 9 symbols", Symbol.COUNT)));
        final Button cell = this.addRenderableWidget(Button.builder(selected.map(world -> Component
                .translatableWithFallback(world.ownCell() ? "screen.sgcraft.settings.cell"
                        : "screen.sgcraft.settings.cell.server", world.ownCell() ? "Cells: %s chunks"
                        : "Cells: %s (server's)", world.cellNext()))
                .orElse(Component.translatableWithFallback("screen.sgcraft.settings.cell.none_selected",
                        "Cell size")), b -> selected.ifPresent(this::pickCell))
                .bounds(left + 216, top, 104, 20).build());
        cell.active = selected.isPresent();
        cell.setTooltip(Tooltip.create(Component.translatableWithFallback("screen.sgcraft.settings.cell.tip",
                "How many chunks across each of this world's cells is. Gates in one cell share an address. "
                        + "Changing it gives every gate in the world a new address")));
        final Button every = this.addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "screen.sgcraft.settings.origin.every", "Every world's origin: %s",
                SettingsScreen.originText(state.originNext())), b -> this.pickServerOrigin())
                .bounds(left, top + 24, 158, 20).build());
        every.setTooltip(Tooltip.create(Component.translatableWithFallback(
                "screen.sgcraft.settings.origin.every.tip", "The point of origin of every world without one of its "
                        + "own")));
        final Button everyCell = this.addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "screen.sgcraft.settings.cell.every", "Every world's cells: %s chunks", state.cellNext()),
                b -> this.pickServerCell()).bounds(left + 162, top + 24, 158, 20).build());
        everyCell.setTooltip(Tooltip.create(Component.translatableWithFallback(
                "screen.sgcraft.settings.cell.every.tip", "How many chunks across the cells are in every world "
                        + "without a cell size of its own")));
    }

    private void groupsPage(final int left, final int top) {
        final State state = this.state;
        final Rows rows = this.list;
        for (final Group group : state.groups()) {
            rows.add(new GroupRow(group), group.name().equals(this.selectedGroup));
        }
        final long count = state.groups().stream().filter(group -> !group.symbolNext().isEmpty()).count();
        final EditBox name = this.addRenderableWidget(new EditBox(this.font, left, top, 156, 20,
                Component.translatableWithFallback("screen.sgcraft.settings.new_group", "New group's name")));
        name.setMaxLength(AddressSettings.MAX_GROUP_NAME);
        name.setHint(Component.translatableWithFallback("screen.sgcraft.settings.new_group", "New group's name"));
        name.setValue(this.newGroup);
        final Button create = this.addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "screen.sgcraft.settings.create", "Create group"), b -> this.send(Kind.CREATE_GROUP, "",
                this.newGroup, "")).bounds(left + 164, top, 156, 20).build());
        final Runnable check = () -> {
            final boolean taken = state.groups().stream().anyMatch(group -> group.name().equals(this.newGroup));
            create.active = count < Symbol.COUNT && AddressSettings.isGroupName(this.newGroup) && !taken;
            create.setTooltip(Tooltip.create(count >= Symbol.COUNT
                    ? Component.translatableWithFallback("screen.sgcraft.settings.create.full", "A server can have "
                            + "only %s groups", Symbol.COUNT)
                    : Component.translatableWithFallback("screen.sgcraft.settings.create.tip", "Makes a group with "
                            + "a symbol SGCraft chooses. Its name is lowercase letters, digits and underscores")));
        };
        name.setResponder(text -> {
            this.newGroup = text.toLowerCase(Locale.ROOT);
            check.run();
        });
        check.run();
        final Optional<Group> selected = state.groups().stream().filter(g -> g.name().equals(this.selectedGroup))
                .findFirst();
        final boolean live = selected.filter(group -> !group.symbolNext().isEmpty()).isPresent();
        final Button symbol = this.addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "screen.sgcraft.settings.group_symbol", "Symbol: %s", selected.map(Group::symbolNext).orElse("")),
                b -> selected.ifPresent(this::pickGroupSymbol)).bounds(left, top + 24, 156, 20).build());
        symbol.active = live;
        symbol.setTooltip(Tooltip.create(Component.translatableWithFallback("screen.sgcraft.settings.group_symbol.tip",
                "The symbol that names this group in addresses of 9 symbols. Changing it gives its worlds' gates new "
                        + "addresses of 9 symbols")));
        final Button remove = this.addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "screen.sgcraft.settings.remove", "Remove group"), b -> selected.ifPresent(group ->
                this.send(Kind.REMOVE_GROUP, "", group.name(), ""))).bounds(left + 164, top + 24, 156, 20).build());
        remove.active = live && !selected.get().name().equals(AddressSettings.DEFAULT_GROUP);
        remove.setTooltip(Tooltip.create(Component.translatableWithFallback("screen.sgcraft.settings.remove.tip",
                "Removes the group. Its worlds go to the default group")));
    }

    private String groupSymbol(final String name) {
        return this.state == null ? "" : this.state.groups().stream().filter(group -> group.name().equals(name))
                .map(Group::symbolNext).findFirst().orElse("");
    }

    private static Component originText(final String origin) {
        return origin.isEmpty() ? Component.translatableWithFallback("screen.sgcraft.settings.origin.no_symbol",
                "none") : Component.literal(origin);
    }

    private void pickOrigin(final World world) {
        this.minecraft.gui.setScreen(new SymbolPickerScreen(this, Component.translatableWithFallback(
                "screen.sgcraft.settings.pick_origin", "Point of origin for %s", world.id()), world.originNext(),
                List.of(Extra.NONE, Extra.SERVER), Set.of(), value -> {
                    if (value.equals(Extra.SERVER.value())) {
                        this.send(Kind.CLEAR_ORIGIN, world.id(), "", "");
                    } else {
                        this.send(Kind.POINT_OF_ORIGIN, world.id(), "", value);
                    }
                }));
    }

    private void pickServerOrigin() {
        this.minecraft.gui.setScreen(new SymbolPickerScreen(this, Component.translatableWithFallback(
                "screen.sgcraft.settings.pick_every_origin", "Every world's point of origin"),
                this.state == null ? "" : this.state.originNext(), List.of(Extra.NONE), Set.of(),
                value -> this.send(Kind.POINT_OF_ORIGIN, "", "", value)));
    }

    private void pickCell(final World world) {
        this.minecraft.gui.setScreen(new CellSizeScreen(this, Component.translatableWithFallback(
                "screen.sgcraft.settings.pick_cell", "Cell size for %s", world.id()), world.cellNext(), true,
                chunks -> this.sendCell(world.id(), chunks)));
    }

    private void pickServerCell() {
        this.minecraft.gui.setScreen(new CellSizeScreen(this, Component.translatableWithFallback(
                "screen.sgcraft.settings.pick_every_cell", "Every world's cell size"),
                this.state == null ? 0 : this.state.cellNext(), false, chunks -> this.sendCell("", chunks)));
    }

    private void sendCell(final String world, final OptionalInt chunks) {
        if (chunks.isPresent()) {
            this.send(Kind.CELL_CHUNKS, world, "", String.valueOf(chunks.getAsInt()));
        } else {
            this.send(Kind.CLEAR_CELL, world, "", "");
        }
    }

    private void pickGroupSymbol(final Group group) {
        // Each symbol another group has is taken
        final Set<String> taken = this.state == null ? Set.of() : this.state.groups().stream()
                .filter(other -> !other.name().equals(group.name()) && !other.symbolNext().isEmpty())
                .map(Group::symbolNext).collect(Collectors.toSet());
        final List<Extra> extras = group.name().equals(AddressSettings.DEFAULT_GROUP) ? List.of()
                : List.of(Extra.AUTOMATIC);
        this.minecraft.gui.setScreen(new SymbolPickerScreen(this, Component.translatableWithFallback(
                "screen.sgcraft.settings.pick_group_symbol", "Symbol for group %s", group.name()),
                group.automatic() ? Extra.AUTOMATIC.value() : group.symbolNext(), extras, taken,
                value -> this.send(Kind.GROUP_SYMBOL, "", group.name(),
                        value.equals(Extra.AUTOMATIC.value()) ? "" : value)));
    }

    // Holds a change to the address settings, and previews it with those held before it
    private void send(final Kind kind, final String world, final String group, final String value) {
        this.changes.add(new Change(kind, world, group, value));
        this.preview();
    }

    @Override
    public void onClose() {
        if (!this.unsaved()) {
            this.minecraft.gui.setScreen(this.parent);
            return;
        }
        this.minecraft.gui.setScreen(new ConfirmScreen(leave -> this.minecraft.gui.setScreen(leave ? this.parent
                : this), Component.translatableWithFallback("screen.sgcraft.settings.leave", "Leave without saving?"),
                Component.translatableWithFallback("screen.sgcraft.settings.leave.text", "The changes made on this "
                        + "screen haven't been saved. Leaving drops them"),
                Component.translatableWithFallback("screen.sgcraft.settings.leave.yes", "Drop them"),
                CommonComponents.GUI_CANCEL));
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
            final float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        final int center = this.width / 2;
        final Component note = this.unsaved() ? Component.translatableWithFallback(
                "screen.sgcraft.settings.unsaved", "Changes not saved yet. Save keeps them, and Discard drops them")
                : switch (this.page) {
            case SettingsScreen.WORLDS -> Component.translatableWithFallback("screen.sgcraft.settings.note",
                    "Changes take effect when the server next starts");
            case SettingsScreen.GROUPS -> Component.translatableWithFallback("screen.sgcraft.settings.groups_note",
                    "%s of %s groups. Changes take effect when the server next starts", this.state == null ? 0
                            : this.state.groups().stream().filter(group -> !group.symbolNext().isEmpty()).count(),
                    Symbol.COUNT);
            case SettingsScreen.CLIENT -> Component.translatableWithFallback(
                    "screen.sgcraft.settings.client_note.save", "Changes apply when you save them");
            default -> Component.translatableWithFallback("screen.sgcraft.settings.options_note.save", "Changes "
                    + "apply when you save them, except those marked * which take effect when the server next starts");
        };
        int y = 30;
        for (final FormattedCharSequence line : this.font.split(note, this.width - 20)) {
            graphics.centeredText(this.font, line, center, y, this.unsaved() ? 0xFF60FFFF : 0xFFA0A0A0);
            y += 10;
        }
        final List<Component> messages = new ArrayList<>(this.messages);
        if (this.page.equals(SettingsScreen.CLIENT) && !this.allowed()) {
            messages.add(Minecraft.getInstance().getConnection() == null
                    ? Component.translatableWithFallback("screen.sgcraft.settings.no_world", "Open a world to change "
                            + "the server's settings. On a server, only operators can change them")
                    : this.state == null
                            ? Component.translatableWithFallback("screen.sgcraft.settings.waiting", "Asking the "
                                    + "server for its settings")
                            : Component.translatableWithFallback("screen.sgcraft.settings.not_operator", "Only "
                                    + "operators can change the server's settings"));
        }
        final boolean options = !this.page.equals(SettingsScreen.WORLDS) && !this.page.equals(SettingsScreen.GROUPS);
        y = this.height - (options ? SettingsScreen.OPTIONS_BOTTOM - 4 : SettingsScreen.BOTTOM - 56);
        for (final Component message : messages) {
            for (final FormattedCharSequence line : this.font.split(message, SettingsScreen.WIDTH)) {
                if (y > this.height - 38) {
                    return;
                }
                graphics.centeredText(this.font, line, center, y, 0xFFFFFFFF);
                y += 10;
            }
        }
    }

    /** The list of worlds or groups. */
    final class Rows extends ObjectSelectionList<Row> {

        Rows(final Minecraft minecraft, final int width, final int height, final int y) {
            super(minecraft, width, height, y, SettingsScreen.ROW);
        }

        void add(final Row row, final boolean selected) {
            this.addEntry(row);
            if (selected) {
                this.setSelected(row);
            }
        }

        @Override
        public int getRowWidth() {
            return SettingsScreen.WIDTH - 12;
        }
    }

    abstract static class Row extends ObjectSelectionList.Entry<Row> {
    }

    // A world: its name and gates, and its group, symbol, point of origin and cell size, after a restart if they
    // change then
    private final class WorldRow extends Row {

        private final World world;

        WorldRow(final World world) {
            this.world = world;
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.world.id());
        }

        @Override
        public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
                final boolean hovered, final float a) {
            final SettingsScreen screen = SettingsScreen.this;
            final World world = this.world;
            graphics.text(screen.font, world.id(), this.getContentX() + 2, this.getContentY() + 1, 0xFFFFFFFF);
            final Component gates = Component.translatableWithFallback("screen.sgcraft.settings.gates", "%s gates",
                    world.gates());
            graphics.text(screen.font, gates, this.getContentRight() - screen.font.width(gates) - 2,
                    this.getContentY() + 1, 0xFFA0A0A0);
            final boolean changes = !world.groupNext().equals(world.groupNow())
                    || !world.symbolNext().equals(world.symbolNow()) || !world.originNext().equals(world.originNow())
                    || world.cellNext() != world.cellNow()
                    || !screen.groupSymbol(world.groupNext()).equals(screen.groupSymbol(world.groupNow()));
            final Component line = Component.translatableWithFallback(changes
                    ? "screen.sgcraft.settings.world_line.next" : "screen.sgcraft.settings.world_line", changes
                    ? "After a restart: group %s (%s), symbol %s, origin %s, %s-chunk cells"
                    : "Group %s (%s), symbol %s, origin %s, %s-chunk cells", world.groupNext(),
                    screen.groupSymbol(world.groupNext()), world.symbolNext().isEmpty()
                            ? Component.translatableWithFallback("screen.sgcraft.settings.origin.no_symbol", "none")
                            : Component.literal(world.symbolNext()), SettingsScreen.originText(world.originNext()),
                    world.cellNext());
            graphics.text(screen.font, line, this.getContentX() + 2, this.getContentY() + 12,
                    changes ? (world.gates() > 0 ? 0xFFFF6060 : 0xFFFFFF60) : 0xFFA0A0A0);
        }

        @Override
        public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
            SettingsScreen.this.selectedWorld = this.world.id();
            SettingsScreen.this.relayout = true;
            return true;
        }
    }

    // A group: its name and symbol and how many worlds it has, and whether it is new, gone or changes its symbol
    private final class GroupRow extends Row {

        private final Group group;

        GroupRow(final Group group) {
            this.group = group;
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.group.name());
        }

        @Override
        public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
                final boolean hovered, final float a) {
            final SettingsScreen screen = SettingsScreen.this;
            final Group group = this.group;
            final boolean gone = group.symbolNext().isEmpty();
            final String symbol = gone ? group.symbolNow() : group.symbolNext();
            graphics.text(screen.font, group.name() + " (" + symbol + ")", this.getContentX() + 2,
                    this.getContentY() + 1, 0xFFFFFFFF);
            if (!gone) {
                final Component worlds = Component.translatableWithFallback("screen.sgcraft.settings.group_worlds",
                        "%s of %s worlds", group.worlds(), Symbol.COUNT);
                graphics.text(screen.font, worlds, this.getContentRight() - screen.font.width(worlds) - 2,
                        this.getContentY() + 1, group.worlds() >= Symbol.COUNT ? 0xFFFF6060 : 0xFFA0A0A0);
            }
            final Component line;
            int color = 0xFFFFFF60;
            if (gone) {
                line = Component.translatableWithFallback("screen.sgcraft.settings.group.gone", "Gone after a "
                        + "restart");
            } else if (group.symbolNow().isEmpty()) {
                line = Component.translatableWithFallback("screen.sgcraft.settings.group.new", "New. It exists after "
                        + "a restart");
            } else if (!group.symbolNow().equals(group.symbolNext())) {
                line = Component.translatableWithFallback("screen.sgcraft.settings.group.symbol_now", "Its symbol "
                        + "is %s until a restart", group.symbolNow());
            } else {
                color = 0xFFA0A0A0;
                line = group.name().equals(AddressSettings.DEFAULT_GROUP)
                        ? Component.translatableWithFallback("screen.sgcraft.settings.group.default", "Every world "
                                + "not put in another group")
                        : group.automatic()
                                ? Component.translatableWithFallback("screen.sgcraft.settings.group.automatic",
                                        "SGCraft chose its symbol")
                                : Component.translatableWithFallback("screen.sgcraft.settings.group.own",
                                        "Its symbol is set in the settings");
            }
            graphics.text(screen.font, line, this.getContentX() + 2, this.getContentY() + 12, color);
        }

        @Override
        public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
            SettingsScreen.this.selectedGroup = this.group.name();
            SettingsScreen.this.relayout = true;
            return true;
        }
    }
}
