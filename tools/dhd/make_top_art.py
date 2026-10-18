"""Repaints the inner ring of the DHD's top, from the original SGCraft's texture, for its 13 buttons: 0 to 9, @, + and
., each clockwise from the top, as on the DHD's screen. The ring keeps its own marble, smoothed so the glyphs and
dividers it had are gone, and the outer ring, the dome and the grooves are left as the original drew them.

Run from the repository root: python tools/dhd/make_top_art.py
Needs Pillow. The glyphs come from the DHD screen's Milky Way sheet; make it first.
"""

import math
from pathlib import Path

from PIL import Image, ImageFilter

TEXTURES = Path('core/src/main/resources/assets/sgcraft/textures')
TOP = TEXTURES / 'block/dhd_top.png'
SHEET = TEXTURES / 'gui/dhd/milkyway_glyphs.png'
CENTER = 64
# The inner ring, between the dome and the groove round it, in pixels from the center
INNER = (12.5, 33.5)
# How wide the smoothing is, in pixels: wider than a glyph's strokes and the dividers
SMOOTH = 7
BUTTONS = 13
FIRST = 26
GLYPH = 8
DIVIDER = (14, 14, 14)
INK = (205, 205, 200)


def main():
    image = Image.open(TOP).convert('RGBA')
    smooth = image.filter(ImageFilter.MedianFilter(SMOOTH))
    for y in range(image.height):
        for x in range(image.width):
            dx, dy = x + 0.5 - CENTER, y + 0.5 - CENTER
            radius = math.hypot(dx, dy)
            if not INNER[0] <= radius <= INNER[1]:
                continue
            turn = math.atan2(dx, -dy) % (2 * math.pi)
            pixel = smooth.getpixel((x, y))
            # The dividers between buttons
            edge = turn / (2 * math.pi) * BUTTONS
            if abs(edge - round(edge)) * 2 * math.pi * radius / BUTTONS < 0.6:
                pixel = DIVIDER + (255,)
            image.putpixel((x, y), pixel)
    sheet = Image.open(SHEET).convert('RGBA')
    middle = (INNER[0] + INNER[1]) / 2
    for slot in range(BUTTONS):
        index = FIRST + slot
        cell = sheet.crop((index % 10 * 48, index // 10 * 48, index % 10 * 48 + 48, index // 10 * 48 + 48))
        glyph = Image.new('RGBA', cell.size, INK + (0,))
        glyph.putalpha(cell.getchannel('A'))
        glyph = glyph.resize((GLYPH, GLYPH), Image.LANCZOS)
        turn = 2 * math.pi * (slot + 0.5) / BUTTONS
        x = round(CENTER + middle * math.sin(turn) - GLYPH / 2)
        y = round(CENTER - middle * math.cos(turn) - GLYPH / 2)
        image.alpha_composite(glyph, (x, y))
    image.save(TOP, optimize=True)
    print('written')


if __name__ == '__main__':
    main()
