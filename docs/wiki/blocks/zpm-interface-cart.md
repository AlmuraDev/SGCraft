# ZPM Interface Cart

The ZPM Interface Cart holds one ZPM. Gates and city shield generators near it draw on its ZPM's gate energy (GE) as they do on a console's. An [Energy Converter](energy-converter.md) turns its GE into FE.

```infobox
ZPM Interface Cart
ID: sgcraft:zpm_interface_cart
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
Size: Half a block deep
Range: 6 blocks along each axis (zpm.search_range)
```

## Obtaining

### Crafting

Craft it from 2 {{item:minecraft:comparator}}, 1 {{item:minecraft:iron_block}}, 4 {{item:minecraft:copper_ingot}}, 1 {{item:sgcraft:power_unit}} and 1 {{item:minecraft:iron_ingot}}:

```recipe
sgcraft:zpm_interface_cart
```

## Usage

1. Place the cart. It faces you, and stands half a block deep across the way it faces.
2. Right-click it to open its screen, and put a {{item:sgcraft:zpm}} in its slot. The cart shows the ZPM while it holds one.
3. Place it within 6 blocks of a gate's base or a city shield generator along each axis, or place an [Energy Converter](energy-converter.md) against it for FE.

## Screen

The **ZPM Interface Cart** screen has one **ZPM** slot, which takes only a ZPM, and shows "Available Power: N GE", the GE left in the ZPM.

## FE

The cart gives no FE itself, and it takes no FE in. To power other mods' machines from its ZPM, place an [Energy Converter](energy-converter.md) against it, input first. The converter takes up to 62.5 GE a tick from the ZPM and gives 80 FE for each GE (`energy.bridge_rates.fe_out_per_ge`).

A full ZPM of 26,843,546 GE gives about 2.1 billion FE through a converter.

## Powering gates

A gate whose base is within 6 blocks of the cart along each axis draws on its ZPM, as it does on a [ZPM Console](zpm-console.md#powering-gates)'s. A supergate draws on it after the ZPMs in its own base, and a city shield generator within 6 blocks draws on it too. See [Power](../power.md#zpms).

## Linking to a gate

A device linked to a gate powers it from anywhere in the same world, however far, as in SGCraft 2.2.0. It still has to be loaded.

1. Sneak and use the device with an empty hand. The message says to use a gate next.
2. Within a minute, use any block of a formed gate's ring or base with an empty hand. You need the `configure` right on that gate.

A device links to one gate at a time. Sneak-use it again before you use a gate to cancel, or to unlink it if it was linked. Breaking the device unlinks it. A gate of any size can have devices linked to it. Its [configurator](../items/configurator.md) shows **Linked ZPMs** on its Energy page, with each device's place and how many ZPMs it holds in the tip.

## Tainted ZPMs

A tainted ZPM in the cart explodes when the cart is drawn on, by a gate, a city shield or an energy converter: power 250, with fire, by default (`zpm.tainted_explosion`, 0 turns it off). See [Zero Point Module](../items/zpm.md#tainted-zpms).

## Breaking

Breaking the cart drops it and its ZPM. The ZPM keeps the energy left in it.

## Settings

The `zpm` object of `config/sgcraft-server.json`:

| Setting | Default | What it does |
| --- | --- | --- |
| `search_range` | 6 | How far from a gate's base or a city shield generator, along each axis, the cart may stand (1 to 16) |
| `tainted_explosion` | 250 | Power of a tainted ZPM's explosion; 0 turns it off |

See [Settings](../settings.md#zpms).

## See also

- [Zero Point Module](../items/zpm.md)
- [ZPM Console](zpm-console.md)
- [ZPM Hub](zpm-hub.md)
- [Energy Converter](energy-converter.md)
- [Power](../power.md#zpms)
