"""Writes the data and assets for SGCraft's items and naquadah: item and block models, loot tables, recipes after the
original SGCraft's, the naquadah ore's world generation, block tags, and the textures, copied from the original.

Run from the repository root: python tools/make_items_data.py <path to the original SGCraft's src/resources>
"""

import json
import shutil
import sys
from pathlib import Path

ORIGINAL = Path(sys.argv[1]) / 'assets/sgcraft/textures'
ASSETS = Path('common/src/main/resources/assets/sgcraft')
DATA = Path('common/src/main/resources/data')


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + '\n', encoding='utf-8')


# Items drawn flat, and the original texture each is drawn with
FLAT = {
    'naquadah': 'items/naquadah.png',
    'naquadah_ingot': 'items/naquadahingot.png',
    'core_crystal': 'items/sgcorecrystal.png',
    'controller_crystal': 'items/sgcontrollercrystal.png',
    'iris_blade': 'items/sgirisblade.png',
    'iris_upgrade': 'items/sgirisupgrade.png',
    'pdd': 'items/pdd.png',
    'gdo': 'items/gdo.png',
    'configurator': 'items/configurator.png',
}
for name, texture in FLAT.items():
    (ASSETS / 'textures/item').mkdir(parents=True, exist_ok=True)
    shutil.copyfile(ORIGINAL / texture, ASSETS / f'textures/item/{name}.png')
    write(ASSETS / f'models/item/{name}.json',
          {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'sgcraft:item/{name}'}})
    write(ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'sgcraft:item/{name}'}})

# Plain cube blocks
CUBES = {'naquadah_ore': 'blocks/naquadahore.png', 'naquadah_block': 'blocks/naquadahblock.png'}
for name, texture in CUBES.items():
    shutil.copyfile(ORIGINAL / texture, ASSETS / f'textures/block/{name}.png')
    write(ASSETS / f'blockstates/{name}.json', {'variants': {'': {'model': f'sgcraft:block/{name}'}}})
    write(ASSETS / f'models/block/{name}.json',
          {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'sgcraft:block/{name}'}})
    write(ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'sgcraft:block/{name}'}})

# Loot: the block drops itself; the ore drops naquadah, more with fortune, or itself with silk touch
write(DATA / 'sgcraft/loot_table/blocks/naquadah_block.json', {
    'type': 'minecraft:block',
    'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'sgcraft:naquadah_block'}],
               'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
    'random_sequence': 'sgcraft:blocks/naquadah_block'})
write(DATA / 'sgcraft/loot_table/blocks/naquadah_ore.json', {
    'type': 'minecraft:block',
    'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:alternatives', 'children': [
        {'type': 'minecraft:item', 'condition': 'minecraft:tool/can_silk_touch', 'name': 'sgcraft:naquadah_ore'},
        {'type': 'minecraft:item', 'name': 'sgcraft:naquadah', 'modifier': [
            {'type': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 2}},
            {'type': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:ore_drops'},
            {'type': 'minecraft:explosion_decay'}]}]}]}],
    'random_sequence': 'sgcraft:blocks/naquadah_ore'})


def shaped(name, pattern, key, result, count=1, category='misc'):
    out = {'id': result}
    if count > 1:
        out['count'] = count
    write(DATA / f'sgcraft/recipe/{name}.json',
          {'type': 'minecraft:crafting_shaped', 'category': category, 'key': key, 'pattern': pattern, 'result': out})


def shapeless(name, ingredients, result, count=1):
    out = {'id': result}
    if count > 1:
        out['count'] = count
    write(DATA / f'sgcraft/recipe/{name}.json',
          {'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': ingredients, 'result': out})


# The original's recipes; its chiselled and smooth sandstone, dyes and charcoal by their modern names
INGOT = 'sgcraft:naquadah_ingot'
shapeless('naquadah_ingot', ['minecraft:iron_ingot', 'sgcraft:naquadah'], INGOT)
shapeless('naquadah_ingot_from_block', ['sgcraft:naquadah_block'], INGOT, 9)
shaped('naquadah_block', ['NNN', 'NNN', 'NNN'], {'N': INGOT}, 'sgcraft:naquadah_block', category='building')
shaped('stargate_ring', ['CCC', 'NNN', 'SSS'],
       {'C': 'minecraft:chiseled_sandstone', 'N': INGOT, 'S': 'minecraft:smooth_sandstone'},
       'sgcraft:stargate_ring', category='building')
shaped('stargate_chevron', ['CgC', 'NpN', 'SrS'],
       {'C': 'minecraft:chiseled_sandstone', 'N': INGOT, 'S': 'minecraft:smooth_sandstone',
        'g': 'minecraft:glowstone_dust', 'r': 'minecraft:redstone', 'p': 'minecraft:ender_pearl'},
       'sgcraft:stargate_chevron', category='building')
shaped('stargate_base', ['CrC', 'NeN', 'ScS'],
       {'C': 'minecraft:chiseled_sandstone', 'N': INGOT, 'S': 'minecraft:smooth_sandstone',
        'r': 'minecraft:redstone', 'e': 'minecraft:ender_eye', 'c': 'sgcraft:core_crystal'},
       'sgcraft:stargate_base', category='building')
