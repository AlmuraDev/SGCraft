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

package com.almuradev.sgcraft.mod.gate;

import com.almuradev.sgcraft.api.ActionResult;
import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.SGCraftRegistries;
import com.almuradev.sgcraft.api.access.GateRight;
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.address.AddressTarget;
import com.almuradev.sgcraft.api.gate.DialMode;
import com.almuradev.sgcraft.api.gate.GatePlacement;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.GateProperty;
import com.almuradev.sgcraft.api.gate.GateType;
import com.almuradev.sgcraft.api.gate.IrisState;
import com.almuradev.sgcraft.api.gate.IrisType;
import com.almuradev.sgcraft.api.gate.PlacementResult;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.api.gate.Stargates;
import com.almuradev.sgcraft.mod.SGConfig;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.address.SGAddressing;
import com.almuradev.sgcraft.mod.block.entity.StargateBaseBlockEntity;
import com.almuradev.sgcraft.mod.computer.ComputerSettings;
import com.almuradev.sgcraft.mod.energy.DialCosts;
import com.almuradev.sgcraft.mod.event.SGEventBus;
import com.almuradev.sgcraft.mod.event.SGEvents;
import com.almuradev.sgcraft.mod.gate.GateStore.GateRecord;
import com.almuradev.sgcraft.mod.shield.ShieldSettings;
import com.almuradev.sgcraft.mod.zpm.ZpmSettings;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The gates on a running server: the formed gates in the {@link GateStore}, each with a live {@link SGStargate} once
 * it is asked for, and the connections between them, which it ticks.
 */
public final class SGStargates implements Stargates {

    // The chevrons that reach another world, and another group of worlds
    private static final int EIGHTH_CHEVRON = 8;
    private static final int NINTH_CHEVRON = 9;

    // How many chevrons the server's settings may give gates as they form
    private static final Codec<Integer> CHEVRONS = Codec.intRange(7, 9);

    private final MinecraftServer server;
    private final SGAddressing addressing;
    private SGConfig config;
    // Gates whose iris is moving, ticked until it stops, with why it moved
    private final Map<SGStargate, Cause> irises = new HashMap<>();
    // While a gate is being placed whole, its forming fires no forming events
    private boolean placing;
    private final Map<GlobalPos, SGStargate> live = new HashMap<>();
    private final List<SGConnection> connections = new ArrayList<>();
    // Gates with symbols locked one at a time, as a DHD does, watched so they let go if left
    private final Set<SGStargate> engaging = new HashSet<>();
    // The players placing gate blocks this tick, by the block, so a gate their block finishes is theirs
    private final Map<GlobalPos, UUID> builders = new HashMap<>();
    // The server's values for gates without their own, read from the settings when first needed
    private @Nullable Map<GateProperty<?>, Optional<?>> defaults;

    public SGStargates(final MinecraftServer server, final SGAddressing addressing, final SGConfig config) {
        this.server = server;
        this.addressing = addressing;
        this.config = config;
    }

    public SGConfig config() {
        return this.config;
    }

    /**
     * Runs with other settings from now on, as the settings screen and {@code /sgcraft config} change them. The
     * caller writes them to the settings file.
     */
    public void useConfig(final SGConfig config) {
        this.config = config;
        this.defaults = null;
    }

    /**
     * Changes the server's values for gates without their own, and writes them to the settings file. The iris and
     * the chevrons only reach gates that form from now on.
     *
     * @return whether the settings file was written
     */
    public boolean setGateDefaults(final Map<Identifier, JsonElement> defaults) {
        this.config = this.config.withGateDefaults(defaults);
        this.defaults = null;
        return SGConfig.saveGateDefaults(this.config.gateDefaults());
    }

    /**
     * Changes whether owners may break their own gates, and whether generated gates without a value of their own
     * may be broken, and writes both to the settings file.
     *
     * @return whether the settings file was written
     */
    public boolean setBreaking(final boolean ownersBreakGates, final boolean generatedGatesBreakable) {
        this.config = this.config.withBreaking(ownersBreakGates, generatedGatesBreakable);
        return SGConfig.saveOwnersBreakGates(ownersBreakGates)
                & SGConfig.saveGeneratedGatesBreakable(generatedGatesBreakable);
    }

