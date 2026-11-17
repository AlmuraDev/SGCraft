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
import com.almuradev.sgcraft.mod.client.tool.PddScreen;
import com.almuradev.sgcraft.mod.gate.DialStyle;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.Change;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.ConfiguratorApply;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.ConfiguratorState;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.ConfiguratorUnlinkZpm;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.Named;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.Property;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ComponentSerialization;
import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.attribute.item.Selection;
import org.inspirenxe.strata.api.control.attribute.text.TextInput;
import org.inspirenxe.strata.api.control.attribute.value.Toggle;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.control.button.ToggleButton;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.layout.GridLength;
import org.inspirenxe.strata.api.layout.GridPanel;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.layout.WrapPanel;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.text.Text;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The configurator drawn with Strata, laid out as SGCraft 2.3's MalisisCore one was: two bordered boxes side by side,
 * the values with numbers on the left, over the gate's type, chevrons, iris and the way it lies, and the Access
 * Control Systems' buttons; the switches on the right as checkboxes; Revert, Save and Close along the bottom, and the
 * gate's address between them. The remake has more settings than 2.3, so each box scrolls. Who may use the gate, the
 * rules for particular addresses, the linked ZPM devices and the settings add-ons give gates open in windows of their
 * own over it, as 2.3's lists did. It sends the same changes the standard screen does, and only what changed, on
 * Save; a setting the gate has no value of its own for shows faintly what applies instead.
 */
public final class StrataConfiguratorScreen implements Screen {

    private static final String NS = "sgcraft:";
    private static final String KEY = "screen.sgcraft.configurator.";
    // The gate's orientations, as the server names them
    private static final String STANDING = "standing";
    private static final String FACE_UP = "face_up";
    private static final String FACE_DOWN = "face_down";
    private static final List<String> RIGHTS = List.of("sgcraft:use", "sgcraft:iris", "sgcraft:configure");
    private static final float WIDTH = 500f;
    private static final float HEIGHT = 350f;
    private static final float ROW_HEIGHT = 18f;
    private static final float LIST_ROW_HEIGHT = 16f;
    private static final float NUMBER_WIDTH = 55f;
    // How far in from a box's sides its rows start, headings, notes and checkboxes alike
    private static final float INSET = 6f;
    // How many characters a line of a tooltip holds before it wraps
    private static final int TIP_WIDTH = 48;
    // Room kept round a window on a small screen
    private static final float MARGIN = 10f;

    // The dial costs' properties, each a switch and an amount, which the server's settings fill in for a gate without
    // its own
    private static final String[][] COSTS = {
            {"chevron_cost", "Chevron cost", "Per chevron"},
            {"open_cost", "Opening cost", "Opening"},
            {"tick_cost", "Tick cost", "Per tick"},
    };
    // The other energy settings, each an amount the server's settings fill in for a gate without its own
    private static final String[][] ENERGY_SETTINGS = {
            {"energy_per_naquadah", "Per naquadah"},
            {"distance_factor_multiplier", "Distance factor"},
            {"inter_world_multiplier", "Other worlds"},
    };
    // How a wormhole behaves, as switches: the property, its label and what applies without a value
    private static final String[][] TRAVEL = {
            {"one_way_travel", "One-way travel", "true"},
            {"reverse_travel_kills", "Reverse travel kills", "false"},
            {"close_from_either_end", "Close from either end", "true"},
            {"opening_damage", "Kawoosh damage", "true"},
            {"spills_fluid", "Fluid pours through", "true"},
            {"phase_shift_passes_iris", "Phase shift passes iris", "true"},
            {"iris_restores_state", "Iris shuts again after", "false"},
            {"preserve_inventory", "Iris deaths keep items", "false"},
            {"redstone_input", "Redstone works iris", "true"},
            {"redstone_output", "Redstone signal out", "true"},
            {"breach_fire", "Breach fire", "true"},
            {"breach_breaks_blocks", "Breach breaks blocks", "true"},
            {"lightning_malfunction", "Storms send it astray", "true"},
    };

    private ConfiguratorState state;
    // The gate's own values being edited, by property identifier; absent means it has none of its own
    private final Map<String, JsonElement> values = new LinkedHashMap<>();
    private final Map<String, JsonElement> original = new LinkedHashMap<>();
    // What applies to a property the gate has no value for, such as the server's value, by property identifier;
    // shown faintly
    private final Map<String, JsonElement> fallbacks = new LinkedHashMap<>();
    private final Map<String, String> playerNames = new LinkedHashMap<>();
    private String type;
    private int chevrons;
    // Whether the gate stands or lies flat, by its serialized name
    private String orientation;
    private String addPlayer = "";
    private @Nullable ComponentFactory factory;
    private @Nullable AnchorPanel root;
    private @Nullable Window window;
    private @Nullable ScrollPanel numbers;
    private @Nullable ScrollPanel switches;
    // The list window open over the configurator, if one is, drawn again when the gate's state changes
    private @Nullable Runnable dialog;
    private @Nullable ListView<String> players;
    private @Nullable ListView<String> rules;
    private @Nullable String chosenPlayer;
    private @Nullable String chosenRule;
    // The ZPM devices unlinked here since the server last sent the gate, by where they are, left out of its list
    private final Set<String> unlinkedZpms = new HashSet<>();

    public StrataConfiguratorScreen(final ConfiguratorState state) {
        this.state = state;
        this.type = state.gate().type();
        this.chevrons = state.chevrons();
        this.orientation = state.orientation();
        this.load(state);
    }

    @Override
    public Text title() {
        return SGStrata.text("screen.sgcraft.configurator", "Configurator");
    }

    private void load(final ConfiguratorState state) {
        this.unlinkedZpms.clear();
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
        this.rebuild();
    }

    @Override
    public Component<?> build(final ComponentFactory factory) {
        this.factory = factory;
        this.root = factory.anchorPanel();
        this.root.add(this.window(factory));
        return this.root.setAnchor(Anchor.FILL);
    }

    // Lays the configurator out again from the values being edited, where it was and scrolled as it was, and the list
    // window over it; after the event being handled, as it may come from a control about to be replaced
    private void rebuild() {
        if (this.factory == null || this.root == null) {
            return;
        }
        final ComponentFactory factory = this.factory;
        final AnchorPanel root = this.root;
        factory.uiThread().execute(() -> {
            final float x = this.window == null ? 0f : this.window.offsetX();
            final float y = this.window == null ? 0f : this.window.offsetY();
            final float left = this.numbers == null ? 0f : this.numbers.scrollY();
            final float right = this.switches == null ? 0f : this.switches.scrollY();
            root.clear();
            root.add(this.window(factory));
            if (this.window != null) {
                this.window.setOffset(x, y);
            }
            if (this.numbers != null) {
                this.numbers.setScroll(0f, left);
            }
            if (this.switches != null) {
                this.switches.setScroll(0f, right);
            }
            if (this.dialog != null) {
                this.dialog.run();
            }
        });
    }

