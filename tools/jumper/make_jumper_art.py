"""Makes the puddle jumper's model and texture.

The jumper is drawn after the Ancients' gate ship on Stargate Atlantis, as its designer, Bridget McGuire, shaped it
after a sewing machine's bobbin shuttle: a long tube, round enough to fly through a gate, its sides straight and its
top a broad arch over a flat underside, its front and back both raked and set in behind a lip of the tube. Its
plating is gunmetal with a blue cast, as the show lights it, in raised panels with grooves cut between them, which run
along its length, step up or down partway, and frame long six sided panels and notched ones, with long slots cut
deeper; its underside is a gray plate of machinery. In the face of its nose is a broad windshield, wider at the top,
its lower corners stepped in, in a rim of tarnished bronze, with two headlamps low down; the back is the cargo door,
with a small window near its top. In the lower sides of the rear half are the drive pods: each a fin of the hull
with a ribbed engine on its inner face, its back a grid of blue lights, pushed out for flight on a pair of arms from a
dark recess, its fin tipped down past flat so the engine rides on top, and drawn back in against the hull; the drone ports' doors are marked in the plating just ahead of them.

Inside, as on the set, the walls are dark bronze, cut by angular grooves, with lamps along them high up where the
arch begins, and frosted panels lit from behind: the dashboard across the nose, with the pilot's two hand controls
and the computer's screen before the co-pilot; a center console between the front seats with the dialer's orange
triangular tiles on top; two more seats behind them; the bulkhead, its doorway flanked by two lit consoles; and along
each wall of the rear compartment a bench of three seats. The seats are gray, padded in sculpted segments, on bronze
pedestals. Nothing is copied from the show; this is drawn from descriptions, photographs of the studio's models and
props, and stills.

Every cube is one material, so a wall that shows one color outside and another inside is two cubes, side by side.
This script lays every cube out on the texture and paints each face of it, working out where in the model each pixel
lies, so the plating's lines and mottling run on unbroken from one cube to the next. It writes the cubes to the
model's data file, which CubeModels reads, so the model and the texture always agree. It draws the item's icon
too, and writes the item's model.

Sizes are in pixels, a sixteenth of a block, as entity models are: up is negative y, the nose is towards negative z,
and the jumper's left is positive x. The hull is 44 wide and 40 tall, its hitbox, and 112 long. The solid parts players
walk among, in JumperHull, follow these shapes, and the seats, in JumperSeat, sit in these chairs.

Run from the repository root: python tools/jumper/make_jumper_art.py
Needs Pillow.
"""

import json
import math
import random
from pathlib import Path

from PIL import Image

ASSETS = Path('core/src/main/resources/assets/sgcraft')
TEXTURE = ASSETS / 'textures/entity/puddle_jumper.png'
ICON_TEXTURE = ASSETS / 'textures/item/puddle_jumper.png'
MODEL = ASSETS / 'entity_models/puddle_jumper.json'
TEXTURE_WIDTH = 1024

# The model's three layers: drawn solid, see-through, and glowing in the dark
MAIN, GLASS, GLOW = 'main', 'glass', 'glow'

# The hull: half its width, its height, half its length, how far its ends rake in towards the top for each pixel up,
# and the top of its straight sides, where the arch of its roof begins
HALF_WIDTH = 22
HEIGHT = 40
HALF_LENGTH = 56
RAKE = 0.25
ARCH = 21
# The drive pods' recesses: from the front to the back of each, its bottom and top, and how far in it goes
RECESS_FRONT, RECESS_BACK = 8, 48
RECESS_LOW, RECESS_HIGH = 6, 20
RECESS_IN = 14
# The arms each pod is pushed out and drawn back in on: how far out from the middle their bases are, at the backs of
# the recesses, how high, and how far back each pair is, as JumperModel has them
ARM_BASE = 15
ARM_HEIGHT = 12
ARMS = (26, 40)
# How far the nose's face and the cargo door are set in behind the lip of the hull's tube
END_SET = 2
# The bulkhead's front and back, and its doorway's half width and height
BULKHEAD = (-12, -10)
DOORWAY = (7, 34)

# How high the seats' cushions are, in blocks, and where the chairs are, as JumperSeat has them: to the left and ahead
SEAT_HEIGHT = 0.7
CHAIRS = [('helm', 0.5625, 2.375), ('dialer', -0.5625, 2.375),
          ('cockpit_left', 0.6875, 1.375), ('cockpit_right', -0.6875, 1.375)]
# The benches' seats' middles, ahead, in pixels as the model has them
BENCH_SEATS = [2, 16, 30]

# Where the dashboard's buttons are, as PuddleJumperEntity.Button has them, in blocks: to the left, up and ahead
BUTTONS = [('green', 1.05, 0.875, 2.95), ('amber', 0.85, 0.875, 2.95), ('cyan', 0.6, 0.875, 2.95), ('red', 0.35, 0.875, 2.95),
           ('yellow', 0.1, 0.875, 2.95), ('blue', -0.15, 0.875, 2.95)]

# Each pod's arm, as the show's, a smooth curve from the hull's side out and up, as JumperModel and
# PuddleJumperEntity have it: how high off the underside it leaves the hull, how far it runs straight out before it
# curves, the radius of its curve, how many segments make it, how thick it is, and from its front to its back
POD_BASE_UP = 8
POD_REACH = 6
POD_RADIUS = 11
POD_SEGMENTS = 9
POD_THICK = 2.5
POD_FRONT, POD_BACK = RECESS_FRONT + 6, RECESS_BACK - 4
# The drone bays, one on each lower side of the front cabin: an arm curving out of the hull and up as the pods' do,
# smaller and not out as far, which slides out as the drones are armed, the drones lying in its curve. As
# JumperModel and PuddleJumperEntity have them: how high off the underside it leaves the hull, how far it runs
# straight, the radius of its curve, from its front to its back; and where each drone lies, at what angle round the
# curve, how far in from it, and how far along it
BAY_BASE_UP = 8
BAY_REACH = 2
BAY_RADIUS = 7
BAY_SEGMENTS = 6
BAY_FRONT, BAY_BACK = -32, -4
BAY_DRONE_ANGLE = -45
BAY_DRONE_IN = 2.5
BAY_DRONES = (-29, -20, -11)

