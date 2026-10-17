# Getting started

This walkthrough starts with your first piece of naquadah and ends with you walking through a wormhole. Along the way you build a Milky Way gate, a DHD to dial it, and a second gate to dial.

## 1. Find naquadah

Naquadah is the metal every gate is made from. It has no recipe.

- Naquadah ore generates in the Overworld. Look under lava below sea level: where lava rests on a solid block, that block turns to ore in clusters up to 3 by 3 blocks and 1 high. Small nodes also generate away from lava, starting between y -48 and -33.
- Mining the ore needs an iron pickaxe or better. It drops 1 or 2 naquadah, more with Fortune, and 2 to 5 experience. Silk Touch gives the ore block.
- Dungeon chests, the bonus chest and village weaponsmith and toolsmith chests can hold naquadah or naquadah alloy ingots.
- Every DHD at a gate the world built holds 3 naquadah.

See [World generation](worldgen.md) for the exact odds.

## 2. Make the materials

Turn naquadah into naquadah alloy ingots:

```recipe
sgcraft:naquadah_ingot
```

The base needs a core crystal and the DHD needs a controller crystal. Craft them from diamonds, trade for them with a Tok'ra at a generated gate, or find them in dungeon, stronghold library, village and bonus chests.

```recipe
sgcraft:core_crystal
```

```recipe
sgcraft:controller_crystal
```

## 3. Craft the gate blocks

A gate needs 8 ring blocks, 7 chevron blocks and 1 base.

```recipe
sgcraft:stargate_ring
```

```recipe
sgcraft:stargate_chevron
```

```recipe
sgcraft:stargate_base
```

One gate takes, in all:

| Material | Count |
| --- | --- |
| Naquadah alloy ingot | 40 |
| Chiseled sandstone | 40 |
| Smooth sandstone | 40 |
| Redstone dust | 8 |
| Ender pearl | 7 |
| Glowstone dust | 7 |
| Eye of ender | 1 |
| Stargate core crystal | 1 |

## 4. Build the ring

Pick a flat spot with 5 by 5 blocks of room. Stand where you want the front of the gate and place the base: it faces you, and the gate faces the same way. Then build the ring up from the base, in the plane that runs across in front of you. Seen from the front:

```layout
C R C R C
R . . . R
C . . . C
R . . . R
C R B R C
```

`C` is a chevron block, `R` a ring block, `B` the base and `.` empty space. The 9 blocks inside must be air (water and other fluids are fine).

The gate forms as soon as the last block goes in, whether that is the base or a ring block. The blocks merge into the gate's model, and you own the gate. If nothing happens, check that every block is in its place and that the inside is empty. See [Building gates](building-gates.md#forming-a-gate) for every rule.

## 5. Place a DHD

```recipe
sgcraft:dhd
```

Place the DHD less than 6 blocks from the base, and no more than 2 blocks above or below it, so its fuel reaches the gate. A spot 3 or 4 blocks in front of the gate, to one side, works well. The DHD links to the nearest Milky Way gate in range on its own. See [The DHD](dhd.md).

## 6. Fuel it

Sneak and right-click the DHD with an empty hand to open **DHD Fuel**. Put naquadah in its 5 slots. The title shows how full the gate's own energy store is, as `Gate %`.

A dial to a gate nearby costs 100 energy for each chevron and 4000 to open the wormhole, then 1 a tick while it stays open. One naquadah gives 96000 energy, and the DHD burns a piece only when the gate needs more. A dial to another world costs 10 times as much, and to another group of worlds 100 times. See [Power](power.md) for distance and other worlds.

## 7. Build a second gate

Build a second gate the same way, at least 128 blocks away so it is in a different address cell, or find one the world built (see [World generation](worldgen.md)). Two gates in one cell share an address.

Right-click the second gate's base or any of its ring blocks. The **Stargate Address** screen shows its address in glyphs and in letters. Click the panel to copy the address. You can also look at the gate and run `/sgcraft address`.

A gate in the same world has a 7 symbol address that ends in `@`, such as {{glyphs:XOBF-R3@}} (`XOBF-R3@`). See [Addresses](addresses.md).

## 8. Dial

Go back to the first gate and right-click its DHD. Click the symbols of the second gate's address in order, or type them on the keyboard. Click the dome in the middle, or press Enter, to dial.

The ring turns to each symbol and a chevron locks for each one, the top chevron last. The second gate locks its chevrons at the same time. Then the wormhole bursts open.

> **Warning:** Stand clear of the front of both gates while the wormhole opens. The burst kills anything in front of the gate and clears blocks in its way.

If the dial fails, the reason shows above your hotbar. See [Dialing](dialing.md#what-can-stop-a-dial).

## 9. Walk through

Walk into the event horizon from the front of the gate you dialed from. You come out of the front of the other gate, facing out of it. The wormhole only goes one way: stepping into the far end does nothing.

The wormhole closes after 5 minutes. To close it sooner, click the DHD's dome again.

## Next steps

- Fit an [iris](iris.md) so nothing can come through uninvited.
- Add a [chevron upgrade](building-gates.md#chevrons-and-upgrades) to dial other worlds.
- Carry a [PDD](tools.md) to dial without a DHD.
