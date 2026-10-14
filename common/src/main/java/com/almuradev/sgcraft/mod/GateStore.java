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

package com.almuradev.sgcraft.mod;

import com.almuradev.sgcraft.SGCraft;
import com.almuradev.sgcraft.api.SGCraftRegistries;
import com.almuradev.sgcraft.api.address.Symbol;
import com.almuradev.sgcraft.api.gate.GateType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Every formed gate on the server, kept with the server's data so gates in unloaded chunks can still be found and
 * dialled, and the symbol each world was given within its group.
 */
public final class GateStore extends SavedData {

    private static final Codec<Symbol> SYMBOL_CODEC = Codec.STRING.comapFlatMap(
            text -> text.length() == 1
                    ? Symbol.of(text.charAt(0)).map(DataResult::success)
                            .orElseGet(() -> DataResult.error(() -> "Not a symbol: " + text))
                    : DataResult.error(() -> "Not a symbol: " + text),
            symbol -> String.valueOf(symbol.character()));

    private static final Codec<GateStore> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GateRecord.CODEC.listOf().optionalFieldOf("gates", List.of())
                    .forGetter(store -> List.copyOf(store.gates.values())),
            Codec.unboundedMap(ResourceKey.codec(Registries.DIMENSION), GateStore.SYMBOL_CODEC)
                    .optionalFieldOf("world_symbols", Map.of())
                    .forGetter(store -> store.worldSymbols),
            CompoundTag.CODEC.listOf().optionalFieldOf("connections", List.of())
                    .forGetter(store -> List.copyOf(store.connections.values()))
    ).apply(instance, GateStore::new));

    public static final SavedDataType<GateStore> TYPE =
            new SavedDataType<>(SGCraft.id("gates"), GateStore::new, GateStore.CODEC, null);

    private final Map<GlobalPos, GateRecord> gates = new LinkedHashMap<>();
    private final Map<ResourceKey<Level>, Symbol> worldSymbols = new LinkedHashMap<>();
    // Each connection under way, by its dialling gate, as the connection saves itself
    private final Map<GlobalPos, CompoundTag> connections = new LinkedHashMap<>();

    private GateStore() {
    }

    private GateStore(final List<GateRecord> gates, final Map<ResourceKey<Level>, Symbol> worldSymbols,
            final List<CompoundTag> connections) {
        gates.forEach(gate -> this.gates.put(gate.position(), gate));
        this.worldSymbols.putAll(worldSymbols);
        connections.forEach(tag -> tag.read("dialler", GlobalPos.CODEC)
                .ifPresent(dialler -> this.connections.put(dialler, tag)));
    }

    /** @return each connection under way when last saved, as it saved itself */
    public Collection<CompoundTag> connections() {
        return List.copyOf(this.connections.values());
    }

    public void putConnection(final GlobalPos dialler, final CompoundTag connection) {
        this.connections.put(dialler, connection);
        this.setDirty();
    }

    public void removeConnection(final GlobalPos dialler) {
        if (this.connections.remove(dialler) != null) {
            this.setDirty();
        }
    }

    public Collection<GateRecord> gates() {
        return Collections.unmodifiableCollection(this.gates.values());
    }

    public Optional<GateRecord> gate(final GlobalPos position) {
        return Optional.ofNullable(this.gates.get(position));
    }

    public void put(final GateRecord gate) {
        if (!gate.equals(this.gates.put(gate.position(), gate))) {
            this.setDirty();
        }
    }

    public void remove(final GlobalPos position) {
        if (this.gates.remove(position) != null) {
            this.setDirty();
        }
    }

    /** @return the symbol each world has been given, in the order they were given */
    public Map<ResourceKey<Level>, Symbol> worldSymbols() {
        return Collections.unmodifiableMap(this.worldSymbols);
    }

    public void putWorldSymbol(final ResourceKey<Level> world, final Symbol symbol) {
        if (!symbol.equals(this.worldSymbols.put(world, symbol))) {
            this.setDirty();
        }
    }

    /**
     * A formed gate as it is saved: where its base is, its type, which way it faces, how many chevrons it has, and
     * the values of its properties that differ from their defaults, by each property's registry name.
     */
    public record GateRecord(GlobalPos position, ResourceKey<GateType> type, Direction facing, int chevrons,
            CompoundTag properties) {

        static final Codec<GateRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                GlobalPos.CODEC.fieldOf("position").forGetter(GateRecord::position),
                ResourceKey.codec(SGCraftRegistries.GATE_TYPE).fieldOf("type").forGetter(GateRecord::type),
                Direction.CODEC.fieldOf("facing").forGetter(GateRecord::facing),
                Codec.INT.fieldOf("chevrons").forGetter(GateRecord::chevrons),
                CompoundTag.CODEC.optionalFieldOf("properties", new CompoundTag()).forGetter(GateRecord::properties)
        ).apply(instance, GateRecord::new));

        public GateRecord {
            properties = properties.copy();
        }

        @Override
        public CompoundTag properties() {
            return this.properties.copy();
        }

        public GateRecord withChevrons(final int chevrons) {
            return new GateRecord(this.position, this.type, this.facing, chevrons, this.properties);
        }

        public GateRecord withType(final ResourceKey<GateType> type) {
            return new GateRecord(this.position, type, this.facing, this.chevrons, this.properties);
        }

        public GateRecord withProperties(final CompoundTag properties) {
            return new GateRecord(this.position, this.type, this.facing, this.chevrons, properties);
        }
    }
}