# The computer's screen, in its case on the dashboard before the co-pilot
SCREEN_CASE = ((-15.5, -21.5, -47), (13.6, 7.5, 1.5))

FRONT_ANGLE = -math.atan(RAKE)
BACK_ANGLE = math.atan(RAKE)
# How far along a raked end a height is
SLANT = math.sqrt(1 + RAKE * RAKE)


def half_width(up):
    """How far out the hull is at a height: a chamfer along the underside, the straight sides, then the arch."""
    if up <= 4:
        return 18 + up
    if up <= ARCH:
        return HALF_WIDTH
    t = min(1.0, (up - ARCH) / (HEIGHT - ARCH))
    return HALF_WIDTH * math.sqrt(max(0.0, 1 - t * t))


def front(up):
    return -HALF_LENGTH + up * RAKE


def back(up):
    return HALF_LENGTH - up * RAKE


def shell_ends(top):
    """Where a length of the hull's shell, up to a height, starts and stops: half way into the thickness of the
    nose and the cargo door, which are set in from the ends, at its top, where the raked ends come in furthest, so
    no corner of it pokes out past them."""
    return front(top) + END_SET - 0.5, back(top) - END_SET + 0.5


class Cube:
    def __init__(self, material, origin, size, layer=MAIN):
        self.material = material
        self.origin = origin
        self.size = size
        self.layer = layer
        self.u = self.v = 0
        self.part = None

    def region(self):
        """The cube's space on the texture, as the game lays out a cube's six faces."""
        w, h, d = self.size
        return math.ceil(2 * (d + w)) + 1, math.ceil(d + h) + 1


class Part:
    def __init__(self, name, cubes, offset=(0, 0, 0), rotation=(0, 0, 0)):
        self.name = name
        self.cubes = cubes
        self.offset = offset
        self.rotation = rotation
        for cube in cubes:
            cube.part = self

    def to_model(self, point):
        """A point in the part's own space, as it lies in the model's, at rest: turned as the game turns a part,
        about x, then y, then z, and moved to the part's place."""
        x, y, z = point
        rx, ry, rz = self.rotation
        c, s = math.cos(rx), math.sin(rx)
        y, z = y * c - z * s, y * s + z * c
        c, s = math.cos(ry), math.sin(ry)
        x, z = x * c + z * s, -x * s + z * c
        c, s = math.cos(rz), math.sin(rz)
        x, y = x * c - y * s, x * s + y * c
        ox, oy, oz = self.offset
        return x + ox, y + oy, z + oz


# The hull

def bevel(name, start, end, sign, inside, material='hull'):
    """A sloped panel along the hull between two points of its cross-section, plating outside and, if asked, the
    cabin's panels inside, running front to back at its middle's height."""
    (x0, y0), (x1, y1) = start, end
    length = math.hypot(x1 - x0, y1 - y0)
    angle = math.atan2(x1 - x0, -(y1 - y0))
    z0, z1 = shell_ends(max(-y0, -y1))
    if sign > 0:
        cubes = [Cube(material, (-1, -length, z0), (1, length, z1 - z0))]
        if inside:
            cubes.append(Cube('interior', (-2, -length, z0 + 1), (1, length, z1 - z0 - 2)))
    else:
        cubes = [Cube(material, (0, -length, z0), (1, length, z1 - z0))]
        if inside:
            cubes.append(Cube('interior', (1, -length, z0 + 1), (1, length, z1 - z0 - 2)))
    return Part(name, cubes, offset=(x0, y0, 0), rotation=(0, 0, angle))


# Where the cabin's frames are, ribs standing out of its walls and over its arch, in pixels back from the middle:
# all well inside the nose and the cargo door, which rake in towards the top
RIBS = (-38, -28, -6, 38)


def arch():
    """The roof's arch, in panels between points on it, each side, from the top of the straight sides to the top,
    with the cabin's frames as ribs across their insides."""
    points = []
    for step in range(7):
        phi = math.radians(step * 15)
        points.append((HALF_WIDTH * math.cos(phi), ARCH + (HEIGHT - ARCH) * math.sin(phi)))
    parts = []
    for i in range(6):
        (xa, ua), (xb, ub) = points[i], points[i + 1]
        left = bevel(f'left_arch_{i}', (xa, -ua), (xb, -ub), 1, True)
        right = bevel(f'right_arch_{i}', (-xa, -ua), (-xb, -ub), -1, True)
        length = left.cubes[0].size[1]
        for z in RIBS:
            left.cubes.append(Cube('rib', (-3, -length, z), (1, length, 2.5)))
            right.cubes.append(Cube('rib', (2, -length, z), (1, length, 2.5)))
        for cube in left.cubes + right.cubes:
            cube.part = left if cube in left.cubes else right
        parts += [left, right]
    return parts


def walls():
    """The straight sides, in rows two pixels high, each running from the front to the back at its height: plating
    outside and panels inside, but around the drive pods' recesses, whose backs the cabin's walls are there."""
    cubes = []
    # The top row reaches a pixel up under the arch, so no seam shows between them
    rows = [(low, min(low + 2, ARCH + 1)) for low in range(4, ARCH, 2)]
    for low, high in rows:
        z0, z1 = shell_ends(high)
        recess = RECESS_LOW <= low and high <= RECESS_HIGH
        for sign in (1, -1):
            outer = HALF_WIDTH - 1 if sign > 0 else -HALF_WIDTH
            inner = HALF_WIDTH - 2 if sign > 0 else -HALF_WIDTH + 1
            spans = [(z0, RECESS_FRONT), (RECESS_BACK, z1)] if recess else [(z0, z1)]
            for a, b in spans:
                cubes.append(Cube('hull', (outer, -high, a), (1, high - low, b - a)))
                ia, ib = max(a, z0 + 1), min(b, z1 - 1)
                cubes.append(Cube('interior', (inner, -high, ia), (1, high - low, ib - ia)))
    # The recesses: their backs, the cabin's walls behind the benches, and their ends
    height = RECESS_HIGH - RECESS_LOW
    length = RECESS_BACK - RECESS_FRONT
    for sign in (1, -1):
        def x(at, width):
            return at if sign > 0 else -at - width
        cubes.append(Cube('recess', (x(RECESS_IN, 1), -RECESS_HIGH, RECESS_FRONT), (1, height, length)))
        cubes.append(Cube('interior', (x(RECESS_IN - 1, 1), -RECESS_HIGH, RECESS_FRONT), (1, height, length)))
        for z in (RECESS_FRONT, RECESS_BACK - 0.5):
            cubes.append(Cube('recess', (x(RECESS_IN + 1, HALF_WIDTH - 2 - RECESS_IN), -RECESS_HIGH, z),
                              (HALF_WIDTH - 2 - RECESS_IN, height, 0.5)))
    return cubes


