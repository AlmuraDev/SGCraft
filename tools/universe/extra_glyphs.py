"""Adds the Universe gate's glyphs for + and . to its glyph sheets, after the 36 measured from the Destiny gate model.

Each Universe glyph is a column of pieces: dots, bars, ovals, rings and S-curves. The two new glyphs are columns of
pieces cut from the measured glyphs, in orders none of those has, cut alike from the glyphs and their cut-outs. Their
glow is the glyph blurred and brightened, as the measured glyphs' glow is, to within a few levels. The sheets go
from six rows of six glyphs to seven, the last row holding the new two; the first 36 are kept as they are, so running
this again gives the same sheets.

Run from the repository root: python tools/universe/extra_glyphs.py
Needs Pillow.
"""

from pathlib import Path

from PIL import Image, ImageChops, ImageFilter

TEXTURES = Path('core/src/main/resources/assets/sgcraft/textures/gate/universe')
SHEETS = ('glyphs', 'glyphs_cut')
CELL = 128
COLUMNS = 6
MEASURED = 36
ROWS = 7
# How far past a piece its edge is cut, and the space between pieces, in pixels
MARGIN = 4
GAP = 7
# The glow: how far the glyph is blurred, in pixels, and how much brighter it is then made
GLOW_BLUR = 7.5
GLOW_GAIN = 2.1
# The pieces of each new glyph, top to bottom: (measured glyph, top, bottom), in pixels down its cell
S_CURVE = (6, 21, 53)
LONG_BAR = (19, 34, 74)
BIG_RING = (5, 91, 112)
RING = (13, 29, 50)
DOT = (0, 53, 64)
NEW = [
    [S_CURVE, LONG_BAR, BIG_RING],
    [RING, DOT, S_CURVE, DOT, RING],
]


def cell_box(index):
    return index % COLUMNS * CELL, index // COLUMNS * CELL


def compose(sheet, pieces, background):
    """A new glyph's cell: its pieces stacked with even gaps, the stack in the middle of the cell."""
    cell = Image.new(sheet.mode, (CELL, CELL), background)
    height = sum(bottom - top for _, top, bottom in pieces) + GAP * (len(pieces) - 1)
    y = (CELL - height) // 2
    for glyph, top, bottom in pieces:
        x0, y0 = cell_box(glyph)
        band = sheet.crop((x0, y0 + top - MARGIN, x0 + CELL, y0 + bottom + MARGIN))
        layer = Image.new(sheet.mode, (CELL, CELL), background)
        layer.paste(band, (0, y - MARGIN))
        cell = ImageChops.lighter(cell, layer)
        y += bottom - top + GAP
    return cell


def glow(glyph):
    """A glyph's glow, from the glyph, white on black."""
    blurred = glyph.convert('L').filter(ImageFilter.GaussianBlur(GLOW_BLUR)).point(
        lambda level: min(255, round(level * GLOW_GAIN)))
    return Image.merge('RGBA', (blurred, blurred, blurred, Image.new('L', glyph.size, 255)))


def main():
    glyphs = []
    for name in SHEETS:
        path = TEXTURES / f'{name}.png'
        sheet = Image.open(path).convert('RGBA')
        measured = sheet.crop((0, 0, COLUMNS * CELL, MEASURED // COLUMNS * CELL))
        # The cut-out sheet is clear where there is no glyph; the others are black
        background = (0, 0, 0, 0) if name == 'glyphs_cut' else (0, 0, 0, 255)
        out = Image.new('RGBA', (COLUMNS * CELL, ROWS * CELL), background)
        out.paste(measured, (0, 0))
        for i, pieces in enumerate(NEW):
            cell = compose(measured, pieces, background)
            if name == 'glyphs':
                glyphs.append(cell)
            out.paste(cell, cell_box(MEASURED + i))
        out.save(path, optimize=True)
        print('written', path)
    path = TEXTURES / 'glyphs_glow.png'
    sheet = Image.open(path).convert('RGBA')
    out = Image.new('RGBA', (COLUMNS * CELL, ROWS * CELL), (0, 0, 0, 255))
    out.paste(sheet.crop((0, 0, COLUMNS * CELL, MEASURED // COLUMNS * CELL)), (0, 0))
    for i, glyph in enumerate(glyphs):
        out.paste(glow(glyph), cell_box(MEASURED + i))
    out.save(path, optimize=True)
    print('written', path)


if __name__ == '__main__':
    main()