    /** @return every key the server's values for gates can have: each property's identifier, and the chevrons */
    public List<Identifier> gateDefaultKeys() {
        final List<Identifier> keys = new ArrayList<>(this.server.registryAccess()
                .lookupOrThrow(SGCraftRegistries.GATE_PROPERTY).keySet());
        keys.add(SGConfig.CHEVRONS);
        keys.sort(Comparator.naturalOrder());
        return keys;
    }

    /** @return whether a value is one gates can have for the key, as {@link #gateDefaultKeys()} names it */
    public boolean isGateDefault(final Identifier key, final JsonElement value) {
        if (key.equals(SGConfig.CHEVRONS)) {
            return SGStargates.CHEVRONS.parse(JsonOps.INSTANCE, value).isSuccess();
        }
        final GateProperty<?> property = this.server.registryAccess().lookupOrThrow(SGCraftRegistries.GATE_PROPERTY)
                .getValue(key);
        return property != null && property.codec().parse(JsonOps.INSTANCE, value).isSuccess();
    }

    /**
     * The value a gate without its own has for a property: the server's, if its settings give one, otherwise the
     * property's default. The iris is only ever given as a gate forms, so it has none here.
     */
    public <T> Optional<T> defaultOf(final GateProperty<T> property) {
        if (property == GateProperties.IRIS) {
            return property.defaultValue();
        }
        @SuppressWarnings("unchecked")
        final Optional<T> server = (Optional<T>) this.defaults().getOrDefault(property, Optional.empty());
        return server.isPresent() ? server : property.defaultValue();
    }

    /**
     * The value the gate at {@code position} has for a property while it has none of its own: a generated gate is
     * breakable as the server's settings say, and otherwise it is {@link #defaultOf(GateProperty)}.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> inherited(final GlobalPos position, final GateProperty<T> property) {
        if (property == GateProperties.BREAKABLE && this.property(position, GateProperties.GENERATED).orElse(false)) {
            return Optional.of((T) (Boolean) this.config.generatedGatesBreakable());
        }
        return this.defaultOf(property);
    }

    // The server's values for gates, by property; a value its property can't read is left out, and logged
    private Map<GateProperty<?>, Optional<?>> defaults() {
        if (this.defaults == null) {
            final Map<GateProperty<?>, Optional<?>> parsed = new HashMap<>();
            final Registry<GateProperty<?>> registry = this.server.registryAccess()
                    .lookupOrThrow(SGCraftRegistries.GATE_PROPERTY);
            this.config.gateDefaults().forEach((id, json) -> {
                final GateProperty<?> property = registry.getValue(id);
                if (property != null) {
                    final Optional<?> value = property.codec().parse(JsonOps.INSTANCE, json).result();
                    if (value.isPresent()) {
                        parsed.put(property, value);
                        return;
                    }
                }
                if (!id.equals(SGConfig.CHEVRONS)) {
                    SGCraft.LOG.warn("The gate default {} = {} isn't a value of any gate property, so it is ignored",
                            id, json);
                }
            });
            this.defaults = parsed;
        }
        return this.defaults;
    }

    /**
     * Changes what wormholes cost from now on, and writes it to the settings file. Wormholes already dialing or open
     * keep what they cost when they were dialed.
     *
     * @return whether the settings file was written
     */
    public boolean setDialCosts(final DialCosts costs) {
        this.config = this.config.withDialCosts(costs);
        return SGConfig.saveDialCosts(costs);
    }

    /**
     * Changes how many times more a wormhole costs with an eighth chevron and with a ninth, from the next dial, and
     * writes them to the settings file.
     *
     * @return whether the settings file was written
     */
    public boolean setChevronMultipliers(final double interWorld, final double otherGroup) {
        this.config = this.config.withChevronMultipliers(interWorld, otherGroup);
        return SGConfig.saveChevronMultipliers(interWorld, otherGroup);
    }