def end_rows(outer_material, window, inner_offset, outer_offset, rim_rows):
    """An end of the hull as rows across it, two pixels high up its slope, each as wide as the hull at its height:
    plating outside and panels inside, with a window cut into it where asked, framed in bronze. The cubes lie in
    the end's own frame, up its slope as negative y. Returns the solid cubes and the glass."""
    solid, glass = [], []
    for low in range(0, HEIGHT, 2):
        high = min(low + 2, HEIGHT)
        width = half_width((low + high) / 2)
        if width < 0.5:
            continue
        y0, y1 = -high * SLANT, -low * SLANT
        span = y1 - y0
        open_half = window(low, high)
        rim_half = None
        for rlow, rhalf in rim_rows:
            if rlow == low:
                rim_half = rhalf

        def row(material, x0, x1, z, layer=MAIN):
            if x1 - x0 > 0.01:
                (glass if layer == GLASS else solid).append(Cube(material, (x0, y0, z), (x1 - x0, span, 1), layer))

        if open_half:
            rim = 1.5
            row(outer_material, -width, -open_half - rim, outer_offset)
            row('rim', -open_half - rim, -open_half, outer_offset)
            row('glass', -open_half, open_half, outer_offset, GLASS)
            row('rim', open_half, open_half + rim, outer_offset)
            row(outer_material, open_half + rim, width, outer_offset)
            row('interior', -width + 1, -open_half - rim, inner_offset)
            row('interior', open_half + rim, width - 1, inner_offset)
        elif rim_half:
            row(outer_material, -width, -rim_half, outer_offset)
            row('rim', -rim_half, rim_half, outer_offset)
            row(outer_material, rim_half, width, outer_offset)
            row('interior', -width + 1, width - 1, inner_offset)
        else:
            row(outer_material, -width, width, outer_offset)
            row('interior', -width + 1, width - 1, inner_offset)
    return solid, glass


def windshield(low, high):
    """How far out from the middle the windshield reaches in a row of the nose, if it is in that row: wider at the
    top than the bottom, its lower corners stepped in, and under the arch at its top."""
    if low < 16 or high > 36:
        return None
    if low < 18:
        half = 7
    elif low < 20:
        half = 10
    elif low < 30:
        half = 13
    elif low < 32:
        half = 12.5
    elif low < 34:
        half = 11
    else:
        half = 8.5
    return min(half, half_width(high) - 4)


def hatch_window(low, high):
    """The small window near the top of the cargo door."""
    return 5 if 28 <= low and high <= 32 else None


def hull_parts():
    wall_cubes = walls()
    hull = Part('hull', [
        # The underside, its machinery's plate, and the cabin's floor
        Cube('belly', (-18, -1, shell_ends(1)[0]), (36, 1, shell_ends(1)[1] - shell_ends(1)[0])),
        Cube('floor', (-20, -4, front(4) + END_SET + 1), (40, 3, back(4) - front(4) - 2 * END_SET - 2)),
    ] + wall_cubes)
    chamfers = [
        bevel('left_chamfer', (18, 0), (22, -4), 1, False),
        bevel('right_chamfer', (-18, 0), (-22, -4), -1, False),
    ]
    nose_solid, nose_glass = end_rows('hull_front', windshield, 0, -1, [(14, 9), (36, 10)])
    nose = Part('nose', nose_solid + nose_glass, offset=(0, 0, -HALF_LENGTH + END_SET),
                rotation=(FRONT_ANGLE, 0, 0))
    door_solid, door_glass = end_rows('hull_back', hatch_window, -1, 0, [(26, 6.5), (32, 6.5)])
    door = Part('cargo_door', door_solid + door_glass, offset=(0, 0, HALF_LENGTH - END_SET),
                rotation=(BACK_ANGLE, 0, 0))
    lamps = Part('headlamps', [
        Cube('lamp', (5, -8 * SLANT, -1.3), (5, 3 * SLANT, 0.3), GLOW),
        Cube('lamp', (-10, -8 * SLANT, -1.3), (5, 3 * SLANT, 0.3), GLOW),
    ], offset=(0, 0, -HALF_LENGTH + END_SET), rotation=(FRONT_ANGLE, 0, 0))
    return [hull] + chamfers + arch() + [nose, door, lamps]


def pod(name, sign):
    """A drive pod's door, a flap of the hull's lower side over its recess, a dark rail along its inner face's edge
    and an amber running light. JumperModel swings it down and out of the way before the arm slides out, and shuts it
    behind the arm once it is drawn back in."""
    def x(at, width):
        return at if sign > 0 else -at - width
    height = RECESS_HIGH - RECESS_LOW
    return Part(name, [
        Cube('hull', (x(-1.5, 1.5), -height, RECESS_FRONT), (1.5, height, RECESS_BACK - RECESS_FRONT)),
        Cube('hull_dark', (x(-2.5, 1), -height + 2, RECESS_FRONT + 2), (1, 1.5, RECESS_BACK - RECESS_FRONT - 4)),
        Cube('amber', (x(-2, 0.5), -5, 34), (0.5, 1.5, 1.5), GLOW),
    ], offset=(HALF_WIDTH * sign, -RECESS_LOW, 0))


def arc_point(sign, angle, inward=0.0, reach=POD_REACH, radius=POD_RADIUS, base_up=POD_BASE_UP):
    """A point on a curved arm, a pod's or a drone bay's, out with it, at an angle round its curve from the bottom,
    -90, to the top, 0, so far in from the curve's middle line, in the model's space; and which way the curve runs
    there, as an angle about z."""
    theta = math.radians(angle)
    r = radius - inward
    u = reach + r * math.cos(theta)
    v = radius + r * math.sin(theta)
    tangent = math.atan2(-math.cos(theta), -math.sin(theta) * sign)
    return ((HALF_WIDTH + u) * sign, -base_up - v), tangent


