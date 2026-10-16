# Settings

SGCraft reads its settings from three config files. This page gives each setting's default and what it does, and lists every property a single gate can hold.

## The files

| File | Who it is for | What it holds |
| --- | --- | --- |
| `config/sgcraft-server.json` | The server | Energy, costs, worldgen, gate defaults, the MALP and the rest |
| `config/sgcraft-client.json` | Each player | How SGCraft looks and sounds for that player |
| `config/sgcraft-default-addresses.json` | The server | The addresses new PDDs and gate control tablets start with |

Each file is written with its defaults the first time the game starts without it. A setting left out of a file takes its default. If a file has a mistake, the log says so and the settings it can't read take their defaults.

The server reads `sgcraft-server.json` as it starts. To change a setting by hand, edit the file and restart the server. Many settings can also be changed in game with a command, which applies the change at once and writes it to the file. See [Changing settings in game](#changing-settings-in-game).

## Example

A short `sgcraft-server.json` that changes a few settings and leaves the rest at their defaults:

```json
{
  "energy_per_naquadah": 96000,
  "dial_costs": {
    "per_chevron": { "enabled": true, "energy": 100 },
    "open": { "enabled": true, "energy": 4000 },
    "per_tick": { "enabled": false, "energy": 1 }
  },
  "chunk_loading_range": 2,
  "gate_defaults": {
    "sgcraft:open_ticks": 12000,
    "sgcraft:chevrons": 9
  },
  "malp": {
    "signal_range": 512
  }
}
```

## Server settings

These are the top-level keys of `config/sgcraft-server.json`. Groups that hold more keys have their own sections below.

### Energy

See [Power](power.md) for how gates use energy.

| Key | Default | Meaning |
| --- | --- | --- |
| `energy_per_naquadah` | `96000` | Energy one naquadah gives a gate when burned as fuel, such as in a DHD |
| `dial_costs` | See [Dial costs](#dial-costs) | What a wormhole costs the gate that dials it, before distance |
| `distance_factor_multiplier` | `1.0` | How much more a wormhole costs the further it reaches; `0` makes distance cost nothing extra |
| `inter_world_multiplier` | `4.0` | How many times more a wormhole to another world costs |
| `supergate_energy_multiplier` | `8.0` | How many times more a wormhole to or from a supergate costs |
| `free_in_creative` | `false` | Players in creative mode dial without energy |
| `bridge_rates` | `{}` | How many gate energy units one unit of a bridged energy system is worth, by the bridge's ID, in place of the bridge's own rate |
| `power_unit_capacity` | `4000000` | Most FE a power unit holds |

SGCraft has two bridges for `bridge_rates`: `sgcraft:forge_energy` (FE, 80 FE to one unit, a rate of `0.0125`) and `sgcraft:direct` (energy already in gate units, `1.0`). Add-ons can register more. For example, `"bridge_rates": { "sgcraft:forge_energy": 0.025 }` makes 40 FE worth one unit.

### Dial costs

The `dial_costs` group has three costs. Each cost is an object with `enabled` (`true` unless set) and `energy` (required, not negative). Each one is taken before the distance and world multipliers.

| Key | Default | When it is taken |
| --- | --- | --- |
| `per_chevron` | `{ "enabled": true, "energy": 100 }` | As each chevron locks |
| `open` | `{ "enabled": true, "energy": 4000 }` | As the wormhole forms |
| `per_tick` | `{ "enabled": true, "energy": 1 }` | Each tick the wormhole stays open |

A gate's own `chevron_cost`, `open_cost` and `tick_cost` properties (and their `_enabled` switches) override these for that gate. See [Gate properties](#gate-properties).

### Gates

| Key | Default | Meaning |
| --- | --- | --- |
| `tool_range` | `6` | How far from a player the handheld tools (PDD, GDO, configurator) look for a gate, in blocks |
| `point_of_origin` | `"@"` | The symbol every address ends with, which the last chevron locks; `""` for addresses without one |
| `kawoosh_breaks_unbreakable` | `false` | A kawoosh also clears blocks that can't be broken, such as bedrock |
| `chunk_loading_range` | `1` | Chunks around a busy gate's own that stay loaded, from `0` to `8` |
| `log_stargate_events` | `false` | Gates forming and breaking, and wormholes opening, closing and going astray, are written to the server log |
| `owners_break_gates` | `true` | A gate's owner may break it whatever its `breakable` property says |
| `generated_gates_breakable` | `false` | Players may break generated gates that have no `breakable` value of their own |
| `tokra_worlds` | `{ "minecraft:overworld": true }` | Whether Tok'ra stand at generated gates, by world ID |
| `gate_defaults` | `{}` | Values gates have for a property when they have none of their own. See [Gate defaults](#gate-defaults) |

> **Warning:** Changing `point_of_origin` changes every address on the server. The server logs a warning naming each world when it does. See [Addresses](addresses.md).

For `tokra_worlds`, a world set to `false` has no Tok'ra at its generated gates. A world left out has them wherever its structure settings (the `tokra` field below) say.

### Gate defaults

`gate_defaults` maps a [gate property](#gate-properties) ID to a JSON value. A gate that has no value of its own for that property uses this one in place of the property's default. Two keys are given to gates once, as they first form, rather than read each time:

- `sgcraft:iris` with `"sgcraft:metal"` fits every new gate with an iris. An iris given this way is part of the gate, so removing it gives no Iris Upgrade back. See [Iris](iris.md).
- `sgcraft:chevrons` with `7`, `8` or `9` sets how many chevrons new gates have.

```json
"gate_defaults": {
  "sgcraft:iris": "sgcraft:metal",
  "sgcraft:chevrons": 9,
  "sgcraft:one_way_travel": false,
  "sgcraft:default_rights": ["sgcraft:use"]
}
```

Use `/sgcraft gate_defaults` to list, set or reset these in game.

### Structure gates

`structure_gates` maps a structure's ID to how a gate is added to it. A structure left out gets no gate. Writing this key replaces the whole default list, so copy every entry you want to keep. See [World generation](worldgen.md).

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

### Village gates

`village_gates` adds gate plazas to village house pools. A village builds one gate at most.

| Field | Default | Meaning |
| --- | --- | --- |
| `weight` | `2` | How often a plaza is chosen against the pool's other pieces (0 to 150); `0` adds none |
| `pools` | The ten pools below | The plaza template for each pool, by pool ID |
| `tokra` | `true` | A Tok'ra stands in front of a village's gate |

By default the plains, desert, savanna, snowy and taiga villages get a plaza in both their `houses` pool and their `zombie/houses` pool, such as `minecraft:village/plains/houses` with `sgcraft:stargate/village_plains`.

> **Warning:** If you write a `village_gates` object without `pools`, the pools are empty and villages build no gates. Include every pool you want.

### Naquadah ore

`naquadah_ore` sets how naquadah ore generates. See [World generation](worldgen.md).

| Field | Default | Meaning |
| --- | --- | --- |
| `enable_naquadah_ore` | `true` | Naquadah ore generates at all |
| `gen_under_lava_odds` | `1` | One chunk in this many gets clusters under lava; `0` gives none |
| `max_nodes_under_lava` | `16` | Most columns of a chunk searched for lava, each giving a cluster where lava rests on a solid block |
| `gen_isolated_odds` | `1` | One chunk in this many gets nodes away from lava; `0` gives none |
| `max_isolated_nodes` | `4` | Most such nodes a chunk gets |
| `dimension_ids` | `["minecraft:overworld"]` | The worlds naquadah ore generates in |
| `add_ores_to_existing_worlds` | `false` | Chunks made without naquadah ore, such as before SGCraft was added, get it when they load |

### Remote view

`remote_view` sets how much of the world around a [MALP](malp.md) is streamed to the player watching it.

| Field | Default | Meaning |
| --- | --- | --- |
| `horizontal_radius` | `32` | How far out from the camera blocks are sent, in blocks (8 to 128) |
| `vertical_radius` | `24` | How far above and below the camera blocks are sent, in blocks (8 to 96) |
| `bytes_per_tick` | `49152` | About how much block data one viewer is sent each tick (4096 to 1048576); a fresh view fills in at this rate |

### MALP

`malp` holds the settings for the [MALP](malp.md) and its batteries.

| Field | Default | Meaning |
| --- | --- | --- |
| `battery_capacity` | `1000000` | Most FE a battery holds |
| `power_unit_charge_rate` | `1000` | FE a power unit puts into a battery in its slot each tick |
| `charger_charge_rate` | `1000` | FE a battery charger puts into its battery each tick |
| `fe_per_naquadah` | `100000` | FE one naquadah gives a battery, crafted in or burned in a charger; a charger stores twice this |
| `idle_drain` | `10` | FE a MALP uses each tick while someone watches its live feed |
| `drive_drain` | `100` | FE a MALP uses for each block it drives |
| `signal_range` | `256` | How far a console or tablet reaches a MALP directly, and reaches a gate to relay through, in blocks |
| `linger_ticks` | `600` | How long a MALP's chunks stay loaded after the last person stops watching it, in ticks |
| `console_damage_interrupts` | `true` | A player at a MALP console or tablet who takes damage is taken off it |
| `console_spectators` | `4` | How many players near a driven console see its feed on the monitor, nearest first (0 to 64) |
| `spectator_range` | `16` | How near a console a player must be to see its monitor, in blocks (1 to 64) |
| `tablet_drain` | `2` | FE a MALP tablet uses from its own battery each tick while it shows a live feed |

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
| `energy_per_naquadah` | Server, `energy_per_naquadah` | Energy one naquadah gives this gate |
| `chevron_cost_enabled` | Server, `dial_costs.per_chevron` | The gate pays a cost as each chevron locks |
| `chevron_cost` | Server, `dial_costs.per_chevron` | Energy for each chevron, before distance |
| `open_cost_enabled` | Server, `dial_costs.open` | The gate pays a cost as the wormhole forms |
| `open_cost` | Server, `dial_costs.open` | Energy to open, before distance |
| `tick_cost_enabled` | Server, `dial_costs.per_tick` | The gate pays a cost each tick the wormhole is open |
| `tick_cost` | Server, `dial_costs.per_tick` | Energy each tick, before distance |
| `distance_factor_multiplier` | Server, `distance_factor_multiplier` | How much more a wormhole costs the further it reaches |
| `inter_world_multiplier` | Server, `inter_world_multiplier` | How many times more a wormhole to another world costs |

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
| `iris` | None | The fitted iris (`"sgcraft:metal"`). Setting it fits an iris, starting open; removing it takes the iris off |
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
| `dial_costs` | `/sgcraft costs` |
| `kawoosh_breaks_unbreakable`, `owners_break_gates`, `generated_gates_breakable` | `/sgcraft settings` |
| `gate_defaults` | `/sgcraft gate_defaults` |
| `chunk_loading_range` | `/sgcraft chunk_loading_range` |
| `log_stargate_events` | `/sgcraft log_events` |
| `tokra_worlds` | `/sgcraft worldgen tokra` |
| `naquadah_ore` | `/sgcraft ore` |
| `malp.console_damage_interrupts`, `malp.console_spectators`, `malp.spectator_range`, `malp.tablet_drain` | `/sgcraft malp ...` |
| Default addresses | `/sgcraft default_addresses` |

Every other server setting needs an edit to the file and a restart. `/sgcraft worldgen always` gives every listed structure a gate until the server stops, but writes nothing to the file.