    private Window window(final ComponentFactory factory) {
        final GridPanel boxes = factory.gridPanel().columns(GridLength.weighted(1f), GridLength.weighted(1f))
                .rows(GridLength.weighted(1f)).setSpacing(4f, 0f);
        boxes.add(this.numbersBox(factory), 0, 0).add(this.switchesBox(factory), 0, 1);
        final Label message = factory.label("").setTextScale(0.8f).setTextAlignment(HorizontalAlignment.CENTER);
        if (!this.state.message().isEmpty()) {
            message.setText(Text.of(this.state.message()).color(this.state.message().equals("Changes saved")
                    ? SGStrata.GREEN : SGStrata.RED));
        }
        final String address = this.state.gate().address();
        Text shown = SGStrata.text(StrataConfiguratorScreen.KEY + "address", "Address: %s",
                address.isEmpty() ? "?" : address).color(SGStrata.BLUE);
        final Optional<Address> parsed = Address.parse(address);
        if (parsed.isPresent()) {
            shown = shown.append("  ").append(SGStrata.glyphs(parsed.get(), this.style()));
        }
        // The gate's address, with what the server last said over it, between the buttons, all in the bar's middle
        final StackPanel footer = factory.stackPanel().setSpacing(1f);
        if (!this.state.message().isEmpty()) {
            footer.add(message.setAlignment(HorizontalAlignment.CENTER, VerticalAlignment.TOP));
        }
        footer.add(factory.label(shown).setAlignment(HorizontalAlignment.CENTER, VerticalAlignment.TOP));
        final AnchorPanel bar = SGStrata.bar(factory)
                .add(footer.setAnchor(Anchor.CENTER))
                .add(SGStrata.button(factory, SGStrata.text(StrataConfiguratorScreen.KEY + "revert", "Revert"), 50f,
                        () -> this.update(this.state)).setAnchor(Anchor.MIDDLE_LEFT))
                .add(SGStrata.button(factory, SGStrata.text(StrataConfiguratorScreen.KEY + "save", "Save"), 50f,
                        this::save).setAnchor(Anchor.MIDDLE_RIGHT).setMargin(0f, 0f, 54f, 0f))
                .add(SGStrata.button(factory, SGStrata.text("screen.sgcraft.close", "Close"), 50f,
                        StrataHost::close).setAnchor(Anchor.MIDDLE_RIGHT));
        final AnchorPanel content = factory.anchorPanel()
                .add(boxes.setAnchor(Anchor.FILL).setMargin(0f, 0f, 0f, SGStrata.BAR_HEIGHT))
                .add(bar);
        this.window = SGStrata.window(factory, this.title(), content, StrataConfiguratorScreen.WIDTH,
                StrataConfiguratorScreen.HEIGHT);
        return this.window;
    }

    // The left box: 2.3's numeric values, its gate type radio buttons and its Access Control Systems
    private AnchorPanel numbersBox(final ComponentFactory factory) {
        final StackPanel rows = factory.stackPanel().setSpacing(2f);
        rows.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
        rows.add(this.heading(factory, "gate", "Gate"));
        rows.add(this.number(factory, "open_ticks", "Open seconds", 20, false));
        rows.add(this.decimal(factory, "ring_speed", "Ring speed", false));
        rows.add(this.decimal(factory, "dhd_range", "DHD reach", false));
        rows.add(this.heading(factory, "energy", "Energy"));
        rows.add(this.number(factory, "energy_capacity", "Reserve", 1, true));
        for (final String[] cost : StrataConfiguratorScreen.COSTS) {
            rows.add(this.decimal(factory, cost[0], cost[2], true));
        }
        for (final String[] setting : StrataConfiguratorScreen.ENERGY_SETTINGS) {
            rows.add(this.decimal(factory, setting[0], setting[1], true));
        }
        rows.add(this.note(factory, SGStrata.text(StrataConfiguratorScreen.KEY + "energy_units",
                "Amounts are gate energy, before distance: one is worth %s FE from other mods",
                StrataConfiguratorScreen.amount(this.state.fePerEnergy()))));
        rows.add(this.note(factory, SGStrata.text(StrataConfiguratorScreen.KEY + "reserve_note",
                "The reserve is energy the gate holds itself, for when its power runs short")));
        rows.add(this.heading(factory, "travel", "Travel"));
        rows.add(this.decimal(factory, "opening_damage_amount", "Kawoosh harm", false));
        rows.add(this.decimal(factory, "breach_explosion", "Breach blast", false));

        // A gate is exactly one type, so its type, chevrons, iris and the way it lies are radio buttons, as 2.3's
        rows.add(this.section(factory, SGStrata.text(StrataConfiguratorScreen.KEY + "gate_style", "Gate Style")));
        final boolean free = this.state.fitFreely();
        rows.add(this.choice(factory, SGStrata.text(StrataConfiguratorScreen.KEY + "type", "Type"),
                this.state.types(), this::typeName, this.type, free, this.upgradeTip("type"),
                value -> this.type = value));
        final List<Integer> counts = List.of(7, 8, 9);
        // An Ori supergate always has nine chevrons' reach
        rows.add(this.choice(factory, SGStrata.text(StrataConfiguratorScreen.KEY + "chevrons", "Chevrons"), counts,
                count -> Text.of(String.valueOf(count)), counts.contains(this.chevrons) ? this.chevrons : 9,
                free && !this.state.gate().type().equals("sgcraft:ori"), this.upgradeTip("chevrons"),
                value -> this.chevrons = value));
        // No iris, or one of the kinds the gate's type takes, such as the metal iris or a shield
        final List<String> irises = new ArrayList<>();
        irises.add("");
        irises.addAll(this.state.irisTypes());
        final String iris = this.values.get(StrataConfiguratorScreen.NS + "iris") instanceof final JsonPrimitive fitted
                ? fitted.getAsString() : "";
        // A type that takes no iris, as an Ori supergate, has nothing to choose
        rows.add(this.choice(factory, SGStrata.text(StrataConfiguratorScreen.KEY + "iris", "Iris"), irises,
                this::irisName, irises.contains(iris) ? iris : "", free && !this.state.irisTypes().isEmpty(),
                this.upgradeTip("iris"), value -> {
                    if (value.isEmpty()) {
                        this.values.remove(StrataConfiguratorScreen.NS + "iris");
                    } else {
                        this.values.put(StrataConfiguratorScreen.NS + "iris", new JsonPrimitive(value));
                    }
                }));
        // Face up or face down for a gate lying flat, as the original's checkboxes; shown but not set for one standing
        final boolean flat = !StrataConfiguratorScreen.STANDING.equals(this.state.orientation());
        final List<String> ways = flat ? List.of(StrataConfiguratorScreen.FACE_UP, StrataConfiguratorScreen.FACE_DOWN)
                : List.of(StrataConfiguratorScreen.STANDING);
        rows.add(this.choice(factory, SGStrata.text(StrataConfiguratorScreen.KEY + "orientation", "Orientation"),
                ways,
                StrataConfiguratorScreen::orientationName, ways.contains(this.orientation) ? this.orientation
                        : ways.getFirst(), flat, StrataConfiguratorScreen.tip("orientation"),
                value -> this.orientation = value));
        // Whether world generation built the gate: a fact about it, shown but not set here
        final Label generated = factory.label(SGStrata.text(StrataConfiguratorScreen.KEY + "generated", "Built by: %s",
                this.bool("generated", false)
                        ? SGStrata.text(StrataConfiguratorScreen.KEY + "generated.world", "World").plain()
                        : SGStrata.text(StrataConfiguratorScreen.KEY + "generated.player", "Player").plain())
                .color(SGStrata.GRAY));
        generated.setTooltip(StrataConfiguratorScreen.tip("generated"));
        rows.add(generated.setMargin(StrataConfiguratorScreen.INSET, 2f, StrataConfiguratorScreen.INSET, 0f));

        // 2.3's Access Control Systems: its lists, each in a window of its own, under the gate's owner
        rows.add(this.section(factory, SGStrata.text(StrataConfiguratorScreen.KEY + "access_control",
                "Access Control Systems")));
        rows.add(factory.label(this.state.owner()
                .map(owner -> SGStrata.text(StrataConfiguratorScreen.KEY + "owner", "Owner: %s", owner.name()))
                .orElseGet(() -> SGStrata.text(StrataConfiguratorScreen.KEY + "no_owner",
                        "Owner: none, so operators manage it"))).setMargin(StrataConfiguratorScreen.INSET, 0f,
                StrataConfiguratorScreen.INSET, 2f));
        rows.add(this.centred(SGStrata.button(factory, SGStrata.text(StrataConfiguratorScreen.KEY + "address_list",
                "Gate Address Access List"), 160f, this::openRules)));
        rows.add(this.centred(SGStrata.button(factory, SGStrata.text(StrataConfiguratorScreen.KEY + "player_list",
                "Player Access List"), 160f, this::openPlayers)));
        final Button zpms = SGStrata.button(factory, SGStrata.text(StrataConfiguratorScreen.KEY + "zpm_links",
                "Linked ZPMs: %s", this.state.zpmLinks().size()), 160f, this::openZpms);
        zpms.setTooltip(this.zpmTip());
        rows.add(this.centred(zpms));
        rows.add(this.centred(SGStrata.button(factory, SGStrata.text(StrataConfiguratorScreen.KEY + "other_list",
                "Add-on Settings"), 160f, this::openOthers).setTooltip(StrataConfiguratorScreen.tip("other"))));
        final Button serverCosts = SGStrata.button(factory, SGStrata.text(StrataConfiguratorScreen.KEY
                + "server_costs", "Use server's costs"), 160f, () -> {
                    for (final String[] cost : StrataConfiguratorScreen.COSTS) {
                        this.values.remove(StrataConfiguratorScreen.NS + cost[0] + "_enabled");
                        this.values.remove(StrataConfiguratorScreen.NS + cost[0]);
                    }
                    for (final String[] setting : StrataConfiguratorScreen.ENERGY_SETTINGS) {
                        this.values.remove(StrataConfiguratorScreen.NS + setting[0]);
                    }
                    this.rebuild();
                });
        serverCosts.setTooltip(StrataConfiguratorScreen.tip("server_costs"));
        rows.add(this.centred(this.operatorsOnly(serverCosts)).setMargin(0f, 0f, 0f, 4f));
        this.numbers = factory.scrollPanel().add(rows);
        return this.box(factory, SGStrata.text(StrataConfiguratorScreen.KEY + "numeric_values", "Numeric Values"),
                this.numbers);
    }