def curved_arm(prefix, sign, segments, reach, radius, base_up, front, back):
    """A curved arm's parts: a straight run out of the hull's side, then segments making a smooth curve up, which
    JumperModel slides out of the hull and back in as one."""
    parts = []
    step = 90 / segments
    length = math.radians(step) * radius + 0.6
    for i in range(segments):
        (ox, oy), tangent = arc_point(sign, -90 + step * (i + 0.5), 0, reach, radius, base_up)
        parts.append(Part(f'{prefix}_{i}', [
            Cube('hull', (-length / 2, -POD_THICK / 2, front), (length, POD_THICK, back - front)),
        ], offset=(ox, oy, 0), rotation=(0, 0, tangent)))
    parts.append(Part(f'{prefix}_run', [
        Cube('hull', (-reach / 2 - 0.3, -POD_THICK / 2, front), (reach + 0.6, POD_THICK, back - front)),
    ], offset=((HALF_WIDTH + reach / 2) * sign, -base_up, 0)))
    return parts


def pod_engine(name, sign):
    """A pod's long, ribbed engine, its back a grid of blue lights round its drive, its base set on the end of the
    arm's curve and its block standing up from it, leaning in towards the hull; folding, it tips down to lie facing
    the hull, as JumperModel turns it."""
    def x(at, width):
        return at if sign > 0 else -at - width
    cubes = [
        Cube('engine', (x(-3, 6), -14, 22), (6, 14, 20)),
        Cube('engine', (x(-2.25, 4.5), -12, 19), (4.5, 10, 3)),
        Cube('hull_dark', (x(-3.5, 7), -14.5, 42), (7, 14.5, 2.5)),
        Cube('engine_lights', (x(-2.75, 5.5), -13.25, 44.5), (5.5, 12.5, 0.25), GLOW),
    ]
    for rib in (25, 31, 37):
        cubes.append(Cube('hull_dark', (x(-3.5, 7), -14.5, rib), (7, 14.5, 1.5)))
    (ox, oy), _ = arc_point(sign, 0)
    return Part(name, cubes, offset=(ox, oy, 0))


def bay_drone(name, sign, z):
    """A drone in its bay's curve, golden and glowing, a long body and a blunt nose ahead, turned with the curve."""
    (ox, oy), tangent = arc_point(sign, BAY_DRONE_ANGLE, BAY_DRONE_IN, BAY_REACH, BAY_RADIUS, BAY_BASE_UP)
    return Part(name, [
        Cube('drone', (-1.25, -1.25, z + 1), (2.5, 2.5, 6), GLOW),
        Cube('drone', (-0.75, -0.75, z), (1.5, 1.5, 1), GLOW),
    ], offset=(ox, oy, 0), rotation=(0, 0, tangent))


def arm(name, sign, z):
    """An arm a pod is pushed out and drawn back on: a sleeve from the back of the recess and a rod from it, a pixel
    long, which JumperModel turns towards the pod's engine and stretches to reach it."""
    def x(at, width):
        return at if sign > 0 else -at - width
    return Part(name, [
        Cube('hull_dark', (x(0, 0.5), -1.25, -1.25), (0.5, 2.5, 2.5)),
        Cube('engine', (x(0.5, 0.5), -0.75, -0.75), (0.5, 1.5, 1.5)),
    ], offset=(ARM_BASE * sign, -ARM_HEIGHT, z))


# The cabin

def chair(name, left, forward):
    """A cockpit chair, facing ahead, as the set's: on a bronze pedestal, a seat of gray pads, its cushion between two
    bolsters with a roll along its front, and a tall back of the same, a column of three pads between two long
    bolsters on a dark frame, under a headrest notched in the middle; dark arms to either side."""
    x, z = left * 16, -forward * 16
    cushion = -SEAT_HEIGHT * 16
    cubes = [
        Cube('console', (-1.5, -7.5, -1.5), (3, 3.5, 3)),
        Cube('console', (-3, -4.5, -3), (6, 0.5, 6)),
        Cube('seat_dark', (-4, -8.5, -4), (8, 1, 8)),
        Cube('seat', (-2.6, cushion, -3.4), (5.2, -8.5 - cushion, 7)),
        Cube('seat', (-4.3, cushion - 0.8, -4), (1.7, -8.5 - cushion + 0.8, 8)),
        Cube('seat', (2.6, cushion - 0.8, -4), (1.7, -8.5 - cushion + 0.8, 8)),
        Cube('seat', (-2.6, cushion - 0.4, -4.6), (5.2, 1.6, 1.2)),
        Cube('seat_dark', (-4, -27.5, 5.1), (8, 27.5 + cushion, 1.4)),
        Cube('seat', (-4.7, -25.5, 3), (1.9, 25.5 + cushion, 2.6)),
        Cube('seat', (2.8, -25.5, 3), (1.9, 25.5 + cushion, 2.6)),
        Cube('seat', (-3.2, -30.5, 3.6), (6.4, 3.4, 2.2)),
        Cube('seat_dark', (-0.9, -30.2, 3.3), (1.8, 1.8, 0.4)),
        Cube('seat_dark', (-5.3, cushion - 3, -3), (1, 1, 6)),
        Cube('seat_dark', (4.3, cushion - 3, -3), (1, 1, 6)),
        Cube('seat_dark', (-5.2, cushion - 2, 1.5), (0.8, 2, 1)),
        Cube('seat_dark', (4.4, cushion - 2, 1.5), (0.8, 2, 1)),
    ]
    for top in (-25, -20.6, -16.2):
        cubes.append(Cube('seat', (-2.7, top, 3.7), (5.4, 4, 1.5)))
    return Part(name, cubes, offset=(x, 0, z))


