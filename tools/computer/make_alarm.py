"""Makes the dialing computer's alarm, core/src/main/resources/assets/sgcraft/sounds/computer_alarm.ogg: a klaxon
blaring in pulses, in the style of a base's alarm, made from scratch so it loops cleanly. Replace the file with any
other mono Ogg Vorbis sound to change it.

Run from the repository root: python tools/computer/make_alarm.py
Needs numpy and soundfile.
"""

from pathlib import Path

import numpy as np
import soundfile

OUT = Path('core/src/main/resources/assets/sgcraft/sounds/computer_alarm.ogg')
RATE = 44100
# One pulse: the horn sweeping up, then quiet; the loop is two pulses
PULSE = 0.95
HORN = 0.7
LOW = 380.0
HIGH = 560.0


def pulse():
    t = np.arange(int(PULSE * RATE)) / RATE
    horn = t < HORN
    # The pitch sweeps up through the horn and holds, as a motor-driven klaxon winds up
    sweep = np.clip(t / (HORN * 0.6), 0, 1)
    pitch = LOW + (HIGH - LOW) * (1 - (1 - sweep) ** 2)
    phase = 2 * np.pi * np.cumsum(pitch) / RATE
    # A buzzing horn: a square wave softened, with a little of its octave
    tone = np.tanh(3 * np.sin(phase)) + 0.35 * np.tanh(2 * np.sin(2 * phase))
    # A quick swell in and a fade out, so the pulses don't click
    attack = np.clip(t / 0.03, 0, 1)
    release = np.clip((HORN - t) / 0.08, 0, 1)
    envelope = attack * release * horn
    return tone * envelope


def main():
    one = pulse()
    sound = np.concatenate([one, one])
    # Rounds off the harshest edges with a short moving average
    kernel = np.ones(6) / 6
    sound = np.convolve(sound, kernel, mode='same')
    sound = 0.8 * sound / np.max(np.abs(sound))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    soundfile.write(OUT, sound.astype(np.float32), RATE, format='OGG', subtype='VORBIS')
    print(f'Wrote {OUT}, {len(sound) / RATE:.2f} seconds')


if __name__ == '__main__':
    main()
