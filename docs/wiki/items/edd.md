# Explorer Dialer Device

The Explorer Dialer Device lists every gate that world generation built, in every world, and shows which of them you have visited. It dials any of them from the gate near you.

```infobox
Explorer Dialer Device
ID: sgcraft:edd
Category: Tools
Type: Item
Stackable: No
Range: The gate near you, 6 blocks (`tools.range`), to dial from
Lists: Every generated gate, in every world
Holds: Your visits and names, kept with the world
```

## Obtaining

### Crafting

```recipe
sgcraft:edd
```

## Usage

1. Use the Explorer Dialer Device, in the air or on a block. Its screen opens.
2. Choose a gate from the list.
3. Stand near a gate and press **Dial**. The gate near you dials the chosen gate. Double-clicking an entry dials it too.

The device finds the gate to dial from the same way every tool does, within `tools.range` (6 blocks). See [How a tool finds its gate](../tools.md#how-a-tool-finds-its-gate). With no gate near you, the screen says "No gate near you to dial from".

## Screen

The header reads "Generated Addresses: (N) - Visited: V", with the number of generated gates and how many of them you have visited.

The list holds every gate world generation built, in every world, sorted by world and then by address. Each row shows:

- the name you gave the gate, or "Unnamed gate",
- its address in letters and in its glyphs,
- its world,
- **Visited** in green, or **Not visited**.

The list refreshes every 3 seconds.

The **Search** box above the list narrows it to the gates whose name, world or address has the text in it. Case doesn't matter, and neither do dashes in an address. The red **x** after it empties it.

### Buttons

| Button | What it does |
| --- | --- |
| **Dial** | Dials the chosen gate from the gate near you |
| **Disconnect** | Closes the near gate's wormhole or stops its dial |
| **Rename** | Gives the chosen gate the name in the **Name** box, up to 40 characters. A blank name clears it |
| **Close** | Closes the screen |

The list comes from world generation, so it has no **Add**, **Remove** or move buttons.

## Visits

A generated gate counts as visited when:

- you use the device at it, or
- you go through any gate as a player. Both ends count, if they are generated gates.

Your visits are kept for you with the world, so they survive death.

## Commands

- `/sgcraft explorer` lists the generated gates and which of them you have visited.
- `/sgcraft explorer <player>` shows another player's list (operators).
- `/sgcraft explorer forget <player>` forgets what a player has found: their visits and their names for gates (operators).

See [Commands](../commands.md#addresses).

## See also

- [Tools](../tools.md#explorer-dialer-device)
- [World generation](../worldgen.md#generated-gates)
- [Dialing](../dialing.md)
- {{item:sgcraft:pdd}}, which dials from an address book you keep yourself
