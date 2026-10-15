"""Writes the empty structure SGCraft's game tests run in: a box of air wide enough for two gates in different address
cells, with their DHDs.

Run from the repository root: python tools/make_test_structure.py
"""

import gzip
import struct
from pathlib import Path

OUT = Path('neoforge/src/main/resources/data/sgcraft/structure/gate_test.nbt')
SIZE = (48, 10, 16)


def name(text):
    data = text.encode('utf-8')
    return struct.pack('>H', len(data)) + data


def tag(kind, key, payload):
    return bytes([kind]) + name(key) + payload


def compound(*entries):
    return b''.join(entries) + b'\x00'


def int_list(values):
    return bytes([3]) + struct.pack('>i', len(values)) + b''.join(struct.pack('>i', v) for v in values)


def empty_list():
    return bytes([0]) + struct.pack('>i', 0)


palette = bytes([10]) + struct.pack('>i', 1) + compound(tag(8, 'Name', name('minecraft:air')))
root = tag(10, '', compound(
    tag(9, 'size', int_list(SIZE)),
    tag(9, 'palette', palette),
    tag(9, 'blocks', empty_list()),
    tag(9, 'entities', empty_list()),
))
OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_bytes(gzip.compress(root))
print('written', OUT)
