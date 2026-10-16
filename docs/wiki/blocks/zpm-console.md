# ZPM Console

The ZPM Console holds one ZPM. Gates and city shield generators near it draw on that ZPM's gate energy (GE). An [Energy Converter](energy-converter.md) turns its GE into FE.

```infobox
ZPM Console
ID: sgcraft:zpm_console
Category: Power
Type: Block
Stackable: Yes (64)
Tool: Pickaxe
Tool tier: Any pickaxe
Hardness: 1.5
Blast resistance: 6
Drops: Itself and its ZPM
Block entity: Yes
Slots: 1 ZPM
Range: 6 blocks along each axis (zpm.search_range)
```

## Obtaining

### Crafting

Craft it from 2 {{item:minecraft:emerald}}, 1 {{item:minecraft:glass_pane}}, 1 {{item:sgcraft:controller_crystal}}, 1 {{item:sgcraft:core_crystal}}, 3 {{item:minecraft:iron_block}} and 1 {{item:minecraft:gold_block}}:

```recipe
sgcraft:zpm_console
```

## Usage

1. Place the console. It faces you.
2. Right-click it to open its screen, and put a {{item:sgcraft:zpm}} in its slot. The console shows the ZPM while it holds one.
3. Place it within 6 blocks of a gate's base along each axis, or of a city shield generator, for them to draw on it.

## Screen

The **ZPM Console** screen has one **ZPM** slot, which takes only a ZPM, and shows "Available Power: N GE", the GE left in the ZPM.

## Powering gates

A gate whose base is within 6 blocks of the console along each axis (`zpm.search_range`) draws on its ZPM after its DHD fuel and power units, the nearest ZPM block first. A gate takes the ZPM's GE as it takes any other.

- A [supergate](../building-gates.md#zpms-for-supergates) draws on it too, after the ZPMs in its own base.
- A [City Shield Generator](city-shield-generator.md) within 6 blocks draws on it too.

See [Power](../power.md#zpms).

## FE

The console gives no FE itself, and it takes no FE in. To power other mods' machines from its ZPM, place an [Energy Converter](energy-converter.md) against it, input first. The converter takes up to 62.5 GE a tick from the ZPM and gives 80 FE for each GE.

## Tainted ZPMs

A tainted ZPM in the console explodes when the console is drawn on, by a gate, a city shield or an energy converter: power 250, with fire, by default (`zpm.tainted_explosion`, 0 turns it off). See [Zero Point Module](../items/zpm.md#tainted-zpms).

## Breaking

Breaking the console drops it and its ZPM. The ZPM keeps the energy left in it.

## Settings

The `zpm` object of `config/sgcraft-server.json`:

| Setting | Default | What it does |
| --- | --- | --- |
| `search_range` | 6 | How far from a gate's base or a city shield generator, along each axis, the console may stand (1 to 16) |
| `tainted_explosion` | 250 | Power of a tainted ZPM's explosion; 0 turns it off |

See [Settings](../settings.md#zpms).

## See also

- [Zero Point Module](../items/zpm.md)
- [ZPM Interface Cart](zpm-interface-cart.md)
- [ZPM Hub](zpm-hub.md)
- [Energy Converter](energy-converter.md)
- [Power](../power.md#zpms)
