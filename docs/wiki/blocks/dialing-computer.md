# Dialing Computer

The Dialing Computer is the SGC's way to dial a gate: a desk three blocks wide with a wide monitor, worked from an [office chair](office-chair.md) in front of it. It dials the nearest gate in reach the slow way, the ring turning to each symbol and each chevron locking in turn, or the fast way, every chevron locking at once. Its monitor shows the dialing program to everyone who can see it.

```infobox
Dialing Computer
ID: sgcraft:dialing_computer
Category: Dialing
Type: Block (six parts)
Size: 3 blocks wide, 2 high, 1 deep
Stackable: Yes (64)
Tool: Pickaxe
Tool tier: Any pickaxe
Hardness: 1.5
Blast resistance: 6
Waterloggable: No
Drops: Itself
Block entity: Yes
Link range: 32 blocks (link_range)
```

```gallery
scenes/dialing-computer-day.jpg | Dialing, by day
scenes/dialing-computer-night.jpg | Dialing, by night
scenes/dialing-computer-screen-day.jpg | The dialing program
scenes/dialing-computer-open-night.jpg | The gate open
```

## Obtaining

### Crafting

```recipe
sgcraft:dialing_computer
```

## Placing

Place the computer where you want the middle of its desk. Its screen faces you, and the rest of it goes one block to each side and one block up, so it needs a space 3 blocks wide and 2 high. If any of that space is taken, it won't place.

The computer is one structure of six parts. Breaking any part breaks all six, and the computer drops once. Pistons can't move it.

## Linking

The computer links to the nearest gate of any type whose base is within 32 blocks (`link_range`) of the middle of its desk. It checks every second, and links to another gate if its gate is broken or moves out of reach. Put the gate room in reach, and the computer anywhere that looks down on it.

## Using it

Put an [office chair](office-chair.md) up to 2 blocks (`chair_reach`) in front of the middle of the desk, facing the computer. Then either sit in the chair, or use the computer, which sits you in its chair. Sitting turns you to face the screen and opens the **Dialing Computer** screen. Getting up closes it, and closing the screen gets you up.

Without a chair in front of it, using the computer says "Put an office chair in front of the computer, facing it, and sit down to use it".

| Control | What it does |
| --- | --- |
| Address box | The address to dial, written in its characters, such as `A1B2C3@` |
| **Slow dial** / **Fast dial** | Picks how the next dial runs. Slow: the gate's ring turns to each symbol and its chevrons lock one at a time, as on the show. Fast: every chevron locks at once and the wormhole forms straight after |
| **Dial** or Enter | Dials the address |
| **Abort** | Shuts the gate down while it dials or is open |
| **Iris** | Shuts the gate's iris, or raises its shield, if it is open, and opens it if it is shut |
| **Stand up** or Escape | Closes the screen and gets you up |

A dial from the computer is a dial like any other: it needs the gate's energy, access and a reachable address, as from a [DHD](dhd.md). If a dial fails, the reason shows over your hotbar.

## The dialing program

The screen shows the dialing program, after the one on the show. The same picture shows on the monitor in the world, so players behind the operator watch the gate dial.

- **Left:** a column of scrolling data, faster while the gate is busy, and the status box: **ACTIVE**, **IDLE** or **OFFLINE**, a light and **OK** for each chevron locked, and the iris or shield state.
- **Middle:** the gate's ring, its glyphs turning as the real ring turns, with its 7 or 9 chevrons. As the gate dials, the symbol being dialed shows large in the middle of the ring. When its chevron locks, the chevron turns red and the symbol flies to its box.
- **Right:** a numbered box for each symbol of the address, wired to its chevron once locked. The next box blinks while its symbol is dialed.
- **Bottom:** the status line, such as **SYSTEM READY**, **SEQUENCE IN PROGRESS** with **CHEVRON 3 ENCODED**, **CHEVRON 7 LOCKED**, **WORMHOLE ESTABLISHED** or **WORMHOLE DISENGAGED**.

An incoming wormhole flashes the screen's border red with **OFFWORLD ACTIVATION**, then **INCOMING WORMHOLE** once it opens. An open wormhole fills the ring with the event horizon. With no gate in reach, the screen shows **NO STARGATE IN RANGE**.

The program shows the glyphs of the gate's own type: Milky Way, Pegasus or Universe. A Pegasus or Ori gate's ring doesn't turn on the screen.

Players within 48 blocks see the monitor. The picture is drawn 20 times a second, only while someone can see it.

## Settings

The `computers` object of `config/sgcraft-server.json`:

| Setting | Default | What it does |
| --- | --- | --- |
| `link_range` | 32 | How far from the computer, in blocks, the base of the gate it dials may be (1 to 64) |
| `chair_reach` | 2 | How far in front of the computer, in blocks, its chair may stand (1 to 4) |

## Commands

`/sgcraft computers` shows the settings. `/sgcraft computers link_range <blocks>` and `/sgcraft computers chair_reach <blocks>` change them. Both need permission level 2. See [Commands](../commands.md#dialing-computers).

## See also

- [Office Chair](office-chair.md)
- [Dialing](../dialing.md#the-dialing-computer)
- [DHD](dhd.md)
