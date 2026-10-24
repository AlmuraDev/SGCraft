# Zero Point Module

The Zero Point Module (ZPM) holds a huge store of gate energy (GE). Gates, city shields and energy converters draw on it through the blocks that hold it.

```infobox
Zero Point Module
ID: sgcraft:zpm
Category: Power
Type: Item
Stackable: No
Capacity: 26,843,546 GE (zpm.capacity)
FE out: 80 FE for each GE, through an energy converter (energy.bridge_rates.fe_out_per_ge)
Found: Chests beside 15% of generated gates (zpm.chest_chance)
```

## Obtaining

The ZPM has no recipe.

### Chests beside generated gates

When the world builds a gate, there is a 15% chance (`zpm.chest_chance`) of a chest beside it holding one full ZPM. This counts for every generated gate: the ones in structures and the ones in villages. See [World generation](../worldgen.md#zpm-chests).

One chest ZPM in ten is tainted (`zpm.tainted_chance`). See [Tainted ZPMs](#tainted-zpms).

### Creative and commands

The SGCraft creative tab has a full ZPM. Operators can run `/sgcraft zpm give` for a full ZPM, or `/sgcraft zpm give tainted` for a tainted one.

## Usage

A ZPM does nothing on its own. Put it in one of these:

| Block | ZPMs | What draws on them |
| --- | --- | --- |
| {{item:sgcraft:zpm_console}} | 1 | Gates and city shield generators within 6 blocks, and an energy converter against it |
| {{item:sgcraft:zpm_interface_cart}} | 1 | Gates and city shield generators within 6 blocks, and an energy converter against it |
| {{item:sgcraft:zpm_hub}} | 3 | Gates and city shield generators within 6 blocks, and an energy converter against it. The more it holds, the further they go |
| {{item:sgcraft:supergate_base}} | 3 | Its own supergate |
| {{item:sgcraft:city_shield_generator}} | 3 | Its own shield |

The 6 blocks are counted from a gate's base, or from the generator, along each axis (`zpm.search_range`).

```gallery
scenes/zpm-devices-day.jpg | A ZPM hub with three ZPMs, an interface cart and a console, each holding ZPMs
scenes/zpm-devices-night.jpg | The same, by night
```

## Energy

A ZPM holds 26,843,546 GE when found (`zpm.capacity`). Gates and city shields take its GE as they take any other. The console, cart and hub give no FE themselves. An [Energy Converter](../blocks/energy-converter.md) placed against one turns its GE into FE, 80 FE for each GE by default (`energy.bridge_rates.fe_out_per_ge`). A dial to a gate nearby with a 7 symbol address costs 4700, so one ZPM opens such a wormhole thousands of times.

- A ZPM is never used up, only emptied. An empty ZPM stays in its slot.
- It keeps what is left in it when you take it out, carry it or break the block that holds it.
- Its tooltip shows the GE left as "Power: N GE".
- Once something has drawn on it, a bar on the item shows how full it is.
- A tainted ZPM shows neither, so it can't be told apart by its power.
- A gate or city shield takes from the nearest ZPM block first, and from the next only when the nearer runs short.
- A hub draws on the ZPMs it holds evenly, and so does a city shield generator on the ZPMs in its own slots.

See [Power](../power.md#zpms) for how gates use ZPMs alongside their other sources.

## Tainted ZPMs

A tainted ZPM shines like an enchanted item. It holds as much as any other, but its tooltip and bar never show its power.

When a ZPM console, interface cart or hub holding a tainted ZPM is drawn on, by a gate, a city shield or an energy converter, it explodes. The explosion has a power of 250 by default (a block of TNT has 4) and sets fires (`zpm.tainted_explosion`, 0 turns it off).

A tainted ZPM in a supergate's base or in a city shield generator doesn't explode.

## Supergates

A supergate needs no ZPMs. It draws on them as any gate does, and its base has three ZPM slots of its own, which it takes from before the consoles, carts and hubs near it. See [Building gates](../building-gates.md#zpms-for-supergates).

## Settings

The `zpm` object of `config/sgcraft-server.json`:

| Setting | Default | What it does |
| --- | --- | --- |
| `capacity` | 26843546 | GE a full ZPM holds |
| `chest_chance` | 15 | Percent of generated gates with a ZPM chest beside them |
| `tainted_chance` | 10 | Percent of chest ZPMs that are tainted |
| `tainted_explosion` | 250 | Power of a tainted ZPM's explosion; 0 turns it off |

See [Settings](../settings.md#zpms) for the rest.

## Commands

Operators can use these:

- `/sgcraft zpm give [tainted]` gives you a full ZPM.
- `/sgcraft zpm energy <ge>` sets the GE in the ZPM in your main hand, from 0 up to `zpm.capacity`, and replies "The ZPM holds N GE".
- `/sgcraft zpm capacity <ge>` sets how much a full ZPM holds, and `/sgcraft zpm` lists it with the other ZPM settings. Without one it says "Hold a ZPM in your main hand".

See [Commands](../commands.md#zpms).

## See also

- [Power](../power.md#zpms)
- [ZPM Console](../blocks/zpm-console.md)
- [ZPM Interface Cart](../blocks/zpm-interface-cart.md)
- [ZPM Hub](../blocks/zpm-hub.md)
- [Energy Converter](../blocks/energy-converter.md)
- [Supergate Base](../blocks/supergate-base.md#zpms)