    // The right box: 2.3's true or false values, as checkboxes
    private AnchorPanel switchesBox(final ComponentFactory factory) {
        final StackPanel rows = factory.stackPanel().setSpacing(3f);
        rows.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
        rows.add(this.heading(factory, "gate", "Gate"));
        rows.add(this.toggle(factory, "address_visible", "Address shown", true));
        // Who may break the gate is for those who manage it
        rows.add(this.toggle(factory, "breakable", "Breakable", false).setEnabled(this.state.manage()));
        // Only operators and creative players, who also manage the gate, make it invulnerable here
        rows.add(this.toggle(factory, "invulnerable", "Invulnerable", false)
                .setEnabled(this.state.manage() && this.state.fitFreely()));
        rows.add(this.toggle(factory, "chevrons_lock_on_dial", "Chevrons lock at once", false));
        rows.add(this.toggle(factory, "chevrons_lock_as_dialed", "Chevrons lock on dial", false));
        rows.add(this.toggle(factory, "horizon_transparent", "See-through horizon", true));
        rows.add(this.heading(factory, "energy", "Energy"));
        // What the gate pays, which only operators change; anyone else sees it but can't change it
        rows.add(this.operatorsOnly(this.toggle(factory, "requires_power", "Needs energy", true)));
        for (final String[] cost : StrataConfiguratorScreen.COSTS) {
            rows.add(this.operatorsOnly(this.toggle(factory, cost[0] + "_enabled", cost[1], true)));
        }
        rows.add(this.toggle(factory, "dhd_fuel", "DHD fuel powers it", true));
        rows.add(this.heading(factory, "travel", "Travel"));
        for (final String[] setting : StrataConfiguratorScreen.TRAVEL) {
            rows.add(this.toggle(factory, setting[0], setting[1], Boolean.parseBoolean(setting[2])));
        }
        rows.add(this.heading(factory, "access", "Access"));
        rows.add(this.toggle(factory, "allow_incoming", "Incoming allowed", true));
        rows.add(this.toggle(factory, "allow_outgoing", "Outgoing allowed", true)
                .setMargin(StrataConfiguratorScreen.INSET, 0f, 0f, 4f));
        this.switches = factory.scrollPanel().add(rows);
        return this.box(factory, SGStrata.text(StrataConfiguratorScreen.KEY + "boolean_values", "Boolean Values"),
                this.switches);
    }

