# ZPM Hub

The ZPM Hub holds three ZPMs. It gives their gate energy (GE) to the gates and city shield generators near it, and to an [Energy Converter](energy-converter.md) placed against it. The more ZPMs it holds, the further their energy goes.

```infobox
ZPM Hub
ID: sgcraft:zpm_hub
Category: Power
Type: Block
Stackable: Yes (64)
Tool: Pickaxe
Tool tier: Any pickaxe
Hardness: 1.5
Blast resistance: 6
Drops: Itself and its ZPMs
Block entity: Yes
Slots: 3 ZPMs
Range: 6 blocks along each axis (zpm.search_range)
```

## Obtaining

### Crafting

Craft it from 1 {{item:minecraft:comparator}}, 1 {{item:minecraft:iron_block}}, 3 {{item:minecraft:copper_ingot}}, 3 {{item:sgcraft:zpm_console}}s and 1 {{item:minecraft:iron_ingot}}:

```recipe
sgcraft:zpm_hub
```

## Usage

1. Place the hub. It faces you.
2. Right-click it to open its screen, and put up to three {{item:sgcraft:zpm}}s in its slots. The hub shows how many it holds.
3. Place it within 6 blocks of a gate's base or a city shield generator along each axis, or place an [Energy Converter](energy-converter.md) against it for FE.

## Screen

The **ZPM Hub** screen has three **ZPM Slots**, which take only ZPMs. It shows:

- **Optimization factor:** how many times further the ZPMs' energy goes, `x2` or `x3`. It shows only while two or three charged ZPMs are in the hub.
- **Available Power:** the GE left in its ZPMs between them, as "Available Power: N GE".

## Optimization

The hub counts the ZPMs in it that have energy left. With two, it draws half as much GE from them for the same energy. With three, it draws a third as much. This holds for the gates and city shields that draw on it, and for an energy converter against it.

| Charged ZPMs | Optimization factor | GE given for each GE drawn |
| --- | --- | --- |
| 1 | x1 | 1 |
| 2 | x2 | 2 |
| 3 | x3 | 3 |

The hub draws on its ZPMs evenly.

## FE

The hub gives no FE itself, and it takes no FE in. An [Energy Converter](energy-converter.md) placed against it, input first, turns the hub's GE into FE. With two or three charged ZPMs, the converter gets 2 or 3 GE for each GE the hub takes from them. The converter still takes up to 62.5 GE a tick (`energy.converter_rate`) and gives 80 FE for each GE (`energy.bridge_rates.fe_out_per_ge`).

## Powering gates

A gate whose base is within 6 blocks of the hub along each axis draws on its ZPMs, after its DHD fuel and power units, the nearest ZPM block first. A [supergate](../building-gates.md#zpms-for-supergates) draws on it after the ZPMs in its own base. A city shield generator within 6 blocks draws on it too. See [Power](../power.md#zpms).

## Linking to a gate

A device linked to a gate powers it from anywhere in the same world, however far, as in SGCraft 2.2.0. It still has to be loaded.

1. Sneak and use the device with an empty hand. The message says to use a gate next.
2. Within a minute, use any block of a formed gate's ring or base with an empty hand. You need the `configure` right on that gate.

Only the player who put the device down, or an operator, links or unlinks it. Anyone else is told "This ZPM device isn't yours". Anyone can link a device put down before devices had owners. A device links to one gate at a time. Sneak-use it again before you use a gate to cancel, or to unlink it if it was linked. Breaking the device unlinks it. A gate of any size can have devices linked to it. Its [configurator](../items/configurator.md) shows **Linked ZPMs** on its Energy page, with each device's place and how many ZPMs it holds in the tip.

## Tainted ZPMs

If any ZPM in the hub is tainted, the hub explodes when it is drawn on, by a gate, a city shield or an energy converter: power 250, with fire, by default (`zpm.tainted_explosion`, 0 turns it off). See [Zero Point Module](../items/zpm.md#tainted-zpms).

## Breaking

Breaking the hub drops it and its ZPMs. Each ZPM keeps the energy left in it.

## Settings

The `zpm` object of `config/sgcraft-server.json`:

| Setting | Default | What it does |
| --- | --- | --- |
| `search_range` | 6 | How far from a gate's base or a city shield generator, along each axis, the hub may stand (1 to 16) |
| `tainted_explosion` | 250 | Power of a tainted ZPM's explosion; 0 turns it off |

See [Settings](../settings.md#zpms).

## See also

- [Zero Point Module](../items/zpm.md)
- [ZPM Console](zpm-console.md)
- [ZPM Interface Cart](zpm-interface-cart.md)
- [Energy Converter](energy-converter.md)
- [Power](../power.md#zpms)
