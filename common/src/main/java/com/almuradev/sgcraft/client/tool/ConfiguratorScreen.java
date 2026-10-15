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

package com.almuradev.sgcraft.client.tool;

import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.client.ClientPlatform;
import com.almuradev.sgcraft.tool.ToolPayloads.Change;
import com.almuradev.sgcraft.tool.ToolPayloads.ConfiguratorApply;
import com.almuradev.sgcraft.tool.ToolPayloads.ConfiguratorState;
import com.almuradev.sgcraft.tool.ToolPayloads.Named;
import com.almuradev.sgcraft.tool.ToolPayloads.Property;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The configurator: everything that can be set on a gate, over five pages. Gate holds its type, chevrons and iris,
 * and whether a gate lying flat faces up or down; Energy its store and what dialing costs it; Travel how its wormholes
 * behave; Access who may use it and which gates may connect; Other the properties add-ons register, as JSON. Changes
 * wait until Save, which sends only what changed.
 */
public final class ConfiguratorScreen extends Screen {

    private static final String NS = "sgcraft:";
    // The gate's orientations, as the server names them
    private static final String STANDING = "standing";
    private static final String FACE_UP = "face_up";
    private static final String FACE_DOWN = "face_down";
    private static final List<String> RIGHTS = List.of("sgcraft:use", "sgcraft:iris", "sgcraft:configure");
    private static final int ROWS = 5;
    // Where the settings start, under the tabs and the gate's address
    private static final int TOP = 46;

    private enum Page {
        GATE, ENERGY, TRAVEL, ACCESS, OTHER
    }

    // The dial costs' properties, each a switch and an amount, which the server's settings fill in for a gate without
    // its own
    private static final String[][] COSTS = {
            {"chevron_cost", "Chevron cost", "Per chevron"},
            {"open_cost", "Opening cost", "Opening"},
            {"tick_cost", "Tick cost", "Per tick"},
    };

