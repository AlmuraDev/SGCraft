"""Makes the puddle jumper's sounds, in core/src/main/resources/assets/sgcraft/sounds/, made from scratch in the
style of the show's gate ship: a soft, smooth, low electronic hum, gently pulsing, with only a breath of air and a
faint, high shimmer over it, as it hums while its drive is on; and the hum swelling up as the drive comes on and
sighing down as it goes off.

    jumper_hum.ogg        the drive's hum, looping cleanly; the game raises its pitch and volume with the thrust
    jumper_power_on.ogg   the drive coming on
    jumper_power_off.ogg  the drive going off

Replace any of them with another mono Ogg Vorbis sound to change it.

Run from the repository root: python tools/jumper/make_jumper_sounds.py
Needs numpy and soundfile.
"""

from pathlib import Path

import numpy as np
import soundfile

OUT = Path('core/src/main/resources/assets/sgcraft/sounds')
RATE = 44100
# The hum's loop, in seconds; every tone in it fits a whole number of cycles into it, so it loops without a seam
LOOP = 4.0
# The drone's root, and the whine over it, in cycles a second, as whole numbers of cycles in the loop
ROOT = 55.0
WHINE = 660.0


def tone(t, frequency, wobble=0.0, wobble_rate=0.25):
    """A sine at a pitch, its pitch swaying a little and slowly, by a share of it; the sway's rate fits the loop."""
    phase = 2 * np.pi * frequency * t
    if wobble:
        phase += wobble * frequency / wobble_rate * np.sin(2 * np.pi * wobble_rate * t)
    return np.sin(phase)


def looped_noise(samples, smooth, seed):
    """Noise, softened by a moving average of so many samples, wrapped round so its end runs into its start."""
    rng = np.random.default_rng(seed)
    kernel = np.ones(smooth) / smooth
    # Wrapped: the moving average's window reaches round from the end to the start
    noise = rng.standard_normal(samples)
    padded = np.concatenate([noise[-smooth:], noise])
    soft = np.convolve(padded, kernel, mode='valid')[1:samples + 1]
    return soft / np.max(np.abs(soft))


def soften(sound, smooth):
    """Rounds off a looping sound's highs with a moving average wrapped round its ends."""
    kernel = np.ones(smooth) / smooth
    padded = np.concatenate([sound[-smooth:], sound, sound[:smooth]])
    return np.convolve(padded, kernel, mode='same')[smooth:-smooth]


def hum():
    samples = int(LOOP * RATE)
    t = np.arange(samples) / RATE
    # The drone: its root and the octave over it, the octave a touch detuned so the two beat slowly, and a little of
    # the fifth, all rounded off, so it is a smooth, warm hum rather than a buzz
    drone = (tone(t, ROOT) + 0.7 * tone(t, ROOT * 2, 0.0015, 0.25) + 0.25 * tone(t, ROOT * 3, 0.002, 0.5))
    # A slow, gentle pulse, as the drive breathes
    drone *= 0.8 + 0.2 * np.sin(2 * np.pi * 0.5 * t) ** 2
    # The shimmer: high and very faint, wavering
    shimmer = tone(t, WHINE, 0.003, 0.75)
    shimmer *= 0.5 + 0.5 * np.sin(2 * np.pi * 0.25 * t) ** 2
    # The air: a low, soft breath
    air = soften(looped_noise(samples, 12, 1), 24)
    air /= np.max(np.abs(air))
    sound = 0.8 * drone + 0.03 * shimmer + 0.1 * air
    sound = soften(sound, 8)
    return 0.6 * sound / np.max(np.abs(sound))


def sweep(seconds, up):
    """The drive coming on, or going off: the whine sweeping up from a growl, or down to it, with the drone and
    the air swelling in, or dying away."""
    samples = int(seconds * RATE)
    t = np.arange(samples) / RATE
    x = t / seconds
    rise = x if up else 1 - x
    # Eased, quick at first and settling as it reaches the hum, or falling away slowly then quickly
    eased = 1 - (1 - rise) ** 3 if up else rise ** 1.5
    pitch = 0.25 + 0.75 * eased
    shimmer = np.sin(2 * np.pi * np.cumsum(WHINE * pitch) / RATE)
    drone = np.sin(2 * np.pi * np.cumsum(ROOT * (0.5 + 0.5 * eased)) / RATE)
    drone += 0.7 * np.sin(2 * np.pi * np.cumsum(ROOT * 2 * (0.5 + 0.5 * eased)) / RATE)
    air = soften(looped_noise(samples, 12, 2), 24)
    air /= np.max(np.abs(air))
    sound = (0.8 * drone + 0.06 * shimmer + 0.1 * air) * eased
    sound = soften(sound, 8)
    # A soft start and end, so it doesn't click
    sound *= np.clip(t / 0.02, 0, 1) * np.clip((seconds - t) / 0.08, 0, 1)
    return 0.6 * sound / np.max(np.abs(sound))


def write(name, sound):
    OUT.mkdir(parents=True, exist_ok=True)
    path = OUT / name
    soundfile.write(path, sound.astype(np.float32), RATE, format='OGG', subtype='VORBIS')
    print(f'Wrote {path}, {len(sound) / RATE:.2f} seconds')


def main():
    write('jumper_hum.ogg', hum())
    write('jumper_power_on.ogg', sweep(1.6, True))
    write('jumper_power_off.ogg', sweep(2.0, False))


if __name__ == '__main__':
    main()
