# Naquadah

Naquadah is the raw material mined from naquadah ore. It fuels DHDs and battery chargers, charges batteries in the crafting grid, and goes into naquadah alloy ingots and several tools.

```infobox
Naquadah
ID: sgcraft:naquadah
Category: Materials
Type: Item
Stackable: Yes (64)
Gate energy: 96000 a piece in a DHD (energy_per_naquadah)
FE: 100,000 a piece for a battery or charger (fe_per_naquadah)
```

## Obtaining

Naquadah has no recipe.

### Mining

Mine {{item:sgcraft:naquadah_ore}} with an iron pickaxe or better. It drops 1 to 2 naquadah, more with Fortune, and 2 to 5 experience. Silk Touch drops the ore block instead. The ore generates in the Overworld, under lava and in small nodes deep down. See [World generation](../worldgen.md#naquadah-ore).

### Chest loot

SGCraft's loot rolls alongside the vanilla loot in these chests:

| Chest | Chance | Count |
| --- | --- | --- |
| Dungeon | 20% that the pool rolls, 1 to 2 rolls; each roll is naquadah 2 times in 7 | 1 to 8 |
| Bonus chest | 75% | 8 to 16 |

See [World generation](../worldgen.md#chest-loot) for the rest of each chest's table.

### Generated gates

Every {{item:sgcraft:dhd}} or {{item:sgcraft:dhd_pegasus}} at a gate the world built holds 3 naquadah. Breaking a DHD drops the naquadah inside it.

## Usage

### DHD fuel

1. Sneak and right-click a DHD with an empty hand to open **DHD Fuel**.
2. Put naquadah in its 5 slots. They take nothing else.

One naquadah gives the linked gate 96000 gate energy. The DHD burns a piece only when the gate needs more than what is left of the last one, taking from the last slot first, and keeps the rest of that piece for later. A dial to a gate nearby with a 7 symbol address costs 4700, so one piece pays for about 20 such dials, or keeps one wormhole open for 80 minutes.

The fuel reaches the gate only when the DHD is linked to it, stands within the gate's `dhd_range` (default 6) of the base across and no more than 2 blocks above or below it, and the gate's `dhd_fuel` property is on (default `true`). See [The DHD](../dhd.md#fuel) and [Power](../power.md#where-a-gate-draws-energy).

### Battery charger fuel

Put naquadah in the naquadah slot of a {{item:sgcraft:battery_charger}}. The charger holds 200,000 FE and burns a piece whenever its store has room for all 100,000 FE of it. It passes the charge to the battery in its bay at up to 1000 FE a tick. See [Power](../power.md#batteries-and-the-battery-charger).

### Charging a battery in the grid

Craft a {{item:sgcraft:battery}} that isn't full with naquadah, in any arrangement. Each naquadah adds 100,000 FE, up to the battery's capacity of 1,000,000 FE. A full battery doesn't craft.

```recipe
sgcraft:battery_recharge
```

### Crafting

Craft naquadah with an {{item:minecraft:iron_ingot}} to make a {{item:sgcraft:naquadah_ingot}}. Naquadah also goes into the {{item:sgcraft:pdd}} and the {{item:sgcraft:gdo}}.

## Settings

| Setting | Default | What it does |
| --- | --- | --- |
| `energy_per_naquadah` | 96000 | Gate energy one naquadah gives in a DHD |
| `malp.fe_per_naquadah` | 100000 | FE one naquadah gives a battery, crafted in or burned in a charger. A charger holds twice this |

A gate can have its own value with the `sgcraft:energy_per_naquadah` property, shown as **Per naquadah** on the configurator's **Energy** page, or with `/sgcraft property sgcraft:energy_per_naquadah set <json>` (operators). See [Power](../power.md#per-gate-settings).

## See also

- [The DHD](../dhd.md#fuel)
- [Power](../power.md)
- [World generation](../worldgen.md#naquadah-ore)
- [Getting started](../getting-started.md#1-find-naquadah)
