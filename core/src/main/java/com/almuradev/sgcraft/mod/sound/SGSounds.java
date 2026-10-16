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

package com.almuradev.sgcraft.mod.sound;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.gate.DialStyle;
import com.almuradev.sgcraft.mod.gate.GateShape;
import com.almuradev.sgcraft.mod.registry.RegistrySink;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;

/**
 * The gates' sounds, from the original SGCraft. A Universe gate sounds like a Milky Way gate, as it did there.
 */
public final class SGSounds {

    // How far away a standard gate can be heard, in blocks. Sounds fade evenly to nothing at this distance, so a gate
    // is at half volume 18 blocks away. A supergate carries further by however much bigger it is, as in the original
    private static final float RANGE = 36.0F;
    // How far a sound played at full volume carries, which louder sounds carry further than
    private static final float VANILLA_RANGE = 16.0F;

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
    public static final SoundEvent TELEPORT = SGSounds.fixed("teleport");
    public static final SoundEvent MILKY_WAY_DHD_PRESS = SGSounds.fixed("milkyway.dhd_press");
    public static final SoundEvent MILKY_WAY_DHD_DIAL = SGSounds.fixed("milkyway.dhd_dial");
    public static final SoundEvent PEGASUS_DHD_PRESS = SGSounds.fixed("pegasus.dhd_press");
    public static final SoundEvent IRIS_OPEN = SGSounds.create("iris_open");
    public static final SoundEvent IRIS_CLOSE = SGSounds.create("iris_close");
    public static final SoundEvent IRIS_HIT = SGSounds.create("iris_hit");
    // A shield rising and falling, in place of the iris's sounds; the city shield's too
    public static final SoundEvent SHIELD_UP = SGSounds.create("shield_up");
    public static final SoundEvent SHIELD_DOWN = SGSounds.create("shield_down");
    // An Ori supergate's segments linking as it dials
    public static final SoundEvent ORI_LINK = SGSounds.create("ori_link");
    // A storm sending a wormhole astray
    public static final SoundEvent MALFUNCTION = SGSounds.create("malfunction");

    private static final SoundEvent[] ALL = {
            SGSounds.MILKY_WAY_ROLL, SGSounds.MILKY_WAY_CHEVRON_OUTGOING, SGSounds.MILKY_WAY_CHEVRON_INCOMING,
            SGSounds.MILKY_WAY_OPEN, SGSounds.MILKY_WAY_FAIL, SGSounds.PEGASUS_ROLL, SGSounds.PEGASUS_CHEVRON_OUTGOING,
            SGSounds.PEGASUS_CHEVRON_INCOMING, SGSounds.PEGASUS_OPEN, SGSounds.PEGASUS_FAIL, SGSounds.EVENT_HORIZON,
            SGSounds.CLOSE, SGSounds.TELEPORT, SGSounds.MILKY_WAY_DHD_PRESS, SGSounds.MILKY_WAY_DHD_DIAL,
            SGSounds.PEGASUS_DHD_PRESS, SGSounds.IRIS_OPEN, SGSounds.IRIS_CLOSE, SGSounds.IRIS_HIT,
            SGSounds.SHIELD_UP, SGSounds.SHIELD_DOWN, SGSounds.ORI_LINK, SGSounds.MALFUNCTION,
    };

    private SGSounds() {
    }

    // A gate's sound, which carries as far as the volume it is played at says; see volume
    private static SoundEvent create(final String name) {
        return SoundEvent.createVariableRangeEvent(SGCraft.id(name));
    }

    // A sound that carries a standard gate's distance however it is played
    private static SoundEvent fixed(final String name) {
        return SoundEvent.createFixedRangeEvent(SGCraft.id(name), SGSounds.RANGE);
    }

    /**
     * @return the volume to play a gate's sounds at, so they carry {@link #RANGE} blocks from a standard gate and
     *     further from a supergate, by however many blocks bigger it is; above 1, a sound is no louder, only heard
     *     further away
     */
    public static float volume(final GateShape shape) {
        final int bigger = Math.max(0, shape.radius() - GateShape.STANDARD.radius());
        return (SGSounds.RANGE + bigger) / SGSounds.VANILLA_RANGE;
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

    /** @return the sound of a chevron locking, on the gate that dialed or the gate that was dialed */
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

    /** @return the sound of a DHD's symbol being pressed, or its center */
    public static SoundEvent dhdPress(final DialStyle style, final boolean center) {
        if (style == DialStyle.PEGASUS) {
            return SGSounds.PEGASUS_DHD_PRESS;
        }
        return center ? SGSounds.MILKY_WAY_DHD_DIAL : SGSounds.MILKY_WAY_DHD_PRESS;
    }

    /** @return the sound of a dial failing */
    public static SoundEvent fail(final DialStyle style) {
        return style == DialStyle.PEGASUS ? SGSounds.PEGASUS_FAIL : SGSounds.MILKY_WAY_FAIL;
    }
}
