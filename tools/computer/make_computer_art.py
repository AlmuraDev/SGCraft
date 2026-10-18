"""Makes the dialing computer's, the MALP console's and the office chair's textures, block models, item models and
blockstates.

The art is drawn here, after the SGC's control room on the show: a gray steel desk three blocks wide with a keyboard,
a wide monitor in a putty plastic case standing on it, and a black office chair. The MALP console is the same desk,
its monitor's idle screen green, with a joystick to drive by in place of the switches on its right. Nothing is copied
from the show.

Run from the repository root: python tools/computer/make_computer_art.py
Needs Pillow.
"""

import json
import random
from pathlib import Path

from PIL import Image, ImageDraw

ASSETS = Path('core/src/main/resources/assets/sgcraft')
TEXTURES = ASSETS / 'textures/block'
MODELS = ASSETS / 'models/block'
ITEM_MODELS = ASSETS / 'models/item'
BLOCKSTATES = ASSETS / 'blockstates'
ITEMS = ASSETS / 'items'
SIZE = 32


def noisy(color, spread, seed):
    """A texture of one color, each pixel a little lighter or darker."""
    rng = random.Random(seed)
    image = Image.new('RGBA', (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            d = rng.randint(-spread, spread)
            image.putpixel((x, y), tuple(max(0, min(255, c + d)) for c in color) + (255,))
    return image


def shade(image, box, amount):
    """Lightens or darkens a rectangle of a texture."""
    x0, y0, x1, y1 = box
    for y in range(max(0, y0), min(SIZE, y1)):
        for x in range(max(0, x0), min(SIZE, x1)):
            r, g, b, a = image.getpixel((x, y))
            image.putpixel((x, y), (max(0, min(255, r + amount)), max(0, min(255, g + amount)),
                                    max(0, min(255, b + amount)), a))


# The keyboard's keys as the operator sees them, the far edge at the top: a letter key (k), a gray key (m), the space
# bar (s) and the enter key (e), each run of one letter being one key, and the tray showing between them (.). Each
# row of keys is two pixels deep, its cap and the front of the cap below. The main keys are on the left, the number
# pad on the right
KEY_ROWS = [
    'm.kk.kk.kk.kk.m.mm.m',
    'mm.kk.kk.kk.eee.kk.k',
    'mmm.kk.kk.kk.ee.kk.k',
    'mm.sssssssss.mm.kk.k',
]
# The keyboard's top, in pixels: a pixel of tray round the keys, and its front lip
KEYBOARD_WIDTH = 22
KEYBOARD_DEPTH = 11
# Where the keyboard's sides are drawn, below its top
KEYBOARD_SIDE = 12


def make_keyboard():
    """The keyboard: off-white keys and gray ones in a dark tray, like the panels beside it, drawn a pixel a key's width
    as the game's own blocks are, with two green lamps over the number pad."""
    rng = random.Random(7)
    tray = (56, 60, 64)
    keys = {'k': ((228, 223, 208), (170, 164, 146)), 's': ((228, 223, 208), (170, 164, 146)),
            'm': ((158, 158, 152), (112, 112, 108)), 'e': ((158, 158, 152), (112, 112, 108))}
    top = Image.new('RGBA', (KEYBOARD_WIDTH, KEYBOARD_DEPTH))

    def put(x, y, color, spread=3):
        d = rng.randint(-spread, spread)
        top.putpixel((x, y), tuple(max(0, min(255, c + d)) for c in color) + (255,))

    for y in range(KEYBOARD_DEPTH):
        for x in range(KEYBOARD_WIDTH):
            put(x, y, tray)
    # The tray's far edge catches the light, and its front lip is a little darker
    for x in range(KEYBOARD_WIDTH):
        put(x, 0, tuple(c + 12 for c in tray))
        put(x, KEYBOARD_DEPTH - 1, tuple(c - 18 for c in tray))
    for x in (17, 19):
        put(x, 0, (90, 220, 110), 0)
    grid = [row for row in KEY_ROWS for _ in range(2)]
    for gy, row in enumerate(grid):
        for gx, key in enumerate(row):
            x, y = gx + 1, gy + 1
            if key == '.':
                put(x, y, (32, 34, 38))
                continue
            cap, front = keys[key]
            # A key reaching down into the next row, as the enter key does, shows its front only at its foot
            below = grid[gy + 1][gx] if gy + 1 < len(grid) else None
            foot = below != key or key in 'km' and gy % 2 == 1
            # The first pixel of each cap, lit from the left
            first = gx == 0 or row[gx - 1] != key
            if foot:
                put(x, y, front)
            else:
                put(x, y, tuple(c + 10 for c in cap) if first else cap)
    keyboard = noisy(tray, 4, 12)
    # Its top, turned to face the operator, who stands at the north looking south: the game draws a top face with
    # north at the top and west at the left
    keyboard.paste(top.rotate(180), (0, 0))
    # Its sides: the plastic's edge, and its shadow below
    shade(keyboard, (0, KEYBOARD_SIDE, SIZE, KEYBOARD_SIDE + 1), 8)
    shade(keyboard, (0, KEYBOARD_SIDE + 1, SIZE, KEYBOARD_SIDE + 2), -24)
    return keyboard


def keyboard(start, end):
    """The keyboard: its top shows the whole drawing of its keys, its sides the strip below it."""
    (x0, y0, z0), (x1, y1, z1) = start, end
    width, depth, height = x1 - x0, z1 - z0, y1 - y0
    side = KEYBOARD_SIDE / 2
    sides = {
        'up': {'uv': [0, 0, KEYBOARD_WIDTH / 2, KEYBOARD_DEPTH / 2], 'texture': '#keyboard'},
        'down': {'uv': uv('down', start, end), 'texture': '#case'},
    }
    for face, length in (('north', width), ('south', width), ('east', depth), ('west', depth)):
        sides[face] = {'uv': [0, side, length, side + height], 'texture': '#keyboard'}
    return {'from': list(start), 'to': list(end), 'faces': sides}


def textures():
    # The monitor's case: putty plastic, a seam round its edge
    case = noisy((196, 189, 168), 5, 1)
    shade(case, (0, 0, 32, 1), 18)
    shade(case, (0, 31, 32, 32), -22)
    shade(case, (0, 0, 1, 32), 10)
    shade(case, (31, 0, 32, 32), -14)
    # Vents in the case's back and sides
    vents = case.copy()
    for y in range(6, 26, 3):
        shade(vents, (6, y, 26, y + 1), -40)
    # The bezel round the screen: dark plastic
    bezel = noisy((46, 48, 52), 4, 2)
    shade(bezel, (0, 0, 32, 1), 16)
    # The screen with nothing drawn on it, seen from far away: dark blue glass, a faint grid and a sheen
    screen = noisy((6, 18, 30), 2, 3)
    for i in range(0, SIZE, 8):
        shade(screen, (i, 0, i + 1, SIZE), 10)
        shade(screen, (0, i, SIZE, i + 1), 10)
    for i in range(10):
        shade(screen, (i * 2, i, i * 2 + 6, i + 1), 14)
    # The desk's top: gray laminate with a darker edge
    desk = noisy((134, 138, 142), 6, 4)
    shade(desk, (0, 0, 32, 2), -26)
    # The desk's cabinets: painted steel, two drawers with handles
    cabinet = noisy((104, 110, 116), 5, 5)
    shade(cabinet, (0, 0, 32, 2), 20)
    for top in (4, 18):
        shade(cabinet, (2, top, 30, top + 1), -36)
        shade(cabinet, (2, top + 12, 30, top + 13), 18)
        shade(cabinet, (12, top + 5, 20, top + 7), 50)
    # A plain steel panel for the desk's sides and the knee hole's back
    steel = noisy((98, 104, 110), 5, 6)
    keyboard = make_keyboard()
    # A small panel of switches and lamps
    panel = noisy((52, 56, 60), 4, 8)
    lamps = [(230, 64, 52), (90, 220, 120), (255, 180, 60), (80, 200, 230)]
    for i in range(8):
        x, y = 4 + (i % 4) * 7, 6 + (i // 4) * 12
        color = lamps[(i * 3) % 4]
        ImageDraw.Draw(panel).rectangle((x, y, x + 2, y + 2), fill=color + (255,))
        ImageDraw.Draw(panel).rectangle((x, y + 5, x + 2, y + 7), fill=(170, 172, 176, 255))
    # The chair: black fabric, and dark metal for its base
    fabric = noisy((30, 31, 35), 5, 9)
    for y in range(0, SIZE, 2):
        shade(fabric, (0, y, SIZE, y + 1), -4)
    metal = noisy((64, 68, 74), 6, 10)
    shade(metal, (0, 0, 32, 3), 24)
    caster = noisy((22, 22, 24), 3, 11)
    # The MALP console's screen with nothing on it: dark green glass, faint scan lines, a grid and a sheen
    malp_screen = noisy((6, 26, 14), 2, 13)
    for i in range(0, SIZE, 2):
        shade(malp_screen, (0, i, SIZE, i + 1), -3)
    for i in range(0, SIZE, 8):
        shade(malp_screen, (i, 0, i + 1, SIZE), 9)
        shade(malp_screen, (0, i, SIZE, i + 1), 9)
    for i in range(10):
        shade(malp_screen, (i * 2, i, i * 2 + 6, i + 1), 12)
    # The joystick's knob: red plastic, lit on top
    knob = noisy((176, 34, 30), 6, 14)
    shade(knob, (0, 0, 32, 4), 30)
    TEXTURES.mkdir(parents=True, exist_ok=True)
    for name, image in [('dialing_computer_case', case), ('dialing_computer_vents', vents),
                        ('dialing_computer_bezel', bezel), ('dialing_computer_screen', screen),
                        ('dialing_computer_desk', desk), ('dialing_computer_cabinet', cabinet),
                        ('dialing_computer_steel', steel), ('dialing_computer_keyboard', keyboard),
                        ('dialing_computer_panel', panel), ('office_chair_fabric', fabric),
                        ('office_chair_metal', metal), ('office_chair_caster', caster),
                        ('malp_console_screen', malp_screen), ('malp_console_knob', knob)]:
        image.save(TEXTURES / f'{name}.png', optimize=True)


def uv(side, start, end):
    """The part of the texture a face shows, as the game would pick from where the face is in its block, moved to
    lie within the texture: the game picks no further than the texture's edge, so a face reaching past its block,
    as in the item's model or the chair's back, would show some other texture."""
    (x0, y0, z0), (x1, y1, z1) = start, end
    u0, v0, u1, v1 = {
        'north': (16 - x1, 16 - y1, 16 - x0, 16 - y0),
        'south': (x0, 16 - y1, x1, 16 - y0),
        'east': (16 - z1, 16 - y1, 16 - z0, 16 - y0),
        'west': (z0, 16 - y1, z1, 16 - y0),
        'up': (x0, z0, x1, z1),
        'down': (x0, 16 - z1, x1, 16 - z0),
    }[side]
    shift_u = -16 * (u0 // 16) if u0 < 0 or u1 > 16 else 0
    shift_v = -16 * (v0 // 16) if v0 < 0 or v1 > 16 else 0
    u0, u1, v0, v1 = u0 + shift_u, u1 + shift_u, v0 + shift_v, v1 + shift_v
    return [u0, v0, min(16, u1), min(16, v1)]


def box(start, end, texture, faces=None, north=None):
    """A cuboid, every face drawn with one texture unless named otherwise."""
    faces = faces or {}
    sides = {}
    for side in ('north', 'east', 'south', 'west', 'up', 'down'):
        sides[side] = {'uv': uv(side, start, end),
                       'texture': '#' + faces.get(side, north if side == 'north' and north else texture)}
    return {'from': list(start), 'to': list(end), 'faces': sides}


# The parts, facing north: the operator stands north of the computer, looking south at the screen. The operator's
# left is east, so the left column is the block east of the middle; in its own block, its outer edge is x = 16
COLUMNS = {'left': 1, 'middle': 0, 'right': -1}


def part(column, row, malp=False):
    """The cuboids of one part of the computer, or of the MALP console, in its own block."""
    elements = []
    outer_east = column == 'left'
    outer_west = column == 'right'
    if row == 'lower':
        elements.append(box((0, 11, 0), (16, 12, 16), 'desk'))
        if column == 'middle':
            # The knee hole, with a panel at its back, and the keyboard in front of the screen
            elements.append(box((0, 2, 12), (16, 11, 14), 'steel'))
            elements.append(keyboard((2.5, 12, 1), (2.5 + KEYBOARD_WIDTH / 2, 12.75, 1 + KEYBOARD_DEPTH / 2)))
        else:
            elements.append(box((0, 0, 1), (16, 11, 16), 'steel', north='cabinet'))
            if malp and outer_west:
                # The MALP console's joystick, on the operator's right: its base, its stick and its knob
                elements.append(box((5, 12, 2), (11, 13, 6), 'steel', faces={'up': 'bezel'}))
                elements.append(box((7.5, 13, 3.5), (8.5, 15.5, 4.5), 'stick'))
                elements.append(box((7, 15.5, 3), (9, 17, 5), 'knob'))
            else:
                # A panel of switches and lamps on the desk to either side
                elements.append(box((4, 12, 2), (12, 13, 6), 'steel', faces={'up': 'panel'}))
        # The monitor's case from the desk up, its face at z = 8, and the bezel's lower edge in front of it
        elements.append(box((0, 12, 8), (16, 16, 15), 'case', north='screen'))
        elements.append(box((0, 12, 7.5), (16, 13.5, 8), 'bezel'))
    else:
        elements.append(box((0, 0, 8), (16, 16, 15), 'case', north='screen', faces={'south': 'vents'}))
        # The tube's bulge behind the case
        elements.append(box((1, 1, 15), (15, 14, 16), 'vents'))
        elements.append(box((0, 14.5, 7.5), (16, 16, 8), 'bezel'))
    # The bezel's ends, on the outer columns
    low, high = (12, 16) if row == 'lower' else (0, 14.5)
    if outer_east:
        elements.append(box((15, low, 7.5), (16, high, 8), 'bezel'))
    if outer_west:
        elements.append(box((0, low, 7.5), (1, high, 8), 'bezel'))
    return elements


COMPUTER_TEXTURES = {
    'particle': 'sgcraft:block/dialing_computer_case',
    'case': 'sgcraft:block/dialing_computer_case',
    'vents': 'sgcraft:block/dialing_computer_vents',
    'bezel': 'sgcraft:block/dialing_computer_bezel',
    'screen': 'sgcraft:block/dialing_computer_screen',
    'desk': 'sgcraft:block/dialing_computer_desk',
    'cabinet': 'sgcraft:block/dialing_computer_cabinet',
    'steel': 'sgcraft:block/dialing_computer_steel',
    'keyboard': 'sgcraft:block/dialing_computer_keyboard',
    'panel': 'sgcraft:block/dialing_computer_panel',
}


# The MALP console's: the same, with its own idle screen and the joystick's stick and knob
MALP_CONSOLE_TEXTURES = dict(COMPUTER_TEXTURES, **{
    'screen': 'sgcraft:block/malp_console_screen',
    'stick': 'sgcraft:block/office_chair_metal',
    'knob': 'sgcraft:block/malp_console_knob',
})


def chair():
    """The office chair, facing north: its seat, its back to the south, arms, and a five footed base."""
    elements = [
        box((7, 1.5, 7), (9, 6, 9), 'metal'),
        box((2, 6, 2), (14, 9, 14), 'fabric'),
        box((7, 8, 12.5), (9, 11, 14.5), 'metal'),
        box((2.5, 10, 13), (13.5, 23, 15), 'fabric'),
        box((1.5, 9, 6), (2.5, 12, 7), 'metal'),
        box((13.5, 9, 6), (14.5, 12, 7), 'metal'),
        box((1, 12, 4), (3, 13, 11), 'fabric'),
        box((13, 12, 4), (15, 13, 11), 'fabric'),
    ]
    # The base's legs, out to the casters
    elements.append(box((1.5, 1, 7.25), (14.5, 2, 8.75), 'metal'))
    elements.append(box((7.25, 1, 1.5), (8.75, 2, 14.5), 'metal'))
    for x, z in ((1, 7.5), (14, 7.5), (7.5, 1), (7.5, 14)):
        elements.append(box((x, 0, z), (x + 1, 1, z + 1), 'caster'))
    return elements


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8', newline='\n')


ROTATIONS = {'north': 0, 'east': 90, 'south': 180, 'west': 270}


def desk(block, textures, malp=False):
    """The six parts' models and the blockstate of a computer at a desk, and its item's model and definition."""
    variants = {}
    whole = []
    for column, offset in COLUMNS.items():
        for row in ('lower', 'upper'):
            name = f'{block}_{column}_{row}'
            elements = part(column, row, malp)
            write(MODELS / f'{name}.json', {'parent': 'minecraft:block/block', 'textures': textures,
                                            'elements': elements})
            for facing, y in ROTATIONS.items():
                variant = {'model': f'sgcraft:block/{name}'}
                if y:
                    variant['y'] = y
                variants[f'facing={facing},part={column}_{row}'] = variant
            # The item shows the whole computer: each part moved to its place beside the middle
            dx, dy = offset * 16, 16 if row == 'upper' else 0
            for element in elements:
                moved = json.loads(json.dumps(element))
                moved['from'][0] += dx
                moved['to'][0] += dx
                moved['from'][1] += dy
                moved['to'][1] += dy
                whole.append(moved)
    write(BLOCKSTATES / f'{block}.json', {'variants': variants})
    write(ITEM_MODELS / f'{block}.json', {
        'parent': 'minecraft:block/block',
        'textures': textures,
        'elements': whole,
        'display': {
            'gui': {'rotation': [30, 225, 0], 'translation': [0, -1.5, 0], 'scale': [0.3, 0.3, 0.3]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.15, 0.15, 0.15]},
            'fixed': {'rotation': [0, 180, 0], 'translation': [0, -2, 0], 'scale': [0.3, 0.3, 0.3]},
            'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0],
                                      'scale': [0.2, 0.2, 0.2]},
            'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.2, 0.2, 0.2]},
            'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [0.2, 0.2, 0.2]},
        },
    })
    write(ITEMS / f'{block}.json', {'model': {'type': 'minecraft:model', 'model': f'sgcraft:item/{block}'}})


def models():
    desk('dialing_computer', COMPUTER_TEXTURES)
    desk('malp_console', MALP_CONSOLE_TEXTURES, malp=True)
    write(MODELS / 'office_chair.json', {'parent': 'minecraft:block/block', 'textures': {
        'particle': 'sgcraft:block/office_chair_fabric',
        'fabric': 'sgcraft:block/office_chair_fabric',
        'metal': 'sgcraft:block/office_chair_metal',
        'caster': 'sgcraft:block/office_chair_caster',
    }, 'elements': chair()})
    write(BLOCKSTATES / 'office_chair.json', {'variants': {
        f'facing={facing}': ({'model': 'sgcraft:block/office_chair', 'y': y} if y
                             else {'model': 'sgcraft:block/office_chair'})
        for facing, y in ROTATIONS.items()}})
    write(ITEMS / 'office_chair.json', {'model': {'type': 'minecraft:model', 'model': 'sgcraft:block/office_chair'}})


if __name__ == '__main__':
    textures()
    models()
    print('Made the dialing computer, MALP console and office chair art')
