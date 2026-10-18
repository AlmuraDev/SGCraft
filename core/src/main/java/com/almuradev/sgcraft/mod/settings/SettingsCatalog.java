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

package com.almuradev.sgcraft.mod.settings;

import com.almuradev.sgcraft.mod.SGConfig;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.computer.ComputerSettings;
import com.almuradev.sgcraft.mod.energy.DialCosts;
import com.almuradev.sgcraft.mod.worldgen.NaquadahOre;
import com.almuradev.sgcraft.mod.worldgen.NaquadahOreSettings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Every setting of {@code sgcraft-server.json} that is one value, as the settings screen and {@code /sgcraft config}
 * show and change them. The address settings, and those that are lists or maps, have commands of their own. A
 * {@linkplain Setting#live() live} setting applies at once; the rest take effect when the server next starts. Either
 * way a change is written to the settings file, leaving the rest of it as it is.
 */
public final class SettingsCatalog {

    /** The kinds of value a setting holds. */
    public enum Type {
        /** {@code true} or {@code false}. */
        BOOL,
        /** A whole number. */
        WHOLE,
        /** Any number. */
        DECIMAL
    }

    /** The screen's pages the settings are on, in the order they are shown. */
    public enum Page {
        GATES(SettingsCatalog.text("page.gates", "Gates")),
        ENERGY(SettingsCatalog.text("page.energy", "Energy")),
        SHIELDS(SettingsCatalog.text("page.shields", "Shields")),
        MALP(SettingsCatalog.text("page.malp", "MALP")),
        WORLDGEN(SettingsCatalog.text("page.worldgen", "Worldgen"));

        private final Component title;

        Page(final Component title) {
            this.title = title;
        }

        /** @return the page's name, lowercase */
        public String id() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        /** @return the page's title, for its tab */
        public Component title() {
            return this.title;
        }
    }

    /**
     * One setting.
     *
     * @param key where it is in the settings file, its sections joined with dots
     * @param page the page it is on
     * @param section the heading it is under on its page
     * @param label its name on the screen
     * @param tip what it does
     * @param type the kind of value it holds
     * @param min the least value it can have, for a number
     * @param aboveMin whether it must be above {@code min} rather than at least {@code min}
     * @param max the most value it can have, for a number
     * @param live whether a change applies at once, rather than when the server next starts
     * @param holder when not null, encodes the whole object holding it from some settings, which the file is given
     *     rather than this one value, as that object can't be read with some of its values missing
     * @param get reads it from the settings
     */
    public record Setting(String key, Page page, Component section, Component label, Component tip, Type type,
            double min, boolean aboveMin, double max, boolean live,
            @Nullable Function<SGConfig, JsonElement> holder, Function<SGConfig, Object> get) {

        /** @return its value in some settings, written as text */
        public String valueIn(final SGConfig config) {
            return SettingsCatalog.format(this.type, this.get.apply(config));
        }

        /** @return its value in the defaults, written as text */
        public String defaultValue() {
            return this.valueIn(SGConfig.DEFAULTS);
        }

        private String[] path() {
            return this.key.split("\\.");
        }
    }

    /**
     * What came of a change.
     *
     * @param refusal why it wasn't made, if it wasn't
     * @param done what it did, if it was made
     * @param saved whether the settings file has it
     */
    public record Result(Optional<Component> refusal, Optional<Component> done, boolean saved) {

        static Result refused(final Component why) {
            return new Result(Optional.of(why), Optional.empty(), false);
        }

        /** @return what to tell the player who made the change: what it did, then whether the file has it */
        public List<Component> messages() {
            final List<Component> messages = new ArrayList<>();
            this.refusal.ifPresent(messages::add);
            this.done.ifPresent(messages::add);
            if (this.refusal.isEmpty() && !this.saved) {
                messages.add(SettingsCatalog.text("not_saved", "The settings file couldn't be written, so the change "
                        + "lasts only until the server stops. The server log says why"));
            }
            return messages;
        }
    }

    private static final List<Setting> ALL = SettingsCatalog.build();

    private SettingsCatalog() {
    }

    /** @return every setting, in the order the screen shows them */
    public static List<Setting> all() {
        return SettingsCatalog.ALL;
    }

    /** @return the setting with a key, if there is one */
    public static Optional<Setting> find(final String key) {
        return SettingsCatalog.ALL.stream().filter(setting -> setting.key().equals(key)).findFirst();
    }

    /**
     * @return the settings the running server uses, with naquadah ore as it generates now rather than as the server
     *     started
     */
    public static SGConfig running(final SGServer server) {
        return server.config().withNaquadahOre(NaquadahOre.settings());
    }

    /**
     * Changes a setting, applying it at once if it is live, and writes it to the settings file.
     *
     * @param key the setting's key
     * @param text its new value, written as text
     */
    public static Result set(final SGServer server, final String key, final String text) {
        final Optional<Setting> found = SettingsCatalog.find(key);
        if (found.isEmpty()) {
            return Result.refused(SettingsCatalog.text("unknown", "There's no setting called %s", key));
        }
        final Setting setting = found.get();
        final Checked checked = SettingsCatalog.check(setting.live() ? SettingsCatalog.running(server)
                : SGConfig.load(), key, text);
        if (checked.refusal().isPresent()) {
            return Result.refused(checked.refusal().get());
        }
        final String shown = checked.shown();
        if (setting.live()) {
            server.stargates().useConfig(checked.next());
            NaquadahOre.use(checked.next().naquadahOre());
            return new Result(Optional.empty(), Optional.of(SettingsCatalog.text("set", "%s is now %s",
                    setting.label(), shown)), SGConfig.edit(checked.edit()));
        }
        if (!SGConfig.edit(checked.edit())) {
            return Result.refused(SettingsCatalog.text("not_written", "The settings file couldn't be written, so "
                    + "nothing changed. The server log says why"));
        }
        return new Result(Optional.empty(), Optional.of(SettingsCatalog.text("set_next", "%s will be %s when the "
                + "server next starts", setting.label(), shown)), true);
    }

    /**
     * A change to a setting, checked but not made.
     *
     * @param refusal why it can't be made, if it can't; the rest is empty then
     * @param shown its new value, written as the screen and commands show it
     * @param edit what it does to the settings file's contents
     * @param next the settings it was checked against, with it made
     */
    public record Checked(Optional<Component> refusal, String shown, Consumer<JsonObject> edit, SGConfig next) {

        static Checked refused(final Component why) {
            return new Checked(Optional.of(why), "", json -> { }, SGConfig.DEFAULTS);
        }
    }

    /**
     * Checks a change to a setting against some settings, without making it.
     *
     * @param base the settings to change
     * @param key the setting's key
     * @param text its new value, written as text
     */
    public static Checked check(final SGConfig base, final String key, final String text) {
        final Optional<Setting> found = SettingsCatalog.find(key);
        if (found.isEmpty()) {
            return Checked.refused(SettingsCatalog.text("unknown", "There's no setting called %s", key));
        }
        final Setting setting = found.get();
        final Optional<JsonPrimitive> value = SettingsCatalog.parse(setting.type(), text.trim());
        if (value.isEmpty()) {
            return Checked.refused(switch (setting.type()) {
                case BOOL -> SettingsCatalog.text("needs_bool", "%s is true or false", setting.label());
                case WHOLE -> SettingsCatalog.text("needs_whole", "%s is a whole number", setting.label());
                case DECIMAL -> SettingsCatalog.text("needs_number", "%s is a number", setting.label());
            });
        }
        final Optional<Component> outside = SettingsCatalog.outside(setting, value.get());
        if (outside.isPresent()) {
            return Checked.refused(outside.get());
        }
        final String shown = SettingsCatalog.format(setting.type(), SettingsCatalog.unbox(setting.type(),
                value.get()));
        final Optional<SGConfig> next = SettingsCatalog.edited(base, setting, value.get());
        if (next.isEmpty() || !setting.valueIn(next.get()).equals(shown)) {
            return Checked.refused(SettingsCatalog.text("not_allowed", "%s can't be %s", setting.label(), shown));
        }
        final Consumer<JsonObject> edit = setting.holder() != null
                ? SettingsCatalog.wholeEdit(setting, next.get())
                : json -> SettingsCatalog.put(json, setting.path(), value.get());
        return new Checked(Optional.empty(), shown, edit, next.get());
    }

    // The settings with one value changed, read back the way the file is, or nothing if they can't be
    private static Optional<SGConfig> edited(final SGConfig config, final Setting setting, final JsonPrimitive value) {
        try {
            final JsonElement json = SGConfig.CODEC.encodeStart(JsonOps.INSTANCE, config).getOrThrow();
            if (!(json instanceof final JsonObject root)) {
                return Optional.empty();
            }
            final String[] path = setting.path();
            if (setting.holder() != null) {
                // The settings leave out an object at its default, so it is put in whole before it is changed
                SettingsCatalog.put(root, Arrays.copyOf(path, path.length - 1), setting.holder().apply(config));
            }
            SettingsCatalog.put(root, path, value);
            return SGConfig.CODEC.parse(JsonOps.INSTANCE, root).result();
        } catch (final RuntimeException e) {
            SGCraft.LOG.error("Couldn't change {}", setting.key(), e);
            return Optional.empty();
        }
    }

    // Writes the whole object holding a setting, as the changed settings have it, or leaves it out of the file when
    // it is at its default
    private static Consumer<JsonObject> wholeEdit(final Setting setting, final SGConfig next) {
        final String[] path = setting.path();
        final String[] parent = Arrays.copyOf(path, path.length - 1);
        final JsonElement whole = setting.holder().apply(next);
        final @Nullable JsonElement written = whole.equals(setting.holder().apply(SGConfig.DEFAULTS)) ? null : whole;
        return json -> SettingsCatalog.put(json, parent, written);
    }

    // Encodes one of the dial costs, the whole object the file keeps it in
    private static Function<SGConfig, JsonElement> cost(final DialCosts.Kind kind) {
        return config -> DialCosts.Cost.CODEC.encodeStart(JsonOps.INSTANCE, config.dialCosts().get(kind)).getOrThrow();
    }

    // Puts a value at its place in the file's sections, making the sections that aren't there, or removes what is
    // there when the value is null
    private static void put(final JsonObject root, final String[] path, final @Nullable JsonElement value) {
        JsonObject into = root;
        for (int part = 0; part < path.length - 1; part++) {
            final JsonObject section = into.get(path[part]) instanceof final JsonObject own ? own : new JsonObject();
            into.add(path[part], section);
            into = section;
        }
        if (value == null) {
            into.remove(path[path.length - 1]);
        } else {
            into.add(path[path.length - 1], value);
        }
    }

    private static Optional<JsonPrimitive> parse(final Type type, final String text) {
        try {
            return switch (type) {
                case BOOL -> text.equalsIgnoreCase("true") || text.equalsIgnoreCase("on")
                        ? Optional.of(new JsonPrimitive(true))
                        : text.equalsIgnoreCase("false") || text.equalsIgnoreCase("off")
                                ? Optional.of(new JsonPrimitive(false)) : Optional.empty();
                case WHOLE -> Optional.of(new JsonPrimitive(Long.parseLong(text)));
                case DECIMAL -> {
                    final double number = Double.parseDouble(text);
                    yield Double.isFinite(number) ? Optional.of(new JsonPrimitive(number)) : Optional.empty();
                }
            };
        } catch (final NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Object unbox(final Type type, final JsonPrimitive value) {
        return switch (type) {
            case BOOL -> value.getAsBoolean();
            case WHOLE -> value.getAsLong();
            case DECIMAL -> value.getAsDouble();
        };
    }

    // Why a number is outside what the setting allows, if it is
    private static Optional<Component> outside(final Setting setting, final JsonPrimitive value) {
        if (setting.type() == Type.BOOL) {
            return Optional.empty();
        }
        final double number = value.getAsDouble();
        final boolean low = setting.aboveMin() ? number <= setting.min() : number < setting.min();
        if (!low && number <= setting.max()) {
            return Optional.empty();
        }
        final String min = SettingsCatalog.format(setting.type(), setting.min());
        final String max = SettingsCatalog.format(setting.type(), setting.max());
        if (setting.aboveMin()) {
            return Optional.of(SettingsCatalog.text("above", "%s must be above %s", setting.label(), min));
        }
        if (setting.max() >= Integer.MAX_VALUE) {
            return Optional.of(SettingsCatalog.text("at_least", "%s must be at least %s", setting.label(), min));
        }
        return Optional.of(SettingsCatalog.text("between", "%s must be from %s to %s", setting.label(), min, max));
    }

    /** @return a value written as text the way the screen and commands show it */
    public static String format(final Type type, final Object value) {
        return switch (type) {
            case BOOL -> String.valueOf(value);
            case WHOLE -> String.valueOf(((Number) value).longValue());
            case DECIMAL -> {
                // A float's own digits, rather than the double nearest them
                final BigDecimal number = value instanceof final Float single ? new BigDecimal(Float.toString(single))
                        : BigDecimal.valueOf(((Number) value).doubleValue());
                yield number.stripTrailingZeros().toPlainString();
            }
        };
    }

    static MutableComponent text(final String key, final String fallback, final Object... args) {
        return Component.translatableWithFallback("setting.sgcraft." + key, fallback, args);
    }

    // The catalog. Only the remote view and the village gates wait for a restart, as both are set up as the server
    // starts; everything else is read as it is used
    private static List<Setting> build() {
        final Builder add = new Builder();
        add.page(Page.GATES, SettingsCatalog.text("section.gates", "Gates"));
        add.whole("gates.chunk_loading_range", SettingsCatalog.text("gates.chunk_loading_range", "Chunks kept loaded"),
                SettingsCatalog.text("gates.chunk_loading_range.tip", "How many chunks around a busy gate's own stay "
                        + "loaded while it dials or holds a wormhole open"), 0, SGConfig.MAX_CHUNK_LOADING_RANGE,
                SGConfig::chunkLoadingRange);
        add.bool("gates.kawoosh_breaks_unbreakable", SettingsCatalog.text("gates.kawoosh_breaks_unbreakable",
                "Kawoosh breaks unbreakable blocks"), SettingsCatalog.text("gates.kawoosh_breaks_unbreakable.tip",
                "Whether a kawoosh clears blocks that can't be broken, such as bedrock"),
                SGConfig::kawooshBreaksUnbreakable);
        add.bool("gates.owners_break_gates", SettingsCatalog.text("gates.owners_break_gates", "Owners can break "
                + "their gates"), SettingsCatalog.text("gates.owners_break_gates.tip", "Whether a gate's owner can "
                + "break it even when the gate is set unbreakable"), SGConfig::ownersBreakGates);
        add.bool("gates.generated_gates_breakable", SettingsCatalog.text("gates.generated_gates_breakable",
                "Generated gates can be broken"), SettingsCatalog.text("gates.generated_gates_breakable.tip",
                "Whether players can break the gates that generate with the world, unless a gate is set otherwise"),
                SGConfig::generatedGatesBreakable);
        add.bool("gates.wormholes_jump", SettingsCatalog.text("gates.wormholes_jump", "Wormholes jump"),
                SettingsCatalog.text("gates.wormholes_jump.tip", "Whether an open wormhole moves to another gate in "
                        + "the same cell when the gate it reaches is broken"), SGConfig::wormholesJump);
        add.bool("gates.log_events", SettingsCatalog.text("gates.log_events", "Log gate events"),
                SettingsCatalog.text("gates.log_events.tip", "Whether gates forming and breaking, and wormholes "
                        + "opening, closing and going astray, are written to the server log"),
                SGConfig::logStargateEvents);
        add.section(SettingsCatalog.text("section.tools", "Tools"));
        add.whole("tools.range", SettingsCatalog.text("tools.range", "Tool reach"), SettingsCatalog.text(
                "tools.range.tip", "How far from a player, in blocks, the handheld tools look for a gate"), 1, 64,
                SGConfig::toolRange);
        add.section(SettingsCatalog.text("section.computers", "Dialing computers"));
        add.whole("computers.link_range", SettingsCatalog.text("computers.link_range", "Link range"),
                SettingsCatalog.text("computers.link_range.tip", "How far from a dialing computer, in blocks, the "
                        + "base of the gate it dials can be"), 1, ComputerSettings.MAX_LINK_RANGE,
                config -> config.options().computers().linkRange());
        add.whole("computers.chair_reach", SettingsCatalog.text("computers.chair_reach", "Chair reach"),
                SettingsCatalog.text("computers.chair_reach.tip", "How far in front of a dialing computer, in blocks, "
                        + "its office chair can stand"), 1, ComputerSettings.MAX_CHAIR_REACH,
                config -> config.options().computers().chairReach());
        add.bool("computers.requires_power", SettingsCatalog.text("computers.requires_power", "Computers need "
                + "power"), SettingsCatalog.text("computers.requires_power.tip", "Whether dialing computers and MALP "
                + "consoles need gate energy from power units or ZPMs near them to run"),
                config -> config.options().computers().requiresPower());
        add.decimal("computers.energy_per_tick", SettingsCatalog.text("computers.energy_per_tick", "Computer energy "
                + "per tick"), SettingsCatalog.text("computers.energy_per_tick.tip", "How much gate energy a dialing "
                + "computer or MALP console uses each tick while it is switched on. It can be a fraction"), 0, false,
                config -> config.options().computers().energyPerTick());
        add.whole("computers.power_reach", SettingsCatalog.text("computers.power_reach", "Computer power reach"),
                SettingsCatalog.text("computers.power_reach.tip", "How far from a dialing computer or MALP console, "
                        + "in blocks along each axis, the power units and ZPMs it draws on can be"), 1,
                ComputerSettings.MAX_POWER_REACH, config -> config.options().computers().powerReach());
        add.whole("computers.history_size", SettingsCatalog.text("computers.history_size", "Dialing history"),
                SettingsCatalog.text("computers.history_size.tip", "How many of the addresses last dialed from a "
                        + "dialing computer its screen lists, to dial again. 0 keeps none"), 0,
                ComputerSettings.MAX_HISTORY_SIZE, config -> config.options().computers().historySize());
        add.section(SettingsCatalog.text("section.remote_view", "Remote view"));
        add.restart();
        add.whole("remote_view.horizontal_radius", SettingsCatalog.text("remote_view.horizontal_radius", "View "
                + "width"), SettingsCatalog.text("remote_view.horizontal_radius.tip", "How far out from the camera "
                + "a remote view sends blocks, in blocks"), 8, 128, config -> config.remoteView().horizontalRadius());
        add.whole("remote_view.vertical_radius", SettingsCatalog.text("remote_view.vertical_radius", "View height"),
                SettingsCatalog.text("remote_view.vertical_radius.tip", "How far above and below the camera a "
                        + "remote view sends blocks, in blocks"), 8, 96,
                config -> config.remoteView().verticalRadius());
        add.whole("remote_view.bytes_per_tick", SettingsCatalog.text("remote_view.bytes_per_tick", "View data per "
                + "tick"), SettingsCatalog.text("remote_view.bytes_per_tick.tip", "About how many bytes of blocks "
                + "each viewer is sent each tick. A new view fills in at this rate"), 4096, 1024 * 1024,
                config -> config.remoteView().bytesPerTick());

        add.page(Page.ENERGY, SettingsCatalog.text("section.energy", "Energy"));
        add.decimal("energy.per_naquadah", SettingsCatalog.text("energy.per_naquadah", "Energy per naquadah"),
                SettingsCatalog.text("energy.per_naquadah.tip", "How much gate energy one naquadah gives a DHD or "
                        + "a battery"), 0, false, SGConfig::energyPerNaquadah);
        add.whole("energy.power_unit_capacity", SettingsCatalog.text("energy.power_unit_capacity", "Power unit "
                + "capacity"), SettingsCatalog.text("energy.power_unit_capacity.tip", "The most gate energy a "
                + "power unit holds"), 1, Long.MAX_VALUE, SGConfig::powerUnitCapacity);
        add.bool("energy.free_in_creative", SettingsCatalog.text("energy.free_in_creative", "Free in creative"),
                SettingsCatalog.text("energy.free_in_creative.tip", "Whether players in creative mode dial without "
                        + "using energy"), SGConfig::freeInCreative);
        add.section(SettingsCatalog.text("section.dial_costs", "Dial costs"));
        add.wholeObject(SettingsCatalog.cost(DialCosts.Kind.CHEVRON));
        add.bool("energy.dial_costs.per_chevron.enabled", SettingsCatalog.text("energy.dial_costs.per_chevron.enabled",
                "Cost per chevron"), SettingsCatalog.text("energy.dial_costs.per_chevron.enabled.tip", "Whether a "
                + "gate pays energy as each chevron locks"),
                config -> config.dialCosts().get(DialCosts.Kind.CHEVRON).enabled());
        add.decimal("energy.dial_costs.per_chevron.energy",
                SettingsCatalog.text("energy.dial_costs.per_chevron.energy", "Energy per chevron"),
                SettingsCatalog.text("energy.dial_costs.per_chevron.energy.tip", "How much energy a gate pays as "
                        + "each chevron locks, before distance"), 0, false,
                config -> config.dialCosts().get(DialCosts.Kind.CHEVRON).energy());
        add.wholeObject(SettingsCatalog.cost(DialCosts.Kind.OPEN));
        add.bool("energy.dial_costs.open.enabled", SettingsCatalog.text("energy.dial_costs.open.enabled", "Cost to "
                + "open"), SettingsCatalog.text("energy.dial_costs.open.enabled.tip", "Whether a gate pays energy "
                + "as its wormhole opens"), config -> config.dialCosts().get(DialCosts.Kind.OPEN).enabled());
        add.decimal("energy.dial_costs.open.energy", SettingsCatalog.text("energy.dial_costs.open.energy", "Energy "
                + "to open"), SettingsCatalog.text("energy.dial_costs.open.energy.tip", "How much energy a gate pays "
                + "as its wormhole opens, before distance"), 0, false,
                config -> config.dialCosts().get(DialCosts.Kind.OPEN).energy());
        add.wholeObject(SettingsCatalog.cost(DialCosts.Kind.TICK));
        add.bool("energy.dial_costs.per_tick.enabled", SettingsCatalog.text("energy.dial_costs.per_tick.enabled",
                "Cost while open"), SettingsCatalog.text("energy.dial_costs.per_tick.enabled.tip", "Whether a gate "
                + "pays energy each tick its wormhole stays open"),
                config -> config.dialCosts().get(DialCosts.Kind.TICK).enabled());
        add.decimal("energy.dial_costs.per_tick.energy", SettingsCatalog.text("energy.dial_costs.per_tick.energy",
                "Energy per tick open"), SettingsCatalog.text("energy.dial_costs.per_tick.energy.tip", "How much "
                + "energy a gate pays each tick its wormhole stays open, before distance"), 0, false,
                config -> config.dialCosts().get(DialCosts.Kind.TICK).energy());
        add.section(SettingsCatalog.text("section.multipliers", "Multipliers"));
        add.decimal("energy.multipliers.distance_factor", SettingsCatalog.text("energy.multipliers.distance_factor",
                "Distance multiplier"), SettingsCatalog.text("energy.multipliers.distance_factor.tip", "How much more "
                + "a wormhole costs the farther it reaches"), 0, false, SGConfig::distanceFactorMultiplier);
        add.decimal("energy.multipliers.inter_world", SettingsCatalog.text("energy.multipliers.inter_world", "Eighth "
                + "chevron multiplier"), SettingsCatalog.text("energy.multipliers.inter_world.tip", "How many times "
                + "more a wormhole costs with an eighth chevron, which reaches another world"), 0, false,
                SGConfig::interWorldMultiplier);
        add.decimal("energy.multipliers.other_group", SettingsCatalog.text("energy.multipliers.other_group", "Ninth "
                + "chevron multiplier"), SettingsCatalog.text("energy.multipliers.other_group.tip", "How many times "
                + "more again a wormhole costs with a ninth chevron, which reaches another group of worlds"), 0,
                false, SGConfig::otherGroupMultiplier);
        add.decimal("energy.multipliers.supergate", SettingsCatalog.text("energy.multipliers.supergate", "Supergate "
                + "multiplier"), SettingsCatalog.text("energy.multipliers.supergate.tip", "How many times more a "
                + "wormhole to or from a supergate costs"), 0, false, SGConfig::supergateEnergyMultiplier);
        add.section(SettingsCatalog.text("section.bridges", "Other mods' energy"));
        add.decimal("energy.bridge_rates.fe_in_per_ge", SettingsCatalog.text("energy.bridge_rates.fe_in_per_ge", "FE "
                + "per gate energy in"), SettingsCatalog.text("energy.bridge_rates.fe_in_per_ge.tip", "How many FE "
                + "buy one gate energy, from cables and other mods' chargers"), 0, true, SGConfig::fePerGeIn);
        add.decimal("energy.bridge_rates.fe_out_per_ge", SettingsCatalog.text("energy.bridge_rates.fe_out_per_ge",
                "FE per gate energy out"), SettingsCatalog.text("energy.bridge_rates.fe_out_per_ge.tip", "How many "
                + "FE one gate energy gives, from an energy converter or a battery other mods draw on"), 0, true,
                SGConfig::fePerGeOut);
        add.decimal("energy.converter_rate", SettingsCatalog.text("energy.converter_rate", "Converter rate"),
                SettingsCatalog.text("energy.converter_rate.tip", "The most gate energy an energy converter turns "
                        + "into FE each tick"), 0, false, SGConfig::converterRate);
        add.section(SettingsCatalog.text("section.zpm", "ZPMs"));
        add.whole("zpm.capacity", SettingsCatalog.text("zpm.capacity", "ZPM capacity"), SettingsCatalog.text(
                "zpm.capacity.tip", "How much gate energy a full ZPM holds"), 1, Long.MAX_VALUE,
                config -> config.options().zpm().capacity());
        add.whole("zpm.search_range", SettingsCatalog.text("zpm.search_range", "ZPM reach"), SettingsCatalog.text(
                "zpm.search_range.tip", "How far from a gate or a city shield the ZPMs it draws on can be, in blocks "
                        + "along each axis"), 1, 16, config -> config.options().zpm().searchRange());
        add.decimal("zpm.tainted_explosion", SettingsCatalog.text("zpm.tainted_explosion", "Tainted ZPM explosion"),
                SettingsCatalog.text("zpm.tainted_explosion.tip", "How big the explosion is when energy is drawn "
                        + "from a tainted ZPM in a console, cart or hub"), 0, false, 1000,
                config -> config.options().zpm().taintedExplosion());
        add.whole("zpm.chest_chance", SettingsCatalog.text("zpm.chest_chance", "ZPM chest chance"),
                SettingsCatalog.text("zpm.chest_chance.tip", "The chance, in percent, that a generated gate has a "
                        + "chest beside it holding a ZPM"), 0, 100, config -> config.options().zpm().chestChance());
        add.whole("zpm.tainted_chance", SettingsCatalog.text("zpm.tainted_chance", "Tainted ZPM chance"),
                SettingsCatalog.text("zpm.tainted_chance.tip", "The chance, in percent, that a ZPM found in such a "
                        + "chest is tainted"), 0, 100, config -> config.options().zpm().taintedChance());

        add.page(Page.SHIELDS, SettingsCatalog.text("section.gate_shields", "Gate shields"));
        add.decimal("shields.gate_energy_per_tick", SettingsCatalog.text("shields.gate_energy_per_tick", "Shield "
                + "energy per tick"), SettingsCatalog.text("shields.gate_energy_per_tick.tip", "How much energy a "
                + "gate's shield takes each tick while it is raised. It can be a fraction"), 0, false,
                config -> config.options().shields().gateEnergyPerTick());
        add.whole("shields.console_range", SettingsCatalog.text("shields.console_range", "Console reach"),
                SettingsCatalog.text("shields.console_range.tip", "How far from a shield console, in blocks, the "
                        + "gate it works can be"), 1, 256, config -> config.options().shields().consoleRange());
        add.section(SettingsCatalog.text("section.city_shields", "City shields"));
        add.decimal("shields.city_energy_per_million_blocks", SettingsCatalog.text(
                "shields.city_energy_per_million_blocks", "Energy per million blocks"), SettingsCatalog.text(
                "shields.city_energy_per_million_blocks.tip", "How much energy a city shield takes each tick for "
                        + "each million blocks it holds"), 0, false,
                config -> config.options().shields().cityEnergyPerMillionBlocks());
        add.whole("shields.city_max_radius", SettingsCatalog.text("shields.city_max_radius", "Largest radius"),
                SettingsCatalog.text("shields.city_max_radius.tip", "The most blocks a city shield reaches from its "
                        + "generator, each way"), 3, 256, config -> config.options().shields().cityMaxRadius());
        add.whole("shields.city_power_reach", SettingsCatalog.text("shields.city_power_reach", "Power reach"),
                SettingsCatalog.text("shields.city_power_reach.tip", "How far from a city shield generator, in blocks "
                        + "along each axis, the power units it draws from can be"), 1, 16,
                config -> config.options().shields().cityPowerReach());
        add.bool("shields.city_keeps_chunks_loaded", SettingsCatalog.text("shields.city_keeps_chunks_loaded",
                "Keeps chunks loaded"), SettingsCatalog.text("shields.city_keeps_chunks_loaded.tip", "Whether a "
                + "raised city shield keeps every chunk under it loaded"),
                config -> config.options().shields().cityKeepsChunksLoaded());

        add.page(Page.MALP, SettingsCatalog.text("section.malp", "MALPs and batteries"));
        add.whole("malp.battery_capacity", SettingsCatalog.text("malp.battery_capacity", "Battery capacity"),
                SettingsCatalog.text("malp.battery_capacity.tip", "The most gate energy a battery holds"), 1,
                Long.MAX_VALUE, config -> config.malp().batteryCapacity());
        add.decimal("malp.power_unit_charge_rate", SettingsCatalog.text("malp.power_unit_charge_rate", "Power unit "
                + "charge rate"), SettingsCatalog.text("malp.power_unit_charge_rate.tip", "How much gate energy a "
                + "power unit puts into the battery in its slot each tick"), 0, false,
                config -> config.malp().powerUnitChargeRate());
        add.decimal("malp.charger_charge_rate", SettingsCatalog.text("malp.charger_charge_rate", "Charger rate"),
                SettingsCatalog.text("malp.charger_charge_rate.tip", "How much gate energy a battery charger puts "
                        + "into its battery each tick"), 0, false, config -> config.malp().chargerChargeRate());
        add.decimal("malp.idle_drain", SettingsCatalog.text("malp.idle_drain", "Energy while watched"),
                SettingsCatalog.text("malp.idle_drain.tip", "How much gate energy a MALP uses each tick while "
                        + "someone watches its feed"), 0, false, config -> config.malp().idleDrain());
        add.decimal("malp.drive_drain", SettingsCatalog.text("malp.drive_drain", "Energy per block driven"),
                SettingsCatalog.text("malp.drive_drain.tip", "How much gate energy a MALP uses for each block it "
                        + "drives"), 0, false, config -> config.malp().driveDrain());
        add.whole("malp.signal_range", SettingsCatalog.text("malp.signal_range", "Signal range"),
                SettingsCatalog.text("malp.signal_range.tip", "How far, in blocks, a console or tablet reaches a MALP "
                        + "directly, and reaches a gate to relay through"), 1, 4096,
                config -> config.malp().signalRange());
        add.whole("malp.linger_ticks", SettingsCatalog.text("malp.linger_ticks", "Chunk stays loaded"),
                SettingsCatalog.text("malp.linger_ticks.tip", "How long a MALP's chunk stays loaded after the last "
                        + "person stops watching it, in ticks"), 0, 72_000, config -> config.malp().lingerTicks());
        add.section(SettingsCatalog.text("section.consoles", "Consoles and tablets"));
        add.bool("malp.console_damage_interrupts", SettingsCatalog.text("malp.console_damage_interrupts", "Damage "
                + "interrupts"), SettingsCatalog.text("malp.console_damage_interrupts.tip", "Whether a player at a "
                + "MALP console who takes damage is taken off it"),
                config -> config.malp().consoleDamageInterrupts());
        add.whole("malp.console_spectators", SettingsCatalog.text("malp.console_spectators", "Spectators"),
                SettingsCatalog.text("malp.console_spectators.tip", "How many of the players near a console, nearest "
                        + "first, see the feed on its monitor, whether or not anyone is at it. Each costs the server a "
                        + "stream of the world around the MALP. 0 shows it to none"), 0, 64,
                config -> config.malp().consoleSpectators());
        add.whole("malp.spectator_range", SettingsCatalog.text("malp.spectator_range", "Spectator range"),
                SettingsCatalog.text("malp.spectator_range.tip", "How near a console, in blocks, a player must be to "
                        + "see the feed on its monitor"), 1, 64, config -> config.malp().spectatorRange());
        add.decimal("malp.tablet_drain", SettingsCatalog.text("malp.tablet_drain", "Tablet energy per tick"),
                SettingsCatalog.text("malp.tablet_drain.tip", "How much gate energy a MALP tablet uses from its own "
                        + "battery each tick while it shows a live feed"), 0, false,
                config -> config.malp().tabletDrain());

        add.page(Page.WORLDGEN, SettingsCatalog.text("section.naquadah_ore", "Naquadah ore"));
        add.bool("worldgen.naquadah_ore.enable_naquadah_ore",
                SettingsCatalog.text("worldgen.naquadah_ore.enable_naquadah_ore", "Naquadah ore"),
                SettingsCatalog.text("worldgen.naquadah_ore.enable_naquadah_ore.tip", "Whether naquadah ore "
                        + "generates at all. /sgcraft ore dimension_ids sets the worlds it generates in"),
                config -> config.naquadahOre().enableNaquadahOre());
        add.bool("worldgen.naquadah_ore.add_ores_to_existing_worlds",
                SettingsCatalog.text("worldgen.naquadah_ore.add_ores_to_existing_worlds", "Add to existing chunks"),
                SettingsCatalog.text("worldgen.naquadah_ore.add_ores_to_existing_worlds.tip", "Whether chunks made "
                        + "without naquadah ore, such as before SGCraft was added, get it when they load"),
                config -> config.naquadahOre().addOresToExistingWorlds());
        add.whole("worldgen.naquadah_ore.gen_under_lava_odds",
                SettingsCatalog.text("worldgen.naquadah_ore.gen_under_lava_odds", "Under lava odds"),
                SettingsCatalog.text("worldgen.naquadah_ore.gen_under_lava_odds.tip", "One chunk in this many gets "
                        + "clusters under lava. 0 gives none"), 0, Integer.MAX_VALUE,
                config -> NaquadahOreSettings.Count.GEN_UNDER_LAVA_ODDS.get(config.naquadahOre()));
        add.whole("worldgen.naquadah_ore.max_nodes_under_lava",
                SettingsCatalog.text("worldgen.naquadah_ore.max_nodes_under_lava", "Under lava columns"),
                SettingsCatalog.text("worldgen.naquadah_ore.max_nodes_under_lava.tip", "The most columns of a chunk "
                        + "searched for lava. Each gives a cluster wherever lava lies on a solid block"), 0,
                Integer.MAX_VALUE, config -> NaquadahOreSettings.Count.MAX_NODES_UNDER_LAVA.get(config.naquadahOre()));
        add.whole("worldgen.naquadah_ore.gen_isolated_odds",
                SettingsCatalog.text("worldgen.naquadah_ore.gen_isolated_odds", "Isolated odds"),
                SettingsCatalog.text("worldgen.naquadah_ore.gen_isolated_odds.tip", "One chunk in this many gets "
                        + "nodes away from lava. 0 gives none"), 0, Integer.MAX_VALUE,
                config -> NaquadahOreSettings.Count.GEN_ISOLATED_ODDS.get(config.naquadahOre()));
        add.whole("worldgen.naquadah_ore.max_isolated_nodes",
                SettingsCatalog.text("worldgen.naquadah_ore.max_isolated_nodes", "Isolated nodes"),
                SettingsCatalog.text("worldgen.naquadah_ore.max_isolated_nodes.tip", "The most nodes away from lava "
                        + "such a chunk gets"), 0, Integer.MAX_VALUE,
                config -> NaquadahOreSettings.Count.MAX_ISOLATED_NODES.get(config.naquadahOre()));
        add.section(SettingsCatalog.text("section.villages", "Villages"));
        add.restart();
        add.whole("worldgen.village_gates.weight", SettingsCatalog.text("worldgen.village_gates.weight", "Gate plaza "
                + "weight"), SettingsCatalog.text("worldgen.village_gates.weight.tip", "How often villages build a "
                + "gate plaza, against the other pieces they can build. 0 builds none"), 0, 150,
                config -> config.villageGates().weight());
        add.bool("worldgen.village_gates.tokra", SettingsCatalog.text("worldgen.village_gates.tokra", "Tok'ra at "
                + "village gates"), SettingsCatalog.text("worldgen.village_gates.tokra.tip", "Whether a Tok'ra "
                + "stands in front of a village's gate once it is built"), config -> config.villageGates().tokra());
        return List.copyOf(add.settings);
    }

    // Adds settings under the page and heading last given. Each page's settings are live until restart() is called,
    // and write their own value until wholeObject() is called, both until the next section
    private static final class Builder {

        private final List<Setting> settings = new ArrayList<>();
        private Page page = Page.GATES;
        private Component section = Component.empty();
        private boolean live = true;
        private @Nullable Function<SGConfig, JsonElement> holder;

        void page(final Page page, final Component section) {
            this.page = page;
            this.section(section);
        }

        void section(final Component section) {
            this.section = section;
            this.live = true;
            this.holder = null;
        }

        void restart() {
            this.live = false;
        }

        void wholeObject(final Function<SGConfig, JsonElement> holder) {
            this.holder = holder;
        }

        void bool(final String key, final Component label, final Component tip, final Function<SGConfig, ?> get) {
            this.add(key, label, tip, Type.BOOL, 0, false, 0, get);
        }

        void whole(final String key, final Component label, final Component tip, final long min, final long max,
                final Function<SGConfig, ?> get) {
            this.add(key, label, tip, Type.WHOLE, min, false, max, get);
        }

        void decimal(final String key, final Component label, final Component tip, final double min,
                final boolean aboveMin, final Function<SGConfig, ?> get) {
            this.add(key, label, tip, Type.DECIMAL, min, aboveMin, Long.MAX_VALUE, get);
        }

        void decimal(final String key, final Component label, final Component tip, final double min,
                final boolean aboveMin, final double max, final Function<SGConfig, ?> get) {
            this.add(key, label, tip, Type.DECIMAL, min, aboveMin, max, get);
        }

        private void add(final String key, final Component label, final Component tip, final Type type,
                final double min, final boolean aboveMin, final double max, final Function<SGConfig, ?> get) {
            this.settings.add(new Setting(key, this.page, this.section, label, tip, type, min, aboveMin, max,
                    this.live, this.holder, get::apply));
        }
    }
}
