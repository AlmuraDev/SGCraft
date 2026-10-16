# Naquadah Ore

Naquadah ore is the stone block that naquadah comes from. It generates deep in the Overworld, under lava and in small nodes, and you mine it with an iron pickaxe or better.

```infobox
Naquadah Ore
ID: sgcraft:naquadah_ore
Category: Materials
Type: Block
Stackable: Yes (64)
Tool: Pickaxe
Tool tier: Iron or better
Hardness: 3
Blast resistance: 3
Waterloggable: No
Drops: 1 to 2 Naquadah, more with Fortune; itself with Silk Touch
Experience: 2 to 5
Block entity: No
Found in: The Overworld
```

## Obtaining

Naquadah ore has no recipe.

### World generation

Naquadah ore generates in the Overworld by default. With the default settings, every chunk tries both of these ways:

- Under lava, the chunk picks 1 to 16 random columns (`max_nodes_under_lava`, default 16) and searches each from the bottom of the world up to sea level. Wherever lava rests on a solid block, that block starts a cluster of ore up to 3 blocks across and 1 block high.
- As isolated nodes, the chunk gets 1 to 4 nodes (`max_isolated_nodes`, default 4), each up to 2 by 2 by 2 blocks. A node starts 16 to 31 blocks above the bottom of the world, which is y -48 to -33 in the Overworld.

Ore replaces only solid blocks that world generation may replace, so it never replaces bedrock. A cluster or node stops at the edge of its chunk. See [World generation](../worldgen.md#naquadah-ore).

### Silk Touch

Mine the ore with a Silk Touch pickaxe of iron or better to get the ore block itself.

## Usage

Mine the ore with an iron, diamond or netherite pickaxe. It drops 1 to 2 {{item:sgcraft:naquadah}} and 2 to 5 experience. A weaker pickaxe breaks it slowly and drops nothing.

| Tool | Drops |
| --- | --- |
| Iron pickaxe or better | 1 to 2 naquadah |
| With Fortune | More naquadah, by the vanilla ore bonus |
| With Silk Touch | The ore block |
| Any other tool | Nothing |

An explosion that breaks the ore loses some of the naquadah it would drop, as with vanilla ores.

Naquadah fuels DHDs and battery chargers, charges batteries in the crafting grid, and goes into {{item:sgcraft:naquadah_ingot}}.

## Old chunks

Chunks made before SGCraft was added have no naquadah ore. With `add_ores_to_existing_worlds` on (default `false`), each such chunk gets its ore when it loads, up to 32 chunks a tick. The world marks every chunk that has had its ore, so no chunk gets ore twice. See [World generation](../worldgen.md#adding-ore-to-old-chunks).

## Settings

The `worldgen.naquadah_ore` object in `config/sgcraft-server.json`:

| Setting | Default | What it does |
| --- | --- | --- |
| `enable_naquadah_ore` | `true` | Whether naquadah ore generates at all |
| `gen_under_lava_odds` | `1` | One chunk in this many gets clusters under lava. `0` for none |
| `max_nodes_under_lava` | `16` | The most columns of a chunk searched for lava |
| `gen_isolated_odds` | `1` | One chunk in this many gets isolated nodes. `0` for none |
| `max_isolated_nodes` | `4` | The most isolated nodes a chunk gets |
| `dimension_ids` | `["minecraft:overworld"]` | The worlds naquadah ore generates in |
| `add_ores_to_existing_worlds` | `false` | Whether chunks made without ore get it when they load |

A data pack can change the ore block and the blocks it replaces through the `sgcraft:naquadah_ore` feature.

## Commands

Operators can use these. Changes apply to chunks generated or loaded from then on and are written to the settings file.

- `/sgcraft ore` lists the settings and how many old chunks wait for ore.
- `/sgcraft ore <setting> <value>` changes one setting.
- `/sgcraft ore dimension_ids <add|remove> <world>` changes the worlds ore generates in.
- `/sgcraft ore chunk [<x z>]` tells whether a chunk has had its ore. Add `generate` to give that loaded chunk ore now, or `unmark` to have it get ore again the next time it loads while `add_ores_to_existing_worlds` is on.

See [Commands](../commands.md#worldgen-and-ore).

## See also

- [World generation](../worldgen.md#naquadah-ore)
- [Settings](../settings.md#naquadah-ore)
- [Getting started](../getting-started.md#1-find-naquadah)
