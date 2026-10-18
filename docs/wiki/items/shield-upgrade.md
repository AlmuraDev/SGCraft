# Stargate Shield Upgrade

The Stargate Shield Upgrade fits an energy shield to a gate, in place of a metal iris. While the shield is raised, nothing comes through the gate, and the shield uses energy for as long as it stays up.

```infobox
Stargate Shield Upgrade
ID: sgcraft:shield_upgrade
Category: Upgrades
Type: Item
Stackable: Yes (64)
Range: 6 blocks, looking at the gate
Right needed: sgcraft:configure
Fits: Shield (sgcraft:shield)
Energy: 1 a tick while rising or raised (gate_energy_per_tick)
```

## Obtaining

### Crafting

Craft it from 4 {{item:sgcraft:naquadah_ingot}}, 2 {{item:minecraft:blue_dye}}, 2 {{item:minecraft:redstone}} and 1 {{item:sgcraft:core_crystal}}:

```recipe
sgcraft:shield_upgrade
```

## Usage

1. Hold the upgrade.
2. Use it on any block of the gate's ring or base, or use it in the air while you look at the gate's ring or opening from within 6 blocks.
3. The shield is fitted. The upgrade is used up, except in creative mode.

A gate has no iris, the metal iris or the shield, never both. You need the `sgcraft:configure` right on the gate.

| Message | Meaning |
| --- | --- |
| Shield fitted | The shield is on. |
| This gate already has a shield | The gate has one. |
| Open the iris or lower the shield before replacing it | The gate's metal iris is shut. Open it first. |
| This type of gate takes no iris or shield | The gate is an Ori supergate. |
| You can't fit a shield to this gate | You lack the right, or something stopped it. |

### Replacing a metal iris

The shield replaces a metal iris, but only while the iris is open. You get the {{item:sgcraft:iris_upgrade}} back, unless the gate came with its iris. It works the other way too: an iris upgrade replaces a lowered shield and gives the shield upgrade back, unless the gate came with its shield.

Once fitted, raise the shield by closing it and lower it by opening it: with a {{item:sgcraft:gdo}}, a {{item:sgcraft:gate_control_tablet}}, a [Shield Console](../blocks/shield-console.md), `/sgcraft iris close` and `/sgcraft iris open`, or redstone at the base. It rises or falls in 10 ticks (half a second). See [The iris](../iris.md#the-shield).

## The upgrade rule

In survival, a gate owner or a player with the configure right fits a shield only with this item. The configurator's **Iris** setting is locked for them.

Operators and players in creative mode can choose **None**, **Metal Iris** or **Shield** with **Iris** on the configurator's **Gate** page. A shield fitted that way gives no upgrade back.

## Getting the upgrade back

- Breaking the gate's base drops the shield upgrade, if the shield was fitted with one.
- An operator who takes off a shield with the configurator, or swaps it for the metal iris, gets the upgrade back, unless in creative mode.
- A shield the gate came with gives nothing back.

## Power

A raised shield uses gate energy: 1 a tick while it rises or stays up (`shields.gate_energy_per_tick`). It draws from the gate's own store and from its energy sources, such as naquadah in its DHD, and power units and ZPMs near it.

- You can't raise it without enough energy for one tick: "The gate doesn't have the energy to raise its shield".
- It falls when the energy runs out.
- A gate with `requires_power` off uses none.

See [Power](../power.md#the-gate-shield).

## Commands

Operators can fit and remove a shield without the item:

- `/sgcraft iris fit shield` fits a shield.
- `/sgcraft iris remove` takes it off.

See [Commands](../commands.md#iris).

## See also

- [The iris](../iris.md#the-shield)
- [Shield Console](../blocks/shield-console.md)
- [Stargate Iris Upgrade](iris-upgrade.md)
- [City Shield Generator](../blocks/city-shield-generator.md)