def bench(name, sign):
    """A bench along a wall of the rear compartment, its back to the wall over the drive pod's recess: three seats of
    gray pads like the chairs', each a cushion with a roll along its front and a back of two pads, on a dark frame
    with dark dividers between."""
    def x(at, width):
        return at if sign > 0 else -at - width
    cushion = -SEAT_HEIGHT * 16
    z0, z1 = BENCH_SEATS[0] - 6, BENCH_SEATS[-1] + 6
    cubes = [
        Cube('console', (x(8.5, 4.5), -7, z0), (4.5, 3, z1 - z0)),
        Cube('seat_dark', (x(7.5, 5.5), -8, z0), (5.5, 1, z1 - z0)),
        Cube('seat_dark', (x(12.6, 1.2), -28, z0), (1.2, 28 + cushion, z1 - z0)),
    ]
    for middle in BENCH_SEATS:
        cubes += [
            Cube('seat', (x(7.6, 4.8), cushion, middle - 5.4), (4.8, -8 - cushion, 10.8)),
            Cube('seat', (x(7.1, 1.2), cushion - 0.4, middle - 5.4), (1.2, 1.4, 10.8)),
            Cube('seat', (x(11.2, 1.4), -26, middle - 5), (1.4, 6.6, 10)),
            Cube('seat', (x(11.2, 1.4), -19, middle - 5), (1.4, 19 + cushion - 0.4, 10)),
        ]
    for between_seats in (BENCH_SEATS[0] + 6, BENCH_SEATS[1] + 6):
        cubes.append(Cube('seat_dark', (x(7.4, 4.9), cushion - 3, between_seats + 0.3), (4.9, 3, 1)))
        cubes.append(Cube('seat_dark', (x(11, 1.6), -27, between_seats + 0.3), (1.6, 27 + cushion, 1)))
    return Part(name, cubes)


def frames():
    """The cabin's frames up its walls, ribs standing out of them where the benches and the consoles leave the wall
    bare, as the arch's ribs do over them; and a dark spine along the middle of the ceiling."""
    cubes = [Cube('rib', (-2, -38, -48), (4, 0.6, 96))]
    for z in RIBS:
        # Over the drive pods' recesses the cabin's wall is further in, behind the benches
        if RECESS_FRONT <= z <= RECESS_BACK:
            continue
        for sign in (1, -1):
            inner = HALF_WIDTH - 2 if sign > 0 else -HALF_WIDTH + 2
            cubes.append(Cube('rib', (inner - 1 if sign > 0 else inner, -(ARCH + 1), z), (1, ARCH + 1 - 4, 2.5)))
    return Part('frames', cubes)


def bulkhead():
    """The bulkhead across the cabin, filling the hull's cross-section but for its doorway, with a lit console to
    either side of the doorway on its cockpit side."""
    z0, z1 = BULKHEAD
    door_half, door_high = DOORWAY
    cubes = []
    for low in range(4, HEIGHT - 1, 2):
        high = low + 2
        width = half_width(high) - 1.5
        if width <= 0:
            continue
        if high <= door_high:
            if width > door_half:
                cubes.append(Cube('interior', (door_half, -high, z0), (width - door_half, 2, z1 - z0)))
                cubes.append(Cube('interior', (-width, -high, z0), (width - door_half, 2, z1 - z0)))
        else:
            cubes.append(Cube('interior', (-width, -high, z0), (2 * width, 2, z1 - z0)))
    # The doorway's frame, and the consoles
    cubes += [
        Cube('hull_dark', (door_half - 0.5, -door_high, z0 - 0.25), (0.5, door_high - 4, z1 - z0 + 0.5)),
        Cube('hull_dark', (-door_half, -door_high, z0 - 0.25), (0.5, door_high - 4, z1 - z0 + 0.5)),
        Cube('hull_dark', (-door_half, -door_high - 0.5, z0 - 0.25), (2 * door_half, 0.5, z1 - z0 + 0.5)),
        Cube('console', (8, -26, z0 - 4), (11, 22, 4)),
        Cube('console', (-19, -26, z0 - 4), (11, 22, 4)),
        Cube('panel', (9, -24, z0 - 4.25), (9, 13, 0.25), GLOW),
        Cube('panel', (-18, -24, z0 - 4.25), (9, 13, 0.25), GLOW),
    ]
    return Part('bulkhead', cubes)


def cockpit():
    """The dashboard across the nose, raised in steps, its frosted panels, the pilot's lit control plate in a bronze
    bezel and two hand controls; the computer's screen in its case before the co-pilot; the center console between
    the front seats, the dialer lit on its top; and a desk on each wall beside the chairs behind, as on the set, its
    top lit."""
    (cx, cy, cz), (cw, ch, cd) = SCREEN_CASE
    return Part('cockpit', [
        Cube('console', (-20, -14, -52), (40, 10, 8)),
        Cube('console', (-19, -15, -48), (38, 1, 4)),
        Cube('panel', (2, -14.25, -51), (16, 0.25, 6), GLOW),
        Cube('panel', (-18, -14.25, -51), (16, 0.25, 3), GLOW),
        Cube('seat_dark', (5, -17.5, -46.5), (2.5, 2.5, 2.5)),
        Cube('seat_dark', (11, -17.5, -46.5), (2.5, 2.5, 2.5)),
        Cube('console', (cx, cy, cz), (cw, ch, cd)),
        Cube('console', (-4, -12, -44), (8, 8, 12)),
        Cube('dhd', (-3, -12.25, -43), (6, 0.25, 9), GLOW),
        Cube('console', (-17, -17, -51.5), (34, 3, 3)),
        Cube('console', (3.5, -15.4, -50.5), (11, 1, 5.5)),
        Cube('control', (4.25, -15.5, -50), (9.5, 0.2, 4.5), GLOW),
        Cube('console', (14, -14, -30), (5.5, 10, 12)),
        Cube('console', (-19.5, -14, -30), (5.5, 10, 12)),
        Cube('panel', (14.5, -14.25, -29), (4.5, 0.25, 10), GLOW),
        Cube('panel', (-19, -14.25, -29), (4.5, 0.25, 10), GLOW),
    ] + [Cube(material, (left * 16 - 1, -up * 16 - 0.5, -ahead * 16 - 1), (2, 0.5, 2), GLOW)
         for material, left, up, ahead in BUTTONS])


def sconces():
    """The lamps along the cabin's walls, high up where the arch begins, as on the set."""
    cubes = []
    for z in (-40, -24, 0, 18, 36):
        cubes.append(Cube('sconce', (17.8, -28, z), (1.5, 2, 7), GLOW))
        cubes.append(Cube('sconce', (-19.3, -28, z), (1.5, 2, 7), GLOW))
        cubes.append(Cube('console', (18.6, -28.6, z - 0.5), (1, 3.2, 8)))
        cubes.append(Cube('console', (-19.6, -28.6, z - 0.5), (1, 3.2, 8)))
    return Part('sconces', cubes)


