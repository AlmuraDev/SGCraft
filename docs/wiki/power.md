# Power

A gate pays energy to dial out: some as each chevron locks, some as the wormhole forms, and some each tick it stays open. It takes that energy from naquadah in its DHD, power units and ZPMs near it, the nearest first, and from its own reserve last.

## Gate energy

Gates count energy in their own unit, gate energy (GE). Everything in SGCraft stores and spends GE: power units, ZPMs, batteries and the battery charger. FE only comes in where SGCraft meets other mods' cables and machines. See [FE and gate energy](#fe-and-gate-energy). Only the dialing gate pays. The gate you dial pays nothing, and neither does a wormhole coming in.

A gate with **Needs energy** off (the `requires_power` property, on by default) dials for free. See [Free dialing](#free-dialing).

## Where a gate draws energy

A gate has these energy sources:

- Naquadah in the [DHDs](dhd.md) linked to the gate. A DHD counts when it stands within the gate's DHD reach (`dhd_range`, 6 blocks) of the base across, and no more than 2 blocks above or below it. One naquadah gives 96000 gate energy (`energy.per_naquadah`). The DHD burns a piece only when the gate needs more, last slot first, and keeps what is left of that piece for the next payment. Turn **DHD fuel powers it** off (`dhd_fuel`) to stop a gate taking DHD fuel.
- Every [power unit](#the-power-unit) within 4 blocks of the gate's base along each axis, in loaded chunks.
- Every [ZPM](#zpms) in a ZPM console, interface cart or hub within 6 blocks of the gate's base along each axis, in loaded chunks, and for a supergate the ZPMs in its base's own slots.

The gate draws on its sources one after another. It takes DHD fuel first, then power units, then ZPMs, and within each kind the source nearest its base first. It moves on to the next source only when the one before runs short. Among the ZPMs, a supergate's own base slots come first, then the consoles, carts and hubs, nearest first. Its own reserve, which holds 1000 energy at most by default (`energy_capacity`, shown as **Reserve** in the configurator), pays only what the sources can't.

Each time the gate pays from its sources, it also draws enough to fill its reserve, which keeps a dial or a wormhole going for a while after the fuel runs out. With the default costs, every dial costs more than 1000, so a gate with only its reserve can't start one.

The DHD's fuel screen (sneak and use the DHD) shows **Gate %**, how full the gate's reserve is.

> **Note:** Add-ons can give gates more energy sources. See [API](api.md).

## The power unit

```infobox
Stargate Power Unit
Icon: sgcraft:power_unit
Type: Block
Holds: 50,000 GE
Takes: FE from cables, 80 FE for 1 GE
Gives: Gate energy to gates within 4 blocks
Battery slot: Charges 12.5 GE a tick
Dictionary: [Stargate Power Unit](blocks/power-unit.md)
```

The power unit stores gate energy. Cables fill it with FE: each 80 FE adds 1 GE, and it keeps the fractions. On NeoForge it takes NeoForge energy. On Fabric it takes energy from cables that use Team Reborn Energy. Cables see its charge as FE, 80 FE for each GE it holds. It gives no FE back to cables. The gates near it, the battery in its slot and an [energy converter](#the-energy-converter) against it use its charge.

Place it within 4 blocks of a gate's base along each axis (any block of the 9 by 9 by 9 cube centered on the base). A gate draws from every unit in that space.

Use the unit to open its screen. It shows the GE it holds (**GE:**), its capacity in GE (**Max:**) and a battery slot. A [battery](#batteries-and-the-battery-charger) in the slot charges from the unit's store at up to 12.5 GE a tick (`power_unit_charge_rate` in the `malp` settings).

A broken power unit keeps its charge: the item carries it, and the unit has it again when you place it. The battery in its slot drops.

```recipe
sgcraft:power_unit
```

## FE and gate energy

SGCraft converts FE through its FE energy bridge, at two rates. Both are 80 FE to 1 GE by default.

The in rate (`fe_in_per_ge`) is how many FE buy one GE coming in:

- Cables filling a power unit or a battery charger.
- Other mods' chargers filling a battery, and other mods reading a power unit's or a battery's charge.

The out rate (`fe_out_per_ge`) is how many FE one GE gives going out:

- An [energy converter](#the-energy-converter) giving FE to the block at its output.
- Another mod drawing FE out of a battery.

A full power unit holds 50,000 GE, which cables fill with 4,000,000 FE.

The configurator's **Energy** page shows the in rate the server uses: "Amounts are gate energy, before distance: one is worth 80 FE from other mods".

### The energy converter

```infobox
Energy Converter
Icon: sgcraft:energy_converter
Type: Block
Takes: GE from the store at its input, up to 62.5 a tick
Gives: 80 FE for each GE to the block at its output
Dictionary: [Energy Converter](blocks/energy-converter.md)
```

The [Energy Converter](blocks/energy-converter.md) is how SGCraft's GE reaches other mods. Place it against a ZPM console, interface cart, hub, power unit or battery charger: its input faces that store and its output faces away. Each tick it takes up to 62.5 GE from the store (`energy.converter_rate`) and gives the block at its output 80 FE for each GE, so up to 5,000 FE a tick. It takes only as much GE as that block accepted as FE, and holds none itself. It never takes FE in. A ZPM hub with 2 or 3 charged ZPMs makes their GE go 2 or 3 times as far here too.

```recipe
sgcraft:energy_converter
```

### Server admins

- `energy.power_unit_capacity` (50000) sets how much GE a power unit holds.
- `energy.converter_rate` (62.5) sets the most GE an energy converter turns into FE each tick.
- `energy.bridge_rates` sets the energy bridges' rates. `fe_in_per_ge` (80) is how many FE buy one GE coming in, and `fe_out_per_ge` (80) is how many FE one GE gives going out. Other bridges, which add-ons register, go in by their identifier, as how much gate energy one of their units is worth. Writing `sgcraft:forge_energy` here is an error. For 40 FE to one gate energy both ways, set:

```json
"bridge_rates": {
  "fe_in_per_ge": 40,
  "fe_out_per_ge": 40
}
```

If the out rate is higher than the in rate, FE run through a power unit and a converter comes back as more FE. Keep the out rate at or below the in rate unless you want that.

Operators can also run `/sgcraft costs fe_in_per_ge <fe>`, `/sgcraft costs fe_out_per_ge <fe>` and `/sgcraft costs converter_rate <ge>`.

## ZPMs

```infobox
Zero Point Module
Icon: sgcraft:zpm
Type: Item
Holds: 26,843,546 GE
Gives: GE to gates, and FE through an energy converter
Found: Chests beside generated gates
Dictionary: [Zero Point Module](items/zpm.md)
```

A [Zero Point Module](items/zpm.md) (ZPM) holds 26,843,546 GE when found (`zpm.capacity`): enough to open a wormhole to a gate nearby thousands of times. A ZPM keeps what is left in it as it is moved about. It is never used up, only emptied.

ZPMs have no recipe. The world puts them in chests beside generated gates (see [ZPM chests](worldgen.md#zpm-chests)), and operators can give them with `/sgcraft zpm give`. One chest ZPM in ten is tainted, and explodes when a console, cart or hub holding it is drawn on. See [Tainted ZPMs](items/zpm.md#tainted-zpms).

### Where ZPMs go

| Block | ZPMs | Gives |
| --- | --- | --- |
| [ZPM Console](blocks/zpm-console.md) | 1 | Gate energy to gates and city shields within 6 blocks, and to an energy converter against it |
| [ZPM Interface Cart](blocks/zpm-interface-cart.md) | 1 | As the console |
| [ZPM Hub](blocks/zpm-hub.md) | 3 | As the cart, and the more ZPMs it holds the further they go: with two it draws half as much, with three a third |
| [Supergate Base](blocks/supergate-base.md#zpms) | 3 | Gate energy to its own supergate |
| [City Shield Generator](blocks/city-shield-generator.md) | 3 | Energy to its own shield |

A gate draws on the ZPM blocks within 6 blocks of its base along each axis (`zpm.search_range`), standard gates as well as supergates, after its DHD fuel and power units, the nearest first. A console, cart or hub gives no FE itself and takes no FE in. An [energy converter](#the-energy-converter) placed against one turns its ZPMs' GE into FE.

```recipe
sgcraft:zpm_console
```

### ZPMs and supergates

A supergate needs no ZPMs. It draws on DHD fuel, power units and ZPMs as a standard gate does. Its base has three ZPM slots, and the ZPMs in them are more energy for it to draw on. It takes from those slots before the consoles, carts and hubs near it.

A wormhole to or from a supergate costs 8 times as much (`energy.multipliers.supergate`), so a full ZPM opens about 700 supergate wormholes to a gate nearby. The base's screen shows how many charged ZPMs the supergate can draw on and the energy they hold. See [Supergate Base](blocks/supergate-base.md#zpms).

## What a wormhole costs

| Cost | Default | Setting | When the gate pays |
| --- | --- | --- | --- |
| Per chevron | 100 | `energy.dial_costs.per_chevron` | As each chevron locks, once for each symbol of the address |
| Opening | 4000 | `energy.dial_costs.open` | As the wormhole starts to form, after the last chevron |
| Per tick | 1 | `energy.dial_costs.per_tick` | Each tick the wormhole stays open (20 a second) |

Each cost can be turned off on its own. Each is multiplied by the [distance factor](#the-distance-factor) and the [chevron and supergate multipliers](#other-worlds-and-supergates). The chevron and opening costs are rounded to whole numbers. The tick cost keeps its fraction, and fractions add up from tick to tick.

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

Here `d` is the distance in blocks and `multiplier` is `energy.multipliers.distance_factor` (1.0). The factor reaches 15 at about 4,480,000 blocks. With the default point of origin, two gates that have addresses are at most about 1,056,000 blocks apart, where the factor is 11.9.

| Distance | Factor | Opening cost | Chevron cost |
| --- | --- | --- | --- |
| 0 | 1.00 | 4000 | 100 |
| 100 | 1.30 | 5185 | 130 |
| 1,000 | 2.43 | 9704 | 243 |
| 10,000 | 4.57 | 18260 | 457 |
| 100,000 | 7.69 | 30769 | 769 |
| 373,248 | 9.92 | 39687 | 992 |
| 1,055,708 | 11.91 | 47632 | 1191 |

The costs in the table are for the same world with the default settings. An `energy.multipliers.distance_factor` of 0 keeps the factor at 1 at any distance. A multiplier of 2 doubles the part above 1.

## Other worlds and supergates

- A dial with an eighth chevron (an 8 symbol address, to another world of the same group) costs 10 times as much (`energy.multipliers.inter_world`, 10.0).
- A dial with a ninth chevron (a 9 symbol address, to another group of worlds) costs 10 times as much again (`energy.multipliers.other_group`, 10.0), so 100 times as much by default.
- A wormhole to or from a supergate costs 8 times as much (`energy.multipliers.supergate`, 8.0). It counts when either gate is a supergate.

The multipliers multiply the distance factor, and the supergate multiplier comes last. A dial from the Overworld to the End costs the same as a dial to any other world, and needs no ZPM.

| Address | Multiplier | Chevrons | Opening | Before the wormhole opens |
| --- | --- | --- | --- | --- |
| 7 symbols, same world | 1 | 700 | 4000 | 4700 |
| 8 symbols, another world of the group | 10 | 8000 | 40,000 | 48,000 |
| 9 symbols, another group | 100 | 90,000 | 400,000 | 490,000 |

The table is for two gates side by side, with the default settings. A supergate dialing a gate 1000 blocks away with an 8 symbol address pays 2.43 × 10 × 8, about 194 times the base costs: 19,409 for each chevron and 776,356 to open.

## Free dialing

- A gate with **Needs energy** off (`requires_power` false) dials for free, for everyone.
- With `energy.free_in_creative` on (off by default), players in creative mode dial without energy. It counts when a creative player starts the dial, with a DHD, a tool or a command.

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
| Per naquadah | `sgcraft:energy_per_naquadah` | Server's `energy.per_naquadah` |
| Distance factor | `sgcraft:distance_factor_multiplier` | Server's `energy.multipliers.distance_factor` |
| Other worlds | `sgcraft:inter_world_multiplier` | Server's `energy.multipliers.inter_world` |

The ninth chevron and supergate multipliers have no per-gate setting. Other worlds sets the eighth chevron multiplier for one gate. The dialing gate's own values apply to its dial.

## The gate shield

A gate fitted with a [shield](iris.md#the-shield) pays for it in gate energy: 1 a tick while the shield rises or stays raised (`shields.gate_energy_per_tick`, which may be a fraction). That is 20 a second, or 72,000 an hour. The shield draws from the same places a dial does: naquadah in the gate's DHDs, power units within 4 blocks of its base and ZPMs within 6, in the same order, and the gate's own reserve.

- The shield can't be raised without enough energy for one tick. The player sees "The gate doesn't have the energy to raise its shield".
- The shield falls when the energy runs out.
- A gate with **Needs energy** off (`requires_power` false) runs its shield for free.

## The city shield

```infobox
City Shield Generator
Icon: sgcraft:city_shield_generator
Type: Block
Takes: Gate energy from power units and ZPMs within 6 blocks, and the 3 ZPMs in its slots
Costs: 0.333 a tick for every million blocks it holds
Dictionary: [City Shield Generator](blocks/city-shield-generator.md)
```

The [City Shield Generator](blocks/city-shield-generator.md) raises a dome of energy around itself. It runs on the power units within 6 blocks of it along each axis (`shields.city_power_reach`), the ZPMs in its own three slots, and the ZPMs in consoles, carts and hubs within 6 blocks of it (`zpm.search_range`). It draws on its own ZPM slots first, then the power units, nearest first, then the ZPM consoles, carts and hubs, nearest first. It uses no DHD fuel.

Its cost grows with its size: 0.333 gate energy a tick for every million blocks inside it (`shields.city_energy_per_million_blocks`).

| Size (out, up, down) | Volume | Energy a tick | One full power unit lasts | One full ZPM lasts |
| --- | --- | --- | --- | --- |
| 32, 24, 8 (default) | About 68,600 blocks | About 0.023 | About 30 hours | About 680 days |
| 128, 128, 128 (largest) | About 8.8 million blocks | About 2.9 | About 14 minutes | About 5 days |

A full power unit holds 50,000 GE, and a full ZPM 26,843,546 GE. Raising the shield needs 10 seconds' worth of energy, and the shield falls when its sources run dry. The generator's screen shows the energy available and how long it will keep the shield up.

## Computers

The [Dialing Computer](blocks/dialing-computer.md#power) and the [MALP Console](blocks/malp-console.md#power) use 0.05 gate energy a tick while switched on (`computers.energy_per_tick`), paid once a second. They draw from the power units within 6 blocks of the middle of their desk along each axis (`computers.power_reach`), nearest first, and then from the ZPM consoles, interface carts and hubs within that reach, nearest first. They use no DHD fuel. A full power unit runs one for about 14 hours, and a full ZPM for about 310 days. Without power, a computer goes dark until there is power again. Sneak and use one with an empty hand to switch it off, so that it uses nothing. Turn `computers.requires_power` off to run every computer for free, or use a configurator on one computer to run it free. Its owner, the player who placed it, and operators can.

## Batteries and the battery charger

Batteries hold gate energy for the [MALP](malp.md) and the MALP tablet. Gates don't use them.

```infobox
Battery
Icon: sgcraft:battery
Type: Item
Stacks: 1
Holds: 12,500 GE
Used by: MALP, MALP tablet
Dictionary: [Battery](items/battery.md)
```

A battery shows its charge as a green bar and in its tooltip, as GE held, capacity and percent. The creative tab has an empty battery and a full one. Both loaders see it as an FE item, so other mods' chargers can fill it at 80 FE for each GE. They see its charge as 80 FE for each GE it holds. They can also draw FE out of it, at the out rate.

Ways to charge a battery:

- Put it in a [power unit's](#the-power-unit) battery slot: up to 12.5 GE a tick from the unit.
- Put it in a battery charger: up to 12.5 GE a tick.
- Craft it with naquadah: each naquadah adds 96,000 GE, up to its capacity, so one piece fills it and the rest is lost. A full battery doesn't craft, so no naquadah is wasted on it.

```recipe
sgcraft:battery_recharge
```

```infobox
Battery Charger
Icon: sgcraft:battery_charger
Type: Block
Holds: 192,000 GE
Slots: Battery, naquadah
Charges: 12.5 GE a tick
Dictionary: [Battery Charger](blocks/battery-charger.md)
```

The battery charger has a bay for a battery and a slot for naquadah. It holds 192,000 GE, two naquadah's worth. It burns a piece of naquadah only when its store has room for all 96,000 GE of it, and passes its charge to the battery at up to 12.5 GE a tick. Cables can also fill it with FE, 80 FE for each GE, but it gives none back. An energy converter against it can draw on its store. Its screen shows how full its store and the battery are.

Breaking the charger drops it and the items in its slots. The energy in its store is lost.

See [The MALP](malp.md#batteries-and-charging) for what a MALP and a tablet use.

### Server admins

The battery settings are in the `malp` section of `config/sgcraft-server.json`. Naquadah gives a battery or a charger `energy.per_naquadah` (96000), the same as a DHD.

| Setting | Default | What it does |
| --- | --- | --- |
| `malp.battery_capacity` | 12500 | GE a battery holds |
| `malp.power_unit_charge_rate` | 12.5 | GE a tick a power unit puts into its battery |
| `malp.charger_charge_rate` | 12.5 | GE a tick a battery charger puts into its battery |

## Server admins

The energy settings, in the `energy` section of `config/sgcraft-server.json`:

| Setting | Default | What it does |
| --- | --- | --- |
| `energy.per_naquadah` | 96000 | GE one naquadah gives in a DHD, a battery or a battery charger |
| `energy.dial_costs` | per chevron 100, open 4000, per tick 1, all on | What a wormhole costs before distance |
| `energy.multipliers.distance_factor` | 1.0 | How much more a wormhole costs the further it reaches |
| `energy.multipliers.inter_world` | 10.0 | How many times more a dial with an eighth chevron costs |
| `energy.multipliers.other_group` | 10.0 | How many times more again a dial with a ninth chevron costs |
| `energy.multipliers.supergate` | 8.0 | How many times more a wormhole to or from a supergate costs |
| `energy.free_in_creative` | false | Whether creative players dial without energy |
| `energy.bridge_rates.fe_in_per_ge` | 80 | How many FE buy one GE coming in |
| `energy.bridge_rates.fe_out_per_ge` | 80 | How many FE one GE gives going out |
| `energy.bridge_rates` | `{ "fe_in_per_ge": 80, "fe_out_per_ge": 80 }` | The energy bridges' rates: FE's two, and any add-on bridge's by its identifier, as gate energy per unit |
| `energy.converter_rate` | 62.5 | The most GE an energy converter turns into FE each tick |
| `energy.power_unit_capacity` | 50000 | GE a power unit holds |

`energy.dial_costs` is written like this:

```json
"energy": {
  "dial_costs": {
    "per_chevron": { "enabled": true, "energy": 100 },
    "open": { "enabled": true, "energy": 4000 },
    "per_tick": { "enabled": true, "energy": 1 }
  }
}
```

The shield settings are in the `shields` section: `gate_energy_per_tick` (1.0), `city_energy_per_million_blocks` (0.333) and `city_power_reach` (6). See [Settings](settings.md#shields).

The ZPM settings are in the `zpm` section:

| Setting | Default | What it does |
| --- | --- | --- |
| `capacity` | 26843546 | GE a full ZPM holds |
| `search_range` | 6 | How far from a gate's base or a city shield generator, along each axis, the ZPM blocks it uses may stand (1 to 16) |
| `tainted_explosion` | 250 | Power of a tainted ZPM's explosion; 0 turns it off |
| `chest_chance` | 15 | Percent of generated gates with a ZPM chest beside them |
| `tainted_chance` | 10 | Percent of chest ZPMs that are tainted |

See [Settings](settings.md#zpms).

Commands (see [Commands](commands.md)). Only operators can use `costs`, `power`, `battery`, `shields`, `zpm` and the commands that change a value:

- `/sgcraft energy` shows a gate's reserve, for anyone. Operators can use `/sgcraft energy add <amount>` and `/sgcraft energy set <amount>`.
- `/sgcraft costs` shows the server's costs. `/sgcraft costs <per_chevron|open|per_tick> enabled <true|false>` and `/sgcraft costs <kind> energy <amount>` change them and write the settings file. It also lists the two chevron multipliers, and `/sgcraft costs inter_world_multiplier <times>` and `/sgcraft costs other_group_multiplier <times>` set them. `/sgcraft costs fe_in_per_ge <fe>`, `/sgcraft costs fe_out_per_ge <fe>` and `/sgcraft costs converter_rate <ge>` set the FE rates and the converter's rate.
- `/sgcraft power [<pos>]` shows the charge of the power unit you look at, or the one at a position. It replies in GE, with the FE that is worth. `set <ge>` sets it.
- `/sgcraft battery` shows the charge of the battery you hold, in GE. `set <ge>` sets it.
- `/sgcraft property <id> set <json>` and `/sgcraft property <id> reset` set and clear a gate's own value.
- `/sgcraft shields` shows the shield settings, and `/sgcraft shields <name> <value>` changes one.
- `/sgcraft zpm` shows the ZPM settings, and `/sgcraft zpm <name> <value>` changes one. `/sgcraft zpm give [tainted]` gives you a full ZPM, and `/sgcraft zpm energy <ge>` sets the GE in the one you hold.
