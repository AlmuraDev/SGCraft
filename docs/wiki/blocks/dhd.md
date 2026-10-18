# Milky Way DHD

The Milky Way DHD (dial-home device) is a pedestal of symbols that dials the Milky Way gate next to it. It also holds the naquadah that powers that gate.

```infobox
Milky Way DHD
ID: sgcraft:dhd
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
Dials: Milky Way gates
Link range: 6 blocks (dhd_range)
Fuel slots: 5, naquadah only
```

## Obtaining

### Crafting

```recipe
sgcraft:dhd
```

### Generated gates

The world puts a Milky Way DHD at the Milky Way gates it builds in desert pyramids, jungle temples, ocean monuments, Nether fortresses, swamp hut platforms and villages. Every DHD at a gate the world built holds 3 {{item:sgcraft:naquadah}}. See [World generation](../worldgen.md#structure-gates).

## Usage

1. Build a Milky Way gate. See [Building gates](../building-gates.md).
2. Place the DHD less than 6 blocks from the gate's base in a straight line, and no more than 2 blocks above or below it. It faces you, with its symbols tilted toward you.
3. Sneak and right-click it with an empty hand, then put naquadah in its fuel slots.
4. Right-click it to open the dial screen, enter an address and click the dome.

## Linking to a gate

The DHD links to the nearest Milky Way gate whose base is less than that gate's DHD range away, measured in a straight line from the DHD to the base. A base exactly that far away is out of range. The range is the gate property `dhd_range` (default 6, from 0 to 32), shown as **DHD reach** in the configurator.

The DHD links when you place it and checks its link every 20 ticks (1 second). If its gate is broken or changes to another type, it links to the next nearest Milky Way gate in range. It never links to a Pegasus or Universe gate. A DHD with no gate in range shows "This DHD isn't linked to a gate it can dial" when you try to dial.

Tools that look for a gate near you, such as the {{item:sgcraft:pdd}}, treat a DHD as the gate it is linked to. See [Tools](../tools.md#how-a-tool-finds-its-gate).

## Dial screen

Right-click the DHD to open the **Dial-Home Device** screen. It shows the DHD face on, with 26 buttons `A` to `Z` on the outer ring, 13 buttons `0` to `9`, `@`, `+` and `.` on the inner ring, and the dome in the middle. You must stay within 8 blocks of the DHD, or the screen closes.

| Input | Does |
| --- | --- |
| Left-click a button | Adds its symbol |
| Right-click | Takes back the last symbol |
| A letter, digit or `@` key | Adds that symbol, in either case |
| Backspace | Takes back the last symbol |
| Enter | Presses the dome |

The screen takes only as many symbols as the linked gate has chevrons (7 to 9), or 9 if the gate isn't loaded on your side. A key with no symbol shows in red for a moment.

Pressing the dome dials the address. The gate turns its ring for each symbol, or locks every chevron at once if its `chevrons_lock_on_dial` property is on. If the dial fails, the reason shows above your hotbar. See [Dialing](../dialing.md#what-can-stop-a-dial).

While the gate is active, the buttons show the address it dialed and the dome is lit. The symbols do nothing then. Clicking the dome shuts the gate down. Dialing and shutting down need the `sgcraft:use` right on the gate. See [Access](../access.md#rights).

## Fuel

Sneak and right-click the DHD with an empty hand to open **DHD Fuel**. It has 5 slots that take only {{item:sgcraft:naquadah}}. The title shows how full the linked gate's own reserve is, as `Gate %`, or `No gate` when the DHD isn't linked.

- One naquadah gives the gate 96000 gate energy, the gate's `energy_per_naquadah` property (default: the server setting `energy.per_naquadah`).
- The DHD burns a piece only when the gate needs more than what is left of the last one, taking from the last slot first. What a piece gives beyond the gate's need stays in the DHD for the next payment.
- A gate draws on DHD fuel first, then power units, then ZPMs, the nearest of each first. It draws on its own reserve only for what they can't give.

The fuel reaches the gate only when all of these hold:

- the DHD is linked to that gate,
- the DHD stands no more than 2 blocks above or below the base,
- the gate's `dhd_fuel` property is on (default `true`, **DHD fuel powers it** in the configurator).

A dial to a gate nearby with a 7 symbol address costs 4700 gate energy, so one naquadah pays for about 20 such dials. See [Power](../power.md#what-a-wormhole-costs).

## Tok'ra workstation

A DHD is the job site of the Tok'ra villager. A villager with no job can take a Milky Way DHD as its workstation and become a Tok'ra, who trades crystals. No other villager takes a DHD. See [World generation](../worldgen.md#tokra-villagers).

## Breaking

Breaking the DHD drops it and the naquadah in its slots. The energy left over from a piece it had started on is lost. When you place it again, it links to the nearest gate in range on its own.

## See also

- [The DHD](../dhd.md)
- [Pegasus DHD](dhd-pegasus.md)
- [Dialing](../dialing.md)
- [Power](../power.md#where-a-gate-draws-energy)
- [Getting started](../getting-started.md#5-place-a-dhd)
