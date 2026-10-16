# World generation

The world makes naquadah ore deep underground and builds gates at some of its structures and villages, each with a DHD and often a Tok'ra keeping watch. Some generated gates have a chest beside them holding a ZPM. SGCraft also adds naquadah, crystals and ingots to some vanilla chests.

## Naquadah ore

```infobox
Naquadah Ore
Icon: sgcraft:naquadah_ore
Type: Block
Found in: The Overworld
Mine with: Iron pickaxe or better
Drops: 1 to 2 naquadah
Experience: 2 to 5
Dictionary: [Naquadah Ore](blocks/naquadah-ore.md)
```

Naquadah ore generates in the Overworld by default, in two ways. With the default settings, every chunk tries both.

- Under lava, the chunk picks from 1 to 16 random columns (`max_nodes_under_lava`) and searches each from the bottom of the world up to sea level. Wherever lava rests on a solid block, that block starts a cluster of ore up to 3 blocks across and 1 block high.
- As isolated nodes, the chunk gets from 1 to 4 nodes (`max_isolated_nodes`), each up to 2 by 2 by 2 blocks. A node starts 16 to 31 blocks above the bottom of the world, which is y -48 to -33 in the Overworld.

Ore replaces solid blocks that world generation may replace, so it never replaces bedrock. A cluster or node stops at the edge of its chunk.

### Mining

| Property | Value |
| --- | --- |
| Hardness | 3 |
| Tool | Iron pickaxe or better |
| Drops | 1 to 2 naquadah, more with Fortune |
| Silk Touch | Drops the ore block |
| Experience | 2 to 5 |

Naquadah fuels [DHDs](dhd.md) and battery chargers, charges batteries in the crafting grid, and goes into naquadah alloy ingots and several recipes. See [Naquadah](items/naquadah.md).

### Adding ore to old chunks

Chunks made before SGCraft was added have no naquadah ore. With `add_ores_to_existing_worlds` on (it is off by default), each such chunk gets its ore when it loads, up to 32 chunks a tick. The world marks every chunk that has had its ore, whether new or old, so no chunk gets ore twice.

### Server admins

The `worldgen.naquadah_ore` object in `config/sgcraft-server.json`:

| Setting | Default | What it does |
| --- | --- | --- |
| `enable_naquadah_ore` | true | Whether naquadah ore generates at all |
| `gen_under_lava_odds` | 1 | One chunk in this many gets clusters under lava. 0 for none |
| `max_nodes_under_lava` | 16 | The most columns of a chunk searched for lava |
| `gen_isolated_odds` | 1 | One chunk in this many gets isolated nodes. 0 for none |
| `max_isolated_nodes` | 4 | The most isolated nodes a chunk gets |
| `dimension_ids` | `["minecraft:overworld"]` | The worlds naquadah ore generates in |
| `add_ores_to_existing_worlds` | false | Whether chunks made without ore get it when they load |

`/sgcraft ore` lists these settings. `/sgcraft ore <setting> <value>` changes one, and `/sgcraft ore dimension_ids <add|remove> <world>` changes the worlds. Changes apply to chunks generated or loaded from then on and are written to the settings file. `/sgcraft ore chunk [<x z>]` tells whether a chunk has had its ore. Add `generate` to give that chunk ore now, or `unmark` to have it get ore again the next time it loads while `add_ores_to_existing_worlds` is on.

A data pack can change the ore block and the blocks it replaces through the `sgcraft:naquadah_ore` feature.

## Structure gates

Some vanilla structures get a gate when they generate, built into the structure or on a platform nearby. Each structure has a 25% chance of a gate. A generated gate has 9 chevrons 25% of the time, otherwise 7. Every gate comes with a DHD holding 3 naquadah.

| Structure | Template | Where the gate stands | Gate | Tok'ra |
| --- | --- | --- | --- | --- |
| Desert pyramid | `pyramid_hall` | In a chamber in the middle of the pyramid's hall | Milky Way | Yes |
| Jungle temple | `jungle_roof` | On the temple's roof | Milky Way | Yes |
| Ocean monument | `ocean_shrine` | In a shrine in place of the monument's top, under the sea | Milky Way | No |
| Nether fortress | `nether_chamber` | In a chamber on the fortress's first crossing, with a doorway onto each bridge | Milky Way | No |
| Swamp hut | `swamp_platform` | On a platform 10 to 29 blocks away along each axis | Milky Way | Yes |
| Igloo | `snowy_platform` | On a platform 10 to 29 blocks away along each axis | Pegasus, with a Pegasus DHD | Yes |

A platform stands on a stone brick foundation down to the ground. One platform in ten is sunk 1 to 3 blocks, half buried. A platform that has no room clear of the structure after 8 tries isn't built.

## Village gates

Plains, desert, savanna, snowy and taiga villages can build a gate plaza among their houses, including zombie villages. The plaza is one more piece the village's pools of houses can choose, so the other houses come up as often as before.

A village builds one gate at most. A plaza within 80 blocks of a gate already built stays empty. A village gate is a Milky Way gate with a DHD, with 9 chevrons 25% of the time, and a Tok'ra stands in front of it.

## Generated gates

The world builds its gates from gate spawners in the templates, once the world runs. A gate built this way is generated:

- It has no owner, so operators manage it. See [Access](access.md).
- The configurator shows "Built by: World".
- Players can't break it unless its **Breakable** setting is on, or `gates.generated_gates_breakable` is on for gates without a setting of their own.

## ZPM chests

