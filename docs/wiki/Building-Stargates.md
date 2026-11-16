# Building Stargates

## The standard gate

A stargate is 5 blocks wide and 5 tall: the Stargate Base at the bottom centre, 7 Chevron Blocks and 8 Ring Blocks.
Seen from the front:

```
C R C R C
R . . . R
C . . . C
R . . . R
C R B R C
```

`B` Stargate Base, `C` Stargate Chevron Block, `R` Stargate Ring Block, `.` left empty (whatever is there isn't
checked, but anything in the way when the gate opens is destroyed).

- Place the base first, facing the way the gate should face. The ring can go in any order; the gate merges into one
  object the moment the pattern is complete, and the blocks then draw as a single ring.
- Breaking any ring block unmerges the gate. Breaking the base while the gate is **connected** causes an explosion.
- Right-click a merged gate to see its address.

### Lying flat

The same pattern laid on the ground, running away from the front of the base, makes a gate that lies face up. The
[Configurator](Configurator) can flip a flat gate face down. A standing gate can't be laid down, or the other way
round; rebuild it.

## Supergates

A supergate is a large circular ring of Supergate Ring and Supergate Chevron Blocks around a Supergate Base.

- The ring is a circle of radius 4 to 30 (9 to 61 blocks across). Any size works: the smallest complete ring around
  the base merges.
- It has 9 chevrons, spaced around the circle (the ring block nearest each chevron's angle must be a chevron block).
  The easiest way to see the exact layout is the creative test spawner or `/sgspawn supergate` (see
  [Server Administration](Server-Administration)).
- Supergates run on Zero Point Modules only: **3 ZPMs** at each end, in the base's ZPM slots, nearby, or linked (see
  [Power](Power)). Their energy costs are 8 times a standard gate's.
- A standard base placed inside a supergate ring (or the other way round) turns itself into the right kind of base.

## Gate types

| Type | How you get it | Notes |
|---|---|---|
| Milky Way | the default | spinning inner ring, Milky Way sounds |
| Pegasus (Atlantis) | Pegasus Type Upgrade Crystal | ring doesn't turn; a light runs round to each symbol; own DHD look and sounds |
| Universe | Universe Type Upgrade Crystal | Milky Way sounds and DHD |
| Ori | supergates only, set with the Configurator, `/sgspawn` or the Ori test spawner | always 9 chevrons; no iris, shield, upgrades or DHD fuel; only connects to other Ori gates |

Right-click a gate (any of its blocks) with an upgrade crystal to change its type; the crystal is used up. The
Configurator can also change the type.

## Upgrades

Right-click any block of the gate with the item. Each is used up.

- **Chevron Upgrade:** 9 chevrons, so the gate can dial 9-symbol addresses (other worlds).
- **Iris Upgrade:** a titanium iris. Anything coming through a closed iris dies (see
  [Addresses and Dialing](Addresses-and-Dialing#the-iris)).
- **Shield Upgrade:** an energy shield instead of an iris. It works like the iris but uses power while raised and
  drops when the power runs out.
- A gate has an iris or a shield, not both. To swap, open the iris or lower the shield first; you get the old upgrade
  back.
- **Invulnerability Upgrade** (creative only): the gate can't be broken except by a player in creative, and survives
  explosions, withers and the dragon.

## Breaking gates

- A connected gate can't be broken; disconnect it first.
- Whether survival players may break a gate is a per-gate setting, separate for player-built and world-generated
  gates (see [Configurator](Configurator)).
- Whether the base, ring blocks and DHD drop as items is a server setting (`block-harvest`); by default they don't.