    /**
     * Changes the rates for FE coming in and going out, and the converter rate, from now on, and writes them to the
     * settings file.
     *
     * @return whether the settings file was written
     */
    public boolean setFeRates(final double fePerGeIn, final double fePerGeOut, final double converterRate) {
        this.config = this.config.withFeRates(fePerGeIn, fePerGeOut, converterRate);
        return SGConfig.saveFeRates(fePerGeIn, fePerGeOut, converterRate);
    }

    /**
     * Changes whether an open wormhole jumps to another gate in the cell when the gate it reaches is broken, and
     * writes it to the settings file.
     *
     * @return whether the settings file was written
     */
    public boolean setWormholesJump(final boolean jump) {
        this.config = this.config.withWormholesJump(jump);
        return SGConfig.saveWormholesJump(jump);
    }

    /**
     * Changes whether a kawoosh clears blocks that can't be broken from now on, and writes it to the settings file.
     *
     * @return whether the settings file was written
     */
    public boolean setKawooshBreaksUnbreakable(final boolean breaks) {
        this.config = this.config.withKawooshBreaksUnbreakable(breaks);
        return SGConfig.saveKawooshBreaksUnbreakable(breaks);
    }

    /**
     * Changes how many chunks around a busy gate's own stay loaded, from the next time its chunks are loaded again,
     * and writes it to the settings file.
     *
     * @return whether the settings file was written
     */
    /**
     * Sets how bright open horizons light the world round them, and writes it to the settings file.
     *
     * @return whether the file was written
     */
    public boolean setEventHorizonLight(final int light) {
        this.config = this.config.withEventHorizonLight(light);
        return SGConfig.saveEventHorizonLight(light);
    }

    public boolean setChunkLoadingRange(final int range) {
        this.config = this.config.withChunkLoadingRange(range);
        return SGConfig.saveChunkLoadingRange(range);
    }

    /**
     * Changes the shield settings, from the next tick, and writes them to the settings file.
     *
     * @return whether the settings file was written
     */
    public boolean setShields(final ShieldSettings shields) {
        this.config = this.config.withShields(shields);
        return SGConfig.saveShields(shields);
    }

    /**
     * Changes the ZPM settings, from the next tick, and writes them to the settings file.
     *
     * @return whether the settings file was written
     */
    public boolean setZpm(final ZpmSettings zpm) {
        this.config = this.config.withZpm(zpm);
        return SGConfig.saveZpm(zpm);
    }

    /**
     * Uses other dialing computer settings from now on, and writes them to the settings file.
     *
     * @return whether the file was written
     */
    public boolean setComputers(final ComputerSettings computers) {
        this.config = this.config.withComputers(computers);
        return SGConfig.saveComputers(computers);
    }

    /**
     * Changes whether a player at a MALP console who takes damage is taken off it, from the next hit, and writes it
     * to the settings file.
     *
     * @return whether the settings file was written
     */
    public boolean setConsoleDamageInterrupts(final boolean interrupts) {
        this.config = this.config.withMalp(this.config.malp().withConsoleDamageInterrupts(interrupts));
        return SGConfig.saveConsoleDamageInterrupts(interrupts);
    }

    /**
     * Changes how many of the players near a driven MALP console see the feed on its monitor, from the consoles' next
     * look around them, and writes it to the settings file.
     *
     * @return whether the settings file was written
     */
    public boolean setConsoleSpectators(final int spectators) {
        this.config = this.config.withMalp(this.config.malp().withConsoleSpectators(spectators));
        return SGConfig.saveConsoleSpectators(spectators);
    }

    /**
     * Changes how near a driven MALP console players see the feed on its monitor, from the consoles' next look around
     * them, and writes it to the settings file.
     *
     * @return whether the settings file was written
     */
    public boolean setSpectatorRange(final int range) {
        this.config = this.config.withMalp(this.config.malp().withSpectatorRange(range));
        return SGConfig.saveSpectatorRange(range);
    }

    /**
     * Changes how much gate energy a MALP tablet uses each tick while it shows a live feed, from the next tick, and
     * writes it to the settings file.
     *
     * @return whether the settings file was written
     */
    public boolean setTabletDrain(final double drain) {
        this.config = this.config.withMalp(this.config.malp().withTabletDrain(drain));
        return SGConfig.saveTabletDrain(drain);
    }

