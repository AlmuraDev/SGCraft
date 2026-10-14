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

    /** How many ticks the wormhole stays open before it closes on its own, or 0 for no limit. */
    public static final GateProperty<Integer> OPEN_TICKS =
            GateProperties.register("open_ticks", ExtraCodecs.NON_NEGATIVE_INT, 6000);

    /** Whether dialling draws energy from the gate's {@linkplain Stargate#energy() store}. */
    public static final GateProperty<Boolean> REQUIRES_POWER =
            GateProperties.register("requires_power", Codec.BOOL, true);

    /** How much energy the gate's store can hold. */
    public static final GateProperty<Long> ENERGY_CAPACITY =
            GateProperties.register("energy_capacity", Codec.LONG.validate(GateProperties::nonNegative), 1000L);

    /** Whether travellers can only pass from the dialling gate to the dialled gate. */
    public static final GateProperty<Boolean> ONE_WAY_TRAVEL =
            GateProperties.register("one_way_travel", Codec.BOOL, true);

    /** Whether a traveller who enters the dialled gate's wormhole while travel is one way dies. */
    public static final GateProperty<Boolean> REVERSE_TRAVEL_KILLS =
            GateProperties.register("reverse_travel_kills", Codec.BOOL, false);

    /** Whether the dialled gate, as well as the dialling gate, can close the wormhole. */
    public static final GateProperty<Boolean> CLOSE_FROM_EITHER_END =
            GateProperties.register("close_from_either_end", Codec.BOOL, true);

    /** Whether the burst of the wormhole opening harms what is in front of the gate. */
    public static final GateProperty<Boolean> OPENING_DAMAGE =
            GateProperties.register("opening_damage", Codec.BOOL, true);

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
     * Removing it takes the iris off. {@link Stargate#iris()} works the fitted iris.
     */
    public static final GateProperty<ResourceKey<IrisType>> IRIS =
            GateProperties.register("iris", ResourceKey.codec(SGCraftRegistries.IRIS_TYPE));

    /** Whether other gates may dial this gate, unless {@link #ADDRESS_RULES} says otherwise for their address. */
    public static final GateProperty<Boolean> ALLOW_INCOMING =
            GateProperties.register("allow_incoming", Codec.BOOL, true);

    /** Whether this gate may dial other gates, unless {@link #ADDRESS_RULES} says otherwise for their address. */
    public static final GateProperty<Boolean> ALLOW_OUTGOING =
            GateProperties.register("allow_outgoing", Codec.BOOL, true);

    /** Rules for particular addresses, which override {@link #ALLOW_INCOMING} and {@link #ALLOW_OUTGOING}. */
    public static final GateProperty<Map<Address, AddressRule>> ADDRESS_RULES =
            GateProperties.register("address_rules", Codec.unboundedMap(Address.CODEC, AddressRule.CODEC), Map.of());

    /** The rights of players {@link #PLAYER_RIGHTS} doesn't list. */
    public static final GateProperty<Set<GateRight>> DEFAULT_RIGHTS =
            GateProperties.register("default_rights", GateProperties.RIGHTS_CODEC,
                    Set.of(GateRight.USE, GateRight.IRIS, GateRight.CONFIGURE));

    /** The rights of particular players, by UUID, in place of {@link #DEFAULT_RIGHTS}. */
    public static final GateProperty<Map<UUID, Set<GateRight>>> PLAYER_RIGHTS =
            GateProperties.register("player_rights",
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, GateProperties.RIGHTS_CODEC), Map.of());

    /** Whether players can see the gate's address on the gate. */
    public static final GateProperty<Boolean> ADDRESS_VISIBLE =
            GateProperties.register("address_visible", Codec.BOOL, true);

    /** Whether players can break the gate. */
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
