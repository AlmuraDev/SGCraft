"""Draws a glyph of the Milky Way gate's symbol ring as the gate model has it, seen from in front: white on clear,
trimmed and centered in a square cell. Slot k is k glyphs clockwise from the top with the ring at rest; slot 0 is the
point of origin. The DHD's and the fonts' sheets take the glyphs that have no other art from here.

Run from the repository root: python tools/milkyway/ring_glyph.py <slot> <size> <out.png>
Needs Pillow.
"""

import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw

MODEL = Path('core/src/main/resources/assets/sgcraft/models/gate/milkyway_gate.txt')
SLOTS = 39
SLOT = 360 / SLOTS
# The ring's cone, from its inner to its outer edge, as (radius, depth); a glyph stands out of it
RING = [[4.8436, 0.0663], [5.4408, 0.2243]]
SLOPE = (RING[1][1] - RING[0][1]) / (RING[1][0] - RING[0][0])
# How much larger the glyph is drawn before it is scaled down, to smooth its edges
SMOOTH = 8


def cone_depth(radius):
    return RING[0][1] + (radius - RING[0][0]) * SLOPE


def ring_faces():
    """The ring's triangles, each as three (x, y, z) corners."""
    faces = []
    for line in MODEL.read_text(encoding='utf-8').splitlines():
        if line.startswith('r '):
            v = [float(x) for x in line.split()[1:]]
            faces.append([v[0:3], v[3:6], v[6:9]])
    return faces


def angle(point):
    return math.degrees(math.atan2(point[1], point[0]))


def glyph(slot, size, pad=2):
    """The glyph in a slot, white on clear, as large as fits in a cell of the size given with a margin of pad."""
    middle = 90 - slot * SLOT
    shapes = []
    for face in ring_faces():
        center = [sum(p[i] for p in face) / 3 for i in range(3)]
        off = ((angle(center) - middle) % 360 + 540) % 360 - 180
        # Short of the dividers between slots, and standing out of the cone
        if abs(off) > SLOT * 0.4 or center[2] - cone_depth(math.hypot(center[0], center[1])) < 0.006:
            continue
        shape = []
        for point in face:
            turn = math.radians(middle - angle(point))
            radius = math.hypot(point[0], point[1])
            shape.append((radius * math.sin(turn), -radius * math.cos(turn)))
        shapes.append(shape)
    cell = Image.new('RGBA', (size, size))
    if not shapes:
        return cell
    xs = [x for shape in shapes for x, _ in shape]
    ys = [y for shape in shapes for _, y in shape]
    x0, x1, y0, y1 = min(xs), max(xs), min(ys), max(ys)
    scale = (size - 2 * pad) / max(x1 - x0, y1 - y0) * SMOOTH
    left = (size * SMOOTH - (x1 - x0) * scale) / 2
    top = (size * SMOOTH - (y1 - y0) * scale) / 2
    mask = Image.new('L', (size * SMOOTH, size * SMOOTH), 0)
    draw = ImageDraw.Draw(mask)
    for shape in shapes:
        draw.polygon([((x - x0) * scale + left, (y - y0) * scale + top) for x, y in shape], fill=255)
    cell = Image.new('RGBA', (size, size), (255, 255, 255, 0))
    cell.putalpha(mask.resize((size, size), Image.LANCZOS))
    return cell


if __name__ == '__main__':
    glyph(int(sys.argv[1]), int(sys.argv[2])).save(sys.argv[3])
    print('written', sys.argv[3])
