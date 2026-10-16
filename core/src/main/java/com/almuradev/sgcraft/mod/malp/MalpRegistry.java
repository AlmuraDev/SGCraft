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

package com.almuradev.sgcraft.mod.malp;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.entity.MalpEntity;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Every MALP on the server, kept with the server's data: where each was last seen, its name, its battery's charge,
 * its console, its owner and its settings. Consoles list their MALPs from here, so a MALP whose chunk is unloaded is
 * still listed and found. Each MALP tells it what it is doing as it ticks, and as it unloads; one destroyed or packed
 * up is taken off.
 */
public final class MalpRegistry extends SavedData {

    /**
     * A MALP as last seen.
     *
     * @param id the MALP's UUID
     * @param position where it was, by block
     * @param name what it is called
     * @param energy the gate energy in its battery
     * @param console the console it is paired with, if any
     * @param owner the player it belongs to, if any
     * @param options its own settings
     */
    public record Entry(UUID id, GlobalPos position, String name, double energy, Optional<GlobalPos> console,
            Optional<UUID> owner, MalpOptions options) {

        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(Entry::id),
                GlobalPos.CODEC.fieldOf("position").forGetter(Entry::position),
                Codec.STRING.fieldOf("name").forGetter(Entry::name),
                Codec.DOUBLE.optionalFieldOf("energy", 0.0).forGetter(Entry::energy),
                GlobalPos.CODEC.optionalFieldOf("console").forGetter(Entry::console),
                UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(Entry::owner),
                MalpOptions.CODEC.optionalFieldOf("options", MalpOptions.DEFAULTS).forGetter(Entry::options)
        ).apply(instance, Entry::new));
    }

    private static final Codec<MalpRegistry> CODEC = Entry.CODEC.listOf().xmap(MalpRegistry::new,
            registry -> List.copyOf(registry.entries.values()));

    public static final SavedDataType<MalpRegistry> TYPE =
            new SavedDataType<>(SGCraft.id("malps"), MalpRegistry::new, MalpRegistry.CODEC, null);

    private final Map<UUID, Entry> entries = new LinkedHashMap<>();

    private MalpRegistry() {
    }

    private MalpRegistry(final List<Entry> entries) {
        entries.forEach(entry -> this.entries.put(entry.id(), entry));
    }

    /** @return the server's registry of MALPs */
    public static MalpRegistry get(final MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(MalpRegistry.TYPE);
    }

    /** Records what a MALP is doing now. */
    public void update(final MalpEntity malp) {
        final Entry entry = new Entry(malp.getUUID(), GlobalPos.of(malp.level().dimension(), malp.blockPosition()),
                malp.displayName(), malp.energy(), Optional.ofNullable(malp.console()),
                Optional.ofNullable(malp.owner()), malp.options());
        if (!entry.equals(this.entries.put(entry.id(), entry))) {
            this.setDirty();
        }
    }

    /** Takes a MALP off, such as when it is destroyed or packed up. */
    public void remove(final UUID id) {
        if (this.entries.remove(id) != null) {
            this.setDirty();
        }
    }

    /** @return the MALP with this UUID, as last seen */
    public Optional<Entry> get(final UUID id) {
        return Optional.ofNullable(this.entries.get(id));
    }

    /** @return every MALP, as last seen, by name */
    public List<Entry> all() {
        return this.entries.values().stream().sorted(Comparator.comparing(Entry::name)).toList();
    }

    /** @return the MALPs paired with a console, as last seen, by name */
    public List<Entry> pairedWith(final GlobalPos console) {
        return this.all().stream().filter(entry -> entry.console().filter(console::equals).isPresent()).toList();
    }

    /** @return the MALP with this UUID, if its chunk is loaded */
    public static @Nullable MalpEntity find(final MinecraftServer server, final UUID id) {
        final Optional<Entry> entry = MalpRegistry.get(server).get(id);
        if (entry.isPresent()) {
            final ServerLevel level = server.getLevel(entry.get().position().dimension());
            if (level != null && level.getEntity(id) instanceof MalpEntity malp) {
                return malp;
            }
        }
        for (final ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(id) instanceof MalpEntity malp) {
                return malp;
            }
        }
        return null;
    }

    /** @return the MALP whose name, or the start of whose UUID, is this, ignoring case */
    public Optional<Entry> byName(final String name) {
        return this.all().stream().filter(entry -> entry.name().equalsIgnoreCase(name)
                || entry.id().toString().startsWith(name.toLowerCase(Locale.ROOT)))
                .findFirst();
    }
}
