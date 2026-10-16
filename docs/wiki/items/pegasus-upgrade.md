# Pegasus Gate Upgrade

The Pegasus Gate Upgrade turns a gate into a Pegasus gate, which a Pegasus DHD dials. It works on standard gates and supergates.

```infobox
Pegasus Gate Upgrade
ID: sgcraft:pegasus_upgrade
Category: Upgrades
Type: Item
Stackable: Yes (64)
Range: 6 blocks, looking at the gate
Right needed: sgcraft:configure
Changes type to: Pegasus (sgcraft:pegasus)
```

## Obtaining

### Crafting

Craft it from 1 {{item:sgcraft:core_crystal}}, 2 {{item:minecraft:lapis_block}}, 4 {{item:minecraft:glowstone_dust}} and 2 {{item:minecraft:redstone}}:

```recipe
sgcraft:pegasus_upgrade
```

The second recipe copies an upgrade. Put a Pegasus Gate Upgrade where the core crystal goes, and you get 2 upgrades back:

```recipe
sgcraft:pegasus_upgrade_copy
```

## Usage

1. Make sure the gate is idle. The tools show its state as **Idle** in green.
2. Hold the upgrade.
3. Use it on any block of the gate's ring or base, or use it in the air while you look at the gate's ring or opening from within 6 blocks.
4. The gate becomes a Pegasus gate. The upgrade is used up, except in creative mode.

You need the `sgcraft:configure` right on the gate. The gate's owner and operators have it.

| Message | Meaning |
| --- | --- |
| The gate is now a Pegasus gate | The type changed. |
| This gate is already a Pegasus gate | Nothing to change. |
| You aren't allowed to change this gate | You lack the right to configure it. |
| You can't change this gate while it's in use | The gate is busy. |

A Pegasus gate keeps its ring still while it dials: a light runs around its screens to each chevron. It is dialed with a {{item:sgcraft:dhd_pegasus}}. A Milky Way DHD linked to the gate stops dialing it and links to the next nearest Milky Way gate in range. See [Building gates](../building-gates.md#gate-types).

## The upgrade rule

In survival, a gate owner or a player with the configure right changes a gate's type only with a gate upgrade. The configurator's **Type** setting is locked for them.

Operators and players in creative mode can set the type freely on the configurator's **Gate** page, while the gate is idle.

No item turns a gate back into a Milky Way gate. Only an operator or a creative player can do that, with the configurator or `/sgcraft type sgcraft:milky_way`.

## Breaking the gate

The gate stays a Pegasus gate while its base stays in place, even if the ring breaks apart and forms again. Breaking the base gives the upgrade back. The base block keeps its own kind: a Milky Way base turned into a Pegasus gate drops a Milky Way base and the upgrade, and forms a Milky Way gate when placed again. A type set by an operator, a creative player or a command drops no upgrade.

## Commands

- `/sgcraft type sgcraft:pegasus` changes the gate's type (operators). The gate must be idle.

## See also

- [Building gates](../building-gates.md#gate-type-upgrades)
- [Building gates](../building-gates.md#gate-types)
- [The DHD](../dhd.md#kinds)
- {{item:sgcraft:universe_upgrade}}
