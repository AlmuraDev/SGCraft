# Pegasus DHD

The Pegasus DHD is the dial-home device for Pegasus gates. It dials the Pegasus gate next to it with Pegasus glyphs and holds the naquadah that powers that gate.

```infobox
Pegasus DHD
ID: sgcraft:dhd_pegasus
Category: DHDs
Type: Block
Stackable: Yes (64)
Tool: Pickaxe
Tool tier: Any pickaxe
Hardness: 1.5
Blast resistance: 6
Waterloggable: Yes
Drops: Itself and the naquadah inside
Block entity: Yes
Dials: Pegasus gates
Link range: 6 blocks (dhd_range)
Fuel slots: 5, naquadah only
```

## Obtaining

### Crafting

```recipe
sgcraft:dhd_pegasus
```

### Generated gates

The gate the world builds on a platform near an igloo is a Pegasus gate with a Pegasus DHD. Every DHD at a gate the world built holds 3 {{item:sgcraft:naquadah}}. See [World generation](../worldgen.md#structure-gates).

## Usage

The Pegasus DHD works like the {{item:sgcraft:dhd}}, for Pegasus gates:

1. Build a Pegasus gate with a {{item:sgcraft:stargate_base_pegasus}}, or turn a gate into one with a {{item:sgcraft:pegasus_upgrade}}. See [Building gates](../building-gates.md#gate-types).
2. Place the DHD less than 6 blocks from the gate's base in a straight line, and no more than 2 blocks above or below it. It faces you, with its symbols tilted toward you.
3. Sneak and right-click it with an empty hand, then put naquadah in its fuel slots.
4. Right-click it to open the dial screen, enter an address and click the dome.

## Linking to a gate

The DHD links to the nearest Pegasus gate whose base is less than that gate's DHD range away, measured in a straight line from the DHD to the base. A base exactly that far away is out of range. The range is the gate property `dhd_range` (default 6, from 0 to 32), shown as **DHD reach** in the configurator.

It links when you place it and checks its link every 20 ticks (1 second). If its gate is broken or changes to another type, it links to the next nearest Pegasus gate in range. It never links to a Milky Way or Universe gate. A DHD with no gate in range shows "This DHD isn't linked to a gate it can dial" when you try to dial.

## Dial screen

Right-click the DHD to open the **Dial-Home Device** screen. It shows the Pegasus face, with 26 buttons for `A` to `Z` on the outer ring and 13 buttons for `0` to `9`, `@`, `+` and `.` on the inner ring, drawn as Pegasus glyphs, with the Milky Way `@`, `+` and `.`. The dome is in the middle. You must stay within 8 blocks of the DHD, or the screen closes.

| Input | Does |
| --- | --- |
| Left-click a button | Adds its symbol |
| Right-click | Takes back the last symbol |
| A letter, digit or `@` key | Adds that symbol, in either case |
| Backspace | Takes back the last symbol |
| Enter | Presses the dome |

The screen takes only as many symbols as the linked gate has chevrons, or 9 if the gate isn't loaded on your side. A key with no symbol shows in red for a moment.

Pressing the dome dials the address. If the dial fails, the reason shows above your hotbar. See [Dialing](../dialing.md#what-can-stop-a-dial). While the gate is active, the buttons show the address it dialed and clicking the dome shuts the gate down. Dialing and shutting down need the `sgcraft:use` right on the gate. See [Access](../access.md#rights).

## Fuel

Sneak and right-click the DHD with an empty hand to open **DHD Fuel**. It has 5 slots that take only {{item:sgcraft:naquadah}}. The title shows how full the linked gate's own reserve is, as `Gate %`, or `No gate`.

One naquadah gives the gate 96000 gate energy (`energy.per_naquadah`). The DHD burns a piece only when the gate needs more than what is left of the last one, last slot first. The fuel reaches the gate only when the DHD is linked to it and stands no more than 2 blocks above or below its base, and the gate's `dhd_fuel` property is on (default `true`). See [The DHD](../dhd.md#fuel).

## Tok'ra workstation

A villager with no job can take a Pegasus DHD as its workstation and become a Tok'ra, as with a Milky Way DHD. See [World generation](../worldgen.md#tokra-villagers).

## Breaking

Breaking the DHD drops it and the naquadah in its slots. The energy left over from a piece it had started on is lost. When you place it again, it links to the nearest Pegasus gate in range on its own.

## See also

- [The DHD](../dhd.md)
- [Milky Way DHD](dhd.md)
- [Building gates](../building-gates.md#gate-types)
- [Power](../power.md#where-a-gate-draws-energy)
