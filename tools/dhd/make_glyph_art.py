"""Turns the DHD screen's glyph sheets white on clear, so the screen can tint each glyph dim or lit, and draws the
outline a button lights with under the mouse, one for each ring, for its first button at the top. The screen turns
it round to the others.

Run from the repository root: python tools/dhd/make_glyph_art.py
Needs numpy and Pillow. The rings must match make_screen_art.py and DhdScreen.
"""

import math
from pathlib import Path

import numpy as np
from PIL import Image

OUT = Path('core/src/main/resources/assets/sgcraft/textures/gui/dhd')
SIZE = 512
OUTER, BETWEEN, INNER = 0.94, 0.58, 0.28
OUTER_BUTTONS, INNER_BUTTONS = 26, 13
GAP = 0.012
LINE = 0.018


def whiten(name):
    """Keeps a sheet's shapes, white, with their strength as alpha; a black or clear ground drops out."""
    rgba = np.asarray(Image.open(OUT / name).convert('RGBA')).astype(np.float64)
    strength = rgba[..., :3].max(axis=2) / 255 * (rgba[..., 3] / 255)
    if rgba[..., 3].min() > 0:
        # An opaque sheet drawn on black: brightness is the shape
        alpha = strength
    else:
        # Already clear round its shapes: keep their alpha
        alpha = rgba[..., 3] / 255
    out = np.dstack([np.full(alpha.shape, 255.0)] * 3 + [np.clip(alpha / max(alpha.max(), 1e-9), 0, 1) * 255])
    Image.fromarray(out.astype(np.uint8), 'RGBA').save(OUT / name)


def outline(r0, r1, count, name):
    c = (SIZE - 1) / 2
    y, x = np.mgrid[0:SIZE, 0:SIZE]
    dx, dy = (x - c) / c, (y - c) / c
    r = np.hypot(dx, dy)
    a = (np.degrees(np.arctan2(dx, -dy)) % 360) / 360
    # The button's own shape: the first wedge clockwise from the top, inside the ring's gaps
    arc = GAP / (2 * math.pi * r.clip(1e-3))
    inside = (r >= r0 + GAP) & (r <= r1 - GAP) & (a > arc) & (a < 1 / count - arc)
    # Its edge: inside it, but near the outside
    line_arc = LINE / (2 * math.pi * r.clip(1e-3))
    edge = inside & ((r < r0 + GAP + LINE) | (r > r1 - GAP - LINE) | (a < arc + line_arc)
                     | (a > 1 / count - arc - line_arc))
    alpha = np.where(edge, 255, np.where(inside, 40, 0))
    out = np.dstack([np.full((SIZE, SIZE), 255)] * 3 + [alpha]).astype(np.uint8)
    Image.fromarray(out, 'RGBA').save(OUT / name)


whiten('milkyway_glyphs.png')
whiten('pegasus_glyphs.png')
outline(BETWEEN, OUTER, OUTER_BUTTONS, 'outline_outer.png')
outline(INNER, BETWEEN, INNER_BUTTONS, 'outline_inner.png')
print('written')
