# Stargate Power Unit

The Stargate Power Unit stores FE that cables bring it and gives it as gate energy to the gates near it. Its slot also charges a battery.

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
Capacity: 4,000,000 FE (power_unit_capacity)
Range: 4 blocks from a gate's base along each axis
Battery slot: 1000 FE a tick (power_unit_charge_rate)
```

## Obtaining

### Crafting

```recipe
sgcraft:power_unit
```

## Usage

1. Place the unit within 4 blocks of a gate's base along each axis: any block of the 9 by 9 by 9 cube centered on the base.
2. Connect FE cables to it. On NeoForge it takes NeoForge energy. On Fabric it takes energy from cables that use Team Reborn Energy. Cables can connect on any side.
3. Dial out from the gate. The gate draws gate energy from the unit when its DHD fuel runs short.

The unit gives no FE back to cables. Only the gates near it and the battery in its slot use its charge.

## Screen

Right-click the unit to open the **Stargate Power Unit** screen. It shows:

- **FE:** the FE the unit holds,
- **Max:** its capacity,
- a **Battery** slot, which takes only a {{item:sgcraft:battery}}, with the battery's charge in percent beside it.

## Powering gates

A gate pays for a dial from these, in this order: naquadah in its linked DHDs, then every power unit in range, then its own reserve. A gate draws from every unit in range whose chunk is loaded. Each time it pays from DHD fuel or power units, it also draws enough to fill its reserve.

One gate energy costs 80 FE, so a full unit of 4,000,000 FE holds 50,000 gate energy. A dial to a gate nearby with a 7 symbol address costs 4700 gate energy, which is 376,000 FE. See [Power](../power.md#what-a-wormhole-costs) for distance and other worlds.

## Charging a battery

A battery in the unit's slot charges from the unit's store at up to 1000 FE a tick (`power_unit_charge_rate` in the `malp` settings). A battery holds 1,000,000 FE. See [Power](../power.md#batteries-and-the-battery-charger).

## Breaking

A broken power unit keeps its charge. The item carries the charge in its `sgcraft:energy` data component, and the unit has it again when you place it. The battery in its slot drops on its own.

## Settings

| Setting | Default | What it does |
| --- | --- | --- |
| `power_unit_capacity` | 4000000 | FE a power unit holds |
| `bridge_rates` | `{}` | Gate energy one unit of each bridged energy system is worth. FE is `sgcraft:forge_energy`, worth 0.0125 (1/80) unless set |
| `malp.power_unit_charge_rate` | 1000 | FE a tick a power unit puts into its battery |

For 40 FE to one gate energy, set `"bridge_rates": { "sgcraft:forge_energy": 0.025 }`. See [Power](../power.md#fe-and-gate-energy).

## Commands

Operators can use these:

- `/sgcraft power [<pos>]` shows the FE and gate energy in the power unit you look at (within 8 blocks), or the one at a position.
- `/sgcraft power [<pos>] set <fe>` sets a unit's FE, kept within its capacity.

See [Commands](../commands.md#energy-and-power).

## See also

- [Power](../power.md#the-power-unit)
- [Battery Charger](battery-charger.md)
- [The DHD](../dhd.md#fuel)
