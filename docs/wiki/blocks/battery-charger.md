# Battery Charger

The Battery Charger charges a battery from naquadah it burns or from FE that cables bring it. Batteries power the MALP and the MALP tablet.

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
Capacity: 200,000 FE
Charge rate: 1000 FE a tick (charger_charge_rate)
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

The charger passes FE from its store into the battery at up to 1000 FE a tick. A battery holds 1,000,000 FE, so an empty battery fills in 1000 ticks (50 seconds) while the store has FE to give.

## Screen

| Part | What it shows |
| --- | --- |
| Left slot | Naquadah, the only item it takes. A stack of up to 64 fits |
| Orange bar beside it | How full the charger's own store is |
| Right slot | The battery bay. It takes only a battery, one at a time |
| Green bar beside it | How full the battery is, with its percent under the slot |

## Store and fuel

The charger has its own store of 200,000 FE, two naquadah's worth (twice `fe_per_naquadah`).

- It burns one naquadah whenever its store has room for all 100,000 FE of that piece, so no FE is wasted. It burns at most one piece a tick. Filling an empty battery from naquadah alone takes 10 pieces.
- FE cables can fill the store too. On NeoForge it takes NeoForge energy, and on Fabric energy from cables that use Team Reborn Energy, on any side.
- It gives no FE back to cables. Only the battery in its bay takes its charge.

## Breaking

Breaking the charger drops it, the battery and the naquadah in its slots. The FE in its store is lost.

## Settings

The battery settings are in the `malp` object of `config/sgcraft-server.json`:

| Setting | Default | What it does |
| --- | --- | --- |
| `charger_charge_rate` | 1000 | FE a tick a battery charger puts into its battery |
| `fe_per_naquadah` | 100000 | FE one naquadah gives a charger. A charger holds twice this |
| `battery_capacity` | 1000000 | FE a battery holds |

## See also

- [Power](../power.md#batteries-and-the-battery-charger)
- [The MALP](../malp.md#batteries-and-charging)
- [Stargate Power Unit](power-unit.md)
