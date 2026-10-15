"""Adds every translation key the Java sources use with a fallback to en_us.json, with the fallback as its English
text, and names for the mod's items and blocks. Keys already in the file keep their text.

Run from the repository root: python tools/sync_lang.py
"""

import json
import re
from pathlib import Path

LANG = Path('common/src/main/resources/assets/sgcraft/lang/en_us.json')
SOURCES = [Path('common/src/main/java'), Path('neoforge/src/main/java'), Path('fabric/src/main/java')]
CALL = re.compile(r'translatableWithFallback\(\s*"([^"]+)"\s*,\s*"((?:[^"\\]|\\.)*)"', re.S)
# SGCommands builds its keys from a prefix
COMMAND = re.compile(r'SGCommands\.text\(\s*"([^"]+)"\s*,\s*"((?:[^"\\]|\\.)*)"', re.S)

NAMES = {
    'item.sgcraft.naquadah': 'Naquadah',
    'item.sgcraft.naquadah_ingot': 'Naquadah Alloy Ingot',
    'item.sgcraft.core_crystal': 'Stargate Core Crystal',
    'item.sgcraft.controller_crystal': 'Stargate Controller Crystal',
    'item.sgcraft.iris_blade': 'Iris Blade',
    'item.sgcraft.iris_upgrade': 'Stargate Iris Upgrade',
    'item.sgcraft.pdd': 'Personal Dialing Device',
    'item.sgcraft.gate_control_tablet': 'Gate Control Tablet',
    'item.sgcraft.gdo': 'Garage Door Opener',
    'item.sgcraft.configurator': 'Stargate Configurator',
    'block.sgcraft.naquadah_ore': 'Naquadah Ore',
    'block.sgcraft.naquadah_block': 'Block of Naquadah Alloy',
    'iris_type.sgcraft.metal': 'Metal Iris',
    'death.attack.sgcraft.iris': '%1$s got splattered against a stargate iris',
    'death.attack.sgcraft.iris.player': '%1$s got splattered against a stargate iris whilst fighting %2$s',
}

lang = json.loads(LANG.read_text(encoding='utf-8'))
added = 0


def add(key, text):
    global added
    if key not in lang:
        lang[key] = text.encode().decode('unicode_escape') if '\\' in text else text
        added += 1


for root in SOURCES:
    for path in root.rglob('*.java'):
        source = path.read_text(encoding='utf-8')
        for key, text in CALL.findall(source):
            add(key, text)
        for key, text in COMMAND.findall(source):
            add('command.sgcraft.' + key, text)
for key, text in NAMES.items():
    add(key, text)
LANG.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
print('added', added)
