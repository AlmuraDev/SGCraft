"""Writes the assets for the gate spawners, the creative only blocks that build a finished gate: their textures,
copied from the original SGCraft, their block states, models and items, and their names.

Run from the repository root: python tools/make_spawner_data.py <path to the original SGCraft's src/resources>
"""

import json
import shutil
import sys
from pathlib import Path

ORIGINAL = Path(sys.argv[1]) / 'assets/sgcraft/textures/blocks'
ASSETS = Path('common/src/main/resources/assets/sgcraft')


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + '\n', encoding='utf-8')


TYPES = {'milky_way': ('milkyway', 'Milky Way'), 'pegasus': ('pegasus', 'Pegasus'), 'universe': ('universe', 'Universe')}
names = {}
for kind, kind_name in [('stargate', 'Stargate'), ('supergate', 'Supergate')]:
    for type_id, (original, type_name) in TYPES.items():
        name = f'{type_id}_{kind}_spawner'
        shutil.copyfile(ORIGINAL / f'gatespawner_{original}_{kind}.png', ASSETS / f'textures/block/{name}.png')
        write(ASSETS / f'blockstates/{name}.json', {'variants': {'': {'model': f'sgcraft:block/{name}'}}})
        write(ASSETS / f'models/block/{name}.json',
              {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'sgcraft:block/{name}'}})
        write(ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'sgcraft:block/{name}'}})
        names[f'block.sgcraft.{name}'] = f'{type_name} {kind_name} Spawner'

lang = ASSETS / 'lang/en_us.json'
values = json.loads(lang.read_text(encoding='utf-8'))
values.update({key: value for key, value in names.items() if key not in values})
lang.write_text(json.dumps(values, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
print('done')