    // What each setting does, shown when the pointer rests on it, by the setting's name
    private static final Map<String, String> TIPS = Map.ofEntries(
            Map.entry("type", "The kind of gate: Milky Way, Pegasus or Universe. It changes how the gate looks and "
                    + "dials"),
            Map.entry("chevrons", "How many chevrons the gate has. More chevrons dial longer addresses, such as "
                    + "gates in other worlds"),
            Map.entry("iris", "Whether an iris is fitted. Taking it off gives back the iris upgrade"),
            Map.entry("orientation", "Whether the gate stands upright or lies flat. A gate lying flat can be turned "
                    + "face up or face down; one standing stays as it was built"),
            Map.entry("open_ticks", "How many seconds a wormhole stays open before it shuts on its own. 0 for no "
                    + "limit"),
            Map.entry("address_visible", "Whether players can see the gate's address on the gate"),
            Map.entry("breakable", "Whether players can break the gate's blocks"),
            Map.entry("generated", "Whether world generation built the gate or a player did. Add-ons can tell them "
                    + "apart by this"),
            Map.entry("chevrons_lock_on_dial", "Whether a DHD or PDD locks every chevron at once. Off, the ring "
                    + "turns for each symbol and each chevron is paid for as it locks"),
            Map.entry("horizon_transparent", "Whether the open wormhole lets a little of what is behind it show "
                    + "through. Off, it is solid"),
            Map.entry("ring_speed", "How fast the gate dials, in degrees a tick: its ring's turn, or a Pegasus "
                    + "gate's running light. Empty for its type's speed, shown faintly"),
            Map.entry("dhd_range", "How far from the gate's base, in blocks, a DHD can stand and still link to it"),
            Map.entry("opening_damage_amount", "How much harm the kawoosh does each tick to what stands in front of "
                    + "the gate, when Kawoosh damage is on. 20 is a player's full health"),
            Map.entry("dhd_fuel", "Whether naquadah in the DHDs linked to the gate powers it"),
            Map.entry("preserve_inventory", "Whether a player the gate kills, against its shut iris or going "
                    + "backwards through a one way wormhole, keeps what they carry. Off, it is lost, unless the "
                    + "keepInventory game rule is on"),
            Map.entry("requires_power", "Whether dialing out costs this gate energy at all. Off, it dials for free"),
            Map.entry("energy_capacity", "How much gate energy the gate holds itself. It fills from power units and "
                    + "DHD fuel, and pays when they run short"),
            Map.entry("chevron_cost_enabled", "Whether the gate pays energy as each chevron locks. Shows the "
                    + "server's setting until changed"),
            Map.entry("chevron_cost", "Gate energy paid as each chevron locks, before distance. Empty for the "
                    + "server's amount, shown faintly"),
            Map.entry("open_cost_enabled", "Whether the gate pays energy as the wormhole forms. Shows the server's "
                    + "setting until changed"),
            Map.entry("open_cost", "Gate energy paid as the wormhole forms, before distance. Empty for the "
                    + "server's amount, shown faintly"),
            Map.entry("tick_cost_enabled", "Whether the gate pays energy while the wormhole is open. Shows the "
                    + "server's setting until changed"),
            Map.entry("tick_cost", "Gate energy paid each tick the wormhole is open, 20 ticks a second, before "
                    + "distance. Fractions add up. Empty for the server's amount, shown faintly"),
            Map.entry("server_costs", "Clears this gate's own costs, so it pays what the server sets"),
            Map.entry("one_way_travel", "Whether travelers can only go from the dialing gate to the dialed gate"),
            Map.entry("reverse_travel_kills", "Whether stepping into the dialed gate's wormhole while travel is one "
                    + "way kills"),
            Map.entry("close_from_either_end", "Whether the dialed gate can shut the wormhole as well as the "
                    + "dialing gate"),
            Map.entry("opening_damage", "Whether the burst of a wormhole opening harms what stands in front of "
                    + "the gate"),
            Map.entry("spills_fluid", "Whether water and other fluids pour through the wormhole to the other gate"),
            Map.entry("phase_shift_passes_iris", "Whether someone holding a Tollan phase shift device passes "
                    + "through this gate's shut iris"),
            Map.entry("breach_explosion", "How strong an explosion goes off if the gate is broken while its "
                    + "wormhole is open. TNT is 4; 0 for none"),
            Map.entry("breach_fire", "Whether that explosion sets fires"),
            Map.entry("breach_breaks_blocks", "Whether that explosion breaks blocks, or only harms what stands near"),
            Map.entry("iris_restores_state", "Whether the iris goes back to how it was before the wormhole opened, "
                    + "once it closes"),
            Map.entry("redstone_input", "Whether a redstone signal can work the gate, such as shutting its iris"),
            Map.entry("redstone_output", "Whether the gate gives out a redstone signal showing what it is doing"),
            Map.entry("allow_incoming", "Whether other gates may dial this one, unless an address rule says "
                    + "otherwise"),
            Map.entry("allow_outgoing", "Whether this gate may dial others, unless an address rule says otherwise"),
            Map.entry("right.use", "Dialing the gate and traveling through it"),
            Map.entry("right.iris", "Opening and shutting the gate's iris"),
            Map.entry("right.configure", "Changing these settings"),
            Map.entry("default_rights", "What players the gate doesn't list may do: %s"),
            Map.entry("player", "A player's name, to give them rights of their own. List player adds them"),
            Map.entry("rule", "An address, to let that gate connect or not whatever the settings above say"),
            Map.entry("rule.incoming", "Whether that gate may dial this one"),
            Map.entry("rule.outgoing", "Whether this gate may dial that one"),
            Map.entry("other", "A setting an add-on gave gates, written as JSON. Empty for its default")
    );

    // Vanilla's tabs, one for each page; choosing one lays out its page
    private final TabManager tabs = new TabManager(this::addRenderableWidget, this::removeWidget, this::choose,
            tab -> { });
    private @Nullable MenuTabBar tabBar;
    private ConfiguratorState state;
    // The values being edited, by property identifier; absent means the property's default
    private final Map<String, JsonElement> values = new LinkedHashMap<>();
    private final Map<String, JsonElement> original = new LinkedHashMap<>();
    // What applies to a property the gate has no value for, by property identifier
    private final Map<String, JsonElement> fallbacks = new LinkedHashMap<>();
    private final Map<String, String> playerNames = new LinkedHashMap<>();
    private String type;
    private int chevrons;
    // Whether the gate stands or lies flat, by its serialized name
    private String orientation;
    private Page page = Page.GATE;
    private int scroll;
    private String addPlayer = "";
    private String addRule = "";
    // Text drawn beside the number boxes
    private final List<Label> labels = new ArrayList<>();

    private record Label(int x, int y, Component text) {
    }

    public ConfiguratorScreen(final ConfiguratorState state) {
        super(Component.translatableWithFallback("screen.sgcraft.configurator", "Configurator"));
        this.state = state;
        this.type = state.gate().type();
        this.chevrons = state.chevrons();
        this.orientation = state.orientation();
        this.load(state);
    }

