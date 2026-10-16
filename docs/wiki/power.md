# Power

A gate pays energy to dial out: some as each chevron locks, some as the wormhole forms, and some each tick it stays open. It takes that energy from naquadah in its DHD and from power units near it first, and from its own reserve last.

## Gate energy

Gates count energy in their own unit, gate energy. Only the dialing gate pays. The gate you dial pays nothing, and neither does a wormhole coming in.

A gate with **Needs energy** off (the `requires_power` property, on by default) dials for free. See [Free dialing](#free-dialing).

## Where a gate draws energy

A gate pays from these, in this order:

1. Naquadah in the [DHDs](dhd.md) linked to the gate. A DHD counts when it stands within the gate's DHD reach (`dhd_range`, 6 blocks) of the base across, and no more than 2 blocks above or below it. One naquadah gives 96000 gate energy (`energy_per_naquadah`). The DHD burns a piece only when the gate needs more, last slot first, and keeps what is left of that piece for the next payment. Turn **DHD fuel powers it** off (`dhd_fuel`) to stop a gate taking DHD fuel.
2. Every [power unit](#the-power-unit) within 4 blocks of the gate's base along each axis, in loaded chunks.
3. The gate's own reserve, which holds 1000 energy at most by default (`energy_capacity`, shown as **Reserve** in the configurator). It pays only what the DHDs and power units can't.

Each time the gate pays from DHD fuel or power units, it also draws enough to fill its reserve, which keeps a dial or a wormhole going for a while after the fuel runs out. With the default costs, every dial costs more than 1000, so a gate with only its reserve can't start one.

The DHD's fuel screen (sneak and use the DHD) shows **Gate %**, how full the gate's reserve is.

> **Note:** Add-ons can give gates more energy sources. See [API](api.md).

## The power unit

```infobox
Stargate Power Unit
Icon: sgcraft:power_unit
Type: Block
Holds: 4,000,000 FE
Takes: FE from cables
Gives: Gate energy to gates within 4 blocks
Battery slot: Charges 1000 FE a tick
Dictionary: [Stargate Power Unit](blocks/power-unit.md)
```

The power unit stores FE that cables bring it. On NeoForge it takes NeoForge energy. On Fabric it takes energy from cables that use Team Reborn Energy. It gives no FE back to cables. Only the gates near it and the battery in its slot use its charge.

Place it within 4 blocks of a gate's base along each axis (any block of the 9 by 9 by 9 cube centered on the base). A gate draws from every unit in that space.

Use the unit to open its screen. It shows the FE it holds (**FE:**), its capacity (**Max:**) and a battery slot. A [battery](#batteries-and-the-battery-charger) in the slot charges from the unit's store at up to 1000 FE a tick (`power_unit_charge_rate` in the `malp` settings).

A broken power unit keeps its charge: the item carries it, and the unit has it again when you place it. The battery in its slot drops.

```recipe
sgcraft:power_unit
```

## FE and gate energy

One gate energy costs 80 FE from a power unit. A full unit of 4,000,000 FE holds 50,000 gate energy.

The configurator's **Energy** page shows the rate the server uses: "one is 80 FE from a power unit".

### Server admins

- `power_unit_capacity` (4000000) sets how much FE a power unit holds.
- `bridge_rates` sets how much gate energy one unit of another energy system is worth, by the bridge's identifier. FE is `sgcraft:forge_energy`, worth 0.0125 (1/80) by default. For 40 FE to one gate energy, set:

```json
"bridge_rates": {
  "sgcraft:forge_energy": 0.025
}
```

## What a wormhole costs

| Cost | Default | Setting | When the gate pays |
| --- | --- | --- | --- |
| Per chevron | 100 | `dial_costs.per_chevron` | As each chevron locks, once for each symbol of the address |
| Opening | 4000 | `dial_costs.open` | As the wormhole starts to form, after the last chevron |
| Per tick | 1 | `dial_costs.per_tick` | Each tick the wormhole stays open (20 a second) |

Each cost can be turned off on its own. Each is multiplied by the [distance factor](#the-distance-factor) and the [other world and supergate multipliers](#other-worlds-and-supergates). The chevron and opening costs are rounded to whole numbers. The tick cost keeps its fraction, and fractions add up from tick to tick.

Before a dial starts, the gate must be able to pay for every chevron of the address and the opening together. If it can't, the dial fails with "This gate doesn't have the energy". A dial with **Chevrons lock at once** on pays for all its chevrons as it starts.

If the energy runs out partway through a dial, the dial fails. If it runs out while the wormhole is open, the wormhole shuts.

A dial to a gate nearby with a 7 symbol address costs 700 for the chevrons and 4000 to open, 4700 in all. The wormhole then costs 20 a second, so 5 minutes open (the default `open_ticks` of 6000) costs 6000 more. One naquadah pays for about 20 such dials, or keeps one wormhole open for 80 minutes.

> **Note:** Add-ons can change what a dial costs before the gate pays. See [API](api.md).

## The distance factor

Every cost grows with the distance between the two gates. The distance is measured across the ground, so height doesn't count. For gates in two worlds, it is measured between their coordinates as if they stood in the same world.

The factor is 1 for gates side by side. It rises fast over the first few thousand blocks and slowly after that, because it follows the square of a logarithm of the distance:

```
factor = 1 + 14 × multiplier × (ln(0.05 × d + 1) / ln(223948))²
```

Here `d` is the distance in blocks and `multiplier` is `distance_factor_multiplier` (1.0). The factor reaches 15 at about 4,480,000 blocks. With the default point of origin, two gates that have addresses are at most about 1,056,000 blocks apart, where the factor is 11.9.

| Distance | Factor | Opening cost | Chevron cost |
| --- | --- | --- | --- |
| 0 | 1.00 | 4000 | 100 |
| 100 | 1.30 | 5185 | 130 |
| 1,000 | 2.43 | 9704 | 243 |
| 10,000 | 4.57 | 18260 | 457 |
| 100,000 | 7.69 | 30769 | 769 |
| 373,248 | 9.92 | 39687 | 992 |
| 1,055,708 | 11.91 | 47632 | 1191 |

The costs in the table are for the same world with the default settings. A `distance_factor_multiplier` of 0 keeps the factor at 1 at any distance. A multiplier of 2 doubles the part above 1.

## Other worlds and supergates

- A wormhole to a gate in another world costs 4 times as much (`inter_world_multiplier`, 4.0).
- A wormhole to or from a supergate costs 8 times as much (`supergate_energy_multiplier`, 8.0). It counts when either gate is a supergate.

Both multiply the distance factor. A supergate dialing a gate 1000 blocks away in another world pays 2.43 × 4 × 8, about 78 times the base costs: 7764 for each chevron and 310,542 to open.

## Free dialing

- A gate with **Needs energy** off (`requires_power` false) dials for free, for everyone.
- With `free_in_creative` on (off by default), players in creative mode dial without energy. It counts when a creative player starts the dial, with a DHD, a tool or a command.

## Per-gate settings

A gate can have its own costs and energy settings in place of the server's. Set them on the configurator's **Energy** page (see [Tools](tools.md#stargate-configurator)) or with `/sgcraft property`. A value the gate has no setting of its own for shows in gray, and the server's value applies. **Use server's costs** clears the gate's own costs, per naquadah, distance factor and other worlds values.

| Configurator | Gate property | Default |
| --- | --- | --- |
| Needs energy | `sgcraft:requires_power` | true |
| Reserve | `sgcraft:energy_capacity` | 1000 |
| Chevron cost, Per chevron | `sgcraft:chevron_cost_enabled`, `sgcraft:chevron_cost` | Server's `per_chevron` |
| Opening cost, Opening | `sgcraft:open_cost_enabled`, `sgcraft:open_cost` | Server's `open` |
| Tick cost, Per tick | `sgcraft:tick_cost_enabled`, `sgcraft:tick_cost` | Server's `per_tick` |
| DHD fuel powers it | `sgcraft:dhd_fuel` | true |
| Per naquadah | `sgcraft:energy_per_naquadah` | Server's `energy_per_naquadah` |
| Distance factor | `sgcraft:distance_factor_multiplier` | Server's `distance_factor_multiplier` |
| Other worlds | `sgcraft:inter_world_multiplier` | Server's `inter_world_multiplier` |

The supergate multiplier has no per-gate setting. The dialing gate's own values apply to its dial.

## Batteries and the battery charger

Batteries hold FE for the [MALP](malp.md) and the MALP tablet. Gates don't use them.

```infobox
Battery
Icon: sgcraft:battery
Type: Item
Stacks: 1
Holds: 1,000,000 FE
Used by: MALP, MALP tablet
Dictionary: [Battery](items/battery.md)
```

A battery shows its charge as a green bar and in its tooltip, as FE held, capacity and percent. The creative tab has an empty battery and a full one. Both loaders see it as an FE item, so other mods' chargers can fill it.

Ways to charge a battery:

- Put it in a [power unit's](#the-power-unit) battery slot: up to 1000 FE a tick from the unit.
- Put it in a battery charger: up to 1000 FE a tick.
- Craft it with naquadah: each naquadah adds 100,000 FE, up to its capacity. A full battery doesn't craft, so no naquadah is wasted.

```recipe
sgcraft:battery_recharge
```

```infobox
Battery Charger
Icon: sgcraft:battery_charger
Type: Block
Holds: 200,000 FE
Slots: Battery, naquadah
Charges: 1000 FE a tick
Dictionary: [Battery Charger](blocks/battery-charger.md)
```

The battery charger has a bay for a battery and a slot for naquadah. It holds 200,000 FE, two naquadah's worth. It burns a piece of naquadah whenever its store has room for all 100,000 FE of it, and passes its charge to the battery at up to 1000 FE a tick. Cables can also bring it FE, but it gives none back. Its screen shows how full its store and the battery are.

Breaking the charger drops it and the items in its slots. The FE in its store is lost.

See [The MALP](malp.md#batteries-and-charging) for what a MALP and a tablet use.

### Server admins

The battery settings are in the `malp` object of `config/sgcraft-server.json`:

| Setting | Default | What it does |
| --- | --- | --- |
| `battery_capacity` | 1000000 | FE a battery holds |
| `power_unit_charge_rate` | 1000 | FE a tick a power unit puts into its battery |
| `charger_charge_rate` | 1000 | FE a tick a battery charger puts into its battery |
| `fe_per_naquadah` | 100000 | FE one naquadah gives a battery or a charger; a charger holds twice this |

## Server admins

The energy settings in `config/sgcraft-server.json`:

| Setting | Default | What it does |
| --- | --- | --- |
| `energy_per_naquadah` | 96000 | Gate energy one naquadah gives in a DHD |
| `dial_costs` | per chevron 100, open 4000, per tick 1, all on | What a wormhole costs before distance |
| `distance_factor_multiplier` | 1.0 | How much more a wormhole costs the further it reaches |
| `inter_world_multiplier` | 4.0 | How many times more a wormhole to another world costs |
| `supergate_energy_multiplier` | 8.0 | How many times more a wormhole to or from a supergate costs |
| `free_in_creative` | false | Whether creative players dial without energy |
| `bridge_rates` | `{}` | Gate energy one unit of each bridged energy system is worth |
| `power_unit_capacity` | 4000000 | FE a power unit holds |

`dial_costs` is written like this:

```json
"dial_costs": {
  "per_chevron": { "enabled": true, "energy": 100 },
  "open": { "enabled": true, "energy": 4000 },
  "per_tick": { "enabled": true, "energy": 1 }
}
```

Commands (see [Commands](commands.md)). Only operators can use `costs`, `power`, `battery` and the commands that change a value:

- `/sgcraft energy` shows a gate's reserve, for anyone. Operators can use `/sgcraft energy add <amount>` and `/sgcraft energy set <amount>`.
- `/sgcraft costs` shows the server's costs. `/sgcraft costs <per_chevron|open|per_tick> enabled <true|false>` and `/sgcraft costs <kind> energy <amount>` change them and write the settings file.
- `/sgcraft power [<pos>]` shows the charge of the power unit you look at, or the one at a position. `set <fe>` sets it.
- `/sgcraft battery` shows the charge of the battery you hold. `set <fe>` sets it.
- `/sgcraft property <id> set <json>` and `/sgcraft property <id> reset` set and clear a gate's own value.
