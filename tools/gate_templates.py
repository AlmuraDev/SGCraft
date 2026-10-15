"""Writes SGCraft's gate structure templates: a chamber for the desert pyramid's hall, a platform for the jungle
temple's roof, platforms beside swamp huts and igloos, village plazas, a nether fortress chamber and an ocean
shrine for the monument's top. Each holds a gate spawner, which builds its gate once the world runs, and a
DHD with some naquadah, as the original SGCraft's generated gates had.

Run from the repository root: python tools/gate_templates.py core/src/main/resources/data/sgcraft/structure/stargate
"""
import gzip
import io
import random
import struct
import sys
from pathlib import Path

OUT = Path(sys.argv[1])
DATA_VERSION = 5023


class Byte(int):
    pass


def w_string(out, s):
    b = s.encode("utf-8")
    out.write(struct.pack(">H", len(b)))
    out.write(b)


def tag_type(v):
    if isinstance(v, Byte):
        return 1
    if isinstance(v, bool):
        return 1
    if isinstance(v, int):
        return 3
    if isinstance(v, str):
        return 8
    if isinstance(v, list):
        return 9
    if isinstance(v, dict):
        return 10
    raise TypeError(v)


def w_payload(out, v):
    t = tag_type(v)
    if t == 1:
        out.write(struct.pack(">b", int(v)))
    elif t == 3:
        out.write(struct.pack(">i", v))
    elif t == 8:
        w_string(out, v)
    elif t == 9:
        et = tag_type(v[0]) if v else 0
        out.write(struct.pack(">bi", et, len(v)))
        for e in v:
            w_payload(out, e)
    elif t == 10:
        for k, e in v.items():
            out.write(struct.pack(">b", tag_type(e)))
            w_string(out, k)
            w_payload(out, e)
        out.write(b"\x00")


def write_nbt(path, root):
    out = io.BytesIO()
    out.write(b"\x0a")
    w_string(out, "")
    w_payload(out, root)
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "wb") as f:
        f.write(gzip.compress(out.getvalue(), mtime=0))


def state(name, **props):
    return (name, tuple(sorted(props.items())))


AIR = state("minecraft:air")
WATER = state("minecraft:water", level="0")


class Template:
    def __init__(self, sx, sy, sz):
        self.size = (sx, sy, sz)
        self.cells = {}

    def set(self, x, y, z, st, nbt=None):
        assert 0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2], (x, y, z)
        self.cells[(x, y, z)] = (st, nbt)

    def fill(self, x0, y0, z0, x1, y1, z1, st):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    self.set(x, y, z, st)

    def save(self, name):
        palette, index, blocks = [], {}, []
        for (x, y, z), (st, nbt) in sorted(self.cells.items(), key=lambda c: (c[0][1], c[0][2], c[0][0])):
            if st not in index:
                index[st] = len(palette)
                entry = {"id": st[0]}
                if st[1]:
                    entry["properties"] = {k: v for k, v in st[1]}
                palette.append(entry)
            block = {"pos": [x, y, z], "state": index[st]}
            if nbt is not None:
                block["nbt"] = nbt
            blocks.append(block)
        write_nbt(OUT / (name + ".nbt"), {
            "DataVersion": DATA_VERSION,
            "size": list(self.size),
            "palette": palette,
            "blocks": blocks,
            "entities": [],
        })


def stairs(name, facing, waterlogged=False):
    return state(name, facing=facing, half="bottom", shape="straight", waterlogged="true" if waterlogged else "false")


def spawner_nbt(unique_within=0, village=False):
    return {
        "id": "sgcraft:gate_spawner",
        "generated": Byte(1),
        "chevrons": 0,
        "upgrade_chance": 25,
        "unique_within": unique_within,
        "village": Byte(1 if village else 0),
    }


def dhd_nbt(dhd):
    return {"id": dhd, "Items": [{"Slot": Byte(0), "id": "sgcraft:naquadah", "count": 3}]}


def spawner(gate, waterlogged=False, facing="north"):
    return state("sgcraft:" + gate + "_stargate_spawner", facing=facing,
                 waterlogged="true" if waterlogged else "false")