    /**
     * Turns logging gate events on or off, and writes it to the settings file.
     *
     * @return whether the settings file was written
     */
    public boolean setLogStargateEvents(final boolean log) {
        this.config = this.config.withLogStargateEvents(log);
        return SGConfig.saveLogStargateEvents(log);
    }

    /**
     * Writes a gate event to the server log, if the server's settings ask for it, as the original did: each gate in
     * the message is its world, position and address.
     */
    void log(final String message, final Stargate... gates) {
        if (this.config.logStargateEvents()) {
            SGCraft.LOG.info("STARGATE " + message, Arrays.stream(gates).map(this::describe).toArray());
        }
    }

    // A gate's world, position and address as a gate in its own world dials it, or ? if it has none
    private String describe(final Stargate gate) {
        final String address = this.addressing.addressOf(gate.position(), gate.position().dimension())
                .map(Address::toString).orElse("?");
        return gate.position().dimension().identifier() + " " + gate.position().pos().toShortString() + " "
                + address;
    }

    /** @return whether a gate is being placed whole, so its forming isn't a player building it */
    public boolean isPlacing() {
        return this.placing;
    }

    /**
     * How many times more a wormhole between two gates costs than the base cost, as in the original: more the further
     * apart they are, a great deal more again with an eighth chevron, which reaches another world, and a great deal
     * more than that with a ninth, which reaches another group of worlds, and more again to or from a supergate,
     * whichever end dials. The dialing gate's own multipliers apply where it has them.
     */
    double distanceFactor(final Stargate from, final Stargate to, final int chevrons) {
        final BlockPos a = from.position().pos();
        final BlockPos b = to.position().pos();
        final double d = Math.hypot(a.getX() - b.getX(), a.getZ() - b.getZ());
        final double scale = Math.log(0.05 * d + 1) / Math.log(0.05 * 16 * 279935);
        double factor = 1 + 14 * this.config.distanceFactorMultiplier(from) * scale * scale;
        // The eighth chevron reaches another world and takes a great deal more; the ninth, another group of worlds,
        // takes a great deal more again
        if (chevrons >= SGStargates.EIGHTH_CHEVRON) {
            factor *= this.config.interWorldMultiplier(from);
        }
        if (chevrons >= SGStargates.NINTH_CHEVRON) {
            factor *= this.config.otherGroupMultiplier();
        }
        if (from.size().radius() > GateShape.STANDARD.radius() || to.size().radius() > GateShape.STANDARD.radius()) {
            factor *= this.config.supergateEnergyMultiplier();
        }
        return factor;
    }

    void moving(final SGStargate gate, final Cause cause) {
        this.irises.put(gate, cause);
    }

    public MinecraftServer server() {
        return this.server;
    }

    SGAddressing addressing() {
        return this.addressing;
    }

    public GateStore store() {
        return this.server.getDataStorage().computeIfAbsent(GateStore.TYPE);
    }