def parts():
    return (hull_parts() + [cockpit(), bulkhead(), sconces(), frames()]
            + [chair(*c) for c in CHAIRS] + [bench('left_bench', 1), bench('right_bench', -1)]
            + [pod('left_pod', 1), pod('right_pod', -1), pod_engine('left_engine', 1), pod_engine('right_engine', -1)]
            + [part for side, sign in (('left', 1), ('right', -1))
               for part in curved_arm(f'{side}_arc', sign, POD_SEGMENTS, POD_REACH, POD_RADIUS, POD_BASE_UP,
                                      POD_FRONT, POD_BACK)
               + curved_arm(f'{side}_bay', sign, BAY_SEGMENTS, BAY_REACH, BAY_RADIUS, BAY_BASE_UP, BAY_FRONT,
                            BAY_BACK)]
            + [bay_drone(f'{side}_drone_{i}', sign, z) for side, sign in (('left', 1), ('right', -1))
               for i, z in enumerate(BAY_DRONES)]
            + [arm(f'{side}_arm_{i}', sign, z) for side, sign in (('left', 1), ('right', -1))
               for i, z in enumerate(ARMS)])


# The texture

def pack(cubes):
    """Lays the cubes' spaces out in rows, tallest first, returning how tall the texture must be."""
    x = y = row = 0
    for cube in sorted(cubes, key=lambda c: (-c.region()[1], -c.region()[0])):
        w, h = cube.region()
        if x + w > TEXTURE_WIDTH:
            x, y, row = 0, y + row, 0
        cube.u, cube.v = x, y
        x += w
        row = max(row, h)
    height = y + row
    size = 64
    while size < height:
        size *= 2
    return size


def faces(cube):
    """Each face of a cube as the game maps it: its place on the texture, and for a pixel across and down it, the
    point on the cube it shows, in the cube's own space."""
    (x0, y0, z0), (w, h, d) = cube.origin, cube.size
    x1, y1, z1 = x0 + w, y0 + h, z0 + d
    u, v = cube.u, cube.v
    return [
        # Down and up, at the cube's least and most y
        ((u + d, v, w, d), lambda a, b: (x0 + a, y0, z1 - b)),
        ((u + d + w, v, w, d), lambda a, b: (x0 + a, y1, z1 - b)),
        # West, north, east and south
        ((u, v + d, d, h), lambda a, b: (x0, y0 + b, z1 - a)),
        ((u + d, v + d, w, h), lambda a, b: (x0 + a, y0 + b, z0)),
        ((u + d + w, v + d, d, h), lambda a, b: (x1, y0 + b, z0 + a)),
        ((u + 2 * d + w, v + d, w, h), lambda a, b: (x1 - a, y0 + b, z1)),
    ]


def hash01(*values):
    h = 0x9E3779B1
    for value in values:
        h ^= (value * 0x85EBCA6B) & 0xFFFFFFFF
        h = ((h << 13) | (h >> 19)) & 0xFFFFFFFF
        h = (h * 5 + 0xE6546B64) & 0xFFFFFFFF
    return (h & 0xFFFFFF) / 0xFFFFFF


def smooth_noise(a, b, scale, seed=0):
    """Value noise over a surface, smooth between grid points so many pixels apart."""
    ga, gb = a / scale, b / scale
    ia, ib = math.floor(ga), math.floor(gb)
    fa, fb = ga - ia, gb - ib
    fa, fb = fa * fa * (3 - 2 * fa), fb * fb * (3 - 2 * fb)
    n00, n10 = hash01(ia, ib, seed), hash01(ia + 1, ib, seed)
    n01, n11 = hash01(ia, ib + 1, seed), hash01(ia + 1, ib + 1, seed)
    return (n00 * (1 - fa) + n10 * fa) * (1 - fb) + (n01 * (1 - fa) + n11 * fa) * fb


def around(x, up):
    """How far round the hull's cross-section a point is, from the middle of the underside, in pixels."""
    ax = abs(x)
    if up <= 4.5 and ax <= 18:
        return ax * 0.3
    if up <= 4.5:
        return 5.4 + up * 1.4
    if up <= ARCH:
        return 11.7 + (up - 4)
    phi = math.atan2((up - ARCH) / (HEIGHT - ARCH), ax / HALF_WIDTH)
    return 28.7 + phi * 20.5


def hexagon(z0, z1, s0, s1):
    cut = (s1 - s0) / 2
    return [(z0 + cut, s0), (z1 - cut, s0), (z1, (s0 + s1) / 2), (z1 - cut, s1), (z0 + cut, s1), (z0, (s0 + s1) / 2),
            (z0 + cut, s0)]


def distance_to_path(p, points):
    best = 1e9
    for i in range(len(points) - 1):
        (px, py), (ax, ay), (bx, by) = p, points[i], points[i + 1]
        dx, dy = bx - ax, by - ay
        t = max(0.0, min(1.0, ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)))
        best = min(best, math.hypot(px - ax - t * dx, py - ay - t * dy))
    return best


def notched(z0, z1, s0, s1, cut, notch):
    """A panel's outline, its back corner cut and a step out of its top at the back."""
    return [(z0, s0), (z1 - cut, s0), (z1, s0 + cut), (z1, s1 - notch), (z1 - notch, s1 - notch),
            (z1 - notch, s1), (z0, s1), (z0, s0)]


# The Ancients' panel work along the plating, as on the show: grooves cut between raised panels, paths over its
# length (z) and round it (s), the same each side; and slots, long and dark, cut deeper
SIDE_GROOVES = [
    [(-54, 40), (-2, 40), (4, 44), (54, 44)],
    notched(-50, -6, 43, 59, 4, 3),
    [(8, 47), (46, 47), (50, 51), (50, 59), (8, 59), (8, 47)],
    hexagon(-44, -16, 17, 27),
    hexagon(14, 40, 30, 39),
    [(-5, 13), (5, 13), (5, 24), (-5, 24), (-5, 13)],
    [(-53, 9), (-30, 9), (-26, 12), (-8, 12)],
    [(-50, 30), (-44, 30), (-44, 34)],
    [(44, 24), (52, 24), (52, 30)],
]
SIDE_SLOTS = [(-46, 53, -20, 55.5), (-44, 46.5, -24, 49), (-24, 48.5, -13, 51), (-12, 51, -9.5, 54),
              (12, 52, 44, 54.5), (16, 34, 36, 35.5)]


