"""Adds every translation key the Java sources use with a fallback to en_us.json, with the fallback as its English
text, and names for the mod's items and blocks. Keys already in the file keep their text.

Run from the repository root: python tools/sync_lang.py
"""

import json
import re
from pathlib import Path

LANG = Path('core/src/main/resources/assets/sgcraft/lang/en_us.json')
SOURCES = [Path('core/src/main/java'), Path('neoforge/src/main/java'), Path('fabric/src/main/java')]
# A fallback can be split into literals joined with +, across lines
TEXT = r'"((?:[^"\\]|\\.)*)"((?:\s*\+\s*"(?:[^"\\]|\\.)*")*)'
CALL = re.compile(r'translatableWithFallback\(\s*"([^"]+)"\s*,\s*' + TEXT, re.S)
# Helpers that build their keys from a prefix: SGCommands' for commands, and the address settings' for their
# messages, AddressPlan's after a flag saying whether the message is an error
PREFIXED = [
    (re.compile(r'SGCommands\.text\(\s*"([^"]+)"\s*,\s*' + TEXT, re.S), 'command.sgcraft.'),
    (re.compile(r'AddressChanges\.text\(\s*"([^"]+)"\s*,\s*' + TEXT, re.S), 'message.sgcraft.addresses.'),
    (re.compile(r'SettingsCatalog\.text\(\s*"([^"]+)"\s*,\s*' + TEXT, re.S), 'setting.sgcraft.'),
    (re.compile(r'AddressPlan\.problem\(\s*(?:true|false)\s*,\s*"([^"]+)"\s*,\s*' + TEXT, re.S),
     'message.sgcraft.addresses.'),
    # The obelisks' and the rings' messages, told to a player
    (re.compile(r'Obelisks\.tell\(\s*\w+\s*,\s*"([^"]+)"\s*,\s*' + TEXT, re.S), 'message.sgcraft.obelisk.'),
    (re.compile(r'Rings\.tell\(\s*\w+\s*,\s*"([^"]+)"\s*,\s*' + TEXT, re.S), 'message.sgcraft.rings.'),
]
PART = re.compile(r'"((?:[^"\\]|\\.)*)"')

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
    'item.sgcraft.puddle_jumper': 'Puddle Jumper',
    'entity.sgcraft.puddle_jumper': 'Puddle Jumper',
    'key.category.sgcraft.puddle_jumper': 'Puddle Jumper',
    'key.sgcraft.jumper_descend': 'Descend',
    'key.sgcraft.jumper_headlights': 'Headlights',
    'key.sgcraft.jumper_door': 'Cargo Door',
    'key.sgcraft.jumper_pods': 'Toggle Drive Pods',
    'key.sgcraft.jumper_computer': 'Open Jumper Computer',
    'key.sgcraft.jumper_arm': 'Arm Drones',
    'key.sgcraft.jumper_target': 'Lock Next Target',
    'key.sgcraft.jumper_fire': 'Launch Drone',
    'key.sgcraft.jumper_friendly_fire': 'Toggle Friendly Fire',
    'key.sgcraft.jumper_power': 'Jumper Power',
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
        for key, first, rest in CALL.findall(source):
            add(key, first + ''.join(PART.findall(rest)))
        for pattern, prefix in PREFIXED:
            for key, first, rest in pattern.findall(source):
                add(prefix + key, first + ''.join(PART.findall(rest)))
for key, text in NAMES.items():
    add(key, text)
LANG.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + '\n', encoding='utf-8', newline='\n')
print('added', added)
