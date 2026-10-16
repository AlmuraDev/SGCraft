# Stargate Ring Block

The Stargate Ring Block is the plain segment of a standard gate's ring. A standard gate takes 8 of them, set between its 7 chevron blocks around a base.

```infobox
Stargate Ring Block
ID: sgcraft:stargate_ring
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
sgcraft:stargate_ring
```

### Generated gates

The standard gates the world builds in structures and villages are made of these blocks. Players can't break a generated gate unless its **Breakable** setting (`sgcraft:breakable`) is on, or the server setting `gates.generated_gates_breakable` is on for gates without a setting of their own. See [World generation](../worldgen.md#generated-gates).

## Usage

Place 8 ring blocks with 7 {{item:sgcraft:stargate_chevron}} around a base, such as a {{item:sgcraft:stargate_base}}. Seen from the front, the ring stands across the base's front like this:

```
C R C R C
R . . . R
C . . . C
R . . . R
C R B R C
```

`R` is a Stargate Ring Block, `C` a Stargate Chevron Block, `B` the base and `.` the opening, which must be air, fluid or event horizon. The gate forms when you place the last block of the ring, or the base next to a complete ring. If there is no standing ring, the base looks for one lying flat at its own height, running out from the base along its back.

A ring block that already belongs to a gate can't be part of a second one. Ring blocks only make standard gates: a supergate needs {{item:sgcraft:supergate_ring}}.

See [Building gates](../building-gates.md#standard-gates) for the full build.

## Behavior

- Once the gate forms, its ring blocks merge into it and the gate draws as one model. A merged ring block lets light through and doesn't hide the faces of the blocks beside it.
- Right-click a ring block of a formed gate to open the gate's **Stargate Address** screen, as if you had used its base. See [Milky Way Stargate Base](stargate-base.md#the-address-screen).
- Gate tools and upgrades, such as the {{item:sgcraft:chevron_upgrade}} or the {{item:sgcraft:configurator}}, work on the gate when you use them on any of its ring blocks.
- Pick block (middle click) on a ring block of a formed gate gives the gate spawner that builds a gate like it.
- A ring block holds water, so a gate built under water stands in water.

## Light

Each lit chevron lights the ring block it stands on, at level 12. The drawn chevrons don't all line up with the chevron blocks, so a lit chevron can light a plain ring block as well as a chevron block. When two chevrons stand on the same block, it is lit while either is. The light goes out as the chevrons go dark, and an unformed ring block gives no light.

## Breaking

Breaking any block of a formed gate breaks the gate apart. The other blocks stay where they are, and the gate forms again when you put the ring back.

A player may break a block of a formed gate if they are an operator, if they own the gate and the server setting `gates.owners_break_gates` is on (the default), or if the gate's **Breakable** setting is on. Anyone else sees "This gate can't be broken". Explosions and commands still break gates. Breaking a gate while its wormhole is open sets off a breach explosion. See [Building gates](../building-gates.md#breaking-a-gate).

## See also

- [Building gates](../building-gates.md#standard-gates)
- [Stargate Chevron Block](stargate-chevron.md)
- [Access](../access.md#breaking-gates)
