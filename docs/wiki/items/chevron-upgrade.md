# Stargate Chevron Upgrade

The Stargate Chevron Upgrade adds one chevron to a gate, up to 9. More chevrons let a gate dial longer addresses, which reach other worlds and other groups.

```infobox
Stargate Chevron Upgrade
ID: sgcraft:chevron_upgrade
Category: Upgrades
Type: Item
Stackable: Yes (64)
Range: 6 blocks, looking at the gate
Right needed: sgcraft:configure
Adds: 1 chevron, up to 9
```

## Obtaining

### Crafting

Craft it from 1 {{item:sgcraft:naquadah_ingot}}, 2 {{item:minecraft:ender_pearl}}, 2 {{item:minecraft:glowstone_dust}} and 2 {{item:minecraft:redstone}}:

```recipe
sgcraft:chevron_upgrade
```

## Usage

1. Make sure the gate is idle. The tools show its state as **Idle** in green.
2. Hold the upgrade.
3. Use it on any block of the gate's ring or base, or use it in the air while you look at the gate's ring or opening from within 6 blocks.
4. The gate gains one chevron. The upgrade is used up, except in creative mode.

You need the `sgcraft:configure` right on the gate. The gate's owner and operators have it.

A gate forms with 7 chevrons. One upgrade takes it to 8, and a second to 9.

| Chevrons | Can dial |
| --- | --- |
| 7 | Gates in the same world |
| 8 | Also gates in other worlds of the same group |
| 9 | Also gates in other groups |

| Message | Meaning |
| --- | --- |
| The gate now has N chevrons | The chevron was added. |
| This gate already has nine chevrons | The gate is full. |
| You can't add a chevron to this gate while it's in use | The gate is busy, or you lack the right. |
| An Ori supergate takes no upgrades | The gate is an Ori supergate, which always reaches as far as 9 chevrons. |

See [Addresses](../addresses.md#address-lengths) for what each length reaches. A dial with an eighth chevron costs 10 times as much energy, and one with a ninth 100 times. See [Power](../power.md#other-worlds-and-supergates).

## The upgrade rule

In survival, a gate owner or a player with the configure right adds chevrons only with this item. The configurator's **Chevrons** setting is locked for them.

Operators and players in creative mode can set 7, 8 or 9 chevrons freely on the configurator's **Gate** page, while the gate is idle. Chevrons set that way give no upgrades back.

## Breaking the gate

Breaking the base drops one chevron upgrade for each chevron added with a Chevron Upgrade. Chevrons the gate came with, and chevrons an operator, a creative player or a command set, drop nothing. When an operator lowers a gate's chevrons, the chevrons added without upgrades are taken away first, so the ones fitted with upgrades still drop.

Gates the world builds have 7 chevrons, or 9 one time in four. Gates from spawners and `/sgcraft place` have 9. None of these give upgrades back.

## Commands

- `/sgcraft chevrons <1-9>` sets a gate's chevrons directly (operators). The gate must be idle.
- The `gates.defaults` setting `sgcraft:chevrons` (7 to 9) gives every new gate players build that many chevrons. See [Settings](../settings.md#gate-defaults).

## See also

- [Building gates](../building-gates.md#chevrons-and-upgrades)
- [Building gates](../building-gates.md#breaking-a-gate)
- [Addresses](../addresses.md#address-lengths)
- [Tools](../tools.md#stargate-configurator)
