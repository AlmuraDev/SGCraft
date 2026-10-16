# Energy Converter

The Energy Converter turns gate energy (GE) from an SGCraft store into FE for other mods' cables and machines. It takes GE in on one face and gives FE out of the opposite face.

```infobox
Energy Converter
ID: sgcraft:energy_converter
Category: Power
Type: Block
Stackable: Yes (64)
Tool: Pickaxe
Tool tier: Any pickaxe
Hardness: 1.5
Blast resistance: 6
Drops: Itself
Block entity: Yes
Faces: 1 input (teal socket), 1 output (orange socket)
Rate: Up to 62.5 GE a tick (energy.converter_rate)
FE out: 80 FE for each GE (energy.bridge_rates.fe_out_per_ge), up to 5,000 FE a tick
```

## Obtaining

### Crafting

Craft it from 4 {{item:minecraft:iron_ingot}}, 2 {{item:sgcraft:naquadah_ingot}}, 2 {{item:minecraft:redstone}} and 1 {{item:minecraft:copper_ingot}}:

```recipe
sgcraft:energy_converter
```

## Placing it

The converter has an input face with a teal socket and an output face with an orange socket. Its other four faces are plain.

1. Place the converter against an SGCraft energy store. Its input faces the block you placed it on.
2. Its output faces away from the store. Put a cable or a machine that takes FE there.

If the block at its output takes no FE, nothing moves.

## What it draws from

The converter draws GE from the store at its input:

- A [ZPM Console](zpm-console.md), [ZPM Interface Cart](zpm-interface-cart.md) or [ZPM Hub](zpm-hub.md). A hub with 2 or 3 charged ZPMs makes their GE go 2 or 3 times as far here too.
- A [Stargate Power Unit](power-unit.md).
- A [Battery Charger](battery-charger.md).

A tainted ZPM explodes when a converter draws on its block. See [Zero Point Module](../items/zpm.md#tainted-zpms).

## Rates and limits

Each tick the converter takes up to 62.5 GE from its input (`energy.converter_rate`). It gives the block at its output 80 FE for each GE (`energy.bridge_rates.fe_out_per_ge`), so up to 5,000 FE a tick by default.

- It takes only as much GE as the output block accepted as FE. Nothing is lost when the output is full.
- It holds no energy itself.
- It never takes FE in. To fill a store with FE, connect the cables to a power unit or a battery charger.

On NeoForge it gives NeoForge energy. On Fabric it gives energy to cables and machines that use Team Reborn Energy.

The in and out rates are separate. If `fe_out_per_ge` is higher than `fe_in_per_ge`, FE run through a power unit and a converter comes back as more FE. Keep the out rate at or below the in rate unless you want that. See [Power](../power.md#fe-and-gate-energy).

## Breaking

Breaking the converter drops it. It holds nothing, so nothing is lost.

## Settings

The `energy` object of `config/sgcraft-server.json`:

| Setting | Default | What it does |
| --- | --- | --- |
| `converter_rate` | 62.5 | The most GE a converter turns into FE each tick |
| `bridge_rates.fe_out_per_ge` | 80 | How many FE one GE gives going out |

See [Settings](../settings.md#energy).

## Commands

- `/sgcraft costs converter_rate <ge>` sets the most GE a converter turns into FE each tick (saved).
- `/sgcraft costs fe_out_per_ge <fe>` sets how many FE one GE gives going out (saved).
- `/sgcraft costs` shows both, with "An energy converter turns up to <ge> gate energy a tick into FE".

See [Commands](../commands.md).

## See also

- [Power](../power.md#fe-and-gate-energy)
- [Stargate Power Unit](power-unit.md)
- [ZPM Console](zpm-console.md)
- [Zero Point Module](../items/zpm.md)
