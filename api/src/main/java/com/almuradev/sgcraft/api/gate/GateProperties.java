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

package com.almuradev.sgcraft.api.gate;

import com.almuradev.sgcraft.api.SGCraftApi;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.almuradev.sgcraft.api.SGCraftRegistries;
import com.almuradev.sgcraft.api.access.AddressRule;
import com.almuradev.sgcraft.api.access.GateRight;
import com.almuradev.sgcraft.api.address.Address;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The gate properties every implementation must register, each under the identifier {@link #all()} gives it.
 *
 * @since 0.1.0
 */
public final class GateProperties {

    private static final Map<Identifier, GateProperty<?>> ALL = new HashMap<>();

    private static final Codec<Set<GateRight>> RIGHTS_CODEC =
            GateRight.CODEC.listOf().xmap(Set::copyOf, List::copyOf);

    private static final Codec<Double> ENERGY_CODEC = Codec.DOUBLE.validate(value -> value >= 0
            && Double.isFinite(value) ? DataResult.success(value)
            : DataResult.error(() -> "Must be a finite number, not negative: " + value));

    /**
     * The most entries {@link #ADDRESS_RULES} and {@link #PLAYER_RIGHTS} may hold. Implementations must refuse a
     * value with more.
     */
    public static final int MAX_ENTRIES = 256;

    /** How many ticks the wormhole stays open before it closes on its own, or 0 for no limit. */
    public static final GateProperty<Integer> OPEN_TICKS =
            GateProperties.register("open_ticks", ExtraCodecs.NON_NEGATIVE_INT, 6000);

    /** Whether dialing draws energy from the gate's {@linkplain Stargate#energy() store}. */
    public static final GateProperty<Boolean> REQUIRES_POWER =
            GateProperties.register("requires_power", Codec.BOOL, true);

    /** How much energy the gate's store can hold. */
    public static final GateProperty<Long> ENERGY_CAPACITY =
            GateProperties.register("energy_capacity", Codec.LONG.validate(GateProperties::nonNegative), 1000L);

    /**
     * Whether the gate pays {@link #CHEVRON_COST} as each chevron locks when it dials. With no value, the server's
     * setting applies.
     */
    public static final GateProperty<Boolean> CHEVRON_COST_ENABLED =
            GateProperties.register("chevron_cost_enabled", Codec.BOOL);

    /**
     * The energy the gate pays as each chevron locks when it dials, before distance. With no value, the server's
     * setting applies.
     */
    public static final GateProperty<Double> CHEVRON_COST =
            GateProperties.register("chevron_cost", GateProperties.ENERGY_CODEC);

    /**
     * Whether the gate pays {@link #OPEN_COST} as a wormhole it dialed forms. With no value, the server's setting
     * applies.
     */
    public static final GateProperty<Boolean> OPEN_COST_ENABLED =
            GateProperties.register("open_cost_enabled", Codec.BOOL);

    /**
     * The energy the gate pays as a wormhole it dialed forms, before distance. With no value, the server's setting
     * applies.
     */
    public static final GateProperty<Double> OPEN_COST =
            GateProperties.register("open_cost", GateProperties.ENERGY_CODEC);

    /**
     * Whether the gate pays {@link #TICK_COST} each tick a wormhole it dialed stays open. With no value, the
     * server's setting applies.
     */
    public static final GateProperty<Boolean> TICK_COST_ENABLED =
            GateProperties.register("tick_cost_enabled", Codec.BOOL);

    /**
     * The energy the gate pays each tick a wormhole it dialed stays open, before distance. With no value, the
     * server's setting applies.
     */
    public static final GateProperty<Double> TICK_COST =
            GateProperties.register("tick_cost", GateProperties.ENERGY_CODEC);

    /**
     * How much energy one piece of naquadah gives the gate when it is burned for fuel, such as in a DHD linked to it.
     * With no value, the server's setting applies.
     */
    public static final GateProperty<Double> ENERGY_PER_NAQUADAH =
            GateProperties.register("energy_per_naquadah", GateProperties.ENERGY_CODEC);

    /**
     * How much more a wormhole the gate dials costs the further it reaches; 0 makes distance cost nothing extra. With
     * no value, the server's setting applies.
     */
    public static final GateProperty<Double> DISTANCE_FACTOR_MULTIPLIER =
            GateProperties.register("distance_factor_multiplier", GateProperties.ENERGY_CODEC);

    /**
     * How many times more a wormhole the gate dials costs with an eighth chevron, which reaches another world. With
     * no value, the server's setting applies.
     */
    public static final GateProperty<Double> INTER_WORLD_MULTIPLIER =
            GateProperties.register("inter_world_multiplier", GateProperties.ENERGY_CODEC);

    /**
     * Whether a storm can disturb a wormhole the gate dials. While it rains on the gate as the wormhole opens,
     * lightning may strike it and send the wormhole to another gate, which the implementation chooses.
     */
    public static final GateProperty<Boolean> LIGHTNING_MALFUNCTION =
            GateProperties.register("lightning_malfunction", Codec.BOOL, true);

    /**
     * Whether a DHD or a PDD dialing out from the gate locks every chevron at once, rather than turning the ring for
     * each symbol in turn.
     */
    public static final GateProperty<Boolean> CHEVRONS_LOCK_ON_DIAL =
            GateProperties.register("chevrons_lock_on_dial", Codec.BOOL, false);

    /**
     * How fast the gate dials, in degrees a tick: how fast its ring turns, or for a gate whose light runs round its
     * ring instead, how fast the light runs. With no value, the gate's type sets it.
     */
    public static final GateProperty<Double> RING_SPEED =
            GateProperties.register("ring_speed", GateProperties.between(0.1, 360));

    /** How far from the gate's base, in blocks, a DHD can stand and still link to it. */
    public static final GateProperty<Double> DHD_RANGE =
            GateProperties.register("dhd_range", GateProperties.between(0, 32), 6.0);

    /**
     * How much harm the burst of a wormhole opening does each tick to what stands in front of the gate, when
     * {@link #OPENING_DAMAGE} is on.
     */
    public static final GateProperty<Double> OPENING_DAMAGE_AMOUNT =
            GateProperties.register("opening_damage_amount", GateProperties.ENERGY_CODEC, 5000.0);

    /** Whether the naquadah in the DHDs linked to the gate powers it. */
    public static final GateProperty<Boolean> DHD_FUEL =
            GateProperties.register("dhd_fuel", Codec.BOOL, true);

    /** Whether the open wormhole's surface lets a little of what is behind it show through. */
    public static final GateProperty<Boolean> HORIZON_TRANSPARENT =
            GateProperties.register("horizon_transparent", Codec.BOOL, true);

    /**
     * Whether a player killed by the gate, against its shut iris or entering its wormhole backwards while travel is
     * one way, keeps what they carry. When false, what they carry is lost rather than dropped, unless the
     * {@code keepInventory} game rule keeps it.
     */
    public static final GateProperty<Boolean> PRESERVE_INVENTORY =
            GateProperties.register("preserve_inventory", Codec.BOOL, false);

    /** Whether travelers can only pass from the dialing gate to the dialed gate. */
    public static final GateProperty<Boolean> ONE_WAY_TRAVEL =
            GateProperties.register("one_way_travel", Codec.BOOL, true);

    /** Whether a traveler who enters the dialed gate's wormhole while travel is one way dies. */
    public static final GateProperty<Boolean> REVERSE_TRAVEL_KILLS =
            GateProperties.register("reverse_travel_kills", Codec.BOOL, false);

    /** Whether the dialed gate, as well as the dialing gate, can close the wormhole. */
    public static final GateProperty<Boolean> CLOSE_FROM_EITHER_END =
            GateProperties.register("close_from_either_end", Codec.BOOL, true);

    /** Whether the burst of the wormhole opening harms what is in front of the gate. */
    public static final GateProperty<Boolean> OPENING_DAMAGE =
            GateProperties.register("opening_damage", Codec.BOOL, true);

    /**
     * Whether this gate moves real fluid through the wormhole while it is open. Fluid coming from a gate at the other
     * end, standing in it, pours out of this gate; fluid this gate stands in is drawn from its side, and a body of it
     * small enough to empty, such as a pond, drains away. Implementations must take the fluid a gate poured away again
     * when the wormhole closes. The fluid pouring is shown either way.
     */
    public static final GateProperty<Boolean> SPILLS_FLUID =
            GateProperties.register("spills_fluid", Codec.BOOL, true);

    /**
     * How strong an explosion goes off in the middle of the gate's ring when it is broken while its wormhole is open,
     * as TNT is 4. 0 for none.
     */
    public static final GateProperty<Double> BREACH_EXPLOSION =
            GateProperties.register("breach_explosion", GateProperties.between(0, 32), 10.0);

    /** Whether the explosion of {@link #BREACH_EXPLOSION} sets fires. */
    public static final GateProperty<Boolean> BREACH_FIRE =
            GateProperties.register("breach_fire", Codec.BOOL, true);

    /** Whether the explosion of {@link #BREACH_EXPLOSION} breaks blocks, or only harms what stands near. */
    public static final GateProperty<Boolean> BREACH_BREAKS_BLOCKS =
            GateProperties.register("breach_breaks_blocks", Codec.BOOL, true);

    /** Whether the iris goes back to how it was before the connection once the wormhole closes. */
    public static final GateProperty<Boolean> IRIS_RESTORES_STATE =
            GateProperties.register("iris_restores_state", Codec.BOOL, false);

    /** Whether a redstone signal can work the gate, such as closing its iris. */
    public static final GateProperty<Boolean> REDSTONE_INPUT =
            GateProperties.register("redstone_input", Codec.BOOL, true);

    /** Whether the gate gives out a redstone signal that reflects its state. */
    public static final GateProperty<Boolean> REDSTONE_OUTPUT =
            GateProperties.register("redstone_output", Codec.BOOL, true);

    /**
     * The iris fitted to the gate. A gate has no iris until this is set, and setting it fits one, starting open.
     * Removing it takes the iris off. {@link Stargate#iris()} works the fitted iris. Implementations may set it as a
     * gate forms, when the server's configuration asks for new gates to have an iris.
     */
    public static final GateProperty<ResourceKey<IrisType>> IRIS =
            GateProperties.register("iris", ResourceKey.codec(SGCraftRegistries.IRIS_TYPE));

    /**
     * Whether a traveler carrying a phase shift device passes through this gate's shut iris, coming out in front of
     * it, instead of meeting the iris. What counts as carrying one is up to the implementation.
     */
    public static final GateProperty<Boolean> PHASE_SHIFT_PASSES_IRIS =
            GateProperties.register("phase_shift_passes_iris", Codec.BOOL, true);

    /** Whether other gates may dial this gate, unless {@link #ADDRESS_RULES} says otherwise for their address. */
    public static final GateProperty<Boolean> ALLOW_INCOMING =
            GateProperties.register("allow_incoming", Codec.BOOL, true);

    /** Whether this gate may dial other gates, unless {@link #ADDRESS_RULES} says otherwise for their address. */
    public static final GateProperty<Boolean> ALLOW_OUTGOING =
            GateProperties.register("allow_outgoing", Codec.BOOL, true);

    /** Rules for particular addresses, which override {@link #ALLOW_INCOMING} and {@link #ALLOW_OUTGOING}. */
    public static final GateProperty<Map<Address, AddressRule>> ADDRESS_RULES =
            GateProperties.register("address_rules",
                    GateProperties.capped(Codec.unboundedMap(Address.CODEC, AddressRule.CODEC)), Map.of());

    /**
     * The rights of players {@link #PLAYER_RIGHTS} doesn't list, other than the gate's
     * {@linkplain Stargate#owner() owner}, who has every right. By default they may use the gate and its iris, but
     * not change its settings.
     */
    public static final GateProperty<Set<GateRight>> DEFAULT_RIGHTS =
            GateProperties.register("default_rights", GateProperties.RIGHTS_CODEC,
                    Set.of(GateRight.USE, GateRight.IRIS));

    /** The rights of particular players, by UUID, in place of {@link #DEFAULT_RIGHTS}. */
    public static final GateProperty<Map<UUID, Set<GateRight>>> PLAYER_RIGHTS =
            GateProperties.register("player_rights",
                    GateProperties.capped(Codec.unboundedMap(UUIDUtil.STRING_CODEC, GateProperties.RIGHTS_CODEC)),
                    Map.of());

    /** Whether players can see the gate's address on the gate. */
    public static final GateProperty<Boolean> ADDRESS_VISIBLE =
            GateProperties.register("address_visible", Codec.BOOL, true);

    /**
     * Whether any player may break the gate. Implementations may let some players break it whatever this says, such
     * as its owner or server operators, and may give a {@link #GENERATED} gate with no value of its own a default
     * from the server's configuration.
     */
    public static final GateProperty<Boolean> BREAKABLE =
            GateProperties.register("breakable", Codec.BOOL, false);

    /** Whether the gate was placed by world generation rather than built by a player. */
    public static final GateProperty<Boolean> GENERATED =
            GateProperties.register("generated", Codec.BOOL, false);

    private GateProperties() {
    }

    /** @return every property here, by the identifier it must be registered under */
    public static Map<Identifier, GateProperty<?>> all() {
        return Collections.unmodifiableMap(GateProperties.ALL);
    }

    private static Codec<Double> between(final double min, final double max) {
        return Codec.DOUBLE.validate(value -> value >= min && value <= max ? DataResult.success(value)
                : DataResult.error(() -> "Must be from " + min + " to " + max + ": " + value));
    }

    private static <K, V> Codec<Map<K, V>> capped(final Codec<Map<K, V>> codec) {
        return codec.validate(map -> map.size() <= GateProperties.MAX_ENTRIES ? DataResult.success(map)
                : DataResult.error(() -> "Must have at most " + GateProperties.MAX_ENTRIES + " entries: "
                        + map.size()));
    }

    private static DataResult<Long> nonNegative(final Long value) {
        return value >= 0 ? DataResult.success(value) : DataResult.error(() -> "Must not be negative: " + value);
    }

    private static <T> GateProperty<T> register(final String path, final Codec<T> codec) {
        return GateProperties.register(path, new GateProperty<>(codec));
    }

    private static <T> GateProperty<T> register(final String path, final Codec<T> codec, final T defaultValue) {
        return GateProperties.register(path, new GateProperty<>(codec, defaultValue));
    }

    private static <T> GateProperty<T> register(final String path, final GateProperty<T> property) {
        GateProperties.ALL.put(Identifier.fromNamespaceAndPath(SGCraftApi.NAMESPACE, path), property);
        return property;
    }
}
