# Battery Charger

The Battery Charger charges a battery with gate energy (GE) from naquadah it burns or from FE that cables bring it. Batteries power the MALP and the MALP tablet.

```infobox
Battery Charger
ID: sgcraft:battery_charger
Category: Power
Type: Block
Stackable: Yes (64)
Tool: Pickaxe
Tool tier: Any pickaxe
Hardness: 1.5
Blast resistance: 6
Waterloggable: No
Drops: Itself and the items in its slots
Block entity: Yes
Slots: 1 battery, 1 naquadah
Capacity: 192,000 GE
Charge rate: 12.5 GE a tick (charger_charge_rate)
```

## Obtaining

### Crafting

```recipe
sgcraft:battery_charger
```

## Usage

1. Place the charger and right-click it to open the **Battery Charger** screen.
2. Put a {{item:sgcraft:battery}} in the bay on the right.
3. Put {{item:sgcraft:naquadah}} in the slot on the left, or connect FE cables to the charger, or both.

The charger passes GE from its store into the battery at up to 12.5 GE a tick. A battery holds 12,500 GE, so an empty battery fills in 1000 ticks (50 seconds) while the store has GE to give.

## Screen

| Part | What it shows |
| --- | --- |
| Left slot | Naquadah, the only item it takes. A stack of up to 64 fits |
| Orange bar beside it | How full the charger's own store is |
| Right slot | The battery bay. It takes only a battery, one at a time |
| Green bar beside it | How full the battery is, with its percent under the slot |

## Store and fuel

The charger has its own store of 192,000 GE, two naquadah's worth (twice `energy.per_naquadah`).

- It burns one naquadah only when its store has room for all 96,000 GE of that piece, so none is wasted. It burns at most one piece a tick. One piece fills an empty battery more than seven times over.
- FE cables can fill the store too, 80 FE for each GE. On NeoForge it takes NeoForge energy, and on Fabric energy from cables that use Team Reborn Energy, on any side.
- It gives no FE back to cables. The battery in its bay and an [Energy Converter](energy-converter.md) placed against it take its charge.

## Breaking

Breaking the charger drops it, the battery and the naquadah in its slots. The GE in its store is lost.

## Settings

The charger's settings are in `config/sgcraft-server.json`:

| Setting | Default | What it does |
| --- | --- | --- |
| `malp.charger_charge_rate` | 12.5 | GE a tick a battery charger puts into its battery |
| `energy.per_naquadah` | 96000 | GE one naquadah gives a charger, the same as a DHD. A charger holds twice this |
| `malp.battery_capacity` | 12500 | GE a battery holds |

## See also

- [Power](../power.md#batteries-and-the-battery-charger)
- [The MALP](../malp.md#batteries-and-charging)
- [Stargate Power Unit](power-unit.md)
- [Energy Converter](energy-converter.md)
