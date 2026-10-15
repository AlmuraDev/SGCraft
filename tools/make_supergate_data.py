"""Writes the data and assets for the supergate's blocks: the textures, copied from the original SGCraft, the block
states and models, the block items, the loot tables, the original's recipes, and the pickaxe tag.

Run from the repository root: python tools/make_supergate_data.py <path to the original SGCraft's src/resources>
"""

import json
import shutil
import sys
from pathlib import Path

ORIGINAL = Path(sys.argv[1]) / 'assets/sgcraft/textures/blocks'
ASSETS = Path('core/src/main/resources/assets/sgcraft')
DATA = Path('core/src/main/resources/data')


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + '\n', encoding='utf-8')


for texture in ['supergateblock', 'supergatering', 'supergatechevron', 'supergatebase_front']:
    shutil.copyfile(ORIGINAL / f'{texture}.png', ASSETS / f'textures/block/{texture}.png')

# The ring and chevron blocks look the same merged or not; a merged one is drawn by its gate instead
for name, side in [('supergate_ring', 'supergatering'), ('supergate_chevron', 'supergatechevron')]:
    write(ASSETS / f'blockstates/{name}.json', {'variants': {
        f'merged={merged}': {'model': f'sgcraft:block/{name}'} for merged in ['false', 'true']}})
    write(ASSETS / f'models/block/{name}.json', {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
        'top': 'sgcraft:block/supergateblock', 'bottom': 'sgcraft:block/supergateblock',
        'side': f'sgcraft:block/{side}'}})

# The base faces the way it was placed, as a stargate's does
ROTATION = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
variants = {}
for facing, y in ROTATION.items():
    for formed in ['false', 'true']:
        variant = {'model': 'sgcraft:block/supergate_base'}
        if y:
            variant['y'] = y
        variants[f'facing={facing},formed={formed}'] = variant
write(ASSETS / 'blockstates/supergate_base.json', {'variants': variants})
write(ASSETS / 'models/block/supergate_base.json', {'parent': 'minecraft:block/orientable_with_bottom', 'textures': {
    'top': 'sgcraft:block/supergateblock', 'bottom': 'sgcraft:block/supergateblock',
    'side': 'sgcraft:block/supergatering', 'front': 'sgcraft:block/supergatebase_front'}})

for name in ['supergate_ring', 'supergate_chevron', 'supergate_base']:
    write(ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'sgcraft:block/{name}'}})
    write(DATA / f'sgcraft/loot_table/blocks/{name}.json', {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'sgcraft:{name}'}],
                   'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
        'random_sequence': f'sgcraft:blocks/{name}'})


def shaped(name, pattern, key, result, count=1):
    out = {'id': result}
    if count > 1:
        out['count'] = count
    write(DATA / f'sgcraft/recipe/{name}.json', {'type': 'minecraft:crafting_shaped', 'category': 'building',
                                                 'key': key, 'pattern': pattern, 'result': out})


# The original's recipes
INGOT = 'sgcraft:naquadah_ingot'
shaped('supergate_ring', ['RNR', 'NBN', 'RNR'],
       {'R': 'sgcraft:stargate_ring', 'N': INGOT, 'B': 'sgcraft:naquadah_block'}, 'sgcraft:supergate_ring', 4)
shaped('supergate_chevron', ['S', 'C'], {'S': 'sgcraft:supergate_ring', 'C': 'sgcraft:stargate_chevron'},
       'sgcraft:supergate_chevron')
shaped('supergate_base', ['SeS', 'NbN', 'SnS'],
       {'S': 'sgcraft:supergate_ring', 'N': INGOT, 'e': 'minecraft:nether_star', 'b': 'sgcraft:stargate_base',
        'n': 'sgcraft:naquadah_block'}, 'sgcraft:supergate_base')

# Mined with a pickaxe, as the stargate's blocks are
tag = DATA / 'minecraft/tags/block/mineable/pickaxe.json'
values = json.loads(tag.read_text(encoding='utf-8'))
for name in ['supergate_ring', 'supergate_chevron', 'supergate_base']:
    if f'sgcraft:{name}' not in values['values']:
        values['values'].append(f'sgcraft:{name}')
write(tag, values)
print('done')
