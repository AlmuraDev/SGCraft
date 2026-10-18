"""Makes the dialing computer's key presses from unicaegames' Keyboard Soundpack 1 (CC0), recorded on a Cherry
KC 1000: https://opengameart.org/content/keyboard-soundpack-1-typing-and-single-keystrokes

Six of its single keystrokes become core/src/main/resources/assets/sgcraft/sounds/computer_key_1.ogg to
computer_key_6.ogg, which the game picks among at random for the symbol keys, and two of its deeper ones become
computer_key_big_1.ogg and computer_key_big_2.ogg, for the larger keys. Each is cut to start at its tap, faded at its
end and brought to the same loudness.

Run from the repository root, with the folder the pack's zip was unpacked into:
python tools/computer/import_keys.py <folder>
Needs numpy and soundfile.
"""

import sys
from pathlib import Path

import numpy as np
import soundfile

OUT = Path('core/src/main/resources/assets/sgcraft/sounds')
KEYS = [7, 13, 17, 19, 1, 30]
BIG_KEYS = [4, 6]
# How much quiet is kept before each tap, how long each sound is at most, and how long its end fades
LEAD = 0.002
LENGTH = 0.2
FADE = 0.03
PEAK = 0.8


def prepare(path):
    sound, rate = soundfile.read(path)
    if sound.ndim > 1:
        sound = sound.mean(axis=1)
    level = np.abs(sound)
    onset = int(np.argmax(level > 0.05 * level.max()))
    start = max(0, onset - int(LEAD * rate))
    sound = sound[start:start + int(LENGTH * rate)]
    t = np.arange(len(sound)) / rate
    sound = sound * np.clip((len(sound) / rate - t) / FADE, 0, 1)
    return PEAK * sound / np.max(np.abs(sound)), rate


def main():
    keys = Path(sys.argv[1]) / 'Single Keys'
    OUT.mkdir(parents=True, exist_ok=True)
    for name, picks in (('computer_key', KEYS), ('computer_key_big', BIG_KEYS)):
        for i, pick in enumerate(picks, 1):
            sound, rate = prepare(keys / f'keypress-{pick:03d}.wav')
            path = OUT / f'{name}_{i}.ogg'
            soundfile.write(path, sound.astype(np.float32), rate, format='OGG', subtype='VORBIS')
            print(f'Wrote {path}')


if __name__ == '__main__':
    main()
