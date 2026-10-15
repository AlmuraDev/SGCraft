"""Builds a chat font for each gate type's glyphs, so commands can show an address as the symbols a gate shows.

Each font maps an address's characters (A to Z, 0 to 9 and @) to that type's glyph. Pegasus and Universe have no @
of their own, so they borrow the Milky Way's point of origin.

Run from the repository root: python tools/dhd/make_glyph_font.py
Needs numpy and Pillow.
"""

import json
from pathlib import Path

import numpy as np
from PIL import Image

ASSETS = Path('core/src/main/resources/assets/sgcraft')
CHARS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@'
CELL = 64
COLUMNS = 10


def white(image):
    """A glyph as white, with its strength as alpha, whether drawn on black or on clear."""
    rgba = np.asarray(image.convert('RGBA')).astype(np.float64)
    if rgba[..., 3].min() > 0:
        alpha = rgba[..., :3].max(axis=2)
    else:
        alpha = rgba[..., 3]
    alpha = alpha / max(alpha.max(), 1e-9) * 255
    out = np.dstack([np.full(alpha.shape, 255.0)] * 3 + [alpha])
    return Image.fromarray(out.astype(np.uint8), 'RGBA')


def fit(glyph):
    """Trims a glyph to its shape and centers it in a cell, as large as fits."""
    box = glyph.getbbox()
    if box:
        glyph = glyph.crop(box)
    scale = (CELL - 4) / max(glyph.size)
    glyph = glyph.resize((max(1, round(glyph.size[0] * scale)), max(1, round(glyph.size[1] * scale))),
                         Image.LANCZOS)
    cell = Image.new('RGBA', (CELL, CELL))
    cell.paste(glyph, ((CELL - glyph.size[0]) // 2, (CELL - glyph.size[1]) // 2))
    return cell


def sheet(path, cell, columns, count):
    image = white(Image.open(path))
    return [image.crop((i % columns * cell, i // columns * cell, (i % columns + 1) * cell, (i // columns + 1) * cell))
            for i in range(count)]


milky = sheet(ASSETS / 'textures/gui/dhd/milkyway_glyphs.png', 48, 10, 38)
origin = milky[37]
fonts = {
    'glyphs_milkyway': milky[:36] + [origin],
    'glyphs_pegasus': sheet(ASSETS / 'textures/gui/dhd/pegasus_glyphs.png', 128, 6, 36) + [origin],
    'glyphs_universe': sheet(ASSETS / 'textures/gate/universe/glyphs.png', 128, 6, 36) + [origin],
}
rows = [CHARS[i:i + COLUMNS] for i in range(0, len(CHARS), COLUMNS)]
rows[-1] = rows[-1].ljust(COLUMNS, '\u0000')
(ASSETS / 'textures/font').mkdir(parents=True, exist_ok=True)
(ASSETS / 'font').mkdir(parents=True, exist_ok=True)
for name, glyphs in fonts.items():
    out = Image.new('RGBA', (COLUMNS * CELL, len(rows) * CELL))
    for i, glyph in enumerate(glyphs):
        out.paste(fit(glyph), (i % COLUMNS * CELL, i // COLUMNS * CELL))
    out.save(ASSETS / f'textures/font/{name}.png')
    definition = {'providers': [{'type': 'bitmap', 'file': f'sgcraft:font/{name}.png', 'ascent': 8, 'height': 10,
                                 'chars': rows}]}
    (ASSETS / f'font/{name}.json').write_text(json.dumps(definition, indent=2) + '\n', encoding='utf-8')
print('written')