    // A bordered box with its name at the top over a line, as 2.3's, holding a panel that scrolls
    private AnchorPanel box(final ComponentFactory factory, final Text title, final ScrollPanel scroll) {
        final AnchorPanel box = SGStrata.box(factory)
                .add(factory.label(title.color(SGStrata.WHITE)).setAnchor(Anchor.TOP_CENTER).setMargin(0f, 1f, 0f, 0f))
                .add(SGStrata.line(factory, 6f, 12f, 6f))
                .add(scroll.setAnchor(Anchor.FILL).setMargin(0f, 15f, 0f, 0f));
        box.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.STRETCH);
        return box;
    }

    // A part of a box, as 2.3's gate style and access control systems: its name, centred over a line
    private Component<?> section(final ComponentFactory factory, final Text title) {
        final AnchorPanel section = factory.anchorPanel()
                .add(factory.label(title.color(SGStrata.WHITE)).setAnchor(Anchor.TOP_CENTER).setMargin(0f, 6f, 0f, 0f))
                .add(SGStrata.line(factory, 6f, 17f, 6f));
        section.setHeight(20f);
        return section.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
    }

    // A group of settings within a box, named faintly, as the standard screen's pages
    private Component<?> heading(final ComponentFactory factory, final String page, final String fallback) {
        return factory.label(SGStrata.text(StrataConfiguratorScreen.KEY + page, fallback).color(SGStrata.GOLD))
                .setTextScale(0.8f).setMargin(StrataConfiguratorScreen.INSET, 3f, StrataConfiguratorScreen.INSET, 0f);
    }

    // Small grey words under a group of settings
    private Component<?> note(final ComponentFactory factory, final Text text) {
        return factory.label(text.color(SGStrata.GRAY)).setTextScale(0.7f).setWrap(true)
                .setMargin(StrataConfiguratorScreen.INSET, 0f, StrataConfiguratorScreen.INSET, 0f);
    }

    private <C extends Component<C>> C centred(final C component) {
        return component.setAlignment(HorizontalAlignment.CENTER, VerticalAlignment.TOP);
    }

    // A whole number, shown divided by {@code scale}, such as ticks shown as seconds; left empty, the fallback
    // applies, and shows faintly in the box
    private Component<?> number(final ComponentFactory factory, final String path, final String label,
            final long scale, final boolean operatorsOnly) {
        final String id = StrataConfiguratorScreen.NS + path;
        final TextBox box = factory.textBox().setMaxLength(18);
        box.setText(this.values.containsKey(id) ? String.valueOf(this.values.get(id).getAsLong() / scale) : "");
        if (this.fallbacks.containsKey(id)) {
            box.setPlaceholder(Text.of(String.valueOf(this.fallbacks.get(id).getAsLong() / scale))
                    .color(SGStrata.DARK_GRAY));
        }
        box.onEvent(TextInput.EDITED, event -> {
            final String text = event.value().replaceAll("[^0-9]", "");
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
        return this.valueRow(factory, path, label, box, operatorsOnly);
    }

    // An amount that may have a fraction; left empty, the fallback applies, and shows faintly in the box
    private Component<?> decimal(final ComponentFactory factory, final String path, final String label,
            final boolean operatorsOnly) {
        final String id = StrataConfiguratorScreen.NS + path;
        final TextBox box = factory.textBox().setMaxLength(18);
        box.setText(this.values.containsKey(id) ? StrataConfiguratorScreen.amount(this.values.get(id).getAsDouble())
                : "");
        if (this.fallbacks.containsKey(id)) {
            box.setPlaceholder(Text.of(StrataConfiguratorScreen.amount(this.fallbacks.get(id).getAsDouble()))
                    .color(SGStrata.DARK_GRAY));
        }
        box.onEvent(TextInput.EDITED, event -> {
            final String text = event.value().replaceAll("[^0-9.]", "");
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
        return this.valueRow(factory, path, label, box, operatorsOnly);
    }

    // A value's name on the left of its row and its box on the right, as 2.3's
    private Component<?> valueRow(final ComponentFactory factory, final String path, final String label,
            final TextBox box, final boolean operatorsOnly) {
        box.setSize(StrataConfiguratorScreen.NUMBER_WIDTH, SGStrata.BUTTON_HEIGHT);
        Tooltip tip = StrataConfiguratorScreen.tip(path);
        if (operatorsOnly && !this.state.operator()) {
            box.setReadOnly(true).setEnabled(false);
            tip = StrataConfiguratorScreen.operatorsTip();
        }
        box.setTooltip(tip);
        final Label name = factory.label(SGStrata.text(StrataConfiguratorScreen.KEY + path, label).append(":"));
        name.setTooltip(tip);
        final AnchorPanel row = factory.anchorPanel()
                .add(name.setAnchor(Anchor.MIDDLE_LEFT).setMargin(StrataConfiguratorScreen.INSET, 0f, 0f, 0f))
                .add(box.setAnchor(Anchor.MIDDLE_RIGHT).setMargin(0f, 0f, StrataConfiguratorScreen.INSET, 0f));
        row.setHeight(StrataConfiguratorScreen.ROW_HEIGHT);
        return row.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
    }

    // A switch as a checkbox, its caption grey while it shows what applies without a value of the gate's own
    private CheckBox toggle(final ComponentFactory factory, final String path, final String label,
            final boolean fallback) {
        final String id = StrataConfiguratorScreen.NS + path;
        final Text caption = SGStrata.text(StrataConfiguratorScreen.KEY + path, label);
        final CheckBox box = SGStrata.checkBox(factory, caption.color(this.values.containsKey(id) ? SGStrata.WHITE
                : SGStrata.GRAY)).setSelected(this.bool(path, fallback));
        box.setTooltip(StrataConfiguratorScreen.tip(path));
        box.onEvent(Toggle.TOGGLED, event -> {
            this.values.put(id, new JsonPrimitive(event.value()));
            // Its own now, so no longer faint
            box.setText(caption.color(SGStrata.WHITE));
        });
        return box.setMargin(StrataConfiguratorScreen.INSET, 0f, 0f, 0f);
    }

    // One of several values, as a selector after a name: its options side by side as one row of buttons, the chosen
    // one held down, which wraps onto a further line when it doesn't fit
    private <T> Component<?> choice(final ComponentFactory factory, final Text label, final List<T> options,
            final Function<T, Text> name, final T chosen, final boolean enabled, final Tooltip tip,
            final Consumer<T> choose) {
        final WrapPanel row = factory.wrapPanel(Orientation.HORIZONTAL).setSpacing(6f, 3f);
        final Label caption = factory.label(label.append(":"));
        caption.setTooltip(tip);
        row.add(caption.setAlignment(HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE));
        final StackPanel selector = factory.stackPanel(Orientation.HORIZONTAL).setSpacing(0f);
        final List<ToggleButton> buttons = new ArrayList<>();
        for (final T option : options) {
            final ToggleButton button = factory.toggleButton(name.apply(option)).setSelected(option.equals(chosen));
            button.setTextPadding(6f, 0f);
            button.setMinSize(0f, 0f);
            button.setHeight(SGStrata.BUTTON_HEIGHT);
            button.setEnabled(enabled);
            button.setTooltip(tip);
            // The chosen one stays down: another is chosen by pressing it, not by letting this one up
            button.onEvent(Toggle.TOGGLE, event -> {
                if (!event.value()) {
                    event.setValue(true);
                }
            });
            button.onEvent(Toggle.TOGGLED, event -> {
                if (!event.value()) {
                    return;
                }
                for (final ToggleButton other : buttons) {
                    if (other != button) {
                        other.setSelected(false);
                    }
                }
                choose.accept(option);
            });
            buttons.add(button);
            selector.add(button);
        }
        row.add(selector);
        return row.setMargin(StrataConfiguratorScreen.INSET, 3f, StrataConfiguratorScreen.INSET, 0f)
                .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
    }

    // A setting only operators change: shown to anyone else, with a tip saying so, but not changed
    private <C extends Component<C>> C operatorsOnly(final C component) {
        if (!this.state.operator()) {
            component.setEnabled(false);
            if (component instanceof final CheckBox box) {
                box.setTooltip(StrataConfiguratorScreen.operatorsTip());
            } else if (component instanceof final Button button) {
                button.setTooltip(StrataConfiguratorScreen.operatorsTip());
            }
        }
        return component;
    }

    private static Tooltip operatorsTip() {
        return StrataConfiguratorScreen.lines(SGStrata.text(StrataConfiguratorScreen.KEY + "operators_only",
                "Only operators change what a gate pays for dialing"));
    }

    // What a setting upgrade items change does, or, for a player the server doesn't let change it here, which item
    // does
    private Tooltip upgradeTip(final String key) {
        return StrataConfiguratorScreen.tip(this.state.fitFreely() ? key : key + ".locked");
    }

    // What a setting does, as the standard screen's tips say it
    private static Tooltip tip(final String key, final Object... args) {
        return StrataConfiguratorScreen.lines(SGStrata.text(StrataConfiguratorScreen.KEY + key + ".tip", "", args));
    }

    // A tooltip of text broken into lines, as Strata draws a tip on one line until told otherwise
    private static Tooltip lines(final Text text) {
        final List<Text> lines = new ArrayList<>();
        for (final String paragraph : text.plain().split("\n")) {
            final StringBuilder line = new StringBuilder();
            for (final String word : paragraph.split(" ")) {
                if (!line.isEmpty() && line.length() + word.length() + 1 > StrataConfiguratorScreen.TIP_WIDTH) {
                    lines.add(Text.of(line.toString()));
                    line.setLength(0);
                }
                if (!line.isEmpty()) {
                    line.append(' ');
                }
                line.append(word);
            }
            lines.add(Text.of(line.toString()));
        }
        return Tooltip.lines(lines);
    }

    // The ZPM devices linked to the gate; they are linked and unlinked at the devices
    private Tooltip zpmTip() {
        final List<String> links = this.state.zpmLinks();
        return StrataConfiguratorScreen.lines(links.isEmpty()
                ? SGStrata.text(StrataConfiguratorScreen.KEY + "zpm_links.none", "No ZPM device is linked to this "
                        + "gate. Sneak-use a ZPM console, cart or hub, then use the gate, to link one")
                : SGStrata.text(StrataConfiguratorScreen.KEY + "zpm_links.tip", "ZPM devices the gate draws on from "
                        + "anywhere in its world. Sneak-use one twice to unlink it:"));
    }

    // The gate's own value of a switch, or what applies without one
    private boolean bool(final String path, final boolean fallback) {
        final String id = StrataConfiguratorScreen.NS + path;
        return this.values.containsKey(id) ? this.values.get(id).getAsBoolean()
                : this.fallbacks.containsKey(id) ? this.fallbacks.get(id).getAsBoolean() : fallback;
    }

    // The rights of players the gate doesn't list: its own, or what applies without them
    private JsonArray defaultRights() {
        final String id = StrataConfiguratorScreen.NS + "default_rights";
        final JsonElement rights = this.values.containsKey(id) ? this.values.get(id) : this.fallbacks.get(id);
        return rights != null && rights.isJsonArray() ? rights.getAsJsonArray().deepCopy() : new JsonArray();
    }

    private JsonObject object(final String path) {
        final JsonElement value = this.values.get(StrataConfiguratorScreen.NS + path);
        return value != null && value.isJsonObject() ? value.getAsJsonObject().deepCopy() : new JsonObject();
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
        return id.startsWith(StrataConfiguratorScreen.NS) ? id.substring(StrataConfiguratorScreen.NS.length()) : id;
    }

    // To 2 decimal places at most, so a long fraction (a Pegasus gate's 7.142857... ring speed) fits its box; shown
    // only, as the box is filled before it listens for typing, so the value the gate has is kept until it's changed
    private static String amount(final double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static Text orientationName(final String orientation) {
        final String fallback = switch (orientation) {
            case StrataConfiguratorScreen.FACE_UP -> "Flat, face up";
            case StrataConfiguratorScreen.FACE_DOWN -> "Flat, face down";
            default -> "Upright";
        };
        return SGStrata.text(StrataConfiguratorScreen.KEY + "orientation." + orientation, fallback);
    }

    // A gate type or kind of iris by the name players see, as the server sent it
    private Text typeName(final String id) {
        return StrataConfiguratorScreen.named(id, this.state.types(), this.state.typeNames());
    }

    private Text irisName(final String id) {
        if (id.isEmpty()) {
            return SGStrata.text(StrataConfiguratorScreen.KEY + "iris.none", "None");
        }
        return StrataConfiguratorScreen.named(id, this.state.irisTypes(), this.state.irisNames());
    }

    private static Text named(final String id, final List<String> ids, final List<String> names) {
        final int index = ids.indexOf(id);
        if (index >= 0 && index < names.size() && !names.get(index).isEmpty()) {
            final Optional<net.minecraft.network.chat.Component> name = ComponentSerialization.CODEC
                    .parse(JsonOps.INSTANCE, JsonParser.parseString(names.get(index))).result();
            if (name.isPresent()) {
                return Text.of(name.get().getString());
            }
        }
        return Text.of(StrataConfiguratorScreen.shortId(id));
    }

    private DialStyle style() {
        return PddScreen.style(this.state.gate());
    }

    // Sends only what changed, as the standard screen does, with a player to list if one was named
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
                newChevrons, changes, this.addPlayer, false, newOrientation));
        this.addPlayer = "";
    }

    // The list windows, over the configurator as 2.3's screens over it were

    // Opens a window over the configurator whose content is drawn again as the gate's state changes, until it closes
    private void openDialog(final Text title, final float width, final float height,
            final Function<Runnable, Component<?>> body) {
        if (this.factory == null) {
            return;
        }
        final ComponentFactory factory = this.factory;
        final AnchorPanel holder = factory.anchorPanel();
        final Overlay[] shown = new Overlay[1];
        final Runnable close = () -> {
            this.dialog = null;
            this.players = null;
            this.rules = null;
            shown[0].close();
        };
        holder.add(body.apply(close).setAnchor(Anchor.FILL));
        this.dialog = () -> holder.clear().add(body.apply(close).setAnchor(Anchor.FILL));
        shown[0] = factory.overlays().dialog(StrataConfiguratorScreen.dialogWindow(factory, title, holder, width,
                height, close));
    }

    private static Window dialogWindow(final ComponentFactory factory, final Text title, final Component<?> content,
            final float width, final float height, final Runnable close) {
        final Minecraft minecraft = Minecraft.getInstance();
        return SGStrata.closesWith(factory.window(title).setMovable(true).setResizable(false).setCollapsible(false)
                .setMaximizable(false).add(content.setAnchor(Anchor.FILL))
                .setSize(Math.min(width, minecraft.getWindow().getGuiScaledWidth() - StrataConfiguratorScreen.MARGIN),
                        Math.min(height, minecraft.getWindow().getGuiScaledHeight() - StrataConfiguratorScreen.MARGIN))
                .setAnchor(Anchor.CENTER), close);
    }

    // A list's box, as 2.3's: its name at the top, the list under it, and the + and - buttons below
    private AnchorPanel listBox(final ComponentFactory factory, final Text title, final ListView<String> list,
            final Button add, final Button remove) {
        final StackPanel buttons = factory.stackPanel(Orientation.HORIZONTAL).setSpacing(2f).add(add).add(remove);
        final AnchorPanel box = SGStrata.box(factory)
                .add(factory.label(title.color(SGStrata.WHITE)).setAnchor(Anchor.TOP_CENTER).setMargin(0f, 1f, 0f, 0f))
                .add(SGStrata.box(factory).add(list.setAnchor(Anchor.FILL)).setAnchor(Anchor.FILL)
                        .setMargin(0f, 13f, 0f, SGStrata.SMALL_HEIGHT + 4f))
                .add(buttons.setAnchor(Anchor.BOTTOM_LEFT).setMargin(1f, 0f, 0f, 1f));
        box.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.STRETCH);
        return box;
    }

    // The box beside a list, with what can be set for its chosen entry
    private AnchorPanel optionsBox(final ComponentFactory factory, final AnchorPanel options) {
        final AnchorPanel box = SGStrata.box(factory)
                .add(factory.label(SGStrata.text(StrataConfiguratorScreen.KEY + "options", "Options")
                        .color(SGStrata.WHITE)).setAnchor(Anchor.TOP_CENTER).setMargin(0f, 1f, 0f, 0f))
                .add(SGStrata.line(factory, 6f, 12f, 6f))
                .add(options.setAnchor(Anchor.FILL).setMargin(0f, 16f, 0f, 0f));
        box.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.STRETCH);
        return box;
    }

    // One row of a list, as 2.3's: black, and grey when chosen
    private Component<?> listRow(final ComponentFactory factory, final @Nullable ListView<String> list,
            final String item, final Text text, final @Nullable Text right) {
        final boolean chosen = list != null && list.selected().filter(item::equals).isPresent();
        final Label label = factory.label(text);
        label.setHitTestVisible(false);
        final AnchorPanel row = factory.anchorPanel().add(label.setAnchor(Anchor.MIDDLE_LEFT)
                .setMargin(3f, 0f, 0f, 0f));
        if (right != null) {
            final Label glyphs = factory.label(right);
            glyphs.setHitTestVisible(false);
            row.add(glyphs.setAnchor(Anchor.MIDDLE_RIGHT).setMargin(0f, 0f, 3f, 0f));
        }
        return SGStrata.row(row, chosen).setAnchor(Anchor.FILL)
                .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.STRETCH);
    }

    // A linked ZPM device: where it is and what it holds, with a red x that unlinks it for whoever manages the gate
    private Component<?> zpmRow(final ComponentFactory factory, final String link) {
        final Label label = factory.label(Text.of(link).color(SGStrata.WHITE));
        label.setHitTestVisible(false);
        final AnchorPanel row = factory.anchorPanel().add(label.setAnchor(Anchor.MIDDLE_LEFT)
                .setMargin(3f, 0f, 0f, 0f));
        final Optional<BlockPos> device = StrataConfiguratorScreen.zpmPos(link);
        if (this.state.manage() && device.isPresent()) {
            row.add(SGStrata.small(factory, "x", SGStrata.RED, SGStrata.text(StrataConfiguratorScreen.KEY
                    + "zpm_links.unlink", "Unlink this ZPM device"), () -> this.unlinkZpm(link, device.get()))
                    .setAnchor(Anchor.MIDDLE_RIGHT).setMargin(0f, 0f, 2f, 0f));
        }
        return SGStrata.row(row, false).setAnchor(Anchor.FILL)
                .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.STRETCH);
    }

    // Asks the server to unlink a device and leaves it out of the list straight away, keeping any unsaved changes
    private void unlinkZpm(final String link, final BlockPos device) {
        ClientPlatform.get().sendToServer(new ConfiguratorUnlinkZpm(this.state.world(), this.state.gate().pos(),
                device));
        this.unlinkedZpms.add(StrataConfiguratorScreen.zpmAt(link));
        if (this.factory != null) {
            // After the press, as the list it came from is about to be replaced
            this.factory.uiThread().execute(() -> {
                if (this.dialog != null) {
                    this.dialog.run();
                }
            });
        }
    }

    // Where a linked device is, as the server writes it at the start of its line: "x, y, z: what it holds"
    private static String zpmAt(final String link) {
        final int colon = link.indexOf(':');
        return colon < 0 ? link : link.substring(0, colon);
    }

    private static Optional<BlockPos> zpmPos(final String link) {
        final String[] parts = StrataConfiguratorScreen.zpmAt(link).split(",");
        if (parts.length != 3) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BlockPos(Integer.parseInt(parts[0].strip()), Integer.parseInt(parts[1].strip()),
                    Integer.parseInt(parts[2].strip())));
        } catch (final NumberFormatException e) {
            return Optional.empty();
        }
    }

    // 2.3's Save and Close at the bottom right of a list window
    private AnchorPanel withButtons(final ComponentFactory factory, final AnchorPanel body, final Runnable close) {
        return this.withButtons(factory, body, close, null);
    }

    // The same, with something at the left of the bar the buttons are in, centred in it as they are
    private AnchorPanel withButtons(final ComponentFactory factory, final AnchorPanel body, final Runnable close,
            final @Nullable Component<?> left) {
        final AnchorPanel bar = SGStrata.bar(factory)
                .add(SGStrata.button(factory, SGStrata.text(StrataConfiguratorScreen.KEY + "save", "Save"), 50f,
                        this::save).setAnchor(Anchor.MIDDLE_RIGHT).setMargin(0f, 0f, 54f, 0f))
                .add(SGStrata.button(factory, SGStrata.text("screen.sgcraft.close", "Close"), 50f, close)
                        .setAnchor(Anchor.MIDDLE_RIGHT));
        if (left != null) {
            bar.add(left.setAnchor(Anchor.MIDDLE_LEFT).setMargin(4f, 0f, 0f, 0f));
        }
        return body.add(bar);
    }

    // The player access list: the players the gate lists on the left, the rights of the one chosen on the right, and
    // the rights of everyone else below, as 2.3's; only the owner or an operator changes them
    private void openPlayers() {
        this.openDialog(SGStrata.text(StrataConfiguratorScreen.KEY + "player_list", "Player Access List"), 400f,
                230f, this::playersBody);
    }

    private Component<?> playersBody(final Runnable close) {
        final ComponentFactory factory = Objects.requireNonNull(this.factory);
        final boolean manage = this.state.manage();
        final List<String> uuids = new ArrayList<>(this.object("player_rights").keySet());
        if (this.chosenPlayer != null && !uuids.contains(this.chosenPlayer)) {
            this.chosenPlayer = null;
        }
        final ListView<String> list = SGStrata.rows(factory.<String>listView(uuid -> this.listRow(factory,
                this.players, uuid, Text.of(this.playerNames.getOrDefault(uuid, uuid.substring(0, 8)))
                        .color(SGStrata.WHITE), null)), StrataConfiguratorScreen.LIST_ROW_HEIGHT);
        this.players = list;
        list.setItems(uuids);
        if (this.chosenPlayer != null) {
            list.selectItem(this.chosenPlayer);
        }
        final AnchorPanel options = factory.anchorPanel();
        this.playerOptions(factory, options);
        list.onEvent(Selection.SELECTED, event -> {
            this.chosenPlayer = list.selected().orElse(null);
            options.clear();
            this.playerOptions(factory, options);
        });
        final Button add = SGStrata.small(factory, "+", SGStrata.GREEN,
                SGStrata.text(StrataConfiguratorScreen.KEY + "add_player.tip", "List a player"), this::addPlayerDialog);
        add.setEnabled(manage);
        final Button remove = SGStrata.small(factory, "-", SGStrata.RED,
                SGStrata.text(StrataConfiguratorScreen.KEY + "remove_player.tip",
                        "Take the chosen player off the list"),
                () -> {
                    if (this.chosenPlayer != null) {
                        final JsonObject now = this.object("player_rights");
                        now.remove(this.chosenPlayer);
                        this.values.put(StrataConfiguratorScreen.NS + "player_rights", now);
                        this.chosenPlayer = null;
                        this.rebuild();
                    }
                });
        remove.setEnabled(manage);
        final GridPanel boxes = factory.gridPanel().columns(GridLength.weighted(1f), GridLength.weighted(1f))
                .rows(GridLength.weighted(1f)).setSpacing(4f, 0f);
        boxes.add(this.listBox(factory, SGStrata.text(StrataConfiguratorScreen.KEY + "players", "Players"), list, add,
                remove), 0, 0).add(this.optionsBox(factory, options), 0, 1);
        // Everyone the gate doesn't list
        final String defaultsId = StrataConfiguratorScreen.NS + "default_rights";
        final StackPanel defaults = factory.stackPanel(Orientation.HORIZONTAL).setSpacing(8f);
        for (final String right : StrataConfiguratorScreen.RIGHTS) {
            final String name = StrataConfiguratorScreen.shortId(right);
            final CheckBox box = SGStrata.checkBox(factory, this.rightName(name).color(
                    this.values.containsKey(defaultsId) ? SGStrata.WHITE : SGStrata.GRAY))
                    .setSelected(this.defaultRights().contains(new JsonPrimitive(right)));
            box.setTooltip(StrataConfiguratorScreen.tip("default_rights", SGStrata.text(StrataConfiguratorScreen.KEY
                    + "right." + name + ".tip", "").plain()));
            box.setEnabled(manage);
            box.onEvent(Toggle.TOGGLED, event -> {
                this.values.put(defaultsId, StrataConfiguratorScreen.withRight(this.defaultRights(), right,
                        event.value()));
                box.setText(this.rightName(name).color(SGStrata.WHITE));
            });
            defaults.add(box);
        }
        // Everyone else's rights in the bar beside Save and Close, so the boxes reach down to it; for anyone who
        // doesn't manage the gate, its name says why they can't be changed
        final Label everyone = factory.label(SGStrata.text(StrataConfiguratorScreen.KEY + "everyone", "Everyone else")
                .append(":"));
        if (!manage) {
            everyone.setTooltip(StrataConfiguratorScreen.lines(SGStrata.text(StrataConfiguratorScreen.KEY
                    + "manage_note", "Only the owner or an operator sees who is listed")));
        }
        final StackPanel bottom = factory.stackPanel(Orientation.HORIZONTAL).setSpacing(6f)
                .add(everyone.setAlignment(HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE))
                .add(defaults.setAlignment(HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE));
        final AnchorPanel body = factory.anchorPanel()
                .add(boxes.setAnchor(Anchor.FILL).setMargin(0f, 0f, 0f, SGStrata.BAR_HEIGHT));
        return this.withButtons(factory, body, close, bottom);
    }

    // The chosen player's rights, each a checkbox
    private void playerOptions(final ComponentFactory factory, final AnchorPanel options) {
        final String uuid = this.chosenPlayer;
        if (uuid == null) {
            options.add(factory.label(SGStrata.text(StrataConfiguratorScreen.KEY + "choose_player",
                    "Choose a player").color(SGStrata.GRAY)).setAnchor(Anchor.TOP_CENTER).setMargin(0f, 4f, 0f, 0f));
            return;
        }
        final StackPanel rights = factory.stackPanel().setSpacing(4f);
        rights.add(factory.label(Text.of(this.playerNames.getOrDefault(uuid, uuid)).color(SGStrata.BLUE))
                .setMargin(4f, 2f, 4f, 2f));
        final JsonArray has = this.object("player_rights").get(uuid).getAsJsonArray();
        for (final String right : StrataConfiguratorScreen.RIGHTS) {
            final String name = StrataConfiguratorScreen.shortId(right);
            final CheckBox box = SGStrata.checkBox(factory, this.rightName(name))
                    .setSelected(has.contains(new JsonPrimitive(right)));
            box.setTooltip(StrataConfiguratorScreen.tip("right." + name));
            box.setEnabled(this.state.manage());
            box.onEvent(Toggle.TOGGLED, event -> {
                final JsonObject now = this.object("player_rights");
                now.add(uuid, StrataConfiguratorScreen.withRight(now.get(uuid).getAsJsonArray(), right,
                        event.value()));
                this.values.put(StrataConfiguratorScreen.NS + "player_rights", now);
            });
            rights.add(box.setMargin(StrataConfiguratorScreen.INSET, 0f, 0f, 0f));
        }
        options.add(rights.setAnchor(Anchor.FILL));
    }

    private Text rightName(final String right) {
        final String fallback = switch (right) {
            case "use" -> "Use";
            case "iris" -> "Iris";
            default -> "Configure";
        };
        return SGStrata.text(StrataConfiguratorScreen.KEY + "right." + right, fallback);
    }

    // 2.3's entry window for a player: their name, listed with the gate's default rights once saved
    private void addPlayerDialog() {
        if (this.factory == null) {
            return;
        }
        final ComponentFactory factory = this.factory;
        final TextBox name = factory.textBox().setMaxLength(16);
        name.setSize(160f, SGStrata.BUTTON_HEIGHT);
        name.setPlaceholder(SGStrata.text(StrataConfiguratorScreen.KEY + "player", "Player name")
                .color(SGStrata.DARK_GRAY));
        name.setTooltip(StrataConfiguratorScreen.tip("player"));
        final Overlay[] shown = new Overlay[1];
        final Runnable list = () -> {
            if (!name.text().isBlank()) {
                this.addPlayer = name.text().strip();
                this.save();
            }
            shown[0].close();
        };
        name.onEvent(TextBox.SUBMITTED, event -> list.run());
        shown[0] = factory.overlays().dialog(this.entryWindow(factory, SGStrata.text(StrataConfiguratorScreen.KEY
                + "add_player", "List player"), SGStrata.text(StrataConfiguratorScreen.KEY + "player",
                "Player name"), name, list, () -> shown[0].close()));
    }

    // The gate address access list: the addresses with rules on the left, whether the chosen one may dial in and be
    // dialed on the right
    private void openRules() {
        this.openDialog(SGStrata.text(StrataConfiguratorScreen.KEY + "address_list", "Gate Address Access List"),
                360f, 210f, this::rulesBody);
    }

    private Component<?> rulesBody(final Runnable close) {
        final ComponentFactory factory = Objects.requireNonNull(this.factory);
        final List<String> addresses = new ArrayList<>(this.object("address_rules").keySet());
        if (this.chosenRule != null && !addresses.contains(this.chosenRule)) {
            this.chosenRule = null;
        }
        final ListView<String> list = SGStrata.rows(factory.<String>listView(address -> this.listRow(factory,
                this.rules, address, Text.of(address).color(SGStrata.BLUE),
                Address.parse(address).map(parsed -> SGStrata.glyphs(parsed, this.style())).orElse(null))),
                StrataConfiguratorScreen.LIST_ROW_HEIGHT);
        this.rules = list;
        list.setItems(addresses);
        if (this.chosenRule != null) {
            list.selectItem(this.chosenRule);
        }
        final AnchorPanel options = factory.anchorPanel();
        this.ruleOptions(factory, options);
        list.onEvent(Selection.SELECTED, event -> {
            this.chosenRule = list.selected().orElse(null);
            options.clear();
            this.ruleOptions(factory, options);
        });
        final Button add = SGStrata.small(factory, "+", SGStrata.GREEN,
                SGStrata.text(StrataConfiguratorScreen.KEY + "add_rule", "Add rule"), this::addRuleDialog);
        add.setTooltip(StrataConfiguratorScreen.tip("rule"));
        final Button remove = SGStrata.small(factory, "-", SGStrata.RED,
                SGStrata.text(StrataConfiguratorScreen.KEY + "remove_rule.tip", "Remove the chosen address's rule"),
                () -> {
                    if (this.chosenRule != null) {
                        final JsonObject now = this.object("address_rules");
                        now.remove(this.chosenRule);
                        this.values.put(StrataConfiguratorScreen.NS + "address_rules", now);
                        this.chosenRule = null;
                        this.rebuild();
                    }
                });
        final GridPanel boxes = factory.gridPanel().columns(GridLength.weighted(1.3f), GridLength.weighted(1f))
                .rows(GridLength.weighted(1f)).setSpacing(4f, 0f);
        boxes.add(this.listBox(factory, SGStrata.text(StrataConfiguratorScreen.KEY + "addresses", "Addresses"), list,
                add, remove), 0, 0).add(this.optionsBox(factory, options), 0, 1);
        final AnchorPanel body = factory.anchorPanel().add(boxes.setAnchor(Anchor.FILL).setMargin(0f, 0f, 0f,
                SGStrata.BAR_HEIGHT));
        return this.withButtons(factory, body, close);
    }

    // Whether the chosen address's gate may dial this one, and whether this one may dial it
    private void ruleOptions(final ComponentFactory factory, final AnchorPanel options) {
        final String address = this.chosenRule;
        if (address == null) {
            options.add(factory.label(SGStrata.text(StrataConfiguratorScreen.KEY + "choose_rule",
                    "Choose an address").color(SGStrata.GRAY)).setAnchor(Anchor.TOP_CENTER).setMargin(0f, 4f, 0f, 0f));
            return;
        }
        final StackPanel sides = factory.stackPanel().setSpacing(4f);
        sides.add(factory.label(Text.of(address).color(SGStrata.BLUE)).setMargin(4f, 2f, 4f, 2f));
        final JsonObject rule = this.object("address_rules").get(address).getAsJsonObject();
        for (final String side : List.of("incoming", "outgoing")) {
            final CheckBox box = SGStrata.checkBox(factory, SGStrata.text(StrataConfiguratorScreen.KEY + "rule." + side,
                    side.equals("incoming") ? "Incoming" : "Outgoing")).setSelected(rule.get(side).getAsBoolean());
            box.setTooltip(StrataConfiguratorScreen.tip("rule." + side));
            box.onEvent(Toggle.TOGGLED, event -> {
                final JsonObject now = this.object("address_rules");
                now.get(address).getAsJsonObject().addProperty(side, event.value());
                this.values.put(StrataConfiguratorScreen.NS + "address_rules", now);
            });
            sides.add(box.setMargin(StrataConfiguratorScreen.INSET, 0f, 0f, 0f));
        }
        options.add(sides.setAnchor(Anchor.FILL));
    }

    // 2.3's entry window for an address: allowed both ways until changed, and kept on Save
    private void addRuleDialog() {
        if (this.factory == null) {
            return;
        }
        final ComponentFactory factory = this.factory;
        final TextBox address = factory.textBox().setMaxLength(11);
        address.setSize(160f, SGStrata.BUTTON_HEIGHT);
        address.setTooltip(StrataConfiguratorScreen.tip("rule"));
        final Overlay[] shown = new Overlay[1];
        final Runnable add = () -> {
            final Optional<Address> parsed = Address.parse(address.text().strip());
            if (parsed.isEmpty()) {
                return;
            }
            final JsonObject now = this.object("address_rules");
            final JsonObject added = new JsonObject();
            added.addProperty("incoming", true);
            added.addProperty("outgoing", true);
            now.add(parsed.get().toString(), added);
            this.values.put(StrataConfiguratorScreen.NS + "address_rules", now);
            this.chosenRule = parsed.get().toString();
            shown[0].close();
            this.rebuild();
        };
        address.onEvent(TextBox.SUBMITTED, event -> add.run());
        shown[0] = factory.overlays().dialog(this.entryWindow(factory, SGStrata.text(StrataConfiguratorScreen.KEY
                + "add_rule", "Add rule"), SGStrata.text(StrataConfiguratorScreen.KEY + "rule", "Address"), address,
                add, () -> shown[0].close()));
    }

    // A small window asking for one thing, as 2.3's entry screens
    private Window entryWindow(final ComponentFactory factory, final Text title, final Text label, final TextBox box,
            final Runnable done, final Runnable cancel) {
        final AnchorPanel content = factory.anchorPanel()
                .add(factory.label(label.append(":")).setAnchor(Anchor.TOP_LEFT).setMargin(2f, 5f, 0f, 0f))
                .add(box.setAnchor(Anchor.TOP_LEFT).setMargin(70f, 1f, 0f, 0f))
                .add(SGStrata.button(factory, title, 70f, done).setAnchor(Anchor.BOTTOM_RIGHT)
                        .setMargin(0f, 0f, 54f, 0f))
                .add(SGStrata.button(factory, SGStrata.text("screen.sgcraft.cancel", "Cancel"), 50f, cancel)
                        .setAnchor(Anchor.BOTTOM_RIGHT));
        return StrataConfiguratorScreen.dialogWindow(factory, title, content, 250f, 64f, cancel);
    }

    // The ZPM devices linked to the gate, as 2.3's list; they are linked and unlinked at the devices
    private void openZpms() {
        this.openDialog(SGStrata.text(StrataConfiguratorScreen.KEY + "zpm_links", "Linked ZPMs: %s",
                this.state.zpmLinks().size()), 300f, 190f, close -> {
                    final ComponentFactory factory = Objects.requireNonNull(this.factory);
                    final ListView<String> list = SGStrata.rows(factory.<String>listView(link -> this.zpmRow(factory,
                            link)), StrataConfiguratorScreen.LIST_ROW_HEIGHT);
                    list.setItems(this.state.zpmLinks().stream()
                            .filter(link -> !this.unlinkedZpms.contains(StrataConfiguratorScreen.zpmAt(link)))
                            .toList());
                    final Label note = factory.label(this.state.zpmLinks().isEmpty()
                            ? SGStrata.text(StrataConfiguratorScreen.KEY + "zpm_links.none", "No ZPM device is "
                                    + "linked to this gate. Sneak-use a ZPM console, cart or hub, then use the gate, "
                                    + "to link one")
                            : SGStrata.text(StrataConfiguratorScreen.KEY + "zpm_links.tip", "ZPM devices the gate "
                                    + "draws on from anywhere in its world. The red x unlinks one, as "
                                    + "sneak-using it twice "
                                    + "does:"));
                    note.setWrap(true).setTextScale(0.8f);
                    return factory.anchorPanel()
                            .add(note.setAnchor(Anchor.TOP_LEFT).setMargin(2f, 1f, 2f, 0f).setAlignment(
                                    HorizontalAlignment.STRETCH, VerticalAlignment.TOP))
                            .add(SGStrata.box(factory).add(list.setAnchor(Anchor.FILL)).setAnchor(Anchor.FILL)
                                    .setMargin(0f, 24f, 0f, SGStrata.BAR_HEIGHT))
                            .add(SGStrata.bar(factory).add(SGStrata.button(factory,
                                    SGStrata.text("screen.sgcraft.close", "Close"), 50f, close)
                                    .setAnchor(Anchor.MIDDLE_RIGHT)));
                });
    }

    // Every property SGCraft doesn't lay out itself, as JSON, kept on Save
    private void openOthers() {
        this.openDialog(SGStrata.text(StrataConfiguratorScreen.KEY + "other_list", "Add-on Settings"), 380f, 220f,
                close -> {
                    final ComponentFactory factory = Objects.requireNonNull(this.factory);
                    final StackPanel rows = factory.stackPanel().setSpacing(2f);
                    rows.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
                    final List<Property> others = this.state.properties().stream()
                            .filter(property -> !property.id().startsWith(StrataConfiguratorScreen.NS)).toList();
                    for (final Property property : others) {
                        rows.add(this.otherRow(factory, property.id()));
                    }
                    if (others.isEmpty()) {
                        rows.add(factory.label(SGStrata.text(StrataConfiguratorScreen.KEY + "no_other",
                                "No add-on has given gates any settings").color(SGStrata.GRAY))
                                .setMargin(4f, 4f, 4f, 0f));
                    }
                    final AnchorPanel body = factory.anchorPanel()
                            .add(SGStrata.box(factory).add(factory.scrollPanel().add(rows).setAnchor(Anchor.FILL))
                                    .setAnchor(Anchor.FILL).setMargin(0f, 0f, 0f, SGStrata.BAR_HEIGHT));
                    return this.withButtons(factory, body, close);
                });
    }

    private Component<?> otherRow(final ComponentFactory factory, final String id) {
        final TextBox box = factory.textBox().setMaxLength(32767);
        box.setSize(180f, SGStrata.BUTTON_HEIGHT);
        box.setText(this.values.containsKey(id) ? this.values.get(id).toString() : "");
        if (this.fallbacks.containsKey(id)) {
            box.setPlaceholder(Text.of(this.fallbacks.get(id).toString()).color(SGStrata.DARK_GRAY));
        }
        box.setTooltip(StrataConfiguratorScreen.tip("other"));
        box.onEvent(TextInput.EDITED, event -> {
            if (event.value().isBlank()) {
                this.values.remove(id);
                return;
            }
            try {
                this.values.put(id, JsonParser.parseString(event.value()));
            } catch (final RuntimeException e) {
                // Left as it was until the text is valid JSON
            }
        });
        final AnchorPanel row = factory.anchorPanel()
                .add(factory.label(Text.of(id)).setTextScale(0.8f).setAnchor(Anchor.MIDDLE_LEFT)
                        .setMargin(4f, 0f, 0f, 0f))
                .add(box.setAnchor(Anchor.MIDDLE_RIGHT).setMargin(0f, 0f, 4f, 0f));
        row.setHeight(StrataConfiguratorScreen.ROW_HEIGHT);
        return row.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
    }
}
