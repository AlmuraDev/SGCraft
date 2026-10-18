# The DHD

The dial-home device (DHD) is a pedestal of symbols that dials the gate next to it. It also holds the naquadah that powers the gate.

```infobox
DHD
Icon: sgcraft:dhd
Type: Block
Kinds: Milky Way DHD, Pegasus DHD
Dials: Gates of its own type
Range: 6 blocks (dhd_range)
Fuel: 5 naquadah slots
Recipe: [below](#recipes)
Dictionary: [Milky Way DHD](blocks/dhd.md), [Pegasus DHD](blocks/dhd-pegasus.md)
```

## Kinds

| DHD | Dials | Symbols |
| --- | --- | --- |
| Milky Way DHD (`sgcraft:dhd`) | Milky Way gates | Milky Way glyphs |
| Pegasus DHD (`sgcraft:dhd_pegasus`) | Pegasus gates | Pegasus glyphs, with the Milky Way `@` |

A Universe gate has no DHD. Dial it with a [PDD, a gate control tablet](tools.md) or `/sgcraft dial`.

The DHD faces the player who placed it, with its symbols tilted toward them. It holds water, so it can stand under the sea next to a gate.

## Linking to a gate

A DHD links to the nearest gate of its type that is closer than the gate's DHD range, measured in a straight line from the DHD to the gate's base. The range is the gate property `dhd_range`: 6 blocks by default, 0 to 32. In the configurator it is **DHD reach**.

The DHD links when placed, checks its link every 20 ticks (1 second), and links to the next nearest gate if its gate is broken or changes type. A DHD with no gate in range shows "This DHD isn't linked to a gate it can dial" when you try to use it.

## The dial screen

Right-click the DHD to open the **Dial-Home Device** screen. You must stay within 8 blocks of the DHD to use it.

The screen shows the DHD face on:

- An outer ring of 26 buttons: `A` to `Z`, clockwise from the top.
- An inner ring of 13 buttons: `0` to `9`, then `@`, `+` and `.`, clockwise from the top.
- The dome in the middle.

To dial:

1. Click the symbols of the address in order. Each one lights up, and the address so far shows above the DHD.
2. Click the dome to dial.

| Input | Does |
| --- | --- |
| Left-click a button | Adds its symbol |
| Right-click | Takes back the last symbol |
| A letter, digit, `@`, `+` or `.` key | Adds that symbol, in either case |
| Backspace | Takes back the last symbol |
| Enter | Presses the dome |

A key with no symbol shows in red for a moment. You can enter only as many symbols as the linked gate has chevrons, or 9 if the gate isn't loaded on your side.

The gate dials the address in standard mode, turning its ring for each symbol, or in quick mode if its `chevrons_lock_on_dial` property is on. If the dial fails, the reason shows above your hotbar. See [Dialing](dialing.md#what-can-stop-a-dial).

While the gate is active, the buttons show the address it dialed and the dome is lit. Clicking the symbols does nothing. Clicking the dome shuts the gate down, which needs the right to use it.

## Fuel

Sneak and right-click the DHD with an empty hand to open **DHD Fuel**. It has 5 slots that take only naquadah. The title shows how full the linked gate's own energy store is, as `Gate %`, or `No gate`.

- One naquadah gives the gate 96000 energy: the gate's `energy_per_naquadah` property, which defaults to the server setting `energy.per_naquadah`.
- The DHD burns a piece only when the gate needs more than what is left of the last one, taking from the last slot first. What a piece gives beyond the gate's need stays in the DHD for next time.
- The gate draws from its sources one after another: DHD fuel first, then power units, then ZPMs, the nearest of each first. It draws from its own store only for what they can't give.

The fuel reaches the gate only when:

- the DHD is linked to that gate,
- the DHD is within the gate's DHD range of the base horizontally, and no more than 2 blocks above or below it, and
- the gate's `dhd_fuel` property is on (the default, **DHD fuel powers it** in the configurator).

A dial to a gate nearby costs 4700 energy (7 chevrons at 100 and 4000 to open) plus 1 a tick while open, so one piece covers several dials. A dial to another world costs 10 times as much, and to another group of worlds 100 times. See [Power](power.md) for distance, other worlds and power from other mods.

## Tok'ra

A DHD is the job site of the Tok'ra. A villager with no job can take any DHD as its workstation and become a Tok'ra, who trades crystals. No other villager takes a DHD. See [World generation](worldgen.md).

## Recipes

```recipe
sgcraft:dhd
```

```recipe
sgcraft:dhd_pegasus
```

Breaking a DHD drops it and the naquadah inside. Every DHD at a gate the world built holds 3 naquadah.
