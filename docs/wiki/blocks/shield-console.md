# Shield Console

The Shield Console raises and lowers the shield of the nearest gate that has one. It stores no link: each time you use it, it finds the nearest shielded gate.

```infobox
Shield Console
ID: sgcraft:shield_console
Category: Tools
Type: Block
Stackable: Yes (64)
Tool: Pickaxe
Tool tier: Any pickaxe
Hardness: 1.5
Blast resistance: 10
Drops: Itself
Block entity: No
Range: 40 blocks to the nearest part of the ring (console_range)
Right needed: sgcraft:iris on the gate
```

## Obtaining

### Crafting

```recipe
sgcraft:shield_console
```

## Usage

1. Place the console within 40 blocks of a gate fitted with a shield. The distance is measured to the nearest part of the gate's ring.
2. Right-click it. Its screen opens for the nearest gate with a shield.
3. Press **Activate Shield** to raise the shield, or **Deactivate Shield** to lower it.

The screen shows the gate's address in letters and in its glyphs (when the gate's address is shown), and the shield's state: **Shield Active**, **Raising Shield...**, **Lowering Shield...** or **Shield Inactive**.

You must stay within 8 blocks of the console while the screen is open.

| Message | Meaning |
| --- | --- |
| No shielded Stargate within 40 blocks | No gate in range has a shield. |
| You don't have access to this Stargate's shield | You lack the `sgcraft:iris` right on the gate. |

The console works only gates fitted with a shield. A gate with a metal iris doesn't count. See [The iris](../iris.md#the-shield) for what the shield does and the energy it uses.

## Settings

| Setting | Default | What it does |
| --- | --- | --- |
| `shields.console_range` | 40 | How far from a console, in blocks, a gate's ring may be for the console to work its shield |

Operators change it with `/sgcraft shields console_range <blocks>`. See [Settings](../settings.md#shields).

## See also

- [The iris](../iris.md#the-shield)
- [Stargate Shield Upgrade](../items/shield-upgrade.md)
- [Access](../access.md#rights)
- [Tools](../tools.md)
