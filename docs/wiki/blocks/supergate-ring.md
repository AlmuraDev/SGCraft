# Supergate Ring Block

The Supergate Ring Block is the plain segment of a supergate's ring. A supergate is a large ring of these blocks and 9 supergate chevron blocks following a circle around a Supergate Base.

```infobox
Supergate Ring Block
ID: sgcraft:supergate_ring
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

## Obtaining

### Crafting

One craft makes 4.

```recipe
sgcraft:supergate_ring
```

## Usage

A supergate's ring is every block a circle passes through. Its radius, from the center of the circle to the base, is 4 to 30 blocks, so the gate is 9 to 61 blocks across. The 9 blocks of the ring nearest the 9 chevron angles are {{item:sgcraft:supergate_chevron}}, the rest are Supergate Ring Blocks, and the {{item:sgcraft:supergate_base}} sits at the bottom.

The ring grows with the radius. Not counting the base, it takes:

| Radius | Across | Ring blocks | Chevron blocks |
| --- | --- | --- | --- |
| 4 | 9 | 22 | 9 |
| 7 | 15 | 46 | 9 |
| 10 | 21 | 70 | 9 |
| 20 | 41 | 150 | 9 |
| 30 | 61 | 230 | 9 |

- The space inside the ring doesn't need to be clear. Only the ring blocks are checked.
- The base looks for the smallest complete ring of supergate blocks around it.
- The gate forms when you place the last block of the ring, or the base next to a complete ring.
- Supergate Ring Blocks only make supergates. A standard gate needs {{item:sgcraft:stargate_ring}}.

The ring is hard to lay out by hand. To see one, an operator can run `/sgcraft place sgcraft:milky_way 9 10`, which builds a supergate of radius 10, or place a supergate spawner, which builds one of radius 7. See [Building gates](../building-gates.md#supergates) for the full build.

## Behavior

- Once the gate forms, its ring blocks merge into it and the gate draws as one model, a standard gate scaled up to fit its blocks. A merged block lets light through and doesn't hide the faces of the blocks beside it.
- Right-click a ring block of a formed supergate to open the gate's **Stargate Address** screen. See [Supergate Base](supergate-base.md#the-address-screen).
- Gate tools and upgrades work on the gate when you use them on any of its ring blocks.
- Pick block (middle click) on a ring block of a formed supergate gives the supergate spawner of its type.
- A ring block holds water, so a supergate built under water stands in water.

## Light

Each lit chevron lights the ring block nearest it, at level 12. That is most often a chevron block, but on some sizes of ring a chevron lights the plain ring block beside it. The light goes out as the chevrons go dark, and an unformed ring block gives no light.

## Breaking

Breaking any block of a formed supergate breaks the gate apart, and the gate forms again when you put the ring back. A player may break a block of a formed gate if they are an operator, if they own the gate and `owners_break_gates` is on (the default), or if the gate's **Breakable** setting is on. Anyone else sees "This gate can't be broken". Explosions and commands still break gates. See [Building gates](../building-gates.md#breaking-a-gate).

## See also

- [Building gates](../building-gates.md#supergates)
- [Supergate Chevron Block](supergate-chevron.md)
- [Supergate Base](supergate-base.md)