def side_trim(z, s):
    p = (z, s)
    for z0, s0, z1, s1 in SIDE_SLOTS:
        if z0 <= z <= z1 and s0 <= s <= s1:
            return 'slot'
    d = min(distance_to_path(p, path) for path in SIDE_GROOVES)
    if d < 0.8:
        return 'line'
    if d < 1.5:
        return 'edge'
    return None


def front_trim(x, up):
    ax = abs(x)
    p = (ax, up)
    d = min(distance_to_path(p, [(3, 11), (12, 11), (15, 8), (19, 8)]), distance_to_path(p, [(0, 5), (4, 5)]),
            distance_to_path(p, [(16, 14), (19, 14), (19, 28)]))
    if d < 0.8:
        return 'line'
    if d < 1.5:
        return 'edge'
    return None


def back_trim(x, up):
    ax = abs(x)
    p = (ax, up)
    edge = half_width(up) - 3.5
    if 2 < up < 36 and abs(ax - edge) < 0.8:
        return 'line'
    d = min(distance_to_path(p, [(0, 12), (10, 12), (13, 15), (13, 24)]), distance_to_path(p, [(0, 22), (8, 22)]))
    if d < 0.8:
        return 'line'
    if d < 1.5:
        return 'edge'
    return None


# Gunmetal with a blue cast, as the show lights it; the grooves between its panels, their lit edges, and its slots
METAL = (88, 96, 108)
METAL_DARK = (72, 79, 90)
GROOVE = (38, 43, 51)
GROOVE_EDGE = (112, 120, 132)
SLOT = (30, 34, 41)


def plating(point, kind, rng):
    """A pixel of the hull's plating, where it lies in the model."""
    x, y, z = point
    up = -y
    if kind == 'front':
        a, b = x, up
        trim = front_trim(x, up)
    elif kind == 'back':
        a, b = x, up
        trim = back_trim(x, up)
    else:
        a, b = z, around(x, up)
        trim = side_trim(a, b)
    if trim == 'line':
        color = GROOVE
    elif trim == 'slot':
        color = SLOT
    else:
        mottle = smooth_noise(a, b + (100 if x < 0 else 0), 9, 1) * 0.7 + smooth_noise(a, b, 3, 2) * 0.3
        t = max(0.0, min(1.0, (mottle - 0.35) * 1.4))
        color = tuple(round(g + (d - g) * t) for g, d in zip(METAL, METAL_DARK))
        if trim == 'edge':
            color = tuple(round(c * 0.5 + e * 0.5) for c, e in zip(color, GROOVE_EDGE))
        # Worn bright here and there, where hands and weather have rubbed it
        elif hash01(round(a * 2), round(b * 2), 7) > 0.996:
            color = tuple(min(255, c + 26) for c in color)
    d = rng.randint(-2, 2)
    return tuple(max(0, min(255, c + d)) for c in color) + (255,)


def bronze(point, rng, base, seed):
    """Dark bronze, a little green with patina, its panels cut by angular grooves as the set's walls are: a zigzag
    along the walls and steps up and down between panels."""
    x, y, z = point
    a, b = z + x * 0.7, y + x * 0.3
    patina = smooth_noise(a, b, 6, seed)
    t = max(0.0, min(1.0, (patina - 0.45) * 2.2))
    teal = (70, 96, 84)
    color = tuple(round(c + (p - c) * t * 0.45) for c, p in zip(base, teal))
    up = -y
    zig = 25 + 5 * abs(((z + 200) / 6) % 2 - 1)
    # The grooves, dark, with a lit edge under each, so they read as cut into raised panels
    if abs(up - zig) < 0.55 or abs((z + 203) % 18) < 0.5 and 6 < up < 22:
        color = tuple(round(c * 0.55) for c in color)
    elif abs(up - zig + 0.9) < 0.4 or abs((z + 202.2) % 18) < 0.4 and 6 < up < 22:
        color = tuple(min(255, round(c * 1.3)) for c in color)
    else:
        # Each panel a little lighter towards its top, as the lamps light it
        color = tuple(min(255, round(c * (0.9 + 0.006 * (up % 18)))) for c in color)
    d = rng.randint(-3, 3)
    return tuple(max(0, min(255, c + d)) for c in color) + (255,)


def material(cube, point, px, py, rng):
    """A pixel of a cube's face: by its material, where it lies in the model, and its place on the texture."""
    name = cube.material

    def noisy(color, spread):
        d = rng.randint(-spread, spread)
        return tuple(max(0, min(255, c + d)) for c in color[:3]) + (color[3] if len(color) > 3 else 255,)

    if name == 'hull':
        return plating(point, 'side', rng)
    if name == 'hull_front':
        return plating(point, 'front', rng)
    if name == 'hull_back':
        return plating(point, 'back', rng)
    if name == 'hull_dark':
        return noisy((46, 51, 60), 3)
    if name == 'recess':
        return noisy((30, 34, 40), 3)
    if name == 'engine':
        ring = abs((point[2] + 200) % 5) < 0.7
        return noisy((60, 66, 76) if ring else (92, 100, 112), 3)
    if name == 'belly':
        x, y, z = point
        line = abs((z + 200) % 14) < 0.7 or abs(abs(x) - 9) < 0.6 or abs(abs(x) - 15) < 0.6
        return noisy((78, 80, 82) if line else (100, 102, 104), 3)
    if name == 'rim':
        return noisy((150, 118, 76), 5)
    if name == 'interior':
        return bronze(point, rng, (80, 74, 56), 11)
    if name == 'console':
        return bronze(point, rng, (66, 60, 46), 12)
    if name == 'floor':
        x, y, z = point
        channel = abs(x) < 3
        grate = (px + py) % 3 == 0
        return noisy((34, 36, 36) if channel and grate else (40, 44, 42) if channel else (44, 48, 46) if grate
                     else (60, 64, 60), 3)
    if name == 'seat':
        # Gray padding in sculpted segments
        x, y, z = point
        # Gray padding, darker in its seams and lit towards the top of each pad
        seam = abs((y + 200) % 4) < 0.35
        shade = 0.92 + 0.1 * (((-y + 200) % 4) / 4)
        base = (78, 82, 86) if seam else tuple(round(c * shade) for c in (122, 126, 128))
        return noisy(base, 3)
    if name == 'seat_dark':
        return noisy((52, 55, 58), 3)
    if name == 'glass':
        return noisy((38, 52, 58, 185), 3)
    if name == 'panel':
        # Frosted panels lit from behind, bronze markings across them as rows of buttons
        mark = px % 4 == 0 or py % 3 == 0
        return noisy((150, 112, 66) if mark else (255, 214, 150), 4)
    if name == 'dhd':
        # The dialer's tiles, orange triangles in rows
        i, j = px % 3, py % 3
        if i == 2 or j == 2:
            return noisy((70, 40, 26), 3)
        return noisy((214, 110, 48) if i <= j else (168, 80, 36), 4)
    if name == 'rib':
        return bronze(point, rng, (104, 94, 70), 13)
    if name == 'control':
        lit = (px + py) % 4 == 0
        return noisy((210, 240, 255) if lit else (120, 196, 230), 4)
    if name == 'sconce':
        return noisy((255, 222, 168), 4)
    if name == 'engine_lights':
        lit = px % 2 == 0 and py % 2 == 0
        return noisy((120, 170, 255) if lit else (16, 22, 36), 4)
    if name == 'amber':
        return noisy((255, 178, 72), 6)
    if name == 'cyan':
        return noisy((190, 235, 255), 6)
    if name == 'green':
        return noisy((90, 230, 130), 6)
    if name == 'red':
        return noisy((232, 70, 60), 6)
    if name == 'lamp':
        return noisy((255, 250, 222), 4)
    if name == 'blue':
        return noisy((90, 160, 250), 6)
    if name == 'yellow':
        return noisy((250, 214, 70), 6)
    if name == 'drone':
        # Golden, brightest down its middle
        core = (px + py) % 3 == 0
        return noisy((255, 236, 160) if core else (240, 176, 64), 5)
    raise ValueError(name)


