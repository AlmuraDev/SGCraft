# Settings

SGCraft reads its settings from three config files. This page gives each setting's default and what it does, and lists every property a single gate can hold.

## The files

| File | Who it is for | What it holds |
| --- | --- | --- |
| `config/sgcraft-server.json` | The server | Energy, costs, worldgen, gate defaults, the MALP, ZPMs and the rest |
| `config/sgcraft-client.json` | Each player | How SGCraft looks and sounds for that player |
| `config/sgcraft-default-addresses.json` | The server | The addresses new PDDs and gate control tablets start with |

Each file is written the first time the game starts without it. A setting left out of a file takes its default. If a file has a mistake, the log says so and the settings it can't read take their defaults.

The server file holds only the settings that differ from their defaults, so a fresh `sgcraft-server.json` is `{}`. Add only the settings you want to change.

The server reads `sgcraft-server.json` as it starts. To change a setting by hand, edit the file and restart the server. Many settings can also be changed in game with a command, which applies the change at once and writes it to the file. See [Changing settings in game](#changing-settings-in-game).

## Example

A short `sgcraft-server.json` that changes a few settings and leaves the rest at their defaults:

```json
{
  "energy": {
    "dial_costs": {
      "per_tick": { "enabled": false, "energy": 1 }
    }
  },
  "gates": {
    "chunk_loading_range": 2,
    "defaults": {
      "sgcraft:open_ticks": 12000,
      "sgcraft:chevrons": 9
    }
  },
  "malp": {
    "signal_range": 512
  }
}
```

## Server settings

`config/sgcraft-server.json` keeps its settings in nine sections: `energy`, `addresses`, `gates`, `tools`, `worldgen`, `remote_view`, `malp`, `shields` and `zpm`. A section left out takes all its defaults. In a section, a setting left out takes its default. This page names a setting by its path, such as `energy.multipliers.inter_world`. [The whole file](#the-whole-file) shows every setting at its default.

### Energy

The `energy` section sets what naquadah and power units hold, the rates of other energy systems, and what dials cost. See [Power](power.md) for how gates use energy.

| Key | Default | Meaning |
| --- | --- | --- |
| `per_naquadah` | `96000` | Gate energy (GE) one naquadah gives, burned in a DHD, crafted into a battery or burned in a battery charger |
| `power_unit_capacity` | `50000` | Most GE a power unit holds; cables fill a full one with 4,000,000 FE |
| `bridge_rates` | `{ "fe_in_per_ge": 80, "fe_out_per_ge": 80 }` | The energy bridges' rates. `fe_in_per_ge` is how many FE buy one GE coming in, and `fe_out_per_ge` how many FE one GE gives going out; an add-on bridge goes in by its ID, as how much GE one of its units is worth |
| `converter_rate` | `62.5` | The most GE an [energy converter](blocks/energy-converter.md) turns into FE each tick; `62.5` is 5,000 FE at the default out rate |
| `free_in_creative` | `false` | Players in creative mode dial without energy |
| `dial_costs` | See [Dial costs](#dial-costs) | What a wormhole costs the gate that dials it, before distance |
| `multipliers` | See [Multipliers](#multipliers) | How many times more a wormhole costs with distance, more chevrons or a supergate |

SGCraft has two bridges. FE has two rates, both 80 FE to one GE unless set. `"bridge_rates": { "fe_in_per_ge": 40, "fe_out_per_ge": 40 }` makes 40 FE worth one GE both ways, as do `/sgcraft costs fe_in_per_ge 40` and `/sgcraft costs fe_out_per_ge 40`. Writing `sgcraft:forge_energy` here is an error the server reports; use `fe_in_per_ge` and `fe_out_per_ge`. `sgcraft:direct` is energy already in GE, worth `1.0`. Add-ons can register more bridges and set their rates by ID, as GE per unit, such as `"bridge_rates": { "fe_in_per_ge": 80, "fe_out_per_ge": 80, "example:steam": 0.5 }`.

Everything inside SGCraft stores and spends GE. The FE rates apply only where SGCraft meets other mods. The in rate covers cables filling power units and battery chargers, and other mods' chargers filling batteries. The out rate covers energy converters, and other mods drawing FE out of a battery.

If `fe_out_per_ge` is higher than `fe_in_per_ge`, FE run through a power unit and an energy converter comes back as more FE. Keep the out rate at or below the in rate unless you want that.

#### Dial costs

`energy.dial_costs` has three costs. Each cost is an object with `enabled` (`true` unless set) and `energy` (required, not negative). Each one is taken before the distance and world multipliers.

| Key | Default | When it is taken |
| --- | --- | --- |
| `per_chevron` | `{ "enabled": true, "energy": 100 }` | As each chevron locks |
| `open` | `{ "enabled": true, "energy": 4000 }` | As the wormhole forms |
| `per_tick` | `{ "enabled": true, "energy": 1 }` | Each tick the wormhole stays open |

A gate's own `chevron_cost`, `open_cost` and `tick_cost` properties (and their `_enabled` switches) override these for that gate. See [Gate properties](#gate-properties).

#### Multipliers

| Key | Default | Meaning |
| --- | --- | --- |
| `distance_factor` | `1.0` | How much more a wormhole costs the further it reaches; `0` makes distance cost nothing extra |
| `inter_world` | `10.0` | How many times more a dial with an eighth chevron costs, to another world of the group |
| `other_group` | `10.0` | How many times more again a dial with a ninth chevron costs, to another group of worlds; `100` times the base by default |
| `supergate` | `8.0` | How many times more a wormhole to or from a supergate costs |

A gate's own `distance_factor_multiplier` and `inter_world_multiplier` properties override the first two for that gate.

### Addresses

The `addresses` section lays each world out into addresses. See [Addresses](addresses.md#cells-and-the-grid).

| Key | Default | Meaning |
| --- | --- | --- |
| `cell_chunks` | `1` | How many chunks wide and long each addressed cell is, from `1` to `256`. One gate per cell can be dialed |
| `point_of_origin` | `"@"` | The symbol every address ends with, which the last chevron locks; `""` for addresses without one |
| `worlds` | `{}` | A world's own `cell_chunks` and `point_of_origin`, by world ID. A value left out is the one above |

```json
"addresses": {
  "cell_chunks": 1,
  "point_of_origin": "@",
  "worlds": {
    "minecraft:the_nether": { "cell_chunks": 8 }
  }
}
```

Bigger cells reach further from the center with the same symbols but leave more space between gates. With cells 8 chunks across, 7 symbols reach about 2,985,984 blocks out each way.

> **Warning:** Changing `cell_chunks` or `point_of_origin` changes every gate address in the worlds it reaches. Address books, default addresses and anything else holding an old address stop reaching the gate. Choose them before players build gates.
>
> The server records each world's layout. When a world that has gates starts with a different one, the server log shows a large `STARGATE ADDRESSES HAVE CHANGED` warning naming each world, its gate count and its old and new layout. It shows once, as the new layout is then recorded. To get the old addresses back, set the old values again and restart. A world with no gates changes without a warning.

### Gates

| Key | Default | Meaning |
| --- | --- | --- |
| `chunk_loading_range` | `1` | Chunks around a busy gate's own that stay loaded, from `0` to `8` |
| `kawoosh_breaks_unbreakable` | `false` | A kawoosh also clears blocks that can't be broken, such as bedrock |
| `owners_break_gates` | `true` | A gate's owner may break it whatever its `breakable` property says |
| `generated_gates_breakable` | `false` | Players may break generated gates that have no `breakable` value of their own |
| `log_events` | `false` | Gates forming and breaking, and wormholes opening, closing and going astray, are written to the server log |
| `defaults` | `{}` | Values gates have for a property when they have none of their own. See [Gate defaults](#gate-defaults) |

#### Gate defaults

`gates.defaults` maps a [gate property](#gate-properties) ID to a JSON value. A gate that has no value of its own for that property uses this one in place of the property's default. Two keys are given to gates once, as they first form, rather than read each time:

- `sgcraft:iris` with `"sgcraft:metal"` fits every new gate with an iris. An iris given this way is part of the gate, so removing it gives no Iris Upgrade back. See [Iris](iris.md).
- `sgcraft:chevrons` with `7`, `8` or `9` sets how many chevrons new gates have.

```json
"gates": {
  "defaults": {
    "sgcraft:iris": "sgcraft:metal",
    "sgcraft:chevrons": 9,
    "sgcraft:one_way_travel": false,
    "sgcraft:default_rights": ["sgcraft:use"]
  }
}
```

Use `/sgcraft gate_defaults` to list, set or reset these in game.

### Tools

| Key | Default | Meaning |
| --- | --- | --- |
| `range` | `6` | How far from a player the handheld tools (PDD, GDO, configurator, Explorer Dialer Device) look for a gate, in blocks |

### World generation

The `worldgen` section sets naquadah ore, the gates the world builds and the Tok'ra at them. See [World generation](worldgen.md).

| Key | Default | Meaning |
| --- | --- | --- |
| `naquadah_ore` | See [Naquadah ore](#naquadah-ore) | How naquadah ore generates |
| `structure_gates` | See [Structure gates](#structure-gates) | The structures that get a gate, and how |
| `village_gates` | See [Village gates](#village-gates) | The gate plazas villages build |
| `tokra_worlds` | `{ "minecraft:overworld": true }` | Whether Tok'ra stand at generated gates, by world ID. See [Tok'ra worlds](#tokra-worlds) |

#### Naquadah ore

`worldgen.naquadah_ore` sets how naquadah ore generates.

| Field | Default | Meaning |
| --- | --- | --- |
| `enable_naquadah_ore` | `true` | Naquadah ore generates at all |
| `gen_under_lava_odds` | `1` | One chunk in this many gets clusters under lava; `0` gives none |
| `max_nodes_under_lava` | `16` | Most columns of a chunk searched for lava, each giving a cluster where lava rests on a solid block |
| `gen_isolated_odds` | `1` | One chunk in this many gets nodes away from lava; `0` gives none |
| `max_isolated_nodes` | `4` | Most such nodes a chunk gets |
| `dimension_ids` | `["minecraft:overworld"]` | The worlds naquadah ore generates in |
| `add_ores_to_existing_worlds` | `false` | Chunks made without naquadah ore, such as before SGCraft was added, get it when they load |

#### Structure gates

`worldgen.structure_gates` maps a structure's ID to how a gate is added to it. A structure left out gets no gate. Writing this key replaces the whole default list, so copy every entry you want to keep.

Each entry has these fields:

| Field | Default | Meaning |
| --- | --- | --- |
| `template` | Required | The structure template the gate and its surroundings are built from |
| `chance` | `25` | How many of these structures in a hundred get a gate (0 to 100) |
| `placement` | `"beside"` | `"inside"` replaces the middle of the structure's first piece; `"beside"` builds it off to one side, clear of every piece |
| `min_distance` | `10` | Beside: fewest blocks from the first piece, along each axis (0 to 96) |
| `max_distance` | `30` | Beside: most blocks from the first piece, along each axis (0 to 96) |
| `ground` | `"surface"` | What the template sits on: `"surface"`, `"ocean_floor"`, `"structure"` (the first piece's bottom) or `"structure_top"` (its top) |
| `y_offset` | `0` | Blocks to raise the template, or lower it if negative |
| `foundation` | `"full"` | What is built under it down to solid ground: `"none"`, `"full"` (the whole bottom layer) or `"corners"` (a 2 by 2 pillar under each corner) |
| `foundation_block` | `stone_bricks` | The block state a foundation is built of |
| `chevron_upgrade_chance` | `25` | How many gates in a hundred have 9 chevrons in place of 7 (0 to 100) |
| `tokra` | `true` | A Tok'ra stands in front of the gate once it is built |

The default list:

| Structure | Template | Placement | Ground | `y_offset` | Tok'ra |
| --- | --- | --- | --- | --- | --- |
| `minecraft:desert_pyramid` | `sgcraft:stargate/pyramid_hall` | Inside | Structure | 0 | Yes |
| `minecraft:jungle_pyramid` | `sgcraft:stargate/jungle_roof` | Inside | Structure top | 0 | Yes |
| `minecraft:monument` | `sgcraft:stargate/ocean_shrine` | Inside | Structure | 16 | No |
| `minecraft:fortress` | `sgcraft:stargate/nether_chamber` | Inside | Structure | 4 | No |
| `minecraft:swamp_hut` | `sgcraft:stargate/swamp_platform` | Beside, 10 to 29 blocks | Surface | 0 | Yes |
| `minecraft:igloo` | `sgcraft:stargate/snowy_platform` | Beside, 10 to 29 blocks | Surface | 0 | Yes |

Every default entry has a chance of 25 and a chevron upgrade chance of 25. The inside entries have no foundation. The beside entries have a full stone brick foundation.

#### Village gates

`worldgen.village_gates` adds gate plazas to village house pools. A village builds one gate at most.

| Field | Default | Meaning |
| --- | --- | --- |
| `weight` | `2` | How often a plaza is chosen against the pool's other pieces (0 to 150); `0` adds none |
| `pools` | The ten pools below | The plaza template for each pool, by pool ID |
| `tokra` | `true` | A Tok'ra stands in front of a village's gate |

By default the plains, desert, savanna, snowy and taiga villages get a plaza in both their `houses` pool and their `zombie/houses` pool, such as `minecraft:village/plains/houses` with `sgcraft:stargate/village_plains`.

> **Warning:** If you write a `village_gates` object without `pools`, the pools are empty and villages build no gates. Include every pool you want.

#### Tok'ra worlds

`worldgen.tokra_worlds` sets whether Tok'ra stand at generated gates, by world ID. A world set to `false` has no Tok'ra at its generated gates. A world left out has them wherever its structure settings (the `tokra` fields above) say.

### Remote view

`remote_view` sets how much of the world around a [MALP](malp.md) is streamed to the player watching it.

| Field | Default | Meaning |
| --- | --- | --- |
| `horizontal_radius` | `32` | How far out from the camera blocks are sent, in blocks (8 to 128) |
| `vertical_radius` | `24` | How far above and below the camera blocks are sent, in blocks (8 to 96) |
| `bytes_per_tick` | `49152` | About how much block data one viewer is sent each tick (4096 to 1048576); a fresh view fills in at this rate |

### MALP

`malp` holds the settings for the [MALP](malp.md) and its batteries. Energy amounts are in GE. Naquadah gives a battery or a battery charger `energy.per_naquadah`, the same as a DHD.

| Field | Default | Meaning |
| --- | --- | --- |
| `battery_capacity` | `12500` | Most GE a battery holds |
| `power_unit_charge_rate` | `12.5` | GE a power unit puts into a battery in its slot each tick |
| `charger_charge_rate` | `12.5` | GE a battery charger puts into its battery each tick |
| `idle_drain` | `0.125` | GE a MALP uses each tick while someone watches its live feed |
| `drive_drain` | `1.25` | GE a MALP uses for each block it drives |
| `signal_range` | `256` | How far a console or tablet reaches a MALP directly, and reaches a gate to relay through, in blocks |
| `linger_ticks` | `600` | How long a MALP's chunks stay loaded after the last person stops watching it, in ticks |
| `console_damage_interrupts` | `true` | A player at a MALP console or tablet who takes damage is taken off it |
| `console_spectators` | `4` | How many players near a driven console see its feed on the monitor, nearest first (0 to 64) |
| `spectator_range` | `16` | How near a console a player must be to see its monitor, in blocks (1 to 64) |
| `tablet_drain` | `0.025` | GE a MALP tablet uses from its own battery each tick while it shows a live feed |

### Shields

`shields` sets the gate shield, the Shield Console and the City Shield Generator. See [The shield](iris.md#the-shield) and [City Shield Generator](blocks/city-shield-generator.md).

| Field | Default | Meaning |
| --- | --- | --- |
| `gate_energy_per_tick` | `1.0` | Gate energy a gate's shield uses each tick while it rises or stays raised; may be a fraction |
| `console_range` | `40` | How far from a Shield Console a gate's ring may be for the console to work its shield, in blocks (1 to 256) |
| `city_energy_per_million_blocks` | `0.333` | Energy a tick a city shield costs for every million blocks it holds |
| `city_max_radius` | `128` | The most a city shield reaches out, up or down from its generator, in blocks (up to 256) |
| `city_power_reach` | `6` | How far from a city shield generator, along each axis, the power units it runs on may stand (1 to 16) |
| `city_keeps_chunks_loaded` | `true` | A raised city shield keeps the chunks under it loaded and running |

### ZPMs

`zpm` sets the ZPM, the ZPM console, interface cart and hub, and ZPM chests. See [Power](power.md#zpms) and [Zero Point Module](items/zpm.md).

A ZPM holds GE, which gates and city shields draw on as on any other source. The console, cart and hub give no FE themselves. An [energy converter](blocks/energy-converter.md) against one turns its GE into FE, at `energy.bridge_rates.fe_out_per_ge` and up to `energy.converter_rate` a tick.

| Field | Default | Meaning |
| --- | --- | --- |
| `capacity` | `26843546` | GE a full ZPM holds |
| `search_range` | `6` | How far from a gate's base or a city shield generator, along each axis, the ZPM consoles, carts and hubs it draws on may stand, in blocks (1 to 16) |
| `tainted_explosion` | `250` | How big the explosion is when a ZPM console, cart or hub draws on a tainted ZPM (0 to 1000); `0` turns it off |
| `chest_chance` | `15` | The chance, in percent, that a generated gate has a chest beside it holding a ZPM (0 to 100) |
| `tainted_chance` | `10` | The chance, in percent, that a ZPM in such a chest is tainted (0 to 100) |

### The whole file

Every server setting at its default. You never need to write all of this. Settings at their defaults can be left out.

```json
{
  "energy": {
    "per_naquadah": 96000,
    "power_unit_capacity": 50000,
    "bridge_rates": {
      "fe_in_per_ge": 80,
      "fe_out_per_ge": 80
    },
    "converter_rate": 62.5,
    "free_in_creative": false,
    "dial_costs": {
      "per_chevron": { "enabled": true, "energy": 100 },
      "open": { "enabled": true, "energy": 4000 },
      "per_tick": { "enabled": true, "energy": 1 }
    },
    "multipliers": {
      "distance_factor": 1.0,
      "inter_world": 10.0,
      "other_group": 10.0,
      "supergate": 8.0
    }
  },
  "addresses": {
    "cell_chunks": 1,
    "point_of_origin": "@",
    "worlds": {}
  },
  "gates": {
    "chunk_loading_range": 1,
    "kawoosh_breaks_unbreakable": false,
    "owners_break_gates": true,
    "generated_gates_breakable": false,
    "log_events": false,
    "defaults": {}
  },
  "tools": {
    "range": 6
  },
  "worldgen": {
    "naquadah_ore": {
      "enable_naquadah_ore": true,
      "gen_under_lava_odds": 1,
      "max_nodes_under_lava": 16,
      "gen_isolated_odds": 1,
      "max_isolated_nodes": 4,
      "dimension_ids": ["minecraft:overworld"],
      "add_ores_to_existing_worlds": false
    },
    "structure_gates": {
      "minecraft:desert_pyramid": {
        "template": "sgcraft:stargate/pyramid_hall",
        "placement": "inside",
        "ground": "structure",
        "foundation": "none"
      },
      "minecraft:jungle_pyramid": {
        "template": "sgcraft:stargate/jungle_roof",
        "placement": "inside",
        "ground": "structure_top",
        "foundation": "none"
      },
      "minecraft:monument": {
        "template": "sgcraft:stargate/ocean_shrine",
        "placement": "inside",
        "ground": "structure",
        "y_offset": 16,
        "foundation": "none",
        "tokra": false
      },
      "minecraft:fortress": {
        "template": "sgcraft:stargate/nether_chamber",
        "placement": "inside",
        "ground": "structure",
        "y_offset": 4,
        "foundation": "none",
        "tokra": false
      },
      "minecraft:swamp_hut": {
        "template": "sgcraft:stargate/swamp_platform",
        "max_distance": 29
      },
      "minecraft:igloo": {
        "template": "sgcraft:stargate/snowy_platform",
        "max_distance": 29
      }
    },
    "village_gates": {
      "weight": 2,
      "pools": {
        "minecraft:village/plains/houses": "sgcraft:stargate/village_plains",
        "minecraft:village/plains/zombie/houses": "sgcraft:stargate/village_plains",
        "minecraft:village/desert/houses": "sgcraft:stargate/village_desert",
        "minecraft:village/desert/zombie/houses": "sgcraft:stargate/village_desert",
        "minecraft:village/savanna/houses": "sgcraft:stargate/village_savanna",
        "minecraft:village/savanna/zombie/houses": "sgcraft:stargate/village_savanna",
        "minecraft:village/snowy/houses": "sgcraft:stargate/village_snowy",
        "minecraft:village/snowy/zombie/houses": "sgcraft:stargate/village_snowy",
        "minecraft:village/taiga/houses": "sgcraft:stargate/village_taiga",
        "minecraft:village/taiga/zombie/houses": "sgcraft:stargate/village_taiga"
      },
      "tokra": true
    },
    "tokra_worlds": { "minecraft:overworld": true }
  },
  "remote_view": {
    "horizontal_radius": 32,
    "vertical_radius": 24,
    "bytes_per_tick": 49152
  },
  "malp": {
    "battery_capacity": 12500,
    "power_unit_charge_rate": 12.5,
    "charger_charge_rate": 12.5,
    "idle_drain": 0.125,
    "drive_drain": 1.25,
    "signal_range": 256,
    "linger_ticks": 600,
    "console_damage_interrupts": true,
    "console_spectators": 4,
    "spectator_range": 16,
    "tablet_drain": 0.025
  },
  "shields": {
    "gate_energy_per_tick": 1.0,
    "console_range": 40,
    "city_energy_per_million_blocks": 0.333,
    "city_max_radius": 128,
    "city_power_reach": 6,
    "city_keeps_chunks_loaded": true
  },
  "zpm": {
    "capacity": 26843546,
    "search_range": 6,
    "tainted_explosion": 250,
    "chest_chance": 15,
    "tainted_chance": 10
  }
}
```

The structure gate entries above leave out the fields at their defaults.

## Client settings

`config/sgcraft-client.json` belongs to each player. It changes nothing on the server or for other players. The game reads it as it starts.

| Key | Default | Meaning |
| --- | --- | --- |
| `hd_event_horizon` | `true` | Draw the event horizon with its detailed texture; `false` uses the original's plain one |
| `copy_address_on_open` | `false` | Opening a gate's base screen copies the gate's address to the clipboard |
| `sound_volume` | `1.0` | How loud SGCraft's sounds play, from `0` (silent) to `1` (full), on top of the game's own volume |

## Default addresses

`config/sgcraft-default-addresses.json` lists the addresses every new PDD and gate control tablet starts with. A PDD or tablet takes them the first time it is used. After that, its addresses belong to its holder. The file starts as an empty list.

Each entry has an `address`, a `name`, and `locked` (`false` unless set). A locked address can be moved in the list but not renamed or removed.

```json
[
  { "address": "ABCD-EFG", "name": "Home", "locked": true },
  { "address": "HIJK-LMN-O", "name": "Outpost" }
]
```

Use `/sgcraft default_addresses` to change the list in game. Changes reach PDDs and tablets used for the first time after the change. See [Addresses](addresses.md).

## Gate properties

Each gate holds its own values for these properties. Every ID starts with `sgcraft:`. A gate with no value of its own uses the server's [gate defaults](#gate-defaults), and otherwise the default below. "Server" in the default column means the gate uses the server setting named.

Change them with the [configurator](tools.md), with `/sgcraft property <id> set <json>` (operators), or through the [API](api.md).

### Wormhole and dialing

| Property | Default | What it does |
| --- | --- | --- |
| `open_ticks` | `6000` | Ticks the wormhole stays open before it closes on its own; `0` for no limit |
| `chevrons_lock_on_dial` | `false` | DHDs and PDDs dialing out lock every chevron at once |
| `ring_speed` | Set by the gate's type | How fast the ring turns, or the light runs round, in degrees a tick (0.1 to 360) |
| `dhd_range` | `6` | How far from the base, in blocks, a DHD can stand and still link (0 to 32) |
| `lightning_malfunction` | `true` | A storm can strike the gate as the wormhole opens and send it to another gate |
| `horizon_transparent` | `true` | The open wormhole lets a little of what is behind it show through |
| `close_from_either_end` | `true` | The dialed gate can close the wormhole, as well as the dialing gate |
| `address_visible` | `true` | Players can see the gate's address on the gate |

### Energy and costs

| Property | Default | What it does |
| --- | --- | --- |
| `requires_power` | `true` | Dialing draws energy; `false` dials for free |
| `energy_capacity` | `1000` | How much energy the gate's store holds |
| `dhd_fuel` | `true` | Naquadah in linked DHDs powers the gate |
| `energy_per_naquadah` | Server, `energy.per_naquadah` | Energy one naquadah gives this gate |
| `chevron_cost_enabled` | Server, `energy.dial_costs.per_chevron` | The gate pays a cost as each chevron locks |
| `chevron_cost` | Server, `energy.dial_costs.per_chevron` | Energy for each chevron, before distance |
| `open_cost_enabled` | Server, `energy.dial_costs.open` | The gate pays a cost as the wormhole forms |
| `open_cost` | Server, `energy.dial_costs.open` | Energy to open, before distance |
| `tick_cost_enabled` | Server, `energy.dial_costs.per_tick` | The gate pays a cost each tick the wormhole is open |
| `tick_cost` | Server, `energy.dial_costs.per_tick` | Energy each tick, before distance |
| `distance_factor_multiplier` | Server, `energy.multipliers.distance_factor` | How much more a wormhole costs the further it reaches |
| `inter_world_multiplier` | Server, `energy.multipliers.inter_world` | How many times more a dial with an eighth chevron costs |

### Travel and safety

| Property | Default | What it does |
| --- | --- | --- |
| `one_way_travel` | `true` | Travelers only pass from the dialing gate to the dialed gate |
| `reverse_travel_kills` | `false` | A traveler entering the dialed gate's wormhole while travel is one way dies |
| `opening_damage` | `true` | The kawoosh harms what is in front of the gate |
| `opening_damage_amount` | `5000` | Harm the kawoosh does each tick |
| `spills_fluid` | `true` | Real fluid moves through the open wormhole |
| `preserve_inventory` | `false` | A player the gate kills (at a shut iris or entering backwards) keeps what they carry; otherwise it is lost unless `keepInventory` is on |
| `breach_explosion` | `10` | Strength of the explosion when the gate is broken while connected, as TNT is 4; `0` for none (0 to 32) |
| `breach_fire` | `true` | The breach explosion sets fires |
| `breach_breaks_blocks` | `true` | The breach explosion breaks blocks |

### Iris and redstone

| Property | Default | What it does |
| --- | --- | --- |
| `iris` | None | The fitted iris: `"sgcraft:metal"` for the metal iris or `"sgcraft:shield"` for a shield. Setting it fits one, starting open (a shield starts lowered); removing it takes it off |
| `phase_shift_passes_iris` | `true` | A traveler carrying a Tollan Phase Shift Device passes the shut iris |
| `iris_restores_state` | `false` | The iris goes back to how it was before the connection once the wormhole closes |
| `redstone_input` | `true` | Redstone can work the gate, such as closing its iris |
| `redstone_output` | `true` | The base gives out a redstone signal while the gate is busy |

### Access

See [Access](access.md).

| Property | Default | What it does |
| --- | --- | --- |
| `allow_incoming` | `true` | Other gates may dial this gate, unless an address rule says otherwise |
| `allow_outgoing` | `true` | This gate may dial other gates, unless an address rule says otherwise |
| `address_rules` | `{}` | Rules for single addresses, each `{ "incoming": <bool>, "outgoing": <bool> }`, by address |
| `default_rights` | `["sgcraft:use", "sgcraft:iris"]` | Rights of players not listed in `player_rights` (the owner has every right) |
| `player_rights` | `{}` | Rights of single players, by UUID, in place of `default_rights` |
| `breakable` | `false` | Any player may break the gate |
| `generated` | `false` | World generation placed the gate |

Only the gate's owner, operators and non-player causes can change `default_rights`, `player_rights`, `breakable` and `generated`. The `configure` right is not enough for these.

## Changing settings in game

These commands change server settings at once and write them to `sgcraft-server.json` (or to `sgcraft-default-addresses.json`). All need permission level 2. If the file can't be written, the change lasts until the server stops. See [Commands](commands.md).

| Setting | Command |
| --- | --- |
| `energy.dial_costs`, `energy.multipliers.inter_world`, `energy.multipliers.other_group` | `/sgcraft costs` |
| `addresses` | `/sgcraft addresses`, which writes the file but takes effect when the server next starts |
| `gates.kawoosh_breaks_unbreakable`, `gates.owners_break_gates`, `gates.generated_gates_breakable` | `/sgcraft settings` |
| `gates.defaults` | `/sgcraft gate_defaults` |
| `gates.chunk_loading_range` | `/sgcraft chunk_loading_range` |
| `gates.log_events` | `/sgcraft log_events` |
| `worldgen.tokra_worlds` | `/sgcraft worldgen tokra` |
| `worldgen.naquadah_ore` | `/sgcraft ore` |
| `malp.console_damage_interrupts`, `malp.console_spectators`, `malp.spectator_range`, `malp.tablet_drain` | `/sgcraft malp ...` |
| `shields` | `/sgcraft shields` |
| `zpm` | `/sgcraft zpm` |
| Default addresses | `/sgcraft default_addresses` |

Every other server setting needs an edit to the file and a restart. `/sgcraft worldgen always` gives every listed structure a gate until the server stops, but writes nothing to the file.