When the world builds a gate, in a structure or a village, there is a 15% chance (`zpm.chest_chance`) that a chest stands beside it holding one full {{item:sgcraft:zpm}}. The chest stands 2 blocks in front of the base and 4 to one side, clear of the ring, where there is room on firm ground. This is the only place ZPMs are found.

One chest ZPM in ten is tainted (`zpm.tainted_chance`). A tainted ZPM shines like an enchanted item, and a ZPM console, interface cart or hub holding one explodes when it is drawn on. It is safe in a supergate's base or a city shield generator. See [Tainted ZPMs](items/zpm.md#tainted-zpms).

### Server admins

- `zpm.chest_chance` (15) is the percent of generated gates with a ZPM chest, 0 to 100. 0 turns the chests off.
- `zpm.tainted_chance` (10) is the percent of chest ZPMs that are tainted, 0 to 100.
- `/sgcraft zpm chest_chance <value>` and `/sgcraft zpm tainted_chance <value>` change them. See [Settings](settings.md#zpms).

## Tok'ra villagers

The Tok'ra are villagers who keep watch over generated gates and trade the crystals a gate is built from. When a generated gate is built, one Tok'ra spawns in front of it, 2 to 5 blocks out and up to 2 blocks up, where there is ground and room to stand. It never despawns. Gates in ocean monuments and Nether fortresses have no Tok'ra.

A Tok'ra works at a DHD (Milky Way or Pegasus), which no other villager takes as a job site.

| Trade | Wants | Gives | Uses |
| --- | --- | --- | --- |
| Core crystal | 8 emeralds and 1 diamond | 1 Stargate Core Crystal | 12 |
| Controller crystal | 16 emeralds and 1 diamond | 1 Stargate Controller Crystal | 12 |

Each trade gives the Tok'ra 5 experience. A data pack can change the trades through the trade set `sgcraft:tokra/level_1`.

### Server admins

- `worldgen.tokra_worlds` turns the Tok'ra on or off for each world's generated gates, by the world's identifier. The default is `{"minecraft:overworld": true}`. A world left out has Tok'ra wherever its structures' settings say.
- `/sgcraft worldgen tokra` lists the worlds set off. `/sgcraft worldgen tokra <world> <true|false>` sets one.
- `/sgcraft tokra` puts a Tok'ra in front of your selected gate, or the one you look at.

## Chest loot

SGCraft's loot rolls alongside the vanilla loot in these chests:

| Chest | Chance | Rolls | Items |
| --- | --- | --- | --- |
| Dungeon | 20% | 1 to 2 | Naquadah (1 to 8), ender pearl, Stargate Core Crystal, Stargate Controller Crystal |
| Bonus chest | Always | 1 to 2 | Stargate Core Crystal (1 to 2), Stargate Controller Crystal (1 to 2) |
| | 50% | 1 | Naquadah Alloy Ingot (32 to 64) |
| | 75% | 1 | Naquadah (8 to 16) |
| | 50% | 1 | Ender pearls (6 to 24) |
| Stronghold library | 75% | 1 to 2 | Stargate Core Crystal, Stargate Controller Crystal |
| Village weaponsmith and toolsmith | 50% | 1 to 2 | Naquadah Alloy Ingot (1 to 8), Stargate Core Crystal, Stargate Controller Crystal |

In the dungeon, naquadah, pearls and core crystals each come up twice as often as controller crystals. In the stronghold library, controller crystals come up twice as often as core crystals. In the smiths' chests, ingots come up twice as often as each crystal.

The tables are `sgcraft:chests/simple_dungeon`, `spawn_bonus_chest`, `stronghold_library` and `village_blacksmith`. A data pack changes or empties SGCraft's share by replacing them.

## Server admins

### Structure gates

`worldgen.structure_gates` in `config/sgcraft-server.json` lists the structures that get gates, by the structure's identifier. A structure left out gets none. Each entry has:

| Field | Default | What it does |
| --- | --- | --- |
| `template` | | The structure template the gate is built from |
| `chance` | 25 | How many structures in a hundred get a gate |
| `placement` | `beside` | `inside` the structure, or `beside` it |
| `min_distance`, `max_distance` | 10, 30 | How far from the structure a gate beside it stands, along each axis |
| `ground` | `surface` | What the gate stands on: `surface` (land or the water on it), `ocean_floor`, `structure` (the bottom of the structure's first piece) or `structure_top` (its top) |
| `y_offset` | 0 | Blocks up or down from there |
| `foundation` | `full` | What holds up a gate beside a structure: `none`, `full` (the whole bottom layer filled down to solid ground) or `corners` (a 2 by 2 pillar under each corner) |
| `foundation_block` | `minecraft:stone_bricks` | The block the foundation is made of |
| `chevron_upgrade_chance` | 25 | How many gates in a hundred have 9 chevrons |
| `tokra` | true | Whether a Tok'ra stands at the gate |

The defaults for the swamp hut and igloo use 10 to 29 blocks.

`worldgen.village_gates` sets the plazas:

| Field | Default | What it does |
| --- | --- | --- |
| `weight` | 2 | How often a plaza is chosen against the pool's other pieces, 0 to 150. 0 adds no plazas |
| `pools` | The five biomes' house pools | The plaza template for each pool of houses |
| `tokra` | true | Whether a Tok'ra stands at a village's gate |

`/sgcraft worldgen` lists the structures set to have gates and their chances. `/sgcraft worldgen always true` gives every such structure a gate until the server stops, for trying them out. `/sgcraft worldgen always false` goes back to their chances.