def paint(cubes, height):
    rng = random.Random(31)
    image = Image.new('RGBA', (TEXTURE_WIDTH, height), (0, 0, 0, 0))
    for cube in cubes:
        for (fu, fv, fw, fh), at in faces(cube):
            # Every pixel the face touches, even in part, as the game samples it
            for py in range(math.floor(fv), max(math.floor(fv) + 1, math.ceil(fv + fh))):
                for px in range(math.floor(fu), max(math.floor(fu) + 1, math.ceil(fu + fw))):
                    a = min(max(px + 0.5 - fu, 0), fw)
                    b = min(max(py + 0.5 - fv, 0), fh)
                    image.putpixel((px, py), material(cube, cube.part.to_model(at(a, b)), px, py, rng))
    return image


# The item's icon, the jumper from its left side, nose to the right: plating (h), its dark lines (d), the windshield
# (w), the bronze rim (r), the gray underside (b), the drive pod's engine glow (a), and nothing (.)
ICON = [
    '................',
    '................',
    '................',
    '................',
    '...hhhhhhhhh....',
    '..hhhhhhhhhhhr..',
    '.hhddddhhhhhhrw.',
    '.hhhhhhdddhhhrw.',
    'hhhhhhhhhhhhhrw.',
    'hhhdddhhhhhhhhh.',
    'hhhhhhhhhhhhhhhh',
    'ahdddddhhhhhhhhh',
    'hhhhhhhhhhhhhhhh',
    '.bbbbbbbbbbbbbb.',
    '................',
    '................',
]
ICON_COLORS = {'h': (92, 100, 112), 'd': (38, 43, 51), 'w': (38, 52, 58), 'r': (150, 118, 76),
               'b': (70, 74, 80), 'a': (120, 170, 255)}


def icon():
    rng = random.Random(5)
    image = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(ICON):
        for x, key in enumerate(row):
            if key != '.':
                # The top of the hull catches the light
                lift = 14 if key == 'h' and (y == 0 or ICON[y - 1][x] == '.') else 0
                d = rng.randint(-4, 4) + lift
                image.putpixel((x, y), tuple(max(0, min(255, c + d)) for c in ICON_COLORS[key]) + (255,))
    return image


def number(value):
    text = f'{value:.4f}'.rstrip('0').rstrip('.')
    return '0' if text == '-0' else text


def numbers(values):
    return '[' + ', '.join(number(value) for value in values) + ']'


def model_json(all_parts, height):
    """The model's data file, as CubeModels reads it: each layer's parts, a cube to a line, so changes diff cleanly."""
    layers = []
    for layer in (MAIN, GLASS, GLOW):
        entries = []
        for part in all_parts:
            cubes = [c for c in part.cubes if c.layer == layer]
            if not cubes:
                continue
            fields = []
            if part.offset != (0, 0, 0):
                fields.append(f'"offset": {numbers(part.offset)}')
            if part.rotation != (0, 0, 0):
                fields.append(f'"rotation": {numbers(math.degrees(r) for r in part.rotation)}')
            rows = [f'          {{"uv": [{cube.u}, {cube.v}], "from": {numbers(cube.origin)}, '
                    f'"size": {numbers(cube.size)}}}' for cube in cubes]
            fields.append('"cubes": [\n' + ',\n'.join(rows) + '\n        ]')
            entries.append(f'      "{part.name}": {{\n        ' + ',\n        '.join(fields) + '\n      }')
        layers.append(f'    "{layer}": {{\n' + ',\n'.join(entries) + '\n    }')
    return (f'{{\n  "texture_size": [{TEXTURE_WIDTH}, {height}],\n  "layers": {{\n' + ',\n'.join(layers)
            + '\n  }\n}\n')


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8', newline='\n')


def main():
    all_parts = parts()
    cubes = [cube for part in all_parts for cube in part.cubes]
    height = pack(cubes)
    TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    paint(cubes, height).save(TEXTURE)
    icon().save(ICON_TEXTURE)
    write(ASSETS / 'items/puddle_jumper.json',
          {'model': {'type': 'minecraft:model', 'model': 'sgcraft:item/puddle_jumper'}})
    write(ASSETS / 'models/item/puddle_jumper.json',
          {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'sgcraft:item/puddle_jumper'}})
    text = model_json(all_parts, height)
    json.loads(text)
    MODEL.parent.mkdir(parents=True, exist_ok=True)
    MODEL.write_text(text, encoding='utf-8', newline='\n')
    print(f'{len(cubes)} cubes on a {TEXTURE_WIDTH}x{height} texture')


if __name__ == '__main__':
    main()