def dhd(gate, facing="north", waterlogged=False):
    return state("sgcraft:dhd" if gate == "milky_way" else "sgcraft:dhd_" + gate, facing=facing,
                 waterlogged="true" if waterlogged else "false")


def gate_and_dhd(t, gx, gy, gz, dx, dz, gate, unique_within=0, village=False, waterlogged=False, dy=None,
                 facing="north"):
    # The base faces out of the gate's front, towards the DHD; the ring rises from it when the gate is built. No
    # camouflage covers the ring's bottom row, so the ring stands in the slit between the steps and turns freely
    t.set(gx, gy, gz, spawner(gate, waterlogged, facing), spawner_nbt(unique_within, village))
    t.set(dx, gy if dy is None else dy, dz, dhd(gate, facing, waterlogged), dhd_nbt(dhd(gate)[0]))


# A step around the foot of a gate whose base is at gx, gy, gz and faces north or south: a row of stairs on each side
# of its bottom row, rising to it, and one at each end, so the ring stands in a slit with room to turn
def step(t, gx, gy, gz, stair, facing="north", waterlogged=False, ends=True):
    front, back = (gz - 1, gz + 1) if facing == "north" else (gz + 1, gz - 1)
    for x in range(gx - 3, gx + 4):
        t.set(x, gy, front, stairs(stair, "south" if front < gz else "north", waterlogged))
    for x in range(gx - 2 if not ends else gx - 3, gx + 3 if not ends else gx + 4):
        t.set(x, gy, back, stairs(stair, "south" if back < gz else "north", waterlogged))
    t.set(gx - 3, gy, gz, stairs(stair, "east", waterlogged))
    t.set(gx + 3, gy, gz, stairs(stair, "west", waterlogged))


# A platform as the original's: a 9 by 9 floor stepped at its edges, with a step around the gate's foot and the DHD
# in front of it, off to one side. With a jigsaw at its front edge it joins a village's streets.
def platform(name, seed, top, high, low, stair, gate="milky_way", village=False):
    rng = random.Random(seed)
    t = Template(9, 8, 9)
    t.fill(0, 1, 0, 8, 7, 8, AIR)
    for x in range(9):
        for z in range(9):
            edge_x = x in (0, 8)
            edge_z = z in (0, 8)
            if edge_x and edge_z:
                st = state(low)
            elif x == 0:
                st = stairs(stair, "east")
            elif x == 8:
                st = stairs(stair, "west")
            elif z == 0:
                st = stairs(stair, "south")
            elif z == 8:
                st = stairs(stair, "north")
            else:
                r = rng.randrange(40)
                st = state(high if r < 15 else low if r < 19 else top)
            t.set(x, 0, z, st)
    step(t, 4, 1, 6, stair)
    gate_and_dhd(t, 4, 1, 6, 6, 2, gate, unique_within=80 if village else 0, village=village)
    if village:
        t.set(4, 0, 0, state("minecraft:jigsaw", orientation="north_up"), {
            "id": "minecraft:jigsaw",
            "name": "minecraft:building_entrance",
            "target": "minecraft:building_entrance",
            "pool": "minecraft:empty",
            "joint": "aligned",
            "final_state": stair + "[facing=south,half=bottom,shape=straight,waterlogged=false]",
            "selection_priority": 0,
            "placement_priority": 0,
        })
    t.save(name)


platform("jungle_roof", 2, "minecraft:cobblestone", "minecraft:mossy_cobblestone",
         "minecraft:mossy_cobblestone", "minecraft:cobblestone_stairs")
platform("swamp_platform", 3, "minecraft:stone_bricks", "minecraft:mossy_stone_bricks",
         "minecraft:cracked_stone_bricks", "minecraft:stone_brick_stairs")
# Igloos have Pegasus gates, as in the original
platform("snowy_platform", 4, "minecraft:stone_bricks", "minecraft:cracked_stone_bricks", "minecraft:gravel",
         "minecraft:stone_brick_stairs", gate="pegasus")

platform("village_plains", 5, "minecraft:stone_bricks", "minecraft:mossy_stone_bricks",
         "minecraft:cracked_stone_bricks", "minecraft:stone_brick_stairs", village=True)
