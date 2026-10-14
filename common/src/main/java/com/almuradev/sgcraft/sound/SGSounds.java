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

package com.almuradev.sgcraft.sound;

import com.almuradev.sgcraft.SGCraft;
import com.almuradev.sgcraft.gate.DialStyle;
import com.almuradev.sgcraft.registry.RegistrySink;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;

/**
 * The gates' sounds, from the original SGCraft. A Universe gate sounds like a Milky Way gate, as it did there.
 */
public final class SGSounds {

    // How far away a gate can be heard, in blocks. Sounds fade evenly to nothing at this distance, so a gate is at half
    // volume 18 blocks away
    private static final float RANGE = 36.0F;

    public static final SoundEvent MILKY_WAY_ROLL = SGSounds.create("milkyway.gate_roll");
    public static final SoundEvent MILKY_WAY_CHEVRON_OUTGOING = SGSounds.create("milkyway.chevron_outgoing");
    public static final SoundEvent MILKY_WAY_CHEVRON_INCOMING = SGSounds.create("milkyway.chevron_incoming");
    public static final SoundEvent MILKY_WAY_OPEN = SGSounds.create("milkyway.gate_open");
    public static final SoundEvent MILKY_WAY_FAIL = SGSounds.create("milkyway.dial_fail");
    public static final SoundEvent PEGASUS_ROLL = SGSounds.create("pegasus.gate_roll");
    public static final SoundEvent PEGASUS_CHEVRON_OUTGOING = SGSounds.create("pegasus.chevron_outgoing");
    public static final SoundEvent PEGASUS_CHEVRON_INCOMING = SGSounds.create("pegasus.chevron_incoming");
    public static final SoundEvent PEGASUS_OPEN = SGSounds.create("pegasus.gate_open");
    public static final SoundEvent PEGASUS_FAIL = SGSounds.create("pegasus.dial_fail");
    public static final SoundEvent EVENT_HORIZON = SGSounds.create("event_horizon");
    public static final SoundEvent CLOSE = SGSounds.create("gate_close");
    public static final SoundEvent TELEPORT = SGSounds.create("teleport");

    private static final SoundEvent[] ALL = {
            SGSounds.MILKY_WAY_ROLL, SGSounds.MILKY_WAY_CHEVRON_OUTGOING, SGSounds.MILKY_WAY_CHEVRON_INCOMING,
            SGSounds.MILKY_WAY_OPEN, SGSounds.MILKY_WAY_FAIL, SGSounds.PEGASUS_ROLL, SGSounds.PEGASUS_CHEVRON_OUTGOING,
            SGSounds.PEGASUS_CHEVRON_INCOMING, SGSounds.PEGASUS_OPEN, SGSounds.PEGASUS_FAIL, SGSounds.EVENT_HORIZON,
            SGSounds.CLOSE, SGSounds.TELEPORT,
    };

    private SGSounds() {
    }

    private static SoundEvent create(final String name) {
        return SoundEvent.createFixedRangeEvent(SGCraft.id(name), SGSounds.RANGE);
    }

    public static void register(final RegistrySink<SoundEvent> sink) {
        for (final SoundEvent sound : SGSounds.ALL) {
            sink.register(ResourceKey.create(Registries.SOUND_EVENT, sound.location()), sound);
        }
    }

    /** @return the sound of the ring turning, or the light running, while a gate of this style dials */
    public static SoundEvent roll(final DialStyle style) {
        return style == DialStyle.PEGASUS ? SGSounds.PEGASUS_ROLL : SGSounds.MILKY_WAY_ROLL;
    }

    /** @return the sound of a chevron locking, on the gate that dialled or the gate that was dialled */
    public static SoundEvent chevron(final DialStyle style, final boolean outgoing) {
        if (style == DialStyle.PEGASUS) {
            return outgoing ? SGSounds.PEGASUS_CHEVRON_OUTGOING : SGSounds.PEGASUS_CHEVRON_INCOMING;
        }
        return outgoing ? SGSounds.MILKY_WAY_CHEVRON_OUTGOING : SGSounds.MILKY_WAY_CHEVRON_INCOMING;
    }

    /** @return the sound of the wormhole forming */
    public static SoundEvent open(final DialStyle style) {
        return style == DialStyle.PEGASUS ? SGSounds.PEGASUS_OPEN : SGSounds.MILKY_WAY_OPEN;
    }

    /** @return the sound of a dial failing */
    public static SoundEvent fail(final DialStyle style) {
        return style == DialStyle.PEGASUS ? SGSounds.PEGASUS_FAIL : SGSounds.MILKY_WAY_FAIL;
    }
}