    /** @return the gate whose base is at {@code position}, if a gate is formed there */
    public Optional<SGStargate> gate(final GlobalPos position) {
        if (this.store().gate(position).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(this.live.computeIfAbsent(position, pos -> new SGStargate(this, pos)));
    }

    /**
     * Records a gate that has just formed, keeping its chevrons, properties and owner if it was known before.
     *
     * @param frame the gate's frame, which gives the way its base faces, how it lies and its size
     * @return whether the gate is new, rather than known before
     */
    public boolean formed(final GlobalPos position, final ResourceKey<GateType> type, final GateFrame frame,
            final int chevrons) {
        final GateStore store = this.store();
        final Optional<GateRecord> known = store.gate(position);
        store.put(new GateRecord(position, type, frame.facing(), known.map(GateRecord::chevrons).orElse(chevrons),
                known.map(GateRecord::properties).orElseGet(CompoundTag::new),
                known.map(GateRecord::state).orElseGet(CompoundTag::new), frame.shape().radius(),
                frame.orientation(), known.flatMap(GateRecord::owner)));
        this.gate(position).ifPresent(SGStargate::sync);
        if (known.isEmpty()) {
            this.gate(position).ifPresent(gate -> this.log("ADDED {}", gate));
        }
        return known.isEmpty();
    }

    /**
     * Notes the player placing a block that may finish a gate's ring, before the block is placed, so the gate it
     * finishes is theirs. Forgotten when the gates next tick.
     */
    public void building(final GlobalPos block, final UUID player) {
        this.builders.put(block, player);
    }

    /**
     * Gives a gate that has just formed for the first time what the server's settings give new gates: an iris, and
     * to one a player built, more chevrons and the player as its owner.
     *
     * @param base the gate's base
     * @param built whether a player's blocks formed it, rather than a gate being placed whole
     */
    public void welcome(final GlobalPos position, final StargateBaseBlockEntity base, final boolean built) {
        final Optional<SGStargate> gate = this.gate(position);
        if (gate.isEmpty()) {
            return;
        }
        if (built) {
            final GateFrame frame = gate.get().frame();
            this.builders.entrySet().stream()
                    .filter(entry -> entry.getKey().dimension().equals(position.dimension())
                            && GateFormation.isPartOfGate(frame, entry.getKey().pos()))
                    .map(Map.Entry::getValue).findFirst()
                    .ifPresent(player -> this.store().gate(position).ifPresent(record ->
                            this.store().put(record.withOwner(Optional.of(player)))));
            final Optional<Integer> chevrons = this.formationDefault(SGConfig.CHEVRONS, SGStargates.CHEVRONS);
            if (chevrons.isPresent() && chevrons.get() > gate.get().chevrons()) {
                this.store().gate(position).ifPresent(record ->
                        this.store().put(record.withChevrons(chevrons.get())));
                base.giveChevrons(chevrons.get());
            }
        }
        final Optional<Identifier> irisId = this.propertyId(GateProperties.IRIS);
        final Optional<ResourceKey<IrisType>> iris = irisId.flatMap(id -> this.formationDefault(id,
                GateProperties.IRIS.codec()));
        if (iris.isPresent() && this.ownProperty(position, GateProperties.IRIS).isEmpty()
                && this.setProperty(position, GateProperties.IRIS, iris.get()) == ActionResult.DONE) {
            gate.get().irisState().fitted();
            gate.get().includeIris(true);
        }
        gate.get().sync();
    }

    // A value the server's settings give gates as they form
    private <T> Optional<T> formationDefault(final Identifier key, final Codec<T> codec) {
        return Optional.ofNullable(this.config.gateDefaults().get(key))
                .flatMap(json -> codec.parse(JsonOps.INSTANCE, json).result());
    }

    /** @return the player the gate at {@code position} belongs to, if any */
    Optional<UUID> owner(final GlobalPos position) {
        return this.store().gate(position).flatMap(GateRecord::owner);
    }

    void setOwner(final GlobalPos position, final Optional<UUID> owner) {
        this.store().gate(position).ifPresent(record -> this.store().put(record.withOwner(owner)));
    }

    /**
     * Forgets a gate that has been broken apart, ending its connection; or, if it was the gate an open wormhole reached
     * and the server lets wormholes jump, sending the wormhole to another gate in its address cell.
     */
    public void broken(final GlobalPos position, final Cause cause) {
        final Optional<SGStargate> gate = this.gate(position);
        gate.ifPresent(broken -> this.log("REMOVED {}", broken));
        final Runnable breach = gate.map(SGStargate::breach).orElse(() -> { });
        if (this.config.wormholesJump()) {
            gate.ifPresent(broken -> broken.liveConnection().ifPresent(connection -> connection.jump(broken, cause)));
        }
        this.live.remove(position);
        gate.ifPresent(SGStargate::gone);
        this.store().remove(position);
        gate.ifPresent(broken -> SGEventBus.get().post(new SGEvents.BreakPost(cause, broken)));
        breach.run();
    }

    /** Carries on the irises that were moving when the server last stopped. */
    private void restoreIrises() {
        for (final GateRecord record : this.store().gates()) {
            final IrisState state = SGIris.stateOf(record.state());
            // A raised shield goes on taking its energy, so it needs its ticks as a moving iris does
            if (state == IrisState.OPENING || state == IrisState.CLOSING || state == IrisState.CLOSED) {
                this.gate(record.position()).filter(gate -> state != IrisState.CLOSED
                        || gate.irisState().shieldRaised()).ifPresent(gate -> this.irises.put(gate,
                        Cause.of(this.server)));
            }
        }
    }

    void connect(final SGConnection connection, final DialMode mode) {
        this.connections.add(connection);
        connection.start(mode);
    }

    /** Moves every connection on by a tick. */
    public void tick() {
        this.builders.clear();
        this.connections.removeIf(SGConnection::tick);
        this.engaging.removeIf(SGStargate::releaseIfIdle);
        this.irises.entrySet().removeIf(entry -> !entry.getKey().exists()
                || entry.getKey().irisState().tick(entry.getValue()));
    }

    void engaging(final SGStargate gate) {
        this.engaging.add(gate);
    }

    /**
     * Forgets the connections as the server stops. They are saved as they go, so they carry on when it starts again.
     */
    public void stop() {
        this.connections.clear();
    }

    /** Carries on the connections that were under way when the server last stopped, and the moving irises. */
    public void restore() {
        this.restoreIrises();
        final GateStore store = this.store();
        for (final CompoundTag tag : store.connections()) {
            final Optional<SGConnection> connection = SGConnection.restore(this, tag);
            if (connection.isEmpty()) {
                tag.read("dialer", GlobalPos.CODEC).ifPresent(store::removeConnection);
                continue;
            }
            this.connections.add(connection.get());
            connection.get().resume();
        }
    }

    /** @return the other formed gates in the address cell holding a gate */
    List<SGStargate> cellMates(final SGStargate gate) {
        final int cell = this.addressing.partition(gate.position().dimension()).cellChunks();
        final ChunkPos home = ChunkPos.containing(gate.position().pos());
        final List<SGStargate> gates = new ArrayList<>();
        for (final GateRecord record : this.store().gates()) {
            final ChunkPos chunk = ChunkPos.containing(record.position().pos());
            if (record.position().dimension().equals(gate.position().dimension())
                    && !record.position().equals(gate.position())
                    && Math.floorDiv(chunk.x(), cell) == Math.floorDiv(home.x(), cell)
                    && Math.floorDiv(chunk.z(), cell) == Math.floorDiv(home.z(), cell)) {
                this.gate(record.position()).ifPresent(gates::add);
            }
        }
        return gates;
    }

    Optional<SGStargate> inCell(final AddressTarget target) {
        final int cell = this.addressing.partition(target.world()).cellChunks();
        return this.store().gates().stream()
                .filter(gate -> gate.position().dimension().equals(target.world()))
                .filter(gate -> {
                    final ChunkPos chunk = ChunkPos.containing(gate.position().pos());
                    return chunk.x() >= target.chunk().x() && chunk.x() < target.chunk().x() + cell
                            && chunk.z() >= target.chunk().z() && chunk.z() < target.chunk().z() + cell;
                })
                .findFirst()
                .flatMap(gate -> this.gate(gate.position()));
    }

    <T> Optional<T> property(final GlobalPos position, final GateProperty<T> property) {
        if (this.propertyId(property).isEmpty()) {
            return Optional.empty();
        }
        final Optional<T> own = this.ownProperty(position, property);
        return own.isPresent() ? own : this.inherited(position, property);
    }

    /** @return the gate's own value of a property, without the default */
    public <T> Optional<T> ownProperty(final GlobalPos position, final GateProperty<T> property) {
        final Optional<Identifier> id = this.propertyId(property);
        if (id.isEmpty()) {
            return Optional.empty();
        }
        return this.store().gate(position)
                .flatMap(gate -> Optional.ofNullable(gate.properties().get(id.get().toString())))
                .flatMap(tag -> property.codec().parse(NbtOps.INSTANCE, tag).result());
    }

    boolean isRegistered(final GateProperty<?> property) {
        return this.propertyId(property).isPresent();
    }

    <T> ActionResult setProperty(final GlobalPos position, final GateProperty<T> property, final T value) {
        final Optional<Identifier> id = this.propertyId(property);
        final Optional<GateRecord> gate = this.store().gate(position);
        if (id.isEmpty() || gate.isEmpty()) {
            return ActionResult.NOT_POSSIBLE;
        }
        final Optional<Tag> encoded = property.codec().encodeStart(NbtOps.INSTANCE, value).result();
        if (encoded.isEmpty()) {
            return ActionResult.NOT_POSSIBLE;
        }
        final CompoundTag properties = gate.get().properties();
        if (encoded.get().equals(properties.get(id.get().toString()))) {
            return ActionResult.NO_CHANGE;
        }
        properties.put(id.get().toString(), encoded.get());
        this.store().put(gate.get().withProperties(properties));
        return ActionResult.DONE;
    }

    ActionResult removeProperty(final GlobalPos position, final GateProperty<?> property) {
        final Optional<Identifier> id = this.propertyId(property);
        final Optional<GateRecord> gate = this.store().gate(position);
        if (id.isEmpty() || gate.isEmpty()) {
            return ActionResult.NOT_POSSIBLE;
        }
        final CompoundTag properties = gate.get().properties();
        if (!properties.contains(id.get().toString())) {
            return ActionResult.NO_CHANGE;
        }
        properties.remove(id.get().toString());
        this.store().put(gate.get().withProperties(properties));
        return ActionResult.DONE;
    }

    private Optional<Identifier> propertyId(final GateProperty<?> property) {
        return Optional.ofNullable(this.server.registryAccess().lookupOrThrow(SGCraftRegistries.GATE_PROPERTY)
                .getKey(property));
    }

    @Override
    public Optional<Stargate> at(final GlobalPos position) {
        for (final GateRecord gate : this.store().gates()) {
            if (gate.position().dimension().equals(position.dimension())
                    && GateFormation.isPartOfGate(gate.frame(), position.pos())) {
                return this.gate(gate.position()).map(Stargate.class::cast);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Stargate> byAddress(final Address address, final ResourceKey<Level> from) {
        return this.addressing.resolve(address, from).flatMap(this::inCell).map(Stargate.class::cast);
    }

    @Override
    public Collection<Stargate> in(final ResourceKey<Level> world) {
        final List<Stargate> gates = new ArrayList<>();
        for (final GateRecord gate : this.store().gates()) {
            if (gate.position().dimension().equals(world)) {
                this.gate(gate.position()).ifPresent(gates::add);
            }
        }
        return gates;
    }

    /** Chooses the gate in the same world whose base is nearest. */
    @Override
    public Optional<Stargate> mostAppropriate(final GlobalPos position) {
        final BlockPos pos = position.pos();
        return this.in(position.dimension()).stream()
                .min(Comparator.comparingDouble(gate -> gate.position().pos().distSqr(pos)));
    }

    /** Chooses the nearest gate in the same world that the cause may use. */
    @Override
    public Optional<Stargate> mostAppropriate(final GlobalPos position, final Cause cause) {
        final BlockPos pos = position.pos();
        return this.in(position.dimension()).stream()
                .filter(gate -> gate.access().allows(cause, GateRight.USE))
                .min(Comparator.comparingDouble(gate -> gate.position().pos().distSqr(pos)));
    }

    @Override
    public PlacementResult place(final GatePlacement placement, final Cause cause) {
        final SGEvents.PlacePre pre = SGEventBus.get().post(new SGEvents.PlacePre(cause, placement));
        if (pre.isCanceled()) {
            return GatePlacer.Result.of(PlacementResult.Status.CANCELED);
        }
        this.placing = true;
        try {
            final PlacementResult result = new GatePlacer(this, cause).place(pre.placement());
            // A gate a player placed is theirs
            result.gate().ifPresent(gate -> cause.first(Player.class).ifPresent(player ->
                    this.setOwner(gate.position(), Optional.of(player.getUUID()))));
            result.gate().ifPresent(gate -> SGEventBus.get().post(new SGEvents.PlacePost(cause, gate,
                    pre.placement().withPosition(gate.position()))));
            return result;
        } finally {
            this.placing = false;
        }
    }
}