platform("village_desert", 6, "minecraft:smooth_sandstone", "minecraft:smooth_sandstone",
         "minecraft:chiseled_sandstone", "minecraft:smooth_sandstone_stairs", village=True)
platform("village_savanna", 7, "minecraft:cobblestone", "minecraft:stone", "minecraft:coarse_dirt",
         "minecraft:cobblestone_stairs", village=True)
platform("village_snowy", 8, "minecraft:stone_bricks", "minecraft:stone_bricks", "minecraft:stone_bricks",
         "minecraft:stone_brick_stairs", village=True)
platform("village_taiga", 9, "minecraft:stone_bricks", "minecraft:cracked_stone_bricks", "minecraft:coarse_dirt",
         "minecraft:stone_brick_stairs", village=True)


# The original's nether fortress chamber: a room of nether bricks with a doorway in each wall
def nether_chamber():
    bricks = state("minecraft:nether_bricks")
    t = Template(11, 7, 11)
    t.fill(0, 0, 0, 10, 6, 10, bricks)
    t.fill(1, 1, 1, 9, 5, 9, AIR)
    for y in range(1, 4):
        for i in range(4, 7):
            t.set(0, y, i, AIR)
            t.set(10, y, i, AIR)
            t.set(i, y, 0, AIR)
            t.set(i, y, 10, AIR)
    step(t, 5, 1, 8, "minecraft:nether_brick_stairs")
    gate_and_dhd(t, 5, 1, 8, 5, 2, "milky_way")
    t.save("nether_chamber")


# The original's ocean monument shrine: a prismarine floor, a pillar at each corner and a roof lit by sea lanterns,
# full of water, with the gate standing in it
def ocean_shrine():
    bricks = state("minecraft:prismarine_bricks")
    dark = state("minecraft:dark_prismarine")
    lantern = state("minecraft:sea_lantern")
    t = Template(9, 6, 9)
    t.fill(0, 1, 0, 8, 5, 8, WATER)
    for x in range(9):
        for z in range(9):
            t.set(x, 0, z, dark if x in (0, 8) or z in (0, 8) else bricks)
    for x, z in ((0, 0), (8, 0), (0, 8), (8, 8)):
        for y in range(1, 5):
            t.set(x, y, z, bricks)
    for i in range(9):
        for x, z in ((i, 0), (i, 8), (0, i), (8, i)):
            t.set(x, 5, z, bricks)
    # The back two sit behind the gate, clear of its ring, whose top corners would take their place dry
    for x, z in ((2, 2), (6, 2), (2, 7), (6, 7)):
        t.set(x, 5, z, lantern)
    step(t, 4, 1, 6, "minecraft:prismarine_brick_stairs", waterlogged=True)
    gate_and_dhd(t, 4, 1, 6, 6, 2, "milky_way", waterlogged=True)
    t.save("ocean_shrine")


# The original's chamber in the desert pyramid's hall: a sandstone floor, a pillar at each corner and room for the
# gate, which stands on the floor with a step round its foot so its inner ring has room to turn. The gate faces the
# pyramid's entrance, to the template's south, with the DHD between them. Only the chamber's own blocks are set, so
# the hall keeps the rest of its walls
def pyramid_hall():
    smooth = state("minecraft:smooth_sandstone")
    chiseled = state("minecraft:chiseled_sandstone")
    t = Template(7, 6, 7)
    t.fill(0, 0, 0, 6, 0, 6, smooth)
    t.fill(1, 1, 1, 5, 5, 5, AIR)
    for x, z in ((0, 0), (6, 0), (0, 6), (6, 6)):
        t.set(x, 1, z, chiseled)
        t.set(x, 2, z, smooth)
        t.set(x, 3, z, chiseled)
    stair = "minecraft:smooth_sandstone_stairs"
    # The back row stops short of the corner pillars
    step(t, 3, 1, 1, stair, facing="south", ends=False)
    gate_and_dhd(t, 3, 1, 1, 3, 5, "milky_way", facing="south")
    t.save("pyramid_hall")


pyramid_hall()
nether_chamber()
ocean_shrine()
print("written to", OUT)
