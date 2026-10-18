"""Draws the DHD screen's art: each style's face, of the block's own marble, with its two rings of buttons and no glyphs, which the screen
draws over it; a soft glow for lit buttons; and the center dome seen from above. Writes PNGs into the mod's textures.

Run from the repository root: python tools/dhd/make_screen_art.py
Needs numpy and Pillow. The layout must match DhdLayout in the mod: the outer ring has 26 buttons, the inner ring 13.
"""

import math
import struct
import zlib
from pathlib import Path

import numpy as np
from PIL import Image

OUT = Path('core/src/main/resources/assets/sgcraft/textures/gui/dhd')
BLOCK = Path('core/src/main/resources/assets/sgcraft/textures/block')
# How many of the face's pixels each of the block texture's pixels takes
PIXEL = 4
SIZE = 512
# Radii as fractions of the face's radius; DhdLayout uses the same
RIM, OUTER, BETWEEN, INNER = 1.0, 0.94, 0.58, 0.28
OUTER_BUTTONS, INNER_BUTTONS = 26, 13
GAP = 0.012


def write_png(path, rgba):
    h, w, _ = rgba.shape
    raw = b''.join(b'\x00' + rgba[y].astype(np.uint8).tobytes() for y in range(h))

    def chunk(kind, data):
        body = kind + data
        return struct.pack('>I', len(data)) + body + struct.pack('>I', zlib.crc32(body) & 0xffffffff)

    png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
    png += chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')
    path.write_bytes(png)


def face(edge):
    c = (SIZE - 1) / 2
    y, x = np.mgrid[0:SIZE, 0:SIZE]
    dx, dy = (x - c) / c, (y - c) / c
    r = np.hypot(dx, dy)
    # Angle clockwise from the top, from 0 to 1
    a = (np.degrees(np.arctan2(dx, -dy)) % 360) / 360
    # The block's own marble, its pixels as large as they are on the block seen up close
    side = Image.open(BLOCK / 'dhd_side.png').convert('RGB')
    tile = np.asarray(side.resize((side.width * PIXEL, side.height * PIXEL), Image.NEAREST)).astype(np.float64)
    reps = (SIZE + tile.shape[0] - 1) // tile.shape[0]
    marble = np.tile(tile, (reps, reps, 1))[:SIZE, :SIZE]
    rgb = np.zeros((SIZE, SIZE, 3))
    alpha = np.zeros((SIZE, SIZE))
    inside = r <= RIM
    rgb[inside] = marble[inside]
    alpha[inside] = 255

    def ring(r0, r1, count):
        within = (r >= r0 + GAP) & (r <= r1 - GAP)
        spot = a * count
        frac = spot - np.floor(spot)
        # Gaps between buttons, the same width all round the ring
        gap = GAP / (2 * math.pi * r.clip(1e-3)) * count
        button_area = within & (frac > gap) & (frac < 1 - gap)
        # Each button is raised: lighter in its middle, darker at its edges
        mid = 1 - np.abs((r - (r0 + r1) / 2) / ((r1 - r0) / 2))
        shade = 0.75 + 0.35 * mid.clip(0, 1) ** 0.5
        rgb[button_area] = marble[button_area] * 1.25 * shade[button_area, None]
        edges = within & ~button_area
        rgb[edges] = np.array(edge)

    ring(BETWEEN, OUTER, OUTER_BUTTONS)
    ring(INNER, BETWEEN, INNER_BUTTONS)
    # The grooves round each ring
    grooves = (np.abs(r - OUTER) < GAP) | (np.abs(r - BETWEEN) < GAP)
    rgb[grooves] = np.array(edge)
    # The rim, and the well the dome sits in
    rim = (r > OUTER) & inside
    rgb[rim] = marble[rim] * 0.8
    well = r < INNER - GAP
    rgb[well] = np.array(edge) * (0.6 + 0.4 * (r[well] / INNER))[:, None]
    # Soft edge
    alpha = np.where(inside, 255 * np.clip((RIM - r) * c, 0, 1), 0)
    return np.dstack([rgb.clip(0, 255), alpha])


def glow():
    c = 63.5
    y, x = np.mgrid[0:128, 0:128]
    r = np.hypot(x - c, y - c) / c
    a = np.clip(1 - r, 0, 1) ** 1.6 * 255
    return np.dstack([np.full((128, 128), 255), np.full((128, 128), 255), np.full((128, 128), 255), a])


def dome():
    c = 63.5
    y, x = np.mgrid[0:128, 0:128]
    dx, dy = (x - c) / c, (y - c) / c
    r = np.hypot(dx, dy)
    z = np.sqrt(np.clip(1 - r * r, 0, 1))
    # Lit from the top left: bright there, a highlight, and darker to the bottom right
    light = np.clip(0.35 + 0.65 * (-0.45 * dx - 0.55 * dy + 0.7 * z), 0, 1)
    spec = np.clip((-0.45 * dx - 0.55 * dy + 0.7 * z) - 0.75, 0, 1) * 3
    v = np.clip(light + spec, 0, 1) * 255
    a = 255 * np.clip((1 - r) * c, 0, 1)
    return np.dstack([v, v, v, a])


OUT.mkdir(parents=True, exist_ok=True)
# Both of the block's marble, as both DHDs are
write_png(OUT / 'milkyway_face.png', face((8, 8, 8)))
write_png(OUT / 'pegasus_face.png', face((8, 8, 8)))
write_png(OUT / 'glow.png', glow())
write_png(OUT / 'dome.png', dome())
print('written')
