# Stargate Iris Upgrade

The Stargate Iris Upgrade fits a metal iris to a gate. While the iris is shut, nothing comes through the gate.

```infobox
Stargate Iris Upgrade
ID: sgcraft:iris_upgrade
Category: Upgrades
Type: Item
Stackable: Yes (64)
Range: 6 blocks, looking at the gate
Right needed: sgcraft:configure
Fits: Metal iris (sgcraft:metal)
```

## Obtaining

### Crafting

Craft it from 8 {{item:sgcraft:iris_blade}} around 1 {{item:minecraft:redstone}}:

```recipe
sgcraft:iris_upgrade
```

## Usage

1. Hold the upgrade.
2. Use it on any block of the gate's ring or base, or use it in the air while you look at the gate's ring or opening from within 6 blocks.
3. The iris is fitted and starts open. The upgrade is used up, except in creative mode.

You need the `sgcraft:configure` right on the gate. The gate's owner and operators have it. The gate doesn't need to be idle.

| Message | Meaning |
| --- | --- |
| Iris fitted | The iris is on. |
| This gate already has an iris | The gate has one. |
| Open the iris or lower the shield before replacing it | The gate has a raised shield. Lower it first. |
| This type of gate takes no iris or shield | The gate is an Ori supergate. |
| You can't fit an iris to this gate | You lack the right, or something stopped it. |

The upgrade also replaces a gate's [shield](../iris.md#swapping-the-iris-and-the-shield), while the shield is lowered. You get the {{item:sgcraft:shield_upgrade}} back, unless the gate came with its shield.

Once fitted, open and close the iris with a {{item:sgcraft:gdo}}, a {{item:sgcraft:gate_control_tablet}}, `/sgcraft iris open` and `/sgcraft iris close`, or redstone at the base. It takes 70 ticks (3.5 seconds) to open or close. See [The iris](../iris.md#opening-and-closing).

## The upgrade rule

In survival, a gate owner or a player with the configure right fits an iris only with this item. The configurator's **Iris** setting is locked for them.

Operators and players in creative mode can fit or take off an iris freely with **Iris** on the configurator's **Gate** page, which chooses **None**, **Metal Iris** or **Shield**. An iris fitted that way gives no upgrade back.

## Getting the upgrade back

- **Breaking the gate:** breaking the base drops the iris upgrade, if the iris was fitted with one.
- **The configurator:** an operator who takes off an iris that was fitted with an upgrade gets the upgrade back, unless in creative mode.
- An iris the gate came with (from the `sgcraft:iris` gate default), or one fitted by a command, an operator's configurator or a creative player's configurator, gives nothing back.

`/sgcraft iris remove` takes the iris off and gives no upgrade back.

## Commands

Operators can fit and remove an iris without the item:

- `/sgcraft iris fit` fits a metal iris.
- `/sgcraft iris remove` takes the iris off.
- `/sgcraft gate_defaults sgcraft:iris "sgcraft:metal"` gives every new gate players build an iris. See [Settings](../settings.md#gate-defaults).

## See also

- [The iris](../iris.md#fitting-an-iris)
- [Building gates](../building-gates.md#breaking-a-gate)
- [Access](../access.md#rights)
- [Tools](../tools.md#stargate-configurator)
