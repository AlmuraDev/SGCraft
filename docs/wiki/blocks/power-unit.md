# Stargate Power Unit

The Stargate Power Unit stores gate energy (GE) for the gates near it. Cables fill it with FE, 80 FE for each GE. Its slot also charges a battery.

```infobox
Stargate Power Unit
ID: sgcraft:power_unit
Category: Power
Type: Block
Stackable: Yes (64)
Tool: Pickaxe
Tool tier: Any pickaxe
Hardness: 1.5
Blast resistance: 10
Waterloggable: No
Drops: Itself, with its charge; the battery in its slot
Block entity: Yes
Capacity: 50,000 GE (energy.power_unit_capacity)
Range: 4 blocks from a gate's base along each axis
Battery slot: 12.5 GE a tick (power_unit_charge_rate)
```

## Obtaining

### Crafting

```recipe
sgcraft:power_unit
```

## Usage

1. Place the unit within 4 blocks of a gate's base along each axis: any block of the 9 by 9 by 9 cube centered on the base.
2. Connect FE cables to it. On NeoForge it takes NeoForge energy. On Fabric it takes energy from cables that use Team Reborn Energy. Cables can connect on any side. Each 80 FE they bring adds 1 GE, and the unit keeps the fractions.
3. Dial out from the gate. The gate draws gate energy from the unit after its DHD fuel.

Cables and other mods see its charge as FE, 80 FE for each GE it holds. The unit gives no FE back to cables. The gates near it, the battery in its slot and an [Energy Converter](energy-converter.md) placed against it use its charge.

## Screen

Right-click the unit to open the **Stargate Power Unit** screen. It shows:

- **GE:** the GE the unit holds,
- **Max:** its capacity in GE,
- a **Battery** slot, which takes only a {{item:sgcraft:battery}}, with the battery's charge in percent beside it.

## Powering gates

A gate pays for a dial from its sources one after another: naquadah in its linked DHDs first, then the power units in range, then the ZPMs near it. It takes from the nearest unit first, and from the next only when the nearer runs short. Its own reserve pays only what they can't. A gate draws from every unit in range whose chunk is loaded. Each time it pays from its sources, it also draws enough to fill its reserve.

A supergate draws on power units as a standard gate does. See [Power](../power.md#zpms-and-supergates).

A full unit holds 50,000 GE, which cables fill with 4,000,000 FE. A dial to a gate nearby with a 7 symbol address costs 4700 GE, so a full unit pays for 10 such dials, with GE left over for the wormholes. A dial to another world costs 10 times as much, and to another group of worlds 100 times. See [Power](../power.md#what-a-wormhole-costs) for distance and other worlds.

## Charging a battery

A battery in the unit's slot charges from the unit's store at up to 12.5 GE a tick (`power_unit_charge_rate` in the `malp` settings). A battery holds 12,500 GE, so an empty one fills in 1000 ticks (50 seconds). See [Power](../power.md#batteries-and-the-battery-charger).

## Breaking

A broken power unit keeps its charge. The item carries the charge in its `sgcraft:energy` data component, and the unit has it again when you place it. The battery in its slot drops on its own.

## Settings

| Setting | Default | What it does |
| --- | --- | --- |
| `energy.power_unit_capacity` | 50000 | GE a power unit holds |
| `energy.bridge_rates.fe_in_per_ge` | 80 | How many FE buy one GE coming in |
| `malp.power_unit_charge_rate` | 12.5 | GE a tick a power unit puts into its battery |

For 40 FE to one GE, set `"bridge_rates": { "fe_in_per_ge": 40, "fe_out_per_ge": 40 }` or run `/sgcraft costs fe_in_per_ge 40`. See [Power](../power.md#fe-and-gate-energy).

## Commands

Operators can use these:

- `/sgcraft power [<pos>]` shows the charge of the power unit you look at (within 8 blocks), or the one at a position: "The power unit at <pos> holds <held> of <capacity> GE (<fe> FE)".
- `/sgcraft power [<pos>] set <ge>` sets a unit's GE, kept within its capacity, and replies "The power unit at <pos> now holds <held> GE".

See [Commands](../commands.md#energy-and-power).

## See also

- [Power](../power.md#the-power-unit)
- [Battery Charger](battery-charger.md)
- [Energy Converter](energy-converter.md)
- [The DHD](../dhd.md#fuel)
