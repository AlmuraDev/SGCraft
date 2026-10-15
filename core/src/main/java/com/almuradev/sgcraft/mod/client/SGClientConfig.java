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

package com.almuradev.sgcraft.mod.client;

import com.almuradev.sgcraft.mod.Platform;
import com.almuradev.sgcraft.mod.SGCraft;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This player's own settings for how SGCraft looks and sounds, read from {@code config/sgcraft-client.json} as the
 * client starts. They change nothing on the server or for other players. A missing file is written with the
 * defaults, and a missing setting takes its default. Each matches an option of the original SGCraft.
 *
 * @param hdEventHorizon whether the event horizon is drawn with its detailed texture, or the original's plain one
 * @param copyAddressOnOpen whether opening a gate's base screen copies the gate's address to the clipboard; off
 *     unless set, as in the original
 * @param soundVolume how loud SGCraft's sounds play, from 0 for silent to 1 for full volume, on top of the game's
 *     own volume settings
 */
public record SGClientConfig(boolean hdEventHorizon, boolean copyAddressOnOpen, double soundVolume) {

    public static final SGClientConfig DEFAULTS = new SGClientConfig(true, false, 1.0);

    private static final String FILE = "sgcraft-client.json";

    public static final Codec<SGClientConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("hd_event_horizon", SGClientConfig.DEFAULTS.hdEventHorizon)
                    .forGetter(SGClientConfig::hdEventHorizon),
            Codec.BOOL.optionalFieldOf("copy_address_on_open", SGClientConfig.DEFAULTS.copyAddressOnOpen)
                    .forGetter(SGClientConfig::copyAddressOnOpen),
            Codec.doubleRange(0, 1).optionalFieldOf("sound_volume", SGClientConfig.DEFAULTS.soundVolume)
                    .forGetter(SGClientConfig::soundVolume)
    ).apply(instance, SGClientConfig::new));

    private static SGClientConfig current = SGClientConfig.DEFAULTS;

    /** @return the settings the client started with, or the defaults before then */
    public static SGClientConfig get() {
        return SGClientConfig.current;
    }

    /**
     * @param sound the sound's identifier
     * @param volume how loud the game would play it, from 0 to 1
     * @return how loud to play it: SGCraft's own sounds as {@link #soundVolume()} says, and others as they are
     */
    public float volume(final Identifier sound, final float volume) {
        return sound.getNamespace().equals(SGCraft.MOD_ID) ? volume * (float) this.soundVolume : volume;
    }

    /** Reads the settings, writing the file with the defaults first if there is none. */
    static void load() {
        final Path path = Platform.get().configDir().resolve(SGClientConfig.FILE);
        try {
            if (Files.notExists(path)) {
                Files.createDirectories(path.getParent());
                final JsonElement json = SGClientConfig.CODEC.encodeStart(JsonOps.INSTANCE, SGClientConfig.DEFAULTS)
                        .getOrThrow();
                Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(json));
                SGClientConfig.current = SGClientConfig.DEFAULTS;
                return;
            }
            final JsonElement json = JsonParser.parseString(Files.readString(path));
            SGClientConfig.current = SGClientConfig.CODEC.parse(JsonOps.INSTANCE, json).resultOrPartial(error ->
                    SGCraft.LOG.error("{} has a mistake, so some settings take their defaults: {}", path, error))
                    .orElse(SGClientConfig.DEFAULTS);
        } catch (final IOException | RuntimeException e) {
            SGCraft.LOG.error("Couldn't read {}, so every setting takes its default", path, e);
            SGClientConfig.current = SGClientConfig.DEFAULTS;
        }
    }
}
