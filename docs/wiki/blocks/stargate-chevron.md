# Stargate Chevron Block

The Stargate Chevron Block is the segment of a standard gate's ring that carries a chevron. A standard gate takes 7 of them: three along the top, one in the middle of each side and one in each bottom corner.

```infobox
Stargate Chevron Block
ID: sgcraft:stargate_chevron
Category: Gate blocks
Type: Block
Stackable: Yes (64)
Tool: Pickaxe
Tool tier: Any pickaxe
Hardness: 1.5
Blast resistance: 6
Light: 12 while a lit chevron stands on it, in a formed gate
Waterloggable: Yes
Drops: Itself
Block entity: No
```

## The finished gate

```gallery
scenes/milky-way-day.jpg | Idle, by day
scenes/milky-way-night.jpg | Idle, by night
scenes/milky-way-open-day.jpg | Open, by day
scenes/milky-way-open-night.jpg | Open, by night
```

## Obtaining

### Crafting

```recipe
sgcraft:stargate_chevron
```

### Generated gates

The standard gates the world builds in structures and villages are made of these blocks. Players can't break a generated gate unless its **Breakable** setting (`sgcraft:breakable`) is on, or the server setting `gates.generated_gates_breakable` is on for gates without a setting of their own. See [World generation](../worldgen.md#generated-gates).

## Usage

Place 7 chevron blocks with 8 {{item:sgcraft:stargate_ring}} around a base, such as a {{item:sgcraft:stargate_base}}. Seen from the front:

```
C R C R C
R . . . R
C . . . C
R . . . R
C R B R C
```

`C` is a Stargate Chevron Block, `R` a Stargate Ring Block, `B` the base and `.` the opening, which must be air, fluid or event horizon. The gate forms when you place the last block of the ring, or the base next to a complete ring. A chevron block only fits a chevron place in the pattern, and a ring block only a ring place.

Chevron blocks only make standard gates. A supergate needs {{item:sgcraft:supergate_chevron}}.

See [Building gates](../building-gates.md#standard-gates) for the full build.

## Chevrons and chevron blocks

A standard gate always has 7 chevron blocks, but the number of chevrons it has is a setting of the gate, kept in its base. A gate forms with 7 chevrons, and each {{item:sgcraft:chevron_upgrade}} adds one, up to 9. The 8th and 9th chevrons are drawn on the bottom row of the ring. The chevrons limit the addresses the gate can dial:

| Chevrons | Can dial |
| --- | --- |
| 7 | Gates in the same world |
| 8 | Also gates in other worlds of the same group |
| 9 | Also gates in other groups |

See [Building gates](../building-gates.md#chevrons-and-upgrades) and [Addresses](../addresses.md#address-lengths).

## Behavior

- Once the gate forms, its chevron blocks merge into it and the gate draws as one model. A merged block lets light through and doesn't hide the faces of the blocks beside it.
- Right-click a chevron block of a formed gate to open the gate's **Stargate Address** screen. See [Milky Way Stargate Base](stargate-base.md#the-address-screen).
- Gate tools and upgrades work on the gate when you use them on any of its ring blocks.
- Pick block (middle click) on a block of a formed gate gives the gate spawner that builds a gate like it.
- A chevron block holds water, so a gate built under water stands in water.

## Light

Each lit chevron lights the block of the ring it stands on, at level 12. The drawn chevrons don't all line up with the chevron blocks, so a plain ring block can be lit too. Chevrons light as they lock while the gate dials and go dark when the gate shuts down. On a Universe gate, every chevron stays lit while the gate is active. An unformed chevron block gives no light.

## Breaking

Breaking any block of a formed gate breaks the gate apart, and the gate forms again when you put the ring back. A player may break a block of a formed gate if they are an operator, if they own the gate and `gates.owners_break_gates` is on (the default), or if the gate's **Breakable** setting is on. Anyone else sees "This gate can't be broken". Explosions and commands still break gates. See [Building gates](../building-gates.md#breaking-a-gate).

## See also

- [Building gates](../building-gates.md#standard-gates)
- [Stargate Ring Block](stargate-ring.md)
- [Lock order](../building-gates.md#lock-order)