# The Pegasus and Universe bases are a Milky Way base with the original's upgrade, folded into one recipe
shaped('stargate_base_pegasus', ['glg', 'rbr', 'glg'],
       {'g': 'minecraft:glowstone_dust', 'l': 'minecraft:lapis_block', 'r': 'minecraft:redstone',
        'b': 'sgcraft:stargate_base'}, 'sgcraft:stargate_base_pegasus', category='building')
shaped('stargate_base_universe', ['ede', 'cbc', 'ede'],
       {'e': 'minecraft:ender_pearl', 'd': 'minecraft:diamond', 'c': 'sgcraft:stargate_chevron',
        'b': 'sgcraft:stargate_base'}, 'sgcraft:stargate_base_universe', category='building')
shaped('dhd', ['bbb', 'OpO', 'OcO'],
       {'b': 'minecraft:stone_button', 'O': 'minecraft:obsidian', 'p': 'minecraft:ender_pearl',
        'c': 'sgcraft:controller_crystal'}, 'sgcraft:dhd')
shaped('dhd_pegasus', ['glg', 'rdr', 'glg'],
       {'g': 'minecraft:glowstone_dust', 'l': 'minecraft:lapis_block', 'r': 'minecraft:redstone',
        'd': 'sgcraft:dhd'}, 'sgcraft:dhd_pegasus')
shaped('core_crystal', ['bbr', 'rdb', 'brb'],
       {'b': 'minecraft:blue_dye', 'r': 'minecraft:redstone', 'd': 'minecraft:diamond'}, 'sgcraft:core_crystal')
shaped('controller_crystal', ['roo', 'odr', 'oor'],
       {'o': 'minecraft:orange_dye', 'r': 'minecraft:redstone', 'd': 'minecraft:diamond'},
       'sgcraft:controller_crystal')
shaped('iris_blade', [' ii', 'ic ', 'i  '], {'i': 'minecraft:iron_ingot', 'c': 'minecraft:charcoal'},
       'sgcraft:iris_blade')
shaped('iris_upgrade', ['bbb', 'brb', 'bbb'], {'b': 'sgcraft:iris_blade', 'r': 'minecraft:redstone'},
       'sgcraft:iris_upgrade')
shaped('pdd', ['rcr', 'nCn', 'xbz'],
       {'r': 'minecraft:redstone', 'c': 'minecraft:compass', 'n': 'sgcraft:naquadah', 'C': 'minecraft:clock',
        'x': 'sgcraft:controller_crystal', 'b': 'minecraft:writable_book', 'z': 'sgcraft:core_crystal'},
       'sgcraft:pdd', category='equipment')
shaped('gdo', ['rCr', 'xLz', 'nbn'],
       {'r': 'minecraft:redstone', 'C': 'minecraft:clock', 'x': 'sgcraft:controller_crystal',
        'L': 'minecraft:lever', 'z': 'sgcraft:core_crystal', 'n': 'sgcraft:naquadah',
        'b': 'minecraft:writable_book'}, 'sgcraft:gdo', category='equipment')
# The configurator had no recipe of its own; this one is new
shaped('configurator', ['rcr', 'xNz', 'rbr'],
       {'r': 'minecraft:redstone', 'c': 'minecraft:comparator', 'x': 'sgcraft:controller_crystal',
        'N': INGOT, 'z': 'sgcraft:core_crystal', 'b': 'minecraft:writable_book'},
       'sgcraft:configurator', category='equipment')

# The ore: small veins deep down, as many as diamonds, in stone and deepslate
ORE_STATE = 'sgcraft:naquadah_ore'
write(DATA / 'sgcraft/worldgen/feature/naquadah_ore.json', {
    'type': 'minecraft:ore', 'size': 4, 'discard_chance_on_air_exposure': 0.5,
    'targets': [
        {'target': {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:stone_ore_replaceables'},
         'state': ORE_STATE},
        {'target': {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:deepslate_ore_replaceables'},
         'state': ORE_STATE}]})
write(DATA / 'sgcraft/worldgen/placed_feature/naquadah_ore.json', {
    'feature': 'sgcraft:naquadah_ore',
    'placement': [
        {'type': 'minecraft:count', 'count': 6},
        {'type': 'minecraft:in_square'},
        {'type': 'minecraft:height_range', 'height': {
            'type': 'minecraft:trapezoid', 'min_inclusive': {'above_bottom': -40}, 'max_inclusive': {'absolute': 40}}},
        {'type': 'minecraft:biome'}]})
# NeoForge adds it to the overworld's biomes from this; Fabric does so in code
write(DATA / 'sgcraft/neoforge/biome_modifier/naquadah_ore.json', {
    'type': 'neoforge:add_features', 'biomes': '#minecraft:is_overworld', 'features': 'sgcraft:naquadah_ore',
    'step': 'underground_ores'})

pickaxe = DATA / 'minecraft/tags/block/mineable/pickaxe.json'
tag = json.loads(pickaxe.read_text(encoding='utf-8'))
for block in ['sgcraft:naquadah_ore', 'sgcraft:naquadah_block']:
    if block not in tag['values']:
        tag['values'].append(block)
write(pickaxe, tag)
write(DATA / 'minecraft/tags/block/needs_iron_tool.json',
      {'replace': False, 'values': ['sgcraft:naquadah_ore', 'sgcraft:naquadah_block']})
print('written')