    private void load(final ConfiguratorState state) {
        this.values.clear();
        this.original.clear();
        this.fallbacks.clear();
        for (final Property property : state.properties()) {
            if (!property.fallback().isEmpty()) {
                this.fallbacks.put(property.id(), JsonParser.parseString(property.fallback()));
            }
            if (!property.json().isEmpty()) {
                final JsonElement json = JsonParser.parseString(property.json());
                this.values.put(property.id(), json);
                this.original.put(property.id(), json.deepCopy());
            }
        }
        this.playerNames.clear();
        for (final Named named : state.players()) {
            this.playerNames.put(named.uuid(), named.name());
        }
    }

    /** Shows the gate as the server now has it, dropping unsaved changes. */
    public void update(final ConfiguratorState state) {
        this.state = state;
        this.type = state.gate().type();
        this.chevrons = state.chevrons();
        this.orientation = state.orientation();
        this.load(state);
        this.rebuildWidgets();
    }

    @Override
    protected void init() {
        final int left = this.width / 2 - 155;
        final MenuTabBar.Builder bar = MenuTabBar.builder(this.tabs, this.width);
        for (final Page each : Page.values()) {
            bar.addTab(new PageTab(each));
        }
        this.tabBar = this.addRenderableWidget(bar.build());
        this.tabBar.selectTab(this.page.ordinal(), false);
        this.tabBar.arrangeElements(this.width);
        final int top = ConfiguratorScreen.TOP;
        switch (this.page) {
            case GATE -> this.gatePage(left, top);
            case ENERGY -> this.energyPage(left, top);
            case TRAVEL -> this.travelPage(left, top);
            case ACCESS -> this.accessPage(left, top);
            case OTHER -> this.otherPage(left, top);
        }
        this.addRenderableWidget(Button.builder(Component.translatableWithFallback("screen.sgcraft.configurator.save",
                "Save"), b -> this.save()).bounds(this.width / 2 - 154, this.height - 28, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "screen.sgcraft.configurator.revert", "Revert"), b -> this.update(this.state))
                .bounds(this.width / 2 - 50, this.height - 28, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatableWithFallback("screen.sgcraft.close", "Close"),
                b -> this.onClose()).bounds(this.width / 2 + 54, this.height - 28, 100, 20).build());
    }

    // A page's tab. The page lays out its own widgets, so the tab holds none.
    private static final class PageTab extends GridLayoutTab {

        private final Page page;

        PageTab(final Page page) {
            super(Component.translatableWithFallback(
                    "screen.sgcraft.configurator." + page.name().toLowerCase(Locale.ROOT),
                    page.name().charAt(0) + page.name().substring(1).toLowerCase(Locale.ROOT)));
            this.page = page;
        }
    }

    private void choose(final @Nullable Tab tab) {
        if (tab instanceof final PageTab chosen && chosen.page != this.page) {
            this.page = chosen.page;
            this.scroll = 0;
            this.rebuildWidgets();
        }
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        return this.tabBar != null && this.tabBar.keyPressed(event) || super.keyPressed(event);
    }

    // Two columns of settings, each 150 wide
    private int[] slot(final int left, final int top, final int index) {
        return new int[] {left + (index % 2) * 160, top + (index / 2) * 24};
    }

    private void gatePage(final int left, final int top) {
        int i = 0;
        int[] at = this.slot(left, top, i++);
        this.tip(this.addRenderableWidget(CycleButton.builder(this::typeName, this.type)
                .withValues(this.state.types()).create(at[0], at[1], 150, 20,
                        Component.translatableWithFallback("screen.sgcraft.configurator.type", "Type"),
                        (b, value) -> this.type = value)), "type");
        at = this.slot(left, top, i++);
        final List<Integer> counts = List.of(7, 8, 9);
        this.tip(this.addRenderableWidget(CycleButton.builder((Integer n) -> Component.literal(String.valueOf(n)),
                counts.contains(this.chevrons) ? this.chevrons : 9).withValues(counts).create(at[0], at[1], 150, 20,
                Component.translatableWithFallback("screen.sgcraft.configurator.chevrons", "Chevrons"),
                (b, value) -> this.chevrons = value)), "chevrons");
        at = this.slot(left, top, i++);
        final boolean iris = this.values.containsKey(ConfiguratorScreen.NS + "iris");
        this.tip(this.addRenderableWidget(CycleButton.onOffBuilder(iris).create(at[0], at[1], 150, 20,
                Component.translatableWithFallback("screen.sgcraft.configurator.iris", "Iris fitted"),
                (b, value) -> {
                    if (value) {
                        this.values.put(ConfiguratorScreen.NS + "iris", new JsonPrimitive("sgcraft:metal"));
                    } else {
                        this.values.remove(ConfiguratorScreen.NS + "iris");
                    }
                })), "iris");
        at = this.slot(left, top, i++);
        this.number(at, "open_ticks", "Open seconds", 20);
        at = this.slot(left, top, i++);
        this.toggle(at, "address_visible", "Address shown", true);
        at = this.slot(left, top, i++);
        this.toggle(at, "breakable", "Breakable", false);
        at = this.slot(left, top, i++);
        this.toggle(at, "chevrons_lock_on_dial", "Chevrons lock at once", false);
        at = this.slot(left, top, i++);
        this.toggle(at, "horizon_transparent", "See-through horizon", true);
        at = this.slot(left, top, i++);
        this.decimal(at, "ring_speed", "Ring speed");
        at = this.slot(left, top, i++);
        this.decimal(at, "dhd_range", "DHD reach");
        at = this.slot(left, top, i);
        // Whether world generation built the gate: a fact about it, shown but not set here
        final boolean generated = this.values.containsKey(ConfiguratorScreen.NS + "generated")
                && this.values.get(ConfiguratorScreen.NS + "generated").getAsBoolean();
        final Button shown = this.addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "screen.sgcraft.configurator.generated", "Built by: %s", generated
                        ? Component.translatableWithFallback("screen.sgcraft.configurator.generated.world", "World")
                        : Component.translatableWithFallback("screen.sgcraft.configurator.generated.player", "Player")),
                b -> { }).bounds(at[0], at[1], 150, 20).build());
        shown.active = false;
        this.tip(shown, "generated");
        this.orientationButton(this.slot(left, top, i + 1));
    }

    // Face up or face down for a gate lying flat, as the original's checkboxes; shown but not set for one standing
    private void orientationButton(final int[] at) {
        final Component label = Component.translatableWithFallback("screen.sgcraft.configurator.orientation",
                "Lies");
        final boolean flat = !ConfiguratorScreen.STANDING.equals(this.state.orientation());
        final List<String> choices = flat ? List.of(ConfiguratorScreen.FACE_UP, ConfiguratorScreen.FACE_DOWN)
                : List.of(ConfiguratorScreen.STANDING);
        final CycleButton<String> button = this.addRenderableWidget(CycleButton.builder(
                ConfiguratorScreen::orientationName, choices.contains(this.orientation) ? this.orientation
                        : choices.getFirst()).withValues(choices).create(at[0], at[1], 150, 20, label,
                (b, value) -> this.orientation = value));
        button.active = flat;
        this.tip(button, "orientation");
    }

    private static Component orientationName(final String orientation) {
        final String fallback = switch (orientation) {
            case ConfiguratorScreen.FACE_UP -> "Flat, face up";
            case ConfiguratorScreen.FACE_DOWN -> "Flat, face down";
            default -> "Upright";
        };
        return Component.translatableWithFallback("screen.sgcraft.configurator.orientation." + orientation, fallback);
    }

    // The store, and what dialing costs: each cost a switch and an amount, the server's unless the gate has its own
    private void energyPage(final int left, final int top) {
        this.toggle(this.slot(left, top, 0), "requires_power", "Needs energy", true);
        this.number(this.slot(left, top, 1), "energy_capacity", "Reserve", 1);
        int i = 2;
        for (final String[] cost : ConfiguratorScreen.COSTS) {
            this.toggle(this.slot(left, top, i++), cost[0] + "_enabled", cost[1], true);
            this.decimal(this.slot(left, top, i++), cost[0], cost[2]);
        }
        this.toggle(this.slot(left, top, i++), "dhd_fuel", "DHD fuel powers it", true);
        final int[] at = this.slot(left, top, i);
        this.tip(this.addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "screen.sgcraft.configurator.server_costs", "Use server's costs"), b -> {
                    for (final String[] cost : ConfiguratorScreen.COSTS) {
                        this.values.remove(ConfiguratorScreen.NS + cost[0] + "_enabled");
                        this.values.remove(ConfiguratorScreen.NS + cost[0]);
                    }
                    this.rebuildWidgets();
                }).bounds(at[0], at[1], 150, 20).build()), "server_costs");
    }

    private void travelPage(final int left, final int top) {
        final String[][] toggles = {
                {"one_way_travel", "One way travel", "true"},
                {"reverse_travel_kills", "Reverse travel kills", "false"},
                {"close_from_either_end", "Close from either end", "true"},
                {"opening_damage", "Kawoosh damage", "true"},
                {"spills_fluid", "Fluid pours through", "true"},
                {"phase_shift_passes_iris", "Phase shift passes iris", "true"},
                {"iris_restores_state", "Iris shuts again after", "false"},
                {"preserve_inventory", "Iris deaths keep items", "false"},
                {"redstone_input", "Redstone works iris", "true"},
                {"redstone_output", "Redstone signal out", "true"},
        };
        for (int i = 0; i < toggles.length; i++) {
            this.toggle(this.slot(left, top, i), toggles[i][0], toggles[i][1], Boolean.parseBoolean(toggles[i][2]));
        }
        this.decimal(this.slot(left, top, toggles.length), "opening_damage_amount", "Kawoosh harm");
        this.decimal(this.slot(left, top, toggles.length + 1), "breach_explosion", "Breach blast");
        this.toggle(this.slot(left, top, toggles.length + 2), "breach_fire", "Breach fire", true);
        this.toggle(this.slot(left, top, toggles.length + 3), "breach_breaks_blocks", "Breach breaks blocks", true);
    }

    private void accessPage(final int left, final int top) {
        this.toggle(this.slot(left, top, 0), "allow_incoming", "Incoming allowed", true);
        this.toggle(this.slot(left, top, 1), "allow_outgoing", "Outgoing allowed", true);
        // The defaults for players the gate doesn't list
        final JsonArray defaults = this.values.containsKey(ConfiguratorScreen.NS + "default_rights")
                ? this.values.get(ConfiguratorScreen.NS + "default_rights").getAsJsonArray()
                : ConfiguratorScreen.allRights();
        int x = left;
        for (final String right : ConfiguratorScreen.RIGHTS) {
            final boolean has = defaults.contains(new JsonPrimitive(right));
            final AbstractWidget all = this.addRenderableWidget(CycleButton.onOffBuilder(has).create(x, top + 24,
                    100, 20,
                    Component.literal("All: " + ConfiguratorScreen.shortId(right)), (b, value) -> {
                        final JsonArray now = this.values.containsKey(ConfiguratorScreen.NS + "default_rights")
                                ? this.values.get(ConfiguratorScreen.NS + "default_rights").getAsJsonArray()
                                : ConfiguratorScreen.allRights();
                        this.values.put(ConfiguratorScreen.NS + "default_rights",
                                ConfiguratorScreen.withRight(now, right, value));
                    }));
            all.setTooltip(Tooltip.create(Component.translatableWithFallback(
                    "screen.sgcraft.configurator.default_rights.tip", ConfiguratorScreen.TIPS.get("default_rights"),
                    ConfiguratorScreen.tipText("right." + ConfiguratorScreen.shortId(right)))));
            x += 105;
        }
        // Listed players, a page at a time, each with their own rights
        final JsonObject players = this.object("player_rights");
        final List<String> uuids = new ArrayList<>(players.keySet());
        int y = top + 52;
        for (int row = this.scroll; row < Math.min(uuids.size(), this.scroll + ConfiguratorScreen.ROWS); row++) {
            final String uuid = uuids.get(row);
            final JsonArray rights = players.get(uuid).getAsJsonArray();
            x = left + 100;
            for (final String right : ConfiguratorScreen.RIGHTS) {
                this.tip(this.addRenderableWidget(CycleButton.onOffBuilder(rights.contains(new JsonPrimitive(right)))
                        .create(x, y, 62, 20, Component.literal(ConfiguratorScreen.shortId(right)), (b, value) -> {
                            final JsonObject now = this.object("player_rights");
                            now.add(uuid, ConfiguratorScreen.withRight(now.get(uuid).getAsJsonArray(), right, value));
                            this.values.put(ConfiguratorScreen.NS + "player_rights", now);
                        })), "right." + ConfiguratorScreen.shortId(right));
                x += 64;
            }
            this.addRenderableWidget(Button.builder(Component.literal("x"), b -> {
                final JsonObject now = this.object("player_rights");
                now.remove(uuid);
                this.values.put(ConfiguratorScreen.NS + "player_rights", now);
                this.rebuildWidgets();
            }).bounds(x, y, 18, 20).build());
            y += 22;
        }
        final EditBox name = this.addRenderableWidget(new EditBox(this.font, left, y + 4, 150, 20,
                Component.translatableWithFallback("screen.sgcraft.configurator.player", "Player name")));
        name.setHint(Component.translatableWithFallback("screen.sgcraft.configurator.player", "Player name"));
        name.setValue(this.addPlayer);
        name.setResponder(text -> this.addPlayer = text);
        this.tip(name, "player");
        this.addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "screen.sgcraft.configurator.add_player", "List player"), b -> this.save())
                .bounds(left + 154, y + 4, 76, 20).build());
        this.pager(left + 234, y + 4, uuids.size());
        // Rules for particular addresses
        final JsonObject rules = this.object("address_rules");
        y += 30;
        for (final String address : rules.keySet()) {
            final JsonObject rule = rules.get(address).getAsJsonObject();
            this.tip(this.addRenderableWidget(CycleButton.onOffBuilder(rule.get("incoming").getAsBoolean())
                    .create(left + 100, y, 80, 20, Component.literal("In"),
                            (b, value) -> this.rule(address, "incoming", value))), "rule.incoming");
            this.tip(this.addRenderableWidget(CycleButton.onOffBuilder(rule.get("outgoing").getAsBoolean())
                    .create(left + 182, y, 80, 20, Component.literal("Out"),
                            (b, value) -> this.rule(address, "outgoing", value))), "rule.outgoing");
            this.addRenderableWidget(Button.builder(Component.literal("x"), b -> {
                final JsonObject now = this.object("address_rules");
                now.remove(address);
                this.values.put(ConfiguratorScreen.NS + "address_rules", now);
                this.rebuildWidgets();
            }).bounds(left + 264, y, 18, 20).build());
            y += 22;
            if (y > this.height - 60) {
                break;
            }
        }
        final EditBox rule = this.addRenderableWidget(new EditBox(this.font, left, y + 4, 150, 20,
                Component.translatableWithFallback("screen.sgcraft.configurator.rule", "Address")));
        rule.setHint(Component.translatableWithFallback("screen.sgcraft.configurator.rule", "Address"));
        rule.setMaxLength(11);
        rule.setValue(this.addRule);
        rule.setResponder(text -> this.addRule = text);
        this.tip(rule, "rule");
        this.addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "screen.sgcraft.configurator.add_rule", "Add rule"), b -> {
                    final Optional<Address> address = Address.parse(this.addRule);
                    if (address.isPresent()) {
                        final JsonObject now = this.object("address_rules");
                        final JsonObject added = new JsonObject();
                        added.addProperty("incoming", true);
                        added.addProperty("outgoing", true);
                        now.add(address.get().toString(), added);
                        this.values.put(ConfiguratorScreen.NS + "address_rules", now);
                        this.addRule = "";
                        this.rebuildWidgets();
                    }
                }).bounds(left + 154, y + 4, 76, 20).build());
    }

    private void rule(final String address, final String side, final boolean value) {
        final JsonObject now = this.object("address_rules");
        now.get(address).getAsJsonObject().addProperty(side, value);
        this.values.put(ConfiguratorScreen.NS + "address_rules", now);
    }

    private void pager(final int x, final int y, final int count) {
        final Button up = this.addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            this.scroll = Math.max(0, this.scroll - ConfiguratorScreen.ROWS);
            this.rebuildWidgets();
        }).bounds(x, y, 20, 20).build());
        up.active = this.scroll > 0;
        final Button down = this.addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            this.scroll += ConfiguratorScreen.ROWS;
            this.rebuildWidgets();
        }).bounds(x + 22, y, 20, 20).build());
        down.active = this.scroll + ConfiguratorScreen.ROWS < count;
    }

    // Every property SGCraft doesn't lay out itself, as JSON
    private void otherPage(final int left, final int top) {
        final List<Property> others = this.state.properties().stream()
                .filter(property -> !property.id().startsWith(ConfiguratorScreen.NS)).toList();
        int y = top;
        for (int row = this.scroll; row < Math.min(others.size(), this.scroll + 8); row++) {
            final String id = others.get(row).id();
            final EditBox box = this.addRenderableWidget(new EditBox(this.font, left + 150, y, 160, 20,
                    Component.literal(id)));
            box.setMaxLength(32767);
            this.tip(box, "other");
            box.setValue(this.values.containsKey(id) ? this.values.get(id).toString() : "");
            box.setResponder(text -> {
                if (text.isBlank()) {
                    this.values.remove(id);
                    return;
                }
                try {
                    this.values.put(id, JsonParser.parseString(text));
                } catch (final RuntimeException e) {
                    // Left as it was until the text is valid JSON
                }
            });
            y += 24;
        }
        this.pager(left, y + 4, others.size());
    }

    private void toggle(final int[] at, final String path, final String label, final boolean fallback) {
        final String id = ConfiguratorScreen.NS + path;
        final boolean value = this.values.containsKey(id) ? this.values.get(id).getAsBoolean()
                : this.fallbacks.containsKey(id) ? this.fallbacks.get(id).getAsBoolean() : fallback;
        this.tip(this.addRenderableWidget(CycleButton.onOffBuilder(value).create(at[0], at[1], 150, 20,
                Component.translatableWithFallback("screen.sgcraft.configurator." + path, label),
                (b, now) -> this.values.put(id, new JsonPrimitive(now)))), path);
    }

    // A whole number, shown divided by {@code scale}, such as ticks shown as seconds
    private void number(final int[] at, final String path, final String label, final long scale) {
        final String id = ConfiguratorScreen.NS + path;
        final EditBox box = this.addRenderableWidget(new EditBox(this.font, at[0] + 90, at[1], 60, 20,
                Component.translatableWithFallback("screen.sgcraft.configurator." + path, label)));
        this.tip(box, path);
        box.setValue(this.values.containsKey(id) ? String.valueOf(this.values.get(id).getAsLong() / scale) : "");
        box.setResponder(entered -> {
            final String text = entered.replaceAll("[^0-9]", "");
            if (text.isEmpty()) {
                this.values.remove(id);
            } else {
                try {
                    this.values.put(id, new JsonPrimitive(Long.parseLong(text) * scale));
                } catch (final NumberFormatException e) {
                    // Too long a number is left as it was
                }
            }
        });
        this.labels.add(new Label(at[0], at[1] + 6, Component.translatableWithFallback(
                "screen.sgcraft.configurator." + path, label)));
    }

    // An amount that may have a fraction; left empty, the fallback applies, and shows faintly in the box
    private void decimal(final int[] at, final String path, final String label) {
        final String id = ConfiguratorScreen.NS + path;
        final EditBox box = this.addRenderableWidget(new EditBox(this.font, at[0] + 90, at[1], 60, 20,
                Component.translatableWithFallback("screen.sgcraft.configurator." + path, label)));
        this.tip(box, path);
        box.setValue(this.values.containsKey(id) ? ConfiguratorScreen.amount(this.values.get(id).getAsDouble()) : "");
        if (this.fallbacks.containsKey(id)) {
            box.setHint(Component.literal(ConfiguratorScreen.amount(this.fallbacks.get(id).getAsDouble()))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        box.setResponder(entered -> {
            final String text = entered.replaceAll("[^0-9.]", "");
            if (text.isEmpty()) {
                this.values.remove(id);
                return;
            }
            try {
                this.values.put(id, new JsonPrimitive(Double.parseDouble(text)));
            } catch (final NumberFormatException e) {
                // Left as it was until the text is a number
            }
        });
        this.labels.add(new Label(at[0], at[1] + 6, Component.translatableWithFallback(
                "screen.sgcraft.configurator." + path, label)));
    }

    private static String amount(final double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    // Shows what a setting does when the pointer rests on it
    private <W extends AbstractWidget> W tip(final W widget, final String key) {
        widget.setTooltip(Tooltip.create(ConfiguratorScreen.tipText(key)));
        return widget;
    }

    private static Component tipText(final String key) {
        return Component.translatableWithFallback("screen.sgcraft.configurator." + key + ".tip",
                ConfiguratorScreen.TIPS.getOrDefault(key, ""));
    }

    // A gate type by the name players see, as the server sent it
    private Component typeName(final String id) {
        final int index = this.state.types().indexOf(id);
        if (index >= 0 && index < this.state.typeNames().size() && !this.state.typeNames().get(index).isEmpty()) {
            final Optional<Component> name = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE,
                    JsonParser.parseString(this.state.typeNames().get(index))).result();
            if (name.isPresent()) {
                return name.get();
            }
        }
        return Component.literal(ConfiguratorScreen.shortId(id));
    }

    @Override
    protected void rebuildWidgets() {
        this.labels.clear();
        super.rebuildWidgets();
    }

    private JsonObject object(final String path) {
        final JsonElement value = this.values.get(ConfiguratorScreen.NS + path);
        return value != null && value.isJsonObject() ? value.getAsJsonObject().deepCopy() : new JsonObject();
    }

    private static JsonArray allRights() {
        final JsonArray all = new JsonArray();
        ConfiguratorScreen.RIGHTS.forEach(all::add);
        return all;
    }

    private static JsonArray withRight(final JsonArray rights, final String right, final boolean has) {
        final JsonArray copy = rights.deepCopy();
        copy.remove(new JsonPrimitive(right));
        if (has) {
            copy.add(right);
        }
        return copy;
    }

    private static String shortId(final String id) {
        return id.startsWith(ConfiguratorScreen.NS) ? id.substring(ConfiguratorScreen.NS.length()) : id;
    }

    private void save() {
        final List<Change> changes = new ArrayList<>();
        final List<String> ids = new ArrayList<>(this.original.keySet());
        this.values.keySet().stream().filter(id -> !ids.contains(id)).forEach(ids::add);
        for (final String id : ids) {
            final JsonElement before = this.original.get(id);
            final JsonElement after = this.values.get(id);
            if (after == null && before != null) {
                changes.add(new Change(id, Optional.empty()));
            } else if (after != null && !after.equals(before)) {
                changes.add(new Change(id, Optional.of(after.toString())));
            }
        }
        final String newType = this.type.equals(this.state.gate().type()) ? "" : this.type;
        final int newChevrons = this.chevrons == this.state.chevrons() ? 0 : this.chevrons;
        final String newOrientation = this.orientation.equals(this.state.orientation()) ? "" : this.orientation;
        ClientPlatform.get().sendToServer(new ConfiguratorApply(this.state.world(), this.state.gate().pos(), newType,
                newChevrons,
                changes, this.addPlayer, false, newOrientation));
        this.addPlayer = "";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
            final float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, Component.literal(this.title.getString() + ": "
                + (this.state.gate().address().isEmpty() ? "?" : this.state.gate().address())),
                this.width / 2, 30, 0xFFFFFFFF);
        for (final Label label : this.labels) {
            graphics.text(this.font, label.text(), label.x(), label.y(), 0xFFFFFFFF);
        }
        if (this.page == Page.ACCESS) {
            final int left = this.width / 2 - 155;
            final JsonObject players = this.object("player_rights");
            final List<String> uuids = new ArrayList<>(players.keySet());
            int y = ConfiguratorScreen.TOP + 52 + 6;
            for (int row = this.scroll; row < Math.min(uuids.size(), this.scroll + ConfiguratorScreen.ROWS); row++) {
                final String uuid = uuids.get(row);
                graphics.text(this.font, this.playerNames.getOrDefault(uuid, uuid.substring(0, 8)), left, y,
                        0xFFFFFFFF);
                y += 22;
            }
            y += 30;
            for (final String address : this.object("address_rules").keySet()) {
                graphics.text(this.font, address, left, y + 2, 0xFF8FB8FF);
                y += 22;
                if (y > this.height - 60) {
                    break;
                }
            }
        }
        if (this.page == Page.ENERGY) {
            graphics.text(this.font, Component.translatableWithFallback("screen.sgcraft.configurator.energy_units",
                    "Amounts are gate energy, before distance: one is %s FE from a power unit",
                    ConfiguratorScreen.amount(this.state.fePerEnergy())), this.width / 2 - 155,
                    ConfiguratorScreen.TOP + 5 * 24 + 6,
                    0xFFA0A0A0);
            graphics.text(this.font, Component.translatableWithFallback("screen.sgcraft.configurator.reserve_note",
                    "The reserve is energy the gate holds itself, for when its power runs short"),
                    this.width / 2 - 155, ConfiguratorScreen.TOP + 5 * 24 + 18, 0xFFA0A0A0);
        }
        if (this.page == Page.OTHER) {
            final int left = this.width / 2 - 155;
            int y = ConfiguratorScreen.TOP + 6;
            final List<Property> others = this.state.properties().stream()
                    .filter(property -> !property.id().startsWith(ConfiguratorScreen.NS)).toList();
            for (int row = this.scroll; row < Math.min(others.size(), this.scroll + 8); row++) {
                graphics.text(this.font, others.get(row).id(), left, y, 0xFFFFFFFF);
                y += 24;
            }
            if (others.isEmpty()) {
                graphics.text(this.font, Component.translatableWithFallback("screen.sgcraft.configurator.no_other",
                        "No add-on has given gates any settings"), left, y, 0xFF808080);
            }
        }
        if (!this.state.message().isEmpty()) {
            graphics.centeredText(this.font, this.state.message(), this.width / 2, this.height - 44,
                    this.state.message().equals("Changes saved") ? 0xFF80FF80 : 0xFFFF6060);
        }
    }
}
