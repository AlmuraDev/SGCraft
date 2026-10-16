# Universe Gate Upgrade

The Universe Gate Upgrade turns a gate into a Universe gate, which has no DHD. It works on standard gates and supergates.

```infobox
Universe Gate Upgrade
ID: sgcraft:universe_upgrade
Category: Upgrades
Type: Item
Stackable: Yes (64)
Range: 6 blocks, looking at the gate
Right needed: sgcraft:configure
Changes type to: Universe (sgcraft:universe)
```

## Obtaining

### Crafting

Craft it from 1 {{item:sgcraft:core_crystal}}, 2 {{item:sgcraft:chevron_upgrade}}, 2 {{item:minecraft:diamond}} and 4 {{item:minecraft:ender_pearl}}:

```recipe
sgcraft:universe_upgrade
```

## Usage

1. Make sure the gate is idle. The tools show its state as **Idle** in green.
2. Hold the upgrade.
3. Use it on any block of the gate's ring or base, or use it in the air while you look at the gate's ring or opening from within 6 blocks.
4. The gate becomes a Universe gate. The upgrade is used up, except in creative mode.

You need the `sgcraft:configure` right on the gate. The gate's owner and operators have it.

| Message | Meaning |
| --- | --- |
| The gate is now a Universe gate | The type changed. |
| This gate is already a Universe gate | Nothing to change. |
| You aren't allowed to change this gate | You lack the right to configure it. |
| You can't change this gate while it's in use | The gate is busy. |
| An Ori supergate takes no upgrades | The gate is an Ori supergate. |

A Universe gate turns its whole ring while it dials, and every chevron stays lit while the gate is active. It has no DHD: dial it with a {{item:sgcraft:pdd}}, a {{item:sgcraft:gate_control_tablet}} or `/sgcraft dial`. A DHD linked to the gate stops dialing it and links to the next nearest gate of its own type in range. See [Building gates](../building-gates.md#gate-types).

## The upgrade rule

In survival, a gate owner or a player with the configure right changes a gate's type only with a gate upgrade. The configurator's **Type** setting is locked for them.

Operators and players in creative mode can set the type freely on the configurator's **Gate** page, while the gate is idle.

No item turns a gate back into a Milky Way gate. Only an operator or a creative player can do that, with the configurator or `/sgcraft type sgcraft:milky_way`.

## Breaking the gate

The gate stays a Universe gate while its base stays in place, even if the ring breaks apart and forms again. Breaking the base gives the upgrade back whole; the chevron upgrades crafted into it don't drop on their own. The base block keeps its own kind: a Milky Way base turned into a Universe gate drops a Milky Way base and the upgrade, and forms a Milky Way gate when placed again. A type set by an operator, a creative player or a command drops no upgrade.

## Commands

- `/sgcraft type sgcraft:universe` changes the gate's type (operators). The gate must be idle.

## See also

- [Building gates](../building-gates.md#gate-type-upgrades)
- [Building gates](../building-gates.md#gate-types)
- [Tools](../tools.md#personal-dialing-device)
- {{item:sgcraft:pegasus_upgrade}}
